package com.beeny.villagesreborn.platform.fabric.spawn;

import com.beeny.villagesreborn.core.world.VillagesRebornWorldData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extensions to VillagesRebornWorldSettings for spawn biome persistence
 */
public class VillagesRebornWorldSettingsExtensions {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagesRebornWorldSettingsExtensions.class);
    
    // DEPRECATED: Static storage removed in favor of persistent storage
    // This field is kept temporarily for migration purposes only
    private static SpawnBiomeChoiceData currentSpawnBiomeChoice;
    
    /**
     * Sets the spawn biome choice data
     * @deprecated Use SpawnBiomeStorageManager instead
     */
    @Deprecated
    public static void setSpawnBiomeChoice(SpawnBiomeChoiceData choiceData) {
        currentSpawnBiomeChoice = choiceData;
        LOGGER.warn("DEPRECATED: setSpawnBiomeChoice called. Use SpawnBiomeStorageManager instead. Data: {}", choiceData);
    }
    
    /**
     * Gets the current spawn biome choice data
     * @deprecated Use SpawnBiomeStorageManager instead
     */
    @Deprecated
    public static SpawnBiomeChoiceData getSpawnBiomeChoice() {
        if (currentSpawnBiomeChoice != null) {
            LOGGER.warn("DEPRECATED: getSpawnBiomeChoice called. Use SpawnBiomeStorageManager instead. Returning legacy data: {}", currentSpawnBiomeChoice);
        }
        return currentSpawnBiomeChoice;
    }
    
    /**
     * Writes spawn biome data to NBT
     */
    public static void writeToNbt(NbtCompound nbt) {
        if (currentSpawnBiomeChoice == null) {
            return;
        }
        
        try {
            Identifier biomeId = currentSpawnBiomeChoice.getBiomeKey().getValue();
            nbt.putString("spawn_biome_namespace", biomeId.getNamespace());
            nbt.putString("spawn_biome_path", biomeId.getPath());
            nbt.putLong("spawn_biome_selection_time", currentSpawnBiomeChoice.getSelectionTimestamp());
            
            LOGGER.debug("Wrote spawn biome choice to NBT: {}", currentSpawnBiomeChoice);
        } catch (Exception e) {
            LOGGER.error("Failed to write spawn biome choice to NBT", e);
        }
    }
    
    /**
     * Reads spawn biome data from NBT
     */
    public static void readFromNbt(NbtCompound nbt) {
        try {
            if (!nbt.contains("spawn_biome_namespace") || !nbt.contains("spawn_biome_path")) {
                currentSpawnBiomeChoice = null;
                return;
            }
            
            String namespace = nbt.getString("spawn_biome_namespace");
            String path = nbt.getString("spawn_biome_path");
            
            // Validate namespace and path
            if (namespace.isEmpty() || path.isEmpty()) {
                LOGGER.warn("Invalid spawn biome data in NBT: empty namespace or path");
                currentSpawnBiomeChoice = null;
                return;
            }
            
            // Get timestamp (with migration support)
            long timestamp = nbt.contains("spawn_biome_selection_time") 
                ? nbt.getLong("spawn_biome_selection_time")
                : System.currentTimeMillis(); // Default for migration
            
            // Validate timestamp
            if (timestamp < 0) {
                LOGGER.warn("Invalid spawn biome timestamp in NBT: {}", timestamp);
                currentSpawnBiomeChoice = null;
                return;
            }
            
            // Create biome registry key - use Identifier.of() instead of constructor
            Identifier biomeId = Identifier.of(namespace, path);
            RegistryKey<Biome> biomeKey = RegistryKey.of(RegistryKeys.BIOME, biomeId);
            
            currentSpawnBiomeChoice = new SpawnBiomeChoiceData(biomeKey, timestamp);
            
            LOGGER.debug("Read spawn biome choice from NBT: {}", currentSpawnBiomeChoice);
        } catch (Exception e) {
            LOGGER.error("Failed to read spawn biome choice from NBT", e);
            currentSpawnBiomeChoice = null;
        }
    }
    
    /**
     * Saves current spawn biome choice to world data
     * @deprecated Use SpawnBiomeStorageManager instead
     */
    @Deprecated
    public static void saveToWorldData() {
        LOGGER.warn("DEPRECATED: saveToWorldData called. Use SpawnBiomeStorageManager instead.");
    }
    
    /**
     * Loads spawn biome choice from world data
     * @deprecated Use SpawnBiomeStorageManager instead
     */
    @Deprecated
    public static void loadFromWorldData() {
        LOGGER.warn("DEPRECATED: loadFromWorldData called. Use SpawnBiomeStorageManager instead.");
    }
    
    /**
     * Resets spawn biome choice (for testing and migration)
     */
    public static void resetForTest() {
        currentSpawnBiomeChoice = null;
        LOGGER.debug("Reset legacy spawn biome choice for test/migration");
    }
    
    /**
     * Checks if there is legacy data that needs migration
     * @return true if legacy data exists, false otherwise
     */
    public static boolean hasLegacyData() {
        return currentSpawnBiomeChoice != null;
    }
}