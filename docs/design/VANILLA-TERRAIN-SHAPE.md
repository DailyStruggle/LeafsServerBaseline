# Vanilla Terrain Shape (and how to reach it in Iris v4)

Our vanilla-derivative biomes generate terrain that is far smoother and flatter
than the Minecraft worldgen they imitate. This doc records (1) the intended
vanilla shape per biome archetype, (2) what our overlay currently produces, and
(3) the concrete Iris **v4** levers that close the gap. It is the authoritative
reference for terrain-amplitude work on the vanilla biome set.

> Scope: terrain **shape** only - height band, relief frequency, slope/cliff
> character. Surface blocks, decoration, trees, and structures are out of scope.

> Layering rule still applies: never edit `iris/pack-base`. Every change below is
> a same-path override or new file under `iris/pack-overlay`. See the project
> guidelines (*Iris Pack Layering*).

---

## Why our terrain is too smooth (root cause)

Three compounding causes, all in our overlay - none are fixed by the 4.0 engine
upgrade itself (4.0 is a namespace/schema/structures/carving overhaul; it does
not touch terrain amplitude):

1. **Uniform low amplitude.** Almost every "flat" vanilla biome shares one band:
   `"generator": "plain", "min": 4, "max": 10` - only ~6 blocks of relief. Plains
   variants use `plain[3-16]`. Vanilla's lowland character comes from *frequent*
   small bumps and dips on top of slow continental drift, which a 6-block band
   cannot express.
2. **Heavy interpolation smoothing.** The shared generators interpolate with
   `BILINEAR_STARCAST_9` at `horizontalScale` 5-52. A wide horizontal scale
   smears the height field across a large window and erases exactly the
   high-frequency micro-relief vanilla has.
3. **Single-octave smooth noise styles.** `plain`/`highplains` use one
   `IRIS_DOUBLE` composite with a single low-frequency fracture, so there is no
   layered detail (vanilla stacks continentalness + erosion + peaks/valleys +
   detail noise).

## Iris v4 terrain-shape levers (the toolbox)

| Lever | Where | Effect on shape |
|-------|-------|-----------------|
| Biome `generators[].min`/`max` | `pack-overlay/biomes/.../*.json` | Sets the height **band** (amplitude) the biome's generator is stretched across. Widen for more relief. |
| `interpolator.function` | generator file | Smoothing kernel. `BILINEAR_STARCAST_9`/`_12` = heavy smoothing; `BILINEAR_STARCAST_3`, `BILINEAR`, or `NONE` preserve sharper variation. |
| `interpolator.horizontalScale` | generator file | Width of the smoothing window. **Lower = sharper** micro-relief; higher = smoother, broader landforms. |
| `composite[]` (multiple entries) | generator file | Stack noise octaves. Add a high-`zoom`, low-`opacity` entry for fine bumps on top of the base landform. |
| noise `style` + `zoom` + `exponent` | generator file | Landform character. High `zoom` = high frequency (small features); `exponent > 1` sharpens ridges/valleys; `negative` carves. |
| `fracture` (nested style + `multiplier`) | generator file | Domain-warps the noise so contours look organic instead of blobby. |
| `bezier` / `sinCentered` / `opacity` | generator composite | Reshapes the height curve and blends octaves. `bezier:true` rounds; drop it for crisper steps. |
| `cliffHeightMin`/`Max` + `cliffHeightGenerator` | generator file | Adds stepped cliffs/plateaus (the `*-cliffs` generators). The vanilla badlands/windswept look. |
| Region/dimension `*Style` + zoom | `regions/*.json`, `dimensions/overworld.json` | Controls how biomes blend at borders and the broad continental shape that biome bands ride on. |

General recipe for "vanilla feel": **widen the band a little**, **lower
`horizontalScale`**, and **add one high-frequency low-opacity `composite`
octave** for micro-bumps - rather than just cranking `max`, which only makes
biomes taller/spikier, not more vanilla.

---

## Intended shape by archetype

Vanilla heights below are approximate surface ranges (sea level = y63) for a
default world; they describe the *feel* to target, not exact noise-router
values. "Relief" = vertical variation across a few hundred blocks.

