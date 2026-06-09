# Iris End Generation: Methodology and Research

Research and step-by-step methodology for authoring an Iris-generated End for this
server: how to build the End dimension, its regions, its biomes, and its structures,
**including floating islands and End-city-style structures placed on them**. Field names
below are JSON keys verified by decompiling `Iris.jar` (the bundled fork
`art.arcane.iris.engine.object.*`: `IrisDimension`, `IrisDimensionTypeOptions`,
`IrisRegion`, `IrisBiome`, `IrisFloatingChildBiomes`, `IrisStructure`,
`IrisStructurePlacement`, `ObjectPlaceMode`) and cross-checked against the live overworld
pack under `iris/pack-overlay`.

> Concept (from the issue): we use a custom **central End island** and let players travel
> outward into a field of **floating islands** (the "outer End"). Iris's
> `floatingChildBiomes` feature is the engine-native way to build that outer-island field.

> Scope note: today this server runs Iris for the **overworld only**; the End currently
> uses vanilla worldgen (see [`VANILLA-STRUCTURE-COVERAGE.md`](VANILLA-STRUCTURE-COVERAGE.md)).
> This document is the research/plan for *adding* an Iris End; it does not assume one
> exists yet.

> Layering contract (same as the overworld): never edit `iris/pack-base`. Add or edit the
> same-path file under `iris/pack-overlay` (whole-file override). See the project
> guidelines (*Iris Pack Layering*).

---

## TL;DR

1. Iris generates **per world**. The End is a separate world (`world_the_end`) bound to an
   Iris dimension whose `environment` is `THE_END`
   (`org.bukkit.World.Environment` -> `IDataFixer.Dimension.END`, verified in
   `IrisDimension.getBaseDimension()`).
2. Same four authoring layers as the overworld, retuned for the End:
   - a **dimension** file (`environment: "THE_END"`, no sky light, End dimension-type),
   - **regions** (weighted buckets of biomes),
   - **biomes** (the central island terrain + the floating-island biomes),
   - **structures** (Iris-native jigsaw structures placed with floating place-modes).
3. **Floating islands are a first-class biome feature**, not a hack: `IrisBiome` exposes
   `floatingChildBiomes` (`KList<IrisFloatingChildBiomes>`) which scatters whole
   sky-island biomes above a base biome's surface with full control over footprint,
   altitude, top/bottom shape, carving, and per-island objects.
4. **End-city-style structures** are authored as Iris jigsaw `IrisStructure`s and placed
   with `IrisStructurePlacement` using a floating `ObjectPlaceMode`
   (`FLOATING` / `VACUUM*` / `STRUCTURE_PIECE`), or attached directly to a floating-island
   biome via its `floatingObjects` / object overrides.
5. Iris does **not** import vanilla End terrain, the dragon fight, exit portal, gateways,
   or End cities/ships for free (no `Dragon`/`EndCity`/`Portal` classes exist in the jar).
   Those are vanilla/NMS mechanics or must be re-created as Iris content.
6. Binding `world_the_end` to Iris is a **server-side** step, not a repo file.

---

## 1. The End dimension file

Path: `iris/pack-overlay/dimensions/the-end.json` (new file; the overworld lives at
`dimensions/overworld.json`). The dimension is the root of the pack graph: it lists the
`regions`, the height envelope, and the dimension-type options that make the world
*behave* like the End on the client and server.

### End-relevant `IrisDimension` fields (verified)

| Field | Type | End usage |
|---|---|---|
| `name` | string | Display name, e.g. `"The End"`. |
| `environment` | `World.Environment` | **`"THE_END"`** - maps to `Dimension.END`; drives client sky/fog and server behavior. |
| `dimensionHeight` | `IrisRange {min,max}` | End sits around `{ "min": 0, "max": 256 }`; islands float well above the base surface. |
| `logicalHeight` | int | Player build/teleport ceiling. |
| `fluidHeight` | int | Sea level. The End has no sea; set low (e.g. 0) so nothing floods. |
| `landChance` | double | Push toward 1.0 for the solid central island, or lower to leave void between landmasses. |
| `bedrock` | boolean | Usually `false` for the End (no bedrock shell). |
| `regions` | `KList<String>` | The End region keys (see section 2). |
| `regionStyle` / `landBiomeStyle` / `continentalStyle` | `IrisGeneratorStyle` | Noise that arranges regions/biomes; reuse overworld patterns, retune zoom. |
| `structures` | `KList<IrisStructurePlacement>` | Dimension-wide Iris structure placement (section 4). |
| `dimensionOptions` | `IrisDimensionTypeOptions` | The vanilla `dimension_type` knobs (next table). |

