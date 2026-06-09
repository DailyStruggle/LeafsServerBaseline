package net.leaf.artifacts;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player, client-only "ghost water" projector that lets Helium Flamingo give
 * the wearer an authentic, client-predicted swim through the air without ever
 * touching the player's velocity or fighting client pose prediction.
 *
 * <p>The swim pose and swim physics are derived by the owning client from
 * <em>water contact</em>: it re-evaluates "am I in water?" from the blocks
 * intersecting its bounding box every tick. Forcing the swim flag server-side
 * therefore never latched (the wearer's own client cleared it in the air). This
 * projector instead makes the client <em>believe</em> it is submerged by
 * sending client-only water block-changes into the air cells around (and ahead
 * of) the wearer's body. The client then runs real swim physics and the swim
 * pose itself -- exactly the rule that previously fought us, now turned in our
 * favour.</p>
 *
 * <p>This is the air analogue of {@link GhostIceProjector}, but fully
 * <b>three-dimensional</b> and oriented along the wearer's <em>look</em>
 * direction (unlike Roller Skates' ice, which follows the horizontal motion
 * vector). Swimming moves you where you point -- including up and down -- so the
 * water must fill the cells you are about to swim into vertically as well as
 * horizontally, or the client briefly loses water contact and snaps you back to
 * the standing pose (the "flash"/kick-out). The footprint is therefore a solid
 * <b>sphere</b> of water around the body plus a <b>rounded 3D cone</b> of water
 * ahead of the look vector, sized by speed. Because keeping the swim pose latched
 * needs a wider, deeper wet volume than ice, this projects considerably more
 * cells than {@link GhostIceProjector}.</p>
 *
 * <p>Only air cells are converted to water -- never solid blocks -- so collision
 * shapes stay in sync (water is non-solid, so there is no rubber-band risk). All
 * edits (new paints and the reverts of cells that left the footprint) are
 * flushed in a single batched
 * {@link Player#sendBlockChanges(java.util.Collection)} call per tick, which
 * Paper groups into per-section chunk packets. Everything only ever touches the
 * owning player, so it is Folia-safe.</p>
 */
public final class GhostWaterProjector {

    /** The fluid material projected around the wearer. */
    private static final Material WATER_MATERIAL = Material.WATER;

    /** Speed (b/t) at which the cone reaches its full reach / narrowest angle. */
    private static final double SWIM_REF_SPEED = 0.55D;

    /** Radius (blocks) of the always-on sphere of water around the body. */
    private static final double BODY_RADIUS = 2.5D;
    /** Cone reach (blocks) when nearly stationary in the air. */
    private static final double REACH_MIN = 3.0D;
    /** Cone reach (blocks) at/above {@link #SWIM_REF_SPEED}. */
    private static final double REACH_MAX = 9.0D;
    /** Cone half-angle (radians) at low speed (~55deg: wide so look-turns stay wet). */
    private static final double THETA_MAX = Math.toRadians(55.0D);
    /** Cone half-angle (radians) at full speed (~35deg: committed/straight). */
    private static final double THETA_MIN = Math.toRadians(35.0D);
    /** Lead the cone apex this many ticks along the look vector to hide latency. */
    private static final double LEAD_TICKS = 1.0D;
    /** Safety cap on cells painted per tick per player. (3D volume -> much larger than ice.) */
    private static final int MAX_CELLS = 600;
    /**
     * How many ticks a cell keeps its ghost water after it has left the
     * footprint. The water lingers (rather than reverting the instant the cone
     * passes) so the swim envelope persists and, because cells are not toggled
     * on/off every tick, the effect looks far less "flashy". Longer than the ice
     * linger because losing water contact for even one tick kicks the client out
     * of the swim pose.
     */
    private static final int LINGER_TICKS = 40;

    /**
     * Air cells currently shown as water per player, mapped to the number of
     * ticks of ghost water they have left before reverting (their linger
     * countdown).
     */
    private final Map<UUID, Map<Long, Integer>> shown = new ConcurrentHashMap<>();

    /**
     * Recomputes and flushes the ghost-water footprint for the player. The cone
     * is oriented along the wearer's <em>look</em> direction (swimming follows
     * where you point, including vertically); {@code speed} is the 3D movement
     * speed (blocks/tick) and only scales the cone's reach/angle.
     */
    public void update(Player player, double speed) {
        Set<Long> target = computeFootprint(player, speed);
        flush(player, target);
    }

    /**
     * Lets the currently-shown ghost water linger and age out gracefully without
     * painting any new cells. Use this on a <em>transient</em> gate miss (a
     * one-tick sprint-key flicker, a momentary {@code isOnGround}, etc.) so the
     * wet volume is not hard-reverted and immediately repainted -- that full
     * revert+repaint is precisely what drops the client out of the swim pose for
     * a frame and reads as the "flash"/kick-out. Cells still revert once their
     * linger countdown expires.
     */
    public void fade(Player player) {
        if (shown.containsKey(player.getUniqueId())) {
            flush(player, java.util.Collections.emptySet());
        }
    }

    /** Reverts every ghost-water cell shown to this player and forgets their state. */
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

    /** Builds the set of air cell keys that should currently appear as water. */
    private Set<Long> computeFootprint(Player player, double speed) {
        Set<Long> target = new HashSet<>();
        World world = player.getWorld();
        Location loc = player.getLocation();

        // Body centre: ~1 block above the feet so the sphere covers feet..head.
        double px = loc.getX();
        double py = loc.getY() + 1.0D;
        double pz = loc.getZ();

        // Cone follows the LOOK direction (3D) -- swimming goes where you point.
        Vector look = loc.getDirection();
        double lx = look.getX();
        double ly = look.getY();
        double lz = look.getZ();
        double llen = Math.sqrt(lx * lx + ly * ly + lz * lz);
        boolean haveDir = llen > 1.0E-6D;
        if (haveDir) {
            lx /= llen;
            ly /= llen;
            lz /= llen;
        }

        double t = clamp01(speed / SWIM_REF_SPEED);
        double reach = REACH_MIN + t * (REACH_MAX - REACH_MIN);
        double cosHalfAngle = Math.cos(THETA_MAX - t * (THETA_MAX - THETA_MIN));

        // Apex is led one tick ahead along the look vector.
        double apexX = haveDir ? px + lx * speed * LEAD_TICKS : px;
        double apexY = haveDir ? py + ly * speed * LEAD_TICKS : py;
        double apexZ = haveDir ? pz + lz * speed * LEAD_TICKS : pz;

        int span = (int) Math.ceil(Math.max(reach, BODY_RADIUS)) + 1;
        int baseX = (int) Math.floor(apexX);
        int baseY = (int) Math.floor(apexY);
        int baseZ = (int) Math.floor(apexZ);

        for (int ox = -span; ox <= span; ox++) {
            for (int oy = -span; oy <= span; oy++) {
                for (int oz = -span; oz <= span; oz++) {
                    int cx = baseX + ox;
                    int cy = baseY + oy;
                    int cz = baseZ + oz;
                    double centerX = cx + 0.5D;
                    double centerY = cy + 0.5D;
                    double centerZ = cz + 0.5D;

                    if (!inFootprint(centerX, centerY, centerZ, px, py, pz,
                            apexX, apexY, apexZ, lx, ly, lz,
                            reach, cosHalfAngle, haveDir)) {
                        continue;
                    }
                    if (isWaterableAir(world, cx, cy, cz)) {
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
     * True if the cell centre lies in the body sphere (around the wearer) or in
     * the forward 3D cone (within {@code reach} of the led apex and within the
     * half-angle of the look direction).
     */
    private static boolean inFootprint(double cx, double cy, double cz,
                                       double px, double py, double pz,
                                       double apexX, double apexY, double apexZ,
                                       double lx, double ly, double lz,
                                       double reach, double cosHalfAngle,
                                       boolean haveDir) {
        double dpx = cx - px;
        double dpy = cy - py;
        double dpz = cz - pz;
        if (dpx * dpx + dpy * dpy + dpz * dpz <= BODY_RADIUS * BODY_RADIUS) {
            return true; // sphere around the body
        }
        if (!haveDir) {
            return false;
        }
        double ax = cx - apexX;
        double ay = cy - apexY;
        double az = cz - apexZ;
        double dist = Math.sqrt(ax * ax + ay * ay + az * az);
        if (dist > reach || dist < 1.0E-6D) {
            return false;
        }
        double dot = (ax * lx + ay * ly + az * lz) / dist; // cos(angle to look dir)
        return dot >= cosHalfAngle;
    }

    /**
     * Only convert a cell the client already treats as empty air (and whose chunk
     * is loaded) into water, so we never delete a real block and collision shapes
     * stay in sync (water is non-solid).
     */
    private static boolean isWaterableAir(World world, int x, int y, int z) {
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
            return false;
        }
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return false;
        }
        Material type = world.getBlockAt(x, y, z).getType();
        return type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR;
    }

    /**
     * Reconciles the new footprint against what is shown. Cells in the footprint
     * are (re)painted and have their linger countdown refreshed; cells no longer
     * in the footprint keep their water and only revert once their countdown runs
     * out. Refreshing instead of immediately reverting is what makes the water
     * both persist and flicker much less. All paints and the (now occasional)
     * reverts are flushed in one batched packet.
     */
    private void flush(Player player, Set<Long> target) {
        UUID id = player.getUniqueId();
        Map<Long, Integer> current = shown.computeIfAbsent(id, k -> new HashMap<>());
        World world = player.getWorld();

        List<BlockState> changes = new ArrayList<>();
        BlockData waterData = WATER_MATERIAL.createBlockData();

        // Paints: cells in the footprint that are not already watered. Either way,
        // refresh their linger countdown so they stay wet while in/near the cone.
        for (long key : target) {
            if (current.put(key, LINGER_TICKS) == null) {
                BlockState state = stateAt(world, key);
                state.setBlockData(waterData);
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
