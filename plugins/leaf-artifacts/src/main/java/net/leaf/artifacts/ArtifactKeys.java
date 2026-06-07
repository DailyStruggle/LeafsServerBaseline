package net.leaf.artifacts;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.plugin.Plugin;

/**
 * Central holder for the plugin's {@link NamespacedKey}s. Initialised once on
 * enable.
 */
public final class ArtifactKeys {

    /** Marks an item as an artifact and stores its {@link ArtifactType} id (string). */
    public static NamespacedKey ARTIFACT_TYPE;

    /** Stores a player's equipped artifacts (serialized item array, byte[]). */
    public static NamespacedKey EQUIPMENT;

    private static Plugin plugin;

    private ArtifactKeys() {
    }

    public static void init(Plugin owner) {
        plugin = owner;
        ARTIFACT_TYPE = new NamespacedKey(owner, "artifact_type");
        EQUIPMENT = new NamespacedKey(owner, "equipment");
    }

    /**
     * Stable per-(artifact, attribute) key used to tag the {@code AttributeModifier}
     * an artifact applies, so reconciliation can add/remove it idempotently.
     */
    public static NamespacedKey modifierKey(ArtifactType type, Attribute attribute) {
        String attr = attribute.name().toLowerCase(java.util.Locale.ROOT);
        return new NamespacedKey(plugin, "art_" + type.id() + "_" + attr);
    }
}
