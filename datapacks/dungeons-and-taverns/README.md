# Dungeons and Taverns (third-party derived datapack - QUARANTINED)

Vanilla-style dungeons, taverns, forts, ruins and more (`nova_structures`) by the
**Dungeons and Taverns** team. Included here for our **private, non-commercial** server only.

## License and redistribution policy

- Source license: **CC BY-NC-ND 4.0** (https://creativecommons.org/licenses/by-nc-nd/4.0/).
- CC BY-NC-ND permits *producing* adapted material for private, non-commercial use but
  forbids **Sharing** adapted/derived material.
- Therefore: every extracted/derived asset under this folder (the `data/` and `assets/`
  trees, `.nbt` pieces, `pack.png`, `credits.txt`) is **git-ignored and MUST NOT be
  committed, redistributed, or published**. Only this `README.md` is tracked, so the policy
  and provenance survive in version control. This mirrors ADR-004 (third-party derived
  structure assets quarantine), updated for the post-Iris vanilla-datapack model (ADR-005).
- Keep attribution: credit Dungeons and Taverns and link the Modrinth page in any private use.

## Provenance

- Source: `Dungeons and Taverns v5.2.0.zip` (Modrinth version `ohqJT0pv`, supports MC 26.1-26.1.2).
- Modrinth: https://modrinth.com/datapack/dungeons-and-taverns

## What was changed vs. the downloaded pack

- Shipped as-is (vanilla datapack; `pack.mcmeta` declares format [101,1] / 26.1).
- Injected our custom `leaf:*` biome ids into the pack's biome collection tags under
  `data/nova_structures/tags/worldgen/biome/collections/*` (and `illager_hideout.json`) so
  the structures generate inside our custom biomes. `collections/land` aggregates the
  granular sub-collections, so the additions bubble up to all `land`-based structures.
- Appended `leaf:bayou` / `leaf:forest_*` to three structures that hard-code a single biome
  (`mangrove_witch_hut`, `firewatch_tower_mangrove`, `firewatch_tower_forest`).

## How to reproduce

1. Download the zip from the Modrinth version above and unzip into this folder.
2. Re-run `datapacks/_patch-dnt.ps1` (if retained) to re-apply the biome mapping.

## Deploy

Auto-discovered by `datapacks/deploy-datapacks.ps1`. Worldgen/structure changes only appear
in **newly generated chunks**; server boots remain user-triggered.
