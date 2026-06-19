package net.leaf.mobs;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads and holds the mob reference table from {@code mobs.yml}.
 *
 * <p>The table is the tunable surface of the framework: each top-level key under
 * {@code mobs:} is one {@link MobDefinition}. Reloading re-parses the file so values can
 * be tweaked live (e.g. with AI assistance) without a restart. The registry is the only
 * place that knows how definitions are stored; the rest of the plugin asks it by id or
 * biome.</p>
 */
public final class MobRegistry {

    private static final String FILE_NAME = "mobs.yml";

    private final Plugin plugin;
    private final Logger log;
    private final Map<String, MobDefinition> byId = new LinkedHashMap<>();

    public MobRegistry(Plugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    /**
     * (Re)loads the reference table from disk, copying the bundled default on first run.
     *
     * @return the number of definitions successfully loaded.
     */
    public int reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection mobs = yaml.getConfigurationSection("mobs");

        byId.clear();
        if (mobs == null) {
            log.warning("[leaf-mobs] " + FILE_NAME + " has no 'mobs' section; no mobs loaded.");
            return 0;
        }

        for (String id : mobs.getKeys(false)) {
            ConfigurationSection s = mobs.getConfigurationSection(id);
            if (s == null) {
                log.warning("[leaf-mobs] mob '" + id + "' is not a section; skipped.");
                continue;
            }
            MobDefinition def = MobDefinition.parse(id.toLowerCase(), s, log);
            if (def != null) {
                byId.put(def.id(), def);
            }
        }
        log.info("[leaf-mobs] loaded " + byId.size() + " mob definition(s) from " + FILE_NAME);
        return byId.size();
    }

    public MobDefinition byId(String id) {
        return id == null ? null : byId.get(id.toLowerCase());
    }

    public Collection<MobDefinition> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public List<String> ids() {
        return new ArrayList<>(byId.keySet());
    }

    /** All definitions eligible to spawn in the given biome (empty-biome rows match any). */
    public List<MobDefinition> byBiome(NamespacedKey biome) {
        List<MobDefinition> out = new ArrayList<>();
        for (MobDefinition def : byId.values()) {
            if (def.appliesToBiome(biome)) {
                out.add(def);
            }
        }
        return out;
    }

    public int size() {
        return byId.size();
    }
}
