# Villages Reborn Mod (1.21.4)

Overhauls village mechanics with enterprise-grade AI systems and deep management features.

## Key Systems

### Advanced Villager AI
- Emotional state tracking (EmotionManager)
- Long-term memory system (MemorySystem)
- Local LLM integration for dialog (LocalLLMManager)
- Patrol routes & threat response (PatrolBehavior)
- Hardware-accelerated TTS (LocalTTSManager)

### Economic Simulation
- Villager banking & loans (VillagerBank)
- Resource-based construction (ConstructionManager)
- Dynamic market pricing (ResourceManager)
- Player tax system (GovernanceSystem)

### Player Interaction
- Multi-stage quests with branching paths (QuestManager)
- Role-based permissions (PlayerRole)
- Custom potions (ReturnToBedPotionItem)
- Village governance voting

### Defense & Security
- Threat level assessment
- Automated patrol coordination
- Crime detection & justice system
- Wall construction AI

## Installation
1. Install [Fabric Loader 0.15.8+](https://fabricmc.net/use/)
2. Download Villages-Reborn-1.21.4.jar
3. Place in `%appdata%/.minecraft/mods`
4. Configure `villagesreborn-config.json`:
```json
{
  "ai": {
    "llm_enabled": true,
    "max_threads": 2
  },
  "economy": {
    "interest_rate": 0.05,
    "tax_interval_hours": 6
  }
}
```

## Hardware Requirements
- Minimum: 4GB RAM, SSD, OpenCL 1.2+ GPU
- Recommended: 8GB RAM, NVMe, CUDA-capable GPU
- AI Features require Windows 10+/Linux kernel 5.4+

## Troubleshooting
```bash
# Enable debug logging
jq '.logging.level = "DEBUG"' villagesreborn-config.json| sponge villagesreborn-config.json

# Reset villager AI
mc-command /villagesreborn resetai
```

[Documentation Portal](https://villagesreborn.dev) | [Bug Reports](https://github.com/VillagesReborn/Issues)