| Archetype | Vanilla biomes (our files) | Intended vanilla shape | Current band | Gap | v4 approach |
|-----------|----------------------------|------------------------|--------------|-----|-------------|
| Flat lowland | `forest`, `birch_forest`, `dark_forest`, `flower_forest`*, `jungle`, `bamboo_jungle`, `sparse_jungle`, `cherry_grove`, `savanna`, `mushroom_fields`, `old_growth_birch_forest` | Gently rolling, y63-72, ~8-12 blocks of *frequent* small undulation; occasional shallow hollows | `plain[4-10]` (~6) | Too flat, undulation too low-frequency | Overlay `plain` gen: `horizontalScale` 12 -> ~5-6; add 2nd `composite` octave (`zoom` ~2-3, `opacity` ~0.15) for micro-bumps; widen band to ~`3-14` |
| Open plains | `plains`, `plains__sparse`, `sunflower_plains`, `snowy_plains` | Broad, very gentle swells, y63-70; the flattest "calm" terrain but never billiard-flat | `plain[3-16]` | Band OK; relief too smeared | Same overlay `plain` gen fix (lower `horizontalScale`, add micro-octave). Keep band roughly as is |
| Wetland (near/below sea) | `swamp`, `swamp__giant`, `mangrove_swamp`, `puddle`, `lake` | Flat, at or just below y63, broken by shallow water pools; almost no relief | `plain[4-10]`, `mountain[-7..3]`, `mountain[-14..-4]`, `plain[-3..-1]` | Acceptable; slightly too smooth | Minor: keep low band; one tiny high-`zoom` octave for hummocks; ensure values straddle `fluidHeight` (75) for water pockets |
| Rolling hills / taiga | `taiga`, `snowy_taiga`, `old_growth_pine_taiga`, `old_growth_spruce_taiga`, `meadow`, `grove` | Rolling hills y65-85, moderate relief, smooth slopes | `rare-hills[5-22]`, `[6-26]`, `[8-28]` | Reasonable; a bit smooth | Slightly lower `horizontalScale`; widen to ~`6-30`; add mid-frequency octave |
| Windswept / jagged hills | `windswept_hills`, `windswept_gravelly_hills`, `windswept_forest`, `windswept_savanna`, `stony_shore` | Sharp, broken, cliff-stepped hills y70-110; steep faces | `rare-hills[15-45]`, `plain-cliffs[20-50]`, `small-cliffs[12-35/2-20]` | Cliffs present but soft | Use/keep `*-cliffs` gens with `bezier:false`, raise `exponent` (~1.2), tune `cliffHeightMin/Max`; lower `horizontalScale` for crispness |
| Plateau / badlands | `badlands`, `wooded_badlands`, `eroded_badlands`, `badlands__ashwood`, `savanna_plateau` | High flat-topped mesas y80-110 with steep eroded walls and spires | `highplains[40-70]`, `cracked-cliffs[70-98]` | Tops too rounded | Keep `cliff*` plateau gens; sharpen walls (`exponent`, drop `bezier`); flatten tops with a low-amplitude cap octave |
| Slopes / sub-alpine | `snowy_slopes`, `meadow__cloud` | Long mountain-flank slopes y90-160 connecting lowland to peaks | `mountain[30-70]`, `rare-hills[5-22]` | `snowy_slopes` OK; `meadow__cloud` too flat for its placement | Use `mountain` gen with wide band (~`30-90`); ensure neighboring peak biomes blend (region style) |
| Peaks | `jagged_peaks`, `frozen_peaks`, `stony_peaks` | Very tall, sharp ridgelines y120-256; dramatic vertical relief | `mountain-aggro[60-110]`, `mountain-large[65-105]` | Reasonable; can be sharper/taller | Keep aggressive gens; raise `max` toward ~`140-180`, `exponent` up, `BILINEAR_STARCAST_3`; layer a ridge octave (`negative`, high `exponent`) |
| Spikes / special | `ice_spikes` | Mostly flat snow field y65-80 with isolated tall spires (spires are objects, not terrain) | `highplains[30-45]` | Terrain a touch high/rolling | Lower to a flatter band (~`3-16` like plains); leave spikes to objects |
| Desert | `desert` | Gentle dunes y63-75, smooth and rounded | `smooth-dunes[8-24]` | Good | Leave; optionally one low-opacity dune octave |
| Ocean / shore | (ocean region biomes), `stony_shore` | Below sea floor, gentle; shores rise sharply at coast | `mountain[neg]`, `small-cliffs[2-20]` | Out of vanilla-folder scope | Tune in ocean region; shore cliffs via `small-cliffs` |

\* `flower_forest`/`flower_forest__young` currently use `mountain[6-28]`. This is
**not** a mismatch: in vanilla, flower forest is the positive-weirdness *variant*
of forest, so it is pinned to bumpier, more dissected terrain (micro-ridges and
water-filled valleys) than plain forest. The hillier Iris band emulates that
feel; keep it (and see *Weirdness-variant biomes* below).

---

## Weirdness-variant biomes ride bumpier terrain (don't flatten them)

