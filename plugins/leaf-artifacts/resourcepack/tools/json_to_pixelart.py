#!/usr/bin/env python3
"""Render JSON pixel-art definitions into 32x32 PNG slot icons.

This is a pure Python 3 standard-library tool (no Pillow / no external
packages, matching the iris/tree-gen convention) that turns a small JSON
description of pixel art into a 32x32 RGBA PNG. It is used to author the
LeafArtifacts curio-slot placeholder icons: pixel art composited on top of a
light-gray stained-glass background.

JSON schema (one file per icon)
-------------------------------
{
  "name": "head",                 // optional; defaults to the file stem
  "size": 32,                      // optional; output is square, default 32
  "background": "light_gray_glass",// optional; a preset name, a "#RRGGBB[AA]"
                                    //   colour, or "none" for transparent
  "palette": {                     // single-character key -> colour
    "k": "#1a1a1a",
    "w": "#f0f0f0ff"
  },
  "art": [                         // rows of single-character cells
    "................................",
    ".......kkkkkkkkkkkkkkkkkk......."
    // ... any rectangular grid; "." or " " == transparent
  ]
}

The "art" grid may be any rectangular size; it is scaled with
nearest-neighbour sampling to fill the output square, so a 16x16 grid scales
2x to 32x32. Colours may be "#RGB", "#RRGGBB", or "#RRGGBBAA"; transparent
cells fall through to the background.

Usage
-----
  python json_to_pixelart.py                 # icons/ -> ../assets/.../textures/item/slot
  python json_to_pixelart.py --in <dir> --out <dir>
  python json_to_pixelart.py icons/head.json # render a single file (out from --out)
"""

from __future__ import annotations

import argparse
import json
import struct
import sys
import zlib
from pathlib import Path
from typing import Dict, List, Optional, Tuple

RGBA = Tuple[int, int, int, int]

# Default I/O locations relative to this script.
_HERE = Path(__file__).resolve().parent
DEFAULT_IN = _HERE / "icons"
DEFAULT_OUT = _HERE.parent / "assets" / "leafartifacts" / "textures" / "item" / "slot"

TRANSPARENT: RGBA = (0, 0, 0, 0)


# --------------------------------------------------------------------------- #
# Colour parsing
# --------------------------------------------------------------------------- #
def parse_color(value: str) -> RGBA:
    """Parse "#RGB", "#RRGGBB" or "#RRGGBBAA" (alpha defaults to 255)."""
    s = value.strip().lstrip("#")
    if len(s) == 3:
        r, g, b = (int(c * 2, 16) for c in s)
        return (r, g, b, 255)
    if len(s) == 6:
        return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)
    if len(s) == 8:
        return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), int(s[6:8], 16))
    raise ValueError(f"Invalid colour '{value}' (expected #RGB, #RRGGBB or #RRGGBBAA)")


def alpha_over(top: RGBA, bottom: RGBA) -> RGBA:
    """Composite `top` over `bottom` using straight-alpha source-over."""
    ta = top[3] / 255.0
    ba = bottom[3] / 255.0
    out_a = ta + ba * (1.0 - ta)
    if out_a <= 0.0:
        return TRANSPARENT
    out = []
    for i in range(3):
        c = (top[i] * ta + bottom[i] * ba * (1.0 - ta)) / out_a
        out.append(max(0, min(255, int(round(c)))))
    return (out[0], out[1], out[2], max(0, min(255, int(round(out_a * 255)))))


# --------------------------------------------------------------------------- #
# Background presets
# --------------------------------------------------------------------------- #
def make_background(spec: Optional[str], size: int) -> List[RGBA]:
    """Build a `size*size` flat background buffer from a preset or colour."""
    if spec is None or spec == "" or spec == "light_gray_glass":
        return _light_gray_glass(size)
    if spec.lower() == "none":
        return [TRANSPARENT] * (size * size)
    # Treat anything else as a solid colour.
    color = parse_color(spec)
    return [color] * (size * size)


