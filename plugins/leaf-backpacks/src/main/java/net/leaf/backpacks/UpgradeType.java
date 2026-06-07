package net.leaf.backpacks;

import org.bukkit.Material;

/**
 * The function modules a backpack can carry in its (tier-gated) upgrade slots.
 *
 * <p>STACK, MAGNET, PICKUP and FEEDING are active in v1. VOID is reserved (the
 * filtering UI is not implemented yet) and is intentionally left inert.</p>
 */
public enum UpgradeType {

    STACK("Stack Upgrade", Material.ANVIL),
    MAGNET("Magnet Upgrade", Material.IRON_INGOT),
    PICKUP("Pickup Upgrade", Material.HOPPER),
    FEEDING("Feeding Upgrade", Material.GOLDEN_APPLE),
    VOID("Void Upgrade (reserved)", Material.LAVA_BUCKET);

    private final String displayName;
    private final Material icon;

    UpgradeType(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    /** @return whether this upgrade has active behaviour in v1. */
    public boolean isActive() {
        return this != VOID;
    }

    public static UpgradeType fromName(String name) {
        if (name == null) {
            return null;
        }
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
