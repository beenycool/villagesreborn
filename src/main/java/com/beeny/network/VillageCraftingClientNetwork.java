package com.beeny.network;

import com.beeny.village.VillageCraftingManager;
import com.beeny.village.VillageCraftingManager.CraftingRecipe;
import com.beeny.network.VillageCraftingClientHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodec;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.beeny.Villagesreborn.MOD_ID;

/**
 * Client-side implementation of the VillageCrafting network system
 */
public class VillageCraftingClientNetwork {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillageCraftingClientNetwork");

    /**
     * Register client-side packet handlers
     */
    public static void register() {
        LOGGER.info("Registering VillageCraftingClientNetwork handlers");
        registerClientHandlers();
        LOGGER.info("VillageCraftingClientNetwork handlers registered successfully");
    }

    public static final CustomPayload.Id<RecipeListPayload> RECIPE_LIST_PAYLOAD_ID = 
        new CustomPayload.Id<>(Identifier.of(MOD_ID, "recipe_list"));
    public static final CustomPayload.Id<CraftStatusPayload> CRAFT_STATUS_PAYLOAD_ID = 
        new CustomPayload.Id<>(Identifier.of(MOD_ID, "craft_status"));

    public static class RecipeListPayload implements CustomPayload {
        public static final CustomPayload.Id<RecipeListPayload> ID = RECIPE_LIST_PAYLOAD_ID;

        private final List<CraftingRecipe> recipes;

        public RecipeListPayload(List<CraftingRecipe> recipes) {
            this.recipes = recipes;
        }

        public RecipeListPayload(PacketByteBuf buf) {
            int size = buf.readInt();
            this.recipes = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                this.recipes.add(new CraftingRecipe(buf));
            }
        }

        public void write(PacketByteBuf buf) {
            buf.writeInt(recipes.size());
            for (CraftingRecipe recipe : recipes) {
                recipe.write(buf);
            }
        }

        @Override
        public CustomPayload.Id<?> getId() { // Renamed from getType
            return ID;
        }
    }

    public static class CraftStatusPayload implements CustomPayload {
        public static final CustomPayload.Id<CraftStatusPayload> ID = CRAFT_STATUS_PAYLOAD_ID;

        private final UUID villagerUuid;
        private final String recipeId;
        private final String status; // Changed from boolean crafting
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
            this.status = buf.readString(); // Changed from readBoolean
            this.message = buf.readString();
        }

        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeString(recipeId);
            buf.writeString(status); // Changed from writeBoolean
            buf.writeString(message);
        }

        @Override
        public CustomPayload.Id<?> getId() { // Renamed from getType
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

    /**
     * Register client-side packet handlers
     */
    private static void registerClientHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(
            RECIPE_LIST_PAYLOAD_ID,
            (payload, context) -> { // No cast needed, payload is already RecipeListPayload
                MinecraftClient client = context.client(); // Use context.client()
                // Extract recipe IDs from the list of CraftingRecipe objects
                List<String> recipeIds = payload.recipes.stream()
                                                        .map(CraftingRecipe::getId)
                                                        .toList();
                // Context handles execution on client thread
                // Need villager UUID - RecipeListPayload doesn't seem to carry it.
                // Assuming the handler needs adjustment or the payload needs the UUID.
                // For now, passing null for UUID as it's missing from payload.
                // TODO: Revisit payload design if UUID is required here.
                VillageCraftingClientHandler.updateAvailableRecipes(null, recipeIds);
            }
        );

        ClientPlayNetworking.registerGlobalReceiver(
            CRAFT_STATUS_PAYLOAD_ID,
            (payload, context) -> { // No cast needed, payload is already CraftStatusPayload
                MinecraftClient client = context.client(); // Use context.client()
                // Context handles execution on client thread
                VillageCraftingClientHandler.updateCraftingStatus(
                    payload.getVillagerUuid(),
                    payload.getRecipeId(),
                    payload.getStatus(),
                    payload.getMessage()
                );
            }
        );
    }

    /**
     * Send a request to the server to get available recipes for a villager
     */
    public static void sendRecipeListRequest(UUID villagerUuid) {
        LOGGER.debug("Sending recipe list request for villager {}", villagerUuid);
        VillageCraftingNetwork.RequestRecipesPayload payload =
            new VillageCraftingNetwork.RequestRecipesPayload(villagerUuid);
        ClientPlayNetworking.send(payload);
    }

    /**
     * Send a request to the server to craft a recipe
     */
    public static void sendCraftingRequest(UUID villagerUuid, String recipeId) {
        LOGGER.debug("Sending crafting request for villager {} recipe {}", villagerUuid, recipeId);
        VillageCraftingNetwork.CraftRecipePayload payload =
            new VillageCraftingNetwork.CraftRecipePayload(villagerUuid, recipeId);
        ClientPlayNetworking.send(payload);
    }

    /**
     * Send a request to the server to cancel crafting
     */
    public static void sendCancelCraftingRequest(UUID villagerUuid, String recipeId) {
        LOGGER.debug("Sending cancel craft request for villager {} recipe {}", villagerUuid, recipeId);
        VillageCraftingNetwork.CancelCraftPayload payload =
            new VillageCraftingNetwork.CancelCraftPayload(villagerUuid, recipeId);
        ClientPlayNetworking.send(payload);
    }


    // Register S2C codecs
    static {
        PayloadTypeRegistry.playS2C().register(RecipeListPayload.ID, PacketCodec.of(RecipeListPayload::write, RecipeListPayload::new));
        PayloadTypeRegistry.playS2C().register(CraftStatusPayload.ID, PacketCodec.of(CraftStatusPayload::write, CraftStatusPayload::new));
        // Add registration for other S2C packets handled here if any (e.g., CraftProgress, CraftComplete)
        // Assuming these IDs and Codecs are defined in VillageCraftingNetwork
        PayloadTypeRegistry.playS2C().register(VillageCraftingNetwork.CRAFT_PROGRESS_ID, VillageCraftingNetwork.CRAFT_PROGRESS_CODEC);
        PayloadTypeRegistry.playS2C().register(VillageCraftingNetwork.CRAFT_COMPLETE_ID, VillageCraftingNetwork.CRAFT_COMPLETE_CODEC);
    }


}
