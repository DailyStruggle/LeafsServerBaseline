package net.leaf.artifacts;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    /** Superstitious Hat: per-drop chance to yield one extra item (Looting-like). */
    private static final double SUPERSTITIOUS_EXTRA_CHANCE = 0.5D;
    /** Anglers Hat: ticks shaved off the fishing wait window (Lure-like). */
    private static final int ANGLERS_LURE_TICKS = 100;
    private static final int ANGLERS_MIN_WAIT_TICKS = 20;
    /** Anglers Hat: chance to double a reeled-in catch (Luck-of-the-Sea-like). */
    private static final double ANGLERS_DOUBLE_CATCH_CHANCE = 0.35D;
    /** Cross Necklace: extended invulnerability window in ticks (vanilla 20). */
    private static final int CROSS_INVULN_TICKS = 30;
    private static final int DEFAULT_INVULN_TICKS = 20;
    /** Antidote Vessel: harmful effects keep this fraction of their duration. */
    private static final double ANTIDOTE_DURATION_FACTOR = 0.5D;
    /** Thorn Pendant: fraction of incoming melee damage reflected back. */
    private static final double THORN_REFLECT_FRACTION = 0.25D;
    /** Eternal Steak: nourishment applied per (non-consuming) bite. */
    private static final int ETERNAL_STEAK_FOOD = 8;
    private static final float ETERNAL_STEAK_SATURATION = 12.8F;

    /** Guards Antidote Vessel re-application against its own re-entrant event. */
    private final Set<UUID> antidoteGuard = ConcurrentHashMap.newKeySet();

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

        // Cross Necklace: lengthen the i-frame window (normalised every hit so it
        // reverts to vanilla once the charm is removed).
        player.setMaximumNoDamageTicks(equipment.carries(player, ArtifactType.CROSS_NECKLACE)
                ? CROSS_INVULN_TICKS : DEFAULT_INVULN_TICKS);

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

    /** Onion Ring: mobile Haste after eating; Eternal Steak: feed without being consumed. */
    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack consumed = event.getItem();

        // Eternal Steak: it is a steak you can eat repeatedly. Cancel the vanilla
        // consume (which would shrink the stack) and apply its nourishment by hand
        // so the item is never used up.
        if (ArtifactItem.typeOf(consumed).filter(t -> t == ArtifactType.ETERNAL_STEAK).isPresent()) {
            event.setCancelled(true);
            int food = Math.min(20, player.getFoodLevel() + ETERNAL_STEAK_FOOD);
            player.setFoodLevel(food);
            player.setSaturation(Math.min(food, player.getSaturation() + ETERNAL_STEAK_SATURATION));
            return;
        }

        if (consumed.getType().isEdible()
                && equipment.carries(player, ArtifactType.ONION_RING)) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.HASTE, ONION_HASTE_TICKS, ONION_HASTE_AMPLIFIER, true, true, true));
        }
    }

    /** Thorn Pendant: reflect a fraction of melee damage back at the attacker. */
    @EventHandler(ignoreCancelled = true)
    public void onReflect(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!(event.getDamager() instanceof LivingEntity attacker) || attacker.equals(victim)) {
            return;
        }
        if (!equipment.carries(victim, ArtifactType.THORN_PENDANT)) {
            return;
        }
        double reflect = event.getFinalDamage() * THORN_REFLECT_FRACTION;
        if (reflect > 0.0D) {
            attacker.damage(reflect, victim);
        }
    }

    /** Antidote Vessel: harmful status effects expire faster. */
    @EventHandler(ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null
                || newEffect.getType().getCategory() != PotionEffectTypeCategory.HARMFUL) {
            return;
        }
        if (antidoteGuard.contains(player.getUniqueId())
                || !equipment.carries(player, ArtifactType.ANTIDOTE_VESSEL)) {
            return;
        }
        int reduced = Math.max(1, (int) Math.round(newEffect.getDuration() * ANTIDOTE_DURATION_FACTOR));
        if (reduced >= newEffect.getDuration()) {
            return;
        }
        // Re-apply the effect with a shortened duration; cancel the original add and
        // guard against our own re-entrant ADDED event.
        event.setCancelled(true);
        UUID id = player.getUniqueId();
        antidoteGuard.add(id);
        try {
            player.addPotionEffect(new PotionEffect(newEffect.getType(), reduced, newEffect.getAmplifier(),
                    newEffect.isAmbient(), newEffect.hasParticles(), newEffect.hasIcon()));
        } finally {
            antidoteGuard.remove(id);
        }
    }

    /**
     * Most artifact placeholder materials are otherwise placeable blocks,
     * throwables, fillable bottles, etc. Deny the interaction for those so an
     * artifact cannot be planted/placed/used. Exceptions: Eternal Steak (eaten)
     * and armour-type artifacts (allowed so they can be worn in an armour slot).
     */
    @EventHandler(ignoreCancelled = true)
    public void onArtifactInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        var type = ArtifactItem.typeOf(item);
        if (type.isEmpty() || type.get() == ArtifactType.ETERNAL_STEAK) {
            return;
        }
        // Armour-type artifacts are meant to be worn: let the right-click equip
        // them into the vanilla armour slot. Everything else has no in-world use.
        if (item != null && ArtifactItem.isArmorMaterial(item.getType())) {
            return;
        }
        event.setCancelled(true);
    }

    /**
     * Allow leather artifacts to be dyed: the vanilla dye recipe strips custom
     * data, so re-stamp the artifact's identity (marker, name, lore, unbreakable)
     * onto the result while keeping the newly computed colour.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack result = inv.getResult();
        if (result == null || !(result.getItemMeta() instanceof LeatherArmorMeta dyedMeta)) {
            return;
        }
        ItemStack artifactInput = null;
        for (ItemStack matrixItem : inv.getMatrix()) {
            if (matrixItem != null && ArtifactItem.typeOf(matrixItem).isPresent()) {
                artifactInput = matrixItem;
                break;
            }
        }
        if (artifactInput == null) {
            return;
        }
        ItemStack preserved = artifactInput.clone();
        preserved.setAmount(result.getAmount());
        if (preserved.getItemMeta() instanceof LeatherArmorMeta artMeta) {
            artMeta.setColor(dyedMeta.getColor());
            preserved.setItemMeta(artMeta);
        }
        inv.setResult(preserved);
    }

    /** Safety net: never let an artifact item be placed as a block. */
    @EventHandler(ignoreCancelled = true)
    public void onArtifactPlace(BlockPlaceEvent event) {
        if (ArtifactItem.typeOf(event.getItemInHand()).isPresent()) {
            event.setCancelled(true);
        }
    }

    /**
     * Golden Hook: extra experience from creatures the wearer kills.
     * Superstitious Hat: a Looting-style boost to the mob's loot.
     */
    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        if (equipment.carries(killer, ArtifactType.GOLDEN_HOOK)) {
            event.setDroppedExp((int) Math.round(event.getDroppedExp() * GOLDEN_HOOK_XP_MULTIPLIER));
        }
        if (equipment.carries(killer, ArtifactType.SUPERSTITIOUS_HAT)) {
            // Vanilla has no "carried looting" hook, so emulate it: each loot stack
            // has a chance to drop one extra item, mirroring how Looting raises the
            // upper bound of mob drops.
            for (ItemStack drop : event.getDrops()) {
                if (drop == null || drop.getType().isAir()) {
                    continue;
                }
                if (ThreadLocalRandom.current().nextDouble() < SUPERSTITIOUS_EXTRA_CHANCE) {
                    drop.setAmount(drop.getAmount() + 1);
                }
            }
        }
    }

    /**
     * Anglers Hat: faster bites (Lure) and better catches (Luck of the Sea).
     * On cast, the hook's wait window is shortened; on a successful catch the
     * reeled-in item has a chance to be doubled.
     */
    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (!equipment.carries(player, ArtifactType.ANGLERS_HAT)) {
            return;
        }
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            FishHook hook = event.getHook();
            hook.setMinWaitTime(Math.max(ANGLERS_MIN_WAIT_TICKS,
                    hook.getMinWaitTime() - ANGLERS_LURE_TICKS));
            hook.setMaxWaitTime(Math.max(ANGLERS_MIN_WAIT_TICKS + ANGLERS_MIN_WAIT_TICKS,
                    hook.getMaxWaitTime() - ANGLERS_LURE_TICKS));
        } else if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH
                && event.getCaught() instanceof Item caught
                && ThreadLocalRandom.current().nextDouble() < ANGLERS_DOUBLE_CATCH_CHANCE) {
            ItemStack stack = caught.getItemStack();
            stack.setAmount(stack.getAmount() + 1);
            caught.setItemStack(stack);
        }
    }
}
