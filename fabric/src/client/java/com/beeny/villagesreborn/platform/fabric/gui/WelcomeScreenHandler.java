package com.beeny.villagesreborn.platform.fabric.gui;

import com.beeny.villagesreborn.core.config.FirstTimeSetupConfig;
import com.beeny.villagesreborn.core.hardware.HardwareInfoManager;
import com.beeny.villagesreborn.core.llm.LLMApiClient;
import com.beeny.villagesreborn.core.llm.LLMProviderManager;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the lifecycle and display logic for the welcome screen
 * Manages when to show the screen and coordinates with other systems
 */
public class WelcomeScreenHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WelcomeScreenHandler.class);
    
    private final HardwareInfoManager hardwareManager;
    private final LLMProviderManager llmManager;
    private FirstTimeSetupConfig setupConfig;
    
    private boolean hasCheckedForFirstTime = false;
    
    public WelcomeScreenHandler() {
        this.hardwareManager = new HardwareInfoManager();
        this.llmManager = new LLMProviderManager(new MockLLMApiClient());
        loadSetupConfig();
        
        LOGGER.debug("WelcomeScreenHandler initialized with default dependencies");
    }

    public WelcomeScreenHandler(HardwareInfoManager hardwareManager, LLMProviderManager llmManager, FirstTimeSetupConfig setupConfig) {
        this.hardwareManager = hardwareManager;
        this.llmManager = llmManager;
        this.setupConfig = setupConfig;
        
        LOGGER.debug("WelcomeScreenHandler initialized with injected dependencies");
    }
    
    /**
     * Check if the welcome screen should be shown and display it if needed
     * This is called when the player joins a world for the first time
     */
    public void checkAndShowWelcomeScreen() {
        if (hasCheckedForFirstTime) {
            return; // Already checked this session
        }
        
        hasCheckedForFirstTime = true;
        
        if (!setupConfig.isSetupCompleted()) {
            LOGGER.info("First-time setup not completed, showing welcome screen");
            showWelcomeScreen();
        } else {
            LOGGER.debug("Setup already completed, skipping welcome screen");
        }
    }
    
    /**
     * Force show the welcome screen (for testing or manual triggering)
     */
    public void showWelcomeScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // Handle null client (test environment)
        if (client == null) {
            LOGGER.debug("MinecraftClient is null (likely test environment), skipping screen display");
            return;
        }
        
        // Ensure we're on the main thread
        if (!client.isOnThread()) {
            client.execute(this::showWelcomeScreen);
            return;
        }
        
        try {
            WelcomeScreen welcomeScreen = new WelcomeScreen(hardwareManager, llmManager, setupConfig);
            client.setScreen(welcomeScreen);
            LOGGER.info("Welcome screen displayed");
        } catch (Exception e) {
            LOGGER.error("Failed to show welcome screen", e);
        }
    }
    
    /**
     * Reset the setup configuration (useful for testing)
     */
    public void resetSetup() {
        setupConfig.reset();
        hasCheckedForFirstTime = false;
        LOGGER.info("Setup configuration reset");
    }
    
    /**
     * Check if setup has been completed
     */
    public boolean isSetupCompleted() {
        return setupConfig.isSetupCompleted();
    }
    
    /**
     * Get the current setup configuration
     */
    public FirstTimeSetupConfig getSetupConfig() {
        return setupConfig;
    }
    
    /**
     * Reload the setup configuration from disk
     */
    public void reloadSetupConfig() {
        loadSetupConfig();
        hasCheckedForFirstTime = false;
    }
    
    private void loadSetupConfig() {
        try {
            setupConfig = FirstTimeSetupConfig.loadWithMigration();
            LOGGER.debug("Setup configuration loaded with migration support");
        } catch (Exception e) {
            LOGGER.error("Failed to load setup configuration", e);
            // Create a default config if loading fails
            setupConfig = FirstTimeSetupConfig.loadWithMigration(); // This will create defaults
        }
    }
    
    /**
     * Handle world join event - this is where we check for first-time setup
     */
    public void onWorldJoin() {
        LOGGER.debug("Player joined world, checking for first-time setup");
        
        // Use a small delay to ensure the world is fully loaded
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.isOnThread()) {
            // Schedule for next tick to avoid potential timing issues
            client.execute(() -> {
                try {
                    Thread.sleep(1000); // 1 second delay
                    checkAndShowWelcomeScreen();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.warn("Interrupted while waiting to show welcome screen");
                }
            });
        } else if (client == null) {
            // In test environment, just check directly
            LOGGER.debug("MinecraftClient is null (likely test environment), checking welcome screen directly");
            checkAndShowWelcomeScreen();
        }
    }
    
    /**
     * Handle world leave event - reset the check flag
     */
    public void onWorldLeave() {
        LOGGER.debug("Player left world");
        hasCheckedForFirstTime = false;
    }
    
    /**
     * Get hardware detection results
     */
    public HardwareInfoManager getHardwareManager() {
        return hardwareManager;
    }
    
    /**
     * Get LLM provider manager
     */
    public LLMProviderManager getLLMManager() {
        return llmManager;
    }

    /**
     * Handle client started event
     */
    public void onClientStarted() {
        LOGGER.debug("Minecraft client started - WelcomeScreenHandler ready");
    }

    /**
     * Handle client stopping event
     */
    public void onClientStopping() {
        LOGGER.debug("Minecraft client stopping - cleaning up WelcomeScreenHandler");
        // Cleanup any resources if needed
    }
}