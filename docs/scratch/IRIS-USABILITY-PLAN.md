# Iris Overworld - Usability Plan
# Goal: Discovery and Building over Visual Density

Date: 2026-06-05
Status: Draft - pending implementation

---

## Design Philosophy

The Iris overworld pack is tuned for screenshots and engine showcases. Every setting that
makes a pack look impressive in a video works against a player who wants to chop wood,
find a build site, or navigate on foot. This plan converts the pack from "showcase density"
to "discovery and building" by applying three categories of change:

1. **Tree rate trims** - reduce oversized tree spawn rates so giants are landmarks, not wallpaper
2. **Rarity adjustments** - make rare biomes actually rare; make common biomes navigable
3. **Transition chains** - use Iris `children` nesting to create natural density gradients
   so players walk *into* dense zones rather than spawning inside them

The ADR-003 goals (distinct palette, recognizable silhouette, build theme anchor) remain
the acceptance bar for every biome. This plan does not relax those goals - it makes them
easier to achieve by ensuring the terrain is legible and the trees don't block the view.

---

## Part 1: Tree Rate Trims

### The rule

| Height range | Feel | Target spawn chance |
|---|---|---|
| h6-h25 | Normal, harvestable | 0.03-0.06 |
| h25-h45 | Tall, notable | 0.005-0.015 |
| h45-h65 | Landmark, harvestable with effort | 0.001-0.005 |
| h65+ | Once-in-a-session find | 0.0003-0.001 |

**Exception:** biomes where the trees ARE the environment (dark forest, redwood, sequoia,
cambian drift, the creaks, ether, magic forest) keep their original rates. Density is the
feature in those biomes, not a problem.

### Biomes to trim

| Biome file | Object | Current | Target | Reason |
|---|---|---|---|---|
| `frozen/tundra-winter.json` | tredwood1-9 | 0.35 | 0.12 | H=93-110, atmosphere ok but 0.35 is too dense |
| `frozen/tundra-winter.json` | tredwoodsmol1-7 | 0.45 | 0.20 | H=21-39, at 0.45 these dominate the biome |
| `tundra/redwood-forest.json` | tredwood1-9 | 0.35 | 0.12 | Same as winter forest |
| `tundra/redwood-extended-cliffs.json` | tredwood1-9 | 0.35 | 0.12 | Same |
| `tundra/redwood-extended-cliffs.json` | sup-pine-1-4 | 0.30 | 0.10 | H=39-66, sequoia on cliffs - reduce |
| `tundra/autumn.json` | tredwood1-9 | 0.35 | 0.12 | Autumn identity is pumpkins + color, not redwood density |
| `tundra/autumn-extended.json` | tredwood1-9 | 0.35 | 0.12 | Same |
| `temperate/birch-forest-extended.json` | antioch3 | 1.0 | 0.40 | Guaranteed spawn per chunk is too dense |
| `temperate/birch-tall.json` | largeponderosa1-2 | 0.39 | 0.15 | H=30-32, tall birch is the identity but 0.39 is overwhelming |
| `temperate/flower-forest.json` | birch/antioch3 | 1.0 | 0.40 | Same as birch-forest-extended |
| `temperate/flower-forest-extended.json` | birch/antioch3 | 1.0 | 0.40 | Same |
| `temperate/longtree-forest.json` | toak1-4 | 0.55 | 0.25 | toak H=15-20 is fine but 0.55 is wall-to-wall |
| `temperate/longtree-forest-extended.json` | toak1-4 | 0.55 | 0.25 | Same |
| `temperate/plateau.json` | birch/antioch3 | 1.0 | 0.50 | Plateau identity is terrain, not birch density |
| `temperate/plateau-extended.json` | birch/antioch3 | 1.0 | 0.50 | Same |
| `terralost/amethyst-canyon.json` | AmyLarge1-4 | 0.20 | 0.08 | H=70-88, crystal trees are environment but 0.20 is too dense |
| `terralost/amethyst-rainforest.json` | AmyLarge1-4 | 0.30 | 0.10 | Same, worse |
| `terralost/amethyst-rainforest.json` | AmyMed1-4 | 0.60 | 0.30 | H=21, fine height but 0.60 is dense |
| `tropical/rainforest-hills.json` | largegeneric1 | 0.035 | 0.008 | H=124 - the largest tree in the pack; 0.035 is far too common |
| `tropical/rainforest.json` | largegeneric1 | 0.010 | 0.004 | H=124, reduce even at lower base rate |
| `tropical/rainforest-wicked.json` | largegeneric1 | 0.010 | 0.004 | Same |

