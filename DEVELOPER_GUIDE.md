# Villages Reborn Developer Guide

This guide provides information for developers who want to understand, modify or contribute to the Villages Reborn mod.

## Project Structure

The mod is organized into several key packages:

- `com.beeny`: Root package containing main mod class and client initialization
- `com.beeny.village`: Core village-related functionality
- `com.beeny.gui`: User interface components
- `com.beeny.config`: Configuration handling
- `com.beeny.network`: Networking for client-server communication
- `com.beeny.mixin`: Minecraft class modifications using Mixin
- `com.beeny.commands`: In-game commands
- `com.beeny.api`: Public API for other mods to interact with
- `com.beeny.ai`: AI integration for villager behavior

## Key Components

### SpawnRegion
Manages the generation of culturally-themed villages. Each region has a specific culture (Roman, Egyptian, Victorian, NYC) that determines its architecture and villager behaviors.

### VillagerAI
Powers enhanced villager behavior with personality traits, decision-making capabilities, and social interactions. Integrates with the LLM service for natural language generation.

### VillagerManager
Central management system for all villagers, their relationships, and village statistics. Handles spawning, despawning, and synchronization of villager data.

### CulturalStructureGenerator
Generates structures based on cultural templates, handling both predefined structures from NBT files and procedurally generated buildings.

### VillageCraftingManager
Manages custom crafting recipes that are specific to villager cultures and professions.

## Setup Development Environment

1. Clone the repository
2. Import as a Gradle project in your preferred IDE
3. Run the Gradle task `genSources` to generate Minecraft sources
4. Run the Gradle task `runClient` to test the mod

## Adding a New Culture

To add a new cultural theme:

1. Create new structure generation methods in `SpawnRegion.java`
2. Add the culture name to the appropriate enums/lists
3. Create structure templates in `resources/data/villagesreborn/structures/[culture_name]/`
4. Add decoration and structure generation logic
5. Update the `CulturalStructureGenerator` to handle the new culture

## Working with LLM Integration

The mod uses LLM (Large Language Model) services for generating dynamic content:

1. Check `LLMService.java` for the interface to language models
2. Village and villager behaviors can be customized by modifying the prompts
3. Local model support can be added through the `ModelDownloader` class

## Contributing Guidelines

1. Follow the existing code style and naming conventions
2. Write unit tests for new functionality
3. Document public methods with JavaDoc comments
4. Create a pull request with a clear description of changes

## Debugging Tips

- Use the mod's logger with appropriate levels (debug, info, warn, error)
- Check mixin conflicts if encountering crashes
- Test compatibility with other mods
- Use JVM arguments to increase memory allocation when testing large villages

## Build and Distribution

Create a release build with the Gradle task `build`. The output JAR will be in `build/libs/`.