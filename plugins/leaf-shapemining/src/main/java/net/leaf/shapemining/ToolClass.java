package net.leaf.shapemining;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * The vanilla tool families the shape/vein system understands. Every tool class
 * follows the same format (filler shapes + a vein/flood-fill list); only the
 * material lists differ, so mining is never the only thing this speeds up: the
 * same ergonomics apply to digging (shovel), tree-felling (axe), and farming
 * (hoe). This keeps the game a "search and build" game rather than a tedious
 * mining game.
 */
public enum ToolClass {

    PICKAXE,
    AXE,
    SHOVEL,
    HOE;

    /** Resolves the tool class of a held item, or {@code null} if it is not a supported tool. */
    public static ToolClass of(ItemStack item) {
        if (item == null) {
            return null;
        }
        return of(item.getType());
    }

    /** Resolves the tool class of a material, or {@code null} if it is not a supported tool. */
    public static ToolClass of(Material material) {
        if (material == null) {
            return null;
        }
        String name = material.name();
        if (name.endsWith("_PICKAXE")) {
            return PICKAXE;
        }
        if (name.endsWith("_AXE")) {
            return AXE;
        }
        if (name.endsWith("_SHOVEL")) {
            return SHOVEL;
        }
        if (name.endsWith("_HOE")) {
            return HOE;
        }
        return null;
    }

    /** Lower-cased config-section key for this tool class (e.g. {@code "pickaxe"}). */
    public String configKey() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
