# LeafArtifacts

Exploration artifacts (wearable trinkets, inspired by the "Artifacts" mod) recreated with vanilla Paper mechanics. Folia-compatible.

See the design note: `docs/scratch/ARTIFACTS-VANILLA-RECREATION.md`.

## Status

Four passes implemented so far (P1, P2, the first P3 movement item, and the easy P4 charms).

P1 - passive attribute modifiers and/or infinite potion effects present while the artifact is carried:

- Running Shoes (step-up + movement speed)
- Crystal Heart (max health)
- Steadfast Spikes (knockback resistance)
- Feral Claws (attack speed)
- Power Glove (attack damage)
- Lucky Scarf (luck)
- Night Vision Goggles (night vision)
- Snorkel (water breathing)
- Scarf of Invisibility (invisibility)
- Obsidian Skull (fire resistance)

P2 - simple event reactions (one-shot reactions to a gameplay event while the artifact is carried):

- Vampiric Glove (heal a fraction of melee damage dealt)
- Flame Pendant (ignite enemies on melee hit)
- Shock Pendant (chance to call lightning on melee hit)
- Fire Gauntlet (extra melee damage + ignite on hit)
- Panic Necklace (Speed burst when hurt)
- Bunny Hoppers (higher jump + cancels fall damage)
- Onion Ring (Haste for a while after eating)
- Golden Hook (extra experience from kills)

P3 - signature movement:

