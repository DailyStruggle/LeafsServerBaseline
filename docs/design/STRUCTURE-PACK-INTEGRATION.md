# Structure-Pack Integration & Deduplication

Date: 2026-06-19

How the extracted third-party structure datapacks are deduplicated and wired
("integrated") into the custom `leaf:*` overworld biomes, and how variants are
selected at runtime.

The packs themselves are sourcing-only and quarantined (see each
`datapacks/<pack>/README.md` and `.gitignore`); this doc covers only the
integration/dedup layer, which IS tracked.

---

## Goals

- **Keep every option, select per spawn where it matters.** No pack is deleted or
  stripped to make room for another. Overlapping providers (mineshafts, strongholds,
  dungeons, naval set-pieces) are allowed to coexist. For **villages** specifically,
  coexistence is upgraded to a single weighted selection so each village location
  resolves to exactly one village type (see *Per-spawn variant selection* below)
  instead of several village grids running in parallel.
- **Runtime on/off as a coarse control.** An operator can still enable/disable whole
  packs at runtime; that is the coarse selector, complementary to the per-spawn
  merge for villages.
- **Make the structures actually generate in our custom biomes.** Our overworld is
  built from `leaf:*` biomes, which are not vanilla, so packs keyed off vanilla
  biome ids/tags would otherwise never place in them.
- **Maximise variety.** Prefer merging contributors over replacing them.

---

## Runtime selection (per-pack toggle)

Each pack deploys as its own datapack folder under
`<ServerBase>\<LevelName>\datapacks\<pack>`. Minecraft enables/disables datapacks
independently at runtime:

```
/datapack list
/datapack disable "file/<pack>"
/datapack enable  "file/<pack>"
```

So "select one at runtime" = enable the pack(s) you want for a given world and
disable the rest; nothing has to be removed from the repo. Disabling a worldgen
pack only affects **newly generated** chunks.

This is a *coarse*, whole-pack control. For roles where you want the game itself to
pick one of several variants **per spawn** (villages), see the next section.

---

## Per-spawn variant selection (villages)

Whole-pack on/off is too coarse for villages: by default each village pack ships its
own `worldgen/structure_set` with its own `salt`, i.e. its own placement grid, so the
game rolls each pack **independently** and several village generators run side by
side (overlap possible) rather than "one village type chosen per location".

A `structure_set` is what actually places villages: it defines a `random_spread` grid
and a **weighted** `structures` list. For each placement slot the game picks one entry
by weight; if the chosen structure's biome does not match, it picks another from the
list until one is valid (per the Minecraft Wiki). So putting every village in **one**
set yields exactly one village per slot, biome-filtered, with no rarity penalty and no
overlap.

We therefore override vanilla `minecraft:villages` to list all compatible village
structures under a single grid:

```
datapacks/leaf-worldgen/data/minecraft/worldgen/structure_set/villages.json
```

It contains the vanilla 5 villages + Towns & Towers (~28) + Dungeons & Taverns (3),
each at equal weight. `datapacks/_patch-merge-villages.ps1` then deletes each pack's
own village `structure_set` (T&T `towns.json`, DnT `villages_birch/jungle/swamp.json`)
so those structures place **only** through the merged set (a structure generates only
if some set references it; the structure/template-pool assets are untouched).

**CTOV is intentionally excluded:** its village structures use
`"type": "lithostitched:jigsaw"` and place via Lithostitched worldgen modifiers, both
of which require the Lithostitched **mod**. On a vanilla/Paper server they do not load
at all, so they cannot join the merged set regardless.

---

## Deduplication

A scan of all extracted packs found only **6** cross-pack resource-path collisions
out of ~16.8k resources. Five are biome/structure/function **tags**, which Minecraft
**merges** across datapacks as long as none sets `"replace": true` - so they are not
real clobbers and are left to merge (this is exactly what gives us runtime variety).

The one genuine clobber, now fixed:

- `towns-and-towers .../has_structure/pillager_outpost.json` shipped `"replace": true`
  (a single value, `minecraft:plains`). A replacing tag wipes every other
  pillager-outpost contributor (vanilla, Structory: Towers, ...) at load. Flipped to
  `"replace": false` by `datapacks/_patch-towns-and-towers.ps1` so it merges.

Cosmetic-only residual collision (not gameplay-affecting, left as-is):
`c/worldgen/structure_icons.json` between `structory` and `structory-towers` (Cristel
Lib map icons; load order decides which icon set wins).

---

## Integration: two mechanisms

### 1. Vanilla biome-category membership (covers most packs, no per-pack code)

