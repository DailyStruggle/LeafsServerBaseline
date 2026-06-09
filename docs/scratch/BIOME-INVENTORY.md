# Biome Inventory: All Biomes (Source + Implemented)

Date: 2026-06-07
Effective Issue: "create a list of biomes we have and biomes we should implement";
updates: "add a column for implemented or not" + "check for redundancies and correct them"
+ "make it a list of ALL biomes we have and use a column for source".

Reconciles the live Iris overlay (`iris/pack-overlay/biomes/`) against the design catalog in
`docs/design/CUSTOM-BIOMES.md`. Every biome is now enumerated individually (no roll-ups).

Columns:
- `Source`: `Custom` = LeafsServer-authored themed biome; `Vanilla` = re-derived Minecraft
  biome (incl. tree-scale variants) under `vanilla/`; `Design catalog` = specced in
  `CUSTOM-BIOMES.md` but no overlay file exists yet.
- `Implemented`: `Yes` = a biome JSON exists in the live overlay; `No` = not built.

Source of truth for `Yes`: `iris/pack-overlay/biomes/**/*.json` (117 files).
Source of truth for `No`: `docs/design/CUSTOM-BIOMES.md` (designed-not-built entries).

---

## All biomes

| Biome (file / id) | Category | Source | Implemented |
|---|---|---|---|
| `carving/andesite-caves.json` | carving | Custom | Yes |
| `carving/desert-caves.json` | carving | Custom | Yes |
| `carving/ice-caves.json` | carving | Custom | Yes |
| `carving/sulfur-caves.json` | carving | Custom | Yes |
| `frozen/boreal-shield.json` | frozen | Custom | Yes |
| `frozen/frostpeak.json` | frozen | Custom | Yes |
| `frozen/frostpeak-lava.json` | frozen | Custom | Yes |
| `frozen/frostpeak-springs.json` | frozen | Custom | Yes |
| `frozen/mountains/extreem-ice-spikes.json` | frozen | Custom | Yes |
| `frozen/mountains/mountian-alpha.json` | frozen | Custom | Yes |
| `hot/mountain-cliffs.json` | hot | Custom | Yes |
| `magnetics/frostfire-caves.json` | magnetics | Custom | Yes |
| `mesa/bryce-spires.json` | mesa | Custom | Yes |
| `mesa/cinnabar-mesa.json` | mesa | Custom | Yes |
| `mesa/cinnabar-mesa-lava.json` | mesa | Custom | Yes |
| `mesa/cinnabar-springs.json` | mesa | Custom | Yes |
| `mesa/mesa.json` | mesa | Custom | Yes |
| `mountain/ashcrown.json` | mountain | Custom | Yes |
| `mountain/ashcrown-lava.json` | mountain | Custom | Yes |
| `mountain/ashcrown-springs.json` | mountain | Custom | Yes |
| `mountain/calcite-base.json` | mountain | Custom | Yes |
| `mountain/floating-islands.json` | mountain | Custom | Yes |
| `ocean/deep.json` | ocean | Custom | Yes |
| `ocean/embertide.json` | ocean | Custom | Yes |
| `ocean/embertide-lava.json` | ocean | Custom | Yes |
| `ocean/embertide-springs.json` | ocean | Custom | Yes |
| `ocean/mirage-isles.json` | ocean | Custom | Yes |
| `prismatics/amethyst-caves.json` | prismatics | Custom | Yes |
| `prismatics/crystal-caves.json` | prismatics | Custom | Yes |
| `prismatics/mantle-caves.json` | prismatics | Custom | Yes |
| `savanna/ashen-plains.json` | savanna | Custom | Yes |
| `swamp/cambian-drift.json` | swamp | Custom | Yes |
| `swamp/cambian-drift-extended.json` | swamp | Custom | Yes |
| `swamp/creaks.json` | swamp | Custom | Yes |
| `swamp/denmyre.json` | swamp | Custom | Yes |
| `swamp/handy-willow-forest.json` | swamp | Custom | Yes |
| `swamp/marsh.json` | swamp | Custom | Yes |
| `swamp/roofed-forest.json` | swamp | Custom | Yes |
| `swamp/roofed-forest-extended.json` | swamp | Custom | Yes |
| `swamp/roofed-wayward.json` | swamp | Custom | Yes |
| `swamp/roofed-wayward-extended.json` | swamp | Custom | Yes |
| `swamp/swamp-forest.json` | swamp | Custom | Yes |
| `swamp/willow-forest.json` | swamp | Custom | Yes |
| `swamp/willow-forest-extended.json` | swamp | Custom | Yes |
| `tropical/cinderfall.json` | tropical | Custom | Yes |
| `tropical/cinderfall-lava.json` | tropical | Custom | Yes |
| `tropical/cinderfall-springs.json` | tropical | Custom | Yes |
| `tropical/volcanic-plains.json` | tropical | Custom | Yes |
| `tundra/frosted-peaks-extended.json` | tundra | Custom | Yes |
| `tundra/maple-forest.json` | tundra | Custom | Yes |
| `vanilla/badlands.json` | vanilla | Vanilla | Yes |
| `vanilla/badlands__ashwood.json` | vanilla | Vanilla | Yes |
| `vanilla/bamboo_jungle.json` | vanilla | Vanilla | Yes |
| `vanilla/birch_forest.json` | vanilla | Vanilla | Yes |
| `vanilla/birch_forest__tall.json` | vanilla | Vanilla | Yes |
| `vanilla/birch_forest__young.json` | vanilla | Vanilla | Yes |
| `vanilla/cherry_grove.json` | vanilla | Vanilla | Yes |
| `vanilla/cherry_grove__spiral.json` | vanilla | Vanilla | Yes |
| `vanilla/cherry_grove__tall.json` | vanilla | Vanilla | Yes |
| `vanilla/cherry_grove__young.json` | vanilla | Vanilla | Yes |
| `vanilla/dark_forest.json` | vanilla | Vanilla | Yes |
| `vanilla/dark_forest__blight.json` | vanilla | Vanilla | Yes |
| `vanilla/dark_forest__dense.json` | vanilla | Vanilla | Yes |
| `vanilla/dark_forest__giant.json` | vanilla | Vanilla | Yes |
| `vanilla/dark_forest__spiral.json` | vanilla | Vanilla | Yes |
| `vanilla/desert.json` | vanilla | Vanilla | Yes |
| `vanilla/eroded_badlands.json` | vanilla | Vanilla | Yes |
| `vanilla/flower_forest.json` | vanilla | Vanilla | Yes |
| `vanilla/flower_forest__young.json` | vanilla | Vanilla | Yes |
| `vanilla/forest.json` | vanilla | Vanilla | Yes |
| `vanilla/forest__giant.json` | vanilla | Vanilla | Yes |
| `vanilla/forest__tall.json` | vanilla | Vanilla | Yes |
| `vanilla/forest__young.json` | vanilla | Vanilla | Yes |
| `vanilla/frozen_peaks.json` | vanilla | Vanilla | Yes |
| `vanilla/grove.json` | vanilla | Vanilla | Yes |
| `vanilla/ice_spikes.json` | vanilla | Vanilla | Yes |
| `vanilla/jagged_peaks.json` | vanilla | Vanilla | Yes |
| `vanilla/jungle.json` | vanilla | Vanilla | Yes |
| `vanilla/jungle__ember.json` | vanilla | Vanilla | Yes |
| `vanilla/jungle__giant.json` | vanilla | Vanilla | Yes |
| `vanilla/jungle__sparse.json` | vanilla | Vanilla | Yes |
| `vanilla/lake.json` | vanilla | Vanilla | Yes |
| `vanilla/mangrove_swamp.json` | vanilla | Vanilla | Yes |
| `vanilla/meadow.json` | vanilla | Vanilla | Yes |
| `vanilla/meadow__cloud.json` | vanilla | Vanilla | Yes |
| `vanilla/meadow__giant.json` | vanilla | Vanilla | Yes |
| `vanilla/mushroom_fields.json` | vanilla | Vanilla | Yes |
| `vanilla/old_growth_birch_forest.json` | vanilla | Vanilla | Yes |
| `vanilla/old_growth_pine_taiga.json` | vanilla | Vanilla | Yes |
| `vanilla/old_growth_spruce_taiga.json` | vanilla | Vanilla | Yes |
| `vanilla/plains.json` | vanilla | Vanilla | Yes |
| `vanilla/plains__sparse.json` | vanilla | Vanilla | Yes |
| `vanilla/puddle.json` | vanilla | Vanilla | Yes |
| `vanilla/savanna.json` | vanilla | Vanilla | Yes |
| `vanilla/savanna__sparse.json` | vanilla | Vanilla | Yes |
| `vanilla/savanna__tall.json` | vanilla | Vanilla | Yes |
| `vanilla/savanna_plateau.json` | vanilla | Vanilla | Yes |
| `vanilla/snowy_plains.json` | vanilla | Vanilla | Yes |
| `vanilla/snowy_slopes.json` | vanilla | Vanilla | Yes |
| `vanilla/snowy_taiga.json` | vanilla | Vanilla | Yes |
| `vanilla/snowy_taiga__glacier.json` | vanilla | Vanilla | Yes |
| `vanilla/snowy_taiga__young.json` | vanilla | Vanilla | Yes |
| `vanilla/sparse_jungle.json` | vanilla | Vanilla | Yes |
| `vanilla/stony_peaks.json` | vanilla | Vanilla | Yes |
| `vanilla/stony_shore.json` | vanilla | Vanilla | Yes |
| `vanilla/sunflower_plains.json` | vanilla | Vanilla | Yes |
| `vanilla/swamp.json` | vanilla | Vanilla | Yes |
| `vanilla/swamp__giant.json` | vanilla | Vanilla | Yes |
| `vanilla/taiga.json` | vanilla | Vanilla | Yes |
| `vanilla/taiga__giant.json` | vanilla | Vanilla | Yes |
| `vanilla/taiga__tall.json` | vanilla | Vanilla | Yes |
| `vanilla/windswept_forest.json` | vanilla | Vanilla | Yes |
| `vanilla/windswept_forest__young.json` | vanilla | Vanilla | Yes |
| `vanilla/windswept_gravelly_hills.json` | vanilla | Vanilla | Yes |
| `vanilla/windswept_hills.json` | vanilla | Vanilla | Yes |
| `vanilla/windswept_savanna.json` | vanilla | Vanilla | Yes |
| `vanilla/wooded_badlands.json` | vanilla | Vanilla | Yes |
| `temperate/auroral-garden.json` (`minecraft:grove`, rarity 15; reward biome, Allay spawns) | temperate | Custom | Yes |
| `hot/glass-beach.json` (`minecraft:beach`/`DESERT`, rarity 8; hot sea-glass shore) | hot (shore) | Custom | Yes |
| `seasonal/spring-meadow.json` (`CHERRY_GROVE`, rarity 1; Spring seasonal region signature biome) | seasonal | Custom | Yes |
| `seasonal/summer-strand.json` (`BEACH`, rarity 1; Summer seasonal region signature biome) | seasonal | Custom | Yes |
| `seasonal/autumnal-canopy.json` (`FOREST`, rarity 1; Autumn seasonal region signature biome) | seasonal | Custom | Yes |
| `seasonal/winter-waste.json` (`SNOWY_TAIGA`, rarity 1; Winter seasonal region signature biome) | seasonal | Custom | Yes |
| `seasonal/spring/light-meadow-bright.json` (`CHERRY_GROVE`, rarity 1; Spring colour-mix child, fresh lime) | seasonal | Custom | Yes |
| `seasonal/spring/light-meadow-soft.json` (`CHERRY_GROVE`, rarity 1; Spring colour-mix child, pale spring-green) | seasonal | Custom | Yes |
| `seasonal/summer/deep-strand-forest.json` (`BEACH`, rarity 1; Summer colour-mix child, deep verdant) | seasonal | Custom | Yes |
| `seasonal/summer/deep-strand-meadow.json` (`BEACH`, rarity 1; Summer colour-mix child, pine verdant) | seasonal | Custom | Yes |
| `seasonal/autumn/warm-canopy-red.json` (`FOREST`, rarity 1; Autumn colour-mix child, crimson) | seasonal | Custom | Yes |
| `seasonal/autumn/warm-canopy-gold.json` (`FOREST`, rarity 1; Autumn colour-mix child, amber) | seasonal | Custom | Yes |

