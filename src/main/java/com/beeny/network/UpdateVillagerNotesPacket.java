package com.beeny.network;

import com.beeny.Villagersreborn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beeny.data.VillagerData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public record UpdateVillagerNotesPacket(int villagerId, String notes) implements CustomPayload {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateVillagerNotesPacket.class);
    
    public static final Id<UpdateVillagerNotesPacket> ID = new Id<>(
        Identifier.of("villagersreborn", "update_notes"));
    
    public static final PacketCodec<RegistryByteBuf, UpdateVillagerNotesPacket> CODEC = PacketCodec.of(
        (value, buf) -> {
            buf.writeInt(value.villagerId);
            String notesToWrite = value.notes != null ? value.notes : "";
            buf.writeString(notesToWrite, 500);
        },
        buf -> {
            int villagerId = buf.readInt();
            String notes = buf.readString(500);
            String safeNotes = notes != null && !notes.isEmpty() ? notes : "";
            return new UpdateVillagerNotesPacket(villagerId, safeNotes);
        }
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
                // Validate villagerId is positive
                if (payload.villagerId() <= 0) {
                    player.sendMessage(Text.literal("§cInvalid villager ID!"), false);
                    return;
                }
                
                // Validate player is not null
                if (player == null) {
                    LOGGER.warn("Player is null in UpdateVillagerNotesPacket");
                    return;
                }
                
                Entity entity = player.getWorld().getEntityById(payload.villagerId());
                
                // Validate entity exists and is a VillagerEntity
                if (!(entity instanceof VillagerEntity)) {
                    player.sendMessage(Text.literal("§cEntity is not a villager!"), false);
                    return;
                }
                
                VillagerEntity villager = (VillagerEntity) entity;
                
                // Validate player is within reasonable distance of the villager (10 blocks)
                double distance = player.getPos().distanceTo(villager.getPos());
                if (distance > 10.0) {
                    player.sendMessage(Text.literal("§cYou are too far from the villager!"), false);
                    return;
                }
                
                VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
                
                // Validate villager data exists
                if (data == null) {
                    player.sendMessage(Text.literal("§cVillager data not found!"), false);
                    return;
                }
                
                data.setNotes(payload.notes());
                player.sendMessage(Text.literal("Notes updated for " + data.getName()), false);
            });
        });
    }
}