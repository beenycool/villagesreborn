package com.beeny.villagesreborn.core.expansion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class VillageExpansionManagerTests {
    
    private VillageExpansionManager expansionManager;
    private VillageResources resources;
    private ExpansionConfig config;
    
    @BeforeEach
    void setUp() {
        expansionManager = new VillageExpansionManager();
        resources = new VillageResources(100, 50, 25); // population, wood, stone
        config = new ExpansionConfig(200, 100, 50); // thresholds
    }
    
    @Test
    @DisplayName("Should schedule next expansion at correct interval based on population")
    void testScheduleNextExpansionTickCorrectInterval() {
        // Given: initial population and resources
        int initialPopulation = 100;
        long currentTick = 1000L;
        
        // When: scheduling next expansion
        long nextTick = expansionManager.scheduleNextExpansion(initialPopulation, currentTick);
        
        // Then: next tick should be calculated based on population-dependent interval
        long expectedInterval = expansionManager.calculateExpansionInterval(initialPopulation);
        assertEquals(currentTick + expectedInterval, nextTick);
    }
    
    @Test
    @DisplayName("Should prevent expansion when resources below threshold")
    void testResourceThresholdPreventsExpansion() {
        // Given: resources below threshold
        VillageResources lowResources = new VillageResources(50, 25, 10);
        
        // When: checking if expansion can proceed
        boolean canExpand = expansionManager.canExpand(lowResources, config);
        
        // Then: expansion should be prevented
        assertFalse(canExpand, "Expansion should be prevented when resources are below threshold");
    }
    
    @Test
    @DisplayName("Should enable expansion when resources meet threshold")
    void testResourceThresholdEnablesExpansion() {
        // Given: resources meeting threshold
        VillageResources sufficientResources = new VillageResources(250, 150, 75);
        
        // When: checking if expansion can proceed
        boolean canExpand = expansionManager.canExpand(sufficientResources, config);
        
        // Then: expansion should be enabled
        assertTrue(canExpand, "Expansion should be enabled when resources meet threshold");
    }
    
    @Test
    @DisplayName("Should place buildings sequentially over multiple ticks")
    void testSequentialBuildingPlacement() {
        // Given: expansion manager with building queue
        expansionManager.initializeExpansion(resources, config);
        long tick1 = 1000L;
        long tick2 = 1100L;
        long tick3 = 1200L;
        
        // When: processing multiple expansion ticks
        BuildingPlacement placement1 = expansionManager.processExpansionTick(tick1);
        BuildingPlacement placement2 = expansionManager.processExpansionTick(tick2);
        BuildingPlacement placement3 = expansionManager.processExpansionTick(tick3);
        
        // Then: buildings should be placed sequentially
        assertNotNull(placement1, "First building should be placed");
        assertNotNull(placement2, "Second building should be placed");
        assertNotNull(placement3, "Third building should be placed");
        
        // And: placements should be different buildings
        assertNotEquals(placement1.getBuildingType(), placement2.getBuildingType());
        assertNotEquals(placement2.getBuildingType(), placement3.getBuildingType());
    }
    
    @Test
    @DisplayName("Should calculate expansion interval based on population density")
    void testPopulationBasedExpansionInterval() {
        // Given: different population sizes
        int smallPopulation = 50;
        int mediumPopulation = 150;
        int largePopulation = 300;
        
        // When: calculating intervals
        long smallInterval = expansionManager.calculateExpansionInterval(smallPopulation);
        long mediumInterval = expansionManager.calculateExpansionInterval(mediumPopulation);
        long largeInterval = expansionManager.calculateExpansionInterval(largePopulation);
        
        // Then: larger populations should have shorter intervals
        assertTrue(smallInterval > mediumInterval, "Small population should have longer interval");
        assertTrue(mediumInterval > largeInterval, "Medium population should have longer interval than large");
    }
}