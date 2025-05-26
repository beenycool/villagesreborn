# Phase 1: Welcome Screen + Hardware/LLM Detection - Specification & TDD Plan

## Overview

Phase 1 implements the first-time setup experience for Villages Reborn, featuring hardware detection, LLM provider configuration, and user onboarding. The implementation builds upon existing infrastructure while enhancing the user experience and adding comprehensive test coverage.

## Architecture Analysis

Based on the existing codebase, the following components are already implemented:
- [`WelcomeScreen.java`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/gui/WelcomeScreen.java): GUI implementation
- [`WelcomeScreenHandler.java`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/gui/WelcomeScreenHandler.java): Lifecycle management
- [`HardwareInfoManager.java`](common/src/main/java/com/beeny/villagesreborn/core/hardware/HardwareInfoManager.java): OSHI-based detection
- [`LLMProviderManager.java`](common/src/main/java/com/beeny/villagesreborn/core/llm/LLMProviderManager.java): Provider & model management
- [`FirstTimeSetupConfig.java`](common/src/main/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfig.java): Configuration persistence
- [`VillagesRebornFabricClient.java`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/VillagesRebornFabricClient.java): Client initialization

## Module Specifications

### 1. WelcomeScreenModule Enhancement

**Purpose**: Enhance the existing welcome screen with improved UX and robust error handling.

**Pseudocode**:
```
MODULE WelcomeScreenModule {
    
    CLASS WelcomeScreenEnhanced EXTENDS WelcomeScreen {
        
        CONSTRUCTOR(hardwareManager, llmManager, setupConfig) {
            CALL super(hardwareManager, llmManager, setupConfig)
            this.validationErrors = []
            this.isProviderValidated = false
            this.recommendedConfig = null
        }
        
        METHOD initializeWithRecommendations() {
            // Enhanced initialization flow
            TRY {
                hardwareInfo = hardwareManager.getHardwareInfo()
                recommendedConfig = generateRecommendedConfig(hardwareInfo)
                
                IF hardwareInfo.tier == HardwareTier.LOW THEN
                    showPerformanceWarning()
                END IF
                
                populateUIWithRecommendations(recommendedConfig)
                enableContinueButton(false) // Disabled until valid selection
                
            } CATCH HardwareDetectionException {
                showFallbackHardwareUI()
                logError("Hardware detection failed, using fallback")
            }
        }
        
        METHOD onProviderChanged(newProvider) {
            validationErrors.clear()
            isProviderValidated = false
            
            // Update model list based on provider and hardware tier
            availableModels = llmManager.getRecommendedModels(newProvider, hardwareInfo.tier)
            updateModelDropdown(availableModels)
            
            // Auto-select first recommended model
            IF availableModels.isNotEmpty() THEN
                selectedModel = availableModels.first()
                updateModelSelection(selectedModel)
            END IF
            
            validateConfiguration()
        }
        
        METHOD validateConfiguration() {
            validationErrors.clear()
            
            // Validate provider selection
            IF selectedProvider == null THEN
                validationErrors.add("Provider must be selected")
            END IF
            
            // Validate model selection
            IF selectedModel == null OR selectedModel.isEmpty() THEN
                validationErrors.add("Model must be selected")
            END IF
            
            // Hardware compatibility check
            IF NOT isModelCompatibleWithHardware(selectedModel, hardwareInfo) THEN
                validationErrors.add("Selected model may not perform well on this hardware")
            END IF
            
            updateUIValidationState()
            enableContinueButton(validationErrors.isEmpty())
        }
        
        METHOD saveConfigurationWithValidation() {
            IF validationErrors.isNotEmpty() THEN
                showValidationErrors()
                RETURN false
            END IF
            
            TRY {
                setupConfig.completeSetup(selectedProvider, selectedModel)
                
                // Log setup completion for analytics
                logSetupCompletion(selectedProvider, selectedModel, hardwareInfo)
                
                showSuccessMessage()
                closeScreen()
                RETURN true
                
            } CATCH ConfigurationSaveException {
                showErrorDialog("Failed to save configuration. Please try again.")
                RETURN false
            }
        }
        
        PRIVATE METHOD generateRecommendedConfig(hardwareInfo) {
            RETURN SWITCH hardwareInfo.tier {
                CASE HIGH -> RecommendedConfig(LLMProvider.OPENAI, "gpt-4")
                CASE MEDIUM -> RecommendedConfig(LLMProvider.OPENAI, "gpt-3.5-turbo")
                CASE LOW -> RecommendedConfig(LLMProvider.LOCAL, "llama2:7b")
                DEFAULT -> RecommendedConfig(LLMProvider.OPENAI, "gpt-3.5-turbo")
            }
        }
    }
}
```

