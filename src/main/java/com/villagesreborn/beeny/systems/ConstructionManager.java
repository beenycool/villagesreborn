package com.villagesreborn.beeny.systems;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ConstructionManager {
    private final Map<Integer, ConstructionProject> activeProjects = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ResourceManager resourceManager;
    private final AtomicInteger projectIdCounter = new AtomicInteger(0);
    
    // Bank building types and capacities
    public enum BankType {
        SMALL_BANK(16, 1000),  // 16 slots, 1000 prosperity required
        MEDIUM_BANK(32, 2500),
        LARGE_BANK(64, 5000),
        STONE_BANK(64, 7500), // Stone Bank - 64 slots, 7500 prosperity
        VAULT_BANK(128, 15000); // Vault Bank - 128 slots, 15000 prosperity
        
        private final int storageSlots;
        private final int requiredProsperity;
        
        BankType(int storageSlots, int requiredProsperity) {
            this.storageSlots = storageSlots;
            this.requiredProsperity = requiredProsperity;
        }
        
        public int getStorageSlots() {
            return storageSlots;
        }
        
        public int getRequiredProsperity() {
            return requiredProsperity;
        }
    }

    public ConstructionManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public boolean startProject(ConstructionProject project) {
        // Check if this is a bank project and validate prosperity level
        if (project.getBuildingType() == BuildingType.BANK) {
            int villageProsperity = resourceManager.getVillageProsperity();
            BankType bankType = determineBankType(villageProsperity);
            if (bankType == null) {
                return false; // Village not prosperous enough
            }
            project.setBankType(bankType);
        }
        
        if (!resourceManager.hasSufficientResources(project.getRequiredResources())) {
            return false;
        }
        
        if (!resourceManager.consumeResources(project.getRequiredResources())) {
            return false;
        }

        int projectId = projectIdCounter.incrementAndGet();
        project.setId(projectId);
        activeProjects.put(projectId, project);
        
        scheduler.scheduleAtFixedRate(
            () -> updateProjectProgress(projectId, 5),
            0, 1, TimeUnit.SECONDS
        );
        
        return true;
    }

    private void updateProjectProgress(int projectId, int progress) {
        ConstructionProject project = activeProjects.get(projectId);
        if (project != null) {
            project.incrementProgress(progress);
            if (project.isCompleted()) {
                activeProjects.remove(projectId);
                resourceManager.releaseResources(project.getLockedResources());
            }
        }
    }

    public boolean cancelProject(int projectId) {
        ConstructionProject project = activeProjects.remove(projectId);
        if (project != null) {
            resourceManager.releaseResources(project.getConsumedResources());
            return true;
        }
        return false;
    }

    public Map<Integer, ConstructionProject> getActiveProjects() {
        return new ConcurrentHashMap<>(activeProjects);
    }
    
    private BankType determineBankType(int villageProsperity) {
        for (BankType type : BankType.values()) {
            if (villageProsperity >= type.getRequiredProsperity()) {
                return type;
            }
        }
        return null;
    }
    
    public BankType getRecommendedBankType() {
        return determineBankType(resourceManager.getVillageProsperity());
    }
}
