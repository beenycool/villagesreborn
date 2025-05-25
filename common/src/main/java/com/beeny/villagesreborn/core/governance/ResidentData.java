package com.beeny.villagesreborn.core.governance;

import java.util.UUID;

/**
 * Data for a village resident including reputation and personal information
 */
public class ResidentData {
    private final UUID id;
    private String name;
    private int reputation;
    private int age;
    private int lastActivityRound;
    
    public ResidentData(UUID id) {
        this.id = id;
        this.reputation = 50; // default reputation
        this.age = 25; // default age
        this.lastActivityRound = 0;
    }
    
    public UUID getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getReputation() {
        return reputation;
    }
    
    public void setReputation(int reputation) {
        this.reputation = Math.max(0, Math.min(100, reputation)); // clamp between 0-100
    }
    
    public int getAge() {
        return age;
    }
    
    public void setAge(int age) {
        this.age = age;
    }
    
    public int getLastActivityRound() {
        return lastActivityRound;
    }
    
    public void setLastActivityRound(int lastActivityRound) {
        this.lastActivityRound = lastActivityRound;
    }
}