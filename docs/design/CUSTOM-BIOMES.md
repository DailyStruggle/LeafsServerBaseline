# Custom Biomes Design Reference

Design catalog for all custom biomes planned or implemented for this server. Covers vanilla client compatibility mechanics, the Iris biome classification system, the full additive biome catalog, cave biomes, and block palette reference.

See [ADR-003](adr/ADR-003-custom-biome-design-goals.md) for the design goals that every biome entry must satisfy before it is considered complete.

---

## Vanilla Client Compatibility

Vanilla Java Edition clients (no mods required) can join this server. Iris Dimension Engine achieves this via two fields in every biome JSON:

| Field | Purpose |
|---|---|
| `derivative` | The vanilla biome ID sent to the client. Controls ambient sounds, sky color, fog, and music the client plays. |
| `vanillaDerivative` | The vanilla biome ID used server-side for mob spawning tables, weather, and game rule lookups. |

The client never receives a foreign biome ID. It sees a standard `minecraft:` biome while the server generates fully custom terrain, surface palettes, and decorators on top. Custom biome names are visible server-side (e.g., in `/locatebiome`, F3 debug screen via Paper) but the underlying protocol remains vanilla-compatible.

**Derivative selection rule:** choose the vanilla biome whose ambient sound and sky color most closely match the custom biome's intended mood. A warm flower plateau should derive from `minecraft:meadow`, not `minecraft:desert`, even if the temperature values are similar.

---

## Iris Biome Classification System

Iris organizes biomes into folder-based climate categories. Each category maps to a range of temperature and humidity values in the dimension's noise configuration. The overworld pack uses the following top-level categories:

| Category folder | Climate profile | Vanilla analogs |
|---|---|---|
| `temperate` | Mild temp, moderate humidity | Plains, forest, meadow |
| `hot` | High temp, low-mid humidity | Desert, savanna, badlands |
| `frozen` | Low temp, any humidity | Snowy plains, ice spikes, frozen peaks |
| `swamp` | Mid temp, high humidity | Swamp, mangrove swamp |
| `savanna` | High temp, low humidity, flat | Savanna, savanna plateau |
| `mesa` | High temp, very low humidity, eroded | Badlands, eroded badlands |
| `mountain` | Any temp, high erosion/altitude | Jagged peaks, stony peaks, grove |
| `ocean` | Continentalness < 0, any depth | All ocean variants |
| `carving` | Underground, shallow | Dripstone caves, lush caves |
| `magnetics` | Underground, mid-depth | Deep dark adjacent |
| `prismatics` | Underground, deep | Crystal/rare mineral zones |
| `mushroom` | Isolated island continentalness | Mushroom fields |

Custom biomes are placed in the category folder that best matches their climate. This determines which region of the world they generate in and which vanilla biomes they neighbor.

---

## Biome JSON Schema (key fields)

Minimum required fields for a custom biome entry:

```json
{
  "name": "Blooming Plateau",
  "color": "#E8A0D0",
  "rarity": 3,
  "derivative": "minecraft:meadow",
  "vanillaDerivative": "minecraft:meadow",
  "generators": [ ... ],
  "decorators": [ ... ]
}
```

| Field | Type | Notes |
|---|---|---|
| `name` | string | Display name, visible in F3 / Paper API |
| `color` | hex string | Map color in Iris Studio |
| `rarity` | int 1-10 | Higher = rarer; 1 is most common |
| `derivative` | namespaced ID | Vanilla biome sent to client (see above) |
| `vanillaDerivative` | namespaced ID | Vanilla biome used for server-side logic |
| `generators` | array | Terrain height/noise generator references |
| `decorators` | array | Surface features: trees, flowers, rocks, objects |
| `palette` | array | Surface and subsurface block layers |

---

## Additive Overworld Biomes Catalog

Planned custom biomes for the overworld. Each entry includes the Iris category, derivative mapping, climate parameters, block palette, build theme anchor, and concept hook.

### Temperate Category

#### Blooming Plateau

