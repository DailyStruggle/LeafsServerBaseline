# Iris End: What Players Respond Best To

Research companion to [`IRIS-END-PICKS.md`](IRIS-END-PICKS.md),
[`IRIS-END-FEATURE-IDEAS.md`](IRIS-END-FEATURE-IDEAS.md), and
[`IRIS-END-GENERATION.md`](IRIS-END-GENERATION.md). Those cover *what* an Iris End can do
and which features we would pick. This doc captures *what players actually want* from End
generation, drawn from community feedback (official Minecraft Feedback site, r/Minecraft,
r/minecraftsuggestions) and the reception of popular End-overhaul mods/datapacks
(BetterEnd, The Outer End, Endercon). It is meant to keep our authoring choices aligned
with player taste rather than novelty for its own sake.

> Scope note: research/opinion synthesis only. Nothing here is implemented. Use it as the
> "design intent" filter when choosing biomes/structures/atmosphere for an Iris End.

---

## The core tension (read first)

Player feedback splits into two camps that must both be respected:

1. **"The End is boring and barely explorable."** The dominant complaint: empty islands,
   almost no structures, one mob, nothing to find after the dragon. These players want
   more biomes, structures, caves, and endgame rewards.
2. **"Do not turn the End into Nether 2.0."** A strong, vocal counter-camp insists the
   End's value is its **eerie, lonely, alien, barren** atmosphere. Overgrowing it with
   lush forests, dense mobs, and constant points of interest is the most-rejected outcome.

The pattern that satisfies both: **add variety and reward without killing the loneliness.**
Keep emptiness as a deliberate design material, not an accident.

---

## What players respond best to (ranked by signal strength)

### 1. Explorable variety in the OUTER islands, sparse and surreal
- More outer-island biomes that feel **alien/surreal**, not Earth-like (crystal spires,
  chorus expanses, dust/void wastes, glowing fields) - high approval.
- Variety in **island shape and height**, not uniform blobs - consistently praised about
  BetterEnd.
- Keep them **spread out** with real void between islands; the travel/isolation is the
  point, not a flaw.
- Iris hook: `floatingChildBiomes` shape fields + 4-6 `biomes/end/*` with End
  `derivative`s (see picks doc P1-P2).

### 2. Reasons to go (loot + structures), but rare and earned
- Players want **destinations and endgame loot** beyond End cities - new structures with
  meaningful rewards rank highly.
- Reception is best when structures are **rare and rewarding**, not dense - frequent
  structures erase the "frontier" feel.
- Iris hook: jigsaw `IrisStructure`s with floating place-modes and tuned
  `spacing`/`separation` (picks doc P4).

### 3. Atmosphere over content
- Per-biome **fog/sky/ambience** and the constant dim End light are repeatedly cited as
  what makes the End feel special. Atmosphere changes are higher-value-per-effort than
  more blocks.
- Keep it **quiet**: minimal/no ambient music spam, sparse mobs. Silence is a feature.
- Iris hook: pick `derivative`/`vanillaDerivative` on real End biomes for correct
  fog/sky; `fixedTime`/`ambientLight` for static ambience.

### 4. Internal caves / hidden interiors in islands
- "Caves inside the islands" is a popular, well-received idea - rewards close inspection
  without cluttering the surface.
- Iris hook: `carveStyle`/`carving`/`carveThreshold` on floating biomes + End-scoped
  `ores`/`deposits`.

### 5. Keep the central island stark
- Strong preference to leave the **central/spawn island bleak and ceremonial** (dragon
  arena feel). Players want the journey *outward* to escalate, not a busy hub at spawn.
- Iris hook: a dedicated `end-central` region kept deliberately plain; richness begins in
  the outer field.

---

## What players react badly to (avoid)

- **Overgrowth / "lush" End** - turning it green/forested or Nether-dense. Most-rejected.
- **Too many mobs** - the End's emptiness is valued; dense hostile spawns break it.
- **Structure spam** - points of interest every few hundred blocks kill exploration value.
- **Earth-like biomes** - players want *alien*, not transplanted overworld.
- **Losing the dragon/endgame ceremony** - the central fight and its finality are liked;
  do not trivialise spawn.
- **Reworking the central island into a theme park** - keep it austere.

---

## Concrete content menu (satisfies both complaints)

The way to answer *both* camps at once: add variety that is **on-theme barren** plus a
distinct **"dead future-tech"** thread (the only structure flavour vanilla already proved
players love - End cities/ships). This keeps the End alien and lonely while giving
explorers destinations and loot.

