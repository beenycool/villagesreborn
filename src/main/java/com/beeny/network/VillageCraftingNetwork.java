package com.beeny.network;

import com.beeny.village.VillageCraftingManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.NetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;

public class VillageCraftingNetwork {
    public static final Identifier CRAFT_CHANNEL = new Identifier("villagesreborn", "craft");

    public static void sendCraftPacket(VillagerEntity villager, String recipeId, ServerPlayerEntity player) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeUuid(villager.getUuid());
        buf.writeString(recipeId);
        
        handleCraftRequest(villager.getUuid(), recipeId, player);
    }

    private static void handleCraftRequest(UUID villagerUuid, String recipeId, ServerPlayerEntity player) {
        // Search for the villager in a small radius around the player
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
        return distanceSquared <= 64.0; // 8 blocks distance
    }
}