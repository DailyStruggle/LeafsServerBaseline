# Towns & Towers (third-party derived datapack - QUARANTINED)

This datapack contains assets derived from **Towns & Towers** by the ChoiceTheorem /
Biawog team (Modrinth project `DjLobEOy`). It is included here for our **private,
non-commercial** server only.

## License and redistribution policy

- Source license: **CC BY-NC-ND 4.0** (https://creativecommons.org/licenses/by-nc-nd/4.0/).
- CC BY-NC-ND permits *producing* adapted material for private, non-commercial use but
  forbids **Sharing** adapted/derived material.
- Therefore: every extracted/derived asset under this folder (the `data/` tree, `.nbt`
  pieces, `pack.png`, bundled license/credits) is **git-ignored and MUST NOT be committed,
  redistributed, or published**. Only this `README.md` is tracked, so the policy and
  provenance survive in version control. This mirrors ADR-004 (third-party derived
  structure assets quarantine), updated for the post-Iris vanilla-datapack model (ADR-005).
- Keep attribution: credit Towns & Towers and link the Modrinth page in any private use.

## Provenance

- Source jar: `t_and_t-fabric-neoforge-1.13.11.jar` (Modrinth version `eN3WLQ3P`,
  supports MC 26.1 / 26.1.1 / 26.1.2).
- Modrinth: https://modrinth.com/mod/towns-and-towers

## What was changed vs. the mod jar

- Extracted the `data/` tree and re-shipped as a plain vanilla datapack.
- Kept namespaces: `towns_and_towers` (structures, structure_sets, tags),
  `kaisyn` (template pools + `.nbt` pieces), `minecraft` (biome/structure tag injections).
- **Dropped** the `cristellib` namespace: Cristel Lib is the mod-only runtime that
  re-tunes structure spacing/separation. It is NOT required for placement - the
  `worldgen/structure_set` JSONs are static and the structures target vanilla
  `#minecraft:has_structure/*` biome tags, so they generate under pure vanilla worldgen.
- Added a `pack.mcmeta` (pack_format 71, `supported_formats` [61, 71]).

## How to reproduce

1. Download the jar from the Modrinth version above.
2. Unzip; copy `data/towns_and_towers`, `data/kaisyn`, `data/minecraft` into `data/`.
3. Add the `pack.mcmeta` above.

## Deploy

Auto-discovered by `datapacks/deploy-datapacks.ps1` (any `datapacks/<pack>` with a
`pack.mcmeta`). Worldgen/structure changes only appear in **newly generated chunks**, so a
fresh world (or new exploration) is needed; server boots remain user-triggered.
