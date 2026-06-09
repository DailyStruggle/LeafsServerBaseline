"""build-lava-pockets.py - generate self-contained "lava pocket" caldera objects.

Like build-magmaspires.py (and akin to the tree-gen generators), this authors the
objects procedurally as diffable JSON sources and compiles them to .iob via
json_to_iob.py, so the binary stays a regenerable build output.

Each pocket is an OBSIDIAN BOWL with a flat lava SOURCE pool inside it. The bowl
floor and sides are solid obsidian; the lava surface sits one block BELOW the
obsidian rim, so the lava is fully contained ("obsidian ends") and cannot flow out
no matter the surrounding terrain. These are placed by a bored jigsaw piece
(placementOptions.bore=true) so the bowl embeds into the surface of the rare
Frostpeak Caldera biome.

Usage:
  python build-lava-pockets.py            # write sources + compile .iob
  python build-lava-pockets.py --sources  # only (re)write the JSON sources
"""
import argparse
import json
import math
import os

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
SRC_ROOT = os.path.join(REPO, "object-src", "jigsaw", "clutter")
OUT_ROOT = os.path.join(REPO, "pack-overlay", "objects", "jigsaw", "clutter")

# (name, radius) - radius is the top-opening radius; bowl depth ~= radius.
VARIANTS = [
    ("lavapocket1", 6),
    ("lavapocket2", 9),
    ("lavapocket3", 4),
]


def build_pocket(radius):
    """Return centered {x,y,z,state} blocks for an obsidian bowl holding a lava pool.

    Coordinate frame (centered): x/z in [-radius..radius], y=0 is the deepest point
    of the bowl, y increases upward, the obsidian rim is at y=radius.
    """
    blocks = []
    rim_y = radius
    lava_top = radius - 1  # one below the rim, so lava never reaches the lip
    for x in range(-radius, radius + 1):
        for z in range(-radius, radius + 1):
            r = math.sqrt(x * x + z * z)
            if r > radius + 0.5:
                continue
            # hemispherical bowl: floor is deepest at the centre, rises to the rim.
            floor_y = int(round(radius - math.sqrt(max(0.0, radius * radius - r * r))))
            # solid obsidian shell: the floor block plus one block of thickness below.
            blocks.append({"x": x, "y": floor_y, "z": z, "state": "minecraft:obsidian"})
            if floor_y - 1 >= 0:
                blocks.append({"x": x, "y": floor_y - 1, "z": z, "state": "minecraft:obsidian"})
            # lava fill above the floor, up to lava_top, only where there is room.
            for y in range(floor_y + 1, lava_top + 1):
                blocks.append({"x": x, "y": y, "z": z, "state": "minecraft:lava[level=0]"})
            # obsidian rim ring around the very top edge for clean "obsidian ends".
            if r > radius - 1.0:
                blocks.append({"x": x, "y": rim_y, "z": z, "state": "minecraft:obsidian"})
    return blocks


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sources", action="store_true",
                    help="only (re)write JSON sources, do not compile")
    args = ap.parse_args()

    os.makedirs(SRC_ROOT, exist_ok=True)
    sources = []
    for name, radius in VARIANTS:
        span = 2 * radius + 1
        height = radius + 1
        src = {
            "size": {"w": span, "h": height, "d": span},
            "centered": True,
            "blocks": build_pocket(radius),
        }
        src_path = os.path.join(SRC_ROOT, name + ".json")
        with open(src_path, "w", encoding="utf-8") as f:
            json.dump(src, f, indent=2)
            f.write("\n")
        sources.append((name, src_path))
        print(f"  source: object-src/jigsaw/clutter/{name}.json "
              f"({span}x{height}x{span}, {len(src['blocks'])} blocks)")

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
