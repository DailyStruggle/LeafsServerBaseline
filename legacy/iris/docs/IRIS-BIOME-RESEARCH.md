# Iris Overworld Biome Research

Research findings on preexisting biomes in the `IrisDimensions/overworld` pack, evaluated against the five design goals in [ADR-003](adr/ADR-003-custom-biome-design-goals.md). Also maps the Gemini research biome concepts to their closest Iris equivalents or identifies gaps requiring new biomes.

---

## Category Map

The Iris overworld pack uses 16 top-level category folders. Two additional categories were found beyond what was documented in `CUSTOM-BIOMES.md`:

| Category | Notes |
|---|---|
| `temperate` | Mild temp, moderate humidity - forests, plains, meadows |
| `tundra` | Cold, forested - taiga, redwood, autumn, magic, bonsai |
| `frozen` | Sub-zero - ice spikes, pine plains, vander, winter forest |
| `hot` | High temp - desert, oasis, mountain cliffs |
| `tropical` | Warm/wet - rainforest, bamboo, volcanoes, jungle |
| `swamp` | High humidity - marsh, cambian drift, willow, creaks |
| `mesa` | Eroded, dry - blue, green, red, yellow, dark terracotta variants |
| `mountain` | High erosion/altitude - cliffs, forest, cute cliffs |
| `savanna` | Flat, hot - acacia, cliff, plateau |
| `ocean` | Continentalness < 0 - deep, warm, rich |
| `mushroom` | Isolated island - crimson, warped, plains variants |
| `terralost` | Rare/exotic - amethyst canyon, ancient sands, alpine grove/highlands |
| `carving` | Underground shallow - 40+ cave biome variants |
| `magnetics` | Underground mid-depth |
| `prismatics` | Underground deep - 16 color-coded crystal variants |
| `vanilla` | Thin wrappers around vanilla biomes for Iris compatibility |

---

## Vanilla Compatibility Flag

One biome is **disqualified** for vanilla client use in its original form:

| Biome | File | Issue |
|---|---|---|
| Tropical Volcanoes | `tropical/volcanoes.json` | `derivative: minecraft:the_void` - sends void biome to client, breaking ambient sound and sky color |

However, the `tropical/volcanoes` + `tropical/volcanoes-lava` pair is **forkable**: the terrain generator, caldera child mechanism (`wall: lava+tuff` + `cave_air` layers), and `magmaspire` object references are all reusable. Swapping the derivative to any valid vanilla ID produces a fully compliant biome. This fork pattern is the basis for the 5 volcano variants documented in `CUSTOM-BIOMES.md`.

All other sampled biomes use valid overworld vanilla derivatives. This should be checked for any biome before adoption.

---

## Biomes That Meet ADR-003 Criteria

### Tier 1 - Strong fit (all 5 goals met)

#### Tundra Redwood Forest
- **File:** `tundra/redwood-forest.json`
- **Derivative:** `minecraft:taiga` / `vanillaDerivative: minecraft:dark_forest`
- **Rarity:** 3
- **Palette:** Coarse dirt, grass block, podzol, dirt, stone
- **Silhouette:** Custom redwood tree objects (`trees/mixed/tredwood1-10`) - tall, columnar conifers with small canopy; boulder and stump clutter at ground level
- **Build theme anchor:** Logging camp, mountain lodge, ranger outpost
- **Concept hook:** A cathedral of ancient redwoods with mossy boulders and fallen stumps - the obvious site for a timber lodge or ranger station.
- **ADR-003 notes:** Distinct silhouette (redwood height), podzol palette differentiates from vanilla taiga, bee-variant redwood trees add resource incentive.

#### Tundra Bonsai Forest
- **File:** `tundra/bonsai-forest.json`
- **Derivative:** `minecraft:taiga` / `vanillaDerivative: minecraft:flower_forest`
- **Rarity:** 1
- **Palette:** Podzol, dirt, coarse dirt, stone, andesite
- **Silhouette:** Custom bonsai tree objects (`trees/bonsai/med-1` to `med-4`) - low, wide-canopy sculpted trees; stone cluster clutter
- **Build theme anchor:** Zen garden, Japanese courtyard, meditation retreat
- **Concept hook:** Sculpted bonsai trees over a stone-and-podzol floor - a natural stage for a Japanese garden or contemplative courtyard.
- **ADR-003 notes:** Bonsai objects are unique to this biome; andesite in palette supports stone lantern builds; pairs well with the Japanese Samurai Temple narrative structure.

