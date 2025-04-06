package com.beeny.village.crafting;

import com.beeny.village.util.DataComponentHelper;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.component.DataComponentTypes;
import java.util.*;
import net.minecraft.registry.BuiltinRegistries;

public class CulturalRecipeInitializer {
    
    public static void init() {
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

    private static void addCustomProperties(ItemStack stack, String name, Formatting color, List<String> loreLines) {
        // Set custom name
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(color));
        
        // Set lore
        if (!loreLines.isEmpty()) {
            List<Text> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(Text.literal(line).formatted(Formatting.GRAY));
            }
            DataComponentHelper.setLore(stack, lore);
        }
    }

    // Modified to pass String IDs directly to DataComponentHelper
    private static void addEnchantments(ItemStack stack, Map<String, Integer> enchantments) {
        if (!enchantments.isEmpty()) {
            DataComponentHelper.setEnchantmentsById(stack, enchantments); // Use new helper method
        }
    }

    private static void addPotionEffect(ItemStack stack, String potionId, String name) {
        // Set custom name
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(Formatting.AQUA));
        
        // Set potion effect
        DataComponentHelper.setPotionEffect(stack, potionId);
        
        // Set lore based on potion type
        List<Text> lore = new ArrayList<>();
        if (potionId.contains("fire_resistance")) {
            lore.add(Text.literal("Provides protection from the desert heat").formatted(Formatting.GRAY));
        } else if (potionId.contains("strength")) {
            lore.add(Text.literal("Enhances physical power").formatted(Formatting.GRAY));
        }
        DataComponentHelper.setLore(stack, lore);
    }

    private static void addFireworkEffect(ItemStack stack, String name, byte flight, int[] colors) {
        // Set custom name
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(Formatting.LIGHT_PURPLE));
        
        // Set firework effect
        DataComponentHelper.setFireworkEffect(stack, flight, colors);
        
        // Set lore
        List<Text> lore = Arrays.asList(
            Text.literal("Celebratory fireworks for special occasions").formatted(Formatting.GRAY)
        );
        DataComponentHelper.setLore(stack, lore);
    }

    private static void registerRomanRecipes() {
        // Roman Gladius
        ItemStack gladius = new ItemStack(Items.IRON_SWORD);
        
        Map<String, Integer> enchantments = new HashMap<>();
        enchantments.put("minecraft:sharpness", 3);
        enchantments.put("minecraft:unbreaking", 2);
        
        List<String> lore = Arrays.asList(
            "The weapon of Roman legionnaires",
            "Forged in the heart of Rome"
        );
        
        addCustomProperties(gladius, "Roman Gladius", Formatting.RED, lore);
        addEnchantments(gladius, enchantments);
        
        Map<String, Object> customData = new HashMap<>();
        customData.put("damage_bonus", 2);
        customData.put("attack_speed_bonus", 0.1f);
        DataComponentHelper.setCustomData(gladius, customData);

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
        
        enchantments = new HashMap<>();
        enchantments.put("minecraft:protection", 3);
        enchantments.put("minecraft:unbreaking", 2);
        
        lore = Arrays.asList(
            "Armor of the Imperial legions"
        );
        
        addCustomProperties(lorica, "Roman Lorica Segmentata", Formatting.RED, lore);
        addEnchantments(lorica, enchantments);
        
        DataComponentHelper.setCustomData(lorica, customData);

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
        
        enchantments = new HashMap<>();
        enchantments.put("minecraft:knockback", 1);
        enchantments.put("minecraft:thorns", 1);
        
        lore = Arrays.asList(
            "The shield of Roman formation tactics"
        );
        
        addCustomProperties(scutum, "Roman Scutum", Formatting.RED, lore);
        addEnchantments(scutum, enchantments);
        
        customData = new HashMap<>();
        customData.put("BlockStrength", 1.5f);
        DataComponentHelper.setCustomData(scutum, customData);

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
        
        Map<String, Integer> enchantments = new HashMap<>();
        enchantments.put("minecraft:fire_aspect", 2);
        enchantments.put("minecraft:power", 3);
        
        List<String> lore = Arrays.asList(
            "A symbol of royal authority",
            "Blessed by Ra, the sun god"
        );
        
        addCustomProperties(staff, "Pharaoh's Staff", Formatting.GOLD, lore);
        addEnchantments(staff, enchantments);
        
        Map<String, Object> customData = new HashMap<>();
        customData.put("damage_bonus", 2);
        customData.put("attack_speed_bonus", 0.1f);
        DataComponentHelper.setCustomData(staff, customData);

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
        
        lore = Arrays.asList(
            "Symbol of eternal life",
            "Grants second chance to its bearer"
        );
        
        addCustomProperties(ankh, "Ankh of Resurrection", Formatting.GOLD, lore);
        
        customData = new HashMap<>();
        customData.put("ResurrectionPower", 2);
        customData.put("HealthRestored", 15);
        DataComponentHelper.setCustomData(ankh, customData);
        
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
    
    private static void registerVictorianRecipes() {
        // Victorian Pocket Watch
        ItemStack pocketWatch = new ItemStack(Items.CLOCK);
        
        List<String> lore = Arrays.asList(
            "A finely crafted timepiece",
            "Keeps perfect time even in the most adverse conditions"
        );
        
        addCustomProperties(pocketWatch, "Victorian Pocket Watch", Formatting.GOLD, lore);
        
        Map<String, Object> customData = new HashMap<>();
        customData.put("IsPrecise", true);
        customData.put("ShowsPhase", true);
        DataComponentHelper.setCustomData(pocketWatch, customData);

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
        
        Map<String, Integer> enchantments = new HashMap<>();
        enchantments.put("minecraft:respiration", 2);
        
        lore = Arrays.asList(
            "Worn by explorers of the British Empire",
            "Provides extra visibility in harsh conditions"
        );
        
        addCustomProperties(explorerHat, "Victorian Explorer's Hat", Formatting.DARK_GREEN, lore);
        addEnchantments(explorerHat, enchantments);
        
        customData = new HashMap<>();
        customData.put("color", 5060991);
        customData.put("ExplorationBonus", 0.15f);
        DataComponentHelper.setCustomData(explorerHat, customData);
        
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
    
    private static void registerNYCRecipes() {
        // NYC-style Pizza
        ItemStack pizza = new ItemStack(Items.COOKED_BEEF);
        
        List<String> lore = Arrays.asList(
            "The best slice in town",
            "Authentic NYC flavor"
        );
        
        addCustomProperties(pizza, "NYC-Style Pizza", Formatting.RED, lore);
        
        Map<String, Object> customData = new HashMap<>();
        customData.put("FoodLevel", 8);
        customData.put("Saturation", 0.8f);
        DataComponentHelper.setCustomData(pizza, customData);

        List<ItemStack> ingredients = Arrays.asList(
            new ItemStack(Items.BREAD, 1),
            new ItemStack(Items.COOKED_BEEF, 1),
            new ItemStack(Items.RED_DYE, 1)
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
        
        lore = Arrays.asList(
            "Strong New York coffee",
            "Provides energy boost and speed"
        );
        
        addCustomProperties(coffee, "NYC Coffee", Formatting.DARK_PURPLE, lore);
        
        customData = new HashMap<>();
        customData.put("Effect", "minecraft:speed");
        customData.put("EffectDuration", 600);
        customData.put("FoodLevel", 3);
        customData.put("Saturation", 0.4f);
        DataComponentHelper.setCustomData(coffee, customData);
        
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
        
        lore = Arrays.asList(
            "Hail a cab from anywhere",
            "The sound carries through the busy streets"
        );
        
        addCustomProperties(taxiWhistle, "NYC Taxi Whistle", Formatting.YELLOW, lore);
        
        customData = new HashMap<>();
        customData.put("instrument", "taxi_call");
        DataComponentHelper.setCustomData(taxiWhistle, customData);
        
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
    
    private static void registerGreekRecipes() {
        // Hoplite Spear
        ItemStack spear = new ItemStack(Items.TRIDENT);
        
        Map<String, Integer> enchantments = new HashMap<>();
        enchantments.put("minecraft:impaling", 3);
        enchantments.put("minecraft:loyalty", 2);
        
        List<String> lore = Arrays.asList(
            "Weapon of the Greek phalanx",
            "Forged in the traditions of Sparta"
        );
        
        addCustomProperties(spear, "Hoplite Spear", Formatting.AQUA, lore);
        addEnchantments(spear, enchantments);
        
        Map<String, Object> customData = new HashMap<>();
        customData.put("damage_bonus", 2);
        customData.put("attack_speed_bonus", 0.1f);
        DataComponentHelper.setCustomData(spear, customData);
        
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
        
        enchantments = new HashMap<>();
        enchantments.put("minecraft:protection", 1);
        enchantments.put("minecraft:feather_falling", 3);
        
        lore = Arrays.asList(
            "Symbol of victory at the Olympic games",
            "Grants the speed and agility of a champion"
        );
        
        addCustomProperties(laurelCrown, "Olympic Laurel Crown", Formatting.GREEN, lore);
        addEnchantments(laurelCrown, enchantments);
        
        customData = new HashMap<>();
        customData.put("SpeedBonus", 0.2f);
        customData.put("JumpBonus", 0.1f);
        DataComponentHelper.setCustomData(laurelCrown, customData);
        
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
    
    private static void registerJapaneseRecipes() {
        // Samurai Katana
        ItemStack katana = new ItemStack(Items.NETHERITE_SWORD);
        
        Map<String, Integer> enchantments = new HashMap<>();
        enchantments.put("minecraft:sharpness", 5);
        enchantments.put("minecraft:sweeping", 3);
        enchantments.put("minecraft:mending", 1);
        
        List<String> lore = Arrays.asList(
            "Folded 1000 times",
            "The soul of a samurai dwells within"
        );
        
        addCustomProperties(katana, "Samurai Katana", Formatting.DARK_RED, lore);
        addEnchantments(katana, enchantments);
        
        Map<String, Object> customData = new HashMap<>();
        customData.put("AttackSpeedBonus", 0.3f);
        DataComponentHelper.setCustomData(katana, customData);
        
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
        
        lore = Arrays.asList(
            "A delicate light source",
            "Provides a calming atmosphere"
        );
        
        addCustomProperties(lantern, "Japanese Paper Lantern", Formatting.LIGHT_PURPLE, lore);
        
        customData = new HashMap<>();
        customData.put("LightLevel", 14);
        customData.put("PeacefulAura", true);
        DataComponentHelper.setCustomData(lantern, customData);
        
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
    
    private static void registerMayanRecipes() {
        // Obsidian Sacrificial Blade
        ItemStack obsidianBlade = new ItemStack(Items.GOLDEN_SWORD);
        
        Map<String, Integer> enchantments = new HashMap<>();
        enchantments.put("minecraft:smite", 5);
        enchantments.put("minecraft:looting", 3);
        
        List<String> lore = Arrays.asList(
            "Used in sacred Mayan rituals",
            "A blade that thirsts for offerings"
        );
        
        addCustomProperties(obsidianBlade, "Obsidian Ceremonial Blade", Formatting.DARK_PURPLE, lore);
        addEnchantments(obsidianBlade, enchantments);
        
        Map<String, Object> customData = new HashMap<>();
        customData.put("BleedingEffect", true);
        customData.put("RitualBonus", 0.5f);
        DataComponentHelper.setCustomData(obsidianBlade, customData);
        
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
        
        lore = Arrays.asList(
            "Ancient knowledge from beyond the stars",
            "Holds the wisdom of forgotten civilizations",
            "A powerful artifact of divination"
        );
        
        addCustomProperties(crystalSkull, "Mayan Crystal Skull", Formatting.AQUA, lore);
        
        customData = new HashMap<>();
        customData.put("ExperienceBonus", 0.25f);
        customData.put("VisionPower", true);
        DataComponentHelper.setCustomData(crystalSkull, customData);
        
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
    
    private static void registerMedievalRecipes() {
        // Knight's Longsword
        ItemStack longsword = new ItemStack(Items.IRON_SWORD);
        
        Map<String, Integer> enchantments = new HashMap<>();
        enchantments.put("minecraft:sharpness", 3);
        enchantments.put("minecraft:knockback", 2);
        
        List<String> lore = Arrays.asList(
            "Sword of a noble knight",
            "Blessed with honor and valor"
        );
        
        addCustomProperties(longsword, "Knight's Longsword", Formatting.GRAY, lore);
        addEnchantments(longsword, enchantments);
        
        Map<String, Object> customData = new HashMap<>();
        customData.put("ReachBonus", 1.0f);
        DataComponentHelper.setCustomData(longsword, customData);
        
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
        
        lore = Arrays.asList(
            "A legendary artifact of immense power",
            "Grants healing to the worthy",
            "The ultimate quest of knights"
        );
        
        addCustomProperties(holyGrail, "Holy Grail", Formatting.YELLOW, lore);
        
        customData = new HashMap<>();
        customData.put("HealthBonus", 4);
        customData.put("HealingPower", 10);
        customData.put("RegenerationEffect", true);
        DataComponentHelper.setCustomData(holyGrail, customData);
        
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
        
        enchantments = new HashMap<>();
        enchantments.put("minecraft:protection", 3);
        enchantments.put("minecraft:unbreaking", 2);
        
        lore = Arrays.asList(
            "Handcrafted links of iron",
            "Light and flexible, yet strong"
        );
        
        addCustomProperties(chainmail, "Authentic Chainmail", Formatting.GRAY, lore);
        addEnchantments(chainmail, enchantments);
        
        customData = new HashMap<>();
        customData.put("damage_bonus", 2);
        customData.put("attack_speed_bonus", 0.1f);
        DataComponentHelper.setCustomData(chainmail, customData);
        
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
    
    private static void registerSpecialRecipes() {
        // Greco-Roman Helmet (Hybrid artifact)
        ItemStack corinthianHelmet = new ItemStack(Items.GOLDEN_HELMET);
        
        Map<String, Integer> enchantments = new HashMap<>();
        enchantments.put("minecraft:protection", 4);
        enchantments.put("minecraft:respiration", 2);
        
        List<String> lore = Arrays.asList(
            "Blends Greek design with Roman craftsmanship",
            "Symbol of classical military might"
        );
        
        addCustomProperties(corinthianHelmet, "Corinthian Helmet", Formatting.GOLD, lore);
        addEnchantments(corinthianHelmet, enchantments);
        
        Map<String, Object> customData = new HashMap<>();
        customData.put("CulturalHybrid", true);
        customData.put("PrimaryCulture", "greek");
        customData.put("SecondaryCulture", "roman");
        customData.put("StrengthBonus", 0.15f);
        DataComponentHelper.setCustomData(corinthianHelmet, customData);
        
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
}
