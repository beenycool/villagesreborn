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

## Finding Villages

Villages spawn naturally throughout the world in appropriate biomes:
- Roman villages typically appear in plains and forest biomes
- Egyptian villages generate in desert and savanna biomes
- Victorian villages can be found in plains, forest, and mountain biomes
- NYC-style villages appear in plains and river biomes

Use the `/locate structure villages_reborn:village_roman` command (replace with appropriate culture) to find the nearest cultural village, or explore naturally to discover them.

## Interacting with Villagers

Villages Reborn villagers are more intelligent and interactive than vanilla Minecraft villagers:

1. **Conversations**: Right-click on a villager to start a conversation. They'll respond based on their personality, profession, culture, and your past interactions.

2. **Relationships**: Villagers form relationships with each other and with players:
   - **Friendship**: Build rapport through positive interactions, gift-giving, and completing tasks
   - **Trust**: Develop trust by keeping promises and helping during events
   - **Community standing**: Your overall reputation in the village affects all interactions

3. **Dynamic Activities**: Villagers engage in culturally appropriate activities:
   - **Roman**: Bathing, debating in forums, trading in markets
   - **Egyptian**: Ceremonial gatherings, crafting, agricultural work
   - **Victorian**: Social tea gatherings, industrial crafting, gardening
   - **NYC**: Business meetings, coffee breaks, artistic pursuits

4. **Villager Crafting**: When interacting with villagers at their workstations, you can access special crafting recipes unique to their culture and profession:
   - **Cultural Artisans**: Unlock decorative items and cultural artifacts
   - **Specialized Crafters**: Create tools and functional items specific to the village culture
   - **Master Villagers**: Highly skilled villagers can teach you rare recipes after building strong relationships

## Village Events

Villages host special events based on their culture:

### Roman Events
- **Forum Debates**: Participate in philosophical discussions for knowledge rewards
- **Market Festivals**: Trade special goods during seasonal markets
- **Gladiatorial Games**: Combat entertainment events with prizes

### Egyptian Events
- **Pyramid Ceremonies**: Ritual gatherings during full moons
- **Harvest Festivals**: Agricultural celebrations with food-based rewards
- **Nile Festivals**: Water-themed events with special trading opportunities

### Victorian Events
- **Garden Parties**: Social gatherings with exclusive trading
- **Industrial Exhibitions**: Showcase new crafting techniques
- **Holiday Markets**: Seasonal trading events with unique items

### NYC Events
- **Art Shows**: Cultural events featuring special decorative items
- **Food Festivals**: Culinary events with unique consumables
- **Business Conventions**: Trading-focused events with rare goods

## Commands

Villages Reborn adds several useful commands:

- `/village info`: Displays information about the nearest village
- `/village events`: Shows active and upcoming village events
- `/village culture <type>`: Provides details about a specific culture
- `/villager personality <ID>`: Reveals details about a villager's personality
- `/village reputation`: Shows your standing with nearby villages
- `/village generate <culture> <radius>`: Creates a new village with specified culture
- `/village reset_reputation`: Resets your reputation in the current village

## Settings and Configuration

Access the mod settings through the Options menu in Minecraft. The available settings depend on your chosen setup mode:

### Quick Start Mode
- No API configuration required
- Pre-configured for immediate play
- Basic AI features using built-in responses
- Can upgrade to Advanced mode anytime

### Advanced Mode Settings
- API Key: For connecting to language model services
- Endpoint URL: The server endpoint for AI requests
- Model Selection: Choose from various AI providers
- Performance Settings: Adjust response caching and context length
- Memory Usage: Control how much history villagers remember

### General Settings (All Modes)
- Village Generation: Frequency and size of cultural villages
- Villager Density: Number of villagers per village
- Event Frequency: How often special events occur
- UI Settings: Customize interaction interfaces
- Performance Options: Adjust complexity for different hardware capabilities

## Cultural Crafting System

Each culture offers unique crafting opportunities:

1. **Cultural Materials**: Gather culture-specific resources from villages
2. **Relationship-Based Unlocks**: Build strong connections with villagers to learn recipes
3. **Workstations**: Use specialized crafting stations found in each village type
4. **Cultural Artifacts**: Craft decorative and functional items with cultural significance

### Crafting Progression
1. Begin with basic cultural recipes from any villager
2. Build relationships to unlock intermediate recipes
3. Participate in events for special material access
4. Master cultural crafting by learning from elder villagers

## Troubleshooting

Common issues and solutions:

1. **Game Crashes**: 
   - Try setting `detailedAnimations` to false in the config file
   - Update to the latest mod version
   - Verify compatibility with other installed mods

2. **AI-Related Issues**:
   - If using Quick Start mode: Verify you have the latest mod version
   - If using Advanced mode: Check that your API key and endpoint are correctly configured
   - If neither works: Try switching to Quick Start mode temporarily
   - Check your network connection if external LLM services aren't responding

3. **Performance Issues**:
   - Lower the values for `maxVillagerAI` and `maxStructureComplexity` in configuration
   - Reduce `eventDensity` to decrease simultaneous events
   - Switch to Quick Start mode which has optimized default settings
   - Adjust `memoryLength` to reduce how much history villagers remember
   - Use the performance optimization settings in the configuration menu

4. **Setup Problems**:
   - If unsure which mode to choose, start with Quick Start
   - Advanced mode not working? Verify your internet connection and API credentials
   - Can't switch modes? Try deleting the config files and restarting
   - Installation issues? Verify Fabric API is properly installed

5. **Village Generation Issues**:
   - Villages not appearing? Check `generationFrequency` in configuration
   - Strange village layouts? Try regenerating the village or creating a new world
   - Culture-specific issues? Verify all resource assets are properly loaded

## Compatibility

Villages Reborn is compatible with most mods that don't fundamentally alter village generation or villager behavior. Known compatible mods include:
- JEI/REI
- OptiFine/Sodium/Iris
- Waystones
- Fabric API (required)
- Architectury API (required)

Known incompatibilities:
- Mods that completely overhaul village generation
- Some AI villager enhancement mods that modify core villager behavior

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
- Fabric Loader version
- List of other installed mods
- Error logs (if applicable)
- Steps to reproduce the issue