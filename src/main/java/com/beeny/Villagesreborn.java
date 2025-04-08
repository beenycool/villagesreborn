package com.beeny;

import com.beeny.village.VillagerWorldTickHandler;
import com.beeny.village.ServerTickHandler;
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
import com.beeny.village.event.CulturalEventSystem;
import com.beeny.village.event.PlayerDataManager;
import com.beeny.setup.SystemSpecs;
import com.beeny.config.VillagesConfig;
import com.beeny.ai.LLMService;
import com.beeny.worldgen.VillagesRebornStructures;
import com.beeny.network.VillageCraftingNetwork;
import com.beeny.network.VillagesNetwork;
import com.beeny.network.VillagesClientNetwork;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
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
import net.minecraft.structure.pool.StructurePool;

public class Villagesreborn implements ModInitializer {
    public static final String MOD_ID = "villagesreborn", VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static SystemSpecs systemSpecs;
    private static VillagesConfig villagesConfig;
    private static Villagesreborn INSTANCE;
    private final ConcurrentHashMap<BlockPos, Culture> villageMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BlockPos, Integer> villageRadiusMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BlockPos, Integer> villageEventsMap = new ConcurrentHashMap<>();
    private static MinecraftServer serverInstance;
    private static final Identifier ROMAN_HOUSE = Identifier.of(MOD_ID, "roman/house"),
            ROMAN_FORUM = Identifier.of(MOD_ID, "roman/forum"),
            EGYPTIAN_HOUSE = Identifier.of(MOD_ID, "egyptian/house"),
            EGYPTIAN_TEMPLE = Identifier.of(MOD_ID, "egyptian/temple"),
            VICTORIAN_HOUSE = Identifier.of(MOD_ID, "victorian/house"),
            VICTORIAN_SQUARE = Identifier.of(MOD_ID, "victorian/square"),
            NYC_APARTMENT = Identifier.of(MOD_ID, "nyc/apartment"),
            NYC_SKYSCRAPER = Identifier.of(MOD_ID, "nyc/skyscraper");

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
        villagesConfig = VillagesConfig.getInstance();
        villagesConfig.load();
        LLMService.getInstance().initialize(villagesConfig);
        VillagerManager.getInstance();
        VillageCraftingManager.getInstance();
        registerPacketTypes();
        VillagerWorldTickHandler.init();
        ServerTickHandler.init();
        registerNetworkingHandlers();
        VillageCraftingNetwork.registerServerHandlers();
        CulturalEventSystem.getInstance();
        ModCommands.register();
        VillagerEvents.registerTheftDetection();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverInstance = server;
            VillagerManager.getInstance().setServer(server);
            LOGGER.info("Villages Reborn detected server start.");
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (serverInstance != null) {
                long startTime = System.nanoTime();
                long endTime = System.nanoTime();
                float tickTime = (endTime - startTime) / 1_000_000.0f;
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
        if (tickTimes.size() > 100) tickTimes.remove(0);
    }

    public float getAverageTickTime() {
        return tickTimes.isEmpty() ? 0 : (float) tickTimes.stream().mapToDouble(Float::doubleValue).average().orElse(0);
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
        if (pendingNetworkRequests > 0) pendingNetworkRequests--;
    }

    public int getPlayerReputation(UUID playerId, String cultureId) {
        return playerReputation
            .getOrDefault(playerId, new HashMap<>())
            .getOrDefault(cultureId, 0);
    }

    public void updatePlayerReputation(UUID playerId, String cultureId, int change) {
        Map<String, Integer> reputations = playerReputation.computeIfAbsent(playerId, k -> new HashMap<>());
        int currentRep = reputations.getOrDefault(cultureId, 0);
        reputations.put(cultureId, Math.max(-100, Math.min(100, currentRep + change)));
    }

