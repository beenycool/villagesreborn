package com.beeny.network;

import com.beeny.client.gui.EnhancedVillagerJournalScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.passive.VillagerEntity;

import java.util.List;
import java.util.stream.Collectors;

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
                    // Get actual villager entities from the world based on entity IDs
                    List<VillagerEntity> villagers = payload.getVillagerDataList().stream()
                        .map(data -> client.world.getEntityById(data.getEntityId()))
                        .filter(entity -> entity instanceof VillagerEntity)
                        .map(entity -> (VillagerEntity) entity)
                        .collect(Collectors.toList());
                    
                    if (!villagers.isEmpty()) {
                        client.setScreen(new EnhancedVillagerJournalScreen(villagers));
                    }
                }
            });
        });
    }
}