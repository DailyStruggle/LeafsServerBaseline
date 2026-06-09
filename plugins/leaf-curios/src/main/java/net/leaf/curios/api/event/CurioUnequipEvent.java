package net.leaf.curios.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Fired after a curio is removed from a slot through the LeafCurios menu or API.
 *
 * <p>Informational (not cancellable): the change has already been applied to the
 * player's curio store.</p>
 */
public final class CurioUnequipEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String slotId;
    private final int index;
    private final ItemStack item;

    public CurioUnequipEvent(Player player, String slotId, int index, ItemStack item) {
        this.player = player;
        this.slotId = slotId;
        this.index = index;
        this.item = item;
    }

    public Player getPlayer() {
        return player;
    }

    public String getSlotId() {
        return slotId;
    }

    public int getIndex() {
        return index;
    }

    /** The item that was removed (a copy). */
    public ItemStack getItem() {
        return item;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
