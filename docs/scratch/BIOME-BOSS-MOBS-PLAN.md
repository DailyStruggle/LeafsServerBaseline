# Biome-Specific Boss Mobs, Advancements, and Artifacts - Design Note

**Status:** Draft (design only; not approved for implementation - Rule D-005)
**Date:** 2026-06-09
**Author:** Leaf (with Junie)

This note designs a server-side "biome boss" layer for the scaled-up vanilla biome
variants now shipped as vanilla datapacks (post-ADR-005, Iris retired). Each signature
biome variant gets one rare boss; killing it grants a biome-themed artifact two ways:
as a guaranteed advancement reward (first kill) and as a rare drop on the boss loot
table (repeat kills). No new server dependencies: Paper API + vanilla datapacks only,
Folia-safe.

This supersedes the Iris-era spawn approach captured in `CUSTOM-MOB-SPAWNS-PLAN.md`
(entities/spawners/loot under `iris/pack-overlay/`), which no longer applies now that
worldgen is vanilla datapacks. The two-tier philosophy and anti-farm intent from that
note are carried forward; the spawn/loot mechanism is re-homed onto Paper + datapacks.

### Canonical boss roster (doc consolidation)

Boss information was previously split across three notes. This document is now the
**single source of truth for the per-biome boss roster and boss -> artifact mapping**.
The other notes keep their own concerns and point here:

- `CUSTOM-MOB-SPAWNS-PLAN.md` - owns the two-tier model, biome-rarity gradient, the
  Tier-1 (farmable) common-mob lists, and cave-structure design. Its "Biome Targets"
  table fed the Tier-2 boss rows now consolidated below.
- `ARTIFACTS-VANILLA-RECREATION.md` - owns the artifact catalogue, effects, and
  structure-vs-boss sourcing split. Its "Boss -> artifact mapping" is merged below.
- `docs/design/IRIS-END-PLAYER-PREFERENCES.md` - owns the separate End void-boss /
  arena concept (a different dimension and mechanic; not part of this overworld roster).

Use the per-biome roster in this file when asking "what boss / artifact / magic does
biome X have?".

**Foundation:** the *generalized* implementation methods (the reusable, data-driven mob
framework) and the single-dependency evaluation live in
`docs/scratch/MOB-IMPROVEMENT-FRAMEWORK.md`. That note is built first; this note is the
content layered on top of it. Recommendation there: build an in-house Paper-API
framework (zero deps) because the obvious single dependency (MythicMobs) has no Folia
support.

---

## Goals

1. Make each signature biome variant feel like its own place via a unique, rare boss.
2. Reward a hard-fought, non-farmable encounter with a meaningful, biome-themed artifact.
3. Keep it vanilla-client-friendly: server-side only, no required client mods, no new
   plugin dependencies.
4. Run identically on Paper and Folia (region-thread-safe scheduling).
5. Reuse existing systems: `leaf-artifacts` (the artifact catalogue + item builder),
   `elite-spawn-gate` (the anti-farm spawn gate), and the `leaf-skilltree` datapack
   pattern for advancements.

---

## Scope and per-biome roster (consolidated)

The "scaled-up vanilla biome variants" are the rarer size/form siblings of vanilla
biomes. Bosses are attached ONLY to the rare/very-rare variants and a few signature
custom biomes, never to common co-listed siblings or plain vanilla (preserve the
rarity gradient).

The table below merges the previously scattered roster: the new size-variant bosses
plus every Tier-2 elite/warden from `CUSTOM-MOB-SPAWNS-PLAN.md`, mapped to its
artifact(s) from `ARTIFACTS-VANILLA-RECREATION.md`. **Archetype** is the combat style
(melee / ranged / magic - see the magic section). **Status:** `done-port` = authored
under Iris, port to the new form; `planned` = agreed candidate; `new` = added here.
`Tier` = implementation priority for this plan.

### Surface - scaled-up vanilla variants (this plan's primary scope)

