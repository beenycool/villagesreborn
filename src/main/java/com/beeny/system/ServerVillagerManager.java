package com.beeny.system;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import com.beeny.ai.AIWorldManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServerVillagerManager {
    private static ServerVillagerManager instance;
    private final Map<UUID, VillagerEntity> trackedVillagers = new ConcurrentHashMap<>();
    @Nullable
    private MinecraftServer server;
    @Nullable
    private AIWorldManager aiWorldManager;

    private ServerVillagerManager() {
    }

    @NotNull
    public static ServerVillagerManager getInstance() {
        if (instance == null) {
            instance = new ServerVillagerManager();
        }
        return instance;
    }

    public void initialize(@NotNull MinecraftServer server) {
        this.server = server;
        
        // Initialize the AI World Manager
        try {
            this.aiWorldManager = new AIWorldManager(server);
            this.aiWorldManager.start();
            Villagersreborn.LOGGER.info("AI World Manager initialized and started successfully");
        } catch (Exception e) {
            this.aiWorldManager = null; // Ensure it's null on failure
            Villagersreborn.LOGGER.error("Failed to initialize/start AI World Manager during server startup", e);
            // Graceful handling: continue startup without AI systems
        }
        
        registerEvents();
        Villagersreborn.LOGGER.info("ServerVillagerManager initialized (AI World Manager: {})", this.aiWorldManager != null ? "enabled" : "disabled due to error");
    }

    private void registerEvents() {
        // Track when chunks are loaded/unloaded
        ServerChunkEvents.CHUNK_LOAD.register(this::onChunkLoad);
        ServerChunkEvents.CHUNK_UNLOAD.register(this::onChunkUnload);

        // Clear tracked villagers when server stops
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);

        // Track all existing villagers when the server finishes starting to avoid missing already loaded entities
        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            this.server = s;
            for (ServerWorld world : s.getWorlds()) {
                for (Entity entity : world.iterateEntities()) {
                    if (entity instanceof VillagerEntity villager) {
                        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
                        if (data != null) {
                            trackedVillagers.put(villager.getUuid(), villager);
                        }
                    }
                }
            }
            Villagersreborn.LOGGER.info("ServerVillagerManager initial scan complete. Tracked: {}", trackedVillagers.size());
        });
    }

    private void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        // When a chunk loads, trigger a world scan for villagers
        scanWorldForVillagers(world);
    }

    private void onChunkUnload(ServerWorld world, WorldChunk chunk) {
        // When a chunk unloads, clean up any dead references
        trackedVillagers.entrySet().removeIf(entry -> {
            VillagerEntity villager = entry.getValue();
            return villager == null || villager.isRemoved() || villager.getWorld() != world;
        });
    }
    
    private void scanWorldForVillagers(ServerWorld world) {
        // Scan for villagers in the world
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof VillagerEntity villager) {
                VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
                if (data != null && !trackedVillagers.containsKey(villager.getUuid())) {
                    trackedVillagers.put(villager.getUuid(), villager);
                    Villagersreborn.LOGGER.debug("Tracking villager: {} ({})", data.getName(), villager.getUuid());
                }
            }
        }
    }

    private void onServerStopping(MinecraftServer server) {
        // Shutdown AI World Manager first
        if (aiWorldManager != null) {
            aiWorldManager.shutdown();
            aiWorldManager = null;
        }
        
        trackedVillagers.clear();
        Villagersreborn.LOGGER.info("Cleared tracked villagers and shut down AI systems");
    }

    public void trackVillager(VillagerEntity villager) {
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (data != null) {
            trackedVillagers.put(villager.getUuid(), villager);
            
            // Initialize AI for the new villager
            if (aiWorldManager != null) {
                aiWorldManager.initializeVillagerAI(villager);
            }
            
            Villagersreborn.LOGGER.debug("Tracking new villager: {} ({})", data.getName(), villager.getUuid());
        }
    }

    public void untrackVillager(UUID villagerUuid) {
        VillagerEntity removed = trackedVillagers.remove(villagerUuid);
        if (removed != null) {
            // Cleanup AI for the removed villager
            if (aiWorldManager != null) {
                aiWorldManager.cleanupVillagerAI(villagerUuid.toString());
            }
            
            VillagerData data = removed.getAttached(Villagersreborn.VILLAGER_DATA);
            if (data != null) {
                Villagersreborn.LOGGER.debug("Untracking villager: {} ({})", data.getName(), villagerUuid);
            }
        }
    }

    public VillagerEntity getVillager(UUID uuid) {
        return trackedVillagers.get(uuid);
    }

    public Iterable<VillagerEntity> getAllTrackedVillagers() {
        return trackedVillagers.values();
    }

    public int getTrackedVillagerCount() {
        return trackedVillagers.size();
    }
    
    /**
     * Get the AI World Manager for this server instance
     */
    public AIWorldManager getAIWorldManager() {
        return aiWorldManager;
    }
}