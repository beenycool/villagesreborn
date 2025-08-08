package com.beeny.network;

import com.beeny.client.gui.VillagerFamilyTreeScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

@Environment(EnvType.CLIENT)
public class FamilyTreeDataPacketClient {
    // Guard to prevent duplicate registration across reloads or multiple entrypoints
    private static volatile boolean REGISTERED = false;

    public static void register() {
        if (REGISTERED) {
            return;
        }

        // Register the payload type for CLIENTBOUND PLAY before registering the handler
        PayloadTypeRegistry.playS2C().register(FamilyTreeDataPacket.ID, FamilyTreeDataPacket.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(FamilyTreeDataPacket.ID, (payload, context) -> {
            // Open the family tree screen with the received data
            context.client().execute(() -> {
                MinecraftClient.getInstance().setScreen(new VillagerFamilyTreeScreen(payload.getVillagerId(), payload.getFamilyMembers()));
            });
        });

        REGISTERED = true;
    }
}