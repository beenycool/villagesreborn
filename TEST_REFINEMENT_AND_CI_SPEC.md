# Test Refinement & CI Stabilization Specification

## Overview

This specification defines the modular pseudocode and TDD plan for stabilizing the Villages Reborn test suite, addressing failing tests, dependency issues, mock utilities, and CI pipeline reliability.

## Problem Analysis

### Current Issues Identified

1. **Missing AI Classes**: Tests reference `PersonalityProfile` and `RelationshipData` from [`com.beeny.villagesreborn.core.ai`](common/src/main/java/com/beeny/villagesreborn/core/ai/) but these classes exist, indicating potential import or dependency resolution issues.

2. **Disabled Tests**: Multiple test files with `.disabled` extensions in fabric module:
   - [`SpawnBiomeStorageIntegrationTest.java.disabled`](fabric/src/test/java/com/beeny/villagesreborn/platform/fabric/spawn/SpawnBiomeStorageIntegrationTest.java.disabled)
   - [`SpawnBiomeStorageManagerTest.java.disabled`](fabric/src/test/java/com/beeny/villagesreborn/platform/fabric/spawn/managers/SpawnBiomeStorageManagerTest.java.disabled)  
   - [`WorldSpawnBiomeStorageTest.java.disabled`](fabric/src/test/java/com/beeny/villagesreborn/platform/fabric/spawn/storage/WorldSpawnBiomeStorageTest.java.disabled)

3. **Mock Infrastructure**: [`FabricLoaderMockUtils`](common/src/test/java/com/beeny/villagesreborn/core/config/FabricLoaderMockUtils.java) exists but may need enhancement for comprehensive mocking.

4. **Test Framework**: Both modules use JUnit 5 with proper dependencies, but consistency and extension configuration may need improvement.

---

## Module 1: Test Failure Analysis Engine

```pseudocode
MODULE TestFailureAnalysisEngine:
  INTERFACE TestReportScanner:
    FUNCTION scanTestReports(reportPaths: List<String>) -> List<TestFailure>
    FUNCTION categorizeFailures(failures: List<TestFailure>) -> Map<FailureCategory, List<TestFailure>>
  
  CLASS TestFailureAnalyzer:
    PROPERTIES:
      reportScanner: TestReportScanner
      failureCategories: Set<FailureCategory> = {
        MISSING_IMPORTS,
        MOCK_CONFIGURATION,
        ENVIRONMENT_SETUP,
        DEPENDENCY_RESOLUTION,
        COMPILATION_ERROR,
        RUNTIME_ERROR
      }
    
    FUNCTION analyzeTestSuite() -> TestAnalysisReport:
      commonFailures = reportScanner.scanTestReports(["common/build/test-results"])
      fabricFailures = reportScanner.scanTestReports(["fabric/build/test-results"])
      
      allFailures = commonFailures + fabricFailures
      categorizedFailures = reportScanner.categorizeFailures(allFailures)
      
      RETURN TestAnalysisReport(
        totalFailures: allFailures.size(),
        categorizedFailures: categorizedFailures,
        rootCauseAnalysis: analyzeRootCauses(categorizedFailures),
        recommendations: generateFixRecommendations(categorizedFailures)
      )
    
    FUNCTION analyzeRootCauses(failures: Map<FailureCategory, List<TestFailure>>) -> List<RootCause>:
      rootCauses = []
      
      FOR category, failureList IN failures:
        SWITCH category:
          CASE MISSING_IMPORTS:
            rootCauses.add(analyzeImportIssues(failureList))
          CASE MOCK_CONFIGURATION:
            rootCauses.add(analyzeMockIssues(failureList))
          CASE ENVIRONMENT_SETUP:
            rootCauses.add(analyzeEnvironmentIssues(failureList))
      
      RETURN rootCauses

  ENUM FailureCategory:
    MISSING_IMPORTS, MOCK_CONFIGURATION, ENVIRONMENT_SETUP,
    DEPENDENCY_RESOLUTION, COMPILATION_ERROR, RUNTIME_ERROR

  DATA CLASS TestFailure:
    testClass: String
    testMethod: String
    failureMessage: String
    stackTrace: String
    category: FailureCategory
    
  DATA CLASS TestAnalysisReport:
    totalFailures: Integer
    categorizedFailures: Map<FailureCategory, List<TestFailure>>
    rootCauseAnalysis: List<RootCause>
    recommendations: List<String>
```

