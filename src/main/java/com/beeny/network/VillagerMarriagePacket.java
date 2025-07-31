package com.beeny.network;

import com.beeny.system.VillagerRelationshipManager;
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
        Identifier.of("villagersreborn", "marriage"));
    
    // Rate limiting - track last time each player sent a marriage packet
    private static final Map<UUID, Long> lastMarriageAttempt = new ConcurrentHashMap<>();
    private static final long MARRIAGE_PACKET_COOLDOWN = 5000; // 5 seconds cooldown
    
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
                
                if (lastAttempt != null && (currentTime - lastAttempt) < MARRIAGE_PACKET_COOLDOWN) {
                    long remainingTime = (MARRIAGE_PACKET_COOLDOWN - (currentTime - lastAttempt)) / 1000;
                    player.sendMessage(Text.literal("Marriage request failed - please wait " + (remainingTime + 1) + " seconds before trying again")
                        .formatted(Formatting.RED), false);
                    return;
                }
                
                // Update last attempt time
                lastMarriageAttempt.put(playerUUID, currentTime);
                
                // Check if the same entity ID is used twice
                if (payload.villager1Id() == payload.villager2Id()) {
                    player.sendMessage(Text.literal("Marriage failed - cannot marry the same villager to themselves")
                        .formatted(Formatting.RED), false);
                    return;
                }
                
                // Get entities by ID
                Entity entity1 = player.getWorld().getEntityById(payload.villager1Id());
                Entity entity2 = player.getWorld().getEntityById(payload.villager2Id());
                
                // Null checks for entities
                if (entity1 == null || entity2 == null) {
                    player.sendMessage(Text.literal("Marriage failed - one or both villagers not found")
                        .formatted(Formatting.RED), false);
                    return;
                }
                
                // Check if entities are villagers
                if (!(entity1 instanceof VillagerEntity villager1) ||
                    !(entity2 instanceof VillagerEntity villager2)) {
                    player.sendMessage(Text.literal("Marriage failed - selected entities are not villagers")
                        .formatted(Formatting.RED), false);
                    return;
                }
                
                // Check if player is within range of both villagers
                double distanceToVillager1 = player.getPos().distanceTo(villager1.getPos());
                double distanceToVillager2 = player.getPos().distanceTo(villager2.getPos());
                double maxInteractionDistance = 5.0; // Max distance for interaction
                
                if (distanceToVillager1 > maxInteractionDistance || distanceToVillager2 > maxInteractionDistance) {
                    player.sendMessage(Text.literal("Marriage failed - you are too far from one or both villagers")
                        .formatted(Formatting.RED), false);
                    return;
                }
                
                // Attempt marriage
                if (VillagerRelationshipManager.attemptMarriage(villager1, villager2)) {
                    player.sendMessage(Text.literal("Marriage successful!")
                        .formatted(Formatting.GREEN), false);
                } else {
                    player.sendMessage(Text.literal("Marriage failed - conditions not met")
                        .formatted(Formatting.RED), false);
                }
            });
        });
    }
}