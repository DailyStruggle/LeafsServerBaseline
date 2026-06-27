"""Rename the removed ``minecraft:potion`` ENTITY type baked into structure (.nbt) templates.

In 1.21.5 the single thrown-potion entity ``minecraft:potion`` was split into
``minecraft:splash_potion`` and ``minecraft:lingering_potion``. Structure templates
that were authored/converted before that split still bake the old id into their
entity data, typically as a passenger (``Passengers[0].id``). On a modern server the
entity loader logs repeated ``Skipping Entity with id minecraft:potion`` /
``Failed to decode value '"minecraft:potion"' from field 'id': Unknown registry key
... minecraft:entity_type`` serialization warnings and silently drops the entity.

This tool walks every ENTITY compound in the template (the top-level ``entities`` list
and any nested ``Passengers`` lists) and rewrites an entity ``id`` of
``minecraft:potion`` to ``minecraft:splash_potion`` (the closest equivalent of the old
thrown-potion entity). It deliberately does NOT touch ``id`` fields elsewhere -- in
particular the ITEM id ``minecraft:potion`` inside chests / containers stays valid and
untouched. Only files that actually change are rewritten, preserving the gzip format.

Usage:
    python fix_potion_entity.py <root-dir> [--apply] [--replacement minecraft:splash_potion]

Without ``--apply`` it runs a dry run and only reports what would change.
"""

from __future__ import annotations

import argparse
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "despawnerize"))

import nbt_io  # noqa: E402

_OLD_ID = "minecraft:potion"


def _fix_entity_compound(value, replacement: str, counter: dict) -> None:
    """``value`` is a ("compound", dict) representing a single ENTITY.

    Rewrite its own ``id`` if it is the removed potion entity, then recurse into
    any ``Passengers`` list (each element is itself an entity).
    """
    if value[0] != "compound":
        return
    d = value[1]
    idv = d.get("id")
    if idv is not None and idv[0] == "string" and idv[1] == _OLD_ID:
        d["id"] = ("string", replacement)
        counter["renamed"] += 1
    passengers = d.get("Passengers")
    if passengers is not None and passengers[0] == "list":
        _etype, items = passengers[1]
        for item in items:
            _fix_entity_compound(item, replacement, counter)


def _walk(value, replacement: str, counter: dict) -> None:
    """Walk the template, dispatching ENTITY compounds to ``_fix_entity_compound``.

    Two entity carriers are recognised:
      * the structure-level ``entities`` list (each element wraps the entity in
        an ``nbt`` compound); and
      * any ``entity`` compound, which is how (trial) spawner block entities store
        the mob to spawn (``SpawnData.entity``, ``SpawnPotentials[].data.entity``,
        ``spawn_data.entity``, ``normal_config/ominous_config.spawn_potentials[].data.entity``).

    ``_fix_entity_compound`` only rewrites an entity's own ``id`` and recurses into
    its ``Passengers``; it never touches item ids (``Items``/``HandItems``/``Item``),
    so the still-valid ITEM id ``minecraft:potion`` is preserved.
    """
    tag = value[0]
    if tag == "compound":
        d = value[1]
        entities = d.get("entities")
        if entities is not None and entities[0] == "list":
            _etype, items = entities[1]
            for item in items:
                # Each structure ``entities`` element wraps the entity under ``nbt``.
                if item[0] == "compound":
                    ent = item[1].get("nbt")
                    if ent is not None:
                        _fix_entity_compound(ent, replacement, counter)
        spawner_entity = d.get("entity")
        if spawner_entity is not None and spawner_entity[0] == "compound":
            _fix_entity_compound(spawner_entity, replacement, counter)
        for child in d.values():
            _walk(child, replacement, counter)
    elif tag == "list":
        _etype, items = value[1]
        for item in items:
            _walk(item, replacement, counter)


def process_file(path: str, replacement: str, apply: bool) -> int:
    try:
        root_name, root_value = nbt_io.read_nbt(path)
    except Exception as exc:  # noqa: BLE001 - report and skip unreadable files
        print("  SKIP (read error): %s (%s)" % (path, exc))
        return 0
    counter = {"renamed": 0}
    _walk(root_value, replacement, counter)
    if counter["renamed"] and apply:
        nbt_io.write_nbt(path, root_name, root_value, gzipped=True)
    return counter["renamed"]


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("root", help="directory to scan recursively for .nbt files")
    ap.add_argument("--apply", action="store_true", help="write changes (default: dry run)")
    ap.add_argument("--replacement", default="minecraft:splash_potion",
                    help="entity id to substitute (default: minecraft:splash_potion)")
    args = ap.parse_args()

    files_changed = 0
    total_renamed = 0
    for dirpath, _dirnames, filenames in os.walk(args.root):
        for name in filenames:
            if not name.endswith(".nbt"):
                continue
            full = os.path.join(dirpath, name)
            renamed = process_file(full, args.replacement, args.apply)
            if renamed:
                files_changed += 1
                total_renamed += renamed
                print("  %s: renamed %d potion entity id(s)" % (full, renamed))

    verb = "renamed" if args.apply else "would rename"
    print("\n%s %d %s entity id(s) -> %s across %d file(s)." % (
        verb.capitalize(), total_renamed, _OLD_ID, args.replacement, files_changed))
    if not args.apply and files_changed:
        print("Re-run with --apply to write the changes.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
