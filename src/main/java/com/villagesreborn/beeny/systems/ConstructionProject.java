package com.villagesreborn.beeny.systems;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ConstructionProject {
    private final int id;
    private final String name;
    private final Map<ResourceType, Integer> requiredResources;
    private final long deadline;
    private int progress;
    private final int totalRequiredProgress;

    private ConstructionProject(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.requiredResources = new ConcurrentHashMap<>(builder.requiredResources);
        this.deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(builder.durationMinutes);
        this.totalRequiredProgress = builder.totalRequiredProgress;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Map<ResourceType, Integer> getRequiredResources() { return new ConcurrentHashMap<>(requiredResources); }
    public long getDeadline() { return deadline; }
    public int getProgress() { return progress; }
    public int getTotalRequiredProgress() { return totalRequiredProgress; }

    public void incrementProgress(int amount) {
        progress = Math.min(progress + amount, totalRequiredProgress);
    }

    public boolean isCompleted() {
        return progress >= totalRequiredProgress;
    }

    public static class Builder {
        private final int id;
        private String name = "Unnamed Project";
        private Map<ResourceType, Integer> requiredResources = new ConcurrentHashMap<>();
        private int durationMinutes = 60;
        private int totalRequiredProgress = 100;

        public Builder(int id) {
            this.id = id;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder requiredResources(Map<ResourceType, Integer> resources) {
            this.requiredResources.putAll(resources);
            return this;
        }

        public Builder durationMinutes(int minutes) {
            this.durationMinutes = minutes;
            return this;
        }

        public Builder totalRequiredProgress(int progress) {
            this.totalRequiredProgress = progress;
            return this;
        }

        public ConstructionProject build() {
            return new ConstructionProject(this);
        }
    }
}
