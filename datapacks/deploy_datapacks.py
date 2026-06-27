#!/usr/bin/env python3
"""Deploy the repo's vanilla datapacks to a Minecraft server's world datapacks folder.

Python port of deploy-datapacks.ps1 (see ADR-005: worldgen ships as vanilla
datapacks copied into <ServerBase>/<LevelName>/datapacks/<pack>).

Like the PowerShell version, this ONLY stages/copies files. It does NOT boot the
server (server boots are user-triggered) - after deploying, /reload (or a server
restart for worldgen/dimension changes) is needed for them to take effect.

Interpreter on this dev box (the bare `python`/`py` PATH aliases are broken Store stubs):
    C:\\Users\\lxgol\\AppData\\Local\\Programs\\Python\\Python313\\python.exe   (Python 3.13.x)
Verified working with that interpreter.

Usage examples (PowerShell `;` chaining not needed - this is a single command):
    python deploy_datapacks.py --server-base "C:\\GameServers\\Minecraft\\testServer\\RTP-Paper\\26.2"
    python deploy_datapacks.py -s <path> --only leaf-worldgen dungeons-arise
    python deploy_datapacks.py -s <path>                        # world reset ON by default (timestamped backup)
    python deploy_datapacks.py -s <path> --no-reset             # keep the existing world
    python deploy_datapacks.py -s <path> --no-backup            # reset with NO backup (DESTRUCTIVE)
    python deploy_datapacks.py -s <path> --no-patch             # skip the integration/dedup patch step
"""
from __future__ import annotations

import argparse
import datetime as _dt
import re
import shutil
import subprocess
import sys
from pathlib import Path

# datapacks/ source folder = this script's directory; repo root = its parent.
DATAPACKS_SRC = Path(__file__).resolve().parent
REPO = DATAPACKS_SRC.parent


def resolve_level_name(server_base: Path, explicit: str | None) -> str:
    """Prefer an explicit level name, else read level-name from server.properties, else 'world'."""
    if explicit:
        return explicit
    props = server_base / "server.properties"
    if props.is_file():
        for line in props.read_text(encoding="utf-8", errors="replace").splitlines():
            m = re.match(r"^\s*level-name\s*=\s*(.+)$", line)
            if m:
                name = m.group(1).strip()
                if name:
                    return name
    return "world"


def reset_world(world_dir: Path, server_base: Path, level_name: str, no_backup: bool) -> None:
    if world_dir.is_dir():
        if no_backup:
            print(f"=== Resetting world (no backup): {world_dir} ===")
            shutil.rmtree(world_dir)
        else:
            stamp = _dt.datetime.now().strftime("%Y%m%d-%H%M%S")
            backup = server_base / f"{level_name}_backup_{stamp}"
            print(f"=== Resetting world: moving {world_dir} -> {backup} ===")
            shutil.move(str(world_dir), str(backup))
    else:
        print(f"=== Reset requested but world folder does not exist yet: {world_dir} (will be created) ===")
    # Recreate an empty world folder so datapacks can be staged before boot.
    world_dir.mkdir(parents=True, exist_ok=True)


def run_patches(no_patch: bool) -> None:
    if no_patch:
        return
    patch_all = DATAPACKS_SRC / "patch-all.ps1"
    if not patch_all.is_file():
        return
    print("=== Running datapack integration/dedup patches ===")
    # The patch tooling is PowerShell; invoke it non-interactively.
    try:
        subprocess.run(
            ["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(patch_all)],
            check=True,
        )
    except FileNotFoundError:
        print("  WARNING: 'powershell' not found on PATH; skipping patch step "
              "(use --no-patch to silence, or run patch-all.ps1 manually).")
    except subprocess.CalledProcessError as exc:
        print(f"  WARNING: patch-all.ps1 exited with code {exc.returncode}; continuing with deploy.")


def discover_packs(only: list[str] | None, reset_world: bool) -> list[Path]:
    """A datapack source folder is any direct subfolder of datapacks/ that has a pack.mcmeta."""
    packs = [
        p for p in sorted(DATAPACKS_SRC.iterdir())
        if p.is_dir() and (p / "pack.mcmeta").is_file()
    ]
    if only:
        if reset_world:
            # A world reset wipes the ENTIRE world (all previously-deployed packs
            # are gone). Honouring --only here would re-stage only the named pack
            # into the fresh world, dropping every other pack - e.g. leaf-worldgen,
            # which DEFINES the leaf:* biomes and leaf:* features that other data
            # still references. That produces "Unbound values in registry ...
            # [leaf:...]" and the server refuses to load. So --only is only valid
            # for in-place (--no-reset) updates; during a reset we redeploy ALL.
            print("  NOTE: --only is ignored during a world reset; redeploying ALL datapacks")
            print(f"        (a fresh world needs every pack, not just {', '.join(only)}).")
            print("        Use --no-reset together with --only to update a single pack in place.")
        else:
            wanted = set(only)
            packs = [p for p in packs if p.name in wanted]
    return packs


def deploy_pack(pack: Path, dest: Path) -> int:
    pack_dest = dest / pack.name
    # Wipe destination first so files removed from the repo don't linger (wipe-then-copy).
    if pack_dest.exists():
        shutil.rmtree(pack_dest)
    shutil.copytree(pack, pack_dest)
    return sum(1 for _ in pack_dest.rglob("*") if _.is_file())


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Deploy repo datapacks to a server world's datapacks folder.")
    parser.add_argument("-s", "--server-base", required=True,
                        help="Path to the server base folder (contains server.properties and the world folder).")
    parser.add_argument("-l", "--level-name", default=None,
                        help="World/level folder name. Defaults to level-name from server.properties, else 'world'.")
    parser.add_argument("-o", "--only", nargs="+", default=None,
                        help="Restrict deploy to specific pack folder names.")
    parser.add_argument("--no-reset", action="store_true",
                        help="Keep the existing world. World reset is ON by default "
                             "(wipe + regenerate from scratch on next boot).")
    parser.add_argument("--no-backup", action="store_true",
                        help="On reset, delete instead of moving the world aside. DESTRUCTIVE.")
    parser.add_argument("--no-patch", action="store_true",
                        help="Skip the integration/dedup patch step (patch-all.ps1).")
    args = parser.parse_args(argv)

    server_base = Path(args.server_base)
    level_name = resolve_level_name(server_base, args.level_name)
    world_dir = server_base / level_name
    dest = world_dir / "datapacks"

    do_reset = not args.no_reset
    if do_reset:
        reset_world(world_dir, server_base, level_name, args.no_backup)

    if not world_dir.is_dir():
        print(f"ERROR: Server world folder not found: {world_dir} "
              f"(check --server-base / --level-name).", file=sys.stderr)
        return 1

    dest.mkdir(parents=True, exist_ok=True)

    run_patches(args.no_patch)

    print(f"=== Deploying datapacks to {dest} ===")
    packs = discover_packs(args.only, do_reset)
    if not packs:
        print("  No datapacks found (expected subfolders of datapacks/ containing pack.mcmeta).")
        return 0

    for pack in packs:
        count = deploy_pack(pack, dest)
        print(f"  deployed: {pack.name} ({count} files)")

    print("Datapack deploy complete.")
    if do_reset:
        print("World was reset; boot the server to generate it FROM SCRATCH")
        print("with these datapacks active (server boots are user-triggered).")
    else:
        print("Reminder: run /reload (data-only changes) or restart the server")
        print("(worldgen/dimension changes regenerate only in newly generated chunks).")
        print("Use --reset-world to wipe the world and regenerate from scratch.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
