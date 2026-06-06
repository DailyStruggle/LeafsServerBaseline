# Structure Integration Research
# Potential jigsaw structure placement by biome, based on structure popularity

Date: 2026-06-05
Status: Research - pending design approval (Rule D-005)

---

## Summary

The pack ships 24 jigsaw structure definitions. Only 14 are currently assigned to biomes.
Ten structures are completely unassigned. Entire biome categories - taiga, swamp, tropical,
mountain, ocean - have zero structure coverage. This document maps every unassigned structure
to candidate biomes and proposes rarity values consistent with existing conventions.

---

## Existing Assignment Baseline

### Rarity convention (jigsaw `rarity` field - higher = rarer)

| Value | Feel |
|---|---|
| 800 | Common - appears frequently in the biome (villages, igloos) |
| 1200 | Standard - one per several hundred chunks (outposts, pyramids) |
| 1500-1750 | Rare - notable find, roughly one per large region |

### Currently assigned structures (reference)

| Structure | Assigned biomes |
|---|---|
| `village-plains` | 10 temperate biomes |
| `village-desert` | `hot/desert-dunes`, `hot/desert-dunes-red`, `terralost/ancient-sands` |
| `village-savanna` | `savanna/acacia-denmyre`, `savanna/forest`, `savanna/plateau`, `mesa/mesa`, `mesa/plateau-dirt` |
| `village-snowy` | `frozen/pines`, `frozen/plains` |
| `pillager-outpost` | 13 temperate/tundra/terralost biomes |
| `pyramid-desert` | `hot/desert-dunes`, `terralost/ancient-sands` |
| `woodland-mansion` | `tundra/redwood-forest`, `tundra/taiga` |
| `igloo` | `frozen/pines`, `frozen/plains` |
| `mush-huts` | `mushroom/plains` |
| `ruins/ruin3` | `temperate/plains` |

### Completely unassigned structures

`dungeon-skeleton`, `dungeon-spider`, `dungeon-zombie`, `trail-ruins`, `ruined-portal`,
`trail_chambers`, `underwater-ruin`, `village-taiga`, `village-small-frosty`, `ancient-city`

---

## Part 1: Unassigned Structures - Candidate Biomes

### `village-taiga`
Vanilla taiga village. Natural fit for taiga and cold forest biomes.

| Candidate biome | Rationale | Proposed rarity |
|---|---|---|
| `taiga/taiga.json` | Direct taiga derivative - primary home | 1000 |
| `taiga/old-growth-pine-taiga.json` | Old growth variant - slightly rarer feel | 1400 |
| `tundra/taiga.json` | Tundra taiga - cold enough to fit | 1200 |
| `tundra/taiga-extended.json` | Extended variant - same logic | 1200 |
| `tundra/bonsai-forest.json` | Cold conifer biome - thematic fit | 1400 |

### `village-small-frosty`
Small snowy village (custom frosty variant). Fits frozen/snowy biomes not already covered
by `village-snowy`.

| Candidate biome | Rationale | Proposed rarity |
|---|---|---|
| `frozen/pine-hills.json` | Snowy pine - natural village site | 1000 |
| `frozen/spruce-hills.json` | Snowy spruce - same logic | 1000 |
| `frozen/hills.json` | Generic frozen hills - good fallback | 1200 |
| `tundra/frosted-peaks.json` | Cold peaks - rare outpost feel | 1500 |
| `tundra/autumn.json` | Cold seasonal biome - occasional settlement | 1400 |

### `trail-ruins`
Buried trail ruins. Vanilla association: taiga, snowy, jungle. Works in any biome with
a sense of age or overgrowth.

| Candidate biome | Rationale | Proposed rarity |
|---|---|---|
| `taiga/taiga.json` | Vanilla canonical home for trail ruins | 1200 |
| `taiga/old-growth-pine-taiga.json` | Old growth - ruins feel earned here | 1200 |
| `tundra/magic-forest.json` | Ancient/mystical feel - ruins fit the lore | 1400 |
| `swamp/roofed-forest.json` | Overgrown dark forest - ruins buried under canopy | 1400 |
| `swamp/willow-forest.json` | Overgrown wetland - ruins partially submerged | 1400 |
| `tropical/rainforest.json` | Jungle ruins - strong thematic fit | 1200 |
| `tropical/highlands.json` | Jungle highlands - ruins on elevated terrain | 1400 |

### `ruined-portal`
Ruined Nether portal. Vanilla: spawns in most overworld biomes. Should be broadly
distributed but weighted toward wilder, less-settled biomes.

| Candidate biome | Rationale | Proposed rarity |
|---|---|---|
| `swamp/swamp-marsh.json` | Isolated wetland - portals feel abandoned here | 1200 |
| `swamp/cambian-drift.json` | Dark, eerie - strong portal atmosphere | 1400 |
| `mountain/mountain.json` | Remote peaks - portals on high ground | 1200 |
| `mountain/cliffs.json` | Dramatic cliff face - visual landmark | 1200 |
| `tundra/ether.json` | Otherworldly biome - portal fits the theme | 1400 |
| `tundra/redwood-forest.json` | Dense forest - portal hidden in trees | 1400 |
| `tropical/rainforest.json` | Jungle - vanilla canonical portal biome | 1200 |
| `tropical/cinderfall.json` | Volcanic/nether-adjacent feel - strong fit | 1000 |
| `terralost/amethyst-canyon.json` | Alien landscape - portal as ancient artifact | 1500 |

