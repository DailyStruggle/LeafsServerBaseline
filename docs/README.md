# Documentation Index

Central hub for all project documentation.

> **Worldgen direction change (see [ADR-005](design/adr/ADR-005-retire-iris-for-vanilla-datapack-worldgen.md)):** The Iris Dimension Engine has been retired in favor of vanilla datapack worldgen. The Iris pack is archived (read-only) under `legacy/iris/`; the tree generator is now an active tool at `tools/tree-gen/`. Iris-specific design/usage docs below describe the retired system and are kept for reference and asset salvage.

## Sections

| Folder | Purpose |
|--------|---------|
| [`requirements/`](requirements/) | What we are building - feature goals, constraints, acceptance criteria |
| [`design/`](design/) | How we are building it - technical specs, architecture, ADRs |
| [`usage/`](usage/) | Player and admin guides |
| [`world-design/`](world-design/) | Narrative process guides for world and biome authoring |
| [`scratch/`](scratch/) | Brainstorming notes, checklists, lessons learned, potential bugs |

## Docs-First Workflow

```
requirements/ --> design/ --> implementation --> usage/
                                    ^
                              scratch/ (throughout)
```

New work always starts with a requirements doc. No implementation begins without a corresponding design doc.

## Key Files

- [`requirements/REQ-002-leafrtp-biome-engagement-tracking.md`](requirements/REQ-002-leafrtp-biome-engagement-tracking.md) - biome engagement tracking plugin (sits atop LeafRTP)
- [`design/GLOSSARY.md`](design/GLOSSARY.md) - canonical term definitions
- [`design/BIOMES.md`](design/BIOMES.md) - vanilla biome ID reference (Java Edition 1.21)
- [`design/CUSTOM-BIOMES.md`](design/CUSTOM-BIOMES.md) - custom biome catalog, Iris schema, and build palette reference
- [`design/IRIS-BIOME-RESEARCH.md`](design/IRIS-BIOME-RESEARCH.md) - preexisting Iris overworld biomes evaluated against ADR-003 criteria, with Gemini concept mapping
- [`design/IOB-FILE-FORMAT.md`](design/IOB-FILE-FORMAT.md) - binary spec for Iris `.iob` objects, derived from our `nbt_to_iris.py` / `iob_inspect.py` / `recenter-iob.py` tooling
- [`design/TILEDATA-FORMAT.md`](design/TILEDATA-FORMAT.md) - binary spec for the Iris `TileData` tile-entity records that follow the `.iob` block list (modern key+JSON and legacy ordinal encodings)
- [`design/IRIS-V4-STRUCTURES.md`](design/IRIS-V4-STRUCTURES.md) - how Iris 4.0 loads and places structures (`importedStructures`, `structures`/`IrisStructurePlacement`, jigsaw pools/pieces) and the legacy `jigsawStructures` migration gap
- [`design/IRIS-NETHER-GENERATION.md`](design/IRIS-NETHER-GENERATION.md) - methodology and research for authoring an Iris-generated Nether (dimension, regions, biomes, structures)
- [`design/VANILLA-TERRAIN-SHAPE.md`](design/VANILLA-TERRAIN-SHAPE.md) - intended vanilla terrain shape per biome archetype and the Iris v4 levers (height band, interpolation, composite octaves, cliffs) to stop over-smoothing
- [`design/adr/ADR-005-retire-iris-for-vanilla-datapack-worldgen.md`](design/adr/ADR-005-retire-iris-for-vanilla-datapack-worldgen.md) - retire Iris in favor of vanilla datapack worldgen
- [`design/adr/`](design/adr/) - Architecture Decision Records
- [`scratch/LESSONS_LEARNED.md`](scratch/LESSONS_LEARNED.md) - engineering pitfalls and notes
- [`scratch/POTENTIAL_BUGS.md`](scratch/POTENTIAL_BUGS.md) - incidental findings backlog
- [`world-design/IRIS-WORLD-BUILDING.md`](world-design/IRIS-WORLD-BUILDING.md) - end-to-end Iris biome authoring walkthrough
- [`usage/DEPLOYMENT.md`](usage/DEPLOYMENT.md) - deployment guide: modular datapack/plugin deploy scripts, switches, and post-deploy steps
- [`usage/TREE-GENERATION.md`](usage/TREE-GENERATION.md) - tree-generation pipeline guide: how it works, the JSON config schema, and outputs
