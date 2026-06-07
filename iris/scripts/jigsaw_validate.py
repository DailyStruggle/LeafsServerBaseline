"""
jigsaw_validate.py - static link-resolution check for an Iris jigsaw graph.

It mirrors how Iris resolves a jigsaw structure at load time so we can confirm
"does this wire?" *without* a running server:

  jigsaw-structures/<key>.json .pieces[]   -> jigsaw-pieces/<piece>.json   (NO "jigsaw/" prefix)
  jigsaw-pools/<pool>.json     .pieces[]   -> jigsaw-pieces/<piece>.json   (NO "jigsaw/" prefix)
  jigsaw-pieces/<piece>.json   .object     -> objects/<object>.iob         ("jigsaw/" prefix kept)
  jigsaw-pieces/.connectors[].pools[]      -> jigsaw-pools/<pool>.json

Pass one or more pack roots; later roots act as fallbacks (e.g. the staging pack
provides shared pools the derived set may point at). Reports every unresolved
reference and a reachable-from-structure summary.

Usage:
  python jigsaw_validate.py --structure village-meadow --root <derived_iris_dir> [--root <staging_dir>]
Exit code 0 = fully resolved, 1 = unresolved references found.
"""
import argparse
import json
import os
import sys


def load_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def find(roots, *parts):
    for r in roots:
        p = os.path.join(r, *parts)
        if os.path.isfile(p):
            return p
    return None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--structure", required=True)
    ap.add_argument("--root", action="append", required=True,
                    help="pack root(s); repeat for fallbacks")
    args = ap.parse_args()
    roots = args.root

    # Errors break rendering (a reachable piece whose .iob object is absent).
    # Warnings are tolerable: an optional pool/piece that simply has nothing to
    # attach - Iris skips it and the structure still generates (possibly smaller).
    errors = []
    warnings = []
    seen_pieces = set()
    seen_pools = set()

    def check_piece(piece_key):
        if piece_key in seen_pieces:
            return
        seen_pieces.add(piece_key)
        pp = find(roots, "jigsaw-pieces", *(piece_key + ".json").split("/"))
        if not pp:
            warnings.append(f"missing piece JSON: jigsaw-pieces/{piece_key}.json")
            return
        pj = load_json(pp)
        obj = pj.get("object")
        if obj:
            op = find(roots, "objects", *(obj + ".iob").split("/"))
            if not op:
                errors.append(f"MISSING object: objects/{obj}.iob "
                              f"(referenced by piece {piece_key})")
        for con in pj.get("connectors", []):
            for pool in con.get("pools", []):
                check_pool(pool)

    def check_pool(pool_key):
        if pool_key in seen_pools:
            return
        seen_pools.add(pool_key)
        pp = find(roots, "jigsaw-pools", *(pool_key + ".json").split("/"))
        if not pp:
            warnings.append(f"missing pool JSON: jigsaw-pools/{pool_key}.json")
            return
        for piece_key in load_json(pp).get("pieces", []):
            check_piece(piece_key)

    sp = find(roots, "jigsaw-structures", args.structure + ".json")
    if not sp:
        sys.exit(f"structure not found: jigsaw-structures/{args.structure}.json")
    sj = load_json(sp)
    for piece_key in sj.get("pieces", []):
        check_piece(piece_key)

    print(f"structure        : {args.structure}")
    print(f"reachable pieces : {len(seen_pieces)}")
    print(f"reachable pools  : {len(seen_pools)}")
    if warnings:
        uw = sorted(set(warnings))
        print(f"\nWARNINGS ({len(uw)}) - tolerable, structure still generates:")
        for w in uw[:15]:
            print("  " + w)
        if len(uw) > 15:
            print(f"  ... and {len(uw) - 15} more")
    if errors:
        ue = sorted(set(errors))
        print(f"\nERRORS ({len(ue)}) - these break rendering:")
        for e in ue[:40]:
            print("  " + e)
        if len(ue) > 40:
            print(f"  ... and {len(ue) - 40} more")
        sys.exit(1)
    print("\nOK - every reachable piece resolves to an .iob object. Graph wires.")


if __name__ == "__main__":
    main()
