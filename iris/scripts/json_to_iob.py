"""json_to_iob.py - compile a human-readable JSON object source into an Iris V2
".iob" binary object (and, optionally, accompanying tile-entity / TileData state).

This is the hand-authoring counterpart to nbt_to_iris.py: instead of converting a
vanilla .nbt, you describe the object directly in a diffable JSON file. The binary
.iob is treated as a build OUTPUT (regenerable); the JSON source is the thing kept
under review in git.

Both producers share the same binary writer in iob_lib.py, so the byte layout can
never drift between them. See docs/design/IOB-FILE-FORMAT.md and
docs/design/TILEDATA-FORMAT.md.

----------------------------------------------------------------------------
JSON source schema
----------------------------------------------------------------------------
{
  "size":     { "w": 5, "h": 4, "d": 5 },   // bounding box (X, Y, Z)
  "centered": false,                          // default false: coords are 0-based
                                              //   and we apply stored = c - size//2.
                                              //   true: coords are already centered.
  "blocks": [
    { "x": 0, "y": 0, "z": 0, "state": "minecraft:oak_planks" },
    { "x": 1, "y": 0, "z": 0, "state": "minecraft:rail[shape=north_south]" }
  ],

  // optional tile-entity records (TileData). Omit entirely for geometry-only.
  "tiles": [
    // modern payload: a material key + free-form NMS tile NBT rendered as JSON.
    //   the JSON contents are Minecraft-version specific (see TILEDATA-FORMAT.md).
    { "x": 2, "y": 1, "z": 2, "encoding": "modern",
      "material": "minecraft:chest", "properties": { } },

    // legacy spawner: stores a Bukkit EntityType ENUM ORDINAL (version-fragile).
    { "x": 0, "y": 1, "z": 0, "encoding": "legacy", "type": "spawner",
      "entityTypeOrdinal": 20 },

    // legacy sign: 4 lines + DyeColor ordinal.
    { "x": 0, "y": 2, "z": 0, "encoding": "legacy", "type": "sign",
      "lines": ["a", "b", "", ""], "dyeColorOrdinal": 0 },

    // legacy banner: base DyeColor ordinal + (color, pattern) ordinal pairs.
    { "x": 0, "y": 3, "z": 0, "encoding": "legacy", "type": "banner",
      "baseColorOrdinal": 15, "patterns": [[0, 1], [4, 2]] }
  ]
}

Coordinates are integers within the bounding box. With "centered": false the
translator subtracts (w//2, h//2, d//2) from every block and tile coordinate, the
same rule the rest of the pipeline uses so connecting pieces stay aligned.

Usage:
  python json_to_iob.py <source.json> <output.iob>
  python json_to_iob.py --src-root <dir> --out-root <dir>   # batch a tree
"""
import argparse
import json
import os

from iob_lib import (
    write_iob,
    encode_tile_modern,
    encode_tile_legacy_sign,
    encode_tile_legacy_spawner,
    encode_tile_legacy_banner,
)


def _center(coord, size, centered):
    return coord if centered else coord - (size // 2)


def _encode_tile(tile):
    """Return the TileData payload bytes for one 'tiles' entry."""
    enc = tile.get("encoding", "modern")
    if enc == "modern":
        return encode_tile_modern(tile.get("material", ""),
                                  tile.get("properties", {}))
    if enc == "legacy":
        t = tile.get("type")
        if t == "sign":
            return encode_tile_legacy_sign(tile.get("lines", []),
                                           int(tile.get("dyeColorOrdinal", 0)))
        if t == "spawner":
            return encode_tile_legacy_spawner(int(tile["entityTypeOrdinal"]))
        if t == "banner":
            patterns = [(int(c), int(p)) for c, p in tile.get("patterns", [])]
            return encode_tile_legacy_banner(int(tile.get("baseColorOrdinal", 0)),
                                             patterns)
        raise ValueError(f"unknown legacy tile type: {t!r}")
    raise ValueError(f"unknown tile encoding: {enc!r}")


def compile_source(src_path, out_path):
    with open(src_path, encoding="utf-8") as f:
        src = json.load(f)

    size = src["size"]
    w, h, d = int(size["w"]), int(size["h"]), int(size["d"])
    centered = bool(src.get("centered", False))

    blocks = []
    for b in src.get("blocks", []):
        x = _center(int(b["x"]), w, centered)
        y = _center(int(b["y"]), h, centered)
        z = _center(int(b["z"]), d, centered)
        blocks.append((x, y, z, b["state"]))

    states = []
    for t in src.get("tiles", []):
        x = _center(int(t["x"]), w, centered)
        y = _center(int(t["y"]), h, centered)
        z = _center(int(t["z"]), d, centered)
        states.append((x, y, z, _encode_tile(t)))

    npal = write_iob(out_path, w, h, d, blocks, states)
    print(f"  {os.path.basename(out_path)}: {w}x{h}x{d} "
          f"blocks={len(blocks)} palette={npal} tiles={len(states)}")
    return npal


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("src", nargs="?", help="single source .json")
    ap.add_argument("out", nargs="?", help="output .iob path")
    ap.add_argument("--src-root", help="batch: root of .json sources")
    ap.add_argument("--out-root", help="batch: root for .iob outputs")
    args = ap.parse_args()

    if args.src and args.out:
        compile_source(args.src, args.out)
        return

    if args.src_root and args.out_root:
        count = 0
        for dirpath, _, files in os.walk(args.src_root):
            for fn in files:
                if not fn.endswith(".json"):
                    continue
                src_path = os.path.join(dirpath, fn)
                rel = os.path.relpath(src_path, args.src_root)
                out_path = os.path.join(args.out_root, rel[:-5] + ".iob")
                compile_source(src_path, out_path)
                count += 1
        print(f"\ncompiled {count} object(s)")
        return

    ap.error("provide either <src> <out> or --src-root <dir> --out-root <dir>")


if __name__ == "__main__":
    main()
