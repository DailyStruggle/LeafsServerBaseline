# Iris 4.0 procedural-tree JSON schema vs our `iris/tree-gen` format

Date: 2026-06-07
Source jar: `Iris.jar` (4.0.0-26.1, `art.arcane.iris`), git-ignored at repo root.
Method: `javap -p` on the `art/arcane/iris/engine/object/IrisTree*` and
`IrisProceduralTree` classes (field names are the Gson JSON keys; Iris serializes
fields directly, camelCase).
Our format: `iris/tree-gen/configs/*.json`, schema documented in
`docs/usage/TREE-GENERATION.md`.

> Bottom line: the two models are conceptually the same tree (trunk shaping +
> volume/branch canopy + decorators + roots), and Iris 4.0 is a strict superset
> of our feature set. The differences are mechanical, not semantic:
> (1) casing, (2) Iris flattens our nested `*_params` objects into discrete
> fields, (3) Iris adds many extra knobs/enum values we can leave at default.

---

## 1. Two structural differences that affect every config

1. **Casing.** Ours is `snake_case`; Iris is `camelCase`.
   e.g. `height_min` -> `heightMin`, `leaf_density` -> `leafDensity`,
   `axis_aware` -> `axisAware`, `y_offset` -> `yOffset`.
2. **Flattened parameters.** We pass function parameters as nested objects
   (`trunk_shape_params`, `lean_azimuth_params`, `trunk_curve_params`,
   `branches.prob_params`, `branches.length_params`). Iris 4.0 has **no params
   object** -- every parameter is a top-level discrete field with a `shape*`,
   `azimuth*`, `curve*`, `probability*`, or `length*` prefix. This is the single
   biggest conversion step.

---

## 2. Top-level tree object

`IrisProceduralTree` fields (camelCase) vs our top-level keys.

| Ours (`snake_case`) | Iris 4.0 (`camelCase`) | Notes |
|---|---|---|
| `name` | `name` | match |
| `comment` | (none) | cosmetic; drop on export |
| `filenames` | (none) | our output-naming only; N/A at runtime |
| `count` | `variants` (+ `density`) | Iris splits "how many variants" (`variants`) from "how many placed" (`density`) |
| `trunk` | `trunk` | match |
| `leaves` | `leaves` | match |
| `profile` | `profile` | enum, see SS 6 |
| `height_min` / `height_max` | `heightMin` / `heightMax` | casing only |
| `seed` | `seed` | match |
| `roots` | `roots` | match |
| `trunk_width` | `trunkWidth` | casing only |
| `trunk_shape` | `trunkShape` | enum `IrisTreeFunction` |
| `trunk_shape_params.{start,end}` | `shapeStart` / `shapeEnd` | flattened |
| `trunk_shape_params.steepness` | `shapeSteepness` | flattened |
| `trunk_shape_params.base` | `shapeBase` | flattened |
| `trunk_shape_params.period` | `shapePeriod` | flattened |
| `trunk_shape_params.amplitude` | `shapeAmplitude` | flattened |
| `trunk_shape_params.peak_offset` | `shapePeakOffset` | flattened |
| `trunk_shape_params.floor` | `shapeFloor` | flattened |
| `lean_angle` | `leanAngle` | casing only |
| `lean_azimuth` | `leanAzimuth` | casing only |
| `lean_azimuth_fn` | `leanAzimuthMode` | enum `IrisTreeAzimuthMode` |
| `lean_azimuth_params.{...}` | `azimuthStart/End/Turns/Amplitude/Period/Offset/Scale/WhorlCount` | flattened |
| `trunk_curve_fn` | `trunkCurve` | enum `IrisTreeFunction` |
| `trunk_curve_params.steepness` | `curveSteepness` | flattened (only steepness exposed) |
| `secondary_trunk` | `secondaryTrunk` | casing only |
| `secondary_trunk_start` / `_end` | `secondaryTrunkStart` / `secondaryTrunkEnd` | casing only |
| `secondary_leaves` (string) | `secondaryLeaves` | casing only |
| `secondary_leaves` (array form) | `weightedSecondaryLeaves` (`IrisTreeSecondaryLeaf[]`) | Iris uses a dedicated typed list |
| `secondary_leaf_fraction` | `secondaryLeafFraction` | casing only |
| `canopy` | `canopy` | object, see SS 3 |
| `decorators` | `decorators` | array, see SS 5 |

