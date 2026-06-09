package net.leaf.curios.internal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.leaf.curios.api.CuriosApi;
import net.leaf.curios.api.SlotType;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code /curios} opens the menu; {@code /curios slot add|remove|list ...} lets
 * an admin manage slot types at runtime (slot types are fully dynamic).
 */
public final class CuriosCommand implements CommandExecutor, TabCompleter {

    private final CuriosApi api;

    public CuriosCommand(CuriosApi api) {
        this.api = api;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                api.openMenu(player);
            } else {
                sender.sendMessage(Component.text("Only a player can open the curios menu.", NamedTextColor.RED));
            }
            return true;
        }

        if (!args[0].equalsIgnoreCase("slot")) {
            sender.sendMessage(Component.text("Usage: /curios [slot <add|remove|list> ...]", NamedTextColor.RED));
            return true;
        }

        if (!sender.hasPermission("leafcurios.admin")) {
            sender.sendMessage(Component.text("You don't have permission to manage curio slots.", NamedTextColor.RED));
            return true;
        }

        String sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "list";
        switch (sub) {
            case "list" -> {
                List<SlotType> types = api.getSlotTypes();
                if (types.isEmpty()) {
                    sender.sendMessage(Component.text("No slot types registered.", NamedTextColor.GRAY));
                } else {
                    sender.sendMessage(Component.text("Slot types:", NamedTextColor.AQUA));
                    for (SlotType type : types) {
                        sender.sendMessage(Component.text(
                                " - " + type.id() + " (\"" + type.displayName() + "\", "
                                        + type.icon() + " x" + type.size() + ")", NamedTextColor.GRAY));
                    }
                }
            }
            case "add" -> {
                // /curios slot add <id> [display] [icon] [size]
                if (args.length < 3) {
                    sender.sendMessage(Component.text(
                            "Usage: /curios slot add <id> [display] [icon] [size]", NamedTextColor.RED));
                    return true;
                }
                String id = args[2];
                SlotType.Builder builder = SlotType.builder(id);
                if (args.length >= 4) {
                    builder.displayName(args[3]);
                }
                if (args.length >= 5) {
                    Material icon = Material.matchMaterial(args[4]);
                    if (icon == null) {
                        sender.sendMessage(Component.text("Unknown material: " + args[4], NamedTextColor.RED));
                        return true;
                    }
                    builder.icon(icon);
                }
                if (args.length >= 6) {
                    try {
                        builder.size(Integer.parseInt(args[5]));
                    } catch (NumberFormatException ex) {
                        sender.sendMessage(Component.text("Size must be a number: " + args[5], NamedTextColor.RED));
                        return true;
                    }
                }
                api.registerSlotType(builder.build());
                sender.sendMessage(Component.text("Registered slot type '" + id.toLowerCase(Locale.ROOT) + "'.",
                        NamedTextColor.GREEN));
            }
            case "remove" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /curios slot remove <id>", NamedTextColor.RED));
                    return true;
                }
                boolean removed = api.unregisterSlotType(args[2]);
                sender.sendMessage(removed
                        ? Component.text("Removed slot type '" + args[2].toLowerCase(Locale.ROOT) + "'.",
                                NamedTextColor.GREEN)
                        : Component.text("No such slot type: " + args[2], NamedTextColor.RED));
            }
            default -> sender.sendMessage(Component.text(
                    "Usage: /curios slot <add|remove|list> ...", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            if ("slot".startsWith(args[0].toLowerCase(Locale.ROOT))) {
                out.add("slot");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("slot")) {
            for (String sub : new String[] {"add", "remove", "list"}) {
                if (sub.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(sub);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("slot")
                && args[1].equalsIgnoreCase("remove")) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            for (SlotType type : api.getSlotTypes()) {
                if (type.id().startsWith(prefix)) {
                    out.add(type.id());
                }
            }
        }
        return out;
    }
}
