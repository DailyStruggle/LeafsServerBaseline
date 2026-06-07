# Custom Mob Variants & Biome-Specific Spawns - Design Note

**Status:** Draft (design discussion captured; not yet approved for implementation)
**Date:** 2026-06-06
**Author:** Leaf (with Junie)

This note captures the agreed design direction for custom mob variants, named mobs,
and biome-specific spawning on this server. It is a planning document, not a spec.
Implementation is gated behind a separate vertical-slice proposal (Rule D-005).

---

## Goal

Make custom biomes feel like their own places by spawning custom mobs in them, while:

1. Keeping the world vanilla-client-friendly (server-side only, no required mods).
2. Augmenting - not replacing - vanilla spawning, scaled by how custom a biome is.
3. Rewarding hard-fought battles with meaningful loot, without rewarding grinding.
4. Still allowing farming as a valid playstyle for ordinary mobs.

These goals come directly from the world-building philosophy in
`docs/world-design/IRIS-WORLD-BUILDING.md` (extend the familiar, do not erase it).

---

## Constraints & Principles

- **Server-side only.** Mobs must function for vanilla clients. Cosmetic texture
  variants (resource pack) are an optional, separate layer and not required for behavior.
- **Repo scope (ADR-001).** Spawning is owned by Iris. Any custom logic we add is
  narrow, server-specific game-design code - it stays in this repo as a thin layer.
  If it ever generalized into a reusable engine, it would graduate to its own repo.
- **Iris is the spawn engine.** We do not build a general custom-mob engine. We lean
  on Iris entities + spawners and add the smallest possible layer on top.

---

## Decision: Two-Tier Mob Model

Custom mobs are split into two tiers. Only the top tier needs custom logic.

### Tier 1 - Common custom mobs (fully Iris, farmable)

- Defined as Iris entities; spawned by Iris spawners referenced per biome.
- Subject to the **biome-rarity gradient** (see below).
- **Farmable on purpose.** Ordinary / bulk-friendly loot. These are the "renewable
  economy" mobs. No validity gate, no anti-farm logic. They behave like vanilla mobs
  with custom flavor (name, gear, stats).

### Tier 2 - Rare loot-bearing elites (Iris spawns, our layer gates)

- Also spawned by Iris, but flagged as "elite / loot-bearing."
- Our thin layer intervenes **only here**: on spawn it runs a validity predicate and
  cancels the spawn if the location looks like a farm (player-placed blocks nearby,
  enclosed dark box, insufficient natural floor/volume, too close to player builds).
- Because the gate touches only flagged mobs, ordinary farms keep working - you simply
  cannot farm the elites. Their drops carry the "hard-fought battle" rewards.

Rationale: the expensive, opinionated logic runs on a small subset of spawns, so it is
cheap and conceptually clean. Farming stays valid; only the best loot refuses to be farmed.

---

## Biome-Rarity Gradient (applies to both tiers' spawn rates)

Spawn weight lives in the per-biome spawner reference, not in the entity, so the same
mob can be rare in one biome and common in another.

| Biome class                         | Custom-mob share | Spawner reference |
|--------------------------------------|------------------|-------------------|
| Vanilla-derived (meadow, grove, ...) | 0%               | none (pure vanilla) |
| Lightly-themed overlays              | low              | low-rate spawner  |
| Signature / "super custom" (embervine, ashwood) | 30-60% | high-rate spawner |

This gradient is the primary tunable design dial.

---

## Loot Philosophy (Tier 2)

Grind-resistance comes from making rewards per-encounter and not cheaply repeatable,
not from drop tables alone:

- **Tie loot to uniqueness, not kills.** Rare spawn + good drop is grind-proof because
  you cannot farm what rarely spawns; common mob + rare drop invites AFK farms.
- **Prefer rewards you cannot auto-collect:** one-time upgrades, progression unlocks,
  keys/tokens, ingredients that are useless in bulk.
- Optional: diminishing returns on repeat elite kills within a session.

Detailed loot/encounter rules are a separate design concern, to be specced later.

---

## Open Decisions (must be settled before / during the vertical slice)

1. **Elite marker mechanism** - how the gate identifies a Tier-2 mob. Candidates:
   Iris `customName`, a scoreboard tag, or a PDC key. Leaning toward a scoreboard tag
   or PDC key (robust, not player-visible) plus an optional display name.
2. **Validity predicate rules** - the minimum bar for "natural enough." A v1 such as
   "reject if any player-placed block within radius N, or open sky/cave volume below a
   threshold" is enough to defeat box farms; tune from there.
3. **Loot model** - one-time progression unlocks vs. repeatable-but-rare gear with
   diminishing returns. This decides whether the thin layer is a datapack predicate set
   or a small Paper plugin.

---

## Vertical Slice - Implemented (embervine)

Approved decisions for the first slice:

- **Thin-layer form:** small, event-driven Paper plugin (more efficient and functionally
  correct than a datapack for must-cancel-before-spawn gating).
