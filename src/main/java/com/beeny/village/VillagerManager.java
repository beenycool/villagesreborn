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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.beeny.ai.LLMService;
import com.beeny.util.NameGenerator;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class VillagerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final VillagerManager INSTANCE = new VillagerManager();
    
    private final Map<UUID, VillagerAI> villagerAIs;
    private final Map<BlockPos, SpawnRegion> spawnRegions;
    private final Map<String, CulturalEvent> culturalEvents;
    private final Map<BlockPos, VillageStats> villageStats;
    private final Map<UUID, Map<UUID, Relationship>> relationships;
    private final NameGenerator nameGenerator;
    private final Random random;
    private ServerWorld world;
    private String currentCulture;

    private VillagerManager() {
        this.villagerAIs = new ConcurrentHashMap<>();
        this.spawnRegions = new HashMap<>();
        this.culturalEvents = new HashMap<>();
        this.villageStats = new ConcurrentHashMap<>();
        this.relationships = new ConcurrentHashMap<>();
        this.nameGenerator = new NameGenerator();
        this.random = new Random();
    }

    public static VillagerManager getInstance() {
        return INSTANCE;
    }

    public void registerSpawnRegion(BlockPos center, int radius, String culture) {
        SpawnRegion region = new SpawnRegion(center, radius, culture);
        spawnRegions.put(center, region);
        addVillageStats(center, culture);
        LOGGER.info("Registered new {} village at {}", culture, center);
    }

    public void addRelationship(UUID villager1, UUID villager2, VillagerAI.RelationshipType type) {
        relationships.computeIfAbsent(villager1, k -> new ConcurrentHashMap<>())
            .put(villager2, new Relationship(type));
        relationships.computeIfAbsent(villager2, k -> new ConcurrentHashMap<>())
            .put(villager1, new Relationship(type));
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
        long timeOfDay = world.getTimeOfDay();
        MoodManager moodManager = MoodManager.getInstance();
        
        // Update cultural events
        updateCulturalEvents(world);
        
        // Update village stats
        updateVillageStats(world);
        
        // Update individual villagers
        for (VillagerAI ai : villagerAIs.values()) {
            ai.updateActivityBasedOnTime(world);
            moodManager.updateVillagerMood(ai, world);
            
            // Social interaction chance
            if (random.nextFloat() < 0.1f) { // 10% chance per tick
                generateRandomSocialInteraction(ai, world);
            }
        }
    }

    private void generateRandomSocialInteraction(VillagerAI villager1, ServerWorld world) {
        List<VillagerAI> nearbyVillagers = villagerAIs.values().stream()
            .filter(v -> v != villager1 &&
                        v.getVillager().squaredDistanceTo(villager1.getVillager()) < 100)
            .collect(Collectors.toList());
            
        if (!nearbyVillagers.isEmpty()) {
            VillagerAI villager2 = nearbyVillagers.get(random.nextInt(nearbyVillagers.size()));
            
            // Make villagers face each other
            VillagerEntity v1 = villager1.getVillager();
            VillagerEntity v2 = villager2.getVillager();
            
            double dx = v2.getX() - v1.getX();
            double dz = v2.getZ() - v1.getZ();
            float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
            v1.setYaw(yaw);
            v2.setYaw(yaw + 180.0F);
            
            // Move slightly closer if needed
            if (v1.squaredDistanceTo(v2) > 4.0) {
                v1.getNavigation().startMovingTo(
                    v2.getX() + dx * 0.3,
                    v2.getY(),
                    v2.getZ() + dz * 0.3,
                    0.5D
                );
            }
            
            // Let the LLM determine their relationship
            villager1.determineRelationshipDynamically(villager2.getVillager())
                .thenAccept(type -> {
                    villager1.addRelationship(villager2.getVillager().getUuid(), type);
                    villager2.addRelationship(villager1.getVillager().getUuid(), type);
                    
                    // Visual effects based on relationship type
                    addInteractionEffects(world, v1, v2, type);
                    
                    // Generate interaction dialogue
                    String situation = String.format("Meeting %s for the first time",
                        villager2.getVillager().getName().getString());
                    villager1.generateDialogue(situation, villager2.getVillager())
                        .thenAccept(dialogue -> {
                            // Show dialogue as floating text
                            world.getPlayers().forEach(player -> {
                                if (player.squaredDistanceTo(v1) < 100) {
                                    player.sendMessage(Text.of("§6" + v1.getName().getString() + ": §f" + dialogue), false);
                                }
                            });
                        });
                    
                    // Update activities to reflect interaction
                    villager1.updateActivity("socializing");
                    villager2.updateActivity("socializing");
                });
        }
    }
    
    private void addInteractionEffects(ServerWorld world, VillagerEntity v1, VillagerEntity v2, VillagerAI.RelationshipType type) {
        double x = (v1.getX() + v2.getX()) / 2;
        double y = (v1.getY() + v2.getY()) / 2 + 1;
        double z = (v1.getZ() + v2.getZ()) / 2;
        
        switch(type) {
            case FRIEND:
                world.spawnParticles(ParticleTypes.HEART, x, y, z, 3, 0.2, 0.2, 0.2, 0.02);
                break;
            case FAMILY:
                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 5, 0.2, 0.2, 0.2, 0.02);
                world.spawnParticles(ParticleTypes.HEART, x, y, z, 2, 0.2, 0.2, 0.2, 0.02);
                break;
            case RIVAL:
                world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, x, y, z, 3, 0.2, 0.2, 0.2, 0.02);
                break;
        }
    }

    private void updateCulturalEvents(ServerWorld world) {
        long timeOfDay = world.getTimeOfDay();
        
        // Clean up expired events
        culturalEvents.entrySet().removeIf(entry -> 
            timeOfDay > entry.getValue().startTime + entry.getValue().duration);
            
        // Generate new events based on culture
        if (culturalEvents.isEmpty() && random.nextFloat() < 0.001f) { // 0.1% chance per tick
            generateCulturalEvent(world);
        }
    }

    private void updateVillageStats(ServerWorld world) {
        for (Map.Entry<BlockPos, VillageStats> entry : villageStats.entrySet()) {
            VillageStats stats = entry.getValue();
            SpawnRegion region = spawnRegions.get(entry.getKey());
            
            if (region != null) {
                StringBuilder prompt = new StringBuilder()
                    .append("Analyze this village's current state:\n")
                    .append("- Culture: ").append(stats.culture).append("\n")
                    .append("- Building count: ").append(region.getCulturalStructures().size()).append("\n")
                    .append("- Population: ").append(getVillagerCount(region)).append("\n")
                    .append("- Recent events: ").append(getRecentEvents(region, world)).append("\n")
                    .append("\nProvide village stats in format:\n")
                    .append("PROSPERITY: (number 0-100)\n")
                    .append("SAFETY: (number 0-100)");

                LLMService.getInstance()
                    .generateResponse(prompt.toString())
                    .thenAccept(response -> {
                        Map<String, String> values = parseResponse(response);
                        stats.prosperity = Integer.parseInt(values.getOrDefault("PROSPERITY", "50"));
                        stats.safety = Integer.parseInt(values.getOrDefault("SAFETY", "50"));
                    })
                    .exceptionally(e -> {
                        LOGGER.error("Error updating village stats", e);
                        return null;
                    });
            }
        }
    }

    private int getVillagerCount(SpawnRegion region) {
        return (int) villagerAIs.values().stream()
            .filter(ai -> region.isWithinRegion(ai.getVillager().getBlockPos()))
            .count();
    }

    private String getRecentEvents(SpawnRegion region, ServerWorld world) {
        return culturalEvents.values().stream()
            .filter(event -> event.startTime + event.duration > world.getTimeOfDay())
            .map(event -> event.name)
            .collect(Collectors.joining(", "));
    }

    private void generateCulturalEvent(ServerWorld world) {
        String culture = getCulture();
        if (culture == null) return;
        
        String prompt = String.format(
            "You are creating a cultural event for a %s-themed Minecraft village.\n" +
            "Design a unique festival or celebration that reflects this culture.\n" +
            "Format your response as:\n" +
            "NAME: (event name)\n" +
            "DURATION: (number of minecraft days)\n" +
            "DESCRIPTION: (brief description)\n" +
            "ACTIVITIES: (list of 2-3 villager activities during event)",
            culture
        );

        LLMService.getInstance()
            .generateResponse(prompt)
            .thenAccept(response -> {
                Map<String, String> eventDetails = parseResponse(response);
                CulturalEvent event = new CulturalEvent(
                    eventDetails.get("NAME"),
                    eventDetails.get("DESCRIPTION"),
                    Integer.parseInt(eventDetails.getOrDefault("DURATION", "1")) * 24000,
                    world.getTimeOfDay()
                );
                
                culturalEvents.put(event.name, event);
                
                // Generate event-specific behaviors for villagers
                List<String> activities = Arrays.asList(
                    eventDetails.getOrDefault("ACTIVITIES", "celebrate").split(", ")
                );
                
                for (VillagerAI ai : villagerAIs.values()) {
                    String activity = activities.get(world.getRandom().nextInt(activities.size()));
                    ai.updateActivity("event_" + activity);
                }
                
                LOGGER.info("Generated cultural event: {} for {} village", event.name, culture);
            })
            .exceptionally(e -> {
                LOGGER.error("Error generating cultural event", e);
                return null;
            });
    }

    private Map<String, String> parseResponse(String response) {
        return Arrays.stream(response.split("\n"))
            .map(line -> line.split(": ", 2))
            .filter(parts -> parts.length == 2)
            .collect(Collectors.toMap(
                parts -> parts[0],
                parts -> parts[1].trim()
            ));
    }

    public void registerTickEvent(MinecraftServer server) {
        ServerTickEvents.END_SERVER_TICK.register(server1 -> {
            for (ServerWorld world : server1.getWorlds()) {
                updateVillagerActivities(world);
            }
        });
    }

    public void setCulture(String culture) {
        this.currentCulture = culture;
    }

    public String getCulture() {
        return currentCulture;
    }

    public void onVillagerSpawn(VillagerEntity villager, ServerWorld world) {
        this.world = world;

        SpawnRegion region = getNearestSpawnRegion(villager.getBlockPos());
        if (region == null) {
            LOGGER.debug("Villager spawned outside any known region: {}", villager.getBlockPos());
            return;
        }

        // Generate personality and name asynchronously
        CompletableFuture.allOf(
            CompletableFuture.runAsync(() -> {
                String personality = generatePersonality(region.getCulture());
                VillagerAI ai = new VillagerAI(villager, personality);
                villagerAIs.put(villager.getUuid(), ai);

                // Generate initial thoughts about their role
                String situation = String.format("Starting life as a %s in a %s village", 
                    villager.getVillagerData().getProfession(),
                    region.getCulture()
                );
                ai.generateBehavior(situation);
            }),
            nameGenerator.generateName(region.getCulture(), 
                villager.getVillagerData().getProfession().toString())
                .thenAccept(name -> {
                    villager.setCustomName(Text.of(name));
                    villager.setCustomNameVisible(true);
                    LOGGER.info("Initialized new villager: {} in {} village", 
                        name, region.getCulture());
                })
        ).exceptionally(e -> {
            LOGGER.error("Error initializing villager", e);
            return null;
        });
    }

    private String generatePersonality(String culture) {
        String prompt = String.format(
            "Create a unique personality for a villager from %s culture.\n" +
            "Consider their cultural values, traditions, and way of life.\n" +
            "Describe their personality in 5-7 words.",
            culture
        );

        try {
            return LLMService.getInstance()
                .generateResponse(prompt)
                .get(10, TimeUnit.SECONDS)
                .trim();
        } catch (Exception e) {
            LOGGER.error("Error generating villager personality", e);
            return "friendly local villager";
        }
    }

    public void removeVillager(UUID uuid) {
        villagerAIs.remove(uuid);
    }

    public VillagerAI getVillagerAI(UUID uuid) {
        return villagerAIs.get(uuid);
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
            if (villager.getName().getString().equalsIgnoreCase(name) && 
                villager.squaredDistanceTo(player) < 100) {
                return villager;
            }
        }
        return null;
    }

    public void handleVillagerInteraction(ServerPlayerEntity player, VillagerEntity villager) {
        VillagerAI villagerAI = villagerAIs.get(villager.getUuid());
        if (villagerAI != null) {
            String situation = String.format("Player %s approaches to talk", 
                player.getName().getString());
                
            villagerAI.generateDialogue(situation, null)
                .thenAccept(dialogue -> {
                    player.sendMessage(Text.literal(
                        villager.getName().getString() + ": " + dialogue
                    ), false);
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
        
        return culturalEvents.values().stream()
            .filter(event -> event.startTime + event.duration > world.getTimeOfDay())
            .collect(Collectors.toList());
    }
    
    private BlockPos getNearestVillageCenter(BlockPos pos) {
        return spawnRegions.keySet().stream()
            .min(Comparator.comparingDouble(center -> 
                center.getSquaredDistance(pos)))
            .orElse(null);
    }
    
    public void addVillageStats(BlockPos center, String culture) {
        villageStats.put(center, new VillageStats(center, culture));
    }

    public void updateVillageStats(BlockPos center, VillageStats stats) {
        villageStats.put(center, stats);
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
}
