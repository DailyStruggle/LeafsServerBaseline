import os

# Maps filename -> (display_name, derivative_enum, category)
# derivative_enum = uppercase Minecraft biome enum (no minecraft: prefix)
# category = valid Iris category value
biomes = [
    ("plains",                   "Plains",                    "PLAINS",                    "plains"),
    ("sunflower_plains",         "Sunflower Plains",          "SUNFLOWER_PLAINS",          "plains"),
    ("forest",                   "Forest",                    "FOREST",                    "forest"),
    ("flower_forest",            "Flower Forest",             "FLOWER_FOREST",             "forest"),
    ("birch_forest",             "Birch Forest",              "BIRCH_FOREST",              "forest"),
    ("old_growth_birch_forest",  "Old Growth Birch Forest",   "OLD_GROWTH_BIRCH_FOREST",   "forest"),
    ("dark_forest",              "Dark Forest",               "DARK_FOREST",               "forest"),
    ("windswept_forest",         "Windswept Forest",          "WINDSWEPT_FOREST",          "extreme_hills"),
    ("windswept_hills",          "Windswept Hills",           "WINDSWEPT_HILLS",           "extreme_hills"),
    ("windswept_gravelly_hills", "Windswept Gravelly Hills",  "WINDSWEPT_GRAVELLY_HILLS",  "extreme_hills"),
    ("taiga",                    "Taiga",                     "TAIGA",                     "taiga"),
    ("old_growth_pine_taiga",    "Old Growth Pine Taiga",     "OLD_GROWTH_PINE_TAIGA",     "taiga"),
    ("old_growth_spruce_taiga",  "Old Growth Spruce Taiga",   "OLD_GROWTH_SPRUCE_TAIGA",   "taiga"),
    ("snowy_taiga",              "Snowy Taiga",               "SNOWY_TAIGA",               "taiga"),
    ("snowy_plains",             "Snowy Plains",              "SNOWY_PLAINS",              "icy"),
    ("snowy_slopes",             "Snowy Slopes",              "SNOWY_SLOPES",              "icy"),
    ("frozen_peaks",             "Frozen Peaks",              "FROZEN_PEAKS",              "icy"),
    ("ice_spikes",               "Ice Spikes",                "ICE_SPIKES",                "icy"),
    ("grove",                    "Grove",                     "GROVE",                     "icy"),
    ("meadow",                   "Meadow",                    "MEADOW",                    "plains"),
    ("cherry_grove",             "Cherry Grove",              "CHERRY_GROVE",              "forest"),
    ("jagged_peaks",             "Jagged Peaks",              "JAGGED_PEAKS",              "extreme_hills"),
    ("stony_peaks",              "Stony Peaks",               "STONY_PEAKS",               "extreme_hills"),
    ("stony_shore",              "Stony Shore",               "STONY_SHORE",               "beach"),
    ("savanna",                  "Savanna",                   "SAVANNA",                   "savanna"),
    ("savanna_plateau",          "Savanna Plateau",           "SAVANNA_PLATEAU",           "savanna"),
    ("windswept_savanna",        "Windswept Savanna",         "WINDSWEPT_SAVANNA",         "savanna"),
    ("desert",                   "Desert",                    "DESERT",                    "desert"),
    ("badlands",                 "Badlands",                  "BADLANDS",                  "mesa"),
    ("eroded_badlands",          "Eroded Badlands",           "ERODED_BADLANDS",           "mesa"),
    ("wooded_badlands",          "Wooded Badlands",           "WOODED_BADLANDS",           "mesa"),
    ("jungle",                   "Jungle",                    "JUNGLE",                    "jungle"),
    ("sparse_jungle",            "Sparse Jungle",             "SPARSE_JUNGLE",             "jungle"),
    ("bamboo_jungle",            "Bamboo Jungle",             "BAMBOO_JUNGLE",             "jungle"),
    ("swamp",                    "Swamp",                     "SWAMP",                     "swamp"),
    ("mangrove_swamp",           "Mangrove Swamp",            "MANGROVE_SWAMP",            "swamp"),
    ("mushroom_fields",          "Mushroom Fields",           "MUSHROOM_FIELDS",           "mushroom"),
]

out_dir = r'C:\Users\lxgol\IdeaProjects\LeafsServerBaseline\iris\pack-base\biomes\vanilla'
os.makedirs(out_dir, exist_ok=True)

for slug, name, deriv, cat in biomes:
    content = '{\n  "name": "vanilla/' + slug + '",\n  "rarity": 1,\n  "derivative": "' + deriv + '",\n  "vanillaDerivative": "' + deriv + '",\n  "category": "' + cat + '",\n  "generators": [{"min": 4, "max": 10, "generator": "plain"}]\n}\n'
    path = os.path.join(out_dir, slug + '.json')
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(content)
    # Verify no BOM
    with open(path, 'rb') as f:
        b = f.read(3)
    if b[0] == 0xEF:
        print(f'BOM detected on {slug}.json - stripping')
        with open(path, 'rb') as f:
            data = f.read()
        with open(path, 'wb') as f:
            f.write(data[3:])

print(f'Done: {len(biomes)} files written to {out_dir}')
