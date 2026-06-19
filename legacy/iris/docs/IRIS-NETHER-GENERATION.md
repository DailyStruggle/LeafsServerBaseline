# Iris Nether Generation: Methodology and Research

Research and step-by-step methodology for authoring an Iris-generated Nether for this
server: how to build the Nether dimension, its regions, its biomes, and its structures.
Field names below are JSON keys verified by decompiling `Iris.jar`
(`art.arcane.iris.engine.object.IrisDimension`, `IrisDimensionTypeOptions`,
`IrisRegion`, `IrisBiome`, and the structure classes documented in
[`IRIS-V4-STRUCTURES.md`](IRIS-V4-STRUCTURES.md)) and cross-checked against the live
overworld pack under `iris/pack-overlay`.

> Scope note: today this server runs Iris for the **overworld only**; the Nether and
> End use vanilla worldgen (see [`VANILLA-STRUCTURE-COVERAGE.md`](VANILLA-STRUCTURE-COVERAGE.md)).
> This document is the research/plan for *adding* an Iris Nether; it does not assume one
> exists yet.

> Layering contract (same as the overworld): never edit `iris/pack-base`. Add or edit
> the same-path file under `iris/pack-overlay` (whole-file override). See the project
> guidelines (*Iris Pack Layering*).

> Implementation status: an initial (un-wired) Nether pack has been authored under
> `iris/pack-overlay`:
> - dimension: `dimensions/nether.json`
> - regions: `regions/{nether-wastes,crimson,warped,soul-valley,basalt-deltas}.json`
> - biomes: `biomes/nether/{nether-wastes,crimson-forest,warped-forest,soul-sand-valley,basalt-deltas}.json`
> - spawners: `spawners/nether/hostile.json`
>
> NOT done yet (deliberately): generalizing `deploy-iris-pack.ps1` for a second
> pack/world and binding `world_nether` to Iris server-side (see section 5). The pack
> is content-only until those steps are taken.

---

## TL;DR

1. Iris generates **per world**. The Nether is a separate world (`world_nether`) that
   must be explicitly bound to an Iris dimension; an Iris dimension declares its kind
   through `environment` (`org.bukkit.World.Environment`: `NORMAL` / `NETHER` / `THE_END`).
2. The authoring pieces are the same four layers as the overworld, just retuned for the
   Nether:
   - a **dimension** file (`environment: "NETHER"`, capped height, lava sea, ceiling),
   - **regions** (groups of biomes with placement weights),
   - **biomes** (terrain generators + block layers + decorators),
   - **structures** (Iris-native jigsaw structures and/or toggled vanilla ones).
3. Iris does **not** import vanilla Nether terrain or biomes for free. Nether wastes,
   crimson/warped forests, basalt deltas, soul sand valley, fortresses, and bastions
   must be **authored or re-created** (just like the overworld biomes here were).
4. The binding of `world_nether` to Iris is a **server-side** step (bukkit.yml generator
   / Iris world setup), not a repo file.

---

## 1. The Nether dimension file

Path: `iris/pack-overlay/dimensions/nether.json` (new file; the overworld lives at
`dimensions/overworld.json`). The dimension is the root of the pack graph: it lists the
`regions`, the height envelope, the fluid (lava) sea, and the dimension-type options
that make the world *behave* like the Nether on the client and server.

### Nether-relevant `IrisDimension` fields (verified)

| Field | Type | Nether usage |
|---|---|---|
| `name` | string | Display name, e.g. `"Nether"`. |
| `environment` | `World.Environment` | **`"NETHER"`** - drives client fog/render and server dimension behavior. |
| `dimensionHeight` | `IrisRange {min,max}` | Nether is classically `{ "min": 0, "max": 128 }`; can be expanded but keep a ceiling. |
| `logicalHeight` | int | Player build/teleport ceiling (vanilla Nether = 128). |
| `fluidHeight` | int | Lava sea level (the overworld uses this for water at 75). For Nether set the lava lake level. |
| `fluidPalette` | `IrisMaterialPalette` | Block(s) used for the dimension fluid body - `minecraft:lava` for the Nether. |
| `rockPalette` | `IrisMaterialPalette` | Default rock fill - `minecraft:netherrack` (+ blackstone/basalt mixes). |
| `bedrock` | boolean | Generate bedrock shell. The Nether has bedrock floor **and** ceiling. |
| `caveLavaHeight` | int | Y level for lava in carved caves. |
| `fullbright` | boolean | Force full block light (Nether-style ambient brightness) if desired. |
| `regions` | `KList<String>` | The Nether region keys (see section 2). |
| `caveBiomeStyle` / `landBiomeStyle` / `regionStyle` / etc. | `IrisGeneratorStyle` | Noise styles that arrange regions/biomes; reuse the overworld patterns and retune zoom. |
| `ores` / `deposits` | ore generators | Nether quartz, nether gold ore, ancient debris, glowstone clusters. |
| `structures` | `KList<IrisStructurePlacement>` | Dimension-wide Iris structure placement (section 4). |
| `importedStructures` | `IrisImportedStructureControl` | Toggle/shift vanilla + datapack structures dimension-wide (section 4). |
| `dimensionOptions` | `IrisDimensionTypeOptions` | The vanilla `dimension_type` knobs - see next table. |

