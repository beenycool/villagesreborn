# Spawn Biome Storage Refactoring Specification

## Overview

Refactor the spawn biome choice storage system to remove static field dependencies and implement proper world-persistent and per-player NBT storage. This ensures data persistence across server restarts and proper isolation in multiplayer environments.

## Current Architecture Issues

### Static Field Dependencies
- [`VillagesRebornWorldSettingsExtensions.currentSpawnBiomeChoice`](fabric/src/main/java/com/beeny/villagesreborn/platform/fabric/spawn/VillagesRebornWorldSettingsExtensions.java:19) creates memory leaks and multiplayer state conflicts
- [`BiomeSelectorEventHandler.hasShownBiomeSelector`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/spawn/BiomeSelectorEventHandler.java:21) prevents proper per-world tracking
- No isolation between different worlds or players

### Persistence Gaps
- Static fields don't survive server restarts
- No per-player tracking for multiplayer scenarios
- Backward compatibility concerns for existing static data

## Refactoring Goals

1. **Remove Static Dependencies**: Eliminate all static fields related to spawn biome choices
2. **World-Persistent Storage**: Integrate with [`VillagesRebornWorldDataPersistent`](fabric/src/main/java/com/beeny/villagesreborn/core/world/VillagesRebornWorldDataPersistent.java:1)
3. **Per-Player Tracking**: Store player-specific choices in player NBT data
4. **Multiplayer Isolation**: Ensure proper state separation between players and worlds
5. **Backward Compatibility**: Migrate existing static data gracefully

## Architecture Design

### Storage Strategy Decision Matrix

| Data Type | Storage Location | Persistence Scope | Use Case |
|-----------|------------------|-------------------|-----------|
| Spawn Biome Choice | World NBT | Per-World | Server-wide spawn configuration |
| Player Biome Preference | Player NBT | Per-Player | Individual player preferences |
| GUI State Tracking | World NBT | Per-World | First-time setup completion |

### Module Structure

```
spawn/
├── storage/
│   ├── SpawnBiomeStorage.java           # Core storage interface
│   ├── WorldSpawnBiomeStorage.java      # World-level persistence
│   └── PlayerSpawnBiomeStorage.java     # Player-level persistence
├── persistence/
│   ├── SpawnBiomeNBTHandler.java        # NBT serialization logic
│   └── SpawnBiomeMigration.java         # Backward compatibility
├── managers/
│   ├── SpawnBiomeStorageManager.java    # Central coordinator
│   └── SpawnBiomeEventManager.java      # Event handling refactor
└── data/
    ├── SpawnBiomeChoiceData.java        # (Existing, enhanced)
    └── SpawnBiomePreferences.java       # Player preferences model
```

## Pseudocode Specifications

### Core Storage Interface

```java
// File: spawn/storage/SpawnBiomeStorage.java
INTERFACE SpawnBiomeStorage {
    METHODS:
        Optional<SpawnBiomeChoiceData> getSpawnBiomeChoice(WorldIdentifier worldId)
        void setSpawnBiomeChoice(WorldIdentifier worldId, SpawnBiomeChoiceData choice)
        void clearSpawnBiomeChoice(WorldIdentifier worldId)
        boolean hasSpawnBiomeChoice(WorldIdentifier worldId)
        
        // Player-specific methods
        Optional<SpawnBiomeChoiceData> getPlayerBiomePreference(PlayerIdentifier playerId)
        void setPlayerBiomePreference(PlayerIdentifier playerId, SpawnBiomeChoiceData preference)
        
        // Migration support
        void migrateFromStaticData()
        boolean hasLegacyData()
}
```

### World-Level Storage Implementation

