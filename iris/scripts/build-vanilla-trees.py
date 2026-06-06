#!/usr/bin/env python3
"""Generate small vanilla-style tree variants and wire them into vanilla biomes.

What this does (repo-controlled, reproducible):
  1. Uses the tree-gen pipeline (iris/tree-gen) to generate N small tree
     variants per vanilla species into iris/output/vanilla-<species>/.
     Those .iob files are staged by deploy-iris-pack.ps1 into
     objects/trees/vanilla-<species>/ (default folder mapping).
  2. Writes overlay biome overrides into iris/pack-overlay/biomes/vanilla/<biome>.json
     (a full copy of the base vanilla biome + an "objects" tree-placement block),
     so the vanilla biomes actually grow trees roughly equivalent to MC.

Run:  python iris/scripts/build-vanilla-trees.py
Then: powershell iris/scripts/deploy-iris-pack.ps1
"""

import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
IRIS = os.path.dirname(HERE)
TREEGEN = os.path.join(IRIS, "tree-gen")
OUTPUT = os.path.join(IRIS, "output")
BASE_VANILLA = os.path.join(IRIS, "pack-base", "biomes", "vanilla")
OVERLAY_VANILLA = os.path.join(IRIS, "pack-overlay", "biomes", "vanilla")

sys.path.insert(0, TREEGEN)
from nbt import write_iob          # noqa: E402
from generate_tree import generate_tree  # noqa: E402

VARIANTS = 8  # variants per species/size bucket (variation is cheap)

# Trunk-height buckets (see plan doc): S/M/L/XL.
SIZES = {"S": (4, 6), "M": (6, 9), "L": (8, 12), "XL": (11, 16)}

# Per-species generation parameters.
# profile must be one of: oak, birch, spruce, jungle, acacia, dark_oak, cherry
SPECIES = {
    "oak":      {"profile": "oak",      "trunk": "minecraft:oak_log",      "leaves": "minecraft:oak_leaves",      "canopy": {"squish": 0.85, "mode": "density", "leaf_density": 0.9}},
    "birch":    {"profile": "birch",    "trunk": "minecraft:birch_log",    "leaves": "minecraft:birch_leaves",    "canopy": {"squish": 0.95, "mode": "density", "leaf_density": 0.88}},
    "spruce":   {"profile": "spruce",   "trunk": "minecraft:spruce_log",   "leaves": "minecraft:spruce_leaves",   "canopy": {"squish": 1.0,  "mode": "density", "leaf_density": 0.92}},
    # 2-wide (2x2) spruce: occasional tall mega-pine for taiga (Part 2). Same
    # spruce profile, but trunk_width 2 so it reads as a thick old pine.
    "spruce2w": {"profile": "spruce",   "trunk": "minecraft:spruce_log",   "leaves": "minecraft:spruce_leaves",   "trunk_width": 2, "canopy": {"squish": 1.0,  "mode": "density", "leaf_density": 0.92}},
    "jungle":   {"profile": "jungle",   "trunk": "minecraft:jungle_log",   "leaves": "minecraft:jungle_leaves",   "canopy": {"squish": 0.9,  "mode": "density", "leaf_density": 0.85}},
    "acacia":   {"profile": "acacia",   "trunk": "minecraft:acacia_log",   "leaves": "minecraft:acacia_leaves",   "canopy": {"squish": 0.55, "mode": "density", "leaf_density": 0.85}},
    "dark_oak": {"profile": "dark_oak", "trunk": "minecraft:dark_oak_log", "leaves": "minecraft:dark_oak_leaves", "trunk_width": 2, "canopy": {"squish": 0.8,  "mode": "density", "leaf_density": 0.92}},
    "cherry":   {"profile": "cherry",   "trunk": "minecraft:cherry_log",   "leaves": "minecraft:cherry_leaves",   "canopy": {"squish": 0.85, "mode": "density", "leaf_density": 0.9}},
    "mangrove": {"profile": "oak",      "trunk": "minecraft:mangrove_log", "leaves": "minecraft:mangrove_leaves", "canopy": {"squish": 0.9,  "mode": "density", "leaf_density": 0.88}},
    # Distinct tree *forms* (not just size): an `extra` dict is merged verbatim
    # into the generate_tree entry, so a species can carry trunk-shape / lean /
    # spiral / branch params. This lets a biome variant ship a recognizably
    # different tree (e.g. a spiral-trunk cherry) rather than only a size reskin.
    # spiral cherry: gently spiralling, leaning trunk with a drooping branched crown.
    "cherry_spiral": {
        "profile": "cherry", "trunk": "minecraft:cherry_log", "leaves": "minecraft:cherry_leaves",
        "canopy": {
            "start_angle": 90, "squish": 0.7, "mode": "density", "leaf_density": 0.9,
            "branches": {
                "prob_fn": "top_heavy", "prob_params": {"exponent": 2.5},
                "length_fn": "linear", "length_params": {"base": 4, "crown": 11},
                "azimuth": "random", "elevation": 12, "leaf_start_up": True,
                "cluster_radius": 4, "cluster_mode": "density", "cluster_density": 0.88,
                "sub_branches": {
                    "count": 3, "pitch_delta": -22, "yaw_delta": 54, "length_scale": 0.55,
                    "cluster_radius": 3, "cluster_mode": "density", "cluster_density": 0.84,
                },
            },
        },
        "extra": {
            "trunk_width": 2,
            "trunk_shape": "parabolic", "trunk_shape_params": {"peak_offset": 0.3},
            "lean_angle": 14, "lean_azimuth": 0,
            "lean_azimuth_fn": "spiral", "lean_azimuth_params": {"turns": 1.5},
            "trunk_curve_fn": "parabolic", "trunk_curve_params": {},
        },
    },
}


