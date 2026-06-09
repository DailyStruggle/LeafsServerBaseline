package net.leaf.artifacts;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player, client-only "ghost ice" projector that gives Roller Skates an
 * authentic, client-predicted ice slide without ever touching the player's
 * velocity.
 *
 * <p>Each tick, while the wearer is sliding (gated by the caller on
 * sprint-speed, on-ground and out-of-water), this paints a <b>rounded cone</b>
 * of {@link Material#PACKED_ICE PACKED_ICE} onto the floor blocks ahead of the
 * wearer's <em>motion</em> direction (explicitly NOT their look direction),
 * sized by speed. The client then runs vanilla ice friction over those blocks
 * itself -- so the slide is genuinely client-side, eliminating the whole class
 * of {@code setVelocity} "fling" bugs.</p>
 *
 * <p>The footprint is the union of an <b>apex disc</b> around the feet (so the
 * supporting block is always iced even mid-turn, keeping the slide steerable)
 * and a <b>forward sector</b> bounded by a radius (the rounded cap). Both the
 * reach ({@code L}) and the half-angle ({@code theta}) scale with speed: faster
 * means a longer, narrower (more committed) cone. Below sprint speed the caller
 * does not invoke {@link #update}; if it ever did, a sub-threshold speed yields
 * an <b>empty</b> footprint (zero ice cells).</p>
 *
 * <p>Only existing solid walkable floor blocks are reskinned -- we never add a
 * block where the server sees air -- so client/server collision shapes stay in
 * agreement (no rubber-banding). All edits (new paints and the reverts of cells
 * that left the footprint) are flushed in a single batched
 * {@link Player#sendBlockChanges(java.util.Collection)} call per tick, which
 * Paper groups into per-section chunk packets. Everything only ever touches the
 * owning player, so it is Folia-safe.</p>
 */
public final class GhostIceProjector {

    /** The slippery material projected under the wearer. (BLUE_ICE is the most slippery vanilla surface, 0.989.) */
    private static final Material ICE_MATERIAL = Material.BLUE_ICE;

    /** Speed (b/t) at/above which the slide engages; below this the footprint is empty. */
    private static final double SLIDE_MIN_SPEED = 0.25D;
    /** Speed (b/t) at which the cone reaches its full reach / narrowest angle. */
    private static final double SLIDE_REF_SPEED = 0.55D;

    /** Radius (blocks) of the always-on apex disc around the feet. */
    private static final double APEX_RADIUS = 2.0D;
    /** Cone reach (blocks) at the sprint threshold. */
    private static final double REACH_MIN = 3.0D;
    /** Cone reach (blocks) at/above {@link #SLIDE_REF_SPEED}. */
    private static final double REACH_MAX = 7.5D;
    /** Cone half-angle (radians) at the sprint threshold (~45deg: easy to turn). */
    private static final double THETA_MAX = Math.toRadians(45.0D);
    /** Cone half-angle (radians) at full speed (~25deg: committed/straight). */
    private static final double THETA_MIN = Math.toRadians(25.0D);
    /** Lead the cone apex this many ticks along the motion vector to hide latency. */
    private static final double LEAD_TICKS = 1.0D;
    /** Safety cap on cells painted per tick per player. */
    private static final int MAX_CELLS = 160;
    /**
     * How many blocks above the block beneath the feet to also probe for an
     * iceable walkable surface. The wearer may be standing on a block 1-2 higher
     * than {@code floorY} (terrain step-ups, higher step height) or briefly above
     * it (jump boost), so we ice every solid walkable surface in this short
     * vertical band within the cone -- not just the single block below the feet.
     */
    private static final int FLOOR_PROBE_UP = 2;
    /**
     * How many ticks a cell keeps its ghost ice after it has left the footprint.
     * The ice lingers (rather than reverting the instant the cone passes) so the
     * surface persists longer and, because cells are not toggled on/off every
     * tick, the slide looks far less "flashy".
     */
    private static final int LINGER_TICKS = 30;

    /**
     * Floor cells currently shown as ice per player, mapped to the number of
     * ticks of ghost ice they have left before reverting (their linger countdown).
     */
    private final Map<UUID, Map<Long, Integer>> shown = new ConcurrentHashMap<>();

    /**
     * Recomputes and flushes the ghost-ice footprint for the player from their
     * current motion. {@code (dx, dz)} is the horizontal displacement this tick
     * (the motion vector, not the look vector); {@code speed} is its magnitude.
     */
    public void update(Player player, double dx, double dz, double speed) {
        Set<Long> target = computeFootprint(player, dx, dz, speed);
        flush(player, target);
    }

    /**
     * Lets the currently-shown ghost ice linger and age out gracefully without
     * painting any new cells. Use this on a <em>transient</em> gate miss (a
     * momentary speed dip, a single tick off the ground) so the slide surface
     * is not hard-reverted and immediately repainted -- the full revert+repaint
     * is what reads as a "flash". Cells still revert once their linger countdown
     * expires.
     */
    public void fade(Player player) {
        if (shown.containsKey(player.getUniqueId())) {
            flush(player, java.util.Collections.emptySet());
        }
    }

    /** Reverts every ghost-ice cell shown to this player and forgets their state. */
    public void clear(Player player) {
        Map<Long, Integer> current = shown.remove(player.getUniqueId());
        if (current == null || current.isEmpty()) {
            return;
        }
        World world = player.getWorld();
        List<BlockState> reverts = new ArrayList<>(current.size());
        for (long key : current.keySet()) {
            addRealState(world, key, reverts);
        }
        if (!reverts.isEmpty()) {
            player.sendBlockChanges(reverts);
        }
    }

    /** Builds the set of floor block keys that should currently appear as ice. */
    private Set<Long> computeFootprint(Player player, double dx, double dz, double speed) {
        Set<Long> target = new HashSet<>();
        if (speed < SLIDE_MIN_SPEED) {
            return target; // empty -> zero ice cells below sprint speed
        }

        double dirX = dx / speed;
        double dirZ = dz / speed;

        double t = clamp01((speed - SLIDE_MIN_SPEED) / (SLIDE_REF_SPEED - SLIDE_MIN_SPEED));
        double reach = REACH_MIN + t * (REACH_MAX - REACH_MIN);
        double cosHalfAngle = Math.cos(THETA_MAX - t * (THETA_MAX - THETA_MIN));

        double px = player.getLocation().getX();
        double pz = player.getLocation().getZ();
        // Apex is led one tick ahead along the motion vector (LEAD_TICKS * v).
        double apexX = px + dirX * speed * LEAD_TICKS;
        double apexZ = pz + dirZ * speed * LEAD_TICKS;

        World world = player.getWorld();
        int floorY = player.getLocation().getBlockY() - 1;
        int span = (int) Math.ceil(Math.max(reach, APEX_RADIUS)) + 1;
        int baseX = (int) Math.floor(apexX);
        int baseZ = (int) Math.floor(apexZ);

        for (int ox = -span; ox <= span; ox++) {
            for (int oz = -span; oz <= span; oz++) {
                int cx = baseX + ox;
                int cz = baseZ + oz;
                double centerX = cx + 0.5D;
                double centerZ = cz + 0.5D;

                if (!inFootprint(centerX, centerZ, px, pz, apexX, apexZ, dirX, dirZ, reach, cosHalfAngle)) {
                    continue;
                }
                // Probe the block below the feet plus a short band above it, so a
                // surface raised by step-up terrain / higher step height / a jump
                // is iced too (not just floorY).
                for (int dy = 0; dy <= FLOOR_PROBE_UP; dy++) {
                    int cy = floorY + dy;
                    if (isIceableFloor(world, cx, cy, cz)) {
                        target.add(blockKey(cx, cy, cz));
                        if (target.size() >= MAX_CELLS) {
                            return target;
                        }
                    }
                }
            }
        }
        return target;
    }

    /**
     * True if the cell centre lies in the apex disc (around the feet) or in the
     * forward sector (within {@code reach} of the led apex and within the
     * half-angle of the motion direction).
     */
    private static boolean inFootprint(double centerX, double centerZ,
                                       double px, double pz,
                                       double apexX, double apexZ,
                                       double dirX, double dirZ,
                                       double reach, double cosHalfAngle) {
        double dpx = centerX - px;
        double dpz = centerZ - pz;
        if (dpx * dpx + dpz * dpz <= APEX_RADIUS * APEX_RADIUS) {
            return true; // apex disc around the feet
        }
        double ax = centerX - apexX;
        double az = centerZ - apexZ;
        double dist = Math.sqrt(ax * ax + az * az);
        if (dist > reach || dist < 1.0E-6D) {
            return false;
        }
        double dot = (ax * dirX + az * dirZ) / dist; // cos(angle to motion dir)
        return dot >= cosHalfAngle;
    }

    /**
     * Only reskin a block the client already treats as solid walkable ground
     * (and whose chunk is loaded), so collision shapes stay in sync and no ghost
     * ice ever floats in air.
     */
    private static boolean isIceableFloor(World world, int x, int y, int z) {
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
            return false;
        }
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return false;
        }
        Block floor = world.getBlockAt(x, y, z);
        if (!floor.getType().isSolid() || floor.getType() == ICE_MATERIAL) {
            return false;
        }
        Block above = world.getBlockAt(x, y + 1, z);
        return above.isPassable();
    }

    /**
     * Reconciles the new footprint against what is shown. Cells in the footprint
     * are (re)painted and have their linger countdown refreshed; cells no longer
     * in the footprint keep their ice and only revert once their countdown runs
     * out. Refreshing instead of immediately reverting is what makes the ice both
     * last longer and flicker much less. All paints and the (now occasional)
     * reverts are flushed in one batched packet.
     */
    private void flush(Player player, Set<Long> target) {
        UUID id = player.getUniqueId();
        Map<Long, Integer> current = shown.computeIfAbsent(id, k -> new HashMap<>());
        World world = player.getWorld();

        List<BlockState> changes = new ArrayList<>();
        BlockData iceData = ICE_MATERIAL.createBlockData();

        // Paints: cells in the footprint that are not already iced. Either way,
        // refresh their linger countdown so they stay iced while in/near the cone.
        for (long key : target) {
            if (current.put(key, LINGER_TICKS) == null) {
                BlockState state = stateAt(world, key);
                state.setBlockData(iceData);
                changes.add(state);
            }
        }
        // Age out cells no longer in the footprint; revert only when fully expired.
        for (var it = current.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Long, Integer> entry = it.next();
            if (target.contains(entry.getKey())) {
                continue;
            }
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                addRealState(world, entry.getKey(), changes);
                it.remove();
            } else {
                entry.setValue(remaining);
            }
        }

        if (!changes.isEmpty()) {
            player.sendBlockChanges(changes);
        }

        if (current.isEmpty()) {
            shown.remove(id);
        }
    }

    private static void addRealState(World world, long key, List<BlockState> out) {
        out.add(stateAt(world, key));
    }

    private static BlockState stateAt(World world, long key) {
        int x = unpackX(key);
        int y = unpackY(key);
        int z = unpackZ(key);
        return world.getBlockAt(x, y, z).getState();
    }

    private static double clamp01(double v) {
        return v < 0.0D ? 0.0D : Math.min(v, 1.0D);
    }

    // --- block-position packing (matches the vanilla 26/12/26-bit layout) ---

    private static long blockKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private static int unpackX(long key) {
        return signExtend26((int) (key >> 38));
    }

    private static int unpackZ(long key) {
        return signExtend26((int) (key >> 12));
    }

    private static int unpackY(long key) {
        int y = (int) (key & 0xFFF);
        return y >= 0x800 ? y - 0x1000 : y;
    }

    private static int signExtend26(int v) {
        v &= 0x3FFFFFF;
        return v >= 0x2000000 ? v - 0x4000000 : v;
    }
}
