package com.beeny.network;

import com.beeny.village.VillageCraftingManager;
import com.beeny.village.SpawnRegion;
import com.beeny.village.VillagerManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.UUID;

/**
 * Handles network communication for the village crafting system.
 * This includes crafting requests, recipe list requests, and crafting status updates.
 * Updated for Minecraft 1.21.4 networking API
 */
public class VillageCraftingNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillageCraftingNetwork");
    
    // Packet identifiers - updated to use Identifier.of()
    public static final Identifier CRAFT_PACKET_ID = Identifier.of("villagesreborn", "craft_recipe");
    public static final Identifier REQUEST_RECIPES_PACKET_ID = Identifier.of("villagesreborn", "request_recipes");
    public static final Identifier RECIPE_LIST_PACKET_ID = Identifier.of("villagesreborn", "recipe_list");
    public static final Identifier CRAFT_STATUS_PACKET_ID = Identifier.of("villagesreborn", "craft_status");
    public static final Identifier CANCEL_CRAFT_PACKET_ID = Identifier.of("villagesreborn", "cancel_craft");
    public static final Identifier CRAFT_PROGRESS_PACKET_ID = Identifier.of("villagesreborn", "craft_progress");
    public static final Identifier CRAFT_COMPLETE_PACKET_ID = Identifier.of("villagesreborn", "craft_complete");

    // Custom payload classes for 1.21.4
    public static class CraftRecipePayload implements CustomPayload {
        public static final CustomPayload.Id<CraftRecipePayload> ID = new CustomPayload.Id<>(CRAFT_PACKET_ID);

        private final UUID villagerUuid;
        private final String recipeId;

        public CraftRecipePayload(UUID villagerUuid, String recipeId) {
            this.villagerUuid = villagerUuid;
            this.recipeId = recipeId;
        }

        // Updated constructor for RegistryByteBuf
        public CraftRecipePayload(RegistryByteBuf buf) {
            this.villagerUuid = buf.readUuid();
            this.recipeId = buf.readString();
        }

        // Updated write method for RegistryByteBuf
        public void write(RegistryByteBuf buf) { // Removed @Override
            buf.writeUuid(villagerUuid);
            buf.writeString(recipeId);
        }

        // Implement required getId method
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public UUID getVillagerUuid() {
            return villagerUuid;
        }
        
        public String getRecipeId() {
            return recipeId;
        }
    }

    public static class RequestRecipesPayload implements CustomPayload {
        public static final CustomPayload.Id<RequestRecipesPayload> ID = new CustomPayload.Id<>(REQUEST_RECIPES_PACKET_ID);

        private final UUID villagerUuid;

        public RequestRecipesPayload(UUID villagerUuid) {
            this.villagerUuid = villagerUuid;
        }

        // Updated constructor for RegistryByteBuf
        public RequestRecipesPayload(RegistryByteBuf buf) {
            this.villagerUuid = buf.readUuid();
        }

        // Updated write method for RegistryByteBuf
        public void write(RegistryByteBuf buf) { // Removed @Override
            buf.writeUuid(villagerUuid);
        }

        // Implement required getId method
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public UUID getVillagerUuid() {
            return villagerUuid;
        }
    }

    public static class CancelCraftPayload implements CustomPayload {
        public static final CustomPayload.Id<CancelCraftPayload> ID = new CustomPayload.Id<>(CANCEL_CRAFT_PACKET_ID);

        private final UUID villagerUuid;
        private final String recipeId;

        public CancelCraftPayload(UUID villagerUuid, String recipeId) {
            this.villagerUuid = villagerUuid;
            this.recipeId = recipeId;
        }

        // Updated constructor for RegistryByteBuf
        public CancelCraftPayload(RegistryByteBuf buf) {
            this.villagerUuid = buf.readUuid();
            this.recipeId = buf.readString();
        }

        // Updated write method for RegistryByteBuf
        public void write(RegistryByteBuf buf) { // Removed @Override
            buf.writeUuid(villagerUuid);
            buf.writeString(recipeId);
        }

        // Implement required getId method
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public UUID getVillagerUuid() {
            return villagerUuid;
        }
        
        public String getRecipeId() {
            return recipeId;
        }
    }

    public static class RecipeListPayload implements CustomPayload {
        public static final CustomPayload.Id<RecipeListPayload> ID = new CustomPayload.Id<>(RECIPE_LIST_PACKET_ID);

        private final UUID villagerUuid;
        private final List<String> recipeIds;

        public RecipeListPayload(UUID villagerUuid, List<String> recipeIds) {
            this.villagerUuid = villagerUuid;
            this.recipeIds = recipeIds;
        }

        // Updated constructor for RegistryByteBuf
        public RecipeListPayload(RegistryByteBuf buf) {
            this.villagerUuid = buf.readUuid();
            int size = buf.readInt();
            this.recipeIds = new java.util.ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                this.recipeIds.add(buf.readString());
            }
        }

        // Updated write method for RegistryByteBuf
        public void write(RegistryByteBuf buf) { // Removed @Override
            buf.writeUuid(villagerUuid);
            buf.writeInt(recipeIds.size());
            for (String recipeId : recipeIds) {
                buf.writeString(recipeId);
            }
        }

        // Implement required getId method
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public UUID getVillagerUuid() {
            return villagerUuid;
        }
        
        public List<String> getRecipeIds() {
            return recipeIds;
        }
    }

    public static class CraftStatusPayload implements CustomPayload {
        public static final CustomPayload.Id<CraftStatusPayload> ID = new CustomPayload.Id<>(CRAFT_STATUS_PACKET_ID);

        private final UUID villagerUuid;
        private final String recipeId;
        private final String status;
        private final String message;

        public CraftStatusPayload(UUID villagerUuid, String recipeId, String status, String message) {
            this.villagerUuid = villagerUuid;
            this.recipeId = recipeId;
            this.status = status;
            this.message = message;
        }

        // Updated constructor for RegistryByteBuf
        public CraftStatusPayload(RegistryByteBuf buf) {
            this.villagerUuid = buf.readUuid();
            this.recipeId = buf.readString();
            this.status = buf.readString();
            this.message = buf.readString();
        }

        // Updated write method for RegistryByteBuf
        public void write(RegistryByteBuf buf) { // Removed @Override
            buf.writeUuid(villagerUuid);
            buf.writeString(recipeId);
            buf.writeString(status);
            buf.writeString(message);
        }

        // Implement required getId method
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public UUID getVillagerUuid() {
            return villagerUuid;
        }
        
        public String getRecipeId() {
            return recipeId;
        }
        
        public String getStatus() {
            return status;
        }
        
        public String getMessage() {
            return message;
        }
    }

    public static class CraftProgressPayload implements CustomPayload {
        public static final CustomPayload.Id<CraftProgressPayload> ID = new CustomPayload.Id<>(CRAFT_PROGRESS_PACKET_ID);

        private final UUID villagerUuid;
        private final String recipeId;
        private final int progress;
        private final int maxProgress;

        public CraftProgressPayload(UUID villagerUuid, String recipeId, int progress, int maxProgress) {
            this.villagerUuid = villagerUuid;
            this.recipeId = recipeId;
            this.progress = progress;
            this.maxProgress = maxProgress;
        }

        // Updated constructor for RegistryByteBuf
        public CraftProgressPayload(RegistryByteBuf buf) {
            this.villagerUuid = buf.readUuid();
            this.recipeId = buf.readString();
            this.progress = buf.readInt();
            this.maxProgress = buf.readInt();
        }

        // Updated write method for RegistryByteBuf
        public void write(RegistryByteBuf buf) { // Removed @Override
            buf.writeUuid(villagerUuid);
            buf.writeString(recipeId);
            buf.writeInt(progress);
            buf.writeInt(maxProgress);
        }

        // Implement required getId method
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public UUID getVillagerUuid() {
            return villagerUuid;
        }
        
        public String getRecipeId() {
            return recipeId;
        }
        
        public int getProgress() {
            return progress;
        }
        
        public int getMaxProgress() {
            return maxProgress;
        }
    }

    public static class CraftCompletePayload implements CustomPayload {
        public static final CustomPayload.Id<CraftCompletePayload> ID = new CustomPayload.Id<>(CRAFT_COMPLETE_PACKET_ID);

        private final UUID villagerUuid;
        private final String recipeId;
        private final boolean success;
        private final ItemStack result;

        public CraftCompletePayload(UUID villagerUuid, String recipeId, boolean success, ItemStack result) {
            this.villagerUuid = villagerUuid;
            this.recipeId = recipeId;
            this.success = success;
            this.result = result != null ? result : ItemStack.EMPTY;
        }

        // Updated constructor for RegistryByteBuf and ItemStack codec
        public CraftCompletePayload(RegistryByteBuf buf) {
            this.villagerUuid = buf.readUuid();
            this.recipeId = buf.readString();
            this.success = buf.readBoolean();
            this.result = success ? ItemStack.PACKET_CODEC.decode(buf) : ItemStack.EMPTY;
        }

        // Updated write method for RegistryByteBuf and ItemStack codec
        public void write(RegistryByteBuf buf) { // Removed @Override
            buf.writeUuid(villagerUuid);
            buf.writeString(recipeId);
            buf.writeBoolean(success);

            if (success) {
                ItemStack.PACKET_CODEC.encode(buf, result);
            }
        }

        // Implement required getId method
        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public UUID getVillagerUuid() {
            return villagerUuid;
        }
        
        public String getRecipeId() {
            return recipeId;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public ItemStack getResult() {
            return result;
        }
    }

    /**
     * Register all network handlers
     */
    public static void register() {
        LOGGER.info("Registering VillageCraftingNetwork packet handlers");
        registerServerHandlers();
        LOGGER.info("VillageCraftingNetwork packet handlers registered successfully");
    }
    
    /**
     * Register server-side network handlers
     */
    public static void registerServerHandlers() {
        // Register server-side packet receivers using the new 1.21.4 API
        ServerPlayNetworking.registerGlobalReceiver(CRAFT_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            CraftRecipePayload payload = new CraftRecipePayload(buf);
            server.execute(() -> {
                handleCraftRequest(payload.getVillagerUuid(), payload.getRecipeId(), player);
            });
        });
        
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_RECIPES_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            RequestRecipesPayload payload = new RequestRecipesPayload(buf);
            server.execute(() -> {
                handleRecipeListRequest(payload.getVillagerUuid(), player);
            });
        });
        
        ServerPlayNetworking.registerGlobalReceiver(CANCEL_CRAFT_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            CancelCraftPayload payload = new CancelCraftPayload(buf);
            server.execute(() -> {
                handleCancelCraftRequest(payload.getVillagerUuid(), payload.getRecipeId(), player);
            });
        });
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
    private static void handleRecipeListRequest(UUID villagerUuid, ServerPlayerEntity player) {
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
                try {
                    List<String> recipeIds = VillageCraftingManager.getInstance().getAvailableRecipes(
                        villager, player, culture);
                    
                    // Send the recipe list back to the client
                    RecipeListPayload payload = new RecipeListPayload(villagerUuid, recipeIds);
                    ServerPlayNetworking.send(player, PacketByteBufs.create().writeCustomPayload(payload));
                } catch (Exception e) {
                    LOGGER.error("Error getting available recipes", e);
                    // Send an empty list if there's an error
                    RecipeListPayload payload = new RecipeListPayload(villagerUuid, java.util.Collections.emptyList());
                    ServerPlayNetworking.send(player, PacketByteBufs.create().writeCustomPayload(payload));
                }
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
        CraftStatusPayload payload = new CraftStatusPayload(villagerUuid, recipeId, status, message);
        ServerPlayNetworking.send(player, PacketByteBufs.create().writeCustomPayload(payload));
    }

    /**
     * Send a craft progress update to a player
     */
    public static void sendCraftProgressToPlayer(ServerPlayerEntity player, UUID villagerUuid,
                                               String recipeId, int progress, int maxProgress) {
        CraftProgressPayload payload = new CraftProgressPayload(villagerUuid, recipeId, progress, maxProgress);
        ServerPlayNetworking.send(player, PacketByteBufs.create().writeCustomPayload(payload));
    }

    /**
     * Send a craft completion notification to a player
     */
    public static void sendCraftCompleteToPlayer(ServerPlayerEntity player, UUID villagerUuid,
                                               String recipeId, boolean success, ItemStack result) {
        CraftCompletePayload payload = new CraftCompletePayload(villagerUuid, recipeId, success, result);
        ServerPlayNetworking.send(player, PacketByteBufs.create().writeCustomPayload(payload));
    }

    /**
     * Check if a player is within reach of a villager
     */
    private static boolean isWithinReach(ServerPlayerEntity player, VillagerEntity villager) {
        double distanceSquared = player.squaredDistanceTo(villager);
        return distanceSquared <= 64.0; // 8 blocks squared
    }
}
