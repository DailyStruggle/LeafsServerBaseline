#!/usr/bin/env python3
"""Deploy the source-controlled LeafTreeGen inputs (config.yml + species/*.json)
to the server's plugin config folder.

LeafTreeGen is an externalized plugin (https://modrinth.com/plugin/leaf-treegen);
this repo only owns its CONFIG. Python twin of deploy-treegen-config.ps1.

Like the other deploy helpers this ONLY stages/copies files - it does NOT boot
the server or reload the plugin. After deploying, run "/leaftree reload"
(config/species changes) or restart for worldgen (only affects newly generated
chunks); both are user-triggered.

Interpreter on this dev box (the bare `python`/`py` aliases are broken Store stubs):
    C:\\Users\\lxgol\\AppData\\Local\\Programs\\Python\\Python313\\python.exe   (Python 3.13.x)

Usage:
    python deploy_treegen_config.py --server-base "C:\\GameServers\\Minecraft\\testServer\\RTP-Paper\\26.2"
"""
from __future__ import annotations

import argparse
import shutil
import sys
from pathlib import Path

# configs/ source folder = this script's directory; repo root = its parent.
CONFIGS_DIR = Path(__file__).resolve().parent
SRC = CONFIGS_DIR / "LeafTreeGen"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Deploy source-controlled LeafTreeGen config to a server.")
    parser.add_argument("-s", "--server-base", required=True,
                        help="Server base folder (contains the plugins/ directory).")
    args = parser.parse_args(argv)

    src_config = SRC / "config.yml"
    src_species = SRC / "species"
    if not src_config.is_file():
        print(f"ERROR: Source config not found: {src_config}", file=sys.stderr)
        return 1

    dest = Path(args.server_base) / "plugins" / "LeafTreeGen"
    dest_species = dest / "species"
    dest_species.mkdir(parents=True, exist_ok=True)

    print(f"=== Deploying LeafTreeGen config to {dest} ===")
    shutil.copy2(src_config, dest / "config.yml")
    print("  config.yml deployed")

    # Wipe-then-copy species so files removed from source control don't linger.
    for old in dest_species.glob("*.json"):
        old.unlink()
    count = 0
    for f in sorted(src_species.glob("*.json")):
        shutil.copy2(f, dest_species / f.name)
        count += 1
    print(f"  species deployed: {count} files")

    print("LeafTreeGen config deploy complete.")
    print("Reminder: run /leaftree reload (config/species) or restart for worldgen;")
    print("worldgen changes only affect newly generated chunks (server boots are user-triggered).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
