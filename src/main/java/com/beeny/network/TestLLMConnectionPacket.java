package com.beeny.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TestLLMConnectionPacket(String provider, String apiKey, String endpoint, String model) implements CustomPayload {
    public static final Id<TestLLMConnectionPacket> ID = new Id<>(
        Identifier.of("villagersreborn", "test_llm_connection"));

    public static final PacketCodec<RegistryByteBuf, TestLLMConnectionPacket> CODEC = PacketCodec.of(
        (value, buf) -> {
            buf.writeString(value.provider);
            buf.writeString(value.apiKey);
            buf.writeString(value.endpoint);
            buf.writeString(value.model);
        },
        buf -> new TestLLMConnectionPacket(
            buf.readString(),
            buf.readString(),
            buf.readString(),
            buf.readString()
        )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
    }
}