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

        long lifetimeMs = 3 * 24 * 60 * 60 * 1000L; // Default 3 days

        if (args.length > 1) {
            String timeStr = args[1].toLowerCase();
            try {
                long calculatedMs = parseDuration(timeStr);
                if (calculatedMs <= 0) {
                    sender.sendMessage(ChatColor.RED + "Duration must be positive (e.g. 3d, 12h, 30m, or 1d12h).");
                    return true;
                }
                lifetimeMs = calculatedMs;
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Invalid duration format: " + args[1] + ". Use e.g. 3d, 12h, 30m, or 1d12h");
                return true;
            }
        }

        ItemStack chopper = plugin.getChopperItemManager().createChopper(lifetimeMs);
        
        target.getInventory().addItem(chopper);
        target.playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        
        long totalSeconds = lifetimeMs / 1000;
        long d = totalSeconds / (24 * 3600);
        long h = (totalSeconds % (24 * 3600)) / 3600;
        long m = (totalSeconds % 3600) / 60;
        String readableTime = String.format("%dd %dh %dm", d, h, m);

        target.sendMessage(ChatColor.GREEN + "You received the " + ChatColor.LIGHT_PURPLE + "Amethyst Tree Chopper " + ChatColor.GREEN + "with a " + readableTime + " lifespan!");
        
        if (target != sender) {
            sender.sendMessage(ChatColor.GREEN + "Gave 1x Amethyst Tree Chopper to " + target.getName() + " with lifespan " + readableTime + ".");
        }

        return true;
    }

    private long parseDuration(String input) {
        long totalMs = 0;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)([dhm])");
        java.util.regex.Matcher matcher = pattern.matcher(input);
        boolean matched = false;

        while (matcher.find()) {
            matched = true;
            long val = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "d": totalMs += val * 24 * 60 * 60 * 1000L; break;
                case "h": totalMs += val * 60 * 60 * 1000L; break;
                case "m": totalMs += val * 60 * 1000L; break;
            }
        }

        if (!matched) {
            // Fallback to days parsing if it's just a raw number
            try {
                return Long.parseLong(input) * 24 * 60 * 60 * 1000L;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException();
            }
        }
        return totalMs;
    }
}
