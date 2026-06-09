# Structure Placement Design (running spec)

Date started: 2026-06-07
Status: PAUSED (2026-06-07) - deferred to Iris 4.0. Draft retained for reference only.
Rule D-005 approval was never sought; do NOT implement this on Iris 3.9.

> PAUSE NOTE (2026-06-07): All currently-foreseen use cases are expected to be solved by
> Iris 4.0 (true underground/vertical jigsaw, better biome targeting). Work on this custom
> three-layer marker + own-jigsaw-assembler pipeline is paused. Revisit only if a concrete
> use case survives the move to Iris 4.0. The analysis below is kept as a record of why the
> 3.9 workaround was considered and what its trade-offs were.

> Purpose: capture the evolving design for placing custom structures into specific biomes
> (above ground and below ground), working around Iris 3.9 limitations. This is the
> single running spec; keep updating it until it is implementation-ready, then promote the
> stable decisions into an ADR.
>
> Related: `docs/design/BIOME-STRUCTURE-CONCEPTS.md` (design catalog of structures per
> biome), `docs/scratch/STRUCTURE-INTEGRATION-RESEARCH.md` (the prior Iris-jigsaw-per-biome
> approach this spec supersedes for large/vertical/biome-exact placement).

---

## 1. Problem statement

We want curated custom structures tied to specific biomes, both surface and underground,
that read well and reward exploration. The current Iris-based approach has hard limits:

- Iris 3.9 jigsaw is effectively single-Y / surface-snap; no real verticality. (4.0 adds
  true underground jigsaw, but we are on 3.9.)
- Iris places structures per REGION, not per BIOME. A region spans many biomes and terrain
  shapes, so structures leak into wrong biomes and land on cliffs/spires.
- Large structures float on pillars over uneven terrain because each footprint corner
  snaps independently.
- Loot: some Iris-placed structures resolve to 0 items (the `vanillaLoot` application is
  unreliable in our setup), even though the chest tile is present in the object source.

The "ice-fire" structures are the canonical failure: placed on pillars and cliffs instead
of a flat anchor, because placement was tied to a region rather than to biome + land shape.

---

## 2. Core architecture (three layers)

Use each tool for what it is good at. Iris becomes a distribution engine only; all hard
geometry moves into our own code.

1. Distribution (Iris, at pregen): Iris sprinkles lightweight, deterministic MARKERS per
   region rarity. Markers are cheap, tiny objects Iris can reliably place. Iris inherits
   us its spacing/separation for free.
2. Discovery + context (anvil-api, off-thread, read-only): an iterator sweeps region files
   after pregen, finds markers, and builds an in-memory pre-placement set. For each marker
   anchor it reads richer-than-NBT context: actual biome and land shape (see Section 4).
3. Assembly (our own jigsaw, matching vanilla behavior): consume standard NBT structure
   templates and assemble them ourselves (NOT via NMS, NOT via the vanilla placement
   pipeline). We only reimplement the jigsaw ASSEMBLY algorithm; the placement DECISION is
   already made by the marker + context layer. Write blocks via Paper APIs.

### Why markers
- Iris cannot do flat/biome-exact/large placement, but it CAN reliably scatter a 1-block
  marker per region roll. We use Iris purely as a seed-stable distribution engine.
- The marker is a known anchor, so the anvil read for biome + land shape happens around a
  point, with no scanning.
- Markers double as the idempotency record for the pregen'd region: mark each marker
  "consumed" so a re-run of the sweep is naturally idempotent.

### Why own the jigsaw assembler (match, do not invoke)
- We control the trigger already (marker + context), so we need only the assembly
  algorithm, not vanilla's placement-decision machinery (structure sets, placement configs,
  biome tags) which we are deliberately bypassing.
- No NMS coupling: stay on stable Paper block APIs; no per-version obfuscation churn.
- Determinism is ours: seed our RNG from `(worldSeed, markerX, markerZ)` so batch-pregen
  and live exploration produce identical results.
