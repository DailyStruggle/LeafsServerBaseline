# Fun Content Backlog (biomes + flora + structures)

Date: 2026-06-09
Status: PROPOSED - none of the entries below are live yet.

Scope: modded-inspired content we could add to the **live vanilla datapack worldgen**
(`datapacks/leaf-worldgen`) "for extra fun". Everything here is expressible with vanilla
datapack mechanics only (biome JSON + `configured_feature`/`placed_feature` + structure-template
`.nbt`/jigsaw), per ADR-005. No mod jars, no custom blocks/items/mobs with client code.

> IMPORTANT: This supersedes the old `BIOME-ADDITIONS-PLAN.md` for tracking purposes. That plan's
> "IMPLEMENTED" status referred to the **retired Iris `pack-overlay`** (now under `legacy/iris/`),
> NOT the live datapack. None of those biomes exist in `leaf-worldgen` today.

---

## Current live biomes (baseline, for reference)

`leaf-worldgen/data/leaf/worldgen/biome/`:

- `forest_*` (core / body / edge)
- `dark_forest_*` (core / body / edge)
- `taiga_*` (core / body / edge)
- `rainbow_forest_01..12`
- `maple_forest_01..08` (warm multicolor variant set)
- `glass_beach`
- `volcanic_mountain`
- `alpine_meadow`, `jacaranda_grove`, `redwood_grove`, `lavender_fields` (Tier 1 #1-5, now LIVE - see below)
  - Maple Forest is now LIVE as a **warm multicolor variant set** `maple_forest_01..08` (the warm-palette cousin of `rainbow_forest`); the original single `maple_forest` biome JSON is retained but no longer placed.

Worldgen wiring: biomes are placed via the overworld override built by
`datapacks/leaf-worldgen/tools/build_overworld_override.py`; flora goes in the biome's
`features` list (vanilla 11-step generation order; surface flora in the
`vegetal_decoration` step, trees just above it).

---

## How to read the effort tiers

- **Tier 1 (low):** New biome JSON only - reuse vanilla `configured_feature`/`placed_feature`
  IDs and a vanilla derivative palette. No new features, no terrain noise, no schematics.
- **Tier 2 (medium):** New biome JSON + a few new `configured_feature`/`placed_feature` files
  (custom flora patches) and/or procedural tree `.nbt` from `tools/tree-gen`.
- **Tier 3 (high):** Requires custom terrain/noise (spires, floating islands, shore edges) or
  new jigsaw structures.

---

## Part 1: Biomes

### Tier 1 - low effort (JSON + existing features)

Status: **IMPLEMENTED / LIVE** in `leaf-worldgen` (JSON-only, vanilla features + tinted
effects). Biomes #1-4 fully replace their vanilla derivative across the whole humidity
span via `build_overworld_override.py` `SPLITS`; #5 (Lavender Fields) is wired as an
additive climate sliver because its intended derivative (`flower_forest`) is already
consumed by the `rainbow_forest` split. Part 2 custom flora and Part 3 structures remain
deferred.

| # | Biome | leaf id | Vanilla derivative | Hook | Flora reused |
|---|---|---|---|---|---|
| 1 | Alpine Meadow | `leaf:alpine_meadow` | `minecraft:meadow` | Open flower-soaked highland; build-friendly | `trees_meadow`, `flower_meadow`, `patch_grass_plain` |
| 2 | Maple Forest | `leaf:maple_forest_01..08` | `minecraft:birch_forest` | Cozy orange/red/gold harvest woods, now a warm multicolor variant set (8-step warm foliage gradient, like rainbow forest) | `leaf:rainbow_trees` (oak-only, clean tint), `forest_flowers`, `patch_pumpkin`, per-variant warm foliage_color |
| 3 | Jacaranda Grove | `leaf:jacaranda_grove` | `minecraft:cherry_grove` | Purple-canopy grove | `trees_cherry`, `flower_cherry`, purple foliage tint |
| 4 | Redwood Grove | `leaf:redwood_grove` | `minecraft:old_growth_pine_taiga` | Towering atmosphere forest | `trees_old_growth_pine_taiga`, `patch_grass_taiga`, `patch_berry_common` |
| 5 | Lavender Fields | `leaf:lavender_fields` | `minecraft:flower_forest` (additive sliver) | Purple bloom sea | `trees_flower_forest`, `flower_flower_forest`, purple tint |

### Tier 2 - medium effort (JSON + new features / trees)

Status: **IMPLEMENTED / LIVE** in `leaf-worldgen` (JSON-only: new biome JSONs + new
`configured_feature`/`placed_feature` flora, vanilla derivative palettes + tinted effects).
Boreal Shield and Bayou fully replace their vanilla derivative across the whole humidity
span via `build_overworld_override.py` `SPLITS`; Ashen Plains and Volcanic Hot Springs are
additive climate slivers clustered around `volcanic_mountain` (so no vanilla biome is
removed). New flora features: `leaf:boreal_boulder`, `leaf:boreal_granite_disk`,
`leaf:dead_oak`, `leaf:ash_disk`, `leaf:magma_seep`, `leaf:bayou_floor`,
`leaf:hot_spring_calcite`, `leaf:hot_spring_ice`.

| # | Biome | leaf id | Vanilla derivative | Hook | New work (LIVE) |
|---|---|---|---|---|---|
| 6 | Boreal Shield | `leaf:boreal_shield` | `minecraft:snowy_taiga` (whole-biome split) | Flat glacier-swept granite; builder-favorite | `boreal_boulder` (granite forest_rock) + `boreal_granite_disk` patch + snowy spruce |
| 7 | Ashen Plains | `leaf:ashen_plains` | hot/dry sliver (volcano outer ring) | Grey ash + standing dead oaks | `dead_oak` tree + `ash_disk` + `magma_seep` patches |
| 8 | Bayou / Cypress Swamp | `leaf:bayou` | `minecraft:mangrove_swamp` (whole-biome split) | Hanging-moss wetlands | `bayou_floor` mud/moss patch + mangrove trees + lily pads |
| 9 | Volcanic Hot Springs | `leaf:volcanic_hot_springs` | pine-taiga-climate sliver near `volcanic_mountain` | Prismatic calcite pools | `hot_spring_calcite` + `hot_spring_ice` (blue-ice glow) pools |

### Tier 3 - high effort (custom terrain / noise)

| # | Biome | Modded source | Vanilla derivative | Hook | Why hard |
|---|---|---|---|---|---|
| 10 | Bryce Spires | Terralith | `minecraft:badlands` | Alien terracotta needles | needs spike/needle terrain noise in the override generator |
| 11 | Floating Islands / Skylands | Terralith | `minecraft:windswept_hills` (or `the_end` sky) | Drifting landmasses over void | inverted-noise terrain; highest wow, riskiest |
| 12 | Mirage Isles | Terralith | `minecraft:beach` | Rare white-sand + turquoise water | shore continentalness edge tuning + rarity gating |

---

## Part 2: Flora (vanilla feature recreations)

All as `configured_feature` + `placed_feature`, added to the `vegetal_decoration` step of the
target biomes. Reuse vanilla blocks only.

- Mixed wildflower patch - allium, cornflower, oxeye daisy, tulips (meadows, lavender, maple).
- Allium/azalea "lavender" patch - dense purple bloom (Lavender Fields).
- Pink-petal carpet - `pink_petals` placement (cherry / jacaranda groves).
- Fallen-log clutter - short stripped/normal log lines (maple, redwood, bayou).
- Pumpkin + gourd + mushroom clusters - autumn dressing (maple forest).
- Fern + sweet-berry undergrowth - taiga/redwood floor.
- Lily-pad + hanging-vine + mangrove-root sets - bayou/cypress swamp.
- Dead-bush + cactus + bone-block accents - ashen plains, badlands-adjacent.
- Calcite-bowl pool + blue-ice glow + bubble-column steam - hot springs.

Flagged tricky (need dedicated single-block features or tiny structure `.nbt`):
**bamboo, sweet_berry_bush, melon** (see `vanilla-biome-content-plan.md`).

---

## Part 3: Structures (vanilla jigsaw / template)

Status update (2026-06-10): themed third-party structure packs are now wired onto the
custom `leaf:*` biomes via three quarantined datapacks (deployed, not committed - assets are
git-ignored per ADR-004; only each pack's `README.md` is tracked):

- `datapacks/structory` (Structory, Stardust Labs) and `datapacks/structory-towers`
  (Structory: Towers) - ruins, towers, outposts mapped per-structure via each structure's
  `biomes` field. NOTE: the Stardust Labs license forbids redistribution, so these are
  quarantined too (not just CC BY-NC-ND packs).
- `datapacks/dungeons-and-taverns` (CC BY-NC-ND) - dungeons/taverns/forts/ruins mapped by
  injecting `leaf:*` ids into the pack's `nova_structures` biome `collections/*` tags.

This covers the "standard injectables / themed villages" intent below for the LIVE biomes;
the bespoke `leaf:*`-authored boss arenas and custom themed builds remain future work.

Themed jigsaw sets, injected per-biome by rarity (see `STRUCTURE-INTEGRATION-RESEARCH.md`).

| Structure theme | Anchor biome | Build style |
|---|---|---|
| Harvest village | Maple Forest | Warm wood, lanterns, market stalls, cider mill |
| Stone frontier outpost | Boreal Shield | Granite walls, iron accents, low profile |
| Post-eruption settlement | Ashen Plains | Blackened stone, forge district, ruins |
| Canyon cliff-dwellings | Bryce Spires | Terracotta carved into spires, rope bridges |
| Sky architecture (airship dock / wizard tower) | Floating Islands | Vertical builds, magic lore anchor |
| Resort / lighthouse / smuggler's cove | Mirage Isles | Open-air, docks |
| Geothermal bathhouse / ranger outpost | Hot Springs | Calcite + pine, pools |

Custom boss arenas (scoped, not built) - "structure-over-biome":
`leaf:caldera_throne`, `leaf:frozen_prison`, `leaf:sunken_tomb`, `leaf:dark_forest_giant`.

Standard vanilla jigsaw injectables (per biome, low effort): ruined portals, trail ruins,
igloos, pillager outposts, trial chambers, ancient-city fragments.

---

## Part 4: Suggested implementation order

1. Tier 1 biomes 1-5 (JSON-only, fastest payoff): Alpine Meadow, Maple Forest, Jacaranda Grove,
   Redwood Grove, Lavender Fields.
2. Part 2 flora features that those biomes depend on (mixed-flower, lavender, pink-petal,
   fallen-log, tall-spruce `.nbt`).
3. Tier 2 biomes 6-9: Boreal Shield, Ashen Plains, Bayou, Hot Springs. **(DONE - LIVE)**
4. Tier 3 biomes 10-12 (custom terrain): Bryce Spires, Floating Islands, Mirage Isles - prototype
   each generator in isolation first.
5. Part 3 structures, starting with standard jigsaw injectables, then themed villages, then boss
   arenas.

---

## Off the list

Anything requiring custom **mobs / blocks / items** with client code (unique BOP/BYG creatures,
modded ores, custom plant blocks) - substitute with the nearest vanilla analog or cut, per the
established "vanilla-only mechanics" rule.
