# Iris End: What We Pick to Replicate (Folia)

Decision shortlist that turns the research in
[`IRIS-END-FEATURE-IDEAS.md`](IRIS-END-FEATURE-IDEAS.md) and the authoring reference in
[`IRIS-END-GENERATION.md`](IRIS-END-GENERATION.md) into a concrete, prioritised set of
things to actually build, filtered through what is safe and worthwhile **on a Folia test
server**. The complaint we are answering: the vanilla End is boring and barely
explorable. The fix: a custom central island plus a rich, varied **outer-island field**
players travel into.

> Status: planning/decision only. Nothing here is implemented yet (server still runs Iris
> overworld-only, vanilla End). Each pick is an Iris-native, deploy-from-`pack-overlay`
> change; no NMS, no redistribution of restricted-source assets (ADR-004).

---

## Folia lens (read first)

Folia is regionized multithreaded Paper. Two facts shape every pick below:

1. **Iris advertises Spigot/Paper, not Folia.** The resource page lists "Supported
   software: Spigot, Paper" with no `folia-supported` claim. So step zero is **verify the
   bundled `Iris.jar` even loads and generates on Folia** (user-triggered boot) before any
   authoring effort. If it does not, the whole Iris-End plan is blocked and we fall back to
   a Paper End world or a datapack End. Record the boot result before building content.
2. **Worldgen is the friendly case for Folia.** Chunk generation is exactly the kind of
   spatially-partitioned work Folia parallelises well, and the End naturally spreads
   players out (the gamemode Folia is built for). So *if* Iris loads, an island-field End
   is a good Folia fit. The danger is not the terrain - it is any **per-tick / cross-region
   plugin logic** (dragon fight, gateways, custom mob AI, day/night driver), which on Folia
   must use region schedulers and is the main source of breakage.

Picking rule used below: **prefer pure-worldgen features (safe on Folia), defer anything
needing per-tick or cross-region runtime code to a separate Folia-aware plugin (or drop
it).**

---

## Picks (priority order)

### P0 - Prove the engine on Folia (blocking gate)
- Boot Folia with the current `Iris.jar` and confirm it loads and can generate an Iris
  world at all. No content work proceeds until this passes.
- Decide central-island exit mechanic now, because it is the one piece that is NOT
  worldgen: vanilla dragon/exit-portal/gateways are NMS and the riskiest thing on Folia.
  Recommended pick: **keep a vanilla-style central platform mechanic out of Iris** (either
  retain vanilla End spawn behaviour or a tiny Folia-aware plugin that teleports players
  outward), so Iris only owns terrain.

### P1 - Outer-island field via `floatingChildBiomes` (the core "explorable" win)
This is the headline replication and is pure worldgen (Folia-safe).
- Base outer biome hosting `floatingChildBiomes`, scattering tapered sky islands.
- Island-shape variety (the BetterEnd selling point) using verified
  `IrisFloatingChildBiomes` fields: `altitudeStyle` + `min/maxHeightAboveSurface` for
  layered altitude bands, `topShapeMode`/`topShapeStyle`/`topShapeAmp` for varied tops,
  `wallWarpStyle`/`wallWarpAmplitude` so islands are not cylinders,
  `bottomStyle`/`bottomExponent`/`bottomDepthMax` for dramatic undersides, `maxThickness`
  to mix thin shelves with thick masses.

### P2 - 4-6 outer-End biomes (variety = explorability)
Author as Iris biomes under `biomes/end/*`, each an `IrisFloatingChildBiomes.biome` entry
with its own `rarity`/`footprintStyle`/altitude band. Pick set:
- Chorus forest (chorus decorators, `end_highlands` derivative).
- Crystal/spire mountains (aggressive `topShapeAmp`).
- Glowing/amber fields (emissive ground cover).
- Barren dust wastes (`end_barrens` derivative, flat - the connective filler).
- Optional 5th/6th: fungal land, shattered shelves.
Keep `derivative`/`vanillaDerivative` on real End biomes so client sky/fog stays correct.

### P3 - Internal island caves + End resources (rewards exploration)
- `carveStyle`/`carving`/`carveThreshold` on `floatingChildBiomes` to hollow islands.
- Dimension-scoped `ores`/`deposits` for End-appropriate materials so travelling out pays
  off.

### P4 - Structure variety on islands (destinations to find)
Pure worldgen placement, Folia-safe at generation time. Author as Iris jigsaw
`IrisStructure`s and place with floating `ObjectPlaceMode`
(`FLOATING`/`VACUUM*`/`CEILING_HANG`) - see
[`IRIS-END-GENERATION.md`](IRIS-END-GENERATION.md) section 4. Pick set:
- Ruined spires / shattered platforms (free-standing `FLOATING`).
- Hanging ships moored under islands (`CEILING_HANG`).
- Ritual altars (BetterEnd flavour) with loot via the structure `loot` field.
- Use `distribution: CONCENTRIC_RINGS` to echo vanilla's ring of outer islands, or
  `RANDOM_SPREAD` with `spacing`/`separation` for even scatter.

### Deferred / dropped (Folia or engine cost too high)
- **Dragon fight, exit portal, End gateways** - NMS, not Iris; biggest Folia hazard. Drop
  from Iris scope; handle via vanilla mechanic or a dedicated Folia-aware plugin.
- **Per-biome music, custom mobs, rituals-as-events** - mod runtime code, not worldgen.
  Defer to a companion plugin only if it is written against Folia region schedulers.
- **~14 min day/night driver (Endercon-style)** - needs a ticking task; on Folia must use
  the global region scheduler. Defer; use `fixedTime`/`ambientLight` for static ambience
  in the meantime.

---

## Why this set

- Everything in P1-P4 is **generation-time only**, which is the workload Folia handles
  best and which directly attacks "boring / barely explorable": more biomes, more island
  shapes, internal caves, and scattered destinations.
- The only Folia-fragile pieces (P0 exit mechanic, deferred list) are quarantined out of
  Iris so a worldgen-only Iris End cannot be what breaks Folia.

---

## Open items before building

1. P0 boot result: does `Iris.jar` run on this Folia build? (user-triggered boot).
2. Central-island exit decision (vanilla mechanic vs small Folia plugin).
3. `deploy-iris-pack.ps1` is overworld-specific; generalise for a second world/pack.
4. `world_the_end` binding is server-side (not in repo).
5. Scope/docs drift vs [`VANILLA-STRUCTURE-COVERAGE.md`](VANILLA-STRUCTURE-COVERAGE.md);
   add an ADR when this is adopted.

---

## See Also

- [`IRIS-END-GENERATION.md`](IRIS-END-GENERATION.md) - how to author each layer (the implementation reference).
- [`IRIS-END-FEATURE-IDEAS.md`](IRIS-END-FEATURE-IDEAS.md) - the full survey these picks are drawn from.
- [`IRIS-NETHER-GENERATION.md`](IRIS-NETHER-GENERATION.md) - sibling research for an Iris Nether.
- [`VANILLA-STRUCTURE-COVERAGE.md`](VANILLA-STRUCTURE-COVERAGE.md) - current (vanilla) End scope statement.
- Project guidelines (*Iris Pack Layering*, *Server Boots Are User-Triggered*).