- **Concept hook:** A high-altitude wildflower carpet so dense it looks painted - the obvious site for a quaint apiary village or automated dye works.
- **Iris category:** `temperate`
- **Derivative:** `minecraft:meadow`
- **Climate:** Temp 0.5, Humidity 0.8, elevated continentalness, low erosion
- **Terrain:** Flat-topped plateau with steep cliff edges; height ~Y=100-120
- **Primary palette:** Short grass, moss blocks, flower-covered dirt
- **Signature blocks:** All 16 dye-color flowers generate densely; bone meal produces rare flower variants
- **Build theme anchor:** Quaint farmhouses, apiaries, automated dye factories
- **Rarity:** 3

#### Lavender Forest

- **Concept hook:** Purple-hued jacaranda canopy with falling leaf particles - a ready-made backdrop for serene fantasy estates.
- **Iris category:** `temperate`
- **Derivative:** `minecraft:cherry_grove`
- **Climate:** Temp 0.7, Humidity 0.8
- **Terrain:** Gently rolling hills, moderate tree density
- **Primary palette:** Custom jacaranda log (or cherry log fallback), purple-tinted leaves, podzol floor
- **Signature blocks:** Lavender flower (or allium fallback), purple falling leaf particles via decorator
- **Build theme anchor:** Serene estates, fantasy gardens, wizard towers
- **Rarity:** 4

#### Orchid Swamp

- **Concept hook:** Clear birch forest rising from shallow, fertile wetlands - stilt houses and elevated walkways feel inevitable here.
- **Iris category:** `swamp`
- **Derivative:** `minecraft:swamp`
- **Climate:** Temp 0.8, Humidity 0.9
- **Terrain:** Flat, waterlogged floor at Y=62-64; birch trees generate with roots in water
- **Primary palette:** Mud, clay, lily pads, blue orchids
- **Signature blocks:** Blue orchid (dense), fertile soil (farmland analog), shallow water sheets
- **Build theme anchor:** Elevated water-walkways, stilt-house villages, fishing docks
- **Rarity:** 3

#### Auroral Garden

- **Concept hook:** Rainbow birch and fir forest with frosted grass and Allay spawns - a peaceful sanctuary where hostile mobs never appear.
- **Iris category:** `temperate`
- **Derivative:** `minecraft:grove`
- **Climate:** Temp -0.25, Humidity 0.5
- **Terrain:** Open forest, moderate hills, frosted grass tint
- **Primary palette:** Birch logs, frosted grass (custom or snow-layer analog), icy iris flowers
- **Signature blocks:** Allay natural spawns; no hostile mob spawner entries
- **Build theme anchor:** Peaceful sanctuaries, secure farms, fairy-tale cottages
- **Rarity:** 5 (rare by design - a reward biome)

### Hot Category

> **Lush Desert** - covered by Iris `terralost/ancient-sands` (sphinx ruins, pyramid jigsaw, adopt as-is). No custom entry needed.

#### Warped Mesa

- **Concept hook:** Jagged, deeply eroded clay peaks in cool deep blue - alien enough to justify a sci-fi research outpost or void-touched base.
- **Iris category:** `mesa`
- **Derivative:** `minecraft:badlands`
- **Climate:** Temp 2.0, Humidity 0.0
- **Terrain:** Highly eroded spires and canyons; cyan/blue terracotta layer stacking
- **Primary palette:** Cyan terracotta, blue-tinted sand (custom or blue concrete powder), packed ice accents
- **Signature blocks:** Cyan terracotta bands, blue-tinted sand floor
- **Build theme anchor:** Alien bases, sci-fi research facilities, void-touched ruins
- **Rarity:** 4

### Ocean / Shore Category

#### Glass Beach

- **Concept hook:** Shimmering shoreline of smoothed colorful sea glass - modernist beach houses and glassworks look like they belong here.
- **Iris category:** `ocean` (shore sub-type)
- **Derivative:** `minecraft:beach`
- **Climate:** Temp 0.7, Humidity 0.4, high continentalness edge
- **Terrain:** Flat shoreline, shallow water; sea glass blocks replace sand at surface
- **Primary palette:** Colorful sea glass (stained glass / glass pane variants), white sand subsurface
- **Signature blocks:** Sea glass surface layer in mixed colors; glass pane decorators as driftwood analog
- **Build theme anchor:** Modernist beach houses, lighthouses, glassworks studios
- **Rarity:** 4

### Mountain Category

> **Canopied Rainforest** - covered by Iris `tropical/rainforest` variants (adopt with largegeneric trim per IRIS-USABILITY-PLAN.md). No custom entry needed.

---

## Seasonal Biome Framework

