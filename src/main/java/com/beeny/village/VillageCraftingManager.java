package com.beeny.village;

import com.beeny.ai.LLMService;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillageCraftingManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillageCraftingManager");
    private static VillageCraftingManager instance;
    private final Map<String, List<CraftingRecipe>> culturalRecipes = new HashMap<>();
    private final VillagerManager villagerManager;
    private final Map<UUID, Map<String, Long>> craftingCooldowns = new HashMap<>();
    private final Map<UUID, String> activeCraftingTasks = new HashMap<>();
    private static final long COOLDOWN_DURATION = 12000; // 10 minutes in game ticks
    private static final Random random = new Random();

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
        LOGGER.info("Assigning crafting task to villager {} for recipe {}", villager.getUuid(), recipeId);
        
        if (villager == null || player == null) {
            LOGGER.error("Cannot assign task - villager or player is null");
            return CompletableFuture.completedFuture("Error: Invalid villager or player");
        }
        
        UUID villagerUuid = villager.getUuid();
        
        // Check if villager has AI associated
        VillagerAI ai = villagerManager.getVillagerAI(villagerUuid);
        if (ai == null) {
            LOGGER.warn("Villager {} has no AI associated", villagerUuid);
            return CompletableFuture.completedFuture("This villager cannot craft items.");
        }
        
        // Check if villager is already busy with another crafting task
        if (activeCraftingTasks.containsKey(villagerUuid)) {
            String currentTask = activeCraftingTasks.get(villagerUuid);
            LOGGER.info("Villager {} is already busy with task {}", villagerUuid, currentTask);
            return CompletableFuture.completedFuture(villager.getName().getString() + " is already crafting " + currentTask);
        }
        
        // Get villager's culture based on location
        VillagerManager vm = VillagerManager.getInstance();
        SpawnRegion region = vm.getNearestSpawnRegion(villager.getBlockPos());
        String culture = region != null ? region.getCultureAsString() : "default";
        
        // Find recipe in villager's culture
        CraftingRecipe recipe = findRecipe(culture, recipeId);

        if (recipe == null) {
            LOGGER.warn("Unknown recipe: {} for culture: {}", recipeId, culture);
            return CompletableFuture.completedFuture("Unknown recipe for " + culture + " culture");
        }
        
        // Check if villager is on cooldown for this recipe
        if (isOnCooldown(villagerUuid, recipeId, player.getWorld().getTime())) {
            LOGGER.info("Villager {} is on cooldown for recipe {}", villagerUuid, recipeId);
            return CompletableFuture.completedFuture(villager.getName().getString() + " needs to rest before crafting this again.");
        }

        // Check if villager has required profession skill
        float craftingSkill = ai.getProfessionSkill("crafting");
        float recipeComplexity = calculateRecipeComplexity(recipe);
        if (craftingSkill < recipeComplexity * 0.5f) {
            LOGGER.info("Villager {} lacks skill for recipe {}", villagerUuid, recipeId);
            return CompletableFuture.completedFuture(villager.getName().getString() + " doesn't have enough skill to craft this item.");
        }

        // Check if player has required materials
        if (!hasRequiredMaterials(player, recipe)) {
            StringBuilder missingItems = new StringBuilder();
            for (Map.Entry<Item, Integer> entry : recipe.getInputs().entrySet()) {
                int required = entry.getValue();
                int playerHas = countPlayerItems(player, entry.getKey());
                if (playerHas < required) {
                    missingItems.append(required - playerHas).append("x ").append(entry.getKey().getName().getString()).append(", ");
                }
            }
            
            if (missingItems.length() > 0) {
                missingItems.setLength(missingItems.length() - 2); // Remove trailing comma and space
                LOGGER.info("Player {} missing materials for recipe {}: {}", player.getName().getString(), recipeId, missingItems);
                return CompletableFuture.completedFuture("Missing materials: " + missingItems);
            }
        }
        
        // All checks passed - mark villager as busy
        activeCraftingTasks.put(villagerUuid, recipeId);
        
        // Consume materials
        for (Map.Entry<Item, Integer> entry : recipe.getInputs().entrySet()) {
            consumePlayerItems(player, entry.getKey(), entry.getValue());
        }

        // Play crafting sound
        villager.getWorld().playSound(
            null, 
            villager.getBlockPos(), 
            SoundEvents.ENTITY_VILLAGER_WORK_MASON, 
            SoundCategory.NEUTRAL, 
            1.0F, 
            1.0F
        );

        String prompt = String.format(
            "Villager %s (%s) is tasked with crafting %s in a %s village. Describe their reaction and work process in 15 words or less.",
            villager.getName().getString(),
            ai.getPersonality(),
            recipeId,
            culture
        );

        return LLMService.getInstance().generateResponse(prompt)
            .thenApply(response -> {
                try {
                    // Apply skill gain based on recipe complexity
                    float skillGain = calculateSkillGain(recipe);
                    ai.updateActivity("crafting_" + recipeId);
                    ai.updateProfessionSkill("crafting", craftingSkill + skillGain);
                    
                    // Calculate quality based on villager's skill
                    float quality = calculateItemQuality(craftingSkill, recipeComplexity);
                    
                    // Create the crafted item with quality if applicable
                    ItemStack result = createQualityItem(recipe.getOutput(), quality, villager.getName().getString());
                    
                    // Give the player the crafted item
                    boolean success = player.getInventory().insertStack(result);
                    if (!success) {
                        // Drop item if inventory is full
                        player.dropItem(result, false);
                    }
                    
                    // Set cooldown
                    setCooldown(villagerUuid, recipeId, player.getWorld().getTime());
                    
                    // Send messages
                    String qualityText = quality > 1.1f ? " (Exceptional Quality)" : 
                                        quality > 1.0f ? " (Good Quality)" : "";
                    player.sendMessage(Text.literal("Received: " + result.getName().getString() + qualityText)
                        .formatted(Formatting.GREEN), true);
                    player.sendMessage(Text.literal(response), false);
                    
                    LOGGER.info("Villager {} successfully crafted {} for player {}", 
                        villagerUuid, recipeId, player.getName().getString());
                    
                    // Complete the task
                    activeCraftingTasks.remove(villagerUuid);
                    
                    return response;
                } catch (Exception e) {
                    LOGGER.error("Error during crafting completion: {}", e.getMessage(), e);
                    activeCraftingTasks.remove(villagerUuid);
                    return "Error occurred during crafting";
                }
            })
            .exceptionally(ex -> {
                LOGGER.error("Error during crafting task: {}", ex.getMessage(), ex);
                activeCraftingTasks.remove(villagerUuid);
                return "Error occurred while crafting";
            });
    }
    
    public void cancelTask(UUID villagerUuid, String recipeId, ServerPlayerEntity player) {
        if (activeCraftingTasks.containsKey(villagerUuid) && 
            activeCraftingTasks.get(villagerUuid).equals(recipeId)) {
            
            activeCraftingTasks.remove(villagerUuid);
            player.sendMessage(Text.literal("Crafting canceled").formatted(Formatting.YELLOW), true);
            LOGGER.info("Crafting task {} canceled for villager {}", recipeId, villagerUuid);
        }
    }
    
    private ItemStack createQualityItem(Item item, float quality, String craftedBy) {
        ItemStack stack = new ItemStack(item);
        
        // Only add quality attributes if quality is above normal
        if (quality > 1.0f) {
            NbtCompound nbt = stack.getOrCreateNbt();
            nbt.putFloat("CraftQuality", quality);
            nbt.putString("CraftedBy", craftedBy);
            
            // Add visual effect to name for high quality items
            if (quality >= 1.2f) {
                stack.setCustomName(Text.literal("✦ " + item.getName().getString() + " ✦")
                    .formatted(Formatting.AQUA));
            }
        }
        
        return stack;
    }
    
    private float calculateItemQuality(float craftingSkill, float recipeComplexity) {
        // Base quality is 1.0
        float baseQuality = 1.0f;
        
        // Skill bonus (up to 0.2 bonus)
        float skillBonus = Math.min(0.2f, craftingSkill / 10f);
        
        // Complexity bonus for matching skill to complex recipes (up to 0.1 bonus)
        float complexityBonus = 0f;
        if (craftingSkill >= recipeComplexity) {
            complexityBonus = Math.min(0.1f, recipeComplexity / 20f);
        }
        
        // Random factor (-0.05 to +0.05)
        float randomFactor = (random.nextFloat() - 0.5f) * 0.1f;
        
        return baseQuality + skillBonus + complexityBonus + randomFactor;
    }
    
    private float calculateRecipeComplexity(CraftingRecipe recipe) {
        int ingredientCount = recipe.getInputs().size();
        int totalItemCount = recipe.getInputs().values().stream().mapToInt(Integer::intValue).sum();
        
        return ingredientCount * 1.5f + totalItemCount * 0.5f;
    }
    
    private boolean isOnCooldown(UUID villagerUuid, String recipeId, long currentGameTime) {
        Map<String, Long> villagerCooldowns = craftingCooldowns.get(villagerUuid);
        if (villagerCooldowns == null) {
            return false;
        }
        
        Long cooldownUntil = villagerCooldowns.get(recipeId);
        if (cooldownUntil == null) {
            return false;
        }
        
        // Check if current game time is still within cooldown period
        return currentGameTime < cooldownUntil;
    }
    
    private void setCooldown(UUID villagerUuid, String recipeId, long currentGameTime) {
        craftingCooldowns
            .computeIfAbsent(villagerUuid, k -> new HashMap<>())
            .put(recipeId, currentGameTime + COOLDOWN_DURATION);
    }
    
    private boolean hasRequiredMaterials(ServerPlayerEntity player, CraftingRecipe recipe) {
        for (Map.Entry<Item, Integer> entry : recipe.getInputs().entrySet()) {
            if (countPlayerItems(player, entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }
    
    private int countPlayerItems(ServerPlayerEntity player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().main) {
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    private void consumePlayerItems(ServerPlayerEntity player, Item item, int amount) {
        int remaining = amount;
        
        for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                int toRemove = Math.min(stack.getCount(), remaining);
                stack.decrement(toRemove);
                remaining -= toRemove;
            }
        }
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
        // More complex recipes provide more skill points
        int ingredientCount = recipe.getInputs().size();
        int totalItemCount = recipe.getInputs().values().stream().mapToInt(Integer::intValue).sum();
        
        // Base skill gain + bonuses for complexity
        return 0.1f * ingredientCount + 0.05f * totalItemCount;
    }
    
    /**
     * Get available recipes for a villager based on their culture and profession
     * 
     * @param villager The villager entity
     * @param player The player who is interacting with the villager
     * @param culture The culture of the villager
     * @return A list of recipe IDs available to this villager
     */
    public List<String> getAvailableRecipes(VillagerEntity villager, ServerPlayerEntity player, String culture) {
        LOGGER.debug("Getting available recipes for villager {} in culture {}", villager.getUuid(), culture);
        
        // Get the villager's AI
        VillagerAI ai = villagerManager.getVillagerAI(villager.getUuid());
        if (ai == null) {
            LOGGER.warn("No AI found for villager {}", villager.getUuid());
            return List.of();
        }
        
        // Get recipes for this culture
        List<CraftingRecipe> recipes = getRecipesForCulture(culture);
        if (recipes.isEmpty()) {
            LOGGER.warn("No recipes found for culture {}", culture);
            return List.of();
        }
        
        // Get the villager's crafting skill
        float craftingSkill = 0.0f;
        try {
            craftingSkill = ai.getProfessionSkill("crafting");
        } catch (Exception e) {
            LOGGER.error("Error getting crafting skill for villager {}: {}", villager.getUuid(), e.getMessage());
        }
        
        // Filter recipes based on skill and return IDs
        return recipes.stream()
            .filter(recipe -> {
                float complexity = calculateRecipeComplexity(recipe);
                // Villager can craft if they have at least half the required skill
                return craftingSkill >= complexity * 0.5f;
            })
            .map(CraftingRecipe::getId)
            .toList();
    }
}
