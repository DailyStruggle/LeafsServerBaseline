# Biome Structure Concepts

A working list of our biomes and the structure concepts that fit each one. The goal is a curated set of 1-2 structure ideas per biome, with varying rarity, drawn from what has proven popular and well-built in established Minecraft modpacks and datapack-based NBT/jigsaw structure sets.

This is a **design catalog**, not an implementation spec. It pairs the biomes we already define (see [CUSTOM-BIOMES.md](CUSTOM-BIOMES.md)) with structure themes that are known to read well in-world and reward exploration. Use it to decide which structures to author or adapt next.

> Asset-sourcing constraint: any third-party-derived structure assets are quarantined per [ADR-004](adr/ADR-004-third-party-derived-structure-assets.md) and are never redistributed. The entries below are **concepts and references**, not bundled assets. Where a real pack is named, it is a design reference for what works, not an instruction to ship its files.

---

## Rarity Tiers

We reuse the project rarity convention (1 = most common, higher = rarer; see CUSTOM-BIOMES.md). For structures we group them into named tiers so spacing/separation can be tuned consistently:

| Tier | Rarity band | Feel | Typical spacing intent |
|---|---|---|---|
| Common | 1-3 | Seen most outings; low loot, high ambience | Frequent (small separation) |
| Uncommon | 4-8 | A pleasant find within a session | Moderate |
| Rare | 9-19 | A genuine landmark; worth a trek | Wide |
| Hidden | 20+ | Destination/treasure-map tier; one of a kind in a region | Very wide; needs a navigation aid |

"Hidden" aligns with the volcano biomes in CUSTOM-BIOMES.md (rarity 20, treasure-map navigation aid).

---

## Reference Packs (what "worked")

Design references for structures that consistently land well, are vanilla-faithful, and translate cleanly to NBT/jigsaw:

- **Repurposed Structures** - biome-spread variants of vanilla structures; the model for "same idea, new biome palette".
- **Towns and Towers** - village/outpost expansions per biome; strong silhouettes, low loot creep.
- **When Dungeons Arise** / **When Dungeons Arise: Forge** - large set-piece dungeons (keeps, galleons, mushroom houses) tuned to specific biomes.
- **Dungeons and Taverns** / **Dungeons Arise - Seven Seas** - taverns, naval set-pieces, pillager forts.
- **ChoiceTheorem's Overhauled Village** - biome-specific village styles; the bar for "village that belongs here".
- **Stoneholm** / **YUNG's Better Dungeons / Mineshafts / Strongholds** - underground/cave structure depth.
- **Explorer's Compass / treasure-map hooks** - the navigation pattern for Hidden-tier finds.

---

## Custom Biomes

### Temperate

#### Blooming Plateau
- **Apiary Hamlet** (Common, rarity 3) - small cottagecore farm cluster with bee nests and dye plots. Ref: ChoiceTheorem meadow/plains village.
- **Floating Dye Mill** (Uncommon, rarity 6) - windmill-driven dye works on the cliff edge. Ref: Towns and Towers windmill.

#### Lavender Forest
- **Wizard's Spire** (Uncommon, rarity 7) - lone purple-roofed mage tower with a small library cache. Ref: When Dungeons Arise small tower.
- **Garden Estate Ruin** (Rare, rarity 10) - overgrown formal-garden manor, hedge maze remnants. Ref: Repurposed Structures mansion variant.

#### Orchid Swamp
- **Stilt Village** (Common, rarity 3) - elevated walkway settlement over the water. Ref: Towns and Towers swamp village.
- **Sunken Fisher's Lodge** (Uncommon, rarity 6) - half-flooded longhouse with drowned-loot chests. Ref: Dungeons and Taverns.

#### Auroral Garden
- **Allay Sanctuary** (Rare, rarity 12) - peaceful shrine matching the reward-biome tone; no hostile spawners, lore/loot cache only. Ref: When Dungeons Arise small sanctuary.

### Hot / Mesa

#### Warped Mesa
- **Void Research Outpost** (Uncommon, rarity 8) - half-buried sci-fi/alien dig site in the blue clay. Ref: Repurposed Structures desert outpost reskin.
- **Spire Observatory** (Rare, rarity 14) - tower wedged into an eroded spire, telescope/ender motifs. Ref: When Dungeons Arise tower.

### Ocean / Shore

#### Glass Beach
- **Glassworks Studio** (Common, rarity 4) - modernist beach house + kiln, decorative glass stock. Ref: Towns and Towers beach house.
- **Sea-Glass Lighthouse** (Uncommon, rarity 7) - shoreline lighthouse, navigation-marker role. Ref: Dungeons Arise - Seven Seas lighthouse.

---

## Volcano Biomes (Hidden tier, rarity 20)