Rather than static biomes, four seasonal profiles can be implemented as region-level biome sets that share the same geographic footprint but swap surface palettes, decorators, and mob spawners. Each season maps to a distinct Iris region with a `seasonalGroup` tag.

| Season | Iris region tag | Key surface features | Signature mob |
|---|---|---|---|
| Summer Strand | `season_summer` | Palm trees, coconut clusters, wild melon patches, white sand | Crabs, white dolphins |
| Autumnal Canopy | `season_autumn` | Orange oak, yellow birch, leaf piles, pumpkins, wild corn/wheat | Hostile scarecrows (night), white-lipped deer |
| Winter Waste | `season_winter` | Deep snow, powder snow patches, frozen lakes, spruce forest | Penguins, hostile iceologers |
| Spring Meadow | `season_spring` | Cherry blossom, birch, active bee hives, abandoned greenhouses | Mooblooms, sniffers |

**Implementation note:** Seasonal biomes share the same `derivative` as their base climate biome. The season is a decorator/palette swap, not a separate biome ID from the client's perspective.

---

## Cave and Speleological Biomes

Underground biomes use Iris's `carving`, `magnetics`, and `prismatics` category folders. Depth is controlled by the `D` (depth) component of the climate vector.

| Biome | Iris category | Depth range | Signature hazard | Key materials |
|---|---|---|---|---|
| Andesite Caves | `carving` | Y=0 to Y=-20 | None - safe exploration | Polished andesite, reflective mineral veins |
| Ice Caves | `carving` | Y=20 to Y=60 (cold biomes only) | Slippery ice floor, hidden crevasses | Packed ice, blue ice, hanging icicles |
| Desert Caves | `carving` | Y=30 to Y=60 (hot biomes only) | Collapsing sand ceiling | Sand, sandstone, buried loot |
| Crystal Caves | `prismatics` | Y=-20 to Y=-50 | None - rare reward zone | Amethyst clusters, glowing crystal objects |
| Frostfire Caves | `magnetics` | Y=-30 to Y=-60 | Sub-zero damage (cold effect) | Soul fire, ice-bound rare ores |
| Mantle Caves | `prismatics` | Y=-50 to Y=-64 | Lava rivers, geothermal vents | Rich ore veins (gold, diamond, netherite analog) |

> **Geothermal Sulfur Caves** - removed. Requires custom mob (Sulfur Cubes) and toxic gas mechanic; not achievable in vanilla Iris.

> **Abyssal Ocean Trench** - removed. Requires 3 custom mobs (Deepworms, Abyssal Clams, The Gleaming); not achievable in vanilla Iris.

> **Yellowstone / Hot Spring** - replaced by the per-volcano Hot Springs child biome system. See each volcano entry below for the `<volcano>-springs.json` child. Color/atmosphere data preserved in BIOME-ADDITIONS-PLAN.md.

---

## Volcano Biomes

Five volcano variants, each placed in a different climate zone. All are forks of the `tropical/volcanoes` + `tropical/volcanoes-lava` Iris biome pair with the `the_void` derivative replaced by a valid vanilla ID. Each uses an "off-color" surface palette - blocks that are subtly wrong for the surrounding biome - so players notice something unusual before they see the caldera.

**Shared mechanics (all variants):**
- Generator: `mountain`, min Y=30 max Y=180 - cone shape
- Caldera: child biome with `wall: lava+tuff` + `cave_air` layers - summit lava pool
- Objects: `clutter/magmaspire1-3` on slopes - basalt spire formations as natural pillars
- Child style: `GLOB`, zoom 0.2, exponent 4 - tight caldera at the peak
- `vanillaDerivative: minecraft:badlands` on all variants - no rain, terracotta mob tables
- Rarity: 20 (Hidden tier per ADR-003 Goal 6); loot-table treasure map required as navigation aid

### Cinderfall (Fantasy Jungle Volcano)

