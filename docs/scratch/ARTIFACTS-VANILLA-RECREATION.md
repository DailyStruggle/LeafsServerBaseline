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

The "Resource-pack image/texture" column describes the texture to make/use. Plan: custom items carrying `custom_model_data`, with a resource pack mapping each value to a 16x16 (item) sprite. Slot column indicates the gear the enchantment/effect binds to.

Source key: `Boss` = drops from a biome Tier-2 elite/warden (see `CUSTOM-MOB-SPAWNS-PLAN.md`); `Structure` = injected into a structure loot table. Boss drops are thematically matched to the biome; structure drops use rarity-appropriate containers.

| Artifact | Slot | Tier | Effect | Build viability | Value | Source (where to get it) | Resource-pack image/texture | Implemented |
|---|---|---|---|---|---|---|---|---|
| Running Shoes | Boots | Mobility | Auto step-up (`STEP_HEIGHT`) + flat speed (`MOVEMENT_SPEED`) | Easy | Uncommon | Structure: mineshaft / abandoned-mineshaft chests | Worn sneaker/trainer with motion lines; warm palette | Yes |
| Roller Skates | Boots | Mobility | Speed ramps with continuous movement, decays on stop/turn | Medium | Rare | Structure: trial chamber (reward vault) | Skate boot with wheels; small speed-streak accent | Yes |
| Cloud in a Bottle | Belt/Charm | Mobility | Double jump | Medium | Epic | Boss: Caldera Tyrant (`terralost/volcanic-crater`) | Glass bottle with a swirling white cloud inside | Yes |
| Helium Flamingo | Charm | Mobility | Timed air-swimming: forces the swim state in midair so you "swim" through the air (NOT gliding) | Hard | Rare | Structure: shipwreck / ocean ruins | Pink inflatable flamingo ring | Yes |
| Flippers | Boots | Mobility | Swim speed boost (in water and, with Helium Flamingo, in air) | Medium | Uncommon | Structure: buried treasure / ocean ruins | Green/teal swim fins | Yes |
| Umbrella | Off-hand | Early assist | Shield + early glide | Hard | Rare | Structure: woodland mansion | Folded/closed black umbrella | No |
| Snorkel | Helmet | Early assist | Breathe underwater | Easy | Common | Structure: buried treasure / shipwreck | Mask + snorkel tube | Yes |
| Charm of Sinking | Charm | Utility | Walk on underwater floor | Medium | Uncommon | Structure: ocean ruins / shipwreck | Anchor/weight charm on a chain | No |
| Crystal Heart | Chest/Charm | Power | Persistent +max health | Easy | Epic | Boss: Sand Revenant (`terralost/ancient-sands`) | Faceted red/pink crystalline heart | Yes |
| Cross Necklace | Charm | Defensive | Longer invuln frames | Medium | Rare | Boss: Glacier Warden (`terralost/glacial-chasm`) | Silver cross pendant on a chain | Yes |
| Antidote Vessel | Charm | Defensive | Shorten negative effects | Medium | Rare | Boss: Blight Warden (`vanilla/dark_forest__blight`) | Small green vial with cork | Yes |
| Panic Necklace | Charm | Reactive | Speed when hurt | Easy | Uncommon | Structure: pillager outpost | Beaded amber necklace | Yes |
| Obsidian Skull | Charm | Defensive | Passive fire immunity | Easy | Uncommon | Boss: Embervine Warden (`vanilla/jungle__ember`) | Carved black obsidian skull | Yes |
| Steadfast Spikes | Boots | Defensive | Knockback immunity | Easy | Rare | Boss: Glacier Warden (`terralost/glacial-chasm`) | Spiked iron sabatons/cleats | Yes |
| Power Glove | Hands | Combat | Always-on +attack damage | Easy | Rare | Structure: trial chamber (ominous vault) | Reinforced studded gauntlet | Yes |
| Feral Claws | Hands | Combat | +Attack speed | Easy | Rare | Boss: The Creak (`swamp/creaks`) | Curved claw blades over knuckles | Yes |
| Vampiric Glove | Hands | Combat | Melee lifesteal | Easy | Rare | Boss: Ashwood Revenant (`vanilla/badlands__ashwood`) | Dark red glove with fang motif | Yes |
| Fire Gauntlet | Hands | Combat | +Melee + ignite on hit | Medium | Rare | Boss: Emberdeep elite (`carving/volcanic`) | Molten-cracked gauntlet, ember glow | Yes |
| Flame Pendant | Charm | Stacking | Ignite on hit (off-weapon) | Easy | Uncommon | Boss: Caldera Tyrant (`terralost/caldera`) | Orange teardrop gem pendant | Yes |
| Thorn Pendant | Charm | Stacking | Reflect melee, no durability cost | Medium | Uncommon | Boss: The Creak (`swamp/creaks`) | Green thorn/bramble pendant | Yes |
| Shock Pendant | Charm | Combat | Lightning on hit (chance) | Easy | Uncommon | Structure: trial chamber | Yellow lightning-bolt pendant | Yes |
| Pocket Piston | Hands | Combat | Knockback burst on hit | Medium | Rare | Structure: ancient city | Tiny redstone-piston trinket | No |
| Digging Claws | Hands | Stacking | +Mining speed (stacks Efficiency) | Medium | Uncommon | Boss: Deep elite (`carving/deep`) | Iron mining-claw grips | No |
| Superstitious Hat | Helmet | Stacking | +Mob loot (stacks Looting) | Medium | Uncommon | Structure: woodland mansion | Pointed witch-style hat | Yes |
| Anglers Hat | Helmet | Stacking | +Fishing luck/lure | Medium | Uncommon | Structure: fishing loot / shipwreck | Fishing cap with hooks/lures | Yes |
| Villager Hat | Helmet | Economy | Permanent trade discount | Hard | Epic | Structure: village (rare desert/savanna chest) | Wide-brim straw/village hat | Yes |
| Night Vision Goggles | Helmet | QoL | Permanent night vision | Easy | Common | Boss: Deep elite (`carving/deep`) | Brass/leather goggles | Yes |
| Scarf of Invisibility | Charm | QoL/PvP | Permanent invisibility | Easy | Epic | Boss: Blight Warden (`vanilla/dark_forest__blight`) | Translucent flowing scarf | Yes |
| Onion Ring | Charm | QoL | Mobile Haste after eating | Easy | Common | Boss: Lush cave elite (`carving/lush`) | Golden ring shaped like a battered onion ring | Yes |
| Eternal Steak | Charm | QoL | Never hungry / auto-feed | Easy | Uncommon | Boss: Lush cave elite (`carving/lush`) | Perpetually sizzling steak | Yes |
| Golden Hook | Charm | Boost | +XP from kills | Easy | Uncommon | Boss: Sand Revenant (`terralost/ancient-sands`) | Ornate golden fish-hook | Yes |
| Lucky Scarf | Charm | Boost | Permanent +Luck | Easy | Uncommon | Structure: trial chamber / buried treasure | Green four-leaf-clover scarf | Yes |
| Universal Attractor | Charm | Utility | Item magnet (toggle) | Medium | Rare | Boss: Deep elite (`carving/deep`) | Horseshoe magnet with sparkles | No |

