package com.beeny.villagesreborn.platform.fabric.spawn.storage;

import com.beeny.villagesreborn.core.world.VillagesRebornWorldDataPersistent;
import com.beeny.villagesreborn.core.world.VillagesRebornWorldSettings;
import com.beeny.villagesreborn.platform.fabric.spawn.SpawnBiomeChoiceData;
import com.beeny.villagesreborn.platform.fabric.spawn.VillagesRebornWorldSettingsExtensions;
import com.beeny.villagesreborn.platform.fabric.spawn.persistence.SpawnBiomeNBTHandler;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * World-level implementation of spawn biome storage using VillagesRebornWorldDataPersistent
 */
public class WorldSpawnBiomeStorage implements SpawnBiomeStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldSpawnBiomeStorage.class);
    
    private final ServerWorld world;
    
    public WorldSpawnBiomeStorage(ServerWorld world) {
        if (world == null) {
            throw new IllegalArgumentException("ServerWorld cannot be null");
        }
        this.world = world;
    }
    
    @Override
    public Optional<SpawnBiomeChoiceData> getSpawnBiomeChoice(RegistryKey<World> worldId) {
        try {
            VillagesRebornWorldDataPersistent persistent = VillagesRebornWorldDataPersistent.get(world);
            
            if (!persistent.hasSettings()) {
                return Optional.empty();
            }
            
            VillagesRebornWorldSettings settings = persistent.getSettings();
            Map<String, Object> customData = settings.getCustomData();
            
            if (!customData.containsKey("spawn_biome_choices")) {
                return Optional.empty();
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> spawnBiomeMap = (Map<String, Object>) customData.get("spawn_biome_choices");
            
            if (spawnBiomeMap == null || !SpawnBiomeNBTHandler.isValidMap(spawnBiomeMap)) {
                return Optional.empty();
            }
            
            SpawnBiomeChoiceData choiceData = SpawnBiomeNBTHandler.deserializeFromMap(spawnBiomeMap);
            LOGGER.debug("Retrieved spawn biome choice for world {}: {}", worldId, choiceData);
            return Optional.of(choiceData);
            
        } catch (Exception e) {
            LOGGER.error("Failed to get spawn biome choice for world {}", worldId, e);
            return Optional.empty();
        }
    }
    
    @Override
    public void setSpawnBiomeChoice(RegistryKey<World> worldId, SpawnBiomeChoiceData choice) {
        if (choice == null) {
            throw new IllegalArgumentException("SpawnBiomeChoiceData cannot be null");
        }
        
        try {
            VillagesRebornWorldDataPersistent persistent = VillagesRebornWorldDataPersistent.get(world);
            
            VillagesRebornWorldSettings settings = persistent.getSettings();
            if (settings == null) {
                settings = new VillagesRebornWorldSettings();
            }
            
            Map<String, Object> customData = settings.getCustomData();
            Map<String, Object> spawnBiomeMap = SpawnBiomeNBTHandler.serializeToMap(choice);
            
            customData.put("spawn_biome_choices", spawnBiomeMap);
            customData.put("spawn_biome_last_updated", System.currentTimeMillis());
            
            settings.setCustomData(customData);
            persistent.setSettings(settings);
            persistent.markDirty();
            
            LOGGER.info("Set spawn biome choice for world {}: {}", worldId, choice);
            
        } catch (Exception e) {
            LOGGER.error("Failed to set spawn biome choice for world {}", worldId, e);
            throw new RuntimeException("Failed to save spawn biome choice", e);
        }
    }
    
    @Override
    public void clearSpawnBiomeChoice(RegistryKey<World> worldId) {
        try {
            VillagesRebornWorldDataPersistent persistent = VillagesRebornWorldDataPersistent.get(world);
            
            if (!persistent.hasSettings()) {
                return; // Nothing to clear
            }
            
            VillagesRebornWorldSettings settings = persistent.getSettings();
            Map<String, Object> customData = settings.getCustomData();
            
            customData.remove("spawn_biome_choices");
            customData.remove("spawn_biome_last_updated");
            
            settings.setCustomData(customData);
            persistent.setSettings(settings);
            persistent.markDirty();
            
            LOGGER.info("Cleared spawn biome choice for world {}", worldId);
            
        } catch (Exception e) {
            LOGGER.error("Failed to clear spawn biome choice for world {}", worldId, e);
        }
    }
    
    @Override
    public boolean hasSpawnBiomeChoice(RegistryKey<World> worldId) {
        return getSpawnBiomeChoice(worldId).isPresent();
    }
    
    @Override
    public Optional<SpawnBiomeChoiceData> getPlayerBiomePreference(UUID playerId) {
        // World-level storage doesn't handle player preferences
        return Optional.empty();
    }
    
    @Override
    public void setPlayerBiomePreference(UUID playerId, SpawnBiomeChoiceData preference) {
        // World-level storage doesn't handle player preferences
        LOGGER.warn("WorldSpawnBiomeStorage cannot handle player preferences. Use PlayerSpawnBiomeStorage instead.");
    }
    
    @Override
    public void migrateFromStaticData() {
        try {
            // Check if migration already performed
            if (hasSpawnBiomeChoice(world.getRegistryKey())) {
                LOGGER.debug("Spawn biome choice already exists for world {}, skipping migration", world.getRegistryKey());
                return;
            }
            
            // Check for legacy static data
            SpawnBiomeChoiceData legacyChoice = VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice();
            if (legacyChoice != null) {
                setSpawnBiomeChoice(world.getRegistryKey(), legacyChoice);
                
                // Clear legacy static storage
                VillagesRebornWorldSettingsExtensions.resetForTest();
                
                LOGGER.info("Migrated legacy spawn biome choice to world storage: {}", legacyChoice);
            } else {
                LOGGER.debug("No legacy spawn biome choice found for migration");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to migrate legacy spawn biome data", e);
        }
    }
    
    @Override
    public boolean hasLegacyData() {
        try {
            return VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice() != null;
        } catch (Exception e) {
            LOGGER.error("Failed to check for legacy data", e);
            return false;
        }
    }
    
    /**
     * Gets the server world this storage is associated with
     * @return The server world
     */
    public ServerWorld getWorld() {
        return world;
    }
}