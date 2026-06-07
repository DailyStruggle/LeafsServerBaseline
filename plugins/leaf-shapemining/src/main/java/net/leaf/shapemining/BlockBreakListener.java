package net.leaf.shapemining;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Executes steps 3-4 of the shape-mining pipeline on a vanilla block break.
 *
 * <p>v1 policy (per {@code docs/scratch/SHAPE-MINING-DESIGN.md}): walk the
 * planner's nearest-first candidate list and break each block only while it is
 * owned by the current region thread and its chunk is loaded; stop at the first
 * block that fails either check. This keeps the whole operation on one thread in
 * one tick and is fully Folia-safe.</p>
 */
public final class BlockBreakListener implements Listener {

    private final ShapeMiningConfig config;
    private final Server server;

    public BlockBreakListener(Server server, ShapeMiningConfig config) {
        this.server = server;
        this.config = config;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!ShapeMiningTool.isTool(tool)) {
            return;
        }
        MineShape shape = ShapeMiningTool.getMode(tool);
        if (shape == MineShape.OFF) {
            return;
        }

        Block origin = event.getBlock();
        // A shape only triggers when the originally-broken block is bulk filler;
        // mining ore (or anything else) behaves like a normal pickaxe.
        if (!config.isFiller(origin.getType())) {
            return;
        }

        List<Block> candidates = ShapePlanner.plan(player, origin, shape, config);
        if (candidates.isEmpty()) {
            return;
        }

        int broken = 0;
        for (Block block : candidates) {
            if (broken >= config.maxBlocksPerSwing()) {
                break;
            }
            // v1: stop the line at the first non-owned or unloaded block.
            if (!server.isOwnedByCurrentRegion(block.getLocation())) {
                break;
            }
            if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) {
                break;
            }
            Material type = block.getType();
            if (type.isAir()) {
                continue;
            }
            if (!config.isFiller(type)) {
                continue;
            }
            // breakNaturally drops as if mined with the tool and does NOT fire a
            // BlockBreakEvent, so there is no recursion to guard against.
            block.breakNaturally(tool);
            broken++;
        }

        if (broken > 0 && config.durabilityPerBlock() > 0) {
            applyDurability(player, tool, broken * config.durabilityPerBlock());
        }
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
            // The tool breaks. Mirror vanilla by clearing it from the main hand.
            player.getInventory().setItemInMainHand(null);
            return;
        }
        damageable.setDamage(newDamage);
        tool.setItemMeta(meta);
        player.getInventory().setItemInMainHand(tool);
    }
}
