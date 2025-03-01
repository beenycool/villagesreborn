package com.beeny.village.crafting;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registry;
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

    public static void init() {
        registerRomanRecipes();
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
        display.putString("Name", Text.Serialization.toJsonString(Text.literal("Roman Gladius"), null));
        nbt.put("display", display);
        
        // Add enchantments
        NbtList enchList = new NbtList();
        addEnchantmentNbt(enchList, "minecraft:sharpness", 3);
        addEnchantmentNbt(enchList, "minecraft:unbreaking", 2);
        nbt.put("Enchantments", enchList);
        
        gladius.setNbt(nbt);

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
        // Use Text.Serialization.toJsonString with the WrapperLookup parameter
        loricaDisplay.putString("Name", Text.Serialization.toJsonString(Text.literal("Roman Lorica Segmentata"), null));
        loricaNbt.put("display", loricaDisplay);
        
        NbtList loricaEnch = new NbtList();
        addEnchantmentNbt(loricaEnch, "minecraft:protection", 2);
        loricaNbt.put("Enchantments", loricaEnch);
        
        lorica.setNbt(loricaNbt);

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
                300
            )
        );

        // Roman Bath Salts
        ItemStack bathSalts = new ItemStack(Items.POTION);
        addPotionEffect(bathSalts, "minecraft:regeneration", "Roman Bath Salts");

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
                140
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
        display.putString("Name", Text.Serialization.toJsonString(Text.literal(name), null));
        nbt.put("display", display);
        
        stack.setNbt(nbt);
    }

    /**
     * Helper method to add status effect
     */
    private static void addStatusEffect(ItemStack stack, String name, byte id, int duration, byte amplifier) {
        NbtCompound nbt = new NbtCompound();
        
        // Add custom name
        NbtCompound display = new NbtCompound();
        // Use Text.Serialization.toJsonString with the WrapperLookup parameter
        display.putString("Name", Text.Serialization.toJsonString(Text.literal(name), null));
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
        
        stack.setNbt(nbt);
    }

    /**
     * Helper method to add firework effect
     */
    private static void addFireworkEffect(ItemStack stack, String name, byte flight, int[] colors) {
        NbtCompound nbt = new NbtCompound();
        
        // Add custom name
        NbtCompound display = new NbtCompound();
        // Use Text.Serialization.toJsonString with the WrapperLookup parameter
        display.putString("Name", Text.Serialization.toJsonString(Text.literal(name), null));
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
        
        stack.setNbt(nbt);
    }
}