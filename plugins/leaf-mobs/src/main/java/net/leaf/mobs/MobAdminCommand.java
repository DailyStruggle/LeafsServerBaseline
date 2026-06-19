package net.leaf.mobs;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin command for inspecting and spawning reference-table mobs.
 *
 * <p>Usage: {@code /leafmobs list | reload | spawn <id> | active}. {@code spawn} runs on
 * the caller's region thread (Folia-safe) via the {@link MobManager} (so ELITE/BOSS mobs
 * get their controller/boss bar immediately), placing the mob at the player's location.
 * {@code active} reports how many controllers are currently live. These are verification
 * aids; a biome-gated SpawnService is a later phase.</p>
 */
public final class MobAdminCommand implements CommandExecutor, TabCompleter {

    private final LeafMobsPlugin plugin;

    public MobAdminCommand(LeafMobsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/leafmobs <list|reload|spawn|active> [id]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> {
                List<String> ids = plugin.registry().ids();
                sender.sendMessage("LeafMobs (" + ids.size() + "): " + String.join(", ", ids));
            }
            case "reload" -> {
                int n = plugin.registry().reload();
                sender.sendMessage("LeafMobs reloaded: " + n + " definition(s).");
            }
            case "spawn" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /leafmobs spawn <id>");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Only a player can use spawn (needs a location).");
                    return true;
                }
                MobDefinition def = plugin.registry().byId(args[1]);
                if (def == null) {
                    sender.sendMessage("Unknown mob id '" + args[1] + "'.");
                    return true;
                }
                LivingEntity spawned = plugin.manager().spawn(def, player.getLocation());
                sender.sendMessage(spawned != null
                        ? "Spawned '" + def.id() + "' (" + def.entityType() + ", " + def.tier() + ")."
                        : "Failed to spawn '" + def.id() + "' (see console).");
            }
            case "active" -> sender.sendMessage("LeafMobs active controllers: "
                    + plugin.manager().activeCount());
            default -> sender.sendMessage("/leafmobs <list|reload|spawn|active> [id]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("list", "reload", "spawn", "active"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            return filter(plugin.registry().ids(), args[1]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase().startsWith(p)) {
                out.add(o);
            }
        }
        return out;
    }
}
