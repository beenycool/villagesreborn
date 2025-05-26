# Phase 3: Spawn Biome Selector GUI - Specification & TDD Plan

## Overview

Phase 3 enhances the existing spawn biome system with a comprehensive GUI for biome selection during world creation. This builds upon the existing [`CreateWorldScreenMixin`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/mixin/client/CreateWorldScreenMixin.java) and [`BiomeSelectorScreen`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/gui/BiomeSelectorScreen.java) infrastructure to provide seamless world creation integration.

## Architecture

```
CreateWorldScreen (Vanilla)
├── CreateWorldScreenMixin (Enhanced)
│   ├── VillagesRebornTab (Existing)
│   └── SpawnBiomeButton (New)
│       └── BiomeSelectorScreen (Enhanced)
│           ├── BiomeSelectionWidget[] (Enhanced) 
│           └── BiomeSelectorEventHandler (Enhanced)
│               └── SpawnBiomeStorageManager (Existing)
│                   └── SpawnPointManager (Enhanced)
```

## 1. Mixin Injection Enhancement

### [`CreateWorldScreenMixin`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/mixin/client/CreateWorldScreenMixin.java:1) Enhancement

```pseudocode
CLASS CreateWorldScreenMixin {
    FIELD spawnBiomeButton: ButtonWidget
    FIELD biomeSelectorOpen: boolean = false
    
    METHOD addVillagesRebornTab() {
        // Existing tab injection logic...
        
        // ADD: Spawn Biome button injection
        IF (shouldInjectSpawnBiomeButton()) {
            CALL createSpawnBiomeButton()
            CALL positionButtonInTabOrder()
            CALL addButtonToScreen()
        }
    }
    
    METHOD createSpawnBiomeButton() {
        spawnBiomeButton = ButtonWidget.builder(
            Text.translatable("villagesreborn.spawn_biome.button"),
            this::onSpawnBiomeButtonClick
        )
        .position(calculateButtonPosition())
        .size(120, 20)
        .build()
        
        // Ensure button appears only once
        IF (spawnBiomeButton NOT IN screenWidgets) {
            ADD spawnBiomeButton TO screenWidgets
        }
    }
    
    METHOD onSpawnBiomeButtonClick(ButtonWidget button) {
        IF (NOT biomeSelectorOpen) {
            biomeSelectorOpen = true
            screen = BiomeSelectorScreen.createForWorldCreation()
            MinecraftClient.getInstance().setScreen(screen)
        }
    }
    
    METHOD shouldInjectSpawnBiomeButton() -> boolean {
        RETURN ModConfig.getInstance().isSpawnBiomeSelectionEnabled()
            AND NOT isCreativeMode()
            AND isOverworldGeneration()
    }
    
    METHOD calculateButtonPosition() -> Position {
        // Position after existing tab but before world creation buttons
        baseY = villagesRebornTab.getY() + villagesRebornTab.getHeight() + 10
        RETURN Position(centerX - 60, baseY)
    }
    
    METHOD positionButtonInTabOrder() {
        // Ensure proper tab navigation order
        SET spawnBiomeButton.tabIndex = villagesRebornTab.tabIndex + 1
    }
}
```

### TDD Anchor Points:
- Button injection timing (once per screen initialization)
- Tab order preservation
- Button positioning calculations
- Configuration-based visibility

## 2. BiomeSelectorScreen Enhancement

### [`BiomeSelectorScreen`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/gui/BiomeSelectorScreen.java:1) Enhancement

