#!/bin/bash

# Advanced Gradle Build Fixer for villagesreborn
# Recursively attempts to fix build issues until gradle build succeeds
# Maintains all functionality while resolving dependency and version conflicts

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "=== Advanced Recursive Gradle Build Fixer ==="
echo "Target: villagesreborn Minecraft mod"
echo "Objective: Continuous build execution until success"
echo "Constraints: Preserve gradle.properties, maintain functionality"
echo ""

MAX_ATTEMPTS=15
attempt=1

# Create backup directory if it doesn't exist
mkdir -p .build-fix-backups

# Backup function
backup_original_files() {
    if [ ! -f ".build-fix-backups/build.gradle.original" ]; then
        cp build.gradle .build-fix-backups/build.gradle.original
        echo "✓ Original build.gradle backed up"
    fi
    if [ ! -f ".build-fix-backups/gradle.properties.original" ]; then
        cp gradle.properties .build-fix-backups/gradle.properties.original
        echo "✓ Original gradle.properties backed up"
    fi
}

# Restore function
restore_original_files() {
    if [ -f ".build-fix-backups/build.gradle.original" ]; then
        cp .build-fix-backups/build.gradle.original build.gradle
        echo "✓ Restored original build.gradle"
    fi
    if [ -f ".build-fix-backups/gradle.properties.original" ]; then
        cp .build-fix-backups/gradle.properties.original gradle.properties
        echo "✓ Restored original gradle.properties"
    fi
}

# Function to test if fabric-loom version works by temporarily modifying build.gradle
test_fabric_loom_version_via_temp_modification() {
    local version="$1"
    echo "  Testing fabric-loom $version via temporary build.gradle modification..."
    
    # Create temporary build.gradle with hardcoded version
    cp build.gradle build.gradle.temp
    sed "s/version \"\${loom_version}\"/version \"$version\"/" build.gradle > build.gradle.modified
    cp build.gradle.modified build.gradle
    
    # Test the build
    if ./gradlew help >/dev/null 2>&1; then
        echo "    ✓ Configuration successful with $version"
        if ./gradlew build >/dev/null 2>&1; then
            echo "    ✓ BUILD SUCCESSFUL with fabric-loom $version!"
            cp build.gradle.temp build.gradle
            rm -f build.gradle.temp build.gradle.modified
            return 0
        else
            echo "    ⚠ Configuration works but build failed"
        fi
    else
        echo "    ✗ Configuration failed with $version"
    fi
    
    # Restore original build.gradle
    cp build.gradle.temp build.gradle
    rm -f build.gradle.temp build.gradle.modified
    return 1
}

# Function to check what's available in fabric repositories
discover_working_fabric_loom_versions() {
    echo "Discovering working fabric-loom versions..."
    
    # Try known stable versions that should exist
    local test_versions=(
        "1.8-SNAPSHOT"
        "1.7-SNAPSHOT"
        "1.6-SNAPSHOT"
        "1.5-SNAPSHOT"
        "1.4-SNAPSHOT"
        "1.3-SNAPSHOT"
        "0.12-SNAPSHOT"
        "0.11-SNAPSHOT"
        "0.10-SNAPSHOT"
        "0.9-SNAPSHOT"
        "0.8-SNAPSHOT"
    )
    
    for version in "${test_versions[@]}"; do
        if test_fabric_loom_version_via_temp_modification "$version"; then
            echo ""
            echo "SUCCESS: Found working version: $version"
            echo "Creating permanent fix..."
            
            # Create a permanent working build.gradle 
            sed "s/version \"\${loom_version}\"/version \"$version\"/" .build-fix-backups/build.gradle.original > build.gradle.fixed
            cp build.gradle.fixed build.gradle
            
            return 0
        fi
    done
    
    echo "✗ No working fabric-loom version found"
    return 1
}

# Function to try gradle property overrides (preserves gradle.properties)
try_gradle_property_overrides() {
    echo "Attempting gradle property overrides (preserves gradle.properties)..."
    
    local test_versions=(
        "1.8-SNAPSHOT"
        "1.7-SNAPSHOT"
        "1.6-SNAPSHOT"  
        "1.5-SNAPSHOT"
        "1.4-SNAPSHOT"
        "0.12-SNAPSHOT"
        "0.11-SNAPSHOT"
    )
    
    for version in "${test_versions[@]}"; do
        echo "  Testing override: -Ploom_version=$version"
        if ./gradlew build -Ploom_version="$version" >/dev/null 2>&1; then
            echo "  ✓ SUCCESS with property override!"
            echo ""
            echo "SOLUTION FOUND: Use gradle property override"
            echo "Command: ./gradlew build -Ploom_version=$version"
            echo ""
            echo "This preserves gradle.properties while fixing the build."
            return 0
        fi
    done
    
    echo "  ✗ Property overrides didn't work"
    return 1
}

