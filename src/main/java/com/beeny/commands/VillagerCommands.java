package com.beeny.commands;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import com.beeny.network.OpenFamilyTreePacket;
import com.beeny.system.VillagerRelationshipManager;
import com.beeny.system.VillagerScheduleManager;
import com.beeny.system.ServerVillagerManager;
import com.beeny.util.VillagerNames;
import com.beeny.commands.DialogueCommands;
import com.beeny.commands.DialogueSetupCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.UUID;

public class VillagerCommands {
    
    private static final double NEAREST_SEARCH_RADIUS = 10.0;
    private static final double LIST_SEARCH_RADIUS = 50.0;
    private static final double FIND_SEARCH_RADIUS = 100.0;
    private static final double RANDOMIZE_SEARCH_RADIUS = 50.0;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerVillagerCommand(dispatcher, registryAccess);
            DialogueCommands.register(dispatcher, registryAccess);
            DialogueSetupCommands.register(dispatcher, registryAccess);
        });
    }

    private static void registerVillagerCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("villager")
            
            .then(CommandManager.literal("rename")
                .then(CommandManager.argument("entities", EntityArgumentType.entities())
                    .then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(VillagerCommands::renameSelectedVillagers)))
                .then(CommandManager.literal("nearest")
                    .then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(VillagerCommands::renameNearestVillager))))
            
            .then(CommandManager.literal("list")
                .executes(VillagerCommands::listNamedVillagers))
            
            .then(CommandManager.literal("find")
                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                    .executes(VillagerCommands::findVillagerByName)))
            
            .then(CommandManager.literal("randomize")
                .executes(VillagerCommands::randomizeAllVillagerNames))
            
            
            .then(CommandManager.literal("info")
                .then(CommandManager.argument("villager", EntityArgumentType.entity())
                    .executes(VillagerCommands::showVillagerInfo)))
            
            .then(CommandManager.literal("stats")
                .executes(VillagerCommands::showVillageStats))
            
            
            .then(CommandManager.literal("family")
                .then(CommandManager.literal("tree")
                    .then(CommandManager.argument("villager", EntityArgumentType.entity())
                        .executes(VillagerCommands::showFamilyTree)))
                .then(CommandManager.literal("marry")
                    .then(CommandManager.argument("villager1", EntityArgumentType.entity())
                        .then(CommandManager.argument("villager2", EntityArgumentType.entity())
                            .executes(VillagerCommands::marryVillagers))))
                .then(CommandManager.literal("divorce")
                    .then(CommandManager.argument("villager1", EntityArgumentType.entity())
                        .then(CommandManager.argument("villager2", EntityArgumentType.entity())
                            .executes(VillagerCommands::divorceVillagers))))
                .then(CommandManager.literal("breed")
                    .then(CommandManager.argument("villager1", EntityArgumentType.entity())
                        .then(CommandManager.argument("villager2", EntityArgumentType.entity())
                            .executes(VillagerCommands::breedVillagers)))))
            
            
            .then(CommandManager.literal("happiness")
                .then(CommandManager.literal("set")
                    .then(CommandManager.argument("villager", EntityArgumentType.entity())
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(0, 100))
                            .executes(VillagerCommands::setHappiness))))
                .then(CommandManager.literal("adjust")
                    .then(CommandManager.argument("villager", EntityArgumentType.entity())
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(-100, 100))
                            .executes(VillagerCommands::adjustHappiness))))
                .then(CommandManager.literal("report")
                    .executes(VillagerCommands::happinessReport)))
            
            
            .then(CommandManager.literal("personality")
                .then(CommandManager.literal("set")
                    .then(CommandManager.argument("villager", EntityArgumentType.entity())
                        .then(CommandManager.argument("personality", StringArgumentType.word())
                            .suggests(VillagerCommands::suggestPersonalities)
                            .executes(VillagerCommands::setPersonality))))
                .then(CommandManager.literal("list")
                    .executes(VillagerCommands::listPersonalities)))
            
            
            .then(CommandManager.literal("schedule")
                .then(CommandManager.argument("villager", EntityArgumentType.entity())
                    .executes(VillagerCommands::showSchedule)))
            
            
            .then(CommandManager.literal("data")
                .then(CommandManager.literal("export")
                    .then(CommandManager.argument("villager", EntityArgumentType.entity())
                        .executes(VillagerCommands::exportVillagerData)))
                .then(CommandManager.literal("reset")
                    .then(CommandManager.argument("villager", EntityArgumentType.entity())
                        .executes(VillagerCommands::resetVillagerData))))
            
            
            .then(CommandManager.literal("debug")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("relationships")
                    .executes(VillagerCommands::debugRelationships))
                .then(CommandManager.literal("cleanup")
                    .executes(VillagerCommands::cleanupData))));
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
            sendError(source, "No villagers found in the selected entities");
            return 0;
        }
        
        int renamedCount = 0;
        for (VillagerEntity villager : villagers) {
            VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            if (data != null) {
                String oldName = data.getName();
                data.setName(newName);
                villager.setAttached(Villagersreborn.VILLAGER_NAME, newName); 
                villager.setCustomName(Text.literal(newName));
                renamedCount++;
                
                String feedback = !oldName.isEmpty() 
                    ? String.format("Renamed villager from '%s' to '%s' at (%.1f, %.1f, %.1f)", 
                        oldName, newName, villager.getX(), villager.getY(), villager.getZ())
                    : String.format("Named villager '%s' at (%.1f, %.1f, %.1f)", 
                        newName, villager.getX(), villager.getY(), villager.getZ());
                
                sendInfo(source, feedback);
            }
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

        VillagerData data = nearestVillager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (data == null) return 0;
        
        String oldName = data.getName();
        data.setName(newName);
        nearestVillager.setAttached(Villagersreborn.VILLAGER_NAME, newName); 
        nearestVillager.setCustomName(Text.literal(newName));

        String feedback = !oldName.isEmpty() 
            ? String.format("Renamed villager from '%s' to '%s'", oldName, newName)
            : String.format("Named villager '%s'", newName);
        
        sendSuccess(source, feedback);
        return 1;
    }

    

    private static int showVillagerInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "villager");
        if (!(entity instanceof VillagerEntity villager)) {
            sendError(context.getSource(), "Entity is not a villager");
            return 0;
        }
        
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (data == null) {
            sendError(context.getSource(), "Villager has no data");
            return 0;
        }
        
        ServerCommandSource source = context.getSource();
        
        sendInfo(source, "=== " + data.getName() + " ===");
        sendInfo(source, "Gender: " + data.getGender() + " | Age: " + data.getAgeInDays());
        sendInfo(source, "Personality: " + data.getPersonality() + " | Happiness: " + 
            data.getHappiness() + "% (" + data.getHappinessDescription() + ")");
        sendInfo(source, "Hobby: " + data.getHobby());
        
        if (!data.getFavoriteFood().isEmpty()) {
            sendInfo(source, "Favorite Food: " + data.getFavoriteFood());
        }
        
        sendInfo(source, "Birth Place: " + data.getBirthPlace());
        sendInfo(source, "Total Trades: " + data.getTotalTrades());
        
        if (!data.getProfessionHistory().isEmpty()) {
            sendInfo(source, "Profession History: " + String.join(" → ", data.getProfessionHistory()));
        }
        
        if (!data.getSpouseId().isEmpty()) {
            String spouseName = getVillagerNameById(source, data.getSpouseId());
            sendInfo(source, "Spouse: " + (spouseName != null ? spouseName : "Unknown (ID: " + data.getSpouseId() + ")"));
        }
        
        if (!data.getChildrenIds().isEmpty()) {
            List<String> childrenNames = data.getChildrenIds().stream()
                .map(childId -> getVillagerNameById(source, childId))
                .filter(name -> name != null)
                .collect(Collectors.toList());
            if (!childrenNames.isEmpty()) {
                sendInfo(source, "Children: " + String.join(", ", childrenNames));
            }
        }
        
        if (!data.getNotes().isEmpty()) {
            sendInfo(source, "Notes: " + data.getNotes());
        }
        
        return 1;
    }

    private static int showVillageStats(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, 200);
        
        if (villagers.isEmpty()) {
            sendInfo(source, "No villagers found in the area");
            return 0;
        }
        
        
        Map<String, Integer> professionCounts = new HashMap<>();
        Map<String, Integer> personalityCounts = new HashMap<>();
        int totalHappiness = 0;
        int marriedCount = 0;
        int totalAge = 0;
        int babyCount = 0;
        int elderCount = 0;
        Map<String, Integer> hobbyCount = new HashMap<>();
        
        for (VillagerEntity villager : villagers) {
            VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            if (data != null) {
                
                String profession = villager.getVillagerData().profession().toString();
                professionCounts.put(profession, professionCounts.getOrDefault(profession, 0) + 1);
                
                
                personalityCounts.put(data.getPersonality(), 
                    personalityCounts.getOrDefault(data.getPersonality(), 0) + 1);
                
                
                totalHappiness += data.getHappiness();
                
                
                if (!data.getSpouseId().isEmpty()) marriedCount++;
                
                
                totalAge += data.getAge();
                if (data.getAge() < 20) babyCount++;
                if (data.getAge() > 300) elderCount++;
                
                
                hobbyCount.put(data.getHobby(), hobbyCount.getOrDefault(data.getHobby(), 0) + 1);
            }
        }
        
        sendInfo(source, "=== Village Statistics ===");
        sendInfo(source, "Total Villagers: " + villagers.size());
        sendInfo(source, "Average Happiness: " + (totalHappiness / villagers.size()) + "%");
        sendInfo(source, "Average Age: " + (totalAge / villagers.size()) + " days");
        sendInfo(source, "Married: " + marriedCount + " (" + (marriedCount * 100 / villagers.size()) + "%)");
        sendInfo(source, "Babies: " + babyCount + " | Elders: " + elderCount);
        
        sendInfo(source, "\nProfessions:");
        professionCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> sendInfo(source, "  " + entry.getKey() + ": " + entry.getValue()));
        
        sendInfo(source, "\nPersonalities:");
        personalityCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> sendInfo(source, "  " + entry.getKey() + ": " + entry.getValue()));
        
        sendInfo(source, "\nPopular Hobbies:");
        hobbyCount.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> sendInfo(source, "  " + entry.getKey() + ": " + entry.getValue()));
        
        return 1;
    }

    private static int showFamilyTree(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "villager");
        if (!(entity instanceof VillagerEntity villager)) {
            sendError(context.getSource(), "Entity is not a villager");
            return 0;
        }
        
        // Check if the command source is a player
        if (context.getSource().getPlayer() == null) {
            sendError(context.getSource(), "Only players can view the family tree GUI");
            return 0;
        }
        
        // Send packet to open family tree GUI on client
        ServerPlayNetworking.send(context.getSource().getPlayer(), 
            new OpenFamilyTreePacket(villager.getId()));
        
        sendSuccess(context.getSource(), "Opening family tree for " + 
            (villager.getAttached(Villagersreborn.VILLAGER_DATA) != null ? 
             villager.getAttached(Villagersreborn.VILLAGER_DATA).getName() : "villager"));
        
        return 1;
    }

    private static int marryVillagers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity1 = EntityArgumentType.getEntity(context, "villager1");
        Entity entity2 = EntityArgumentType.getEntity(context, "villager2");
        
        if (!(entity1 instanceof VillagerEntity villager1) || !(entity2 instanceof VillagerEntity villager2)) {
            sendError(context.getSource(), "Both entities must be villagers");
            return 0;
        }
        
        if (VillagerRelationshipManager.attemptMarriage(villager1, villager2)) {
            sendSuccess(context.getSource(), "Marriage successful!");
        } else {
            sendError(context.getSource(), "Marriage failed - conditions not met");
            
            
            if (!VillagerRelationshipManager.canMarry(villager1, villager2)) {
                VillagerData data1 = villager1.getAttached(Villagersreborn.VILLAGER_DATA);
                VillagerData data2 = villager2.getAttached(Villagersreborn.VILLAGER_DATA);
                
                if (data1 != null && data2 != null) {
                    if (data1.getAge() < 100 || data2.getAge() < 100) {
                        sendInfo(context.getSource(), "Reason: One or both villagers are too young");
                    } else if (!data1.getSpouseId().isEmpty() || !data2.getSpouseId().isEmpty()) {
                        sendInfo(context.getSource(), "Reason: One or both villagers are already married");
                    } else if (data1.getHappiness() < 40 || data2.getHappiness() < 40) {
                        sendInfo(context.getSource(), "Reason: One or both villagers are too unhappy");
                    } else {
                        sendInfo(context.getSource(), "Reason: Incompatible personalities or other factors");
                    }
                }
            }
        }
        
        return 1;
    }

    private static int divorceVillagers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity1 = EntityArgumentType.getEntity(context, "villager1");
        Entity entity2 = EntityArgumentType.getEntity(context, "villager2");
        
        if (!(entity1 instanceof VillagerEntity villager1) || !(entity2 instanceof VillagerEntity villager2)) {
            sendError(context.getSource(), "Both entities must be villagers");
            return 0;
        }
        
        VillagerRelationshipManager.divorce(villager1, villager2);
        sendSuccess(context.getSource(), "Villagers have been divorced");
        
        return 1;
    }
    
    private static int breedVillagers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity1 = EntityArgumentType.getEntity(context, "villager1");
        Entity entity2 = EntityArgumentType.getEntity(context, "villager2");
        
        if (!(entity1 instanceof VillagerEntity villager1) || !(entity2 instanceof VillagerEntity villager2)) {
            sendError(context.getSource(), "Both entities must be villagers");
            return 0;
        }
        
        // Check if they're married
        VillagerData data1 = villager1.getAttached(Villagersreborn.VILLAGER_DATA);
        VillagerData data2 = villager2.getAttached(Villagersreborn.VILLAGER_DATA);
        
        if (data1 == null || data2 == null) {
            sendError(context.getSource(), "Villager data not found");
            return 0;
        }
        
        // Check if they are married to each other
        if (!data1.getSpouseId().equals(villager2.getUuidAsString()) || 
            !data2.getSpouseId().equals(villager1.getUuidAsString())) {
            sendError(context.getSource(), "Villagers must be married to each other to breed");
            return 0;
        }
        
        // Spawn baby villager
        if (villager1.getWorld() instanceof ServerWorld serverWorld) {
            VillagerEntity baby = new VillagerEntity(net.minecraft.entity.EntityType.VILLAGER, serverWorld);
            
            // Position baby between parents
            Vec3d pos1 = villager1.getPos();
            Vec3d pos2 = villager2.getPos();
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
            VillagerRelationshipManager.onVillagerBreed(villager1, villager2, baby);
            
            // Visual effects
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.HEART,
                babyPos.x, babyPos.y + 1, babyPos.z,
                15, 0.5, 0.5, 0.5, 0.1);
            
            serverWorld.playSound(null, baby.getBlockPos(),
                net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_YES, 
                net.minecraft.sound.SoundCategory.NEUTRAL, 1.0f, 1.5f);
            
            sendSuccess(context.getSource(), "Baby villager born! Welcome " + babyName + "!");
            
            // Notify nearby players
            serverWorld.getPlayers().forEach(player -> {
                if (player.getPos().distanceTo(babyPos) < 50) {
                    player.sendMessage(Text.literal("👶 " + data1.getName() + " and " + 
                        data2.getName() + " had a baby: " + babyName + "!").formatted(Formatting.GREEN), false);
                }
            });
        } else {
            sendError(context.getSource(), "Can only breed villagers in server world");
            return 0;
        }
        
        return 1;
    }

    private static int setHappiness(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "villager");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        
        if (!(entity instanceof VillagerEntity villager)) {
            sendError(context.getSource(), "Entity is not a villager");
            return 0;
        }
        
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (data != null) {
            data.setHappiness(amount);
            sendSuccess(context.getSource(), "Set " + data.getName() + "'s happiness to " + amount + "%");
        }
        
        return 1;
    }

    private static int adjustHappiness(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "villager");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        
        if (!(entity instanceof VillagerEntity villager)) {
            sendError(context.getSource(), "Entity is not a villager");
            return 0;
        }
        
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (data != null) {
            data.adjustHappiness(amount);
            sendSuccess(context.getSource(), "Adjusted " + data.getName() + "'s happiness by " + 
                (amount >= 0 ? "+" : "") + amount + "% (now " + data.getHappiness() + "%)");
        }
        
        return 1;
    }

    private static int happinessReport(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, 100);
        
        if (villagers.isEmpty()) {
            sendInfo(source, "No villagers found in the area");
            return 0;
        }
        
        
        List<VillagerEntity> sortedVillagers = villagers.stream()
            .filter(v -> v.getAttached(Villagersreborn.VILLAGER_DATA) != null)
            .sorted((v1, v2) -> {
                VillagerData d1 = v1.getAttached(Villagersreborn.VILLAGER_DATA);
                VillagerData d2 = v2.getAttached(Villagersreborn.VILLAGER_DATA);
                return Integer.compare(d2.getHappiness(), d1.getHappiness());
            })
            .collect(Collectors.toList());
        
        sendInfo(source, "=== Happiness Report ===");
        
        
        sendInfo(source, "Happiest Villagers:");
        sortedVillagers.stream().limit(5).forEach(villager -> {
            VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            sendInfo(source, String.format("  %s: %d%% (%s)", 
                data.getName(), data.getHappiness(), data.getHappinessDescription()));
        });
        
        
        if (sortedVillagers.size() > 5) {
            sendInfo(source, "\nUnhappiest Villagers:");
            sortedVillagers.stream()
                .skip(Math.max(0, sortedVillagers.size() - 5))
                .forEach(villager -> {
                    VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
                    sendInfo(source, String.format("  %s: %d%% (%s)", 
                        data.getName(), data.getHappiness(), data.getHappinessDescription()));
                });
        }
        
        return 1;
    }

    private static int setPersonality(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "villager");
        String personality = StringArgumentType.getString(context, "personality");
        
        if (!(entity instanceof VillagerEntity villager)) {
            sendError(context.getSource(), "Entity is not a villager");
            return 0;
        }
        
        
        if (!Arrays.asList(VillagerData.PERSONALITIES).contains(personality)) {
            sendError(context.getSource(), "Invalid personality. Use /villager personality list to see valid options");
            return 0;
        }
        
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (data != null) {
            String oldPersonality = data.getPersonality();
            data.setPersonality(personality);
            sendSuccess(context.getSource(), "Changed " + data.getName() + "'s personality from " + 
                oldPersonality + " to " + personality);
        }
        
        return 1;
    }

    private static int listPersonalities(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        sendInfo(source, "Available Personalities:");
        for (String personality : VillagerData.PERSONALITIES) {
            sendInfo(source, "  - " + personality);
        }
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestPersonalities(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        for (String personality : VillagerData.PERSONALITIES) {
            builder.suggest(personality);
        }
        return builder.buildFuture();
    }

    private static int showSchedule(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "villager");
        
        if (!(entity instanceof VillagerEntity villager)) {
            sendError(context.getSource(), "Entity is not a villager");
            return 0;
        }
        
        List<Text> scheduleInfo = VillagerScheduleManager.getScheduleInfo(villager);
        scheduleInfo.forEach(text -> context.getSource().sendFeedback(() -> text, false));
        
        return 1;
    }

    private static int exportVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "villager");
        
        if (!(entity instanceof VillagerEntity villager)) {
            sendError(context.getSource(), "Entity is not a villager");
            return 0;
        }
        
        VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (data == null) {
            sendError(context.getSource(), "Villager has no data");
            return 0;
        }
        
        
        ServerCommandSource source = context.getSource();
        sendInfo(source, "=== Villager Data Export ===");
        sendInfo(source, "Name: " + data.getName());
        sendInfo(source, "Gender: " + data.getGender());
        sendInfo(source, "Age: " + data.getAge());
        sendInfo(source, "Personality: " + data.getPersonality());
        sendInfo(source, "Happiness: " + data.getHappiness());
        sendInfo(source, "Hobby: " + data.getHobby());
        sendInfo(source, "Favorite Food: " + data.getFavoriteFood());
        sendInfo(source, "Birth Time: " + data.getBirthTime());
        sendInfo(source, "Birth Place: " + data.getBirthPlace());
        sendInfo(source, "Total Trades: " + data.getTotalTrades());
        sendInfo(source, "Spouse: " + data.getSpouseName() + " (ID: " + data.getSpouseId() + ")");
        sendInfo(source, "Children: " + String.join(", ", data.getChildrenNames()));
        sendInfo(source, "Family: " + String.join(", ", data.getFamilyMembers()));
        sendInfo(source, "Profession History: " + String.join(", ", data.getProfessionHistory()));
        sendInfo(source, "Notes: " + data.getNotes());
        sendInfo(source, "=== End Export ===");
        
        return 1;
    }

    private static int resetVillagerData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgumentType.getEntity(context, "villager");
        
        if (!(entity instanceof VillagerEntity villager)) {
            sendError(context.getSource(), "Entity is not a villager");
            return 0;
        }
        
        
        VillagerData newData = new VillagerData();
        villager.setAttached(Villagersreborn.VILLAGER_DATA, newData);
        
        sendSuccess(context.getSource(), "Reset data for villager at " + villager.getBlockPos());
        return 1;
    }

    private static int debugRelationships(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        List<VillagerEntity> villagers = getAllVillagersInArea(source, 200);
        
        Map<String, List<String>> marriages = new HashMap<>();
        Map<String, List<String>> families = new HashMap<>();
        
        for (VillagerEntity villager : villagers) {
            VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            if (data != null) {
                if (!data.getSpouseName().isEmpty()) {
                    marriages.computeIfAbsent(data.getName(), k -> new ArrayList<>())
                        .add(data.getSpouseName());
                }
                
                if (!data.getFamilyMembers().isEmpty()) {
                    families.put(data.getName(), data.getFamilyMembers());
                }
            }
        }
        
        sendInfo(source, "=== Relationship Debug ===");
        sendInfo(source, "Marriages:");
        marriages.forEach((name, spouses) -> 
            sendInfo(source, "  " + name + " ↔ " + String.join(", ", spouses)));
        
        sendInfo(source, "\nFamily Connections:");
        families.forEach((name, members) -> 
            sendInfo(source, "  " + name + ": " + String.join(", ", members)));
        
        return 1;
    }

    private static int cleanupData(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        
        // Clean up stale proposal times
        int cleanedEntries = VillagerRelationshipManager.cleanupStaleProposalTimes();
        
        sendInfo(source, "Data cleanup completed. Removed " + cleanedEntries + " stale proposal time entries.");
        
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
            .sorted((v1, v2) -> {
                VillagerData d1 = v1.getAttached(Villagersreborn.VILLAGER_DATA);
                VillagerData d2 = v2.getAttached(Villagersreborn.VILLAGER_DATA);
                if (d1 == null || d2 == null) return 0;
                return d1.getName().compareTo(d2.getName());
            })
            .forEach(villager -> {
                VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
                if (data != null) {
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
                VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
                return data != null && data.getName().toLowerCase().contains(searchName.toLowerCase());
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
        VillagerData data = closestMatch.getAttached(Villagersreborn.VILLAGER_DATA);
        Vec3d pos = closestMatch.getPos();
        double distance = Math.sqrt(source.getPosition().squaredDistanceTo(pos));
        String professionId = closestMatch.getVillagerData().profession().toString();
        
        
        Text coordsText = Text.literal(String.format("[%.1f, %.1f, %.1f]", pos.x, pos.y, pos.z))
            .formatted(Formatting.AQUA);
        
        Text message = Text.literal("Found ")
            .append(Text.literal(data.getName()).formatted(Formatting.GREEN))
            .append(Text.literal(" (" + professionId + ")").formatted(Formatting.GRAY))
            .append(Text.literal(" at "))
            .append(coordsText)
            .append(Text.literal(String.format(" - %.1f blocks away", distance)).formatted(Formatting.GRAY));
        
        source.sendFeedback(() -> message, false);
        
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
                world,
                villager.getBlockPos()
            );
            
            VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            if (data != null) {
                data.setName(newName);
                villager.setAttached(Villagersreborn.VILLAGER_NAME, newName); 
                villager.setCustomName(Text.literal(newName));
                renamedCount++;
            }
        }
        
        sendSuccess(source, String.format("Randomized names for %d villager%s", 
            renamedCount, renamedCount == 1 ? "" : "s"));
        return renamedCount;
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
            .filter(villager -> {
                VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
                return data != null && !data.getName().isEmpty();
            })
            .collect(Collectors.toList());
    }

    private static List<VillagerEntity> getAllVillagersInArea(ServerCommandSource source, double radius) {
        World world = source.getWorld();
        Vec3d sourcePos = source.getPosition();
        
        // Use ServerVillagerManager instead of scanning the world
        List<VillagerEntity> villagers = new ArrayList<>();
        for (VillagerEntity villager : ServerVillagerManager.getInstance().getAllTrackedVillagers()) {
            // Only include villagers in the same world
            if (villager.getWorld() != world) continue;
            
            // Check if villager is within radius
            if (sourcePos.distanceTo(villager.getPos()) <= radius) {
                villagers.add(villager);
            }
        }
        
        return villagers;
    }

    private static String getVillagerNameById(ServerCommandSource source, String villagerUuid) {
        if (villagerUuid == null || villagerUuid.isEmpty()) {
            return null;
        }
        
        // Use ServerVillagerManager instead of scanning the world
        try {
            VillagerEntity villager = ServerVillagerManager.getInstance().getVillager(UUID.fromString(villagerUuid));
            if (villager != null) {
                VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
                return data != null ? data.getName() : null;
            }
        } catch (IllegalArgumentException e) {
            // Invalid UUID format
            return null;
        }
        
        return null;
    }

    

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