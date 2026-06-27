"""Minimal but complete Java-edition NBT reader/writer.

Supports the full tag set used by Minecraft structure (.nbt) templates, including
nested compounds/lists and the typed array tags. Reads and writes gzip-compressed
NBT (the on-disk format for structure templates).

Tag model (Python representation):
  - TAG_End            -> n/a (structural)
  - TAG_Byte    (1)    -> ("byte",  int)
  - TAG_Short   (2)    -> ("short", int)
  - TAG_Int     (3)    -> ("int",   int)
  - TAG_Long    (4)    -> ("long",  int)
  - TAG_Float   (5)    -> ("float", float)
  - TAG_Double  (6)    -> ("double",float)
  - TAG_Byte_Array(7)  -> ("byte_array", list[int])
  - TAG_String  (8)    -> ("string", str)
  - TAG_List    (9)    -> ("list", (elem_type:int, list[value]))
  - TAG_Compound(10)   -> ("compound", dict[str, value])
  - TAG_Int_Array(11)  -> ("int_array", list[int])
  - TAG_Long_Array(12) -> ("long_array", list[int])

Each "value" is the tuple ("tagname", payload) so type information round-trips
losslessly. Helper constructors (tbyte, tint, tstring, ...) keep call sites short.
"""

from __future__ import annotations

import gzip
import io
import struct
from typing import Any, Tuple

# Tag id constants
TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11
TAG_LONG_ARRAY = 12

_NAME_BY_ID = {
    TAG_BYTE: "byte",
    TAG_SHORT: "short",
    TAG_INT: "int",
    TAG_LONG: "long",
    TAG_FLOAT: "float",
    TAG_DOUBLE: "double",
    TAG_BYTE_ARRAY: "byte_array",
    TAG_STRING: "string",
    TAG_LIST: "list",
    TAG_COMPOUND: "compound",
    TAG_INT_ARRAY: "int_array",
    TAG_LONG_ARRAY: "long_array",
}
_ID_BY_NAME = {v: k for k, v in _NAME_BY_ID.items()}


class _Reader:
    def __init__(self, data: bytes):
        self.b = data
        self.i = 0

    def u1(self) -> int:
        v = self.b[self.i]
        self.i += 1
        return v

    def take(self, n: int) -> bytes:
        v = self.b[self.i:self.i + n]
        self.i += n
        return v

    def s2(self) -> int:
        return struct.unpack(">h", self.take(2))[0]

    def s4(self) -> int:
        return struct.unpack(">i", self.take(4))[0]

    def s8(self) -> int:
        return struct.unpack(">q", self.take(8))[0]

    def f4(self) -> float:
        return struct.unpack(">f", self.take(4))[0]

    def f8(self) -> float:
        return struct.unpack(">d", self.take(8))[0]

    def string(self) -> str:
        n = struct.unpack(">H", self.take(2))[0]
        return self.take(n).decode("utf-8")

    def payload(self, tag: int) -> Any:
        if tag == TAG_BYTE:
            v = self.u1()
            if v >= 128:
                v -= 256
            return ("byte", v)
        if tag == TAG_SHORT:
            return ("short", self.s2())
        if tag == TAG_INT:
            return ("int", self.s4())
        if tag == TAG_LONG:
            return ("long", self.s8())
        if tag == TAG_FLOAT:
            return ("float", self.f4())
        if tag == TAG_DOUBLE:
            return ("double", self.f8())
        if tag == TAG_BYTE_ARRAY:
            n = self.s4()
            return ("byte_array", list(struct.unpack(">%db" % n, self.take(n))) if n else [])
        if tag == TAG_STRING:
            return ("string", self.string())
        if tag == TAG_LIST:
            etype = self.u1()
            n = self.s4()
            items = [self.payload(etype) for _ in range(n)]
            return ("list", (etype, items))
        if tag == TAG_COMPOUND:
            d = {}
            while True:
                t = self.u1()
                if t == TAG_END:
                    break
                name = self.string()
                d[name] = self.payload(t)
            return ("compound", d)
        if tag == TAG_INT_ARRAY:
            n = self.s4()
            return ("int_array", list(struct.unpack(">%di" % n, self.take(n * 4))) if n else [])
        if tag == TAG_LONG_ARRAY:
            n = self.s4()
            return ("long_array", list(struct.unpack(">%dq" % n, self.take(n * 8))) if n else [])
        raise ValueError("unknown tag id %d at %d" % (tag, self.i))