def _light_gray_glass(size: int) -> List[RGBA]:
    """A light-gray stained-glass-pane look: a darker frame around a lighter,
    faintly streaked fill. Designed to read as the vanilla light-gray glass
    placeholder while leaving the centre clear for pixel art."""
    fill: RGBA = (176, 176, 184, 235)
    fill_alt: RGBA = (188, 188, 196, 235)
    frame: RGBA = (120, 120, 128, 245)
    frame_hi: RGBA = (208, 208, 214, 245)
    border = max(1, size // 16)  # ~2px on a 32px icon
    buf: List[RGBA] = []
    for y in range(size):
        for x in range(size):
            on_outer = x < border or y < border or x >= size - border or y >= size - border
            if on_outer:
                # Top/left edges read as a highlight, bottom/right as shadow.
                buf.append(frame_hi if (x < border or y < border) else frame)
            else:
                # Subtle diagonal streak so it looks like glass, not flat paint.
                buf.append(fill_alt if ((x + y) // 2) % 2 == 0 else fill)
    return buf


# --------------------------------------------------------------------------- #
# Art rasterisation
# --------------------------------------------------------------------------- #
def rasterize(definition: dict, size: int) -> List[RGBA]:
    """Render one icon definition to a `size*size` RGBA buffer."""
    background = make_background(definition.get("background"), size)

    palette_raw: Dict[str, str] = definition.get("palette", {}) or {}
    palette: Dict[str, RGBA] = {k: parse_color(v) for k, v in palette_raw.items()}

    art: List[str] = definition.get("art", []) or []
    if not art:
        return background

    src_h = len(art)
    src_w = max(len(row) for row in art)

    out: List[RGBA] = list(background)
    for y in range(size):
        sy = (y * src_h) // size
        row = art[sy]
        for x in range(size):
            sx = (x * src_w) // size
            cell = row[sx] if sx < len(row) else "."
            if cell in (".", " "):
                continue
            color = palette.get(cell)
            if color is None:
                raise ValueError(f"Art cell '{cell}' has no palette entry")
            idx = y * size + x
            out[idx] = alpha_over(color, out[idx])
    return out


# --------------------------------------------------------------------------- #
# PNG writer (pure stdlib)
# --------------------------------------------------------------------------- #
def write_png(path: Path, size: int, pixels: List[RGBA]) -> None:
    """Write an 8-bit RGBA PNG (colour type 6) with no external deps."""
    raw = bytearray()
    for y in range(size):
        raw.append(0)  # filter type 0 (None) for each scanline
        base = y * size
        for x in range(size):
            r, g, b, a = pixels[base + x]
            raw += bytes((r & 255, g & 255, b & 255, a & 255))

    def chunk(typ: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + typ
            + data
            + struct.pack(">I", zlib.crc32(typ + data) & 0xFFFFFFFF)
        )

    ihdr = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw), 9)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "wb") as fh:
        fh.write(b"\x89PNG\r\n\x1a\n")
        fh.write(chunk(b"IHDR", ihdr))
        fh.write(chunk(b"IDAT", idat))
        fh.write(chunk(b"IEND", b""))


# --------------------------------------------------------------------------- #
# Driver
# --------------------------------------------------------------------------- #
def render_file(src: Path, out_dir: Path, size_override: Optional[int]) -> Path:
    definition = json.loads(src.read_text(encoding="utf-8"))
    size = int(size_override or definition.get("size", 32))
    name = definition.get("name") or src.stem
    pixels = rasterize(definition, size)
    dest = out_dir / f"{name}.png"
    write_png(dest, size, pixels)
    return dest


def main(argv: Optional[List[str]] = None) -> int:
    parser = argparse.ArgumentParser(description="Render JSON pixel-art into 32x32 PNG icons.")
    parser.add_argument("files", nargs="*", type=Path,
                        help="Specific JSON files to render (default: all in --in).")
    parser.add_argument("--in", dest="in_dir", type=Path, default=DEFAULT_IN,
                        help=f"Input directory of *.json icon definitions (default: {DEFAULT_IN}).")
    parser.add_argument("--out", dest="out_dir", type=Path, default=DEFAULT_OUT,
                        help=f"Output directory for PNGs (default: {DEFAULT_OUT}).")
    parser.add_argument("--size", type=int, default=None,
                        help="Override the output square size (default: 32 or the JSON's 'size').")
    args = parser.parse_args(argv)

    if args.files:
        sources = list(args.files)
    elif args.in_dir.is_dir():
        sources = sorted(args.in_dir.glob("*.json"))
    else:
        print(f"No input: '{args.in_dir}' is not a directory and no files were given.",
              file=sys.stderr)
        return 2

    if not sources:
        print(f"No *.json icon definitions found in '{args.in_dir}'.", file=sys.stderr)
        return 1

    count = 0
    for src in sources:
        try:
            dest = render_file(src, args.out_dir, args.size)
        except (ValueError, KeyError, json.JSONDecodeError) as exc:
            print(f"[FAIL] {src.name}: {exc}", file=sys.stderr)
            return 1
        print(f"[ok] {src.name} -> {dest}")
        count += 1
    print(f"Rendered {count} icon(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
