# Tree Generator Integration Plan
# Leveraging scripts/output/ .iob files to adapt Iris overworld biomes

Date: 2026-06-05
Status: Draft - ready for implementation

This plan is the execution layer beneath `IRIS-USABILITY-PLAN.md`. That doc defines
*what* to change and *why*. This doc defines *how* to do it using our generated trees.

---

## Key principle: classify by species + height tier, not filename

Our generator produces filenames like `oak_oak_oak_log_oak_leaves_h12_s3201.iob`.
The seed suffix changes every time the config is re-run. **Never reference exact filenames
in biome JSONs.** Instead:

1. After generation, rename output files to a stable short form before copying to the pack:
   `custom-<species>-<tier>-<n>.iob`
   Example: `custom-oak-med-1.iob`, `custom-oak-med-2.iob`, `custom-spruce-tall-1.iob`
2. Use the height tiers below to decide which generated files map to which tier label.
3. Place renamed files in `objects/trees/custom/` inside the Iris pack.
4. Reference them in biome JSONs as `trees/custom/custom-<species>-<tier>-<n>`.

### Height tier labels

| Label | Height range | Harvestable? | Spawn rate guideline |
|---|---|---|---|
| `smol` | h5-h12 | Yes, easily | 0.30-0.60 |
| `med` | h13-h25 | Yes | 0.10-0.30 |
| `tall` | h26-h45 | With scaffolding | 0.02-0.08 |
| `large` | h46-h70 | Atmosphere only | 0.003-0.010 |
| `giant` | h71+ | Atmosphere only | 0.0003-0.002 |

---

## Per-species output inventory

These are the height tiers available from each tree-config. Exact filenames vary per run;
use the tier label system above when renaming for the pack.

### oak (forest/)
- smol: h7-h12 (5 variants across 3 profiles: oak, leaning, gnarled)
- med: h13-h20 (6 variants: oak, leaning, gnarled, windswept)
- tall: h20-h34 (3 variants: oak)
- large: h70-h88 (ancient_oak - 1 variant)
- giant: h88-h108 (ancient_oak - 1 variant)

### birch (birch-forest/)
- smol: h5-h12 (4 variants)
- med: h12-h21 (5 variants)
- tall: h22-h35 (4 variants)

### birch tall (old-growth-birch-forest/)
- med: h14-h21 (3 variants)
- tall: h22-h36 (3 variants)
- large: h34-h55 (2 variants)

### cherry (cherry-grove/)
- smol: h8-h13 (4 variants)
- med: h13-h22 (5 variants)
- tall: h40-h58 (3 variants)
- large: h55-h75 (3 variants)
- giant: h72-h112 (4 variants)

### dark oak (dark-forest/)
- smol: h7-h12 (4 variants: darkoak, twisted)
- med: h12-h22 (3 variants: darkoak, twisted)
- large: h55-h75 (titan - 1 variant)
- giant: h72-h95 (titan - 1 variant; spiral h50-h88 - 2 variants)

### jungle (jungle/)
- med: h20-h40 (3 variants: jungle)
- tall: h40-h70 (3 variants: jungle, snake)
- large: h52-h88 (3 variants: jungle, serpentine, snake)
- giant: h82-h108 (2 variants: jungle, serpentine)

### spruce/taiga (taiga/)
- smol: h10-h16 (3 variants)
- med: h18-h32 (3 variants)
- tall: h32-h52 (2 variants)
- large: h52-h68 (1 variant)
- giant: h65-h82 (1 variant)

### spruce/OG pine (old-growth-pine-taiga/)
- med: h20-h34 (2 variants)
- tall: h32-h58 (3 variants)
- large: h55-h72 (1 variant)
- giant: h68-h88 (1 variant)

### acacia (savanna/)
- smol: h5-h12 (3 variants: acacia, savanna_acacia)
- med: h12-h28 (4 variants: acacia, savanna_acacia)
- tall: h28-h48 (3 variants: acacia, savanna_acacia)

