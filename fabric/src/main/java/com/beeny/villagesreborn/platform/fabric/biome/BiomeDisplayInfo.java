package com.beeny.villagesreborn.platform.fabric.biome;

import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.world.biome.Biome;

/**
 * Data class containing display information for a biome in the selector
 */
public class BiomeDisplayInfo {
    private final RegistryKey<Biome> registryKey;
    private final Text displayName;
    private final float temperature;
    private final float humidity;
    private final int difficultyRating;
    private final Text description;

    public BiomeDisplayInfo(RegistryKey<Biome> registryKey, Text displayName, 
                           float temperature, float humidity, int difficultyRating, 
                           Text description) {
        this.registryKey = registryKey;
        this.displayName = displayName;
        this.temperature = temperature;
        this.humidity = humidity;
        this.difficultyRating = difficultyRating;
        this.description = description;
    }

    public RegistryKey<Biome> getRegistryKey() {
        return registryKey;
    }

    public Text getDisplayName() {
        return displayName;
    }

    public float getTemperature() {
        return temperature;
    }

    public float getHumidity() {
        return humidity;
    }

    public int getDifficultyRating() {
        return difficultyRating;
    }

    public Text getDescription() {
        return description;
    }
}