# Vanilla biome variants plan (size-distribution reskins alongside vanilla)

Goal: for each major wooded `vanilla/*` biome, define one or more sibling "variant" biomes that
look like the same biome but shift the tree **size mix** (and rarity) so the world reads with more
visual range. These are dropped into the same region `landBiomes` list next to the vanilla parent
so Iris interleaves them; the rarer the intended variant, the fewer times it is listed (or use a
lower `rarity` weight / region zoom) -- e.g. a "giant" forest variant appears occasionally next to
ordinary forest.

How a variant differs from its vanilla parent:
- Same `derivative` / `vanillaDerivative` / `category` (so surface block + climate match vanilla).
- Same `decorators` (flora) unless noted.
- Only the `objects[]` tree entries change: which size buckets are placed, and their `chance`.

Mechanics recap (see `iris/pack-overlay/biomes/vanilla/forest.json`):
- Each `objects[]` entry has `chance` (per-column place probability) + `density` and a `place[]`
  list of `trees/vanilla-<species>/<SIZE>#` schematics.
- Size buckets per species (8 schematics each, in `iris/output/vanilla-<species>/`):
  `S` (4-6), `M` (6-9), `L` (8-12), `XL` (11-16) -- legend matches `vanilla-biome-content-plan.md`.
- "fewer small / more medium" = lower the `S` entry chance, raise the `M` entry chance.
- "rarer large-tree variant" = a separate biome listed sparsely whose mix is weighted to `L`/`XL`.

Variant naming convention: `vanilla/<parent>__<variant-tag>` (double underscore marks a reskin
sibling of the same parent). Tags: `sparse`, `dense`, `tall`, `giant`, `young`, `mixed`.

Placement column legend:
- `co-listed`  = add to the same region `landBiomes` once, equal footing with vanilla.
- `rare`       = list once but expect <~10% share; pair with low region `landBiomeZoom` clumping.
- `very rare`  = list once as an occasional "feature" patch (giant groves etc.).

Size-mix column shows approximate object `chance` per bucket (S / M / L / XL); blank = bucket absent.

| variant biome | parent vanilla | placement | species | size mix (S / M / L / XL) | intent / notes |
|---|---|---|---|---|---|
| vanilla/forest__young     | forest                  | co-listed | oak, birch     | 0.22 / 0.10 / - / -        | thicket of small/medium oaks; fewer big crowns |
| vanilla/forest__tall      | forest                  | rare      | oak, birch     | - / 0.18 / 0.10 / -        | fewer small trees, more medium, some large |
| vanilla/forest__giant     | forest                  | very rare | oak            | - / 0.06 / 0.10 / 0.08     | occasional old oak stand with XL crowns |
| vanilla/birch_forest__tall| birch_forest            | rare      | birch          | - / 0.16 / 0.12 / -        | leans toward old_growth without being it |
| vanilla/birch_forest__young| birch_forest           | co-listed | birch          | 0.20 / 0.10 / - / -        | dense young birch scrub |
| vanilla/dark_forest__dense| dark_forest             | co-listed | dark_oak, oak, birch | 0.10 / 0.18 / 0.10 / - | thicker canopy, mostly medium dark oak |
| vanilla/dark_forest__giant| dark_forest             | very rare | dark_oak       | - / 0.08 / 0.14 / 0.10     | rare cathedral grove of XL dark oak |
| vanilla/flower_forest__young | flower_forest        | co-listed | oak, birch     | 0.16 / 0.08 / - / -        | shorter trees so flowers read; keep full flower set |
| vanilla/taiga__tall       | taiga                   | rare      | spruce         | - / 0.14 / 0.16 / -        | fewer saplings, more medium/large spruce |
| vanilla/taiga__giant      | taiga                   | very rare | spruce         | - / 0.06 / 0.12 / 0.10     | rare XL pine pocket short of old_growth |
| vanilla/snowy_taiga__young| snowy_taiga             | co-listed | spruce         | 0.18 / 0.08 / - / -        | stunted snowy spruce |
| vanilla/jungle__giant     | jungle                  | very rare | jungle, oak    | - / 0.10 / 0.16 / 0.12     | rare emergent-canopy giant jungle |
| vanilla/jungle__sparse    | jungle                  | rare      | jungle, oak    | 0.10 / 0.12 / - / -        | reads between jungle and sparse_jungle |
| vanilla/cherry_grove__tall| cherry_grove            | rare      | cherry         | - / 0.12 / 0.14 / -        | taller cherries, fewer shrubs |
| vanilla/cherry_grove__young| cherry_grove           | co-listed | cherry         | 0.16 / 0.08 / - / -        | low blossom thicket, heavy pink_petals |
| vanilla/savanna__sparse   | savanna                 | co-listed | acacia, oak    | 0.04 / 0.03 / - / -        | even thinner tree scatter, more grass |
| vanilla/savanna__tall     | savanna                 | rare      | acacia         | - / 0.05 / 0.06 / -        | occasional larger acacia umbrellas |
| vanilla/swamp__giant      | swamp                   | very rare | oak (vined)    | - / 0.06 / 0.10 / 0.06     | rare big swamp oaks over the marsh |
| vanilla/windswept_forest__young | windswept_forest  | co-listed | spruce, oak    | 0.16 / 0.08 / - / -        | wind-stunted small trees |
| vanilla/plains__sparse    | plains                  | co-listed | oak            | 0.015 / - / - / -          | even fewer lone oaks; keep flower set |
| vanilla/meadow__giant     | meadow                  | very rare | oak            | - / 0.02 / 0.02 / -        | rare lone large meadow oak landmark |

