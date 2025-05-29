package com.beeny.villagesreborn.platform.fabric.spawn;

import com.beeny.villagesreborn.core.world.VillagesRebornWorldData;
import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Extensions to VillagesRebornWorldSettings for spawn biome persistence
 */
public class VillagesRebornWorldSettingsExtensions {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagesRebornWorldSettingsExtensions.class);
    
    // DEPRECATED: Static storage removed in favor of persistent storage
    // This field is kept temporarily for migration purposes only
    private static SpawnBiomeChoiceData currentSpawnBiomeChoice;
    
    
    
    /**
     * Writes spawn biome data to NBT
     */
    public static void writeToNbt(NbtCompound nbt) {
        if (currentSpawnBiomeChoice == null) {
            return;
        }
        
        try {
            Identifier biomeId = currentSpawnBiomeChoice.getBiomeKey().getValue();
            nbt.putString("spawn_biome_namespace", biomeId.getNamespace());
            nbt.putString("spawn_biome_path", biomeId.getPath());
            nbt.putLong("spawn_biome_selection_time", currentSpawnBiomeChoice.getSelectionTimestamp());
            
            LOGGER.debug("Wrote spawn biome choice to NBT: {}", currentSpawnBiomeChoice);
        } catch (Exception e) {
            LOGGER.error("Failed to write spawn biome choice to NBT", e);
        }
    }
    
    /**
     * Reads spawn biome data from NBT
     */
    public static void readFromNbt(NbtCompound nbt) {
        try {
            if (!nbt.contains("spawn_biome_namespace") || !nbt.contains("spawn_biome_path")) {
                currentSpawnBiomeChoice = null;
                return;
            }
            
            String namespace = nbt.getString("spawn_biome_namespace");
            String path = nbt.getString("spawn_biome_path");
            
            // Validate namespace and path
            if (namespace.isEmpty() || path.isEmpty()) {
                LOGGER.warn("Invalid spawn biome data in NBT: empty namespace or path");
                currentSpawnBiomeChoice = null;
                return;
            }
            
            // Get timestamp (with migration support)
            long timestamp = nbt.contains("spawn_biome_selection_time") 
                ? nbt.getLong("spawn_biome_selection_time")
                : System.currentTimeMillis(); // Default for migration
            
            // Validate timestamp
            if (timestamp < 0) {
                LOGGER.warn("Invalid spawn biome timestamp in NBT: {}", timestamp);
                currentSpawnBiomeChoice = null;
                return;
            }
            
            // Create biome registry key - use Identifier.of() instead of constructor
            Identifier biomeId = Identifier.of(namespace, path);
            RegistryKey<Biome> biomeKey = RegistryKey.of(RegistryKeys.BIOME, biomeId);
            
            currentSpawnBiomeChoice = new SpawnBiomeChoiceData(biomeKey, timestamp);
            
            LOGGER.debug("Read spawn biome choice from NBT: {}", currentSpawnBiomeChoice);
        } catch (Exception e) {
            LOGGER.error("Failed to read spawn biome choice from NBT", e);
            currentSpawnBiomeChoice = null;
        }
    }
    
    
    /**
     * Resets spawn biome choice (for testing and migration)
     */
    public static void resetForTest() {
        currentSpawnBiomeChoice = null;
        LOGGER.debug("Reset legacy spawn biome choice for test/migration");
    }
    
    /**
     * Checks if there is legacy data that needs migration
     * @return true if legacy data exists, false otherwise
     */
    public static boolean hasLegacyData() {
        return currentSpawnBiomeChoice != null;
    }
    
    /**
     * Converts NBT compound to Map
     */
    public static Map<String, Object> nbtToMap(NbtCompound nbt) {
        Map<String, Object> map = new HashMap<>();
        
        for (String key : nbt.getKeys()) {
            NbtElement element = nbt.get(key);
            map.put(key, nbtElementToObject(element));
        }
        
        return map;
    }
    
    /**
     * Converts Map to NBT compound
     */
    public static NbtCompound mapToNbt(Map<String, Object> map) {
        NbtCompound nbt = new NbtCompound();
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            NbtElement element = objectToNbtElement(entry.getValue());
            if (element != null) {
                nbt.put(entry.getKey(), element);
            }
        }
        
        return nbt;
    }
    
    /**
     * Converts NBT element to Java object
     */
    private static Object nbtElementToObject(NbtElement element) {
        if (element == null) return null;
        
        switch (element.getType()) {
            case NbtElement.BYTE_TYPE:
                return ((NbtByte) element).byteValue();
            case NbtElement.SHORT_TYPE:
                return ((NbtShort) element).shortValue();
            case NbtElement.INT_TYPE:
                return ((NbtInt) element).intValue();
            case NbtElement.LONG_TYPE:
                return ((NbtLong) element).longValue();
            case NbtElement.FLOAT_TYPE:
                return ((NbtFloat) element).floatValue();
            case NbtElement.DOUBLE_TYPE:
                return ((NbtDouble) element).doubleValue();
            case NbtElement.STRING_TYPE:
                return element.asString();
            case NbtElement.COMPOUND_TYPE:
                return nbtToMap((NbtCompound) element);
            case NbtElement.LIST_TYPE:
                NbtList list = (NbtList) element;
                List<Object> javaList = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    javaList.add(nbtElementToObject(list.get(i)));
                }
                return javaList;
            default:
                return element.toString();
        }
    }
    
    /**
     * Converts Java object to NBT element
     */
    private static NbtElement objectToNbtElement(Object obj) {
        if (obj == null) return null;
        
        if (obj instanceof Byte) {
            return NbtByte.of((Byte) obj);
        } else if (obj instanceof Short) {
            return NbtShort.of((Short) obj);
        } else if (obj instanceof Integer) {
            return NbtInt.of((Integer) obj);
        } else if (obj instanceof Long) {
            return NbtLong.of((Long) obj);
        } else if (obj instanceof Float) {
            return NbtFloat.of((Float) obj);
        } else if (obj instanceof Double) {
            return NbtDouble.of((Double) obj);
        } else if (obj instanceof String) {
            return NbtString.of((String) obj);
        } else if (obj instanceof Boolean) {
            return NbtByte.of((Boolean) obj);
        } else if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            return mapToNbt(map);
        } else if (obj instanceof List) {
            NbtList list = new NbtList();
            for (Object item : (List<?>) obj) {
                NbtElement element = objectToNbtElement(item);
                if (element != null) {
                    list.add(element);
                }
            }
            return list;
        } else {
            return NbtString.of(obj.toString());
        }
    }
}