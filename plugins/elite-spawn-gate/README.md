# EliteSpawnGate

Server-specific anti-farm spawn gate for Iris elite mobs (Leaf's server only, per ADR-001).

The gate is biome-agnostic: it serves every signature biome, not just embervine. Which mobs
are gated and in which worlds is driven entirely by `config.yml` (`gated-types`, `worlds`).

## What it does

Iris owns mob spawning. This plugin does NOT spawn or define mobs. It only listens for
spawns of configured "elite" entity types and cancels the ones that occur in farm-like
geometry, so the rare loot-bearing elites (Tier 2) cannot be farmed while ordinary mob
farms and Tier-1 custom mobs keep working.

See `docs/scratch/CUSTOM-MOB-SPAWNS-PLAN.md` for the full design.

## How it identifies elites

By entity type (`gated-types` in `config.yml`). For example, in the Iris overworld the
Embervine Warden is a `WITHER_SKELETON`, which never spawns naturally in the overworld, so
that type uniquely identifies a gated elite. Additional elites from other biomes are added
by listing their types here. The common path for every other spawn is a single set lookup.

## v1 validity predicate

A gated spawn is cancelled (before the entity is added to the world) if it fails any of:

1. `require-natural-ground` - the block beneath must be a natural material.
2. `require-sky-access` - the spawn must be at (or within `surface-tolerance` blocks below)
   the terrain surface, measured by the `MOTION_BLOCKING_NO_LEAVES` heightmap so the leaf
   canopy is ignored. This keeps gated elites on the surface instead of underground caves.
3. `reject-artificial-nearby` - no more than `max-artificial-blocks` artificial blocks
   within `scan-radius`.

All rules and their material lists are configurable in `config.yml`.

## Build

Built with Gradle and a JDK 21+ (paper-api 1.21). From this directory:

```
gradle jar
```

The jar lands in `build/libs/EliteSpawnGate-0.1.0.jar`; drop it in the server `plugins/` folder.

> A `pom.xml` is also kept for Maven-based CI (`mvn -q clean package` -> `target/EliteSpawnGate-0.1.0.jar`).
> The built jar is not committed here.