```pseudocode
CLASS BiomeSelectorScreen EXTENDS Screen {
    FIELD creationMode: WorldCreationMode
    FIELD parentScreen: CreateWorldScreen
    FIELD selectedBiomeForCreation: BiomeDisplayInfo
    
    ENUM WorldCreationMode {
        WORLD_CREATION,    // Called from Create World screen
        POST_JOIN          // Called after joining world
    }
    
    STATIC METHOD createForWorldCreation(parentScreen: CreateWorldScreen) -> BiomeSelectorScreen {
        screen = new BiomeSelectorScreen()
        screen.creationMode = WORLD_CREATION
        screen.parentScreen = parentScreen
        screen.availableBiomes = BiomeManager.getCreationSelectableBiomes()
        RETURN screen
    }
    
    METHOD initializeControlButtons() {
        // Enhanced button layout for world creation mode
        IF (creationMode == WORLD_CREATION) {
            confirmButton = createConfirmButton("villagesreborn.spawn_biome.confirm_for_creation")
            cancelButton = createCancelButton("villagesreborn.spawn_biome.cancel_creation")
            previewButton = createPreviewButton("villagesreborn.spawn_biome.preview")
        } ELSE {
            // Existing post-join logic...
        }
        
        // Position buttons appropriately
        layoutButtonsForMode()
    }
    
    METHOD confirmBiomeSelection() {
        IF (selectedBiome == null) RETURN
        
        IF (creationMode == WORLD_CREATION) {
            // Store choice for world creation
            WorldCreationSettingsCapture.setSpawnBiomeChoice(selectedBiome)
            LOGGER.info("Stored spawn biome choice for world creation: {}", selectedBiome.getRegistryKey())
            
            // Return to parent screen
            MinecraftClient.getInstance().setScreen(parentScreen)
        } ELSE {
            // Existing post-join teleportation logic...
        }
    }
    
    METHOD initializeBiomeWidgets() {
        // Enhanced grid layout with better spacing
        gridConfig = calculateOptimalGridLayout()
        
        FOR (biome IN availableBiomes) {
            widget = new BiomeSelectionWidget(
                biome, 
                gridConfig.widgetWidth, 
                gridConfig.widgetHeight,
                this::onBiomeSelected
            )
            
            // Enhanced widget configuration
            widget.setCreationMode(creationMode == WORLD_CREATION)
            widget.setTooltipProvider(this::createBiomeTooltip)
            widget.setPreviewHandler(this::onBiomePreview)
            
            biomeWidgets.add(widget)
            addDrawableChild(widget)
        }
    }
    
    METHOD createBiomeTooltip(BiomeDisplayInfo biome) -> Text[] {
        tooltip = []
        tooltip.add(Text.translatable("villagesreborn.biome.name." + biome.getTranslationKey()))
        tooltip.add(Text.translatable("villagesreborn.biome.difficulty." + biome.getDifficulty()))
        
        IF (creationMode == WORLD_CREATION) {
            tooltip.add(Text.translatable("villagesreborn.biome.creation_hint"))
        }
        
        RETURN tooltip
    }
    
    METHOD onBiomePreview(BiomeDisplayInfo biome) {
        // Show preview information
        previewPanel.setBiome(biome)
        previewPanel.setVisible(true)
    }
}
```

### TDD Anchor Points:
- Dual-mode operation (creation vs. post-join)
- Parent screen management
- Enhanced widget configuration
- Preview functionality

## 3. BiomeSelectionWidget Enhancement

### [`BiomeSelectionWidget`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/gui/BiomeSelectionWidget.java:1) Enhancement

