package net.leaf.artifacts;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Universal Attractor (P4 utility): an item magnet.
 *
 * <p>While the attractor is carried, nearby dropped items are pulled toward the
 * wearer each tick: any {@link Item} within {@link #PICKUP_RADIUS} blocks is
 * nudged with a velocity aimed at the player, scaled so it accelerates smoothly
 * instead of teleporting. Once an item is within {@link #MERGE_RADIUS} it is
 * left alone so the normal vanilla pickup takes over.</p>
 *
 * <p><b>Toggle:</b> the mod's attractor is toggleable so you can still drop loot
 * on purpose. Rather than add a separate interaction/keybind, the magnet is
 * suspended while the wearer is sneaking - hold shift to drop items normally,
 * release to vacuum them back up. This keeps the behaviour entirely within the
 * per-entity tick (no extra event wiring) and is intuitive in play.</p>
 *
 * <p>Items still respect a short post-drop pickup delay, so an item the player
 * just threw is not immediately yanked back until that delay elapses (the same
 * delay vanilla uses), preventing a fight with the sneak-to-drop control.</p>
 *
 * <p>The loop runs on the player's own entity scheduler, so it is Folia-safe:
 * {@link Player#getNearbyEntities} and the velocity nudges all execute on the
 * region thread that owns the player.</p>
 */
public final class AttractorController {

    /** Delay before a freshly-tracked player's loop begins. */
    private static final long INITIAL_DELAY_TICKS = 2L;
    /** Run a few times per second; the pull is gentle and need not be every tick. */
    private static final long PERIOD_TICKS = 4L;

    /** Items within this many blocks are attracted. */
    private static final double PICKUP_RADIUS = 8.0D;
    /** Inside this radius the item is left to be picked up normally. */
    private static final double MERGE_RADIUS = 1.0D;
    /** Per-pull speed (blocks/tick) applied toward the player. */
    private static final double PULL_SPEED = 0.6D;

    private final Plugin plugin;
    private final ArtifactEquipment equipment;

    public AttractorController(Plugin plugin, ArtifactEquipment equipment) {
        this.plugin = plugin;
        this.equipment = equipment;
    }

    /** Begins the per-tick magnet loop for the given player on its entity scheduler. */
    public void start(Player player) {
        player.getScheduler().runAtFixedRate(
                plugin,
                task -> tick(player),
                null,
                INITIAL_DELAY_TICKS,
                PERIOD_TICKS);
    }

    /** No persistent state to revert; the magnet simply stops nudging items. */
    public void clear(Player player) {
        // Intentionally a no-op: the controller only nudges item velocities while
        // the attractor is carried, so it stops affecting items the moment the
        // wearer unequips it (or leaves).
    }

    private void tick(Player player) {
        if (!equipment.carries(player, ArtifactType.UNIVERSAL_ATTRACTOR)) {
            return;
        }
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        // Sneaking suspends the magnet so the wearer can drop loot on purpose.
        if (player.isSneaking()) {
            return;
        }
        UUID owner = player.getUniqueId();
        Location target = player.getLocation().add(0.0D, 0.5D, 0.0D);
        for (org.bukkit.entity.Entity entity
                : player.getNearbyEntities(PICKUP_RADIUS, PICKUP_RADIUS, PICKUP_RADIUS)) {
            if (!(entity instanceof Item item)) {
                continue;
            }
            // Respect the post-drop pickup delay so freshly-thrown items are not
            // immediately yanked back (the same delay vanilla pickup honours).
            if (item.getPickupDelay() > 0) {
                continue;
            }
            // Leave items flagged to never be picked up (e.g. quest/keep items).
            if (item.getPickupDelay() == Short.MAX_VALUE) {
                continue;
            }
            // Do not steal items reserved for another player's pickup.
            if (item.getOwner() != null && !item.getOwner().equals(owner)) {
                continue;
            }
            Vector toPlayer = target.toVector().subtract(item.getLocation().toVector());
            if (toPlayer.lengthSquared() <= MERGE_RADIUS * MERGE_RADIUS) {
                continue;
            }
            item.setVelocity(toPlayer.normalize().multiply(PULL_SPEED));
        }
    }
}
