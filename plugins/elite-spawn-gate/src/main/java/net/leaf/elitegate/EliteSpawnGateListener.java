package net.leaf.elitegate;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * Cancels spawns of configured elite entity types that occur in farm-like geometry.
 *
 * <p>Common-path cost is a single set lookup: any spawn whose type is not gated returns
 * immediately, so vanilla mobs and Tier-1 custom mobs are never inspected. Only the rare
 * gated elites run the validity predicate, and rejection happens before the entity is added
 * to the world (true pre-spawn cancellation).</p>
 */
public final class EliteSpawnGateListener implements Listener {

    private final EliteSpawnGatePlugin plugin;
    private final SpawnValidityPredicate predicate;

    public EliteSpawnGateListener(EliteSpawnGatePlugin plugin) {
        this.plugin = plugin;
        this.predicate = new SpawnValidityPredicate(plugin.gateConfig());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        GateConfig config = plugin.gateConfig();

        if (!config.isGated(event.getEntityType())) {
            return;
        }

        Location at = event.getLocation();
        if (at.getWorld() == null || !config.appliesToWorld(at.getWorld().getName())) {
            return;
        }

        String reason = predicate.rejectionReason(at);
        if (reason != null) {
            event.setCancelled(true);
            if (config.logRejections()) {
                plugin.getLogger().info("Rejected " + event.getEntityType()
                        + " at " + at.getWorld().getName()
                        + " " + at.getBlockX() + "," + at.getBlockY() + "," + at.getBlockZ()
                        + " (" + reason + ")");
            }
        }
    }
}
