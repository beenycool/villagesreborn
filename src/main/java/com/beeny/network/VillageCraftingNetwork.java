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
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;
import net.minecraft.network.codec.PacketCodec;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.network.RegistryByteBuf;

/**
 * Handles network communication for the village crafting system.
 * This includes crafting requests, recipe list requests, and crafting status updates.
 * Updated for Minecraft 1.21.4 networking API
 */
public class VillageCraftingNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillageCraftingNetwork");
    private static final String MOD_ID = "villagesreborn";
    
    // Packet identifiers - updated to use Identifier.of()
    public static final Identifier CRAFT_PACKET_ID = Identifier.of(MOD_ID, "craft_recipe");
    public static final Identifier REQUEST_RECIPES_PACKET_ID = Identifier.of(MOD_ID, "request_recipes");
    public static final Identifier RECIPE_LIST_PACKET_ID = Identifier.of(MOD_ID, "recipe_list");
    public static final Identifier CRAFT_STATUS_PACKET_ID = Identifier.of(MOD_ID, "craft_status");
    public static final Identifier CANCEL_CRAFT_PACKET_ID = Identifier.of(MOD_ID, "cancel_craft");
    public static final Identifier CRAFT_PROGRESS_PACKET_ID = Identifier.of(MOD_ID, "craft_progress");
    public static final Identifier CRAFT_COMPLETE_PACKET_ID = Identifier.of(MOD_ID, "craft_complete");

    // Custom payload IDs for 1.21.4
    public static final CustomPayload.Id<CraftRecipePayload> CRAFT_RECIPE_ID = new CustomPayload.Id<>(CRAFT_PACKET_ID);
    public static final PacketCodec<PacketByteBuf, CraftRecipePayload> CRAFT_RECIPE_CODEC =
        PacketCodec.of(CraftRecipePayload::write, CraftRecipePayload::new); // Uses PacketByteBuf, might be okay if no registry items involved directly

    public static final CustomPayload.Id<RequestRecipesPayload> REQUEST_RECIPES_ID = new CustomPayload.Id<>(REQUEST_RECIPES_PACKET_ID);
    public static final PacketCodec<PacketByteBuf, RequestRecipesPayload> REQUEST_RECIPES_CODEC =
        PacketCodec.of(RequestRecipesPayload::write, RequestRecipesPayload::new); // Uses PacketByteBuf
    
    public static final CustomPayload.Id<CancelCraftPayload> CANCEL_CRAFT_ID = new CustomPayload.Id<>(CANCEL_CRAFT_PACKET_ID);
    public static final PacketCodec<PacketByteBuf, CancelCraftPayload> CANCEL_CRAFT_CODEC =
        PacketCodec.of(CancelCraftPayload::write, CancelCraftPayload::new); // Uses PacketByteBuf
    
    public static final CustomPayload.Id<RecipeListPayload> RECIPE_LIST_ID = new CustomPayload.Id<>(RECIPE_LIST_PACKET_ID);
    public static final PacketCodec<PacketByteBuf, RecipeListPayload> RECIPE_LIST_CODEC =
        PacketCodec.of(RecipeListPayload::write, RecipeListPayload::new); // Uses PacketByteBuf
    
    public static final CustomPayload.Id<CraftStatusPayload> CRAFT_STATUS_ID = new CustomPayload.Id<>(CRAFT_STATUS_PACKET_ID);
    public static final PacketCodec<PacketByteBuf, CraftStatusPayload> CRAFT_STATUS_CODEC =
        PacketCodec.of(CraftStatusPayload::write, CraftStatusPayload::new); // Uses PacketByteBuf
    
    public static final CustomPayload.Id<CraftProgressPayload> CRAFT_PROGRESS_ID = new CustomPayload.Id<>(CRAFT_PROGRESS_PACKET_ID);
    public static final PacketCodec<PacketByteBuf, CraftProgressPayload> CRAFT_PROGRESS_CODEC =
        PacketCodec.of(CraftProgressPayload::write, CraftProgressPayload::new); // Uses PacketByteBuf
    
    public static final CustomPayload.Id<CraftCompletePayload> CRAFT_COMPLETE_ID = new CustomPayload.Id<>(CRAFT_COMPLETE_PACKET_ID);
    // Needs RegistryByteBuf because it handles ItemStack
    public static final PacketCodec<RegistryByteBuf, CraftCompletePayload> CRAFT_COMPLETE_CODEC =
        PacketCodec.of(CraftCompletePayload::write, CraftCompletePayload::new);

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

        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeString(recipeId);
        }

        @Override
        public CustomPayload.Id<?> getId() {
            return CRAFT_RECIPE_ID;
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

        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
        }

        @Override
        public CustomPayload.Id<?> getId() {
            return REQUEST_RECIPES_ID;
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

        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeString(recipeId);
        }

        @Override
        public CustomPayload.Id<?> getId() {
            return CANCEL_CRAFT_ID;
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
            this.recipeIds = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                this.recipeIds.add(buf.readString());
            }
        }

        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeInt(recipeIds.size());
            for (String recipeId : recipeIds) {
                buf.writeString(recipeId);
            }
        }

        @Override
        public CustomPayload.Id<?> getId() {
            return RECIPE_LIST_ID;
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

        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeString(recipeId);
            buf.writeString(status);
            buf.writeString(message);
        }

        @Override
        public CustomPayload.Id<?> getId() {
            return CRAFT_STATUS_ID;
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

        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeString(recipeId);
            buf.writeInt(progress);
            buf.writeInt(maxProgress);
        }

        @Override
        public CustomPayload.Id<?> getId() {
            return CRAFT_PROGRESS_ID;
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

        // Constructor now takes RegistryByteBuf
        public CraftCompletePayload(RegistryByteBuf buf) {
            this.villagerUuid = buf.readUuid();
            this.recipeId = buf.readString();
            this.success = buf.readBoolean();
            // Decode ItemStack using the codec and the registry lookup from the buffer
            this.result = ItemStack.PACKET_CODEC.decode(buf);
        }

        // Write method now takes RegistryByteBuf
        public void write(RegistryByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeString(recipeId);
            buf.writeBoolean(success);
            // Encode ItemStack using the codec and the registry lookup from the buffer
            ItemStack.PACKET_CODEC.encode(buf, result);
        }

        @Override
        public CustomPayload.Id<?> getId() {
            return CRAFT_COMPLETE_ID;
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
        // Updated registration to use ID and correct context type
        ServerPlayNetworking.registerGlobalReceiver(CRAFT_RECIPE_ID,
            (payload, context) -> {
                ServerPlayerEntity player = context.player();
                // No need for separate server.execute, context handles it
                VillagerEntity villager = (VillagerEntity) player.getServerWorld()
                    .getEntity(payload.getVillagerUuid());
                if (villager != null) {
                    VillageCraftingManager.getInstance()
                        .assignTask(villager, payload.getRecipeId(), player);
                } else {
                    LOGGER.warn("Crafting request received for unknown villager {}",
                        payload.getVillagerUuid());
                }
            });

        // Updated registration to use ID and correct context type
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_RECIPES_ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            VillagerEntity villager = (VillagerEntity) player.getServerWorld().getEntity(payload.getVillagerUuid());
            if (villager != null) {
                // Pass player directly instead of responseSender if needed by handleRecipeRequest
                handleRecipeRequest(villager, player);
            }
        });

        // Updated registration to use ID and correct context type
        ServerPlayNetworking.registerGlobalReceiver(CANCEL_CRAFT_ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            VillagerEntity villager = (VillagerEntity) player.getServerWorld().getEntity(payload.getVillagerUuid());
            if (villager != null) {
                VillageCraftingManager.getInstance().cancelTask(villager, payload.getRecipeId());
            }
        });
    }

    // Updated handleRecipeRequest signature (removed PacketSender if not needed)
    private static void handleRecipeRequest(VillagerEntity villager, ServerPlayerEntity player) {
        VillagerManager vm = VillagerManager.getInstance();
        SpawnRegion region = vm.getNearestSpawnRegion(villager.getBlockPos());
        String culture = region != null ? region.getCultureAsString() : "default";

        List<String> recipeIds = VillageCraftingManager.getInstance()
            .getRecipesForCulture(culture).stream()
            .map(VillageCraftingManager.CraftingRecipe::getId)
            .collect(Collectors.toList());

        RecipeListPayload payload = new RecipeListPayload(villager.getUuid(), recipeIds);
        // Send payload using the ID - Assuming RecipeListPayload is S2C
        ServerPlayNetworking.send(player, payload); // Sending S2C requires the payload object directly now
    }
    
    // Packet type registration should happen centrally in Villagesreborn.onInitialize
}
