package com.beeny.util;

import net.minecraft.village.VillagerProfession;
import net.minecraft.util.Identifier;
import net.minecraft.registry.RegistryKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class VillagerNames {
    private static final Random RANDOM = new Random();
    
    private static final Map<Identifier, String> PROFESSION_TITLES = new HashMap<>();
    
    static {
        PROFESSION_TITLES.put(VillagerProfession.ARMORER.getValue(), "Blacksmith");
        PROFESSION_TITLES.put(VillagerProfession.BUTCHER.getValue(), "Butcher");
        PROFESSION_TITLES.put(VillagerProfession.CARTOGRAPHER.getValue(), "Cartographer");
        PROFESSION_TITLES.put(VillagerProfession.CLERIC.getValue(), "Cleric");
        PROFESSION_TITLES.put(VillagerProfession.FARMER.getValue(), "Farmer");
        PROFESSION_TITLES.put(VillagerProfession.FISHERMAN.getValue(), "Fisherman");
        PROFESSION_TITLES.put(VillagerProfession.FLETCHER.getValue(), "Fletcher");
        PROFESSION_TITLES.put(VillagerProfession.LEATHERWORKER.getValue(), "Leatherworker");
        PROFESSION_TITLES.put(VillagerProfession.LIBRARIAN.getValue(), "Librarian");
        PROFESSION_TITLES.put(VillagerProfession.MASON.getValue(), "Mason");
        PROFESSION_TITLES.put(VillagerProfession.SHEPHERD.getValue(), "Shepherd");
        PROFESSION_TITLES.put(VillagerProfession.TOOLSMITH.getValue(), "Toolsmith");
        PROFESSION_TITLES.put(VillagerProfession.WEAPONSMITH.getValue(), "Weaponsmith");
    }
    
    private static final String[] FIRST_NAMES = {
        "Alex", "Emma", "Oliver", "Sophia", "William", "Ava", "James", "Isabella",
        "Benjamin", "Charlotte", "Lucas", "Amelia", "Henry", "Mia", "Alexander", "Harper",
        "Michael", "Evelyn", "Ethan", "Abigail", "Daniel", "Emily", "Matthew", "Elizabeth",
        "Aiden", "Sofia", "Jackson", "Avery", "Logan", "Ella", "David", "Scarlett",
        "Joseph", "Grace", "Samuel", "Chloe", "Andrew", "Victoria", "Joshua", "Riley",
        "John", "Aria", "Christopher", "Zoey", "Gabriel", "Nora", "Noah", "Lily",
        "Caleb", "Eleanor", "Ryan", "Hannah", "Nathan", "Lillian", "Isaac", "Addison",
        "Griffin", "Aubrey", "Owen", "Ellie", "Christian", "Stella", "Hunter", "Natalie",
        "Connor", "Zoe", "Eli", "Leah", "Landon", "Hazel", "Adrian", "Violet",
        "Jonathan", "Aurora", "Kevin", "Savannah", "Zachary", "Audrey", "Evan", "Brooklyn",
        "Robert", "Bella", "Gavin", "Claire", "Jaxon", "Skylar", "Aaron", "Lucy",
        "Isaiah", "Paisley", "Thomas", "Everly", "Charles", "Anna", "Ian", "Caroline",
        "Mason", "Nova", "Sebastian", "Genesis", "Jack", "Emilia", "Luke", "Kennedy",
        "Eugene", "Madison", "Miles", "Samantha", "Dominic", "Aaliyah", "Madeline", "Jason", "Piper",
    };

    public static String generateNameForProfession(RegistryKey<VillagerProfession> profession) {
        String professionTitle = PROFESSION_TITLES.get(profession.getValue());
        String firstName = FIRST_NAMES[RANDOM.nextInt(FIRST_NAMES.length)];
        
        if (professionTitle == null) {
            return firstName;
        }
        
        return professionTitle + " " + firstName;
    }
    
    public static String updateProfessionInName(String currentName, RegistryKey<VillagerProfession> newProfession) {
        String newProfessionTitle = PROFESSION_TITLES.get(newProfession.getValue());
        
        // Extract the first name from the current name (everything after the first space, or the whole name if no space)
        String firstName = extractFirstName(currentName);
        
        if (newProfessionTitle == null) {
            // No profession title (unemployed villagers, nitwits) - just use first name
            return firstName;
        }
        
        return newProfessionTitle + " " + firstName;
    }
    
    private static String extractFirstName(String fullName) {
        if (fullName == null) {
            // Fallback: generate a new first name if something went wrong
            return FIRST_NAMES[RANDOM.nextInt(FIRST_NAMES.length)];
        }
        
        if (!fullName.contains(" ")) {
            // Name is already just a first name (like "Bob")
            return fullName;
        }
        
        // Return everything after the first space
        return fullName.substring(fullName.indexOf(" ") + 1);
    }

    public static String generateRandomName() {
        return generateNameForProfession(VillagerProfession.NITWIT);
    }

    public static String generateRandomFirstName() {
        return FIRST_NAMES[RANDOM.nextInt(FIRST_NAMES.length)];
    }
}