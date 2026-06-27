package net.leaf.shapemining;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Helpers for the shape/vein state, now spread across all vanilla tool classes
 * rather than a single bespoke item.
 *
 * <ul>
 *   <li>The current {@link MineShape} mode lives in the held tool's
 *   {@code PersistentDataContainer} (per-item, survives relog / death / grave
 *   recovery / transfer), and defaults to {@link MineShape#OFF}.</li>
 *   <li>The feature is opt-in per player via a flag stored in the player's
 *   {@code PersistentDataContainer}, toggled with {@code /shapemine}.</li>
 * </ul>
 */
public final class ShapeMiningTool {

    private static final byte FLAG = (byte) 1;

    private ShapeMiningTool() {
    }

    /** A tool is mode-aware when it is one of the supported vanilla tool classes. */
    public static boolean isTool(ItemStack item) {
        return ToolClass.of(item) != null;
    }

    public static MineShape getMode(ItemStack item) {
        if (!isTool(item) || !item.hasItemMeta()) {
            return MineShape.OFF;
        }
        Integer ordinal = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.MODE, PersistentDataType.INTEGER);
        return ordinal == null ? MineShape.OFF : MineShape.fromOrdinal(ordinal);
    }

    /** Mutates the given item in place to record the new mode. */
    public static void setMode(ItemStack item, MineShape shape) {
        if (!isTool(item)) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(Keys.MODE, PersistentDataType.INTEGER, shape.ordinal());
        item.setItemMeta(meta);
    }

    /** Whether the player has opted in to shape/vein behaviour. */
    public static boolean isEnabled(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        Byte flag = pdc.get(Keys.ENABLED, PersistentDataType.BYTE);
        return flag != null && flag == FLAG;
    }

    /** Sets the player's opt-in flag; returns the new state. */
    public static boolean setEnabled(Player player, boolean enabled) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (enabled) {
            pdc.set(Keys.ENABLED, PersistentDataType.BYTE, FLAG);
        } else {
            pdc.remove(Keys.ENABLED);
        }
        return enabled;
    }
}
