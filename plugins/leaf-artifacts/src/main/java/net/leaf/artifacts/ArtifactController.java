package net.leaf.artifacts;

import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
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
    }

    /** Removes all artifact-applied effects/modifiers for a leaving player. */
    public void clear(Player player) {
        for (ArtifactType type : ArtifactType.values()) {
            for (AttributeSpec spec : type.attributes()) {
                AttributeInstance inst = player.getAttribute(spec.attribute());
                if (inst == null) {
                    continue;
                }
                AttributeModifier existing = findModifier(inst, ArtifactKeys.modifierKey(type, spec.attribute()));
                if (existing != null) {
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
                if (desired && existing == null) {
                    inst.addModifier(new AttributeModifier(
                            key, spec.amount(), spec.operation(), EquipmentSlotGroup.ANY));
                } else if (!desired && existing != null) {
                    inst.removeModifier(existing);
                }
            }
        }
    }

    private void reconcileEffects(Player player, Set<ArtifactType> active) {
        Set<PotionEffectType> desired = new HashSet<>();
        for (ArtifactType type : active) {
            desired.addAll(type.effects());
        }

        Set<PotionEffectType> owned = appliedEffects.computeIfAbsent(
                player.getUniqueId(), id -> Collections.synchronizedSet(new HashSet<>()));

        for (ArtifactType type : ArtifactType.values()) {
            for (PotionEffectType effect : type.effects()) {
                boolean wanted = desired.contains(effect);
                if (wanted) {
                    // Respect an externally-applied effect we do not own.
                    if (!owned.contains(effect) && player.hasPotionEffect(effect)) {
                        continue;
                    }
                    if (!player.hasPotionEffect(effect)) {
                        player.addPotionEffect(new PotionEffect(
                                effect, PotionEffect.INFINITE_DURATION, 0, true, false, false));
                    }
                    owned.add(effect);
                } else if (owned.contains(effect)) {
                    player.removePotionEffect(effect);
                    owned.remove(effect);
                }
            }
        }
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
