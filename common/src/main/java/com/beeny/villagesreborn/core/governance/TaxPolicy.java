package com.beeny.villagesreborn.core.governance;

import com.beeny.villagesreborn.core.expansion.VillageResources;

/**
 * Tax policy implementation for village governance
 */
public class TaxPolicy {
    private final double taxRate;
    
    public TaxPolicy(double taxRate) {
        if (taxRate < 0.0 || taxRate > 1.0) {
            throw new PolicyValidationException("Tax rate must be between 0.0 and 1.0");
        }
        this.taxRate = taxRate;
    }
    
    public PolicyResult apply(VillageResources villageResources) {
        int currentGold = villageResources.getGold();
        int taxCollected = (int) (currentGold * taxRate);
        
        villageResources.setGold(currentGold + taxCollected);
        
        // High tax rates reduce satisfaction
        if (taxRate > 0.3) {
            int currentSatisfaction = villageResources.getResidentSatisfaction();
            int satisfactionPenalty = (int) ((taxRate - 0.3) * 50);
            villageResources.setResidentSatisfaction(currentSatisfaction - satisfactionPenalty);
        }
        
        return new PolicyResult(true, "Tax collection successful: " + taxCollected + " gold collected");
    }
}