### `IrisDimensionTypeOptions` (the vanilla dimension-type knobs, verified)

These map 1:1 to a vanilla `dimension_type`. Setting them is what makes the world feel
like the Nether rather than a dark overworld.

| Field | Type | Nether value (vanilla parity) |
|---|---|---|
| `ultrawarm` | TriState | `TRUE` - water evaporates, lava flows faster. |
| `natural` | TriState | `FALSE` - no natural portals/sleeping, compasses spin. |
| `piglinSafe` | TriState | `TRUE` - piglins do not zombify. |
| `respawnAnchorWorks` | TriState | `TRUE`. |
| `bedWorks` | TriState | `FALSE` - beds explode. |
| `raids` | TriState | `FALSE`. |
| `skylight` | TriState | `FALSE` - no sky light. |
| `ceiling` | TriState | `TRUE` - has a solid ceiling. |
| `ambientLight` | float | `0.1` (vanilla Nether ambient glow). |
| `fixedTime` | Long | `18000` (constant Nether lighting), optional. |
| `monsterSpawnBlockLightLimit` | int | Nether allows spawns at higher light; tune to taste. |
| `coordinateScale` | double | `8.0` for vanilla 8:1 overworld<->nether portal scaling. |
| `cloudHeight` | Integer | Usually unset for the Nether. |

> TriState is `TRUE` / `FALSE` / `DEFAULT`. Leave a field at `DEFAULT` to inherit the
> engine default; set it explicitly for the values above to guarantee Nether behavior.

### Minimal dimension skeleton

```json
{
  "name": "Nether",
  "environment": "NETHER",
  "dimensionHeight": { "min": 0, "max": 128 },
  "logicalHeight": 128,
  "fluidHeight": 32,
  "fluidPalette": { "palette": [{ "block": "minecraft:lava" }] },
  "rockPalette":  { "palette": [{ "block": "minecraft:netherrack" }] },
  "bedrock": true,
  "fullbright": false,
  "dimensionOptions": {
    "ultrawarm": "TRUE",
    "natural": "FALSE",
    "piglinSafe": "TRUE",
    "bedWorks": "FALSE",
    "respawnAnchorWorks": "TRUE",
    "skylight": "FALSE",
    "ceiling": "TRUE",
    "ambientLight": 0.1,
    "coordinateScale": 8.0
  },
  "regions": [ "nether-wastes", "crimson", "warped", "soul-valley", "basalt-deltas" ],
  "importedStructures": { "mode": "ALL_OFF" },
  "structures": []
}
```

---

## 2. Nether regions

Path: `iris/pack-overlay/regions/<name>.json`. A region is a weighted bucket of biomes
plus region-scoped structures and objects. The Nether dimension's `regions` array lists
the region file keys.

### Region fields used in this pack (from `regions/temperate.json`)

| Field | Type | Nether usage |
|---|---|---|
| `name` | string | Region display name. |
| `color` | hex | Studio map color. |
| `rarity` | int | Relative frequency vs other regions (1 = most common). |
| `landBiomes` | `KList<String>` | Biome keys placed as the region's primary terrain. In the Nether all biomes are "land" (there is no sea); put every Nether biome here. |
| `caveBiomes` | `KList<String>` | Biomes for carved cave volumes (optional in the Nether). |
| `jigsawStructures` | array of `{structure, rarity}` | Legacy-style structure placement by region (see migration note in [`IRIS-V4-STRUCTURES.md`](IRIS-V4-STRUCTURES.md)). |
| `structures` | `KList<IrisStructurePlacement>` | v4 Iris-native structure placement scoped to the region. |
| `objects` | array | Region-wide scattered objects (e.g. lava columns, fungus clusters). |

