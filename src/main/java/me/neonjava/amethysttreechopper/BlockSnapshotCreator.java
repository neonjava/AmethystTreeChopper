package me.neonjava.amethysttreechopper;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.Map;

/**
 * Capture block states synchronously on the main thread for safe async BFS discovery.
 */
public class BlockSnapshotCreator {

    public static BlockSnapshot captureTreeRegion(Block startBlock, int maxRadius) {
        Map<BlockSnapshot.LocationKey, Material> blockData = new HashMap<>();
        Location center = startBlock.getLocation();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        // Capture a larger bounding region (16 blocks horizontally, 42 blocks upwards)
        for (int x = -16; x <= 16; x++) {
            for (int y = -3; y <= 42; y++) { 
                for (int z = -16; z <= 16; z++) {
                    Block block = startBlock.getWorld().getBlockAt(cx + x, cy + y, cz + z);
                    Material type = block.getType();
                    
                    if (isLog(type) || isLeaves(type)) {
                        BlockSnapshot.LocationKey key = new BlockSnapshot.LocationKey(cx + x, cy + y, cz + z);
                        blockData.put(key, type);
                    }
                }
            }
        }

        return new BlockSnapshot(blockData, startBlock.getWorld(), center);
    }

    private static boolean isLog(org.bukkit.Material material) {
        String name = material.name();
        return name.contains("_LOG") || name.contains("_WOOD") || name.contains("_STEM") || name.contains("_HYPHAE") || name.equals("MANGROVE_ROOTS");
    }

    private static boolean isLeaves(org.bukkit.Material material) {
        String name = material.name();
        return name.contains("_LEAVES") || name.equals("AZALEA_LEAVES") || name.equals("FLOWERING_AZALEA_LEAVES");
    }
}