Status: IMPLEMENTED. All 21 variants above are generated by `iris/scripts/build-vanilla-trees.py`
(`BIOME_VARIANTS` + `write_variants()`) into `pack-base/biomes/vanilla/<variant>.json` (minimal)
and `pack-overlay/biomes/vanilla/<variant>.json` (full), and wired once each into the parent's
region `landBiomes`.

Resolved decisions:
- Rarity is expressed per-biome via the biome `rarity` field (a single `landBiomes` listing each,
  not repeated entries): 1 = co-listed, 4 = rare, 9 = very rare. Region `landBiomeZoom` is left at
  its existing value per region for patch coherence (avoids the "lone spruce in plains" look from
  too many tiny biomes).
- Variants reuse the parent's exact `decorators` array (the generator copies `BIOME_DECORATORS`
  of the parent).
- `density` values are first-pass per-bucket; tune after a render.

---

# Part 2: Fix the current vanilla state (parent biomes), too

The variant work above only makes sense if the **parent** `vanilla/*` biomes already read correctly.
Several parents currently place a single size bucket, so they have no internal size range -- the most
visible offender is `vanilla/taiga`, which places only `spruce L` (one group, chance 0.32). In real
vanilla, a taiga is mostly short/medium spruce with the **occasional tall 2-wide (2x2) spruce**, so
our version looks uniformly large and same-y. This part plans the corrective edits to the parent
biome JSON (and the one generator gap they need) so each parent ships a believable size spread.

Source of truth for "current" mix: `iris/scripts/build-vanilla-trees.py` `BIOME_TREES`, which is what
generated the live `objects[]` in `iris/pack-overlay/biomes/vanilla/*.json`.

Status: IMPLEMENTED (Phase 2). `BIOME_TREES` now ships the corrected size spreads below, a new
`spruce2w` (`trunk_width: 2`) species generates the 2-wide bucket (`iris/output/vanilla-spruce2w/`),
and `vanilla/taiga` places spruce S/M plus an occasional 2x2 spruce. Re-run the script to regenerate;
deploy is left to the user.

## 2-wide (2x2) trunk gap

- The generator already supports wide trunks: `build-vanilla-trees.py` `SPECIES["dark_oak"]` sets
  `"trunk_width": 2`, threaded through to `tree-gen/trunk.py` (`_square_positions`, `generate_trunk`).
- **Spruce has no 2-wide bucket yet** -- `iris/output/vanilla-spruce/` only holds width-1
  `spruceS/M/L/XL`. To give taiga its occasional 2x2 spruce we must generate a wide bucket, e.g. a
  second spruce profile `spruce2w` with `"trunk_width": 2` (suggest sizes `M`/`L` only -- a 2x2
  spruce reads as a tall mega-pine). Schematic refs would be `trees/vanilla-spruce2w/spruce2wM#`.
- Until that bucket exists, the taiga fix can ship the small/medium width-1 spread and add the 2-wide
  group as a follow-up (flagged below).

## Corrected parent mixes

Mix columns use the same `species size @ chance` shorthand as `BIOME_TREES`. "2w" = 2-wide bucket
(to be generated). Bold = the change from current.

| biome | current mix (BIOME_TREES) | problem | proposed corrected mix |
|---|---|---|---|
| taiga | spruce L @ 0.32 | all large, no range, no 2-wide | **spruce S @ 0.20, spruce M @ 0.12, spruce 2w-M @ 0.04** |
| snowy_taiga | spruce M @ 0.26 | single bucket, no small scrub | **spruce S @ 0.18, spruce M @ 0.10** |
| grove | spruce M @ 0.24 | single bucket | **spruce S @ 0.16, spruce M @ 0.08** |
| forest | oak M @ 0.30, birch M @ 0.10 | only medium, no saplings/giants | **oak S @ 0.10, oak M @ 0.20, birch M @ 0.10** |
| birch_forest | birch M @ 0.30 | single bucket | **birch S @ 0.10, birch M @ 0.22** |
| dark_forest | dark_oak L @ 0.34, oak M @ 0.08, birch M @ 0.04, mushroom @ 0.03 | dark_oak all L | **dark_oak M @ 0.18, dark_oak L @ 0.16** (keep oak/birch/mushroom) |
| cherry_grove | cherry L @ 0.28 | single bucket | **cherry M @ 0.16, cherry L @ 0.12** |
| jungle | jungle L @ 0.36, jungle XL @ 0.10, oak M @ 0.06 | ok range; keep | keep (already S-less but layered) |
| savanna | acacia M @ 0.05, oak S @ 0.01 | ok | keep |
| swamp | oak M @ 0.16, mushroom @ 0.01 | single oak bucket | **oak S @ 0.06, oak M @ 0.12** |
| windswept_forest | spruce M @ 0.16, oak M @ 0.10 | medium-only | **spruce S @ 0.08, spruce M @ 0.10, oak M @ 0.10** |

