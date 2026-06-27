import gzip, struct, sys, os

# Minimal NBT reader: extract the structure "size" (list of 3 ints) and footprint.

def read_tag(b, i, tag_type):
    if tag_type == 1:  # byte
        return b[i], i + 1
    if tag_type == 2:  # short
        return struct.unpack_from('>h', b, i)[0], i + 2
    if tag_type == 3:  # int
        return struct.unpack_from('>i', b, i)[0], i + 4
    if tag_type == 4:  # long
        return struct.unpack_from('>q', b, i)[0], i + 8
    if tag_type == 5:  # float
        return struct.unpack_from('>f', b, i)[0], i + 4
    if tag_type == 6:  # double
        return struct.unpack_from('>d', b, i)[0], i + 8
    if tag_type == 7:  # byte array
        n = struct.unpack_from('>i', b, i)[0]; i += 4
        return None, i + n
    if tag_type == 8:  # string
        n = struct.unpack_from('>H', b, i)[0]; i += 2
        return b[i:i+n].decode('utf-8', 'replace'), i + n
    if tag_type == 9:  # list
        et = b[i]; i += 1
        n = struct.unpack_from('>i', b, i)[0]; i += 4
        vals = []
        for _ in range(n):
            v, i = read_tag(b, i, et)
            vals.append(v)
        return vals, i
    if tag_type == 10:  # compound
        d = {}
        while True:
            t = b[i]; i += 1
            if t == 0:
                break
            nl = struct.unpack_from('>H', b, i)[0]; i += 2
            name = b[i:i+nl].decode('utf-8', 'replace'); i += nl
            v, i = read_tag(b, i, t)
            d[name] = v
        return d, i
    if tag_type == 11:  # int array
        n = struct.unpack_from('>i', b, i)[0]; i += 4
        return None, i + n * 4
    if tag_type == 12:  # long array
        n = struct.unpack_from('>i', b, i)[0]; i += 4
        return None, i + n * 8
    raise ValueError(f'bad tag {tag_type} at {i}')


def size_of(path):
    with open(path, 'rb') as f:
        raw = f.read()
    if raw[:2] == b'\x1f\x8b':
        raw = gzip.decompress(raw)
    i = 0
    t = raw[i]; i += 1
    nl = struct.unpack_from('>H', raw, i)[0]; i += 2 + nl
    root, _ = read_tag(raw, i, t)
    sz = root.get('size')
    if sz and len(sz) == 3:
        return tuple(sz)
    return None


def main():
    root = sys.argv[1]
    rows = []
    for dirpath, _, files in os.walk(root):
        for fn in files:
            if fn.endswith('.nbt'):
                p = os.path.join(dirpath, fn)
                try:
                    s = size_of(p)
                except Exception as e:
                    s = None
                if s:
                    x, y, z = s
                    footprint = x * z
                    volume = x * y * z
                    rows.append((footprint, volume, x, y, z, os.path.relpath(p, root)))
    rows.sort(reverse=True)
    print(f'{"footprint":>10} {"vol":>10} {"X":>4} {"Y":>4} {"Z":>4}  path')
    for footprint, volume, x, y, z, rel in rows[:50]:
        print(f'{footprint:>10} {volume:>10} {x:>4} {y:>4} {z:>4}  {rel}')


if __name__ == '__main__':
    main()
