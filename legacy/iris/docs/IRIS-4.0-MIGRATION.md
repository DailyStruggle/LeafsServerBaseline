# Migrating to Iris 4.0: Findings and How-To

This document records what we currently know about Iris 4.0 and the methodology for
re-syncing `pack-base` and re-validating our overlay/deploy workflow against it. It exists
because Iris 4.0 is a breaking change versus the 3.9.1 line this repo was built on, and the
move needs to be deliberate rather than a drop-in jar swap.

Status as of 2026-06-07: **Iris 4.0 is a private/beta build, not a public release.** Treat
everything here as preparation, not a completed migration. The actual in-game cutover is
user-triggered (see "The cutover is user-triggered" below).

---

## What we found

The jar in hand is `Iris.jar` reporting (from its `plugin.yml`):

```yaml
name: Iris
version: 4.0.0-26.1
main: art.arcane.iris.Iris
folia-supported: true
load: STARTUP
authors: [ cyberpwn, NextdoorPsycho, Vatuu ]
api-version: '26.1'
```

Key facts and what each one implies for this repo:

- **It really is 4.0** (`4.0.0-26.1`). This is *not* on public GitHub/Spigot/Modrinth yet.
  The public top-of-tree (both `master` and `dev` branches, and the newest git tag) is still
  `3.9.1-1.20.1-1.21.11`. So there is no public 4.0 jar or branch to build - this build came
  from a non-public channel (Discord beta/tester role, paid early-access storefront, or a
  direct link from the dev). If you need to re-obtain it, ask the dev for the exact source.
- **New package namespace**: `art.arcane.iris.Iris` (the 3.9 line is `com.volmit.iris...`).
  This is a significant internal restructure. Any of our notes that cite Iris source by class
  or line number (most of `docs/scratch/LESSONS_LEARNED.md`) were read against the
  `com.volmit.iris` 3.9.1 tree and must be re-verified before trusting them on 4.0.
- **`api-version: '26.1'` and Folia support**: 4.0 targets a very new server API. The test
  server must be a Paper/Purpur (or Folia) build that actually matches `26.1`, or the plugin
  will not load. Our scripts are pinned to `...\RTP-Paper\1.21.11`; that path/version almost
  certainly has to change for a 4.0 boot.
- **Pack format may have changed.** Because of the namespace move and the new api/pack
  version, our current `pack-base`/`pack-overlay` and the `deploy-iris-pack.ps1` assembly may
  need adjustment. Do not assume the 3.9.1 pack loads unchanged under 4.0 - re-sync the base
  and diff it (see below) before relying on any overlay.

### Where to get Iris (general)

For the public line, pick whichever channel fits:

- Modrinth (free, all versions): https://modrinth.com/plugin/irisworldgen - use the Versions
  tab and take the build that matches your Minecraft/Paper version.
- SpigotMC (supports the devs, paid): https://www.spigotmc.org/resources/iris.24851/
- GitHub releases / source: https://github.com/VolmitSoftware/Iris

Always match the jar to your Paper/Minecraft server version - a wrong-version build is the
usual cause of biome/region load failures. For 4.0 specifically, the build is only available
through a non-public channel until Volmit opens a public 4.x line.

---

## Why this is a real migration, not a jar swap

The 3.9.1 -> 4.0 jump combines three independent breaking vectors:

1. **Namespace** (`com.volmit.iris` -> `art.arcane.iris`) - invalidates every source-level
   assumption we documented while reverse-engineering 3.9.1 behavior.
2. **Server API** (`api-version 26.1`, Folia) - changes which server build can even load it.
3. **Pack/engine version** - may change how packs are read, cached, or prefetched.

Our entire customisation pipeline (`pack-base` + `pack-overlay` + `pack-patches`, assembled by
`deploy-iris-pack.ps1`) sits downstream of all three. The pipeline mechanics (layering,
whole-file overlay override, BOM stripping, cache clearing) are ours and stay valid; what can
break is the *content contract* with the engine - field names, defaults, generator/jigsaw
semantics, and where the server expects the pack to live.

---

## How to migrate (step by step)

