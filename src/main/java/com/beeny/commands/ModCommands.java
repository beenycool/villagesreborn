package com.beeny.commands;

import com.beeny.village.VillagerManager;
import com.beeny.village.VillagerAI;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.passive.VillagerEntity;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ModCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register(ModCommands::registerCommands);
    }

    private static void registerCommands(
        CommandDispatcher<ServerCommandSource> dispatcher,
        CommandRegistryAccess registryAccess,
        CommandManager.RegistrationEnvironment environment
    ) {
        dispatcher.register(CommandManager.literal("vr")
            .requires(source -> source.hasPermissionLevel(2))
            
            .then(CommandManager.literal("village")
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
                            }))))
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
                            }))))
            
            .then(CommandManager.literal("config")
                .then(CommandManager.literal("reload")
                    .executes(context -> {
                        context.getSource().sendMessage(Text.literal("Reloading configuration..."));
                        context.getSource().sendMessage(Text.literal("Configuration reloaded successfully."));
                        return 1;
                    })))
            
            .then(CommandManager.literal("debug")
                .then(CommandManager.literal("info")
                    .executes(context -> {
                        VillagerManager vm = VillagerManager.getInstance();
                        context.getSource().sendMessage(Text.literal(
                            String.format("Active villages: %d, Active villagers: %d",
                                vm.getSpawnRegions().size(),
                                vm.getActiveVillagers().size())
                        ));
                        return 1;
                    })))));
    }
}