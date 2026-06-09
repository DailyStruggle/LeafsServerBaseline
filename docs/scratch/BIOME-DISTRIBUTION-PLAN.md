# Biome Distribution Plan
# Goal: 50-80% Vanilla | Vanilla-Adjacent Second | Custom Third | Iris Rare

Date: 2026-06-05
Status: PARTIALLY IMPLEMENTED 2026-06-07 - the custom (Tier 3) biome layer is now live: 11 custom biomes (5 volcano families + 6 new biomes) authored into `iris/pack-overlay/biomes/` and wired into region biome lists; deployed and reference-validated. Tier ratios / rarity tuning remain to be confirmed in-world (user-triggered server boot). See `docs/scratch/BIOME-ADDITIONS-EXECUTION-SEQUENCE.md`.

---

## Design Intent

The world should feel like Minecraft first. Players spawn in recognizable terrain, gather
familiar resources, and navigate by landmarks they already understand. Custom content is
a discovery layer on top of that foundation - not the default experience.

### Tier definitions

| Tier | Target share | What belongs here |
|---|---|---|
| **T1 - Vanilla** | 50-80% | Pure passthrough biomes: `derivative` + `vanillaDerivative` pointing at a real `minecraft:*` biome, minimal or no custom objects |
| **T2 - Vanilla-adjacent** | 10-30% | Custom biomes that feel like a natural extension of vanilla - same palette, same scale, slight twist. Includes small/medium/large scale variants of vanilla types and our custom-tree versions of vanilla forests |
| **T3 - Custom thematic** | 5-15% | Distinctive non-vanilla biomes with a clear identity: volcanic families, dead/corrupted, crystal, alien. Rare enough to feel like a find |
| **T4 - Pre-existing Iris showcase** | 5-10% | The most visually extreme Iris biomes (Ether, Magic Forest, Reaching Forest, Cambian Drift, Amethyst families, Sequoia Redwoods). Kept for discovery value but pushed to genuine rarity |

---

## The Core Problem (from prior research)

The `vanilla/*` biome files **do not exist** in the pack. Every `vanilla/forest`,
`vanilla/plains`, etc. entry in the region JSONs silently fails to load, giving vanilla
biomes an effective frequency of **0%**. Fixing this is the prerequisite for everything
else.

---

## Phase 1 - Create the vanilla/ biome library (prerequisite)

### What to build

A `iris/pack-base/biomes/vanilla/` folder containing one thin passthrough JSON per
vanilla biome type we want to use. Minimal format:

```json
{
  "name": "Forest",
  "rarity": 1,
  "derivative": "minecraft:forest",
  "vanillaDerivative": "minecraft:forest"
}
```

No custom objects. No terrain overrides. Pure vanilla feel.

### Required vanilla biome files (by region need)

#### Temperate / Forests
- `vanilla/plains.json` - `minecraft:plains`
- `vanilla/sunflower_plains.json` - `minecraft:sunflower_plains`
- `vanilla/forest.json` - `minecraft:forest`
- `vanilla/flower_forest.json` - `minecraft:flower_forest`
- `vanilla/birch_forest.json` - `minecraft:birch_forest`
- `vanilla/old_growth_birch_forest.json` - `minecraft:old_growth_birch_forest`
- `vanilla/dark_forest.json` - `minecraft:dark_forest`
- `vanilla/windswept_forest.json` - `minecraft:windswept_forest`
- `vanilla/meadow.json` - `minecraft:meadow`
- `vanilla/cherry_grove.json` - `minecraft:cherry_grove`

#### Frozen / Tundra / Taiga
- `vanilla/taiga.json` - `minecraft:taiga`
- `vanilla/snowy_taiga.json` - `minecraft:snowy_taiga`
- `vanilla/old_growth_pine_taiga.json` - `minecraft:old_growth_pine_taiga`
- `vanilla/old_growth_spruce_taiga.json` - `minecraft:old_growth_spruce_taiga`
- `vanilla/snowy_plains.json` - `minecraft:snowy_plains`
- `vanilla/snowy_slopes.json` - `minecraft:snowy_slopes`
- `vanilla/frozen_peaks.json` - `minecraft:frozen_peaks`
- `vanilla/jagged_peaks.json` - `minecraft:jagged_peaks`
- `vanilla/windswept_hills.json` - `minecraft:windswept_hills`
- `vanilla/windswept_gravelly_hills.json` - `minecraft:windswept_gravelly_hills`
- `vanilla/grove.json` - `minecraft:grove`
- `vanilla/ice_spikes.json` - `minecraft:ice_spikes`

