package net.leaf.backpacks.upgrades;

import net.leaf.backpacks.BackpackConfig;
import net.leaf.backpacks.BackpackItem;
import net.leaf.backpacks.BackpackStore;
import net.leaf.backpacks.UpgradeType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Periodic driver for the "active while carried" upgrades: MAGNET (pull nearby
 * drops toward the holder) and FEEDING (auto-eat from the backpack). Runs only
 * for online players and only inspects backpacks they are carrying.
 */
public final class UpgradeRunner extends BukkitRunnable {

    private final org.bukkit.plugin.Plugin plugin;
    private final BackpackConfig config;
    private final BackpackStore store;
    private final BackpackItem items;
    private final Set<UUID> openBackpacks;

    public UpgradeRunner(org.bukkit.plugin.Plugin plugin, BackpackConfig config, BackpackStore store,
                         BackpackItem items, Set<UUID> openBackpacks) {
        this.plugin = plugin;
        this.config = config;
        this.store = store;
        this.items = items;
        this.openBackpacks = openBackpacks;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Set<UpgradeType> active = EnumSet.noneOf(UpgradeType.class);
            UUID feedingBackpack = null;

            ItemStack[] storage = player.getInventory().getStorageContents();
            ItemStack offhand = player.getInventory().getItemInOffHand();
            for (ItemStack item : append(storage, offhand)) {
                if (!items.isBackpack(item)) {
                    continue;
                }
                UUID id = items.getId(item);
                if (id == null) {
                    continue;
                }
                for (ItemStack up : store.getUpgrades(id)) {
                    UpgradeType type = items.upgradeType(up);
                    if (type != null && type.isActive()) {
                        active.add(type);
                        if (type == UpgradeType.FEEDING && feedingBackpack == null
                                && !openBackpacks.contains(id)) {
                            feedingBackpack = id;
                        }
                    }
                }
            }

            if (active.contains(UpgradeType.MAGNET)) {
                applyMagnet(player);
            }
            if (active.contains(UpgradeType.FEEDING) && feedingBackpack != null) {
                applyFeeding(player, feedingBackpack);
            }
        }
    }

    private void applyMagnet(Player player) {
        double radius = config.magnetRadius();
        if (radius <= 0) {
            return;
        }
        for (Item drop : player.getWorld().getNearbyEntitiesByType(Item.class, player.getLocation(), radius)) {
            if (drop.getPickupDelay() > 0 || !drop.isValid()) {
                continue;
            }
            Vector pull = player.getLocation().add(0, 0.5, 0).toVector()
                    .subtract(drop.getLocation().toVector());
            if (pull.lengthSquared() > 0.01) {
                drop.setVelocity(pull.normalize().multiply(0.45));
            }
        }
    }

    private void applyFeeding(Player player, UUID backpackId) {
        if (player.getFoodLevel() > config.feedingThreshold()) {
            return;
        }
        ItemStack[] contents = store.getContents(backpackId);
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || !stack.getType().isEdible()) {
                continue;
            }
            int amount = stack.getAmount() - 1;
            if (amount <= 0) {
                contents[i] = null;
            } else {
                stack.setAmount(amount);
            }
            // Conservative restore: vanilla food values are not exposed via the API,
            // so we top up a fixed amount and let saturation follow.
            player.setFoodLevel(Math.min(20, player.getFoodLevel() + 4));
            player.setSaturation(Math.min(player.getFoodLevel(), player.getSaturation() + 4f));
            store.setContents(backpackId, contents);
            return;
        }
    }

    private static ItemStack[] append(ItemStack[] arr, ItemStack extra) {
        ItemStack[] out = new ItemStack[arr.length + 1];
        System.arraycopy(arr, 0, out, 0, arr.length);
        out[arr.length] = extra;
        return out;
    }
}
