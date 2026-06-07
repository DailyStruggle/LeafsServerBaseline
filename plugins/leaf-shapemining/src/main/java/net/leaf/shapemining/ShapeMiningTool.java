package net.leaf.shapemining;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the shape-mining tool {@link ItemStack} and reads/writes its current
 * {@link MineShape} mode via the item's {@code PersistentDataContainer}.
 *
 * <p>The mode lives on the item itself (not in a transient session map) so it
 * survives relog, death/grave recovery, and item transfer, per the design
 * note.</p>
 */
public final class ShapeMiningTool {

    private static final byte FLAG = (byte) 1;

    private ShapeMiningTool() {
    }

    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(Component.text("Excavator", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(Keys.SHAPE_TOOL, PersistentDataType.BYTE, FLAG);
        pdc.set(Keys.MODE, PersistentDataType.INTEGER, MineShape.OFF.ordinal());
        applyLore(meta, MineShape.OFF);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isTool(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(Keys.SHAPE_TOOL, PersistentDataType.BYTE);
    }

    public static MineShape getMode(ItemStack item) {
        if (!isTool(item)) {
            return MineShape.OFF;
        }
        Integer ordinal = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.MODE, PersistentDataType.INTEGER);
        return ordinal == null ? MineShape.OFF : MineShape.fromOrdinal(ordinal);
    }

    /** Mutates the given item in place to record the new mode and refresh its lore. */
    public static void setMode(ItemStack item, MineShape shape) {
        if (!isTool(item)) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.MODE, PersistentDataType.INTEGER, shape.ordinal());
        applyLore(meta, shape);
        item.setItemMeta(meta);
    }

    private static void applyLore(ItemMeta meta, MineShape shape) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Mode: " + shape.size() + " (" + shape.label() + ")", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Sneak + scroll to change shape", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shape-Mining Tool", NamedTextColor.DARK_GREEN)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
    }
}
