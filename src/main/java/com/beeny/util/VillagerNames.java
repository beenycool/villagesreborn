package com.beeny.util;

import net.minecraft.village.VillagerProfession;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.biome.Biome;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.List;
import java.util.Arrays;
import java.util.Optional;

public class VillagerNames {
    // Private constructor to prevent instantiation
    private VillagerNames() {}
    
    private static final Random RANDOM = new Random();
    
    private static final Map<Identifier, String> PROFESSION_TITLES = new HashMap<>();
    private static final Map<Identifier, String[]> PROFESSION_SURNAMES = new HashMap<>();
    
    static {
        // Profession titles (prefixes)
        PROFESSION_TITLES.put(VillagerProfession.ARMORER.getValue(), "Smith");
        PROFESSION_TITLES.put(VillagerProfession.BUTCHER.getValue(), "Butcher");
        PROFESSION_TITLES.put(VillagerProfession.CARTOGRAPHER.getValue(), "Cartographer");
        PROFESSION_TITLES.put(VillagerProfession.CLERIC.getValue(), "Cleric");
        PROFESSION_TITLES.put(VillagerProfession.FARMER.getValue(), "Farmer");
        PROFESSION_TITLES.put(VillagerProfession.FISHERMAN.getValue(), "Fisher");
        PROFESSION_TITLES.put(VillagerProfession.FLETCHER.getValue(), "Fletcher");
        PROFESSION_TITLES.put(VillagerProfession.LEATHERWORKER.getValue(), "Leatherworker");
        PROFESSION_TITLES.put(VillagerProfession.LIBRARIAN.getValue(), "Scholar");
        PROFESSION_TITLES.put(VillagerProfession.MASON.getValue(), "Mason");
        PROFESSION_TITLES.put(VillagerProfession.SHEPHERD.getValue(), "Shepherd");
        PROFESSION_TITLES.put(VillagerProfession.TOOLSMITH.getValue(), "Toolsmith");
        PROFESSION_TITLES.put(VillagerProfession.WEAPONSMITH.getValue(), "Weaponsmith");
        
        // Profession-themed surnames - now arrays for variety
        PROFESSION_SURNAMES.put(VillagerProfession.ARMORER.getValue(), new String[]{
            "Ironforge", "Steelhand", "Anvilborn", "Hammerfall", "Forgeheart"
        });
        PROFESSION_SURNAMES.put(VillagerProfession.BUTCHER.getValue(), new String[]{
            "Meatworth", "Cleaver", "Carver", "Tenderloin", "Primecut"
        });
        PROFESSION_SURNAMES.put(VillagerProfession.CARTOGRAPHER.getValue(), new String[]{
            "Mapsworth", "Chartwell", "Compass", "Atlas", "Globemaker"
        });
        PROFESSION_SURNAMES.put(VillagerProfession.CLERIC.getValue(), new String[]{
            "Holyworth", "Sanctum", "Divine", "Blessed", "Sacred"
        });
        PROFESSION_SURNAMES.put(VillagerProfession.FARMER.getValue(), new String[]{
            "Fieldsworth", "Greenfield", "Harvest", "Cropwell", "Farmington", 
            "Meadows", "Barley", "Wheatworth", "Cornwell", "Orchard"
        });
        PROFESSION_SURNAMES.put(VillagerProfession.FISHERMAN.getValue(), new String[]{
            "Fishworth", "Netcaster", "Seaworthy", "Gill", "Riverborn"
        });
        PROFESSION_SURNAMES.put(VillagerProfession.FLETCHER.getValue(), new String[]{
            "Arrowworth", "Bowyer", "Stringhand", "Feather", "Quill"
        });
        PROFESSION_SURNAMES.put(VillagerProfession.LEATHERWORKER.getValue(), new String[]{
            "Hideford", "Tanner", "Leather", "Skin", "Hidebound"
        });
        PROFESSION_SURNAMES.put(VillagerProfession.LIBRARIAN.getValue(), new String[]{
            "Bookworth", "Scroll", "Parchment", "Inkwell", "Lorekeeper"
        });
        PROFESSION_SURNAMES.put(VillagerProfession.MASON.getValue(), new String[]{
            "Stoneford", "Rockwell", "Bricklayer", "Quarry", "Mason"
        });
        PROFESSION_SURNAMES.put(VillagerProfession.SHEPHERD.getValue(), new String[]{
            "Woolworth", "Flock", "Shepherd", "Ramsey", "Lambkin"
        });
        PROFESSION_SURNAMES.put(VillagerProfession.TOOLSMITH.getValue(), new String[]{
            "Toolford", "Craftsman", "Workhand", "Instrument", "Smithy"
        });
        PROFESSION_SURNAMES.put(VillagerProfession.WEAPONSMITH.getValue(), new String[]{
            "Bladeworth", "Swordhand", "Steel", "Weapon", "Forged"
        });
    }
    
