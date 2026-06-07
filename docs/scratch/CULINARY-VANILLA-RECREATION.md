# Culinary (Farmer's Delight-style) -> Vanilla (Paper/Folia) Recreation Plan

Working note. Goal: recreate the cooking/food content of Farmer's Delight-style mods (cooking, prepared meals, "comfort" foods, new crops with growth, and a kitchen Knife tool) using only vanilla mechanics on a Paper 1.21.x / Folia-compatible server.

## Status / scope

- Source of inspiration: Farmer's Delight (and adjacent "Culinary"/"Cuisine" content packs).
- Delivery target: a single Folia-supported Paper plugin (working name `net.leaf.culinary`) plus a layered vanilla datapack (recipes) and a resource pack (item textures via `custom_model_data`).
- Hard constraint from the owner: NO custom furniture and no large custom-block catalogue. Functional "stations" reuse existing vanilla blocks; placed multi-serving "feast" blocks are out of scope (see "Dropped / out of scope").
- This is a design scratch note, not a spec. Subject to Rule D-005 proposal before any implementation.

## Delivery mechanism decision: recipes-first, stations only when a recipe demands one

Two options were considered for "how does a player make a prepared meal":

1. Custom interactive stations (cooking pot / skillet / cutting board) with bespoke GUIs and processing loops.
2. Vanilla crafting/cooking recipes (datapack) producing the meal item directly, with no new station.

Decision: prefer vanilla recipes by default; introduce a station only when the fantasy genuinely needs one.

Rationale:

- No custom inventory/GUI plumbing, no per-block tick loops, and naturally Folia-safe (crafting events run on the right region thread).
- Matches the established porting pattern in `ARTIFACTS-VANILLA-RECREATION.md`: map the idea onto the nearest vanilla primitive instead of registering a brand-new thing.
- Keeps items as vanilla items carrying `custom_model_data` / `custom_data`, so they survive plugin removal and stay inspectable.

Station mapping when needed (no new blocks):

- "Cooking pot" (soups/stews) -> reuse the vanilla `smoker` (or `furnace`) recipe type for cooked-meal outputs, or a campfire-cooking recipe for slow simmer flavor. The bowl-based stews become items with `food`/`consumable` components.
- "Stove/skillet" -> folded into smoker/campfire recipes; purely cosmetic in the source mod.

Note: the Knife is NOT a cooking ingredient. It is a standalone custom tool/weapon (see "The Knife (custom tool/weapon)" below). It IS, however, the interaction tool for the optional Cutting Board station (see "Cutting Board station" below): you place an input on the board and left/right-click it with the knife in hand to "cut" it.

## Viability legend

- Easy = native item component, recipe, or one simple event; trivially Folia-safe.
- Medium = event logic and/or per-entity scheduling, standard API.
- Hard = needs emulation/ticking or only partial fidelity.

## Value legend (effort vs. payoff)

- Core = signature, do first; defines the "feels like Farmer's Delight" experience.
- Nice = clear value, second wave.
- Flavor = low-stakes additions for breadth.

## Dropped / out of scope