---

## Per-biome integration table

For each biome: what Iris object is being replaced or supplemented, what our tier
provides, and the target spawn chance.

### temperate/forest (oak supplement)

Goal: add variety alongside existing `hoakgeneric` Iris oaks.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| oak smol x3 | `trees/custom/custom-oak-smol-1` through `-3` | 0.04 each | Blend with hoakgeneric |
| oak med x3 | `trees/custom/custom-oak-med-1` through `-3` | 0.02 each | Taller variety |
| oak tall x2 | `trees/custom/custom-oak-tall-1` through `-2` | 0.005 each | Notable finds |
| oak large x1 | `trees/custom/custom-oak-large-1` | 0.0005 | Ancient oak - once per region |

Do not remove hoakgeneric - our trees supplement it. Total added chance ~0.15.

---

### temperate/birch-forest + birch-forest-extended (birch supplement + density fix)

`birch-forest-extended` has `antioch3` at 1.0 (trim target: 0.40). After trimming,
fill the gap with our birch variants.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| birch smol x3 | `trees/custom/custom-birch-smol-1` through `-3` | 0.05 each | Base density |
| birch med x3 | `trees/custom/custom-birch-med-1` through `-3` | 0.02 each | Mid-height variety |

Apply to both `birch-forest.json` and `birch-forest-extended.json`.
In extended: trim antioch3 to 0.40 first, then add our entries.

---

### temperate/birch-tall (tall birch supplement)

`largeponderosa` trim target: 0.39 -> 0.15. Supplement with old-growth-birch tiers.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| birch-tall med x2 | `trees/custom/custom-birch-tall-med-1` through `-2` | 0.06 each | Fills gap left by trim |
| birch-tall tall x2 | `trees/custom/custom-birch-tall-tall-1` through `-2` | 0.02 each | Tall birch identity |
| birch-tall large x1 | `trees/custom/custom-birch-tall-large-1` | 0.005 | Landmark tall birch |

---

### temperate/flower-forest + flower-forest-extended (birch density fix)

Same antioch3 issue as birch-forest-extended. Trim to 0.40, add birch smol/med.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| birch smol x2 | `trees/custom/custom-birch-smol-1` through `-2` | 0.04 each | Reuse birch-forest set |
| birch med x1 | `trees/custom/custom-birch-med-1` | 0.015 | Occasional taller birch |

---

### temperate/plateau + plateau-extended (birch density fix)

antioch3 at 1.0 -> 0.50. Plateau identity is terrain; trees are accent only.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| birch smol x2 | `trees/custom/custom-birch-smol-1` through `-2` | 0.03 each | Sparse accent |

---

### temperate/longtree-forest + longtree-forest-extended (oak tall supplement)

`toak` trim target: 0.55 -> 0.25. toak H=15-20 is fine height; just too dense.
Our oak med/tall fills the visual gap.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| oak med x2 | `trees/custom/custom-oak-med-1` through `-2` | 0.04 each | Variety alongside toak |
| oak tall x1 | `trees/custom/custom-oak-tall-1` | 0.01 | Occasional landmark |

---

### tropical/rainforest + rainforest-hills + rainforest-wicked (largegeneric replacement)

`largegeneric1` H=124 is the largest tree in the pack. Trim targets:
- rainforest: 0.010 -> 0.004
- rainforest-hills: 0.035 -> 0.008
- rainforest-wicked: 0.010 -> 0.004

Replace the removed density with harvestable jungle tiers.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| jungle med x3 | `trees/custom/custom-jungle-med-1` through `-3` | 0.04 each | Base canopy |
| jungle tall x2 | `trees/custom/custom-jungle-tall-1` through `-2` | 0.015 each | Mid canopy |
| jungle large x1 | `trees/custom/custom-jungle-large-1` | 0.004 | Emergent layer |
| jungle giant x1 | `trees/custom/custom-jungle-giant-1` | 0.0008 | Rare emergent - replaces largegeneric role |

