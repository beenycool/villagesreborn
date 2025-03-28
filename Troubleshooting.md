# Troubleshooting Guide

This guide helps you resolve common issues you might encounter while using Villages Reborn.

## Performance Issues

### Low FPS / Game Stuttering
- Ensure your system meets the minimum requirements
- Lower the AI processing settings in mod configuration:
  - In-game: Options → Mod Settings → Villages Reborn → AI Settings → Lower "AI Complexity"
  - Config file: Reduce `maxProcessingTime` in `villagesreborn_llm.json`
- Reduce village size and density settings:
  - Decrease `villagerDensity` and `structureComplexity` in settings
- Optimize Minecraft performance:
  - Install performance mods like Sodium, Lithium, and Phosphor
  - Reduce render distance and graphics settings
  - Update your graphics drivers
  - Close unnecessary background applications

### Memory Issues
- Allocate more RAM to Minecraft (recommended: 4GB minimum, 8GB optimal)
  - In launcher: Edit profile → More Options → JVM Arguments → Change `-Xmx2G` to `-Xmx4G` or higher
- Enable garbage collection optimization:
  - Add `-XX:+UseG1GC -XX:+ParallelRefProcEnabled` to JVM arguments
- Monitor memory usage with F3 debug menu
- Disable "Background Processing" in mod settings if RAM usage is high
- Try using "Basic Mode" if persistent memory issues occur

## AI-Related Issues

### Villager Behavior Problems
- Verify AI configuration in settings:
  - Check that the selected model is compatible with your setup
  - Ensure API keys are correctly entered (if using Advanced Mode)
- Try resetting villager data:
  - Use command `/village reset_ai` near problematic village
  - Or delete the folder `.minecraft/config/villagesreborn/village_data/[world_id]`
- Verify AI response status:
  - Enable "Show AI Status" in UI settings
  - Check for errors in the status indicator
- Switch to Quick Start mode temporarily to isolate the issue

