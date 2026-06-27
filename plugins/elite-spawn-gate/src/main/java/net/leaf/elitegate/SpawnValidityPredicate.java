package net.leaf.elitegate;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * The v1 anti-farm predicate. Returns a reason string when a location should be REJECTED,
 * or {@code null} when the spawn is allowed.
 *
 * <p>The rules are deliberately simple and farm-hostile rather than exhaustive:
 * legitimate exploration encounters happen on natural ground in unmodified terrain, while
 * the overwhelming majority of mob farms violate one of these two checks.</p>
 */
public final class SpawnValidityPredicate {

    private final GateConfig config;

    public SpawnValidityPredicate(GateConfig config) {
        this.config = config;
    }

    /**
     * @return a human-readable rejection reason, or {@code null} if the spawn is valid.
     */
    public String rejectionReason(Location at) {
        if (config.requireNaturalGround()) {
            Block ground = at.getBlock().getRelative(0, -1, 0);
            if (!config.naturalGround().contains(ground.getType())) {
                return "non-natural ground (" + ground.getType() + ")";
            }
        }

        if (config.requireSkyAccess() && !isAtSurface(at)) {
            return "below terrain surface (underground)";
        }

        if (config.rejectArtificialNearby()) {
            int found = countArtificialNearby(at);
            if (found > config.maxArtificialBlocks()) {
                return found + " artificial block(s) within radius " + config.scanRadius();
            }
        }

        return null;
    }

    /**
     * A spawn is "at the surface" if its Y is no more than {@code surface-tolerance} blocks
     * below the terrain surface. The surface height is taken from the
     * {@link HeightMap#MOTION_BLOCKING_NO_LEAVES} heightmap, which reports the topmost solid
     * ground/log block while ignoring the leaf canopy - so a spawn on the ground under a tree
     * still counts as surface, while cave and other underground spawns (well below the
     * ground) are rejected. This is an O(1) heightmap lookup rather than a vertical scan.
     */
    private boolean isAtSurface(Location at) {
        World world = at.getWorld();
        int surfaceY = world.getHighestBlockYAt(at.getBlockX(), at.getBlockZ(),
                HeightMap.MOTION_BLOCKING_NO_LEAVES);
        return at.getBlockY() >= surfaceY - config.surfaceTolerance();
    }

    private int countArtificialNearby(Location at) {
        int r = config.scanRadius();
        if (r <= 0) {
            return 0;
        }
        Block origin = at.getBlock();
        int count = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    Material m = origin.getRelative(dx, dy, dz).getType();
                    if (config.artificialBlocks().contains(m)) {
                        count++;
                        if (count > config.maxArtificialBlocks()) {
                            return count;
                        }
                    }
                }
            }
        }
        return count;
    }
}
