# FirstTimeSetupConfig FabricLoader Integration Specification

## Overview

Update [`FirstTimeSetupConfig.getConfigPath()`](common/src/main/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfig.java:37) to use Fabric's standard config directory while maintaining backward compatibility and graceful fallback when FabricLoader is unavailable.

## Current Implementation Analysis

**Current [`getConfigPath()`](common/src/main/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfig.java:37-40):**
```java
private static Path getConfigPath() {
    String userDir = System.getProperty("user.dir");
    return Paths.get(userDir, SETUP_CONFIG_FILE);
}
```

**Issues:**
- Uses working directory instead of standard Minecraft config location
- No integration with Fabric's config management
- Config files may be scattered across different directories
- Not following mod ecosystem conventions

## Functional Requirements

### FR-1: FabricLoader Config Directory Integration
- **Requirement**: Use [`FabricLoader.getInstance().getConfigDir()`](https://fabricmc.net/wiki/tutorial:config) when available
- **Expected Path**: `<minecraft_instance>/config/villagesreborn_setup.properties`
- **Benefit**: Follows Fabric ecosystem conventions for config file placement

### FR-2: Graceful Fallback Mechanism
- **Requirement**: Fall back to current behavior when FabricLoader unavailable
- **Scenarios**: 
  - Testing environments without Fabric
  - Potential future platform abstraction
  - Development environments with incomplete setup

### FR-3: Filename Preservation
- **Requirement**: Retain existing filename [`SETUP_CONFIG_FILE = "villagesreborn_setup.properties"`](common/src/main/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfig.java:19)
- **Benefit**: Maintains backward compatibility with existing installations

### FR-4: Exception Safety
- **Requirement**: Handle [`FabricLoader`](fabric/src/main/java/com/beeny/villagesreborn/platform/fabric/FabricPlatform.java:18) access failures gracefully
- **Error Handling**: Catch and log exceptions, fall back to default behavior

## Technical Design

### Module Structure

```
ConfigPathResolver
├── FabricConfigPathProvider (primary)
├── DefaultConfigPathProvider (fallback)
└── ConfigPathStrategy (interface)
```

### Core Logic Pseudocode

```pseudocode
MODULE ConfigPathResolver:
    INTERFACE ConfigPathStrategy:
        METHOD getConfigPath() -> Path
        METHOD isAvailable() -> boolean
        METHOD getPriority() -> int

    CLASS FabricConfigPathProvider IMPLEMENTS ConfigPathStrategy:
        FIELD fabricLoader: Optional<FabricLoader>
        
        CONSTRUCTOR():
            TRY:
                fabricLoader = Optional.of(FabricLoader.getInstance())
            CATCH ClassNotFoundException, LinkageError:
                fabricLoader = Optional.empty()
                LOG.debug("FabricLoader not available, will use fallback")
        
        METHOD isAvailable() -> boolean:
            RETURN fabricLoader.isPresent()
        
        METHOD getPriority() -> int:
            RETURN 100  // Higher priority than default
        
        METHOD getConfigPath() -> Path:
            IF fabricLoader.isPresent():
                configDir = fabricLoader.get().getConfigDir()
                RETURN configDir.resolve(SETUP_CONFIG_FILE).toString()
            ELSE:
                THROW UnsupportedOperationException("FabricLoader not available")

    CLASS DefaultConfigPathProvider IMPLEMENTS ConfigPathStrategy:
        METHOD isAvailable() -> boolean:
            RETURN true  // Always available
        
        METHOD getPriority() -> int:
            RETURN 1     // Lower priority
        
        METHOD getConfigPath() -> Path:
            userDir = System.getProperty("user.dir")
            RETURN Paths.get(userDir, SETUP_CONFIG_FILE)

    CLASS ConfigPathResolver:
        FIELD providers: List<ConfigPathStrategy>
        
        CONSTRUCTOR():
            providers = [
                new FabricConfigPathProvider(),
                new DefaultConfigPathProvider()
            ]
            providers.sort(BY priority DESCENDING)
        
        METHOD resolveConfigPath() -> Path:
            FOR provider IN providers:
                IF provider.isAvailable():
                    TRY:
                        RETURN provider.getConfigPath()
                    CATCH Exception e:
                        LOG.warn("Config path provider failed", e)
                        CONTINUE
            
            THROW IllegalStateException("No config path provider available")

MODULE FirstTimeSetupConfig:
    FIELD configPathResolver: ConfigPathResolver = new ConfigPathResolver()
    
    METHOD getConfigPath() -> Path:
        RETURN configPathResolver.resolveConfigPath()
```

### Implementation Strategy

#### Phase 1: Core Infrastructure
1. Create [`ConfigPathStrategy`](common/src/main/java/com/beeny/villagesreborn/core/config/ConfigPathStrategy.java) interface
2. Implement [`FabricConfigPathProvider`](common/src/main/java/com/beeny/villagesreborn/core/config/FabricConfigPathProvider.java)
3. Implement [`DefaultConfigPathProvider`](common/src/main/java/com/beeny/villagesreborn/core/config/DefaultConfigPathProvider.java)
4. Create [`ConfigPathResolver`](common/src/main/java/com/beeny/villagesreborn/core/config/ConfigPathResolver.java)

#### Phase 2: Integration
1. Update [`FirstTimeSetupConfig.getConfigPath()`](common/src/main/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfig.java:37)
2. Add dependency injection for testing
3. Maintain method signature compatibility

#### Phase 3: Testing & Validation
1. Comprehensive test suite (see TDD plan below)
2. Integration testing with Fabric environment
3. Fallback behavior validation

## Test-Driven Development Plan

### Test Categories

#### TC-1: FabricLoader Integration Tests
**File**: [`FirstTimeSetupConfigFabricIntegrationTest.java`](common/src/test/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfigFabricIntegrationTest.java)

```pseudocode
TEST_CLASS FirstTimeSetupConfigFabricIntegrationTest:
    FIELD mockFabricLoader: FabricLoader
    FIELD tempConfigDir: Path
    
    @BeforeEach
    METHOD setup():
        tempConfigDir = createTempDirectory("fabric-config-test")
        mockFabricLoader = mock(FabricLoader.class)
        when(mockFabricLoader.getConfigDir()).thenReturn(tempConfigDir)
    
    @Test
    METHOD testFabricLoaderConfigPath():
        // GIVEN: FabricLoader returns temp directory
        WITH_MOCKED_STATIC(FabricLoader.class):
            when(FabricLoader.getInstance()).thenReturn(mockFabricLoader)
            
            // WHEN: getConfigPath() is called
            actualPath = FirstTimeSetupConfig.getConfigPath()
            
            // THEN: Path should use Fabric config directory
            expectedPath = tempConfigDir.resolve("villagesreborn_setup.properties")
            assertEqual(expectedPath.toString(), actualPath.toString())
    
    @Test
    METHOD testConfigFileCreationInFabricDir():
        // GIVEN: FabricLoader available and temp config directory
        WITH_MOCKED_STATIC(FabricLoader.class):
            when(FabricLoader.getInstance()).thenReturn(mockFabricLoader)
            
            // WHEN: Config is saved
            config = FirstTimeSetupConfig.load()
            config.completeSetup(LLMProvider.OPENAI, "gpt-3.5-turbo")
            
            // THEN: File should be created in Fabric config directory
            expectedFile = tempConfigDir.resolve("villagesreborn_setup.properties")
            assertTrue(Files.exists(expectedFile))
            
            // AND: File should contain expected content
            content = Files.readString(expectedFile)
            assertContains(content, "setup.completed=true")
            assertContains(content, "llm.provider=OPENAI")
    
    @Test
    METHOD testFabricConfigPathToString():
        // GIVEN: FabricLoader returns non-absolute path
        when(mockFabricLoader.getConfigDir()).thenReturn(Paths.get("relative/config"))
        
        WITH_MOCKED_STATIC(FabricLoader.class):
            when(FabricLoader.getInstance()).thenReturn(mockFabricLoader)
            
            // WHEN: getConfigPath() called
            path = FirstTimeSetupConfig.getConfigPath()
            
            // THEN: Should handle path-to-string conversion correctly
            assertNotNull(path)
            assertTrue(path.toString().endsWith("villagesreborn_setup.properties"))
```

#### TC-2: Fallback Behavior Tests
**File**: [`FirstTimeSetupConfigFallbackTest.java`](common/src/test/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfigFallbackTest.java)

```pseudocode
TEST_CLASS FirstTimeSetupConfigFallbackTest:
    FIELD originalUserDir: String
    FIELD tempUserDir: Path
    
    @BeforeEach
    METHOD setup():
        originalUserDir = System.getProperty("user.dir")
        tempUserDir = createTempDirectory("fallback-test")
        System.setProperty("user.dir", tempUserDir.toString())
    
    @AfterEach
    METHOD cleanup():
        System.setProperty("user.dir", originalUserDir)
    
    @Test
    METHOD testFallbackWhenFabricLoaderUnavailable():
        // GIVEN: FabricLoader.getInstance() throws exception
        WITH_MOCKED_STATIC(FabricLoader.class):
            when(FabricLoader.getInstance()).thenThrow(
                new RuntimeException("FabricLoader not available")
            )
            
            // WHEN: getConfigPath() is called
            path = FirstTimeSetupConfig.getConfigPath()
            
            // THEN: Should fall back to user.dir behavior
            expectedPath = tempUserDir.resolve("villagesreborn_setup.properties")
            assertEqual(expectedPath.toString(), path.toString())
    
    @Test
    METHOD testFallbackWhenClassNotFound():
        // GIVEN: FabricLoader class not in classpath (simulated)
        WITH_MOCKED_STATIC(FabricLoader.class):
            when(FabricLoader.getInstance()).thenThrow(
                new NoClassDefFoundError("net/fabricmc/loader/api/FabricLoader")
            )
            
            // WHEN: getConfigPath() is called
            path = FirstTimeSetupConfig.getConfigPath()
            
            // THEN: Should gracefully fall back
            expectedPath = tempUserDir.resolve("villagesreborn_setup.properties")
            assertEqual(expectedPath.toString(), path.toString())
    
    @Test
    METHOD testFallbackConfigOperations():
        // GIVEN: FabricLoader unavailable, fallback active
        WITH_MOCKED_STATIC(FabricLoader.class):
            when(FabricLoader.getInstance()).thenThrow(
                new RuntimeException("No Fabric environment")
            )
            
            // WHEN: Standard config operations performed
            config = FirstTimeSetupConfig.load()
            config.completeSetup(LLMProvider.LOCAL, "llama2:7b")
            
            // THEN: Should work with fallback path
            configFile = tempUserDir.resolve("villagesreborn_setup.properties")
            assertTrue(Files.exists(configFile))
            
            // AND: Reloading should work
            reloadedConfig = FirstTimeSetupConfig.load()
            assertEqual(LLMProvider.LOCAL, reloadedConfig.getSelectedProvider())
            assertEqual("llama2:7b", reloadedConfig.getSelectedModel())
```

#### TC-3: Error Handling & Edge Cases
**File**: [`FirstTimeSetupConfigErrorHandlingTest.java`](common/src/test/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfigErrorHandlingTest.java)

```pseudocode
TEST_CLASS FirstTimeSetupConfigErrorHandlingTest:
    
    @Test
    METHOD testNullConfigDirFromFabric():
        // GIVEN: FabricLoader returns null config directory
        mockFabricLoader = mock(FabricLoader.class)
        when(mockFabricLoader.getConfigDir()).thenReturn(null)
        
        WITH_MOCKED_STATIC(FabricLoader.class):
            when(FabricLoader.getInstance()).thenReturn(mockFabricLoader)
            
            // WHEN: getConfigPath() called
            // THEN: Should handle gracefully and fall back
            assertDoesNotThrow(() -> {
                path = FirstTimeSetupConfig.getConfigPath()
                assertNotNull(path)
            })
    
    @Test
    METHOD testIOExceptionOnConfigDirAccess():
        // GIVEN: FabricLoader getConfigDir() throws IOException
        mockFabricLoader = mock(FabricLoader.class)
        when(mockFabricLoader.getConfigDir()).thenThrow(
            new RuntimeException("IO error accessing config dir")
        )
        
        WITH_MOCKED_STATIC(FabricLoader.class):
            when(FabricLoader.getInstance()).thenReturn(mockFabricLoader)
            
            // WHEN: getConfigPath() called
            // THEN: Should fall back to default behavior
            path = FirstTimeSetupConfig.getConfigPath()
            
            // Should not throw and should return valid path
            assertNotNull(path)
            assertTrue(path.toString().endsWith("villagesreborn_setup.properties"))
    
    @Test
    METHOD testConcurrentAccess():
        // GIVEN: Multiple threads accessing getConfigPath()
        executor = Executors.newFixedThreadPool(10)
        results = Collections.synchronizedList(new ArrayList<String>())
        
        // WHEN: 100 concurrent calls to getConfigPath()
        futures = (1..100).map { _ ->
            executor.submit(() -> {
                path = FirstTimeSetupConfig.getConfigPath()
                results.add(path.toString())
            })
        }
        
        futures.forEach { it.get(5, TimeUnit.SECONDS) }
        
        // THEN: All calls should return same path
        expectedPath = results.first()
        assertTrue(results.all { it == expectedPath })
        assertEqual(100, results.size())
```

#### TC-4: Integration with Existing Tests
**File**: [`FirstTimeSetupConfigTest.java`](common/src/test/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfigTest.java) (Updates)

```pseudocode
UPDATED_TEST_CLASS FirstTimeSetupConfigTest:
    
    @BeforeEach
    METHOD setUp():
        // Mock FabricLoader to ensure predictable test environment
        WITH_MOCKED_STATIC(FabricLoader.class):
            when(FabricLoader.getInstance()).thenThrow(
                new RuntimeException("Test environment - no Fabric")
            )
        
        // Set working directory to temp directory for isolated tests
        System.setProperty("user.dir", tempDir.toString())
    
    // Existing tests remain unchanged - they should continue to pass
    // with fallback behavior in test environment
    
    @Test
    METHOD testConfigPathConsistency():
        // GIVEN: Multiple calls to getConfigPath()
        path1 = FirstTimeSetupConfig.getConfigPath()
        path2 = FirstTimeSetupConfig.getConfigPath()
        
        // THEN: Should return consistent results
        assertEqual(path1.toString(), path2.toString())
    
    @Test
    METHOD testConfigPathFormat():
        // WHEN: getConfigPath() called
        path = FirstTimeSetupConfig.getConfigPath()
        
        // THEN: Should end with correct filename
        assertTrue(path.toString().endsWith("villagesreborn_setup.properties"))
        assertNotNull(path.getParent()) // Should have a parent directory
```

### Test Execution Strategy

#### TDD Red-Green-Refactor Cycle

**Iteration 1: Basic FabricLoader Integration**
1. **RED**: Write [`testFabricLoaderConfigPath()`](common/src/test/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfigFabricIntegrationTest.java) - should fail
2. **GREEN**: Implement minimal [`FabricConfigPathProvider`](common/src/main/java/com/beeny/villagesreborn/core/config/FabricConfigPathProvider.java)
3. **REFACTOR**: Extract common interfaces, improve error handling

**Iteration 2: Fallback Mechanism**
1. **RED**: Write [`testFallbackWhenFabricLoaderUnavailable()`](common/src/test/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfigFallbackTest.java)
2. **GREEN**: Implement [`DefaultConfigPathProvider`](common/src/main/java/com/beeny/villagesreborn/core/config/DefaultConfigPathProvider.java) and resolver logic
3. **REFACTOR**: Optimize provider selection and caching

**Iteration 3: Error Handling**
1. **RED**: Write error handling tests
2. **GREEN**: Add comprehensive exception handling
3. **REFACTOR**: Simplify error recovery paths

**Iteration 4: Performance & Edge Cases**
1. **RED**: Write concurrency and edge case tests
2. **GREEN**: Add thread safety and edge case handling
3. **REFACTOR**: Final optimization and documentation

### Mock Strategy

#### FabricLoader Mocking Patterns

```pseudocode
UTILITY_CLASS FabricLoaderMockUtils:
    
    METHOD mockFabricLoaderWithConfigDir(configDir: Path) -> MockedStatic<FabricLoader>:
        mockLoader = mock(FabricLoader.class)
        when(mockLoader.getConfigDir()).thenReturn(configDir)
        
        mockedStatic = mockStatic(FabricLoader.class)
        mockedStatic.when(FabricLoader::getInstance).thenReturn(mockLoader)
        
        RETURN mockedStatic
    
    METHOD mockFabricLoaderUnavailable() -> MockedStatic<FabricLoader>:
        mockedStatic = mockStatic(FabricLoader.class)
        mockedStatic.when(FabricLoader::getInstance)
                   .thenThrow(new RuntimeException("FabricLoader not available"))
        
        RETURN mockedStatic
    
    METHOD mockFabricLoaderClassNotFound() -> MockedStatic<FabricLoader>:
        mockedStatic = mockStatic(FabricLoader.class)
        mockedStatic.when(FabricLoader::getInstance)
                   .thenThrow(new NoClassDefFoundError("FabricLoader not in classpath"))
        
        RETURN mockedStatic
```

## Implementation Checklist

### Core Implementation
- [ ] Create [`ConfigPathStrategy`](common/src/main/java/com/beeny/villagesreborn/core/config/ConfigPathStrategy.java) interface
- [ ] Implement [`FabricConfigPathProvider`](common/src/main/java/com/beeny/villagesreborn/core/config/FabricConfigPathProvider.java)
- [ ] Implement [`DefaultConfigPathProvider`](common/src/main/java/com/beeny/villagesreborn/core/config/DefaultConfigPathProvider.java)
- [ ] Create [`ConfigPathResolver`](common/src/main/java/com/beeny/villagesreborn/core/config/ConfigPathResolver.java)
- [ ] Update [`FirstTimeSetupConfig.getConfigPath()`](common/src/main/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfig.java:37)

### Testing Implementation
- [ ] Implement [`FirstTimeSetupConfigFabricIntegrationTest`](common/src/test/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfigFabricIntegrationTest.java)
- [ ] Implement [`FirstTimeSetupConfigFallbackTest`](common/src/test/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfigFallbackTest.java)
- [ ] Implement [`FirstTimeSetupConfigErrorHandlingTest`](common/src/test/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfigErrorHandlingTest.java)
- [ ] Update existing [`FirstTimeSetupConfigTest`](common/src/test/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfigTest.java)
- [ ] Create [`FabricLoaderMockUtils`](common/src/test/java/com/beeny/villagesreborn/core/config/FabricLoaderMockUtils.java)

### Validation & Documentation
- [ ] Verify all existing tests pass with fallback behavior
- [ ] Test with actual Fabric environment
- [ ] Update JavaDoc documentation
- [ ] Add logging for troubleshooting
- [ ] Performance testing for config path resolution

## Success Criteria

1. **Functional**: [`getConfigPath()`](common/src/main/java/com/beeny/villagesreborn/core/config/FirstTimeSetupConfig.java:37) uses Fabric config directory when available
2. **Compatibility**: Existing installations continue working without changes
3. **Resilience**: Graceful fallback when FabricLoader unavailable
4. **Testing**: 100% test coverage for new functionality
5. **Performance**: No significant performance impact on config operations
6. **Standards**: Follows Fabric ecosystem conventions for mod configuration

## Risk Mitigation

### Risk: Breaking Existing Installations
**Mitigation**: Comprehensive fallback mechanism ensures existing behavior preserved

### Risk: FabricLoader API Changes
**Mitigation**: Defensive programming with exception handling and interface abstraction

### Risk: Test Environment Complexity
**Mitigation**: Comprehensive mocking strategy isolates FabricLoader dependencies

### Risk: Performance Impact
**Mitigation**: Lazy initialization and caching of config path resolution

## Future Extensions

1. **Multi-Platform Support**: Abstract config path resolution for other mod loaders
2. **Config Migration**: Automatic migration from old to new config locations
3. **Hot Reload**: Watch for config directory changes in development
4. **Validation**: Enhanced config file validation and repair mechanisms