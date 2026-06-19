# Iris v4.0 Carving / Cave Options Reference

Reference for the Iris 4.0 noise-field carving engine, derived by decompiling the
classes in `Iris.jar` (`art.arcane.iris.engine.object.*`,
`art.arcane.iris.engine.modifier.IrisCarveModifier`,
`art.arcane.iris.engine.mantle.components.IrisCaveCarver3D`). Field names below are the
JSON keys (the decompiled getters/setters map 1:1 to these keys).

Live examples in the pack: `iris/pack-base/biomes/carving/volcanic.json`,
`iris/pack-base/biomes/carving/glacial.json`, and the dimension default in
`iris/pack-base/dimensions/overworld.json`.

> Layering note: never edit `iris/pack-base`. To change carving, add/edit the
> same-path file under `iris/pack-overlay` (whole-file override).

---

## Where carving is configured

| Scope | Field / file | Type | Purpose |
|-------|--------------|------|---------|
| Dimension toggle | `carvingEnabled` | boolean | Master on/off for the v4 carver. |
| Dimension default carve | `caveProfile` | `IrisCaveProfile` | Default 3D noise carve applied dimension-wide. |
| Dimension carve scoping | `carving` | `KList<IrisDimensionCarvingEntry>` | Maps carving biomes to world-Y bands. |
| Dimension lava level | `caveLavaHeight` | int | World Y at/below which carved air floods with lava. |
| Upper-dimension carve | `upperDimensionCarving` | boolean | Whether the upper dimension is carved. |
| Per-biome override | `caveProfile` on a biome | `IrisCaveProfile` | Overrides the dimension carve inside that carving biome. |

---

## `IrisCaveProfile` fields

The core 3D density carve. All keys are optional in JSON (defaults come from the class).

### Enable / range

| Field | Type | Purpose |
|-------|------|---------|
| `enabled` | boolean | Enables this profile. |
| `verticalRange` | `IrisRange` (`min`/`max`) | World-Y span the carve operates in (observed up to ~700). |
| `verticalEdgeFade` | int | Vertical distance over which carve strength fades near the range edges. |
| `verticalEdgeFadeStrength` | double | How strongly the edge fade reduces carving. |

### Density noise

| Field | Type | Purpose |
|-------|------|---------|
| `baseDensityStyle` | `IrisGeneratorStyle` | Primary noise that shapes the void mass. |
| `detailDensityStyle` | `IrisGeneratorStyle` | Secondary high-frequency detail noise. |
| `warpStyle` | `IrisGeneratorStyle` | Domain-warp noise that distorts the field. |
| `baseWeight` | double | Weight of the base density noise. |
| `detailWeight` | double | Weight of the detail noise. |
| `warpStrength` | double | Strength of the domain warp. |
| `densityThreshold` | `IrisStyledRange` (`min`/`max`/`style`) | Threshold band (noise-modulated) that decides carved vs solid. |
| `thresholdBias` | double | Constant bias added to the threshold (more negative carves more). |

### Sampling / quality

| Field | Type | Purpose |
|-------|------|---------|
| `sampleStep` | int | Grid step for sampling the field (higher = faster, coarser). |
| `adaptiveSampling` | boolean | Enable adaptive (variable-resolution) sampling. |
| `adaptiveSampleStep` | int | Step used when adaptive sampling refines a region. |
| `adaptiveThresholdMargin` | double | Margin around the threshold that triggers adaptive refinement. |
| `minCarveCells` | int | Minimum carved cells for a pocket to be kept (culls tiny specks). |
| `recoveryThresholdBoost` | double | Threshold boost used to recover/reconnect under-carved areas. |

### Surface break (open to daylight)

| Field | Type | Purpose |
|-------|------|---------|
| `surfaceClearance` | int | Blocks of solid roof kept under the surface by default. |
| `allowSurfaceBreak` | boolean | Allow the carve to punch through to the surface. |
| `surfaceBreakStyle` | `IrisGeneratorStyle` | Noise that decides where surface breaks occur. |
| `surfaceBreakNoiseThreshold` | double | Threshold the break-noise must exceed to open a hole. |
| `surfaceBreakDepth` | int | How deep the surface break cuts in. |
| `surfaceBreakThresholdBoost` | double | Extra carve threshold applied within a surface-break zone. |

