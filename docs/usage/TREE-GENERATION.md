# Tree Generation Guide

How the offline tree-generation pipeline works, how to control it, and what it
produces. This is the practical companion to the design rationale in
[ADR-002](../design/adr/ADR-002-tree-generation-script-approach.md) and the
requirements in [REQ-001](../requirements/REQ-001-tree-generation-script.md).

> The pipeline is pure Python 3 standard library (no external packages) and runs
> fully offline. No Minecraft server is needed to generate trees.

---

## 1. Overview

Trees are authored offline as data-driven JSON configs, generated into binary
object files, and then staged into the Iris world-gen pack. Nothing is generated
at world-gen time; the committed object files are the source of truth.

```
iris/tree-gen/configs/*.json        hand-authored fantasy/species tree definitions
        |
        v
iris/tree-gen/generate_tree.py      CLI: reads a config, writes .iob / .schem
        |
        v
iris/tree-gen/configs/output/*.iob  generated object files (Iris V2 IOB)
        |
        v
iris/scripts/deploy-iris-pack.ps1   stages objects/ + biomes into iris/staging
```

Two entry points exist:

| Entry point | Use it to | Output |
|---|---|---|
| `iris/tree-gen/generate_tree.py` | Generate trees from a JSON config (the general tool) | `.iob` and/or `.schem` |
| `iris/scripts/build-vanilla-trees.py` | Regenerate the vanilla species set AND wire them into vanilla biome overlays | `.iob` per species/size + overlay biome JSON |

---

## 2. The generator pipeline (`iris/tree-gen/`)

The generator is fully self-contained in `iris/tree-gen/`: the geometry modules,
the tree definition `configs/` (with `configs/expanded/`), and a directory-local
[`README.md`](../../iris/tree-gen/README.md) quick-start all live there, so the
folder can be shared and run on its own with only Python 3 (no PowerShell, no
server).

`generate_tree.py` assembles each tree from independent geometry modules:

| Module | Responsibility |
|---|---|
| `generate_tree.py` | CLI, config parsing, batch/height loop, output naming, file writing |
| `trunk.py` | Trunk column: width shaping, lean/curve, secondary trunk, bark (wood) faces |
| `canopy.py` | Canopy: species presets, volume layers, and the optional branch system |
| `decorators.py` | Accent blocks placed after the tree is built (vines, fruit, snow, etc.) |
| `roots.py` | Downward taproot + buttress legs so the trunk always meets the ground |
| `nbt.py` | The `.iob` (Iris V2 IOB) and `.schem` (Sponge Schematic v3) writers |

Build order for a single tree (`generate_tree.generate_tree`):

1. **Trunk** is rasterized column-by-column. Each layer's width comes from the
   `trunk_shape` function; lean/curve shift the layer center; gaps from steep
   leans are bridged so the trunk stays connected. Top/bottom blocks touching
   air are swapped to the all-bark `*_wood` variant.
2. **Canopy** is placed on top. Without a `branches` block this is a stack of
   horizontal leaf discs (the "volume" model) sized by the `profile` preset.
   With a `branches` block, only the top crown layer is volume-placed and the
   rest of the foliage hangs off branch tips.
3. **Decorators** (if any) are applied to the assembled blocks.
4. **Roots** (on by default) extend the base downward so Iris, which anchors
   objects by their center, never leaves the trunk floating on uneven terrain.

Determinism: identical config + `seed` always produces identical output (REQ-001).

---

## 3. Running the generator

```powershell
# From the repo root
python iris/tree-gen/generate_tree.py --config iris/tree-gen/configs/embervine.json
```

By default output is written next to the config, in `iris/tree-gen/configs/output/`.

CLI flags:

