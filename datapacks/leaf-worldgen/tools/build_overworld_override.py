import json, copy, os, sys

# Path to the vanilla data generator's biome_parameters/minecraft/overworld.json
# report (run `java "-DbundlerMainClass=net.minecraft.data.Main" -jar
# <mojang-bundler>.jar --reports`). Pass as argv[1] or via the OVERWORLD_REPORT
# env var; defaults to the temp location used during development.
REPORT = (sys.argv[1] if len(sys.argv) > 1
          else os.environ.get("OVERWORLD_REPORT",
               os.path.join(os.environ.get("TEMP", "."), "mcdatagen", "generated",
                            "reports", "biome_parameters", "minecraft", "overworld.json")))
# Output is the datapack override, resolved relative to this script.
OUT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", "data",
                                    "minecraft", "dimension", "overworld.json"))

with open(REPORT, "r", encoding="utf-8") as f:
    report = json.load(f)

# CONDITIONAL rainbow-forest swap (see the flower_forest handling in the main
# loop). Every vanilla flower_forest parameter point shares the same humidity span
# ([-1.0, -0.35]) but tiles the WEIRDNESS axis across many distinct bands. Weirdness
# is therefore the natural "condition" for swapping in the rainbow variant: only the
# portion of each flower_forest's weirdness span below the split threshold is
# replaced by the rainbow gradient, and the rest stays vanilla minecraft:flower_forest.
# So a flower forest becomes a rainbow forest only WHERE the weirdness noise lands in
# that sub-band -- a genuine in-world condition, not a whole-biome substitution.
# Issue: "might as well all be rainbow forest for what used to be flower forest".
# Set to 1.0 so the ENTIRE weirdness span of every vanilla flower_forest point is
# replaced by the rainbow gradient -- no vanilla minecraft:flower_forest remainder
# is kept (the "rest" entry below is skipped when the fraction is >= 1.0).
RAINBOW_WEIRDNESS_FRACTION = 1.0

# Number of times the 12-colour rainbow gradient repeats across the humidity span of
# the rainbow (weird) portion. Humidity is a low-frequency field, so a higher repeat
# count cycles the same 12 leaf-colour biomes more times, producing many tight bands
# so the colours alternate frequently and the gradient reads clearly. Tune here.
RAINBOW_REPEATS = 8

# 12-colour rainbow gradient (rainbow_forest_01..12) banded across the FULL humidity
# span (fractions 0.0..1.0), cycled RAINBOW_REPEATS times into many narrow alternating
# bands. The swap CONDITION lives on the weirdness axis (handled in the main loop);
# this helper only lays out the colour gradient over humidity.
def _rainbow_humidity_bands(repeats=RAINBOW_REPEATS):
    bands = 12 * repeats
    return [
        ("leaf:rainbow_forest_%02d" % ((k % 12) + 1),
         k / float(bands),
         (k + 1) / float(bands))
        for k in range(bands)
    ]

# Maple Forest is a WARM-palette cousin of the rainbow set: 8 leaf:maple_forest_NN
# biomes whose foliage_color steps across the warm arc only (deep red -> orange ->
# amber -> gold; see tools/build_maple_biomes.py). Same banding trick as the
# rainbow tiers -- cycle the 8-colour warm gradient MAPLE_REPEATS times across the
# biome's humidity span so the autumn canopy alternates in narrow strips.
MAPLE_COLORS = 8
MAPLE_REPEATS = 3

def _maple_tiers(repeats=MAPLE_REPEATS):
    bands = MAPLE_COLORS * repeats
    return [
        ("leaf:maple_forest_%02d" % ((k % MAPLE_COLORS) + 1), k / float(bands), (k + 1) / float(bands))
        for k in range(bands)
    ]

