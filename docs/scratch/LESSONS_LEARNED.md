# Lessons Learned

Dated engineering pitfalls, non-obvious behaviors, and "things that bit us". Add entries here so future sessions don't repeat the same mistakes.

## Format

```
### YYYY-MM-DD - <short title>

**Context:** what task or feature was being worked on.
**What happened:** the surprising or painful behavior.
**Fix / workaround:** what resolved it.
```

---

### 2026-06-06 - Iris objects never PASTE air; carved pieces need stored air + `bore`

**Context:** Mineshaft jigsaw rooms generated sporadic and disconnected in spectator mode, and a few pieces did not connect "due to lack of overlap". Suspicion was that some mineshaft pieces "fail to paste air".
**What happened:** Two separate Iris `IrisObject` facts combine here. (1) `IrisObject.place` computes `place = !AIR && !CAVE_AIR ...` - it NEVER writes air from an object, so you cannot carve a tunnel just by putting air in the `.iob`. (2) `IrisObject.shrinkwrap()` recomputes the object's `width/height/depth` AND `center` from the stored blocks only. Our `nbt_to_iris.py` dropped `minecraft:air` on conversion, so each mineshaft piece shrank to its solid-block extent. That made the `bore` placement (lines 761-770: the ONLY thing that hollows a piece, by setting the whole bounding box to AIR before placing solids) under-carve the real footprint, and - because connectors were centered on the FULL vanilla size while blocks got re-centered on the smaller shrinkwrapped box on rotation - shifted pieces off their connectors, so neighbours stopped overlapping.
**Fix / workaround:** Added an opt-in `--keep-air` flag to `nbt_to_iris.py` (split `SKIP_BLOCKS` into `ALWAYS_SKIP_BLOCKS` = `structure_void`/`jigsaw`, plus a conditional air drop). With `--keep-air` the air cells are stored - still not pasted, but they preserve the full footprint/center so `bore` carves the whole tunnel and connectors stay aligned. Mineshaft (and any cave/`bore` structure) must be re-converted with `--keep-air` and the `.iob` objects redeployed. Left off by default so surface villages (which do not `bore`) keep their current output. Default is fine for surface pieces; air only matters when a piece relies on `bore`.

### 2026-06-06 - Mineshaft `bore` carve uses declared w/h/d; a thin object + bedrock Y-range broke it

**Context:** Follow-up to the air fix. Mineshaft rooms were still sporadic/disconnected ("lack of overlap"), pieces looked "reduced to minimum size, excluding air", and some rooms carved through bedrock. The actual mineshaft `.iob` are NOT in the repo pipeline (not in pack-base/overlay/output/pack-patches) - they only existed in the deployed server pack and persisted because the deploy copies over the destination without cleaning it.
**What happened:** Two distinct causes. (1) `IrisObject.place` `bore` (the AIR-carve loop) sizes the carved box from the object's DECLARED `w/h/d` using `floorDiv(d,2)` - it does NOT use the block extent. The deployed `way_empty.iob` was 6x3x**1**, so bore hollowed only a single-block-deep slice that never overlapped the d=3 corridors -> disconnected passages. `shrinkwrap()` was a red herring here: it early-returns on empty objects and the solid pieces already span their full box. (2) The overlay `jigsaw-structures/mineshaft.json` had lowered the stock Y range (min 7) to min **-50**; world floor is -64 and bedrock generates up to ~-60, so deep bored placements carved into the bedrock band.
**Fix / workaround:** Reconstructed all 8 mineshaft objects as regenerable JSON sources under `iris/object-src/jigsaw/mineshaft/*.json` (centered, fully air-filled across the bounding box so the footprint/center are always preserved), compiled with `json_to_iob.py` to `iris/pack-overlay/objects/jigsaw/mineshaft/*.iob` (overlay is copied into staging->dest by deploy, overriding the stale server copies). `way_empty` was forced to 6x3x3. Raised the structure Y-min from -50 to -40. TEMPORARY: dropped the dimension `mineshaft` rarity from 240 to 4 (much more common) to make in-game verification easy - REVERT to 240 after testing.

