package com.beeny.util;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;

import java.util.*;

public class VillagerNames {
    private static final Map<String, List<String>> REGIONAL_MALE_NAMES = new HashMap<>();
    private static final Map<String, List<String>> REGIONAL_FEMALE_NAMES = new HashMap<>();
    private static final Map<String, List<String>> REGIONAL_SURNAMES = new HashMap<>();
    private static final Map<String, List<String>> PROFESSION_NAMES = new HashMap<>();
    
    private static final Random RANDOM = new Random();
    
    static {
        // Desert names
        REGIONAL_MALE_NAMES.put("desert", Arrays.asList("Amir", "Omar", "Khalid", "Rashid", "Tariq", "Zaid", "Farid", "Hakim", "Jamal", "Karim"));
        REGIONAL_FEMALE_NAMES.put("desert", Arrays.asList("Amina", "Fatima", "Leila", "Noor", "Sara", "Yasmin", "Zahra", "Amira", "Hana", "Layla"));
        REGIONAL_SURNAMES.put("desert", Arrays.asList("Al-Rashid", "Ibn-Saud", "Al-Farsi", "Al-Masri", "Al-Tikriti", "Al-Baghdadi", "Al-Dimashqi", "Al-Kufi"));
        
        // Snow names
        REGIONAL_MALE_NAMES.put("snow", Arrays.asList("Bjorn", "Erik", "Gunnar", "Harald", "Leif", "Olaf", "Ragnar", "Sven", "Thor", "Ulf"));
        REGIONAL_FEMALE_NAMES.put("snow", Arrays.asList("Astrid", "Freya", "Greta", "Helga", "Ingrid", "Liv", "Sigrid", "Thyra", "Yrsa", "Asta"));
        REGIONAL_SURNAMES.put("snow", Arrays.asList("Bjornsson", "Eriksson", "Gunnarsson", "Haraldsson", "Leifsson", "Olafsson", "Ragnarsson", "Svensson"));
        
        // Taiga names
        REGIONAL_MALE_NAMES.put("taiga", Arrays.asList("Ivan", "Dmitri", "Sergei", "Vladimir", "Alexei", "Nikolai", "Pavel", "Yuri", "Mikhail", "Andrei"));
        REGIONAL_FEMALE_NAMES.put("taiga", Arrays.asList("Anya", "Katya", "Nadia", "Olga", "Svetlana", "Tatiana", "Vera", "Yelena", "Irina", "Ludmila"));
        REGIONAL_SURNAMES.put("taiga", Arrays.asList("Ivanov", "Petrov", "Sidorov", "Smirnov", "Kuznetsov", "Popov", "Volkov", "Romanov"));
        
        // Plains names
        REGIONAL_MALE_NAMES.put("plains", Arrays.asList("William", "Henry", "James", "John", "Robert", "Michael", "David", "Richard", "Charles", "Thomas"));
        REGIONAL_FEMALE_NAMES.put("plains", Arrays.asList("Mary", "Elizabeth", "Sarah", "Margaret", "Catherine", "Anne", "Jane", "Alice", "Emily", "Charlotte"));
        REGIONAL_SURNAMES.put("plains", Arrays.asList("Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Wilson", "Moore", "Taylor"));
        
        // Savanna names
        REGIONAL_MALE_NAMES.put("savanna", Arrays.asList("Kwame", "Kofi", "Amara", "Chike", "Jabari", "Baraka", "Hasani", "Jelani", "Omari", "Tafari"));
        REGIONAL_FEMALE_NAMES.put("savanna", Arrays.asList("Aisha", "Fatima", "Imani", "Khadija", "Mariam", "Nia", "Saida", "Zahra", "Amina", "Halima"));
        REGIONAL_SURNAMES.put("savanna", Arrays.asList("Diallo", "Keita", "Touré", "Sow", "Ba", "Diop", "Fall", "Ndiaye", "Sarr", "Cissé"));
        
        // Jungle names
        REGIONAL_MALE_NAMES.put("jungle", Arrays.asList("Miguel", "Carlos", "José", "Antonio", "Luis", "Manuel", "Pedro", "Rafael", "Francisco", "Juan"));
        REGIONAL_FEMALE_NAMES.put("jungle", Arrays.asList("Maria", "Ana", "Isabel", "Carmen", "Rosa", "Julia", "Teresa", "Dolores", "Sofia", "Lucia"));
        REGIONAL_SURNAMES.put("jungle", Arrays.asList("Silva", "Santos", "Oliveira", "Souza", "Rodrigues", "Ferreira", "Almeida", "Costa", "Pereira", "Carvalho"));
        
        // Default names for other biomes
        REGIONAL_MALE_NAMES.put("default", Arrays.asList("Alex", "Sam", "Jordan", "Casey", "Morgan", "Taylor", "Jamie", "Riley", "Avery", "Quinn"));
        REGIONAL_FEMALE_NAMES.put("default", Arrays.asList("Alex", "Sam", "Jordan", "Casey", "Morgan", "Taylor", "Jamie", "Riley", "Avery", "Quinn"));
        REGIONAL_SURNAMES.put("default", Arrays.asList("Stone", "River", "Forest", "Hill", "Brook", "Lake", "Field", "Wood", "Meadow", "Glen"));
        
        // Profession names
        PROFESSION_NAMES.put("minecraft:none", Arrays.asList("Villager", "Settler", "Commoner", "Peasant", "Townsperson"));
        PROFESSION_NAMES.put("minecraft:armorer", Arrays.asList("Armorer", "Smith", "Blacksmith", "Metalsmith", "Armorsmith"));
        PROFESSION_NAMES.put("minecraft:butcher", Arrays.asList("Butcher", "Meatcutter", "Slaughterer", "Meatmonger", "Purveyor"));
        PROFESSION_NAMES.put("minecraft:cartographer", Arrays.asList("Cartographer", "Mapmaker", "Surveyor", "Navigator", "Chartmaker"));
        PROFESSION_NAMES.put("minecraft:cleric", Arrays.asList("Cleric", "Priest", "Monk", "Healer", "Diviner"));
        PROFESSION_NAMES.put("minecraft:farmer", Arrays.asList("Farmer", "Agriculturist", "Cultivator", "Grower", "Planter"));
        PROFESSION_NAMES.put("minecraft:fisherman", Arrays.asList("Fisherman", "Angler", "Fisher", "Trawler", "Netcaster"));
        PROFESSION_NAMES.put("minecraft:fletcher", Arrays.asList("Fletcher", "Bowyer", "Arrowmaker", "Bowsmith", "Stringer"));
        PROFESSION_NAMES.put("minecraft:leatherworker", Arrays.asList("Leatherworker", "Tanner", "Skinner", "Hidecrafter", "Leathersmith"));
        PROFESSION_NAMES.put("minecraft:librarian", Arrays.asList("Librarian", "Scholar", "Scribe", "Bookkeeper", "Archivist"));
        PROFESSION_NAMES.put("minecraft:mason", Arrays.asList("Mason", "Stonemason", "Stonecutter", "Rockworker", "Bricklayer"));
        PROFESSION_NAMES.put("minecraft:nitwit", Arrays.asList("Nitwit", "Fool", "Simpleton", "Dullard", "Idler"));
        PROFESSION_NAMES.put("minecraft:shepherd", Arrays.asList("Shepherd", "Herdsman", "Pastor", "Flockmaster", "Woolworker"));
        PROFESSION_NAMES.put("minecraft:toolsmith", Arrays.asList("Toolsmith", "Toolmaker", "Craftsman", "Artisan", "Smith"));
        PROFESSION_NAMES.put("minecraft:weaponsmith", Arrays.asList("Weaponsmith", "Bladesmith", "Swordsmith", "Warmaker", "Battleforger"));
    }
    
