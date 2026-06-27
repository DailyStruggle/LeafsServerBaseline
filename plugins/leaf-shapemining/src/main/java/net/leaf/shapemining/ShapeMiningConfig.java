package net.leaf.shapemining;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Immutable snapshot of the plugin's tunables, loaded from {@code config.yml}.
 *
 * <p>Holds the direction-aware tunnel caps, the per-swing block cap, the
 * per-block durability cost, and a per-{@link ToolClass} filler/vein material
 * list. Every tool class follows the same format: a {@code filler} list that the
 * shape modes ({@code 1x2}/{@code 3x3}/tunnel) clear, and a {@code vein} list
 * that the {@code vein} mode flood-fills.</p>
 */
public final class ShapeMiningConfig {

    private final int maxHorizontal;
    private final int maxUp;
    private final int maxDown;
    private final int maxBlocksPerSwing;
    private final int durabilityPerBlock;
    private final int maxVein;
    private final Map<ToolClass, Set<Material>> filler;
    private final Map<ToolClass, Set<Material>> vein;

    private ShapeMiningConfig(int maxHorizontal, int maxUp, int maxDown,
                             int maxBlocksPerSwing, int durabilityPerBlock, int maxVein,
                             Map<ToolClass, Set<Material>> filler,
                             Map<ToolClass, Set<Material>> vein) {
        this.maxHorizontal = maxHorizontal;
        this.maxUp = maxUp;
        this.maxDown = maxDown;
        this.maxBlocksPerSwing = maxBlocksPerSwing;
        this.durabilityPerBlock = durabilityPerBlock;
        this.maxVein = maxVein;
        this.filler = filler;
        this.vein = vein;
    }

    public static ShapeMiningConfig load(FileConfiguration config, Logger logger) {
        int maxHorizontal = Math.max(1, config.getInt("tunnel.max-horizontal", 32));
        int maxUp = Math.max(1, config.getInt("tunnel.max-up", 8));
        int maxDown = Math.max(1, config.getInt("tunnel.max-down", 3));
        int maxBlocksPerSwing = Math.max(1, config.getInt("max-blocks-per-swing", 64));
        int durabilityPerBlock = Math.max(0, config.getInt("durability-per-block", 1));
        int maxVein = Math.max(1, config.getInt("vein.max-vein", 64));

        Map<ToolClass, Set<Material>> filler = new EnumMap<>(ToolClass.class);
        Map<ToolClass, Set<Material>> vein = new EnumMap<>(ToolClass.class);
        ConfigurationSection tools = config.getConfigurationSection("tools");
        for (ToolClass tc : ToolClass.values()) {
            ConfigurationSection section = tools == null ? null : tools.getConfigurationSection(tc.configKey());
            List<String> fillerNames = section == null ? List.of() : section.getStringList("filler");
            List<String> veinNames = section == null ? List.of() : section.getStringList("vein");
            filler.put(tc, parseMaterials(fillerNames, "tools." + tc.configKey() + ".filler", logger));
            vein.put(tc, parseMaterials(veinNames, "tools." + tc.configKey() + ".vein", logger));
        }

        return new ShapeMiningConfig(maxHorizontal, maxUp, maxDown,
                maxBlocksPerSwing, durabilityPerBlock, maxVein, filler, vein);
    }

    private static Set<Material> parseMaterials(List<String> names, String where, Logger logger) {
        Set<Material> out = EnumSet.noneOf(Material.class);
        for (String name : names) {
            Material material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
            if (material == null) {
                logger.warning("Unknown material in " + where + ", skipping: " + name);
                continue;
            }
            out.add(material);
        }
        return out;
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

    public int maxVein() {
        return maxVein;
    }

    /** Whether the given block is filler for the given tool class (cleared by the shape modes). */
    public boolean isFiller(ToolClass tool, Material material) {
        if (tool == null) {
            return false;
        }
        Set<Material> set = filler.get(tool);
        return set != null && set.contains(material);
    }

    /** Whether the given block is a vein material for the given tool class (cleared by vein mode). */
    public boolean isVein(ToolClass tool, Material material) {
        if (tool == null) {
            return false;
        }
        Set<Material> set = vein.get(tool);
        return set != null && set.contains(material);
    }
}
