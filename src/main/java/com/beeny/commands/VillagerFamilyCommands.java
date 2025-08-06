package com.beeny.commands;

import com.beeny.Villagersreborn;
import com.beeny.commands.base.BaseVillagerCommand;
import com.beeny.data.VillagerData;
import com.beeny.network.OpenFamilyTreePacket;
import com.beeny.system.VillagerRelationshipManager;
import com.beeny.util.CommandMessageUtils;
import com.beeny.util.VillagerDataUtils;
import com.beeny.util.VillagerNames;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Commands for managing villager family relationships and breeding
 */
public class VillagerFamilyCommands extends BaseVillagerCommand {
    
    public static void register(com.mojang.brigadier.builder.LiteralArgumentBuilder<ServerCommandSource> villagerCommand) {
        villagerCommand.then(CommandManager.literal("family")
            .then(CommandManager.literal("tree")
                .then(CommandManager.argument("villager", EntityArgumentType.entity())
                    .executes(VillagerFamilyCommands::showFamilyTree)))
            .then(CommandManager.literal("marry")
                .then(CommandManager.argument("villager1", EntityArgumentType.entity())
                    .then(CommandManager.argument("villager2", EntityArgumentType.entity())
                        .executes(VillagerFamilyCommands::marryVillagers))))
            .then(CommandManager.literal("divorce")
                .then(CommandManager.argument("villager1", EntityArgumentType.entity())
                    .then(CommandManager.argument("villager2", EntityArgumentType.entity())
                        .executes(VillagerFamilyCommands::divorceVillagers))))
            .then(CommandManager.literal("breed")
                .then(CommandManager.argument("villager1", EntityArgumentType.entity())
                    .then(CommandManager.argument("villager2", EntityArgumentType.entity())
                        .executes(VillagerFamilyCommands::breedVillagers))))
            .then(CommandManager.literal("debug")
                .then(CommandManager.literal("relationships")
                    .executes(VillagerFamilyCommands::debugRelationships))
                .then(CommandManager.literal("cleanup")
                    .executes(VillagerFamilyCommands::cleanupData))));
    }
    
    private static int showFamilyTree(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "villager");
        if (!validateVillager(entity, context.getSource())) return 0;
        
        VillagerEntity villager = (VillagerEntity) entity;
        
        // Check if the command source is a player
        if (context.getSource().getPlayer() == null) {
            CommandMessageUtils.sendError(context.getSource(), "Only players can view the family tree GUI");
            return 0;
        }
        
        // Send packet to open family tree GUI on client
        ServerPlayNetworking.send(context.getSource().getPlayer(), 
            new OpenFamilyTreePacket(villager.getId()));
        
        String villagerName = VillagerDataUtils.getVillagerName(villager);
        CommandMessageUtils.sendSuccessWithFormat(context.getSource(), "Opening family tree for %s", villagerName);
        
