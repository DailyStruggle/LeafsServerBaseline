package net.leaf.artifacts;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helium Flamingo (P3 signature movement): swim through the air.
 *
 * <p>The intended fantasy is that the wearer "swims" through the air rather than
 * falling. Both the swim <em>pose</em> and the swim <em>physics</em> are derived
 * by the owning client from <em>water contact</em>: it re-evaluates "am I in
 * water?" from the blocks intersecting its bounding box every tick. Forcing the
 * swim flag server-side ({@link Player#setSwimming(boolean)}) therefore never
 * latched -- the wearer's own {@code LocalPlayer} cleared it in the air every
 * tick (it only showed on observers).</p>
 *
 * <p>So, mirroring the Roller Skates ghost-ice approach, we make the client
 * <em>believe</em> it is submerged: {@link GhostWaterProjector} sends client-only
 * water block-changes into the air cells around (and ahead of) the wearer's body.
 * The client then runs real swim physics and the swim pose itself -- exactly the
 * rule that previously fought us, now turned in our favour. Nothing touches the
 * server world or the player's velocity.</p>
 *
 * <p>The air-swim only engages while the wearer is equipped, airborne, out of
 * real water, not on an elytra glide, and not (creative/ability) flying. Crucially
 * it can only <b>begin</b> when the player <em>starts</em> pressing the sprint key
 * (Ctrl) <em>while already mid-air and not falling</em> -- so it must be a
 * deliberate jump-then-swim, never a fall-saver and never while flying. Once
 * started it stays latched while those conditions hold; releasing Ctrl, landing,
 * entering water, unequipping or quitting reverts the ghost water immediately, so
 * nothing persists.</p>
 *
 * <p>Air-swim is a <b>timed, rechargeable</b> resource matching the original
 * Artifacts mod's Helium Flamingo: the wearer gets {@value #FLIGHT_DURATION_TICKS}
 * ticks (8s) of flight, which drains while swimming and refills over 15s once they
 * are back on the ground; emptying it ends the swim and starts a 3s cooldown. The
 * remaining flight time (and the recharge cooldown) is surfaced to the wearer via
 * a per-player {@link BossBar}.</p>
 *
 * <p>The loop is driven from the player's own entity scheduler once per tick, so
 * it is Folia-safe (only ever touches the owning player on its region thread).</p>
 */
public final class HeliumFlamingoController {

    /** Delay before a freshly-tracked player's loop begins. */
    private static final long INITIAL_DELAY_TICKS = 2L;
    /** The air-swim is evaluated every tick. */
    private static final long PERIOD_TICKS = 1L;

    /**
     * Horizontal distance (blocks/tick) above which a position delta is treated
     * as a teleport/relocation (RTP, /tp, ender pearl, portal) rather than real
     * motion, so relocations never drive the projected water cone.
     */
    private static final double TELEPORT_DISTANCE = 1.5D;

    /**
     * Vertical velocity (blocks/tick) at or below which the wearer counts as
     * "falling" for the purpose of <em>starting</em> an air-swim. The start trigger
     * is rejected while the player is descending so a Ctrl press during a free-fall
     * never engages the swim (it must be initiated near a jump/apex, not used as a
     * fall-saver). A small negative tolerance keeps the jump-apex window forgiving.
     */
    private static final double FALL_VELOCITY_THRESHOLD = -0.08D;

    /**
     * Maximum continuous air-swim time, in ticks. Matches the original Artifacts
     * mod's Helium Flamingo {@code flightDuration} default of 8 seconds.
     */
    private static final int FLIGHT_DURATION_TICKS = 8 * 20;
    /**
     * Time to fully recharge the flight meter from empty, in ticks, while the
     * wearer is on the ground (the original recharges on touching ground). Matches
     * the original {@code rechargeDuration} default of 15 seconds.
     */
    private static final int RECHARGE_DURATION_TICKS = 15 * 20;
    /**
     * Cooldown applied when an air-swim ends (matches the original {@code cooldown}
     * default of 3 seconds): the wearer cannot start swimming again until it
     * elapses. The original recharges only after this cooldown, so the meter also
     * holds steady while the cooldown is counting down.
     */
    private static final int COOLDOWN_TICKS = 3 * 20;
    /** Charge regained per grounded tick so a full recharge takes {@link #RECHARGE_DURATION_TICKS}. */
    private static final double RECHARGE_PER_TICK =
            (double) FLIGHT_DURATION_TICKS / RECHARGE_DURATION_TICKS;

    private final Plugin plugin;
    private final ArtifactEquipment equipment;

    /** Players whose air-swim ghost water we are currently projecting. */
    private final Map<UUID, Boolean> forcing = new ConcurrentHashMap<>();
    /** Last sampled horizontal position (x, z) per player, for motion sampling. */
    private final Map<UUID, double[]> lastXz = new ConcurrentHashMap<>();
    /** Previous tick's sprint-key state per player, for rising-edge detection. */
    private final Map<UUID, Boolean> wasSprinting = new ConcurrentHashMap<>();
    /** Players in an active air-swim session (latched once started, until conditions drop). */
    private final Map<UUID, Boolean> active = new ConcurrentHashMap<>();
    /** Remaining air-swim time per player, in ticks (0..{@link #FLIGHT_DURATION_TICKS}). */
    private final Map<UUID, Double> chargeTicks = new ConcurrentHashMap<>();
    /** Remaining post-flight cooldown per player, in ticks. */
    private final Map<UUID, Integer> cooldownTicks = new ConcurrentHashMap<>();
    /** Per-player boss bar displaying the remaining flight time / recharge. */
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();
    /** Projects the client-only ghost water that produces the air-swim. */
    private final GhostWaterProjector ghostWater = new GhostWaterProjector();

    public HeliumFlamingoController(Plugin plugin, ArtifactEquipment equipment) {
        this.plugin = plugin;
        this.equipment = equipment;
    }

    /** Begins the per-tick air-swim loop for the given player on its entity scheduler. */
    public void start(Player player) {
        player.getScheduler().runAtFixedRate(
                plugin,
                task -> tick(player),
                null,
                INITIAL_DELAY_TICKS,
                PERIOD_TICKS);
    }

    /** Reverts any projected water and forgets all state for the player. */
    public void clear(Player player) {
        UUID id = player.getUniqueId();
        lastXz.remove(id);
        wasSprinting.remove(id);
        active.remove(id);
        chargeTicks.remove(id);
        cooldownTicks.remove(id);
        BossBar bar = bossBars.remove(id);
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
        }
        if (forcing.remove(id) != null) {
            ghostWater.clear(player);
        }
    }

    /**
     * Whether the player is currently in an active Helium Flamingo air-swim (the
     * ghost-water swim state is being projected for them this tick). Other
     * artifacts (e.g. Flippers) read this so their in-water effects also apply
     * during the air-swim, matching the documented air-swim synergy.
     */
    public boolean isAirSwimming(Player player) {
        return Boolean.TRUE.equals(forcing.get(player.getUniqueId()));
    }

    private void tick(Player player) {
        UUID id = player.getUniqueId();

        double[] disp = sampleDisplacement(player, id);
        double speed = disp == null ? 0.0D
                : Math.sqrt(disp[0] * disp[0] + disp[1] * disp[1] + disp[2] * disp[2]);
        boolean relocation = speed >= TELEPORT_DISTANCE;

        boolean carry = equipment.carries(player, ArtifactType.HELIUM_FLAMINGO);
        double charge = chargeTicks.getOrDefault(id, (double) FLIGHT_DURATION_TICKS);
        int cooldown = cooldownTicks.getOrDefault(id, 0);

        // Always evaluate the swim-state machine so rising-edge (Ctrl press)
        // tracking stays current even on ticks where flight is unavailable.
        boolean swimStateOk = carry && !relocation && updateAirSwimState(player, id);
        // Flight is only actually granted while there is charge left and no
        // active post-flight cooldown -- matching the original timed meter.
        boolean swimming = swimStateOk && charge > 0.0D && cooldown <= 0;

        if (swimming) {
            charge -= 1.0D;
            if (charge <= 0.0D) {
                // Meter drained: end the session, start the cooldown, and hard
                // clear the ghost water immediately (no lingering wet volume once
                // the player is out of charge).
                charge = 0.0D;
                active.remove(id);
                cooldown = COOLDOWN_TICKS;
                forcing.remove(id);
                ghostWater.clear(player);
            } else {
                // The water cone follows the look direction inside the projector;
                // we only pass the 3D speed so it can size the cone's reach/angle.
                ghostWater.update(player, speed);
                forcing.put(id, Boolean.TRUE);
            }
        } else {
            // No longer swimming: a depleted meter or an active cooldown must drop
            // any latched session so it cannot silently resume when cooldown ends.
            if (charge <= 0.0D || cooldown > 0) {
                active.remove(id);
            }
            // Don't hard-revert all water on a transient gate miss (a one-tick
            // sprint-key flicker, a momentary isOnGround, a relocation false
            // positive). The full revert+repaint is exactly what drops the client
            // out of the swim pose for a frame and reads as the "flash"/kick-out.
            // Instead let the existing water linger/age out gracefully; only
            // clear()/quit/unequip performs a hard revert.
            //
            // Exception: when the wearer is actually out of charge (empty meter or
            // an active post-flight cooldown), hard-clear the ghost water right
            // away rather than letting it linger -- being out of charge should
            // leave no water behind.
            forcing.remove(id);
            if (charge <= 0.0D || cooldown > 0) {
                ghostWater.clear(player);
            } else {
                ghostWater.fade(player);
            }

            if (cooldown > 0) {
                cooldown--;
            } else if (charge < FLIGHT_DURATION_TICKS
                    && (player.isOnGround() || player.isInWater())) {
                // Recharge only once grounded (or in water), as in the original.
                charge = Math.min(FLIGHT_DURATION_TICKS, charge + RECHARGE_PER_TICK);
            }
        }

        chargeTicks.put(id, charge);
        cooldownTicks.put(id, cooldown);
        updateBossBar(player, id, carry, charge, cooldown);
    }

    /**
     * Shows/updates a per-player boss bar with the remaining flight time. The bar
     * is hidden when the wearer is idle at full charge (no clutter) and shown while
     * swimming, while the meter is below full, or during the recharge cooldown. It
     * turns red while on cooldown and blue otherwise; the fill is the fraction of
     * flight time remaining.
     */
    private void updateBossBar(Player player, UUID id, boolean carry, double charge, int cooldown) {
        boolean swimming = Boolean.TRUE.equals(forcing.get(id));
        boolean show = carry && (swimming || charge < FLIGHT_DURATION_TICKS || cooldown > 0);
        BossBar bar = bossBars.get(id);
        if (!show) {
            if (bar != null) {
                bar.setVisible(false);
            }
            return;
        }
        if (bar == null) {
            bar = Bukkit.createBossBar("Helium Flamingo", BarColor.BLUE, BarStyle.SEGMENTED_10);
            bar.addPlayer(player);
            bossBars.put(id, bar);
        }
        double progress = charge / FLIGHT_DURATION_TICKS;
        bar.setProgress(Math.max(0.0D, Math.min(1.0D, progress)));
        if (cooldown > 0) {
            bar.setColor(BarColor.RED);
            bar.setTitle(String.format("Helium Flamingo - recharging (%.1fs)", cooldown / 20.0D));
        } else {
            bar.setColor(BarColor.BLUE);
            bar.setTitle(String.format("Helium Flamingo - %.1fs", charge / 20.0D));
        }
        bar.setVisible(true);
    }

    /**
     * Decides whether the wearer is air-swimming this tick, with a latched session
     * that can only <em>begin</em> on a deliberate mid-air Ctrl press.
     *
     * <p>Base conditions (must hold every tick to swim, and to keep a session
     * alive): the wearer is sprinting (Ctrl held), aloft, out of water, not riding
     * a vehicle, <b>not flying</b> (never while creative/ability flight is on), and
     * not gliding an elytra.</p>
     *
     * <p>Starting a session additionally requires a <em>rising edge</em> of the
     * sprint key (the player STARTS pressing Ctrl) that occurs while already
     * mid-air and <b>not while falling</b>. Pressing Ctrl on the ground and then
     * jumping does not start it (the press did not happen mid-air), and pressing
     * Ctrl during a descent does not start it (falling). Once started, the session
     * stays latched while the base conditions hold, so swimming downward (which is
     * itself a descent) does not cancel it.</p>
     */
    private boolean updateAirSwimState(Player player, UUID id) {
        boolean sprintingNow = player.isSprinting();
        boolean wasSprintingLast = Boolean.TRUE.equals(wasSprinting.put(id, sprintingNow));

        boolean baseOk = sprintingNow
                && !player.isOnGround()
                && !player.isInWater()
                && !player.isInsideVehicle()
                && !player.isFlying()
                && !hasElytra(player);
        if (!baseOk) {
            active.remove(id);
            return false;
        }

        if (Boolean.TRUE.equals(active.get(id))) {
            return true; // session already running -- keep it latched
        }

        // Not yet active: only START on a rising edge of Ctrl, mid-air, not falling.
        boolean risingEdge = !wasSprintingLast;
        boolean falling = player.getVelocity().getY() < FALL_VELOCITY_THRESHOLD;
        if (risingEdge && !falling) {
            active.put(id, Boolean.TRUE);
            return true;
        }
        return false;
    }

    /**
     * Samples the player's 3D displacement (dx, dy, dz, blocks) since the last
     * tick; returns {@code null} on the first sample. Swimming in air moves the
     * player vertically too, so the vertical delta is included in the speed.
     */
    private double[] sampleDisplacement(Player player, UUID id) {
        double x = player.getLocation().getX();
        double y = player.getLocation().getY();
        double z = player.getLocation().getZ();
        double[] prev = lastXz.put(id, new double[] {x, y, z});
        if (prev == null) {
            return null;
        }
        return new double[] {x - prev[0], y - prev[1], z - prev[2]};
    }

    /** Whether the player is wearing an elytra in the chest slot (let it glide normally). */
    private static boolean hasElytra(Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        return chest != null && chest.getType() == Material.ELYTRA;
    }
}
