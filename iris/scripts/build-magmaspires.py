"""build-magmaspires.py - generate the basalt "magmaspire" clutter objects.

These are the natural basalt spire pillars used by the volcano-family biomes
(Frostpeak, Cinderfall, Ashcrown, Embertide, Cinnabar Mesa). They are authored
procedurally as diffable JSON object sources (object-src/clutter/*.json) and then
compiled to .iob via json_to_iob.py so the binary stays a regenerable build output.

Each spire is a tapering basalt cone with a blackstone foot and a magma core that
glows from the base, matching the "basalt spire formations as natural pillars"
description in docs/design/CUSTOM-BIOMES.md.

Usage:
  python build-magmaspires.py            # write sources + compile .iob
  python build-magmaspires.py --sources  # only (re)write the JSON sources
"""
import argparse
import json
import os

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
SRC_ROOT = os.path.join(REPO, "object-src", "clutter")
OUT_ROOT = os.path.join(REPO, "pack-overlay", "objects", "clutter")

# (name, height, base_radius)
VARIANTS = [
    ("magmaspire1", 14, 3),
    ("magmaspire2", 10, 2),
    ("magmaspire3", 18, 4),
]


def build_spire(height, base_radius):
    """Return a list of {x,y,z,state} block dicts, centered on x/z, base at y=0."""
    blocks = []
    for y in range(height):
        # radius tapers from base_radius at the foot to ~0 at the tip
        frac = 1.0 - (y / max(1, height - 1))
        radius = base_radius * frac
        r_int = int(round(radius))
        for x in range(-r_int, r_int + 1):
            for z in range(-r_int, r_int + 1):
                if (x * x + z * z) > (radius + 0.35) ** 2:
                    continue
                if y == 0:
                    state = "minecraft:blackstone"
                elif x == 0 and z == 0 and y < height // 2:
                    state = "minecraft:magma_block"
                else:
                    state = "minecraft:basalt[axis=y]"
                blocks.append({"x": x, "y": y, "z": z, "state": state})
    return blocks


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sources", action="store_true",
                    help="only (re)write JSON sources, do not compile")
    args = ap.parse_args()

    os.makedirs(SRC_ROOT, exist_ok=True)
    sources = []
    for name, height, base_radius in VARIANTS:
        span = 2 * base_radius + 1
        src = {
            "size": {"w": span, "h": height, "d": span},
            "centered": True,
            "blocks": build_spire(height, base_radius),
        }
        src_path = os.path.join(SRC_ROOT, name + ".json")
        with open(src_path, "w", encoding="utf-8") as f:
            json.dump(src, f, indent=2)
            f.write("\n")
        sources.append((name, src_path))
        print(f"  source: object-src/clutter/{name}.json "
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
