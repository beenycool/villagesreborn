package com.beeny;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import com.beeny.commands.ModCommands;
import com.beeny.event.VillagerEvents;
import com.beeny.village.VillageCraftingManager;
import com.beeny.village.Culture;
import com.beeny.village.VillagerManager;
import com.beeny.village.SpawnRegion;
import com.beeny.village.VillageEvent;
import com.beeny.village.event.CulturalEventSystem;
import com.beeny.village.event.PlayerDataManager;
import com.beeny.setup.SystemSpecs;
import com.beeny.setup.LLMConfig;
import com.beeny.worldgen.VillagesRebornStructures;
import com.beeny.network.VillageCraftingNetwork;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Villagesreborn implements ModInitializer {
    public static final String MOD_ID = "villagesreborn";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static SystemSpecs systemSpecs;
    private static LLMConfig llmConfig;
    private static Villagesreborn INSTANCE;
    private final Map<BlockPos, Culture> villageMap = new ConcurrentHashMap<>();
    private final Map<BlockPos, Integer> villageRadiusMap = new ConcurrentHashMap<>();
    private final Map<BlockPos, Integer> villageEventsMap = new ConcurrentHashMap<>();
    private static MinecraftServer serverInstance;
    private static final Identifier ROMAN_HOUSE = new Identifier(MOD_ID, "roman/house");
    private static final Identifier ROMAN_FORUM = new Identifier(MOD_ID, "roman/forum");
    private static final Identifier EGYPTIAN_HOUSE = new Identifier(MOD_ID, "egyptian/house");
    private static final Identifier EGYPTIAN_TEMPLE = new Identifier(MOD_ID, "egyptian/temple");
    private static final Identifier VICTORIAN_HOUSE = new Identifier(MOD_ID, "victorian/house");
    private static final Identifier VICTORIAN_SQUARE = new Identifier(MOD_ID, "victorian/square");
    private static final Identifier NYC_APARTMENT = new Identifier(MOD_ID, "nyc/apartment");
    private static final Identifier NYC_SKYSCRAPER = new Identifier(MOD_ID, "nyc/skyscraper");

    private final List<Float> tickTimes = new ArrayList<>();
    private int pendingNetworkRequests = 0;
    private final Map<UUID, Map<String, Integer>> playerReputation = new ConcurrentHashMap<>();

    public static Villagesreborn getInstance() {
        return INSTANCE;
    }

    public static MinecraftServer getServerInstance() {
        return serverInstance;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Villages Reborn mod");
        INSTANCE = this;

        systemSpecs = new SystemSpecs();
        systemSpecs.analyzeSystem();

        llmConfig = new LLMConfig();
        llmConfig.load();
        llmConfig.initialize(systemSpecs);

        VillagerManager.getInstance();
        VillageCraftingManager.getInstance();

        // Initialize our tick handlers for villager behavior execution
        VillagerWorldTickHandler.init();
        ServerTickHandler.init();

        registerNetworking();
        CulturalEventSystem.getInstance();
        ModCommands.register();
        VillagerEvents.registerTheftDetection();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverInstance = server;
            VillagerManager.getInstance().setServer(server);
            LOGGER.info("Villages Reborn detected server start.");
        });

        // Keep for backward compatibility but our new tick handlers will handle this
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (serverInstance != null) {
                // This will be handled by our specialized tick handlers
                // VillagerManager.getInstance().updateVillagerActivities(server.getOverworld());

                // Track tick time for performance monitoring
                long startTime = System.nanoTime();
                // Main update logic would go here
                long endTime = System.nanoTime();
                float tickTime = (endTime - startTime) / 1_000_000.0f; // Convert to milliseconds
                trackTickTime(tickTime);
            }
        });

        registerVillagerLoadCallback();
        registerPlayerDataEvents();
        registerStructureModifications();
        VillagesRebornStructures.registerStructures();

        LOGGER.info("Villages Reborn mod initialization complete");
    }

    private void trackTickTime(float tickTime) {
        tickTimes.add(tickTime);
        // Keep only the last 100 tick times
        if (tickTimes.size() > 100) {
            tickTimes.remove(0);
        }
    }

    public float getAverageTickTime() {
        if (tickTimes.isEmpty()) {
            return 0;
        }
        float sum = 0;
        for (float time : tickTimes) {
            sum += time;
        }
        return sum / tickTimes.size();
    }

    public int getTotalActiveEvents() {
        return getAllVillages().stream()
            .mapToInt(culture -> culture.getActiveEvents().size())
            .sum();
    }

    public int getPendingNetworkRequests() {
        return pendingNetworkRequests;
    }

    public void incrementPendingNetworkRequests() {
        pendingNetworkRequests++;
    }

    public void decrementPendingNetworkRequests() {
        if (pendingNetworkRequests > 0) {
            pendingNetworkRequests--;
        }
    }

    public int getPlayerReputation(UUID playerUuid, String cultureId) {
        return playerReputation
            .getOrDefault(playerUuid, new HashMap<>())
            .getOrDefault(cultureId, 0);
    }

    public void updatePlayerReputation(UUID playerUuid, String cultureId, int change) {
        Map<String, Integer> reputations = playerReputation.computeIfAbsent(playerUuid, k -> new HashMap<>());
        int currentRep = reputations.getOrDefault(cultureId, 0);
        reputations.put(cultureId, Math.max(-100, Math.min(100, currentRep + change)));
    }

    private void registerNetworking() {
        LOGGER.info("Registering networking handlers");
        VillageCraftingNetwork.register();
    }

    private void registerVillagerLoadCallback() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof VillagerEntity villager && !world.isClient()) {
                if (VillagerManager.getInstance().getVillagerAI(villager.getUuid()) == null) {
                    LOGGER.debug("Detected Villager load: {}", villager.getUuid());
                    if (serverInstance != null) {
                        serverInstance.execute(() -> {
                            VillagerManager.getInstance().onVillagerSpawn(villager, (ServerWorld) world);
                        });
                    } else {
                        LOGGER.warn("Server instance not available during villager load for {}", villager.getUuid());
                    }
                }
            }
        });
        LOGGER.info("Registered Villager load callback.");
    }

    private void registerPlayerDataEvents() {
        LOGGER.info("Registering player data event handlers");
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            LOGGER.debug("Player joined: {}", player.getName().getString());
            server.execute(() -> {
                PlayerDataManager.getInstance().loadPlayerData(player);
            });
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            LOGGER.debug("Player disconnected: {}", player.getName().getString());
            server.execute(() -> {
                PlayerDataManager.getInstance().savePlayerData(player);
            });
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Saving all player data before server stop");
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                PlayerDataManager.getInstance().savePlayerData(player);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 6000 == 0) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    PlayerDataManager.getInstance().cleanupPlayerData(player);
                }
            }
        });
        LOGGER.info("Player data event handlers registered successfully");
    }

    private void registerStructureModifications() {
        LOGGER.info("Registering structure modifications...");
        registerCultureStructures(
            "plains",
            BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.MEADOW, BiomeKeys.SUNFLOWER_PLAINS),
            "roman",
            Map.of(
                ROMAN_HOUSE, 15,
                ROMAN_FORUM, 8
            )
        );
        registerCultureStructures(
            "desert",
            BiomeSelectors.includeByKey(BiomeKeys.DESERT, BiomeKeys.BADLANDS),
            "egyptian",
            Map.of(
                EGYPTIAN_HOUSE, 15,
                EGYPTIAN_TEMPLE, 7
            )
        );
        registerCultureStructures(
            "taiga",
            BiomeSelectors.includeByKey(BiomeKeys.TAIGA, BiomeKeys.OLD_GROWTH_PINE_TAIGA, BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA),
            "victorian",
            Map.of(
                VICTORIAN_HOUSE, 15,
                VICTORIAN_SQUARE, 8
            )
        );
        registerCultureStructures(
            "savanna",
            BiomeSelectors.includeByKey(BiomeKeys.SAVANNA, BiomeKeys.SAVANNA_PLATEAU, BiomeKeys.WINDSWEPT_SAVANNA),
            "nyc",
            Map.of(
                NYC_APARTMENT, 15,
                NYC_SKYSCRAPER, 7
            )
        );
        LOGGER.info("Structure modifications registration complete.");
    }

    private void registerCultureStructures(String vanillaType, net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext selector,
                                         String cultureType, Map<Identifier, Integer> structures) {
        Identifier housesPoolId = new Identifier("minecraft", "village/" + vanillaType + "/houses");
        RegistryKey<StructurePool> housesPoolKey = RegistryKey.of(RegistryKeys.TEMPLATE_POOL, housesPoolId);
        Identifier centerPoolId = new Identifier("minecraft", "village/" + vanillaType + "/town_centers");
        RegistryKey<StructurePool> centerPoolKey = RegistryKey.of(RegistryKeys.TEMPLATE_POOL, centerPoolId);

        // Update to use the correct BiomeSelectors method for 1.21.4
        VillagerManager.getInstance().registerBiomeCultureAssociation(selector, cultureType);

        net.fabricmc.fabric.api.structure.v1.FabricStructurePool.registerAddition(housesPoolId, builder -> {
            for (Map.Entry<Identifier, Integer> entry : structures.entrySet()) {
                if (entry.getKey().getPath().contains("forum") ||
                    entry.getKey().getPath().contains("temple") ||
                    entry.getKey().getPath().contains("square") ||
                    entry.getKey().getPath().contains("skyscraper")) {
                    continue;
                }
                LOGGER.info("Adding {} to {} village houses pool with weight {}",
                            entry.getKey(), vanillaType, entry.getValue());
                builder.element(entry.getKey().toString(), entry.getValue());
            }
        });
        net.fabricmc.fabric.api.structure.v1.FabricStructurePool.registerAddition(centerPoolId, builder -> {
            for (Map.Entry<Identifier, Integer> entry : structures.entrySet()) {
                if (entry.getKey().getPath().contains("forum") ||
                    entry.getKey().getPath().contains("temple") ||
                    entry.getKey().getPath().contains("square") ||
                    entry.getKey().getPath().contains("skyscraper")) {
                    LOGGER.info("Adding {} to {} village center pool with weight {}",
                                entry.getKey(), vanillaType, entry.getValue());
                    builder.element(entry.getKey().toString(), entry.getValue());
                }
            }
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Registered structure generation callback for {} culture in {} villages",
                       cultureType, vanillaType);
        });
    }

    public static SystemSpecs getSystemSpecs() {
        if (systemSpecs == null) {
            systemSpecs = new SystemSpecs();
            systemSpecs.analyzeSystem();
        }
        return systemSpecs;
    }

    public static LLMConfig getLLMConfig() {
        if (llmConfig == null) {
            llmConfig = new LLMConfig();
            llmConfig.load();
        }
        return llmConfig;
    }

    public void pingVillageInfo(PlayerEntity player) {
        LOGGER.info("Village info requested for player: " + player.getName().getString());
    }

    public Culture getNearestVillageCulture(BlockPos pos) {
        SpawnRegion region = VillagerManager.getInstance().getNearestSpawnRegion(pos);
        return region != null ? region.getCulture() : null;
    }

    public BlockPos getVillageCenterPos(BlockPos pos) {
        SpawnRegion region = VillagerManager.getInstance().getNearestSpawnRegion(pos);
        if (region != null && region.isWithinRegion(pos)) {
            return region.getCenter();
        }
        return null;
    }

    public int getVillageRadius(BlockPos pos) {
        SpawnRegion region = VillagerManager.getInstance().getNearestSpawnRegion(pos);
        if (region != null && region.isWithinRegion(pos)) {
            return region.getRadius();
        }
        return 0;
    }

    public int getActiveEventCount(BlockPos pos) {
        return VillageEvent.findEventsNear(pos, getVillageRadius(pos)).size();
    }

    public Collection<Culture> getAllVillages() {
        return VillagerManager.getInstance().getSpawnRegions().stream()
                .map(SpawnRegion::getCulture)
                .distinct()
                .collect(Collectors.toList());
    }

    @Deprecated
    public void registerVillage(BlockPos center, Culture culture, int radius) {
        VillagerManager.getInstance().registerSpawnRegion(center, radius, culture.getName());
    }

    @Deprecated
    public void updateVillageEvents(BlockPos center, int eventCount) {
    }
}