package net.leaf.artifacts;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The catalogue of implemented artifacts.
 *
 * <p>This first pass covers the "P1" batch from
 * {@code docs/scratch/ARTIFACTS-VANILLA-RECREATION.md}: artifacts whose whole
 * effect is a passive attribute modifier and/or an infinite potion effect that
 * is simply present while the artifact is carried. More complex (ticking /
 * event-reactive) artifacts are intentionally not implemented yet.</p>
 *
 * <p>An artifact is "active" while it is equipped in the curio menu. Each type
 * declares the {@link CurioSlot} categories it may be placed into ({@link #slots()}).
 * Custom models and loot sourcing are deferred to later integration work.</p>
 */
public enum ArtifactType {

    RUNNING_SHOES(
            "running_shoes",
            "Running Shoes",
            Material.LEATHER_BOOTS,
            "Auto step-up and a steady speed boost while worn.",
            List.of(
                    AttributeSpec.add(Attribute.STEP_HEIGHT, 0.4D),
                    AttributeSpec.scale(Attribute.MOVEMENT_SPEED, 0.20D)),
            List.of(),
            Set.of(CurioSlot.FEET)),

    CRYSTAL_HEART(
            "crystal_heart",
            "Crystal Heart",
            Material.HEART_OF_THE_SEA,
            "Permanently raises your maximum health.",
            List.of(AttributeSpec.add(Attribute.MAX_HEALTH, 6.0D)),
            List.of(),
            Set.of(CurioSlot.NECKLACE, CurioSlot.CHARM)),

    STEADFAST_SPIKES(
            "steadfast_spikes",
            "Steadfast Spikes",
            Material.IRON_BOOTS,
            "You cannot be knocked back.",
            List.of(AttributeSpec.add(Attribute.KNOCKBACK_RESISTANCE, 1.0D)),
            List.of(),
            Set.of(CurioSlot.FEET)),

    FERAL_CLAWS(
            "feral_claws",
            "Feral Claws",
            Material.PRISMARINE_SHARD,
            "Greatly increases your attack speed.",
            List.of(AttributeSpec.scale(Attribute.ATTACK_SPEED, 0.40D)),
            List.of(),
            Set.of(CurioSlot.HANDS)),

    POWER_GLOVE(
            "power_glove",
            "Power Glove",
            Material.RABBIT_HIDE,
            "Increases your melee attack damage.",
            List.of(AttributeSpec.add(Attribute.ATTACK_DAMAGE, 2.0D)),
            List.of(),
            Set.of(CurioSlot.HANDS)),

    LUCKY_SCARF(
            "lucky_scarf",
            "Lucky Scarf",
            Material.WHITE_WOOL,
            "Fortune favours you (increased luck).",
            List.of(AttributeSpec.add(Attribute.LUCK, 2.0D)),
            List.of(),
            Set.of(CurioSlot.NECKLACE)),

    NIGHT_VISION_GOGGLES(
            "night_vision_goggles",
            "Night Vision Goggles",
            Material.SPYGLASS,
            "You can see clearly in the dark.",
            List.of(),
            List.of(PotionEffectType.NIGHT_VISION),
            Set.of(CurioSlot.HEAD)),

    SNORKEL(
            "snorkel",
            "Snorkel",
            Material.GLASS_BOTTLE,
            "You can breathe underwater.",
            List.of(),
            List.of(PotionEffectType.WATER_BREATHING),
            Set.of(CurioSlot.HEAD)),

    SCARF_OF_INVISIBILITY(
            "scarf_of_invisibility",
            "Scarf of Invisibility",
            Material.PHANTOM_MEMBRANE,
            "You are permanently invisible.",
            List.of(),
            List.of(PotionEffectType.INVISIBILITY),
            Set.of(CurioSlot.NECKLACE)),

    OBSIDIAN_SKULL(
            "obsidian_skull",
            "Obsidian Skull",
            Material.WITHER_SKELETON_SKULL,
            "You are immune to fire and lava.",
            List.of(),
            List.of(PotionEffectType.FIRE_RESISTANCE),
            Set.of(CurioSlot.HEAD, CurioSlot.CHARM)),

    // --- P2: simple event reactions (handled in ArtifactEventListener) ---

    VAMPIRIC_GLOVE(
            "vampiric_glove",
            "Vampiric Glove",
            Material.REDSTONE,
            "Heal for a fraction of the melee damage you deal.",
            List.of(),
            List.of(),
            Set.of(CurioSlot.HANDS)),

    FLAME_PENDANT(
            "flame_pendant",
            "Flame Pendant",
            Material.BLAZE_POWDER,
            "Sets enemies ablaze when you hit them in melee.",
            List.of(),
            List.of(),
            Set.of(CurioSlot.NECKLACE)),

    SHOCK_PENDANT(
            "shock_pendant",
            "Shock Pendant",
            Material.LIGHTNING_ROD,
            "A chance to call lightning down on whatever you strike.",
            List.of(),
            List.of(),
            Set.of(CurioSlot.NECKLACE)),

    PANIC_NECKLACE(
            "panic_necklace",
            "Panic Necklace",
            Material.HONEYCOMB,
            "Grants a burst of speed when you take damage.",
            List.of(),
            List.of(),
            Set.of(CurioSlot.NECKLACE)),

    ONION_RING(
            "onion_ring",
            "Onion Ring",
            Material.GOLD_NUGGET,
            "Gain mobile Haste for a while after eating.",
            List.of(),
            List.of(),
            Set.of(CurioSlot.RING, CurioSlot.CHARM)),

    GOLDEN_HOOK(
            "golden_hook",
            "Golden Hook",
            Material.FISHING_ROD,
            "Creatures you slay yield extra experience.",
            List.of(),
            List.of(),
            Set.of(CurioSlot.CHARM)),

    FIRE_GAUNTLET(
            "fire_gauntlet",
            "Fire Gauntlet",
            Material.MAGMA_CREAM,
            "Increases melee damage and ignites the enemies you hit.",
            List.of(AttributeSpec.add(Attribute.ATTACK_DAMAGE, 2.0D)),
            List.of(),
            Set.of(CurioSlot.HANDS)),

    BUNNY_HOPPERS(
            "bunny_hoppers",
            "Bunny Hoppers",
            Material.RABBIT_FOOT,
            "Jump higher and never take fall damage.",
            List.of(AttributeSpec.add(Attribute.JUMP_STRENGTH, 0.25D)),
            List.of(),
            Set.of(CurioSlot.FEET)),

    CLOUD_IN_A_BOTTLE(
            "cloud_in_a_bottle",
            "Cloud in a Bottle",
            Material.SNOWBALL,
            "Jump again in mid-air; the lift scales with your jump strength.",
            List.of(),
            List.of(),
            Set.of(CurioSlot.CHARM));

    private final String id;
    private final String displayName;
    private final Material material;
    private final String description;
    private final List<AttributeSpec> attributes;
    private final List<PotionEffectType> effects;
    private final Set<CurioSlot> slots;

    ArtifactType(String id, String displayName, Material material, String description,
                 List<AttributeSpec> attributes, List<PotionEffectType> effects,
                 Set<CurioSlot> slots) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.description = description;
        this.attributes = attributes;
        this.effects = effects;
        this.slots = slots;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public Material material() {
        return material;
    }

    public String description() {
        return description;
    }

    public List<AttributeSpec> attributes() {
        return attributes;
    }

    public List<PotionEffectType> effects() {
        return effects;
    }

    /** The curio slot categories this artifact may be equipped into. */
    public Set<CurioSlot> slots() {
        return slots;
    }

    /** Resolves an artifact type from its persisted id. */
    public static Optional<ArtifactType> fromId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        for (ArtifactType type : values()) {
            if (type.id.equals(id)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /** Resolves an artifact type from a user-supplied command argument. */
    public static Optional<ArtifactType> fromArgument(String arg) {
        if (arg == null) {
            return Optional.empty();
        }
        String normalised = arg.toLowerCase(Locale.ROOT).replace('-', '_');
        return fromId(normalised);
    }
}
