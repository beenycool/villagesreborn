package com.beeny.villagesreborn.platform.fabric;

import com.beeny.villagesreborn.platform.fabric.gui.WelcomeScreenHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric client-side initialization for Villages Reborn
 * Handles client-specific features and UI components
 */
@Environment(EnvType.CLIENT)
public class VillagesRebornFabricClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn-fabric-client");
    
    private static WelcomeScreenHandler welcomeScreenHandler;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Villages Reborn client for Fabric");
        
        try {
            // Initialize client-specific features
            setupClientFeatures();
            
            // Register client-side event handlers
            registerClientEvents();
            
            LOGGER.info("Villages Reborn Fabric client initialization completed successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Villages Reborn client for Fabric", e);
            throw new RuntimeException("Fabric client module initialization failed", e);
        }
    }
    
    private void setupClientFeatures() {
        LOGGER.debug("Setting up client-specific features");
        
        // Initialize welcome screen handler
        welcomeScreenHandler = new WelcomeScreenHandler();
        LOGGER.debug("Welcome screen handler initialized");
    }
    
    private void registerClientEvents() {
        LOGGER.debug("Registering client-side event handlers");
        
        // Register world join event to trigger welcome screen
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            LOGGER.debug("Client joined world, triggering welcome screen check");
            if (welcomeScreenHandler != null) {
                welcomeScreenHandler.onWorldJoin();
            }
        });
        
        // Register world disconnect event
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.debug("Client disconnected from world");
            if (welcomeScreenHandler != null) {
                welcomeScreenHandler.onWorldLeave();
            }
        });
        
        // Register client tick event for any per-tick processing
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Can be used for periodic checks or updates if needed
        });
        
        LOGGER.debug("Client event handlers registered successfully");
    }
    
    /**
     * Get the welcome screen handler instance
     */
    public static WelcomeScreenHandler getWelcomeScreenHandler() {
        return welcomeScreenHandler;
    }
}