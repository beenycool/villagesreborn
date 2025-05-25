package com.beeny.villagesreborn.core.world;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Platform-agnostic world data storage for Villages Reborn settings
 * Actual persistence is handled by platform-specific implementations
 */
public class VillagesRebornWorldData {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagesRebornWorldData.class);
    
    // Constants for data keys
    private static final String VERSION_KEY = "version";
    private static final String CREATED_KEY = "created";
    private static final String LAST_MODIFIED_KEY = "last_modified";
    private static final String SETTINGS_KEY = "settings";
    
    // Default version for new data
    private static final String DEFAULT_VERSION = "2.0";
    
    // Data fields
    private String dataVersion = DEFAULT_VERSION;
    private long createdTimestamp;
    private long lastModifiedTimestamp;
    private VillagesRebornWorldSettings settings;
    
    public VillagesRebornWorldData() {
        this.createdTimestamp = System.currentTimeMillis();
        this.lastModifiedTimestamp = this.createdTimestamp;
    }
    
    public VillagesRebornWorldData(VillagesRebornWorldSettings settings) {
        this();
        this.settings = settings;
    }
    
    /**
     * Serializes world data to a Map for platform-agnostic storage
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        
        try {
            // Save metadata
            map.put(VERSION_KEY, dataVersion);
            map.put(CREATED_KEY, createdTimestamp);
            map.put(LAST_MODIFIED_KEY, System.currentTimeMillis());
            
            // Save settings if present
            if (settings != null) {
                map.put(SETTINGS_KEY, settings.toMap());
                LOGGER.debug("Saved world settings to map: {}", settings);
            } else {
                LOGGER.warn("No settings to save to map");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to save world data to map", e);
        }
        
        return map;
    }
    
    /**
     * Deserializes world data from a Map
     */
    public static VillagesRebornWorldData fromMap(Map<String, Object> map) {
        VillagesRebornWorldData data = new VillagesRebornWorldData();
        
        try {
            // Load metadata
            if (map.containsKey(VERSION_KEY)) {
                data.dataVersion = (String) map.get(VERSION_KEY);
            }
            if (map.containsKey(CREATED_KEY)) {
                data.createdTimestamp = ((Number) map.get(CREATED_KEY)).longValue();
            }
            if (map.containsKey(LAST_MODIFIED_KEY)) {
                data.lastModifiedTimestamp = ((Number) map.get(LAST_MODIFIED_KEY)).longValue();
            }
            
            // Load settings if present
            if (map.containsKey(SETTINGS_KEY)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> settingsMap = (Map<String, Object>) map.get(SETTINGS_KEY);
                data.settings = VillagesRebornWorldSettings.fromMap(settingsMap);
                LOGGER.debug("Loaded world settings from map: {}", data.settings);
            } else {
                LOGGER.warn("No settings found in world data map");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load world data from map, creating new instance", e);
            data = new VillagesRebornWorldData();
        }
        
        return data;
    }
    
    /**
     * Updates the settings and marks the data as modified
     */
    public void setSettings(VillagesRebornWorldSettings settings) {
        this.settings = settings;
        this.lastModifiedTimestamp = System.currentTimeMillis();
        LOGGER.debug("Updated world settings: {}", settings);
    }
    
    /**
     * Gets the current settings
     */
    public VillagesRebornWorldSettings getSettings() {
        return settings;
    }
    
    /**
     * Checks if settings are present
     */
    public boolean hasSettings() {
        return settings != null;
    }
    
    /**
     * Gets the data version
     */
    public String getDataVersion() {
        return dataVersion;
    }
    
    /**
     * Gets the creation timestamp
     */
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }
    
    /**
     * Gets the last modified timestamp
     */
    public long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }
    
    /**
     * Creates a copy of this world data
     */
    public VillagesRebornWorldData copy() {
        VillagesRebornWorldData copy = new VillagesRebornWorldData();
        copy.dataVersion = this.dataVersion;
        copy.createdTimestamp = this.createdTimestamp;
        copy.lastModifiedTimestamp = this.lastModifiedTimestamp;
        copy.settings = this.settings != null ? this.settings.copy() : null;
        return copy;
    }
    
    @Override
    public String toString() {
        return "VillagesRebornWorldData{" +
                "dataVersion='" + dataVersion + '\'' +
                ", createdTimestamp=" + createdTimestamp +
                ", lastModifiedTimestamp=" + lastModifiedTimestamp +
                ", hasSettings=" + hasSettings() +
                '}';
    }
}