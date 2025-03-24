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
        
        // Add enchantments directly using NBT Component
        NbtList enchantmentsList = new NbtList();
        
        // Add Sharpness III
        NbtCompound sharpness = new NbtCompound();
        sharpness.putString("id", "minecraft:sharpness");
        sharpness.putInt("lvl", 3);
        enchantmentsList.add(sharpness);
        
        // Add Unbreaking II
        NbtCompound unbreaking = new NbtCompound();
        unbreaking.putString("id", "minecraft:unbreaking");
        unbreaking.putInt("lvl", 2);
        enchantmentsList.add(unbreaking);
        
        // Set enchantments to the component
        NbtCompound enchantmentsNbt = new NbtCompound();
        enchantmentsNbt.put("Enchantments", enchantmentsList);
        
        // Apply the enchantments using the appropriate method for Minecraft 1.21.4
        // Create a hash map to store enchantments and their levels
        Object2IntOpenHashMap<RegistryEntry<Enchantment>> enchantMap = new Object2IntOpenHashMap<>();
        
        for (int i = 0; i < enchantmentsList.size(); i++) {
            NbtCompound enchant = enchantmentsList.getCompound(i);
            String id = enchant.getString("id");
            int level = enchant.getInt("lvl");
            
            // Create identifier with namespace and path (minecraft:sharpness)
            String[] parts = id.split(":", 2);
            String namespace = parts.length > 1 ? parts[0] : "minecraft";
            String path = parts.length > 1 ? parts[1] : id;
            Identifier enchantId = new Identifier(namespace, path);
            
            Optional<RegistryEntry<Enchantment>> enchantment = wrapperLookup.getOptional(RegistryKeys.ENCHANTMENT)
                .flatMap(registry -> registry.getRegistryLookup().get(enchantId));
                
            if (enchantment.isPresent()) {
                enchantMap.put(enchantment.get(), level);
            }
        }
        
        // Create ItemEnchantmentsComponent with the correct constructor parameters
        ItemEnchantmentsComponent enchantmentComponent = new ItemEnchantmentsComponent(enchantMap, false);
        gladius.set(DataComponentTypes.ENCHANTMENTS, enchantmentComponent);

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
        
        // Add enchantments directly using NBT Component
        NbtList loricaEnchantList = new NbtList();
        
        // Add Protection III
        NbtCompound protection = new NbtCompound();
        protection.putString("id", "minecraft:protection");
        protection.putInt("lvl", 3);
        loricaEnchantList.add(protection);
        
        // Add Unbreaking II
        NbtCompound loricaUnbreaking = new NbtCompound();
        loricaUnbreaking.putString("id", "minecraft:unbreaking");
        loricaUnbreaking.putInt("lvl", 2);
        loricaEnchantList.add(loricaUnbreaking);
        
        // Apply the enchantments using the appropriate method for Minecraft 1.21.4
        Object2IntOpenHashMap<RegistryEntry<Enchantment>> loricaEnchantMap = new Object2IntOpenHashMap<>();
        for (int i = 0; i < loricaEnchantList.size(); i++) {
            NbtCompound enchant = loricaEnchantList.getCompound(i);
            String id = enchant.getString("id");
            int level = enchant.getInt("lvl");
            
            // Create identifier with namespace and path
            String[] parts = id.split(":", 2);
            String namespace = parts.length > 1 ? parts[0] : "minecraft";
            String path = parts.length > 1 ? parts[1] : id;
            Identifier enchantId = new Identifier(namespace, path);
            
            Optional<RegistryEntry<Enchantment>> enchantment = wrapperLookup.getOptional(RegistryKeys.ENCHANTMENT)
                .flatMap(registry -> registry.getRegistryLookup().get(enchantId));
                
            if (enchantment.isPresent()) {
                loricaEnchantMap.put(enchantment.get(), level);
            }
        }
        
        // Create component with correct constructor
        ItemEnchantmentsComponent loricaEnchantComponent = new ItemEnchantmentsComponent(loricaEnchantMap, false);
        lorica.set(DataComponentTypes.ENCHANTMENTS, loricaEnchantComponent);

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
        
        // Add enchantments directly using NBT Component
        NbtList staffEnchantList = new NbtList();
        
        // Add Fire Aspect II
        NbtCompound fireAspect = new NbtCompound();
        fireAspect.putString("id", "minecraft:fire_aspect");
        fireAspect.putInt("lvl", 2);
        staffEnchantList.add(fireAspect);
        
        // Apply the enchantments using the appropriate method for Minecraft 1.21.4
        Object2IntOpenHashMap<RegistryEntry<Enchantment>> staffEnchantMap = new Object2IntOpenHashMap<>();
        for (int i = 0; i < staffEnchantList.size(); i++) {
            NbtCompound enchant = staffEnchantList.getCompound(i);
            String id = enchant.getString("id");
            int level = enchant.getInt("lvl");
            
            // Create identifier with namespace and path
            String[] parts = id.split(":", 2);
            String namespace = parts.length > 1 ? parts[0] : "minecraft";
            String path = parts.length > 1 ? parts[1] : id;
            Identifier enchantId = new Identifier(namespace, path);
            
            Optional<RegistryEntry<Enchantment>> enchantment = wrapperLookup.getOptional(RegistryKeys.ENCHANTMENT)
                .flatMap(registry -> registry.getRegistryLookup().get(enchantId));
                
            if (enchantment.isPresent()) {
                staffEnchantMap.put(enchantment.get(), level);
            }
        }
        
        // Create component with correct constructor
        ItemEnchantmentsComponent staffEnchantComponent = new ItemEnchantmentsComponent(staffEnchantMap, false);
        staff.set(DataComponentTypes.ENCHANTMENTS, staffEnchantComponent);

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