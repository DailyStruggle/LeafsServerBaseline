# Iris Overworld - Adoption Plan and Tree Inventory
Generated: 2026-06-04 23:38 - heights verified from .iob binary headers

## Adoption Key

| Symbol | Meaning |
|--------|---------|
| (as-is) | Adopt unchanged |
| (trim) | Adopt with reduced spawn rate - see inline note |
| H=N | Verified block height from .iob schematic header |
| [LARGE] | Height >= 45 blocks (harvestable only with scaffolding) |
| [TRIM] | This specific rate should be reduced before going live |

## Verified Height Reference

Key schematics measured from .iob headers:

| Schematic | H | Notes |
|-----------|---|-------|
| tredwood1-5 | 90-110 | Full redwood - atmosphere tree, not harvest |
| tredwoodsmol1-5 | 21-39 | 'Small' redwood - still tall at density |
| tredwoodbee1 | 92 | Bee redwood variant |
| AmyLarge1-4 | 70-88 | Large amethyst crystal tree |
| AmyMed1-4 | 21 | Medium amethyst - harvestable |
| AmyNormal1-4 | 9-10 | Normal amethyst - fine |
| AmySmol1-2 | 4 | Small amethyst - fine |
| sup-pine-1-4 | 39-66 | Sequoia pine - atmosphere tree |
| levergreen1-4 | 42-58 | Tall spruce - borderline |
| sproak/sp1 | 60 | Tall spruce-oak hybrid |
| dotree1-3 | 47-51 | Magic/Ether forest tree - atmosphere |
| smoakog1 | 94 | Massive oak (name misleading - not 1 block) |
| smoakog80 | 75 | Large oak |
| smoakog160 | 60 | Large oak (name misleading - not 160 blocks) |
| jungle/largegeneric1 | 124 | CRITICAL - largest tree in pack |
| jungle/largegeneric2 | 60 | Large jungle tree |
| sakura/genericsak1-2 | 48-51 | Sakura - atmosphere/spectacle |
| darkoak/talldrift1-2 | 76-77 | Tall dark oak - atmosphere (Cambian Drift) |
| toak1-4 | 15-20 | Long oak - FINE, fully harvestable |
| largeponderosa1-2 | 30-32 | Tall birch - borderline but ok |
| antioch1-3 | 13-18 | Birch variant - fine |
| hoakgeneric1-4 | 27-34 | Tall oak - harvestable |
| pollup1-4 | 6-8 | Round snow-capped - fine |
| bonsai/med-1-4 | 12-18 | Bonsai - fine |
| sproak/generic1-4 | 25-30 | Spruce-oak - fine |
| pine1-4 | 25-34 | Standard spruce - fine |
| lfrostgeneric1-2 | 29-36 | Frost spruce - fine |
| vgeneric1 | 28 | Mountain spruce - fine |

## frozen

| Biome | File | Rarity | Derivative | Tree Objects | Palette |
|-------|------|--------|------------|--------------|---------|
| Frozen Hills (as-is) | "hills.json" | 1 | SNOWY_TAIGA | "trees/spruce/lfrostgeneric1" +19 @ 0.62 H=36 [spruce_wood, spruce_leaves, dark_oak_wood, snow] | dark_oak_wood, snow, spruce_leaves, spruce_wood |
| Frozen Hills (as-is) | "hills-extended.json" | 1 | SNOWY_TAIGA | "trees/spruce/pine1" +8 @ 0.12 H=25 [spruce_wood, spruce_leaves, dark_oak_wood, dark_oak_fence, dark_oak_fence_gate, spruce_sapling] | dark_oak_fence, dark_oak_fence_gate, dark_oak_wood, spruce_leaves, spruce_sapling, spruce_wood |
| Ice Spikes (as-is) | "ice-spikes.json" | 1 | SNOWY_PLAINS | none |  |
| Frozen Pine Hills (as-is) | "pine-hills.json" | 1 | SNOWY_TAIGA | "trees/spruce/pine1" +8 @ 0.9 H=25 [spruce_wood, spruce_leaves, dark_oak_wood, dark_oak_fence, dark_oak_fence_gate, spruce_sapling] | dark_oak_fence, dark_oak_fence_gate, dark_oak_wood, spruce_leaves, spruce_sapling, spruce_wood |
| Frozen Pine Plains (as-is) | "pine-plains.json" | 1 | SNOWY_TAIGA | "trees/spruce/pine1" +8 @ 0.2 H=25 [spruce_wood, spruce_leaves, dark_oak_wood, dark_oak_fence, dark_oak_fence_gate, spruce_sapling] | dark_oak_fence, dark_oak_fence_gate, dark_oak_wood, spruce_leaves, spruce_sapling, spruce_wood |
| Frozen Pines (as-is) | "pines.json" | 1 | SNOWY_TAIGA | "trees/spruce/levergreen1" +4 @ 0.5 H=42 [spruce_wood, spruce_leaves] | spruce_leaves, spruce_wood |
| Frozen Plains (as-is) | "plains.json" | 1 | SNOWY_TAIGA | "trees/spruce/pine1" +8 @ 0.6 H=25 [spruce_wood, spruce_leaves, dark_oak_wood, dark_oak_fence, dark_oak_fence_gate, spruce_sapling] | dark_oak_fence, dark_oak_fence_gate, dark_oak_wood, spruce_leaves, spruce_sapling, spruce_wood |
| Frozen Spruce Hills (as-is) | "spruce-hills.json" | 1 | SNOWY_TAIGA | "trees/spruce/levergreen1" +4 @ 0.8 H=42 [spruce_wood, spruce_leaves] | spruce_leaves, spruce_wood |
| Frozen Spruce Hills (as-is) | "spruce-hills-extended.json" | 1 | SNOWY_TAIGA | "trees/spruce/levergreen1" +4 @ 0.8 H=42 [spruce_wood, spruce_leaves] | spruce_leaves, spruce_wood |
| Frozen Spruce Plains (as-is) | "spruce-plains.json" | 1 | SNOWY_TAIGA | "trees/spruce/levergreen1" +4 @ 0.8 H=42 [spruce_wood, spruce_leaves] | spruce_leaves, spruce_wood |
| Winter Forest (trim) | "tundra-winter.json" | 1 | TAIGA | "trees/mixed/tredwood1" +9 @ 0.35 H=93 [LARGE] -> 0.35 -> 0.12 [TRIM] (H=93-110) [dark_oak_wood, spruce_leaves, spruce_wood, birch_leaves]<br>"trees/mixed/tredwoodsmol1" +7 @ 0.45 H=30 -> 0.45 -> 0.20 [TRIM] (H=21-39, dense) [spruce_wood, spruce_leaves, oak_leaves]<br>"trees/mixed/tredwoodbee1" +3 @ 0.01 H=92 [LARGE] [dark_oak_wood, spruce_leaves, spruce_wood, birch_leaves] | birch_leaves, dark_oak_wood, oak_leaves, spruce_leaves, spruce_wood |
| Frozen Vander (as-is) | "vander.json" | 4 | FROZEN_PEAKS | "trees/mushroom/ice1" +8 @ 0.02 H=19<br>"trees/mushroom/froShroom1" +8 @ 0.1 H=13<br>"trees/sproak/generic1" +10 @ 0.45 H=30 [spruce_wood, spruce_leaves, oak_wood, stripped_spruce_wood, spruce_fence, spruce_planks, snow] | oak_wood, snow, spruce_fence, spruce_leaves, spruce_planks, spruce_wood, stripped_spruce_wood |

## hot