Apply same entries to all three rainforest variants. Adjust base chance for
rainforest-hills (slightly higher density is appropriate for hills terrain).

---

### tundra/redwood-forest + redwood-extended-cliffs + autumn + autumn-extended (tredwood trim)

`tredwood` trim target: 0.35 -> 0.12. `tredwoodsmol` trim target: 0.45 -> 0.20.
Our OG pine spruce fills the gap left by the trim.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| spruce-og med x2 | `trees/custom/custom-spruce-og-med-1` through `-2` | 0.06 each | Fills tredwoodsmol gap |
| spruce-og tall x2 | `trees/custom/custom-spruce-og-tall-1` through `-2` | 0.02 each | Mid-height spruce |
| spruce-og large x1 | `trees/custom/custom-spruce-og-large-1` | 0.004 | Tall spruce landmark |

For `redwood-extended-cliffs` also: `sup-pine` trim 0.30 -> 0.10. No replacement
needed - the cliff terrain provides visual interest without dense trees.

---

### frozen/tundra-winter (tredwood + tredwoodsmol trim)

Same trim targets as redwood-forest. Same OG pine supplement applies.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| spruce-og med x2 | `trees/custom/custom-spruce-og-med-1` through `-2` | 0.05 each | Winter forest base |
| spruce-og tall x1 | `trees/custom/custom-spruce-og-tall-1` | 0.015 | Occasional tall spruce |

---

### swamp/roofed-forest variants (dark oak supplement)

Not a trim biome - atmosphere biome, keep density. Our dark oak adds variety
to the existing Iris dark oak pool without changing overall density.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| darkoak smol x2 | `trees/custom/custom-darkoak-smol-1` through `-2` | 0.03 each | Variety in base canopy |
| darkoak med x2 | `trees/custom/custom-darkoak-med-1` through `-2` | 0.015 each | Mid canopy variety |

Do not add titan/spiral variants here - the Iris pack already has those at correct rates.

---

### temperate/sakura-forest + osaka-violet-forest (cherry supplement)

These biomes use `genericsak` (sakura) and `hoakgeneric`. Our cherry trees add
variety at harvestable sizes.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| cherry smol x2 | `trees/custom/custom-cherry-smol-1` through `-2` | 0.03 each | Small cherry accent |
| cherry med x2 | `trees/custom/custom-cherry-med-1` through `-2` | 0.015 each | Mid cherry variety |

Do not add cherry tall/large/giant - these biomes are human-scale by design.

---

### tundra/maple-forest (our custom biome - full tree set)

This is our own biome with no Iris tree objects yet. Use oak and birch tiers
to build a deciduous autumn forest at harvest scale.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| oak smol x3 | `trees/custom/custom-oak-smol-1` through `-3` | 0.05 each | Base canopy |
| oak med x3 | `trees/custom/custom-oak-med-1` through `-3` | 0.025 each | Mid canopy |
| birch smol x2 | `trees/custom/custom-birch-smol-1` through `-2` | 0.04 each | Birch mix |
| birch med x2 | `trees/custom/custom-birch-med-1` through `-2` | 0.02 each | Taller birch |
| oak tall x1 | `trees/custom/custom-oak-tall-1` | 0.005 | Occasional landmark oak |

---

### hot/small-valley + mesa/valleys (acacia supplement)

These biomes have sparse or no acacia. Our acacia adds character.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| acacia smol x2 | `trees/custom/custom-acacia-smol-1` through `-2` | 0.03 each | Sparse valley acacia |
| acacia med x1 | `trees/custom/custom-acacia-med-1` | 0.01 | Occasional taller acacia |

---

### frozen/pine-hills + mountain/forest (spruce supplement)