```pseudocode
CLASS BiomeSelectionWidget EXTENDS ClickableWidget {
    FIELD creationMode: boolean = false
    FIELD tooltipProvider: Function<BiomeDisplayInfo, Text[]>
    FIELD previewHandler: Consumer<BiomeDisplayInfo>
    FIELD hoverAnimationProgress: float = 0.0f
    
    METHOD setCreationMode(isCreationMode: boolean) {
        this.creationMode = isCreationMode
        updateVisualStyle()
    }
    
    METHOD setTooltipProvider(provider: Function<BiomeDisplayInfo, Text[]>) {
        this.tooltipProvider = provider
    }
    
    METHOD setPreviewHandler(handler: Consumer<BiomeDisplayInfo>) {
        this.previewHandler = handler
    }
    
    METHOD render(context: DrawContext, mouseX: int, mouseY: int, delta: float) {
        // Enhanced rendering with animations
        updateHoverAnimation(delta)
        
        // Base widget rendering
        renderBackground(context)
        renderBiomeIcon(context)
        renderBiomeName(context)
        renderSelectionIndicator(context)
        
        // Creation mode enhancements
        IF (creationMode) {
            renderCreationModeIndicators(context)
        }
        
        // Hover effects
        IF (isHovered()) {
            renderHoverEffects(context)
            IF (previewHandler != null) {
                previewHandler.accept(biome)
            }
        }
    }
    
    METHOD renderCreationModeIndicators(context: DrawContext) {
        // Render difficulty indicator
        difficultyColor = getDifficultyColor(biome.getDifficulty())
        context.fill(x + width - 8, y + 2, x + width - 2, y + 8, difficultyColor)
        
        // Render resource indicator
        resourceLevel = biome.getResourceAbundance()
        renderResourceIndicator(context, resourceLevel)
    }
    
    METHOD updateHoverAnimation(delta: float) {
        targetProgress = isHovered() ? 1.0f : 0.0f
        animationSpeed = 0.1f
        
        hoverAnimationProgress = MathHelper.lerp(
            animationSpeed * delta, 
            hoverAnimationProgress, 
            targetProgress
        )
    }
    
    METHOD renderTooltip(context: DrawContext, mouseX: int, mouseY: int) {
        IF (tooltipProvider != null) {
            tooltipLines = tooltipProvider.apply(biome)
            context.drawTooltip(MinecraftClient.getInstance().textRenderer, tooltipLines, mouseX, mouseY)
        }
    }
    
    METHOD onClick(double mouseX, double mouseY) -> boolean {
        IF (clickHandler != null) {
            clickHandler.accept(biome)
            playClickSound()
            RETURN true
        }
        RETURN false
    }
}
```

### TDD Anchor Points:
- Mode-specific rendering
- Animation system
- Enhanced tooltip integration
- Preview trigger handling

## 4. Event Handling Enhancement

### [`BiomeSelectorEventHandler`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/spawn/BiomeSelectorEventHandler.java:1) Enhancement

```pseudocode
CLASS BiomeSelectorEventHandler {
    FIELD worldCreationChoices: Map<String, BiomeDisplayInfo> = ConcurrentHashMap()
    
    METHOD onSpawnBiomeButtonClick(CreateWorldScreen parentScreen) {
        // Handle button click from Create World screen
        IF (NOT isScreenOpen("BiomeSelectorScreen")) {
            screen = BiomeSelectorScreen.createForWorldCreation(parentScreen)
            MinecraftClient.getInstance().setScreen(screen)
            LOGGER.info("Opened biome selector from world creation")
        }
    }
    
    METHOD onBiomeChoiceConfirmed(BiomeDisplayInfo choice, WorldCreationMode mode) {
        IF (mode == WORLD_CREATION) {
            // Store choice for world creation process
            worldId = generateTempWorldId()
            worldCreationChoices.put(worldId, choice)
            
            LOGGER.info("Stored biome choice for world creation: {}", choice.getRegistryKey())
        } ELSE {
            // Existing post-join handling...
        }
    }
    
    METHOD onWorldCreated(World world) {
        // Apply stored choice when world is actually created
        worldId = world.getRegistryKey().getValue().toString()
        
        IF (worldCreationChoices.containsKey(worldId)) {
            choice = worldCreationChoices.remove(worldId)
            SpawnBiomeStorageManager.setWorldSpawnBiome(world, choice)
            LOGGER.info("Applied biome choice to new world: {}", choice.getRegistryKey())
        }
    }
    
    METHOD shouldShowBiomeSelector(MinecraftClient client) -> boolean {
        // Enhanced logic for multiple trigger conditions
        
        // Don't show if already chosen during creation
        IF (hasCreationTimeChoice(client.world)) {
            RETURN false
        }
        
        // Existing post-join logic...
        RETURN NOT hasWorldCompletedFirstTimeSetup(client.world)
            AND isOverworld(client.world)
            AND NOT hasExistingSpawnChoice(client.world)
    }
    
    METHOD hasCreationTimeChoice(World world) -> boolean {
        worldId = world.getRegistryKey().getValue().toString()
        RETURN worldCreationChoices.containsKey(worldId)
            OR SpawnBiomeStorageManager.hasStoredChoice(world)
    }
}
```