| Biome | File | Rarity | Derivative | Tree Objects | Palette |
|-------|------|--------|------------|--------------|---------|
| Hot Desert Dunes (as-is) | "desert-dunes.json" | 1 | DESERT | none |  |
| Hot Desert Dunes Red (as-is) | "desert-dunes-red.json" | 1 | DESERT | none |  |
| Hot Mountains (as-is) | "mountain.json" | 1 | DESERT | none |  |
| Hot Mountain Cliffs (as-is) | "mountain-cliffs.json" | 3 | DESERT | none |  |
| Hot Mountain Middle (as-is) | "mountain-middle.json" | 1 | DESERT | none |  |
| Hot Mountain Plains (as-is) | "mountain-plains.json" | 1 | DESERT | none |  |
| Hot Oasis (as-is) | "oasis.json" | 1 | DESERT | none |  |
| Mesa Valley (as-is) | "small-valley.json" | 1 | SAVANNA | "trees/acacia/vexed1" +2 @ 0.4 H=12 [acacia_wood, acacia_leaves]<br>"trees/acacia/savannaD1" +2 @ 0.07 H=8 [acacia_wood, acacia_leaves]<br>"trees/acacia/savannaF1" +12 @ 0.2 H=10 [acacia_wood, acacia_leaves, acacia_fence]<br>"trees/acacia/savannaS1" +11 @ 0.04 H=3 [acacia_wood, acacia_leaves] | acacia_fence, acacia_leaves, acacia_wood |

## mesa

| Biome | File | Rarity | Derivative | Tree Objects | Palette |
|-------|------|--------|------------|--------------|---------|
| Mesa Blue (as-is) | "blue.json" | 1 | WINDSWEPT_SAVANNA | none |  |
| Mesa Cliffs (as-is) | "cliffs.json" | 1 | WINDSWEPT_SAVANNA | none |  |
| Mesa Dark (as-is) | "dark.json" | 1 | WINDSWEPT_SAVANNA | none |  |
| Mesa Green (as-is) | "green.json" | 1 | WINDSWEPT_SAVANNA | none |  |
| Mesa (as-is) | "mesa.json" | 2 | BADLANDS | none |  |
| Mesa Plateau (as-is) | "plateau.json" | 1 | BADLANDS | none |  |
| Mesa Plateau Dirt (as-is) | "plateau-dirt.json" | 1 | BADLANDS | "trees/acacia/vexed1" +2 @ 0.2 H=12 [acacia_wood, acacia_leaves] | acacia_leaves, acacia_wood |
| Mesa Plateau Dirt high (as-is) | "plateau-dirt-high.json" | 1 | BADLANDS | "trees/acacia/vexed1" +2 @ 0.2 H=12 [acacia_wood, acacia_leaves] | acacia_leaves, acacia_wood |
| Mesa Plateau High (as-is) | "plateau-high.json" | 3 | BADLANDS | none |  |
| Mesa Red (as-is) | "red.json" | 1 | WINDSWEPT_SAVANNA | none |  |
| Mesa Valley (as-is) | "valleys.json" | 1 | SAVANNA | "trees/acacia/vexed1" +2 @ 0.4 H=12 [acacia_wood, acacia_leaves]<br>"trees/acacia/savannaD1" +2 @ 0.07 H=8 [acacia_wood, acacia_leaves]<br>"trees/acacia/savannaF1" +12 @ 0.2 H=10 [acacia_wood, acacia_leaves, acacia_fence]<br>"trees/acacia/savannaS1" +11 @ 0.04 H=3 [acacia_wood, acacia_leaves] | acacia_fence, acacia_leaves, acacia_wood |
| Mesa Yellow (as-is) | "yellow.json" | 1 | WINDSWEPT_SAVANNA | none |  |

## mountain

| Biome | File | Rarity | Derivative | Tree Objects | Palette |
|-------|------|--------|------------|--------------|---------|
| Calcite Peaks (as-is) | "calcite-base.json" | 1 | WINDSWEPT_HILLS | none |  |
| Mountain Cliffs (as-is) | "cliffs.json" | 3 | OLD_GROWTH_SPRUCE_TAIGA | none |  |
| Mountain Cliffs (as-is) | "cliffs-extended.json" | 3 | OLD_GROWTH_SPRUCE_TAIGA | none |  |
| Lower Mountain (as-is) | "Cute_Cliffs.json" | 1 | WINDSWEPT_HILLS | none |  |
| Mountain Middle (as-is) | "Cute_Cliffs+.json" | - | OLD_GROWTH_SPRUCE_TAIGA | none |  |
| Mountain Forest (as-is) | "forest.json" | 1 | OLD_GROWTH_SPRUCE_TAIGA | "trees/spruce/vgeneric1" +31 @ 0.314 H=28 [spruce_wood, spruce_leaves, dark_oak_wood] | dark_oak_wood, spruce_leaves, spruce_wood |
| Mountain Forest Hills (as-is) | "forest-extended.json" | 1 | OLD_GROWTH_SPRUCE_TAIGA | "trees/spruce/vgeneric1" +31 @ 0.42 H=28 [spruce_wood, spruce_leaves, dark_oak_wood] | dark_oak_wood, spruce_leaves, spruce_wood |
| Mountain Hills (as-is) | "hills.json" | 1 | OLD_GROWTH_SPRUCE_TAIGA | none |  |
| Mountain (as-is) | "mountain.json" | 1 | WINDSWEPT_HILLS | none |  |
| Mountain Middle (as-is) | "mountain-extended.json" | - | OLD_GROWTH_SPRUCE_TAIGA | none |  |
| Mountain Plains Hills (as-is) | "mplain-extended.json" | 1 | PLAINS | "trees/oak/truegeneric1" +4 @ 0.07 H=19<br>"trees/oak/lponderosa1" +13 @ 0.28 H=32 [oak_wood, oak_leaves] | oak_leaves, oak_wood |
| Mountain Plains Hills (as-is) | "plain-extended.json" | 1 | PLAINS | "trees/oak/truegeneric1" +4 @ 0.07 H=19 |  |
| Mountain Plains (as-is) | "plains.json" | 1 | OLD_GROWTH_SPRUCE_TAIGA | "trees/sproak/sp1" +5 @ 0.05 H=60 [LARGE] [spruce_wood, spruce_leaves, dark_oak_wood, dark_oak_leaves, oak_leaves] | dark_oak_leaves, dark_oak_wood, oak_leaves, spruce_leaves, spruce_wood |

## mushroom