- **Concept hook:** A crimson-fogged volcano erupting from the jungle floor - the only place where a dark fantasy fortress built into a lava caldera feels architecturally honest.
- **Iris category:** `tropical`
- **File pair:** `tropical/cinderfall.json` + `tropical/cinderfall-lava.json`
- **Derivative:** `minecraft:crimson_forest` / `vanillaDerivative: minecraft:badlands`
- **Climate:** Temp 2.0+, Humidity 0.9, high continentalness (inland jungle zone)
- **Primary palette:** Basalt (surface), blackstone (layer 2), red terracotta + orange terracotta (layers 3-10), tuff (alternate subsurface)
- **Off-color signal:** Red/orange terracotta spires rising through jungle canopy - wrong color for a jungle, immediately suspicious
- **Atmosphere on vanilla client:** Deep red fog, crimson forest ambient sound - fantasy/magical volcano feel
- **Build theme anchor:** Fantasy volcanic fortress - terracotta ramparts on the caldera rim, magmaspire pillars as natural towers, lava pool as the central courtyard
- **Rarity:** 20
- **Hot Springs child:** `tropical/cinderfall-springs.json` - jungle-edge springs; lush ferns mix with calcite pools; derivative `minecraft:jungle`

### Frostpeak (Ice Volcano)

- **Concept hook:** A black basalt volcano punching through a frozen tundra, its summit lava pool steaming against the ice - a shrine or fortress here plays the fire-and-ice contrast as its entire identity.
- **Iris category:** `frozen`
- **File pair:** `frozen/frostpeak.json` + `frozen/frostpeak-lava.json`
- **Derivative:** `minecraft:frozen_peaks` / `vanillaDerivative: minecraft:badlands`
- **Climate:** Temp -0.5 to -1.0, any humidity, high erosion
- **Primary palette:** Basalt (surface), blue ice (layer 2), packed ice (layers 3-5), light blue terracotta (layers 6-10)
- **Off-color signal:** Black basalt cone rising from a white/blue frozen landscape - dark silhouette visible from far away, wrong for the zone
- **Atmosphere on vanilla client:** Icy sky, frozen peaks ambient sound - a volcano that should not exist in a frozen zone
- **Build theme anchor:** Frozen caldera shrine - ice palace built around the lava pool, heat-cold contrast as the central architectural tension
- **Rarity:** 20
- **Hot Springs child:** `frozen/frostpeak-springs.json` - ice-rimmed pools; steam contrast against snow is the visual; derivative `minecraft:snowy_taiga`

### Ashcrown (Mountain Volcano)

- **Concept hook:** A gray ash-cone rising among the mountain peaks, its caldera venting smoke - a geothermal research station or dwarven forge built into the rock face.
- **Iris category:** `mountain`
- **File pair:** `mountain/ashcrown.json` + `mountain/ashcrown-lava.json`
- **Derivative:** `minecraft:basalt_deltas` / `vanillaDerivative: minecraft:badlands`
- **Climate:** Any temp, high erosion, high continentalness
- **Primary palette:** Basalt (surface), andesite (layer 2), gray terracotta (layers 3-8), tuff (layers 9-10)
- **Off-color signal:** Ash-gray terracotta layers visible in cliff faces - too uniform and dark compared to normal mountain stone
- **Atmosphere on vanilla client:** Ash-gray fog, basalt deltas ambient sound - geological, industrial feel
- **Build theme anchor:** Geothermal research station - industrial stone-and-basalt architecture built into the mountain face, caldera as the power source
- **Rarity:** 20
- **Hot Springs child:** `mountain/ashcrown-springs.json` - classic Yellowstone feel; pine forest + prismatic pools; derivative `minecraft:old_growth_pine_taiga`

### Embertide (Island/Ocean Volcano)

- **Concept hook:** A black basalt island rising from the warm ocean, its summit glowing with lava - a pirate outpost or island fortress built up the slopes to the caldera.
- **Iris category:** `ocean`
- **File pair:** `ocean/embertide.json` + `ocean/embertide-lava.json`
- **Derivative:** `minecraft:basalt_deltas` / `vanillaDerivative: minecraft:badlands`
- **Climate:** Any temp, low continentalness (ocean zone), moderate erosion
- **Primary palette:** Basalt (surface), blackstone (layer 2), orange terracotta (layers 3-8), sand (base rim at sea level)
- **Off-color signal:** Black basalt island rising from warm ocean - dark cone visible from the water surface; sand rim at waterline gives natural beach-to-volcano transition
- **Atmosphere on vanilla client:** Ash-gray fog rising from ocean - island volcano visible from sea
- **Build theme anchor:** Volcanic island outpost - docks at the sand rim, basalt watchtowers on the slopes, caldera as the inner sanctum
- **Rarity:** 20
- **Hot Springs child:** `ocean/embertide-springs.json` - coastal springs; pools drain toward the ocean shore; derivative `minecraft:beach`

