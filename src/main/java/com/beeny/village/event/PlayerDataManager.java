package com.beeny.village.event;

import com.beeny.Villagesreborn;
import com.beeny.village.artifacts.CulturalArtifactSystem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Manages player data persistence for the Villages Reborn mod.
 * Handles saving and loading of player event participation, reputation, and artifact data.
 * This class is specific to the 'event' package.
 */
public class PlayerDataManager { // Keep the class name as is within this package
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final PlayerDataManager INSTANCE = new PlayerDataManager();

    private PlayerDataManager() {
        // Private constructor to enforce singleton pattern
    }

    /**
     * Get singleton instance
     */
    public static PlayerDataManager getInstance() {
        return INSTANCE;
    }

    /**
     * Save all player data to the player's persistent data storage
     * Should be called when a player logs out or when the server is saving data
     *
     * @param player The player whose data should be saved
     */
    public void savePlayerData(ServerPlayerEntity player) {
        LOGGER.debug("Saving player data for {}", player.getName().getString());

        UUID playerUUID = player.getUuid();

        // Get data from PlayerEventParticipation
        NbtCompound eventData = PlayerEventParticipation.getInstance().savePlayerData(playerUUID);

        // Get data from CulturalArtifactSystem
        NbtCompound artifactData = CulturalArtifactSystem.getInstance().savePlayerArtifacts(playerUUID);

        // Combine into one compound
        NbtCompound villagesData = new NbtCompound();
        villagesData.put("eventParticipation", eventData);
        villagesData.put("artifacts", artifactData);

        // Store in the player's persistent data using the correct PlayerDataManager from the 'data' package
        NbtCompound persistentData = com.beeny.data.PlayerDataManager.getOrCreatePersistentData(player);
        persistentData.put(Villagesreborn.MOD_ID, villagesData);
        // No need to call savePersistentData explicitly if getOrCreatePersistentData modifies the original NBT
    }

    /**
     * Load player data from the player's persistent data storage
     * Should be called when a player logs in or changes dimensions
     *
     * @param player The player whose data should be loaded
     */
    public void loadPlayerData(ServerPlayerEntity player) {
        LOGGER.debug("Loading player data for {}", player.getName().getString());

        UUID playerUUID = player.getUuid();

        // Get the player's persistent data using the correct PlayerDataManager from the 'data' package
        NbtCompound persistentData = com.beeny.data.PlayerDataManager.getOrCreatePersistentData(player);

        // Return if no data exists for our mod
        if (!persistentData.contains(Villagesreborn.MOD_ID)) {
            LOGGER.debug("No saved data found for player {}", player.getName().getString());
            return;
        }

        // Get our mod's data
        // .get() is safe here because line 76 checks contains()
        NbtCompound villagesData = persistentData.getCompound(Villagesreborn.MOD_ID);

        // Load event participation data
        if (villagesData.contains("eventParticipation")) {
            // .get() is safe here because line 85 checks contains()
            NbtCompound eventData = villagesData.getCompound("eventParticipation");
            PlayerEventParticipation.getInstance().loadPlayerData(playerUUID, eventData);
        }

        // Load artifact data
        if (villagesData.contains("artifacts")) {
            // .get() is safe here because line 91 checks contains()
            NbtCompound artifactData = villagesData.getCompound("artifacts");
            CulturalArtifactSystem.getInstance().loadPlayerArtifacts(playerUUID, artifactData);
        }
    }

    /**
     * Clean up expired events and data for a player
     * Should be called periodically to keep player data size manageable
     *
     * @param player The player whose data should be cleaned up
     */
    public void cleanupPlayerData(ServerPlayerEntity player) {
        PlayerEventParticipation.getInstance().cleanupPlayerEvents(player);
        // Could add more cleanup tasks here for other data types
    }
}