package com.beeny;

import com.beeny.gui.EventNotificationManager;
import com.beeny.gui.VillageInfoHud;
import com.beeny.network.VillagesClientNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side initialization for the Villages Reborn mod.
 */
public class VillagesrebornClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn-client");
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Villages Reborn client");
        
        // Create HUD components
        VillageInfoHud villageInfoHud = VillageInfoHud.getInstance();
        EventNotificationManager eventNotificationManager = EventNotificationManager.getInstance();
        
        // Register HUD rendering
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            villageInfoHud.render(context, context.getClient(), tickDelta);
            eventNotificationManager.render(context, context.getClient(), tickDelta);
        });
        
        // Register client-side networking
        registerNetworking();
        
        LOGGER.info("Villages Reborn client initialized");
    }
    
    /**
     * Register client-side networking components
     */
    private void registerNetworking() {
        LOGGER.info("Registering client-side networking handlers");
        
        // Register all client-side packet handlers
        VillagesClientNetwork.registerHandlers();
        
        LOGGER.info("Client-side network handlers registered successfully");
    }
    
    /**
     * Ping the server for village info
     * This can be called from the HUD during gameplay
     */
    public void pingVillageInfo() {
        VillageInfoHud.getInstance().requestUpdate();
    }
}
