# Potential Bugs

Backlog of incidental findings discovered while working on other tasks. Each entry is a problem the current task chose not to solve.

**This file is NOT for:** work you just finished, session state, build errors, or roadmap items. See `.junie/AGENTS.md` - *Stay-On-Task Policy* for the full rules.

## Format

```
### YYYY-MM-DD - <short title>

**Discovered during:** <link or short ref to the task>
**Location:** <file path + line range or symbol>
**Symptom / hypothesis:** one or two sentences.
**Impact:** best guess at user-visible effect.
**Suggested next step:** minimal investigation or fix sketch.
```

---

## 2026-06-05 | discovered-during: iris-tree-integration fix

- **Location**: `scripts/apply-tree-integration.ps1` lines 65-75, 228-237, 483-489
- **Symptom**: `apply-tree-integration.ps1` references `temperate/forest.json`, `temperate/forest-extended.json`, `temperate/forest-flat.json`, `tundra/maple-forest.json`, and `mountain/floating-islands.json` - none of these exist in the current server's iris pack. The script emits MISSING warnings for all five.
- **Impact**: Those biomes never receive custom tree objects. If the pack is updated and these files appear, they will be patched automatically on next run - but currently they are silently skipped.
- **Suggested next step**: Confirm whether these biome files exist in a newer version of the overworld pack, or remove the dead entries from the script.

## 2026-06-05 | discovered-during: iris-tree-integration place-array fix

- **Location**: `scripts/apply-tree-integration.ps1` `Trim-Objects` (lines 24-30)
- **Symptom**: The trim regex requires `"place"\s*:\s*"<string>"` with `[^{}]*` between the opening `{` and `place`. Stock Iris object entries store `place` as a multi-line JSON array and contain nested `{...}` (e.g. `rotation`), so the pattern never matches.
- **Impact**: Every `Trim-Objects` call (antioch, tredwood, AmyLarge, etc.) is a silent no-op; stock tree spawn rates are never reduced, only custom trees are added.
- **Suggested next step**: Rework `Trim-Objects` to locate the target object via its `place` array element and adjust `chance` (e.g. brace-aware object scan like the insert logic in `Apply`), rather than a single-line string regex.

## 2026-06-05 | discovered-during: iris-tree-integration place-array fix

- **Location**: `scripts/apply-tree-integration.ps1` `Make-Entry` (line 11) and call sites (e.g. lines 94-97)
- **Symptom**: `Make-Entry` is declared `([string[]]$paths, [double]$chance)` but call sites pass a third positional argument (e.g. `(Make-Entry (Custom "oak" "smol" 10) 0.04 2)`). The trailing number is silently dropped into `$args` and ignored.
- **Impact**: Likely an intended per-tier weight/count that has no effect; placement counts may differ from author intent.
- **Suggested next step**: Decide whether the third value is meaningful; if so, add a `$weight`/`$count` parameter and use it, otherwise remove it from call sites.

## 2026-06-06 | discovered-during: village-meadow rarity / in-game visibility test

