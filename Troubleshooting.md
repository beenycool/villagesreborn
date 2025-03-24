# Troubleshooting Guide

This guide helps you resolve common issues you might encounter while using Villages Reborn.

## Performance Issues

### Low FPS
- Ensure your system meets the minimum requirements
- Lower the AI processing settings in mod configuration
- Reduce village size and density settings
- Update your graphics drivers

### Memory Issues
- Allocate more RAM to Minecraft (recommended: 4GB minimum)
- Close unnecessary background applications
- Check for memory leaks using F3 debug menu

## AI-Related Issues

### Villager Behavior Problems
- Verify LLM configuration in mod settings
- Check if selected AI model is properly downloaded
- Ensure API keys are correctly configured (if using external AI services)
- Try resetting villager data if behavior seems corrupted

### Model Download Issues
- Check internet connection
- Verify available disk space
- Try manual download from mod settings menu
- Check logs for specific error messages

## Cultural Features

### Missing Structures
- Ensure structure generation is enabled
- Try regenerating village (in creative mode)
- Check for conflicts with other terrain generation mods
- Verify file integrity of mod installation

### Crafting Issues
- Confirm you have required relationship levels with villagers
- Check if crafting station is properly placed
- Verify recipe unlocks in player data
- Ensure required materials are correct type/variant

## Network & Multiplayer

### Sync Issues
- Verify all players have same mod version
- Check server configuration
- Ensure proper fabric API version
- Try reconnecting to server

### Event Problems
- Check server tick rate
- Verify event scheduling in server config
- Ensure proper synchronization settings
- Check for timezone conflicts

## Installation Problems

### Mod Conflicts
```
Common incompatible mods:
- Village overhaul mods
- AI behavior mods
- Structure generation mods
```

### Fabric API Issues
- Verify Fabric API version matches mod requirements
- Try updating/reinstalling Fabric loader
- Check Minecraft version compatibility

## General Fixes

1. **First Steps**
   - Check logs for error messages
   - Verify mod version matches Minecraft version
   - Ensure all dependencies are installed

2. **Reset Steps**
   - Delete and reinstall mod
   - Clear config files
   - Create new world to test
   - Reset mod settings to default

3. **Still Having Issues?**
   - Share logs on our Discord
   - Report bugs on GitHub
   - Check existing issues for solutions

## Config File Locations

- Main config: `.minecraft/config/villagesreborn.json`
- AI settings: `.minecraft/config/villagesreborn/ai_config.json`
- Village data: `.minecraft/config/villagesreborn/village_data/`

## Debug Mode

Enable debug mode in settings to:
- View detailed logs
- Access debug GUI (F6)
- Monitor AI processing
- Track villager behavior

## Getting Help

If you're still experiencing issues:

1. **Discord Support**
   - Join our [Discord server](https://discord.gg/villagesreborn)
   - Post in #support channel
   - Include your logs and system info

2. **GitHub Issues**
   - Check [existing issues](https://github.com/beeny/villagesreborn/issues)
   - Create new issue with detailed description
   - Include crash reports and logs

3. **Documentation**
   - Review [setup guide](Installation-Guide)
   - Check [compatibility list](Compatibility)
   - Read [FAQ](FAQ)
