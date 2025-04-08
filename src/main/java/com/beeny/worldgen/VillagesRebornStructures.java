package com.beeny.worldgen;

import com.beeny.Villagesreborn;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.TagKey;

public class VillagesRebornStructures {
    
    public static final TagKey<Structure> ROMAN_VILLAGE =
        TagKey.of(RegistryKeys.STRUCTURE, Identifier.of(Villagesreborn.MOD_ID, "roman_village"));
    
    public static final RegistryKey<Structure> ROMAN_FORUM = 
        RegistryKey.of(RegistryKeys.STRUCTURE, Identifier.of(Villagesreborn.MOD_ID, "roman/forum"));
    
    public static void registerStructures() {
        Villagesreborn.LOGGER.info("Registering Villages Reborn structures");
        
    }
}