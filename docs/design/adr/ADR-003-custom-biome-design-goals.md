# ADR-003: Custom Biome Design Goals - Elegant Builds and Concept Sharing

**Date:** 2026-06-04
**Status:** Accepted

---

## Context

The server uses Iris Dimension Engine (Paper plugin) to generate custom biomes beyond the 64 vanilla Minecraft biomes. Two competing pressures shape every biome design decision:

1. **Vanilla client compatibility** - players must be able to join with an unmodified Java Edition client. Iris satisfies this via the `derivative` and `vanillaDerivative` fields in each biome JSON, which map the custom biome onto an existing vanilla biome ID for the client. The client never sees a foreign biome ID; it receives a vanilla one while the server generates fully custom terrain.

2. **Creative build quality** - biomes should actively inspire players to build well-composed, shareable structures rather than generic boxes. A biome that offers no distinct palette, silhouette, or atmospheric identity produces forgettable builds.

These two goals are not in conflict: vanilla compatibility is a hard technical constraint handled by Iris, while build quality is a design constraint applied during biome authoring.

---

## Decision

Every custom biome added to this server must satisfy all of the following design goals before it is considered complete:

### Goal 1: Vanilla Client Compatibility

- The biome JSON **must** set both `derivative` and `vanillaDerivative` to a valid `minecraft:` biome ID.
- The chosen vanilla derivative should be the closest climate/mood match to avoid jarring ambient sound or sky color mismatches on vanilla clients.
- No custom biome ID may be exposed to the client directly.

### Goal 2: Distinct Block Palette

- Each biome must define a primary surface palette of 2-4 blocks that are visually distinct from adjacent biomes.
- At least one block in the palette should be non-trivially obtainable in vanilla (i.e., not just dirt/stone), giving players a material incentive to visit.
- Custom blocks (added via datapack) are permitted but must have a vanilla fallback palette for compatibility testing.

### Goal 3: Recognizable Silhouette

- The terrain generator profile (height, erosion, feature density) must produce a skyline or ground shape that a player can identify from a distance.
- Flat biomes must compensate with dense or tall surface features (tall grass, large trees, rock formations).
- Vertical biomes must have at least one navigable path to the top without requiring flight.

### Goal 4: Build Theme Anchor

- Each biome must be documented with at least one named build theme (e.g., "gothic fortress", "stilt village", "geothermal factory").
- The theme must be achievable using blocks available in the biome's own palette plus standard vanilla blocks.
- The theme serves as a creative prompt for players and a quality bar for the biome designer.

### Goal 5: Shareable Concept

- Biomes should be designed so that a screenshot of a player build within them is immediately legible as belonging to that biome.
- Avoid generic "green hills" or "brown dirt" profiles that produce builds indistinguishable from vanilla plains or forests.
- Each biome entry in `docs/design/CUSTOM-BIOMES.md` must include a one-sentence "concept hook" that a player could use to pitch their build to others.

---

## Consequences

- Biome authors must fill out all fields in the `CUSTOM-BIOMES.md` catalog entry before the biome JSON is merged.
- The `derivative` mapping must be reviewed for ambient sound and sky color accuracy on each new Minecraft version.
- Build theme anchors are design guidance, not enforcement - players are free to build anything, but the biome should make the anchor theme feel natural and rewarding.
- Biomes that fail Goal 3 (no recognizable silhouette) are candidates for terrain generator revision before release.

---

## Alternatives Considered

- **Require client-side resource pack for custom biome names** - rejected; adds friction for new players and breaks the vanilla compatibility goal.
- **Use only vanilla biome overrides (datapack `worldgen/biome/`)** - viable for sky/fog/grass color changes but cannot produce the terrain complexity Iris enables; rejected as insufficient.
- **No build theme requirement** - rejected; past experience shows undirected biomes produce low build engagement and are rarely featured in community screenshots.