def gen_species():
    """Generate VARIANTS .iob per (species, size). Returns {(species,size): [ref,...]}."""
    refs = {}
    for sp, cfg in SPECIES.items():
        folder = "vanilla-%s" % sp
        out_dir = os.path.join(OUTPUT, folder)
        os.makedirs(out_dir, exist_ok=True)
        total = 0
        for size, (h_min, h_max) in SIZES.items():
            sp_refs = []
            for i in range(1, VARIANTS + 1):
                seed = 9000 + abs(hash(sp + size)) % 1000 + i
                if VARIANTS > 1:
                    height = int(round(h_min + (h_max - h_min) * (i - 1) / (VARIANTS - 1)))
                else:
                    height = h_min
                entry = {
                    "name": sp,
                    "trunk": cfg["trunk"],
                    "leaves": cfg["leaves"],
                    "profile": cfg["profile"],
                    "seed": seed,
                    "trunk_width": cfg.get("trunk_width", 1),
                    "trunk_shape": "constant",
                    "canopy": dict(cfg["canopy"]),
                }
                # Merge any species-specific form params (trunk shape, lean,
                # spiral, curve) so non-size tree variants are possible.
                entry.update(cfg.get("extra", {}))
                blocks = generate_tree(entry, height)
                fname = "%s%s%d.iob" % (sp, size, i)
                write_iob(os.path.join(out_dir, fname), blocks)
                sp_refs.append("trees/%s/%s%s%d" % (folder, sp, size, i))
            refs[(sp, size)] = sp_refs
            total += len(sp_refs)
        print("  generated %2d %-9s -> objects/%s" % (total, sp, folder))
    return refs


# Base-pack-provided huge mushroom objects (always shipped by the downloaded
# Iris overworld pack under objects/trees/mushroom/). Referenced, not generated.
MUSHROOM_REFS = (
    ["trees/mushroom/redgeneric%d" % i for i in range(1, 9)]
    + ["trees/mushroom/browngeneric%d" % i for i in range(1, 3)]
    + ["trees/mushroom/smolshroom%d" % i for i in range(1, 6)]
)

