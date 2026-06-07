package net.leaf.shapemining;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

/**
 * Admin/testing command: {@code /shapemine give [player]} hands out a
 * shape-mining tool so the feature can be reviewed in-game before any loot or
 * crafting integration exists.
 */
public final class ShapeMineCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(Component.text("Usage: /shapemine give [player]", NamedTextColor.RED));
            return true;
        }
        if (!sender.hasPermission("leafshapemining.admin")) {
            sender.sendMessage(Component.text("You don't have permission to give shape-mining tools.",
                    NamedTextColor.RED));
            return true;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
                return true;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            sender.sendMessage(Component.text("Console must specify a player.", NamedTextColor.RED));
            return true;
        }

        ItemStack item = ShapeMiningTool.create();
        target.getInventory().addItem(item).values()
                .forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));
        sender.sendMessage(Component.text(
                "Gave a shape-mining tool to " + target.getName() + ".", NamedTextColor.GREEN));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            if ("give".startsWith(args[0].toLowerCase(Locale.ROOT))) {
                out.add("give");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                out.add(player.getName());
            }
        }
        return out;
    }
}
