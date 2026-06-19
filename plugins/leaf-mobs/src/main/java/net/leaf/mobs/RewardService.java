package net.leaf.mobs;

import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * F5 rewards: when a PDC-marked managed mob dies, roll its configured loot table and grant
 * its configured advancement to the killer. This is the generic, data-driven half of the
 * "artifact on first kill + rare on the loot table" design:
 *
 * <ul>
 *   <li><b>Loot table</b> ({@code loot_table:}) - a vanilla/datapack {@link LootTable}, rolled
 *       with the killer as looter so the drop is tied to the marked boss, not the base type.
 *       This is the <em>repeatable, rare</em> path; the datapack table decides weights, so a
 *       biome-themed artifact can sit at a low weight there.</li>
 *   <li><b>Advancement</b> ({@code advancement:}) - granted to the killer on every kill, but
 *       vanilla records "done" once, so its reward (the <em>guaranteed first-kill</em>
 *       artifact, via the datapack advancement's rewards) fires only the first time.</li>
 *   <li>Optional {@code clear_vanilla_drops:} wipes the base mob's drops first (bosses
 *       usually carry no vanilla loot); optional {@code xp:} overrides dropped experience.</li>
 * </ul>
 *
 * <p>Folia: {@link EntityDeathEvent} fires on the dying entity's region thread, so editing
 * {@code drops}/{@code droppedExp} is safe here. The advancement grant touches the killer, so
 * it is scheduled on that player's own scheduler.</p>
 */
public final class RewardService {

    private final LeafMobsPlugin plugin;
    private final MobRegistry registry;
    private final MobKeys keys;
    private final Logger log;

    public RewardService(LeafMobsPlugin plugin) {
        this.plugin = plugin;
        this.registry = plugin.registry();
        this.keys = plugin.keys();
        this.log = plugin.getLogger();
    }

    /**
     * Applies rewards for a managed-mob death. No-op for unmanaged entities or ids whose
     * definition is no longer in the reference table.
     */
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (!keys.isManaged(pdc)) {
            return;
        }
        MobDefinition def = registry.byId(keys.readMobId(pdc));
        if (def == null) {
            return;
        }

        if (def.clearVanillaDrops()) {
            event.getDrops().clear();
        }
        if (def.xpOverride() >= 0) {
            event.setDroppedExp(def.xpOverride());
        }

        Player killer = entity.getKiller();
        rollLoot(def, entity, killer, event);
        grantAdvancement(def, killer);
    }

    private void rollLoot(MobDefinition def, LivingEntity entity, Player killer, EntityDeathEvent event) {
        if (def.lootTable() == null) {
            return;
        }
        NamespacedKey key = NamespacedKey.fromString(def.lootTable().toLowerCase());
        if (key == null) {
            log.warning("[leaf-mobs] '" + def.id() + "': invalid loot_table key '" + def.lootTable() + "'");
            return;
        }
        LootTable table = plugin.getServer().getLootTable(key);
        if (table == null) {
            log.warning("[leaf-mobs] '" + def.id() + "': loot_table '" + key + "' not found (datapack loaded?)");
            return;
        }
        LootContext.Builder ctx = new LootContext.Builder(entity.getLocation()).lootedEntity(entity);
        if (killer != null) {
            ctx.killer(killer);
        }
        Collection<org.bukkit.inventory.ItemStack> loot =
                table.populateLoot(ThreadLocalRandom.current(), ctx.build());
        event.getDrops().addAll(loot);
    }

    private void grantAdvancement(MobDefinition def, Player killer) {
        if (def.advancement() == null || killer == null) {
            return;
        }
        NamespacedKey key = NamespacedKey.fromString(def.advancement().toLowerCase());
        if (key == null) {
            log.warning("[leaf-mobs] '" + def.id() + "': invalid advancement key '" + def.advancement() + "'");
            return;
        }
        Advancement advancement = plugin.getServer().getAdvancement(key);
        if (advancement == null) {
            log.warning("[leaf-mobs] '" + def.id() + "': advancement '" + key + "' not found (datapack loaded?)");
            return;
        }
        // Award on the killer's own region thread (Folia-safe).
        killer.getScheduler().run(plugin, t -> {
            AdvancementProgress progress = killer.getAdvancementProgress(advancement);
            if (progress.isDone()) {
                return;
            }
            for (String criterion : progress.getRemainingCriteria()) {
                progress.awardCriteria(criterion);
            }
        }, null);
    }
}
