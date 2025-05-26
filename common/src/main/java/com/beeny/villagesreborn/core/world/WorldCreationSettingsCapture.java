package com.beeny.villagesreborn.core.world;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe mechanism for capturing and transferring world creation settings
 * between the UI thread and server world initialization
 */
public class WorldCreationSettingsCapture {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldCreationSettingsCapture.class);
    
    // Thread-local storage for immediate capture
    private static final ThreadLocal<VillagesRebornWorldSettings> CAPTURED_SETTINGS = new ThreadLocal<>();
    
    // Global cache with session IDs for multi-thread scenarios
    private static final Map<String, VillagesRebornWorldSettings> WORLD_SETTINGS_CACHE = new ConcurrentHashMap<>();
    
    // Thread-local storage for spawn biome choices
    private static final ThreadLocal<Object> SPAWN_BIOME_CHOICE = new ThreadLocal<>();
    
    // Global cache for spawn biome choices with session IDs
    private static final Map<String, Object> SPAWN_BIOME_CACHE = new ConcurrentHashMap<>();
    
    // Cleanup timeout (5 minutes)
    private static final int CLEANUP_TIMEOUT_MINUTES = 5;
    
    /**
     * Captures world settings from the UI thread
     * Uses both thread-local storage and global cache for reliability
     * 
     * @param settings The settings to capture
     */
    public static void capture(VillagesRebornWorldSettings settings) {
        if (settings == null) {
            LOGGER.warn("Attempted to capture null world settings");
            return;
        }
        
        // Validate settings before capture
        VillagesRebornWorldSettings validatedSettings = settings.copy();
        validatedSettings.validate();
        
        // Store in thread-local for immediate access
        CAPTURED_SETTINGS.set(validatedSettings);
        
        // Generate session ID and cache for cross-thread access
        String sessionId = generateSessionId();
        WORLD_SETTINGS_CACHE.put(sessionId, validatedSettings);
        
        LOGGER.info("Captured world settings for session: {} - {}", sessionId, validatedSettings);
        
        // Schedule cleanup to prevent memory leaks
        scheduleCleanup(sessionId);
    }
    
    /**
     * Retrieves captured world settings
     * First tries thread-local, then falls back to most recent cached settings
     * 
     * @return The captured settings, or null if none available
     */
    public static VillagesRebornWorldSettings retrieve() {
        // Try thread-local first
        VillagesRebornWorldSettings settings = CAPTURED_SETTINGS.get();
        if (settings != null) {
            CAPTURED_SETTINGS.remove(); // Clean up after retrieval
            LOGGER.debug("Retrieved settings from thread-local storage");
            return settings;
        }
        
        // Fallback to most recent cached settings
        if (!WORLD_SETTINGS_CACHE.isEmpty()) {
            settings = WORLD_SETTINGS_CACHE.values().stream()
                                          .findFirst()
                                          .orElse(null);
            if (settings != null) {
                LOGGER.debug("Retrieved settings from global cache");
                return settings;
            }
        }
        
        LOGGER.warn("No captured world settings found");
        return null;
    }
    
    /**
     * Retrieves and removes settings for a specific session
     * 
     * @param sessionId The session identifier
     * @return The settings for this session, or null
     */
    public static VillagesRebornWorldSettings retrieveForSession(String sessionId) {
        VillagesRebornWorldSettings settings = WORLD_SETTINGS_CACHE.remove(sessionId);
        if (settings != null) {
            LOGGER.debug("Retrieved and removed settings for session: {}", sessionId);
        } else {
            LOGGER.warn("No settings found for session: {}", sessionId);
        }
        return settings;
    }
    
    /**
     * Sets the spawn biome choice for world creation
     * @param biomeChoice The biome choice (BiomeDisplayInfo or similar)
     */
    public static void setSpawnBiomeChoice(Object biomeChoice) {
        if (biomeChoice == null) {
            LOGGER.warn("Attempted to set null spawn biome choice");
            return;
        }
        
        // Store in thread-local for immediate access
        SPAWN_BIOME_CHOICE.set(biomeChoice);
        
        // Generate session ID and cache for cross-thread access
        String sessionId = generateSessionId();
        SPAWN_BIOME_CACHE.put(sessionId, biomeChoice);
        
        LOGGER.info("Set spawn biome choice for session: {} - {}", sessionId, biomeChoice);
        
        // Schedule cleanup to prevent memory leaks
        scheduleSpawnBiomeCleanup(sessionId);
    }
    
    /**
     * Retrieves the spawn biome choice
     * @return The spawn biome choice, or null if none available
     */
    public static Object getSpawnBiomeChoice() {
        // Try thread-local first
        Object choice = SPAWN_BIOME_CHOICE.get();
        if (choice != null) {
            SPAWN_BIOME_CHOICE.remove(); // Clean up after retrieval
            LOGGER.debug("Retrieved spawn biome choice from thread-local storage");
            return choice;
        }
        
        // Fallback to most recent cached choice
        if (!SPAWN_BIOME_CACHE.isEmpty()) {
            choice = SPAWN_BIOME_CACHE.values().stream()
                                     .findFirst()
                                     .orElse(null);
            if (choice != null) {
                LOGGER.debug("Retrieved spawn biome choice from global cache");
                return choice;
            }
        }
        
        LOGGER.debug("No spawn biome choice found");
        return null;
    }
    
    /**
     * Clears all captured settings
     * Used for cleanup and testing
     */
    public static void clearAll() {
        CAPTURED_SETTINGS.remove();
        SPAWN_BIOME_CHOICE.remove();
        int cacheSize = WORLD_SETTINGS_CACHE.size();
        int biomesCacheSize = SPAWN_BIOME_CACHE.size();
        WORLD_SETTINGS_CACHE.clear();
        SPAWN_BIOME_CACHE.clear();
        LOGGER.debug("Cleared all captured settings (cache had {} entries, biomes cache had {} entries)",
                    cacheSize, biomesCacheSize);
    }
    
    /**
     * Returns the number of cached settings (for monitoring/debugging)
     */
    public static int getCacheSize() {
        return WORLD_SETTINGS_CACHE.size();
    }
    
    /**
     * Checks if any settings are currently captured
     */
    public static boolean hasSettings() {
        return CAPTURED_SETTINGS.get() != null || !WORLD_SETTINGS_CACHE.isEmpty();
    }
    
    /**
     * Generates a unique session ID for cross-thread identification
     */
    private static String generateSessionId() {
        return Thread.currentThread().getName() + "_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString(System.identityHashCode(Thread.currentThread()));
    }
    
    /**
     * Schedules cleanup of cached settings to prevent memory leaks
     */
    private static void scheduleCleanup(String sessionId) {
        CompletableFuture.delayedExecutor(CLEANUP_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                        .execute(() -> {
                            VillagesRebornWorldSettings removed = WORLD_SETTINGS_CACHE.remove(sessionId);
                            if (removed != null) {
                                LOGGER.debug("Cleaned up expired settings for session: {}", sessionId);
                            }
                        });
    }
    
    /**
     * Manual cleanup method for immediate removal of old entries
     * Can be called periodically to manage memory usage
     */
    public static void performCleanup() {
        long cutoffTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(CLEANUP_TIMEOUT_MINUTES);
        
        WORLD_SETTINGS_CACHE.entrySet().removeIf(entry -> {
            String sessionId = entry.getKey();
            // Extract timestamp from session ID
            String[] parts = sessionId.split("_");
            if (parts.length >= 2) {
                try {
                    long timestamp = Long.parseLong(parts[1]);
                    if (timestamp < cutoffTime) {
                        LOGGER.debug("Cleaning up expired session: {}", sessionId);
                        return true;
                    }
                } catch (NumberFormatException e) {
                    LOGGER.warn("Invalid session ID format, removing: {}", sessionId);
                    return true;
                }
            }
            return false;
        });
    }
    
    /**
     * Schedules cleanup of cached spawn biome choices to prevent memory leaks
     */
    private static void scheduleSpawnBiomeCleanup(String sessionId) {
        CompletableFuture.delayedExecutor(CLEANUP_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                        .execute(() -> {
                            Object removed = SPAWN_BIOME_CACHE.remove(sessionId);
                            if (removed != null) {
                                LOGGER.debug("Cleaned up expired spawn biome choice for session: {}", sessionId);
                            }
                        });
    }
}