### Cinnabar Mesa (Mesa Volcano)

- **Concept hook:** A terracotta mesa capped in black basalt with nether-orange fog bleeding through the clay layers - a corrupted citadel built into the mesa face, its caldera throne room glowing from within.
- **Iris category:** `mesa`
- **File pair:** `mesa/cinnabar-mesa.json` + `mesa/cinnabar-mesa-lava.json`
- **Derivative:** `minecraft:nether_wastes` / `vanillaDerivative: minecraft:badlands`
- **Climate:** Temp 2.0, very low humidity, high erosion (badlands zone)
- **Primary palette:** Basalt (surface), red terracotta (layers 2-3), orange terracotta (layers 4-6), brown terracotta (layers 7-10)
- **Off-color signal:** Basalt cap on a terracotta mesa - black top layer is wrong for badlands; nether-orange fog tint makes the zone feel corrupted
- **Atmosphere on vanilla client:** Nether-orange fog bleeding into mesa - darker and more ominous than surrounding clay
- **Build theme anchor:** Corrupted mesa citadel - terracotta walls and towers built into the mesa face, basalt cap as the fortress roof, lava caldera as the throne room
- **Rarity:** 20
- **Hot Springs child:** `mesa/cinnabar-springs.json` - terracotta-rimmed pools; mineral-rich orange/white contrast; derivative `minecraft:badlands`

---

## New Biomes