A class of vanilla biomes are not their own climate cell - they are the
**positive-weirdness variant** of a calmer base biome that shares the same
temperature/humidity slot. In the 1.18+ multi-noise builder (`OverworldBiomeBuilder`)
the *sign* of weirdness selects the variant, while the *magnitude* of weirdness
feeds the peaks-and-valleys (PV) shaper. Because these variants are gated to
positive weirdness, they avoid the flattest (near-zero weirdness) terrain and
**trend rougher than their base biome**: more micro-relief, and valley floors that
dip under sea level and fill with water/lakes. This is a placement artifact, not a
per-biome terrain or lake generator.

Iris biome placement does not reproduce weirdness-gated variant selection, so the
faithful way to emulate the effect is to give each variant a **hillier generator
than its base**, ideally one whose low band undercuts `fluidHeight` (75) so valley
pools form.

| Variant biome (our file) | Vanilla base | Should be hillier than base? | Current band | Note |
|--------------------------|--------------|------------------------------|--------------|------|
| `flower_forest`, `flower_forest__young` | forest/plains | Yes | `mountain[6-28]` | Correct as-is; the rough/laky look is the variant effect. Consider a low band that dips below y75 for pools. |
| `sunflower_plains` | plains | Slightly | `plain[3-16]` | Mostly flat in practice (low PV); leave near plains. A touch more relief than `plains` is fine. |
| `old_growth_birch_forest` | birch_forest | Yes | `plain[4-10]` | On the same flat band as `birch_forest`; give it a mildly hillier gen (e.g. `rare-hills` low band) to read as the variant. |
| `old_growth_pine_taiga` | taiga / spruce taiga | Slightly | `rare-hills[6-26]` | Already on a hill gen (a touch above `taiga`'s `rare-hills[5-22]`); keep. |
| `ice_spikes` | snowy_plains | Mostly flat | `highplains[30-45]` | Vanilla is a flat snow field with object spires; this is the exception - flatten toward plains (per table). |
| `windswept_savanna` | savanna | Yes (eroded/shattered) | `plain-cliffs[15-45]` | Already cliff-driven (vs savanna's `plain[4-10]`); correct - keep the shattered look, sharpen per the windswept row. |
| `eroded_badlands` | badlands | Yes | `cracked-cliffs[70-98]` | Already cliff-driven; keep sharp (see Plateau/badlands row). |

Takeaway: only `ice_spikes` should actually be flattened among the variants; the
rest legitimately warrant relief at or above their base biome, so do **not** demote
them to the shared `plain` band when widening flat-lowland amplitude.

---

## Recommended implementation order

> **Status (2026-06-08): steps 1-4 implemented in the overlay.** The `plain`,
> `rare-hills`, and `highplains` generators now carry a high-frequency,
> low-`opacity` detail octave (and lighter `horizontalScale`); flat-lowland
> forest/jungle/savanna bands were widened `plain[4-10]` -> `plain[3-16]`;
> `ice_spikes` was flattened to `plain[3-16]`; and `old_growth_birch_forest` was
> promoted to `rare-hills[6-24]`. Still needs fresh-chunk verification on a
> user-booted server.

Do this as an approved, multi-file change (Rule D-005), verified on **fresh**
terrain (Iris never regenerates existing chunks) on a **user-booted** server.

1. **Overlay `plain` generator** (`pack-overlay/generators/plain.json`, copy base
   whole): drop `interpolator.horizontalScale` to ~5-6 and add a second
   high-`zoom` low-`opacity` `composite` octave. This single change lifts most
   flat-lowland and plains biomes at once.
2. **Overlay `rare-hills` and `highplains`** the same way (lighter smoothing +
   detail octave) for the rolling-hill and plateau groups.
3. **Sharpen the `*-cliffs` family**: `bezier:false`, `exponent` ~1.2, tuned
   `cliffHeightMin/Max` for badlands and windswept biomes.
4. **Per-biome band tweaks**: widen flat-lowland bands to ~`3-14`; raise peak
   `max`; flatten `ice_spikes`; resolve the `flower_forest` mismatch.
5. **Re-derive trees/objects only if band changes** alter where surface objects
   anchor (check tall biomes).

Each generator edit is whole-file in the overlay; each biome edit is a small
`min`/`max` change. Deploy with `iris/scripts/deploy-iris-pack.ps1`, then pause
and ask the user to boot and inspect fresh chunks.

## See also

- [`../world-design/IRIS-4.0-MIGRATION.md`](../world-design/IRIS-4.0-MIGRATION.md) - what 4.0 changed (and did not)
- [`CUSTOM-BIOMES.md`](CUSTOM-BIOMES.md) - biome JSON schema reference
- [`BIOMES.md`](BIOMES.md) - vanilla biome ID reference