### Model Download Issues
- Check internet connection and firewall settings
- Verify available disk space (at least 1GB free)
- Try manual download:
  - In-game: Options → Mod Settings → Villages Reborn → AI Settings → "Download Models"
  - Or download directly from our [model repository](https://villagesreborn.net/models)
- Check logs for specific error messages:
  - Review `.minecraft/logs/latest.log` for download errors
  - Look for "VillagesReborn: Model download failed" entries
- Try connecting through a different network if possible

### Slow AI Responses
- Enable response caching in AI settings
- Reduce context length in advanced settings
- Limit active village size in generation settings
- Try using lightweight model option
- Disable "Detailed Personality" feature for better performance
- Increase "Response Timeout" if responses are cutting off

## Village Generation Issues

### Villages Not Spawning
- Verify generation settings:
  - Check that village generation frequency is not set too low
  - Ensure cultural villages are enabled for all types
- Try using location commands:
  - `/locate structure villages_reborn:village_roman` (also try other culture types)
- Generate test village using:
  - `/village generate roman 5` (creates a Roman village of size 5)
- Check for biome compatibility:
  - Roman villages need plains or forest biomes
  - Egyptian villages require desert or savanna biomes
  - Victorian villages spawn in plains, forest, or mountain biomes
  - NYC villages appear in plains or river biomes
- Create a new world if updating from previous mod version

### Corrupted or Incomplete Villages
- Reset village structure:
  - `/village regen` while standing in problematic village
- Check for mod conflicts with other structure generation mods
- Verify world generation settings aren't limiting structure sizes
- Try disabling "Experimental Generation" in settings
- Ensure mod files aren't corrupted (reinstall if necessary)

## Cultural Features

### Missing Cultural Buildings
- Ensure cultural structures are enabled in configuration
- Check world generation settings for structure size limits
- Try regenerating village with `/village regen` command
- Verify compatibility with terrain modification mods
- Check for resource pack conflicts affecting structures

### Crafting System Problems
- Verify crafting stations are properly placed and undamaged
- Confirm relationship levels with villagers:
  - Use `/village reputation` to check your standing
  - Most special recipes require at least "Friendly" status
- Check recipe unlocks:
  - Talk to master crafters in villages
  - Participate in cultural events to unlock recipes
  - Complete villager tasks to improve reputation
- Ensure you have all required materials:
  - Some recipes need culture-specific resources
  - Check JEI/REI for complete recipe information

### Event Issues
- Events not triggering:
  - Check `eventFrequency` setting (higher values = more events)
  - Verify village has required cultural buildings for events
  - Ensure village has enough villagers (minimum 5 for most events)
  - Wait for appropriate in-game time/season for specific events
- Event participation problems:
  - Make sure you have sufficient reputation with village
  - Stay within event boundaries during participation
  - Complete event prerequisites if required

## Network & Multiplayer

### Synchronization Issues
- Verify all players and server have identical mod versions
- Check server configuration for AI limitations
- Try lowering synchronization frequency in network settings
- Restart server if AI responses appear inconsistent
- Set `networkCompressionLevel` to balance bandwidth and performance

### Permissions Problems
- Ensure all players have appropriate permissions for commands
- Configure command permissions in server properties
- Use permission manager mods for granular control
- Set `allowClientCommands` to true in server configuration

### Server Performance
- Configure server-side AI limitations:
  - Reduce `maxServerAIOperations` in config
  - Lower village density for server worlds
  - Enable "Fast Path" AI option for multiplayer
- Optimize server JVM arguments:
  - Allocate sufficient RAM (minimum 8GB recommended)
  - Use Aikars flags for better performance
- Consider running AI processing as background thread

## Installation Problems

### Mod Conflicts
Common incompatible mods:
- Village overhaul mods: Minecolonies, Millenaire (without compatibility patch)
- Structure generation mods: Repurposed Structures (requires config adjustment)
- AI behavior mods: Most villager AI enhancers with behavior modifications

Resolving conflicts:
1. Identify conflicting mods through testing
2. Check for compatibility patches
3. Adjust configurations when possible
4. Prioritize which mod controls village generation

### Crash on Startup
- Check crash reports in `.minecraft/crash-reports/`
- Verify Fabric API version matches mod requirements
- Try minimal mod setup (just Villages Reborn and required dependencies)
- Update Java to latest version
- Check for hardware compatibility issues
- Try Clean Installation:
  1. Backup saves and configurations
  2. Delete mod files and config folder
  3. Reinstall Fabric and mod with dependencies
  4. Test with new world

## Config File Management

### Config File Locations
- Main config: `.minecraft/config/villagesreborn.json`
- AI settings: `.minecraft/config/villagesreborn_llm.json`
- UI settings: `.minecraft/config/villagesreborn_ui.json`
- General settings: `.minecraft/config/villagesreborn_general.json`

### Restoring Default Configuration
- Delete config files and restart game to regenerate defaults
- Use in-game "Reset to Default" button in settings
- Download backup configs from our [repository](https://github.com/beeny/villages-reborn/config)

### Config Optimization Templates
For different system capabilities:
- Low-end systems: Use our [low-resource config](https://villagesreborn.net/configs/low-end.json)
- Mid-range systems: Default configuration is optimized for this
- High-end systems: [Enhanced AI config](https://villagesreborn.net/configs/high-end.json)
- Server optimization: [Server-focused config](https://villagesreborn.net/configs/server.json)

## Debug Tools

### Debug Mode
Enable debug mode in settings to:
- View detailed logs in-game
- Access debug GUI (press F6)
- Monitor AI processing statistics
- Track villager behavior patterns
- Enable development commands

### Logging Options
- Standard logging: No configuration needed
- Verbose logging: Set `logLevel` to "VERBOSE" in config
- AI process logging: Enable "Log AI Requests" in advanced settings
- Output file: Check `.minecraft/logs/villagesreborn.log`

### Performance Analysis
Use built-in performance tools:
- Press F6 → Performance tab to view metrics
- Enable "Show Performance Overlay" in debug settings
- Check "Timings Report" for detailed execution time
- Generate performance report with `/village debug report`

## Getting Help

If you're still experiencing issues:

1. **Discord Support**
   - Join our [Discord server](https://discord.gg/villagesreborn)
   - Post in #support channel with details
   - Share logs and system information
   - Include screenshots/videos when possible

2. **GitHub Issues**
   - Check [existing issues](https://github.com/beeny/villagesreborn/issues) for solutions
   - Create new issue with detailed description
   - Include crash reports and logs
   - Describe steps to reproduce the problem

3. **Gathering Information for Support**
   - Generate debug report: `/village debug report`
   - Share mod and system info: Press F3 + C (copy debug info)
   - Include mod version and Minecraft version
   - Describe your system specifications
