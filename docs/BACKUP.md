# Villagers Reborn Backup Export

Overview
- The backup export captures the current villager dataset into a timestamped JSON file on disk.
- Trigger command: /villager backup export
- Implementation references:
  - Command registration: [`src/main/java/com/beeny/commands/VillagerCommands.registerVillagerCommand()`](src/main/java/com/beeny/commands/VillagerCommands.java:77)
  - Export logic: [`src/main/java/com/beeny/system/ServerVillagerManager.exportVillagerDataBackup()`](src/main/java/com/beeny/system/ServerVillagerManager.java:174)

How to run
1) Ensure you are in a world with villagers tracked by the mod (singleplayer or server).
2) In the game chat/console, execute:
   - /villager backup export
3) You will see feedback: "Villager data backup export triggered"
4) The file will be written to the world save directory (see File location below).

File location
- Root folder is the world save directory resolved by Minecraft:
  - <world>/villagersreborn/backups/<timestamp>/villagers.json
- Timestamp format: UTC in pattern yyyyMMdd-HHmmssZ (e.g., 20250131-120455Z)
- Example path:
  - .minecraft/saves/MyWorld/villagersreborn/backups/20250805-173012Z/villagers.json (client)
  - server/world/villagersreborn/backups/20250805-173012Z/villagers.json (dedicated server)
- Typical size:
  - Backups are typically small, ranging from a few KB to several hundred KB depending on world size and villager count.

Notes on export behavior
- Note: Backup exports exclude dead villagers; only villagers currently considered alive are included.
- Only villagers currently tracked by the ServerVillagerManager are exported (alive and not removed).
- The exporter builds the JSON manually with proper escaping; no extra dependencies are required.
- If the world directory cannot be resolved or directory creation fails, an error is logged and no file is written.

JSON structure
Top-level object contains one array field:

{
  "villagers": [ VillagerObject, ... ]
}

VillagerObject fields
All fields correspond to current in-memory villager data at the moment of export. Some may be null or empty depending on gameplay state.

- id: string
  - The UUID of the villager entity.
- name: string
  - The villager’s display name.
- age: number
  - The villager’s age.
- gender: string
  - Gender label stored in villager data.
- personality: string
  - Personality enum name (e.g., "FRIENDLY", "KIND").
- happiness: number
  - Current happiness value (0–100).
- professionHistory: string[]
  - Chronological list of profession names/labels the villager has had.
- spouseId: string or null
  - UUID of the spouse villager, if known.
- spouseName: string
  - Cached spouse name, if available.
- childrenIds: string[]
  - UUIDs of the villager’s children.
- childrenNames: string[]
  - Cached names of the villager’s children (parallel to childrenIds).
- birthPlace: string
  - Text description of birthplace.
- notes: string
  - Arbitrary notes associated with the villager.
- birthTime: number
  - Milliseconds since epoch when the villager was "born" (recorded).
- deathTime: number
  - Milliseconds since epoch if the villager died; default or 0 if not applicable.
- isAlive: boolean
  - True while the villager is considered alive.
- lastConversationTime: number
  - Milliseconds since epoch of the last conversation.
- totalTrades: number
  - Aggregate number of trades completed.
- favoriteFood: string
  - Favorite food label.
- hobby: string
  - Hobby enum name (e.g., "GARDENING", "FISHING").

Example JSON
{
  "villagers": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "Alice",
      "age": 25,
      "gender": "F",
      "personality": "KIND",
      "happiness": 87,
      "professionHistory": ["Novice Librarian", "Apprentice Librarian"],
      "spouseId": null,
      "spouseName": "",
      "childrenIds": ["c1", "c2"],
      "childrenNames": ["Bob", "Carol"],
      "birthPlace": "Village Center",
      "notes": "Enjoys trading paper",
      "birthTime": 1722520000000,
      "deathTime": 0,
      "isAlive": true,
      "lastConversationTime": 1722529999999,
      "totalTrades": 42,
      "favoriteFood": "bread",
      "hobby": "FISHING"
    }
  ]
}

Intended usage
- Archival: Keep periodic snapshots of villagers.json to track village evolution across sessions.
- Migration: Use exported data as a read-only source for external tools or analytics.
  - The mod does not include a built-in restore/import command at this time; the export is designed for backup/inspection purposes.

Troubleshooting
- No file appears:
  - Ensure the command ran on a server with an active world and the mod loaded.
  - Check logs for errors related to the backup directory creation or world path resolution.
- Empty villagers array:
  - The manager exports only tracked villagers. If none are tracked (e.g., no villagers loaded or attached with data), the array may be empty.

Technical references
- Export entry point:
  - [`src/main/java/com/beeny/commands/VillagerCommands`](src/main/java/com/beeny/commands/VillagerCommands.java:107)
- File and JSON serialization:
  - [`src/main/java/com/beeny/system/ServerVillagerManager`](src/main/java/com/beeny/system/ServerVillagerManager.java:174)
  - Serializer helpers used by export (private):
    - serializeObject(Map) and serializeValue(Object) ensure proper JSON escaping and array handling.
  
  See also
  - Configuration options and related settings: [`docs/CONFIG.md`](docs/CONFIG.md)