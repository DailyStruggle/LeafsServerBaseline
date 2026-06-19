"""build_volcano_nbt.py - bake a self-contained volcano cone as a vanilla structure .nbt.

This revives the (previously abandoned) lava-tree mountaintop builder
(`legacy/iris/scripts/build-lavatree-mountaintops.py`) and retargets its output
from an Iris `.iob` object to a vanilla structure-template `.nbt`, so the cone can
be placed by vanilla datapack worldgen (jigsaw `template_pool` / `/place template`)
per ADR-005.

The cone is fully self-contained: it bakes its own basalt/tuff shell, a flat lava
pool near the summit, and an open `void_air` crater, so the dramatic volcano relief
does NOT depend on the global terrain noise router (which is shared by all biomes).
The structure is gated to `leaf:volcanic_mountain` and placed with `terrain_adaptation`
so it marries into whatever surface it lands on.

Usage (run from repo root):
  python tools/volcano-gen/build_volcano_nbt.py
  python tools/volcano-gen/build_volcano_nbt.py --family cinderfall --seed 42101
"""
import argparse
import importlib.util
import os
import sys

_HERE = os.path.dirname(os.path.abspath(__file__))
_ROOT = os.path.abspath(os.path.join(_HERE, "..", ".."))
_TREE_GEN = os.path.join(_ROOT, "tools", "tree-gen")
_LEGACY = os.path.join(_ROOT, "legacy", "iris", "scripts", "build-lavatree-mountaintops.py")

# tree-gen must be importable BEFORE the legacy module is loaded (it does
# `from generate_tree import ...` / `from nbt import ...` against this path).
sys.path.insert(0, _TREE_GEN)

from generate_tree import generate_tree          # noqa: E402  (path injected above)
from nbt import write_structure_nbt              # noqa: E402

# Default cone template lives inside the leaf-worldgen datapack.
OUT_DEFAULT = os.path.join(_ROOT, "datapacks", "leaf-worldgen", "data", "leaf",
                           "structure", "volcano")


def _load_legacy():
    """Import the legacy lava-tree-mountaintop builder as a module by file path."""
    spec = importlib.util.spec_from_file_location("lavatree_mt", _LEGACY)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def build_cone(family: str, seed: int, trunk_width: int, height: int,
               dome_radius: int, out_path: str) -> None:
    lavamt = _load_legacy()
    palette = lavamt.PALETTES[family]

    entry = lavamt._lava_tree_entry(seed, trunk_width, height, height)
    blocks = generate_tree(entry, height)
    lavamt._add_mountaintop(blocks, seed, dome_radius, lavamt.MOUNTAIN_HEIGHT, palette)

    # Anchor the cone at its BELLY (the widest ring, i.e. the natural ground line)
    # so that vanilla `project_start_to_heightmap` drops the base onto the surface
    # and the cone rises ABOVE ground (crater near the summit), while the buried
    # lava mass extends BELOW the surface. (The legacy IOB builder anchored the
    # crater at y=0 instead, which would have buried the whole cone.)
    top_y, _cx, _cz, _r = lavamt._crater_geometry(blocks)
    belly_y = top_y - lavamt.MOUNTAIN_HEIGHT
    blocks = {(x, y - belly_y, z): s for (x, y, z), s in blocks.items()}

    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    write_structure_nbt(out_path, blocks)

    xs = [p[0] for p in blocks]
    ys = [p[1] for p in blocks]
    zs = [p[2] for p in blocks]
    print(f"  wrote {out_path}")
    print(f"  W={max(xs)-min(xs)+1} H={max(ys)-min(ys)+1} L={max(zs)-min(zs)+1} "
          f"blocks={len(blocks)} (family={family}, tw={trunk_width}, h={height})")


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--family", default="cinderfall",
                    help="volcano palette family (cinderfall/ashcrown/embertide/...)")
    ap.add_argument("--seed", type=int, default=42101, help="RNG seed for the cone")
    ap.add_argument("--trunk-width", type=int, default=60,
                    help="lava-mass trunk width (cone girth driver)")
    ap.add_argument("--height", type=int, default=70, help="lava-tree height")
    ap.add_argument("--dome-radius", type=int, default=44,
                    help="cone flank radius at the belly")
    ap.add_argument("--out", default=None,
                    help="output .nbt path (default: leaf-worldgen structure/volcano/<family>_cone.nbt)")
    args = ap.parse_args()

    out_path = args.out or os.path.join(OUT_DEFAULT, f"{args.family}_cone.nbt")
    print(f"Building volcano cone '{args.family}'...")
    build_cone(args.family, args.seed, args.trunk_width, args.height,
               args.dome_radius, out_path)
    print("Done.")


if __name__ == "__main__":
    main()
