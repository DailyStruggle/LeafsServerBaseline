package net.leaf.shapemining;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Vanilla/Folia-safe shape-mining tool (Excavate / tunnel-bore style).
 *
 * <p>v1 (from {@code docs/scratch/SHAPE-MINING-DESIGN.md}): a marked pickaxe
 * carries its {@link MineShape} mode in its {@code PersistentDataContainer}.
 * Sneak + scroll cycles the shape ({@link ModeSwitchListener}); breaking a bulk-
 * filler block then clears the rest of the shape ({@link BlockBreakListener}),
 * stopping at the first block not owned by the current region thread so the whole
 * operation stays on one thread in one tick.</p>
 */
public final class LeafShapeMiningPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Keys.init(this);

        ShapeMiningConfig config = ShapeMiningConfig.load(getConfig(), getLogger());

        getServer().getPluginManager().registerEvents(new ModeSwitchListener(), this);
        getServer().getPluginManager().registerEvents(
                new BlockBreakListener(getServer(), config), this);

        ShapeMineCommand command = new ShapeMineCommand();
        if (getCommand("shapemine") != null) {
            getCommand("shapemine").setExecutor(command);
            getCommand("shapemine").setTabCompleter(command);
        }

        getLogger().info("LeafShapeMining enabled.");
    }
}
