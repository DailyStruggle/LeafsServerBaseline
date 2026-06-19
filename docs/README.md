# Documentation Index

Central hub for all project documentation.

> **Worldgen direction change (see [ADR-005](design/adr/ADR-005-retire-iris-for-vanilla-datapack-worldgen.md)):** The Iris Dimension Engine has been retired in favor of vanilla datapack worldgen. The Iris pack is archived (read-only) under `legacy/iris/`; the tree generator is now an active tool at `tools/tree-gen/`. The Iris-specific design/world-design/scratch docs have been archived alongside it under [`legacy/iris/docs/`](../legacy/iris/docs/) for reference and asset salvage.

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
- [`design/adr/ADR-005-retire-iris-for-vanilla-datapack-worldgen.md`](design/adr/ADR-005-retire-iris-for-vanilla-datapack-worldgen.md) - retire Iris in favor of vanilla datapack worldgen
- [`design/adr/`](design/adr/) - Architecture Decision Records
- [`scratch/LESSONS_LEARNED.md`](scratch/LESSONS_LEARNED.md) - engineering pitfalls and notes
- [`scratch/POTENTIAL_BUGS.md`](scratch/POTENTIAL_BUGS.md) - incidental findings backlog
- [`usage/DEPLOYMENT.md`](usage/DEPLOYMENT.md) - deployment guide: modular datapack/plugin deploy scripts, switches, and post-deploy steps
- [`usage/TREE-GENERATION.md`](usage/TREE-GENERATION.md) - tree-generation pipeline guide: how it works, the JSON config schema, and outputs
