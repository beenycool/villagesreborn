package com.beeny.worldgen;

import com.beeny.Villagesreborn;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.structure.StructureSet;

public class VillagesRebornStructures {
    
    // Updated to use the correct class name for 1.21.4
    public static final RegistryKey<StructureSet> ROMAN_VILLAGE = 
        RegistryKey.of(RegistryKeys.STRUCTURE_SET, Identifier.of(Villagesreborn.MOD_ID, "roman_village"));
    
    // Structure keys
    public static final RegistryKey<net.minecraft.world.gen.structure.Structure> ROMAN_FORUM = 
        RegistryKey.of(RegistryKeys.STRUCTURE, Identifier.of(Villagesreborn.MOD_ID, "roman/forum"));
    
    public static void registerStructures() {
        Villagesreborn.LOGGER.info("Registering Villages Reborn structures");
        
        // Add Roman structures to appropriate biomes using BiomeModifications
        BiomeModifications.addStructure(
            BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.MEADOW, BiomeKeys.SUNFLOWER_PLAINS),
            ROMAN_VILLAGE
        );
    }
}