### TDD Anchor Points:
- Multiple trigger condition handling
- World creation lifecycle integration
- Choice persistence across creation process
- Duplicate prevention logic

## 5. SpawnPointManager Integration

### [`SpawnPointManager`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/spawn/SpawnPointManager.java:1) Enhancement

```pseudocode
CLASS SpawnPointManager {
    METHOD selectSpawnPoint(World world, Player player) -> BlockPos {
        // Check for stored biome choice first
        storedChoice = SpawnBiomeStorageManager.getWorldSpawnBiome(world)
        
        IF (storedChoice != null) {
            LOGGER.info("Using stored spawn biome choice: {}", storedChoice.getRegistryKey())
            RETURN findSpawnInBiome(world, storedChoice.getRegistryKey())
        }
        
        // Check for player-specific choice
        playerChoice = SpawnBiomeStorageManager.getPlayerSpawnBiome(player)
        
        IF (playerChoice != null) {
            LOGGER.info("Using player spawn biome choice: {}", playerChoice.getRegistryKey())
            RETURN findSpawnInBiome(world, playerChoice.getRegistryKey())
        }
        
        // Fallback to default behavior
        LOGGER.debug("No stored biome choice, using default spawn logic")
        RETURN findDefaultSpawnPoint(world)
    }
    
    METHOD findSpawnInBiome(World world, RegistryKey<Biome> biomeKey) -> BlockPos {
        // Enhanced biome-specific spawn finding
        biomeLocations = scanForBiome(world, biomeKey, SPAWN_SEARCH_RADIUS)
        
        IF (biomeLocations.isEmpty()) {
            LOGGER.warn("Could not find biome {} for spawn, using default", biomeKey)
            RETURN findDefaultSpawnPoint(world)
        }
        
        // Select best spawn location within biome
        FOR (location IN biomeLocations) {
            spawnPos = findSafeSpawnNear(world, location)
            IF (spawnPos != null AND isValidSpawnLocation(world, spawnPos)) {
                RETURN spawnPos
            }
        }
        
        // Fallback if no safe location found
        LOGGER.warn("No safe spawn found in biome {}, using default", biomeKey)
        RETURN findDefaultSpawnPoint(world)
    }
    
    METHOD scanForBiome(World world, RegistryKey<Biome> biomeKey, int radius) -> List<BlockPos> {
        locations = []
        centerX = world.getSpawnPos().getX()
        centerZ = world.getSpawnPos().getZ()
        
        // Spiral search pattern for efficiency
        FOR (distance = 0 TO radius STEP SEARCH_STEP) {
            FOR (angle = 0 TO 360 STEP ANGLE_STEP) {
                x = centerX + distance * cos(angle)
                z = centerZ + distance * sin(angle)
                pos = BlockPos(x, 0, z)
                
                IF (world.getBiome(pos).matchesKey(biomeKey)) {
                    locations.add(pos)
                    IF (locations.size() >= MAX_LOCATIONS) {
                        RETURN locations
                    }
                }
            }
        }
        
        RETURN locations
    }
    
    METHOD isValidSpawnLocation(World world, BlockPos pos) -> boolean {
        // Enhanced spawn validation
        RETURN NOT isInLava(world, pos)
            AND NOT isInVoid(world, pos)
            AND hasGroundSupport(world, pos)
            AND hasBreathingSpace(world, pos)
            AND NOT isInStructure(world, pos, DANGEROUS_STRUCTURES)
    }
}
```

