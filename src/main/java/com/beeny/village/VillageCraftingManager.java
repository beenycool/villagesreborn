package com.beeny.village;

import com.beeny.ai.LLMService;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillageCraftingManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillageCraftingManager");
    private static VillageCraftingManager instance;
    private final Map<String, List<CraftingRecipe>> culturalRecipes = new HashMap<>();
    private final VillagerManager villagerManager;
    private final Map<UUID, Map<String, Long>> craftingCooldowns = new HashMap<>();
    private static final long COOLDOWN_DURATION = 12000; // 10 minutes in game ticks

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
        
        VillagerAI ai = villagerManager.getVillagerAI(villager.getUuid());
        VillagerManager vm = VillagerManager.getInstance();
        SpawnRegion region = vm.getNearestSpawnRegion(villager.getBlockPos());
        String culture = region != null ? region.getCultureAsString() : "default";
        CraftingRecipe recipe = findRecipe(culture, recipeId);

        if (recipe == null) {
            LOGGER.warn("Unknown recipe: {} for culture: {}", recipeId, culture);
            return CompletableFuture.completedFuture("Unknown recipe");
        }
        
        // Check if villager is on cooldown for this recipe
        if (isOnCooldown(villager.getUuid(), recipeId, player.getWorld().getTime())) {
            LOGGER.info("Villager {} is on cooldown for recipe {}", villager.getUuid(), recipeId);
            return CompletableFuture.completedFuture(villager.getName().getString() + " needs to rest before crafting this again.");
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
        
        // Consume materials
        for (Map.Entry<Item, Integer> entry : recipe.getInputs().entrySet()) {
            consumePlayerItems(player, entry.getKey(), entry.getValue());
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
                // Apply skill gain
                float skillGain = calculateSkillGain(recipe);
                ai.updateActivity("crafting_" + recipeId);
                ai.updateProfessionSkill("crafting");
                
                // Give the player the crafted item
                ItemStack result = new ItemStack(recipe.getOutput());
                player.getInventory().insertStack(result);
                
                // Set cooldown
                setCooldown(villager.getUuid(), recipeId, player.getWorld().getTime());
                
                // Send messages
                player.sendMessage(Text.literal("Received: " + result.getName().getString()).formatted(Formatting.GREEN), true);
                player.sendMessage(Text.literal(response), false);
                
                LOGGER.info("Villager {} successfully crafted {} for player {}", 
                    villager.getUuid(), recipeId, player.getName().getString());
                
                return response;
            });
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
}
