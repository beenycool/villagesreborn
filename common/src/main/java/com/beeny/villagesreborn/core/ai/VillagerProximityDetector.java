package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.common.BlockPos;
import com.beeny.villagesreborn.core.common.VillagerEntity;

import java.util.List;

/**
 * Service for detecting villagers within proximity of a given location
 */
public interface VillagerProximityDetector {
    
    /**
     * Finds all villagers within the specified radius of a location
     * @param location the center position
     * @param radius the search radius in blocks
     * @return list of villagers within range
     */
    List<VillagerEntity> findNearbyVillagers(BlockPos location, int radius);
}