### Region design for the Nether

Map vanilla Nether biome families to regions so the noise can separate them:

| Region | Biomes it groups | Notes |
|---|---|---|
| `nether-wastes` | nether wastes variants | The default filler; highest `rarity` weight. |
| `crimson` | crimson forest (+ variants) | Red fungal forest, weeping vines. |
| `warped` | warped forest (+ variants) | Cyan fungal forest, endermen-friendly. |
| `soul-valley` | soul sand valley | Soul sand/soil floor, fossils, blue fire. |
| `basalt-deltas` | basalt deltas | Basalt columns, blackstone, magma. |

`rarity` controls how much of the world each region occupies. Seasonal/blended region
tricks from the overworld (`regionStyle` fracture noise in `dimensions/overworld.json`)
transfer directly - retune `regionZoom` for the desired patch size.

---

## 3. Nether biomes

Path: `iris/pack-overlay/biomes/<category>/<name>.json`. A biome defines client
appearance (`derivative`), server logic biome (`vanillaDerivative`), terrain
(`generators`), block stacking (`layers` / `palette`), and surface features
(`decorators`). See [`CUSTOM-BIOMES.md`](CUSTOM-BIOMES.md) for the full field reference;
the Nether-specific guidance follows.

### Critical: derivative mapping for vanilla clients

The single most important Nether-specific rule is the derivative pair (see the
"Vanilla Client Compatibility" and "Biome JSON Schema" sections of
[`CUSTOM-BIOMES.md`](CUSTOM-BIOMES.md)):

- `derivative` - the vanilla biome **sent to the client**. For Nether biomes this MUST
  be a Nether biome (`minecraft:nether_wastes`, `minecraft:crimson_forest`,
  `minecraft:warped_forest`, `minecraft:soul_sand_valley`, `minecraft:basalt_deltas`),
  otherwise the client renders overworld fog/sky and the illusion breaks.
- `vanillaDerivative` - the vanilla biome used for **server-side logic** (mob spawns,
  fog, music). Keep it a Nether biome too so spawn tables and ambience match.

> The overworld pack sometimes points `derivative` at a *visually* matching overworld
> biome while keeping a different `vanillaDerivative` (e.g. `mesa/cinnabar-mesa` uses
> `minecraft:nether_wastes` for atmosphere). For an actual Nether dimension, both keys
> should be Nether biomes.

### Terrain: `generators` and `layers`

- `generators` reference height/noise generator files (e.g. `"generator": "plain"` as in
  `biomes/vanilla/puddle.json`), with `min`/`max` height bands. Nether biomes are
  bounded by the dimension's `dimensionHeight` (0-128) and want low, rolling, cavernous
  terrain rather than tall mountains.
- `layers` / `palette` stack the surface blocks. Nether examples:
  - nether wastes: `minecraft:netherrack` fill.
  - soul sand valley: `minecraft:soul_sand` / `minecraft:soul_soil` surface over
    netherrack.
  - crimson: `minecraft:crimson_nylium` cap over netherrack.
  - warped: `minecraft:warped_nylium` cap.
  - basalt deltas: `minecraft:basalt` + `minecraft:blackstone` mix, magma pockets.

### Decorators (surface features)

`decorators` place the flora/props. Nether examples: crimson/warped fungi and roots,
weeping/twisting vines, nether wart blocks, shroomlights, glowstone clusters (often as
ceiling decorators), fire on soul soil. Use the same decorator schema as overworld
biomes; anchor ceiling-hanging features appropriately (the Nether has a roof).

### Spawns

Wire mob spawners with `entitySpawners` at biome or region level (piglins, hoglins,
zombified piglins, ghasts, magma cubes, striders, enderman in warped). Same mechanism as
the overworld biomes' `entitySpawners`.

---

## 4. Nether structures

Structures use the v4 model fully documented in [`IRIS-V4-STRUCTURES.md`](IRIS-V4-STRUCTURES.md).
Two independent paths:

### A. Vanilla / datapack structures (`importedStructures`)

The dimension's `importedStructures` (`IrisImportedStructureControl`) toggles real Mojang
structures. For the Nether the relevant keys are `minecraft:fortress`,
`minecraft:bastion_remnant`, `minecraft:nether_fossil`, and
`minecraft:ruined_portal_nether`.

- Easiest parity path: `"mode": "ALL_ON"` and let vanilla fortresses/bastions generate,
  optionally nudged with `adjustments` (`yShift`/`xShift`/`zShift`).
