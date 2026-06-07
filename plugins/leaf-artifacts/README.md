# LeafArtifacts

Exploration artifacts (wearable trinkets, inspired by the "Artifacts" mod) recreated with vanilla Paper mechanics. Folia-compatible.

See the design note: `docs/scratch/ARTIFACTS-VANILLA-RECREATION.md`.

## Status

Three passes implemented so far (P1, P2, and the first P3 movement item).

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

Not yet implemented (deferred for review-then-build):

- Remaining movement/ticking artifacts (Roller Skates, Helium Flamingo + Flippers, etc.).
- Loot-table / datapack sourcing (boss drops and structure loot).
- Resource-pack custom models (current items use placeholder materials).

## Equipment menu (curios)

Artifacts are equipped through a dedicated vanilla-UI menu instead of taking up main-inventory space. Open it with `/artifact` (no args).

- The menu is a single chest row (9 slots). Each slot is dedicated to a curio category: Head, Necklace, Hands x2, Ring, Charm x2, Feet x2. The two Feet slots let you combine footwear (e.g. Running Shoes + Bunny Hoppers).
- Empty slots show a light-gray glass placeholder naming the category. Click an artifact onto a matching slot to equip it; click an equipped artifact out to unequip. Shift-click an artifact in your inventory to auto-equip it into the first matching free slot.
- Each artifact declares which categories it accepts (see `ArtifactSlots`); some accept more than one (e.g. Obsidian Skull = Head or Charm, Crystal Heart = Necklace or Charm).
- The equipped set is the single source of truth for which artifacts are active. It is stored on the player's `PersistentDataContainer`, so it survives relogs and restarts.
- Future plan: once the feature set is final, replace/augment `/artifact` with an in-game written-book menu (as done for RTP), with discoverable lore notes scattered in the world.

## How it works

- An artifact is "active" while it is equipped in the curio menu (above); main-inventory copies no longer do anything.
- A per-player reconcile loop runs on the player's entity scheduler (Folia-safe; also supported on regular Paper). Each pass recomputes the desired attribute modifiers / infinite effects from the equipped set (`ArtifactEquipment`) and adds or removes only what changed. Equipping in the menu also triggers an immediate reconcile.
- Attribute modifiers are tagged with a stable per-(artifact, attribute) `NamespacedKey`, so reconciliation is idempotent and never double-stacks.
- Effects the plugin applies are tracked per player so they are removed when the artifact is unequipped, while externally-applied effects (e.g. a real potion) are left untouched.
- Event-reactive artifacts (P2) are handled in `ArtifactEventListener`, which checks the equipped set (`ArtifactEquipment.carries`) when the triggering event fires. The reactions act only on entities already involved in the event, so they stay on the correct region thread under Folia with no extra scheduling.
- Cloud in a Bottle (P3) is handled in `CloudJumpListener`. Without an elytra: flight is kept enabled while equipped so the jump key fires `PlayerToggleFlightEvent`, which is cancelled and converted into a vertical impulse (scaled by `JUMP_STRENGTH`). With an elytra: flight is yielded so the glide deploys normally, and the charge is spent as a launch boost on `EntityToggleGlideEvent` (applied next tick so it isn't swallowed by the state change). One charge per airtime, refreshed on landing; flight is only managed in Survival/Adventure. Note: the no-elytra air-jump uses the vanilla double-tap-jump fly gesture.

## Testing

- `/artifact` opens the curio equipment menu (permission `leafartifacts.use`, default true).
- `/artifact give <type> [player]` (permission `leafartifacts.admin`, default op) hands out an artifact item for in-game review. Example: `/artifact give running_shoes`, then open `/artifact` and slot it.

## Build

Mirrors the other plugins in this repo (Gradle or Maven, Java 21, Paper 1.21.4 API):

- Gradle: `gradle jar` -> `build/libs/LeafArtifacts-0.1.0.jar`
- Maven: `mvn package` -> `target/LeafArtifacts-0.1.0.jar`