#### Hot / Savanna / Mesa
- `vanilla/desert.json` - `minecraft:desert`
- `vanilla/savanna.json` - `minecraft:savanna`
- `vanilla/savanna_plateau.json` - `minecraft:savanna_plateau`
- `vanilla/windswept_savanna.json` - `minecraft:windswept_savanna`
- `vanilla/badlands.json` - `minecraft:badlands`
- `vanilla/eroded_badlands.json` - `minecraft:eroded_badlands`
- `vanilla/wooded_badlands.json` - `minecraft:wooded_badlands`

#### Tropical / Jungle / Swamp
- `vanilla/jungle.json` - `minecraft:jungle`
- `vanilla/sparse_jungle.json` - `minecraft:sparse_jungle`
- `vanilla/bamboo_jungle.json` - `minecraft:bamboo_jungle`
- `vanilla/swamp.json` - `minecraft:swamp`
- `vanilla/mangrove_swamp.json` - `minecraft:mangrove_swamp`

#### Mountain
- `vanilla/stony_peaks.json` - `minecraft:stony_peaks`
- `vanilla/stony_shore.json` - `minecraft:stony_shore`
- `vanilla/mushroom_fields.json` - `minecraft:mushroom_fields`

**Total: ~37 vanilla biome files.** All are thin JSONs - this is a low-risk, high-impact
batch creation.

---

## Phase 2 - Rebuild region landBiomes lists

Each region's `landBiomes` array is rebuilt to hit the tier targets. The mechanism:
- More slots = more frequency (Iris picks uniformly from the list)
- Lower `rarity` on the biome JSON = more common within its slot
- Vanilla biomes get `rarity: 1`; T2 variants get `rarity: 2-3`; T3/T4 get `rarity: 4-8`

### Per-region targets and slot allocations

#### temperate (target: 55% V / 25% T2 / 15% T3 / 5% T4)

**T1 vanilla (11 slots):**
`vanilla/plains` x3, `vanilla/forest` x2, `vanilla/flower_forest`, `vanilla/birch_forest`,
`vanilla/dark_forest`, `vanilla/meadow`, `vanilla/cherry_grove`, `vanilla/sunflower_plains`

**T2 vanilla-adjacent (5 slots):**
`temperate/plains`, `temperate/birch-forest`, `temperate/oak-forest`,
`temperate/birch-thin`, `temperate/sakura-forest`

**T3 custom thematic (3 slots):**
`temperate/wilds`, `temperate/stranged-plains`, `temperate/croak`

**T4 rare Iris (1 slot):**
`temperate/reaching-forest` (rarity 5 on the biome JSON)

**Remove:** `temperate/combo-forest`, `temperate/lush-plains`, `temperate/osaka-violet-forest`,
`temperate/osaka-red-forest`, `temperate/roughplains`, `temperate/calmplains`,
`temperate/fancyplains`, `temperate/cherry-blossom-forest`, `mountain/plains`,
`mountain/Cute_Cliffs`, `temperate/oak-forest-flat`

---

#### forests (target: 60% V / 20% T2 / 15% T3 / 5% T4)

**T1 vanilla (12 slots):**
`vanilla/forest` x3, `vanilla/birch_forest` x2, `vanilla/old_growth_birch_forest` x2,
`vanilla/dark_forest` x2, `vanilla/windswept_forest`, `vanilla/flower_forest`,
`vanilla/old_growth_pine_taiga`

**T2 vanilla-adjacent (4 slots):**
`temperate/longtree-forest`, `temperate/birch-forest`, `temperate/oak-forest`,
`temperate/birch-tall` (if exists, else swap for another)

