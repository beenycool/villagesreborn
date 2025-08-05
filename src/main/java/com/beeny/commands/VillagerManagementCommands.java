package com.beeny.commands;

import com.beeny.commands.base.BaseVillagerCommand;
import com.beeny.constants.VillagerConstants;
import com.beeny.data.VillagerData;
import com.beeny.system.VillagerScheduleManager;
import com.beeny.util.CommandMessageUtils;
import com.beeny.util.VillagerDataUtils;
import com.beeny.util.VillagerNames;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Commands for managing villager basic operations and information
 */
public class VillagerManagementCommands extends BaseVillagerCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("villager")
            .then(CommandManager.literal("manage")
                .then(CommandManager.literal("rename")
                    .then(CommandManager.argument("entities", EntityArgumentType.entities())
                        .then(CommandManager.argument("name", StringArgumentType.string())
                            .executes(VillagerManagementCommands::renameSelectedVillagers)))
                    .then(CommandManager.argument("name", StringArgumentType.string())
                        .executes(VillagerManagementCommands::renameNearestVillager)))
                .then(CommandManager.literal("list")
                    .executes(VillagerManagementCommands::listNamedVillagers))
                .then(CommandManager.literal("find")
                    .then(CommandManager.argument("name", StringArgumentType.string())
                        .executes(VillagerManagementCommands::findVillagerByName)))
                .then(CommandManager.literal("info")
                    .then(CommandManager.argument("villager", EntityArgumentType.entity())
                        .executes(VillagerManagementCommands::showVillagerInfo)))
                .then(CommandManager.literal("stats")
                    .executes(VillagerManagementCommands::showVillageStats))
                .then(CommandManager.literal("randomize")
                    .executes(VillagerManagementCommands::randomizeAllVillagerNames))
                .then(CommandManager.literal("schedule")
                    .then(CommandManager.argument("villager", EntityArgumentType.entity())
                        .executes(VillagerManagementCommands::showSchedule)))
                .then(CommandManager.literal("export")
                    .then(CommandManager.argument("villager", EntityArgumentType.entity())
                        .executes(VillagerManagementCommands::exportVillagerData)))
                .then(CommandManager.literal("reset")
                    .then(CommandManager.argument("villager", EntityArgumentType.entity())
                        .executes(VillagerManagementCommands::resetVillagerData))))
            .then(CommandManager.literal("personality")
                .then(CommandManager.literal("set")
                    .then(CommandManager.argument("villager", EntityArgumentType.entity())
                        .then(CommandManager.argument("personality", StringArgumentType.string())
                            .suggests(VillagerManagementCommands::suggestPersonalities)
                            .executes(VillagerManagementCommands::setPersonality))))
                .then(CommandManager.literal("list")
                    .executes(VillagerManagementCommands::listPersonalities)))
            .then(CommandManager.literal("happiness")
                .then(CommandManager.literal("set")
                    .then(CommandManager.argument("villager", EntityArgumentType.entity())
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0, 100))
                            .executes(VillagerManagementCommands::setHappiness))))
                .then(CommandManager.literal("adjust")
                    .then(CommandManager.argument("villager", EntityArgumentType.entity())
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(-100, 100))
                            .executes(VillagerManagementCommands::adjustHappiness))))
                .then(CommandManager.literal("report")
                    .executes(VillagerManagementCommands::happinessReport)))
        );
    }
    
    private static int renameSelectedVillagers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String newName = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();
        Collection<? extends Entity> entities = EntityArgumentType.getEntities(context, "entities");
        
        List<VillagerEntity> villagers = entities.stream()
            .filter(entity -> entity instanceof VillagerEntity)
            .map(entity -> (VillagerEntity) entity)
            .collect(Collectors.toList());
        
        if (villagers.isEmpty()) {
            CommandMessageUtils.sendError(source, "No villagers found in the selected entities");
            return 0;
        }
        
        int renamedCount = 0;
        for (VillagerEntity villager : villagers) {
            if (renameVillager(villager, newName, source)) {
                renamedCount++;
            }
        }
        
        CommandMessageUtils.sendSuccessWithFormat(source, "Successfully renamed %d villager%s to '%s'", 
            renamedCount, renamedCount == 1 ? "" : "s", newName);
        return renamedCount;
    }

    private static int renameNearestVillager(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String newName = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();
        
        VillagerEntity nearestVillager = findNearestVillager(source);
        if (nearestVillager == null) {
            CommandMessageUtils.sendErrorWithFormat(source, "No villager found within %.0f blocks", NEAREST_SEARCH_RADIUS);
            return 0;
        }

        if (renameVillager(nearestVillager, newName, source)) {
            return 1;
        }
        return 0;
    }
    
    private static boolean renameVillager(VillagerEntity villager, String newName, ServerCommandSource source) {
        Optional<VillagerData> dataOpt = getVillagerDataSafely(villager, source);
        if (dataOpt.isEmpty()) return false;
        
        VillagerData data = dataOpt.get();
        String oldName = data.getName();
        data.setName(newName);
        villager.setCustomName(Text.literal(newName));
        
        String feedback = !oldName.isEmpty() 
            ? String.format("Renamed villager from '%s' to '%s' at (%.1f, %.1f, %.1f)", 
                oldName, newName, villager.getX(), villager.getY(), villager.getZ())
            : String.format("Named villager '%s' at (%.1f, %.1f, %.1f)", 
                newName, villager.getX(), villager.getY(), villager.getZ());
        
        CommandMessageUtils.sendInfo(source, feedback);
        return true;
    }

    private static int listNamedVillagers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        List<VillagerEntity> namedVillagers = getNamedVillagersInArea(source, LIST_SEARCH_RADIUS);
        
        if (namedVillagers.isEmpty()) {
            CommandMessageUtils.sendFormattedMessage(source, "No named villagers found within %.0f blocks", LIST_SEARCH_RADIUS);
            return 0;
        }

        CommandMessageUtils.sendInfo(source, "Named villagers in area:");
        
        namedVillagers.stream()
            .sorted((v1, v2) -> {
                String name1 = VillagerDataUtils.getVillagerName(v1);
                String name2 = VillagerDataUtils.getVillagerName(v2);
                return name1.compareTo(name2);
            })
            .forEach(villager -> displayVillagerInList(villager, source));
        
        CommandMessageUtils.sendSuccessWithFormat(source, "Total: %d villagers", namedVillagers.size());
        return namedVillagers.size();
    }
    
    private static void displayVillagerInList(VillagerEntity villager, ServerCommandSource source) {
        VillagerData data = VillagerDataUtils.getVillagerDataOrNull(villager);
        if (data == null) return;
        
        String professionId = villager.getVillagerData().profession().toString();
        Vec3d pos = villager.getPos();
        double distance = source.getPosition().distanceTo(pos);
        
        Text coordsText = Text.literal(String.format("[%.1f, %.1f, %.1f]", pos.x, pos.y, pos.z))
            .formatted(Formatting.AQUA);

        Text message = Text.literal("- ")
            .append(Text.literal(data.getName()).formatted(Formatting.WHITE))
            .append(Text.literal(" (" + professionId + ")").formatted(Formatting.GRAY))
            .append(Text.literal(" at "))
            .append(coordsText)
            .append(Text.literal(String.format(" - %.1f blocks away", distance)).formatted(Formatting.GRAY));
        
        source.sendFeedback(() -> message, false);
    }

    private static int findVillagerByName(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String searchName = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();
        
        List<VillagerEntity> matchingVillagers = getNamedVillagersInArea(source, FIND_SEARCH_RADIUS)
            .stream()
            .filter(villager -> {
                String name = VillagerDataUtils.getVillagerName(villager);
                return name.toLowerCase().contains(searchName.toLowerCase());
            })
            .sorted(Comparator.comparingDouble(villager -> 
                source.getPosition().squaredDistanceTo(villager.getPos())))
            .collect(Collectors.toList());
        
        if (matchingVillagers.isEmpty()) {
            CommandMessageUtils.sendErrorWithFormat(source, "No villager found with name containing '%s' within %.0f blocks", 
                searchName, FIND_SEARCH_RADIUS);
            return 0;
        }
        
        VillagerEntity closestMatch = matchingVillagers.get(0);
        displayFoundVillager(closestMatch, source);
        
        if (matchingVillagers.size() > 1) {
            CommandMessageUtils.sendFormattedMessage(source, "Found %d more villager%s with matching names", 
                matchingVillagers.size() - 1, matchingVillagers.size() == 2 ? "" : "s");
        }
        
        return matchingVillagers.size();
    }
    
    private static void displayFoundVillager(VillagerEntity villager, ServerCommandSource source) {
        VillagerData data = VillagerDataUtils.getVillagerDataOrNull(villager);
        if (data == null) return;
        
        Vec3d pos = villager.getPos();
        double distance = Math.sqrt(source.getPosition().squaredDistanceTo(pos));
        String professionId = villager.getVillagerData().profession().toString();
        
        Text coordsText = Text.literal(String.format("[%.1f, %.1f, %.1f]", pos.x, pos.y, pos.z))
            .formatted(Formatting.AQUA);

        Text message = Text.literal("Found ")
            .append(Text.literal(data.getName()).formatted(Formatting.GREEN))
            .append(Text.literal(" (" + professionId + ")").formatted(Formatting.GRAY))
            .append(Text.literal(" at "))
            .append(coordsText)
            .append(Text.literal(String.format(" - %.1f blocks away", distance)).formatted(Formatting.GRAY));
        
        source.sendFeedback(() -> message, false);
    }

    private static int showVillagerInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "villager");
        if (!validateVillager(entity, context.getSource())) return 0;
        
        VillagerEntity villager = (VillagerEntity) entity;
        Optional<VillagerData> dataOpt = getVillagerDataSafely(villager, context.getSource());
        if (dataOpt.isEmpty()) return 0;
        
        VillagerData data = dataOpt.get();
        ServerCommandSource source = context.getSource();
        
        displayVillagerInfo(data, source);
        return 1;
    }
    
    private static void displayVillagerInfo(VillagerData data, ServerCommandSource source) {
        CommandMessageUtils.sendInfo(source, "=== " + data.getName() + " ===");
        CommandMessageUtils.sendInfo(source, "Gender: " + data.getGender() + " | Age: " + data.getAge());
        CommandMessageUtils.sendInfo(source, "Personality: " + VillagerConstants.PersonalityType.toString(data.getPersonality()) + 
            " | Happiness: " + data.getHappiness() + "% (" + data.getHappinessDescription() + ")");
        CommandMessageUtils.sendInfo(source, "Hobby: " + VillagerConstants.HobbyType.toString(data.getHobby()));
        
        if (!data.getFavoriteFood().isEmpty()) {
            CommandMessageUtils.sendInfo(source, "Favorite Food: " + data.getFavoriteFood());
        }
        
        CommandMessageUtils.sendInfo(source, "Birth Place: " + data.getBirthPlace());
        CommandMessageUtils.sendInfo(source, "Total Trades: " + data.getTotalTrades());
        
        if (!data.getProfessionHistory().isEmpty()) {
            CommandMessageUtils.sendInfo(source, "Profession History: " + String.join(" → ", data.getProfessionHistory()));
        }
        
        displayFamilyInfo(data, source);
        
        if (!data.getNotes().isEmpty()) {
            CommandMessageUtils.sendInfo(source, "Notes: " + data.getNotes());
        }
    }
    
    private static void displayFamilyInfo(VillagerData data, ServerCommandSource source) {
        if (!data.getSpouseId().isEmpty()) {
            String spouseName = data.getSpouseName();
            CommandMessageUtils.sendInfo(source, "Spouse: " + (spouseName != null ? spouseName : "Unknown"));
        }
        
        if (!data.getChildrenIds().isEmpty()) {
            List<String> childrenNames = data.getChildrenNames();
            if (!childrenNames.isEmpty()) {
                CommandMessageUtils.sendInfo(source, "Children: " + String.join(", ", childrenNames));
            }
        }
    }

    private static int showVillageStats(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        if (villagers.isEmpty()) {
            CommandMessageUtils.sendInfo(source, "No villagers found in the area");
            return 0;
        }
        
        VillageStats stats = calculateVillageStats(villagers);
        displayVillageStats(stats, source);
        
        return 1;
    }
    
    private static VillageStats calculateVillageStats(List<VillagerEntity> villagers) {
        VillageStats stats = new VillageStats();
        stats.totalVillagers = villagers.size();
        
        for (VillagerEntity villager : villagers) {
            VillagerData data = VillagerDataUtils.getVillagerDataOrNull(villager);
            if (data != null) {
                updateStatsWithVillager(stats, villager, data);
            }
        }
        
        return stats;
    }
    
    private static void updateStatsWithVillager(VillageStats stats, VillagerEntity villager, VillagerData data) {
        // Profession counts
        String profession = villager.getVillagerData().profession().toString();
        stats.professionCounts.put(profession, stats.professionCounts.getOrDefault(profession, 0) + 1);
        
        // Personality counts
        String personalityKey = VillagerConstants.PersonalityType.toString(data.getPersonality());
        stats.personalityCounts.put(personalityKey, stats.personalityCounts.getOrDefault(personalityKey, 0) + 1);
        
        // Hobby counts
        String hobbyKey = VillagerConstants.HobbyType.toString(data.getHobby());
        stats.hobbyCount.put(hobbyKey, stats.hobbyCount.getOrDefault(hobbyKey, 0) + 1);
        
        // Aggregate stats
        stats.totalHappiness += data.getHappiness();
        stats.totalAge += data.getAge();
        
        if (!data.getSpouseId().isEmpty()) stats.marriedCount++;
        if (data.getAge() < 20) stats.babyCount++;
        if (data.getAge() > 300) stats.elderCount++;
    }
    
    private static void displayVillageStats(VillageStats stats, ServerCommandSource source) {
        CommandMessageUtils.sendInfo(source, "=== Village Statistics ===");
        CommandMessageUtils.sendInfo(source, "Total Villagers: " + stats.totalVillagers);
        CommandMessageUtils.sendInfo(source, "Average Happiness: " + (stats.totalHappiness / stats.totalVillagers) + "%");
        CommandMessageUtils.sendInfo(source, "Average Age: " + (stats.totalAge / stats.totalVillagers) + " days");
        CommandMessageUtils.sendInfo(source, "Married: " + stats.marriedCount + " (" + (stats.marriedCount * 100 / stats.totalVillagers) + "%)");
        CommandMessageUtils.sendInfo(source, "Babies: " + stats.babyCount + " | Elders: " + stats.elderCount);
        
        displayTopCounts(source, "Professions", stats.professionCounts);
        displayTopCounts(source, "Personalities", stats.personalityCounts);
        displayTopCounts(source, "Popular Hobbies", stats.hobbyCount, 5);
    }
    
    private static void displayTopCounts(ServerCommandSource source, String title, Map<String, Integer> counts) {
        displayTopCounts(source, title, counts, Integer.MAX_VALUE);
    }
    
    private static void displayTopCounts(ServerCommandSource source, String title, Map<String, Integer> counts, int limit) {
        CommandMessageUtils.sendInfo(source, "\n" + title + ":");
        counts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(limit)
            .forEach(entry -> CommandMessageUtils.sendInfo(source, "  " + entry.getKey() + ": " + entry.getValue()));
    }

    private static int randomizeAllVillagerNames(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        World world = source.getWorld();
        
        List<VillagerEntity> villagers = getAllVillagersInArea(source, FIND_SEARCH_RADIUS);
        
        if (villagers.isEmpty()) {
            CommandMessageUtils.sendFormattedMessage(source, "No villagers found within %.0f blocks", FIND_SEARCH_RADIUS);
            return 0;
        }
        
        int renamedCount = 0;
        for (VillagerEntity villager : villagers) {
            String newName = VillagerNames.generateNameForProfession(world, villager.getBlockPos());
            
            Optional<VillagerData> dataOpt = VillagerDataUtils.getVillagerData(villager);
            if (dataOpt.isPresent()) {
                VillagerData data = dataOpt.get();
                data.setName(newName);
                villager.setCustomName(Text.literal(newName));
                renamedCount++;
            }
        }
        
        CommandMessageUtils.sendSuccessWithFormat(source, "Randomized names for %d villager%s", 
            renamedCount, renamedCount == 1 ? "" : "s");
        return renamedCount;
    }

    private static int setPersonality(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "villager");
        String personalityStr = StringArgumentType.getString(context, "personality");
        
        if (!validateVillager(entity, context.getSource())) return 0;
        
        VillagerEntity villager = (VillagerEntity) entity;
        VillagerConstants.PersonalityType personality = VillagerConstants.PersonalityType.fromString(personalityStr);

        if (!Arrays.stream(VillagerConstants.PersonalityType.values()).anyMatch(type -> type == personality)) {
            CommandMessageUtils.sendError(context.getSource(), "Invalid personality. Use /villager personality list to see valid options");
            return 0;
        }

        Optional<VillagerData> dataOpt = getVillagerDataSafely(villager, context.getSource());
        if (dataOpt.isEmpty()) return 0;
        
        VillagerData data = dataOpt.get();
        VillagerConstants.PersonalityType oldPersonality = data.getPersonality();
        data.setPersonality(personality);
        
        CommandMessageUtils.sendSuccessWithFormat(context.getSource(), "Changed %s's personality from %s to %s",
            data.getName(), VillagerConstants.PersonalityType.toString(oldPersonality), 
            VillagerConstants.PersonalityType.toString(personality));

        return 1;
    }

    private static int listPersonalities(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        CommandMessageUtils.sendInfo(source, "Available Personalities:");
        for (VillagerConstants.PersonalityType personality : VillagerConstants.PersonalityType.values()) {
            CommandMessageUtils.sendInfo(source, "  - " + VillagerConstants.PersonalityType.toString(personality));
        }
        return 1;
    }

    private static int setHappiness(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "villager");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        
        if (!validateVillager(entity, context.getSource())) return 0;
        
        VillagerEntity villager = (VillagerEntity) entity;
        Optional<VillagerData> dataOpt = getVillagerDataSafely(villager, context.getSource());
        if (dataOpt.isEmpty()) return 0;
        
        VillagerData data = dataOpt.get();
        data.setHappiness(amount);
        CommandMessageUtils.sendSuccessWithFormat(context.getSource(), "Set %s's happiness to %d%%", data.getName(), amount);
        
        return 1;
    }

    private static int adjustHappiness(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "villager");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        
        if (!validateVillager(entity, context.getSource())) return 0;
        
        VillagerEntity villager = (VillagerEntity) entity;
        Optional<VillagerData> dataOpt = getVillagerDataSafely(villager, context.getSource());
        if (dataOpt.isEmpty()) return 0;
        
        VillagerData data = dataOpt.get();
        data.adjustHappiness(amount);
        CommandMessageUtils.sendSuccessWithFormat(context.getSource(), "Adjusted %s's happiness by %s%d%% (now %d%%)", 
            data.getName(), amount >= 0 ? "+" : "", amount, data.getHappiness());
        
        return 1;
    }

    private static int happinessReport(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, 100);
        
        if (villagers.isEmpty()) {
            CommandMessageUtils.sendInfo(source, "No villagers found in the area");
            return 0;
        }
        
        List<VillagerEntity> sortedVillagers = villagers.stream()
            .filter(v -> VillagerDataUtils.hasVillagerData(v))
            .sorted((v1, v2) -> Integer.compare(
                VillagerDataUtils.getVillagerHappiness(v2), 
                VillagerDataUtils.getVillagerHappiness(v1)))
            .collect(Collectors.toList());
        
        CommandMessageUtils.sendInfo(source, "=== Happiness Report ===");
        
        CommandMessageUtils.sendInfo(source, "Happiest Villagers:");
        sortedVillagers.stream().limit(5).forEach(villager -> {
            VillagerData data = VillagerDataUtils.getVillagerDataOrNull(villager);
            if (data != null) {
                CommandMessageUtils.sendFormattedMessage(source, "  %s: %d%% (%s)", 
                    data.getName(), data.getHappiness(), data.getHappinessDescription());
            }
        });
        
        if (sortedVillagers.size() > 5) {
            CommandMessageUtils.sendInfo(source, "\nUnhappiest Villagers:");
            sortedVillagers.stream()
                .skip(Math.max(0, sortedVillagers.size() - 5))
                .forEach(villager -> {
                    VillagerData data = VillagerDataUtils.getVillagerDataOrNull(villager);
                    if (data != null) {
                        CommandMessageUtils.sendFormattedMessage(source, "  %s: %d%% (%s)", 
                            data.getName(), data.getHappiness(), data.getHappinessDescription());
                    }
                });
        }
        
        return 1;
    }

    private static int showSchedule(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "villager");
        
        if (!validateVillager(entity, context.getSource())) return 0;
        
        VillagerEntity villager = (VillagerEntity) entity;
        List<Text> scheduleInfo = VillagerScheduleManager.getScheduleInfo(villager);
        scheduleInfo.forEach(text -> context.getSource().sendFeedback(() -> text, false));
        
        return 1;
    }

    private static int exportVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "villager");
        
        if (!validateVillager(entity, context.getSource())) return 0;
        
        VillagerEntity villager = (VillagerEntity) entity;
        Optional<VillagerData> dataOpt = getVillagerDataSafely(villager, context.getSource());
        if (dataOpt.isEmpty()) return 0;
        
        VillagerData data = dataOpt.get();
        ServerCommandSource source = context.getSource();
        
        exportVillagerDataToConsole(data, source);
        return 1;
    }
    
    private static void exportVillagerDataToConsole(VillagerData data, ServerCommandSource source) {
        CommandMessageUtils.sendInfo(source, "=== Villager Data Export ===");
        CommandMessageUtils.sendInfo(source, "Name: " + data.getName());
        CommandMessageUtils.sendInfo(source, "Gender: " + data.getGender());
        CommandMessageUtils.sendInfo(source, "Age: " + data.getAge());
        CommandMessageUtils.sendInfo(source, "Personality: " + data.getPersonality());
        CommandMessageUtils.sendInfo(source, "Happiness: " + data.getHappiness());
        CommandMessageUtils.sendInfo(source, "Hobby: " + data.getHobby());
        CommandMessageUtils.sendInfo(source, "Favorite Food: " + data.getFavoriteFood());
        CommandMessageUtils.sendInfo(source, "Birth Time: " + data.getBirthTime());
        CommandMessageUtils.sendInfo(source, "Birth Place: " + data.getBirthPlace());
        CommandMessageUtils.sendInfo(source, "Total Trades: " + data.getTotalTrades());
        CommandMessageUtils.sendInfo(source, "Spouse: " + data.getSpouseName() + " (ID: " + data.getSpouseId() + ")");
        CommandMessageUtils.sendInfo(source, "Children: " + String.join(", ", data.getChildrenNames()));
        CommandMessageUtils.sendInfo(source, "Family: " + String.join(", ", data.getFamilyMembers()));
        CommandMessageUtils.sendInfo(source, "Profession History: " + String.join(", ", data.getProfessionHistory()));
        CommandMessageUtils.sendInfo(source, "Notes: " + data.getNotes());
        CommandMessageUtils.sendInfo(source, "=== End Export ===");
    }

    private static int resetVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "villager");
        
        if (!validateVillager(entity, context.getSource())) return 0;
        
        VillagerEntity villager = (VillagerEntity) entity;
        VillagerData newData = new VillagerData();
        VillagerDataUtils.ensureVillagerData(villager); // This will set new data if needed
        
        CommandMessageUtils.sendSuccessWithFormat(context.getSource(), "Reset data for villager at %s", villager.getBlockPos());
        return 1;
    }

    // Suggestion providers
    
    private static CompletableFuture<Suggestions> suggestPersonalities(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        for (VillagerConstants.PersonalityType personality : VillagerConstants.PersonalityType.values()) {
            builder.suggest(VillagerConstants.PersonalityType.toString(personality));
        }
        return builder.buildFuture();
    }
    
    // Helper classes
    
    private static class VillageStats {
        int totalVillagers = 0;
        int totalHappiness = 0;
        int totalAge = 0;
        int marriedCount = 0;
        int babyCount = 0;
        int elderCount = 0;
        Map<String, Integer> professionCounts = new HashMap<>();
        Map<String, Integer> personalityCounts = new HashMap<>();
        Map<String, Integer> hobbyCount = new HashMap<>();
    }
}