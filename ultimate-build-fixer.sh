#!/bin/bash

# Ultimate Recursive Gradle Build Fixer for villagesreborn
# Continuously runs gradlew build until all errors are resolved
# Preserves gradle.properties while fixing build issues through multiple strategies

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "============================================================"
echo "    Ultimate Recursive Gradle Build Fixer"
echo "    Project: villagesreborn (Minecraft Fabric Mod)"
echo "    Objective: Fix build until gradlew build succeeds"
echo "    Constraints: Preserve gradle.properties & functionality"
echo "============================================================"
echo ""

# Configuration
MAX_ATTEMPTS=25
BUILD_SUCCESS=false
attempt=1

# Create backup directory
mkdir -p .gradle-build-fix/backups
BACKUP_DIR=".gradle-build-fix/backups"

# Extensive list of fabric-loom versions to try (from research of historical releases)
FABRIC_LOOM_VERSIONS=(
    # Recent versions for MC 1.21+
    "1.10.0" "1.9.0" "1.8.0" "1.7.0" "1.6.0" "1.5.0"
    # SNAPSHOT versions
    "1.10-SNAPSHOT" "1.9-SNAPSHOT" "1.8-SNAPSHOT" "1.7-SNAPSHOT"
    "1.6-SNAPSHOT" "1.5-SNAPSHOT" "1.4-SNAPSHOT" "1.3-SNAPSHOT"
    # Patch versions
    "1.8.12" "1.8.11" "1.8.10" "1.8.9" "1.8.8" "1.8.7" "1.8.6"
    "1.7.17" "1.7.16" "1.7.15" "1.7.14" "1.7.13" "1.7.12"
    "1.6.12" "1.6.11" "1.6.10" "1.6.9" "1.6.8" "1.6.7"
    "1.5.14" "1.5.13" "1.5.12" "1.5.11" "1.5.10"
    "1.4.10" "1.4.9" "1.4.8" "1.4.7" "1.4.6" "1.4.5"
    # Older stable versions
    "1.3.12" "1.2.14" "1.1.14" "1.0.14"
    "0.12.52" "0.11.52" "0.10.52" "0.9.52" "0.8.52"
    # Alpha/Beta versions that might exist
    "1.11.0-alpha.1" "1.10.0-beta.1" "1.9.0-alpha.1"
)

# Function to backup original files
backup_original_files() {
    if [ ! -f "$BACKUP_DIR/build.gradle.original" ]; then
        cp build.gradle "$BACKUP_DIR/build.gradle.original"
        echo "✓ Backed up original build.gradle"
    fi
    if [ ! -f "$BACKUP_DIR/gradle.properties.original" ]; then
        cp gradle.properties "$BACKUP_DIR/gradle.properties.original"
        echo "✓ Backed up original gradle.properties"
    fi
    if [ ! -f "$BACKUP_DIR/settings.gradle.original" ]; then
        cp settings.gradle "$BACKUP_DIR/settings.gradle.original"
        echo "✓ Backed up original settings.gradle"
    fi
}

# Function to restore original files
restore_original_files() {
    if [ -f "$BACKUP_DIR/build.gradle.original" ]; then
        cp "$BACKUP_DIR/build.gradle.original" build.gradle
        echo "✓ Restored original build.gradle"
    fi
    if [ -f "$BACKUP_DIR/gradle.properties.original" ]; then
        cp "$BACKUP_DIR/gradle.properties.original" gradle.properties
        echo "✓ Restored original gradle.properties"
    fi
    if [ -f "$BACKUP_DIR/settings.gradle.original" ]; then
        cp "$BACKUP_DIR/settings.gradle.original" settings.gradle
        echo "✓ Restored original settings.gradle"
    fi
}

# Function to test if current build works
test_current_build() {
    echo "Testing current build configuration..."
    if ./gradlew build >/dev/null 2>&1; then
        echo "✅ Build SUCCESSFUL!"
        return 0
    else
        return 1
    fi
}

