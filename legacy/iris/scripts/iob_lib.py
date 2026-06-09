"""Shared Iris V2 ".iob" object writer + TileData encoders.

Single source of truth for the binary layout so every producer stays in sync:
- nbt_to_iris.py (vanilla .nbt  -> .iob)
- json_to_iob.py (plaintext JSON source -> .iob)

Byte layout (big-endian, Java DataOutputStream order), matching the spec in
docs/design/IOB-FILE-FORMAT.md and Iris' own IrisObject.write:

    int32  width (X)
    int32  height (Y)
    int32  depth (Z)
    UTF    "Iris V2 IOB;"                 (uint16 length + UTF-8 bytes)
    int16  paletteSize
    UTF    palette[paletteSize]
    int32  blockCount
    { int16 x, int16 y, int16 z, int16 paletteIndex } * blockCount
    int32  statesCount
    { int16 x, int16 y, int16 z, <TileData payload> } * statesCount

TileData payloads are documented in docs/design/TILEDATA-FORMAT.md and produced
by the encode_tile_* helpers below.

Coordinate convention: callers pass coordinates ALREADY centered
(stored = original - size // 2) for both blocks and states.
"""
import json
import os
import struct

SIGNATURE = "Iris V2 IOB;"


def write_utf(buf, s):
    """Append a Java writeUTF-style string: uint16 byte length + UTF-8 bytes."""
    b = s.encode("utf-8")
    if len(b) > 0xFFFF:
        raise ValueError(f"UTF string too long ({len(b)} bytes): {s[:40]!r}...")
    buf += struct.pack(">H", len(b))
    buf += b


# --- TileData payload encoders --------------------------------------------
# Modern payload (TileData.toBinary): two UTF strings.
def encode_tile_modern(material, properties):
    """material: namespaced key string (e.g. 'minecraft:chest') or '' for null.
    properties: a dict serialized to JSON (NMS tile NBT). Version-specific.
    """
    buf = bytearray()
    write_utf(buf, material or "")
    # Gson is configured with disableHtmlEscaping + lenient; compact JSON with
    # non-ASCII left as UTF-8 is the closest faithful match.
    write_utf(buf, json.dumps(properties or {}, separators=(",", ":"),
                              ensure_ascii=False))
    return bytes(buf)


# Legacy payloads (LegacyTileData): int16 id + handler body.
# NOTE: legacy bodies store Bukkit ENUM ORDINALS, which are version-fragile.
_LEGACY_SIGN, _LEGACY_SPAWNER, _LEGACY_BANNER = 0, 1, 2


def encode_tile_legacy_sign(lines, dye_ordinal):
    """lines: up to 4 strings; dye_ordinal: DyeColor enum ordinal (int8)."""
    ln = list(lines) + ["", "", "", ""]
    buf = bytearray()
    buf += struct.pack(">h", _LEGACY_SIGN)
    for i in range(4):
        write_utf(buf, ln[i])
    buf += struct.pack(">b", dye_ordinal)
    return bytes(buf)


def encode_tile_legacy_spawner(entity_type_ordinal):
    """entity_type_ordinal: Bukkit EntityType enum ordinal (int16)."""
    buf = bytearray()
    buf += struct.pack(">h", _LEGACY_SPAWNER)
    buf += struct.pack(">h", entity_type_ordinal)
    return bytes(buf)


def encode_tile_legacy_banner(base_color_ordinal, patterns):
    """base_color_ordinal: DyeColor ordinal (int8).
    patterns: list of (color_ordinal, pattern_ordinal) int8 pairs.
    """
    buf = bytearray()
    buf += struct.pack(">h", _LEGACY_BANNER)
    buf += struct.pack(">b", base_color_ordinal)
    buf += struct.pack(">b", len(patterns))
    for color_ordinal, pattern_ordinal in patterns:
        buf += struct.pack(">b", color_ordinal)
        buf += struct.pack(">b", pattern_ordinal)
    return bytes(buf)


# --- IOB writer ------------------------------------------------------------
def build_iob_bytes(w, h, d, blocks, states=None):
    """blocks: list of (x, y, z, blockstring) with coords already centered.
    states: optional list of (x, y, z, payload_bytes) with coords centered.
    Returns (bytes, palette_size).
    """
    palette = []
    index = {}
    for _, _, _, bs in blocks:
        if bs not in index:
            index[bs] = len(palette)
            palette.append(bs)

    buf = bytearray()
    buf += struct.pack(">i", w)
    buf += struct.pack(">i", h)
    buf += struct.pack(">i", d)
    write_utf(buf, SIGNATURE)
    buf += struct.pack(">h", len(palette))
    for p in palette:
        write_utf(buf, p)
    buf += struct.pack(">i", len(blocks))
    for x, y, z, bs in blocks:
        buf += struct.pack(">hhhh", x, y, z, index[bs])

    states = states or []
    buf += struct.pack(">i", len(states))
    for x, y, z, payload in states:
        buf += struct.pack(">hhh", x, y, z)
        buf += payload
    return bytes(buf), len(palette)


def write_iob(path, w, h, d, blocks, states=None):
    """Write an .iob file. Returns the palette size (for logging)."""
    data, npal = build_iob_bytes(w, h, d, blocks, states)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(data)
    return npal
