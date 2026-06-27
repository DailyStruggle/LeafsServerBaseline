# strip-forge-attrs

One-off maintenance tool that removes Forge-only entity attributes baked into
Minecraft structure (`.nbt`) templates shipped by Forge-converted datapacks.

## Why

Forge-converted packs (When Dungeons Arise, Seven Seas) store mob attribute lists
that include Forge-namespaced ids such as `forge:swim_speed`. On a vanilla/Paper
server these are unknown registry keys, so the structure template loader spams:

```
[WARN] StructureTemplate Serialization errors:
 Failed to decode value '[{base:1.0d,id:"forge:swim_speed"},...]' from field
 'attributes': Unknown registry key in ResourceKey[minecraft:root /
 minecraft:attribute]: forge:swim_speed
```

The templates use the legacy capitalised `Attributes` / `Name` / `Base` format;
Minecraft's DataFixerUpper upgrades it to the modern `attributes` / `id` / `base`
form on load and then trips on the unknown id. Removing the entry just lets the
mob fall back to its default value for that attribute.

## Usage

```powershell
& "C:\Users\lxgol\AppData\Local\Programs\Python\Python313\python.exe" `
    tools\strip-forge-attrs\strip_forge_attributes.py datapacks            # dry run
& "C:\Users\lxgol\AppData\Local\Programs\Python\Python313\python.exe" `
    tools\strip-forge-attrs\strip_forge_attributes.py datapacks --apply    # write
```

- Dry run by default; pass `--apply` to write changes.
- `--namespace <ns>` strips a different namespace (default `forge`).
- Reuses `tools/despawnerize/nbt_io.py` for byte-faithful gzip NBT round-trips.
- Only files that actually change are rewritten.
