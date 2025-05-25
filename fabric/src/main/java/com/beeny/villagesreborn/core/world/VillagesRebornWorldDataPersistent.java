package com.beeny.villagesreborn.core.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Fabric-specific implementation of VillagesRebornWorldData using PersistentState
 * Handles NBT serialization and world data persistence
 */
public class VillagesRebornWorldDataPersistent extends PersistentState {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagesRebornWorldDataPersistent.class);
    private static final String DATA_NAME = "villagesreborn_world_settings";
    
    private final VillagesRebornWorldData data;
    
    public VillagesRebornWorldDataPersistent() {
        this.data = new VillagesRebornWorldData();
    }
    
    public VillagesRebornWorldDataPersistent(VillagesRebornWorldData data) {
        this.data = data;
    }
    
    /**
     * Deserializes world data from NBT
     */
    public static VillagesRebornWorldDataPersistent fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        try {
            // Convert NBT to Map and use common implementation
            Map<String, Object> map = VillagesRebornWorldSettingsExtensions.nbtToMap(nbt);
            VillagesRebornWorldData data = VillagesRebornWorldData.fromMap(map);
            
            VillagesRebornWorldDataPersistent persistent = new VillagesRebornWorldDataPersistent(data);
            LOGGER.debug("Loaded world data from NBT: {}", data);
            return persistent;
        } catch (Exception e) {
            LOGGER.error("Failed to load world data from NBT, creating new instance", e);
            return new VillagesRebornWorldDataPersistent();
        }
    }
    
    /**
     * Serializes world data to NBT - Updated for newer MC versions
     */
    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        try {
            // Convert common data to Map then to NBT
            Map<String, Object> map = data.toMap();
            NbtCompound dataCompound = VillagesRebornWorldSettingsExtensions.mapToNbt(map);
            
            // Copy all entries to the provided NBT compound
            for (String key : dataCompound.getKeys()) {
                nbt.put(key, dataCompound.get(key));
            }
            
            LOGGER.debug("Saved world data to NBT: {}", data);
        } catch (Exception e) {
            LOGGER.error("Failed to save world data to NBT", e);
        }
        
        return nbt;
    }
    
    /**
     * Gets or creates world data for the specified world
     */
    public static VillagesRebornWorldDataPersistent get(ServerWorld world) {
        return world.getPersistentStateManager()
                   .getOrCreate(
                       new PersistentState.Type<>(
                           VillagesRebornWorldDataPersistent::new,
                           VillagesRebornWorldDataPersistent::fromNbt,
                           null
                       ),
                       DATA_NAME
                   );
    }
    
    /**
     * Updates the settings and marks the data as dirty for saving
     */
    public void setSettings(VillagesRebornWorldSettings settings) {
        data.setSettings(settings);
        markDirty();
        LOGGER.debug("Updated world settings and marked dirty: {}", settings);
    }
    
    /**
     * Gets the current settings
     */
    public VillagesRebornWorldSettings getSettings() {
        return data.getSettings();
    }
    
    /**
     * Checks if settings are present
     */
    public boolean hasSettings() {
        return data.hasSettings();
    }
    
    /**
     * Gets the data version
     */
    public String getDataVersion() {
        return data.getDataVersion();
    }
    
    /**
     * Gets the creation timestamp
     */
    public long getCreatedTimestamp() {
        return data.getCreatedTimestamp();
    }
    
    /**
     * Gets the last modified timestamp
     */
    public long getLastModifiedTimestamp() {
        return data.getLastModifiedTimestamp();
    }
    
    /**
     * Gets the underlying common data object
     */
    public VillagesRebornWorldData getData() {
        return data;
    }
    
    /**
     * Updates a specific setting by name (for compatibility)
     */
    public void updateSetting(String settingName, Object value) {
        if (data.hasSettings()) {
            VillagesRebornWorldSettings settings = data.getSettings();
            // Update the specific setting based on name
            switch (settingName.toLowerCase()) {
                case "villager_memory_limit":
                    if (value instanceof Number) {
                        settings.setVillagerMemoryLimit(((Number) value).intValue());
                    }
                    break;
                case "ai_aggression_level":
                    if (value instanceof Number) {
                        settings.setAiAggressionLevel(((Number) value).floatValue());
                    }
                    break;
                case "enable_advanced_ai":
                    if (value instanceof Boolean) {
                        settings.setEnableAdvancedAI((Boolean) value);
                    }
                    break;
                // Add more cases as needed
                default:
                    LOGGER.warn("Unknown setting name: {}", settingName);
                    return;
            }
            data.setSettings(settings);
            markDirty();
        }
    }
}