### 2. HardwareInfoManager Integration Enhancement

**Purpose**: Enhance hardware detection with better error handling and tier determination.

**Pseudocode**:
```
MODULE HardwareInfoManagerEnhanced {
    
    CLASS HardwareInfoManager {
        
        METHOD getHardwareInfoWithFallback() {
            TRY {
                IF cachedHardwareInfo != null THEN
                    RETURN cachedHardwareInfo
                END IF
                
                detectedInfo = detectHardwareInfo()
                
                // Validate detection results
                IF NOT isValidHardwareInfo(detectedInfo) THEN
                    THROW HardwareDetectionException("Invalid hardware detection results")
                END IF
                
                cachedHardwareInfo = detectedInfo
                RETURN detectedInfo
                
            } CATCH Exception {
                logWarning("Hardware detection failed, using fallback configuration")
                RETURN createFallbackHardwareInfo()
            }
        }
        
        METHOD detectHardwareInfoWithRetry() {
            maxRetries = 3
            
            FOR attempt = 1 TO maxRetries {
                TRY {
                    hardware = systemInfo.getHardware()
                    
                    ramGB = detectRAM(hardware.getMemory())
                    cpuCores = detectCPUCores(hardware.getProcessor())
                    hasAvx2 = detectAVX2Support(hardware.getProcessor())
                    
                    // Enhanced tier classification with performance scoring
                    performanceScore = calculatePerformanceScore(ramGB, cpuCores, hasAvx2)
                    tier = classifyHardwareTier(performanceScore)
                    
                    info = HardwareInfo(ramGB, cpuCores, hasAvx2, tier, performanceScore)
                    
                    // Validate minimum requirements
                    validateMinimumRequirements(info)
                    
                    RETURN info
                    
                } CATCH Exception {
                    IF attempt == maxRetries THEN
                        THROW HardwareDetectionException("Failed after " + maxRetries + " attempts")
                    END IF
                    
                    sleep(1000 * attempt) // Exponential backoff
                }
            }
        }
        
        PRIVATE METHOD calculatePerformanceScore(ramGB, cpuCores, hasAvx2) {
            score = 0
            
            // RAM scoring (40% weight)
            ramScore = min(ramGB / 32.0, 1.0) * 40
            score += ramScore
            
            // CPU cores scoring (40% weight)
            coreScore = min(cpuCores / 16.0, 1.0) * 40
            score += coreScore
            
            // AVX2 support (20% weight)
            avxScore = hasAvx2 ? 20 : 0
            score += avxScore
            
            RETURN score
        }
        
        PRIVATE METHOD classifyHardwareTier(performanceScore) {
            RETURN SWITCH {
                CASE performanceScore >= 80 -> HardwareTier.HIGH
                CASE performanceScore >= 50 -> HardwareTier.MEDIUM
                CASE performanceScore >= 25 -> HardwareTier.LOW
                DEFAULT -> HardwareTier.UNKNOWN
            }
        }
    }
}
```

### 3. LLMProviderManager Enhancement

**Purpose**: Expand provider management with better model recommendations and validation.

