# TDD Test Implementation Plan for Project Refactoring

## Test Structure Overview

### Test Organization Strategy
```
test/
├── unit/
│   ├── LegacyFileClassificationTests.java
│   ├── ResourceConflictDetectionTests.java
│   ├── BuildConfigurationTests.java
│   ├── EntrypointValidationTests.java
│   └── ClassResolutionTests.java
├── integration/
│   ├── IntegrationCleanupTests.java
│   └── RefactoringWorkflowTests.java
└── fixtures/
    ├── sample-legacy-structure/
    ├── expected-conflicts.json
    └── validation-checksums.json
```

## Detailed Test Implementation

### Test Suite 1: Legacy File Classification
```java
// File: test/unit/LegacyFileClassificationTests.java
package com.beeny.villagesreborn.refactoring.test;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Set;
import java.util.Map;

@DisplayName("Legacy File Classification Tests")
class LegacyFileClassificationTests {
    
    @TempDir
    Path tempProjectDir;
    
    private LegacyFileAnalyzer analyzer;
    private ProjectStructureScanner scanner;
    
    @BeforeEach
    void setupTestEnvironment() {
        analyzer = new LegacyFileAnalyzer();
        scanner = new ProjectStructureScanner(tempProjectDir);
        createTestLegacyStructure();
    }
    
    @Test
    @DisplayName("Should identify obsolete example files correctly")
    void shouldIdentifyObsoleteExampleFiles() {
        // Given: Legacy structure with example files
        Path legacyMain = tempProjectDir.resolve("src/main/java/com/beeny/Villagesreborn.java");
        createExampleMainClass(legacyMain);
        
        // When: Analyzing legacy files
        FileClassificationReport result = analyzer.analyzeLegacyFiles(tempProjectDir);
        
        // Then: Example files should be marked for deletion
        assertThat(result.getClassification(legacyMain))
            .isEqualTo(FileClassification.OBSOLETE_EXAMPLE);
        assertThat(result.shouldDelete(legacyMain)).isTrue();
        assertThat(result.getDeletionReason(legacyMain))
            .contains("superseded by modern platform implementation");
    }
    
    @Test
    @DisplayName("Should identify outdated config files requiring migration")
    void shouldIdentifyOutdatedConfigFiles() {
        // Given: Legacy mod.json with outdated entrypoints
        Path legacyModJson = tempProjectDir.resolve("src/main/resources/fabric.mod.json");
        createLegacyModJson(legacyModJson);
        
        // When: Analyzing configuration files
        FileClassificationReport result = analyzer.analyzeLegacyFiles(tempProjectDir);
        
        // Then: Config should be marked for migration analysis
        assertThat(result.getClassification(legacyModJson))
            .isEqualTo(FileClassification.OUTDATED_CONFIG);
        assertThat(result.requiresMigration(legacyModJson)).isTrue();
        
        MigrationPlan plan = result.getMigrationPlan(legacyModJson);
        assertThat(plan.getTargetPath())
            .isEqualTo("fabric/src/main/resources/fabric.mod.json");
        assertThat(plan.getRequiredTransformations())
            .contains("update_entrypoints", "update_package_references");
    }
    
    @Test
    @DisplayName("Should detect all legacy files without missing any")
    void shouldDetectAllLegacyFiles() {
        // Given: Complete legacy structure
        createCompleteLegacyStructure();
        
        // When: Scanning for legacy files
        FileClassificationReport result = analyzer.analyzeLegacyFiles(tempProjectDir);
        
        // Then: All expected files should be detected
        Set<Path> expectedFiles = Set.of(
            tempProjectDir.resolve("src/main/java/com/beeny/Villagesreborn.java"),
            tempProjectDir.resolve("src/client/java/com/beeny/VillagesrebornClient.java"),
            tempProjectDir.resolve("src/main/java/com/beeny/mixin/ExampleMixin.java"),
            tempProjectDir.resolve("src/client/java/com/beeny/mixin/client/ExampleClientMixin.java"),
            tempProjectDir.resolve("src/main/resources/fabric.mod.json"),
            tempProjectDir.resolve("src/main/resources/villagesreborn.mixins.json"),
            tempProjectDir.resolve("src/client/resources/villagesreborn.client.mixins.json"),
            tempProjectDir.resolve("src/main/resources/assets/villagesreborn/icon.png")
        );
        
        assertThat(result.getTotalFiles()).isEqualTo(expectedFiles.size());
        assertThat(result.getDetectedFiles()).containsExactlyInAnyOrderElementsOf(expectedFiles);
        assertThat(result.hasUnclassifiedFiles()).isFalse();
    }
    
    @Test
    @DisplayName("Should handle edge cases in file classification")
    void shouldHandleEdgeCasesInFileClassification() {
        // Given: Edge case files (empty files, corrupted content, etc.)
        Path emptyFile = tempProjectDir.resolve("src/main/java/com/beeny/EmptyClass.java");
        createEmptyFile(emptyFile);
        
        Path corruptedFile = tempProjectDir.resolve("src/main/resources/corrupted.json");
        createCorruptedJsonFile(corruptedFile);
        
        // When: Analyzing edge cases
        FileClassificationReport result = analyzer.analyzeLegacyFiles(tempProjectDir);
        
        // Then: Edge cases should be handled gracefully
        assertThat(result.getClassification(emptyFile))
            .isEqualTo(FileClassification.UNKNOWN);
        assertThat(result.requiresManualReview(emptyFile)).isTrue();
        
        assertThat(result.getClassification(corruptedFile))
            .isEqualTo(FileClassification.CORRUPTED);
        assertThat(result.getWarnings()).anyMatch(w -> 
            w.contains("corrupted.json") && w.contains("manual review"));
    }
    
    private void createTestLegacyStructure() {
        // Implementation details for creating test structure
    }
    
    private void createExampleMainClass(Path path) {
        String content = """
            package com.beeny;
            
            import net.fabricmc.api.ModInitializer;
            
            public class Villagesreborn implements ModInitializer {
                public static final String MOD_ID = "villagesreborn";
                
                @Override
                public void onInitialize() {
                    // Legacy example implementation
                }
            }
            """;
        writeToFile(path, content);
    }
    
    private void createLegacyModJson(Path path) {
        String content = """
            {
                "entrypoints": {
                    "main": ["com.beeny.Villagesreborn"],
                    "client": ["com.beeny.VillagesrebornClient"]
                }
            }
            """;
        writeToFile(path, content);
    }
}
```

