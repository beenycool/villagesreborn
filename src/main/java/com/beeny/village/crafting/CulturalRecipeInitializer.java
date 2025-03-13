package com.beeny.village.crafting;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.text.Style;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Cultural recipe initialization system
 */
public class CulturalRecipeInitializer {
    private static WrapperLookup wrapperLookup = null;
    
    /**
     * Initialize all cultural recipes
     */
    public static void init(WrapperLookup lookup) {
        wrapperLookup = lookup;
        registerRomanRecipes();
        registerEgyptianRecipes();
        registerVictorianRecipes();
        registerNYCRecipes();
    }
    
    /**
     * Register Roman crafting recipes
     */
    private static void registerRomanRecipes() {
        // Roman Gladius
        ItemStack gladius = new ItemStack(Items.IRON_SWORD);
        NbtCompound nbt = new NbtCompound();
        
        // Add display name
        NbtCompound display = new NbtCompound();
        // Use Text.Serialization.toJsonString with the WrapperLookup parameter
        display.putString("Name", Text.Serialization.toJsonString(Text.literal("Roman Gladius"), wrapperLookup));
        nbt.put("display", display);
        
        // Add enchantments
        NbtList enchList = new NbtList();
        addEnchantmentNbt(enchList, "minecraft:sharpness", 3);
        addEnchantmentNbt(enchList, "minecraft:unbreaking", 2);
        nbt.put("Enchantments", enchList);
        
        // Set NBT data on the item
        gladius = ItemStack.fromNbt(nbt);

        // Register recipe
        List<ItemStack> ingredients = Arrays.asList(
            new ItemStack(Items.IRON_INGOT, 3),
            new ItemStack(Items.GOLD_INGOT, 1),
            new ItemStack(Items.STICK, 1)
        );

        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "roman_gladius",
                ingredients,
                gladius,
                CulturalCraftingStation.StationType.ROMAN_FORGE,
                110,
                200
            )
        );

        // Roman Lorica Segmentata
        ItemStack lorica = new ItemStack(Items.IRON_CHESTPLATE);
        NbtCompound loricaNbt = new NbtCompound();
        
        NbtCompound loricaDisplay = new NbtCompound();
        loricaDisplay.putString("Name", Text.Serialization.toJsonString(Text.literal("Roman Lorica Segmentata"), wrapperLookup));
        loricaNbt.put("display", loricaDisplay);
        
        NbtList loricaEnch = new NbtList();
        addEnchantmentNbt(loricaEnch, "minecraft:protection", 3);
        addEnchantmentNbt(loricaEnch, "minecraft:unbreaking", 2);
        loricaNbt.put("Enchantments", loricaEnch);
        
        // Set NBT data on the item
        lorica = ItemStack.fromNbt(loricaNbt);

        ingredients = Arrays.asList(
            new ItemStack(Items.IRON_INGOT, 7),
            new ItemStack(Items.LEATHER, 2)
        );

        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "roman_lorica",
                ingredients,
                lorica,
                CulturalCraftingStation.StationType.ROMAN_FORGE,
                120,
                250
            )
        );
    }
    
    /**
     * Register Egyptian crafting recipes
     */
    private static void registerEgyptianRecipes() {
        // Desert Potion
        ItemStack desertPotion = new ItemStack(Items.POTION);
        addPotionEffect(desertPotion, "minecraft:fire_resistance", "Desert Protection Elixir");

        List<ItemStack> ingredients = Arrays.asList(
            new ItemStack(Items.GLASS_BOTTLE, 1),
            new ItemStack(Items.CACTUS, 2),
            new ItemStack(Items.BLAZE_POWDER, 1)
        );

        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "desert_protection",
                ingredients,
                desertPotion,
                CulturalCraftingStation.StationType.EGYPTIAN_ALTAR,
                100,
                150
            )
        );

        // Pharaoh's Staff
        ItemStack staff = new ItemStack(Items.BLAZE_ROD);
        NbtCompound nbt = new NbtCompound();
        
        NbtCompound display = new NbtCompound();
        display.putString("Name", Text.Serialization.toJsonString(Text.literal("Pharaoh's Staff"), wrapperLookup));
        nbt.put("display", display);
        
        NbtList enchList = new NbtList();
        addEnchantmentNbt(enchList, "minecraft:fire_aspect", 2);
        nbt.put("Enchantments", enchList);
        
        // Set NBT data on the item
        staff = ItemStack.fromNbt(nbt);

        ingredients = Arrays.asList(
            new ItemStack(Items.STICK, 1),
            new ItemStack(Items.GOLD_INGOT, 2),
            new ItemStack(Items.BLAZE_ROD, 1)
        );

        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "pharaohs_staff",
                ingredients,
                staff,
                CulturalCraftingStation.StationType.EGYPTIAN_ALTAR,
                130,
                180
            )
        );
    }
    
    /**
     * Register Victorian crafting recipes
     */
    private static void registerVictorianRecipes() {
        // Victorian Pocket Watch
        ItemStack pocketWatch = new ItemStack(Items.CLOCK);
        NbtCompound nbt = new NbtCompound();
        
        NbtCompound display = new NbtCompound();
        display.putString("Name", Text.Serialization.toJsonString(Text.literal("Victorian Pocket Watch"), wrapperLookup));
        
        NbtList lore = new NbtList();
        lore.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("A finely crafted timepiece"), wrapperLookup)));
        display.put("Lore", lore);
        
        nbt.put("display", display);
        
        // Set NBT data on the item
        pocketWatch = ItemStack.fromNbt(nbt);

        List<ItemStack> ingredients = Arrays.asList(
            new ItemStack(Items.GOLD_INGOT, 3),
            new ItemStack(Items.REDSTONE, 1),
            new ItemStack(Items.GLASS_PANE, 1)
        );

        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "victorian_watch",
                ingredients,
                pocketWatch,
                CulturalCraftingStation.StationType.VICTORIAN_WORKSHOP,
                100,
                120
            )
        );

        // Victorian Fireworks
        ItemStack firework = new ItemStack(Items.FIREWORK_ROCKET);
        addFireworkEffect(firework, "Royal Victorian Fireworks", (byte)2, new int[]{11743532, 2437522});

        ingredients = Arrays.asList(
            new ItemStack(Items.PAPER, 1),
            new ItemStack(Items.GUNPOWDER, 2),
            new ItemStack(Items.GLOWSTONE_DUST, 1)
        );

        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "victorian_fireworks",
                ingredients,
                firework,
                CulturalCraftingStation.StationType.VICTORIAN_WORKSHOP,
                90,
                100
            )
        );
    }
    
    /**
     * Register NYC crafting recipes
     */
    private static void registerNYCRecipes() {
        // NYC-style Pizza
        ItemStack pizza = new ItemStack(Items.COOKED_BEEF);
        NbtCompound nbt = new NbtCompound();
        
        NbtCompound display = new NbtCompound();
        display.putString("Name", Text.Serialization.toJsonString(Text.literal("NYC-Style Pizza"), wrapperLookup));
        
        NbtList lore = new NbtList();
        lore.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("The best slice in town"), wrapperLookup)));
        display.put("Lore", lore);
        
        nbt.put("display", display);
        
        // Add food properties
        nbt.putInt("FoodLevel", 8);
        nbt.putFloat("Saturation", 0.8f);
        
        // Set NBT data on the item
        pizza = ItemStack.fromNbt(nbt);

        List<ItemStack> ingredients = Arrays.asList(
            new ItemStack(Items.BREAD, 1),
            new ItemStack(Items.COOKED_BEEF, 1),
            new ItemStack(Items.RED_DYE, 1) // Tomato sauce
        );

        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "nyc_pizza",
                ingredients,
                pizza,
                CulturalCraftingStation.StationType.NYC_STUDIO,
                80,
                60
            )
        );
    }
    
    /**
     * Helper method to add enchantment NBT
     */
    private static void addEnchantmentNbt(NbtList enchList, String enchantmentId, int level) {
        NbtCompound enchTag = new NbtCompound();
        enchTag.putString("id", enchantmentId);
        enchTag.putInt("lvl", level);
        enchList.add(enchTag);
    }
    
    /**
     * Helper method to add potion effect
     */
    private static void addPotionEffect(ItemStack stack, String potionId, String name) {
        NbtCompound nbt = new NbtCompound();
        
        // Add potion type
        nbt.putString("Potion", potionId);
        
        // Add custom name
        NbtCompound display = new NbtCompound();
        // Use Text.Serialization.toJsonString with the WrapperLookup parameter
        display.putString("Name", Text.Serialization.toJsonString(Text.literal(name), wrapperLookup));
        nbt.put("display", display);
        
        // Set NBT data on the item
        ItemStack newStack = ItemStack.fromNbt(nbt);
        stack.setCount(newStack.getCount());
        stack.copyNbtFrom(newStack);
    }
    
    /**
     * Helper method to add status effect
     */
    private static void addStatusEffect(ItemStack stack, String name, byte id, int duration, byte amplifier) {
        NbtCompound nbt = new NbtCompound();
        
        // Add custom name
        NbtCompound display = new NbtCompound();
        // Use Text.Serialization.toJsonString with the WrapperLookup parameter
        display.putString("Name", Text.Serialization.toJsonString(Text.literal(name), wrapperLookup));
        nbt.put("display", display);
        
        // Add effect
        NbtList effects = new NbtList();
        NbtCompound effect = new NbtCompound();
        effect.putByte("Id", id);
        effect.putInt("Duration", duration);
        effect.putByte("Amplifier", amplifier);
        effect.putBoolean("ShowParticles", false);
        effects.add(effect);
        nbt.put("CustomPotionEffects", effects);
        
        // Set NBT data on the item
        ItemStack newStack = ItemStack.fromNbt(nbt);
        stack.setCount(newStack.getCount());
        stack.copyNbtFrom(newStack);
    }
    
    /**
     * Helper method to add firework effect
     */
    private static void addFireworkEffect(ItemStack stack, String name, byte flight, int[] colors) {
        NbtCompound nbt = new NbtCompound();
        
        // Add custom name
        NbtCompound display = new NbtCompound();
        // Use Text.Serialization.toJsonString with the WrapperLookup parameter
        display.putString("Name", Text.Serialization.toJsonString(Text.literal(name), wrapperLookup));
        nbt.put("display", display);
        
        // Add firework data
        NbtCompound fireworks = new NbtCompound();
        fireworks.putByte("Flight", flight);
        
        NbtList explosions = new NbtList();
        NbtCompound explosion = new NbtCompound();
        explosion.putByte("Type", (byte)1);
        explosion.putIntArray("Colors", colors);
        explosions.add(explosion);
        
        fireworks.put("Explosions", explosions);
        nbt.put("Fireworks", fireworks);
        
        // Set NBT data on the item
        ItemStack newStack = ItemStack.fromNbt(nbt);
        stack.setCount(newStack.getCount());
        stack.copyNbtFrom(newStack);
    }
}