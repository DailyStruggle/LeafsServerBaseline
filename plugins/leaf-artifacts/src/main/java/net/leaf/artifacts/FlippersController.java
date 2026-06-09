package net.leaf.artifacts;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Flippers (P3 movement): a swim-speed boost.
 *
 * <p>Vanilla has no "swim speed" attribute, so the effect is delivered with the
 * {@link PotionEffectType#DOLPHINS_GRACE} mob effect -- the same lever vanilla
 * uses to make swimming faster. While the wearer carries Flippers and is in
 * water, an ambient, hidden Dolphin's Grace (amplifier 0, the vanilla dolphin
 * boost) is refreshed each tick.</p>
 *
 * <p><b>Air-swim synergy:</b> Dolphin's Grace also speeds movement during the
 * Helium Flamingo air-swim, because that state is itself a (ghost-)water swim
 * state. The effect is therefore also applied while
 * {@link HeliumFlamingoController#isAirSwimming(Player)} is true, matching the
 * documented Flippers + Helium Flamingo combo.</p>
 *
 * <p>The effect is short-lived and merely refreshed while applicable, so it
 * lapses on its own once the wearer leaves the water / air-swim or unequips --
 * nothing persists, and a genuine Dolphin's Grace (e.g. from a real dolphin) is
 * never stripped.</p>
 *
 * <p>The loop runs on the player's own entity scheduler once per tick, so it is
 * Folia-safe (only ever touches the owning player on its region thread).</p>
 */
public final class FlippersController {

    /** Delay before a freshly-tracked player's loop begins. */
    private static final long INITIAL_DELAY_TICKS = 2L;
    /** Re-evaluated every tick so the boost engages/lapses promptly. */
    private static final long PERIOD_TICKS = 1L;

    /**
     * Duration (ticks) the Dolphin's Grace is granted for; only needs to outlast
     * the refresh period so it lapses quickly once conditions drop.
     */
    private static final int EFFECT_DURATION_TICKS = 40;
    /** Vanilla dolphin boost strength. */
    private static final int EFFECT_AMPLIFIER = 0;

    private final Plugin plugin;
    private final ArtifactEquipment equipment;
    private final HeliumFlamingoController heliumFlamingo;

    public FlippersController(Plugin plugin, ArtifactEquipment equipment,
                              HeliumFlamingoController heliumFlamingo) {
        this.plugin = plugin;
        this.equipment = equipment;
        this.heliumFlamingo = heliumFlamingo;
    }

    /** Begins the per-tick swim-speed loop for the given player on its entity scheduler. */
    public void start(Player player) {
        player.getScheduler().runAtFixedRate(
                plugin,
                task -> tick(player),
                null,
                INITIAL_DELAY_TICKS,
                PERIOD_TICKS);
    }

    /** No persistent state to revert; the short Dolphin's Grace lapses on its own. */
    public void clear(Player player) {
        // Intentionally a no-op: the granted Dolphin's Grace is short-lived and
        // refreshed only while applicable, so it expires shortly after the player
        // stops swimming / unequips. We deliberately do not strip it so a genuine
        // (non-Flippers) Dolphin's Grace is never removed.
    }

    private void tick(Player player) {
        if (!equipment.carries(player, ArtifactType.FLIPPERS)) {
            return;
        }
        if (player.isInWater() || heliumFlamingo.isAirSwimming(player)) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.DOLPHINS_GRACE,
                    EFFECT_DURATION_TICKS,
                    EFFECT_AMPLIFIER,
                    true,   // ambient
                    false,  // no particles
                    false)); // no HUD icon
        }
    }
}
