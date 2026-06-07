package net.leaf.artifacts;

/**
 * The curio (trinket) slot categories used by the artifact equipment menu.
 *
 * <p>Each slot in the menu is dedicated to one category; an artifact declares
 * (in {@link ArtifactSlots}) the set of categories it may be placed into, so a
 * single artifact can be valid for more than one slot type.</p>
 */
public enum CurioSlot {

    HEAD("Head"),
    NECKLACE("Necklace"),
    HANDS("Hands"),
    RING("Ring"),
    CHARM("Charm"),
    FEET("Feet");

    private final String displayName;

    CurioSlot(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
