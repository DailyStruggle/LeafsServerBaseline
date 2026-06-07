package net.leaf.elitegate;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Immutable snapshot of the gate configuration, parsed once on enable.
 */
public final class GateConfig {

    private final Set<String> worlds;
    private final Set<EntityType> gatedTypes;
    private final boolean requireNaturalGround;
    private final Set<Material> naturalGround;
    private final boolean rejectArtificialNearby;
    private final int scanRadius;
    private final int maxArtificialBlocks;
    private final Set<Material> artificialBlocks;
    private final boolean logRejections;

    private GateConfig(Set<String> worlds, Set<EntityType> gatedTypes, boolean requireNaturalGround,
                       Set<Material> naturalGround, boolean rejectArtificialNearby, int scanRadius,
                       int maxArtificialBlocks, Set<Material> artificialBlocks, boolean logRejections) {
        this.worlds = worlds;
        this.gatedTypes = gatedTypes;
        this.requireNaturalGround = requireNaturalGround;
        this.naturalGround = naturalGround;
        this.rejectArtificialNearby = rejectArtificialNearby;
        this.scanRadius = scanRadius;
        this.maxArtificialBlocks = maxArtificialBlocks;
        this.artificialBlocks = artificialBlocks;
        this.logRejections = logRejections;
    }

    public static GateConfig from(FileConfiguration c) {
        Set<String> worlds = new HashSet<>(c.getStringList("worlds"));

        Set<EntityType> gated = EnumSet.noneOf(EntityType.class);
        for (String s : c.getStringList("gated-types")) {
            EntityType t = parseEntityType(s);
            if (t != null) {
                gated.add(t);
            }
        }

        return new GateConfig(
                worlds,
                gated,
                c.getBoolean("require-natural-ground", true),
                materials(c.getStringList("natural-ground")),
                c.getBoolean("reject-artificial-nearby", true),
                Math.max(0, c.getInt("scan-radius", 4)),
                Math.max(0, c.getInt("max-artificial-blocks", 0)),
                materials(c.getStringList("artificial-blocks")),
                c.getBoolean("log-rejections", true)
        );
    }

    private static Set<Material> materials(Iterable<String> names) {
        Set<Material> set = EnumSet.noneOf(Material.class);
        for (String n : names) {
            Material m = Material.matchMaterial(n);
            if (m != null) {
                set.add(m);
            }
        }
        return set;
    }

    private static EntityType parseEntityType(String s) {
        try {
            return EntityType.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean appliesToWorld(String worldName) {
        return worlds.isEmpty() || worlds.contains(worldName);
    }

    public boolean isGated(EntityType type) {
        return gatedTypes.contains(type);
    }

    public Set<EntityType> gatedTypes() {
        return gatedTypes;
    }

    public boolean requireNaturalGround() {
        return requireNaturalGround;
    }

    public Set<Material> naturalGround() {
        return naturalGround;
    }

    public boolean rejectArtificialNearby() {
        return rejectArtificialNearby;
    }

    public int scanRadius() {
        return scanRadius;
    }

    public int maxArtificialBlocks() {
        return maxArtificialBlocks;
    }

    public Set<Material> artificialBlocks() {
        return artificialBlocks;
    }

    public boolean logRejections() {
        return logRejections;
    }
}