## Source assignment: boss drops vs structure loot

Two acquisition channels, matching the existing two-tier mob model:

- Boss drops (biome Tier-2 elites/wardens): thematically tied to the biome's identity and reserved for higher-value, identity-defining artifacts. Delivered via the same Iris loot-table pattern already shipped for `embervine-warden` (e.g. add `iris/pack-overlay/loot/<biome>-<boss>.json` entries). Grind-resistant per `CUSTOM-MOB-SPAWNS-PLAN.md` (rare spawn + gated anti-farm).
- Structure loot: spread across exploration containers for items that are less biome-specific or are early-game assists. Delivered via a vanilla datapack injecting into structure loot tables (layered on top, not baked into Iris), consistent with the deferred "enchanted books reward" approach.

Boss -> artifact mapping (thematic):

- Embervine Warden (`jungle__ember`) -> Obsidian Skull (fire).
- Caldera Tyrant (`terralost/volcanic-crater` / `caldera`) -> Cloud in a Bottle (eruption launch), Flame Pendant.
- Emberdeep elite (`carving/volcanic`) -> Fire Gauntlet.
- Ashwood Revenant (`badlands__ashwood`) -> Vampiric Glove (undead lifesteal).
- Glacier Warden (`terralost/glacial-chasm`) -> Steadfast Spikes (unyielding), Cross Necklace.
- Blight Warden (`dark_forest__blight`) -> Antidote Vessel (toxins), Scarf of Invisibility (gloom).
- Sand Revenant (`terralost/ancient-sands`) -> Crystal Heart (tomb treasure), Golden Hook.
- Deep elite (`carving/deep`) -> Night Vision Goggles, Digging Claws, Universal Attractor.
- Lush cave elite (`carving/lush`) -> Onion Ring, Eternal Steak.
- The Creak (`swamp/creaks`) -> Thorn Pendant (brambles), Feral Claws.

Items intentionally left to structures (not biome-thematic, or early-game/exploration items): Running Shoes, Roller Skates, Helium Flamingo, Umbrella, Snorkel, Charm of Sinking, Panic Necklace, Power Glove, Shock Pendant, Pocket Piston, Superstitious Hat, Anglers Hat, Villager Hat, Lucky Scarf.

