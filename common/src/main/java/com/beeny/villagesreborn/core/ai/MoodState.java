package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.common.NBTCompound;

/**
 * Minimal MoodState implementation for TDD
 */
public class MoodState {
    private float happiness = 0.5f;
    private float energy = 0.5f;
    private float social = 0.5f;
    private long lastUpdated = System.currentTimeMillis();

    public MoodState() {}

    public float getHappiness() { return happiness; }
    public float getEnergy() { return energy; }
    public float getSocial() { return social; }
    public long getLastUpdated() { return lastUpdated; }

    public void setHappiness(float happiness) { 
        this.happiness = Math.max(0.0f, Math.min(1.0f, happiness));
        this.lastUpdated = System.currentTimeMillis();
    }

    public void setEnergy(float energy) { 
        this.energy = Math.max(0.0f, Math.min(1.0f, energy));
        this.lastUpdated = System.currentTimeMillis();
    }

    public void setSocial(float social) { 
        this.social = Math.max(0.0f, Math.min(1.0f, social));
        this.lastUpdated = System.currentTimeMillis();
    }

    public void setLastUpdated(long time) {
        this.lastUpdated = time;
    }

    public MoodCategory getOverallMood() {
        float average = (happiness + energy + social) / 3.0f;
        if (average > 0.8f) return MoodCategory.HAPPY;
        if (average > 0.6f) return MoodCategory.CONTENT;
        if (average > 0.4f) return MoodCategory.NEUTRAL;
        if (average > 0.2f) return MoodCategory.SAD;
        return MoodCategory.ANGRY;
    }

    public void decay(long timeElapsed) {
        // Simple decay toward neutral (0.5)
        float decayFactor = timeElapsed / 3600000.0f; // 1 hour = full decay
        happiness += (0.5f - happiness) * decayFactor * 0.1f;
        energy += (0.5f - energy) * decayFactor * 0.1f;
        social += (0.5f - social) * decayFactor * 0.1f;
        lastUpdated = System.currentTimeMillis();
    }

    public MoodState copy() {
        MoodState copy = new MoodState();
        copy.happiness = this.happiness;
        copy.energy = this.energy;
        copy.social = this.social;
        copy.lastUpdated = this.lastUpdated;
        return copy;
    }

    public NBTCompound toNBT() {
        NBTCompound nbt = new NBTCompound();
        nbt.putFloat("happiness", happiness);
        nbt.putFloat("energy", energy);
        nbt.putFloat("social", social);
        nbt.putLong("lastUpdated", lastUpdated);
        return nbt;
    }

    public static MoodState fromNBT(NBTCompound nbt) {
        MoodState mood = new MoodState();
        mood.happiness = nbt.getFloat("happiness");
        mood.energy = nbt.getFloat("energy");
        mood.social = nbt.getFloat("social");
        mood.lastUpdated = nbt.getLong("lastUpdated");
        return mood;
    }
}