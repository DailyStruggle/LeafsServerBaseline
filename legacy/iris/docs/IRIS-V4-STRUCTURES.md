# Iris v4.0 Structures Reference

Reference for how Iris 4.0 loads, assembles, and places structures, derived by
inspecting the classes in `Iris.jar` (`art.arcane.iris.engine.object.IrisStructure`,
`IrisStructurePlacement`, `IrisJigsawPool`, `IrisJigsawPiece`, `IrisJigsawConnector`,
`IrisImportedStructureControl`, `IrisVanillaStructureAdjustment`, and the loader
registrations in `art.arcane.iris.core.loader.IrisData`). Field names below are the
JSON keys (the decompiled fields/getters map 1:1 to these keys).

Live examples in the pack: `iris/pack-base/dimensions/overworld.json` (the
`importedStructures` + `structures` blocks), `iris/pack-base/structures/*.json`,
`iris/pack-base/jigsaw-pools/*.json`, and `iris/pack-base/jigsaw-pieces/*.json`. The
runtime catalogue of every placeable structure key is in
`iris/pack-base/structures/structure-index.json`.

> Layering note: never edit `iris/pack-base`. To change a structure/pool/piece or a
> dimension's structure placement, add/edit the same-path file under
> `iris/pack-overlay` (whole-file override).

---

## TL;DR -- the v4 model

In Iris 4.0 there are two independent ways structures appear in the world:

1. **Vanilla / datapack structures** -- toggled (and shifted) by the dimension's
   `importedStructures` block (`IrisImportedStructureControl`). These are the real
   Mojang structures (villages, trail ruins, ancient cities, ...). You do **not**
   redefine them; you switch them on/off and optionally nudge their position.
2. **Iris-native structures** -- defined as `IrisStructure` files in the `structures/`
   folder and *placed* by adding `IrisStructurePlacement` entries to a biome's,
   region's, or dimension's `structures` array. An `IrisStructure` is assembled from a
   jigsaw graph: `startPool` -> `IrisJigsawPool` -> `IrisJigsawPiece` -> `.iob` object.

Both kinds are catalogued by key in `structures/structure-index.json`.

### Folder -> class -> loader map (v4)

| Folder | Class | `IrisData` loader |
|--------|-------|-------------------|
| `structures/` | `IrisStructure` | `getStructureLoader()` |
| `jigsaw-pools/` | `IrisJigsawPool` | `getJigsawPoolLoader()` |
| `jigsaw-pieces/` | `IrisJigsawPiece` | `getJigsawPieceLoader()` |

These three (plus the object loaders) are the **only** structure-related loaders
registered in `IrisData`. See *Migration note* below for what this means for the
legacy `jigsaw-structures/` folder.

---

## Where structures are configured

| Scope | Field / file | Type | Purpose |
|-------|--------------|------|---------|
| Dimension vanilla control | `importedStructures` | `IrisImportedStructureControl` | Enable/disable/shift vanilla + datapack structures dimension-wide. |
| Dimension Iris placement | `structures` | `KList<IrisStructurePlacement>` | Place Iris-native structures across the whole dimension. |
| Region Iris placement | `structures` | `KList<IrisStructurePlacement>` | Place Iris-native structures within a region's biomes. |
| Biome Iris placement | `structures` | `KList<IrisStructurePlacement>` | Place Iris-native structures within a single biome. |

`IrisDimension`, `IrisRegion`, and `IrisBiome` all expose a `structures` array of
`IrisStructurePlacement`. Only `IrisDimension` exposes `importedStructures`.

---

## `IrisImportedStructureControl` fields

Controls the real vanilla/datapack structure generators. Lives in the dimension under
`importedStructures`.

| Field | Type | Purpose |
|-------|------|---------|
| `mode` | `VanillaStructureMode` | Master switch: `ALL_ON`, `ALL_OFF`, or `CUSTOM`. |
| `enabled` | `KList<String>` | Structure keys to force on (used with `CUSTOM`). |
| `disabled` | `KList<String>` | Structure keys to force off. |
| `undergroundYShift` | int | Global Y shift applied to underground vanilla structures. |
| `datapackOverrides` | boolean | Allow datapack-defined structures to participate. |
| `adjustments` | `KList<IrisVanillaStructureAdjustment>` | Per-structure position nudges. |

