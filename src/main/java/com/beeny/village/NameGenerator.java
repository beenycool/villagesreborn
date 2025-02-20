package com.beeny.village;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.Map.Entry;

public class NameGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final Random RANDOM = new Random();

    private static final Map<String, List<String>> CULTURE_FIRST_NAMES = Map.of(
        "roman", List.of("Marcus", "Lucius", "Gaius", "Julius", "Claudius",
                        "Cornelia", "Julia", "Livia", "Octavia", "Antonia"),
        "egyptian", List.of("Amun", "Ramses", "Thoth", "Horus", "Anubis",
                           "Nefertiti", "Isis", "Hathor", "Nephthys", "Merit"),
        "victorian", List.of("Albert", "Edward", "William", "Charles", "George",
                            "Victoria", "Elizabeth", "Mary", "Adelaide", "Charlotte"),
        "nyc", List.of("Frank", "Tony", "Joey", "Jimmy", "Mike",
                       "Maria", "Sarah", "Rachel", "Emma", "Sophie")
    );

    private static final Map<String, List<String>> PROFESSION_TITLES = Map.ofEntries(
        Map.entry("farmer", List.of("Farmer", "Planter", "Harvester", "Gardener")),
        Map.entry("fisherman", List.of("Fisher", "Angler", "Seafarer", "Mariner")),
        Map.entry("shepherd", List.of("Shepherd", "Herder", "Wool-Worker", "Flock-Keeper")),
        Map.entry("fletcher", List.of("Fletcher", "Bowyer", "Arrow-Maker", "Craftsman")),
        Map.entry("librarian", List.of("Scholar", "Sage", "Librarian", "Scribe")),
        Map.entry("cartographer", List.of("Mapper", "Explorer", "Cartographer", "Surveyor")),
        Map.entry("cleric", List.of("Priest", "Cleric", "Oracle", "Healer")),
        Map.entry("armorer", List.of("Armorer", "Smith", "Forge-Master", "Metal-Worker")),
        Map.entry("weaponsmith", List.of("Weaponsmith", "Blade-Forger", "Arms-Maker", "Sword-Smith")),
        Map.entry("toolsmith", List.of("Toolsmith", "Craftsman", "Artisan", "Tool-Maker")),
        Map.entry("butcher", List.of("Butcher", "Meat-Cutter", "Food-Merchant", "Cook")),
        Map.entry("leatherworker", List.of("Tanner", "Leather-Worker", "Hide-Crafter", "Craftsman")),
        Map.entry("mason", List.of("Mason", "Stone-Worker", "Builder", "Architect")),
        Map.entry("nitwit", List.of("Wanderer", "Dreamer", "Free-Spirit", "Villager"))
    );

    private static final Map<String, List<String>> CULTURE_NAME_SUFFIXES = Map.of(
        "roman", List.of("us", "ius", "ex", "ix", "a"),
        "egyptian", List.of("-hotep", "-amun", "-ra", "-ket", "-et"),
        "victorian", List.of("son", "worth", "shire", "ton", "field"),
        "nyc", List.of("ski", "man", "berg", "stein", "")
    );

    public String generateName(String culture, String profession) {
        culture = culture.toLowerCase();
        profession = profession.toLowerCase();

        try {
            String firstName = getRandomElement(CULTURE_FIRST_NAMES.getOrDefault(culture, 
                CULTURE_FIRST_NAMES.get("roman")));
            
            String title = getRandomElement(PROFESSION_TITLES.getOrDefault(profession,
                List.of("Villager")));

            // 30% chance to add a culture-specific suffix to the first name
            if (RANDOM.nextDouble() < 0.3) {
                String suffix = getRandomElement(CULTURE_NAME_SUFFIXES.getOrDefault(culture,
                    CULTURE_NAME_SUFFIXES.get("roman")));
                firstName = firstName + suffix;
            }

            String name = firstName + " the " + title;
            LOGGER.debug("Generated name for {} {}: {}", culture, profession, name);
            return name;

        } catch (Exception e) {
            LOGGER.error("Error generating villager name", e);
            return getRandomDefaultName(culture);
        }
    }

    private <T> T getRandomElement(List<T> list) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("List cannot be null or empty");
        }
        return list.get(RANDOM.nextInt(list.size()));
    }

    private String getRandomDefaultName(String culture) {
        String prefix = culture.substring(0, 1).toUpperCase() + culture.substring(1);
        return prefix + "Villager#" + (RANDOM.nextInt(1000) + 1);
    }

    public boolean isValidName(String name) {
        return name != null && 
               !name.isEmpty() && 
               name.length() <= 48 &&
               name.matches("^[\\p{L}\\s'-]+$");
    }
}