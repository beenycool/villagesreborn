package com.beeny.util;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VillagerNames {
    private static final Map<String, List<String>> REGIONAL_MALE_NAMES = new HashMap<>();
    private static final Map<String, List<String>> REGIONAL_FEMALE_NAMES = new HashMap<>();
    private static final Map<String, List<String>> REGIONAL_SURNAMES = new HashMap<>();
    private static final Map<String, List<String>> PROFESSION_NAMES = new HashMap<>();
    
    
    private static final Map<String, Boolean> VILLAGER_GENDERS = new ConcurrentHashMap<>();
    
    private static final Random RANDOM = new Random();
    
    static {
        
        REGIONAL_MALE_NAMES.put("desert", Arrays.asList("Amir", "Omar", "Khalid", "Rashid", "Tariq", "Zaid", "Farid", "Hakim", "Jamal", "Karim"));
        REGIONAL_FEMALE_NAMES.put("desert", Arrays.asList("Amina", "Fatima", "Leila", "Noor", "Sara", "Yasmin", "Zahra", "Amira", "Hana", "Layla"));
        REGIONAL_SURNAMES.put("desert", Arrays.asList("Al-Rashid", "Ibn-Saud", "Al-Farsi", "Al-Masri", "Al-Tikriti", "Al-Baghdadi", "Al-Dimashqi", "Al-Kufi"));
        
        
        REGIONAL_MALE_NAMES.put("snow", Arrays.asList("Bjorn", "Erik", "Gunnar", "Harald", "Leif", "Olaf", "Ragnar", "Sven", "Thor", "Ulf"));
        REGIONAL_FEMALE_NAMES.put("snow", Arrays.asList("Astrid", "Freya", "Greta", "Helga", "Ingrid", "Liv", "Sigrid", "Thyra", "Yrsa", "Asta"));
        REGIONAL_SURNAMES.put("snow", Arrays.asList("Bjornsson", "Eriksson", "Gunnarsson", "Haraldsson", "Leifsson", "Olafsson", "Ragnarsson", "Svensson"));
        
        
        REGIONAL_MALE_NAMES.put("taiga", Arrays.asList("Ivan", "Dmitri", "Sergei", "Vladimir", "Alexei", "Nikolai", "Pavel", "Yuri", "Mikhail", "Andrei"));
        REGIONAL_FEMALE_NAMES.put("taiga", Arrays.asList("Anya", "Katya", "Nadia", "Olga", "Svetlana", "Tatiana", "Vera", "Yelena", "Irina", "Ludmila"));
        REGIONAL_SURNAMES.put("taiga", Arrays.asList("Ivanov", "Petrov", "Sidorov", "Smirnov", "Kuznetsov", "Popov", "Volkov", "Romanov"));
        
        
        REGIONAL_MALE_NAMES.put("plains", Arrays.asList("William", "Henry", "James", "John", "Robert", "Michael", "David", "Richard", "Charles", "Thomas"));
        REGIONAL_FEMALE_NAMES.put("plains", Arrays.asList("Mary", "Elizabeth", "Sarah", "Margaret", "Catherine", "Anne", "Jane", "Alice", "Emily", "Charlotte"));
        REGIONAL_SURNAMES.put("plains", Arrays.asList("Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Wilson", "Moore", "Taylor"));
        
        
        REGIONAL_MALE_NAMES.put("savanna", Arrays.asList("Kwame", "Kofi", "Amara", "Chike", "Jabari", "Baraka", "Hasani", "Jelani", "Omari", "Tafari"));
        REGIONAL_FEMALE_NAMES.put("savanna", Arrays.asList("Aisha", "Fatima", "Imani", "Khadija", "Mariam", "Nia", "Saida", "Zahra", "Amina", "Halima"));
        REGIONAL_SURNAMES.put("savanna", Arrays.asList("Diallo", "Keita", "Touré", "Sow", "Ba", "Diop", "Fall", "Ndiaye", "Sarr", "Cissé"));
        
        
        REGIONAL_MALE_NAMES.put("jungle", Arrays.asList("Miguel", "Carlos", "José", "Antonio", "Luis", "Manuel", "Pedro", "Rafael", "Francisco", "Juan"));
        REGIONAL_FEMALE_NAMES.put("jungle", Arrays.asList("Maria", "Ana", "Isabel", "Carmen", "Rosa", "Julia", "Teresa", "Dolores", "Sofia", "Lucia"));
        REGIONAL_SURNAMES.put("jungle", Arrays.asList("Silva", "Santos", "Oliveira", "Souza", "Rodrigues", "Ferreira", "Almeida", "Costa", "Pereira", "Carvalho"));
        
        
        REGIONAL_MALE_NAMES.put("swamp", Arrays.asList("Tobias", "Jeremiah", "Ezekiel", "Mordecai", "Silas", "Ambrose", "Cornelius", "Barnabas", "Thaddeus", "Ichabod"));
        REGIONAL_FEMALE_NAMES.put("swamp", Arrays.asList("Prudence", "Temperance", "Constance", "Mercy", "Charity", "Verity", "Felicity", "Serenity", "Patience", "Grace"));
        REGIONAL_SURNAMES.put("swamp", Arrays.asList("Blackwater", "Grimm", "Marsh", "Boggart", "Murkwood", "Fenwick", "Shadowmere", "Gloom"));
        
        
        REGIONAL_MALE_NAMES.put("ocean", Arrays.asList("Dylan", "Morgan", "Kai", "Finn", "Murphy", "Ronan", "Caspian", "Drake", "Neptune", "Triton"));
        REGIONAL_FEMALE_NAMES.put("ocean", Arrays.asList("Marina", "Coral", "Pearl", "Nerissa", "Delphine", "Mira", "Cordelia", "Isla", "Moana", "Ariel"));
        REGIONAL_SURNAMES.put("ocean", Arrays.asList("Seaworth", "Tidewater", "Saltwind", "Wavecrest", "Deepwater", "Stormshore", "Seafoam", "Shellhaven"));
        
        
        REGIONAL_MALE_NAMES.put("default", Arrays.asList("Alex", "Sam", "Jordan", "Casey", "Morgan", "Taylor", "Jamie", "Riley", "Avery", "Quinn"));
        REGIONAL_FEMALE_NAMES.put("default", Arrays.asList("Alex", "Sam", "Jordan", "Casey", "Morgan", "Taylor", "Jamie", "Riley", "Avery", "Quinn"));
        REGIONAL_SURNAMES.put("default", Arrays.asList("Stone", "River", "Forest", "Hill", "Brook", "Lake", "Field", "Wood", "Meadow", "Glen"));
        
        
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
        RegistryEntry<Biome> biomeEntry = world.getBiome(pos);
        
        String biomeKey = getBiomeKey(biomeEntry);
        boolean isMale = getOrAssignGender(villager);
        
        List<String> firstNames = isMale ? 
            REGIONAL_MALE_NAMES.getOrDefault(biomeKey, REGIONAL_MALE_NAMES.get("default")) :
            REGIONAL_FEMALE_NAMES.getOrDefault(biomeKey, REGIONAL_FEMALE_NAMES.get("default"));
            
        List<String> surnames = REGIONAL_SURNAMES.getOrDefault(biomeKey, REGIONAL_SURNAMES.get("default"));
        
        if (firstNames.isEmpty() || surnames.isEmpty()) {
            return "Unnamed Villager";
        }
        
        String firstName = firstNames.get(RANDOM.nextInt(firstNames.size()));
        String surname = surnames.get(RANDOM.nextInt(surnames.size()));
        
        return firstName + " " + surname;
    }
    
    private static boolean getOrAssignGender(VillagerEntity villager) {
        return VILLAGER_GENDERS.computeIfAbsent(villager.getUuidAsString(), k -> RANDOM.nextBoolean());
    }
    
    private static String getBiomeKey(RegistryEntry<Biome> biomeEntry) {
        
        Optional<RegistryKey<Biome>> keyOpt = biomeEntry.getKey();
        if (keyOpt.isEmpty()) {
            return "default";
        }
        
        RegistryKey<Biome> biomeKey = keyOpt.get();
        
        
        if (biomeKey.equals(BiomeKeys.DESERT) || biomeKey.equals(BiomeKeys.BADLANDS) || 
            biomeKey.equals(BiomeKeys.ERODED_BADLANDS) || biomeKey.equals(BiomeKeys.WOODED_BADLANDS)) {
            return "desert";
        }
        
        
        if (biomeKey.equals(BiomeKeys.SNOWY_PLAINS) || biomeKey.equals(BiomeKeys.SNOWY_TAIGA) || 
            biomeKey.equals(BiomeKeys.FROZEN_RIVER) || biomeKey.equals(BiomeKeys.SNOWY_BEACH) ||
            biomeKey.equals(BiomeKeys.GROVE) || biomeKey.equals(BiomeKeys.SNOWY_SLOPES) ||
            biomeKey.equals(BiomeKeys.FROZEN_PEAKS) || biomeKey.equals(BiomeKeys.JAGGED_PEAKS)) {
            return "snow";
        }
        
        
        if (biomeKey.equals(BiomeKeys.TAIGA) || biomeKey.equals(BiomeKeys.OLD_GROWTH_PINE_TAIGA) || 
            biomeKey.equals(BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA)) {
            return "taiga";
        }
        
        
        if (biomeKey.equals(BiomeKeys.PLAINS) || biomeKey.equals(BiomeKeys.SUNFLOWER_PLAINS) ||
            biomeKey.equals(BiomeKeys.MEADOW)) {
            return "plains";
        }
        
        
        if (biomeKey.equals(BiomeKeys.SAVANNA) || biomeKey.equals(BiomeKeys.SAVANNA_PLATEAU) ||
            biomeKey.equals(BiomeKeys.WINDSWEPT_SAVANNA)) {
            return "savanna";
        }
        
        
        if (biomeKey.equals(BiomeKeys.JUNGLE) || biomeKey.equals(BiomeKeys.SPARSE_JUNGLE) ||
            biomeKey.equals(BiomeKeys.BAMBOO_JUNGLE)) {
            return "jungle";
        }
        
        
        if (biomeKey.equals(BiomeKeys.SWAMP) || biomeKey.equals(BiomeKeys.MANGROVE_SWAMP)) {
            return "swamp";
        }
        
        
        if (biomeKey.equals(BiomeKeys.BEACH) || biomeKey.equals(BiomeKeys.OCEAN) || 
            biomeKey.equals(BiomeKeys.WARM_OCEAN) || biomeKey.equals(BiomeKeys.LUKEWARM_OCEAN) ||
            biomeKey.equals(BiomeKeys.COLD_OCEAN) || biomeKey.equals(BiomeKeys.FROZEN_OCEAN) ||
            biomeKey.equals(BiomeKeys.DEEP_OCEAN) || biomeKey.equals(BiomeKeys.DEEP_FROZEN_OCEAN)) {
            return "ocean";
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
        
        
        return generateName(villager);
    }
    
    public static String generateNameForProfession(World world, BlockPos pos) {
        RegistryEntry<Biome> biomeEntry = world.getBiome(pos);
        String biomeKey = getBiomeKey(biomeEntry);
        
        
        boolean isMale = (pos.getX() + pos.getZ()) % 2 == 0;
        
        List<String> firstNames = isMale ?
            REGIONAL_MALE_NAMES.getOrDefault(biomeKey, REGIONAL_MALE_NAMES.get("default")) :
            REGIONAL_FEMALE_NAMES.getOrDefault(biomeKey, REGIONAL_FEMALE_NAMES.get("default"));
            
        List<String> surnames = REGIONAL_SURNAMES.getOrDefault(biomeKey, REGIONAL_SURNAMES.get("default"));
        
        if (firstNames.isEmpty() || surnames.isEmpty()) {
            return "Unnamed Villager";
        }
        
        String firstName = firstNames.get(RANDOM.nextInt(firstNames.size()));
        String surname = surnames.get(RANDOM.nextInt(surnames.size()));
        
        return firstName + " " + surname;
    }
    
    public static String generateNameForProfession(World world, BlockPos pos, String inheritedSurname) {
        if (inheritedSurname == null || inheritedSurname.trim().isEmpty()) {
            // When no inherited surname, delegate to the simpler method
            return generateNameForProfession(world, pos);
        }
        
        // When we have an inherited surname, we need to generate just the first name
        RegistryEntry<Biome> biomeEntry = world.getBiome(pos);
        String biomeKey = getBiomeKey(biomeEntry);
        
        boolean isMale = (pos.getX() + pos.getZ()) % 2 == 0;
        
        List<String> firstNames = isMale ?
            REGIONAL_MALE_NAMES.getOrDefault(biomeKey, REGIONAL_MALE_NAMES.get("default")) :
            REGIONAL_FEMALE_NAMES.getOrDefault(biomeKey, REGIONAL_FEMALE_NAMES.get("default"));
        
        if (firstNames.isEmpty()) {
            return "Unnamed Villager";
        }
        
        String firstName = firstNames.get(RANDOM.nextInt(firstNames.size()));
        return firstName + " " + inheritedSurname;
    }
    
    public static String updateProfessionInName(String currentName, RegistryKey<VillagerProfession> profession, World world, BlockPos pos) {
        if (currentName == null || currentName.trim().isEmpty()) {
            return generateNameForProfession(world, pos);
        }
        
        
        return currentName;
    }
    
    
    public static void cleanupVillager(String villagerUuid) {
        VILLAGER_GENDERS.remove(villagerUuid);
    }
}