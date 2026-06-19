# ChoiceTheorem's Overhauled Village (third-party datapack - QUARANTINED)

Biome-specific overhauled villages and pillager outposts (`ctov`) by **ChoiceTheorem**.
Kept here so the 2107 `.nbt` jigsaw structure pieces can be reused as plain NBT structures
on our **private, non-commercial** server only.

## License and redistribution policy

- No license file ships inside the jar, and the Modrinth project page does not state a
  permissive license. We therefore treat the assets conservatively as
  **All-Rights-Reserved**: private, non-commercial server use only.
- Accordingly, every extracted asset under this folder (the `data/` tree, all `.nbt`
  pieces, the worldgen/loot/tag JSONs, `pack.mcmeta`) is **git-ignored**. Only this
  `README.md` is tracked, so the policy and provenance survive in version control. This
  mirrors the quarantine pattern used for the other extracted packs (ADR-004 / ADR-005).
- Keep attribution: credit ChoiceTheorem / ChoiceTheorem's Overhauled Village and link the
  Modrinth page in any private use.

## Provenance

- Source: `[Neoforge]ctov-3.6.3.jar` (NeoForge mod, version 3.6.3, targets MC 1.21.x).
- Modrinth: https://modrinth.com/mod/ct-overhaul-village
- Extracted from `C:\Users\lxgol\Downloads\` on 2026-06-19.

## What was extracted

- The mod jar's full `data/` tree plus the original `pack.mcmeta` (`pack_format` 48,
  `supported_formats` [18, 48] - i.e. MC 1.20.5+ .. 1.21.x). The Java code (`net/`),
  mixins/accesswidener, `assets/`, `META-INF/`, the architectury inject blob, and the two
  bundled add-on sub-packs (`ctov-extended-mushrooms`, `ctov-savage-and-ravage-add-on`)
  were intentionally NOT extracted - only the core datapack-side, reusable structure
  assets.
- 2107 `.nbt` structure pieces under `data/ctov/structure/`: `village/` (2079 pieces,
  grouped by biome/theme) and `pillager_outpost/` (144 pieces).
- 1643 supporting JSONs across namespaces `ctov`, `monobank`, `wares`, `minecraft`:
  `worldgen/` (jigsaw `template_pool`, `processor_list`, `structure`, `structure_set`),
  `lithostitched/` worldgen-modifier definitions (~1073 files), `loot_table/`, `tags/`,
  and `structure_icons.json` - retained so a piece's jigsaw assembly and loot can be
  reused/adapted.

## Dependency note (Lithostitched)

- CTOV wires its villages/outposts into world generation via the **Lithostitched**
  worldgen-modifier library (see `data/ctov/lithostitched/`). Without Lithostitched (or an
  equivalent worldgen-modifier mechanism), the `lithostitched/` JSONs are inert and the
  structures will not auto-generate as a plain vanilla datapack. The raw `.nbt` pieces and
  the `worldgen/template_pool` graph are still directly reusable for hand-placement or for
  re-wiring under your own jigsaw `structure_set`s.

## Reuse as NBT structures

- The raw `.nbt` files are standard Minecraft structure NBT and can be loaded directly via
  a Structure Block (Load mode, name `ctov:<path>/<piece>`) once this is deployed as a
  datapack, or copied into any other pack's `data/<namespace>/structure/` path.
- Jigsaw-assembled structures need their matching `worldgen/template_pool` entries under
  `data/ctov/worldgen/`.

## Deploy

Auto-discovered by `datapacks/deploy-datapacks.ps1`. Worldgen/structure changes only appear
in **newly generated chunks**; server boots remain user-triggered.
