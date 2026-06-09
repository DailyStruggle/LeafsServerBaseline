# Structure Region Variants (design details)

Date: 2026-06-07
Status: Phase A wired (2026-06-07, approved). Phase B (palette variants) built + wired (2026-06-07).

> Scope: the "simple" structures only (single-piece / low `maxDepth`, no `forcePlace`, no
> missing Y-range) that are compatible with the per-region `jigsawStructures` spec. This
> note designs a REGION-FLAVORED VARIANT for each structure-region pairing before wiring,
> as requested. It supersedes the flat "wire it as-is" list discussed earlier.
>
> Related: `docs/scratch/STRUCTURE-INTEGRATION-RESEARCH.md` (the gap audit this builds on),
> `docs/design/BIOME-STRUCTURE-CONCEPTS.md` (design catalog + cave-structure precedent),
> `docs/design/adr/ADR-004-third-party-derived-structure-assets.md` (asset quarantine).

---

## How to read this

- Iris places structures PER REGION, not per biome (the known limitation). A "variant" is a
  region-themed flavor of a base structure so the same idea reads differently across regions.
- **Base def** = the `iris/pack-base/jigsaw-structures/<slug>.json` (vanilla-faithful) or
  `iris/pack-overlay/jigsaw-structures/<slug>.json` (custom) the variant derives from.
- **Region rarity** = Iris spacing weight (higher = rarer). Convention: 800 common,
  1000 standard-common, 1200 standard, 1500+ rare. Underground defs keep their own
  `overrideYRange`; the region rarity is the only per-region knob.
- **New asset?** distinguishes the two delivery phases:
  - **Phase A (wire-as-is)**: reuse the existing base def unchanged; only add a
    `jigsawStructures` entry. No new `.iob`/piece work. Ship first.
  - **Phase B (palette variant)**: author a region-palette piece (new `object-src` ->
    `.iob` + `jigsaw-pieces` + `jigsaw-structures` slug), mirroring the cave-structure
    pipeline in BIOME-STRUCTURE-CONCEPTS.md. Deferred follow-up; ADR-004-clean assets only.

---

## Per-region variant tables

### Temperate (plains / mixed forest)
Existing: village-plains, ruined-portal, dungeon-skeleton/spider/zombie, stoneborne-outpost,
prismatic-shrine, geode-vault.

| Variant | Base def | Rarity | Loot table | Theme / palette | New asset? |
|---|---|---|---|---|---|
| Plains Watchpost | `pillager-outpost` | 1200 | `chests/pillager_outpost` | Vanilla oak/cobble outpost on open grass | Phase A |
| Abandoned Mineshaft | `mineshaft` | 700 | `chests/abandoned_mineshaft` | Underground, biome-agnostic (Y 7-45) | Phase A |

### Forests (dense forest / dark forest)
Existing: village-plains, ruined-portal, dungeon-skeleton/spider/zombie, stoneborne-outpost.

| Variant | Base def | Rarity | Loot table | Theme / palette | New asset? |
|---|---|---|---|---|---|
| Woodland Stockade | `pillager-outpost` | 1200 | `chests/pillager_outpost` | Same frame, mossy/dark-oak accents under canopy | Phase B (palette) |
| Abandoned Mineshaft | `mineshaft` | 700 | `chests/abandoned_mineshaft` | Underground, biome-agnostic | Phase A |

### Frozen (snowy plains / pine / spruce)
Existing: village-snowy, village-small-frosty, ruined-portal, dungeon-skeleton/spider/zombie,
frozen-adventurers-camp, soulforge-ruin.

| Variant | Base def | Rarity | Loot table | Theme / palette | New asset? |
|---|---|---|---|---|---|
| Snowfield Igloo | `igloo` | 800 | `chests/igloo_chest` | Vanilla snow/ice igloo with basement | Phase A |
| Frozen Mineshaft | `mineshaft` | 700 | `chests/abandoned_mineshaft` | Underground, biome-agnostic | Phase A |

