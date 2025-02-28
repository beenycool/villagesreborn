package com.beeny.village.crafting;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Initializes unique cultural recipes for specialized crafting stations
 */
public class CulturalRecipeInitializer {
    
    /**
     * Initialize all cultural recipes
     */
    public static void init() {
        initializeRomanRecipes();
        initializeEgyptianRecipes();
        initializeVictorianRecipes();
        initializeNYCRecipes();
    }
    
    /**
     * Initialize recipes for the Roman Forge
     */
    private static void initializeRomanRecipes() {
        // Roman Gladius - Enhanced sword with sharpness
        ItemStack romanGladius = new ItemStack(Items.IRON_SWORD);
        romanGladius.setCustomName(Text.of("Roman Gladius"));
        
        // Add enchantments using the new registry method
        addEnchantment(romanGladius, Enchantments.SHARPNESS, 3);
        addEnchantment(romanGladius, Enchantments.UNBREAKING, 2);
        
        List<ItemStack> gladiusIngredients = Arrays.asList(
            new ItemStack(Items.IRON_INGOT, 3),
            new ItemStack(Items.GOLD_INGOT, 1),
            new ItemStack(Items.STICK, 1)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "roman_gladius",
                gladiusIngredients,
                romanGladius,
                CulturalCraftingStation.StationType.ROMAN_FORGE,
                110,
                200 // 10 seconds
            )
        );
        
        // Roman Lorica Segmentata - Enhanced chestplate
        ItemStack lorica = new ItemStack(Items.IRON_CHESTPLATE);
        lorica.setCustomName(Text.of("Roman Lorica Segmentata"));
        addEnchantment(lorica, Enchantments.PROTECTION, 2);
        
