#!/usr/bin/env python3
"""Migrate missing-object vanilla tree placers to Iris 4.0 native procedural trees.

Problem this solves
--------------------
A set of vanilla tree families is referenced by biome ``objects`` placers but has
NO producing source in the repo (``iris/output`` only emits ``vanilla-*`` and
biome-named tree sets, never a bare ``trees/oak`` / ``trees/spruce`` / ... folder).
Those references therefore resolve to nothing at world-gen ("Couldn't find Object:
trees/oak/...").

Instead of shipping pre-baked ``.iob`` objects for them, this script regenerates
the same vanilla trees with Iris 4.0's NATIVE procedural-tree generator
(``IrisBiome.proceduralObjects.trees`` -> ``IrisProceduralTree``) - exactly the
mechanism pack-base's "estranged" biomes already use.

How it works (reproducible)
---------------------------
1. Build a procedural-tree variant library from our existing offline tree-gen
   configs (``iris/tree-gen/configs/*.json``, excluding the deprecated
   ``lavatree.json``) by running each entry through
   ``iris/tree-gen/to_procedural.py`` (``convert_entry``). The library is keyed by
   tree profile (oak, spruce, birch, cherry, ...).
2. For each biome under ``iris/pack-base/biomes`` and ``iris/pack-overlay/biomes``
   that references one of the unsatisfiable families (``MISSING_FAMILIES``), build
   an overlay override (whole-file, per the layering contract): copy the source
   biome (the existing overlay file if present, otherwise the base file), strip the
   dead ``trees/<family>/...`` object placers, and inject equivalent
   ``proceduralObjects.trees`` (mushrooms -> ``proceduralObjects.fungi``) preserving
   the original placement chance.
3. Output overlay files are validated as JSON before being written.

Run:  python iris/scripts/build-procedural-trees.py
Then: powershell iris/scripts/deploy-iris-pack.ps1

The script is idempotent: once a biome's dead placers have been converted, a
re-run finds no missing-family references in the (overlay) source and skips it.
"""

import copy
import json
import os
import sys
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
IRIS = os.path.dirname(HERE)
TREEGEN = os.path.join(IRIS, "tree-gen")
CONFIGS = os.path.join(TREEGEN, "configs")
BASE_BIOMES = os.path.join(IRIS, "pack-base", "biomes")
OVERLAY_BIOMES = os.path.join(IRIS, "pack-overlay", "biomes")

sys.path.insert(0, TREEGEN)
from to_procedural import convert_entry  # noqa: E402

# Tree families referenced by biome placers that the repo cannot satisfy with a
# baked .iob object (no matching iris/output folder). These get regenerated as
# native procedural trees instead.
MISSING_FAMILIES = ("oak", "spruce", "mushroom", "sproak", "willow", "sakura", "mixed")

# family -> tree profiles to grow procedurally (drawn from the config-derived
# variant library). "mushroom" is special-cased to procedural fungi.
FAMILY_TO_PROFILES = {
    "oak":    ["oak"],
    "spruce": ["spruce"],
    "sproak": ["spruce", "oak"],
    "mixed":  ["oak", "birch", "spruce"],
    "sakura": ["cherry"],
    "willow": ["willow"],
}

# Max procedural tree variants emitted per family per biome (keeps biome files
# compact while still giving shape variety; the original placer density/chance is
# preserved by splitting the summed chance across the chosen variants).
MAX_VARIANTS_PER_FAMILY = 4

# Default per-tree chance used when the original placers carried no explicit chance.
DEFAULT_CHANCE = 0.02


def _warn(msg):
    print("  WARN: %s" % msg, file=sys.stderr)


def _stable_seed(*parts):
    """Deterministic 31-bit seed from the given string parts (reproducible runs)."""
    return zlib.crc32("|".join(str(p) for p in parts).encode("utf-8")) & 0x7FFFFFFF


def build_profile_library():
    """Convert every tree-gen config entry to procedural form, grouped by profile."""
    library = {}
    if not os.path.isdir(CONFIGS):
        return library
    for fn in sorted(os.listdir(CONFIGS)):
        if not fn.endswith(".json") or fn == "lavatree.json":
            continue
        path = os.path.join(CONFIGS, fn)
        with open(path, "r", encoding="utf-8-sig") as f:
            entries = json.load(f)
        if not isinstance(entries, list):
            continue
        for entry in entries:
            profile = str(entry.get("profile", "")).lower()
            if not profile:
                continue
            proc = convert_entry(entry, _warn)
            library.setdefault(profile, []).append(proc)
    # Deterministic ordering per profile.
    for profile in library:
        library[profile].sort(key=lambda d: (d.get("seed", 0), d.get("name", "")))
    return library


