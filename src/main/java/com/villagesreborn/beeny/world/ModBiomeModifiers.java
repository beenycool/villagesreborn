package com.villagesreborn.beeny.world;

import com.villagesreborn.beeny.entities.HorseRidingTrader;
import com.villagesreborn.beeny.Villagesreborn;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnSettings;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModBiomeModifiers {
    public static void registerBiomeModifiers() {
        BiomeModifications.addSpawn(
            BiomeSelectors.categories(biome -> true), // Apply to all biomes for now, can be refined
            SpawnGroup.CREATURE, // Or MISC? needs checking
            new SpawnSettings.SpawnEntry(Registry.ENTITY_TYPE.get(new Identifier("villagesreborn", "horse_riding_trader")), 1, 1, 1)
        );
    }
}