SPLITS = {
    # dark_forest humidity span is the whole top vanilla humidity band
    # ([0.3, 1.0]). Nested-shell ordering as humidity rises spatially toward the
    # wet interior: edge (outer ring, driest) -> body (middle) -> core (inner,
    # wettest), so core reads as the heart of each dark-forest region. Humidity
    # noise rarely peaks high, so to keep core from being rare we go LOOSE: edge
    # and body are thin outer rings and core takes the entire wide upper HALF of
    # the span. That wide core band makes dark_forest_core appear MORE often than
    # the narrow forest_core (which is only the wettest third of its span).
    "minecraft:dark_forest": [
        ("leaf:dark_forest_edge", 0.0, 0.25),
        ("leaf:dark_forest_body", 0.25, 0.5),
        ("leaf:dark_forest_core", 0.5, 1.0),
    ],
    # Nested shells over the humidity span: humidity rises spatially toward a
    # local peak, so the LOW end is the outer ring and the HIGH end is the
    # innermost point. Make edge the WIDEST (outer ring), body MEDIUM, and core
    # a NARROW top slice so core reads as a small centre reliably surrounded by
    # body, and body by edge (issue: core surrounded by mid, mid by edge).
    "minecraft:taiga": [
        ("leaf:taiga_edge", 0.0, 0.45),
        ("leaf:taiga_body", 0.45, 0.8),
        ("leaf:taiga_core", 0.8, 1.0),
    ],
    # Plain forest gradient: mirror the dark_forest edge/body/core humidity-thirds
    # split, but the forest stays an OPEN (non-roofed) canopy -- trees are
    # sporadic enough to let light through at every tier. forest_body is pure
    # vanilla forest (vanilla trees_birch_and_oak_leaf_litter selector); edge is
    # sparse small oaks/birch, core is taller oak_large-weighted but still low
    # count so it never closes into a roof.
    # Same nested-shell widths as taiga: wide edge ring, medium body, narrow
    # core centre (see the taiga comment) so the open forest transitions
    # edge -> body -> core as you move inward.
    "minecraft:forest": [
        ("leaf:forest_edge", 0.0, 0.45),
        ("leaf:forest_body", 0.45, 0.8),
        ("leaf:forest_core", 0.8, 1.0),
    ],
    # NOTE: minecraft:flower_forest is intentionally NOT listed here. It is handled
    # specially in the main loop as a CONDITIONAL weirdness-axis swap (see
    # RAINBOW_WEIRDNESS_FRACTION / _rainbow_humidity_bands) rather than as a
    # humidity-span split, so only a weirdness sub-band becomes rainbow_forest while
    # the remainder stays vanilla minecraft:flower_forest.
    # Fun Content Backlog Tier 1 "flavored biomes": each fully replaces its
    # vanilla derivative across the whole humidity span (single tier 0.0..1.0),
    # the same whole-biome substitution pattern used for forest/taiga. JSON-only,
    # vanilla features/palette reused (see FUN-CONTENT-BACKLOG.md).
    "minecraft:meadow": [("leaf:alpine_meadow", 0.0, 1.0)],
    # Maple Forest: warm multicolor variant set (leaf:maple_forest_01..08), the
    # warm-palette cousin of the rainbow_forest split. Cycles the 8-colour warm
    # gradient across birch_forest's humidity span (see _maple_tiers).
    "minecraft:birch_forest": _maple_tiers(),
    "minecraft:cherry_grove": [("leaf:jacaranda_grove", 0.0, 1.0)],
    "minecraft:old_growth_pine_taiga": [("leaf:redwood_grove", 0.0, 1.0)],
    # Fun Content Backlog Tier 2 whole-biome substitutions. Same pattern as the
    # Tier 1 entries above: each fully replaces its vanilla derivative across the
    # whole humidity span. Boreal Shield supplants snowy_taiga (glacier-swept
    # granite + sparse spruce); Bayou supplants the rare mangrove_swamp
    # (cypress/mud wetland). See FUN-CONTENT-BACKLOG.md.
    "minecraft:snowy_taiga": [("leaf:boreal_shield", 0.0, 1.0)],
    "minecraft:mangrove_swamp": [("leaf:bayou", 0.0, 1.0)],
}


