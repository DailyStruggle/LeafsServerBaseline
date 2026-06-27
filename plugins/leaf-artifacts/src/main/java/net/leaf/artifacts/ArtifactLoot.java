package net.leaf.artifacts;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Decides whether (and which) artifact a given loot source yields, and builds the
 * artifact item via {@link ArtifactItem#create(ArtifactType)} so there is a single
 * source of truth for an artifact's identity (no duplicated loot-table JSON).
 *
 * <p>Two channels, matching {@code docs/scratch/ARTIFACTS-VANILLA-RECREATION.md}:</p>
 * <ul>
 *   <li><b>Structure chests</b> - keyed off the container's loot-table id (handled
 *       via {@code LootGenerateEvent}). Because a structure only generates in its
 *       own biome(s), the loot-table id already encodes the structure (and hence
 *       biome), so no runtime biome lookup is needed. Two axes drive the result:
 *       <ol>
 *         <li><b>Structure base tier</b> - an ordered {@link StructureRule} match
 *             on the id (namespace + token), covering vanilla and every bundled
 *             third-party structure pack, with a {@code chest} catch-all so any
 *             future pack chest still rewards exploration.</li>
 *         <li><b>Per-chest rarity shift</b> - a single +/-1 tier shift read from
 *             keywords in the chest's leaf segment (e.g. {@code treasure}/{@code big}
 *             bump up, {@code common}/{@code barrel} bump down), so a "treasure"
 *             chest in a structure outranks its "common" chest.</li>
 *       </ol>
 *       The two axes combine and clamp to {@code COMMON..SPECIAL}.</li>
 *   <li><b>Mob drops</b> - a curated set of hostile {@link EntityType}s (handled
 *       via {@code EntityDeathEvent}). Biome bosses are NOT here: their artifacts
 *       ship through the {@code leaf-bosses} datapack and are tier {@code BOSS}.</li>
 * </ul>
 */
public final class ArtifactLoot {

    private ArtifactLoot() {
    }

