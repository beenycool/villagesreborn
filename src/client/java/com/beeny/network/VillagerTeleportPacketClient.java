package com.beeny.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class VillagerTeleportPacketClient {
    public static void register() {
        
    }
    
    public static void sendToServer(int villagerId) {
        
        if (villagerId < 0) {
            throw new IllegalArgumentException("Villager ID must be non-negative");
        }
        
        ClientPlayNetworking.send(new VillagerTeleportPacket(villagerId));
    }
}