### Biomes explicitly NOT trimmed (atmosphere biomes)

These biomes have dense or large trees by design. The trees are the environment, not a
resource. Players are not expected to harvest them.

- `swamp/cambian-drift` + extended - talldrift dark oaks H=76-77 at 0.7 - keep
- `swamp/the-creaks` - roofed oaks at 1.0 - keep (rarity 15 means players rarely find it)
- `swamp/roofed-forest` + extended + wayward variants - keep
- `tundra/sequoia-redwoods` + extended - sup-pine at 0.69 - keep (sequoias ARE the biome)
- `tundra/ether` + extended - dotree at 0.8, rarity 13 - keep
- `tundra/magic-forest` + extended - dotree at 0.8, rarity 7 - keep
- `temperate/reaching-forest` + violet - dotree at 0.8, rarity 7 - keep

---

## Part 2: Rarity Adjustments

### The problem

Iris rarity is inverse: rarity 1 = most common, higher numbers = rarer. Several biomes
have rarity settings that make them either too common (diluting the world) or so rare
players never find them.

### Adjustments

| Biome | Current rarity | Target | Reason |
|---|---|---|---|
| `swamp/the-creaks` | 15 | 8 | Rarity 15 is near-impossible to find; 8 makes it a genuine discovery |
| `tundra/ether` | 13 | 9 | Same - players should be able to find it in a session |
| `tundra/magic-forest` | 7 | 5 | Rare enough to feel special, findable in a few hours |
| `temperate/reaching-forest` | 7 | 5 | Same |
| `frozen/vander` | 4 | 3 | Vander is a strong visual biome; slightly more common helps discovery |
| `terralost/amethyst-canyon` | 1 | 4 | Currently spawns as often as any common biome - needs to be a notable find |
| `terralost/amethyst-rainforest` | 1 | 5 | Currently spawns as often as any common biome - should be rarer than canyon |

Note: alpine-grove and alpine-highlands stay at rarity 1 - they serve as the outer transition
zone for the amethyst chain (Part 3) and should be common enough to find easily.

---

## Part 3: Transition Chains (Children Nesting)

### The concept

Instead of a player spawning directly inside a dense redwood forest or amethyst canyon,
use Iris `children` to create a gradient: sparse outer zone -> medium middle zone ->
dense/dramatic core. Players walk into the experience rather than being dropped into it.

### Recommended transition chains

#### Forest density gradient (temperate)
```
temperate/forest (standard oak, h6-h19 trees at normal rates)
  -> child: temperate/longtree-forest (toak h15-20 at 0.25 after trim)
      -> child: temperate/reaching-forest (dotree h47-51, rarity 5, rare cores)
```
childStyle: NOWHERE_SIMPLEX, zoom 5 for longtree patches; zoom 3 for reaching cores

#### Redwood approach (tundra)
```
tundra/bonsai-forest (bonsai med h12-18, tredwoodsmol at 0.75 - fine as-is)
  -> child: tundra/redwood-forest (tredwood at 0.12 after trim, tredwoodsmol at 0.20)
      -> child: tundra/sequoia-redwoods (sup-pine at 0.69 - full density core)
```
childStyle: NOWHERE_SIMPLEX, zoom 6 for redwood patches; zoom 4 for sequoia cores

#### Amethyst approach (terralost)
```
terralost/alpine-grove (pollup h6-8, no large objects)
  -> child: terralost/amethyst-canyon (AmyLarge at 0.08 after trim)
      -> child: terralost/amethyst-rainforest (AmyLarge at 0.10, AmyMed at 0.30 after trim)
```
childStyle: NOWHERE_SIMPLEX, zoom 5 for canyon patches; zoom 3 for rainforest cores

#### Swamp depth gradient
```
swamp/swamp-marsh (lgeneric jungle trees h~20 at 0.8 - fine)
  -> child: swamp/roofed-forest (dense dark oak - atmosphere)
      -> child: swamp/cambian-drift (talldrift dark oak h76-77 - deepest zone)
```
childStyle: NOWHERE_SIMPLEX, zoom 4 for roofed patches; zoom 3 for cambian cores

### Implementation notes

