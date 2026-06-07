package net.leaf.artifacts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Optional;

/**
 * The vanilla-UI "curio" equipment menu: a single chest row whose slots are
 * each dedicated to a {@link CurioSlot} category (see {@link ArtifactSlots}).
 *
 * <p>The menu is a view over {@link ArtifactEquipment} (the real storage).
 * Empty slots show a light-gray glass placeholder that names the slot category;
 * placing an artifact equips it, taking it out unequips it. After any change the
 * player's modifiers/effects are reconciled immediately.</p>
 */
public final class ArtifactMenu implements Listener {

    private static final Component TITLE = Component.text("Artifacts", NamedTextColor.DARK_PURPLE);

    private final ArtifactEquipment equipment;
    private final ArtifactController controller;

    public ArtifactMenu(ArtifactEquipment equipment, ArtifactController controller) {
        this.equipment = equipment;
        this.controller = controller;
    }

    /** Opens the curio menu for the given player. */
    public void open(Player player) {
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, ArtifactSlots.SLOT_COUNT, TITLE);
        holder.inventory = inv;
        for (int i = 0; i < ArtifactSlots.SLOT_COUNT; i++) {
            ItemStack equipped = equipment.getItem(player, i);
            inv.setItem(i, (equipped == null) ? placeholder(ArtifactSlots.categoryAt(i)) : equipped);
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        boolean clickedTop = event.getClickedInventory() == top;

        if (clickedTop) {
            event.setCancelled(true);
            handleTopClick(event, player, top);
            return;
        }

        // Clicked in the player's own inventory: block any move/collect into the
        // menu, but auto-equip on shift-click of an artifact.
        if (event.isShiftClick()) {
            event.setCancelled(true);
            handleShiftEquip(event, player, top);
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

    private void handleTopClick(InventoryClickEvent event, Player player, Inventory top) {
        int index = event.getRawSlot();
        if (index < 0 || index >= ArtifactSlots.SLOT_COUNT) {
            return;
        }
        CurioSlot category = ArtifactSlots.categoryAt(index);
        ItemStack cursor = event.getCursor();
        boolean cursorEmpty = cursor == null || cursor.getType().isAir();
        ItemStack equipped = equipment.getItem(player, index);

        if (cursorEmpty) {
            if (equipped != null) {
                // Unequip: hand the artifact back onto the cursor.
                equipment.setItem(player, index, null);
                top.setItem(index, placeholder(category));
                event.getView().setCursor(equipped);
                reconcile(player);
            }
            return;
        }

        Optional<ArtifactType> type = ArtifactItem.typeOf(cursor);
        if (type.isEmpty() || !ArtifactSlots.fits(type.get(), index)) {
            player.sendMessage(Component.text(
                    "That doesn't fit the " + category.displayName() + " slot.", NamedTextColor.RED));
            return;
        }

        if (equipped == null) {
            // Equip one copy from the cursor stack.
            ItemStack one = cursor.clone();
            one.setAmount(1);
            equipment.setItem(player, index, one);
            top.setItem(index, one.clone());
            int left = cursor.getAmount() - 1;
            event.getView().setCursor(left <= 0 ? null : withAmount(cursor, left));
            reconcile(player);
        } else if (cursor.getAmount() == 1) {
            // Swap the equipped artifact with the single one on the cursor.
            ItemStack incoming = cursor.clone();
            incoming.setAmount(1);
            equipment.setItem(player, index, incoming);
            top.setItem(index, incoming.clone());
            event.getView().setCursor(equipped);
            reconcile(player);
        } else {
            player.sendMessage(Component.text(
                    "Hold a single artifact to swap it in.", NamedTextColor.RED));
        }
    }

    private void handleShiftEquip(InventoryClickEvent event, Player player, Inventory top) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        Optional<ArtifactType> type = ArtifactItem.typeOf(clicked);
        if (type.isEmpty()) {
            return;
        }
        int index = ArtifactSlots.firstFitting(type.get(), i -> equipment.isEmpty(player, i));
        if (index < 0) {
            player.sendMessage(Component.text(
                    "No free slot for " + type.get().displayName() + ".", NamedTextColor.RED));
            return;
        }
        ItemStack one = clicked.clone();
        one.setAmount(1);
        equipment.setItem(player, index, one);
        top.setItem(index, one.clone());
        int left = clicked.getAmount() - 1;
        if (left <= 0) {
            event.getClickedInventory().setItem(event.getSlot(), null);
        } else {
            clicked.setAmount(left);
        }
        reconcile(player);
    }

    private void reconcile(Player player) {
        controller.reconcile(player);
    }

    private static ItemStack withAmount(ItemStack base, int amount) {
        ItemStack copy = base.clone();
        copy.setAmount(amount);
        return copy;
    }

    private static ItemStack placeholder(CurioSlot category) {
        ItemStack pane = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(category.displayName() + " slot", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text(
                            "Place a " + category.displayName().toLowerCase() + " artifact here.",
                            NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    /** Marker holder so menu inventories can be recognised in events. */
    private static final class Holder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
