package me.neonjava.amethysttreechopper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GiveChopperCommand implements CommandExecutor {

    private final AmethystTreeChopperPlugin plugin;

    public GiveChopperCommand(AmethystTreeChopperPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("amethysttreechopper.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to execute this command!");
            return true;
        }

        Player target;
        long days = 3; // Default lifespan is 3 days

        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found.");
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Please specify a player name when running this from console: /givechopper [player] [days]");
                return true;
            }
            target = (Player) sender;
        }

        if (args.length > 1) {
            try {
                days = Long.parseLong(args[1]);
                if (days <= 0) {
                    sender.sendMessage(ChatColor.RED + "Days must be a positive integer.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid number of days: " + args[1]);
                return true;
            }
        }

        long lifetimeMs = days * 24 * 60 * 60 * 1000L;
        ItemStack chopper = plugin.getChopperItemManager().createChopper(lifetimeMs);
        
        target.getInventory().addItem(chopper);
        target.playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        target.sendMessage(ChatColor.GREEN + "You received the " + ChatColor.LIGHT_PURPLE + "Amethyst Tree Chopper " + ChatColor.GREEN + "with a " + days + "-day lifespan!");
        
        if (target != sender) {
            sender.sendMessage(ChatColor.GREEN + "Gave 1x Amethyst Tree Chopper to " + target.getName() + " with lifespan " + days + " days.");
        }

        return true;
    }
}
