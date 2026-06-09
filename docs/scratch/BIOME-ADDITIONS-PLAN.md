# Biome Additions and Cleanup Plan

Date: 2026-06-05
Status: IMPLEMENTED 2026-06-07 - all listed new biomes + per-volcano Hot Springs children are live in `iris/pack-overlay/biomes/` and wired into region `landBiomes`/`shoreBiomes`; deployed and reference-validated. See `docs/scratch/BIOME-ADDITIONS-EXECUTION-SEQUENCE.md`. In-world smoke test pending a user-triggered server boot.

---

## Summary

This plan does three things:

1. **Removes duplicates** - entries in CUSTOM-BIOMES.md that are already covered by the Iris
   overworld set (adopt-as-is) or that require custom mobs/mechanics beyond vanilla.
2. **Adds new biomes** identified from player sentiment research across Terralith, BYG, and
   Biomes O' Plenty - concepts players specifically name and seek out.
3. **Integrates Hot Springs** as a per-volcano child biome system rather than a standalone
   entry, so each volcano variant has a geothermal transition zone on its lower slopes.

---

## Part 1: Removals from CUSTOM-BIOMES.md

These entries are cut. Reason is given for each.

| Entry | Reason |
|---|---|
| `Geothermal Sulfur Caves` | Requires custom mob (Sulfur Cubes) - not vanilla-achievable |
| `Abyssal Ocean Trench` | Requires 3 custom mobs (Deepworms, Abyssal Clams, The Gleaming) - not vanilla-achievable |
| `Lush Desert` | Fully covered by Iris `terralost/ancient-sands` (sphinx ruins, pyramid jigsaw, adopt as-is) |
| `Canopied Rainforest` | Fully covered by Iris `tropical/rainforest` variants (adopt with largegeneric trim) |
| `Yellowstone Cave / Hot Spring Surface Biome` (standalone) | Replaced by the per-volcano Hot Springs child system in Part 3 below; the color/palette data from this entry is preserved there |

**What to do in CUSTOM-BIOMES.md:**
- Remove the 5 sections above entirely.
- Add a note under each volcano entry pointing to the Hot Springs child biome.
- Add the 6 new biome entries from Part 2.

---

## Part 2: New Biome Additions

Six new biomes drawn from player sentiment research. Ordered by effort (low to high).

---

### 1. Maple Forest

**Source signal:** BYG's most-cited "cozy" biome; players specifically name it in favorite-biome
threads. The Iris `tundra/autumn` biome exists but uses redwood-scale trees - it is not the
same concept. A human-scale deciduous autumn forest is missing from both the Iris set and the
current catalog.

- **Concept hook:** A cathedral of orange, red, and gold - the obvious site for a harvest
  festival village, a treehouse inn, or a cider mill.
- **Iris category:** `tundra` (child of `tundra/autumn` or standalone sibling)
- **Derivative:** `minecraft:birch_forest`
- **vanillaDerivative:** `minecraft:birch_forest`
- **Climate:** Temp 0.2-0.5, Humidity 0.6, moderate continentalness
- **Terrain:** Gently rolling hills, no dramatic elevation changes - build-friendly surface
- **Primary palette:** Podzol (surface), coarse dirt (layer 2), dirt (layers 3-5)
- **Tree objects:** Our generated oak/birch variants h8-h22 at standard rates (0.04-0.06);
  use `secondary_leaves` with orange/red/yellow tones (birch_leaves + oak_leaves mix);
  no trees above h25 - this is a harvest biome, not an atmosphere biome
- **Accent objects:** Pumpkins at 0.002, mushrooms at 0.01, fallen log clutter at 0.005
- **Build theme anchor:** Harvest village - warm wood tones, lanterns, market stalls
- **Rarity:** 3
- **Transition:** Add as child of `tundra/autumn` with NOWHERE_SIMPLEX zoom 5, so maple
  patches appear inside the larger autumn zone

---

### 2. Boreal Shield

**Source signal:** Terralith's Shield biome is praised specifically by builders for being flat,
open, and texturally interesting. Nothing in the Iris set matches - mountain plains is grassy,
not exposed stone.

- **Concept hook:** Ancient exposed granite swept clean by glaciers - flat enough to build
  anything, interesting enough to make it worth it. Frontier outpost, stone hall, mining camp.