### Hot (desert / savanna / mesa)
Existing: village-desert, village-savanna, desert-well, trail-ruins, ruined-portal,
dungeon-skeleton/spider/zombie, buried-sand-temple-annex, deep-forge.

| Variant | Base def | Rarity | Loot table | Theme / palette | New asset? |
|---|---|---|---|---|---|
| Desert Pyramid | `pyramid-desert` | 1200 | `chests/desert_pyramid` | Vanilla sandstone pyramid w/ TNT trap room | Phase A |
| Savanna Kraal | `pillager-outpost` | 1200 | `chests/pillager_outpost` | Acacia-log outpost, dry-grass palette | Phase B (palette) |
| Desert Mineshaft | `mineshaft` | 700 | `chests/abandoned_mineshaft` | Underground, biome-agnostic | Phase A |

### Swamp (swamp / mangrove / dark forest)
Existing: swamp-hut only.

| Variant | Base def | Rarity | Loot table | Theme / palette | New asset? |
|---|---|---|---|---|---|
| Bog Ruined Portal | `ruined-portal` | 1200 | `chests/ruined_portal` | Vanilla ruined portal, mossy/overgrown | Phase A |
| Mire Dungeon (skeleton) | `dungeon-skeleton` | 800 | (def-native) | Underground spawner room | Phase A |
| Mire Dungeon (spider) | `dungeon-spider` | 800 | (def-native) | Underground spawner room | Phase A |
| Mire Dungeon (zombie) | `dungeon-zombie` | 800 | (def-native) | Underground spawner room | Phase A |
| Sunken Mineshaft | `mineshaft` | 700 | `chests/abandoned_mineshaft` | Underground, biome-agnostic | Phase A |

### Terralost (badlands / windswept peaks)
Existing: village-desert only.

| Variant | Base def | Rarity | Loot table | Theme / palette | New asset? |
|---|---|---|---|---|---|
| Eroded Tomb | `pyramid-desert` | 1200 | `chests/desert_pyramid` | Reuse desert pyramid; reads as a badlands tomb | Phase A (then Phase B terracotta palette) |
| Highland Ruined Portal | `ruined-portal` | 1200 | `chests/ruined_portal` | Vanilla ruined portal on stony peaks | Phase A |
| Lost Dungeon (skeleton) | `dungeon-skeleton` | 800 | (def-native) | Underground spawner room | Phase A |
| Lost Dungeon (spider) | `dungeon-spider` | 800 | (def-native) | Underground spawner room | Phase A |
| Lost Dungeon (zombie) | `dungeon-zombie` | 800 | (def-native) | Underground spawner room | Phase A |
| Headframe Mineshaft | `mineshaft` | 700 | `chests/abandoned_mineshaft` | Underground, biome-agnostic | Phase A |

### Tropical (jungle / warm ocean coast)
Existing: shipwreck, buried-treasure, ruined-portal, dungeon-skeleton/spider/zombie,
toxic-prospectors-dig.

| Variant | Base def | Rarity | Loot table | Theme / palette | New asset? |
|---|---|---|---|---|---|
| Jungle Temple | `pyramid-jungle` | 1200 | `chests/jungle_temple` | Vanilla mossy-cobble temple w/ trap + dispensers | Phase A |
| Coral Ocean Ruins | `underwater-ruin` | 1000 | `#minecraft:ocean_ruin` (def) | Warm ocean-ruin set, applies in `ocean/warm` seaBiomes | Phase A |
| Jungle Mineshaft | `mineshaft` | 700 | `chests/abandoned_mineshaft` | Underground, biome-agnostic | Phase A |

### Tundra (taiga / cold forest)
Existing: village-taiga, ruined-portal, dungeon-skeleton/spider/zombie,
frozen-adventurers-camp.

