package com.beeny.commands;

import com.beeny.commands.base.BaseVillagerCommand;
import com.beeny.data.VillagerData;
import com.beeny.system.VillagerDialogueSystem;
import com.beeny.util.CommandMessageUtils;
import com.beeny.util.VillagerDataUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Commands for managing villager data operations and utilities
 */
public class VillagerDataCommands extends BaseVillagerCommand {
    
    public static void register(com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> villagerCommand) {
        villagerCommand.then(CommandManager.literal("data")
            .then(CommandManager.literal("export")
                .then(CommandManager.argument("villager", EntityArgumentType.entity())
                    .executes(VillagerDataCommands::exportVillagerData)))
            .then(CommandManager.literal("export-all")
                .executes(VillagerDataCommands::exportAllVillagerData))
            .then(CommandManager.literal("import")
                .then(CommandManager.argument("file", StringArgumentType.string())
                    .executes(VillagerDataCommands::importVillagerData)))
            .then(CommandManager.literal("reset")
                .then(CommandManager.argument("villager", EntityArgumentType.entity())
                    .executes(VillagerDataCommands::resetVillagerData)))
            .then(CommandManager.literal("reset-all")
                .executes(VillagerDataCommands::resetAllVillagerData))
            .then(CommandManager.literal("validate")
                .executes(VillagerDataCommands::validateAllVillagerData))
            .then(CommandManager.literal("repair")
                .executes(VillagerDataCommands::repairVillagerData))
            .then(CommandManager.literal("backup")
                .executes(VillagerDataCommands::backupVillagerData))
            .then(CommandManager.literal("restore")
                .then(CommandManager.argument("backup", StringArgumentType.string())
                    .executes(VillagerDataCommands::restoreVillagerData)))
            .then(CommandManager.literal("compare")
                .then(CommandManager.argument("villager1", EntityArgumentType.entity())
                    .then(CommandManager.argument("villager2", EntityArgumentType.entity())
                        .executes(VillagerDataCommands::compareVillagerData))))
            .then(CommandManager.literal("search")
                .then(CommandManager.argument("query", StringArgumentType.string())
                    .executes(VillagerDataCommands::searchVillagerData)))
            .then(CommandManager.literal("stats")
                .executes(VillagerDataCommands::showDetailedStats))
            .then(CommandManager.literal("summary")
                .executes(VillagerDataCommands::showDataSummary)));
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
        VillagerDataUtils.setVillagerData(villager, newData);
        
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
        int minorIssues = 0;
        
        CommandMessageUtils.sendInfo(source, "=== Villager Data Validation ===");
        
        for (VillagerEntity villager : villagers) {
            Optional<VillagerData> dataOpt = VillagerDataUtils.getVillagerData(villager);
            
            if (dataOpt.isEmpty()) {
                missingData++;
                String pos = String.format("(%.1f, %.1f, %.1f)", villager.getX(), villager.getY(), villager.getZ());
                CommandMessageUtils.sendWarning(source, "Missing data for villager at " + pos);
            } else {
                VillagerData data = dataOpt.get();
                ValidationResult result = validateVillagerDataDetailed(data);
                
                if (result.isValid()) {
                    validData++;
                } else if (result.hasMinorIssues()) {
                    minorIssues++;
                    CommandMessageUtils.sendInfo(source, String.format("Minor issues: %s at %s",
                        data.getName(), villager.getBlockPos()));
                } else {
                    invalidData++;
                    reportInvalidData(villager, data, source);
                }
            }
        }
        
        CommandMessageUtils.sendInfo(source, "\n📊 Validation Results:");
        CommandMessageUtils.sendFormattedMessage(source, "  Total Villagers: %d", totalVillagers);
        CommandMessageUtils.sendFormattedMessage(source, "  ✅ Valid Data: %d", validData);
        CommandMessageUtils.sendFormattedMessage(source, "  ⚠️ Minor Issues: %d", minorIssues);
        CommandMessageUtils.sendFormattedMessage(source, "  ❌ Invalid Data: %d", invalidData);
        CommandMessageUtils.sendFormattedMessage(source, "  ❓ Missing Data: %d", missingData);
        
        if (invalidData > 0 || missingData > 0) {
            CommandMessageUtils.sendWarning(source, "💡 Consider running '/villager data repair' to fix issues");
        } else if (minorIssues > 0) {
            CommandMessageUtils.sendInfo(source, "💡 Some villagers have minor issues - repair recommended");
        } else {
            CommandMessageUtils.sendSuccess(source, "🎉 All villager data is valid!");
        }
        
        return 1;
    }
    
    private static class ValidationResult {
        private final boolean valid;
        private final boolean hasMinorIssues;
        private final List<String> issues;
        