# biome file (without .json) -> list of tree groups (species_or_'mushroom', size, chance, density)
BIOME_TREES = {
    "forest":                   [("oak", "S", 0.10, 3), ("oak", "M", 0.20, 5), ("birch", "M", 0.10, 3)],
    "flower_forest":            [("oak", "M", 0.12, 3), ("birch", "M", 0.06, 2)],
    "birch_forest":             [("birch", "S", 0.10, 3), ("birch", "M", 0.22, 5)],
    "old_growth_birch_forest":  [("birch", "XL", 0.32, 6)],
    "dark_forest":              [("dark_oak", "M", 0.18, 4), ("dark_oak", "L", 0.16, 4), ("oak", "M", 0.08, 2), ("birch", "M", 0.04, 2), ("mushroom", None, 0.03, 1)],
    "cherry_grove":             [("cherry", "M", 0.16, 4), ("cherry", "L", 0.12, 3)],
    "taiga":                    [("spruce", "S", 0.20, 5), ("spruce", "M", 0.12, 3), ("spruce2w", "M", 0.04, 2)],
    "snowy_taiga":              [("spruce", "S", 0.18, 5), ("spruce", "M", 0.10, 3)],
    "grove":                    [("spruce", "S", 0.16, 4), ("spruce", "M", 0.08, 2)],
    "old_growth_pine_taiga":    [("spruce", "XL", 0.34, 6), ("mushroom", None, 0.02, 1)],
    "old_growth_spruce_taiga":  [("spruce", "XL", 0.34, 6), ("mushroom", None, 0.02, 1)],
    "jungle":                   [("jungle", "L", 0.36, 6), ("jungle", "XL", 0.10, 2), ("oak", "M", 0.06, 2)],
    "bamboo_jungle":            [("jungle", "L", 0.26, 5)],
    "sparse_jungle":            [("jungle", "M", 0.14, 3)],
    "savanna":                  [("acacia", "M", 0.05, 3), ("oak", "S", 0.01, 1)],
    "savanna_plateau":          [("acacia", "M", 0.05, 3), ("oak", "S", 0.01, 1)],
    "windswept_savanna":        [("acacia", "S", 0.03, 2)],
    "swamp":                    [("oak", "S", 0.06, 2), ("oak", "M", 0.12, 3), ("mushroom", None, 0.01, 1)],
    "mangrove_swamp":           [("mangrove", "M", 0.22, 5)],
    "plains":                   [("oak", "S", 0.02, 1)],
    "sunflower_plains":         [("oak", "S", 0.02, 1)],
    "meadow":                   [("oak", "S", 0.015, 1)],
    "windswept_forest":         [("spruce", "S", 0.08, 2), ("spruce", "M", 0.10, 3), ("oak", "M", 0.10, 3)],
    "windswept_hills":          [("oak", "M", 0.12, 4), ("spruce", "M", 0.08, 2)],
    "windswept_gravelly_hills": [("spruce", "S", 0.02, 2), ("oak", "S", 0.02, 1)],
    "wooded_badlands":          [("oak", "S", 0.05, 2)],
    "mushroom_fields":          [("mushroom", None, 0.12, 3)],
    # Dry biomes: no trees in vanilla (cactus/dead_bush handled as decorators).
}

# Reusable flora palettes (block-id, weight) for decorators. Single-block plants
# AND vanilla double-plants (tall_grass, large_fern, lilac, rose_bush, peony,
# sunflower) are placed directly by Iris's decorator (it handles the upper half).
GRASS = [("short_grass", 100), ("tall_grass", 18), ("fern", 8)]
GRASS_SPARSE = [("short_grass", 100), ("fern", 6)]
FERNY = [("fern", 60), ("short_grass", 100), ("large_fern", 18)]
SMALL_FLOWERS = [
    ("dandelion", 10), ("poppy", 10), ("azure_bluet", 6), ("oxeye_daisy", 6),
    ("cornflower", 6), ("red_tulip", 4), ("orange_tulip", 4),
    ("white_tulip", 4), ("pink_tulip", 4),
]
FLOWER_FOREST_FLOWERS = SMALL_FLOWERS + [
    ("allium", 6), ("lily_of_the_valley", 6),
    ("lilac", 2), ("rose_bush", 2), ("peony", 2),
]

