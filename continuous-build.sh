#!/bin/bash

# Continuous Build Script for villagesreborn
# Recursively runs gradlew build until all errors are fixed
# Preserves functionality while addressing build failures

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "=== Continuous Gradle Build for villagesreborn ==="
echo "Project directory: $PROJECT_DIR"
echo "Objective: Run gradlew build continuously until all issues are resolved"
echo ""

# Maximum attempts
MAX_ATTEMPTS=20
attempt=1

# Known fabric-loom versions that might work (from actual releases)
LOOM_VERSIONS=(
    "1.8-SNAPSHOT"
    "1.7-SNAPSHOT" 
    "1.6-SNAPSHOT"
    "1.5-SNAPSHOT"
    "1.4-SNAPSHOT"
    "1.3-SNAPSHOT"
    "1.2-SNAPSHOT"
    "1.1-SNAPSHOT"
    "1.0-SNAPSHOT"
    "0.12-SNAPSHOT"
    "0.11-SNAPSHOT"
    "0.10-SNAPSHOT"
)

# Function to backup original build files
backup_files() {
    if [ ! -f "build.gradle.backup" ]; then
        cp build.gradle build.gradle.backup
        echo "✓ Backed up original build.gradle"
    fi
    if [ ! -f "gradle.properties.backup" ]; then
        cp gradle.properties gradle.properties.backup  
        echo "✓ Backed up original gradle.properties"
    fi
}

# Function to restore original files  
restore_files() {
    if [ -f "build.gradle.backup" ]; then
        cp build.gradle.backup build.gradle
        echo "✓ Restored original build.gradle"
    fi
    if [ -f "gradle.properties.backup" ]; then
        cp gradle.properties.backup gradle.properties
        echo "✓ Restored original gradle.properties"
    fi
}

# Function to test if a specific loom version works
test_loom_version() {
    local version="$1"
    echo "  Testing fabric-loom version: $version"
    
    # Try using command line property override first (preserves gradle.properties)
    if ./gradlew help -Ploom_version="$version" >/dev/null 2>&1; then
        echo "  ✓ Version $version is accessible"
        if ./gradlew build -Ploom_version="$version" >/dev/null 2>&1; then
            echo "  ✓ Build successful with fabric-loom $version!"
            return 0
        else
            echo "  ⚠ Version $version accessible but build failed"
            return 1
        fi
    else
        echo "  ✗ Version $version not accessible"
        return 1
    fi
}

# Function to attempt fabric-loom version fixes
fix_loom_version() {
    echo "Attempting to fix fabric-loom version issues..."
    echo "Current loom_version in gradle.properties: $(grep loom_version gradle.properties | cut -d'=' -f2)"
    echo ""
    
    # First try command-line property overrides to preserve gradle.properties
    for version in "${LOOM_VERSIONS[@]}"; do
        if test_loom_version "$version"; then
            echo ""
            echo "SUCCESS: Found working fabric-loom version: $version"
            echo "Solution: Use command-line override: ./gradlew build -Ploom_version=$version"
            return 0
        fi
    done
    
    echo "✗ No working fabric-loom version found with property overrides"
    return 1
}

# Function to try alternative approaches
try_alternative_fixes() {
    echo "Attempting alternative build fixes..."
    
    # Try with Gradle's offline mode (use cached dependencies)
    echo "  Trying offline mode..."
    if ./gradlew build --offline >/dev/null 2>&1; then
        echo "  ✓ Build successful in offline mode"
        return 0
    fi
    
    # Try refreshing dependencies
    echo "  Trying dependency refresh..."
    if ./gradlew build --refresh-dependencies >/dev/null 2>&1; then
        echo "  ✓ Build successful with refreshed dependencies"
        return 0
    fi
    
    # Try clean build
    echo "  Trying clean build..."
    if ./gradlew clean build >/dev/null 2>&1; then
        echo "  ✓ Build successful after clean"
        return 0
    fi
    
    # Try with different Gradle daemon
    echo "  Trying with fresh Gradle daemon..."
    if ./gradlew --stop >/dev/null 2>&1 && ./gradlew build >/dev/null 2>&1; then
        echo "  ✓ Build successful with fresh daemon"
        return 0
    fi
    
    echo "  ✗ All alternative approaches failed"
    return 1
}

# Function to run a single build attempt
run_build_attempt() {
    echo "Running: ./gradlew build"
    
    # Capture build output
    build_output=$(./gradlew build 2>&1 || true)
    
    if echo "$build_output" | grep -q "BUILD SUCCESSFUL"; then
        echo "✓ Build successful!"
        return 0
    else
        echo "✗ Build failed"
        echo ""
        echo "Error analysis:"
        if echo "$build_output" | grep -q "fabric-loom.*was not found"; then
            echo "  Issue: fabric-loom plugin version not found"
            echo "  Current version: $(grep loom_version gradle.properties | cut -d'=' -f2)"
        elif echo "$build_output" | grep -q "Could not resolve"; then
            echo "  Issue: Dependency resolution failure"
        elif echo "$build_output" | grep -q "Compilation failed"; then
            echo "  Issue: Java compilation errors"
        else
            echo "  Issue: Unknown build failure"
        fi
        echo ""
        return 1
    fi
}

# Main execution
echo "Creating file backups..."
backup_files
echo ""

# Set up cleanup trap
trap 'restore_files; echo "Cleanup completed."; exit' EXIT INT TERM

echo "Starting continuous build process..."
echo ""

# Main build loop
while [ $attempt -le $MAX_ATTEMPTS ]; do
    echo "=== Attempt #$attempt/$MAX_ATTEMPTS ==="
    
    if run_build_attempt; then
        echo ""
        echo "🎉 SUCCESS: Build completed successfully on attempt #$attempt!"
        echo ""
        echo "The build is now working. You can run:"
        echo "  ./gradlew build"
        echo ""
        restore_files
        exit 0
    fi
    
    # If build failed, try to fix it
    echo "Attempting to fix build issues..."
    
    if fix_loom_version; then
        echo "Retrying build with fixed configuration..."
        if run_build_attempt; then
            echo ""
            echo "🎉 SUCCESS: Build fixed and completed on attempt #$attempt!"
            restore_files
            exit 0
        fi
    fi
    
    if try_alternative_fixes; then
        echo ""
        echo "🎉 SUCCESS: Build fixed with alternative approach on attempt #$attempt!"
        restore_files  
        exit 0
    fi
    
    echo "Fix attempt #$attempt failed. Trying again..."
    echo ""
    
    ((attempt++))
done

echo "❌ FAILURE: Unable to fix build after $MAX_ATTEMPTS attempts"
echo ""
echo "The following issues were identified but could not be automatically resolved:"
./gradlew build 2>&1 | grep -E "(FAILURE|ERROR|Could not|was not found)" | head -10
echo ""
echo "Manual intervention may be required."
echo "Original files have been restored."

restore_files
exit 1