**Pseudocode**:
```
MODULE LLMProviderManagerEnhanced {
    
    CLASS LLMProviderManager {
        
        METHOD getRecommendedModelsWithReasoning(provider, hardwareTier) {
            allModels = getAvailableModels(provider)
            
            RETURN SWITCH hardwareTier {
                CASE HIGH -> ModelRecommendation(
                    models: allModels,
                    reason: "Your hardware can handle all available models",
                    performance: "Excellent"
                )
                CASE MEDIUM -> ModelRecommendation(
                    models: filterModelsForMediumTier(allModels),
                    reason: "Optimized for your hardware configuration",
                    performance: "Good"
                )
                CASE LOW -> ModelRecommendation(
                    models: filterModelsForLowTier(allModels),
                    reason: "Lightweight models recommended for your hardware",
                    performance: "Basic"
                )
                DEFAULT -> ModelRecommendation(
                    models: filterModelsForLowTier(allModels),
                    reason: "Conservative selection due to unknown hardware",
                    performance: "Basic"
                )
            }
        }
        
        METHOD validateProviderCompatibility(provider, hardwareTier) {
            RETURN SWITCH provider {
                CASE LOCAL -> hardwareTier != HardwareTier.LOW ? 
                    ValidationResult.COMPATIBLE : 
                    ValidationResult.WARNING("Local models may be slow on low-tier hardware")
                    
                CASE OPENAI, ANTHROPIC -> ValidationResult.COMPATIBLE
                
                CASE GROQ -> hardwareTier == HardwareTier.HIGH ?
                    ValidationResult.OPTIMAL :
                    ValidationResult.COMPATIBLE
                    
                DEFAULT -> ValidationResult.UNKNOWN
            }
        }
        
        METHOD estimateModelPerformance(model, hardwareInfo) {
            // Performance estimation based on model requirements and hardware
            modelRequirements = getModelRequirements(model)
            
            ramUtilization = modelRequirements.minRamGB / hardwareInfo.ramGB
            cpuUtilization = modelRequirements.minCores / hardwareInfo.cpuCores
            
            IF ramUtilization > 0.8 OR cpuUtilization > 0.8 THEN
                RETURN PerformanceEstimate.POOR
            ELSE IF ramUtilization > 0.5 OR cpuUtilization > 0.5 THEN
                RETURN PerformanceEstimate.MODERATE
            ELSE
                RETURN PerformanceEstimate.GOOD
            END IF
        }
    }
    
    CLASS ModelRecommendation {
        models: List<String>
        reason: String
        performance: String
        warnings: List<String>
    }
    
    ENUM ValidationResult {
        COMPATIBLE, OPTIMAL, WARNING(message), INCOMPATIBLE(reason)
    }
    
    ENUM PerformanceEstimate {
        POOR, MODERATE, GOOD, EXCELLENT
    }
}
```

### 4. Configuration Persistence Enhancement

**Purpose**: Enhance configuration management with better error handling and migration support.

**Pseudocode**:
```
MODULE FirstTimeSetupConfigEnhanced {
    
    CLASS FirstTimeSetupConfig {
        
        METHOD completeSetupWithValidation(provider, model) {
            // Validate inputs
            validateProvider(provider)
            validateModel(model)
            
            // Create backup of existing config
            IF configFileExists() THEN
                createConfigBackup()
            END IF
            
            TRY {
                this.setupCompleted = true
                this.selectedProvider = provider
                this.selectedModel = model
                this.hardwareDetectionCompleted = true
                this.setupTimestamp = getCurrentTimestamp()
                this.configVersion = getCurrentConfigVersion()
                
                saveWithRetry()
                
                // Verify save was successful
                verifyConfigSaved()
                
                logSetupCompletion(provider, model)
                
            } CATCH Exception {
                // Restore from backup if save failed
                restoreConfigBackup()
                THROW ConfigurationSaveException("Failed to save setup configuration")
            }
        }
        
        METHOD loadWithMigration() {
            IF NOT configFileExists() THEN
                RETURN createDefaultConfig()
            END IF
            
            TRY {
                rawConfig = loadRawConfig()
                configVersion = detectConfigVersion(rawConfig)
                
                IF configVersion < getCurrentConfigVersion() THEN
                    migratedConfig = migrateConfig(rawConfig, configVersion)
                    RETURN createFromProperties(migratedConfig)
                ELSE
                    RETURN createFromProperties(rawConfig)
                END IF
                
            } CATCH Exception {
                logError("Failed to load configuration, using defaults")
                RETURN createDefaultConfig()
            }
        }
        
        PRIVATE METHOD saveWithRetry() {
            maxRetries = 3
            
            FOR attempt = 1 TO maxRetries {
                TRY {
                    properties = createPropertiesFromConfig()
                    configPath = getConfigPath()
                    
                    // Ensure parent directory exists
                    createDirectories(configPath.parent)
                    
                    // Write to temporary file first
                    tempPath = configPath + ".tmp"
                    writeProperties(properties, tempPath)
                    
                    // Atomic move to final location
                    moveFile(tempPath, configPath)
                    
                    RETURN // Success
                    
                } CATCH IOException {
                    IF attempt == maxRetries THEN
                        THROW ConfigurationSaveException("Failed to save after " + maxRetries + " attempts")
                    END IF
                    
                    sleep(1000 * attempt)
                }
            }
        }
        
        PRIVATE METHOD migrateConfig(rawConfig, fromVersion) {
            RETURN SWITCH fromVersion {
                CASE 1 -> migrateFromV1ToV2(rawConfig)
                CASE 2 -> migrateFromV2ToV3(rawConfig)
                DEFAULT -> rawConfig // No migration needed
            }
        }
    }
}
```