### TDD Anchor Points:
- Priority-based choice resolution
- Biome search algorithms
- Spawn safety validation
- Fallback handling

## 6. Persistence & Migration

### [`SpawnBiomeStorageManager`](fabric/src/main/java/com/beeny/villagesreborn/platform/fabric/spawn/managers/SpawnBiomeStorageManager.java:1) Enhancement

```pseudocode
CLASS SpawnBiomeStorageManager {
    METHOD migrateWorldData(World world) {
        // Check for old static data format
        oldData = checkForLegacyData(world)
        
        IF (oldData != null) {
            LOGGER.info("Migrating legacy spawn biome data for world {}", world.getRegistryKey())
            
            // Convert to new format
            newChoice = SpawnBiomeChoiceData.fromLegacy(oldData)
            setWorldSpawnBiome(world, newChoice)
            
            // Remove old data
            removeLegacyData(world)
            
            LOGGER.info("Migration completed for world {}", world.getRegistryKey())
        }
    }
    
    METHOD setWorldSpawnBiome(World world, SpawnBiomeChoiceData choice) {
        // Enhanced validation
        IF (choice == null OR world == null) {
            LOGGER.warn("Invalid parameters for setWorldSpawnBiome")
            RETURN
        }
        
        // Validate biome exists in world
        IF (NOT BiomeManager.isBiomeAvailable(world, choice.getBiomeKey())) {
            LOGGER.warn("Biome {} not available in world, storing anyway", choice.getBiomeKey())
        }
        
        // Store with metadata
        storage = WorldSpawnBiomeStorage.getInstance(world)
        storage.setChoice(choice)
        
        // Update cache
        updateSpawnChoiceCache(world, choice)
        
        LOGGER.debug("Stored spawn biome choice: {}", choice)
    }
    
    METHOD getWorldSpawnBiome(World world) -> SpawnBiomeChoiceData {
        // Check cache first
        cached = getFromCache(world)
        IF (cached != null) {
            RETURN cached
        }
        
        // Load from storage
        storage = WorldSpawnBiomeStorage.getInstance(world)
        choice = storage.getChoice()
        
        // Update cache
        IF (choice != null) {
            updateSpawnChoiceCache(world, choice)
        }
        
        RETURN choice
    }
    
    METHOD validateStoredChoice(World world, SpawnBiomeChoiceData choice) -> boolean {
        // Comprehensive validation
        IF (choice == null) RETURN false
        
        // Check biome still exists
        IF (NOT BiomeManager.isBiomeAvailable(world, choice.getBiomeKey())) {
            LOGGER.warn("Stored biome {} no longer available", choice.getBiomeKey())
            RETURN false
        }
        
        // Check choice is not too old
        maxAge = TimeUnit.DAYS.toMillis(30) // 30 days
        IF (System.currentTimeMillis() - choice.getTimestamp() > maxAge) {
            LOGGER.info("Stored biome choice is older than 30 days, may be stale")
        }
        
        RETURN true
    }
}
```

### TDD Anchor Points:
- Legacy data migration
- Choice validation and cache management
- Error handling and recovery
- Data consistency checks

## 7. Comprehensive TDD Plan

### Module 1: [`CreateWorldScreenMixin`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/mixin/client/CreateWorldScreenMixin.java:1) Tests

```java
@Test
class CreateWorldScreenMixinTest {
    
    @Test
    void shouldInjectSpawnBiomeButtonWhenEnabled() {
        // Given: Mod config enables spawn biome selection
        // When: Screen initializes
        // Then: Spawn biome button is added exactly once
    }
    
    @Test
    void shouldNotInjectButtonWhenDisabled() {
        // Given: Mod config disables spawn biome selection
        // When: Screen initializes  
        // Then: No spawn biome button is added
    }
    
    @Test
    void shouldPositionButtonInCorrectTabOrder() {
        // Given: Villages Reborn tab exists
        // When: Spawn biome button is injected
        // Then: Button appears after tab with correct tab index
    }
    
    @Test
    void shouldPreventDuplicateButtonInjection() {
        // Given: Screen already has spawn biome button
        // When: init() called again
        // Then: Only one button exists
    }
    
    @Test
    void shouldOpenBiomeSelectorOnButtonClick() {
        // Given: Spawn biome button exists
        // When: Button is clicked
        // Then: BiomeSelectorScreen opens in creation mode
    }
    
    @Test
    void shouldPreventMultipleSelectorScreens() {
        // Given: BiomeSelectorScreen is already open
        // When: Button is clicked again
        // Then: No additional screen opens
    }
}
```

