package com.beeny.village;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;

import java.util.*;
import java.util.function.Consumer;

public class CulturalCrafting {
    private static final Map<Culture.CultureType, List<CulturalRecipe>> CULTURAL_RECIPES = new HashMap<>();
    private static final Map<String, CulturalArtifact> REGISTERED_ARTIFACTS = new HashMap<>();
    
    public static class CulturalRecipe {
        private final String id;
        private final String name;
        private final Culture.CultureType cultureType;
        private final List<ItemStack> ingredients;
        private final ItemStack result;
        private final int skillLevel;
        private final Map<String, Object> additionalData;
        private Consumer<PlayerEntity> onCraftAction;
        
        public CulturalRecipe(String id, String name, Culture.CultureType cultureType, 
                            List<ItemStack> ingredients, ItemStack result, int skillLevel) {
            this.id = id;
            this.name = name;
            this.cultureType = cultureType;
            this.ingredients = ingredients;
            this.result = result;
            this.skillLevel = skillLevel;
            this.additionalData = new HashMap<>();
        }
        
        public static class Builder {
            private final String id;
            private String name;
            private Culture.CultureType cultureType;
            private List<ItemStack> ingredients = new ArrayList<>();
            private ItemStack result;
            private int skillLevel = 1;
            private Map<String, Object> additionalData = new HashMap<>();
            private Consumer<PlayerEntity> onCraftAction;
            
            public Builder(String id) {
                this.id = id;
                this.name = id;
            }
            
            public Builder withName(String name) {
                this.name = name;
                return this;
            }
            
            public Builder withCulture(Culture.CultureType cultureType) {
                this.cultureType = cultureType;
                return this;
            }
            
            public Builder addIngredient(Item item, int count) {
                this.ingredients.add(new ItemStack(item, count));
                return this;
            }
            
            public Builder withResult(Item item, int count) {
                this.result = new ItemStack(item, count);
                return this;
            }
            
            public Builder withSkillLevel(int level) {
                this.skillLevel = level;
                return this;
            }
            
            public Builder withData(String key, Object value) {
                this.additionalData.put(key, value);
                return this;
            }
            
            public Builder withOnCraftAction(Consumer<PlayerEntity> action) {
                this.onCraftAction = action;
                return this;
            }
            
            public CulturalRecipe build() {
                if (cultureType == null || result == null || ingredients.isEmpty()) {
                    throw new IllegalStateException("Recipe must have culture, result, and ingredients");
                }
                
                CulturalRecipe recipe = new CulturalRecipe(id, name, cultureType, ingredients, result, skillLevel);
                
                for (Map.Entry<String, Object> entry : additionalData.entrySet()) {
                    recipe.additionalData.put(entry.getKey(), entry.getValue());
                }
                
                if (onCraftAction != null) {
                    recipe.onCraftAction = onCraftAction;
                }
                
                return recipe;
            }
        }
        
        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public Culture.CultureType getCultureType() { return cultureType; }
        public List<ItemStack> getIngredients() { return ingredients; }
        public ItemStack getResult() { return result; }
        public int getSkillLevel() { return skillLevel; }
        
        // Can a player craft this recipe?
        public boolean canCraft(PlayerEntity player, int culturalSkillLevel) {
            return culturalSkillLevel >= skillLevel;
        }
        
        // Handle crafting logic
        public void onCraft(PlayerEntity player) {
            if (onCraftAction != null) {
                onCraftAction.accept(player);
            }
        }
    }
    
    public static class CulturalArtifact {
        private final String id;
        private final String name;
        private final Culture.CultureType cultureType;
        private ItemStack itemRepresentation;
        private final String description;
        private final int durability;
        private final List<ArtifactBonus> bonuses;
        private final Map<String, Object> additionalData;
        
