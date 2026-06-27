package net.leaf.shapemining;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Sneak + scroll mode switching (the primary toggle from the design note).
 *
 * <p>While sneaking and scrolling away from a held shape-mining tool, the scroll
 * is consumed to cycle the tool's shape instead of changing the hotbar slot. The
 * cycle is non-trapping: scrolling past the last shape (forward) or past
 * {@link MineShape#OFF} (backward) resets the tool to {@code OFF} and lets the
 * slot change fall through, so a player can always swap tools (e.g. while
 * sneaking constantly in the deep dark).</p>
 */
public final class ModeSwitchListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!ShapeMiningTool.isEnabled(player)) {
            return; // feature is opt-in per player (/shapemine); leave normal scrolling alone.
        }
        if (!player.isSneaking()) {
            return;
        }

        int direction = scrollDirection(event.getPreviousSlot(), event.getNewSlot());
        if (direction == 0) {
            return; // not a single-step scroll (e.g. a number-key jump): let it pass.
        }

        // The tool we are scrolling away from lives in the previous slot.
        ItemStack tool = player.getInventory().getItem(event.getPreviousSlot());
        if (!ShapeMiningTool.isTool(tool)) {
            return;
        }

        MineShape current = ShapeMiningTool.getMode(tool);
        int count = MineShape.values().length;
        int nextOrdinal = current.ordinal() + direction;

        if (nextOrdinal >= 0 && nextOrdinal < count) {
            // Still within the cycle: consume the scroll and advance the shape.
            MineShape next = MineShape.fromOrdinal(nextOrdinal);
            ShapeMiningTool.setMode(tool, next);
            player.getInventory().setItem(event.getPreviousSlot(), tool);
            event.setCancelled(true);
            player.sendActionBar(Component.text(
                    "Shape: " + next.size() + " (" + next.label() + ")", NamedTextColor.AQUA));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
        } else {
            // Past the end (either direction): reset to OFF and let the slot change
            // fall through so the player escapes the tool with one extra scroll.
            if (current != MineShape.OFF) {
                ShapeMiningTool.setMode(tool, MineShape.OFF);
                player.getInventory().setItem(event.getPreviousSlot(), tool);
            }
            player.sendActionBar(Component.text("Exiting shape mode", NamedTextColor.GRAY));
        }
    }

    /**
     * Returns +1 for a forward scroll, -1 for a backward scroll, or 0 if the slot
     * change is not a single hotbar step (number-key jump). Handles the 8 -&gt; 0
     * and 0 -&gt; 8 wrap-around.
     */
    static int scrollDirection(int previous, int next) {
        int delta = next - previous;
        if (delta == 1 || delta == -8) {
            return 1;
        }
        if (delta == -1 || delta == 8) {
            return -1;
        }
        return 0;
    }
}