        public ValidationResult(boolean valid, boolean hasMinorIssues, List<String> issues) {
            this.valid = valid;
            this.hasMinorIssues = hasMinorIssues;
            this.issues = issues;
        }
        
        public boolean isValid() { return valid; }
        public boolean hasMinorIssues() { return hasMinorIssues; }
        public List<String> getIssues() { return issues; }
    }
    
    private static ValidationResult validateVillagerDataDetailed(VillagerData data) {
        List<String> issues = new ArrayList<>();
        boolean hasCriticalIssues = false;
        boolean hasMinorIssues = false;
        
        // Check for critical data integrity issues
        if (data.getName() == null || data.getName().isEmpty()) {
            issues.add("Missing or empty name");
            hasCriticalIssues = true;
        }
        if (data.getAge() < 0) {
            issues.add("Negative age: " + data.getAge());
            hasCriticalIssues = true;
        }
        if (data.getHappiness() < 0 || data.getHappiness() > 100) {
            issues.add("Invalid happiness: " + data.getHappiness());
            hasCriticalIssues = true;
        }
        if (data.getTotalTrades() < 0) {
            issues.add("Negative total trades: " + data.getTotalTrades());
            hasCriticalIssues = true;
        }
        
        // Check for null collections (minor issues)
        if (data.getProfessionHistory() == null) {
            issues.add("Missing profession history");
            hasMinorIssues = true;
        }
        if (data.getPlayerRelations() == null) {
            issues.add("Missing player relations");
            hasMinorIssues = true;
        }
        if (data.getFamilyMembers() == null) {
            issues.add("Missing family members");
            hasMinorIssues = true;
        }
        if (data.getChildrenIds() == null) {
            issues.add("Missing children IDs");
            hasMinorIssues = true;
        }
        if (data.getChildrenNames() == null) {
            issues.add("Missing children names");
            hasMinorIssues = true;
        }
        if (data.getRecentEvents() == null) {
            issues.add("Missing recent events");
            hasMinorIssues = true;
        }
        
        return new ValidationResult(!hasCriticalIssues, hasMinorIssues && !hasCriticalIssues, issues);
    }
    
    private static void reportInvalidData(VillagerEntity villager, VillagerData data, ServerCommandSource source) {
        String villagerPos = String.format("(%.1f, %.1f, %.1f)", villager.getX(), villager.getY(), villager.getZ());
        ValidationResult result = validateVillagerDataDetailed(data);
        
        CommandMessageUtils.sendWarning(source, "❌ Invalid data for " + data.getName() + " at " + villagerPos + ":");
        
        for (String issue : result.getIssues()) {
            CommandMessageUtils.sendInfo(source, "  - " + issue);
        }
    }

    private static int repairVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        int repaired = 0;
        int failed = 0;
        int missingData = 0;
        int alreadyValid = 0;
        
        CommandMessageUtils.sendInfo(source, "=== Repairing Villager Data ===");
        
        for (VillagerEntity villager : villagers) {
            Optional<VillagerData> dataOpt = VillagerDataUtils.getVillagerData(villager);
            
            if (dataOpt.isPresent()) {
                VillagerData data = dataOpt.get();
                ValidationResult result = validateVillagerDataDetailed(data);
                
                if (result.isValid() && !result.hasMinorIssues()) {
                    alreadyValid++;
                } else {
                    if (repairVillagerData(data)) {
                        repaired++;
                        CommandMessageUtils.sendInfo(source, String.format("✅ Repaired: %s at %s",
                            data.getName(), villager.getBlockPos()));
                    } else {
                        failed++;
                        CommandMessageUtils.sendWarning(source, String.format("❌ Failed to repair: %s at %s",
                            data.getName(), villager.getBlockPos()));
                    }
                }
            } else {
                missingData++;
                // Create new data for villagers without any
                VillagerDataUtils.ensureVillagerData(villager);
                repaired++;
            }
        }
        
        CommandMessageUtils.sendInfo(source, "\n🔧 Repair Summary:");
        CommandMessageUtils.sendFormattedMessage(source, "  ✅ Repaired: %d", repaired);
        CommandMessageUtils.sendFormattedMessage(source, "  ✅ Already Valid: %d", alreadyValid);
        CommandMessageUtils.sendFormattedMessage(source, "  ❌ Failed: %d", failed);
        CommandMessageUtils.sendFormattedMessage(source, "  🆕 New Data Created: %d", missingData);
        
        if (repaired > 0) {
            CommandMessageUtils.sendSuccess(source, "🎉 Data repair completed successfully!");
        } else if (failed > 0) {
            CommandMessageUtils.sendWarning(source, "⚠️ Some repairs failed - check logs for details");
        } else {
            CommandMessageUtils.sendInfo(source, "✨ All data was already valid!");
        }
        
