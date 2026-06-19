package net.leaf.mobs;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Tracks the live {@link MobController}s for managed elites/bosses and bridges the durable
 * PDC identity (written by {@link MobFactory}) to that transient runtime state.
 *
 * <p>F2 responsibilities:</p>
 * <ul>
 *   <li><b>spawn</b> - spawn a definition and attach a controller in one step;</li>
 *   <li><b>reattach</b> - given a PDC-marked entity discovered on (re)load, look its
 *       definition up by id and rebuild a controller, so bosses survive {@code /reload}
 *       and a full restart;</li>
 *   <li><b>detach/shutdown</b> - dispose controllers on death/removal/plugin disable so no
 *       orphan boss bars linger.</li>
 * </ul>
 *
 * <p>The controller map is concurrent because lifecycle callbacks fire on different region
 * threads under Folia.</p>
 */
public final class MobManager {

    private final LeafMobsPlugin plugin;
    private final MobKeys keys;
    private final MobRegistry registry;
    private final MobFactory factory;
    private final Logger log;

    private final ConcurrentHashMap<UUID, MobController> controllers = new ConcurrentHashMap<>();

    public MobManager(LeafMobsPlugin plugin, MobKeys keys, MobRegistry registry, MobFactory factory) {
        this.plugin = plugin;
        this.keys = keys;
        this.registry = registry;
        this.factory = factory;
        this.log = plugin.getLogger();
    }

    /**
     * Spawns the definition and, for controller tiers (ELITE/BOSS), attaches a controller.
     * Must run on the region thread owning {@code loc}.
     *
     * @return the spawned entity, or {@code null} if spawning failed.
     */
    public LivingEntity spawn(MobDefinition def, Location loc) {
        LivingEntity entity = factory.spawn(def, loc);
        if (entity != null) {
            attach(entity, def);
        }
        return entity;
    }

    /** Creates and starts a controller for {@code entity} if its tier uses one. */
    public void attach(LivingEntity entity, MobDefinition def) {
        if (def == null || !def.tier().usesController()) {
            return;
        }
        // Build/insert the controller inside computeIfAbsent, but start() it only *after* the
        // map mutation completes. start() can synchronously dispose the controller (e.g. the
        // entity was already retired before the tick task could be scheduled), and dispose()
        // calls back into onControllerDisposed -> controllers.remove(...). Doing that from
        // within the computeIfAbsent mapping function re-enters the same ConcurrentHashMap bin
        // and throws "IllegalStateException: Recursive update".
        UUID uuid = entity.getUniqueId();
        boolean[] created = {false};
        MobController controller = controllers.computeIfAbsent(uuid, id -> {
            created[0] = true;
            return new MobController(plugin, entity, def);
        });
        if (created[0]) {
            controller.start();
        }
    }

    /**
     * Rebuilds a controller for a PDC-marked entity that just (re)entered the world. No-op
     * for unmanaged entities, entities we already track, or ids whose definition is no
     * longer in the reference table (the entity keeps living, just without a controller).
     */
    public void reattach(LivingEntity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (!keys.isManaged(pdc)) {
            return;
        }
        UUID uuid = entity.getUniqueId();
        if (controllers.containsKey(uuid)) {
            return;
        }
        String id = keys.readMobId(pdc);
        MobDefinition def = registry.byId(id);
        if (def == null) {
            log.fine("[leaf-mobs] no definition for managed mob '" + id + "' (" + uuid
                    + "); leaving it without a controller.");
            return;
        }
        attach(entity, def);
    }

    /** Disposes the controller for the given entity, if any (idempotent). */
    public void detach(UUID uuid) {
        MobController c = controllers.get(uuid);
        if (c != null) {
            c.dispose();
        }
    }

    /** Callback from {@link MobController#dispose()} so the map drops the entry. */
    void onControllerDisposed(UUID uuid) {
        controllers.remove(uuid);
    }

    /**
     * Best-effort scan of already-loaded entities, used when the plugin is enabled while
     * the server is already running (e.g. a plugin reload). On a normal restart the
     * per-entity {@code EntityAddToWorldEvent} path handles reattach instead.
     */
    public int scanLoaded() {
        int scheduled = 0;
        for (World world : plugin.getServer().getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (!keys.isManaged(entity.getPersistentDataContainer())) {
                    continue;
                }
                // Touch the entity only on its owning region thread (Folia-safe).
                entity.getScheduler().run(plugin, t -> reattach(entity), null);
                scheduled++;
            }
        }
        return scheduled;
    }

    /** Disposes every controller (e.g. on plugin disable) so no boss bar is orphaned. */
    public void shutdown() {
        for (MobController c : controllers.values()) {
            c.dispose();
        }
        controllers.clear();
    }

    public int activeCount() {
        return controllers.size();
    }

    /** True if the entity carries our managed-mob PDC marker (an ELITE/BOSS we control). */
    public boolean isManaged(LivingEntity entity) {
        return entity != null && keys.isManaged(entity.getPersistentDataContainer());
    }

    /**
     * True if the entity is one of our managed mobs whose stored tier is BOSS. Read from the
     * durable tier PDC (written by {@link MobFactory}) so it works on bosses that loaded from a
     * previous session. Used to gate the anti-burst per-hit damage cap to bosses only - common
     * reskins and elites keep vanilla damage behaviour.
     */
    public boolean isBossTier(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        String tier = entity.getPersistentDataContainer()
                .get(keys.tier(), PersistentDataType.STRING);
        return MobTier.BOSS.name().equals(tier);
    }

    /**
     * Returns the fraction of melee damage {@code entity} should reflect right now, driven by
     * the Thorn-Pendant-themed {@code thorns_guard} boost, or {@code 0.0} when no buff is active
     * (expired or never cast). The expiry/fraction live in the mob's PDC so the buff persists
     * across a chunk reload mid-fight.
     */
    public double thornsReflect(LivingEntity entity) {
        if (entity == null) {
            return 0.0;
        }
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        Long until = pdc.get(keys.thornsUntil(), PersistentDataType.LONG);
        if (until == null || System.currentTimeMillis() >= until) {
            return 0.0;
        }
        Double pct = pdc.get(keys.thornsPct(), PersistentDataType.DOUBLE);
        return pct == null ? 0.0 : Math.max(0.0, Math.min(1.0, pct));
    }
}