- Loot is native: jigsaw chests carry a `loot_table` we set at paste time. This removes the
  Iris `vanillaLoot` 0-items bug from the critical path entirely.

### Why vanilla jigsaw semantics
- Verticality: vanilla jigsaw places pieces with real Y offsets via connector alignment,
  enabling towers, multi-floor keeps, descending dungeons, deep-dark-city style. This is the
  decisive advantage over Iris jigsaw even before considering 4.0.

---

## 3. Jigsaw assembler - behavior to match

The vanilla jigsaw algorithm is well-specified and small to reproduce:

1. Read standard structure NBT: palette + block list + block entities + `minecraft:jigsaw`
   blocks (`name`, `target`, `pool`, `final_state`, `joint`).
2. Template pool model: a start pool, then per jigsaw block a target pool to draw the next
   element from, with weights.
3. Connector matching: pair a jigsaw block to a candidate element's jigsaw block whose
   `target` matches `name`; align by orientation; honor `joint` (`rollable` vs `aligned`)
   for rotation.
4. Placement + recursion: place start element, queue its jigsaw blocks, draw/align/attach
   children up to `max_depth`, with bounding-box overlap rejection (gives branching +
   verticality).
5. `project` mode: `rigid` (exact offset incl. vertical stacking) vs `terrain_matching`
   (snap that piece to heightmap). This is the verticality knob.
6. Block-entity post-processing: jigsaw blocks become `final_state`; chests get their
   `loot_table` NBT set (native loot).

Pieces 3-5 are the only non-trivial logic; they are deterministic graph/geometry code,
easier to own and unit-test than to drive through NMS.

Risk: subtle rotation/joint and bounding-box rules can drift from vanilla, causing overlaps
or gaps. Mitigation: a golden-test set (known pool + seed -> asserted layout) to pin
behavior and catch regressions.

---

## 4. Richer-than-NBT placement context (new)

The anvil iterator can gather more than the NBT template provides, and feed it into the
assembler so placement adapts to the actual world at each marker:

- Biome (exact): read the real biome at the anchor (and across the footprint). This is the
  true fix for "structures not tied to specific biomes" - Iris regions can't give this, but
  the anvil read can. Use it to confirm the marker's intended structure belongs here, or to
  pick a biome-appropriate template pool / palette variant.
- Land shape: sample the surface heightmap over the intended footprint to derive:
  - flatness (max-min delta) -> accept/reject site, or choose a foundation strategy;
  - slope direction/grade -> orient the structure, or pick a terraced/stilted variant;
  - surface material / waterline -> choose palette (e.g. stilts over water, sand vs grass
    foundation), avoid placing in lava/water unless intended.
- Vertical room: probe air/solid above and below to decide between surface, dug-in, or
  cliff-anchored placement, and to validate that a vertical jigsaw has room to grow.

Design implication: the template pool can expose CONDITIONAL pieces/variants keyed on this
context (e.g. a "foundation" pool element selected when flatness is poor; a "stilt skirt"
element selected over water). The assembler picks among them deterministically using the
context + seeded RNG. This is where "more powerful than vanilla jigsaw" lives: vanilla picks
pieces by pool weights alone; we additionally gate piece selection on measured biome + land
shape.

Open question (4.a): how much context to bake into the marker vs measure at sweep time. Lean
toward marker = "which start pool + variant family", context = "which conditional pieces".

---

## 5. Surface vs underground (within this model)

- Surface: `terrain_matching` start piece (or a measured anchor Y from the heightmap), plus
  conditional foundation/stilt pieces selected from land-shape context. Replaces the old
  Iris `bore: false` / surface-snap approach.
- Underground: `rigid` placement at a context-validated depth band; the start piece is a
  sealed chamber, children grow downward/outward via connectors. Replaces the old Iris
  `bore: true` + `overrideYRange` STATIC hack. Verticality lets multi-room dungeons descend
  properly instead of being a single forced Y level.

---

## 6. Runtime extension (conditional)

For chunks generated AFTER pregen (only if exploration past the worldborder is allowed):

