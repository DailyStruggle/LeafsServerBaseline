package net.leaf.backpacks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

/**
 * The open backpack screen. Layout (36 slots / 4 rows):
 * <ul>
 *   <li>slots 0-26: the 27-slot main storage grid;</li>
 *   <li>slots 27..(27+tierSlots-1): unlocked upgrade slots;</li>
 *   <li>remaining bottom-row slots: locked filler.</li>
 * </ul>
 * The number of unlocked upgrade slots is gated by the backpack's tier.
 */
public final class BackpackGui implements InventoryHolder {

    public static final int SIZE = 36;
    public static final int MAIN_START = 0;
    public static final int MAIN_END = 27; // exclusive
    public static final int UPGRADE_ROW_START = 27;

    private final UUID backpackId;
    private final int tier;
    private final int upgradeSlots;
    private final Inventory inventory;

    public BackpackGui(UUID backpackId, int tier, BackpackConfig config, BackpackStore store) {
        this.backpackId = backpackId;
        this.tier = tier;
        this.upgradeSlots = Math.min(BackpackStore.MAX_UPGRADE_SLOTS, config.upgradeSlots(tier));
        this.inventory = Bukkit.createInventory(this, SIZE,
                Component.text("Backpack (Tier " + tier + ")", NamedTextColor.GOLD));

        ItemStack[] contents = store.getContents(backpackId);
        for (int i = MAIN_START; i < MAIN_END; i++) {
            inventory.setItem(i, contents[i]);
        }

        ItemStack[] installed = store.getUpgrades(backpackId);
        for (int i = 0; i < upgradeSlots; i++) {
            inventory.setItem(UPGRADE_ROW_START + i, installed[i]);
        }
        ItemStack locked = lockedFiller();
        for (int i = UPGRADE_ROW_START + upgradeSlots; i < SIZE; i++) {
            inventory.setItem(i, locked);
        }
    }

    public UUID backpackId() {
        return backpackId;
    }

    public int tier() {
        return tier;
    }

    public int upgradeSlots() {
        return upgradeSlots;
    }

    public boolean isMainSlot(int slot) {
        return slot >= MAIN_START && slot < MAIN_END;
    }

    public boolean isUnlockedUpgradeSlot(int slot) {
        return slot >= UPGRADE_ROW_START && slot < UPGRADE_ROW_START + upgradeSlots;
    }

    public boolean isLockedSlot(int slot) {
        return slot >= UPGRADE_ROW_START + upgradeSlots && slot < SIZE;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static ItemStack lockedFiller() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text("Locked upgrade slot", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        filler.setItemMeta(meta);
        return filler;
    }
}
