package com.beeny.network;

import com.beeny.constants.StringConstants;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client-to-server packet requesting a test of the LLM connection.
 * 
 * This packet DOES NOT contain any sensitive information like API keys.
 * The server uses its own securely stored configuration to perform the test.
 */
public record TestLLMConnectionPacket() implements CustomPayload {
    public static final Id<TestLLMConnectionPacket> ID = new Id<>(
        Identifier.of(StringConstants.MOD_ID, StringConstants.CH_TEST_LLM_CONNECTION));

    public static final PacketCodec<RegistryByteBuf, TestLLMConnectionPacket> CODEC = PacketCodec.of(
        (value, buf) -> {
            // No data to serialize - this is just a request signal
        },
        buf -> new TestLLMConnectionPacket()
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
    }
}