- Iris still writes the marker during live generation.
- A region-thread-local handler detects the marker (ChunkLoad hint -> off-thread anvil
  confirm), runs the SAME deterministic context + conflict resolution scoped to the local
  neighborhood, and pastes on the owning region thread in bounded batches.
- Determinism parity: runtime placement must equal what the batch sweep would have produced.
  Conflict resolution must be a pure function of a fixed-radius WORLD-COORDINATE window
  around the marker, independent of load order / what is currently loaded.
- Hard problem - cross-region-thread footprint spanning: a structure near a region boundary
  may need to write blocks owned by another region thread (worse with large vertical
  structures). Options: constrain footprints to a single region's ownership; hand the
  cross-border slice to the neighbor region thread via the scheduler; or reject placements
  whose footprint spans a boundary (lose a few sites, keep correctness). MUST be decided
  before implementing Phase 3.

If exploration is bounded by the worldborder and pregen covers it, Phase 3 may not be needed
at all (batch sweep handles everything once). Prefer this if the border is fixed.

---

## 7. Idempotency, determinism, conflict resolution

- Deterministic roll: placement is a pure function of `(worldSeed, sectionX, sectionZ)` (or
  marker coords). The visited-set / marker-consumed state is only a dedup cache, never the
  source of truth. This sidesteps the unreliable `ChunkLoad.isNewChunk()` entirely (Iris
  generates off-thread and via pregen, so first-gametick is unobservable).
- Authority on conflict: the anvil-api (read-only, off-thread) + the consumed-marker state
  are the cross-session authority on "already placed"; the in-memory pending set is the
  within-session authority (guards double-enqueue before a write lands on disk).
- Conflict between nearby markers: a resolution pass over the in-memory set BEFORE any paste,
  with deterministic priority (e.g. tier/rarity, then coordinate hash) so it is reproducible.
- Every placement leaves a machine-detectable MARKER/sentinel so future anvil reads can
  distinguish our structures from incidental terrain.
- Marker hygiene: markers must be inert/buried so a pregen-then-crash leaves no
  player-visible litter.

### 7.1 Chunk consumption / Y-banded overlap guard

Model overlap prevention as a chunk reservation table with two distinct states:

- `visited` = this section's deterministic roll was EVALUATED (stops re-evaluation/retry
  loops, including sites rejected for poor flatness). A failed-flatness section is `visited`
  but never `used`.
- `used` = a structure occupies space here. This is the actual overlap guard.

Rules:

- Reserve the bounding VOLUME, not a point or a 2D column. Claim every chunk the assembled
  bounding box touches - not just the marker's chunk. Footprints are variable (verticality +
  branching), so the extent is unknown until the jigsaw is assembled (or bounded). Therefore:
  assemble/bound FIRST, then claim.
- Reservation key carries a Y-band: `used:{chunkX, chunkZ, yMin, yMax, structureId, anchor}`.
  Overlap = same `(chunkX, chunkZ)` AND overlapping Y-bands (plus a vertical buffer of a few
  blocks of intervening rock). This is the direct fix for "prevent cave structures under land
  structures": a surface keep (`y 60..90`) and a deep dungeon (`y -40..-10`) legitimately
  share a column and do NOT collide; two surface structures do. A purely 2D claim table would
  over-reject and destroy all desirable surface/underground co-location.
- Deterministic claim order: when two markers contest the same volume, resolve by
  `(tier/rarity, then coordinate hash)` over a fixed world-coordinate window (the §7 conflict
  pass). The winner claims; the loser sees `used` and aborts. Order must be independent of
  load order so batch-pregen and runtime exploration agree.
- Cross-session reconstruction: the in-memory table is within-session truth; it MUST be
  reconstructable from on-disk consumed-markers/sentinels (anvil-api authority) so it does
  not drift across a sweep re-run or crash.
- Future (not Phase 1): instead of evicting a contested cave marker under a surface landmark,
  the conflict pass MAY intentionally PAIR them and assemble a linked surface+cave set.

---

## 8. anvil-api integration notes