### `IrisDimensionTypeOptions` (End parity, verified)

These map 1:1 to a vanilla `dimension_type`.

| Field | TriState/value | End value |
|---|---|---|
| `natural` | TriState | `FALSE` - no natural portals/sleeping. |
| `bedWorks` | TriState | `FALSE` - beds explode. |
| `respawnAnchorWorks` | TriState | `FALSE`. |
| `skylight` | TriState | `FALSE` - dim End ambience. |
| `ceiling` | TriState | `FALSE`. |
| `raids` | TriState | `FALSE`. |
| `ultrawarm` | TriState | `FALSE`. |
| `piglinSafe` | TriState | `FALSE`. |
| `ambientLight` | float | `0.0` (vanilla End). |
| `fixedTime` | Long | `6000` to mimic the End's constant lighting (optional). |
| `coordinateScale` | double | `1.0`. |

> TriState is `TRUE` / `FALSE` / `DEFAULT`. Leave at `DEFAULT` to inherit the engine
> default; set the values above explicitly to guarantee End behavior.

### Minimal dimension skeleton

```json
{
  "name": "The End",
  "environment": "THE_END",
  "dimensionHeight": { "min": 0, "max": 256 },
  "logicalHeight": 256,
  "fluidHeight": 0,
  "bedrock": false,
  "dimensionOptions": {
    "natural": "FALSE",
    "bedWorks": "FALSE",
    "respawnAnchorWorks": "FALSE",
    "skylight": "FALSE",
    "ceiling": "FALSE",
    "ambientLight": 0.0,
    "coordinateScale": 1.0
  },
  "regions": [ "end-central", "end-outer-islands" ],
  "structures": []
}
```

---

## 2. End regions

Path: `iris/pack-overlay/regions/<name>.json`. A region is a weighted bucket of biomes
plus region-scoped structures/objects. The dimension's `regions` array lists the region
keys.

| Region | Biomes it groups | Notes |
|---|---|---|
| `end-central` | the solid main island biome | The spawn/landing island; high `rarity`/`landChance`. |
| `end-outer-islands` | the base "void/sky" biome that hosts `floatingChildBiomes` | The outer End field; its base surface is mostly void, the content is the floating islands above it. |

Region fields (verified on `IrisRegion`): `name`, `color`, `rarity`, `landBiomes`,
`caveBiomes`, `objects` (`KList<IrisObjectPlacement>`), `structures`
(`KList<IrisStructurePlacement>`), plus the legacy `jigsawStructures` path (see
[`IRIS-V4-STRUCTURES.md`](IRIS-V4-STRUCTURES.md)).

---

## 3. End biomes (and floating islands)

Path: `iris/pack-overlay/biomes/<category>/<name>.json`. See
[`CUSTOM-BIOMES.md`](CUSTOM-BIOMES.md) for the full field reference; End-specific guidance
follows.

### Derivative mapping for vanilla clients

- `derivative` - the vanilla biome **sent to the client**. For End biomes use an End biome
  (`minecraft:the_end`, `minecraft:end_highlands`, `minecraft:end_midlands`,
  `minecraft:small_end_islands`, `minecraft:end_barrens`) so the client renders the End
  sky/fog. Using `minecraft:the_end` also gives the eerie sky color and ambient sound.
- `vanillaDerivative` - the vanilla biome for **server-side logic** (spawns, fog, music);
  keep it an End biome too.

### Terrain and palette

- `generators` (`IrisBiomeGeneratorLink`: `generator`, `min`, `max`) shape the central
  island's height; outer base biomes can be near-flat or void.
- `layers` / `palette` stack surface blocks: `minecraft:end_stone` fill, optional
  chorus-plant decorators.

### Floating islands: `floatingChildBiomes` (the key feature)

`IrisBiome.floatingChildBiomes` is `KList<IrisFloatingChildBiomes>` plus a
`mergeFloatingChildBiomes` boolean. Attach it to the **base** outer biome; each entry
generates a population of sky islands of another biome above the surface. Verified
`IrisFloatingChildBiomes` fields:

| Field | Type | Purpose |
|---|---|---|
| `biome` | string | The biome key rendered **on** the floating island. |
| `rarity` | int | Relative frequency of this island type. |
| `footprintStyle` / `footprintThreshold` | style / double | Noise + cutoff deciding **where** islands appear (their plan-view shape/spread). |
| `pickerStyle` | style | Noise that picks between multiple island variants. |
| `altitudeStyle` | style | Noise driving per-island floating height. |
| `minHeightAboveSurface` / `maxHeightAboveSurface` | int | Island altitude band relative to the base surface. |
| `minAbsoluteY` / `maxAbsoluteY` | Integer | Hard world-Y clamps (optional). |
| `topShapeMode` | `TopShapeMode` | `BIOME` / `NOISE` / `FLAT` - how the island top is shaped. |
| `maxTopHeight` / `topShapeStyle` / `topShapeAmp` | int / style / double | Top-surface relief. |
| `bottomStyle` / `bottomDepthMin` / `bottomDepthMax` / `bottomExponent` | style / int / double | The tapered underside (the classic End-island point). |
| `bottomPaletteMode` | `FloatingBottomPaletteMode` | `DEPTH` / `MIRROR_TOP` / `CUSTOM` underside blocks. |
| `bottomPalette` | `KList<IrisBiomePaletteLayer>` | Custom underside blocks when `CUSTOM`. |
| `maxThickness` | int | Cap on island vertical thickness. |
| `wallWarpStyle` / `wallWarpAmplitude` | style / double | Edge warping so islands are not cylinders. |
| `carveStyle` / `carving` / `carveThreshold` | style / string / double | Optional cave/hole carving inside an island. |
| `localFluidHeight` / `fluidBlock` | Integer / string | Optional pool on top of an island. |
| `inheritDecorators` / `inheritObjects` | boolean | Reuse the child biome's decorators/objects. |
| `objectShrinkFactor` | double | Scale objects down to fit small islands. |
| `extraObjects` / `floatingObjects` | `KList<IrisObjectPlacement>` | Extra objects, and **objects placed specifically on the floating island** (e.g. towers, ruins). |
| `topObjectMode` / `bottomObjectMode` | `OverrideMode` | `INHERIT_ONLY` / `MERGE` / `REPLACE` for top/bottom object sets. |
| `topObjectOverrides` / `bottomObjectOverrides` | `KList<IrisObjectPlacement>` | Explicit object placements for the top and underside. |
| `color` | string | Studio map color. |

This single feature reproduces the vanilla outer-End look (scattered tapered islands) and
is also where you hang structures/objects directly onto islands via `floatingObjects` /
`topObjectOverrides`.

---

## 4. End structures (including End cities on floating islands)

Structures use the v4 model fully documented in [`IRIS-V4-STRUCTURES.md`](IRIS-V4-STRUCTURES.md).
There are two complementary ways to land an End-city-style build on a floating island.

### A. Object/structure attached to the floating biome (recommended for on-island builds)

Place the build through the floating biome itself so it follows the island:

- Put the `.iob` build into the floating biome's `objects`, or better into the
  `IrisFloatingChildBiomes` `floatingObjects` / `topObjectOverrides` so it is anchored to
  the island top and scaled by `objectShrinkFactor`.
- Use `objectShrinkFactor` / `IrisObjectScale` so a big city footprint fits the island.

### B. Jigsaw `IrisStructure` placed with a floating place-mode

Author an End city as an `IrisStructure` (jigsaw graph: `startPool` ->
`IrisJigsawPool` -> `IrisJigsawPiece` -> `.iob`) and place it with an
`IrisStructurePlacement`. Verified placement controls:

- `IrisStructure`: `startPool`, `maxDepth`, `maxSizeChunks`, `placeMode`
  (`ObjectPlaceMode`), `edit`, `loot`, `vanillaSource`.
- `IrisStructurePlacement`: `structures`, `distribution`
  (`RANDOM_SPREAD` / `DENSITY` / `CONCENTRIC_RINGS`), `spacing`, `separation`, `salt`,
  `density`, `ringCount`/`ringDistance`/`ringSpread` (for ring distribution),
  `rotation`, `translate`, `scale`, `minHeight`, `maxHeight`, `underground`, `bore`,
  `borePadding`, `overbore`/`overboreRadius`/`overboreHeight`/`overboreFloor`,
  `underwater`.

