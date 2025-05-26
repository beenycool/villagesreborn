# Villages Reborn Project Structure Refactoring Specification

## Overview
Refactor project structure to remove top-level [`src`](src) files and consolidate entry points into proper [`fabric/src`](fabric/src) and [`common/src`](common/src) directories, ensuring clean modular architecture.

## Current State Analysis

### Legacy Top-Level Structure (TO BE REMOVED)
```
src/
├── client/
│   ├── java/com/beeny/VillagesrebornClient.java
│   ├── java/com/beeny/mixin/client/ExampleClientMixin.java
│   └── resources/villagesreborn.client.mixins.json
└── main/
    ├── java/com/beeny/Villagesreborn.java
    ├── java/com/beeny/mixin/ExampleMixin.java
    ├── resources/fabric.mod.json
    ├── resources/villagesreborn.mixins.json
    └── resources/assets/villagesreborn/icon.png
```

### Target Modern Structure (CURRENT)
```
fabric/src/
├── client/java/com/beeny/villagesreborn/platform/fabric/VillagesRebornFabricClient.java
├── main/java/com/beeny/villagesreborn/platform/fabric/VillagesRebornFabric.java
└── main/resources/fabric.mod.json

common/src/
└── main/java/com/beeny/villagesreborn/core/...
```

## Refactoring Plan

### Module 1: Legacy File Analysis and Classification
```pseudocode
FUNCTION analyzeLegacyFiles():
    legacyFiles = scanDirectory("src/")
    
    FOR EACH file IN legacyFiles:
        classification = classifyFile(file)
        
        SWITCH classification:
            CASE "obsolete_example":
                markForDeletion(file)
            CASE "outdated_config":
                analyzeConfigMigration(file)
            CASE "duplicate_resource":
                validateResourceConflict(file)
            CASE "unknown":
                flagForManualReview(file)
    
    RETURN FileClassificationReport
```

### Module 2: Resource Conflict Detection
```pseudocode
FUNCTION detectResourceConflicts():
    legacyResources = findResources("src/main/resources/")
    modernResources = findResources("fabric/src/main/resources/")
    
    conflicts = []
    
    FOR EACH legacyResource IN legacyResources:
        modernPath = mapToModernPath(legacyResource)
        
        IF exists(modernPath):
            conflict = compareFiles(legacyResource, modernPath)
            IF conflict.isDifferent():
                conflicts.append({
                    legacy: legacyResource,
                    modern: modernPath,
                    differences: conflict.getDifferences()
                })
    
    RETURN ConflictReport(conflicts)
```

### Module 3: Build Configuration Update
```pseudocode
FUNCTION updateBuildConfiguration():
    buildFiles = ["build.gradle", "fabric/build.gradle", "common/build.gradle"]
    
    FOR EACH buildFile IN buildFiles:
        config = parseBuildFile(buildFile)
        
        // Remove legacy src directory references
        config.removeSourceSet("src/main/java")
        config.removeSourceSet("src/client/java")
        config.removeResourceDirectory("src/main/resources")
        config.removeResourceDirectory("src/client/resources")
        
        // Validate modern source sets
        validateSourceSet("fabric/src/main/java")
        validateSourceSet("fabric/src/client/java")
        validateSourceSet("common/src/main/java")
        validateSourceSet("common/src/test/java")
        
        writeUpdatedConfig(buildFile, config)
```

### Module 4: Fabric Mod Metadata Consolidation
```pseudocode
FUNCTION consolidateModMetadata():
    legacyModJson = readJson("src/main/resources/fabric.mod.json")
    modernModJson = readJson("fabric/src/main/resources/fabric.mod.json")
    
    // Validate no critical information is lost
    validateMetadataConsistency(legacyModJson, modernModJson)
    
    // Update entrypoints to modern package structure
    modernModJson.entrypoints.main = [
        "com.beeny.villagesreborn.platform.fabric.VillagesRebornFabric"
    ]
    modernModJson.entrypoints.client = [
        "com.beeny.villagesreborn.platform.fabric.VillagesRebornFabricClient"
    ]
    
    // Validate all referenced classes exist
    FOR EACH entrypoint IN modernModJson.entrypoints:
        FOR EACH className IN entrypoint:
            validateClassExists(className)
    
    writeJson("fabric/src/main/resources/fabric.mod.json", modernModJson)
```

