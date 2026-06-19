package net.leaf.artifacts;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

/**
 * Charm of Sinking (P4 utility): walk on the underwater floor.
 *
 * <p>Vanilla has no "negate buoyancy" mob effect, so the charm is delivered by
 * gently pushing the wearer down while they are submerged: each tick, if their
 * vertical velocity is above a small sink target, it is nudged toward that
 * target. This cancels the upward bob of water so the player settles onto the
 * bottom and can walk along it normally, exactly like the mod's accessory.</p>
 *
 * <p>The push is mild and only applied while in water and off the ground, so the
 * wearer can still swim upward deliberately (it merely takes a little effort) and
 * normal walking on the floor is unaffected. Nothing persists once the wearer
 * leaves the water or unequips.</p>
 *
 * <p>The loop runs on the player's own entity scheduler once per tick, so it is
 * Folia-safe (only ever touches the owning player on its region thread).</p>
 */
public final class SinkingController {

    /** Delay before a freshly-tracked player's loop begins. */
    private static final long INITIAL_DELAY_TICKS = 2L;
    /** Re-evaluated every tick so sinking engages/lapses promptly. */
    private static final long PERIOD_TICKS = 1L;

    /**
     * Target downward velocity (blocks/tick) while submerged. Slightly faster
     * than vanilla's water descent so the wearer settles to the floor instead of
     * bobbing, but slow enough to feel like a calm sink rather than a yank.
     */
    private static final double SINK_VELOCITY = -0.10D;

    private final Plugin plugin;
    private final ArtifactEquipment equipment;

    public SinkingController(Plugin plugin, ArtifactEquipment equipment) {
        this.plugin = plugin;
        this.equipment = equipment;
    }

    /** Begins the per-tick sinking loop for the given player on its entity scheduler. */
    public void start(Player player) {
        player.getScheduler().runAtFixedRate(
                plugin,
                task -> tick(player),
                null,
                INITIAL_DELAY_TICKS,
                PERIOD_TICKS);
    }

    /** No persistent state to revert; the per-tick velocity nudge simply stops. */
    public void clear(Player player) {
        // Intentionally a no-op: the controller only nudges velocity while the
        // wearer is submerged, so it stops affecting the player the moment they
        // leave the water or unequip.
    }

    private void tick(Player player) {
        if (!equipment.carries(player, ArtifactType.CHARM_OF_SINKING)) {
            return;
        }
        if (player.getGameMode() == GameMode.SPECTATOR || player.isFlying()) {
            return;
        }
        // Only act while genuinely submerged and not already standing on the
        // floor, so it cancels buoyancy without fighting normal floor-walking.
        if (!player.isInWater() || player.isOnGround()) {
            return;
        }
        Vector velocity = player.getVelocity();
        if (velocity.getY() > SINK_VELOCITY) {
            velocity.setY(SINK_VELOCITY);
            player.setVelocity(velocity);
        }
    }
}