```java
// File: spawn/storage/WorldSpawnBiomeStorage.java
CLASS WorldSpawnBiomeStorage IMPLEMENTS SpawnBiomeStorage {
    FIELDS:
        private final ServerWorld world
        private final Logger logger
        
    CONSTRUCTOR:
        WorldSpawnBiomeStorage(ServerWorld world)
        
    METHODS:
        getSpawnBiomeChoice(WorldIdentifier worldId):
            persistent = VillagesRebornWorldDataPersistent.get(world)
            worldData = persistent.getData()
            settings = worldData.getSettings()
            
            IF settings IS NULL:
                RETURN Optional.empty()
                
            spawnBiomeMap = settings.getCustomData().get("spawn_biome_choices")
            IF spawnBiomeMap IS NULL:
                RETURN Optional.empty()
                
            choiceData = deserializeFromMap(spawnBiomeMap)
            RETURN Optional.of(choiceData)
            
        setSpawnBiomeChoice(WorldIdentifier worldId, SpawnBiomeChoiceData choice):
            persistent = VillagesRebornWorldDataPersistent.get(world)
            worldData = persistent.getData()
            
            settings = worldData.getSettings()
            IF settings IS NULL:
                settings = new VillagesRebornWorldSettings()
                
            customData = settings.getCustomData()
            customData.put("spawn_biome_choices", serializeToMap(choice))
            customData.put("spawn_biome_last_updated", System.currentTimeMillis())
            
            settings.setCustomData(customData)
            persistent.setSettings(settings)
            persistent.markDirty()
            
        migrateFromStaticData():
            legacyChoice = VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice()
            IF legacyChoice IS NOT NULL:
                setSpawnBiomeChoice(world.getRegistryKey(), legacyChoice)
                VillagesRebornWorldSettingsExtensions.resetForTest() // Clear static
                logger.info("Migrated legacy spawn biome choice to world storage")
}
```

### Player-Level Storage Implementation

```java
// File: spawn/storage/PlayerSpawnBiomeStorage.java
CLASS PlayerSpawnBiomeStorage IMPLEMENTS SpawnBiomeStorage {
    FIELDS:
        private final ServerPlayerEntity player
        private final Logger logger
        private static final String PLAYER_NBT_KEY = "villagesreborn_spawn_biome_preference"
        
    CONSTRUCTOR:
        PlayerSpawnBiomeStorage(ServerPlayerEntity player)
        
    METHODS:
        getPlayerBiomePreference(PlayerIdentifier playerId):
            playerData = player.writeNbt(new NbtCompound())
            
            IF NOT playerData.contains(PLAYER_NBT_KEY):
                RETURN Optional.empty()
                
            biomeNbt = playerData.getCompound(PLAYER_NBT_KEY)
            choice = SpawnBiomeNBTHandler.deserializeFromNbt(biomeNbt)
            RETURN Optional.of(choice)
            
        setPlayerBiomePreference(PlayerIdentifier playerId, SpawnBiomeChoiceData preference):
            playerData = player.writeNbt(new NbtCompound())
            biomeNbt = SpawnBiomeNBTHandler.serializeToNbt(preference)
            playerData.put(PLAYER_NBT_KEY, biomeNbt)
            
            player.readNbt(playerData)
            logger.debug("Saved player biome preference: {}", preference)
}
```

### NBT Serialization Handler

```java
// File: spawn/persistence/SpawnBiomeNBTHandler.java
CLASS SpawnBiomeNBTHandler {
    STATIC FIELDS:
        private static final String BIOME_NAMESPACE_KEY = "biome_namespace"
        private static final String BIOME_PATH_KEY = "biome_path"
        private static final String SELECTION_TIME_KEY = "selection_timestamp"
        private static final String DATA_VERSION_KEY = "data_version"
        private static final String CURRENT_VERSION = "2.0"
        
    STATIC METHODS:
        serializeToNbt(SpawnBiomeChoiceData choice):
            nbt = new NbtCompound()
            biomeId = choice.getBiomeKey().getValue()
            
            nbt.putString(BIOME_NAMESPACE_KEY, biomeId.getNamespace())
            nbt.putString(BIOME_PATH_KEY, biomeId.getPath())
            nbt.putLong(SELECTION_TIME_KEY, choice.getSelectionTimestamp())
            nbt.putString(DATA_VERSION_KEY, CURRENT_VERSION)
            
            RETURN nbt
            
        deserializeFromNbt(NbtCompound nbt):
            IF NOT nbt.contains(BIOME_NAMESPACE_KEY) OR NOT nbt.contains(BIOME_PATH_KEY):
                THROW IllegalArgumentException("Missing required biome data")
                
            namespace = nbt.getString(BIOME_NAMESPACE_KEY)
            path = nbt.getString(BIOME_PATH_KEY)
            
            IF namespace.isEmpty() OR path.isEmpty():
                THROW IllegalArgumentException("Empty biome identifier components")
                
            biomeId = Identifier.of(namespace, path)
            biomeKey = RegistryKey.of(RegistryKeys.BIOME, biomeId)
            
            timestamp = nbt.getLong(SELECTION_TIME_KEY)
            IF timestamp <= 0:
                timestamp = System.currentTimeMillis() // Backward compatibility
                
            RETURN new SpawnBiomeChoiceData(biomeKey, timestamp)
            
        serializeToMap(SpawnBiomeChoiceData choice):
            map = new HashMap<String, Object>()
            biomeId = choice.getBiomeKey().getValue()
            
            map.put("biome_namespace", biomeId.getNamespace())
            map.put("biome_path", biomeId.getPath())
            map.put("selection_timestamp", choice.getSelectionTimestamp())
            map.put("data_version", CURRENT_VERSION)
            
            RETURN map
            
        deserializeFromMap(Map<String, Object> map):
            namespace = (String) map.get("biome_namespace")
            path = (String) map.get("biome_path")
            timestamp = ((Number) map.get("selection_timestamp")).longValue()
            
            biomeId = Identifier.of(namespace, path)
            biomeKey = RegistryKey.of(RegistryKeys.BIOME, biomeId)
            
            RETURN new SpawnBiomeChoiceData(biomeKey, timestamp)
}
```

