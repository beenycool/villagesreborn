package com.beeny.network;

import com.beeny.village.VillageCraftingManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.PacketByteBuf;

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
        
        ClientPlayNetworking.send(VillageCraftingNetwork.CRAFT_PACKET, buf);
    }
}