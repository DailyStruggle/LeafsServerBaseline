# Hopo Better Mineshaft (third-party datapack - QUARANTINED)

More and better themed mineshafts (`hopo` / `zhopo`) by **Hopo**. Kept here so the
1282 `.nbt` jigsaw structure pieces can be reused as plain NBT structures on our
**private, non-commercial** server only.

## License and redistribution policy

- Source license: bundled `LICENSE` grants free personal/server use and permits
  redistribution **only in UNMODIFIED form** (e.g. inside a public modpack);
  **distributing modified versions is forbidden** and Hopo retains full copyright.
- To stay clearly within those terms, every extracted asset under this folder (the
  `data/` tree, all `.nbt` pieces, the worldgen/loot/tag JSONs, `pack.mcmeta`) is
  **git-ignored**. Only this `README.md` is tracked, so the policy and provenance
  survive in version control. This mirrors the quarantine pattern used for the other
  extracted packs (ADR-004 / ADR-005).
- Keep attribution: credit Hopo / Hopo Better Mineshaft in any private use.

## Provenance

- Source: `hopobettermineshaft-26-1-1-3-6.zip` (datapack version 1.3.6, MC 26.1).
- Modrinth: https://modrinth.com/datapack/hopo-better-mineshaft
- Extracted from `C:\Users\lxgol\Downloads\` on 2026-06-19.

## What was extracted

- The pack's full `data/` tree plus the original `pack.mcmeta` (`pack_format` 101.1,
  `min_format` 101.1, `max_format` 107 - i.e. MC 26.1+). The mod-side wrapper files
  (`META-INF/`, `fabric.mod.json`, `pack.png`, `icon.png`, `changelog.txt`,
  `credits.txt`, `RemoveVanillaMineshaft.zip`) were intentionally NOT extracted - only
  the datapack-side, reusable structure assets.
- 1282 `.nbt` structure pieces under `data/hopo/structure/mineshaft/<theme>/`, grouped
  by 13 themes (`oak`, `birch`, `spruce`, `dark_oak`, `acacia`, `jungle`, `mangrove`,
  `bamboo`, `cherry`, `mud`, `pale`, `stone`, `deepslate`), each with `trap`,
  `minecart`, and `big_rooms` sub-pieces, plus shared `mobs` pieces.
- 378 supporting JSONs across namespaces `hopo`, `zhopo` and `minecraft`: `worldgen/`
  (jigsaw `template_pool`, `processor_list`, `structure`, `structure_set`),
  `loot_table/`, `tags/`, `advancement/`, `function/` - retained so a piece's jigsaw
  assembly and loot can be reused/adapted.

## Reuse as NBT structures

- The raw `.nbt` files are standard Minecraft structure NBT and can be loaded directly
  via a Structure Block (Load mode, name `hopo:mineshaft/<theme>/<piece>`) once this is
  deployed as a datapack, or copied into any other pack's `data/<namespace>/structure/`
  path.
- Jigsaw-assembled structures need their matching `worldgen/template_pool` entries under
  `data/hopo/worldgen/`.

## Deploy

Auto-discovered by `datapacks/deploy-datapacks.ps1`. Worldgen/structure changes only
appear in **newly generated chunks**; server boots remain user-triggered.