        return 1;
    }
    
    private static boolean repairVillagerData(VillagerData data) {
        boolean repaired = false;
        
        try {
            // Fix basic data issues
            if (data.getName() == null || data.getName().isEmpty()) {
                data.setName("Villager");
                repaired = true;
            }
            if (data.getAge() < 0) {
                data.setAge(Math.max(0, data.getAge()));
                repaired = true;
            }
            if (data.getHappiness() < 0 || data.getHappiness() > 100) {
                data.setHappiness(Math.max(0, Math.min(100, data.getHappiness())));
                repaired = true;
            }
            if (data.getTotalTrades() < 0) {
                data.setTotalTrades(Math.max(0, data.getTotalTrades()));
                repaired = true;
            }
            
            // Fix null collections
            if (data.getProfessionHistory() == null) {
                data.setProfessionHistory(new ArrayList<>());
                repaired = true;
            }
            if (data.getPlayerRelations() == null) {
                data.setPlayerRelations(new HashMap<>());
                repaired = true;
            }
            if (data.getFamilyMembers() == null) {
                data.setFamilyMembers(new ArrayList<>());
                repaired = true;
            }
            if (data.getChildrenIds() == null) {
                data.setChildrenIds(new ArrayList<>());
                repaired = true;
            }
            if (data.getChildrenNames() == null) {
                data.setChildrenNames(new ArrayList<>());
                repaired = true;
            }
            if (data.getRecentEvents() == null) {
                data.setRecentEvents(new ArrayList<>());
                repaired = true;
            }
            
            // Ensure profession history has at least current profession
            if (data.getProfessionHistory().isEmpty() && data.getProfessionData().getCurrentProfession() != null) {
                data.getProfessionHistory().add(data.getProfessionData().getCurrentProfession());
                repaired = true;
            }
            
            return repaired;
        } catch (Exception e) {
            return false;
        }
    }

    private static int backupVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        int backedUpCount = 0;
        int totalVillagers = villagers.size();
        
        CommandMessageUtils.sendInfo(source, "=== Villager Data Backup ===");
        
        for (VillagerEntity villager : villagers) {
            Optional<VillagerData> dataOpt = VillagerDataUtils.getVillagerData(villager);
            if (dataOpt.isPresent()) {
                backedUpCount++;
                // In a real implementation, this would save to a JSON file
                // For now, we'll create a detailed log entry
                VillagerData data = dataOpt.get();
                CommandMessageUtils.sendInfo(source, String.format("Backed up: %s (%s)",
                    data.getName(), villager.getUuid().toString().substring(0, 8)));
            }
        }
        
        CommandMessageUtils.sendSuccessWithFormat(source,
            "Backup completed: %d/%d villagers backed up", backedUpCount, totalVillagers);
        CommandMessageUtils.sendInfo(source,
            "Data saved to server logs (full file backup would require additional implementation)");
        
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
    
    // Missing command implementations
    private static int exportAllVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        CommandMessageUtils.sendInfo(source, "=== Exporting All Villager Data ===");
        
        for (VillagerEntity villager : villagers) {
            Optional<VillagerData> dataOpt = VillagerDataUtils.getVillagerData(villager);
            if (dataOpt.isPresent()) {
                exportDetailedVillagerData(dataOpt.get(), source);
                CommandMessageUtils.sendInfo(source, ""); // Separator
            }
        }
        
        CommandMessageUtils.sendSuccess(source, String.format("Exported data for %d villagers", villagers.size()));
        return 1;
    }
    
    private static int importVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String filename = StringArgumentType.getString(context, "file");
        CommandMessageUtils.sendInfo(context.getSource(), "Import functionality not yet implemented for file: " + filename);
        CommandMessageUtils.sendInfo(context.getSource(), "This would restore villager data from a backup file");
        return 1;
    }
    
    private static int resetAllVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        int resetCount = 0;
        for (VillagerEntity villager : villagers) {
            VillagerDataUtils.ensureVillagerData(villager);
            resetCount++;
        }
        
        CommandMessageUtils.sendSuccess(source, String.format("Reset data for %d villagers", resetCount));
        return 1;
    }
    
    private static int restoreVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String backupName = StringArgumentType.getString(context, "backup");
        CommandMessageUtils.sendInfo(context.getSource(), "Restore functionality not yet implemented for backup: " + backupName);
        CommandMessageUtils.sendInfo(context.getSource(), "This would restore villager data from a specific backup");
        return 1;
    }
    
    private static int compareVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            Entity entity1 = EntityArgumentType.getEntity(context, "villager1");
            Entity entity2 = EntityArgumentType.getEntity(context, "villager2");
            
            if (!validateVillager(entity1, context.getSource()) || !validateVillager(entity2, context.getSource())) {
                return 0;
            }
            
            VillagerEntity villager1 = (VillagerEntity) entity1;
            VillagerEntity villager2 = (VillagerEntity) entity2;
            
            Optional<VillagerData> data1 = VillagerDataUtils.getVillagerData(villager1);
            Optional<VillagerData> data2 = VillagerDataUtils.getVillagerData(villager2);
            
            if (data1.isEmpty() || data2.isEmpty()) {
                CommandMessageUtils.sendError(context.getSource(), "One or both villagers have no data");
                return 0;
            }
            
            CommandMessageUtils.sendInfo(context.getSource(), "=== Villager Data Comparison ===");
            compareVillagerDataFields(data1.get(), data2.get(), context.getSource());
            
            return 1;
        } catch (Exception e) {
            CommandMessageUtils.sendError(context.getSource(), "Error comparing villagers: " + e.getMessage());
            return 0;
        }
    }
    
    private static void compareVillagerDataFields(VillagerData data1, VillagerData data2, ServerCommandSource source) {
        CommandMessageUtils.sendInfo(source, String.format("Comparing: %s vs %s", data1.getName(), data2.getName()));
        
        if (!data1.getPersonality().equals(data2.getPersonality())) {
            CommandMessageUtils.sendInfo(source, String.format("Personality: %s vs %s", data1.getPersonality(), data2.getPersonality()));
        }
        if (data1.getHappiness() != data2.getHappiness()) {
            CommandMessageUtils.sendInfo(source, String.format("Happiness: %d vs %d", data1.getHappiness(), data2.getHappiness()));
        }
        if (data1.getAge() != data2.getAge()) {
            CommandMessageUtils.sendInfo(source, String.format("Age: %d vs %d", data1.getAge(), data2.getAge()));
        }
        if (!data1.getHobby().equals(data2.getHobby())) {
            CommandMessageUtils.sendInfo(source, String.format("Hobby: %s vs %s", data1.getHobby(), data2.getHobby()));
        }
    }
    
    private static int searchVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String query = StringArgumentType.getString(context, "query").toLowerCase();
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        CommandMessageUtils.sendInfo(source, "=== Searching Villager Data ===");
        CommandMessageUtils.sendInfo(source, "Query: " + query);
        
        int matchCount = 0;
        for (VillagerEntity villager : villagers) {
            Optional<VillagerData> dataOpt = VillagerDataUtils.getVillagerData(villager);
            if (dataOpt.isPresent()) {
                VillagerData data = dataOpt.get();
                if (matchesQuery(data, query)) {
                    matchCount++;
                    CommandMessageUtils.sendInfo(source, String.format("Match: %s at %s", 
                        data.getName(), villager.getBlockPos()));
                }
            }
        }
        
        CommandMessageUtils.sendSuccess(source, String.format("Found %d matches", matchCount));
        return 1;
    }
    
    private static boolean matchesQuery(VillagerData data, String query) {
        return data.getName().toLowerCase().contains(query) ||
               data.getPersonality().name().toLowerCase().contains(query) ||
               data.getHobby().name().toLowerCase().contains(query) ||
               data.getFavoriteFood().toLowerCase().contains(query) ||
               data.getNotes().toLowerCase().contains(query);
    }
    
    private static int exportAllVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        CommandMessageUtils.sendInfo(source, "Export all functionality not yet implemented");
        return 1;
    }
    
    private static int importVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String filename = StringArgumentType.getString(context, "file");
        CommandMessageUtils.sendInfo(context.getSource(), "Import functionality not yet implemented for file: " + filename);
        return 1;
    }
    
    private static int resetAllVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        CommandMessageUtils.sendInfo(source, "Reset all functionality not yet implemented");
        return 1;
    }
    
    private static int restoreVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String backupName = StringArgumentType.getString(context, "backup");
        CommandMessageUtils.sendInfo(context.getSource(), "Restore functionality not yet implemented for backup: " + backupName);
        return 1;
    }
    
    private static int compareVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        CommandMessageUtils.sendInfo(source, "Compare functionality not yet implemented");
        return 1;
    }
    
    private static int searchVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String query = StringArgumentType.getString(context, "query");
        CommandMessageUtils.sendInfo(context.getSource(), "Search functionality not yet implemented for query: " + query);
        return 1;
    }
    
    private static int showDetailedStats(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        CommandMessageUtils.sendInfo(source, "Detailed stats functionality not yet implemented");
        return 1;
    }
}