# Pack Patches - private "special structures" additive overlay

This directory is the **additive overlay** for special structures (e.g. the
Towns & Towers-derived `village-meadow`). Unlike `pack-overlay`, which performs a
**whole-file override**, this layer **amends** the built pack: it copies purely
additive files in, then applies small **add/remove** patches to existing biome
JSON. It is applied by `iris/scripts/deploy-iris-pack.ps1` as "Layer 2.5", after
`pack-base` + `pack-overlay` are staged and before the BOM-strip step.

> **Policy: NOT supplied to consumers.** The contents here are derived from
> CC BY-NC-ND 4.0 source packs (see `iris/thirdparty-derived/README.md` and
> `docs/design/adr/ADR-004-third-party-derived-structure-assets.md`). Private,
> non-commercial use is permitted; the assets and patches MUST NOT be
> redistributed. Everything under this folder is git-ignored except this README.

## Layout

```
iris/pack-patches/
|- README.md            # tracked; this file
|- assets/              # git-ignored; purely ADDITIVE files merged into staging
|  |- objects/jigsaw/...        # .iob objects (new paths, never override base)
|  |- jigsaw-pieces/...
|  |- jigsaw-pools/...
|  |- jigsaw-structures/...
|- patches/            # git-ignored; *.patch.json files that AMEND staged files
   |- biomes/temperate/meadows.json.patch.json
```

- `assets/` is copied into `staging` with `-Force` but contains only **new**
  paths, so it never silently replaces a base/overlay file.
- `patches/` holds one `*.patch.json` per target file.

## Patch format

One target per file, suffix `*.patch.json`:

```json
{
    "target": "biomes/temperate/meadows.json",
    "patches": [
        {
            "op": "addUnique",
            "array": "jigsawStructures",
            "key": "structure",
            "value": { "structure": "village-meadow", "rarity": 1 }
        }
    ]
}
```

Supported ops (see `iris/scripts/apply-pack-patches.py`):

- `addUnique` - append `value` to `array` only if no element already has the same
  `key` value (idempotent).
- `remove` - drop every element of `array` whose `key` equals `match`.
- `set` - set a top-level `field` to `value`.

`rarity` follows the Iris convention: **lower = more common** (1 = appears in
nearly every instance of the biome; 800/1200/1750 = progressively rarer).

## Applying

Run by the deploy automatically. To apply manually onto a staging tree:

```powershell
python iris\scripts\apply-pack-patches.py --staging iris\staging --patches iris\pack-patches\patches
```

## Stub population pools (silencing `Can't find jigsaw pool: village/...`)

The Towns & Towers village pieces (and the vanilla Iris base village pieces) carry
jigsaw connectors that point at vanilla `minecraft:` "population" pools and one
modded pool. These pools are never shipped as Iris jigsaw pools: in vanilla they
spawn **entities** (cats, animals, an iron golem, biome villagers) or place a
**worldgen feature** (grass patches, decor), and the modded one belongs to the
Waystones mod. Iris has no converted `.iob` object for any of them, so at load it
logs `Can't find jigsaw pool: village/...` for each connector and attaches
nothing. Villages still generate; the lines are pure log noise.

To remove the spam we ship an **empty stub pool** (`{"pieces": []}`) at each
referenced key. An empty pool resolves cleanly (no warning) and still attaches
nothing, so generation is byte-for-byte unchanged. These live under
`assets/jigsaw-pools/village/...`. Do NOT add pieces to them unless a real,
converted `.iob` object exists for that content.

| Stub pool (key under `jigsaw-pools/`) | Refs | Vanilla / source intent | Why a stub (no Iris object) |
|---|---|---|---|
| `village/common/cats` | 47 | Spawns village cats | Entity spawn pool; no structure object |
| `village/common/animals` | 16 | Spawns assorted farm animals | Entity spawn pool; no structure object |
| `village/common/butcher_animals` | 8 | Spawns butcher pen animals | Entity spawn pool; no structure object |
| `village/common/sheep` | 14 | Spawns sheep | Entity spawn pool; no structure object |
| `village/common/iron_golem` | 18 | Spawns the village iron golem | Entity spawn pool; no structure object |
| `village/common/well_bottoms` | 1 | Vanilla well-bottom piece | Vanilla NBT not converted to `.iob` |
| `village/common/decor/decor_grass_patches` | 123 | `minecraft:patch_grass` / `patch_tall_grass` feature | `feature_pool_element`, not an object Iris renders |
| `village/plains/villagers` | 119 | Plains villager entities (nitwit/baby/unemployed) | Entity spawn pool; no structure object |
| `village/savanna/villagers` | 14 | Savanna villager entities | Entity spawn pool; no structure object |
| `village/snowy/villagers` | 8 | Snowy villager entities | Entity spawn pool; no structure object |
| `village/taiga/villagers` | 134 | Taiga villager entities | Entity spawn pool; no structure object |
| `village/meadow_swiss/villagers` | 1 | T&T meadow shepherd villager slot | Mod never ships this pool; no piece/object |
| `village/plains/decor` | 2 | Plains decor features | Feature/decor pool; no object |

> Genuinely convertible villager pieces are handled differently: see
> `village/beach_lighthouse/villager_lighthouse_master` and
> `village/sparse_jungle_polynesian/villager_village_chief`, which wrap real
> converted `.iob` objects instead of being empty.

## Waystone pool recreated without the Waystones block

`village/modded/waystones/waystone_default` is no longer an empty stub. The
Waystones mod ships a `t_and_t_waystones_patch` overlay whose structure piece is a
1x3x1 column: a jigsaw bottom block (its `final_state` is `minecraft:polished_andesite`,
the base pad) topped by two `waystones:waystone` blocks. The base T&T data only
has a 1x1x1 `minecraft:air` placeholder for the same key.

Since this server does not run the Waystones mod, the block `waystones:waystone`
would fail to place. We therefore converted the patch piece **without the
Waystones block**: the `.iob` keeps only the `polished_andesite` base pad and the
bottom jigsaw connector is preserved. Result: where a waystone would stand, the
village now gets a small polished-andesite pad instead of a missing/broken block,
and the `Can't find jigsaw pool: village/modded/waystones/waystone_default`
warning is gone.

Assets (all under `assets/`):

- `objects/jigsaw/village/modded/waystones/waystone_default.iob` (1x3x1, single `polished_andesite`)
- `jigsaw-pieces/village/modded/waystones/waystone_default.json`
- `jigsaw-pools/village/modded/waystones/waystone_default.json` (wraps the piece)