- **Elite marker (v1):** entity type. The warden is a `WITHER_SKELETON`, which never spawns
  naturally in the overworld, so type uniquely identifies the gated elite. (PDC marker is a
  later refinement for loot recognition.)
- **Validity predicate (v1):** require natural ground beneath the spawn, and reject if any
  artificial block is within the scan radius. Both configurable.
- **Reward:** one netherite ingot, dropped via the Iris loot table `embervine-warden`.

Files in this slice:

- `iris/pack-overlay/entities/embervine/embertouched_husk.json` - Tier-1 (HUSK, farmable).
- `iris/pack-overlay/entities/embervine/embervine_warden.json` - Tier-2 elite (WITHER_SKELETON,
  named, geared, buffed attributes, references the loot table).
- `iris/pack-overlay/loot/embervine-warden.json` - netherite ingot reward.
- `iris/pack-overlay/spawners/embervine-common.json` - Tier-1 spawner (higher rate).
- `iris/pack-overlay/spawners/embervine-elite.json` - Tier-2 spawner (rare).
- `iris/pack-overlay/biomes/vanilla/jungle__ember.json` - references both spawners via
  `entitySpawners`.
- `plugins/elite-spawn-gate/` - the Paper plugin (biome-agnostic anti-farm gate; gated
  entity types are configured per server, so it serves all signature biomes, not just embervine).

## Biome Targets

Candidate biomes for custom mobs, with tier intent and edit surface. Source layer:
`overlay` = `iris/pack-overlay/biomes/`, `base` = `iris/pack-base/biomes/` (terralost/carving
exist only in base, so they are edited in place). Status `done` = shipped, `planned` =
agreed candidate, `maybe` = lighter/secondary. Priority 1 = next slices.

| Biome | Source layer | Theme | Tier-1 (farmable) | Tier-2 (gated elite) | Anti-farm fit | Status | Prio |
|-------|--------------|-------|-------------------|----------------------|---------------|--------|------|
| `vanilla/jungle__ember` | overlay | Embervine / ash-jungle | Ember-touched Husk + Cinderborn (ZOMBIFIED_PIGLIN) + Ashfang Spider (CAVE_SPIDER) | Embervine Warden (WITHER_SKELETON) | surface | done | - |
| `vanilla/badlands__ashwood` | overlay | Ashwood mesa | Ashbound Stray (STRAY) + Ashveil Spider (SPIDER) | Ashwood Revenant (WITHER_SKELETON) | surface | done | - |
| `terralost/volcanic-crater` | base | Volcanic crater | Cinderborn (ZOMBIFIED_PIGLIN) + Magma-forged Husk | Caldera Tyrant | surface | planned | 1 |
| `carving/volcanic` | base | Volcanic cavern | Emberdeep ambient | Emberdeep elite | cave (high) | planned | 1 |
| `terralost/caldera` | base | Caldera highlands | Magma-forged Husk (shared) | Caldera Tyrant (shared) | surface | planned | 2 |
| `vanilla/dark_forest__blight` | overlay | Blighted dark forest | Witch + Cave Spider (poison) | Blight Warden | surface | planned | 2 |
| `terralost/glacial-chasm` | base | Frost chasm | Rime Stray (STRAY) | Glacier Warden | surface/cave | planned | 2 |
| `terralost/frozen-cliffs` | base | Frost cliffs | Rime Stray (shared) | Glacier Warden (shared) | surface | maybe | 3 |
| `terralost/ancient-sands` | base | Desert ruin | Sundried Husk + Stray (ranged) | Sand Revenant | surface | planned | 2 |
| `terralost/desert-canyon` | base | Desert canyon | Sundried Husk (shared) | Sand Revenant (shared) | surface | maybe | 3 |
| `carving/deep` | base | Deep dread cave | Silverfish swarm + Cave Spider | Deep elite | cave (high) | planned | 2 |
| `carving/deepravine` | base | Deep ravine | - | Deep elite (shared) | cave (high) | maybe | 3 |
| `carving/lush` | base | Lush cave | Lush ambient | Lush cave elite | cave (med) | maybe | 3 |
| `swamp/creaks` | overlay | Ambush mire | Spider/Cave Spider ambush + Slime swarm | The Creak | surface | maybe | 3 |
| `terralost/skylands` (+seasonal) | base | Floating isles | Sky flyer (PHANTOM-style) | (optional) | n/a | maybe | 3 |
| `vanilla/jungle__giant` / `forest__giant` / `meadow__giant` | overlay | Overgrowth | oversized flavor mob | none | n/a | maybe | 3 |

Vanilla-flavor variants (`__young` / `__tall` / `__spiral` / `__dense`) and plain vanilla
biomes stay at 0% custom spawns to preserve the gradient and are intentionally absent here.

## Cave Structure Design

**Status:** Active (current focus). Vanilla spawn density in caves is solved; caves now
lack hand-built structures, which this section tracks. Note: **custom mobs are not yet
solved** - that work is real but out of scope for this structure-design effort and is
tracked separately (see Biome Targets `planned` rows and Deferred / Follow-ups).

### Problem

