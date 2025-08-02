package com.beeny.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TestLLMConnectionResultPacket(boolean success, String message) implements CustomPayload {
    public static final Id<TestLLMConnectionResultPacket> ID = new Id<>(
        Identifier.of("villagersreborn", "test_llm_connection_result"));

    public static final PacketCodec<RegistryByteBuf, TestLLMConnectionResultPacket> CODEC = PacketCodec.of(
        (value, buf) -> {
            buf.writeBoolean(value.success);
            buf.writeString(value.message == null ? "" : value.message);
        },
        buf -> new TestLLMConnectionResultPacket(
            buf.readBoolean(),
            buf.readString()
        )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }
}