- Add `children` and `childStyle` to the OUTER biome JSON only
- Child biomes do not need modification - they inherit parent terrain
- `zoom` controls patch size: 3 = small rare cores, 6 = large gradual patches
- Child biome `rarity` is ignored - frequency is controlled by parent's `childStyle` zoom
- Test each chain in a fresh world before committing; use `/iris tp` to jump to biomes

---

## Part 4: Custom Tree Integration

Our generated `.iob` files (in `scripts/output/<biome>/`) are sized for harvestability.
The integration plan:

### Where our trees fit

| Our tree config | Target Iris biome | Role |
|---|---|---|
| `forest/` output | `temperate/forest` | Replace or supplement hoakgeneric at standard rates |
| `dark-forest/` output | `swamp/roofed-forest` | Supplement existing dark oaks at low rates (0.01-0.03) |
| `birch-forest/` output | `temperate/birch-forest` | Supplement antioch birch variants |
| `jungle/` output | `tropical/rainforest` | Replace largegeneric1 (H=124) at harvestable sizes |
| `cherry-grove/` output | `temperate/sakura-forest` | Supplement genericsak at lower rates |
| `savanna/` output | `hot/small-valley`, `mesa/valleys` | Supplement acacia variants |
| `taiga/` output | `frozen/pine-hills`, `mountain/forest` | Supplement pine/levergreen |
| `old-growth-pine-taiga/` output | `tundra/redwood-forest` | Supplement tredwoodsmol after trim |
| `old-growth-birch-forest/` output | `temperate/birch-tall` | Supplement largeponderosa after trim |

### Integration steps (per biome)

1. Copy the relevant `.iob` files from `scripts/output/<biome>/` into the Iris pack's
   `objects/trees/<category>/` folder on the server
2. Add object entries to the biome JSON referencing the new files
3. Set spawn chances using the height table from Part 1
4. Test in-world; adjust rates if the biome feels sparse or cluttered

### Naming convention for our objects

Our files follow: `<name>_<profile>_<trunk>_<leaves>_h<height>_s<seed>.iob`
When placing in the Iris pack, rename to a shorter form: `custom-<species>-h<height>-<n>.iob`
Example: `custom-oak-h12-1.iob`, `custom-oak-h19-2.iob`

---

## Part 5: Biomes to Skip or Defer

These biomes are excluded from the initial adoption pass:

| Biome | Reason |
|---|---|
| Any biome referencing `smoakog80`/`smoakog160` | H=75/60 at unknown rates; verify before adopting |
| Seasonal Biome Framework (Summer Strand etc.) | Requires region-swap system; phase-2 project |
| Geothermal Sulfur Caves | Custom mob/mechanic required; not vanilla-achievable |
| `tropical/submerged-volcanic` | Underwater biome; low build value for surface focus |

---

## Implementation Order

### Phase 1 - Apply trims (no new files, lowest risk)
- [x] Apply all rate changes from Part 1 to the live pack JSON files
- [x] Apply rarity adjustments from Part 2
- [ ] Test in a fresh world: walk each trimmed biome, verify giants are rare

### Phase 2 - Add transition chains
- [x] Add `children` + `childStyle` to the 4 outer biomes listed in Part 3
- [ ] Test each chain: verify gradient exists and cores are findable but not common

### Phase 3 - Integrate custom trees
- [x] Copy `.iob` files from `scripts/output/` to server pack `objects/trees/custom/`
- [x] Add object entries to target biome JSONs (Part 4 table)
- [x] Set rates using Part 1 height table
- [ ] Test each biome in-world

### Phase 4 - Catalog and document
- [x] Update `docs/design/CUSTOM-BIOMES.md` with final biome list
- [ ] Verify every adopted biome meets ADR-003 goals (palette, silhouette, build theme)
- [ ] Write one-sentence concept hook per biome

---

## Quick Reference: What NOT to Change

- All mesa biomes - terrain-only, no objects, adopt as-is
- All hot/mountain biomes - terrain-only, adopt as-is
- All mushroom biomes - mushroom-scale objects, adopt as-is
- Sequoia Redwoods - density IS the biome, do not trim
- The Creaks, Ether, Magic Forest, Reaching Forest - rare atmosphere biomes, do not trim
- Cambian Drift, Roofed Forest variants - dark forest logic applies, do not trim
- Frozen Vander - ice mushrooms + sproak are well-balanced, adopt as-is
- Bonsai Forest - bonsai med h12-18 is fully harvestable, adopt as-is
- Ancient Sands, Alpine Grove, Alpine Highlands - no large objects, adopt as-is
