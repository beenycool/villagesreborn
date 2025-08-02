# Villagers Reborn

Hey there! 👋 Welcome to Villagers Reborn—a Minecraft Fabric mod I made to give villagers cool custom names and let them chat with you using AI!

## What Is This Mod?

I always thought Minecraft villagers were kinda boring, so I made this mod to spice things up. Now, every villager gets a unique name that matches their job and the biome they live in. No more "Villager"—you'll meet folks like "Blacksmith Alex Ironforge" or "Librarian Emma Bookworth". If they switch jobs, their names change too, but their first name stays the same so you can keep track of your favorites.

Plus, villagers can actually talk! Thanks to AI (like OpenAI, Gemini, or even local models), they have conversations based on their job, who they know, and what's been happening around them.

## Features

### Villager Naming
- Every villager gets a special name based on their profession and biome
- Names have a job title and a random first name
- Names update automatically if they change jobs
- You can always see their names above their heads
- Names match the biome they're from (so desert villagers sound different than snowy ones!)
- Supports all the usual Minecraft jobs:
  - Armorer, Butcher, Cartographer, Cleric, Farmer, Fisherman, Fletcher, Leatherworker, Librarian, Mason, Shepherd, Toolsmith, Weaponsmith

### Dynamic Dialogue System
- Villagers chat using AI (OpenAI, Gemini, or local models)
- They talk about their job, friends, and recent stuff that happened
- There's a journal system to keep track of conversations and relationships
- You can see family trees and even set up marriages!
- Villagers have schedules and routines

## Commands

You can use these commands to interact with villagers:

### Basic Commands
- `/villager rename <entities> <name>` — Rename villagers however you want
- `/villager rename nearest <name>` — Rename the closest villager
- `/villager list` — See all named villagers nearby, with their jobs and locations
- `/villager find <name>` — Search for villagers by name (even partial matches)
- `/villager randomize` — Give all villagers new random names

### Advanced Commands
- `/villager journal` — Open the villager journal
- `/villager familytree` — See villager family trees
- `/villager marriage` — Manage villager marriages
- `/villager removecomments` — Clear all villager comments/journal entries

## Config Stuff

You can tweak settings in `config/villagersreborn.json`:

### AI Settings
- `enableDynamicDialogue`: Turn AI chat on or off
- `llmProvider`: Choose "gemini", "local", or "openrouter"
- `llmApiKey`: Your API key (required for "gemini" and "openrouter")
- `llmLocalUrl`: The local API endpoint for "local" provider (e.g., Ollama: `http://localhost:11434/v1/chat/completions`, llama.cpp: `http://localhost:8080`)
- `llmApiEndpoint`: Optional override for remote API endpoints (used with "gemini" or "openrouter" if you need a custom URL)
- `llmModel`: Which model to use
- `llmTemperature`: How creative the responses are (0.0–2.0)
- `llmMaxTokens`: Max response length
- `llmRequestTimeout`: Timeout in seconds

### Local LLM Support
You can use local AI models like Ollama or llama.cpp:
- Set `llmProvider` to `"local"`
- Set `llmLocalUrl` to your local API endpoint:
  - Ollama: `http://localhost:11434/v1/chat/completions`
  - llama.cpp: `http://localhost:8080`
- Set `llmModel` to your model's name

For remote providers ("gemini", "openrouter"), use `llmApiEndpoint` only if you need to override the default API URL.

## How to Install

1. Get [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.7
2. Download the latest mod release
3. Grab [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
4. Put both mods in your `.minecraft/mods` folder
5. Start Minecraft with Fabric!

## How to Use

Just install and play! Villagers will get names automatically when they spawn, and you'll see their names above their heads. If they change jobs, their names update too.

Use the villager journal item to chat and keep track of your conversations.

## For Developers

I built this mod with:
- Java 21
- Fabric Loader 0.16.14
- Fabric API 0.129.0+1.21.7
- Minecraft 1.21.7

### Building

1. Clone the repo
2. Run `./gradlew build`
3. Find the mod in `build/libs/`

### Setting Up for Coding

1. Clone the repo
2. Open in your favorite IDE (I use IntelliJ IDEA, but Eclipse works too)
3. Run `./gradlew genSources` to get Minecraft sources
4. Run `./gradlew idea` or `./gradlew eclipse` for IDE setup

## How It Works

I used Fabric's mixin system to change how villagers behave:

1. When a villager spawns, they get a random name based on their job
2. The name is saved with the villager
3. The name shows up above their head
4. If they change jobs, their name updates (but keeps their first name)
5. Villager chat is powered by AI and saved in a journal

## Credits

- Made by Beeny (that's me!)
- Built with Fabric modding tools
- Inspired by wanting villagers to feel more like real people

## License

This mod is CC0-1.0 licensed—do whatever you want with it!