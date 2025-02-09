package com.villagesreborn.beeny.systems;

import java.util.concurrent.*;

public class DefenseCoordinator {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private volatile boolean defenseActive = false;
    private long lastDefenseActivation = 0;
    private static final long COOLDOWN_DURATION = 300_000; // 5 minutes in milliseconds
    private final ConstructionManager constructionManager;
    private final ResourceManager resourceManager;

    public DefenseCoordinator(ConstructionManager constructionManager, ResourceManager resourceManager) {
        this.constructionManager = constructionManager;
        this.resourceManager = resourceManager;
    }

    public void coordinateDefense(ThreatEvent threat) {
        executor.submit(() -> {
            if (!defenseActive && (System.currentTimeMillis() - lastDefenseActivation) > COOLDOWN_DURATION) {
                lastDefenseActivation = System.currentTimeMillis();
                initiateDefense();
            }
            assessThreat(threat);
        });
    }

    private void initiateDefense() {
        defenseActive = true;
        EventBus.post(new DefenseInitiatedEvent(System.currentTimeMillis()));
    }

    private void assessThreat(ThreatEvent threat) {
        if (resourceManager.hasResources(Map.of(ResourceType.WOOD, 100, ResourceType.STONE, 200))) {
            switch (threat.getType()) {
                case RAID:
                    summonIronGolems(threat.getLocation(), 3, Villager.getPopulationCount());
                    createPatrols(threat.getLocation(), 2);
                    constructionManager.startProject(new DefenseConstructionProject(threat));
                    break;
                case MOB_ATTACK:
                    summonIronGolems(threat.getLocation(), 1, Villager.getPopulationCount());
                    createPatrols(threat.getLocation(), 1);
                    break;
                case SUSPICIOUS_ACTIVITY:
                    createPatrols(threat.getLocation(), 1);
                    break;
            }
        }
        EventBus.post(new ThreatAssessmentEvent(threat.getType().toString(), System.currentTimeMillis()));
    }

    private void summonIronGolems(Location location, int threatLevel, int villagePopulation) {
        executor.submit(() -> {
            if (threatLevel >= 2 && villagePopulation >= 4) {
                // Calculate golems based on threat level and population
                int baseGolems = 1 + (threatLevel / 2);
                int maxFromPopulation = (int) Math.ceil(villagePopulation * 0.25);
                int numGolems = Math.min(baseGolems, maxFromPopulation);

                if (resourceManager.hasResources(Map.of(
                        ResourceType.IRON_INGOT, numGolems * 4,
                        ResourceType.REDSTONE, numGolems * 1))) {

                    for (int i = 0; i < numGolems; i++) {
                        Villager.spawnDefender(location); // Assuming Villager.spawnDefender exists
                        resourceManager.consumeResources(Map.of(
                                ResourceType.IRON_INGOT, 4,
                                ResourceType.REDSTONE, 1));
                    }
                    Logger.debug("Spawned " + numGolems + " iron golems at " + location);
                }
            }
        });
    }

    private void createPatrols(Location location, int numPatrols) {
        TaskSystem taskSystem = ServiceLocator.getService(TaskSystem.class);
        for (int i = 0; i < numPatrols; i++) {
            taskSystem.assignTask(new PatrolTask(location, 30));
        }
    }

    public void cancelDefense() {
        defenseActive = false;
        EventBus.post(new DefenseCompletedEvent(System.currentTimeMillis()));
    }

    private static class DefenseConstructionProject extends ConstructionProject {
        public DefenseConstructionProject(ThreatEvent threat) {
            super("DefenseBarrier-" + threat.getThreatType(), 
                  Map.of(ResourceType.WOOD, 100, ResourceType.STONE, 200),
                  100);
        }
    }
}