### 5. Client Integration Enhancement

**Purpose**: Improve the client-side integration with better event handling and lifecycle management.

**Pseudocode**:
```
MODULE VillagesRebornFabricClientEnhanced {
    
    CLASS VillagesRebornFabricClient {
        
        METHOD onInitializeClient() {
            TRY {
                initializeLogging()
                validateClientEnvironment()
                setupClientFeatures()
                registerEventHandlers()
                
                logClientInitialization()
                
            } CATCH Exception {
                handleInitializationFailure(exception)
                THROW RuntimeException("Client initialization failed")
            }
        }
        
        METHOD setupClientFeaturesEnhanced() {
            // Initialize components with dependency injection
            hardwareManager = HardwareInfoManager.getInstance()
            llmManager = createLLMProviderManager()
            setupConfig = FirstTimeSetupConfig.loadWithMigration()
            
            // Initialize welcome screen handler with enhanced features
            welcomeScreenHandler = WelcomeScreenHandler(
                hardwareManager, 
                llmManager, 
                setupConfig,
                createUIThemeManager()
            )
            
            // Pre-warm hardware detection in background thread
            CompletableFuture.runAsync(() -> {
                hardwareManager.getHardwareInfoWithFallback()
            })
        }
        
        METHOD registerEventHandlersEnhanced() {
            // World join event with improved timing
            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
                scheduleWelcomeScreenCheck(client)
            })
            
            // World disconnect event
            ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
                welcomeScreenHandler.onWorldLeave()
            })
            
            // Client lifecycle events
            ClientLifecycleEvents.CLIENT_STARTED.register((client) -> {
                welcomeScreenHandler.onClientStarted()
            })
            
            ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> {
                welcomeScreenHandler.onClientStopping()
            })
        }
        
        PRIVATE METHOD scheduleWelcomeScreenCheck(client) {
            // Use proper scheduling to avoid timing issues
            client.execute(() -> {
                Timer.schedule(() -> {
                    IF client.world != null AND client.player != null THEN
                        welcomeScreenHandler.checkAndShowWelcomeScreen()
                    END IF
                }, 2000) // 2 second delay to ensure world is fully loaded
            })
        }
    }
}
```

## Test-Driven Development Plan

### 1. Unit Tests

