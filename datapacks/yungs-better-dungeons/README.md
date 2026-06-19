# YUNG's Better Dungeons (third-party derived assets)

Overhauled, jigsaw-assembled dungeons (`betterdungeons`) by **YUNGNICKYOUNG**. Extracted
from the Fabric mod jar and kept here so the 227 `.nbt` structure pieces can be reused as
plain NBT structures on our server. This also backs the previously-noted "YUNG's Better
Dungeons" references in `docs/design/BIOME-STRUCTURE-CONCEPTS.md` (Ice Caves "Frozen
Adventurer's Camp", Amethyst Caves "Geode Vault" / amethyst vault).

## License

- Source license: bundled `LICENSE_YungsBetterDungeons` is the **GNU LGPL v3**.
  LGPL permits use, modification, and redistribution provided the license/source terms
  are honoured, so - unlike the All-Rights-Reserved / CC BY-NC-ND packs in this repo -
  these assets are *legally* committable.
- We nonetheless **git-ignore** them (track only this `README.md`) for repo-leanness and
  consistency with the other extracted packs. This is policy, not a license restriction;
  they may be committed later if desired.
- Keep attribution: credit YUNGNICKYOUNG / YUNG's Better Dungeons and retain the LGPL
  notice in any use.

## Provenance

- Source: `YungsBetterDungeons-1.21.4-Fabric-5.4.0.jar` (Fabric mod, version 5.4.0,
  targets MC 1.21.4).
- Extracted from `C:\Users\lxgol\Downloads\` on 2026-06-19.

## What was extracted

- The mod jar's full `data/` tree plus the original `pack.mcmeta` (`pack_format` 46).
  The Java code (`com/`), mixins/refmap, `assets/`, `META-INF/`, `fabric.mod.json` and
  logos were intentionally NOT extracted - only the datapack-side, reusable structure
  assets.
- 227 `.nbt` structure pieces under `data/betterdungeons/structure/`, grouped by dungeon
  type: `skeleton_dungeon` (arches, bridges, stairs, campfire props), `zombie_dungeon`
  (rooms, terminators, tombstones, cubbies), `small_dungeon` (loot piles, shells), and
  `small_nether_dungeon` (2x1 / 2x2 / 3x2 props, shells).
- 80 supporting JSONs across namespaces `betterdungeons` and `yungsapi`: `worldgen/`
  (jigsaw `template_pool`, `processor_list`, `structure`, `structure_set`), `loot_table/`,
  `tags/`, `advancement/`, and forge/neoforge compat - retained so a piece's jigsaw
  assembly and loot can be reused/adapted.

## Reuse as NBT structures

- The raw `.nbt` files are standard Minecraft structure NBT and can be loaded directly via
  a Structure Block (Load mode, name `betterdungeons:<group>/<piece>`) once this is
  deployed as a datapack, or copied into any other pack's `data/<namespace>/structure/`
  path.
- Jigsaw-assembled structures need their matching `worldgen/template_pool` entries under
  `data/betterdungeons/worldgen/` (and the `yungsapi` processors they reference).

## Deploy

Auto-discovered by `datapacks/deploy-datapacks.ps1`. Worldgen/structure changes only appear
in **newly generated chunks**; server boots remain user-triggered.
