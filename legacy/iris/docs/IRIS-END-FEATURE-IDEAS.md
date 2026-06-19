# Iris End: Feature Ideas from Existing End Generators

Research companion to [`IRIS-END-GENERATION.md`](IRIS-END-GENERATION.md). That doc covers
*how* to author an Iris End (dimension, regions, biomes, floating islands, structures).
This doc surveys popular **plugin/mod/datapack End generators** and distills concrete
**ideas worth adding**, each mapped to the Iris feature that would implement it.

> Scope note: this is research only. Nothing here is implemented; the server still runs
> Iris for the overworld only and a vanilla End. Restricted-source assets are never
> redistributed (see ADR-004); only ideas/mechanics are borrowed, not files.

---

## Sources surveyed

| Source | Type | What it does to the End | Notes |
|---|---|---|---|
| **BetterEnd** (Fabric) | Mod | Custom End generator: islands of varied shape/height, internal caves with resources, many biomes, custom sky (purple nebulas/stars), per-biome music/sound, rituals, mobs, central-island structures. | Richest reference for biome variety and island shaping. |
| **Endercon** | Datapack | End revamp focused on level design/traversal/variety; per-biome fog, a subtle ~14 min day/night cycle (dimmer "night", End flashes dim), creaking/bee behavior. | Datapack-only, so its tricks are achievable without NMS. |
| **The Outer End** | Datapack/mod | New outer-End biomes with surreal terrain; vanilla-friendly. | Good "outer islands" biome palette inspiration. |
| **Endless Biomes / Unusual End** | Datapack/mod | Additional outer-End biomes, more terrain variety. | Pick-one-of style; avoid stacking. |
| **Vanilla End** (baseline) | - | Central island + dragon + gateways, outer ring of islands, End cities/ships, chorus. | The mechanics Iris does NOT provide for free. |

---

## Idea backlog (mapped to Iris)

### Biome variety (the biggest win)
Borrow the *kinds* of outer-End biomes these mods popularised and author them as Iris
biomes (`biomes/end/*.json`) grouped under the `end-outer-islands` region:

- **Chorus forest** - dense chorus plants, purple palette. -> Iris biome with chorus
  decorators on `minecraft:end_highlands` `derivative`.
- **Amber / glowing fields** - bright emissive ground cover. -> palette + glow-block
  decorators; `derivative` an End biome to keep End sky.
- **Crystal / spire mountains** - tall jagged island tops. -> aggressive `topShapeStyle`
  + `topShapeAmp` on `floatingChildBiomes`.
- **Mushroom / fungal land** - End-flavoured fungal cover. -> decorator set.
- **Dust / barren wastes** - low-relief filler between richer biomes. -> `end_barrens`
  derivative, flat generator.

Implementation hook: each becomes an `IrisFloatingChildBiomes.biome` entry on the base
outer biome, with its own `rarity`, `footprintStyle`, and altitude band.

### Island shape variety
BetterEnd's selling point is islands of *different shapes and heights*, not uniform blobs.
Iris equivalents (all on `IrisFloatingChildBiomes`):

- Vary `minHeightAboveSurface`/`maxHeightAboveSurface` + `altitudeStyle` for layered
  altitude bands.
- `topShapeMode` (`BIOME`/`NOISE`/`FLAT`) + `wallWarpStyle`/`wallWarpAmplitude` so islands
  are not cylinders.
- `bottomStyle`/`bottomExponent`/`bottomDepthMax` for dramatic tapered undersides.
- `maxThickness` to mix thin shelves with thick masses.

### Internal island caves + resources
BetterEnd puts caves with unique resources *inside* islands. Iris hook: the
`carveStyle`/`carving`/`carveThreshold` fields on `IrisFloatingChildBiomes`, plus
dimension `ores`/`deposits` scoped to End materials.

### Atmosphere: per-biome fog + day/night feel
Endercon shows fog/ambience changes are datapack-doable. Iris hook:

- `derivative` choice already drives client sky/fog per biome - pick End biomes whose
  vanilla fog matches the mood.
- `IrisDimensionTypeOptions.fixedTime` / `ambientLight` for the constant-light End feel; a
  true day/night cycle would need a companion plugin (out of Iris scope, record as idea).

### Structures and loot variety
Vanilla gives only End city/ship. Borrow the *variety* idea (multiple structure
archetypes) and author them as Iris jigsaw `IrisStructure`s placed with floating
`ObjectPlaceMode` (`FLOATING`/`VACUUM*`/`CEILING_HANG`) - see
[`IRIS-END-GENERATION.md`](IRIS-END-GENERATION.md) section 4. Structure ideas: ruined
spires, hanging ships (`CEILING_HANG`), shattered platforms, ritual altars (BetterEnd
"rituals" flavour). Pipe loot via the structure `loot` field.

### Central-island identity
The issue's concept is a **custom central island** players leave outward from. BetterEnd
replaces the central island with custom structures. Iris hook: a dedicated `end-central`
region/biome with a hand-authored `.iob` spawn structure placed via a fixed
`IrisStructurePlacement`.

---

## What still cannot be borrowed (engine limits)

These appear in mods but rely on NMS / custom code, not worldgen, and Iris does not
provide them (no `Dragon`/`EndCity`/`Portal` classes in `Iris.jar`):

- Ender dragon fight, exit portal, and End gateways (vanilla/NMS only).
- New mobs, custom music/sound per biome, rituals, day/night cycle behaviour - these are
  mod code, not Iris worldgen. Capture as separate plugin work if desired.

---

## Suggested next step

If we pursue an Iris End, prioritise: (1) 4-6 outer biomes from the list above, (2)
island-shape variety via `floatingChildBiomes`, (3) 2-3 jigsaw structures with floating
place-modes. Defer atmosphere/day-night and dragon mechanics to a companion plugin and an
ADR, per the scope-drift note in [`IRIS-END-GENERATION.md`](IRIS-END-GENERATION.md).

---

## See Also

- [`IRIS-END-GENERATION.md`](IRIS-END-GENERATION.md) - how to author the Iris End (the implementation reference).
- [`IRIS-NETHER-GENERATION.md`](IRIS-NETHER-GENERATION.md) - sibling research for an Iris Nether.
- [`IRIS-V4-STRUCTURES.md`](IRIS-V4-STRUCTURES.md) - structure/jigsaw schema and placement fields.
- [`VANILLA-STRUCTURE-COVERAGE.md`](VANILLA-STRUCTURE-COVERAGE.md) - current (vanilla) End scope statement.