### Central Storage Manager

```java
// File: spawn/managers/SpawnBiomeStorageManager.java
CLASS SpawnBiomeStorageManager {
    FIELDS:
        private final Map<WorldIdentifier, WorldSpawnBiomeStorage> worldStorages
        private final Map<PlayerIdentifier, PlayerSpawnBiomeStorage> playerStorages
        private final Logger logger
        private static SpawnBiomeStorageManager instance
        
    SINGLETON PATTERN:
        getInstance():
            IF instance IS NULL:
                instance = new SpawnBiomeStorageManager()
            RETURN instance
            
    METHODS:
        getWorldStorage(ServerWorld world):
            worldId = world.getRegistryKey()
            storage = worldStorages.get(worldId)
            
            IF storage IS NULL:
                storage = new WorldSpawnBiomeStorage(world)
                worldStorages.put(worldId, storage)
                
                // Perform migration on first access
                storage.migrateFromStaticData()
                
            RETURN storage
            
        getPlayerStorage(ServerPlayerEntity player):
            playerId = player.getUuid()
            storage = playerStorages.get(playerId)
            
            IF storage IS NULL:
                storage = new PlayerSpawnBiomeStorage(player)
                playerStorages.put(playerId, storage)
                
            RETURN storage
            
        setWorldSpawnBiome(ServerWorld world, SpawnBiomeChoiceData choice):
            storage = getWorldStorage(world)
            storage.setSpawnBiomeChoice(world.getRegistryKey(), choice)
            logger.info("Set world spawn biome: {} for world {}", choice, world.getRegistryKey())
            
        getWorldSpawnBiome(ServerWorld world):
            storage = getWorldStorage(world)
            RETURN storage.getSpawnBiomeChoice(world.getRegistryKey())
            
        setPlayerBiomePreference(ServerPlayerEntity player, SpawnBiomeChoiceData preference):
            storage = getPlayerStorage(player)
            storage.setPlayerBiomePreference(player.getUuid(), preference)
            
        getPlayerBiomePreference(ServerPlayerEntity player):
            storage = getPlayerStorage(player)
            RETURN storage.getPlayerBiomePreference(player.getUuid())
            
        clearWorldData(WorldIdentifier worldId):
            worldStorages.remove(worldId)
            logger.debug("Cleared world storage for: {}", worldId)
            
        clearPlayerData(PlayerIdentifier playerId):
            playerStorages.remove(playerId)
            logger.debug("Cleared player storage for: {}", playerId)
}
```

### Enhanced Event Handler