# biome file (without .json) -> list of decorator specs (chance, palette, stackMin, stackMax)
BIOME_DECORATORS = {
    "forest":                   [(0.30, GRASS, 1, 1), (0.06, [("dandelion", 1), ("poppy", 1)], 1, 1)],
    "flower_forest":            [(0.35, GRASS, 1, 1), (0.45, FLOWER_FOREST_FLOWERS, 1, 1)],
    "birch_forest":             [(0.30, GRASS, 1, 1), (0.05, [("lily_of_the_valley", 6), ("dandelion", 4), ("poppy", 4)], 1, 1)],
    "old_growth_birch_forest":  [(0.30, GRASS, 1, 1), (0.05, [("lily_of_the_valley", 6), ("dandelion", 4), ("poppy", 4)], 1, 1)],
    "dark_forest":              [(0.25, GRASS, 1, 1), (0.05, [("lily_of_the_valley", 4), ("rose_bush", 3), ("peony", 3), ("lilac", 3)], 1, 1), (0.02, [("red_mushroom", 1), ("brown_mushroom", 1)], 1, 1)],
    "cherry_grove":             [(0.30, [("pink_petals", 30), ("short_grass", 100), ("tall_grass", 14)], 1, 1), (0.05, [("dandelion", 1)], 1, 1)],
    "taiga":                    [(0.30, FERNY, 1, 1)],
    "snowy_taiga":              [(0.30, GRASS_SPARSE, 1, 1)],
    "grove":                    [(0.20, GRASS_SPARSE, 1, 1)],
    "old_growth_pine_taiga":    [(0.32, FERNY, 1, 1)],
    "old_growth_spruce_taiga":  [(0.32, FERNY, 1, 1)],
    "jungle":                   [(0.40, [("short_grass", 100), ("tall_grass", 20), ("fern", 12)], 1, 1), (0.02, [("melon", 1)], 1, 1)],
    "bamboo_jungle":            [(0.35, [("short_grass", 100), ("fern", 12)], 1, 1), (0.20, [("bamboo", 1)], 2, 12)],
    "sparse_jungle":            [(0.30, [("short_grass", 100), ("fern", 10)], 1, 1)],
    "savanna":                  [(0.65, [("short_grass", 100), ("tall_grass", 30)], 1, 1)],
    "savanna_plateau":          [(0.65, [("short_grass", 100), ("tall_grass", 30)], 1, 1)],
    "windswept_savanna":        [(0.40, [("short_grass", 100), ("tall_grass", 14)], 1, 1)],
    "swamp":                    [(0.20, [("short_grass", 100)], 1, 1), (0.04, [("blue_orchid", 1)], 1, 1), (0.02, [("red_mushroom", 1), ("brown_mushroom", 1)], 1, 1), (0.01, [("dead_bush", 1)], 1, 1)],
    "mangrove_swamp":           [(0.25, [("short_grass", 100)], 1, 1), (0.03, [("mangrove_propagule", 1)], 1, 1)],
    "plains":                   [(0.55, [("short_grass", 100), ("tall_grass", 22)], 1, 1), (0.06, SMALL_FLOWERS, 1, 1)],
    "sunflower_plains":         [(0.45, [("short_grass", 100), ("tall_grass", 18)], 1, 1), (0.18, [("sunflower", 1)], 1, 1), (0.05, [("dandelion", 1), ("poppy", 1)], 1, 1)],
    "meadow":                   [(0.55, [("short_grass", 100), ("tall_grass", 22)], 1, 1), (0.10, SMALL_FLOWERS + [("allium", 6)], 1, 1)],
    "windswept_forest":         [(0.30, FERNY, 1, 1)],
    "windswept_hills":          [(0.30, FERNY, 1, 1)],
    "windswept_gravelly_hills": [(0.15, GRASS_SPARSE, 1, 1)],
    "wooded_badlands":          [(0.10, GRASS_SPARSE, 1, 1), (0.03, [("dead_bush", 1)], 1, 1)],
    "mushroom_fields":          [(0.05, [("red_mushroom", 1), ("brown_mushroom", 1)], 1, 1)],
    # Dry biomes (vanilla-accurate: cactus 1-3 tall + dead_bush, no grass/trees).
    "desert":                   [(0.04, [("dead_bush", 1)], 1, 1), (0.025, [("cactus", 1)], 1, 3)],
    "badlands":                 [(0.05, [("dead_bush", 1)], 1, 1), (0.01, [("cactus", 1)], 1, 2)],
    "eroded_badlands":          [(0.05, [("dead_bush", 1)], 1, 1), (0.01, [("cactus", 1)], 1, 2)],
}


