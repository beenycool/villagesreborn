package com.beeny.network;

import com.beeny.client.gui.VillagerFamilyTreeScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

@Environment(EnvType.CLIENT)
public class FamilyTreeDataPacketClient {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(FamilyTreeDataPacket.ID, (payload, context) -> {
            // Open the family tree screen with the received data
            context.client().execute(() -> {
                MinecraftClient.getInstance().setScreen(new VillagerFamilyTreeScreen(payload.getVillagerId(), payload.getFamilyMembers()));
            });
        });
    }
}