Do these in order. Steps that require a running server are flagged - those are user-triggered
(the agent never boots the server; see the project guidelines).

### 1. Stage the new jar and a matching server (USER)

- Confirm the test server build matches `api-version 26.1` (Paper/Purpur/Folia). If the
  current server is `1.21.11` only, stand up a `26.1`-compatible build before going further.
- Drop the 4.0 `Iris.jar` into that server's `plugins/`. Keep the public
  `Iris-3.9.1-1.20.1-1.21.11.jar` as a fallback.
- Back up the world and the existing Iris config first. Do not run 4.0 against the live world.

### 2. Let 4.0 auto-download its own vanilla pack (USER)

- Boot the `26.1` server once with **no `overworld` pack present** so 4.0 downloads the pack
  version it expects (same trick as 3.9.1). Confirm `/iris version` reads `4.0.0-26.1`.
- This produces the 4.0 baseline pack under
  `<ServerBase>\plugins\Iris\packs\overworld`.

### 3. Re-sync `pack-base` from the 4.0 download

- Point `iris/scripts/sync-base-pack.ps1` at the new server base and run it. It wipes and
  recreates `iris/pack-base` from the server's auto-downloaded `overworld` pack (objects/
  excluded) and strips BOMs.
- The script's `$ServerBase` default is `C:\GameServers\Minecraft\testServer\RTP-Folia\26.1`.
  Pass the new path explicitly: `./sync-base-pack.ps1 -ServerBase '<new 26.1 server base>'`.

### 4. Diff old vs new base (the heart of "based on findings")

- `pack-base` is **git-ignored**, so there is no committed 3.9.1 copy to `git diff` against.
  Compare the current on-disk `pack-base` (3.9.1) against the downloaded 4.0 pack by relative
  path (see "Comparison against the downloaded 4.0 pack" below for the exact method), objects
  excluded. Look specifically for:
  - renamed/removed/added top-level pack folders (biomes/regions/dimensions/generators/
    jigsaw-*),
  - changed field names or defaults in dimension/region/biome JSON,
  - changes to how objects/jigsaw prefetch is laid out.
- Every difference is a candidate break for our `pack-overlay` (whole-file overrides) and
  `pack-patches` (amend files). Record concrete deltas as dated entries in
  `docs/scratch/LESSONS_LEARNED.md` so they are not re-derived.

### 5. Re-validate the overlay against the new base

- Run the existing audits: `check-overlay-conflicts.ps1`, `check-broken-refs.ps1`,
  `check-all-region-biomes.py`, and `jigsaw_validate.py` per structure. These are
  pack-content checks and do not need the new jar, but they only catch our internal
  consistency - they do not prove 4.0 accepts the format.
- For any overlay file that whole-file-overrides a base file, re-copy the *new* base file and
  re-apply our changes on top, so the override does not silently revert a 4.0 base change.

### 6. Deploy and boot-test (USER for the boot)

- Run `deploy-iris-pack.ps1 -ServerBase '<new 26.1 server base>'`. It rebuilds staging
  (base + overlay + `.iob` + patches), strips BOMs, deploys to both destinations, and clears
  the world/engine/prefetch caches.
- Ask the user to boot the `26.1` server and report: does the pack parse, do regions/biomes
  load without NPEs, do our custom biomes and structures still generate? Re-verify against
  FRESH terrain (Iris never regenerates existing chunks).

### 7. Re-verify the source-level lessons

- The reverse-engineered behaviors in `LESSONS_LEARNED.md` (object centering, `clamp` gating,
  jigsaw `lockY`, `overrideYRange` semantics, the "no `generators` block" NPE, unweighted
  pools, etc.) were all read from the `com.volmit.iris` 3.9.1 source. Before trusting any of
  them on 4.0, re-check the corresponding `art.arcane.iris` class. Flag any that changed with a
  new dated lesson; mark superseded ones rather than deleting them.

---

## Comparison against the downloaded 4.0 pack (2026-06-07)

The user placed a 4.0 auto-download at
`C:\GameServers\Minecraft\testServer\RTP-Folia\26.1\plugins\Iris\packs\overworld`. We
compared it by relative path against our current (3.9.1) `iris/pack-base` and `iris/pack-overlay`.
Method (PowerShell): build relative-path sets for each tree, then `Compare-Object`. Objects are
not synced into `pack-base`, so they are out of scope here.

