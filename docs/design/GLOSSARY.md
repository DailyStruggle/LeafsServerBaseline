# Glossary

Canonical term definitions for this project. Use these names in code, configs, docs, and ADRs. Add new terms here before using them in implementation.

| Term | Definition |
|------|------------|
| datapack | A Minecraft data-driven content bundle (functions, advancements, loot tables, etc.) loaded by the server at runtime. |
| plugin | A server-side Java mod loaded by the server platform (Paper, Spigot, etc.) to extend server behavior. |
| config | A server or plugin configuration file (YAML, TOML, properties) that controls runtime behavior without code changes. |
| script | An automation script (PowerShell, Python, shell) used for deployment, backups, or toolchain tasks. |
| additive biome | A custom biome added on top of the vanilla biome set via Iris Dimension Engine, without replacing any existing vanilla biome. |
| derivative | The vanilla `minecraft:` biome ID that Iris sends to the client for a custom biome, controlling ambient sound, sky color, fog, and music. |
| vanillaDerivative | The vanilla `minecraft:` biome ID used server-side by Iris for mob spawning tables, weather, and game rule lookups. |
| climate vector | The six-component noise vector `<T, H, C, E, W, D>` (temperature, humidity, continentalness, erosion, weirdness, depth) used by Iris to select which biome generates at a given coordinate. |
| speleological biome | A cave or underground biome defined in Iris using the `carving`, `magnetics`, or `prismatics` category folders, selected by the depth (D) component of the climate vector. |
