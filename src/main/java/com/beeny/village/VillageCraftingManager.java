package com.beeny.village;

import com.beeny.Villagesreborn; // Added import
import com.beeny.ai.LLMService;
import com.beeny.config.VillagesConfig; // Added import
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
import net.minecraft.network.PacketByteBuf;
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
        // Check if unique crafting recipes are enabled in the config
        if (!VillagesConfig.getInstance().getGameplaySettings().isUniqueCraftingRecipesEnabled()) {
            // If disabled, return an empty list, effectively hiding cultural recipes
            return Collections.emptyList();
        }
        // If enabled, return the recipes for the specified culture
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
    
    public void assignTask(VillagerEntity villager, String recipeId, ServerPlayerEntity player) {
        CraftingRecipe recipe = getRecipeById(recipeId);
        if (recipe == null) {
            LOGGER.warn("Recipe with ID {} not found", recipeId);
            return;
        }
        
        Map<Item, Integer> requiredIngredients = recipe.getIngredients();
        boolean bonusApplied = false;
        float costMultiplier = 1.0f;

        // Check for cultural trading bonus
        if (VillagesConfig.getInstance().getGameplaySettings().isVillagerTradingBoostEnabled()) {
            VillagerManager vm = VillagerManager.getInstance();
            SpawnRegion region = vm.getNearestSpawnRegion(villager.getBlockPos());
            String culture = (region != null) ? region.getCultureAsString().toUpperCase() : null; // Use uppercase for consistency

            if (culture != null) {
                int reputation = Villagesreborn.getInstance().getPlayerReputation(player.getUuid(), culture);
                final int REPUTATION_THRESHOLD = 50; // Example threshold for bonus
                final float BONUS_MULTIPLIER = 0.8f; // Example: 20% discount

                if (reputation >= REPUTATION_THRESHOLD) {
                    costMultiplier = BONUS_MULTIPLIER;
                    bonusApplied = true;
                    LOGGER.debug("Applying trading bonus (Rep: {}, Culture: {}) for player {} crafting with {}", reputation, culture, player.getName().getString(), villager.getName().getString());
                }
            }
        }

        // Calculate potentially modified ingredients
        Map<Item, Integer> finalIngredients = new HashMap<>();
        if (bonusApplied) {
            for (Map.Entry<Item, Integer> entry : requiredIngredients.entrySet()) {
                // Apply multiplier, round up, ensure minimum of 1
                int modifiedCount = Math.max(1, (int) Math.ceil(entry.getValue() * costMultiplier));
                finalIngredients.put(entry.getKey(), modifiedCount);
            }
        } else {
            finalIngredients.putAll(requiredIngredients); // Use original ingredients
        }


        // Check if player has the FINAL required materials
        boolean hasMaterials = true;
        for (Map.Entry<Item, Integer> entry : finalIngredients.entrySet()) {
            if (!player.getInventory().containsAny(stack -> stack.isOf(entry.getKey()) && stack.getCount() >= entry.getValue())) {
                hasMaterials = false;
                break;
            }
        }

        if (!hasMaterials) {
            player.sendMessage(Text.literal("You do not have the required materials for this recipe.").formatted(Formatting.RED), false);
            // Optionally list missing materials
            return;
        }

        // Consume the FINAL required materials
        for (Map.Entry<Item, Integer> entry : finalIngredients.entrySet()) {
            player.getInventory().remove(stack -> stack.isOf(entry.getKey()), entry.getValue(), player.getInventory());
        }
        
        // Assign the task to the villager (logic can be expanded as needed)
        LOGGER.info("Assigning crafting task {} to villager {}", recipeId, villager.getUuid());
        if (bonusApplied) {
            player.sendMessage(Text.literal("Task assigned! Your good reputation granted a discount.").formatted(Formatting.GREEN), false);
        } else {
            player.sendMessage(Text.literal("Task assigned successfully.").formatted(Formatting.GREEN), false);
        }
    }

    public void cancelTask(VillagerEntity villager, String recipeId) {
        LOGGER.info("Cancelling crafting task {} for villager {}", recipeId, villager.getUuid());
        // Logic to cancel the task (can be expanded as needed)
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

        // Add serialization and deserialization methods to CraftingRecipe
        public CraftingRecipe(PacketByteBuf buf) {
            this.id = buf.readString();
            this.result = Item.byRawId(buf.readInt());
            int size = buf.readInt();
            this.ingredients = new HashMap<>(size);
            for (int i = 0; i < size; i++) {
                Item item = Item.byRawId(buf.readInt());
                int count = buf.readInt();
                this.ingredients.put(item, count);
            }
            this.displayName = buf.readBoolean() ? buf.readString() : null;
            int loreSize = buf.readInt();
            this.lore = new ArrayList<>(loreSize);
            for (int i = 0; i < loreSize; i++) {
                this.lore.add(buf.readString());
            }
        }

        public void write(PacketByteBuf buf) {
            buf.writeString(this.id);
            buf.writeInt(Item.getRawId(this.result));
            buf.writeInt(this.ingredients.size());
            for (Map.Entry<Item, Integer> entry : this.ingredients.entrySet()) {
                buf.writeInt(Item.getRawId(entry.getKey()));
                buf.writeInt(entry.getValue());
            }
            buf.writeBoolean(this.displayName != null);
            if (this.displayName != null) {
                buf.writeString(this.displayName);
            }
            buf.writeInt(this.lore.size());
            for (String loreLine : this.lore) {
                buf.writeString(loreLine);
            }
        }
    }
}