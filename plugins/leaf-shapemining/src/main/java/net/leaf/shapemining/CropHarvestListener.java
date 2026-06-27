package net.leaf.shapemining;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The hoe's farming special case: a right-click area-harvest in {@code vein}
 * mode. Replacing crops one at a time is the tedious part of farming, so this
 * mirrors the modded "right-click harvest" - it harvests the whole connected
 * field of mature crops, drops the produce, and resets each plant to growth
 * stage 0 (leaving it planted) rather than breaking it.
 *
 * <p>It is the right-click twin of {@link BlockBreakListener}'s left-click
 * {@code vein} flood-fill: left-click bulk-BREAKS the field, right-click bulk-
 * HARVESTS-and-replants it. Both are scoped to the hoe's configured
 * {@code vein} list and gated by the same opt-in flag, tool class, and
 * Folia-safe region-ownership / loaded-chunk checks.</p>
 */
public final class CropHarvestListener implements Listener {

    /** The replanted seed item removed from the drops of each harvested crop. */
    private static final Map<Material, Material> REPLANT_SEED = new EnumMap<>(Material.class);

    static {
        REPLANT_SEED.put(Material.WHEAT, Material.WHEAT_SEEDS);
        REPLANT_SEED.put(Material.CARROTS, Material.CARROT);
        REPLANT_SEED.put(Material.POTATOES, Material.POTATO);
        REPLANT_SEED.put(Material.BEETROOTS, Material.BEETROOT_SEEDS);
        REPLANT_SEED.put(Material.NETHER_WART, Material.NETHER_WART);
    }

    private final ShapeMiningConfig config;
    private final Server server;

    public CropHarvestListener(Server server, ShapeMiningConfig config) {
        this.server = server;
        this.config = config;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block origin = event.getClickedBlock();
        if (origin == null) {
            return;
        }
        Player player = event.getPlayer();
        if (!ShapeMiningTool.isEnabled(player)) {
            return;
        }
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (ToolClass.of(tool) != ToolClass.HOE) {
            return;
        }
        if (ShapeMiningTool.getMode(tool) != MineShape.VEIN) {
            return;
        }
        if (!config.isVein(ToolClass.HOE, origin.getType()) || !isMature(origin)) {
            return;
        }

        // We are taking over this interaction; stop vanilla from doing anything
        // else with the click (e.g. block placement from the off-hand context).
        event.setCancelled(true);
        runHarvest(player, origin, tool);
    }

    /**
     * Flood-fills the connected field of the same crop type and harvests every
     * mature plant. 26-connectivity matches the left-click vein fill; the same
     * region-ownership / loaded-chunk gate keeps the whole operation on one
     * region thread in this tick (Folia-safe).
     */
    private void runHarvest(Player player, Block origin, ItemStack tool) {
        Material cropType = origin.getType();
        int limit = Math.min(config.maxVein(), config.maxBlocksPerSwing());

        Set<Long> visited = new HashSet<>();
        Deque<Block> frontier = new ArrayDeque<>();
        visited.add(blockKey(origin));
        frontier.add(origin);

        int harvested = harvestCrop(origin, tool) ? 1 : 0;
        while (!frontier.isEmpty() && harvested < limit) {
            Block current = frontier.poll();
            for (int dx = -1; dx <= 1 && harvested < limit; dx++) {
                for (int dy = -1; dy <= 1 && harvested < limit; dy++) {
                    for (int dz = -1; dz <= 1 && harvested < limit; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        Block next = current.getRelative(dx, dy, dz);
                        if (!visited.add(blockKey(next))) {
                            continue;
                        }
                        if (next.getType() != cropType) {
                            continue;
                        }
                        if (!server.isOwnedByCurrentRegion(next.getLocation())) {
                            continue;
                        }
                        if (!next.getWorld().isChunkLoaded(next.getX() >> 4, next.getZ() >> 4)) {
                            continue;
                        }
                        // Same-type immature plants still bridge the field, so keep
                        // walking through them; only mature ones are harvested.
                        frontier.add(next);
                        if (harvestCrop(next, tool)) {
                            harvested++;
                        }
                    }
                }
            }
        }

        if (harvested > 0 && config.durabilityPerBlock() > 0) {
            applyDurability(player, tool, harvested * config.durabilityPerBlock());
        }
    }

    /**
     * Harvests a single mature crop: drops its produce (minus one replanted
     * seed) and resets it to growth stage 0, leaving the plant in the ground.
     * Returns {@code false} (no-op) if the block is not a mature crop.
     */
    private boolean harvestCrop(Block block, ItemStack tool) {
        BlockData data = block.getBlockData();
        if (!(data instanceof Ageable age) || age.getAge() < age.getMaximumAge()) {
            return false;
        }
        List<ItemStack> drops = new ArrayList<>(block.getDrops(tool));
        removeOneReplantSeed(drops, block.getType());
        for (ItemStack drop : drops) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
        }
        age.setAge(0);
        block.setBlockData(age);
        return true;
    }

    /** Removes a single replant seed from the drops, mirroring a manual harvest + replant. */
    private static void removeOneReplantSeed(List<ItemStack> drops, Material cropType) {
        Material seed = REPLANT_SEED.get(cropType);
        if (seed == null) {
            return;
        }
        for (Iterator<ItemStack> it = drops.iterator(); it.hasNext(); ) {
            ItemStack drop = it.next();
            if (drop.getType() != seed) {
                continue;
            }
            int amount = drop.getAmount() - 1;
            if (amount <= 0) {
                it.remove();
            } else {
                drop.setAmount(amount);
            }
            return;
        }
    }

    private static boolean isMature(Block block) {
        return block.getBlockData() instanceof Ageable age && age.getAge() >= age.getMaximumAge();
    }

    private static long blockKey(Block block) {
        return (((long) block.getX() & 0x3FFFFFF) << 38)
                | (((long) block.getZ() & 0x3FFFFFF) << 12)
                | ((long) block.getY() & 0xFFF);
    }

    private void applyDurability(Player player, ItemStack tool, int amount) {
        short maxDurability = tool.getType().getMaxDurability();
        if (maxDurability <= 0) {
            return;
        }
        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }
        if (damageable.isUnbreakable()) {
            return;
        }
        int newDamage = damageable.getDamage() + amount;
        if (newDamage >= maxDurability) {
            player.getInventory().setItemInMainHand(null);
            return;
        }
        damageable.setDamage(newDamage);
        tool.setItemMeta(meta);
        player.getInventory().setItemInMainHand(tool);
    }
}