### 2026-06-06 - Iris jigsaw `/ by zero` crash = too-small `rarity` makes `spacing`=0

**Context:** After dropping the dimension `mineshaft` rarity to 4 to make spawns easy to find, the test server crashed during world gen: every `structure_starts` chunk task threw `java.lang.ArithmeticException: / by zero` at `IrisJigsawStructurePlacement.shouldPlaceSpread` (`Math.floorDiv(x, spacing)`), via `IrisStructurePopulator.pick`.
**What happened:** When a jigsaw placement omits `spacing`/`separation`, Iris derives them in `calculateMissing`: `separation = round(rarity / divisor)`, then `spacing = rng.i(separation, separation*2)`. With a tiny `rarity` (4), `separation` rounds to **0**, so `spacing` becomes `rng.i(0,0) = 0`, and `shouldPlaceSpread` does `floorDiv(x, 0)` -> crash on every column. The high-rarity stock value (240/1600) never hit this.
**Fix / workaround:** Set explicit non-zero `spacing`/`separation` on the placement (`"spacing": 8, "separation": 4`) instead of relying on a tiny `rarity` to densify. Keeps the dense spawn for testing without zero-spacing. REVERT to the stock `rarity: 240` (and drop the explicit spacing/separation) after testing.

### 2026-06-06 - Iris jigsaw children scatter vertically unless the connector has `lockY: true`

**Context:** Mineshaft rooms still did not connect after the object/geometry fixes: pieces were "off by one or two in height", some separated by a wall, and mostly there were no visible paths between the carved pockets. The world was confirmed clean and the deployed objects were the correct sizes (corridors 6x3x3, intersections 3x3x3, air-filled).
**What happened:** In `PlannedStructure.place`, a child piece only inherits its parent's Y when the PARENT connector has `lockY == true` (`connection.connector().isLockY()` -> `height = targetPosition.getY(); offset = 0`). Otherwise Iris recomputes each piece's Y INDEPENDENTLY from the structure's `overrideYRange` (ours is `STATIC`, position-seeded), so every piece lands at its own height. With `IrisJigsawPieceConnector.lockY` defaulting to **false**, all 8 mineshaft pieces let every connection re-roll Y -> vertically scattered, disconnected pockets even though X/Z adjacency and the bore carve were correct. The horizontal openings (corridor faces are solid at z=+/-1, air at z=0) line up fine once Y is consistent.
**Fix / workaround:** Added `"lockY": true` to every connector in all 8 `pack-overlay/jigsaw-pieces/mineshaft/*.json` pieces, then redeployed. Lesson: for any chained/leveled jigsaw (mineshaft corridors, dungeons), connectors MUST set `lockY: true` or pieces drift on Y; `lockY` is off by default.

### 2026-06-06 - Iris jigsaw pools are UNWEIGHTED; dilute with alias pieces to lower a piece's odds

**Context:** After mineshafts connected, the ask was to cut cave-spider spawner rooms (the `way_spawner` piece - cobwebs + `minecraft:spawner`) by >=75% while keeping some.
**What happened:** Iris jigsaw pools have NO weight/chance/limit field (`IrisJigsawPool` is just `pieces: KList<String>`, `IrisJigsawPiece` has no count field), and `PlannedStructure.getShuffledPiecesFor` `addIfMissing`s each loaded piece then picks the first that places -> effectively UNIFORM over the distinct pieces. So you cannot weight by listing a piece twice (same key/instance dedups). A quick BFS sim (start=intersection, maxDepth 10) showed `way_spawner` averaged ~22 rooms/structure; removing it from the `generic` pool only cut 46% (corridors are not the only spawner path; intersections feed the `way` pool too), and removing it from both pools -> 0 (too much).
**Fix / workaround:** Diluted instead: added 10 ALIAS corridor pieces (2 extra copies each of way_1/way_2/way_chest/way_rails/way_empty, e.g. `way_1b`/`way_1c`) that point at the SAME existing `.iob` objects but are distinct piece keys (distinct loader instances -> not deduped), and listed them in overlay overrides of both `jigsaw-pools/mineshaft/generic.json` and `way.json`. Sim: ~79% fewer spawner rooms, variety preserved. Lesson: to rebalance an Iris jigsaw pool, add/remove distinct piece keys - duplicating one key does nothing. Also reverted the temporary test settings: dimension `mineshaft` back to stock `rarity: 240` (dropped the explicit `spacing`/`separation`).

