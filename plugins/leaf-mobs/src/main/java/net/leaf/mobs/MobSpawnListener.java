package net.leaf.mobs;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * F4 spawn service: turns a fraction of natural vanilla spawns into managed mobs using the
 * approved random-selection model (filter by spawn reason + base entity type + biome, roll a
 * per-mob chance, then enforce a cooldown and a live-count cap before transforming).
 *
 * <p>This mirrors {@code elite-spawn-gate}'s {@link CreatureSpawnEvent} pattern: the common
 * path is a couple of cheap checks, so vanilla and command-spawned mobs are never inspected
 * beyond a reason/type test. Only a candidate that already passed reason+type+biome+chance
 * pays for the (rare) cooldown and live-count work.</p>
 *
 * <p>Folia: {@link CreatureSpawnEvent} fires on the region thread that owns the spawn
 * location, so reading the biome there is safe. We do not spawn inside the event (you must
 * not spawn while another spawn is being processed); instead we cancel the vanilla spawn and
 * hand the managed spawn to the region scheduler for the next tick, which keeps it on the
 * owning region thread on both Paper and Folia. {@link MobFactory} spawns with
 * {@link CreatureSpawnEvent.SpawnReason#CUSTOM}, which is not in any rule's reason set, so our
 * own re-spawn never re-enters this listener.</p>
 */
public final class MobSpawnListener implements Listener {

    private final LeafMobsPlugin plugin;
    private final MobRegistry registry;
    private final MobManager manager;
    private final MobKeys keys;
    private final Logger log;

    /** Last successful natural spawn time per mob id (epoch millis), for the cooldown gate. */
    private final ConcurrentHashMap<String, Long> lastSpawnMillis = new ConcurrentHashMap<>();

    public MobSpawnListener(LeafMobsPlugin plugin) {
        this.plugin = plugin;
        this.registry = plugin.registry();
        this.manager = plugin.manager();
        this.keys = plugin.keys();
        this.log = plugin.getLogger();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        EntityType baseType = event.getEntityType();
        Location loc = event.getLocation();
        World world = loc.getWorld();
        if (world == null) {
            return;
        }

        // Gather rows whose rule matches this base type + reason (cheap; usually empty).
        List<MobDefinition> candidates = new ArrayList<>();
        for (MobDefinition def : registry.all()) {
            if (def.spawnRule().matches(baseType, reason)) {
                candidates.add(def);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }

        // Biome filter (safe to read here - we are on the spawn's region thread).
        NamespacedKey biome = world.getBiome(loc).getKey();
        Collections.shuffle(candidates);

        long now = System.currentTimeMillis();
        for (MobDefinition def : candidates) {
            if (!def.appliesToBiome(biome)) {
                continue;
            }
            MobDefinition.SpawnRule rule = def.spawnRule();
            if (ThreadLocalRandom.current().nextDouble() >= rule.chance()) {
                continue;
            }
            if (onCooldown(def.id(), rule, now)) {
                continue;
            }
            if (rule.maxLive() > 0 && countLive(def.id()) >= rule.maxLive()) {
                continue;
            }

            // Selected: cancel the vanilla spawn and spawn our managed mob next tick.
            event.setCancelled(true);
            lastSpawnMillis.put(def.id(), now);
            plugin.getServer().getRegionScheduler().run(plugin, loc, t -> {
                LivingEntity spawned = manager.spawn(def, loc);
                if (spawned == null) {
                    // Spawn failed; let the cooldown lapse so a future attempt can retry.
                    lastSpawnMillis.remove(def.id());
                }
            });
            log.fine("[leaf-mobs] natural spawn: '" + def.id() + "' replacing " + baseType
                    + " (" + reason + ") in " + biome + " at " + loc.getBlockX() + ","
                    + loc.getBlockY() + "," + loc.getBlockZ());
            return;
        }
    }

    private boolean onCooldown(String id, MobDefinition.SpawnRule rule, long now) {
        if (rule.cooldownMillis() <= 0L) {
            return false;
        }
        Long last = lastSpawnMillis.get(id);
        return last != null && (now - last) < rule.cooldownMillis();
    }

    /**
     * Counts currently-loaded living entities carrying {@code id}'s mob marker. Only called
     * for a rare already-selected candidate, so the loaded-entity scan cost is acceptable and
     * avoids the unload/death bookkeeping bugs of an incrementally-maintained counter.
     */
    private int countLive(String id) {
        int count = 0;
        for (World world : plugin.getServer().getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                PersistentDataContainer pdc = entity.getPersistentDataContainer();
                if (keys.isManaged(pdc) && id.equalsIgnoreCase(keys.readMobId(pdc))) {
                    count++;
                }
            }
        }
        return count;
    }
}
