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
    private final Map<ResourceType, Integer> resourceStock = new ConcurrentHashMap<>();

    public synchronized void addResource(ResourceType type, int quantity) {
        resourceStock.merge(type, quantity, Integer::sum);
        // Emit ResourceUpdatedEvent with type and quantity
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
