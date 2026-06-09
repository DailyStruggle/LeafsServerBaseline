# LeafCurios

A Curios-style trinket/accessory slot API for Paper/Folia servers. It provides
the shared "equip an accessory into a dedicated slot" system that other plugins
(e.g. LeafArtifacts) build on, instead of each plugin shipping its own menu.

## What it provides

- A dynamic slot-type registry (`SlotType`): id, display name, icon, size, and an
  optional validator. Slot types can be added/removed at runtime.
- A per-player curio store, persisted in the player's `PersistentDataContainer`
  (survives relog/restart), keyed by slot id.
- A shared vanilla-UI menu rendered from the registered slot types (`/curios`).
- A cross-plugin item contract (`CurioItems`): tag any `ItemStack` with the slot
  ids it fits, no dependency on internals.
- Equip/unequip events (`CurioEquipEvent`, `CurioUnequipEvent`).
- Generic "wear-as-armour also counts": a curio-tagged item worn in a vanilla
  armour slot is reported as active alongside menu-equipped curios.

## For consumers

1. Add `depend: [LeafCurios]` to your `plugin.yml`.
2. In your `onEnable`, get the service: `CuriosApi api = CuriosProvider.get();`
   (or via the Bukkit `ServicesManager`).
3. Register the slot types you need:
   `api.registerSlotType(SlotType.builder("charm").displayName("Charm").icon(Material.AMETHYST_SHARD).size(2).build());`
4. Tag your items: `CurioItems.setSlots(item, List.of("charm"));`
5. Query what's active: `api.getEquippedItems(player)` / `api.isEquipped(player, predicate)`.
6. React to changes by listening for `CurioEquipEvent` / `CurioUnequipEvent`.

## Commands

- `/curios` - open the curios equipment menu (permission `leafcurios.use`, default true).
- `/curios slot list` - list registered slot types.
- `/curios slot add <id> [display] [icon] [size]` - register a slot type at runtime.
- `/curios slot remove <id>` - remove a slot type.
  (slot management requires `leafcurios.admin`, default op.)

## Build

Mirrors the other plugins in this repo (Gradle or Maven, Java 21, Paper 1.21.4 API):

- Gradle: `gradle jar` -> `build/libs/LeafCurios-0.1.0.jar`
- Maven: `mvn package` -> `target/LeafCurios-0.1.0.jar`

Deploy `LeafCurios-0.1.0.jar` alongside any plugin that depends on it.
