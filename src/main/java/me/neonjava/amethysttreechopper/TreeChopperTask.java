package me.neonjava.amethysttreechopper;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
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
    
    // Sets to keep track of blocks to break
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
     * Scans and starts the asynchronous/Folia-compatible animated felling.
     */
    public void start() {
        // Detect tree structure
        detectTree(startBlock);

        if (logsToBreak.isEmpty()) return;

        // Perform animation log by log, and leaves in batches
        runAnimation();
    }

    /**
     * Breadth-first search to detect connected tree logs and adjacent leaves.
     */
    private void detectTree(Block start) {
        Queue<Block> queue = new LinkedList<>();
        queue.add(start);
        logsToBreak.add(start);

        // Find logs
        while (!queue.isEmpty() && logsToBreak.size() < MAX_LOGS) {
            Block current = queue.poll();

            // Check surrounding 3x3x3 space for same wood type
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block relative = current.getRelative(x, y, z);
                        if (logsToBreak.contains(relative)) continue;

                        if (isLog(relative.getType())) {
                            logsToBreak.add(relative);
                            queue.add(relative);
                        }
                    }
                }
            }
        }

        // Find adjacent leaves to logs
        for (Block log : logsToBreak) {
            for (int x = -3; x <= 3; x++) {
                for (int y = -1; y <= 4; y++) {
                    for (int z = -3; z <= 3; z++) {
                        Block relative = log.getRelative(x, y, z);
                        if (isLeaves(relative.getType()) && !leavesToBreak.contains(relative)) {
                            leavesToBreak.add(relative);
                            if (leavesToBreak.size() >= MAX_LEAVES) break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Runs the block felling animation log by log sequentially.
     */
    private void runAnimation() {
        Iterator<Block> logIterator = logsToBreak.iterator();
        Iterator<Block> leavesIterator = leavesToBreak.iterator();

        // We run a recurring region-based task in the world region
        Bukkit.getRegionScheduler().runAtFixedRate(plugin, startBlock.getLocation(), task -> {
            if (!player.isOnline()) {
                task.cancel();
                return;
            }

            // Break 2 logs per tick for smooth animation
            boolean logsLeft = false;
            for (int i = 0; i < 2; i++) {
                if (logIterator.hasNext()) {
                    Block log = logIterator.next();
                    breakBlockWithEffects(log);
                    logsLeft = true;
                }
            }

            // Shred 8 leaves per tick alongside wood
            boolean leavesLeft = false;
            for (int i = 0; i < 8; i++) {
                if (leavesIterator.hasNext()) {
                    Block leaf = leavesIterator.next();
                    breakBlockWithEffects(leaf);
                    leavesLeft = true;
                }
            }

            // Cancel the task once all elements are processed
            if (!logsLeft && !leavesLeft) {
                task.cancel();
            }
        }, 1L, 1L);
    }

    /**
     * Breaks a block, playing Amethyst sound/particles, dropping correct items/saplings.
     */
    private void breakBlockWithEffects(Block block) {
        if (block.isEmpty()) return;

        World world = block.getWorld();
        
        // Spawn Amethyst break sound and particles
        world.playSound(block.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.5f, 1.0f);
        world.spawnParticle(org.bukkit.Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 6, block.getBlockData());
        world.spawnParticle(org.bukkit.Particle.INSTANT_EFFECT, block.getLocation().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.0);

        // Break naturally honoring the tool (Silk Touch/Efficiency)
        block.breakNaturally(tool);
    }

    private boolean isLog(org.bukkit.Material material) {
        String name = material.name();
        return name.contains("_LOG") || name.contains("_WOOD") || name.equals("MANGROVE_ROOTS");
    }

    private boolean isLeaves(org.bukkit.Material material) {
        String name = material.name();
        return name.contains("_LEAVES") || name.equals("AZALEA_LEAVES") || name.equals("FLOWERING_AZALEA_LEAVES");
    }
}
