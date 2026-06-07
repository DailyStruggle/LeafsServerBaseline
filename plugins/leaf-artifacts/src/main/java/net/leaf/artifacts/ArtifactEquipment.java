package net.leaf.artifacts;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Per-player store of the artifacts a player has equipped in the curio menu.
 *
 * <p>This store - not the player's main inventory - is the single source of
 * truth for which artifacts are "active". Each player owns a fixed-size array of
 * {@link ItemStack}s (one per menu slot; see {@link ArtifactSlots#LAYOUT}). The
 * array is persisted to the player's {@code PersistentDataContainer} so it
 * survives relogs and restarts.</p>
 *
 * <p>All access for a given player happens on that player's region thread (join,
 * menu interaction, quit), so the per-player array is effectively confined to a
 * single thread; the outer map is concurrent only to tolerate overlapping
 * players.</p>
 */
public final class ArtifactEquipment {

    private final Map<UUID, ItemStack[]> store = new ConcurrentHashMap<>();
    private final Logger logger;

    public ArtifactEquipment(Logger logger) {
        this.logger = logger;
    }

    /** Loads (or initialises) the player's equipped artifacts from their PDC. */
    public void load(Player player) {
        byte[] bytes = player.getPersistentDataContainer()
                .get(ArtifactKeys.EQUIPMENT, PersistentDataType.BYTE_ARRAY);
        ItemStack[] items = new ItemStack[ArtifactSlots.SLOT_COUNT];
        if (bytes != null && bytes.length > 0) {
            try {
                deserialize(bytes, items);
            } catch (IOException | ClassNotFoundException ex) {
                logger.log(Level.WARNING,
                        "Failed to read equipped artifacts for " + player.getName() + "; starting empty.", ex);
            }
        }
        store.put(player.getUniqueId(), items);
    }

    /** Persists the player's equipped artifacts back into their PDC. */
    public void save(Player player) {
        ItemStack[] items = store.get(player.getUniqueId());
        if (items == null) {
            return;
        }
        try {
            player.getPersistentDataContainer()
                    .set(ArtifactKeys.EQUIPMENT, PersistentDataType.BYTE_ARRAY, serialize(items));
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to save equipped artifacts for " + player.getName() + ".", ex);
        }
    }

    /** Drops the in-memory copy for a player (after a final {@link #save}). */
    public void unload(UUID playerId) {
        store.remove(playerId);
    }

    private ItemStack[] items(Player player) {
        return store.computeIfAbsent(player.getUniqueId(), id -> new ItemStack[ArtifactSlots.SLOT_COUNT]);
    }

    /** The (possibly null) artifact item equipped in the given slot. */
    public ItemStack getItem(Player player, int index) {
        ItemStack item = items(player)[index];
        return item == null ? null : item.clone();
    }

    /** Whether the given slot is empty. */
    public boolean isEmpty(Player player, int index) {
        ItemStack item = items(player)[index];
        return item == null || item.getType().isAir();
    }

    /** Sets (or clears, when {@code item} is null/air) the slot and persists. */
    public void setItem(Player player, int index, ItemStack item) {
        items(player)[index] = (item == null || item.getType().isAir()) ? null : item.clone();
        save(player);
    }

    /** The set of artifact types currently equipped by the player. */
    public Set<ArtifactType> activeTypes(Player player) {
        Set<ArtifactType> active = EnumSet.noneOf(ArtifactType.class);
        for (ItemStack item : items(player)) {
            ArtifactItem.typeOf(item).ifPresent(active::add);
        }
        return active;
    }

    /** Whether the player currently has the given artifact equipped. */
    public boolean carries(Player player, ArtifactType type) {
        for (ItemStack item : items(player)) {
            Optional<ArtifactType> t = ArtifactItem.typeOf(item);
            if (t.isPresent() && t.get() == type) {
                return true;
            }
        }
        return false;
    }

    private static byte[] serialize(ItemStack[] items) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream data = new BukkitObjectOutputStream(out)) {
            data.writeInt(items.length);
            for (ItemStack item : items) {
                boolean present = item != null && !item.getType().isAir();
                data.writeBoolean(present);
                if (present) {
                    data.writeObject(item);
                }
            }
        }
        return out.toByteArray();
    }

    private static void deserialize(byte[] bytes, ItemStack[] target) throws IOException, ClassNotFoundException {
        try (BukkitObjectInputStream data = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
            int count = data.readInt();
            for (int i = 0; i < count; i++) {
                boolean present = data.readBoolean();
                ItemStack item = present ? (ItemStack) data.readObject() : null;
                if (i < target.length) {
                    target[i] = item;
                }
            }
        }
    }
}
