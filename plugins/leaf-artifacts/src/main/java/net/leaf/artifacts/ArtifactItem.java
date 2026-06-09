package net.leaf.artifacts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.leaf.curios.api.CurioItems;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds artifact {@link ItemStack}s and reads the artifact type back off an
 * item via its {@code PersistentDataContainer}.
 *
 * <p>Items are marked with {@link ArtifactKeys#ARTIFACT_TYPE}; the chosen
 * {@link org.bukkit.Material} is only a placeholder until a resource pack with
 * custom models is added.</p>
 */
public final class ArtifactItem {

    private ArtifactItem() {
    }

    public static ItemStack create(ArtifactType type) {
        ItemStack item = new ItemStack(type.material());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(Component.text(type.displayName(), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(type.description(), NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        if (type.slots().isEmpty()) {
            lore.add(Component.text("Consumable", NamedTextColor.DARK_AQUA)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            String slots = ArtifactSlotDefs.displayList(type.slots());
            lore.add(Component.text("Slot: " + slots, NamedTextColor.DARK_AQUA)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Artifact", NamedTextColor.DARK_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        // Artifacts never wear out: many can be worn as real armour, where they
        // would otherwise take durability damage.
        meta.setUnbreakable(true);

        meta.getPersistentDataContainer().set(
                ArtifactKeys.ARTIFACT_TYPE, PersistentDataType.STRING, type.id());

        item.setItemMeta(meta);
        // Tag the item with the LeafCurios slot ids it fits so it can be equipped
        // through the shared curios menu/API (no-op for slotless types like Eternal Steak).
        CurioItems.setSlots(item, type.slots());
        return item;
    }

    /**
     * Whether the given material is wearable armour (helmet/chestplate/leggings/
     * boots or a special head slot item). Used to allow armour-type artifacts to
     * be equipped into the vanilla armour slots while other placeholder materials
     * have all in-world interaction denied.
     */
    public static boolean isArmorMaterial(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")) {
            return true;
        }
        return switch (material) {
            case TURTLE_HELMET, CARVED_PUMPKIN, ELYTRA -> true;
            default -> false;
        };
    }

    /** Returns the artifact type carried by this item, if any. */
    public static Optional<ArtifactType> typeOf(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        String id = meta.getPersistentDataContainer()
                .get(ArtifactKeys.ARTIFACT_TYPE, PersistentDataType.STRING);
        return ArtifactType.fromId(id);
    }
}