**TDD Anchor**: [`TestFailureAnalysisEngineTest`](common/src/test/java/com/beeny/villagesreborn/core/testing/TestFailureAnalysisEngineTest.java)

---

## Module 2: Dependency Resolution & Import Correction Engine

```pseudocode
MODULE DependencyResolutionEngine:
  CLASS DependencyValidator:
    PROPERTIES:
      commonBuildGradle: Path = "common/build.gradle"
      fabricBuildGradle: Path = "fabric/build.gradle"
      requiredTestDependencies: Set<String> = {
        "org.junit.jupiter:junit-jupiter:5.10.0",
        "org.assertj:assertj-core:3.24.2",
        "org.mockito:mockito-core:5.6.0",
        "org.mockito:mockito-junit-jupiter:5.6.0"
      }
    
    FUNCTION validateTestDependencies() -> DependencyValidationReport:
      commonDeps = parseBuildGradle(commonBuildGradle)
      fabricDeps = parseBuildGradle(fabricBuildGradle)
      
      missingCommonDeps = requiredTestDependencies - commonDeps.testDependencies
      missingFabricDeps = requiredTestDependencies - fabricDeps.testDependencies
      
      RETURN DependencyValidationReport(
        commonMissing: missingCommonDeps,
        fabricMissing: missingFabricDeps,
        versionConflicts: detectVersionConflicts(commonDeps, fabricDeps)
      )
    
    FUNCTION addMissingDependencies(report: DependencyValidationReport) -> Void:
      IF report.commonMissing.isNotEmpty():
        updateBuildGradle(commonBuildGradle, report.commonMissing)
      
      IF report.fabricMissing.isNotEmpty():
        updateBuildGradle(fabricBuildGradle, report.fabricMissing)

  CLASS ImportCorrector:
    PROPERTIES:
      javaSourcePaths: List<Path> = [
        "common/src/test/java",
        "fabric/src/test/java"
      ]
    
    FUNCTION correctMissingImports() -> ImportCorrectionReport:
      allJavaFiles = scanJavaFiles(javaSourcePaths)
      corrections = []
      
      FOR javaFile IN allJavaFiles:
        missingImports = detectMissingImports(javaFile)
        
        FOR missingImport IN missingImports:
          correction = resolveImport(missingImport)
          IF correction.isResolved():
            applyImportCorrection(javaFile, correction)
            corrections.add(correction)
      
      RETURN ImportCorrectionReport(corrections)
    
    FUNCTION resolveImport(className: String) -> ImportCorrection:
      // Known problematic imports and their resolutions
      knownResolutions = {
        "PersonalityProfile": "com.beeny.villagesreborn.core.ai.PersonalityProfile",
        "RelationshipData": "com.beeny.villagesreborn.core.ai.RelationshipData",
        "FabricLoader": "net.fabricmc.loader.api.FabricLoader"
      }
      
      IF knownResolutions.contains(className):
        RETURN ImportCorrection(
          originalClass: className,
          resolvedImport: knownResolutions[className],
          isResolved: true
        )
      
      RETURN ImportCorrection(className, null, false)

  DATA CLASS DependencyValidationReport:
    commonMissing: Set<String>
    fabricMissing: Set<String>
    versionConflicts: List<VersionConflict>
    
  DATA CLASS ImportCorrection:
    originalClass: String
    resolvedImport: String
    isResolved: Boolean
```

**TDD Anchor**: [`DependencyResolutionEngineTest`](common/src/test/java/com/beeny/villagesreborn/core/testing/DependencyResolutionEngineTest.java)

---

## Module 3: Enhanced Mock Utilities System