### `dungeon-skeleton`, `dungeon-spider`, `dungeon-zombie`
Underground dungeons with mob spawners. The `overrideYRange` (y=7-20) means these spawn
deep underground regardless of surface biome. Appropriate to assign broadly - they add
exploration value without affecting surface character. Differentiate by biome feel:

**`dungeon-skeleton`** - cold/open biomes (skeletons feel at home in frozen/tundra/mountain)

| Candidate biome | Proposed rarity |
|---|---|
| `frozen/pines.json` | 1000 |
| `frozen/pine-hills.json` | 1000 |
| `tundra/taiga.json` | 1000 |
| `mountain/mountain.json` | 1000 |
| `mountain/cliffs.json` | 1000 |
| `taiga/taiga.json` | 1000 |

**`dungeon-spider`** - dark/overgrown biomes (spiders fit dense canopy and swamp)

| Candidate biome | Proposed rarity |
|---|---|
| `swamp/roofed-forest.json` | 1000 |
| `swamp/cambian-drift.json` | 1000 |
| `tundra/magic-forest.json` | 1000 |
| `temperate/combo-forest.json` | 1000 |
| `tropical/rainforest.json` | 1000 |

**`dungeon-zombie`** - plains/village-adjacent biomes (zombies feel like failed settlements)

| Candidate biome | Proposed rarity |
|---|---|
| `temperate/plains.json` | 1000 |
| `temperate/meadows.json` | 1000 |
| `savanna/forest.json` | 1000 |
| `swamp/marsh.json` | 1000 |
| `hot/desert-dunes.json` | 1000 |

### `trail_chambers`
Trial Chambers (1.21 underground structure, `forcePlace: true`). Large, rare, combat-focused.
Should be in biomes players actively explore - not atmosphere-only biomes.

| Candidate biome | Rationale | Proposed rarity |
|---|---|---|
| `temperate/plains.json` | High-traffic biome - players will find it | 1750 |
| `temperate/calmplains.json` | Same logic | 1750 |
| `savanna/plateau.json` | Open terrain - structure visible from distance | 1750 |
| `mountain/plains.json` | Mountain plains - accessible but dramatic | 1750 |
| `taiga/taiga.json` | Common biome - good discovery rate | 1750 |

Note: `forcePlace: true` means this structure will always generate when selected. Keep
rarity high (1750) to avoid over-saturation.

### `underwater-ruin`
Ocean ruins. Only relevant for ocean and coastal biomes.

| Candidate biome | Rationale | Proposed rarity |
|---|---|---|
| `ocean/ocean.json` | Primary ocean - canonical home | 800 |
| `ocean/deep.json` | Deep ocean - ruins on seafloor | 800 |
| `ocean/rich-oceans.json` | Rich ocean variant - ruins add exploration | 1000 |
| `ocean/warm.json` | Warm ocean - warm ruin variants fit | 800 |
| `tropical/island-beach.json` | Island beach - coastal ruins | 1200 |
| `tropical/beach.json` | Jungle beach - ruins at waterline | 1200 |
| `swamp/swamp-mangrove-lake.json` | Mangrove water - partially submerged ruins | 1400 |

### `ancient-city`
Ancient City (deep dark). No `structureKey` override - relies on Iris placement.
Should be extremely rare and placed only in biomes with deep underground access.
Note: the JSON has no `overrideYRange` - placement depth depends on Iris/server config.

| Candidate biome | Rationale | Proposed rarity |
|---|---|---|
| `mountain/mountain.json` | Deep mountain - natural deep dark access | 2000 |
| `mountain/cliffs.json` | Cliff terrain - underground depth available | 2000 |
| `tundra/ether.json` | Otherworldly - ancient city fits the mystery | 2000 |
| `swamp/cambian-drift.json` | Darkest swamp biome - thematic fit | 2000 |

---

## Part 2: Biome Categories with Zero Structure Coverage

### taiga (0/2 biomes covered)
Both taiga biomes are completely bare. Minimum viable coverage:
- `taiga/taiga.json`: `village-taiga` (1000), `trail-ruins` (1200), `dungeon-skeleton` (1000)
- `taiga/old-growth-pine-taiga.json`: `village-taiga` (1400), `trail-ruins` (1200)

### swamp (0/16 biomes covered)
Largest uncovered category. Priority biomes for structure assignment:
- `swamp/roofed-forest.json`: `dungeon-spider` (1000), `trail-ruins` (1400)
- `swamp/willow-forest.json`: `trail-ruins` (1400)
- `swamp/cambian-drift.json`: `ruined-portal` (1400), `dungeon-spider` (1000), `ancient-city` (2000)
- `swamp/swamp-mangrove-lake.json`: `underwater-ruin` (1400)
- `swamp/marsh.json`: `dungeon-zombie` (1000)