Counts: 129 implemented overlay files (62 Custom + 67 Vanilla). Auroral Garden and Glass
Beach are built (overlay JSON + region wiring). All four seasonal profiles (Spring, Summer,
Autumn, Winter) are now built, so no design-catalog biomes remain outstanding. Four prior
design-catalog candidates (Blooming Plateau, Lavender Forest,
Orchid Swamp, Warped Mesa) are cut per the latest decision - see the "Cut or
already-covered" table below.

### Design-catalog keepers (now implemented)

Both keepers are now live overlay biomes:
- `iris/pack-overlay/biomes/temperate/auroral-garden.json` -> wired into `regions/temperate.json` `landBiomes`.
- `iris/pack-overlay/biomes/hot/glass-beach.json` -> wired into `regions/hot.json` `shoreBiomes`.

**Auroral Garden** (`temperate`, derivative `minecraft:grove`, rarity 15; reward biome).
Look: a luminous twilight sanctuary. Open rainbow-birch and fir forest over a frosted,
snow-dusted grass floor tinted pale teal/lilac (aurora wash). Scatter icy iris flowers,
lilac, and the odd patch of pink/cyan petals for the rainbow read; thin snow layers and
sparse blue-ice glints sell the "frosted" feel without making it a cold biome visually.
Ambient: cool aurora-coloured sky/fog tint and Allay natural spawns. Moderate rolling
hills, generous open sightlines for fairy-tale cottage builds. LIMITATION: the biome wires
the `structure/allay` spawner, but Iris biome-level `entitySpawners` are additive to the
region's; vanilla Iris cannot subtract the temperate region's hostile spawners, so a
guaranteed "no hostiles" read is not achievable at the biome level (would need a dedicated
region or a future Iris mechanic).

