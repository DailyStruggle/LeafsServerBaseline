package net.leaf.artifacts;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Rarity buckets used to source artifacts into structure-chest and mob loot.
 *
 * <p>Tiers mirror the skewed tier list in
 * {@code docs/scratch/ARTIFACTS-VANILLA-RECREATION.md}. Each {@link ArtifactType}
 * maps to exactly one tier, except artifacts that are reserved for biome bosses
 * (delivered by the {@code leaf-bosses} datapack) which map to {@link #BOSS} and
 * are intentionally excluded from this plugin's structure/mob loot injection so
 * the two channels never double up.</p>
 */
public enum ArtifactLootTier {

    COMMON,
    UNCOMMON,
    RARE,
    SPECIAL,
    /** Reserved for {@code leaf-bosses} datapack drops; never injected by this plugin. */
    BOSS;

    /**
     * Lazy holder for the artifact->tier map. Kept out of the enum's own static init
     * so that code needing only the tier ordering (e.g. the structure-chest
     * classifier) can load {@link ArtifactLootTier} without forcing {@link ArtifactType}
     * (and its Bukkit {@code Material}/{@code PotionEffectType} references) to initialise.
     */
    private static final class Tiers {
        static final Map<ArtifactType, ArtifactLootTier> MAP = buildTierMap();
    }

    /** The loot tier of an artifact, or {@link #BOSS} when it is boss-only. */
    public static ArtifactLootTier of(ArtifactType type) {
        return Tiers.MAP.getOrDefault(type, ArtifactLootTier.RARE);
    }

    /** All artifacts assigned to the given tier (empty for {@link #BOSS} consumers). */
    public static List<ArtifactType> artifactsOf(ArtifactLootTier tier) {
        List<ArtifactType> result = new ArrayList<>();
        for (Map.Entry<ArtifactType, ArtifactLootTier> entry : Tiers.MAP.entrySet()) {
            if (entry.getValue() == tier) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private static Map<ArtifactType, ArtifactLootTier> buildTierMap() {
        Map<ArtifactType, ArtifactLootTier> map = new EnumMap<>(ArtifactType.class);

        // common
        map.put(ArtifactType.SNORKEL, COMMON);
        map.put(ArtifactType.NIGHT_VISION_GOGGLES, COMMON);
        map.put(ArtifactType.ONION_RING, COMMON);
        map.put(ArtifactType.OBSIDIAN_SKULL, COMMON);
        map.put(ArtifactType.PANIC_NECKLACE, COMMON);
        map.put(ArtifactType.THORN_PENDANT, COMMON);

        // uncommon
        map.put(ArtifactType.RUNNING_SHOES, UNCOMMON);
        map.put(ArtifactType.FLIPPERS, UNCOMMON);
        map.put(ArtifactType.CHARM_OF_SINKING, UNCOMMON);
        map.put(ArtifactType.SHOCK_PENDANT, UNCOMMON);
        map.put(ArtifactType.FLAME_PENDANT, UNCOMMON);
        map.put(ArtifactType.DIGGING_CLAWS, UNCOMMON);
        map.put(ArtifactType.SUPERSTITIOUS_HAT, UNCOMMON);
        map.put(ArtifactType.ANGLERS_HAT, UNCOMMON);
        map.put(ArtifactType.ETERNAL_STEAK, UNCOMMON);
        map.put(ArtifactType.GOLDEN_HOOK, UNCOMMON);
        map.put(ArtifactType.LUCKY_SCARF, UNCOMMON);
        map.put(ArtifactType.CROSS_NECKLACE, UNCOMMON);
        map.put(ArtifactType.ANTIDOTE_VESSEL, UNCOMMON);
        map.put(ArtifactType.STEADFAST_SPIKES, UNCOMMON);
        map.put(ArtifactType.BUNNY_HOPPERS, UNCOMMON);

        // rare
        map.put(ArtifactType.ROLLER_SKATES, RARE);
        map.put(ArtifactType.HELIUM_FLAMINGO, RARE);
        map.put(ArtifactType.UMBRELLA, RARE);
        map.put(ArtifactType.POWER_GLOVE, RARE);
        map.put(ArtifactType.FERAL_CLAWS, RARE);
        map.put(ArtifactType.VAMPIRIC_GLOVE, RARE);
        map.put(ArtifactType.POCKET_PISTON, RARE);
        map.put(ArtifactType.UNIVERSAL_ATTRACTOR, RARE);
        map.put(ArtifactType.CRYSTAL_HEART, RARE);

        // special
        map.put(ArtifactType.CLOUD_IN_A_BOTTLE, SPECIAL);
        map.put(ArtifactType.VILLAGER_HAT, SPECIAL);
        map.put(ArtifactType.SCARF_OF_INVISIBILITY, SPECIAL);
        map.put(ArtifactType.FIRE_GAUNTLET, SPECIAL);

        // boss-only (delivered by leaf-bosses datapack; excluded here)
        map.put(ArtifactType.VERDANT_CROWN, BOSS);
        map.put(ArtifactType.FROSTWARD_CHARM, BOSS);

        return map;
    }
}
