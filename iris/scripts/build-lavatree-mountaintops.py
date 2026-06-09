"""build-lavatree-mountaintops.py - generate self-contained "lava-tree mountaintop" objects.

DEPRECATED: implicitly superseded by the Iris 4.0 cave-carving method
(docs/design/IRIS-V4-CARVING.md). The baked obsidian-encased lava-tree objects this
script bakes were a workaround to fake carved lava pockets/volcanoes under Iris 3.9;
the Iris 4.0 noise-field carver (surface-breaking lava conduits via caveProfile +
allowLava + a dimension carving[] band) produces real carved lava terrain instead.
This script and its lavatree*/lavatree-mt-* outputs are kept in-repo, unreferenced,
for history/reference only - do not wire them into new biomes.

Each object is the validated inverted, obsidian-encased lava tree (see
docs/scratch/FROSTPEAK-LAVATREE-CHECKPOINT.md) WRAPPED in a noise-shaped mountaintop
cap, so the whole summit (buried lava mass + flat lava pool + open crater + snowy/icy
mountain dome) is baked into one .iob. This sidesteps the Iris v3.9 per-biome shaping
limits (continuation-plan item 3): the object IS the summit, so the caldera biome no
longer has to fight the world generator for width/height.

Design notes:
  * The lava tree itself is produced by reusing tree-gen's generate_tree() as a library
    (same params as the live "lavatreebig" entry). It bakes:
      - invert_y          -> the lava mass drives DOWN below y=0 (buried),
      - encase/open_top   -> a single FLAT lava pool at the mouth (~y=0),
      - clear_cone (void_air) -> an open conic crater rising above the pool.
  * The mountaintop dome is added on top (y >= 0). Its shape and its SURFACE block
    patches are both driven by LAYERED value noise (fBm), NOT per-block random rolls,
    so patches of snow / packed-ice / stone / obsidian come out as coherent blobs of
    varying size rather than salt-and-pepper noise.
  * The dome only fills cells that are still empty, so it never overwrites the obsidian
    shell, the lava pool, or the void_air crater. It also explicitly skips an expanding
    crater column so the pool/crater always stays open regardless of noise.

Filenames sanitize '/' (tree-gen limitation), so we emit bare names into
pack-overlay/objects/clutter (point Iris at clutter/lavatree-mt-<n>).

Usage (run from iris/scripts):
  python build-lavatree-mountaintops.py            # generate all Frostpeak variants
  python build-lavatree-mountaintops.py --family frostpeak --count 8
"""
import argparse
import math
import os
import random
import sys

_HERE = os.path.dirname(os.path.abspath(__file__))
_TREE_GEN = os.path.abspath(os.path.join(_HERE, "..", "tree-gen"))
sys.path.insert(0, _TREE_GEN)

from generate_tree import generate_tree  # noqa: E402  (path injected above)
from nbt import write_iob                # noqa: E402

OUT_ROOT = os.path.abspath(os.path.join(_HERE, "..", "pack-overlay", "objects", "clutter"))

# Visible mountain height above the belly (crater sits this far above the widest ring).
# CONSTANT across variants so a single biome clamp lines every pool up at one world Y.
# The biome buries the belly ~1/4 of this below ground to blend the base into the land
# (see frostpeak-lava.json clamp/basin tuning).
MOUNTAIN_HEIGHT = 48

# Palettes per volcano family. Each is (surface band list, fill block). The surface
# band list is ordered low->high noise value; a layered-noise field is bucketed into
# these bands so each band paints coherent patches of varying size. The same script
# produces the other volcano families just by swapping the palette (continuation-plan
# item 5: Cinderfall / Ashcrown / Embertide / Cinnabar Mesa).
PALETTES = {
    "frostpeak": {
        # Ice theme: the natural snow/ice/rock surface with colored terracotta mixed in
        # ADDITIVELY (low->high noise), deep blue rock up to icy white. Interior fill stays stone.
        "surface": [
            "minecraft:blue_terracotta",
            "minecraft:blackstone",
            "minecraft:cyan_terracotta",
            "minecraft:stone",
            "minecraft:light_blue_terracotta",
            "minecraft:packed_ice",
            "minecraft:white_terracotta",
            "minecraft:snow_block",
        ],
        "fill": "minecraft:stone",
    },
    "cinderfall": {
        "surface": [
            "minecraft:obsidian",
            "minecraft:blackstone",
            "minecraft:basalt[axis=y]",
            "minecraft:magma_block",
            "minecraft:netherrack",
        ],
        "fill": "minecraft:basalt[axis=y]",
    },
    "ashcrown": {
        "surface": [
            "minecraft:blackstone",
            "minecraft:gray_concrete_powder",
            "minecraft:stone",
            "minecraft:andesite",
            "minecraft:tuff",
        ],
        "fill": "minecraft:stone",
    },
    "embertide": {
        "surface": [
            "minecraft:obsidian",
            "minecraft:blackstone",
            "minecraft:magma_block",
            "minecraft:basalt[axis=y]",
            "minecraft:smooth_basalt",
        ],
        "fill": "minecraft:smooth_basalt",
    },
    "cinnabar_mesa": {
        "surface": [
            "minecraft:terracotta",
            "minecraft:red_terracotta",
            "minecraft:red_sandstone",
            "minecraft:granite",
            "minecraft:stone",
        ],
        "fill": "minecraft:red_sandstone",
    },
}