### Top-level folder deltas (4.0 vs our 3.9.1 base)

- **Added in 4.0**: `structures/` (a flat per-vanilla-structure system: `minecraft_village_*`,
  `minecraft_ancient_city`, `minecraft_trial_chambers`, `shipwreck`, `ruined_portal`, etc.,
  plus a `structure-index.json`).
- **Removed in 4.0**: `caves/`, `entities/`, `jigsaw-structures/`, `markers/`, `ravines/`, and
  the `Object-or-Jigsaw.md` note.
- **Heavily expanded in 4.0**: `jigsaw-pieces` (688 -> 2271), `jigsaw-pools` (72 -> 225),
  `regions` (9 -> 12: adds `estranged`, `magnetics`, `prismatics`), `generators` (19 -> 23).
- **Trimmed in 4.0**: `biomes` (390 -> 373), `loot` (58 -> 24), `spawners` (57 -> 30).
- The 4.0 pack self-identifies as **"Overworld V3000"** in its `README.md`.

### What is genuinely OURS (valuable - keep in overlay, re-apply on the 4.0 base)

- Our **vanilla and vanilla-replica biomes** under `pack-overlay/biomes/vanilla/*` (the
  `plains`, `forest`, `dark_forest__*`, `taiga__*`, `cherry_grove__*`, ... and the `__variant`
  recreations). The user confirmed these are valuable work. They already live in `pack-overlay`,
  so nothing needs moving; they must be **re-validated and re-applied on top of the 4.0 base**.
- All other custom regions/biomes/spawners/generators already in `pack-overlay` (frostpeak,
  ashcrown, embertide, cinderfall, cinnabar, rainbow groves, embervine, etc.).

### What is NOT ours (do not move)

- `biomes/terralost/WIP/*` (76 files) exists in our 3.9.1 `pack-base` but not in 4.0. This is
  **stock Iris community content** (`terralost` is a standard Iris region; its non-WIP biomes -
  `alpine-grove`, `amethyst-rainforest`, `ancient-sands`, ... - are identical in both packs).
  4.0 simply dropped the WIP staging folder. Leave it alone; do not import it into the overlay.

### Cleanup / rework decisions (user-directed)

- **Drop**: the amethyst crystal-cave **tubes** overlays
  (`pack-overlay/caves/cavesv4/crystalized/tubes.json` and
  `pack-overlay/caves/cavesv4/crystalized-overgrown/tubes.json`). Removed 2026-06-07.
- **Rework for the new carving system**: 4.0 has **no `caves/` folder** - caves are now handled
  via the carving/structures system. Our `pack-overlay/caves/vanilla/*` (`chamber`, `cheese`,
  `crack`, `spaghetti`, `spaghetti-small`) and `caves/amethyst/small` need to be re-expressed in
  the 4.0 carving model rather than copied over. Track this as a 4.0 migration TODO.
- **Rework**: 4.0 has **no `jigsaw-structures/` folder**. Our `pack-overlay/jigsaw-structures/*`
  (`dungeon-skeleton`, `dungeon-spider`, `dungeon-zombie`, `mineshaft`) and the matching
  `jigsaw-pieces/mineshaft/*` + `jigsaw-pools/mineshaft/*` must be re-checked against 4.0's
  expanded jigsaw layout / new `structures/` system before they can be re-applied.

---

## Base replaced + conversion findings (2026-06-07)

`iris/pack-base` was re-synced from the 4.0 download via `sync-base-pack.ps1 -ServerBase
'C:\GameServers\Minecraft\testServer\RTP-Folia\26.1'` (now 2990 files; was 1508 under 3.9.1).
The previous 3.9.1 base was copied to `iris/_pack-base-39-backup/` as a read-only reference for
the conversion (git-ignored along with `pack-base`; delete once the migration lands).

