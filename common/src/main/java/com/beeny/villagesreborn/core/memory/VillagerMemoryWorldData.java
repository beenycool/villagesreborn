package com.beeny.villagesreborn.core.memory;

import com.beeny.villagesreborn.core.ai.VillagerBrain;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages world-level villager memory data with persistence support
 */
public class VillagerMemoryWorldData {
    private final Map<UUID, VillagerBrain> villagerBrains = new ConcurrentHashMap<>();
    private final List<GlobalMemoryEvent> globalEvents = new ArrayList<>();
    private final Map<String, Object> villageRelationships = new HashMap<>();
    private boolean dirty = false;
    
    public VillagerMemoryWorldData() {
        // Initialize empty world data
    }
    
    /**
     * Store a villager brain in world data
     */
    public void storeVillagerBrain(UUID villagerUUID, VillagerBrain brain) {
        villagerBrains.put(villagerUUID, brain);
        markDirty();
    }
    
    /**
     * Load a villager brain from world data
     */
    public VillagerBrain loadVillagerBrain(UUID villagerUUID) {
        return villagerBrains.get(villagerUUID);
    }
    
    /**
     * Record a global memory event
     */
    public void recordGlobalEvent(GlobalMemoryEvent event) {
        globalEvents.add(event);
        markDirty();
    }
    
    /**
     * Get all villager brains
     */
    public Map<UUID, VillagerBrain> getVillagerBrains() {
        return villagerBrains;
    }
    
    /**
     * Get all global memory events
     */
    public List<GlobalMemoryEvent> getGlobalMemoryEvents() {
        return globalEvents;
    }
    
    /**
     * Check if data has been modified
     */
    public boolean isDirty() {
        return dirty;
    }
    
    /**
     * Mark data as modified
     */
    public void markDirty() {
        this.dirty = true;
    }
    
    /**
     * Mark data as clean (saved)
     */
    public void markClean() {
        this.dirty = false;
    }
    
    /**
     * Serialize to map for persistence
     */
    public Map<String, Object> toMap() {
        Map<String, Object> data = new HashMap<>();
        
        // Serialize villager brains
        Map<String, Object> brainData = new HashMap<>();
        for (Map.Entry<UUID, VillagerBrain> entry : villagerBrains.entrySet()) {
            brainData.put(entry.getKey().toString(), entry.getValue().toNBT().getData());
        }
        data.put("villager_brains", brainData);
        
        // Serialize global events
        List<Map<String, Object>> eventData = new ArrayList<>();
        for (GlobalMemoryEvent event : globalEvents) {
            Map<String, Object> eventMap = new HashMap<>();
            eventMap.put("type", event.getEventType());
            eventMap.put("timestamp", event.getTimestamp());
            eventMap.put("data", event.getEventData());
            eventData.add(eventMap);
        }
        data.put("global_events", eventData);
        
        // Serialize village relationships
        data.put("village_relationships", villageRelationships);
        
        return data;
    }
    
    /**
     * Deserialize from map
     */
    public static VillagerMemoryWorldData fromMap(Map<String, Object> data) {
        VillagerMemoryWorldData worldData = new VillagerMemoryWorldData();
        
        // Handle migration from older formats
        if (data.containsKey("version") && "1.0.0".equals(data.get("version"))) {
            // Legacy format - attempt graceful migration
            return worldData; // Return empty data for now
        }
        
        // Load villager brains
        if (data.containsKey("villager_brains")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> brainData = (Map<String, Object>) data.get("villager_brains");
            for (Map.Entry<String, Object> entry : brainData.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    // Note: Would need proper NBT deserialization in real implementation
                    VillagerBrain brain = new VillagerBrain(uuid);
                    worldData.villagerBrains.put(uuid, brain);
                } catch (Exception e) {
                    // Skip corrupted entries
                }
            }
        }
        
        // Load global events
        if (data.containsKey("global_events")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> eventData = (List<Map<String, Object>>) data.get("global_events");
            for (Map<String, Object> eventMap : eventData) {
                try {
                    String type = (String) eventMap.get("type");
                    Long timestamp = (Long) eventMap.get("timestamp");
                    @SuppressWarnings("unchecked")
                    Map<String, String> eventDataMap = (Map<String, String>) eventMap.get("data");
                    
                    GlobalMemoryEvent event = new GlobalMemoryEvent(type, timestamp, eventDataMap);
                    worldData.globalEvents.add(event);
                } catch (Exception e) {
                    // Skip corrupted events
                }
            }
        }
        
        return worldData;
    }
}