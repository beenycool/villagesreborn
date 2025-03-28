package com.beeny.network;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles client-side logic for village crafting interactions.
 * This includes updating UI elements when receiving packets from the server.
 */
public class VillageCraftingClientHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillageCraftingClientHandler");
    private static final Map<UUID, List<String>> availableRecipes = new HashMap<>();
    private static final Map<UUID, Map<String, CraftingStatus>> craftingStatus = new HashMap<>();

    /**
     * Updates the available recipes for a villager
     */
    public static void updateAvailableRecipes(UUID villagerUuid, List<String> recipeIds) {
        LOGGER.debug("Updating available recipes for villager {}: {}", villagerUuid, recipeIds);
        availableRecipes.put(villagerUuid, recipeIds);
        
        // Find the villager entity on the client side
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            Box searchBox = Box.of(client.player.getPos(), 10, 10, 10);
            List<VillagerEntity> nearbyVillagers = client.world.getEntitiesByClass(
                VillagerEntity.class,
                searchBox,
                v -> v.getUuid().equals(villagerUuid)
            );
            
            if (!nearbyVillagers.isEmpty()) {
                VillagerEntity villager = nearbyVillagers.get(0);
                // Trigger UI update - this would integrate with your UI system
                // For example, updating a crafting menu if it's open
                if (client.currentScreen instanceof VillageCraftingScreen) {
                    ((VillageCraftingScreen) client.currentScreen).updateRecipeList(recipeIds);
                }
            }
        }
    }

    /**
     * Updates the crafting status for a specific recipe
     */
    public static void updateCraftingStatus(UUID villagerUuid, String recipeId, String status, String message) {
        LOGGER.debug("Updating crafting status for villager {} recipe {}: {} - {}", 
            villagerUuid, recipeId, status, message);
            
        Map<String, CraftingStatus> villagerStatus = craftingStatus.computeIfAbsent(
            villagerUuid, k -> new HashMap<>());
            
        CraftingStatus currentStatus = villagerStatus.computeIfAbsent(
            recipeId, k -> new CraftingStatus());
            
        currentStatus.status = status;
        currentStatus.message = message;
        
        // If a crafting UI is open, update it
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof VillageCraftingScreen) {
            ((VillageCraftingScreen) client.currentScreen).updateCraftingStatus(recipeId, status, message);
        } else if (client.player != null) {
            // Otherwise, show a message to the player
            client.player.sendMessage(Text.literal(message), true);
        }
    }

    /**
     * Updates the crafting progress for a specific recipe
     */
    public static void updateCraftingProgress(UUID villagerUuid, String recipeId, int progress, int maxProgress) {
        LOGGER.debug("Updating crafting progress for villager {} recipe {}: {}/{}", 
            villagerUuid, recipeId, progress, maxProgress);
            
        Map<String, CraftingStatus> villagerStatus = craftingStatus.computeIfAbsent(
            villagerUuid, k -> new HashMap<>());
            
        CraftingStatus currentStatus = villagerStatus.computeIfAbsent(
            recipeId, k -> new CraftingStatus());
            
        currentStatus.progress = progress;
        currentStatus.maxProgress = maxProgress;
        
        // Update UI if open
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof VillageCraftingScreen) {
            ((VillageCraftingScreen) client.currentScreen).updateCraftingProgress(recipeId, progress, maxProgress);
        }
    }

    /**
     * Handles crafting completion notification
     */
    public static void handleCraftingComplete(UUID villagerUuid, String recipeId, boolean success) {
        LOGGER.debug("Crafting complete for villager {} recipe {}: success={}", 
            villagerUuid, recipeId, success);
            
        // Remove from active crafting statuses
        Map<String, CraftingStatus> villagerStatus = craftingStatus.get(villagerUuid);
        if (villagerStatus != null) {
            villagerStatus.remove(recipeId);
            if (villagerStatus.isEmpty()) {
                craftingStatus.remove(villagerUuid);
            }
        }
        
        // Update UI or show notification
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof VillageCraftingScreen) {
            ((VillageCraftingScreen) client.currentScreen).handleCraftingComplete(recipeId, success);
        } else if (client.player != null) {
            String message = success 
                ? "The villager successfully crafted your item!"
                : "The villager failed to craft your item.";
            client.player.sendMessage(Text.literal(message), false);
        }
    }

    /**
     * Get available recipes for a villager
     */
    public static List<String> getAvailableRecipes(UUID villagerUuid) {
        return availableRecipes.getOrDefault(villagerUuid, List.of());
    }

    /**
     * Get crafting status for a villager's recipe
     */
    public static CraftingStatus getCraftingStatus(UUID villagerUuid, String recipeId) {
        Map<String, CraftingStatus> villagerStatus = craftingStatus.get(villagerUuid);
        if (villagerStatus != null) {
            return villagerStatus.getOrDefault(recipeId, new CraftingStatus());
        }
        return new CraftingStatus();
    }

    /**
     * Send a request to the server to get available recipes for a villager
     */
    public static void requestRecipes(UUID villagerUuid) {
        VillageCraftingClientNetwork.sendRecipeListRequest(villagerUuid);
    }

    /**
     * Send a request to the server to craft a recipe
     */
    public static void requestCrafting(UUID villagerUuid, String recipeId) {
        VillageCraftingClientNetwork.sendCraftingRequest(villagerUuid, recipeId);
    }

    /**
     * Send a request to the server to cancel crafting
     */
    public static void cancelCrafting(UUID villagerUuid, String recipeId) {
        VillageCraftingClientNetwork.sendCancelCraftingRequest(villagerUuid, recipeId);
    }

    /**
     * Represents the current status of a crafting task
     */
    public static class CraftingStatus {
        public String status = "none";
        public String message = "";
        public int progress = 0;
        public int maxProgress = 100;
    }
}