### A. Barren-theme biomes (sparse, alien, low-flora)
Author under `biomes/end/*`, each an `IrisFloatingChildBiomes.biome` entry with its own
`rarity`/`footprintStyle`/altitude band. All keep an End `derivative`/`vanillaDerivative`
so client sky/fog stays correct.

| Biome idea | Palette (vanilla blocks) | Iris hooks |
|---|---|---|
| **Pale dust flats** (the "white sand" look - see note) | calcite + smooth quartz + white concrete powder accents, end stone base | flat generator, `end_barrens` derivative; connective filler |
| **Bleached spires / bone reefs** | bone block + calcite + end stone | aggressive `topShapeAmp`/`topShapeStyle`; tall jagged tops |
| **Obsidian/blackstone scree** | end stone + blackstone + obsidian flecks | low relief, dark counterpoint to pale flats |
| **Cracked end barrens** | end stone + cracked/ chiseled variants, deepslate accents | `carveStyle` for fissures, sparse |
| **Void shelf wastes** | thin end stone shelves over void | `maxThickness` low, `bottomDepthMax` dramatic underside |

Keep flora minimal: scattered chorus only, no grass/forests. Variety comes from
**shape, colour, and light**, not vegetation - this is what keeps the barren camp happy.

### B. "Dead future-tech" theme (the End-city/ship lineage)
Players already respond well to End cities/ships because they read as **abandoned alien
technology** - sterile purpur geometry, end rods as lights, shulkers, loot in a dead
civilisation. Lean into that as the End's signature structure identity rather than
medieval ruins. Author as Iris jigsaw `IrisStructure`s placed with floating
`ObjectPlaceMode` (`FLOATING`/`CEILING_HANG`); pipe loot via the structure `loot` field.

**Lore frame (drives the palette):** the End civilisation *escaped the Deep Dark
corruption*. Their tech is therefore the **antithesis** of the deep dark - clean, pale,
bright, sterile: **purpur, concrete (esp. white/light), quartz block, end stone bricks,
end rods, sea lanterns**. Deepslate/sculk/copper-decay read as the *corruption they fled*,
so they are NOT building materials. Use deepslate/sculk only sparingly as an **encroaching
corruption motif** (a creeping threat at ruin edges or in the void-boss arena), never as
the civilisation's own architecture.

**Lore: what broke them.** The End's near-vacuum / thin atmosphere offered no shielding,
so the civilisation fell to **bombardment (meteor/projectile impacts) and radiation** -
not war or decay. This explains *why* the pristine tech is ruined and seeds two new design
threads:

- **Impact damage (visual, worldgen-friendly).** Scar the barren biomes and ruins with
  craters and strike marks: bowl depressions blasted into islands, glass/tinted-glass
  domes shattered open, half-melted purpur/quartz, scorched rings, and **embedded
  meteorite material** (magma block, obsidian, basalt, raw-iron/copper nuggets in stone -
  basalt/magma is acceptable here as *impact debris*, distinct from the deep-dark
  corruption motif). Iris hooks: crater `carving`/`carveStyle`, `deposits` for meteorite
  pockets, and damaged structure variants in the jigsaw pools.
- **Radiation (gameplay hazard, runtime).** Lingering "hot zones" around the worst impact
  sites / reactor ruins apply a damage-over-time or wither/weakness effect, making those
  the highest-loot, highest-risk spots. There is no vanilla radiation block, so this is a
  companion-plugin effect-zone (Folia region scheduler), not Iris - Iris only marks the
  zone via structure/biome; the plugin applies the effect. Pairs naturally with the Tier 3
  void-boss as another risk-gated reward source.

Structure archetypes (rare, rewarding):
- **Derelict spires / broken towers** - purpur + end stone bricks + end rods, half
  collapsed (free-standing `FLOATING`).
- **Wrecked sky-ships** - the End-ship idea extended: hanging hulls moored under islands
  (`CEILING_HANG`), with salvage loot.
- **Dead beacon / power arrays** - rows of end rods and sea lanterns, broken redstone-less
  "machinery" suggested with quartz pillars, redstone lamps, chains, lightning rods.
