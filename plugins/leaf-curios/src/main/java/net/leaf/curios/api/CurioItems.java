package net.leaf.curios.api;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Helpers for marking an {@link ItemStack} as a "curio" and reading back which
 * slot type id(s) it may be equipped into.
 *
 * <p>This is the cross-plugin contract (the LeafCurios equivalent of Curios'
 * {@code ICurioItem}): any plugin can tag its own items with the slot ids they
 * fit, without depending on LeafCurios internals. The tag is stored in the
 * item's {@code PersistentDataContainer} under a stable namespaced key so it is
 * portable and survives serialization.</p>
 */
public final class CurioItems {

    /** Stable key (namespace {@code leafcurios}, key {@code slots}). */
    public static final NamespacedKey SLOTS_KEY = new NamespacedKey("leafcurios", "slots");

    private CurioItems() {
    }

    /**
     * Tags the item with the slot type ids it may be equipped into. Passing an
     * empty collection clears the tag (the item is no longer a curio).
     */
    public static ItemStack setSlots(ItemStack item, Collection<String> slotIds) {
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        if (slotIds == null || slotIds.isEmpty()) {
            meta.getPersistentDataContainer().remove(SLOTS_KEY);
        } else {
            meta.getPersistentDataContainer().set(
                    SLOTS_KEY, PersistentDataType.STRING, String.join(",", normalise(slotIds)));
        }
        item.setItemMeta(meta);
        return item;
    }

    /** The slot type ids this item declares it fits (empty if not a curio). */
    public static Set<String> getSlots(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Set.of();
        }
        ItemMeta meta = item.getItemMeta();
        String raw = meta.getPersistentDataContainer().get(SLOTS_KEY, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String id = part.trim().toLowerCase(Locale.ROOT);
            if (!id.isEmpty()) {
                out.add(id);
            }
        }
        return out;
    }

    /** Whether the item is a curio (declares at least one slot id). */
    public static boolean isCurio(ItemStack item) {
        return !getSlots(item).isEmpty();
    }

    /** Whether the item declares it fits the given slot type id. */
    public static boolean fitsSlot(ItemStack item, String slotId) {
        return slotId != null && getSlots(item).contains(slotId.toLowerCase(Locale.ROOT));
    }

    private static Set<String> normalise(Collection<String> slotIds) {
        Set<String> out = new LinkedHashSet<>();
        for (String id : slotIds) {
            if (id != null && !id.isBlank()) {
                out.add(id.trim().toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }
}
