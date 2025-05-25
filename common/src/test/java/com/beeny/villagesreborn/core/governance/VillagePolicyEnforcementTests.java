package com.beeny.villagesreborn.core.governance;

import com.beeny.villagesreborn.core.expansion.VillageResources;
import com.beeny.villagesreborn.core.combat.ThreatLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for village policy enforcement - tax policies and defense policies
 */
@ExtendWith(MockitoExtension.class)
public class VillagePolicyEnforcementTests {

    private VillageResources villageResources;
    private TaxPolicy taxPolicy;
    private DefensePolicy defensePolicy;
    
    @BeforeEach
    void setUp() {
        villageResources = new VillageResources();
        villageResources.setGold(1000);
        villageResources.setFood(500);
        villageResources.setWood(800);
        villageResources.setStone(300);
        villageResources.setGuardCount(5);
        villageResources.setThreatLevel(ThreatLevel.MODERATE);
    }
    
    @Test
    void taxPolicy_WithValidRate_ShouldUpdateResourcePoolsCorrectly() {
        // Given
        taxPolicy = new TaxPolicy(0.15); // 15% tax rate
        int initialGold = villageResources.getGold();
        
        // When
        PolicyResult result = taxPolicy.apply(villageResources);
        
        // Then
        assertTrue(result.isSuccessful());
        assertEquals(initialGold + 150, villageResources.getGold()); // 15% of 1000 = 150
        assertEquals("Tax collection successful: 150 gold collected", result.getMessage());
    }
    
    @Test
    void taxPolicy_WithHighRate_ShouldReduceResidentSatisfaction() {
        // Given
        taxPolicy = new TaxPolicy(0.40); // 40% tax rate - very high
        villageResources.setResidentSatisfaction(80);
        
        // When
        PolicyResult result = taxPolicy.apply(villageResources);
        
        // Then
        assertTrue(result.isSuccessful());
        assertTrue(villageResources.getResidentSatisfaction() < 80);
        assertEquals(400, villageResources.getGold() - 1000); // 40% of 1000
    }
    
    @Test
    void taxPolicy_WithNegativeRate_ShouldThrowValidationException() {
        // Given & When & Then
        assertThrows(PolicyValidationException.class, () -> {
            new TaxPolicy(-0.1);
        });
    }
    
    @Test
    void taxPolicy_WithExcessiveRate_ShouldThrowValidationException() {
        // Given & When & Then
        assertThrows(PolicyValidationException.class, () -> {
            new TaxPolicy(1.1); // 110% tax rate
        });
    }
    
    @Test
    void defensePolicy_WithValidBudget_ShouldIncreaseGuardCountAndReduceThreat() {
        // Given
        defensePolicy = new DefensePolicy(200); // spend 200 gold on defense
        int initialGuards = villageResources.getGuardCount();
        ThreatLevel initialThreat = villageResources.getThreatLevel();
        
        // When
        PolicyResult result = defensePolicy.apply(villageResources);
        
        // Then
        assertTrue(result.isSuccessful());
        assertEquals(800, villageResources.getGold()); // 1000 - 200
        assertTrue(villageResources.getGuardCount() > initialGuards);
        assertTrue(villageResources.getThreatLevel().ordinal() < initialThreat.ordinal());
    }
    
    @Test
    void defensePolicy_WithLargeBudget_ShouldMaximizeDefenseImprovements() {
        // Given
        defensePolicy = new DefensePolicy(500); // large defense budget
        villageResources.setThreatLevel(ThreatLevel.HIGH);
        
        // When
        PolicyResult result = defensePolicy.apply(villageResources);
        
        // Then
        assertTrue(result.isSuccessful());
        assertEquals(500, villageResources.getGold()); // 1000 - 500
        assertTrue(villageResources.getGuardCount() >= 8); // significant guard increase
        assertEquals(ThreatLevel.LOW, villageResources.getThreatLevel());
    }
    
    @Test
    void defensePolicy_WithInsufficientFunds_ShouldReturnFailureResult() {
        // Given
        villageResources.setGold(50); // not enough for defense spending
        defensePolicy = new DefensePolicy(200);
        
        // When
        PolicyResult result = defensePolicy.apply(villageResources);
        
        // Then
        assertFalse(result.isSuccessful());
        assertEquals(50, villageResources.getGold()); // unchanged
        assertEquals("Insufficient funds for defense policy", result.getMessage());
    }
    
    @Test
    void defensePolicy_WithNegativeBudget_ShouldThrowValidationException() {
        // Given & When & Then
        assertThrows(PolicyValidationException.class, () -> {
            new DefensePolicy(-100);
        });
    }
    
    @Test
    void policyChain_TaxThenDefense_ShouldApplySequentially() {
        // Given
        taxPolicy = new TaxPolicy(0.20); // 20% tax
        defensePolicy = new DefensePolicy(300); // spend 300 on defense
        
        // When
        PolicyResult taxResult = taxPolicy.apply(villageResources);
        PolicyResult defenseResult = defensePolicy.apply(villageResources);
        
        // Then
        assertTrue(taxResult.isSuccessful());
        assertTrue(defenseResult.isSuccessful());
        assertEquals(900, villageResources.getGold()); // 1000 + 200 - 300
        assertTrue(villageResources.getGuardCount() > 5);
    }
    
    @Test
    void taxPolicy_WithZeroResources_ShouldReturnZeroCollection() {
        // Given
        villageResources.setGold(0);
        taxPolicy = new TaxPolicy(0.15);
        
        // When
        PolicyResult result = taxPolicy.apply(villageResources);
        
        // Then
        assertTrue(result.isSuccessful());
        assertEquals(0, villageResources.getGold());
        assertEquals("Tax collection successful: 0 gold collected", result.getMessage());
    }
    
    @Test
    void defensePolicy_AtMaximumSecurity_ShouldStillAcceptFunding() {
        // Given
        villageResources.setThreatLevel(ThreatLevel.NONE);
        villageResources.setGuardCount(20); // already high guard count
        defensePolicy = new DefensePolicy(100);
        
        // When
        PolicyResult result = defensePolicy.apply(villageResources);
        
        // Then
        assertTrue(result.isSuccessful());
        assertEquals(900, villageResources.getGold());
        // Guards might not increase much more, but policy still applies
        assertTrue(villageResources.getGuardCount() >= 20);
    }
}