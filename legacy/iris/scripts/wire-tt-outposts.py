"""wire-tt-outposts.py - deploy the converted Towns & Towers pillager-outpost
variants from the git-ignored quarantine into the (also git-ignored) pack-patches
"special structures" overlay, and amend the per-biome *.patch.json files so each
biome that already carries its matching T&T village also gets the matching outpost.

This is TOOLING (tracked); it only ever WRITES into git-ignored dirs:
  - iris/pack-patches/assets/         (additive .iob/pieces/pools/structures)
  - iris/pack-patches/patches/biomes/ (addUnique `structures` placement entries)

Iris 4.0 format: outposts are emitted as `structures/<key>.json` (IrisStructure)
and placed via `structures` arrays of IrisStructurePlacement (NOT the 3.x
`jigsaw-structures/` + `jigsawStructures` form, which 4.0 does not load). See
docs/design/IRIS-V4-STRUCTURES.md.
No third-party asset is committed (ADR-004): the converted .iob/piece/pool files
live in the quarantine and the pack-patches overlay, both git-ignored.

The outpost->biome mapping mirrors the existing village wiring exactly (same biome
files), so an outpost only lands in a biome that is already proven to exist in the
staged pack. Run AFTER nbt_to_iris.py has produced the pillager-outpost-* sets in
the quarantine iris dir.

Usage:
  python wire-tt-outposts.py
"""
import json
import os
import shutil

HERE = os.path.dirname(os.path.abspath(__file__))
IRIS = os.path.dirname(HERE)
QUAR = os.path.join(IRIS, "thirdparty-derived", "towns-and-towers", "iris")
PATCHES = os.path.join(IRIS, "pack-patches")
ASSETS = os.path.join(PATCHES, "assets")
BIOME_PATCH_DIR = os.path.join(PATCHES, "patches", "biomes")

# v4 placement grid (chunks). Outposts are a rarer find than the common villages
# so the two do not visually collide. RANDOM_SPREAD = vanilla-style spacing grid.
OUTPOST_SPACING = 48
OUTPOST_SEPARATION = 12

# biome file (relative to biomes/, no .json) -> outpost structure slug.
# Mirrors the existing village->biome wiring one-for-one.
BIOME_TO_OUTPOST = {
    "frozen/hills": "pillager-outpost-snowy-slopes",
    "frozen/pine-plains": "pillager-outpost-grove",
    "frozen/spruce-hills": "pillager-outpost-snowy-taiga",
    "frozen/spruce-plains": "pillager-outpost-snowy-taiga",
    "mesa/mesa": "pillager-outpost-badlands",
    "mesa/plateau-dirt": "pillager-outpost-wooded-badlands",
    "mesa/red": "pillager-outpost-badlands",
    "mushroom/plains": "pillager-outpost-mushroom-fields",
    "savanna/plateau": "pillager-outpost-savanna-plateau",
    "swamp/marsh": "pillager-outpost-swamp",
    "swamp/swamp-forest": "pillager-outpost-swamp",
    "temperate/birch-forest": "pillager-outpost-birch-forest",
    "temperate/fancyplains": "pillager-outpost-sunflower-plains",
    "temperate/flower-forest": "pillager-outpost-flower-forest",
    "temperate/meadows": "pillager-outpost-meadow",
    "temperate/oak-forest": "pillager-outpost-forest",
    "temperate/plains": "pillager-outpost-sunflower-plains",
    "tropical/bamboo-forest": "pillager-outpost-sparse-jungle",
    "tropical/beach": "pillager-outpost-beach",
    "tropical/island-beach": "pillager-outpost-beach",
    "tropical/jungle-denmyre": "pillager-outpost-jungle",
    "tropical/rainforest": "pillager-outpost-jungle",
    "tropical/wilds": "pillager-outpost-sparse-jungle",
    "tundra/taiga-extended": "pillager-outpost-old-growth-taiga",
    "tundra/taiga": "pillager-outpost-old-growth-taiga",
}

