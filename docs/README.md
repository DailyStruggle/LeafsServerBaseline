# Documentation Index

Central hub for all project documentation.

## Sections

| Folder | Purpose |
|--------|---------|
| [`requirements/`](requirements/) | What we are building - feature goals, constraints, acceptance criteria |
| [`design/`](design/) | How we are building it - technical specs, architecture, ADRs |
| [`usage/`](usage/) | Player and admin guides |
| [`scratch/`](scratch/) | Brainstorming notes, checklists, lessons learned, potential bugs |

## Docs-First Workflow

```
requirements/ --> design/ --> implementation --> usage/
                                    ^
                              scratch/ (throughout)
```

New work always starts with a requirements doc. No implementation begins without a corresponding design doc.

## Key Files

- [`design/GLOSSARY.md`](design/GLOSSARY.md) - canonical term definitions
- [`design/adr/`](design/adr/) - Architecture Decision Records
- [`scratch/LESSONS_LEARNED.md`](scratch/LESSONS_LEARNED.md) - engineering pitfalls and notes
- [`scratch/POTENTIAL_BUGS.md`](scratch/POTENTIAL_BUGS.md) - incidental findings backlog
