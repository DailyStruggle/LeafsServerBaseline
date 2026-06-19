# Frostpeak Lava-Tree Caldera - Checkpoint and Continuation

> UPDATE 2026-06-07 (later thread): the baked lava-tree "object volcano" approach below
> was PARKED. After hitting Iris 3.9 limits (object Y is only terrain + ~h/2 + translate.y;
> `clamp` is a placement GATE not a Y pin; only a 3-block-radius `slopeCondition`, no wide
> radial slope test; objects can't level a narrow peak), we pivoted to a TERRAIN-DRIVEN
> ice-volcano THEME that mirrors Iris's stock `tropical/volcanoes(-lava)`:
>   - `frozen/frostpeak-lava.json` now uses the stock `mountain` generator (max 180 / min 3)
>     so the cone + sunken lava well are real terrain; lava runs the cone faces via a `wall`
>     palette (lava/tuff/packed_ice), and the caldera cap is a lava pool over a cave_air gap.
>   - `frozen/frostpeak.json` uses a GLOB childStyle (zoom 0.2, exp 4) to scatter small,
>     central calderas, and its SURFACE layer is an icy colored-terracotta mix.
>   - The lava-tree object pipeline (`iris/scripts/build-lavatree-mountaintops.py`,
>     `clutter/lavatree-mt-*`) is kept in-repo, UNREFERENCED, for the Iris 4.0 carving
>     mechanisms (real carved volcanoes later).
> The sections below are retained as the history/rationale for that parked object approach.
>
> UPDATE 2026-06-08: the lava-tree object approach is now considered DEPRECATED, implicitly
> superseded by the Iris 4.0 cave-carving method (`docs/design/IRIS-V4-CARVING.md`). A
> surface-breaking lava conduit (`caveProfile` + `allowLava` + a dimension `carving[]` band)
> carves real lava terrain, so the baked `lavatree`/`lavatreebig`/`lavatree-mt-*` objects and
> `build-lavatree-mountaintops.py` are kept unreferenced for history only - do not wire them
> into new biomes; build carved volcanoes via the v4 carver instead.

Date: 2026-06-07
Status: WORKING / deployed, paused mid-design for a new thread.
Effective Issue lineage: "work on biome additions e.g. Frostpeak" -> iterating on the
Frostpeak ice-volcano caldera look (lava pools / calderas / lava trees).

This is a durable checkpoint so a fresh thread can resume without re-deriving everything.
It records (1) the current working state, (2) the hard-won Iris facts and the dead ends we
hit, and (3) the agreed next design + a concrete continuation point.

---

## 1. Current working state (all deployed + reference-validated)

The Frostpeak feature is a parent/child biome whose summit carries an inverted,
obsidian-encased "lava tree" (a lava pocket-mass) with a single level lava pool.

### Generators (`iris/pack-overlay/generators/`)
- `frostpeak-plateau.json` - copy of base `highplains` but interpolation
  `BILINEAR_STARCAST_9` horizontalScale = 32 (wide blend). Used by the WIDE base biome.
  (We do NOT edit `highplains`; it is shared.)
- `frostpeak-basin.json` - FLAT composite + `BILINEAR_STARCAST_9` horizontalScale = 10.
  Used by the caldera child so even a small patch ramps to its full flat top.

### Biomes (`iris/pack-overlay/biomes/frozen/`)
- `frostpeak.json` - WIDE base biome. `frostpeak-plateau` generator, value 96-124
  (world Y ~171-199). Children: `["frozen/frostpeak-lava"]`, `childStyle` CELLULAR
  zoom 1.4, `childShrinkFactor` 1.4. No objects (just the icy surface). In
  `regions/frozen.json` `landBiomes`.
- `frostpeak-lava.json` - caldera CHILD. `frostpeak-basin` value 170 (world ground
  Y ~245). One object: the big lava tree `clutter/lavatreebig1-3`, `chance` 1.0,
  pinned to an absolute world Y via `clamp { minHeight: 235, maxHeight: 235 }`,
  `slopeCondition.maximumSlope` 3.0, rotation on. Scorched basalt/blackstone/obsidian
  layers. `rarity` 6.
  - Height relationship (all PINNED, so every caldera matches): pool/structure
    Y=235 (clamp) < ground Y~245 (generator 170) < clear-cone top ~249 (235 + 14).
    So the lava reads as a recessed pool and the void_air cone carves the ground open
    above it.

### Objects in use
- `clutter/lavatreebig1-3.iob` - the mega lava tree (currently the live feature),
  generated from `iris/tree-gen/configs/lavatree.json` (entry "lavatreebig").
  Currently ~77x77 footprint, ~110-136 tall (the cone widened it).
- `clutter/lavatree1-3.iob` - smaller variant, generated but NOT currently placed.

### Object pipeline (`iris/tree-gen/`)
The lava tree reuses the tree generator with new features we added this thread:
- `generate_tree.py`:
  - `invert_y` - flip the whole model so the trunk drives DOWN, "roots" flip UP.
  - `root_block` - override root material (we used `minecraft:void_air`).
  - `encase` + `encase_targets` - wrap every exposed face of a target material
    (lava) in a sheath block (obsidian) -> fully sealed, round obsidian-skinned trunk.
  - `encase_open_top` - expose lava ONLY on the single highest Y plane (the flat
    trunk mouth) so the pool is one flat level; branches stay encased.
  - `encase_top_variation` - random upward obsidian stacking for a bumpy crust.
  - `clear_cone_height` / `clear_cone_expand` - bake a widening cone of `void_air`
    above the pool to guarantee clear airspace (conic).
- `trunk.py`: `set_round_trunk(flag, seed)` + `_irregular_radius` - disc trunk whose
  radius is modulated by angular sine harmonics (NOT a perfect circle); `_axis_block`
  guard so non-orientable blocks (lava/obsidian) never get a bad `[axis=]`.
- `canopy.py`: `_axis_block` guard (same reason, for branches).
- `roots.py`: `root_block` parameter.
- `iris/tree-gen/configs/lavatree.json` - two entries: "lavatree" (small) and
  "lavatreebig" (mega, trunk_width 30, sigmoid taper, irregular round, encased,
  open-top, clear-cone). Regenerate with:
  `python ../tree-gen/generate_tree.py --config ../tree-gen/configs/lavatree.json --out ../pack-overlay/objects/clutter --format iob`
  (run from `iris/scripts`).

### Deploy + validate
- Deploy: `iris/scripts/deploy-iris-pack.ps1`.
- Validate: `iris/scripts/check-all-region-biomes.py` (all refs valid).

### Superseded / unused experiments (left in place, not referenced)
- `build-caldera-objects.py` -> `clutter/caldera1-3` (irregular open lava craters).
- `build-lava-pockets.py` -> `clutter/lavapocket1-3` (obsidian-bowl pockets).
- `build-lava-jigsaw.py` -> `clutter/lavacell|lavaedge` + the `jigsaw-pieces/clutter`,
  `jigsaw-pools/clutter/lava-spread`, `jigsaw-structures/frost-caldera` (emergent
  jigsaw caldera). The `frost-caldera` jigsaw is no longer referenced by any biome.
- `build-magmaspires.py` -> `clutter/magmaspire1-3`.

---

## 2. Hard-won Iris facts (READ THIS before iterating - it saves cycles)

- World Y of a generator value = `fluidHeight` (75 in `dimensions/overworld.json`) +
  the biome generator's `min`/`max` value. So generator 150 -> world ~225, 160 -> ~235,
  170 -> ~245. (We burned a round confusing 230 -> ~305.)
- Iris object placement SKIPS `minecraft:air` and `minecraft:cave_air` (they never
  carve/replace). It DOES place `minecraft:void_air` - so void_air is the only baked
  "carve" block. (We burned a round on floating air that did nothing.)
- Iris does NOT tick placed `minecraft:fire` - it just floats midair forever. Do not
  use fire as a carve/scorch material. (Burned a round.)
- Per-biome size: there is NO per-biome "zoom". `rarity` = FREQUENCY only (high rarity
  looks sporadic, not smaller). `landBiomeZoom` (region) scales ALL land biomes. The
  only way to shrink ONE biome's footprint is to make it a CHILD and use the parent's
  `childShrinkFactor` (higher = smaller, keep < 3.0) + `childStyle`.
- A child biome has its OWN generators, so it CAN be taller than its parent.
- BUT a small child patch + a WIDE interpolation horizontalScale can't ramp up to its
  full flat height -> small calderas top out lower/inconsistently. Lower the child
  generator's interpolation horizontalScale (we used 10) so even small patches reach
  the full flat top. (This was the real cause of "pools at varying Y", which we first
  misdiagnosed as the open-top logic.)
- `clamp { minHeight: N, maxHeight: N }` on an object placement PINS it to an absolute
  world Y (use to force every pool co-level regardless of local terrain).
- `slopeCondition` (minimumSlope/maximumSlope, 0-10, ~3-block radius) exists ONLY on
  biome OBJECT placements, NOT on jigsaw structure placements. Use it to keep features
  off slopes / on flat ground.
- Jigsaw `IrisJigsawStructurePlacement`: a too-small `rarity` makes
  `separation = round(rarity/divisor)` round to 0 -> `spacing` 0 -> `floorDiv` by zero
  -> server chunk crash. Set explicit non-zero `salt`/`spacing`/`separation` to bypass
  the auto-calc. (Also in `docs/scratch/LESSONS_LEARNED.md`.)
- The tree-gen filename sanitizer flattens `/`, so object `filenames` must be bare
  names and you point `--out` at the target subdir (we output into `objects/clutter`).
- Iris places objects with the base at terrain surface by DEFAULT (no `mode`). Using
  `mode: CENTER_HEIGHT` lifts the object by ~half its height (we saw ~+10).
- `customDerivitives` on a biome registers a NEW datapack biome that only installs on a
  server RESTART (`autoRestartOnCustomBiomeInstall`); until then you get
  "Cannot find biome for IrisBiomeCustom ..." warnings. We dropped customDerivitives on
  the caldera/parent to avoid that and used `vanillaDerivative` instead.

### Process frustrations (so we don't repeat them)
- Long loop because every visual check needs a USER-triggered server boot (agent must
  not boot the server - no console/rcon). Each tweak = a full deploy + manual boot.
- Several root causes were only findable by reading Iris source on GitHub
  (`IrisJigsawStructurePlacement`, `IrisObjectPlacement`) - do that early, not late.
- We cycled through 4 approaches for the caldera before landing on the lava tree:
  (1) child-biome bowls with carved cave_air, (2) flat-basin biome bowls,
  (3) self-contained obsidian-bowl `.iob` objects, (4) an emergent jigsaw of lava
  cells, then (5) the inverted obsidian-encased "lava tree". The lava tree is the
  KEEPER (the user likes that it mirrors real lava pockets).

---

## 3. Agreed next design (the new thread starts here)

Goal: keep the lava tree, but make each one a SELF-CONTAINED noisy mountaintop, and
generate MANY of them via a SCRIPT (noise-seeded) so they rarely repeat. This pattern
will be REUSED for the other volcano variants (Cinderfall, Ashcrown, Embertide,
Cinnabar Mesa) by swapping palettes/derivatives.

Decisions locked in with the user:
- Keep the lava tree (it reflects IRL lava pockets). Trunk width 50-70.
- Flat top is good.
- Generate the lava tree, THEN wrap it in a NOISY mountaintop (noise-shaped terrain
  cap around/over the tree), baked into the object.
- Make a LOT of variants via script + noise patterns (not one at a time) so they are
  unlikely to repeat.
- This whole approach is intended to be reused for the other 4 volcano variants.

### Concrete continuation plan (suggested)
1. Write a script (e.g. `iris/scripts/build-lavatree-mountaintops.py`) that, per variant:
   a. Generates the inverted obsidian-encased lava tree (reuse `generate_tree.py` as a
      library - import `generate_single` / the build path - with trunk_width 50-70,
      flat-top pool, void_air clear-cone, irregular round trunk).
   b. WRAPS it in a noise-shaped mountaintop: build a noisy dome/cap of terrain
      (snow/stone/ice/obsidian palette) around and over the tree's obsidian shell so
      the object reads as a whole mountaintop; the central lava pool + cone stay clear.
   c. Emits N seeded variants (e.g. 8-16) -> `clutter/lavatree-mt-<v>.iob`.
2. Wire ALL variants into `frostpeak-lava.json` `objects` `place` list (Iris picks one
   per placement) for non-repetition; keep the absolute-Y `clamp` so pools stay co-level.
3. Re-check the width/protrusion relationship: if the mountaintop wrap is baked into the
   object, the caldera biome can be smaller (the object IS the summit), reducing the
   biome-width fight.
4. Parameterize palette + `vanillaDerivative`/`customDerivitives` so the same script
   produces the Cinderfall / Ashcrown / Embertide / Cinnabar Mesa variants.
5. Deploy (`deploy-iris-pack.ps1`) + validate (`check-all-region-biomes.py`), then ask
   the user to boot and verify (pools co-level, no protrusion, varied mountaintops).

### Open questions to confirm at the start of the new thread
- Should the noisy mountaintop be BAKED into the tree object (one self-contained `.iob`,
  recommended for variety + to kill the width fight), or stay as biome terrain + tree?
- Mountaintop palette (snow/packed-ice/stone/obsidian mix?) and how much it should bury
  vs expose the obsidian tree.
- How many variants, and the noise style for the cap.
- Confirm trunk width (50-70) and overall mountaintop diameter.

---

## 4. Quick resume commands (from `iris/scripts`)
- Regenerate lava trees:
  `python ../tree-gen/generate_tree.py --config ../tree-gen/configs/lavatree.json --out ../pack-overlay/objects/clutter --format iob`
- Deploy: `powershell -File deploy-iris-pack.ps1`
- Validate refs: `python check-all-region-biomes.py`
- Inspect an object: `python iob_inspect.py ../pack-overlay/objects/clutter/lavatreebig1.iob`
- Server boots are USER-triggered (do not boot it from the agent).
