# LeafShapeMining

Vanilla/Folia-safe "select a shape, clear stone fast" mining tool (Excavate / tunnel-bore style). Matches the ergonomic of the shape-selection mods some players depend on, without importing veinminer's ore-pacing problem.

See the full design note: `docs/scratch/SHAPE-MINING-DESIGN.md`.

## Status

v1 implemented (Paper 1.21.x, Folia-supported). The stop-at-first-boundary execution model from the design note is shipped; the v2 region-grouped-tasks model remains deferred.

Implemented:

- Shape-mining tool item (marked netherite pickaxe, mode stored in `custom_data` / PDC).
- Sneak + scroll mode switching with the non-trapping pass-through (`ModeSwitchListener`).
- `BlockBreakEvent`-driven shape execution: generate -> filter (filler-only) -> per-block region-ownership gate, stopping at the first non-owned / unloaded block (`BlockBreakListener`).
- Direction-aware tunnel-bore length caps and a per-swing block cap (`ShapeMiningConfig` / `config.yml`).
- Per-block durability cost on the tool.
- `/shapemine give [player]` admin command for in-game review.

Priority: this is the more critical of the in-flight mining ideas. The related magic "square-cone land-clear" spell is explicitly deferred to post-first-release and depends on this pipeline.

Not yet implemented (deferred): v2 region-grouped tasks, XP skill-tree gating of shape sizes, crafting/loot acquisition, and resource-pack custom models (the tool uses a placeholder netherite pickaxe).

## Intent and scope

- Speed up traversal through bulk filler (stone, deepslate, dirt, etc.), which is pure tedium.
- Do NOT auto-harvest ores. Keeping ores out of the shape preserves the "dopamine window" on rare finds. This is the explicit line that separates shape-mining (good) from veinminer (dropped).
- Delivery: a single Folia-supported Paper plugin, consistent with the artifact/backpack item-marker approach (custom item or a toggle on existing picks via `custom_data`).

## Shapes

- `1x1` (off / precise mode).
- `1x2` (player-height corridor).
- `3x3` (room/corridor clear), fixed cross-section.
- `1x1xN` tunnel-bore along the look direction.

## Mode switching (how a player changes shape)

Primary toggle: sneak + scroll (closest vanilla analogue to a mod "mode wheel").

- While holding the shape-mining tool, hold sneak (shift) and scroll the hotbar.
- Detect via `PlayerItemHeldEvent` while `player.isSneaking()`; compare `getPreviousSlot()` / `getNewSlot()` to read scroll direction.
- Cycle order: `1x1` -> `1x2` -> `3x3` -> `1x1xN` tunnel -> (full rotation) -> back to `1x1`.
- Persist the current mode in the tool's `custom_data` (per-tool, survives relog / death / grave recovery / transfer), not in a transient session map.
- Echo the new mode on the action bar (`sendActionBar`) plus an optional click sound; no chat spam.

Non-trapping pass-through (so sneak-scroll can still change tools, e.g. in the deep dark where you sneak constantly):

- While there is still a next shape in the scroll direction: cancel the event, advance the shape, show it on the action bar.
- When already on the last shape in that direction: reset the tool's mode to `1x1`, do NOT cancel the event (the hotbar slot change proceeds to the next tool), and show a brief "exiting shape mode" hint.
- Reverse-scroll mirrors this: scrolling back past `1x1` also resets to `1x1` and passes through to the previous slot.
- Resetting to `1x1` on exit means a tool never sits silently in `3x3`/tunnel mode; re-selecting it later starts in safe precise mode.

Fallbacks (config-selectable / later): shift-right-click cycle; chest-GUI mode picker (deferred until skill-tree gating makes a visual "what's unlocked" screen worthwhile).

## Pipeline (shared by all versions)

Keep steps 1-3 pure (coordinates + a snapshot read, no world mutation) so they are testable and reusable. Only step 4 changes between versions.

1. Resolve direction and shape from the player's facing + tool mode.
2. Generate the ordered block list (candidate cells of the shape, in mining order, nearest-first).
3. Filter by the allow-list (bulk filler only; ores/chests/spawners excluded) and by what is actually present.
4. Partition / execute by region ownership.

### Allow-list (bulk filler only)