    /**
     * A structure base-tier rule: matches when the (lower-cased) loot-table id
     * contains <em>all</em> of {@link #tokens}. Ordered most-specific first.
     */
    private record StructureRule(List<String> tokens, ArtifactLootTier base) {
        boolean matches(String id) {
            for (String token : tokens) {
                if (!id.contains(token)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Ordered structure base-tier rules (axis 1). The first whose tokens are all
     * contained in the loot-table id wins. Pack-namespace rules come before the
     * generic vanilla tokens so e.g. a Dungeons-and-Taverns mansion chest named
     * {@code ancient_city_raid_chest} is read as the mansion (RARE), not wrongly
     * promoted by the bare {@code ancient_city} substring.
     */
    private static final List<StructureRule> STRUCTURE_RULES = List.of(
            // --- third-party packs (match by namespace, with sub-structure tokens) ---
            // Dungeons and Taverns (namespace nova_structures)
            new StructureRule(List.of("nova_structures", "illager_mansion"), ArtifactLootTier.RARE),
            new StructureRule(List.of("nova_structures", "lone_citadel"), ArtifactLootTier.RARE),
            new StructureRule(List.of("nova_structures"), ArtifactLootTier.UNCOMMON),
            // When the Dungeons (Seven Seas)
            new StructureRule(List.of("dungeons_arise_seven_seas"), ArtifactLootTier.UNCOMMON),
            // When Dungeons Arise (big landmark dungeons; mineshaft-like "mines" demoted)
            new StructureRule(List.of("dungeons_arise", "mines"), ArtifactLootTier.COMMON),
            new StructureRule(List.of("dungeons_arise"), ArtifactLootTier.RARE),
            // YUNG's Better Strongholds / Dungeons
            new StructureRule(List.of("betterstrongholds"), ArtifactLootTier.RARE),
            new StructureRule(List.of("betterdungeons"), ArtifactLootTier.UNCOMMON),
            // Structory Towers (end tower is the standout)
            new StructureRule(List.of("structory_towers", "end_tower"), ArtifactLootTier.RARE),
            new StructureRule(List.of("structory_towers"), ArtifactLootTier.UNCOMMON),
            // Structory
            new StructureRule(List.of("structory"), ArtifactLootTier.UNCOMMON),
            // Towns and Towers (namespace kaisyn): outposts/ruins above plain villages
            new StructureRule(List.of("kaisyn", "outpost"), ArtifactLootTier.UNCOMMON),
            new StructureRule(List.of("kaisyn", "archeology"), ArtifactLootTier.UNCOMMON),
            new StructureRule(List.of("kaisyn"), ArtifactLootTier.COMMON),
            // ChoiceTheorem's Overhauled Village, Explorify, Hopo Better Mineshaft, Stoneholm
            new StructureRule(List.of("ctov"), ArtifactLootTier.COMMON),
            new StructureRule(List.of("explorify"), ArtifactLootTier.COMMON),
            new StructureRule(List.of("hopo"), ArtifactLootTier.COMMON),
            new StructureRule(List.of("stoneholm"), ArtifactLootTier.COMMON),
            // --- vanilla structures (minecraft: namespace) ---
            new StructureRule(List.of("ancient_city"), ArtifactLootTier.SPECIAL),
            new StructureRule(List.of("trial_chamber"), ArtifactLootTier.SPECIAL),
            new StructureRule(List.of("end_city"), ArtifactLootTier.SPECIAL),
            new StructureRule(List.of("bastion"), ArtifactLootTier.SPECIAL),
            new StructureRule(List.of("stronghold"), ArtifactLootTier.RARE),
            new StructureRule(List.of("mansion"), ArtifactLootTier.RARE),
            new StructureRule(List.of("pillager_outpost"), ArtifactLootTier.UNCOMMON),
            new StructureRule(List.of("desert_pyramid"), ArtifactLootTier.UNCOMMON),
            new StructureRule(List.of("jungle_temple"), ArtifactLootTier.UNCOMMON),
            new StructureRule(List.of("jungle_pyramid"), ArtifactLootTier.UNCOMMON),
            new StructureRule(List.of("buried_treasure"), ArtifactLootTier.UNCOMMON),
            new StructureRule(List.of("nether_fortress"), ArtifactLootTier.UNCOMMON),
            new StructureRule(List.of("igloo"), ArtifactLootTier.UNCOMMON),
            new StructureRule(List.of("mineshaft"), ArtifactLootTier.COMMON),
            new StructureRule(List.of("shipwreck"), ArtifactLootTier.COMMON),
            new StructureRule(List.of("underwater_ruin"), ArtifactLootTier.COMMON),
            new StructureRule(List.of("ocean_ruin"), ArtifactLootTier.COMMON),
            // --- catch-all: any other loot table that looks like a structure chest ---
            new StructureRule(List.of("chest"), ArtifactLootTier.COMMON));

    /** Per-chest keywords that bump the structure base tier one step rarer (axis 2). */
    private static final List<String> RARITY_UP = List.of(
            "treasure", "treasury", "big", "grand", "vault", "boss", "special", "high", "rare", "top");

    /** Per-chest keywords that bump the structure base tier one step more common (axis 2). */
    private static final List<String> RARITY_DOWN = List.of(
            "common", "small", "barrel", "normal", "supply", "loot_piles", "mess", "food", "low");

    /** Per-final-tier chance that a matched container actually yields an artifact. */
    private static double chanceFor(ArtifactLootTier tier) {
        return switch (tier) {
            case COMMON -> 0.03D;
            case UNCOMMON -> 0.05D;
            case RARE -> 0.05D;
            case SPECIAL -> 0.05D;
            case BOSS -> 0.0D;
        };
    }

    /** A mob loot rule: the tier it grants and the per-kill chance. */
    private record MobRule(ArtifactLootTier tier, double chance) {
    }

    /**
     * Lazy holder for the curated hostile-mob rules. Kept out of {@link ArtifactLoot}'s
     * own static init so that loading the class for the pure {@link #classifyChest}
     * logic never touches the {@link EntityType} registry (which is unavailable off a
     * running server, e.g. in unit tests). Initialised on first {@link #rollForMob}.
     */
    private static final class MobRules {
        static final Map<EntityType, MobRule> MAP = buildMobRules();
    }

    private static Map<EntityType, MobRule> buildMobRules() {
        Map<EntityType, MobRule> map = new LinkedHashMap<>();
        map.put(EntityType.EVOKER, new MobRule(ArtifactLootTier.RARE, 0.15D));
        map.put(EntityType.ILLUSIONER, new MobRule(ArtifactLootTier.RARE, 0.15D));
        map.put(EntityType.RAVAGER, new MobRule(ArtifactLootTier.UNCOMMON, 0.10D));
        map.put(EntityType.VINDICATOR, new MobRule(ArtifactLootTier.UNCOMMON, 0.04D));
        map.put(EntityType.PILLAGER, new MobRule(ArtifactLootTier.COMMON, 0.02D));
        map.put(EntityType.WITCH, new MobRule(ArtifactLootTier.COMMON, 0.03D));
        return map;
    }

    /**
     * Rolls an artifact for a structure container identified by its loot-table id.
     * Returns empty when no structure rule matches or the chance roll fails.
     */
    public static Optional<ItemStack> rollForChest(NamespacedKey lootTable) {
        if (lootTable == null) {
            return Optional.empty();
        }
        ArtifactLootTier tier = classifyChest(lootTable.toString().toLowerCase(Locale.ROOT));
        if (tier == null) {
            return Optional.empty();
        }
        return rollTier(tier);
    }

    /**
     * Pure (randomness-free) two-axis classification of a lower-cased loot-table id:
     * the matched {@link StructureRule} base tier shifted by the chest's leaf rarity
     * keywords and clamped to {@code COMMON..SPECIAL}. Returns {@code null} when no
     * structure rule matches (the container is not treated as a structure chest).
     * Package-private so the tier table can be unit-tested without rolling chance.
     */
    static ArtifactLootTier classifyChest(String id) {
        ArtifactLootTier base = null;
        for (StructureRule rule : STRUCTURE_RULES) {
            if (rule.matches(id)) {
                base = rule.base();
                break;
            }
        }
        if (base == null) {
            return null;
        }
        return applyRarityShift(base, leafOf(id));
    }

    /** Rolls an artifact for a slain mob, or empty when the mob has no rule / fails the roll. */
    public static Optional<ItemStack> rollForMob(EntityType type) {
        MobRule rule = MobRules.MAP.get(type);
        if (rule == null) {
            return Optional.empty();
        }
        if (ThreadLocalRandom.current().nextDouble() >= rule.chance()) {
            return Optional.empty();
        }
        return pick(rule.tier());
    }

    /** The last '/'-separated path segment of the loot-table id (the chest's own name). */
    private static String leafOf(String id) {
        int slash = id.lastIndexOf('/');
        return slash < 0 ? id : id.substring(slash + 1);
    }

    /**
     * Applies a single +/-1 tier shift from the chest's leaf keywords, clamped to
     * {@code COMMON..SPECIAL}. Up keywords win ties; at most one step either way.
     */
    private static ArtifactLootTier applyRarityShift(ArtifactLootTier base, String leaf) {
        int shift = 0;
        for (String up : RARITY_UP) {
            if (leaf.contains(up)) {
                shift = 1;
                break;
            }
        }
        if (shift == 0) {
            for (String down : RARITY_DOWN) {
                if (leaf.contains(down)) {
                    shift = -1;
                    break;
                }
            }
        }
        int min = ArtifactLootTier.COMMON.ordinal();
        int max = ArtifactLootTier.SPECIAL.ordinal();
        int idx = Math.max(min, Math.min(max, base.ordinal() + shift));
        return ArtifactLootTier.values()[idx];
    }

    /** Rolls the per-tier chance then picks an artifact of that tier. */
    private static Optional<ItemStack> rollTier(ArtifactLootTier tier) {
        if (ThreadLocalRandom.current().nextDouble() >= chanceFor(tier)) {
            return Optional.empty();
        }
        return pick(tier);
    }

    private static Optional<ItemStack> pick(ArtifactLootTier tier) {
        List<ArtifactType> pool = ArtifactLootTier.artifactsOf(tier);
        if (pool.isEmpty()) {
            return Optional.empty();
        }
        ArtifactType chosen = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        return Optional.of(ArtifactItem.create(chosen));
    }
}
