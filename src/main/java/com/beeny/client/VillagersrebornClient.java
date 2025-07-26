package com.beeny.client;

import com.beeny.network.VillagerTeleportPacket;
import net.fabricmc.api.ClientModInitializer;

public class VillagersrebornClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register networking packets
        VillagerTeleportPacket.register();
    }
}