| Biome | File | Rarity | Derivative | Tree Objects | Palette |
|-------|------|--------|------------|--------------|---------|
| Crimson Mushroom Swamp (as-is) | "crimson-forest.json" | 2 | CRIMSON_FOREST | "trees/mushroom/mushclut1" +9 @ 0.21 H=11<br>"trees/mushroom/mushclut1" +9 @ 0.21 H=11<br>"trees/mushroom/redgeneric1" +11 @ 0.5 H=15<br>"trees/mushroom/redgeneric1" +11 @ 0.5 H=15<br>"trees/mushroom/smolshroom1" +4 @ 0.1 H=2<br>"trees/mushroom/smolshroom1" +4 @ 0.1 H=2 |  |
| Crimson Mushroom Swamp (as-is) | "crimson-forest-extended.json" | 2 | CRIMSON_FOREST | "trees/mushroom/mushclut1" +9 @ 0.21 H=11<br>"trees/mushroom/mushclut1" +9 @ 0.21 H=11<br>"trees/mushroom/redgeneric1" +11 @ 0.5 H=15<br>"trees/mushroom/redgeneric1" +11 @ 0.5 H=15<br>"trees/mushroom/smolshroom1" +4 @ 0.1 H=2<br>"trees/mushroom/smolshroom1" +4 @ 0.1 H=2 |  |
| Mushroom Forest (as-is) | "forest.json" | 1 | MUSHROOM_FIELDS | "trees/mushroom/mushclut1" +9 @ 0.15 H=11<br>"trees/mushroom/mushclut1" +9 @ 0.15 H=11<br>"trees/mushroom/browngeneric1" +10 @ 0.6 H=9<br>"trees/mushroom/redlumotall1" +10 @ 0.05 H=15<br>"trees/mushroom/browngeneric1" +10 @ 0.4 H=9<br>"trees/mushroom/smolshroom1" +4 @ 0.02 H=2<br>"trees/mushroom/smolshroom1" +4 @ 0.05 H=2 |  |
| Mushroom Forest Hills (as-is) | "forest-extended.json" | 1 | MUSHROOM_FIELDS | "trees/mushroom/mushclut1" +9 @ 0.18 H=11<br>"trees/mushroom/mushclut1" +9 @ 0.18 H=11<br>"trees/mushroom/browngeneric1" +10 @ 0.8 H=9<br>"trees/mushroom/redlumotall1" +10 @ 0.05 H=15<br>"trees/mushroom/browngeneric1" +10 @ 0.5 H=9<br>"trees/mushroom/smolshroom1" +4 @ 0.025 H=2<br>"trees/mushroom/smolshroom1" +4 @ 0.1 H=2 |  |
| Mushroom Plains (as-is) | "plains.json" | 1 | MUSHROOM_FIELDS | "trees/mushroom/mushclut1" +9 @ 0.08 H=11<br>"trees/mushroom/mushclut1" +9 @ 0.08 H=11<br>"trees/mushroom/smolshroom1" +4 @ 0.025 H=2<br>"trees/mushroom/smolshroom1" +4 @ 0.1 H=2 |  |
| Mushroom Warped Forest (as-is) | "warped-forest.json" | 2 | WARPED_FOREST | "trees/mushroom/mushclut1" +9 @ 0.21 H=11<br>"trees/mushroom/mushclut1" +9 @ 0.21 H=11<br>"trees/mushroom/redgeneric1" +11 @ 0.5 H=15<br>"trees/mushroom/redgeneric1" +11 @ 0.5 H=15<br>"trees/mushroom/smolshroom1" +4 @ 0.1 H=2<br>"trees/mushroom/smolshroom1" +4 @ 0.1 H=2 |  |
| Mushroom Warped Forest (as-is) | "warped-forest-extended.json" | 2 | WARPED_FOREST | "trees/mushroom/mushclut1" +9 @ 0.21 H=11<br>"trees/mushroom/mushclut1" +9 @ 0.21 H=11<br>"trees/mushroom/redgeneric1" +11 @ 0.5 H=15<br>"trees/mushroom/redgeneric1" +11 @ 0.5 H=15<br>"trees/mushroom/smolshroom1" +4 @ 0.1 H=2<br>"trees/mushroom/smolshroom1" +4 @ 0.1 H=2 |  |

## savanna

| Biome | File | Rarity | Derivative | Tree Objects | Palette |
|-------|------|--------|------------|--------------|---------|
| Savanna Acacia Denmyre (as-is) | "acacia-denmyre.json" | 1 | DESERT | "trees/acacia/denmyre1" +6 @ 0.5 H=26 [acacia_wood, acacia_leaves, acacia_planks, acacia_fence] | acacia_fence, acacia_leaves, acacia_planks, acacia_wood |
| Savanna Cliffs (as-is) | "cliff.json" | 1 | SAVANNA | "trees/acacia/savannaD1" +2 @ 0.07 H=8 [acacia_wood, acacia_leaves]<br>"trees/acacia/savannaF1" +12 @ 0.2 H=10 [acacia_wood, acacia_leaves, acacia_fence]<br>"trees/acacia/savannaS1" +11 @ 0.04 H=3 [acacia_wood, acacia_leaves] | acacia_fence, acacia_leaves, acacia_wood |
| Savanna Cliffs (as-is) | "cliff-extended.json" | 1 | SAVANNA | "trees/acacia/savannaD1" +2 @ 0.07 H=8 [acacia_wood, acacia_leaves]<br>"trees/acacia/savannaF1" +12 @ 0.2 H=10 [acacia_wood, acacia_leaves, acacia_fence]<br>"trees/acacia/savannaS1" +11 @ 0.04 H=3 [acacia_wood, acacia_leaves] | acacia_fence, acacia_leaves, acacia_wood |
| Savanna Forest (as-is) | "forest.json" | 3 | SAVANNA | "trees/oak/dadwood1" +5 @ 0.335 H=18 [oak_wood, stripped_oak_wood, oak_fence] | oak_fence, oak_wood, stripped_oak_wood |
| Savanna Plateau (as-is) | "plateau.json" | 1 | SAVANNA_PLATEAU | "trees/acacia/savannaD1" +2 @ 0.07 H=8 [acacia_wood, acacia_leaves]<br>"trees/acacia/savannaF1" +12 @ 0.2 H=10 [acacia_wood, acacia_leaves, acacia_fence]<br>"trees/acacia/savannaS1" +11 @ 0.04 H=3 [acacia_wood, acacia_leaves] | acacia_fence, acacia_leaves, acacia_wood |
| Savanna (as-is) | "savanna.json" | 1 | SAVANNA | "trees/acacia/savannaD1" +2 @ 0.07 H=8 [acacia_wood, acacia_leaves]<br>"trees/acacia/savannaF1" +12 @ 0.2 H=10 [acacia_wood, acacia_leaves, acacia_fence]<br>"trees/acacia/savannaS1" +11 @ 0.04 H=3 [acacia_wood, acacia_leaves] | acacia_fence, acacia_leaves, acacia_wood |

## swamp

