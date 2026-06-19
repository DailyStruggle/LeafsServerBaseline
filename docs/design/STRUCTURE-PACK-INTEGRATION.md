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

- **Keep every option, select at runtime.** No pack is deleted or stripped to make
  room for another. Overlapping providers (villages, mineshafts, strongholds,
  dungeons, naval set-pieces) are allowed to coexist; an operator turns individual
  packs on/off at runtime instead.
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

`datapacks/patch-all.ps1` runs every `_patch-*.ps1` and is invoked automatically by
`deploy-datapacks.ps1` before the copy step (skip with `-NoPatch`). The patches edit
the repo datapack source in place and are all idempotent (safe to re-run).

> Note: a structure's `biomes` field in **list** form may contain only plain biome
> ids, not `#tag` references. That is why integration targets the biome **tags** the
> structures point at, rather than the structures' `biomes` lists.

---

## Adding a new pack later

1. Extract + quarantine it like the others (README + `.gitignore`).
2. Check how it places: if it uses vanilla `#minecraft:is_*` categories, it is
   already covered by mechanism 1 - nothing to do. If it uses its own collection
   tags, add a `datapacks/_patch-<name>.ps1` (mechanism 2); `patch-all.ps1`
   discovers it automatically.
3. If two packs fill the same role, leave both enabled (variety) and rely on the
   per-pack runtime toggle to pick at runtime.