### 2026-06-06 - Iris world = the bukkit world named `test`, not `world`; deploy already wipes it

**Context:** After fixing the pack the user reported "no change since before the errors". The deployed pack was actually current.
**What happened:** The Iris-generated overworld is the bukkit world named **`test`** (`bukkit.yml`: `worlds: test: generator: Iris:overworld`), NOT the vanilla `level-name=world`. Previously-generated chunks in `test/region` (etc.) reload from disk and never regenerate against an updated pack, so pack edits appear to have "no effect" until those chunks are wiped. The `deploy-iris-pack.ps1` cache list already targets `test/region|entities|poi|mantle`, `test/iris/engine-data|cache`, and `plugins/Iris/cache|prefetch`, so running deploy both ships the pack AND cleans the world. The catch: any server boot AFTER a deploy regenerates chunks again, so to retest cleanly you must re-run deploy (or re-clear those dirs) right before the boot.
**Fix / workaround:** To "clean + update" the world: run `deploy-iris-pack.ps1` (rebuilds staging from repo, deploys to both dests, clears all the above caches) immediately before a user-triggered boot. Do not look for changes in `world/` - it is vanilla and unused for Iris gen.

### 2026-06-06 - Recreate the T&T waystone pool WITHOUT the Waystones mod block

**Context:** Replacing the empty stub pool `village/modded/waystones/waystone_default` with a real piece, but the server has no Waystones mod. The base T&T data (`thirdparty-derived/.../structure/village/modded/waystones/waystone_default.nbt`) is only a 1x1x1 `minecraft:air` placeholder - the real piece lives in the jar's separate `t_and_t_waystones_patch` overlay (`resources/t_and_t_waystones_patch/data/kaisyn/.../waystone_default.nbt`).
**What happened:** The patch piece is 1x3x1: a `minecraft:jigsaw` bottom block whose `final_state` is `minecraft:polished_andesite` (the base pad), topped by two `waystones:waystone[half=lower/upper]` blocks. Converting it as-is would place a `waystones:waystone` block that does not exist on this server.
**Fix / workaround:** Converted the patch piece manually (mirroring `nbt_to_iris.py` centering + `PLACEMENT_OPTIONS`) but skipping any `waystones:*` block and emitting the jigsaw's `final_state` as a real block. Result: `.iob` keeps only the single `polished_andesite` pad; the bottom connector is preserved. Created the piece + pool under `pack-patches/assets/...`. `jigsaw_validate.py` then shows `village-beach` 5 -> 6 reachable, no errors, and the `Can't find jigsaw pool: village/modded/waystones/waystone_default` warning is gone. Documented in `iris/pack-patches/README.md` ("Waystone pool recreated without the Waystones block").

### 2026-06-06 - Silence `Can't find jigsaw pool: village/...` with empty stub pools

