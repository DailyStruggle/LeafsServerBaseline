package net.leaf.artifacts;

import net.leaf.curios.api.CuriosApi;
import net.leaf.curios.api.CuriosProvider;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Exploration artifacts (wearable trinkets) recreated with vanilla mechanics.
 *
 * <p>Artifacts are equipped through the shared LeafCurios menu/API (this plugin
 * depends on LeafCurios). On enable it registers the curio slot types it needs
 * (Head, Necklace, Hands x2, Ring, Charm x2, Feet x2) and marks its items as
 * curios; the per-player curio store and menu live in LeafCurios.</p>
 *
 * <p>A per-player reconcile loop (Folia-safe entity scheduler) keeps each
 * player's attribute modifiers / infinite effects in sync with their active
 * curio set, and event-reactive artifacts are handled in
 * {@link ArtifactEventListener}. Cloud in a Bottle (movement) lives in
 * {@link CloudJumpListener}.</p>
 */
public final class LeafArtifactsPlugin extends JavaPlugin {

    private static final long INITIAL_DELAY_TICKS = 2L;
    private static final long PERIOD_TICKS = 20L;

    private ArtifactController controller;
    private RollerSkatesController rollerSkates;
    private HeliumFlamingoController heliumFlamingo;
    private FlippersController flippers;
    private UmbrellaController umbrella;
    private SinkingController sinking;
    private AttractorController attractor;

    @Override
    public void onEnable() {
        CuriosApi curios = CuriosProvider.get();
        if (curios == null) {
            getLogger().severe("LeafCurios API not available; disabling LeafArtifacts.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        ArtifactKeys.init(this);
        ArtifactSlotDefs.registerAll(curios);

        ArtifactEquipment equipment = new ArtifactEquipment(curios);
        this.controller = new ArtifactController(equipment);
        this.rollerSkates = new RollerSkatesController(this, equipment);
        this.heliumFlamingo = new HeliumFlamingoController(this, equipment);
        this.flippers = new FlippersController(this, equipment, heliumFlamingo);
        this.umbrella = new UmbrellaController(this, equipment);
        this.sinking = new SinkingController(this, equipment);
        this.attractor = new AttractorController(this, equipment);

        getServer().getPluginManager().registerEvents(
                new ArtifactListener(this, controller, rollerSkates, heliumFlamingo, flippers, umbrella, sinking, attractor, curios), this);
        getServer().getPluginManager().registerEvents(new ArtifactEventListener(equipment), this);
        getServer().getPluginManager().registerEvents(new CloudJumpListener(this, equipment), this);

        ArtifactCommand command = new ArtifactCommand(curios);
        if (getCommand("artifact") != null) {
            getCommand("artifact").setExecutor(command);
            getCommand("artifact").setTabCompleter(command);
        }

        // Handle players already online (e.g. after a /reload): migrate legacy
        // equipment and start their reconcile loop now.
        for (Player player : getServer().getOnlinePlayers()) {
            player.getScheduler().runDelayed(
                    this,
                    task -> {
                        ArtifactLegacyMigration.migrate(this, player, curios);
                        controller.clear(player);
                        controller.reconcile(player);
                    },
                    null,
                    INITIAL_DELAY_TICKS);
            player.getScheduler().runAtFixedRate(
                    this,
                    task -> controller.reconcile(player),
                    null,
                    PERIOD_TICKS,
                    PERIOD_TICKS);
            rollerSkates.start(player);
            heliumFlamingo.start(player);
            flippers.start(player);
            umbrella.start(player);
            sinking.start(player);
            attractor.start(player);
        }

        getLogger().info("LeafArtifacts enabled.");
    }

    @Override
    public void onDisable() {
        if (controller != null) {
            for (Player player : getServer().getOnlinePlayers()) {
                controller.clear(player);
                if (rollerSkates != null) {
                    rollerSkates.clear(player);
                }
                if (heliumFlamingo != null) {
                    heliumFlamingo.clear(player);
                }
                if (flippers != null) {
                    flippers.clear(player);
                }
                if (umbrella != null) {
                    umbrella.clear(player);
                }
                if (sinking != null) {
                    sinking.clear(player);
                }
                if (attractor != null) {
                    attractor.clear(player);
                }
            }
        }
    }
}
