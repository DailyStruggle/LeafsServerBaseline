# Shape-Mining (Excavate / Tunnel-Bore style) - Design Note

Working note. Goal: give players a vanilla/Folia-safe "select a shape, clear stone fast" mining tool, matching the ergonomic of mods some players are dependent on, without importing veinminer's ore-pacing problem.

This is a design scratch note, not a spec. Implementation is gated behind a Rule D-005 proposal (plugin/file layout) before any code.

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
- Reverse-scroll mirrors this: scrolling back past `1x1` also resets to `1x1` and passes through to the previous slot. Both directions can always leave the tool with one extra scroll.
- Resetting to `1x1` on exit means a tool never sits silently in `3x3`/tunnel mode; re-selecting it later starts in safe precise mode (right default for the deep dark).

Fallbacks (config-selectable / later):

- Shift-right-click cycle: `PlayerInteractEvent`, right-click + sneak + holding the tool; cancel the event and advance one shape per press. Simple, but right-click is overloaded so only consume it when the held item is the tool. Offer as an alternative for players who bind scroll elsewhere.
- Chest-GUI mode picker (later): a small inventory with one icon per shape; lets players jump directly to a shape and is a natural place to surface XP skill-tree gating. Heavier and interrupts mining flow; defer until the skill-tree gate makes a visual "what's unlocked" screen worthwhile.

Folia/safety notes for all triggers:

- These events arrive on the player's region thread already, so reading/writing the held item and `custom_data` needs no extra scheduling.
- Always cancel the underlying vanilla action only when the gesture is consumed as a mode switch AND the held item is the shape-mining tool; otherwise normal hotbar/right-click behavior passes through untouched.

Default to ship: sneak + scroll as primary, shift-right-click cycle as a config alternative; GUI picker deferred. Player can always drop to `1x1` for precise work.

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

## v1 (ship now): stop at first non-owned / unloaded block

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

Note on the 32 cap: it is a balance/perf limit (bounded work per swing), NOT a region-safety guarantee. Folia regions are dynamic groups of nearby chunks that merge/split at runtime and are not aligned to any fixed block grid, so a 32-block line can still cross a region boundary. The per-block ownership check is what makes it safe; the cap just reduces how often a boundary is hit.

## v2 (revisit later): group by region into a few executable tasks

Goal: a model that feels more consistent to the player than the v1 hitch. Swap ONLY the partition step (step 4); steps 1-3 are unchanged.

```
groups = {}                      // regionKey -> list<block>
for block in filtered:
    if not block.chunkLoaded:  defer or skip   // policy TBD
    key = regionKeyOf(block.location)          // identity of owning region
    groups[key].add(block)

for (key, blocks) in groups:
    scheduler.forRegionOf(blocks[0]).run(() => breakAll(blocks))
```

- "Small number of tasks" falls out naturally: a 32-long tunnel touches at most a handful of regions, so ~1-3 tasks, not one-per-block.
- Each task only touches its own region's blocks -> Folia-safe.

Open consistency questions to settle before building v2:

- Ordering / atomicity: drops from different regions land at slightly different ticks.
- Rollback / partial failure: what to do if a region's task cannot run because its chunk unloaded between planning and execution.
- Unloaded-chunk policy: defer (force-load + schedule) vs skip.

These are exactly the costs v1 sidesteps by stopping at the boundary.

## Balance knobs

- Filler-only allow-list (most important rule).
- Per-block durability (and optional hunger) cost: convenience, not free infinite digging.
- Optionally gate shape size behind the XP skill-tree (`1x2` early, `3x3` mid, tunnel-bore late) so the dependence becomes a progression unlock rather than a day-one given.

## Folia checklist (for implementation)

- `folia-supported: true` in `paper-plugin.yml`.
- Never use `Bukkit.getScheduler()`. Use entity (`player.getScheduler()`), region (`Bukkit.getRegionScheduler()`), or global region schedulers.
- Touch each block only from its owning region thread; gate every break on an ownership check.
- Cap blocks broken per swing so a single action cannot lag-spike a region.

## Staging summary

- v1 ships fast and is provably safe; localized to the partition step.
- Because steps 1-3 produce a plain block list, moving to v2 is a localized change to step 4 - no churn in shape generation, allow-listing, or direction caps.
