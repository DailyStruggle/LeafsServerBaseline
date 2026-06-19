# Iris ".iob" Object File Format (V2)

Reference spec for the binary `.iob` object format used by Iris jigsaw/tree objects,
reverse-engineered from and kept in sync with our own tooling:

- `iris/scripts/iob_lib.py` (`write_iob` / `build_iob_bytes`) - the single shared
  binary writer (also exposes the `TileData` `encode_tile_*` helpers)
- `iris/scripts/nbt_to_iris.py` - writer front-end (vanilla `.nbt` -> `.iob`), now
  delegates the bytes to `iob_lib`
- `iris/scripts/json_to_iob.py` - writer front-end (hand-authored JSON source ->
  `.iob`), also delegates to `iob_lib`; can emit `TileData` states
- `iris/scripts/iob_inspect.py` - reader / diagnostic (dumps blocks and states)
- `iris/scripts/recenter-iob.py` - rewriter (re-centers block coords)

All of these agree on the layout below. If you change the byte layout, change it
in `iob_lib.py` and update this doc plus the reader.

## Authoring source: JSON -> `.iob`

For hand-built objects, author a diffable JSON source and compile it with
`json_to_iob.py`; the binary `.iob` is a regenerable build OUTPUT, not the
canonical asset. Sources live under `iris/object-src/`. The schema (`size`,
optional `centered`, `blocks[]`, optional `tiles[]`) is documented in the module
docstring of `iris/scripts/json_to_iob.py`; `tiles[]` entries compile to the
`TileData` records described in [`TILEDATA-FORMAT.md`](TILEDATA-FORMAT.md).

## At a glance

- Binary, **big-endian** (Java `DataOutputStream` byte order).
- Strings use the Java `writeUTF` convention: a `uint16` byte-length prefix
  followed by the UTF-8 bytes (no terminator).
- A fixed header, then a palette of block-state strings, then a flat block list
  that indexes into that palette, then a trailing tile-entity ("states") count.
- Block coordinates are stored **centered** on the object middle (see
  *Coordinate convention*).

## Byte layout

Fields are listed in file order. "Java UTF" = `uint16` length + that many UTF-8
bytes.

| # | Field | Type | Notes |
|---|-------|------|-------|
| 1 | `width` (X size) | `int32` | bounding-box size along X |
| 2 | `height` (Y size) | `int32` | bounding-box size along Y |
| 3 | `depth` (Z size) | `int32` | bounding-box size along Z |
| 4 | `signature` | Java UTF | literal `Iris V2 IOB;` |
| 5 | `paletteSize` | `int16` | number of distinct block-state strings |
| 6 | `palette[paletteSize]` | Java UTF each | block-state strings (see *Block-state strings*) |
| 7 | `blockCount` | `int32` | number of placed blocks |
| 8 | `blocks[blockCount]` | 4 x `int16` each | `x, y, z, paletteIndex` (see *Coordinate convention*) |
| 9 | `statesCount` | `int32` | tile-entity / `TileData` records that follow |

After field 9, `statesCount` tile-entity records follow in Iris'
`TileData` binary format (documented separately in
[`TILEDATA-FORMAT.md`](TILEDATA-FORMAT.md)). `nbt_to_iris.py` always writes
`statesCount = 0` (jigsaw connectors are lifted into the jigsaw-piece JSON and
container loot is wired via Iris loot tables), but `json_to_iob.py` can emit
real records from a source's `tiles[]` array (modern key+JSON or legacy
sign/spawner/banner payloads). Readers that only need geometry can stop after
field 9 when `statesCount == 0`.

### Each block record (field 8)

Four signed `int16` values, in order:

1. `x` - centered X coordinate
2. `y` - centered Y coordinate
3. `z` - centered Z coordinate
4. `paletteIndex` - index into `palette` (field 6)

## Coordinate convention

Block coordinates are stored relative to the object center, not a 0-based corner:

```
stored = original - (width // 2, height // 2, depth // 2)
```

- `//` is integer floor division.
- The **same** centering is applied to jigsaw connector positions in the
  generated jigsaw-piece JSON, so connecting pieces stay aligned.
- This matches stock Iris objects. Older 0-based files render offset (e.g. trees
  floating ~height/2 above ground); `recenter-iob.py` fixes them in place and is
  idempotent (it skips any file that already has a negative min coordinate).

## Block-state strings (palette)

Each palette entry is a Minecraft block-state string:

- No properties: `minecraft:oak_planks`
- With properties: `minecraft:oak_log[axis=x]`, `minecraft:rail[shape=east_west]`

Built as `Name` + `[k1=v1,k2=v2,...]` where the bracketed part is omitted when
the block has no properties (`nbt_to_iris.py` `block_string`).

### Blocks skipped on conversion

When converting from vanilla `.nbt`, these are always dropped (`nbt_to_iris.py`
`ALWAYS_SKIP_BLOCKS`) - never written to the palette or block list:

- `minecraft:structure_void`
- `minecraft:jigsaw` (metadata only - its connector info goes into the piece JSON)

`minecraft:air` is also dropped by default, but only because Iris never PASTES
air from an object (`IrisObject.place` skips `AIR`/`CAVE_AIR`). Air still matters
for geometry: `IrisObject.shrinkwrap()` derives the object's `width/height/depth`
and `center` from the stored blocks, so dropping air shrinks the object to its
solid extent. For pieces that rely on a `bore` placement to hollow themselves out
(mineshafts, cave structures), that shrink makes `bore` under-carve and, on
rotation, shifts the blocks off the connector positions - so adjacent pieces stop
overlapping. Convert those with `--keep-air`: the air cells are stored (still not
pasted) purely to preserve the full footprint/center so `bore` carves the whole
piece and connectors stay aligned.

## Worked read (pseudocode)

Mirrors `iob_inspect.py`:

```
read int32  -> w
read int32  -> h
read int32  -> d
read uint16 -> n;  read n bytes -> signature   # "Iris V2 IOB;"
read int16  -> paletteSize
repeat paletteSize: read uint16 -> n; read n bytes -> palette entry
read int32  -> blockCount
repeat blockCount: read int16 x4 -> x, y, z, paletteIndex
read int32  -> statesCount                      # 0 from our writer
```

## Notes and gotchas

- **Endianness is big-endian everywhere.** Python `struct` format strings in the
  scripts use `>` (e.g. `">iii"`, `">hhhh"`, `">H"`).
- `paletteSize` is a signed `int16`, so a single object is limited to 32767
  distinct block states (far beyond any real piece).
- Coordinates are signed `int16`; with centering this comfortably covers Iris
  object sizes (roughly +/- 32767 per axis).
- The signature string is exactly `Iris V2 IOB;` (trailing semicolon included).
- Strings are written as plain UTF-8 with a `uint16` length. Block-state strings
  are ASCII in practice, so this is interchangeable with Java modified UTF-8.
