# fix-potion-entity

Rewrites the removed `minecraft:potion` ENTITY type baked into structure (`.nbt`)
templates to `minecraft:splash_potion`.

## Why

In 1.21.5 the single thrown-potion entity `minecraft:potion` was split into
`minecraft:splash_potion` and `minecraft:lingering_potion`. Pre-split structure
templates (e.g. `dungeons-arise`, `dungeons-arise-seven-seas`) still bake the old id
into their (trial) spawner mob data, typically as a passenger. On a modern server the
entity loader spams:

```
Skipping Entity with id minecraft:potion
[net.minecraft.world.entity.EntityType] Serialization errors:
.Passengers[0]: Failed to decode value '"minecraft:potion"' from field 'id': Unknown registry key in ResourceKey[minecraft:root / minecraft:entity_type]: minecraft:potion
```

and silently drops the entity.

## What it touches (and what it does NOT)

It only rewrites an **entity** `id` of `minecraft:potion`:

- the structure-level `entities` list (each element's `nbt` compound), and
- any spawner `entity` compound (`SpawnData.entity`, `SpawnPotentials[].data.entity`,
  `spawn_data.entity`, `normal_config/ominous_config.spawn_potentials[].data.entity`),
- plus each entity's nested `Passengers`.

It deliberately leaves the still-valid **item** id `minecraft:potion` (inside
`Items` / `HandItems` / `Item` / villager `buy`/`sell`) untouched.

## Usage

```powershell
& "C:\Users\lxgol\AppData\Local\Programs\Python\Python313\python.exe" tools\fix-potion-entity\fix_potion_entity.py datapacks
& "C:\Users\lxgol\AppData\Local\Programs\Python\Python313\python.exe" tools\fix-potion-entity\fix_potion_entity.py datapacks --apply
```

Without `--apply` it is a dry run. It reuses `tools/despawnerize/nbt_io.py` for a
byte-faithful gzip NBT round-trip and only rewrites files that actually change.
