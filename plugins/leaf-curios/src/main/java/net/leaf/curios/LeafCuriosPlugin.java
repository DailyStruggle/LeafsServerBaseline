package net.leaf.curios;

import net.leaf.curios.api.CuriosApi;
import net.leaf.curios.api.CuriosProvider;
import net.leaf.curios.internal.CurioStorage;
import net.leaf.curios.internal.CurioStorageListener;
import net.leaf.curios.internal.CuriosCommand;
import net.leaf.curios.internal.CuriosMenu;
import net.leaf.curios.internal.CuriosServiceImpl;
import net.leaf.curios.internal.SlotRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * LeafCurios: a Curios-style trinket/accessory slot API for Paper/Folia.
 *
 * <p>Provides a dynamic slot-type registry, a per-player persisted curio store,
 * a shared vanilla-UI equipment menu, equip/unequip events, and a {@code /curios}
 * command. Other plugins consume it via {@link CuriosProvider#get()} after
 * declaring {@code depend: [LeafCurios]}. This plugin ships no slot types of its
 * own; consumers register the slot types they need.</p>
 */
public final class LeafCuriosPlugin extends JavaPlugin {

    private CurioStorage storage;
    private CuriosServiceImpl service;

    @Override
    public void onEnable() {
        SlotRegistry registry = new SlotRegistry();
        NamespacedKey equipmentKey = new NamespacedKey(this, "equipment");
        this.storage = new CurioStorage(registry, equipmentKey, getLogger());
        CuriosMenu menu = new CuriosMenu(registry, storage);
        this.service = new CuriosServiceImpl(registry, storage, menu);

        CuriosProvider.set(service);
        getServer().getServicesManager().register(CuriosApi.class, service, this, ServicePriority.Normal);

        getServer().getPluginManager().registerEvents(menu, this);
        getServer().getPluginManager().registerEvents(new CurioStorageListener(storage), this);

        CuriosCommand command = new CuriosCommand(service);
        if (getCommand("curios") != null) {
            getCommand("curios").setExecutor(command);
            getCommand("curios").setTabCompleter(command);
        }

        // Load any players already online (e.g. after a /reload).
        for (Player player : getServer().getOnlinePlayers()) {
            storage.load(player);
        }

        getLogger().info("LeafCurios enabled.");
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            for (Player player : getServer().getOnlinePlayers()) {
                storage.save(player);
            }
        }
        getServer().getServicesManager().unregisterAll(this);
        CuriosProvider.set(null);
    }
}
