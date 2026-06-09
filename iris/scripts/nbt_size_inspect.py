"""nbt_size_inspect.py - read-only diagnostic: report the bounding-box size,
block-entry count and (non-air) solid-block count of a vanilla structure-block
.nbt. Dependency-free, stdlib only - it does NOT embed or emit any third-party
asset (it only prints integers), so running it against quarantined source under
iris/thirdparty-derived/ is inspection, not redistribution (ADR-004).

Usage:
  python nbt_size_inspect.py <file.nbt> [<file.nbt> ...]
"""
import gzip
import struct
import sys

TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG, TAG_FLOAT, TAG_DOUBLE, \
    TAG_BYTE_ARRAY, TAG_STRING, TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY, \
    TAG_LONG_ARRAY = range(13)


def _read(data, off, tag):
    if tag == TAG_BYTE:
        return data[off], off + 1
    if tag == TAG_SHORT:
        return struct.unpack_from(">h", data, off)[0], off + 2
    if tag == TAG_INT:
        return struct.unpack_from(">i", data, off)[0], off + 4
    if tag == TAG_LONG:
        return struct.unpack_from(">q", data, off)[0], off + 8
    if tag == TAG_FLOAT:
        return struct.unpack_from(">f", data, off)[0], off + 4
    if tag == TAG_DOUBLE:
        return struct.unpack_from(">d", data, off)[0], off + 8
    if tag == TAG_BYTE_ARRAY:
        n = struct.unpack_from(">i", data, off)[0]; off += 4
        return data[off:off + n], off + n
    if tag == TAG_STRING:
        n = struct.unpack_from(">H", data, off)[0]; off += 2
        return data[off:off + n].decode("utf-8"), off + n
    if tag == TAG_LIST:
        itag = data[off]; off += 1
        n = struct.unpack_from(">i", data, off)[0]; off += 4
        out = []
        for _ in range(n):
            v, off = _read(data, off, itag)
            out.append(v)
        return out, off
    if tag == TAG_COMPOUND:
        out = {}
        while True:
            t = data[off]; off += 1
            if t == TAG_END:
                break
            nl = struct.unpack_from(">H", data, off)[0]; off += 2
            name = data[off:off + nl].decode("utf-8"); off += nl
            v, off = _read(data, off, t)
            out[name] = v
        return out, off
    if tag == TAG_INT_ARRAY:
        n = struct.unpack_from(">i", data, off)[0]; off += 4
        out = list(struct.unpack_from(">%di" % n, data, off))
        return out, off + 4 * n
    if tag == TAG_LONG_ARRAY:
        n = struct.unpack_from(">i", data, off)[0]; off += 4
        out = list(struct.unpack_from(">%dq" % n, data, off))
        return out, off + 8 * n
    raise ValueError("unsupported tag %d at %d" % (tag, off))


def load_nbt(path):
    raw = open(path, "rb").read()
    if raw[:2] == b"\x1f\x8b":
        raw = gzip.decompress(raw)
    off = 0
    t = raw[off]; off += 1
    nl = struct.unpack_from(">H", raw, off)[0]; off += 2 + nl
    val, _ = _read(raw, off, t)
    return val


def inspect(path):
    root = load_nbt(path)
    size = root.get("size", [None, None, None])
    blocks = root.get("blocks", [])
    palette = root.get("palette", [])
    solid = 0
    for b in blocks:
        state = b.get("state")
        name = palette[state].get("Name", "") if isinstance(state, int) and state < len(palette) else ""
        if name not in ("minecraft:air", "minecraft:structure_void", "minecraft:jigsaw"):
            solid += 1
    print("%-70s size=%sx%sx%s entries=%d solid=%d palette=%d" % (
        path.split("\\")[-1], size[0], size[1], size[2],
        len(blocks), solid, len(palette)))


if __name__ == "__main__":
    for p in sys.argv[1:]:
        inspect(p)
