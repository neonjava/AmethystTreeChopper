package me.neonjava.amethysttreechopper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class ChopperListener implements Listener {

    private final AmethystTreeChopperPlugin plugin;

    public ChopperListener(AmethystTreeChopperPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        if (!plugin.getChopperItemManager().isChopper(mainHand)) return;

        // Verify if the item is expired before felling
        if (checkAndSelfDestruct(player, mainHand, true)) {
            event.setCancelled(true);
            return;
        }

        // Validate that the broken block or the block directly above/adjacent to it is a log (handles various tree shapes)
        boolean hasLog = isLog(event.getBlock().getType()) ||
                         isLog(event.getBlock().getRelative(BlockFace.UP).getType()) ||
                         isLog(event.getBlock().getRelative(BlockFace.NORTH).getType()) ||
                         isLog(event.getBlock().getRelative(BlockFace.SOUTH).getType()) ||
                         isLog(event.getBlock().getRelative(BlockFace.EAST).getType()) ||
                         isLog(event.getBlock().getRelative(BlockFace.WEST).getType());
                         
        if (!hasLog) return;

        // Felling starting block
        event.setCancelled(true); 
        
        // Play Amethyst break sound on the primary block the player breaks
        event.getBlock().getWorld().playSound(event.getBlock().getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.9f, 1.0f);

        new TreeChopperTask(plugin, player, event.getBlock(), mainHand).start();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && plugin.getChopperItemManager().isChopper(item)) {
            checkAndSelfDestruct(player, item, true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack item = event.getCurrentItem();
        if (item != null && plugin.getChopperItemManager().isChopper(item)) {
            if (checkAndSelfDestruct(player, item, false)) {
                event.setCurrentItem(null);
                player.sendMessage(ChatColor.RED + "Your Amethyst Tree Chopper has expired and self-destructed!");
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Scan joining player's inventory to clean up any expired choppers
        Player player = event.getPlayer();
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && plugin.getChopperItemManager().isChopper(item)) {
                if (checkAndSelfDestruct(player, item, false)) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
    }

    /**
     * Checks if the item is expired and removes it.
     * @return true if self-destructed.
     */
    private boolean checkAndSelfDestruct(Player player, ItemStack item, boolean updateInHand) {
        if (item == null || item.getItemMeta() == null) return false;
        
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        if (!pdc.has(ChopperItemManager.CREATION_KEY, PersistentDataType.LONG) ||
            !pdc.has(ChopperItemManager.LIFETIME_KEY, PersistentDataType.LONG)) {
            return false;
        }

        long creation = pdc.get(ChopperItemManager.CREATION_KEY, PersistentDataType.LONG);
        long lifetime = pdc.get(ChopperItemManager.LIFETIME_KEY, PersistentDataType.LONG);
        long elapsed = System.currentTimeMillis() - creation;
        
        if (elapsed >= lifetime) {
            if (updateInHand) {
                item.setAmount(0); // Destroys item in hand
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 0.5f);
                player.sendMessage(ChatColor.RED + "Your Amethyst Tree Chopper has expired and self-destructed!");
            }
            return true;
        }
        return false;
    }

    private boolean isLog(org.bukkit.Material material) {
        String name = material.name();
        return name.contains("_LOG") || name.contains("_WOOD") || name.contains("_STEM") || name.contains("_HYPHAE") || name.equals("MANGROVE_ROOTS");
    }
}