```java
// File: spawn/managers/SpawnBiomeEventManager.java
CLASS SpawnBiomeEventManager IMPLEMENTS ClientPlayConnectionEvents.Join {
    FIELDS:
        private final SpawnBiomeStorageManager storageManager
        private final Logger logger
        
    CONSTRUCTOR:
        SpawnBiomeEventManager(SpawnBiomeStorageManager storageManager)
        
    METHODS:
        onPlayReady(ClientPlayNetworkHandler handler, PacketSender sender, MinecraftClient client):
            IF NOT shouldShowBiomeSelector(client):
                RETURN
                
            // Check if biome selector has been shown for this world
            IF hasWorldCompletedFirstTimeSetup(client.world):
                RETURN
                
            showBiomeSelector(client)
            markWorldFirstTimeSetupComplete(client.world)
            
        shouldShowBiomeSelector(MinecraftClient client):
            IF client.world IS NULL:
                RETURN false
                
            worldKey = client.world.getRegistryKey()
            IF worldKey IS NULL:
                RETURN false
                
            worldId = worldKey.getValue()
            isOverworld = worldId.getNamespace().equals("minecraft") AND worldId.getPath().equals("overworld")
            RETURN isOverworld
            
        hasWorldCompletedFirstTimeSetup(ClientWorld world):
            // Query server for world-specific setup status
            worldStorage = storageManager.getWorldStorage(world)
            worldData = worldStorage.getSpawnBiomeChoice(world.getRegistryKey())
            RETURN worldData.isPresent()
            
        markWorldFirstTimeSetupComplete(ClientWorld world):
            // Send packet to server to mark setup complete
            // Implementation depends on networking setup
            
        showBiomeSelector(MinecraftClient client):
            screen = BiomeSelectorScreen.create()
            client.setScreen(screen)
            logger.info("Displayed biome selector for world: {}", client.world.getRegistryKey())
}
```

### Integration with VillagesRebornWorldDataPersistent

```java
// File: fabric/src/main/java/com/beeny/villagesreborn/core/world/VillagesRebornWorldDataPersistent.java
// Enhancement to existing class

CLASS VillagesRebornWorldDataPersistent {
    // ... existing code ...
    
    NEW METHODS:
        setSpawnBiomeChoice(SpawnBiomeChoiceData choice):
            IF NOT data.hasSettings():
                data.setSettings(new VillagesRebornWorldSettings())
                
            settings = data.getSettings()
            customData = settings.getCustomData()
            spawnBiomeMap = SpawnBiomeNBTHandler.serializeToMap(choice)
            
            customData.put("spawn_biome_choice", spawnBiomeMap)
            customData.put("spawn_biome_updated", System.currentTimeMillis())
            
            settings.setCustomData(customData)
            data.setSettings(settings)
            markDirty()
            
        getSpawnBiomeChoice():
            IF NOT data.hasSettings():
                RETURN Optional.empty()
                
            settings = data.getSettings()
            customData = settings.getCustomData()
            spawnBiomeMap = customData.get("spawn_biome_choice")
            
            IF spawnBiomeMap IS NULL:
                RETURN Optional.empty()
                
            choice = SpawnBiomeNBTHandler.deserializeFromMap((Map<String, Object>) spawnBiomeMap)
            RETURN Optional.of(choice)
            
        hasSpawnBiomeChoice():
            RETURN getSpawnBiomeChoice().isPresent()
            
        clearSpawnBiomeChoice():
            IF data.hasSettings():
                settings = data.getSettings()
                customData = settings.getCustomData()
                customData.remove("spawn_biome_choice")
                customData.remove("spawn_biome_updated")
                settings.setCustomData(customData)
                data.setSettings(settings)
                markDirty()
}
```

### Migration Strategy

```java
// File: spawn/persistence/SpawnBiomeMigration.java
CLASS SpawnBiomeMigration {
    STATIC FIELDS:
        private static final Logger LOGGER = LoggerFactory.getLogger(SpawnBiomeMigration.class)
        private static final String MIGRATION_MARKER_KEY = "spawn_biome_migration_v2"
        
    STATIC METHODS:
        performMigration(ServerWorld world):
            worldData = VillagesRebornWorldDataPersistent.get(world)
            
            // Check if migration already performed
            IF worldData.getData().getCustomData().containsKey(MIGRATION_MARKER_KEY):
                RETURN
                
            // Check for legacy static data
            legacyChoice = VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice()
            IF legacyChoice IS NOT NULL:
                // Migrate to new storage
                worldData.setSpawnBiomeChoice(legacyChoice)
                
                // Clear legacy static storage
                VillagesRebornWorldSettingsExtensions.resetForTest()
                
                LOGGER.info("Migrated legacy spawn biome choice: {}", legacyChoice)
            
            // Mark migration as complete
            customData = worldData.getData().getCustomData()
            customData.put(MIGRATION_MARKER_KEY, true)
            worldData.markDirty()
            
        needsMigration(ServerWorld world):
            worldData = VillagesRebornWorldDataPersistent.get(world)
            migrationMarker = worldData.getData().getCustomData().get(MIGRATION_MARKER_KEY)
            RETURN migrationMarker IS NULL OR NOT (Boolean) migrationMarker
}
```

## TDD Test Implementation Plan

### Core Storage Tests