**Floating-relevant `ObjectPlaceMode` values (verified):**

| Mode | Effect for End builds |
|---|---|
| `FLOATING` | Place at the object's own Y with no surface snapping - good for free sky structures. |
| `VACUUM` / `VACUUM_HIGH` / `VACUUM_FAST` / `VACUUM_ORGANIC` | Place into open air pockets / above terrain (sky placement) without needing solid ground. |
| `CEILING_HANG` | Hang a build under an island (e.g. a ship moored beneath). |
| `STRUCTURE_PIECE` | Marks the object as a jigsaw-assembled structure piece. |
| `CENTER_HEIGHT` / `MAX_HEIGHT` / `MIN_HEIGHT` / `*_STILT` | Surface-relative modes - use only for the solid central island. |

To scatter End cities across the outer islands, add an `IrisStructurePlacement` (with
`placeMode: FLOATING` or a `VACUUM*` mode and `minHeight`/`maxHeight` matching the island
altitude band) to the `end-outer-islands` region or the base outer biome's `structures`.
For deterministic spacing like vanilla, use `distribution: RANDOM_SPREAD` with
`spacing`/`separation`, or `CONCENTRIC_RINGS` to mimic the ring of outer islands.

> No vanilla End city/ship is provided by Iris (no `EndCity` class in the jar). Re-create
> them as `.iob` objects/jigsaw pieces, and pipe loot via the structure/object `loot`
> field (e.g. `minecraft:chests/end_city_treasure`).

---

## 5. Build, deploy, and bind (workflow)

1. **Author** the four layers under `iris/pack-overlay` (dimension, regions, biomes with
   `floatingChildBiomes`, structures) per sections 1-4. Never touch `iris/pack-base`.
2. **Deploy** with `iris/scripts/deploy-iris-pack.ps1`. Note: the script currently hard-
   codes the overworld destination (`...\plugins\Iris\packs\overworld`); adding a second
   world/pack requires generalizing the destination(s) and clearing the End dimension's
   prefetch/caches too.
3. **Bind `world_the_end` to Iris** (server-side, not in this repo): point the End world's
   generator at the Iris pack/dimension (via `bukkit.yml` `worlds:` generator entry, an
   Iris world-creation command, or a world manager).
4. **Verify on a user-triggered server boot** (per project guidelines, the agent does not
   boot the server): regenerate the End, fly the central island and the outer-island
   field, confirm client sky/fog (`derivative` mapping), floating-island shapes, and
   structure placement.

---

## 6. Risks and open questions

- **Dragon fight / exit portal / gateways are vanilla-only.** A pure-Iris End loses them
  unless you keep the vanilla central-island mechanics or re-implement via a plugin. The
  issue's "custom central island + travel outward" model needs a decision on how the
  player leaves the spawn platform.
- **No free vanilla End content.** Terrain, palettes, End cities/ships, and chorus growth
  must be authored as Iris content.
- **Deploy script is overworld-specific** and must be generalized for a second pack/world.
- **World binding is server-side** and lives outside this repository.
- **Performance**: large `floatingChildBiomes` populations add noise sampling per column;
  validate generation throughput on the (Folia) test server.
- **Scope/docs drift**: adopting an Iris End contradicts the current "End = vanilla"
  stance in [`VANILLA-STRUCTURE-COVERAGE.md`](VANILLA-STRUCTURE-COVERAGE.md); that doc and
  an ADR should be updated when/if this is implemented.

---

## See Also

- [`IRIS-NETHER-GENERATION.md`](IRIS-NETHER-GENERATION.md) - sibling research for an Iris Nether (same four-layer workflow).
- [`IRIS-V4-STRUCTURES.md`](IRIS-V4-STRUCTURES.md) - full structure/jigsaw schema and placement fields.
- [`CUSTOM-BIOMES.md`](CUSTOM-BIOMES.md) - biome JSON schema, derivative mapping, palette reference.
- [`VANILLA-STRUCTURE-COVERAGE.md`](VANILLA-STRUCTURE-COVERAGE.md) - current (vanilla) Nether/End scope statement.
- Project guidelines (*Iris Pack Layering*, *Server Boots Are User-Triggered*).
