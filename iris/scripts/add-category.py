import os, re

overlay = r'C:\Users\lxgol\IdeaProjects\LeafsServerBaseline\iris\pack-overlay\biomes'

vd_map = {
    'BADLANDS': 'mesa', 'ERODED_BADLANDS': 'mesa',
    'BAMBOO_JUNGLE': 'jungle', 'JUNGLE': 'jungle', 'SPARSE_JUNGLE': 'jungle',
    'BEACH': 'beach', 'SNOWY_BEACH': 'beach',
    'BIRCH_FOREST': 'forest', 'DARK_FOREST': 'forest', 'FLOWER_FOREST': 'forest',
    'FOREST': 'forest', 'OLD_GROWTH_BIRCH_FOREST': 'forest', 'WINDSWEPT_FOREST': 'forest',
    'FROZEN_PEAKS': 'icy', 'SNOWY_PLAINS': 'icy', 'SNOWY_TAIGA': 'taiga',
    'MUSHROOM_FIELDS': 'mushroom',
    'OCEAN': 'ocean', 'FROZEN_OCEAN': 'ocean', 'LUKEWARM_OCEAN': 'ocean', 'WARM_OCEAN': 'ocean', 'DEEP_OCEAN': 'ocean',
    'PLAINS': 'plains',
    'RIVER': 'river', 'FROZEN_RIVER': 'river',
    'SAVANNA': 'savanna',
    'SWAMP': 'swamp',
    'TAIGA': 'taiga', 'OLD_GROWTH_PINE_TAIGA': 'taiga', 'OLD_GROWTH_SPRUCE_TAIGA': 'taiga',
    'DESERT': 'desert',
    'WINDSWEPT_HILLS': 'extreme_hills',
    'THE_VOID': 'none', 'DEEP_DARK': 'none',
}

for root, dirs, files in os.walk(overlay):
    for fname in files:
        if not fname.endswith('.json'):
            continue
        path = os.path.join(root, fname)
        with open(path, 'r', encoding='utf-8') as f:
            raw = f.read()
        if '"category"' in raw:
            continue
        m = re.search(r'"vanillaDerivative"\s*:\s*"([^"]+)"', raw)
        cat = vd_map.get(m.group(1) if m else '', 'plains')
        if m:
            old = m.group(0)
            new = old + ',\n  "category": "' + cat + '"'
            raw = raw.replace(old, new, 1)
            with open(path, 'w', encoding='utf-8', newline='\n') as f:
                f.write(raw)
            print(f'Fixed {fname}: {cat}')
        else:
            print(f'NO vanillaDerivative: {fname}')
