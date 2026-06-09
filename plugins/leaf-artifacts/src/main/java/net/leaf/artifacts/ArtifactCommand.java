package net.leaf.artifacts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.leaf.curios.api.CuriosApi;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Admin/testing command: {@code /artifact give <type> [player]}.
 *
 * <p>Provided so the artifacts can be reviewed in-game before any loot-table or
 * datapack integration exists.</p>
 */
public final class ArtifactCommand implements CommandExecutor, TabCompleter {

    private final CuriosApi curios;

    public ArtifactCommand(CuriosApi curios) {
        this.curios = curios;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // No args: open the shared curios equipment menu for the player.
        if (args.length == 0) {
            if (sender instanceof Player self) {
                curios.openMenu(self);
            } else {
                sender.sendMessage(Component.text("Only a player can open the artifact menu.", NamedTextColor.RED));
            }
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(Component.text("Usage: /artifact [give <type> [player]]", NamedTextColor.RED));
            return true;
        }

        if (!sender.hasPermission("leafartifacts.admin")) {
            sender.sendMessage(Component.text("You don't have permission to give artifacts.", NamedTextColor.RED));
            return true;
        }

        Optional<ArtifactType> type = ArtifactType.fromArgument(args[1]);
        if (type.isEmpty()) {
            sender.sendMessage(Component.text("Unknown artifact: " + args[1], NamedTextColor.RED));
            return true;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[2], NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            sender.sendMessage(Component.text("Console must specify a player.", NamedTextColor.RED));
            return true;
        }

        ItemStack item = ArtifactItem.create(type.get());
        target.getInventory().addItem(item).values()
                .forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));
        sender.sendMessage(Component.text(
                "Gave " + type.get().displayName() + " to " + target.getName() + ".", NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : new String[] {"give"}) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            for (ArtifactType type : ArtifactType.values()) {
                if (type.id().startsWith(prefix)) {
                    out.add(type.id());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                out.add(player.getName());
            }
        }
        return out;
    }
}
