"""
nbt_to_iris.py - convert vanilla structure-block .nbt pieces into Iris jigsaw
assets (a V2 ".iob" object + a jigsaw-piece JSON whose connectors are derived
from the minecraft:jigsaw blocks inside the piece). It also rewrites the vanilla
template_pool / jigsaw-structure JSON into the Iris equivalents.

This is our own tooling (dependency-free, stdlib only) - it does NOT embed any
third-party asset, so it is safe to track in git. The assets it *produces* from a
restricted source pack must stay under the git-ignored quarantine
(iris/thirdparty-derived/), per ADR-004.

Coordinate convention (verified against existing Iris tree objects):
  stored = original - (w//2, h//2, d//2)   for BOTH blocks and connectors.
Using one consistent centering for blocks and connectors preserves the relative
geometry Iris needs to align connecting pieces.

Tile entities (chests, signs, the jigsaw blocks themselves) are NOT written into
the .iob states table: that needs Iris' TileData binary format. Jigsaw blocks are
metadata (their connector info is lifted into the piece JSON) and are dropped from
the object; container loot is handled separately by Iris and is out of scope here.

Air handling (--keep-air):
  Iris never PASTES air from an object (IrisObject.place skips AIR/CAVE_AIR), so
  for surface pieces air is normally dropped. But Iris DOES use the stored blocks
  to define an object's bounding box: IrisObject.shrinkwrap() recomputes
  width/height/depth AND the center from the solid-block AABB. If air is dropped,
  the object shrinks to its solid extent, which (a) makes a `bore` placement
  under-carve the real footprint and (b) on rotation re-centers the blocks away
  from the connector positions (which were centered on the FULL piece size here).
  For carved structures that rely on `bore` to hollow themselves out (e.g.
  mineshafts), pass --keep-air so the air cells are stored: they are still not
  pasted, but they preserve the full footprint/centering so bore carves the
  whole tunnel and connecting pieces overlap. Leave it off for surface villages.

Usage:
  python nbt_to_iris.py --raw <raw_dir> --out <out_dir> \
      --namespace kaisyn --set village/meadow_swiss \
      --structure-json <village_meadow.json> --structure-key village-meadow
      [--keep-air]
"""
import argparse
import gzip
import json
import os
import struct
import sys

from iob_lib import write_iob

# --- NBT tag ids -----------------------------------------------------------
TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG, TAG_FLOAT, TAG_DOUBLE, \
    TAG_BYTE_ARRAY, TAG_STRING, TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY, \
    TAG_LONG_ARRAY = range(13)

# Always dropped: structure_void is "leave existing terrain" (not a real block)
# and jigsaw blocks are metadata lifted into the piece JSON. Air is dropped too
# UNLESS --keep-air is set (see module docstring): air is never pasted by Iris
# but, when stored, it preserves the object's full bounding box / center so a
# `bore` placement carves the whole footprint and pieces overlap.
ALWAYS_SKIP_BLOCKS = {"minecraft:structure_void", "minecraft:jigsaw"}
AIR_BLOCK = "minecraft:air"

# vanilla jigsaw "orientation" front-facing -> Iris connector direction
DIR_MAP = {
    "north": "NORTH_NEGATIVE_Z",
    "south": "SOUTH_POSITIVE_Z",
    "east": "EAST_POSITIVE_X",
    "west": "WEST_NEGATIVE_X",
    "up": "UP_POSITIVE_Y",
    "down": "DOWN_NEGATIVE_Y",
}


class NBTReader:
    def __init__(self, data):
        self.d = data
        self.o = 0

    def u1(self):
        v = self.d[self.o]; self.o += 1; return v

    def i2(self):
        v = struct.unpack_from(">h", self.d, self.o)[0]; self.o += 2; return v

    def i4(self):
        v = struct.unpack_from(">i", self.d, self.o)[0]; self.o += 4; return v

    def i8(self):
        v = struct.unpack_from(">q", self.d, self.o)[0]; self.o += 8; return v

    def f4(self):
        v = struct.unpack_from(">f", self.d, self.o)[0]; self.o += 4; return v

    def f8(self):
        v = struct.unpack_from(">d", self.d, self.o)[0]; self.o += 8; return v

    def string(self):
        ln = struct.unpack_from(">H", self.d, self.o)[0]; self.o += 2
        s = self.d[self.o:self.o + ln].decode("utf-8"); self.o += ln; return s

    def payload(self, tag):
        if tag == TAG_BYTE:
            return self.u1()
        if tag == TAG_SHORT:
            return self.i2()
        if tag == TAG_INT:
            return self.i4()
        if tag == TAG_LONG:
            return self.i8()
        if tag == TAG_FLOAT:
            return self.f4()
        if tag == TAG_DOUBLE:
            return self.f8()
        if tag == TAG_BYTE_ARRAY:
            n = self.i4(); v = self.d[self.o:self.o + n]; self.o += n; return list(v)
        if tag == TAG_STRING:
            return self.string()
        if tag == TAG_LIST:
            item = self.u1(); n = self.i4()
            return [self.payload(item) for _ in range(n)]
        if tag == TAG_COMPOUND:
            out = {}
            while True:
                t = self.u1()
                if t == TAG_END:
                    break
                name = self.string()
                out[name] = self.payload(t)
            return out
        if tag == TAG_INT_ARRAY:
            n = self.i4(); return [self.i4() for _ in range(n)]
        if tag == TAG_LONG_ARRAY:
            n = self.i4(); return [self.i8() for _ in range(n)]
        raise ValueError(f"unknown tag {tag}")

    def root(self):
        t = self.u1()
        assert t == TAG_COMPOUND, f"root not compound: {t}"
        self.string()  # root name
        return self.payload(TAG_COMPOUND)


