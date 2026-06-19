# schem-to-nbt

Dependency-free converter from a WorldEdit **Sponge schematic** (`.schem`, gzipped
NBT, v2 or v3) to a vanilla Minecraft **structure template** (`.nbt`) that a
datapack jigsaw structure can place via a `single_pool_element`.

No external libraries are needed - it ships a minimal NBT reader/writer. Requires a
JDK (tested with Adoptium JDK 21).

## Usage (PowerShell)

```
$jdk = "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin"
& "$jdk\javac.exe" SchemToNbt.java VerifyNbt.java
& "$jdk\java.exe" SchemToNbt <input.schem> <output.nbt> [dataVersion]
& "$jdk\java.exe" VerifyNbt <output.nbt>   # optional round-trip sanity check
```

- `dataVersion` is optional; by default the schematic's own `DataVersion` is copied
  through (so the game's data-fixers run against the correct version).
- Block-entity NBT is carried across (`Id` -> `id`, `Pos` stripped). Entities are
  dropped. Every cell - including `minecraft:air` - is emitted, matching
  structure-block export behaviour (the air clears the build's interior box).

## Provenance / licensing

Source schematics may be third-party-derived (e.g. abfielder.com). Per
[ADR-004](../../docs/design/adr/ADR-004-third-party-derived-structure-assets.md)
such assets are for our private, non-commercial server only and are never
redistributed. Keep source `.schem`/`.mcstructure` files out of the repo; only the
converted `.nbt` we actually deploy lives under a datapack.