| Biome variant          | Boss (working name)  | Base entity (reskin)   | Archetype | Artifact(s)                       | Status | Tier |
|------------------------|----------------------|------------------------|-----------|-----------------------------------|--------|------|
| `dark_forest__giant`   | The Grovewarden      | `RAVAGER`              | melee     | `verdant_crown` (new)             | new    | 1    |
| `dark_forest__blight`  | The Blight Warden    | `WITCH` (amped)        | magic     | Antidote Vessel, Scarf of Invis.  | planned| 1    |
| `taiga__giant`         | The Frost Elder      | `IRON_GOLEM` (hostile) | melee     | `frostward_charm` (new)           | new    | 2    |
| `jungle__giant`        | The Canopy Tyrant    | `RAVAGER`              | melee     | Feral Claws                       | new    | 2    |
| `jungle__ember`        | The Embervine Warden | `WITHER_SKELETON`      | ranged    | Obsidian Skull                    | done-port | - |
| `cherry_grove__spiral` | The Blossom Sovereign| `EVOKER` (amped)       | magic     | Lucky Scarf                       | new    | 3    |
| `badlands__ashwood`    | The Ashwood Revenant | `WITHER_SKELETON`      | ranged    | Vampiric Glove                    | done-port | - |
| `meadow__giant`        | The Old Shepherd     | `IRON_GOLEM` (hostile) | melee     | Crystal Heart                     | new    | 3    |
| `swamp__giant`         | The Mire Colossus    | `RAVAGER`              | melee     | Steadfast Spikes                  | new    | 3    |
| `swamp/creaks`         | The Creak            | `EVOKER` (amped, ambush)| magic    | Thorn Pendant, Feral Claws        | planned| 3    |

### Custom structures / carving (structure + cave bosses, from CUSTOM-MOB-SPAWNS-PLAN)

Not "scaled-up variants" but part of the same boss layer; listed here so the roster is
complete and searchable in one place. Spawn/structure work tracked in that note.

The former `terralost/*` finds are **retired** (Iris dropped for vanilla-datapack worldgen,
ADR-005): each is now a rare custom jigsaw **structure** injected into a fitting vanilla biome
(structure-over-biome rule, wired like `leaf:dark_forest_giant`), not a custom biome. See the
"Terralost retirement" note below.

| Source (structure @ vanilla biome) | Boss (working name) | Base entity (reskin)        | Archetype | Artifact(s)                                | Status |
|------------------------------------|---------------------|-----------------------------|-----------|--------------------------------------------|--------|
| `leaf:caldera_throne` @ Nether `basalt_deltas` | The Caldera Tyrant | `BLAZE` (amped) + `MAGMA_CUBE` court adds | magic | Cloud in a Bottle, Flame Pendant           | planned|
| `carving/volcanic`                 | Emberdeep elite     | `WITHER_SKELETON`           | ranged    | Fire Gauntlet                              | planned|
| `leaf:frozen_prison` @ snowy biomes | Glacier Warden     | undead `WITHER_SKELETON` + skeleton/stray adds | magic | Steadfast Spikes, Cross Necklace        | planned|
| `leaf:sunken_tomb` @ `desert`/`badlands` | Sand Revenant  | `HUSK`/`STRAY` (amped)      | ranged    | Crystal Heart, Golden Hook                 | planned|
| `carving/deep`/`deepravine`        | Deep elite          | `SILVERFISH` swarm + caster | magic     | Night Vision Goggles, Digging Claws, Univ. Attractor | planned|
| `carving/lush`                     | Lush cave elite     | `EVOKER` (amped)            | magic     | Onion Ring, Eternal Steak                  | maybe  |

#### Terralost retirement (Iris -> vanilla-datapack structures)

The three `terralost/*` biome sources no longer generate in the live world, so each boss moves to a
rare custom structure dropped into a fitting vanilla biome - one shared pipeline (custom jigsaw
structure + amped vanilla-entity boss + PDC anti-farm), no custom biomes required:

- **Caldera Tyrant** (`volcanic-crater`/`caldera` -> `leaf:caldera_throne`): a **throne-room** over a
  lava basin in the Nether (`basalt_deltas`); basalt/blackstone/gilded-blackstone build, magma
  braziers, soul-fire sconces. `BLAZE` boss anchored to the throne (interact/approach to wake),
  `MAGMA_CUBE` court adds, fireball-volley + fire-rain hazard zones. Ambient Nether mobs suppressed
  inside the arena bounds; structure-set spacing tuned sparse so it's a genuine expedition.
- **Glacier Warden** (`glacial-chasm` -> `leaf:frozen_prison`): an **undead "ice jail"** structure in
  vanilla snowy biomes (`snowy_taiga`/`snowy_slopes`/`frozen_peaks`); ice/packed-ice cell blocks,
  iron-bar cages, soul-lantern light. Boss is now **undead** (`WITHER_SKELETON`) rather than an
  `IRON_GOLEM`, with skeleton/stray "draugr" jailer adds (PDC `boss_add`, despawn with the site).
  Cross Necklace reads as the on-theme holy ward drop.
