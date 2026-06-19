package net.leaf.mobs;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Data-driven, Folia-safe framework for custom and biome-specific mobs.
 *
 * <p>F0/F1 scope: load the {@code mobs.yml} reference table, expose a generic
 * {@link MobFactory} that spawns and PDC-marks any definition, and an admin command to
 * list/reload/spawn for verification.</p>
 *
 * <p>F2 scope: a {@link MobManager} owns per-mob {@link MobController}s (boss bar +
 * lifecycle), and a {@link MobLifecycleListener} rebuilds those controllers from the
 * durable PDC identity whenever an entity (re)loads, so bosses survive {@code /reload} and
 * a restart.</p>
 *
 * <p>F3 scope (this build): a data-driven ability system. An {@link AbilityRegistry} maps
 * a {@code type} string to an {@link Ability} (backed by the reusable {@link SpellKit}
 * casts); each ELITE/BOSS row's {@code abilities:} list is gated by cooldown/health-phase/
 * chance and cast from the {@link MobController}'s entity-scheduler tick (Folia-safe).</p>
 *
 * <p>F4 scope (this build): a biome-gated spawn service. A {@link MobSpawnListener} turns a
 * configurable fraction of natural vanilla spawns into managed mobs (filter by spawn reason
 * + base type + biome, roll a per-mob chance, enforce cooldown and a live-count cap), driven
 * by each row's {@code spawn:} section.</p>
 *
 * <p>F5 scope (this build): rewards. A {@link RewardService} (invoked from
 * {@link MobLifecycleListener} on a PDC-marked death) rolls the row's {@code loot_table:}
 * (with the killer as looter) and grants its {@code advancement:} on the killer's Folia
 * scheduler, giving the "rare-on-loot-table + guaranteed-on-first-kill" reward model.</p>
 */
public final class LeafMobsPlugin extends JavaPlugin {

    private MobKeys keys;
    private MobRegistry registry;
    private MobFactory factory;
    private MobManager manager;
    private AbilityRegistry abilities;
    private RewardService rewards;

    @Override
    public void onEnable() {
        this.keys = new MobKeys(this);
        this.registry = new MobRegistry(this);
        this.factory = new MobFactory(keys, getLogger());
        this.abilities = new AbilityRegistry(getLogger());
        this.manager = new MobManager(this, keys, registry, factory);
        this.rewards = new RewardService(this);

        registry.reload();

        getServer().getPluginManager().registerEvents(new MobLifecycleListener(manager, rewards), this);
        getServer().getPluginManager().registerEvents(new MobSpawnListener(this), this);

        MobAdminCommand command = new MobAdminCommand(this);
        if (getCommand("leafmobs") != null) {
            getCommand("leafmobs").setExecutor(command);
            getCommand("leafmobs").setTabCompleter(command);
        }

        // Re-adopt any managed mobs already loaded (covers a live plugin reload; a normal
        // restart is handled per-entity by the lifecycle listener as chunks load).
        int reattached = manager.scanLoaded();

        getLogger().info("LeafMobs enabled with " + registry.size() + " definition(s); "
                + "scheduled reattach for " + reattached + " loaded managed mob(s).");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.shutdown();
        }
    }

    public MobKeys keys() {
        return keys;
    }

    public MobRegistry registry() {
        return registry;
    }

    public MobFactory factory() {
        return factory;
    }

    public MobManager manager() {
        return manager;
    }

    public AbilityRegistry abilities() {
        return abilities;
    }
}
