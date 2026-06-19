package net.leaf.mobs;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * Drives the F2 lifecycle of managed mobs by reacting to entity load/unload/death. These
 * Paper events fire on the region thread that owns the entity, so the handlers may touch
 * it and the {@link MobManager} directly.
 *
 * <ul>
 *   <li>{@link EntityAddToWorldEvent} - the entity (re)entered a loaded world (fresh spawn,
 *       chunk load, or server restart): rebuild a controller from its PDC identity. This is
 *       what makes a boss "survive" a restart - identity is durable, the controller is
 *       rebuilt on load.</li>
 *   <li>{@link EntityRemoveFromWorldEvent} - chunk unload or removal: dispose the controller
 *       so the boss bar does not linger; it will be rebuilt if the entity loads again.</li>
 *   <li>{@link EntityDeathEvent} - apply F5 rewards (loot-table roll + advancement grant
 *       via {@link RewardService}), then dispose the controller.</li>
 * </ul>
 */
public final class MobLifecycleListener implements Listener {

    private final MobManager manager;
    private final RewardService rewards;

    public MobLifecycleListener(MobManager manager, RewardService rewards) {
        this.manager = manager;
        this.rewards = rewards;
    }

    @EventHandler
    public void onAdd(EntityAddToWorldEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            manager.reattach(living);
        }
    }

    @EventHandler
    public void onRemove(EntityRemoveFromWorldEvent event) {
        if (event.getEntity() instanceof LivingEntity) {
            manager.detach(event.getEntity().getUniqueId());
        }
    }

    /**
     * Damage handler for managed mobs. Two effects:
     * <ul>
     *   <li><b>Fall immunity</b> - the melee {@code leap}/{@code grapple} casts fling entities
     *       around, so cancelling FALL on our mobs keeps those a pure repositioning tool and
     *       stops a boss "hurting itself" on touchdown.</li>
     *   <li><b>Per-hit damage cap (bosses)</b> - a single attack can never delete more than
     *       {@link #BOSS_HIT_CAP_FRACTION} of a boss's max health. The fraction scales with each
     *       boss's own max health, so it works regardless of stat tuning. With ~6% per hit a
     *       fully-kitted player can no longer "bum rush" a boss on raw gear alone - they must land
     *       many hits, i.e. survive the boss's mechanics (grapple, shockwaves, fangs, adds) for
     *       long enough. Applied only to BOSS-tier managed mobs; elites/commons keep vanilla
     *       damage so they stay killable in a few hits.</li>
     * </ul>
     */
    private static final double BOSS_HIT_CAP_FRACTION = 0.06;

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        // Managed mobs ignore fall damage (the leap/grapple casts fling things around).
        if (event.getCause() == DamageCause.FALL) {
            if (manager.isManaged(living)) {
                event.setCancelled(true);
            }
            return;
        }
        // Bosses cap incoming damage per hit so they cannot be bursted down on gear alone.
        if (manager.isBossTier(living)) {
            AttributeInstance max = living.getAttribute(Attribute.MAX_HEALTH);
            double maxHealth = max != null ? max.getValue() : living.getHealth();
            double cap = maxHealth * BOSS_HIT_CAP_FRACTION;
            if (cap > 0.0 && event.getDamage() > cap) {
                event.setDamage(cap);
            }
        }
    }

    /**
     * Entity types that can fly/hover under their own power; for everything else a managed mob is
     * treated as "ground-only" and gets the arrow-resistance below. Kept as a small denylist so a
     * new ground reskin/boss is covered automatically without extra config.
     */
    private static final Set<EntityType> FLYING_TYPES = EnumSet.of(
            EntityType.BEE, EntityType.PARROT, EntityType.BAT, EntityType.ALLAY,
            EntityType.VEX, EntityType.PHANTOM, EntityType.GHAST, EntityType.BLAZE,
            EntityType.WITHER, EntityType.ENDER_DRAGON, EntityType.BREEZE);

    /**
     * Arrow resistance for ground-only managed mobs. A grounded elite/boss shrugs off ordinary
     * arrows so a player cannot plink it to death from safety with a starter bow - the fight has
     * to be earned up close. The single exception is <b>Piercing</b> arrows: an arrow whose
     * pierce level is &gt; 0 (the Piercing enchantment, or a piercing crossbow bolt) punches
     * through, so a player who has actually advanced enough to enchant Piercing is rewarded with
     * a working ranged option. Tridents are exempt (a thrown trident is its own "advanced" tool)
     * and flying mobs keep vanilla behaviour (arrows are the intended counter to them).
     *
     * <p>Runs at {@link EventPriority#LOW} so the resist/cancel happens before the thorns-reflect
     * handler, and a deflected arrow neither reflects nor deals damage.</p>
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onArrowResist(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim) || !manager.isManaged(victim)) {
            return;
        }
        if (!(event.getDamager() instanceof AbstractArrow arrow) || arrow instanceof Trident) {
            return;
        }
        if (FLYING_TYPES.contains(victim.getType())) {
            return; // only ground-only mobs resist arrows
        }
        if (arrow.getPierceLevel() > 0) {
            return; // Piercing arrows bypass the resistance - the player has advanced
        }
        // Deflect the ordinary arrow: cancel the hit and give a clear "clink" of feedback.
        event.setCancelled(true);
        victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1.0, 0),
                8, 0.3, 0.4, 0.3, 0.0);
        victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.8f, 1.4f);
    }

    /**
     * Thorn-Pendant-themed reflect: while a managed mob's {@code thorns_guard} boost is active it
     * bounces a fraction of the melee damage it takes back at the attacker (the boss echo of the
     * Thorn Pendant it drops). Only direct living attackers are reflected, never the mob itself,
     * and the reflected hit cannot re-trigger this handler (it is not melee-by-our-mob).
     */
    @EventHandler(ignoreCancelled = true)
    public void onAttacked(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim) || !manager.isManaged(victim)) {
            return;
        }
        double reflect = manager.thornsReflect(victim);
        if (reflect <= 0.0) {
            return;
        }
        Entity damager = event.getDamager();
        if (damager instanceof LivingEntity attacker && !attacker.equals(victim)) {
            double back = event.getFinalDamage() * reflect;
            if (back > 0.0) {
                attacker.damage(back, victim);
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        rewards.onDeath(event);
        manager.detach(event.getEntity().getUniqueId());
    }
}
