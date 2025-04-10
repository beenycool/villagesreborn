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
import net.minecraft.network.codec.PacketCodec;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main network handler for Villages Reborn mod.
 * Handles communication between client and server for village-related data.
 * Updated for Minecraft 1.21.4 networking API
 */
public class VillagesNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillagesNetwork");
    private static final String MOD_ID = "villagesreborn";

    // Village Info Packets - using Identifier.of() instead of new Identifier()
    public static final Identifier VILLAGE_INFO_ID = Identifier.of(MOD_ID, "village_info");
    public static final Identifier REQUEST_VILLAGE_INFO_ID = Identifier.of(MOD_ID, "request_village_info");
    
    // Event Notification Packets
    public static final Identifier EVENT_NOTIFICATION_ID = Identifier.of(MOD_ID, "event_notification");
    public static final Identifier EVENT_UPDATE_ID = Identifier.of(MOD_ID, "event_update");
    public static final Identifier JOIN_EVENT_ID = Identifier.of(MOD_ID, "join_event");
    
    // AI State Sync Packets
    public static final Identifier VILLAGER_AI_STATE_ID = Identifier.of(MOD_ID, "villager_ai_state");

    // Custom payload ID objects for 1.21.4
    public static final CustomPayload.Id<VillageInfoPayload> VILLAGE_INFO_ID_PAYLOAD = new CustomPayload.Id<>(VILLAGE_INFO_ID);
    public static final PacketCodec<PacketByteBuf, VillageInfoPayload> VILLAGE_INFO_CODEC =
        PacketCodec.of(VillageInfoPayload::write, VillageInfoPayload::new);

    public static final CustomPayload.Id<RequestVillageInfoPayload> REQUEST_VILLAGE_INFO_ID_PAYLOAD = new CustomPayload.Id<>(REQUEST_VILLAGE_INFO_ID);
    public static final PacketCodec<PacketByteBuf, RequestVillageInfoPayload> REQUEST_VILLAGE_INFO_CODEC =
        PacketCodec.of(RequestVillageInfoPayload::write, RequestVillageInfoPayload::new);
    
    public static final CustomPayload.Id<EventNotificationPayload> EVENT_NOTIFICATION_ID_PAYLOAD = new CustomPayload.Id<>(EVENT_NOTIFICATION_ID);
    public static final PacketCodec<PacketByteBuf, EventNotificationPayload> EVENT_NOTIFICATION_CODEC =
        PacketCodec.of(EventNotificationPayload::write, EventNotificationPayload::new);
    
    public static final CustomPayload.Id<EventUpdatePayload> EVENT_UPDATE_ID_PAYLOAD = new CustomPayload.Id<>(EVENT_UPDATE_ID);
    public static final PacketCodec<PacketByteBuf, EventUpdatePayload> EVENT_UPDATE_CODEC =
        PacketCodec.of(EventUpdatePayload::write, EventUpdatePayload::new);
    
    public static final CustomPayload.Id<JoinEventPayload> JOIN_EVENT_ID_PAYLOAD = new CustomPayload.Id<>(JOIN_EVENT_ID);
    public static final PacketCodec<PacketByteBuf, JoinEventPayload> JOIN_EVENT_CODEC =
        PacketCodec.of(JoinEventPayload::write, JoinEventPayload::new);
    
    public static final CustomPayload.Id<VillagerAIStatePayload> VILLAGER_AI_STATE_ID_PAYLOAD = new CustomPayload.Id<>(VILLAGER_AI_STATE_ID);
    public static final PacketCodec<PacketByteBuf, VillagerAIStatePayload> VILLAGER_AI_STATE_CODEC =
        PacketCodec.of(VillagerAIStatePayload::write, VillagerAIStatePayload::new);

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
            return VILLAGE_INFO_ID_PAYLOAD;
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
        
        public void write(PacketByteBuf buf) {
            buf.writeBlockPos(playerPos);
        }
        
        public BlockPos getPlayerPos() {
            return playerPos;
        }
        
        @Override
        public CustomPayload.Id<?> getId() {
            return REQUEST_VILLAGE_INFO_ID_PAYLOAD;
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
            return EVENT_NOTIFICATION_ID_PAYLOAD;
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
            return EVENT_UPDATE_ID_PAYLOAD;
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
        
        public void write(PacketByteBuf buf) {
            buf.writeString(eventId);
        }
        
        public String getEventId() {
            return eventId;
        }
        
        @Override
        public CustomPayload.Id<?> getId() {
            return JOIN_EVENT_ID_PAYLOAD;
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
            return VILLAGER_AI_STATE_ID_PAYLOAD;
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
        // Register handlers for C2S packets
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_VILLAGE_INFO_ID_PAYLOAD,
            (payload, context) -> {
                ServerPlayerEntity player = context.player();
                // Context handles execution on server thread
                handleVillageInfoRequest(player.getServer(), player, payload);
            });
        
        ServerPlayNetworking.registerGlobalReceiver(JOIN_EVENT_ID_PAYLOAD,
            (payload, context) -> {
                ServerPlayerEntity player = context.player();
                handleJoinEventRequest(player.getServer(), player, payload);
            });
            
        LOGGER.info("Server packet handlers registered successfully");
    }

    /**
     * Register client-side packet handlers
     */
    public static void registerClientHandlers() {
        // Register handlers for S2C packets
        ClientPlayNetworking.registerGlobalReceiver(VILLAGE_INFO_ID_PAYLOAD,
            (payload, context) -> {
                // Context handles execution on client thread
                handleVillageInfoUpdate(context.client(), payload);
            });

        ClientPlayNetworking.registerGlobalReceiver(EVENT_NOTIFICATION_ID_PAYLOAD,
            (payload, context) -> {
                handleEventNotification(context.client(), payload);
            });

        ClientPlayNetworking.registerGlobalReceiver(EVENT_UPDATE_ID_PAYLOAD,
            (payload, context) -> {
                handleEventUpdate(context.client(), payload);
            });

        ClientPlayNetworking.registerGlobalReceiver(VILLAGER_AI_STATE_ID_PAYLOAD,
            (payload, context) -> {
                handleVillagerAIState(context.client(), payload);
            });
            
        LOGGER.info("Client packet handlers registered successfully");
    }

    // Server-side packet handlers

    /**
     * Handle request for village info from client
     */
    private static void handleVillageInfoRequest(MinecraftServer server, ServerPlayerEntity player,
                                           RequestVillageInfoPayload payload) {
        BlockPos playerPos = payload.getPlayerPos();
        // Get village stats at the player's position
        VillagerManager vm = VillagerManager.getInstance();
        VillagerManager.VillageStats stats = vm.getVillageStats(playerPos);
        
        if (stats != null) {
            // Send village info back to the client
            sendVillageInfoToClient(player, stats);
        }
    }

    /**
     * Handle request to join an event from client
     */
    private static void handleJoinEventRequest(MinecraftServer server, ServerPlayerEntity player,
                                         JoinEventPayload payload) {
        String eventId = payload.getEventId();
        VillageEvent event = VillageEvent.getEvent(eventId); // Updated method
        if (event != null) {
            PlayerEventParticipation.getInstance().joinEvent(player, event);
            
            EventUpdatePayload updatePayload = new EventUpdatePayload(
                eventId, true, "You've joined the " + event.getType() + " event!");
            
            // Send S2C payload directly
            ServerPlayNetworking.send(player, updatePayload);
        }
    }

    // Client-side packet handlers

    /**
     * Handle village info update packet from server
     */
    private static void handleVillageInfoUpdate(MinecraftClient client, VillageInfoPayload payload) {
        // Get village info from payload
        String cultureName = payload.getCulture();
        int prosperity = payload.getProsperity();
        int safety = payload.getSafety();
        int population = payload.getPopulation();
        
        // Update the HUD directly (context handles thread)
        VillageInfoHud hud = VillageInfoHud.getInstance();
        if (hud != null) {
            hud.update(cultureName, prosperity, safety, population); // Updated method
        }
    }

    /**
     * Handle event notification packet from server
     */
    private static void handleEventNotification(MinecraftClient client, EventNotificationPayload payload) {
        String title = payload.getTitle();
        String description = payload.getDescription();
        int durationTicks = payload.getDurationTicks();
        
        // Show notification directly (context handles thread)
        EventNotificationManager notificationManager = EventNotificationManager.getInstance();
        notificationManager.showNotification(title, description, durationTicks);
    }

    /**
     * Handle event update packet from server
     */
    private static void handleEventUpdate(MinecraftClient client, EventUpdatePayload payload) {
        String eventId = payload.getEventId();
        boolean success = payload.isSuccess();
        String message = payload.getMessage();
        
        // Handle update directly (context handles thread)
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), false);
        }
    }

    /**
     * Handle villager AI state update packet from server
     */
    private static void handleVillagerAIState(MinecraftClient client, VillagerAIStatePayload payload) {
        UUID villagerUuid = payload.getVillagerUuid();
        String activity = payload.getActivity();
        BlockPos position = payload.getPosition();
        
        // Update villager rendering state directly (context handles thread)
        // Find the villager entity and update its rendering state
        // This might involve setting NBT data or other client-side entity properties
        // which the renderer would use to show special animations or effects
        // Example: (Requires access to the entity)
        // Entity entity = client.world.getEntityById(villagerUuid); // Need a way to get entity by UUID on client
        // if (entity instanceof VillagerEntity villager) {
        //     // Apply state update logic here
        // }
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


    // Packet type registration should happen centrally in Villagesreborn.onInitialize
}