package net.leaf.artifacts;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles the "P2" event-reactive artifacts from
 * {@code docs/scratch/ARTIFACTS-VANILLA-RECREATION.md}: artifacts whose effect
 * is a one-shot reaction to a gameplay event rather than a passive modifier.
 *
 * <p>An artifact is considered active while at least one copy is carried in the
 * player's inventory (see {@link ArtifactItem#isCarriedBy}). All reactions act
 * on entities already involved in the triggering event, so they run on the
 * correct region thread under Folia without any extra scheduling.</p>
 */
public final class ArtifactEventListener implements Listener {

    /** Vampiric Glove: heal this fraction of melee damage dealt. */
    private static final double LIFESTEAL_FRACTION = 0.15D;
    /** Flame Pendant: ticks of fire applied to struck enemies (4s). */
    private static final int FLAME_PENDANT_FIRE_TICKS = 80;
    /** Fire Gauntlet: ticks of fire applied to struck enemies (5s). */
    private static final int FIRE_GAUNTLET_FIRE_TICKS = 100;
    /** Shock Pendant: chance per melee hit to call lightning. */
    private static final double SHOCK_CHANCE = 0.15D;
    /** Panic Necklace: Speed II for 5s when hurt. */
    private static final int PANIC_SPEED_TICKS = 100;
    private static final int PANIC_SPEED_AMPLIFIER = 1;
    /** Onion Ring: Haste II for 30s after eating. */
    private static final int ONION_HASTE_TICKS = 600;
    private static final int ONION_HASTE_AMPLIFIER = 1;
    /** Golden Hook: multiplier applied to dropped experience on a kill. */
    private static final double GOLDEN_HOOK_XP_MULTIPLIER = 1.5D;

    private final ArtifactEquipment equipment;

    public ArtifactEventListener(ArtifactEquipment equipment) {
        this.equipment = equipment;
    }

    /** Melee-hit reactions: lifesteal, ignite, and lightning. */
    @EventHandler(ignoreCancelled = true)
    public void onMeleeDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        if (equipment.carries(attacker, ArtifactType.VAMPIRIC_GLOVE)) {
            healAttacker(attacker, event.getFinalDamage() * LIFESTEAL_FRACTION);
        }

        int fireTicks = 0;
        if (equipment.carries(attacker, ArtifactType.FIRE_GAUNTLET)) {
            fireTicks = Math.max(fireTicks, FIRE_GAUNTLET_FIRE_TICKS);
        }
        if (equipment.carries(attacker, ArtifactType.FLAME_PENDANT)) {
            fireTicks = Math.max(fireTicks, FLAME_PENDANT_FIRE_TICKS);
        }
        if (fireTicks > 0) {
            victim.setFireTicks(Math.max(victim.getFireTicks(), fireTicks));
        }

        if (equipment.carries(attacker, ArtifactType.SHOCK_PENDANT)
                && ThreadLocalRandom.current().nextDouble() < SHOCK_CHANCE) {
            victim.getWorld().strikeLightning(victim.getLocation());
        }
    }

    private void healAttacker(Player attacker, double amount) {
        if (amount <= 0.0D) {
            return;
        }
        AttributeInstance maxHealth = attacker.getAttribute(Attribute.MAX_HEALTH);
        double cap = maxHealth != null ? maxHealth.getValue() : 20.0D;
        attacker.setHealth(Math.min(cap, attacker.getHealth() + amount));
    }

    /** Panic Necklace: Speed burst when the wearer takes damage. */
    @EventHandler(ignoreCancelled = true)
    public void onHurt(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && equipment.carries(player, ArtifactType.BUNNY_HOPPERS)) {
            event.setCancelled(true);
            return;
        }

        if (equipment.carries(player, ArtifactType.PANIC_NECKLACE)) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, PANIC_SPEED_TICKS, PANIC_SPEED_AMPLIFIER, true, true, true));
        }
    }

    /** Onion Ring: mobile Haste after eating. */
    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (event.getItem().getType().isEdible()
                && equipment.carries(player, ArtifactType.ONION_RING)) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.HASTE, ONION_HASTE_TICKS, ONION_HASTE_AMPLIFIER, true, true, true));
        }
    }

    /** Golden Hook: extra experience from creatures the wearer kills. */
    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null && equipment.carries(killer, ArtifactType.GOLDEN_HOOK)) {
            event.setDroppedExp((int) Math.round(event.getDroppedExp() * GOLDEN_HOOK_XP_MULTIPLIER));
        }
    }
}
