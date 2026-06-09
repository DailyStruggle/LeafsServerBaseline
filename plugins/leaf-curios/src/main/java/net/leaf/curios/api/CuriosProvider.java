package net.leaf.curios.api;

import org.bukkit.Bukkit;

/**
 * Static access point for the {@link CuriosApi} service.
 *
 * <p>LeafCurios sets the instance on enable. Consumers should fetch it lazily
 * (e.g. in their own {@code onEnable} after declaring {@code depend: [LeafCurios]}
 * in their plugin.yml) via {@link #get()}.</p>
 */
public final class CuriosProvider {

    private static CuriosApi instance;

    private CuriosProvider() {
    }

    /** Called by LeafCurios on enable. */
    public static void set(CuriosApi api) {
        instance = api;
    }

    /**
     * The active {@link CuriosApi}, or {@code null} if LeafCurios is not loaded.
     * Falls back to the Bukkit {@code ServicesManager} if the direct instance is
     * not set yet.
     */
    public static CuriosApi get() {
        if (instance != null) {
            return instance;
        }
        var registration = Bukkit.getServicesManager().getRegistration(CuriosApi.class);
        return registration == null ? null : registration.getProvider();
    }
}