- **Location**: `com.volmit.iris.engine.object.IrisBiome.getGenLinkMax(IrisBiome.java:210)` (Iris 3.9.1), reached via `IrisComplex.interpolateGenerators` during chunk-gen on the `test` (Iris:overworld) world.
- **Symptom**: Chunk-gen worker threads flood the log with `NullPointerException: Cannot invoke "Object.hashCode()" because "key" is null` from a `ConcurrentHashMap.get(null)` inside `getGenLinkMax`. Appears tied to a biome `generators` link lookup (e.g. `temperate/meadows.json` uses `generator: "highplains"`); a generator name is resolving to null in the gen-link map.
- **Impact**: Worldgen still proceeds (logged as WARN, not fatal) but per-column height interpolation throws repeatedly, likely degrading gen performance and possibly terrain heights near affected biomes. Not caused by the village-meadow jigsaw wiring (that touches `jigsawStructures`, not `generators`).
- **Suggested next step**: Audit `generators[].generator` names across pack-overlay biomes against the available `generators/` definitions in the active pack; a referenced generator (e.g. `highplains`) may be missing/renamed so its gen-link key resolves to null. Reproduce in isolation on a single biome before changing anything.
- **RESOLVED 2026-06-06 (FINAL)**: Confirmed from the full stack trace that `key` (= `gen.getLoadKey()`) is null, i.e. a globally-registered `IrisGenerator` is the `new IrisGenerator()` fallback (null loadKey). Decompiling Iris 3.9.1 showed `IrisBiome`'s `generators` field defaults to a single `IrisBiomeGeneratorLink` with `generator="default"`; with no `generators/default.json`, `ResourceLoader.load("default")` returns null -> fallback. `IrisComplex` registers generators from EVERY region biome (incl. children) into one global set, so a single biome with NO `generators` block poisons height interpolation for the whole world. Trigger: new cave biomes added to region `caveBiomes` without a `generators` block: `carving/{andesite,desert,ice,sulfur}-caves`, `magnetics/frostfire-caves`, `prismatics/{amethyst,crystal,mantle}-caves`. Fixed by adding `"generators": [{"generator": "rare-hills", "min": 1, "max": 1}]` (mirrors `carving/rocky-cavebiome`) to each; redeployed + caches cleared. NB: the earlier `creaks` `category` fix was a separate valid correction but NOT this NPE's cause. See LESSONS_LEARNED 2026-06-06.

## 2026-06-06 | discovered-during: post-deploy verification of the cave-biome / jigsaw NPE fixes

- **Location**: `iris/pack-patches/assets/jigsaw-*` (village jigsaw structures); referenced sub-pools missing.
- **Symptom (NON-FATAL, INFO-level)**: village generation logs `[Iris]: Can't find jigsaw pool: <name>` for 16 distinct pools and skips them: `village/common/{animals,butcher_animals,cats,sheep,iron_golem,well_bottoms,decor/decor_grass_patches}`, `village/{plains,savanna,snowy,taiga,meadow_swiss}/villagers`, `village/plains/decor`, `village/beach_lighthouse/villager_lighthouse_master`, `village/sparse_jungle_polynesian/villager_village_chief`, `village/modded/waystones/waystone_default`.
- **Impact**: villages still generate (no NPE, no failed chunks), but lack villagers/animals/iron golems/cats/decor and the modded waystone; `village/modded/waystones` requires the Waystones mod and is expected to be absent.
- **Suggested next step**: port the missing `common` mob-spawn + `<biome>/villagers` + `decor` pools (and their pieces) from the source village pack into `pack-patches/assets/jigsaw-pools` (+ pieces), or remove the dangling pool references from the consuming pieces if these features are intentionally dropped. Not blocking generation.

## 2026-06-07 | discovered-during: biome additions execution (Phases 1-4)

- **Location**: iris/pack-overlay-backup* vs live iris/pack-overlay/biomes/; clutter/magmaspire1-3 object references in volcano-family biomes.
- **Symptom / hypothesis**: The backup overlay sets and the design docs marked custom biomes as "Done" while they were absent from the live overlay; the backup volcano biomes referenced clutter/magmaspire1-3 objects that did not exist anywhere under iris/ (pack-base/output/staging). Divergent overlay sets (pack-overlay, pack-overlay-backup, pack-overlay-backup-20260605-213215) can mislead future audits.
- **Impact**: Without the objects, the ported volcano biomes would fail object placement at gen time; the stale "Done" status risked masking missing live content. Both addressed this session (magmaspires generated via iris/scripts/build-magmaspires.py; biomes authored into live overlay).
- **Suggested next step**: Consider pruning or clearly archiving the pack-overlay-backup* directories so they are not mistaken for the authoritative overlay; add a one-line README in each backup noting it is a historical snapshot.

## 2026-06-07 | discovered-during: seasonal-biome (Spring) implementation

