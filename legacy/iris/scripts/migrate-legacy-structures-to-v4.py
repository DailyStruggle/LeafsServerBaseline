"""migrate-legacy-structures-to-v4.py - one-shot cutover of the Towns & Towers
"special structures" overlay from the Iris 3.x legacy format to the Iris 4.0 format.

Iris 4.0 does NOT load the legacy `jigsaw-structures/` folder or biome
`jigsawStructures` arrays (see docs/design/IRIS-V4-STRUCTURES.md). This tool
transitions the git-ignored pack-patches overlay to the v4 model:

  1. assets/jigsaw-structures/<key>.json   (legacy IrisJigsawStructure:
       {maxDepth, terminate, structureKey, pieces:[<startPiece>]})
     -> assets/structures/<key>.json       (v4 IrisStructure:
       {startPool, maxDepth, maxSizeChunks, placeMode})
     startPool is resolved as the jigsaw POOL whose `pieces` list contains the
     legacy start piece (villages -> .../town_centers, outposts -> .../base_plate).
     placeMode = STRUCTURE_PIECE (mirrors base structures/minecraft_village_plains.json).
     vanillaSource is intentionally omitted: these are MODDED (T&T) structures with
     no real `minecraft:` registry key (the legacy `structureKey` was synthetic).

  2. patches/biomes/**/*.patch.json
     Each `addUnique` op on the `jigsawStructures` array ({structure, rarity})
     becomes an `addUnique` op on the `structures` array, carrying a v4
     IrisStructurePlacement. rarity -> RANDOM_SPREAD grid:
       village-*          spacing 32 / separation 8
       pillager-outpost-* spacing 48 / separation 12
     A deterministic per-structure `salt` decouples placements that share a grid.

  3. Removes the now-dead assets/jigsaw-structures/ folder.

The jigsaw-pieces/ and jigsaw-pools/ folders are UNCHANGED across 3.x -> 4.0, so
the converted objects/pieces/pools are reused as-is.

Idempotent: re-running after the cutover is a no-op (no jigsaw-structures/ left,
patches already carry `structures`).

Usage:
  python migrate-legacy-structures-to-v4.py
"""
import json
import os
import shutil
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
IRIS = os.path.dirname(HERE)
PATCHES = os.path.join(IRIS, "pack-patches")
ASSETS = os.path.join(PATCHES, "assets")
LEGACY_STRUCT_DIR = os.path.join(ASSETS, "jigsaw-structures")
V4_STRUCT_DIR = os.path.join(ASSETS, "structures")
POOLS_DIR = os.path.join(ASSETS, "jigsaw-pools")
BIOME_PATCH_DIR = os.path.join(PATCHES, "patches", "biomes")

# rarity-class -> RANDOM_SPREAD grid (chunks). Villages are a common find, the
# T&T pillager outposts a rarer landmark; see iris/pack-patches/README.md.
GRID = {
    "village": {"spacing": 32, "separation": 8},
    "pillager-outpost": {"spacing": 48, "separation": 12},
}
DEFAULT_GRID = {"spacing": 40, "separation": 10}


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=4, ensure_ascii=False)
        f.write("\n")


def build_pool_index():
    """Map each piece key -> set of pool keys whose `pieces` list contains it."""
    idx = {}
    for dp, _, files in os.walk(POOLS_DIR):
        for fn in files:
            if not fn.endswith(".json"):
                continue
            full = os.path.join(dp, fn)
            pool_key = os.path.relpath(full, POOLS_DIR)[:-5].replace("\\", "/")
            try:
                doc = load(full)
            except Exception:
                continue
            for piece in doc.get("pieces", []) or []:
                idx.setdefault(piece, set()).add(pool_key)
    return idx


def grid_for(struct_key):
    for prefix, g in GRID.items():
        if struct_key.startswith(prefix):
            return g
    return DEFAULT_GRID


