"""build-lava-jigsaw.py - generate the tile objects for the emergent lava-caldera jigsaw.

Instead of one predefined bowl object, the caldera is assembled at generation time
from small connecting tiles (the bowl is an EMERGENT result of the jigsaw):

  * lavacell  - a 3x3 footprint, 2 tall: an obsidian floor with a flat lava top.
                It carries 4 horizontal connectors (N/S/E/W) at the lava plane with
                lockY=true, so neighbouring lava cells always tile at the SAME Y -
                guaranteeing one level lava surface across the whole structure.
  * lavaedge  - a 3x3x3 solid obsidian block used as the TERMINAL piece. It has no
                connectors, so wherever the jigsaw stops it caps the lava with an
                obsidian wall one block above the lava ("obsidian ends").

Both are authored as diffable JSON sources compiled to .iob via json_to_iob.py, like
build-magmaspires.py / build-lava-pockets.py.

Usage:
  python build-lava-jigsaw.py            # write sources + compile .iob
  python build-lava-jigsaw.py --sources  # only (re)write the JSON sources
"""
import argparse
import json
import os

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
SRC_ROOT = os.path.join(REPO, "object-src", "jigsaw", "clutter")
OUT_ROOT = os.path.join(REPO, "pack-overlay", "objects", "jigsaw", "clutter")


def lavacell_blocks():
    """3x3 footprint, y=0 obsidian floor, y=1 flat lava top (level source)."""
    blocks = []
    for x in range(3):
        for z in range(3):
            blocks.append({"x": x, "y": 0, "z": z, "state": "minecraft:obsidian"})
            blocks.append({"x": x, "y": 1, "z": z, "state": "minecraft:lava[level=0]"})
    return blocks


def lavaedge_blocks():
    """3x3x3 solid obsidian terminal cap (top sits one block above the lava plane)."""
    blocks = []
    for x in range(3):
        for y in range(3):
            for z in range(3):
                blocks.append({"x": x, "y": y, "z": z, "state": "minecraft:obsidian"})
    return blocks


OBJECTS = [
    ("lavacell", {"w": 3, "h": 2, "d": 3}, lavacell_blocks),
    ("lavaedge", {"w": 3, "h": 3, "d": 3}, lavaedge_blocks),
]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sources", action="store_true",
                    help="only (re)write JSON sources, do not compile")
    args = ap.parse_args()

    os.makedirs(SRC_ROOT, exist_ok=True)
    sources = []
    for name, size, fn in OBJECTS:
        src = {
            "size": size,
            "centered": False,
            "blocks": fn(),
        }
        src_path = os.path.join(SRC_ROOT, name + ".json")
        with open(src_path, "w", encoding="utf-8") as f:
            json.dump(src, f, indent=2)
            f.write("\n")
        sources.append((name, src_path))
        print(f"  source: object-src/jigsaw/clutter/{name}.json "
              f"({size['w']}x{size['h']}x{size['d']}, {len(src['blocks'])} blocks)")

    if args.sources:
        return

    import importlib.util
    spec = importlib.util.spec_from_file_location(
        "json_to_iob", os.path.join(os.path.dirname(__file__), "json_to_iob.py"))
    j2i = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(j2i)

    os.makedirs(OUT_ROOT, exist_ok=True)
    for name, src_path in sources:
        out_path = os.path.join(OUT_ROOT, name + ".iob")
        j2i.compile_source(src_path, out_path)


if __name__ == "__main__":
    main()