### `VanillaStructureMode`

| Value | Meaning |
|-------|---------|
| `ALL_ON` | All vanilla/datapack structures generate (then apply `disabled` + `adjustments`). |
| `ALL_OFF` | No vanilla structures generate (then apply `enabled`). |
| `CUSTOM` | Only keys listed in `enabled` generate. |

### `IrisVanillaStructureAdjustment` fields

| Field | Type | Purpose |
|-------|------|---------|
| `match` | `KList<String>` | Structure keys this adjustment applies to (e.g. `minecraft:stronghold`). |
| `yShift` | int | Vertical offset (blocks). |
| `xShift` | int | X offset (blocks). |
| `zShift` | int | Z offset (blocks). |

Live example (`iris/pack-base/dimensions/overworld.json`):

```json
"importedStructures": {
  "datapackOverrides": true,
  "mode": "ALL_ON",
  "disabled": [],
  "adjustments": [
    { "match": ["minecraft:stronghold"], "yShift": -64 }
  ]
}
```

---

## `IrisStructurePlacement` fields

One entry in a `structures[]` array. Decides *where* and *how often* one or more
`IrisStructure` keys are placed.

| Field | Type | Purpose |
|-------|------|---------|
| `structures` | `KList<String>` | Structure keys (file names under `structures/`, no extension) to place; one is chosen per site. |
| `distribution` | `StructureDistribution` | Placement grid strategy (see enum). |
| `spacing` | int | Average chunk spacing between structure sites (grid distribution). |
| `separation` | int | Minimum chunk separation between sites (must be `< spacing`). |
| `salt` | int | Random salt; change to decouple two placements that share spacing/separation. |
| `density` | double | Placement density (used by the `DENSITY` distribution). |
| `ringCount` | int | Number of rings (`CONCENTRIC_RINGS`). |
| `ringDistance` | int | Distance between rings (`CONCENTRIC_RINGS`). |
| `ringSpread` | int | How many sites are spread around each ring (`CONCENTRIC_RINGS`). |
| `rotation` | `IrisObjectRotation` | Rotation applied to the placed structure. |
| `translate` | `IrisObjectTranslate` | Translation offset applied to the structure. |
| `scale` | `IrisObjectScale` | Scaling applied to the structure. |
| `minHeight` | int | Minimum world-Y for placement. |
| `maxHeight` | int | Maximum world-Y for placement. |
| `underground` | boolean | Mark the placement as underground (depth-anchored). |
| `bore` | boolean | Carve a cavity to fit the structure before placing. |
| `borePadding` | int | Extra padding around the bore cavity. |
| `overbore` | boolean | Aggressively clear a box around the structure. |
| `overboreRadius` | int | Horizontal radius of the overbore box. |
| `overboreHeight` | int | Height of the overbore box. |
| `overboreFloor` | int | Floor offset of the overbore box. |
| `underwater` | boolean | Allow/expect the placement underwater. |

### `StructureDistribution`

| Value | Meaning |
|-------|---------|
| `RANDOM_SPREAD` | Vanilla-style spacing/separation grid with random jitter. |
| `DENSITY` | Noise/`density`-driven scatter. |
| `CONCENTRIC_RINGS` | Rings around origin (stronghold-style); uses `ring*` fields. |

Live example (`iris/pack-base/dimensions/overworld.json` -- ancient city placement):

```json
"structures": [
  {
    "structures": ["minecraft_ancient_city"],
    "underground": true,
    "overbore": true,
    "overboreRadius": 6,
    "overboreHeight": 10,
    "overboreFloor": 0,
    "minHeight": -220,
    "maxHeight": -220,
    "distribution": "RANDOM_SPREAD",
    "spacing": 64,
    "separation": 5,
    "salt": 42069
  }
]
```

---

## `IrisStructure` fields (the `structures/` folder)

Defines one assembled structure. Each file's name (minus `.json`) is the key used in
`IrisStructurePlacement.structures`.