```pseudocode
MODULE EnhancedMockUtilities:
  CLASS FabricLoaderMockUtilsEnhanced EXTENDS FabricLoaderMockUtils:
    FUNCTION createHeadlessFabricEnvironment() -> FabricTestEnvironment:
      mockLoader = mockStatic(FabricLoader.class)
      mockGameDir = createTempDirectory("fabric-test-game")
      mockConfigDir = mockGameDir.resolve("config")
      
      mockLoader.when(FabricLoader::getInstance).thenReturn(createMockInstance())
      
      RETURN FabricTestEnvironment(
        mockLoader: mockLoader,
        gameDirectory: mockGameDir,
        configDirectory: mockConfigDir,
        cleanup: () -> mockLoader.close()
      )
    
    FUNCTION createMockInstance() -> FabricLoader:
      mockInstance = mock(FabricLoader.class)
      
      when(mockInstance.getGameDir()).thenReturn(mockGameDir)
      when(mockInstance.getConfigDir()).thenReturn(mockConfigDir)
      when(mockInstance.isModLoaded(anyString())).thenReturn(true)
      
      RETURN mockInstance

  CLASS WorldCreationMockUtils:
    FUNCTION createMockWorldEnvironment() -> WorldTestEnvironment:
      mockWorld = mock(World.class)
      mockServer = mock(MinecraftServer.class)
      mockWorldSettings = new VillagesRebornWorldSettings()
      
      // Configure realistic world mock
      when(mockWorld.isClient()).thenReturn(false)
      when(mockWorld.getServer()).thenReturn(mockServer)
      
      RETURN WorldTestEnvironment(
        world: mockWorld,
        server: mockServer,
        settings: mockWorldSettings
      )
    
    FUNCTION mockWorldCreationCapture(settings: VillagesRebornWorldSettings) -> Void:
      // Enhance WorldCreationSettingsCapture for testing
      WorldCreationSettingsCapture.capture(settings)

  CLASS ChatEventMockUtils:
    FUNCTION createMockChatEvent(message: String, player: Player) -> ServerChatEvent:
      mockEvent = mock(ServerChatEvent.class)
      
      when(mockEvent.getMessage()).thenReturn(message)
      when(mockEvent.getPlayer()).thenReturn(player)
      when(mockEvent.isCancelled()).thenReturn(false)
      
      RETURN mockEvent
    
    FUNCTION createMockPlayer(name: String) -> Player:
      mockPlayer = mock(Player.class)
      playerUUID = UUID.randomUUID()
      
      when(mockPlayer.getName()).thenReturn(name)
      when(mockPlayer.getUUID()).thenReturn(playerUUID)
      when(mockPlayer.isOnline()).thenReturn(true)
      
      RETURN mockPlayer

  CLASS JUnitTestHarness:
    ANNOTATION @TestMethodOrder(OrderAnnotation.class)
    ANNOTATION @ExtendWith(MockitoExtension.class)
    
    FUNCTION setupStandardTestEnvironment() -> TestEnvironment:
      fabricEnv = FabricLoaderMockUtilsEnhanced.createHeadlessFabricEnvironment()
      worldEnv = WorldCreationMockUtils.createMockWorldEnvironment()
      
      RETURN TestEnvironment(
        fabricEnvironment: fabricEnv,
        worldEnvironment: worldEnv
      )
    
    FUNCTION @BeforeAll setupClass() -> Void:
      // Initialize test logging
      LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory()
      context.getLogger("com.beeny.villagesreborn").setLevel(Level.DEBUG)
    
    FUNCTION @AfterAll cleanupClass() -> Void:
      // Cleanup any static state
      WorldCreationSettingsCapture.clearAll()
      
  DATA CLASS FabricTestEnvironment:
    mockLoader: MockedStatic<FabricLoader>
    gameDirectory: Path
    configDirectory: Path
    cleanup: Runnable
    
  DATA CLASS WorldTestEnvironment:
    world: World
    server: MinecraftServer
    settings: VillagesRebornWorldSettings
    
  DATA CLASS TestEnvironment:
    fabricEnvironment: FabricTestEnvironment
    worldEnvironment: WorldTestEnvironment
```

**TDD Anchor**: [`FabricLoaderMockUtilsEnhancedTest`](common/src/test/java/com/beeny/villagesreborn/core/config/FabricLoaderMockUtilsEnhancedTest.java)

---

## Module 4: Test Framework Standardization Engine

