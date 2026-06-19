package net.leaf.artifacts;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Umbrella (P5): conditional slow falling.
 *
 * <p>The Umbrella is a colored, unbreakable shield (a placeholder material until
 * a resource-pack umbrella model is added). Its whole effect is delivered with
 * the vanilla {@link PotionEffectType#SLOW_FALLING} mob effect: while the wearer
 * carries it and is airborne and descending (out of water, not gliding on an
 * elytra, and not flying), an ambient Slow Falling is refreshed each tick so
 * they drift down gently.</p>
 *
 * <p>Unlike a potion you choose to drink, the umbrella opens on <em>every</em>
 * fall -- even short hops and drops -- which is the deliberately mild "cost" of
 * carrying it: the float is a little annoying when you do not want it. There is
 * no durability cost (the item is unbreakable) and it occupies a hand curio
 * slot rather than a real off-hand.</p>
 *
 * <p>The effect is short-lived and merely refreshed while applicable, so it
 * lapses on its own once the wearer lands / enters water / unequips. We
 * deliberately do not strip it, so a genuine Slow Falling (e.g. from a potion)
 * is never removed.</p>
 *
 * <p>The loop runs on the player's own entity scheduler once per tick, so it is
 * Folia-safe (only ever touches the owning player on its region thread).</p>
 */
public final class UmbrellaController {

    /** Delay before a freshly-tracked player's loop begins. */
    private static final long INITIAL_DELAY_TICKS = 2L;
    /** Re-evaluated every tick so the float engages/lapses promptly. */
    private static final long PERIOD_TICKS = 1L;

    /**
     * Duration (ticks) the Slow Falling is granted for; only needs to outlast the
     * refresh period so it lapses quickly once the wearer is no longer falling.
     */
    private static final int EFFECT_DURATION_TICKS = 20;
    /** Vanilla slow-falling strength. */
    private static final int EFFECT_AMPLIFIER = 0;

    private final Plugin plugin;
    private final ArtifactEquipment equipment;

    public UmbrellaController(Plugin plugin, ArtifactEquipment equipment) {
        this.plugin = plugin;
        this.equipment = equipment;
    }

    /** Begins the per-tick slow-falling loop for the given player on its entity scheduler. */
    public void start(Player player) {
        player.getScheduler().runAtFixedRate(
                plugin,
                task -> tick(player),
                null,
                INITIAL_DELAY_TICKS,
                PERIOD_TICKS);
    }

    /** No persistent state to revert; the short Slow Falling lapses on its own. */
    public void clear(Player player) {
        // Intentionally a no-op: the granted Slow Falling is short-lived and
        // refreshed only while the wearer is falling, so it expires shortly after
        // they land / unequip. We deliberately do not strip it so a genuine
        // (non-Umbrella) Slow Falling is never removed.
    }

    private void tick(Player player) {
        if (!equipment.carries(player, ArtifactType.UMBRELLA)) {
            return;
        }
        if (player.getGameMode() == GameMode.SPECTATOR || player.isFlying()) {
            return;
        }
        if (player.isOnGround() || player.isGliding() || player.isInWater()) {
            return;
        }
        // Only while actually descending, so jumps rise normally before the
        // umbrella eases the way back down.
        if (player.getVelocity().getY() >= 0.0D) {
            return;
        }
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW_FALLING,
                EFFECT_DURATION_TICKS,
                EFFECT_AMPLIFIER,
                true,   // ambient
                false,  // no particles
                false)); // no HUD icon
    }
}
