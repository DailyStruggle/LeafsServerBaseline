# Mob Improvement Framework - Implementation Methods + Dependency Options

**Status:** Draft (design only; not approved for implementation - Rule D-005)
**Date:** 2026-06-09
**Author:** Leaf (with Junie)

This note plans the **generalized implementation methods** for improving mobs on this
server (custom stats, gear, behaviour, biome-themed mobs, and the boss layer), before
any specific boss is built. It is the foundation `BIOME-BOSS-MOBS-PLAN.md` builds on:
that note says *what* each biome's boss is; this note says *how* we build mob
improvements in a reusable way, and whether a single plugin dependency is worth it.

Guiding constraint from the issue: prefer no dependencies; consider **single
dependency** options only if they clearly pay for themselves. The hard requirement is
**Paper + Folia** parity, which is the main filter below.

---

## What "mob improvements" must cover

One framework should serve every tier so we do not write three systems:

1. **Stat/gear reskins** (Tier-1 common custom mobs): renamed, re-geared, re-statted
   vanilla mobs with biome flavour; farmable.
2. **Behaviour/abilities** (elites + bosses): spell casts, summons, hazard zones,
   phases, target logic.
3. **Lifecycle + identity:** durable marking, persistence across chunk unload/restart,
   boss bars, cleanup of summoned adds.
4. **Spawn integration:** biome-gated, rarity-controlled, anti-farm.
5. **Rewards:** loot tables + advancement grants (handled in the boss/artifact notes).

---

## Method A - In-house, data-driven framework (pure Paper API, recommended)

A small shared layer (a `leaf-mobs` core, or a package reused by `leaf-bosses`) that is
**data-driven**: mob definitions live in config/JSON, code stays generic. This is the
recommended path because it is fully Folia-native and dependency-free.

### A1. Mob definition (data, not code)

A `MobDefinition` loaded from YAML/JSON per mob:

```
id: grovewarden
base_entity: RAVAGER
display_name: "<dark_green>The Grovewarden"
tier: BOSS                 # COMMON | ELITE | BOSS
biomes: [leaf:dark_forest_core, leaf:dark_forest_body]
attributes: { MAX_HEALTH: 300, ATTACK_DAMAGE: 14, KNOCKBACK_RESISTANCE: 1.0, ... }
equipment: { helmet: ..., main_hand: ..., drop_chance: 0 }
abilities: [ { type: summon_adds, ... }, { type: hazard_zone, ... } ]
spawn: { chance: 0.02, cooldown_s: 1800, max_nearby: 1, radius: 64 }
loot_table: leafbosses:boss/grovewarden
advancement: leafbosses:boss/grovewarden
```

This makes "add a new mob/boss" a config edit + (only if a brand-new ability is needed)
one ability class. Tier-1 reskins need no code at all.

### A2. Core code components (generic, reused by every mob)

- `MobRegistry` - loads `MobDefinition`s; resolves by id and by biome.
- `MobFactory` - spawns from a definition: sets entity, attributes, gear, name, PDC
  markers (`mob_id`, `tier`, `biome`), persistence flags.
- `AbilityRegistry` + `Ability` interface - pluggable behaviours
  (`onTick`, `onDamaged`, `onTarget`, `onDeath`); ships the `SpellKit` casts from the
  boss plan (`fangLine`, `potionVolley`, `summonAdds`, `hazardZone`, `teleportBlink`).
  New mechanics = new `Ability` implementations, registered by `type` string.
- `MobController` (per active elite/boss) - owns boss bar, ability scheduler handle,
  add tracking; created on spawn and on entity-load reattach.
- `MobLifecycleListener` - `EntityAddToWorldEvent`/chunk load: re-attach a controller to
  any PDC-marked mob so bosses survive restart; `EntityDeathEvent`: loot/advancement +
  add cleanup; `EntityRemoveFromWorldEvent`: pause controller.
- `SpawnService` - the rarity/cooldown sweep (or `CreatureSpawnEvent` transform) that
  consults `MobRegistry` by biome; defers farm-geometry checks to `elite-spawn-gate`.

### A3. Persistence (the one genuinely tricky part)

- Identity persists for free via `PersistentDataContainer` (survives unload/restart).
- **Transient runtime state** (boss bar, ability cooldowns, add list) does NOT persist,
  so on entity-load the lifecycle listener must **rebuild the controller** from the PDC
  id. Plan: scan loaded entities on world load + handle `EntityAddToWorldEvent`.
- Summoned adds are PDC-tagged with their owner id so a reloaded boss can re-adopt or
  cull orphaned adds.

### A4. Folia model (non-negotiable)

- `folia-supported: true`; never the legacy `BukkitScheduler`.
- Per-mob ability ticking on `entity.getScheduler()`; area effects on
  `world.getRegionScheduler()`; the spawn sweep iterates players and schedules per
  region. Touch each entity only on its owning region thread.

### A5. Effort / risk

- Effort: medium. ~6-8 small classes + a config loader; abilities incremental.
- Risk: low-medium. The only real complexity is persistence reattach and Folia
  threading - both well-understood and already used by `elite-spawn-gate`/`leaf-artifacts`.