| Biome | File | Rarity | Derivative | Tree Objects | Palette |
|-------|------|--------|------------|--------------|---------|
| Swamp Cambian Drift (as-is) | "cambian-drift.json" | 1 | DARK_FOREST | "trees/darkoak/talldrift1" +8 @ 0.7 H=77 [LARGE] [dark_oak_wood, dark_oak_leaves, spruce_wood, oak_leaves]<br>"trees/mushroom/browngeneric1" +10 @ 0.02 H=9<br>"trees/mushroom/browngeneric1" +10 @ 0.07 H=9 | dark_oak_leaves, dark_oak_wood, oak_leaves, spruce_wood |
| Swamp Cambian Drift (as-is) | "cambian-drift-extended.json" | 1 | DARK_FOREST | "trees/darkoak/talldrift1" +8 @ 0.7 H=77 [LARGE] [dark_oak_wood, dark_oak_leaves, spruce_wood, oak_leaves]<br>"trees/mushroom/browngeneric1" +10 @ 0.02 H=9<br>"trees/mushroom/browngeneric1" +10 @ 0.07 H=9 | dark_oak_leaves, dark_oak_wood, oak_leaves, spruce_wood |
| The Creaks (as-is) | "creaks.json" | 15 | SWAMP | "trees/oak/troofed1" +9 @ 1 H=16 [oak_wood, stripped_dark_oak_wood, dark_oak_leaves, spruce_leaves, vine]<br>"trees/oak/mroofed1" +11 @ 0.5 H=12<br>"trees/mushroom/browngeneric1" +10 @ 0.02 H=9<br>"trees/mushroom/browngeneric1" +10 @ 0.07 H=9 | dark_oak_leaves, oak_wood, spruce_leaves, stripped_dark_oak_wood, vine |
| Swamp Denmyre (as-is) | "denmyre.json" | 1 | SWAMP | "trees/acacia/denmyre1" +6 @ 0.7 H=26 [acacia_wood, acacia_leaves, acacia_planks, acacia_fence] | acacia_fence, acacia_leaves, acacia_planks, acacia_wood |
| Swamp Haunted Hands Forest (as-is) | "handy-willow-forest.json" | 1 | SWAMP | "trees/darkoak/generic1" +9 @ 0.632 H=20 [dark_oak_wood, dark_oak_fence, oak_leaves] | dark_oak_fence, dark_oak_wood, oak_leaves |
| Swamp Marsh (as-is) | "marsh.json" | 1 | SWAMP | "trees/jungle/lgeneric1" +7 @ 0.8 H=12 [jungle_wood, jungle_leaves, birch_leaves, vine] | birch_leaves, jungle_leaves, jungle_wood, vine |
| Swamp Marsh Rotten (as-is) | "marsh-rotten.json" | 1 | SWAMP | "trees/oak/dead1" +5 @ 0.4 H=9 [oak_wood, oak_leaves] | oak_leaves, oak_wood |
| Swamp Roofed Forest (as-is) | "roofed-forest.json" | 1 | SWAMP | "trees/oak/mroofed1" +11 @ 0.1 H=12<br>"trees/mushroom/browngeneric1" +10 @ 0.02 H=9<br>"trees/mushroom/browngeneric1" +10 @ 0.07 H=9<br>"trees/oak/troofed1" +9 @ 0.7 H=16 [oak_wood, stripped_dark_oak_wood, dark_oak_leaves, spruce_leaves, vine] | dark_oak_leaves, oak_wood, spruce_leaves, stripped_dark_oak_wood, vine |
| Swamp Roofed Forest (as-is) | "roofed-forest-extended.json" | 1 | SWAMP | "trees/oak/mroofed1" +11 @ 0.1 H=12<br>"trees/mushroom/browngeneric1" +10 @ 0.02 H=9<br>"trees/mushroom/browngeneric1" +10 @ 0.07 H=9<br>"trees/oak/troofed1" +9 @ 0.7 H=16 [oak_wood, stripped_dark_oak_wood, dark_oak_leaves, spruce_leaves, vine] | dark_oak_leaves, oak_wood, spruce_leaves, stripped_dark_oak_wood, vine |
| Swamp Roofed Wayward (as-is) | "roofed-wayward.json" | 1 | SWAMP | "trees/oak/troofed1" +9 @ 0.5 H=16 [oak_wood, stripped_dark_oak_wood, dark_oak_leaves, spruce_leaves, vine]<br>"trees/oak/mroofed1" +11 @ 0.5 H=12<br>"trees/mushroom/browngeneric1" +10 @ 0.02 H=9<br>"trees/mushroom/browngeneric1" +10 @ 0.07 H=9 | dark_oak_leaves, oak_wood, spruce_leaves, stripped_dark_oak_wood, vine |
| Swamp Roofed Wayward (as-is) | "roofed-wayward-extended.json" | 1 | SWAMP | "trees/oak/troofed1" +9 @ 0.5 H=16 [oak_wood, stripped_dark_oak_wood, dark_oak_leaves, spruce_leaves, vine]<br>"trees/oak/mroofed1" +11 @ 0.5 H=12<br>"trees/mushroom/browngeneric1" +10 @ 0.02 H=9<br>"trees/mushroom/browngeneric1" +10 @ 0.07 H=9 | dark_oak_leaves, oak_wood, spruce_leaves, stripped_dark_oak_wood, vine |
| Swamp Forest (as-is) | "swamp-forest.json" | 2 | SWAMP | "trees/mixed/dotree1" +9 @ 0.65 H=49 [LARGE] [dark_oak_wood, dark_oak_leaves, stripped_dark_oak_wood, spruce_wood, dark_oak_fence, oak_fence, spruce_fence, dark_oak_slab]<br>"trees/acacia/17" +1 @ 0.3 H=3 [acacia_wood, acacia_leaves, acacia_planks, acacia_fence] | acacia_fence, acacia_leaves, acacia_planks, acacia_wood, dark_oak_fence, dark_oak_leaves, dark_oak_slab, dark_oak_wood, oak_fence, spruce_fence, spruce_wood, stripped_dark_oak_wood |
| Swamp Mangrove Forest (as-is) | "swamp-mangrove-lake.json" | 2 | SWAMP | "trees/mangrove/mangrove1" +19 @ 0.45 H=48 [LARGE] [mangrove_wood, mangrove_leaves, mangrove_roots, jungle_leaves, oak_fence] | jungle_leaves, mangrove_leaves, mangrove_roots, mangrove_wood, oak_fence |
| Swamp Puddle (as-is) | "swamp-puddle.json" | 2 | SWAMP | "trees/willow/t1" +11 @ 0.5 |  |
| Swamp Willow Forest (as-is) | "willow-forest.json" | 1 | SWAMP | "trees/darkoak/generic1" +9 @ 0.7 H=20 [dark_oak_wood, dark_oak_fence, oak_leaves]<br>"trees/darkoak/willowgeneric1" +1 @ 0.2 | dark_oak_fence, dark_oak_wood, oak_leaves |
| Swamp Willow Forest (as-is) | "willow-forest-extended.json" | 1 | SWAMP | "trees/darkoak/generic1" +9 @ 0.7 H=20 [dark_oak_wood, dark_oak_fence, oak_leaves]<br>"trees/darkoak/willowgeneric1" +1 @ 0.2 | dark_oak_fence, dark_oak_wood, oak_leaves |

## temperate

