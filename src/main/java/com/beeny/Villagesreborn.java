// File: src/main/java/com/beeny/Villagesreborn.java
package com.beeny;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents; // Import server event
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents; // Tick events
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry; // Needed for villager attributes if modified
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents; // Import entity load event
// Structure related imports (Example - adjust if needed)
import net.fabricmc.fabric.api.structure.v1.FabricStructurePool;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.structure.pool.StructureTemplatePool;
import net.minecraft.structure.processor.StructureProcessorList;
import java.util.stream.Collectors;


import com.beeny.commands.ModCommands; // Import commands
import com.beeny.event.VillagerEvents; // Import theft detection
import com.beeny.village.VillageCraftingManager;
import com.beeny.village.Culture;
import com.beeny.village.VillagerManager; // Import VillagerManager
import com.beeny.setup.SystemSpecs;
import com.beeny.setup.LLMConfig;
import net.minecraft.entity.Entity; // Import Entity
import net.minecraft.entity.passive.VillagerEntity; // Import VillagerEntity
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer; // Import MinecraftServer
import net.minecraft.server.world.ServerWorld; // Import ServerWorld
import net.minecraft.util.Identifier; // Import Identifier
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Villagesreborn implements ModInitializer {
    public static final String MOD_ID = "villagesreborn"; // Define MOD_ID
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static SystemSpecs systemSpecs;
    private static LLMConfig llmConfig;
    private static Villagesreborn INSTANCE;
    private final Map<BlockPos, Culture> villageMap = new ConcurrentHashMap<>();
    private final Map<BlockPos, Integer> villageRadiusMap = new ConcurrentHashMap<>();
    private final Map<BlockPos, Integer> villageEventsMap = new ConcurrentHashMap<>();
    private static MinecraftServer serverInstance; // Store server instance

    // Define structure identifiers for each culture
    private static final Identifier ROMAN_HOUSE = new Identifier(MOD_ID, "roman/house");
    private static final Identifier ROMAN_FORUM = new Identifier(MOD_ID, "roman/forum");
    private static final Identifier EGYPTIAN_HOUSE = new Identifier(MOD_ID, "egyptian/house");
    private static final Identifier EGYPTIAN_TEMPLE = new Identifier(MOD_ID, "egyptian/temple");
    private static final Identifier VICTORIAN_HOUSE = new Identifier(MOD_ID, "victorian/house");
    private static final Identifier VICTORIAN_SQUARE = new Identifier(MOD_ID, "victorian/square");
    private static final Identifier NYC_APARTMENT = new Identifier(MOD_ID, "nyc/apartment");
    private static final Identifier NYC_SKYSCRAPER = new Identifier(MOD_ID, "nyc/skyscraper");

    public static Villagesreborn getInstance() {
        return INSTANCE;
    }

    // Getter for server instance for other classes
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
        llmConfig.load(); // Load config on init
        llmConfig.initialize(systemSpecs); // Initialize based on specs

        // Initialize Managers (ensure correct order if dependencies exist)
        VillagerManager.getInstance(); // Initialize VillagerManager
        VillageCraftingManager.getInstance(); // Initialize Crafting Manager
        // Initialize other managers like VillageInfluenceManager, CulturalEventSystem etc. if needed here

        // Register Commands
        ModCommands.register();

        // Register Theft Detection Events
        VillagerEvents.registerTheftDetection();

        // Register Server Start Callback to get server instance
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverInstance = server;
            VillagerManager.getInstance().setServer(server); // Pass server instance to manager
            LOGGER.info("Villages Reborn detected server start.");
        });

         // Register Server Tick Callback for updates
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (serverInstance != null) {
                // Perform tick updates for managers
                 VillagerManager.getInstance().updateVillagerActivities(server.getOverworld()); // Example update call
                 // Add updates for EventManager, etc.
                 // CulturalEventSystem.getInstance().tickEventScheduler(server.getOverworld());
            }
        });

        // Register Villager Load Callback
        registerVillagerLoadCallback();

        // Register Structure Modifications (Placeholder - Needs specific implementation)
        registerStructureModifications();


        LOGGER.info("Villages Reborn mod initialization complete");
    }

    /**
     * Registers a callback to initialize VillagerAI when a VillagerEntity is loaded.
     */
    private void registerVillagerLoadCallback() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof VillagerEntity villager && !world.isClient()) {
                // Check if AI already exists to avoid re-initialization
                if (VillagerManager.getInstance().getVillagerAI(villager.getUuid()) == null) {
                     LOGGER.debug("Detected Villager load: {}", villager.getUuid());
                    // Ensure server instance is available before executing
                    if (serverInstance != null) {
                        // Initialize AI on the server thread
                         serverInstance.execute(() -> {
                             VillagerManager.getInstance().onVillagerSpawn(villager, (ServerWorld) world);
                         });
                    } else {
                        LOGGER.warn("Server instance not available during villager load for {}", villager.getUuid());
                        // Optionally queue the initialization for later when the server is ready
                    }
                }
            }
        });
         LOGGER.info("Registered Villager load callback.");
    }

    /**
     * Registers custom structures with vanilla world generation.
     * Uses Fabric API's StructurePoolAddCallback to add our custom structures to vanilla pools.
     */
    private void registerStructureModifications() {
        LOGGER.info("Registering structure modifications...");
        
        // Register Roman structures in Plains villages
        registerCultureStructures(
            "plains", 
            BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.MEADOW, BiomeKeys.SUNFLOWER_PLAINS),
            "roman",
            Map.of(
                ROMAN_HOUSE, 15,
                ROMAN_FORUM, 8
            )
        );
        
        // Register Egyptian structures in Desert villages
        registerCultureStructures(
            "desert", 
            BiomeSelectors.includeByKey(BiomeKeys.DESERT, BiomeKeys.BADLANDS),
            "egyptian",
            Map.of(
                EGYPTIAN_HOUSE, 15,
                EGYPTIAN_TEMPLE, 7
            )
        );
        
        // Register Victorian structures in Taiga villages
        registerCultureStructures(
            "taiga", 
            BiomeSelectors.includeByKey(BiomeKeys.TAIGA, BiomeKeys.OLD_GROWTH_PINE_TAIGA, BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA),
            "victorian",
            Map.of(
                VICTORIAN_HOUSE, 15,
                VICTORIAN_SQUARE, 8
            )
        );
        
        // Register NYC structures in Savanna villages
        registerCultureStructures(
            "savanna", 
            BiomeSelectors.includeByKey(BiomeKeys.SAVANNA, BiomeKeys.SAVANNA_PLATEAU, BiomeKeys.WINDSWEPT_SAVANNA),
            "nyc",
            Map.of(
                NYC_APARTMENT, 15,
                NYC_SKYSCRAPER, 7
            )
        );
        
        // Add more registrations for other culture types as needed
        
        LOGGER.info("Structure modifications registration complete.");
    }
    
    /**
     * Registers structures for a specific culture and associates them with vanilla village pools.
     * 
     * @param vanillaType The vanilla village type (plains, desert, taiga, etc.)
     * @param biomeSelector The biome selector for this culture type
     * @param cultureType The culture type name
     * @param structures Map of structure identifiers to their weight in the pool
     */
    private void registerCultureStructures(String vanillaType, BiomeSelectors.BiomeSelector biomeSelector, 
                                         String cultureType, Map<Identifier, Integer> structures) {
        
        // Houses pool - add our structures to vanilla houses pool
        Identifier housesPoolId = new Identifier("minecraft", "village/" + vanillaType + "/houses");
        RegistryKey<StructurePool> housesPoolKey = RegistryKey.of(RegistryKeys.TEMPLATE_POOL, housesPoolId);
        
        // Town center pool - for special buildings like forums, temples, etc.
        Identifier centerPoolId = new Identifier("minecraft", "village/" + vanillaType + "/town_centers");
        RegistryKey<StructurePool> centerPoolKey = RegistryKey.of(RegistryKeys.TEMPLATE_POOL, centerPoolId);
        
        // Register biome-culture association for the VillageManager to use in AI
        VillagerManager.getInstance().registerBiomeCultureAssociation(biomeSelector, cultureType);
        
        // Register structure pool elements
        net.fabricmc.fabric.api.structure.v1.FabricStructurePool.registerAddition(housesPoolId, builder -> {
            for (Map.Entry<Identifier, Integer> entry : structures.entrySet()) {
                // Skip central buildings for house pools
                if (entry.getKey().getPath().contains("forum") || 
                    entry.getKey().getPath().contains("temple") || 
                    entry.getKey().getPath().contains("square") ||
                    entry.getKey().getPath().contains("skyscraper")) {
                    continue;
                }
                
                LOGGER.info("Adding {} to {} village houses pool with weight {}", 
                            entry.getKey(), vanillaType, entry.getValue());
                
                // Add the structure to the pool with appropriate weight
                builder.element(entry.getKey().toString(), entry.getValue());
            }
        });
        
        // Register central structures to the center pool
        net.fabricmc.fabric.api.structure.v1.FabricStructurePool.registerAddition(centerPoolId, builder -> {
            for (Map.Entry<Identifier, Integer> entry : structures.entrySet()) {
                // Only include central buildings
                if (entry.getKey().getPath().contains("forum") || 
                    entry.getKey().getPath().contains("temple") || 
                    entry.getKey().getPath().contains("square") ||
                    entry.getKey().getPath().contains("skyscraper")) {
                    
                    LOGGER.info("Adding {} to {} village center pool with weight {}", 
                                entry.getKey(), vanillaType, entry.getValue());
                    
                    // Add the structure to the pool with appropriate weight
                    builder.element(entry.getKey().toString(), entry.getValue());
                }
            }
        });
        
        // Register structure generation listener to create SpawnRegion when structure is generated
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Register callback for when villages are generated to create SpawnRegions
            // Note: This is a placeholder - real implementation would need to hook into structure generation events
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
        // Implementation needed: Find nearest village, get stats, send to client via packets
    }

    // --- Village Management Methods ---
    // These might be better placed entirely within VillagerManager or a dedicated VillageRegistry

    public Culture getNearestVillageCulture(BlockPos pos) {
        SpawnRegion region = VillagerManager.getInstance().getNearestSpawnRegion(pos);
        return region != null ? region.getCulture() : null;
        // Old implementation:
        // if (villageMap.isEmpty()) return null;
        // BlockPos closest = null;
        // double closestDist = Double.MAX_VALUE;
        // for (BlockPos center : villageMap.keySet()) {
        //     double dist = center.getSquaredDistance(pos);
        //     if (dist < closestDist) {
        //         closestDist = dist;
        //         closest = center;
        //     }
        // }
        // return closest != null ? villageMap.get(closest) : null;
    }

    public BlockPos getVillageCenterPos(BlockPos pos) {
         SpawnRegion region = VillagerManager.getInstance().getNearestSpawnRegion(pos);
         if (region != null && region.isWithinRegion(pos)) {
             return region.getCenter();
         }
         return null;
        // Old implementation:
        // if (villageMap.isEmpty()) return null;
        // BlockPos closest = null;
        // double closestDist = Double.MAX_VALUE;
        // for (BlockPos center : villageMap.keySet()) {
        //     double dist = center.getSquaredDistance(pos);
        //     int radius = villageRadiusMap.getOrDefault(center, 64);
        //     if (dist <= radius * radius && dist < closestDist) {
        //         closestDist = dist;
        //         closest = center;
        //     }
        // }
        // return closest;
    }

    public int getVillageRadius(BlockPos pos) {
        SpawnRegion region = VillagerManager.getInstance().getNearestSpawnRegion(pos);
         if (region != null && region.isWithinRegion(pos)) {
             return region.getRadius();
         }
         return 0;
        // BlockPos center = getVillageCenterPos(pos);
        // return center != null ? villageRadiusMap.getOrDefault(center, 64) : 0;
    }

    public int getActiveEventCount(BlockPos pos) {
         // This needs integration with the actual event system (e.g., CulturalEventSystem)
         // Placeholder:
         return VillageEvent.findEventsNear(pos, getVillageRadius(pos)).size();
    }

     public Collection<Culture> getAllVillages() {
         // Get cultures from registered SpawnRegions in VillagerManager
         return VillagerManager.getInstance().getSpawnRegions().stream()
                 .map(SpawnRegion::getCulture)
                 .distinct()
                 .collect(Collectors.toList());
         // return villageMap.values(); // Old implementation
     }

     // Registering villages should primarily happen via VillagerManager now
     @Deprecated
     public void registerVillage(BlockPos center, Culture culture, int radius) {
         VillagerManager.getInstance().registerSpawnRegion(center, radius, culture.getName());
         // Old implementation:
         // villageMap.put(center, culture);
         // villageRadiusMap.put(center, radius);
         // villageEventsMap.put(center, 0); // Initialize event count
         // LOGGER.info("Registered village (deprecated method): {} at {}", culture.getType().getId(), center);
     }

     // Updating events should go through the event system
     @Deprecated
     public void updateVillageEvents(BlockPos center, int eventCount) {
         // Old implementation:
         // if (villageMap.containsKey(center)) {
         //     villageEventsMap.put(center, eventCount);
         // }
     }
}