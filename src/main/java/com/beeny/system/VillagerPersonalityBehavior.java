package com.beeny.system;

import com.beeny.Villagersreborn;
import com.beeny.ai.core.AISubsystem;
import com.beeny.data.VillagerData;
import com.beeny.constants.VillagerConstants.PersonalityType;
import com.beeny.system.personality.PersonalityStrategy;
import com.beeny.system.personality.PersonalityStrategyFactory;
import com.beeny.util.BoundingBoxUtils;
import com.beeny.util.VillagerUtils;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class VillagerPersonalityBehavior implements AISubsystem {
    
    private final Map<String, Long> villagerLastUpdate = new ConcurrentHashMap<>();
    private long updateCount = 0;
    private long lastMaintenanceTime = System.currentTimeMillis();
    
    public VillagerPersonalityBehavior() {
        // Initialize if needed
    }
    
    public static void applyPersonalityEffects(VillagerEntity villager, VillagerData data) {
        if (villager.getWorld().isClient || data == null) return;
        
        PersonalityType personality = data.getPersonality();
        ServerWorld world = (ServerWorld) villager.getWorld();
        
        // Use strategy pattern instead of switch statement
        PersonalityStrategy strategy = PersonalityStrategyFactory.getStrategy(personality);
        if (strategy != null) {
            strategy.applyPersonalityEffects(villager, data, world);
        }
    }
    
    // Static utility methods using strategy pattern
    
    public static float getPersonalityHappinessModifier(PersonalityType personality) {
        PersonalityStrategy strategy = PersonalityStrategyFactory.getStrategy(personality);
        return strategy != null ? strategy.getHappinessModifier() : 1.0f;
    }
    
    public static int getPersonalityTradeBonus(PersonalityType personality, int playerReputation) {
        PersonalityStrategy strategy = PersonalityStrategyFactory.getStrategy(personality);
        return strategy != null ? strategy.getTradeBonus(playerReputation) : 0;
    }
    
    public static String getPersonalitySpecificGreeting(PersonalityType personality, String villagerName, int reputation) {
        PersonalityStrategy strategy = PersonalityStrategyFactory.getStrategy(personality);
        return strategy != null ? strategy.getGreeting(villagerName, reputation) : "Hello.";
    }
    
    // AISubsystem interface implementation
    
    @Override
    public void initializeVillager(@NotNull VillagerEntity villager, @NotNull VillagerData data) {
        String villagerUuid = villager.getUuidAsString();
        villagerLastUpdate.put(villagerUuid, System.currentTimeMillis());
    }
    
    @Override
    public void updateVillager(@NotNull VillagerEntity villager) {
        String villagerUuid = villager.getUuidAsString();
        long currentTime = System.currentTimeMillis();
        villagerLastUpdate.put(villagerUuid, currentTime);
        updateCount++;
        
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (data != null) {
            applyPersonalityEffects(villager, data);
        }
    }
    
    @Override
    public void cleanupVillager(@NotNull String villagerUuid) {
        villagerLastUpdate.remove(villagerUuid);
    }
    
    @Override
    public boolean needsUpdate(@NotNull VillagerEntity villager) {
        String villagerUuid = villager.getUuidAsString();
        Long lastUpdate = villagerLastUpdate.get(villagerUuid);
        if (lastUpdate == null) return true;
        
        return System.currentTimeMillis() - lastUpdate >= this.getUpdateInterval();
    }
    
    @Override
    public long getUpdateInterval() {
        return 15000; // Update every 15 seconds
    }
    
    @Override
    @NotNull
    public Map<String, Object> getAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("trackedVillagers", villagerLastUpdate.size());
        analytics.put("updateCount", updateCount);
        analytics.put("lastMaintenanceTime", lastMaintenanceTime);
        return analytics;
    }
    
    @Override
    public void performMaintenance() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMaintenanceTime > 300000) { // 5 minutes
            // Clean up old entries for villagers that might have been unloaded
            villagerLastUpdate.entrySet().removeIf(entry -> 
                currentTime - entry.getValue() > 600000); // Remove entries older than 10 minutes
            lastMaintenanceTime = currentTime;
        }
    }
    
    @Override
    @NotNull
    public String getSubsystemName() {
        return "VillagerPersonalityBehavior";
    }
    
    @Override
    public int getPriority() {
        return 40; // Medium priority for personality effects
    }
    
    @Override
    public void shutdown() {
        villagerLastUpdate.clear();
    }
}