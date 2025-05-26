# Fabric Config Integration - Usage Example

## How the Implementation Works

### Before (Original Implementation)
```java
private static Path getConfigPath() {
    String userDir = System.getProperty("user.dir");
    return Paths.get(userDir, SETUP_CONFIG_FILE);
}
```
**Result**: `<working_directory>/villagesreborn_setup.properties`

### After (New Implementation)
```java
private static Path getConfigPath() {
    return configPathResolver.resolveConfigPath();
}
```

## Runtime Behavior Examples

### Scenario 1: Running in Fabric Environment
```
Environment: Minecraft with Fabric Loader
FabricLoader.getConfigDir() -> /minecraft/config/
Result: /minecraft/config/villagesreborn_setup.properties
```

### Scenario 2: Running in Test Environment
```
Environment: JUnit tests, no Fabric
FabricLoader.getInstance() -> ClassNotFoundException
Fallback: System.getProperty("user.dir") -> /project/temp/
Result: /project/temp/villagesreborn_setup.properties
```

### Scenario 3: Development Environment
```
Environment: IDE, FabricLoader present but getConfigDir() fails
FabricLoader.getConfigDir() -> RuntimeException
Fallback: System.getProperty("user.dir") -> /dev/workspace/
Result: /dev/workspace/villagesreborn_setup.properties
```

## Provider Priority Resolution

```
ConfigPathResolver checks providers in priority order:

1. FabricConfigPathProvider (Priority: 100)
   ├─ isAvailable()? 
   │  ├─ YES → getConfigPath() → Success ✓
   │  └─ NO → Continue to next provider
   └─ Exception? → Log warning, continue to next provider

2. DefaultConfigPathProvider (Priority: 1)
   ├─ isAvailable()? → Always YES
   └─ getConfigPath() → Success ✓

Result: First successful provider wins
```

## Code Flow Demonstration

### Successful Fabric Path Resolution
```java
// User calls
FirstTimeSetupConfig config = FirstTimeSetupConfig.load();

// Internally resolves to:
Path configPath = configPathResolver.resolveConfigPath();
// ↓
FabricConfigPathProvider.isAvailable() → true
// ↓ 
FabricConfigPathProvider.getConfigPath()
// ↓
FabricLoader.getInstance().getConfigDir().resolve("villagesreborn_setup.properties")
// ↓
/minecraft/config/villagesreborn_setup.properties
```

### Fallback Path Resolution
```java
// User calls (same code)
FirstTimeSetupConfig config = FirstTimeSetupConfig.load();

// Internally resolves to:
Path configPath = configPathResolver.resolveConfigPath();
// ↓
FabricConfigPathProvider.isAvailable() → false (or exception)
// ↓
DefaultConfigPathProvider.isAvailable() → true
// ↓
DefaultConfigPathProvider.getConfigPath()
// ↓
Paths.get(System.getProperty("user.dir"), "villagesreborn_setup.properties")
// ↓
/working/directory/villagesreborn_setup.properties
```

## Verification Steps

You can verify the implementation works by:

1. **Check Config File Location**:
   ```java
   FirstTimeSetupConfig config = FirstTimeSetupConfig.load();
   config.completeSetup(LLMProvider.OPENAI, "gpt-3.5-turbo");
   // File will be saved to appropriate location based on environment
   ```

2. **Inspect Logs**:
   ```
   DEBUG - FabricLoader detected and initialized successfully
   DEBUG - Config path resolved using FabricConfigPathProvider: /minecraft/config/villagesreborn_setup.properties
   ```

   Or in fallback scenario:
   ```
   DEBUG - FabricLoader not found in classpath, will use fallback
   DEBUG - Config path resolved using DefaultConfigPathProvider: /working/dir/villagesreborn_setup.properties
   ```

3. **Run Tests**:
   ```bash
   gradlew :common:test --tests "ConfigPathResolverTest"
   # All tests should pass, demonstrating correct behavior
   ```

## Benefits in Practice

1. **Mod Users**: Config files appear in standard Minecraft config directory
2. **Developers**: Tests work without Fabric dependencies
3. **Server Admins**: Config location is predictable and follows conventions
4. **Mod Packs**: Configs are properly organized with other mod configurations

## Backward Compatibility

Existing installations will continue to work because:
- Same filename is used
- Fallback behavior matches original implementation
- No breaking changes to public APIs
- Existing config files will be found and loaded correctly