**T3 custom thematic (3 slots):**
`temperate/spiral-crown-forest` (our custom tree biome), `swamp/roofed-forest`,
`temperate/reaching-forest-violet`

**T4 rare Iris (1 slot):**
`temperate/reaching-forest` (rarity 5)

**Remove:** `temperate/combo-forest`, `temperate/osaka-red-forest`, `mountain/forest`,
`temperate/wilds` (move to temperate region)

---

#### frozen (target: 55% V / 25% T2 / 15% T3 / 5% T4)

**T1 vanilla (11 slots):**
`vanilla/snowy_plains` x3, `vanilla/snowy_taiga` x2, `vanilla/frozen_peaks` x2,
`vanilla/windswept_hills`, `vanilla/windswept_gravelly_hills`, `vanilla/ice_spikes`,
`vanilla/grove`

**T2 vanilla-adjacent (5 slots):**
`frozen/pine-hills`, `frozen/boreal-shield`, `frozen/tundra-winter`,
`frozen/spruce-plains`, `frozen/pine-plains`

**T3 custom thematic (3 slots):**
`frozen/frostpeak` (volcanic family - extra rare), `frozen/ashcrown` (if cold variant),
`frozen/vander`

**T4 rare Iris (1 slot):**
`tundra/ether` (rarity 9)

---

#### tundra (target: 50% V / 30% T2 / 15% T3 / 5% T4)

**T1 vanilla (10 slots):**
`vanilla/taiga` x3, `vanilla/old_growth_pine_taiga` x2, `vanilla/old_growth_spruce_taiga` x2,
`vanilla/snowy_taiga` x2, `vanilla/grove`

**T2 vanilla-adjacent (6 slots):**
`tundra/taiga`, `tundra/redwood-forest`, `tundra/autumn`, `tundra/bonsai-forest`,
`tundra/spruce-denmyre`, `tundra/taiga-extended`

**T3 custom thematic (3 slots):**
`tundra/ashcrown` (volcanic), `mountain/ashcrown`, `mountain/floating-islands`

**T4 rare Iris (1 slot):**
`tundra/magic-forest` (rarity 5) OR `tundra/sequia-redwoods` (rarity 6)

---

#### hot (target: 55% V / 25% T2 / 15% T3 / 5% T4)

**T1 vanilla (11 slots):**
`vanilla/desert` x4, `vanilla/savanna` x3, `vanilla/savanna_plateau` x2,
`vanilla/windswept_savanna`, `vanilla/badlands`

**T2 vanilla-adjacent (5 slots):**
`hot/small-valley`, `hot/mesa-valley`, `savanna/acacia-denmyre`,
`mesa/valleys`, `hot/mountain-plains`

**T3 custom thematic (3 slots):**
`mesa/cinnabar-mesa`, `tropical/cinderfall` (volcanic), `savanna/ashen-plains`

**T4 rare Iris (1 slot):**
`terralost/amethyst-canyon` (rarity 5) - desert crystal as a rare hot-region find

---

#### tropical (target: 55% V / 25% T2 / 15% T3 / 5% T4)

**T1 vanilla (11 slots):**
`vanilla/jungle` x4, `vanilla/sparse_jungle` x3, `vanilla/bamboo_jungle` x2,
`vanilla/mangrove_swamp` x2

**T2 vanilla-adjacent (5 slots):**
`tropical/rainforest`, `tropical/rainforest-hills`, `tropical/jungle-denmyre`,
`tropical/wilds`, `tropical/highlands`

**T3 custom thematic (3 slots):**
`tropical/cinderfall`, `tropical/submerged-volcanic` (if surface-viable),
`tropical/rainforest-wicked`

**T4 rare Iris (1 slot):**
`terralost/amethyst-rainforest` (rarity 6)

---

#### swamp (target: 50% V / 25% T2 / 20% T3 / 5% T4)

**T1 vanilla (10 slots):**
`vanilla/swamp` x5, `vanilla/mangrove_swamp` x3, `vanilla/dark_forest` x2

