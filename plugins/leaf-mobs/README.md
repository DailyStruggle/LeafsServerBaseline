# LeafMobs

Data-driven, Folia-safe framework for custom and biome-specific mobs. A mob is a row in
a reference table (`mobs.yml`), not new code, so stats/gear/biomes are easy to tune
(including with AI assistance). This is the foundation the biome-boss content layers on
top of - see `docs/scratch/MOB-IMPROVEMENT-FRAMEWORK.md` and
`docs/scratch/BIOME-BOSS-MOBS-PLAN.md`.

Dependency-free: builds against `paper-api` only (`compileOnly`) and declares
`folia-supported: true`, so it runs on both Paper and Folia.

## Current scope (F0 + F1 + F2 + F3 + F4 + F5)

- `mobs.yml` reference table loader (`MobRegistry`) with live `/leafmobs reload`.
- Generic `MobFactory` that spawns any definition and stamps the managed-mob PDC markers
  (`mob_id`, `tier`, `biome`) used by later phases.
- Immutable `MobDefinition` parsing: entity, tier, MiniMessage name, biomes, attributes
  (resolved via `Registry.ATTRIBUTE`), equipment + drop chance, persistence, glow, and
  loot/advancement ids (captured for the rewards phase).
- **Lifecycle + persistence (F2):** `MobManager` tracks a `MobController` per ELITE/BOSS,
  which shows an Adventure boss bar to nearby players and updates it on the entity's own
  (Folia-safe) scheduler. `MobLifecycleListener` rebuilds the controller from the durable
  PDC identity on `EntityAddToWorldEvent`, so a boss survives `/reload` and a full restart;
  controllers are disposed on removal/death so no boss bar lingers.
- **Abilities + `SpellKit` (F3):** a data-driven ability layer. Each ELITE/BOSS row may
  carry an `abilities:` list; every entry has a `type` (resolved via `AbilityRegistry`),
  cooldown, chance, and an optional health-phase window. `MobController` casts the gated
  abilities from the entity's own (Folia-safe) scheduler after resolving a target (AI
  target or nearest player). Built-in casts (pure Paper API) live in `SpellKit`:
  - Magic: `fang_line`, `fang_ring`, `potion_volley`, `summon_adds`, `teleport_blink`.
  - Melee: `leap`, `shockwave`, `enrage`.
  Summoned adds are PDC-tagged with their owner's id (`add_owner`). A new mechanic is one
  `AbilityRegistry.register(type, ...)` call plus a `SpellKit` method - no controller or
  data-model change.
- **Spawn service (F4):** `MobSpawnListener` turns a configurable fraction of natural
  vanilla spawns into managed mobs. Each row's optional `spawn:` section (parsed into an
  immutable `MobDefinition.SpawnRule`) declares which base `EntityType`s and spawn
  `reasons` to consider, a per-mob `chance`, a server-wide `cooldown_s`, and a `max_live`
  loaded-copy cap. On a matching `CreatureSpawnEvent` the listener filters by the
  spawn-location biome, rolls the chance, checks cooldown + live count, then cancels the
  vanilla spawn and re-spawns the managed mob via `MobManager` on the region scheduler
  (Folia-safe). `MobFactory` spawns with `SpawnReason.CUSTOM`, so re-spawns never re-enter
  the listener; farm-geometry rejection stays with `elite-spawn-gate`. Omit `spawn:` (or
  set `enabled: false`) to keep a mob command-only.
- **Rewards (F5):** `RewardService` (invoked from `MobLifecycleListener` on a PDC-marked
  death) rolls the row's `loot_table` (a vanilla/datapack `LootTable`, with the killer as
  looter) and adds the result to the drops - the rare, repeatable path. It also grants the
  row's `advancement` to the killer on the killer's own (Folia-safe) scheduler; vanilla
  records "done" once, so the datapack advancement's reward (e.g. a guaranteed first-kill
  artifact) fires only the first time. Optional `clear_vanilla_drops: true` wipes the base
  mob's drops first and `xp:` overrides dropped experience. The datapack loot table /
  advancement (e.g. `leaf-bosses`) owns the actual item content.
- `/leafmobs <list|reload|spawn|active> [id]` admin command (perm `leafmobs.admin`,
  default op) for verification (`active` shows live controller count).

Not yet implemented (later phases): F6 - composing the above into one end-to-end boss
(plus the `leaf-bosses` datapack that supplies the loot tables and advancements referenced
by the sample bosses).

## Build & deploy

Gradle only (no Maven). From the repo root:

```
plugins\deploy-plugins.ps1 -Only leaf-mobs
```

This builds the module with `gradle jar` and copies `build/libs/LeafMobs-0.1.0.jar` into
the server's `plugins` folder. Use `-NoBuild` to copy an already-built jar. A fresh jar
loads only on the next (user-triggered) server start.

## Quick verification

1. Start the server (user-triggered), then in-game:
   - `/leafmobs list` - shows loaded ids (e.g. `taiga_frostling`, `grovewarden`,
     `blight_warden`).
   - `/leafmobs spawn grovewarden` - spawns the buffed, named, glowing boss base and a red
     boss bar appears for nearby players; damaging it moves the bar. As you fight, the
     melee kit fires: it leaps at you, slams with a shockwave, and enrages below half HP.
   - `/leafmobs spawn blight_warden` - exercises the magic kit: evoker-fang lines, poison
     volleys, summoned Vex adds, and a blink when low on health.
   - `/leafmobs active` - confirms a controller is tracked.
   - Walk far away and back (chunk unload/reload), or run `/reload`, and restart the
     server: the boss bar should reappear (controller rebuilt from PDC identity).
   - Edit `mobs.yml`, then `/leafmobs reload` to re-read without a restart.
2. Natural-spawn (F4): go to a dark forest (for `grovewarden`) or swamp (for
   `blight_warden`) and let mobs spawn; a rare ravager/witch is replaced by the named,
   boss-barred mob (subject to the per-mob chance, 1200s cooldown, and `max_live: 1`).
   Tune `spawn.chance` up temporarily in `mobs.yml` + `/leafmobs reload` to verify faster.
3. Rewards (F5): kill a spawned boss. With `clear_vanilla_drops: true` the base mob's loot
   is gone; if the referenced `loot_table`/`advancement` exist (via the `leaf-bosses`
   datapack) the loot table is rolled and the advancement toast shows on the first kill.
   Until that datapack ships, leaf-mobs logs a one-line warning that the table/advancement
   was not found - that is expected and harmless.
