package net.leaf.artifacts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies the pure two-axis structure-chest classification (base structure tier
 * shifted by the per-chest rarity keyword), matching the table in
 * {@code docs/scratch/ARTIFACTS-VANILLA-RECREATION.md}. Ids are passed lower-cased,
 * exactly as {@link ArtifactLoot#rollForChest} lower-cases them.
 */
class ArtifactLootTest {

    private static ArtifactLootTier classify(String id) {
        return ArtifactLoot.classifyChest(id);
    }

    @Test
    void perChestRarityShiftWithinAStructure() {
        // YUNG's Better Strongholds: base RARE, shifted by the chest's own name.
        assertEquals(ArtifactLootTier.SPECIAL, classify("betterstrongholds:chests/treasure"));
        assertEquals(ArtifactLootTier.UNCOMMON, classify("betterstrongholds:chests/common"));
        // Structory: base UNCOMMON.
        assertEquals(ArtifactLootTier.RARE, classify("structory:harvest/old_manor/treasure"));
        assertEquals(ArtifactLootTier.COMMON, classify("structory:harvest/old_manor/common"));
        // Towns and Towers archeology: base UNCOMMON, common/rare variants.
        assertEquals(ArtifactLootTier.RARE, classify("kaisyn:archeology/forest_ruins_rare"));
        assertEquals(ArtifactLootTier.COMMON, classify("kaisyn:archeology/forest_ruins_common"));
    }

    @Test
    void shiftIsClampedAndSingleStep() {
        // Dungeons Arise mines: base COMMON, "treasure" bumps +1 only (not +2 for "big").
        assertEquals(ArtifactLootTier.UNCOMMON, classify("dungeons_arise:chests/mines_treasure_big"));
        // Seven Seas: base UNCOMMON, barrels demote, treasure promotes.
        assertEquals(ArtifactLootTier.COMMON, classify("dungeons_arise_seven_seas:chests/pirate_junk/pirate_junk_barrels"));
        assertEquals(ArtifactLootTier.RARE, classify("dungeons_arise_seven_seas:chests/pirate_junk/pirate_junk_treasure"));
    }

    @Test
    void gapPacksAreNowCovered() {
        // These packs have no "chest" token nor a vanilla keyword in their ids;
        // the namespace rules ensure they still classify (previously returned null).
        assertEquals(ArtifactLootTier.COMMON, classify("stoneholm:armorer"));
        assertEquals(ArtifactLootTier.COMMON, classify("stoneholm:crypt"));
        assertEquals(ArtifactLootTier.RARE, classify("structory_towers:top/ocean_pillar_top"));
    }

    @Test
    void highTierMatchesAreNotOverPromoted() {
        // A Dungeons-and-Taverns mansion chest named "ancient_city_raid_chest" must be
        // read as the mansion (RARE), not promoted to SPECIAL by the bare substring.
        assertEquals(ArtifactLootTier.RARE,
                classify("nova_structures:chests/illager_mansion/ancient_city_raid_chest"));
    }

    @Test
    void vanillaStructuresKeepTheirTiers() {
        assertEquals(ArtifactLootTier.SPECIAL, classify("minecraft:chests/ancient_city"));
        assertEquals(ArtifactLootTier.RARE, classify("minecraft:chests/woodland_mansion"));
        assertEquals(ArtifactLootTier.RARE, classify("minecraft:chests/stronghold_corridor"));
        assertEquals(ArtifactLootTier.COMMON, classify("minecraft:chests/abandoned_mineshaft"));
        assertEquals(ArtifactLootTier.COMMON, classify("minecraft:chests/village/village_armorer"));
    }

    @Test
    void nonStructureLootTablesAreIgnored() {
        // No structure rule and no "chest" token -> not a structure chest.
        assertNull(classify("minecraft:gameplay/fishing/fish"));
        assertNull(classify("minecraft:entities/sheep"));
    }

    @Test
    void bossTierIsNeverProducible() {
        // The classifier clamps to SPECIAL, so BOSS-only artifacts can't leak into chests.
        for (String id : new String[] {
                "minecraft:chests/ancient_city",
                "betterstrongholds:chests/treasure",
                "dungeons_arise:chests/some_treasure_vault" }) {
            ArtifactLootTier tier = classify(id);
            assertEquals(ArtifactLootTier.SPECIAL, tier,
                    "expected clamp at SPECIAL for " + id);
        }
    }
}
