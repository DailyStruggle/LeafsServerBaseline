package net.leaf.backpacks;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Off-item persistence for backpack contents and installed upgrades, keyed by
 * the backpack's storage UUID. Keeping the data off the item makes duplication
 * impossible: a copied item just references the same logical storage.
 *
 * <p>Backed by a single YAML file ({@code data.yml}) in the plugin data folder.</p>
 */
public final class BackpackStore {

    public static final int CONTENTS_SIZE = 27;
    public static final int MAX_UPGRADE_SLOTS = BackpackConfig.MAX_TIER;

    private final File file;
    private final Logger logger;
    private final Map<UUID, ItemStack[]> contents = new HashMap<>();
    private final Map<UUID, ItemStack[]> upgrades = new HashMap<>();

    public BackpackStore(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "data.yml");
        this.logger = logger;
    }

    public synchronized void load() {
        contents.clear();
        upgrades.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("backpacks");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            ItemStack[] c = toArray(root.getList(key + ".contents"), CONTENTS_SIZE);
            ItemStack[] u = toArray(root.getList(key + ".upgrades"), MAX_UPGRADE_SLOTS);
            contents.put(id, c);
            upgrades.put(id, u);
        }
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, ItemStack[]> e : contents.entrySet()) {
            String base = "backpacks." + e.getKey();
            yaml.set(base + ".contents", toList(e.getValue()));
            yaml.set(base + ".upgrades", toList(upgrades.getOrDefault(e.getKey(), new ItemStack[MAX_UPGRADE_SLOTS])));
        }
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            yaml.save(file);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to save LeafBackpacks data", ex);
        }
    }

    public synchronized ItemStack[] getContents(UUID id) {
        return contents.computeIfAbsent(id, k -> new ItemStack[CONTENTS_SIZE]).clone();
    }

    public synchronized void setContents(UUID id, ItemStack[] items) {
        contents.put(id, normalise(items, CONTENTS_SIZE));
    }

    public synchronized ItemStack[] getUpgrades(UUID id) {
        return upgrades.computeIfAbsent(id, k -> new ItemStack[MAX_UPGRADE_SLOTS]).clone();
    }

    public synchronized void setUpgrades(UUID id, ItemStack[] items) {
        upgrades.put(id, normalise(items, MAX_UPGRADE_SLOTS));
    }

    private static ItemStack[] normalise(ItemStack[] items, int size) {
        ItemStack[] out = new ItemStack[size];
        if (items != null) {
            System.arraycopy(items, 0, out, 0, Math.min(size, items.length));
        }
        return out;
    }

    private static List<ItemStack> toList(ItemStack[] arr) {
        List<ItemStack> list = new ArrayList<>(arr.length);
        for (ItemStack item : arr) {
            list.add(item); // nulls preserved to keep slot positions
        }
        return list;
    }

    private static ItemStack[] toArray(List<?> list, int size) {
        ItemStack[] arr = new ItemStack[size];
        if (list == null) {
            return arr;
        }
        for (int i = 0; i < Math.min(size, list.size()); i++) {
            Object o = list.get(i);
            if (o instanceof ItemStack stack) {
                arr[i] = stack;
            }
        }
        return arr;
    }
}
