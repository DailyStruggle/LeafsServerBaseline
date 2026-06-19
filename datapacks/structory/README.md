# Structory (third-party derived datapack - QUARANTINED)

Atmospheric, lore-based structures by **Botany / Stardust Labs**. Included here for our
**private, non-commercial** server only.

## License and redistribution policy

- Source license: the **Stardust Labs License** (see `license.txt` shipped inside the pack).
- It PERMITS use on a private or public server, including in MODIFIED form, but FORBIDS
  reuploading/republishing/redistributing the datapack in standalone OR modified form.
- Committing the assets to version control would constitute redistribution, so every
  derived asset under this folder (the `data/` tree, `.nbt` pieces, `pack.png`,
  `license.txt`, `patrons.txt`) is **git-ignored and MUST NOT be committed or published**.
  Only this `README.md` is tracked, so the policy/provenance survive in version control.
  This mirrors ADR-004 (third-party derived structure assets quarantine), updated for the
  post-Iris vanilla-datapack model (ADR-005).
- Keep attribution: credit Stardust Labs and link the project in any private use.

## Provenance

- Source: `Structory_v1.3.15.zip` (Modrinth version `KjDonUTl`, supports MC 1.21-26.1.2).
- Modrinth: https://modrinth.com/datapack/structory

## What was changed vs. the downloaded pack

- Shipped as-is (vanilla datapack; `pack.mcmeta` already declares format 101 / 26.1 support).
- Appended our custom `leaf:*` biome ids to the `biomes` field of selected structure
  definitions under `data/structory/worldgen/structure/` so they generate inside our
  custom biomes (the `leaf:*` biomes replaced their vanilla derivatives, so the structures'
  original vanilla biome tags no longer match inside those regions).

## How to reproduce

1. Download the zip from the Modrinth version above and unzip into this folder.
2. Re-run `datapacks/_patch-structory.ps1` (if retained) to re-apply the biome mapping.

## Deploy

Auto-discovered by `datapacks/deploy-datapacks.ps1`. Worldgen/structure changes only appear
in **newly generated chunks**; server boots remain user-triggered.