Include: stone, deepslate, dirt, granite, andesite, diorite, tuff, netherrack, and similar filler.

Exclude: all ores (and their deepslate variants), chests, barrels, spawners, and any container/block-entity. Exact list lives in config.

## Direction-aware length caps (replaces a flat 32)

Digging straight down is the dangerous case (lava pockets, void drops, blind cave falls), so cap the tunnel-bore axis by the vertical component of the dig direction:

- Horizontal (pitch ~0): full length, e.g. up to 32.
- Diagonal: interpolate, e.g. 8-16.
- Straight down (steep negative pitch): clamp hard, e.g. 1-3 blocks per swing.
- Straight up: moderate clamp (falling-block / ceiling-collapse risk), e.g. 4-8.

Suggested formula: `maxLen = lerp(maxVertical, maxHorizontal, 1 - abs(dir.y))`, then round and clamp. All bounds are config tunables. This caps only the tunnel-bore axis; the `3x3` cross-section is fixed regardless of direction.

## v1 (ship first): stop at first non-owned / unloaded block

Simple, single-task, single-thread, no scheduling hops. Fully Folia-safe.

```
ordered = filtered block list (mining order, nearest-first)
toBreak = []
for block in ordered:
    if not isOwnedByCurrentRegion(block.location):  break   // stop the line here
    if not block.chunkLoaded:                       break   // treat unloaded as boundary
    toBreak.add(block)
breakAll(toBreak)   // all on the current region thread, in this tick
```

- The player keeps mining; the next swing starts from the new position, which is then on the correct thread.
- Apply durability and (optional) hunger cost per block actually broken, on the player's entity scheduler.
- Visible artifact: an occasional one-block "hitch" at a region edge. Usually invisible.

Note on the 32 cap: it is a balance/perf limit, NOT a region-safety guarantee. Folia regions are dynamic groups of nearby chunks that merge/split at runtime and are not aligned to any fixed block grid, so a 32-block line can still cross a region boundary. The per-block ownership check is what makes it safe.

## v2 (revisit later): group by region into a few executable tasks

Goal: a model that feels more consistent to the player than the v1 hitch. Swap ONLY the partition step (step 4); steps 1-3 are unchanged. A 32-long tunnel touches at most a handful of regions, so ~1-3 tasks dispatched via each owning region's scheduler. Open questions before building: cross-region ordering/atomicity, partial-failure rollback, and unloaded-chunk policy (defer vs skip).

## Balance knobs

- Filler-only allow-list (most important rule).
- Per-block durability (and optional hunger) cost: convenience, not free infinite digging.
- Optionally gate shape size behind the XP skill-tree (`1x2` early, `3x3` mid, tunnel-bore late) so the dependence becomes a progression unlock rather than a day-one given.

## Folia checklist (for implementation)

- `folia-supported: true` in `paper-plugin.yml`.
- Never use `Bukkit.getScheduler()`. Use entity (`player.getScheduler()`), region (`Bukkit.getRegionScheduler()`), or global region schedulers.
- Touch each block only from its owning region thread; gate every break on an ownership check.
- Cap blocks broken per swing so a single action cannot lag-spike a region.

## Relationship to the magic system

Magic block-breaking (deferred, post-first-release) reuses this exact pipeline but with a different shape generator - an expanding "square cone" for clearing large vertical slices of filler. It stays filler-only and is costed against a shared mana pool. See the magic design discussion; shape-mining ships first because magic depends on this pipeline.

## Testing

- `/shapemine give [player]` (permission `leafshapemining.admin`, default op) hands out a shape-mining tool.
- Hold the tool, sneak, and scroll to cycle `1x1` -> `1x2` -> `3x3` -> `1x1xN` tunnel; one more scroll past the end resets to `1x1` and changes tool slot (non-trapping).
- Mine bulk filler (stone/deepslate/etc.) to clear the selected shape; ores and containers are never broken by the shape.

## Build

Mirrors the other plugins in this repo (Gradle or Maven, Java 21, Paper 1.21.4 API):

- Gradle: `gradle jar` -> `build/libs/LeafShapeMining-0.1.0.jar`
- Maven: `mvn package` -> `target/LeafShapeMining-0.1.0.jar`
