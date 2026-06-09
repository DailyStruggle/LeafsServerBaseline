package net.leaf.artifacts;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Roller Skates (P3 signature movement): a momentum-based speed artifact.
 *
 * <p>This component owns the <em>speed ramp</em>: while the wearer keeps moving,
 * a dynamic {@link Attribute#MOVEMENT_SPEED} modifier grows from zero up to a
 * ceiling; when they stop it decays back to zero. The modifier is multiplicative
 * ({@link AttributeModifier.Operation#ADD_SCALAR}) so it stacks cleanly on top of
 * Running Shoes' flat boost -- Running Shoes sets the floor, this ramp adds up to
 * the ceiling (see the balance note in
 * {@code docs/scratch/ARTIFACTS-VANILLA-RECREATION.md}).</p>
 *
 * <p>The ramp is driven from the player's own entity scheduler once per tick, so
 * it is Folia-safe (only ever touches the owning player on its region thread).</p>
 *
 * <p>The <em>sliding mechanism</em> is the artifact's deliberate drawback and the
 * price of its speed. It is implemented client-side via {@link GhostIceProjector}:
 * while the wearer is moving at/above (roughly) vanilla sprint speed, on solid
 * ground and out of water, we project a rounded cone of client-only ghost ice
 * onto the floor ahead of their <em>motion</em> direction (not their look
 * direction), sized by speed. The client then runs vanilla ice friction over it
 * itself, so the slide is genuinely client-predicted and we never call
 * {@code setVelocity} (which eliminates the old "fling" bugs). Plain walking
 * (below the sprint threshold) projects no ice and stays fully controllable.
 * Ghost-ice packets only ever target the owning player, so this stays
 * Folia-safe.</p>
 */
public final class RollerSkatesController {

    /** Delay before a freshly-tracked player's ramp loop begins. */
    private static final long INITIAL_DELAY_TICKS = 2L;
    /** The ramp is updated every tick for a smooth acceleration curve. */
    private static final long PERIOD_TICKS = 1L;

    /**
     * Horizontal distance (blocks) covered in one tick above which the player
     * counts as "moving" for the purpose of ramping up. Roughly a slow walk.
     */
    private static final double MOVING_THRESHOLD = 0.05D;
    /** Ramp fraction gained per tick while moving (~2s of running to reach full). */
    private static final double RAMP_PER_TICK = 0.025D;
    /** Ramp fraction lost per tick while stopped (drops off noticeably faster). */
    private static final double DECAY_PER_TICK = 0.08D;
    /** Multiplicative MOVEMENT_SPEED bonus at a full ramp (the ceiling). */
    private static final double MAX_SPEED_BONUS = 0.60D;
    /**
     * Extra ramp ceiling granted while Running Shoes are also worn. Running Shoes
     * already contributes its own flat MOVEMENT_SPEED modifier (+0.20), so the
     * net advantage of wearing them with Roller Skates is ~+0.30 at full ramp:
     * more than the Running Shoes difference alone (the balance note's "floor sets
     * the bound, ceiling combines" intent), but still short of doubling the ramp.
     */
    private static final double RUNNING_SHOES_SYNERGY = 0.10D;
    /** Below this ramp value the modifier is removed entirely (treated as zero). */
    private static final double EPSILON = 1.0E-3D;

    // --- Sliding mechanism (the "catch"); tune these from in-game feel. ---
    /**
     * Horizontal speed (blocks/tick) at/above which the ice slide engages. Picked
     * to sit around vanilla sprint speed so plain walking never slides.
     */
    private static final double SLIDE_MIN_SPEED = 0.25D;
    /**
     * Horizontal distance (blocks/tick) above which a position delta is treated as
     * a teleport/relocation (RTP, /tp, ender pearl, portal, chunk reposition) and
     * NOT as movement, so relocations never engage the ice slide.
     */
    private static final double TELEPORT_DISTANCE = 1.5D;

    private final Plugin plugin;
    private final ArtifactEquipment equipment;

    /** Current ramp progress in [0, 1] per player. */
    private final Map<UUID, Double> ramp = new ConcurrentHashMap<>();
    /** Last sampled horizontal position (packed x then z) per player. */
    private final Map<UUID, double[]> lastXz = new ConcurrentHashMap<>();
    /** Previous tick's horizontal displacement (dx, dz) per player. */
    private final Map<UUID, double[]> lastDisp = new ConcurrentHashMap<>();
    /** The MOVEMENT_SPEED bonus amount currently applied per player. */
    private final Map<UUID, Double> appliedBonus = new ConcurrentHashMap<>();
    /** Projects the client-only ghost ice that produces the slide. */
    private final GhostIceProjector ghostIce = new GhostIceProjector();

    public RollerSkatesController(Plugin plugin, ArtifactEquipment equipment) {
        this.plugin = plugin;
        this.equipment = equipment;
    }

    /** Begins the per-tick ramp loop for the given player on its entity scheduler. */
    public void start(Player player) {
        player.getScheduler().runAtFixedRate(
                plugin,
                task -> tick(player),
                null,
                INITIAL_DELAY_TICKS,
                PERIOD_TICKS);
    }

    /** Removes the ramped modifier and forgets all state for the player. */
    public void clear(Player player) {
        UUID id = player.getUniqueId();
        ramp.remove(id);
        lastXz.remove(id);
        lastDisp.remove(id);
        appliedBonus.remove(id);
        ghostIce.clear(player);
        removeModifier(player);
    }

    private void tick(Player player) {
        UUID id = player.getUniqueId();

        boolean active = equipment.carries(player, ArtifactType.ROLLER_SKATES);

        double[] disp = sampleDisplacement(player, id);
        double horizontalSpeed = disp == null ? 0.0D : Math.sqrt(disp[0] * disp[0] + disp[1] * disp[1]);

        if (active && disp != null && player.isOnGround() && !player.isInWater()
                && !player.isInsideVehicle()
                && horizontalSpeed >= SLIDE_MIN_SPEED && horizontalSpeed < TELEPORT_DISTANCE) {
            ghostIce.update(player, disp[0], disp[1], horizontalSpeed);
        } else {
            // Don't hard-revert on a transient gate miss (a momentary speed dip,
            // a tick off-ground, etc.) -- that full revert + repaint is exactly
            // what reads as a "flash". Instead let the existing ice linger/age out
            // gracefully; only clear()/quit/unequip performs a hard revert.
            ghostIce.fade(player);
        }
        if (disp != null) {
            lastDisp.put(id, disp);
        } else {
            lastDisp.remove(id);
        }

        double current = ramp.getOrDefault(id, 0.0D);
        if (active && horizontalSpeed > MOVING_THRESHOLD) {
            current = Math.min(1.0D, current + RAMP_PER_TICK);
        } else {
            current = Math.max(0.0D, current - DECAY_PER_TICK);
        }

        if (current <= EPSILON) {
            ramp.remove(id);
            if (appliedBonus.remove(id) != null) {
                removeModifier(player);
            }
            return;
        }

        ramp.put(id, current);
        double maxBonus = MAX_SPEED_BONUS
                + (equipment.carries(player, ArtifactType.RUNNING_SHOES) ? RUNNING_SHOES_SYNERGY : 0.0D);
        applyBonus(player, current * maxBonus);
    }

    /**
     * Samples the player's horizontal displacement (dx, dz, blocks) since the last
     * tick; returns {@code null} on the first sample.
     */
    private double[] sampleDisplacement(Player player, UUID id) {
        double x = player.getLocation().getX();
        double z = player.getLocation().getZ();
        double[] prev = lastXz.put(id, new double[] {x, z});
        if (prev == null) {
            return null;
        }
        return new double[] {x - prev[0], z - prev[1]};
    }

    private void applyBonus(Player player, double bonus) {
        Double previous = appliedBonus.get(player.getUniqueId());
        if (previous != null && Math.abs(previous - bonus) < EPSILON) {
            return;
        }
        AttributeInstance inst = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (inst == null) {
            return;
        }
        NamespacedKey key = ArtifactKeys.modifierKey(ArtifactType.ROLLER_SKATES, Attribute.MOVEMENT_SPEED);
        removeModifier(inst, key);
        inst.addModifier(new AttributeModifier(key, bonus, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY));
        appliedBonus.put(player.getUniqueId(), bonus);
    }

    private void removeModifier(Player player) {
        AttributeInstance inst = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (inst == null) {
            return;
        }
        removeModifier(inst, ArtifactKeys.modifierKey(ArtifactType.ROLLER_SKATES, Attribute.MOVEMENT_SPEED));
    }

    private static void removeModifier(AttributeInstance inst, NamespacedKey key) {
        AttributeModifier existing;
        while ((existing = find(inst, key)) != null) {
            inst.removeModifier(existing);
        }
    }

    private static AttributeModifier find(AttributeInstance inst, NamespacedKey key) {
        for (AttributeModifier modifier : inst.getModifiers()) {
            if (modifier.getKey().equals(key)) {
                return modifier;
            }
        }
        return null;
    }
}
