package net.leaf.artifacts;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Artifact -> configured-spawner crafting sink.
 *
 * <p>Players inevitably build XP/resource farms; rather than scatter farmable
 * vanilla spawners (uncontrolled) or rely on natural spawns (which breeds ugly,
 * laggy contraptions), this offers a controlled, bounded conversion: sacrifice a
 * specific named artifact <em>plus a Netherite Block</em> to craft a single
 * pre-configured {@link Material#SPAWNER} of the themed mob.</p>
 *
 * <p>Renewability is governed by the consumed artifact: the four SPECIAL-tier
 * artifacts (Fire Gauntlet, Villager Hat, Scarf of Invisibility, Cloud in a
 * Bottle) come only from finite structure chests, so those spawners are
 * non-renewable. The two BOSS-tier artifacts (Frostward Charm, Verdant Crown)
 * are boss-gated, so their spawners are double-gated (beat the boss <em>and</em>
 * pay the netherite). Netherite is the game's most valuable/difficult material
 * and themes the spawner's dark metal cage.</p>
 *
 * <p>Artifacts are plain vanilla-{@link Material} items distinguished only by the
 * {@link ArtifactKeys#ARTIFACT_TYPE} PDC string, so a vanilla datapack recipe
 * cannot match "an artifact" (it would match the bare base item and cannot read
 * the PDC). Instead each recipe is registered as a {@link ShapelessRecipe} keyed
 * on the artifact's <em>base</em> material (+ a Netherite Block), and
 * {@link PrepareItemCraftEvent} verifies the real artifact via
 * {@link ArtifactItem#typeOf(ItemStack)} - voiding the result when only a bare
 * base item (or the wrong artifact) is supplied. This mirrors the leather-dye
 * preservation pattern in {@link ArtifactEventListener}.</p>
 */
public final class SpawnerCraftListener implements Listener {

    // Bounded spawner tuning so a crafted spawner cannot drive runaway mob lag.
    private static final int SPAWN_COUNT = 2;
    private static final int MAX_NEARBY_ENTITIES = 6;
    private static final int REQUIRED_PLAYER_RANGE = 16;
    private static final int SPAWN_RANGE = 4;
    private static final int MIN_SPAWN_DELAY = 200;
    private static final int MAX_SPAWN_DELAY = 800;

    /** One artifact -> spawner conversion. {@code extras} maps each extra ingredient material to its count. */
    private record SpawnerRecipe(ArtifactType artifact, EntityType mob, Map<Material, Integer> extras) {
    }

    private static Map<Material, Integer> extras(Object... pairs) {
        Map<Material, Integer> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((Material) pairs[i], (Integer) pairs[i + 1]);
        }
        return map;
    }

    // The themed sink table (see plugin README / issue discussion). Each artifact
    // is used by exactly one recipe; every spawner additionally costs one Netherite
    // Block (valuable + difficult), themed as the spawner's dark metal cage.
    private static final List<SpawnerRecipe> RECIPES = List.of(
            new SpawnerRecipe(ArtifactType.FIRE_GAUNTLET, EntityType.BLAZE,
                    extras(Material.NETHERITE_BLOCK, 1)),
            new SpawnerRecipe(ArtifactType.VILLAGER_HAT, EntityType.ZOMBIE,
                    extras(Material.NETHERITE_BLOCK, 1)),
            new SpawnerRecipe(ArtifactType.SCARF_OF_INVISIBILITY, EntityType.SPIDER,
                    extras(Material.NETHERITE_BLOCK, 1)),
            new SpawnerRecipe(ArtifactType.CLOUD_IN_A_BOTTLE, EntityType.WITCH,
                    extras(Material.NETHERITE_BLOCK, 1)),
            new SpawnerRecipe(ArtifactType.FROSTWARD_CHARM, EntityType.STRAY,
                    extras(Material.NETHERITE_BLOCK, 1)),
            new SpawnerRecipe(ArtifactType.VERDANT_CROWN, EntityType.SLIME,
                    extras(Material.NETHERITE_BLOCK, 1)));

    private final Plugin plugin;
    /** Pre-built configured spawner result per artifact, used to (re)set the craft result. */
    private final Map<ArtifactType, ItemStack> results = new EnumMap<>(ArtifactType.class);

    public SpawnerCraftListener(Plugin plugin) {
        this.plugin = plugin;
        for (SpawnerRecipe recipe : RECIPES) {
            results.put(recipe.artifact(), buildSpawner(recipe.mob()));
        }
    }

    /** Builds a {@link Material#SPAWNER} item pre-configured for {@code mob} with bounded spawn settings. */
    private static ItemStack buildSpawner(EntityType mob) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        CreatureSpawner spawner = (CreatureSpawner) meta.getBlockState();
        spawner.setSpawnedType(mob);
        spawner.setSpawnCount(SPAWN_COUNT);
        spawner.setMaxNearbyEntities(MAX_NEARBY_ENTITIES);
        spawner.setRequiredPlayerRange(REQUIRED_PLAYER_RANGE);
        spawner.setSpawnRange(SPAWN_RANGE);
        spawner.setMinSpawnDelay(MIN_SPAWN_DELAY);
        spawner.setMaxSpawnDelay(MAX_SPAWN_DELAY);
        meta.setBlockState(spawner);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Registers (idempotently) one shapeless recipe per sink entry. Existing
     * recipes under our keys are removed first so a {@code /reload} re-enable does
     * not throw on duplicate registration.
     */
    public void registerRecipes() {
        for (SpawnerRecipe recipe : RECIPES) {
            NamespacedKey key = keyFor(recipe);
            Bukkit.removeRecipe(key);
            ShapelessRecipe shapeless = new ShapelessRecipe(key, results.get(recipe.artifact()).clone());
            shapeless.addIngredient(1, recipe.artifact().material());
            for (Map.Entry<Material, Integer> extra : recipe.extras().entrySet()) {
                shapeless.addIngredient(extra.getValue(), extra.getKey());
            }
            Bukkit.addRecipe(shapeless);
        }
    }

    private NamespacedKey keyFor(SpawnerRecipe recipe) {
        return new NamespacedKey(plugin, "spawner_" + recipe.artifact().id());
    }

    /**
     * Guard: a shapeless recipe keyed on the artifact's base material would also
     * fire for a bare base item, so only keep the spawner result when the real
     * artifact (correct {@link ArtifactType}, verified by PDC) is present in the
     * grid. When present, the result is re-set to that artifact's configured
     * spawner (defensive against a base-item collision picking the wrong recipe);
     * otherwise the result is voided so plain materials cannot mint a spawner.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack result = inv.getResult();
        if (result == null || result.getType() != Material.SPAWNER) {
            return;
        }
        ArtifactType present = verifiedArtifactInGrid(inv.getMatrix());
        if (present == null || !results.containsKey(present)) {
            inv.setResult(null);
            return;
        }
        inv.setResult(results.get(present).clone());
    }

    /** The first matrix slot holding a PDC-verified artifact that this sink consumes, or {@code null}. */
    private ArtifactType verifiedArtifactInGrid(ItemStack[] matrix) {
        if (matrix == null) {
            return null;
        }
        for (ItemStack item : matrix) {
            if (item == null) {
                continue;
            }
            ArtifactType type = ArtifactItem.typeOf(item).orElse(null);
            if (type != null && results.containsKey(type)) {
                return type;
            }
        }
        return null;
    }

    /** The artifact types consumed by this sink (exposed for docs/tests). */
    public static List<ArtifactType> sinkArtifacts() {
        List<ArtifactType> list = new ArrayList<>();
        for (SpawnerRecipe recipe : RECIPES) {
            list.add(recipe.artifact());
        }
        return list;
    }
}
