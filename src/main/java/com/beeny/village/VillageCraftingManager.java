package com.beeny.village;

import com.beeny.ai.LLMService;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.entity.passive.VillagerEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VillageCraftingManager {
    private static VillageCraftingManager instance;
    private final Map<String, List<CraftingRecipe>> culturalRecipes = new HashMap<>();
    private final VillagerManager villagerManager;

    private VillageCraftingManager() {
        this.villagerManager = VillagerManager.getInstance();
        initializeRecipes();
    }

    public static VillageCraftingManager getInstance() {
        if (instance == null) {
            instance = new VillageCraftingManager();
        }
        return instance;
    }

    private void initializeRecipes() {
        culturalRecipes.put("roman", List.of(
            new CraftingRecipe("legionary_sword", Items.IRON_SWORD, Map.of(Items.IRON_INGOT, 3)),
            new CraftingRecipe("legion_shield", Items.SHIELD, Map.of(Items.IRON_INGOT, 1, Items.OAK_PLANKS, 6))
        ));

        culturalRecipes.put("egyptian", List.of(
            new CraftingRecipe("desert_potion", Items.POTION, Map.of(Items.GLASS_BOTTLE, 1, Items.CACTUS, 2)),
            new CraftingRecipe("sandstone_tools", Items.STONE_PICKAXE, Map.of(Items.SANDSTONE, 3, Items.STICK, 2))
        ));

        culturalRecipes.put("victorian", List.of(
            new CraftingRecipe("tea_set", Items.BOWL, Map.of(Items.CLAY_BALL, 4)),
            new CraftingRecipe("fancy_clock", Items.CLOCK, Map.of(Items.GOLD_INGOT, 4, Items.REDSTONE, 1))
        ));

        culturalRecipes.put("nyc", List.of(
            new CraftingRecipe("hot_dog", Items.COOKED_BEEF, Map.of(Items.BEEF, 1, Items.BREAD, 1)),
            new CraftingRecipe("coffee", Items.MUSHROOM_STEW, Map.of(Items.COCOA_BEANS, 2, Items.MILK_BUCKET, 1))
        ));
    }

    public List<CraftingRecipe> getRecipesForCulture(String culture) {
        return culturalRecipes.getOrDefault(culture, List.of());
    }

    public CompletableFuture<String> assignTask(VillagerEntity villager, String recipeId, ServerPlayerEntity player) {
        VillagerAI ai = villagerManager.getVillagerAI(villager.getUuid());
        VillagerManager vm = VillagerManager.getInstance();
        SpawnRegion region = vm.getNearestSpawnRegion(villager.getBlockPos());
        String culture = region != null ? region.getCulture() : "unknown";
        CraftingRecipe recipe = findRecipe(culture, recipeId);

        if (recipe == null) {
            return CompletableFuture.completedFuture("Unknown recipe");
        }

        String prompt = String.format(
            "Villager %s (%s) is tasked with crafting %s in a %s village. Describe their reaction and work process in 15 words or less.",
            villager.getName().getString(),
            ai.getPersonality(),
            recipeId,
            culture
        );

        return LLMService.getInstance().generateResponse(prompt)
            .thenApply(response -> {
                ai.updateActivity("crafting_" + recipeId);
                ai.updateProfessionSkill("crafting");
                player.sendMessage(Text.literal(response), false);
                return response;
            });
    }

    public static class CraftingRecipe {
        private final String id;
        private final Item output;
        private final Map<Item, Integer> inputs;

        public CraftingRecipe(String id, Item output, Map<Item, Integer> inputs) {
            this.id = id;
            this.output = output;
            this.inputs = inputs;
        }

        public String getId() { return id; }
        public Item getOutput() { return output; }
        public Map<Item, Integer> getInputs() { return inputs; }
    }

    private CraftingRecipe findRecipe(String culture, String recipeId) {
        List<CraftingRecipe> recipes = culturalRecipes.get(culture);
        if (recipes == null) return null;
        return recipes.stream()
            .filter(r -> r.getId().equals(recipeId))
            .findFirst()
            .orElse(null);
    }

    private float calculateSkillGain(CraftingRecipe recipe) {
        return 0.1f * recipe.getInputs().size();
    }
}