package com.beeny.village.event;

import com.beeny.village.VillageInfluenceManager;
import com.beeny.village.artifacts.CulturalArtifactSystem;
import net.minecraft.block.Block;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement; // Added import
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Main integration class for the cultural event system.
 * Initializes and provides access to all components of the event system.
 */
public class CulturalEventSystem {
    private static final CulturalEventSystem INSTANCE = new CulturalEventSystem();
    
    private CulturalEventSystem() {
        // Private constructor for singleton
    }
    
    /**
     * Get the singleton instance
     */
    public static CulturalEventSystem getInstance() {
        return INSTANCE;
    }
    
    /**
     * Initialize the entire cultural event system
     */
    public void initialize() {
        // Nothing to initialize explicitly since components initialize themselves when accessed
    }
    
    /**
     * Update the event scheduler (should be called from a tick event)
     */
    public void tickEventScheduler(World world) {
        VillageEventScheduler.getInstance().update(world);
    }
    
    /**
     * Register a village for event scheduling
     */
    public void registerVillageForEvents(BlockPos center, String culture, int frequency, int radius) {
        // First, register with village influence manager if not already registered
        VillageInfluenceManager.getInstance().registerVillage(center, culture);
        
        // Then register for event scheduling
        VillageEventScheduler.getInstance().registerVillage(center, frequency, radius);
    }
    
    /**
     * Handle player interaction with a block
     */
    public void handlePlayerBlockInteraction(ServerPlayerEntity player, BlockPos pos, Block block) {
        CulturalEventActivityHandler.getInstance().handleBlockInteraction(player, pos, block);
    }
    
    /**
     * Handle player interaction with a villager
     */
    public void handlePlayerVillagerInteraction(ServerPlayerEntity player, VillagerEntity villager) {
        CulturalEventActivityHandler.getInstance().handleVillagerInteraction(player, villager);
    }
    
    /**
     * Handle player using an item
     */
    public void handlePlayerItemUse(ServerPlayerEntity player, ItemStack itemStack) {
        CulturalEventActivityHandler.getInstance().handleItemUse(player, itemStack);
    }
    
    /**
     * Player joining an event
     */
    public void joinEvent(ServerPlayerEntity player, String eventId) {
        VillageEvent event = VillageEvent.getEvent(eventId);
        if (event != null) {
            event.addParticipant(player);
        }
    }
    
    /**
     * Force an event to happen at a village
     */
    public void forceEvent(World world, BlockPos villageCenter, String eventType) {
        VillageEventScheduler.getInstance().forceEvent(world, villageCenter, eventType);
    }
    
    /**
     * Generate a cultural artifact for a player
     */
    public ItemStack generateArtifact(ServerPlayerEntity player, String culture, String eventType) {
        return CulturalArtifactSystem.getInstance().generateEventArtifact(player, culture, eventType);
    }
    
    /**
     * Display a cultural artifact at a location
     */
    public boolean displayArtifact(ServerPlayerEntity player, String artifactId, BlockPos location) {
        return CulturalArtifactSystem.getInstance().displayArtifact(player, artifactId, location);
    }
    
    /**
     * Get player's reputation with a culture
     */
    public int getPlayerReputation(ServerPlayerEntity player, String culture) {
        return PlayerEventParticipation.getInstance().getReputation(player, culture);
    }
    
    /**
     * Add reputation points for a player
     */
    public void addPlayerReputation(ServerPlayerEntity player, String culture, int points) {
        PlayerEventParticipation.getInstance().addReputationPoints(player, culture, points);
    }
    
    /**
     * Get player's reputation title with a culture
     */
    public String getPlayerReputationTitle(ServerPlayerEntity player, String culture) {
        int reputation = PlayerEventParticipation.getInstance().getReputation(player, culture);
        return PlayerEventParticipation.getInstance().getReputationTitle(culture, reputation);
    }
    
    /**
     * Clean up expired events for a player
     */
    public void cleanupPlayerEvents(ServerPlayerEntity player) {
        PlayerEventParticipation.getInstance().cleanupPlayerEvents(player);
    }
    
    /**
     * Save player's cultural data (should be combined with other player data)
     */
    public void savePlayerData(ServerPlayerEntity player) {
        UUID playerUUID = player.getUuid();
        
        // This should be integrated with your mod's player data saving system
        // Here's what would need to be saved:
        
        // 1. Event participation data
        NbtCompound eventData = PlayerEventParticipation.getInstance().savePlayerData(playerUUID);
        
        // 2. Artifact data
        NbtCompound artifactData = CulturalArtifactSystem.getInstance().savePlayerArtifacts(playerUUID);
        
        // Combine into one compound and save to player persistent data
        NbtCompound combined = new NbtCompound();
        combined.put("eventParticipation", eventData);
        combined.put("artifacts", artifactData);
        
        // This is where you would save the combined data to your player data system
    }
    
    /**
     * Load player's cultural data
     */
    public void loadPlayerData(ServerPlayerEntity player, NbtCompound data) {
        if (data == null) {
            return;
        }
        
        UUID playerUUID = player.getUuid();
        
        // Load event participation data
        if (data.contains("eventParticipation")) {
            NbtCompound eventData = data.contains("eventParticipation", NbtElement.COMPOUND_TYPE) ? data.getCompound("eventParticipation") : new NbtCompound();
            PlayerEventParticipation.getInstance().loadPlayerData(playerUUID, eventData);
        }
        
        // Load artifact data
        if (data.contains("artifacts")) {
            NbtCompound artifactData = data.contains("artifacts", NbtElement.COMPOUND_TYPE) ? data.getCompound("artifacts") : new NbtCompound();
            CulturalArtifactSystem.getInstance().loadPlayerArtifacts(playerUUID, artifactData);
        }
    }
}