class _Writer:
    def __init__(self):
        self.buf = io.BytesIO()

    def u1(self, v: int):
        self.buf.write(bytes([v & 0xFF]))

    def string(self, s: str):
        data = s.encode("utf-8")
        self.buf.write(struct.pack(">H", len(data)))
        self.buf.write(data)

    def payload(self, value: Tuple[str, Any]):
        tagname, v = value
        if tagname == "byte":
            self.buf.write(struct.pack(">b", v))
        elif tagname == "short":
            self.buf.write(struct.pack(">h", v))
        elif tagname == "int":
            self.buf.write(struct.pack(">i", v))
        elif tagname == "long":
            self.buf.write(struct.pack(">q", v))
        elif tagname == "float":
            self.buf.write(struct.pack(">f", v))
        elif tagname == "double":
            self.buf.write(struct.pack(">d", v))
        elif tagname == "byte_array":
            self.buf.write(struct.pack(">i", len(v)))
            if v:
                self.buf.write(struct.pack(">%db" % len(v), *v))
        elif tagname == "string":
            self.string(v)
        elif tagname == "list":
            etype, items = v
            # Preserve the stored element type exactly (Minecraft sometimes writes
            # a concrete element type even for empty lists); do NOT coerce to END.
            self.u1(etype)
            self.buf.write(struct.pack(">i", len(items)))
            for it in items:
                self.payload(it)
        elif tagname == "compound":
            for name, child in v.items():
                cid = _ID_BY_NAME[child[0]]
                self.u1(cid)
                self.string(name)
                self.payload(child)
            self.u1(TAG_END)
        elif tagname == "int_array":
            self.buf.write(struct.pack(">i", len(v)))
            if v:
                self.buf.write(struct.pack(">%di" % len(v), *v))
        elif tagname == "long_array":
            self.buf.write(struct.pack(">i", len(v)))
            if v:
                self.buf.write(struct.pack(">%dq" % len(v), *v))
        else:
            raise ValueError("unknown tag name %r" % tagname)


def read_nbt(path: str):
    """Return (root_name, root_value) where root_value is ("compound", dict)."""
    with open(path, "rb") as fh:
        raw = fh.read()
    if raw[:2] == b"\x1f\x8b":
        raw = gzip.decompress(raw)
    r = _Reader(raw)
    t = r.u1()
    if t != TAG_COMPOUND:
        raise ValueError("root tag is not a compound (%d)" % t)
    name = r.string()
    val = r.payload(TAG_COMPOUND)
    return name, val


def write_nbt(path: str, root_name: str, root_value, gzipped: bool = True):
    w = _Writer()
    w.u1(TAG_COMPOUND)
    w.string(root_name)
    w.payload(root_value)
    data = w.buf.getvalue()
    if gzipped:
        data = gzip.compress(data)
    with open(path, "wb") as fh:
        fh.write(data)


# ---- small constructors / accessors -------------------------------------

def tbyte(v):
    return ("byte", v)


def tint(v):
    return ("int", v)


def tdouble(v):
    return ("double", v)


def tstring(v):
    return ("string", v)


def tlist(etype, items):
    return ("list", (etype, items))


def tcompound(d):
    return ("compound", d)


def comp(value):
    """Unwrap a ("compound", dict) value to its dict."""
    assert value[0] == "compound", value[0]
    return value[1]


def listval(value):
    """Unwrap a ("list", (etype, items)) value to (etype, items)."""
    assert value[0] == "list", value[0]
    return value[1]