```java
// File: fabric/src/test/java/com/beeny/villagesreborn/platform/fabric/spawn/storage/WorldSpawnBiomeStorageTest.java
CLASS WorldSpawnBiomeStorageTest {
    FIELDS:
        private MockServerWorld mockWorld
        private WorldSpawnBiomeStorage storage
        private SpawnBiomeChoiceData testChoice
        
    SETUP:
        beforeEach():
            mockWorld = createMockServerWorld()
            storage = new WorldSpawnBiomeStorage(mockWorld)
            testChoice = new SpawnBiomeChoiceData(BiomeKeys.PLAINS, System.currentTimeMillis())
            
    TESTS:
        testSetAndGetSpawnBiomeChoice():
            // GIVEN: Storage is empty
            ASSERT storage.getSpawnBiomeChoice(mockWorld.getRegistryKey()).isEmpty()
            
            // WHEN: Setting spawn biome choice
            storage.setSpawnBiomeChoice(mockWorld.getRegistryKey(), testChoice)
            
            // THEN: Choice should be retrievable
            retrieved = storage.getSpawnBiomeChoice(mockWorld.getRegistryKey())
            ASSERT retrieved.isPresent()
            ASSERT retrieved.get().getBiomeKey() == testChoice.getBiomeKey()
            ASSERT retrieved.get().getSelectionTimestamp() == testChoice.getSelectionTimestamp()
            
        testPersistenceAcrossInstances():
            // GIVEN: Choice set in first storage instance
            storage.setSpawnBiomeChoice(mockWorld.getRegistryKey(), testChoice)
            
            // WHEN: Creating new storage instance for same world
            newStorage = new WorldSpawnBiomeStorage(mockWorld)
            
            // THEN: Choice should still be accessible
            retrieved = newStorage.getSpawnBiomeChoice(mockWorld.getRegistryKey())
            ASSERT retrieved.isPresent()
            ASSERT retrieved.get().getBiomeKey() == testChoice.getBiomeKey()
            
        testClearSpawnBiomeChoice():
            // GIVEN: Choice is set
            storage.setSpawnBiomeChoice(mockWorld.getRegistryKey(), testChoice)
            
            // WHEN: Clearing choice
            storage.clearSpawnBiomeChoice(mockWorld.getRegistryKey())
            
            // THEN: Choice should be empty
            ASSERT storage.getSpawnBiomeChoice(mockWorld.getRegistryKey()).isEmpty()
            
        testMigrationFromStaticData():
            // GIVEN: Legacy static data exists
            VillagesRebornWorldSettingsExtensions.setSpawnBiomeChoice(testChoice)
            
            // WHEN: Performing migration
            storage.migrateFromStaticData()
            
            // THEN: Data should be migrated and static cleared
            retrieved = storage.getSpawnBiomeChoice(mockWorld.getRegistryKey())
            ASSERT retrieved.isPresent()
            ASSERT retrieved.get().getBiomeKey() == testChoice.getBiomeKey()
            ASSERT VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice() IS NULL
}
```

### Player Storage Tests

```java
// File: fabric/src/test/java/com/beeny/villagesreborn/platform/fabric/spawn/storage/PlayerSpawnBiomeStorageTest.java
CLASS PlayerSpawnBiomeStorageTest {
    FIELDS:
        private MockServerPlayer mockPlayer
        private PlayerSpawnBiomeStorage storage
        private SpawnBiomeChoiceData testPreference
        
    SETUP:
        beforeEach():
            mockPlayer = createMockServerPlayer()
            storage = new PlayerSpawnBiomeStorage(mockPlayer)
            testPreference = new SpawnBiomeChoiceData(BiomeKeys.FOREST, System.currentTimeMillis())
            
    TESTS:
        testSetAndGetPlayerBiomePreference():
            // GIVEN: Player has no preference set
            ASSERT storage.getPlayerBiomePreference(mockPlayer.getUuid()).isEmpty()
            
            // WHEN: Setting player preference
            storage.setPlayerBiomePreference(mockPlayer.getUuid(), testPreference)
            
            // THEN: Preference should be retrievable
            retrieved = storage.getPlayerBiomePreference(mockPlayer.getUuid())
            ASSERT retrieved.isPresent()
            ASSERT retrieved.get().getBiomeKey() == testPreference.getBiomeKey()
            
        testPlayerNBTPersistence():
            // GIVEN: Player preference is set
            storage.setPlayerBiomePreference(mockPlayer.getUuid(), testPreference)
            
            // WHEN: Simulating player data save/load cycle
            playerNbt = mockPlayer.writeNbt(new NbtCompound())
            newPlayer = createMockServerPlayer()
            newPlayer.readNbt(playerNbt)
            newStorage = new PlayerSpawnBiomeStorage(newPlayer)
            
            // THEN: Preference should survive the cycle
            retrieved = newStorage.getPlayerBiomePreference(newPlayer.getUuid())
            ASSERT retrieved.isPresent()
            ASSERT retrieved.get().getBiomeKey() == testPreference.getBiomeKey()
}
```

