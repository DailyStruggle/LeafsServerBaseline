#!/usr/bin/env python3
"""Generate pack-overlay spawner overrides that drop the *top-level*
`energyMultiplier` key which the current Iris build does not recognise on the
Spawner object (logged as 'Unknown Spawner field "energyMultiplier"').

Why: IrisSpawner has no top-level `energyMultiplier` field; only the per-entry
spawn objects (IrisEntitySpawn) accept it. The top-level occurrence is silently
ignored by Gson but spams the server log on every world load. The base spawner
files live under iris/pack-base (treated as read-only, re-synced upstream), so
instead of editing them we emit same-path whole-file overrides under
iris/pack-overlay with only the top-level key removed. All nested per-entry
`energyMultiplier` values are preserved verbatim.

The remover is depth-aware and format-preserving: it walks the base file,
tracks object/array nesting (ignoring string contents), and deletes only the
single `energyMultiplier` line that sits at object-depth 1 / array-depth 0.
Each emitted overlay is re-validated as JSON.
"""
import json
import re
import sys
from pathlib import Path

KEY_RE = re.compile(r'^\s*"energyMultiplier"\s*:')


def line_start_depths(lines):
    """Yield (curly, square) nesting depth at the START of each line."""
    curly = square = 0
    in_str = esc = False
    for line in lines:
        yield curly, square
        for ch in line:
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
            if ch == "{":
                curly += 1
            elif ch == "}":
                curly -= 1
            elif ch == "[":
                square += 1
            elif ch == "]":
                square -= 1


def strip_text(text):
    lines = text.split("\n")
    depths = list(line_start_depths(lines))
    out = []
    removed = 0
    for line, (curly, square) in zip(lines, depths):
        # Top-level Spawner key: directly inside the root object, no array.
        if curly == 1 and square == 0 and KEY_RE.match(line):
            removed += 1
            continue
        out.append(line)
    return "\n".join(out), removed


def main():
    repo = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[1]
    base_dir = repo / "pack-base" / "spawners"
    overlay_dir = repo / "pack-overlay" / "spawners"
    written = 0
    for f in sorted(base_dir.rglob("*.json")):
        rel = f.relative_to(base_dir)
        raw = f.read_bytes()
        had_bom = raw.startswith(b"\xef\xbb\xbf")
        text = raw.decode("utf-8-sig")
        new_text, removed = strip_text(text)
        if not removed:
            continue
        json.loads(new_text)  # validate
        dest = overlay_dir / rel
        if dest.exists():
            print(f"  SKIP (overlay exists): {rel}")
            continue
        dest.parent.mkdir(parents=True, exist_ok=True)
        data = new_text.encode("utf-8")
        if had_bom:
            data = b"\xef\xbb\xbf" + data
        dest.write_bytes(data)
        print(f"  {rel}: removed {removed} top-level energyMultiplier")
        written += 1
    print(f"Done. {written} overlay spawner file(s) written.")


if __name__ == "__main__":
    main()
