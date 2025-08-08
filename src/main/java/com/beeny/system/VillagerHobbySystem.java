package com.beeny.system;

import com.beeny.Villagersreborn;
import com.beeny.ai.core.AISubsystem;
import com.beeny.data.VillagerData;
import com.beeny.constants.VillagerConstants.HobbyType;
import com.beeny.util.BoundingBoxUtils;
import com.beeny.util.VillagerUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class VillagerHobbySystem implements AISubsystem {
    
    @Override
    public void initializeVillager(@NotNull VillagerEntity villager, @NotNull VillagerData data) {
        // Hobby system doesn't need special initialization
    }
    
    @Override
    public void updateVillager(@NotNull VillagerEntity villager) {
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (data == null) return;
        
        performHobbyActivity(villager, data);
    }
    
    @Override
    public void cleanupVillager(@NotNull String villagerUuid) {
        // No persistent state to clean up
    }
    
    @Override
    public boolean needsUpdate(@NotNull VillagerEntity villager) {
        if (villager.getWorld().isClient) return false;
        
        ServerWorld world = (ServerWorld) villager.getWorld();
        boolean isHobbyTime = VillagerUtils.isTimeOfDay(world, VillagerUtils.TimeOfDay.NOON) ||
                             VillagerUtils.isTimeOfDay(world, VillagerUtils.TimeOfDay.AFTERNOON);
        
        return isHobbyTime && VillagerUtils.shouldPerformRandomAction(0.1f);
    }
    
    @Override
    public long getUpdateInterval() {
        return 10000; // Update every 10 seconds
    }
    
    @Override
    public @NotNull Map<String, Object> getAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("subsystem", "hobby");
        return analytics;
    }
    
    @Override
    public void performMaintenance() {
        // No maintenance needed
    }
    
    @Override
    public @NotNull String getSubsystemName() {
        return "VillagerHobbySystem";
    }
    
    @Override
    public void shutdown() {
        // No resources to clean up
    }
    
    public static void performHobbyActivity(VillagerEntity villager, VillagerData data) {
        if (villager.getWorld().isClient || data == null) return;
        
        HobbyType hobby = data.getHobby();
        ServerWorld world = (ServerWorld) villager.getWorld();
        
        // Only perform hobby activities during certain times and with some randomness
        boolean isHobbyTime = VillagerUtils.isTimeOfDay(world, VillagerUtils.TimeOfDay.NOON) ||
                             VillagerUtils.isTimeOfDay(world, VillagerUtils.TimeOfDay.AFTERNOON);
        
        if (!isHobbyTime || !VillagerUtils.shouldPerformRandomAction(0.1f)) return;
        
        switch (hobby) {
            case GARDENING -> performGardening(villager, data, world);
            case FISHING -> performFishing(villager, data, world);
            case COOKING -> performCooking(villager, data, world);
            case READING -> performReading(villager, data, world);
            case CRAFTING -> performCrafting(villager, data, world);
            case SINGING -> performSinging(villager, data, world);
            case DANCING -> performDancing(villager, data, world);
            case EXPLORING -> performExploring(villager, data, world);
            case COLLECTING -> performCollecting(villager, data, world);
            case GOSSIPING -> performGossiping(villager, data, world);
        }
    }
    
    private static void performGardening(VillagerEntity villager, VillagerData data, ServerWorld world) {
        BlockPos pos = villager.getBlockPos();
        
        // Look for farmland or grass nearby
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                BlockPos checkPos = pos.add(x, 0, z);
                if (world.getBlockState(checkPos).isOf(Blocks.GRASS_BLOCK) && 
                    world.getBlockState(checkPos.up()).isAir()) {
                    
                    // Plant flowers occasionally
                    if (VillagerUtils.shouldPerformRandomAction(0.02f)) {
                        world.setBlockState(checkPos.up(), Blocks.POPPY.getDefaultState());
                        VillagerUtils.spawnHappinessParticles(world, 
                            new Vec3d(checkPos.getX() + 0.5, checkPos.getY() + 1, checkPos.getZ() + 0.5), 5);
                        data.adjustHappiness(3);
                        return;
                    }
                }
            }
        }
        
        // If no suitable spot found, just get happiness from "tending plants"
        VillagerUtils.spawnParticles(world, villager.getPos().add(0, 1, 0), 
            ParticleTypes.COMPOSTER, 3, 0.3);
        data.adjustHappiness(1);
    }
    
    private static void performFishing(VillagerEntity villager, VillagerData data, ServerWorld world) {
        BlockPos pos = villager.getBlockPos();
        
        // Look for water nearby
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                BlockPos checkPos = pos.add(x, 0, z);
                if (world.getBlockState(checkPos).isOf(Blocks.WATER)) {
                    // "Catch" fish occasionally
                    if (ThreadLocalRandom.current().nextFloat() < 0.05f) {
                        // Give villager a fish
                        villager.getInventory().addStack(new ItemStack(Items.COD));
                        world.spawnParticles(ParticleTypes.FISHING,
                            checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5,
                            8, 0.5, 0.2, 0.5, 0.1);
                        world.playSound(null, checkPos, SoundEvents.ENTITY_FISHING_BOBBER_SPLASH,
                            SoundCategory.NEUTRAL, 0.8f, 1.0f);
                        data.adjustHappiness(5);
                        return;
                    }
                    
                    // Just show fishing particles
                    world.spawnParticles(ParticleTypes.BUBBLE,
                        checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5,
                        2, 0.3, 0.1, 0.3, 0.1);
                    data.adjustHappiness(1);
                    return;
                }
            }
        }
        
        // No water found, disappointed
        data.adjustHappiness(-1);
    }
    
    private static void performCooking(VillagerEntity villager, VillagerData data, ServerWorld world) {
        // Look for furnaces or campfires nearby
        BlockPos pos = villager.getBlockPos();
        boolean foundCookingSpot = false;
        
        for (int x = -5; x <= 5; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    if (world.getBlockState(checkPos).isOf(Blocks.FURNACE) ||
                        world.getBlockState(checkPos).isOf(Blocks.CAMPFIRE)) {
                        foundCookingSpot = true;
                        
                        // Occasionally produce food
                        if (ThreadLocalRandom.current().nextFloat() < 0.08f) {
                            ItemStack[] foods = {
                                new ItemStack(Items.BREAD),
                                new ItemStack(Items.COOKED_BEEF),
                                new ItemStack(Items.BAKED_POTATO),
                                new ItemStack(Items.COOKED_CHICKEN)
                            };
                            ItemStack food = foods[ThreadLocalRandom.current().nextInt(foods.length)];
                            villager.getInventory().addStack(food);
                            
                            world.spawnParticles(ParticleTypes.SMOKE,
                                checkPos.getX() + 0.5, checkPos.getY() + 1, checkPos.getZ() + 0.5,
                                5, 0.3, 0.3, 0.3, 0.1);
                            data.adjustHappiness(4);
                        }
                        break;
                    }
                }
            }
        }
        
        if (foundCookingSpot) {
            data.adjustHappiness(2);
        } else {
            // No cooking spot, mild disappointment
            data.adjustHappiness(-1);
        }
    }
    
    private static void performReading(VillagerEntity villager, VillagerData data, ServerWorld world) {
        // Look for bookshelves nearby
        BlockPos pos = villager.getBlockPos();
        boolean foundBooks = false;
        
        for (int x = -6; x <= 6; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -6; z <= 6; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    if (world.getBlockState(checkPos).isOf(Blocks.BOOKSHELF)) {
                        foundBooks = true;
                        world.spawnParticles(ParticleTypes.ENCHANT,
                            checkPos.getX() + 0.5, checkPos.getY() + 1, checkPos.getZ() + 0.5,
                            3, 0.5, 0.5, 0.5, 0.5);
                        break;
                    }
                }
            }
        }
        
        if (foundBooks) {
            data.adjustHappiness(3);
            // Readers become slightly smarter over time (affect AI decisions)
            data.getEmotionalState().adjustEmotion("intelligence", 0.1f);
        } else {
            data.adjustHappiness(1); // Still enjoys quiet time
        }
    }
    
    private static void performSinging(VillagerEntity villager, VillagerData data, ServerWorld world) {
        // Singing affects nearby villagers
        List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 10);
        
        VillagerUtils.spawnParticles(world, villager.getPos().add(0, 2, 0), 
            ParticleTypes.NOTE, 8, 1.0);
        
        VillagerUtils.playVillagerSound(world, villager.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(),
            0.6f, 1.0f + ThreadLocalRandom.current().nextFloat() * 0.4f);
        
        data.adjustHappiness(2);
        
        // Make nearby villagers happier
        VillagerUtils.adjustHappinessForNearbyVillagers(nearbyVillagers, 1);
    }
    
    private static void performDancing(VillagerEntity villager, VillagerData data, ServerWorld world) {
        // Dancing creates joy particles
        VillagerUtils.spawnParticles(world, villager.getPos().add(0, 1, 0), 
            ParticleTypes.HEART, 5, 0.8);
        
        data.adjustHappiness(3);
        
        // Encourage nearby villagers to dance too
        List<VillagerEntity> nearbyVillagers = VillagerUtils.getNearbyVillagers(villager, 8);
        
        for (VillagerEntity nearby : nearbyVillagers) {
            VillagerData nearbyData = VillagerUtils.getVillagerData(nearby);
            if (nearbyData != null && nearbyData.getHobby() == HobbyType.DANCING) {
                nearbyData.adjustHappiness(2);
                VillagerUtils.spawnHappinessParticles(world, nearby.getPos(), 3);
            }
        }
    }
    
    private static void performCrafting(VillagerEntity villager, VillagerData data, ServerWorld world) {
        // Look for crafting tables
        BlockPos pos = villager.getBlockPos();
        boolean foundCraftingTable = false;
        
        for (int x = -5; x <= 5; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    if (world.getBlockState(checkPos).isOf(Blocks.CRAFTING_TABLE)) {
                        foundCraftingTable = true;
                        
                        // Occasionally create simple items
                        if (ThreadLocalRandom.current().nextFloat() < 0.06f) {
                            ItemStack[] craftedItems = {
                                new ItemStack(Items.STICK, 2),
                                new ItemStack(Items.LADDER),
                                new ItemStack(Items.BOWL),
                                new ItemStack(Items.WOODEN_PICKAXE)
                            };
                            ItemStack item = craftedItems[ThreadLocalRandom.current().nextInt(craftedItems.length)];
                            villager.getInventory().addStack(item);
                            
                            world.spawnParticles(ParticleTypes.CRIT,
                                checkPos.getX() + 0.5, checkPos.getY() + 1, checkPos.getZ() + 0.5,
                                4, 0.3, 0.3, 0.3, 0.1);
                            data.adjustHappiness(4);
                        }
                        break;
                    }
                }
            }
        }
        
        if (foundCraftingTable) {
            data.adjustHappiness(2);
        } else {
            data.adjustHappiness(1); // Still enjoys working with hands
        }
    }
    
    private static void performExploring(VillagerEntity villager, VillagerData data, ServerWorld world) {
        // Explorers occasionally wander further and discover things
        if (ThreadLocalRandom.current().nextFloat() < 0.03f) {
            // "Discover" something interesting
            String[] discoveries = {
                "a beautiful flower patch",
                "an interesting rock formation", 
                "a small cave entrance",
                "a lovely view",
                "an unusual tree"
            };
            
            String discovery = discoveries[ThreadLocalRandom.current().nextInt(discoveries.length)];
            data.addRecentEvent("Discovered " + discovery + " while exploring");
            data.adjustHappiness(5);
            
            world.spawnParticles(ParticleTypes.END_ROD,
                villager.getX(), villager.getY() + 1, villager.getZ(),
                6, 1.0, 0.5, 1.0, 0.1);
        } else {
            data.adjustHappiness(2); // Enjoys wandering around
        }
    }
    
    private static void performCollecting(VillagerEntity villager, VillagerData data, ServerWorld world) {
        // Collectors look for items on the ground
        List<net.minecraft.entity.ItemEntity> nearbyItems = world.getEntitiesByClass(
            net.minecraft.entity.ItemEntity.class,
            BoundingBoxUtils.fromBlocks(villager.getBlockPos(), villager.getBlockPos()).expand(8),
            item -> item.isAlive() && !item.cannotPickup()
        );
        
        if (!nearbyItems.isEmpty() && ThreadLocalRandom.current().nextFloat() < 0.1f) {
            net.minecraft.entity.ItemEntity item = nearbyItems.get(0);
            ItemStack stack = item.getStack();
            
            // Only collect certain "collectible" items
            if (stack.isOf(Items.STICK) || stack.isOf(Items.FLINT) || 
                stack.isOf(Items.STONE) || stack.isOf(Items.FEATHER)) {
                
                villager.getInventory().addStack(stack.copy());
                item.discard();
                
                data.adjustHappiness(3);
                data.addRecentEvent("Found and collected " + stack.getItem().getName().getString());
                
                world.spawnParticles(ParticleTypes.CRIT,
                    item.getX(), item.getY(), item.getZ(),
                    5, 0.3, 0.3, 0.3, 0.1);
            }
        }
        
        data.adjustHappiness(1); // Enjoys searching
    }
    
    private static void performGossiping(VillagerEntity villager, VillagerData data, ServerWorld world) {
        // Find other villagers to gossip with
        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
            VillagerEntity.class,
            BoundingBoxUtils.fromBlocks(villager.getBlockPos(), villager.getBlockPos()).expand(6),
            v -> v != villager && v.isAlive()
        );
        
        if (!nearbyVillagers.isEmpty()) {
            VillagerEntity target = nearbyVillagers.get(ThreadLocalRandom.current().nextInt(nearbyVillagers.size()));
            VillagerData targetData = target.getAttached(Villagersreborn.VILLAGER_DATA);
            
            if (targetData != null) {
                // Share gossip
                world.spawnParticles(ParticleTypes.BUBBLE_POP,
                    (villager.getX() + target.getX()) / 2,
                    Math.max(villager.getY(), target.getY()) + 2,
                    (villager.getZ() + target.getZ()) / 2,
                    4, 0.5, 0.5, 0.5, 0.1);
                
                data.adjustHappiness(2);
                targetData.adjustHappiness(1);
                
                // Both villagers learn about recent events from each other
                if (!data.getRecentEvents().isEmpty()) {
                    String event = data.getRecentEvents().get(ThreadLocalRandom.current().nextInt(data.getRecentEvents().size()));
                    targetData.addRecentEvent("Heard from " + data.getName() + ": " + event);
                }
            }
        } else {
            // No one to gossip with, slightly disappointed
            data.adjustHappiness(-1);
        }
    }
    
    public static String getHobbyActivityDescription(HobbyType hobby) {
        return switch (hobby) {
            case GARDENING -> "tending to plants";
            case FISHING -> "fishing by the water";
            case COOKING -> "preparing delicious meals";
            case READING -> "reading quietly";
            case CRAFTING -> "crafting useful items";
            case SINGING -> "singing cheerfully";
            case DANCING -> "dancing joyfully";
            case EXPLORING -> "exploring the area";
            case COLLECTING -> "collecting interesting items";
            case GOSSIPING -> "chatting with neighbors";
        };
    }
}