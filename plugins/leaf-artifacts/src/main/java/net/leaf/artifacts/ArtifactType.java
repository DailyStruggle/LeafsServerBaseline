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
 * declares the curio slot type ids it may be placed into ({@link #slots()}).
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
            Set.of("feet")),

    CRYSTAL_HEART(
            "crystal_heart",
            "Crystal Heart",
            Material.HEART_OF_THE_SEA,
            "Permanently raises your maximum health.",
            List.of(AttributeSpec.add(Attribute.MAX_HEALTH, 6.0D)),
            List.of(),
            Set.of("necklace", "charm")),

    // Verdant Crown: the Grovewarden's trophy (dark-forest giant boss). A leafy
    // circlet worn on the head that toughens its bearer with extra maximum health
    // and a slow, ambient Regeneration - the forest's enduring vitality.
    VERDANT_CROWN(
            "verdant_crown",
            "Verdant Crown",
            Material.FLOWERING_AZALEA,
            "A living circlet: raises your maximum health and slowly mends your wounds.",
            List.of(AttributeSpec.add(Attribute.MAX_HEALTH, 4.0D)),
            List.of(EffectSpec.of(PotionEffectType.REGENERATION)),
            Set.of("head")),

    // Frostward Charm: the Frost Elder's trophy (taiga giant boss). Wards off the
    // cold - its bearer cannot be frozen by powder snow and shrugs off Slowness -
    // and lends a little steadiness against knockback. Freeze/slow immunity is
    // event/tick-driven (see ArtifactController + ArtifactEventListener); the
    // knockback resistance is the only static attribute.
    FROSTWARD_CHARM(
            "frostward_charm",
            "Frostward Charm",
            Material.PRISMARINE_CRYSTALS,
            "Wards off the cold: immune to freezing and Slowness, and steadier on your feet.",
            List.of(AttributeSpec.add(Attribute.KNOCKBACK_RESISTANCE, 0.3D)),
            List.of(),
            Set.of("charm")),

    STEADFAST_SPIKES(
            "steadfast_spikes",
            "Steadfast Spikes",
            Material.IRON_BOOTS,
            "You cannot be knocked back.",
            List.of(AttributeSpec.add(Attribute.KNOCKBACK_RESISTANCE, 1.0D)),
            List.of(),
            Set.of("feet")),

    FERAL_CLAWS(
            "feral_claws",
            "Feral Claws",
            Material.PRISMARINE_SHARD,
            "Greatly increases your attack speed.",
            List.of(AttributeSpec.scale(Attribute.ATTACK_SPEED, 0.40D)),
            List.of(),
            Set.of("hands")),

    POWER_GLOVE(
            "power_glove",
            "Power Glove",
            Material.RABBIT_HIDE,
            "Increases your melee attack damage.",
            List.of(AttributeSpec.add(Attribute.ATTACK_DAMAGE, 2.0D)),
            List.of(),
            Set.of("hands")),

    LUCKY_SCARF(
            "lucky_scarf",
            "Lucky Scarf",
            Material.WHITE_WOOL,
            "Fortune favours you (increased luck).",
            List.of(AttributeSpec.add(Attribute.LUCK, 2.0D)),
            List.of(),
            Set.of("necklace")),

    NIGHT_VISION_GOGGLES(
            "night_vision_goggles",
            "Night Vision Goggles",
            Material.SPYGLASS,
            "You can see clearly in the dark.",
            List.of(),
            List.of(EffectSpec.of(PotionEffectType.NIGHT_VISION)),
            Set.of("head")),

    SNORKEL(
            "snorkel",
            "Snorkel",
            Material.GLASS_BOTTLE,
            "You can breathe underwater.",
            List.of(),
            List.of(EffectSpec.of(PotionEffectType.WATER_BREATHING)),
            Set.of("head")),

    SCARF_OF_INVISIBILITY(
            "scarf_of_invisibility",
            "Scarf of Invisibility",
            Material.PHANTOM_MEMBRANE,
            "You are permanently invisible.",
            List.of(),
            List.of(EffectSpec.of(PotionEffectType.INVISIBILITY)),
            Set.of("necklace")),

    OBSIDIAN_SKULL(
            "obsidian_skull",
            "Obsidian Skull",
            Material.WITHER_SKELETON_SKULL,
            "You are immune to fire and lava.",
            List.of(),
            List.of(EffectSpec.of(PotionEffectType.FIRE_RESISTANCE)),
            Set.of("head", "charm")),

    // --- P2: simple event reactions (handled in ArtifactEventListener) ---

    VAMPIRIC_GLOVE(
            "vampiric_glove",
            "Vampiric Glove",
            Material.REDSTONE,
            "Heal for a fraction of the melee damage you deal.",
            List.of(),
            List.of(),
            Set.of("hands")),

    FLAME_PENDANT(
            "flame_pendant",
            "Flame Pendant",
            Material.BLAZE_POWDER,
            "Sets enemies ablaze when you hit them in melee.",
            List.of(),
            List.of(),
            Set.of("necklace")),

    SHOCK_PENDANT(
            "shock_pendant",
            "Shock Pendant",
            Material.LIGHTNING_ROD,
            "A chance to call lightning down on whatever you strike.",
            List.of(),
            List.of(),
            Set.of("necklace")),

    PANIC_NECKLACE(
            "panic_necklace",
            "Panic Necklace",
            Material.HONEYCOMB,
            "Grants a burst of speed when you take damage.",
            List.of(),
            List.of(),
            Set.of("necklace")),

    ONION_RING(
            "onion_ring",
            "Onion Ring",
            Material.GOLD_NUGGET,
            "Gain mobile Haste for a while after eating.",
            List.of(),
            List.of(),
            Set.of("ring", "charm")),

    GOLDEN_HOOK(
            "golden_hook",
            "Golden Hook",
            Material.FISHING_ROD,
            "Creatures you slay yield extra experience.",
            List.of(),
            List.of(),
            Set.of("charm")),

    FIRE_GAUNTLET(
            "fire_gauntlet",
            "Fire Gauntlet",
            Material.MAGMA_CREAM,
            "Increases melee damage and ignites the enemies you hit.",
            List.of(AttributeSpec.add(Attribute.ATTACK_DAMAGE, 2.0D)),
            List.of(),
            Set.of("hands")),

    BUNNY_HOPPERS(
            "bunny_hoppers",
            "Bunny Hoppers",
            Material.RABBIT_FOOT,
            "Jump higher and never take fall damage.",
            List.of(AttributeSpec.add(Attribute.JUMP_STRENGTH, 0.25D)),
            List.of(),
            Set.of("feet")),

    CLOUD_IN_A_BOTTLE(
            "cloud_in_a_bottle",
            "Cloud in a Bottle",
            Material.SNOWBALL,
            "Jump again in mid-air; the lift scales with your jump strength.",
            List.of(),
            List.of(),
            Set.of("charm")),

    // Roller Skates (P3 signature movement, handled in RollerSkatesController):
    // its speed is a dynamic, ramping MOVEMENT_SPEED modifier driven per-tick,
    // not a static AttributeSpec, so it declares no attributes here.
    ROLLER_SKATES(
            "roller_skates",
            "Roller Skates",
            Material.GOLDEN_BOOTS,
            "Build up speed the longer you keep moving; slow to a stop when you halt.",
            List.of(),
            List.of(),
            Set.of("feet")),

    // Helium Flamingo (P3 signature movement, handled in HeliumFlamingoController):
    // timed air-swimming. It forces the swimming pose/state in midair so the
    // player "swims" through the air; driven per-tick, it declares no static
    // attributes here.
    HELIUM_FLAMINGO(
            "helium_flamingo",
            "Helium Flamingo",
            Material.SALMON,
            "Swim through the air: hold your swimming form aloft instead of falling.",
            List.of(),
            List.of(),
            Set.of("charm")),

    // Flippers (P3 movement, handled in FlippersController): a swim-speed boost.
    // Vanilla has no swim-speed attribute, so the effect is applied dynamically
    // as an ambient Dolphin's Grace while in water (and, by synergy, during the
    // Helium Flamingo air-swim); it therefore declares no static attributes here.
    FLIPPERS(
            "flippers",
            "Flippers",
            Material.LIME_DYE,
            "Swim faster through water (and through the air with a Helium Flamingo).",
            List.of(),
            List.of(),
            Set.of("feet")),

    // Umbrella (P5, handled in UmbrellaController): a colored, unbreakable shield
    // whose effect is conditional slow falling. While carried and the wearer is
    // airborne and descending, an ambient Slow Falling is refreshed each tick so
    // they drift down gently. The float triggers on every fall (even short drops),
    // which is the deliberately mild "cost" of carrying it. Driven per-tick, so it
    // declares no static attributes/effects here. The shield material is a
    // placeholder for a later resource-pack umbrella model.
    UMBRELLA(
            "umbrella",
            "Umbrella",
            Material.SHIELD,
            "You always drift down gently whenever you are falling.",
            List.of(),
            List.of(),
            Set.of("hands")),

    // --- P4: event-driven charms (handled in ArtifactEventListener) ---

    // Charm of Sinking: lets the wearer walk on the underwater floor. Vanilla has
    // no "negate buoyancy" mob effect, so it is delivered by SinkingController,
    // which gently pushes the wearer down while submerged; it therefore declares
    // no static attributes/effects here.
    CHARM_OF_SINKING(
            "charm_of_sinking",
            "Charm of Sinking",
            Material.HEAVY_CORE,
            "You sink in water and can walk along the bottom.",
            List.of(),
            List.of(),
            Set.of("charm")),

    // Pocket Piston (handled in ArtifactEventListener): a knockback burst on melee
    // hit. Vanilla has no "extra knockback" attribute that fires only on attack, so
    // on each melee hit the struck enemy is shoved away from the attacker with an
    // added velocity impulse; declares no static state.
    POCKET_PISTON(
            "pocket_piston",
            "Pocket Piston",
            Material.PISTON,
            "Your melee hits send enemies flying with a knockback burst.",
            List.of(),
            List.of(),
            Set.of("hands")),

    // Universal Attractor (handled in AttractorController): an item magnet that
    // pulls nearby dropped items toward the wearer. Vanilla has no "magnet" mob
    // effect, so it is delivered by a per-tick velocity nudge; sneaking suspends
    // it so loot can still be dropped. Declares no static state.
    UNIVERSAL_ATTRACTOR(
            "universal_attractor",
            "Universal Attractor",
            Material.LODESTONE,
            "Nearby dropped items are drawn to you; sneak to switch the pull off.",
            List.of(),
            List.of(),
            Set.of("charm")),

    CROSS_NECKLACE(
            "cross_necklace",
            "Cross Necklace",
            Material.IRON_NUGGET,
            "Lengthens your invulnerability frames after taking a hit.",
            List.of(),
            List.of(),
            Set.of("necklace", "charm")),

    ANTIDOTE_VESSEL(
            "antidote_vessel",
            "Antidote Vessel",
            Material.LINGERING_POTION,
            "Negative status effects wear off faster.",
            List.of(),
            List.of(),
            Set.of("charm")),

    THORN_PENDANT(
            "thorn_pendant",
            "Thorn Pendant",
            Material.SWEET_BERRIES,
            "Reflects part of the melee damage you take, with no durability cost.",
            List.of(),
            List.of(),
            Set.of("charm")),

    // Digging Claws: a flat mining-speed boost that stacks on top of the tool's
    // own Efficiency. Vanilla's Haste is exactly the "faster mining" mob effect
    // and it stacks additively with Efficiency, so it is delivered as an infinite
    // ambient Haste while the claws are carried - the same declarative pattern as
    // Night Vision Goggles / Villager Hat, no per-tick controller required.
    // Declared at Haste II so it adds on top of a beacon's Haste via the generic
    // EffectSpec amplifier mechanism (the reconciler samples the external level
    // and re-applies at the sum, so beacon Haste II + claws Haste II = Haste III).
    DIGGING_CLAWS(
            "digging_claws",
            "Digging Claws",
            Material.IRON_INGOT,
            "You mine faster - the boost stacks on top of Efficiency.",
            List.of(),
            List.of(EffectSpec.level(PotionEffectType.HASTE, 2)),
            Set.of("hands")),

    // --- Hats (helmet/head slot) ---

    // Superstitious Hat (handled in ArtifactEventListener): a Looting-style boost
    // to mob loot. Vanilla has no "carried looting" hook, so EntityDeathEvent
    // drops are augmented when the killer wears it; declares no static state.
    SUPERSTITIOUS_HAT(
            "superstitious_hat",
            "Superstitious Hat",
            Material.LEATHER,
            "Mobs you slay drop more loot (acts like extra Looting).",
            List.of(),
            List.of(),
            Set.of("head")),

    // Anglers Hat (handled in ArtifactEventListener): faster bites (Lure) and
    // better catches (Luck of the Sea). Driven off PlayerFishEvent; declares no
    // static state.
    ANGLERS_HAT(
            "anglers_hat",
            "Anglers Hat",
            Material.PUFFERFISH,
            "Fish bite faster and you reel in better catches.",
            List.of(),
            List.of(),
            Set.of("head")),

    // Villager Hat: a permanent trade discount. Hero of the Village is exactly
    // the vanilla mechanic that discounts villager trades, so it is delivered as
    // an infinite ambient effect while the hat is carried.
    VILLAGER_HAT(
            "villager_hat",
            "Villager Hat",
            Material.WHEAT,
            "Villagers give you a permanent trade discount.",
            List.of(),
            List.of(EffectSpec.of(PotionEffectType.HERO_OF_THE_VILLAGE)),
            Set.of("head")),

    // Eternal Steak is NOT a curio: it is a steak you can eat repeatedly that is
    // never consumed. It declares no slots (so it cannot be equipped) and its
    // effect is handled on consume in ArtifactEventListener.
    ETERNAL_STEAK(
            "eternal_steak",
            "Eternal Steak",
            Material.COOKED_BEEF,
            "An everlasting meal: eat it as often as you like, it is never used up.",
            List.of(),
            List.of(),
            Set.of());

    private final String id;
    private final String displayName;
    private final Material material;
    private final String description;
    private final List<AttributeSpec> attributes;
    private final List<EffectSpec> effects;
    private final Set<String> slots;

    ArtifactType(String id, String displayName, Material material, String description,
                 List<AttributeSpec> attributes, List<EffectSpec> effects,
                 Set<String> slots) {
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

    public List<EffectSpec> effects() {
        return effects;
    }

    /** The curio slot type ids this artifact may be equipped into. */
    public Set<String> slots() {
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
