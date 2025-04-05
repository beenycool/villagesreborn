package com.beeny.village;

import com.beeny.ai.LLMService;
import com.beeny.village.util.DataComponentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.component.DataComponentTypes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillageCraftingManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillageCraftingManager");
    private static VillageCraftingManager instance;
    private final Map<String, List<CraftingRecipe>> culturalRecipes = new HashMap<>();
    private final LLMService llmService;
    private List<CraftingRecipe> recipes = new ArrayList<>();
    
    private VillageCraftingManager() {
        this.llmService = LLMService.getInstance();
        initializeCulturalRecipes();
    }
    
    public static VillageCraftingManager getInstance() {
        if (instance == null) {
            instance = new VillageCraftingManager();
        }
        return instance;
    }
    
    private void initializeCulturalRecipes() {
        // Initialize basic recipes for each culture
        culturalRecipes.put("roman", Arrays.asList(
            new CraftingRecipe("legionary_sword", Items.IRON_SWORD, Map.of(Items.IRON_INGOT, 3)),
            new CraftingRecipe("legion_shield", Items.SHIELD, Map.of(Items.IRON_INGOT, 1, Items.OAK_PLANKS, 6))
        ));

        culturalRecipes.put("egyptian", Arrays.asList(
            new CraftingRecipe("desert_potion", Items.POTION, Map.of(Items.GLASS_BOTTLE, 1, Items.CACTUS, 2)),
            new CraftingRecipe("sandstone_tools", Items.STONE_PICKAXE, Map.of(Items.SANDSTONE, 3, Items.STICK, 2))
        ));

        culturalRecipes.put("victorian", Arrays.asList(
            new CraftingRecipe("tea_set", Items.BOWL, Map.of(Items.CLAY_BALL, 4)),
            new CraftingRecipe("fancy_clock", Items.CLOCK, Map.of(Items.GOLD_INGOT, 4, Items.REDSTONE, 1))
        ));
    }

    public boolean canCraft(ServerPlayerEntity player, String recipeId, String culture) {
        List<CraftingRecipe> recipes = culturalRecipes.get(culture);
        if (recipes == null) return false;
        
        Optional<CraftingRecipe> recipe = recipes.stream()
            .filter(r -> r.getId().equals(recipeId))
            .findFirst();
            
        if (recipe.isEmpty()) return false;
        
        // Check if player has required materials
        return recipe.get().hasRequiredMaterials(player);
    }

    public ItemStack craft(ServerPlayerEntity player, String recipeId, String culture) {
        List<CraftingRecipe> recipes = culturalRecipes.get(culture);
        if (recipes == null) return ItemStack.EMPTY;
        
        Optional<CraftingRecipe> recipe = recipes.stream()
            .filter(r -> r.getId().equals(recipeId))
            .findFirst();
            
        if (recipe.isEmpty()) return ItemStack.EMPTY;
        
        CraftingRecipe selectedRecipe = recipe.get();
        if (!selectedRecipe.hasRequiredMaterials(player)) return ItemStack.EMPTY;
        
        // Consume materials
        selectedRecipe.consumeMaterials(player);
        
        // Create result item
        ItemStack result = new ItemStack(selectedRecipe.getResult());
        
        // Add cultural crafting data
        Map<String, Object> customData = new HashMap<>();
        customData.put("culture", culture);
        customData.put("recipe_id", recipeId);
        customData.put("crafter", player.getUuid().toString());
        customData.put("craft_time", System.currentTimeMillis());
        DataComponentHelper.setCustomData(result, customData);
        
        // Add custom name if specified
        if (selectedRecipe.getDisplayName() != null) {
            result.set(DataComponentTypes.CUSTOM_NAME, Text.literal(selectedRecipe.getDisplayName()));
        }
        
        // Add lore if any
        if (!selectedRecipe.getLore().isEmpty()) {
            List<Text> lore = new ArrayList<>();
            for (String loreLine : selectedRecipe.getLore()) {
                lore.add(Text.literal(loreLine).formatted(Formatting.GRAY));
            }
            DataComponentHelper.setLore(result, lore);
        }
        
        // Play crafting sound
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 0.3f, 1.0f);
        
        return result;
    }

    public CompletableFuture<List<String>> getCustomRecipeSuggestions(
            ServerPlayerEntity player, VillagerEntity villager, String culture) {
        return llmService.getRecipeSuggestions(player, villager, culture);
    }
    
    public List<CraftingRecipe> getRecipesForCulture(String culture) {
        return culturalRecipes.getOrDefault(culture, Collections.emptyList());
    }
    
    public CraftingRecipe getRecipeById(String id) {
        // Iterate through the recipes list to find a matching recipe.
        for (CraftingRecipe recipe : recipes) {
            if (recipe.getId().equals(id)) {
                return recipe;
            }
        }
        return null;
    }
    
    public static class CraftingRecipe {
        private final String id;
        private final Item result;
        private final Map<Item, Integer> ingredients;
        private String displayName;
        private final List<String> lore;
        
        public CraftingRecipe(String id, Item result, Map<Item, Integer> ingredients) {
            this.id = id;
            this.result = result;
            this.ingredients = ingredients;
            this.lore = new ArrayList<>();
        }
        
        public String getId() { return id; }
        public Item getResult() { return result; }
        public Map<Item, Integer> getIngredients() { return ingredients; }
        public String getDisplayName() { return displayName; }
        public List<String> getLore() { return lore; }
        
        public void setDisplayName(String name) {
            this.displayName = name;
        }
        
        public void addLore(String line) {
            this.lore.add(line);
        }
        
        public boolean hasRequiredMaterials(ServerPlayerEntity player) {
            for (Map.Entry<Item, Integer> entry : ingredients.entrySet()) {
                if (!player.getInventory().containsAny(stack -> stack.isOf(entry.getKey()) &&
                    stack.getCount() >= entry.getValue())) {
                    return false;
                }
            }
            return true;
        }
        
        public void consumeMaterials(ServerPlayerEntity player) {
            for (Map.Entry<Item, Integer> entry : ingredients.entrySet()) {
                player.getInventory().remove(stack -> stack.isOf(entry.getKey()), 
                    entry.getValue(), player.getInventory());
            }
        }

        public net.minecraft.item.Item getOutput() {
            return result;
        }

        public java.util.Map<net.minecraft.item.Item, Integer> getInputs() {
            return ingredients;
        }
    }
}