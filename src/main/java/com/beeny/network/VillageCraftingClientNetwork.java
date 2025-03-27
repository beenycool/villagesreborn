package com.beeny.network;

import com.beeny.village.VillageCraftingManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.client.MinecraftClient;

/**
 * Handles client-side networking for the village crafting system
 */
public class VillageCraftingClientNetwork {
    
    /**
     * Sends a crafting request to the server for the specified villager and recipe
     *
     * @param villager The villager to craft with
     * @param recipeId The ID of the recipe to craft
     */
    public static void sendCraftingRequest(VillagerEntity villager, String recipeId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(villager.getUuid());
        buf.writeString(recipeId);
        
        // Log the request to help with debugging
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal("§7Requesting crafting: " + recipeId + "..."), true);
        }
        
        // Send the packet to the server
        ClientPlayNetworking.send(VillageCraftingNetwork.CRAFT_PACKET, buf);
    }
}