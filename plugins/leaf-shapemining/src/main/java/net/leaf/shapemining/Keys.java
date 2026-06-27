package net.leaf.shapemining;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Central holder for the plugin's {@link NamespacedKey}s. Initialised once on
 * enable.
 */
public final class Keys {

    /** Stores the held tool's current {@link MineShape} ordinal (int). */
    public static NamespacedKey MODE;

    /** Per-player opt-in flag for the whole feature (byte, 1 = enabled). */
    public static NamespacedKey ENABLED;

    private Keys() {
    }

    public static void init(Plugin owner) {
        MODE = new NamespacedKey(owner, "mode");
        ENABLED = new NamespacedKey(owner, "enabled");
    }
}