- Cloud in a Bottle (mid-air double jump; vertical impulse scales with jump strength, so it stacks with Bunny Hoppers). With an elytra, the charge is instead spent as a vertical launch boost when you deploy the glide.
- Roller Skates (momentum-based speed: while moving, a multiplicative MOVEMENT_SPEED bonus ramps up to a ceiling and decays when you stop, stacking on top of Running Shoes' flat floor; at/above ~vanilla sprint speed it slides via client-only **ghost ice** (`GhostIceProjector`). Each tick, a rounded cone of packed-ice block-change packets is sent only to the wearer, painted onto the floor ahead of their *motion* direction (not look direction) and sized by speed (an always-on apex disc around the feet plus a forward sector whose reach grows and half-angle narrows as you speed up; the cone spans a few blocks so the slide surface reads clearly). The client then runs vanilla ice friction itself, so the slide is genuinely client-predicted and we never call `setVelocity` (eliminating the old "fling" bugs). Only existing solid walkable floor blocks are reskinned (never air, so collision stays in sync), edits are batched into per-tick chunk packets, and the ice lingers (~30 ticks) after a cell leaves the cone before reverting, so the surface lasts longer and flickers far less (cells are refreshed, not toggled on/off each tick); it still reverts on stop / water / teleport / unequip / quit. Below sprint speed no ice is sent, so walking is fully controllable - and because the cone is only present at speed (never while you stand still to mine), the wearer never interacts with the ghost blocks).
- Helium Flamingo (air-swimming: mirrors the Roller Skates ghost-block trick with client-only **ghost water** (`GhostWaterProjector`). The swim pose and swim physics are derived by the wearer's own client from water contact (which is why `setSwimming`/`setGliding` never gave a real swim feel), so instead, while the wearer is airborne, out of real water, not on an elytra, **and is pressing the sprint key (Ctrl) to "swim"**, water block-change packets are sent only to that player. Unlike the ice slide, the water footprint is fully **3D and oriented along the wearer's *look* direction** (swimming goes where you point, including up and down): a solid **sphere** of water around the body plus a rounded **3D cone** of water ahead of the look vector, sized by the 3D speed. Filling the cells the player is about to swim into vertically as well as horizontally is what keeps the client in the swim pose instead of snapping it back to standing (the old "flash"/kick-out); the water also lingers longer (~40 ticks) for the same reason, so it persists and flickers far less. Because the wet volume must be larger and deeper than the ice cone, this projects considerably more cells than `GhostIceProjector`. The client then runs real swim physics/pose itself. Only air cells are converted to water (never solid blocks, and water is non-solid, so collision stays in sync), and edits are batched into per-tick chunk packets. Releasing Ctrl / landing / entering water / unequip / quit reverts it immediately. Feel still needs in-game evaluation; the fullscreen water overlay/fog is the known UX trade-off to judge).

- Flippers (swim-speed boost). Vanilla has no swim-speed attribute, so the boost is delivered as an ambient, hidden **Dolphin's Grace** (amplifier 0, the vanilla dolphin strength) refreshed each tick on the player's entity scheduler while the wearer carries Flippers and is in water. By synergy it also applies during the **Helium Flamingo air-swim** (the controller exposes `isAirSwimming(player)`, which Flippers reads), so swim speed carries into the air-swim state too - the documented Flippers + Helium Flamingo combo. Since swimming is the only thing Dolphin's Grace changes, the same effect serves both water and air-swim with no extra logic. The effect is short-lived and only refreshed while applicable, so it lapses on its own when the wearer leaves the water / air-swim or unequips, and a genuine Dolphin's Grace (e.g. from a real dolphin) is never stripped.

P4 - event-driven charms (no per-entity ticking):

- Cross Necklace (lengthens your invulnerability frames after a hit)
- Antidote Vessel (harmful status effects expire faster - currently halved)
- Thorn Pendant (reflects a fraction of melee damage taken, no durability cost)
- Eternal Steak (NOT a curio: a steak you can eat repeatedly that is never consumed)

Hats (helmet/head slot):

- Superstitious Hat (Looting-style mob loot: when the wearer gets the kill, each loot stack in `EntityDeathEvent` has a chance to drop one extra item, emulating how Looting raises the upper bound of drops).
- Anglers Hat (fishing Lure + Luck of the Sea: on cast, the hook's wait window is shortened on `PlayerFishEvent` (FISHING); on a successful catch (CAUGHT_FISH) the reeled-in item has a chance to be doubled).
- Villager Hat (permanent trade discount, delivered as an infinite ambient **Hero of the Village** effect while carried - the same vanilla mechanic that discounts villager trades).

Not yet implemented (deferred for review-then-build):

- Remaining ticking/utility charms (Universal Attractor, Digging Claws, Pocket Piston, Charm of Sinking).
- Hard/partial-fidelity items (Umbrella).
- Loot-table / datapack sourcing (boss drops and structure loot).
- Resource-pack custom models (current items use placeholder materials).

## Equipment menu (curios)

Artifacts are equipped through the shared **LeafCurios** menu/API (this plugin `depend`s on LeafCurios) instead of taking up main-inventory space. Open it with `/artifact` (no args) or `/curios`.

- On enable, LeafArtifacts registers its curio slot types with LeafCurios (`ArtifactSlotDefs`): Head, Necklace, Hands x2, Ring, Charm x2, Feet x2. The two Feet slots let you combine footwear (e.g. Running Shoes + Bunny Hoppers).
- Empty slots show a light-gray glass placeholder naming the category. Click an artifact onto a matching slot to equip it; click an equipped artifact out to unequip. Shift-click an artifact in your inventory to auto-equip it into the first matching free slot.
- Each artifact declares the slot ids it accepts (`ArtifactType.slots()`), and its item is tagged via `CurioItems`; some accept more than one (e.g. Obsidian Skull = Head or Charm, Crystal Heart = Necklace or Charm).
- The active set (curios menu + any curio worn in a vanilla armour slot) is the single source of truth for which artifacts are active. LeafCurios persists it on the player's `PersistentDataContainer`, so it survives relogs and restarts. Equipment from the previous in-plugin store is migrated to LeafCurios once on join (`ArtifactLegacyMigration`).
- Future plan: once the feature set is final, replace/augment `/artifact` with an in-game written-book menu (as done for RTP), with discoverable lore notes scattered in the world.

## How it works

- An artifact is "active" while it is equipped in the curio menu (above) or worn in a vanilla armour slot (for armour-type artifacts such as Running Shoes / Steadfast Spikes); main-inventory copies do nothing.
- Artifact items are unbreakable, so wearing one as real armour never costs durability. Their placeholder materials otherwise have all in-world use denied (no planting/placing/throwing/filling), except: Eternal Steak can be eaten, and armour-type artifacts can be equipped to an armour slot.
- Leather artifacts can be dyed: because the vanilla dye recipe strips custom data, a `PrepareItemCraftEvent` handler re-stamps the artifact's identity (marker, name, lore, unbreakable) onto the dyed result while keeping the new colour.
- A per-player reconcile loop runs on the player's entity scheduler (Folia-safe; also supported on regular Paper). Each pass recomputes the desired attribute modifiers / infinite effects from the equipped set (`ArtifactEquipment`) and adds or removes only what changed. Equipping in the menu also triggers an immediate reconcile.
- Attribute modifiers are tagged with a stable per-(artifact, attribute) `NamespacedKey`, so reconciliation is idempotent and never double-stacks.
- Effects the plugin applies are tracked per player so they are removed when the artifact is unequipped, while externally-applied effects (e.g. a real potion) are left untouched.
- Event-reactive artifacts (P2 and the P4 charms) are handled in `ArtifactEventListener`, which checks the equipped set (`ArtifactEquipment.carries`) when the triggering event fires. The reactions act only on entities already involved in the event, so they stay on the correct region thread under Folia with no extra scheduling.
- Eternal Steak is the one non-equippable artifact: it declares no curio slots (so the menu rejects it and its lore reads "Consumable" instead of "Slot:"), and its effect is keyed off the item itself. On `PlayerItemConsumeEvent` for an Eternal Steak, the consume is cancelled (so the stack is never reduced) and the steak's nourishment is applied by hand.
- Cloud in a Bottle (P3) is handled in `CloudJumpListener`. Without an elytra: flight is kept enabled while equipped so the jump key fires `PlayerToggleFlightEvent`, which is cancelled and converted into a vertical impulse (scaled by `JUMP_STRENGTH`). With an elytra: flight is yielded so the glide deploys normally, and the charge is spent as a launch boost on `EntityToggleGlideEvent` (applied next tick so it isn't swallowed by the state change). One charge per airtime, refreshed on landing; flight is only managed in Survival/Adventure. Note: the no-elytra air-jump uses the vanilla double-tap-jump fly gesture.

## Testing

- `/artifact` opens the curio equipment menu (permission `leafartifacts.use`, default true).
- `/artifact give <type> [player]` (permission `leafartifacts.admin`, default op) hands out an artifact item for in-game review. Example: `/artifact give running_shoes`, then open `/artifact` and slot it.

## Build

Mirrors the other plugins in this repo (Gradle or Maven, Java 21, Paper 1.21.4 API):

- Gradle: `gradle jar` -> `build/libs/LeafArtifacts-0.1.0.jar`
- Maven: `mvn package` -> `target/LeafArtifacts-0.1.0.jar`
