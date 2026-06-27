# despawnerize (structure spawner -> trial spawner)

Converts the farmable plain `minecraft:spawner` blocks shipped inside the
third-party structure packs into non-farmable vanilla `minecraft:trial_spawner`
blocks, so those structures still ambush the player (and eject earned loot) but
can no longer be farmed.

## Files

- `nbt_io.py` - a small, dependency-free Java-edition NBT reader/writer
  (gzip-aware) covering the full tag set used by structure `.nbt` templates.
- `trial_spawnerize.py` - the converter. Walks `datapacks/`, rewrites each
  affected template, and is idempotent.

## Usage

The bare `python` / `py` aliases on this machine are broken Windows Store stubs;
use the real interpreter:

```
# preview (no writes)
& "C:\Users\lxgol\AppData\Local\Programs\Python\Python313\python.exe" tools\despawnerize\trial_spawnerize.py --dry-run

# apply
& "C:\Users\lxgol\AppData\Local\Programs\Python\Python313\python.exe" tools\despawnerize\trial_spawnerize.py

# limit to specific pack folders
... trial_spawnerize.py --only structory,explorify
```

Normally you do not run it by hand: `datapacks/_patch-trial-spawners.ps1` wraps
it and is executed by `datapacks/patch-all.ps1` (and thus by
`deploy-datapacks.ps1`) before the copy step.

## Behaviour

See `docs/design/STRUCTURE-PACK-INTEGRATION.md` -> *Anti-farm: trial spawners*
for the full rules (mob carry-over, loot ladder, the dungeon/mineshaft/ancient
exemption, trap-spawner skip, and the 1.21+ requirement).
