package com.beeny.network;

import com.beeny.gui.EventNotificationManager;
import com.beeny.gui.VillageInfoHud;
import com.beeny.village.VillageEvent;
import com.beeny.village.VillageInfluenceManager;
import com.beeny.village.VillagerManager;
import com.beeny.village.event.PlayerEventParticipation;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Main network handler for Villages Reborn mod.
 * Handles communication between client and server for village-related data.
 */
public class VillagesNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillagesNetwork");

    // Village Info Packets
    public static final Identifier VILLAGE_INFO_PACKET = new Identifier("villagesreborn", "village_info");
    public static final Identifier REQUEST_VILLAGE_INFO_PACKET = new Identifier("villagesreborn", "request_village_info");
    
    // Event Notification Packets
    public static final Identifier EVENT_NOTIFICATION_PACKET = new Identifier("villagesreborn", "event_notification");
    public static final Identifier EVENT_UPDATE_PACKET = new Identifier("villagesreborn", "event_update");
    public static final Identifier JOIN_EVENT_PACKET = new Identifier("villagesreborn", "join_event");
    
    // AI State Sync Packets
    public static final Identifier VILLAGER_AI_STATE_PACKET = new Identifier("villagesreborn", "villager_ai_state");

    /**
     * Register all network handlers for both client and server
     */
    public static void register() {
        LOGGER.info("Registering VillagesNetwork packet handlers");
        // No implementation yet - will be added in specific methods
    }

    /**
     * Register server-side packet handlers
     */
    public static void registerServerHandlers() {
        // Register handler for village info requests
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_VILLAGE_INFO_PACKET, 
            VillagesNetwork::handleVillageInfoRequest);
        
        // Register handler for event join requests
        ServerPlayNetworking.registerGlobalReceiver(JOIN_EVENT_PACKET,
            VillagesNetwork::handleJoinEventRequest);
            
        LOGGER.info("Server packet handlers registered successfully");
    }

    /**
     * Register client-side packet handlers
     */
    public static void registerClientHandlers() {
        // Register handler for village info updates
        ClientPlayNetworking.registerGlobalReceiver(VILLAGE_INFO_PACKET,
            VillagesNetwork::handleVillageInfoUpdate);
            
        // Register handler for event notifications
        ClientPlayNetworking.registerGlobalReceiver(EVENT_NOTIFICATION_PACKET,
            VillagesNetwork::handleEventNotification);
            
        // Register handler for event updates
        ClientPlayNetworking.registerGlobalReceiver(EVENT_UPDATE_PACKET,
            VillagesNetwork::handleEventUpdate);
            
        // Register handler for villager AI state updates
        ClientPlayNetworking.registerGlobalReceiver(VILLAGER_AI_STATE_PACKET,
            VillagesNetwork::handleVillagerAIState);
            
        LOGGER.info("Client packet handlers registered successfully");
    }

    // Server-side packet handlers

    /**
     * Handle request for village info from client
     */
    private static void handleVillageInfoRequest(MinecraftServer server, ServerPlayerEntity player,
                                              ServerPlayNetworkHandler handler, PacketByteBuf buf,
                                              PacketSender responseSender) {
        // Read player position from the packet
        BlockPos playerPos = buf.readBlockPos();
        
        // Execute on server thread
        server.execute(() -> {
            // Get village stats at the player's position
            VillagerManager vm = VillagerManager.getInstance();
            VillagerManager.VillageStats stats = vm.getVillageStats(playerPos);
            
            if (stats != null) {
                // Send village info back to the client
                sendVillageInfoToClient(player, stats);
            }
        });
    }

    /**
     * Handle request to join an event from client
     */
    private static void handleJoinEventRequest(MinecraftServer server, ServerPlayerEntity player,
                                            ServerPlayNetworkHandler handler, PacketByteBuf buf,
                                            PacketSender responseSender) {
        // Read event ID from the packet
        String eventId = buf.readString();
        
        // Execute on server thread
        server.execute(() -> {
            // Find the event and add the player as a participant
            VillageEvent event = VillageEvent.getEvent(eventId);
            if (event != null) {
                PlayerEventParticipation.getInstance().joinEvent(player, event);
                
                // Send confirmation back to client
                PacketByteBuf responseBuf = PacketByteBufs.create();
                responseBuf.writeString(eventId);
                responseBuf.writeBoolean(true); // Success
                responseBuf.writeString("You've joined the " + event.getName() + " event!");
                
                ServerPlayNetworking.send(player, EVENT_UPDATE_PACKET, responseBuf);
            }
        });
    }

    // Client-side packet handlers

    /**
     * Handle village info update packet from server
     */
    private static void handleVillageInfoUpdate(MinecraftClient client, ClientPlayNetworkHandler handler,
                                             PacketByteBuf buf, PacketSender responseSender) {
        // Read village info from packet
        String cultureName = buf.readString();
        int prosperity = buf.readInt();
        int safety = buf.readInt();
        int population = buf.readInt();
        
        // Update the HUD on the main thread
        client.execute(() -> {
            VillageInfoHud hud = client.getInstance().getVillageInfoHud();
            if (hud != null) {
                hud.update(cultureName, prosperity, safety, population);
            }
        });
    }

    /**
     * Handle event notification packet from server
     */
    private static void handleEventNotification(MinecraftClient client, ClientPlayNetworkHandler handler,
                                             PacketByteBuf buf, PacketSender responseSender) {
        // Read event notification from packet
        String title = buf.readString();
        String description = buf.readString();
        int durationTicks = buf.readInt();
        
        // Show notification on the main thread
        client.execute(() -> {
            EventNotificationManager notificationManager = EventNotificationManager.getInstance();
            notificationManager.showNotification(title, description, durationTicks);
        });
    }

    /**
     * Handle event update packet from server
     */
    private static void handleEventUpdate(MinecraftClient client, ClientPlayNetworkHandler handler,
                                       PacketByteBuf buf, PacketSender responseSender) {
        // Read event update from packet
        String eventId = buf.readString();
        boolean success = buf.readBoolean();
        String message = buf.readString();
        
        // Handle update on the main thread
        client.execute(() -> {
            // Typically used to update the player's HUD or show a message
            if (client.player != null) {
                client.player.sendMessage(net.minecraft.text.Text.literal(message), false);
            }
        });
    }

    /**
     * Handle villager AI state update packet from server
     */
    private static void handleVillagerAIState(MinecraftClient client, ClientPlayNetworkHandler handler,
                                           PacketByteBuf buf, PacketSender responseSender) {
        // Read villager AI state from packet
        UUID villagerUuid = buf.readUuid();
        String activity = buf.readString();
        BlockPos position = buf.readBlockPos();
        
        // Update villager rendering state on the main thread
        client.execute(() -> {
            // Find the villager entity and update its rendering state
            // This might involve setting NBT data or other client-side entity properties
            // which the renderer would use to show special animations or effects
        });
    }

    // Server-to-client packet sending methods

    /**
     * Send village info to a specific client
     */
    public static void sendVillageInfoToClient(ServerPlayerEntity player, VillagerManager.VillageStats stats) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(stats.culture);
        buf.writeInt(stats.prosperity);
        buf.writeInt(stats.safety);
        
        // Get villager count for the village
        int population = VillagerManager.getInstance().getVillagerCount(
            VillagerManager.getInstance().getNearestSpawnRegion(stats.center));
        buf.writeInt(population);
        
        ServerPlayNetworking.send(player, VILLAGE_INFO_PACKET, buf);
    }

    /**
     * Send event notification to a specific client
     */
    public static void sendEventNotificationToClient(ServerPlayerEntity player, String title, 
                                                  String description, int durationTicks) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(title);
        buf.writeString(description);
        buf.writeInt(durationTicks);
        
        ServerPlayNetworking.send(player, EVENT_NOTIFICATION_PACKET, buf);
    }

    /**
     * Send villager AI state update to all nearby clients
     */
    public static void broadcastVillagerAIState(VillagerEntity villager, String activity, BlockPos position) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(villager.getUuid());
        buf.writeString(activity);
        buf.writeBlockPos(position);
        
        // Get all players within render distance of the villager
        for (ServerPlayerEntity player : villager.getServer().getPlayerManager().getPlayerList()) {
            if (player.getWorld() == villager.getWorld() && 
                player.squaredDistanceTo(villager) < 16384) { // 128 blocks squared
                ServerPlayNetworking.send(player, VILLAGER_AI_STATE_PACKET, buf);
            }
        }
    }

    // Client-to-server packet sending methods

    /**
     * Send a request for village info to the server
     */
    public static void requestVillageInfo(BlockPos playerPos) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(playerPos);
        
        ClientPlayNetworking.send(REQUEST_VILLAGE_INFO_PACKET, buf);
    }

    /**
     * Send a request to join an event to the server
     */
    public static void requestJoinEvent(String eventId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(eventId);
        
        ClientPlayNetworking.send(JOIN_EVENT_PACKET, buf);
    }
}