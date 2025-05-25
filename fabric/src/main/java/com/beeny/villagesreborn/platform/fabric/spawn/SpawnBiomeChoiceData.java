package com.beeny.villagesreborn.platform.fabric.spawn;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;

/**
 * Data class representing a player's spawn biome choice
 */
public class SpawnBiomeChoiceData {
    private final RegistryKey<Biome> biomeKey;
    private final long selectionTimestamp;
    
    public SpawnBiomeChoiceData(RegistryKey<Biome> biomeKey, long selectionTimestamp) {
        if (biomeKey == null) {
            throw new IllegalArgumentException("Biome key cannot be null");
        }
        if (selectionTimestamp < 0) {
            throw new IllegalArgumentException("Selection timestamp cannot be negative");
        }
        
        this.biomeKey = biomeKey;
        this.selectionTimestamp = selectionTimestamp;
    }
    
    public RegistryKey<Biome> getBiomeKey() {
        return biomeKey;
    }
    
    public long getSelectionTimestamp() {
        return selectionTimestamp;
    }
    
    @Override
    public String toString() {
        return "SpawnBiomeChoiceData{" +
                "biomeKey=" + biomeKey.getValue() +
                ", selectionTimestamp=" + selectionTimestamp +
                '}';
    }
}