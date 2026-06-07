package net.leaf.backpacks;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Central holder for the plugin's {@link NamespacedKey}s used in item
 * {@code PersistentDataContainer}s. Initialised once on enable.
 */
public final class Keys {

    /** Marks a backpack item and stores its persistent storage id (UUID string). */
    public static NamespacedKey BACKPACK_ID;
    /** Stores the backpack tier (int). */
    public static NamespacedKey BACKPACK_TIER;
    /** Marks an upgrade module item and stores its {@link UpgradeType} name. */
    public static NamespacedKey UPGRADE_TYPE;

    private Keys() {
    }

    public static void init(Plugin plugin) {
        BACKPACK_ID = new NamespacedKey(plugin, "backpack_id");
        BACKPACK_TIER = new NamespacedKey(plugin, "backpack_tier");
        UPGRADE_TYPE = new NamespacedKey(plugin, "upgrade_type");
    }
}