Biomes already shipping a sensible single bucket on purpose (`plains`, `meadow`,
`old_growth_*`, `mushroom_fields`) are intentionally left as-is.

How to apply:
- Preferred: edit `BIOME_TREES` in `build-vanilla-trees.py` and re-run it so both
  `pack-base/biomes/vanilla` and `pack-overlay/biomes/vanilla` regenerate consistently (it owns the
  `objects[]` arrays); do not hand-edit the JSON, which the script would overwrite.
- Add a `spruce2w` (`trunk_width: 2`) entry to `SPECIES` before wiring the taiga 2-wide group.

Open decisions (Part 2):
- Exact `chance`/`density` per bucket above are first-pass; tune after one render.
- Whether the 2-wide spruce is a new species key (`spruce2w`) or a per-bucket width flag on `spruce`.
- Whether to also widen the per-size `SIZES` height ranges, or just spread across existing buckets.

---

# Part 3: distinct tree forms per variant (not just size)

Status: IMPLEMENTED (first example). Size reskins alone make variants feel samey;
some variants should ship a recognizably different *tree form* (different trunk
shape, lean, spiral, branched crown), e.g. a spiral-trunk cherry forest.

Mechanism:
- A `SPECIES` entry may carry an optional `extra` dict. `gen_species()` merges it
  verbatim into the per-tree generate_tree entry, so any generator param is
  available: `trunk_shape` / `trunk_shape_params`, `lean_angle` / `lean_azimuth`,
  `lean_azimuth_fn` (e.g. `spiral`) / `lean_azimuth_params`, `trunk_curve_fn` /
  `trunk_curve_params`, and a full `canopy.branches` block.
- A form species is just another species key (e.g. `cherry_spiral`) generated into
  `objects/trees/vanilla-cherry_spiral/` across the S/M/L/XL buckets, and referenced
  from a variant in `BIOME_VARIANTS` exactly like any other species.

First example:
- `cherry_spiral` species: parabolic trunk, `lean_angle: 14`, `lean_azimuth_fn: spiral`
  (`turns: 1.5`), `trunk_width: 2`, drooping `top_heavy` branched crown.
- `cherry_grove__spiral` variant (parent `cherry_grove`, rarity 4): `cherry_spiral` L/XL
  plus a few normal cherry M; reuses cherry_grove decorators; wired into `temperate.json`.

Open decisions (Part 3):
- Which other biomes get a form variant next (e.g. twisted/gnarled dark_oak for a
  dark_forest variant, leaning windswept spruce, titan jungle).
- Whether form params belong inline in `SPECIES.extra` or should be pulled from the
  richer `tree-configs/expanded/*.json` library to avoid duplicating tuned values.
- `turns`/`lean_angle` are first-pass; tune spiral readability at L/XL after a render.

---

# Part 4: tree-form catalogue (real + fantasy) and wiring gap

Status: PLANNING. Two tree systems exist and barely overlap:

- `iris/scripts/build-vanilla-trees.py` (`SPECIES` / `BIOME_VARIANTS`): the only path
  that is actually wired into `vanilla/*` biomes. Only one true form variant exists so
  far: `cherry_spiral`. Everything else is a size/species reskin.
- `iris/tree-configs/*.json` (run via `tree-gen/generate_tree.py`): a richer standalone
  library generated to `iris/output/*` but NOT referenced by any biome variant.

Wiring gap (mechanical, not creative): `gen_species` only passes through
`profile/trunk/leaves/canopy` + the new `extra` block. It does NOT surface
`secondary_leaves`, `decorators` (soul torch, wither rose, snow, glow lichen,
shroomlight), or layered `canopy.layers`. Those are exactly what make the fantasy
configs look fantasy. Wiring any of them requires extending that passthrough (same
small pattern as `extra`).

## Existing fantasy configs (built, not yet wired)

 Config | Trunk / leaves | Signature look | Suggested home (rarity) |
--------|----------------|----------------|-------------------------|
 `glacierpine` | spruce + snow_block | frost-laden pine, snow cap, dripstone icicles | `snowy_taiga` / `grove` variant (4) |
 `blightroot` | dark_oak (sigmoid) | gnarled trunk, decayed canopy, wither roses | `dark_forest` "haunted" variant (9) |
 `glowcap` | mushroom_stem + shroomlight | giant flat mushroom, glow lichen | `dark_forest` understory variant (4) |
 `embervine` | jungle + shroomlight | drooping branches, ember fruit tips | `jungle` "ember" variant (9) |
 `cloudcap` | spruce + azalea | tall trunk, floating azalea sphere | `meadow` / cliff variant (9) |
 `ashwood` | basalt + dead_bush | slim dead tree, soul torch tips | volcanic / badlands variant (9) |
 `spiral-crown-forest` | (spiral crown) | spiralling crowned canopy | `forest` variant (4) |

## Real-form gaps (not covered anywhere yet)

 Proposed species | Params sketch | Suggested home (rarity) |
