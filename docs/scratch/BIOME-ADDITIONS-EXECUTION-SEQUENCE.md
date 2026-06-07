# Biome Additions - Compiled Execution Sequence

Date: 2026-06-06
Status: Draft - awaiting approval (Rule D-005)
Effective Issue: "work on biome additions e.g. Frostpeak and more"

This document reconciles the existing biome docs into one ordered, verifiable execution
sequence. It is a planning deliverable; no biome files are created/edited until the
approval gate at the bottom is cleared.

---

## Source docs read (where the biome docs live)

- `docs/design/CUSTOM-BIOMES.md` - canonical catalog: 5 volcano variants (incl. Frostpeak),
  6 new biomes, cave biomes, schema, palettes, climate vector.
- `docs/design/IRIS-BIOME-RESEARCH.md` - preexisting Iris biomes vs ADR-003; concept mapping.
- `docs/design/BIOMES.md` - vanilla biome ID reference.
- `docs/design/adr/ADR-003-custom-biome-design-goals.md` - the 5 acceptance goals every
  custom biome must satisfy.
- `docs/world-design/IRIS-WORLD-BUILDING.md` - authoring process (feeling -> spec -> folder +
  derivative decisions -> incremental JSON build -> tune rarity in-world).
- `docs/scratch/BIOME-ADDITIONS-PLAN.md` - the 6 new biomes + per-volcano Hot Springs system
  (checklist marked done, but see "Reality check" below).
- `docs/scratch/BIOME-DISTRIBUTION-PLAN.md` - tiering (T1 vanilla 50-80% ... T4 rare Iris) and
  the vanilla/ library + region-rebuild prerequisite.

---

## Reality check (current repo state vs docs)

1. The docs/checklists claim Frostpeak and the other custom biomes are "Done", but those
   files exist only in `iris/pack-overlay-backup/` and `iris/pack-overlay-backup-20260605-213215/`.
   They are NOT in the live `iris/pack-overlay/biomes/`.
2. Live `iris/pack-overlay/biomes/` currently has: `carving/`, `magnetics/`, `ocean/`,
   `prismatics/`, `swamp/`, `tropical/volcanic-plains.json`, and a large `vanilla/` set.
   No `frozen/`, `savanna/`, `mesa/`, `mountain/`, `tundra/`, custom `tropical/cinderfall*`.
3. Live overlay conventions (must match when porting):
   - `name` = "category/file" string (e.g. "vanilla/snowy_taiga"); NOT a display name.
   - `derivative` / `vanillaDerivative` use Iris enum form (e.g. `SNOWY_TAIGA`, `BADLANDS`,
     `FROZEN_PEAKS`) - NOT `minecraft:...`. The backup `frostpeak.json` already uses enum
     form, so porting is low-friction.
   - Regions wire biomes via `landBiomes` (and volcano children via the parent biome's
     `children` + `childStyle`). Live `regions/frozen.json` lists `vanilla/*` + base
     `frozen/*` but NO `frozen/frostpeak`.
4. OBJECT GAP (blocker for volcano biomes): backup volcano biomes reference
   `clutter/magmaspire1-3`, which do NOT exist anywhere in `iris/` (pack-base, output, or
   staging). The live `tropical/volcanic-plains.json` instead uses `structures/landspike1-3`
   (these DO exist in pack-base). Ported volcano biomes must either reuse
   `structures/landspike*` or have magmaspire `.iob` objects created/sourced first.
5. Frostfire Caves note (CUSTOM-BIOMES.md line ~222): once Frostpeak is live, switch
   `magnetics/frostfire-caves` from the `frozen` region `caveBiomes` to Frostpeak's
   `carvingBiome` field.

---

## Scope options (pick one before implementation)

- Option A (recommended): Port the documented custom biomes from the backups into the live
  `iris/pack-overlay`, adapt to live conventions, fix the object gap, wire into live
  `regions/*.json`, deploy and validate. Start with Frostpeak as the pilot, then the rest.
- Option B: Author brand-new biome(s) from scratch per CUSTOM-BIOMES.md (specify which).
- Option C: A specific subset (e.g. only the frozen family: Frostpeak + Boreal Shield).

The sequence below assumes Option A, Frostpeak-first.

---

## Execution sequence (Option A)

### Phase 0 - Prerequisites and decisions (no file changes)
- [ ] 0.1 Confirm scope option (A/B/C) and the target biome list with the user.
- [ ] 0.2 Decide object strategy for volcano spires: reuse `structures/landspike1-3`
      (zero new assets) OR create `clutter/magmaspire*.iob` (extra work). Recommend reuse.
- [ ] 0.3 Confirm the distribution-plan prerequisite status: are `vanilla/*` files and the
      rebuilt region `landBiomes` already loading without Iris silent-drop errors?
      (BIOME-DISTRIBUTION-PLAN steps 1-2 marked done but "verify in-world" is unchecked.)

### Phase 1 - Frostpeak pilot (single biome, end to end)
- [ ] 1.1 Create `iris/pack-overlay/biomes/frozen/frostpeak.json` (port from backup; set
      `name` to "frozen/frostpeak"; keep enum derivatives `FROZEN_PEAKS` / `BADLANDS`).
- [ ] 1.2 Create `iris/pack-overlay/biomes/frozen/frostpeak-lava.json` (caldera child) and
      `iris/pack-overlay/biomes/frozen/frostpeak-springs.json` (Hot Springs child) from backups.
- [ ] 1.3 Resolve the object gap per 0.2 (swap `clutter/magmaspire*` -> `structures/landspike*`
      or add the `.iob` objects). Confirm every `place` path resolves in staging.
