"""One-shot: insert a village-meadow jigsawStructures entry next to village-plains
in a live Iris biome JSON, preserving exact formatting. Idempotent.
Usage: python wire_meadow_biome.py <biome.json> [rarity]
"""
import json
import sys

p = sys.argv[1]
rarity = int(sys.argv[2]) if len(sys.argv) > 2 else 800
s = open(p, encoding="utf-8").read()

if '"village-meadow"' in s:
    print("already wired; no change")
    json.load(open(p, encoding="utf-8"))
    sys.exit(0)

old = (
    '        {\n'
    '            "structure": "village-plains",\n'
    '            "rarity": 1200\n'
    '        }'
)
assert s.count(old) == 1, "anchor count=%d" % s.count(old)
new = old + (
    ',\n        {\n'
    '            "structure": "village-meadow",\n'
    '            "rarity": %d\n'
    '        }'
) % rarity
s = s.replace(old, new)
open(p, "w", encoding="utf-8", newline="\n").write(s)
json.load(open(p, encoding="utf-8"))
print("OK inserted village-meadow (rarity %d) + valid JSON" % rarity)
