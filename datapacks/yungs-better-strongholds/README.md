# YUNG's Better Strongholds (third-party derived assets)

Overhauled, jigsaw-assembled strongholds (`betterstrongholds`) by **YUNGNICKYOUNG**.
Extracted from the Fabric mod jar and kept here so the 97 `.nbt` structure pieces can be
reused as plain NBT structures on our server.

## License

- Source license: bundled `LICENSE_YungsBetterStrongholds` is the **GNU LGPL v3**.
  LGPL permits use, modification, and redistribution provided the license/source terms
  are honoured, so - unlike the All-Rights-Reserved / CC BY-NC-ND packs in this repo -
  these assets are *legally* committable.
- We nonetheless **git-ignore** them (track only this `README.md`) for repo-leanness and
  consistency with the other extracted packs. This is policy, not a license restriction;
  they may be committed later if desired.
- Keep attribution: credit YUNGNICKYOUNG / YUNG's Better Strongholds and retain the LGPL
  notice in any use.

## Provenance

- Source: `YungsBetterStrongholds-1.21.4-Fabric-5.4.0.jar` (Fabric mod, version 5.4.0,
  targets MC 1.21.4).
- Extracted from `C:\Users\lxgol\Downloads\` on 2026-06-19.

## What was extracted

- The mod jar's full `data/` tree plus the original `pack.mcmeta` (`pack_format` 46).
  The Java code (`com/`), mixins/refmap, `assets/`, `META-INF/`, `fabric.mod.json` and
  logos were intentionally NOT extracted - only the datapack-side, reusable structure
  assets.
- 97 `.nbt` structure pieces under `data/betterstrongholds/structure/<group>/`, grouped
  by role: `rooms` (16), `terminators` (27), `statues` (9), `hall_doorways` (7),
  `hallways` (5), `stairs` (5), `walls` (2), `starts` (1), `portal_rooms` (1), and
  `treasure/` corner/portal/wall (24 total).
- 37 supporting JSONs: `worldgen/` (jigsaw `template_pool`, `processor_list`,
  `structure`, `structure_set`), `loot_table/`, and `tags/` - retained so a piece's
  jigsaw assembly and loot can be reused/adapted.

## Reuse as NBT structures

- The raw `.nbt` files are standard Minecraft structure NBT and can be loaded directly via
  a Structure Block (Load mode, name `betterstrongholds:<group>/<piece>`) once this is
  deployed as a datapack, or copied into any other pack's `data/<namespace>/structure/`
  path.
- Jigsaw-assembled structures need their matching `worldgen/template_pool` entries under
  `data/betterstrongholds/worldgen/`.

## Deploy

Auto-discovered by `datapacks/deploy-datapacks.ps1`. Worldgen/structure changes only appear
in **newly generated chunks**; server boots remain user-triggered.