| Biome | File | Rarity | Derivative | Tree Objects | Palette |
|-------|------|--------|------------|--------------|---------|
| Temperate Birch Denmyre (as-is) | "birch-denmyre.json" | 1 | FOREST | "trees/acacia/denmyre1" +6 @ 0.5 H=26 [acacia_wood, acacia_leaves, acacia_planks, acacia_fence] | acacia_fence, acacia_leaves, acacia_planks, acacia_wood |
| Birch Forest (as-is) | "birch-forest.json" | 6 | BIRCH_FOREST | "trees/birch/antioch3" +10 @ 0.29 H=13 [birch_wood, birch_leaves]<br>"trees/birch/forest1" +3 @ 0.08<br>"trees/birch/antioch3b" +3 @ 0.08 | birch_leaves, birch_wood |
| Birch Forest (trim) | "birch-forest-extended.json" | 3 | BIRCH_FOREST | "trees/birch/antioch3" +10 @ 1 H=13 -> 1.0 -> 0.4 [TRIM] (density, not height) [birch_wood, birch_leaves]<br>"trees/birch/forest1" +3 @ 0.08<br>"trees/birch/antioch3b" +3 @ 0.08 -> 1.0 -> 0.4 [TRIM] (density, not height) | birch_leaves, birch_wood |
| Birch Tall Forest (trim) | "birch-tall.json" | 6 | BIRCH_FOREST | "trees/birch/largeponderosa1" +19 @ 0.39 H=30 -> 0.39 -> 0.15 [TRIM] (H=30-32) [birch_wood, birch_leaves]<br>"trees/birch/forest1" +3 @ 0.08<br>"trees/birch/antioch3b" +3 @ 0.08 | birch_leaves, birch_wood |
| Birch Thin Forest (as-is) | "birch-thin.json" | 4 | BIRCH_FOREST | "trees/birch/antioch3" +10 @ 0.9 H=13 [birch_wood, birch_leaves]<br>"trees/birch/antioch3b" +3 @ 0.08 | birch_leaves, birch_wood |
| Calm Plains (as-is) | "calmplains.json" | - | PLAINS | "trees/mixed/pollup1" +11 @ 1 H=6 [birch_wood, birch_leaves, spruce_wood, spruce_leaves, oak_leaves, azalea_leaves] | azalea_leaves, birch_leaves, birch_wood, oak_leaves, spruce_leaves, spruce_wood |
| Cherry Forest (as-is) | "cherry-blossom-forest.json" | 4 | FLOWER_FOREST | "trees/oak/hoakgeneric3" +16 @ 0.2 H=31 [oak_wood, oak_leaves]<br>"trees/sakura/genericsak1" +4 @ 0.25 H=51 [LARGE] [stripped_birch_wood, oak_leaves] | oak_leaves, oak_wood, stripped_birch_wood |
| Combo Forest (as-is) | "combo-forest.json" | 2 | FOREST | "trees/oak/hoakgeneric3" +16 @ 0.07 H=31 [oak_wood, oak_leaves]<br>"trees/oak/antioch3" +10 @ 1<br>"trees/birch/antioch3" +10 @ 0.9 H=13 [birch_wood, birch_leaves]<br>"trees/oak/toak1" +5 @ 0.18 H=16 [oak_wood, oak_leaves, spruce_fence] | birch_leaves, birch_wood, oak_leaves, oak_wood, spruce_fence |
| Combo Forest (as-is) | "combo-forest-extended.json" | 2 | FOREST | "trees/oak/hoakgeneric3" +16 @ 0.07 H=31 [oak_wood, oak_leaves]<br>"trees/oak/antioch3" +10 @ 0.452<br>"trees/birch/antioch3" +10 @ 0.9 H=13 [birch_wood, birch_leaves]<br>"trees/oak/toak1" +5 @ 0.18 H=16 [oak_wood, oak_leaves, spruce_fence] | birch_leaves, birch_wood, oak_leaves, oak_wood, spruce_fence |
| Croak (as-is) | "croak.json" | 1 | PLAINS | "trees/oak/croak1" +18 @ 0.49 H=31 [oak_log, oak_leaves] | oak_leaves, oak_log |
| Fancy Plains (as-is) | "fancyplains.json" | - | PLAINS | "trees/oak/oakFancy1" +13 @ 1 H=16 |  |
| Temperate Flower Forest (trim) | "flower-forest.json" | 3 | FLOWER_FOREST | "trees/oak/hoakgeneric3" +16 @ 0.07 H=31 [oak_wood, oak_leaves]<br>"trees/oak/antioch3" +10 @ 0.77<br>"trees/birch/antioch3" +10 @ 0.07 H=13 -> 1.0 -> 0.4 [TRIM] (density) [birch_wood, birch_leaves]<br>"trees/birch/antioch3" +10 @ 1 H=13 -> 1.0 -> 0.4 [TRIM] (density) [birch_wood, birch_leaves]<br>"trees/oak/thoakgeneric1" +5 @ 0.18 | birch_leaves, birch_wood, oak_leaves, oak_wood |
| Temperate Flower Forest (trim) | "flower-forest-extended.json" | 3 | FLOWER_FOREST | "trees/oak/hoakgeneric3" +16 @ 0.07 H=31 [oak_wood, oak_leaves]<br>"trees/oak/antioch3" +10 @ 0.25<br>"trees/birch/antioch3" +10 @ 0.07 H=13 -> 1.0 -> 0.4 [TRIM] (density) [birch_wood, birch_leaves]<br>"trees/birch/antioch3" +10 @ 1 H=13 -> 1.0 -> 0.4 [TRIM] (density) [birch_wood, birch_leaves]<br>"trees/oak/thoakgeneric1" +5 @ 0.18 | birch_leaves, birch_wood, oak_leaves, oak_wood |
| Temperate Highlands (as-is) | "highlands.json" | 1 | FOREST | none |  |
| Temperate Island (as-is) | "island.json" | 3 | PLAINS | "trees/oak/hoakgeneric3" +16 @ 0.01 H=31 [oak_wood, oak_leaves] | oak_leaves, oak_wood |
| Long tree forest (as-is) | "longtree-forest.json" | 7 | FOREST | "trees/oak/toak1" +7 @ 0.55 H=16 [oak_wood, oak_leaves, spruce_fence] | oak_leaves, oak_wood, spruce_fence |
| Long tree forest (as-is) | "longtree-forest-extended.json" | 3 | FOREST | "trees/oak/toak1" +7 @ 0.55 H=16 [oak_wood, oak_leaves, spruce_fence] | oak_leaves, oak_wood, spruce_fence |
| Lush Plains (as-is) | "lush-plains.json" | 2 | FLOWER_FOREST | "trees/oak/hoakgeneric3" +22 @ 0.01 H=31 [oak_wood, oak_leaves] | oak_leaves, oak_wood |
| Lush Plains Red (as-is) | "lush-plains-red.json" | 3 | FLOWER_FOREST | "trees/oak/hoakgeneric3" +22 @ 0.01 H=31 [oak_wood, oak_leaves] | oak_leaves, oak_wood |
| Lush Plains Yellow (as-is) | "lush-plains-yellow.json" | 3 | FLOWER_FOREST | "trees/oak/hoakgeneric3" +22 @ 0.01 H=31 [oak_wood, oak_leaves] | oak_leaves, oak_wood |
| Meadows (as-is) | "meadows.json" | 4 | FOREST | "trees/oak/hoakgeneric3" +22 @ 0.01 H=31 [oak_wood, oak_leaves] | oak_leaves, oak_wood |
| Oak Denmyre (as-is) | "oak-denmyre.json" | 4 | FOREST | "trees/acacia/denmyre1" +6 @ 0.2 H=26 [acacia_wood, acacia_leaves, acacia_planks, acacia_fence]<br>"trees/oak/generic1" +5 @ 0.4 | acacia_fence, acacia_leaves, acacia_planks, acacia_wood |
| Oak Forest (as-is) | "oak-forest.json" | 3 | FOREST | "trees/oak/hoakgeneric1" +18 @ 0.9 H=34 [oak_wood, oak_leaves] | oak_leaves, oak_wood |
| Oak Forest (as-is) | "oak-forest-extended.json" | 3 | FOREST | "trees/oak/hoakgeneric1" +18 @ 0.9 H=34 [oak_wood, oak_leaves] | oak_leaves, oak_wood |
| Oak Forest (as-is) | "oak-forest-flat.json" | 3 | FOREST | "trees/oak/hoakgeneric1" +18 @ 0.9 H=34 [oak_wood, oak_leaves] | oak_leaves, oak_wood |
| Temperate Osaka Red Forest (as-is) | "osaka-red-forest.json" | 4 | FLOWER_FOREST | "trees/oak/hoakgeneric3" +16 @ 0.2 H=31 [oak_wood, oak_leaves]<br>"trees/sakura/genericsak1" +4 @ 0.35 H=51 [LARGE] [stripped_birch_wood, oak_leaves] | oak_leaves, oak_wood, stripped_birch_wood |
| Osaka Violet Forest (as-is) | "osaka-violet-forest.json" | 4 | FLOWER_FOREST | "trees/oak/hoakgeneric3" +16 @ 0.2 H=31 [oak_wood, oak_leaves]<br>"trees/sakura/genericsak1" +4 @ 0.35 H=51 [LARGE] [stripped_birch_wood, oak_leaves] | oak_leaves, oak_wood, stripped_birch_wood |
| Overflowed (as-is) | "overflowed.json" | 2 | PLAINS | "trees/oak/hoakgeneric3" +16 @ 0.01 H=31 [oak_wood, oak_leaves]<br>"trees/oak/generic1" +5 @ 0.07 | oak_leaves, oak_wood |
| Plains (as-is) | "plains.json" | 2 | PLAINS | "trees/oak/hoakgeneric3" +16 @ 0.01 H=31 [oak_wood, oak_leaves]<br>"trees/oak/generic1" +5 @ 0.07 | oak_leaves, oak_wood |
| Temperate Plateau (trim) | "plateau.json" | 4 | DEEP_LUKEWARM_OCEAN | "trees/oak/hoakgeneric3" +16 @ 0.07 H=31 [oak_wood, oak_leaves]<br>"trees/birch/antioch3" +10 @ 1 H=13 -> 1.0 -> 0.5 [TRIM] (density) [birch_wood, birch_leaves]<br>"trees/oak/thoakgeneric1" +5 @ 0.18 | birch_leaves, birch_wood, oak_leaves, oak_wood |
| Temperate Plateau (trim) | "plateau-extended.json" | 4 | DEEP_LUKEWARM_OCEAN | "trees/oak/hoakgeneric3" +16 @ 0.07 H=31 [oak_wood, oak_leaves]<br>"trees/birch/antioch3" +10 @ 0.78 H=13 -> 1.0 -> 0.5 [TRIM] (density) [birch_wood, birch_leaves]<br>"trees/oak/thoakgeneric1" +5 @ 0.18 | birch_leaves, birch_wood, oak_leaves, oak_wood |
| Tundra Magic Forest (as-is) | "reaching-forest.json" | 7 | WINDSWEPT_HILLS | "trees/mixed/dotree1" +9 @ 0.8 H=49 [LARGE] [dark_oak_wood, dark_oak_leaves, stripped_dark_oak_wood, spruce_wood, dark_oak_fence, oak_fence, spruce_fence, dark_oak_slab] | dark_oak_fence, dark_oak_leaves, dark_oak_slab, dark_oak_wood, oak_fence, spruce_fence, spruce_wood, stripped_dark_oak_wood |
| Tundra Magic Violet Forest (as-is) | "reaching-forest-violet.json" | 7 | WINDSWEPT_HILLS | "trees/mixed/dotree1" +9 @ 0.8 H=49 [LARGE] [dark_oak_wood, dark_oak_leaves, stripped_dark_oak_wood, spruce_wood, dark_oak_fence, oak_fence, spruce_fence, dark_oak_slab] | dark_oak_fence, dark_oak_leaves, dark_oak_slab, dark_oak_wood, oak_fence, spruce_fence, spruce_wood, stripped_dark_oak_wood |
| Rough Plains (as-is) | "roughplains.json" | - | PLAINS | "trees/oak/antioch1" +21 @ 0.5<br>"trees/mixed/tredwood1" +9 @ 0.0025 H=93 [LARGE] [dark_oak_wood, spruce_leaves, spruce_wood, birch_leaves] | birch_leaves, dark_oak_wood, spruce_leaves, spruce_wood |
| Sakura Forest (as-is) | "sakura-forest.json" | 4 | FLOWER_FOREST | "trees/oak/hoakgeneric3" +16 @ 0.2 H=31 [oak_wood, oak_leaves]<br>"trees/sakura/genericsak1" +4 @ 0.2 H=51 [LARGE] [stripped_birch_wood, oak_leaves] | oak_leaves, oak_wood, stripped_birch_wood |
| Shattered Plains (as-is) | "shattered-plains.json" | 2 | PLAINS | "trees/oak/hoakgeneric3" +16 @ 0.01 H=31 [oak_wood, oak_leaves]<br>"trees/oak/generic1" +5 @ 0.07 | oak_leaves, oak_wood |
| Stranged Plains (as-is) | "stranged-plains.json" | 5 | PLAINS | "trees/oak/hoakgeneric3" +16 @ 0.01 H=31 [oak_wood, oak_leaves]<br>"trees/oak/generic1" +5 @ 0.07 | oak_leaves, oak_wood |
| Temperate Wilds (as-is) | "wilds.json" | 1 | PLAINS | none |  |
| Temperate Wilds (as-is) | "wilds-extended.json" | 1 | PLAINS | none |  |

