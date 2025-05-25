package com.beeny.villagesreborn.core.expansion;

import com.beeny.villagesreborn.core.combat.ThreatLevel;

public class VillageResources {
    private final int population;
    private int wood;
    private int stone;
    private int gold;
    private int food;
    private int guardCount;
    private ThreatLevel threatLevel;
    private int residentSatisfaction;
    
    public VillageResources(int population, int wood, int stone) {
        this.population = population;
        this.wood = wood;
        this.stone = stone;
        this.gold = 0;
        this.food = 0;
        this.guardCount = 0;
        this.threatLevel = ThreatLevel.NONE;
        this.residentSatisfaction = 100;
    }
    
    // Constructor with no parameters for policy tests
    public VillageResources() {
        this(0, 0, 0);
    }
    
    public int getPopulation() {
        return population;
    }
    
    public int getWood() {
        return wood;
    }
    
    public void setWood(int wood) {
        this.wood = wood;
    }
    
    public int getStone() {
        return stone;
    }
    
    public void setStone(int stone) {
        this.stone = stone;
    }
    
    public int getGold() {
        return gold;
    }
    
    public void setGold(int gold) {
        this.gold = gold;
    }
    
    public int getFood() {
        return food;
    }
    
    public void setFood(int food) {
        this.food = food;
    }
    
    public int getGuardCount() {
        return guardCount;
    }
    
    public void setGuardCount(int guardCount) {
        this.guardCount = guardCount;
    }
    
    public ThreatLevel getThreatLevel() {
        return threatLevel;
    }
    
    public void setThreatLevel(ThreatLevel threatLevel) {
        this.threatLevel = threatLevel;
    }
    
    public int getResidentSatisfaction() {
        return residentSatisfaction;
    }
    
    public void setResidentSatisfaction(int residentSatisfaction) {
        this.residentSatisfaction = Math.max(0, Math.min(100, residentSatisfaction));
    }
}