package net.leaf.artifacts;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Wires each online player to a lightweight per-player reconcile loop.
 *
 * <p>Uses the player's entity scheduler (Folia-safe; also supported on regular
 * Paper) so all artifact state changes run on the thread that owns the player.
 * A periodic reconcile keeps the design simple for this first pass: any way an
 * artifact enters or leaves the inventory is picked up within roughly one
 * second without hooking every inventory event.</p>
 */
public final class ArtifactListener implements Listener {

    private static final long INITIAL_DELAY_TICKS = 1L;
    private static final long PERIOD_TICKS = 20L;

    private final Plugin plugin;
    private final ArtifactController controller;
    private final ArtifactEquipment equipment;

    public ArtifactListener(Plugin plugin, ArtifactController controller, ArtifactEquipment equipment) {
        this.plugin = plugin;
        this.controller = controller;
        this.equipment = equipment;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        equipment.load(player);
        // Strip any artifact modifiers persisted in the player's NBT from a
        // previous session, then apply the current equipped set cleanly. Without
        // the clear, reconcile would see a stale-but-present modifier and skip
        // re-adding it, leaving the artifact inactive until manually re-equipped.
        player.getScheduler().runDelayed(
                plugin,
                task -> {
                    controller.clear(player);
                    controller.reconcile(player);
                },
                null,
                INITIAL_DELAY_TICKS);
        player.getScheduler().runAtFixedRate(
                plugin,
                task -> controller.reconcile(player),
                null,
                PERIOD_TICKS,
                PERIOD_TICKS);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        controller.clear(player);
        equipment.save(player);
        equipment.unload(player.getUniqueId());
    }
}