- Payoff: total control, zero deps, one system for all tiers, trivially Folia-safe.

---

## Method B - Single-dependency options (only if they pay off)

Evaluated against the Folia requirement, which eliminates most of them.

| Option | What it gives | Folia | License/cost | Verdict |
|--------|---------------|-------|--------------|---------|
| **MythicMobs** (free) | Full data-driven mobs, skills, spawners, drops, bosses - little/no code | **No native Folia** (Folia support is not supported / premium-unclear) | Free tier limited; premium paid | Reject while Folia is required; revisit only if we drop Folia |
| **MythicMobs (premium)** | Above + more skills | Same Folia gap | Paid | Reject (Folia + cost) |
| **AdvancedMobControl / similar** | Spawn control | Mostly Paper-only | Varies | Reject (Folia) |
| **Citizens** | Scriptable NPCs | Folia: no | Free | Wrong tool (NPCs, not hostile combat mobs) |
| **A shaded library** (not a server plugin): e.g. config (Configurate) or a small math/AI util | Code convenience only | N/A (compiled in) | Permissive | Acceptable but unnecessary; Paper bundles Adventure + YAML |

Key finding: the popular "single dependency" that would save the most work
(**MythicMobs**) does not currently support Folia, which is the whole reason we are
dependency-light. So a server-plugin dependency does **not** pay off today.

If Folia were dropped as a target, MythicMobs becomes the obvious single-dependency
choice and would replace most of Method A's config/ability code. Document this as the
explicit fork in the road.

### Shaded-library nuance

A *shaded* (compiled-in) library is not a server dependency and does not break the
"no dependencies" intent. If config ergonomics get painful we may shade Configurate or
similar, but Paper already ships SnakeYAML + Adventure, so even this is likely unneeded
for v1.

---

## Recommendation

- **Build Method A** (in-house, data-driven, Paper-API-only). It satisfies Paper+Folia
  with zero runtime dependencies and serves all mob tiers from one system.
- **Do not** take a server-plugin dependency (MythicMobs etc.) while Folia is a target;
  record it as the fallback only if Folia is later abandoned.
- Reuse existing pieces: `elite-spawn-gate` (farm-geometry predicate), `leaf-artifacts`
  (reward items), `leaf-skilltree` datapack pattern (advancements).

---

## Phased build order (framework-first, then content)

1. **F0 - Core scaffold. [DONE]** `plugins/leaf-mobs` Gradle module (Folia-safe,
   `folia-supported: true`, paper-api only): `MobDefinition` + reference-table loader
   `MobRegistry` (`mobs.yml`), generic `MobFactory` (spawn + attributes/gear/name +
   `MobKeys` PDC markers), and a `/leafmobs <list|reload|spawn>` admin command. No
   abilities yet. Attributes resolved via `Registry.ATTRIBUTE` for version safety.
2. **F1 - Tier-1 reskins. [DONE, sample]** `mobs.yml` ships a config-only flavour mob
   (`taiga_frostling`) plus a boss base (`grovewarden`) to validate the data path
   (naming/stats/gear/biomes). Needs in-game spawn verification on next server boot.
