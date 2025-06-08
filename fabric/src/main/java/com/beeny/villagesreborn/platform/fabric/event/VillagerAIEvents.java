package com.beeny.villagesreborn.platform.fabric.event;

import com.beeny.villagesreborn.platform.fabric.ai.VillagerAIIntegration;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Event handlers for integrating AI with villager lifecycle
 */
public class VillagerAIEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerAIEvents.class);
    private static boolean registered = false;
    
    public static void register() {
        if (registered) {
            LOGGER.warn("VillagerAIEvents already registered");
            return;
        }
        
        LOGGER.info("Registering Villager AI event handlers");
        
        // Register server tick handler for AI updates
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                // Update AI for all villagers in all worlds every few ticks
                if (server.getTicks() % 100 == 0) { // Every 5 seconds (20 ticks/second * 5)
                    updateAllVillagerAI(server);
                }
                
                // Cleanup every 10 minutes
                if (server.getTicks() % 12000 == 0) {
                    VillagerAIIntegration.getInstance().cleanup();
                }
            } catch (Exception e) {
                LOGGER.error("Error in villager AI tick handler", e);
            }
        });
        
        registered = true;
        LOGGER.info("Successfully registered Villager AI event handlers");
    }
    
    /**
     * Updates AI for all villagers across all worlds
     */
    private static void updateAllVillagerAI(net.minecraft.server.MinecraftServer server) {
        try {
            VillagerAIIntegration aiIntegration = VillagerAIIntegration.getInstance();
            
            for (ServerWorld world : server.getWorlds()) {
                List<VillagerEntity> villagers = world.getEntitiesByClass(
                    VillagerEntity.class, 
                    net.minecraft.util.math.Box.of(
                        net.minecraft.util.math.Vec3d.ZERO,
                        world.getWorldBorder().getSize(),
                        world.getWorldBorder().getSize(),
                        world.getWorldBorder().getSize()
                    ), 
                    villager -> villager.isAlive()
                );
                
                for (VillagerEntity villager : villagers) {
                    try {
                        aiIntegration.updateVillagerAI(villager, world);
                    } catch (Exception e) {
                        LOGGER.debug("Error updating AI for villager {}: {}", villager.getUuid(), e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error updating villager AI across all worlds", e);
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