**Context:** Following up the same warning spam, the decision was to actually create every still-missing pool rather than leave it as tolerable noise. A static scan of all `jigsaw-pieces` connectors across `pack-patches/assets` + `pack-base` found 14 distinct unresolved pools: `village/common/{cats,animals,butcher_animals,sheep,iron_golem,well_bottoms}`, `village/common/decor/decor_grass_patches`, `village/{plains,savanna,snowy,taiga,meadow_swiss}/villagers`, `village/plains/decor`, and `village/modded/waystones/waystone_default`.
**What happened:** None of these have convertible content - they are vanilla `minecraft:` entity-spawn pools (cats/animals/villagers/iron_golem), a `feature_pool_element` (grass patches/decor), a vanilla well-bottom NBT, or the Waystones mod pool - so there is no `.iob` object to wrap. `meadow_swiss/villagers` is referenced once by `meadow_shepherd_1` but the mod never ships that pool either.
**Fix / workaround:** Shipped an empty stub pool (`{"pieces": []}`) at each of the 14 keys under `iris/pack-patches/assets/jigsaw-pools/village/...`. An empty pool resolves cleanly (no warning) and attaches nothing, so generation is unchanged. The re-run scan reports `MISSING POOLS: 0` and `jigsaw_validate.py` on `village-beach`/`village-sparse-jungle`/`village-meadow` shows zero warnings. Design rationale + per-pool table are documented in `iris/pack-patches/README.md` ("Stub population pools"). Do NOT add pieces to these stubs unless a real converted `.iob` exists.

### 2026-06-06 - Unique T&T villager NBTs convert but get no wrapping pool

**Context:** Triaging the runtime spam `[Iris]: Can't find jigsaw pool: village/...`. Cross-checked the authoritative mod jar (`Downloads\t_and_t-fabric-neoforge-1.13.9.jar`) against `iris/thirdparty-derived/towns-and-towers/raw/` - they match, so nothing extractable was missing.
**What happened:** Most warnings are upstream/tolerable: `village/common/{cats,iron_golem,animals,sheep,butcher_animals,well_bottoms}`, biome `villagers`/`decor`, and `modded/waystones/waystone_default` are vanilla `minecraft:`/modded pools the mod itself never ships (and `common/decor/decor_grass_patches` is a vanilla `feature_pool_element`, not an object Iris can render). BUT two unique villagers - `village/beach_lighthouse/villager_lighthouse_master` and `village/sparse_jungle_polynesian/villager_village_chief` - had their pieces AND `.iob` objects converted, yet no pool wrapped them, so the town-center jigsaw could never place them. The `lighthouse_master` piece also carried a spurious self-loop connector pool pointing at itself (a conversion artifact; the correct `village_chief` piece has a bare `minecraft:bottom` connector with no `pools`).
**Fix / workaround:** Added the two wrapping pools under `iris/pack-patches/assets/jigsaw-pools/village/...` (each `{"pieces": ["<the villager piece>"]}`) and stripped the self-loop `pools` from the `lighthouse_master` piece connector. `jigsaw_validate.py` then showed `village-beach` 4 -> 5 reachable pieces and `village-sparse-jungle` 30 -> 31, with those two warnings gone and no new ones. The remaining `common/*`, biome `villagers`, and `waystones` warnings are expected and left as-is.

### 2026-06-06 - nbt_to_iris must read template_pools RECURSIVELY (nested pools)

**Context:** Batch-importing the rest of the Towns & Towers villages (`import-tnt-villages.py`) into the patch layer. Some converted villages generated with only their town center (validator `reachable pieces : 1`) e.g. `village-beach`, `village-sunflower-plains`.
**What happened:** `nbt_to_iris.py` converted structure *pieces* recursively (`os.walk`) but converted *template_pools* with a flat `os.listdir(pool_src)`. T&T nests pools in subfolders (e.g. `template_pool/village/beach_lighthouse/side/house.json`, `.../side/cross.json`). The flat scan skipped every nested pool, so the start piece's connectors pointed at unconverted pools and the jigsaw could not expand past the center. `meadow_swiss` happened to have all pools flat, which is why it worked first and masked the bug.
**Fix / workaround:** Walk `pool_src` recursively and preserve the relative subpath in the output pool key (`set_prefix + "/" + relsubpath`). After the fix, `village-beach` went 1 -> 4 reachable, `village-sunflower-plains` 2 -> 8, `village-mushroom-fields` 17 -> 25. Genuinely tiny T&T variants (`grove_villager_outpost`, `snowy_slopes_inn`) legitimately stay at 1-2 pieces - their only unresolved pools are the tolerable population pools (villagers/cats/iron_golem/waystones). Always re-run `jigsaw_validate.py` per structure after converting and treat a low `reachable pieces` count as a wiring smell, not "done".

