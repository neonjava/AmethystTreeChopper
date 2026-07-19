package me.neonjava.amethysttreechopper;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class AmethystTreeChopperPlugin extends JavaPlugin {

    private ChopperItemManager chopperItemManager;

    @Override
    public void onEnable() {
        // Save and initialize config values
        saveDefaultConfig();

        chopperItemManager = new ChopperItemManager(this);

        // Register Command
        if (getCommand("givechopper") != null) {
            getCommand("givechopper").setExecutor(new GiveChopperCommand(this));
        }

        // Register Event Listener
        getServer().getPluginManager().registerEvents(new ChopperListener(this), this);

        // Asynchronous/Folia-compatible task to update the remaining times displayed in Lores of online players
        // Runs globally once every minute (1200 ticks)
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, task -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                // Run on the player's owning thread (Folia-safe)
                player.getScheduler().run(this, pTask -> {
                    boolean updatedAny = false;
                    ItemStack[] contents = player.getInventory().getContents();
                    
                    for (int i = 0; i < contents.length; i++) {
                        ItemStack item = contents[i];
                        if (item != null && chopperItemManager.isChopper(item)) {
                            ItemMeta meta = item.getItemMeta();
                            if (meta == null) continue;
                            
                            PersistentDataContainer pdc = meta.getPersistentDataContainer();
                            if (!pdc.has(ChopperItemManager.CREATION_KEY, PersistentDataType.LONG) ||
                                !pdc.has(ChopperItemManager.LIFETIME_KEY, PersistentDataType.LONG)) {
                                continue;
                            }

                            long creation = pdc.get(ChopperItemManager.CREATION_KEY, PersistentDataType.LONG);
                            long lifetime = pdc.get(ChopperItemManager.LIFETIME_KEY, PersistentDataType.LONG);
                            long elapsed = System.currentTimeMillis() - creation;
                            long remaining = lifetime - elapsed;

                            if (remaining <= 0) {
                                // Item expired, destroy it instantly from player inventory
                                player.getInventory().setItem(i, null);
                                updatedAny = true;
                            } else {
                                // Update Lore countdown timer formatting dynamically
                                chopperItemManager.updateLore(meta, remaining, lifetime);
                                item.setItemMeta(meta);
                            }
                        }
                    }
                    if (updatedAny) {
                        player.sendMessage(org.bukkit.ChatColor.RED + "An expired Amethyst Tree Chopper in your inventory has self-destructed!");
                    }
                }, null);
            }
        }, 1200L, 1200L); // 1 minute interval

        getLogger().info("AmethystTreeChopper Plugin has been successfully enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AmethystTreeChopper Plugin has been successfully disabled!");
    }

    public ChopperItemManager getChopperItemManager() {
        return chopperItemManager;
    }
}
