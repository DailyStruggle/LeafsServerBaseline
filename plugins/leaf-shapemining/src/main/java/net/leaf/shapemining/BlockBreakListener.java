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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Executes steps 3-4 of the shape/vein pipeline on a vanilla block break.
 *
 * <p>The feature is opt-in per player ({@code /shapemine}) and runs off whatever
 * vanilla tool the player is holding ({@link ToolClass}); it never breaks a
 * block the held tool cannot actually harvest, so a stone pickaxe will not
 * shape-break a diamond-tier block.</p>
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
        if (!ShapeMiningTool.isEnabled(player)) {
            return;
        }
        ItemStack tool = player.getInventory().getItemInMainHand();
        ToolClass toolClass = ToolClass.of(tool);
        if (toolClass == null) {
            return;
        }
        MineShape shape = ShapeMiningTool.getMode(tool);
        if (shape == MineShape.OFF) {
            return;
        }

        Block origin = event.getBlock();

        // Vein mode is the Ultimine-style branch: it triggers only on a configured
        // vein material for this tool class (low-value ore for pickaxes, logs for
        // axes, etc.) and clears the connected vein of that same material.
        if (shape == MineShape.VEIN) {
            if (config.isVein(toolClass, origin.getType())) {
                runVein(player, origin, tool);
            }
            return;
        }

        // A filler shape only triggers when the originally-broken block is filler
        // for this tool class; breaking anything else behaves like a normal tool.
        if (!config.isFiller(toolClass, origin.getType())) {
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
            if (!config.isFiller(toolClass, type)) {
                continue;
            }
            if (!canHarvest(block, tool)) {
                continue; // "within what they can mine": never destroy a block this tool can't harvest.
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

    /**
     * Ultimine-style flood fill: clears the vein of the same material as the
     * origin. The originally-broken block is handled by vanilla; this breaks the
     * connected neighbours. Folia-safe: it only ever reads/breaks blocks owned by
     * the current region thread and in loaded chunks, so all work stays on one
     * thread in this tick. 26-connectivity catches diagonal vein segments.
     */
    private void runVein(Player player, Block origin, ItemStack tool) {
        Material veinType = origin.getType();
        int limit = Math.min(config.maxVein(), config.maxBlocksPerSwing());

        Set<Long> visited = new HashSet<>();
        Deque<Block> frontier = new ArrayDeque<>();
        visited.add(blockKey(origin));
        frontier.add(origin);

        int broken = 0;
        while (!frontier.isEmpty() && broken < limit) {
            Block current = frontier.poll();
            for (int dx = -1; dx <= 1 && broken < limit; dx++) {
                for (int dy = -1; dy <= 1 && broken < limit; dy++) {
                    for (int dz = -1; dz <= 1 && broken < limit; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        Block next = current.getRelative(dx, dy, dz);
                        if (!visited.add(blockKey(next))) {
                            continue;
                        }
                        // Skip (don't break the whole fill) any neighbour we cannot
                        // safely touch from this region thread.
                        if (!server.isOwnedByCurrentRegion(next.getLocation())) {
                            continue;
                        }
                        if (!next.getWorld().isChunkLoaded(next.getX() >> 4, next.getZ() >> 4)) {
                            continue;
                        }
                        if (next.getType() != veinType) {
                            continue;
                        }
                        if (!canHarvest(next, tool)) {
                            continue;
                        }
                        next.breakNaturally(tool);
                        broken++;
                        frontier.add(next);
                    }
                }
            }
        }

        if (broken > 0 && config.durabilityPerBlock() > 0) {
            applyDurability(player, tool, broken * config.durabilityPerBlock());
        }
    }

    /**
     * Whether the held tool can actually harvest the block for drops. Honours the
     * "within what they can mine" rule: a non-empty drop set means the tool tier
     * is sufficient, so we never silently destroy a block the player could not
     * mine by hand.
     */
    private static boolean canHarvest(Block block, ItemStack tool) {
        return !block.getDrops(tool).isEmpty();
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
            // The tool breaks. Mirror vanilla by clearing it from the main hand.
            player.getInventory().setItemInMainHand(null);
            return;
        }
        damageable.setDamage(newDamage);
        tool.setItemMeta(meta);
        player.getInventory().setItemInMainHand(tool);
    }
}