# Brand-new custom biomes injected as narrow climate slivers (not splits of an
# existing vanilla biome). glass_beach (coast continentalness) and
# volcanic_mountain (near-inland) share temperature/humidity/weirdness/erosion so
# they border each other along the coastline: the volcanic cone structure
# (leaf:volcano, gated to leaf:volcanic_mountain) always has a glass beach next to
# it. Slivers are additive, so every vanilla biome remains available.
def _sliver(biome, continentalness, depth):
    return {
        "biome": biome,
        "parameters": {
            "continentalness": continentalness,
            "depth": depth,
            "erosion": [-0.375, 0.05],
            "humidity": [-1.0, -0.35],
            "offset": 0.0,
            "temperature": [0.9, 1.0],
            "weirdness": [0.4, 0.7],
        },
    }

# Lavender Fields (Fun Content Backlog Tier 1 #5). Its intended vanilla
# derivative (flower_forest) is still mostly retained now that rainbow_forest only
# takes a RAINBOW_FRACTION sub-slice of the span, but it is kept as a distinct
# climate sliver (rather than another flower_forest split) so it occupies its own
# warm dry-ish niche. Instead of a whole-biome substitution it is injected as an
# additive climate sliver: warm, dry-ish, flat inland (high erosion) so it reads
# as an open purple bloom field. Additive => no vanilla biome is removed.
def _lavender(depth):
    return {
        "biome": "leaf:lavender_fields",
        "parameters": {
            "continentalness": [0.03, 0.3],
            "depth": depth,
            "erosion": [0.45, 0.85],
            "humidity": [-0.35, 0.1],
            "offset": 0.0,
            "temperature": [0.55, 0.8],
            "weirdness": [-0.2, 0.2],
        },
    }

# Volcanic Hot Springs (Fun Content Backlog Tier 2 #9). Sits just past the
# volcanic_mountain band (near-inland continentalness) so prismatic calcite
# pools cluster around the volcano, but with a cooler/wetter pine-taiga climate
# (distinct from the hot/dry volcanic sliver) so it reads as a separate biome.
# Additive => no vanilla biome is removed.
def _hot_springs(depth):
    return {
        "biome": "leaf:volcanic_hot_springs",
        "parameters": {
            "continentalness": [0.03, 0.1],
            "depth": depth,
            "erosion": [-0.375, 0.05],
            "humidity": [0.1, 0.5],
            "offset": 0.0,
            "temperature": [0.1, 0.4],
            "weirdness": [0.4, 0.7],
        },
    }

NEW_BIOMES = [
    _sliver("leaf:glass_beach", [-0.19, -0.11], 0.0),
    _sliver("leaf:glass_beach", [-0.19, -0.11], 1.0),
    _sliver("leaf:volcanic_mountain", [-0.11, 0.03], 0.0),
    _sliver("leaf:volcanic_mountain", [-0.11, 0.03], 1.0),
    # Ashen Plains (Tier 2 #7): the volcano's outer ash ring. Reuses the hot/dry
    # volcanic climate (_sliver) but one continentalness band further inland
    # [0.03, 0.1] so it forms a grey apron around the volcanic_mountain cone.
    _sliver("leaf:ashen_plains", [0.03, 0.1], 0.0),
    _sliver("leaf:ashen_plains", [0.03, 0.1], 1.0),
    _hot_springs(0.0),
    _hot_springs(1.0),
    _lavender(0.0),
    _lavender(1.0),
]