- **Sand Revenant** (`ancient-sands` -> `leaf:sunken_tomb`): a sphinx/tomb ruin in `desert`/`badlands`;
  undead `HUSK`/`STRAY` boss. Shares the structure schema/pipeline with the frozen prison.

Bosses use entity types that never spawn naturally in the relevant context where
possible, so the gate and loot logic recognise "this is one of ours" by a PDC marker
rather than fragile heuristics. The amped spellcasters above are detailed in the next
section.

---

## Boss anatomy (all bosses, dependency-free)

Each boss is a vanilla entity, buffed and tagged at spawn through the Paper API:

- **Identity:** `customName` (Adventure Component), `setCustomNameVisible(true)`,
  `setPersistent(true)`, `setRemoveWhenFarAway(false)` so it does not despawn.
- **PDC marker:** `pdc.set(BOSS_ID_KEY, STRING, "<bossId>")` + `BOSS_BIOME_KEY`. This is
  the single source of truth for "this entity is boss X" used by the kill listener,
  loot, and the anti-farm gate. (Resolves CUSTOM-MOB-SPAWNS-PLAN open decision #1.)
- **Stats:** `MAX_HEALTH`, `ATTACK_DAMAGE`, `MOVEMENT_SPEED`, `KNOCKBACK_RESISTANCE`,
  `ARMOR` via the Attribute API; per-boss values in config.
- **Gear / look:** equipment + drop-chance 0 so gear is not farmable.
- **Health bar:** vanilla `Bukkit.createBossBar(...)`; players within radius are added,
  `setProgress(hp/maxHp)` updated on `EntityDamageEvent`; removed on death/flee.
- **Abilities (optional, per boss):** simple, event/timer driven via the entity
  scheduler (e.g. summon adds, AoE pull, frost slow). Kept minimal for v1.

### Folia-safety

- Add `folia-supported: true` to the plugin `plugin.yml`.
- Use `entity.getScheduler()` for per-boss ticking/abilities and
  `world.getRegionScheduler()` for location-bound effects; never `BukkitRunnable`/the
  legacy global scheduler. Event handlers already run on the owning region thread.

---

## Magic / spellcaster enemies (amped up)

Magic enemies are the most "fun when amped up" archetype: vanilla already ships
spellcasters (Evoker, Witch, Illusioner) and projectile casters (Blaze) whose abilities
can be re-driven server-side into genuinely threatening encounters with zero
dependencies. Several bosses in the roster above are magic archetype; this section
defines the shared toolkit so they read as casters, not just high-HP melee.

### Why magic fits this layer

- Spellcaster bosses create readable, dodge-able "tells" (vanilla evoker fangs, witch
  potion arcs), so an amped fight stays fair rather than just a damage sponge.
- They naturally justify **adds**, hazards, and zones - more variety than a melee bruiser.
- All the pieces are Paper API: summon entities, launch `ThrownPotion`/`SmallFireball`,
  spawn `EVOKER_FANGS`, apply potion effects, particles - no client mod needed.

### Base casters and how to amp them (server-side only)

| Base entity      | Vanilla magic                          | Amped behaviour (config-driven, scheduled) |
|------------------|----------------------------------------|---------------------------------------------|
| `EVOKER`         | summons Vexes, casts fangs             | denser fang lines aimed at target, larger Vex waves, shorter cast cooldown |
| `WITCH`          | throws splash potions, self-heals      | volley of harmful potions (poison/weakness/slow), buffs nearby adds, throws lingering clouds |
| `ILLUSIONER`     | blindness + mirror-image decoys (unused in vanilla survival) | blindness pulses + spectral duplicate adds; great for an "trickster" forest/cave boss |
| `BLAZE`          | fireball volleys                       | spread/spiral fireball patterns, brief fire-rain zones |
| `EVOKER_FANGS`   | (spell entity, not a mob)              | summoned in rings/lines by any caster boss as a telegraphed AoE |
| `VEX`            | phasing melee adds                     | summoned in waves; capped count; despawn with the boss to prevent farming |

Note: `ILLUSIONER` does not spawn naturally in survival, so (like WITHER_SKELETON in the
overworld) it is a clean PDC-markable boss base.

### Shared "spell" toolkit (one helper, reused per boss)

A small `SpellKit` in `leaf-bosses` exposes composable, Folia-safe casts the death/tick
listener can schedule on the boss's entity scheduler:

- `fangLine(origin, direction, length)` / `fangRing(center, radius)` - spawn `EVOKER_FANGS`.
- `potionVolley(target, effects)` - launch `ThrownPotion`s (witch-style).
- `summonAdds(type, count, cap)` - PDC-tag adds as `boss_add` so they despawn with the
  boss and never drop boss loot (anti-farm).
- `hazardZone(center, radius, effect, ticks)` - timed area effect (fire, slow, wither)
  via region-scheduler particles + per-entity effect application.
- `teleportBlink(maxDist)` - short reposition for trickster casters.
- All casts respect a per-boss global cooldown and are picked by a simple weighted
  phase table (e.g. < 50% HP unlocks the bigger spells).

### Magic adds for Tier-1 (flavour, farmable)

The biome-rarity gradient still applies: signature magic biomes can sprinkle **Tier-1**
amped-but-farmable casters (a lone amped Witch in the blight, Vex motes in the spiral
grove) as ambient threat, separate from the gated boss. These use the same `SpellKit`
with weak parameters and ordinary loot, and are NOT PDC-marked as bosses.

### Anti-farm specifics for magic bosses

- Summoned adds (`Vex`, fangs, duplicates) are PDC-tagged `boss_add`, capped, and removed
  on boss death/flee so players cannot farm the adds.
- Spell damage and add waves scale with player count in the boss bar radius, not with
  time, so a longer fight is not a bigger reward.

---

## Spawning (rare, biome-gated, anti-farm)

Two complementary options; recommend **A** for v1.

**Option A - plugin-driven scheduled spawn (recommended).**
A `BossSpawnService` runs a low-frequency region-scheduler sweep around online players.
When a player is in a boss biome and no live boss of that type exists nearby (tracked by
PDC scan + a per-biome cooldown), it rolls a low chance to spawn the boss at a valid
natural surface point. Biome is read with `world.getBiome(x,y,z)` and matched against the
datapack biome key (e.g. `leaf:dark_forest_core`). This needs no datapack spawn rules and
gives full control of rarity/cooldown.

**Option B - datapack/natural spawn + gate.**
Use a vanilla `spawn_data`/spawn-cost datapack tweak (or natural spawns of the base type)
and let a listener "promote" a fraction into bosses. More vanilla-native but far less
controllable and noisier; deferred.

Either way, `elite-spawn-gate` is extended to also reject boss spawns in farm-like
geometry (its existing `SpawnValidityPredicate`), so bosses cannot be box-farmed.
One live boss per biome instance + cooldown enforces "rare, hard-fought, not grindable".

---

## Artifacts: one biome-themed artifact per boss

Each boss yields a biome-flavoured artifact. Several can reuse existing `ArtifactType`
entries; a few new types are proposed. Artifacts are built by the existing
`leaf-artifacts` item builder so they slot into the curio system unchanged.

| Boss               | Artifact (reward)        | New? | Effect sketch (fits existing AttributeSpec/effect model) |
|--------------------|--------------------------|------|----------------------------------------------------------|
| Grovewarden        | `verdant_crown`          | new  | head; small MAX_HEALTH + Regeneration ambient            |
| Blight Warden      | `antidote_vessel`        | exists | faster negative-effect decay (thematic for blight)     |
| Frost Elder        | `frostward_charm`        | new  | charm; immune to freeze/slow, minor KNOCKBACK_RESISTANCE |
| Canopy Tyrant      | `feral_claws`            | exists | attack-speed (jungle predator)                         |
| Embervine Warden   | `obsidian_skull`         | exists | fire/lava immunity (ash-jungle)                        |
| Blossom Sovereign  | `lucky_scarf`            | exists | luck (spiral grove is a lucky landmark)                |
| Ashwood Revenant   | `vampiric_glove`         | exists | undead lifesteal (Fire Gauntlet is Emberdeep's)        |
| Old Shepherd       | `crystal_heart`          | exists | max-health (meadow landmark prize)                     |
| Mire Colossus      | `steadfast_spikes`       | exists | knockback immunity (the immovable mire)                |

New artifact types are added to the `ArtifactType` enum exactly like the current ones
(id, display, material, description, attribute/effect lists, slot set). No mechanism
change needed for attribute/effect-only artifacts; event-reactive ones would follow the
existing `ArtifactEventListener` pattern.

The artifact item must be obtainable from a datapack loot table and an advancement
reward function. Today artifacts are only created in-code (`/artifact give`). Proposed
bridge: define each artifact as a vanilla item carrying its `leafartifacts` id in
custom data (so a datapack `loot_table` / `/give ... with components` can mint it), and
have `leaf-artifacts` recognise that id on pickup/equip (it already keys items by id via
`ArtifactKeys`). This is the one cross-cutting addition and the main open question
(see Open Decisions).

---

## Advancements (first-kill reward) + loot table (repeat-kill drop)

Mirror the `leaf-skilltree` datapack convention in a new datapack `leaf-bosses`
(auto-discovered by `deploy-datapacks.ps1`).

### Advancement (guaranteed, once)

- One advancement per boss under `data/leafbosses/advancement/boss/<boss>.json`.
- Trigger: `minecraft:player_killed_entity` filtered by the base entity type. Because
  vanilla advancement predicates cannot read our PDC, the plugin instead listens to
  `EntityDeathEvent`, checks the PDC marker, and on first kill grants the advancement +
  the artifact directly (and runs a themed reward function). This keeps the "first kill
  -> artifact" guarantee robust and farm-proof.
- Advancement is display/announce only (toast + chat); the plugin owns the grant logic,
  so it works on Folia and cannot be faked by killing a vanilla same-type mob.

### Loot table (rare, repeatable)

- `data/leafbosses/loot_table/boss/<boss>.json` with the themed artifact at a low
  weight/`set_count`, plus minor vanilla rewards (xp handled separately).
- The boss entity has no natural loot; on `EntityDeathEvent` for a PDC-marked boss the
  plugin rolls this loot table (`LootTables`/`LootContext`) so the drop is tied to the
  marked boss, not the base type. Rare artifact drop on repeats; guaranteed on first
  kill via the advancement path above.

This satisfies "artifact given for the advancement PLUS rare on the loot table":
first kill = certain (advancement), subsequent kills = rare (loot table).

---

## Components to add / touch

- **`plugins/leaf-bosses/`** (new Paper plugin module, mirrors `elite-spawn-gate`
  `build.gradle`; `folia-supported: true`):
  - `BossType` (enum/registry: id, biome key, base entity, stats, artifact id, abilities)
  - `BossManager` (spawn + PDC tag + boss bar)
  - `BossSpawnService` (region-scheduler rarity/cooldown sweep)
  - `BossDeathListener` (PDC check -> first-kill advancement+artifact, else loot roll)
  - `config.yml` (per-boss stats, spawn chance, cooldown, radius)
- **`datapacks/leaf-bosses/`** (new datapack): per-boss advancement + loot table.
- **`plugins/leaf-artifacts/`**: add new `ArtifactType`s; add a loot/datapack mint path
  (recognise the artifact id on dropped/given items). Small, additive.
- **`plugins/elite-spawn-gate/`**: add boss entity types to the gated set (config only),
  or have `leaf-bosses` reuse its predicate.
- **Docs:** an ADR if the artifact loot-minting bridge is deemed architecturally
  significant.

---

## Open Decisions (settle during the vertical slice)

1. **Artifact loot bridge.** Simplest robust path is plugin-minted artifacts on a
   PDC-marked boss death (no datapack item needed for the drop), with the datapack loot
   table only describing vanilla extras. Decide whether datapacks ever need to mint
   artifacts directly (would require the item-id-in-custom-data recognition path).
2. **Spawn model.** Confirm Option A (plugin sweep) vs B (datapack natural + promote).
3. **Boss bar scope.** Per-player vs shared; radius to add/remove players.
4. **Ability depth for v1.** Stats-only bosses first, abilities as a follow-up?
5. **Vertical slice target.** Recommend `dark_forest__giant` -> Grovewarden ->
   `verdant_crown` as the first end-to-end slice (spawn, gate, boss bar, first-kill
   advancement+artifact, rare loot drop), then generalise.

---

## Proposed first slice (for approval)

Build only the Grovewarden end-to-end:

1. `leaf-bosses` plugin scaffold (Folia-safe), config for one boss.
2. Spawn + PDC tag + boss bar in `dark_forest_core` variant, gated by `elite-spawn-gate`.
3. New `verdant_crown` artifact in `leaf-artifacts`.
4. `leaf-bosses` datapack: Grovewarden advancement + loot table.
5. Death listener: first kill grants advancement + `verdant_crown`; repeats roll the
   rare loot table.
6. Deploy via `deploy-plugins.ps1` + `deploy-datapacks.ps1`; pause and ask the user to
   boot/`/reload` for in-game verification (server boots are user-triggered).