### Module 5: Mixin Configuration Validation
```pseudocode
FUNCTION validateMixinConfigurations():
    legacyMixins = [
        "src/main/resources/villagesreborn.mixins.json",
        "src/client/resources/villagesreborn.client.mixins.json"
    ]
    
    modernMixins = [
        "fabric/src/main/resources/villagesreborn.mixins.json",
        "fabric/src/client/resources/villagesreborn.client.mixins.json"
    ]
    
    FOR i IN range(legacyMixins.length):
        legacyConfig = readJson(legacyMixins[i])
        modernConfig = readJson(modernMixins[i])
        
        // Ensure all mixin classes are properly relocated
        FOR EACH mixinClass IN legacyConfig.mixins:
            modernEquivalent = mapToModernPackage(mixinClass)
            
            IF modernEquivalent NOT IN modernConfig.mixins:
                logWarning("Missing mixin migration: " + mixinClass)
            
            validateMixinClassExists(modernEquivalent)
```

### Module 6: Safe Directory Removal
```pseudocode
FUNCTION safelyRemoveLegacyDirectory():
    // Pre-removal validation
    validateNoActiveDependencies("src/")
    validateAllFilesClassified("src/")
    validateBuildConfigUpdated()
    
    // Create backup before removal
    createBackup("src/", "backup/legacy-src-" + timestamp())
    
    // Perform removal with verification
    removeDirectory("src/")
    
    // Post-removal validation
    validateProjectBuilds()
    validateTestsSuiteRuns()
    validateNoMissingClasses()
```

## Test-Driven Development Plan

### Test Suite 1: Legacy File Classification
```pseudocode
CLASS LegacyFileClassificationTests:
    
    TEST shouldIdentifyObsoleteExampleFiles():
        files = ["src/main/java/com/beeny/Villagesreborn.java"]
        result = analyzeLegacyFiles()
        
        ASSERT result.getClassification(files[0]) == "obsolete_example"
        ASSERT result.shouldDelete(files[0]) == true
    
    TEST shouldIdentifyOutdatedConfigFiles():
        configFile = "src/main/resources/fabric.mod.json"
        result = analyzeLegacyFiles()
        
        ASSERT result.getClassification(configFile) == "outdated_config"
        ASSERT result.requiresMigration(configFile) == true
    
    TEST shouldDetectAllLegacyFiles():
        expectedCount = 9  // Based on current structure
        result = analyzeLegacyFiles()
        
        ASSERT result.getTotalFiles() == expectedCount
        ASSERT result.hasUnclassifiedFiles() == false
```

### Test Suite 2: Resource Conflict Detection
```pseudocode
CLASS ResourceConflictDetectionTests:
    
    TEST shouldDetectDuplicateIcons():
        conflicts = detectResourceConflicts()
        iconConflicts = conflicts.filterByType("icon")
        
        ASSERT iconConflicts.size() >= 1
        ASSERT iconConflicts.contains("assets/villagesreborn/icon.png")
    
    TEST shouldValidateNoContentLoss():
        conflicts = detectResourceConflicts()
        
        FOR EACH conflict IN conflicts:
            modern = readFile(conflict.modernPath)
            legacy = readFile(conflict.legacyPath)
            
            // Modern should be superset of legacy functionality
            ASSERT modern.functionality.contains(legacy.functionality)
    
    TEST shouldIdentifyModJsonDifferences():
        conflicts = detectResourceConflicts()
        modJsonConflict = conflicts.findByName("fabric.mod.json")
        
        ASSERT modJsonConflict != null
        ASSERT modJsonConflict.differences.contains("entrypoints")
```