### Test Suite 2: Resource Conflict Detection
```java
// File: test/unit/ResourceConflictDetectionTests.java
@DisplayName("Resource Conflict Detection Tests")
class ResourceConflictDetectionTests {
    
    @TempDir
    Path tempProjectDir;
    
    private ResourceConflictDetector detector;
    
    @BeforeEach
    void setupConflictScenarios() {
        detector = new ResourceConflictDetector();
        createConflictingResourceStructure();
    }
    
    @Test
    @DisplayName("Should detect duplicate icon files with different content")
    void shouldDetectDuplicateIconsWithDifferentContent() {
        // Given: Icon files in both legacy and modern locations
        Path legacyIcon = tempProjectDir.resolve("src/main/resources/assets/villagesreborn/icon.png");
        Path modernIcon = tempProjectDir.resolve("fabric/src/main/resources/assets/villagesreborn/icon.png");
        
        createIconFile(legacyIcon, "legacy-icon-content");
        createIconFile(modernIcon, "modern-icon-content");
        
        // When: Detecting conflicts
        ConflictReport conflicts = detector.detectResourceConflicts(tempProjectDir);
        
        // Then: Icon conflict should be detected with differences
        ResourceConflict iconConflict = conflicts.findByRelativePath("assets/villagesreborn/icon.png");
        assertThat(iconConflict).isNotNull();
        assertThat(iconConflict.getConflictType()).isEqualTo(ConflictType.CONTENT_DIFFERENCE);
        assertThat(iconConflict.getDifferences()).contains("binary_content_mismatch");
        
        ConflictResolution resolution = iconConflict.getRecommendedResolution();
        assertThat(resolution.getAction()).isEqualTo(ResolutionAction.USE_MODERN_VERSION);
        assertThat(resolution.getReason()).contains("modern version is more recent");
    }
    
    @Test
    @DisplayName("Should validate no critical content is lost during migration")
    void shouldValidateNoCriticalContentLoss() {
        // Given: Legacy mod.json with additional metadata
        Path legacyModJson = tempProjectDir.resolve("src/main/resources/fabric.mod.json");
        Path modernModJson = tempProjectDir.resolve("fabric/src/main/resources/fabric.mod.json");
        
        createLegacyModJsonWithExtraMetadata(legacyModJson);
        createModernModJson(modernModJson);
        
        // When: Detecting conflicts and analyzing content loss
        ConflictReport conflicts = detector.detectResourceConflicts(tempProjectDir);
        ContentLossAnalysis analysis = detector.analyzeContentLoss(conflicts);
        
        // Then: No critical content should be lost
        assertThat(analysis.hasCriticalLoss()).isFalse();
        assertThat(analysis.getPreservedFields()).contains(
            "id", "version", "name", "dependencies", "entrypoints"
        );
        
        // Non-critical fields may be lost but should be documented
        if (analysis.hasNonCriticalLoss()) {
            assertThat(analysis.getNonCriticalLossFields())
                .allMatch(field -> field.startsWith("custom.") || field.equals("suggests"));
        }
    }
    
    @Test
    @DisplayName("Should identify and resolve mixin configuration conflicts")
    void shouldIdentifyMixinConfigurationConflicts() {
        // Given: Conflicting mixin configurations
        Path legacyMixins = tempProjectDir.resolve("src/main/resources/villagesreborn.mixins.json");
        Path modernMixins = tempProjectDir.resolve("fabric/src/main/resources/villagesreborn.mixins.json");
        
        createMixinConfig(legacyMixins, List.of("com.beeny.mixin.ExampleMixin"));
        createMixinConfig(modernMixins, List.of("com.beeny.villagesreborn.platform.fabric.mixin.ExampleMixin"));
        
        // When: Analyzing mixin conflicts
        ConflictReport conflicts = detector.detectResourceConflicts(tempProjectDir);
        MixinConflictAnalysis mixinAnalysis = detector.analyzeMixinConflicts(conflicts);
        
        // Then: Mixin package migration should be detected and resolved
        assertThat(mixinAnalysis.hasPackageMigrationConflicts()).isTrue();
        
        PackageMigrationPlan plan = mixinAnalysis.getPackageMigrationPlan();
        assertThat(plan.getMappings()).containsEntry(
            "com.beeny.mixin.ExampleMixin",
            "com.beeny.villagesreborn.platform.fabric.mixin.ExampleMixin"
        );
        
        // Validation that target classes exist
        for (String targetClass : plan.getMappings().values()) {
            Path targetPath = convertClassNameToPath(targetClass);
            assertThat(Files.exists(tempProjectDir.resolve(targetPath)))
                .as("Target mixin class should exist: %s", targetClass)
                .isTrue();
        }
    }
    
    @Test
    @DisplayName("Should handle complex multi-file conflicts with dependencies")
    void shouldHandleComplexMultiFileConflicts() {
        // Given: Complex scenario with interdependent resource conflicts
        createComplexConflictScenario();
        
        // When: Detecting and resolving complex conflicts
        ConflictReport conflicts = detector.detectResourceConflicts(tempProjectDir);
        ConflictResolutionPlan plan = detector.createResolutionPlan(conflicts);
        
        // Then: Resolution plan should handle dependencies correctly
        assertThat(plan.getResolutionSteps()).isSorted(new DependencyOrderComparator());
        
        // Validate that no step depends on a later step
        for (int i = 0; i < plan.getResolutionSteps().size(); i++) {
            ResolutionStep step = plan.getResolutionSteps().get(i);
            for (String dependency : step.getDependencies()) {
                boolean dependencyResolved = plan.getResolutionSteps().subList(0, i)
                    .stream()
                    .anyMatch(prevStep -> prevStep.resolves(dependency));
                assertThat(dependencyResolved)
                    .as("Dependency %s should be resolved before step %d", dependency, i)
                    .isTrue();
            }
        }
    }
}
```

