# Installation Guide

This guide provides detailed instructions for installing Villages Reborn and setting up your environment for the best experience.

## System Requirements

### Minimum Requirements
- Minecraft 1.21.4
- Fabric Loader (latest version)
- 4GB RAM allocation
- Java 17 or higher
- Storage: 500MB free space

### Recommended Requirements
- 8GB RAM allocation
- SSD storage
- Multi-core processor (4+ cores)
- Dedicated GPU with 2GB+ VRAM
- Stable internet connection (for Advanced AI features)

## Step-by-Step Installation

### 1. Install Fabric Loader
1. Visit [Fabric's official website](https://fabricmc.net/use/)
2. Download the installer for your operating system
3. Run the installer
4. Select Minecraft version 1.21.4
5. Click "Install"

### 2. Install Required Dependencies
1. [Fabric API](https://modrinth.com/mod/fabric-api) (required)
2. Additional recommended mods:
   - [Mod Menu](https://modrinth.com/mod/modmenu)
   - [Cloth Config API](https://modrinth.com/mod/cloth-config)
   - [Architectury API](https://modrinth.com/mod/architectury-api)

### 3. Install Villages Reborn
1. Download the latest Villages Reborn from [Modrinth](https://modrinth.com/mod/villages-reborn)
2. Locate your Minecraft mods folder:
   - Windows: `%appdata%\.minecraft\mods`
   - macOS: `~/Library/Application Support/minecraft/mods`
   - Linux: `~/.minecraft/mods`
3. Place the downloaded JAR file into the mods folder
4. Create the folder if it doesn't exist
5. Ensure all dependency mods are also in the mods folder

## First Launch Setup

When you first launch Minecraft with Villages Reborn installed, you'll see a setup wizard with three options:

### 1. Quick Start Mode (Recommended)
- Ideal for most players
- Pre-configured AI settings
- No external services required
- Balanced performance optimization

### 2. Advanced Setup Mode
- For customizing AI capabilities
- Configure external LLM services
- Higher quality villager interactions
- Requires additional configuration

### 3. Basic Mode
- Village enhancements without advanced AI
- Best performance on lower-end systems
- Cultural villages with simplified behaviors
- No internet connection required

After selecting your preferred mode, the mod will:
- Create necessary configuration files
- Download required resources
- Generate initial village data
- Configure optimal settings for your system

## Configuration Files

Villages Reborn creates several configuration files in your `.minecraft/config` folder:

- `villagesreborn_general.json`: General mod settings
- `villagesreborn_llm.json`: AI configuration options
- `villagesreborn_ui.json`: User interface preferences
- `villagesreborn.json`: Core mod configuration

These files can be edited manually or through the in-game settings menu.

## In-Game Configuration

### Accessing Mod Settings
1. Launch Minecraft
2. Go to Options → Mod Settings
3. Select "Villages Reborn"

### General Settings
- Village Generation: Frequency and size of villages
- Cultural Distribution: Balance between different village types
- Villager Density: Number of villagers per village
- Event Frequency: How often special events occur

### AI Settings
- Model Selection: Choose AI model for villager interactions
- Memory Length: Control how much history villagers remember
- Response Timing: Adjust AI response speed
- Processing Limits: Set maximum AI operations per game tick

### Performance Optimization
- Rendering Detail: Adjust cultural structure complexity
- Animation Quality: Control villager animation smoothness
- Processing Priority: Balance between AI and game performance
- Background Processing: Enable/disable offline AI calculations

## Verifying Installation

After installation, follow these steps to verify everything is working correctly:

1. Create a new world (Superflat is quickest for testing)
2. Enable cheats for testing purposes
3. Use commands to check functionality:
   - `/locate structure villages_reborn:village_roman`
   - `/village info`
   - Right-click a villager to test interactions

## Optimizing Performance

If you experience performance issues:

1. Reduce "AI Complexity" in settings
2. Lower "Villager Animation Detail"
3. Decrease "Maximum Active Villagers"
4. Disable "Background Processing"
5. Switch to Basic Mode if problems persist

## Cross-Platform Installation

### MultiMC / Prism Launcher
1. Create a new Minecraft 1.21.4 instance
2. Select "Install Fabric"
3. Add Villages Reborn and dependencies to the instance mods folder
4. Configure instance memory allocation (8GB recommended)

### Server Installation
1. Set up a Fabric server for Minecraft 1.21.4
2. Place Villages Reborn and dependencies in the server's mods folder
3. Configure server properties for optimal performance
4. Adjust AI settings in config files for server load

## Updating the Mod

When updating Villages Reborn:

1. Download the latest version
2. Back up your world and configuration files
3. Delete the old Villages Reborn JAR file
4. Place the new JAR in your mods folder
5. Launch the game to apply updates
6. Check configuration files for new options

## Compatibility Notes

### Compatible Mods
- Performance optimization mods (Sodium, Lithium, etc.)
- Most utility and quality-of-life mods
- JEI/REI item lookup mods
- Mini-map and waypoint mods

### Known Compatibility Issues
- Mods that completely overhaul village generation
- Some AI-related NPC enhancement mods
- Certain world generation mods without patches

## Troubleshooting Installation

If you encounter issues during installation:

1. Verify you're using the correct Minecraft version (1.21.4)
2. Ensure all required dependencies are installed
3. Check for mod conflicts
4. Review logs in `.minecraft/logs` folder
5. See our [Troubleshooting Guide](Troubleshooting.md) for more help

## Additional Resources

- [User Guide](USER_GUIDE.md): Complete instructions for gameplay
- [Cultural Villages](Cultural-Villages.md): Details on each village type
- [FAQ](FAQ.md): Answers to common questions
- [Discord Support](https://discord.gg/villagesreborn): Community help and updates

## Next Steps

After successful installation:
1. Explore the world to find cultural villages
2. Interact with villagers to learn their personalities
3. Participate in village events and activities
4. Discover cultural crafting recipes
5. Build relationships with villagers to unlock special features