        public CulturalArtifact(String id, String name, Culture.CultureType cultureType, 
                               ItemStack itemRepresentation, String description, int durability) {
            this.id = id;
            this.name = name;
            this.cultureType = cultureType;
            this.itemRepresentation = itemRepresentation;
            this.description = description;
            this.durability = durability;
            this.bonuses = new ArrayList<>();
            this.additionalData = new HashMap<>();
            
            NbtCompound customData = itemRepresentation.getOrDefault(
                DataComponentTypes.CUSTOM_DATA, 
                NbtComponent.DEFAULT
            ).copyNbt();
            
            customData.putString("cultural_artifact_id", id);
            customData.putString("culture_type", cultureType.getId());
            
            this.itemRepresentation.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
            this.itemRepresentation.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        }
        
        public static class Builder {
            private final String id;
            private String name;
            private Culture.CultureType cultureType;
            private ItemStack itemRepresentation;
            private String description = "";
            private int durability = -1;
            private final List<ArtifactBonus> bonuses = new ArrayList<>();
            private final Map<String, Object> additionalData = new HashMap<>();
            
            public Builder(String id) {
                this.id = id;
                this.name = id;
            }
            
            public Builder withName(String name) {
                this.name = name;
                return this;
            }
            
            public Builder withCulture(Culture.CultureType cultureType) {
                this.cultureType = cultureType;
                return this;
            }
            
            public Builder withItem(Item item) {
                this.itemRepresentation = new ItemStack(item);
                return this;
            }
            
            public Builder withDescription(String description) {
                this.description = description;
                return this;
            }
            
            public Builder withDurability(int durability) {
                this.durability = durability;
                return this;
            }
            
            public Builder addBonus(ArtifactBonus bonus) {
                this.bonuses.add(bonus);
                return this;
            }
            
            public Builder withData(String key, Object value) {
                this.additionalData.put(key, value);
                return this;
            }
            
            public CulturalArtifact build() {
                if (cultureType == null || itemRepresentation == null) {
                    throw new IllegalStateException("Artifact must have culture and item representation");
                }
                
                CulturalArtifact artifact = new CulturalArtifact(id, name, cultureType, 
                                                               itemRepresentation, description, durability);
                
                for (ArtifactBonus bonus : bonuses) {
                    artifact.addBonus(bonus);
                }
                
                for (Map.Entry<String, Object> entry : additionalData.entrySet()) {
                    artifact.additionalData.put(entry.getKey(), entry.getValue());
                }
                
                return artifact;
            }
        }
        
        public void addBonus(ArtifactBonus bonus) {
            bonuses.add(bonus);
        }
        
        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public Culture.CultureType getCultureType() { return cultureType; }
        public ItemStack getItemRepresentation() { return itemRepresentation.copy(); }
        public String getDescription() { return description; }
        public int getDurability() { return durability; }
        public List<ArtifactBonus> getBonuses() { return bonuses; }
        
        /**
         * Apply all bonuses from this artifact to a player
         */
        public void applyBonuses(PlayerEntity player) {
            for (ArtifactBonus bonus : bonuses) {
                bonus.apply(player);
            }
        }
        
        public void removeBonuses(PlayerEntity player) {
            for (ArtifactBonus bonus : bonuses) {
                bonus.remove(player);
            }
        }
    }
    
    public static class ArtifactBonus {
        private final BonusType type;
        private final float value;
        private final String description;
        private final Map<String, Object> bonusData;
        
        public enum BonusType {
            MOVEMENT_SPEED,
            ATTACK_DAMAGE,
            ATTACK_SPEED,
            MAX_HEALTH,
            KNOCKBACK_RESISTANCE,
            TRADING_DISCOUNT,
            CRAFTING_SPEED,
            EXPERIENCE_BOOST,
            HUNGER_REDUCTION,
            SPECIAL_ABILITY
        }
        
        public ArtifactBonus(BonusType type, float value, String description) {
            this.type = type;
            this.value = value;
            this.description = description;
            this.bonusData = new HashMap<>();
        }
        
        public ArtifactBonus withData(String key, Object value) {
            bonusData.put(key, value);
            return this;
        }
        