Against the new 4.0 base, the overlay splits into **51 whole-file overrides** of a 4.0 base file
and **243 purely additive** files. Starting the "prefer vanilla / theme-matching" conversion
surfaced that nearly every remaining override category is a *schema-level* change, i.e. a bigger
overhaul that needs design decisions and a user-booted 26.1 server to validate:

- **Biome `derivative` / `vanillaDerivative` is now namespaced.** 4.0 base biomes use
  `"minecraft:jagged_peaks"`; our overlay biomes use the old enum form `"JAGGED_PEAKS"`. ~128
  overlay biome files still use the enum form. Unknown (needs server) whether 4.0 still accepts
  the enum or rejects it.
- **Top-level `category` was REMOVED from biomes.** Decompiling `Iris.jar` (4.0) shows
  `art.arcane.iris.engine.object.IrisBiome` has NO `category` field at all - only `derivative` /
  `vanillaDerivative` (Bukkit `Biome`, accepts the namespaced `minecraft:*` form) and a
  `customDerivitives` list. The `category` concept now lives ONLY inside each `customDerivitives`
  entry (`IrisBiomeCustom.category`, an enum: `beach`, `desert`, `extreme_hills`, `forest`, `icy`,
  `jungle`, `mesa`, `mushroom`, `nether`, `none`, `ocean`, `plains`, `river`, `savanna`, `swamp`,
  `taiga`, `the_end`). A biome's own category is otherwise inferred from its `vanillaDerivative`.
- **Vanilla biomes were restructured.** 4.0 `biomes/vanilla/` keeps only ~20 "special" biomes
  (cherry_grove, jagged_peaks, ice_spikes, oceans, windswept_*, ...). Simple ones (plains,
  desert, forest, taiga, ...) are **no longer files in base** - so our valuable vanilla/replica
  recreations under `pack-overlay/biomes/vanilla/*` are now mostly *additive*, not overrides.
  The 4.0 versions that DO exist are far richer (e.g. cherry_grove 17.9 KB vs our 2.0 KB), so a
  blind whole-file override would *downgrade* them.
- **Region schema changed to a carving model.** 4.0 base regions use `caveProfile`,
  `caveBiomes`, `caveBiomeZoom` and have **no** `caves` / `ravines` / `jigsawStructures` /
  `objects` keys. Our overlay regions still carry the 3.9.1 `caves`/`ravines`/`jigsawStructures`/
  `objects`/`carving` keys. 4.0 also adds a `biomes/carving/` folder (the new cave-biome home).
- **`caves/` and `jigsaw-structures/` folders are gone** (already flagged). Our overlay
  `caves/vanilla/*`, `caves/amethyst/small`, and `jigsaw-structures/*` (+ `jigsaw-pieces/mineshaft`,
  `jigsaw-pools/mineshaft`) must be re-expressed in 4.0's carving + `structures/` systems.

### Recommended conversion order (await user direction per "pause for bigger overhauls")

1. **Derivative namespacing sweep** - DONE 2026-06-07. `iris/scripts/namespace-biome-derivatives.py`
   rewrote `derivative` / `vanillaDerivative` from the old Bukkit enum form (`JAGGED_PEAKS`) to the
   4.0 namespaced form (`minecraft:jagged_peaks`): 256 values across 128 overlay biome files. The
   script does targeted regex edits (formatting/diff preserved), skips already-namespaced or
   non-enum values, and carries a `RENAME` map for legacy enum names whose plain lowercase would be
   the WRONG registry key (`MOUNTAINS` -> `windswept_hills`, `SNOWY_TUNDRA` -> `snowy_plains`,
   `GIANT_TREE_TAIGA` -> `old_growth_pine_taiga`, etc.). None of those legacy names were present in
   the current overlay (all 41 distinct values were already modern 1.21 enum names), but the map
   stays for re-runs/new content. Re-runnable and idempotent.
2. **Strip dead top-level `category`** - DONE 2026-06-08. Resolved by source: decompiling
   `Iris.jar` proved `IrisBiome` has no `category` field in 4.0, so the top-level key is dead (no
   server boot needed to confirm). `iris/scripts/strip-biome-category.py` removed it from **87**
   overlay biomes (targeted regex on the top-level indentation only, JSON-validated, idempotent).
   It deliberately does **not** move the value into `customDerivitives`: a `customDerivitives` entry
   *registers a new custom biome derivative* (a registry/behaviour change) and its `category` is a
   different, narrower concept - moving would be wrong. The 41 `customDerivitives[].category`
   values already present (36 custom biomes) were left untouched.
