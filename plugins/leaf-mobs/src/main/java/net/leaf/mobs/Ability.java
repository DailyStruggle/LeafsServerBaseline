package net.leaf.mobs;

/**
 * A single pluggable behaviour a managed elite/boss can perform. Abilities are the F3
 * extension point: a new mechanic is a new {@code Ability} implementation registered by a
 * {@code type} string in {@link AbilityRegistry}, then referenced from {@code mobs.yml}.
 *
 * <p>Implementations are stateless singletons - all per-cast tuning arrives through the
 * {@link AbilityContext} ({@code params} from the reference-table row), and all cast
 * gating (cooldown, health phase, chance) is handled by {@link MobController} before
 * {@link #cast} is ever called. This keeps abilities small and trivially reusable across
 * mobs.</p>
 *
 * <p>Folia: {@link #cast} always runs on the casting entity's own region thread (it is
 * invoked from the controller's entity-scheduler tick), so it may freely read/write the
 * caster and nearby entities/blocks without further scheduling.</p>
 */
@FunctionalInterface
public interface Ability {

    /** Performs the ability once. Called only when the controller's gating allows it. */
    void cast(AbilityContext ctx);
}
