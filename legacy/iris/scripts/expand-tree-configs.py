"""
expand-tree-configs.py
Expands each tree-config JSON by duplicating every entry with N additional seeds,
producing N+1 variants per original entry. Output written to tools/tree-gen/configs/expanded/.

Usage: python scripts/expand-tree-configs.py [--variants N]
Default: --variants 9  (gives 10 total per original entry)
"""

import json
import os
import argparse
from pathlib import Path

def expand_config(entries, variants):
    """Return expanded list: original + (variants) copies with offset seeds."""
    result = []
    for entry in entries:
        base_seed = entry.get("seed", 1000)
        # Original entry unchanged
        result.append(entry)
        # Additional variants with seed offsets of 100, 200, ... variants*100
        for i in range(1, variants + 1):
            copy = dict(entry)
            copy["seed"] = base_seed + i * 100
            # Update name to reflect variant number if name exists
            if "name" in copy:
                copy["name"] = copy["name"]  # keep same profile name - seed drives shape
            result.append(copy)
    return result

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--variants", type=int, default=9,
                        help="Number of additional seed variants per entry (default: 9)")
    parser.add_argument("--configs-dir", default="tools/tree-gen/configs",
                        help="Directory containing tree-config JSON files")
    parser.add_argument("--out-dir", default="tools/tree-gen/configs/expanded",
                        help="Output directory for expanded configs")
    args = parser.parse_args()

    configs_dir = Path(args.configs_dir)
    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    config_files = sorted(configs_dir.glob("*.json"))
    if not config_files:
        print(f"No JSON files found in {configs_dir}")
        return

    for cfg_path in config_files:
        with open(cfg_path, "r", encoding="utf-8") as f:
            entries = json.load(f)

        expanded = expand_config(entries, args.variants)
        out_path = out_dir / cfg_path.name

        with open(out_path, "w", encoding="utf-8") as f:
            json.dump(expanded, f, indent=2)

        print(f"{cfg_path.name}: {len(entries)} -> {len(expanded)} entries")

    print(f"\nDone. Expanded configs written to {out_dir}")

if __name__ == "__main__":
    main()
