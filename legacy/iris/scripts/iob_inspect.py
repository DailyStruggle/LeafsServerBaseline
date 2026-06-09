"""Tiny diagnostic: read an Iris V2 .iob and report dimensions, palette size,
block count and the min/max stored coordinates (to confirm the centering scheme).
Dependency-free. Usage: python iob_inspect.py <file.iob>
"""
import struct
import sys


def read(path):
    with open(path, "rb") as f:
        data = f.read()
    off = 0

    def rint():
        nonlocal off
        v = struct.unpack_from(">i", data, off)[0]
        off += 4
        return v

    def rshort():
        nonlocal off
        v = struct.unpack_from(">h", data, off)[0]
        off += 2
        return v

    def rutf():
        nonlocal off
        ln = struct.unpack_from(">H", data, off)[0]
        off += 2
        s = data[off:off + ln].decode("utf-8")
        off += ln
        return s

    w, h, d = rint(), rint(), rint()
    sig = rutf()
    psize = rshort()
    palette = [rutf() for _ in range(psize)]
    bcount = rint()
    xs, ys, zs = [], [], []
    for _ in range(bcount):
        x, y, z, _idx = rshort(), rshort(), rshort(), rshort()
        xs.append(x); ys.append(y); zs.append(z)
    def rbyte():
        nonlocal off
        v = struct.unpack_from(">b", data, off)[0]
        off += 1
        return v

    def read_legacy():
        # int16 type id then a handler-specific body (LegacyTileData).
        tid = rshort()
        if tid == 0:  # sign: 4 UTF lines + int8 dye
            lines = [rutf() for _ in range(4)]
            dye = rbyte()
            return f"type-id=0 sign lines={lines} dye={dye}"
        if tid == 1:  # spawner: int16 EntityType ordinal
            return f"type-id=1 spawner entityOrdinal={rshort()}"
        if tid == 2:  # banner: int8 base + int8 count + count*(int8,int8)
            base = rbyte()
            n = rbyte()
            pats = [(rbyte(), rbyte()) for _ in range(n)]
            return f"type-id=2 banner base={base} patterns={pats}"
        return f"type-id={tid} (unknown)"

    scount = rint()
    states = []
    for _ in range(scount):
        sx, sy, sz = rshort(), rshort(), rshort()
        # Distinguish modern (UTF material key) from legacy (int16 type id) by
        # peeking: modern starts with a uint16-prefixed UTF material key whose
        # first two bytes (0x00 0x01) would otherwise be a legacy type id of 1.
        # Legacy ids are small (0/1/2), so we use a heuristic: a legacy record's
        # first short is in {0,1,2}; anything else is treated as a modern UTF len.
        peek = struct.unpack_from(">h", data, off)[0]
        if peek in (0, 1, 2):
            states.append((sx, sy, sz, read_legacy(), ""))
        else:
            material = rutf()
            props = rutf()
            states.append((sx, sy, sz, f"modern {material}", props[:40]))
    print(f"file={path}")
    print(f"  w,h,d = {w},{h},{d}  signature={sig!r}")
    print(f"  palette({psize}) sample = {palette[:4]}")
    print(f"  blocks = {bcount}")
    if xs:
        print(f"  x range [{min(xs)}..{max(xs)}] (center w/2={w // 2})")
        print(f"  y range [{min(ys)}..{max(ys)}] (center h/2={h // 2})")
        print(f"  z range [{min(zs)}..{max(zs)}] (center d/2={d // 2})")
    print(f"  states = {scount}")
    for s in states:
        print(f"    @({s[0]},{s[1]},{s[2]}) {s[3]} {s[4]!r}")


if __name__ == "__main__":
    read(sys.argv[1])
