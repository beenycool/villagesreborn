package com.beeny.worldgen;

import com.beeny.Villagesreborn;
import com.beeny.config.VillagesConfig; // Added import

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureSet; // Added import
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.TagKey;

import java.util.List; // Added import
import net.minecraft.registry.tag.TagKey;

public class VillagesRebornStructures {
    
    public static final TagKey<Structure> ROMAN_VILLAGE =
        TagKey.of(RegistryKeys.STRUCTURE, Identifier.of(Villagesreborn.MOD_ID, "roman_village"));
    
    public static final RegistryKey<Structure> ROMAN_FORUM =
        RegistryKey.of(RegistryKeys.STRUCTURE, Identifier.of(Villagesreborn.MOD_ID, "roman/forum"));

    // Keys for the Structure Sets defined in JSON
    public static final RegistryKey<StructureSet> ROMAN_VILLAGE_LOW_SET =
        RegistryKey.of(RegistryKeys.STRUCTURE_SET, Identifier.of(Villagesreborn.MOD_ID, "roman_village_low"));
    public static final RegistryKey<StructureSet> ROMAN_VILLAGE_MEDIUM_SET = // Renamed from ROMAN_VILLAGE_SET for clarity
        RegistryKey.of(RegistryKeys.STRUCTURE_SET, Identifier.of(Villagesreborn.MOD_ID, "roman_village"));
    public static final RegistryKey<StructureSet> ROMAN_VILLAGE_HIGH_SET =
        RegistryKey.of(RegistryKeys.STRUCTURE_SET, Identifier.of(Villagesreborn.MOD_ID, "roman_village_high"));
    public static void registerStructures() {
        Villagesreborn.LOGGER.info("Registering Villages Reborn structures and adding to biomes based on config...");

        VillagesConfig config = VillagesConfig.getInstance();
        List<String> enabledCultures = config.getEnabledCultures();
        String spawnRate = config.getVillageSpawnRate(); // Get spawn rate setting

        // --- Roman Culture ---
        if (enabledCultures.stream().anyMatch(s -> s.equalsIgnoreCase("ROMAN"))) {
            RegistryKey<StructureSet> romanSetToRegister;

            // Select the structure set based on the config setting
            switch (spawnRate.toUpperCase()) {
                case "LOW":
                    romanSetToRegister = ROMAN_VILLAGE_LOW_SET;
                    Villagesreborn.LOGGER.info("Roman culture enabled (LOW spawn rate).");
                    break;
                case "HIGH":
                    romanSetToRegister = ROMAN_VILLAGE_HIGH_SET;
                    Villagesreborn.LOGGER.info("Roman culture enabled (HIGH spawn rate).");
                    break;
                case "MEDIUM":
                default: // Default to MEDIUM if value is unexpected
                    romanSetToRegister = ROMAN_VILLAGE_MEDIUM_SET;
                    Villagesreborn.LOGGER.info("Roman culture enabled (MEDIUM spawn rate).");
                    break;
            }

            // Add the selected Roman village structure set to appropriate biomes
            BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.SAVANNA), // Select biomes
                GenerationStep.Feature.SURFACE_STRUCTURES, // Generation step
                romanSetToRegister // Use the selected key
            );
        } else {
            Villagesreborn.LOGGER.info("Roman culture disabled in config.");
        }

        // --- Add similar checks for other cultures (Egyptian, Victorian, NYC, etc.) here ---
        // Example for Egyptian (assuming a structure set key exists):
        /*
        RegistryKey<StructureSet> EGYPTIAN_VILLAGE_SET = RegistryKey.of(RegistryKeys.STRUCTURE_SET, Identifier.of(Villagesreborn.MOD_ID, "egyptian_village"));
        if (enabledCultures.stream().anyMatch(s -> s.equalsIgnoreCase("EGYPTIAN"))) {
            Villagesreborn.LOGGER.info("Egyptian culture enabled, adding structure set to biomes.");
            BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(BiomeKeys.DESERT), // Example: Add to deserts
                GenerationStep.Feature.SURFACE_STRUCTURES,
                EGYPTIAN_VILLAGE_SET
            );
        } else {
            Villagesreborn.LOGGER.info("Egyptian culture disabled in config.");
        }
        */

    }
}