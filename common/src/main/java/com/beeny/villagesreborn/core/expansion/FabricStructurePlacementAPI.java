package com.beeny.villagesreborn.core.expansion;

import com.beeny.villagesreborn.core.common.BlockPos;

public interface FabricStructurePlacementAPI {
    boolean place(StructureTemplate template, BlockPos position, PlacementSettings settings, World world);
}