### Object placement inside caves

| Field | Type | Purpose |
|-------|------|---------|
| `objectMinDepthBelowSurface` | int | Minimum depth below surface before cave objects place. |
| `surfaceObjectExclusionDepth` | int | Depth band near surface where cave objects are excluded. |
| `defaultObjectAnchor` | `IrisCaveAnchorMode` | Default anchor (floor/ceiling/center/...) for cave objects. |
| `defaultObjectPlaceMode` | `ObjectPlaceMode` | Default placement mode (e.g. `ORGANIC_STILT`). |
| `anchorScanStep` | int | Step used when scanning for a valid anchor surface. |
| `anchorSearchAttempts` | int | Number of anchor-search attempts per placement. |

### Fluids

| Field | Type | Purpose |
|-------|------|---------|
| `allowWater` | boolean | Permit water in carved cavities. |
| `waterMinDepthBelowSurface` | int | Minimum depth before water may appear. |
| `waterRequiresFloor` | boolean | Require a solid floor before placing water. |
| `allowLava` | boolean | Permit lava in carved cavities (flooded up to `caveLavaHeight`). |

### Modules

| Field | Type | Purpose |
|-------|------|---------|
| `modules` | `KList<IrisCaveFieldModule>` | Extra additive/subtractive noise fields layered onto the base carve. |

---

## `IrisCaveFieldModule` fields

Each module is an extra noise field combined into the carve. Vertically-biased styles
(e.g. `SIMPLEX_VASCULAR`, `VASCULAR_THIN`) with a steep `verticalRange` produce
tube/vein channels rather than blobby chambers.

| Field | Type | Purpose |
|-------|------|---------|
| `style` | `IrisGeneratorStyle` | The module's noise style/zoom. |
| `weight` | double | Contribution weight of this module. |
| `threshold` | double | Threshold the module's noise must cross to act. |
| `verticalRange` | `IrisRange` | World-Y span where this module is active. |
| `invert` | boolean | Invert the module (carve where it would normally keep solid, and vice versa). |

---

## `IrisCaveShape` fields

Carve a baked object as the cavity shape, masked by noise (object-driven carving).

| Field | Type | Purpose |
|-------|------|---------|
| `noise` | `IrisGeneratorStyle` | Noise used to mask/scatter the shape. |
| `noiseThreshold` | double | Threshold the noise must exceed for the shape to carve. |
| `object` | String | Object (`.iob`) key used as the carve shape. |
| `objectRotation` | `IrisObjectRotation` | Rotation applied to the carve object. |

---

## `IrisDimensionCarvingEntry` fields

Scopes a carving biome to a world-Y band (the `dimension.carving[]` list).

| Field | Type | Purpose |
|-------|------|---------|
| `id` | String | Unique entry id. |
| `enabled` | boolean | Enable this entry. |
| `biome` | String | Carving biome key (e.g. `carving/standard-deepdark`). |
| `worldYRange` | `IrisRange` | World-Y band where this carving biome applies. |
| `childShrinkFactor` | double | Shrinks child carving biomes (higher = smaller patches). |
| `childStyle` | `IrisGeneratorStyle` | Noise distributing the children. |
| `children` | `KList<String>` | Child carving biomes. |
| `childRecursionDepth` | int | How many child levels to recurse. |

---

## Object-placement carving fields

Set on a biome `objects[]` entry to control behavior relative to the carve.

| Field | Type | Purpose |
|-------|------|---------|
| `carvingSupport` | `CarvingMode` | Where the object may place relative to carving (see enum). |
| `caveAnchorMode` | `IrisCaveAnchorMode` | Anchor for this object inside a cave. |

---

## Enums

### `CarvingMode`

| Value | Meaning | Helper |
|-------|---------|--------|
| `SURFACE_ONLY` | Place only on normal (un-carved) surface. | `supportsSurface()` true |
| `CARVING_ONLY` | Place only inside carved cavities. | `supportsCarving()` true |
| `ANYWHERE` | Place in both surface and carved space. | both true |