### Module 2: [`BiomeSelectorScreen`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/gui/BiomeSelectorScreen.java:1) Tests

```java
@Test
class BiomeSelectorScreenTest {
    
    @Test
    void shouldCreateInWorldCreationMode() {
        // Given: Parent CreateWorldScreen
        // When: createForWorldCreation() called
        // Then: Screen is in WORLD_CREATION mode
    }
    
    @Test
    void shouldDisplayCorrectBiomesForCreation() {
        // Given: Screen in creation mode
        // When: Screen initializes
        // Then: Only creation-selectable biomes shown
    }
    
    @Test
    void shouldStoreChoiceOnConfirmInCreationMode() {
        // Given: Biome selected in creation mode
        // When: Confirm button clicked
        // Then: Choice stored in WorldCreationSettingsCapture
    }
    
    @Test
    void shouldReturnToParentScreenOnConfirm() {
        // Given: Screen opened from CreateWorldScreen
        // When: Confirm button clicked
        // Then: Returns to parent screen
    }
    
    @Test
    void shouldShowEnhancedTooltipsInCreationMode() {
        // Given: Screen in creation mode
        // When: Hovering over biome widget
        // Then: Creation-specific tooltip displayed
    }
    
    @Test
    void shouldEnablePreviewFunctionality() {
        // Given: Screen with preview enabled
        // When: Hovering over biome widget
        // Then: Preview panel updates with biome info
    }
    
    @Test
    void shouldHandleBiomeSelectionAndVisualUpdate() {
        // Given: Multiple biome widgets
        // When: One biome is selected
        // Then: Visual selection state updates correctly
    }
}
```

### Module 3: [`BiomeSelectionWidget`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/gui/BiomeSelectionWidget.java:1) Tests

```java
@Test  
class BiomeSelectionWidgetTest {
    
    @Test
    void shouldRenderBasicBiomeInfo() {
        // Given: Widget with biome data
        // When: Widget is rendered
        // Then: Icon, name, and basic info displayed
    }
    
    @Test
    void shouldShowCreationModeIndicators() {
        // Given: Widget in creation mode
        // When: Widget is rendered
        // Then: Difficulty and resource indicators shown
    }
    
    @Test
    void shouldTriggerClickHandlerOnClick() {
        // Given: Widget with click handler
        // When: Widget is clicked
        // Then: Click handler called with biome info
    }
    
    @Test
    void shouldDisplayEnhancedTooltips() {
        // Given: Widget with tooltip provider
        // When: Mouse hovers over widget
        // Then: Enhanced tooltip displayed
    }
    
    @Test
    void shouldAnimateHoverEffects() {
        // Given: Widget with hover animations
        // When: Mouse enters/exits widget
        // Then: Smooth animation transitions occur
    }
    
    @Test
    void shouldTriggerPreviewOnHover() {
        // Given: Widget with preview handler
        // When: Mouse hovers over widget
        // Then: Preview handler called with biome
    }
    
    @Test
    void shouldUpdateSelectionVisualState() {
        // Given: Widget selection state changes
        // When: setSelected() called
        // Then: Visual appearance updates appropriately
    }
}
```

### Module 4: [`BiomeSelectorEventHandler`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/spawn/BiomeSelectorEventHandler.java:1) Tests

