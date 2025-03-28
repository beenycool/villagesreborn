package com.beeny.village;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles server tick events to process global village updates
 */
public class ServerTickHandler implements ServerTickEvents.EndTick {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final ServerTickHandler INSTANCE = new ServerTickHandler();
    
    // Tick counter for staggered updates
    private int tickCounter = 0;
    
    private ServerTickHandler() {
        // Private constructor for singleton pattern
    }
    
    public static ServerTickHandler getInstance() {
        return INSTANCE;
    }
    
    /**
     * Initialize and register the tick handler
     */
    public static void init() {
        LOGGER.info("Initializing ServerTickHandler");
        ServerTickEvents.END_TICK.register(INSTANCE);
    }
    
    /**
     * End server tick event handler
     * @param server The Minecraft server
     */
    @Override
    public void onEndTick(MinecraftServer server) {
        tickCounter++;
        
        // Process village events every 10 ticks (0.5 seconds)
        if (tickCounter % 10 == 0) {
            VillagerWorldTickHandler.getInstance().processVillageEvents(server);
        }
        
        // Synchronize village data every 5 minutes
        if (tickCounter % 6000 == 0) {
            VillagerManager vm = VillagerManager.getInstance();
            vm.syncVillageData(server);
        }
    }
}