These biomes use `levergreen` and `pine` Iris spruce. Our taiga spruce adds variety.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| spruce smol x2 | `trees/custom/custom-spruce-smol-1` through `-2` | 0.04 each | Base spruce variety |
| spruce med x2 | `trees/custom/custom-spruce-med-1` through `-2` | 0.02 each | Mid spruce |

---

### frozen/hills + frozen/hills-extended + frozen/plains + frozen/pine-plains (spruce variety)

These biomes use only `lfrostgeneric` or `pine` Iris spruce at a single chance value (0.12-0.62).
Our taiga spruce adds shape variety without changing overall density.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| spruce smol x2 | `trees/custom/custom-spruce-smol-1` through `-2` | 0.03 each | Shape variety alongside lfrostgeneric |
| spruce med x1 | `trees/custom/custom-spruce-med-1` | 0.015 | Occasional taller spruce |

Apply same entries to all four biomes. Do not raise total density - these are supplement entries only.

---

### frozen/spruce-hills + frozen/spruce-hills-extended + frozen/spruce-plains + frozen/pines (spruce variety)

These biomes use `levergreen` + `mevergreen` at 0.5-0.8. Dense spruce is the identity; add variety only.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| spruce smol x2 | `trees/custom/custom-spruce-smol-1` through `-2` | 0.02 each | Low-rate variety, density already high |
| spruce med x1 | `trees/custom/custom-spruce-med-1` | 0.01 | Occasional shape break |

---

### tundra/taiga + tundra/taiga-extended (spruce supplement)

These biomes use `tredwoodsmol` at 0.25 (large, trim target) and `pine` at 0.05.
After trimming tredwoodsmol, our spruce fills the gap at the right scale.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| spruce smol x3 | `trees/custom/custom-spruce-smol-1` through `-3` | 0.04 each | Replaces tredwoodsmol density |
| spruce med x2 | `trees/custom/custom-spruce-med-1` through `-2` | 0.02 each | Mid spruce layer |

Note: tredwoodsmol trim target for taiga is 0.25 -> 0.08 (same rule as redwood-forest).

---

### tundra/mountains + tundra/mountains-extended-cliffs + tundra/forest-extended-cliffs (spruce supplement)

These biomes use `levergreen` + `pine` at 0.125-0.8. Mountain spruce is the identity; add variety.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| spruce smol x2 | `trees/custom/custom-spruce-smol-1` through `-2` | 0.025 each | Shape variety |
| spruce med x1 | `trees/custom/custom-spruce-med-1` | 0.01 | Occasional taller spruce |

---

### fields/mountain-spruce-frosty + fields/mountain-spruce-winter (and extended variants) (spruce variety)

These biomes use `vgeneric` spruce at 1.0 - a single Iris type at full density.
Our spruce adds shape variety without raising density.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| spruce smol x2 | `trees/custom/custom-spruce-smol-1` through `-2` | 0.04 each | Shape variety |
| spruce med x1 | `trees/custom/custom-spruce-med-1` | 0.015 | Occasional taller form |

Apply to all four variants (frosty, frosty-extended, winter, winter-extended).

---

### temperate/oak-forest + oak-forest-extended + oak-forest-flat (oak supplement)

These biomes use `hoakgeneric` at 0.9 and `mixed/tredwood` at 0.001 (already very rare, fine).
Our oak adds shape variety to the dominant hoakgeneric pool.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| oak smol x3 | `trees/custom/custom-oak-smol-1` through `-3` | 0.04 each | Shape variety alongside hoakgeneric |
| oak med x2 | `trees/custom/custom-oak-med-1` through `-2` | 0.02 each | Taller oak variety |
| oak tall x1 | `trees/custom/custom-oak-tall-1` | 0.004 | Occasional landmark |

Apply to all three variants.

---

### temperate/combo-forest + combo-forest-extended (oak + birch supplement)