- **Iris category:** `frozen` or `mountain` (flat variant, low erosion)
- **Derivative:** `minecraft:snowy_taiga`
- **vanillaDerivative:** `minecraft:snowy_taiga`
- **Climate:** Temp -0.2 to 0.2, Humidity 0.4, moderate continentalness, low erosion
- **Terrain:** Flat to gently rolling; exposed stone surface with shallow soil pockets
- **Primary palette:** Granite (surface, 60%), stone (surface, 30%), podzol (surface, 10%
  in soil pockets); gravel (layer 2); stone (layers 3+)
- **Tree objects:** Sparse spruce h8-h14 at 0.015; lichen clutter at 0.05; boulder objects
  at 0.003
- **Accent objects:** Exposed ore veins (iron/copper) as surface clutter at 0.002
- **Build theme anchor:** Stone frontier - granite walls, iron accents, low-profile builds
  that read against the flat horizon
- **Rarity:** 4

---

### 3. Ashen Plains

**Source signal:** Terralith's Ashen Savanna is cited as "eerie and memorable"; Volcanic Peaks
also praised. The Iris tropical set has volcanic terrain but it is underwater or lava-dominant.
A walkable ash plain with sparse dead trees is missing. This also serves as the outer
transition zone for all five volcano variants (see Part 3).

- **Concept hook:** Grey ash drifts over a landscape that was alive not long ago - dead oaks
  still standing, lava seeps cooling at the edges. Ruined settlement, forge district,
  post-eruption outpost.
- **Iris category:** `savanna` (hot, dry, flat - matches ash plain climate)
- **Derivative:** `minecraft:basalt_deltas`
- **vanillaDerivative:** `minecraft:savanna`
- **Climate:** Temp 1.5-2.0, Humidity 0.1, moderate continentalness
- **Terrain:** Flat to gently rolling; occasional shallow lava seep depressions
- **Primary palette:** Basalt (surface, 50%), gray terracotta (surface, 30%), gravel
  (surface, 20%); tuff (layer 2); andesite (layers 3-8)
- **Tree objects:** Dead oak (bare oak_log columns, no leaves) at 0.008; basalt spire
  clutter at 0.004
- **Accent objects:** Magma block patches at 0.002 (lava seeps); ash pile clutter (gravel
  mounds) at 0.01
- **Build theme anchor:** Post-eruption settlement - blackened stone, forge aesthetic,
  survival-era ruins
- **Rarity:** 5
- **Transition role:** Used as the outer child biome for all 5 volcano variants (see Part 3)

---

### 4. Bryce Spires

**Source signal:** Terralith's most-screenshotted terrain type; players call it "alien". The
Iris mesa set has flat plateaus and cliffs but no spire terrain. This is a terrain generator
addition - no new objects needed, the mesa palette already exists.

- **Concept hook:** Thin terracotta needles rising 20-40 blocks from a canyon floor - cliff
  dwellings, a desert monastery, or a canyon market built between the spires.
- **Iris category:** `mesa`
- **Derivative:** `minecraft:badlands`
- **vanillaDerivative:** `minecraft:badlands`
- **Climate:** Temp 2.0, Humidity 0.0, moderate continentalness, high erosion
- **Terrain:** Canyon floor at Y=50-60 with spire formations rising to Y=80-100; generator
  uses high-frequency noise with narrow peaks (spike profile, not plateau profile)
- **Primary palette:** Orange terracotta (surface), red terracotta (layer 2), yellow
  terracotta (layer 3), white terracotta (layer 4), terracotta (layers 5+); red sand on
  canyon floor
- **Tree objects:** None - spire terrain is the feature
- **Accent objects:** Dead bush at 0.03; cactus at 0.01; bone block clutter at 0.002
- **Build theme anchor:** Canyon cliff dwellings - terracotta carved into spire faces,
  rope bridges between needles, market at canyon floor
- **Rarity:** 4
- **Implementation note:** This is primarily a generator config change. Add as a new entry
  in the mesa category using a spike/needle noise profile. The Iris `generators/` folder
  has canyon-steep.json as a reference starting point.

---

### 5. Floating Islands (Skylands)

