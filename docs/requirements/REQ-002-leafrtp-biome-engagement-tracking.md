# REQ-002: Biome Engagement Tracking Plugin

**Status:** Draft  
**Date:** 2026-06-04  
**Author:** Leaf

---

## Goal

Build a new server-side plugin that sits atop LeafRTP, listening to its events alongside standard Bukkit/Paper events, to collect per-biome engagement signals. Biomes that are underperforming - not being explored, built in, or returned to - can then be identified and improved.

## Background

The design goal is build variety and player identity distributed across biome types, not centralization in one or two familiar places. A single biome dominating engagement metrics is a signal that other biomes need work. Raw per-biome data is collected by the plugin; clustering and interpretation are done by a human as a postprocessing step.

This is a standalone plugin, not a fork or modification of LeafRTP. It depends on LeafRTP being present and registers listeners against LeafRTP's API events (e.g. the RTP teleport event) as well as standard server events. LeafRTP itself is not changed.

## Signals to Collect

For each player session in a biome, record:

| Signal | What it measures |
|--------|-----------------|
| Time spent in biome | Exploration - did the player linger? |
| Block placements and breaks | Building - did the player invest in the space? |
| Re-RTP triggered from biome | Rejection - did the player immediately leave? |
| Chat or proximity interaction with other players | Social engagement |
| Player death in biome | Risk engagement - the player was doing something active |
| Bed or respawn point set in biome | Ownership - the strongest "I live here" signal |
| Return visits to same biome | Stickiness - the player came back |

A visit with no block interaction, no player interaction, and no return is a weak or negative signal regardless of time spent.

## Requirements

- The plugin shall record, per biome per player session: time spent, block interaction count (placements + breaks), re-RTP flag, player-proximity interaction flag, death flag, respawn-set flag, and return-visit count.
- Data shall be stored per biome with enough granularity to produce a per-biome summary report.
- The plugin shall expose a report command listing all biomes with their aggregate signal counts, readable at a glance in a single screen of console or chat output.
- The plugin shall not require players to install any client-side mods.
- Biome grouping and clustering are explicitly out of scope for the plugin; raw per-biome data is sufficient.

## Acceptance Criteria

- [ ] All seven signals are recorded per biome session with no noticeable server performance impact.
- [ ] A command produces a per-biome summary table with all signal totals.
- [ ] The report fits in a single screen of console or chat output.
- [ ] Data persists across server restarts.
- [ ] A biome with zero block interaction and zero return visits is distinguishable in the report from one with high block interaction and multiple return visits.

## Out of Scope

- Biome clustering or grouping (human postprocessing step).
- Tracking reasons for re-RTP (no player-facing prompt or survey).
- Trend analysis or time-series reporting.
- Any client-side mod or resource pack changes.

## Related

- Design doc: [`../design/CUSTOM-BIOMES.md`](../design/CUSTOM-BIOMES.md)
- ADR: [`../design/adr/ADR-003-custom-biome-design-goals.md`](../design/adr/ADR-003-custom-biome-design-goals.md)
