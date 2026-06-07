package net.leaf.artifacts;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Exploration artifacts (wearable trinkets) recreated with vanilla mechanics.
 *
 * <p>P1 (from {@code docs/scratch/ARTIFACTS-VANILLA-RECREATION.md}): artifacts
 * whose effect is a passive attribute modifier and/or an infinite potion
 * effect that is present while the artifact is carried. A per-player reconcile
 * loop (driven by the Folia-safe entity scheduler) keeps each player's
 * modifiers/effects in sync with their inventory.</p>
 *
 * <p>P2: simple event-reactive artifacts (lifesteal, ignite/lightning on hit,
 * speed when hurt, fall-damage immunity, haste on eating, bonus kill XP),
 * handled immediately in {@link ArtifactEventListener} on the triggering
 * event's region thread.</p>
 *
 * <p>Not yet implemented (deferred): the signature movement combos and other
 * ticking artifacts, loot-table/datapack sourcing, and resource-pack custom
 * models. This build is for review only.</p>
 */
public final class LeafArtifactsPlugin extends JavaPlugin {

    private static final long INITIAL_DELAY_TICKS = 1L;
    private static final long PERIOD_TICKS = 20L;

    private ArtifactController controller;
    private ArtifactEquipment equipment;

    @Override
    public void onEnable() {
        ArtifactKeys.init(this);
        this.equipment = new ArtifactEquipment(getLogger());
        this.controller = new ArtifactController(equipment);

        ArtifactMenu menu = new ArtifactMenu(equipment, controller);

        getServer().getPluginManager().registerEvents(new ArtifactListener(this, controller, equipment), this);
        getServer().getPluginManager().registerEvents(new ArtifactEventListener(equipment), this);
        getServer().getPluginManager().registerEvents(new CloudJumpListener(this, equipment), this);
        getServer().getPluginManager().registerEvents(menu, this);

        ArtifactCommand command = new ArtifactCommand(menu);
        if (getCommand("artifact") != null) {
            getCommand("artifact").setExecutor(command);
            getCommand("artifact").setTabCompleter(command);
        }

        // Handle players already online (e.g. after a /reload) by loading their
        // equipment and starting their reconcile loop now; on a fresh start this
        // set is empty.
        for (Player player : getServer().getOnlinePlayers()) {
            equipment.load(player);
            player.getScheduler().runAtFixedRate(
                    this,
                    task -> controller.reconcile(player),
                    null,
                    INITIAL_DELAY_TICKS,
                    PERIOD_TICKS);
        }

        getLogger().info("LeafArtifacts enabled.");
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            if (equipment != null) {
                equipment.save(player);
            }
            if (controller != null) {
                controller.clear(player);
            }
        }
    }
}
