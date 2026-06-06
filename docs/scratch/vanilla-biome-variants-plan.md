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