### `IrisCaveAnchorMode`

| Value | Meaning |
|-------|---------|
| `PROFILE_DEFAULT` | Use the profile's `defaultObjectAnchor`. |
| `FLOOR` | Anchor to the cave floor. |
| `CEILING` | Anchor to the cave ceiling. |
| `CENTER` | Anchor to the vertical center of the cavity. |
| `ANY` | No anchor preference. |

---

## Relevant `IrisDimension` getter functions

These accessors drive the carve at runtime (decompiled from `IrisDimension`).

| Function | Returns | Purpose |
|----------|---------|---------|
| `isCarvingEnabled()` | boolean | Whether the v4 carver runs. |
| `getCaveProfile()` | `IrisCaveProfile` | Dimension default carve profile. |
| `getCarving()` | `KList<IrisDimensionCarvingEntry>` | Carving-biome band entries. |
| `getCarvingEntryIndex()` | `Map<String, IrisDimensionCarvingEntry>` | Cached id -> entry lookup. |
| `getCaveLavaHeight()` | int | World-Y lava flood level for carved air. |
| `getCaveBiomeStyle()` | `IrisGeneratorStyle` | Noise selecting cave biomes. |
| `isUpperDimensionCarving()` | boolean | Whether the upper dimension is carved. |

The carve itself is executed by `IrisCarveModifier` / `IrisCaveCarver3D`, which read the
profile, evaluate the density field + modules, apply surface breaks, and flood lava
(`isLava` / `getCaveLavaHeight`) / water per the `allow*` flags.

---

## Notes for a carved volcano

- A surface-breaking vertical conduit = `caveProfile` with a tall `verticalRange`, a
  vertically-biased `modules` entry, `allowSurfaceBreak: true`, and `allowLava: true`,
  scoped via a `dimension.carving[]` entry.
- `caveLavaHeight` is dimension-wide, so a summit lava pool above the normal cave-lava
  level needs an object / `IrisCaveShape` rather than the global flood alone.
- The deployed `iris/pack-overlay/dimensions/overworld.json` now uses the v4 schema:
  the legacy `carving: { caves: [...] }` object form (3.x) was replaced with
  `carvingEnabled: true`, a dimension-default `caveProfile`, and a `carving[]` band array,
  ported from `iris/pack-base`. Tune the `caveProfile` / add `carving[]` entries there.

### Worked example: the volcanic peak + caldera (in-repo)

A first carved volcano is wired up as a worked example of the above:

| Piece | File | Role |
|-------|------|------|
| Surface cone | `iris/pack-overlay/biomes/tropical/volcanic-peak.json` | Real `mountain` terrain (gen 30-200) in basalt/blackstone; the visible peak. Added to `regions/tropical.json` `landBiomes`. |
| Caldera throat | `iris/pack-overlay/biomes/carving/volcanic-caldera.json` (+ `-child`) | v4 `caveProfile` with a dominant vertical `VASCULAR_THIN` module (tall `verticalRange` 60-290) = near-vertical chimney; `allowSurfaceBreak` opens the caldera mouth; `allowLava` + magma/lava `wall`/`layers`/`caveCeilingLayers` palettes give the lava throat; `CARVING_ONLY` magma-spire objects. |
| Scoping band | `dimension.carving[]` entry `volcanic-caldera-band` | Applies the caldera biome only in world-Y 190-290, so the carve acts on the genuine tall volcanic-peak summits (raised from 150 to avoid venting frozen-pine mountains). |

Known limitations of this approach (carving is Y-band scoped, not biome-scoped):

- The `volcanic-caldera-band` carves any terrain that reaches Y 190-290, not just the
  `volcanic-peak` biome, so other very tall mountains in range can also get a vent. The
  band min was raised from 150 to 190 so ordinary tall mountains (e.g. frozen-pine
  summits, `mountain` generator) no longer qualify; only the volcanic peak (gen ~30-200)
  reaches the band.
- `caveLavaHeight` is left unset (dimension-wide); the visible lava is from the throat
  palettes, not a global flood. A true summit lava lake above normal cave-lava level
  still needs an object / `IrisCaveShape`.
