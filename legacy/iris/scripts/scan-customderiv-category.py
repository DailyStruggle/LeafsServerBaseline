import json, os, sys

VALID = {"beach","desert","extreme_hills","forest","icy","jungle","mesa","mushroom",
         "nether","none","ocean","plains","river","savanna","swamp","taiga","the_end"}

base = r"iris/pack-base/biomes"
overlay = r"iris/pack-overlay/biomes"

# build merged map: relpath -> actual file path (overlay wins)
merged = {}
for root_dir in (base, overlay):
    if not os.path.isdir(root_dir):
        continue
    for dirpath, _, files in os.walk(root_dir):
        for f in files:
            if not f.endswith(".json"):
                continue
            full = os.path.join(dirpath, f)
            rel = os.path.relpath(full, root_dir).replace("\\", "/")
            merged[rel] = full  # overlay processed last => wins

problems = []
for rel, full in sorted(merged.items()):
    try:
        with open(full, "r", encoding="utf-8") as fh:
            data = json.load(fh)
    except Exception as e:
        problems.append((full, f"PARSE ERROR: {e}"))
        continue
    cds = data.get("customDerivitives")
    if not isinstance(cds, list):
        continue
    for i, cd in enumerate(cds):
        if not isinstance(cd, dict):
            continue
        cat = cd.get("category")
        if cat is None:
            problems.append((full, f"customDerivitives[{i}] MISSING category (id={cd.get('id')})"))
        elif cat not in VALID:
            problems.append((full, f"customDerivitives[{i}] INVALID category '{cat}' (id={cd.get('id')})"))

for full, msg in problems:
    print(f"{full}: {msg}")
print(f"\nTOTAL PROBLEMS: {len(problems)}")
