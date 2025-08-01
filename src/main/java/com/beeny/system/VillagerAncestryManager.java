package com.beeny.system;

import com.beeny.data.VillagerData;
import com.beeny.util.VillagerNames;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class VillagerAncestryManager {
    
    // Historical time periods (years ago from current time)
    private static final Map<String, int[]> HISTORICAL_PERIODS = Map.of(
        "Ancient Times", new int[]{200, 500},
        "Old Kingdom", new int[]{100, 200},
        "Classical Era", new int[]{50, 100},
        "Recent Past", new int[]{20, 50},
        "Living Memory", new int[]{5, 20}
    );
    
    private static final String[] ANCIENT_PROFESSIONS = {
        "Ancient Farmer", "Village Elder", "Stone Mason", "Herb Gatherer", 
        "Beast Hunter", "Sacred Keeper", "Star Reader", "Memory Keeper",
        "Iron Forger", "Wool Weaver", "Grain Miller", "Well Digger"
    };
    
    private static final String[] HISTORICAL_EVENTS = {
        "Great Drought of", "Village Founding in", "The Iron Discovery of",
        "Trading Route Established in", "Great Harvest of", "The Settlement Wars of",
        "Elder Council Formation of", "Sacred Grove Planted in", "First Market of",
        "The Unity Pact of", "Golden Season of", "Peace Treaty of"
    };
    
    /**
     * Generate fictional ancestors for a villager up to a specified number of generations
     */
    public static Map<String, VillagerData> generateAncestors(VillagerEntity villager, int generations) {
        Map<String, VillagerData> ancestors = new HashMap<>();
        VillagerData villagerData = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        
        if (villagerData == null || generations <= 0) return ancestors;
        
        // Generate parents first
        List<VillagerData> parents = generateParents(villager, villagerData);
        for (VillagerData parent : parents) {
            ancestors.put(parent.getName(), parent);
        }
        
        // Generate grandparents and beyond
        if (generations > 1) {
            for (VillagerData parent : parents) {
                Map<String, VillagerData> parentAncestors = generateAncestorsRecursive(parent, generations - 1);
                ancestors.putAll(parentAncestors);
            }
        }
        
        return ancestors;
    }
    
    private static Map<String, VillagerData> generateAncestorsRecursive(VillagerData child, int remainingGenerations) {
        Map<String, VillagerData> ancestors = new HashMap<>();
        
        if (remainingGenerations <= 0) return ancestors;
        
        // Generate parents for this child
        List<VillagerData> parents = generateParentsFromData(child);
        for (VillagerData parent : parents) {
            ancestors.put(parent.getName(), parent);
        }
        
        // Continue recursively
        if (remainingGenerations > 1) {
            for (VillagerData parent : parents) {
                Map<String, VillagerData> parentAncestors = generateAncestorsRecursive(parent, remainingGenerations - 1);
                ancestors.putAll(parentAncestors);
            }
        }
        
        return ancestors;
    }
    
    private static List<VillagerData> generateParents(VillagerEntity villager, VillagerData villagerData) {
        List<VillagerData> parents = new ArrayList<>();
        World world = villager.getWorld();
        BlockPos pos = villager.getBlockPos();
        
        // Generate mother
        VillagerData mother = generateAncestor(villagerData, "female", 1, world, pos);
        parents.add(mother);
        
        // Generate father
        VillagerData father = generateAncestor(villagerData, "male", 1, world, pos);
        parents.add(father);
        
        // Make them married to each other
        mother.marry(father.getName(), "ancestor_" + father.getName().hashCode());
        father.marry(mother.getName(), "ancestor_" + mother.getName().hashCode());
        
        // Add child relationship
        mother.addChild(villager.getUuidAsString());
        father.addChild(villager.getUuidAsString());
        
        return parents;
    }
    
    private static List<VillagerData> generateParentsFromData(VillagerData child) {
        List<VillagerData> parents = new ArrayList<>();
        
        // Calculate generation level based on birth time
        // Use a fixed time reference for consistency
        long currentTime = System.currentTimeMillis(); // In a real implementation, this would use world.getTime()
        long childAge = currentTime - child.getBirthTime();
        int generationLevel = (int)(childAge / (1000L * 60 * 60 * 24 * 365 * 25)) + 1; // 25 years per generation
        
        // Generate mother
        VillagerData mother = generateAncestorFromChild(child, "female", generationLevel);
        parents.add(mother);
        
        // Generate father  
        VillagerData father = generateAncestorFromChild(child, "male", generationLevel);
        parents.add(father);
        
        // Make them married
        mother.marry(father.getName(), "ancestor_" + father.getName().hashCode());
        father.marry(mother.getName(), "ancestor_" + mother.getName().hashCode());
        
        return parents;
    }
    
    private static VillagerData generateAncestor(VillagerData descendant, String gender, int generationsBack, World world, BlockPos pos) {
        Random random = ThreadLocalRandom.current();
        
        // Generate name based on region
        String name = VillagerNames.generateNameForProfession(world, pos);
        if (gender.equals("female")) {
            // Simple way to feminize names - could be enhanced
            name = generateFeminineName(name, world, pos);
        }
        
        // Calculate birth time (years ago)
        long yearsAgoMin = generationsBack * 20 + random.nextInt(10);
        long yearsAgoMax = generationsBack * 35 + random.nextInt(15);
        long yearsAgo = yearsAgoMin + random.nextInt((int)(yearsAgoMax - yearsAgoMin + 1));
        // Use a fixed time reference for consistency
        long birthTime = System.currentTimeMillis() - (yearsAgo * 365L * 24 * 60 * 60 * 1000); // In a real implementation, this would use world.getTime()
        
        // Generate personality influenced by descendant
        String personality = generateInheritedPersonality(descendant.getPersonality(), random);
        
        // Age and other attributes
        int age = (int)(yearsAgo * 365 + random.nextInt(365 * 5)); // Add some variation
        int happiness = 40 + random.nextInt(60); // Ancestors were generally content
        
        // Historical profession
        String profession = getHistoricalProfession(generationsBack, random);
        List<String> professionHistory = List.of(profession);
        
        // Generate historical birth place
        String birthPlace = generateHistoricalBirthPlace(generationsBack, world, pos, random);
        
        // Create ancestor data
        VillagerData ancestor = new VillagerData(
            name, age, gender, personality, happiness, 
            random.nextInt(50), "", professionHistory,
            new HashMap<>(), new ArrayList<>(), "", "",
            new ArrayList<>(), new ArrayList<>(), 
            "", getHistoricalHobby(random), birthTime, birthPlace,
            generateHistoricalNotes(generationsBack, random), 
            birthTime + (65L + random.nextInt(20)) * 365 * 24 * 60 * 60 * 1000, // Death time (lived 65-85 years)
            false // Ancestors are not alive
        );
        
        return ancestor;
    }
    
    private static VillagerData generateAncestorFromChild(VillagerData child, String gender, int generationLevel) {
        Random random = ThreadLocalRandom.current();
        
        // Generate name with similar cultural background
        String name = generateCulturalName(child.getName(), gender, random);
        
        // Calculate birth time based on generation
        long parentAgeAtBirth = 20 + random.nextInt(15); // Parents were 20-35 when child was born
        long birthTime = child.getBirthTime() - (parentAgeAtBirth * 365L * 24 * 60 * 60 * 1000);
        
        // Inherit some personality traits
        String personality = generateInheritedPersonality(child.getPersonality(), random);
        
        int age = (int)((System.currentTimeMillis() - birthTime) / (1000L * 60 * 60 * 24 * 365));
        int happiness = 40 + random.nextInt(60);
        
        String profession = getHistoricalProfession(generationLevel, random);
        String birthPlace = generateAncestralBirthPlace(child.getBirthPlace(), generationLevel, random);
        
        VillagerData ancestor = new VillagerData(
            name, age, gender, personality, happiness,
            random.nextInt(100), "", List.of(profession),
            new HashMap<>(), new ArrayList<>(), "", "",
            new ArrayList<>(), new ArrayList<>(),
            "", getHistoricalHobby(random), birthTime, birthPlace,
            generateHistoricalNotes(generationLevel, random),
            birthTime + (60L + random.nextInt(25)) * 365 * 24 * 60 * 60 * 1000,
            false
        );
        
        return ancestor;
    }
    
    private static String generateFeminineName(String baseName, World world, BlockPos pos) {
        // Extract surname and generate feminine first name
        String[] parts = baseName.split(" ");
        if (parts.length > 1) {
            return VillagerNames.generateNameForProfession(world, pos).split(" ")[0] + " " + parts[parts.length - 1];
        }
        return baseName + "a"; // Simple feminization
    }
    
    private static String generateCulturalName(String childName, String gender, Random random) {
        String[] parts = childName.split(" ");
        String surname = parts.length > 1 ? parts[parts.length - 1] : "Ancestor";
        
        String[] maleNames = {"Aldric", "Bran", "Cedric", "Dorian", "Edmund", "Finn", "Gareth", "Harold"};
        String[] femaleNames = {"Aria", "Beatrice", "Clara", "Diana", "Elena", "Fiona", "Grace", "Helena"};
        
        String firstName = gender.equals("female") ? 
            femaleNames[random.nextInt(femaleNames.length)] :
            maleNames[random.nextInt(maleNames.length)];
            
        return firstName + " " + surname;
    }
    
    private static String generateInheritedPersonality(String childPersonality, Random random) {
        // 60% chance to inherit, 40% chance to be different
        if (random.nextFloat() < 0.6f) {
            return childPersonality;
        }
        
        String[] personalities = VillagerData.PERSONALITIES;
        return personalities[random.nextInt(personalities.length)];
    }
    
    private static String getHistoricalProfession(int generationsBack, Random random) {
        if (generationsBack >= 3) {
            return ANCIENT_PROFESSIONS[random.nextInt(ANCIENT_PROFESSIONS.length)];
        }
        
        String[] recentProfessions = {"Farmer", "Blacksmith", "Merchant", "Scholar", "Guard", "Healer"};
        return recentProfessions[random.nextInt(recentProfessions.length)];
    }
    
    private static String generateHistoricalBirthPlace(int generationsBack, World world, BlockPos pos, Random random) {
        String period = getHistoricalPeriod(generationsBack);
        String[] locations = {"Old Village", "Ancient Settlement", "Trading Post", "Sacred Grove", "Stone Circle", "River Crossing"};
        return locations[random.nextInt(locations.length)] + " (" + period + ")";
    }
    
    private static String generateAncestralBirthPlace(String childBirthPlace, int generationLevel, Random random) {
        if (childBirthPlace.isEmpty()) {
            return "Unknown Ancient Land";
        }
        
        String[] prefixes = {"Old", "Ancient", "Elder", "First", "Sacred", "Lost"};
        return prefixes[random.nextInt(prefixes.length)] + " " + childBirthPlace.split(" ")[0];
    }
    
    private static String getHistoricalPeriod(int generationsBack) {
        return switch (generationsBack) {
            case 1 -> "Recent Past";
            case 2 -> "Living Memory"; 
            case 3 -> "Classical Era";
            case 4 -> "Old Kingdom";
            default -> "Ancient Times";
        };
    }
    
    private static String getHistoricalHobby(Random random) {
        String[] hobbies = {"Stone Carving", "Storytelling", "Star Gazing", "Herb Gathering", 
                           "Wood Crafting", "Ceremonial Dancing", "Ancient Songs", "Rune Reading"};
        return hobbies[random.nextInt(hobbies.length)];
    }
    
    private static String generateHistoricalNotes(int generationsBack, Random random) {
        String event = HISTORICAL_EVENTS[random.nextInt(HISTORICAL_EVENTS.length)];
        int year = generationsBack * 25 + random.nextInt(50);
        return "Lived during the " + event + " " + year + " years ago";
    }
    
    /**
     * Format a timestamp into a readable date for the family tree
     */
    public static String formatHistoricalDate(long timestamp) {
        if (timestamp == 0) return "Unknown";
        
        // Use a fixed time reference for consistency
        long currentTime = System.currentTimeMillis(); // In a real implementation, this would use world.getTime()
        long yearsAgo = (currentTime - timestamp) / (1000L * 60 * 60 * 24 * 365);
        
        if (yearsAgo < 1) return "This year";
        if (yearsAgo < 5) return yearsAgo + " years ago";
        if (yearsAgo < 20) return yearsAgo + " years ago";
        if (yearsAgo < 50) return yearsAgo + " years ago (Recent Past)";
        if (yearsAgo < 100) return yearsAgo + " years ago (Classical Era)";
        if (yearsAgo < 200) return yearsAgo + " years ago (Old Kingdom)";
        return yearsAgo + " years ago (Ancient Times)";
    }
    
    /**
     * Get a user-friendly age description
     */
    public static String getAgeDescription(long birthTime, long deathTime, boolean isAlive) {
        if (birthTime == 0) return "Unknown age";
        
        // Use a fixed time reference for consistency
        long currentTime = System.currentTimeMillis(); // In a real implementation, this would use world.getTime()
        long endTime = isAlive ? currentTime : deathTime;
        long ageInYears = (endTime - birthTime) / (1000L * 60 * 60 * 24 * 365);
        
        if (isAlive) {
            return ageInYears + " years old";
        } else {
            return "Lived " + ageInYears + " years";
        }
    }
}