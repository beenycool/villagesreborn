package com.beeny.villagesreborn.platform.fabric;

import com.beeny.villagesreborn.core.config.FirstTimeSetupConfig;
import com.beeny.villagesreborn.core.hardware.HardwareInfoManager;
import com.beeny.villagesreborn.core.llm.LLMApiClientImpl;
import com.beeny.villagesreborn.core.llm.LLMProviderManager;
import com.beeny.villagesreborn.platform.fabric.gui.WelcomeScreenHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

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
            initializeLogging();
            validateClientEnvironment();
            setupClientFeatures();
            registerEventHandlers();
            
            LOGGER.info("Villages Reborn Fabric client initialization completed successfully");
            
        } catch (Exception e) {
            handleInitializationFailure(e);
            throw new RuntimeException("Fabric client module initialization failed", e);
        }
    }

    private void initializeLogging() {
        LOGGER.debug("Logging initialized for Villages Reborn Fabric client");
    }

    private void validateClientEnvironment() {
        // Basic environment validation
        if (MinecraftClient.getInstance() == null) {
            throw new RuntimeException("Minecraft client instance not available");
        }
        LOGGER.debug("Client environment validation passed");
    }

    private void handleInitializationFailure(Exception exception) {
        LOGGER.error("Villages Reborn Fabric client initialization failed", exception);
        // In a real implementation, we might want to disable certain features
        // or provide a degraded experience rather than crashing
    }
    
    private void setupClientFeatures() {
        LOGGER.debug("Setting up client-specific features with enhanced dependencies");
        
        // Initialize components with dependency injection
        HardwareInfoManager hardwareManager = HardwareInfoManager.getInstance();
        LLMProviderManager llmManager = createLLMProviderManager();
        FirstTimeSetupConfig setupConfig = FirstTimeSetupConfig.loadWithMigration();
        
        // Initialize welcome screen handler with enhanced features
        welcomeScreenHandler = new WelcomeScreenHandler(
            hardwareManager,
            llmManager,
            setupConfig
        );
        
        // Pre-warm hardware detection in background thread
        CompletableFuture.runAsync(() -> {
            try {
                hardwareManager.getHardwareInfoWithFallback();
                LOGGER.debug("Hardware detection pre-warmed successfully");
            } catch (Exception e) {
                LOGGER.warn("Failed to pre-warm hardware detection", e);
            }
        });
        
        LOGGER.debug("Enhanced welcome screen handler initialized");
    }

    private LLMProviderManager createLLMProviderManager() {
        // Create LLM provider manager with API client
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();
        LLMApiClientImpl apiClient = new LLMApiClientImpl(httpClient);
        return new LLMProviderManager(apiClient);
    }
    
    private void registerEventHandlers() {
        LOGGER.debug("Registering enhanced client-side event handlers");
        
        // World join event with improved timing
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            LOGGER.debug("Client joined world, scheduling welcome screen check");
            scheduleWelcomeScreenCheck(client);
        });
        
        // World disconnect event
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LOGGER.debug("Client disconnected from world");
            if (welcomeScreenHandler != null) {
                welcomeScreenHandler.onWorldLeave();
            }
        });
        
        // Client lifecycle events
        ClientLifecycleEvents.CLIENT_STARTED.register((client) -> {
            LOGGER.debug("Minecraft client started");
            if (welcomeScreenHandler != null) {
                welcomeScreenHandler.onClientStarted();
            }
        });
        
        ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> {
            LOGGER.debug("Minecraft client stopping");
            if (welcomeScreenHandler != null) {
                welcomeScreenHandler.onClientStopping();
            }
        });
        
        // Register client tick event for any per-tick processing
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Can be used for periodic checks or updates if needed
        });
        
        LOGGER.debug("Enhanced client event handlers registered successfully");
    }

    private void scheduleWelcomeScreenCheck(MinecraftClient client) {
        // Use proper scheduling to avoid timing issues
        client.execute(() -> {
            Timer timer = new Timer("WelcomeScreenCheck");
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (client.world != null && client.player != null && welcomeScreenHandler != null) {
                        client.execute(() -> welcomeScreenHandler.checkAndShowWelcomeScreen());
                    }
                    timer.cancel();
                }
            }, 2000); // 2 second delay to ensure world is fully loaded
        });
    }
    
    /**
     * Get the welcome screen handler instance
     */
    public static WelcomeScreenHandler getWelcomeScreenHandler() {
        return welcomeScreenHandler;
    }
}