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
