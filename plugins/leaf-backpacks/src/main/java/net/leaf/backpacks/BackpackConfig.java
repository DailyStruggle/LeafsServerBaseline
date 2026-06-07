package net.leaf.backpacks;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Immutable snapshot of the LeafBackpacks configuration, parsed once on enable.
 */
public final class BackpackConfig {

    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 5;

    private final Map<Integer, Integer> tierSlots;
    private final int tierUpCost;
    private final int stackMaxSize;
    private final double magnetRadius;
    private final int feedingThreshold;
    private final boolean customIconEnabled;
    private final Material baseItem;
    private final String iconModel;
    private final Map<Integer, String> perTierModels;
    private final boolean upgradesCraftable;
    private final boolean upgradesAsBook;

    private BackpackConfig(Map<Integer, Integer> tierSlots, int tierUpCost, int stackMaxSize,
                           double magnetRadius, int feedingThreshold, boolean customIconEnabled,
                           Material baseItem, String iconModel, Map<Integer, String> perTierModels,
                           boolean upgradesCraftable, boolean upgradesAsBook) {
        this.tierSlots = tierSlots;
        this.tierUpCost = tierUpCost;
        this.stackMaxSize = stackMaxSize;
        this.magnetRadius = magnetRadius;
        this.feedingThreshold = feedingThreshold;
        this.customIconEnabled = customIconEnabled;
        this.baseItem = baseItem;
        this.iconModel = iconModel;
        this.perTierModels = perTierModels;
        this.upgradesCraftable = upgradesCraftable;
        this.upgradesAsBook = upgradesAsBook;
    }

    public static BackpackConfig from(FileConfiguration c) {
        Map<Integer, Integer> slots = new HashMap<>();
        ConfigurationSection tiers = c.getConfigurationSection("tiers");
        if (tiers != null) {
            for (String key : tiers.getKeys(false)) {
                try {
                    slots.put(Integer.parseInt(key), Math.max(0, tiers.getInt(key)));
                } catch (NumberFormatException ignored) {
                    // skip malformed tier keys
                }
            }
        }
        // Sensible defaults if a tier is missing.
        for (int t = MIN_TIER; t <= MAX_TIER; t++) {
            slots.putIfAbsent(t, t);
        }

        Map<Integer, String> models = new HashMap<>();
        ConfigurationSection perTier = c.getConfigurationSection("custom-icon.per-tier-models");
        if (perTier != null) {
            for (String key : perTier.getKeys(false)) {
                try {
                    models.put(Integer.parseInt(key), perTier.getString(key));
                } catch (NumberFormatException ignored) {
                    // skip malformed tier keys
                }
            }
        }

        Material base = Material.matchMaterial(c.getString("custom-icon.base-item", "SHULKER_BOX"));
        if (base == null) {
            base = Material.SHULKER_BOX;
        }

        return new BackpackConfig(
                slots,
                Math.max(0, c.getInt("tier-up-cost", 4)),
                Math.min(99, Math.max(1, c.getInt("stack-upgrade.max-stack-size", 99))),
                Math.max(0.0, c.getDouble("magnet.radius", 6.0)),
                Math.max(0, c.getInt("feeding.food-threshold", 16)),
                c.getBoolean("custom-icon.enabled", false),
                base,
                c.getString("custom-icon.model", "leafbackpacks:backpack"),
                models,
                c.getBoolean("upgrades.craftable", true),
                c.getBoolean("upgrades.as-book", true)
        );
    }

    public int clampTier(int tier) {
        return Math.max(MIN_TIER, Math.min(MAX_TIER, tier));
    }

    public int upgradeSlots(int tier) {
        return tierSlots.getOrDefault(clampTier(tier), clampTier(tier));
    }

    public int tierUpCost() {
        return tierUpCost;
    }

    public int stackMaxSize() {
        return stackMaxSize;
    }

    public double magnetRadius() {
        return magnetRadius;
    }

    public int feedingThreshold() {
        return feedingThreshold;
    }

    public boolean customIconEnabled() {
        return customIconEnabled;
    }

    public Material baseItem() {
        return baseItem;
    }

    public String iconModel() {
        return iconModel;
    }

    public String modelForTier(int tier) {
        return perTierModels.getOrDefault(clampTier(tier), iconModel);
    }

    public boolean upgradesCraftable() {
        return upgradesCraftable;
    }

    public boolean upgradesAsBook() {
        return upgradesAsBook;
    }
}