| Field | Type | Purpose |
|-------|------|---------|
| `startPool` | String | Key of the `IrisJigsawPool` that seeds assembly. |
| `maxDepth` | int | Maximum jigsaw recursion depth from the start pool. |
| `maxSizeChunks` | int | Bounding box cap, in chunks. |
| `placeMode` | `ObjectPlaceMode` | How the assembled structure anchors to terrain (see enum). |
| `edit` | `KList<IrisObjectReplace>` | Block find/replace edits applied after assembly. |
| `loot` | `KList<String>` | Loot table keys injected into the structure's containers. |
| `vanillaSource` | String | Optional link to a vanilla structure key (re-creations / hybrids). |

Live example (`iris/pack-base/structures/ruined_portal.json`):

```json
{
  "startPool": "ruined_portal",
  "maxDepth": 1,
  "maxSizeChunks": 2,
  "placeMode": "CENTER_HEIGHT",
  "vanillaSource": "minecraft:ruined_portal"
}
```

### `ObjectPlaceMode` (anchor modes)

`CENTER_HEIGHT`, `MAX_HEIGHT`, `FAST_MAX_HEIGHT`, `MIN_HEIGHT`, `FAST_MIN_HEIGHT`,
`STILT`, `FAST_STILT`, `MIN_STILT`, `FAST_MIN_STILT`, `CENTER_STILT`, `ERODE_STILT`,
`ORGANIC_STILT`, `CEILING_HANG`, `VACUUM`, `VACUUM_HIGH`, `VACUUM_FAST`,
`VACUUM_ORGANIC`, `PAINT`, `FLOATING`, `STRUCTURE_PIECE`.

`STRUCTURE_PIECE` is the mode used by vanilla-recreation jigsaw structures (e.g.
`minecraft_village_plains.json`), where each piece anchors independently rather than the
whole structure to a single height.

---

## Jigsaw graph: pools and pieces

An `IrisStructure` assembles by walking a jigsaw graph starting at `startPool`.

### `IrisJigsawPool` (the `jigsaw-pools/` folder)

| Field | Type | Purpose |
|-------|------|---------|
| `pieces` | `KList<IrisJigsawPieceEntry>` | Weighted list of candidate pieces for this pool. |
| `fallback` | String | Pool key used when a connector cannot be satisfied (often an empty piece pool). |

#### `IrisJigsawPieceEntry`

| Field | Type | Purpose |
|-------|------|---------|
| `piece` | String | Key of an `IrisJigsawPiece`. |
| `weight` | int | Selection weight relative to siblings. |

### `IrisJigsawPiece` (the `jigsaw-pieces/` folder)

| Field | Type | Purpose |
|-------|------|---------|
| `object` | String | `.iob` object key this piece places. |
| `connectors` | `KList<IrisJigsawConnector>` | Jigsaw connection points on this object. |
| `rotatable` | boolean | Whether the piece may be rotated when connecting. |

#### `IrisJigsawConnector`

| Field | Type | Purpose |
|-------|------|---------|
| `position` | `IrisPosition` | Local block position of the connector within the object. |
| `direction` | `IrisDirection` | Face the connector points out of. |
| `pool` | String | `IrisJigsawPool` key the connector draws the next piece from. |
| `name` | String | This connector's name. |
| `targetName` | String | Name a mating connector must have to connect here. |
| `joint` | `JigsawJoint` | `ROLLABLE` (free roll) or `ALIGNED` (fixed orientation). |

Live examples:

```json
// jigsaw-pools/iglootop.json
{ "pieces": [ { "piece": "iglootop", "weight": 1 } ] }

// jigsaw-pieces/empty.json
{ "object": "empty", "connectors": [], "rotatable": true }
```

---

## How to add a structure in v4 (recipes)

### A. Toggle / shift a vanilla structure

Edit the dimension's `importedStructures` (via a `pack-overlay` override of
`dimensions/overworld.json`):

- Disable one: add its key to `disabled`.
- Whitelist-only: set `mode: "CUSTOM"` and list keys in `enabled`.
- Move one: add an `adjustments` entry with `match` + `yShift`/`xShift`/`zShift`.

No new assets are required -- keys come from `structures/structure-index.json`.

### B. Place an existing Iris-native structure more/less often or in new biomes

