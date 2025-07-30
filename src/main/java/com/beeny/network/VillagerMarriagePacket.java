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

public record VillagerMarriagePacket(int villager1Id, int villager2Id) implements CustomPayload {
    public static final Id<VillagerMarriagePacket> ID = new Id<>(
        Identifier.of("villagersreborn", "marriage"));
    
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
                Entity entity1 = player.getWorld().getEntityById(payload.villager1Id());
                Entity entity2 = player.getWorld().getEntityById(payload.villager2Id());
                
                if (entity1 instanceof VillagerEntity villager1 && 
                    entity2 instanceof VillagerEntity villager2) {
                    
                    if (VillagerRelationshipManager.attemptMarriage(villager1, villager2)) {
                        player.sendMessage(Text.literal("Marriage successful!")
                            .formatted(Formatting.GREEN), false);
                    } else {
                        player.sendMessage(Text.literal("Marriage failed - conditions not met")
                            .formatted(Formatting.RED), false);
                    }
                }
            });
        });
    }
}