#### 1.1 WelcomeScreen Unit Tests
```java
// File: fabric/src/test/java/com/beeny/villagesreborn/platform/fabric/gui/WelcomeScreenTest.java

CLASS WelcomeScreenTest {
    
    @Test
    METHOD testProviderSelectionUpdatesModelDropdown() {
        // GIVEN: Welcome screen with mocked dependencies
        mockHardwareManager = mock(HardwareInfoManager.class)
        mockLLMManager = mock(LLMProviderManager.class)
        mockSetupConfig = mock(FirstTimeSetupConfig.class)
        
        when(mockHardwareManager.getHardwareInfo()).thenReturn(createHighTierHardware())
        when(mockLLMManager.getRecommendedModels(OPENAI, HIGH)).thenReturn(["gpt-4", "gpt-3.5-turbo"])
        
        welcomeScreen = new WelcomeScreen(mockHardwareManager, mockLLMManager, mockSetupConfig)
        
        // WHEN: Provider is changed to OpenAI
        welcomeScreen.onProviderChanged(LLMProvider.OPENAI)
        
        // THEN: Model dropdown is updated with recommended models
        assertThat(welcomeScreen.getAvailableModels()).containsExactly("gpt-4", "gpt-3.5-turbo")
        assertThat(welcomeScreen.getSelectedModel()).isEqualTo("gpt-4")
    }
    
    @Test
    METHOD testContinueButtonDisabledWithoutSelection() {
        // GIVEN: Welcome screen with no selection
        welcomeScreen = createWelcomeScreen()
        
        // WHEN: Screen is initialized
        welcomeScreen.init()
        
        // THEN: Continue button is disabled
        assertThat(welcomeScreen.getContinueButton().isEnabled()).isFalse()
    }
    
    @Test
    METHOD testValidationErrorsPreventSave() {
        // GIVEN: Welcome screen with invalid configuration
        welcomeScreen = createWelcomeScreen()
        welcomeScreen.setSelectedProvider(null)
        
        // WHEN: User attempts to save
        result = welcomeScreen.saveConfiguration()
        
        // THEN: Save fails and errors are displayed
        assertThat(result).isFalse()
        assertThat(welcomeScreen.getValidationErrors()).isNotEmpty()
    }
}
```

#### 1.2 HardwareInfoManager Unit Tests
```java
// File: common/src/test/java/com/beeny/villagesreborn/core/hardware/HardwareInfoManagerEnhancedTest.java

CLASS HardwareInfoManagerEnhancedTest {
    
    @Test
    METHOD testHighTierHardwareDetection() {
        // GIVEN: System with high-end hardware
        mockSystemInfo = createMockSystemInfo(ramGB: 32, cpuCores: 16, hasAvx2: true)
        hardwareManager = new HardwareInfoManager(mockSystemInfo)
        
        // WHEN: Hardware is detected
        hardwareInfo = hardwareManager.getHardwareInfo()
        
        // THEN: Hardware is classified as HIGH tier
        assertThat(hardwareInfo.getHardwareTier()).isEqualTo(HardwareTier.HIGH)
        assertThat(hardwareInfo.getRamGB()).isEqualTo(32)
        assertThat(hardwareInfo.getCpuCores()).isEqualTo(16)
        assertThat(hardwareInfo.hasAvx2Support()).isTrue()
    }
    
    @Test
    METHOD testFallbackOnDetectionFailure() {
        // GIVEN: SystemInfo that throws exceptions
        mockSystemInfo = mock(SystemInfo.class)
        when(mockSystemInfo.getHardware()).thenThrow(new RuntimeException("Detection failed"))
        
        hardwareManager = new HardwareInfoManager(mockSystemInfo)
        
        // WHEN: Hardware detection is attempted
        hardwareInfo = hardwareManager.getHardwareInfo()
        
        // THEN: Fallback configuration is returned
        assertThat(hardwareInfo).isNotNull()
        assertThat(hardwareInfo.getHardwareTier()).isEqualTo(HardwareTier.UNKNOWN)
    }
    
    @Test
    METHOD testPerformanceScoreCalculation() {
        // GIVEN: Hardware with known specifications
        ramGB = 16
        cpuCores = 8
        hasAvx2 = true
        
        // WHEN: Performance score is calculated
        score = HardwareInfoManager.calculatePerformanceScore(ramGB, cpuCores, hasAvx2)
        
        // THEN: Score reflects hardware capabilities
        expectedScore = (16/32.0 * 40) + (8/16.0 * 40) + 20 // 20 + 20 + 20 = 60
        assertThat(score).isEqualTo(60.0, within(0.1))
    }
}
```

