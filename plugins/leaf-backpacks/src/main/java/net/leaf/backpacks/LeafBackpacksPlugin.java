package net.leaf.backpacks;

import net.leaf.backpacks.upgrades.UpgradeRunner;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Sophisticated-lite backpacks for Leaf's server.
 *
 * <p>A backpack is a vanilla {@code container}-style item whose contents and
 * installed upgrades are persisted off-item by UUID (dupe-safe). Tiers gate how
 * many upgrade slots (functions) a backpack exposes; the main grid stays at 27.
 * Upgrade modules (STACK, MAGNET, PICKUP, FEEDING; VOID reserved) drive the
 * behaviour. An optional resource-pack-delivered custom icon can be enabled in
 * config; clients without the pack still get a fully functional backpack.</p>
 */
public final class LeafBackpacksPlugin extends JavaPlugin {

    private BackpackConfig backpackConfig;
    private BackpackStore store;
    private BackpackItem itemFactory;
    private BackpackRecipes recipes;
    private final Set<UUID> openBackpacks = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Keys.init(this);

        this.backpackConfig = BackpackConfig.from(getConfig());
        this.itemFactory = new BackpackItem(backpackConfig);
        this.store = new BackpackStore(getDataFolder(), getLogger());
        this.store.load();

        getServer().getPluginManager().registerEvents(new BackpackListener(this), this);

        BackpackCommand command = new BackpackCommand(this);
        if (getCommand("backpack") != null) {
            getCommand("backpack").setExecutor(command);
            getCommand("backpack").setTabCompleter(command);
        }

        new UpgradeRunner(this, backpackConfig, store, itemFactory, openBackpacks)
                .runTaskTimer(this, 20L, 20L);

        this.recipes = new BackpackRecipes(this, itemFactory);
        if (backpackConfig.upgradesCraftable()) {
            recipes.registerAll();
        }

        getLogger().info("LeafBackpacks enabled.");
    }

    @Override
    public void onDisable() {
        if (recipes != null) {
            recipes.unregisterAll();
        }
        if (store != null) {
            store.save();
        }
    }

    public BackpackConfig backpackConfig() {
        return backpackConfig;
    }

    public BackpackStore store() {
        return store;
    }

    public BackpackItem itemFactory() {
        return itemFactory;
    }

    public Set<UUID> openBackpacks() {
        return openBackpacks;
    }
}