**Source signal:** Terralith's Skylands are consistently in player top-5 lists; 4 seasonal
variants keep it fresh. Iris supports floating island terrain via generator config. High
effort but highest wow factor of any biome type.

- **Concept hook:** Small landmasses drifting above cloud level - a sky fortress, an airship
  dock, a wizard tower that can only be reached by climbing or flying.
- **Iris category:** `mountain` (high continentalness, extreme altitude)
- **Derivative:** `minecraft:the_end` (gives eerie sky color and ambient sound on client)
  or `minecraft:windswept_hills` for a more grounded feel
- **vanillaDerivative:** `minecraft:windswept_hills`
- **Climate:** Any temp, low humidity, very high continentalness, extreme erosion
- **Terrain:** Floating island generator - inverted noise profile creates islands suspended
  at Y=150-200 with void below; each island 20-60 blocks wide
- **Primary palette:** Grass (surface), dirt (layers 2-3), stone (layers 4+); moss patches
  on undersides
- **Tree objects:** Sparse oak h6-h12 at 0.02; hanging vines at 0.05
- **Build theme anchor:** Sky architecture - the constraint of limited flat space forces
  vertical builds; natural anchor for airship/magic server lore
- **Rarity:** 8 (rare enough to feel like a discovery, findable in a session)
- **Status:** Stretch goal - implement after surface set is stable. Floating island
  generators are the trickiest terrain type in Iris; test in isolation first.

---

### 6. Mirage Isles

**Source signal:** Terralith's Mirage Isles are cited as a "rare ocean discovery"; players
post screenshots when they find one. Iris has beach biomes but nothing with this rarity tier
or palette.

- **Concept hook:** A white-sand island with turquoise water so clear you can see the bottom
  - a resort, a lighthouse, a smuggler's cove. Rare enough that finding one feels like luck.
- **Iris category:** `ocean` (low continentalness, shore zone)
- **Derivative:** `minecraft:beach`
- **vanillaDerivative:** `minecraft:beach`
- **Climate:** Temp 1.5-2.0, Humidity 0.3, low continentalness (ocean/shore boundary)
- **Terrain:** Small flat island, max Y=68-72; surrounded by shallow warm ocean
- **Primary palette:** White concrete powder (surface) or calcite; sand (layer 2);
  sandstone (layers 3+)
- **Water color:** `#00E5FF` (vivid turquoise - the defining visual)
- **Tree objects:** Palm (beach/palm objects from Iris tropical set) at 0.4; flowering
  bush clutter at 0.02
- **Build theme anchor:** Island getaway - open-air structures, docks, lighthouse
- **Rarity:** 9 (near-rare; players should find one per multi-hour session if they explore
  ocean)
- **Status:** Stretch goal - shore biomes are tricky continentalness edge cases in Iris.
  Implement after Bryce Spires and Boreal Shield are stable.

---

## Part 3: Hot Springs as Per-Volcano Child Biome System

Rather than a standalone biome, Hot Springs generates as a child of each volcano variant on
its lower slopes. This creates a natural gradient: outer biome -> ashen plains -> hot springs
-> volcano slopes -> caldera. Players walk into the geothermal zone before reaching the
volcano itself.

### Shared Hot Springs design

All five variants share the same core palette and mechanics; only the surrounding context
and derivative differ.

- **Concept hook:** Steaming prismatic pools in a pine-and-calcite landscape - bathhouse,
  ranger outpost, geothermal settlement. The pools glow; the steam rises; it feels alive.
- **Terrain:** Flat to gently rolling; shallow bowl depressions hold the spring pools
- **Primary palette:** Calcite (surface, 60%), white terracotta (surface, 30%), coarse
  dirt (surface, 10%); tuff (layer 2); stone (layers 3+)
- **Water color:** `#77E0F7` (glowing saturated turquoise - from the existing Yellowstone
  entry)
- **Pool objects:** Water source blocks in shallow calcite bowls; blue ice beneath water
  for the glow effect; bubble column sources (soul sand beneath) for steam visual
- **Tree objects:** Sparse dead spruce or pine h8-h14 at 0.01 - sparse, not a forest
- **Accent objects:** Calcite boulder clutter at 0.008; sulfur-yellow concrete powder
  patches at 0.003 (mineral deposits)
