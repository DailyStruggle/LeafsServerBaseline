package net.leaf.curios.api;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A registered curio slot category (the LeafCurios equivalent of Curios'
 * {@code SlotType}).
 *
 * <p>A slot type has a stable {@code id}, a human-readable display name, an icon
 * {@link Material} used for the empty-slot placeholder, a {@code size} (how many
 * copies of this slot appear in the menu), and an optional extra
 * {@link Predicate} validator. An item is accepted into a slot when it declares
 * the slot's id (see {@link CurioItems#fitsSlot}) or the validator accepts it.</p>
 */
public final class SlotType {

    private final String id;
    private final String displayName;
    private final Material icon;
    private final int size;
    private final Predicate<ItemStack> validator;

    private SlotType(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.icon = builder.icon;
        this.size = builder.size;
        this.validator = builder.validator;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public int size() {
        return size;
    }

    /** Whether the given item may be placed into this slot type. */
    public boolean accepts(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        if (CurioItems.fitsSlot(item, id)) {
            return true;
        }
        return validator != null && validator.test(item);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof SlotType other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /** Fluent builder for a {@link SlotType}. */
    public static final class Builder {
        private final String id;
        private String displayName;
        private Material icon = Material.LIGHT_GRAY_STAINED_GLASS_PANE;
        private int size = 1;
        private Predicate<ItemStack> validator;

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id").trim().toLowerCase(Locale.ROOT);
            if (this.id.isEmpty()) {
                throw new IllegalArgumentException("Slot type id must not be blank");
            }
            this.displayName = this.id.substring(0, 1).toUpperCase(Locale.ROOT) + this.id.substring(1);
        }

        public Builder displayName(String displayName) {
            if (displayName != null && !displayName.isBlank()) {
                this.displayName = displayName;
            }
            return this;
        }

        public Builder icon(Material icon) {
            if (icon != null) {
                this.icon = icon;
            }
            return this;
        }

        public Builder size(int size) {
            this.size = Math.max(1, size);
            return this;
        }

        public Builder validator(Predicate<ItemStack> validator) {
            this.validator = validator;
            return this;
        }

        public SlotType build() {
            return new SlotType(this);
        }
    }
}
