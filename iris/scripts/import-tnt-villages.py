"""Batch-import Towns & Towers village sets into the private patch layer.

This is orchestration only: it shells out to nbt_to_iris.py to convert the raw
(git-ignored, CC BY-NC-ND) T&T NBT pieces into Iris jigsaw assets, then it
generates/merges the additive biome *.patch.json files that wire each village
into our biome taxonomy (New World theme: villages in common/temperate biomes).

It writes NO third-party bytes itself - the converted assets land in the
git-ignored quarantine and are copied into the (also git-ignored) pack-patches
overlay. Per ADR-004 none of that is redistributed.

MANIFEST maps each T&T village set -> Iris structure key -> target biome files
(relative to the pack root, e.g. biomes/swamp/marsh.json) -> rarity
(Iris convention: lower = more common).

Run:
  python iris/scripts/import-tnt-villages.py
"""
import json
import os
import subprocess
import sys

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
IRIS = os.path.join(REPO, "iris")
RAW = os.path.join(IRIS, "thirdparty-derived", "towns-and-towers", "raw")
DERIVED = os.path.join(IRIS, "thirdparty-derived", "towns-and-towers", "iris")
STRUCT_DEFS = os.path.join(RAW, "data", "towns_and_towers", "worldgen", "structure")
PATCH_ASSETS = os.path.join(IRIS, "pack-patches", "assets")
PATCH_SPECS = os.path.join(IRIS, "pack-patches", "patches")
NBT_TO_IRIS = os.path.join(IRIS, "scripts", "nbt_to_iris.py")

# set dir (under kaisyn structure village/) | structure def json | iris key |
# target biome files | rarity
MANIFEST = [
    # set, struct_json, key, targets, rarity
    ("swamp_boat", "village_swamp", "village-swamp",
     ["biomes/swamp/marsh.json", "biomes/swamp/swamp-forest.json"], 1200),
    ("old_growth_taiga_polish", "village_old_growth_taiga",
     "village-old-growth-taiga",
     ["biomes/tundra/taiga.json", "biomes/tundra/taiga-extended.json"], 1000),
    ("snowy_taiga_viking", "village_snowy_taiga", "village-snowy-taiga",
     ["biomes/frozen/spruce-plains.json", "biomes/frozen/spruce-hills.json"],
     1200),
    ("savanna_plateau_ramshackled", "village_savanna_plateau",
     "village-savanna-plateau", ["biomes/savanna/plateau.json"], 1200),
    ("jungle_tribal", "village_jungle", "village-jungle",
     ["biomes/tropical/rainforest.json", "biomes/tropical/jungle-denmyre.json"],
     1400),
    ("sparse_jungle_polynesian", "village_sparse_jungle",
     "village-sparse-jungle",
     ["biomes/tropical/bamboo-forest.json", "biomes/tropical/wilds.json"], 1400),
    ("badlands_pueblo", "village_badlands", "village-badlands",
     ["biomes/mesa/mesa.json", "biomes/mesa/red.json"], 1200),
    ("wooded_badlands_tipi", "village_wooded_badlands",
     "village-wooded-badlands", ["biomes/mesa/plateau-dirt.json"], 1400),
    ("sunflower_plains_farm", "village_sunflower_plains",
     "village-sunflower-plains",
     ["biomes/temperate/plains.json", "biomes/temperate/fancyplains.json"],
     1000),
    ("beach_lighthouse", "village_beach", "village-beach",
     ["biomes/tropical/beach.json", "biomes/tropical/island-beach.json"], 1200),
    ("mushroom_fields_fantasy", "village_mushroom_fields",
     "village-mushroom-fields", ["biomes/mushroom/plains.json"], 1500),
    ("grove_villager_outpost", "village_grove", "village-grove",
     ["biomes/frozen/pine-plains.json"], 1400),
    ("snowy_slopes_inn", "village_snowy_slopes", "village-snowy-slopes",
     ["biomes/frozen/hills.json"], 1400),
    ("birch_forest_romanian", "village_birch_forest", "village-birch-forest",
     ["biomes/temperate/birch-forest.json"], 1200),
    ("flower_forest_japanese", "village_flower_forest", "village-flower-forest",
     ["biomes/temperate/flower-forest.json"], 1200),
    ("forest_ruins", "village_forest", "village-forest",
     ["biomes/temperate/oak-forest.json"], 1200),
]


def convert(set_dir, struct_json, key):
    struct_path = os.path.join(STRUCT_DEFS, struct_json + ".json")
    if not os.path.isfile(struct_path):
        print("  SKIP %s: structure def missing (%s)" % (key, struct_path))
        return False
    cmd = [
        sys.executable, NBT_TO_IRIS,
        "--raw", RAW, "--out", DERIVED,
        "--namespace", "kaisyn", "--set", "village/" + set_dir,
        "--structure-json", struct_path, "--structure-key", key,
    ]
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print("  ERROR converting %s:\n%s\n%s" % (key, r.stdout, r.stderr))
        return False
    # echo just the summary tail
    tail = [ln for ln in r.stdout.splitlines() if ln.startswith((
        "converted", "total", "pools", "structure"))]
    print("  %s: %s" % (key, " | ".join(tail)))
    return True


def merge_patch(target_rel, key, rarity):
    """Create/merge a *.patch.json adding {structure:key, rarity} to a biome."""
    # patch file path mirrors the target under PATCH_SPECS with .patch.json
    rel_os = target_rel.replace("/", os.sep)
    patch_path = os.path.join(PATCH_SPECS, rel_os + ".patch.json")
    if os.path.isfile(patch_path):
        with open(patch_path, encoding="utf-8") as f:
            spec = json.load(f)
    else:
        spec = {"target": target_rel, "patches": []}
    # idempotent: skip if this structure already present
    for op in spec["patches"]:
        if op.get("op") == "addUnique" and \
                op.get("value", {}).get("structure") == key:
            return "exists"
    spec["patches"].append({
        "op": "addUnique",
        "array": "jigsawStructures",
        "key": "structure",
        "value": {"structure": key, "rarity": rarity},
    })
    os.makedirs(os.path.dirname(patch_path), exist_ok=True)
    with open(patch_path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(spec, f, indent=4)
        f.write("\n")
    return "written"


def copy_assets():
    """Mirror derived objects/jigsaw-* into pack-patches/assets (additive)."""
    import shutil
    copied = 0
    for sub in ("objects", "jigsaw-pieces", "jigsaw-pools", "jigsaw-structures"):
        src = os.path.join(DERIVED, sub)
        if not os.path.isdir(src):
            continue
        for root, _dirs, files in os.walk(src):
            for fn in files:
                s = os.path.join(root, fn)
                rel = os.path.relpath(s, DERIVED)
                d = os.path.join(PATCH_ASSETS, rel)
                os.makedirs(os.path.dirname(d), exist_ok=True)
                shutil.copy2(s, d)
                copied += 1
    return copied


def main():
    print("=== converting %d T&T village sets ===" % len(MANIFEST))
    ok = 0
    patches = 0
    for set_dir, struct_json, key, targets, rarity in MANIFEST:
        if convert(set_dir, struct_json, key):
            ok += 1
            for t in targets:
                res = merge_patch(t, key, rarity)
                if res == "written":
                    patches += 1
                print("    patch %s -> %s (%s, rarity %d)" % (
                    key, t, res, rarity))
    print("=== copying assets into pack-patches/assets ===")
    n = copy_assets()
    print("converted sets : %d/%d" % (ok, len(MANIFEST)))
    print("patch ops added: %d" % patches)
    print("asset files cp : %d" % n)


if __name__ == "__main__":
    main()
