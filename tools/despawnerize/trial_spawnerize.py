"""Replace farmable structure spawners with non-farmable trial spawners.

Problem: several third-party structure packs ship real ``minecraft:spawner``
blocks inside their templates. A plain spawner keeps re-spawning mobs forever,
which turns those structures into farms and breaks vanilla balance.

Fix: convert every plain spawner in a non-exempt template into a vanilla
``minecraft:trial_spawner``. A trial spawner only activates when a player is
near, spawns a capped wave, then enters a long cooldown (30 min by default) and
will not re-fire while its mobs live, so it cannot be farmed - yet the structure
still greets the player with mobs and rewards the fight with loot.

Rules (agreed with the maintainer):
  * Carry the spawner's mob over (its ``SpawnPotentials`` map directly onto the
    trial spawner's ``spawn_potentials``); size the wave from ``SpawnCount``.
  * Eject loot that is "one step up" from the structure's own chests: scan the
    template for container ``LootTable``s, find the best one on a vanilla loot
    ladder, and eject the next rung up. Structures with no/unknown (modded)
    chest loot eject the trial-chamber reward table as a sensible default. The
    loot is earned (you must defeat the wave), so it is not "free".
  * EXCEPTION - leave spawners untouched in "underground dungeon" templates,
    detected by the file path/name containing ``dungeon``, ``mineshaft`` or
    ``ancient`` (case-insensitive). Vanilla itself uses spawners there.
  * Non-living "trap" spawners (firework_rocket, potion, evoker_fangs, ...) are
    NOT mob farms and make no sense as trial spawners, so they are left as-is.

Because all ``minecraft:spawner`` blocks in a template share a single palette
entry, a NEW ``minecraft:trial_spawner`` palette entry is appended and only the
converted blocks are repointed at it; skipped (trap) spawners keep the original
entry. The pass is idempotent: once converted no plain spawner remains.

It edits the repo's quarantined datapack sources in place (same contract as the
``datapacks/_patch-*.ps1`` scripts).
"""

from __future__ import annotations

import argparse
import gzip
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import nbt_io as N  # noqa: E402

# Entity ids that are not living mobs - leave their spawners as plain spawners.
NON_LIVING = {
    "minecraft:firework_rocket",
    "minecraft:potion",
    "minecraft:evoker_fangs",
    "minecraft:arrow",
    "minecraft:spectral_arrow",
    "minecraft:snowball",
    "minecraft:small_fireball",
    "minecraft:fireball",
    "minecraft:dragon_fireball",
    "minecraft:wither_skull",
    "minecraft:item",
    "minecraft:experience_orb",
    "minecraft:tnt",
    "minecraft:falling_block",
    "minecraft:area_effect_cloud",
}

EXCEPTION_KEYWORDS = ("dungeon", "mineshaft", "ancient")
EXCLUDE_DIR_PARTS = (os.sep + "leaf-worldgen" + os.sep,)

# Vanilla loot ladder, roughly ascending in value. The trial spawner ejects the
# rung ONE ABOVE the best chest table found in the same structure.
LOOT_LADDER = [
    "minecraft:chests/igloo_chest",
    "minecraft:chests/abandoned_mineshaft",
    "minecraft:chests/simple_dungeon",
    "minecraft:chests/desert_pyramid",
    "minecraft:chests/jungle_temple",
    "minecraft:chests/shipwreck_supply",
    "minecraft:chests/shipwreck_map",
    "minecraft:chests/pillager_outpost",
    "minecraft:chests/ruined_portal",
    "minecraft:chests/nether_bridge",
    "minecraft:chests/stronghold_corridor",
    "minecraft:chests/stronghold_crossing",
    "minecraft:chests/bastion_other",
    "minecraft:chests/stronghold_library",
    "minecraft:chests/shipwreck_treasure",
    "minecraft:chests/woodland_mansion",
    "minecraft:chests/buried_treasure",
    "minecraft:chests/bastion_hoglin_stable",
    "minecraft:chests/bastion_bridge",
    "minecraft:chests/trial_chambers/reward_common",
    "minecraft:chests/trial_chambers/reward",
    "minecraft:chests/bastion_treasure",
    "minecraft:chests/end_city_treasure",
    "minecraft:chests/trial_chambers/reward_rare",
    "minecraft:chests/trial_chambers/reward_unique",
]
DEFAULT_EJECT = "minecraft:chests/trial_chambers/reward"
LADDER_INDEX = {t: i for i, t in enumerate(LOOT_LADDER)}