- **Sealed vaults / data-spires** - quartz block + purpur + white concrete shells, sea
  lanterns/redstone lamps, trial-style loot rooms (use whatever vault/trial blocks the
  server's version exposes). Pristine and clean, not deepslate.
- **Shattered platforms / launch pads** - flat purpur decks scattered with void gaps;
  good `CONCENTRIC_RINGS` distribution to echo vanilla's outer ring.

Block vocabulary for the "tech" read (all vanilla, pale/clean to contrast the deep dark):
purpur, **white/light concrete**, **quartz block + smooth quartz**, end stone bricks, end
rods, sea lanterns, redstone lamps, chains, lightning rods, chiseled/cracked variants,
glass/tinted glass, calcite accents, shulker boxes for loot. Avoid deepslate/sculk/copper
in the civilisation's own builds - reserve those for the encroaching-corruption motif.

### B1. Iris mob tagging/decoration (verified capability)
Iris places entities through spawners (`IrisSpawner.initialSpawns`/`spawns` ->
`IrisEntitySpawn` -> `IrisEntity`) attached via `entitySpawners` on biome/region/dimension.
The `IrisEntity` definition can **tag and kit out** each mob directly, which is the
intended use here:

- **Equipment:** `helmet`/`chestplate`/`leggings`/`boots`/`mainHand`/`offHand` (each an
  `IrisLoot`, so they can carry enchantments, custom names, etc.).
- **Attributes:** `attributes` (`KList<IrisAttributeModifier>`) - max health, movement
  speed, attack damage, knockback resistance, etc.
- **Effects:** `spawnEffect` (`IrisEffect`) carries a `potionEffect` (+ strength and
  tick range) plus particle/sound aura. For a guaranteed persistent self-buff, also use
  `rawCommands` (e.g. `/effect give ...`) which run on spawn.
- **Flags / identity:** `customName`, `aware`, `ai`, `glowing`, `invulnerable`, `silent`,
  `baby`, `gravity`, `pickupItems`, on-death `loot`, `passengers`, leash, and
  `specialType` for custom-mob-plugin integration.

So armour, weapons, and effects on dressing mobs are pure Iris config - no plugin needed
for those. (Only things `IrisEntity` lacks a setter for - e.g. villager profession - need
the lectern trick or a small plugin; see below.)

### B2. Survivors: occasional villagers, always librarians
A few **villagers** appear as scattered survivors/descendants of the escaped civilisation -
rare, never crowds (the End stays lonely). Constraint: they must **always be librarians**,
because the librarian's white/light robe is the only profession skin that matches the pale
purpur/quartz/concrete colour theme; any other profession's colours would clash and break
the aesthetic. It also fits the lore - keepers of the civilisation's knowledge.

- **Lock the profession to librarian.** Force `VILLAGER` profession = `librarian` on spawn
  (and prevent re-profession drift). Thematically reinforce by pre-placing a **lectern** in
  their ruin nook so even vanilla profession logic settles them as librarians.
- **Keep them rare and quiet** - a lone librarian in a dead-tech ruin, not a village. This
  doubles as a soft loot/trade source (enchanted books) without adding crowds or breaking
  the barren feel.
- **Implementation split:** Iris *can* place the villager itself at generation time via an
  `IrisSpawner` with `initialSpawns` (an `IrisEntitySpawn` -> `IrisEntity`) referenced from
  the biome/region `entitySpawners`, so the lone survivor is worldgen, not natural spawning.
  `IrisEntity` supports custom name, AI/aware, equipment, passengers, baby, and loot, but
  exposes **no villager-profession setter**, so pinning the profession to *librarian* still
  needs the lectern trick and/or a small companion plugin (Folia region scheduler). Iris
  builds the ruin/lectern nook and spawns the villager; the plugin guarantees the profession
  lock. Consistent with the Folia/NMS boundary used for the void-boss and radiation zones.

### B3. Tag-and-ingest pattern (Iris marks, plugin enriches)
The clean way to get past `IrisEntity`'s gaps (no profession setter, no custom trades, no
boss AI) without coupling worldgen to runtime: have **Iris stamp a machine-readable marker
on each mob at spawn, and a companion plugin ingest that marker** to apply the further
improvements. Iris owns *placement + base kit*; the plugin owns *behaviour*.

How to stamp the marker with Iris (any/all of):
- **Scoreboard tag via `rawCommands`** on spawn, e.g. `tag add <entity> end:librarian` or
  `end:boss_minion` - the most robust, since `Entity.getScoreboardTags()` is trivial for a
  plugin to read and filter on. (Target the just-spawned entity with a tight selector,
  e.g. nearest within a 1-block radius of the spawn location.)