    public static String generateName(VillagerEntity villager) {
        if (villager == null) {
            throw new IllegalArgumentException("Villager cannot be null");
        }
        
        World world = villager.getWorld();
        BlockPos pos = villager.getBlockPos();
        Biome biome = world.getBiome(pos).value();
        
        String biomeKey = getBiomeKey(biome);
        boolean isMale = RANDOM.nextBoolean();
        
        List<String> firstNames = isMale ? 
            REGIONAL_MALE_NAMES.getOrDefault(biomeKey, REGIONAL_MALE_NAMES.get("default")) :
            REGIONAL_FEMALE_NAMES.getOrDefault(biomeKey, REGIONAL_FEMALE_NAMES.get("default"));
            
        List<String> surnames = REGIONAL_SURNAMES.getOrDefault(biomeKey, REGIONAL_SURNAMES.get("default"));
        
        // Defensive programming – should not happen with current static data
        if (firstNames.isEmpty() || surnames.isEmpty()) {
            return "Unnamed Villager";
        }
        
        String firstName = firstNames.get(RANDOM.nextInt(firstNames.size()));
        String surname = surnames.get(RANDOM.nextInt(surnames.size()));
        
        return firstName + " " + surname;
    }
    
    private static String getBiomeKey(Biome biome) {
        // This is a simplified mapping - in a real mod you'd use the actual biome registry
        if (biome.getKey().isPresent()) {
            String key = biome.getKey().get().getValue().getPath();
            if (key.contains("desert")) return "desert";
            if (key.contains("snow") || key.contains("ice")) return "snow";
            if (key.contains("taiga")) return "taiga";
            if (key.contains("plains")) return "plains";
            if (key.contains("savanna")) return "savanna";
            if (key.contains("jungle")) return "jungle";
        }
        return "default";
    }
    
    public static String getProfessionName(String professionKey) {
        List<String> names = PROFESSION_NAMES.getOrDefault(professionKey, Arrays.asList("Villager"));
        return names.get(RANDOM.nextInt(names.size()));
    }
    
    public static String extractSurname(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }
        
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length >= 2) {
            return parts[parts.length - 1];
        }
        return "";
    }
    
    public static String getFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "";
        }
        
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length >= 1) {
            return parts[0];
        }
        return "";
    }
    
    public static String getVillagerName(VillagerEntity villager) {
        if (villager.hasCustomName()) {
            return villager.getCustomName().getString();
        }
        
        // Generate a name if the villager doesn't have one
        return generateName(villager);
    }
}