#### Tundra Autumn
- **File:** `tundra/autumn.json`
- **Derivative:** `minecraft:taiga` / `vanillaDerivative: minecraft:dark_forest`
- **Rarity:** 1
- **Palette:** Coarse dirt, grass block, podzol, dirt, stone
- **Silhouette:** Redwood trees + pumpkin clutter objects (`clutter/pumpkins1-6`) - warm harvest atmosphere
- **Build theme anchor:** Harvest village, autumnal cottagecore, pumpkin farm
- **Concept hook:** Redwood forest floor scattered with wild pumpkins and stumps - a harvest festival waiting to happen.
- **ADR-003 notes:** Pumpkin clutter is the key differentiator from Redwood Forest; `biomeStyle: STATIC` means consistent color rendering.

#### Terralost Amethyst Canyon / Amethyst Rainforest
- **Files:** `terralost/amethyst-canyon.json`, `terralost/amethyst-rainforest.json`
- **Derivative:** `minecraft:jungle`
- **Rarity:** 1
- **Palette:** Calcite, smooth basalt, tuff (canyon); grass block, diorite, coarse dirt (rainforest)
- **Silhouette:** Large custom amethyst tree objects (`trees/mixed/AmyLarge1-8`, `AmyMed`, `AmyNormal`, `AmySmol`) - crystal-topped trees at multiple scales
- **Build theme anchor:** Crystal research outpost, amethyst mining colony, alien jungle temple
- **Concept hook:** A jungle of crystal-topped trees rising from calcite and basalt - the only biome where an amethyst palace feels architecturally honest.
- **ADR-003 notes:** Calcite/basalt/tuff palette is highly distinct; amethyst tree objects exist at 4 size tiers giving strong visual depth; canyon variant uses `vascular-cracked-cliffs` generator for dramatic terrain.

#### Terralost Ancient Sands
- **File:** `terralost/ancient-sands.json`
- **Derivative:** `minecraft:desert`
- **Rarity:** 1
- **Palette:** Orange terracotta, red sand, sandstone, sand
- **Silhouette:** Sphinx structure objects (`clutter/rsphinx1`, `clutter/rbrksphinx1`) - pre-placed ruins as landmark features; smooth dunes + mountain + rare-hills generators
- **Build theme anchor:** Archaeological dig site, desert ruins, lost civilization outpost
- **Concept hook:** Orange dunes broken by crumbling sphinx ruins - the starting point for an archaeology-themed expedition base.
- **ADR-003 notes:** Sphinx objects are unique narrative anchors; orange terracotta over red sand is visually distinct from vanilla desert; directly maps to the Gemini "Lush Desert" concept with stronger narrative.

#### Swamp Cambian Drift
- **File:** `swamp/cambian-drift.json`
- **Derivative:** `minecraft:dark_forest`
- **Rarity:** 1
- **Palette:** Grass block, podzol, coarse dirt, dirt, stone, andesite
- **Silhouette:** Tall dark oak drift trees (`trees/darkoak/talldrift1-9`), large and small mushroom objects, generic grave clutter (`clutter/genericgrave1`)
- **Build theme anchor:** Gothic village, haunted forest settlement, dark fantasy ruins
- **Concept hook:** Towering dark oaks draped over mushroom clusters and scattered graves - a gothic village here would look like it grew from the ground.
- **ADR-003 notes:** Grave clutter is a strong narrative anchor; mushroom + dark oak combination is visually distinct; dark_forest derivative gives appropriate ambient mood.