- Full custom path: `"mode": "ALL_OFF"` and re-create them as Iris-native jigsaw
  structures (path B), or `"mode": "CUSTOM"` with an explicit `enabled` list.

> Caveat: whether a given vanilla Nether structure honors Iris's custom biomes/terrain
> depends on its biome-tag placement; verify in-game (server boot required).

### B. Iris-native jigsaw structures (`structures/` + `jigsaw-pools/` + `jigsaw-pieces/`)

Author a structure as an `IrisStructure` (in `structures/`) assembled from a jigsaw graph
(`startPool` -> `IrisJigsawPool` -> `IrisJigsawPiece` -> `.iob` object), then place it via
`IrisStructurePlacement` entries in the dimension/region/biome `structures` array. All
fields (`distribution`, `spacing`, `separation`, `placeMode`, `bore`/`overbore`, height
clamps) are in [`IRIS-V4-STRUCTURES.md`](IRIS-V4-STRUCTURES.md).

Nether-specific placement notes:

- Use `placeMode` `CEILING_HANG` for roof-anchored builds and the stilt/min-height modes
  for floor builds; `CENTER_HEIGHT` for free-standing.
- Clamp `minHeight`/`maxHeight` inside the 0-128 envelope; remember the bedrock ceiling.
- `bore`/`overbore` are useful to carve a pocket in dense netherrack before placing.
- Pipe Nether loot via the structure `loot` field (e.g. `minecraft:chests/nether_bridge`,
  `minecraft:chests/bastion_*`).

Custom Nether structure ideas that fit this server's bespoke style: soul-forge ruins,
basalt watchtowers, fungal villages, ancient-debris dig sites.

---

## 5. Build, deploy, and bind (workflow)

1. **Author** the four layers under `iris/pack-overlay` (dimension, regions, biomes,
   structures) per sections 1-4. Never touch `iris/pack-base`.
2. **Deploy** with `iris/scripts/deploy-iris-pack.ps1`. Note: the script currently hard-
   codes the overworld destination (`...\plugins\Iris\packs\overworld`) and clears the
   per-dimension jigsaw `prefetch`. Adding a second world/pack requires generalizing the
   destination(s) and ensuring the Nether dimension's prefetch/caches are cleared too.
3. **Bind `world_nether` to Iris** (server-side, not in this repo): point the nether
   world's generator at the Iris pack/dimension (via `bukkit.yml` `worlds:` generator
   entry, an Iris world-creation command, or a world manager). This is the step that
   actually switches the Nether to Iris.
4. **Verify on a user-triggered server boot** (per project guidelines, the agent does not
   boot the server): regenerate the nether, fly through each region, confirm client fog
   per biome (`derivative` mapping), structure placement, and spawns.

---

## 6. Risks and open questions

- **No free vanilla content.** Every Nether biome's terrain, palette, decorators, and
  spawns must be authored; only the four vanilla structures can be toggled on cheaply.
- **Deploy script is overworld-specific** and must be generalized for a second pack/world.
- **World binding is server-side** and lives outside this repository.
- **Biome-tag placement of imported structures** (fortress/bastion) against custom biomes
  needs in-game verification.
- **Folia/perf**: the deploy target is a Folia test server; validate Nether generation
  throughput and region threading.
- **Scope/docs drift**: adopting an Iris Nether contradicts the current documented
  "Nether = vanilla" stance in [`VANILLA-STRUCTURE-COVERAGE.md`](VANILLA-STRUCTURE-COVERAGE.md);
  that doc and an ADR should be updated when/if this is implemented.

---

## See Also

- [`IRIS-V4-STRUCTURES.md`](IRIS-V4-STRUCTURES.md) - full structure/jigsaw schema and placement fields.
- [`CUSTOM-BIOMES.md`](CUSTOM-BIOMES.md) - biome JSON schema, derivative mapping, palette reference.
- [`IRIS-BIOME-RESEARCH.md`](IRIS-BIOME-RESEARCH.md) - methodology for evaluating/adopting Iris biomes.
- [`VANILLA-STRUCTURE-COVERAGE.md`](VANILLA-STRUCTURE-COVERAGE.md) - current (vanilla) Nether/End scope statement.
- [`../world-design/IRIS-WORLD-BUILDING.md`](../world-design/IRIS-WORLD-BUILDING.md) - end-to-end overworld authoring walkthrough (same workflow, retuned here for the Nether).
- Project guidelines (*Iris Pack Layering*, *Server Boots Are User-Triggered*).