```pseudocode
MODULE TestFrameworkStandardization:
  CLASS JUnit5MigrationEngine:
    PROPERTIES:
      testSourcePaths: List<Path> = [
        "common/src/test/java",
        "fabric/src/test/java"
      ]
    
    FUNCTION standardizeTestFramework() -> MigrationReport:
      allTestFiles = scanTestFiles(testSourcePaths)
      migratedFiles = []
      
      FOR testFile IN allTestFiles:
        IF requiresJUnit5Migration(testFile):
          migratedFile = migrateToJUnit5(testFile)
          migratedFiles.add(migratedFile)
      
      RETURN MigrationReport(
        totalFiles: allTestFiles.size(),
        migratedFiles: migratedFiles.size(),
        migrationDetails: migratedFiles
      )
    
    FUNCTION migrateToJUnit5(testFile: Path) -> MigrationDetail:
      content = readFile(testFile)
      
      // Replace JUnit 4 imports with JUnit 5
      content = content.replace("import org.junit.Test;", "import org.junit.jupiter.api.Test;")
      content = content.replace("import org.junit.Before;", "import org.junit.jupiter.api.BeforeEach;")
      content = content.replace("import org.junit.After;", "import org.junit.jupiter.api.AfterEach;")
      content = content.replace("import org.junit.BeforeClass;", "import org.junit.jupiter.api.BeforeAll;")
      content = content.replace("import org.junit.AfterClass;", "import org.junit.jupiter.api.AfterAll;")
      
      // Add proper annotations
      content = addStandardAnnotations(content)
      
      writeFile(testFile, content)
      
      RETURN MigrationDetail(
        filePath: testFile,
        changesMade: detectChanges(content)
      )
    
    FUNCTION addStandardAnnotations(content: String) -> String:
      // Ensure proper test class annotations
      IF !content.contains("@ExtendWith"):
        content = content.replace(
          "class ",
          "@ExtendWith(MockitoExtension.class)\nclass "
        )
      
      RETURN content

  CLASS TestConfigurationValidator:
    FUNCTION validateTestConfiguration() -> ValidationReport:
      commonConfig = validateModuleConfiguration("common")
      fabricConfig = validateModuleConfiguration("fabric")
      
      RETURN ValidationReport(
        commonModule: commonConfig,
        fabricModule: fabricConfig,
        overallStatus: commonConfig.isValid AND fabricConfig.isValid
      )
    
    FUNCTION validateModuleConfiguration(moduleName: String) -> ModuleValidation:
      buildGradle = Path.of(moduleName, "build.gradle")
      buildContent = readFile(buildGradle)
      
      hasJUnit5Platform = buildContent.contains("useJUnitPlatform()")
      hasJacocoConfig = buildContent.contains("jacoco")
      hasMockito = buildContent.contains("mockito")
      
      RETURN ModuleValidation(
        moduleName: moduleName,
        hasJUnit5Platform: hasJUnit5Platform,
        hasJacocoConfig: hasJacocoConfig,
        hasMockito: hasMockito,
        isValid: hasJUnit5Platform AND hasMockito
      )

  DATA CLASS MigrationReport:
    totalFiles: Integer
    migratedFiles: Integer
    migrationDetails: List<MigrationDetail>
    
  DATA CLASS MigrationDetail:
    filePath: Path
    changesMade: List<String>
    
  DATA CLASS ValidationReport:
    commonModule: ModuleValidation
    fabricModule: ModuleValidation
    overallStatus: Boolean
```

**TDD Anchor**: [`JUnit5MigrationEngineTest`](common/src/test/java/com/beeny/villagesreborn/core/testing/JUnit5MigrationEngineTest.java)

---

## Module 5: Headless Test Environment Engine