------------------|---------------|-------------------------|
 `spruce_columnar` | tall, `trunk_width 1`, low squish, steep top_heavy | `old_growth_pine_taiga` (4) |
 `spruce_mega3w` | `trunk_width 3`, sparse high crown | rare titan-pine taiga (9) |
 `oak_spreading` | low `start_angle`, long crown, big cluster_radius | `meadow` / `plains` (4) |
 `oak_swamp` | `secondary_trunk` buttress, droopy crown | `swamp` (4) |
 `oak_autumn` | `secondary_leaves` orange/yellow tint | `forest` seasonal (4) |
 `birch_weeping` | tall thin, drooping `sub_branches` | `old_growth_birch_forest` (4) |
 `mangrove_stilt` | multiple `secondary_trunk` legs | `mangrove_swamp` (1) |
 `acacia_flattop` | strong lean/curve, thin flat canopy | `savanna` (4) |

## Fantasy-media references (new species to author)

 Species | Inspiration | Block realization | Suggested home (rarity) |
---------|-------------|-------------------|-------------------------|
 `mallorn` | LOTR Lothlorien | pale trunk, golden `secondary_leaves` | "elven grove" `forest`/`birch_forest` (9) |
 `worldtree` | Yggdrasil / Great Deku | `trunk_width 3-4`, parabolic flare, huge layered crown | unique landmark, not scattered |
 `weirwood` | GoT heart tree | white birch trunk, red `secondary_leaves` | rare `forest` landmark (9) |
 `truffula` | Dr. Seuss | bare slim trunk, tiny bright puffball top | whimsical `meadow`/plateau (9) |
 `spirit_glow` | Avatar / FernGully | glow_lichen + shroomlight + azalea decorators | bioluminescent night-forest (9) |
 `bloodwood` | Warhammer / Dark Souls | crimson/mangrove logs, sparse dark canopy | "blighted" variant (9) |
 `dragonblood` | dragon tree | thick trunk, flat wide umbrella crown | arid plateau (4) |

Open decisions (Part 4):
- Extend `gen_species` passthrough (`secondary_leaves` / `decorators` / `layers`) before
  wiring any existing fantasy config or new fantasy species.
- Decide whether fantasy variants stay rare landmarks (rarity 9) or get co-listed (1).
- Whether to import `tree-configs/*` values verbatim or re-tune as `SPECIES` entries.

---

# Part 5: wiring pre-built tree-config objects (first: spiral-crown-forest)

Status: IMPLEMENTED (first example). Hand-authored `tree-configs/*.json` objects
(generated to `iris/output/<config>/` and staged by `deploy-iris-pack.ps1`'s
`iobFolderMap`) can now be referenced directly from biome variants without going
through `gen_species`.

Mechanism:
- `build-vanilla-trees.py` gained a `CUSTOM_OBJECT_REFS` table and a `resolve_place`
  helper. A `BIOME_VARIANTS` group whose species is `custom:<key>` resolves to the
  explicit object-ref list (size field ignored); `mushroom` and normal species are
  handled by the same helper.

First example:
- `spiral-crown-forest` (objects `spiral_large_*` / `titan_large_*` / `twisted_small_*`,
  staged to `objects/trees/darkoak/` via the deploy `iobFolderMap`) is wired as
  `dark_forest__spiral` (parent `dark_forest`, rarity 9 = rare find), listed once in
  `regions/forests.json`. Reuses dark_forest decorators.

Open decisions (Part 5):
- Tune chance/density and whether titan should be rarer; confirm in-game it reads as a
  rare find rather than common.

---

# Part 6: wiring the remaining fantasy tree-configs

Status: IMPLEMENTED. The Part 4/Part 5 note that the other configs "need the
gen_species passthrough" was wrong: the `tree-gen` generator already renders their
decorators / secondary_leaves into the baked `.iob`. So they wire exactly like
spiral-crown-forest via `custom:` refs - no `gen_species` change needed.

Pipeline used:
- Added stable size-tiered `filenames` arrays to each config so outputs have
  predictable names (e.g. `glacierpine_small_1`, `ashwood_large_3`).
- Generated each to `iris/output/<config>/` via
  `tree-gen/generate_tree.py --config tree-configs/<config>.json --out output/<config>`.
- These output folders are NOT remapped in the deploy `iobFolderMap`, so they stage to
  `objects/trees/<config>/` by default; `CUSTOM_OBJECT_REFS` references
  `trees/<config>/<filename>` accordingly.

Variants wired (parent, rarity, region):
- `snowy_taiga__glacier` - glacierpine S/M/L - snowy_taiga (4) - frozen + tundra.
- `dark_forest__blight` - blightroot - dark_forest (9) - forests.
- `jungle__ember` - embervine S/M/L - jungle (9) - tropical.
- `meadow__cloud` - cloudcap S/L - meadow (9) - temperate.
- `badlands__ashwood` - ashwood S/M/L - badlands (9) - hot + terralost.
- glowcap is NOT a standalone variant: per design it is the spiral-crown biome's
  special "small" understory tree, so `glowcap_small`/`glowcap_large` were folded into
  `dark_forest__spiral` alongside the spiral/titan/twisted dark oaks.

