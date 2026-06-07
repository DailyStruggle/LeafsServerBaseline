package net.leaf.backpacks;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

/**
 * Wires up the backpack item: right-click to open, GUI slot protection, content
 * persistence on close, stack-boost normalisation on withdrawal, and the PICKUP
 * upgrade routing dropped items straight into a carried backpack.
 */
public final class BackpackListener implements Listener {

    private final LeafBackpacksPlugin plugin;
    private final BackpackConfig config;
    private final BackpackStore store;
    private final BackpackItem items;

    public BackpackListener(LeafBackpacksPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.backpackConfig();
        this.store = plugin.store();
        this.items = plugin.itemFactory();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (!items.isBackpack(item)) {
            return;
        }
        event.setCancelled(true); // never place the base item
        Player player = event.getPlayer();
        UUID id = items.getId(item);
        if (id == null) {
            return;
        }
        int tier = items.getTier(item);
        BackpackGui gui = new BackpackGui(id, tier, config, store);
        plugin.openBackpacks().add(id);
        player.openInventory(gui.getInventory());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof BackpackGui gui)) {
            return;
        }
        Inventory inv = event.getInventory();
        UUID id = gui.backpackId();

        ItemStack[] contents = new ItemStack[BackpackStore.CONTENTS_SIZE];
        for (int i = BackpackGui.MAIN_START; i < BackpackGui.MAIN_END; i++) {
            contents[i] = inv.getItem(i);
        }

        boolean stackBoost = hasStackUpgrade(inv, gui);
        for (ItemStack stack : contents) {
            applyStackBoost(stack, stackBoost);
        }
        store.setContents(id, contents);

        ItemStack[] upgrades = new ItemStack[BackpackStore.MAX_UPGRADE_SLOTS];
        for (int i = 0; i < gui.upgradeSlots(); i++) {
            upgrades[i] = inv.getItem(BackpackGui.UPGRADE_ROW_START + i);
        }
        store.setUpgrades(id, upgrades);

        plugin.openBackpacks().remove(id);
        store.save();
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof BackpackGui gui)) {
            return;
        }
        int raw = event.getRawSlot();
        boolean topInventory = raw < BackpackGui.SIZE;

        if (topInventory) {
            if (gui.isLockedSlot(raw)) {
                event.setCancelled(true);
                return;
            }
            if (gui.isUnlockedUpgradeSlot(raw)) {
                ItemStack incoming = event.getCursor();
                if (incoming != null && !incoming.getType().isAir() && !items.isUpgrade(incoming)) {
                    event.setCancelled(true); // only upgrade modules belong here
                    return;
                }
            }
            if (gui.isMainSlot(raw)) {
                // Disallow nesting a backpack inside a backpack.
                ItemStack cursor = event.getCursor();
                if (cursor != null && items.isBackpack(cursor)) {
                    event.setCancelled(true);
                    return;
                }
            }
        } else if (event.isShiftClick() && items.isBackpack(event.getCurrentItem())) {
            // Prevent shift-clicking a backpack into another backpack.
            event.setCancelled(true);
            return;
        }

        // Best-effort parity: items leaving the backpack normalise to vanilla stack rules.
        scheduleStackNormalize((Player) event.getWhoClicked());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof BackpackGui gui)) {
            return;
        }
        for (int raw : event.getRawSlots()) {
            if (raw >= BackpackGui.SIZE) {
                continue;
            }
            if (gui.isLockedSlot(raw)
                    || (gui.isUnlockedUpgradeSlot(raw) && !items.isUpgrade(event.getOldCursor()))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        UUID target = firstBackpackWithUpgrade(player, UpgradeType.PICKUP);
        if (target == null || plugin.openBackpacks().contains(target)) {
            return;
        }
        ItemStack drop = event.getItem().getItemStack();
        if (items.isBackpack(drop) || items.isUpgrade(drop)) {
            return; // do not vacuum backpacks/modules into storage
        }
        ItemStack[] contents = store.getContents(target);
        ItemStack leftover = insert(contents, drop.clone());
        if (leftover != null && leftover.getAmount() == drop.getAmount()) {
            return; // backpack full, fall through to normal pickup
        }
        store.setContents(target, contents);
        event.setCancelled(true);
        if (leftover == null) {
            event.getItem().remove();
        } else {
            event.getItem().setItemStack(leftover);
        }
        store.save();
    }

    private UUID firstBackpackWithUpgrade(Player player, UpgradeType type) {
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (!items.isBackpack(item)) {
                continue;
            }
            UUID id = items.getId(item);
            if (id == null) {
                continue;
            }
            for (ItemStack up : store.getUpgrades(id)) {
                if (items.upgradeType(up) == type) {
                    return id;
                }
            }
        }
        return null;
    }

    private boolean hasStackUpgrade(Inventory inv, BackpackGui gui) {
        for (int i = 0; i < gui.upgradeSlots(); i++) {
            if (items.upgradeType(inv.getItem(BackpackGui.UPGRADE_ROW_START + i)) == UpgradeType.STACK) {
                return true;
            }
        }
        return false;
    }

    private void applyStackBoost(ItemStack stack, boolean boost) {
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        if (boost) {
            meta.setMaxStackSize(config.stackMaxSize());
        } else if (meta.hasMaxStackSize()) {
            meta.setMaxStackSize(null);
        }
        stack.setItemMeta(meta);
    }

    private void scheduleStackNormalize(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (ItemStack item : player.getInventory().getStorageContents()) {
                if (item == null || items.isBackpack(item) || items.isUpgrade(item)) {
                    continue;
                }
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasMaxStackSize()) {
                    meta.setMaxStackSize(null);
                    item.setItemMeta(meta);
                }
            }
        });
    }

    /**
     * Insert {@code stack} into {@code contents}; returns the leftover that did
     * not fit (or {@code null} if all of it fit). Mutates {@code contents}.
     */
    private ItemStack insert(ItemStack[] contents, ItemStack stack) {
        int max = stack.getMaxStackSize();
        // Merge into existing similar stacks first.
        for (int i = 0; i < contents.length && stack.getAmount() > 0; i++) {
            ItemStack slot = contents[i];
            if (slot != null && slot.isSimilar(stack) && slot.getAmount() < max) {
                int move = Math.min(max - slot.getAmount(), stack.getAmount());
                slot.setAmount(slot.getAmount() + move);
                stack.setAmount(stack.getAmount() - move);
            }
        }
        // Fill empty slots.
        for (int i = 0; i < contents.length && stack.getAmount() > 0; i++) {
            if (contents[i] == null || contents[i].getType().isAir()) {
                contents[i] = stack.clone();
                stack.setAmount(0);
            }
        }
        return stack.getAmount() > 0 ? stack : null;
    }
}
