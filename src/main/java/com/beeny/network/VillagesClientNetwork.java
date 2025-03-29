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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Handles client-side networking for Villages Reborn
 * Updated for Minecraft 1.21.4 networking API
 */
public class VillagesClientNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillagesClientNetwork");

    // Channel identifiers for different packet types - using Identifier.of() for 1.21.4
    public static final Identifier VILLAGER_CULTURE_ID = Identifier.of("villagesreborn", "villager_culture");
    public static final Identifier VILLAGER_MOOD_ID = Identifier.of("villagesreborn", "villager_mood");
    public static final Identifier VILLAGE_INFO_ID = Identifier.of("villagesreborn", "village_info");
    public static final Identifier REQUEST_VILLAGE_INFO_ID = Identifier.of("villagesreborn", "request_village_info");
    public static final Identifier JOIN_EVENT_ID = Identifier.of("villagesreborn", "join_event");

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
        
        @Override
        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeString(culture);
        }
        
        @Override
        public Identifier id() {
            return VILLAGER_CULTURE_ID;
        }
        
        public UUID getVillagerUuid() {
            return villagerUuid;
        }
        
        public String getCulture() {
            return culture;
        }
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
        
        @Override
        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeString(mood);
        }
        
        @Override
        public Identifier id() {
            return VILLAGER_MOOD_ID;
        }
        
        public UUID getVillagerUuid() {
            return villagerUuid;
        }
        
        public String getMood() {
            return mood;
        }
    }
    
    public static class RequestVillageInfoPayload implements CustomPayload {
        private final BlockPos position;
        
        public RequestVillageInfoPayload(BlockPos position) {
            this.position = position;
        }
        
        public RequestVillageInfoPayload(PacketByteBuf buf) {
            this.position = buf.readBlockPos();
        }
        
        @Override
        public void write(PacketByteBuf buf) {
            buf.writeBlockPos(position);
        }
        
        @Override
        public Identifier id() {
            return REQUEST_VILLAGE_INFO_ID;
        }
        
        public BlockPos getPosition() {
            return position;
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
        
        @Override
        public Identifier id() {
            return JOIN_EVENT_ID;
        }
        
        public String getEventId() {
            return eventId;
        }
    }

    /**
     * Register all client-side packet receivers
     */
    public static void registerReceivers() {
        // Register receiver for villager culture data
        ClientPlayNetworking.registerGlobalReceiver(VillagerCulturePayload.class, (payload, client, handler, responseSender) -> {
            // Process on the main client thread
            client.execute(() -> {
                handleVillagerCulturePacket(payload.getVillagerUuid(), payload.getCulture());
            });
        });

        // Register receiver for villager mood updates
        ClientPlayNetworking.registerGlobalReceiver(VillagerMoodPayload.class, (payload, client, handler, responseSender) -> {
            client.execute(() -> {
                handleVillagerMoodPacket(payload.getVillagerUuid(), payload.getMood());
            });
        });

        // Register receiver for village information - reuse the payload from VillagesNetwork.java
        ClientPlayNetworking.registerGlobalReceiver(VillagesNetwork.VillageInfoPayload.class, (payload, client, handler, responseSender) -> {
            client.execute(() -> {
                handleVillageInfoPacket(
                    payload.getCulture(),
                    payload.getProsperity(),
                    payload.getSafety(),
                    payload.getPopulation()
                );
            });
        });
        
        LOGGER.info("Client-side village network receivers registered");
    }

    /**
     * Request culture data for a specific villager
     */
    public static void requestVillagerCultureData(UUID villagerUuid) {
        VillagerCulturePayload payload = new VillagerCulturePayload(villagerUuid, "");
        ClientPlayNetworking.send(payload);
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
        // Update the village info HUD
        VillageInfoHud.getInstance().updateVillageInfo(cultureName, prosperity, safety, population);
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
        RequestVillageInfoPayload payload = new RequestVillageInfoPayload(pos);
        ClientPlayNetworking.send(payload);
    }

    /**
     * Sends a request to the server to join an event
     * 
     * @param eventId The ID of the event to join
     */
    public static void requestJoinEvent(String eventId) {
        LOGGER.debug("Requesting to join event {}", eventId);
        JoinEventPayload payload = new JoinEventPayload(eventId);
        ClientPlayNetworking.send(payload);

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
}