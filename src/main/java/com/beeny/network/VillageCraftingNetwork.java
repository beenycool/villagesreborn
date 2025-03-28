package com.beeny.network;

import com.beeny.village.VillageCraftingManager;
import com.beeny.village.SpawnRegion;
import com.beeny.village.VillagerManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.UUID;

public class VillageCraftingNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillageCraftingNetwork");
    
    // Packet identifiers
    public static final Identifier CRAFT_PACKET = new Identifier("villagesreborn", "craft_recipe");
    public static final Identifier REQUEST_RECIPES_PACKET = new Identifier("villagesreborn", "request_recipes");
    public static final Identifier RECIPE_LIST_PACKET = new Identifier("villagesreborn", "recipe_list");
    public static final Identifier CRAFT_STATUS_PACKET = new Identifier("villagesreborn", "craft_status");
    public static final Identifier CANCEL_CRAFT_PACKET = new Identifier("villagesreborn", "cancel_craft");

    public static void register() {
        LOGGER.info("Registering VillageCraftingNetwork packet handlers");
        
        // Register server-side packet receiver for crafting requests
        ServerPlayNetworking.registerGlobalReceiver(CRAFT_PACKET, (server, player, handler, buf, responseSender) -> {
            UUID villagerUuid = buf.readUuid();
            String recipeId = buf.readString();
            
            // Execute on the server thread
            server.execute(() -> {
                handleCraftRequest(villagerUuid, recipeId, player);
            });
        });
        
        // Register server-side packet receiver for recipe list requests
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_RECIPES_PACKET, (server, player, handler, buf, responseSender) -> {
            UUID villagerUuid = buf.readUuid();
            
            server.execute(() -> {
                handleRecipeListRequest(villagerUuid, player, responseSender);
            });
        });
        
        // Register server-side packet receiver for canceling crafting
        ServerPlayNetworking.registerGlobalReceiver(CANCEL_CRAFT_PACKET, (server, player, handler, buf, responseSender) -> {
            UUID villagerUuid = buf.readUuid();
            String recipeId = buf.readString();
            
            server.execute(() -> {
                handleCancelCraftRequest(villagerUuid, recipeId, player);
            });
        });
        
        // Register connection events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Initialize connection - no packet handling needed here
        });
        
        LOGGER.info("VillageCraftingNetwork packet handlers registered successfully");
    }

    public static void handleCraftingRequest(VillagerEntity villager, String recipeId, ServerPlayerEntity player) {
        // Direct server-side handling without networking
        if (isWithinReach(player, villager)) {
            VillageCraftingManager.getInstance().assignTask(villager, recipeId, player);
        }
    }

    private static void handleCraftRequest(UUID villagerUuid, String recipeId, ServerPlayerEntity player) {
        LOGGER.debug("Handling craft request for recipe {} from player {}", recipeId, player.getName().getString());
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
            } else {
                sendCraftStatusToPlayer(player, villagerUuid, recipeId, "too_far", "You're too far from the villager.");
            }
        } else {
            sendCraftStatusToPlayer(player, villagerUuid, recipeId, "not_found", "Villager not found.");
        }
    }
    
    private static void handleRecipeListRequest(UUID villagerUuid, ServerPlayerEntity player, PacketSender responseSender) {
        LOGGER.debug("Handling recipe list request for villager {} from player {}", villagerUuid, player.getName().getString());
        // Find the villager
        Box searchBox = Box.of(player.getPos(), 10, 10, 10);
        List<VillagerEntity> nearbyVillagers = player.getWorld().getEntitiesByClass(
            VillagerEntity.class,
            searchBox,
            v -> v.getUuid().equals(villagerUuid)
        );
        
        if (!nearbyVillagers.isEmpty()) {
            VillagerEntity villager = nearbyVillagers.get(0);
            if (isWithinReach(player, villager)) {
                // Determine culture from villager's location
                SpawnRegion region = VillagerManager.getInstance().getNearestSpawnRegion(villager.getBlockPos());
                String culture = (region != null) ? region.getCultureAsString() : "default";
                
                // Get recipes for that culture and villager profession
                List<String> recipeIds = VillageCraftingManager.getInstance().getAvailableRecipes(
                    villager, player, culture);
                
                // Send the recipe list back to the client
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeUuid(villagerUuid);
                buf.writeInt(recipeIds.size());
                for (String recipeId : recipeIds) {
                    buf.writeString(recipeId);
                }
                
                responseSender.sendPacket(RECIPE_LIST_PACKET, buf);
            }
        }
    }
    
    private static void handleCancelCraftRequest(UUID villagerUuid, String recipeId, ServerPlayerEntity player) {
        LOGGER.debug("Handling cancel craft request for recipe {} from player {}", recipeId, player.getName().getString());
        // Implementation for canceling an in-progress crafting task
        VillageCraftingManager.getInstance().cancelTask(villagerUuid, recipeId, player);
    }
    
    private static void sendCraftStatusToPlayer(ServerPlayerEntity player, UUID villagerUuid, 
                                              String recipeId, String status, String message) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(villagerUuid);
        buf.writeString(recipeId);
        buf.writeString(status);
        buf.writeString(message);
        ServerPlayNetworking.send(player, CRAFT_STATUS_PACKET, buf);
    }

    private static boolean isWithinReach(ServerPlayerEntity player, VillagerEntity villager) {
        double distanceSquared = player.squaredDistanceTo(villager);
        return distanceSquared <= 64.0;
    }
}