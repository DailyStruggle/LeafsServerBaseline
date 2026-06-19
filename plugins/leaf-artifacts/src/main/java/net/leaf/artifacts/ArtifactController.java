package net.leaf.artifacts;

import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reconciles a player's active attribute modifiers and infinite potion effects
 * to exactly match the artifacts they are currently carrying.
 *
 * <p>The operation is idempotent: every call recomputes the desired state from
 * the inventory and adds/removes only what differs. All access touches a single
 * player, so it is safe to drive from that player's entity scheduler under
 * Folia.</p>
 */
public final class ArtifactController {

    /** Per-player set of potion effect types this plugin currently owns. */
    private final Map<UUID, Set<PotionEffectType>> appliedEffects = new ConcurrentHashMap<>();

    private final ArtifactEquipment equipment;

    public ArtifactController(ArtifactEquipment equipment) {
        this.equipment = equipment;
    }

    /** Recomputes and applies the artifact state for the given player. */
    public void reconcile(Player player) {
        Set<ArtifactType> active = activeArtifacts(player);
        reconcileAttributes(player, active);
        reconcileEffects(player, active);
        reconcileFreeze(player, active);
    }

    /**
     * Frostward Charm: keep the wearer thawed. Vanilla powder-snow freezing is a
     * hardcoded counter (not a potion effect), so the only way to make a carrier
     * immune is to drive the freeze counter back to zero each reconcile tick -
     * before it ever reaches the slow/damage threshold.
     */
    private void reconcileFreeze(Player player, Set<ArtifactType> active) {
        if (active.contains(ArtifactType.FROSTWARD_CHARM) && player.getFreezeTicks() > 0) {
            player.setFreezeTicks(0);
        }
    }

    /** Removes all artifact-applied effects/modifiers for a leaving player. */
    public void clear(Player player) {
        for (ArtifactType type : ArtifactType.values()) {
            for (AttributeSpec spec : type.attributes()) {
                AttributeInstance inst = player.getAttribute(spec.attribute());
                if (inst == null) {
                    continue;
                }
                var key = ArtifactKeys.modifierKey(type, spec.attribute());
                AttributeModifier existing;
                while ((existing = findModifier(inst, key)) != null) {
                    inst.removeModifier(existing);
                }
            }
        }
        Set<PotionEffectType> owned = appliedEffects.remove(player.getUniqueId());
        if (owned != null) {
            for (PotionEffectType effect : owned) {
                player.removePotionEffect(effect);
            }
        }
    }

    public void forget(UUID playerId) {
        appliedEffects.remove(playerId);
    }

    private Set<ArtifactType> activeArtifacts(Player player) {
        return equipment.activeTypes(player);
    }

    private void reconcileAttributes(Player player, Set<ArtifactType> active) {
        for (ArtifactType type : ArtifactType.values()) {
            boolean desired = active.contains(type);
            for (AttributeSpec spec : type.attributes()) {
                AttributeInstance inst = player.getAttribute(spec.attribute());
                if (inst == null) {
                    continue;
                }
                var key = ArtifactKeys.modifierKey(type, spec.attribute());
                AttributeModifier existing = findModifier(inst, key);
                if (desired) {
                    // Re-apply when missing, or when a persisted modifier carries a
                    // stale amount/operation (e.g. after an artifact's value was
                    // retuned between builds). Without the refresh a stale NBT
                    // modifier looks "present" and the live value never updates
                    // until the player manually re-equips.
                    boolean stale = existing != null
                            && (existing.getAmount() != spec.amount()
                            || existing.getOperation() != spec.operation());
                    if (existing == null || stale) {
                        if (existing != null) {
                            inst.removeModifier(existing);
                        }
                        inst.addModifier(new AttributeModifier(
                                key, spec.amount(), spec.operation(), EquipmentSlotGroup.ANY));
                    }
                } else if (existing != null) {
                    inst.removeModifier(existing);
                }
            }
        }
    }

    private void reconcileEffects(Player player, Set<ArtifactType> active) {
        // Highest amplifier requested per effect type across the active set, so
        // two artifacts asking for the same effect collapse to the strongest.
        Map<PotionEffectType, Integer> desired = new HashMap<>();
        for (ArtifactType type : active) {
            for (EffectSpec spec : type.effects()) {
                desired.merge(spec.type(), spec.amplifier(), Math::max);
            }
        }

        Set<PotionEffectType> owned = appliedEffects.computeIfAbsent(
                player.getUniqueId(), id -> Collections.synchronizedSet(new HashSet<>()));

        for (PotionEffectType effect : MANAGED_EFFECTS) {
            Integer wantAmplifier = desired.get(effect);
            if (wantAmplifier != null) {
                // Additive "buff a pre-existing effect" path: the artifact's level
                // stacks on top of whatever the player already has from another
                // source (a beacon, a potion), the way enchantment levels add - so
                // a beacon's Haste II (amplifier 1) plus the claws' Haste II
                // (amplifier 1) yields Haste III (amplifier 2). Because vanilla
                // keeps only the strongest instance of an effect, our own infinite
                // effect would otherwise mask the external source; so we first
                // remove ours to sample the external amplifier, then re-apply at
                // the sum.
                if (owned.contains(effect)) {
                    player.removePotionEffect(effect);
                }
                PotionEffect external = player.getPotionEffect(effect);
                int externalAmplifier = external != null ? external.getAmplifier() : 0;
                int total = externalAmplifier + wantAmplifier;
                player.addPotionEffect(new PotionEffect(
                        effect, PotionEffect.INFINITE_DURATION, total, true, false, false));
                owned.add(effect);
            } else if (owned.contains(effect)) {
                player.removePotionEffect(effect);
                owned.remove(effect);
            }
        }
    }

    /** Every potion effect type any artifact can grant; computed once. */
    private static final Set<PotionEffectType> MANAGED_EFFECTS = collectManagedEffects();

    private static Set<PotionEffectType> collectManagedEffects() {
        Set<PotionEffectType> all = new HashSet<>();
        for (ArtifactType type : ArtifactType.values()) {
            for (EffectSpec spec : type.effects()) {
                all.add(spec.type());
            }
        }
        return all;
    }

    private static AttributeModifier findModifier(AttributeInstance inst, org.bukkit.NamespacedKey key) {
        for (AttributeModifier modifier : inst.getModifiers()) {
            if (modifier.getKey().equals(key)) {
                return modifier;
            }
        }
        return null;
    }
}
