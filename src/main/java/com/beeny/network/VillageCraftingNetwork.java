package com.beeny.network;

import com.beeny.village.VillageCraftingManager;
import com.beeny.village.SpawnRegion;
import com.beeny.village.VillagerManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
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

    // Custom payload IDs for 1.21.4
    public static final CustomPayload.Id<CraftRecipePayload> CRAFT_RECIPE_PAYLOAD_ID = 
        CustomPayload.id("villagesreborn:craft_recipe");
    
    public static final CustomPayload.Id<RequestRecipesPayload> REQUEST_RECIPES_PAYLOAD_ID = 
        CustomPayload.id("villagesreborn:request_recipes");
    
    public static final CustomPayload.Id<CancelCraftPayload> CANCEL_CRAFT_PAYLOAD_ID = 
        CustomPayload.id("villagesreborn:cancel_craft");
    
    public static final CustomPayload.Id<RecipeListPayload> RECIPE_LIST_PAYLOAD_ID = 
        CustomPayload.id("villagesreborn:recipe_list");
    
    public static final CustomPayload.Id<CraftStatusPayload> CRAFT_STATUS_PAYLOAD_ID = 
        CustomPayload.id("villagesreborn:craft_status");
    
    public static final CustomPayload.Id<CraftProgressPayload> CRAFT_PROGRESS_PAYLOAD_ID = 
        CustomPayload.id("villagesreborn:craft_progress");
    
    public static final CustomPayload.Id<CraftCompletePayload> CRAFT_COMPLETE_PAYLOAD_ID = 
        CustomPayload.id("villagesreborn:craft_complete");

    // Custom payload classes for 1.21.4
    public static class CraftRecipePayload implements CustomPayload {
        private final UUID villagerUuid;
        private final String recipeId;

        public CraftRecipePayload(UUID villagerUuid, String recipeId) {
            this.villagerUuid = villagerUuid;
            this.recipeId = recipeId;
        }

        public CraftRecipePayload(PacketByteBuf buf) {
            this.villagerUuid = buf.readUuid();
            this.recipeId = buf.readString();
        }

        @Override
        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeString(recipeId);
        }

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return CRAFT_RECIPE_PAYLOAD_ID;
        }

        public UUID getVillagerUuid() {
            return villagerUuid;
        }
        
        public String getRecipeId() {
            return recipeId;
        }
    }

    public static class RequestRecipesPayload implements CustomPayload {
        private final UUID villagerUuid;

        public RequestRecipesPayload(UUID villagerUuid) {
            this.villagerUuid = villagerUuid;
        }

        public RequestRecipesPayload(PacketByteBuf buf) {
            this.villagerUuid = buf.readUuid();
        }

        @Override
        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
        }

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return REQUEST_RECIPES_PAYLOAD_ID;
        }

        public UUID getVillagerUuid() {
            return villagerUuid;
        }
    }

    public static class CancelCraftPayload implements CustomPayload {
        private final UUID villagerUuid;
        private final String recipeId;

        public CancelCraftPayload(UUID villagerUuid, String recipeId) {
            this.villagerUuid = villagerUuid;
            this.recipeId = recipeId;
        }

        public CancelCraftPayload(PacketByteBuf buf) {
            this.villagerUuid = buf.readUuid();
            this.recipeId = buf.readString();
        }

        @Override
        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeString(recipeId);
        }

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return CANCEL_CRAFT_PAYLOAD_ID;
        }

        public UUID getVillagerUuid() {
            return villagerUuid;
        }
        
        public String getRecipeId() {
            return recipeId;
        }
    }

    public static class RecipeListPayload implements CustomPayload {
        private final UUID villagerUuid;
        private final List<String> recipeIds;

        public RecipeListPayload(UUID villagerUuid, List<String> recipeIds) {
            this.villagerUuid = villagerUuid;
            this.recipeIds = recipeIds;
        }

        public RecipeListPayload(PacketByteBuf buf) {
            this.villagerUuid = buf.readUuid();
            int size = buf.readInt();
            this.recipeIds = new java.util.ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                this.recipeIds.add(buf.readString());
            }
        }

        @Override
        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeInt(recipeIds.size());
            for (String recipeId : recipeIds) {
                buf.writeString(recipeId);
            }
        }

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return RECIPE_LIST_PAYLOAD_ID;
        }

        public UUID getVillagerUuid() {
            return villagerUuid;
        }
        
        public List<String> getRecipeIds() {
            return recipeIds;
        }
    }

    public static class CraftStatusPayload implements CustomPayload {
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

        public CraftStatusPayload(PacketByteBuf buf) {
            this.villagerUuid = buf.readUuid();
            this.recipeId = buf.readString();
            this.status = buf.readString();
            this.message = buf.readString();
        }

        @Override
        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeString(recipeId);
            buf.writeString(status);
            buf.writeString(message);
        }

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return CRAFT_STATUS_PAYLOAD_ID;
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

        public CraftProgressPayload(PacketByteBuf buf) {
            this.villagerUuid = buf.readUuid();
            this.recipeId = buf.readString();
            this.progress = buf.readInt();
            this.maxProgress = buf.readInt();
        }

        @Override
        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeString(recipeId);
            buf.writeInt(progress);
            buf.writeInt(maxProgress);
        }

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return CRAFT_PROGRESS_PAYLOAD_ID;
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

        public CraftCompletePayload(PacketByteBuf buf) {
            this.villagerUuid = buf.readUuid();
            this.recipeId = buf.readString();
            this.success = buf.readBoolean();
            this.result = buf.readItemStack();
        }

        @Override
        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeString(recipeId);
            buf.writeBoolean(success);
            buf.writeItemStack(result);
        }

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return CRAFT_COMPLETE_PAYLOAD_ID;
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

    public static void registerServerHandlers() {
        ServerPlayNetworking.registerGlobalReceiver(CRAFT_RECIPE_PAYLOAD_ID,
            (payload, context) -> {
                ServerPlayerEntity player = context.player();
                MinecraftServer server = player.getServer();
                server.execute(() -> {
                    // Find villager entity from payload.villagerUuid()
                    VillagerEntity villager = (VillagerEntity) player.getServerWorld().getEntity(payload.villagerUuid());
                    if (villager != null) {
                        VillageCraftingManager.getInstance().assignTask(villager, payload.recipeId(), player);
                        // Handle potential future/result? Maybe assignTask returns void and uses callbacks/packets?
                        // Based on current code, assignTask seems to handle sending results back.
                    } else {
                        LOGGER.warn("Crafting request received for unknown villager {}", payload.villagerUuid());
                    }
                });
            });
        // Register other server-side handlers (REQUEST_RECIPES, CANCEL_CRAFT) similarly
    }
}