**Iris-only top-level fields (no equivalent in ours; safe to omit / leave default):**
`chance`, `density`, `plausible`, `mode` (`ObjectPlaceMode`), `rotation`,
`clamp`, `carvingSupport`, `underwater`, `translate`, `stiltSettings`,
`vacuumSettings`, `trunkPalette`, `leavesPalette`, `secondaryLeavesPalette`,
`secondaryTrunkPalette`, `rootStyle`, `rootDepth`, `rootFlare`, `trunkForks`,
`forkHeight`, `forkAngle`.

**Our-only fields with no Iris field:** `comment`, `filenames` (both are
authoring/output-naming concerns that do not exist in a runtime generator).

---

## 3. Canopy (`canopy` -> `IrisTreeCanopy`)

| Ours | Iris 4.0 | Notes |
|---|---|---|
| `start_angle` | `startAngle` | casing only |
| `squish` | `squish` | match |
| `mode` | `mode` | enum `IrisTreeLeafMode` |
| `leaf_density` | `leafDensity` | casing only |
| `layers` (`{y_offset,radius}`) | `layers` (`IrisTreeLayer{yOffset,radius}`) | casing only |
| `branches` | `branches` | object, see SS 4 |
| (none) | `crownStretchX` / `crownStretchZ` | Iris-only oval-crown control |

---

## 4. Branch model (`canopy.branches` -> `IrisTreeBranches`)

| Ours | Iris 4.0 | Notes |
|---|---|---|
| `prob_fn` | `probabilityFunction` | enum `IrisTreeBranchProbability` |
| `prob_params.p` | `probabilityConstant` | flattened |
| `prob_params.base_p` / `crown_p` | `probabilityBase` / `probabilityCrown` | flattened |
| `prob_params.steepness` / `midpoint` | `probabilitySteepness` / `probabilityMidpoint` | flattened |
| `prob_params.exponent` | `probabilityExponent` | flattened |
| `prob_params.mean` / `std` | `probabilityMean` / `probabilityStd` | flattened |
| `prob_params.scale` | `probabilityScale` | flattened |
| (none) | `probabilityPeriods` | for the Iris-only `PERIODIC` mode |
| `length_fn` | `lengthFunction` | enum `IrisTreeFunction` |
| `length_params.base` / `crown` | `lengthBase` / `lengthCrown` | flattened |
| `length_params.max_len` | `lengthMax` | flattened |
| (constant length) | `lengthConstant` | flattened |
| (none) | `lengthSteepness` | flattened |
| `azimuth` (string `random` or float) | `azimuthMode` (enum) + `azimuth` (double) | Iris splits the union into mode + value |
| `elevation` | `elevation` | match |
| `leaf_start_up` | `leafStartUp` | casing only |
| `cluster_radius` | `clusterRadius` | casing only |
| `cluster_mode` | `clusterMode` | enum `IrisTreeLeafMode` |
| `cluster_density` | `clusterDensity` | casing only |
| `sub_branches` | `subBranches` | object, see below |
| (none) | `sag` | Iris-only branch droop |
| (none) | `branchDepth` | Iris-only recursion depth control |

### Sub-branches (`sub_branches` -> `IrisTreeSubBranches`)

| Ours | Iris 4.0 | Notes |
|---|---|---|
| `count` | `count` | match |
| `pitch_delta` | `pitchDelta` | casing only |
| `yaw_delta` | `yawDelta` | casing only |
| `length_scale` | `lengthScale` | casing only |
| `cluster_radius` | `clusterRadius` | casing only |
| `cluster_mode` | `clusterMode` | enum |
| `cluster_density` | `clusterDensity` | casing only |
| (none) | `sag` | Iris-only |

