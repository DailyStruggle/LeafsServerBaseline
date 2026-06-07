package net.leaf.backpacks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Factory and inspector for the backpack item. The item carries only an id and
 * tier in its {@code PersistentDataContainer}; contents and installed upgrades
 * live off-item in {@link BackpackStore}, keyed by the id (dupe-safe).
 */
public final class BackpackItem {

    private final BackpackConfig config;

    public BackpackItem(BackpackConfig config) {
        this.config = config;
    }

    /** Create a fresh backpack item of the given tier with a brand-new storage id. */
    public ItemStack create(int tier) {
        int t = config.clampTier(tier);
        ItemStack item = new ItemStack(config.baseItem());
        ItemMeta meta = item.getItemMeta();

        UUID id = UUID.randomUUID();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(Keys.BACKPACK_ID, PersistentDataType.STRING, id.toString());
        pdc.set(Keys.BACKPACK_TIER, PersistentDataType.INTEGER, t);

        meta.displayName(Component.text("Backpack (Tier " + t + ")", NamedTextColor.GOLD)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        applyIcon(meta, t);
        item.setItemMeta(meta);
        return item;
    }

    private void applyIcon(ItemMeta meta, int tier) {
        if (!config.customIconEnabled()) {
            return;
        }
        NamespacedKey model = NamespacedKey.fromString(config.modelForTier(tier));
        if (model != null) {
            meta.setItemModel(model);
        }
    }

    public boolean isBackpack(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .has(Keys.BACKPACK_ID, PersistentDataType.STRING);
    }

    public UUID getId(ItemStack item) {
        if (!isBackpack(item)) {
            return null;
        }
        String raw = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.BACKPACK_ID, PersistentDataType.STRING);
        try {
            return raw == null ? null : UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public int getTier(ItemStack item) {
        if (!isBackpack(item)) {
            return BackpackConfig.MIN_TIER;
        }
        Integer tier = item.getItemMeta().getPersistentDataContainer()
                .get(Keys.BACKPACK_TIER, PersistentDataType.INTEGER);
        return tier == null ? BackpackConfig.MIN_TIER : config.clampTier(tier);
    }

    /** Apply a new tier to an existing backpack item (mutates and returns it). */
    public ItemStack withTier(ItemStack item, int newTier) {
        int t = config.clampTier(newTier);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.BACKPACK_TIER, PersistentDataType.INTEGER, t);
        meta.displayName(Component.text("Backpack (Tier " + t + ")", NamedTextColor.GOLD)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        applyIcon(meta, t);
        item.setItemMeta(meta);
        return item;
    }

    /** Create an upgrade-module item of the given type. */
    public ItemStack createUpgrade(UpgradeType type) {
        boolean asBook = config.upgradesAsBook();
        ItemStack item = new ItemStack(asBook ? org.bukkit.Material.ENCHANTED_BOOK : type.icon());
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(Keys.UPGRADE_TYPE, PersistentDataType.STRING, type.name());
        meta.displayName(Component.text(type.displayName(), NamedTextColor.AQUA)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(
                Component.text("Backpack upgrade module", NamedTextColor.DARK_PURPLE)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
                Component.text("Install it in a backpack upgrade slot.", NamedTextColor.GRAY)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)));
        if (asBook) {
            meta.setEnchantmentGlintOverride(true);
        }
        item.setItemMeta(meta);
        return item;
    }

    public boolean isUpgrade(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .has(Keys.UPGRADE_TYPE, PersistentDataType.STRING);
    }

    public UpgradeType upgradeType(ItemStack item) {
        if (!isUpgrade(item)) {
            return null;
        }
        return UpgradeType.fromName(item.getItemMeta().getPersistentDataContainer()
                .get(Keys.UPGRADE_TYPE, PersistentDataType.STRING));
    }
}
