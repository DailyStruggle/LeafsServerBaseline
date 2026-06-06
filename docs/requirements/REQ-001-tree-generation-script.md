# REQ-001: Tree Generation Script for Iris

**Status:** Active  
**Date:** 2026-06-04  
**Author:** TBD

---

## Goal

Provide a script (or set of scripts) that generates tree structures in a format Iris can consume, so that custom or procedural trees can be placed in the world without manual schematic authoring.

---

## Resolved Design Decisions

1. **Output format:** `.nbt` schematic files - Iris consumes schematics for object placement.
2. **Approach:** Offline script (runs outside Minecraft, produces static `.nbt` files committed to the repo).
3. **Tree variety:** All vanilla Minecraft tree species must be coverable via parameterization. Custom/mixed-material trees (e.g. acacia wood with jungle leaves) must also be supported.
4. **Parameterisation:** At minimum - trunk block, leaf block, height range (min/max). Additional knobs (canopy radius, trunk width, branch density) to be defined during implementation.
5. **Output location:** TBD - likely `scripts/output/` or an Iris dimension folder path.
6. **Reproducibility:** Deterministic given a seed.
7. **Toolchain constraints:** TBD.

---

## Requirements

- The script shall accept a trunk block type, leaf block type, height range (min and max), and a random seed as inputs.
- The script shall accept a trunk width (integer) and a trunk shaping function (`constant`, `linear`, `sigmoid`, `log`, `sine`, `parabolic`) with per-function parameters supplied via JSON config.
- The script shall accept canopy parameters via JSON config: `start_angle` (elevation angle controlling dome/flat/flared shape), `squish` (vertical flatten factor), placement `mode` (`trimmed`, `filled`, `density`, `noise`), and `leaf_density` for probabilistic modes.
- The script shall support an optional branch system via JSON config: a branch spawn probability function over normalized height (`constant`, `linear`, `sigmoid`, `top_heavy`, `gaussian`, `noise`), a branch length function, azimuth and elevation angles, and leaf cluster parameters at each branch tip.
- The script shall support one level of recursive sub-branches from each primary branch tip, with configurable pitch and yaw deflection relative to the parent direction, such that cumulative pitch may exceed 90 degrees and produce downward-pointing sub-branches in absolute world space.
- The script shall produce one or more `.nbt` schematic files per invocation, in the format Iris expects, without manual post-processing.
- The script shall support all vanilla Minecraft tree species implicitly through parameterization (e.g. oak trunk + oak leaves = oak tree; acacia trunk + jungle leaves = custom hybrid).
- The script shall allow trunk block and leaf block to be specified independently, enabling mixed-material trees.
- The script shall generate trees whose height falls within the specified min/max range.
- The script shall be runnable from the repo root with a single command.
- The script shall not require a running Minecraft server to execute.
- The script shall produce identical output for the same inputs and seed (deterministic).

---

## Acceptance Criteria

- [ ] Running the script with the same inputs and seed produces identical `.nbt` output on re-run.
- [ ] Generated `.nbt` files are valid and loadable by Iris without errors.
- [ ] All vanilla tree species are producible by varying trunk/leaf block parameters (no hardcoded species list required).
- [ ] A mixed-material tree (e.g. acacia wood + jungle leaves, 32-48 blocks tall) can be generated successfully.
- [ ] A standard oak tree (oak wood + oak leaves, 8-12 blocks tall) can be generated successfully.
- [ ] A spherical canopy tree (start_angle < 90, squish=1.0) produces a visibly dome-shaped leaf volume.
- [ ] A flat-top tree (squish <= 0.3) produces a visibly flattened canopy.
- [ ] Trunk width > 1 with a shaping function produces a correctly tapered multi-block-wide trunk.
- [ ] A branch-driven canopy tree produces visibly distinct branching structure compared to a volume-only canopy.
- [ ] A tree configured with negative cumulative sub-branch pitch produces sub-branches that point downward in world space (below the branch origin).
- [ ] Script usage and parameter reference are documented in `scripts/README.md`.

---

## Out of Scope

- In-game GUI or interactive tree editor.
- Automatic placement of trees into an existing world (placement is Iris's responsibility).
- Support for modded tree blocks beyond what Iris already handles.

---

## Related

- ADR: [`../design/adr/ADR-002-tree-generation-script-approach.md`](../design/adr/ADR-002-tree-generation-script-approach.md)
- Scripts directory: [`../../scripts/`](../../scripts/)
