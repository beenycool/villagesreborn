package com.beeny.event;

import com.beeny.config.VillagesConfig;
import com.beeny.village.VillageInfluenceManager;
import com.beeny.village.VillagerAI;
import com.beeny.village.VillagerManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VillagerEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    
    // Map to track chest ownership by position
    private static final Map<BlockPos, UUID> chestOwnership = new HashMap<>();
    
    // Map to track inventory contents before player interaction
    private static final Map<BlockPos, List<ItemStack>> previousInventoryContents = new HashMap<>();
    
    /**
     * Register theft detection events to monitor player interaction with villager-owned storage
     */
    public static void registerTheftDetection() {
        // Register callback for block use (opening chests, etc.)
        UseBlockCallback.EVENT.register(VillagerEvents::onPlayerUseBlock);
        
        // Register callback for block breaking
        PlayerBlockBreakEvents.AFTER.register(VillagerEvents::onPlayerBreakBlock);
        
        LOGGER.info("Registered theft detection events");
    }
    
    /**
     * Called when a player interacts with a block
     * Records chest contents before interaction to compare later
     */
    private static ActionResult onPlayerUseBlock(PlayerEntity player, World world, 
                                              Hand hand, BlockHitResult hitResult) {
        if (world.isClient() || !VillagesConfig.getInstance().isTheftDetectionEnabled()) {
            return ActionResult.PASS;
        }
        
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = world.getBlockState(pos);
        
        // Check if the block is a chest or similar container
        if (state.getBlock() instanceof ChestBlock) {
            // Store the inventory contents before player interaction
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof Inventory inventory) {
                storeInventoryContents(pos, inventory);
                
                // If chest isn't already assigned to a villager, check if it should be
                if (!chestOwnership.containsKey(pos)) {
                    checkAndAssignChestOwnership(pos, (ServerWorld)world);
                }
                
                // Inform player if this is a villager's chest (subtle warning)
                if (chestOwnership.containsKey(pos)) {
                    UUID ownerId = chestOwnership.get(pos);
                    VillagerEntity owner = findVillagerById(ownerId, (ServerWorld)world);
                    
                    if (owner != null && player.isSneaking()) {
                        player.sendMessage(Text.of("§6This chest belongs to " + 
                                                  owner.getName().getString() + "..."), true);
                    }
                }
            }
        }
        
        return ActionResult.PASS;
    }
    
    /**
     * Called when a player breaks a block
     * Detects if a player breaks a villager-owned chest
     */
    private static void onPlayerBreakBlock(World world, PlayerEntity player, 
                                         BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (world.isClient() || !VillagesConfig.getInstance().isTheftDetectionEnabled()) {
            return;
        }
        
        // Check if the broken block was a chest owned by a villager
        if (chestOwnership.containsKey(pos)) {
            UUID ownerId = chestOwnership.get(pos);
            VillagerEntity owner = findVillagerById(ownerId, (ServerWorld)world);
            
            if (owner != null) {
                VillagerAI villagerAI = VillagerManager.getInstance().getVillagerAI(ownerId);
                if (villagerAI != null) {
                    // Record the theft - breaking a chest is serious!
                    villagerAI.recordTheft(player.getUuid(), "storage chest", pos);
                    player.sendMessage(Text.of("§c" + owner.getName().getString() + 
                                              " notices you breaking their chest!"), false);
                }
            }
            
            // Remove the ownership entry as the chest no longer exists
            chestOwnership.remove(pos);
        }
    }
    
    /**
     * Compare inventory contents before and after player interaction
     * to detect if items were taken
     */
    public static void checkForStolenItems(BlockPos pos, World world) {
        if (!previousInventoryContents.containsKey(pos) || !chestOwnership.containsKey(pos)) {
            return;
        }
        
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof Inventory currentInventory)) {
            return;
        }
        
        List<ItemStack> previousItems = previousInventoryContents.get(pos);
        List<ItemStack> stolenItems = new ArrayList<>();
        
        // Compare previous inventory with current to find missing items
        for (int i = 0; i < previousItems.size(); i++) {
            ItemStack previousStack = previousItems.get(i);
            ItemStack currentStack = i < currentInventory.size() ? currentInventory.getStack(i) : ItemStack.EMPTY;
            
            if (!previousStack.isEmpty() && 
                (currentStack.isEmpty() || currentStack.getCount() < previousStack.getCount())) {
                // This item was taken or reduced in count
                stolenItems.add(previousStack.copy());
            }
        }
        
        // If items were taken, notify the villager owner
        if (!stolenItems.isEmpty() && world instanceof ServerWorld serverWorld) {
            UUID ownerId = chestOwnership.get(pos);
            notifyOwnerAboutTheft(ownerId, stolenItems, pos, serverWorld);
        }
        
        // Clear the previous contents record
        previousInventoryContents.remove(pos);
    }
    
    /**
     * Store the current inventory contents for later comparison
     */
    private static void storeInventoryContents(BlockPos pos, Inventory inventory) {
        List<ItemStack> contents = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            contents.add(inventory.getStack(i).copy());
        }
        previousInventoryContents.put(pos, contents);
    }
    
    /**
     * Check if a chest should be assigned to a nearby villager
     */
    private static void checkAndAssignChestOwnership(BlockPos pos, ServerWorld world) {
        // Find villagers within 16 blocks of the chest
        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
            VillagerEntity.class, 
            new Box(pos).expand(16), 
            villager -> true
        );
        
        if (!nearbyVillagers.isEmpty()) {
            // Assign to the closest villager
            VillagerEntity closest = nearbyVillagers.get(0);
            double closestDistance = closest.getBlockPos().getSquaredDistance(pos);
            
            for (VillagerEntity villager : nearbyVillagers) {
                double distance = villager.getBlockPos().getSquaredDistance(pos);
                if (distance < closestDistance) {
                    closest = villager;
                    closestDistance = distance;
                }
            }
            
            // Only assign if the villager is reasonably close (within 8 blocks)
            if (closestDistance <= 64) { // 8 blocks squared
                chestOwnership.put(pos, closest.getUuid());
                LOGGER.debug("Assigned chest at {} to villager {}", pos, closest.getName().getString());
            }
        }
    }
    
    /**
     * Find a villager by UUID
     */
    private static VillagerEntity findVillagerById(UUID id, ServerWorld world) {
        for (VillagerEntity villager : world.getEntitiesByClass(VillagerEntity.class, 
                                                              new Box(world.getSpawnPos()).expand(256), 
                                                              v -> true)) {
            if (villager.getUuid().equals(id)) {
                return villager;
            }
        }
        return null;
    }
    
    /**
     * Notify the owner villager about items stolen from their chest
     */
    private static void notifyOwnerAboutTheft(UUID ownerId, List<ItemStack> stolenItems, 
                                           BlockPos pos, ServerWorld world) {
        VillagerEntity owner = findVillagerById(ownerId, world);
        if (owner == null) {
            return;
        }
        
        VillagerAI villagerAI = VillagerManager.getInstance().getVillagerAI(ownerId);
        if (villagerAI == null) {
            return;
        }
        
        // Determine the player most likely to have stolen items
        PlayerEntity thief = findNearestPlayer(world, pos, 8);
        if (thief == null) {
            return;
        }
        
        // Build a description of what was stolen
        StringBuilder itemDesc = new StringBuilder();
        for (int i = 0; i < Math.min(3, stolenItems.size()); i++) {
            if (i > 0) {
                itemDesc.append(i == stolenItems.size() - 1 ? " and " : ", ");
            }
            itemDesc.append(stolenItems.get(i).getCount())
                   .append("x ")
                   .append(stolenItems.get(i).getName().getString());
        }
        if (stolenItems.size() > 3) {
            itemDesc.append(" and other items");
        }
        
        // Record the theft in the villager's memory
        villagerAI.recordTheft(thief.getUuid(), itemDesc.toString(), pos);
        
        // Send a message to the thief
        thief.sendMessage(Text.of("§c" + owner.getName().getString() + 
                              " noticed you taking " + itemDesc + " from their chest!"), false);
        
        // Affect village reputation through the influence manager if implemented
        VillageInfluenceManager influenceManager = VillageInfluenceManager.getInstance();
        if (influenceManager != null) {
            influenceManager.recordTheft(thief.getUuid(), owner.getUuid(), stolenItems.size());
        }
    }
    
    /**
     * Find the nearest player to a position
     */
    private static PlayerEntity findNearestPlayer(ServerWorld world, BlockPos pos, int maxDistance) {
        PlayerEntity closest = null;
        double closestDistance = maxDistance * maxDistance;
        
        for (PlayerEntity player : world.getPlayers()) {
            double distance = player.getBlockPos().getSquaredDistance(pos);
            if (distance < closestDistance) {
                closest = player;
                closestDistance = distance;
            }
        }
        
        return closest;
    }
    
    /**
     * Register a chest as belonging to a specific villager
     */
    public static void registerChestOwnership(BlockPos chestPos, UUID villagerUuid) {
        chestOwnership.put(chestPos, villagerUuid);
    }
    
    /**
     * Check if a chest at the given position is owned by a villager
     */
    public static boolean isChestOwned(BlockPos pos) {
        return chestOwnership.containsKey(pos);
    }
    
    /**
     * Get the UUID of the villager who owns the chest at the given position
     */
    public static UUID getChestOwner(BlockPos pos) {
        return chestOwnership.get(pos);
    }
}