```java
@Test
class BiomeSelectorEventHandlerTest {
    
    @Test
    void shouldHandleSpawnBiomeButtonClick() {
        // Given: CreateWorldScreen with spawn biome button
        // When: Button click event triggered
        // Then: BiomeSelectorScreen opens
    }
    
    @Test
    void shouldStoreCreationTimeChoice() {
        // Given: Biome choice confirmed in creation mode
        // When: onBiomeChoiceConfirmed() called
        // Then: Choice stored for world creation
    }
    
    @Test
    void shouldApplyChoiceToNewWorld() {
        // Given: Stored creation time choice
        // When: New world is created
        // Then: Choice applied to world spawn data
    }
    
    @Test
    void shouldNotShowSelectorIfCreationChoiceExists() {
        // Given: World has creation time choice
        // When: Player joins world
        // Then: Biome selector not shown
    }
    
    @Test
    void shouldCleanupTempChoicesAfterWorldCreation() {
        // Given: Temporary choice stored during creation
        // When: World creation completes
        // Then: Temporary choice removed from memory
    }
    
    @Test
    void shouldHandleMultipleWorldCreationsConcurrently() {
        // Given: Multiple worlds being created
        // When: Choices made for different worlds
        // Then: Each choice applied to correct world
    }
}
```

### Module 5: [`SpawnPointManager`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/spawn/SpawnPointManager.java:1) Tests

```java
@Test
class SpawnPointManagerTest {
    
    @Test
    void shouldUseStoredBiomeChoiceForSpawn() {
        // Given: World with stored biome choice
        // When: selectSpawnPoint() called
        // Then: Spawn point found in chosen biome
    }
    
    @Test
    void shouldFallbackToDefaultWhenBiomeNotFound() {
        // Given: Stored choice for unavailable biome
        // When: selectSpawnPoint() called
        // Then: Default spawn logic used
    }
    
    @Test
    void shouldPrioritizeWorldChoiceOverPlayerChoice() {
        // Given: Both world and player choices exist
        // When: selectSpawnPoint() called
        // Then: World choice takes precedence
    }
    
    @Test
    void shouldFindSafeSpawnLocationInBiome() {
        // Given: Biome with various terrain
        // When: findSpawnInBiome() called
        // Then: Safe, valid spawn location returned
    }
    
    @Test
    void shouldValidateSpawnLocationSafety() {
        // Given: Potential spawn location
        // When: isValidSpawnLocation() called
        // Then: Accurate safety assessment returned
    }
    
    @Test
    void shouldUseSpiralSearchForBiomeLocations() {
        // Given: World with scattered biome instances
        // When: scanForBiome() called
        // Then: Efficient spiral search finds closest
    }
    
    @Test
    void shouldHandleNoValidSpawnInChosenBiome() {
        // Given: Chosen biome with no safe spawn locations
        // When: findSpawnInBiome() called
        // Then: Graceful fallback to default spawn
    }
}
```

### Module 6: Integration Tests

```java
@Test
class BiomeSelectorIntegrationTest {
    
    @Test
    void shouldCompleteFullCreationWorkflow() {
        // Given: Clean world creation environment
        // When: User opens Create World, clicks Spawn Biome, selects biome, confirms
        // Then: World created with chosen spawn biome
    }
    
    @Test
    void shouldPersistChoiceThroughWorldCreation() {
        // Given: Biome choice made during creation
        // When: World creation completes and player joins
        // Then: Player spawns in chosen biome
    }
    
    @Test
    void shouldNotShowSelectorAfterCreationChoice() {
        // Given: Spawn biome chosen during world creation
        // When: Player joins world for first time
        // Then: BiomeSelectorScreen not shown
    }
    
    @Test
    void shouldMigrateLegacyData() {
        // Given: World with legacy spawn biome data
        // When: World loads with new system
        // Then: Data migrated to new format successfully
    }
    
    @Test
    void shouldHandleModDisabledScenario() {
        // Given: Mod disabled after world creation
        // When: Player joins world
        // Then: No errors, normal spawn behavior
    }
    
    @Test
    void shouldWorkAcrossMinecraftVersions() {
        // Given: World created in one Minecraft version
        // When: Loaded in newer version
        // Then: Spawn biome choice preserved and functional
    }
    
    @Test
    void shouldHandleMultiplayerScenarios() {
        // Given: Multiplayer world with spawn biome choice
        // When: Multiple players join
        // Then: All players respect world spawn biome choice
    }
}
```

