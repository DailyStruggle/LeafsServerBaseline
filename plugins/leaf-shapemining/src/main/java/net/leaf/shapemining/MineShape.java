package net.leaf.shapemining;

/**
 * The mining shapes a tool can be set to.
 *
 * <p>Cycle order (used by sneak-scroll mode switching) follows the enum
 * declaration order: {@link #OFF} -&gt; {@link #CORRIDOR} -&gt; {@link #SQUARE}
 * -&gt; {@link #TUNNEL} -&gt; {@link #VEIN} -&gt; (full rotation) back to
 * {@link #OFF}.</p>
 */
public enum MineShape {

    /** 1x1: precise / off. Behaves like a normal pickaxe. */
    OFF("1x1", "Precise"),

    /** 1x2: player-height corridor. */
    CORRIDOR("1x2", "Corridor"),

    /** 3x3: fixed cross-section room/corridor clear. */
    SQUARE("3x3", "Square"),

    /** 1x1xN: tunnel-bore along the look direction. */
    TUNNEL("1x1xN", "Tunnel-bore"),

    /**
     * Ultimine-style vein miner for low-value ores only. Breaking a configured
     * low-value ore (e.g. coal) clears the connected vein of the same ore;
     * high-dopamine ores (diamond/gold/redstone/etc.) are intentionally excluded
     * to preserve the "find" payoff. Filler is left to the other shapes.
     */
    VEIN("vein", "Ore vein");

    private final String size;
    private final String label;

    MineShape(String size, String label) {
        this.size = size;
        this.label = label;
    }

    public String size() {
        return size;
    }

    public String label() {
        return label;
    }

    /** Returns the shape for the given ordinal, defaulting to {@link #OFF}. */
    public static MineShape fromOrdinal(int ordinal) {
        MineShape[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return OFF;
        }
        return values[ordinal];
    }
}