def _lava_tree_entry(seed, trunk_width, height_min, height_max):
    """Build a tree-gen entry dict equivalent to the live 'lavatreebig' (parameterised)."""
    return {
        "name": "lavatree-mt",
        "trunk": "minecraft:lava[level=0]",
        "leaves": "minecraft:lava[level=0]",
        "profile": "oak",
        "height_min": height_min,
        "height_max": height_max,
        "seed": seed,
        "count": 1,
        "trunk_width": trunk_width,
        "trunk_shape": "sigmoid",
        "trunk_shape_params": {"steepness": 4.0},
        "trunk_round": True,
        "invert_y": True,
        "roots": True,
        "root_block": "minecraft:void_air",
        "encase": "minecraft:obsidian",
        "encase_targets": ["minecraft:lava"],
        "encase_open_top": True,
        "encase_top_variation": 4,
        "clear_cone_height": 16,
        "clear_cone_expand": 2.2,
        "canopy": {
            "mode": "density",
            "leaf_density": 1.0,
            "branches": {
                "prob_fn": "constant",
                "prob_params": {"p": 1.0},
                "length_fn": "linear",
                "length_params": {"base": 8, "crown": 20},
                "azimuth": "random",
                "elevation": 40,
                "leaf_start_up": False,
                "cluster_radius": 5,
                "cluster_mode": "density",
                "cluster_density": 1.0,
                "sub_branches": {
                    "count": 4,
                    "pitch_delta": 8,
                    "yaw_delta": 40,
                    "length_scale": 0.8,
                    "cluster_radius": 2,
                    "cluster_mode": "density",
                    "cluster_density": 1.0,
                },
            },
        },
    }


# --------------------------------------------------------------------------------------
# Layered value noise (fBm). Pure-python, seeded, deterministic. No numpy dependency.
# --------------------------------------------------------------------------------------
def _hash01(ix, iz, seed):
    """Deterministic pseudo-random value in [0,1) for an integer lattice point."""
    h = (ix * 374761393 + iz * 668265263 + seed * 1442695040888963407) & 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    h ^= (h >> 16)
    return (h & 0xFFFFFFFF) / 4294967296.0


def _smooth(t):
    return t * t * (3.0 - 2.0 * t)


def _value_noise(x, z, seed):
    """Bilinearly-interpolated value noise in [0,1) at fractional (x,z)."""
    ix, iz = math.floor(x), math.floor(z)
    fx, fz = x - ix, z - iz
    v00 = _hash01(ix, iz, seed)
    v10 = _hash01(ix + 1, iz, seed)
    v01 = _hash01(ix, iz + 1, seed)
    v11 = _hash01(ix + 1, iz + 1, seed)
    sx, sz = _smooth(fx), _smooth(fz)
    a = v00 + (v10 - v00) * sx
    b = v01 + (v11 - v01) * sx
    return a + (b - a) * sz


def _fbm(x, z, seed, octaves=4, base_freq=0.05, lacunarity=2.0, gain=0.5):
    """Layered fractional Brownian motion in ~[0,1]. Lower base_freq = bigger patches."""
    total = 0.0
    amp = 1.0
    freq = base_freq
    norm = 0.0
    for o in range(octaves):
        total += amp * _value_noise(x * freq, z * freq, seed + o * 1013)
        norm += amp
        amp *= gain
        freq *= lacunarity
    return total / norm if norm else 0.0


# --------------------------------------------------------------------------------------
def _crater_geometry(blocks):
    """Find the lava pool mouth: its top Y, centroid, and radius (to keep the crater open)."""
    lava = [(x, y, z) for (x, y, z), s in blocks.items()
            if s.split("[")[0] == "minecraft:lava"]
    if not lava:
        return 0, 0.0, 0.0, 3.0
    top_y = max(y for (x, y, z) in lava)
    mouth = [(x, z) for (x, y, z) in lava if y == top_y]
    cx = sum(p[0] for p in mouth) / len(mouth)
    cz = sum(p[1] for p in mouth) / len(mouth)
    r = max(math.hypot(px - cx, pz - cz) for (px, pz) in mouth) + 1.0
    return top_y, cx, cz, r


