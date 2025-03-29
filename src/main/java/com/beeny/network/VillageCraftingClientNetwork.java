package com.beeny.network;

import com.beeny.village.VillageCraftingManager;
import com.beeny.network.VillageCraftingClientHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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

    public static final CustomPayload.Id<RecipeListPayload> RECIPE_LIST_PAYLOAD_ID = CustomPayload.id(Identifier.of(MOD_ID, "recipe_list"));
    public static final CustomPayload.Id<CraftStatusPayload> CRAFT_STATUS_PAYLOAD_ID = CustomPayload.id(Identifier.of(MOD_ID, "craft_status"));

    public static class RecipeListPayload implements CustomPayload {
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

        @Override
        public void write(PacketByteBuf buf) {
            buf.writeInt(recipes.size());
            for (CraftingRecipe recipe : recipes) {
                recipe.write(buf);
            }
        }
    }

    public static class CraftStatusPayload implements CustomPayload {
        private final UUID villagerUuid;
        private final String recipeId;
        private final boolean crafting;
        private final String message;

        public CraftStatusPayload(UUID villagerUuid, String recipeId, boolean crafting, String message) {
            this.villagerUuid = villagerUuid;
            this.recipeId = recipeId;
            this.crafting = crafting;
            this.message = message;
        }

        public CraftStatusPayload(PacketByteBuf buf) {
            this.villagerUuid = buf.readUuid();
            this.recipeId = buf.readString();
            this.crafting = buf.readBoolean();
            this.message = buf.readString();
        }

        @Override
        public void write(PacketByteBuf buf) {
            buf.writeUuid(villagerUuid);
            buf.writeString(recipeId);
            buf.writeBoolean(crafting);
            buf.writeString(message);
        }
    }

    /**
     * Register client-side packet handlers
     */
    private static void registerClientHandlers() {
        // Register client-side packet receivers using the updated API for 1.21.4
        ClientPlayNetworking.registerGlobalReceiver(
            RECIPE_LIST_PAYLOAD_ID,
            (payload, context) -> {
                MinecraftClient client = context.client();
                client.execute(() -> {
                    RecipeListPayload recipeListPayload = (RecipeListPayload) payload;
                    VillageCraftingClientHandler.updateAvailableRecipes(
                        recipeListPayload.recipes.get(0).getVillagerUuid(),
                        null
                    );
                });
            }
        );

        ClientPlayNetworking.registerGlobalReceiver(
            CRAFT_STATUS_PAYLOAD_ID,
            (payload, context) -> {
                MinecraftClient client = context.client();
                CraftStatusPayload craftStatusPayload = (CraftStatusPayload) payload;
                client.execute(() -> {
                    VillageCraftingClientHandler.updateCraftingStatus(
                        craftStatusPayload.villagerUuid,
                        craftStatusPayload.recipeId,
                        craftStatusPayload.crafting,
                        craftStatusPayload.message
                    );
                });
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
}
