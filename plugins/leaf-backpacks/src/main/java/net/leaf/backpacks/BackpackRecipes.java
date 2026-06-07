package net.leaf.backpacks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Registers crafting recipes that turn rare materials into upgrade modules,
 * mirroring how Sophisticated Backpacks gates each function behind a distinct
 * recipe. Each active {@link UpgradeType} gets one shaped recipe whose result is
 * the tagged upgrade item (rendered as an "upgrade enchantment book" when
 * {@code upgrades.as-book} is enabled). The crafted module is then installed by
 * dropping it into one of a backpack's tier-gated upgrade slots.
 */
public final class BackpackRecipes {

    private final Plugin plugin;
    private final BackpackItem items;
    private final Logger log;
    private final List<NamespacedKey> registered = new ArrayList<>();

    public BackpackRecipes(Plugin plugin, BackpackItem items) {
        this.plugin = plugin;
        this.items = items;
        this.log = plugin.getLogger();
    }

    /** Register a recipe for every active upgrade type. Idempotent per type key. */
    public void registerAll() {
        for (UpgradeType type : UpgradeType.values()) {
            if (!type.isActive()) {
                continue;
            }
            ShapedRecipe recipe = build(type);
            if (recipe == null) {
                continue;
            }
            // Replace any stale recipe left over from a previous load.
            Bukkit.removeRecipe(recipe.getKey());
            if (Bukkit.addRecipe(recipe)) {
                registered.add(recipe.getKey());
            }
        }
        log.info("Registered " + registered.size() + " upgrade recipe(s).");
    }

    /** Remove the recipes we registered (called on disable/reload). */
    public void unregisterAll() {
        for (NamespacedKey key : registered) {
            Bukkit.removeRecipe(key);
        }
        registered.clear();
    }

    private NamespacedKey keyFor(UpgradeType type) {
        return new NamespacedKey(plugin, "upgrade_" + type.name().toLowerCase());
    }

    /**
     * Build the rare-material recipe for a given upgrade type. Shapes use a 3x3
     * grid; the centre ingredient is the "rare core" that identifies the module.
     */
    private ShapedRecipe build(UpgradeType type) {
        ItemStack result = items.createUpgrade(type);
        ShapedRecipe recipe = new ShapedRecipe(keyFor(type), result);

        switch (type) {
            case STACK -> {
                // Diamonds + pistons around a netherite core.
                recipe.shape("DPD", "PNP", "DPD");
                recipe.setIngredient('D', Material.DIAMOND);
                recipe.setIngredient('P', Material.PISTON);
                recipe.setIngredient('N', Material.NETHERITE_INGOT);
            }
            case MAGNET -> {
                // Iron frame, redstone, ender pearls around a gold core.
                recipe.shape("IRI", "ENE", "IRI");
                recipe.setIngredient('I', Material.IRON_INGOT);
                recipe.setIngredient('R', Material.REDSTONE);
                recipe.setIngredient('E', Material.ENDER_PEARL);
                recipe.setIngredient('N', Material.GOLD_INGOT);
            }
            case PICKUP -> {
                // String + hoppers around an ender-eye core.
                recipe.shape("SHS", "HEH", "SHS");
                recipe.setIngredient('S', Material.STRING);
                recipe.setIngredient('H', Material.HOPPER);
                recipe.setIngredient('E', Material.ENDER_EYE);
            }
            case FEEDING -> {
                // Gold + golden carrots around a golden-apple core.
                recipe.shape("GCG", "CAC", "GCG");
                recipe.setIngredient('G', Material.GOLD_INGOT);
                recipe.setIngredient('C', Material.GOLDEN_CARROT);
                recipe.setIngredient('A', Material.GOLDEN_APPLE);
            }
            default -> {
                return null;
            }
        }
        return recipe;
    }
}