- **`customName`** as a sentinel string (simple, but visible unless name is hidden).
- **`specialType`** if we wire a custom-mob provider.

How the plugin ingests it:
- Listen for the entity entering the world (e.g. `EntityAddToWorldEvent` /
  `EntitiesLoadEvent`, Folia region scheduler), read the tag, then apply the enrichment:
  lock villager profession to **librarian** + bespoke trade list, attach persistent data to
  the entity's **PDC** (`PersistentDataContainer`) for durable state, swap in boss AI/phases,
  scale loot, etc.
- PDC keeps the enrichment durable across reloads; the scoreboard tag is just the spawn-time
  hand-off signal.

Why this is the right split: Iris places the mob deterministically *with the terrain/
structure* (generation-time, Folia-friendly) and pre-applies armour/weapons/attributes/
effects (section B1); the plugin only does the things Iris cannot express, keyed off the
tag. It generalises cleanly to the librarian survivors, dead-tech "guardian" mobs, and
later the void-boss and its minions.

### C. White sand - status and substitutes
**There is no "white sand" block in vanilla Minecraft.** Only generic `sand` (tan) and
`red_sand` exist; "white sand" is a long-standing community suggestion, never implemented.
To get the white-sand aesthetic, use vanilla substitutes:
- **Calcite** - closest matte white, non-gravity.
- **Smooth quartz / quartz block** - clean bright white.
- **White concrete powder** - gravity-affected (behaves like sand) for drifts/dunes; turns
  to concrete only on water contact (rare in the End, so it stays "sandy"-looking).
- **Diorite / polished diorite, bone block, white terracotta** - off-white accents.

Recommendation: use **calcite + white concrete powder** as the "pale dust flats" surface
(concrete powder gives the granular/dune behaviour, calcite the matte tone), with end
stone beneath. If the server ever adds a datapack/plugin white-sand block, swap the
palette - the biome structure stays the same.

### D. White concrete powder as a mineable End resource (economy angle)
Beyond aesthetics, surfacing **white concrete powder** in the pale-dust-flats biome makes
it a *directly mineable* resource - an End-only way to obtain sand/gravel-equivalent
material without strip-mining overworld beaches/rivers (which are slow, finite-feeling,
and unpopular to gather). This gives the barren biome a tangible reason to visit.

- Vanilla, white concrete powder is crafted (4 sand + 4 gravel + white dye), so mining it
  natively effectively "refunds" that sand+gravel+dye cost.
- With a **datapack recolour/decolour recipe** (e.g. wash or re-dye concrete powder to any
  of the 16 colours, or strip back toward a neutral feedstock), naturally-generated white
  concrete powder becomes a flexible base material: mine the End flats, then recolour to
  whatever the build needs. This sidesteps the sand/gravel gathering loop entirely.
- Design caution (keep the barren camp happy): meter it. Make it a *reason to explore the
  End*, not a free infinite quarry - bound it to specific biomes/altitudes and keep
  densities modest so the flats still read as desolate, not a resource farm.
- Iris hook: place it via the biome `layers`/`palette` (surface cap over end stone) and/or
  dimension `deposits`/`ores` for buried pockets; tune `maxPerChunk`/`minSize` to control
  yield. The recolour recipe itself is a `datapacks/` change, not an Iris pack change.

---

## High-use / low-availability resources worth sourcing in the End

Purpur is on-theme but cheap (chorus fruit -> popped chorus -> purpur is renewable and
trivial), so it adds little pull. The real draw is supplying materials players **need a
lot of but find scarce** in base Minecraft. Below: demand-vs-availability, and how the End
could supply each on-theme without trivialising other dimensions.

### Tier 1 - best fits (high demand, scarce, thematically End)
| Resource | Why high-use | Why scarce in vanilla | End hook |
|---|---|---|---|
| **Shulker shells** | Shulker boxes = the storage meta; everyone wants stacks | Only from shulkers in End cities; slow, RNG | More shulker-bearing structures (dead-tech vaults), or shell loot - the single biggest pull |
| **Ender pearls** | Ender chests, eyes of ender, end crystals, pearl-stasis, elytra travel | Enderman drops only; tedious early | The End is already enderman-rich; lean in (safe enderman biomes / pearl loot) |
| **Phantom membrane** | The *only* non-Mending way to repair Elytra; slow falling pots | Phantoms only, sleep-gated and annoying | Pair with the Elytra economy; loot in dead-tech ruins |
| **Firework rockets / gunpowder** | Elytra fuel - consumed endlessly | Creepers/witches/wandering trader; grindy | Gunpowder loot in tech ruins makes the End the "mobility hub" (Elytra + repair + fuel in one place) |

