package com.beeny;

import net.fabricmc.api.ModInitializer;
import com.beeny.village.VillageCraftingManager;
import com.beeny.village.Culture;
import com.beeny.setup.SystemSpecs;
import com.beeny.setup.LLMConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main entry point for the Villages Reborn mod.
 * <p>
 * Villages Reborn is a comprehensive overhaul mod that transforms Minecraft villages into 
 * culturally diverse, intelligent communities with enhanced villager AI and themed structures.
 * </p>
 * 
 * @author Beeny
 * @version 0.1.0-alpha
 */
public class Villagesreborn implements ModInitializer {
    /** Logger instance for the mod */
    public static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    
    /** System specifications for determining AI capabilities */
    private static SystemSpecs systemSpecs;
    
    /** Configuration for LLM (Large Language Model) integration */
    private static LLMConfig llmConfig;

    /** Singleton instance */
    private static Villagesreborn INSTANCE;
    
    /** Map to store village locations and their associated cultures */
    private final Map<BlockPos, Culture> villageMap = new ConcurrentHashMap<>();
    
    /** Map to store village radiuses */
    private final Map<BlockPos, Integer> villageRadiusMap = new ConcurrentHashMap<>();
    
    /** Map to store village active events */
    private final Map<BlockPos, Integer> villageEventsMap = new ConcurrentHashMap<>();
    
    /**
     * Get the singleton instance of the mod
     * @return Mod instance
     */
    public static Villagesreborn getInstance() {
        return INSTANCE;
    }
    
    /**
     * Initializes the mod components when Minecraft loads.
     * <p>
     * This method is called by the Fabric mod loader during game initialization.
     * It sets up system specifications analysis, LLM configuration, and initializes
     * the villager crafting system.
     * </p>
     */
    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Villages Reborn mod");
        
        // Set singleton instance
        INSTANCE = this;
        
        // Initialize system specifications
        systemSpecs = new SystemSpecs();
        systemSpecs.analyzeSystem();
        
        // Initialize LLM configuration
        llmConfig = new LLMConfig();
        
        // Initialize the crafting manager
        VillageCraftingManager.getInstance();
        
        LOGGER.info("Villages Reborn mod initialization complete");
    }

    /**
     * Gets the system specifications instance.
     * <p>
     * Creates and initializes a new instance if one doesn't exist.
     * </p>
     *
     * @return The SystemSpecs instance containing hardware capability information
     */
    public static SystemSpecs getSystemSpecs() {
        if (systemSpecs == null) {
            systemSpecs = new SystemSpecs();
            systemSpecs.analyzeSystem();
        }
        return systemSpecs;
    }

    /**
     * Gets the LLM configuration instance.
     * <p>
     * Creates a new instance if one doesn't exist.
     * </p>
     *
     * @return The LLMConfig instance containing AI model settings
     */
    public static LLMConfig getLLMConfig() {
        if (llmConfig == null) {
            llmConfig = new LLMConfig();
        }
        return llmConfig;
    }

    /**
     * Request village information for the player's current location
     * @param player The player to get village information for
     */
    public void pingVillageInfo(PlayerEntity player) {
        // This would be implemented to send village info to the client
        // For now it's a stub to fix compilation issues
        LOGGER.info("Village info requested for player: " + player.getName().getString());
    }
    
    /**
     * Gets the nearest village culture to a position
     * @param pos The position to check
     * @return The culture of the nearest village, or null if none found
     */
    public Culture getNearestVillageCulture(BlockPos pos) {
        if (villageMap.isEmpty()) {
            return null;
        }
        
        // Find the closest village center
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        
        for (BlockPos center : villageMap.keySet()) {
            double dist = center.getSquaredDistance(pos);
            if (dist < closestDist) {
                closestDist = dist;
                closest = center;
            }
        }
        
        if (closest != null) {
            return villageMap.get(closest);
        }
        
        return null;
    }
    
    /**
     * Gets the center position of the village at a given position
     * @param pos The position to check
     * @return The center position of the village, or null if none found
     */
    public BlockPos getVillageCenterPos(BlockPos pos) {
        if (villageMap.isEmpty()) {
            return null;
        }
        
        // Find the closest village center
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        
        for (BlockPos center : villageMap.keySet()) {
            double dist = center.getSquaredDistance(pos);
            int radius = villageRadiusMap.getOrDefault(center, 64);
            
            // Check if position is within village radius
            if (dist <= radius * radius && dist < closestDist) {
                closestDist = dist;
                closest = center;
            }
        }
        
        return closest;
    }
    
    /**
     * Gets the radius of the village at a position
     * @param pos The position to check
     * @return The radius of the village, or 0 if none found
     */
    public int getVillageRadius(BlockPos pos) {
        BlockPos center = getVillageCenterPos(pos);
        if (center != null) {
            return villageRadiusMap.getOrDefault(center, 64);
        }
        return 0;
    }
    
    /**
     * Gets the number of active events in the village at a position
     * @param pos The position to check
     * @return The number of active events, or 0 if none found
     */
    public int getActiveEventCount(BlockPos pos) {
        BlockPos center = getVillageCenterPos(pos);
        if (center != null) {
            return villageEventsMap.getOrDefault(center, 0);
        }
        return 0;
    }
    
    /**
     * Gets all villages in the world
     * @return A collection of all village cultures
     */
    public Collection<Culture> getAllVillages() {
        return villageMap.values();
    }
    
    /**
     * Registers a village with the system
     * @param center The center position of the village
     * @param culture The culture of the village
     * @param radius The radius of the village
     */
    public void registerVillage(BlockPos center, Culture culture, int radius) {
        villageMap.put(center, culture);
        villageRadiusMap.put(center, radius);
        villageEventsMap.put(center, 0);
        LOGGER.info("Registered village: " + culture.getType().getId() + " at " + center);
    }
    
    /**
     * Updates the active event count for a village
     * @param center The center of the village
     * @param eventCount The new event count
     */
    public void updateVillageEvents(BlockPos center, int eventCount) {
        if (villageMap.containsKey(center)) {
            villageEventsMap.put(center, eventCount);
        }
    }
}
