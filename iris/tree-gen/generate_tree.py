#!/usr/bin/env python3
"""CLI entry point. Reads a JSON tree config list and writes .iob and/or .schem files."""

import argparse
import json
import os
import random
import sys

_HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, _HERE)

from nbt import write_schematic, write_iob
from trunk import generate_trunk_with_offsets
from canopy import generate_canopy
from decorators import apply_decorators
from roots import build_roots


def _sanitize(s: str) -> str:
    s = s.split(":")[-1]
    return "".join(c if c.isalnum() or c == "_" else "_" for c in s)


def output_filename(entry: dict, height: int, ext: str = "iob", index: int = 0) -> str:
    """Returns filename for a generated tree file.

    If the entry contains a ``filenames`` list, the element at ``index`` is used
    (with the extension appended).  Otherwise the legacy auto-generated name is
    used: ``<name>_<profile>_<trunk>_<leaves>_h<height>_s<seed>.<ext>``.
    """
    filenames = entry.get("filenames")
    if filenames and index < len(filenames):
        base = _sanitize(filenames[index])
        return "%s.%s" % (base, ext)
    name = _sanitize(entry.get("name", ""))
    profile = _sanitize(entry.get("profile", "oak"))
    trunk = _sanitize(entry.get("trunk", "oak_log"))
    leaves = _sanitize(entry.get("leaves", "oak_leaves"))
    seed = entry.get("seed", 0)
    prefix = "%s_" % name if name else ""
    return "%s%s_%s_%s_h%d_s%d.%s" % (prefix, profile, trunk, leaves, height, seed, ext)


def _heights_for_entry(entry: dict, count_override: int | None) -> list:
    h_min = int(entry.get("height_min", 8))
    h_max = int(entry.get("height_max", 12))
    count = count_override if count_override is not None else int(entry.get("count", 1))
    seed = int(entry.get("seed", 0))

    if count == 1:
        rng = random.Random(seed)
        return [rng.randint(h_min, h_max)]

    if h_min == h_max:
        return [h_min] * count

    rng = random.Random(seed)
    step = (h_max - h_min) / max(count - 1, 1)
    heights = []
    for i in range(count):
        base = h_min + step * i
        jitter = rng.uniform(-step * 0.3, step * 0.3)
        h = int(round(max(h_min, min(h_max, base + jitter))))
        heights.append(h)
    return heights


def generate_tree(entry: dict, height: int) -> dict:
    """All blocks for one tree; returns {(x,y,z): blockstate}."""
    trunk_block = entry.get("trunk", "minecraft:oak_log[axis=y]")
    leaf_block = entry.get("leaves", "minecraft:oak_leaves[distance=1,persistent=true,waterlogged=false]")
    profile = entry.get("profile", "oak")
    seed = int(entry.get("seed", 0))
    trunk_width = int(entry.get("trunk_width", 1))
    trunk_shape = entry.get("trunk_shape", "constant")
    trunk_shape_params = entry.get("trunk_shape_params", {})
    lean_azimuth = float(entry.get("lean_azimuth", 0.0))
    lean_angle = float(entry.get("lean_angle", 0.0))
    curve_fn = entry.get("trunk_curve_fn", "linear")
    curve_params = entry.get("trunk_curve_params", {})
    azimuth_fn = entry.get("lean_azimuth_fn", "constant")
    azimuth_params = entry.get("lean_azimuth_params", {})
    canopy_cfg = entry.get("canopy", {})
    decorator_cfgs = entry.get("decorators", None)
    secondary_leaves_raw = entry.get("secondary_leaves", None)
    secondary_fraction = float(entry.get("secondary_leaf_fraction", 0.35))
    secondary_trunk_raw = entry.get("secondary_trunk", None)
    secondary_trunk_start = float(entry.get("secondary_trunk_start", 0.5))
    secondary_trunk_end = float(entry.get("secondary_trunk_end", 1.0))

    if ":" in leaf_block and "[" not in leaf_block:
        leaf_block = leaf_block + "[distance=1,persistent=true,waterlogged=false]"

    # Normalise secondary_trunk block
    secondary_trunk = None
    if isinstance(secondary_trunk_raw, str) and secondary_trunk_raw:
        st = secondary_trunk_raw
        if "[" not in st:
            st = st + "[axis=y]"
        secondary_trunk = st

    # Normalise secondary_leaves: string stays as-is; list entries get their block
    # field expanded with leaf blockstate if missing.
    secondary_leaves = None
    if isinstance(secondary_leaves_raw, str):
        sl = secondary_leaves_raw
        if ":" in sl and "[" not in sl:
            sl = sl + "[distance=1,persistent=true,waterlogged=false]"
        secondary_leaves = sl
    elif isinstance(secondary_leaves_raw, list):
        expanded = []
        for entry_sl in secondary_leaves_raw:
            blk = entry_sl.get("block", "")
            if ":" in blk and "[" not in blk:
                blk = blk + "[distance=1,persistent=true,waterlogged=false]"
            expanded.append({"block": blk, "weight": entry_sl.get("weight", 1)})
        secondary_leaves = expanded if expanded else None

    blocks, trunk_offsets = generate_trunk_with_offsets(
        height, trunk_block, trunk_width, trunk_shape, trunk_shape_params,
        lean_azimuth, lean_angle, curve_fn, curve_params,
        azimuth_fn, azimuth_params,
        secondary_trunk=secondary_trunk,
        secondary_trunk_start=secondary_trunk_start,
        secondary_trunk_end=secondary_trunk_end
    )
    trunk_positions = set(blocks.keys())
    branch_endpoints = [] if decorator_cfgs else None
    canopy_blocks = generate_canopy(height, trunk_block, leaf_block, profile, canopy_cfg, seed, blocks,
                                    trunk_offsets=trunk_offsets,
                                    secondary_leaves=secondary_leaves,
                                    secondary_fraction=secondary_fraction,
                                    collect_endpoints=branch_endpoints)
    blocks.update(canopy_blocks)
    if decorator_cfgs:
        dec_blocks = apply_decorators(blocks, decorator_cfgs, seed,
                                      branch_endpoints=branch_endpoints,
                                      trunk_positions=trunk_positions)
        blocks.update(dec_blocks)

    # Root system: extend the trunk base downward so the tree always connects to
    # the ground (Iris anchors objects by center; this bridges any residual gap).
    if entry.get("roots", True):
        base_cells = set((x, z) for (x, y, z) in trunk_positions if y == 0)
        if base_cells:
            for pos, blk in build_roots(base_cells, height, trunk_block, seed).items():
                if pos not in blocks:
                    blocks[pos] = blk
    return blocks