**Glass Beach** (`hot` category, derivative `minecraft:beach`, rarity 8).
Look: a flat, sun-baked shoreline where smoothed multi-colour sea glass replaces surface
sand, white sand subsurface, shallow water, glass-pane "driftwood" decorators. DESIGN
DECISION: the sea-glass concept (glass = melted sand) is now justified by the ambient heat of
a hot biome rather than by lava/volcanic adjacency. It stays a proper beach/shore (derivative
`minecraft:beach`) but is sited under the `hot` category so its warm climate alone sells the
melted-glass story - no lava neighbour required and no adjacency-gating dependency. A
volcanic region is feasible but not wanted right now. If a future Iris mechanic allows
biome-adjacency gating, an optional revisit could bias it toward the Embertide/Cinderfall
volcanic fringes.

Volcano families (Frostpeak, Cinderfall, Ashcrown, Embertide, Cinnabar Mesa) each have a
parent + `-lava` caldera child + `-springs` Hot Springs child; all rows are enumerated above.

---

## Seasonal Biome Framework

Region-level seasonal profiles sharing a geographic footprint (palette/decorator/spawner
swap). All four are now implemented as distinct regions wired into
`dimensions/overworld.json` `regions`.

| Season | Region tag | Signature features | Vanilla mob substitutes | Source | Implemented |
|---|---|---|---|---|---|
| Summer Strand | `season_summer` | Acacia palms, wild melon, white sand | turtle (crab), dolphin | Custom | Yes |
| Autumnal Canopy | `season_autumn` | Oak/birch canopy, fern + pumpkin litter | fox/rabbit/horse (deer), husk (scarecrow) | Custom | Yes |
| Winter Waste | `season_winter` | Deep/powder snow, glacier-pine + spruce | polar bear (penguin), stray (iceologer) | Custom | Yes |
| Spring Meadow | `season_spring` | Cherry blossom, birch, bee hives, sniffers | sniffer + bee | Custom | Yes |

