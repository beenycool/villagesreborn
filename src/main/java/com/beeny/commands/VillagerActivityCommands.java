package com.beeny.commands;

import com.beeny.data.VillagerData;
import com.beeny.system.DailyActivityTracker;
import com.beeny.system.ServerVillagerManager;
import com.beeny.system.VillagerScheduleManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VillagerActivityCommands {
    private static final Logger LOGGER = LogManager.getLogger(VillagerActivityCommands.class);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("villager")
            .then(CommandManager.literal("activity")
                .then(CommandManager.literal("current")
                    .executes(VillagerActivityCommands::showCurrentActivity))
                .then(CommandManager.literal("daily")
                    .then(CommandManager.argument("day", IntegerArgumentType.integer(0))
                        .executes(VillagerActivityCommands::showDailyLog)))
                .then(CommandManager.literal("recent")
                    .then(CommandManager.argument("days", IntegerArgumentType.integer(1, 30))
                        .executes(VillagerActivityCommands::showRecentLogs)))
                .then(CommandManager.literal("weekly")
                    .executes(VillagerActivityCommands::showWeeklySummary))
                .then(CommandManager.literal("patterns")
                    .executes(VillagerActivityCommands::showActivityPatterns))
                .then(CommandManager.literal("list")
                    .executes(VillagerActivityCommands::listAllVillagersActivity))
                .then(CommandManager.literal("export")
                    .then(CommandManager.argument("villager", StringArgumentType.string())
                        .then(CommandManager.argument("days", IntegerArgumentType.integer(1, 30))
                            .executes(VillagerActivityCommands::exportActivityData)))))
        );
    }

    private static int showCurrentActivity(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player;
        try {
            player = context.getSource().getPlayerOrThrow();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            context.getSource().sendError(net.minecraft.text.Text.literal("This command can only be used by a player."));
            return 0;
        }
        VillagerEntity villager = getTargetedVillager(player);
        
        if (villager == null) {
            context.getSource().sendFeedback(() -> Text.literal("You must be looking at a villager!")
                .formatted(Formatting.RED), false);
            return 0;
        }

        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        if (data == null) {
            context.getSource().sendFeedback(() -> Text.literal("This villager has no data!")
                .formatted(Formatting.RED), false);
            return 0;
        }

        String villagerId = villager.getUuidAsString();
        DailyActivityTracker.ActivityEntry currentActivity = DailyActivityTracker.getCurrentActivity(villagerId);
        VillagerScheduleManager.Activity scheduleActivity = VillagerScheduleManager.getCurrentActivity(villager);

        context.getSource().sendFeedback(() -> {
            MutableText message = Text.literal("=== Current Activity for " + data.getName() + " ===\n")
                .formatted(Formatting.GOLD);
            
            if (currentActivity != null) {
                long duration = (villager.getWorld().getTimeOfDay() - currentActivity.getStartTime()) / 1200;
                message = message.append(Text.literal("Activity: " + currentActivity.getActivity() + "\n")
                    .formatted(Formatting.GREEN))
                    .append(Text.literal("Duration: " + duration + " minutes\n")
                        .formatted(Formatting.YELLOW))
                    .append(Text.literal("Location: " + currentActivity.getLocation() + "\n")
                        .formatted(Formatting.AQUA))
                    .append(Text.literal("Details: " + currentActivity.getDetails() + "\n")
                        .formatted(Formatting.GRAY));
            } else {
                message = message.append(Text.literal("No current activity tracked\n")
                    .formatted(Formatting.RED));
            }
            
            message = message.append(Text.literal("Scheduled Activity: " + scheduleActivity.description)
                .formatted(Formatting.LIGHT_PURPLE));
            
            return message;
        }, false);

        return 1;
    }

    private static int showDailyLog(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player;
        try {
            player = context.getSource().getPlayerOrThrow();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            context.getSource().sendError(net.minecraft.text.Text.literal("This command can only be used by a player."));
            return 0;
        }
        int day = IntegerArgumentType.getInteger(context, "day");
        VillagerEntity villager = getTargetedVillager(player);
        
        if (villager == null) {
            context.getSource().sendFeedback(() -> Text.literal("You must be looking at a villager!")
                .formatted(Formatting.RED), false);
            return 0;
        }

        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        if (data == null) {
            context.getSource().sendFeedback(() -> Text.literal("This villager has no data!")
                .formatted(Formatting.RED), false);
            return 0;
        }

        String villagerId = villager.getUuidAsString();
        DailyActivityTracker.DailyLog dailyLog = DailyActivityTracker.getDailyLog(villagerId, day);

        if (dailyLog == null) {
            context.getSource().sendFeedback(() -> Text.literal("No activity data for day " + day)
                .formatted(Formatting.RED), false);
            return 0;
        }

        context.getSource().sendFeedback(() -> {
            MutableText message = Text.literal("=== Daily Log for " + data.getName() + " (Day " + day + ") ===\n")
                .formatted(Formatting.GOLD);
            
            message = message.append(Text.literal("Activities (" + dailyLog.getActivities().size() + " total):\n")
                .formatted(Formatting.YELLOW));
            
            for (DailyActivityTracker.ActivityEntry activity : dailyLog.getActivities()) {
                long durationMinutes = activity.getDuration() / 1200;
                message = message.append(Text.literal("• " + activity.getActivity() + 
                    " (" + durationMinutes + "min) - " + activity.getDetails() + "\n")
                    .formatted(Formatting.GREEN));
            }
            
            message = message.append(Text.literal("\nSummary:\n").formatted(Formatting.AQUA));
            for (Map.Entry<String, Long> entry : dailyLog.getActivityDurations().entrySet()) {
                long minutes = entry.getValue() / 1200;
                message = message.append(Text.literal("• " + entry.getKey() + ": " + minutes + " minutes\n")
                    .formatted(Formatting.GRAY));
            }
            
            message = message.append(Text.literal("\nHappiness Change: " + 
                (dailyLog.getHappinessChange() >= 0 ? "+" : "") + dailyLog.getHappinessChange() + "\n")
                .formatted(dailyLog.getHappinessChange() >= 0 ? Formatting.GREEN : Formatting.RED));
            
            message = message.append(Text.literal("Social Interactions: " + dailyLog.getSocialInteractions() + "\n")
                .formatted(Formatting.LIGHT_PURPLE));
            
            if (!dailyLog.getNotableEvent().isEmpty()) {
                message = message.append(Text.literal("Notable Event: " + dailyLog.getNotableEvent())
                    .formatted(Formatting.GOLD));
            }
            
            return message;
        }, false);

        return 1;
    }

    private static int showRecentLogs(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player;
        try {
            player = context.getSource().getPlayerOrThrow();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            context.getSource().sendError(net.minecraft.text.Text.literal("This command can only be used by a player."));
            return 0;
        }
        int days = IntegerArgumentType.getInteger(context, "days");
        VillagerEntity villager = getTargetedVillager(player);
        
        if (villager == null) {
            context.getSource().sendFeedback(() -> Text.literal("You must be looking at a villager!")
                .formatted(Formatting.RED), false);
            return 0;
        }

        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        if (data == null) {
            context.getSource().sendFeedback(() -> Text.literal("This villager has no data!")
                .formatted(Formatting.RED), false);
            return 0;
        }

        String villagerId = villager.getUuidAsString();
        List<DailyActivityTracker.DailyLog> recentLogs = DailyActivityTracker.getRecentLogs(villagerId, days);

        if (recentLogs.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.literal("No activity data found for the last " + days + " days")
                .formatted(Formatting.RED), false);
            return 0;
        }

        context.getSource().sendFeedback(() -> {
            MutableText message = Text.literal("=== Recent Activity for " + data.getName() + " (Last " + days + " days) ===\n")
                .formatted(Formatting.GOLD);
            
            for (DailyActivityTracker.DailyLog log : recentLogs) {
                message = message.append(Text.literal("Day " + log.getDay() + ": ")
                    .formatted(Formatting.YELLOW));
                
                DailyActivityTracker.ActivityEntry mostFrequent = log.getMostFrequentActivity();
                if (mostFrequent != null) {
                    long duration = log.getTotalActivityTime(mostFrequent.getActivity()) / 1200;
                    message = message.append(Text.literal(mostFrequent.getActivity() + " (" + duration + "min)")
                        .formatted(Formatting.GREEN));
                }
                
                if (!log.getNotableEvent().isEmpty()) {
                    message = message.append(Text.literal(" - " + log.getNotableEvent())
                        .formatted(Formatting.AQUA));
                }
                
                message = message.append(Text.literal("\n"));
            }
            
            return message;
        }, false);

        return 1;
    }

    private static int showWeeklySummary(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player;
        try {
            player = context.getSource().getPlayerOrThrow();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            context.getSource().sendError(net.minecraft.text.Text.literal("This command can only be used by a player."));
            return 0;
        }
        VillagerEntity villager = getTargetedVillager(player);
        
        if (villager == null) {
            context.getSource().sendFeedback(() -> Text.literal("You must be looking at a villager!")
                .formatted(Formatting.RED), false);
            return 0;
        }

        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        if (data == null) {
            context.getSource().sendFeedback(() -> Text.literal("This villager has no data!")
                .formatted(Formatting.RED), false);
            return 0;
        }

        String villagerId = villager.getUuidAsString();
        Map<String, Long> weeklySummary = DailyActivityTracker.getWeeklyActivitySummary(villagerId);

        if (weeklySummary.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.literal("No weekly activity data available")
                .formatted(Formatting.RED), false);
            return 0;
        }

        context.getSource().sendFeedback(() -> {
            MutableText message = Text.literal("=== Weekly Activity Summary for " + data.getName() + " ===\n")
                .formatted(Formatting.GOLD);
            
            long totalTime = weeklySummary.values().stream().mapToLong(Long::longValue).sum();
            
            for (Map.Entry<String, Long> entry : weeklySummary.entrySet()) {
                long minutes = entry.getValue() / 1200;
                long hours = minutes / 60;
                minutes = minutes % 60;
                
                double percentage = totalTime > 0 ? (entry.getValue() * 100.0 / totalTime) : 0;
                
                message = message.append(Text.literal("• " + entry.getKey() + ": " + 
                    hours + "h " + minutes + "m (" + String.format("%.1f", percentage) + "%)\n")
                    .formatted(Formatting.GREEN));
            }
            
            return message;
        }, false);

        return 1;
    }

    private static int showActivityPatterns(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player;
        try {
            player = context.getSource().getPlayerOrThrow();
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            context.getSource().sendError(net.minecraft.text.Text.literal("This command can only be used by a player."));
            return 0;
        }
        VillagerEntity villager = getTargetedVillager(player);
        
        if (villager == null) {
            context.getSource().sendFeedback(() -> Text.literal("You must be looking at a villager!")
                .formatted(Formatting.RED), false);
            return 0;
        }

        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        if (data == null) {
            context.getSource().sendFeedback(() -> Text.literal("This villager has no data!")
                .formatted(Formatting.RED), false);
            return 0;
        }

        String villagerId = villager.getUuidAsString();
        List<String> patterns = DailyActivityTracker.getActivityPatterns(villagerId);

        if (patterns.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.literal("No pattern data available")
                .formatted(Formatting.RED), false);
            return 0;
        }

        context.getSource().sendFeedback(() -> {
            MutableText message = Text.literal("=== Activity Patterns for " + data.getName() + " ===\n")
                .formatted(Formatting.GOLD);
            
            for (String pattern : patterns) {
                message = message.append(Text.literal("• " + pattern + "\n")
                    .formatted(Formatting.GREEN));
            }
            
            return message;
        }, false);

        return 1;
    }

    private static int listAllVillagersActivity(CommandContext<ServerCommandSource> context) {
        context.getSource().sendFeedback(() -> {
            MutableText message = Text.literal("=== All Villagers Current Activity ===\n")
                .formatted(Formatting.GOLD);
            
            for (VillagerEntity villager : ServerVillagerManager.getInstance().getAllTrackedVillagers()) {
                VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
                if (data == null) continue;
                
                String villagerId = villager.getUuidAsString();
                DailyActivityTracker.ActivityEntry currentActivity = DailyActivityTracker.getCurrentActivity(villagerId);
                
                message = message.append(Text.literal("• " + data.getName() + ": ")
                    .formatted(Formatting.YELLOW));
                
                if (currentActivity != null) {
                    long duration = (villager.getWorld().getTimeOfDay() - currentActivity.getStartTime()) / 1200;
                    message = message.append(Text.literal(currentActivity.getActivity() + " (" + duration + "min)")
                        .formatted(Formatting.GREEN));
                } else {
                    message = message.append(Text.literal("No activity tracked")
                        .formatted(Formatting.GRAY));
                }
                
                message = message.append(Text.literal("\n"));
            }
            
            return message;
        }, false);

        return 1;
    }

    private static int exportActivityData(CommandContext<ServerCommandSource> context) {
        String villagerName = StringArgumentType.getString(context, "villager");
        int days = IntegerArgumentType.getInteger(context, "days");

        VillagerEntity villager = StreamSupport.stream(ServerVillagerManager.getInstance().getAllTrackedVillagers().spliterator(), false)
            .filter(v -> {
                VillagerData data = v.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
                return data != null && data.getName().equalsIgnoreCase(villagerName);
            })
            .findFirst()
            .orElse(null);

        if (villager == null) {
            context.getSource().sendFeedback(() -> Text.literal("Villager '" + villagerName + "' not found!")
                .formatted(Formatting.RED), false);
            return 0;
        }

        VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
        String villagerId = villager.getUuidAsString();
        List<DailyActivityTracker.DailyLog> logs = DailyActivityTracker.getRecentLogs(villagerId, days);

        if (logs.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.literal("No activity data to export")
                .formatted(Formatting.RED), false);
            return 0;
        }

        StringBuilder export = new StringBuilder();
        export.append("Activity Export for ").append(data.getName()).append(" (").append(days).append(" days)\n");
        export.append("=".repeat(50)).append("\n\n");

        for (DailyActivityTracker.DailyLog log : logs) {
            export.append("Day ").append(log.getDay()).append(":\n");
            export.append("Activities: ").append(log.getActivities().size()).append("\n");
            export.append("Social Interactions: ").append(log.getSocialInteractions()).append("\n");
            export.append("Happiness Change: ").append(log.getHappinessChange()).append("\n");
            
            if (!log.getNotableEvent().isEmpty()) {
                export.append("Notable Event: ").append(log.getNotableEvent()).append("\n");
            }
            
            export.append("Activity Breakdown:\n");
            for (Map.Entry<String, Long> entry : log.getActivityDurations().entrySet()) {
                long minutes = entry.getValue() / 1200;
                export.append("  ").append(entry.getKey()).append(": ").append(minutes).append(" minutes\n");
            }
            export.append("\n");
        }

        context.getSource().sendFeedback(() -> Text.literal("Activity data exported to server log")
            .formatted(Formatting.GREEN), false);

        LOGGER.info("[INFO] [VillagerActivityCommands] [exportActivityData] - {}", export);

        return 1;
    }

    private static VillagerEntity getTargetedVillager(ServerPlayerEntity player) {
        HitResult hitResult = player.raycast(5.0, 1.0f, false);
        if (hitResult.getType() == HitResult.Type.ENTITY && hitResult instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof VillagerEntity villager) {
                return villager;
            }
        }
        return null;
    }
}