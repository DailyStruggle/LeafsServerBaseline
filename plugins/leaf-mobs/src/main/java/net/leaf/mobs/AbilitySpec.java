package net.leaf.mobs;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Immutable, data-driven description of one ability slot on a {@link MobDefinition},
 * parsed from an entry of the row's {@code abilities:} list. The {@code type} selects the
 * {@link Ability} implementation from {@link AbilityRegistry}; the gating fields
 * ({@code cooldown}, {@code chance}, health phase) are enforced by {@link MobController}
 * before the ability is cast; {@link #params()} carries ability-specific tuning.
 *
 * <p>Example row:</p>
 * <pre>
 * abilities:
 *   - type: fang_line
 *     cooldown_s: 6
 *     chance: 0.7
 *     min_health: 0.0
 *     max_health: 0.5      # only in the second phase (below half health)
 *     params: { length: 8, damage: 6.0 }
 * </pre>
 */
public record AbilitySpec(
        String type,
        long cooldownTicks,
        double chance,
        double minHealthFrac,
        double maxHealthFrac,
        ConfigurationSection params) {

    /**
     * Parses the {@code abilities:} list of a mob row. Unusable entries (missing/blank
     * {@code type}) are skipped with a warning so one bad row does not discard the mob.
     */
    public static List<AbilitySpec> parseList(String mobId, ConfigurationSection mobSection, Logger log) {
        List<AbilitySpec> out = new ArrayList<>();
        if (mobSection == null || !mobSection.isList("abilities")) {
            return out;
        }
        List<Map<?, ?>> rows = mobSection.getMapList("abilities");
        for (Map<?, ?> row : rows) {
            ConfigurationSection cfg = toSection(row);
            String type = cfg.getString("type");
            if (type == null || type.isBlank()) {
                log.warning("[leaf-mobs] mob '" + mobId + "': ability entry missing 'type'; skipped.");
                continue;
            }
            double cooldownSeconds = cfg.getDouble("cooldown_s", 5.0);
            long cooldownTicks = Math.max(1L, Math.round(cooldownSeconds * 20.0));
            double chance = clamp01(cfg.getDouble("chance", 1.0));
            double minHealth = clamp01(cfg.getDouble("min_health", 0.0));
            double maxHealth = clamp01(cfg.getDouble("max_health", 1.0));
            if (minHealth > maxHealth) {
                double tmp = minHealth;
                minHealth = maxHealth;
                maxHealth = tmp;
            }
            ConfigurationSection params = cfg.getConfigurationSection("params");
            out.add(new AbilitySpec(type.trim().toLowerCase(), cooldownTicks, chance,
                    minHealth, maxHealth, params));
        }
        return out;
    }

    /** Wraps a raw YAML map into a {@link ConfigurationSection} so nested params parse cleanly. */
    private static ConfigurationSection toSection(Map<?, ?> row) {
        MemoryConfiguration cfg = new MemoryConfiguration();
        for (Map.Entry<?, ?> e : row.entrySet()) {
            cfg.set(String.valueOf(e.getKey()), e.getValue());
        }
        return cfg;
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    /** True if the given current health fraction (0..1) is inside this ability's phase window. */
    public boolean phaseAllows(double healthFraction) {
        return healthFraction >= minHealthFrac && healthFraction <= maxHealthFrac;
    }
}