#### 1.3 LLMProviderManager Unit Tests
```java
// File: common/src/test/java/com/beeny/villagesreborn/core/llm/LLMProviderManagerEnhancedTest.java

CLASS LLMProviderManagerEnhancedTest {
    
    @Test
    METHOD testRecommendedModelsForHighTierHardware() {
        // GIVEN: LLM provider manager and high-tier hardware
        mockApiClient = mock(LLMApiClient.class)
        llmManager = new LLMProviderManager(mockApiClient)
        
        // WHEN: Recommendations are requested for high-tier hardware
        recommendations = llmManager.getRecommendedModelsWithReasoning(OPENAI, HIGH)
        
        // THEN: All models are recommended
        assertThat(recommendations.getModels()).contains("gpt-4", "gpt-3.5-turbo")
        assertThat(recommendations.getPerformance()).isEqualTo("Excellent")
    }
    
    @Test
    METHOD testModelFilteringForLowTierHardware() {
        // GIVEN: LLM provider manager
        llmManager = new LLMProviderManager(mock(LLMApiClient.class))
        
        // WHEN: Models are filtered for low-tier hardware
        filteredModels = llmManager.getRecommendedModels(OPENAI, LOW)
        
        // THEN: Only lightweight models are included
        assertThat(filteredModels).contains("gpt-3.5-turbo")
        assertThat(filteredModels).doesNotContain("gpt-4")
    }
    
    @Test
    METHOD testProviderCompatibilityValidation() {
        // GIVEN: LLM provider manager
        llmManager = new LLMProviderManager(mock(LLMApiClient.class))
        
        // WHEN: Compatibility is checked for LOCAL provider on LOW hardware
        result = llmManager.validateProviderCompatibility(LOCAL, LOW)
        
        // THEN: Warning is returned
        assertThat(result.getType()).isEqualTo(ValidationResult.Type.WARNING)
        assertThat(result.getMessage()).contains("may be slow")
    }
}
```

#### 1.4 Configuration Persistence Tests
```java
// File: common/src/test/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfigEnhancedTest.java

CLASS FirstTimeSetupConfigEnhancedTest {
    
    @Test
    METHOD testConfigurationSaveWithRetry() {
        // GIVEN: Setup config and temporary I/O failure
        tempDir = createTempDirectory()
        config = FirstTimeSetupConfig.create()
        
        // Mock file system to fail first attempt
        mockFileSystem = mockFileSystemWithFailure(attempts: 1)
        
        // WHEN: Configuration is saved
        config.completeSetupWithValidation(OPENAI, "gpt-3.5-turbo")
        
        // THEN: Save succeeds after retry
        savedConfig = FirstTimeSetupConfig.loadFrom(tempDir)
        assertThat(savedConfig.getSelectedProvider()).isEqualTo(OPENAI)
        assertThat(savedConfig.getSelectedModel()).isEqualTo("gpt-3.5-turbo")
    }
    
    @Test
    METHOD testConfigMigrationFromOldVersion() {
        // GIVEN: Old version config file
        oldConfigFile = createOldVersionConfig(version: 1)
        
        // WHEN: Config is loaded
        config = FirstTimeSetupConfig.loadWithMigration()
        
        // THEN: Config is migrated to current version
        assertThat(config.getConfigVersion()).isEqualTo(getCurrentConfigVersion())
        assertThat(config.isSetupCompleted()).isTrue()
    }
    
    @Test
    METHOD testBackupAndRestoreOnSaveFailure() {
        // GIVEN: Existing config and save failure scenario
        existingConfig = createExistingConfig()
        mockPersistentFailure()
        
        // WHEN: Save is attempted and fails
        exception = assertThrows(ConfigurationSaveException.class, () -> {
            existingConfig.completeSetupWithValidation(ANTHROPIC, "claude-3")
        })
        
        // THEN: Original config is restored
        restoredConfig = FirstTimeSetupConfig.load()
        assertThat(restoredConfig).isEqualTo(existingConfig)
    }
}
```

### 2. Integration Tests

