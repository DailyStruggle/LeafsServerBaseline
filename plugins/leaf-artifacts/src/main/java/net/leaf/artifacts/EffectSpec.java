package net.leaf.artifacts;

import org.bukkit.potion.PotionEffectType;

/**
 * Declarative description of an infinite ambient potion effect an artifact
 * grants while active.
 *
 * <p>{@code amplifier} is the zero-based effect level (0 = level I, 1 = level
 * II, ...). The reconciler adds this amplifier on top of any pre-existing effect
 * of the same type from another source - e.g. a beacon's Haste - the way
 * enchantment levels stack: a beacon's Haste II (amplifier 1) plus an artifact's
 * Haste II (amplifier 1) yields Haste III (amplifier 2). This is the generalized
 * mechanism that lets any artifact buff an existing effect without a bespoke
 * per-artifact workaround.</p>
 *
 * <p>Note: vanilla never sums two instances of the same effect; it keeps the
 * strongest contributing source. The reconciler works around that by briefly
 * removing its own effect to sample the external amplifier and then re-applying
 * at the sum, so the addition is genuine (not just a floor).</p>
 *
 * @param type      the potion effect to grant
 * @param amplifier zero-based effect level
 */
public record EffectSpec(PotionEffectType type, int amplifier) {

    /** An ambient level-I effect (amplifier 0). */
    public static EffectSpec of(PotionEffectType type) {
        return new EffectSpec(type, 0);
    }

    /** An ambient effect at the given 1-based level (level 1 = amplifier 0). */
    public static EffectSpec level(PotionEffectType type, int level) {
        return new EffectSpec(type, Math.max(1, level) - 1);
    }
}
