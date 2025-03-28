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
// Remove the ClientPlayNetworking import since we'll handle client-side logic in a separate class
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.UUID;

/**
 * Handles network communication for the village crafting system.
 * This includes crafting requests, recipe list requests, and crafting status updates.
 */
public class VillageCraftingNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillageCraftingNetwork");
    
    // Packet identifiers
    public static final Identifier CRAFT_PACKET = new Identifier("villagesreborn", "craft_recipe");
    public static final Identifier REQUEST_RECIPES_PACKET = new Identifier("villagesreborn", "request_recipes");
    public static final Identifier RECIPE_LIST_PACKET = new Identifier("villagesreborn", "recipe_list");
    public static final Identifier CRAFT_STATUS_PACKET = new Identifier("villagesreborn", "craft_status");
    public static final Identifier CANCEL_CRAFT_PACKET = new Identifier("villagesreborn", "cancel_craft");
    public static final Identifier CRAFT_PROGRESS_PACKET = new Identifier("villagesreborn", "craft_progress");
    public static final Identifier CRAFT_COMPLETE_PACKET = new Identifier("villagesreborn", "craft_complete");

    /**
     * Register all network handlers
     */
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
        
        LOGGER.info("VillageCraftingNetwork packet handlers registered successfully");
    }

    /**
     * Register client-side network handlers - to be called from client mod initializer
     * NOTE: Client-side implementation is moved to VillageCraftingNetworkClient class to avoid import issues
     */
    public static void registerClientHandlers() {
        // Client-side registration has been moved to VillageCraftingNetworkClient
        // to handle the ClientPlayNetworking import issue
        LOGGER.info("VillageCraftingNetwork delegating client registration to client-side class");
    }

    /**
     * Direct method to handle a crafting request on the server side without going through networking
     */
    public static void handleCraftingRequest(VillagerEntity villager, String recipeId, ServerPlayerEntity player) {
        // Direct server-side handling without networking
        if (isWithinReach(player, villager)) {
            VillageCraftingManager.getInstance().assignTask(villager, recipeId, player);
        }
    }

    /**
     * Handle a crafting request from a client
     */
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
    
    /**
     * Handle a recipe list request from a client
     */
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
                
                ServerPlayNetworking.send(player, RECIPE_LIST_PACKET, buf);
            }
        }
    }
    
    /**
     * Handle a cancel craft request from a client
     */
    private static void handleCancelCraftRequest(UUID villagerUuid, String recipeId, ServerPlayerEntity player) {
        LOGGER.debug("Handling cancel craft request for recipe {} from player {}", recipeId, player.getName().getString());
        // Implementation for canceling an in-progress crafting task
        VillageCraftingManager.getInstance().cancelTask(villagerUuid, recipeId, player);
    }
    
    /**
     * Send a craft status update to a player
     */
    public static void sendCraftStatusToPlayer(ServerPlayerEntity player, UUID villagerUuid, 
                                             String recipeId, String status, String message) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(villagerUuid);
        buf.writeString(recipeId);
        buf.writeString(status);
        buf.writeString(message);
        ServerPlayNetworking.send(player, CRAFT_STATUS_PACKET, buf);
    }

    /**
     * Send a craft progress update to a player
     */
    public static void sendCraftProgressToPlayer(ServerPlayerEntity player, UUID villagerUuid,
                                               String recipeId, int progress, int maxProgress) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(villagerUuid);
        buf.writeString(recipeId);
        buf.writeInt(progress);
        buf.writeInt(maxProgress);
        ServerPlayNetworking.send(player, CRAFT_PROGRESS_PACKET, buf);
    }

    /**
     * Send a craft completion notification to a player
     */
    public static void sendCraftCompleteToPlayer(ServerPlayerEntity player, UUID villagerUuid,
                                               String recipeId, boolean success, ItemStack result) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(villagerUuid);
        buf.writeString(recipeId);
        buf.writeBoolean(success);
        
        // Only write the result if crafting was successful
        if (success) {
            buf.writeItemStack(result);
        }
        
        ServerPlayNetworking.send(player, CRAFT_COMPLETE_PACKET, buf);
    }

    /**
     * Check if a player is within reach of a villager
     */
    private static boolean isWithinReach(ServerPlayerEntity player, VillagerEntity villager) {
        double distanceSquared = player.squaredDistanceTo(villager);
        return distanceSquared <= 64.0; // 8 blocks squared
    }
}