    // Regional name pools based on biomes - now separated by gender
    private static final Map<String, String[]> REGIONAL_MALE_NAMES = new HashMap<>();
    private static final Map<String, String[]> REGIONAL_FEMALE_NAMES = new HashMap<>();
    private static final Map<String, String[]> REGIONAL_SURNAMES = new HashMap<>();
    
    static {
        // Plains names - traditional English
        REGIONAL_MALE_NAMES.put("plains", new String[]{
            "William", "James", "John", "Robert", "Charles", "George", "Thomas", "Henry",
            "Edward", "Richard", "Joseph", "David", "Michael", "Peter", "Paul", "Daniel"
        });
        REGIONAL_FEMALE_NAMES.put("plains", new String[]{
            "Elizabeth", "Mary", "Sarah", "Margaret", "Dorothy", "Helen", "Florence", "Ruth",
            "Alice", "Clara", "Emma", "Grace", "Rose", "Lily", "Anne", "Jane"
        });
        REGIONAL_SURNAMES.put("plains", new String[]{
            "Greenfield", "Meadows", "Rivers", "Hills", "Fields", "Brooks", "Woods", "Stone",
            "Carter", "Miller", "Taylor", "Cooper", "Smith", "Baker", "Clark", "Walker"
        });
        
        // Desert names - Arabic/Desert themed
        REGIONAL_MALE_NAMES.put("desert", new String[]{
            "Omar", "Hassan", "Ali", "Khalid", "Ibrahim", "Yusuf", "Ahmed", "Karim",
            "Rashid", "Tariq", "Nasir", "Jamal", "Farid", "Samir", "Nadir", "Rafiq"
        });
        REGIONAL_FEMALE_NAMES.put("desert", new String[]{
            "Aisha", "Fatima", "Zara", "Layla", "Noor", "Amira", "Sara", "Leila",
            "Yasmin", "Samira", "Nadia", "Hana", "Rania", "Dalia", "Salma", "Amina"
        });
        REGIONAL_SURNAMES.put("desert", new String[]{
            "Sandwalker", "Duneborn", "Sunscar", "Heatforge", "Oasis", "Camelstride", "Dustwind", "Mirage",
            "Al-Rashid", "Ibn-Khalid", "Al-Farid", "Qasim", "Al-Mansur", "Al-Hakim", "Smith"
        });
        
        // Taiga names - Nordic/Forest themed
        REGIONAL_MALE_NAMES.put("taiga", new String[]{
            "Bjorn", "Leif", "Erik", "Olaf", "Sven", "Lars", "Gunnar", "Rolf",
            "Thor", "Odin", "Magnus", "Harald", "Ivar", "Sigurd", "Knut", "Arne"
        });
        REGIONAL_FEMALE_NAMES.put("taiga", new String[]{
            "Astrid", "Freya", "Ingrid", "Greta", "Helga", "Birgit", "Sigrid", "Anja",
            "Liv", "Elsa", "Thyra", "Asta", "Ragna", "Solveig", "Gudrun", "Astrid"
        });
        REGIONAL_SURNAMES.put("taiga", new String[]{
            "Frostborn", "Pineheart", "Snowstride", "Winterwolf", "Iceforge", "Coldstone", "Evergreen", "Northwind",
            "Stormborn", "Frostmane", "Snowdrift", "Winterhold", "Glacierheart", "Blizzardborn"
        });
        
        // Savanna names - African/Savanna themed
        REGIONAL_MALE_NAMES.put("savanna", new String[]{
            "Kwame", "Jabari", "Baraka", "Chike", "Tafari", "Omari", "Kofi", "Hasani",
            "Malik", "Jabari", "Amare", "Zuberi", "Tau", "Kito", "Simba", "Jengo"
        });
        REGIONAL_FEMALE_NAMES.put("savanna", new String[]{
            "Amara", "Zuri", "Imani", "Nia", "Amina", "Sanaa", "Eshe", "Dalila",
            "Zola", "Kesi", "Ayana", "Zalika", "Nala", "Safiya", "Zuri", "Asha"
        });
        REGIONAL_SURNAMES.put("savanna", new String[]{
            "Lionstride", "Grassrunner", "Sunwatcher", "Acaciaborn", "Heatseeker", "Plainshoof", "Goldensavanna", "Wildmane",
            "Sunrunner", "Prideborn", "Savannawalker", "Dusthoof", "Heatborn", "Wildstride"
        });
        
        // Snow names - Arctic themed
        REGIONAL_MALE_NAMES.put("snow", new String[]{
        REGIONAL_MALE_NAMES.put("snow", new String[]{
            "Frost", "Blizzard", "Ice", "Winter", "Glacier", "Arctic", "North", "Polar",
            "Storm", "Boreas", "Crystal", "Snow", "Chill", "Tundra", "Yukon", "Icicle"
        REGIONAL_FEMALE_NAMES.put("snow", new String[]{
            "Crystal", "Snow", "Winter", "Aurora", "Frosty", "Icicle", "Neva", "Glacier",
            "Frostine", "Snowflake", "Neve", "Arctic", "Blizzara", "Freya", "Ice", "Polar"
        });
        });
        REGIONAL_SURNAMES.put("snow", new String[]{
            "Iceborn", "Frostmane", "Snowdrift", "Winterhold", "Glacierheart", "Blizzardborn", "Northwind", "Everfrost",
        REGIONAL_MALE_NAMES.put("jungle", new String[]{
            "River", "Kai", "Rio", "Jade", "Forest", "Rain", "Zephyr", "Moss",
            "Fern", "Basil", "Sage", "Leo", "Felix", "Cruz", "Diego", "Emerald"
        });
        REGIONAL_FEMALE_NAMES.put("jungle", new String[]{
            "River", "Maya", "Luna", "Jade", "Emerald", "Flora", "Rain", "Selva",
            "Orchid", "Rosa", "Lily", "Fern", "Paloma", "Coco", "Iris", "Tigris"
        });
        REGIONAL_FEMALE_NAMES.put("jungle", new String[]{
            "River", "Leaf", "Vine", "Jade", "Emerald", "Forest", "Rain", "Tropical",
            "Orchid", "Parrot", "Lily", "Fern", "Palm", "Coconut", "Moss", "Tiger"
        });
        REGIONAL_SURNAMES.put("jungle", new String[]{
            "Vineborn", "Canopywalker", "Rainseeker", "Jungleheart", "Emeraldleaf", "Tropicalbloom", "Wildvine", "Parrotcaller",
            "Leafwalker", "Vineheart", "Canopyborn", "Rainforest", "Wildbloom", "Junglestride"
        });
        
        // Forest names - Woodland themed
        REGIONAL_MALE_NAMES.put("forest", new String[]{
            "Rowan", "Ash", "Oak", "Birch", "Elm", "Pine", "Cedar", "Hazel",
            "Forest", "Wood", "Leaf", "Branch", "Root", "Moss", "Fern", "Thorn"
        });
        REGIONAL_FEMALE_NAMES.put("forest", new String[]{
            "Willow", "Hazel", "Ivy", "Rose", "Lily", "Fern", "Moss", "Laurel",
            "Forest", "Wood", "Leaf", "Branch", "Root", "Blossom", "Petal", "Thorn"
        });
        REGIONAL_SURNAMES.put("forest", new String[]{
            "Woods", "Forest", "Greenleaf", "Oakheart", "Wildwood", "Thornfield", "Mossborn", "Branchwalker",
            "Treeborn", "Leafwhisper", "Rootkeeper", "Forestguard", "Woodland", "Natureborn"
        });
        
        REGIONAL_MALE_NAMES.put("swamp", new String[]{
            "Bogdan", "Marsh", "Reed", "Willow", "Moss", "Fen", "Wade", "Glen",
            "Brook", "Sedge", "Rush", "Cypress", "Alder", "Pike", "Heron", "Drake"
        });
        REGIONAL_FEMALE_NAMES.put("swamp", new String[]{
            "Lily", "Reed", "Willow", "Moss", "Fern", "Iris", "Brook", "Misty",
            "Brooke", "Marsh", "Ivy", "Sage", "River", "Marina", "Delta", "Bayou"
        });
        });
        REGIONAL_SURNAMES.put("swamp", new String[]{
            "Bogwalker", "Marshborn", "Swampdweller", "Mirefoot", "Reedwhisper", "Willowshade", "Fenwalker", "Murkwater",
            "Sludgeborn", "Dankdweller", "Mudfoot", "Gloomhunter", "Wetlands", "Bogborn"
        });
        
        // Mountain names - Highland themed
        REGIONAL_MALE_NAMES.put("mountain", new String[]{
            "Rock", "Stone", "Cliff", "Peak", "Summit", "Ridge", "Craig", "Glen",
            "High", "Tall", "Mighty", "Strong", "Iron", "Steel", "Granite", "Basalt"
        });
        REGIONAL_FEMALE_NAMES.put("mountain", new String[]{
            "Crystal", "Ruby", "Emerald", "Sapphire", "Topaz", "Jade", "Pearl", "Opal",
            "High", "Tall", "Mighty", "Strong", "Stone", "Rock", "Cliff", "Peak"
        });
        REGIONAL_SURNAMES.put("mountain", new String[]{
            "Stonepeak", "Highmountain", "Rockborn", "Cliffdweller", "Summitwalker", "Ridgeguard", "Peakborn", "Stoneheart",
            "Mountainborn", "Highlander", "Cragwalker", "Rockforge", "Graniteborn", "Peakkeeper"
        });
        
        // Coastal names - Ocean themed
        REGIONAL_MALE_NAMES.put("coastal", new String[]{
            "Wave", "Tide", "Ocean", "Sea", "Coral", "Pearl", "Shell", "Beach",
            "Mariner", "Sailor", "Captain", "Anchor", "Harbor", "Bay", "Cove", "Reef"
        });
        REGIONAL_FEMALE_NAMES.put("coastal", new String[]{
            "Coral", "Pearl", "Shell", "Wave", "Tide", "Ocean", "Sea", "Marina",
            "Aqua", "Marine", "Shelly", "Sandy", "Beach", "Cove", "Bay", "Reef"
        });
        REGIONAL_SURNAMES.put("coastal", new String[]{
            "Seaworth", "Tideborn", "Oceanheart", "Coastwalker", "Seafarer", "Shellborn", "Coralguard", "Wavebreaker",
            "Harborborn", "Mariner", "Seashore", "Saltwater", "Beachcomber", "Reefdweller"
        });
        
        // Default names if biome not found
        REGIONAL_MALE_NAMES.put("default", new String[]{
            "Alex", "Oliver", "William", "James", "Benjamin", "Lucas", "Henry", "Alexander",
            "Daniel", "Matthew", "David", "Joseph", "Carter", "Samuel", "Nathan", "Ryan"
        });
        REGIONAL_FEMALE_NAMES.put("default", new String[]{
            "Emma", "Sophia", "Ava", "Isabella", "Charlotte", "Amelia", "Mia", "Harper",
            "Evelyn", "Abigail", "Emily", "Elizabeth", "Ella", "Scarlett", "Grace", "Victoria"
        });
        REGIONAL_SURNAMES.put("default", new String[]{
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
            "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas"
        });
    }

    private static final String[] UNIVERSAL_FIRST_NAMES = {
        "Alex", "Emma", "Oliver", "Sophia", "William", "Ava", "James", "Isabella",
        "Benjamin", "Charlotte", "Lucas", "Amelia", "Henry", "Mia", "Alexander", "Harper"
    };

    public static String generateNameForProfession(RegistryKey<VillagerProfession> profession, World world, BlockPos pos) {
        String biomeName = getBiomeName(world, pos);
        String gender = determineGenderFromProfession(profession);
        String firstName = getRegionalFirstName(biomeName, gender);
        String surname = getSurnameForProfession(profession, biomeName);
        
        String professionTitle = PROFESSION_TITLES.get(profession.getValue());
        
        if (professionTitle == null) {
            // For unemployed villagers or nitwits, just use first name + surname
            return firstName + " " + surname;
        }
        
        return professionTitle + " " + firstName + " " + surname;
    }
    
    public static String updateProfessionInName(String currentName, RegistryKey<VillagerProfession> newProfession, World world, BlockPos pos) {
        String newProfessionTitle = PROFESSION_TITLES.get(newProfession.getValue());
        String biomeName = getBiomeName(world, pos);
        
        // Extract the first name and surname from the current name
        String[] nameParts = parseName(currentName);
        String firstName = nameParts[0];
        String surname = nameParts[1];
        
        // Generate new surname based on new profession
        String newSurname = getSurnameForProfession(newProfession, biomeName);
        
        if (newProfessionTitle == null) {
            // No profession title (unemployed villagers, nitwits) - just use first name + surname
            return firstName + " " + newSurname;
        }
        
        return newProfessionTitle + " " + firstName + " " + newSurname;
    }
    
    private static String[] parseName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            String biomeName = "default";
            String gender = "male"; // Default fallback
            String firstName = getRegionalFirstName(biomeName, gender);
            String surname = getRegionalSurname(biomeName);
            return new String[]{firstName, surname};
        }
        
        String[] parts = fullName.trim().split("\\s+");
        
        if (parts.length == 1) {
            // Only first name provided
            String biomeName = "default";
            String surname = getRegionalSurname(biomeName);
            return new String[]{parts[0], surname};
        } else if (parts.length == 2) {
            // First name + surname
            return new String[]{parts[0], parts[1]};
        } else if (parts.length >= 3) {
            // Has profession title + first name + surname
            return new String[]{parts[1], parts[2]};
        }
        
        // Fallback
        String biomeName = "default";
        String gender = "male";
        String firstName = getRegionalFirstName(biomeName, gender);
// Add to the top of src/main/java/com/beeny/util/VillagerNames.java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VillagerNames {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerNames.class);
    private static final Random RANDOM = new Random();

    private static String getBiomeName(World world, BlockPos pos) {
        if (world == null || pos == null) {
-            System.out.println("[VillagerNames] World or pos is null, using default biome");
+            LOGGER.debug("World or pos is null, using default biome");
            return "default";
        }
        
        try {
            var biomeEntry = world.getBiome(pos);
            var optionalBiomeKey = biomeEntry.getKey();
            
            if (optionalBiomeKey.isEmpty()) {
                System.out.println("[VillagerNames] Biome key is empty, using default biome");
                return "default";
            }
            
            Identifier biomeId = optionalBiomeKey.get().getValue();
            String biomePath = biomeId.getPath().toLowerCase();
            System.out.println("[VillagerNames] Detected biome: " + biomePath + " at position " + pos);
            
            String biomeCategory;
            if (biomePath.contains("plains") || biomePath.contains("meadow") || biomePath.contains("sunflower")) {
                biomeCategory = "plains";
            } else if (biomePath.contains("forest") || biomePath.contains("birch") || biomePath.contains("dark_forest") || biomePath.contains("flower_forest")) {
                biomeCategory = "forest";
            } else if (biomePath.contains("taiga") || biomePath.contains("grove") || biomePath.contains("snowy_taiga")) {
                biomeCategory = "taiga";
            } else if (biomePath.contains("desert") || biomePath.contains("badlands") || biomePath.contains("mesa")) {
                biomeCategory = "desert";
            } else if (biomePath.contains("savanna")) {
                biomeCategory = "savanna";
            } else if (biomePath.contains("jungle") || biomePath.contains("bamboo")) {
                biomeCategory = "jungle";
            } else if (biomePath.contains("swamp") || biomePath.contains("mangrove")) {
                biomeCategory = "swamp";
            } else if (biomePath.contains("snow") || biomePath.contains("ice") || biomePath.contains("frozen")) {
                biomeCategory = "snow";
            } else if (biomePath.contains("mountain") || biomePath.contains("peak") || biomePath.contains("slope")) {
                biomeCategory = "mountain";
            } else if (biomePath.contains("ocean") || biomePath.contains("beach") || biomePath.contains("river")) {
                biomeCategory = "coastal";
            } else {
                biomeCategory = "default";
            }
            
            System.out.println("[VillagerNames] Biome '" + biomePath + "' categorized as: " + biomeCategory);
            return biomeCategory;
-        } catch (Exception e) {
+        } catch (NullPointerException | IllegalStateException e) {
+            LOGGER.warn("Failed to detect biome at position {}: {}", pos, e.getMessage());
            return "default";
        }
    }
    // ...
}
            } else {
                biomeCategory = "default";
            }
            
            System.out.println("[VillagerNames] Biome '" + biomePath + "' categorized as: " + biomeCategory);
            return biomeCategory;
