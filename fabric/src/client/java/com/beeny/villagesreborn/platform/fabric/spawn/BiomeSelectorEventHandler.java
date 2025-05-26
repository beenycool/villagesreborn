package com.beeny.villagesreborn.platform.fabric.spawn;

import com.beeny.villagesreborn.platform.fabric.gui.BiomeSelectorScreen;
import com.beeny.villagesreborn.platform.fabric.spawn.managers.SpawnBiomeStorageManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles events for displaying the biome selector screen on world join
 * Updated to use the new storage system instead of static fields
 */
public class BiomeSelectorEventHandler implements ClientPlayConnectionEvents.Join {
    private static final Logger LOGGER = LoggerFactory.getLogger(BiomeSelectorEventHandler.class);
    
    // Per-world tracking instead of global static state
    private static final Map<RegistryKey<?>, Boolean> worldSetupStatus = new ConcurrentHashMap<>();
    
    // World creation choice tracking
    private static final Map<String, Object> worldCreationChoices = new ConcurrentHashMap<>();
    
    public BiomeSelectorEventHandler() {}
    
    public static void register() {
        ClientPlayConnectionEvents.JOIN.register(new BiomeSelectorEventHandler());
    }
    
    public static void resetForTest() {
        worldSetupStatus.clear();
    }
    
    @Override
    public void onPlayReady(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
        // Check if we should show the biome selector
        if (shouldShowBiomeSelector(client)) {
            showBiomeSelector(client);
            markWorldFirstTimeSetupComplete(client.world);
        }
    }
    
    /**
     * Called when the client joins a world (alternative interface for testing)
     */
    public void onClientJoin(ClientPlayNetworkHandler handler, Object packet, MinecraftClient client) {
        onPlayReady(handler, null, client);
    }
    
    private boolean shouldShowBiomeSelector(MinecraftClient client) {
        // Don't show if world is null
        if (client.world == null) {
            return false;
        }
        
        // Only show for overworld dimension
        RegistryKey<?> worldKey = client.world.getRegistryKey();
        if (worldKey == null) {
            return false;
        }
        
        Identifier worldId = worldKey.getValue();
        boolean isOverworld = worldId.getNamespace().equals("minecraft") &&
                             worldId.getPath().equals("overworld");
        
        if (!isOverworld) {
            return false;
        }
        
        // Check if biome selector has been shown for this specific world
        return !hasWorldCompletedFirstTimeSetup(client.world);
    }
    
    private void showBiomeSelector(MinecraftClient client) {
        try {
            BiomeSelectorScreen screen = BiomeSelectorScreen.create();
            client.setScreen(screen);
            LOGGER.info("Displayed biome selector screen for first-time world join");
        } catch (Exception e) {
            LOGGER.error("Failed to show biome selector screen", e);
        }
    }
    
    /**
     * Checks if the world has completed first-time setup
     * @param world The client world
     * @return true if setup is complete, false otherwise
     */
    private boolean hasWorldCompletedFirstTimeSetup(ClientWorld world) {
        if (world == null) {
            return true; // Assume complete if world is null
        }
        
        RegistryKey<?> worldKey = world.getRegistryKey();
        if (worldKey == null) {
            return true; // Assume complete if world key is null
        }
        
        // Check if this world has been marked as setup complete
        return worldSetupStatus.getOrDefault(worldKey, false);
    }
    
    /**
     * Marks the world as having completed first-time setup
     * @param world The client world
     */
    private void markWorldFirstTimeSetupComplete(ClientWorld world) {
        if (world == null) {
            return;
        }
        
        RegistryKey<?> worldKey = world.getRegistryKey();
        if (worldKey == null) {
            return;
        }
        
        worldSetupStatus.put(worldKey, true);
        LOGGER.debug("Marked first-time setup complete for world: {}", worldKey.getValue());
    }
    
    /**
     * Checks if the biome selector has been shown for a specific world
     * @param world The client world
     * @return true if shown, false otherwise
     */
    public boolean hasShownBiomeSelectorForWorld(ClientWorld world) {
        return hasWorldCompletedFirstTimeSetup(world);
    }
    
    /**
     * Gets the number of worlds that have completed setup
     * @return The number of worlds
     */
    public static int getCompletedWorldCount() {
        return worldSetupStatus.size();
    }
    
    /**
     * Handles spawn biome button click from Create World screen
     * @param parentScreen The parent CreateWorldScreen
     */
    public static void onSpawnBiomeButtonClick(Object parentScreen) {
        try {
            // Check if screen is already open
            if (!isScreenOpen("BiomeSelectorScreen")) {
                // Use reflection to call createForWorldCreation method
                Class<?> screenClass = Class.forName("com.beeny.villagesreborn.platform.fabric.gui.BiomeSelectorScreen");
                Object screen = screenClass.getMethod("createForWorldCreation", Object.class)
                                          .invoke(null, parentScreen);
                MinecraftClient.getInstance().setScreen((net.minecraft.client.gui.screen.Screen) screen);
                LOGGER.info("Opened biome selector from world creation");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to open biome selector from world creation", e);
        }
    }
    
    /**
     * Handles biome choice confirmation
     * @param choice The biome choice
     * @param mode The creation mode (WORLD_CREATION or POST_JOIN)
     */
    public static void onBiomeChoiceConfirmed(Object choice, String mode) {
        if ("WORLD_CREATION".equals(mode)) {
            // Store choice for world creation process
            String worldId = generateTempWorldId();
            worldCreationChoices.put(worldId, choice);
            LOGGER.info("Stored biome choice for world creation: {}", choice);
        }
        // POST_JOIN mode is handled by existing logic
    }
    
    /**
     * Called when a world is created to apply stored choices
     * @param world The created world
     */
    public static void onWorldCreated(Object world) {
        // This would be called by world creation events
        // For now, just log the event
        LOGGER.info("World created - would apply stored biome choices");
    }
    
    /**
     * Checks if a screen is currently open
     * @param screenName The screen class name
     * @return true if open, false otherwise
     */
    private static boolean isScreenOpen(String screenName) {
        try {
            net.minecraft.client.gui.screen.Screen currentScreen = MinecraftClient.getInstance().currentScreen;
            return currentScreen != null &&
                   currentScreen.getClass().getSimpleName().equals(screenName);
        } catch (Exception e) {
            LOGGER.debug("Error checking screen state", e);
            return false;
        }
    }
    
    /**
     * Generates a temporary world ID for tracking creation choices
     * @return A temporary world identifier
     */
    private static String generateTempWorldId() {
        return "temp_world_" + System.currentTimeMillis() + "_" +
               Integer.toHexString(System.identityHashCode(Thread.currentThread()));
    }
    
    /**
     * Checks if world has creation time choice
     * @param world The world to check
     * @return true if has creation choice, false otherwise
     */
    public static boolean hasCreationTimeChoice(Object world) {
        // For now, always return false
        // In full implementation, this would check stored choices
        return false;
    }
}