| Flag | Default | Description |
|---|---|---|
| `--config <path>` | required | JSON file containing a flat array of tree definitions |
| `--out <dir>` | `<config-dir>/output` | Output directory (overrides per-entry defaults) |
| `--format iob\|schem\|both` | `iob` | `.iob` for Iris, `.schem` for editors/WorldEdit, or both |
| `--count <N>` | per-entry `count` | Override the number of variants generated per entry |

Each generated file prints its bounding box and block count, e.g.:

```
  [1/3] embervine_small_1.iob  W=11 H=18 L=11  blocks=412
```

### Regenerating the vanilla set

`build-vanilla-trees.py` is a higher-level wrapper. It calls the generator for
every vanilla species at four size buckets (`S`/`M`/`L`/`XL`), writes the `.iob`
files into `iris/output/vanilla-<species>/`, and then rewrites the vanilla biome
overlays so those biomes actually place the trees:

```powershell
python iris/scripts/build-vanilla-trees.py
powershell iris/scripts/deploy-iris-pack.ps1
```

It edits overlay files only (`iris/pack-overlay/biomes/vanilla/`); it reads
`pack-base` for inheritance but never writes to it.

---

## 4. Controlling a tree: config schema

A config file is a JSON array; each element is one tree definition. Generation
loops over entries, and within each entry over `count` height variants.

### Identity and sizing

| Field | Type | Default | Description |
|---|---|---|---|
| `name` | string | `""` | Logical name; used as a filename prefix in legacy naming |
| `comment` | string | - | Free-text note (ignored by the generator) |
| `filenames` | string[] | - | Explicit output basenames, one per variant (preferred over auto-naming) |
| `trunk` | string | `minecraft:oak_log` | Trunk block ID |
| `leaves` | string | `minecraft:oak_leaves` | Leaf block ID (leaf blockstate auto-appended if missing) |
| `profile` | string | `oak` | Canopy preset: `oak`, `birch`, `spruce`, `jungle`, `acacia`, `dark_oak`, `dark_oak_flat`, `dark_oak_flat_wide`, `cherry` |
| `height_min` / `height_max` | int | `8` / `12` | Trunk height range |
| `count` | int | `1` | Variants per entry; heights are spread across the range with seeded jitter |
| `seed` | int | `0` | RNG seed (deterministic output) |
| `roots` | bool | `true` | Generate the downward root system |

### Trunk shaping

| Field | Type | Default | Description |
|---|---|---|---|
| `trunk_width` | int | `1` | Base width (1 = single block, 2 = 2x2, ...) |
| `trunk_shape` | string | `constant` | Width function: `constant`, `linear`, `sigmoid`, `log`, `sine`, `parabolic` |
| `trunk_shape_params` | object | `{}` | Parameters for the width function |
| `lean_angle` | float | `0` | Lean from vertical, in degrees |
| `lean_azimuth` | float | `0` | Compass direction (deg) the trunk leans toward |
| `lean_azimuth_fn` | string | `constant` | Azimuth over height: `constant`, `linear`, `spiral`, `sine`, `noise` |
| `lean_azimuth_params` | object | `{}` | Parameters for the azimuth function (e.g. `turns` for `spiral`) |
| `trunk_curve_fn` | string | `linear` | How lean accumulates with height: `linear`, `log`, `sigmoid`, `parabolic`, `constant` |
| `trunk_curve_params` | object | `{}` | Parameters for the curve function |
| `secondary_trunk` | string | - | Alternate trunk block used over a height band |
| `secondary_trunk_start` / `_end` | float | `0.5` / `1.0` | Normalized height band (0-1) where the secondary trunk applies |

Width-function parameters:

| `trunk_shape` | Params | Notes |
|---|---|---|
| `constant` | - | Uniform width |
| `linear` | `start`, `end` | Width multiplier at base and crown |
| `sigmoid` | `steepness` | S-curve taper |
| `log` | `base` | Fast narrowing near the base |
| `sine` | `period`, `amplitude` | Oscillating bulge/pinch |
| `parabolic` | `peak_offset`, `floor` | Waisted: narrowest at `peak_offset` |

