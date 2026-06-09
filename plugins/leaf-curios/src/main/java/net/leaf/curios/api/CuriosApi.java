package net.leaf.curios.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Public service for the LeafCurios trinket/accessory system.
 *
 * <p>Obtain it via {@link CuriosProvider#get()} (or the Bukkit
 * {@code ServicesManager}). Other plugins use it to register their own slot
 * types, mark items as curios (see {@link CurioItems}), inspect or change a
 * player's equipped curios, and open the shared equipment menu.</p>
 *
 * <p>All per-player methods must be called on the thread that owns the player
 * (the player's region thread under Folia); event handlers already run there.</p>
 */
public interface CuriosApi {

    /** Registers (or replaces) a slot type. The menu layout follows registration order. */
    void registerSlotType(SlotType type);

    /** Removes a slot type by id; returns true if one was removed. */
    boolean unregisterSlotType(String id);

    /** The slot type with the given id, if registered. */
    Optional<SlotType> getSlotType(String id);

    /** All registered slot types, in registration (menu) order. */
    List<SlotType> getSlotTypes();

    /** The items equipped in the given slot type (array length is the slot's size; entries may be null). */
    ItemStack[] getEquipped(Player player, String slotId);

    /** Sets (or clears, when {@code item} is null) the item in a slot copy and fires the equip/unequip event. */
    void setEquipped(Player player, String slotId, int index, ItemStack item);

    /**
     * Every curio currently active for the player: those equipped in the curios
     * menu plus any curio-tagged item worn in a vanilla armour slot. Never null.
     */
    List<ItemStack> getEquippedItems(Player player);

    /** Whether the player has any active curio matching the predicate. */
    boolean isEquipped(Player player, Predicate<ItemStack> predicate);

    /** Opens the shared curios equipment menu for the player. */
    void openMenu(Player player);
}