Open decisions (Part 6):
- Tune chance/density per variant after an in-game render; confirm rarity-9 variants
  read as rare landmarks and the glacier (rarity 4) is not too common.
- Decide whether any of these deserve a dedicated deploy `iobFolderMap` entry instead
  of the default folder-name staging.


---

# Part 7: in-game feedback fixes (density, flora, coverage, glowcap underside)

Status: IMPLEMENTED. After the first in-game load, four issues were addressed.

Calibration note: a native dense Iris biome (`frozen/pines`) places trees at
`chance 0.5, density 1`. Our forest `chance` values (~0.18) were far too low, so all
forests read as sparse. `chance` is the dominant density knob.

Changes:
- dark_forest: now a dense stand of SHORT 2-wide (2x2) flat-top dark oak
  (`dark_oak S 0.30 + M 0.34`, dark_oak is `trunk_width 2` so there are no 1-wide
  dark oaks), with only sparse oak/birch/mushroom accents. `dark_forest__dense` and
  `__giant` bumped to match.
- flower_forest: tree density raised to forest levels (`oak S/M + birch M`); flora
  palette made tulip-heavy with only weight-1 lilac/rose_bush/peony, and grass switched
  to `GRASS_SPARSE` to cut the "too many 2-tall flowers" look.
- Added previously-uncovered vanilla biomes: `snowy_slopes` (sparse spruce) and
  `snowy_plains` (very sparse spruce). Both were already listed in frozen/tundra
  regions, so the new overlays simply give them trees.
- glowcap: cap is now solid `brown_mushroom_block` (removed the shroomlight secondary
  mix and the `canopy_top` shroomlight); a single shroomlight layer is placed on the
  cap underside via a new `canopy_bottom` decorator target (added to
  `tree-gen/decorators.py`), so the lights are viewable from below.

Open decisions (Part 7):
- All chance/density values are first-pass; tune after the next render.
- `deploy-iris-pack.ps1` left `plugins/Iris/cache` with files remaining (server likely
  running); clear it / restart the server so the new pack regenerates.

---

# Part 8: second in-game feedback (global sparseness + dark oak crown shape)

Status: IMPLEMENTED. After the Part 7 load, trees still read too sparse "across the
board" and dark oak crowns were spherical instead of flat-topped.

Changes:
- Density: raised `chance` across `BIOME_TREES` and `BIOME_VARIANTS` toward the native
  dense calibration (~0.5). Main forests (forest/flower_forest/birch_forest/dark_forest/
  cherry_grove/taiga/snowy_taiga/grove/jungle/old_growth*) and their variants now place
  trees at roughly 1.5-1.7x the prior chances; sparse biomes (plains/meadow/savanna)
  left intentionally low.
- Dark oak crown shape: added a dedicated `dark_oak_flat` canopy profile to
  `tree-gen/canopy.py` (layers compressed into the top ~25% of the crown, radius scale
  1.25) and pointed the vanilla `dark_oak` SPECIES at it with `start_angle 150,
  squish 0.28`. Result: crown height ~3-4 vs width ~6-10 (a flat wide umbrella, not a
  sphere). The generic `dark_oak` profile is unchanged, so glowcap/blightroot (which
  reuse it) are unaffected.

Open decisions (Part 8):
- All chance/density values remain first-pass; confirm the new density is not too dense
  after the next render.
- Clear `plugins/Iris/cache` / restart the server so the regenerated pack takes effect.

---

# Part 9: dark oak roof density

Status: IMPLEMENTED. Dark forest still read too sparse to form a closed canopy roof.
Significantly raised the small/medium dark oak density so the flat-top crowns overlap:
- `dark_forest`: dark_oak S 0.48->0.85 (density 3->5), M 0.50->0.90 (density 4->6).
- `dark_forest__dense`: S 0.92, M 0.95 (density 5/6).
- `dark_forest__giant`: M 0.45, L 0.55, XL 0.30 so the big-tree variant also closes
  overhead.
Accent oak/birch/mushroom left low so the roof stays dark-oak dominated.

Open decisions (Part 9):
- Confirm the canopy now reads as a continuous roof (and is not so dense trunks crowd
  the floor) after the next render; back off if needed.

---

# Part 10: wetter swamps, mud, lakes/puddles, hilly flower forest (overlay-only)

Status: IMPLEMENTED. In-game the swamps/mangroves read dry, lakes were too rare in
general, puddles were missing from forests/plains, and flower forest was awkwardly
flat. Also confirmed the stray 1-wide dark oak trees are native Iris roofed-forest
schematics (`trees/oak/troofed*`/`mroofed*` in `swamp/roofed-forest`/`roofed-wayward`),
NOT anything we authored - left as-is per user.

Constraint (per user): edit the OVERLAY only; scripts must not write pack-base.
Sea level is `fluidHeight = 75`; biome `generators` min/max are heights relative to
sea level (negative = underwater), so lowering them floods terrain.