new_biomes = list(NEW_BIOMES)
counts = {k: 0 for k in SPLITS}
counts["minecraft:flower_forest"] = 0
for entry in report["biomes"]:
    tiers = SPLITS.get(entry["biome"])
    if entry["biome"] == "minecraft:flower_forest":
        # Conditional rainbow swap on the WEIRDNESS axis: the low sub-band of this
        # entry's weirdness span (width RAINBOW_WEIRDNESS_FRACTION) is replaced by the
        # rainbow colour gradient (banded across humidity); the remaining weirdness
        # stays vanilla minecraft:flower_forest. So rainbow forests appear only where
        # the weirdness noise lands in that sub-band.
        counts["minecraft:flower_forest"] += 1
        w = entry["parameters"]["weirdness"]
        wlo, whi = float(w[0]), float(w[1])
        wsplit = wlo + (whi - wlo) * RAINBOW_WEIRDNESS_FRACTION
        h = entry["parameters"]["humidity"]
        hlo, hhi = float(h[0]), float(h[1])
        hspan = hhi - hlo
        for name, a, b in _rainbow_humidity_bands():
            c = copy.deepcopy(entry)
            c["biome"] = name
            c["parameters"]["weirdness"] = [round(wlo, 4), round(wsplit, 4)]
            c["parameters"]["humidity"] = [round(hlo + hspan * a, 4), round(hlo + hspan * b, 4)]
            new_biomes.append(c)
        # Keep the vanilla flower_forest remainder only if the rainbow swap did
        # NOT consume the entire weirdness span. With RAINBOW_WEIRDNESS_FRACTION
        # >= 1.0 the remainder would be a zero-width [whi, whi] range, so skip it
        # and let the whole former flower_forest read as rainbow forest.
        if RAINBOW_WEIRDNESS_FRACTION < 1.0:
            rest = copy.deepcopy(entry)
            rest["parameters"]["weirdness"] = [round(wsplit, 4), round(whi, 4)]
            new_biomes.append(rest)
    elif tiers:
        counts[entry["biome"]] += 1
        h = entry["parameters"]["humidity"]
        lo, hi = float(h[0]), float(h[1])
        span = hi - lo
        for name, a, b in tiers:
            c = copy.deepcopy(entry)
            c["biome"] = name
            c["parameters"]["humidity"] = [round(lo + span * a, 4), round(lo + span * b, 4)]
            new_biomes.append(c)
    else:
        new_biomes.append(entry)

# Make room between ocean and land for beaches and inclines. Continentalness is the
# ocean<->inland axis; vanilla packs the coast (beach) band into [-0.19, -0.11]
# (width 0.08) and the near-inland "incline" band into [-0.11, 0.03] (width 0.14).
# We apply a single MONOTONIC piecewise-linear remap to every biome entry's
# continentalness range so those two bands are stretched (and the large far-inland
# band absorbs the compression). Ocean boundaries (<= -0.19) are kept fixed so the
# ocean extent is unchanged; only the coastline transition gets wider.
CONT_REMAP = [
    (-1.2, -1.2),
    (-1.05, -1.05),
    (-0.455, -0.455),
    (-0.19, -0.19),   # ocean/coast boundary fixed
    (-0.11, -0.03),   # coast (beach) band 0.08 -> 0.16
    (0.03, 0.15),     # near-inland (incline) band 0.14 -> 0.18
    (0.3, 0.42),      # mid-inland width preserved
    (0.8, 0.86),
    (1.0, 1.0),       # far-inland absorbs the compression
]

def _remap_cont(x):
    if x <= CONT_REMAP[0][0]:
        return CONT_REMAP[0][1]
    if x >= CONT_REMAP[-1][0]:
        return CONT_REMAP[-1][1]
    for i in range(len(CONT_REMAP) - 1):
        x0, y0 = CONT_REMAP[i]
        x1, y1 = CONT_REMAP[i + 1]
        if x0 <= x <= x1:
            t = (x - x0) / (x1 - x0) if x1 > x0 else 0.0
            return y0 + t * (y1 - y0)
    return x

for c in new_biomes:
    cont = c["parameters"].get("continentalness")
    if isinstance(cont, list):
        c["parameters"]["continentalness"] = [
            round(_remap_cont(float(cont[0])), 4),
            round(_remap_cont(float(cont[1])), 4),
        ]

# Use the standard "minecraft:overworld" noise settings (regular biome size).
dim = {
    "type": "minecraft:overworld",
    "generator": {
        "type": "minecraft:noise",
        "settings": "minecraft:overworld",
        "biome_source": {
            "type": "minecraft:multi_noise",
            "biomes": new_biomes,
        },
    },
}

os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(dim, f, indent=2)

for k, v in counts.items():
    print(f"{k} source entries:", v)
print("total biome entries written:", len(new_biomes))
print("output:", OUT)