def structure_key(path):
    """The structure-relative identity: everything after the last ``structure``/
    ``structures`` path segment (so the pack folder and namespace, which often
    contain words like "dungeons", do NOT count toward the exemption match)."""
    parts = path.replace("/", os.sep).split(os.sep)
    idx = None
    for i, p in enumerate(parts):
        if p.lower() in ("structure", "structures"):
            idx = i
    tail = parts[idx + 1:] if idx is not None else parts[-1:]
    return os.sep.join(tail).lower()


def is_exempt(path):
    key = structure_key(path)
    return any(k in key for k in EXCEPTION_KEYWORDS)


def deep_copy(value):
    tag, v = value
    if tag == "compound":
        return (tag, {k: deep_copy(c) for k, c in v.items()})
    if tag == "list":
        etype, items = v
        return (tag, (etype, [deep_copy(it) for it in items]))
    if tag in ("byte_array", "int_array", "long_array"):
        return (tag, list(v))
    return (tag, v)


def palette_names(palette_items):
    out = []
    for it in palette_items:
        nm = N.comp(it).get("Name")
        out.append(nm[1] if nm else None)
    return out


def ensure_trial_palette(palette_items):
    """Append (if needed) a trial_spawner palette entry; return its index."""
    for i, it in enumerate(palette_items):
        d = N.comp(it)
        nm = d.get("Name")
        if nm and nm[1] == "minecraft:trial_spawner":
            return i
    entry = N.tcompound({
        "Name": N.tstring("minecraft:trial_spawner"),
        "Properties": N.tcompound({
            "ominous": N.tstring("false"),
            "trial_spawner_state": N.tstring("inactive"),
        }),
    })
    palette_items.append(entry)
    return len(palette_items) - 1


def primary_entity_id(block_entity):
    """Return the mob id a spawner would spawn, or None."""
    bed = N.comp(block_entity)
    if "SpawnData" in bed:
        sdd = N.comp(bed["SpawnData"])
        if "entity" in sdd:
            ed = N.comp(sdd["entity"])
            if "id" in ed:
                return ed["id"][1]
    if "SpawnPotentials" in bed:
        _, sp = N.listval(bed["SpawnPotentials"])
        if sp:
            dd = N.comp(sp[0])
            if "data" in dd:
                e = N.comp(dd["data"]).get("entity")
                if e and "id" in N.comp(e):
                    return N.comp(e)["id"][1]
    return None


def collect_chest_loot_tables(blocks):
    tables = set()
    for blk in blocks:
        be = N.comp(blk).get("nbt")
        if be is None:
            continue
        bed = N.comp(be)
        lt = bed.get("LootTable")
        if lt and lt[0] == "string":
            tables.add(lt[1])
    return tables


def choose_eject_table(chest_tables):
    best = -1
    for t in chest_tables:
        if t in LADDER_INDEX:
            best = max(best, LADDER_INDEX[t])
    if best < 0:
        return DEFAULT_EJECT
    return LOOT_LADDER[min(best + 1, len(LOOT_LADDER) - 1)]


def short(value, default):
    return value[1] if value is not None else default


def build_trial_block_entity(block_entity, eject_table):
    """Translate a plain spawner block entity into a trial_spawner one."""
    bed = N.comp(block_entity)

    # spawn_potentials: reuse the spawner's list verbatim (same {weight,data{entity}}
    # shape); fall back to a single entry built from SpawnData.
    if "SpawnPotentials" in bed:
        spawn_potentials = deep_copy(bed["SpawnPotentials"])
    else:
        sd = N.comp(bed["SpawnData"]) if "SpawnData" in bed else {}
        ent = deep_copy(sd["entity"]) if "entity" in sd else N.tcompound({})
        spawn_potentials = N.tlist(N.TAG_COMPOUND, [
            N.tcompound({"weight": N.tint(1), "data": N.tcompound({"entity": ent})})
        ])

    if "SpawnData" in bed:
        spawn_data = deep_copy(bed["SpawnData"])
    else:
        _, sp = N.listval(spawn_potentials)
        spawn_data = deep_copy(N.comp(sp[0])["data"])

    spawn_count = short(bed.get("SpawnCount"), 4)
    spawn_range = short(bed.get("SpawnRange"), 4)
    player_range = short(bed.get("RequiredPlayerRange"), 14)

    simultaneous = float(max(1, spawn_count))
    total = float(max(2, 2 * spawn_count))

    normal_config = N.tcompound({
        "spawn_range": N.tint(int(spawn_range)),
        "total_mobs": ("float", total),
        "simultaneous_mobs": ("float", simultaneous),
        "spawn_potentials": spawn_potentials,
        "loot_tables_to_eject": N.tlist(N.TAG_COMPOUND, [
            N.tcompound({"weight": N.tint(1), "data": N.tstring(eject_table)})
        ]),
    })

    return N.tcompound({
        "id": N.tstring("minecraft:trial_spawner"),
        "required_player_range": N.tint(int(player_range)),
        "normal_config": normal_config,
        "spawn_data": spawn_data,
    })


