# When Dungeons Arise: Seven Seas (third-party derived assets)

Naval set-pieces (ships, galleons, pirate vessels) (`dungeons_arise_seven_seas`) by the
**When Dungeons Arise** team. Extracted from the Fabric mod jar and kept here so the 36
`.nbt` structure pieces can be reused as plain NBT structures on our server. This also
fills the previously-noted "Dungeons Arise - Seven Seas" reference gap in
`docs/design/BIOME-STRUCTURE-CONCEPTS.md` (lighthouses / naval ocean set-pieces).

## License

- Source license: bundled `LICENSE_dungeons_arise_seven_seas` is **CC0 1.0 Universal**
  (public domain dedication). CC0 places no restrictions on use, modification, or
  redistribution, so - unlike the All-Rights-Reserved / CC BY-NC-ND packs in this repo -
  these assets are fully free to commit and share.
- We nonetheless **git-ignore** them (track only this `README.md`) for repo-leanness and
  consistency with the other extracted packs. This is policy, not a license restriction;
  they may be committed freely later if desired.
- Attribution is not legally required under CC0, but crediting When Dungeons Arise /
  Seven Seas is good practice.

## Provenance

- Source: `DungeonsAriseSevenSeas-1.21.x-1.0.4-fabric.jar` (Fabric mod, version 1.0.4,
  targets MC 1.21.x).
- Modrinth: https://modrinth.com/mod/when-dungeons-arise-seven-seas
- Extracted from `C:\Users\lxgol\Downloads\` on 2026-06-19.

## What was extracted

- The mod jar's full `data/dungeons_arise_seven_seas/` tree. The Java code (`net/`),
  mixins (`dungeons_arise_seven_seas.mixins.json`), `assets/`, `META-INF/`,
  `fabric.mod.json` and the CC0 license file were intentionally NOT extracted - only the
  datapack-side, reusable structure assets.
- A `pack.mcmeta` was added (none ships in the jar; the mod loads its data directly) so
  the folder loads as a vanilla datapack (`pack_format` 48, MC 1.21.x).
- 36 `.nbt` structure pieces under `data/dungeons_arise_seven_seas/structure/`, grouped by
  vessel: `corsair_corvette` (6), `pirate_junk` (7), `small_yacht` (4),
  `unicorn_galleon` (7), `victory_frigate` (12).
- 40 supporting JSONs: `worldgen/` (jigsaw `template_pool`, `processor_list`, `structure`,
  `structure_set`), `loot_table/`, and `tags/` - retained so a piece's jigsaw assembly and
  loot can be reused/adapted.

## Reuse as NBT structures

- The raw `.nbt` files are standard Minecraft structure NBT and can be loaded directly via
  a Structure Block (Load mode, name `dungeons_arise_seven_seas:<vessel>/<piece>`) once
  this is deployed as a datapack, or copied into any other pack's
  `data/<namespace>/structure/` path.
- Jigsaw-assembled structures need their matching `worldgen/template_pool` entries under
  `data/dungeons_arise_seven_seas/worldgen/`.

## Deploy

Auto-discovered by `datapacks/deploy-datapacks.ps1`. Worldgen/structure changes only appear
in **newly generated chunks**; server boots remain user-triggered.
