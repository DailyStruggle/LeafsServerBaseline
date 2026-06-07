package net.leaf.artifacts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        String slots = type.slots().stream()
                .map(CurioSlot::displayName)
                .collect(Collectors.joining(" / "));
        lore.add(Component.text("Slot: " + slots, NamedTextColor.DARK_AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Artifact", NamedTextColor.DARK_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(
                ArtifactKeys.ARTIFACT_TYPE, PersistentDataType.STRING, type.id());

        item.setItemMeta(meta);
        return item;
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
