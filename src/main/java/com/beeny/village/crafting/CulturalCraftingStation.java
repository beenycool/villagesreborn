package com.beeny.village.crafting;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import com.beeny.village.event.PlayerEventParticipation;

import java.util.*;

/**
 * Handles specialized cultural crafting stations that provide unique
 * crafting recipes and bonuses based on the player's reputation
 */
public class CulturalCraftingStation {
    private static final int MIN_REPUTATION_TO_USE = 100;
    
    /**
     * The different types of cultural crafting stations
     */
    public enum StationType {
        ROMAN_FORGE("Roman Forge", "roman", "Specialized forge for crafting enhanced weapons with Roman aesthetics"),
        EGYPTIAN_ALTAR("Egyptian Altar", "egyptian", "Mystical altar for creating items using desert resources"),
        VICTORIAN_WORKSHOP("Victorian Workshop", "victorian", "Advanced workshop for industrial gadgets and machinery parts"),
        NYC_STUDIO("NYC Studio", "nyc", "Modern studio for unique NYC-themed creations");
        
        private final String name;
        private final String culture;
        private final String description;
        
        StationType(String name, String culture, String description) {
            this.name = name;
            this.culture = culture;
            this.description = description;
        }
        
        public String getName() { return name; }
        public String getCulture() { return culture; }
        public String getDescription() { return description; }
        
