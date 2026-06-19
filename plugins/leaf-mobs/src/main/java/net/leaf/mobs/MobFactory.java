package net.leaf.mobs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Spawns a {@link MobDefinition} into the world and stamps it with the managed-mob PDC
 * markers. This is the generic core: it never hard-codes a specific mob, it only applies
 * whatever the reference table says.
 *
 * <p>Folia note: {@link #spawn} must be called on the region thread that owns the target
 * location (e.g. inside a command handler for the caller's location, or a region-scheduler
 * task). The configuration consumer runs pre-add, so attributes/gear/markers are present
 * before the entity is exposed to the world.</p>
 */
public final class MobFactory {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final MobKeys keys;
    private final Logger log;

    public MobFactory(MobKeys keys, Logger log) {
        this.keys = keys;
        this.log = log;
    }

    /**
     * Spawns the definition at {@code loc}.
     *
     * @return the spawned entity, or {@code null} if the entity class could not be resolved.
     */
    public LivingEntity spawn(MobDefinition def, Location loc) {
        World world = loc.getWorld();
        if (world == null) {
            return null;
        }
        EntityType type = def.entityType();
        Class<? extends Entity> clazz = type.getEntityClass();
        if (clazz == null || !LivingEntity.class.isAssignableFrom(clazz)) {
            log.warning("[leaf-mobs] cannot spawn '" + def.id() + "': " + type + " is not a living entity class.");
            return null;
        }

        Entity spawned = world.spawn(loc, clazz, CreatureSpawnEvent.SpawnReason.CUSTOM,
                e -> configure((LivingEntity) e, def));
        return spawned instanceof LivingEntity living ? living : null;
    }

    private void configure(LivingEntity entity, MobDefinition def) {
        // Identity markers (durable across unload/restart).
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(keys.mobId(), PersistentDataType.STRING, def.id());
        pdc.set(keys.tier(), PersistentDataType.STRING, def.tier().name());
        if (!def.biomes().isEmpty()) {
            pdc.set(keys.biome(), PersistentDataType.STRING,
                    def.biomes().iterator().next().toString());
        }

        // Display name.
        if (def.displayName() != null) {
            Component name = MINI.deserialize(def.displayName());
            entity.customName(name);
            entity.setCustomNameVisible(true);
        }

        // Attributes.
        for (Map.Entry<Attribute, Double> e : def.attributes().entrySet()) {
            AttributeInstance inst = entity.getAttribute(e.getKey());
            if (inst == null) {
                log.warning("[leaf-mobs] '" + def.id() + "': " + e.getKey().getKey()
                        + " not applicable to " + def.entityType() + "; skipped.");
                continue;
            }
            inst.setBaseValue(e.getValue());
        }
        // Heal to the (possibly raised) max health so the bar starts full.
        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            entity.setHealth(maxHealth.getValue());
        }

        // Equipment.
        applyEquipment(entity, def.equipment());

        // Lifecycle flags.
        entity.setPersistent(def.persistent());
        entity.setRemoveWhenFarAway(!def.persistent());
        entity.setGlowing(def.glowing());
    }

    private void applyEquipment(LivingEntity entity, MobDefinition.EquipmentSpec spec) {
        if (spec == null) {
            return;
        }
        EntityEquipment eq = entity.getEquipment();
        if (eq == null) {
            return;
        }
        if (spec.helmet() != null) {
            eq.setHelmet(item(spec.helmet()));
        }
        if (spec.chestplate() != null) {
            eq.setChestplate(item(spec.chestplate()));
        }
        if (spec.leggings() != null) {
            eq.setLeggings(item(spec.leggings()));
        }
        if (spec.boots() != null) {
            eq.setBoots(item(spec.boots()));
        }
        if (spec.mainHand() != null) {
            eq.setItemInMainHand(item(spec.mainHand()));
        }
        if (spec.offHand() != null) {
            eq.setItemInOffHand(item(spec.offHand()));
        }
        // Uniform drop chance for all slots (default 0 = gear is not farmable).
        float c = spec.dropChance();
        eq.setHelmetDropChance(c);
        eq.setChestplateDropChance(c);
        eq.setLeggingsDropChance(c);
        eq.setBootsDropChance(c);
        eq.setItemInMainHandDropChance(c);
        eq.setItemInOffHandDropChance(c);
    }

    private static ItemStack item(Material material) {
        return new ItemStack(material);
    }
}
