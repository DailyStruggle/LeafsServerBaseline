package net.leaf.shapemining;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Immutable snapshot of the plugin's tunables, loaded from {@code config.yml}.
 *
 * <p>Holds the direction-aware tunnel caps, the per-swing block cap, the
 * per-block durability cost, and the bulk-filler allow-list.</p>
 */
public final class ShapeMiningConfig {

    private final int maxHorizontal;
    private final int maxUp;
    private final int maxDown;
    private final int maxBlocksPerSwing;
    private final int durabilityPerBlock;
    private final Set<Material> allowList;

    private ShapeMiningConfig(int maxHorizontal, int maxUp, int maxDown,
                             int maxBlocksPerSwing, int durabilityPerBlock,
                             Set<Material> allowList) {
        this.maxHorizontal = maxHorizontal;
        this.maxUp = maxUp;
        this.maxDown = maxDown;
        this.maxBlocksPerSwing = maxBlocksPerSwing;
        this.durabilityPerBlock = durabilityPerBlock;
        this.allowList = allowList;
    }

    public static ShapeMiningConfig load(FileConfiguration config, Logger logger) {
        int maxHorizontal = Math.max(1, config.getInt("tunnel.max-horizontal", 32));
        int maxUp = Math.max(1, config.getInt("tunnel.max-up", 8));
        int maxDown = Math.max(1, config.getInt("tunnel.max-down", 3));
        int maxBlocksPerSwing = Math.max(1, config.getInt("max-blocks-per-swing", 64));
        int durabilityPerBlock = Math.max(0, config.getInt("durability-per-block", 1));

        Set<Material> allow = EnumSet.noneOf(Material.class);
        List<String> names = config.getStringList("allow-list");
        for (String name : names) {
            Material material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
            if (material == null) {
                logger.warning("Unknown material in allow-list, skipping: " + name);
                continue;
            }
            allow.add(material);
        }
        if (allow.isEmpty()) {
            logger.warning("allow-list is empty; shape-mining will not break any blocks.");
        }

        return new ShapeMiningConfig(maxHorizontal, maxUp, maxDown,
                maxBlocksPerSwing, durabilityPerBlock, allow);
    }

    public int maxHorizontal() {
        return maxHorizontal;
    }

    public int maxUp() {
        return maxUp;
    }

    public int maxDown() {
        return maxDown;
    }

    public int maxBlocksPerSwing() {
        return maxBlocksPerSwing;
    }

    public int durabilityPerBlock() {
        return durabilityPerBlock;
    }

    public boolean isFiller(Material material) {
        return allowList.contains(material);
    }
}
