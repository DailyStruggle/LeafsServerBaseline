package net.leaf.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Parses {@code config.yml} into the cost curve and the node catalog.
 *
 * <p>The cost to allocate the next point is {@code base + step * pointsSpent}
 * (linear; deliberately rising so each additional point demands more, echoing
 * D&amp;D 5e multiclass costs).</p>
 */
public final class SkillConfig {

    private final int costBase;
    private final int costStep;
    private final long pollIntervalTicks;
    private final Map<String, SkillNode> byKey = new LinkedHashMap<>();
    private final Map<Integer, SkillNode> byId = new LinkedHashMap<>();

    private SkillConfig(int costBase, int costStep, long pollIntervalTicks) {
        this.costBase = costBase;
        this.costStep = costStep;
        this.pollIntervalTicks = pollIntervalTicks;
    }

    public static SkillConfig load(FileConfiguration config, Logger logger) {
        int base = config.getInt("cost.base", 5);
        int step = config.getInt("cost.step", 3);
        long poll = Math.max(1L, config.getLong("poll-interval-ticks", 10L));
        SkillConfig result = new SkillConfig(base, step, poll);

        ConfigurationSection nodes = config.getConfigurationSection("nodes");
        if (nodes == null) {
            logger.warning("No 'nodes' section in config.yml; no skills will be selectable.");
            return result;
        }

        for (String key : nodes.getKeys(false)) {
            ConfigurationSection n = nodes.getConfigurationSection(key);
            if (n == null) {
                continue;
            }
            int id = n.getInt("id", -1);
            if (id <= 0) {
                logger.warning("Skill node '" + key + "' has no valid positive id; skipping.");
                continue;
            }
            List<String> requires = new ArrayList<>(n.getStringList("requires"));
            List<String> excludes = new ArrayList<>(n.getStringList("excludes"));
            String gate = n.getString("gate", null);

            String attrKey = null;
            double attrAmount = 0.0;
            ConfigurationSection attr = n.getConfigurationSection("attribute");
            if (attr != null) {
                attrKey = attr.getString("key", null);
                attrAmount = attr.getDouble("amount", 0.0);
            }

            String effectKey = null;
            int effectAmp = 0;
            ConfigurationSection effect = n.getConfigurationSection("effect");
            if (effect != null) {
                effectKey = effect.getString("key", null);
                effectAmp = effect.getInt("amplifier", 0);
            }

            SkillNode node = new SkillNode(key, id, requires, excludes, gate,
                    attrKey, attrAmount, effectKey, effectAmp);
            if (result.byId.containsKey(id)) {
                logger.warning("Duplicate skill node id " + id + " ('" + key + "'); skipping.");
                continue;
            }
            result.byKey.put(key, node);
            result.byId.put(id, node);
        }
        logger.info("Loaded " + result.byKey.size() + " skill nodes.");
        return result;
    }

    public int costFor(int pointsSpent) {
        return costBase + costStep * Math.max(0, pointsSpent);
    }

    public long pollIntervalTicks() {
        return pollIntervalTicks;
    }

    public SkillNode byId(int id) {
        return byId.get(id);
    }

    public SkillNode byKey(String key) {
        return byKey.get(key);
    }

    public Map<String, SkillNode> nodes() {
        return Collections.unmodifiableMap(byKey);
    }
}
