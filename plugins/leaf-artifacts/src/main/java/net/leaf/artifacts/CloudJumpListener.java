package net.leaf.artifacts;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cloud in a Bottle (P3 signature movement): a mid-air double jump.
 *
 * <p>Implemented with the standard "allow-flight" double-jump trick: while the
 * artifact is equipped, flight is kept enabled so that pressing the jump key in
 * mid-air makes the client fire a {@link PlayerToggleFlightEvent}. That event is
 * cancelled (the player never actually flies) and converted into a velocity
 * impulse. One air-jump charge is granted per airtime and refreshed on landing;
 * jumping while standing on the ground does nothing special.</p>
 *
 * <p>The impulse is always vertical (Cloud in a Bottle is a lift artifact) and
 * scales with the player's {@link Attribute#JUMP_STRENGTH}, so it combines with
 * Bunny Hoppers. The travel synergy with an elytra is emergent: gain altitude
 * with the air-jump, then glide it out into distance.</p>
 */
public final class CloudJumpListener implements Listener {

    /** Vanilla baseline jump strength; used to normalise the scale factor. */
    private static final double BASE_JUMP_STRENGTH = 0.42D;
    /** Upward impulse (blocks/tick) at baseline jump strength. */
    private static final double VERTICAL_IMPULSE = 0.55D;
    /** Cap on the resulting upward speed at baseline jump strength. */
    private static final double MAX_VERTICAL = 0.95D;
    /** Forward nudge along the current x/z movement direction (does not touch Y). */
    private static final double HORIZONTAL_IMPULSE = 0.35D;

    private final Plugin plugin;
    private final ArtifactEquipment equipment;

    /** Players who still have their air-jump charge for the current airtime. */
    private final Set<UUID> airJumpReady = ConcurrentHashMap.newKeySet();
    /** Players to whom we have temporarily granted flight (and must revoke). */
    private final Set<UUID> flightGranted = ConcurrentHashMap.newKeySet();

    public CloudJumpListener(Plugin plugin, ArtifactEquipment equipment) {
        this.plugin = plugin;
        this.equipment = equipment;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        if (!equipment.carries(player, ArtifactType.CLOUD_IN_A_BOTTLE) || !isManaged(player)) {
            revokeGrantedFlight(player);
            airJumpReady.remove(id);
            return;
        }

        // Refresh the air-jump charge whenever the player is on the ground
        // (used by both the mid-air double jump and the elytra-launch boost).
        if (player.isOnGround()) {
            airJumpReady.add(id);
        }

        // While an elytra is in play, yield flight so the jump key can deploy /
        // maintain the glide; the boost is delivered via the glide-start event
        // instead. Otherwise keep flight enabled so the jump key fires the
        // toggle event for the mid-air double jump.
        if (player.isGliding() || hasElytra(player)) {
            revokeGrantedFlight(player);
        } else if (!player.getAllowFlight()) {
            player.setAllowFlight(true);
            flightGranted.add(id);
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        if (!event.isFlying()) {
            return;
        }
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (!equipment.carries(player, ArtifactType.CLOUD_IN_A_BOTTLE)
                || !isManaged(player)
                || player.isGliding()
                || hasElytra(player)) {
            return;
        }

        // We never let the player actually start flying.
        event.setCancelled(true);
        player.setFlying(false);

        // Only an in-air press with an unspent charge performs the air-jump.
        if (player.isOnGround() || !airJumpReady.remove(id)) {
            return;
        }
        applyImpulse(player);
    }

    /** Deploying an elytra spends the cloud charge as a launch boost. */
    @EventHandler
    public void onStartGlide(EntityToggleGlideEvent event) {
        if (!event.isGliding() || !(event.getEntity() instanceof Player player)) {
            return;
        }
        UUID id = player.getUniqueId();
        if (!equipment.carries(player, ArtifactType.CLOUD_IN_A_BOTTLE)
                || !isManaged(player)
                || !airJumpReady.remove(id)) {
            return;
        }
        // Apply next tick so the boost isn't swallowed by the glide state change.
        player.getScheduler().run(plugin, task -> applyImpulse(player), null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        airJumpReady.remove(id);
        flightGranted.remove(id);
    }

    private void applyImpulse(Player player) {
        AttributeInstance js = player.getAttribute(Attribute.JUMP_STRENGTH);
        double scale = (js != null ? js.getValue() : BASE_JUMP_STRENGTH) / BASE_JUMP_STRENGTH;

        Vector velocity = player.getVelocity();
        double newY = Math.min(MAX_VERTICAL * scale, Math.max(velocity.getY(), 0.0D) + VERTICAL_IMPULSE * scale);
        velocity.setY(newY);

        // Add a forward nudge along the current horizontal movement direction so
        // the boost carries momentum, without altering the vertical lift.
        Vector horizontal = new Vector(velocity.getX(), 0.0D, velocity.getZ());
        if (horizontal.lengthSquared() > 1.0E-6D) {
            Vector forward = horizontal.normalize().multiply(HORIZONTAL_IMPULSE * scale);
            velocity.setX(velocity.getX() + forward.getX());
            velocity.setZ(velocity.getZ() + forward.getZ());
        }

        player.setVelocity(velocity);

        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 12, 0.3D, 0.1D, 0.3D, 0.02D);
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 0.7F, 1.2F);
    }

    private void revokeGrantedFlight(Player player) {
        if (flightGranted.remove(player.getUniqueId()) && !player.isFlying() && isManaged(player)) {
            player.setAllowFlight(false);
        }
    }

    /** Only manage flight for game modes that don't normally fly. */
    private static boolean isManaged(Player player) {
        GameMode mode = player.getGameMode();
        return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
    }

    /** Whether the player is wearing an elytra in the chest slot. */
    private static boolean hasElytra(Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        return chest != null && chest.getType() == Material.ELYTRA;
    }
}
