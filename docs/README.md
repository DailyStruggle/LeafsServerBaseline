# Documentation Index

Central hub for all project documentation.

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
- [`design/adr/`](design/adr/) - Architecture Decision Records
- [`scratch/LESSONS_LEARNED.md`](scratch/LESSONS_LEARNED.md) - engineering pitfalls and notes
- [`scratch/POTENTIAL_BUGS.md`](scratch/POTENTIAL_BUGS.md) - incidental findings backlog
- [`world-design/IRIS-WORLD-BUILDING.md`](world-design/IRIS-WORLD-BUILDING.md) - end-to-end Iris biome authoring walkthrough
