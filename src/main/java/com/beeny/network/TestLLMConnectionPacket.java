package com.beeny.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Packet for testing LLM connection.
 *
 * <b>Security Warning:</b>
 * The {@code apiKey} field is serialized and transmitted in plain text.
 * This exposes sensitive credentials to anyone with access to the network traffic.
 * <p>
 * <b>To mitigate this risk:</b>
 * <ul>
 *   <li>Always transmit this packet over encrypted channels (e.g., TLS/SSL).</li>
 *   <li>Consider implementing a secure key exchange mechanism if possible.</li>
 *   <li>Never send API keys over untrusted or unencrypted connections.</li>
 * </ul>
 * Failure to follow these recommendations may result in credential leakage and compromise of connected services.
 */
public record TestLLMConnectionPacket(String provider, String apiKey, String endpoint, String model) implements CustomPayload {
    public static final Id<TestLLMConnectionPacket> ID = new Id<>(
        Identifier.of("villagersreborn", "test_llm_connection"));

    // --- SECURITY NOTE ---
    // The apiKey field is now encrypted using AES before transmission.
    // For real security, use proper key management and exchange.
    private static final String AES_SECRET = "villagersrebornAES"; // 16 chars for AES-128

    private static String encrypt(String plainText) {
        try {
            javax.crypto.SecretKey key = new javax.crypto.spec.SecretKeySpec(AES_SECRET.getBytes(), "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    private static String decrypt(String cipherText) {
        try {
            javax.crypto.SecretKey key = new javax.crypto.spec.SecretKeySpec(AES_SECRET.getBytes(), "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key);
            byte[] decoded = java.util.Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public static final PacketCodec<RegistryByteBuf, TestLLMConnectionPacket> CODEC = PacketCodec.of(
        (value, buf) -> {
            buf.writeString(value.provider);
            buf.writeString(encrypt(value.apiKey)); // Encrypt apiKey before sending
            buf.writeString(value.endpoint);
            buf.writeString(value.model);
        },
        buf -> new TestLLMConnectionPacket(
            buf.readString(),
            decrypt(buf.readString()), // Decrypt apiKey after receiving
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
        
        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            context.server().execute(() -> {
                // Test the LLM connection with the provided parameters
                com.beeny.dialogue.LLMDialogueManager.testConnection(
                    payload.provider(),
                    payload.apiKey(),
                    payload.endpoint(),
                    payload.model()
                ).whenComplete((success, throwable) -> {
                    String message;
                    boolean result;
                    
                    if (throwable != null) {
                        result = false;
                        message = "Connection test failed: " + throwable.getMessage();
                    } else {
                        result = success;
                        message = success ? "Connection successful!" : "Connection failed - please check your configuration";
                    }
                    
                    // Send result back to client
                    TestLLMConnectionResultPacket resultPacket = new TestLLMConnectionResultPacket(result, message);
                    ServerPlayNetworking.send(context.player(), resultPacket);
                });
            });
        });
    }
}