def load_nbt(path):
    with open(path, "rb") as f:
        raw = f.read()
    if raw[:2] == b"\x1f\x8b":
        raw = gzip.decompress(raw)
    return NBTReader(raw).root()


def block_string(entry):
    """Build 'minecraft:foo[a=b,c=d]' from a palette entry compound."""
    name = entry["Name"]
    props = entry.get("Properties")
    if not props:
        return name
    inner = ",".join(f"{k}={v}" for k, v in props.items())
    return f"{name}[{inner}]"


# --- IOB V2 writer ---------------------------------------------------------
# The binary writer lives in iob_lib.write_iob (shared with json_to_iob.py).
# nbt_to_iris emits geometry only, so it calls write_iob with no states.


def iris_pool_path(set_prefix, vanilla_pool):
    """'kaisyn:village/meadow_swiss/streets' -> 'village/meadow_swiss/streets'."""
    if vanilla_pool in ("minecraft:empty", "", None):
        return None
    return vanilla_pool.split(":", 1)[-1]


def convert_piece(nbt_path, object_key, set_prefix, keep_air=False):
    """Return (w,h,d, blocks, connectors) for a single piece."""
    nbt = load_nbt(nbt_path)
    sx, sy, sz = nbt["size"]
    cx, cy, cz = sx // 2, sy // 2, sz // 2
    palette = nbt["palette"]
    blocks = []
    connectors = []
    for b in nbt["blocks"]:
        px, py, pz = b["pos"]
        x, y, z = px - cx, py - cy, pz - cz
        entry = palette[b["state"]]
        name = entry["Name"]
        if name == "minecraft:jigsaw":
            j = b.get("nbt", {})
            orientation = (entry.get("Properties", {}) or {}).get(
                "orientation", "north_up")
            front = orientation.split("_", 1)[0]
            pool = iris_pool_path(set_prefix, j.get("pool"))
            con = {
                "targetName": j.get("target", "minecraft:empty"),
                "innerConnector": False,
                "rotateConnector": False,
                "name": j.get("name", "minecraft:empty"),
                "position": {"x": x, "y": y, "z": z},
                "direction": DIR_MAP.get(front, "NORTH_NEGATIVE_Z"),
            }
            if pool:
                con["pools"] = [pool]
            connectors.append(con)
            continue
        if name in ALWAYS_SKIP_BLOCKS:
            continue
        if name == AIR_BLOCK and not keep_air:
            continue
        blocks.append((x, y, z, block_string(entry)))
    return sx, sy, sz, blocks, connectors