| Variant | Base def | Rarity | Loot table | Theme / palette | New asset? |
|---|---|---|---|---|---|
| Taiga Outpost | `pillager-outpost` | 1200 | `chests/pillager_outpost` | Spruce-log outpost, cold-forest palette | Phase B (palette) |
| Taiga Mineshaft | `mineshaft` | 700 | `chests/abandoned_mineshaft` | Underground, biome-agnostic | Phase A |

### Mushroom (mycelium fields / nether-forest flavored)
Existing: none.

| Variant | Base def | Rarity | Loot table | Theme / palette | New asset? |
|---|---|---|---|---|---|
| Mycelium Hut | `mush-huts` | 700 | (def-native, villager feature) | Custom mushroom-hut, the region's signature build | Phase A |
| Spore Mineshaft | `mineshaft` | 700 | `chests/abandoned_mineshaft` | Underground, biome-agnostic | Phase A |

---

## Deferred (NOT in this pass)

Not "simple"; need isolated testing or large-footprint handling before any region wiring:

| Structure | Reason deferred |
|---|---|
| `trail_chambers` | `forcePlace: true` - over-saturation risk; needs rarity tuning in isolation |
| `ancient-city` | No `overrideYRange` - placement depth unverified |
| `woodland-mansion` | Large footprint; pillar/leak risk on uneven terrain |
| `stronghold` | Special placement semantics (end-portal expectation) |
| `ocean-monument` | Large; needs deep-ocean validation |
| `frost-caldera` | Large set-piece; treat as Hidden-tier landmark, separate pass |

---

## Delivery order

1. **Phase A (wire-as-is)**: DONE 2026-06-07. Added the Phase A rows to each region's
   `jigsawStructures` array (pure JSON, no new assets). All 9 region files parse-validate.
   Note: `jigsaw_validate.py` reports the vanilla defs' `.iob` objects as "missing" against
   the static pack roots - but this is true for the ALREADY-SHIPPED `ruined-portal` and
   `dungeon-*` too: those objects are generated by the deploy build step
   (`build-vanilla-structures.py`), not stored statically. So the new entries are at full
   wiring parity with existing ones. Still needs the user to deploy + `/iris tp` in-world.
2. **Phase B (palette variants)**: DONE 2026-06-07. Authored 4 region-palette pieces
   via `scripts/build-phaseb-structures.py` (deterministic generator, mirroring
   `build-vanilla-structures.py`), each compiled to `.iob` and wired through a
   `jigsaw-pieces/surface/<slug>.json` + `jigsaw-structures/<slug>.json`:
   - `woodland-stockade` (dark-oak) -> forests, rarity 1200
   - `savanna-kraal` (acacia) -> hot, rarity 1200
   - `taiga-outpost` (spruce) -> tundra, rarity 1200
   - `eroded-tomb` (banded terracotta) -> terralost, rarity 1200 (repointed from `pyramid-desert`)
   All 4 pass `jigsaw_validate.py` (every reachable piece resolves to an `.iob`).

   SCALE NOTE: the first hand-authored pass was too small (outposts 5x8x5/87 blocks,
   tomb 7x4x7/84). Compared against the originals via `scripts/nbt_size_inspect.py`
   (vanilla desert pyramid 21x35x21/3820 solid; region temples ~9x16x9/449; region
   watchtowers ~6x11x6/101), the variants were rebuilt larger: outposts now 7x13x7
   (174 blocks, enclosed watchtower) and the tomb 11x6x11 (286 blocks, stepped pyramid
   with a hollow inner chamber) - in line with the project's own bespoke surface
   structures (swamp-hut 7x7x9, shipwreck 5x6x13). Still needs the user to deploy +
   `/iris tp` for in-world placement/loot verification. ADR-004-clean.

## Open questions before implementation

1. Confirm the Phase A rarities (table values) match desired in-world spacing, or adjust.
2. Underwater-ruin in tropical relies on `ocean/warm` appearing in that region's
   `seaBiomes` - confirm it should surface there vs. a dedicated ocean region.
3. Phase B palette variants: author now, or ship Phase A first and revisit after in-world
   review?