```pseudocode
MODULE HeadlessTestEnvironment:
  CLASS FabricHeadlessTestRunner:
    PROPERTIES:
      testEnvironment: FabricTestEnvironment
      integrationTestClasspath: List<String>
    
    FUNCTION initializeHeadlessEnvironment() -> HeadlessEnvironment:
      // Setup headless Fabric environment for integration tests
      systemProperties = {
        "java.awt.headless": "true",
        "fabric.development": "true",
        "fabric.test.environment": "true"
      }
      
      FOR property, value IN systemProperties:
        System.setProperty(property, value)
      
      fabricEnv = FabricLoaderMockUtilsEnhanced.createHeadlessFabricEnvironment()
      
      RETURN HeadlessEnvironment(
        fabricEnvironment: fabricEnv,
        systemProperties: systemProperties
      )
    
    FUNCTION runIntegrationTests(testClasses: List<Class>) -> TestExecutionReport:
      headlessEnv = initializeHeadlessEnvironment()
      
      TRY:
        results = []
        FOR testClass IN testClasses:
          result = executeTestClass(testClass, headlessEnv)
          results.add(result)
        
        RETURN TestExecutionReport(
          environment: headlessEnv,
          results: results,
          success: results.all(r -> r.isSuccess())
        )
      FINALLY:
        headlessEnv.cleanup()

  CLASS EnvironmentConfigurationProvider:
    FUNCTION createTestConfiguration() -> TestConfiguration:
      configDir = createTempDirectory("villagesreborn-test-config")
      
      // Create mock configuration files
      createMockConfigFile(configDir.resolve("villagesreborn_setup.properties"))
      
      RETURN TestConfiguration(
        configDirectory: configDir,
        gameDirectory: createTempDirectory("villagesreborn-test-game"),
        worldDirectory: createTempDirectory("villagesreborn-test-worlds")
      )
    
    FUNCTION createMockConfigFile(path: Path) -> Void:
      properties = Properties()
      properties.setProperty("firstTimeSetupComplete", "true")
      properties.setProperty("llmProvider", "TEST_PROVIDER")
      properties.setProperty("enableAdvancedAI", "false")
      
      try (FileOutputStream fos = new FileOutputStream(path.toFile())):
        properties.store(fos, "Test configuration")

  CLASS EnvironmentVariableMockProvider:
    FUNCTION setupEnvironmentMocks() -> EnvironmentMockConfiguration:
      environmentMocks = {
        "MINECRAFT_VERSION": "1.21.4",
        "FABRIC_VERSION": "0.100.0",
        "VILLAGESREBORN_TEST_MODE": "true"
      }
      
      FOR key, value IN environmentMocks:
        System.setProperty(key, value)
      
      RETURN EnvironmentMockConfiguration(environmentMocks)

  DATA CLASS HeadlessEnvironment:
    fabricEnvironment: FabricTestEnvironment
    systemProperties: Map<String, String>
    
    FUNCTION cleanup() -> Void:
      fabricEnvironment.cleanup.run()
      // Restore system properties
      FOR key IN systemProperties.keySet():
        System.clearProperty(key)
    
  DATA CLASS TestConfiguration:
    configDirectory: Path
    gameDirectory: Path
    worldDirectory: Path
    
  DATA CLASS TestExecutionReport:
    environment: HeadlessEnvironment
    results: List<TestResult>
    success: Boolean
```

**TDD Anchor**: [`FabricHeadlessTestRunnerTest`](fabric/src/test/java/com/beeny/villagesreborn/platform/fabric/testing/FabricHeadlessTestRunnerTest.java)

---

## Module 6: Master Test Orchestration Engine

