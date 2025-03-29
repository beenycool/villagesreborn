package com.beeny.network;

import com.beeny.village.VillageCraftingManager;
import com.beeny.network.VillageCraftingClientHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side implementation of the VillageCrafting network system
 */
public class VillageCraftingClientNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillageCraftingClientNetwork");

    /**
     * Register client-side packet handlers
     */
    public static void register() {
        LOGGER.info("Registering VillageCraftingClientNetwork handlers");
        registerClientHandlers();
        LOGGER.info("VillageCraftingClientNetwork handlers registered successfully");
    }

    /**
     * Register client-side packet handlers
     */
    private static void registerClientHandlers() {
        // Register client-side packet receivers using the updated API for 1.21.4
        ClientPlayNetworking.registerGlobalReceiver(
            VillageCraftingNetwork.RECIPE_LIST_PACKET_ID,
            (client, handler, payload, responseSender) -> {
                if (payload instanceof VillageCraftingNetwork.RecipeListPayload recipeListPayload) {
                    client.execute(() -> {
                        VillageCraftingClientHandler.updateAvailableRecipes(
                            recipeListPayload.getVillagerUuid(), 
                            recipeListPayload.getRecipeIds()
                        );
                    });
                }
            }
        );
        
        ClientPlayNetworking.registerGlobalReceiver(
            VillageCraftingNetwork.CRAFT_STATUS_PACKET_ID,
            (client, handler, payload, responseSender) -> {
                if (payload instanceof VillageCraftingNetwork.CraftStatusPayload statusPayload) {
                    client.execute(() -> {
                        VillageCraftingClientHandler.updateCraftingStatus(
                            statusPayload.getVillagerUuid(),
                            statusPayload.getRecipeId(),
                            statusPayload.getStatus(),
                            statusPayload.getMessage()
                        );
                    });
                }
            }
        );
        
        ClientPlayNetworking.registerGlobalReceiver(
            VillageCraftingNetwork.CRAFT_PROGRESS_PACKET_ID,
            (client, handler, payload, responseSender) -> {
                if (payload instanceof VillageCraftingNetwork.CraftProgressPayload progressPayload) {
                    client.execute(() -> {
                        VillageCraftingClientHandler.updateCraftingProgress(
                            progressPayload.getVillagerUuid(),
                            progressPayload.getRecipeId(),
                            progressPayload.getProgress(),
                            progressPayload.getMaxProgress()
                        );
                    });
                }
            }
        );
        
        ClientPlayNetworking.registerGlobalReceiver(
            VillageCraftingNetwork.CRAFT_COMPLETE_PACKET_ID,
            (client, handler, payload, responseSender) -> {
                if (payload instanceof VillageCraftingNetwork.CraftCompletePayload completePayload) {
                    client.execute(() -> {
                        VillageCraftingClientHandler.handleCraftingComplete(
                            completePayload.getVillagerUuid(),
                            completePayload.getRecipeId(),
                            completePayload.isSuccess()
                        );
                    });
                }
            }
        );
    }

    /**
     * Send a request to the server to get available recipes for a villager
     */
    public static void sendRecipeListRequest(UUID villagerUuid) {
        LOGGER.debug("Sending recipe list request for villager {}", villagerUuid);
        VillageCraftingNetwork.RequestRecipesPayload payload = 
            new VillageCraftingNetwork.RequestRecipesPayload(villagerUuid);
        ClientPlayNetworking.send(new CustomPayloadC2SPacket(payload));
    }

    /**
     * Send a request to the server to craft a recipe
     */
    public static void sendCraftingRequest(UUID villagerUuid, String recipeId) {
        LOGGER.debug("Sending crafting request for villager {} recipe {}", villagerUuid, recipeId);
        VillageCraftingNetwork.CraftRecipePayload payload = 
            new VillageCraftingNetwork.CraftRecipePayload(villagerUuid, recipeId);
        ClientPlayNetworking.send(new CustomPayloadC2SPacket(payload));
    }

    /**
     * Send a request to the server to cancel crafting
     */
    public static void sendCancelCraftingRequest(UUID villagerUuid, String recipeId) {
        LOGGER.debug("Sending cancel craft request for villager {} recipe {}", villagerUuid, recipeId);
        VillageCraftingNetwork.CancelCraftPayload payload = 
            new VillageCraftingNetwork.CancelCraftPayload(villagerUuid, recipeId);
        ClientPlayNetworking.send(new CustomPayloadC2SPacket(payload));
    }
}