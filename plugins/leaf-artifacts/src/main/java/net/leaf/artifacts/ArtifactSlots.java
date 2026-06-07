package net.leaf.artifacts;

import java.util.Set;

/**
 * Layout of the artifact equipment menu: the fixed, per-index slot categories
 * ({@link #LAYOUT}). Which {@link CurioSlot} categories an artifact may occupy
 * is declared by the artifact itself ({@link ArtifactType#slots()}).
 *
 * <p>The menu is a single chest row (9 slots). Two {@link CurioSlot#FEET} slots
 * are provided so footwear artifacts (e.g. Running Shoes + Bunny Hoppers) can be
 * combined. An artifact mapped to more than one category may be placed into any
 * matching slot.</p>
 */
public final class ArtifactSlots {

    /** The category dedicated to each of the 9 menu slots, in order. */
    public static final CurioSlot[] LAYOUT = {
            CurioSlot.HEAD,
            CurioSlot.NECKLACE,
            CurioSlot.HANDS,
            CurioSlot.HANDS,
            CurioSlot.RING,
            CurioSlot.CHARM,
            CurioSlot.CHARM,
            CurioSlot.FEET,
            CurioSlot.FEET,
    };

    /** Number of equipment slots (kept in sync with {@link #LAYOUT}). */
    public static final int SLOT_COUNT = LAYOUT.length;

    private ArtifactSlots() {
    }

    /** The slot category dedicated to the given menu index. */
    public static CurioSlot categoryAt(int index) {
        return LAYOUT[index];
    }

    /** The categories the given artifact may be placed into (never empty). */
    public static Set<CurioSlot> allowed(ArtifactType type) {
        Set<CurioSlot> declared = type.slots();
        return (declared == null || declared.isEmpty()) ? Set.of(CurioSlot.CHARM) : declared;
    }

    /** Whether the artifact may be placed into the slot at the given index. */
    public static boolean fits(ArtifactType type, int index) {
        return allowed(type).contains(categoryAt(index));
    }

    /** The first empty menu index whose category fits the type, or -1. */
    public static int firstFitting(ArtifactType type, ItemPresence presence) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (presence.isEmpty(i) && fits(type, i)) {
                return i;
            }
        }
        return -1;
    }

    /** Minimal callback so {@link #firstFitting} can test slot occupancy. */
    @FunctionalInterface
    public interface ItemPresence {
        boolean isEmpty(int index);
    }
}
