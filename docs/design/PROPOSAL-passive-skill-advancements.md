# Proposal (Rule D-005): Advancement-Driven Passive Skill System

**Status:** Approved (defaults set 2026-06-07)
**Date:** 2026-06-07
**Author:** Leaf (drafted with Junie)

---

## 1. Goal

A classless, exploration-and-choice-driven passive progression system that:

- Uses the **vanilla advancement screen** as the only player-facing surface (no extra GUIs).
- Models nodes after the **Passive Skill Tree** mod (Path of Exile style): connected nodes, start points, multi-branch requirements, emergent "classes" via reachable paths - never explicitly declared.
- Spends **player XP levels** per point, with a **rising cost curve** to discourage min-maxing.
- Supports **reset** that revokes selections (and resets the cost counter) but never refunds XP.
- Backs effects with a **thin plugin** for anything beyond vanilla `/attribute`.

## 2. Model borrowed from Passive Skill Tree (PST)

PST (PoE-inspired) characteristics we mirror:

- **Nodes** apply attribute modifiers; modifier operations like flat-add / multiply-base / multiply-total.
- **Connections**: a node is only allocatable if a connected node is already owned.
- **Start points / limited class picks**: early forks gate which deep branches are reachable.
- **Respec** refunds allocated nodes.
- **Modded attributes supported**: in our case, plugin-defined buffs extend beyond vanilla attributes.

We drop PST's custom tree-rendering GUI and re-express the graph as **advancement tabs + parent chains**.

## 3. Affected components

- **New datapack**: `datapacks/leaf-skilltree/` (advancements + functions + predicates + scoreboards).
- **New/extended plugin**: `plugins/leaf-skills/` (or fold into `leaf-artifacts`) - applies/clears buffs on advancement grant/revoke, persists via PDC.
- **Docs**: this proposal -> ADR if approved.

## 4. Before/after structure

Before: progression is grinding-based (mob kills, farms). No deliberate build identity.

After: players earn XP normally, then **spend levels to allocate passive nodes** they browse in the advancement screen; reachable node combinations form emergent archetypes.

## 5. Mechanics

### 5.1 Tree as advancements
- One root advancement per tab = a "start cluster".
- Each node = an advancement with a `minecraft:impossible` criterion (grantable only by command/function).
- `parent` chains + display icons/frames render the connection graph in the `L` screen.

### 5.2 Selection action (no GUI)
- A single enabled scoreboard `trigger` objective (e.g. `skill_pick`).
- Player runs `/trigger skill_pick set <nodeId>`; a repeating function reads it and routes to that node's grant function.

### 5.3 Allocation rules (per node grant function)
1. **Connection check**: require at least one connected node via `@s[advancements={leaf-skilltree:<connected>=true}]`.
2. **Multi-base requirement**: deep nodes may require two+ distinct branch nodes simultaneously -> emergent hybrid classes, no class label.
3. **Cost check**: read `points_spent` scoreboard; `cost = base + step * points_spent`; verify `xp_level >= cost`.
4. On success: `/xp add @s -<cost> levels`, `scoreboard players add @s points_spent 1`, `/advancement grant @s only <nodeId>`.
5. Grant fires the node's `function` reward -> applies attribute modifier (vanilla) and/or tags the plugin to apply a custom buff.

### 5.4 Reset
- Operator/rare-item-gated function: `/advancement revoke @s` (tree tab), clear attribute modifiers, reset `points_spent`, signal plugin to clear buffs. **No XP refund.**

### 5.5 Plugin role
- Listens to advancement grant/revoke; maps nodeId -> buff; applies via attribute modifiers or custom logic (procs/cooldowns); persists owned set in PDC for re-apply on join.

## 6. Risks / trade-offs

- Advancement screen layout is auto-arranged; complex graphs may render awkwardly (mitigate with multiple tabs / curated parent chains).
- `impossible`-node trees can grow large; keep node functions generated/templated to avoid hand-edit drift.
- Folia threading: plugin buff application must be region/entity-scheduler safe.
- No native "click to allocate"; the `/trigger` step is the one unavoidable command.

## 7. Resolved decisions

1. **Plugin**: new standalone `leaf-skills` plugin (not folded into `leaf-artifacts`).
2. **Cost curve**: linear `cost = base + step * points_spent`. Rationale: parallels D&D 5e multiclass (flat rising XP demand) vs 2e tables.
3. **Start clusters**: 1 at launch.
4. **Gating**: biome-rarity feeds XP gain (later), AND a few nodes/branches are **hard-gated behind specific achievements** - beating a boss mob, discovering a lost structure, or finding a specific artifact. These gates are extra `@s[advancements=...]` / tag checks layered on top of the normal connection+cost rules.

## 8. PoC scope (BUILT)

One tab, 6 nodes: 2 start nodes (`vigor`, `swift`), 2 single-connection mids that are mutually exclusive (`bulwark` / `reaver`), 1 more mid (`skirmish`), and 1 apex `warden` that needs BOTH `bulwark` and `skirmish` plus the Ender-Dragon-kill hard gate. Full `base+step*n` cost curve + reset; plugin applies a vanilla attribute buff per node and one infinite potion effect (Resistance) for `warden`.

Delivered:
- Datapack `datapacks/leaf-skilltree/` (advancements, `load`/`tick` functions, function tags).
- Plugin `plugins/leaf-skills/` (`LeafSkillsPlugin`, `SkillController`, `SkillConfig`, `SkillNode`, `SkillCommand`, `config.yml`, `plugin.yml`, README).

Not yet done (needs a user-triggered server boot): in-game verification of selection, cost, gating, and buff reconcile.

## 9. Notable deviation from section 5

Section 5.3 imagined cost/prereq checks inside datapack functions. In practice a vanilla datapack **cannot read a player's current XP level** (`/data` does not target players, and no scoreboard criterion tracks the live level). Therefore the plugin is the **single authority** for validation, XP-level charging, and granting; the datapack only renders the tree and exposes the `skill_pick` trigger.