**T2 vanilla-adjacent (5 slots):**
`swamp/swamp-marsh`, `swamp/denmyre`, `swamp/swamp-forest`,
`swamp/roofed-forest`, `swamp/marsh-rotten`

**T3 custom thematic (4 slots):**
`swamp/cambian-drift`, `swamp/handy-willow-forest`, `swamp/the-creaks`,
`swamp/roofed-forest-wayward`

**T4 rare Iris (1 slot):**
`tundra/ether` (rarity 9) - deep swamp portal to the ether

---

#### terralost (target: 40% V / 25% T2 / 30% T3 / 5% T4)

Terralost is intentionally wilder - vanilla is still the plurality but the region has
a strong alien identity that justifies a lower vanilla floor.

**T1 vanilla (8 slots):**
`vanilla/badlands` x3, `vanilla/eroded_badlands` x2, `vanilla/wooded_badlands` x2,
`vanilla/windswept_hills`

**T2 vanilla-adjacent (5 slots):**
`terralost/alpine-grove`, `terralost/alpine-highlands`, `mesa/cliffs`,
`mesa/valleys`, `mountain/hills`

**T3 custom thematic (6 slots):**
`terralost/amethyst-canyon`, `terralost/amethyst-rainforest`,
`mountain/ashcrown`, `mountain/floating-islands`,
`mesa/cinnabar-mesa`, `mountain/ashcrown-lava`

**T4 rare Iris (1 slot):**
`tundra/magic-forest` (rarity 5) - terralost magic forest as a deep-region secret

---

#### mushroom (target: 60% V / 20% T2 / 20% T3 / 0% T4)

**T1 vanilla (6 slots):**
`vanilla/mushroom_fields` x6

**T2 vanilla-adjacent (2 slots):**
`mushroom/mushroom-forest`, `mushroom/mushroom-hills` (if they exist)

**T3 custom thematic (2 slots):**
`mushroom/crimson-forest`, `mushroom/warped-forest`

**Remove from mushroom:** `swamp/cambian-drift`, `swamp/the-creaks`,
`terralost/amethyst-canyon` - these are thematically wrong for a mushroom region.

---

## Phase 3 - Vanilla-adjacent custom biome audit (T2 quality bar)

Before a custom biome is listed as T2, it must pass this check:

1. **Palette check** - does it use the same base blocks as its vanilla counterpart?
   (e.g. a T2 birch forest variant must still be primarily birch logs + birch leaves)
2. **Scale check** - are trees/objects at harvestable scale per the IRIS-USABILITY-PLAN
   height table? (h6-h25 normal, h25-h45 notable, h45+ landmark)
3. **Silhouette check** - can a player identify the biome type from 50 blocks away?

Biomes that fail the palette or silhouette check get demoted to T3 or removed.

---

## Phase 4 - Custom tree biomes (new T2/T3 entries)

Our generated `.iob` trees (from `iris/scripts/output/`) enable new biomes that sit
firmly in T2 - they look vanilla but use our custom-sized trees:

| New biome concept | Tier | Base vanilla feel | Custom twist | Custom trees used |
|---|---|---|---|---|
| `custom/oak-forest-small` | T2 | Forest | Shorter oaks, denser understory | `forest/` output h6-h12 |
| `custom/oak-forest-large` | T2 | Forest | Tall oaks as landmarks | `forest/` output h19-h25 |
| `custom/birch-grove` | T2 | Birch forest | Slender birch clusters | `birch-forest/` output |
| `custom/old-birch-canopy` | T2 | Old growth birch | Tall canopy, open floor | `old-growth-birch-forest/` output |
| `custom/pine-ridge` | T2 | Taiga | Ridge-line pines, rocky floor | `taiga/` + `old-growth-pine-taiga/` output |
| `custom/cherry-meadow` | T2 | Cherry grove | Sparser cherry, meadow floor | `cherry-grove/` output |
| `custom/dark-understory` | T2 | Dark forest | Lower canopy, more floor space | `dark-forest/` output |
| `custom/savanna-sparse` | T2 | Savanna | Scattered acacias, open sight lines | `savanna/` output |
| `custom/jungle-edge` | T2 | Sparse jungle | Jungle edge feel, harvestable trees | `jungle/` output h12-h20 |

