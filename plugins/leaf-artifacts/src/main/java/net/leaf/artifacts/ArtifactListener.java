package net.leaf.artifacts;

import net.leaf.curios.api.CuriosApi;
import net.leaf.curios.api.event.CurioEquipEvent;
import net.leaf.curios.api.event.CurioUnequipEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Wires each online player to a lightweight per-player reconcile loop and reacts
 * to curio equip/unequip events from LeafCurios.
 *
 * <p>Uses the player's entity scheduler (Folia-safe; also supported on regular
 * Paper) so all artifact state changes run on the thread that owns the player.
 * The curio store itself (load/save/persistence) is owned by LeafCurios; this
 * plugin only recomputes attribute modifiers / infinite effects from the active
 * curio set whenever it changes (and once per second as a safety net).</p>
 */
public final class ArtifactListener implements Listener {

    private static final long INITIAL_DELAY_TICKS = 2L;
    private static final long PERIOD_TICKS = 20L;

    private final Plugin plugin;
    private final ArtifactController controller;
    private final RollerSkatesController rollerSkates;
    private final HeliumFlamingoController heliumFlamingo;
    private final FlippersController flippers;
    private final CuriosApi curios;

    public ArtifactListener(Plugin plugin, ArtifactController controller,
                            RollerSkatesController rollerSkates,
                            HeliumFlamingoController heliumFlamingo,
                            FlippersController flippers, CuriosApi curios) {
        this.plugin = plugin;
        this.controller = controller;
        this.rollerSkates = rollerSkates;
        this.heliumFlamingo = heliumFlamingo;
        this.flippers = flippers;
        this.curios = curios;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Slight delay so LeafCurios has loaded this player's curio store first.
        // Migrate any legacy in-plugin equipment, then strip stale persisted
        // modifiers and re-apply the active set cleanly (so equipped artifacts
        // are effective immediately on login without a manual re-equip).
        player.getScheduler().runDelayed(
                plugin,
                task -> {
                    ArtifactLegacyMigration.migrate(plugin, player, curios);
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
        rollerSkates.start(player);
        heliumFlamingo.start(player);
        flippers.start(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        controller.clear(event.getPlayer());
        rollerSkates.clear(event.getPlayer());
        heliumFlamingo.clear(event.getPlayer());
        flippers.clear(event.getPlayer());
    }

    @EventHandler
    public void onCurioEquip(CurioEquipEvent event) {
        controller.reconcile(event.getPlayer());
    }

    @EventHandler
    public void onCurioUnequip(CurioUnequipEvent event) {
        controller.reconcile(event.getPlayer());
    }
}
