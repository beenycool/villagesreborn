package com.beeny.villagesreborn.core.world;

import net.minecraft.nbt.NbtCompound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Fabric-specific extensions for VillagesRebornWorldSettings
 * Provides NBT serialization using Minecraft's codec system
 */
public class VillagesRebornWorldSettingsExtensions {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagesRebornWorldSettingsExtensions.class);
    
    /**
     * Direct serialization to NBT without using codec - more reliable approach
     */
    
    /**
     * Serialize VillagesRebornWorldSettings to NBT
     */
    public static NbtCompound toNbt(VillagesRebornWorldSettings settings) {
        try {
            Map<String, Object> settingsMap = settings.toMap();
            return mapToNbt(settingsMap);
        } catch (Exception e) {
            LOGGER.error("Failed to serialize world settings to NBT", e);
            return new NbtCompound();
        }
    }
    
    /**
     * Deserialize VillagesRebornWorldSettings from NBT
     */
    public static VillagesRebornWorldSettings fromNbt(NbtCompound nbt) {
        try {
            Map<String, Object> settingsMap = nbtToMap(nbt);
            return VillagesRebornWorldSettings.fromMap(settingsMap);
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize world settings from NBT, using defaults", e);
            return new VillagesRebornWorldSettings();
        }
    }
    
    /**
     * Convert NBT to Map for platform-agnostic processing
     */
    public static Map<String, Object> nbtToMap(NbtCompound nbt) {
        Map<String, Object> map = new HashMap<>();
        
        try {
            // Convert all NBT entries to map
            for (String key : nbt.getKeys()) {
                var element = nbt.get(key);
                if (element == null) continue;
                
                switch (element.getType()) {
                    case 1: // Byte (boolean)
                        map.put(key, nbt.getBoolean(key));
                        break;
                    case 3: // Int
                        map.put(key, nbt.getInt(key));
                        break;
                    case 5: // Float
                        map.put(key, nbt.getFloat(key));
                        break;
                    case 6: // Double
                        map.put(key, nbt.getDouble(key));
                        break;
                    case 4: // Long
                        map.put(key, nbt.getLong(key));
                        break;
                    case 8: // String
                        map.put(key, nbt.getString(key));
                        break;
                    case 10: // Compound (nested map)
                        NbtCompound nestedNbt = nbt.getCompound(key);
                        Map<String, Object> nestedMap = nbtToMap(nestedNbt);
                        map.put(key, nestedMap);
                        break;
                    default:
                        // For other types, try to get as string
                        String stringValue = nbt.getString(key);
                        if (!stringValue.isEmpty()) {
                            map.put(key, stringValue);
                        }
                        break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to convert NBT to Map", e);
        }
        
        return map;
    }
    
    /**
     * Convert Map to NBT for platform-specific storage
     */
    public static NbtCompound mapToNbt(Map<String, Object> map) {
        NbtCompound nbt = new NbtCompound();
        
        try {
            // Convert all map entries to NBT
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                if (value instanceof Boolean) {
                    nbt.putBoolean(key, (Boolean) value);
                } else if (value instanceof Integer) {
                    nbt.putInt(key, (Integer) value);
                } else if (value instanceof Float) {
                    nbt.putFloat(key, (Float) value);
                } else if (value instanceof Double) {
                    nbt.putDouble(key, (Double) value);
                } else if (value instanceof Long) {
                    nbt.putLong(key, (Long) value);
                } else if (value instanceof String) {
                    nbt.putString(key, (String) value);
                } else if (value instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nestedMap = (Map<String, Object>) value;
                    NbtCompound nestedNbt = mapToNbt(nestedMap);
                    nbt.put(key, nestedNbt);
                } else if (value != null) {
                    // Fallback to string representation
                    nbt.putString(key, value.toString());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to convert Map to NBT", e);
        }
        
        return nbt;
    }
}