package net.leaf.mobs;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Immutable, data-driven description of one managed mob, parsed from a reference-table
 * entry (see {@code mobs.yml}). This is the heart of the framework: a new mob is a new
 * table row, not new code. Unknown/optional fields degrade gracefully so the table stays
 * easy to tune (including by AI assistance).
 *
 * <p>Attributes are keyed by their registry id (e.g. {@code minecraft:max_health}) and
 * resolved via {@link Registry#ATTRIBUTE}, which is stable across the enum-to-interface
 * change in recent API versions. Loot-table and advancement ids are captured now but only
 * consumed by later phases (rewards).</p>
 */
public final class MobDefinition {

    /**
     * Natural-spawn transform rule, parsed from the {@code spawn:} sub-section (F4). When a
     * vanilla mob of one of {@link #replaceTypes} spawns for an allowed {@link #reasons} in a
     * matching biome, the spawn service rolls {@link #chance}; on success it cancels the
     * vanilla spawn and re-spawns this definition instead, subject to a per-mob
     * {@link #cooldownMillis} and a {@link #maxLive} live-count cap (anti-farm). A row with no
     * {@code spawn:} section, or {@code enabled: false}, never spawns naturally (admin/command
     * only).
     */
    public record SpawnRule(
            boolean enabled,
            Set<EntityType> replaceTypes,
            Set<CreatureSpawnEvent.SpawnReason> reasons,
            double chance,
            long cooldownMillis,
            int maxLive) {

        /** A disabled rule (never spawns naturally). */
        public static final SpawnRule DISABLED = new SpawnRule(
                false, Set.of(), Set.of(), 0.0, 0L, 0);

        /** True if this base entity type and spawn reason are eligible to transform. */
        public boolean matches(EntityType type, CreatureSpawnEvent.SpawnReason reason) {
            return enabled && replaceTypes.contains(type) && reasons.contains(reason);
        }
    }

    /** Equipment + drop-chance, parsed from the {@code equipment} sub-section. */
    public record EquipmentSpec(
            Material helmet,
            Material chestplate,
            Material leggings,
            Material boots,
            Material mainHand,
            Material offHand,
            float dropChance) {
    }

    private final String id;
    private final EntityType entityType;
    private final String displayName;
    private final MobTier tier;
    private final Set<NamespacedKey> biomes;
    private final Map<Attribute, Double> attributes;
    private final EquipmentSpec equipment;
    private final boolean persistent;
    private final boolean glowing;
    private final String lootTable;
    private final String advancement;
    private final boolean clearVanillaDrops;
    private final int xpOverride;
    private final List<AbilitySpec> abilities;
    private final SpawnRule spawnRule;

    private MobDefinition(String id, EntityType entityType, String displayName, MobTier tier,
                          Set<NamespacedKey> biomes, Map<Attribute, Double> attributes,
                          EquipmentSpec equipment, boolean persistent, boolean glowing,
                          String lootTable, String advancement, boolean clearVanillaDrops,
                          int xpOverride, List<AbilitySpec> abilities,
                          SpawnRule spawnRule) {
        this.id = id;
        this.entityType = entityType;
        this.displayName = displayName;
        this.tier = tier;
        this.biomes = biomes;
        this.attributes = attributes;
        this.equipment = equipment;
        this.persistent = persistent;
        this.glowing = glowing;
        this.lootTable = lootTable;
        this.advancement = advancement;
        this.clearVanillaDrops = clearVanillaDrops;
        this.xpOverride = xpOverride;
        this.abilities = abilities;
        this.spawnRule = spawnRule;
    }

    /**
     * Parses a single reference-table entry. Returns {@code null} (and logs) when the row
     * is unusable (missing/invalid {@code entity}); other invalid fields are skipped so a
     * typo in one attribute does not discard the whole mob.
     */
    public static MobDefinition parse(String id, ConfigurationSection s, Logger log) {
        EntityType type = parseEntityType(s.getString("entity"));
        if (type == null || !type.isAlive() || !type.isSpawnable()) {
            log.warning("[leaf-mobs] mob '" + id + "' skipped: missing/invalid 'entity' ("
                    + s.getString("entity") + ")");
            return null;
        }

        MobTier tier = MobTier.fromConfig(s.getString("tier"), MobTier.COMMON);
        String displayName = s.getString("display_name", null);

        Set<NamespacedKey> biomes = new LinkedHashSet<>();
        for (String b : s.getStringList("biomes")) {
            NamespacedKey key = NamespacedKey.fromString(b.trim().toLowerCase());
            if (key != null) {
                biomes.add(key);
            } else {
                log.warning("[leaf-mobs] mob '" + id + "': ignoring invalid biome key '" + b + "'");
            }
        }

        Map<Attribute, Double> attributes = parseAttributes(id, s.getConfigurationSection("attributes"), log);
        EquipmentSpec equipment = parseEquipment(s.getConfigurationSection("equipment"));
        boolean persistent = s.getBoolean("persistent", tier.isPersistentByDefault());
        boolean glowing = s.getBoolean("glowing", false);
        String lootTable = s.getString("loot_table", null);
        String advancement = s.getString("advancement", null);
        boolean clearVanillaDrops = s.getBoolean("clear_vanilla_drops", false);
        int xpOverride = s.getInt("xp", -1);
        List<AbilitySpec> abilities = AbilitySpec.parseList(id, s, log);
        SpawnRule spawnRule = parseSpawnRule(id, type, s.getConfigurationSection("spawn"), log);

        return new MobDefinition(id, type, displayName, tier, biomes, attributes, equipment,
                persistent, glowing, lootTable, advancement, clearVanillaDrops, xpOverride,
                abilities, spawnRule);
    }

    /**
     * Parses the optional {@code spawn:} sub-section into a {@link SpawnRule}. Missing section
     * or {@code enabled: false} yields {@link SpawnRule#DISABLED}. {@code replace} defaults to
     * the mob's own base {@code entity} type, and {@code reasons} defaults to {@code NATURAL}.
     */
    private static SpawnRule parseSpawnRule(String id, EntityType ownType,
                                            ConfigurationSection s, Logger log) {
        if (s == null || !s.getBoolean("enabled", true)) {
            return SpawnRule.DISABLED;
        }

        Set<EntityType> replace = new LinkedHashSet<>();
        List<String> replaceRaw = s.getStringList("replace");
        if (replaceRaw.isEmpty()) {
            replace.add(ownType);
        } else {
            for (String r : replaceRaw) {
                EntityType t = parseEntityType(r);
                if (t != null) {
                    replace.add(t);
                } else {
                    log.warning("[leaf-mobs] mob '" + id + "': ignoring invalid spawn.replace type '" + r + "'");
                }
            }
            if (replace.isEmpty()) {
                replace.add(ownType);
            }
        }

        Set<CreatureSpawnEvent.SpawnReason> reasons = EnumSet.noneOf(CreatureSpawnEvent.SpawnReason.class);
        List<String> reasonRaw = s.getStringList("reasons");
        for (String r : reasonRaw) {
            try {
                reasons.add(CreatureSpawnEvent.SpawnReason.valueOf(r.trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                log.warning("[leaf-mobs] mob '" + id + "': ignoring invalid spawn.reason '" + r + "'");
            }
        }
        if (reasons.isEmpty()) {
            reasons.add(CreatureSpawnEvent.SpawnReason.NATURAL);
        }

        double chance = Math.max(0.0, Math.min(1.0, s.getDouble("chance", 0.0)));
        long cooldownMillis = Math.max(0L, Math.round(s.getDouble("cooldown_s", 0.0) * 1000.0));
        int maxLive = Math.max(0, s.getInt("max_live", 1));

        if (chance <= 0.0) {
            log.warning("[leaf-mobs] mob '" + id + "': spawn.chance is 0; it will never spawn naturally.");
        }
        return new SpawnRule(true, replace, reasons, chance, cooldownMillis, maxLive);
    }

    private static EntityType parseEntityType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return EntityType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Map<Attribute, Double> parseAttributes(String id, ConfigurationSection s, Logger log) {
        Map<Attribute, Double> out = new LinkedHashMap<>();
        if (s == null) {
            return out;
        }
        for (String key : s.getKeys(false)) {
            NamespacedKey nk = NamespacedKey.fromString(key.trim().toLowerCase());
            Attribute attr = nk == null ? null : Registry.ATTRIBUTE.get(nk);
            if (attr == null) {
                log.warning("[leaf-mobs] mob '" + id + "': ignoring unknown attribute '" + key + "'");
                continue;
            }
            out.put(attr, s.getDouble(key));
        }
        return out;
    }

    private static EquipmentSpec parseEquipment(ConfigurationSection s) {
        if (s == null) {
            return null;
        }
        float drop = (float) s.getDouble("drop_chance", 0.0);
        return new EquipmentSpec(
                material(s.getString("helmet")),
                material(s.getString("chestplate")),
                material(s.getString("leggings")),
                material(s.getString("boots")),
                material(s.getString("main_hand")),
                material(s.getString("off_hand")),
                Math.max(0f, Math.min(1f, drop)));
    }

    private static Material material(String raw) {
        return raw == null ? null : Material.matchMaterial(raw.trim());
    }

    public String id() {
        return id;
    }

    public EntityType entityType() {
        return entityType;
    }

    public String displayName() {
        return displayName;
    }

    public MobTier tier() {
        return tier;
    }

    public Set<NamespacedKey> biomes() {
        return biomes;
    }

    public Map<Attribute, Double> attributes() {
        return attributes;
    }

    public EquipmentSpec equipment() {
        return equipment;
    }

    public boolean persistent() {
        return persistent;
    }

    public boolean glowing() {
        return glowing;
    }

    public String lootTable() {
        return lootTable;
    }

    public String advancement() {
        return advancement;
    }

    /** When true, the F5 reward service removes the entity's vanilla drops before adding loot. */
    public boolean clearVanillaDrops() {
        return clearVanillaDrops;
    }

    /** Dropped-XP override ({@code -1} = leave the vanilla amount untouched). */
    public int xpOverride() {
        return xpOverride;
    }

    /** The mob's ability slots (may be empty); consumed by the runtime {@link MobController}. */
    public List<AbilitySpec> abilities() {
        return abilities;
    }

    /** The natural-spawn transform rule (F4); {@link SpawnRule#DISABLED} when the row opts out. */
    public SpawnRule spawnRule() {
        return spawnRule;
    }

    public boolean appliesToBiome(NamespacedKey biome) {
        return biomes.isEmpty() || biomes.contains(biome);
    }

    @Override
    public String toString() {
        return "MobDefinition{" + id + ", " + entityType + ", " + tier
                + ", biomes=" + biomes + ", attrs=" + attributes.size() + "}";
    }

    /** Convenience for callers that want a stable, ordered key list. */
    public static List<String> attributeKeysOf(MobDefinition def) {
        return def.attributes.keySet().stream().map(a -> a.getKey().toString()).toList();
    }
}