These biomes are created in Phase 4 after Phase 1-2 are tested in-world.

---

## Phase 5 - Volcanic / Dead biome family (T3 rare)

The five existing volcanic families (Ashcrown, Cinderfall, Embertide, Cinnabar Mesa,
Frostpeak) are already correctly configured with lava calderas. They belong in T3.

Additional "dead" biome concepts for T3 using our custom tree research:

| Concept | Tree type | Fits existing biome? |
|---|---|---|
| Ashen Plains expansion | Ashwood (basalt trunk, no leaves) | Yes - `savanna/ashen-plains` has no trees yet |
| Cinderfall understory | Embervine (jungle log + magma/shroomlight) | Yes - `tropical/cinderfall` has no trees yet |
| Floating Islands canopy | Cloudcap (tall trunk, large leaf sphere) | Yes - `mountain/floating-islands` has no trees yet |
| Blightroot cursed forest | Blightroot (dark oak, no leaves, wither rose) | New T3 biome needed |

These are low-effort wins: the biomes exist, they just need tree objects added.

---

## Implementation Order

### Step 1 - Create vanilla/ biome files (Phase 1)
- [x] Create all ~37 `iris/pack-base/biomes/vanilla/*.json` passthrough files -- 2026-06-05
- [ ] Verify Iris loads them (no silent-drop errors in server log)

### Step 2 - Rebuild region lists (Phase 2)
- [x] Update all 9 `iris/pack-base/regions/*.json` landBiomes arrays per the tables above -- 2026-06-05
- [ ] Test in fresh world: `/iris tp` to each region, confirm vanilla biomes generate

### Step 3 - T2 audit (Phase 3)
- [ ] Walk each T2 biome in-world, apply palette/scale/silhouette checks
- [ ] Demote or remove failures; document results

### Step 4 - Custom tree biomes (Phase 4)
- [ ] Create `iris/pack-base/biomes/custom/` folder
- [ ] Build the 9 T2 custom-tree biome JSONs
- [ ] Add them to appropriate region landBiomes lists (replacing some T2 slots)

### Step 5 - Dead/volcanic tree objects (Phase 5)
- [ ] Add Ashwood objects to `savanna/ashen-plains.json`
- [ ] Add Embervine objects to `tropical/cinderfall.json`
- [ ] Add Cloudcap objects to `mountain/floating-islands.json`
- [ ] Design and create Blightroot T3 biome JSON

---

## Frequency Sanity Check

With the slot counts above, approximate vanilla share per region:

| Region | Total slots | Vanilla slots | Vanilla % |
|---|---|---|---|
| temperate | 20 | 11 | 55% |
| forests | 20 | 12 | 60% |
| frozen | 20 | 11 | 55% |
| tundra | 20 | 10 | 50% |
| hot | 20 | 11 | 55% |
| tropical | 20 | 11 | 55% |
| swamp | 20 | 10 | 50% |
| terralost | 20 | 8 | 40% |
| mushroom | 10 | 6 | 60% |
| **Weighted avg** | | | **~54%** |

Terralost pulls the average down intentionally. If the target is 60-80%, increase vanilla
slots in temperate/forests/frozen to 14-16 each and reduce T2 slots accordingly.

---

## Rule D-005 - Approval Gate

This plan touches:
- ~37 new biome files (vanilla/ folder)
- 9 region JSON files (all landBiomes arrays)
- Potentially 9 new custom/ biome files (Phase 4)
- 3-4 existing biome JSONs (Phase 5 tree additions)

**Please confirm before implementation begins.** Key decisions to approve:
1. Is ~54% vanilla average acceptable, or should we push to 60-80% by adding more vanilla slots?
2. Should T4 (Iris showcase) biomes be removed entirely from most regions, or kept at 1 slot each?
3. Should the `vanilla/` biomes have any custom objects at all (e.g. our custom-sized trees at low rates), or stay pure passthrough?