Note: several mapped bosses are still `planned` in `CUSTOM-MOB-SPAWNS-PLAN.md`; those artifact drops ship when the corresponding biome elite ships. Until then their artifacts can fall back to a structure source.

## Structure-rarity guideline (for the structure-sourced subset)

- Common -> shipwrecks, mineshafts, ocean ruins.
- Uncommon -> desert/jungle temples, pillager outposts, buried treasure.
- Rare -> bastions, strongholds, woodland mansions.
- Epic -> end cities, ancient cities, raid drops (or boss-gated).

## Folia compatibility checklist (for implementation)

- Set `folia-supported: true` in `paper-plugin.yml`.
- Never use `Bukkit.getScheduler()`; use entity (`player.getScheduler()`), region (`Bukkit.getRegionScheduler()`), or global (`Bukkit.getGlobalRegionScheduler()`) schedulers.
- Touch each entity/world only from its owning region thread; event handlers already run on the correct thread.
- Per-player ticking items (Roller Skates, Antidote Vessel, Universal Attractor, Eternal Steak) drive off the entity scheduler.

## Balance notes

- Running Shoes + Roller Skates share one speed subsystem (both velocity-based, not separate or mutually exclusive): Running Shoes sets the starting/floor speed, Roller Skates sets the max/ceiling speed that ramping accelerates toward. Implement as a single per-player speed value clamped between floor (Running Shoes) and ceiling (Roller Skates); wearing only one just sets that bound. This avoids double-stacking while letting the pair combine cleanly.
- Stacking artifacts (Digging Claws, Power Glove, Flame/Thorn Pendant, Superstitious/Anglers Hat) add on top of enchants; verify caps so totals stay reasonable.
- Air-swim combo (signature synergy): Helium Flamingo forces the swim state in midair (you do the swimming pose/motion, NOT gliding), and Flippers adds swim speed that applies in that air-swim state too. Together they make a fast air-swim traversal mode - deliberately unlike Elytra. Implementation: detect Helium Flamingo equipped + airborne, set the player to the swimming state (and/or apply velocity along the look vector) on a per-entity tick; have the Flippers swim-speed modifier apply whenever the player is in the swim state (water or Flamingo-induced air-swim). Folia: drive from the entity scheduler. Tune as a strong-but-limited (timed/charged) mobility tool, not free permanent flight.
- Cloud + glide combo: Cloud in a Bottle's air-jump adds a fixed upward (+Y) impulse to current velocity (one charge per airtime, speed-capped). +Y only means it doubles as fall-arrest. It is not usable during an active glide; the travel boost is just a side effect of firing on the same key press that enters glide, so the +Y is converted to forward momentum as the glide starts (like a small firework).

## Application prioritization

Build order, easiest/highest-leverage first. Each phase is independently shippable.

- P0 - Foundation (do first): plugin skeleton (`net.leaf.artifacts`, `folia-supported: true`), item-marker scheme (`custom_data`/enchant), and equip/unequip + join detection. Nothing else works without this.
- P1 - Attribute/effect artifacts (Easy, no ticking): Running Shoes, Crystal Heart, Steadfast Spikes, Feral Claws, Power Glove, Lucky Scarf, Night Vision Goggles, Snorkel, Scarf of Invisibility, Obsidian Skull. Pure equip-time modifiers/effects.
- P2 - Simple event reactions (Easy): Vampiric Glove, Flame Pendant, Shock Pendant, Panic Necklace, Onion Ring, Golden Hook, Fire Gauntlet, Bunny Hoppers.
- P3 - Signature movement (Medium, the marquee items): Cloud in a Bottle (+Y impulse, glide-entry synergy), Helium Flamingo + Flippers (air-swim combo), Roller Skates (momentum tick). First per-entity scheduler work.
- P4 - Remaining ticking/utility (Medium): Antidote Vessel, Cross Necklace, Thorn Pendant, Pocket Piston, Charm of Sinking, Universal Attractor, Eternal Steak, Digging Claws, Superstitious Hat, Anglers Hat.
- P5 - Hard/partial-fidelity (last): Umbrella (glide+shield), Villager Hat (trade discounts).
- P6 - Sourcing: wire boss drops into Iris loot tables (per shipped biome elites) and structure drops via a layered datapack; add resource-pack textures.

Open question to settle before P0: 1.21 custom-enchantment datapack registration vs. plugin-driven `custom_data` markers.

Implementation is gated behind a Rule D-005 proposal (file layout for `net.leaf.artifacts` + texture list) before any code.
