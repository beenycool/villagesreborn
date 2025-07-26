package com.beeny.commands;

import com.beeny.util.VillagerNames;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import com.mojang.serialization.Codec;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class VillagerCommands {
    private static final AttachmentType<String> VILLAGER_NAME = AttachmentRegistry.<String>builder()
            .persistent(Codec.STRING)
            .buildAndRegister(Identifier.of("villagersreborn", "villager_name"));

    // Configuration constants
    private static final double NEAREST_SEARCH_RADIUS = 10.0;
    private static final double LIST_SEARCH_RADIUS = 50.0;
    private static final double FIND_SEARCH_RADIUS = 100.0;
    private static final double RANDOMIZE_SEARCH_RADIUS = 50.0;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerVillagerCommand(dispatcher, registryAccess);
        });
    }

    // ========== Command Registration ==========

    private static void registerVillagerCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("villager")
            // Enhanced rename command with entity selector support
            .then(CommandManager.literal("rename")
                .then(CommandManager.argument("entities", EntityArgumentType.entities())
                    .then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(VillagerCommands::renameSelectedVillagers)))
                .then(CommandManager.literal("nearest")
                    .then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(VillagerCommands::renameNearestVillager))))
            
            // List command
            .then(CommandManager.literal("list")
                .executes(VillagerCommands::listNamedVillagers))
            
            // Find command
            .then(CommandManager.literal("find")
                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                    .executes(VillagerCommands::findVillagerByName)))
            
            // Randomize command
            .then(CommandManager.literal("randomize")
                .executes(VillagerCommands::randomizeAllVillagerNames)));
    }

    // ========== Command Handlers ==========

    private static int renameSelectedVillagers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String newName = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();
        Collection<? extends Entity> entities = EntityArgumentType.getEntities(context, "entities");
        
        List<VillagerEntity> villagers = entities.stream()
            .filter(entity -> entity instanceof VillagerEntity)
            .map(entity -> (VillagerEntity) entity)
            .collect(Collectors.toList());
        
        if (villagers.isEmpty()) {
            sendError(source, "No villagers found in the selected entities");
            return 0;
        }
        
        int renamedCount = 0;
        for (VillagerEntity villager : villagers) {
            String oldName = villager.getAttached(VILLAGER_NAME);
            setVillagerName(villager, newName);
            renamedCount++;
            
            String feedback = oldName != null 
                ? String.format("Renamed villager from '%s' to '%s' at (%.1f, %.1f, %.1f)", 
                    oldName, newName, villager.getX(), villager.getY(), villager.getZ())
                : String.format("Named villager '%s' at (%.1f, %.1f, %.1f)", 
                    newName, villager.getX(), villager.getY(), villager.getZ());
            
            sendInfo(source, feedback);
        }
        
        sendSuccess(source, String.format("Successfully renamed %d villager%s to '%s'", 
            renamedCount, renamedCount == 1 ? "" : "s", newName));
        return renamedCount;
    }

    private static int renameNearestVillager(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String newName = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();
        
        VillagerEntity nearestVillager = findNearestVillager(source);
        if (nearestVillager == null) {
            sendError(source, String.format("No villager found within %.0f blocks", NEAREST_SEARCH_RADIUS));
            return 0;
        }

        String oldName = nearestVillager.getAttached(VILLAGER_NAME);
        setVillagerName(nearestVillager, newName);

        String feedback = oldName != null 
            ? String.format("Renamed villager from '%s' to '%s'", oldName, newName)
            : String.format("Named villager '%s'", newName);
        
        sendSuccess(source, feedback);
        return 1;
    }

    private static int listNamedVillagers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        List<VillagerEntity> namedVillagers = getNamedVillagersInArea(source, LIST_SEARCH_RADIUS);
        
        if (namedVillagers.isEmpty()) {
            sendInfo(source, String.format("No named villagers found within %.0f blocks", LIST_SEARCH_RADIUS));
            return 0;
        }

        sendInfo(source, "Named villagers in area:");
        
        namedVillagers.stream()
            .sorted(Comparator.comparing(villager -> villager.getAttached(VILLAGER_NAME)))
            .forEach(villager -> {
                String name = villager.getAttached(VILLAGER_NAME);
                String professionId = villager.getVillagerData().profession().toString();
                Vec3d pos = villager.getPos();
                double distance = source.getPosition().distanceTo(pos);
                
                sendInfo(source, String.format("- %s (%s) at (%.1f, %.1f, %.1f) - %.1f blocks away", 
                    name, professionId, pos.x, pos.y, pos.z, distance));
            });
        
        sendSuccess(source, String.format("Total: %d villagers", namedVillagers.size()));
        return namedVillagers.size();
    }

    private static int findVillagerByName(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String searchName = StringArgumentType.getString(context, "name");
        ServerCommandSource source = context.getSource();
        
        List<VillagerEntity> matchingVillagers = getNamedVillagersInArea(source, FIND_SEARCH_RADIUS)
            .stream()
            .filter(villager -> {
                String villagerName = villager.getAttached(VILLAGER_NAME);
                return villagerName != null && villagerName.toLowerCase().contains(searchName.toLowerCase());
            })
            .sorted(Comparator.comparingDouble(villager -> 
                source.getPosition().squaredDistanceTo(villager.getPos())))
            .collect(Collectors.toList());
        
        if (matchingVillagers.isEmpty()) {
            sendError(source, String.format("No villager found with name containing '%s' within %.0f blocks", 
                searchName, FIND_SEARCH_RADIUS));
            return 0;
        }
        
        VillagerEntity closestMatch = matchingVillagers.get(0);
        String name = closestMatch.getAttached(VILLAGER_NAME);
        Vec3d pos = closestMatch.getPos();
        double distance = Math.sqrt(source.getPosition().squaredDistanceTo(pos));
        String professionId = closestMatch.getVillagerData().profession().toString();
        
        sendSuccess(source, String.format("Found %s (%s) at (%.1f, %.1f, %.1f) - %.1f blocks away", 
            name, professionId, pos.x, pos.y, pos.z, distance));
        
        if (matchingVillagers.size() > 1) {
            sendInfo(source, String.format("Found %d more villager%s with matching names", 
                matchingVillagers.size() - 1, matchingVillagers.size() == 2 ? "" : "s"));
        }
        
        return matchingVillagers.size();
    }

    private static int randomizeAllVillagerNames(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        World world = source.getWorld();
        
        List<VillagerEntity> villagers = getAllVillagersInArea(source, RANDOMIZE_SEARCH_RADIUS);
        
        if (villagers.isEmpty()) {
            sendInfo(source, String.format("No villagers found within %.0f blocks", RANDOMIZE_SEARCH_RADIUS));
            return 0;
        }
        
        int renamedCount = 0;
        for (VillagerEntity villager : villagers) {
            String newName = VillagerNames.generateNameForProfession(
                villager.getVillagerData().profession().getKey().orElse(net.minecraft.village.VillagerProfession.NITWIT),
                world,
                villager.getBlockPos()
            );
            
            setVillagerName(villager, newName);
            renamedCount++;
        }
        
        sendSuccess(source, String.format("Randomized names for %d villager%s", 
            renamedCount, renamedCount == 1 ? "" : "s"));
        return renamedCount;
    }

    // ========== Helper Methods ==========

    private static void setVillagerName(VillagerEntity villager, String name) {
        Objects.requireNonNull(villager, "Villager cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        
        villager.setAttached(VILLAGER_NAME, name);
        villager.setCustomName(Text.literal(name));
        villager.setCustomNameVisible(true);
    }

    private static VillagerEntity findNearestVillager(ServerCommandSource source) {
        Vec3d sourcePos = source.getPosition();
        World world = source.getWorld();
        
        return getAllVillagersInArea(source, NEAREST_SEARCH_RADIUS)
            .stream()
            .min(Comparator.comparingDouble(villager -> 
                sourcePos.squaredDistanceTo(villager.getPos())))
            .orElse(null);
    }

    private static List<VillagerEntity> getNamedVillagersInArea(ServerCommandSource source, double radius) {
        return getAllVillagersInArea(source, radius)
            .stream()
            .filter(villager -> villager.hasAttached(VILLAGER_NAME))
            .collect(Collectors.toList());
    }

    private static List<VillagerEntity> getAllVillagersInArea(ServerCommandSource source, double radius) {
        World world = source.getWorld();
        Vec3d sourcePos = source.getPosition();
        Box searchBox = Box.of(sourcePos, radius * 2, radius * 2, radius * 2);
        
        return world.getEntitiesByClass(VillagerEntity.class, searchBox, 
            villager -> sourcePos.distanceTo(villager.getPos()) <= radius);
    }

    // ========== Messaging Utilities ==========

    private static void sendSuccess(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(message).formatted(Formatting.GREEN), false);
    }

    private static void sendError(ServerCommandSource source, String message) {
        source.sendError(Text.literal(message).formatted(Formatting.RED));
    }

    private static void sendInfo(ServerCommandSource source, String message) {
        source.sendFeedback(() -> Text.literal(message).formatted(Formatting.YELLOW), false);
    }
}