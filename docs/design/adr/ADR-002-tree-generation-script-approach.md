# ADR-002: Tree Generation Script Approach

**Status:** Revised - supersedes the offline Python-script decision in favour of the `leaf-treegen` plugin  
**Date:** 2026-06-04 (revised 2026-06-19)  
**Author:** TBD

---

## Revision (2026-06-19): prefer `leaf-treegen`

The original decision below selected an offline Python 3 script (Option A) that wrote Sponge Schematic v3 `.schem` files for Iris to consume. Two things have changed since:

1. Iris has been retired for vanilla-datapack worldgen (see [ADR-005](ADR-005-retire-iris-for-vanilla-datapack-worldgen.md)), so an Iris-consumed `.schem` pipeline no longer fits the architecture.
2. A dedicated, server-agnostic tree-generation plugin now exists: [`leaf-treegen`](https://github.com/DailyStruggle/leaf-treegen).

**Revised decision:** prefer the [`leaf-treegen`](https://github.com/DailyStruggle/leaf-treegen) plugin as the primary tree-generation mechanism. `leaf-treegen`:

- Generates trees in pure Java and **bakes each procedural species into `N` vanilla structure templates (`.nbt`)** (`N` = the species' `count`) directly at datapack-generation time - no external tooling required.
- Wires the baked templates into **vanilla jigsaw template pools** and emits a **standard vanilla worldgen datapack on the fly**, so Minecraft's native jigsaw/structure systems handle placement (robust and efficient) instead of fragile runtime listeners. This matches the vanilla-datapack direction of ADR-005.
- Is **server-agnostic** via a `Platform` abstraction (Paper/Folia today; Fabric/NeoForge planned).
- Keeps the same **parameter-driven model** the original ADR required: per-species `trunk`/`leaf` block, `heightMin`/`heightMax`, `trunk-width`, `trunk-shape`, `lean`, and explicit `canopy` layers (`yOffset` + `radius`), defined in `species/*.json` with biome/placement overrides in `config.yml`.
- Supports `generation-mode` of `DATAPACK` (recommended, jigsaw-based), `PROCEDURAL` (runtime), `BOTH`, or `NONE`, plus custom NBT-tagged saplings for player planting.

Consequences of the revision:

- The offline Python script (`scripts/tree-gen/`, Option A) is **superseded** as the shipping pipeline. The Python `tools/tree-gen` project is **retained only as a reference** for the prior externally-baked NBT model; pre-baked `.nbt` templates it produces can still be dropped into the datapack `structure/` folder and referenced via `variants`.
- Determinism, offline authoring, and parameterisation requirements from [REQ-001](../../requirements/REQ-001-tree-generation-script.md) remain satisfied: `leaf-treegen` bakes deterministic variants and requires no manual schematic editing.
- Building/deploying follows the plugin module flow (`plugins/deploy-plugins.ps1`); worldgen changes apply on the next (user-triggered) server boot, data-only changes via `/leaftree reload`.

The sections below are **retained for historical context** and describe the now-superseded offline-script design.

---

## Context

We want to generate tree structures that Iris (the world-generation plugin) can consume as `.nbt` schematics. Trees need to be authored somewhere - the question is whether that happens offline (a script produces static asset files committed to the repo) or at runtime (generation happens during world-gen). The choice affects toolchain, iteration speed, and how designers tweak trees.

Requirements discussion (see [REQ-001](../../requirements/REQ-001-tree-generation-script.md)) established that:
- Iris consumes `.nbt` schematic files for object placement.
- Trees must be parameterized by trunk block, leaf block, and height range (min/max) rather than by a hardcoded species list, so all vanilla species and mixed-material hybrids are supported implicitly.
- Output must be deterministic given a seed.
- The script must run offline with no running Minecraft server required.

---

## Decision

Use an **offline Python 3 script (Option A)** that accepts trunk block, leaf block, height range, and seed as parameters and writes Sponge Schematic v3 `.schem` files. Generated schematics are committed to the repo and consumed by Iris at world-gen time.

Key decisions:
- **Output format:** Sponge Schematic v3 `.schem` files, GZIP-compressed NBT (not `.iob` or datapack functions).
- **Parameterization model:** trunk block + leaf block + height range (min/max) + seed. No hardcoded species enum - vanilla species and hybrids are all expressed through block choices.
- **Offline, not runtime:** script runs outside Minecraft; no server required.
- **Toolchain:** Python 3, zero external dependencies. NBT writing reuses the minimal big-endian struct-based writer pattern proven in `RTP/scripts/schematics/generate_skyblock_island.py`.
- **Shape algorithm:** column trunk (straight vertical) + radial disc canopy layers (1B).
- **Canopy model:** 2A primary - geometry core accepts explicit `(y_offset, radius)` layer profiles; 2B secondary - named species presets (`oak`, `birch`, `spruce`, `jungle`, `acacia`, `dark_oak`, `cherry`) that compute scaled 2A layer profiles from height.
- **Input interface:** Simple scalars via CLI flags (`--trunk`, `--leaves`, `--profile`, `--height-min`, `--height-max`, `--seed`, `--count`, `--out`); all structural parameters (trunk shaping, canopy) via `--config <file>` - a single JSON file with a flat list of tree definitions that the script iterates.
- **Output naming:** `<profile>_<trunk>_<leaves>_h<height>_s<seed>.schem` (fully descriptive, no collision risk).
- **Batch:** `--count N` (default 1); heights distributed across the min/max range using the seed.
- **Default output directory:** `scripts/output/`.
- **Trunk width shaping:** integer base width (`trunk_width`) shaped over height by a named function (`trunk_shape`): `constant`, `linear`, `sigmoid`, `log`, `sine`, `parabolic`. Function parameters supplied as a JSON object (`trunk_shape_params`). Width at layer y = `max(1, round(trunk_width * shape_fn(y / height)))`.
- **Leaf placement:** canopy volume defined by `start_angle` (elevation angle from vertical, default 90 deg = flat disc; lower = dome/sphere; higher = flared/umbrella), `squish` (0.0-1.0 vertical scale, lower = flatter top), and placement `mode`: `trimmed` (corner-trimmed disc, default), `filled` (full disc), `density` (seeded probabilistic falloff, controlled by `leaf_density` float), `noise` (seeded value noise mask).
- **Branch system (optional):** canopy may be driven by an explicit branch model instead of (or composited with) the volume model. Branches sprout from the trunk at each layer y with a probability given by a named function (`branch_prob_fn`) over normalized height. Each branch has a direction vector (azimuth angle, elevation angle relative to horizontal) and a length given by a named function (`branch_length_fn`). Branches support one level of recursive sub-branches: each sub-branch inherits the parent direction and applies a configurable angular deflection (pitch delta, yaw delta), allowing sub-branches to droop below horizontal or point downward in absolute world space when the cumulative pitch exceeds 90 deg. Leaf clusters are placed at every branch and sub-branch tip using `cluster_radius` and `cluster_mode`.

---

## Implementation Design

### Module Structure

```
scripts/tree-gen/
  generate_tree.py   - CLI entry point; JSON config parsing; batch loop; output naming
  nbt.py             - zero-dep Sponge Schematic v3 NBT writer (struct + gzip)
  trunk.py           - column trunk geometry; width shaping functions
  canopy.py          - layer profile engine (2A); species preset table (2B); leaf placement modes
scripts/output/      - default output directory for generated .schem files
scripts/trees.json   - sample config file (flat list of tree definitions)
```

### Invocation

```
python scripts/tree-gen/generate_tree.py --config scripts/trees.json [--out scripts/output/] [--count N]
```

Simple scalars (`--out`, `--count`) may be supplied on the CLI and override the per-entry JSON values. All structural parameters live in the config file.

### Output Naming

```
<profile>_<trunk_block>_<leaves_block>_h<height>_s<seed>.schem
```

Example: `oak_oak_log_oak_leaves_h10_s42.schem`

### JSON Config Schema

```json
[
  {
    "trunk": "minecraft:oak_log",
    "leaves": "minecraft:oak_leaves",
    "profile": "oak",
    "height_min": 8,
    "height_max": 12,
    "seed": 42,
    "count": 5,
    "trunk_width": 1,
    "trunk_shape": "sigmoid",
    "trunk_shape_params": { "steepness": 2.0 },
    "canopy": {
      "start_angle": 90,
      "squish": 1.0,
      "mode": "density",
      "leaf_density": 0.85,
      "branches": {
        "prob_fn": "top_heavy",
        "prob_params": { "exponent": 2.0 },
        "length_fn": "linear",
        "length_params": { "base": 1, "crown": 4 },
        "azimuth": "random",
        "elevation": 0,
        "cluster_radius": 2,
        "cluster_mode": "trimmed",
        "sub_branches": {
          "count": 2,
          "pitch_delta": 30,
          "yaw_delta": 45,
          "length_scale": 0.5,
          "cluster_radius": 1,
          "cluster_mode": "trimmed"
        }
      }
    }
  }
]
```

Field reference:

| Field | Type | Default | Description |
|---|---|---|---|
| `trunk` | string | required | Minecraft block ID for trunk (e.g. `minecraft:oak_log`) |
| `leaves` | string | required | Minecraft block ID for leaves |
| `profile` | string | `"oak"` | Named canopy preset; drives default layer radii scaled to height |
| `height_min` | int | required | Minimum trunk height in blocks |
| `height_max` | int | required | Maximum trunk height in blocks |
| `seed` | int | required | RNG seed; same seed + inputs = identical output |
| `count` | int | `1` | Number of schematics to generate; heights distributed across range |
| `trunk_width` | int | `1` | Base trunk width (1 = single block, 2 = 2x2, etc.) |
| `trunk_shape` | string | `"constant"` | Width shaping function: `constant`, `linear`, `sigmoid`, `log`, `sine`, `parabolic` |
| `trunk_shape_params` | object | `{}` | Per-function parameters (see table below) |
| `canopy.start_angle` | float | `90` | Elevation angle (deg) from vertical; 90 = flat disc, <90 = dome/sphere, >90 = flared |
| `canopy.squish` | float | `1.0` | Vertical scale of canopy volume; lower = flatter top |
| `canopy.mode` | string | `"trimmed"` | Leaf placement: `trimmed`, `filled`, `density`, `noise` |
| `canopy.leaf_density` | float | `0.85` | Fill probability for `density` mode |
| `canopy.branches` | object | omit | Optional branch system; if present, branches drive canopy placement (see branch table below) |
| `canopy.branches.prob_fn` | string | `"top_heavy"` | Branch spawn probability function over normalized height (see branch prob table) |
| `canopy.branches.prob_params` | object | `{}` | Parameters for `prob_fn` |
| `canopy.branches.length_fn` | string | `"linear"` | Branch length function over normalized height (same function names as trunk shape) |
| `canopy.branches.length_params` | object | `{}` | Parameters for `length_fn` |
| `canopy.branches.azimuth` | string or float | `"random"` | Horizontal angle (deg) of branches; `"random"` = seeded uniform distribution |
| `canopy.branches.elevation` | float | `0` | Elevation angle (deg) from horizontal; 0 = horizontal, positive = upward, negative = downward |
| `canopy.branches.cluster_radius` | int | `2` | Leaf ball radius at each branch tip |
| `canopy.branches.cluster_mode` | string | `"trimmed"` | Leaf placement within each cluster; same values as `canopy.mode` |
| `canopy.branches.sub_branches` | object | omit | Optional one level of recursive sub-branches from each primary branch tip |
| `canopy.branches.sub_branches.count` | int | `1` | Number of sub-branches per primary branch |
| `canopy.branches.sub_branches.pitch_delta` | float | `0` | Pitch deflection (deg) applied to sub-branch relative to parent direction; positive = upward, negative = downward; cumulative pitch may exceed 90 deg, producing downward-pointing sub-branches in world space |
| `canopy.branches.sub_branches.yaw_delta` | float | `0` | Yaw (horizontal) deflection (deg) applied to sub-branch relative to parent direction |
| `canopy.branches.sub_branches.length_scale` | float | `0.5` | Sub-branch length as a fraction of the parent branch length |
| `canopy.branches.sub_branches.cluster_radius` | int | `1` | Leaf ball radius at each sub-branch tip |
| `canopy.branches.sub_branches.cluster_mode` | string | `"trimmed"` | Leaf placement within each sub-branch cluster |

Branch probability function reference:

| `prob_fn` | Params | Effect |
|---|---|---|
| `constant` | `p` (float 0-1) | Same probability at every trunk layer |
| `linear` | `base_p` (float), `crown_p` (float) | Linearly ramps from base to crown |
| `sigmoid` | `steepness` (float), `midpoint` (float 0-1) | Branches cluster around a height band |
| `top_heavy` | `exponent` (float) | `p = (y/height)^exponent`; exponentially more branches near crown |
| `gaussian` | `mean` (float 0-1), `std` (float) | Bell curve; branches peak at a specific height band |
| `noise` | `scale` (float), `seed` (int) | Seeded value noise; irregular organic branching |

Trunk shape parameter reference:

| `trunk_shape` | Params | Notes |
|---|---|---|
| `constant` | - | Same width all the way up |
| `linear` | `start` (float), `end` (float) | Width multiplier at base and crown |
| `sigmoid` | `steepness` (float) | S-curve taper; higher = sharper transition |
| `log` | `base` (float) | Logarithmic taper; fast narrow near base |
| `sine` | `period` (float), `amplitude` (float) | Oscillating bulge/pinch along trunk |
| `parabolic` | `peak_offset` (float, 0-1) | Thicker at base and top, narrower at peak_offset |

---

## Options Considered

### Option A - Offline Python script (selected)

A Python script in `scripts/tree-gen/` reads parameters (CLI args or config file) and writes Sponge Schematic v3 `.schem` files using a zero-dependency NBT writer (struct + gzip, same pattern as `RTP/scripts/schematics/generate_skyblock_island.py`).

- Pro: no runtime dependency; deterministic by default; fits repo-as-source-of-truth.
- Pro: zero-dependency NBT writer already proven in the RTP repo - no `nbtlib` or other packages needed.
- Con: requires Python 3 in the contributor toolchain.
- Con: generated `.schem` files are binary; large batch regenerations produce noisy git diffs.

### Option B - Node/JS script (not selected)

Same as Option A but in JavaScript/Node.

- Not selected: NBT writing libraries less mature; no existing reference implementation in this project's ecosystem.

### Option C - Minecraft datapack / function (rejected)

Datapack command sequence places blocks in-world via `/fill` and `/setblock`.

- Rejected: requires a running server; hard to version-control output; limited procedural complexity.

### Option D - Runtime Iris plugin generator (rejected)

Extend Iris to generate trees procedurally at world-gen time.

- Rejected: tightly coupled to Iris internals; harder to test offline; may require Java/plugin changes.

---

## Consequences

- Positive: designers can inspect and version-control `.schem` output; no server needed to iterate.
- Positive: all vanilla tree species and arbitrary hybrids are supported without code changes - just different block parameters.
- Positive: deterministic output makes CI validation straightforward.
- Positive: zero external Python dependencies - only stdlib (`struct`, `gzip`, `os`).
- Negative: generated `.schem` files are binary; large batch regenerations produce noisy git diffs.
- Negative: contributors need Python 3 installed locally (no other dependencies required).

---

## Alternatives Considered

See Options A-D above. Options B, C, and D were rejected. Option A (Python, zero-dependency NBT writer) was selected.

---

## Related

- Requirements: [`../../requirements/REQ-001-tree-generation-script.md`](../../requirements/REQ-001-tree-generation-script.md)
- Scripts directory: [`../../../scripts/`](../../../scripts/)