- **childStyle:** `NOWHERE_SIMPLEX`, zoom 4 - medium patches on lower volcano slopes

### Per-volcano Hot Springs variants

| Volcano | Hot Springs file | Derivative | Context note |
|---|---|---|---|
| Cinderfall (jungle) | `tropical/cinderfall-springs.json` | `minecraft:jungle` | Jungle-edge springs; lush ferns mix with calcite pools |
| Frostpeak (frozen) | `frozen/frostpeak-springs.json` | `minecraft:snowy_taiga` | Ice-rimmed pools; steam contrast against snow is the visual |
| Ashcrown (mountain) | `mountain/ashcrown-springs.json` | `minecraft:old_growth_pine_taiga` | Classic Yellowstone feel; pine forest + prismatic pools |
| Embertide (ocean island) | `ocean/embertide-springs.json` | `minecraft:beach` | Coastal springs; pools drain toward the ocean shore |
| Cinnabar Mesa (mesa) | `mesa/cinnabar-springs.json` | `minecraft:badlands` | Terracotta-rimmed pools; mineral-rich orange/white contrast |

### Full volcano transition chain

```
[outer biome] (e.g. tropical/rainforest, frozen/pine-hills, mesa/red)
  -> child: savanna/ashen-plains (Ashen Plains - Part 2 #3)
      -> child: <volcano>-springs (Hot Springs variant above)
          -> child: <volcano>.json (the volcano cone)
              -> child: <volcano>-lava.json (caldera - existing design)
```

childStyle zoom values: ashen-plains zoom 6 (large outer ring), springs zoom 4 (medium
mid-ring), volcano zoom 2 (tight cone), lava zoom 0.2 (caldera cap - existing).

---

## Part 4: Implementation Order

### Phase 1 - Cleanup (edit CUSTOM-BIOMES.md only, no new files)
- [x] Remove Geothermal Sulfur Caves section
- [x] Remove Abyssal Ocean Trench section
- [x] Remove Lush Desert section (add note: covered by terralost/ancient-sands)
- [x] Remove Canopied Rainforest section (add note: covered by tropical/rainforest)
- [x] Remove standalone Yellowstone section (add note: see Hot Springs child system)
- [x] Add Hot Springs child system section to each volcano entry

### Phase 2 - Low-effort new biomes (JSON only, no new schematics)
- [x] Boreal Shield - `iris-biomes/frozen/boreal-shield.json`
- [x] Ashen Plains - `iris-biomes/savanna/ashen-plains.json`
- [x] Bryce Spires - `iris-biomes/mesa/bryce-spires.json` (uses canyon-steep + mountain generators)

### Phase 3 - Medium-effort new biomes (JSON + our generated .iob trees)
- [x] Maple Forest - `iris-biomes/tundra/maple-forest.json`; added as child of autumn.json + autumn-extended.json in live pack
- [x] Hot Springs x5 - cinderfall-springs, frostpeak-springs, ashcrown-springs, embertide-springs, cinnabar-springs all created in matching category folders

### Phase 4 - Stretch goals (after surface set is stable)
- [x] Floating Islands - generator research + new JSON in `mountain/`; copied to live pack
- [x] Mirage Isles - shore continentalness tuning + new JSON in `ocean/`; copied to live pack

---

## Duplicate/Coverage Reference

For the catalog, these concepts are now covered by Iris adopt-as-is biomes and do not need
custom entries:

| Concept | Covered by |
|---|---|
| Lush Desert / Ancient Sands | `terralost/ancient-sands` |
| Canopied Rainforest | `tropical/rainforest` + `rainforest-hills` (with largegeneric trim) |
| Redwood Forest | `tundra/redwood-forest` (with tredwood trim) |
| Bonsai Forest | `tundra/bonsai-forest` |
| Autumn Forest (redwood-scale) | `tundra/autumn` |
| Amethyst Canyon | `terralost/amethyst-canyon` |
| Amethyst Rainforest | `terralost/amethyst-rainforest` |
| Lavender/Violet Forest | `temperate/osaka-violet-forest` |
| Haunted Swamp | `swamp/handy-willow-forest` + `swamp/cambian-drift` |
| Crystal Caves | Iris cave set covers this |
