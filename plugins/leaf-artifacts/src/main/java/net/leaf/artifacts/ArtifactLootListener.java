package net.leaf.artifacts;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Injects artifacts into world loot, sourced from {@link ArtifactLoot}.
 *
 * <p>Two channels:</p>
 * <ul>
 *   <li>{@link LootGenerateEvent} - structure-chest / container loot. This event
 *       fires when a loot table populates an inventory; it does NOT fire for mob
 *       death drops or fishing, which is why mobs use the death channel below.</li>
 *   <li>{@link EntityDeathEvent} - curated hostile mob drops. A separate handler
 *       from {@link ArtifactEventListener#onDeath} (whose drops depend on the
 *       killer's carried artifacts); this one is killer-independent.</li>
 * </ul>
 *
 * <p>Both events run on the owning region thread, so adding to the loot/drops
 * lists is Folia-safe with no extra scheduling.</p>
 */
public final class ArtifactLootListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent event) {
        LootTable table = event.getLootTable();
        if (table == null) {
            return;
        }
        ArtifactLoot.rollForChest(table.getKey()).ifPresent(artifact -> {
            List<ItemStack> loot = new ArrayList<>(event.getLoot());
            loot.add(artifact);
            event.setLoot(loot);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent event) {
        ArtifactLoot.rollForMob(event.getEntityType())
                .ifPresent(artifact -> event.getDrops().add(artifact));
    }
}