Delivery (per season): region `regions/season_<s>.json`, signature biome
`biomes/seasonal/<biome>.json`, and spawner `spawners/seasonal/<s>.json`.

Seasonal colour mixing (Spring, Summer, Autumn): each signature biome carries a base
`customDerivitives` foliage/grass tint and blends in two colour-variant child biomes via
`children` + `childStyle` (CELLULAR, zoom 0.35) so its vegetation reads as a mingled palette -
warm crimson/amber for Autumn, darker greens for Summer, lighter greens for Spring. The
child biomes (`seasonal/<season>/...`) mirror the parent terrain and differ only by colour.
Winter intentionally has no colour-mix children.
Vanilla-Iris-expressible features only; per the "work the mobs in as vanilla variants"
decision, non-vanilla catalog mobs are substituted with the closest vanilla entity (see
table) rather than dropped. Purely decorative non-vanilla content (coconut clusters,
abandoned greenhouses) and Spring's mooblooms remain omitted.

### Optional follow-up refinement (not a new biome)

- Switch Frostfire Caves from the `frozen` region `caveBiomes` to Frostpeak's per-biome
  `carvingBiome` field (BIOME-ADDITIONS-EXECUTION-SEQUENCE.md step 2.5, deferred).

---

## Cut or already-covered (do NOT implement as new)

| Concept | Disposition |
|---|---|
| Blooming Plateau | Cut per latest decision (not worth a dedicated biome) |
| Lavender Forest | Cut - redundant with `cherry_grove` variants / Blooming concept |
| Orchid Swamp | Cut - judged not fun enough to build |
| Warped Mesa | Cut per latest decision |
| Lush Desert | Covered by Iris `terralost/ancient-sands`; adopt as-is |
| Canopied Rainforest | Covered by Iris `tropical/rainforest` variants; adopt as-is |
| Abyssal Ocean Trench | Removed - needs 3 custom mobs, not vanilla-Iris expressible |
| Yellowstone / Hot Spring | Replaced by the per-volcano `-springs` child biomes (all live) |
| Sulfur Cube mob | Out of scope - Sulfur Caves shipped terrain + gas only |

---

## See also

- `docs/design/CUSTOM-BIOMES.md` - canonical biome design catalog
- `docs/design/BIOMES.md` - vanilla biome ID reference
- `docs/scratch/BIOME-ADDITIONS-EXECUTION-SEQUENCE.md` - what was built in the last pass
- `docs/design/IRIS-BIOME-RESEARCH.md` - preexisting Iris biomes vs ADR-003