# Part 1 variant biomes (see docs/scratch/vanilla-biome-variants-plan.md): sibling
# "reskins" of a parent vanilla biome that only shift the tree size mix. Each variant
# is listed once per region with a per-biome `rarity` (1 = co-listed, 4 = rare,
# 9 = very rare) and reuses the parent's decorators (flora).
# name -> (parent biome, rarity, [(species, size, chance, density), ...])
BIOME_VARIANTS = {
    "forest__young":           ("forest",           1, [("oak", "S", 0.22, 5), ("oak", "M", 0.10, 3), ("birch", "S", 0.06, 2)]),
    "forest__tall":            ("forest",           4, [("oak", "M", 0.14, 4), ("oak", "L", 0.10, 3), ("birch", "M", 0.06, 2)]),
    "forest__giant":           ("forest",           9, [("oak", "M", 0.06, 2), ("oak", "L", 0.10, 3), ("oak", "XL", 0.08, 2)]),
    "birch_forest__young":     ("birch_forest",     1, [("birch", "S", 0.20, 5), ("birch", "M", 0.10, 3)]),
    "birch_forest__tall":      ("birch_forest",     4, [("birch", "M", 0.16, 4), ("birch", "L", 0.12, 3)]),
    "dark_forest__dense":      ("dark_forest",      1, [("dark_oak", "S", 0.06, 2), ("dark_oak", "M", 0.18, 4), ("dark_oak", "L", 0.10, 3), ("oak", "M", 0.06, 2), ("birch", "M", 0.04, 2)]),
    "dark_forest__giant":      ("dark_forest",      9, [("dark_oak", "M", 0.08, 2), ("dark_oak", "L", 0.14, 3), ("dark_oak", "XL", 0.10, 2)]),
    "flower_forest__young":    ("flower_forest",    1, [("oak", "S", 0.16, 4), ("oak", "M", 0.08, 2), ("birch", "S", 0.05, 2)]),
    "taiga__tall":             ("taiga",            4, [("spruce", "M", 0.14, 4), ("spruce", "L", 0.16, 4)]),
    "taiga__giant":            ("taiga",            9, [("spruce", "M", 0.06, 2), ("spruce", "L", 0.12, 3), ("spruce", "XL", 0.10, 2)]),
    "snowy_taiga__young":      ("snowy_taiga",      1, [("spruce", "S", 0.18, 5), ("spruce", "M", 0.08, 2)]),
    "jungle__sparse":          ("jungle",           4, [("jungle", "S", 0.10, 3), ("jungle", "M", 0.12, 3), ("oak", "M", 0.04, 2)]),
    "jungle__giant":           ("jungle",           9, [("jungle", "M", 0.10, 3), ("jungle", "L", 0.16, 4), ("jungle", "XL", 0.12, 3), ("oak", "M", 0.06, 2)]),
    "cherry_grove__young":     ("cherry_grove",     1, [("cherry", "S", 0.16, 4), ("cherry", "M", 0.08, 2)]),
    "cherry_grove__tall":      ("cherry_grove",     4, [("cherry", "M", 0.12, 3), ("cherry", "L", 0.14, 3)]),
    "cherry_grove__spiral":    ("cherry_grove",     4, [("cherry_spiral", "L", 0.14, 3), ("cherry_spiral", "XL", 0.08, 2), ("cherry", "M", 0.06, 2)]),
    "savanna__sparse":         ("savanna",          1, [("acacia", "S", 0.03, 2), ("acacia", "M", 0.03, 2), ("oak", "S", 0.02, 1)]),
    "savanna__tall":           ("savanna",          4, [("acacia", "M", 0.05, 2), ("acacia", "L", 0.06, 2)]),
    "swamp__giant":            ("swamp",            9, [("oak", "M", 0.06, 2), ("oak", "L", 0.10, 3), ("oak", "XL", 0.06, 2)]),
    "windswept_forest__young": ("windswept_forest", 1, [("spruce", "S", 0.10, 3), ("spruce", "M", 0.06, 2), ("oak", "S", 0.06, 2)]),
    "plains__sparse":          ("plains",           1, [("oak", "S", 0.015, 1)]),
    "meadow__giant":           ("meadow",           9, [("oak", "M", 0.02, 1), ("oak", "L", 0.02, 1)]),
}


