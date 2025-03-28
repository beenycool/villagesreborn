package com.beeny.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles client-side networking for village info and events
 */
public class VillagesClientNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillagesClientNetwork");
    
    /**
     * Sends a request to the server for village information at the player's position
     */
    public static void requestVillageInfo() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            BlockPos playerPos = client.player.getBlockPos();
            requestVillageInfo(playerPos);
        }
    }
    
    /**
     * Sends a request to the server for village information at a specific position
     * 
     * @param pos The position to get village info for
     */
    public static void requestVillageInfo(BlockPos pos) {
        LOGGER.debug("Requesting village info at position {}", pos);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        
        // Send the packet to the server
        ClientPlayNetworking.send(VillagesNetwork.REQUEST_VILLAGE_INFO_PACKET, buf);
    }
    
    /**
     * Sends a request to the server to join an event
     * 
     * @param eventId The ID of the event to join
     */
    public static void requestJoinEvent(String eventId) {
        LOGGER.debug("Requesting to join event {}", eventId);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(eventId);
        
        // Send the packet to the server
        ClientPlayNetworking.send(VillagesNetwork.JOIN_EVENT_PACKET, buf);
        
        // Log the request to help with debugging
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§7Joining event: " + eventId + "..."), true);
        }
    }
    
    /**
     * Registers client-side packet handlers
     */
    public static void registerHandlers() {
        VillagesNetwork.registerClientHandlers();
        VillageCraftingNetwork.registerClientHandlers();
        LOGGER.info("Client-side village network handlers registered");
    }
}