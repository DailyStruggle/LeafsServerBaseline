package net.leaf.shapemining;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-ish step 1-2 of the shape-mining pipeline: resolve the dig direction and
 * shape, then generate the ordered list of <em>additional</em> candidate blocks
 * (the originally-broken block is excluded). The list is nearest-first so the
 * caller can "stop at the first non-owned / unloaded block" (v1).
 *
 * <p>No world mutation happens here; only block-coordinate resolution (reads).
 * Allow-list filtering and region-ownership gating are the caller's job.</p>
 */
public final class ShapePlanner {

    private ShapePlanner() {
    }

    public static List<Block> plan(Player player, Block broken, MineShape shape, ShapeMiningConfig config) {
        List<Block> out = new ArrayList<>();
        if (shape == MineShape.OFF) {
            return out;
        }

        Vector dir = player.getEyeLocation().getDirection();
        BlockFace depth = dominantFace(dir);

        switch (shape) {
            case CORRIDOR -> planCorridor(player, broken, depth, out);
            case SQUARE -> planSquare(broken, depth, out);
            case TUNNEL -> planTunnel(broken, depth, config, out);
            default -> {
            }
        }
        return out;
    }

    /** 1x2: the broken block plus one more to clear player height. */
    private static void planCorridor(Player player, Block broken, BlockFace depth, List<Block> out) {
        if (depth == BlockFace.UP || depth == BlockFace.DOWN) {
            // Mining vertically: extend the "2" along the player's horizontal facing.
            out.add(broken.getRelative(player.getFacing()));
        } else {
            // Mining horizontally: clear the block above (head height).
            out.add(broken.getRelative(BlockFace.UP));
        }
    }

    /** 3x3: a single fixed cross-section slice perpendicular to the dig axis. */
    private static void planSquare(Block broken, BlockFace depth, List<Block> out) {
        int[] u;
        int[] v;
        if (depth == BlockFace.UP || depth == BlockFace.DOWN) {
            u = new int[] {1, 0, 0};
            v = new int[] {0, 0, 1};
        } else if (depth == BlockFace.EAST || depth == BlockFace.WEST) {
            u = new int[] {0, 1, 0};
            v = new int[] {0, 0, 1};
        } else { // NORTH / SOUTH
            u = new int[] {0, 1, 0};
            v = new int[] {1, 0, 0};
        }
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) {
                    continue; // skip the originally-broken block
                }
                int dx = u[0] * a + v[0] * b;
                int dy = u[1] * a + v[1] * b;
                int dz = u[2] * a + v[2] * b;
                out.add(broken.getRelative(dx, dy, dz));
            }
        }
    }

    /** 1x1xN: extend along the dig axis, with a direction-aware length cap. */
    private static void planTunnel(Block broken, BlockFace depth, ShapeMiningConfig config, List<Block> out) {
        int length = tunnelLength(depth, config);
        for (int i = 1; i <= length; i++) {
            out.add(broken.getRelative(depth, i));
        }
    }

    private static int tunnelLength(BlockFace depth, ShapeMiningConfig config) {
        return switch (depth) {
            case DOWN -> config.maxDown();
            case UP -> config.maxUp();
            default -> config.maxHorizontal();
        };
    }

    /** Snaps a look vector to the dominant axis-aligned {@link BlockFace}. */
    static BlockFace dominantFace(Vector dir) {
        double ax = Math.abs(dir.getX());
        double ay = Math.abs(dir.getY());
        double az = Math.abs(dir.getZ());
        if (ay >= ax && ay >= az) {
            return dir.getY() >= 0 ? BlockFace.UP : BlockFace.DOWN;
        }
        if (ax >= az) {
            return dir.getX() >= 0 ? BlockFace.EAST : BlockFace.WEST;
        }
        return dir.getZ() >= 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }
}