def _object_group(place, chance, density):
    return {
        "chance": chance,
        "density": density,
        "rotation": {
            "yAxis": {"min": 0, "max": 270, "interval": 90, "enabled": True},
            "enabled": True,
        },
        "place": place,
    }


def _decorator(chance, palette, stack_min=1, stack_max=1):
    pal = []
    for blk, weight in palette:
        entry = {"block": "minecraft:" + blk}
        if weight != 1:
            entry["weight"] = weight
        pal.append(entry)
    dec = {"chance": chance, "style": {"style": "STATIC"}, "palette": pal}
    if stack_max > 1:
        dec["stackMin"] = stack_min
        dec["stackMax"] = stack_max
        dec["scaleStack"] = True
    return dec


def write_overlays(refs):
    os.makedirs(OVERLAY_VANILLA, exist_ok=True)
    written = 0
    biomes = set(BIOME_TREES) | set(BIOME_DECORATORS)
    for biome in sorted(biomes):
        base_path = os.path.join(BASE_VANILLA, biome + ".json")
        if not os.path.exists(base_path):
            print("  SKIP missing base biome:", biome)
            continue
        with open(base_path, "r", encoding="utf-8") as f:
            data = json.load(f)
        objects = []
        for sp, size, chance, density in BIOME_TREES.get(biome, []):
            if sp == "mushroom":
                place = MUSHROOM_REFS
            else:
                place = refs.get((sp, size))
            if not place:
                continue
            objects.append(_object_group(list(place), chance, density))
        if objects:
            data["objects"] = objects
        decorators = [_decorator(*spec) for spec in BIOME_DECORATORS.get(biome, [])]
        if decorators:
            data["decorators"] = decorators
        out_path = os.path.join(OVERLAY_VANILLA, biome + ".json")
        with open(out_path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2)
            f.write("\n")
        written += 1
        print("  overlay biome %-26s (%d trees, %d decorators)" % (biome, len(objects), len(decorators)))
    print("  wrote %d overlay vanilla biome files" % written)


def write_variants(refs):
    """Write Part 1 variant biomes: a minimal base file (name + rarity inherited
    from the parent) plus a full overlay with the variant tree mix and the
    parent's decorators."""
    os.makedirs(BASE_VANILLA, exist_ok=True)
    os.makedirs(OVERLAY_VANILLA, exist_ok=True)
    written = 0
    for variant in sorted(BIOME_VARIANTS):
        parent, rarity, groups = BIOME_VARIANTS[variant]
        parent_base = os.path.join(BASE_VANILLA, parent + ".json")
        if not os.path.exists(parent_base):
            print("  SKIP variant (missing parent base):", variant)
            continue
        with open(parent_base, "r", encoding="utf-8") as f:
            base = json.load(f)
        base["name"] = "vanilla/" + variant
        base["rarity"] = rarity
        with open(os.path.join(BASE_VANILLA, variant + ".json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(base, f, indent=2)
            f.write("\n")
        data = dict(base)
        objects = []
        for sp, size, chance, density in groups:
            if sp == "mushroom":
                place = MUSHROOM_REFS
            else:
                place = refs.get((sp, size))
            if not place:
                continue
            objects.append(_object_group(list(place), chance, density))
        if objects:
            data["objects"] = objects
        decorators = [_decorator(*spec) for spec in BIOME_DECORATORS.get(parent, [])]
        if decorators:
            data["decorators"] = decorators
        out_path = os.path.join(OVERLAY_VANILLA, variant + ".json")
        with open(out_path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2)
            f.write("\n")
        written += 1
        print("  variant biome %-26s parent=%-18s rarity=%d (%d trees)" % (variant, parent, rarity, len(objects)))
    print("  wrote %d variant biome files" % written)


def main():
    print("=== Generating vanilla tree variants ===")
    refs = gen_species()
    print("=== Writing vanilla biome overlays ===")
    write_overlays(refs)
    print("=== Writing vanilla biome variants ===")
    write_variants(refs)
    print("Done. Next: run iris/scripts/deploy-iris-pack.ps1")


if __name__ == "__main__":
    main()
