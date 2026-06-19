# Tree generation: transition from baked `.iob` to Iris-4.0-native procedural trees

Date: 2026-06-08
Status: in progress (vanilla biomes done; custom/fantasy biomes pending review)

This document records how vanilla tree generation moved off the offline `.iob`
pipeline onto Iris 4.0's native procedural-tree generator, and - importantly - how
we keep the new trees faithful to our own tuned `canopy.py` geometry instead of
falling back to Iris' built-in defaults.

> SUPERSEDED by [ADR-005](adr/ADR-005-retire-iris-for-vanilla-datapack-worldgen.md):
> Iris has been retired in favor of vanilla datapack worldgen, so this Iris-4.0
> procedural-tree transition is historical. The offline tree generator is NOT
> deprecated: it is kept as an active tool under `tools/tree-gen/` and now emits a
> vanilla structure-template `.nbt` (`--format nbt`) to feed vanilla tree features
> and jigsaw pieces directly. The text below describes the retired Iris pipeline.

---

## 1. Background: two tree models

We have two descriptions of "a tree":

1. Our offline model in `iris/tree-gen/` (`canopy.py`, `trunk.py`, `branches`,
   `roots.py`, ...). It bakes a tree into a block dict and writes an `.iob`.
   The canopy geometry lives in `canopy.py`:
   - `_PRESETS`: per-profile list of `(y_frac, r_frac)` layer pairs (the
     silhouette of oak/birch/spruce/jungle/acacia/dark_oak_flat/cherry).
   - `_PROFILE_RADIUS_SCALE`: per-profile radius multiplier.
   - `preset_layers(profile, height, branch_driven)`: scales those fractions to a
     concrete `(y_offset, radius)` list, anchored to the trunk top
     (`y_top = (height - 1) + crown_extra`).
   - `_layer_half_height` / volume stacking: each layer is a domed half-ellipsoid;
     `start_angle` controls how much of the BOTTOM hemisphere is kept
     (`0` = full sphere, `90` = flat bottom + rounded top, `>=180` = flat disc).
   - default canopy `mode` is `trimmed` (solid disc minus the 4 box corners).
   - branch-driven trees place only the TOP crown layer as volume and build the
     rest from the branch system.

2. Iris 4.0's native model (`art.arcane.iris.engine.object.IrisProceduralTree`),
   grown at world-gen time from JSON in `biome.proceduralObjects.trees`. Field
   mapping vs our format: see `docs/scratch/IRIS4-TREE-SCHEMA-COMPARISON.md`.

Iris 4.0 faithfully implements the same `start_angle`/`squish`/layer math as our
`canopy.py` (verified against `Iris.jar` bytecode of `TreeCanopyBuilder.volumeCanopy`
and `layerHalfHeight`). So a tree described by the same fields renders the same -
**with one critical exception below.**

---

## 2. The faithfulness gap (and the bug it caused)

Iris has its OWN built-in per-profile layer fractions and radius scales
(`TreeProfiles.presetLayers` / `radiusScale`). These are NOT the same numbers as
our `canopy.py` `_PRESETS` / `_PROFILE_RADIUS_SCALE`.

Therefore: if we convert one of our trees to Iris JSON and emit NO explicit
`layers`, Iris substitutes its own silhouette, and the tree no longer matches what
our `canopy.py` would have grown. Our tuned `_PRESETS` live only in `canopy.py`
code; they were never present in the config JSON, so a naive converter dropped them
entirely.

A previous transition attempt compounded this by also setting the canopy `mode` to
`density` for most vanilla species. `density` applies a radial falloff
(`1 - dist/(radius+1)`) that empties the outer shell of the crown, so trees came out
sparse / "bare" and the trunk showed through the leaves. (It also wrote canopy
`yOffset` as a small absolute value, which placed the crown on the ground, because
Iris treats canopy `yOffset` as absolute from the tree base - it does not rescale it
to the runtime trunk height.)

---

## 3. The fix: bake our `canopy.py` silhouette into the Iris JSON

`iris/tree-gen/to_procedural.py` gained `convert_entry(entry, warn, bake_layers=True)`.
When enabled and the entry has no explicit canopy `layers`, `_bake_preset_layers`:

1. calls `canopy.preset_layers(profile, height, branch_driven)` - the *same* code
   path `generate_canopy` uses - and writes the result as explicit Iris `layers`;
2. for branch-driven trees keeps only the top crown layer (`layers[-1]`), exactly
   like `generate_canopy` (Iris' branch system builds the rest);
3. defaults the canopy `mode` to `trimmed` (our `canopy.py` default) so the crown is
   solid and always encloses the trunk.

Because Iris places canopy `yOffset` absolutely, the vanilla generator emits one
tree per FIXED trunk height across each size bucket (`heightMin == heightMax`) and
bakes its layers for that exact height, so every crown caps its own trunk. The
group's placement `chance` is split evenly across the fixed-height variants to
preserve overall tree density.

Verification: the emitted `layers` are byte-for-byte equal to
`canopy.preset_layers(...)` for the same profile/height (e.g. oak h6/h9).

### Roots
Our offline `roots` flag drove an `.iob`-only *underground* bridging structure
(`roots.py`), unrelated to Iris' *visible* procedural root system. The converter
emits `roots: false` by default; only large/custom trees opt back in (via
`iris_roots`). This matches the design intent: only very large / custom trees carry
visible trunk-to-ground structure.

---

## 4. Generators

- `iris/scripts/build-vanilla-trees.py` - VANILLA biomes. Repurposed: it no longer
  bakes `.iob` for the generated species; it writes `proceduralObjects.trees`
  derived from `canopy.py` via `convert_entry(bake_layers=True)`. Each `SPECIES`
  entry now carries only `profile` (+ optional `squish`/`start_angle`); the
  silhouette comes from `canopy.py`. Mushrooms and pre-built `custom:` objects are
  still placed as `.iob`.
- `iris/scripts/build-procedural-trees.py` - non-vanilla "missing family" biomes,
  built from `iris/tree-gen/configs/*.json`. NOTE: this generator does not yet pass
  `bake_layers=True`, so its trees still rely on Iris' internal presets and some use
  `density` mode. As of this transition there are ~239 `density` trees across custom
  biome dirs (temperate, swamp, tundra, ...). Applying the same `bake_layers` +
  `trimmed` treatment here is a follow-up, pending review (those are authored
  fantasy trees, not vanilla-parity trees).

Run order, then deploy:
```
python iris/scripts/build-vanilla-trees.py
powershell iris/scripts/deploy-iris-pack.ps1
```
The deployed pack only takes effect on the next (user-triggered) server boot.