PLACEMENT_OPTIONS = {
    "overStilt": 0,
    "chance": 1,
    "meld": False,
    "density": 1,
    "bottom": False,
    "translateCenter": False,
    "rotation": {
        "yAxis": {"min": 0, "max": 270, "interval": 90, "enabled": True},
        "xAxis": {"min": 0, "max": 0, "interval": 0, "enabled": False},
        "zAxis": {"min": 0, "max": 0, "interval": 0, "enabled": False},
        "enabled": True,
    },
    "boreExtendMinY": -1,
    "smartBore": False,
    "waterloggable": False,
    "bore": False,
    "mode": "PAINT",
    "carvingSupport": "SURFACE_ONLY",
    "snow": 0,
    "underwater": False,
    "boreExtendMaxY": 0,
    "onwater": False,
}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=4)
        f.write("\n")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--raw", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--namespace", required=True)
    ap.add_argument("--set", dest="set_prefix", required=True,
                    help="e.g. village/meadow_swiss")
    ap.add_argument("--structure-json", required=True)
    ap.add_argument("--structure-key", required=True)
    ap.add_argument("--keep-air", action="store_true",
                    help="store minecraft:air cells in the .iob (not pasted by "
                         "Iris, but preserves footprint/center so `bore` carves "
                         "the full piece - needed for mineshafts/cave structures)")
    args = ap.parse_args()

    struct_root = os.path.join(args.raw, "data", args.namespace, "structure")
    set_dir = os.path.join(struct_root, *args.set_prefix.split("/"))
    if not os.path.isdir(set_dir):
        sys.exit(f"structure dir not found: {set_dir}")

    obj_root = os.path.join(args.out, "objects", "jigsaw")
    piece_root = os.path.join(args.out, "jigsaw-pieces")

    converted = 0
    total_connectors = 0
    for dirpath, _, files in os.walk(set_dir):
        for fn in files:
            if not fn.endswith(".nbt"):
                continue
            nbt_path = os.path.join(dirpath, fn)
            rel = os.path.relpath(nbt_path, struct_root).replace("\\", "/")
            key = rel[:-4]  # strip .nbt -> e.g. village/meadow_swiss/streets/corner_01
            object_key = "jigsaw/" + key
            w, h, d, blocks, connectors = convert_piece(
                nbt_path, object_key, args.set_prefix, keep_air=args.keep_air)
            npal = write_iob(os.path.join(obj_root, key + ".iob"),
                             w, h, d, blocks)
            piece = {
                "connectors": connectors,
                "placementOptions": PLACEMENT_OPTIONS,
                "object": object_key,
            }
            write_json(os.path.join(piece_root, key + ".json"), piece)
            converted += 1
            total_connectors += len(connectors)
            print(f"  {key}: {w}x{h}x{d} blocks={len(blocks)} "
                  f"palette={npal} connectors={len(connectors)}")

    # --- pools: vanilla template_pool/*.json -> iris jigsaw-pools/*.json ---
    pool_src = os.path.join(args.raw, "data", args.namespace,
                            "worldgen", "template_pool", *args.set_prefix.split("/"))
    pool_out_root = os.path.join(args.out, "jigsaw-pools")
    pools_written = 0
    if os.path.isdir(pool_src):
        # Walk RECURSIVELY: T&T nests pools in subfolders (e.g.
        # beach_lighthouse/side/house.json). A flat listdir misses them, which
        # leaves the start piece's connectors pointing at unconverted pools and
        # produces a village with only its town center.
        for dirpath, _, files in os.walk(pool_src):
            for fn in files:
                if not fn.endswith(".json"):
                    continue
                with open(os.path.join(dirpath, fn), encoding="utf-8") as f:
                    vp = json.load(f)
                pieces = []
                for el in vp.get("elements", []):
                    loc = el.get("element", {}).get("location")
                    if loc:
                        # pool/structure piece refs are jigsaw-piece KEYS (no
                        # "jigsaw/" prefix); only piece .object is prefixed.
                        pieces.append(loc.split(":", 1)[-1])
                sub = os.path.relpath(os.path.join(dirpath, fn), pool_src)
                out_key = args.set_prefix + "/" + sub[:-5].replace("\\", "/")
                write_json(os.path.join(pool_out_root, out_key + ".json"),
                           {"pieces": pieces})
                pools_written += 1

    # --- structure: vanilla jigsaw structure -> iris 4.0 IrisStructure -----
    # Iris 4.0 loads the flat `structures/` folder (IrisStructure), NOT the 3.x
    # `jigsaw-structures/` folder. An IrisStructure seeds assembly from a single
    # `startPool` (a jigsaw POOL key), so we carry the vanilla `start_pool`
    # through directly (namespace stripped) instead of resolving it to pieces.
    # See docs/design/IRIS-V4-STRUCTURES.md.
    with open(args.structure_json, encoding="utf-8") as f:
        vs = json.load(f)
    start_pool = vs.get("start_pool", "")
    # vanilla start_pool is "<ns>:<set>/town_centers" (or .../base_plate); the
    # Iris pool key is the namespace-stripped path, matching our converted pools.
    start_pool_key = start_pool.split(":", 1)[-1]
    iris_structure = {
        "startPool": start_pool_key,
        "maxDepth": vs.get("size", 6),
        "maxSizeChunks": 8,
        # STRUCTURE_PIECE: each jigsaw piece anchors independently, mirroring the
        # 4.0 base `structures/minecraft_village_plains.json`.
        "placeMode": "STRUCTURE_PIECE",
    }
    write_json(os.path.join(args.out, "structures",
                            args.structure_key + ".json"), iris_structure)

    print(f"\nconverted pieces : {converted}")
    print(f"total connectors : {total_connectors}")
    print(f"pools written    : {pools_written}")
    print(f"structure written: {args.structure_key}.json "
          f"(startPool: {start_pool_key})")


if __name__ == "__main__":
    main()