### Test Suite 3: Build Configuration Validation
```java
// File: test/unit/BuildConfigurationTests.java
@DisplayName("Build Configuration Tests")
class BuildConfigurationTests {
    
    @TempDir
    Path tempProjectDir;
    
    private BuildConfigurationUpdater updater;
    private GradleBuildValidator validator;
    
    @BeforeEach
    void setupBuildConfiguration() {
        updater = new BuildConfigurationUpdater();
        validator = new GradleBuildValidator();
        createTestBuildStructure();
    }
    
    @Test
    @DisplayName("Should remove all legacy source sets from build configuration")
    void shouldRemoveAllLegacySourceSets() {
        // Given: Build configuration with legacy source sets
        Path buildGradle = tempProjectDir.resolve("build.gradle");
        createBuildGradleWithLegacySources(buildGradle);
        
        // When: Updating build configuration
        BuildConfigurationResult result = updater.updateBuildConfiguration(tempProjectDir);
        
        // Then: Legacy source sets should be removed
        GradleBuildConfig updatedConfig = parseBuildFile(buildGradle);
        
        assertThat(updatedConfig.hasSourceSet("src/main/java")).isFalse();
        assertThat(updatedConfig.hasSourceSet("src/client/java")).isFalse();
        assertThat(updatedConfig.hasResourceDirectory("src/main/resources")).isFalse();
        assertThat(updatedConfig.hasResourceDirectory("src/client/resources")).isFalse();
        
        // Should preserve modern source sets
        Path fabricBuildGradle = tempProjectDir.resolve("fabric/build.gradle");
        GradleBuildConfig fabricConfig = parseBuildFile(fabricBuildGradle);
        
        assertThat(fabricConfig.hasSourceSet("fabric/src/main/java")).isTrue();
        assertThat(fabricConfig.hasSourceSet("fabric/src/client/java")).isTrue();
        
        // Validation should pass
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.getWarnings()).isEmpty();
    }
    
    @Test
    @DisplayName("Should preserve all modern source sets and configurations")
    void shouldPreserveAllModernSourceSets() {
        // Given: Properly configured modern build structure
        createModernBuildStructure();
        
        // When: Updating configuration (should be no-op for modern parts)
        BuildConfigurationResult result = updater.updateBuildConfiguration(tempProjectDir);
        
        // Then: All modern configurations should be preserved
        Path fabricBuild = tempProjectDir.resolve("fabric/build.gradle");
        Path commonBuild = tempProjectDir.resolve("common/build.gradle");
        
        GradleBuildConfig fabricConfig = parseBuildFile(fabricBuild);
        GradleBuildConfig commonConfig = parseBuildFile(commonBuild);
        
        // Fabric module source sets
        assertThat(fabricConfig.getSourceSets()).contains(
            "fabric/src/main/java",
            "fabric/src/client/java",
            "fabric/src/test/java"
        );
        
        // Common module source sets
        assertThat(commonConfig.getSourceSets()).contains(
            "common/src/main/java",
            "common/src/test/java"
        );
        
        // Dependencies should be preserved
        assertThat(fabricConfig.getDependencies()).contains("project(':common')");
        assertThat(commonConfig.getDependencies()).contains("fabricloader", "minecraft");
        
        assertThat(result.getPreservationReport().getAllPreserved()).isTrue();
    }
    
    @Test
    @DisplayName("Should maintain project buildability after configuration update")
    void shouldMaintainProjectBuildabilityAfterUpdate() {
        // Given: Working project with legacy configuration
        createWorkingProjectWithLegacyConfig();
        
        // When: Updating build configuration
        updater.updateBuildConfiguration(tempProjectDir);
        
        // Then: Project should still build successfully
        GradleBuildResult buildResult = validator.executeBuild(tempProjectDir, "build");
        
        assertThat(buildResult.isSuccess()).isTrue();
        assertThat(buildResult.hasCompilationErrors()).isFalse();
        assertThat(buildResult.getExitCode()).isEqualTo(0);
        
        // All modules should compile
        BuildModuleResult fabricResult = buildResult.getModuleResult("fabric");
        BuildModuleResult commonResult = buildResult.getModuleResult("common");
        
        assertThat(fabricResult.isSuccess()).isTrue();
        assertThat(commonResult.isSuccess()).isTrue();
        
        // Artifacts should be generated
        assertThat(Files.exists(tempProjectDir.resolve("fabric/build/libs")))
            .as("Fabric artifacts should be generated").isTrue();
        assertThat(Files.exists(tempProjectDir.resolve("common/build/libs")))
            .as("Common artifacts should be generated").isTrue();
    }
    
    @Test
    @DisplayName("Should validate dependency resolution after source set changes")
    void shouldValidateDependencyResolutionAfterSourceSetChanges() {
        // Given: Project with complex inter-module dependencies
        createComplexDependencyStructure();
        
        // When: Updating build configuration
        BuildConfigurationResult result = updater.updateBuildConfiguration(tempProjectDir);
        
        // Then: All dependencies should still resolve correctly
        DependencyResolutionReport depResult = validator.validateDependencyResolution(tempProjectDir);
        
        assertThat(depResult.hasUnresolvedDependencies()).isFalse();
        assertThat(depResult.hasCircularDependencies()).isFalse();
        
        // Verify specific dependency paths
        assertThat(depResult.canResolve("common", "fabricloader")).isTrue();
        assertThat(depResult.canResolve("fabric", "project(':common')")).isTrue();
        assertThat(depResult.canResolve("fabric", "fabric-api")).isTrue();
        
        // No legacy dependencies should remain
        assertThat(depResult.hasLegacyDependencyReferences()).isFalse();
    }
}
```