# Function to try build environment fixes
try_build_environment_fixes() {
    echo "Attempting build environment fixes..."
    
    # Clean everything
    echo "  Cleaning project..."
    ./gradlew clean >/dev/null 2>&1 || true
    
    # Clear gradle caches
    echo "  Clearing gradle caches..."
    rm -rf ~/.gradle/caches/ 2>/dev/null || true
    
    # Try with refresh dependencies
    echo "  Trying with refreshed dependencies..."
    if ./gradlew build --refresh-dependencies >/dev/null 2>&1; then
        echo "  ✓ Build successful with refreshed dependencies"
        return 0
    fi
    
    # Try offline mode (use only cached dependencies)
    echo "  Trying offline mode..."
    if ./gradlew build --offline >/dev/null 2>&1; then
        echo "  ✓ Build successful in offline mode"
        return 0
    fi
    
    echo "  ✗ Environment fixes didn't work"
    return 1
}

# Main build test function
test_current_build() {
    echo "Testing current build configuration..."
    build_output=$(./gradlew build 2>&1 || true)
    
    if echo "$build_output" | grep -q "BUILD SUCCESSFUL"; then
        return 0
    else
        echo "Build failed with current configuration"
        echo "Error details:"
        echo "$build_output" | grep -E "(FAILURE|fabric-loom.*was not found|Could not resolve|Plugin.*was not found)" | head -3
        echo ""
        return 1
    fi
}

# Setup
echo "Setting up build fix environment..."
backup_original_files
echo ""

# Cleanup trap
trap 'restore_original_files; echo "Cleanup completed."; exit' EXIT INT TERM

# Main execution loop
echo "Starting recursive build fixing process..."
echo ""

while [ $attempt -le $MAX_ATTEMPTS ]; do
    echo "=== ATTEMPT #$attempt/$MAX_ATTEMPTS ==="
    
    if test_current_build; then
        echo "✅ BUILD SUCCESSFUL on attempt #$attempt!"
        echo ""
        echo "The gradle build is now working correctly."
        echo "You can run: ./gradlew build"
        echo ""
        restore_original_files
        exit 0
    fi
    
    echo "Build failed on attempt #$attempt. Applying fixes..."
    echo ""
    
    # Strategy 1: Try gradle property overrides (preserves gradle.properties)
    if [ $attempt -le 3 ]; then
        if try_gradle_property_overrides; then
            echo "Fix applied successfully via property override."
            echo "Testing the fix..."
            if test_current_build; then
                echo "✅ BUILD SUCCESSFUL with property override fix!"
                restore_original_files
                exit 0
            fi
        fi
    fi
    
    # Strategy 2: Try temporary build.gradle modifications  
    if [ $attempt -le 8 ]; then
        if discover_working_fabric_loom_versions; then
            echo "Fix applied via build.gradle modification."
            echo "Testing the fix..."
            if test_current_build; then
                echo "✅ BUILD SUCCESSFUL with build.gradle fix!"
                # Keep the fixed build.gradle since it works
                exit 0
            fi
        fi
    fi
    
    # Strategy 3: Try environment fixes
    if [ $attempt -le 12 ]; then
        if try_build_environment_fixes; then
            echo "✅ BUILD SUCCESSFUL with environment fix!"
            restore_original_files
            exit 0
        fi
    fi
    
    echo "Fix attempt #$attempt failed. Trying next approach..."
    echo ""
    
    # Restore for next attempt
    restore_original_files
    
    ((attempt++))
done

echo "❌ FAILED: Could not fix build after $MAX_ATTEMPTS attempts"
echo ""
echo "Final error summary:"
./gradlew build 2>&1 | grep -E "(FAILURE|ERROR|Could not|was not found)" | head -5
echo ""
echo "Manual intervention required."
echo "Original files restored."

restore_original_files
exit 1