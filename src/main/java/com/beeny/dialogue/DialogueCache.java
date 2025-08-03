package com.beeny.dialogue;

import com.beeny.config.VillagersRebornConfig;
import com.beeny.system.VillagerDialogueSystem;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DialogueCache {
    private static final int CACHE_SIZE_LIMIT = VillagersRebornConfig.DIALOGUE_CACHE_SIZE;
    private static final LinkedHashMap<String, CacheEntry> cache = new LinkedHashMap<String, CacheEntry>(CACHE_SIZE_LIMIT, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
            return size() > CACHE_SIZE_LIMIT;
        }
    };
    
    // Synchronized cache access
    public static String get(String key) {
        synchronized (cache) {
            CacheEntry entry = cache.get(key);
            if (entry == null || entry.isExpired()) {
                cache.remove(key);
                return null;
            }
            return entry.dialogue;
        }
    }
    
    public static void put(String key, String dialogue, long ttl) {
        synchronized (cache) {
            cache.put(key, new CacheEntry(dialogue, ttl));
        }
    }
    
    public static void cleanup() {
        synchronized (cache) {
            Iterator<Map.Entry<String, CacheEntry>> it = cache.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, CacheEntry> entry = it.next();
                if (entry.getValue().isExpired()) {
                    it.remove();
                }
            }
        }
    }
    private static final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    static {
        // Schedule cleanup every 10 minutes
        cleanupExecutor.scheduleAtFixedRate(DialogueCache::cleanup, 10, 10, TimeUnit.MINUTES);
    }
    // Removed redundant shutdownCleanupExecutor() method; use shutdown() only

    private static class CacheEntry {
        public final String dialogue;
        public final long timestamp;
        public final long ttl; // Time to live in milliseconds
        
        public CacheEntry(String dialogue, long ttl) {
            this.dialogue = dialogue;
            this.timestamp = System.currentTimeMillis();
            this.ttl = ttl;
        }
        
        public boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > ttl;
        }
    }
    
    public static String generateCacheKey(VillagerDialogueSystem.DialogueContext context, 
                                        VillagerDialogueSystem.DialogueCategory category,
                                        String conversationHistory) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // Villager identity (null safety)
        String villagerName = (context.villagerData != null && context.villagerData.getName() != null)
                ? context.villagerData.getName() : "unknown";
        String villagerPersonality = (context.villagerData != null && context.villagerData.getPersonality() != null)
                ? context.villagerData.getPersonality() : "unknown";
        keyBuilder.append(villagerName).append("_");
        keyBuilder.append(villagerPersonality).append("_");

        // Category
        keyBuilder.append(category != null ? category.name() : "unknown").append("_");

        // Player relationship tier (to avoid too specific caching)
        int reputationTier = context.playerReputation / 20; // Groups of 20 reputation points
        keyBuilder.append("rep").append(reputationTier).append("_");

        // Time of day (null safety)
        String timeOfDayName = (context.timeOfDay != null && context.timeOfDay.name != null)
                ? context.timeOfDay.name : "unknown";
        keyBuilder.append(timeOfDayName).append("_");

        // Weather (null safety)
        String weatherStr = (context.weather != null) ? context.weather : "unknown";
        keyBuilder.append(weatherStr).append("_");

        // Happiness tier (null safety)
        int happiness = (context.villagerData != null) ? context.villagerData.getHappiness() : 0;
        int happinessTier = happiness / 25; // Groups of 25 happiness points
        keyBuilder.append("happy").append(happinessTier).append("_");

        // Conversation context hash (guaranteed non-negative)
        if (conversationHistory != null && !conversationHistory.trim().isEmpty()) {
            long convHash = Integer.toUnsignedLong(conversationHistory.hashCode()) % 1000;
            keyBuilder.append("conv").append(convHash);
        } else {
            keyBuilder.append("noconv");
        }

        return keyBuilder.toString();
    }
    
    public static void put(String key, String dialogue) {
        put(key, dialogue, getDefaultTTL());
    }
    
    // removed duplicate put(key, dialogue, long) definition to avoid overload collision
    
    public static boolean contains(String key) {
        if (!VillagersRebornConfig.ENABLE_DIALOGUE_CACHE) return false;
        synchronized (cache) {
            CacheEntry entry = cache.get(key);
            if (entry == null || entry.isExpired()) {
                if (entry != null) {
                    cache.remove(key); // Clean up expired entry
                }
                return false;
            }
            return true;
        }
    }
    
    public static void invalidate(String key) {
        synchronized (cache) {
            cache.remove(key);
        }
    }
    
    public static void invalidateAll() {
        synchronized (cache) {
            cache.clear();
        }
    }
    
    public static void invalidateVillager(String villagerName) {
        synchronized (cache) {
            cache.entrySet().removeIf(entry -> entry.getKey().startsWith(villagerName + "_"));
        }
    }
    
    // duplicate cleanup removed (single cleanup method defined above)
    
    public static int size() {
        synchronized (cache) {
            return cache.size();
        }
    }
    
    public static void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
        }
    }
    
    private static long getDefaultTTL() {
        // Cache entries for 5-15 minutes depending on category
        return 5 * 60 * 1000 + (long)(Math.random() * 10 * 60 * 1000);
    }
    
    public static long getCategorySpecificTTL(VillagerDialogueSystem.DialogueCategory category) {
        return switch (category) {
            case GREETING, FAREWELL -> 2 * 60 * 1000;  // 2 minutes (more dynamic)
            case WEATHER -> 10 * 60 * 1000;            // 10 minutes (weather doesn't change often)
            case WORK, HOBBY -> 15 * 60 * 1000;       // 15 minutes (more stable topics)
            case FAMILY -> 20 * 60 * 1000;            // 20 minutes (very stable)
            case GOSSIP, MOOD -> 5 * 60 * 1000;       // 5 minutes (more dynamic)
            case ADVICE, STORY -> 30 * 60 * 1000;     // 30 minutes (can be reused more)
            default -> getDefaultTTL();
        };
    }
}