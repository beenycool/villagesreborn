package com.beeny.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import com.beeny.network.RequestVillagerListPacket;

@Environment(EnvType.CLIENT)
public class VillagerJournalItemClient {
    private static final int SCAN_RADIUS = 50; 

    public static void openVillagerJournal(World world, PlayerEntity player) {
        if (player == null) return;

        // Debug logging
        System.out.println("[VillagerJournalItemClient] Sending RequestVillagerListPacket to server...");
        
        // Request villager list from server
        ClientPlayNetworking.send(new RequestVillagerListPacket());
    }
}