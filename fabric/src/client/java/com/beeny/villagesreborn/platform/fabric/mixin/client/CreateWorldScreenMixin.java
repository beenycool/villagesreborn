package com.beeny.villagesreborn.platform.fabric.mixin.client;

import com.beeny.villagesreborn.core.config.ModConfig;
import com.beeny.villagesreborn.core.world.WorldCreationSettingsCapture;
import com.beeny.villagesreborn.platform.fabric.gui.BiomeSelectorScreen;
import com.beeny.villagesreborn.platform.fabric.gui.world.VillagesRebornTab;
import com.beeny.villagesreborn.platform.fabric.spawn.BiomeSelectorEventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.GridWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to inject Villages Reborn configuration tab into the Create World screen
 * Provides seamless integration without modifying vanilla code directly
 */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateWorldScreenMixin.class);
    
    @Unique
    private VillagesRebornTab villagesRebornTab;
    
    @Unique
    private ButtonWidget spawnBiomeButton;
    
    @Unique
    private boolean biomeSelectorOpen = false;
    
    /**
     * Injects our custom tab after the screen has been initialized
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void addVillagesRebornTab(CallbackInfo ci) {
        if (!shouldInjectTab()) {
            LOGGER.debug("Villages Reborn tab injection disabled");
            return;
        }
        
        try {
            CreateWorldScreen self = (CreateWorldScreen) (Object) this;
            
            // Create our custom tab widget for 1.21.4
            this.villagesRebornTab = new VillagesRebornTab(self.width - 50, self.height - 100);
            
            LOGGER.info("Successfully created Villages Reborn tab widget for Create World screen");
            
        } catch (Exception e) {
            LOGGER.error("Failed to create Villages Reborn tab widget", e);
        }
        
        // Add spawn biome button if enabled (separate try-catch to not break tab creation)
        if (shouldInjectSpawnBiomeButton()) {
            try {
                createSpawnBiomeButton();
                LOGGER.info("Successfully created Spawn Biome button for Create World screen");
            } catch (Exception e) {
                LOGGER.error("Failed to create Spawn Biome button", e);
            }
        }
    }
    
    /**
     * Captures world settings when the world is about to be created
     * This ensures settings are transferred from UI to world generation
     */
    @Inject(method = "createLevel", at = @At("HEAD"))
    private void captureWorldSettings(CallbackInfo ci) {
        if (this.villagesRebornTab == null) {
            LOGGER.debug("No Villages Reborn tab to capture settings from");
            return;
        }
        
        try {
            var settings = this.villagesRebornTab.getSettings();
            if (settings != null) {
                WorldCreationSettingsCapture.capture(settings);
                LOGGER.info("Captured world settings for new world: {}", settings);
            } else {
                LOGGER.warn("Villages Reborn tab has no settings to capture");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to capture world settings", e);
        }
    }
    
    /**
     * Cleanup when screen is closed
     */
    @Inject(method = "close", at = @At("HEAD"))
    private void onScreenClose(CallbackInfo ci) {
        if (this.villagesRebornTab != null) {
            try {
                this.villagesRebornTab.onClose();
                LOGGER.debug("Cleaned up Villages Reborn tab");
            } catch (Exception e) {
                LOGGER.error("Error cleaning up Villages Reborn tab", e);
            }
        }
    }
    
    /**
     * Determines whether the tab should be injected
     * Checks mod configuration and feature flags
     */
    @Unique
    private boolean shouldInjectTab() {
        try {
            // Check if world creation UI is enabled in mod config
            boolean enabled = ModConfig.getInstance().isWorldCreationUIEnabled();
            LOGGER.info("World creation UI enabled: {}", enabled);
            return enabled;
        } catch (Exception e) {
            LOGGER.error("Failed to check mod config for tab injection", e);
            // Default to enabled if config check fails
            return true;
        }
    }
    
    /**
     * Provides access to the injected tab for external use
     * Used primarily for testing and debugging
     */
    @Unique
    public VillagesRebornTab getVillagesRebornTab() {
        return this.villagesRebornTab;
    }
    
    /**
     * Validates that the injection was successful
     * Used for monitoring and debugging
     */
    @Unique
    public boolean isTabInjected() {
        return this.villagesRebornTab != null;
    }
    
    /**
     * Determines whether the spawn biome button should be injected
     */
    @Unique
    private boolean shouldInjectSpawnBiomeButton() {
        try {
            boolean spawnBiomeEnabled = ModConfig.getInstance().isSpawnBiomeSelectionEnabled();
            boolean notCreativeMode = !isCreativeMode();
            boolean overworldGen = isOverworldGeneration();
            
            LOGGER.info("Spawn biome button injection check: enabled={}, notCreative={}, overworld={}", 
                       spawnBiomeEnabled, notCreativeMode, overworldGen);
            
            return spawnBiomeEnabled && notCreativeMode && overworldGen;
        } catch (Exception e) {
            LOGGER.error("Failed to check spawn biome button injection config", e);
            return false;
        }
    }
    
    /**
     * Creates and positions the spawn biome button
     */
    @Unique
    private void createSpawnBiomeButton() {
        if (this.spawnBiomeButton != null) {
            // Prevent duplicate button creation
            return;
        }
        
        CreateWorldScreen self = (CreateWorldScreen) (Object) this;
        
        // Calculate GUI scaling for better responsiveness
        double guiScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        int buttonWidth = Math.min(200, (int) Math.max(150, 200 / guiScale * 2));
        int buttonHeight = (int) Math.max(20, 24 / guiScale * 2);
        int buttonX = self.width / 2 - buttonWidth / 2;
        int bottomPadding = (int) Math.max(80, 100 / guiScale * 2);
        int buttonY = self.height - bottomPadding; // Position with scaled padding
        
        LOGGER.info("Creating spawn biome button at position ({}, {}) with size {}x{} (GUI scale: {})", 
                   buttonX, buttonY, buttonWidth, buttonHeight, guiScale);
        
        this.spawnBiomeButton = ButtonWidget.builder(
            Text.translatable("villagesreborn.spawn_biome.button"),
            this::onSpawnBiomeButtonClick
        )
        .position(buttonX, buttonY)
        .size(buttonWidth, buttonHeight)
        .build();
        
        // Add to screen using reflection to access protected method
        try {
            java.lang.reflect.Method addMethod = self.getClass().getSuperclass().getDeclaredMethod("addDrawableChild", net.minecraft.client.gui.Element.class);
            addMethod.setAccessible(true);
            addMethod.invoke(self, this.spawnBiomeButton);
            LOGGER.info("Successfully added spawn biome button to screen");
        } catch (Exception e) {
            LOGGER.error("Failed to add spawn biome button to screen", e);
        }
    }
    
    /**
     * Handles spawn biome button click
     */
    @Unique
    private void onSpawnBiomeButtonClick(ButtonWidget button) {
        if (!biomeSelectorOpen) {
            try {
                biomeSelectorOpen = true;
                CreateWorldScreen self = (CreateWorldScreen) (Object) this;
                BiomeSelectorScreen screen = BiomeSelectorScreen.createForWorldCreation(self);
                MinecraftClient.getInstance().setScreen(screen);
                LOGGER.info("Opened biome selector from world creation screen");
            } catch (Exception e) {
                LOGGER.error("Failed to open biome selector screen", e);
                biomeSelectorOpen = false; // Reset state on error
            }
        } else {
            LOGGER.debug("Biome selector already open, ignoring button click");
        }
    }
    
    /**
     * Checks if we're in creative mode
     */
    @Unique
    private boolean isCreativeMode() {
        // For simplicity, always return false for now
        // In a full implementation, this would check the game mode setting
        return false;
    }
    
    /**
     * Checks if we're generating an overworld
     */
    @Unique
    private boolean isOverworldGeneration() {
        // For simplicity, always return true for now
        // In a full implementation, this would check the dimension type
        return true;
    }
    
    /**
     * Gets the spawn biome button for testing
     */
    @Unique
    public ButtonWidget getSpawnBiomeButton() {
        return this.spawnBiomeButton;
    }
    
    /**
     * Checks if biome selector is open
     */
    @Unique
    public boolean isBiomeSelectorOpen() {
        return this.biomeSelectorOpen;
    }
    
    /**
     * Resets biome selector open state (called when returning from selector)
     */
    @Unique
    public void resetBiomeSelectorState() {
        this.biomeSelectorOpen = false;
        LOGGER.debug("Reset biome selector state");
    }
}