All five share the Hidden-tier treasure-map navigation pattern. One signature set-piece each, sized to the caldera.

| Biome | Structure concept | Tier | Reference |
|---|---|---|---|
| Cinderfall (jungle) | **Crimson Caldera Fortress** - terracotta ramparts on the rim, lava-pool courtyard | Hidden (20) | When Dungeons Arise keep |
| Frostpeak (ice) | **Ice-and-Fire Shrine** - ice palace ringing the steaming lava pool | Hidden (20) | When Dungeons Arise mountain shrine |
| Ashcrown (mountain) | **Geothermal Research Station** - industrial vents tapping the caldera | Hidden (20) | Towns and Towers industrial outpost |
| Embertide (island) | **Pirate Slope Outpost** - docks at the sand rim, watchtowers up the cone | Hidden (20) | Dungeons Arise - Seven Seas |
| Cinnabar Mesa | **Corrupted Mesa Citadel** - basalt-capped throne room over the lava caldera | Hidden (20) | When Dungeons Arise citadel |

Hot Springs child biomes (per volcano) suit a small, low-loot **Bathhouse Ruin** (Uncommon, rarity 5) - a calm counterpoint to the caldera set-piece.

---

## New Biomes (from BIOME-ADDITIONS-PLAN)

| Biome | Structure concept | Tier | Reference |
|---|---|---|---|
| Maple Forest | **Autumn Wayshrine** - small roadside shrine with cozy loot | Common (3) | ChoiceTheorem roadside |
| Maple Forest | **Cider Homestead** - barn + orchard cluster | Uncommon (6) | Towns and Towers farm |
| Boreal Shield | **Nordic Medieval Hall** - timber longhall, fur/axe loot | Uncommon (5) | Dungeons and Taverns hall |
| Ashen Plains | **Burned Watchpost** - charred ruined outpost, salvage loot | Common (4) | Repurposed Structures ruined portal/outpost |
| Bryce Spires | **Hoodoo Hermitage** - cliff-dwelling carved into the spires | Rare (11) | When Dungeons Arise cliff dwelling |
| Floating Islands | **Skybound Ruin** - broken aerial platform with chain bridges | Rare (15) | When Dungeons Arise floating ruin |
| Mirage Isles | **Mirage Bazaar** - illusory island market, vanishing-loot theme | Hidden (20) | bespoke; treasure-map hook |

---

## Vanilla-Category Biomes (adopt/adapt)

For the vanilla-derived biomes we keep, prefer adapting an existing well-liked structure to the biome palette rather than authoring from scratch.

| Biome group | Structure concept | Tier | Reference |
|---|---|---|---|
| Forest / Birch Forest | **Forester's Cabin** + **Overgrown Ruin** | Common (2) / Uncommon (6) | Repurposed Structures / Towns and Towers |
| Dark Forest | **Witch's Coven Cottage** (alt to vanilla mansion) | Rare (12) | When Dungeons Arise mushroom/witch house |
| Cherry Grove | **Japanese Samurai Temple** - paper walls, katana stands | Rare (10) | bespoke (already in narrative catalog) |
| Old-Growth Pine/Spruce Taiga | **Lumber Camp** + **Hunter's Lodge** | Common (3) / Uncommon (7) | Towns and Towers taiga |
| Jungle | **Eldorado Statue** (signal) -> **Jungle Temple Vault** | Uncommon (6) / Rare (13) | bespoke + Repurposed Structures jungle temple |
| Bamboo Jungle | **Panda Sanctuary Pavilion** | Uncommon (8) | ChoiceTheorem-style pavilion |
| Savanna | **Acacia Trading Post** + **Pillager Kraal** | Common (3) / Uncommon (7) | Towns and Towers savanna |
| Desert / Lush Desert | **Sphinx Ruins / Pyramid Jigsaw** (Iris ancient-sands) + **Oasis Caravan** | Rare (10) / Common (4) | terralost ancient-sands + Towns and Towers |
| Badlands / Mesa | **Abandoned Mineshaft Headframe** + **Gold Rush Boomtown** | Common (3) / Rare (12) | YUNG's mineshafts / Towns and Towers |
| Snowy Plains / Slopes | **Nordic Medieval Hall** (cold variant) + **Frozen Wayshrine** | Uncommon (5) / Common (3) | Dungeons and Taverns |
| Swamp / Mangrove | **Witch Hut Cluster** + **Sunken Galleon** | Common (3) / Rare (14) | When Dungeons Arise galleon |
| Mushroom Fields | **Giant Mushroom House** | Rare (16) | When Dungeons Arise mushroom house |
| Deep Ocean | **Atlantis Ruins** + **Skull Island Vault** | Rare (13) / Hidden (20) | bespoke + Dungeons Arise - Seven Seas |
| Ocean (hidden archipelago) | **Pirate Utopia** - conquerable island base | Hidden (20) | bespoke + Seven Seas |