def salt_for(struct_key):
    # deterministic, stable, small positive salt
    return (zlib.crc32(struct_key.encode("utf-8")) % 90000) + 1000


def resolve_start_pool(struct_key, start_piece, pool_index):
    pools = pool_index.get(start_piece)
    if not pools:
        return None, "no pool contains start piece '%s'" % start_piece
    if len(pools) == 1:
        return next(iter(pools)), None
    # Prefer the pool whose key matches the start piece path exactly (outposts:
    # base_plate piece lives in the base_plate pool) or shares the longest prefix.
    if start_piece in pools:
        return start_piece, None
    best = sorted(pools, key=lambda p: (-len(os.path.commonprefix([p, start_piece])), p))[0]
    return best, "ambiguous (%d pools) -> chose %s" % (len(pools), best)


def convert_structures(pool_index):
    if not os.path.isdir(LEGACY_STRUCT_DIR):
        print("no legacy jigsaw-structures/ dir - structures already migrated")
        return 0, 0
    os.makedirs(V4_STRUCT_DIR, exist_ok=True)
    converted = 0
    warned = 0
    for fn in sorted(os.listdir(LEGACY_STRUCT_DIR)):
        if not fn.endswith(".json"):
            continue
        key = fn[:-5]
        legacy = load(os.path.join(LEGACY_STRUCT_DIR, fn))
        pieces = legacy.get("pieces") or []
        if not pieces:
            print("  WARN %s: no start pieces, skipped" % key)
            warned += 1
            continue
        start_pool, note = resolve_start_pool(key, pieces[0], pool_index)
        if start_pool is None:
            print("  ERROR %s: %s" % (key, note))
            warned += 1
            continue
        if note:
            print("  note %s: %s" % (key, note))
        v4 = {
            "startPool": start_pool,
            "maxDepth": legacy.get("maxDepth", 6),
            "maxSizeChunks": 8,
            "placeMode": "STRUCTURE_PIECE",
        }
        write(os.path.join(V4_STRUCT_DIR, fn), v4)
        converted += 1
        print("  %s: startPool=%s maxDepth=%d" % (key, start_pool, v4["maxDepth"]))
    return converted, warned


def convert_patches():
    if not os.path.isdir(BIOME_PATCH_DIR):
        print("no biome patches dir")
        return 0
    changed = 0
    for dp, _, files in os.walk(BIOME_PATCH_DIR):
        for fn in files:
            if not fn.endswith(".patch.json"):
                continue
            full = os.path.join(dp, fn)
            doc = load(full)
            touched = False
            for op in doc.get("patches", []):
                if op.get("array") != "jigsawStructures":
                    continue
                old = op.get("value", {})
                struct_key = old.get("structure")
                if not struct_key:
                    continue
                g = grid_for(struct_key)
                placement = {
                    "structures": [struct_key],
                    "distribution": "RANDOM_SPREAD",
                    "spacing": g["spacing"],
                    "separation": g["separation"],
                    "salt": salt_for(struct_key),
                }
                op["array"] = "structures"
                op["key"] = "structures"
                op["value"] = placement
                touched = True
            if touched:
                write(full, doc)
                changed += 1
                print("  rewrote %s" % os.path.relpath(full, PATCHES))
    return changed


def main():
    pool_index = build_pool_index()
    print("pool index: %d distinct pieces" % len(pool_index))
    conv, warned = convert_structures(pool_index)
    print("structures converted: %d (warnings/errors: %d)" % (conv, warned))
    patched = convert_patches()
    print("patch files rewritten: %d" % patched)
    if os.path.isdir(LEGACY_STRUCT_DIR) and warned == 0:
        shutil.rmtree(LEGACY_STRUCT_DIR)
        print("removed legacy assets/jigsaw-structures/")
    elif warned:
        print("KEPT legacy jigsaw-structures/ (resolve %d warning(s) first)" % warned)


if __name__ == "__main__":
    main()
