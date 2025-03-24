package com.beeny.village.crafting;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.text.Style;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.Registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        
        // Add custom name using Components
        gladius.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Roman Gladius"));
        
        // Add enchantments
        ItemEnchantmentsComponent enchantments = new ItemEnchantmentsComponent();
        Registry<Enchantment> enchantmentRegistry = wrapperLookup.getRegistryLookup(RegistryKeys.ENCHANTMENT).orElseThrow();
        enchantments.add(enchantmentRegistry.getEntry(Enchantments.SHARPNESS).get(), 3);
        enchantments.add(enchantmentRegistry.getEntry(Enchantments.UNBREAKING).get(), 2);
        gladius.set(DataComponentTypes.ENCHANTMENTS, enchantments);

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
        
        // Add custom name using Components
        lorica.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Roman Lorica Segmentata"));
        
        // Add enchantments
        ItemEnchantmentsComponent loricaEnchantments = new ItemEnchantmentsComponent();
        loricaEnchantments.add(enchantmentRegistry.getEntry(Enchantments.PROTECTION).get(), 3);
        loricaEnchantments.add(enchantmentRegistry.getEntry(Enchantments.UNBREAKING).get(), 2);
        lorica.set(DataComponentTypes.ENCHANTMENTS, loricaEnchantments);

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
        
        // Add custom name using Components
        staff.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Pharaoh's Staff"));
        
        // Add enchantments
        ItemEnchantmentsComponent staffEnchantments = new ItemEnchantmentsComponent();
        Registry<Enchantment> enchantmentRegistry = wrapperLookup.getRegistryLookup(RegistryKeys.ENCHANTMENT).orElseThrow();
        staffEnchantments.add(enchantmentRegistry.getEntry(Enchantments.FIRE_ASPECT).get(), 2);
        staff.set(DataComponentTypes.ENCHANTMENTS, staffEnchantments);

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
        
        // Add custom name using Components
        pocketWatch.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Victorian Pocket Watch"));
        
        // Add lore using Components and custom NBT
        NbtCompound customData = new NbtCompound();
        NbtList loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("A finely crafted timepiece"), wrapperLookup)));
        customData.put("Lore", loreList);
        pocketWatch.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));

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
        
        // Add custom name using Components
        pizza.set(DataComponentTypes.CUSTOM_NAME, Text.literal("NYC-Style Pizza"));
        
        // Add lore and food properties using custom data component
        NbtCompound customData = new NbtCompound();
        
        // Add lore
        NbtList loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("The best slice in town"), wrapperLookup)));
        customData.put("Lore", loreList);
        
        // Add food properties
        customData.putInt("FoodLevel", 8);
        customData.putFloat("Saturation", 0.8f);
        
        pizza.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));

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
     * Helper method to add potion effect
     */
    private static void addPotionEffect(ItemStack stack, String potionId, String name) {
        // Add custom name using Components
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        
        // Add potion type using custom data component
        NbtCompound customData = new NbtCompound();
        customData.putString("Potion", potionId);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
    }
    
    /**
     * Helper method to add firework effect
     */
    private static void addFireworkEffect(ItemStack stack, String name, byte flight, int[] colors) {
        // Add custom name using Components
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        
        // Add firework data using custom data component
        NbtCompound customData = new NbtCompound();
        NbtCompound fireworks = new NbtCompound();
        fireworks.putByte("Flight", flight);
        
        NbtList explosions = new NbtList();
        NbtCompound explosion = new NbtCompound();
        explosion.putByte("Type", (byte)1);
        explosion.putIntArray("Colors", colors);
        explosions.add(explosion);
        
        fireworks.put("Explosions", explosions);
        customData.put("Fireworks", fireworks);
        
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
    }
}