These biomes mix `hoakgeneric` oak at 0.18-1.0 and `antioch3` birch at 0.9.
Our oak and birch add shape variety to both pools.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| oak smol x2 | `trees/custom/custom-oak-smol-1` through `-2` | 0.03 each | Oak variety |
| oak med x1 | `trees/custom/custom-oak-med-1` | 0.015 | Taller oak |
| birch smol x2 | `trees/custom/custom-birch-smol-1` through `-2` | 0.03 each | Birch variety |
| birch med x1 | `trees/custom/custom-birch-med-1` | 0.015 | Taller birch |

---

### temperate/croak (oak supplement)

Uses `croak` oak at 0.49 - a single Iris type. Our oak smol/med adds variety.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| oak smol x2 | `trees/custom/custom-oak-smol-1` through `-2` | 0.04 each | Shape variety |
| oak med x1 | `trees/custom/custom-oak-med-1` | 0.015 | Occasional taller oak |

---

### temperate/roughplains (oak supplement)

Uses `hoakgeneric` at 0.5 and `mixed/tredwood` at 0.0025 (already rare, keep).
Plains context - keep additions sparse.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| oak smol x2 | `trees/custom/custom-oak-smol-1` through `-2` | 0.025 each | Sparse variety |
| oak med x1 | `trees/custom/custom-oak-med-1` | 0.008 | Occasional taller oak |

---

### tropical/plains + tropical/plains-hills (jungle supplement)

These biomes use `lgeneric` jungle at 0.4-0.71 and `coco` at 0.632.
Our jungle med adds canopy variety at harvestable scale.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| jungle med x2 | `trees/custom/custom-jungle-med-1` through `-2` | 0.03 each | Canopy variety |
| jungle tall x1 | `trees/custom/custom-jungle-tall-1` | 0.008 | Occasional taller tree |

---

### tropical/mountain + tropical/mountain-middle + tropical/mountain-extreme + tropical/mountain-plains (jungle supplement)

These biomes use `lgeneric` jungle at 0.489-0.9. Mountain jungle - dense is appropriate,
but our jungle med/tall adds shape variety.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| jungle med x2 | `trees/custom/custom-jungle-med-1` through `-2` | 0.03 each | Shape variety |
| jungle tall x1 | `trees/custom/custom-jungle-tall-1` | 0.01 | Occasional emergent |

Apply to all four mountain variants.

---

### tropical/bamboo-forest (jungle supplement)

Uses `lgeneric` jungle at 0.35 and `bamboo` at 0.5. Our jungle smol/med adds tree variety
alongside the bamboo without competing with it.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| jungle med x2 | `trees/custom/custom-jungle-med-1` through `-2` | 0.025 each | Tree variety among bamboo |

---

### swamp/willow-forest + willow-forest-extended (dark oak supplement)

These biomes use `talldrift` dark oak at 0.7 and `generic` dark oak at 0.2.
Our darkoak smol/med adds variety to the base canopy layer.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| darkoak smol x2 | `trees/custom/custom-darkoak-smol-1` through `-2` | 0.025 each | Base canopy variety |
| darkoak med x1 | `trees/custom/custom-darkoak-med-1` | 0.01 | Mid canopy variety |

---

### swamp/cambian-drift + cambian-drift-extended (dark oak supplement)

Atmosphere biome - `talldrift` dark oak at 0.7. Our darkoak smol/med adds variety
to the base layer without disrupting the gothic atmosphere.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| darkoak smol x2 | `trees/custom/custom-darkoak-smol-1` through `-2` | 0.02 each | Base variety |
| darkoak med x1 | `trees/custom/custom-darkoak-med-1` | 0.008 | Occasional mid-height |

---

### mountain/mplain-extended (oak supplement)

Uses `truegeneric` oak at 0.07 and `lponderosa` oak at 0.28. Our oak smol/med
adds variety alongside the existing ponderosa pool.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| oak smol x2 | `trees/custom/custom-oak-smol-1` through `-2` | 0.03 each | Variety alongside lponderosa |
| oak med x1 | `trees/custom/custom-oak-med-1` | 0.012 | Taller oak variety |

