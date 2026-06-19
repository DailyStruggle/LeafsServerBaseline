# Stoneholm (third-party derived assets)

Underground villages (`stoneholm`) by **TheGrimsey**. Extracted from the Fabric mod jar
and kept here so the 74 `.nbt` structure pieces can be reused as plain NBT structures on
our server.

## License

- Source license: bundled `LICENSE_stoneholm` is the **MIT License** (Copyright (c) 2021
  TheGrimsey). MIT permits use, modification, and redistribution **with attribution**
  (keep the copyright + permission notice).
- So, unlike the All-Rights-Reserved / CC BY-NC-ND packs in this repo, these assets are
  *legally* committable. We nonetheless **git-ignore** them (track only this `README.md`)
  for repo-leanness and consistency with the other extracted packs. This is policy, not a
  license restriction - they may be committed later if desired.
- Keep attribution: credit TheGrimsey / Stoneholm and include the MIT notice in any use.

## Provenance

- Source: `stoneholm-2.0.0.jar` (Fabric mod, version 2.0.0, targets MC 1.20.*).
- Extracted from `C:\Users\lxgol\Downloads\` on 2026-06-19.
- Homepage / sources: https://github.com/TheGrimsey/Stoneholm/

## What was extracted

- The mod jar's `data/stoneholm/` tree only. The Java code (`net/`), mixins,
  `assets/`, `META-INF/`, `fabric.mod.json` and refmap were intentionally NOT extracted -
  only the datapack-side, reusable structure assets.
- 74 `.nbt` structure pieces under `data/stoneholm/structures/<group>/`, grouped by the
  mod's piece roles: `corridor`, `bedroom`, `courtyard`, `job`, `misc_room`, `villager`,
  `iron_golem`, `entrace/stairs`, `entrace/tip`, `end_cap`, `wall_lighting`, `easter_eggs`.
- 39 supporting JSONs: `worldgen/` (jigsaw `template_pool`, `processor_list`, `structure`,
  `structure_set`), `loot_tables/`, and `tags/` - retained so a piece's jigsaw assembly
  and loot can be reused/adapted.
- The jar had no `pack.mcmeta` (it is a mod, not a datapack); a minimal `pack.mcmeta`
  (`pack_format` 15, MC 1.20.1) was added so the folder loads as a vanilla datapack.

## Note on folder naming

- These assets use the pre-1.21 `data/stoneholm/structures/` and `loot_tables/` folder
  names. On MC 1.21+ vanilla datapacks expect singular `structure/` and `loot_table/`.
  The raw `.nbt` files are version-agnostic, but to wire the jigsaw `template_pool`s on
  1.21+ the folders/`pack_format` would need updating to the 1.21 layout.

## Reuse as NBT structures

- The raw `.nbt` files are standard Minecraft structure NBT and can be loaded directly via
  a Structure Block (Load mode, name `stoneholm:<group>/<piece>`) once deployed as a
  datapack, or copied into any other pack's `data/<namespace>/structure(s)/` path.
- Jigsaw-assembled structures need their matching `worldgen/template_pool` entries under
  `data/stoneholm/worldgen/`.

## Deploy

Auto-discovered by `datapacks/deploy-datapacks.ps1`. Worldgen/structure changes only appear
in **newly generated chunks**; server boots remain user-triggered.