- Feasts as placed multi-serving blocks (Roast Chicken, Honey-Glazed Ham, Stuffed Pumpkin, Shepherd's Pie as blocks) -> out of scope per the "no custom furniture/blocks" constraint. Ship the same fantasy as a single high-value food item instead (e.g. a "Roast Chicken" food item that grants several feed ticks / strong saturation).
- Canvas signs / rope / decorative props -> decoration, not gameplay; skipped.
- Organic Compost -> Rich Soil pipeline's compost half -> redundant with the vanilla `composter` (already produces bone meal). Only the "rich soil" growth boost is worth emulating (see table).
- Plain cooked meats / honey bottle / mushroom stew / beetroot soup -> already vanilla; do not re-add.
- Cutting raw ingredients into "X pieces" purely to add steps -> only port the cut outputs that unlock a downstream meal recipe; skip busywork cuts.

## The Knife (custom tool/weapon)

The Knife is a custom tool/weapon, not a crafting ingredient consumed by recipes. Think "reskinned sword" with a distinct combat profile. It doubles as the interaction tool for the optional Cutting Board (see that section), but it is never itself an ingredient.

- Base item: a sword (e.g. `iron_sword`/`golden_sword`) carrying `custom_model_data` for the kitchen-knife sprite; optionally a `custom_data` marker so the plugin can identify it.
- Combat profile via item components (1.21.x): override `attribute_modifiers` to set the intended feel: HALF the base sword's attack damage and 1.5x its attack speed (a fast, light blade that hits for less per swing). Concretely, on an iron-sword base (6 attack damage, 1.6 attack speed in vanilla) that is ~3 attack damage and ~2.4 attack speed; tune the exact base item at proposal time. Optionally adjust reach via the `player.entity_interaction_range` / `player.block_interaction_range` attributes if a longer or shorter reach is wanted.
- Durability/material: pick a base whose durability and enchantability suit the intended tier; no plugin durability loop needed since it takes damage like any vanilla weapon.
- Obtaining it: a normal crafting recipe (e.g. iron + stick) so it is freely craftable.
- Folia: purely component-driven, so no scheduling concerns; any plugin identification runs on the firing event's region thread.

Settled feel: half base-sword damage, 1.5x base-sword attack speed. Remaining open question: which base sword/material to anchor those multipliers to, and whether to tweak reach.

## Cutting Board station (optional, item-frame-style)

In Farmer's Delight the cutting board is a placed block: you put an item on it and left/right-click with a knife to "cut" it into outputs. In-game it reads like a reskinned horizontal item frame (an item lying flat on a surface).

Vanilla recreation (no custom block, stays within the "no custom furniture" spirit by reusing an existing entity):

- Visual + holder: a vanilla `ItemFrame` (or `GlowItemFrame`) placed flat on the top face of a block so the held item lies horizontal, optionally with `custom_model_data` on a placeholder "board" item or a reskinned frame look. This is the "reskinned horizontal item frame" the source mod evokes.
- Interaction: handle `PlayerInteractEntityEvent` (and the left-click variant) on the frame - when a player left/right-clicks the frame while holding the tagged Knife, the plugin reads the framed input item, matches it against a cutting-recipe table, and swaps the framed item for the output (or drops the outputs).
- Knife wear: the cut action can apply durability to the Knife on the firing event's region thread (vanilla-style damage), keeping the "tool gets used" feel without a tick loop.
- This is OPTIONAL and gated: most prepared meals are reachable via plain crafting/smoker recipes (recipes-first decision above). The cutting board is only worth building if a meal genuinely needs a distinct "cut raw -> pieces" step that crafting cannot express satisfyingly.
- Folia: item-frame interaction events run on the frame's region thread; touch the frame, the item, and the knife only there. No global scheduler.

Open question: whether to ship the Cutting Board at all (it reintroduces a placed-entity station) versus folding all cutting into shapeless crafting recipes.

## "Comfort food" effect (the signature mechanic)

Farmer's Delight's prepared meals grant a long, mild "Comfort"/Nourishment buff rather than just filling hunger. Vanilla recreation:

- On eating a tagged meal, apply a short, gentle `REGENERATION` (low amplifier) plus generous `food`/`saturation` from the component. Optionally a brief `SATURATION` effect for the "stays full longer" feel.
- Prefer the item `consumable` component's `consume_effects` (datapack/components) for fixed effects; use the plugin only for stacking rules or duration scaling.
- Tiering: light snacks (short comfort), full meals (longer comfort), feast-tier items (longest comfort). Keep amplifiers low so it is QoL, not combat power.
- Folia: apply effects on the firing event thread (the player's region); no global scheduler.

## Feature table

Approach column = the nearest vanilla primitive. Texture column = the `custom_model_data` sprite to make.

### New crops / ingredients

| Feature | Vanilla recreation approach | Viability | Value | Notes / texture |
|---|---|---|---|---|
| Tomato | Custom crop emulated on a vanilla plantable (reuse a berry/`sweet_berry_bush`-style or a `custom_model_data` item harvested from a marked plot); fruit is an item with `food` component | Medium | Core | Red fruit sprite; growable crop (P4), trade/loot-sourced in the interim (P3) |
| Onion | Same crop pattern as Tomato; trade/loot-sourced until the crop system lands | Medium | Nice | Onion sprite |
| Cabbage | Same crop pattern; leaf-style food | Medium | Flavor | Cabbage sprite |
| Rice | Wetland crop; emulate with a marked waterside plot or craft "rice" from existing items | Hard | Nice | Rice grain + cooked rice sprites |

The Knife is a custom tool/weapon, not an ingredient; it is covered in its own section ("The Knife (custom tool/weapon)") rather than this ingredients table.

Custom crop growth IS in scope (build it when we get to it - see P4). Until the crop system lands, these ingredients can be SOURCED from loot/villager trades so meals are craftable early, but the end state is real plantable/growable crops, not a permanent trade-only workaround.

### Prepared meals (food items)

| Meal | Vanilla recreation approach | Viability | Value | Notes / texture |
|---|---|---|---|---|
| Vegetable Soup | Smoker/crafting recipe -> bowl food item; Comfort effect | Easy | Core | Bowl with greens |
| Beef Stew | Recipe -> bowl food; strong saturation + Comfort | Easy | Core | Bowl with beef chunks |
| Chicken Soup | Recipe -> bowl food; Comfort | Easy | Core | Bowl with broth |
| Noodle Soup | Recipe -> bowl food; Comfort | Easy | Nice | Bowl with noodles |
| Hamburger | Crafting recipe -> food item; high feed | Easy | Core | Stacked burger |
| Fried Egg | Smoker/campfire recipe from egg -> food | Easy | Flavor | Fried egg |
| Cooked Rice / Rice Roll | Recipe -> food | Easy | Nice | Rice ball/roll |
| Dumplings | Recipe -> food | Easy | Flavor | Dumpling |
| Stuffed Potato | Recipe (baked potato base) -> food | Easy | Flavor | Loaded potato |
| Ratatouille | Recipe (needs tomato/onion/cabbage) -> food; Comfort | Medium | Nice | Veg medley bowl |
| Glow Berry Custard | Recipe (glow berries + egg + sugar) -> food | Easy | Flavor | Custard cup |
| Hot Cocoa | Campfire recipe -> drink item; brief warmth/Comfort | Easy | Flavor | Mug of cocoa |
| Pie (sweet berry / apple) | Recipe -> multi-bite food (custom_model_data per bite optional) | Medium | Nice | Pie slice |
| "Roast Chicken" (ex-feast) | Single high-value food item replacing the placed feast block | Easy | Core | Roast bird |
| "Honey-Glazed Ham" (ex-feast) | Single high-value food item | Easy | Nice | Glazed ham |

### Mechanics / world

| Feature | Vanilla recreation approach | Viability | Value | Notes |
|---|---|---|---|---|
| Comfort food buff | `consume_effects` (components) + plugin for stacking/tiering | Easy | Core | See dedicated section above |
| Rich Soil (faster crop growth) | Plugin marks a block (PDC on the chunk/block) and periodically random-ticks/bonemeals crops above it on the owning region thread; OR simply lean on vanilla bone meal and skip | Medium | Nice | Avoid a heavy block-state system; cap tick cost |
| Campfire/skillet cooking flavor | Vanilla `campfire` + `smoker` recipe types | Easy | Nice | No new block |
| Cutting Board (cut-with-knife) | Reskinned horizontal `ItemFrame` holding the input; knife left/right-click swaps it for the cut output | Medium | Nice | Optional, P3.5; see "Cutting Board station" |
| Serving on plates | Skip (decorative); meals are just items | n/a | n/a | Out of scope |

## Sourcing (where new ingredients/recipes come from)

Consistent with the artifacts plan's two channels and the layered-datapack convention:

- Recipes: a layered vanilla datapack (smoker/campfire/crafting), applied on top, not baked into Iris.
- Seed/ingredient acquisition: villager trades and structure/loot injection (same layered-datapack approach used for the deferred "enchanted books reward"), so a player can bootstrap the crop loop without a from-scratch crop framework.
- Knife (tool/weapon): a crafting recipe (iron + stick) so it is freely obtainable; see "The Knife (custom tool/weapon)".

## Folia compatibility checklist (for implementation)

- Set `folia-supported: true` in `paper-plugin.yml`.
- Never use `Bukkit.getScheduler()`; use entity (`player.getScheduler()`), region (`Bukkit.getRegionScheduler()`), or global (`Bukkit.getGlobalRegionScheduler()`) schedulers.
- Eat/craft event handlers already run on the correct region thread; apply effects and modify items there.
- Rich Soil ticking (if built) must run on the owning region scheduler for the soil block's chunk, and must be cheap (batch/throttle, hard cap per tick).
- Touch each entity/world/block only from its owning region thread.

## Application prioritization

Build order, highest-leverage first. Each phase is independently shippable.

- P0 - Foundation: plugin skeleton (`net.leaf.culinary`, `folia-supported: true`), item-marker scheme (`custom_data` + `custom_model_data`), resource-pack mapping pipeline, and the Comfort-effect helper.
- P1 - Core meals (no station): the "Core" food items above via crafting/smoker recipes + Comfort effect. Highest payoff, lowest risk.
- P2 - Knife (tool/weapon): the custom sword-based Knife item with its combat profile (half-damage, 1.5x attack speed) and crafting recipe.
- P3 - Ingredients sourcing (interim): villager trades / loot injection for tomato/onion/etc.; "Nice" meals that depend on them, available before the crop system lands.
- P3.5 - Cutting Board station (optional): item-frame-based board + knife-interaction cutting recipes, only if a meal needs a distinct cut step (see "Cutting Board station").
- P4 - Crop system: marked-plot (or vanilla-plantable) growth for Tomato/Onion/Cabbage/Rice. In scope - build when we get to it; the P3 sourcing is just the bridge until then.
- P5 - Rich Soil growth boost (layers on top of P4).
- P6 - Flavor breadth: remaining "Flavor" meals and drinks.

Open questions to settle before P0:
- Which base sword/material anchors the Knife's half-damage / 1.5x-speed profile, and whether to adjust reach.
- Whether to ship the Cutting Board station at all vs. folding cutting into shapeless crafting.
- Crop growth implementation: marked-plot plugin tick vs. a vanilla-plantable emulation.

Implementation is gated behind a Rule D-005 proposal (file layout for `net.leaf.culinary` + recipe list + texture list) before any code.
