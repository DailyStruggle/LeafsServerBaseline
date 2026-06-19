package net.leaf.mobs;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Maps the {@code type} string used in {@code mobs.yml} to a concrete {@link Ability}
 * implementation. This is the F3 "register a mechanic by name" point: built-in casts (the
 * {@link SpellKit}) are registered here, and a brand-new mechanic is one
 * {@link #register} call plus a class - no change to the controller or the data model.
 *
 * <p>All built-ins are stateless lambdas reading their tuning from {@link AbilityContext}.
 * The registry itself is consulted at controller-build time, so unknown types are reported
 * once (with a warning) rather than every tick.</p>
 */
public final class AbilityRegistry {

    private final Logger log;
    private final Map<String, Ability> abilities = new HashMap<>();

    public AbilityRegistry(Logger log) {
        this.log = log;
        registerBuiltins();
    }

    /** Registers (or overrides) an ability under {@code type} (case-insensitive). */
    public void register(String type, Ability ability) {
        abilities.put(type.toLowerCase(), ability);
    }

    /** Resolves an ability by type, or {@code null} if none is registered. */
    public Ability get(String type) {
        return type == null ? null : abilities.get(type.toLowerCase());
    }

    public Set<String> types() {
        return abilities.keySet();
    }

    private void registerBuiltins() {
        // --- Magic casts ---------------------------------------------------------------
        register("fang_line", ctx -> SpellKit.fangLine(
                ctx.self(), ctx.target(),
                ctx.param("length", 8),
                (float) ctx.param("damage", 6.0)));

        register("fang_ring", ctx -> SpellKit.fangRing(
                ctx.self(),
                ctx.param("count", 10),
                ctx.param("radius", 3.0),
                (float) ctx.param("damage", 6.0)));

        register("potion_volley", ctx -> SpellKit.potionVolley(
                ctx.self(), ctx.target(),
                effect(ctx.param("effect", "minecraft:slowness")),
                (int) Math.round(ctx.param("duration_s", 6.0) * 20),
                ctx.param("amplifier", 0),
                ctx.param("radius", 3.0)));

        register("teleport_blink", ctx -> SpellKit.teleportBlink(
                ctx.self(), ctx.target(),
                ctx.param("distance", 2.0)));

        register("summon_adds", ctx -> SpellKit.summonAdds(
                ctx.plugin(), ctx.self(),
                entityType(ctx.param("entity", "VEX")),
                ctx.param("count", 3),
                ctx.param("radius", 3.0)));

        // --- Melee / physical casts ----------------------------------------------------
        register("leap", ctx -> SpellKit.leap(
                ctx.self(), ctx.target(),
                ctx.param("power", 1.4)));

        register("shockwave", ctx -> SpellKit.hazardZone(
                ctx.self(),
                ctx.param("radius", 4.0),
                ctx.param("damage", 6.0),
                ctx.param("knockback", 1.2)));

        register("enrage", ctx -> SpellKit.selfBuff(
                ctx.self(),
                effect(ctx.param("effect", "minecraft:strength")),
                (int) Math.round(ctx.param("duration_s", 8.0) * 20),
                ctx.param("amplifier", 1)));

        // grapple: anti-cheese gap-closer - yanks a far/pillaring/kiting target back toward
        // the boss so it cannot be cheesed from a tower or by running away (see SpellKit).
        register("grapple", ctx -> SpellKit.grapple(
                ctx.self(), ctx.target(),
                ctx.param("power", 1.2),
                ctx.param("min_distance", 5.0)));

        // --- Artifact-themed boss boosts (mirror the trinket the boss drops) -----------
        // thorns_guard: Thorn Pendant echo - a timed melee-reflect window (see SpellKit).
        register("thorns_guard", ctx -> SpellKit.thornsGuard(
                ctx.plugin(), ctx.self(),
                (int) Math.round(ctx.param("duration_s", 8.0) * 20),
                ctx.param("reflect", 0.3)));

        // cleanse: Antidote Vessel echo - strips the caster's own harmful effects.
        register("cleanse", ctx -> SpellKit.cleanse(
                ctx.self(),
                (int) Math.round(ctx.param("immunity_s", 4.0) * 20)));
    }

    private PotionEffectType effect(String raw) {
        if (raw == null) {
            return null;
        }
        NamespacedKey key = NamespacedKey.fromString(raw.trim().toLowerCase());
        PotionEffectType type = key == null ? null : Registry.EFFECT.get(key);
        if (type == null) {
            log.warning("[leaf-mobs] unknown potion effect '" + raw + "'; ability will skip it.");
        }
        return type;
    }

    private EntityType entityType(String raw) {
        if (raw == null) {
            return EntityType.VEX;
        }
        try {
            return EntityType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warning("[leaf-mobs] unknown summon entity '" + raw + "'; defaulting to VEX.");
            return EntityType.VEX;
        }
    }
}
