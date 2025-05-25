package com.beeny.villagesreborn.core.governance;

import com.beeny.villagesreborn.core.expansion.VillageResources;
import com.beeny.villagesreborn.core.combat.ThreatLevel;

/**
 * Defense policy implementation for village governance
 */
public class DefensePolicy {
    private final int defenseBudget;
    
    public DefensePolicy(int defenseBudget) {
        if (defenseBudget < 0) {
            throw new PolicyValidationException("Defense budget cannot be negative");
        }
        this.defenseBudget = defenseBudget;
    }
    
    public PolicyResult apply(VillageResources villageResources) {
        int currentGold = villageResources.getGold();
        
        if (currentGold < defenseBudget) {
            return new PolicyResult(false, "Insufficient funds for defense policy");
        }
        
        // Deduct budget from gold
        villageResources.setGold(currentGold - defenseBudget);
        
        // Increase guard count based on budget
        int guardIncrease = defenseBudget / 50; // 1 guard per 50 gold
        int currentGuards = villageResources.getGuardCount();
        villageResources.setGuardCount(currentGuards + guardIncrease);
        
        // Reduce threat level based on budget
        ThreatLevel currentThreat = villageResources.getThreatLevel();
        if (defenseBudget >= 400) {
            villageResources.setThreatLevel(ThreatLevel.LOW);
        } else if (defenseBudget >= 200 && currentThreat.ordinal() > ThreatLevel.LOW.ordinal()) {
            ThreatLevel[] levels = ThreatLevel.values();
            int newThreatIndex = Math.max(0, currentThreat.ordinal() - 1);
            villageResources.setThreatLevel(levels[newThreatIndex]);
        }
        
        return new PolicyResult(true, "Defense improvements applied successfully");
    }
}