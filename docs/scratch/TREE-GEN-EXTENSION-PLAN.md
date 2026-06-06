# Tree-Gen Extension Plan

Generated: 2026-06-04

This document maps what the Iris overworld tree palettes actually need against what
`generate_tree.py` / `trunk.py` / `canopy.py` currently support, then specifies the
minimum script amendments required to replicate each tree family at multiple sizes.

---

## 1. What the script can do today

| Feature | Supported | Config key |
|---------|-----------|------------|
| Single trunk wood type | Yes | `trunk` |
| Single leaf type | Yes | `leaves` |
| Trunk width + shape functions | Yes | `trunk_width`, `trunk_shape` |
| Lean + spiral/sine azimuth | Yes | `lean_angle`, `lean_azimuth_fn` |
| Volume canopy (preset profiles) | Yes | `canopy.profile` |
| Branch system with sub-branches | Yes | `canopy.branches` |
| Mixed trunk wood types | **No** | - |
| Mixed leaf types | **No** | - |
| Decorative accent blocks (fence, vine, snow, slab, planks) | **No** | - |
| Stripped-wood trunk sections | **No** | - |
| Surface-attached blocks (roots, sapling at base) | **No** | - |

---

## 2. What the Iris trees actually need

Derived from verified `.iob` palette data. Trees with no gaps are already fully
replicable; the rest need one or more new features.

### 2a. Already replicable (no changes needed)

| Iris tree | Palette | Our equivalent |
|-----------|---------|----------------|
| `birch/antioch3` | birch_wood, birch_leaves | Standard birch config |
| `birch/largeponderosa1` | birch_wood, birch_leaves | Standard birch config |
| `acacia/vexed1` | acacia_wood, acacia_leaves | Standard acacia config |
| `acacia/savannaD1` | acacia_wood, acacia_leaves | Standard acacia config |
| `acacia/savannaS1` | acacia_wood, acacia_leaves | Standard acacia config |
| `spruce/levergreen1` | spruce_wood, spruce_leaves | Standard spruce config |
| `oak/hoakgeneric1` | oak_wood, oak_leaves | Standard oak config |
| `oak/lponderosa1` | oak_wood, oak_leaves | Standard oak config |
| `oak/croak1` | oak_log, oak_leaves | Standard oak config |
| `oak/dead1` | oak_wood, oak_leaves | Standard oak config |

### 2b. Needs multi-leaf support only

These have one trunk type but two or more leaf types. The secondary leaves appear
in the canopy mix - typically the outer/lower fringe uses a different species.

| Iris tree | Primary | Secondary leaf(ves) | Visual effect |
|-----------|---------|---------------------|---------------|
| `mixed/tredwoodsmol1` | spruce_wood, spruce_leaves | oak_leaves | Spruce with oak fringe |
| `mixed/AmyLarge1` | dark_oak_wood, jungle_leaves | oak_leaves | Crystal tree - jungle canopy, oak tips |
| `mixed/AmyMed1` | dark_oak_wood, jungle_leaves | oak_leaves, birch_leaves, spruce_leaves | Crystal tree - multi-species fringe |
| `mixed/AmyNormal1` | dark_oak_wood, jungle_leaves | oak_leaves | Same as AmyLarge, smaller |
| `mixed/AmySmol1` | dark_oak_wood, jungle_leaves | oak_leaves | Same, smallest |
| `bonsai/med-1` | oak_wood, oak_leaves | spruce_leaves | Bonsai - spruce accent on outer layers |
| `jungle/lgeneric1` | jungle_wood, jungle_leaves | birch_leaves | Jungle with birch fringe |
| `darkoak/talldrift1` | dark_oak_wood, dark_oak_leaves | oak_leaves | Drift oak - oak leaf accent |
| `sproak/sp1` | spruce_wood, spruce_leaves | dark_oak_wood, dark_oak_leaves, oak_leaves | Spruce/oak hybrid |

### 2c. Needs multi-wood (secondary trunk) support only

These use a second wood type on the trunk - typically as a structural accent or
bark variation on the lower trunk or branch sections.

| Iris tree | Primary trunk | Secondary trunk | Where secondary appears |
|-----------|---------------|-----------------|------------------------|
| `spruce/vgeneric1` | spruce_wood | dark_oak_wood | Lower trunk accent |
| `spruce/lfrostgeneric1` | spruce_wood | dark_oak_wood | Lower trunk + branch bases |
| `mixed/tredwood1` | dark_oak_wood | spruce_wood | Upper trunk / branch sections |

### 2d. Needs both multi-wood and multi-leaf

| Iris tree | Trunk mix | Leaf mix | Notes |
|-----------|-----------|----------|-------|
| `mixed/tredwood1` | dark_oak_wood + spruce_wood | spruce_leaves + birch_leaves | Redwood - dark oak base, spruce upper |
| `mixed/pollup1` | birch_wood + spruce_wood | birch_leaves + spruce_leaves + oak_leaves + azalea_leaves | Round snow-capped - 4 leaf types |
| `sproak/generic1` | spruce_wood + oak_wood + stripped_spruce_wood | spruce_leaves | Spruce/oak hybrid + stripped accent |
| `sproak/sp1` | spruce_wood + dark_oak_wood | spruce_leaves + dark_oak_leaves + oak_leaves | Full hybrid |
| `darkoak/generic1` | dark_oak_wood | oak_leaves (secondary) | Plus fence accent |
| `troofed1` | oak_wood + stripped_dark_oak_wood | dark_oak_leaves + spruce_leaves | Roofed oak - stripped accent on branches |

