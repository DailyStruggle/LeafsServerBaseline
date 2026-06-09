#!/usr/bin/env python3
"""Namespace Iris biome derivative values for the 4.0 migration.

Iris 4.0 expects ``derivative`` / ``vanillaDerivative`` to be a namespaced
Minecraft biome key (e.g. ``minecraft:jagged_peaks``) instead of the old
3.9.1 Bukkit enum form (``JAGGED_PEAKS``). This script rewrites those two
keys across the overlay biomes in place.

Most enum names map 1:1 to ``minecraft:<lowercase>`` because the modern
(1.18+) Bukkit Biome enum already matches the registry keys. A few historical
enum names, however, do NOT (the registry was renamed out from under the old
enum). Those are handled explicitly via RENAME so we never emit a key the
engine cannot resolve.

The rewrite is done with a targeted regex on the raw text so existing
indentation / formatting (and therefore the git diff) stays minimal. Values
that are already namespaced (contain ``:``) or are not all-uppercase enums are
left untouched.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

# Repo-relative root of the biomes we convert.
OVERLAY_BIOMES = Path(__file__).resolve().parents[1] / "pack-overlay" / "biomes"

# Keys whose enum value must become a namespaced biome key.
KEYS = ("derivative", "vanillaDerivative")

# Historical Bukkit enum -> current Minecraft registry key, for the cases
# where a plain lowercase is WRONG. (Kept exhaustive for robustness even if a
# given pack does not currently use every entry.)
RENAME: dict[str, str] = {
    # 1.18 "Caves & Cliffs" / "The Wild" registry renames vs older enum names.
    "MOUNTAINS": "windswept_hills",
    "WOODED_MOUNTAINS": "windswept_forest",
    "GRAVELLY_MOUNTAINS": "windswept_gravelly_hills",
    "SHATTERED_SAVANNA": "windswept_savanna",
    "SNOWY_TUNDRA": "snowy_plains",
    "JUNGLE_EDGE": "sparse_jungle",
    "STONE_SHORE": "stony_shore",
    "GIANT_TREE_TAIGA": "old_growth_pine_taiga",
    "GIANT_SPRUCE_TAIGA": "old_growth_spruce_taiga",
    "TALL_BIRCH_FOREST": "old_growth_birch_forest",
    "WOODED_BADLANDS_PLATEAU": "wooded_badlands",
    "MODIFIED_BADLANDS_PLATEAU": "badlands",
    "MODIFIED_WOODED_BADLANDS_PLATEAU": "wooded_badlands",
    "SWAMP_HILLS": "swamp",
    "TAIGA_MOUNTAINS": "taiga",
    "SNOWY_TAIGA_MOUNTAINS": "snowy_taiga",
    "MODIFIED_JUNGLE": "jungle",
    "MODIFIED_JUNGLE_EDGE": "sparse_jungle",
    "DESERT_HILLS": "desert",
    "MOUNTAIN_EDGE": "windswept_hills",
    "NETHER": "nether_wastes",
    "TALL_BIRCH_HILLS": "old_growth_birch_forest",
}

NAMESPACE = "minecraft:"

# An all-uppercase enum token (letters, digits, underscores), e.g. JAGGED_PEAKS.
ENUM_RE = re.compile(r"^[A-Z][A-Z0-9_]*$")


def to_namespaced(value: str) -> str | None:
    """Return the namespaced key for an enum value, or None to leave as-is."""
    if ":" in value:
        return None  # already namespaced
    if not ENUM_RE.match(value):
        return None  # not an enum token (custom path, mixed case, etc.)
    key = RENAME.get(value, value.lower())
    return NAMESPACE + key


def convert_text(text: str) -> tuple[str, int]:
    """Rewrite derivative/vanillaDerivative enum values in raw JSON text."""
    count = 0
    pattern = re.compile(
        r'("(?:' + "|".join(KEYS) + r')"\s*:\s*")([^"]+)(")'
    )

    def repl(m: re.Match) -> str:
        nonlocal count
        new = to_namespaced(m.group(2))
        if new is None:
            return m.group(0)
        count += 1
        return m.group(1) + new + m.group(3)

    return pattern.sub(repl, text), count


def main() -> int:
    if not OVERLAY_BIOMES.is_dir():
        print(f"ERROR: biomes dir not found: {OVERLAY_BIOMES}", file=sys.stderr)
        return 1

    files_changed = 0
    total_repls = 0
    for path in sorted(OVERLAY_BIOMES.rglob("*.json")):
        original = path.read_text(encoding="utf-8")
        updated, n = convert_text(original)
        if n and updated != original:
            path.write_text(updated, encoding="utf-8", newline="\n")
            files_changed += 1
            total_repls += n
            rel = path.relative_to(OVERLAY_BIOMES)
            print(f"  {rel}: {n} value(s) namespaced")

    print(
        f"Done. {total_repls} derivative value(s) namespaced "
        f"across {files_changed} file(s)."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