def authored_willow():
    """Native Iris WILLOW-profile tree (no offline tree-gen source exists)."""
    return {
        "name": "willow",
        "trunk": "minecraft:oak_log",
        "leaves": "minecraft:oak_leaves",
        "profile": "WILLOW",
        "heightMin": 6,
        "heightMax": 10,
        "variants": 6,
        "trunkWidth": 1,
        "canopy": {"squish": 0.7, "mode": "DENSITY", "leafDensity": 0.85},
    }


def authored_fungi():
    """Procedural red/brown giant mushrooms (proceduralObjects.fungi)."""
    return [
        {
            "name": "red-mushroom",
            "variants": 5,
            "stem": "minecraft:mushroom_stem",
            "cap": "minecraft:red_mushroom_block",
            "stemHeightMin": 4,
            "stemHeightMax": 8,
            "stemWidth": 1,
            "capRadiusMin": 2,
            "capRadiusMax": 4,
            "capThickness": 1,
        },
        {
            "name": "brown-mushroom",
            "variants": 5,
            "stem": "minecraft:mushroom_stem",
            "cap": "minecraft:brown_mushroom_block",
            "stemHeightMin": 3,
            "stemHeightMax": 6,
            "stemWidth": 1,
            "capRadiusMin": 3,
            "capRadiusMax": 5,
            "capThickness": 1,
        },
    ]


def _clamp_chance(c):
    return max(0.001, min(0.95, round(c, 4)))


def make_family_trees(family, total_chance, library, biome_rel):
    """Return a list of IrisProceduralTree dicts for a non-mushroom family."""
    profiles = FAMILY_TO_PROFILES.get(family, [])
    pool = []
    if family == "willow":
        pool = [authored_willow()]
    else:
        # Round-robin across the mapped profiles so a mix family stays mixed.
        per_profile = [library.get(p, []) for p in profiles]
        idx = 0
        while len(pool) < MAX_VARIANTS_PER_FAMILY and any(
            idx < len(pp) for pp in per_profile
        ):
            for pp in per_profile:
                if idx < len(pp) and len(pool) < MAX_VARIANTS_PER_FAMILY:
                    pool.append(pp[idx])
            idx += 1
    if not pool:
        _warn("no procedural source for family '%s' (biome %s) - skipped" % (family, biome_rel))
        return []

    n = len(pool)
    per_chance = _clamp_chance((total_chance or DEFAULT_CHANCE * n) / n)
    trees = []
    for i, src in enumerate(pool):
        tree = copy.deepcopy(src)
        tree["name"] = "pv-%s-%s-%d" % (family, tree.get("name", "tree"), i)
        tree["chance"] = per_chance
        tree["density"] = 1
        tree["seed"] = _stable_seed(biome_rel, family, tree["name"], i)
        trees.append(tree)
    return trees


def make_family_fungi(total_chance, biome_rel):
    fungi = authored_fungi()
    per_chance = _clamp_chance((total_chance or DEFAULT_CHANCE * len(fungi)) / len(fungi))
    out = []
    for i, src in enumerate(fungi):
        f = copy.deepcopy(src)
        f["name"] = "pv-mushroom-%s-%d" % (f["name"], i)
        f["chance"] = per_chance
        f["density"] = 1
        f["seed"] = _stable_seed(biome_rel, "mushroom", f["name"], i)
        out.append(f)
    return out


def _family_of(key):
    """trees/<family>/... -> family, else None."""
    parts = key.split("/")
    if len(parts) >= 3 and parts[0] == "trees":
        return parts[1]
    return None


def _process_node(node, family_chance, stats):
    """Recursively strip dead-family keys from every placer (any nesting depth).

    A placer is any dict carrying a "place" key. Records the MAX placer chance per
    family in ``family_chance``. Returns the rewritten node, or None to signal that
    a placer became empty and must be dropped from its parent list.
    """
    if isinstance(node, dict):
        if "place" in node:
            place = node["place"]
            place_list = [place] if isinstance(place, str) else list(place)
            kept = []
            chance = float(node.get("chance", DEFAULT_CHANCE))
            for key in place_list:
                fam = _family_of(key) if isinstance(key, str) else None
                if fam in MISSING_FAMILIES:
                    family_chance[fam] = max(family_chance.get(fam, 0.0), chance)
                    stats["removed"] += 1
                else:
                    kept.append(key)
            if not kept:
                return None  # whole placer was dead-family -> drop it
            if len(kept) != len(place_list):
                node = dict(node)
                node["place"] = kept
            return node
        return {k: _process_node(v, family_chance, stats) for k, v in node.items()}
    if isinstance(node, list):
        out = []
        for item in node:
            r = _process_node(item, family_chance, stats)
            if r is not None:
                out.append(r)
        return out
    return node


