package net.leaf.curios.internal;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Per-player store of equipped curios, keyed by slot type id. Each slot id maps
 * to a fixed-size array (the slot type's {@code size}); entries may be null.
 *
 * <p>The map is serialized into the player's {@code PersistentDataContainer} so
 * it survives relog/restart. All per-player access happens on that player's
 * region thread, so each player's map is effectively single-threaded; the outer
 * map is concurrent only to tolerate overlapping players.</p>
 */
public final class CurioStorage {

    private final Map<UUID, Map<String, ItemStack[]>> store = new ConcurrentHashMap<>();
    private final SlotRegistry registry;
    private final NamespacedKey key;
    private final Logger logger;

    public CurioStorage(SlotRegistry registry, NamespacedKey key, Logger logger) {
        this.registry = registry;
        this.key = key;
        this.logger = logger;
    }

    public void load(Player player) {
        Map<String, ItemStack[]> data = new HashMap<>();
        byte[] bytes = player.getPersistentDataContainer().get(key, PersistentDataType.BYTE_ARRAY);
        if (bytes != null && bytes.length > 0) {
            try {
                deserialize(bytes, data);
            } catch (IOException | ClassNotFoundException ex) {
                logger.log(Level.WARNING,
                        "Failed to read curios for " + player.getName() + "; starting empty.", ex);
            }
        }
        store.put(player.getUniqueId(), data);
    }

    public void save(Player player) {
        Map<String, ItemStack[]> data = store.get(player.getUniqueId());
        if (data == null) {
            return;
        }
        try {
            player.getPersistentDataContainer().set(
                    key, PersistentDataType.BYTE_ARRAY, serialize(data));
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to save curios for " + player.getName() + ".", ex);
        }
    }

    public void unload(UUID playerId) {
        store.remove(playerId);
    }

    private Map<String, ItemStack[]> data(Player player) {
        return store.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>());
    }

    /** Size declared by the slot type, or 1 if the slot is no longer registered. */
    private int sizeOf(String slotId) {
        return registry.get(slotId).map(t -> t.size()).orElse(1);
    }

    /** The (resized-to-current-size) array for a slot id. Never null. */
    public ItemStack[] array(Player player, String slotId) {
        String id = slotId.toLowerCase(Locale.ROOT);
        Map<String, ItemStack[]> data = data(player);
        int size = sizeOf(id);
        ItemStack[] current = data.get(id);
        if (current == null) {
            current = new ItemStack[size];
            data.put(id, current);
        } else if (current.length != size) {
            ItemStack[] resized = new ItemStack[size];
            System.arraycopy(current, 0, resized, 0, Math.min(current.length, size));
            current = resized;
            data.put(id, current);
        }
        return current;
    }

    public ItemStack getItem(Player player, String slotId, int index) {
        ItemStack[] arr = array(player, slotId);
        if (index < 0 || index >= arr.length) {
            return null;
        }
        ItemStack item = arr[index];
        return item == null ? null : item.clone();
    }

    public boolean isEmpty(Player player, String slotId, int index) {
        ItemStack[] arr = array(player, slotId);
        if (index < 0 || index >= arr.length) {
            return true;
        }
        ItemStack item = arr[index];
        return item == null || item.getType().isAir();
    }

    public void set(Player player, String slotId, int index, ItemStack item) {
        ItemStack[] arr = array(player, slotId);
        if (index < 0 || index >= arr.length) {
            return;
        }
        arr[index] = (item == null || item.getType().isAir()) ? null : item.clone();
        save(player);
    }

    /** Every non-null curio across all the player's slots (clones). */
    public List<ItemStack> allItems(Player player) {
        List<ItemStack> out = new ArrayList<>();
        Map<String, ItemStack[]> data = store.get(player.getUniqueId());
        if (data == null) {
            return out;
        }
        for (ItemStack[] arr : data.values()) {
            for (ItemStack item : arr) {
                if (item != null && !item.getType().isAir()) {
                    out.add(item.clone());
                }
            }
        }
        return out;
    }

    private static byte[] serialize(Map<String, ItemStack[]> data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream stream = new BukkitObjectOutputStream(out)) {
            stream.writeInt(data.size());
            for (Map.Entry<String, ItemStack[]> entry : data.entrySet()) {
                stream.writeUTF(entry.getKey());
                ItemStack[] arr = entry.getValue();
                stream.writeInt(arr.length);
                for (ItemStack item : arr) {
                    boolean present = item != null && !item.getType().isAir();
                    stream.writeBoolean(present);
                    if (present) {
                        stream.writeObject(item);
                    }
                }
            }
        }
        return out.toByteArray();
    }

    private static void deserialize(byte[] bytes, Map<String, ItemStack[]> target)
            throws IOException, ClassNotFoundException {
        try (BukkitObjectInputStream stream = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
            int slots = stream.readInt();
            for (int s = 0; s < slots; s++) {
                String id = stream.readUTF();
                int len = stream.readInt();
                ItemStack[] arr = new ItemStack[len];
                for (int i = 0; i < len; i++) {
                    boolean present = stream.readBoolean();
                    arr[i] = present ? (ItemStack) stream.readObject() : null;
                }
                target.put(id, arr);
            }
        }
    }
}