> These four form a coherent identity: **the End as the mobility + storage endgame hub**
> (Elytra already lives here; add its repair, its fuel, and storage shells). That is a
> strong, on-theme reason to keep returning.

### Tier 2 - strong utility, currently dimension-locked or tedious
| Resource | Why high-use | Why scarce | End hook |
|---|---|---|---|
| **Nether quartz** | Comparators/observers (redstone), tons of decorative blocks | Nether-only; portal trip | Fits the white/quartz palette and dead-tech look; supply modestly so it isn't a Nether bypass |
| **Redstone lamps / sea lanterns / tech lights** | Toggleable + bright lighting; fits the dead-tech look far better than raw glowstone | Sea lanterns are monument-locked; redstone lamps cost glowstone+redstone | Pre-placed as functional lighting in dead-tech ruins; loot redstone lamps/sea lanterns directly |
| **Obsidian / crying obsidian** | Portals, beacon bases, respawn anchors, enchant-proof builds | Slow to mine (diamond pick + lava) | Obsidian scree biome; crying obsidian in ruins |
| **Amethyst (shards/clusters)** | Spyglass, tinted glass, decorative | Geodes only, finite-feeling | Crystal-spire biome geodes - reinforces the alien look |

### Tier 3 - boss-fight rewards (difficult but renewable, no farm glitches)
The highest-value items must stay hard to get *without* being AFK-farmable. Pin them to
**boss encounters guarded by the void's danger** (fall = death), so they are renewable
through genuine risk rather than exploitable mob/duplication farms:
- **Netherite scrap** and **Totems of undying** - drop only from a void-dimension boss
  (custom mini-boss / arena encounter on a high, fall-risk island). This makes them
  obtainable on this server without raids/Nether grinding, yet gated behind a real fight
  and the constant threat of the void - difficult, renewable, not farmable.
- Keep drop counts modest and the encounter non-trivially repeatable (travel + combat +
  void risk each time) so it never degrades into a passive farm.
- **Nether stars / wither skulls** - leave to the wither; do not duplicate here.

> Boss encounters are runtime logic, not worldgen - on Folia they must use region
> schedulers and live in a companion plugin, outside Iris (see the Folia/NMS boundary in
> [`IRIS-END-PICKS.md`](IRIS-END-PICKS.md)). Iris only builds the arena terrain/structure.

### Recommendation
Anchor the End's value on **Tier 1** (Elytra ecosystem + shulker shells + ender pearls) -
thematically native, high-demand, and doesn't step on other dimensions. Sprinkle a **few
Tier 2** materials (quartz, redstone lamps / sea lanterns, amethyst, obsidian) at *modest*
rates as exploration bonuses tied to specific barren biomes / dead-tech structures. Reserve
**Tier 3** (netherite scrap, totems) for void-guarded boss fights - difficult and
renewable without farming glitches. Meter everything (per the barren-camp guardrail) so the
End rewards exploration without becoming a farm.

---

## Design principles for our Iris End

1. **Loneliness is a feature.** Budget emptiness deliberately; let void and silence carry
   long stretches.
2. **Escalate outward.** Stark centre -> increasingly varied/rewarding outer field.
3. **Alien, not lush.** Surreal palettes and shapes; avoid overworld/Nether mimicry.
4. **Rare and rewarding.** Few, high-value structures and loot beats density.
5. **Atmosphere first.** Invest in fog/sky/light/sound mood before adding more blocks.
6. **Don't touch the ceremony.** Leave the dragon/central-island endgame feel intact;
   Iris owns terrain only (see picks doc P0 on the Folia/NMS boundary).

---

## See Also

- [`IRIS-END-PICKS.md`](IRIS-END-PICKS.md) - the prioritised, Folia-filtered build list these preferences validate.
- [`IRIS-END-FEATURE-IDEAS.md`](IRIS-END-FEATURE-IDEAS.md) - the mod/datapack survey behind the ideas.
- [`IRIS-END-GENERATION.md`](IRIS-END-GENERATION.md) - how to author each Iris End layer.
- [`VANILLA-STRUCTURE-COVERAGE.md`](VANILLA-STRUCTURE-COVERAGE.md) - current (vanilla) End scope statement.
