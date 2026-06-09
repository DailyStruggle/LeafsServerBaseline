package net.leaf.curios.internal;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Loads each player's curios into memory on join and persists them on quit.
 *
 * <p>Join runs at {@code LOWEST} priority so the per-player store is ready before
 * consumer plugins (which depend on LeafCurios) react to the same join.</p>
 */
public final class CurioStorageListener implements Listener {

    private final CurioStorage storage;

    public CurioStorageListener(CurioStorage storage) {
        this.storage = storage;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        storage.load(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        storage.save(event.getPlayer());
        storage.unload(event.getPlayer().getUniqueId());
    }
}
