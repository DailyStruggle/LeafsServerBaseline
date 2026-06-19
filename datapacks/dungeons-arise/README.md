# When Dungeons Arise (third-party derived assets - QUARANTINED)

Large set-piece dungeons, keeps, galleons, towers and more (`dungeons_arise`) by the
**When Dungeons Arise** team. Extracted from the Fabric mod jar and kept here so the
877 `.nbt` structure pieces can be reused as plain NBT structures on our **private,
non-commercial** server only.

## License and redistribution policy

- Source license: bundled `LICENSE_dungeons_arise` reads **"All Rights Reserved."**
- Private, non-commercial server use is permitted, but the extracted/derived assets
  **MUST NOT be committed, redistributed, or published**.
- Therefore: everything under this folder (the `data/` tree, all `.nbt` pieces, the
  worldgen/loot/tag JSONs, `pack.mcmeta`) is **git-ignored**. Only this `README.md` is
  tracked, so the policy and provenance survive in version control. This mirrors ADR-004
  (third-party derived structure assets quarantine), under the post-Iris vanilla-datapack
  model (ADR-005).
- Keep attribution: credit When Dungeons Arise in any private use.

## Provenance

- Source: `DungeonsArise-1.21.1-2.1.68-fabric-release.jar` (MC 1.21.1, mod version 2.1.68, Fabric).
- Extracted from `C:\Users\lxgol\Downloads\` on 2026-06-19.

## What was extracted

- The mod jar's full `data/` tree plus `pack.mcmeta` (`pack_format` 15). The Java code
  (`net/`), `assets/`, `META-INF/`, `fabric.mod.json` and logo were intentionally NOT
  extracted - only the datapack-side, reusable structure assets.
- 877 `.nbt` structure pieces under `data/dungeons_arise/structure/<theme>/`, grouped by
  the mod's 43 structure themes (e.g. `aviary`, `foundry`, `shiraz_palace`,
  `mechanical_nest`, `bandit_village`, `coliseum`, ...).
- 483 supporting JSONs: `worldgen/` (jigsaw `template_pool`, `processor_list`,
  `structure`, `structure_set`), `loot_table/`, `tags/`, `advancement/`, `enchantment/`,
  `predicate/`, `function/` - retained so a piece's jigsaw assembly and loot can be
  reused/adapted.

## Reuse as NBT structures

- The raw `.nbt` files are standard Minecraft structure NBT and can be loaded directly via
  a Structure Block (`/setblock ... structure_block`, set to Load mode with name
  `dungeons_arise:<theme>/<piece>`) once this is deployed as a datapack, or copied into any
  other pack's `data/<namespace>/structure/` path.
- Jigsaw-assembled structures need their matching `worldgen/template_pool` entries; those
  live under `data/dungeons_arise/worldgen/`.

## Deploy

Auto-discovered by `datapacks/deploy-datapacks.ps1`. Worldgen/structure changes only appear
in **newly generated chunks**; server boots remain user-triggered.
