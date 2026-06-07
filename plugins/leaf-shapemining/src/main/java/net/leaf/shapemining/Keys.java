package net.leaf.shapemining;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/**
 * Central holder for the plugin's {@link NamespacedKey}s. Initialised once on
 * enable.
 */
public final class Keys {

    /** Marks an item as a shape-mining tool (byte flag, value 1). */
    public static NamespacedKey SHAPE_TOOL;

    /** Stores the tool's current {@link MineShape} ordinal (int). */
    public static NamespacedKey MODE;

    private Keys() {
    }

    public static void init(Plugin owner) {
        SHAPE_TOOL = new NamespacedKey(owner, "shape_tool");
        MODE = new NamespacedKey(owner, "mode");
    }
}
