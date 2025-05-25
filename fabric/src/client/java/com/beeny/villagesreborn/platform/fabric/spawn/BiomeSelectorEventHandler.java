package com.beeny.villagesreborn.platform.fabric.spawn;

import com.beeny.villagesreborn.platform.fabric.gui.BiomeSelectorScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles events for displaying the biome selector screen on world join
 * Simplified version for testing compatibility
 */
public class BiomeSelectorEventHandler implements ClientPlayConnectionEvents.Join {
    private static final Logger LOGGER = LoggerFactory.getLogger(BiomeSelectorEventHandler.class);
    
    // Static state to track if biome selector has been shown
    private static boolean hasShownBiomeSelector = false;
    
    public BiomeSelectorEventHandler() {}
    
    public static void register() {
        ClientPlayConnectionEvents.JOIN.register(new BiomeSelectorEventHandler());
    }
    
    public static void resetForTest() {
        hasShownBiomeSelector = false;
    }
    
    @Override
    public void onPlayReady(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client) {
        // Check if we should show the biome selector
        if (shouldShowBiomeSelector(client)) {
            showBiomeSelector(client);
            hasShownBiomeSelector = true;
        }
    }
    
    /**
     * Called when the client joins a world (alternative interface for testing)
     */
    public void onClientJoin(ClientPlayNetworkHandler handler, Object packet, MinecraftClient client) {
        onPlayReady(handler, null, client);
    }
    
    private boolean shouldShowBiomeSelector(MinecraftClient client) {
        // Don't show if already shown
        if (hasShownBiomeSelector) {
            return false;
        }
        
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
        
        return isOverworld;
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
    
    public boolean hasShownBiomeSelector() {
        return hasShownBiomeSelector;
    }
}