### Canopy (volume model)

The `canopy` object controls foliage. Without `branches`, leaves are placed as
stacked discs sized by `profile` (or explicit `layers`).

| Field | Type | Default | Description |
|---|---|---|---|
| `canopy.start_angle` | float | `90` | Dome shape: `0` = full sphere, `90` = upper hemisphere, `180` = flat disc |
| `canopy.squish` | float | `1.0` | Vertical scale of the canopy (lower = flatter) |
| `canopy.mode` | string | `trimmed` | Leaf fill: `trimmed`, `filled`, `density`, `noise` |
| `canopy.leaf_density` | float | `0.85` | Fill probability for `density`/`noise` modes |
| `canopy.layers` | array | - | Explicit `{ "y_offset", "radius" }` layers, overriding the `profile` preset |
| `secondary_leaves` | string or array | - | Extra leaf block(s); a list takes `{ "block", "weight" }` entries |
| `secondary_leaf_fraction` | float | `0.35` | Probability a placed leaf uses a secondary block |

### Canopy (branch model)

Add `canopy.branches` to grow a branched crown. Branches sprout per trunk layer
with a spawn probability, rasterize outward to a tip, and place a leaf cluster
there. One level of recursive `sub_branches` is supported.

| Field | Type | Default | Description |
|---|---|---|---|
| `branches.prob_fn` | string | `top_heavy` | Spawn probability over height: `constant`, `linear`, `sigmoid`, `top_heavy`, `gaussian`, `noise` |
| `branches.prob_params` | object | `{}` | Parameters for `prob_fn` |
| `branches.length_fn` | string | `linear` | Branch length over height: `constant`, `linear`, `sigmoid`, `log`, `parabolic` |
| `branches.length_params` | object | `{}` | Parameters for `length_fn` (e.g. `base`, `crown`, `max_len`) |
| `branches.azimuth` | string or float | `random` | Horizontal direction (deg), or `random` for seeded uniform |
| `branches.elevation` | float | `0` | Elevation from horizontal; negative = drooping branches |
| `branches.leaf_start_up` | bool | `false` | Clamp elevation to non-negative (branches angle upward) |
| `branches.cluster_radius` | int | `2` | Leaf-ball radius at each branch tip |
| `branches.cluster_mode` | string | `trimmed` | Cluster fill mode (same values as `canopy.mode`) |
| `branches.cluster_density` | float | `0.85` | Fill probability for `density`/`noise` cluster modes |
| `branches.sub_branches` | object | - | One recursive level (see below) |

Sub-branch fields: `count`, `pitch_delta`, `yaw_delta`, `length_scale`,
`cluster_radius`, `cluster_mode`, `cluster_density`. Pitch/yaw deltas are applied
relative to the parent branch; a cumulative pitch beyond 90 degrees points a
sub-branch downward in world space.

Branch probability functions:

| `prob_fn` | Params | Effect |
|---|---|---|
| `constant` | `p` | Same chance at every layer |
| `linear` | `base_p`, `crown_p` | Ramps base to crown |
| `sigmoid` | `steepness`, `midpoint` | Branches band around a height |
| `top_heavy` | `exponent` | `(y/height)^exponent`; crown-weighted |
| `gaussian` | `mean`, `std` | Bell curve around a height band |
| `noise` | `scale`, `seed` | Irregular organic branching |

### Decorators

`decorators` is a list of accent placements applied after the tree is built.

| Field | Type | Default | Description |
|---|---|---|---|
| `target` | string | `branch_tip` | Where to place: `branch_tip`, `trunk_surface`, `canopy_top`, `canopy_bottom`, `trunk_base` |
| `block` | string | required | Block ID to place |
| `chance` | float | varies | Per-candidate placement probability |
| `axis_aware` | bool | `false` | For `branch_tip`: orient the block to face away from the trunk |

Targets:

