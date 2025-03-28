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
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cultural recipe initialization system for defining culturally-specific
 * crafting recipes with custom NBT data, enchantments, and special properties.
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
        registerGreekRecipes();
        registerJapaneseRecipes();
        registerMayanRecipes();
        registerMedievalRecipes();
        registerSpecialRecipes();
    }
    
    /**
     * Register Roman crafting recipes
     */
    private static void registerRomanRecipes() {
        // Roman Gladius
        ItemStack gladius = new ItemStack(Items.IRON_SWORD);
        
        // Add custom name using Components
        gladius.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Roman Gladius").formatted(Formatting.RED));
        
        // Add enchantments using custom NBT data component
        NbtCompound enchantmentData = new NbtCompound();
        NbtList enchantmentList = new NbtList();
        
        // Sharpness enchantment
        NbtCompound sharpness = new NbtCompound();
        sharpness.putString("id", "minecraft:sharpness");
        sharpness.putShort("lvl", (short)3);
        enchantmentList.add(sharpness);
        
        // Unbreaking enchantment
        NbtCompound unbreaking = new NbtCompound();
        unbreaking.putString("id", "minecraft:unbreaking");
        unbreaking.putShort("lvl", (short)2);
        enchantmentList.add(unbreaking);
        
        enchantmentData.put("Enchantments", enchantmentList);
        
        // Add lore
        NbtList loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("The weapon of Roman legionnaires").formatted(Formatting.GRAY), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Forged in the heart of Rome").formatted(Formatting.GRAY), wrapperLookup)));
        enchantmentData.put("Lore", loreList);
        
        gladius.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(enchantmentData));

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
        lorica.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Roman Lorica Segmentata").formatted(Formatting.RED));
        
        // Add enchantments using custom NBT data component
        enchantmentData = new NbtCompound();
        enchantmentList = new NbtList();
        
        // Protection enchantment
        NbtCompound protection = new NbtCompound();
        protection.putString("id", "minecraft:protection");
        protection.putShort("lvl", (short)3);
        enchantmentList.add(protection);
        
        // Unbreaking enchantment
        unbreaking = new NbtCompound();
        unbreaking.putString("id", "minecraft:unbreaking");
        unbreaking.putShort("lvl", (short)2);
        enchantmentList.add(unbreaking);
        
        enchantmentData.put("Enchantments", enchantmentList);
        
        // Add lore
        loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Armor of the Imperial legions").formatted(Formatting.GRAY), wrapperLookup)));
        enchantmentData.put("Lore", loreList);
        
        lorica.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(enchantmentData));

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
        
        // Roman Scutum Shield
        ItemStack scutum = new ItemStack(Items.SHIELD);
        
        // Add custom name using Components
        scutum.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Roman Scutum").formatted(Formatting.RED));
        
        // Add enchantments using custom NBT data component
        enchantmentData = new NbtCompound();
        enchantmentList = new NbtList();
        
        // Protection enchantment
        NbtCompound knockback = new NbtCompound();
        knockback.putString("id", "minecraft:knockback");
        knockback.putShort("lvl", (short)1);
        enchantmentList.add(knockback);
        
        // Thorns enchantment
        NbtCompound thorns = new NbtCompound();
        thorns.putString("id", "minecraft:thorns");
        thorns.putShort("lvl", (short)1);
        enchantmentList.add(thorns);
        
        enchantmentData.put("Enchantments", enchantmentList);
        
        // Add lore
        loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("The shield of Roman formation tactics").formatted(Formatting.GRAY), wrapperLookup)));
        enchantmentData.put("Lore", loreList);
        
        // Add attributes
        enchantmentData.putFloat("BlockStrength", 1.5f);
        
        scutum.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(enchantmentData));

        ingredients = Arrays.asList(
            new ItemStack(Items.IRON_INGOT, 1),
            new ItemStack(Items.OAK_PLANKS, 5),
            new ItemStack(Items.LEATHER, 2)
        );

        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "roman_scutum",
                ingredients,
                scutum,
                CulturalCraftingStation.StationType.ROMAN_FORGE,
                90,
                180
            )
        );
    }
    
    /**
     * Register Egyptian crafting recipes
     */
    private static void registerEgyptianRecipes() {
        // Desert Protection Elixir
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
        staff.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Pharaoh's Staff").formatted(Formatting.GOLD));
        
        // Add enchantments using custom NBT data component
        NbtCompound enchantmentData = new NbtCompound();
        NbtList enchantmentList = new NbtList();
        
        // Fire Aspect enchantment
        NbtCompound fireAspect = new NbtCompound();
        fireAspect.putString("id", "minecraft:fire_aspect");
        fireAspect.putShort("lvl", (short)2);
        enchantmentList.add(fireAspect);
        
        // Power enchantment
        NbtCompound power = new NbtCompound();
        power.putString("id", "minecraft:power");
        power.putShort("lvl", (short)3);
        enchantmentList.add(power);
        
        enchantmentData.put("Enchantments", enchantmentList);
        
        // Add lore
        NbtList loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("A symbol of royal authority").formatted(Formatting.GRAY), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Blessed by Ra, the sun god").formatted(Formatting.GOLD), wrapperLookup)));
        enchantmentData.put("Lore", loreList);
        
        staff.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(enchantmentData));

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
        
        // Ankh of Resurrection
        ItemStack ankh = new ItemStack(Items.TOTEM_OF_UNDYING);
        
        // Add custom name
        ankh.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Ankh of Resurrection").formatted(Formatting.GOLD));
        
        // Add custom NBT data
        NbtCompound customData = new NbtCompound();
        
        // Add lore
        loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Symbol of eternal life").formatted(Formatting.GRAY), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Grants second chance to its bearer").formatted(Formatting.AQUA), wrapperLookup)));
        customData.put("Lore", loreList);
        
        // Add custom properties
        customData.putInt("ResurrectionPower", 2); // More powerful than standard totem
        customData.putInt("HealthRestored", 15);
        
        ankh.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
        
        ingredients = Arrays.asList(
            new ItemStack(Items.GOLD_INGOT, 4),
            new ItemStack(Items.EMERALD, 1),
            new ItemStack(Items.GHAST_TEAR, 1)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "ankh_of_resurrection",
                ingredients,
                ankh,
                CulturalCraftingStation.StationType.EGYPTIAN_ALTAR,
                150,
                300
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
        pocketWatch.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Victorian Pocket Watch").formatted(Formatting.GOLD));
        
        // Add lore using Components and custom NBT
        NbtCompound customData = new NbtCompound();
        NbtList loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("A finely crafted timepiece").formatted(Formatting.GRAY), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Keeps perfect time even in the most adverse conditions").formatted(Formatting.GRAY), wrapperLookup)));
        customData.put("Lore", loreList);
        
        // Add special properties
        customData.putBoolean("IsPrecise", true);
        customData.putBoolean("ShowsPhase", true);
        
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
        
        // Victorian Explorer's Hat
        ItemStack explorerHat = new ItemStack(Items.LEATHER_HELMET);
        
        // Add custom name
        explorerHat.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Victorian Explorer's Hat").formatted(Formatting.DARK_GREEN));
        
        // Add enchantments and custom NBT
        customData = new NbtCompound();
        NbtList enchantments = new NbtList();
        
        // Respiration enchantment
        NbtCompound respiration = new NbtCompound();
        respiration.putString("id", "minecraft:respiration");
        respiration.putShort("lvl", (short)2);
        enchantments.add(respiration);
        
        customData.put("Enchantments", enchantments);
        
        // Add lore
        loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Worn by explorers of the British Empire").formatted(Formatting.GRAY), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Provides extra visibility in harsh conditions").formatted(Formatting.DARK_GREEN), wrapperLookup)));
        customData.put("Lore", loreList);
        
        // Add color
        customData.putInt("color", 5060991); // Brown color
        
        // Add special properties
        customData.putFloat("ExplorationBonus", 0.15f);
        
        explorerHat.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
        
        ingredients = Arrays.asList(
            new ItemStack(Items.LEATHER, 5),
            new ItemStack(Items.IRON_INGOT, 1),
            new ItemStack(Items.SPYGLASS, 1)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "victorian_explorer_hat",
                ingredients,
                explorerHat,
                CulturalCraftingStation.StationType.VICTORIAN_WORKSHOP,
                110,
                150
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
        pizza.set(DataComponentTypes.CUSTOM_NAME, Text.literal("NYC-Style Pizza").formatted(Formatting.RED));
        
        // Add lore and food properties using custom data component
        NbtCompound customData = new NbtCompound();
        
        // Add lore
        NbtList loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("The best slice in town").formatted(Formatting.GRAY), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Authentic NYC flavor").formatted(Formatting.GRAY), wrapperLookup)));
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
        
        // NYC Coffee
        ItemStack coffee = new ItemStack(Items.SUSPICIOUS_STEW);
        
        // Add custom name
        coffee.set(DataComponentTypes.CUSTOM_NAME, Text.literal("NYC Coffee").formatted(Formatting.DARK_PURPLE));
        
        // Add custom properties
        customData = new NbtCompound();
        
        // Add lore
        loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Strong New York coffee").formatted(Formatting.GRAY), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Provides energy boost and speed").formatted(Formatting.BLUE), wrapperLookup)));
        customData.put("Lore", loreList);
        
        // Add effects
        customData.putString("Effect", "minecraft:speed");
        customData.putInt("EffectDuration", 600); // 30 seconds
        
        // Add food properties
        customData.putInt("FoodLevel", 3);
        customData.putFloat("Saturation", 0.4f);
        
        coffee.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
        
        ingredients = Arrays.asList(
            new ItemStack(Items.BOWL, 1),
            new ItemStack(Items.COCOA_BEANS, 3),
            new ItemStack(Items.SUGAR, 1)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "nyc_coffee",
                ingredients,
                coffee,
                CulturalCraftingStation.StationType.NYC_STUDIO,
                60,
                50
            )
        );
        
        // NYC Taxi Whistle
        ItemStack taxiWhistle = new ItemStack(Items.GOAT_HORN);
        
        // Add custom name
        taxiWhistle.set(DataComponentTypes.CUSTOM_NAME, Text.literal("NYC Taxi Whistle").formatted(Formatting.YELLOW));
        
        // Add custom properties
        customData = new NbtCompound();
        
        // Add lore
        loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Hail a cab from anywhere").formatted(Formatting.GRAY), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("The sound carries through the busy streets").formatted(Formatting.GRAY), wrapperLookup)));
        customData.put("Lore", loreList);
        
        // Special horn properties
        customData.putString("instrument", "taxi_call");
        
        taxiWhistle.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
        
        ingredients = Arrays.asList(
            new ItemStack(Items.GOLD_INGOT, 1),
            new ItemStack(Items.STRING, 2)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "nyc_taxi_whistle",
                ingredients,
                taxiWhistle,
                CulturalCraftingStation.StationType.NYC_STUDIO,
                70,
                60
            )
        );
    }
    
    /**
     * Register Greek crafting recipes
     */
    private static void registerGreekRecipes() {
        // Hoplite Spear
        ItemStack spear = new ItemStack(Items.TRIDENT);
        
        // Add custom name
        spear.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Hoplite Spear").formatted(Formatting.AQUA));
        
        // Add enchantments
        NbtCompound enchantmentData = new NbtCompound();
        NbtList enchantments = new NbtList();
        
        // Impaling
        NbtCompound impaling = new NbtCompound();
        impaling.putString("id", "minecraft:impaling");
        impaling.putShort("lvl", (short)3);
        enchantments.add(impaling);
        
        // Loyalty
        NbtCompound loyalty = new NbtCompound();
        loyalty.putString("id", "minecraft:loyalty");
        loyalty.putShort("lvl", (short)2);
        enchantments.add(loyalty);
        
        enchantmentData.put("Enchantments", enchantments);
        
        // Add lore
        NbtList loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Weapon of the Greek phalanx").formatted(Formatting.GRAY), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Forged in the traditions of Sparta").formatted(Formatting.RED), wrapperLookup)));
        enchantmentData.put("Lore", loreList);
        
        spear.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(enchantmentData));
        
        List<ItemStack> ingredients = Arrays.asList(
            new ItemStack(Items.IRON_INGOT, 3),
            new ItemStack(Items.PRISMARINE_SHARD, 2),
            new ItemStack(Items.STICK, 2)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "hoplite_spear",
                ingredients,
                spear,
                CulturalCraftingStation.StationType.GREEK_ACADEMY,
                120,
                180
            )
        );
        
        // Olympic Laurel Crown
        ItemStack laurelCrown = new ItemStack(Items.GOLDEN_HELMET);
        
        // Add custom name
        laurelCrown.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Olympic Laurel Crown").formatted(Formatting.GREEN, Formatting.BOLD));
        
        // Add enchantments and NBT
        NbtCompound customData = new NbtCompound();
        enchantments = new NbtList();
        
        // Protection enchantment
        NbtCompound protection = new NbtCompound();
        protection.putString("id", "minecraft:protection");
        protection.putShort("lvl", (short)1);
        enchantments.add(protection);
        
        // Feather falling (speed)
        NbtCompound featherFalling = new NbtCompound();
        featherFalling.putString("id", "minecraft:feather_falling");
        featherFalling.putShort("lvl", (short)3);
        enchantments.add(featherFalling);
        
        customData.put("Enchantments", enchantments);
        
        // Add lore
        loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Symbol of victory at the Olympic games").formatted(Formatting.GRAY), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Grants the speed and agility of a champion").formatted(Formatting.GREEN), wrapperLookup)));
        customData.put("Lore", loreList);
        
        // Special properties
        customData.putFloat("SpeedBonus", 0.2f);
        customData.putFloat("JumpBonus", 0.1f);
        
        laurelCrown.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
        
        ingredients = Arrays.asList(
            new ItemStack(Items.GOLD_NUGGET, 5),
            new ItemStack(Items.VINE, 3),
            new ItemStack(Items.GOLDEN_APPLE, 1)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "olympic_laurel_crown",
                ingredients,
                laurelCrown,
                CulturalCraftingStation.StationType.GREEK_ACADEMY,
                150,
                200
            )
        );
    }
    
    /**
     * Register Japanese crafting recipes
     */
    private static void registerJapaneseRecipes() {
        // Samurai Katana
        ItemStack katana = new ItemStack(Items.NETHERITE_SWORD);
        
        // Add custom name
        katana.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Samurai Katana").formatted(Formatting.DARK_RED));
        
        // Add enchantments
        NbtCompound enchantmentData = new NbtCompound();
        NbtList enchantments = new NbtList();
        
        // Sharpness
        NbtCompound sharpness = new NbtCompound();
        sharpness.putString("id", "minecraft:sharpness");
        sharpness.putShort("lvl", (short)5);
        enchantments.add(sharpness);
        
        // Sweeping Edge
        NbtCompound sweeping = new NbtCompound();
        sweeping.putString("id", "minecraft:sweeping");
        sweeping.putShort("lvl", (short)3);
        enchantments.add(sweeping);
        
        // Mending
        NbtCompound mending = new NbtCompound();
        mending.putString("id", "minecraft:mending");
        mending.putShort("lvl", (short)1);
        enchantments.add(mending);
        
        enchantmentData.put("Enchantments", enchantments);
        
        // Add lore
        NbtList loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Folded 1000 times").formatted(Formatting.GRAY), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("The soul of a samurai dwells within").formatted(Formatting.DARK_PURPLE), wrapperLookup)));
        enchantmentData.put("Lore", loreList);
        
        // Special properties
        enchantmentData.putFloat("AttackSpeedBonus", 0.3f);
        
        katana.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(enchantmentData));
        
        List<ItemStack> ingredients = Arrays.asList(
            new ItemStack(Items.NETHERITE_INGOT, 1),
            new ItemStack(Items.DIAMOND, 3),
            new ItemStack(Items.IRON_INGOT, 5),
            new ItemStack(Items.BLAZE_POWDER, 2)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "samurai_katana",
                ingredients,
                katana,
                CulturalCraftingStation.StationType.JAPANESE_FORGE,
                200,
                400
            )
        );
        
        // Paper Lantern
        ItemStack lantern = new ItemStack(Items.LANTERN);
        
        // Add custom name
        lantern.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Japanese Paper Lantern").formatted(Formatting.LIGHT_PURPLE));
        
        // Add custom properties
        NbtCompound customData = new NbtCompound();
        
        // Add lore
        loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("A delicate light source").formatted(Formatting.GRAY), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Provides a calming atmosphere").formatted(Formatting.LIGHT_PURPLE), wrapperLookup)));
        customData.put("Lore", loreList);
        
        // Special properties
        customData.putInt("LightLevel", 14);
        customData.putBoolean("PeacefulAura", true);
        
        lantern.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
        
        ingredients = Arrays.asList(
            new ItemStack(Items.PAPER, 6),
            new ItemStack(Items.BAMBOO, 4),
            new ItemStack(Items.TORCH, 1)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "japanese_paper_lantern",
                ingredients,
                lantern,
                CulturalCraftingStation.StationType.JAPANESE_FORGE,
                80,
                100
            )
        );
    }
    
    /**
     * Register Mayan crafting recipes
     */
    private static void registerMayanRecipes() {
        // Obsidian Sacrificial Blade
        ItemStack obsidianBlade = new ItemStack(Items.GOLDEN_SWORD);
        
        // Add custom name
        obsidianBlade.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Obsidian Ceremonial Blade").formatted(Formatting.DARK_PURPLE));
        
        // Add enchantments
        NbtCompound enchantmentData = new NbtCompound();
        NbtList enchantments = new NbtList();
        
        // Smite
        NbtCompound smite = new NbtCompound();
        smite.putString("id", "minecraft:smite");
        smite.putShort("lvl", (short)5);
        enchantments.add(smite);
        
        // Looting
        NbtCompound looting = new NbtCompound();
        looting.putString("id", "minecraft:looting");
        looting.putShort("lvl", (short)3);
        enchantments.add(looting);
        
        enchantmentData.put("Enchantments", enchantments);
        
        // Add lore
        NbtList loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Used in sacred Mayan rituals").formatted(Formatting.GRAY), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("A blade that thirsts for offerings").formatted(Formatting.DARK_RED), wrapperLookup)));
        enchantmentData.put("Lore", loreList);
        
        // Special properties
        enchantmentData.putBoolean("BleedingEffect", true);
        enchantmentData.putFloat("RitualBonus", 0.5f);
        
        obsidianBlade.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(enchantmentData));
        
        List<ItemStack> ingredients = Arrays.asList(
            new ItemStack(Items.OBSIDIAN, 5),
            new ItemStack(Items.GOLD_INGOT, 2),
            new ItemStack(Items.EMERALD, 1)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "mayan_ritual_blade",
                ingredients,
                obsidianBlade,
                CulturalCraftingStation.StationType.MAYAN_ALTAR,
                150,
                220
            )
        );
        
        // Crystal Skull
        ItemStack crystalSkull = new ItemStack(Items.DIAMOND);
        
        // Add custom name
        crystalSkull.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Mayan Crystal Skull").formatted(Formatting.AQUA, Formatting.BOLD));
        
        // Add custom properties
        NbtCompound customData = new NbtCompound();
        
        // Add lore
        loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Ancient knowledge from beyond the stars").formatted(Formatting.GRAY), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Holds the wisdom of forgotten civilizations").formatted(Formatting.DARK_AQUA), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("A powerful artifact of divination").formatted(Formatting.LIGHT_PURPLE), wrapperLookup)));
        customData.put("Lore", loreList);
        
        // Special properties
        customData.putFloat("ExperienceBonus", 0.25f);
        customData.putBoolean("VisionPower", true);
        
        crystalSkull.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
        
        ingredients = Arrays.asList(
            new ItemStack(Items.DIAMOND, 1),
            new ItemStack(Items.QUARTZ, 3),
            new ItemStack(Items.AMETHYST_SHARD, 4),
            new ItemStack(Items.EMERALD, 2)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "crystal_skull",
                ingredients,
                crystalSkull,
                CulturalCraftingStation.StationType.MAYAN_ALTAR,
                180,
                300
            )
        );
    }
    
    /**
     * Register Medieval crafting recipes
     */
    private static void registerMedievalRecipes() {
        // Knight's Longsword
        ItemStack longsword = new ItemStack(Items.IRON_SWORD);
        
        // Add custom name
        longsword.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Knight's Longsword").formatted(Formatting.GRAY, Formatting.BOLD));
        
        // Add enchantments
        NbtCompound enchantmentData = new NbtCompound();
        NbtList enchantments = new NbtList();
        
        // Sharpness
        NbtCompound sharpness = new NbtCompound();
        sharpness.putString("id", "minecraft:sharpness");
        sharpness.putShort("lvl", (short)3);
        enchantments.add(sharpness);
        
        // Knockback
        NbtCompound knockback = new NbtCompound();
        knockback.putString("id", "minecraft:knockback");
        knockback.putShort("lvl", (short)2);
        enchantments.add(knockback);
        
        enchantmentData.put("Enchantments", enchantments);
        
        // Add lore
        NbtList loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Sword of a noble knight").formatted(Formatting.GRAY), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Blessed with honor and valor").formatted(Formatting.BLUE), wrapperLookup)));
        enchantmentData.put("Lore", loreList);
        
        // Special properties
        enchantmentData.putFloat("ReachBonus", 1.0f);
        
        longsword.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(enchantmentData));
        
        List<ItemStack> ingredients = Arrays.asList(
            new ItemStack(Items.IRON_INGOT, 3),
            new ItemStack(Items.DIAMOND, 1),
            new ItemStack(Items.STICK, 1),
            new ItemStack(Items.LEATHER, 1)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "knights_longsword",
                ingredients,
                longsword,
                CulturalCraftingStation.StationType.MEDIEVAL_SMITHY,
                120,
                180
            )
        );
        
        // Holy Grail
        ItemStack holyGrail = new ItemStack(Items.GOLDEN_APPLE);
        
        // Add custom name
        holyGrail.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Holy Grail").formatted(Formatting.YELLOW, Formatting.BOLD));
        
        // Add custom properties
        NbtCompound customData = new NbtCompound();
        
        // Add lore
        loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("A legendary artifact of immense power").formatted(Formatting.GRAY), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Grants healing to the worthy").formatted(Formatting.GREEN), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("The ultimate quest of knights").formatted(Formatting.GOLD), wrapperLookup)));
        customData.put("Lore", loreList);
        
        // Special properties
        customData.putInt("HealthBonus", 4);
        customData.putInt("HealingPower", 10);
        customData.putBoolean("RegenerationEffect", true);
        
        holyGrail.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
        
        ingredients = Arrays.asList(
            new ItemStack(Items.GOLD_BLOCK, 1),
            new ItemStack(Items.DIAMOND, 2),
            new ItemStack(Items.GOLDEN_APPLE, 1),
            new ItemStack(Items.GHAST_TEAR, 2)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "holy_grail",
                ingredients,
                holyGrail,
                CulturalCraftingStation.StationType.MEDIEVAL_SMITHY,
                200,
                320
            )
        );
        
        // Authentic Chainmail
        ItemStack chainmail = new ItemStack(Items.CHAINMAIL_CHESTPLATE);
        
        // Add custom name
        chainmail.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Authentic Chainmail").formatted(Formatting.GRAY));
        
        // Add enchantments
        enchantmentData = new NbtCompound();
        enchantments = new NbtList();
        
        // Protection
        NbtCompound protection = new NbtCompound();
        protection.putString("id", "minecraft:protection");
        protection.putShort("lvl", (short)3);
        enchantments.add(protection);
        
        // Unbreaking
        NbtCompound unbreaking = new NbtCompound();
        unbreaking.putString("id", "minecraft:unbreaking");
        unbreaking.putShort("lvl", (short)2);
        enchantments.add(unbreaking);
        
        enchantmentData.put("Enchantments", enchantments);
        
        // Add lore
        loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Handcrafted links of iron").formatted(Formatting.GRAY), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Light and flexible, yet strong").formatted(Formatting.GRAY), wrapperLookup)));
        enchantmentData.put("Lore", loreList);
        
        chainmail.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(enchantmentData));
        
        ingredients = Arrays.asList(
            new ItemStack(Items.IRON_NUGGET, 20),
            new ItemStack(Items.STRING, 5)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "authentic_chainmail",
                ingredients,
                chainmail,
                CulturalCraftingStation.StationType.MEDIEVAL_SMITHY,
                100,
                150
            )
        );
    }
    
    /**
     * Register special hybrid cultural recipes
     */
    private static void registerSpecialRecipes() {
        // Greco-Roman Helmet (Hybrid artifact)
        ItemStack corinthianHelmet = new ItemStack(Items.GOLDEN_HELMET);
        
        // Add custom name
        corinthianHelmet.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Corinthian Helmet").formatted(Formatting.GOLD, Formatting.BOLD));
        
        // Add enchantments
        NbtCompound enchantmentData = new NbtCompound();
        NbtList enchantments = new NbtList();
        
        // Protection
        NbtCompound protection = new NbtCompound();
        protection.putString("id", "minecraft:protection");
        protection.putShort("lvl", (short)4);
        enchantments.add(protection);
        
        // Respiration
        NbtCompound respiration = new NbtCompound();
        respiration.putString("id", "minecraft:respiration");
        respiration.putShort("lvl", (short)2);
        enchantments.add(respiration);
        
        enchantmentData.put("Enchantments", enchantments);
        
        // Add lore
        NbtList loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Blends Greek design with Roman craftsmanship").formatted(Formatting.GRAY), wrapperLookup)));
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Symbol of classical military might").formatted(Formatting.GOLD), wrapperLookup)));
        enchantmentData.put("Lore", loreList);
        
        // Cultural fusion properties
        enchantmentData.putBoolean("CulturalHybrid", true);
        enchantmentData.putString("PrimaryCulture", "greek");
        enchantmentData.putString("SecondaryCulture", "roman");
        enchantmentData.putFloat("StrengthBonus", 0.15f);
        
        corinthianHelmet.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(enchantmentData));
        
        List<ItemStack> ingredients = Arrays.asList(
            new ItemStack(Items.GOLD_INGOT, 5),
            new ItemStack(Items.IRON_INGOT, 3),
            new ItemStack(Items.RED_DYE, 2)
        );
        
        CulturalCraftingStation.RecipeRegistry.registerRecipe(
            new CulturalCraftingStation.CulturalRecipe(
                "corinthian_helmet",
                ingredients,
                corinthianHelmet,
                CulturalCraftingStation.StationType.GREEK_ACADEMY,
                160,
                240
            )
        );
    }
    
    /**
     * Helper method to add potion effect
     */
    private static void addPotionEffect(ItemStack stack, String potionId, String name) {
        // Add custom name using Components
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(Formatting.AQUA));
        
        // Add potion type using custom data component
        NbtCompound customData = new NbtCompound();
        customData.putString("Potion", potionId);
        
        // Add lore based on potion type
        NbtList loreList = new NbtList();
        if (potionId.contains("fire_resistance")) {
            loreList.add(NbtString.of(Text.Serialization.toJsonString(
                Text.literal("Provides protection from the desert heat").formatted(Formatting.GRAY), wrapperLookup)));
        } else if (potionId.contains("strength")) {
            loreList.add(NbtString.of(Text.Serialization.toJsonString(
                Text.literal("Enhances physical power").formatted(Formatting.GRAY), wrapperLookup)));
        }
        customData.put("Lore", loreList);
        
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
    }
    
    /**
     * Helper method to add firework effect
     */
    private static void addFireworkEffect(ItemStack stack, String name, byte flight, int[] colors) {
        // Add custom name using Components
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(Formatting.LIGHT_PURPLE));
        
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
        
        // Add lore
        NbtList loreList = new NbtList();
        loreList.add(NbtString.of(Text.Serialization.toJsonString(
            Text.literal("Celebratory fireworks for special occasions").formatted(Formatting.GRAY), wrapperLookup)));
        customData.put("Lore", loreList);
        
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
    }
    
    /**
     * Helper method to add enchanted book with custom enchantments
     */
    private static ItemStack createEnchantedBook(String name, Map<String, Integer> enchantments, List<String> lore) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        
        // Add custom name
        book.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        
        // Add enchantments
        NbtCompound customData = new NbtCompound();
        NbtList enchantmentList = new NbtList();
        
        for (Map.Entry<String, Integer> enchant : enchantments.entrySet()) {
            NbtCompound enchantNbt = new NbtCompound();
            enchantNbt.putString("id", enchant.getKey());
            enchantNbt.putShort("lvl", enchant.getValue().shortValue());
            enchantmentList.add(enchantNbt);
        }
        
        customData.put("StoredEnchantments", enchantmentList);
        
        // Add lore
        if (lore != null && !lore.isEmpty()) {
            NbtList loreList = new NbtList();
            for (String loreLine : lore) {
                loreList.add(NbtString.of(Text.Serialization.toJsonString(
                    Text.literal(loreLine).formatted(Formatting.GRAY), wrapperLookup)));
            }
            customData.put("Lore", loreList);
        }
        
        book.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customData));
        return book;
    }
}
