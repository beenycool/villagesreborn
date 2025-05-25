package com.beeny.villagesreborn.core.combat;

import java.util.List;

/**
 * Represents a combat situation with contextual information
 */
public class CombatSituation {
    
    private final List<LivingEntity> enemies;
    private final List<LivingEntity> allies;
    private final ThreatLevel overallThreatLevel;
    private final boolean isDefensive;
    
    public CombatSituation(List<LivingEntity> enemies, List<LivingEntity> allies, 
                          ThreatLevel overallThreatLevel, boolean isDefensive) {
        this.enemies = enemies;
        this.allies = allies;
        this.overallThreatLevel = overallThreatLevel;
        this.isDefensive = isDefensive;
    }
    
    public List<LivingEntity> getEnemies() {
        return enemies;
    }
    
    public List<LivingEntity> getAllies() {
        return allies;
    }
    
    public ThreatLevel getOverallThreatLevel() {
        return overallThreatLevel;
    }
    
    public boolean isDefensive() {
        return isDefensive;
    }
    
    public int getEnemyCount() {
        return enemies.size();
    }
    
    public int getAllyCount() {
        return allies.size();
    }
}