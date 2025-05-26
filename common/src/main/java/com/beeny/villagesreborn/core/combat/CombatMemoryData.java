package com.beeny.villagesreborn.core.combat;

import com.beeny.villagesreborn.core.common.NBTCompound;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores AI memory data for combat decisions and learning.
 * Tracks encounter history, learned strategies, and decision patterns.
 */
public class CombatMemoryData {
    
    private final Map<UUID, Integer> enemyEncounterHistory = new HashMap<>();
    private final Map<String, CombatDecision> learnedStrategies = new HashMap<>();
    private long lastAIDecisionTimestamp = 0;
    private int totalCombatEncounters = 0;
    private float averageSuccessRate = 0.5f;
    
    public CombatMemoryData() {
    }
    
    /**
     * Records an encounter with a specific enemy type
     */
    public void recordEnemyEncounter(UUID enemyId) {
        enemyEncounterHistory.merge(enemyId, 1, Integer::sum);
        totalCombatEncounters++;
    }
    
    /**
     * Stores a successful combat strategy for future reference
     */
    public void learnStrategy(String situationType, CombatDecision decision) {
        learnedStrategies.put(situationType, decision);
    }
    
    /**
     * Gets the number of times this enemy type has been encountered
     */
    public int getEnemyEncounterCount(UUID enemyId) {
        return enemyEncounterHistory.getOrDefault(enemyId, 0);
    }
    
    /**
     * Gets a learned strategy for a specific situation type
     */
    public CombatDecision getLearnedStrategy(String situationType) {
        return learnedStrategies.get(situationType);
    }
    
    /**
     * Updates the timestamp of the last AI decision
     */
    public void updateLastAIDecision() {
        this.lastAIDecisionTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Updates the success rate based on combat outcomes
     */
    public void updateSuccessRate(boolean wasSuccessful) {
        // Simple exponential moving average
        float weight = 0.1f;
        float outcome = wasSuccessful ? 1.0f : 0.0f;
        averageSuccessRate = (1 - weight) * averageSuccessRate + weight * outcome;
    }
    
    // Getters
    public Map<UUID, Integer> getEnemyEncounterHistory() {
        return new HashMap<>(enemyEncounterHistory);
    }
    
    public Map<String, CombatDecision> getLearnedStrategies() {
        return new HashMap<>(learnedStrategies);
    }
    
    public long getLastAIDecisionTimestamp() {
        return lastAIDecisionTimestamp;
    }
    
    public int getTotalCombatEncounters() {
        return totalCombatEncounters;
    }
    
    public float getAverageSuccessRate() {
        return averageSuccessRate;
    }
    
    /**
     * Serializes memory data to NBT for persistence
     */
    public NBTCompound serializeToNBT() {
        NBTCompound nbt = new NBTCompound();
        
        // Serialize encounter history
        NBTCompound encounterNbt = new NBTCompound();
        for (Map.Entry<UUID, Integer> entry : enemyEncounterHistory.entrySet()) {
            encounterNbt.putInt(entry.getKey().toString(), entry.getValue());
        }
        nbt.put("enemyEncounters", encounterNbt);
        
        // Serialize basic stats
        nbt.putLong("lastAIDecision", lastAIDecisionTimestamp);
        nbt.putInt("totalEncounters", totalCombatEncounters);
        nbt.putFloat("successRate", averageSuccessRate);
        
        return nbt;
    }
    
    /**
     * Deserializes memory data from NBT
     */
    public static CombatMemoryData fromNBT(NBTCompound nbt) {
        CombatMemoryData memory = new CombatMemoryData();
        
        // Deserialize encounter history
        if (nbt.contains("enemyEncounters")) {
            NBTCompound encounterNbt = nbt.getCompound("enemyEncounters");
            for (String key : encounterNbt.getData().keySet()) {
                try {
                    UUID enemyId = UUID.fromString(key);
                    int count = encounterNbt.getInt(key);
                    memory.enemyEncounterHistory.put(enemyId, count);
                } catch (IllegalArgumentException e) {
                    // Skip invalid UUIDs
                }
            }
        }
        
        // Deserialize basic stats
        memory.lastAIDecisionTimestamp = nbt.getLong("lastAIDecision");
        memory.totalCombatEncounters = nbt.getInt("totalEncounters");
        memory.averageSuccessRate = nbt.getFloat("successRate");
        
        return memory;
    }
    
    /**
     * Imports data from rule-based combat history for migration
     */
    public void importFromRuleBasedHistory(Object legacyHistory) {
        // Placeholder for migration logic
        // In a real implementation, this would parse legacy combat data
        this.totalCombatEncounters = 0;
        this.averageSuccessRate = 0.5f;
    }
    
    /**
     * Clears old data to prevent memory bloat
     */
    public void cleanup() {
        // Remove encounters older than a certain threshold
        if (enemyEncounterHistory.size() > 1000) {
            // Keep only the most recent encounters
            enemyEncounterHistory.clear();
        }
        
        // Clear old strategies
        if (learnedStrategies.size() > 50) {
            learnedStrategies.clear();
        }
    }
}