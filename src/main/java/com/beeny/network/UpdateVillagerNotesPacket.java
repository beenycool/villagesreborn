package com.beeny.network;

import com.beeny.Villagersreborn;
import com.beeny.constants.StringConstants;
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
        Identifier.of(StringConstants.MOD_ID, StringConstants.CH_UPDATE_NOTES));
    
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
                
                if (payload.villagerId() <= 0) {
                    player.sendMessage(Text.literal(StringConstants.MSG_INVALID_VILLAGER_ID), false);
                    return;
                }
                
                
                if (player == null) {
                    LOGGER.warn("[WARN] [UpdateVillagerNotesPacket] [register] - Player is null in UpdateVillagerNotesPacket");
                    return;
                }
                
                Entity entity = player.getWorld().getEntityById(payload.villagerId());
                
                
                if (!(entity instanceof VillagerEntity)) {
                    player.sendMessage(Text.literal(StringConstants.MSG_NOT_A_VILLAGER), false);
                    return;
                }
                
                VillagerEntity villager = (VillagerEntity) entity;
                
                
                double distance = player.getPos().distanceTo(villager.getPos());
                if (distance > 10.0) {
                    player.sendMessage(Text.literal(StringConstants.MSG_TOO_FAR), false);
                    return;
                }
                
                VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
                
                
                if (data == null) {
                    player.sendMessage(Text.literal(StringConstants.MSG_VILLAGER_DATA_NOT_FOUND), false);
                    return;
                }
                
                data.setNotes(payload.notes());
                player.sendMessage(Text.literal(StringConstants.MSG_NOTES_UPDATED_PREFIX + data.getName()), false);
            });
        });
    }
}