### 2026-06-06 - Iris jigsaw piece references: only `.object` gets the `jigsaw/` prefix

**Context:** Wiring the Towns & Towers `village_meadow` jigsaw into Iris (`nbt_to_iris.py`) and verifying it would actually spawn.
**What happened:** The converter prefixed piece references with `jigsaw/` everywhere. That is wrong. In a working Iris pack the prefixing is asymmetric:
- `jigsaw-structures/<key>.json` `.pieces[]` -> jigsaw-PIECE keys, NO prefix (e.g. `village/plains/town_centers/fountain_01`).
- `jigsaw-pools/<pool>.json` `.pieces[]` -> jigsaw-PIECE keys, NO prefix.
- `jigsaw-pieces/<piece>.json` `.object` -> OBJECT key, WITH `jigsaw/` prefix (e.g. `jigsaw/village/plains/...`).
- `jigsaw-pieces/...connectors[].pools[]` -> jigsaw-POOL keys, NO prefix.
With the extra prefix Iris cannot resolve any piece, so nothing connects and no village spawns.
**Fix / workaround:** Drop the `jigsaw/` prefix when emitting pool/structure `pieces`; keep it only on the piece `.object`. Added `iris/scripts/jigsaw_validate.py`, a dependency-free loader-mirroring resolver: it walks structure -> pools -> pieces -> `.iob` objects and reports unresolved refs. Missing population pools (villagers/cats/sheep/iron_golem/waystones) are tolerable WARNINGS (Iris just attaches nothing there); a missing `.iob` for a reachable piece is a hard ERROR. Note: the pack's stock `village-*` jigsaws have NO `.iob` objects under `objects/jigsaw/` at all, so they would not render either - vanilla village objects must still be produced/deployed.

### 2026-06-05 - Iris objects store block coords relative to center (custom trees floated)

**Context:** Custom tree `.iob` objects placed by Iris floated dozens of blocks above the ground, worse for larger trees (offset scaled with height).
**What happened:** Iris stores object block coordinates RELATIVE TO THE OBJECT CENTER (`w//2, h//2, d//2`) and re-adds `getCenter()` at placement time (`IrisObject.place` -> `at.add(getCenter()).add(i)`). Stock objects are saved this way (e.g. `antioch1`: w8/h18/d9 -> x[-4,3] y[-9,8] z[-4,4]). Our generator (`scripts/tree-gen/nbt.py`) normalised coords to a 0-based origin (base at y=0), so every object was shifted up by ~h//2 -> giants floated ~38 blocks. A small `translate.y` could not compensate (it is dwarfed by h//2).
**Fix / workaround:** `nbt.py write_iob` now subtracts the center on all axes. Existing compiled `.iob`s were re-centered in place with `scripts/recenter-iob.py` (idempotent: skips files that already have a negative min coord). After re-centering, objects match stock and sit on the ground; the per-placement `translate.y` (default -3, via `-CustomYOffset` in `apply-tree-integration.ps1`) just mirrors stock root burial.



### 2026-06-05 - Iris biome object placement format: array only, never bare string

**Context:** Editing biome JSON files to fix missing object references.
**What happened:** Used `{ "place": "trees/foo/bar", "chance": 0.05 }` (bare string) instead of the correct array format. Iris only accepts `place` as an array. The bare-string format causes `IrisObjectPlacement` parse errors at startup.
**Fix / workaround:** Always use array format with `density` and `rotation`:
```json
{
  "chance": 0.05,
  "density": 2,
  "rotation": { "yAxis": { "min": 0, "max": 270, "interval": 90, "enabled": true }, "enabled": true },
  "place": ["trees/foo/bar1", "trees/foo/bar2"]
}
```
Never use `{ "place": "single/string", "chance": 0.05 }` - this is invalid and will break Iris.


