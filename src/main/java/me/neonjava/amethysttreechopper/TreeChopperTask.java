package me.neonjava.amethysttreechopper;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class TreeChopperTask {

    private final AmethystTreeChopperPlugin plugin;
    private final Player player;
    private final Block startBlock;
    private final ItemStack tool;
    
    // Captured snapshot to scan values safely off thread
    private BlockSnapshot snapshot;

    // Discovered blocks
    private final Set<Block> logsToBreak = new LinkedHashSet<>();
    private final Set<Block> leavesToBreak = new LinkedHashSet<>();
    
    private static final int MAX_LOGS = 500;
    private static final int MAX_LEAVES = 2000;

    public TreeChopperTask(AmethystTreeChopperPlugin plugin, Player player, Block startBlock, ItemStack tool) {
        this.plugin = plugin;
        this.player = player;
        this.startBlock = startBlock;
        this.tool = tool;
    }

    /**
     * Captures tree region state on the main thread, and starts discovery asynchronously.
     */
    public void start() {
        this.snapshot = BlockSnapshotCreator.captureTreeRegion(startBlock, 8);

        // Run BFS discovery calculation off-thread to avoid locking tick rates
        Bukkit.getAsyncScheduler().runNow(plugin, asyncTask -> {
            detectTreeBFS();

            // Run felling animation back on the main scheduler once BFS calculation finishes
            if (!logsToBreak.isEmpty()) {
                runAnimation();
            }
        });
    }

    /**
     * BFS detection of tree logs and leaves inside the captured snapshot (Thread-Safe).
     */
    private void detectTreeBFS() {
        Queue<BlockSnapshot.LocationKey> queue = new LinkedList<>();
        Set<BlockSnapshot.LocationKey> visited = new HashSet<>();
        
        Location center = startBlock.getLocation();
        BlockSnapshot.LocationKey startKey = new BlockSnapshot.LocationKey(center);

        // AutoTreeChop style: adjust start block location if we started on a base/foliage block
        if (!isLog(snapshot.getBlockType(center))) {
            for (BlockFace face : new BlockFace[]{BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                int nx = center.getBlockX() + face.getModX();
                int ny = center.getBlockY() + face.getModY();
                int nz = center.getBlockZ() + face.getModZ();
                if (isLog(snapshot.getBlockType(nx, ny, nz))) {
                    startKey = new BlockSnapshot.LocationKey(nx, ny, nz);
                    break;
                }
            }
        }

        Material targetLogType = snapshot.getBlockType(startKey.getX(), startKey.getY(), startKey.getZ());

        if (isLog(targetLogType)) {
            queue.add(startKey);
            visited.add(startKey);
            logsToBreak.add(startKey.toLocation(snapshot.getWorld()).getBlock());
        }

        // BFS to traverse connected logs - only fells same log type to prevent breaking adjacent log builds
        while (!queue.isEmpty() && logsToBreak.size() < MAX_LOGS) {
            BlockSnapshot.LocationKey current = queue.poll();

            for (int x = -1; x <= 1; x++) {
                // Limit horizontal log connection bounds slightly (mostly tree trunks grow vertically)
                for (int y = -1; y <= 2; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        
                        int nx = current.getX() + x;
                        int ny = current.getY() + y;
                        int nz = current.getZ() + z;
                        
                        BlockSnapshot.LocationKey neighbor = new BlockSnapshot.LocationKey(nx, ny, nz);
                        if (visited.contains(neighbor)) continue;

                        Material type = snapshot.getBlockType(nx, ny, nz);
                        // Restrict felling to only connected logs of the exact same type
                        if (type == targetLogType) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                            logsToBreak.add(neighbor.toLocation(snapshot.getWorld()).getBlock());
                        }
                    }
                }
            }
        }

        // Identify target leaf material matching the target log material to prevent breaking different tree types
        Material targetLeafType = getMatchingLeafType(targetLogType);

        // BFS-adjacent leaf scanning (2-pass model)
        // Limits leaf detection radius strictly to the tree structure itself (horizontal: 4 blocks, vertical: 6 blocks)
        for (Block log : logsToBreak) {
            for (int x = -4; x <= 4; x++) {
                for (int y = -1; y <= 6; y++) {
                    for (int z = -4; z <= 4; z++) {
                        int lx = log.getX() + x;
                        int ly = log.getY() + y;
                        int lz = log.getZ() + z;

                        Material type = snapshot.getBlockType(lx, ly, lz);
                        // Restrict leaves search strictly to matching leaf type
                        if (type == targetLeafType) {
                            Block leafBlock = snapshot.getWorld().getBlockAt(lx, ly, lz);
                            leavesToBreak.add(leafBlock);
                            if (leavesToBreak.size() >= MAX_LEAVES) break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Map logs to their respective leaf types to prevent felling adjacent tree leaf canopies.
     */
    private Material getMatchingLeafType(Material logMaterial) {
        String name = logMaterial.name();
        if (name.contains("OAK")) {
            return name.contains("DARK_OAK") ? Material.DARK_OAK_LEAVES : Material.OAK_LEAVES;
        }
        if (name.contains("SPRUCE")) return Material.SPRUCE_LEAVES;
        if (name.contains("BIRCH")) return Material.BIRCH_LEAVES;
        if (name.contains("JUNGLE")) return Material.JUNGLE_LEAVES;
        if (name.contains("ACACIA")) return Material.ACACIA_LEAVES;
        if (name.contains("CHERRY")) return Material.CHERRY_LEAVES;
        if (name.contains("MANGROVE")) return Material.MANGROVE_LEAVES;
        if (name.contains("PALE")) return Material.PALE_OAK_LEAVES;
        if (name.contains("AZALEA")) return Material.AZALEA_LEAVES;
        
        // Fallback default leaves type
        return Material.OAK_LEAVES;
    }

    /**
     * Runs block felling animation log by log sequentially on the global scheduler.
     */
    private void runAnimation() {
        Iterator<Block> logIterator = logsToBreak.iterator();
        Iterator<Block> leavesIterator = leavesToBreak.iterator();

        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
            if (!player.isOnline()) {
                task.cancel();
                return;
            }

            player.getScheduler().run(plugin, pTask -> {
                boolean logsLeft = false;
                for (int i = 0; i < 2; i++) {
                    if (logIterator.hasNext()) {
                        Block log = logIterator.next();
                        breakBlockWithEffects(log, true);
                        logsLeft = true;
                    }
                }

                boolean leavesLeft = false;
                for (int i = 0; i < 8; i++) {
                    if (leavesIterator.hasNext()) {
                        Block leaf = leavesIterator.next();
                        breakBlockWithEffects(leaf, false);
                        leavesLeft = true;
                    }
                }

                if (!logsLeft && !leavesLeft) {
                    task.cancel();
                }
            }, null);
        }, 1L, 1L);
    }

    private void breakBlockWithEffects(Block block, boolean isLog) {
        if (block.isEmpty()) return;

        World world = block.getWorld();
        
        world.spawnParticle(org.bukkit.Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 6, block.getBlockData());
        world.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.0);
        world.playSound(block.getLocation(), block.getBlockData().getSoundGroup().getBreakSound(), 0.5f, 1.0f);

        // Logs drop honoring tool enchantments (Silk Touch / Fortune). 
        // Leaves drop naturally (saplings/apples) without using the tool parameter, simulating vanilla leaf decay.
        Collection<ItemStack> drops = isLog ? block.getDrops(tool) : block.getDrops();
        for (ItemStack drop : drops) {
            world.dropItemNaturally(block.getLocation(), drop);
        }
        
        block.setType(Material.AIR, true);
    }

    private boolean isLog(Material material) {
        String name = material.name();
        return name.contains("_LOG") || name.contains("_WOOD") || name.contains("_STEM") || name.contains("_HYPHAE") || name.equals("MANGROVE_ROOTS");
    }

    private boolean isLeaves(Material material) {
        String name = material.name();
        return name.contains("_LEAVES") || name.equals("AZALEA_LEAVES") || name.equals("FLOWERING_AZALEA_LEAVES");
    }
}