### Test Suite 3: Build Configuration Validation
```pseudocode
CLASS BuildConfigurationTests:
    
    TEST shouldRemoveAllLegacySourceSets():
        updateBuildConfiguration()
        
        buildConfig = parseBuildFile("build.gradle")
        fabricConfig = parseBuildFile("fabric/build.gradle")
        
        ASSERT NOT buildConfig.hasSourceSet("src/main/java")
        ASSERT NOT buildConfig.hasSourceSet("src/client/java")
        ASSERT fabricConfig.hasSourceSet("fabric/src/main/java")
    
    TEST shouldPreserveAllModernSourceSets():
        updateBuildConfiguration()
        
        fabricConfig = parseBuildFile("fabric/build.gradle")
        commonConfig = parseBuildFile("common/build.gradle")
        
        ASSERT fabricConfig.hasSourceSet("fabric/src/main/java")
        ASSERT fabricConfig.hasSourceSet("fabric/src/client/java")
        ASSERT commonConfig.hasSourceSet("common/src/main/java")
        ASSERT commonConfig.hasSourceSet("common/src/test/java")
    
    TEST shouldMaintainProjectBuildability():
        updateBuildConfiguration()
        
        buildResult = executeGradleBuild()
        ASSERT buildResult.isSuccess()
        ASSERT buildResult.hasNoErrors()
```

### Test Suite 4: Entrypoint Validation
```pseudocode
CLASS EntrypointValidationTests:
    
    TEST shouldUpdateModJsonEntrypoints():
        consolidateModMetadata()
        
        modJson = readJson("fabric/src/main/resources/fabric.mod.json")
        
        expectedMain = "com.beeny.villagesreborn.platform.fabric.VillagesRebornFabric"
        expectedClient = "com.beeny.villagesreborn.platform.fabric.VillagesRebornFabricClient"
        
        ASSERT modJson.entrypoints.main.contains(expectedMain)
        ASSERT modJson.entrypoints.client.contains(expectedClient)
    
    TEST shouldValidateAllEntrypointClassesExist():
        consolidateModMetadata()
        
        modJson = readJson("fabric/src/main/resources/fabric.mod.json")
        
        FOR EACH entrypointList IN modJson.entrypoints.values():
            FOR EACH className IN entrypointList:
                classPath = convertToFilePath(className)
                ASSERT fileExists(classPath)
    
    TEST shouldPreserveModMetadata():
        consolidateModMetadata()
        
        modJson = readJson("fabric/src/main/resources/fabric.mod.json")
        
        ASSERT modJson.id == "villagesreborn"
        ASSERT modJson.name == "Villages Reborn"
        ASSERT modJson.license == "MIT"
        ASSERT modJson.depends.minecraft == "~1.21.4"
```

### Test Suite 5: Class Resolution Verification
```pseudocode
CLASS ClassResolutionTests:
    
    TEST shouldResolveAllCoreClasses():
        coreClasses = scanJavaFiles("common/src/main/java/")
        
        FOR EACH className IN coreClasses:
            compilationResult = compileClass(className)
            ASSERT compilationResult.isSuccess()
    
    TEST shouldResolveFabricPlatformClasses():
        fabricClasses = scanJavaFiles("fabric/src/main/java/")
        
        FOR EACH className IN fabricClasses:
            compilationResult = compileClass(className)
            ASSERT compilationResult.isSuccess()
    
    TEST shouldHaveNoMissingDependencies():
        allClasses = scanAllProjectClasses()
        
        FOR EACH className IN allClasses:
            dependencies = analyzeDependencies(className)
            
            FOR EACH dependency IN dependencies:
                ASSERT canResolve(dependency)
    
    TEST shouldHaveNoDuplicateClasses():
        allClasses = scanAllProjectClasses()
        classNames = extractClassNames(allClasses)
        
        duplicates = findDuplicates(classNames)
        ASSERT duplicates.isEmpty()
```