### Integration Tests

```java
// File: fabric/src/test/java/com/beeny/villagesreborn/platform/fabric/spawn/SpawnBiomeStorageIntegrationTest.java
CLASS SpawnBiomeStorageIntegrationTest {
    FIELDS:
        private MockServerWorld world1, world2
        private MockServerPlayer player1, player2
        private SpawnBiomeStorageManager manager
        
    SETUP:
        beforeEach():
            world1 = createMockServerWorld("world1")
            world2 = createMockServerWorld("world2")
            player1 = createMockServerPlayer("player1")
            player2 = createMockServerPlayer("player2")
            manager = SpawnBiomeStorageManager.getInstance()
            
    TESTS:
        testWorldIsolation():
            // GIVEN: Different biomes set for different worlds
            choice1 = new SpawnBiomeChoiceData(BiomeKeys.PLAINS, System.currentTimeMillis())
            choice2 = new SpawnBiomeChoiceData(BiomeKeys.DESERT, System.currentTimeMillis())
            
            // WHEN: Setting different choices for each world
            manager.setWorldSpawnBiome(world1, choice1)
            manager.setWorldSpawnBiome(world2, choice2)
            
            // THEN: Each world should have its own choice
            retrieved1 = manager.getWorldSpawnBiome(world1)
            retrieved2 = manager.getWorldSpawnBiome(world2)
            
            ASSERT retrieved1.isPresent()
            ASSERT retrieved2.isPresent()
            ASSERT retrieved1.get().getBiomeKey() == BiomeKeys.PLAINS
            ASSERT retrieved2.get().getBiomeKey() == BiomeKeys.DESERT
            
        testPlayerIsolation():
            // GIVEN: Different preferences for different players
            pref1 = new SpawnBiomeChoiceData(BiomeKeys.FOREST, System.currentTimeMillis())
            pref2 = new SpawnBiomeChoiceData(BiomeKeys.TAIGA, System.currentTimeMillis())
            
            // WHEN: Setting different preferences for each player
            manager.setPlayerBiomePreference(player1, pref1)
            manager.setPlayerBiomePreference(player2, pref2)
            
            // THEN: Each player should have their own preference
            retrieved1 = manager.getPlayerBiomePreference(player1)
            retrieved2 = manager.getPlayerBiomePreference(player2)
            
            ASSERT retrieved1.isPresent()
            ASSERT retrieved2.isPresent()
            ASSERT retrieved1.get().getBiomeKey() == BiomeKeys.FOREST
            ASSERT retrieved2.get().getBiomeKey() == BiomeKeys.TAIGA
            
        testServerRestartPersistence():
            // GIVEN: Data is set before "server restart"
            worldChoice = new SpawnBiomeChoiceData(BiomeKeys.PLAINS, System.currentTimeMillis())
            playerPref = new SpawnBiomeChoiceData(BiomeKeys.FOREST, System.currentTimeMillis())
            
            manager.setWorldSpawnBiome(world1, worldChoice)
            manager.setPlayerBiomePreference(player1, playerPref)
            
            // WHEN: Simulating server restart (clearing memory, reloading from persistence)
            SpawnBiomeStorageManager.resetInstance() // Clear static state
            newManager = SpawnBiomeStorageManager.getInstance()
            
            // THEN: Data should survive restart
            retrievedWorld = newManager.getWorldSpawnBiome(world1)
            retrievedPlayer = newManager.getPlayerBiomePreference(player1)
            
            ASSERT retrievedWorld.isPresent()
            ASSERT retrievedPlayer.isPresent()
            ASSERT retrievedWorld.get().getBiomeKey() == BiomeKeys.PLAINS
            ASSERT retrievedPlayer.get().getBiomeKey() == BiomeKeys.FOREST
            
        testNoStaticFieldDependencies():
            // GIVEN: Static fields are cleared
            VillagesRebornWorldSettingsExtensions.resetForTest()
            BiomeSelectorEventHandler.resetForTest()
            
            // WHEN: Using storage manager
            choice = new SpawnBiomeChoiceData(BiomeKeys.PLAINS, System.currentTimeMillis())
            manager.setWorldSpawnBiome(world1, choice)
            retrieved = manager.getWorldSpawnBiome(world1)
            
            // THEN: Operations should succeed without static fields
            ASSERT retrieved.isPresent()
            ASSERT retrieved.get().getBiomeKey() == BiomeKeys.PLAINS
            
            // AND: Static fields should remain clear
            ASSERT VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice() IS NULL
}
```

