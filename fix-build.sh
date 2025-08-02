#!/bin/bash

# Recursive Gradle Build Fix Script
# Automatically attempts to fix common Gradle build issues until build succeeds
# Maintains functionality while addressing build failures

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "=== Recursive Gradle Build Fixer ==="
echo "Project: $(basename "$PROJECT_DIR")"
echo "Working directory: $PROJECT_DIR"
echo ""

# Maximum number of fix attempts
MAX_ATTEMPTS=10
attempt=1

# Known working fabric-loom versions to try as fallbacks
FABRIC_LOOM_VERSIONS=(
    "1.8.12"
    "1.8.11"
    "1.8.10"
    "1.8.9"
    "1.8.8"
    "1.8.7"
    "1.8.6"
    "1.8.5"
    "1.7.18"
    "1.7.17"
    "1.7.16"
    "1.7.15"
    "1.7.14"
    "1.7.13"
    "1.7.12"
    "1.6.12"
    "1.6.11"
    "1.6.10"
    "1.5.14"
    "1.4.5"
)

# Function to run gradle build and capture output
run_gradle_build() {
    local extra_args="$1"
    echo "Running: ./gradlew build $extra_args"
    if ./gradlew build $extra_args 2>&1; then
        return 0
    else
        return 1
    fi
}

# Function to analyze build failure and suggest fixes
analyze_failure() {
    local build_output="$1"
    
    if echo "$build_output" | grep -q "Plugin.*fabric-loom.*was not found"; then
        echo "ISSUE: fabric-loom plugin version not found"
        return 1
    elif echo "$build_output" | grep -q "Could not resolve.*fabric-loom"; then
        echo "ISSUE: fabric-loom dependency resolution failed"
        return 2
    elif echo "$build_output" | grep -q "Could not find.*minecraft"; then
        echo "ISSUE: Minecraft version not found"
        return 3
    elif echo "$build_output" | grep -q "Could not find.*yarn"; then
        echo "ISSUE: Yarn mappings not found"
        return 4
    elif echo "$build_output" | grep -q "Could not find.*fabric-api"; then
        echo "ISSUE: Fabric API version not found"
        return 5
    elif echo "$build_output" | grep -q "Compilation failed"; then
        echo "ISSUE: Java compilation errors"
        return 6
    else
        echo "ISSUE: Unknown build failure"
        return 99
    fi
}

# Function to attempt fabric-loom version fixes
fix_fabric_loom_version() {
    echo "Attempting to fix fabric-loom version issue..."
    
    for version in "${FABRIC_LOOM_VERSIONS[@]}"; do
        echo "  Trying fabric-loom version: $version"
        
        # Test if this version works
        if ./gradlew help -Ploom_version="$version" >/dev/null 2>&1; then
            echo "  ✓ Version $version is available, attempting build..."
            if run_gradle_build "-Ploom_version=$version"; then
                echo "  ✓ Build successful with fabric-loom $version"
                echo ""
                echo "SUCCESS: Build completed successfully!"
                echo "Solution: Use fabric-loom version $version"
                echo "Command: ./gradlew build -Ploom_version=$version"
                return 0
            else
                echo "  ✗ Build failed with version $version, trying next..."
            fi
        else
            echo "  ✗ Version $version not available"
        fi
    done
    
    echo "  ✗ No working fabric-loom version found"
    return 1
}

# Function to fix dependency resolution issues
fix_dependency_issues() {
    echo "Attempting to fix dependency resolution issues..."
    
    # Try with --refresh-dependencies
    echo "  Trying with refreshed dependencies..."
    if run_gradle_build "--refresh-dependencies"; then
        echo "  ✓ Build successful with refreshed dependencies"
        return 0
    fi
    
    # Try cleaning first
    echo "  Trying clean build..."
    if ./gradlew clean >/dev/null 2>&1 && run_gradle_build ""; then
        echo "  ✓ Build successful after clean"
        return 0
    fi
    
    echo "  ✗ Dependency fixes failed"
    return 1
}

# Function to fix compilation issues
fix_compilation_issues() {
    echo "Attempting to fix compilation issues..."
    
    # Check for common Java compilation issues
    echo "  Checking Java version compatibility..."
    java_version=$(java -version 2>&1 | grep "version" | cut -d'"' -f2 | cut -d'.' -f1)
    echo "  Java version: $java_version"
    
    # Try with different Java version targets if needed
    if run_gradle_build "-Djava.version=21"; then
        echo "  ✓ Build successful with Java 21 target"
        return 0
    fi
    
    echo "  ✗ Compilation fixes failed"
    return 1
}

# Main build fixing loop
echo "Starting recursive build fix process..."
echo ""

while [ $attempt -le $MAX_ATTEMPTS ]; do
    echo "=== Attempt #$attempt ==="
    
    # Capture build output
    build_output=$(./gradlew build 2>&1 || true)
    
    if echo "$build_output" | grep -q "BUILD SUCCESSFUL"; then
        echo "✓ Build successful!"
        echo ""
        echo "SUCCESS: Gradle build completed successfully on attempt #$attempt"
        exit 0
    fi
    
    echo "✗ Build failed on attempt #$attempt"
    echo ""
    
    # Analyze the failure
    failure_type=$(analyze_failure "$build_output")
    failure_code=$?
    
    echo "Analysis: $failure_type"
    echo ""
    
    # Apply appropriate fix based on failure type
    case $failure_code in
        1|2)  # fabric-loom issues
            if fix_fabric_loom_version; then
                exit 0
            fi
            ;;
        3|4|5)  # dependency issues
            if fix_dependency_issues; then
                exit 0
            fi
            ;;
        6)  # compilation issues
            if fix_compilation_issues; then
                exit 0
            fi
            ;;
        *)  # unknown issues
            echo "Attempting generic fixes..."
            if ./gradlew clean >/dev/null 2>&1 && run_gradle_build "--refresh-dependencies"; then
                echo "✓ Build successful with clean + refresh"
                exit 0
            fi
            ;;
    esac
    
    echo "Fix attempt #$attempt failed, trying next approach..."
    echo ""
    
    ((attempt++))
done

echo "FAILURE: Unable to fix build issues after $MAX_ATTEMPTS attempts"
echo ""
echo "Final build output:"
echo "==================="
echo "$build_output"
echo "==================="
echo ""
echo "Manual intervention may be required."
exit 1