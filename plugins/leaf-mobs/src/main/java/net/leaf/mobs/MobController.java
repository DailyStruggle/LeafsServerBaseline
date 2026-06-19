package net.leaf.mobs;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Runtime owner of one active managed mob (ELITE/BOSS). It holds the transient state that
 * does <em>not</em> persist on disk - the boss bar and its viewer set - and rebuilds it
 * from the durable PDC identity whenever the entity (re)enters the world. This is the F2
 * "lifecycle" piece: a boss spawned before a {@code /reload} or restart gets a fresh
 * controller via {@link MobManager#reattach} when its entity loads again.
 *
 * <p>Folia: the periodic update runs on the entity's own scheduler, so every read/write of
 * the entity and the surrounding players happens on the owning region thread. The task
 * auto-retires when the entity is removed, which disposes the bar.</p>
 */
public final class MobController {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    /** How far a player can be (blocks) and still see the boss bar. */
    private static final double VIEW_RADIUS = 48.0;
    /** Update cadence in server ticks (~1s). */
    private static final long PERIOD_TICKS = 20L;

    private final LeafMobsPlugin plugin;
    private final LivingEntity entity;
    private final String mobId;
    private final MobTier tier;

    private final BossBar bossBar;
    private final Set<UUID> viewers = new HashSet<>();
    private final List<ActiveAbility> abilities = new ArrayList<>();

    private ScheduledTask task;
    private boolean disposed;
    /** Server ticks this controller has been running; the cooldown/phase clock for abilities. */
    private long ticksLived;

    MobController(LeafMobsPlugin plugin, LivingEntity entity, MobDefinition def) {
        this.plugin = plugin;
        this.entity = entity;
        this.mobId = def.id();
        this.tier = def.tier();
        this.bossBar = tier.usesBossBar() ? createBar(def) : null;
        buildAbilities(def);
    }

    /** Resolves each ability slot against the registry once, dropping unknown types. */
    private void buildAbilities(MobDefinition def) {
        for (AbilitySpec spec : def.abilities()) {
            Ability ability = plugin.abilities().get(spec.type());
            if (ability == null) {
                plugin.getLogger().warning("[leaf-mobs] '" + def.id()
                        + "': unknown ability type '" + spec.type() + "'; skipped.");
                continue;
            }
            abilities.add(new ActiveAbility(spec, ability));
        }
    }

    private static BossBar createBar(MobDefinition def) {
        Component title = def.displayName() != null
                ? MINI.deserialize(def.displayName())
                : Component.text(def.id());
        return BossBar.bossBar(title, 1.0f, def.tier().barColor(), BossBar.Overlay.PROGRESS);
    }

    /** Begins the periodic update on the entity's region thread. Safe to call once. */
    void start() {
        if (disposed || task != null) {
            return;
        }
        this.task = entity.getScheduler().runAtFixedRate(
                plugin, t -> tick(), this::dispose, PERIOD_TICKS, PERIOD_TICKS);
        if (task == null) {
            // Entity was already retired before we could schedule; clean up immediately.
            dispose();
        }
    }

    private void tick() {
        if (disposed) {
            return;
        }
        if (!entity.isValid() || entity.isDead()) {
            dispose();
            return;
        }
        ticksLived += PERIOD_TICKS;
        if (bossBar != null) {
            bossBar.progress(healthFraction());
            refreshViewers();
        }
        tickAbilities();
    }

    /**
     * Evaluates each ability's gating (health phase, cooldown, chance) and casts the ones
     * that pass. Runs on the entity's region thread, so casts may act on the world directly.
     * Abilities are skipped entirely when no player is nearby, so an idle boss is silent.
     */
    private void tickAbilities() {
        if (abilities.isEmpty()) {
            return;
        }
        LivingEntity target = resolveTarget();
        if (target == null) {
            return;
        }
        double hpFrac = healthFraction();
        for (ActiveAbility active : abilities) {
            AbilitySpec spec = active.spec;
            if (!spec.phaseAllows(hpFrac)) {
                continue;
            }
            if (ticksLived - active.lastCastTick < spec.cooldownTicks()) {
                continue;
            }
            if (spec.chance() < 1.0 && ThreadLocalRandom.current().nextDouble() > spec.chance()) {
                // Roll failed this opportunity; re-roll next time the cooldown elapses.
                active.lastCastTick = ticksLived;
                continue;
            }
            AbilityContext ctx = new AbilityContext(plugin, entity, target, spec.params());
            active.ability.cast(ctx);
            active.lastCastTick = ticksLived;
        }
    }

    /**
     * The mob's combat target: its explicit AI target if it is a {@link Mob} and that target
     * is a valid living entity, else the nearest player inside the view radius.
     */
    private LivingEntity resolveTarget() {
        if (entity instanceof Mob mob) {
            LivingEntity ai = mob.getTarget();
            if (ai != null && ai.isValid() && !ai.isDead()) {
                return ai;
            }
        }
        Player nearest = null;
        double best = Double.MAX_VALUE;
        for (Player p : entity.getWorld().getNearbyPlayers(entity.getLocation(), VIEW_RADIUS)) {
            if (p.isDead() || !p.isValid() || p.getGameMode().name().equals("SPECTATOR")) {
                continue;
            }
            double d = p.getLocation().distanceSquared(entity.getLocation());
            if (d < best) {
                best = d;
                nearest = p;
            }
        }
        return nearest;
    }

    private float healthFraction() {
        AttributeInstance max = entity.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = max != null ? max.getValue() : entity.getHealth();
        if (maxHealth <= 0) {
            return 0f;
        }
        double frac = entity.getHealth() / maxHealth;
        return (float) Math.max(0.0, Math.min(1.0, frac));
    }

    private void refreshViewers() {
        Set<UUID> current = new HashSet<>();
        for (Player p : entity.getWorld().getNearbyPlayers(entity.getLocation(), VIEW_RADIUS)) {
            current.add(p.getUniqueId());
            if (viewers.add(p.getUniqueId())) {
                p.showBossBar(bossBar);
            }
        }
        // Hide from players who left the radius (or the world).
        viewers.removeIf(id -> {
            if (current.contains(id)) {
                return false;
            }
            Player gone = plugin.getServer().getPlayer(id);
            if (gone != null) {
                gone.hideBossBar(bossBar);
            }
            return true;
        });
    }

    /** Hides the bar from all viewers and cancels the tick. Idempotent. */
    void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        if (bossBar != null) {
            for (UUID id : viewers) {
                Player p = plugin.getServer().getPlayer(id);
                if (p != null) {
                    p.hideBossBar(bossBar);
                }
            }
            viewers.clear();
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
        plugin.manager().onControllerDisposed(entity.getUniqueId());
    }

    public String mobId() {
        return mobId;
    }

    public MobTier tier() {
        return tier;
    }

    public boolean isDisposed() {
        return disposed;
    }

    /** Runtime pairing of an {@link AbilitySpec} with its resolved {@link Ability} + cooldown clock. */
    private static final class ActiveAbility {
        private final AbilitySpec spec;
        private final Ability ability;
        private long lastCastTick = Long.MIN_VALUE / 2;

        private ActiveAbility(AbilitySpec spec, Ability ability) {
            this.spec = spec;
            this.ability = ability;
        }
    }
}
