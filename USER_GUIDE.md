# Villages Reborn User Guide

Welcome to Villages Reborn, a comprehensive overhaul mod that transforms Minecraft villages into culturally diverse, intelligent communities!

## Getting Started

When you first launch the game with Villages Reborn installed, you'll be presented with three setup options:

1. **Quick Start (Recommended)**: Jump right into playing with basic AI features enabled. No configuration needed!
2. **Advanced Setup**: Configure custom LLM settings for enhanced villager intelligence.
3. **Skip AI Features**: Play with basic village enhancements only.

After setup, you'll discover enhanced villages around the world. These villages come in four cultural variations:

- **Roman**: Mediterranean-inspired architecture with forums, villas, and bathhouses
- **Egyptian**: Desert settlements with temples, pyramids, and oases
- **Victorian**: Industrial-era towns with factories, terraced houses, and cobblestone streets
- **NYC**: Modern urban areas with skyscrapers, retail districts, and subway entrances

## Interacting with Villagers

Villages Reborn villagers are more intelligent and interactive than vanilla Minecraft villagers:

1. **Conversations**: Right-click on a villager to start a conversation. They'll respond based on their personality, profession, and your past interactions.

2. **Relationships**: Villagers form relationships with each other and with players. You can become friends with villagers by regularly interacting with them positively.

3. **Dynamic Activities**: Villagers have schedules and engage in activities like farming, socializing, reading, and more based on their profession and the time of day.

4. **Villager Crafting**: When interacting with villagers at their workstations, you can access special crafting recipes unique to their culture and profession.

## Village Events

Villages occasionally host special events based on their culture:

- Festivals
- Markets
- Celebrations
- Cultural ceremonies

These events bring villagers together in central locations and provide opportunities for special trades and interactions.

## Commands

Villages Reborn adds several useful commands:

- `/village info`: Displays information about the nearest village
- `/village events`: Shows active and upcoming village events
- `/villager personality <name>`: Reveals details about a villager's personality
- `/village generate <culture> <radius>`: Creates a new village with specified culture

## Settings and Configuration

Access the mod settings through the Options menu in Minecraft. The available settings depend on your chosen setup mode:

### Quick Start Mode
- No API configuration required
- Pre-configured for immediate play
- Basic AI features using built-in responses
- Can upgrade to Advanced mode anytime

### Advanced Mode Settings
- API Key: For connecting to language model services
- Endpoint: The server endpoint for AI requests
- Model Selection: Choose from various AI providers
- Performance Settings: Adjust response caching and context length

### General Settings (All Modes)
- Feature toggles for various mod components
- Performance options for complex village features
- Villager behavior adjustments

## Troubleshooting

Common issues and solutions:

1. **Game Crashes**: Try setting `detailedAnimations` to false in the config file

2. **AI-Related Issues**:
   - If using Quick Start mode: Verify you have the latest mod version
   - If using Advanced mode: Check that your API key and endpoint are correctly configured
   - If neither works: Try switching to Quick Start mode temporarily

3. **Performance Issues**:
   - Lower the values for `maxVillagerAI` and `maxStructureComplexity`
   - Switch to Quick Start mode which has optimized default settings
   - Reduce the number of active village events

4. **Setup Problems**:
   - If unsure which mode to choose, start with Quick Start
   - Advanced mode not working? Verify your internet connection and API credentials
   - Can't switch modes? Try deleting the config file and restarting

5. **Missing Structures**: Verify that all required resource packs are enabled

## Compatibility

Villages Reborn is compatible with most mods that don't fundamentally alter village generation or villager behavior. Known compatible mods include:
- JEI/REI
- OptiFine/Sodium
- Waystones

## Support

For help, bug reports, or feature suggestions:

### Community Support
- GitHub Issues: [https://github.com/beeny/villages-reborn/issues](https://github.com/beeny/villages-reborn/issues)
- Discord: [Villages Reborn Community](https://discord.gg/villagesreborn)
- Wiki: [Villages Reborn Wiki](https://villagesreborn.net/wiki)

### Mode-Specific Help
- Quick Start Mode: [Getting Started Guide](https://villagesreborn.net/quickstart)
- Advanced Mode: [AI Setup Tutorial](https://villagesreborn.net/advanced)
- Performance Tips: [Optimization Guide](https://villagesreborn.net/performance)

When reporting issues, please include:
- Your setup mode (Quick Start/Advanced/Skip AI)
- Mod version number
- Minecraft version
- Error logs (if applicable)