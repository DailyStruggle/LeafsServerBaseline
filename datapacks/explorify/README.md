# Explorify (third-party datapack - QUARANTINED)

A simplistic, vanilla-friendly collection of new structures (`explorify`) by **everloste**.
Kept here so the 330 `.nbt` jigsaw structure pieces can be reused as plain NBT structures on
our **private, non-commercial** server only.

## License and redistribution policy

- No license file ships inside the downloaded zip, and the Modrinth project page does not
  state a permissive license. We therefore treat the assets conservatively as
  **All-Rights-Reserved**: private, non-commercial server use only.
- Accordingly, every extracted asset under this folder (the `data/` tree, the `f15`/`f41`
  overlays, all `.nbt` pieces, the worldgen/loot/tag JSONs, `pack.mcmeta`, `pack.png`) is
  **git-ignored**. Only this `README.md` is tracked, so the policy and provenance survive in
  version control. This mirrors the quarantine pattern used for the other extracted packs
  (ADR-004 / ADR-005).
- Keep attribution: credit everloste / Explorify and link the Modrinth page in any private use.

## Provenance

- Source: `Explorify v1.6.5.dp.zip` (datapack version 1.6.5).
- Modrinth: https://modrinth.com/datapack/explorify
- Author site: https://everloste.github.io
- Extracted from `C:\Users\lxgol\Downloads\` on 2026-06-19.

## What was extracted

- The pack's full `data/` tree, both pack-format overlays (`f15`, `f41`), the original
  `pack.mcmeta` and `pack.png`. There are no mod-wrapper files in this zip (it is a pure
  datapack download, not a Fabric mod jar).
- `pack.mcmeta` declares `pack_format` 15 with `supported_formats` / `min_format` 15 and
  `max_format` 512, plus two overlays: `f15` (formats 15..512) and `f41` (formats 41..512).
  The overlays carry the per-format variants of a few `processor_list` / `loot_table` files.
- 330 `.nbt` structure pieces. The pack ships **both** the pre-1.21 plural layout
  (`data/explorify/structures/` + `loot_tables/`) and the 1.21+ singular layout
  (`data/explorify/structure/` + `loot_table/`) so it loads across versions.
- 142 supporting JSONs across namespaces `explorify` and `cristellib` (config compat):
  `worldgen/` (jigsaw `template_pool`, `processor_list`, `structure`, `structure_set`),
  `loot_table(s)/`, and `tags/` - retained so a piece's jigsaw assembly and loot can be
  reused/adapted.
- Structures included (per the Modrinth gallery): Dark Forest Settlement, Desert Shrine,
  Badlands Pyramid, Guide Post, End Shipwreck, Ruins, Campsite, Farmstead, Black/Bastion
  Spiral, Supply Cache, Mausoleum, Mangrove Hut, Watchtower, Tavern, and biome variants.

## Reuse as NBT structures

- The raw `.nbt` files are standard Minecraft structure NBT and can be loaded directly via a
  Structure Block (Load mode, name `explorify:<path>/<piece>`) once this is deployed as a
  datapack, or copied into any other pack's `data/<namespace>/structure/` path.
- Jigsaw-assembled structures need their matching `worldgen/template_pool` entries under
  `data/explorify/worldgen/`.

## Deploy

Auto-discovered by `datapacks/deploy-datapacks.ps1`. Worldgen/structure changes only appear
in **newly generated chunks**; server boots remain user-triggered.
