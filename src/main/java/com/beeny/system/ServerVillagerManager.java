package com.beeny.system;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import com.beeny.ai.AIWorldManagerRefactored;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
    private AIWorldManagerRefactored aiWorldManager;

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
            AIWorldManagerRefactored.initialize(server);
            this.aiWorldManager = AIWorldManagerRefactored.getInstance();
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
            AIWorldManagerRefactored.cleanup();
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
    public AIWorldManagerRefactored getAIWorldManager() {
        return aiWorldManager;
    }

    /**
     * Export all tracked VillagerData to world/villagersreborn/backups/<timestamp>/villagers.json
     * The export includes: id, name, age, gender, personality, happiness, profession history,
     * spouse, children, birth place, notes, timestamps (birth/death/lastConversation), and more.
     */
    public void exportVillagerDataBackup() {
        if (this.server == null) {
            Villagersreborn.LOGGER.error("exportVillagerDataBackup(): Server reference is null; cannot resolve world directory");
            return;
        }

        // Resolve the root world directory (same location Minecraft saves level.dat)
        Path worldDir = this.server.getSavePath(net.minecraft.world.WorldSavePath.ROOT);
        if (worldDir == null) {
            Villagersreborn.LOGGER.error("exportVillagerDataBackup(): Failed to resolve world directory path");
            return;
        }

        // Prepare timestamped backup directory
        String timestamp = java.time.ZonedDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss'Z'"));
        Path backupDir = worldDir.resolve("villagersreborn").resolve("backups").resolve(timestamp);

        try {
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            Villagersreborn.LOGGER.error("exportVillagerDataBackup(): Failed to create backup directory {}", backupDir, e);
            return;
        }

        // Build JSON manually to avoid adding a new dependency (Gson/Jackson).
        // We escape strings safely for JSON.
        StringBuilder json = new StringBuilder();
        json.append("{\"villagers\":[");

        boolean first = true;
        for (VillagerEntity villager : getAllTrackedVillagers()) {
            if (villager == null || villager.isRemoved()) continue;

            VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            if (data == null) continue;

            Map<String, Object> obj = new HashMap<>();
            obj.put("id", villager.getUuid().toString());
            obj.put("name", data.getName());
            obj.put("age", data.getAge());
            obj.put("gender", data.getGender());
            obj.put("personality", data.getPersonality().name());
            obj.put("happiness", data.getHappiness());
            obj.put("professionHistory", data.getProfessionHistory());
            obj.put("spouseId", data.getSpouseId());
            obj.put("spouseName", data.getSpouseName());
            obj.put("childrenIds", data.getChildrenIds());
            obj.put("childrenNames", data.getChildrenNames());
            obj.put("birthPlace", data.getBirthPlace());
            obj.put("notes", data.getNotes());
            obj.put("birthTime", data.getBirthTime());
            obj.put("deathTime", data.getDeathTime());
            obj.put("isAlive", data.isAlive());
            obj.put("lastConversationTime", data.getLastConversationTime());
            // include a couple of useful aggregates for completeness
            obj.put("totalTrades", data.getTotalTrades());
            obj.put("favoriteFood", data.getFavoriteFood());
            obj.put("hobby", data.getHobby().name());

            if (!first) {
                json.append(",");
            } else {
                first = false;
            }
            json.append(serializeObject(obj));
        }

        json.append("]}");

        Path file = backupDir.resolve("villagers.json");
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write(json.toString());
        } catch (IOException e) {
            Villagersreborn.LOGGER.error("exportVillagerDataBackup(): Failed writing backup JSON to {}", file, e);
            return;
        }

        Villagersreborn.LOGGER.info("Villager data backup exported: {}", file.toAbsolutePath());
    }

    // Minimal JSON utilities to avoid new dependencies

    private static String serializeObject(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escape(e.getKey())).append("\":").append(serializeValue(e.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static String serializeValue(Object v) {
        if (v == null) return "null";
        if (v instanceof String s) return "\"" + escape(s) + "\"";
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        if (v instanceof Iterable<?> it) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (Object item : it) {
                if (!first) sb.append(",");
                first = false;
                sb.append(serializeValue(item));
            }
            sb.append("]");
            return sb.toString();
        }
        // Fallback to string
        return "\"" + escape(String.valueOf(v)) + "\"";
    }

    private static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}