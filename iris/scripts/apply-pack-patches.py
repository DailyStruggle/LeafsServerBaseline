"""Additive pack-patch layer for the Iris deploy pipeline.

Applies private, git-ignored JSON "patch" files onto the BUILT staging tree
(after pack-base + pack-overlay have been layered) so that special structures
(e.g. the Towns & Towers-derived village-meadow) can AMEND an existing biome
file instead of whole-file replacing it via pack-overlay.

Why this exists: deploy-iris-pack.ps1 copies pack-overlay over pack-base with
whole-file override. Putting a village-meadow reference in a tracked
pack-overlay biome file would (a) be erased semantics-wise into a full-file
override we must keep in sync, and (b) leak a reference into the redistributable
overlay. Patches live under iris/pack-patches/ which is git-ignored (not shipped
to consumers); only this engine and the README are tracked.

Patch file format (one target per file, *.patch.json), e.g.
iris/pack-patches/biomes/temperate/meadows.json.patch.json:

    {
        "target": "biomes/temperate/meadows.json",
        "patches": [
            {
                "op": "addUnique",
                "array": "jigsawStructures",
                "key": "structure",
                "value": { "structure": "village-meadow", "rarity": 1 }
            }
        ]
    }

Supported ops:
  - addUnique : ensure `array` exists on the target object and append `value`
                only if no existing element has the same `key` value (idempotent).
  - remove    : remove every element of `array` whose `key` equals `match`.
  - set       : set top-level field `field` to `value`.

The target file is rewritten as standard UTF-8 (no BOM, LF) JSON with 4-space
indent, matching the pack's existing style. Staging is disposable and rebuilt on
every deploy, so reformatting it is safe.

Usage:
  python apply-pack-patches.py --staging <stagingDir> --patches <patchesDir>
"""
import argparse
import json
import os
import sys


def load_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def write_json(path, data):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=4, ensure_ascii=False)
        f.write("\n")


def apply_patch_op(doc, op, target_rel):
    kind = op.get("op")
    if kind == "addUnique":
        arr_name = op["array"]
        key = op["key"]
        value = op["value"]
        arr = doc.get(arr_name)
        if not isinstance(arr, list):
            arr = []
            doc[arr_name] = arr
        for el in arr:
            if isinstance(el, dict) and el.get(key) == value.get(key):
                return "skip (already present): %s[%s=%s]" % (
                    arr_name, key, value.get(key))
        arr.append(value)
        return "addUnique: %s += {%s: %s}" % (arr_name, key, value.get(key))
    if kind == "remove":
        arr_name = op["array"]
        key = op["key"]
        match = op["match"]
        arr = doc.get(arr_name)
        if not isinstance(arr, list):
            return "skip (no array): %s" % arr_name
        before = len(arr)
        doc[arr_name] = [
            el for el in arr
            if not (isinstance(el, dict) and el.get(key) == match)
        ]
        return "remove: %s -= %d element(s) where %s=%s" % (
            arr_name, before - len(doc[arr_name]), key, match)
    if kind == "set":
        field = op["field"]
        doc[field] = op["value"]
        return "set: %s" % field
    raise ValueError("unknown op '%s' in patch for %s" % (kind, target_rel))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--staging", required=True, help="built staging dir")
    ap.add_argument("--patches", required=True, help="patches source dir")
    args = ap.parse_args()

    if not os.path.isdir(args.patches):
        print("no patches dir (%s) - nothing to apply" % args.patches)
        return 0

    patch_files = []
    for root, _dirs, files in os.walk(args.patches):
        for name in files:
            if name.endswith(".patch.json"):
                patch_files.append(os.path.join(root, name))
    patch_files.sort()

    if not patch_files:
        print("no *.patch.json files under %s" % args.patches)
        return 0

    applied = 0
    failed = 0
    for pf in patch_files:
        spec = load_json(pf)
        target_rel = spec["target"].replace("/", os.sep)
        target_abs = os.path.join(args.staging, target_rel)
        print("patch %s -> %s" % (os.path.basename(pf), spec["target"]))
        if not os.path.isfile(target_abs):
            print("  ERROR: target not found in staging: %s" % target_abs)
            failed += 1
            continue
        doc = load_json(target_abs)
        for op in spec.get("patches", []):
            try:
                msg = apply_patch_op(doc, op, spec["target"])
                print("  " + msg)
            except Exception as e:  # noqa: BLE001 - report and fail loudly
                print("  ERROR: %s" % e)
                failed += 1
        write_json(target_abs, doc)
        # validate round-trip
        load_json(target_abs)
        applied += 1

    print("patched %d file(s), %d error(s)" % (applied, failed))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