### Test Suite 6: Integration and Cleanup Verification
```pseudocode
CLASS IntegrationCleanupTests:
    
    TEST shouldBuildProjectAfterRefactoring():
        safelyRemoveLegacyDirectory()
        
        buildResult = executeGradleBuild("build")
        ASSERT buildResult.isSuccess()
        ASSERT buildResult.containsNoWarnings()
    
    TEST shouldRunAllTestsSuccessfully():
        safelyRemoveLegacyDirectory()
        
        testResult = executeGradleBuild("test")
        ASSERT testResult.allTestsPassed()
        ASSERT testResult.coverage >= previousCoverage
    
    TEST shouldHaveNoLegacyDirectoryReferences():
        safelyRemoveLegacyDirectory()
        
        allFiles = scanAllProjectFiles()
        
        FOR EACH file IN allFiles:
            content = readFile(file)
            ASSERT NOT content.contains("src/main/java")
            ASSERT NOT content.contains("src/client/java")
    
    TEST shouldPreserveAllFunctionality():
        originalClassCount = countClasses()
        originalResourceCount = countResources()
        
        safelyRemoveLegacyDirectory()
        
        newClassCount = countClasses()
        newResourceCount = countResources()
        
        // Should have same or more classes (no loss)
        ASSERT newClassCount >= originalClassCount - expectedRemovals
        // Should have consolidated resources
        ASSERT newResourceCount <= originalResourceCount
```

## Implementation Steps

### Phase 1: Analysis and Validation (Safety First)
1. Run [`LegacyFileClassificationTests`](PROJECT_REFACTORING_SPEC.md:89)
2. Execute [`ResourceConflictDetectionTests`](PROJECT_REFACTORING_SPEC.md:119)
3. Create backup of current state
4. Document all findings

### Phase 2: Build Configuration Updates
1. Update root [`build.gradle`](build.gradle) to remove legacy source sets
2. Validate [`fabric/build.gradle`](fabric/build.gradle) configuration
3. Run [`BuildConfigurationTests`](PROJECT_REFACTORING_SPEC.md:149)

### Phase 3: Metadata Consolidation
1. Consolidate [`fabric.mod.json`](fabric/src/main/resources/fabric.mod.json) configurations
2. Update mixin configurations
3. Run [`EntrypointValidationTests`](PROJECT_REFACTORING_SPEC.md:184)

### Phase 4: Verification and Cleanup
1. Run [`ClassResolutionTests`](PROJECT_REFACTORING_SPEC.md:220)
2. Execute full test suite
3. Remove legacy [`src`](src) directory
4. Run [`IntegrationCleanupTests`](PROJECT_REFACTORING_SPEC.md:254)

## Success Criteria

- [ ] All legacy files properly classified and handled
- [ ] No resource conflicts or data loss
- [ ] All build configurations updated and functional
- [ ] All entrypoints correctly reference existing classes
- [ ] No missing class dependencies
- [ ] No duplicate classes across modules
- [ ] Project builds successfully without legacy [`src`](src) directory
- [ ] All tests pass with maintained or improved coverage
- [ ] No references to legacy paths in any project files

## Risk Mitigation

### Backup Strategy
- Full project backup before any changes
- Incremental backups at each phase
- Git commits at validation checkpoints

### Rollback Plan
- Restore from backup if any validation fails
- Maintain legacy directory until all tests pass
- Progressive removal with validation gates

### Validation Gates
- Must pass all tests before proceeding to next phase
- Build must succeed at each checkpoint
- No functionality regression allowed

## Edge Cases and Constraints

### Package Name Consistency
- Legacy uses `com.beeny.*` package structure
- Modern uses `com.beeny.villagesreborn.*` structure
- Ensure no package conflicts

### Resource Path Handling
- Icon files must not be duplicated
- Mixin configurations must reference correct classes
- Asset paths must remain consistent

### Build System Compatibility
- Maintain Gradle multi-module structure
- Preserve test source sets
- Keep development workflow tasks functional

### Module Boundaries
- Keep [`common/src`](common/src) platform-agnostic
- Keep [`fabric/src`](fabric/src) Fabric-specific
- No cross-contamination of concerns