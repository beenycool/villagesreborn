package com.beeny.villagesreborn.platform.fabric.event;

import com.beeny.villagesreborn.platform.fabric.ai.VillagerAIIntegration;
import com.beeny.villagesreborn.platform.fabric.trading.DynamicTradingSystem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles AI-related events for villagers
 * Integrates with VillagerAIIntegration to provide intelligent behavior
 */
public class VillagerAIEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerAIEvents.class);
    private static final AtomicInteger tickCounter = new AtomicInteger(0);
    private static final int AI_UPDATE_INTERVAL = 100; // Update AI every 100 ticks (5 seconds)
    
    /**
     * Registers all AI-related event handlers
     */
    public static void register() {
        LOGGER.info("Registering villager AI event handlers");
        
        // Register server tick handler for AI updates
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int currentTick = tickCounter.incrementAndGet();
            
            // Only process AI updates every AI_UPDATE_INTERVAL ticks
            if (currentTick % AI_UPDATE_INTERVAL == 0) {
                processVillagerAIUpdates(server);
            }
        });
        
        LOGGER.info("Villager AI event handlers registered successfully");
    }
    
    /**
     * Processes AI updates for all villagers across all worlds
     */
    private static void processVillagerAIUpdates(net.minecraft.server.MinecraftServer server) {
        try {
            VillagerAIIntegration aiIntegration = VillagerAIIntegration.getInstance();
            
            // Process all loaded server worlds
            for (ServerWorld world : server.getWorlds()) {
                // Find all villagers in the world using a large bounding box
                List<VillagerEntity> villagers = world.getEntitiesByClass(
                    VillagerEntity.class, 
                    net.minecraft.util.math.Box.of(
                        net.minecraft.util.math.Vec3d.ZERO, 
                        30000000, 30000000, 30000000
                    ),
                    villager -> villager != null && villager.isAlive()
                );
                
                // Update AI for each villager
                for (VillagerEntity villager : villagers) {
                    try {
                        aiIntegration.updateVillagerAI(villager, world);
                    } catch (Exception e) {
                        LOGGER.debug("Failed to update AI for villager {}: {}", villager.getUuid(), e.getMessage());
                    }
                }
                
                if (!villagers.isEmpty()) {
                    LOGGER.debug("Updated AI for {} villagers in world {}", villagers.size(), world.getRegistryKey().getValue());
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error during villager AI update process", e);
        }
    }
    
    /**
     * Initializes AI for a specific villager
     */
    public static void initializeVillagerAI(VillagerEntity villager, ServerWorld world) {
        try {
            VillagerAIIntegration.getInstance().initializeVillagerAI(villager, world);
        } catch (Exception e) {
            LOGGER.error("Error initializing AI for villager {}", villager.getUuid(), e);
        }
    }
    
    /**
     * Handles villager interaction events for relationship building
     */
    public static void onVillagerInteraction(VillagerEntity villager1, VillagerEntity villager2, String interactionType) {
        try {
            VillagerAIIntegration aiIntegration = VillagerAIIntegration.getInstance();
            
            // Determine sentiment change based on interaction type
            float sentimentChange = 0.0f;
            switch (interactionType.toLowerCase()) {
                case "trade":
                    sentimentChange = 0.1f;
                    break;
                case "gift":
                    sentimentChange = 0.3f;
                    break;
                case "help":
                    sentimentChange = 0.2f;
                    break;
                case "conflict":
                    sentimentChange = -0.2f;
                    break;
                default:
                    sentimentChange = 0.05f; // Default positive interaction
            }
            
            aiIntegration.updateVillagerRelationship(
                villager1.getUuid(), 
                villager2.getUuid(), 
                sentimentChange, 
                interactionType
            );
            
        } catch (Exception e) {
            LOGGER.error("Error handling villager interaction", e);
        }
    }
} 