```pseudocode
MODULE MasterTestOrchestration:
  CLASS TestMetaTestEngine:
    PROPERTIES:
      allTestSuites: List<String> = [
        "common:test",
        "fabric:test"
      ]
    
    FUNCTION executeMetaTest() -> MetaTestResult:
      // This is the master test that fails if any test suite fails
      suiteResults = []
      
      FOR suite IN allTestSuites:
        result = executeTestSuite(suite)
        suiteResults.add(result)
        
        IF !result.isSuccess():
          FAIL("Test suite " + suite + " failed with " + result.failureCount + " failures")
      
      RETURN MetaTestResult(
        executedSuites: suiteResults,
        overallSuccess: suiteResults.all(r -> r.isSuccess()),
        totalTests: suiteResults.sum(r -> r.testCount),
        totalFailures: suiteResults.sum(r -> r.failureCount)
      )
    
    FUNCTION validateMockInitialization() -> MockValidationResult:
      // Test that all mock utilities initialize correctly
      fabricMockTest = testFabricLoaderMockInitialization()
      worldMockTest = testWorldCreationMockInitialization()
      chatMockTest = testChatEventMockInitialization()
      
      allPassed = fabricMockTest.isSuccess() AND 
                  worldMockTest.isSuccess() AND 
                  chatMockTest.isSuccess()
      
      RETURN MockValidationResult(
        fabricMockSuccess: fabricMockTest.isSuccess(),
        worldMockSuccess: worldMockTest.isSuccess(),
        chatMockSuccess: chatMockTest.isSuccess(),
        overallSuccess: allPassed
      )
    
    FUNCTION testFabricLoaderMockInitialization() -> TestResult:
      TRY:
        env = FabricLoaderMockUtilsEnhanced.createHeadlessFabricEnvironment()
        
        // Test basic mock functionality
        assertNotNull(env.gameDirectory)
        assertNotNull(env.configDirectory)
        assertTrue(Files.exists(env.configDirectory))
        
        env.cleanup()
        RETURN TestResult.success("FabricLoaderMock initialization")
      CATCH Exception e:
        RETURN TestResult.failure("FabricLoaderMock initialization", e)

  CLASS ContinuousIntegrationValidator:
    FUNCTION validateCIConfiguration() -> CIValidationReport:
      // Validate that tests can run in CI environment
      gradleConfig = validateGradleConfiguration()
      testExecution = validateTestExecution()
      coverageReporting = validateCoverageReporting()
      
      RETURN CIValidationReport(
        gradleConfig: gradleConfig,
        testExecution: testExecution,
        coverageReporting: coverageReporting,
        ciReady: gradleConfig.isValid AND testExecution.isValid
      )
    
    FUNCTION validateGradleConfiguration() -> GradleValidation:
      // Check gradle wrapper and configuration
      hasGradleWrapper = Files.exists(Path.of("gradlew"))
      hasSettings = Files.exists(Path.of("settings.gradle"))
      
      RETURN GradleValidation(
        hasWrapper: hasGradleWrapper,
        hasSettings: hasSettings,
        isValid: hasGradleWrapper AND hasSettings
      )
    
    FUNCTION validateTestExecution() -> TestExecutionValidation:
      // Run a minimal test to ensure CI can execute tests
      TRY:
        result = executeGradleCommand("clean", "test", "--no-daemon")
        RETURN TestExecutionValidation(
          canExecute: true,
          exitCode: result.exitCode,
          isValid: result.exitCode == 0
        )
      CATCH Exception e:
        RETURN TestExecutionValidation(false, -1, false)

  DATA CLASS MetaTestResult:
    executedSuites: List<TestSuiteResult>
    overallSuccess: Boolean
    totalTests: Integer
    totalFailures: Integer
    
  DATA CLASS MockValidationResult:
    fabricMockSuccess: Boolean
    worldMockSuccess: Boolean
    chatMockSuccess: Boolean
    overallSuccess: Boolean
    
  DATA CLASS CIValidationReport:
    gradleConfig: GradleValidation
    testExecution: TestExecutionValidation
    coverageReporting: CoverageValidation
    ciReady: Boolean
```

**TDD Anchor**: [`TestMetaTestEngineTest`](common/src/test/java/com/beeny/villagesreborn/core/testing/TestMetaTestEngineTest.java)

---

## TDD Implementation Plan

### Phase 1: Foundation Tests (Week 1)

1. **Create Master Test**: [`TestMetaTest`](common/src/test/java/com/beeny/villagesreborn/core/testing/TestMetaTest.java)
   - Fails if any test suite fails
   - Validates build configuration
   - Checks dependency resolution

2. **Mock Validation Tests**: [`FabricLoaderMockUtilsTest`](common/src/test/java/com/beeny/villagesreborn/core/config/FabricLoaderMockUtilsTest.java)
   - Test enhanced mock utilities
   - Validate headless environment setup
   - Check configuration providers

### Phase 2: Failing Test Resolution (Week 2)