_NEIGHBORS6 = ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))


def _add_mountaintop(blocks, seed, dome_radius, mountain_height, palette):
    """Wrap the lava tree in a SOLID volcano whose CRATER sits near the SUMMIT.

    Profile (a proper volcano silhouette):
      * The lava pool/crater is near the TOP of the mountain.
      * The cone is widest at a "belly" sitting ``mountain_height`` below the pool, so
        the mountain rises from the land up to the crater. ``mountain_height`` is held
        CONSTANT across all variants so a single biome clamp lines every pool up.
      * Above the pool a short crater lip; the void_air clear-cone keeps the crater open.
      * Below the belly the cone tapers to a point at the bottom of the buried lava
        tree, so there is no flat floating underside and it blends into the land.
    Filled SOLID (no mid-air slabs), then skinned via an exposed-face pass whose blocks
    come from a layered-noise field (coherent snow/ice/stone/obsidian patches).

    Mutates and returns the blocks dict.
    """
    occupied = set(blocks.keys())
    top_y, cx, cz, mouth_r = _crater_geometry(blocks)
    bottom_y = min(y for (x, y, z) in blocks)  # bottom of the buried lava mass

    surface_bands = palette["surface"]
    fill_block = palette["fill"]
    nbands = len(surface_bands)

    rng = random.Random(seed ^ 0x5151)
    shape_seed = rng.randint(0, 1 << 30)
    surf_seed = rng.randint(0, 1 << 30)

    R = float(dome_radius)
    H_m = float(mountain_height)
    rim_height = 6                       # short crater lip above the pool
    crater_rim_r = mouth_r + 4.0         # cone radius at the summit crater
    belly_y = top_y - H_m                # widest ring (aligns with the land)
    rim_top = top_y + rim_height
    y_apex = rim_top
    flank_pow = 1.0                      # straight conical flank (belly -> crater)
    down_pow = 1.4                       # sharper taper below the belly

    # Per-Y radial extent of the buried lava/tree mass, so the stone shell is guaranteed
    # to wrap the lava ALL THE WAY DOWN to its lowest block (no exposed lava below the
    # belly even where the taper would otherwise narrow inside the lava column).
    shell_margin = 3.0
    occ_radius = {}
    for (ox, oy, oz) in occupied:
        rr = math.hypot(ox - cx, oz - cz)
        if rr > occ_radius.get(oy, 0.0):
            occ_radius[oy] = rr

    def shell_radius(y):
        """Min radius needed to encase the lava/tree at height y (0 if none)."""
        if y >= top_y:
            return 0.0
        rr = occ_radius.get(y, 0.0)
        return rr + shell_margin if rr > 0.0 else 0.0

    def env_radius(y):
        """Max cone radius at world-relative height y (0 outside the cone)."""
        if y > rim_top:
            return 0.0
        if y >= top_y:                                    # ROUNDED crater lip above the pool
            # Half-ellipse profile so the rim domes over smoothly a little above the
            # lava (rounded crest), instead of a sharp narrowing edge.
            f = (y - top_y) / max(1.0, float(rim_height))
            return crater_rim_r * math.sqrt(max(0.0, 1.0 - f * f))
        if y >= belly_y:                                  # flank: belly -> crater rim
            f = (top_y - y) / max(1.0, H_m)               # 0 at pool -> 1 at belly
            return crater_rim_r + (R - crater_rim_r) * (f ** flank_pow)
        f = (belly_y - y) / max(1.0, (belly_y - bottom_y))  # 0 at belly -> 1 at bottom
        return R * (1.0 - f) ** down_pow                  # taper to a point at the bottom

    icx, icz = int(round(cx)), int(round(cz))
    max_occ = max(occ_radius.values()) if occ_radius else 0.0
    ri = int(math.ceil(max(R, max_occ + shell_margin))) + 2
    solid = {}
    for dx in range(-ri, ri + 1):
        for dz in range(-ri, ri + 1):
            r = math.hypot(dx, dz)
            # One layered-noise wobble per column -> irregular (non-circular) silhouette
            # of varying size, applied consistently up the whole column.
            wob = 0.74 + 0.46 * _fbm(dx, dz, shape_seed, octaves=4, base_freq=0.05)
            x = icx + dx
            z = icz + dz
            for y in range(bottom_y, y_apex + 1):
                env = env_radius(y) * wob
                # Guarantee the stone shell wraps the lava down to its lowest block,
                # even where the noise wobble or taper would otherwise expose it.
                shell = shell_radius(y)
                if shell > env:
                    env = shell
                if env <= 0.0 or r > env:
                    continue
                # Keep an expanding crater open above the pool (matches the void_air cone).
                if y >= top_y:
                    crater_r = mouth_r * (1.0 + 0.14 * (y - top_y))
                    if r < crater_r:
                        continue
                pos = (x, y, z)
                if pos in occupied:
                    continue
                solid[pos] = fill_block

    # Exposed-face skin pass: any solid cell touching air/void becomes a surface block,
    # banded by a layered-noise field so the cone's skin shows coherent patches.
    all_set = occupied | set(solid.keys())
    for (x, y, z) in list(solid.keys()):
        exposed = False
        for ddx, ddy, ddz in _NEIGHBORS6:
            if (x + ddx, y + ddy, z + ddz) not in all_set:
                exposed = True
                break
        if exposed:
            band_val = _fbm(x, z + y * 0.3, surf_seed, octaves=3, base_freq=0.05)
            band = min(nbands - 1, int(band_val * nbands))
            solid[(x, y, z)] = surface_bands[band]

    blocks.update(solid)
    return blocks


