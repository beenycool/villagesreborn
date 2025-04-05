package com.beeny.village;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.beeny.ai.LLMService;
import com.beeny.ai.CulturalPromptTemplates;
import com.beeny.util.NameGenerator;
import com.beeny.api.VillageHudAPI;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import com.beeny.network.VillagesNetwork;
import com.beeny.server.EventNotificationManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.math.Box;

public class VillagerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final VillagerManager INSTANCE = new VillagerManager();
    private final Map<UUID, VillagerAI> villagerAIs;
    private final Map<UUID, VillagerDialogue> villagerDialogues;
    private final Map<BlockPos, SpawnRegion> spawnRegions;
    private final Map<String, CulturalEvent> culturalEvents;
    private final Map<BlockPos, VillageStats> villageStats;
    private final Map<UUID, Map<UUID, Relationship>> relationships;
    private final NameGenerator nameGenerator;
    private final Random random;
    private final Cache<UUID, List<UUID>> nearbyVillagersCache;
    private long lastCacheUpdateTime = 0;
    private static final long CACHE_UPDATE_INTERVAL = 100;
    private static final double NEARBY_DISTANCE_SQUARED = 100.0;
    private final ReentrantReadWriteLock villageLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock culturalEventLock = new ReentrantReadWriteLock();
    private ServerWorld world;
    private Culture.CultureType currentCulture;
    private MinecraftServer server;
    private final Map<java.util.function.Predicate<net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext>, String> biomeCultureAssociations = new HashMap<>();
    
    // Add a static reference to the server for safe access
    private static MinecraftServer staticServerInstance;

    private VillagerManager() {
        this.villagerAIs = new ConcurrentHashMap<>();
        this.villagerDialogues = new ConcurrentHashMap<>();
        this.spawnRegions = new HashMap<>();
        this.culturalEvents = new HashMap<>();
        this.villageStats = new ConcurrentHashMap<>();
        this.relationships = new ConcurrentHashMap<>();
        this.nameGenerator = new NameGenerator();
        this.random = new Random();
        this.nearbyVillagersCache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).maximumSize(1000).build();
    }

    public static VillagerManager getInstance() {
        return INSTANCE;
    }

    public MinecraftServer getServer() {
        return server;
    }

    /**
     * Get the server instance safely from a static context
     * @return The current MinecraftServer instance
     */
    public static MinecraftServer getServerInstance() {
        if (staticServerInstance == null) {
            staticServerInstance = INSTANCE.server;
        }
        return staticServerInstance;
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
        staticServerInstance = server; // Also update static reference
    }

    public void registerSpawnRegion(BlockPos center, int radius, String cultureName) {
        villageLock.writeLock().lock();
        try {
            Culture culture = new Culture(Culture.CultureType.valueOf(cultureName.toUpperCase()));
            SpawnRegion region = new SpawnRegion(culture, center, radius);
            spawnRegions.put(center, region);
            addVillageStats(center, cultureName);
            LOGGER.info("Registered new {} village at {}", cultureName, center);
        } finally {
            villageLock.writeLock().unlock();
        }
    }

    public void addRelationship(UUID villager1, UUID villager2, VillagerAI.RelationshipType type) {
        relationships.computeIfAbsent(villager1, k -> new ConcurrentHashMap<>()).put(villager2, new Relationship(type));
        relationships.computeIfAbsent(villager2, k -> new ConcurrentHashMap<>()).put(villager1, new Relationship(type));
    }

    public Relationship getRelationship(UUID villager1, UUID villager2) {
        Map<UUID, Relationship> villagerRelations = relationships.get(villager1);
        return villagerRelations != null ? villagerRelations.get(villager2) : null;
    }

    public int getRelationshipCount(UUID villagerUUID) {
        Map<UUID, Relationship> villagerRelations = relationships.get(villagerUUID);
        return villagerRelations != null ? villagerRelations.size() : 0;
    }

    public void updateVillagerActivities(ServerWorld world) {
        long currentTime = world.getTimeOfDay();
        MoodManager moodManager = MoodManager.getInstance();
        VillageRelationshipEffects relationshipEffects = VillageRelationshipEffects.getInstance();
        if (currentTime - lastCacheUpdateTime >= CACHE_UPDATE_INTERVAL) {
            updateNearbyVillagersCache();
            lastCacheUpdateTime = currentTime;
        }
        updateCulturalEvents(world);
        if (currentTime % 24000 == 0) {
            updateVillageStats(world);
        }
        villagerAIs.values().parallelStream().forEach(ai -> {
            ai.updateActivityBasedOnTime(world);
            moodManager.updateVillagerMood(ai, world);
            VillagerEntity villager = ai.getVillager();
            if (villager != null && villager.isAlive() && currentTime % 60 == 0) {
                relationshipEffects.processAllyEffects(villager);
                relationshipEffects.processHostileEffects(villager);
            }
        });
        if (currentTime % 300 == 0) {
            processVillageRelationships(world);
        }
        if (currentTime % 20 == 0) {
            for (VillagerAI ai : villagerAIs.values()) {
                if (random.nextFloat() < 0.02f) {
                    generateRandomSocialInteraction(ai, world);
                }
            }
        }
    }

    private void updateNearbyVillagersCache() {
        nearbyVillagersCache.cleanUp();
        Map<Long, List<VillagerAI>> chunkMap = new HashMap<>();
        for (VillagerAI ai : villagerAIs.values()) {
            VillagerEntity villager = ai.getVillager();
            if (villager == null || !villager.isAlive()) continue;
            int chunkX = villager.getBlockX() >> 4;
            int chunkZ = villager.getBlockZ() >> 4;
            long chunkPos = (((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL);
            chunkMap.computeIfAbsent(chunkPos, k -> new ArrayList<>()).add(ai);
        }
        for (VillagerAI ai1 : villagerAIs.values()) {
            VillagerEntity v1 = ai1.getVillager();
            if (v1 == null || !v1.isAlive()) continue;
            int chunkX = v1.getBlockX() >> 4;
            int chunkZ = v1.getBlockZ() >> 4;
            List<UUID> nearby = new ArrayList<>();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    long adjacentChunk = (((long) (chunkX + dx)) << 32) | ((chunkZ + dz) & 0xFFFFFFFFL);
                    List<VillagerAI> chunkVillagers = chunkMap.get(adjacentChunk);
                    if (chunkVillagers != null) {
                        for (VillagerAI ai2 : chunkVillagers) {
                            if (ai1 == ai2) continue;
                            VillagerEntity v2 = ai2.getVillager();
                            if (v2 != null && v1.squaredDistanceTo(v2) < NEARBY_DISTANCE_SQUARED) {
                                nearby.add(v2.getUuid());
                            }
                        }
                    }
                }
            }
            if (!nearby.isEmpty()) {
                nearbyVillagersCache.put(v1.getUuid(), nearby);
            }
        }
    }

    private void generateRandomSocialInteraction(VillagerAI villager1, ServerWorld world) {
        VillagerEntity v1 = villager1.getVillager();
        if (v1 == null || !v1.isAlive()) return;
        UUID villager1UUID = v1.getUuid();
        List<UUID> nearbyIds = null;
        try {
            nearbyIds = nearbyVillagersCache.getIfPresent(villager1UUID);
        } catch (Exception e) {
            LOGGER.debug("Cache error when retrieving nearby villagers", e);
            return;
        }
        if (nearbyIds == null || nearbyIds.isEmpty()) return;
        UUID villager2UUID = nearbyIds.get(random.nextInt(nearbyIds.size()));
        VillagerAI villager2 = villagerAIs.get(villager2UUID);
        if (villager2 == null) return;
        VillagerEntity v2 = villager2.getVillager();
        if (v2 == null || !v2.isAlive()) return;
        if (villager1.isBusy() || villager2.isBusy()) return;
        villager1.setBusy(true);
        villager2.setBusy(true);
        try {
            positionVillagersForInteraction(v1, v2);
            String location = getLocationContext(v1.getBlockPos());
            String timeContext = getTimeContext(world.getTimeOfDay());
            String sharedHistory = getSharedHistory(villager1, villager2);
            SpawnRegion region = getNearestSpawnRegion(v1.getBlockPos());
            String culturalContext = region != null ? region.getCulture().toString() : "unknown";
            String weather = world.isRaining() ? "raining" : "clear";
            String nearbyActivities = getNearbyActivities(world, v1.getBlockPos());
            String interactionPrompt = CulturalPromptTemplates.getTemplate("villager_interaction", v1.getName().getString(), villager1.getPersonality(), villager1.getHappiness(), v2.getName().getString(), villager2.getPersonality(), villager2.getHappiness(), location, timeContext, sharedHistory, culturalContext, weather, nearbyActivities);
            LLMService.getInstance().generateResponse(interactionPrompt).orTimeout(5, TimeUnit.SECONDS).thenAccept(response -> {
                try {
                    Map<String, String> interaction = parseResponse(response);
                    applyInteractionEffects(world, v1, v2, interaction);
                    updateRelationships(villager1, villager2, interaction);
                    if (arePlayersNearby(world, v1.getBlockPos(), 32)) {
                        displayInteraction(world, v1, v2, interaction);
                    }
                    updateVillagerStates(villager1, villager2, interaction);
                } finally {
                    CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS).execute(() -> {
                        villager1.setBusy(false);
                        villager2.setBusy(false);
                    });
                }
            }).exceptionally(e -> {
                LOGGER.error("Error in social interaction", e);
                villager1.setBusy(false);
                villager2.setBusy(false);
                return null;
            });
        } catch (Exception e) {
            villager1.setBusy(false);
            villager2.setBusy(false);
            LOGGER.error("Error in social interaction", e);
        }
    }

    private boolean arePlayersNearby(ServerWorld world, BlockPos pos, int radius) {
        double radiusSquared = radius * radius;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.getBlockPos().getSquaredDistance(pos) <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    private void addInteractionEffects(ServerWorld world, VillagerEntity v1, VillagerEntity v2, VillagerAI.RelationshipType type) {
        double x = (v1.getX() + v2.getX()) / 2;
        double y = (v1.getY() + v2.getY()) / 2 + 1;
        double z = (v1.getZ() + v2.getZ()) / 2;
        switch (type) {
            case FRIEND:
                world.spawnParticles(ParticleTypes.HEART, x, y, z, 2, 0.2, 0.2, 0.2, 0.02);
                break;
            case FAMILY:
                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 3, 0.2, 0.2, 0.2, 0.02);
                world.spawnParticles(ParticleTypes.HEART, x, y, z, 1, 0.2, 0.2, 0.2, 0.02);
                break;
            case RIVAL:
                world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, x, y, z, 2, 0.2, 0.2, 0.2, 0.02);
                break;
        }
    }

    private void updateCulturalEvents(ServerWorld world) {
        long timeOfDay = world.getTimeOfDay();
        culturalEventLock.writeLock().lock();
        try {
            List<String> expiredEvents = new ArrayList<>();
            for (Map.Entry<String, CulturalEvent> entry : culturalEvents.entrySet()) {
                if (timeOfDay > entry.getValue().startTime + entry.getValue().duration) {
                    expiredEvents.add(entry.getKey());
                }
            }
            for (String eventName : expiredEvents) {
                culturalEvents.remove(eventName);
            }
            if (culturalEvents.isEmpty() && random.nextFloat() < 0.0005f) {
                generateCulturalEvent(world);
            }
        } finally {
            culturalEventLock.writeLock().unlock();
        }
    }

    private void updateVillageStats(ServerWorld world) {
        villageLock.readLock().lock();
        try {
            for (Map.Entry<BlockPos, VillageStats> entry : villageStats.entrySet()) {
                VillageStats stats = entry.getValue();
                SpawnRegion region = spawnRegions.get(entry.getKey());
                if (region != null) {
                    ServerPlayerEntity nearestPlayer = getNearestPlayer(world, entry.getKey());
                    int villagerCount = getVillagerCount(region);
                    if (world.getTimeOfDay() % 72000 == 0) {
                        updateVillageStatsWithLLM(stats, region, villagerCount, world);
                    } else {
                        updateVillageStatsHeuristically(stats, region, villagerCount);
                    }
                    if (nearestPlayer != null) {
                        VillageHudAPI.getInstance().updateVillageInfo(stats.culture, stats.prosperity, stats.safety, villagerCount);
                    }
                }
            }
        } finally {
            villageLock.readLock().unlock();
        }
    }

    private ServerPlayerEntity getNearestPlayer(ServerWorld world, BlockPos center) {
        double closestDistance = 100 * 100;
        ServerPlayerEntity closest = null;
        for (ServerPlayerEntity player : world.getPlayers()) {
            double distance = player.getBlockPos().getSquaredDistance(center);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = player;
            }
        }
        return closest;
    }

    private void updateVillageStatsHeuristically(VillageStats stats, SpawnRegion region, int villagerCount) {
        int structureCount = region.getCulturalStructures().size();
        int poiCount = region.getPointsOfInterest().size();
        stats.prosperity = Math.min(100, Math.max(10, (structureCount * 5) + (villagerCount * 3) + (poiCount * 2)));
        double density = villagerCount / (Math.PI * region.getRadius() * region.getRadius());
        stats.safety = Math.min(100, Math.max(0, (int) (density * 50) + (stats.prosperity / 4) + 20));
    }

    private void updateVillageStatsWithLLM(VillageStats stats, SpawnRegion region, int villagerCount, ServerWorld world) {
        StringBuilder prompt = new StringBuilder().append("Analyze this village's current state:\n").append("- Culture: ").append(stats.culture).append("\n").append("- Building count: ").append(region.getCulturalStructures().size()).append("\n").append("- Population: ").append(villagerCount).append("\n").append("- Recent events: ").append(getRecentEvents(region, world)).append("\n").append("\nProvide village stats in format:\n").append("PROSPERITY: (number 0-100)\n").append("SAFETY: (number 0-100)");
        LLMService.getInstance().generateResponse(prompt.toString()).orTimeout(5, TimeUnit.SECONDS).exceptionally(e -> {
            LOGGER.warn("LLM village stats generation failed, using heuristics instead", e);
            return "PROSPERITY: " + stats.prosperity + "\nSAFETY: " + stats.safety;
        }).thenAccept(response -> {
            Map<String, String> values = parseResponse(response);
            try {
                stats.prosperity = Integer.parseInt(values.getOrDefault("PROSPERITY", String.valueOf(stats.prosperity)));
                stats.safety = Integer.parseInt(values.getOrDefault("SAFETY", String.valueOf(stats.safety)));
            } catch (NumberFormatException e) {
                LOGGER.error("Failed to parse village stats numbers", e);
            }
        });
    }

    private int getVillagerCount(SpawnRegion region) {
        return (int) villagerAIs.values().stream().filter(ai -> region.isWithinRegion(ai.getVillager().getBlockPos())).count();
    }

    private String getRecentEvents(SpawnRegion region, ServerWorld world) {
        return culturalEvents.values().stream().filter(event -> event.startTime + event.duration > world.getTimeOfDay()).map(event -> event.name).collect(Collectors.joining(", "));
    }

    private void generateCulturalEvent(ServerWorld world) {
        String culture = getCultureName();
        if (culture == null) return;
        culturalEventLock.writeLock().lock();
        try {
            String prompt = String.format("You are creating a cultural event for a %s-themed Minecraft village.\n" + "Design a unique festival or celebration that reflects this culture.\n" + "Format your response as:\n" + "NAME: (event name)\n" + "DURATION: (number of minecraft days)\n" + "DESCRIPTION: (brief description)\n" + "ACTIVITIES: (list of 2-3 villager activities during event)", culture);
            LLMService.getInstance().generateResponse(prompt).orTimeout(8, TimeUnit.SECONDS).exceptionally(e -> {
                LOGGER.error("Cultural event generation failed", e);
                return "NAME: Village Gathering\nDURATION: 1\nDESCRIPTION: A simple gathering.\nACTIVITIES: socialize, trade";
            }).thenAccept(response -> {
                Map<String, String> eventDetails = parseResponse(response);
                String eventName = eventDetails.getOrDefault("NAME", "Village Gathering");
                String description = eventDetails.getOrDefault("DESCRIPTION", "A cultural gathering");
                int duration = 0;
                try {
                    duration = Integer.parseInt(eventDetails.getOrDefault("DURATION", "1")) * 24000;
                } catch (NumberFormatException e) {
                    duration = 24000;
                }
                CulturalEvent event = new CulturalEvent(eventName, description, duration, world.getTimeOfDay());
                culturalEventLock.writeLock().lock();
                try {
                    culturalEvents.put(event.name, event);
                } finally {
                    culturalEventLock.writeLock().unlock();
                }
                List<String> activities = Arrays.asList(eventDetails.getOrDefault("ACTIVITIES", "celebrate").split(", "));
                for (VillagerAI ai : villagerAIs.values()) {
                    String activity = activities.get(world.getRandom().nextInt(activities.size()));
                    ai.updateActivity("event_" + activity);
                    broadcastVillagerAIUpdate(ai.getVillager(), "event_" + activity);
                }
                SpawnRegion eventRegion = null;
                for (SpawnRegion region : spawnRegions.values()) {
                    if (region.getCulture().toString().equalsIgnoreCase(culture)) {
                        eventRegion = region;
                        break;
                    }
                }
                int notificationRadius = eventRegion != null ? eventRegion.getRadius() * 2 : 128;
                BlockPos eventCenter = eventRegion != null ? eventRegion.getCenter() : world.getSpawnPos();
                broadcastEventNotification(eventName, description, eventCenter, notificationRadius);
                LOGGER.info("Generated cultural event: {} for {} village", event.name, culture);
            });
        } finally {
            culturalEventLock.writeLock().unlock();
        }
    }

    public void registerTickEvent(MinecraftServer server) {
        ServerTickEvents.END_SERVER_TICK.register(server1 -> {
            for (ServerWorld world : server1.getWorlds()) {
                updateVillagerActivities(world);
            }
        });
    }

    public void setCulture(String cultureName) {
        this.currentCulture = Culture.CultureType.valueOf(cultureName.toUpperCase());
    }

    public Culture.CultureType getCulture() {
        return currentCulture;
    }

    public String getCultureName() {
        return currentCulture != null ? currentCulture.getDisplayName() : "Unknown";
    }

    public void onVillagerSpawn(VillagerEntity villager, ServerWorld world) {
        this.world = world;
        SpawnRegion region = getNearestSpawnRegion(villager.getBlockPos());
        if (region == null) {
            LOGGER.debug("Villager spawned outside any known region: {}", villager.getBlockPos());
            return;
        }
        CompletableFuture.allOf(CompletableFuture.runAsync(() -> {
            String personality = generatePersonality(region.getCulture().toString());
            VillagerAI ai = new VillagerAI(villager, personality);
            villagerAIs.put(villager.getUuid(), ai);
            VillagerMemory memory = new VillagerMemory();
            VillagerDialogue dialogue = new VillagerDialogue(villager, memory, region.getCulture().toString(), villager.getVillagerData().getProfession().toString());
            villagerDialogues.put(villager.getUuid(), dialogue);
            String situation = String.format("Starting life as a %s in a %s village", villager.getVillagerData().getProfession(), region.getCulture().toString());
            ai.generateBehavior(situation);
        }), nameGenerator.generateName(region.getCulture().toString(), villager.getVillagerData().getProfession().toString()).thenAccept(name -> {
            villager.setCustomName(Text.of(name));
            villager.setCustomNameVisible(true);
            LOGGER.info("Initialized new villager: {} in {} village", name, region.getCulture().toString());
        })).exceptionally(e -> {
            LOGGER.error("Error initializing villager", e);
            return null;
        });
    }

    private String generatePersonality(String culture) {
        String prompt = String.format("Create a unique personality for a villager from %s culture.\n" + "Consider their cultural values, traditions, and way of life.\n" + "Describe their personality in 5-7 words.", culture);
        try {
            return LLMService.getInstance().generateResponse(prompt).get(10, TimeUnit.SECONDS).trim();
        } catch (Exception e) {
            LOGGER.error("Error generating villager personality", e);
            return "friendly local villager";
        }
    }

    public void removeVillager(UUID uuid) {
        villagerAIs.remove(uuid);
        villagerDialogues.remove(uuid);
    }

    public VillagerAI getVillagerAI(UUID uuid) {
        return villagerAIs.get(uuid);
    }

    public VillagerDialogue getVillagerDialogue(VillagerEntity villager) {
        return villagerDialogues.get(villager.getUuid());
    }

    public VillagerDialogue getVillagerDialogue(UUID uuid) {
        return villagerDialogues.get(uuid);
    }

    public SpawnRegion getNearestSpawnRegion(BlockPos pos) {
        SpawnRegion nearest = null;
        double minDistance = Double.MAX_VALUE;
        for (SpawnRegion region : spawnRegions.values()) {
            double distance = pos.getSquaredDistance(region.getCenter());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = region;
            }
        }
        return nearest;
    }

    public SpawnRegion getSpawnRegionForVillager(VillagerEntity villager) {
        return getNearestSpawnRegion(villager.getBlockPos());
    }

    public void addPointOfInterest(BlockPos center, BlockPos poi) {
        SpawnRegion region = spawnRegions.get(center);
        if (region != null) {
            region.addPointOfInterest(poi);
        }
    }

    public Collection<SpawnRegion> getSpawnRegions() {
        return spawnRegions.values();
    }

    public Collection<VillagerAI> getActiveVillagers() {
        return villagerAIs.values();
    }

    public VillagerEntity findNearbyVillagerByName(ServerPlayerEntity player, String name) {
        for (Map.Entry<UUID, VillagerAI> entry : villagerAIs.entrySet()) {
            VillagerAI ai = entry.getValue();
            VillagerEntity villager = ai.getVillager();
            if (villager.getName().getString().equalsIgnoreCase(name) && villager.squaredDistanceTo(player) < 100) {
                return villager;
            }
        }
        return null;
    }

    public void handleVillagerInteraction(ServerPlayerEntity player, VillagerEntity villager) {
        VillagerDialogue dialogue = villagerDialogues.get(villager.getUuid());
        if (dialogue != null) {
            dialogue.generatePlayerInteraction(player, "general_conversation").thenAccept(response -> {
                Map<String, String> parsed = parseResponse(response);
                String greeting = parsed.getOrDefault("GREETING", "");
                String dialogueText = parsed.getOrDefault("DIALOGUE", "");
                String actions = parsed.getOrDefault("ACTIONS", "");
                if (!greeting.isEmpty()) {
                    player.sendMessage(Text.literal("§6" + villager.getName().getString() + ": §f" + greeting), false);
                }
                player.sendMessage(Text.literal("§6" + villager.getName().getString() + ": §f" + dialogueText), false);
                if (!actions.isEmpty()) {
                    player.sendMessage(Text.literal("§7* " + villager.getName().getString() + " " + actions), false);
                }
            }).exceptionally(e -> {
                LOGGER.error("Error generating dialogue", e);
                player.sendMessage(Text.literal(villager.getName().getString() + ": ..."), false);
                return null;
            });
        }
    }

    public VillageStats getVillageStats(BlockPos pos) {
        BlockPos center = getNearestVillageCenter(pos);
        return center != null ? villageStats.get(center) : null;
    }

    public List<CulturalEvent> getCurrentEvents(BlockPos pos, ServerWorld world) {
        SpawnRegion region = getNearestSpawnRegion(pos);
        if (region == null) return Collections.emptyList();
        return culturalEvents.values().stream().filter(event -> event.startTime + event.duration > world.getTimeOfDay()).collect(Collectors.toList());
    }

    private BlockPos getNearestVillageCenter(BlockPos pos) {
        return spawnRegions.keySet().stream().min(Comparator.comparingDouble(center -> center.getSquaredDistance(pos))).orElse(null);
    }

    public void addVillageStats(BlockPos center, String culture) {
        villageStats.put(center, new VillageStats(center, culture));
    }

    public void updateVillageStats(BlockPos center, VillageStats stats) {
        villageStats.put(center, stats);
        broadcastVillageInfoUpdate(center);
    }

    public static class CulturalEvent {
        public final String name;
        public final String description;
        public final int duration;
        public final long startTime;

        public CulturalEvent(String name, String description, int duration, long startTime) {
            this.name = name;
            this.description = description;
            this.duration = duration;
            this.startTime = startTime;
        }
    }

    public static class VillageStats {
        public int prosperity = 50;
        public int safety = 50;
        public BlockPos center;
        public String culture;
        public UUID leaderUUID;

        public VillageStats(BlockPos center, String culture) {
            this.center = center;
            this.culture = culture;
        }
    }

    private void positionVillagersForInteraction(VillagerEntity v1, VillagerEntity v2) {
        double dx = v2.getX() - v1.getX();
        double dz = v2.getZ() - v1.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        v1.setYaw(yaw);
        v2.setYaw(yaw + 180.0F);
        if (v1.squaredDistanceTo(v2) > 4.0) {
            v1.getNavigation().startMovingTo(v2.getX() + dx * 0.3, v2.getY(), v2.getZ() + dz * 0.3, 0.5D);
        }
    }

    private String getLocationContext(BlockPos pos) {
        SpawnRegion region = getNearestSpawnRegion(pos);
        if (region == null) return "village outskirts";
        int distanceToCenter = (int) Math.sqrt(pos.getSquaredDistance(region.getCenter()));
        if (distanceToCenter < 5) return "village center";
        if (distanceToCenter < 15) return "village interior";
        return "village outskirts";
    }

    private String getTimeContext(long timeOfDay) {
        long adjustedTime = timeOfDay % 24000;
        if (adjustedTime < 6000) return "early morning";
        if (adjustedTime < 12000) return "midday";
        if (adjustedTime < 18000) return "evening";
        return "night";
    }

    private String getSharedHistory(VillagerAI v1, VillagerAI v2) {
        Relationship relationship = getRelationship(v1.getVillager().getUuid(), v2.getVillager().getUuid());
        if (relationship == null) return "first meeting";
        return relationship.toString();
    }

    private String getNearbyActivities(ServerWorld world, BlockPos pos) {
        List<String> activities = new ArrayList<>();
        int searchRadius = 10;
        List<VillagerAI> nearbyVillagers = villagerAIs.values().stream().filter(ai -> ai.getVillager().getBlockPos().isWithinDistance(pos, searchRadius)).collect(Collectors.toList());
        for (VillagerAI ai : nearbyVillagers) {
            activities.add(ai.getCurrentActivity());
        }
        return activities.stream().distinct().collect(Collectors.joining(", "));
    }

    private void applyInteractionEffects(ServerWorld world, VillagerEntity v1, VillagerEntity v2, Map<String, String> interaction) {
        String outcome = interaction.getOrDefault("OUTCOME", "neutral");
        double x = (v1.getX() + v2.getX()) / 2;
        double y = (v1.getY() + v2.getY()) / 2 + 1;
        double z = (v1.getZ() + v2.getZ()) / 2;
        if (outcome.contains("positive") || outcome.contains("friend")) {
            world.spawnParticles(ParticleTypes.HEART, x, y, z, 2, 0.2, 0.2, 0.2, 0.02);
        } else if (outcome.contains("negative") || outcome.contains("rival")) {
            world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, x, y, z, 2, 0.2, 0.2, 0.2, 0.02);
        }
    }

    private void updateRelationships(VillagerAI v1, VillagerAI v2, Map<String, String> interaction) {
        String outcome = interaction.getOrDefault("OUTCOME", "neutral");
        VillagerAI.RelationshipType type;
        if (outcome.contains("positive") || outcome.contains("friend")) {
            type = VillagerAI.RelationshipType.FRIEND;
        } else if (outcome.contains("negative") || outcome.contains("rival")) {
            type = VillagerAI.RelationshipType.RIVAL;
        } else {
            type = VillagerAI.RelationshipType.NEUTRAL;
        }
        v1.addRelationship(v2.getVillager().getUuid(), type);
        v2.addRelationship(v1.getVillager().getUuid(), type);
    }

    private void displayInteraction(ServerWorld world, VillagerEntity v1, VillagerEntity v2, Map<String, String> interaction) {
        String dialogue = interaction.getOrDefault("DIALOGUE", "...");
        String actions = interaction.getOrDefault("ACTIONS", "");
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(v1) < 100) {
                player.sendMessage(Text.of("§6" + v1.getName().getString() + ": §f" + dialogue), false);
                if (!actions.isEmpty()) {
                    player.sendMessage(Text.of("§7* " + v1.getName().getString() + " " + actions), false);
                }
            }
        }
    }

    private void updateVillagerStates(VillagerAI v1, VillagerAI v2, Map<String, String> interaction) {
        String moodChange = interaction.getOrDefault("MOOD_CHANGE", "neutral");
        int moodDelta = 0;
        if (moodChange.contains("positive")) {
            moodDelta = 5;
        } else if (moodChange.contains("negative")) {
            moodDelta = -3;
        }
        if (moodDelta != 0) {
            v1.adjustHappiness(moodDelta);
            v2.adjustHappiness(moodDelta);
        }
        String topic = interaction.getOrDefault("TOPIC", "");
        if (topic.contains("work")) {
            v1.updateActivity("discussing_work");
            v2.updateActivity("discussing_work");
        } else {
            v1.updateActivity("socializing");
            v2.updateActivity("socializing");
        }
    }

    public Map<String, String> parseInteractionResponse(String response) {
        return parseResponse(response); // Reuse the existing private parseResponse method
    }

    private Map<String, String> parseResponse(String response) {
        Map<String, String> result = new HashMap<>();
        if (response == null) return result;
        try {
            for (String line : response.split("\n")) {
                int separatorIndex = line.indexOf(": ");
                if (separatorIndex > 0) {
                    String key = line.substring(0, separatorIndex).trim();
                    String value = line.substring(separatorIndex + 2).trim();
                    result.put(key, value);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing response", e);
        }
        return result;
    }

    public void broadcastVillageInfoUpdate(BlockPos center) {
        VillageStats stats = villageStats.get(center);
        if (stats == null || server == null) return;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getWorld() == world && player.getBlockPos().getSquaredDistance(center) < 16384) {
                VillagesNetwork.sendVillageInfoToClient(player, stats);
            }
        }
        LOGGER.debug("Broadcast village info update for {} village at {}", stats.culture, center);
    }

    public void broadcastEventNotification(String title, String description, BlockPos center, int radius) {
        if (server == null || world == null) return;
        EventNotificationManager.getInstance().broadcastNotificationInRadius((ServerWorld) world, center, radius, title, description, 200);
        LOGGER.debug("Broadcast event notification '{}' at {}", title, center);
    }

    public void broadcastVillagerAIUpdate(VillagerEntity villager, String activity) {
        if (server == null || villager == null) return;
        VillagesNetwork.broadcastVillagerAIState(villager, activity, villager.getBlockPos());
        LOGGER.debug("Broadcast villager AI update for {}: {}", villager.getName().getString(), activity);
    }

    private void processVillageRelationships(ServerWorld world) {
        VillageInfluenceManager vim = VillageInfluenceManager.getInstance();
        VillageRelationshipEffects relationshipEffects = VillageRelationshipEffects.getInstance();
        List<BlockPos> villageCenters = new ArrayList<>(spawnRegions.keySet());
        for (int i = 0; i < villageCenters.size(); i++) {
            for (int j = i + 1; j < villageCenters.size(); j++) {
                BlockPos village1 = villageCenters.get(i);
                BlockPos village2 = villageCenters.get(j);
                relationshipEffects.processVillageRelationships(village1, village2);
            }
        }
    }

    /**
     * Synchronize village data between server and clients
     * Called periodically from the ServerTickHandler
     * @param server The Minecraft server
     */
    public void syncVillageData(MinecraftServer server) {
        if (server == null) return;
        
        villageLock.readLock().lock();
        try {
            // Sync all village regions data to clients
            for (Map.Entry<BlockPos, SpawnRegion> entry : spawnRegions.entrySet()) {
                BlockPos center = entry.getKey();
                SpawnRegion region = entry.getValue();
                VillageStats stats = villageStats.get(center);
                
                if (stats == null) continue;
                
                // Get all villagers in this region
                List<VillagerEntity> regionVillagers = new ArrayList<>();
                for (VillagerAI ai : villagerAIs.values()) {
                    VillagerEntity villager = ai.getVillager();
                    if (villager != null && villager.isAlive() && 
                        region.isWithinRegion(villager.getBlockPos())) {
                        regionVillagers.add(villager);
                    }
                }
                
                // Calculate region leadership if needed
                if (stats.leaderUUID == null && !regionVillagers.isEmpty()) {
                    determineVillageLeader(stats, regionVillagers);
                }
                
                // Broadcast stats to nearby players
                broadcastVillageInfoUpdate(center);
            }
            
            // Sync all active cultural events
            culturalEventLock.readLock().lock();
            try {
                for (CulturalEvent event : culturalEvents.values()) {
                    // Broadcast event info to nearby players
                    for (SpawnRegion region : spawnRegions.values()) {
                        broadcastEventNotification(
                            event.name, event.description, region.getCenter(), region.getRadius() * 2);
                    }
                }
            } finally {
                culturalEventLock.readLock().unlock();
            }
        } finally {
            villageLock.readLock().unlock();
        }
        
        LOGGER.debug("Synchronized village data across the server");
    }

    /**
     * Determine which villager should be the village leader
     * @param stats The village stats to update
     * @param villagers List of villagers in the region
     */
    private void determineVillageLeader(VillageStats stats, List<VillagerEntity> villagers) {
        // Simple algorithm: pick the villager with the most relationships
        UUID bestCandidate = null;
        int maxRelationships = -1;
        
        for (VillagerEntity villager : villagers) {
            UUID id = villager.getUuid();
            VillagerAI ai = getVillagerAI(id);
            
            if (ai != null) {
                int relationshipCount = getRelationshipCount(id);
                if (relationshipCount > maxRelationships) {
                    maxRelationships = relationshipCount;
                    bestCandidate = id;
                }
            }
        }
        
        // Set the village leader
        if (bestCandidate != null) {
            stats.leaderUUID = bestCandidate;
            
            // Notify the villager they're now a leader
            VillagerAI leaderAI = getVillagerAI(bestCandidate);
            if (leaderAI != null) {
                Map<String, Object> params = new HashMap<>();
                params.put("leadershipRole", "village_leader");
                leaderAI.startBehavior("leadership", 1200, params);
                
                VillagerEntity leader = leaderAI.getVillager();
                if (leader != null && leader.getWorld() instanceof ServerWorld) {
                    ServerWorld world = (ServerWorld) leader.getWorld();
                    world.spawnParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        leader.getX(), leader.getY() + 1, leader.getZ(),
                        10, 0.5, 0.5, 0.5, 0.05
                    );
                }
            }
        }
    }

    /**
     * Get the villagers in a spawn region
     * @param region The spawn region
     * @return A list of villagers in the region
     */
    public List<VillagerEntity> getVillagersInRegion(SpawnRegion region) {
        if (region == null || server == null) {
            return Collections.emptyList();
        }
        List<VillagerEntity> villagersInRegion = new ArrayList<>();
        // Iterate through all server worlds to find villagers
        for (ServerWorld world : server.getWorlds()) {
             // Check if the world dimension matches the region, if necessary (using region.canSpawnInDimension)
             // Efficiently get entities in the region's bounding box
             Box regionBox = new Box(region.getCenter()).expand(region.getRadius());
             List<VillagerEntity> worldVillagers = world.getEntitiesByClass(VillagerEntity.class, regionBox, v -> region.isWithinRegion(v.getBlockPos()));
             villagersInRegion.addAll(worldVillagers);
        }
         return villagersInRegion;
     }
    
    public void registerBiomeCultureAssociation(java.util.function.Predicate<net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext> selector, String cultureType) {
        biomeCultureAssociations.put(selector, cultureType);
    }
}