#### 2.1 Welcome Screen Handler Integration Test
```java
// File: fabric/src/test/java/com/beeny/villagesreborn/platform/fabric/gui/WelcomeScreenHandlerIntegrationTest.java

CLASS WelcomeScreenHandlerIntegrationTest {
    
    @Test
    METHOD testCompleteWelcomeScreenFlow() {
        // GIVEN: Fresh installation (no config file)
        deleteConfigFile()
        welcomeHandler = new WelcomeScreenHandler()
        
        // WHEN: Player joins world for first time
        welcomeHandler.onWorldJoin()
        
        // THEN: Welcome screen is displayed
        verify(mockedMinecraftClient).setScreen(any(WelcomeScreen.class))
        
        // WHEN: User completes setup
        welcomeScreen = captureDisplayedScreen()
        welcomeScreen.selectProvider(OPENAI)
        welcomeScreen.selectModel("gpt-3.5-turbo")
        welcomeScreen.clickContinue()
        
        // THEN: Configuration is saved and screen closes
        config = FirstTimeSetupConfig.load()
        assertThat(config.isSetupCompleted()).isTrue()
        assertThat(config.getSelectedProvider()).isEqualTo(OPENAI)
    }
    
    @Test
    METHOD testWelcomeScreenNotShownAfterCompletion() {
        // GIVEN: Completed setup configuration
        config = FirstTimeSetupConfig.create()
        config.completeSetup(ANTHROPIC, "claude-3")
        
        welcomeHandler = new WelcomeScreenHandler()
        
        // WHEN: Player joins world
        welcomeHandler.onWorldJoin()
        
        // THEN: Welcome screen is not displayed
        verify(mockedMinecraftClient, never()).setScreen(any(WelcomeScreen.class))
    }
}
```

#### 2.2 End-to-End Client Integration Test
```java
// File: fabric/src/test/java/com/beeny/villagesreborn/platform/fabric/ClientIntegrationTest.java

CLASS ClientIntegrationTest {
    
    @Test
    METHOD testClientInitializationWithFirstTimeSetup() {
        // GIVEN: Test Minecraft client environment
        testClient = createTestMinecraftClient()
        
        // WHEN: Villages Reborn client initializes
        villagesRebornClient = new VillagesRebornFabricClient()
        villagesRebornClient.onInitializeClient()
        
        // AND: Player joins world
        simulateWorldJoin(testClient)
        
        // THEN: Welcome screen is displayed
        assertThat(testClient.getCurrentScreen()).isInstanceOf(WelcomeScreen.class)
        
        // WHEN: Setup is completed
        welcomeScreen = (WelcomeScreen) testClient.getCurrentScreen()
        completeSetup(welcomeScreen, OPENAI, "gpt-3.5-turbo")
        
        // THEN: Screen closes and config is persisted
        assertThat(testClient.getCurrentScreen()).isNull()
        
        // WHEN: Player rejoins world
        simulateWorldLeave(testClient)
        simulateWorldJoin(testClient)
        
        // THEN: Welcome screen is not shown again
        assertThat(testClient.getCurrentScreen()).isNull()
    }
}
```

### 3. Performance Tests

#### 3.1 Hardware Detection Performance Test
```java
// File: common/src/test/java/com/beeny/villagesreborn/core/hardware/HardwareDetectionPerformanceTest.java

CLASS HardwareDetectionPerformanceTest {
    
    @Test
    METHOD testHardwareDetectionPerformance() {
        // GIVEN: Hardware info manager
        hardwareManager = new HardwareInfoManager()
        
        // WHEN: Hardware detection is performed multiple times
        long startTime = System.currentTimeMillis()
        
        for (int i = 0; i < 100; i++) {
            hardwareInfo = hardwareManager.getHardwareInfo()
            assertThat(hardwareInfo).isNotNull()
        }
        
        long endTime = System.currentTimeMillis()
        
        // THEN: Detection completes within reasonable time
        long totalTime = endTime - startTime
        assertThat(totalTime).isLessThan(5000) // 5 seconds for 100 detections
        
        // AND: Subsequent calls use cached result
        long cacheStartTime = System.currentTimeMillis()
        hardwareManager.getHardwareInfo()
        long cacheEndTime = System.currentTimeMillis()
        
        assertThat(cacheEndTime - cacheStartTime).isLessThan(10) // < 10ms for cached result
    }
}
```

