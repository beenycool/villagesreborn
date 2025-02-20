package com.beeny.village;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.MinecraftServer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import com.beeny.ai.LLMService;

public class VillagerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final VillagerManager INSTANCE = new VillagerManager();
    
    private final Map<UUID, VillagerAI> villagerAIs;
    private final Map<BlockPos, SpawnRegion> spawnRegions;
    private final NameGenerator nameGenerator;
    private final Random random;
    private final Map<UUID, Relationship> relationships = new ConcurrentHashMap<>();
    private World world;
    private String culture;

    private VillagerManager() {
        this.villagerAIs = new ConcurrentHashMap<>();
        this.spawnRegions = new HashMap<>();
        this.nameGenerator = new NameGenerator();
        this.random = new Random();
        this.world = null; // Initialize world to null
    }

    public void addRelationship(UUID villager1, UUID villager2, String type) {
        Relationship relationship = new Relationship(villager1, villager2, type);
        relationships.put(villager1, relationship);
        relationships.put(villager2, relationship);
    }

    public Relationship getRelationship(UUID villager1, UUID villager2) {
        return relationships.get(villager1);
    }

    public int getRelationshipCount(UUID villagerUUID) {
        return (int) relationships.entrySet().stream()
            .filter(entry -> entry.getValue().getVillager1().equals(villagerUUID) ||
                           entry.getValue().getVillager2().equals(villagerUUID))
            .count();
    }

    public VillagerManager(World world) {
        this.world = world;
        this.villagerAIs = new ConcurrentHashMap<>();
        this.spawnRegions = new HashMap<>();
        this.nameGenerator = new NameGenerator();
        this.random = new Random();
    }

    public void updateVillagerActivities(ServerWorld world) {
        MoodManager moodManager = MoodManager.getInstance();
        for (VillagerAI ai : villagerAIs.values()) {
            ai.updateActivityBasedOnTime(world);
            moodManager.updateVillagerMood(ai, world);
        }
    }

    public void registerTickEvent(MinecraftServer server) {
        ServerTickEvents.END_SERVER_TICK.register(server1 -> {
            for (ServerWorld world : server.getWorlds()) {
                updateVillagerActivities(world);
            }
        });
    }

    public static VillagerManager getInstance() {
        return INSTANCE;
    }

    public void setCulture(String culture) {
        this.culture = culture;
    }

    public String getCulture() {
        return culture;
    }

    public void registerSpawnRegion(BlockPos center, int radius, String culture) {
        SpawnRegion region = new SpawnRegion(center, radius, culture);
        spawnRegions.put(center, region);
        LOGGER.info("Registered new {} village at {}", culture, center);
    }

    public void onVillagerSpawn(VillagerEntity villager, World world) {
        if (!(world instanceof ServerWorld)) return;

        // Find nearest spawn region
        SpawnRegion region = getNearestSpawnRegion(villager.getBlockPos());
        if (region == null) {
            LOGGER.debug("Villager spawned outside any known region: {}", villager.getBlockPos());
            return;
        }

        // Generate personality and name
        String personality = generatePersonality(region.getCulture().toString());
        String name = nameGenerator.generateName(region.getCulture().toString(), villager.getVillagerData().getProfession().toString());

        // Create and store AI controller
        VillagerAI ai = new VillagerAI(villager, personality);
        villagerAIs.put(villager.getUuid(), ai);

        // Set custom name
        villager.setCustomName(net.minecraft.text.Text.of(name));
        villager.setCustomNameVisible(true);

        LOGGER.info("Initialized new villager: {} ({}) in {} village", 
            name, personality, region.getCulture());
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

    private String generatePersonality(String culture) {
        try {
            String prompt = String.format(
                "Generate a personality for a Minecraft villager from %s culture. " +
                "Provide a brief description in 10 words or less.", 
                culture
            );

            String response = LLMService.getInstance()
                .generateResponse(prompt)
                .get(10, TimeUnit.SECONDS)
                .trim();

            return response;
        } catch (Exception e) {
            LOGGER.error("Error generating villager personality", e);
            return "default personality";
        }
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
            player.sendMessage(Text.literal("Talking to " + villager.getName().getString()), false);
        }
    }
}