#### Swamp Haunted Willow Forest (Handy Willow Forest)
- **File:** `swamp/handy-willow-forest.json`
- **Derivative:** `minecraft:swamp` / `vanillaDerivative: minecraft:dark_forest`
- **Rarity:** 1
- **Palette:** Grass block, podzol, dirt, coarse dirt, stone, andesite
- **Silhouette:** Generic dark oak trees, boulder clutter, grave clutter; soul sand valley ambient sound effect + smoke particle effect
- **Build theme anchor:** Horror cabin, witch hut village, cursed outpost
- **Concept hook:** Dark oaks over a smoke-hazed floor with soul-valley moans - a witch village or horror cabin fits here without any decoration.
- **ADR-003 notes:** Soul sand valley ambient sound (`minecraft:ambient.soul_sand_valley.mood`) is a powerful atmospheric differentiator requiring no custom audio; smoke particles reinforce the mood.

#### Mesa Blue
- **File:** `mesa/blue.json`
- **Derivative:** `minecraft:windswept_savanna` / `vanillaDerivative: minecraft:badlands`
- **Rarity:** 1
- **Palette:** Cyan terracotta, blue terracotta, light blue terracotta, magenta terracotta
- **Silhouette:** Terrain-only (no objects) - eroded mesa spires in cool blue/cyan tones; `highplains + mountain` generators
- **Build theme anchor:** Alien base, sci-fi research facility, void-touched ruins
- **Concept hook:** Eroded cyan and blue terracotta spires that look like a different planet - a sci-fi outpost here needs no explanation.
- **ADR-003 notes:** Directly maps to the Gemini "Warped Mesa" concept; cyan/blue/magenta terracotta layer stack is immediately recognizable; no objects means the terrain silhouette must carry the biome identity, which the eroded mesa generator achieves.

### Tier 2 - Good fit (4 of 5 goals met, one gap noted)

#### Frozen Vander
- **File:** `frozen/vander.json`
- **Derivative:** `minecraft:frozen_peaks`
- **Rarity:** 4
- **Palette:** Snow block, ice, dirt, stone, andesite
- **Silhouette:** Custom ice mushroom objects (`trees/mushroom/ice1-4`); end rod particle effect (rarity 1500 interval)
- **Build theme anchor:** Ice palace, frozen shrine, arctic research station
- **Concept hook:** Frozen peaks dotted with ice-crystal mushroom formations and drifting end-rod sparks - an ice palace here looks inevitable.
- **Gap:** `frozen_peaks` derivative gives correct sky but vanilla clients hear frozen peaks ambient, which is appropriate; no palette differentiation from vanilla frozen peaks beyond the ice mushroom objects. Recommend adding a custom fog color to strengthen identity.

#### The Creaks
- **File:** `swamp/creaks.json`
- **Derivative:** `minecraft:swamp`
- **Rarity:** 15 (very rare)
- **Palette:** Grass block, pale moss block, dirt, stone, andesite
- **Silhouette:** Roofed oak trees (`trees/oak/troofed1+`); pale moss block surface layer is the key differentiator
- **Build theme anchor:** Pale garden outpost, eerie moss-covered ruins, ghost town
- **Concept hook:** Pale moss carpeting under roofed oaks so dense the light barely reaches - a ghost town here would look genuinely abandoned.
- **Gap:** Rarity 15 means players will rarely encounter it naturally; consider reducing rarity or using as a reward biome. Pale moss block requires 1.21.4+ (same as `pale_garden` vanilla biome).

#### Terralost Alpine Grove
- **File:** `terralost/alpine-grove.json`
- **Derivative:** `minecraft:frozen_peaks`
- **Rarity:** 1
- **Palette:** Powder snow, snow block, packed ice
- **Silhouette:** Custom pollup tree objects (`trees/mixed/pollup1-13`) - rounded snow-capped trees; shrub clutter
- **Build theme anchor:** Alpine ski lodge, snowy village, winter sanctuary
- **Concept hook:** Rounded snow-capped pollup trees over a powder snow floor - a cozy alpine village fits here without any forced decoration.
- **Gap:** `frozen_peaks` derivative may give jarring ambient sound for a gentle grove; consider `minecraft:grove` as derivative instead.

