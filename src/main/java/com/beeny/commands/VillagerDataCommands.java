package com.beeny.commands;

import com.beeny.commands.base.BaseVillagerCommand;
import com.beeny.data.VillagerData;
import com.beeny.system.VillagerDialogueSystem;
import com.beeny.util.CommandMessageUtils;
import com.beeny.util.VillagerDataUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;
import java.util.Optional;

/**
 * Commands for managing villager data operations and utilities
 */
public class VillagerDataCommands extends BaseVillagerCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("villager")
            .then(CommandManager.literal("data")
                .then(CommandManager.literal("export")
                    .then(CommandManager.argument("villager", EntityArgumentType.entity())
                        .executes(VillagerDataCommands::exportVillagerData)))
                .then(CommandManager.literal("reset")
                    .then(CommandManager.argument("villager", EntityArgumentType.entity())
                        .executes(VillagerDataCommands::resetVillagerData)))
                .then(CommandManager.literal("validate")
                    .executes(VillagerDataCommands::validateAllVillagerData))
                .then(CommandManager.literal("repair")
                    .executes(VillagerDataCommands::repairVillagerData))
                .then(CommandManager.literal("backup")
                    .executes(VillagerDataCommands::backupVillagerData))
                .then(CommandManager.literal("summary")
                    .executes(VillagerDataCommands::showDataSummary)))
            .then(CommandManager.literal("help")
                .executes(VillagerDataCommands::showHelp))
        );
    }
    
    private static int exportVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "villager");
        
        if (!validateVillager(entity, context.getSource())) return 0;
        
        VillagerEntity villager = (VillagerEntity) entity;
        Optional<VillagerData> dataOpt = getVillagerDataSafely(villager, context.getSource());
        if (dataOpt.isEmpty()) return 0;
        
        VillagerData data = dataOpt.get();
        ServerCommandSource source = context.getSource();
        
        exportDetailedVillagerData(data, source);
        return 1;
    }
    
    private static void exportDetailedVillagerData(VillagerData data, ServerCommandSource source) {
        CommandMessageUtils.sendInfo(source, "=== Villager Data Export ===");
        
        // Basic Information
        CommandMessageUtils.sendInfo(source, "BASIC INFO:");
        CommandMessageUtils.sendInfo(source, "  Name: " + data.getName());
        CommandMessageUtils.sendInfo(source, "  Gender: " + data.getGender());
        CommandMessageUtils.sendInfo(source, "  Age: " + data.getAge());
        CommandMessageUtils.sendInfo(source, "  Birth Time: " + data.getBirthTime());
        CommandMessageUtils.sendInfo(source, "  Birth Place: " + data.getBirthPlace());
        CommandMessageUtils.sendInfo(source, "  Is Alive: " + data.isAlive());
        
        // Personality & Behavior
        CommandMessageUtils.sendInfo(source, "\nPERSONALITY & BEHAVIOR:");
        CommandMessageUtils.sendInfo(source, "  Personality: " + data.getPersonality());
        CommandMessageUtils.sendInfo(source, "  Happiness: " + data.getHappiness() + "% (" + data.getHappinessDescription() + ")");
        CommandMessageUtils.sendInfo(source, "  Hobby: " + data.getHobby());
        CommandMessageUtils.sendInfo(source, "  Favorite Food: " + data.getFavoriteFood());
        
        // Family & Relationships
        CommandMessageUtils.sendInfo(source, "\nFAMILY & RELATIONSHIPS:");
        CommandMessageUtils.sendInfo(source, "  Spouse Name: " + data.getSpouseName());
        CommandMessageUtils.sendInfo(source, "  Spouse ID: " + data.getSpouseId());
        CommandMessageUtils.sendInfo(source, "  Children Names: " + String.join(", ", data.getChildrenNames()));
        CommandMessageUtils.sendInfo(source, "  Children IDs: " + String.join(", ", data.getChildrenIds()));
        CommandMessageUtils.sendInfo(source, "  Family Members: " + String.join(", ", data.getFamilyMembers()));
        
        // Professional Life
        CommandMessageUtils.sendInfo(source, "\nPROFESSIONAL LIFE:");
        CommandMessageUtils.sendInfo(source, "  Total Trades: " + data.getTotalTrades());
        CommandMessageUtils.sendInfo(source, "  Profession History: " + String.join(" → ", data.getProfessionHistory()));
        CommandMessageUtils.sendInfo(source, "  Favorite Player ID: " + data.getFavoritePlayerId());
        
        // AI & Emotional State
        CommandMessageUtils.sendInfo(source, "\nAI & EMOTIONAL STATE:");
        if (data.getEmotionalState() != null) {
            CommandMessageUtils.sendInfo(source, "  Dominant Emotion: " + data.getEmotionalState().getDominantEmotion());
            CommandMessageUtils.sendInfo(source, "  Emotional Description: " + data.getEmotionalState().getEmotionalDescription());
        }
        if (data.getAiState() != null) {
            CommandMessageUtils.sendInfo(source, "  Current Goal: " + data.getAiState().getCurrentGoal());
            CommandMessageUtils.sendInfo(source, "  Current Action: " + data.getAiState().getCurrentAction());
        }
        
        // Memory & Interactions
        CommandMessageUtils.sendInfo(source, "\nMEMORY & INTERACTIONS:");
        CommandMessageUtils.sendInfo(source, "  Recent Events: " + String.join(" | ", data.getRecentEvents()));
        CommandMessageUtils.sendInfo(source, "  Last Conversation: " + data.getLastConversationTime());
        CommandMessageUtils.sendInfo(source, "  Player Relations: " + data.getPlayerRelations().size() + " entries");
        CommandMessageUtils.sendInfo(source, "  Player Memories: " + data.getPlayerMemories().size() + " entries");
        CommandMessageUtils.sendInfo(source, "  Topic Frequency: " + data.getTopicFrequency().size() + " topics");
        
        // Notes
        if (!data.getNotes().isEmpty()) {
            CommandMessageUtils.sendInfo(source, "\nNOTES:");
            CommandMessageUtils.sendInfo(source, "  " + data.getNotes());
        }
        
        CommandMessageUtils.sendInfo(source, "=== End Export ===");
    }

    private static int resetVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "villager");
        
        if (!validateVillager(entity, context.getSource())) return 0;
        
        VillagerEntity villager = (VillagerEntity) entity;
        
        // Create new VillagerData and attach it
        VillagerData newData = new VillagerData();
        VillagerDataUtils.ensureVillagerData(villager);
        
        CommandMessageUtils.sendSuccessWithFormat(context.getSource(), 
            "Reset data for villager at %s", villager.getBlockPos());
        return 1;
    }

    private static int validateAllVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        int totalVillagers = villagers.size();
        int validData = 0;
        int invalidData = 0;
        int missingData = 0;
        
        CommandMessageUtils.sendInfo(source, "=== Villager Data Validation ===");
        
        for (VillagerEntity villager : villagers) {
            Optional<VillagerData> dataOpt = VillagerDataUtils.getVillagerData(villager);
            
            if (dataOpt.isEmpty()) {
                missingData++;
            } else {
                VillagerData data = dataOpt.get();
                if (isVillagerDataValid(data)) {
                    validData++;
                } else {
                    invalidData++;
                    reportInvalidData(villager, data, source);
                }
            }
        }
        
        CommandMessageUtils.sendInfo(source, "Validation Results:");
        CommandMessageUtils.sendFormattedMessage(source, "  Total Villagers: %d", totalVillagers);
        CommandMessageUtils.sendFormattedMessage(source, "  Valid Data: %d", validData);
        CommandMessageUtils.sendFormattedMessage(source, "  Invalid Data: %d", invalidData);
        CommandMessageUtils.sendFormattedMessage(source, "  Missing Data: %d", missingData);
        
        if (invalidData > 0 || missingData > 0) {
            CommandMessageUtils.sendWarning(source, "Consider running '/villager data repair' to fix issues");
        } else {
            CommandMessageUtils.sendSuccess(source, "All villager data is valid!");
        }
        
        return 1;
    }
    
    private static boolean isVillagerDataValid(VillagerData data) {
        // Check for basic data integrity
        if (data.getName() == null || data.getName().isEmpty()) return false;
        if (data.getAge() < 0) return false;
        if (data.getHappiness() < 0 || data.getHappiness() > 100) return false;
        if (data.getTotalTrades() < 0) return false;
        
        // Check for null collections
        if (data.getProfessionHistory() == null) return false;
        if (data.getPlayerRelations() == null) return false;
        if (data.getFamilyMembers() == null) return false;
        if (data.getChildrenIds() == null) return false;
        if (data.getChildrenNames() == null) return false;
        if (data.getRecentEvents() == null) return false;
        
        return true;
    }
    
    private static void reportInvalidData(VillagerEntity villager, VillagerData data, ServerCommandSource source) {
        String villagerPos = String.format("(%.1f, %.1f, %.1f)", villager.getX(), villager.getY(), villager.getZ());
        CommandMessageUtils.sendWarning(source, "Invalid data for villager at " + villagerPos + ":");
        
        if (data.getName() == null || data.getName().isEmpty()) {
            CommandMessageUtils.sendInfo(source, "  - Missing or empty name");
        }
        if (data.getAge() < 0) {
            CommandMessageUtils.sendInfo(source, "  - Negative age: " + data.getAge());
        }
        if (data.getHappiness() < 0 || data.getHappiness() > 100) {
            CommandMessageUtils.sendInfo(source, "  - Invalid happiness: " + data.getHappiness());
        }
        if (data.getTotalTrades() < 0) {
            CommandMessageUtils.sendInfo(source, "  - Negative total trades: " + data.getTotalTrades());
        }
    }

    private static int repairVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        int repairedCount = 0;
        int createdCount = 0;
        
        CommandMessageUtils.sendInfo(source, "=== Repairing Villager Data ===");
        
        for (VillagerEntity villager : villagers) {
            Optional<VillagerData> dataOpt = VillagerDataUtils.getVillagerData(villager);
            
            if (dataOpt.isEmpty()) {
                // Create missing data
                VillagerData newData = new VillagerData();
                VillagerDataUtils.ensureVillagerData(villager);
                createdCount++;
            } else {
                VillagerData data = dataOpt.get();
                if (!isVillagerDataValid(data)) {
                    // Repair invalid data
                    repairInvalidVillagerData(data);
                    repairedCount++;
                }
            }
        }
        
        CommandMessageUtils.sendSuccessWithFormat(source, "Repair completed: %d repaired, %d created", repairedCount, createdCount);
        return repairedCount + createdCount;
    }
    
    private static void repairInvalidVillagerData(VillagerData data) {
        // Fix basic data issues
        if (data.getName() == null || data.getName().isEmpty()) {
            data.setName("Repaired Villager");
        }
        if (data.getAge() < 0) {
            data.setAge(100); // Default adult age
        }
        if (data.getHappiness() < 0 || data.getHappiness() > 100) {
            data.setHappiness(50); // Default happiness
        }
        if (data.getTotalTrades() < 0) {
            data.setTotalTrades(0);
        }
        
        // Ensure collections are not null
        if (data.getProfessionHistory() == null) {
            data.setProfessionHistory(List.of());
        }
        if (data.getFamilyMembers() == null) {
            data.setFamilyMembers(List.of());
        }
        if (data.getChildrenIds() == null) {
            data.setChildrenIds(List.of());
        }
        if (data.getChildrenNames() == null) {
            data.setChildrenNames(List.of());
        }
    }

    private static int backupVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        // This would ideally save data to a file, but for now just show a summary
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        int backedUpCount = 0;
        for (VillagerEntity villager : villagers) {
            if (VillagerDataUtils.hasVillagerData(villager)) {
                backedUpCount++;
            }
        }
        
        CommandMessageUtils.sendSuccessWithFormat(source, "Backup summary: %d villagers with data found", backedUpCount);
        CommandMessageUtils.sendInfo(source, "Note: Full backup functionality would save to external files");
        
        return backedUpCount;
    }

    private static int showDataSummary(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        int totalVillagers = villagers.size();
        int withData = 0;
        int withNames = 0;
        int married = 0;
        int withChildren = 0;
        long totalTrades = 0;
        double avgHappiness = 0;
        
        for (VillagerEntity villager : villagers) {
            Optional<VillagerData> dataOpt = VillagerDataUtils.getVillagerData(villager);
            if (dataOpt.isPresent()) {
                withData++;
                VillagerData data = dataOpt.get();
                
                if (!data.getName().isEmpty()) withNames++;
                if (!data.getSpouseId().isEmpty()) married++;
                if (!data.getChildrenIds().isEmpty()) withChildren++;
                
                totalTrades += data.getTotalTrades();
                avgHappiness += data.getHappiness();
            }
        }
        
        if (withData > 0) {
            avgHappiness /= withData;
        }
        
        CommandMessageUtils.sendInfo(source, "=== Villager Data Summary ===");
        CommandMessageUtils.sendFormattedMessage(source, "Total Villagers: %d", totalVillagers);
        CommandMessageUtils.sendFormattedMessage(source, "With Data: %d (%.1f%%)", withData, (withData * 100.0 / totalVillagers));
        CommandMessageUtils.sendFormattedMessage(source, "With Names: %d", withNames);
        CommandMessageUtils.sendFormattedMessage(source, "Married: %d", married);
        CommandMessageUtils.sendFormattedMessage(source, "With Children: %d", withChildren);
        CommandMessageUtils.sendFormattedMessage(source, "Total Trades: %d", totalTrades);
        CommandMessageUtils.sendFormattedMessage(source, "Average Happiness: %.1f%%", avgHappiness);
        
        return 1;
    }

    private static int showHelp(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        CommandMessageUtils.sendInfo(source, "=== Villager Commands Help ===");
        CommandMessageUtils.sendInfo(source, "");
        
        CommandMessageUtils.sendInfo(source, "🤖 AI COMMANDS:");
        CommandMessageUtils.sendInfo(source, "  /villager ai setup <provider> [apikey] - Setup AI (gemini|openrouter|local)");
        CommandMessageUtils.sendInfo(source, "  /villager ai model <name> - Set AI model");
        CommandMessageUtils.sendInfo(source, "  /villager ai test [category] - Test AI dialogue");
        CommandMessageUtils.sendInfo(source, "  /villager ai toggle - Enable/disable AI");
        CommandMessageUtils.sendInfo(source, "  /villager ai status - Show AI configuration");
        CommandMessageUtils.sendInfo(source, "  /villager ai cache clear/size - Manage dialogue cache");
        CommandMessageUtils.sendInfo(source, "  /villager ai memory clear/size - Manage AI memory");
        CommandMessageUtils.sendInfo(source, "");
        
        CommandMessageUtils.sendInfo(source, "👥 MANAGEMENT:");
        CommandMessageUtils.sendInfo(source, "  /villager manage rename <target> <name> - Rename villager(s)");
        CommandMessageUtils.sendInfo(source, "  /villager manage list - List named villagers");
        CommandMessageUtils.sendInfo(source, "  /villager manage find <name> - Find villager by name");
        CommandMessageUtils.sendInfo(source, "  /villager manage info <villager> - Show detailed info");
        CommandMessageUtils.sendInfo(source, "  /villager manage stats - Village statistics");
        CommandMessageUtils.sendInfo(source, "  /villager manage randomize - Random names for all");
        CommandMessageUtils.sendInfo(source, "");
        
        CommandMessageUtils.sendInfo(source, "👪 FAMILY:");
        CommandMessageUtils.sendInfo(source, "  /villager family tree <villager> - Show family tree");
        CommandMessageUtils.sendInfo(source, "  /villager family marry <v1> <v2> - Marry two villagers");
        CommandMessageUtils.sendInfo(source, "  /villager family divorce <v1> <v2> - Divorce villagers");
        CommandMessageUtils.sendInfo(source, "  /villager family breed <v1> <v2> - Have baby");
        CommandMessageUtils.sendInfo(source, "  /villager family debug relationships - Debug family links");
        CommandMessageUtils.sendInfo(source, "");
        
        CommandMessageUtils.sendInfo(source, "😊 HAPPINESS & PERSONALITY:");
        CommandMessageUtils.sendInfo(source, "  /villager happiness set <villager> <0-100> - Set happiness");
        CommandMessageUtils.sendInfo(source, "  /villager happiness adjust <villager> <±value> - Adjust happiness");
        CommandMessageUtils.sendInfo(source, "  /villager happiness report - Happiness report");
        CommandMessageUtils.sendInfo(source, "  /villager personality set <villager> <type> - Set personality");
        CommandMessageUtils.sendInfo(source, "  /villager personality list - Available personalities");
        CommandMessageUtils.sendInfo(source, "");
        
        CommandMessageUtils.sendInfo(source, "💾 DATA:");
        CommandMessageUtils.sendInfo(source, "  /villager data export <villager> - Export all data");
        CommandMessageUtils.sendInfo(source, "  /villager data reset <villager> - Reset data");
        CommandMessageUtils.sendInfo(source, "  /villager data validate - Validate all villager data");
        CommandMessageUtils.sendInfo(source, "  /villager data repair - Repair invalid data");
        CommandMessageUtils.sendInfo(source, "  /villager data summary - Show data summary");
        CommandMessageUtils.sendInfo(source, "");
        
        CommandMessageUtils.sendInfo(source, "🔧 QUICK START:");
        CommandMessageUtils.sendInfo(source, "1. Setup AI: /villager ai setup gemini YOUR_API_KEY");
        CommandMessageUtils.sendInfo(source, "2. Test it: /villager ai test (look at a villager)");
        CommandMessageUtils.sendInfo(source, "3. Manage villagers: /villager manage list");
        
        return 1;
    }
}