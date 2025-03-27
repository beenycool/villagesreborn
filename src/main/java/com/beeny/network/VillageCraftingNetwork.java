package com.beeny.network;

import com.beeny.village.VillageCraftingManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import java.util.List;
import java.util.UUID;

public class VillageCraftingNetwork {
    public static final Identifier CRAFT_PACKET = new Identifier("villagesreborn", "craft_recipe");

    public static void register() {
        // Register server-side packet receiver
        ServerPlayNetworking.registerGlobalReceiver(CRAFT_PACKET, (server, player, handler, buf, responseSender) -> {
            UUID villagerUuid = buf.readUuid();
            String recipeId = buf.readString();
            
            // Execute on the server thread
            server.execute(() -> {
                handleCraftRequest(villagerUuid, recipeId, player);
            });
        });
        
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Initialize connection - no packet handling needed here
        });
    }

    public static void handleCraftingRequest(VillagerEntity villager, String recipeId, ServerPlayerEntity player) {
        // Direct server-side handling without networking
        if (isWithinReach(player, villager)) {
            VillageCraftingManager.getInstance().assignTask(villager, recipeId, player);
        }
    }

    private static void handleCraftRequest(UUID villagerUuid, String recipeId, ServerPlayerEntity player) {
        Box searchBox = Box.of(player.getPos(), 10, 10, 10);
        List<VillagerEntity> nearbyVillagers = player.getWorld().getEntitiesByClass(
            VillagerEntity.class,
            searchBox,
            v -> v.getUuid().equals(villagerUuid)
        );

        if (!nearbyVillagers.isEmpty()) {
            VillagerEntity villager = nearbyVillagers.get(0);
            if (isWithinReach(player, villager)) {
                VillageCraftingManager.getInstance().assignTask(villager, recipeId, player);
            }
        }
    }

    private static boolean isWithinReach(ServerPlayerEntity player, VillagerEntity villager) {
        double distanceSquared = player.squaredDistanceTo(villager);
        return distanceSquared <= 64.0;
    }
}