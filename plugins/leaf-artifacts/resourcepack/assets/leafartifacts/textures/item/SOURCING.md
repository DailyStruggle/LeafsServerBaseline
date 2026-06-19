# Artifact texture sourcing

One 32x32 PNG per artifact (Faithful 32x resolution), named `<id>.png`, dropped into this folder
(`assets/leafartifacts/textures/item/`). Each is wired up by:

- `assets/leafartifacts/models/item/<id>.json` - `layer0` -> `leafartifacts:item/<id>`
- `assets/leafartifacts/items/<id>.json` - the item-model definition
- `ArtifactItem.create` - sets the `minecraft:item_model` component to `leafartifacts:<id>`

So adding `<id>.png` here is the only step left to give an artifact its real icon.

Art must be original (or appropriately licensed) work: do **not** copy sprites from
the ochotonida "Artifacts" mod or any other third-party pack. The briefs below are
the design intent (see `docs/scratch/ARTIFACTS-VANILLA-RECREATION.md`), not a request
to trace existing art.

| id | model id | texture brief (32x32, Faithful 32x style) |
|---|---|---|
| running_shoes | leafartifacts:running_shoes | Worn sneaker/trainer with motion lines; warm palette |
| crystal_heart | leafartifacts:crystal_heart | Faceted red/pink crystalline heart |
| steadfast_spikes | leafartifacts:steadfast_spikes | Spiked iron sabatons/cleats |
| feral_claws | leafartifacts:feral_claws | Curved claw blades over knuckles |
| power_glove | leafartifacts:power_glove | Reinforced studded gauntlet |
| lucky_scarf | leafartifacts:lucky_scarf | Green four-leaf-clover scarf |
| night_vision_goggles | leafartifacts:night_vision_goggles | Brass/leather goggles |
| snorkel | leafartifacts:snorkel | Dive mask plus snorkel tube |
| scarf_of_invisibility | leafartifacts:scarf_of_invisibility | Translucent flowing scarf |
| obsidian_skull | leafartifacts:obsidian_skull | Carved black obsidian skull |
| vampiric_glove | leafartifacts:vampiric_glove | Dark red glove with fang motif |
| flame_pendant | leafartifacts:flame_pendant | Orange teardrop gem pendant |
| shock_pendant | leafartifacts:shock_pendant | Yellow lightning-bolt pendant |
| panic_necklace | leafartifacts:panic_necklace | Beaded amber necklace |
| onion_ring | leafartifacts:onion_ring | Golden ring shaped like a battered onion ring |
| golden_hook | leafartifacts:golden_hook | Ornate golden fish-hook |
| fire_gauntlet | leafartifacts:fire_gauntlet | Molten-cracked gauntlet with ember glow |
| bunny_hoppers | leafartifacts:bunny_hoppers | Springy rabbit-foot boots with a bounce accent |
| cloud_in_a_bottle | leafartifacts:cloud_in_a_bottle | Glass bottle with a swirling white cloud inside |
| roller_skates | leafartifacts:roller_skates | Skate boot with wheels; small speed-streak accent |
| helium_flamingo | leafartifacts:helium_flamingo | Pink inflatable flamingo ring |
| flippers | leafartifacts:flippers | Green/teal swim fins |
| umbrella | leafartifacts:umbrella | Red-and-white pinwheel umbrella |
| charm_of_sinking | leafartifacts:charm_of_sinking | Anchor/weight charm on a chain |
| pocket_piston | leafartifacts:pocket_piston | Tiny redstone-piston trinket |
| universal_attractor | leafartifacts:universal_attractor | Horseshoe magnet with sparkles |
| cross_necklace | leafartifacts:cross_necklace | Silver cross pendant on a chain |
| antidote_vessel | leafartifacts:antidote_vessel | Small green vial with cork |
| thorn_pendant | leafartifacts:thorn_pendant | Green thorn/bramble pendant |
| digging_claws | leafartifacts:digging_claws | Iron mining-claw grips |
| superstitious_hat | leafartifacts:superstitious_hat | Pointed witch-style hat |
| anglers_hat | leafartifacts:anglers_hat | Fishing cap with hooks/lures |
| villager_hat | leafartifacts:villager_hat | Wide-brim straw/village hat |
| eternal_steak | leafartifacts:eternal_steak | Perpetually sizzling steak |
