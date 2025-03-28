package com.beeny.network;

import com.beeny.village.VillageCraftingManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Handles client-side networking for the village crafting system
 */
public class VillageCraftingClientNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillageCraftingClientNetwork");
    
    /**
     * Sends a crafting request to the server for the specified villager and recipe
     *
     * @param villagerUuid The UUID of the villager to craft with
     * @param recipeId The ID of the recipe to craft
     */
    public static void sendCraftingRequest(UUID villagerUuid, String recipeId) {
        LOGGER.debug("Sending crafting request for recipe {}", recipeId);
        
        // Create a payload instead of direct buffer for 1.21.4
        VillageCraftingNetwork.CraftRecipePayload payload = 
            new VillageCraftingNetwork.CraftRecipePayload(villagerUuid, recipeId);
        
        // Send the packet to the server using the new API
        ClientPlayNetworking.send(payload);
    }
    
    /**
     * Sends a crafting request to the server for the specified villager and recipe
     *
     * @param villager The villager to craft with
     * @param recipeId The ID of the recipe to craft
     */
    public static void sendCraftingRequest(VillagerEntity villager, String recipeId) {
        sendCraftingRequest(villager.getUuid(), recipeId);
        
        // Log the request to help with debugging
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal("§7Requesting crafting: " + recipeId + "..."), true);
        }
    }
    
    /**
     * Sends a request to the server for the list of recipes available from a villager
     * 
     * @param villagerUuid The UUID of the villager to get recipes from
     */
    public static void sendRecipeListRequest(UUID villagerUuid) {
        LOGGER.debug("Requesting recipe list from villager {}", villagerUuid);
        
        // Create a payload instead of direct buffer for 1.21.4
        VillageCraftingNetwork.RequestRecipesPayload payload = 
            new VillageCraftingNetwork.RequestRecipesPayload(villagerUuid);
        
        // Send the packet to the server using the new API
        ClientPlayNetworking.send(payload);
    }
    
    /**
     * Sends a request to the server for the list of recipes available from a villager
     * 
     * @param villager The villager to get recipes from
     */
    public static void sendRecipeListRequest(VillagerEntity villager) {
        sendRecipeListRequest(villager.getUuid());
    }
    
    /**
     * Sends a request to the server to cancel an in-progress crafting task
     * 
     * @param villagerUuid The UUID of the villager
     * @param recipeId The ID of the recipe to cancel
     */
    public static void sendCancelCraftingRequest(UUID villagerUuid, String recipeId) {
        LOGGER.debug("Sending cancel crafting request for recipe {}", recipeId);
        
        // Create a payload instead of direct buffer for 1.21.4
        VillageCraftingNetwork.CancelCraftPayload payload = 
            new VillageCraftingNetwork.CancelCraftPayload(villagerUuid, recipeId);
        
        // Send the packet to the server using the new API
        ClientPlayNetworking.send(payload);
    }
    
    /**
     * Sends a request to the server to cancel an in-progress crafting task
     * 
     * @param villager The villager
     * @param recipeId The ID of the recipe to cancel
     */
    public static void sendCancelCraftingRequest(VillagerEntity villager, String recipeId) {
        sendCancelCraftingRequest(villager.getUuid(), recipeId);
        
        // Log the request to help with debugging
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal("§7Canceling crafting: " + recipeId + "..."), true);
        }
    }
}