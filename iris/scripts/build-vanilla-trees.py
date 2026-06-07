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
    # Vanilla dark oak: flat, wide umbrella crown (not a sphere). Uses the
    # dedicated "dark_oak_flat" canopy profile + a disc-like start_angle and low
    # squish so the top reads as a shallow slab.
    "dark_oak": {"profile": "dark_oak_flat", "trunk": "minecraft:dark_oak_log", "leaves": "minecraft:dark_oak_leaves", "trunk_width": 2, "canopy": {"start_angle": 150, "squish": 0.28, "mode": "density", "leaf_density": 0.95}},
    # Giant roofed-forest dark oak: same flat umbrella but on the extra-wide
    # canopy profile so the crowns are broad enough to overlap into a continuous
    # closed roof in the dark_forest__giant variant.
    "dark_oak_wide": {"profile": "dark_oak_flat_wide", "trunk": "minecraft:dark_oak_log", "leaves": "minecraft:dark_oak_leaves", "trunk_width": 2, "canopy": {"start_angle": 150, "squish": 0.28, "mode": "density", "leaf_density": 0.97}},
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

# Pre-generated, hand-authored tree objects that are NOT produced by gen_species()
# (they come from iris/tree-gen/configs/*.json run through tree-gen and live in their
# own iris/output/<config>/ folder). The deploy script's $iobFolderMap controls
# which objects/trees/<dest> they stage into; spiral-crown-forest -> trees/darkoak.
# Reference these from BIOME_VARIANTS groups via a "custom:<key>" species, e.g.
# ("custom:spiral_crown_large", None, 0.20, 4). The size field is ignored.
CUSTOM_OBJECT_REFS = {
    "spiral_crown_large":   ["trees/darkoak/spiral_large_%d" % i for i in range(1, 6)],
    "spiral_crown_titan":   ["trees/darkoak/titan_large_%d" % i for i in range(1, 6)],
    "spiral_crown_twisted": ["trees/darkoak/twisted_small_%d" % i for i in range(1, 6)],
    # Fantasy configs (iris/tree-gen/configs/) generated to iris/output/<config>/ with stable filenames.
    # These output folders are NOT remapped in the deploy $iobFolderMap, so they
    # stage to objects/trees/<config>/ (folder name = config name) by default.
    "glacierpine_small":    ["trees/glacierpine/glacierpine_small_%d" % i for i in range(1, 4)],
    "glacierpine_medium":   ["trees/glacierpine/glacierpine_medium_%d" % i for i in range(1, 4)],
    "glacierpine_large":    ["trees/glacierpine/glacierpine_large_%d" % i for i in range(1, 5)],
    "blightroot":           ["trees/blightroot/blightroot_%d" % i for i in range(1, 5)],
    "glowcap_small":        ["trees/glowcap/glowcap_small_%d" % i for i in range(1, 5)],
    "glowcap_large":        ["trees/glowcap/glowcap_large_%d" % i for i in range(1, 5)],
    "embervine_small":      ["trees/embervine/embervine_small_%d" % i for i in range(1, 4)],
    "embervine_medium":     ["trees/embervine/embervine_medium_%d" % i for i in range(1, 4)],
    "embervine_large":      ["trees/embervine/embervine_large_%d" % i for i in range(1, 5)],
    "cloudcap_small":       ["trees/cloudcap/cloudcap_small_%d" % i for i in range(1, 4)],
    "cloudcap_large":       ["trees/cloudcap/cloudcap_large_%d" % i for i in range(1, 4)],
    "ashwood_small":        ["trees/ashwood/ashwood_small_%d" % i for i in range(1, 4)],
    "ashwood_medium":       ["trees/ashwood/ashwood_medium_%d" % i for i in range(1, 4)],
    "ashwood_large":        ["trees/ashwood/ashwood_large_%d" % i for i in range(1, 4)],
}


def resolve_place(refs, sp, size):
    """Map a (species, size) group to a list of object refs.
    Handles the special "mushroom" species, "custom:<key>" pre-built objects, and
    normal generated species buckets."""
    if sp == "mushroom":
        return MUSHROOM_REFS
    if sp.startswith("custom:"):
        return CUSTOM_OBJECT_REFS.get(sp[len("custom:"):])
    return refs.get((sp, size))