- **Location**: `iris/pack-overlay/biomes/temperate/auroral-garden.json` lines 74-77 (`objects[].place`).
- **Symptom / hypothesis**: Auroral Garden places `trees/birch/antioch1` and `trees/birch/antioch2`, but no `antioch*.iob` exists anywhere under `iris/` (output/staging/pack-base/pack-overlay). The valid birch objects are under `trees/vanilla-birch/birch*`.
- **Impact**: The biome's birch-tree object placement silently fails at gen time, so Auroral Garden generates without its intended birch canopy (only the spruce object block places).
- **Suggested next step**: Repoint the `antioch1/2` entries to existing objects (e.g. `trees/vanilla-birch/birchM1..M8`, as used by the new Spring Meadow biome), or restore/author the `antioch` birch objects if a distinct rainbow-birch asset was intended.

---

### 2026-06-08 - Two dangling object refs surviving a clean pack snapshot

- **Discovered-during**: working remaining `Couldn't find Object` errors from `testServer/RTP-Paper/26.1/logs/latest.log` (460/462 were a stale frozen world snapshot, now fixed by wiping `test/iris/pack` + clean-deploy).
- **Location**: `iris/pack-overlay/biomes/temperate/stranged-plains.json` lines 91-95 (`void/void` x3) and `iris/pack-overlay/biomes/ocean/deep.json` line 97 (`jigsaw/ocean-monument/ocean_monument`).
- **Symptom / hypothesis**: Both keys exist in NO pack version (source pack 11090 .iob, pack-base, iris/output, pack-overlay) - genuine content gaps, not snapshot drift. `void/void` is also dead upstream in `pack-base` and has chance 2e-07. `jigsaw/ocean-monument` has no matching jigsaw object/piece/pool/structure.
- **Impact**: `void/void` effectively never places (negligible). `ocean-monument` never generates, so deep oceans get no Iris-placed monuments.
- **Suggested next step**: Drop the `void/void` placer from the stranged-plains overlay; for the monument, either remove the placer (no monuments) or supply a real `jigsaw/ocean-monument` asset.
- **RESOLVED 2026-06-08 (ocean-monument)**: The `jigsaw/ocean-monument/ocean_monument` object exists in no pack version (incl. the Folia server pack and `_pack-base-39-backup`, which only has the jigsaw piece/structure JSON, not the `.iob`), so the placer only logged a "Couldn't find Object" error and never generated a monument. Removed the dead placer from `ocean/deep.json` (now `"objects": []`); real ocean monuments still generate via the dimension's vanilla `importedStructures` (ALL_ON). The `void/void` placer in `stranged-plains.json` remains (never appears in logs, chance 2e-07).

---

### 2026-06-09 - Dark-forest giants likely float above ground (same root cause as taiga spruce)

- **Discovered-during**: dropping the taiga mega-spruce giants to ground level.
- **Location**: `datapacks/leaf-worldgen/data/leaf/structure/dark_forest/*.nbt` (titan/spiral dark oaks), placed via `leaf:dark_forest_giant` jigsaw.
- **Symptom / hypothesis**: Those NBTs also bake the generator's downward root system (taproot + buttress legs) into the bottom `root_depth_for(height)` layers (verified: y=0 layers are dark_oak_log roots). Vanilla jigsaw worldgen anchors the structure floor to the surface heightmap, so the trunk base floats up by ~`depth` blocks - identical to the taiga bug just fixed.
- **Impact**: Dark-forest landmark giants probably spawn floating on stilt-like roots; only manifests in the custom dark-forest dimension (not loaded on Paper), so it may have gone unnoticed.
- **Suggested next step**: Apply the same fix (strip layers below the trunk base, shift down by `depth`) to the four dark_forest `.nbt`, e.g. by generalising `tools/tree-gen/_fix_roots.py`; or, better, stop baking roots into exported giant NBTs at generation time.
- **RESOLVED 2026-06-25 (natural placement)**: User reported the float in-game for the LeafTreeGen-placed dark-forest canopy. Fixed by setting `"roots": false` on every dark-oak definition in `configs/LeafTreeGen/species/dark-forest.json`, `dark-forest-mids.json`, and `dark-forest-megas.json` (same fix already applied to the taiga mega spruces), so the plugin no longer bakes the downward root system into the heightmap-anchored NBTs and the trunk base sits on the surface. NOTE: the separate `datapacks/leaf-worldgen/data/leaf/structure/dark_forest/*.nbt` landmark giants (custom dark-forest dimension, not loaded on Paper) are pre-baked binaries and still carry roots; regenerate them via the tree-gen tooling if that dimension is ever shipped.

