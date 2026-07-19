package me.neonjava.amethysttreechopper;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ChopperItemManager {

    private final AmethystTreeChopperPlugin plugin;
    public static NamespacedKey LIFETIME_KEY;
    public static NamespacedKey CREATION_KEY;
    public static NamespacedKey CHOPPER_ID_KEY;

    public ChopperItemManager(AmethystTreeChopperPlugin plugin) {
        this.plugin = plugin;
        LIFETIME_KEY = new NamespacedKey(plugin, "lifetime_ms");
        CREATION_KEY = new NamespacedKey(plugin, "creation_ms");
        CHOPPER_ID_KEY = new NamespacedKey(plugin, "chopper_id");
    }

    /**
     * Creates an Amethyst Tree Chopper item stack.
     * @param lifetimeMs The lifetime duration in milliseconds.
     * @return The custom item stack.
     */
    public ItemStack createChopper(long lifetimeMs) {
        // TODO: Base item is a Netherite Axe as shown in user specs
        ItemStack axe = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = axe.getItemMeta();
        if (meta == null) return axe;

        // Custom Display Name
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&d&lAMETHYST TREE CHOPPER"));

        // Custom Enchantments
        meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);

        // Persistent Data for Lifespan tracking
        long now = System.currentTimeMillis();
        meta.getPersistentDataContainer().set(LIFETIME_KEY, PersistentDataType.LONG, lifetimeMs);
        meta.getPersistentDataContainer().set(CREATION_KEY, PersistentDataType.LONG, now);
        meta.getPersistentDataContainer().set(CHOPPER_ID_KEY, PersistentDataType.STRING, java.util.UUID.randomUUID().toString());

        // Update the item lore with initial state
        updateLore(meta, lifetimeMs, lifetimeMs);

        axe.setItemMeta(meta);
        return axe;
    }

    /**
     * Updates the Lore representing the countdown timer on the item meta.
     */
    public void updateLore(ItemMeta meta, long remainingMs, long totalLifetimeMs) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Silk Touch");
        lore.add(ChatColor.GRAY + "Efficiency V");
        lore.add(ChatColor.GRAY + "Unbreaking III");
        lore.add(ChatColor.GRAY + "Mending");
        lore.add(ChatColor.GRAY + "Breaks Trees Instantly");
        lore.add(ChatColor.GRAY + "Self Destruct");

        if (remainingMs <= 0) {
            lore.add(ChatColor.RED + "0d 0h 0m (Expired)");
        } else {
            long totalSeconds = remainingMs / 1000;
            long days = totalSeconds / (24 * 3600);
            long hours = (totalSeconds % (24 * 3600)) / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            
            // Format example: 3d 0h 0m
            lore.add(ChatColor.DARK_GRAY + String.format("%dd %dh %dm", days, hours, minutes));
        }

        meta.setLore(lore);
    }

    /**
     * Checks if the item is a valid Amethyst Tree Chopper.
     */
    public boolean isChopper(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_AXE) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(CHOPPER_ID_KEY, PersistentDataType.STRING);
    }
}