## 8. Success Criteria

### Functional Requirements
1. ✅ "Spawn Biome" button appears in Create World screen
2. ✅ Button opens [`BiomeSelectorScreen`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/gui/BiomeSelectorScreen.java:1) in creation mode
3. ✅ Screen displays creation-appropriate biomes with enhanced UI
4. ✅ Choice confirmation stores selection for world creation
5. ✅ New worlds spawn players in chosen biome
6. ✅ Choice persistence survives world creation process
7. ✅ No biome selector shown post-join if creation choice exists
8. ✅ Legacy data migration maintains compatibility

### Technical Requirements
1. ✅ No duplicate UI element injection
2. ✅ Proper tab order and accessibility
3. ✅ Thread-safe choice storage during creation
4. ✅ Efficient biome search algorithms
5. ✅ Graceful fallback for invalid choices
6. ✅ Comprehensive error handling and logging
7. ✅ Memory cleanup after world creation
8. ✅ Configuration-based feature toggles

### Performance Requirements
1. ✅ Button injection < 50ms overhead
2. ✅ Biome search completes < 5 seconds
3. ✅ Screen transitions < 100ms
4. ✅ Memory usage < 10MB during selection
5. ✅ No frame rate impact during creation

## 9. Implementation Notes

### Key Challenges
- **Timing Coordination**: Ensuring spawn biome choice is available when [`SpawnPointManager`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/spawn/SpawnPointManager.java:1) needs it
- **UI Integration**: Seamlessly adding button without disrupting vanilla Create World screen
- **Data Lifecycle**: Managing choice data from creation through world loading
- **Fallback Handling**: Graceful degradation when chosen biome unavailable

### Architecture Decisions
- **Mode-Based Screen**: Single [`BiomeSelectorScreen`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/gui/BiomeSelectorScreen.java:1) class handles both creation and post-join scenarios
- **Temporary Storage**: Choice data temporarily stored during world creation process
- **Priority System**: World-level choices override player-level choices
- **Validation Layers**: Multiple validation points prevent invalid spawn locations

### Future Extensions
- Biome recommendation based on player history
- Preview mode showing biome characteristics
- Advanced filtering and search capabilities
- Integration with world generation parameters
- Custom biome creation support

## 10. File Structure

```
fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/
├── mixin/client/
│   └── CreateWorldScreenMixin.java (Enhanced)
├── gui/
│   ├── BiomeSelectorScreen.java (Enhanced)
│   └── BiomeSelectionWidget.java (Enhanced)
├── spawn/
│   ├── BiomeSelectorEventHandler.java (Enhanced)
│   ├── SpawnPointManager.java (Enhanced)
│   └── managers/
│       └── SpawnBiomeStorageManager.java (Enhanced)
└── biome/
    └── BiomeManager.java (Enhanced)

fabric/src/test/java/com/beeny/villagesreborn/platform/fabric/
├── mixin/
│   └── CreateWorldScreenMixinTest.java (New)
├── gui/
│   ├── BiomeSelectorScreenTest.java (Enhanced)
│   └── BiomeSelectionWidgetTest.java (Enhanced)
├── spawn/
│   ├── BiomeSelectorEventHandlerTest.java (Enhanced)
│   ├── SpawnPointManagerTest.java (Enhanced)
│   └── BiomeSelectorIntegrationTest.java (New)
```

This specification provides comprehensive pseudocode and TDD anchors for implementing Phase 3's spawn biome selector GUI, building upon the existing infrastructure while adding seamless world creation integration.