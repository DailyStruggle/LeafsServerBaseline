package net.leaf.curios.internal;

import net.leaf.curios.api.SlotType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Thread-safe registry of {@link SlotType}s, preserving registration order so
 * the menu layout is deterministic. Slot types are fully dynamic: plugins (or
 * the {@code /curios slot} command) may add or remove them at runtime.
 */
public final class SlotRegistry {

    private final Map<String, SlotType> slots = Collections.synchronizedMap(new LinkedHashMap<>());

    public void register(SlotType type) {
        slots.put(type.id(), type);
    }

    public boolean unregister(String id) {
        if (id == null) {
            return false;
        }
        return slots.remove(id.toLowerCase(Locale.ROOT)) != null;
    }

    public Optional<SlotType> get(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(slots.get(id.toLowerCase(Locale.ROOT)));
    }

    public List<SlotType> all() {
        synchronized (slots) {
            return new ArrayList<>(slots.values());
        }
    }
}