---

### 2026-06-09 - Worldgen "Feature order cycle" crashes the overworld (jacaranda_grove vs windswept_savanna)

- **Discovered-during**: booting the test server to verify the leaf-mobs framework (LeafMobs itself loaded fine).
- **Location**: `datapacks/leaf-worldgen/data/leaf/worldgen/biome/jacaranda_grove.json` feature lists vs vanilla `minecraft:windswept_savanna`.
- **Symptom / hypothesis**: Chunk gen aborts with `IllegalStateException: Feature order cycle found, involved sources: [minecraft:windswept_savanna, leaf:jacaranda_grove]`. The two biomes list shared placed features in conflicting relative orders, so MC cannot compute a single global feature order. Propagates to `unrecoverableChunkSystemFailure` and the server shuts down.
- **Impact**: FATAL - the overworld cannot generate new chunks; server crashes on boot/first gen, blocking all in-game testing (including the mob framework).
- **Suggested next step**: Align the ordering of the shared placed features (especially vegetation/tree decoration steps) in `leaf:jacaranda_grove` to match the order used by vanilla biomes like `windswept_savanna` (do not interleave features in a different relative order). Compare each `features[step]` list entry-by-entry against a conflicting vanilla biome and reorder to remove the cycle.

---

### 2026-06-09 - Two leaf-worldgen biome JSONs carry a UTF-8 BOM

- **Discovered-during**: Tier 2 custom biome work (validating JSON parses).
- **Location**: `datapacks/leaf-worldgen/data/leaf/worldgen/biome/taiga_body.json` and `taiga_core.json` (byte 0 = EF BB BF).
- **Symptom / hypothesis**: Both files begin with a UTF-8 BOM; every other leaf-worldgen JSON is BOM-less. Strict UTF-8 parsers (e.g. Python `json.load` without `utf-8-sig`) reject them; Minecraft/Gson tolerate a BOM so worldgen is unaffected.
- **Impact**: No in-game effect (Gson skips the BOM); only trips tooling/CI that reads the files as plain UTF-8.
- **Suggested next step**: Re-save both files as UTF-8 without BOM to match the rest of the pack (no content change needed).

---

### 2026-06-25 - dungeons_arise foundry_passages.json declares a duplicate pool name

- **Discovered-during**: fixing the "Empty or non-existent pool: dungeons_arise:underworld/foundry/foundry_corridor_gears" worldgen spam.
- **Location**: `datapacks/dungeons-arise/data/dungeons_arise/worldgen/template_pool/underworld/foundry/foundry_passages.json` line 2.
- **Symptom / hypothesis**: The file's `name` is `dungeons_arise:underworld/foundry/foundry_corridors`, which collides with the separate `foundry_corridors.json` pool (same `name`). Looks like a copy-paste slip; the `name` should likely be `.../foundry_passages`.
- **Impact**: A jigsaw block targeting `foundry_passages` resolves to whichever pool registration wins; the intended passages pool may never be selected.
- **Suggested next step**: Confirm against upstream When Dungeons Arise data whether `foundry_passages.json` should declare `name` `.../foundry_passages`; if so, correct the `name` field.
- **RESOLVED 2026-06-25**: Set `foundry_passages.json` `name` to `dungeons_arise:underworld/foundry/foundry_passages`, removing the collision with `foundry_corridors.json`. (Registration keys derive from the file path, so this is a cosmetic correctness fix - no runtime resolution change.)

### 2026-06-25 - "Unsafe terrain read" spam for nova_structures:cave_chamber_archeology_ruins

