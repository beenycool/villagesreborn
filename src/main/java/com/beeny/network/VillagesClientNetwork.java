package com.beeny.network;

import com.beeny.gui.ConversationHud;
import com.beeny.gui.VillageInfoHud;
import com.beeny.village.VillagerManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.network.codec.PacketCodec;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Handles client-side networking for Villages Reborn
 * Updated for Minecraft 1.21.4 networking API
 */
public class VillagesClientNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillagesClientNetwork");
    private static final String MOD_ID = "villagesreborn";

    // Channel identifiers for different packet types - using Identifier.of() for 1.21.4
    public static final Identifier VILLAGER_CULTURE_ID = Identifier.of(MOD_ID, "villager_culture");
    public static final Identifier VILLAGER_MOOD_ID = Identifier.of(MOD_ID, "villager_mood");
    public static final Identifier VILLAGE_INFO_ID = Identifier.of(MOD_ID, "village_info");
    public static final Identifier REQUEST_VILLAGE_INFO_ID = Identifier.of(MOD_ID, "request_village_info");
    public static final Identifier JOIN_EVENT_ID = Identifier.of(MOD_ID, "join_event");

    // Custom payload IDs for 1.21.4
    public static final CustomPayload.Id<VillagerCulturePayload> VILLAGER_CULTURE_ID_PAYLOAD = new CustomPayload.Id<>(VILLAGER_CULTURE_ID);
    public static final PacketCodec<PacketByteBuf, VillagerCulturePayload> VILLAGER_CULTURE_CODEC =
        PacketCodec.of(VillagerCulturePayload::write, VillagerCulturePayload::new);

    public static final CustomPayload.Id<VillagerMoodPayload> VILLAGER_MOOD_ID_PAYLOAD = new CustomPayload.Id<>(VILLAGER_MOOD_ID);
    public static final PacketCodec<PacketByteBuf, VillagerMoodPayload> VILLAGER_MOOD_CODEC =
        PacketCodec.of(VillagerMoodPayload::write, VillagerMoodPayload::new);

    // C2S Payloads
    public static final CustomPayload.Id<RequestVillageInfoPayload> REQUEST_VILLAGE_INFO_ID_PAYLOAD = new CustomPayload.Id<>(REQUEST_VILLAGE_INFO_ID);
    public static final PacketCodec<PacketByteBuf, RequestVillageInfoPayload> REQUEST_VILLAGE_INFO_CODEC =
        PacketCodec.of(RequestVillageInfoPayload::write, RequestVillageInfoPayload::new);
    
    public static final CustomPayload.Id<JoinEventPayload> JOIN_EVENT_ID_PAYLOAD = new CustomPayload.Id<>(JOIN_EVENT_ID);
    public static final PacketCodec<PacketByteBuf, JoinEventPayload> JOIN_EVENT_CODEC =
        PacketCodec.of(JoinEventPayload::write, JoinEventPayload::new);

    // Custom payload classes for 1.21.4
    public static class VillagerCulturePayload implements CustomPayload {
        private final UUID villagerUuid;
        private final String culture;
        
        public VillagerCulturePayload(UUID villagerUuid, String culture) {
            this.villagerUuid = villagerUuid;
            this.culture = culture;
        }
        
        public VillagerCulturePayload(PacketByteBuf buf) {
            this.villagerUuid = buf.readUuid();
            this.culture = buf.readString();
        }
        
        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeString(culture);
        }
        
        @Override
        public CustomPayload.Id<?> getId() {
            return VILLAGER_CULTURE_ID_PAYLOAD;
        }
        
        public UUID getVillagerUuid() {
            return villagerUuid;
        }
        
        public String getCulture() {
            return culture;
        }

        // Remove conflicting getId method
    }
    
    public static class VillagerMoodPayload implements CustomPayload {
        private final UUID villagerUuid;
        private final String mood;
        
        public VillagerMoodPayload(UUID villagerUuid, String mood) {
            this.villagerUuid = villagerUuid;
            this.mood = mood;
        }
        
        public VillagerMoodPayload(PacketByteBuf buf) {
            this.villagerUuid = buf.readUuid();
            this.mood = buf.readString();
        }
        
        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeString(mood);
        }
        
        @Override
        public CustomPayload.Id<?> getId() {
            return VILLAGER_MOOD_ID_PAYLOAD;
        }
        
        public UUID getVillagerUuid() {
            return villagerUuid;
        }
        
        public String getMood() {
            return mood;
        }

        // Remove conflicting getId method
    }
    
    public static class RequestVillageInfoPayload implements CustomPayload {
        private final BlockPos position;
        
        public RequestVillageInfoPayload(BlockPos position) {
            this.position = position;
        }
        
        public RequestVillageInfoPayload(PacketByteBuf buf) {
            this.position = buf.readBlockPos();
        }
        
        public void write(PacketByteBuf buf) {
            buf.writeBlockPos(position);
        }
        
        @Override
        public CustomPayload.Id<?> getId() {
            return REQUEST_VILLAGE_INFO_ID_PAYLOAD;
        }
        
        public BlockPos getPosition() {
            return position;
        }

        // Remove conflicting getId method
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
        
        @Override
        public CustomPayload.Id<?> getId() {
            return JOIN_EVENT_ID_PAYLOAD;
        }
        
        public String getEventId() {
            return eventId;
        }

        // Remove conflicting getId method
    }

    /**
     * Register all client-side packet receivers
     */
    public static void registerReceivers() {
        // Updated registration to use ID and correct context type
        ClientPlayNetworking.registerGlobalReceiver(VILLAGER_CULTURE_ID_PAYLOAD,
            (payload, context) -> {
                // No need for separate client.execute, context handles it
                handleVillagerCulturePacket(payload.getVillagerUuid(), payload.getCulture());
            });

        // Updated registration to use ID and correct context type
        ClientPlayNetworking.registerGlobalReceiver(VILLAGER_MOOD_ID_PAYLOAD,
            (payload, context) -> {
                handleVillagerMoodPacket(payload.getVillagerUuid(), payload.getMood());
            });

        // Assuming VillagesNetwork.VILLAGE_INFO_ID_PAYLOAD exists and is S2C
        // Need to ensure VillagesNetwork.VillageInfoPayload class is accessible
        ClientPlayNetworking.registerGlobalReceiver(VillagesNetwork.VILLAGE_INFO_ID_PAYLOAD,
            (payload, context) -> {
                handleVillageInfoPacket(
                    payload.getCulture(),
                    payload.getProsperity(),
                    payload.getSafety(),
                    payload.getPopulation()
                );
            });
        
        LOGGER.info("Client-side village network receivers registered");
    }

    /**
     * Request culture data for a specific villager
     */
    public static void requestVillagerCultureData(UUID villagerUuid) {
        // Sending requires the payload object directly
        // This seems like a C2S request, but the payload is registered S2C? Re-evaluating registration.
        // Let's assume this is meant to be a request *to* the server.
        // We need a C2S payload for requesting culture, or reuse an existing one if appropriate.
        // For now, commenting out as the logic seems flawed.
        // RequestVillagerCulturePayload payload = new RequestVillagerCulturePayload(villagerUuid); // Assuming this payload exists
        // ClientPlayNetworking.send(payload);
        LOGGER.warn("requestVillagerCultureData called, but no corresponding C2S payload seems registered.");
    }

    /**
     * Process a villager culture packet from the server
     */
    private static void handleVillagerCulturePacket(UUID villagerUuid, String culture) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Find the villager entity if it exists
        if (client.world != null && client.player != null) {
            // Update the conversation HUD if this is the current villager
            ConversationHud conversationHud = ConversationHud.getInstance();
            VillagerEntity currentVillager = conversationHud.getCurrentVillager();

            if (currentVillager != null && currentVillager.getUuid().equals(villagerUuid)) {
                // This is for the current villager, update the conversationHud
                conversationHud.updateVillagerCulture(villagerUuid, culture);
            }
        }
    }

    /**
     * Process a villager mood packet from the server
     */
    private static void handleVillagerMoodPacket(UUID villagerUuid, String mood) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Find the villager entity
        if (client.world != null) {
            // Update conversation HUD if this is the current villager
            ConversationHud conversationHud = ConversationHud.getInstance();
            VillagerEntity currentVillager = conversationHud.getCurrentVillager();

            if (currentVillager != null && currentVillager.getUuid().equals(villagerUuid)) {
                conversationHud.updateVillagerMood(mood);
            }
        }
    }

    /**
     * Process a village info packet from the server
     */
    private static void handleVillageInfoPacket(String cultureName, int prosperity, int safety, int population) {
        VillageInfoHud hud = VillageInfoHud.getInstance();
        if (hud != null) {
            hud.update(cultureName, prosperity, safety, population);
        }
    }

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
        // Sending requires the payload object directly
        RequestVillageInfoPayload payload = new RequestVillageInfoPayload(pos);
        ClientPlayNetworking.send(payload); // Send C2S payload
    }

    /**
     * Sends a request to the server to join an event
     * 
     * @param eventId The ID of the event to join
     */
    public static void requestJoinEvent(String eventId) {
        LOGGER.debug("Requesting to join event {}", eventId);
        // Sending requires the payload object directly
        JoinEventPayload payload = new JoinEventPayload(eventId);
        ClientPlayNetworking.send(payload); // Send C2S payload

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
        // Register handlers for our custom packet channels
        registerReceivers();
        LOGGER.info("Client-side village network handlers registered");
    }

    // Payload type registration should happen centrally (e.g., in VillagesNetwork.java)
    // This class should only register client-side *handlers*.
}