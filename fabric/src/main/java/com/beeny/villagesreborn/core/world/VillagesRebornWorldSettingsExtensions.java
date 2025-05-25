package com.beeny.villagesreborn.core.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
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
     * Codec for NBT operations
     */
    public static final Codec<VillagesRebornWorldSettings> CODEC = RecordCodecBuilder.create(instance -> 
        instance.group(
            Codec.INT.fieldOf("villager_memory_limit").forGetter(VillagesRebornWorldSettings::getVillagerMemoryLimit),
            Codec.FLOAT.fieldOf("ai_aggression_level").forGetter(VillagesRebornWorldSettings::getAiAggressionLevel),
            Codec.BOOL.fieldOf("enable_advanced_ai").forGetter(VillagesRebornWorldSettings::isEnableAdvancedAI),
            Codec.BOOL.fieldOf("auto_expansion_enabled").forGetter(VillagesRebornWorldSettings::isAutoExpansionEnabled),
            Codec.INT.fieldOf("max_village_size").forGetter(VillagesRebornWorldSettings::getMaxVillageSize),
            Codec.FLOAT.fieldOf("expansion_rate").forGetter(VillagesRebornWorldSettings::getExpansionRate),
            Codec.BOOL.fieldOf("elections_enabled").forGetter(VillagesRebornWorldSettings::isElectionsEnabled),
            Codec.BOOL.fieldOf("assistant_villagers_enabled").forGetter(VillagesRebornWorldSettings::isAssistantVillagersEnabled),
            Codec.BOOL.fieldOf("dynamic_trading_enabled").forGetter(VillagesRebornWorldSettings::isDynamicTradingEnabled),
            Codec.BOOL.fieldOf("villager_relationships").forGetter(VillagesRebornWorldSettings::isVillagerRelationships),
            Codec.BOOL.fieldOf("adaptive_performance").forGetter(VillagesRebornWorldSettings::isAdaptivePerformance),
            Codec.INT.fieldOf("tick_optimization_level").forGetter(VillagesRebornWorldSettings::getTickOptimizationLevel),
            Codec.STRING.optionalFieldOf("version", "2.0").forGetter(VillagesRebornWorldSettings::getVersion),
            Codec.LONG.optionalFieldOf("created_timestamp", System.currentTimeMillis()).forGetter(VillagesRebornWorldSettings::getCreatedTimestamp)
        ).apply(instance, VillagesRebornWorldSettings::new)
    );
    
    /**
     * Serialize VillagesRebornWorldSettings to NBT
     */
    public static NbtCompound toNbt(VillagesRebornWorldSettings settings) {
        try {
            return (NbtCompound) CODEC.encodeStart(NbtOps.INSTANCE, settings)
                                     .getOrThrow(RuntimeException::new);
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
            return CODEC.parse(NbtOps.INSTANCE, nbt)
                       .getOrThrow(RuntimeException::new);
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
            // Convert NBT fields to map
            if (nbt.contains("version")) {
                map.put("version", nbt.getString("version"));
            }
            if (nbt.contains("created")) {
                map.put("created", nbt.getLong("created"));
            }
            if (nbt.contains("last_modified")) {
                map.put("last_modified", nbt.getLong("last_modified"));
            }
            if (nbt.contains("settings")) {
                // If settings are stored as a nested compound
                NbtCompound settingsNbt = nbt.getCompound("settings");
                VillagesRebornWorldSettings settings = fromNbt(settingsNbt);
                map.put("settings", settings.toMap());
            } else {
                // If settings are stored at root level
                VillagesRebornWorldSettings settings = fromNbt(nbt);
                map.put("settings", settings.toMap());
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
            // Convert map fields to NBT
            if (map.containsKey("version")) {
                nbt.putString("version", (String) map.get("version"));
            }
            if (map.containsKey("created")) {
                nbt.putLong("created", ((Number) map.get("created")).longValue());
            }
            if (map.containsKey("last_modified")) {
                nbt.putLong("last_modified", ((Number) map.get("last_modified")).longValue());
            }
            if (map.containsKey("settings")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> settingsMap = (Map<String, Object>) map.get("settings");
                VillagesRebornWorldSettings settings = VillagesRebornWorldSettings.fromMap(settingsMap);
                nbt.put("settings", toNbt(settings));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to convert Map to NBT", e);
        }
        
        return nbt;
    }
}