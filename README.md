# LeafsServerBaseline

A baseline repository for Minecraft server development. Contains scripts, datapacks, plugins, configs, and documentation following a docs-first development cycle.

## Repository Structure

```
LeafsServerBaseline/
|- configs/          # Server configuration files (server.properties, etc.)
|- datapacks/        # Custom Minecraft datapacks
|- docs/
|  |- design/        # Technical design docs and ADRs
|  |  |- adr/        # Architecture Decision Records
|  |- requirements/  # Feature and system requirements
|  |- scratch/       # Brainstorming notes, memory assistance, checklists
|  |- usage/         # Player and admin guides
|- plugins/          # Plugin source or configuration
|- scripts/          # Automation scripts (deployment, backups, toolchain)
```

## Docs-First Workflow

Every new feature or change follows this sequence:

1. **Requirements** - define the goal in `docs/requirements/`
2. **Design** - draft the technical approach in `docs/design/`
3. **Implementation** - write code/datapacks/scripts only after design is settled
4. **Usage** - document the result in `docs/usage/`
5. **Scratch** - use `docs/scratch/` throughout for brainstorming and notes

## Getting Started

See [`docs/README.md`](docs/README.md) for the full documentation index.
