package net.leaf.artifacts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.leaf.curios.api.CurioItems;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Banner;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds artifact {@link ItemStack}s and reads the artifact type back off an
 * item via its {@code PersistentDataContainer}.
 *
 * <p>Items are marked with {@link ArtifactKeys#ARTIFACT_TYPE}. The chosen
 * {@link org.bukkit.Material} is the no-pack base item; each artifact also sets
 * the {@code minecraft:item_model} component to {@code leafartifacts:<id>} so a
 * loaded resource pack renders its dedicated sprite (see {@code resourcepack/}).</p>
 */
public final class ArtifactItem {

    /** Resource-pack namespace that holds the artifact item models/textures. */
    public static final String ARTIFACT_MODEL_NAMESPACE = "leafartifacts";

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

        // No custom item_model is set: the dedicated artifact sprites/textures do
        // not exist yet, so forcing leafartifacts:<id> would render as the purple
        // "missing texture" checkerboard. Until an original resource pack is
        // authored and delivered, artifacts deliberately fall back to their base
        // vanilla Material texture (type.material()). When the pack lands, restore:
        //   meta.setItemModel(new NamespacedKey(ARTIFACT_MODEL_NAMESPACE, type.id()));

        // The Umbrella is a colored shield: dye it like a banner with a white
        // base and red blades so it reads as a red-and-white pinwheel until a
        // dedicated resource-pack umbrella model replaces the shield material.
        if (type == ArtifactType.UMBRELLA) {
            applyUmbrellaPinwheel(meta);
        }

        item.setItemMeta(meta);
        // Tag the item with the LeafCurios slot ids it fits so it can be equipped
        // through the shared curios menu/API (no-op for slotless types like Eternal Steak).
        CurioItems.setSlots(item, type.slots());
        return item;
    }

    /**
     * Paints a shield {@link ItemMeta} as a red-and-white pinwheel using vanilla
     * banner patterns (shields are dyed exactly like banners). A white base with
     * two diagonally-opposite red blades gives the rotational "windmill" look.
     */
    private static void applyUmbrellaPinwheel(ItemMeta meta) {
        if (!(meta instanceof BlockStateMeta blockStateMeta)) {
            return;
        }
        if (!(blockStateMeta.getBlockState() instanceof Banner banner)) {
            return;
        }
        banner.setBaseColor(DyeColor.WHITE);
        banner.setPatterns(List.of(
                new Pattern(DyeColor.RED, PatternType.DIAGONAL_RIGHT),
                new Pattern(DyeColor.RED, PatternType.DIAGONAL_UP_LEFT)));
        banner.update();
        blockStateMeta.setBlockState(banner);
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