### 2e. Needs decorator blocks (fence, vine, snow, slab, planks)

These use non-leaf, non-wood blocks as accents. They are the most complex category.

| Iris tree | Decorator blocks | Where they appear |
|-----------|-----------------|-------------------|
| `spruce/pine1` | dark_oak_fence, dark_oak_fence_gate, spruce_sapling | Branch tips / base |
| `spruce/sup-pine-1` | spruce_fence | Branch tips |
| `spruce/lfrostgeneric1` | snow | Upper canopy surface |
| `sproak/generic1` | spruce_fence, spruce_planks, snow | Branch tips + upper surface |
| `acacia/denmyre1` | acacia_planks, acacia_fence | Branch tips |
| `mixed/dotree1` | dark_oak_fence, oak_fence, spruce_fence, dark_oak_slab | Branch tips + trunk |
| `jungle/lgeneric1` | vine | Trunk surface, hanging |
| `jungle/largegeneric1` | vine | Trunk surface, hanging |
| `mangrove/mangrove1` | mangrove_roots, oak_fence | Base / prop roots |
| `sakura/genericsak1` | (stripped_birch_wood trunk) | Trunk only - stripped variant |

---

## 3. Proposed script amendments

Three additions cover all gaps. They are independent and can be implemented in order.

---

### Amendment A - Secondary leaves (`secondary_leaves`)

**Config key:** `secondary_leaves` (string, optional)
**Config key:** `secondary_leaf_fraction` (float 0-1, default 0.35)

**Behaviour:** When present, the canopy generator randomly substitutes
`secondary_leaves` for `leaves` on each leaf block placement with probability
`secondary_leaf_fraction`. For multi-leaf trees (pollup, AmyMed) a list form
`secondary_leaves` (array of `{block, weight}`) is also accepted.

**Implementation location:** `canopy.py` - `_place_leaf_disc` and
`_place_leaf_cluster`. Before writing `leaf_block`, draw from rng; if below
threshold, write `secondary_leaf_block` instead.

**Covers:** tredwoodsmol, AmyLarge/Med/Normal/Smol, bonsai/med, lgeneric,
talldrift, sp1, pollup (partial).

**Example config fragment:**
```json
{
  "leaves": "minecraft:spruce_leaves",
  "secondary_leaves": "minecraft:oak_leaves",
  "secondary_leaf_fraction": 0.25
}
```

For multiple secondaries:
```json
{
  "leaves": "minecraft:birch_leaves",
  "secondary_leaves": [
    { "block": "minecraft:spruce_leaves", "weight": 2 },
    { "block": "minecraft:oak_leaves",    "weight": 1 },
    { "block": "minecraft:azalea_leaves", "weight": 1 }
  ],
  "secondary_leaf_fraction": 0.4
}
```

---

### Amendment B - Secondary trunk (`secondary_trunk`)

**Config key:** `secondary_trunk` (string, optional)
**Config key:** `secondary_trunk_start` (float 0-1, default 0.5) - height fraction
above which the secondary block is used instead of primary.

**Behaviour:** In `trunk.py` `generate_trunk_with_offsets`, when writing each
layer's block, if `y / height >= secondary_trunk_start` use `secondary_trunk`
instead of `trunk`. This replicates the Iris pattern of dark_oak base + spruce
upper on tredwood, or spruce main + dark_oak lower accent on vgeneric/lfrostgeneric.

For lower-accent trees (secondary on bottom), set `secondary_trunk_start: 0.0` and
add `secondary_trunk_end` (float, default 1.0) to bound the range.

**Implementation location:** `trunk.py` - `generate_trunk_with_offsets`, pass
secondary params through; `generate_tree.py` - extract and forward the new keys.

**Covers:** tredwood1, vgeneric1, lfrostgeneric1, sproak/generic1, troofed1.

**Example config fragment:**
```json
{
  "trunk": "minecraft:dark_oak_log",
  "secondary_trunk": "minecraft:spruce_log",
  "secondary_trunk_start": 0.55
}
```

---

### Amendment C - Decorator blocks (`decorators`)

**Config key:** `decorators` (array of decorator objects, optional)

Each decorator object:
```json
{
  "block": "minecraft:spruce_fence[facing=north,...]",
  "target": "branch_tip",
  "chance": 0.4,
  "axis_aware": true
}
```

`target` options:
- `branch_tip` - placed at the endpoint of each branch (replaces or extends the
  leaf cluster tip block)
- `trunk_surface` - placed adjacent to trunk blocks that face open air (for vine,
  snow cap)
- `canopy_top` - placed on the topmost leaf block in each column (snow layer)
- `trunk_base` - placed at y=0 around the trunk base (roots, sapling)

