# Biome Additions - Compiled Execution Sequence

Date: 2026-06-06
Status: IMPLEMENTED (Option B, batch) - 2026-06-06
Effective Issue: "work on biome additions e.g. Frostpeak and more"

Implementation note (2026-06-06): Per user direction (option B, author new from the
backups; new spire objects; batch; vanilla/region prerequisite already verified), all 5
volcano families (parent + -lava + -springs) and the 6 new biomes were authored into the
live `iris/pack-overlay/biomes/`, the `clutter/magmaspire1-3` spire objects were created
(`iris/scripts/build-magmaspires.py` -> `object-src/clutter/*.json` -> `pack-overlay/objects/clutter/*.iob`),
all biomes were wired into `regions/{frozen,tropical,hot,tundra,terralost}.json`, the pack
was deployed via `deploy-iris-pack.ps1`, and validation passed: `check-all-region-biomes.py`
(all refs valid), object-ref resolution (all 18 resolve to `.iob`), and JSON parse of all 21
new files. Remaining optional refinement: switch Frostfire Caves to Frostpeak `carvingBiome`.

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

### Phase 0 - Prerequisites and decisions (no file changes) - DONE 2026-06-07
- [x] 0.1 Confirm scope option (A/B/C) and the target biome list with the user. -- User chose **Option B** (author new from the backups), **batch** all at once; target list = all 11 biomes (5 volcano families + 6 new biomes).
- [x] 0.2 Decide object strategy for volcano spires: reuse `structures/landspike1-3`
      (zero new assets) OR create `clutter/magmaspire*.iob` (extra work). -- User chose **new objects**; `clutter/magmaspire1-3` generated by `iris/scripts/build-magmaspires.py`.
- [x] 0.3 Confirm the distribution-plan prerequisite status: are `vanilla/*` files and the
      rebuilt region `landBiomes` already loading without Iris silent-drop errors? -- User confirmed **verified** ("yes it's verified").

### Phase 1 - Frostpeak pilot (single biome, end to end) - DONE 2026-06-07
- [x] 1.1 `iris/pack-overlay/biomes/frozen/frostpeak.json` authored (FROZEN_PEAKS / BADLANDS derivatives).
- [x] 1.2 `frozen/frostpeak-lava.json` (caldera) and `frozen/frostpeak-springs.json` (Hot Springs) authored.
- [x] 1.3 Object gap resolved: new `clutter/magmaspire1-3` `.iob` objects generated; all `place` paths resolve (object-ref check: all 18 resolve).
- [x] 1.4 `frozen/frostpeak` wired into `regions/frozen.json` `landBiomes`; children pulled via parent `children`.
- [x] 1.5 ADR-003 conformance verified (derivative pair, palette, silhouette, concept hook in CUSTOM-BIOMES.md).
- [x] 1.6 Deployed via `deploy-iris-pack.ps1` (25 files patched, no errors); `check-all-region-biomes.py` all refs valid.
- [ ] 1.7 In-world validate (requires user-triggered server boot - see Phase 4.3).

### Phase 2 - Remaining volcano family - DONE 2026-06-07
- [x] 2.1 Cinderfall (`tropical/cinderfall*`) -> `regions/tropical.json`.
- [x] 2.2 Ashcrown (`mountain/ashcrown*`) -> `regions/tundra.json`.
- [x] 2.3 Embertide (`ocean/embertide*`) -> `regions/tropical.json`.
- [x] 2.4 Cinnabar Mesa (`mesa/cinnabar-mesa*` + `cinnabar-springs`) -> `regions/hot.json`.
- [ ] 2.5 Switch Frostfire Caves to Frostpeak `carvingBiome` - deferred as optional refinement (documented in CUSTOM-BIOMES.md).

### Phase 3 - The six "new" biomes - DONE 2026-06-07
- [x] 3.1 Boreal Shield (`frozen/boreal-shield.json`) -> `regions/frozen.json`.
- [x] 3.2 Ashen Plains (`savanna/ashen-plains.json`) -> `regions/hot.json`.
- [x] 3.3 Bryce Spires (`mesa/bryce-spires.json`) -> `regions/hot.json`.
- [x] 3.4 Maple Forest (`tundra/maple-forest.json`) -> `regions/tundra.json`.
- [x] 3.5 Floating Islands (`mountain/floating-islands.json`) -> `regions/terralost.json`.
- [x] 3.6 Mirage Isles (`ocean/mirage-isles.json`) -> `regions/tropical.json` `shoreBiomes`.

### Phase 4 - Finalize
- [x] 4.1 Update status tables in `docs/design/CUSTOM-BIOMES.md` and the two scratch plans to
      reflect ACTUAL live-overlay state (replace inaccurate "Done" with verified status).
- [x] 4.2 Record any incidental issues in `docs/scratch/POTENTIAL_BUGS.md` (e.g. the
      magmaspire object gap, backup-vs-live divergence).
- [ ] 4.3 Final deploy (done) + in-world smoke test across all touched regions (requires user-triggered server boot).

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