---

### mountain/floating-islands (oak + spruce supplement)

Our custom biome uses `hoakgeneric` at 0.04 and `levergreen` at 0.008 - very sparse by design.
Add a small number of our oak/spruce to increase variety without raising density.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| oak smol x2 | `trees/custom/custom-oak-smol-1` through `-2` | 0.015 each | Sparse oak variety |
| spruce smol x1 | `trees/custom/custom-spruce-smol-1` | 0.005 | Occasional spruce |

---

### savanna/savanna + savanna/cliff + savanna/cliff-extended (acacia supplement)

These biomes use `savannaD/F/S` acacia at 0.04-0.2. Our acacia smol/med adds
shape variety to the existing acacia pool.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| acacia smol x2 | `trees/custom/custom-acacia-smol-1` through `-2` | 0.025 each | Shape variety |
| acacia med x1 | `trees/custom/custom-acacia-med-1` | 0.01 | Occasional taller acacia |

Apply to all three savanna variants.

---

### mesa/plateau-dirt + mesa/plateau-dirt-high (acacia supplement)

Uses `vexed` acacia at 0.2. Mesa plateau - sparse acacia is the identity, keep additions minimal.

| Our tier | Object path | Chance | Notes |
|---|---|---|---|
| acacia smol x1 | `trees/custom/custom-acacia-smol-1` | 0.02 | Sparse variety |
| acacia med x1 | `trees/custom/custom-acacia-med-1` | 0.006 | Occasional taller acacia |

---

## Rename script

Before copying to the pack, run a rename pass on the output directory.
The script should:
1. Parse the height from the filename (`_h<N>_`)
2. Assign a tier label based on the height tier table above
3. Group by species (first segment of filename)
4. Rename to `custom-<species>-<tier>-<n>.iob` with sequential `n` per species+tier

A PowerShell rename script should be created at `scripts/rename-for-pack.ps1`.
It takes `--source <output-subdir>` and `--species <label>` and outputs renamed
copies to `scripts/output/renamed/<species>/`.

---

## Implementation order

### Step 1 - Apply rate trims to live pack (no new files)
Apply all trim targets from IRIS-USABILITY-PLAN.md Part 1 to the live pack JSONs.
This is safe and reversible. Do this first so the baseline is correct before adding trees.

### Step 2 - Build rename script
Create `scripts/rename-for-pack.ps1`. Test on one species (oak) before running all.

### Step 3 - Regenerate and rename
Re-run `generate_tree.py` for each config, then run the rename script per species.
Output lands in `scripts/output/renamed/<species>/`.

### Step 4 - Copy to pack
Copy renamed `.iob` files to `objects/trees/custom/` in the live Iris pack.
The `objects/` folder does not currently exist in the pack - create it.

### Step 5 - Update biome JSONs
For each biome in the per-biome table above, add the object entries.
Work one biome at a time. Test in-world before moving to the next.

### Step 6 - Transition chains
After trees are in place, add `children` + `childStyle` per IRIS-USABILITY-PLAN.md Part 3.
Transition chains depend on the trimmed rates being correct first (Step 1).

---

## What we are NOT doing with the generator

- **Dark forest titans/spirals** - the Iris pack already has these at correct rates for
  an atmosphere biome. We do not replace them.
- **Sequoia redwoods** - density is the point; no trim, no replacement.
- **Amethyst canyon/rainforest Amy trees** - these are dark oak + mixed leaves, not
  a species our generator currently produces. Rate trim only; no replacement.
- **largegeneric1 (H=124) direct replacement** - we replace the *density gap* left by
  trimming it, not the tree itself. Our jungle giant (h82-h108) fills the emergent role
  at a harvestable-adjacent height.
- **Any biome in the "do not change" list** from IRIS-USABILITY-PLAN.md.
