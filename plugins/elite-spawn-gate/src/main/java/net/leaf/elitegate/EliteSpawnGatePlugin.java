package net.leaf.elitegate;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Server-specific anti-farm spawn gate for Iris elite mobs.
 *
 * <p>This plugin is intentionally narrow (ADR-001): it does not spawn or define mobs - Iris
 * owns spawning. It only inspects spawns of configured "elite" entity types and cancels the
 * ones that occur in farm-like geometry, so the rare loot-bearing elites cannot be farmed
 * while ordinary mob farms keep working.</p>
 */
public final class EliteSpawnGatePlugin extends JavaPlugin {

    private GateConfig gateConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.gateConfig = GateConfig.from(getConfig());
        getServer().getPluginManager().registerEvents(new EliteSpawnGateListener(this), this);
        getLogger().info("EliteSpawnGate enabled; gating types " + gateConfig.gatedTypes());
    }

    public GateConfig gateConfig() {
        return gateConfig;
    }
}
