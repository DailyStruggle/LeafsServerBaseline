package net.leaf.backpacks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Management command: {@code /backpack give|upgrade|tierup}.
 *
 * <p>v1 distributes backpacks and upgrade modules via this admin command;
 * crafting/anvil recipes are a deliberate follow-up.</p>
 */
public final class BackpackCommand implements CommandExecutor, TabCompleter {

    private final BackpackConfig config;
    private final BackpackItem items;

    public BackpackCommand(LeafBackpacksPlugin plugin) {
        this.config = plugin.backpackConfig();
        this.items = plugin.itemFactory();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /backpack <give|upgrade|tierup>", NamedTextColor.YELLOW));
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "upgrade" -> handleUpgrade(sender, args);
            case "tierup" -> handleTierUp(sender);
            default -> sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        int tier = BackpackConfig.MIN_TIER;
        if (args.length >= 2) {
            try {
                tier = config.clampTier(Integer.parseInt(args[1]));
            } catch (NumberFormatException ex) {
                sender.sendMessage(Component.text("Tier must be a number.", NamedTextColor.RED));
                return;
            }
        }
        player.getInventory().addItem(items.create(tier));
        sender.sendMessage(Component.text("Gave a tier " + tier + " backpack.", NamedTextColor.GREEN));
    }

    private void handleUpgrade(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /backpack upgrade <type>", NamedTextColor.YELLOW));
            return;
        }
        UpgradeType type = UpgradeType.fromName(args[1]);
        if (type == null) {
            sender.sendMessage(Component.text("Unknown upgrade type.", NamedTextColor.RED));
            return;
        }
        player.getInventory().addItem(items.createUpgrade(type));
        sender.sendMessage(Component.text("Gave a " + type.displayName() + ".", NamedTextColor.GREEN));
    }

    private void handleTierUp(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!items.isBackpack(held)) {
            sender.sendMessage(Component.text("Hold the backpack you want to upgrade.", NamedTextColor.RED));
            return;
        }
        int current = items.getTier(held);
        if (current >= BackpackConfig.MAX_TIER) {
            sender.sendMessage(Component.text("This backpack is already at the maximum tier.", NamedTextColor.RED));
            return;
        }
        int next = current + 1;
        Material material = tierMaterial(next);
        int cost = config.tierUpCost();
        if (material != null && !player.getInventory().containsAtLeast(new ItemStack(material), cost)) {
            sender.sendMessage(Component.text("You need " + cost + " " + material + " to upgrade.", NamedTextColor.RED));
            return;
        }
        if (material != null) {
            player.getInventory().removeItem(new ItemStack(material, cost));
        }
        items.withTier(held, next);
        sender.sendMessage(Component.text("Upgraded backpack to tier " + next + ".", NamedTextColor.GREEN));
    }

    private Material tierMaterial(int tier) {
        return switch (tier) {
            case 2 -> Material.IRON_INGOT;
            case 3 -> Material.GOLD_INGOT;
            case 4 -> Material.DIAMOND;
            case 5 -> Material.NETHERITE_INGOT;
            default -> null;
        };
    }

    private Player asPlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
        return null;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("give", "upgrade", "tierup"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("upgrade")) {
            List<String> types = new ArrayList<>();
            for (UpgradeType t : UpgradeType.values()) {
                types.add(t.name().toLowerCase());
            }
            return filter(types, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return filter(Arrays.asList("1", "2", "3", "4", "5"), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(prefix.toLowerCase())) {
                out.add(option);
            }
        }
        return out;
    }
}
