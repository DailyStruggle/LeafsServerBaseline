# Provenance: Towns & Towers (derived assets)

> This whole directory is git-ignored except the top-level quarantine `README.md`.
> The assets here are a **private, non-commercial derivative** and MUST NOT be
> redistributed or published (see ADR-004 and `../README.md`).

## Source

- **Pack:** Towns & Towers (`towns_and_towers` / `kaisyn` namespaces)
- **Jar:** `t_and_t-fabric-neoforge-1.13.9.jar`
- **Authors:** see `raw/CREDITS.txt` (extracted from the jar)
- **License:** CC BY-NC-ND 4.0 (jar `LICENSE`). NC satisfied (private server);
  ND satisfied as long as the derivative is never Shared.
- **Runtime dependency in the mod:** Cristel Lib (placement glue only - replaced by
  Iris, not needed for the extracted data).

## What was extracted (`raw/`)

The `village_meadow` set only:
- 35 `.nbt` structure pieces under `data/kaisyn/structure/village/meadow_swiss/`
- 6 `data/kaisyn/worldgen/template_pool/village/meadow_swiss/*.json`
- `data/towns_and_towers/worldgen/structure/village_meadow.json`

## How it was converted (`iris/`)

Tooling (tracked, in `iris/scripts/`):
- `nbt_to_iris.py` - reads each `.nbt` (gzip + vanilla NBT), writes an Iris V2
  `.iob` object, and emits a `jigsaw-pieces/*.json` whose `connectors` are derived
  from the `minecraft:jigsaw` blocks (name/target -> name/targetName,
  `pool` -> Iris `pools`, `orientation` front -> Iris `direction`, position centered
  as `orig - size/2`). Also rewrites the template pools and the jigsaw structure.
- `iob_inspect.py` - round-trip verifier for produced `.iob` files.
- `jigsaw_validate.py` - loader-mirroring resolver: walks structure -> pools ->
  pieces -> `.iob` objects and reports unresolved references.

Command used:

```
python iris\scripts\nbt_to_iris.py ^
  --raw  iris\thirdparty-derived\towns-and-towers\raw ^
  --out  iris\thirdparty-derived\towns-and-towers\iris ^
  --namespace kaisyn --set village/meadow_swiss ^
  --structure-json iris\thirdparty-derived\towns-and-towers\raw\data\towns_and_towers\worldgen\structure\village_meadow.json ^
  --structure-key village-meadow
```

Result: 35 pieces -> 35 `.iob` + 35 piece JSON (297 jigsaw connectors total),
6 pools, 1 `jigsaw-structures/village-meadow.json` (start pool = town_centers).

## Wiring verification (does it resolve like Iris would?)

`jigsaw_validate.py --structure village-meadow --root <this iris dir>`:

```
reachable pieces : 32
reachable pools  : 10
WARNINGS (6) - tolerable: missing population pools (villagers/cats/sheep/
  iron_golem + a modded waystone pool) - Iris simply attaches nothing there.
OK - every reachable piece resolves to an .iob object. Graph wires.
```

A prior bug emitted the `jigsaw/` prefix on pool/structure piece refs, which Iris
cannot resolve (only the piece `.object` is prefixed). Fixed in `nbt_to_iris.py`;
see `docs/scratch/LESSONS_LEARNED.md` (2026-06-06).

This is a STATIC resolution proof only. An actual in-game spawn still requires:
1. Deploying these assets into the ACTIVE Iris pack (not this git-ignored
   quarantine) - i.e. merge `objects/`, `jigsaw-pieces/`, `jigsaw-pools/`,
   `structures/` into the live `overworld` pack on the server.
   (Iris 4.0 loads the flat `structures/` folder - `IrisStructure` - not the 3.x
   `jigsaw-structures/` folder; see `docs/design/IRIS-V4-STRUCTURES.md`.)
2. Referencing `village-meadow` from a biome's `structures` array (an
   `IrisStructurePlacement` with `distribution`/`spacing`/`separation`/`salt`),
   per the New World theme (meadow/plains-adjacent, common: spacing 32 / sep 8).
3. A running Paper/Purpur + Iris server with a freshly generated world; confirm
   with `/iris tp` to the structure (`/locate` will NOT find it - these are
   modded structures with no real vanilla key, so the v4 `IrisStructure` omits
   `vanillaSource`).

## Known limitations (fidelity)

- **Tile entities are dropped** (chests/loot, signs). The `.iob` `states` table is
  written empty because that needs Iris' `TileData` binary format. Container loot is
  handled separately by Iris.
- A few connectors reference optional external pools that ship with T&T data (e.g.
  `village/modded/waystones/...`). With no matching pool file, Iris simply has nothing
  to attach there - harmless.
- Iris jigsaw is not byte-for-byte identical to vanilla; final placement should be
  spot-checked in-game with `/iris` before wiring these pools into live biomes
  (Rule D-005: get approval before editing biome JSONs).

## Next steps (not done here)

1. In-game `/iris` placement test of `village-meadow` to confirm fidelity.
2. Optionally extend tooling to emit Iris `TileData` so chests/signs survive.
3. Per the New World / Old World theme, assign `village-meadow` (New World) to
   meadow/plains-adjacent biomes at common rarity - only after the placement test
   and explicit approval.
