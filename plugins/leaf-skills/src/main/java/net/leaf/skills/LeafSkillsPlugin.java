package net.leaf.skills;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Advancement-driven passive skill selection.
 *
 * <p>Companion to the {@code leaf-skilltree} datapack, which renders the tree in
 * the vanilla advancement screen and exposes a single {@code skill_pick} trigger
 * objective. This plugin polls that trigger on each player's own scheduler
 * (Folia-safe), validates the selection (classless prerequisites, mutual
 * exclusions, achievement hard-gates), charges a rising number of experience
 * levels ({@code base + step * pointsSpent}), awards the matching advancement,
 * and reconciles the resulting buffs. Resets never refund experience.</p>
 */
public final class LeafSkillsPlugin extends JavaPlugin implements Listener {

    private static final long INITIAL_DELAY_TICKS = 20L;

    private SkillConfig config;
    private SkillController controller;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = SkillConfig.load(getConfig(), getLogger());
        this.controller = new SkillController(this, config);

        getServer().getPluginManager().registerEvents(this, this);

        SkillCommand command = new SkillCommand(controller);
        if (getCommand("leafskills") != null) {
            getCommand("leafskills").setExecutor(command);
            getCommand("leafskills").setTabCompleter(command);
        }

        // Players already online (e.g. after /reload) need their loop started now.
        for (Player player : getServer().getOnlinePlayers()) {
            startLoop(player);
        }

        getLogger().info("LeafSkills enabled.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        startLoop(event.getPlayer());
    }

    private void startLoop(Player player) {
        player.getScheduler().runAtFixedRate(
                this,
                task -> controller.pollAndReconcile(player),
                null,
                INITIAL_DELAY_TICKS,
                config.pollIntervalTicks());
    }
}
