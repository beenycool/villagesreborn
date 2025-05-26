package com.beeny.villagesreborn.platform.fabric.spawn.persistence;

import com.beeny.villagesreborn.platform.fabric.spawn.SpawnBiomeChoiceData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles NBT serialization and deserialization for spawn biome choice data
 */
public class SpawnBiomeNBTHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnBiomeNBTHandler.class);
    
    private static final String BIOME_NAMESPACE_KEY = "biome_namespace";
    private static final String BIOME_PATH_KEY = "biome_path";
    private static final String SELECTION_TIME_KEY = "selection_timestamp";
    private static final String DATA_VERSION_KEY = "data_version";
    private static final String CURRENT_VERSION = "2.0";
    
    /**
     * Serializes spawn biome choice data to NBT
     * @param choice The spawn biome choice data to serialize
     * @return NBT compound containing the serialized data
     */
    public static NbtCompound serializeToNbt(SpawnBiomeChoiceData choice) {
        if (choice == null) {
            throw new IllegalArgumentException("SpawnBiomeChoiceData cannot be null");
        }
        
        NbtCompound nbt = new NbtCompound();
        Identifier biomeId = choice.getBiomeKey().getValue();
        
        nbt.putString(BIOME_NAMESPACE_KEY, biomeId.getNamespace());
        nbt.putString(BIOME_PATH_KEY, biomeId.getPath());
        nbt.putLong(SELECTION_TIME_KEY, choice.getSelectionTimestamp());
        nbt.putString(DATA_VERSION_KEY, CURRENT_VERSION);
        
        LOGGER.debug("Serialized spawn biome choice to NBT: {}", choice);
        return nbt;
    }
    
    /**
     * Deserializes spawn biome choice data from NBT
     * @param nbt The NBT compound to deserialize from
     * @return The deserialized spawn biome choice data
     * @throws IllegalArgumentException if NBT data is invalid
     */
    public static SpawnBiomeChoiceData deserializeFromNbt(NbtCompound nbt) {
        if (nbt == null || !nbt.contains(BIOME_NAMESPACE_KEY) || !nbt.contains(BIOME_PATH_KEY)) {
            throw new IllegalArgumentException("Missing required biome data in NBT");
        }
        
        String namespace = nbt.getString(BIOME_NAMESPACE_KEY);
        String path = nbt.getString(BIOME_PATH_KEY);
        
        if (namespace.isEmpty() || path.isEmpty()) {
            throw new IllegalArgumentException("Empty biome identifier components");
        }
        
        Identifier biomeId = Identifier.of(namespace, path);
        RegistryKey<Biome> biomeKey = RegistryKey.of(RegistryKeys.BIOME, biomeId);
        
        long timestamp = nbt.getLong(SELECTION_TIME_KEY);
        if (timestamp <= 0) {
            timestamp = System.currentTimeMillis(); // Backward compatibility
            LOGGER.debug("Using current timestamp for backward compatibility");
        }
        
        SpawnBiomeChoiceData choice = new SpawnBiomeChoiceData(biomeKey, timestamp);
        LOGGER.debug("Deserialized spawn biome choice from NBT: {}", choice);
        return choice;
    }
    
    /**
     * Serializes spawn biome choice data to a Map for world data storage
     * @param choice The spawn biome choice data to serialize
     * @return Map containing the serialized data
     */
    public static Map<String, Object> serializeToMap(SpawnBiomeChoiceData choice) {
        if (choice == null) {
            throw new IllegalArgumentException("SpawnBiomeChoiceData cannot be null");
        }
        
        Map<String, Object> map = new HashMap<>();
        Identifier biomeId = choice.getBiomeKey().getValue();
        
        map.put("biome_namespace", biomeId.getNamespace());
        map.put("biome_path", biomeId.getPath());
        map.put("selection_timestamp", choice.getSelectionTimestamp());
        map.put("data_version", CURRENT_VERSION);
        
        LOGGER.debug("Serialized spawn biome choice to Map: {}", choice);
        return map;
    }
    
    /**
     * Deserializes spawn biome choice data from a Map
     * @param map The map to deserialize from
     * @return The deserialized spawn biome choice data
     * @throws IllegalArgumentException if map data is invalid
     */
    public static SpawnBiomeChoiceData deserializeFromMap(Map<String, Object> map) {
        if (map == null || !map.containsKey("biome_namespace") || !map.containsKey("biome_path")) {
            throw new IllegalArgumentException("Missing required biome data in map");
        }
        
        String namespace = (String) map.get("biome_namespace");
        String path = (String) map.get("biome_path");
        
        if (namespace == null || path == null || namespace.isEmpty() || path.isEmpty()) {
            throw new IllegalArgumentException("Invalid biome identifier components");
        }
        
        Object timestampObj = map.get("selection_timestamp");
        long timestamp = System.currentTimeMillis(); // Default
        
        if (timestampObj instanceof Number) {
            timestamp = ((Number) timestampObj).longValue();
            if (timestamp <= 0) {
                timestamp = System.currentTimeMillis();
                LOGGER.debug("Invalid timestamp, using current time");
            }
        }
        
        Identifier biomeId = Identifier.of(namespace, path);
        RegistryKey<Biome> biomeKey = RegistryKey.of(RegistryKeys.BIOME, biomeId);
        
        SpawnBiomeChoiceData choice = new SpawnBiomeChoiceData(biomeKey, timestamp);
        LOGGER.debug("Deserialized spawn biome choice from Map: {}", choice);
        return choice;
    }
    
    /**
     * Validates that NBT contains valid spawn biome data
     * @param nbt The NBT compound to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidNbt(NbtCompound nbt) {
        if (nbt == null) return false;
        
        if (!nbt.contains(BIOME_NAMESPACE_KEY) || !nbt.contains(BIOME_PATH_KEY)) {
            return false;
        }
        
        String namespace = nbt.getString(BIOME_NAMESPACE_KEY);
        String path = nbt.getString(BIOME_PATH_KEY);
        
        return !namespace.isEmpty() && !path.isEmpty();
    }
    
    /**
     * Validates that Map contains valid spawn biome data
     * @param map The map to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidMap(Map<String, Object> map) {
        if (map == null) return false;
        
        if (!map.containsKey("biome_namespace") || !map.containsKey("biome_path")) {
            return false;
        }
        
        Object namespaceObj = map.get("biome_namespace");
        Object pathObj = map.get("biome_path");
        
        if (!(namespaceObj instanceof String) || !(pathObj instanceof String)) {
            return false;
        }
        
        String namespace = (String) namespaceObj;
        String path = (String) pathObj;
        
        return !namespace.isEmpty() && !path.isEmpty();
    }
}