Add an `IrisStructurePlacement` to the `structures` array of the target biome, region,
or dimension (override file under `pack-overlay`). Reference the structure key in the
placement's `structures` list and tune `distribution` + `spacing`/`separation`/`salt`
(or `density`, or the `ring*` fields).

### C. Author a brand-new Iris-native structure

1. Capture/build the `.iob` object(s) (see `IOB-FILE-FORMAT.md` and the object tooling).
2. Create `jigsaw-pieces/<name>.json` describing the object + its `connectors`.
3. Create `jigsaw-pools/<name>.json` listing the piece(s) with weights (and a
   `fallback` pool for open connectors).
4. Create `structures/<name>.json` with `startPool`, `maxDepth`, `maxSizeChunks`, and a
   suitable `placeMode`.
5. Add an `IrisStructurePlacement` referencing the new structure key to a biome/region/
   dimension `structures` array.

Rebuild `structure-index.json` (it is generated) so the new key is catalogued.

---

## Verifying placement

Per the guidelines, **do not boot the test server yourself** -- it cannot be driven
headlessly. When in-game verification is needed (e.g. `/iris pregen`, `/locate`, or
teleporting to a placed structure), deploy the pack, then pause and `ask_user` to boot
the server and report back. For pure parse validation, a headless "does the pack load"
check is acceptable if the user explicitly asks.

---

## Migration note: legacy `jigsaw-structures/` is NOT loaded by v4

The pack still contains an `iris/pack-overlay/jigsaw-structures/` folder, and several
regions/biomes/dimensions carry a `jigsawStructures: [{ "structure", "rarity" }]`
array. **Neither is part of the Iris 4.0 schema.** `IrisData` in `Iris.jar` registers
loaders only for `IrisStructure` (`structures/`), `IrisJigsawPool` (`jigsaw-pools/`),
and `IrisJigsawPiece` (`jigsaw-pieces/`) -- there is no `IrisJigsawStructure` class or
loader, and `IrisBiome`/`IrisRegion`/`IrisDimension` expose a `structures`
(`KList<IrisStructurePlacement>`) field, not `jigsawStructures`.

Consequences:

- Entries in `jigsaw-structures/*.json` and any `jigsawStructures` arrays are ignored at
  runtime under v4 (the `structure`/`rarity` shape does not match
  `IrisStructurePlacement`).
- To make those custom structures generate under v4 they must be re-expressed as
  `IrisStructure` + jigsaw pool/piece files and placed via `structures`
  (`IrisStructurePlacement`) on the relevant biome/region/dimension.
- Existing structure-research docs that describe biome/region `jigsawStructures`
  (`STRUCTURE-INTEGRATION-RESEARCH.md`, `STRUCTURE-REGION-VARIANTS.md`, etc.) capture
  the *intended assignments* but use the legacy schema; treat their rarity/biome tables
  as design intent to be re-targeted onto the v4 `structures` schema, not as
  ready-to-ship JSON.

This is the single biggest gotcha when "working with structures in Iris v4": use
`structures` + `IrisStructurePlacement`, not `jigsawStructures`.

### Done: Towns & Towers modded structures cut over (2026-06-08)

The private pack-patches overlay (17 T&T villages + 23 pillager outposts) has been
migrated off the legacy schema by `iris/scripts/migrate-legacy-structures-to-v4.py`:
each `jigsaw-structures/<key>.json` became a `structures/<key>.json` `IrisStructure`
(`startPool` resolved as the pool wrapping the legacy start piece - villages
`.../town_centers`, outposts `.../base_plate`; `placeMode: STRUCTURE_PIECE`; no
`vanillaSource`), and the biome `*.patch.json` `jigsawStructures` arrays became
`structures` `IrisStructurePlacement` arrays (villages spacing 32 / sep 8, outposts
spacing 48 / sep 12, deterministic per-structure `salt`). The emitter scripts
(`nbt_to_iris.py`, `import-tnt-villages.py`, `wire-tt-outposts.py`) and the static
validator (`jigsaw_validate.py`) now produce/read the v4 form. Our own
`pack-overlay/jigsaw-structures/*` (dungeons + mineshaft) are NOT yet migrated.
