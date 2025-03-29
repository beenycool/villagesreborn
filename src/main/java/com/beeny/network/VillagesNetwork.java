package com.beeny.network;

import com.beeny.gui.EventNotificationManager;
import com.beeny.gui.VillageInfoHud;
import com.beeny.village.VillageInfluenceManager;
import com.beeny.village.VillagerManager;
import com.beeny.village.event.PlayerEventParticipation;
import com.beeny.village.event.VillageEvent;  // Updated import
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
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
 * Updated for Minecraft 1.21.4 networking API
 */
public class VillagesNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillagesNetwork");

    // Village Info Packets - using Identifier.of() instead of new Identifier()
    public static final Identifier VILLAGE_INFO_ID = Identifier.of("villagesreborn", "village_info");
    public static final Identifier REQUEST_VILLAGE_INFO_ID = Identifier.of("villagesreborn", "request_village_info");
    
    // Event Notification Packets
    public static final Identifier EVENT_NOTIFICATION_ID = Identifier.of("villagesreborn", "event_notification");
    public static final Identifier EVENT_UPDATE_ID = Identifier.of("villagesreborn", "event_update");
    public static final Identifier JOIN_EVENT_ID = Identifier.of("villagesreborn", "join_event");
    
    // AI State Sync Packets
    public static final Identifier VILLAGER_AI_STATE_ID = Identifier.of("villagesreborn", "villager_ai_state");

    // Custom payload ID objects for 1.21.4
    public static final CustomPayload.Id<VillageInfoPayload> VILLAGE_INFO_PAYLOAD_ID = 
        CustomPayload.id("villagesreborn:village_info");
    
    public static final CustomPayload.Id<RequestVillageInfoPayload> REQUEST_VILLAGE_INFO_PAYLOAD_ID = 
        CustomPayload.id("villagesreborn:request_village_info");
    
    public static final CustomPayload.Id<EventNotificationPayload> EVENT_NOTIFICATION_PAYLOAD_ID = 
        CustomPayload.id("villagesreborn:event_notification");
    
    public static final CustomPayload.Id<EventUpdatePayload> EVENT_UPDATE_PAYLOAD_ID = 
        CustomPayload.id("villagesreborn:event_update");
    
    public static final CustomPayload.Id<JoinEventPayload> JOIN_EVENT_PAYLOAD_ID = 
        CustomPayload.id("villagesreborn:join_event");
    
    public static final CustomPayload.Id<VillagerAIStatePayload> VILLAGER_AI_STATE_PAYLOAD_ID = 
        CustomPayload.id("villagesreborn:villager_ai_state");

    // Custom payload classes for 1.21.4
    public static class VillageInfoPayload implements CustomPayload {
        private final String culture;
        private final int prosperity;
        private final int safety;
        private final int population;
        
        public VillageInfoPayload(String culture, int prosperity, int safety, int population) {
            this.culture = culture;
            this.prosperity = prosperity;
            this.safety = safety;
            this.population = population;
        }
        
        public VillageInfoPayload(PacketByteBuf buf) {
            this.culture = buf.readString();
            this.prosperity = buf.readInt();
            this.safety = buf.readInt();
            this.population = buf.readInt();
        }
        
        @Override
        public void write(PacketByteBuf buf) {
            buf.writeString(culture);
            buf.writeInt(prosperity);
            buf.writeInt(safety);
            buf.writeInt(population);
        }
        
        public String getCulture() {
            return culture;
        }
        
        public int getProsperity() {
            return prosperity;
        }
        
        public int getSafety() {
            return safety;
        }
        
        public int getPopulation() {
            return population;
        }
        
        @Override
        public CustomPayload.Id<?> getId() {
            return VILLAGE_INFO_PAYLOAD_ID;
        }
    }
    
    public static class RequestVillageInfoPayload implements CustomPayload {
        private final BlockPos playerPos;
        
        public RequestVillageInfoPayload(BlockPos playerPos) {
            this.playerPos = playerPos;
        }
        
        public RequestVillageInfoPayload(PacketByteBuf buf) {
            this.playerPos = buf.readBlockPos();
        }
        
        @Override
        public void write(PacketByteBuf buf) {
            buf.writeBlockPos(playerPos);
        }
        
        public BlockPos getPlayerPos() {
            return playerPos;
        }
        
        @Override
        public CustomPayload.Id<?> getId() {
            return REQUEST_VILLAGE_INFO_PAYLOAD_ID;
        }
    }
    
    public static class EventNotificationPayload implements CustomPayload {
        private final String title;
        private final String description;
        private final int durationTicks;
        
        public EventNotificationPayload(String title, String description, int durationTicks) {
            this.title = title;
            this.description = description;
            this.durationTicks = durationTicks;
        }
        
        public EventNotificationPayload(PacketByteBuf buf) {
            this.title = buf.readString();
            this.description = buf.readString();
            this.durationTicks = buf.readInt();
        }
        
        @Override
        public void write(PacketByteBuf buf) {
            buf.writeString(title);
            buf.writeString(description);
            buf.writeInt(durationTicks);
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
        
        public int getDurationTicks() {
            return durationTicks;
        }
        
        @Override
        public CustomPayload.Id<?> getId() {
            return EVENT_NOTIFICATION_PAYLOAD_ID;
        }
    }
    
    public static class EventUpdatePayload implements CustomPayload {
        private final String eventId;
        private final boolean success;
        private final String message;
        
        public EventUpdatePayload(String eventId, boolean success, String message) {
            this.eventId = eventId;
            this.success = success;
            this.message = message;
        }
        
        public EventUpdatePayload(PacketByteBuf buf) {
            this.eventId = buf.readString();
            this.success = buf.readBoolean();
            this.message = buf.readString();
        }
        
        @Override
        public void write(PacketByteBuf buf) {
            buf.writeString(eventId);
            buf.writeBoolean(success);
            buf.writeString(message);
        }
        
        public String getEventId() {
            return eventId;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public CustomPayload.Id<?> getId() {
            return EVENT_UPDATE_PAYLOAD_ID;
        }
    }
    
    public static class JoinEventPayload implements CustomPayload {
        private final String eventId;
        
        public JoinEventPayload(String eventId) {
            this.eventId = eventId;
        }
        
        public JoinEventPayload(PacketByteBuf buf) {
            this.eventId = buf.readString();
        }
        
        @Override
        public void write(PacketByteBuf buf) {
            buf.writeString(eventId);
        }
        
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public CustomPayload.Id<?> getId() {
            return JOIN_EVENT_PAYLOAD_ID;
        }
    }
    
    public static class VillagerAIStatePayload implements CustomPayload {
        private final UUID villagerUuid;
        private final String activity;
        private final BlockPos position;
        
        public VillagerAIStatePayload(UUID villagerUuid, String activity, BlockPos position) {
            this.villagerUuid = villagerUuid;
            this.activity = activity;
            this.position = position;
        }
        
        public VillagerAIStatePayload(PacketByteBuf buf) {
            this.villagerUuid = buf.readUuid();
            this.activity = buf.readString();
            this.position = buf.readBlockPos();
        }
        
        @Override
        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeString(activity);
            buf.writeBlockPos(position);
        }
        
        public UUID getVillagerUuid() {
            return villagerUuid;
        }
        
        public String getActivity() {
            return activity;
        }
        
        public BlockPos getPosition() {
            return position;
        }
        
        @Override
        public CustomPayload.Id<?> getId() {
            return VILLAGER_AI_STATE_PAYLOAD_ID;
        }
    }

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
        // Register handlers for the custom payload packets
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_VILLAGE_INFO_PAYLOAD_ID,
            (payload, context) -> {
                ServerPlayerEntity player = context.player();
                MinecraftServer server = player.getServer();
                server.execute(() -> {
                    handleVillageInfoRequest(server, player, player.networkHandler, payload, context.responseSender());
                });
            });
        
        ServerPlayNetworking.registerGlobalReceiver(JOIN_EVENT_PAYLOAD_ID,
            (payload, context) -> {
                ServerPlayerEntity player = context.player();
                MinecraftServer server = player.getServer();
                server.execute(() -> {
                    handleJoinEventRequest(server, player, player.networkHandler, payload, context.responseSender());
                });
            });
            
        LOGGER.info("Server packet handlers registered successfully");
    }

    /**
     * Register client-side packet handlers
     */
    public static void registerClientHandlers() {
        // Register handlers for the custom payload packets
        ClientPlayNetworking.registerGlobalReceiver(VILLAGE_INFO_PAYLOAD_ID,
            (payload, context) -> {
                MinecraftClient client = context.client();
                client.execute(() -> {
                    handleVillageInfoUpdate(client, context.networkHandler(), payload, context.responseSender());
                });
            });
        
        ClientPlayNetworking.registerGlobalReceiver(EVENT_NOTIFICATION_PAYLOAD_ID,
            (payload, context) -> {
                MinecraftClient client = context.client();
                client.execute(() -> {
                    handleEventNotification(client, context.networkHandler(), payload, context.responseSender());
                });
            });
        
        ClientPlayNetworking.registerGlobalReceiver(EVENT_UPDATE_PAYLOAD_ID,
            (payload, context) -> {
                MinecraftClient client = context.client();
                client.execute(() -> {
                    handleEventUpdate(client, context.networkHandler(), payload, context.responseSender());
                });
            });
        
        ClientPlayNetworking.registerGlobalReceiver(VILLAGER_AI_STATE_PAYLOAD_ID,
            (payload, context) -> {
                MinecraftClient client = context.client();
                client.execute(() -> {
                    handleVillagerAIState(client, context.networkHandler(), payload, context.responseSender());
                });
            });
            
        LOGGER.info("Client packet handlers registered successfully");
    }

    // Server-side packet handlers

    /**
     * Handle request for village info from client
     */
    private static void handleVillageInfoRequest(MinecraftServer server, ServerPlayerEntity player, 
                                           ServerPlayNetworkHandler handler, RequestVillageInfoPayload payload, 
                                           PacketSender responseSender) {
        // Get player position from the payload
        BlockPos playerPos = payload.getPlayerPos();
        
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
                                         ServerPlayNetworkHandler handler, JoinEventPayload payload,
                                         PacketSender responseSender) {
        String eventId = payload.getEventId();
        
        server.execute(() -> {
            // Updated to use correct VillageEvent class from event package
            VillageEvent event = VillageEvent.findById(eventId);  // Assuming method name is findById
            if (event != null) {
                PlayerEventParticipation.getInstance().joinEvent(player, event);
                
                EventUpdatePayload updatePayload = new EventUpdatePayload(
                    eventId, true, "You've joined the " + event.getName() + " event!");
                
                ServerPlayNetworking.send(player, updatePayload);
            }
        });
    }

    // Client-side packet handlers

    /**
     * Handle village info update packet from server
     */
    private static void handleVillageInfoUpdate(MinecraftClient client, ClientPlayNetworkHandler handler, 
                                          VillageInfoPayload payload, PacketSender responseSender) {
        // Get village info from payload
        String cultureName = payload.getCulture();
        int prosperity = payload.getProsperity();
        int safety = payload.getSafety();
        int population = payload.getPopulation();
        
        // Update the HUD on the main thread
        client.execute(() -> {
            VillageInfoHud hud = VillageInfoHud.getInstance();
            if (hud != null) {
                hud.setCultureInfo(cultureName, prosperity, safety, population);
            }
        });
    }

    /**
     * Handle event notification packet from server
     */
    private static void handleEventNotification(MinecraftClient client, ClientPlayNetworkHandler handler,
                                          EventNotificationPayload payload, PacketSender responseSender) {
        // Read event notification from payload
        String title = payload.getTitle();
        String description = payload.getDescription();
        int durationTicks = payload.getDurationTicks();
        
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
                                    EventUpdatePayload payload, PacketSender responseSender) {
        // Read event update from payload
        String eventId = payload.getEventId();
        boolean success = payload.isSuccess();
        String message = payload.getMessage();
        
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
                                        VillagerAIStatePayload payload, PacketSender responseSender) {
        // Read villager AI state from payload
        UUID villagerUuid = payload.getVillagerUuid();
        String activity = payload.getActivity();
        BlockPos position = payload.getPosition();
        
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
        // Get villager count for the village
        int population = 0;
        try {
            VillagerManager vm = VillagerManager.getInstance();
            if (vm != null && stats != null && stats.center != null) {
                population = vm.getVillagersInRegion(
                    vm.getNearestSpawnRegion(stats.center)).size();
            }
        } catch (Exception e) {
            LOGGER.error("Error getting villager count", e);
        }
        
        VillageInfoPayload payload = new VillageInfoPayload(
            stats.culture, stats.prosperity, stats.safety, population);
        
        ServerPlayNetworking.send(player, payload);
    }

    /**
     * Send event notification to a specific client
     */
    public static void sendEventNotificationToClient(ServerPlayerEntity player, String title, 
                                                  String description, int durationTicks) {
        EventNotificationPayload payload = new EventNotificationPayload(title, description, durationTicks);
        ServerPlayNetworking.send(player, payload);
    }

    /**
     * Send villager AI state update to all nearby clients
     */
    public static void broadcastVillagerAIState(VillagerEntity villager, String activity, BlockPos position) {
        VillagerAIStatePayload payload = new VillagerAIStatePayload(
            villager.getUuid(), activity, position);
        
        // Get all players within render distance of the villager
        for (ServerPlayerEntity player : villager.getServer().getPlayerManager().getPlayerList()) {
            if (player.getWorld() == villager.getWorld() && 
                player.squaredDistanceTo(villager) < 16384) { // 128 blocks squared
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    // Client-to-server packet sending methods

    /**
     * Send a request for village info to the server
     */
    public static void requestVillageInfo(BlockPos playerPos) {
        RequestVillageInfoPayload payload = new RequestVillageInfoPayload(playerPos);
        ClientPlayNetworking.send(payload);
    }

    /**
     * Send a request to join an event to the server
     */
    public static void requestJoinEvent(String eventId) {
        JoinEventPayload payload = new JoinEventPayload(eventId);
        ClientPlayNetworking.send(payload);
    }
}