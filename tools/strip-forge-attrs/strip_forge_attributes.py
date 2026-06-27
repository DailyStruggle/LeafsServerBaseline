"""Strip Forge-only entity attributes from Minecraft structure (.nbt) templates.

Forge-converted structure packs (e.g. dungeons-and-taverns / nova_structures) bake
mob attribute lists that include Forge-namespaced ids such as ``forge:swim_speed``.
On a vanilla/Paper server these are unknown registry keys, so the template loader
logs repeated ``Failed to decode value ... Unknown registry key ... forge:swim_speed``
serialization warnings while placing the structure.

This tool walks every entity's ``attributes`` list inside the template and removes
any attribute entry whose ``id`` is in the ``forge:`` namespace (no vanilla
equivalent exists, so the mob simply falls back to its default). Only files that
actually change are rewritten, preserving the original gzip on-disk format.

Usage:
    python strip_forge_attributes.py <root-dir> [--apply] [--namespace forge]

Without ``--apply`` it runs a dry run and only reports what would change.
"""

from __future__ import annotations

import argparse
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "despawnerize"))

import nbt_io  # noqa: E402


# Attribute-list key -> id key within each entry. Covers both the modern
# (1.21+) lowercase form and the legacy capitalised form that older / Forge
# converted templates store (DataFixerUpper upgrades it on load, then trips on
# the forge-namespaced id).
_ATTR_KEYS = (("attributes", "id"), ("Attributes", "Name"))


def _strip_attributes(value, namespace_prefix: str, counter: dict) -> None:
    """Recursively walk an NBT value, removing forge-namespaced attribute entries."""
    tag = value[0]
    if tag == "compound":
        d = value[1]
        for list_key, id_key in _ATTR_KEYS:
            attrs = d.get(list_key)
            if attrs is None or attrs[0] != "list":
                continue
            etype, items = attrs[1]
            kept = []
            for item in items:
                if item[0] == "compound":
                    idv = item[1].get(id_key)
                    if idv is not None and idv[0] == "string" and idv[1].startswith(namespace_prefix):
                        counter["removed"] += 1
                        continue
                kept.append(item)
            if len(kept) != len(items):
                d[list_key] = ("list", (etype, kept))
        for child in d.values():
            _strip_attributes(child, namespace_prefix, counter)
    elif tag == "list":
        _etype, items = value[1]
        for item in items:
            _strip_attributes(item, namespace_prefix, counter)


def process_file(path: str, namespace_prefix: str, apply: bool) -> int:
    try:
        root_name, root_value = nbt_io.read_nbt(path)
    except Exception as exc:  # noqa: BLE001 - report and skip unreadable files
        print("  SKIP (read error): %s (%s)" % (path, exc))
        return 0
    counter = {"removed": 0}
    _strip_attributes(root_value, namespace_prefix, counter)
    if counter["removed"] and apply:
        nbt_io.write_nbt(path, root_name, root_value, gzipped=True)
    return counter["removed"]


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("root", help="directory to scan recursively for .nbt files")
    ap.add_argument("--apply", action="store_true", help="write changes (default: dry run)")
    ap.add_argument("--namespace", default="forge", help="attribute namespace to strip (default: forge)")
    args = ap.parse_args()

    prefix = args.namespace.rstrip(":") + ":"
    files_changed = 0
    total_removed = 0
    for dirpath, _dirnames, filenames in os.walk(args.root):
        for name in filenames:
            if not name.endswith(".nbt"):
                continue
            full = os.path.join(dirpath, name)
            removed = process_file(full, prefix, args.apply)
            if removed:
                files_changed += 1
                total_removed += removed
                print("  %s: removed %d %s* attribute(s)" % (full, removed, prefix))

    verb = "removed" if args.apply else "would remove"
    print("\n%s %d %s* attribute entry(ies) across %d file(s)." % (
        verb.capitalize(), total_removed, prefix, files_changed))
    if not args.apply and files_changed:
        print("Re-run with --apply to write the changes.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
