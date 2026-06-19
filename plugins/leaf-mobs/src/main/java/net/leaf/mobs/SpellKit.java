package net.leaf.mobs;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Reusable, dependency-free combat "casts" shared by the magic and melee abilities (the
 * {@code SpellKit} from {@code BIOME-BOSS-MOBS-PLAN.md}). Each method performs one discrete
 * effect with only the Paper API.
 *
 * <p>Folia: every method must be called on the region thread owning {@code caster} (which
 * is the case from {@link MobController}'s entity-scheduler tick). The effects act at or
 * near the caster's location, i.e. inside the same region, so no further scheduling is
 * needed. None of these methods schedule delayed work, which keeps them region-safe.</p>
 */
public final class SpellKit {

    private SpellKit() {
    }

    /**
     * Spawns a straight line of {@link EvokerFangs} from the caster toward {@code target}.
     * Iconic evoker mechanic, usable by any caster.
     *
     * @param length number of fang steps (1 block apart).
     */
    public static void fangLine(LivingEntity caster, LivingEntity target, int length, float damage) {
        if (target == null) {
            return;
        }
        World world = caster.getWorld();
        Location origin = caster.getLocation();
        Vector dir = target.getLocation().toVector().subtract(origin.toVector());
        dir.setY(0);
        if (dir.lengthSquared() < 1.0e-4) {
            return;
        }
        dir.normalize();
        for (int i = 1; i <= Math.max(1, length); i++) {
            Location at = origin.clone().add(dir.clone().multiply(i));
            spawnFang(world, at, caster, damage);
        }
        world.playSound(origin, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.0f);
    }

    /**
     * Spawns a ring of {@link EvokerFangs} around the caster - a close-range "nova" that
     * punishes players standing on top of the boss.
     *
     * @param count  number of fangs in the ring.
     * @param radius ring radius in blocks.
     */
    public static void fangRing(LivingEntity caster, int count, double radius, float damage) {
        World world = caster.getWorld();
        Location center = caster.getLocation();
        int n = Math.max(1, count);
        for (int i = 0; i < n; i++) {
            double angle = (2 * Math.PI * i) / n;
            Location at = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            spawnFang(world, at, caster, damage);
        }
        world.playSound(center, Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.0f, 0.8f);
    }

    private static void spawnFang(World world, Location at, LivingEntity owner, float damage) {
        at.setYaw(owner.getLocation().getYaw());
        world.spawn(at, EvokerFangs.class, f -> {
            if (owner instanceof Mob) {
                f.setOwner(owner);
            }
        });
        // Paper's EvokerFangs API exposes no attack-damage setter; the fang deals its
        // vanilla damage. The 'damage' parameter is retained for call-site/API stability.
    }

    /**
     * Summons up to {@code count} adds of {@code type} around the caster, tagging each with
     * the owner's mob id so the lifecycle layer can recognise/cull them. Returns the number
     * actually spawned.
     */
    public static int summonAdds(LeafMobsPlugin plugin, LivingEntity caster, EntityType type,
                                 int count, double radius) {
        if (type.getEntityClass() == null || !LivingEntity.class.isAssignableFrom(type.getEntityClass())) {
            return 0;
        }
        World world = caster.getWorld();
        Location center = caster.getLocation();
        String ownerId = caster.getPersistentDataContainer()
                .get(plugin.keys().mobId(), PersistentDataType.STRING);
        int spawned = 0;
        for (int i = 0; i < Math.max(1, count); i++) {
            double angle = Math.random() * 2 * Math.PI;
            double dist = 1.0 + Math.random() * Math.max(0.5, radius);
            Location at = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            world.spawn(at, type.getEntityClass(), e -> {
                if (e instanceof LivingEntity living && ownerId != null) {
                    living.getPersistentDataContainer().set(
                            plugin.keys().addOwner(), PersistentDataType.STRING, ownerId);
                    living.setRemoveWhenFarAway(true);
                }
            });
            spawned++;
        }
        world.playSound(center, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 1.0f);
        return spawned;
    }

    /**
     * Throws a harmful splash effect at the target by applying it directly in a small area
     * (dependency-free, no thrown-potion item needed). Affects the target and anything near
     * its feet.
     */
    public static void potionVolley(LivingEntity caster, LivingEntity target,
                                    PotionEffectType effect, int durationTicks, int amplifier, double radius) {
        if (target == null || effect == null) {
            return;
        }
        World world = caster.getWorld();
        Location at = target.getLocation();
        for (LivingEntity victim : world.getNearbyLivingEntities(at, Math.max(0.5, radius))) {
            if (victim.equals(caster)) {
                continue;
            }
            victim.addPotionEffect(new PotionEffect(effect, durationTicks, amplifier, false, true));
        }
        world.spawnParticle(Particle.WITCH, at.clone().add(0, 1, 0), 30, 0.6, 0.8, 0.6, 0.0);
        world.playSound(at, Sound.ENTITY_WITCH_THROW, 1.0f, 1.0f);
    }

    /**
     * Damages and knocks back everything (except the caster) within {@code radius} - a
     * close-range melee shockwave for non-magic bosses.
     */
    public static void hazardZone(LivingEntity caster, double radius, double damage, double knockback) {
        World world = caster.getWorld();
        Location center = caster.getLocation();
        for (LivingEntity victim : world.getNearbyLivingEntities(center, Math.max(0.5, radius))) {
            if (victim.equals(caster)) {
                continue;
            }
            victim.damage(damage, caster);
            Vector push = victim.getLocation().toVector().subtract(center.toVector());
            push.setY(0);
            if (push.lengthSquared() > 1.0e-4) {
                push.normalize().multiply(knockback).setY(0.4);
                victim.setVelocity(victim.getVelocity().add(push));
            }
        }
        world.spawnParticle(Particle.EXPLOSION, center, 6, radius / 2, 0.3, radius / 2, 0.0);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
    }

    /** Launches the caster toward {@code target} (a leaping melee gap-closer). */
    public static void leap(LivingEntity caster, LivingEntity target, double power) {
        if (target == null) {
            return;
        }
        Vector dir = target.getLocation().toVector().subtract(caster.getLocation().toVector());
        if (dir.lengthSquared() < 1.0e-4) {
            return;
        }
        dir.normalize().multiply(power);
        dir.setY(Math.max(0.4, dir.getY() + 0.4));
        caster.setVelocity(dir);
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.2f);
    }

    /**
     * Anti-cheese gap-closer, themed as a <b>fishing-rod yank</b>: yanks {@code target} toward the
     * caster. This is the boss's answer to pillar/ledge cheese and long-range kiting - a player
     * perched on a tower or fleeing on the ground is reeled horizontally back toward the boss
     * (with a small upward "pop" so they clear a pillar edge and fall off it, taking the fall
     * themselves since only the boss is fall-immune). No-ops when the target is already within
     * {@code minDistance} blocks, so it only fires when the player is out of melee reach.
     *
     * <p>The presentation deliberately mirrors a PvP fishing-rod pull (cast + reel-in sounds, a
     * "line" of particles from the boss to the hooked target, and a splash on impact) - getting
     * rod-yanked by a boss reads as the same cheeky reaction players know from rod combat.</p>
     *
     * @param power       horizontal pull strength (blocks/tick of added velocity, roughly).
     * @param minDistance only pull when the target is at least this many blocks away.
     */
    public static void grapple(LivingEntity caster, LivingEntity target, double power, double minDistance) {
        if (target == null) {
            return;
        }
        Vector toCaster = caster.getLocation().toVector().subtract(target.getLocation().toVector());
        double dist = toCaster.length();
        if (dist < Math.max(0.0, minDistance) || dist < 1.0e-4) {
            return;
        }
        Vector pull = toCaster.clone().normalize().multiply(Math.max(0.1, power));
        // A small fixed upward kick dislodges a player standing on a pillar/ledge so the
        // horizontal pull can drag them off it; for ground kiters it is just a light hop.
        pull.setY(0.4);
        target.setVelocity(target.getVelocity().add(pull));
        World world = caster.getWorld();
        // Fishing-rod theme: cast + reel-in sounds bracket the yank.
        world.playSound(caster.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1.0f, 0.8f);
        world.playSound(caster.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.2f, 0.6f);
        // Draw a "fishing line" of particles from the boss's hand up to the hooked target so the
        // pull visibly reads as a rod reel-in rather than a generic tug.
        Location from = caster.getLocation().add(0, 1.2, 0);
        Location to = target.getLocation().add(0, 1.0, 0);
        Vector line = to.toVector().subtract(from.toVector());
        int steps = Math.max(4, (int) Math.round(line.length() * 2));
        for (int i = 0; i <= steps; i++) {
            Location p = from.clone().add(line.clone().multiply((double) i / steps));
            world.spawnParticle(Particle.CRIT, p, 1, 0.0, 0.0, 0.0, 0.0);
        }
        // Splash where the "hook" lands on the target, completing the fishing-rod gag.
        world.spawnParticle(Particle.SPLASH, to, 16, 0.3, 0.4, 0.3, 0.1);
    }

    /** Self-buff: applies a potion effect to the caster (e.g. enrage with STRENGTH/SPEED). */
    public static void selfBuff(LivingEntity caster, PotionEffectType effect, int durationTicks, int amplifier) {
        if (effect == null) {
            return;
        }
        caster.addPotionEffect(new PotionEffect(effect, durationTicks, amplifier, false, true));
        caster.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, caster.getLocation().add(0, 1.5, 0),
                10, 0.4, 0.4, 0.4, 0.0);
    }

    /**
     * Thorn-Pendant-themed boss boost: arms a timed melee-reflect buff on the caster. While it
     * lasts, {@link MobLifecycleListener} bounces {@code reflectFraction} of any melee damage the
     * mob takes back at the attacker - the boss echo of the Thorn Pendant it drops. The window
     * and fraction are stored in the mob's PDC so the buff survives a chunk reload mid-fight.
     */
    public static void thornsGuard(LeafMobsPlugin plugin, LivingEntity caster,
                                   int durationTicks, double reflectFraction) {
        long until = System.currentTimeMillis() + Math.max(1, durationTicks) * 50L;
        double pct = Math.max(0.0, Math.min(1.0, reflectFraction));
        caster.getPersistentDataContainer().set(
                plugin.keys().thornsUntil(), PersistentDataType.LONG, until);
        caster.getPersistentDataContainer().set(
                plugin.keys().thornsPct(), PersistentDataType.DOUBLE, pct);
        World world = caster.getWorld();
        world.spawnParticle(Particle.CRIT, caster.getLocation().add(0, 1.0, 0),
                24, 0.6, 0.8, 0.6, 0.1);
        world.playSound(caster.getLocation(), Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, 1.0f, 0.8f);
    }

    /**
     * Antidote-Vessel-themed boss boost: strips every harmful potion effect off the caster (and
     * grants a brief poison/wither immunity via Regeneration), mirroring the artifact the Blight
     * Warden drops - "negative status effects wear off faster". Pure self-cleanse, no target.
     */
    public static void cleanse(LivingEntity caster, int immunityTicks) {
        for (PotionEffect active : List.copyOf(caster.getActivePotionEffects())) {
            PotionEffectType type = active.getType();
            if (type.getCategory() == PotionEffectTypeCategory.HARMFUL) {
                caster.removePotionEffect(type);
            }
        }
        if (immunityTicks > 0) {
            caster.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION, immunityTicks, 0, false, true));
        }
        World world = caster.getWorld();
        world.spawnParticle(Particle.HAPPY_VILLAGER, caster.getLocation().add(0, 1.2, 0),
                16, 0.5, 0.6, 0.5, 0.0);
        world.playSound(caster.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.2f);
    }

    /** Blinks the caster a short distance behind the target (magic repositioning). */
    public static void teleportBlink(LivingEntity caster, LivingEntity target, double behindDistance) {
        if (target == null) {
            return;
        }
        Location dest = target.getLocation().clone()
                .add(target.getLocation().getDirection().normalize().multiply(-Math.max(0.5, behindDistance)));
        dest.setY(target.getLocation().getY());
        World world = caster.getWorld();
        world.spawnParticle(Particle.PORTAL, caster.getLocation().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.2);
        caster.teleport(dest);
        world.spawnParticle(Particle.PORTAL, dest.clone().add(0, 1, 0), 30, 0.4, 0.8, 0.4, 0.2);
        world.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }
}
