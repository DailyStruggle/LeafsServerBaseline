#!/usr/bin/env python3
"""One-shot migration helper: remove deprecated top-level Region fields that the
current Iris build silently ignores (logged as 'Unknown Region field ...').

Removed keys and why they are safe to drop:
  - jigsawStructures: superseded by dimension importedStructures (defaults ALL_ON,
    so vanilla structures still generate) + dimension/region `structures` lists.
  - carving:          moved to the dimension (carving/carvingEnabled/caveProfile).
  - riverRarity:      no region-level equivalent in current Iris.
  - riverThickness:   no region-level equivalent in current Iris.
  - _comment:         documentation-only key the current Iris build rejects as an
    unknown field; design rationale lives in docs/ instead.
  - seasonalGroup:    custom tag the current Iris build rejects as an unknown
    field; the seasonal mapping is documented in docs/design/CUSTOM-BIOMES.md.

The remover is text-based and format-preserving: it deletes only the targeted
top-level key/value spans, then strips any dangling trailing comma it created,
and finally validates that each file still parses as JSON.
"""
import json
import re
import sys
from pathlib import Path

KEYS = {"jigsawStructures", "carving", "riverRarity", "riverThickness",
        "_comment", "seasonalGroup"}
KEY_RE = re.compile(r'^  "(' + "|".join(KEYS) + r')"\s*:\s*(.*)$')


def find_value_end(lines, start):
    """Return index of the last line of the value beginning at line `start`."""
    after = lines[start].split(":", 1)[1].lstrip()
    if not after or after[0] not in "{[":
        return start  # scalar value, single line
    open_ch = after[0]
    close_ch = "}" if open_ch == "{" else "]"
    depth = 0
    in_str = False
    esc = False
    for i in range(start, len(lines)):
        # for the first line only scan from the value start
        text = lines[i]
        if i == start:
            text = lines[i][lines[i].index(":") + 1:]
        for ch in text:
            if esc:
                esc = False
                continue
            if ch == "\\":
                esc = True
                continue
            if ch == '"':
                in_str = not in_str
                continue
            if in_str:
                continue
            if ch == open_ch:
                depth += 1
            elif ch == close_ch:
                depth -= 1
                if depth == 0:
                    return i
    raise ValueError(f"unbalanced {open_ch} starting at line {start}")


def strip_file(path: Path) -> bool:
    raw = path.read_bytes()
    had_bom = raw.startswith(b"\xef\xbb\xbf")
    text = raw.decode("utf-8-sig")
    lines = text.split("\n")
    out = []
    removed = []
    i = 0
    while i < len(lines):
        m = KEY_RE.match(lines[i])
        if m:
            end = find_value_end(lines, i)
            removed.append(m.group(1))
            i = end + 1
            continue
        out.append(lines[i])
        i += 1
    if not removed:
        return False
    new_text = "\n".join(out)
    # Remove any dangling comma we just created before a closing } or ].
    new_text = re.sub(r",(\s*[}\]])", r"\1", new_text)
    json.loads(new_text)  # validate
    data = new_text.encode("utf-8")
    if had_bom:
        data = b"\xef\xbb\xbf" + data
    path.write_bytes(data)
    print(f"  {path.name}: removed {sorted(set(removed))}")
    return True


def main():
    regions = Path(sys.argv[1])
    changed = 0
    for f in sorted(regions.glob("*.json")):
        if strip_file(f):
            changed += 1
    print(f"Done. {changed} region file(s) updated.")


if __name__ == "__main__":
    main()
