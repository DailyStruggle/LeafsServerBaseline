# Artifacts -> Vanilla (Paper/Folia) Recreation Plan

Working note. Goal: recreate the "Artifacts" mod (by ochotonida) accessories - plus a couple of Terraria-style additions - using only vanilla mechanics on a Paper 1.21.4 / Folia-compatible server.

## Status / scope

- Source of inspiration: Terraria accessories + the Minecraft "Artifacts" mod (CurseForge/Modrinth, repo `github.com/ochotonida/artifacts`).
- Delivery target: a single Folia-supported Paper plugin (working name `net.leaf.artifacts`).
- This is a design scratch note, not a spec. Subject to Rule D-005 proposal before any implementation.

## Delivery mechanism decision: enchantments over inventory/curio slots

Two options were considered for "how does an artifact attach to a player":

1. Inventory-based curio slots (extra GUI slots that hold accessory items).
2. Custom enchantments applied to existing vanilla gear.

Decision: prefer custom enchantments on existing gear.

Rationale:
- No custom inventory/GUI plumbing, no slot-sync edge cases, and naturally Folia-safe (gear is per-player).
- Effects bind to where they make sense (boots, helmet, chestplate, weapon), matching the mod's slot semantics.
- Stacks cleanly with vanilla enchant rules and is visible/inspectable like any enchant.
- Inventory-slot replication remains a fallback if a given effect cannot map to an equippable item, but enchantments are the default.

Open question: 1.21 custom enchantment datapack registration vs. plugin-driven enchant-like markers via item components (`custom_data`). To resolve at proposal time.

## Viability legend

- Easy = native attribute/effect or one simple event; trivially Folia-safe.
- Medium = event logic + per-entity scheduling, standard API.
- Hard = needs emulation/ticking or only partial fidelity.

## Value (obtain difficulty) legend

- Common = early loot (mineshafts, shipwrecks, ocean ruins); QoL or early-game assist.
- Uncommon = mid loot (desert/jungle temples, outposts, buried treasure).
- Rare = late loot (bastions, strongholds, woodland mansions).
- Epic = end-game gated (end cities, ancient cities, raids); power-defining.

## Dropped as truly redundant with vanilla

- Aqua-Dashers -> Frost Walker (permanent boot enchant, also freezes water).
- Snowshoes -> plain leather boots already cross powder snow at no cost.

## Full artifact table

The "Resource-pack image/texture" column describes the texture to make/use. Plan: custom items carrying `custom_model_data`, with a resource pack mapping each value to a 32x32 (Faithful 32x resolution) item sprite. Slot column indicates the gear the enchantment/effect binds to.

Source key: `Boss` = drops from a biome Tier-2 elite/warden (see `CUSTOM-MOB-SPAWNS-PLAN.md`); `Structure` = injected into a structure loot table. Boss drops are thematically matched to the biome; structure drops use rarity-appropriate containers.

