package com.villagesreborn.beeny.systems.events;

import com.villagesreborn.beeny.systems.ResourceType;
import java.util.Map;

public class ResourcesConsumedEvent {
    private final Map<ResourceType, Integer> resources;

    public ResourcesConsumedEvent(Map<ResourceType, Integer> resources) {
        this.resources = resources;
    }

    public Map<ResourceType, Integer> getResources() {
        return resources;
    }
}