- **Discovered-during**: same worldgen log triage.
- **Location**: `datapacks/dungeons-and-taverns/data/nova_structures/worldgen/structure/cave_chamber_archeology_ruins.json` (`size: 4`, `terrain_adaptation: none`, `max_distance_from_center: 128`).
- **Symptom / hypothesis**: Paper logs "Detected unsafe terrain read during worldgen" (distance 2, write radius 1) during the `features` step for this jigsaw. A feature in one chunk reads terrain from a chunk beyond the structure write radius - inherent to a large multi-piece jigsaw, not a malformed-data error.
- **Impact**: Log spam and a small risk of order-dependent feature placement; generation itself proceeds (diagnostic, not a crash).
- **Suggested next step**: If undesirable, consider reducing the structure `size`/`max_distance_from_center` or a terrain_adaptation mode - but verify in-game the structure still assembles before shipping.
- **RESOLVED 2026-06-25**: Lowered `max_distance_from_center` from `128` (extreme max) to `80` (vanilla-village-typical) to tighten piece spread and reduce the cross-chunk `features`-step reads. No Paper config toggle exists for this diagnostic, and the pool has no `terrain_matching` pieces, so reducing spread is the only datapack lever. Needs a user-triggered boot to confirm the spam drops and the structure still fully assembles.
- **RESOLVED 2026-06-26 (root cause of recurrence)**: The spam reappeared on the active `26.2` server because its `start.bat` already carried `-Dlog4j.configurationFile=log4j2.xml` but the `log4j2.xml` file was MISSING from the server root, so Log4j2 silently fell back to its default config and the suppression RegexFilter never loaded. Re-ran `configs/deploy-logging-config.ps1 -ServerBase ...\26.2` to copy `log4j2.xml` into place (start.bat flag left as-is). Takes effect on the next user-triggered boot. Lesson: deploying logging requires the file present on the SAME server base the flag points at; check both when a new server version dir is spun up.

### 2026-06-26 - "Unsafe terrain read" spam for towns_and_towers:village_snowy_taiga

- **Discovered-during**: worldgen log triage (snowy-taiga viking village).
- **Location**: `datapacks/towns-and-towers/data/towns_and_towers/worldgen/structure/village_snowy_taiga.json` (was `max_distance_from_center: 116`, `size: 6`, `terrain_adaptation: beard_thin`).
- **Symptom / hypothesis**: Same benign Paper diagnostic ("Detected unsafe terrain read during worldgen", distance 2, write radius 1) during the `features` step - a large multi-piece jigsaw village reads terrain from chunks beyond its write radius. Already globally suppressed by the generic RegexFilter in `configs/logging/log4j2.xml`; generation proceeds (diagnostic, not a crash).
- **Impact**: Log spam (when the logging config is not deployed) plus a small order-dependent feature-placement risk; no crash.
- **RESOLVED 2026-06-26**: Lowered `max_distance_from_center` from `116` to `80` (vanilla-village-typical, matching the `village_beach`/`village_grove`/`village_snowy_slopes` siblings) to tighten piece spread and reduce the cross-chunk `features`-step reads. The remaining `116`-distance towns_and_towers villages (e.g. `village_birch_forest`, `village_forest`, `village_jungle`, `village_swamp`) can emit the same benign diagnostic but stay log-suppressed; tighten them similarly only if needed. Needs a user-triggered boot to confirm the spam drops and the village still fully assembles.
- **FOLLOW-UP 2026-06-26 (`village_swamp`)**: `village_swamp` escalated past the benign "unsafe terrain read" to the more severe "Detected setBlock in a far chunk" error (swamp boat village actually writing blocks into chunks beyond the safe write radius during the `fluid_springs`/`features` step). Applied the same lever: lowered its `max_distance_from_center` from `116` to `80`. Needs a user-triggered boot to confirm the errors stop and the swamp village still fully assembles.
- **FOLLOW-UP 2026-06-26 (`village_snowy_taiga`, far-chunk recurrence at 80)**: After the world was regenerated, `village_snowy_taiga` STILL emitted "Detected setBlock in a far chunk" (e.g. chunk [-322,-357], pos x=-5150 y=111, during `minecraft:features` for `towns_and_towers:village_snowy_taiga`), so `80` was insufficient for this sprawling snowy-taiga viking village (size 6, beard_thin, tall longhouse/watchtower pieces on hilly snowy slopes spread past the safe write radius). Lowered `max_distance_from_center` further from `80` to `64` to clamp piece spread within the write radius. Needs a user-triggered world-reset boot to confirm the far-chunk errors stop and the village still fully assembles; if it persists, the next lever is reducing `size` (6 -> 5).