`axis_aware`: if true and the block has a `facing` property, orient it away from
the trunk center (used for fence posts, fence gates).

**Implementation location:** new `decorators.py` module; called from
`generate_tree.py` after trunk + canopy are assembled.

**Covers:** pine1 (fence + gate + sapling), sup-pine (fence tips), lfrostgeneric
(snow cap), sproak/generic (fence + planks + snow), denmyre (planks + fence),
dotree (multi-fence + slab), lgeneric/largegeneric (vine on trunk surface),
mangrove (roots at base).

**Example config fragment (pine1 equivalent):**
```json
{
  "decorators": [
    { "block": "minecraft:dark_oak_fence", "target": "branch_tip", "chance": 0.5, "axis_aware": true },
    { "block": "minecraft:dark_oak_fence_gate", "target": "branch_tip", "chance": 0.1, "axis_aware": true },
    { "block": "minecraft:spruce_sapling", "target": "trunk_base", "chance": 0.3 }
  ]
}
```

**Example config fragment (vine jungle):**
```json
{
  "decorators": [
    { "block": "minecraft:vine", "target": "trunk_surface", "chance": 0.35 }
  ]
}
```

---

## 4. Implementation priority

| Amendment | Covers N Iris trees | Effort | Priority |
|-----------|---------------------|--------|----------|
| A - secondary_leaves | ~12 trees | Low - 2 files, ~30 lines | **First** |
| B - secondary_trunk | ~6 trees | Low - 2 files, ~20 lines | **Second** |
| C - decorators | ~10 trees | Medium - new module ~120 lines | **Third** |

Amendment A alone unlocks the Amy crystal trees, bonsai, talldrift, and lgeneric -
the four most-used Iris tree families in the adoption plan. Do A first.

---

## 5. Trees that remain out of scope

These require geometry the script cannot produce regardless of amendments:

| Iris tree | Why out of scope |
|-----------|-----------------|
| `mixed/smoakog1/80/160` | Wool blocks used as canopy - decorative art tree, not a natural form |
| `mixed/dotree1` | Fence slab combination on trunk mid-sections requires per-block placement logic beyond decorator targets |
| `mangrove/mangrove1` | Prop-root geometry (roots growing down from branches) needs a dedicated root-drape system |
| `oak/troofed1` / `oak/mroofed1` | Roofed oak canopy shape (flat dense ceiling) needs a new `roofed` profile preset |

The `roofed` profile is worth adding as a fourth amendment if roofed-oak biomes
(Creaks, Roofed Forest, Roofed Wayward) are a priority - it is a single new entry
in `_PRESETS` in `canopy.py` with a very flat squish and wide radius.

---

## 6. Mapping Iris families to config templates

Once amendments A-C are in place, each Iris tree family maps to a config template:

| Iris family | Template name | Key params |
|-------------|---------------|------------|
| tredwood (redwood) | `redwood` | trunk=dark_oak, secondary_trunk=spruce @0.5, leaves=spruce, secondary_leaves=birch @0.2 |
| tredwoodsmol | `redwood_small` | trunk=spruce, leaves=spruce, secondary_leaves=oak @0.25 |
| AmyLarge/Med/Normal/Smol | `crystal_tree` | trunk=dark_oak, leaves=jungle, secondary_leaves=oak @0.3 |
| pollup | `pollup` | trunk=birch, secondary_trunk=spruce @0.4, leaves=birch, secondary_leaves=[spruce,oak,azalea] @0.5 |
| talldrift | `drift_oak` | trunk=dark_oak, leaves=dark_oak, secondary_leaves=oak @0.2 |
| bonsai/med | `bonsai` | trunk=oak, leaves=oak, secondary_leaves=spruce @0.3 |
| lgeneric (jungle small) | `jungle_lgeneric` | trunk=jungle, leaves=jungle, secondary_leaves=birch @0.15, decorators=[vine @0.3] |
| largegeneric (jungle large) | `jungle_large` | trunk=jungle, secondary_trunk=oak @0.3, leaves=jungle, decorators=[vine @0.35] |
| pine1 | `pine` | trunk=spruce, leaves=spruce, decorators=[dark_oak_fence @0.5, sapling @0.2] |
| sup-pine | `sequoia` | trunk=spruce, leaves=spruce, decorators=[spruce_fence @0.3] |
| lfrostgeneric | `frost_spruce` | trunk=spruce, secondary_trunk=dark_oak @0.0-0.3, leaves=spruce, decorators=[snow @canopy_top 0.6] |
| sproak/generic | `sproak` | trunk=spruce, secondary_trunk=oak @0.4, leaves=spruce, decorators=[fence @0.3, snow @canopy_top 0.4] |
| denmyre (acacia) | `denmyre` | trunk=acacia, leaves=acacia, decorators=[acacia_planks @branch_tip 0.2, acacia_fence @branch_tip 0.3] |
| genericsak (sakura) | `sakura` | trunk=stripped_birch_log, leaves=oak (cherry profile) |

The `genericsak` sakura uses `stripped_birch_log` as trunk - this is already
supported by the `trunk` field; just pass `minecraft:stripped_birch_log` directly.