| Artifact | Slot | Tier | Effect | Build viability | Value | Source (where to get it) | Resource-pack image/texture | Implemented |
|---|---|---|---|---|---|---|---|---|
| Running Shoes | Boots | Mobility | Auto step-up (`STEP_HEIGHT`) + flat speed (`MOVEMENT_SPEED`) | Easy | Uncommon | Structure: mineshaft / abandoned-mineshaft chests | Worn sneaker/trainer with motion lines; warm palette | Yes |
| Roller Skates | Boots | Mobility | Speed ramps with continuous movement, decays on stop/turn | Medium | Rare | Structure: trial chamber (reward vault) | Skate boot with wheels; small speed-streak accent | Yes |
| Cloud in a Bottle | Belt/Charm | Mobility | Double jump | Medium | Epic | Boss: Caldera Tyrant (structure `leaf:caldera_throne`, Nether) | Glass bottle with a swirling white cloud inside | Yes |
| Helium Flamingo | Charm | Mobility | Timed air-swimming: forces the swim state in midair so you "swim" through the air (NOT gliding) | Hard | Rare | Structure: shipwreck / ocean ruins | Pink inflatable flamingo ring | Yes |
| Flippers | Boots | Mobility | Swim speed boost (in water and, with Helium Flamingo, in air) | Medium | Uncommon | Structure: buried treasure / ocean ruins | Green/teal swim fins | Yes |
| Umbrella | Hands (curio) | Early assist | Conditional slow falling (drift down gently while falling) | Easy | Rare | Structure: woodland mansion | Red-and-white pinwheel umbrella (banner-dyed shield placeholder) | Yes |
| Snorkel | Helmet | Early assist | Breathe underwater | Easy | Common | Structure: buried treasure / shipwreck | Mask + snorkel tube | Yes |
| Charm of Sinking | Charm | Utility | Walk on underwater floor (`SinkingController` cancels buoyancy with a per-tick downward nudge while submerged) | Medium | Uncommon | Structure: ocean ruins / shipwreck | Anchor/weight charm on a chain | Yes |
| Crystal Heart | Chest/Charm | Power | Persistent +max health | Easy | Epic | Boss: Sand Revenant (structure `leaf:sunken_tomb`, desert/badlands) | Faceted red/pink crystalline heart | Yes |
| Cross Necklace | Charm | Defensive | Longer invuln frames | Medium | Rare | Boss: Glacier Warden (structure `leaf:frozen_prison`, snowy biomes) | Silver cross pendant on a chain | Yes |
| Antidote Vessel | Charm | Defensive | Shorten negative effects | Medium | Rare | Boss: Blight Warden (`vanilla/dark_forest__blight`) | Small green vial with cork | Yes |
| Panic Necklace | Charm | Reactive | Speed when hurt | Easy | Uncommon | Structure: pillager outpost | Beaded amber necklace | Yes |
| Obsidian Skull | Charm | Defensive | Passive fire immunity | Easy | Uncommon | Boss: Embervine Warden (`vanilla/jungle__ember`) | Carved black obsidian skull | Yes |
| Steadfast Spikes | Boots | Defensive | Knockback immunity | Easy | Rare | Boss: Glacier Warden (structure `leaf:frozen_prison`, snowy biomes) | Spiked iron sabatons/cleats | Yes |
| Power Glove | Hands | Combat | Always-on +attack damage | Easy | Rare | Structure: trial chamber (ominous vault) | Reinforced studded gauntlet | Yes |
| Feral Claws | Hands | Combat | +Attack speed | Easy | Rare | Boss: The Creak (`swamp/creaks`) | Curved claw blades over knuckles | Yes |
| Vampiric Glove | Hands | Combat | Melee lifesteal | Easy | Rare | Boss: Ashwood Revenant (`vanilla/badlands__ashwood`) | Dark red glove with fang motif | Yes |
| Fire Gauntlet | Hands | Combat | +Melee + ignite on hit | Medium | Rare | Boss: Emberdeep elite (`carving/volcanic`) | Molten-cracked gauntlet, ember glow | Yes |
| Flame Pendant | Charm | Stacking | Ignite on hit (off-weapon) | Easy | Uncommon | Boss: Caldera Tyrant (structure `leaf:caldera_throne`, Nether) | Orange teardrop gem pendant | Yes |
| Thorn Pendant | Charm | Stacking | Reflect melee, no durability cost | Medium | Uncommon | Boss: The Creak (`swamp/creaks`) | Green thorn/bramble pendant | Yes |
| Shock Pendant | Charm | Combat | Lightning on hit (chance) | Easy | Uncommon | Structure: trial chamber | Yellow lightning-bolt pendant | Yes |
| Pocket Piston | Hands | Combat | Knockback burst on hit (`ArtifactEventListener` adds an away-from-attacker velocity impulse on each melee hit) | Medium | Rare | Structure: ancient city | Tiny redstone-piston trinket | Yes |
| Digging Claws | Hands | Stacking | +Mining speed (ambient Haste II via `EffectSpec`; stacks with Efficiency and adds on top of a beacon's Haste) | Easy | Uncommon | Boss: Deep elite (`carving/deep`) | Iron mining-claw grips | Yes |
| Superstitious Hat | Helmet | Stacking | +Mob loot (stacks Looting) | Medium | Uncommon | Structure: woodland mansion | Pointed witch-style hat | Yes |
| Anglers Hat | Helmet | Stacking | +Fishing luck/lure | Medium | Uncommon | Structure: fishing loot / shipwreck | Fishing cap with hooks/lures | Yes |
| Villager Hat | Helmet | Economy | Permanent trade discount | Hard | Epic | Structure: village (rare desert/savanna chest) | Wide-brim straw/village hat | Yes |
| Night Vision Goggles | Helmet | QoL | Permanent night vision | Easy | Common | Boss: Deep elite (`carving/deep`) | Brass/leather goggles | Yes |
| Scarf of Invisibility | Charm | QoL/PvP | Permanent invisibility | Easy | Epic | Boss: Blight Warden (`vanilla/dark_forest__blight`) | Translucent flowing scarf | Yes |
| Onion Ring | Charm | QoL | Mobile Haste after eating | Easy | Common | Boss: Lush cave elite (`carving/lush`) | Golden ring shaped like a battered onion ring | Yes |
| Eternal Steak | Charm | QoL | Never hungry / auto-feed | Easy | Uncommon | Boss: Lush cave elite (`carving/lush`) | Perpetually sizzling steak | Yes |
| Golden Hook | Charm | Boost | +XP from kills | Easy | Uncommon | Boss: Sand Revenant (structure `leaf:sunken_tomb`, desert/badlands) | Ornate golden fish-hook | Yes |
| Lucky Scarf | Charm | Boost | Permanent +Luck | Easy | Uncommon | Structure: trial chamber / buried treasure | Green four-leaf-clover scarf | Yes |
| Universal Attractor | Charm | Utility | Item magnet (`AttractorController` pulls nearby dropped items in with a per-tick velocity nudge; sneak to suspend the pull) | Medium | Rare | Boss: Deep elite (`carving/deep`) | Horseshoe magnet with sparkles | Yes |

## Source assignment: boss drops vs structure loot

> **Boss side moved.** The per-biome boss roster (entities, archetypes, magic design,
> advancement + loot-table delivery) is consolidated in
> `docs/scratch/BIOME-BOSS-MOBS-PLAN.md`. The boss -> artifact mapping below is mirrored
> there; keep the two in sync, but treat the boss-mobs plan as canonical for the boss
> side and this note as canonical for the artifact side. The structure-loot channel and
> the Iris loot-table references below are unchanged here.

Two acquisition channels, matching the existing two-tier mob model:

- Boss drops (biome Tier-2 elites/wardens): thematically tied to the biome's identity and reserved for higher-value, identity-defining artifacts. Delivered via the same Iris loot-table pattern already shipped for `embervine-warden` (e.g. add `iris/pack-overlay/loot/<biome>-<boss>.json` entries). Grind-resistant per `CUSTOM-MOB-SPAWNS-PLAN.md` (rare spawn + gated anti-farm).
- Structure loot: spread across exploration containers for items that are less biome-specific or are early-game assists. Delivered via a vanilla datapack injecting into structure loot tables (layered on top, not baked into Iris), consistent with the deferred "enchanted books reward" approach.

Boss -> artifact mapping (thematic):

- Embervine Warden (`jungle__ember`) -> Obsidian Skull (fire).
- Caldera Tyrant (structure `leaf:caldera_throne`, Nether `basalt_deltas`; `BLAZE` throne boss + `MAGMA_CUBE` court adds) -> Cloud in a Bottle (eruption launch), Flame Pendant.
- Emberdeep elite (`carving/volcanic`) -> Fire Gauntlet.
- Ashwood Revenant (`badlands__ashwood`) -> Vampiric Glove (undead lifesteal).
- Glacier Warden (structure `leaf:frozen_prison`, snowy biomes; undead warden `WITHER_SKELETON`) -> Steadfast Spikes (unyielding), Cross Necklace.
- Blight Warden (`dark_forest__blight`) -> Antidote Vessel (toxins), Scarf of Invisibility (gloom).
- Sand Revenant (structure `leaf:sunken_tomb`, desert/badlands; undead `HUSK`/`STRAY`) -> Crystal Heart (tomb treasure), Golden Hook.
- Deep elite (`carving/deep`) -> Night Vision Goggles, Digging Claws, Universal Attractor.
- Lush cave elite (`carving/lush`) -> Onion Ring, Eternal Steak.
- The Creak (`swamp/creaks`) -> Thorn Pendant (brambles), Feral Claws.

Items intentionally left to structures (not biome-thematic, or early-game/exploration items): Running Shoes, Roller Skates, Helium Flamingo, Umbrella, Snorkel, Charm of Sinking, Panic Necklace, Power Glove, Shock Pendant, Pocket Piston, Superstitious Hat, Anglers Hat, Villager Hat, Lucky Scarf.

Note: several mapped bosses are still `planned` in `CUSTOM-MOB-SPAWNS-PLAN.md`; those artifact drops ship when the corresponding biome elite ships. Until then their artifacts can fall back to a structure source.

### Terralost retirement (Iris -> vanilla-datapack finds)

The three `terralost/*` biome sources are retired. Iris was dropped for vanilla-datapack worldgen
(ADR-005), so `terralost/volcanic-crater`/`caldera`, `terralost/glacial-chasm`, and
`terralost/ancient-sands` no longer generate in the live world and cannot host a boss. Rather than
re-author them as custom biomes, each becomes a **rare custom jigsaw structure injected into a fitting
vanilla biome** (the "structure-over-biome" rule), wired exactly like the shipped `leaf:dark_forest_giant`:

| Boss | Was (terralost biome) | Now (structure @ vanilla biome) | Boss base |
|---|---|---|---|
| Caldera Tyrant | `volcanic-crater`/`caldera` | `leaf:caldera_throne` @ Nether `basalt_deltas` (throne over lava) | `BLAZE` + `MAGMA_CUBE` court adds |
| Glacier Warden | `glacial-chasm` | `leaf:frozen_prison` @ snowy biomes (undead "ice jail") | undead `WITHER_SKELETON` + skeleton/stray adds |
| Sand Revenant | `ancient-sands` | `leaf:sunken_tomb` @ `desert`/`badlands` (sphinx/tomb ruin) | undead `HUSK`/`STRAY` |

All three share one pipeline (custom structure + amped vanilla-entity boss + PDC anti-farm), so no
custom biomes are required. The artifact drops are unchanged; only the source channel moves from an
Iris biome to a datapack structure.

## Structure-rarity guideline (for the structure-sourced subset)

- Common -> shipwrecks, mineshafts, ocean ruins.
- Uncommon -> desert/jungle temples, pillager outposts, buried treasure.
- Rare -> bastions, strongholds, woodland mansions.
- Epic -> end cities, ancient cities, raid drops (or boss-gated).

## Structure-chest loot mapping (biome + rarity, implemented)

The plugin injects artifacts into structure chests at runtime via `LootGenerateEvent`
(no datapack; see `plugins/leaf-artifacts` `ArtifactLoot.classifyChest`). A structure
only generates in its own biome(s), so the loot-table id already encodes the structure
(and hence biome) - no runtime biome lookup is needed. Classification is **two axes**:
a structure base tier (axis 1) shifted by a per-chest rarity keyword (axis 2), clamped
to `COMMON..SPECIAL` (so `BOSS`-only artifacts never leak into chests). Tiers map onto
the four loot buckets below; mob drops and biome-boss drops are separate channels.

### Axis 1 - structure base tier (by namespace + token)

| Pack (loot-table namespace) | Structure group / token | Base tier |
|---|---|---|
| vanilla (`minecraft`) | mineshaft, shipwreck, ocean/underwater ruin | COMMON |
| vanilla (`minecraft`) | desert/jungle temple, igloo, pillager outpost, buried treasure, nether fortress | UNCOMMON |
| vanilla (`minecraft`) | stronghold, woodland mansion | RARE |
| vanilla (`minecraft`) | ancient city, trial chamber, end city, bastion | SPECIAL |
| CTOV (`ctov`) | villages | COMMON |
| Dungeons and Taverns (`nova_structures`) | general | UNCOMMON |
| Dungeons and Taverns (`nova_structures`) | `illager_mansion`, `lone_citadel` | RARE |
| When Dungeons Arise (`dungeons_arise`) | large dungeons/monuments | RARE |
| When Dungeons Arise (`dungeons_arise`) | `mines_*` | COMMON |
| Seven Seas (`dungeons_arise_seven_seas`) | pirate ships | UNCOMMON |
| YUNG's Better Strongholds (`betterstrongholds`) | stronghold | RARE |
| YUNG's Better Dungeons (`betterdungeons`) | skeleton/zombie/spider/nether dungeon | UNCOMMON |
| Structory (`structory`) | ruins, manors, graveyards, library | UNCOMMON |
| Structory Towers (`structory_towers`) | towers | UNCOMMON |
| Structory Towers (`structory_towers`) | `end_tower` | RARE |
| Towns and Towers (`kaisyn`) | village | COMMON |
| Towns and Towers (`kaisyn`) | `outpost`, archeology/ruins | UNCOMMON |
| Explorify (`explorify`) | settlements, caches, mausoleum | COMMON |
| Hopo Better Mineshaft (`hopo`) | mineshaft | COMMON |
| Stoneholm (`stoneholm`) | underground village rooms | COMMON |
| (any other loot table containing `chest`) | catch-all | COMMON |

Pack-namespace rules are matched **before** the generic vanilla tokens, so a chest whose
name merely contains a high-tier vanilla word (e.g. `nova_structures` mansion chest
`ancient_city_raid_chest`) is read as its real structure (RARE), not over-promoted.

### Axis 2 - per-chest rarity shift (from the chest's leaf name)

| Shift | Keywords |
|---|---|
| +1 tier | `treasure`, `treasury`, `big`, `grand`, `vault`, `boss`, `special`, `high`, `rare`, `top` |
| -1 tier | `common`, `small`, `barrel`, `normal`, `supply`, `loot_piles`, `mess`, `food`, `low` |
| 0 (none) | everything else |

At most one step either way (up wins ties), then clamp to `COMMON..SPECIAL`. Examples:
`betterstrongholds:chests/treasure` = RARE+1 -> SPECIAL; `.../common` = RARE-1 -> UNCOMMON;
`dungeons_arise:chests/mines_treasure_big` = COMMON+1 -> UNCOMMON; `seven_seas:.../barrels`
= UNCOMMON-1 -> COMMON.

> First pass per the "get it working, refine later" agreement; base tiers and keyword
> lists are easy to retune in `ArtifactLoot`. Covered by `ArtifactLootTest`.

## Tier-ordered artifact list (for loot-table placement)

Artifacts grouped by their `Value` tier, mapped onto the four loot-table buckets used by the
loot-injection datapack/boss tables: `common`, `uncommon`, `rare`, and `special` (the `Epic`
value tier maps to `special`). Ids match `leafartifacts:<id>` (see the resource-pack `SOURCING.md`).
Use this as the canonical placement list; the per-artifact source channel (boss vs structure) is in
the "Source" column of the full artifact table above.

### common

- `snorkel` - Snorkel
- `night_vision_goggles` - Night Vision Goggles
- `onion_ring` - Onion Ring

### uncommon

- `running_shoes` - Running Shoes
- `flippers` - Flippers
- `charm_of_sinking` - Charm of Sinking
- `panic_necklace` - Panic Necklace
- `obsidian_skull` - Obsidian Skull
- `flame_pendant` - Flame Pendant
- `thorn_pendant` - Thorn Pendant
- `shock_pendant` - Shock Pendant
- `digging_claws` - Digging Claws
- `superstitious_hat` - Superstitious Hat
- `anglers_hat` - Anglers Hat
- `eternal_steak` - Eternal Steak
- `golden_hook` - Golden Hook
- `lucky_scarf` - Lucky Scarf

### rare

- `roller_skates` - Roller Skates
- `helium_flamingo` - Helium Flamingo
- `umbrella` - Umbrella
- `cross_necklace` - Cross Necklace
- `antidote_vessel` - Antidote Vessel
- `steadfast_spikes` - Steadfast Spikes
- `power_glove` - Power Glove
- `feral_claws` - Feral Claws
- `vampiric_glove` - Vampiric Glove
- `fire_gauntlet` - Fire Gauntlet
- `pocket_piston` - Pocket Piston
- `universal_attractor` - Universal Attractor

### special

- `cloud_in_a_bottle` - Cloud in a Bottle
- `crystal_heart` - Crystal Heart
- `villager_hat` - Villager Hat
- `scarf_of_invisibility` - Scarf of Invisibility

Note: `bunny_hoppers` exists as an item id but is not in the full artifact table above; assign it a
tier (suggested `uncommon`) when it gets a row before adding it to a loot table.

## Defensive-vs-aggressive rarity skew (with dimension)

Balance intent: defensive/survival artifacts should drop **more readily** than aggressive/combat
artifacts of the same nominal power, so players can cover their defenses before they snowball
offense. The table below re-grades artifacts by combat role and applies a one-tier skew:

- Defensive / survival role -> shift **one tier more common** than its base `Value`.
- Aggressive / offense role -> keep base tier (or shift one tier rarer for the strongest hitters).
- Neutral (mobility, QoL, utility, economy) -> unchanged from the tier-ordered list above.

The `Dimension` column ties a drop to a dimension only where the artifact's theme makes it natural
(fire-themed -> Nether; end-game power -> End); blank means Overworld / dimension-agnostic.

| Artifact | id | Role | Base tier | Skewed tier | Dimension |
|---|---|---|---|---|---|
| Obsidian Skull | `obsidian_skull` | Defensive (fire immunity) | Uncommon | common | Nether |
| Panic Necklace | `panic_necklace` | Defensive (reactive) | Uncommon | common | - |
| Thorn Pendant | `thorn_pendant` | Defensive (reflect) | Uncommon | common | - |
| Cross Necklace | `cross_necklace` | Defensive (invuln) | Rare | uncommon | - |
| Antidote Vessel | `antidote_vessel` | Defensive (cleanse) | Rare | uncommon | - |
| Steadfast Spikes | `steadfast_spikes` | Defensive (knockback immune) | Rare | uncommon | - |
| Crystal Heart | `crystal_heart` | Defensive (+max health) | Epic | rare | End |
| Shock Pendant | `shock_pendant` | Aggressive | Uncommon | uncommon | - |
| Flame Pendant | `flame_pendant` | Aggressive (ignite) | Uncommon | uncommon | Nether |
| Feral Claws | `feral_claws` | Aggressive (attack speed) | Rare | rare | - |
| Power Glove | `power_glove` | Aggressive (+damage) | Rare | rare | - |
| Vampiric Glove | `vampiric_glove` | Aggressive (lifesteal) | Rare | rare | - |
| Pocket Piston | `pocket_piston` | Aggressive (knockback) | Rare | rare | - |
| Fire Gauntlet | `fire_gauntlet` | Aggressive (+melee + ignite) | Rare | special | Nether |

Neutral artifacts (Running Shoes, Roller Skates, Cloud in a Bottle, Helium Flamingo, Flippers,
Umbrella, Snorkel, Charm of Sinking, Night Vision Goggles, Scarf of Invisibility, Onion Ring,
Eternal Steak, Golden Hook, Lucky Scarf, Superstitious Hat, Anglers Hat, Villager Hat, Digging
Claws, Universal Attractor) keep their tier from the tier-ordered list above and carry no dimension
skew, except where their source boss/structure already lives in a specific dimension.

## Folia compatibility checklist (for implementation)

- Set `folia-supported: true` in `paper-plugin.yml`.
- Never use `Bukkit.getScheduler()`; use entity (`player.getScheduler()`), region (`Bukkit.getRegionScheduler()`), or global (`Bukkit.getGlobalRegionScheduler()`) schedulers.
- Touch each entity/world only from its owning region thread; event handlers already run on the correct thread.
- Per-player ticking items (Roller Skates, Antidote Vessel, Universal Attractor, Eternal Steak) drive off the entity scheduler.

## Balance notes

- Running Shoes + Roller Skates share one speed subsystem (both velocity-based, not separate or mutually exclusive): Running Shoes sets the starting/floor speed, Roller Skates sets the max/ceiling speed that ramping accelerates toward. Implement as a single per-player speed value clamped between floor (Running Shoes) and ceiling (Roller Skates); wearing only one just sets that bound. This avoids double-stacking while letting the pair combine cleanly.
- Stacking artifacts (Digging Claws, Power Glove, Flame/Thorn Pendant, Superstitious/Anglers Hat) add on top of enchants; verify caps so totals stay reasonable.
- Ambient potion-effect artifacts are declared generically as `EffectSpec(type, amplifier)` and reconciled with a single "buff a pre-existing effect" rule that stacks **additively** like enchantment levels: each pass briefly removes the plugin's own effect, samples the remaining external amplifier (a beacon, a potion), and re-applies at `externalAmplifier + artifactAmplifier`. So a beacon's Haste II plus Digging Claws' Haste II gives Haste III, with no per-artifact workaround. Vanilla itself only keeps the strongest instance of a same-type effect, which is why the plugin samples-then-sums; sampling excludes the plugin's own contribution each pass, so the total never runs away.
- Air-swim combo (signature synergy): Helium Flamingo forces the swim state in midair (you do the swimming pose/motion, NOT gliding), and Flippers adds swim speed that applies in that air-swim state too. Together they make a fast air-swim traversal mode - deliberately unlike Elytra. Implementation: detect Helium Flamingo equipped + airborne, set the player to the swimming state (and/or apply velocity along the look vector) on a per-entity tick; have the Flippers swim-speed modifier apply whenever the player is in the swim state (water or Flamingo-induced air-swim). Folia: drive from the entity scheduler. Tune as a strong-but-limited (timed/charged) mobility tool, not free permanent flight.
- Cloud + glide combo: Cloud in a Bottle's air-jump adds a fixed upward (+Y) impulse to current velocity (one charge per airtime, speed-capped). +Y only means it doubles as fall-arrest. It is not usable during an active glide; the travel boost is just a side effect of firing on the same key press that enters glide, so the +Y is converted to forward momentum as the glide starts (like a small firework).

## Application prioritization

Build order, easiest/highest-leverage first. Each phase is independently shippable.

- P0 - Foundation (do first): plugin skeleton (`net.leaf.artifacts`, `folia-supported: true`), item-marker scheme (`custom_data`/enchant), and equip/unequip + join detection. Nothing else works without this.
- P1 - Attribute/effect artifacts (Easy, no ticking): Running Shoes, Crystal Heart, Steadfast Spikes, Feral Claws, Power Glove, Lucky Scarf, Night Vision Goggles, Snorkel, Scarf of Invisibility, Obsidian Skull. Pure equip-time modifiers/effects.
- P2 - Simple event reactions (Easy): Vampiric Glove, Flame Pendant, Shock Pendant, Panic Necklace, Onion Ring, Golden Hook, Fire Gauntlet, Bunny Hoppers.
- P3 - Signature movement (Medium, the marquee items): Cloud in a Bottle (+Y impulse, glide-entry synergy), Helium Flamingo + Flippers (air-swim combo), Roller Skates (momentum tick). First per-entity scheduler work.
- P4 - Remaining ticking/utility (Medium): Antidote Vessel, Cross Necklace, Thorn Pendant, Eternal Steak, Superstitious Hat, Anglers Hat. (Digging Claws shipped early as an infinite ambient Haste - declarative, no ticking needed. Charm of Sinking shipped via `SinkingController`, a per-tick downward velocity nudge while submerged. Pocket Piston shipped as an event-driven melee knockback burst in `ArtifactEventListener`. Universal Attractor shipped via `AttractorController`, a per-tick item magnet that sneaking suspends.)
- P5 - Hard/partial-fidelity (last): Umbrella (delivered as conditional Slow Falling on an unbreakable colored shield, `UmbrellaController`; the always-on float while falling is its cost), Villager Hat (trade discounts).
- P6 - Presentation (textures): the resource-pack scaffold ships in `plugins/leaf-artifacts/resourcepack/` - a player-optional, server-sent pack with a per-artifact item-model (`assets/leafartifacts/items/<id>.json` + `models/item/<id>.json`) wired to the item via the `minecraft:item_model` component (`leafartifacts:<id>`) in `ArtifactItem.create`. The only remaining step is dropping a 32x32 (Faithful 32x resolution) `<id>.png` per artifact into `assets/leafartifacts/textures/item/`; the id -> texture-brief list lives in that folder's `SOURCING.md`. Use original art (do not copy the ochotonida mod's sprites).
- Loot sourcing (where artifacts drop): **structure + mob drops are now implemented in the `leaf-artifacts` plugin** (no datapack) via a runtime hook - structure-chest loot through `LootGenerateEvent` and a curated hostile-mob set through `EntityDeathEvent` (see `ArtifactLoot` / `ArtifactLootListener` / `ArtifactLootTier` and the plugin README "Loot sourcing"). Tier buckets follow the skewed tier list above. Boss drops remain owned by `docs/scratch/BIOME-BOSS-MOBS-PLAN.md` (each artifact ships with its biome elite's loot table); the two `BOSS`-tagged artifacts (Verdant Crown, Frostward Charm) are excluded from the plugin injection so the channels never double up. The "Source (where to get it)" column and the boss -> artifact mapping below remain the design intent these consume.

Open question to settle before P0: 1.21 custom-enchantment datapack registration vs. plugin-driven `custom_data` markers.

Implementation is gated behind a Rule D-005 proposal (file layout for `net.leaf.artifacts` + texture list) before any code.