        public static StationType getByName(String name) {
            for (StationType type : values()) {
                if (type.name.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }
        
        public static StationType getByCulture(String culture) {
            for (StationType type : values()) {
                if (type.culture.equalsIgnoreCase(culture)) {
                    return type;
                }
            }
            return null;
        }
    }
    
    /**
     * Represents a unique cultural recipe
     */
    public static class CulturalRecipe {
        private final String id;
        private final List<ItemStack> ingredients;
        private final ItemStack result;
        private final StationType stationType;
        private final int reputationRequired;
        private final int craftingTime; // in ticks
        
        public CulturalRecipe(String id, List<ItemStack> ingredients, ItemStack result, 
                              StationType stationType, int reputationRequired, int craftingTime) {
            this.id = id;
            this.ingredients = ingredients;
            this.result = result;
            this.stationType = stationType;
            this.reputationRequired = reputationRequired;
            this.craftingTime = craftingTime;
        }
        
        public String getId() { return id; }
        public List<ItemStack> getIngredients() { return ingredients; }
        public ItemStack getResult() { return result; }
        public StationType getStationType() { return stationType; }
        public int getReputationRequired() { return reputationRequired; }
        public int getCraftingTime() { return craftingTime; }
    }
    
    /**
     * Represents a block entity for a cultural crafting station
     */
    public static class CulturalCraftingBlockEntity extends BlockEntity {
        private StationType stationType;
        private final Map<UUID, CraftingSession> activeSessions = new HashMap<>();
        
        public CulturalCraftingBlockEntity(BlockPos pos, BlockState state, StationType type) {
            super(null, pos, state); // Block entity type would need to be registered
            this.stationType = type;
        }
        
        public StationType getStationType() { return stationType; }
        
        /**
         * Start a crafting session for a player
         */
        public void startCrafting(ServerPlayerEntity player, CulturalRecipe recipe) {
            UUID playerId = player.getUuid();
            CraftingSession session = new CraftingSession(player, recipe);
            activeSessions.put(playerId, session);
        }
        
        /**
         * Check if a player can use this crafting station
         */
        public boolean canPlayerUse(PlayerEntity player) {
            int reputation = PlayerEventParticipation.getInstance()
                .getReputation(player, stationType.getCulture());
                
            return reputation >= MIN_REPUTATION_TO_USE;
        }
        
        /**
         * Update active crafting sessions
         */
        public void tick(ServerWorld world) {
            Iterator<Map.Entry<UUID, CraftingSession>> it = activeSessions.entrySet().iterator();
            
            while (it.hasNext()) {
                Map.Entry<UUID, CraftingSession> entry = it.next();
                CraftingSession session = entry.getValue();
                
                if (session.tick()) {
                    // Crafting is complete
                    ServerPlayerEntity player = world.getServer().getPlayerManager()
                        .getPlayer(entry.getKey());
                        
                    if (player != null && player.isAlive()) {
                        // Give the player the crafted item
                        ItemStack result = session.recipe.getResult().copy();
                        if (!player.giveItemStack(result)) {
                            player.dropItem(result, false);
                        }
                        
                        // Play sound and effects
                        world.playSound(null, getPos(), SoundEvents.BLOCK_ANVIL_USE, 
                            SoundCategory.BLOCKS, 0.5f, 1.0f);
                            
                        player.sendMessage(Text.of("§6You've crafted " + result.getName().getString() + "!"), false);
                    }
                    
                    // Remove this session
                    it.remove();
                }
            }
        }
        
        @Override
        protected void writeNbt(NbtCompound nbt) {
            super.writeNbt(nbt);
            nbt.putString("StationType", stationType.name());
        }
        
        public void readNbt(NbtCompound nbt) {
            super.readNbt(nbt);
            if (nbt.contains("StationType")) {
                try {
                    this.stationType = StationType.valueOf(nbt.getString("StationType"));
                } catch (IllegalArgumentException e) {
                    this.stationType = StationType.ROMAN_FORGE; // Default fallback
                }
            }
        }
        
        /**
         * Represents an active crafting session
         */
        private static class CraftingSession {
            private final ServerPlayerEntity player;
            private final CulturalRecipe recipe;
            private int remainingTicks;
            
            public CraftingSession(ServerPlayerEntity player, CulturalRecipe recipe) {
                this.player = player;
                this.recipe = recipe;
                this.remainingTicks = recipe.getCraftingTime();
                
                // Adjust crafting time based on player's reputation (faster for loyal players)
                int reputation = PlayerEventParticipation.getInstance()
                    .getReputation(player, recipe.getStationType().getCulture());
                    
                if (reputation >= 150) {
                    this.remainingTicks = (int)(remainingTicks * 0.7); // 30% faster for reputation 150+
                } else if (reputation >= 120) {
                    this.remainingTicks = (int)(remainingTicks * 0.8); // 20% faster for reputation 120+
                } else if (reputation >= 100) {
                    this.remainingTicks = (int)(remainingTicks * 0.9); // 10% faster for reputation 100+
                }
                
                // Send start message
                player.sendMessage(Text.of("§aCrafting started! It will take " + 
                    (remainingTicks / 20) + " seconds to complete."), false);
            }
            
            /**
             * Update crafting progress
             * @return true if crafting is complete
             */
            public boolean tick() {
                remainingTicks--;
                
                // Occasionally update the player on progress
                if (player.isAlive() && remainingTicks > 0 && remainingTicks % 60 == 0) {
                    player.sendMessage(Text.of("§aCrafting progress: " + 
                        (int)((1 - (double)remainingTicks / recipe.getCraftingTime()) * 100) + "%"), false);
                }
                
                return remainingTicks <= 0;
            }
        }
    }
    
    /**
     * Recipe registry for cultural crafting
     */
    public static class RecipeRegistry {
        private static final Map<String, CulturalRecipe> recipes = new HashMap<>();
        
        /**
         * Register a new cultural recipe
         */
        public static void registerRecipe(CulturalRecipe recipe) {
            recipes.put(recipe.getId(), recipe);
        }
        
        /**
         * Get a recipe by its ID
         */
        public static CulturalRecipe getRecipe(String id) {
            return recipes.get(id);
        }
        
        /**
         * Get all recipes for a specific station type
         */
        public static List<CulturalRecipe> getRecipesForStation(StationType type) {
            List<CulturalRecipe> result = new ArrayList<>();
            
            for (CulturalRecipe recipe : recipes.values()) {
                if (recipe.getStationType() == type) {
                    result.add(recipe);
                }
            }
            
            return result;
        }
        
        /**
         * Get all recipes a player can craft at a specific station
         */
        public static List<CulturalRecipe> getAvailableRecipes(PlayerEntity player, StationType type) {
            List<CulturalRecipe> result = new ArrayList<>();
            int reputation = PlayerEventParticipation.getInstance().getReputation(player, type.getCulture());
            
            for (CulturalRecipe recipe : getRecipesForStation(type)) {
                if (reputation >= recipe.getReputationRequired()) {
                    result.add(recipe);
                }
            }
            
            return result;
        }
        
        /**
         * Initialize the registry with default recipes
         */
        public static void initialize() {
            // Recipes will be initialized here or loaded from data files
        }
    }
}