package com.villagesreborn.beeny.systems;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceManager {
    private final Map<ResourceType, Integer> resourceStock = new ConcurrentHashMap<>();

    public synchronized void addResource(ResourceType
package com.villagesreborn.beeny.systems;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceManager {
    public static void adjustCachingStrategy(float performanceFactor) {
        // Get base cache size from config
        int baseCacheSize = ConfigHandler.getConfig().performance.maxCachedEntities.base;
        
        // Get multiplier for current performance rating
        float multiplier = HardwareChecker.currentRating.performanceFactor;
        
        // Apply dynamic adjustment with configurable multipliers
        int adjustedCacheSize = (int)(baseCacheSize * multiplier * performanceFactor);
        
        LOGGER.info("Adjusting resource cache size to {} (Base: {}, Multiplier: {}, Factor: {})",
            adjustedCacheSize, baseCacheSize, multiplier, performanceFactor);
            
        // Update cache implementation
        ResourceCache.setMaxSize(adjustedCacheSize);
        
        // Dispatch event for UI updates
        EventDispatcher.publish(new CacheSizeUpdatedEvent(adjustedCacheSize));
    }
    private final Map<ResourceType, Integer> resourceStock = new ConcurrentHashMap<>();

    public synchronized void addResource(ResourceType type, int quantity) {
        resourceStock.merge(type, quantity, Integer::sum);
        EventDispatcher.publish(new ResourceUpdatedEvent(type, quantity));
    }

    public synchronized boolean consumeResources(Map<ResourceType, Integer> requiredResources) {
        if (!hasSufficientResources(requiredResources)) {
            return false;
        }
        
        requiredResources.forEach((type, quantity) -> 
            resourceStock.compute(type, (k, v) -> v != null ? v - quantity : -quantity)
        );
        // Emit ResourcesConsumedEvent here
        return true;
    }

    public synchronized boolean hasSufficientResources(Map<ResourceType, Integer> requiredResources) {
        return requiredResources.entrySet().stream()
            .allMatch(entry -> 
                resourceStock.getOrDefault(entry.getKey(), 0) >= entry.getValue()
            );
    }

    public synchronized Map<ResourceType, Integer> getCurrentStock() {
        return new ConcurrentHashMap<>(resourceStock);
    }

    public synchronized void releaseResources(Map<ResourceType, Integer> resources) {
        resources.forEach((type, quantity) -> 
            resourceStock.merge(type, quantity, Integer::sum)
        );
        // Emit ResourcesReleasedEvent here
    }
}