        List<ItemStack> loricaIngredients = Arrays.asList(
            new ItemStack(Items.IRON_INGOT, 8),
            new ItemStack(Items.LEATHER, 2)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "roman_lorica",
                loricaIngredients,
                lorica,
                CulturalCraftingStation.StationType.ROMAN_FORGE,
                125,
                300 // 15 seconds
            )
        );
        
        // Roman Bath Salts - Regeneration potion
        ItemStack bathSalts = new ItemStack(Items.POTION);
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Potion", "minecraft:regeneration");
        bathSalts.setNbt(nbt);
        bathSalts.setCustomName(Text.of("Roman Bath Salts"));
        
        List<ItemStack> bathSaltsIngredients = Arrays.asList(
            new ItemStack(Items.GHAST_TEAR, 1),
            new ItemStack(Items.REDSTONE, 2),
            new ItemStack(Items.GLASS_BOTTLE, 1)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "roman_bath_salts",
                bathSaltsIngredients,
                bathSalts,
                CulturalCraftingStation.StationType.ROMAN_FORGE,
                110,
                140 // 7 seconds
            )
        );
    }
    
    /**
     * Initialize recipes for the Egyptian Altar
     */
    private static void initializeEgyptianRecipes() {
        // Eye of Horus - Night vision item
        ItemStack eyeOfHorus = new ItemStack(Items.GOLDEN_CARROT);
        eyeOfHorus.setCustomName(Text.of("Eye of Horus"));
        
        // Use NBT to add custom effects
        NbtCompound nbt = new NbtCompound();
        NbtList effectsList = new NbtList();
        NbtCompound effect = new NbtCompound();
        effect.putByte("Id", (byte)16); // Night Vision ID
        effect.putInt("Duration", 6000); // 5 minutes
        effect.putByte("Amplifier", (byte)0);
        effect.putBoolean("ShowParticles", false);
        effectsList.add(effect);
        nbt.put("CustomPotionEffects", effectsList);
        eyeOfHorus.setNbt(nbt);
        
        List<ItemStack> eyeIngredients = Arrays.asList(
            new ItemStack(Items.GOLDEN_CARROT, 1),
            new ItemStack(Items.GLOWSTONE_DUST, 3),
            new ItemStack(Items.EMERALD, 1)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "eye_of_horus",
                eyeIngredients,
                eyeOfHorus,
                CulturalCraftingStation.StationType.EGYPTIAN_ALTAR,
                110,
                180 // 9 seconds
            )
        );
        
        // Scarab Charm - Luck enhancement
        ItemStack scarabCharm = new ItemStack(Items.GOLD_INGOT);
        scarabCharm.setCustomName(Text.of("Scarab Charm"));
        addEnchantment(scarabCharm, Enchantments.LUCK_OF_THE_SEA, 3);
        
        List<ItemStack> scarabIngredients = Arrays.asList(
            new ItemStack(Items.GOLD_INGOT, 2),
            new ItemStack(Items.LAPIS_LAZULI, 4),
            new ItemStack(Items.RABBIT_FOOT, 1)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "scarab_charm",
                scarabIngredients,
                scarabCharm,
                CulturalCraftingStation.StationType.EGYPTIAN_ALTAR,
                125,
                240 // 12 seconds
            )
        );
        
        // Pharaoh's Crown - Enhanced helmet
        ItemStack pharaohCrown = new ItemStack(Items.GOLDEN_HELMET);
        pharaohCrown.setCustomName(Text.of("Pharaoh's Crown"));
        addEnchantment(pharaohCrown, Enchantments.PROTECTION, 2);
        addEnchantment(pharaohCrown, Enchantments.RESPIRATION, 1);
        
        List<ItemStack> crownIngredients = Arrays.asList(
            new ItemStack(Items.GOLD_INGOT, 5),
            new ItemStack(Items.LAPIS_LAZULI, 2),
            new ItemStack(Items.DIAMOND, 1)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "pharaoh_crown",
                crownIngredients,
                pharaohCrown,
                CulturalCraftingStation.StationType.EGYPTIAN_ALTAR,
                150,
                320 // 16 seconds
            )
        );
    }
    
    /**
     * Initialize recipes for the Victorian Workshop
     */
    private static void initializeVictorianRecipes() {
        // Victorian Pocket Watch - Haste effect
        ItemStack pocketWatch = new ItemStack(Items.CLOCK);
        pocketWatch.setCustomName(Text.of("Victorian Pocket Watch"));
        
        NbtCompound nbt = new NbtCompound();
        NbtList watchEffects = new NbtList();
        NbtCompound hasteEffect = new NbtCompound();
        hasteEffect.putByte("Id", (byte)3); // Haste ID
        hasteEffect.putInt("Duration", 4800); // 4 minutes
        hasteEffect.putByte("Amplifier", (byte)0);
        watchEffects.add(hasteEffect);
        nbt.put("CustomPotionEffects", watchEffects);
        pocketWatch.setNbt(nbt);
        
        List<ItemStack> watchIngredients = Arrays.asList(
            new ItemStack(Items.GOLD_INGOT, 1),
            new ItemStack(Items.REDSTONE, 3),
            new ItemStack(Items.QUARTZ, 2)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "victorian_pocket_watch",
                watchIngredients,
                pocketWatch,
                CulturalCraftingStation.StationType.VICTORIAN_WORKSHOP,
                110,
                200 // 10 seconds
            )
        );
        
        // Industrial Pickaxe - Enhanced mining tool
        ItemStack industrialPickaxe = new ItemStack(Items.IRON_PICKAXE);
        industrialPickaxe.setCustomName(Text.of("Industrial Pickaxe"));
        addEnchantment(industrialPickaxe, Enchantments.EFFICIENCY, 3);
        addEnchantment(industrialPickaxe, Enchantments.UNBREAKING, 2);
        
        List<ItemStack> pickaxeIngredients = Arrays.asList(
            new ItemStack(Items.IRON_INGOT, 3),
            new ItemStack(Items.REDSTONE, 4),
            new ItemStack(Items.STICK, 2)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "industrial_pickaxe",
                pickaxeIngredients,
                industrialPickaxe,
                CulturalCraftingStation.StationType.VICTORIAN_WORKSHOP,
                120,
                260 // 13 seconds
            )
        );
        
        // Steam-Powered Boots - Speed enhancement
        ItemStack steamBoots = new ItemStack(Items.IRON_BOOTS);
        steamBoots.setCustomName(Text.of("Steam-Powered Boots"));
        addEnchantment(steamBoots, Enchantments.DEPTH_STRIDER, 2);
        addEnchantment(steamBoots, Enchantments.FEATHER_FALLING, 2);
        
        List<ItemStack> bootsIngredients = Arrays.asList(
            new ItemStack(Items.IRON_INGOT, 4),
            new ItemStack(Items.REDSTONE, 3),
            new ItemStack(Items.COAL, 2)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "steam_boots",
                bootsIngredients,
                steamBoots,
                CulturalCraftingStation.StationType.VICTORIAN_WORKSHOP,
                135,
                280 // 14 seconds
            )
        );
    }
    
    /**
     * Initialize recipes for the NYC Studio
     */
    private static void initializeNYCRecipes() {
        // NYC Fireworks - Special celebration item
        ItemStack nycFireworks = new ItemStack(Items.FIREWORK_ROCKET, 3);
        nycFireworks.setCustomName(Text.of("NYC Celebration Fireworks"));
        
        NbtCompound nbt = new NbtCompound();
        NbtCompound fireworksItem = new NbtCompound();
        fireworksItem.putByte("Flight", (byte)3); // Maximum flight duration
        
        NbtList explosions = new NbtList();
        NbtCompound explosion1 = new NbtCompound();
        explosion1.putByte("Type", (byte)1); // Star-shaped explosion
        explosion1.putIntArray("Colors", new int[]{11743532, 3887386, 14602026}); // Red, blue, and white
        explosions.add(explosion1);
        
        fireworksItem.put("Explosions", explosions);
        nbt.put("Fireworks", fireworksItem);
        nycFireworks.setNbt(nbt);
        
        List<ItemStack> fireworkIngredients = Arrays.asList(
            new ItemStack(Items.PAPER, 3),
            new ItemStack(Items.GUNPOWDER, 3),
            new ItemStack(Items.GLOWSTONE_DUST, 1),
            new ItemStack(Items.BLUE_DYE, 1),
            new ItemStack(Items.RED_DYE, 1)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "nyc_fireworks",
                fireworkIngredients,
                nycFireworks,
                CulturalCraftingStation.StationType.NYC_STUDIO,
                110,
                160 // 8 seconds
            )
        );
        
        // Urban Crossbow - Enhanced ranged weapon
        ItemStack urbanCrossbow = new ItemStack(Items.CROSSBOW);
        urbanCrossbow.setCustomName(Text.of("Urban Crossbow"));
        addEnchantment(urbanCrossbow, Enchantments.QUICK_CHARGE, 2);
        addEnchantment(urbanCrossbow, Enchantments.PIERCING, 2);
        
        List<ItemStack> crossbowIngredients = Arrays.asList(
            new ItemStack(Items.IRON_INGOT, 2),
            new ItemStack(Items.STRING, 3),
            new ItemStack(Items.STICK, 3),
            new ItemStack(Items.TRIPWIRE_HOOK, 1)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "urban_crossbow",
                crossbowIngredients,
                urbanCrossbow,
                CulturalCraftingStation.StationType.NYC_STUDIO,
                130,
                240 // 12 seconds
            )
        );
        
        // Builder's Blueprint - Efficiency tool
        ItemStack buildersBlueprint = new ItemStack(Items.FILLED_MAP);
        buildersBlueprint.setCustomName(Text.of("NYC Builder's Blueprint"));
        
        List<ItemStack> blueprintIngredients = Arrays.asList(
            new ItemStack(Items.PAPER, 3),
            new ItemStack(Items.LAPIS_LAZULI, 1),
            new ItemStack(Items.QUARTZ, 2)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "nyc_blueprint",
                blueprintIngredients,
                buildersBlueprint,
                CulturalCraftingStation.StationType.NYC_STUDIO,
                115,
                180 // 9 seconds
            )
        );
    }
    
    /**
     * Helper method to add enchantments properly using the Registry system in 1.21.4
     */
    private static void addEnchantment(ItemStack stack, Enchantment enchantment, int level) {
        // Get or create enchantments NBT
        NbtCompound nbt = stack.getNbt() != null ? stack.getNbt() : new NbtCompound();
        NbtList enchList;
        
        // Check if there are existing enchantments
        if (nbt.contains("Enchantments", 9)) {
            enchList = nbt.getList("Enchantments", 10);
        } else {
            enchList = new NbtList();
        }
        
        // Create the enchantment entry
        NbtCompound enchEntry = new NbtCompound();
        enchEntry.putString("id", Registries.ENCHANTMENT.getId(enchantment).toString());
        enchEntry.putInt("lvl", level);
        
        // Add to the list
        enchList.add(enchEntry);
        
        // Put back in the NBT
        nbt.put("Enchantments", enchList);
        stack.setNbt(nbt);
    }
}