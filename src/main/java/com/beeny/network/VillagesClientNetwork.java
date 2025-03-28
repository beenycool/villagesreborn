package com.beeny.network;

import com.beeny.gui.ConversationHud;
import com.beeny.gui.VillageInfoHud;
import com.beeny.village.VillagerManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Handles client-side networking for Villages Reborn
 */
public class VillagesClientNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillagesClientNetwork");

    // Channel identifiers for different packet types
    public static final Identifier VILLAGER_CULTURE_CHANNEL = 
        new Identifier("villagesreborn", "villager_culture");
    public static final Identifier VILLAGER_MOOD_CHANNEL = 
        new Identifier("villagesreborn", "villager_mood");
    public static final Identifier VILLAGE_INFO_CHANNEL = 
        new Identifier("villagesreborn", "village_info");
    public static final Identifier REQUEST_VILLAGE_INFO_PACKET = 
        new Identifier("villagesreborn", "request_village_info");
    public static final Identifier JOIN_EVENT_PACKET = 
        new Identifier("villagesreborn", "join_event");

    /**
     * Register all client-side packet receivers
     */
    public static void registerReceivers() {
        // Register receiver for villager culture data
        ClientPlayNetworking.registerGlobalReceiver(VILLAGER_CULTURE_CHANNEL, (client, handler, buf, responseSender) -> {
            UUID villagerUuid = buf.readUuid();
            String culture = buf.readString();

            // Process on the main client thread
            client.execute(() -> {
                handleVillagerCulturePacket(villagerUuid, culture);
            });
        });

        // Register receiver for villager mood updates
        ClientPlayNetworking.registerGlobalReceiver(VILLAGER_MOOD_CHANNEL, (client, handler, buf, responseSender) -> {
            UUID villagerUuid = buf.readUuid();
            String mood = buf.readString();

            client.execute(() -> {
                handleVillagerMoodPacket(villagerUuid, mood);
            });
        });

        // Register receiver for village information
        ClientPlayNetworking.registerGlobalReceiver(VILLAGE_INFO_CHANNEL, (client, handler, buf, responseSender) -> {
            String cultureName = buf.readString();
            int prosperity = buf.readInt();
            int safety = buf.readInt();
            int population = buf.readInt();

            client.execute(() -> {
                handleVillageInfoPacket(cultureName, prosperity, safety, population);
            });
        });
    }

    /**
     * Request culture data for a specific villager
     */
    public static void requestVillagerCultureData(UUID villagerUuid) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(villagerUuid);

        // Send the request to the server
        ClientPlayNetworking.send(VILLAGER_CULTURE_CHANNEL, buf);
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
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);

        // Send the packet to the server
        ClientPlayNetworking.send(REQUEST_VILLAGE_INFO_PACKET, buf);
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
        ClientPlayNetworking.send(JOIN_EVENT_PACKET, buf);

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