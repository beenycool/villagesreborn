package com.beeny.village;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handles world tick events to process villager behaviors
 * This class ensures that villager AI behaviors are processed regularly in the game loop
 */
public class VillagerWorldTickHandler implements ServerTickEvents.EndWorldTick {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final VillagerWorldTickHandler INSTANCE = new VillagerWorldTickHandler();
    
    // Tick counter for staggering updates to avoid performance spikes
    private int tickCounter = 0;
    
    // How many villagers to process per tick
    private static final int VILLAGERS_PER_TICK = 5;
    
    private VillagerWorldTickHandler() {
        // Private constructor for singleton pattern
    }
    
    public static VillagerWorldTickHandler getInstance() {
        return INSTANCE;
    }
    
    /**
     * Initialize and register the tick handler
     */
    public static void init() {
        LOGGER.info("Initializing VillagerWorldTickHandler");
        ServerTickEvents.END_WORLD_TICK.register(INSTANCE);
    }

    /**
     * End world tick event handler
     * @param world The server world that was ticked
     */
    @Override
    public void onEndTick(ServerWorld world) {
        tickCounter++;
        
        // Only process every other tick to reduce performance impact
        if (tickCounter % 2 != 0) {
            return;
        }
        
        VillagerManager vm = VillagerManager.getInstance();
        
        // Process time-based activities for all villagers
        if (tickCounter % 100 == 0) {
            vm.updateVillagerActivities(world);
        }
        
        // Get all loaded villagers in the world
        List<VillagerEntity> loadedVillagers = world.getEntitiesByClass(
            VillagerEntity.class,
            world.getWorldBorder().asBox(),
            villager -> true
        );
        
        // Process a subset of villagers each tick for performance
        int startIdx = (tickCounter / 2) % Math.max(1, loadedVillagers.size());
        int endIdx = Math.min(startIdx + VILLAGERS_PER_TICK, loadedVillagers.size());
        
        for (int i = startIdx; i < endIdx; i++) {
            VillagerEntity villager = loadedVillagers.get(i);
            VillagerAI ai = vm.getVillagerAI(villager.getUuid());
            
            if (ai != null) {
                // Execute behavior tick for this villager
                ai.tickBehavior(world);
            }
        }
    }
    
    /**
     * Process village and cultural event updates
     * This is called less frequently than the regular tick
     */
    public void processVillageEvents(MinecraftServer server) {
        // This method would be called elsewhere, perhaps in a ServerTickEvents.END_SERVER_TICK handler
        for (ServerWorld world : server.getWorlds()) {
            VillagerManager vm = VillagerManager.getInstance();
            
            // Update cultural events
            if (tickCounter % 1200 == 0) { // Every minute (20 ticks/sec * 60 sec)
                vm.updateCulturalEvents(world);
            }
            
            // Update village statistics
            if (tickCounter % 6000 == 0) { // Every 5 minutes
                vm.updateVillageStats(world);
            }
        }
    }
}