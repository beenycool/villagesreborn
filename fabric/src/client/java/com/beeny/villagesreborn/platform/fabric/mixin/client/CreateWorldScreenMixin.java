package com.beeny.villagesreborn.platform.fabric.mixin.client;

import com.beeny.villagesreborn.core.config.ModConfig;
import com.beeny.villagesreborn.core.world.WorldCreationSettingsCapture;
import com.beeny.villagesreborn.platform.fabric.gui.world.VillagesRebornTab;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
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
            return ModConfig.getInstance().isWorldCreationUIEnabled();
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
}