        public BonusType getType() { return type; }
        public float getValue() { return value; }
        public String getDescription() { return description; }
        
        public void apply(PlayerEntity player) {
            player.sendMessage(Text.of("You feel the power of the artifact: " + description), false);
        }
        
        public void remove(PlayerEntity player) {
        }
    }
    
    public static void initialize() {
        registerDefaultRecipes();
        registerDefaultArtifacts();
    }
    
    public static void registerRecipe(CulturalRecipe recipe) {
        CULTURAL_RECIPES.computeIfAbsent(recipe.getCultureType(), k -> new ArrayList<>())
                       .add(recipe);
    }
    
    public static void registerArtifact(CulturalArtifact artifact) {
        REGISTERED_ARTIFACTS.put(artifact.getId(), artifact);
    }
    
    public static List<CulturalRecipe> getRecipesForCulture(Culture.CultureType cultureType) {
        return CULTURAL_RECIPES.getOrDefault(cultureType, Collections.emptyList());
    }
    
    public static List<CulturalRecipe> getRecipesForHybridCulture(Culture culture) {
        if (!culture.isHybrid()) {
            return getRecipesForCulture(culture.getType());
        }
        
        List<CulturalRecipe> recipes = new ArrayList<>(getRecipesForCulture(culture.getType()));
        
        // Add recipes from secondary culture based on hybrid ratio
        List<CulturalRecipe> secondaryRecipes = getRecipesForCulture(culture.getSecondaryType());
        for (CulturalRecipe recipe : secondaryRecipes) {
            if (Math.random() < culture.getHybridRatio()) {
                recipes.add(recipe);
            }
        }
        
        return recipes;
    }
    
    public static CulturalArtifact getArtifact(String artifactId) {
        return REGISTERED_ARTIFACTS.get(artifactId);
    }
    
    private static void registerDefaultRecipes() {
        registerRecipe(new CulturalRecipe.Builder("gladius")
            .withName("Gladius")
            .withCulture(Culture.CultureType.ROMAN)
            .addIngredient(Items.IRON_INGOT, 2)
            .addIngredient(Items.GOLD_INGOT, 1)
            .withResult(Items.IRON_SWORD, 1)
            .withSkillLevel(2)
            .withData("damage_bonus", 2)
            .build());
            
        registerRecipe(new CulturalRecipe.Builder("roman_armor")
            .withName("Roman Lorica Segmentata")
            .withCulture(Culture.CultureType.ROMAN)
            .addIngredient(Items.IRON_INGOT, 7)
            .addIngredient(Items.LEATHER, 3)
            .withResult(Items.IRON_CHESTPLATE, 1)
            .withSkillLevel(3)
            .build());
            
        // MEDIEVAL RECIPES
        registerRecipe(new CulturalRecipe.Builder("longsword")
            .withName("Knight's Longsword")
            .withCulture(Culture.CultureType.MEDIEVAL)
            .addIngredient(Items.IRON_INGOT, 3)
            .addIngredient(Items.DIAMOND, 1)
            .withResult(Items.IRON_SWORD, 1)
            .withSkillLevel(2)
            .withData("reach_bonus", 1)
            .build());
            
        registerRecipe(new CulturalRecipe.Builder("chainmail")
            .withName("Authentic Chainmail")
            .withCulture(Culture.CultureType.MEDIEVAL)
            .addIngredient(Items.IRON_NUGGET, 20)
            .addIngredient(Items.STRING, 5)
            .withResult(Items.CHAINMAIL_CHESTPLATE, 1)
            .withSkillLevel(3)
            .build());
            
        // GREEK RECIPES
        registerRecipe(new CulturalRecipe.Builder("bronze_spear")
            .withName("Hoplite Spear")
            .withCulture(Culture.CultureType.GREEK)
            .addIngredient(Items.IRON_INGOT, 1)
            .addIngredient(Items.STICK, 3)
            .withResult(Items.TRIDENT, 1)
            .withSkillLevel(2)
            .build());
            
        // JAPANESE RECIPES
        registerRecipe(new CulturalRecipe.Builder("katana")
            .withName("Samurai Katana")
            .withCulture(Culture.CultureType.JAPANESE)
            .addIngredient(Items.IRON_INGOT, 4)
            .addIngredient(Items.GOLD_INGOT, 1)
            .addIngredient(Items.COAL, 10)
            .withResult(Items.IRON_SWORD, 1)
            .withSkillLevel(4)
            .withData("damage_bonus", 3)
            .withData("attack_speed_bonus", 0.2f)
            .build());
            
        registerRecipe(new CulturalRecipe.Builder("paper_lantern")
            .withName("Paper Lantern")
            .withCulture(Culture.CultureType.JAPANESE)
            .addIngredient(Items.PAPER, 6)
            .addIngredient(Items.TORCH, 1)
            .addIngredient(Items.BAMBOO, 4)
            .withResult(Items.LANTERN, 2)
            .withSkillLevel(1)
            .build());
            
        registerRecipe(new CulturalRecipe.Builder("obsidian_blade")
            .withName("Ceremonial Obsidian Blade")
            .withCulture(Culture.CultureType.MAYAN)
            .addIngredient(Items.OBSIDIAN, 5)
            .addIngredient(Items.EMERALD, 2)
            .withResult(Items.GOLDEN_SWORD, 1)
            .withSkillLevel(3)
            .withData("damage_bonus", 4)
            .withData("bleeding_effect", true)
            .build());
    }
    
