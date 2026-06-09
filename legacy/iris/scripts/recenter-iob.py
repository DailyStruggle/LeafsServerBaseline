"""Re-center existing Iris V2 IOB files so block coords are relative to the object
center (w//2, h//2, d//2), matching stock Iris objects. Older generated files used a
0-based origin which made trees float ~h/2 above ground. Idempotent: files already
centered (any min coord < 0) are skipped.

Usage: python scripts/recenter-iob.py <root-dir>
"""
import struct, sys, os, io


def _utf(buf, s):
    e = s.encode("utf-8")
    buf.write(struct.pack(">H", len(e)))
    buf.write(e)


def recenter(path):
    data = open(path, "rb").read()
    o = 0
    w, h, d = struct.unpack_from(">iii", data, o); o += 12
    sl = struct.unpack_from(">H", data, o)[0]; o += 2
    magic = data[o:o + sl].decode("utf-8"); o += sl
    ps = struct.unpack_from(">h", data, o)[0]; o += 2
    palette = []
    for _ in range(ps):
        l = struct.unpack_from(">H", data, o)[0]; o += 2
        palette.append(data[o:o + l].decode("utf-8")); o += l
    bc = struct.unpack_from(">i", data, o)[0]; o += 4
    blocks = []
    for _ in range(bc):
        x, y, z, pi = struct.unpack_from(">hhhh", data, o); o += 8
        blocks.append((x, y, z, pi))
    states = struct.unpack_from(">i", data, o)[0]

    if not blocks:
        return "empty"
    minx = min(b[0] for b in blocks); miny = min(b[1] for b in blocks); minz = min(b[2] for b in blocks)
    if minx < 0 or miny < 0 or minz < 0:
        return "already-centered"

    cx, cy, cz = w // 2, h // 2, d // 2
    buf = io.BytesIO()
    buf.write(struct.pack(">iii", w, h, d))
    _utf(buf, magic)
    buf.write(struct.pack(">h", len(palette)))
    for s in palette:
        _utf(buf, s)
    buf.write(struct.pack(">i", len(blocks)))
    for (x, y, z, pi) in blocks:
        buf.write(struct.pack(">hhhh", x - cx, y - cy, z - cz, pi))
    buf.write(struct.pack(">i", states))
    open(path, "wb").write(buf.getvalue())
    return "recentered"


root = sys.argv[1]
counts = {}
for dp, _, fns in os.walk(root):
    for fn in fns:
        if fn.lower().endswith(".iob"):
            r = recenter(os.path.join(dp, fn))
            counts[r] = counts.get(r, 0) + 1
print(counts)