## terralost

| Biome | File | Rarity | Derivative | Tree Objects | Palette |
|-------|------|--------|------------|--------------|---------|
| Alpine Grove (as-is) | "alpine-grove.json" | 1 | FROZEN_PEAKS | "trees/mixed/pollup1" +12 @ 0.6 H=6 [birch_wood, birch_leaves, spruce_wood, spruce_leaves, oak_leaves, azalea_leaves] | azalea_leaves, birch_leaves, birch_wood, oak_leaves, spruce_leaves, spruce_wood |
| Alpine Highlands (as-is) | "alpine-highlands.json" | 1 | TAIGA | "trees/mixed/pollup1" +12 @ 0.6 H=6 [birch_wood, birch_leaves, spruce_wood, spruce_leaves, oak_leaves, azalea_leaves] | azalea_leaves, birch_leaves, birch_wood, oak_leaves, spruce_leaves, spruce_wood |
| Amethyst Rainforest (trim) | "amethyst-canyon.json" | 1 | JUNGLE | "trees/mixed/AmyLarge1" +7 @ 0.2 H=88 [LARGE] -> 0.2 -> 0.08 [TRIM] (H=70-88) [dark_oak_wood, jungle_leaves, oak_leaves]<br>"trees/mixed/AmyMed1" +8 @ 0.1 H=21 [dark_oak_wood, jungle_leaves, oak_leaves, birch_leaves, spruce_leaves]<br>"trees/mixed/AmyNormal1" +10 @ 0.3 H=10 [dark_oak_wood, jungle_leaves, oak_leaves]<br>"trees/mixed/AmySmol1" +11 @ 0.3 H=4 [dark_oak_wood, jungle_leaves, oak_leaves] | birch_leaves, dark_oak_wood, jungle_leaves, oak_leaves, spruce_leaves |
| Amethyst Rainforest (trim) | "amethyst-rainforest.json" | 1 | JUNGLE | "trees/mixed/AmyLarge1" +6 @ 0.3 H=88 [LARGE] -> 0.3 -> 0.1 [TRIM] (H=70-88) [dark_oak_wood, jungle_leaves, oak_leaves]<br>"trees/mixed/AmyMed1" +8 @ 0.6 H=21 -> 0.6 -> 0.3 [TRIM] (density) [dark_oak_wood, jungle_leaves, oak_leaves, birch_leaves, spruce_leaves]<br>"trees/mixed/AmyNormal1" +10 @ 0.8 H=10 [dark_oak_wood, jungle_leaves, oak_leaves]<br>"trees/mixed/AmySmol1" +11 @ 0.8 H=4 [dark_oak_wood, jungle_leaves, oak_leaves] | birch_leaves, dark_oak_wood, jungle_leaves, oak_leaves, spruce_leaves |
| Ancient Sands (as-is) | "ancient-sands.json" | 1 | DESERT | none |  |

## tropical