---

## Cave Biomes (underground structures)

| Cave biome | Structure concept | Tier | Reference |
|---|---|---|---|
| Andesite Caves | **Stoneborne Outpost** - small dwarven dig camp | Common (4) | Stoneholm |
| Ice Caves | **Frozen Adventurer's Camp** - ice-locked supply cache | Uncommon (7) | YUNG's Better Dungeons |
| Desert Caves | **Buried Sand Temple Annex** | Uncommon (8) | Repurposed Structures |
| Crystal Caves | **Prismatic Shrine** - rainbow-glass reward room | Rare (15) | bespoke |
| Amethyst Caves | **Geode Vault** - budding-amethyst treasure pocket | Rare (14) | YUNG's amethyst vault |
| Frostfire Caves | **Soulforge Ruin** - soul-fire smithy, hazard-gated loot | Rare (16) | When Dungeons Arise |
| Mantle Caves | **Deep Forge** - magma-lit ore vault, high-risk loot | Hidden (20) | YUNG's Better Mineshafts deep tier |
| Sulfur Caves | **Toxic Prospector's Dig** - gas-masked salvage camp | Uncommon (8) | bespoke (gas hazard already modeled) |

### Implementation status (cave set)

All eight cave structures are implemented as our own assets (created outright, ADR-004-clean - no third-party files bundled). Each is a single-piece, hand-authored 5x4x5 themed chamber compiled through our `.iob` pipeline:

- Sources: `iris/object-src/jigsaw/cave/<name>.json` (block lists), compiled to `iris/pack-overlay/objects/jigsaw/cave/<name>.iob` via `iris/scripts/json_to_iob.py`.
- Pieces: `iris/pack-overlay/jigsaw-pieces/cave/<name>.json` (each carries a `vanillaLoot` table - vanilla for now; custom loot is a follow-up).
- Structures: `iris/pack-overlay/jigsaw-structures/<name>.json` (`maxDepth: 1`, depth-tuned `overrideYRange`).
- Wiring: each is added to its cave biome's region(s) `jigsawStructures` list. Iris places structures per-region (not per-biome), so this is the closest achievable tie - the same limitation noted for cave biomes in CUSTOM-BIOMES.md.

| Structure | File slug | Region(s) wired | Region rarity | Vanilla loot table | Y range |
|---|---|---|---|---|---|
| Stoneborne Outpost | `stoneborne-outpost` | temperate, forests | 60 (was 600) | `chests/abandoned_mineshaft` | -25..5 |
| Frozen Adventurer's Camp | `frozen-adventurers-camp` | frozen, tundra | 900 (restored from 90 - 90 placed one every few chunks) | `chests/igloo_chest` | 15..65 |
| Buried Sand Temple Annex | `buried-sand-temple-annex` | hot | 90 (was 900) | `chests/desert_pyramid` | 25..65 |
| Prismatic Shrine | `prismatic-shrine` | temperate | 150 (was 1500) | `chests/end_city_treasure` | -55..-15 |
| Geode Vault | `geode-vault` | temperate | 150 (was 1500) | `chests/buried_treasure` | -55..-15 |
| Soulforge Ruin | `soulforge-ruin` | frozen | 1600 (restored from 160 - 160 placed it too often) | `chests/nether_bridge` | -64..-25 |
| Deep Forge | `deep-forge` | hot | 250 (was 2500) | `chests/ruined_portal` | -64..-45 |
| Toxic Prospector's Dig | `toxic-prospectors-dig` | tropical | 90 (was 900) | `chests/abandoned_mineshaft` | 0..55 |

Region `rarity` is the Iris spacing weight (higher = rarer). The initial values (the `was ...` figures) proved too sparse to find in-world, so they were lowered ~10x for visibility; the region JSON keeps the prior value in a `_rarityComment` field next to each entry. Every structure's piece graph resolves to an `.iob` (`jigsaw_validate.py` passes against both `pack-overlay` and `staging`).

---

## See Also

- [CUSTOM-BIOMES.md](CUSTOM-BIOMES.md) - biome catalog, palettes, and the existing Narrative Structures table.
- [BIOMES.md](BIOMES.md) - vanilla biome ID reference.
- [ADR-003](adr/ADR-003-custom-biome-design-goals.md) - biome design goals (silhouette, palette, build-theme anchor).
- [ADR-004](adr/ADR-004-third-party-derived-structure-assets.md) - third-party-derived asset quarantine policy.
- `docs/scratch/BIOME-ADDITIONS-PLAN.md` - full specs for the New Biomes rows above.