Changes:
- `build-vanilla-trees.py`: now overlay-only (removed the pack-base writes in
  `write_variants`; pack-base is still read for inheritance). Added
  `BIOME_GENERATORS` + `BIOME_LAYERS` overrides applied to overlays (and inherited
  by variants):
  - `flower_forest` -> `mountain` generator 6/28 so it rolls like short hills/ridges.
  - `mangrove_swamp` -> low generator -4/3 (~half floods) + a muddy floor (mud +
    muddy_mangrove_roots over mud/dirt).
- Native swamp land biomes dropped to shallow wetland (~half underwater) via overlay
  copies in `pack-overlay/biomes/swamp/`: marsh -4/3, swamp-forest -3/2, willow-forest
  -4/4, handy-willow-forest -3/8, roofed-forest/-wayward -4/3, cambian-drift -6/4,
  creaks -2/6, denmyre -3/3; `-extended` siblings brought down to short hills (6/22-30).
- New overlay biomes `vanilla/lake` (RIVER, -14/-4) and `vanilla/puddle` (RIVER, -3/-1),
  patterned on the temperate river biome. Wired into region `landBiomes`: forests +
  temperate (lake x2, puddle x3), tundra (lake x2, puddle x2), tropical (lake x2),
  hot (lake x1), swamp (lake x2). Repeated entries raise frequency.

Open decisions (Part 10):
- Tune depths/frequencies after a render: lake depth, puddle shallowness, and how much
  of the swamp floods (currently aiming ~half underwater).
- Decide later whether to remove/replace the native roofed-forest 1-wide dark oaks.
- Clear `plugins/Iris/cache` / restart the server so the regenerated pack takes effect.

---

# Part 11: terrain relief for biomes that read wrong as flats (overlay-only)

Status: IMPLEMENTED. Audit showed nearly every `vanilla/*` biome still used the flat
`plain` generator (min 4 / max 10, ~6 blocks relief); only flower_forest and
mangrove_swamp had been given terrain. Peaks, badlands/mesa, plateaus, dunes and
windswept biomes all read as flat fields, and the taiga family was table-flat when
vanilla generates it gently hilly (per user note).

Mechanism: extended the `BIOME_GENERATORS` table in `build-vanilla-trees.py` (overlay
only). Also extended `write_overlays` to emit an overlay for any biome that has only a
generator/layer override (so treeless peaks/ice_spikes get written). Values are
relative to sea level (y75), calibrated against native Iris biomes (mountains ~66/89,
mesa highplains 50/70, plateaus small-cliffs 25/37, hills rare-hills 0/40, ice spikes
highplains 40/50).

New generators:
- Peaks: jagged_peaks mountain-aggro 60/110, frozen_peaks mountain-large 65/105,
  stony_peaks mountain-aggro 55/95, snowy_slopes mountain 30/70.
- Mesa/badlands: badlands + wooded_badlands highplains 50/70, eroded_badlands
  cracked-cliffs 70/98.
- Plateau/dunes/etc: savanna_plateau highplains 40/60, desert smooth-dunes 8/24,
  meadow rare-hills 5/22, stony_shore small-cliffs 2/20.
- Windswept: windswept_hills rare-hills 15/45, windswept_gravelly_hills plain-cliffs
  20/50, windswept_forest small-cliffs 12/35, windswept_savanna plain-cliffs 15/45.
- ice_spikes highplains 30/45 (spikes themselves remain decorator/object work).
- Taiga family gently hilly: taiga + snowy_taiga rare-hills 5/22, old_growth_pine_taiga
  + old_growth_spruce_taiga rare-hills 6/26, grove rare-hills 8/28.

Variants inherit their parent's generator automatically (write_variants copies the
parent `BIOME_GENERATORS` entry), so taiga__tall/giant, snowy_taiga__*, meadow__*,
windswept_forest__young etc. become hilly too.

Open decisions (Part 11):
- Tune amplitudes after a render: peaks may be too tall and create harsh borders with
  adjacent flat biomes; back off min/max if so.
- ice_spikes still needs actual packed-ice spike objects/decorators; only the base
  terrain was raised.
- Clear `plugins/Iris/cache` / restart the server so the regenerated pack takes effect.

---

# Part 12: make the giant roofed forest actually "roofed" (wider crowns + more trees)

Status: IMPLEMENTED. The giant roofed-forest variant (`dark_forest__giant`) still read
too open - the big dark oaks were tall but their crowns were not wide enough to overlap
into a continuous roof.

Changes:
- `tree-gen/canopy.py`: added a `dark_oak_flat_wide` canopy profile - same shallow
  flat-slab layer shape as `dark_oak_flat` but radius scale 1.8 (vs 1.25). Empirically
  this widens a height-12 dark oak crown from ~15-16 to ~21 blocks across.
- `build-vanilla-trees.py`: added a `dark_oak_wide` SPECIES (flat-wide profile,
  trunk_width 2, leaf_density 0.97) and pointed `dark_forest__giant` at it, raising
  chance/density so the broad crowns interlock: M 0.45/4 -> 0.70/6, L 0.55/5 -> 0.85/7,
  XL 0.30/3 -> 0.55/5.
- The new `vanilla-dark_oak_wide` output folder is unmapped in the deploy
  `iobFolderMap`, so it stages to `objects/trees/vanilla-dark_oak_wide/` (matching the
  overlay refs). The regular `dark_oak` SPECIES / `dark_oak_flat` profile are untouched,
  so `dark_forest`/`dark_forest__dense` and glowcap/blightroot are unaffected.

