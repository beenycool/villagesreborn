# Villagers Reborn

A Minecraft Fabric mod that gives villagers custom names based on their professions.

## Description

This mod enhances the Minecraft experience by giving each villager a unique name that reflects their profession. Instead of generic names like "Villager", you'll see names like "Blacksmith Alex" or "Librarian Emma". When villagers change professions, their names update accordingly to match their new job.

## Features

- Villagers get unique names based on their profession
- Names consist of a profession title and a random first name
- Names update automatically when villagers change professions
- Names remain visible above villagers' heads for easy identification
- Supports all standard Minecraft villager professions:
  - Armorer
  - Butcher
  - Cartographer
  - Cleric
  - Farmer
  - Fisherman
  - Fletcher
  - Leatherworker
  - Librarian
  - Mason
  - Shepherd
  - Toolsmith
  - Weaponsmith

## Installation

1. Download and install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.7
2. Download the latest version of this mod from the releases page
3. Download [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api) mod
4. Place both mod files in your `.minecraft/mods` folder
5. Launch Minecraft with the Fabric profile

## Usage

Once installed, the mod works automatically. Villagers will be assigned names based on their professions when they spawn. The names will appear above their heads and update when they change professions.

## Development

This mod is built using:
- Java 21
- Fabric Loader 0.16.14
- Fabric API 0.129.0+1.21.7
- Minecraft 1.21.7

### Building from Source

1. Clone the repository
2. Run `./gradlew build` to build the mod
3. The built mod will be in `build/libs/`

### Setup for Development

1. Clone the repository
2. Import the project into your preferred IDE (IntelliJ IDEA or Eclipse)
3. Run `./gradlew genSources` to generate Minecraft sources
4. Run `./gradlew idea` or `./gradlew eclipse` to generate IDE project files

## How It Works

The mod uses Fabric's mixin system to modify villager behavior:

1. When a villager is created, it's assigned a random name based on its profession
2. The name is stored as an attachment to the villager entity
3. The name is displayed above the villager's head
4. When a villager changes profession, the name is updated to reflect the new profession while keeping the same first name

## Credits

- Developed by Beeny
- Uses the Fabric modding toolchain
- Inspired by the desire to make villagers more personable and identifiable

## License

This mod is licensed under the CC0-1.0 license.