def transform_biome(data, biome_rel, library):
    """Strip dead-family tree placers and inject procedural equivalents.

    Returns (changed: bool, summary: dict). Operates on a clean ORIGINAL biome, so
    every dead family is recomputed from scratch (no incremental state).
    """
    family_chance = {}
    stats = {"removed": 0}
    data = _process_node(data, family_chance, stats)
    removed = stats["removed"]

    if not family_chance:
        return False, {}, data

    proc = data.get("proceduralObjects")
    if not isinstance(proc, dict):
        proc = {}
    # Drop any previously generated entries so rebuilds are clean (idempotent).
    trees = [t for t in proc.get("trees", [])
             if not (isinstance(t, dict) and str(t.get("name", "")).startswith("pv-"))] \
        if isinstance(proc.get("trees"), list) else []
    fungi = [f for f in proc.get("fungi", [])
             if not (isinstance(f, dict) and str(f.get("name", "")).startswith("pv-"))] \
        if isinstance(proc.get("fungi"), list) else []

    summary = {"removed_placers": removed, "families": {}}
    for fam, total in sorted(family_chance.items()):
        if fam == "mushroom":
            added = make_family_fungi(total, biome_rel)
            fungi.extend(added)
        else:
            added = make_family_trees(fam, total, library, biome_rel)
            trees.extend(added)
        summary["families"][fam] = len(added)

    if trees:
        proc["trees"] = trees
    if fungi:
        proc["fungi"] = fungi
    data["proceduralObjects"] = proc
    return True, summary, data


def iter_biome_files(root):
    for dirpath, _dirs, files in os.walk(root):
        for fn in files:
            if fn.endswith(".json"):
                yield os.path.join(dirpath, fn)


def _has_missing_ref(text):
    return any(('"trees/%s/' % fam) in text for fam in MISSING_FAMILIES)


def load_original(rel):
    """Return the biome to convert, or None to leave the current file untouched.

    Rules (edit-preserving and idempotent):
      * If an overlay file exists and STILL references a missing family, it is an
        un-converted original (possibly hand-edited) - use it as-is so any manual
        edits are preserved through the conversion.
      * If an overlay file exists without missing-family references, it is either
        already converted or simply unrelated - skip it (return None).
      * Otherwise fall back to the base-pack biome, but only when it actually
        references a missing family.
    """
    overlay_path = os.path.join(OVERLAY_BIOMES, rel.replace("/", os.sep))
    if os.path.isfile(overlay_path):
        with open(overlay_path, "r", encoding="utf-8-sig") as f:
            text = f.read()
        if _has_missing_ref(text):
            return json.loads(text)
        return None
    base_path = os.path.join(BASE_BIOMES, rel.replace("/", os.sep))
    if os.path.isfile(base_path):
        with open(base_path, "r", encoding="utf-8-sig") as f:
            text = f.read()
        if _has_missing_ref(text):
            return json.loads(text)
    return None


def main():
    library = build_profile_library()
    print("Procedural variant library: " +
          ", ".join("%s=%d" % (p, len(v)) for p, v in sorted(library.items())))

    # Candidate biome rel paths: only biomes whose base/overlay text references a
    # missing family (still to convert) or already carry our "pv-" entries (a
    # previous run to rebuild cleanly). This keeps the (per-file) git lookups in
    # load_original limited to relevant biomes.
    markers = ['"trees/%s/' % fam for fam in MISSING_FAMILIES] + ['"pv-']
    rel_paths = set()
    for root in (BASE_BIOMES, OVERLAY_BIOMES):
        if not os.path.isdir(root):
            continue
        for path in iter_biome_files(root):
            try:
                with open(path, "r", encoding="utf-8-sig") as f:
                    text = f.read()
            except OSError:
                continue
            if any(m in text for m in markers):
                rel_paths.add(os.path.relpath(path, root).replace("\\", "/"))

    converted = 0
    skipped = 0
    errors = 0
    total_families = {}

    for rel in sorted(rel_paths):
        overlay_path = os.path.join(OVERLAY_BIOMES, rel.replace("/", os.sep))
        data = load_original(rel)
        if data is None:
            skipped += 1
            continue

        changed, summary, data = transform_biome(data, rel, library)
        if not changed:
            skipped += 1
            continue

        # Validate the result round-trips as JSON.
        try:
            json.loads(json.dumps(data))
        except ValueError as e:
            _warn("produced invalid JSON for %s: %s" % (rel, e))
            errors += 1
            continue

        os.makedirs(os.path.dirname(overlay_path), exist_ok=True)
        with open(overlay_path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
        converted += 1
        for fam, cnt in summary["families"].items():
            total_families[fam] = total_families.get(fam, 0) + cnt
        print("  %s  -> %s" % (rel, summary["families"]))

    print("")
    print("Biomes converted : %d" % converted)
    print("Biomes skipped   : %d (no missing-family placers in source)" % skipped)
    print("Errors           : %d" % errors)
    print("Procedural defs added per family: %s" % total_families)


if __name__ == "__main__":
    main()
