package com.beeny.villagesreborn.core.expansion;

import com.beeny.villagesreborn.core.common.BlockPos;

public class StructurePlacer {
    private final FabricStructurePlacementAPI placementAPI;
    
    public StructurePlacer(FabricStructurePlacementAPI placementAPI) {
        this.placementAPI = placementAPI;
    }
    
    public boolean placeStructure(StructureTemplate template, BlockPos position, PlacementSettings settings) {
        return placementAPI.place(template, position, settings, new World());
    }
    
    public boolean validatePlacementBounds(StructureTemplate template, BlockPos position) {
        BlockPos size = template.getSize();
        // Simple bounds check - avoid positions too close to world edge
        return position.getX() > -10 && position.getZ() > -10 && 
               position.getY() >= 0 && position.getY() + size.getY() <= 256;
    }
}