3. **F2 - Lifecycle + persistence. [DONE]** `MobController` (Adventure boss bar + per-mob
   update on the entity's Folia scheduler, viewer set by radius), `MobManager`
   (UUID -> controller map; `spawn`/`attach`/`reattach`/`detach`/`scanLoaded`/`shutdown`),
   and `MobLifecycleListener` (`EntityAddToWorldEvent` rebuilds a controller from the PDC
   id so bosses survive `/reload` + restart; `EntityRemoveFromWorldEvent`/`EntityDeathEvent`
   dispose it). Controllers are tier-gated via `MobTier.usesController()`/`usesBossBar()`.
   Identity is durable (PDC); only the transient bar/viewers are rebuilt on load. Needs
   in-game validation that a boss bar shows, tracks damage, and survives `/reload`+restart
   on the next server boot.
4. **F3 - Ability system + `SpellKit`. [DONE]** A data-driven ability layer:
   `Ability` (functional interface) + `AbilityContext` (per-cast, self/target/params) +
   `AbilitySpec` (reference-table parse with cooldown/chance/health-phase gating) +
   `AbilityRegistry` (maps a `type` string to a built-in cast) + a static `SpellKit`
   (`fang_line`, `fang_ring`, `potion_volley`, `summon_adds`, `teleport_blink`, `leap`,
   `shockwave`, `enrage`). `MobController` builds the resolved abilities per mob and casts
   them from its entity-scheduler tick (Folia-safe), gating by health phase, cooldown, and
   chance, after resolving a target (AI target or nearest player). `mobs.yml` ships a melee
   kit (`grovewarden`: leap + shockwave + sub-50% enrage) and a magic kit (`blight_warden`:
   fang_line + poison volley + Vex adds + sub-40% blink), exercising both a melee and a
   magic ability. Summoned adds are PDC-tagged with the owner id (`add_owner`) for later
   culling. Needs in-game validation of casts on the next user-triggered server boot.
5. **F4 - Spawn + anti-farm. [DONE]** Implemented as a `CreatureSpawnEvent` transform
   with random selection (the approved, cheaper, more-vanilla model). A `MobSpawnListener`
   inspects each natural spawn, gathers definitions whose per-row `spawn:` rule matches the
   base `EntityType` + spawn reason, filters by the spawn-location biome, rolls the per-mob
   `chance`, then enforces a server-wide `cooldown_s` and a `max_live` loaded-copy cap before
   cancelling the vanilla spawn and re-spawning the managed mob via `MobManager` on the
   region scheduler (Folia-safe; `MobFactory` uses `SpawnReason.CUSTOM` so re-spawns never
   re-enter the listener). `spawn:` is added to `MobDefinition` as an immutable `SpawnRule`
   (`enabled`, `replace`, `reasons`, `chance`, `cooldown_s`, `max_live`); a missing/disabled
   section means command-only spawning. `mobs.yml` ships rare rules for `grovewarden`
   (RAVAGER, chance 0.04) and `blight_warden` (WITCH, chance 0.05), each `max_live: 1` with a
   1200s cooldown. Farm-geometry rejection is still owned by `elite-spawn-gate` (a separate
   listener on the same event); needs in-game spawn-rate verification on the next boot.
6. **F5 - Rewards. [DONE]** A generic, data-driven reward step on PDC-marked death.
   `RewardService` (invoked from `MobLifecycleListener.onDeath`) reads the dead mob's
   definition, rolls its `loot_table:` (a vanilla/datapack `LootTable`, with the killer as
   looter, added to the drops - the rare, repeatable path) and grants its `advancement:`
   to the killer on that player's own (Folia-safe) scheduler. Because vanilla records an
   advancement "done" once, the datapack advancement's reward (e.g. a guaranteed first-kill
   artifact) fires only the first time, satisfying "artifact on first kill PLUS rare on the
   loot table". Two optional tuning fields were added to `MobDefinition`:
   `clear_vanilla_drops:` (wipe the base mob's drops first - bosses carry no vanilla loot)
   and `xp:` (override dropped experience). The sample `grovewarden`/`blight_warden` rows
   set both and reference `leafbosses:boss/<id>` loot tables/advancements; those datapack
   assets are supplied in F6. If a referenced table/advancement is absent the service logs a
   single warning and is otherwise a no-op (so leaf-mobs is safe to ship before the
   datapack). Artifact minting is intentionally delegated to the datapack loot table /
   advancement rewards, keeping leaf-mobs dependency-free (no `leaf-artifacts` dependency).
7. **F6 - First full bosses. [DONE]** Composed the framework into two end-to-end bosses
   by adding the `datapacks/leaf-bosses` datapack (auto-discovered by
   `deploy-datapacks.ps1`) that supplies the loot tables and advancements the F5
   `RewardService` already references by id. **Grovewarden** (giant dark forest, RAVAGER)
   carries a **Thorn Pendant**; **Blight Warden** (swamp, WITCH) carries an **Antidote
   Vessel**. Each boss yields its artifact two ways: guaranteed on the first kill via the
   boss advancement's `rewards.loot` (a `*_firstkill` table that is just the artifact), and
   rare on repeat kills via the boss `EntityType` loot table behind a `random_chance` gate,
   alongside plenty of biome-specific valuables (dark-forest: emeralds, dark oak, glow
   berries, lapis, moss, golden/enchanted apples; swamp/brewing: glowstone, redstone, slime,
   gunpowder, fermented spider eye, bottles, sugar, enchanted books, a rare totem). The
   artifacts are dependency-free: the loot table stamps the same PDC tags `leaf-artifacts`/
   `leaf-curios` read (`leafartifacts:artifact_type` + `leafcurios:slots` inside
   `minecraft:custom_data` -> `PublicBukkitValues`) plus the artifact's `item_model`,
   `unbreakable`, name and lore, so the drop is a real equippable artifact when those plugins
   are loaded and a harmless themed vanilla item otherwise. Needs in-game validation on the
   next user-triggered boot (deploy with `datapacks\deploy-datapacks.ps1 -Only leaf-bosses`,
   then `/reload` and kill a spawned boss).

Each phase is independently shippable and testable; the framework exists before any
specific boss is "finished", which is what this issue asked for.

---

## Open decisions

1. **Module shape:** a standalone `plugins/leaf-mobs` core that `leaf-bosses` depends on,
   vs folding the framework into a single `leaf-bosses` plugin. (Standalone is cleaner if
   Tier-1 reskins ship without bosses.)
2. **Config format:** YAML (matches existing plugin `config.yml` style) vs JSON (matches
   datapack style). Lean YAML for hand-editing.
3. **Spawn mechanism:** DECIDED (F4) - `CreatureSpawnEvent` transform with random
   selection (cheaper, more vanilla). A scheduled sweep can be added later per-biome if
   guaranteed placement is ever needed.
4. **First vertical boss** after the framework: melee (Grovewarden) or magic (Blight
   Warden) first.
