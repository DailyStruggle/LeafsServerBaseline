package net.leaf.shapemining;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code /shapemine [on|off]} toggles the shape/vein feature for the calling
 * player. The feature is opt-in (default off) and works off whatever vanilla
 * pickaxe/axe/shovel/hoe the player holds; once on, sneak + scroll cycles the
 * active shape on the held tool.
 */
public final class ShapeMineCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can toggle shape-mining.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("leafshapemining.use")) {
            player.sendMessage(Component.text("You don't have permission to use shape-mining.", NamedTextColor.RED));
            return true;
        }

        boolean target;
        if (args.length == 0) {
            target = !ShapeMiningTool.isEnabled(player); // bare command toggles.
        } else {
            String arg = args[0].toLowerCase(Locale.ROOT);
            switch (arg) {
                case "on", "enable", "true" -> target = true;
                case "off", "disable", "false" -> target = false;
                default -> {
                    player.sendMessage(Component.text("Usage: /shapemine [on|off]", NamedTextColor.RED));
                    return true;
                }
            }
        }

        ShapeMiningTool.setEnabled(player, target);
        if (target) {
            player.sendMessage(Component.text(
                    "Shape-mining enabled. Hold a pickaxe/axe/shovel/hoe, sneak + scroll to pick a shape.",
                    NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Shape-mining disabled.", NamedTextColor.YELLOW));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String option : new String[] {"on", "off"}) {
                if (option.startsWith(prefix)) {
                    out.add(option);
                }
            }
        }
        return out;
    }
}