Open decisions (Part 12):
- Confirm after a render that the giant crowns now form a closed roof without the floor
  feeling overcrowded by the thick 2-wide trunks; back off density if too tight.
- Clear `plugins/Iris/cache` / restart the server so the regenerated pack takes effect.

---

# Part 13: scale up biome widths (less densely packed biomes)

Status: IMPLEMENTED. In-game the biomes read as packed too densely (each individual
land biome too small). The lever is the per-region `landBiomeZoom` in
`iris/pack-overlay/regions/*.json` (larger = wider individual land biomes within a
region). This is independent of terrain relief and tree mixes.

Change: multiplied `landBiomeZoom` by 2.5x across every overlay region so biomes are
much larger:
- forests 3.5 -> 8.75, temperate 3.5 -> 8.75, frozen 3 -> 7.5, terralost 3 -> 7.5,
  tundra 4.5 -> 11.25, tropical 5.5 -> 13.75, hot 7 -> 17.5, swamp 2 -> 5.0,
  mushroom 1.8 -> 4.5.

Open decisions (Part 13):
- Tune the 2.5x factor after a render; if biomes are now too large (mono-biome
  stretches), back the multiplier down toward ~1.8-2x.
- Clear `plugins/Iris/cache` / restart the server so the regenerated pack takes effect.

---

# Part 14: spiral roofed forest - fewer shrooms, more roof (mid/tall trees)

Status: IMPLEMENTED. In-game `dark_forest__spiral` had too many glowcap mushrooms and
did not read as roofed enough; it needed more mid-to-tall trees overhead.

Change: retuned the `dark_forest__spiral` mix in `BIOME_VARIANTS` (build-vanilla-trees.py):
- spiral_crown_large 0.20/4 -> 0.40/6 (denser mid-tall roof layer)
- spiral_crown_titan 0.05/2 -> 0.14/3 (more tall emergents)
- spiral_crown_twisted (small) 0.12/3 -> 0.10/3 (slightly fewer short trees)
- glowcap_small 0.10/3 -> 0.05/2, glowcap_large 0.03/1 -> 0.02/1 (fewer shrooms)

Open decisions (Part 14):
- Confirm after a render that the roof now closes while glowcaps remain an accent;
  tune chance/density if still too sparse or too crowded.
- Clear `plugins/Iris/cache` / restart the server so the regenerated pack takes effect.

---

# Part 15: vanilla roofed forest - add large mushrooms

Status: IMPLEMENTED. In-game the plain `dark_forest` was "nearly perfect" but lacked the
large huge-mushroom growths classic to roofed forests; the mushroom tree group was set
too rare (`("mushroom", None, 0.03, 1)`).

Change: in `BIOME_TREES`, raised the `dark_forest` mushroom group to `0.14, 2`. The
`mushroom` species resolves to `MUSHROOM_REFS`, which already includes the large
`redgeneric1..8` and `browngeneric1..2` huge-mushroom objects (plus `smolshroom`), so
large mushrooms now appear scattered among the dark oaks.

Open decisions (Part 15):
- Tune the 0.14 chance after a render; if huge mushrooms feel too frequent, back off,
  or split a large-only mushroom ref if more big caps (vs smol) are wanted.
- Clear `plugins/Iris/cache` / restart the server so the regenerated pack takes effect.

---

# Part 16: spiral cherry forest - one species, varying sizes (bigger = rarer)

Status: IMPLEMENTED. Request: make a spiral cherry biome variant that, unlike the
roofed forest (which mixes several dark-oak forms), is built entirely from the spiral
cherry as a SPECIES at varying sizes, with the bigger trees rarer.

The `cherry_grove__spiral` variant already existed (parent cherry_grove, rarity 4,
wired into `temperate.json`) but mixed only `cherry_spiral` L/XL plus a normal `cherry`
M. Reworked its `BIOME_VARIANTS` mix to use the `cherry_spiral` SPECIES across the full
S/M/L/XL size spread with descending frequency so big spiral cherries are rare:
- cherry_spiral S 0.32/4, M 0.22/3, L 0.12/3, XL 0.05/2 (no plain cherry).

`cherry_spiral` is already generated by `gen_species` for every size (S/M/L/XL ->
`objects/trees/vanilla-cherry_spiral/`), so no new generation logic was needed; the
overlay now references `cherry_spiralS1..XL1`.

Open decisions (Part 16):
- Tune the size frequencies after a render so the variety reads clearly and the big
  spirals stay rare.
- Clear `plugins/Iris/cache` / restart the server so the regenerated pack takes effect.

---

# Part 17: harsher variant rarity + wider biomes (special biomes harder to find)

Status: IMPLEMENTED. In-game the large/special variant biomes and the hand-authored
"iris" fantasy biomes read as too easy to find. Two levers were turned, both
overlay-only:

1) Harsher rarity. Added a central `RARITY_SCALE` map in `build-vanilla-trees.py`,
   applied in `write_variants` (`rarity = RARITY_SCALE.get(rarity, rarity)`), so the
   authored tiers map to a steeper curve:
   - 1 -> 1 (co-listed common siblings stay common: forest__young, dark_forest__dense,
     cherry_grove__young, plains__sparse, etc.)
   - 4 -> 12 (rare variants: forest__tall, taiga__tall, jungle__sparse,
     snowy_taiga__glacier, cherry_grove__spiral/tall, savanna__tall, etc.)
   - 9 -> 30 (very-rare finds: forest__giant, dark_forest__giant/spiral/blight,
     jungle__giant/ember, taiga__giant, meadow__cloud, badlands__ashwood, swamp__giant,
     etc.)
   Editing the map (not each entry) keeps the authored intent visible while making the
   final scarcity easy to retune.

2) Wider biomes. Multiplied every overlay region `landBiomeZoom` by 1.6x on top of the
   Part 13 2.5x, so each individual land biome renders larger and special biomes are
   spread further apart:
   - forests 8.75 -> 14.0, temperate 8.75 -> 14.0, frozen 7.5 -> 12.0,
     terralost 7.5 -> 12.0, tundra 11.25 -> 18.0, tropical 13.75 -> 22.0,
     hot 17.5 -> 28.0, swamp 5.0 -> 8.0, mushroom 4.5 -> 7.2.

Open decisions (Part 17):
- Tune both levers after a render: if special biomes are now too scarce, ease
  `RARITY_SCALE` (e.g. 9 -> ~20); if mono-biome stretches feel too large, back the
  zoom multiplier down.
- Clear `plugins/Iris/cache` / restart the server so the regenerated pack takes effect.

---

# Part 18: swamp y-variation + lily pads (less dry dirt/mud, more water)

Status: IMPLEMENTED. In-game the swamp biomes read too dry - a lot of dirt/mud
standing above water level instead of grass + water - and they lacked lily pads.

Changes (overlay-only):
- Deepened terrain troughs for more y-level variation and more water. For every
  non-extended `swamp/*` overlay biome, lowered the generator `min` by 4 and
  switched flat `plain` generators to `mountain` (more surface variance):
  marsh -4/3 -> -8/3, swamp-forest -3/2 -> -7/2, roofed-forest/roofed-wayward
  -4/3 -> -8/3, willow-forest -4/4 -> -8/4, cambian-drift -6/4 -> -10/4,
  creaks -2/6 -> -6/6, denmyre -3/3 -> -7/3, handy-willow-forest -3/8 -> -7/8.
  The high points (grassy hummocks) are unchanged; the deeper troughs flood, so
  the biome reads as grass islands among water rather than a dry dirt plateau.
- Mangrove swamp likewise deepened in `BIOME_GENERATORS` (-4/3 -> -7/3,
  plain -> mountain) for more water while keeping its muddy floor.
- Added a lily-pad decorator to each swamp biome using the native sea-surface
  mechanism (`"partOf": "SEA_SURFACE"`, `lily_pad` palette, chance 0.06),
  copied from `pack-base/biomes/swamp/sea/ocean.json`.

Open decisions (Part 18):
- Tune flood depth/lily density after a render; if too much water drowns the
  willows, raise `min` back up a couple blocks, and adjust lily `chance` to taste.
- Clear `plugins/Iris/cache` / restart the server so the regenerated pack takes effect.

---

# Part 19: scale biome widths back 50% (Part 17 zoom was too big)

Status: IMPLEMENTED. In-game the biome scale from Part 17 ended up too big (the
landBiomeZoom multipliers compounded - 2.5x then 1.6x = 4x - so biomes felt
exponentially large). Halved every overlay region landBiomeZoom (0.5x):
- forests 14 -> 7, temperate 14 -> 7, frozen 12 -> 6, terralost 12 -> 6,
  tundra 18 -> 9, tropical 22 -> 11, hot 28 -> 14, swamp 8 -> 4, mushroom 7.2 -> 3.6.

Open decisions (Part 19):
- Confirm after a render that biomes now read at a comfortable scale; nudge up/down
  if still too large/small.
- Clear plugins/Iris/cache / restart the server so the regenerated pack takes effect.

---

# Part 20: swamp grass above water, dirt below water

Status: IMPLEMENTED. Request: swamp biomes should show grass above water level and dirt
below water level (previously the dirt/coarse_dirt sub-surface was showing through the
flooded troughs).

Mechanism: Iris paints the underwater floor from a biome's `seaLayers` (where terrain
sits below the fluid), while the normal `layers` paint the above-water surface. Added a
dirt `seaLayers` (mostly dirt + a little coarse_dirt) to every non-extended swamp
overlay (`iris/pack-overlay/biomes/swamp/*.json`: marsh, swamp-forest, willow-forest,
roofed-forest, roofed-wayward, cambian-drift, creaks, denmyre, handy-willow-forest).
Their existing grass-heavy top `layers` already cover the above-water hummocks, so the
biomes now read as grass islands with dirt floors under the water.

Open decisions (Part 20):
- Tune the seaLayers palette if a different floor (mud, gravel) reads better underwater.
- Clear plugins/Iris/cache / restart the server so the regenerated pack takes effect.
