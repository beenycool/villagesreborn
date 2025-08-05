package com.beeny.network;

import com.beeny.constants.StringConstants;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenFamilyTreePacket(int villagerEntityId) implements CustomPayload {
    public static final Id<OpenFamilyTreePacket> ID = new Id<>(
        Identifier.of(StringConstants.MOD_ID, StringConstants.CH_OPEN_FAMILY_TREE));
    
    public static final PacketCodec<RegistryByteBuf, OpenFamilyTreePacket> CODEC = PacketCodec.of(
        (value, buf) -> buf.writeInt(value.villagerEntityId),
        buf -> new OpenFamilyTreePacket(buf.readInt())
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
    
    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }
}