    private static void registerDefaultArtifacts() {
        registerArtifact(new CulturalArtifact.Builder("imperial_seal")
            .withName("Imperial Seal of Rome")
            .withCulture(Culture.CultureType.ROMAN)
            .withItem(Items.GOLD_BLOCK)
            .withDescription("A symbol of Roman authority, granting influence over NPCs.")
            .addBonus(new ArtifactBonus(ArtifactBonus.BonusType.TRADING_DISCOUNT, 0.15f, 
                     "Roman merchants respect your authority and offer better prices."))
            .build());
            
        registerArtifact(new CulturalArtifact.Builder("holy_grail")
            .withName("Holy Grail")
            .withCulture(Culture.CultureType.MEDIEVAL)
            .withItem(Items.GOLDEN_APPLE)
            .withDescription("A legendary artifact said to grant healing powers.")
            .addBonus(new ArtifactBonus(ArtifactBonus.BonusType.MAX_HEALTH, 4.0f, 
                     "The Grail's magic increases your vitality."))
            .build());
            
        registerArtifact(new CulturalArtifact.Builder("olympic_wreath")
            .withName("Olympic Victor's Wreath")
            .withCulture(Culture.CultureType.GREEK)
            .withItem(Items.VINE)
            .withDescription("A laurel wreath awarded to Olympic victors, enhancing physical abilities.")
            .addBonus(new ArtifactBonus(ArtifactBonus.BonusType.MOVEMENT_SPEED, 0.15f,
                     "Your movements become swift as an Olympic athlete."))
            .build());
            
        registerArtifact(new CulturalArtifact.Builder("samurai_banner")
            .withName("Samurai Clan Banner")
            .withCulture(Culture.CultureType.JAPANESE)
            .withItem(Items.WHITE_BANNER)
            .withDescription("A battle banner representing an ancient samurai clan.")
            .addBonus(new ArtifactBonus(ArtifactBonus.BonusType.ATTACK_DAMAGE, 2.0f,
                     "The warrior spirit of your ancestors guides your blade."))
            .addBonus(new ArtifactBonus(ArtifactBonus.BonusType.KNOCKBACK_RESISTANCE, 0.2f,
                     "You stand firm like a disciplined warrior."))
            .build());
            
        registerArtifact(new CulturalArtifact.Builder("crystal_skull")
            .withName("Crystal Skull of Knowledge")
            .withCulture(Culture.CultureType.MAYAN)
            .withItem(Items.DIAMOND)
            .withDescription("A mysterious crystal skull containing ancient knowledge.")
            .addBonus(new ArtifactBonus(ArtifactBonus.BonusType.EXPERIENCE_BOOST, 0.25f,
                     "The skull's knowledge helps you learn faster."))
            .build());
    }
    