def _normalize_crater_to_origin(blocks):
    """Shift the object vertically so the lava CRATER (top lava plane) is at object-y 0.

    Iris pins the schematic origin (y=0) to the placement clamp, so anchoring the crater
    at 0 makes the biome clamp equal the absolute world Y of the crater (predictable,
    matching the original working lava-tree). Returns the shifted blocks dict.
    """
    top_y, _cx, _cz, _r = _crater_geometry(blocks)
    if top_y == 0:
        return blocks
    return {(x, y - top_y, z): s for (x, y, z), s in blocks.items()}


def build_variant(family, index, seed, trunk_width, height, dome_radius, mountain_height):
    """Generate one lava-tree mountaintop and write it to clutter/lavatree-mt[-family]-<n>.iob."""
    palette = PALETTES[family]
    entry = _lava_tree_entry(seed, trunk_width, height, height)
    blocks = generate_tree(entry, height)
    _add_mountaintop(blocks, seed, dome_radius, mountain_height, palette)
    blocks = _normalize_crater_to_origin(blocks)

    suffix = "" if family == "frostpeak" else f"-{family}"
    name = f"lavatree-mt{suffix}-{index}"
    os.makedirs(OUT_ROOT, exist_ok=True)
    out_path = os.path.join(OUT_ROOT, name + ".iob")
    write_iob(out_path, blocks)

    xs = [p[0] for p in blocks]
    ys = [p[1] for p in blocks]
    zs = [p[2] for p in blocks]
    print(f"  {name}.iob  W={max(xs)-min(xs)+1} H={max(ys)-min(ys)+1} "
          f"L={max(zs)-min(zs)+1}  blocks={len(blocks)}  (tw={trunk_width}, h={height})")
    return name


def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--family", default="frostpeak", choices=sorted(PALETTES.keys()),
                    help="volcano family palette to use (default: frostpeak)")
    ap.add_argument("--count", type=int, default=8,
                    help="number of seeded variants to emit (default: 8)")
    ap.add_argument("--base-seed", type=int, default=42000,
                    help="base RNG seed; each variant offsets from it")
    args = ap.parse_args()

    rng = random.Random(args.base_seed)
    print(f"Building {args.count} '{args.family}' lava-tree mountaintop variant(s)...")
    written = []
    for i in range(1, args.count + 1):
        seed = args.base_seed + i * 101
        # Variant character: trunk width 50-70 (per locked design), proportional height.
        trunk_width = rng.randint(50, 70)
        height = rng.randint(56, 80)
        # Cone hugs the structure (radius ~ trunk half-width + a small skirt) so the
        # mountain "fits the size of the main structure it contains".
        dome_radius = trunk_width // 2 + rng.randint(10, 18)
        # Mountain height is CONSTANT across variants so one biome clamp lines every
        # pool up at the same world Y (the crater sits MOUNTAIN_HEIGHT above the belly).
        name = build_variant(args.family, i, seed, trunk_width, height,
                             dome_radius, MOUNTAIN_HEIGHT)
        written.append(name)

    print(f"Done. {len(written)} object(s) written to "
          f"pack-overlay/objects/clutter:")
    for n in written:
        print(f"    clutter/{n}")


if __name__ == "__main__":
    main()