        return 1;
    }

    private static int marryVillagers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity1 = EntityArgumentType.getEntity(context, "villager1");
        Entity entity2 = EntityArgumentType.getEntity(context, "villager2");
        
        if (!validateVillager(entity1, context.getSource()) || !validateVillager(entity2, context.getSource())) {
            return 0;
        }
        
        VillagerEntity villager1 = (VillagerEntity) entity1;
        VillagerEntity villager2 = (VillagerEntity) entity2;
        
        if (VillagerRelationshipManager.attemptMarriage(villager1, villager2)) {
            CommandMessageUtils.sendSuccess(context.getSource(), "Marriage successful!");
            displayMarriageSuccess(villager1, villager2, context.getSource());
        } else {
            CommandMessageUtils.sendError(context.getSource(), "Marriage failed - conditions not met");
            explainMarriageFailure(villager1, villager2, context.getSource());
        }
        
        return 1;
    }
    
    private static void displayMarriageSuccess(VillagerEntity villager1, VillagerEntity villager2, ServerCommandSource source) {
        String name1 = VillagerDataUtils.getVillagerName(villager1);
        String name2 = VillagerDataUtils.getVillagerName(villager2);
        CommandMessageUtils.sendSuccessWithFormat(source, "%s and %s are now married! 💕", name1, name2);
    }
    
    private static void explainMarriageFailure(VillagerEntity villager1, VillagerEntity villager2, ServerCommandSource source) {
        if (!VillagerRelationshipManager.canMarry(villager1, villager2)) {
            Optional<VillagerData> data1Opt = VillagerDataUtils.getVillagerData(villager1);
            Optional<VillagerData> data2Opt = VillagerDataUtils.getVillagerData(villager2);
            
            if (data1Opt.isPresent() && data2Opt.isPresent()) {
                VillagerData data1 = data1Opt.get();
                VillagerData data2 = data2Opt.get();
                
                if (data1.getAge() < 100 || data2.getAge() < 100) {
                    CommandMessageUtils.sendInfo(source, "Reason: One or both villagers are too young");
                } else if (!data1.getSpouseId().isEmpty() || !data2.getSpouseId().isEmpty()) {
                    CommandMessageUtils.sendInfo(source, "Reason: One or both villagers are already married");
                } else if (data1.getHappiness() < 40 || data2.getHappiness() < 40) {
                    CommandMessageUtils.sendInfo(source, "Reason: One or both villagers are too unhappy");
                } else {
                    CommandMessageUtils.sendInfo(source, "Reason: Incompatible personalities or other factors");
                }
            }
        }
    }

    private static int divorceVillagers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity1 = EntityArgumentType.getEntity(context, "villager1");
        Entity entity2 = EntityArgumentType.getEntity(context, "villager2");
        
        if (!validateVillager(entity1, context.getSource()) || !validateVillager(entity2, context.getSource())) {
            return 0;
        }
        
        VillagerEntity villager1 = (VillagerEntity) entity1;
        VillagerEntity villager2 = (VillagerEntity) entity2;
        
        VillagerRelationshipManager.divorce(villager1, villager2);
        
        String name1 = VillagerDataUtils.getVillagerName(villager1);
        String name2 = VillagerDataUtils.getVillagerName(villager2);
        CommandMessageUtils.sendSuccessWithFormat(context.getSource(), "%s and %s have been divorced", name1, name2);
        
        return 1;
    }
    
    private static int breedVillagers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity1 = EntityArgumentType.getEntity(context, "villager1");
        Entity entity2 = EntityArgumentType.getEntity(context, "villager2");
        
        if (!validateVillager(entity1, context.getSource()) || !validateVillager(entity2, context.getSource())) {
            return 0;
        }
        
        VillagerEntity villager1 = (VillagerEntity) entity1;
        VillagerEntity villager2 = (VillagerEntity) entity2;
        
        // Validate marriage status
        if (!validateMarriageForBreeding(villager1, villager2, context.getSource())) {
            return 0;
        }
        
        // Spawn baby villager
        if (!validateServerWorld(villager1.getWorld(), context.getSource())) {
            return 0;
        }
        
        return createBabyVillager(villager1, villager2, context.getSource());
    }
    
    private static boolean validateMarriageForBreeding(VillagerEntity villager1, VillagerEntity villager2, ServerCommandSource source) {
        Optional<VillagerData> data1Opt = VillagerDataUtils.getVillagerData(villager1);
        Optional<VillagerData> data2Opt = VillagerDataUtils.getVillagerData(villager2);
        
        if (data1Opt.isEmpty() || data2Opt.isEmpty()) {
            CommandMessageUtils.sendError(source, "Villager data not found");
            return false;
        }
        
        VillagerData data1 = data1Opt.get();
        VillagerData data2 = data2Opt.get();
        
        // Check if they are married to each other
        if (!data1.getSpouseId().equals(villager2.getUuidAsString()) || 
            !data2.getSpouseId().equals(villager1.getUuidAsString())) {
            CommandMessageUtils.sendError(source, "Villagers must be married to each other to breed");
            return false;
        }
        
        return true;
    }
    
    private static int createBabyVillager(VillagerEntity parent1, VillagerEntity parent2, ServerCommandSource source) {
        ServerWorld serverWorld = (ServerWorld) parent1.getWorld();
        
        // Create baby villager
        VillagerEntity baby = new VillagerEntity(EntityType.VILLAGER, serverWorld);
        
        // Position baby between parents
        Vec3d pos1 = parent1.getPos();
        Vec3d pos2 = parent2.getPos();
        Vec3d babyPos = pos1.add(pos2).multiply(0.5);
        baby.setPosition(babyPos.x, babyPos.y, babyPos.z);
        
        // Make it a baby
        baby.setBaby(true);
        
        // Initialize baby with VillagerData
        VillagerData babyData = new VillagerData();
        baby.setAttached(Villagersreborn.VILLAGER_DATA, babyData);
        
        // Set baby name using VillagerNames utility
        String babyName = VillagerNames.generateName(baby);
        babyData.setName(babyName);
        baby.setCustomName(Text.literal(babyName));
        
        // Spawn the baby in world
        serverWorld.spawnEntity(baby);
        
        // Call the relationship manager to handle family setup
        VillagerRelationshipManager.onVillagerBreed(parent1, parent2, baby);
        
        // Visual and audio effects
        createBreedingEffects(serverWorld, babyPos);
        
        // Success message
        VillagerData parent1Data = VillagerDataUtils.getVillagerDataOrNull(parent1);
        VillagerData parent2Data = VillagerDataUtils.getVillagerDataOrNull(parent2);
        
        if (parent1Data != null && parent2Data != null) {
            CommandMessageUtils.sendSuccessWithFormat(source, "Baby villager born! Welcome %s!", babyName);
            notifyNearbyPlayers(serverWorld, babyPos, parent1Data.getName(), parent2Data.getName(), babyName);
        }
        
        return 1;
    }
    
    private static void createBreedingEffects(ServerWorld world, Vec3d position) {
        // Heart particles
        world.spawnParticles(ParticleTypes.HEART,
            position.x, position.y + 1, position.z,
            15, 0.5, 0.5, 0.5, 0.1);
        
        // Happy sound
        world.playSound(null, net.minecraft.util.math.BlockPos.ofFloored(position),
            SoundEvents.ENTITY_VILLAGER_YES, 
            SoundCategory.NEUTRAL, 1.0f, 1.5f);
    }
    
    private static void notifyNearbyPlayers(ServerWorld world, Vec3d position, String parent1Name, String parent2Name, String babyName) {
        world.getPlayers().stream()
            .filter(player -> player.getPos().distanceTo(position) < 50)
            .forEach(player -> {
                Text message = Text.literal("👶 " + parent1Name + " and " + parent2Name + " had a baby: " + babyName + "!")
                    .formatted(Formatting.GREEN);
                player.sendMessage(message, false);
            });
    }

    private static int debugRelationships(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, STATS_SEARCH_RADIUS);
        
        Map<String, List<String>> marriages = new HashMap<>();
        Map<String, List<String>> families = new HashMap<>();
        
        // Collect relationship data
        for (VillagerEntity villager : villagers) {
            Optional<VillagerData> dataOpt = VillagerDataUtils.getVillagerData(villager);
            if (dataOpt.isPresent()) {
                VillagerData data = dataOpt.get();
                
                if (!data.getSpouseName().isEmpty()) {
                    marriages.computeIfAbsent(data.getName(), k -> new ArrayList<>())
                        .add(data.getSpouseName());
                }
                
                if (!data.getFamilyMembers().isEmpty()) {
                    families.put(data.getName(), data.getFamilyMembers());
                }
            }
        }
        
        // Display relationship debug info
        CommandMessageUtils.sendInfo(source, "=== Relationship Debug ===");
        displayMarriages(marriages, source);
        displayFamilies(families, source);
        
        return 1;
    }
    
    private static void displayMarriages(Map<String, List<String>> marriages, ServerCommandSource source) {
        CommandMessageUtils.sendInfo(source, "Marriages:");
        if (marriages.isEmpty()) {
            CommandMessageUtils.sendInfo(source, "  No marriages found");
        } else {
            marriages.forEach((name, spouses) -> 
                CommandMessageUtils.sendFormattedMessage(source, "  %s ↔ %s", name, String.join(", ", spouses)));
        }
    }
    
    private static void displayFamilies(Map<String, List<String>> families, ServerCommandSource source) {
        CommandMessageUtils.sendInfo(source, "\nFamily Connections:");
        if (families.isEmpty()) {
            CommandMessageUtils.sendInfo(source, "  No family connections found");
        } else {
            families.forEach((name, members) -> 
                CommandMessageUtils.sendFormattedMessage(source, "  %s: %s", name, String.join(", ", members)));
        }
    }

    private static int cleanupData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        // Clean up stale proposal times and other relationship data
        int cleanedEntries = VillagerRelationshipManager.cleanupStaleProposalTimes();
        
        CommandMessageUtils.sendFormattedMessage(source, "Data cleanup completed. Removed %d stale proposal time entries.", cleanedEntries);
        
        return 1;
    }
}