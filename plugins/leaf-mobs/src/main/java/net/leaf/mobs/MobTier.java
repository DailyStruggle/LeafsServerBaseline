package net.leaf.mobs;

import net.kyori.adventure.bossbar.BossBar;

/**
 * Coarse classification of a managed mob, driving defaults and (in later phases)
 * lifecycle behaviour such as boss bars and persistence.
 *
 * <ul>
 *   <li>{@link #COMMON} - farmable flavour reskins; ordinary loot, may despawn.</li>
 *   <li>{@link #ELITE} - rarer mini-bosses with abilities.</li>
 *   <li>{@link #BOSS} - unique, gated encounters; persistent, boss bar, rare rewards.</li>
 * </ul>
 */
public enum MobTier {
    COMMON,
    ELITE,
    BOSS;

    public static MobTier fromConfig(String raw, MobTier fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return MobTier.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    /** Bosses and elites are kept loaded/persistent; common reskins follow vanilla rules. */
    public boolean isPersistentByDefault() {
        return this == BOSS || this == ELITE;
    }

    /**
     * Whether this tier gets a runtime {@link MobController} (boss bar + lifecycle tracking).
     * Common reskins are plain entities and need no controller.
     */
    public boolean usesController() {
        return this == BOSS || this == ELITE;
    }

    /** Whether this tier shows a boss bar to nearby players. */
    public boolean usesBossBar() {
        return this == BOSS || this == ELITE;
    }

    /** Boss-bar colour for this tier (red for bosses, yellow for elites). */
    public BossBar.Color barColor() {
        return this == BOSS ? BossBar.Color.RED : BossBar.Color.YELLOW;
    }
}