| Biome | File | Rarity | Derivative | Tree Objects | Palette |
|-------|------|--------|------------|--------------|---------|
| Tropical Bamboo Forest (as-is) | "bamboo-forest.json" | 1 | DESERT | "trees/jungle/bmbogeneric1" +8 @ 0.5<br>"trees/jungle/spire1" +6 @ 0.35 |  |
| Tropical Beach (as-is) | "beach.json" | 1 | JUNGLE | "trees/jungle/palm1" +6 @ 0.34 |  |
| Tropical Beach Bamboo (as-is) | "beach-bamboo.json" | 1 | JUNGLE | none |  |
| Tropical Beach Charred (as-is) | "beach-charred.json" | 1 | DESERT | "trees/jungle/palm1" +6 @ 0.66 |  |
| Tropical Highlands (as-is) | "highlands.json" | 1 | JUNGLE | none |  |
| Tropical Island Beach (as-is) | "island-beach.json" | 1 | JUNGLE | "trees/jungle/palm1" +7 @ 0.4 |  |
| Tropical Jungle Denmyre (as-is) | "jungle-denmyre.json" | 1 | JUNGLE | "trees/acacia/denmyre1" +6 @ 0.5 H=26 [acacia_wood, acacia_leaves, acacia_planks, acacia_fence] | acacia_fence, acacia_leaves, acacia_planks, acacia_wood |
| Extreme mountains (as-is) | "mountain.json" | 1 | JUNGLE | "trees/jungle/lgeneric1" +8 @ 0.63 H=12 [jungle_wood, jungle_leaves, birch_leaves, vine] | birch_leaves, jungle_leaves, jungle_wood, vine |
| Tropical Mountain Extreme (as-is) | "mountain-extreme.json" | 1 | SPARSE_JUNGLE | "trees/jungle/lgeneric1" +8 @ 0.85 H=12 [jungle_wood, jungle_leaves, birch_leaves, vine] | birch_leaves, jungle_leaves, jungle_wood, vine |
| Tropical Mountain Middle (as-is) | "mountain-middle.json" | 1 | JUNGLE | "trees/jungle/lgeneric1" +8 @ 0.489 H=12 [jungle_wood, jungle_leaves, birch_leaves, vine] | birch_leaves, jungle_leaves, jungle_wood, vine |
| Tropical Mountain Plains (as-is) | "mountain-plains.json" | 1 | JUNGLE | "trees/jungle/lgeneric1" +8 @ 0.9 H=12 [jungle_wood, jungle_leaves, birch_leaves, vine] | birch_leaves, jungle_leaves, jungle_wood, vine |
| Tropical Mountain Water (as-is) | "mountain-water.json" | - | JUNGLE | none |  |
| Tropical Plains (as-is) | "plains.json" | 1 | JUNGLE | "trees/jungle/cocogeneric2" +3 @ 0.4<br>"trees/jungle/lgeneric1" +8 @ 0.632 H=12 [jungle_wood, jungle_leaves, birch_leaves, vine] | birch_leaves, jungle_leaves, jungle_wood, vine |
| Tropical Plains Hills (as-is) | "plains-hills.json" | 1 | JUNGLE | "trees/jungle/cocogeneric2" +10 @ 0.4<br>"trees/jungle/lgeneric1" +8 @ 0.71 H=12 [jungle_wood, jungle_leaves, birch_leaves, vine] | birch_leaves, jungle_leaves, jungle_wood, vine |
| Tropical Rainforest (trim) | "rainforest.json" | 1 | JUNGLE | "trees/jungle/cocogeneric2" +10 @ 0.4<br>"trees/jungle/sgeneric1" +3 @ 0.45<br>"trees/jungle/largegeneric1" +4 @ 0.01 H=124 [LARGE] -> 0.01 -> 0.005 [TRIM] (H=124) [jungle_wood, jungle_leaves, oak_wood, vine] | jungle_leaves, jungle_wood, oak_wood, vine |
| Rainforest Hills (trim) | "rainforest-hills.json" | 1 | JUNGLE | "trees/jungle/cocogeneric2" +10 @ 0.7<br>"trees/jungle/cocogeneric2" +10 @ 0.4<br>"trees/jungle/largegeneric1" +4 @ 0.035 H=124 [LARGE] -> 0.035 -> 0.01 [TRIM] (H=124 - critical) [jungle_wood, jungle_leaves, oak_wood, vine] | jungle_leaves, jungle_wood, oak_wood, vine |
| Tropical Rainforest Island (trim) | "rainforest-island.json" | 1 | JUNGLE | "trees/jungle/cocogeneric2" +10 @ 0.4<br>"trees/jungle/sgeneric1" +3 @ 0.45<br>"trees/jungle/largegeneric1" +4 @ 0.01 H=124 [LARGE] -> 0.01 -> 0.005 [TRIM] (H=124) [jungle_wood, jungle_leaves, oak_wood, vine] | jungle_leaves, jungle_wood, oak_wood, vine |
| Tropical Rainforest Wicked (trim) | "rainforest-wicked.json" | 1 | JUNGLE | "trees/jungle/cocogeneric2" +10 @ 0.4<br>"trees/jungle/sgeneric1" +3 @ 0.45<br>"trees/jungle/largegeneric1" +4 @ 0.01 H=124 [LARGE] -> 0.01 -> 0.005 [TRIM] (H=124) [jungle_wood, jungle_leaves, oak_wood, vine] | jungle_leaves, jungle_wood, oak_wood, vine |
| Tropical Rainforest Wicked Child (trim) | "rainforest-wicked-child.json" | 1 | JUNGLE | "trees/jungle/cocogeneric2" +10 @ 0.4<br>"trees/jungle/sgeneric1" +3 @ 0.45<br>"trees/jungle/largegeneric1" +4 @ 0.01 H=124 [LARGE] -> 0.01 -> 0.005 [TRIM] (H=124) [jungle_wood, jungle_leaves, oak_wood, vine] | jungle_leaves, jungle_wood, oak_wood, vine |
| Tropical Submerged Volcanic (as-is) | "submerged-volcanic.json" | 1 | WARM_OCEAN | none |  |
| Tropical Volcanic Plains (as-is) | "volcanic-plains.json" | 1 | THE_VOID | none |  |
| Tropical Volcanoes (as-is) | "volcanoes.json" | 1 | THE_VOID | none |  |
| Tropical Volcanoes Lava (as-is) | "volcanoes-lava.json" | 1 | THE_VOID | none |  |
| Tropical Wilds (trim) | "wilds.json" | 1 | JUNGLE | "trees/jungle/cocogeneric2" +10 @ 0.4<br>"trees/jungle/sgeneric1" +3 @ 0.45<br>"trees/jungle/largegeneric1" +4 @ 0.01 H=124 [LARGE] -> 0.01 -> 0.005 [TRIM] (H=124) [jungle_wood, jungle_leaves, oak_wood, vine] | jungle_leaves, jungle_wood, oak_wood, vine |

## tundra