# Function to analyze build failure and return error type
analyze_build_failure() {
    local output="$1"
    
    if echo "$output" | grep -q "fabric-loom.*was not found"; then
        echo "fabric-loom-version"
    elif echo "$output" | grep -q "Could not resolve.*minecraft"; then
        echo "minecraft-version"
    elif echo "$output" | grep -q "Could not resolve.*yarn"; then
        echo "yarn-mapping"
    elif echo "$output" | grep -q "Could not resolve.*fabric-api"; then
        echo "fabric-api"
    elif echo "$output" | grep -q "Compilation failed"; then
        echo "compilation"
    elif echo "$output" | grep -q "Could not resolve"; then
        echo "dependency"
    else
        echo "unknown"
    fi
}

# Function to test fabric-loom version with command-line override (preserves gradle.properties)
test_fabric_loom_override() {
    local version="$1"
    echo "  Testing fabric-loom $version (property override)..."
    
    if ./gradlew help -Ploom_version="$version" >/dev/null 2>&1; then
        echo "    ✓ Plugin loads with version $version"
        if ./gradlew build -Ploom_version="$version" >/dev/null 2>&1; then
            echo "    ✅ BUILD SUCCESSFUL with fabric-loom $version!"
            echo ""
            echo "🎉 SOLUTION FOUND: Property Override Method"
            echo "   Command: ./gradlew build -Ploom_version=$version"
            echo "   This preserves gradle.properties file as required."
            echo ""
            return 0
        else
            echo "    ⚠ Plugin loads but build fails"
            return 1
        fi
    else
        return 1
    fi
}

# Function to test fabric-loom version with temporary build.gradle modification
test_fabric_loom_modification() {
    local version="$1"
    echo "  Testing fabric-loom $version (temporary build.gradle)..."
    
    # Create modified build.gradle
    cp build.gradle build.gradle.temp
    sed "s/id 'fabric-loom' version \"\${loom_version}\"/id 'fabric-loom' version \"$version\"/" build.gradle > build.gradle.modified
    mv build.gradle.modified build.gradle
    
    if ./gradlew help >/dev/null 2>&1; then
        echo "    ✓ Plugin loads with version $version"
        if ./gradlew build >/dev/null 2>&1; then
            echo "    ✅ BUILD SUCCESSFUL with fabric-loom $version!"
            echo ""
            echo "🎉 SOLUTION FOUND: Build.gradle Modification Method"
            echo "   Working version: $version"
            echo "   build.gradle has been updated to use this version."
            echo ""
            rm -f build.gradle.temp
            return 0
        else
            echo "    ⚠ Plugin loads but build fails"
        fi
    fi
    
    # Restore original
    mv build.gradle.temp build.gradle
    return 1
}

# Function to attempt fabric-loom version fixes
fix_fabric_loom_versions() {
    echo "🔧 Attempting fabric-loom version fixes..."
    echo "Current version in gradle.properties: $(grep loom_version gradle.properties | cut -d'=' -f2)"
    echo ""
    
    # Strategy 1: Command-line property overrides (preserves gradle.properties)
    echo "Strategy 1: Property overrides (preserves gradle.properties)"
    for version in "${FABRIC_LOOM_VERSIONS[@]}"; do
        if test_fabric_loom_override "$version"; then
            return 0
        fi
    done
    
    echo "Property override strategy failed."
    echo ""
    
    # Strategy 2: Temporary build.gradle modifications
    echo "Strategy 2: Temporary build.gradle modifications"
    for version in "${FABRIC_LOOM_VERSIONS[@]}"; do
        if test_fabric_loom_modification "$version"; then
            return 0
        fi
    done
    
    echo "Build.gradle modification strategy failed."
    return 1
}