- `branch_tip` - on branch endpoints (e.g. magma "fruit"); needs the branch model.
- `trunk_surface` - on air-facing trunk sides (e.g. vines, moss), oriented outward.
- `canopy_top` - on top of the highest block in each column (e.g. snow layers).
- `canopy_bottom` - one block under the lowest leaf in each column (e.g. a glowing underside).
- `trunk_base` - in the ground ring around the trunk base.

---

## 5. Worked example

From `iris/tree-gen/configs/embervine.json` (the "small" variant): a slim jungle
trunk with drooping branches, scattered shroomlight leaves, and magma-block
fruit at the branch tips.

```json
{
  "name": "embervine",
  "filenames": ["embervine_small_1", "embervine_small_2", "embervine_small_3"],
  "trunk": "minecraft:jungle_log",
  "leaves": "minecraft:jungle_leaves",
  "secondary_leaves": "minecraft:shroomlight",
  "secondary_leaf_fraction": 0.1,
  "profile": "jungle",
  "height_min": 9,
  "height_max": 12,
  "seed": 4100,
  "count": 3,
  "canopy": {
    "mode": "density",
    "leaf_density": 0.82,
    "branches": {
      "prob_fn": "top_heavy",
      "prob_params": { "exponent": 2.0 },
      "length_fn": "linear",
      "length_params": { "base": 2, "crown": 5 },
      "azimuth": "random",
      "elevation": -15,
      "cluster_radius": 2,
      "cluster_mode": "density",
      "cluster_density": 0.8
    }
  },
  "decorators": [
    { "target": "branch_tip", "block": "minecraft:magma_block", "chance": 0.28 },
    { "target": "trunk_surface", "block": "minecraft:vine", "chance": 0.18 }
  ]
}
```

```powershell
python iris/tree-gen/generate_tree.py --config iris/tree-gen/configs/embervine.json
# -> iris/tree-gen/configs/output/embervine_small_1.iob, _2, _3, and the medium/large entries
```

---

## 6. Outputs

### `.iob` - Iris V2 IOB (default)

The format Iris consumes. Binary, written by `nbt.write_iob` as a Java
`DataOutputStream` big-endian stream:

```
int   w, h, d                         bounding-box dimensions
UTF   "Iris V2 IOB;"                  magic string
short paletteSize
UTF   blockstate            x paletteSize
int   blockCount
short x, short y, short z, short paletteIndex   x blockCount
int   stateCount                      always 0 (no tile entities for trees)
```

Block coordinates are stored relative to the object center `(w//2, h//2, d//2)`,
which Iris re-adds at placement. Y is anchored on the caller's `y=0` (the trunk
base), so roots (`y<0`) bury and the canopy rises above the surface.

### `.schem` - Sponge Schematic v3

A standard GZIP-compressed NBT schematic (DataVersion 3578 / MC 1.20.1) readable
by WorldEdit and schematic editors. Useful for visual inspection; not consumed by
Iris. Produced with `--format schem` or `--format both`.

### Where files land, and staging

By default files are written to `<config-dir>/output/`. `build-vanilla-trees.py`
writes to `iris/output/vanilla-<species>/`. From there, `deploy-iris-pack.ps1`
stages object files into `iris/staging/objects/trees/...`; biomes reference them
by path-relative key (e.g. `trees/embervine/embervine_small_1`).

> Per Iris pack layering, never edit `iris/pack-base` or `iris/staging` directly.
> Place biome/object overrides under `iris/pack-overlay` (whole-file override).

---

## 7. Related docs

- [ADR-002](../design/adr/ADR-002-tree-generation-script-approach.md) - design decision and options considered.
- [REQ-001](../requirements/REQ-001-tree-generation-script.md) - requirements and acceptance criteria.
- [IRIS-WORLD-BUILDING](../world-design/IRIS-WORLD-BUILDING.md) - end-to-end Iris biome authoring, including object placement.