def process_file(path, dry_run=False):
    root_name, root_val = N.read_nbt(path)
    d = N.comp(root_val)
    if "palette" not in d or "blocks" not in d:
        return None
    _, palette_items = N.listval(d["palette"])
    names = palette_names(palette_items)
    spawner_idx = {i for i, n in enumerate(names) if n == "minecraft:spawner"}
    if not spawner_idx:
        return None

    _, blocks = N.listval(d["blocks"])
    eject_table = choose_eject_table(collect_chest_loot_tables(blocks))

    trial_idx = None
    converted = 0
    skipped_nonliving = 0
    for blk in blocks:
        bd = N.comp(blk)
        st = bd.get("state")
        if st is None or st[1] not in spawner_idx:
            continue
        be = bd.get("nbt")
        if be is None:
            continue
        ent_id = primary_entity_id(be)
        if ent_id is None or ent_id in NON_LIVING:
            skipped_nonliving += 1
            continue
        if trial_idx is None:
            trial_idx = ensure_trial_palette(palette_items)
        bd["state"] = N.tint(trial_idx)
        bd["nbt"] = build_trial_block_entity(be, eject_table)
        converted += 1

    if converted == 0:
        return {"converted": 0, "skipped_nonliving": skipped_nonliving, "eject": eject_table}

    if not dry_run:
        N.write_nbt(path, root_name, root_val, gzipped=True)
    return {"converted": converted, "skipped_nonliving": skipped_nonliving, "eject": eject_table}


def iter_templates(root):
    for dp, _, fs in os.walk(root):
        if any(part in (dp + os.sep) for part in EXCLUDE_DIR_PARTS):
            continue
        for f in fs:
            if f.endswith(".nbt"):
                yield os.path.join(dp, f)


def has_spawner(path):
    raw = open(path, "rb").read()
    if raw[:2] == b"\x1f\x8b":
        raw = gzip.decompress(raw)
    return b"minecraft:spawner" in raw and b"minecraft:mob_spawner" in raw


def main(argv=None):
    ap = argparse.ArgumentParser(description="Replace farmable structure spawners with trial spawners.")
    ap.add_argument("--datapacks", "-d", default=None, help="datapacks root (default: repo datapacks dir)")
    ap.add_argument("--dry-run", action="store_true", help="report only, do not write files")
    ap.add_argument("--only", "-o", default=None, help="comma-separated pack folder names to limit to")
    args = ap.parse_args(argv)

    here = os.path.dirname(os.path.abspath(__file__))
    repo = os.path.dirname(os.path.dirname(here))
    root = args.datapacks or os.path.join(repo, "datapacks")
    only = set(args.only.split(",")) if args.only else None

    tot_files = tot_conv = tot_skip = exempt_files = 0
    for path in iter_templates(root):
        rel = os.path.relpath(path, root)
        pack = rel.split(os.sep)[0]
        if only and pack not in only:
            continue
        if not path.endswith(".nbt"):
            continue
        if not has_spawner(path):
            continue
        if is_exempt(path):
            exempt_files += 1
            continue
        try:
            res = process_file(path, dry_run=args.dry_run)
        except Exception as e:  # noqa: BLE001
            print("ERROR %s: %r" % (rel, e))
            continue
        if not res or res["converted"] == 0:
            if res and res["skipped_nonliving"]:
                tot_skip += res["skipped_nonliving"]
            continue
        tot_files += 1
        tot_conv += res["converted"]
        tot_skip += res["skipped_nonliving"]
        print("  %-66s spawners->trial=%d eject=%s" % (rel, res["converted"], res["eject"]))

    mode = "DRY-RUN" if args.dry_run else "applied"
    print("\n[%s] files changed=%d spawners->trial=%d non-living spawners left=%d "
          "exempt (dungeon/mineshaft/ancient) files=%d"
          % (mode, tot_files, tot_conv, tot_skip, exempt_files))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
