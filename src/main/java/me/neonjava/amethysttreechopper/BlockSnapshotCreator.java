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

        // Capture a bounding scan block region for async check (e.g. 16x32x16 radius)
        for (int x = -maxRadius; x <= maxRadius; x++) {
            for (int y = -2; y <= 36; y++) { // Trees mostly grow upwards
                for (int z = -maxRadius; z <= maxRadius; z++) {
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