- The custom anvil-api (used in RTP, under `Documents`) gives reliable off-thread read-only
  region data. It is a STATE oracle ("what is on disk"), not an EVENT oracle ("first
  gametick") - and we do not need the latter.
- Read/write race: anvil reads disk state, which can lag a chunk that is loaded-but-unsaved.
  In-memory pending set guards within-session; anvil is cross-session authority.
- Coordinate alignment: align section-key bucketing to region-file boundaries (32x32 chunks)
  where convenient so each job stays within one region file.
- Read-only discipline: all anvil-api use stays read-only; all writes go through Paper block
  APIs on the region thread. Never edit region files under a live server.

TODO: point this spec at the actual anvil-api path/API surface and align the read interface
to it (currently described abstractly).

---

## 9. Plugin boundary

- New, separate plugin (working name `leaf-structures`). Do NOT bolt onto `leaf-artifacts`
  (wearables) - different concern.
- Author structures as standard NBT structure templates (in `datapacks/` or a plugin
  resource dir, TBD), matching the format the reference packs ship; the marker selects which
  start pool to assemble.
- Build hard parts (assembler) as PURE functions (NBT + context + seed -> block plan) so they
  are unit-testable headless. Fits the "agent never boots the server" constraint - assembly
  can be validated without a running server; only in-world visual verification needs a
  user-triggered boot.

---

## 10. Phasing

- Phase 0: (obsolete) standalone loot fix - now subsumed; jigsaw chests fill natively at
  paste time. Keep only as a validation checkpoint for the chest-fill code.
- Phase 1: marker scheme + anvil iterator + assembler with a SINGLE small pool +
  surface-foundation handling; batch-only; worldborder-bounded. Proves the pipeline
  end-to-end and the loot path.
- Phase 2: full vertical multi-piece pools (towers/dungeons) per biome, with conditional
  context-gated pieces (foundation/stilt/terrace; surface vs underground).
- Phase 3: runtime region-thread extension - ONLY if beyond-border exploration is enabled,
  with the boundary-spanning policy fixed first.

---

## 11. Open decisions (must close before implementation)

1. Exploration past the worldborder: allowed, or bounded to a pregen'd region? (Decides
   whether Phase 3 exists.)
2. Server threading model: Folia/region-threaded vs standard Paper. (Decides how hard the
   boundary-spanning problem is.)
3. Marker encoding: sentinel block + block-entity NBT vs an Iris object whose palette encodes
   a structure id. How much intent to bake in (start pool + variant family) vs measure at
   sweep (conditional pieces). (See 4.a.)
4. Conflict-resolution window radius and priority ordering.
5. Boundary-spanning policy for Phase 3 (constrain footprint / hand-off / reject).
6. Where NBT templates and pool definitions live (datapack vs plugin resources).
7. anvil-api read interface alignment (Section 8 TODO).
8. Foundation/stilt synthesis: authored conditional pieces vs procedural fill-down.
9. Vertical buffer size for Y-banded overlap (minimum intervening rock between a surface
   structure floor and an underground structure ceiling). (See 7.1.)

---

## 12. Changelog

- 2026-06-07: Initial draft. Captured three-layer marker architecture, own-jigsaw-assembler
  (match-not-invoke) decision, vanilla-jigsaw verticality rationale, richer-than-NBT context
  (biome + land shape), surface/underground mapping, conditional runtime path, idempotency/
  determinism model, anvil-api notes, plugin boundary, phasing, and open decisions.
- 2026-06-07: PAUSED. User is deferring this work to Iris 4.0, which is expected to cover
  all currently-foreseen use cases (true underground/vertical jigsaw, biome targeting).
  No implementation started; spec retained for reference only.
- 2026-06-07: Added 7.1 (chunk consumption / Y-banded overlap guard): visited vs used states,
  reserve-the-volume rule, Y-banded keys to allow legitimate surface/underground stacking
  while preventing real overlap, deterministic claim order, and cross-session reconstruction.
  Added open decision 9 (vertical buffer size). Pause status unchanged - refinement only.