    private void registerPacketTypes() {
        LOGGER.info("Registering Villages Reborn packet types");
        PayloadTypeRegistry.playS2C().register(VillagesNetwork.VILLAGE_INFO_ID_PAYLOAD, VillagesNetwork.VILLAGE_INFO_CODEC);
        PayloadTypeRegistry.playS2C().register(VillagesNetwork.EVENT_NOTIFICATION_ID_PAYLOAD, VillagesNetwork.EVENT_NOTIFICATION_CODEC);
        PayloadTypeRegistry.playS2C().register(VillagesNetwork.EVENT_UPDATE_ID_PAYLOAD, VillagesNetwork.EVENT_UPDATE_CODEC);
        PayloadTypeRegistry.playS2C().register(VillagesNetwork.VILLAGER_AI_STATE_ID_PAYLOAD, VillagesNetwork.VILLAGER_AI_STATE_CODEC);
        PayloadTypeRegistry.playS2C().register(VillagesClientNetwork.VILLAGER_CULTURE_ID_PAYLOAD, VillagesClientNetwork.VILLAGER_CULTURE_CODEC);
        PayloadTypeRegistry.playS2C().register(VillagesClientNetwork.VILLAGER_MOOD_ID_PAYLOAD, VillagesClientNetwork.VILLAGER_MOOD_CODEC);
        PayloadTypeRegistry.playS2C().register(VillageCraftingNetwork.RECIPE_LIST_ID, VillageCraftingNetwork.RECIPE_LIST_CODEC);
        PayloadTypeRegistry.playS2C().register(VillageCraftingNetwork.CRAFT_STATUS_ID, VillageCraftingNetwork.CRAFT_STATUS_CODEC);
        PayloadTypeRegistry.playS2C().register(VillageCraftingNetwork.CRAFT_PROGRESS_ID, VillageCraftingNetwork.CRAFT_PROGRESS_CODEC);
        PayloadTypeRegistry.playS2C().register(VillageCraftingNetwork.CRAFT_COMPLETE_ID, VillageCraftingNetwork.CRAFT_COMPLETE_CODEC);
        PayloadTypeRegistry.playC2S().register(VillagesNetwork.REQUEST_VILLAGE_INFO_ID_PAYLOAD, VillagesNetwork.REQUEST_VILLAGE_INFO_CODEC);
        PayloadTypeRegistry.playC2S().register(VillagesNetwork.JOIN_EVENT_ID_PAYLOAD, VillagesNetwork.JOIN_EVENT_CODEC);
        PayloadTypeRegistry.playC2S().register(VillageCraftingNetwork.CRAFT_RECIPE_ID, VillageCraftingNetwork.CRAFT_RECIPE_CODEC);
        PayloadTypeRegistry.playC2S().register(VillageCraftingNetwork.REQUEST_RECIPES_ID, VillageCraftingNetwork.REQUEST_RECIPES_CODEC);
        PayloadTypeRegistry.playC2S().register(VillageCraftingNetwork.CANCEL_CRAFT_ID, VillageCraftingNetwork.CANCEL_CRAFT_CODEC);
        LOGGER.info("Packet types registered successfully");
    }

    private void registerNetworkingHandlers() {
        LOGGER.info("Registering networking handlers");
        VillagesNetwork.registerServerHandlers();
        VillageCraftingNetwork.registerServerHandlers();
    }

    private void registerVillagerLoadCallback() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof VillagerEntity villager && !world.isClient()) {
                if (VillagerManager.getInstance().getVillagerAI(villager.getUuid()) == null) {
                    LOGGER.debug("Detected Villager load: {}", villager.getUuid());
                    if (serverInstance != null) {
                        serverInstance.execute(() -> VillagerManager.getInstance().onVillagerSpawn(villager, (ServerWorld) world));
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
            server.execute(() -> PlayerDataManager.getInstance().loadPlayerData(player));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            LOGGER.debug("Player disconnected: {}", player.getName().getString());
            server.execute(() -> PlayerDataManager.getInstance().savePlayerData(player));
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
        registerCultureStructures("plains", BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.MEADOW, BiomeKeys.SUNFLOWER_PLAINS), "roman", Map.of(ROMAN_HOUSE, 15, ROMAN_FORUM, 8));
        registerCultureStructures("desert", BiomeSelectors.includeByKey(BiomeKeys.DESERT, BiomeKeys.BADLANDS), "egyptian", Map.of(EGYPTIAN_HOUSE, 15, EGYPTIAN_TEMPLE, 7));
        registerCultureStructures("taiga", BiomeSelectors.includeByKey(BiomeKeys.TAIGA, BiomeKeys.OLD_GROWTH_PINE_TAIGA, BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA), "victorian", Map.of(VICTORIAN_HOUSE, 15, VICTORIAN_SQUARE, 8));
        registerCultureStructures("savanna", BiomeSelectors.includeByKey(BiomeKeys.SAVANNA, BiomeKeys.SAVANNA_PLATEAU, BiomeKeys.WINDSWEPT_SAVANNA), "nyc", Map.of(NYC_APARTMENT, 15, NYC_SKYSCRAPER, 7));
        LOGGER.info("Structure modifications registration complete.");
    }

    private void registerCultureStructures(String vanillaType, java.util.function.Predicate<net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext> selector, String cultureType, Map<Identifier, Integer> structures) {
        VillagerManager.getInstance().registerBiomeCultureAssociation(selector, cultureType);

        LOGGER.info("Registered {} culture for {} biomes", cultureType, vanillaType);

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Initializing structure generation for {} culture in {} villages",
                       cultureType, vanillaType);
            VillagerManager.getInstance().initializeCultureStructures(server, cultureType, vanillaType, structures);
        });
    }

    public static SystemSpecs getSystemSpecs() {
        if (systemSpecs == null) {
            systemSpecs = new SystemSpecs();
            systemSpecs.analyzeSystem();
        }
        return systemSpecs;
    }

    public static VillagesConfig getVillagesConfig() {
        if (villagesConfig == null) {
            villagesConfig = VillagesConfig.getInstance();
            villagesConfig.load();
        }
        return villagesConfig;
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
        return com.beeny.village.event.VillageEvent.findEventsNear(pos, getVillageRadius(pos)).size();
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