def parse_args():
    parser = argparse.ArgumentParser(
        description="Generate Sponge Schematic v3 tree files for Iris."
    )
    parser.add_argument(
        "--config", required=True,
        help="Path to JSON config file containing a flat list of tree definitions."
    )
    parser.add_argument(
        "--out", default=None,
        help="Output directory for output files (overrides per-entry 'out' field). "
             "Defaults to scripts/output/ relative to repo root."
    )
    parser.add_argument(
        "--format", default="iob", choices=["iob", "schem", "both"],
        help="Output format: iob (Iris V2 IOB, default), schem (Sponge Schematic v3), or both."
    )
    parser.add_argument(
        "--count", type=int, default=None,
        help="Number of schematics to generate per entry (overrides per-entry 'count' field)."
    )
    return parser.parse_args()


def resolve_out_dir(cli_out: str | None, config_path: str) -> str:
    if cli_out:
        return os.path.abspath(cli_out)
    config_dir = os.path.dirname(os.path.abspath(config_path))
    return os.path.join(config_dir, "output")


def main():
    args = parse_args()

    with open(args.config, "r", encoding="utf-8") as f:
        entries = json.load(f)

    if not isinstance(entries, list):
        print("ERROR: config file must contain a JSON array of tree definitions.", file=sys.stderr)
        sys.exit(1)

    out_dir = resolve_out_dir(args.out, args.config)
    os.makedirs(out_dir, exist_ok=True)

    fmt = args.format
    total = 0
    grand_total = sum(len(_heights_for_entry(e, args.count)) for e in entries)
    for i, entry in enumerate(entries):
        heights = _heights_for_entry(entry, args.count)
        for idx, height in enumerate(heights):
            blocks = generate_tree(entry, height)
            xs = [p[0] for p in blocks]
            ys = [p[1] for p in blocks]
            zs = [p[2] for p in blocks]
            W = max(xs) - min(xs) + 1
            H = max(ys) - min(ys) + 1
            L = max(zs) - min(zs) + 1

            written = []
            if fmt in ("iob", "both"):
                fname = output_filename(entry, height, "iob", index=idx)
                write_iob(os.path.join(out_dir, fname), blocks)
                written.append(fname)
            if fmt in ("schem", "both"):
                fname = output_filename(entry, height, "schem", index=idx)
                write_schematic(
                    os.path.join(out_dir, fname),
                    blocks,
                    name=fname.replace(".schem", ""),
                    author="tree-gen",
                )
                written.append(fname)

            print("  [%d/%d] %s  W=%d H=%d L=%d  blocks=%d" % (
                total + 1, grand_total,
                written[0], W, H, L, len(blocks)
            ))
            total += 1

    print("Done. %d file(s) written to %s" % (total, out_dir))


if __name__ == "__main__":
    main()