Skip: `creaks`, `ether-adjacent`, `roofed-wayward` variants - atmosphere biomes, structures
would dilute the mood.

### tropical (0/26 biomes covered)
Large category. Focus on the navigable surface biomes; skip volcanic/underwater:
- `tropical/rainforest.json`: `trail-ruins` (1200), `dungeon-spider` (1000), `ruined-portal` (1200)
- `tropical/highlands.json`: `trail-ruins` (1400)
- `tropical/plains.json`: `dungeon-zombie` (1000)
- `tropical/bamboo-forest.json`: `trail-ruins` (1200)
- `tropical/beach.json`: `underwater-ruin` (1200)
- `tropical/island-beach.json`: `underwater-ruin` (1200)
- `tropical/cinderfall.json`: `ruined-portal` (1000)

Skip: `submerged-volcanic`, `volcanoes`, `volcanoes-lava`, `cinderfall-lava` - terrain/lore
biomes where structures would feel out of place.

### mountain (0/17 biomes covered)
Terrain-focused category. Structures should feel like remote discoveries:
- `mountain/mountain.json`: `ruined-portal` (1200), `dungeon-skeleton` (1000), `ancient-city` (2000)
- `mountain/cliffs.json`: `ruined-portal` (1200), `dungeon-skeleton` (1000)
- `mountain/forest.json`: `trail-ruins` (1400), `dungeon-spider` (1000)
- `mountain/plains.json`: `trail_chambers` (1750), `dungeon-zombie` (1000)

Skip: `ashcrown`, `floating-islands`, `calcite-base` - exotic terrain biomes where
structures would feel anachronistic.

### ocean (0/10 biomes covered)
Only `underwater-ruin` and `ocean-monument` are thematically appropriate:
- `ocean/ocean.json`: `underwater-ruin` (800), `ocean-monument` (1500)
- `ocean/deep.json`: `underwater-ruin` (800), `ocean-monument` (1200)
- `ocean/warm.json`: `underwater-ruin` (800)
- `ocean/rich-oceans.json`: `underwater-ruin` (1000), `ocean-monument` (1500)

Note: `ocean-monument` is already defined in `jigsaw-structures/ocean-monument.json` but
currently assigned to zero biomes - this is a separate gap worth fixing alongside this work.

---

## Part 3: Popularity-Based Priority Ranking

Structures ranked by how recognizable/popular they are in vanilla Minecraft, used to
prioritize which gaps to close first.

| Priority | Structure | Vanilla popularity | Gap severity |
|---|---|---|---|
| 1 | `village-taiga` | Very high - taiga villages are iconic | taiga has zero coverage |
| 2 | `trail-ruins` | High - 1.20 feature, players actively seek | 0 biomes assigned |
| 3 | `ruined-portal` | High - universal overworld feature | 0 biomes assigned |
| 4 | `underwater-ruin` | High - ocean exploration staple | ocean has zero coverage |
| 5 | `trail_chambers` | High - 1.21 flagship structure | 0 biomes assigned |
| 6 | `dungeon-zombie/skeleton/spider` | Medium-high - classic dungeons | 0 biomes assigned |
| 7 | `village-small-frosty` | Medium - custom variant, less known | frozen partially covered |
| 8 | `ancient-city` | Medium - rare/niche, deep dark | 0 biomes assigned |

---

## Part 4: Structures to Defer or Skip

| Structure | Reason to defer |
|---|---|
| `ancient-city` | No `overrideYRange` - placement depth unverified; test in isolation first |
| `dungeon-*` in atmosphere biomes | Creaks, Ether, Magic Forest, Cambian Drift - structures break the mood |
| `trail_chambers` in rare biomes | `forcePlace: true` at rarity 1750 in rare biomes risks over-saturation |
| `underwater-ruin` in non-ocean biomes | Only `swamp-mangrove-lake` and beach biomes are borderline candidates |
| `ruined-portal` in vanilla-derivative biomes | Already handled by vanilla worldgen; adding via Iris risks duplicates |

---

## Implementation Notes

1. **Schema**: add a `jigsawStructures` array to the biome JSON alongside existing keys.
   Each entry needs `"structure": "<name>"` and `"rarity": <int>`. See `hot/desert-dunes.json`
   for a reference implementation.

2. **Test order**: assign one structure to one biome, generate a fresh world, use `/iris tp`
   to confirm placement before bulk-assigning.

3. **Dungeon depth**: `dungeon-skeleton/spider/zombie` all use `overrideYRange y=7-20` -
   they are underground-only and safe to assign broadly without affecting surface character.

4. **`forcePlace` caution**: `trail_chambers` has `"forcePlace": true` - keep rarity at
   1750 minimum. Do not assign to rare biomes (rarity > 5 in Iris biome terms).

5. **Ocean monument**: `ocean-monument.json` exists but is unassigned - worth adding to
   `ocean/ocean.json` and `ocean/deep.json` in the same pass as `underwater-ruin`.

6. **Rule D-005**: this touches potentially 30+ biome files across 8 categories. Full
   implementation requires explicit approval before editing any biome JSON.
