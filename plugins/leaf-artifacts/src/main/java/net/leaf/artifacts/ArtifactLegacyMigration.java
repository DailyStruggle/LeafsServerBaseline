package net.leaf.artifacts;

import net.leaf.curios.api.CurioItems;
import net.leaf.curios.api.CuriosApi;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.io.BukkitObjectInputStream;

import java.io.ByteArrayInputStream;
import java.util.logging.Level;

/**
 * One-time migration of equipped artifacts from the old in-plugin storage (a
 * flat {@code ItemStack[9]} stored under {@link ArtifactKeys#EQUIPMENT}) to the
 * new LeafCurios per-slot store.
 *
 * <p>The legacy layout was a fixed 9-slot row; this maps each legacy index to
 * the equivalent LeafCurios slot id and copy index, re-tags the item with its
 * curio slot ids (legacy items predate the tag), pushes it through the API, then
 * removes the legacy key so the migration runs only once per player.</p>
 */
public final class ArtifactLegacyMigration {

    /** Legacy index -> (slotId, copy index) for the original fixed layout. */
    private static final String[] LEGACY_SLOT_ID = {
            "head", "necklace", "hands", "hands", "ring", "charm", "charm", "feet", "feet"
    };
    private static final int[] LEGACY_SLOT_INDEX = {
            0, 0, 0, 1, 0, 0, 1, 0, 1
    };

    private ArtifactLegacyMigration() {
    }

    public static void migrate(Plugin plugin, Player player, CuriosApi curios) {
        byte[] bytes = player.getPersistentDataContainer()
                .get(ArtifactKeys.EQUIPMENT, PersistentDataType.BYTE_ARRAY);
        if (bytes == null || bytes.length == 0) {
            return;
        }
        try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                boolean present = in.readBoolean();
                ItemStack item = present ? (ItemStack) in.readObject() : null;
                if (item == null || i >= LEGACY_SLOT_ID.length) {
                    continue;
                }
                ArtifactItem.typeOf(item).ifPresent(type -> CurioItems.setSlots(item, type.slots()));
                curios.setEquipped(player, LEGACY_SLOT_ID[i], LEGACY_SLOT_INDEX[i], item);
            }
            plugin.getLogger().info("Migrated legacy artifacts equipment for " + player.getName() + ".");
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to migrate legacy artifacts equipment for " + player.getName() + ".", ex);
        } finally {
            // Always drop the legacy key so we never re-run (even on a partial failure).
            player.getPersistentDataContainer().remove(ArtifactKeys.EQUIPMENT);
        }
    }
}
