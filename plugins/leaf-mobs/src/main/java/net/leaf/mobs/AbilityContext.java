package net.leaf.mobs;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;

/**
 * Everything an {@link Ability} needs to perform one cast, assembled fresh per cast by
 * {@link MobController}. Abilities stay stateless by reading all tuning from {@link #params}
 * (the {@code params:} sub-map of the ability's reference-table row) and acting on
 * {@link #self} / {@link #target}.
 *
 * <p>All fields are resolved on the caster's region thread, so the ability may use them
 * directly (Folia-safe).</p>
 */
public final class AbilityContext {

    private final LeafMobsPlugin plugin;
    private final LivingEntity self;
    private final LivingEntity target;
    private final ConfigurationSection params;

    AbilityContext(LeafMobsPlugin plugin, LivingEntity self, LivingEntity target,
                   ConfigurationSection params) {
        this.plugin = plugin;
        this.self = self;
        this.target = target;
        this.params = params;
    }

    /** Owning plugin (for scheduling/keys when an ability needs them). */
    public LeafMobsPlugin plugin() {
        return plugin;
    }

    /** The casting mob. Always non-null and valid for the duration of the cast. */
    public LivingEntity self() {
        return self;
    }

    /** The mob's current target (nearest player if the mob has no explicit target), or {@code null}. */
    public LivingEntity target() {
        return target;
    }

    /** The ability's {@code params:} section ({@code null} if the row had none). */
    public ConfigurationSection params() {
        return params;
    }

    /** Convenience reader with a default for a numeric param. */
    public double param(String key, double def) {
        return params == null ? def : params.getDouble(key, def);
    }

    /** Convenience reader with a default for an int param. */
    public int param(String key, int def) {
        return params == null ? def : params.getInt(key, def);
    }

    /** Convenience reader with a default for a String param. */
    public String param(String key, String def) {
        return params == null ? def : params.getString(key, def);
    }
}
