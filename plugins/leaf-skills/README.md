# LeafSkills

Advancement-driven passive skill selection (PoC). Companion to the
`datapacks/leaf-skilltree` datapack. See
`docs/design/PROPOSAL-passive-skill-advancements.md` for the full design and the
Passive Skill Tree (Path of Exile) inspiration.

## How it works

- The **datapack** renders the tree in the vanilla advancement screen (one tab,
  `leafskills:root`) and exposes a single selection trigger objective,
  `skill_pick`. There are no custom GUIs.
- A player chooses a node with one command:
  `/trigger skill_pick set <id>` (ids are shown in each node's description).
- The **plugin** polls that trigger on each player's own scheduler (Folia-safe),
  then validates the request:
  - `requires` - all listed nodes must already be owned (classless tiering).
  - `excludes` - none of the listed nodes may be owned (mutual exclusivity).
  - `gate` - optional achievement that must be completed first (hard gate, e.g.
    `minecraft:end/kill_dragon` for the apex `warden` node).
  - cost - `base + step * pointsSpent` **experience levels**, which are spent and
    never refunded.
- On success it awards the `leafskills:node/<key>` advancement and applies the
  node's buff (a vanilla attribute modifier and/or an infinite potion effect).
  Buffs are reconciled every poll, so they survive relogs.

## Nodes (default config)

| id | node     | requires            | excludes | gate                      | buff                |
|----|----------|---------------------|----------|---------------------------|---------------------|
| 1  | vigor    | -                   | -        | -                         | +4 max health       |
| 2  | swift    | -                   | -        | -                         | +0.02 move speed    |
| 3  | bulwark  | vigor               | reaver   | -                         | +8 armor            |
| 4  | reaver   | vigor               | bulwark  | -                         | +2 attack damage    |
| 5  | skirmish | swift               | -        | -                         | +0.8 attack speed   |
| 6  | warden   | bulwark, skirmish   | -        | minecraft:end/kill_dragon | Resistance I        |

`warden` demonstrates a hybrid "class" that emerges from a multi-base
requirement plus an achievement hard-gate, without any class ever being named.

## Admin

- `/leafskills reset [player]` - revokes selections and managed buffs and resets
  the spent-point counter. **Does not refund experience.** (`leafskills.admin`,
  op by default.)

## Cost curve

Configured in `config.yml` (`cost.base`, `cost.step`). Linear `base + step * n`
so each additional point demands more levels - a deliberate brake on min-maxing,
echoing D&D 5e multiclass costs.

## Build

Standalone module (Gradle or Maven), Paper API 1.21.4, Java 21. Produces
`LeafSkills-0.1.0.jar`. Requires the `leaf-skilltree` datapack to be installed in
the world for the advancements/objectives to exist.
