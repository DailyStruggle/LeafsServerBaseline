"""build-caldera-objects.py - generate irregular deep lava-caldera crater objects.

These replace the jigsaw approach: each caldera is a single predefined object so it
can be placed through a biome `objects` block with a `slopeCondition` (jigsaw
placements cannot slope-gate). Each crater is:

  * Irregular in outline (a few sine harmonics modulate the radius by angle) so it is
    NOT a perfect circle.
  * Tall and deep: a steep obsidian wall rises H blocks above the ground (rim) and the
    pit drops D blocks below it, so the transition from the surrounding biome ground
    into the crater reads as a CLIFF.
  * Flat, level lava: the lava fills the pit to one block below the ground plane, so
    every crater has a single level lava surface, capped by obsidian walls / blackstone
    reaching down.
  * Open bowl: the volume above the lava is authored as VOID_AIR. Iris SKIPS plain air
    and cave_air when placing objects (so they never carve), but it DOES place void_air -
    so void_air is what actually removes terrain to open the crater mouth. Bounded by the
    irregular obsidian rim, it opens the mouth without carving a full cube. bore=false.

Authored as diffable JSON sources compiled to .iob via json_to_iob.py, like the other
iris/scripts/build-*.py generators. Place these with mode=CENTER_HEIGHT so the y=0
ground/rim plane lands on the terrain surface.

Usage:
  python build-caldera-objects.py            # write sources + compile .iob
  python build-caldera-objects.py --sources  # only (re)write the JSON sources
"""
import argparse
import json
import math
import os

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
SRC_ROOT = os.path.join(REPO, "object-src", "clutter")
OUT_ROOT = os.path.join(REPO, "pack-overlay", "objects", "clutter")

# (name, avg_radius R, pit_depth D, rim_height H, seed)
VARIANTS = [
    ("caldera1", 8, 7, 3, 1337),
    ("caldera2", 11, 9, 3, 4242),
    ("caldera3", 6, 6, 2, 9001),
]


def _harmonics(seed):
    """Deterministic per-variant phase offsets for the radius harmonics."""
    rng = random_like(seed)
    return [rng() * math.tau, rng() * math.tau, rng() * math.tau]


def random_like(seed):
    """Tiny deterministic LCG returning floats in [0,1) - no numpy dependency."""
    state = {"s": (seed * 2654435761) & 0xFFFFFFFF}

    def nxt():
        state["s"] = (1103515245 * state["s"] + 12345) & 0x7FFFFFFF
        return state["s"] / 0x7FFFFFFF

    return nxt


def eff_radius(R, theta, phases):
    p1, p2, p3 = phases
    f = 1.0 + 0.22 * math.sin(3 * theta + p1) \
            + 0.14 * math.sin(5 * theta + p2) \
            + 0.10 * math.sin(2 * theta + p3)
    return R * f


AIR_ABOVE = 6            # blocks of open air above the lava surface (the crater mouth)


def build_crater(R, D, H, seed):
    phases = _harmonics(seed)
    r_max = int(math.ceil(R * 1.5)) + 1
    lava_top = -1            # one block below the ground/rim plane (y=0)
    floor = -D               # obsidian pit floor
    blocks = []
    for x in range(-r_max, r_max + 1):
        for z in range(-r_max, r_max + 1):
            r = math.hypot(x, z)
            theta = math.atan2(z, x)
            eff = eff_radius(R, theta, phases)
            if r > eff:
                continue
            near_edge = (eff - r) <= 1.7
            # reach-down base under every interior column
            blocks.append({"x": x, "y": floor - 2, "z": z, "state": "minecraft:blackstone"})
            blocks.append({"x": x, "y": floor - 1, "z": z, "state": "minecraft:blackstone"})
            blocks.append({"x": x, "y": floor, "z": z, "state": "minecraft:obsidian"})
            if near_edge:
                # steep obsidian wall: pit floor up past the ground plane to the rim top
                for y in range(floor + 1, H + 1):
                    blocks.append({"x": x, "y": y, "z": z, "state": "minecraft:obsidian"})
            else:
                # level lava pool, then open air (the crater mouth, bounded by the rim)
                for y in range(floor + 1, lava_top + 1):
                    blocks.append({"x": x, "y": y, "z": z, "state": "minecraft:lava[level=0]"})
                for y in range(lava_top + 1, lava_top + 1 + AIR_ABOVE):
                    blocks.append({"x": x, "y": y, "z": z, "state": "minecraft:void_air"})
    return blocks, r_max


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sources", action="store_true",
                    help="only (re)write JSON sources, do not compile")
    args = ap.parse_args()

    os.makedirs(SRC_ROOT, exist_ok=True)
    sources = []
    for name, R, D, H, seed in VARIANTS:
        blocks, r_max = build_crater(R, D, H, seed)
        span = 2 * r_max + 1
        height = D + AIR_ABOVE + 3
        src = {
            "size": {"w": span, "h": height, "d": span},
            "centered": True,
            "blocks": blocks,
        }
        src_path = os.path.join(SRC_ROOT, name + ".json")
        with open(src_path, "w", encoding="utf-8") as f:
            json.dump(src, f, indent=2)
            f.write("\n")
        sources.append((name, src_path))
        print(f"  source: object-src/clutter/{name}.json "
              f"({span}x{height}x{span}, {len(blocks)} blocks)")

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