3. **Vanilla biome overrides** - see "How we apply vanilla biomes" below for the exact split.
   16 overlay `biomes/vanilla/*` files override a 4.0 base file; 15 of those would *downgrade* a
   much richer 4.0 biome. Prefer the 4.0 base (drop our override / reference the base biome)
   unless our file adds a deliberate theme element (e.g. `jagged_peaks` wiring our custom
   `mountain-aggro` generator). Decide drop-vs-rebase per file (derivatives already namespaced).
4. **Region carving rework** (bigger overhaul) - re-base our 12 region overrides on the 4.0
   `caveProfile`/`caveBiomes` model; drop the dead `caves`/`ravines` arrays.
5. **Caves -> carving biomes** (bigger overhaul, USER is documenting) - re-author `caves/vanilla/*`
   + `amethyst/small` as 4.0 `biomes/carving/*`.
6. **Jigsaw-structures -> `structures/`** (bigger overhaul) - re-wire our dungeons + mineshaft.
   - **Towns & Towers modded structures: DONE 2026-06-08.** The private pack-patches
     overlay (17 T&T villages + 23 pillager outposts) was cut over from the 3.x
     `jigsaw-structures/` + biome `jigsawStructures` arrays to the 4.0 `structures/`
     (`IrisStructure`) + `structures` (`IrisStructurePlacement`) model by
     `iris/scripts/migrate-legacy-structures-to-v4.py` (40 structures, 25 biome
     patches; legacy folder removed). Each `IrisStructure` carries
     `startPool`/`maxDepth`/`maxSizeChunks:8`/`placeMode:STRUCTURE_PIECE` (no
     `vanillaSource` - these are modded, no vanilla key). Placement grid: villages
     spacing 32 / separation 8, outposts spacing 48 / separation 12, per-structure
     `salt`. The emitters (`nbt_to_iris.py`, `import-tnt-villages.py`,
     `wire-tt-outposts.py`) and the validator (`jigsaw_validate.py`) now produce/read
     the v4 form; all 40 graphs still wire statically. **In-game placement (a booted
     26.1 server) is still pending** to confirm density feels right.
   - **Still TODO**: re-wire our own `pack-overlay/jigsaw-structures/*` (dungeons +
     mineshaft) the same way.

Each of 4-6 is a sweeping, multi-file change that must be validated on a booted 26.1 server, so
they are paused pending user direction and a server boot. Steps 1-2 (derivative namespacing,
dead-category strip) are done; step 6's Towns & Towers modded-structure cutover is done (pending
in-game verification).

---

## How we apply vanilla biomes (3.9.1 vs 4.0) (2026-06-08)

"Applying" a vanilla biome means two separate things, and 4.0 changed both:

### 1. The biome file (`pack-overlay/biomes/vanilla/<name>.json`)

- In 3.9.1, base shipped a `biomes/vanilla/*` file for essentially every overworld biome, and we
  authored same-path overlay files to recreate / tweak them (whole-file override).
- 4.0 ships only **20 "special" vanilla biome files** (`cherry_grove`, `jagged_peaks`,
  `ice_spikes`, the oceans, `grove`, `mangrove_swamp`, `old_growth_*`, `savanna_plateau`,
  `snowy_slopes`, `stony_peaks`, `stony_shore`, `sunflower_plains`, `windswept_*`,
  `wooded_badlands`). The "simple" biomes (`plains`, `desert`, `forest`, `taiga`, `swamp`, ...)
  are **no longer files** - 4.0 derives them directly from the vanilla registry.
