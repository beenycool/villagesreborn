# Installation Guide

This guide provides detailed instructions for installing Villages Reborn and setting up your environment for the best experience.

## System Requirements

### Minimum Requirements
- Minecraft 1.21.4
- Fabric Loader
- 4GB RAM allocation
- Java 17 or higher
- Storage: 500MB free space

### Recommended Requirements
- 8GB RAM allocation
- SSD storage
- Multi-core processor
- Stable internet connection (for AI features)

## Step-by-Step Installation

### 1. Install Fabric Loader
1. Visit [Fabric's official website](https://fabricmc.net/use/)
2. Download the installer for your operating system
3. Run the installer
4. Select Minecraft version 1.21.4
5. Click "Install"

### 2. Install Villages Reborn
1. Download Villages Reborn from [Modrinth](https://modrinth.com/mod/villages-reborn)
2. Locate your Minecraft mods folder:
   - Windows: `%appdata%/.minecraft/mods`
   - macOS: `~/Library/Application Support/minecraft/mods`
   - Linux: `~/.minecraft/mods`
3. Place the downloaded JAR file into the mods folder
4. Create the folder if it doesn't exist

### 3. Install Required Dependencies
1. Fabric API (required)
2. Additional recommended mods:
   - Mod Menu
   - Cloth Config API
   - Fabric Language Kotlin

## First Launch Setup

1. Launch Minecraft with Fabric profile
2. On first launch, the mod will:
   - Create configuration files
   - Download necessary AI models
   - Generate initial village data

## Configuration

### Basic Settings
1. Open Minecraft Settings
2. Click "Mod Settings"
3. Find "Villages Reborn"
4. Adjust basic settings:
   - Village generation frequency
   - Cultural diversity options
   - Performance settings

### AI Configuration
1. Navigate to AI settings tab
2. Choose your preferred AI provider
3. Configure API settings if using external services
4. Adjust processing limits based on your system

## Verifying Installation

1. Create a new world
2. Enable cheats (for testing)
3. Use `/locate village` command
4. Check for cultural village generation
5. Interact with villagers to test AI features

## Common Installation Issues

### Game Crashes on Launch
- Verify Fabric API version
- Check for mod conflicts
- Review crash reports in `.minecraft/crash-reports`

### Villages Not Generating
- Check world generation settings
- Verify mod is properly loaded
- Clear biome cache if updating existing world

### AI Features Not Working
- Check internet connection
- Verify AI configuration
- Ensure system meets requirements

## Updating the Mod

1. Download new version
2. Replace old JAR in mods folder
3. Backup your saves and configs
4. Launch game to verify update

## Compatibility

### Known Compatible Mods
- Structure mods (with patches)
- Performance optimization mods
- Most utility mods

### Potential Conflicts
- Village overhaul mods
- AI-related mods
- Some terrain generation mods

## Additional Resources

- [Troubleshooting Guide](Troubleshooting)
- [FAQ](FAQ)
- [Discord Support](https://discord.gg/villagesreborn)

## Next Steps

After installation:
1. Read the [Getting Started Guide](Getting-Started)
2. Review [Controls & Keybinds](Controls)
3. Check [Cultural Village Guide](Cultural-Villages)