#### Hot Oasis
- **File:** `hot/oasis.json`
- **Derivative:** `minecraft:desert`
- **Rarity:** 1
- **Palette:** Grass block, moss block
- **Silhouette:** No objects; plain generator; the contrast of green moss/grass against surrounding desert is the entire silhouette
- **Build theme anchor:** Desert oasis trading post, water shrine, caravan rest stop
- **Concept hook:** A pocket of green moss and grass in the middle of the desert - a trading post or water shrine here reads immediately.
- **Gap:** No tree or structure objects means the silhouette is weak in isolation; needs palm tree or acacia objects added to meet Goal 3 fully.

---

## Gemini Research Concept Mapping

The Gemini research proposed 8 additive overworld biomes. This table maps each to the closest Iris preexisting biome and identifies whether a new custom biome is needed.

| Gemini Concept | Closest Iris Biome | Match Quality | Action |
|---|---|---|---|
| Blooming Plateau | `temperate/flower-forest.json` (Temperate Flower Forest, rarity 3) | Partial - flower forest exists but lacks plateau terrain and dye-factory incentive | Extend with plateau generator + denser flower decorators |
| Lavender Forest | `temperate/osaka-violet-forest.json` (Osaka Violet Forest, rarity 4) | Good - violet-tinted forest with flower_forest derivative | Verify violet leaf particle decorator; may be sufficient as-is |
| Lush Desert | `terralost/ancient-sands.json` (Ancient Sands, rarity 1) | Good - orange terracotta + red sand + sphinx ruins | Stronger narrative than Gemini concept; adopt with oasis-style water feature added |
| Warped Mesa | `mesa/blue.json` (Mesa Blue, rarity 1) | Excellent - cyan/blue terracotta, eroded terrain, alien aesthetic | Adopt directly; rename concept to "Blue Mesa" in catalog |
| Auroral Garden | No match | None - no biome with Allay spawns, frosted grass, and no-hostile-spawn rule | New biome required |
| Orchid Swamp | `swamp/marsh.json` (Swamp Marsh, rarity 1) | Partial - jungle trees over swamp floor, swamp derivative | Lacks blue orchid density and stilt-house silhouette; extend or create new |
| Canopied Rainforest | `tropical/rainforest.json` (Tropical Rainforest, rarity 1) | Partial - jungle derivative, rainforest terrain | Lacks 75-block mahogany trees; custom tree objects needed |
| Glass Beach | No match | None - no sea glass surface palette biome exists | New biome required |

### Cave Biome Mapping

| Gemini Concept | Closest Iris Cave Biome | Notes |
|---|---|---|
| Andesite Caves | Implemented as `carving/andesite-caves.json` | New overlay biome (andesite/polished andesite/diorite); wired into `temperate` + `forests` |
| Ice Caves | Implemented as `carving/ice-caves.json` | New overlay biome (packed/blue ice, snow); wired into `frozen` + `tundra` |
| Crystal Caves | Implemented as `prismatics/crystal-caves.json` | Repurposed name: now a rainbow stained-glass biome; wired into `temperate` |
| Amethyst Caves | Implemented as `prismatics/amethyst-caves.json` | Amethyst reward biome (was the old "Crystal Caves" concept); wired into `temperate` |
| Frostfire Caves | Implemented as `magnetics/frostfire-caves.json` | New overlay biome; ice + soul fire palette, low-grade `SLOW` cold via Iris `effects`; wired into `frozen` (ties to Frostpeak via `carvingBiome` once that volcano deploys) |
| Mantle Caves | Implemented as `prismatics/mantle-caves.json` | New overlay biome (magma/blackstone + deepslate gold/diamond ore); wired into `hot` |
| Sulfur Caves | Implemented as `carving/sulfur-caves.json` | Forked from `carving/volcanic.json`; sulfur palette (yellow terracotta/concrete) added and toxic gas done via Iris `effects` low-grade Poison; wired into the tropical region `caveBiomes` |

---

## Recommended Adoption List

Biomes from the Iris pack that can be adopted with minimal or no modification:

| Biome | File | Priority | Notes |
|---|---|---|---|
| Tundra Redwood Forest | `tundra/redwood-forest.json` | High | Adopt as-is; strong silhouette and palette |
| Tundra Bonsai Forest | `tundra/bonsai-forest.json` | High | Adopt as-is; pairs with Japanese Samurai Temple structure |
| Tundra Autumn | `tundra/autumn.json` | High | Adopt as-is; harvest/cottagecore theme |
| Terralost Amethyst Canyon | `terralost/amethyst-canyon.json` | High | Adopt as-is; calcite/basalt palette is unique |
| Terralost Ancient Sands | `terralost/ancient-sands.json` | High | Adopt as-is; sphinx ruins are a narrative anchor |
| Swamp Cambian Drift | `swamp/cambian-drift.json` | High | Adopt as-is; gothic theme with grave clutter |
| Swamp Haunted Willow | `swamp/handy-willow-forest.json` | High | Adopt as-is; soul valley audio effect is free atmosphere |
| Mesa Blue | `mesa/blue.json` | High | Adopt as-is; maps directly to Warped Mesa concept |
| Frozen Vander | `frozen/vander.json` | Medium | Adopt; consider adding custom fog color |
| The Creaks | `swamp/creaks.json` | Medium | Adopt; reduce rarity from 15 to 6-8 |
| Terralost Alpine Grove | `terralost/alpine-grove.json` | Medium | Adopt; change derivative to `minecraft:grove` |
| Hot Oasis | `hot/oasis.json` | Low | Needs palm/acacia objects before Goal 3 is met |

### Volcano Variants (Forks of tropical/volcanoes)

Five new biomes derived from the `tropical/volcanoes` fork pattern. JSON files in `iris-biomes/`.

| Biome | Files | Derivative | Notes |
|---|---|---|---|
| Cinderfall | `tropical/cinderfall.json` + `tropical/cinderfall-lava.json` | `minecraft:crimson_forest` | Fantasy jungle volcano; crimson fog + ambient |
| Frostpeak | `frozen/frostpeak.json` + `frozen/frostpeak-lava.json` | `minecraft:frozen_peaks` | Ice volcano; black basalt cone in frozen zone |
| Ashcrown | `mountain/ashcrown.json` + `mountain/ashcrown-lava.json` | `minecraft:basalt_deltas` | Mountain volcano; ash-gray terracotta layers |
| Embertide | `ocean/embertide.json` + `ocean/embertide-lava.json` | `minecraft:basalt_deltas` | Island volcano; sand rim at sea level |
| Cinnabar Mesa | `mesa/cinnabar-mesa.json` + `mesa/cinnabar-mesa-lava.json` | `minecraft:nether_wastes` | Mesa volcano; nether-orange fog over terracotta |

All variants: rarity 20 (Hidden tier), `vanillaDerivative: minecraft:badlands`, `magmaspire1-3` objects on slopes, loot-table treasure map required.

---

## Biomes Requiring New Custom Work

| Concept | Gap | Effort |
|---|---|---|
| Auroral Garden | No Iris equivalent; needs Allay spawn config + no-hostile rule + frosted grass palette | Medium |
| Glass Beach | No Iris equivalent; needs sea glass surface layer + shore generator | Medium |
| Orchid Swamp | Marsh exists but lacks orchid density and stilt silhouette | Low (extend marsh) |
| Canopied Rainforest | Rainforest exists but lacks 75-block mahogany tree objects | Medium (custom tree schematics) |
| Blooming Plateau | Flower forest exists but lacks plateau terrain | Low (swap generator) |
| Sulfur Caves | DONE - `carving/sulfur-caves.json`; sulfur palette + toxic gas via Iris `effects` (low-grade Poison). Custom Sulfur Cube mob still optional/future | Done |

---

## See Also

- [CUSTOM-BIOMES.md](CUSTOM-BIOMES.md) - full additive biome catalog and Iris schema reference
- [BIOMES.md](BIOMES.md) - vanilla biome ID reference
- [ADR-003](adr/ADR-003-custom-biome-design-goals.md) - design goals and acceptance criteria
- Source: `IrisDimensions/overworld` GitHub repository, sampled 2026-06-04