# Entity-spawn / population pools the outpost pieces reference but that have no
# Iris .iob (pillagers/captives are mob spawns, not structure objects). Shipped as
# empty stubs so Iris resolves them cleanly (no "Can't find jigsaw pool") and
# attaches nothing - identical pattern to the village population stubs.
STUB_POOLS = [
    "outpost/pillagers/grunt",
    "outpost/pillagers/outpost_captain",
    "outpost/pillagers/captive_jungle",
    "outpost/pillagers/captive_snow",
]


def copy_tree_additive(sub):
    """Copy QUAR/<sub> into ASSETS/<sub>, only adding/overwriting files under it."""
    src = os.path.join(QUAR, sub)
    if not os.path.isdir(src):
        return 0
    n = 0
    for dp, _, files in os.walk(src):
        for fn in files:
            s = os.path.join(dp, fn)
            rel = os.path.relpath(s, QUAR)
            d = os.path.join(ASSETS, rel)
            os.makedirs(os.path.dirname(d), exist_ok=True)
            shutil.copy2(s, d)
            n += 1
    return n


def copy_outpost_structures():
    src = os.path.join(QUAR, "structures")
    dst = os.path.join(ASSETS, "structures")
    os.makedirs(dst, exist_ok=True)
    n = 0
    for fn in os.listdir(src):
        if fn.startswith("pillager-outpost-") and fn.endswith(".json"):
            shutil.copy2(os.path.join(src, fn), os.path.join(dst, fn))
            n += 1
    return n


def _salt_for(struct_key):
    import zlib
    return (zlib.crc32(struct_key.encode("utf-8")) % 90000) + 1000


def write_stub_pools():
    for key in STUB_POOLS:
        p = os.path.join(ASSETS, "jigsaw-pools", *key.split("/")) + ".json"
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump({"pieces": []}, f, indent=4)
            f.write("\n")


def amend_patch(biome_rel, outpost):
    """Add an addUnique `structures` (IrisStructurePlacement) entry for `outpost`
    to the biome's existing *.patch.json (created when its village was wired)."""
    pf = os.path.join(BIOME_PATCH_DIR, *biome_rel.split("/")) + ".json.patch.json"
    if not os.path.isfile(pf):
        print("  WARN missing patch file (village not wired here?): %s" % pf)
        return False
    with open(pf, encoding="utf-8") as f:
        doc = json.load(f)
    entry = {
        "op": "addUnique",
        "array": "structures",
        "key": "structures",
        "value": {
            "structures": [outpost],
            "distribution": "RANDOM_SPREAD",
            "spacing": OUTPOST_SPACING,
            "separation": OUTPOST_SEPARATION,
            "salt": _salt_for(outpost),
        },
    }
    for p in doc.get("patches", []):
        if (p.get("array") == "structures"
                and p.get("value", {}).get("structures") == [outpost]):
            return False  # already present -> idempotent
    doc.setdefault("patches", []).append(entry)
    with open(pf, "w", encoding="utf-8", newline="\n") as f:
        json.dump(doc, f, indent=4)
        f.write("\n")
    return True


def main():
    objs = copy_tree_additive(os.path.join("objects", "jigsaw", "outpost"))
    objs += copy_tree_additive(os.path.join("objects", "jigsaw", "ships"))
    pcs = copy_tree_additive(os.path.join("jigsaw-pieces", "outpost"))
    pcs += copy_tree_additive(os.path.join("jigsaw-pieces", "ships"))
    pls = copy_tree_additive(os.path.join("jigsaw-pools", "outpost"))
    pls += copy_tree_additive(os.path.join("jigsaw-pools", "ships"))
    structs = copy_outpost_structures()
    write_stub_pools()
    print("copied: objects=%d pieces=%d pools=%d structures=%d stubs=%d"
          % (objs, pcs, pls, structs, len(STUB_POOLS)))
    wired = 0
    for biome_rel, outpost in sorted(BIOME_TO_OUTPOST.items()):
        if amend_patch(biome_rel, outpost):
            wired += 1
            print("  wired %s -> %s (spacing %d/sep %d)"
                  % (biome_rel, outpost, OUTPOST_SPACING, OUTPOST_SEPARATION))
    print("patches amended: %d / %d" % (wired, len(BIOME_TO_OUTPOST)))


if __name__ == "__main__":
    main()
