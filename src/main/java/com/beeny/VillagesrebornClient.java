package com.beeny;

import com.beeny.gui.SetupScreen;
import com.beeny.setup.LLMConfig;
import com.beeny.gui.VillageInfoHud;
import com.beeny.gui.EventNotificationManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side initialization for Villages Reborn.
 * <p>
 * Handles client-specific features like GUI elements, HUD rendering,
 * and user configuration.
 * </p>
 */
public class VillagesrebornClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private boolean setupScreenShown = false;
    private int tickCounter = 0;
    private static final int SETUP_DELAY_TICKS = 40; // Delay showing setup screen for about 2 seconds
    
    // Singleton instance for access from other classes
    private static VillagesrebornClient INSTANCE;
    
    // HUD components
    private final VillageInfoHud villageInfoHud;
    private final EventNotificationManager eventManager;
    
    public VillagesrebornClient() {
        INSTANCE = this;
        villageInfoHud = new VillageInfoHud();
        eventManager = new EventNotificationManager();
    }
    
    public static VillagesrebornClient getInstance() {
        return INSTANCE;
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Villages Reborn client");

        // Register client tick event with delayed setup screen
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!setupScreenShown && client.world != null) {
                // Wait a short time before showing setup screen to avoid overwhelming the player at startup
                if (tickCounter >= SETUP_DELAY_TICKS) {
                    setupScreenShown = true;
                    LLMConfig llmConfig = Villagesreborn.getLLMConfig();
                    client.setScreen(new SetupScreen(llmConfig));
                }
                tickCounter++;
            }
        });

        // Register HUD rendering
        HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.world != null && !client.options.hudHidden) {
                // Render village info HUD
                villageInfoHud.render(matrixStack, client);
                
                // Render event notifications
                eventManager.render(matrixStack, client, tickDelta);
            }
        });

        // Client-side event handlers could be registered here
        registerEventHandlers();

        LOGGER.info("Villages Reborn client initialized successfully");
    }
    
    private void registerEventHandlers() {
        // Any additional event handlers can be registered here
    }
    
    /**
     * Displays a cultural event notification to the player.
     *
     * @param title The event title
     * @param description Brief description of the event
     * @param durationTicks How long to display the notification
     */
    public void showEventNotification(String title, String description, int durationTicks) {
        eventManager.addNotification(title, description, durationTicks);
    }
    
    /**
     * Updates the village info displayed in the HUD.
     *
     * @param cultureName The name of the village culture
     * @param prosperity The prosperity value (0-100)
     * @param safety The safety value (0-100)
     * @param population The current population count
     */
    public void updateVillageInfo(String cultureName, int prosperity, int safety, int population) {
        villageInfoHud.update(cultureName, prosperity, safety, population);
    }
}