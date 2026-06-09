package net.leaf.artifacts;

import net.leaf.curios.api.CuriosApi;
import net.leaf.curios.api.SlotType;
import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The curio slot types this plugin registers with LeafCurios on enable, plus a
 * display-name lookup used when building artifact item lore.
 *
 * <p>This mirrors the original fixed layout (Head, Necklace, Hands x2, Ring,
 * Charm x2, Feet x2) but now expressed as dynamic LeafCurios slot types. Other
 * plugins may register additional slot types of their own; these are simply the
 * ones artifacts needs.</p>
 */
public final class ArtifactSlotDefs {

    /** id -> (display name, copies in the menu). Order is the menu order. */
    private static final Map<String, Def> DEFS = new LinkedHashMap<>();

    static {
        // Each slot type gets its own coloured glass-pane placeholder so the
        // empty slots are easy to tell apart at a glance.
        add("head", "Head", 1, Material.ORANGE_STAINED_GLASS_PANE);
        add("necklace", "Necklace", 1, Material.MAGENTA_STAINED_GLASS_PANE);
        add("hands", "Hands", 2, Material.RED_STAINED_GLASS_PANE);
        add("ring", "Ring", 1, Material.LIME_STAINED_GLASS_PANE);
        add("charm", "Charm", 2, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        add("feet", "Feet", 2, Material.BLUE_STAINED_GLASS_PANE);
    }

    private ArtifactSlotDefs() {
    }

    private static void add(String id, String display, int size, Material icon) {
        DEFS.put(id, new Def(id, display, size, icon));
    }

    /** Registers all of this plugin's slot types with the LeafCurios API. */
    public static void registerAll(CuriosApi api) {
        for (Def def : DEFS.values()) {
            api.registerSlotType(SlotType.builder(def.id())
                    .displayName(def.display())
                    .icon(def.icon())
                    .size(def.size())
                    .build());
        }
    }

    /** Human-readable name for a slot id (falls back to the id itself). */
    public static String display(String slotId) {
        if (slotId == null) {
            return "";
        }
        Def def = DEFS.get(slotId.toLowerCase(Locale.ROOT));
        return def != null ? def.display() : slotId;
    }

    /** Joins the display names of a set of slot ids for item lore. */
    public static String displayList(Iterable<String> slotIds) {
        StringBuilder sb = new StringBuilder();
        for (String id : slotIds) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            sb.append(display(id));
        }
        return sb.toString();
    }

    public static List<String> ids() {
        return List.copyOf(DEFS.keySet());
    }

    private record Def(String id, String display, int size, Material icon) {
    }
}
