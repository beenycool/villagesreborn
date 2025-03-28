package com.beeny.village;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;
import java.util.UUID;

/**
 * Custom implementation of a villager for Villages Reborn mod.
 * Extends the functionality of vanilla villagers with additional properties.
 */
public class Villager {
    private final VillagerEntity entity;
    private final UUID id;
    private String name;
    private int happiness = 50;
    private int influence = 0;
    private boolean isLeader = false;
    private boolean isChild = false;
    private BlockPos home = null;
    private String profession = "none";
    private String personality = "neutral";

    public Villager(VillagerEntity entity) {
        this.entity = entity;
        this.id = entity.getUuid();
        this.name = entity.getName().getString();
        this.isChild = entity.isBaby();
        
        // Initialize with default values that can be changed later
        this.happiness = 50 + (int)(Math.random() * 30) - 15; // Random initial happiness
        this.influence = (int)(Math.random() * 20); // Random initial influence
    }

    /**
     * Gets the unique identifier for this villager.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the name of this villager.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this villager.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the happiness level of this villager (0-100).
     */
    public int getHappiness() {
        return happiness;
    }

    /**
     * Sets the happiness level of this villager.
     */
    public void setHappiness(int happiness) {
        this.happiness = Math.max(0, Math.min(100, happiness));
    }

    /**
     * Gets the influence level of this villager (0-100).
     */
    public int getInfluence() {
        return influence;
    }

    /**
     * Sets the influence level of this villager.
     */
    public void setInfluence(int influence) {
        this.influence = Math.max(0, Math.min(100, influence));
    }

    /**
     * Checks if this villager is a leader in their village.
     */
    public boolean isLeader() {
        return isLeader;
    }

    /**
     * Sets whether this villager is a leader.
     */
    public void setLeader(boolean leader) {
        isLeader = leader;
    }

    /**
     * Checks if this villager is a child.
     */
    public boolean isChild() {
        return isChild;
    }

    /**
     * Sets whether this villager is a child.
     */
    public void setChild(boolean child) {
        isChild = child;
    }

    /**
     * Gets the home location of this villager.
     */
    public BlockPos getHome() {
        return home;
    }

    /**
     * Sets the home location of this villager.
     */
    public void setHome(BlockPos home) {
        this.home = home;
    }

    /**
     * Gets the profession of this villager.
     */
    public String getProfession() {
        return profession;
    }

    /**
     * Sets the profession of this villager.
     */
    public void setProfession(String profession) {
        this.profession = profession;
    }

    /**
     * Gets the personality of this villager.
     */
    public String getPersonality() {
        return personality;
    }

    /**
     * Sets the personality of this villager.
     */
    public void setPersonality(String personality) {
        this.personality = personality;
    }

    /**
     * Gets the underlying vanilla villager entity.
     */
    public VillagerEntity getEntity() {
        return entity;
    }
}