### 4. Error Handling Tests

#### 4.1 Graceful Degradation Tests
```java
// File: common/src/test/java/com/beeny/villagesreborn/core/ErrorHandlingTest.java

CLASS ErrorHandlingTest {
    
    @Test
    METHOD testGracefulDegradationOnHardwareDetectionFailure() {
        // GIVEN: Hardware manager that fails detection
        failingHardwareManager = createFailingHardwareManager()
        llmManager = mock(LLMProviderManager.class)
        setupConfig = mock(FirstTimeSetupConfig.class)
        
        // WHEN: Welcome screen is created
        welcomeScreen = new WelcomeScreen(failingHardwareManager, llmManager, setupConfig)
        
        // THEN: Screen initializes with fallback configuration
        assertThat(welcomeScreen).isNotNull()
        assertThat(welcomeScreen.getHardwareInfo().getHardwareTier()).isEqualTo(HardwareTier.UNKNOWN)
    }
    
    @Test
    METHOD testConfigurationSaveFailureHandling() {
        // GIVEN: Setup config that fails to save
        failingSetupConfig = createFailingSetupConfig()
        welcomeScreen = createWelcomeScreenWithConfig(failingSetupConfig)
        
        // WHEN: User attempts to save configuration
        welcomeScreen.selectProvider(OPENAI)
        welcomeScreen.selectModel("gpt-3.5-turbo")
        result = welcomeScreen.saveConfiguration()
        
        // THEN: User is informed of the error
        assertThat(result).isFalse()
        assertThat(welcomeScreen.getErrorMessage()).contains("Failed to save configuration")
    }
}
```

## Implementation Priorities

### Phase 1.1: Core Enhancements (Week 1)
1. Enhance [`WelcomeScreen.java`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/gui/WelcomeScreen.java) with validation and error handling
2. Improve [`HardwareInfoManager.java`](common/src/main/java/com/beeny/villagesreborn/core/hardware/HardwareInfoManager.java) with performance scoring
3. Enhance [`LLMProviderManager.java`](common/src/main/java/com/beeny/villagesreborn/core/llm/LLMProviderManager.java) with reasoning
4. Implement comprehensive unit tests for all components

### Phase 1.2: Integration & Polish (Week 2)
1. Enhance [`FirstTimeSetupConfig.java`](common/src/main/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfig.java) with migration support
2. Improve [`VillagesRebornFabricClient.java`](fabric/src/client/java/com/beeny/villagesreborn/platform/fabric/VillagesRebornFabricClient.java) event handling
3. Implement integration tests and performance tests
4. Add comprehensive error handling and graceful degradation

### Phase 1.3: User Experience & Testing (Week 3)
1. Implement user experience improvements (themes, animations, help text)
2. Add comprehensive logging and analytics
3. Implement end-to-end tests
4. Performance optimization and caching improvements

## Success Criteria

- [ ] Welcome screen displays on first launch with hardware detection results
- [ ] Provider and model selection works correctly with hardware-based recommendations
- [ ] Configuration persists correctly to FabricLoader config directory
- [ ] Setup completion prevents welcome screen from showing on subsequent launches
- [ ] All tests pass with >90% code coverage
- [ ] Graceful degradation when hardware detection fails
- [ ] Performance meets requirements (< 2 second initialization, < 100ms UI response)
- [ ] User experience is intuitive and informative

## Technical Debt & Future Considerations

1. **Hardware Detection**: Current AVX2 detection is simplified - future versions should use JNI for precise feature detection
2. **Model Performance Estimation**: Implement more sophisticated performance prediction based on actual benchmarks
3. **Configuration Migration**: Current migration is basic - implement schema-based migration for complex version changes
4. **UI Themes**: Current implementation is basic - future versions should support full theme customization
5. **Accessibility**: Ensure welcome screen meets accessibility standards for keyboard navigation and screen readers

This specification provides a comprehensive foundation for implementing Phase 1 with robust testing, error handling, and user experience considerations.