- [ ] 1.4 Wire `frozen/frostpeak` into `iris/pack-overlay/regions/frozen.json` `landBiomes`
      (rarity-20 family => one slot; T3 per distribution plan). Children are pulled in via
      the parent biome's `children`, not the region list.
- [ ] 1.5 ADR-003 conformance check on the 3 files (derivative pair, 2-4 block palette,
      silhouette, build-theme anchor, one-line concept hook already in CUSTOM-BIOMES.md).
- [ ] 1.6 Deploy: run `iris/scripts/deploy-iris-pack.ps1`; confirm staging builds with no
      "biome not found" / "object not found" errors in the server log.
- [ ] 1.7 In-world validate: `/iris tp` / locate the frozen region, confirm Frostpeak
      generates with cone shape, palette, caldera child, and springs child.

### Phase 2 - Remaining volcano family (repeat 1.1-1.7 per variant)
- [ ] 2.1 Cinderfall (`tropical/cinderfall.json` + `-lava` + `-springs`) -> tropical region.
- [ ] 2.2 Ashcrown (`mountain/ashcrown.json` + `-lava` + `-springs`) -> mountain/tundra region.
- [ ] 2.3 Embertide (`ocean/embertide.json` + `-lava` + `-springs`) -> ocean region.
- [ ] 2.4 Cinnabar Mesa (`mesa/cinnabar-mesa.json` + `-lava` + `cinnabar-springs`) -> hot/terralost.
- [ ] 2.5 After all volcanoes live, switch Frostfire Caves to Frostpeak `carvingBiome`
      (CUSTOM-BIOMES.md note) and update that doc line.

### Phase 3 - The six "new" biomes (low to high effort, per BIOME-ADDITIONS-PLAN)
- [ ] 3.1 Boreal Shield (`frozen/boreal-shield.json`) -> frozen region (T2).
- [ ] 3.2 Ashen Plains (`savanna/ashen-plains.json`) -> hot/savanna region (T3).
- [ ] 3.3 Bryce Spires (`mesa/bryce-spires.json`) -> mesa/hot region (T3, generator-driven).
- [ ] 3.4 Maple Forest (`tundra/maple-forest.json`) -> child of `tundra/autumn` (T2/T3).
- [ ] 3.5 Floating Islands (`mountain/floating-islands.json`) -> mountain region (stretch).
- [ ] 3.6 Mirage Isles (`ocean/mirage-isles.json`) -> ocean shore (stretch).

### Phase 4 - Finalize
- [ ] 4.1 Update status tables in `docs/design/CUSTOM-BIOMES.md` and the two scratch plans to
      reflect ACTUAL live-overlay state (replace inaccurate "Done" with verified status).
- [ ] 4.2 Record any incidental issues in `docs/scratch/POTENTIAL_BUGS.md` (e.g. the
      magmaspire object gap, backup-vs-live divergence).
- [ ] 4.3 Final deploy + in-world smoke test across all touched regions.

---

## Per-biome backup-to-live mapping (Option A inventory)

| Biome | Backup source | Live target | Region wiring |
|---|---|---|---|
| Frostpeak (+lava,+springs) | `backup/biomes/frozen/frostpeak*.json` | `pack-overlay/biomes/frozen/` | `regions/frozen.json` |
| Cinderfall (+lava,+springs) | `backup/biomes/tropical/cinderfall*.json` | `pack-overlay/biomes/tropical/` | `regions/tropical.json` |
| Ashcrown (+lava,+springs) | `backup/biomes/mountain/ashcrown*.json` | `pack-overlay/biomes/mountain/` | `regions/tundra.json` / mountain |
| Embertide (+lava,+springs) | `backup/biomes/ocean/embertide*.json` | `pack-overlay/biomes/ocean/` | `regions/tropical.json` (ocean) |
| Cinnabar Mesa (+lava,+springs) | `backup/biomes/mesa/cinnabar*.json` | `pack-overlay/biomes/mesa/` | `regions/hot.json` / terralost |
| Boreal Shield | `backup/biomes/frozen/boreal-shield.json` | `pack-overlay/biomes/frozen/` | `regions/frozen.json` |
| Ashen Plains | `backup/biomes/savanna/ashen-plains.json` | `pack-overlay/biomes/savanna/` | `regions/hot.json` |
| Bryce Spires | `backup/biomes/mesa/bryce-spires.json` | `pack-overlay/biomes/mesa/` | `regions/hot.json` |
| Maple Forest | `backup/biomes/tundra/maple-forest.json` | `pack-overlay/biomes/tundra/` | `regions/tundra.json` (child of autumn) |
| Floating Islands | `backup/biomes/mountain/floating-islands.json` | `pack-overlay/biomes/mountain/` | mountain region |
| Mirage Isles | `backup/biomes/ocean/mirage-isles.json` | `pack-overlay/biomes/ocean/` | ocean shore |

Note: do NOT copy the backup's `biomes/regions/*.json` over the live `regions/*.json` - the
live region files are the working, restructured set; only add biome entries to them.

---

## Rule D-005 - Approval gate (decisions needed before implementation)

1. Scope option: A (port from backups), B (new from scratch), or C (subset)? If C, which biomes?
2. Volcano spire objects: reuse existing `structures/landspike1-3`, or invest in new
   `clutter/magmaspire*.iob` assets?
3. Pilot-first (Frostpeak alone, deploy + validate, then proceed) vs batch all at once?
4. Is the vanilla/ + region-rebuild prerequisite (BIOME-DISTRIBUTION-PLAN) already verified
   in-world, or should that verification be part of this work?