## Test Execution Strategy

### Continuous Integration Pipeline
```yaml
# File: .github/workflows/refactoring-tests.yml
name: Project Refactoring Tests

on:
  push:
    paths:
      - 'src/**'
      - 'build.gradle'
      - 'fabric/build.gradle'
      - 'common/build.gradle'
      - 'test/**'

jobs:
  refactoring-validation:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        java-version: [21]
        
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK ${{ matrix.java-version }}
      uses: actions/setup-java@v4
      with:
        java-version: ${{ matrix.java-version }}
        distribution: 'temurin'
        
    - name: Cache Gradle packages
      uses: actions/cache@v4
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        
    - name: Run Legacy File Classification Tests
      run: ./gradlew test --tests "*LegacyFileClassificationTests*"
      
    - name: Run Resource Conflict Detection Tests
      run: ./gradlew test --tests "*ResourceConflictDetectionTests*"
      
    - name: Run Build Configuration Tests
      run: ./gradlew test --tests "*BuildConfigurationTests*"
      
    - name: Run Integration Tests
      run: ./gradlew test --tests "*IntegrationCleanupTests*"
      
    - name: Generate Test Report
      run: ./gradlew testReport
      
    - name: Upload Test Results
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: test-results-${{ matrix.java-version }}
        path: build/reports/tests/
```

### Local Development Test Commands
```bash
# Run specific test suites
./gradlew test --tests "*LegacyFileClassification*"
./gradlew test --tests "*ResourceConflictDetection*"
./gradlew test --tests "*BuildConfiguration*"

# Run integration tests
./gradlew test --tests "*Integration*"

# Run with coverage
./gradlew test jacocoTestReport

# Validate entire refactoring workflow
./gradlew testRefactoringWorkflow
```

## Success Metrics and Coverage Requirements

### Minimum Coverage Requirements
- **Unit Tests**: 95% line coverage
- **Integration Tests**: 85% path coverage  
- **Edge Case Handling**: 90% branch coverage

### Quality Gates
- All tests must pass before proceeding to next phase
- No test failures allowed in CI pipeline
- Performance regression threshold: <5% build time increase
- Memory usage threshold: <10% increase during refactoring

### Documentation Requirements
- Each test must have clear `@DisplayName` annotations
- Test failure messages must provide actionable guidance
- Edge cases must be documented with rationale
- Integration test scenarios must map to real refactoring steps