package com.beeny.commands;

import com.beeny.commands.base.BaseVillagerCommand;
import com.beeny.data.VillagerData;
import com.beeny.network.VillagerDataPacket;
import com.beeny.system.ServerVillagerManager;
import com.beeny.util.CommandMessageUtils;
import com.beeny.util.VillagerDataUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Date;

/**
 * Commands for managing villager lists with filtering, sorting, and export capabilities
 */
public class VillagerListCommands extends BaseVillagerCommand {
    
    private static final int MAX_DISPLAY_VILLAGERS = 50;
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("villager")
            .then(CommandManager.literal("list")
                .executes(VillagerListCommands::listAllVillagers)
                .then(CommandManager.literal("filter")
                    .then(CommandManager.argument("criteria", StringArgumentType.string())
                        .suggests(FILTER_SUGGESTIONS)
                        .executes(VillagerListCommands::filterVillagers)))
                .then(CommandManager.literal("sort")
                    .then(CommandManager.argument("field", StringArgumentType.string())
                        .suggests(SORT_SUGGESTIONS)
                        .executes(VillagerListCommands::sortVillagers)))
                .then(CommandManager.literal("search")
                    .then(CommandManager.argument("query", StringArgumentType.string())
                        .executes(VillagerListCommands::searchVillagers)))
                .then(CommandManager.literal("export")
                    .executes(VillagerListCommands::exportVillagerList))
                .then(CommandManager.literal("count")
                    .executes(VillagerListCommands::countVillagers))
                .then(CommandManager.literal("nearby")
                    .then(CommandManager.argument("radius", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            builder.suggest("10");
                            builder.suggest("25");
                            builder.suggest("50");
                            builder.suggest("100");
                            return builder.buildFuture();
                        })
                        .executes(VillagerListCommands::listNearbyVillagers)))
                .then(CommandManager.literal("profession")
                    .then(CommandManager.argument("profession", StringArgumentType.string())
                        .suggests(PROFESSION_SUGGESTIONS)
                        .executes(VillagerListCommands::listByProfession)))
                .then(CommandManager.literal("family")
                    .then(CommandManager.literal("married")
                        .executes(VillagerListCommands::listMarriedVillagers))
                    .then(CommandManager.literal("children")
                        .executes(VillagerListCommands::listVillagersWithChildren))
                    .then(CommandManager.literal("single")
                        .executes(VillagerListCommands::listSingleVillagers)))
                .then(CommandManager.literal("stats")
                    .executes(VillagerListCommands::showListStats))));
    }
    
    private static final SuggestionProvider<ServerCommandSource> FILTER_SUGGESTIONS = (context, builder) -> {
        builder.suggest("married");
        builder.suggest("single");
        builder.suggest("with-children");
        builder.suggest("without-children");
        builder.suggest("high-happiness");
        builder.suggest("low-happiness");
        builder.suggest("many-trades");
        builder.suggest("few-trades");
        builder.suggest("named");
        builder.suggest("unnamed");
        return builder.buildFuture();
    };
    
    private static final SuggestionProvider<ServerCommandSource> SORT_SUGGESTIONS = (context, builder) -> {
        builder.suggest("name");
        builder.suggest("age");
        builder.suggest("happiness");
        builder.suggest("trades");
        builder.suggest("profession");
        builder.suggest("distance");
        builder.suggest("birth-time");
        return builder.buildFuture();
    };
    
    private static final SuggestionProvider<ServerCommandSource> PROFESSION_SUGGESTIONS = (context, builder) -> {
        builder.suggest("farmer");
        builder.suggest("librarian");
        builder.suggest("cleric");
        builder.suggest("armorer");
        builder.suggest("weaponsmith");
        builder.suggest("toolsmith");
        builder.suggest("butcher");
        builder.suggest("leatherworker");
        builder.suggest("cartographer");
        builder.suggest("fisherman");
        builder.suggest("shepherd");
        builder.suggest("fletcher");
        builder.suggest("mason");
        builder.suggest("nitwit");
        return builder.buildFuture();
    };
    
    private static int listAllVillagers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        if (villagers.isEmpty()) {
            CommandMessageUtils.sendWarning(source, "No villagers found in the area");
            return 0;
        }
        
        displayVillagerList(source, villagers, "All Villagers", Comparator.comparing(v -> {
            Optional<VillagerData> data = VillagerDataUtils.getVillagerData(v);
            return data.map(VillagerData::getName).orElse("Unnamed Villager");
        }));
        
        return 1;
    }
    
    private static int filterVillagers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String criteria = StringArgumentType.getString(context, "criteria");
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        List<VillagerEntity> filtered = villagers.stream()
            .filter(v -> matchesFilter(v, criteria))
            .collect(Collectors.toList());
        
        if (filtered.isEmpty()) {
            CommandMessageUtils.sendWarning(source, "No villagers match the filter: " + criteria);
            return 0;
        }
        
        displayVillagerList(source, filtered, "Filtered Villagers (" + criteria + ")", 
            getComparatorForField("name", source));
        
        return 1;
    }
    
    private static int sortVillagers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String field = StringArgumentType.getString(context, "field");
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        Comparator<VillagerEntity> comparator = getComparatorForField(field, source);
        
        displayVillagerList(source, villagers, "Villagers sorted by " + field, comparator);
        
        return 1;
    }
    
    private static int searchVillagers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String query = StringArgumentType.getString(context, "query").toLowerCase();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        List<VillagerEntity> matches = villagers.stream()
            .filter(v -> {
                Optional<VillagerData> dataOpt = VillagerDataUtils.getVillagerData(v);
                if (dataOpt.isEmpty()) return false;
                
                VillagerData data = dataOpt.get();
                return data.getName().toLowerCase().contains(query) ||
                       data.getProfessionData().getCurrentProfession().toLowerCase().contains(query) ||
                       data.getHobby().name().toLowerCase().contains(query) ||
                       data.getBirthPlace().toLowerCase().contains(query) ||
                       data.getFamilyMembers().stream().anyMatch(f -> f.toLowerCase().contains(query)) ||
                       data.getChildrenNames().stream().anyMatch(c -> c.toLowerCase().contains(query));
            })
            .collect(Collectors.toList());
        
        if (matches.isEmpty()) {
            CommandMessageUtils.sendWarning(source, "No villagers found matching: " + query);
            return 0;
        }
        
        displayVillagerList(source, matches, "Search Results for: " + query, 
            getComparatorForField("name", source));
        
        return 1;
    }
    
    private static int listNearbyVillagers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String radiusStr = StringArgumentType.getString(context, "radius");
        
        int radius;
        try {
            radius = Integer.parseInt(radiusStr);
            if (radius <= 0 || radius > 1000) {
                CommandMessageUtils.sendError(source, "Radius must be between 1 and 1000");
                return 0;
            }
        } catch (NumberFormatException e) {
            CommandMessageUtils.sendError(source, "Invalid radius: " + radiusStr);
            return 0;
        }
        
        List<VillagerEntity> villagers = getAllVillagersInArea(source, radius);
        
        if (villagers.isEmpty()) {
            CommandMessageUtils.sendWarning(source, "No villagers found within " + radius + " blocks");
            return 0;
        }
        
        displayVillagerList(source, villagers, "Villagers within " + radius + " blocks", 
            Comparator.comparingDouble(v -> v.squaredDistanceTo(source.getPosition())));
        
        return 1;
    }
    
    private static int listByProfession(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String profession = StringArgumentType.getString(context, "profession").toLowerCase();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        List<VillagerEntity> professionVillagers = villagers.stream()
            .filter(v -> {
                Optional<VillagerData> data = VillagerDataUtils.getVillagerData(v);
                return data.isPresent() && data.get().getProfessionData().getCurrentProfession().toLowerCase().contains(profession);
            })
            .collect(Collectors.toList());
        
        if (professionVillagers.isEmpty()) {
            CommandMessageUtils.sendWarning(source, "No villagers found with profession: " + profession);
            return 0;
        }
        
        displayVillagerList(source, professionVillagers, "Villagers with profession: " + profession, 
            getComparatorForField("name", source));
        
        return 1;
    }
    
    private static int listMarriedVillagers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        List<VillagerEntity> married = villagers.stream()
            .filter(v -> {
                Optional<VillagerData> data = VillagerDataUtils.getVillagerData(v);
                return data.isPresent() && !data.get().getSpouseId().isEmpty();
            })
            .collect(Collectors.toList());
        
        if (married.isEmpty()) {
            CommandMessageUtils.sendWarning(source, "No married villagers found");
            return 0;
        }
        
        displayVillagerList(source, married, "Married Villagers", 
            getComparatorForField("name", source));
        
        return 1;
    }
    
    private static int listVillagersWithChildren(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        List<VillagerEntity> withChildren = villagers.stream()
            .filter(v -> {
                Optional<VillagerData> data = VillagerDataUtils.getVillagerData(v);
                return data.isPresent() && !data.get().getChildrenIds().isEmpty();
            })
            .collect(Collectors.toList());
        
        if (withChildren.isEmpty()) {
            CommandMessageUtils.sendWarning(source, "No villagers with children found");
            return 0;
        }
        
        displayVillagerList(source, withChildren, "Villagers with Children", 
            getComparatorForField("name", source));
        
        return 1;
    }
    
    private static int listSingleVillagers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        List<VillagerEntity> single = villagers.stream()
            .filter(v -> {
                Optional<VillagerData> data = VillagerDataUtils.getVillagerData(v);
                return data.isPresent() && data.get().getSpouseId().isEmpty();
            })
            .collect(Collectors.toList());
        
        if (single.isEmpty()) {
            CommandMessageUtils.sendWarning(source, "No single villagers found");
            return 0;
        }
        
        displayVillagerList(source, single, "Single Villagers", 
            getComparatorForField("name", source));
        
        return 1;
    }
    
    private static int countVillagers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        int total = villagers.size();
        int withData = 0;
        int named = 0;
        int married = 0;
        int withChildren = 0;
        int highHappiness = 0;
        int lowHappiness = 0;
        
        Map<String, Integer> professionCounts = new HashMap<>();
        
        for (VillagerEntity villager : villagers) {
            Optional<VillagerData> dataOpt = VillagerDataUtils.getVillagerData(villager);
            if (dataOpt.isPresent()) {
                withData++;
                VillagerData data = dataOpt.get();
                
                if (!data.getName().isEmpty()) named++;
                if (!data.getSpouseId().isEmpty()) married++;
                if (!data.getChildrenIds().isEmpty()) withChildren++;
                if (data.getHappiness() >= 75) highHappiness++;
                if (data.getHappiness() <= 25) lowHappiness++;
                
                professionCounts.merge(data.getProfessionData().getCurrentProfession(), 1, Integer::sum);
            }
        }
        
        CommandMessageUtils.sendInfo(source, "=== Villager Count Summary ===");
        CommandMessageUtils.sendFormattedMessage(source, "Total Villagers: %d", total);
        CommandMessageUtils.sendFormattedMessage(source, "With Data: %d (%.1f%%)", withData, (withData * 100.0 / total));
        CommandMessageUtils.sendFormattedMessage(source, "Named: %d", named);
        CommandMessageUtils.sendFormattedMessage(source, "Married: %d", married);
        CommandMessageUtils.sendFormattedMessage(source, "With Children: %d", withChildren);
        CommandMessageUtils.sendFormattedMessage(source, "High Happiness (≥75): %d", highHappiness);
        CommandMessageUtils.sendFormattedMessage(source, "Low Happiness (≤25): %d", lowHappiness);
        
        if (!professionCounts.isEmpty()) {
            CommandMessageUtils.sendInfo(source, "\n📊 By Profession:");
            professionCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> 
                    CommandMessageUtils.sendFormattedMessage(source, "  %s: %d", entry.getKey(), entry.getValue()));
        }
        
        return 1;
    }
    
    private static int showListStats(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        int total = villagers.size();
        if (total == 0) {
            CommandMessageUtils.sendWarning(source, "No villagers found");
            return 0;
        }
        
        double avgAge = 0;
        double avgHappiness = 0;
        double avgTrades = 0;
        int maxAge = 0;
        int minAge = Integer.MAX_VALUE;
        int maxHappiness = 0;
        int minHappiness = 100;
        long maxTrades = 0;
        
        for (VillagerEntity villager : villagers) {
            Optional<VillagerData> dataOpt = VillagerDataUtils.getVillagerData(villager);
            if (dataOpt.isPresent()) {
                VillagerData data = dataOpt.get();
                
                avgAge += data.getAge();
                avgHappiness += data.getHappiness();
                avgTrades += data.getTotalTrades();
                
                maxAge = Math.max(maxAge, data.getAge());
                minAge = Math.min(minAge, data.getAge());
                maxHappiness = Math.max(maxHappiness, data.getHappiness());
                minHappiness = Math.min(minHappiness, data.getHappiness());
                maxTrades = Math.max(maxTrades, data.getTotalTrades());
            }
        }
        
        avgAge /= total;
        avgHappiness /= total;
        avgTrades /= total;
        
        CommandMessageUtils.sendInfo(source, "=== Villager Statistics ===");
        CommandMessageUtils.sendFormattedMessage(source, "Total Villagers: %d", total);
        CommandMessageUtils.sendFormattedMessage(source, "Average Age: %.1f (Range: %d-%d)", avgAge, minAge, maxAge);
        CommandMessageUtils.sendFormattedMessage(source, "Average Happiness: %.1f%% (Range: %d-%d%%)", avgHappiness, minHappiness, maxHappiness);
        CommandMessageUtils.sendFormattedMessage(source, "Average Trades: %.1f (Max: %d)", avgTrades, maxTrades);
        
        return 1;
    }
    
    private static int exportVillagerList(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        if (villagers.isEmpty()) {
            CommandMessageUtils.sendWarning(source, "No villagers to export");
            return 0;
        }
        
        CommandMessageUtils.sendInfo(source, "=== Villager List Export ===");
        CommandMessageUtils.sendInfo(source, "Generated: " + new Date());
        CommandMessageUtils.sendInfo(source, "Total Villagers: " + villagers.size());
        CommandMessageUtils.sendInfo(source, "");
        
        int index = 1;
        for (VillagerEntity villager : villagers) {
            Optional<VillagerData> dataOpt = VillagerDataUtils.getVillagerData(villager);
            if (dataOpt.isPresent()) {
                VillagerData data = dataOpt.get();
                String pos = String.format("(%.1f, %.1f, %.1f)", villager.getX(), villager.getY(), villager.getZ());
                
                CommandMessageUtils.sendFormattedMessage(source, "%d. %s [%s] - %s - Happiness: %d%% - Trades: %d - %s",
                    index++, data.getName(), data.getProfession(), data.getGender(), 
                    data.getHappiness(), data.getTotalTrades(), pos);
            }
        }
        
        CommandMessageUtils.sendSuccess(source, "Export completed! Check server logs for full details.");
        
        return 1;
    }
    
    private static void displayVillagerList(ServerCommandSource source, List<VillagerEntity> villagers, 
                                          String title, Comparator<VillagerEntity> comparator) {
        List<VillagerEntity> sortedVillagers = villagers.stream()
            .sorted(comparator)
            .limit(MAX_DISPLAY_VILLAGERS)
            .collect(Collectors.toList());
        
        CommandMessageUtils.sendInfo(source, "=== " + title + " ===");
        CommandMessageUtils.sendFormattedMessage(source, "Showing %d of %d villagers", 
            sortedVillagers.size(), villagers.size());
        CommandMessageUtils.sendInfo(source, "");
        
        int index = 1;
        for (VillagerEntity villager : sortedVillagers) {
            Optional<VillagerData> dataOpt = VillagerDataUtils.getVillagerData(villager);
            if (dataOpt.isPresent()) {
                VillagerData data = dataOpt.get();
                String pos = String.format("(%.1f, %.1f, %.1f)", villager.getX(), villager.getY(), villager.getZ());
                
                CommandMessageUtils.sendFormattedMessage(source, "%d. %s [%s] - %s - Happiness: %d%% - Trades: %d - %s",
                    index++, data.getName(), data.getProfession(), data.getGender(),
                    data.getHappiness(), data.getTotalTrades(), pos);
            } else {
                String pos = String.format("(%.1f, %.1f, %.1f)", villager.getX(), villager.getY(), villager.getZ());
                CommandMessageUtils.sendFormattedMessage(source, "%d. Unnamed Villager - %s", index++, pos);
            }
        }
        
        if (villagers.size() > MAX_DISPLAY_VILLAGERS) {
            CommandMessageUtils.sendInfo(source, "");
            CommandMessageUtils.sendInfo(source, "💡 Use /villager list export to see all villagers");
        }
    }
    
    private static boolean matchesFilter(VillagerEntity villager, String criteria) {
        Optional<VillagerData> dataOpt = VillagerDataUtils.getVillagerData(villager);
        if (dataOpt.isEmpty()) return false;
        
        VillagerData data = dataOpt.get();
        
        switch (criteria.toLowerCase()) {
            case "married":
                return !data.getSpouseId().isEmpty();
            case "single":
                return data.getSpouseId().isEmpty();
            case "with-children":
                return !data.getChildrenIds().isEmpty();
            case "without-children":
                return data.getChildrenIds().isEmpty();
            case "high-happiness":
                return data.getHappiness() >= 75;
            case "low-happiness":
                return data.getHappiness() <= 25;
            case "many-trades":
                return data.getTotalTrades() >= 50;
            case "few-trades":
                return data.getTotalTrades() <= 10;
            case "named":
                return !data.getName().isEmpty() && !data.getName().equals("Villager");
            case "unnamed":
                return data.getName().isEmpty() || data.getName().equals("Villager");
            default:
                return true;
        }
    }
    
    private static Comparator<VillagerEntity> getComparatorForField(String field, ServerCommandSource source) {
        switch (field.toLowerCase()) {
            case "name":
                return Comparator.comparing(v -> {
                    Optional<VillagerData> data = VillagerDataUtils.getVillagerData(v);
                    return data.map(VillagerData::getName).orElse("Unnamed Villager");
                });
            case "age":
                return Comparator.comparingInt(v -> {
                    Optional<VillagerData> data = VillagerDataUtils.getVillagerData(v);
                    return data.map(VillagerData::getAge).orElse(0);
                });
            case "happiness":
                return Comparator.<VillagerEntity>comparingInt(v -> {
                    Optional<VillagerData> data = VillagerDataUtils.getVillagerData(v);
                    return data.map(VillagerData::getHappiness).orElse(0);
                }).reversed();
            case "trades":
                return Comparator.<VillagerEntity>comparingInt(v -> {
                    Optional<VillagerData> data = VillagerDataUtils.getVillagerData(v);
                    return data.map(VillagerData::getTotalTrades).orElse(0);
                }).reversed();
            case "profession":
                return Comparator.comparing(v -> {
                    Optional<VillagerData> data = VillagerDataUtils.getVillagerData(v);
                    return data.map(VillagerData::getProfession).orElse("Unknown");
                });
            case "distance":
                return Comparator.comparingDouble(v -> {
                    if (source == null) return 0;
                    return v.squaredDistanceTo(source.getPosition());
                });
            case "birth-time":
                return Comparator.comparingLong(v -> {
                    Optional<VillagerData> data = VillagerDataUtils.getVillagerData(v);
                    return data.map(VillagerData::getBirthTime).orElse(0L);
                });
            default:
                return Comparator.comparing(v -> {
                    Optional<VillagerData> data = VillagerDataUtils.getVillagerData(v);
                    return data.map(VillagerData::getName).orElse("Unnamed Villager");
                });
        }
    }
}