package com.beeny.worldgen;

import com.beeny.Villagesreborn;
import com.beeny.config.ModConfig;        // Use new config
import com.beeny.config.ModConfigManager; // Use new config manager

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureSet;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.TagKey;

// Removed unused List import
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
    // --- Structure Set Keys for other cultures (assuming similar LOW/MEDIUM/HIGH variants) ---
    // Egyptian
    public static final RegistryKey<StructureSet> EGYPTIAN_VILLAGE_LOW_SET =
        RegistryKey.of(RegistryKeys.STRUCTURE_SET, Identifier.of(Villagesreborn.MOD_ID, "egyptian_village_low"));
    public static final RegistryKey<StructureSet> EGYPTIAN_VILLAGE_MEDIUM_SET =
        RegistryKey.of(RegistryKeys.STRUCTURE_SET, Identifier.of(Villagesreborn.MOD_ID, "egyptian_village")); // Assuming medium is default name
    public static final RegistryKey<StructureSet> EGYPTIAN_VILLAGE_HIGH_SET =
        RegistryKey.of(RegistryKeys.STRUCTURE_SET, Identifier.of(Villagesreborn.MOD_ID, "egyptian_village_high"));

    // Victorian
    public static final RegistryKey<StructureSet> VICTORIAN_VILLAGE_LOW_SET =
        RegistryKey.of(RegistryKeys.STRUCTURE_SET, Identifier.of(Villagesreborn.MOD_ID, "victorian_village_low"));
    public static final RegistryKey<StructureSet> VICTORIAN_VILLAGE_MEDIUM_SET =
        RegistryKey.of(RegistryKeys.STRUCTURE_SET, Identifier.of(Villagesreborn.MOD_ID, "victorian_village"));
    public static final RegistryKey<StructureSet> VICTORIAN_VILLAGE_HIGH_SET =
        RegistryKey.of(RegistryKeys.STRUCTURE_SET, Identifier.of(Villagesreborn.MOD_ID, "victorian_village_high"));

    // NYC
    public static final RegistryKey<StructureSet> NYC_VILLAGE_LOW_SET =
        RegistryKey.of(RegistryKeys.STRUCTURE_SET, Identifier.of(Villagesreborn.MOD_ID, "nyc_village_low"));
    public static final RegistryKey<StructureSet> NYC_VILLAGE_MEDIUM_SET =
        RegistryKey.of(RegistryKeys.STRUCTURE_SET, Identifier.of(Villagesreborn.MOD_ID, "nyc_village"));
    public static final RegistryKey<StructureSet> NYC_VILLAGE_HIGH_SET =
        RegistryKey.of(RegistryKeys.STRUCTURE_SET, Identifier.of(Villagesreborn.MOD_ID, "nyc_village_high"));


    public static void registerStructures() {
        Villagesreborn.LOGGER.info("Registering Villages Reborn structures and adding to biomes based on ModConfig...");

        ModConfig config = ModConfigManager.getConfig();
        double spawnRateModifier = config.general.spawnRateModifier;
        ModConfig.CultureToggles cultureToggles = config.general.cultureToggles;

        // Determine spawn rate category based on modifier
        String spawnCategory;
        RegistryKey<StructureSet> romanSetKey;
        RegistryKey<StructureSet> egyptianSetKey;
        RegistryKey<StructureSet> victorianSetKey;
        RegistryKey<StructureSet> nycSetKey;

        if (spawnRateModifier < 0.7) {
            spawnCategory = "LOW";
            romanSetKey = ROMAN_VILLAGE_LOW_SET;
            egyptianSetKey = EGYPTIAN_VILLAGE_LOW_SET;
            victorianSetKey = VICTORIAN_VILLAGE_LOW_SET;
            nycSetKey = NYC_VILLAGE_LOW_SET;
        } else if (spawnRateModifier > 1.5) {
            spawnCategory = "HIGH";
            romanSetKey = ROMAN_VILLAGE_HIGH_SET;
            egyptianSetKey = EGYPTIAN_VILLAGE_HIGH_SET;
            victorianSetKey = VICTORIAN_VILLAGE_HIGH_SET;
            nycSetKey = NYC_VILLAGE_HIGH_SET;
        } else {
            spawnCategory = "MEDIUM";
            romanSetKey = ROMAN_VILLAGE_MEDIUM_SET;
            egyptianSetKey = EGYPTIAN_VILLAGE_MEDIUM_SET;
            victorianSetKey = VICTORIAN_VILLAGE_MEDIUM_SET;
            nycSetKey = NYC_VILLAGE_MEDIUM_SET;
        }
        Villagesreborn.LOGGER.info("Effective Spawn Rate Category: {} (Modifier: {})", spawnCategory, spawnRateModifier);


        // --- Roman Culture ---
        if (cultureToggles.enableRoman) {
             Villagesreborn.LOGGER.info("Roman culture enabled ({} spawn rate). Adding structure set to biomes.", spawnCategory);
            BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.SAVANNA), // Select biomes
                GenerationStep.Feature.SURFACE_STRUCTURES, // Generation step
                romanSetKey // Use the selected key based on spawn rate modifier
            );
        } else {
            Villagesreborn.LOGGER.info("Roman culture disabled in config.");
        }

        // --- Egyptian Culture ---
        if (cultureToggles.enableEgyptian) {
             Villagesreborn.LOGGER.info("Egyptian culture enabled ({} spawn rate). Adding structure set to biomes.", spawnCategory);
            BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(BiomeKeys.DESERT), // Example: Add to deserts
                GenerationStep.Feature.SURFACE_STRUCTURES,
                egyptianSetKey // Use the selected key
            );
        } else {
            Villagesreborn.LOGGER.info("Egyptian culture disabled in config.");
        }

        // --- Victorian Culture ---
        if (cultureToggles.enableVictorian) {
             Villagesreborn.LOGGER.info("Victorian culture enabled ({} spawn rate). Adding structure set to biomes.", spawnCategory);
            BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(BiomeKeys.TAIGA, BiomeKeys.OLD_GROWTH_PINE_TAIGA, BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA), // Example: Add to taiga variants
                GenerationStep.Feature.SURFACE_STRUCTURES,
                victorianSetKey // Use the selected key
            );
        } else {
            Villagesreborn.LOGGER.info("Victorian culture disabled in config.");
        }

        // --- NYC Culture ---
        if (cultureToggles.enableNyc) {
             Villagesreborn.LOGGER.info("NYC culture enabled ({} spawn rate). Adding structure set to biomes.", spawnCategory);
            // Note: NYC might fit better in plains/forests or have custom biome tags
            BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.FOREST), // Example: Add to plains/forests
                GenerationStep.Feature.SURFACE_STRUCTURES,
                nycSetKey // Use the selected key
            );
        } else {
            Villagesreborn.LOGGER.info("NYC culture disabled in config.");
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