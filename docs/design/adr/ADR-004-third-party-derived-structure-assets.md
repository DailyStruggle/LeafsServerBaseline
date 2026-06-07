# ADR-004: Third-party derived structure assets quarantine

**Status:** Accepted  
**Date:** 2026-06-06  
**Author:** Junie (with server owner)

---

## Context

We want to populate biomes with structures, and a strong source is preexisting structure packs
such as Towns & Towers. T&T ships as a mod (depends on Cristel Lib), but its jar is a plain zip
whose `data/<namespace>/` tree contains standard datapack-format assets: vanilla gzip `.nbt`
pieces plus `worldgen/template_pool`, `worldgen/structure`, and `structure_set` JSON. Iris
replaces the mod's placement layer, so the pieces can be extracted and converted to Iris objects
without running the mod.

The constraint is legal, not technical. T&T is licensed **CC BY-NC-ND 4.0**. CC BY-NC-ND 4.0
Section 2 explicitly permits *producing* adapted material for private, non-commercial use but
forbids **Sharing** adapted material. Our server is private and non-commercial, so extraction and
conversion are permitted - provided the derivatives are never redistributed or published.

## Decision

Introduce a single, clearly-marked quarantine directory `iris/thirdparty-derived/` for all
extracted/converted third-party structure assets.

- A tracked `README.md` documents the redistribution policy (private, non-commercial, never
  publish derivatives; keep attribution).
- Root `.gitignore` excludes everything under `iris/thirdparty-derived/` except that `README.md`,
  so derived `.nbt`/Iris objects are never committed.
- Derived assets are merged only into the already-git-ignored `iris/staging/` tree at build time,
  never into the tracked `iris/pack-overlay/` tree.

## Consequences

- Positive: license-restricted derivatives live in one obvious, version-control-excluded place;
  the policy is documented in-repo and survives independently of chat history.
- Positive: defense-in-depth - both the quarantine dir and `iris/staging/` are git-ignored.
- Negative / trade-offs: derived assets are not version-controlled, so reproducibility relies on
  retaining the source jars plus the conversion tooling; per-pack provenance must be recorded
  manually (`SOURCE.md`).

## Alternatives Considered

- **Option A - track derived assets normally:** rejected; committing them would constitute
  Sharing Adapted Material under ND, and would leak into any public remote.
- **Option B - rely only on `iris/staging/` already being git-ignored:** rejected as too implicit;
  a dedicated, documented quarantine makes the restriction explicit and auditable.
