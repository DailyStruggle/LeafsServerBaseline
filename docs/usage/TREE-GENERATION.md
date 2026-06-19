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
the tree definition `configs/`, and a directory-local
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
| `nbt.py` | The `.iob` (Iris V2 IOB), `.schem` (Sponge Schematic v3), and `.nbt` (vanilla structure template) writers |

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
| `--format iob\|schem\|nbt\|both\|all` | `iob` | `.iob` for Iris, `.schem` for editors/WorldEdit, `.nbt` for vanilla structure templates, `both` (iob+schem), or `all` (iob+schem+nbt) |
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

### `.nbt` - vanilla structure template

A GZIP-compressed vanilla structure NBT (DataVersion 3578 / MC 1.20.1), written by
`nbt.write_structure_nbt`. This is the format Minecraft itself loads via
`/place template`, structure blocks, and jigsaw `template_pool` pieces, so a
generated tree can be dropped straight into vanilla datapack worldgen (see
[ADR-005](../design/adr/ADR-005-retire-iris-for-vanilla-datapack-worldgen.md)).
The root compound carries:

```
DataVersion : int
size        : [int, int, int]                       W, H, L
palette     : [ {Name: str, Properties?: {str:str}} ]
blocks      : [ {state: int, pos: [int, int, int]} ]
entities    : []
```

Each blockstate string (e.g. `minecraft:oak_log[axis=y]`) is split into its block
`Name` and a `Properties` compound. Only the generated blocks are emitted;
unfilled positions are left as structure void. Produced with `--format nbt` or
`--format all` (iob+schem+nbt).

### Where files land, and staging

By default files are written to `<config-dir>/output/`. `build-vanilla-trees.py`
writes to `iris/output/vanilla-<species>/`. From there, `deploy-iris-pack.ps1`
stages object files into `iris/staging/objects/trees/...`; biomes reference them
by path-relative key (e.g. `trees/embervine/embervine_small_1`).

> Per Iris pack layering, never edit `iris/pack-base` or `iris/staging` directly.
> Place biome/object overrides under `iris/pack-overlay` (whole-file override).

---

## 7. Natural Placement (LeafTreeGen Plugin)

While the Python tool generates the structures, the **LeafTreeGen** plugin (located in `plugins/leaf-treegen`) handles their placement in a live Minecraft world.

### Installation & Build

To compile the plugin, use the provided Gradle wrapper. This produces a **unified JAR** at the root of the plugin project:

```powershell
# Build the unified JAR
./gradlew :plugins:leaf-treegen:jar
```

The output will be at:
`plugins/leaf-treegen/build/libs/leaf-treegen-0.1.0.jar`

Drop this JAR into your server's `plugins/` folder. On first startup, it will extract the bundled tree species JSONs to `plugins/LeafTreeGen/species/`.

### How it works

1.  **Registry**: The plugin scans its `plugins/leaf-treegen/species/` folder for JSON configurations.
2.  **Discovery**: It then scans world datapacks for `.nbt` templates matching the configured `namespace:group` folder.
3.  **Generation**: On enable/reload, it generates a vanilla worldgen datapack in each world's `datapacks/` folder, mapping species to biomes.

### Configuration

Base species are defined in JSON files under `plugins/leaf-treegen/species/`. Placement mapping and overrides are in `plugins/leaf-treegen/config.yml`.

Example `species/giant_oak.json`:
```json
{
  "name": "giant_oak",
  "biomes": ["minecraft:forest"],
  "trunk": "minecraft:oak_log",
  "leaves": "minecraft:oak_leaves",
  "heightMin": 15,
  "heightMax": 25
}
```

Example `config.yml` override:
```yaml
placement:
  giant_oak:
    spacing: 10 # Make them rare
    weight: 10 # Lower chance compared to other species in the same biome
    definitions:
      titan: 100 # Heavily weight the "titan" variation of giant_oak
```

- **namespace/group**: Matches the `data/<namespace>/structure/<group>/` path where Python tool outputs live.
- **biomes**: Biomes where the tree spawns naturally and where the sapling is allowed to grow.
- **spacing/separation**: Controls how sparsely the trees are scattered in the world.

### Admin Commands

The plugin uses a parameter-based command system:

- `/leaftree generate species=<id>`: Test placement at your cursor.
- `/leaftree give species=<id> [player=<name>] [amount=<n>]`: Hands out special tagged saplings.
- `/leaftree list [species=<id>]`: Lists available species or variants for a specific species.
- `/leaftree reload`: Reloads `config.yml` and regenerates the worldgen datapacks (if in DATAPACK mode).

### Adding a New Tree (Full Workflow)

The intended way to create a new tree is to bridge the offline generator with the live plugin:

1.  **Geometry Design**: Edit a JSON definition in `tools/tree-gen/configs/`. Use the [Schema Section](#4-controlling-a-tree-config-schema) above to tune the trunk shape, canopy layers, and branches.
2.  **Baking**: Run `python tools/tree-gen/generate_tree.py --config <path> --format nbt`. This produces vanilla-compatible `.nbt` templates.
3.  **Deployment**: Place the `.nbt` files in a world datapack (e.g., `datapacks/leaf-worldgen/data/leaf/structure/<species_group>/`).
4.  **Registration**: Add the species entry to `plugins/leaf-treegen/config.yml`. Ensure the `namespace` and `group` keys match the datapack path.
5.  **Placement Mapping**: In `config.yml`, use the `placement` section to map species IDs to biomes and tune their frequency (spacing/separation).
6.  **Activation**: Use `/leaftree reload` in-game. This automatically handles the complex vanilla worldgen wiring (template pools, structure sets, etc.) via a generated datapack.

### Generation Modes

LeafTreeGen supports two primary ways of placing trees naturally:

1. **DATAPACK** (Recommended):
   - Automatically generates a vanilla worldgen datapack in the world's `datapacks` folder.
   - Trees are placed by Minecraft's native jigsaw system during chunk generation.
   - Best for performance and compatibility with other worldgen tools.
2. **PROCEDURAL**:
   - Trees are placed by the plugin at runtime during the `ChunkLoadEvent` (only for new chunks).
   - Useful if you want more dynamic control or if jigsaw-based generation is not desired.
   - If the `procedural` block is present in `config.yml`, trees are generated algorithmically on-the-fly; otherwise, random `.nbt` variants are placed.
   - Density is roughly controlled by the `spacing` config value.

Set the mode in `config.yml` using the `generation-mode` key.

### Biome Placement Mapping (`config.yml`)

The `placement` section in `config.yml` maps tree species (from their JSON ID) to specific biomes and controls their generation density.

```yaml
placement:
  forest:
    biomes: ["forest", "flower_forest"]
    spacing: 4
    separation: 2
  dark_forest:
    biomes: ["dark_forest"]
    spacing: 5
    separation: 2
```

- **`biomes`**: A list of namespaced biome IDs (e.g. `minecraft:forest`). If the namespace is omitted, `minecraft:` is assumed.
- **`spacing`**: The average distance in chunks between structure placement attempts.
- **`separation`**: The minimum distance in chunks between structures.
- **`trees`**: (Optional) A weighted map of species and individual trees for probabilistic selection within the biome. Use the format `species_id:tree_name` to reference a specific tree inside a JSON config, or just `species_id` to reference the entire file.
    - **Note**: Weights are **relative**, not percentages. They do not need to add up to 100. If you have `tree_a: 1` and `tree_b: 1`, they each have a 50% chance. If they add up to 10, each unit is 10%.

Example with granular probabilistic selection:
```yaml
placement:
  forest:
    biomes: ["forest"]
    spacing: 4
    separation: 2
    trees:
      forest:oak: 40      # 40% chance for standard Oak from forest.json
      forest:gnarled: 20  # 20% chance for Gnarled Oak from forest.json
      ashwood: 10         # 10% chance for any tree in ashwood.json
      birch_forest: 10    # 10% chance for any tree in birch-forest.json
```

### High-Density Forests (Overlapping Species)

If you want a dense forest with more than one tree per chunk, you can overlap multiple species mappings for the same biome. Each entry in `config.yml` creates an independent vanilla structure placement attempt.

Example of a "Dense Dark Forest":
```yaml
placement:
  # LAYER 1: Giant Titan Trees (spaced out)
  dark_forest_titan:
    biomes: ["dark_forest"]
    spacing: 4
    separation: 2
    trees:
      dark_forest_megas: 100

  # Dense Understory (placed in every chunk)
  dark_forest_brush:
    biomes: ["dark_forest"]
    spacing: 1
    separation: 0
    trees:
      # Mix of medium dark oak varieties from dark-forest.json
      dark_forest:tree_2: 50  # Medium variant A
      dark_forest:tree_3: 50  # Medium variant B
```

In this example, the Dark Forest will generate with massive titan trees occasionally, while the ground is filled with a dense layer of medium dark oaks in nearly every chunk.

### Tree Configuration (`species/*.json`)

The `leaf-treegen` plugin reads tree definitions from JSON files in the `species/` resource folder (or `plugins/leaf-treegen/species/` on disk). The schema is designed to be compatible with both the Python generator and the Java plugin.

| Field | Type | Default | Description |
|---|---|---|---|
| `id` | string | filename | Internal unique ID for the species. |
| `name` / `display-name` | string | `id` | User-facing name of the tree. |
| `weight` | int | `1` | Species-level weight (frequency in biomes). |
| `definitions` | map | `{}` | Override weights for internal tree variations (e.g. `{ titan: 90 }`). |
| `namespace` | string | `leaf` | Datapack namespace for structures. |
| `group` | string | `id` | Subfolder under the namespace for structure variants. |
| `biomes` | string[] | `[]` | List of biome IDs where this tree spawns. If empty, spawns nowhere by default. |
| `sapling-item` | string | `OAK_SAPLING` | The Bukkit Material name for the sapling that grows this tree. |
| `worldgen` | bool | `!biomes.isEmpty()` | Whether to include this species in the generated worldgen datapack. |
| `spacing` | int | `3` | Distance between structure centers in chunks (jigsaw `spacing`). |
| `separation` | int | `2` | Minimum distance between structure centers (jigsaw `separation`). |
| `salt` | int | `-1` | Random salt for placement. `-1` auto-generates from the ID. |
| `spread-type` | string | `linear` | Jigsaw placement spread type (`linear` or `triangular`). |
| `step` | string | `surface_structures` | Worldgen step for structure placement. |
| `variants` | object / array | `[]` | Manual list of `.nbt` structure locations and weights. |

### Procedural Parameters

If the `procedural` block (or flat fields like `trunk`, `leaves`) is present, the plugin can generate trees algorithmically at runtime.

#### Base Procedural Fields
| Field | Type | Default | Description |
|---|---|---|---|
| `trunk-block` / `trunk` | string | `minecraft:oak_log` | Block ID for the trunk. |
| `leaf-block` / `leaves` | string | `minecraft:oak_leaves` | Block ID for the canopy. |
| `height-min` | int | `5` | Minimum height in blocks. |
| `height-max` | int | `10` | Maximum height in blocks. |
| `profile` | string | `OAK` | Canopy preset (see [Section 4](#4-controlling-a-tree-config-schema)). |
| `trunk-width` | double | `1.0` | Base thickness of the trunk. |
| `trunk-shape` | string | `CONSTANT` | Width function: `CONSTANT`, `LINEAR`, `SIGMOID`, `LOG`, `SINE`, `PARABOLIC`. |
| `trunk-shape-params` | map | `{}` | Parameters for the width function. |
| `round-trunk` | bool | `false` | If true, rasterizes the trunk as a cylinder instead of a square column. |
| `lean-angle` | double | `0.0` | Initial lean angle in degrees. |
| `lean-azimuth` | double | `0.0` | Compass direction for the lean. |
| `azimuth-fn` | string | `CONSTANT` | Lean direction function: `CONSTANT`, `SPIRAL`. |
| `azimuth-params` | map | `{}` | Parameters for the azimuth function (e.g., `turns`, `start`). |
| `curve-fn` | string | `LINEAR` | How lean accumulates: `LINEAR`, `LOG`, `SIGMOID`, `PARABOLIC`, `CONSTANT`. |
| `curve-params` | map | `{}` | Parameters for the curve function. |
| `secondary-trunk` | string | `null` | Optional block for a height band. |
| `secondary-trunk-start` / `_end` | double | `0.5` / `1.0` | Normalized height range for the secondary trunk. |

#### Canopy Parameters (`canopy`)
| Field | Type | Default | Description |
|---|---|---|---|
| `mode` | string | `DENSITY` | Fill mode: `DENSITY`, `FILLED`, `TRIMMED`, `NOISE`. |
| `density` | double | `1.0` | Probability of leaf placement in `DENSITY` mode. |
| `secondary-leaves` | string | `null` | Optional second leaf block type. |
| `secondary-fraction` | double | `0.0` | Probability of using the secondary leaf block. |
| `layers` | array | `[]` | List of `{ yOffset, radius }` objects. |
| `branches` | object | `null` | Recursive branching logic (see below). |

#### Branch Parameters (`branches`)
| Field | Type | Default | Description |
|---|---|---|---|
| `count` | int | `0` | Number of branches to sprout. |
| `min-length` / `max-length` | double | `2.0` / `5.0` | Length range for branches. |
| `min-elevation` / `max-elevation` | double | `-20.0` / `45.0` | Elevation angle range. |
| `spacing` | double | `1.0` | Minimum vertical spacing between branches. |
| `start-height` | double | `0.6` | Normalized height where branching begins. |

## 8. Related docs

- [ADR-002](../design/adr/ADR-002-tree-generation-script-approach.md) - design decision and options considered.
- [REQ-001](../requirements/REQ-001-tree-generation-script.md) - requirements and acceptance criteria.
- [IRIS-WORLD-BUILDING](../../legacy/iris/docs/IRIS-WORLD-BUILDING.md) - (archived per ADR-005) end-to-end Iris biome authoring, including object placement.