| Biome | File | Rarity | Derivative | Tree Objects | Palette |
|-------|------|--------|------------|--------------|---------|
| Tundra Autumn (trim) | "autumn.json" | 1 | TAIGA | "trees/mixed/tredwood1" +9 @ 0.35 H=93 [LARGE] -> 0.35 -> 0.12 [TRIM] (H=93-110) [dark_oak_wood, spruce_leaves, spruce_wood, birch_leaves]<br>"trees/mixed/tredwoodsmol1" +7 @ 0.45 H=30 -> 0.45 -> 0.20 [TRIM] (H=21-39, dense) [spruce_wood, spruce_leaves, oak_leaves]<br>"trees/mixed/tredwoodbee1" +3 @ 0.01 H=92 [LARGE] [dark_oak_wood, spruce_leaves, spruce_wood, birch_leaves] | birch_leaves, dark_oak_wood, oak_leaves, spruce_leaves, spruce_wood |
| Tundra Autumn (trim) | "autumn-extended.json" | 1 | TAIGA | "trees/mixed/tredwood1" +9 @ 0.35 H=93 [LARGE] -> 0.35 -> 0.12 [TRIM] (H=93-110) [dark_oak_wood, spruce_leaves, spruce_wood, birch_leaves]<br>"trees/mixed/tredwoodsmol1" +7 @ 0.45 H=30 -> 0.45 -> 0.20 [TRIM] (H=21-39, dense) [spruce_wood, spruce_leaves, oak_leaves]<br>"trees/mixed/tredwoodbee1" +3 @ 0.01 H=92 [LARGE] [dark_oak_wood, spruce_leaves, spruce_wood, birch_leaves] | birch_leaves, dark_oak_wood, oak_leaves, spruce_leaves, spruce_wood |
| Tundra Bonsai Forest (as-is) | "bonsai-extended.json" | 1 | TAIGA | "trees/bonsai/med-1" +3 @ 0.8 H=18 [oak_wood, oak_leaves, spruce_leaves]<br>"trees/mixed/tredwoodsmol1" +7 @ 0.75 H=30 [spruce_wood, spruce_leaves, oak_leaves]<br>"trees/mixed/tredwoodbee1" +3 @ 0.01 H=92 [LARGE] [dark_oak_wood, spruce_leaves, spruce_wood, birch_leaves] | birch_leaves, dark_oak_wood, oak_leaves, oak_wood, spruce_leaves, spruce_wood |
| Tundra Bonsai Forest (as-is) | "bonsai-forest.json" | 1 | TAIGA | "trees/bonsai/med-1" +3 @ 0.8 H=18 [oak_wood, oak_leaves, spruce_leaves]<br>"trees/mixed/tredwoodsmol1" +7 @ 0.75 H=30 [spruce_wood, spruce_leaves, oak_leaves]<br>"trees/mixed/tredwoodbee1" +3 @ 0.01 H=92 [LARGE] [dark_oak_wood, spruce_leaves, spruce_wood, birch_leaves] | birch_leaves, dark_oak_wood, oak_leaves, oak_wood, spruce_leaves, spruce_wood |
| Tundra Ether (as-is) | "ether.json" | 13 | WINDSWEPT_HILLS | "trees/mixed/dotree1" +9 @ 0.8 H=49 [LARGE] [dark_oak_wood, dark_oak_leaves, stripped_dark_oak_wood, spruce_wood, dark_oak_fence, oak_fence, spruce_fence, dark_oak_slab] | dark_oak_fence, dark_oak_leaves, dark_oak_slab, dark_oak_wood, oak_fence, spruce_fence, spruce_wood, stripped_dark_oak_wood |
| Tundra Ether (as-is) | "ether-extended.json" | 13 | WINDSWEPT_HILLS | "trees/mixed/dotree1" +9 @ 0.8 H=49 [LARGE] [dark_oak_wood, dark_oak_leaves, stripped_dark_oak_wood, spruce_wood, dark_oak_fence, oak_fence, spruce_fence, dark_oak_slab] | dark_oak_fence, dark_oak_leaves, dark_oak_slab, dark_oak_wood, oak_fence, spruce_fence, spruce_wood, stripped_dark_oak_wood |
| Tundra Forest (as-is) | "forest.json" | 1 | WINDSWEPT_HILLS | "trees/spruce/levergreen1" +12 @ 0.8 H=42 [spruce_wood, spruce_leaves]<br>"trees/spruce/pine1" +11 @ 0.35 H=25 [spruce_wood, spruce_leaves, dark_oak_wood, dark_oak_fence, dark_oak_fence_gate, spruce_sapling] | dark_oak_fence, dark_oak_fence_gate, dark_oak_wood, spruce_leaves, spruce_sapling, spruce_wood |
| Tundra Forest Cliffs (as-is) | "forest-extended-cliffs.json" | 3 | WINDSWEPT_HILLS | "trees/spruce/levergreen1" +4 @ 0.8 H=42 [spruce_wood, spruce_leaves]<br>"trees/spruce/pine1" +11 @ 0.125 H=25 [spruce_wood, spruce_leaves, dark_oak_wood, dark_oak_fence, dark_oak_fence_gate, spruce_sapling] | dark_oak_fence, dark_oak_fence_gate, dark_oak_wood, spruce_leaves, spruce_sapling, spruce_wood |
| Frosted Peaks (as-is) | "frosted-peaks.json" | 5 | OLD_GROWTH_SPRUCE_TAIGA | none |  |
| Frosted Peaks (as-is) | "frosted-peaks-extended.json" | 5 | OLD_GROWTH_SPRUCE_TAIGA | none |  |
| Tundra Magic Forest (as-is) | "magic-forest.json" | 7 | WINDSWEPT_HILLS | "trees/mixed/dotree1" +9 @ 0.8 H=49 [LARGE] [dark_oak_wood, dark_oak_leaves, stripped_dark_oak_wood, spruce_wood, dark_oak_fence, oak_fence, spruce_fence, dark_oak_slab] | dark_oak_fence, dark_oak_leaves, dark_oak_slab, dark_oak_wood, oak_fence, spruce_fence, spruce_wood, stripped_dark_oak_wood |
| Tundra Magic Forest (as-is) | "magic-forest-extended.json" | 7 | WINDSWEPT_HILLS | "trees/mixed/dotree1" +9 @ 0.8 H=49 [LARGE] [dark_oak_wood, dark_oak_leaves, stripped_dark_oak_wood, spruce_wood, dark_oak_fence, oak_fence, spruce_fence, dark_oak_slab] | dark_oak_fence, dark_oak_leaves, dark_oak_slab, dark_oak_wood, oak_fence, spruce_fence, spruce_wood, stripped_dark_oak_wood |
| Tundra Mountains (as-is) | "mountains.json" | 1 | WINDSWEPT_HILLS | "trees/spruce/levergreen1" +12 @ 0.8 H=42 [spruce_wood, spruce_leaves]<br>"trees/spruce/pine1" +11 @ 0.25 H=25 [spruce_wood, spruce_leaves, dark_oak_wood, dark_oak_fence, dark_oak_fence_gate, spruce_sapling] | dark_oak_fence, dark_oak_fence_gate, dark_oak_wood, spruce_leaves, spruce_sapling, spruce_wood |
| Tundra Magic Forest Cliffs (as-is) | "mountains-extended-cliffs.json" | 1 | WINDSWEPT_HILLS | "trees/spruce/levergreen1" +12 @ 0.8 H=42 [spruce_wood, spruce_leaves]<br>"trees/spruce/pine1" +11 @ 0.125 H=25 [spruce_wood, spruce_leaves, dark_oak_wood, dark_oak_fence, dark_oak_fence_gate, spruce_sapling] | dark_oak_fence, dark_oak_fence_gate, dark_oak_wood, spruce_leaves, spruce_sapling, spruce_wood |
| Tundra Redwood Cliffs (trim) | "redwood-extended-cliffs.json" | 3 | TAIGA | "trees/mixed/tredwood1" +9 @ 0.35 H=93 [LARGE] -> 0.35 -> 0.12 [TRIM] (H=93-110) [dark_oak_wood, spruce_leaves, spruce_wood, birch_leaves]<br>"trees/spruce/sup-pine-1" +11 @ 0.3 H=63 [LARGE] -> 0.30 -> 0.10 [TRIM] (H=52-66) [spruce_wood, spruce_leaves, spruce_fence]<br>"trees/mixed/tredwoodsmol1" +7 @ 0.45 H=30 [spruce_wood, spruce_leaves, oak_leaves]<br>"trees/mixed/tredwoodbee1" +3 @ 0.01 H=92 [LARGE] [dark_oak_wood, spruce_leaves, spruce_wood, birch_leaves] | birch_leaves, dark_oak_wood, oak_leaves, spruce_fence, spruce_leaves, spruce_wood |
| Tundra Redwood Forest (trim) | "redwood-forest.json" | 3 | TAIGA | "trees/mixed/tredwood1" +9 @ 0.35 H=93 [LARGE] -> 0.35 -> 0.12 [TRIM] (H=93-110) [dark_oak_wood, spruce_leaves, spruce_wood, birch_leaves]<br>"trees/mixed/tredwoodsmol1" +7 @ 0.45 H=30 -> 0.45 -> 0.20 [TRIM] (H=21-39, dense) [spruce_wood, spruce_leaves, oak_leaves]<br>"trees/mixed/tredwoodbee1" +3 @ 0.01 H=92 [LARGE] [dark_oak_wood, spruce_leaves, spruce_wood, birch_leaves] | birch_leaves, dark_oak_wood, oak_leaves, spruce_leaves, spruce_wood |
| Tundra Sequoia Redwoods (as-is) | "sequia-redwoods.json" | 1 | TAIGA | "trees/spruce/sup-pine-1" +11 @ 0.69 H=63 [LARGE] [spruce_wood, spruce_leaves, spruce_fence] | spruce_fence, spruce_leaves, spruce_wood |
| Tundra Sequoia Redwoods (as-is) | "sequia-redwoods-extended.json" | 1 | TAIGA | "trees/spruce/sup-pine-1" +11 @ 0.69 H=63 [LARGE] [spruce_wood, spruce_leaves, spruce_fence] | spruce_fence, spruce_leaves, spruce_wood |
| Tundra Spruce Denmyre (as-is) | "spruce-denmyre.json" | 1 | STONY_PEAKS | "trees/acacia/denmyre1" +6 @ 0.5 H=26 [acacia_wood, acacia_leaves, acacia_planks, acacia_fence] | acacia_fence, acacia_leaves, acacia_planks, acacia_wood |
| Tundra Taiga (as-is) | "taiga.json" | 1 | OLD_GROWTH_SPRUCE_TAIGA | "trees/mixed/tredwoodsmol1" +4 @ 0.25 H=30 [spruce_wood, spruce_leaves, oak_leaves]<br>"trees/spruce/pine1" +11 @ 0.05 H=25 [spruce_wood, spruce_leaves, dark_oak_wood, dark_oak_fence, dark_oak_fence_gate, spruce_sapling] | dark_oak_fence, dark_oak_fence_gate, dark_oak_wood, oak_leaves, spruce_leaves, spruce_sapling, spruce_wood |
| Tundra Taiga (as-is) | "taiga-extended.json" | 1 | OLD_GROWTH_SPRUCE_TAIGA | "trees/mixed/tredwoodsmol1" +4 @ 0.25 H=30 [spruce_wood, spruce_leaves, oak_leaves]<br>"trees/spruce/pine1" +11 @ 0.05 H=25 [spruce_wood, spruce_leaves, dark_oak_wood, dark_oak_fence, dark_oak_fence_gate, spruce_sapling] | dark_oak_fence, dark_oak_fence_gate, dark_oak_wood, oak_leaves, spruce_leaves, spruce_sapling, spruce_wood |