# biome file (without .json) -> list of tree groups (species_or_'mushroom', size, chance, density)
BIOME_TREES = {
    "forest":                   [("oak", "S", 0.30, 4), ("oak", "M", 0.48, 5), ("birch", "M", 0.20, 3)],
    # Flower forest reads as a real (moderately dense) forest, not a flower field.
    "flower_forest":            [("oak", "S", 0.28, 4), ("oak", "M", 0.44, 5), ("birch", "M", 0.18, 3)],
    "birch_forest":             [("birch", "S", 0.28, 4), ("birch", "M", 0.52, 5)],
    "old_growth_birch_forest":  [("birch", "XL", 0.55, 6)],
    # Classic dark forest: dense stand of SHORT 2-wide (2x2) flat-top dark oak.
    # dark_oak is trunk_width 2 (no 1-wide dark oak), with sparse oak/birch accents.
    "dark_forest":              [("dark_oak", "S", 0.85, 5), ("dark_oak", "M", 0.90, 6), ("oak", "M", 0.08, 2), ("birch", "M", 0.06, 2), ("mushroom", None, 0.14, 2)],
    "cherry_grove":             [("cherry", "M", 0.30, 4), ("cherry", "L", 0.22, 3)],
    "taiga":                    [("spruce", "S", 0.36, 5), ("spruce", "M", 0.22, 3), ("spruce2w", "M", 0.07, 2)],
    "snowy_taiga":              [("spruce", "S", 0.34, 5), ("spruce", "M", 0.18, 3)],
    # Snowy slopes / snowy plains: vanilla leaves these (near) treeless, but a few
    # scattered spruce read better than bare snow and cover the gap (see issue).
    "snowy_slopes":             [("spruce", "S", 0.08, 2), ("spruce", "M", 0.05, 2)],
    "snowy_plains":             [("spruce", "S", 0.02, 1)],
    "grove":                    [("spruce", "S", 0.30, 4), ("spruce", "M", 0.16, 2)],
    "old_growth_pine_taiga":    [("spruce", "XL", 0.52, 6), ("mushroom", None, 0.03, 1)],
    "old_growth_spruce_taiga":  [("spruce", "XL", 0.52, 6), ("mushroom", None, 0.03, 1)],
    "jungle":                   [("jungle", "L", 0.52, 6), ("jungle", "XL", 0.16, 2), ("oak", "M", 0.10, 2)],
    "bamboo_jungle":            [("jungle", "L", 0.40, 5)],
    "sparse_jungle":            [("jungle", "M", 0.22, 3)],
    "savanna":                  [("acacia", "M", 0.05, 3), ("oak", "S", 0.01, 1)],
    "savanna_plateau":          [("acacia", "M", 0.05, 3), ("oak", "S", 0.01, 1)],
    "windswept_savanna":        [("acacia", "S", 0.03, 2)],
    "swamp":                    [("oak", "S", 0.12, 2), ("oak", "M", 0.22, 3), ("mushroom", None, 0.02, 1)],
    "mangrove_swamp":           [("mangrove", "M", 0.36, 5)],
    "plains":                   [("oak", "S", 0.02, 1)],
    "sunflower_plains":         [("oak", "S", 0.02, 1)],
    "meadow":                   [("oak", "S", 0.015, 1)],
    "windswept_forest":         [("spruce", "S", 0.16, 2), ("spruce", "M", 0.18, 3), ("oak", "M", 0.18, 3)],
    "windswept_hills":          [("oak", "M", 0.20, 4), ("spruce", "M", 0.14, 2)],
    "windswept_gravelly_hills": [("spruce", "S", 0.02, 2), ("oak", "S", 0.02, 1)],
    "wooded_badlands":          [("oak", "S", 0.12, 2), ("oak", "M", 0.06, 2)],
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
# Flower-forest flora: tulip-heavy small flowers, only a touch of 2-tall plants
# (lilac/rose_bush/peony at weight 1) so the ground reads as tulips/small flowers
# rather than a sea of 2-tall blocks.
FLOWER_FOREST_FLOWERS = [
    ("red_tulip", 12), ("orange_tulip", 12), ("white_tulip", 12), ("pink_tulip", 12),
    ("dandelion", 8), ("poppy", 8), ("azure_bluet", 6), ("oxeye_daisy", 6),
    ("cornflower", 6), ("allium", 6), ("lily_of_the_valley", 6),
    ("lilac", 1), ("rose_bush", 1), ("peony", 1),
]

# biome file (without .json) -> list of decorator specs (chance, palette, stackMin, stackMax)
BIOME_DECORATORS = {
    "forest":                   [(0.30, GRASS, 1, 1), (0.06, [("dandelion", 1), ("poppy", 1)], 1, 1)],
    "flower_forest":            [(0.20, GRASS_SPARSE, 1, 1), (0.55, FLOWER_FOREST_FLOWERS, 1, 1)],
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


# Per-biome terrain height overrides (generator min/max relative to sea level y75;
# negative = below sea level / underwater). Applied to the overlay (and inherited
# by variants of the same parent). Lets a biome read with the right relief without
# editing pack-base.
BIOME_GENERATORS = {
    # Flower forest rolls like short hills/ridges (vanilla generates it hilly),
    # not a flat field.
    "flower_forest":  [{"min": 6, "max": 28, "generator": "mountain"}],
    # Mangrove swamp sits low so roughly half of it floods with water (shallow
    # wetland), instead of standing above sea level and reading dry.
    "mangrove_swamp": [{"min": -7, "max": 3, "generator": "mountain"}],

    # --- Part 11: terrain relief for biomes that read wrong as flats ---
    # Values are relative to sea level (y75); calibrated against native Iris
    # biomes (mountains ~66/89, mesa highplains 50/70, plateaus small-cliffs
    # 25/37, hills rare-hills 0/40, ice spikes highplains 40/50).

    # Peaks: tall, dramatic mountain relief (currently flat = very wrong).
    "jagged_peaks":             [{"min": 60, "max": 110, "generator": "mountain-aggro"}],
    "frozen_peaks":             [{"min": 65, "max": 105, "generator": "mountain-large"}],
    "stony_peaks":              [{"min": 55, "max": 95,  "generator": "mountain-aggro"}],
    "snowy_slopes":             [{"min": 30, "max": 70,  "generator": "mountain"}],

    # Mesa / badlands: layered plateaus and carved canyons, not flat.
    "badlands":                 [{"min": 50, "max": 70,  "generator": "highplains"}],
    "eroded_badlands":          [{"min": 70, "max": 98,  "generator": "cracked-cliffs"}],
    "wooded_badlands":          [{"min": 50, "max": 70,  "generator": "highplains"}],

    # Plateaus, dunes, alpine meadow, rocky shore.
    "savanna_plateau":          [{"min": 40, "max": 60,  "generator": "highplains"}],
    "desert":                   [{"min": 8,  "max": 24,  "generator": "smooth-dunes"}],
    "meadow":                   [{"min": 5,  "max": 22,  "generator": "rare-hills"}],
    "stony_shore":              [{"min": 2,  "max": 20,  "generator": "small-cliffs"}],

    # Windswept biomes: wind-carved hills and cliffs.
    "windswept_hills":          [{"min": 15, "max": 45,  "generator": "rare-hills"}],
    "windswept_gravelly_hills": [{"min": 20, "max": 50,  "generator": "plain-cliffs"}],
    "windswept_forest":         [{"min": 12, "max": 35,  "generator": "small-cliffs"}],
    "windswept_savanna":        [{"min": 15, "max": 45,  "generator": "plain-cliffs"}],

    # Ice spikes: a low plain base (spikes themselves are decorators/objects).
    "ice_spikes":               [{"min": 30, "max": 45,  "generator": "highplains"}],

    # Taiga family is gently hilly in vanilla, not table-flat (per user note).
    "taiga":                    [{"min": 5,  "max": 22,  "generator": "rare-hills"}],
    "snowy_taiga":              [{"min": 5,  "max": 22,  "generator": "rare-hills"}],
    "old_growth_pine_taiga":    [{"min": 6,  "max": 26,  "generator": "rare-hills"}],
    "old_growth_spruce_taiga":  [{"min": 6,  "max": 26,  "generator": "rare-hills"}],
    "grove":                    [{"min": 8,  "max": 28,  "generator": "rare-hills"}],
}

# Per-biome surface layer overrides (full "layers" block). Lets a biome ship a
# distinct floor palette (e.g. muddy mangrove ground) from the overlay.
BIOME_LAYERS = {
    # Muddy mangrove floor: mostly mud + muddy roots over a mud/dirt subsurface.
    "mangrove_swamp": [
        {
            "style": {"style": "NOWHERE"},
            "palette": [
                {"block": "minecraft:mud", "weight": 6},
                {"block": "minecraft:muddy_mangrove_roots", "weight": 2},
                {"block": "minecraft:dirt", "weight": 1},
            ],
        },
        {
            "minHeight": 1,
            "maxHeight": 3,
            "palette": [
                {"block": "minecraft:mud", "weight": 2},
                {"block": "minecraft:dirt"},
            ],
        },
        {"palette": [{"block": "minecraft:dirt"}]},
    ],
}


# Part 1 variant biomes (see docs/scratch/vanilla-biome-variants-plan.md): sibling
# "reskins" of a parent vanilla biome that only shift the tree size mix. Each variant
# is listed once per region with a per-biome `rarity` (1 = co-listed, 4 = rare,
# 9 = very rare) and reuses the parent's decorators (flora).
# name -> (parent biome, rarity, [(species, size, chance, density), ...])
#
# RARITY_SCALE harshens the authored rarity tiers so the rarer variants (and the
# hand-authored "iris" fantasy biomes) are much harder to stumble across. The base
# tiers (1 = co-listed common sibling, 4 = rare, 9 = very rare) are mapped to a
# steeper curve: common siblings stay common, while rare/very-rare finds become
# genuinely scarce. Combined with the wider landBiomeZoom this makes special biomes
# feel like a real discovery rather than something you trip over.
RARITY_SCALE = {1: 1, 4: 12, 9: 30}
BIOME_VARIANTS = {
    "forest__young":           ("forest",           1, [("oak", "S", 0.40, 5), ("oak", "M", 0.18, 3), ("birch", "S", 0.10, 2)]),
    "forest__tall":            ("forest",           4, [("oak", "M", 0.26, 4), ("oak", "L", 0.18, 3), ("birch", "M", 0.10, 2)]),
    "forest__giant":           ("forest",           9, [("oak", "M", 0.12, 2), ("oak", "L", 0.18, 3), ("oak", "XL", 0.14, 2)]),
    "birch_forest__young":     ("birch_forest",     1, [("birch", "S", 0.36, 5), ("birch", "M", 0.18, 3)]),
    "birch_forest__tall":      ("birch_forest",     4, [("birch", "M", 0.28, 4), ("birch", "L", 0.22, 3)]),
    "dark_forest__dense":      ("dark_forest",      1, [("dark_oak", "S", 0.92, 5), ("dark_oak", "M", 0.95, 6), ("oak", "M", 0.06, 2), ("birch", "M", 0.05, 2)]),
    "dark_forest__giant":      ("dark_forest",      9, [("dark_oak_wide", "M", 0.70, 6), ("dark_oak_wide", "L", 0.85, 7), ("dark_oak_wide", "XL", 0.55, 5)]),
    "flower_forest__young":    ("flower_forest",    1, [("oak", "S", 0.32, 4), ("oak", "M", 0.16, 2), ("birch", "S", 0.10, 2)]),
    "taiga__tall":             ("taiga",            4, [("spruce", "M", 0.26, 4), ("spruce", "L", 0.28, 4)]),
    "taiga__giant":            ("taiga",            9, [("spruce", "M", 0.12, 2), ("spruce", "L", 0.22, 3), ("spruce", "XL", 0.18, 2)]),
    "snowy_taiga__young":      ("snowy_taiga",      1, [("spruce", "S", 0.36, 5), ("spruce", "M", 0.16, 2)]),
    "jungle__sparse":          ("jungle",           4, [("jungle", "S", 0.18, 3), ("jungle", "M", 0.22, 3), ("oak", "M", 0.07, 2)]),
    "jungle__giant":           ("jungle",           9, [("jungle", "M", 0.18, 3), ("jungle", "L", 0.28, 4), ("jungle", "XL", 0.20, 3), ("oak", "M", 0.10, 2)]),
    "cherry_grove__young":     ("cherry_grove",     1, [("cherry", "S", 0.30, 4), ("cherry", "M", 0.16, 2)]),
    "cherry_grove__tall":      ("cherry_grove",     4, [("cherry", "M", 0.22, 3), ("cherry", "L", 0.26, 3)]),
    # Spiral cherry forest: a stand made entirely of the spiral cherry SPECIES in
    # a full size spread (S/M/L/XL). Unlike the roofed forest (mixed dark-oak
    # forms), every tree here is the same spiral form, just at varying sizes, with
    # the bigger trees progressively rarer (S common -> XL rare).
    "cherry_grove__spiral":    ("cherry_grove",     4, [("cherry_spiral", "S", 0.32, 4), ("cherry_spiral", "M", 0.22, 3), ("cherry_spiral", "L", 0.12, 3), ("cherry_spiral", "XL", 0.05, 2)]),
    "savanna__sparse":         ("savanna",          1, [("acacia", "S", 0.03, 2), ("acacia", "M", 0.03, 2), ("oak", "S", 0.02, 1)]),
    "savanna__tall":           ("savanna",          4, [("acacia", "M", 0.05, 2), ("acacia", "L", 0.06, 2)]),
    "swamp__giant":            ("swamp",            9, [("oak", "M", 0.06, 2), ("oak", "L", 0.10, 3), ("oak", "XL", 0.06, 2)]),
    # Rare in-the-wild find: the hand-authored spiral-crown dark oak forest.
    # Uses pre-built spiral/titan/twisted dark_oak objects (custom: refs above),
    # parent dark_forest, very rare (rarity 9). Wired into forests.json.
    # glowcap (giant glowing mushroom) is this biome's special "small" understory
    # tree, so it is placed here rather than as a standalone variant.
    "dark_forest__spiral":     ("dark_forest",      9, [("custom:spiral_crown_large", None, 0.40, 6), ("custom:spiral_crown_titan", None, 0.14, 3), ("custom:spiral_crown_twisted", None, 0.10, 3), ("custom:glowcap_small", None, 0.05, 2), ("custom:glowcap_large", None, 0.02, 1)]),
    # Rare in-the-wild finds wiring the hand-authored fantasy tree-configs
    # (generated to iris/output/<config>/, referenced via custom: refs above).
    # Frosted pine grove (snow caps, dripstone icicles) - moderately rare.
    "snowy_taiga__glacier":    ("snowy_taiga",      4, [("custom:glacierpine_small", None, 0.16, 4), ("custom:glacierpine_medium", None, 0.10, 3), ("custom:glacierpine_large", None, 0.04, 2)]),
    # Haunted gnarled dark-oak (wither roses baked in) - very rare.
    "dark_forest__blight":     ("dark_forest",      9, [("custom:blightroot", None, 0.22, 4)]),
    # Ember jungle (drooping shroomlight) - very rare.
    "jungle__ember":           ("jungle",           9, [("custom:embervine_small", None, 0.14, 3), ("custom:embervine_medium", None, 0.10, 3), ("custom:embervine_large", None, 0.05, 2)]),
    # Floating azalea cloud-tree meadow - very rare landmark.
    "meadow__cloud":           ("meadow",           9, [("custom:cloudcap_small", None, 0.04, 2), ("custom:cloudcap_large", None, 0.02, 1)]),
    # Dead basalt ashwood stand - very rare badlands accent.
    "badlands__ashwood":       ("badlands",         9, [("custom:ashwood_small", None, 0.05, 2), ("custom:ashwood_medium", None, 0.04, 2), ("custom:ashwood_large", None, 0.02, 1)]),
    "windswept_forest__young": ("windswept_forest", 1, [("spruce", "S", 0.20, 3), ("spruce", "M", 0.12, 2), ("oak", "S", 0.12, 2)]),
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
    biomes = set(BIOME_TREES) | set(BIOME_DECORATORS) | set(BIOME_GENERATORS) | set(BIOME_LAYERS)
    for biome in sorted(biomes):
        base_path = os.path.join(BASE_VANILLA, biome + ".json")
        if not os.path.exists(base_path):
            print("  SKIP missing base biome:", biome)
            continue
        with open(base_path, "r", encoding="utf-8") as f:
            data = json.load(f)
        if biome in BIOME_GENERATORS:
            data["generators"] = BIOME_GENERATORS[biome]
        if biome in BIOME_LAYERS:
            data["layers"] = BIOME_LAYERS[biome]
        objects = []
        for sp, size, chance, density in BIOME_TREES.get(biome, []):
            place = resolve_place(refs, sp, size)
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
    """Write Part 1 variant biomes as a full overlay file (inheriting the parent's
    derivative/category/generators + per-variant tree mix and the parent's
    decorators). Overlay-only: pack-base is read for inheritance but never written."""
    os.makedirs(OVERLAY_VANILLA, exist_ok=True)
    written = 0
    for variant in sorted(BIOME_VARIANTS):
        parent, rarity, groups = BIOME_VARIANTS[variant]
        rarity = RARITY_SCALE.get(rarity, rarity)
        parent_base = os.path.join(BASE_VANILLA, parent + ".json")
        if not os.path.exists(parent_base):
            print("  SKIP variant (missing parent base):", variant)
            continue
        with open(parent_base, "r", encoding="utf-8") as f:
            base = json.load(f)
        base["name"] = "vanilla/" + variant
        base["rarity"] = rarity
        if parent in BIOME_GENERATORS:
            base["generators"] = BIOME_GENERATORS[parent]
        if parent in BIOME_LAYERS:
            base["layers"] = BIOME_LAYERS[parent]
        data = dict(base)
        objects = []
        for sp, size, chance, density in groups:
            place = resolve_place(refs, sp, size)
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
