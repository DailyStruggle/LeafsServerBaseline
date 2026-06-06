# ADR-001: Repository Scope and Plugin Policy

**Status:** Accepted  
**Date:** 2026-06-04  
**Author:** Leaf

---

## Context

This repository was created to support the design and operation of Leaf's Minecraft server. As the project grows it may produce plugins or tools that have value beyond this specific server. Without a clear policy, generally-applicable code could accumulate here, making it harder to reuse, share, or maintain independently.

## Decision

1. This repository (`LeafsServerBaseline`) is scoped to **Leaf's server only**: server-specific datapacks, scripts, configs, documentation, and thin plugin wrappers or configuration.
2. Any plugin (or other tool) that is **generally applicable** - i.e. useful on servers other than this one without modification - must be developed and published in its **own separate repository**. Only a reference or minimal integration shim may live here.

## Consequences

- Positive: keeps this repo focused and easy to navigate; reusable plugins get proper standalone visibility and versioning.
- Positive: clear decision boundary prevents scope creep.
- Negative / trade-offs: initial overhead of creating a new repo when a plugin graduates to general use; cross-repo coordination needed for integration.

## Alternatives Considered

- **Keep everything in one repo (monorepo)** - simpler at first, but conflates server-specific concerns with general-purpose tooling and complicates reuse.
- **No policy, decide case-by-case** - avoids upfront overhead but leads to inconsistent structure over time.
