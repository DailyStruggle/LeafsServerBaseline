package net.leaf.curios.internal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.leaf.curios.api.SlotType;
import net.leaf.curios.api.event.CurioEquipEvent;
import net.leaf.curios.api.event.CurioUnequipEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The shared vanilla-UI curios menu, rendered dynamically from the registered
 * {@link SlotType}s. Each slot type contributes {@code size} cells, laid out in
 * registration order; empty cells show the slot's icon as a placeholder naming
 * the category. Equipping/unequipping mutates {@link CurioStorage} and fires the
 * public equip/unequip events.
 */
public final class CuriosMenu implements Listener {

    private static final Component TITLE = Component.text("Curios", NamedTextColor.DARK_PURPLE);
    private static final int MAX_SLOTS = 54;

    private final SlotRegistry registry;
    private final CurioStorage storage;

    public CuriosMenu(SlotRegistry registry, CurioStorage storage) {
        this.registry = registry;
        this.storage = storage;
    }

    /** A single menu cell: which slot type id and which copy within it. */
    private record Cell(String slotId, int index) {
    }

    public void open(Player player) {
        List<Cell> layout = buildLayout();
        int size = Math.min(MAX_SLOTS, Math.max(9, ((layout.size() + 8) / 9) * 9));
        Holder holder = new Holder(layout);
        Inventory inv = Bukkit.createInventory(holder, size, TITLE);
        holder.inventory = inv;
        for (int i = 0; i < layout.size() && i < size; i++) {
            Cell cell = layout.get(i);
            ItemStack equipped = storage.getItem(player, cell.slotId(), cell.index());
            inv.setItem(i, equipped != null ? equipped : placeholder(cell.slotId()));
        }
        player.openInventory(inv);
    }

    private List<Cell> buildLayout() {
        List<Cell> layout = new ArrayList<>();
        for (SlotType type : registry.all()) {
            for (int i = 0; i < type.size(); i++) {
                layout.add(new Cell(type.id(), i));
            }
        }
        return layout;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        boolean clickedTop = event.getClickedInventory() == top;

        if (clickedTop) {
            event.setCancelled(true);
            handleTopClick(event, player, top, holder);
            return;
        }
        if (event.isShiftClick()) {
            event.setCancelled(true);
            handleShiftEquip(event, player, top, holder);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) {
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        for (int raw : event.getRawSlots()) {
            if (raw < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void handleTopClick(InventoryClickEvent event, Player player, Inventory top, Holder holder) {
        int index = event.getRawSlot();
        if (index < 0 || index >= holder.layout.size()) {
            return;
        }
        Cell cell = holder.layout.get(index);
        SlotType slot = registry.get(cell.slotId()).orElse(null);
        if (slot == null) {
            return;
        }
        ItemStack cursor = event.getCursor();
        boolean cursorEmpty = cursor == null || cursor.getType().isAir();
        ItemStack equipped = storage.getItem(player, cell.slotId(), cell.index());

        if (cursorEmpty) {
            if (equipped != null) {
                storage.set(player, cell.slotId(), cell.index(), null);
                top.setItem(index, placeholder(cell.slotId()));
                event.getView().setCursor(equipped);
                fireUnequip(player, cell, equipped);
            }
            return;
        }

        if (!slot.accepts(cursor)) {
            player.sendMessage(Component.text(
                    "That doesn't fit the " + slot.displayName() + " slot.", NamedTextColor.RED));
            return;
        }

        if (equipped == null) {
            ItemStack one = cursor.clone();
            one.setAmount(1);
            storage.set(player, cell.slotId(), cell.index(), one);
            top.setItem(index, one.clone());
            int left = cursor.getAmount() - 1;
            event.getView().setCursor(left <= 0 ? null : withAmount(cursor, left));
            fireEquip(player, cell, one);
        } else if (cursor.getAmount() == 1) {
            ItemStack incoming = cursor.clone();
            incoming.setAmount(1);
            storage.set(player, cell.slotId(), cell.index(), incoming);
            top.setItem(index, incoming.clone());
            event.getView().setCursor(equipped);
            fireUnequip(player, cell, equipped);
            fireEquip(player, cell, incoming);
        } else {
            player.sendMessage(Component.text(
                    "Hold a single curio to swap it in.", NamedTextColor.RED));
        }
    }

    private void handleShiftEquip(InventoryClickEvent event, Player player, Inventory top, Holder holder) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        int cellIndex = -1;
        Cell target = null;
        for (int i = 0; i < holder.layout.size(); i++) {
            Cell cell = holder.layout.get(i);
            SlotType slot = registry.get(cell.slotId()).orElse(null);
            if (slot != null && slot.accepts(clicked)
                    && storage.isEmpty(player, cell.slotId(), cell.index())) {
                cellIndex = i;
                target = cell;
                break;
            }
        }
        if (target == null) {
            player.sendMessage(Component.text("No free slot for that curio.", NamedTextColor.RED));
            return;
        }
        ItemStack one = clicked.clone();
        one.setAmount(1);
        storage.set(player, target.slotId(), target.index(), one);
        top.setItem(cellIndex, one.clone());
        int left = clicked.getAmount() - 1;
        if (left <= 0) {
            event.getClickedInventory().setItem(event.getSlot(), null);
        } else {
            clicked.setAmount(left);
        }
        fireEquip(player, target, one);
    }

    private void fireEquip(Player player, Cell cell, ItemStack item) {
        Bukkit.getPluginManager().callEvent(
                new CurioEquipEvent(player, cell.slotId(), cell.index(), item.clone()));
    }

    private void fireUnequip(Player player, Cell cell, ItemStack item) {
        Bukkit.getPluginManager().callEvent(
                new CurioUnequipEvent(player, cell.slotId(), cell.index(), item.clone()));
    }

    private static ItemStack withAmount(ItemStack base, int amount) {
        ItemStack copy = base.clone();
        copy.setAmount(amount);
        return copy;
    }

    private ItemStack placeholder(String slotId) {
        SlotType slot = registry.get(slotId).orElse(null);
        String display = slot != null ? slot.displayName() : slotId;
        ItemStack pane = new ItemStack(slot != null ? slot.icon()
                : org.bukkit.Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(display + " slot", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text(
                            "Place a " + display.toLowerCase(Locale.ROOT) + " curio here.",
                            NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    /** Marker holder so menu inventories can be recognised and carry their layout. */
    private static final class Holder implements InventoryHolder {
        private final List<Cell> layout;
        private Inventory inventory;

        private Holder(List<Cell> layout) {
            this.layout = layout;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
