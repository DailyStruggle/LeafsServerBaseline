package net.leaf.skills;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin command: {@code /leafskills reset [player]} clears a player's
 * selections and managed buffs. Experience levels are never refunded.
 */
public final class SkillCommand implements CommandExecutor, TabCompleter {

    private final SkillController controller;

    public SkillCommand(SkillController controller) {
        this.controller = controller;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("reset")) {
            sender.sendMessage("Usage: /leafskills reset [player]");
            return true;
        }

        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("Player not found: " + args[1]);
                return true;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            sender.sendMessage("Console must specify a player: /leafskills reset <player>");
            return true;
        }

        Player resolved = target;
        // Run the mutation on the target's own thread for Folia safety.
        resolved.getScheduler().run(controller.plugin(), task -> controller.reset(resolved), null);
        sender.sendMessage("Reset skill selections for " + resolved.getName() + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("reset");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                out.add(p.getName());
            }
        }
        return out;
    }
}
