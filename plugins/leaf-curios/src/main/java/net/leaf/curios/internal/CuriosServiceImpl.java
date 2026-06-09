package net.leaf.curios.internal;

import net.leaf.curios.api.CurioItems;
import net.leaf.curios.api.CuriosApi;
import net.leaf.curios.api.SlotType;
import net.leaf.curios.api.event.CurioEquipEvent;
import net.leaf.curios.api.event.CurioUnequipEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Default {@link CuriosApi} implementation backing the menu, registry and store.
 *
 * <p>The "active" set generically includes both curios slotted in the menu and
 * any curio-tagged item worn in a vanilla armour slot, so wearing an artifact as
 * real armour activates it without any consumer-specific code.</p>
 */
public final class CuriosServiceImpl implements CuriosApi {

    private final SlotRegistry registry;
    private final CurioStorage storage;
    private final CuriosMenu menu;

    public CuriosServiceImpl(SlotRegistry registry, CurioStorage storage, CuriosMenu menu) {
        this.registry = registry;
        this.storage = storage;
        this.menu = menu;
    }

    @Override
    public void registerSlotType(SlotType type) {
        registry.register(type);
    }

    @Override
    public boolean unregisterSlotType(String id) {
        return registry.unregister(id);
    }

    @Override
    public Optional<SlotType> getSlotType(String id) {
        return registry.get(id);
    }

    @Override
    public List<SlotType> getSlotTypes() {
        return registry.all();
    }

    @Override
    public ItemStack[] getEquipped(Player player, String slotId) {
        ItemStack[] arr = storage.array(player, slotId);
        ItemStack[] copy = new ItemStack[arr.length];
        for (int i = 0; i < arr.length; i++) {
            copy[i] = arr[i] == null ? null : arr[i].clone();
        }
        return copy;
    }

    @Override
    public void setEquipped(Player player, String slotId, int index, ItemStack item) {
        ItemStack previous = storage.getItem(player, slotId, index);
        storage.set(player, slotId, index, item);
        if (previous != null) {
            Bukkit.getPluginManager().callEvent(new CurioUnequipEvent(player, slotId, index, previous));
        }
        if (item != null && !item.getType().isAir()) {
            Bukkit.getPluginManager().callEvent(new CurioEquipEvent(player, slotId, index, item.clone()));
        }
    }

    @Override
    public List<ItemStack> getEquippedItems(Player player) {
        List<ItemStack> out = storage.allItems(player);
        for (ItemStack worn : player.getInventory().getArmorContents()) {
            if (worn != null && CurioItems.isCurio(worn)) {
                out.add(worn.clone());
            }
        }
        return out;
    }

    @Override
    public boolean isEquipped(Player player, Predicate<ItemStack> predicate) {
        for (ItemStack item : getEquippedItems(player)) {
            if (predicate.test(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void openMenu(Player player) {
        menu.open(player);
    }
}
