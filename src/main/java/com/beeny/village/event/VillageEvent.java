package com.beeny.village.event;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.UUID;
import java.util.List;

public class VillageEvent {
    private final String type;
    private final String description;
    private final BlockPos location;
    private final long duration;
    private final long startTime;
    private final List<UUID> participants;
    private final String culture;
    private final String outcome;

    private VillageEvent(Builder builder) {
        this.type = builder.type;
        this.description = builder.description;
        this.location = builder.location;
        this.duration = builder.duration;
        this.startTime = builder.startTime;
        this.participants = builder.participants;
        this.culture = builder.culture;
        this.outcome = builder.outcome;
    }

    public static class Builder {
        private String type;
        private String description;
        private BlockPos location;
        private long duration;
        private long startTime;
        private List<UUID> participants;
        private String culture;
        private String outcome;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder location(BlockPos location) {
            this.location = location;
            return this;
        }

        public Builder duration(long duration) {
            this.duration = duration;
            return this;
        }

        public Builder startTime(long startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder participants(List<UUID> participants) {
            this.participants = participants;
            return this;
        }

        public Builder culture(String culture) {
            this.culture = culture;
            return this;
        }

        public Builder outcome(String outcome) {
            this.outcome = outcome;
            return this;
        }

        public VillageEvent build() {
            return new VillageEvent(this);
        }
    }

    public String getType() { return type; }
    public String getDescription() { return description; }
    public BlockPos getLocation() { return location; }
    public long getDuration() { return duration; }
    public long getStartTime() { return startTime; }
    public List<UUID> getParticipants() { return participants; }
    public String getCulture() { return culture; }
    public String getOutcome() { return outcome; }

    public boolean isActive(long currentTime) {
        return currentTime >= startTime && currentTime < startTime + duration;
    }
}