### Migration Tests

```java
// File: fabric/src/test/java/com/beeny/villagesreborn/platform/fabric/spawn/SpawnBiomeMigrationTest.java
CLASS SpawnBiomeMigrationTest {
    FIELDS:
        private MockServerWorld mockWorld
        private SpawnBiomeChoiceData legacyChoice
        
    SETUP:
        beforeEach():
            mockWorld = createMockServerWorld()
            legacyChoice = new SpawnBiomeChoiceData(BiomeKeys.PLAINS, System.currentTimeMillis())
            
    TESTS:
        testMigrationFromLegacyStaticField():
            // GIVEN: Legacy static data exists
            VillagesRebornWorldSettingsExtensions.setSpawnBiomeChoice(legacyChoice)
            
            // WHEN: Performing migration
            SpawnBiomeMigration.performMigration(mockWorld)
            
            // THEN: Data should be migrated to new storage
            worldData = VillagesRebornWorldDataPersistent.get(mockWorld)
            migratedChoice = worldData.getSpawnBiomeChoice()
            
            ASSERT migratedChoice.isPresent()
            ASSERT migratedChoice.get().getBiomeKey() == legacyChoice.getBiomeKey()
            ASSERT migratedChoice.get().getSelectionTimestamp() == legacyChoice.getSelectionTimestamp()
            
            // AND: Legacy static field should be cleared
            ASSERT VillagesRebornWorldSettingsExtensions.getSpawnBiomeChoice() IS NULL
            
        testMigrationIdempotency():
            // GIVEN: Migration has already been performed
            VillagesRebornWorldSettingsExtensions.setSpawnBiomeChoice(legacyChoice)
            SpawnBiomeMigration.performMigration(mockWorld)
            
            // WHEN: Attempting migration again
            anotherChoice = new SpawnBiomeChoiceData(BiomeKeys.DESERT, System.currentTimeMillis())
            VillagesRebornWorldSettingsExtensions.setSpawnBiomeChoice(anotherChoice)
            SpawnBiomeMigration.performMigration(mockWorld)
            
            // THEN: Original migrated data should remain unchanged
            worldData = VillagesRebornWorldDataPersistent.get(mockWorld)
            finalChoice = worldData.getSpawnBiomeChoice()
            
            ASSERT finalChoice.isPresent()
            ASSERT finalChoice.get().getBiomeKey() == legacyChoice.getBiomeKey() // Original, not the new one
            
        testMigrationWithNoLegacyData():
            // GIVEN: No legacy data exists
            VillagesRebornWorldSettingsExtensions.resetForTest()
            
            // WHEN: Performing migration
            SpawnBiomeMigration.performMigration(mockWorld)
            
            // THEN: No data should be created
            worldData = VillagesRebornWorldDataPersistent.get(mockWorld)
            ASSERT worldData.getSpawnBiomeChoice().isEmpty()
            
            // BUT: Migration marker should still be set
            ASSERT NOT SpawnBiomeMigration.needsMigration(mockWorld)
}
```

### Event Handler Tests