3. **Import Resolution Tests**: For each failing test class:
   - [`CombatDecisionLogicTestsFixed`](common/src/test/java/com/beeny/villagesreborn/core/combat/CombatDecisionLogicTestsFixed.java)
   - [`HagglingEngineTestsFixed`](common/src/test/java/com/beeny/villagesreborn/core/combat/HagglingEngineTestsFixed.java)
   - [`TradeNegotiationTestsFixed`](common/src/test/java/com/beeny/villagesreborn/core/combat/TradeNegotiationTestsFixed.java)

4. **Disabled Test Reactivation**: 
   - [`SpawnBiomeStorageIntegrationTestRestored`](fabric/src/test/java/com/beeny/villagesreborn/platform/fabric/spawn/SpawnBiomeStorageIntegrationTestRestored.java)
   - [`SpawnBiomeStorageManagerTestRestored`](fabric/src/test/java/com/beeny/villagesreborn/platform/fabric/spawn/managers/SpawnBiomeStorageManagerTestRestored.java)

### Phase 3: Environment Stabilization (Week 3)

5. **Integration Test Harness**: [`FabricHeadlessIntegrationTest`](fabric/src/test/java/com/beeny/villagesreborn/platform/fabric/testing/FabricHeadlessIntegrationTest.java)
   - Headless Fabric test environment
   - Mock configuration providers
   - Environment variable handling

6. **CI Configuration Tests**: [`ContinuousIntegrationTest`](common/src/test/java/com/beeny/villagesreborn/core/testing/ContinuousIntegrationTest.java)
   - Gradle configuration validation
   - Test execution verification
   - Coverage reporting validation

### Phase 4: Full Suite Validation (Week 4)

7. **Complete Test Suite**: Run `./gradlew clean test` successfully
   - All tests pass
   - No disabled tests remain
   - Coverage reports generated
   - CI pipeline stable

## Success Criteria

### Test Execution
- [ ] `./gradlew clean test` executes without failures
- [ ] All previously disabled tests are reactivated and passing
- [ ] Mock utilities provide comprehensive test environment setup
- [ ] Integration tests run successfully in headless mode

### Code Quality
- [ ] Test coverage > 80% for core modules
- [ ] No compilation errors in test sources
- [ ] All imports resolve correctly
- [ ] JUnit 5 standardization complete

### CI Pipeline
- [ ] Tests execute successfully in CI environment
- [ ] Coverage reports generate and publish
- [ ] Build artifacts created without errors
- [ ] No flaky tests (consistent pass/fail behavior)

### Documentation
- [ ] Test setup documentation updated
- [ ] Mock utility usage examples provided
- [ ] CI configuration documented
- [ ] Troubleshooting guide created

## File Dependencies

### Core Test Classes
- [`TestMetaTest.java`](common/src/test/java/com/beeny/villagesreborn/core/testing/TestMetaTest.java) ↔ All test suites
- [`FabricLoaderMockUtilsEnhanced.java`](common/src/test/java/com/beeny/villagesreborn/core/config/FabricLoaderMockUtilsEnhanced.java) ↔ [`FabricLoaderMockUtils.java`](common/src/test/java/com/beeny/villagesreborn/core/config/FabricLoaderMockUtils.java)
- [`WorldCreationSettingsCaptureTest.java`](common/src/test/java/com/beeny/villagesreborn/core/world/WorldCreationSettingsCaptureTest.java) ↔ [`WorldCreationSettingsCapture.java`](common/src/main/java/com/beeny/villagesreborn/core/world/WorldCreationSettingsCapture.java)

### Build Configuration
- [`common/build.gradle`](common/build.gradle) - AssertJ and Mockito dependencies
- [`fabric/build.gradle`](fabric/build.gradle) - Fabric-specific test dependencies
- [`settings.gradle`](settings.gradle) - Multi-module test configuration

### Mock Infrastructure
- [`FabricLoaderMockUtils.java`](common/src/test/java/com/beeny/villagesreborn/core/config/FabricLoaderMockUtils.java) - Enhanced for comprehensive mocking
- Test harness classes for consistent setup across modules
- Environment configuration providers for headless testing

This specification provides a comprehensive roadmap for stabilizing the Villages Reborn test suite with modular, maintainable, and thoroughly tested components that ensure reliable CI pipeline execution.