-        } catch (Exception e) {
+        } catch (NullPointerException | IllegalStateException e) {
+            LOGGER.warn("Failed to detect biome at position {}: {}", pos, e.getMessage());
            return "default";
        }
    }
    // ...
}
            return biomeCategory;
        } catch (Exception e) {
            return "default";
        }
    }
    
    private static String determineGenderFromProfession(RegistryKey<VillagerProfession> profession) {
        // Simple 50/50 split based on profession hash
        return Math.abs(profession.getValue().toString().hashCode()) % 2 == 0 ? "male" : "female";
    }
    
    private static String getRegionalFirstName(String biomeName, String gender) {
        String[] names;
        
        if ("male".equals(gender)) {
            names = REGIONAL_MALE_NAMES.getOrDefault(biomeName, REGIONAL_MALE_NAMES.get("default"));
        } else {
            names = REGIONAL_FEMALE_NAMES.getOrDefault(biomeName, REGIONAL_FEMALE_NAMES.get("default"));
        }
        
        if (names == null || names.length == 0) {
            names = UNIVERSAL_FIRST_NAMES;
        }
        
        return names[RANDOM.nextInt(names.length)];
    }
    
    private static String getRegionalSurname(String biomeName) {
        String[] surnames = REGIONAL_SURNAMES.getOrDefault(biomeName, REGIONAL_SURNAMES.get("default"));
        
        if (surnames == null || surnames.length == 0) {
            surnames = new String[]{"Smith", "Johnson", "Williams", "Brown"};
        }
        
        return surnames[RANDOM.nextInt(surnames.length)];
    }
    
    private static String getSurnameForProfession(RegistryKey<VillagerProfession> profession, String biomeName) {
        String[] professionSurnames = PROFESSION_SURNAMES.get(profession.getValue());
        
        if (professionSurnames != null && professionSurnames.length > 0) {
            return professionSurnames[RANDOM.nextInt(professionSurnames.length)];
        }
        
        // Fallback to regional surnames
        return getRegionalSurname(biomeName);
    }
    
    public static String generateRandomName(World world, BlockPos pos) {
        String biomeName = getBiomeName(world, pos);
        String gender = RANDOM.nextBoolean() ? "male" : "female";
        String firstName = getRegionalFirstName(biomeName, gender);
        String surname = getRegionalSurname(biomeName);
        
        return firstName + " " + surname;
    }
}