```java
// File: fabric/src/test/java/com/beeny/villagesreborn/platform/fabric/spawn/SpawnBiomeEventManagerTest.java
CLASS SpawnBiomeEventManagerTest {
    FIELDS:
        private SpawnBiomeStorageManager storageManager
        private SpawnBiomeEventManager eventManager
        private MockMinecraftClient mockClient
        private MockServerWorld mockWorld
        
    SETUP:
        beforeEach():
            storageManager = SpawnBiomeStorageManager.getInstance()
            eventManager = new SpawnBiomeEventManager(storageManager)
            mockClient = createMockMinecraftClient()
            mockWorld = createMockServerWorld()
            
    TESTS:
        testFirstTimeWorldJoin():
            // GIVEN: World has no spawn biome set (first time)
            mockClient.setWorld(mockWorld)
            
            // WHEN: Player joins world
            eventManager.onPlayReady(null, null, mockClient)
            
            // THEN: Biome selector should be shown
            VERIFY mockClient.setScreen() WAS CALLED WITH BiomeSelectorScreen
            
        testSubsequentWorldJoins():
            // GIVEN: World already has spawn biome configured
            existingChoice = new SpawnBiomeChoiceData(BiomeKeys.PLAINS, System.currentTimeMillis())
            storageManager.setWorldSpawnBiome(mockWorld, existingChoice)
            mockClient.setWorld(mockWorld)
            
            // WHEN: Player joins world again
            eventManager.onPlayReady(null, null, mockClient)
            
            // THEN: Biome selector should NOT be shown
            VERIFY mockClient.setScreen() WAS NOT CALLED
            
        testNonOverworldDimension():
            // GIVEN: Player is in non-overworld dimension
            netherWorld = createMockNetherWorld()
            mockClient.setWorld(netherWorld)
            
            // WHEN: Player joins world
            eventManager.onPlayReady(null, null, mockClient)
            
            // THEN: Biome selector should NOT be shown
            VERIFY mockClient.setScreen() WAS NOT CALLED
}
```

## Migration Strategy

### Phase 1: Foundation (Week 1)
- Implement core storage interfaces and data structures
- Create NBT serialization handlers
- Add basic world storage implementation
- Write unit tests for core components

### Phase 2: Integration (Week 2)
- Integrate with [`VillagesRebornWorldDataPersistent`](fabric/src/main/java/com/beeny/villagesreborn/core/world/VillagesRebornWorldDataPersistent.java:1)
- Implement player storage functionality
- Create storage manager coordinator
- Add integration tests

### Phase 3: Event Handling (Week 3)
- Refactor [`BiomeSelectorEventHandler`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/spawn/BiomeSelectorEventHandler.java:1) and [`SpawnPointManager`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/spawn/SpawnPointManager.java:1)
- Remove static field dependencies
- Implement migration logic
- Add backward compatibility tests

### Phase 4: Validation (Week 4)
- Comprehensive multiplayer testing
- Server restart persistence validation
- Performance optimization
- Documentation updates

## Acceptance Criteria

### ✅ Static Field Removal
- [ ] No static fields in spawn biome storage classes
- [ ] All spawn biome data persisted in NBT format
- [ ] Memory usage reduced for multiplayer scenarios

### ✅ Persistence Requirements
- [ ] Spawn biome choices survive server restarts
- [ ] World-specific choices isolated properly
- [ ] Player preferences persist across sessions

### ✅ Multiplayer Isolation
- [ ] Multiple players can have different biome preferences
- [ ] Different worlds maintain separate spawn configurations
- [ ] No data bleeding between players or worlds

### ✅ Backward Compatibility
- [ ] Existing static data migrated seamlessly
- [ ] Migration only runs once per world
- [ ] No data loss during migration process

### ✅ API Consistency
- [ ] [`VillagesRebornWorldSettingsExtensions`](fabric/src/main/java/com/beeny/villagesreborn/platform/fabric/spawn/VillagesRebornWorldSettingsExtensions.java:1) updated to use new storage
- [ ] Event handlers use NBT-based storage
- [ ] Consistent error handling across all storage operations

## Risk Mitigation

### Data Loss Prevention
- Migration runs automatically on first access
- Comprehensive backup mechanism before migration
- Extensive testing with edge cases

### Performance Considerations
- Lazy loading of storage instances
- Efficient NBT serialization
- Memory cleanup for disconnected players/worlds

### Compatibility Assurance
- Feature flags for gradual rollout
- Rollback mechanism if issues arise
- Extensive testing across different scenarios

## Testing Strategy Summary

1. **Unit Tests**: Individual component functionality and NBT serialization
2. **Integration Tests**: Cross-component interaction and persistence
3. **Migration Tests**: Backward compatibility and data migration
4. **Multiplayer Tests**: Isolation and concurrent access scenarios
5. **Performance Tests**: Memory usage and serialization efficiency
6. **End-to-End Tests**: Complete workflow from GUI to persistence

This specification ensures a robust, maintainable, and properly tested spawn biome storage system that eliminates static field dependencies while maintaining full backward compatibility.