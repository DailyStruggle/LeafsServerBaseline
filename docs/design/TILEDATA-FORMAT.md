# Iris "TileData" Binary Format

Reference spec for the per-tile-entity ("states") records that may follow the
block list inside an Iris V2 `.iob` object (see
[`IOB-FILE-FORMAT.md`](IOB-FILE-FORMAT.md), field 9 `statesCount`). A TileData
record stores the extra block-entity state that a plain block-state string
cannot carry: chest/sign/spawner/banner contents and similar.

This was reverse-engineered from the upstream Iris source. `nbt_to_iris.py` still
always writes `statesCount = 0`, but `json_to_iob.py` can now emit these records
from a JSON source's `tiles[]` array (via the `encode_tile_*` helpers in
`iris/scripts/iob_lib.py`). Relevant upstream classes:

- `IrisObject.read` / `IrisObject.write` - frames each record with its position.
- `TileData.read` / `TileData.toBinary` - the modern (V2) per-tile payload.
- `LegacyTileData` - the older ordinal-based payload, kept for back-compat.

## Where it sits in the `.iob`

After the block list, the object ends with:

```
read int32 -> statesCount
repeat statesCount:
    read int16 -> x
    read int16 -> y
    read int16 -> z          # same object-centered coords as blocks
    <TileData payload>       # see below
```

So each state is a centered `(x, y, z)` (signed `int16`, same centering rule as
blocks: `stored = original - size // 2`) followed immediately by one TileData
payload. The framing (the three coordinate shorts) lives in `IrisObject`, not in
`TileData`.

## Two payload encodings (modern vs legacy)

There is no version flag in front of a TileData payload. The reader
(`TileData.read`) is mark/reset based:

1. `mark` the stream.
2. Try to read the **modern** payload (two Java-UTF strings).
3. If anything throws, `reset` to the mark and parse a **legacy** payload
   instead.

This works because the two encodings start with structurally different bytes
(modern = a `uint16`-prefixed UTF material key; legacy = a bare `int16` type id).
A correct reader must support `mark`/`reset`; `IrisObject` wraps the object
stream so this holds.

### Modern payload (`TileData.toBinary`)

Two Java-UTF strings, big-endian, `writeUTF` convention (`uint16` byte-length +
UTF-8 bytes):

| # | Field | Type | Notes |
|---|-------|------|-------|
| 1 | `material` | Java UTF | namespaced key, e.g. `minecraft:chest`; empty string `""` if null |
| 2 | `properties` | Java UTF | a JSON object string (Gson, HTML-escaping disabled, lenient) |

- `material` round-trips through `Material.matchMaterial(...)` / `Material.getKey()`.
- `properties` is the NMS-serialized tile-entity NBT rendered to JSON
  (`INMS.get().serializeTile(...)` -> map -> Gson). On load it is parsed back to a
  map and applied via `INMS.get().deserializeTile(...)`.
- **Caveat:** the JSON contents are NMS/Minecraft-version-specific. The container
  format (two UTF strings) is stable, but the JSON inside `properties` is only
  guaranteed to deserialize on a compatible server version.

### Legacy payload (`LegacyTileData`)

Starts with an `int16` type id, then a handler-specific body. Only three tile
types are supported; an unknown id raises `Unknown tile type: <id>`.

```
read int16 -> id        # 0 = sign, 1 = spawner, 2 = banner
<handler body for id>
```

Handler bodies (all multi-byte values big-endian):

- **Sign (`id = 0`)** - `SignHandler`
  1. `line1` Java UTF
  2. `line2` Java UTF
  3. `line3` Java UTF
  4. `line4` Java UTF
  5. `dyeColor` `int8` - `DyeColor` enum ordinal

- **Spawner (`id = 1`)** - `SpawnerHandler`
  1. `entityType` `int16` - `EntityType` enum ordinal (spawned mob)

- **Banner (`id = 2`)** - `BannerHandler`
  1. `baseColor` `int8` - `DyeColor` enum ordinal
  2. `patternCount` `int8`
  3. `patternCount` x:
     - `color` `int8` - `DyeColor` enum ordinal
     - `pattern` `int8` - `PatternType` enum ordinal

- **Caveat:** the legacy encoding stores Bukkit **enum ordinals**, not stable
  keys, so it is fragile across Bukkit/Minecraft versions (an enum reorder or
  insertion shifts every value). The modern key/JSON payload exists precisely to
  avoid this; prefer it for anything new.

## Worked read (pseudocode)

```
read int32 -> statesCount
repeat statesCount:
    read int16 -> x
    read int16 -> y
    read int16 -> z
    mark()
    try:
        read uint16+utf -> material
        read uint16+utf -> propertiesJson      # modern
    except:
        reset()
        read int16 -> id                        # legacy
        switch id:
            0: utf x4 (lines), int8 dyeColor
            1: int16 entityTypeOrdinal
            2: int8 baseColor, int8 n, n x (int8 color, int8 pattern)
```

## Notes and gotchas

- **Big-endian everywhere** (Java `DataInputStream`/`DataOutputStream`); strings
  use the `writeUTF` length-prefixed form.
- **Coordinates are framed by `IrisObject`, not `TileData`** - do not expect a
  position inside the payload itself.
- **`statesCount = 0`** is the common case (and the only case our `nbt_to_iris.py`
  writer emits): a geometry-only reader can stop after the block list.
- A modern payload's `material` may be the empty string `""` (serialized from a
  null material); guard against it on read.
- The legacy reader needs `mark`/`reset`; if your stream does not support marking,
  modern payloads can still be read but the legacy fallback cannot.
