package net.leaf.artifacts;

import net.leaf.curios.api.CuriosApi;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

/**
 * Adapter exposing the artifact-specific "what is active" queries on top of the
 * generic {@link CuriosApi}.
 *
 * <p>The curio store and menu now live in the LeafCurios plugin; this class just
 * maps the player's currently-active curios (those slotted in the curios menu
 * <em>and</em> any curio worn in a vanilla armour slot) to {@link ArtifactType}s
 * so the rest of the plugin (controller, event listeners, Cloud in a Bottle) can
 * keep asking the same questions it always did.</p>
 */
public final class ArtifactEquipment {

    private final CuriosApi curios;

    public ArtifactEquipment(CuriosApi curios) {
        this.curios = curios;
    }

    /** The set of artifact types currently active for the player. */
    public Set<ArtifactType> activeTypes(Player player) {
        Set<ArtifactType> active = EnumSet.noneOf(ArtifactType.class);
        for (ItemStack item : curios.getEquippedItems(player)) {
            ArtifactItem.typeOf(item).ifPresent(active::add);
        }
        return active;
    }

    /** Whether the player currently has the given artifact active. */
    public boolean carries(Player player, ArtifactType type) {
        return curios.isEquipped(player,
                item -> ArtifactItem.typeOf(item).filter(t -> t == type).isPresent());
    }
}
