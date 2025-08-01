package com.beeny.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class OpenFamilyTreePacketClient {
    
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OpenFamilyTreePacket.ID, (payload, context) -> {
            System.out.println("[OpenFamilyTreePacket] Received packet for villager ID: " + payload.villagerEntityId());
            context.client().execute(() -> {
                // Request family tree data from server
                ClientPlayNetworking.send(new FamilyTreeDataPacket.RequestPacket(payload.villagerEntityId()));
            });
        });
    }
}