### 2026-06-05 - Safe additive edits to Iris biome JSON files

**Context:** Adding custom tree entries to vanilla Iris biome JSON files without breaking the pack.
**What happened:** Round-tripping a biome JSON through PowerShell `ConvertFrom-Json` / `ConvertTo-Json` then writing it back causes Iris to reject the file with "JSON document was not fully consumed". PowerShell's serializer changes formatting, adds trailing commas, and restructures arrays in ways Iris's strict parser rejects. Deleting a JSON file from an existing pack directory also causes errors - Iris detects the presence of the `overworld` directory and expects all files to be present. Downloading or replacing pack files via script introduces BOMs and format corruption.
**Fix / workaround:** The only safe method for additive edits:
1. Read the file as raw text with `[System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)`.
2. Use regex to remove the trailing `]\n}` (closing of objects array + root object): `$raw.TrimEnd() -replace '\s*\]\s*\}\s*$', $newEntry` where `$newEntry` is a raw JSON string that ends with `  ]\n}`.
3. Validate with `$updated | ConvertFrom-Json | Out-Null` before writing.
4. Write back with `[System.IO.File]::WriteAllText($path, $updated, (New-Object System.Text.UTF8Encoding($false)))` - the `$false` suppresses BOM.
5. Never use `ConvertTo-Json` to rewrite a biome file - only use it for validation.
6. Never delete files from an existing pack directory.
7. Never download or copy pack files from external sources - the server auto-downloads the correct version on startup when the `overworld` directory is absent.
8. Custom tree `.iob` files from other biome folders (e.g. `trees/darkoak/spiral_large_1`) can be referenced in any biome - the path is relative to `objects/` in the pack, not the biome folder.
9. After a successful additive edit directly on the server file, always copy the working server file back to `iris-pack/` so the repo stays in sync: `Copy-Item <server-file> <iris-pack-file> -Force`. The deploy script copies `iris-pack` as-is, so a stale repo file will overwrite the working server file on next deploy.
10. Never use PowerShell `-replace` with a scriptblock (`{ param($m) ... }`) on pack JSON files - PowerShell treats the scriptblock as a literal string replacement, injecting `' + {` artifacts and duplicating the entire file. Use `[regex]::new().Replace()` with a `[System.Text.RegularExpressions.MatchEvaluator]` delegate, or better yet use Python for any non-trivial JSON field transformations on pack files.

### 2026-06-05 - Iris custom biome JSON: exact required fields and valid values

**Context:** Adding custom biomes to the Iris overworld pack overlay.
**What happened:** Multiple fields had invalid values causing `IrisBiomeCustom.getCategory()` to return null and crash Iris on startup with `NullPointerException` at `IrisBiomeCustom.generateJson`.
**Fix / workaround:** Every custom biome JSON (and every entry in `customDerivitives` arrays) must follow these exact rules:

1. **`category` field is required** at the top level of every biome file AND inside every `customDerivitives` array entry. Without it, `getCategory()` returns null.
2. **Valid `category` values** (must be exact lowercase): `beach`, `desert`, `extreme_hills`, `forest`, `icy`, `jungle`, `mesa`, `mushroom`, `nether`, `none`, `ocean`, `plains`, `river`, `savanna`, `swamp`, `taiga`, `the_end`. Values like `frozen`, `mountain`, `tropical`, `tundra` are NOT valid.
3. **`derivative` field** uses uppercase Minecraft biome enum names (e.g. `FROZEN_PEAKS`, `DARK_FOREST`). These are the `IrisBiome` derivative values, not the `IrisBiomeCustomCategory` enum.
4. **`vanillaDerivative` field** also uses uppercase Minecraft biome names (e.g. `SNOWY_TAIGA`, `BADLANDS`). This is separate from `category`.
5. **`place` arrays** must always be arrays, never bare strings: `"place": ["trees/foo/bar"]` not `"place": "trees/foo/bar"`.
6. **No BOM, no CRLF** - files must be UTF-8 without BOM, LF line endings only. Both PowerShell `Set-Content`/`Out-File` AND the Junie `create` tool write UTF-8 BOM by default. After creating any JSON file via either method, strip the BOM with: `$b = [System.IO.File]::ReadAllBytes($f); if ($b[0] -eq 0xEF) { [System.IO.File]::WriteAllBytes($f, $b[3..($b.Length-1)]) }`
7. **No PowerShell JSON round-tripping** - never use `ConvertFrom-Json` / `ConvertTo-Json` to rewrite biome files. Use Python scripts or raw text manipulation only.
8. **Deployment pipeline**: base pack (Layer 1) + overlay custom files (Layer 2) + custom `.iob` files (Layer 3). The overlay must contain ONLY files not present in the base pack, or intentional overrides. Run `iris/scripts/check-overlay-conflicts.ps1` to verify.
## 2026-06-06 - Iris "Failed to sample hi/lo biome" NPE = biome with NO `generators` block

- Symptom: chunk-gen worker threads flood the log with `java.lang.NullPointerException: Cannot invoke "Object.hashCode()" because "key" is null` at `IrisBiome.getGenLinkMax` (a `ConcurrentHashMap.get(null)`), via `IrisComplex.interpolateGenerators` -> `getHeight`.
- Root cause (Iris 3.9.1): a biome JSON with NO `generators` field does NOT mean "no generators". `IrisBiome` defaults the field to a single `IrisBiomeGeneratorLink` whose `generator` defaults to `"default"`. There is no `generators/default.json`, so `ResourceLoader.load("default")` returns null and `getCachedGenerator` falls back to `new IrisGenerator()` whose `loadKey` is null. `IrisComplex` registers generators from EVERY region biome (incl. children) into one global set, so a SINGLE such biome poisons height interpolation for the whole world -> NPE on every generated column.
- Trigger here: new cave biomes (`carving/*`, `magnetics/*`, `prismatics/*`) were added to region `caveBiomes` without a `generators` block.
- Fix: every reachable biome MUST have a `generators` block. Cave biomes mirror `carving/rocky-cavebiome`: `"generators": [{"generator": "rare-hills", "min": 1, "max": 1}]`.
- Audit tip: walk the dimension's regions only (not all region files in the pack - unused base regions cause false positives), BFS through `land/sea/shore/caveBiomes` + `children`, and flag any biome missing/empty `generators`. Do NOT trust "all generator references resolve" - the killer is the ABSENT generators block, not a bad name.

## 2026-06-06 - Iris jigsaw `getMaxDimension` NPE = pool references a non-existent piece

- Symptom: `NullPointerException` at `IrisJigsawStructure.getMaxDimension` / `lambda.getMaxDimension.0` (`IrisJigsawPiece.getMax2dDimension()` on a null piece), via `MantleJigsawComponent.computeRadius` -> `Failed to generate X, Z`.
- Cause: a jigsaw POOL lists a piece key that does not resolve to a `jigsaw-pieces/...` file, so the piece loads as null. Here `jigsaw-pools/village/swamp_boat/streets.json` referenced `crossroad_03`/`crossroad_05` but the piece files, their `object` refs, and `.iob` objects were all consistently misspelled `corssroad_03`/`corssroad_05` (01/02/04 correct).
- Fix: repointed the pool's two entries to the existing `corssroad_03`/`corssroad_05` pieces (pack-patches/assets). Audit: build the set of `jigsaw-pieces` keys and verify every `pieces[]` entry in `jigsaw-structures` and `jigsaw-pools` resolves.
