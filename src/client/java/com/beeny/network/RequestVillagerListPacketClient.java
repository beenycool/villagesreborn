package com.beeny.network;

import com.beeny.client.gui.VillagerJournalScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public class RequestVillagerListPacketClient {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RequestVillagerListPacket.ResponsePacket.ID, (payload, context) -> {
            // Debug logging
            System.out.println("[RequestVillagerListPacketClient] Received villager list response with " + payload.getVillagerDataList().size() + " villagers");
            
            // Open the villager journal screen with the received data
            context.client().execute(() -> {
                MinecraftClient.getInstance().setScreen(VillagerJournalScreen.createFromPacketData(payload.getVillagerDataList()));
            });
        });
    }
}