Cave biomes under `iris/pack-base/biomes/carving/**` (`deep`, `lush`, `volcanic`,
`deepravine`, plus the `cavesv4/` and `deepdark/` sets) define only terrain
(`wall` / `layers` / `caveCeilingLayers`). They have no `objects` and no jigsaw
structures, so caves feel empty between mob encounters.

### Two structure mechanisms (pick per use case)

1. **`objects[]` placement** (same mechanism `vanilla/jungle__ember` uses for trees):
   scatter `.iob` objects from `iris/output` with `chance` / `density` / rotation.
   Best for decoration-scale set-pieces: ruined pillars, crystal clusters, abandoned
   camps, ore-vein features. Cheap to author, no connectors. **Ships in-repo today.**
2. **Jigsaw structures** via `iris/scripts/nbt_to_iris.py` (vanilla `.nbt` -> Iris
   jigsaw pieces + `.iob`). Best for multi-piece cave dungeons. **Not blocked - usable
   now under ADR-004.** This server is private and non-commercial, so extracting and
   converting restricted-source packs (e.g. Towns & Towers, CC BY-NC-ND 4.0) for our own
   use is permitted; only redistribution (Sharing adapted material) is forbidden.
   Operationally that means derived assets live in the git-ignored
   `iris/thirdparty-derived/` quarantine and are merged only into the git-ignored
   `iris/staging/` at build time - they are never committed to `pack-overlay` and never
   published. Net: we can wire these dungeons into the live server today; we just cannot
   ship them in-repo.

### Layering constraint (overlay, not base)

Carving biomes are read-only `pack-base` files (project guideline: never edit
`pack-base`). To attach structures, create same-path overrides under
`pack-overlay/biomes/carving/` (e.g. `pack-overlay/biomes/carving/deep.json`).
Overlay is a whole-file replacement, so each override must copy the full base biome
and add the `objects` block; it goes stale if `sync-base-pack.ps1` updates the base.
This argues for overriding only a handful of signature caves, not all ~30.

### Implemented - vanilla-like jigsaw structures (dungeons + mineshafts)

The upstream Iris `pack-base` already ships complete dungeon and mineshaft jigsaw
structures (`jigsaw-structures/dungeon-{zombie,skeleton,spider}.json`,
`jigsaw-structures/mineshaft.json`) with their pieces and pools - they were just not
referenced by the dimension. ADR-004 does **not** apply: these are upstream base-pack
assets, not assets we derived via `nbt_to_iris.py`.

Wired them in by extending `jigsawStructures` in the overlay dimension override
`iris/pack-overlay/dimensions/overworld.json` (no `pack-base` edits):

- `mineshaft` (rarity 80), `dungeon-zombie` / `dungeon-skeleton` / `dungeon-spider`
  (rarity 40 each), alongside the pre-existing `trail_chambers` (rarity 1600).

Rarities are first-pass and deliberately on the common side; they will be tuned in-game
and re-balanced as more structures are added to the same array.

### Licensing & redistribution boundary (settled)

The asset-licensing question is resolved (ADR-004 + owner direction):

- **Use:** allowed. Restricted-source structure packs may be extracted/converted and run
  on this private, non-commercial server.
- **Redistribution:** the only constraint. What we ship publicly is essentially the
  "flat" Iris world-gen adjustment (regions/biomes/dimension tuning), **not** the
  structures. Restricted-source-derived structures stay quarantined
  (`iris/thirdparty-derived/`, git-ignored) and are never published.
- **Redistributable builds:** there will be very few structures we can actually
  redistribute. Where redistribution matters, prefer ideologically similar original or
  free-licensed work to replace the restricted supplied packs.

### Direction / next slice (pending Rule D-005 proposal)

1. Wire `nbt_to_iris.py`-derived dungeon/cave packs (e.g. Towns & Towers cave/dungeon
   sets) into the live server via the quarantine + `iris/staging/` build path - permitted
   for our private use, kept out of the tracked tree.
2. Author or source original / free-licensed `.iob` cave set-pieces for anything we may
   want to redistribute, and attach them via `objects[]` in 2-3 signature caves
   (`carving/volcanic`, `carving/deep`) through overlay overrides.
3. Keep the redistribution boundary intact: only the flat Iris adjustment is shippable
   in-repo; restricted-source structures remain quarantined.

---

## Deferred / Follow-ups

- **Enchanted books reward** - to be added later via a vanilla datapack, layered on top of
  the netherite drop (do not bake into the Iris loot table). Tracked here as the next loot
  iteration.
- **PDC marker** - stamp gated elites with a PersistentDataContainer key so future loot /
  encounter rules recognize them independently of entity type.
- **Density tuning** - DONE (vanilla density only). Vanilla spawn density in caves and
  signature biomes has been validated and tuned in-game; spawner `maximumRate` / `rarity`
  settled. **Custom mobs remain unsolved** and are not closed by this item - they stay an
  open follow-up, just out of scope for the current cave structure work. Current focus has
  moved to cave structure design (see *Cave Structure Design* above).
- **Generalize** - once validated, extend the two-tier pattern to other signature biomes
  (e.g. ashwood) and the special caves.
