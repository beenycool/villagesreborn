package com.beeny;

import com.beeny.ai.ModelChecker;
import com.beeny.gui.ModelDownloadScreen;
import com.beeny.gui.SetupScreen;
import com.beeny.gui.VillageInfoHud;
import com.beeny.gui.ConversationHud;
import com.beeny.gui.EventNotificationManager;
import com.beeny.gui.VillageDebugScreen;
import com.beeny.setup.LLMConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side initialization for Villages Reborn.
 * <p>
 * Handles client-specific features like GUI elements, HUD rendering,
 * user configuration and keybinds.
 * </p>
 */
public class VillagesrebornClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private boolean setupScreenShown = false;
    private boolean modelCheckDone = false;
    private int tickCounter = 0;
    private static final int SETUP_DELAY_TICKS = 40; // Delay showing setup screen for about 2 seconds
    
    // Singleton instance for access from other classes
    private static VillagesrebornClient INSTANCE;
    
    // HUD components
    private final VillageInfoHud villageInfoHud;
    private final EventNotificationManager eventManager;
    private final ConversationHud conversationHud;
    
    // Keybindings
    private KeyBinding toggleHudKeyBinding;
    private KeyBinding villagePingKeyBinding;
    private KeyBinding villageDebugKeyBinding;
    private KeyBinding cycleConversationPositionKeyBinding;
    
    // HUD state
    private boolean hudEnabled = true;
    
    public VillagesrebornClient() {
        INSTANCE = this;
        villageInfoHud = new VillageInfoHud();
        eventManager = new EventNotificationManager();
        conversationHud = new ConversationHud();
    }
    
    public static VillagesrebornClient getInstance() {
        return INSTANCE;
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Villages Reborn client");

        // Register keybindings
        registerKeybindings();
        
        // Register client tick event with delayed setup screen
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Initial setup screen handling
            if (!setupScreenShown && client.world != null) {
                if (tickCounter >= SETUP_DELAY_TICKS) {
                    setupScreenShown = true;
                    LLMConfig llmConfig = Villagesreborn.getLLMConfig();
                    
                    // Check if we need to show the model download screen
                    if (!modelCheckDone) {
                        modelCheckDone = true;
                        // Only show model download screen if:
                        // 1. We don't have a local model
                        // 2. The provider is set to local or isn't configured yet
                        String provider = llmConfig.getProvider();
                        boolean needsLocalModel = !ModelChecker.hasLocalModel() && 
                                                 (provider.equals("local") || !llmConfig.isConfigured());
                        
                        if (needsLocalModel) {
                            client.setScreen(new ModelDownloadScreen(null, llmConfig));
                            return;
                        }
                    }
                    
                    // If no model screen needed, show regular setup
                    if (!llmConfig.isConfigured()) {
                        client.setScreen(new SetupScreen(llmConfig));
                    }
                }
                tickCounter++;
            }
            
            // Process keybindings
            handleKeybindings(client);
        });
        
        // Register HUD rendering
        HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.world != null && !client.options.hudHidden && hudEnabled) {
                // Render village info HUD
                villageInfoHud.render(context, client);
                
                // Render event notifications
                // Fix: Pass tickDelta directly as it's already a float in the callback
                eventManager.render(context, client, tickDelta);
                
                // Render conversation indicator
                conversationHud.render(context, client);
            }
        });

        // Register other client-side event handlers
        registerEventHandlers();

        LOGGER.info("Villages Reborn client initialized successfully");
    }
    
    /**
     * Registers the mod's keybindings.
     */
    private void registerKeybindings() {
        // Toggle village HUD visibility (V key)
        toggleHudKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.villagesreborn.togglehud",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.villagesreborn.general"
        ));
        
        // Ping nearby village info (B key)
        villagePingKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.villagesreborn.villageping",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "category.villagesreborn.general"
        ));
        
        // Debug village information (Shift+V key combination)
        villageDebugKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.villagesreborn.villagedebug",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "category.villagesreborn.debug"
        ));
        
        // Cycle conversation HUD position (C key)
        cycleConversationPositionKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.villagesreborn.cycleconversationposition",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "category.villagesreborn.general"
        ));
    }
    
    /**
     * Handles keybinding presses.
     */
    private void handleKeybindings(MinecraftClient client) {
        // Toggle HUD
        if (toggleHudKeyBinding.wasPressed()) {
            hudEnabled = !hudEnabled;
            if (client.player != null) {
                client.player.sendMessage(
                    Text.of("Village HUD: " + (hudEnabled ? "§aEnabled" : "§cDisabled")),
                    true); // Action bar message
            }
        }
        
        // Village ping
        if (villagePingKeyBinding.wasPressed() && client.player != null) {
            // Request latest village info for the player's current location
            Villagesreborn.getInstance().pingVillageInfo(client.player);
        }
        
        // Village debug screen (only when shift is held)
        if (villageDebugKeyBinding.wasPressed() && 
                InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), 
                GLFW.GLFW_KEY_LEFT_SHIFT)) {
            client.setScreen(new VillageDebugScreen());
        }
        
        // Cycle conversation HUD position
        if (cycleConversationPositionKeyBinding.wasPressed()) {
            cycleConversationPosition();
        }
    }
    
    /**
     * Cycle through the available positions for the conversation HUD
     */
    private void cycleConversationPosition() {
        ConversationHud.HudPosition currentPosition = conversationHud.getPosition();
        ConversationHud.HudPosition newPosition;
        
        switch (currentPosition) {
            case TOP_LEFT:
                newPosition = ConversationHud.HudPosition.TOP_RIGHT;
                break;
            case TOP_RIGHT:
                newPosition = ConversationHud.HudPosition.BOTTOM_RIGHT;
                break;
            case BOTTOM_RIGHT:
                newPosition = ConversationHud.HudPosition.BOTTOM_LEFT;
                break;
            case BOTTOM_LEFT:
            default:
                newPosition = ConversationHud.HudPosition.TOP_LEFT;
                break;
        }
        
        conversationHud.setPosition(newPosition);
        
        // Show feedback to the player
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(
                Text.of("Conversation indicator position: " + newPosition.name()),
                true
            );
        }
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
        // Only show if HUD is enabled
        if (hudEnabled) {
            eventManager.addNotification(title, description, durationTicks);
        }
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
        // Persist the info even when HUD is disabled
        villageInfoHud.update(cultureName, prosperity, safety, population);
    }
    
    /**
     * Force the HUD to show for a specific duration.
     * Useful when pinging for village info.
     * 
     * @param durationMillis Duration to show in milliseconds
     */
    public void showHudTemporarily(long durationMillis) {
        villageInfoHud.forceDisplay(durationMillis);
    }
    
    /**
     * Checks if the HUD is currently enabled.
     */
    public boolean isHudEnabled() {
        return hudEnabled;
    }
    
    /**
     * Sets whether the HUD should be enabled or disabled.
     */
    public void setHudEnabled(boolean enabled) {
        this.hudEnabled = enabled;
    }
    
    /**
     * Start or update a conversation with a villager
     * 
     * @param villager The villager being spoken to
     */
    public void startConversation(VillagerEntity villager) {
        conversationHud.startConversation(villager);
    }
    
    /**
     * End the current conversation
     */
    public void endConversation() {
        conversationHud.endConversation();
    }
    
    /**
     * Update the timestamp of the current conversation to prevent timeout
     */
    public void updateConversation() {
        conversationHud.updateInteraction();
    }
    
    /**
     * Check if a conversation is currently active
     */
    public boolean isInConversation() {
        return conversationHud.isConversationActive();
    }
    
    /**
     * Get the villager currently being spoken to
     */
    public VillagerEntity getCurrentConversationVillager() {
        return conversationHud.getCurrentVillager();
    }
}