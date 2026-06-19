package net.leaf.mobs;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Holder for the {@link NamespacedKey}s used to mark managed mobs in their
 * {@link PersistentDataContainer}.
 *
 * <p>The PDC marker is the single source of truth for "this entity is one of ours". It
 * survives chunk unload and server restart, so later phases can re-attach runtime
 * controllers (boss bars, ability tickers) to mobs that were spawned in a previous
 * session. F0 only writes the markers; nothing reads them yet beyond {@link #isManaged}.</p>
 */
public final class MobKeys {

    private final NamespacedKey mobId;
    private final NamespacedKey tier;
    private final NamespacedKey biome;
    private final NamespacedKey addOwner;
    private final NamespacedKey thornsUntil;
    private final NamespacedKey thornsPct;

    public MobKeys(Plugin plugin) {
        this.mobId = new NamespacedKey(plugin, "mob_id");
        this.tier = new NamespacedKey(plugin, "tier");
        this.biome = new NamespacedKey(plugin, "biome");
        this.addOwner = new NamespacedKey(plugin, "add_owner");
        this.thornsUntil = new NamespacedKey(plugin, "thorns_until");
        this.thornsPct = new NamespacedKey(plugin, "thorns_pct");
    }

    public NamespacedKey mobId() {
        return mobId;
    }

    public NamespacedKey tier() {
        return tier;
    }

    public NamespacedKey biome() {
        return biome;
    }

    /**
     * Marker written on summoned adds, carrying the mob id of the boss that summoned them so
     * the lifecycle layer can recognise and cull them (F3 {@code summon_adds}).
     */
    public NamespacedKey addOwner() {
        return addOwner;
    }

    /**
     * Expiry timestamp (epoch millis) of an active Thorn-Pendant-themed reflect buff. While
     * {@code now < thornsUntil} the mob reflects a fraction (see {@link #thornsPct()}) of the
     * melee damage it takes back at the attacker - the boss-side echo of the dropped artifact.
     */
    public NamespacedKey thornsUntil() {
        return thornsUntil;
    }

    /** Fraction (0..1) of incoming melee damage reflected while the thorns buff is active. */
    public NamespacedKey thornsPct() {
        return thornsPct;
    }

    /** True if the container carries our mob-id marker. */
    public boolean isManaged(PersistentDataContainer pdc) {
        return pdc.has(mobId, PersistentDataType.STRING);
    }

    /** Reads the managed mob id, or {@code null} if the container is not one of ours. */
    public String readMobId(PersistentDataContainer pdc) {
        return pdc.get(mobId, PersistentDataType.STRING);
    }
}
