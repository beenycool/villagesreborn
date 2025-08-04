package com.beeny.network;

import com.beeny.client.gui.EnhancedVillagerJournalScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.util.List;

@Environment(EnvType.CLIENT)
public class RequestVillagerListPacketClient {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RequestVillagerListPacket.ResponsePacket.ID, (payload, context) -> {
            // Debug logging
            System.out.println("[RequestVillagerListPacketClient] Received villager list response with " + payload.getVillagerDataList().size() + " villagers");
            
            // Open the enhanced villager journal screen
            context.client().execute(() -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.world != null) {
                    List<VillagerDataPacket> villagerDataList = payload.getVillagerDataList();
                    
                    if (!villagerDataList.isEmpty()) {
                        client.setScreen(new EnhancedVillagerJournalScreen(villagerDataList));
                    }
                }
            });
        });
    }
}