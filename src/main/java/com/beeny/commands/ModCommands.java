package com.beeny.commands;

import com.beeny.village.VillagerManager;
import com.beeny.village.VillagerAI;
import com.beeny.village.VillageInfluenceManager;
import com.beeny.village.VillageInfluenceManager.VillageDevelopmentData;
import com.beeny.village.VillageInfluenceManager.VillageRelationship;
import com.beeny.village.VillageInfluenceManager.RelationshipStatus;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.passive.VillagerEntity;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ModCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register(ModCommands::registerCommands);
    }

    private static void registerCommands(
        CommandDispatcher<ServerCommandSource> dispatcher,
        CommandRegistryAccess registryAccess,
        CommandManager.RegistrationEnvironment environment
    ) {
        dispatcher.register(
            CommandManager.literal("vr")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("village")
                    /* Village subcommands */
                    .then(CommandManager.literal("create")
                        .then(CommandManager.argument("culture", StringArgumentType.word())
                            .then(CommandManager.argument("radius", IntegerArgumentType.integer(16, 256))
                                .executes(context -> {
                                    String culture = StringArgumentType.getString(context, "culture");
                                    int radius = IntegerArgumentType.getInteger(context, "radius");
                                    BlockPos pos = context.getSource().getPlayer().getBlockPos();
                                    
                                    VillagerManager.getInstance().registerSpawnRegion(pos, radius, culture);
                                    
                                    context.getSource().sendMessage(Text.literal(
                                        String.format("Created new %s village at %s with radius %d",
                                            culture, pos.toShortString(), radius)
                                    ));
                                    
                                    return 1;
                                })
                            )
                        )
                    )
                    .then(CommandManager.literal("talk")
                        .then(CommandManager.argument("villagerName", StringArgumentType.word())
                            .then(CommandManager.argument("situation", StringArgumentType.string())
                                .executes(context -> {
                                    String villagerName = StringArgumentType.getString(context, "villagerName");
                                    String situation = StringArgumentType.getString(context, "situation");
                                     
                                    VillagerManager vm = VillagerManager.getInstance();
                                    VillagerAI villagerAI = vm.getActiveVillagers().stream()
                                        .filter(v -> v.getVillager().getCustomName() != null && 
                                                    v.getVillager().getCustomName().getString().equals(villagerName))
                                        .findFirst()
                                        .orElse(null);
                                         
                                    if (villagerAI == null) {
                                        context.getSource().sendMessage(Text.literal("Villager not found: " + villagerName));
                                        return 0;
                                    }
                                     
                                    VillagerEntity villager = villagerAI.getVillager();
                                    
                                    try {
                                        String behavior = villagerAI.generateBehavior(situation).get(5, TimeUnit.SECONDS);
                                        context.getSource().sendMessage(Text.literal(
                                            String.format("Villager %s: %s", villagerName, behavior)
                                        ));
                                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                                        context.getSource().sendMessage(Text.literal("Failed to generate behavior: " + e.getMessage()));
                                        return 0;
                                    }
                                    
                                    return 1;
                                })
                            )
                        )
                    )
                    .then(CommandManager.literal("info")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            BlockPos playerPos = player.getBlockPos();
                            
                            VillageInfluenceManager vim = VillageInfluenceManager.getInstance();
                            BlockPos villagePos = vim.findNearestVillage(playerPos);
                            
                            if (villagePos == null) {
                                context.getSource().sendMessage(Text.literal("§cNo villages found in the world.§r"));
                                return 0;
                            }
                            
                            double distance = Math.sqrt(playerPos.getSquaredDistance(villagePos));
                            VillageDevelopmentData village = vim.getVillageDevelopment(villagePos);
                            
                            context.getSource().sendMessage(Text.literal("§6======= Village Information =======§r"));
                            context.getSource().sendMessage(Text.literal(
                                String.format("§eLocation: §f%s §7(%.1f blocks away)§r", 
                                    villagePos.toShortString(), distance)
                            ));
                            context.getSource().sendMessage(Text.literal(
                                String.format("§eCulture: §f%s§r", village.getCulture())
                            ));
                            context.getSource().sendMessage(Text.literal(
                                String.format("§eLevel: §f%d §7(Points: %d)§r", 
                                    village.getVillageLevel(), village.getDevelopmentPoints())
                            ));
                            
                            // Building information
                            context.getSource().sendMessage(Text.literal("§6--- Buildings ---§r"));
                            for (Map.Entry<String, Integer> entry : village.getBuildingCounts().entrySet()) {
                                context.getSource().sendMessage(Text.literal(
                                    String.format("§f%s: §a%d§r", entry.getKey(), entry.getValue())
                                ));
                            }
                            
                            // Pending upgrades
                            List<String> pendingUpgrades = vim.getAvailableUpgrades(villagePos);
                            if (!pendingUpgrades.isEmpty()) {
                                context.getSource().sendMessage(Text.literal("§6--- Available Upgrades ---§r"));
                                for (String upgrade : pendingUpgrades) {
                                    context.getSource().sendMessage(Text.literal("§a- " + upgrade + "§r"));
                                }
                            }
                            
                            // Unlocked features
                            List<String> unlockedFeatures = village.getUnlockedFeatures();
                            if (!unlockedFeatures.isEmpty()) {
                                context.getSource().sendMessage(Text.literal("§6--- Unlocked Features ---§r"));
                                for (String feature : unlockedFeatures) {
                                    context.getSource().sendMessage(Text.literal("§a- " + feature + "§r"));
                                }
                            }
                            
                            return 1;
                        })
                    )
                    .then(CommandManager.literal("development")
                        .executes(context -> {
                            context.getSource().sendMessage(Text.literal("§6======= Village Development =======§r"));
                            Collection<VillageDevelopmentData> allVillages = VillageInfluenceManager.getInstance().getAllVillages();
                            
                            if (allVillages.isEmpty()) {
                                context.getSource().sendMessage(Text.literal("§cNo villages found in the world.§r"));
                                return 0;
                            }
                            
                            for (VillageDevelopmentData village : allVillages) {
                                BlockPos pos = village.getCenter();
                                context.getSource().sendMessage(Text.literal(
                                    String.format("§e%s Village §f(at %s)§r:", 
                                        village.getCulture(), pos.toShortString())
                                ));
                                
                                context.getSource().sendMessage(Text.literal(
                                    String.format("  §fLevel: §a%d§f, Points: §a%d§r", 
                                        village.getVillageLevel(), village.getDevelopmentPoints())
                                ));
                                
                                int buildingCount = village.getBuildingCounts().values().stream()
                                    .mapToInt(Integer::intValue).sum();
                                context.getSource().sendMessage(Text.literal(
                                    String.format("  §fBuildings: §a%d§f, Features: §a%d§r", 
                                        buildingCount, village.getUnlockedFeatures().size())
                                ));
                                
                                UUID founderUUID = village.getFounderUUID();
                                String founderInfo = (founderUUID != null) ? founderUUID.toString().substring(0, 8) : "None";
                                context.getSource().sendMessage(Text.literal(
                                    String.format("  §fFounder: §a%s§f, Contributors: §a%d§r", 
                                        founderInfo, village.getContributors().size())
                                ));
                            }
                            
                            return 1;
                        })
                    )
                    .then(CommandManager.literal("relationships")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            BlockPos playerPos = player.getBlockPos();
                            
                            VillageInfluenceManager vim = VillageInfluenceManager.getInstance();
                            BlockPos villagePos = vim.findNearestVillage(playerPos);
                            
                            if (villagePos == null) {
                                context.getSource().sendMessage(Text.literal("§cNo villages found in the world.§r"));
                                return 0;
                            }
                            
                            VillageDevelopmentData village = vim.getVillageDevelopment(villagePos);
                            context.getSource().sendMessage(Text.literal(
                                String.format("§6======= %s Village Relationships =======§r", 
                                    village.getCulture())
                            ));
                            context.getSource().sendMessage(Text.literal(
                                String.format("§eLocation: §f%s§r", villagePos.toShortString())
                            ));
                            
                            // Allies
                            List<BlockPos> allies = village.getAllies();
                            if (!allies.isEmpty()) {
                                context.getSource().sendMessage(Text.literal("§2--- Allies ---§r"));
                                for (BlockPos allyPos : allies) {
                                    VillageDevelopmentData allyVillage = vim.getVillageDevelopment(allyPos);
                                    if (allyVillage == null) continue;
                                    
                                    VillageRelationship relationship = vim.getVillageRelationship(villagePos, allyPos);
                                    String status = relationship != null ? relationship.getStatus().getColoredDisplayName() : "§7Unknown§r";
                                    int score = relationship != null ? relationship.getRelationshipScore() : 0;
                                    
                                    context.getSource().sendMessage(Text.literal(
                                        String.format("§f%s Village §7at %s§f: %s §7(Score: %d)§r", 
                                            allyVillage.getCulture(), allyPos.toShortString(), status, score)
                                    ));
                                    
                                    // Show most recent event
                                    if (relationship != null && !relationship.getRecentEvents().isEmpty()) {
                                        VillageInfluenceManager.RelationshipEvent event = relationship.getRecentEvents().get(0);
                                        Date date = new Date(event.timestamp());
                                        SimpleDateFormat sdf = new SimpleDateFormat("MMM d");
                                        context.getSource().sendMessage(Text.literal(
                                            String.format("  §7Last event: %s (%s%d§7) - %s§r", 
                                                sdf.format(date), 
                                                event.scoreChange() >= 0 ? "§a+" : "§c",
                                                event.scoreChange(),
                                                event.description())
                                        ));
                                    }
                                }
                            } else {
                                context.getSource().sendMessage(Text.literal("§7No allied villages.§r"));
                            }
                            
                            // Rivals
                            List<BlockPos> rivals = village.getRivals();
                            if (!rivals.isEmpty()) {
                                context.getSource().sendMessage(Text.literal("§c--- Rivals ---§r"));
                                for (BlockPos rivalPos : rivals) {
                                    VillageDevelopmentData rivalVillage = vim.getVillageDevelopment(rivalPos);
                                    if (rivalVillage == null) continue;
                                    
                                    VillageRelationship relationship = vim.getVillageRelationship(villagePos, rivalPos);
                                    String status = relationship != null ? relationship.getStatus().getColoredDisplayName() : "§7Unknown§r";
                                    int score = relationship != null ? relationship.getRelationshipScore() : 0;
                                    
                                    context.getSource().sendMessage(Text.literal(
                                        String.format("§f%s Village §7at %s§f: %s §7(Score: %d)§r", 
                                            rivalVillage.getCulture(), rivalPos.toShortString(), status, score)
                                    ));
                                    
                                    // Show most recent event
                                    if (relationship != null && !relationship.getRecentEvents().isEmpty()) {
                                        VillageInfluenceManager.RelationshipEvent event = relationship.getRecentEvents().get(0);
                                        Date date = new Date(event.timestamp());
                                        SimpleDateFormat sdf = new SimpleDateFormat("MMM d");
                                        context.getSource().sendMessage(Text.literal(
                                            String.format("  §7Last event: %s (%s%d§7) - %s§r", 
                                                sdf.format(date), 
                                                event.scoreChange() >= 0 ? "§a+" : "§c",
                                                event.scoreChange(),
                                                event.description())
                                        ));
                                    }
                                }
                            } else {
                                context.getSource().sendMessage(Text.literal("§7No rival villages.§r"));
                            }
                            
                            // Other villages
                            context.getSource().sendMessage(Text.literal("§e--- Other Villages ---§r"));
                            boolean hasOthers = false;
                            for (VillageDevelopmentData otherVillage : vim.getAllVillages()) {
                                BlockPos otherPos = otherVillage.getCenter();
                                if (otherPos.equals(villagePos) || allies.contains(otherPos) || rivals.contains(otherPos)) {
                                    continue; // Skip self, allies and rivals
                                }
                                
                                hasOthers = true;
                                VillageRelationship relationship = vim.getVillageRelationship(villagePos, otherPos);
                                String status = relationship != null ? relationship.getStatus().getColoredDisplayName() : "§7Unknown§r";
                                int score = relationship != null ? relationship.getRelationshipScore() : 0;
                                
                                context.getSource().sendMessage(Text.literal(
                                    String.format("§f%s Village §7at %s§f: %s §7(Score: %d)§r", 
                                        otherVillage.getCulture(), otherPos.toShortString(), status, score)
                                ));
                            }
                            
                            if (!hasOthers) {
                                context.getSource().sendMessage(Text.literal("§7No other villages.§r"));
                            }
                            
                            return 1;
                        })
                    )
                    .then(CommandManager.literal("contribute")
                        .then(CommandManager.argument("type", StringArgumentType.word())
                            .executes(context -> {
                                String type = StringArgumentType.getString(context, "type");
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                BlockPos playerPos = player.getBlockPos();
                                
                                VillageInfluenceManager vim = VillageInfluenceManager.getInstance();
                                BlockPos villagePos = vim.findNearestVillage(playerPos);
                                
                                if (villagePos == null) {
                                    context.getSource().sendMessage(Text.literal("§cNo villages found nearby.§r"));
                                    return 0;
                                }
                                
                                vim.startBuildingContribution(player, villagePos, type);
                                context.getSource().sendMessage(Text.literal(
                                    String.format("§aStarted contribution to build a %s.§r", type)
                                ));
                                context.getSource().sendMessage(Text.literal(
                                    "§eUse /vr village progress <amount> to make progress.§r"
                                ));
                                
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("progress")
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 100))
                            .executes(context -> {
                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                ServerPlayerEntity player = context.getSource().getPlayer();
                                
                                VillageInfluenceManager vim = VillageInfluenceManager.getInstance();
                                vim.recordContributionProgress(player, amount);
                                
                                context.getSource().sendMessage(Text.literal(
                                    String.format("§aAdded %d progress to your contribution.§r", amount)
                                ));
                                
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("cancel")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            VillageInfluenceManager vim = VillageInfluenceManager.getInstance();
                            vim.cancelContribution(player);
                            
                            context.getSource().sendMessage(Text.literal(
                                "§eContribution cancelled.§r"
                            ));
                            
                            return 1;
                        })
                    )
                    .then(CommandManager.literal("founder")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            VillageInfluenceManager vim = VillageInfluenceManager.getInstance();
                            
                            BlockPos villagePos = vim.findNearestVillage(player.getBlockPos());
                            if (villagePos == null) {
                                context.getSource().sendMessage(Text.literal("§cNo villages found nearby.§r"));
                                return 0;
                            }
                            
                            vim.registerVillageFounder(player, villagePos);
                            return 1;
                        })
                    )
                    .then(CommandManager.literal("diplomacy")
                        .then(CommandManager.argument("targetX", IntegerArgumentType.integer())
                            .then(CommandManager.argument("targetY", IntegerArgumentType.integer())
                                .then(CommandManager.argument("targetZ", IntegerArgumentType.integer())
                                    .executes(context -> {
                                        int x = IntegerArgumentType.getInteger(context, "targetX");
                                        int y = IntegerArgumentType.getInteger(context, "targetY");
                                        int z = IntegerArgumentType.getInteger(context, "targetZ");
                                        BlockPos targetPos = new BlockPos(x, y, z);
                                        
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        VillageInfluenceManager vim = VillageInfluenceManager.getInstance();
                                        
                                        BlockPos playerVillagePos = vim.findVillageOfPlayer(player.getUuid());
                                        if (playerVillagePos == null) {
                                            playerVillagePos = vim.findNearestVillage(player.getBlockPos());
                                        }
                                        
                                        if (playerVillagePos == null) {
                                            context.getSource().sendMessage(Text.literal("§cYou don't have a village to represent.§r"));
                                            return 0;
                                        }
                                        
                                        if (vim.getVillageDevelopment(targetPos) == null) {
                                            context.getSource().sendMessage(Text.literal("§cNo village at the target coordinates.§r"));
                                            return 0;
                                        }
                                        
                                        vim.startDiplomaticMission(player, playerVillagePos, targetPos);
                                        context.getSource().sendMessage(Text.literal(
                                            "§eUse /vr village progress <amount> to advance your diplomatic mission.§r"
                                        ));
                                        
                                        return 1;
                                    })
                                )
                            )
                        )
                    )
                    .then(CommandManager.literal("relationship")
                        .then(CommandManager.argument("targetX", IntegerArgumentType.integer())
                            .then(CommandManager.argument("targetY", IntegerArgumentType.integer())
                                .then(CommandManager.argument("targetZ", IntegerArgumentType.integer())
                                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(-100, 100))
                                        .then(CommandManager.argument("reason", StringArgumentType.greedyString())
                                            .executes(context -> {
                                                int x = IntegerArgumentType.getInteger(context, "targetX");
                                                int y = IntegerArgumentType.getInteger(context, "targetY");
                                                int z = IntegerArgumentType.getInteger(context, "targetZ");
                                                int amount = IntegerArgumentType.getInteger(context, "amount");
                                                String reason = StringArgumentType.getString(context, "reason");
                                                
                                                BlockPos targetPos = new BlockPos(x, y, z);
                                                ServerPlayerEntity player = context.getSource().getPlayer();
                                                VillageInfluenceManager vim = VillageInfluenceManager.getInstance();
                                                
                                                BlockPos playerVillagePos = vim.findNearestVillage(player.getBlockPos());
                                                if (playerVillagePos == null) {
                                                    context.getSource().sendMessage(Text.literal("§cNo village nearby.§r"));
                                                    return 0;
                                                }
                                                
                                                if (vim.getVillageDevelopment(targetPos) == null) {
                                                    context.getSource().sendMessage(Text.literal("§cNo village at the target coordinates.§r"));
                                                    return 0;
                                                }
                                                
                                                RelationshipStatus status = vim.adjustVillageRelationship(
                                                    playerVillagePos, targetPos, amount, reason
                                                );
                                                
                                                context.getSource().sendMessage(Text.literal(
                                                    String.format("§aRelationship adjusted by %s%d§a. New status: %s§r", 
                                                        amount >= 0 ? "§a+" : "§c", amount, status.getColoredDisplayName())
                                                ));
                                                
                                                return 1;
                                            })
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
                .then(CommandManager.literal("config")
                    .then(CommandManager.literal("reload")
                        .executes(context -> {
                            context.getSource().sendMessage(Text.literal("Reloading configuration..."));
                            context.getSource().sendMessage(Text.literal("Configuration reloaded successfully."));
                            return 1;
                        })
                    )
                )
                .then(CommandManager.literal("debug")
                    .then(CommandManager.literal("info")
                        .executes(context -> {
                            VillagerManager vm = VillagerManager.getInstance();
                            VillageInfluenceManager vim = VillageInfluenceManager.getInstance();
                            
                            context.getSource().sendMessage(Text.literal("§6======= Debug Information =======§r"));
                            context.getSource().sendMessage(Text.literal(
                                String.format("§eActive villages: §f%d§r", vm.getSpawnRegions().size())
                            ));
                            context.getSource().sendMessage(Text.literal(
                                String.format("§eActive villagers: §f%d§r", vm.getActiveVillagers().size())
                            ));
                            context.getSource().sendMessage(Text.literal(
                                String.format("§eDeveloped villages: §f%d§r", vim.getAllVillages().size())
                            ));
                            
                            return 1;
                        })
                    )
                )
        );
    }
}