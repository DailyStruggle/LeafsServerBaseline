#!/usr/bin/env python3
"""Strip the dead top-level ``category`` key from overlay biomes for Iris 4.0.

Iris 4.0's ``IrisBiome`` class has NO ``category`` field (verified by
decompiling ``Iris.jar``: the only fields are ``derivative`` /
``vanillaDerivative`` and the ``customDerivitives[]`` list). In 3.9.1 a biome
carried a top-level ``category`` enum; in 4.0 that concept lives ONLY inside
each ``customDerivitives`` entry (``IrisBiomeCustom.category``), and the
biome's own category is inferred from its ``vanillaDerivative``.

So the correct 4.0 migration for our overlay is to REMOVE the now-dead
top-level ``category`` key. We deliberately do NOT move it into
``customDerivitives``: a ``customDerivitives`` entry REGISTERS a new custom
biome derivative (a behaviour/registry change), which is not what a plain
top-level category meant.

Only the top-level ``category`` line is removed. Any ``category`` nested inside
a ``customDerivitives`` entry (deeper indentation) is left untouched. The edit
is done on raw text so formatting / diffs stay minimal, and it is idempotent.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

OVERLAY_BIOMES = Path(__file__).resolve().parents[1] / "pack-overlay" / "biomes"


def top_indent(text: str) -> str | None:
    """Indentation used by the object's top-level keys (from the first key)."""
    m = re.search(r'^(\s+)"[^"]+"\s*:', text, re.MULTILINE)
    return m.group(1) if m else None


def strip_top_category(text: str) -> tuple[str, bool]:
    """Remove the single top-level ``category`` line; return (text, changed)."""
    try:
        data = json.loads(text)
    except json.JSONDecodeError:
        return text, False
    if not isinstance(data, dict) or data.get("category") in (None, ""):
        return text, False

    indent = top_indent(text)
    if indent is None:
        return text, False

    # Match exactly the top-level "category": "..." line (this indentation),
    # including its line break. Nested customDerivitives categories are more
    # deeply indented and will not match.
    line_re = re.compile(
        r'^' + re.escape(indent) + r'"category"\s*:\s*"[^"]*"(,?)[ \t]*\r?\n',
        re.MULTILINE,
    )
    m = line_re.search(text)
    if not m:
        return text, False

    had_comma = m.group(1) == ","
    new_text = text[: m.start()] + text[m.end():]

    if not had_comma:
        # category was the LAST top-level key: drop the now-trailing comma on
        # the preceding key line so the JSON stays valid.
        new_text = re.sub(r",(\s*)$", r"\1", new_text, count=1)
        new_text = re.sub(r",(\s*\n\s*\})", r"\1", new_text, count=1)

    # Validate before committing.
    try:
        json.loads(new_text)
    except json.JSONDecodeError:
        return text, False
    return new_text, True


def main() -> int:
    if not OVERLAY_BIOMES.is_dir():
        print(f"ERROR: biomes dir not found: {OVERLAY_BIOMES}", file=sys.stderr)
        return 1

    changed = 0
    for path in sorted(OVERLAY_BIOMES.rglob("*.json")):
        original = path.read_text(encoding="utf-8")
        updated, did = strip_top_category(original)
        if did and updated != original:
            path.write_text(updated, encoding="utf-8", newline="\n")
            changed += 1
            print(f"  {path.relative_to(OVERLAY_BIOMES)}: top-level category removed")

    print(f"Done. Removed top-level category from {changed} file(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