Most packs (When Dungeons Arise, YUNG's Better Dungeons/Strongholds, Explorify, ...)
place via vanilla biome-category tags - `#minecraft:is_forest`, `is_taiga`,
`is_badlands`, `is_mountain`, `is_beach`, etc. We add the `leaf:*` biomes into those
tags **once**, with `"replace": false` so they merge with vanilla:

```
datapacks/leaf-worldgen/data/minecraft/tags/worldgen/biome/is_forest.json
                                                          /is_taiga.json
                                                          /is_badlands.json
                                                          /is_mountain.json
                                                          /is_beach.json
```

Current leaf -> category mapping:

| Category tag | leaf biomes |
|---|---|
| `is_forest` | forest_core/body/edge, dark_forest_core/body/edge, maple_forest(+_01..08), rainbow_forest_01..12, jacaranda_grove, lavender_fields |
| `is_taiga` | taiga_core/body/edge, redwood_grove, boreal_shield |
| `is_badlands` | ashen_plains |
| `is_mountain` | volcanic_mountain, volcanic_hot_springs, alpine_meadow |
| `is_beach` | glass_beach |

Because this is a single merging contribution, every category-driven pack picks the
`leaf:*` biomes up automatically - no per-pack edits, no duplication. `leaf:bayou`
(swamp) has no vanilla `is_*` category, so swamp-keyed structures are wired by the
patch scripts below instead.

### 2. Per-pack patch scripts (packs that use their own collection tags)

Packs that key off their own collection tags (Dungeons & Taverns'
`nova_structures:collections/*`, Structory's `has_structure/*`) or that ship a
clobbering tag get a dedicated, idempotent `datapacks/_patch-*.ps1` that splices
`leaf:*` biomes into the right tags:

- `_patch-dnt.ps1` - Dungeons & Taverns.
- `_patch-structory.ps1` - Structory + Structory: Towers.
- `_patch-towns-and-towers.ps1` - Towns & Towers dedup fix (above).
- `_patch-merge-villages.ps1` - removes each pack's own village `structure_set` so
  all villages place through the merged `minecraft:villages` set (see *Per-spawn
  variant selection*).
- `_patch-trial-spawners.ps1` - replaces farmable plain `minecraft:spawner` blocks
  inside the packs' structure templates with non-farmable vanilla trial spawners
  (see *Anti-farm: trial spawners* below).
- `_patch-remove-heavenly.ps1` - drops the airborne "heavenly" When Dungeons Arise
  structures (heavenly_challenger / heavenly_conqueror / heavenly_rider) from
  dungeons-arise's `major_structures.json` so they no longer generate.

`datapacks/patch-all.ps1` runs every `_patch-*.ps1` and is invoked automatically by
`deploy-datapacks.ps1` before the copy step (skip with `-NoPatch`). The patches edit
the repo datapack source in place and are all idempotent (safe to re-run).

> Note: a structure's `biomes` field in **list** form may contain only plain biome
> ids, not `#tag` references. That is why integration targets the biome **tags** the
> structures point at, rather than the structures' `biomes` lists.

---

## Anti-farm: trial spawners

Several packs ship real `minecraft:spawner` blocks inside their structure
templates. A plain spawner re-spawns mobs forever, turning those structures into
mob farms and breaking vanilla balance. `_patch-trial-spawners.ps1` (a thin
wrapper around the Python tool `tools/despawnerize/trial_spawnerize.py`, which
includes a small standalone NBT reader/writer `nbt_io.py`) rewrites every
affected structure `.nbt` so each farmable spawner becomes a vanilla
`minecraft:trial_spawner`: it only activates when a player is near, spawns a
capped wave, then enters a long cooldown and will not re-fire while its mobs
live - not farmable, but the structure still ambushes the player.

Conversion details:

- The spawner's mob carries over verbatim: its `SpawnPotentials` map onto the
  trial spawner's `normal_config.spawn_potentials`, and the wave size is derived
  from `SpawnCount` (`simultaneous_mobs = SpawnCount`, `total_mobs = 2x`).
- Loot is "one step up from adjacent chests": the tool scans the template for
  container `LootTable`s and ejects the next rung up a vanilla loot ladder
  (`loot_tables_to_eject`); structures with no/unknown (modded) chest loot eject
  `minecraft:chests/trial_chambers/reward` by default. The loot is earned (the
  wave must be defeated), so it is not free.
- **Exception - underground dungeons keep their spawners.** Templates whose
  structure-relative path/name contains `dungeon`, `mineshaft` or `ancient` are
  left untouched (vanilla itself uses spawners there). The match is on the path
  **after** the `structure/` folder, so pack folder names like `dungeons-arise`
  do not count.
- Non-living "trap" spawners (firework_rocket, potion, evoker_fangs, ...) are not
  mob farms and are left as plain spawners.
- Because all `minecraft:spawner` blocks share one palette entry, the tool
  appends a new `minecraft:trial_spawner` palette entry and repoints only the
  converted blocks; it is idempotent (no plain `mob_spawner` remains afterwards).
- Old (pre-1.21) mob NBT inside the carried-over spawn data is upgraded by
  Minecraft's DataFixerUpper on load, so the resulting trial spawners require a
  1.21+ server.

---

## Adding a new pack later

1. Extract + quarantine it like the others (README + `.gitignore`).
2. Check how it places: if it uses vanilla `#minecraft:is_*` categories, it is
   already covered by mechanism 1 - nothing to do. If it uses its own collection
   tags, add a `datapacks/_patch-<name>.ps1` (mechanism 2); `patch-all.ps1`
   discovers it automatically.
3. If two packs fill the same role, leave both enabled (variety) and rely on the
   per-pack runtime toggle to pick at runtime.
4. If you want the game to pick **one variant per spawn** for that role (as done for
   villages), add the new pack's structures to a single shared `structure_set` and
   extend `_patch-merge-villages.ps1` (or a sibling) to delete the pack's own set so
   it does not also place on an independent grid.
