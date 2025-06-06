package com.beeny.villagesreborn.core.governance;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents current state and data about a village for governance decisions
 */
public class VillageData {
    private final UUID villageId;
    private final int population;
    private final Map<String, Integer> resources;
    private final List<String> currentIssues;
    private final float happiness;
    private final float security;
    
    public VillageData(UUID villageId, int population, Map<String, Integer> resources, 
                      List<String> currentIssues, float happiness, float security) {
        this.villageId = villageId;
        this.population = population;
        this.resources = resources;
        this.currentIssues = currentIssues;
        this.happiness = Math.max(0.0f, Math.min(1.0f, happiness));
        this.security = Math.max(0.0f, Math.min(1.0f, security));
    }
    
    public UUID getVillageId() { return villageId; }
    public int getPopulation() { return population; }
    public Map<String, Integer> getResources() { return resources; }
    public List<String> getCurrentIssues() { return currentIssues; }
    public float getHappiness() { return happiness; }
    public float getSecurity() { return security; }
    
    public String getResourceSummary() {
        if (resources.isEmpty()) {
            return "No resources tracked";
        }
        
        StringBuilder summary = new StringBuilder();
        for (Map.Entry<String, Integer> entry : resources.entrySet()) {
            if (summary.length() > 0) summary.append(", ");
            summary.append(entry.getKey()).append(": ").append(entry.getValue());
        }
        return summary.toString();
    }
}