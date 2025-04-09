package com.beeny.mixin.levelgen;

import com.beeny.config.VillagesConfig;
import com.beeny.Villagesreborn; // Assuming logger is accessible here or manage logging differently
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomSpreadStructurePlacement.class)
public abstract class RandomSpreadStructurePlacementMixin extends StructurePlacement {

    // Shadow the fields we want to modify
    @Shadow @Mutable private int spacing;
    @Shadow @Mutable private int separation;

    // Inject code at the end of the constructor
    @Inject(method = "<init>", at = @At("RETURN"))
    private void villagesreborn_modifySpacingAndSeparation(CallbackInfo ci) {
        // TODO: Ideally, identify if this specific placement is for villages.
        // This current implementation modifies *all* RandomSpread placements.
        // This might be acceptable if villages are the primary user, otherwise,
        // refinement is needed (e.g., check StructureSet tags/registry name).

        String spawnRateSetting = VillagesConfig.getInstance().getVillageSpawnRate();
        int newSpacing;
        int newSeparation;

        switch (spawnRateSetting.toUpperCase()) {
            case "LOW":
                newSpacing = 48;
                newSeparation = 12;
                break;
            case "HIGH":
                newSpacing = 24;
                newSeparation = 6;
                break;
            case "MEDIUM":
            default: // Default to MEDIUM
                newSpacing = 32;
                newSeparation = 8;
                break;
        }

        // Apply the calculated values from config
        this.spacing = newSpacing;
        this.separation = newSeparation;

        // Log the modification (adjust logger access as needed)
        Villagesreborn.LOGGER.debug("Applied custom village spawn settings: Spacing={}, Separation={}", newSpacing, newSeparation);
    }

    // Required constructor for extending StructurePlacement
    protected RandomSpreadStructurePlacementMixin(net.minecraft.core.Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod, float frequency, int salt, java.util.Optional<ExclusionZone> exclusionZone) {
        super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone);
    }
}