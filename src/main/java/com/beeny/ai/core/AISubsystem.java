package com.beeny.ai.core;

import com.beeny.data.VillagerData;
import net.minecraft.entity.passive.VillagerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Base interface for all AI subsystems.
 * Provides lifecycle management and consistent update patterns.
 */
public interface AISubsystem {
    
    /**
     * Initialize AI state for a villager when they are first loaded or created.
     * This should be idempotent - calling multiple times should not cause issues.
     * 
     * @param villager The villager entity
     * @param data The villager's persistent data
     */
    void initializeVillager(@NotNull VillagerEntity villager, @NotNull VillagerData data);
    
    /**
     * Update AI processing for a villager.
     * This is called periodically based on the subsystem's update frequency.
     * 
     * @param villager The villager entity
     */
    void updateVillager(@NotNull VillagerEntity villager);
    
    /**
     * Clean up AI state when a villager is unloaded or removed.
     * This should free any resources and clear any cached data.
     * 
     * @param villagerUuid The UUID of the villager to clean up
     */
    void cleanupVillager(@NotNull String villagerUuid);
    
    /**
     * Check if this subsystem needs an update for the given villager.
     * Used for efficient scheduling and load balancing.
     * 
     * @param villager The villager entity
     * @return true if this subsystem should be updated this tick
     */
    boolean needsUpdate(@NotNull VillagerEntity villager);
    
    /**
     * Get the minimum interval between updates for this subsystem in milliseconds.
     * Used by the scheduler to determine update frequency.
     * 
     * @return minimum update interval in milliseconds
     */
    long getUpdateInterval();
    
    /**
     * Get analytics data for monitoring and debugging.
     * This should return lightweight data suitable for frequent collection.
     * 
     * @return map of analytics data
     */
    @NotNull
    Map<String, Object> getAnalytics();
    
    /**
     * Perform any periodic maintenance tasks.
     * Called less frequently than update() for cleanup and optimization.
     */
    void performMaintenance();
    
    /**
     * Get the name of this subsystem for logging and debugging.
     * 
     * @return human-readable subsystem name
     */
    @NotNull
    String getSubsystemName();
    
    /**
     * Check if this subsystem is enabled and should be running.
     * Allows for runtime disabling of subsystems.
     * 
     * @return true if this subsystem should be active
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * Get the priority of this subsystem for execution ordering.
     * Lower numbers run first. Used when subsystem dependencies matter.
     * 
     * @return priority value (0 = highest priority)
     */
    default int getPriority() {
        return 100; // Default medium priority
    }
    
    /**
     * Called when the server is shutting down.
     * Override if the subsystem needs special shutdown handling.
     */
    void shutdown();
}