    public static CulturalArtifact createHybridArtifact(Culture.CultureType primary, Culture.CultureType secondary) {
        String id = "hybrid_" + primary.getId() + "_" + secondary.getId() + "_artifact";
        String name = "Artifact of Cultural Exchange: " + primary.getId() + " and " + secondary.getId();
        
        CulturalArtifact.Builder builder = new CulturalArtifact.Builder(id)
            .withName(name)
            .withCulture(primary) // Primary culture is the main one
            .withItem(Items.NETHER_STAR) // Special item for hybrid artifacts
            .withDescription("A unique artifact displaying traits of both " 
                          + primary.getId() + " and " + secondary.getId() + " cultures.");
            
        // Get bonuses based on both cultures
        switch(primary) {
            case ROMAN -> builder.addBonus(new ArtifactBonus(ArtifactBonus.BonusType.TRADING_DISCOUNT, 0.1f, 
                           "Roman influence improves your trading abilities."));
            case MEDIEVAL -> builder.addBonus(new ArtifactBonus(ArtifactBonus.BonusType.MAX_HEALTH, 2.0f, 
                             "Medieval fortitude strengthens your constitution."));
            case GREEK -> builder.addBonus(new ArtifactBonus(ArtifactBonus.BonusType.MOVEMENT_SPEED, 0.1f,
                         "Greek athletic tradition enhances your speed."));
            case JAPANESE -> builder.addBonus(new ArtifactBonus(ArtifactBonus.BonusType.ATTACK_SPEED, 0.1f,
                            "Japanese martial discipline improves your combat speed."));
            case MAYAN -> builder.addBonus(new ArtifactBonus(ArtifactBonus.BonusType.EXPERIENCE_BOOST, 0.15f,
                         "Mayan wisdom helps you learn faster."));
            default -> builder.addBonus(new ArtifactBonus(ArtifactBonus.BonusType.SPECIAL_ABILITY, 1.0f,
                          "A mysterious power emanates from the artifact."));
        }
        
        // Add bonus from secondary culture
        switch(secondary) {
            case ROMAN -> builder.addBonus(new ArtifactBonus(ArtifactBonus.BonusType.KNOCKBACK_RESISTANCE, 0.1f, 
                           "Roman discipline helps you stand your ground."));
            case MEDIEVAL -> builder.addBonus(new ArtifactBonus(ArtifactBonus.BonusType.ATTACK_DAMAGE, 1.0f, 
                             "Medieval combat techniques enhance your striking power."));
            case GREEK -> builder.addBonus(new ArtifactBonus(ArtifactBonus.BonusType.CRAFTING_SPEED, 0.1f,
                         "Greek philosophy enhances your crafting knowledge."));
            case JAPANESE -> builder.addBonus(new ArtifactBonus(ArtifactBonus.BonusType.HUNGER_REDUCTION, 0.1f,
                            "Japanese discipline reduces your consumption needs."));
            case MAYAN -> builder.addBonus(new ArtifactBonus(ArtifactBonus.BonusType.SPECIAL_ABILITY, 1.0f,
                         "Mayan mysticism grants you an unusual ability."));
            default -> builder.addBonus(new ArtifactBonus(ArtifactBonus.BonusType.MAX_HEALTH, 1.0f,
                          "The cultural fusion provides additional vitality."));
        }
        
        // Create the artifact
        CulturalArtifact artifact = builder.build();
        
        // Register it so it can be referenced later
        registerArtifact(artifact);
        
        return artifact;
    }
}