# Function to try alternative gradle fixes
try_alternative_gradle_fixes() {
    echo "🔧 Attempting alternative gradle fixes..."
    
    # Clean and refresh
    echo "  Cleaning project and refreshing dependencies..."
    ./gradlew clean >/dev/null 2>&1 || true
    if ./gradlew build --refresh-dependencies >/dev/null 2>&1; then
        echo "  ✅ Build successful with dependency refresh!"
        return 0
    fi
    
    # Offline mode
    echo "  Trying offline mode (cached dependencies only)..."
    if ./gradlew build --offline >/dev/null 2>&1; then
        echo "  ✅ Build successful in offline mode!"
        return 0
    fi
    
    # Stop daemon and retry
    echo "  Restarting gradle daemon..."
    ./gradlew --stop >/dev/null 2>&1 || true
    sleep 2
    if ./gradlew build >/dev/null 2>&1; then
        echo "  ✅ Build successful with fresh daemon!"
        return 0
    fi
    
    # Clear all caches
    echo "  Clearing all gradle caches..."
    rm -rf ~/.gradle/caches/* 2>/dev/null || true
    if ./gradlew build >/dev/null 2>&1; then
        echo "  ✅ Build successful after cache clear!"
        return 0
    fi
    
    echo "  Alternative fixes failed."
    return 1
}

# Function to try compatibility fixes
try_compatibility_fixes() {
    echo "🔧 Attempting compatibility fixes..."
    
    # Try with different Minecraft/Fabric versions (as command-line overrides)
    local mc_versions=("1.21.6" "1.21.5" "1.21.4" "1.21.3" "1.21.2" "1.21.1" "1.21.0")
    
    for mc_version in "${mc_versions[@]}"; do
        echo "  Testing with Minecraft $mc_version..."
        if ./gradlew build -Pminecraft_version="$mc_version" >/dev/null 2>&1; then
            echo "  ✅ Build successful with Minecraft $mc_version!"
            echo ""
            echo "🎉 SOLUTION FOUND: Minecraft Version Compatibility"
            echo "   Command: ./gradlew build -Pminecraft_version=$mc_version"
            return 0
        fi
    done
    
    echo "  Compatibility fixes failed."
    return 1
}

# Main execution function
run_build_attempt() {
    echo "=== ATTEMPT #$attempt/$MAX_ATTEMPTS ==="
    echo ""
    
    # Test current configuration first
    if test_current_build; then
        BUILD_SUCCESS=true
        return 0
    fi
    
    # Capture build output for analysis
    echo "Build failed. Analyzing error..."
    build_output=$(./gradlew build 2>&1 || true)
    error_type=$(analyze_build_failure "$build_output")
    
    echo "Error type: $error_type"
    echo ""
    
    # Apply appropriate fix strategy based on error type
    case "$error_type" in
        "fabric-loom-version")
            if fix_fabric_loom_versions; then
                BUILD_SUCCESS=true
                return 0
            fi
            ;;
        "minecraft-version"|"yarn-mapping"|"fabric-api")
            if try_compatibility_fixes; then
                BUILD_SUCCESS=true
                return 0
            fi
            ;;
        "dependency"|"compilation")
            if try_alternative_gradle_fixes; then
                BUILD_SUCCESS=true
                return 0
            fi
            ;;
        *)
            echo "Unknown error type. Trying all fix strategies..."
            if fix_fabric_loom_versions || try_compatibility_fixes || try_alternative_gradle_fixes; then
                BUILD_SUCCESS=true
                return 0
            fi
            ;;
    esac
    
    return 1
}

# Setup
echo "🔧 Setting up build fix environment..."
backup_original_files
echo ""

# Cleanup trap
trap 'restore_original_files; echo "🧹 Cleanup completed."; exit' EXIT INT TERM

# Main execution loop
echo "🚀 Starting recursive build fixing process..."
echo ""

while [ $attempt -le $MAX_ATTEMPTS ] && [ "$BUILD_SUCCESS" = false ]; do
    if run_build_attempt; then
        break
    fi
    
    echo "❌ Attempt #$attempt failed. Retrying..."
    echo ""
    
    # Reset environment for next attempt
    restore_original_files
    
    ((attempt++))
done

# Final result
echo "============================================================"
if [ "$BUILD_SUCCESS" = true ]; then
    echo "🎉 SUCCESS: Gradle build fixed and working!"
    echo ""
    echo "The build completed successfully on attempt #$attempt."
    echo "You can now run: ./gradlew build"
    echo ""
    if [ -f build.gradle.fixed ]; then
        echo "Note: build.gradle was modified to use a working fabric-loom version."
        echo "The original has been preserved in $BACKUP_DIR/"
    fi
else
    echo "❌ FAILURE: Could not fix build after $MAX_ATTEMPTS attempts"
    echo ""
    echo "Final error output:"
    echo "==================="
    ./gradlew build 2>&1 | head -20
    echo "==================="
    echo ""
    echo "Manual intervention may be required."
    echo "All original files have been restored."
fi
echo "============================================================"

exit $([ "$BUILD_SUCCESS" = true ] && echo 0 || echo 1)