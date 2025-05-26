package com.beeny.villagesreborn.platform.fabric.spawn;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;

/**
 * Data class representing a player's spawn biome choice
 * NOTE: Static field currentSpawnBiomeChoice has been completely removed
 * Use SpawnBiomeStorage implementations instead
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
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        SpawnBiomeChoiceData that = (SpawnBiomeChoiceData) obj;
        return selectionTimestamp == that.selectionTimestamp &&
               biomeKey.equals(that.biomeKey);
    }
    
    @Override
    public int hashCode() {
        return biomeKey.hashCode() * 31 + Long.hashCode(selectionTimestamp);
    }
    
    @Override
    public String toString() {
        return "SpawnBiomeChoiceData{" +
                "biomeKey=" + biomeKey.getValue() +
                ", selectionTimestamp=" + selectionTimestamp +
                '}';
    }
}