---

## 5. Decorators (`decorators[]` -> `IrisTreeDecorator`)

| Ours | Iris 4.0 | Notes |
|---|---|---|
| `target` | `target` | enum `IrisTreeDecoratorTarget` |
| `block` | `block` | match |
| `chance` | `chance` | match |
| `axis_aware` | `axisAware` | casing only |
| (none) | `palette` (`IrisMaterialPalette`) | Iris-only block-palette alternative to `block` |
| (none) | `length` | Iris-only (e.g. hanging-vine length) |

---

## 6. Enum coverage (ours is always a subset -> direct map, no loss)

In every enum, our string values map 1:1 onto an Iris constant (uppercased).
Iris adds extra values we simply do not use yet.

| Enum | Ours | Iris-only additions |
|---|---|---|
| `IrisTreeProfile` | oak, birch, spruce, jungle, acacia, dark_oak, dark_oak_flat, dark_oak_flat_wide, cherry | PALM, WILLOW, COLUMNAR, BUSH, MEGA_SPRUCE |
| `IrisTreeLeafMode` (canopy/cluster `mode`) | trimmed, filled, density, noise | HOLLOW, GRADIENT, CLUMPED, TATTERED, SPARSE |
| `IrisTreeFunction` (trunk_shape / curve / length) | constant, linear, sigmoid, log, sine, parabolic | EXPONENTIAL, SQRT, STEP, BELL, EASE_IN_OUT |
| `IrisTreeAzimuthMode` (lean / branch azimuth) | constant, linear, spiral, sine, noise, random | GOLDEN_ANGLE, ALTERNATING, WHORL, ZIGZAG |
| `IrisTreeBranchProbability` (`prob_fn`) | constant, linear, sigmoid, top_heavy, gaussian, noise | BOTTOM_HEAVY, PERIODIC, BAND, INVERSE_GAUSSIAN, EXPONENTIAL_DECAY |
| `IrisTreeDecoratorTarget` (`target`) | branch_tip, trunk_surface, canopy_top, canopy_bottom, trunk_base | LEAF_SURFACE, CANOPY_HANG, BRANCH_SURFACE, TRUNK_TOP, GROUND_SCATTER |
| `IrisTreeRootStyle` (Iris-only) | (n/a) | TAPROOT, BUTTRESS, STILT |

---

## 7. Conversion checklist (our config -> Iris 4.0)

To emit Iris-4.0-native tree JSON from our configs:

1. Recase every key to camelCase.
2. Drop `comment` and `filenames`.
3. Flatten each `*_params` object into the prefixed discrete fields:
   - `trunk_shape_params.*` -> `shape*`
   - `lean_azimuth_params.*` -> `azimuth*`, and `lean_azimuth_fn` -> `leanAzimuthMode`
   - `trunk_curve_params.steepness` -> `curveSteepness`
   - `branches.prob_params.*` -> `probability*`, `branches.prob_fn` -> `probabilityFunction`
   - `branches.length_params.*` -> `length*`, `branches.length_fn` -> `lengthFunction`
4. Split `branches.azimuth`: if `"random"` -> `azimuthMode: RANDOM`; if a number ->
   `azimuthMode: CONSTANT` + `azimuth: <n>`.
5. Convert the array form of `secondary_leaves` to `weightedSecondaryLeaves`
   (`{block, weight}` -> already matches `IrisTreeSecondaryLeaf{block, weight}`).
6. Uppercase enum string values (no value is lost; see SS 6).
7. Leave all Iris-only fields unset to take their defaults (palettes, rotation,
   forks, sag, plausible, density/chance, crownStretch, root style/depth/flare).

No feature in our format is missing from Iris 4.0; the mapping is lossless in our
-> Iris direction. The reverse (Iris -> ours) would lose the Iris-only knobs.
