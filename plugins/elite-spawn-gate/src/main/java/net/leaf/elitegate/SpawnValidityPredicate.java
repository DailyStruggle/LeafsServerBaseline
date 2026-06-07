package net.leaf.elitegate;

import org.bukkit.Location;
import org.bukkit.Material;
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

        if (config.rejectArtificialNearby()) {
            int found = countArtificialNearby(at);
            if (found > config.maxArtificialBlocks()) {
                return found + " artificial block(s) within radius " + config.scanRadius();
            }
        }

        return null;
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