- Our 67 overlay `biomes/vanilla/*` files therefore split into:
  - **51 ADDITIVE** (no 4.0 base file at that path): `plains`, `desert`, `forest`, `taiga`,
    `swamp`, `birch_forest*`, `dark_forest*`, `meadow*`, `jungle*`, `snowy_*`, `savanna*`,
    `lake`, `puddle`, ... These are pure additions and apply cleanly - keep them.
  - **16 OVERRIDES** of a 4.0 base file: `cherry_grove`, `grove`, `ice_spikes`, `jagged_peaks`,
    `mangrove_swamp`, `old_growth_birch_forest`, `old_growth_pine_taiga`, `savanna_plateau`,
    `snowy_slopes`, `stony_peaks`, `stony_shore`, `sunflower_plains`, `windswept_forest`,
    `windswept_gravelly_hills`, `windswept_savanna`, `wooded_badlands`. **15 of these would
    downgrade a much richer 4.0 biome** (e.g. `cherry_grove` 2.0 KB ours vs 17.9 KB base;
    `ice_spikes` 0.2 KB vs 6.1 KB; `stony_shore` 0.2 KB vs 7.1 KB). Only `sunflower_plains`
    (1.5 KB vs 1.3 KB) is comparable. Recommended: drop the override and let the 4.0 base win,
    unless the file carries a deliberate theme hook (e.g. `jagged_peaks` -> `mountain-aggro`).

### 2. Wiring (where the biome is placed)

- Vanilla biomes are placed by **path reference** from region files
  (`landBiomes` / `seaBiomes` / `shoreBiomes` / `caveBiomes`), e.g. `"vanilla/forest"`. This
  mechanism is unchanged in 4.0.
- The difference is volume: our overlay regions inject many extra `vanilla/*` references that the
  4.0 base region does not. Example - `regions/forests.json` `landBiomes`: the 4.0 base lists 3
  vanilla biomes (`old_growth_birch_forest`, `old_growth_pine_taiga`, `windswept_forest`); our
  overlay lists ~20 (adds `forest`, `birch_forest`, `dark_forest`, `flower_forest`, the `__*`
  variants, `lake`, `puddle`, ...). Our region overrides therefore must be rebased onto the 4.0
  region schema (see step 4 / the carving rework) while preserving these references, since the
  referenced biomes (the 51 additive files) only exist in our overlay.
- Caveat for the 16 overrides: if we drop an override file but a region still references
  `vanilla/<name>`, the reference now resolves to the **4.0 base** biome at that path (the
  intended "prefer vanilla" outcome) - so dropping is safe as long as the path still exists in
  base (it does, for all 16).

---

## The cutover is user-triggered

Per the repo guidelines, the agent does not boot the test server (no console/stdin, rcon
disabled). Every step above that needs a running server - the initial 4.0 pack auto-download,
the `/iris version` check, and the final generation verification - is the user's to run. When
a step needs the server, the agent pauses and asks, stating exactly what changed and that it
is deploy-ready.

---

## Open questions to resolve during the actual migration

- Does 4.0 keep the same pack folder layout (biomes/regions/dimensions/generators/jigsaw-*)?
- Are the custom-biome required fields (`category`, `derivative`, `vanillaDerivative`,
  array-only `place`) unchanged? (See `LESSONS_LEARNED.md` 2026-06-05 entries.)
- Do the cache/prefetch paths (`plugins/Iris/cache|prefetch`, `test/iris/...`) still match,
  or did the namespace/engine change move them?
- Does the `pack-patches` amend approach (`apply-pack-patches.py`) still produce valid 4.0
  JSON?

---

## See Also

- [`IRIS-WORLD-BUILDING.md`](IRIS-WORLD-BUILDING.md) - how the custom world was built (3.9.1 era)
- [`../design/CUSTOM-BIOMES.md`](../design/CUSTOM-BIOMES.md) - biome catalog and JSON schema reference
- [`../scratch/LESSONS_LEARNED.md`](../scratch/LESSONS_LEARNED.md) - dated Iris pitfalls (mostly 3.9.1; re-verify on 4.0)
- [`../scratch/IRIS-ADOPTION-PLAN.md`](../scratch/IRIS-ADOPTION-PLAN.md) - tree/biome adoption inventory
- `iris/scripts/sync-base-pack.ps1` - re-syncs `pack-base` from the server's auto-downloaded pack
- `iris/scripts/deploy-iris-pack.ps1` - assembles base+overlay+objects+patches and deploys
