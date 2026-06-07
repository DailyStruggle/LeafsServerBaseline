# LeafBackpacks

A "Sophisticated-lite" backpack plugin for Leaf's Paper 1.21.x server. It recreates
the core of the Sophisticated Backpacks mod with vanilla-friendly mechanics:

- A backpack is a single item that opens a 27-slot storage GUI on right-click.
- Contents and installed upgrades are stored **off-item by UUID** (dupe-safe): the
  item only carries an id + tier in its `PersistentDataContainer`.
- **Tiers gate how many upgrade slots (functions)** a backpack has - not the size of
  the main grid (which stays 27).
- **Upgrade modules** drive behaviour:
  - `STACK` - raises the max stack size of items stored inside (engine cap 99). Items
    normalise to vanilla stack rules when withdrawn (parity with backpack mods).
  - `MAGNET` - pulls nearby dropped items toward the holder.
  - `PICKUP` - routes picked-up items straight into a carried backpack.
  - `FEEDING` - auto-eats food from the backpack when hunger is low.
  - `VOID` - reserved (filtering UI not implemented yet); inert.

## Commands

`/backpack` (permission `leafbackpacks.admin`):

- `/backpack give [tier]` - get a backpack (default tier 1).
- `/backpack upgrade <type>` - get an upgrade module (`stack`, `magnet`, `pickup`, `feeding`).
- `/backpack tierup` - raise the held backpack one tier, consuming the configured cost
  (default **4 ingots**) of the next tier's material (iron -> gold -> diamond -> netherite).

Install upgrade modules by opening the backpack and placing them in the unlocked
upgrade slots in the bottom row (locked slots are greyed out until you tier up).

## Crafting upgrade modules (rare materials)

Like Sophisticated Backpacks, each upgrade module is crafted from a recipe of rare
materials (the centre slot is the "rare core" that identifies the module):

- `STACK` - diamonds + pistons around a **netherite ingot**.
- `MAGNET` - iron + redstone + ender pearls around a **gold ingot**.
- `PICKUP` - string + hoppers around an **ender eye**.
- `FEEDING` - gold + golden carrots around a **golden apple**.

By default modules render as glinting **"upgrade enchantment books"** (`upgrades.as-book`),
so they read as enchantment-book-style upgrades rather than plain items. Set
`upgrades.as-book: false` to use each module's plain icon material, or
`upgrades.craftable: false` to disable the recipes and hand modules out only via
`/backpack upgrade <type>`.

## Optional custom icon (player-optional resource pack)

`custom-icon` in `config.yml` is **disabled by default**. When enabled, the backpack
item uses the `minecraft:item_model` component pointing at `leafbackpacks:backpack`
(per-tier overrides supported). The art ships in the optional resource pack under
`resourcepack/` (drop a `backpack.png` into `assets/leafbackpacks/textures/item/`).

This is a **server-sent, player-optional** pack: a client that declines it keeps a
fully functional backpack, but with `custom-icon` enabled it will render with the
missing-model look. Leave `custom-icon.enabled: false` to fall back to a plain vanilla
base item (`SHULKER_BOX` by default) for everyone.

## Building

Built with Gradle and a JDK 21+ (paper-api 1.21). From this directory:

```
gradle jar
```

The jar lands in `build/libs/LeafBackpacks-0.1.0.jar` (Maven: `mvn package` ->
`target/LeafBackpacks-0.1.0.jar`). Copy it into the server's `plugins/` folder.

## Known limitations / follow-ups

- One real 27-slot grid per backpack; tiers add upgrade slots, not grid size.
- Stack-boost normalisation on withdrawal is best-effort across all click paths.
- Backpacks themselves are still distributed via the admin command; only upgrade
  modules are craftable so far.
- `VOID` upgrade is a reserved placeholder pending a filter UI.
