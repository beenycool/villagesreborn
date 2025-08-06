package com.beeny.network;

import com.beeny.system.VillagerRelationshipManager;
import com.beeny.constants.StringConstants;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record VillagerMarriagePacket(int villager1Id, int villager2Id) implements CustomPayload {
    public static final Id<VillagerMarriagePacket> ID = new Id<>(
        Identifier.of(StringConstants.MOD_ID, StringConstants.CH_MARRIAGE));
    
    // Rate limiting - track last time each player sent a marriage packet
    private static final Map<UUID, Long> lastMarriageAttempt = new ConcurrentHashMap<>();
    
    private static long getMarriageCooldown() {
        return com.beeny.config.ConfigManager.getInt("marriagePacketCooldown", 5000);
    }
    
    public static final PacketCodec<RegistryByteBuf, VillagerMarriagePacket> CODEC = PacketCodec.of(
        (value, buf) -> {
            buf.writeInt(value.villager1Id);
            buf.writeInt(value.villager2Id);
        },
        buf -> new VillagerMarriagePacket(buf.readInt(), buf.readInt())
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
        
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            
            context.server().execute(() -> {
                // Rate limiting check
                UUID playerUUID = player.getUuid();
                long currentTime = System.currentTimeMillis();
                Long lastAttempt = lastMarriageAttempt.get(playerUUID);
                
                if (lastAttempt != null && (currentTime - lastAttempt) < getMarriageCooldown()) {
                    long remainingTime = (getMarriageCooldown() - (currentTime - lastAttempt)) / 1000;
                    player.sendMessage(Text.literal(StringConstants.MSG_MARRIAGE_RATE_LIMIT_PREFIX + (remainingTime + 1) + StringConstants.MSG_MARRIAGE_RATE_LIMIT_SUFFIX)
                        .formatted(Formatting.RED), false);
                    return;
                }
                
                // Update last attempt time
                lastMarriageAttempt.put(playerUUID, currentTime);
                
                // Check if the same entity ID is used twice
                if (payload.villager1Id() == payload.villager2Id()) {
                    player.sendMessage(Text.literal(StringConstants.MSG_MARRIAGE_SAME_VILLAGER)
                        .formatted(Formatting.RED), false);
                    return;
                }
                
                // Get entities by ID
                Entity entity1 = player.getWorld().getEntityById(payload.villager1Id());
                Entity entity2 = player.getWorld().getEntityById(payload.villager2Id());
                
                // Null checks for entities
                if (entity1 == null || entity2 == null) {
                    player.sendMessage(Text.literal(StringConstants.MSG_MARRIAGE_NOT_FOUND)
                        .formatted(Formatting.RED), false);
                    return;
                }
                
                // Check if entities are villagers
                if (!(entity1 instanceof VillagerEntity villager1) ||
                    !(entity2 instanceof VillagerEntity villager2)) {
                    player.sendMessage(Text.literal(StringConstants.MSG_MARRIAGE_NOT_VILLAGERS)
                        .formatted(Formatting.RED), false);
                    return;
                }
                
                // Check if player is within range of both villagers
                double distanceToVillager1 = player.getPos().distanceTo(villager1.getPos());
                double distanceToVillager2 = player.getPos().distanceTo(villager2.getPos());
                double maxInteractionDistance = 5.0; // Max distance for interaction
                
                if (distanceToVillager1 > maxInteractionDistance || distanceToVillager2 > maxInteractionDistance) {
                    player.sendMessage(Text.literal(StringConstants.MSG_MARRIAGE_TOO_FAR)
                        .formatted(Formatting.RED), false);
                    return;
                }
                
                // Attempt marriage
                if (VillagerRelationshipManager.attemptMarriage(villager1, villager2)) {
                    player.sendMessage(Text.literal(StringConstants.MSG_MARRIAGE_SUCCESS)
                        .formatted(Formatting.GREEN), false);
                } else {
                    player.sendMessage(Text.literal(StringConstants.MSG_MARRIAGE_FAILED_CONDITIONS)
                        .formatted(Formatting.RED), false);
                }
            });
        });
    }
}