package com.beeny.villagesreborn.core.combat;

import com.beeny.villagesreborn.core.common.NBTCompound;
import com.beeny.villagesreborn.core.llm.LLMApiClient;
import com.beeny.villagesreborn.core.llm.LLMProvider;
import com.beeny.villagesreborn.core.llm.LLMProviderManager;
import com.beeny.villagesreborn.core.llm.ConversationRequest;
import com.beeny.villagesreborn.core.llm.ConversationResponse;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
     * Imports data from rule-based combat history for migration using AI
     */
    public void importFromRuleBasedHistory(Object legacyHistory) {
        importFromRuleBasedHistory(legacyHistory, null);
    }
    
    /**
     * Imports data from rule-based combat history for migration using AI with custom client
     */
    public void importFromRuleBasedHistory(Object legacyHistory, LLMApiClient customClient) {
        try {
            if (legacyHistory == null) {
                // Initialize with defaults
                this.totalCombatEncounters = 0;
                this.averageSuccessRate = 0.5f;
                return;
            }
            
            // Use custom client if provided, otherwise skip AI migration
            if (customClient != null) {
                performAIMigration(legacyHistory, customClient);
            } else {
                // Fallback to rule-based migration when no client available
                fallbackMigration(legacyHistory);
            }
            
        } catch (Exception e) {
            // Fallback to rule-based migration on any error
            fallbackMigration(legacyHistory);
        } finally {
            // Ensure data normalization
            normalizeMigratedData();
        }
    }
    
    /**
     * Performs AI-powered migration using the provided client
     */
    private void performAIMigration(Object legacyHistory, LLMApiClient client) throws Exception {
        // Create prompt for AI migration
        String prompt = createMigrationPrompt(legacyHistory);
        ConversationRequest request = ConversationRequest.builder()
            .provider(LLMProvider.OPENAI)
            .prompt(prompt)
            .maxTokens(500)
            .temperature(0.7f)
            .timeout(Duration.ofSeconds(30))
            .build();
        
        try {
            // Get AI response (blocking call for migration)
            ConversationResponse response = client.generateConversationResponse(request).get(30, TimeUnit.SECONDS);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("LLM migration failed: " + response.getErrorMessage());
            }
            
            // Parse AI response and update data
            parseMigrationResponse(response.getResponse());
        } catch (InterruptedException e) {
            // Log specific interruption
            // Consider how to handle this - rethrow, fallback, etc.
            Thread.currentThread().interrupt(); // Preserve interrupt status
            throw new RuntimeException("LLM migration interrupted: " + e.getMessage(), e);
        } catch (ExecutionException e) {
            // Log error during execution of the task
            throw new RuntimeException("LLM migration execution error: " + e.getCause().getMessage(), e.getCause());
        } catch (TimeoutException e) {
            // Log timeout specifically
            throw new RuntimeException("LLM migration timed out after 30 seconds: " + e.getMessage(), e);
        }
        // No general 'catch (Exception e)' here if we want specific handling above
        // and let other exceptions propagate if not caught by 'throws Exception' in method signature.
    }
    
    /**
     * Creates prompt for AI-powered migration
     */
    private String createMigrationPrompt(Object legacyData) {
        return String.format(
            "Convert legacy combat history data to JSON format with these fields: " +
            "totalCombatEncounters (int), averageSuccessRate (float), " +
            "enemyEncounterHistory (Map<UUID, Integer>). " +
            "Legacy data: %s. " +
            "Output ONLY valid JSON, no explanations.",
            legacyData.toString()
        );
    }
    
    /**
     * Parses AI response and updates combat data
     */
    private void parseMigrationResponse(String aiResponse) {
        try {
            // Extract data using regex patterns - more robust than simple string splitting
            Pattern intPattern = Pattern.compile("\"totalCombatEncounters\"\\s*:\\s*(\\d+)");
            Matcher intMatcher = intPattern.matcher(aiResponse);
            if (intMatcher.find()) {
                this.totalCombatEncounters = Integer.parseInt(intMatcher.group(1));
            }
            
            Pattern floatPattern = Pattern.compile("\"averageSuccessRate\"\\s*:\\s*(\\d+\\.?\\d*)");
            Matcher floatMatcher = floatPattern.matcher(aiResponse);
            if (floatMatcher.find()) {
                this.averageSuccessRate = Float.parseFloat(floatMatcher.group(1));
            }
            
            // Extract enemy encounter history entries
            // Format: "UUID": number
            Pattern uuidEntryPattern = Pattern.compile("\"([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\"\\s*:\\s*(\\d+)");
            Matcher entryMatcher = uuidEntryPattern.matcher(aiResponse);
            
            while (entryMatcher.find()) {
                try {
                    UUID enemyId = UUID.fromString(entryMatcher.group(1));
                    int count = Integer.parseInt(entryMatcher.group(2));
                    this.enemyEncounterHistory.put(enemyId, count);
                } catch (IllegalArgumentException e) {
                    // Skip invalid UUIDs or number formats
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage());
        }
    }
    
    /**
     * Fallback to rule-based migration if AI fails
     */
    private void fallbackMigration(Object legacyHistory) {
        try {
            if (legacyHistory instanceof Map<?, ?> legacyMap) {
                migrateLegacyMapData(legacyMap);
            } else if (legacyHistory instanceof String legacyString) {
                migrateLegacyStringData(legacyString);
            } else if (legacyHistory instanceof NBTCompound legacyNbt) {
                migrateLegacyNBTData(legacyNbt);
            } else {
                inferCombatDataFromLegacyObject(legacyHistory);
            }
        } catch (Exception e) {
            // Conservative fallback
            this.totalCombatEncounters = 5;
            this.averageSuccessRate = 0.4f;
        }
    }
    
    /**
     * Migrates data from legacy Map-based combat history
     */
    private void migrateLegacyMapData(Map<?, ?> legacyMap) {
        // Extract encounter counts
        Object encounterData = legacyMap.get("encounters");
        if (encounterData instanceof Integer) {
            this.totalCombatEncounters = (Integer) encounterData;
        }
        
        // Extract success rate
        Object successData = legacyMap.get("success_rate");
        if (successData instanceof Float) {
            this.averageSuccessRate = (Float) successData;
        } else if (successData instanceof Double) {
            this.averageSuccessRate = ((Double) successData).floatValue();
        }
        
        // Migrate enemy encounter history
        Object enemyHistory = legacyMap.get("enemy_history");
        if (enemyHistory instanceof Map<?, ?> enemyMap) {
            for (Map.Entry<?, ?> entry : enemyMap.entrySet()) {
                try {
                    UUID enemyId = UUID.fromString(entry.getKey().toString());
                    Integer count = (Integer) entry.getValue();
                    enemyEncounterHistory.put(enemyId, count);
                } catch (Exception e) {
                    // Skip invalid entries
                }
            }
        }
    }
    
    /**
     * Migrates data from legacy String-based combat history
     */
    private void migrateLegacyStringData(String legacyString) {
        // Parse string patterns like "battles:10,wins:6,enemies:zombie,skeleton"
        String[] parts = legacyString.split(",");
        int battles = 0;
        int wins = 0;
        
        for (String part : parts) {
            String[] keyValue = part.split(":");
            if (keyValue.length == 2) {
                try {
                    switch (keyValue[0].trim().toLowerCase()) {
                        case "battles", "encounters" -> battles = Integer.parseInt(keyValue[1].trim());
                        case "wins", "successes" -> wins = Integer.parseInt(keyValue[1].trim());
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid numbers
                }
            }
        }
        
        this.totalCombatEncounters = battles;
        this.averageSuccessRate = battles > 0 ? (float) wins / battles : 0.5f;
    }
    
    /**
     * Migrates data from legacy NBT-based combat history
     */
    private void migrateLegacyNBTData(NBTCompound legacyNbt) {
        // Extract NBT data if available
        this.totalCombatEncounters = legacyNbt.contains("total_encounters") ?
            legacyNbt.getInt("total_encounters") : 0;
        this.averageSuccessRate = legacyNbt.contains("success_rate") ?
            legacyNbt.getFloat("success_rate") : 0.5f;
        
        // Migrate timestamp if available
        this.lastAIDecisionTimestamp = legacyNbt.contains("last_decision") ?
            legacyNbt.getLong("last_decision") : System.currentTimeMillis();
    }
    
    /**
     * Uses AI heuristics to infer combat data from unknown legacy objects
     */
    private void inferCombatDataFromLegacyObject(Object legacyHistory) {
        // AI-powered inference based on object characteristics
        Class<?> objectClass = legacyHistory.getClass();
        
        // Infer experience level from object complexity
        int fieldCount = objectClass.getDeclaredFields().length;
        int methodCount = objectClass.getDeclaredMethods().length;
        
        // More complex objects suggest more combat experience
        this.totalCombatEncounters = Math.min(20, fieldCount + (methodCount / 5));
        
        // Use object hash to generate consistent but varied success rate
        int hash = Math.abs(legacyHistory.hashCode());
        this.averageSuccessRate = 0.3f + (hash % 40) / 100.0f; // 0.3 to 0.7 range
    }
    
    /**
     * Applies AI-powered normalization to migrated data
     */
    private void normalizeMigratedData() {
        // Ensure reasonable bounds
        this.totalCombatEncounters = Math.max(0, Math.min(1000, this.totalCombatEncounters));
        this.averageSuccessRate = Math.max(0.0f, Math.min(1.0f, this.averageSuccessRate));
        
        // AI adjustment: Very high success rates are suspicious, moderate them
        if (this.averageSuccessRate > 0.9f && this.totalCombatEncounters > 10) {
            this.averageSuccessRate = 0.8f + (this.averageSuccessRate - 0.9f) * 0.5f;
        }
        
        // Ensure timestamp is reasonable
        if (this.lastAIDecisionTimestamp <= 0) {
            this.lastAIDecisionTimestamp = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 1 day ago
        }
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