Six additions drawn from player sentiment research (Terralith, BYG, Biomes O' Plenty). Full design specs in `docs/scratch/BIOME-ADDITIONS-PLAN.md`.

| Biome | Category | Phase | Status |
|---|---|---|---|
| Maple Forest | `tundra` (child of autumn) | 3 | Done |
| Boreal Shield | `frozen` | 2 | Done |
| Ashen Plains | `savanna` | 2 | Done |
| Bryce Spires | `mesa` | 2 | Done |
| Floating Islands | `mountain` | 4 (stretch) | Done |
| Mirage Isles | `ocean` | 4 (stretch) | Done |
| Hot Springs (Cinderfall) | `tropical` (child) | 3 | Done |
| Hot Springs (Frostpeak) | `frozen` (child) | 3 | Done |
| Hot Springs (Ashcrown) | `mountain` (child) | 3 | Done |
| Hot Springs (Embertide) | `ocean` (child) | 3 | Done |
| Hot Springs (Cinnabar Mesa) | `mesa` (child) | 3 | Done |

---

## Vanilla Biome Tree Scale Variants

Additive biomes placed adjacent to their vanilla counterparts (rarity 4-5, vs vanilla rarity 1). Each biome uses the same `derivative` and `vanillaDerivative` as its vanilla analog so the client experience is identical - the only difference is the tree object mix, which blends small, medium, large, and exotic schematic variants generated by the tree-gen script.

**Spawn chance design principle:** chances decrease as height increases. Small trees (h6-h20) spawn at 0.04-0.055, medium (h20-h45) at 0.018-0.035, large (h50-h80) at 0.007-0.018, giants/exotics (h80+) at 0.001-0.008. Dark forest is the exception - no small trees; all entries are medium-to-giant to enforce a closed canopy.

**Object path convention:** `trees/<species>/<filename_without_extension>` - schematics must be placed in the Iris dimension pack's `objects/trees/<species>/` folder.

| Biome file | Vanilla analog | Tree species | Scale mix | Exotic variants |
|---|---|---|---|---|
| `temperate/forest.json` | `minecraft:forest` | Oak | h8-h32 common, h32+ sporadic | ancient_oak (h85, h100) |
| `temperate/birch-forest.json` | `minecraft:birch_forest` | Birch | h6-h20 common, h20-h25 sporadic | - |
| `temperate/old-growth-birch-forest.json` | `minecraft:old_growth_birch_forest` | Birch | h14-h25 dominant, h6-h9 rare | - |
| `temperate/dark-forest.json` | `minecraft:dark_forest` | Dark oak | h8-h23 base canopy, h60-h98 giants | titan_dark_oak, spiral_dark_oak |
| `temperate/cherry-grove.json` | `minecraft:cherry_grove` | Cherry | h8-h20 common, h56-h109 very sporadic | - |
| `taiga/taiga.json` | `minecraft:taiga` | Spruce | h12-h36 common, h41-h71 sporadic | - |
| `taiga/old-growth-pine-taiga.json` | `minecraft:old_growth_pine_taiga` | Spruce | h24-h52 dominant, h55-h71 moderate | - |
| `hot/jungle.json` | `minecraft:jungle` | Jungle | h33-h61 common, h80-h106 sporadic | serpentine_jungle, snake_jungle |
| `savanna/savanna.json` | `minecraft:savanna` | Acacia | h6-h20 common, h32-h45 sporadic | savanna_acacia wide-canopy |

---

## Narrative Structures Catalog

Structures that guide players across the world and reward exploration. Each structure generates in a specific biome group and provides unique loot.

| Structure | Target biome group | Unique loot | Exploration purpose |
|---|---|---|---|
| Skull Island Vault | Deep warm oceans | Navigation maps, gold bars, ancient compasses | Leads to Pirate Utopia coordinates |
| Pirate Utopia | Hidden archipelago | Custom pirate villager trades, cannons, ship sails | Conquerable island base |
| Nordic Medieval Hall | Cold taiga / snowy slopes | Battle axes, heavy fur armor, custom wood carvings | Winter outpost / cozy hub |
| Eldorado Statue | Dense jungle / canopy forest | Gold blocks, emeralds, chiseled stone bricks | Signals nearby jungle temple |
| Japanese Samurai Temple | Bamboo groves / birch forests | Katana stands, custom paper walls, red glazed tiles | Peaceful detailed build reference |
| Atlantis Ruins | Deep ocean trenches | Prismarine bricks, sea lanterns, conduit cores | High-depth exploration reward |

---

## Block Palette Reference

Reusable palette sets for build theme consistency. Players can use these as a starting point when building in or near a custom biome.

| Theme | Core blocks | Accent blocks |
|---|---|---|
| Ethereal & Magical | Purpur blocks, azalea leaves, calcite | Pearlescent froglight, quartz, amethyst |
| Warm Crimson | Stripped crimson stems, pink stained glass | Pink concrete powder, magenta terracotta |
| Gothic & Spooky | Gilded blackstone, honeycomb blocks | Deepslate tiles, jack o' lanterns, honey |
| Modern Pink & Grey | Deepslate tiles, polished basalt, white calcite | Pink stained glass, pink glazed terracotta |
| Fairytale Cottagecore | Mud bricks, brown mushroom blocks | Dark oak planks, spruce logs, raw iron |
| Cozy Medieval Cabin | Dark spruce logs, dark oak planks | Cobblestone, mossy stone bricks, ochre froglight |
| Geothermal Industrial | Calcite, tuff, basalt | White terracotta, coarse dirt, sulfur-yellow concrete powder |

---

## Multi-Noise Climate Vector

Every point in the world is defined by a six-component climate vector used by Iris to select the nearest matching biome:

```
V_P = <T, H, C, E, W, D>
```

| Component | Name | Range | Effect |
|---|---|---|---|
| T | Temperature | -2.0 to 2.0 | Cold wastes to hot deserts |
| H | Humidity | 0.0 to 1.0 | Barren to dense forest |
| C | Continentalness | -1.0 to 1.0 | Deep ocean to far inland |
| E | Erosion | 0.0 to 1.0 | Flat plains to jagged peaks |
| W | Weirdness | -1.0 to 1.0 | Normal terrain to rare variants |
| D | Depth | 0.0 to 1.0 | Surface to deep underground |

Biome selection uses Euclidean distance between the coordinate's climate vector and each biome's target vector. The biome with the lowest distance wins.

**Noise offset rule:** Temperature, humidity, and height noise layers must use different seed offsets. Aligned seeds cause low-humidity zones to always coincide with low altitude, producing artificial repetitive landscapes. Offsetting seeds allows mountains to form naturally as barriers between opposing climates.

---

## See Also

- [BIOMES.md](BIOMES.md) - complete vanilla biome ID reference (Java Edition 1.21)
- [ADR-003](adr/ADR-003-custom-biome-design-goals.md) - design goals and acceptance criteria
- [GLOSSARY.md](GLOSSARY.md) - canonical term definitions
