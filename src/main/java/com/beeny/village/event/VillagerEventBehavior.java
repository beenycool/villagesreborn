package com.beeny.village.event;

import com.beeny.village.VillageEvent;
import com.beeny.village.VillagerAI;
import com.beeny.village.VillagerManager;
import com.beeny.village.VillagerFeedbackHelper;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.entity.player.PlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Provides behavior handlers for villagers during different event phases.
 * This class defines specific actions for villagers to perform during
 * the various phases of cultural and social events.
 */
public class VillagerEventBehavior {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerEventBehavior.class);
    private static final Random RANDOM = new Random();
    private static final int EVENT_RADIUS = 32; // Default radius for event activities
    
    /**
     * Factory method to create behavior handlers for festival events
     * @param eventName The name of the event
     * @param phaseName The name of the phase
     * @return A consumer to handle the specific phase's behavior
     */
    public static Consumer<VillageEvent> createFestivalBehavior(String eventName, String phaseName) {
        return event -> {
            LOGGER.debug("Festival phase starting: {} - {}", eventName, phaseName);
            
            BlockPos eventCenter = event.getEventCenter();
            ServerWorld world = (ServerWorld) event.getParticipatingPlayers().iterator().next().getWorld();
            
            // Find villagers in the event area
            List<VillagerEntity> nearbyVillagers = getNearbyVillagers(world, eventCenter, EVENT_RADIUS);
            
            // Different behavior based on the phase
            switch (phaseName.toLowerCase()) {
                case "preparation":
                case "arrivals":
                case "herald's call":
                case "opening ceremony":
                    // Villagers gather at the event center
                    gatherVillagersAtLocation(nearbyVillagers, eventCenter);
                    announceEventStart(world, eventCenter, event.getName(), phaseName);
                    break;
                
                case "feast":
                case "celebration":
                case "dining":
                case "dancing":
                    // Festive activities - dancing, celebrating
                    makeVillagersCelebrate(nearbyVillagers, eventCenter);
                    spawnFestiveParticles(world, eventCenter);
                    break;
                    
                case "games":
                case "contests":
                case "jousting":
                case "athletic competitions":
                case "melee":
                    // Organize villagers into performance areas
                    organizeCompetition(nearbyVillagers, eventCenter);
                    break;
                    
                case "awards":
                case "results":
                case "ceremony":
                    // Gather and celebrate winners
                    gatherVillagersAtLocation(nearbyVillagers, eventCenter);
                    spawnAwardParticles(world, eventCenter);
                    break;
                    
                default:
                    // General festival behavior
                    makeVillagersSocialize(nearbyVillagers, eventCenter);
                    break;
            }
        };
    }
    
    /**
     * Factory method to create behavior handlers for disaster events
     * @param eventName The name of the event
     * @param phaseName The name of the phase
     * @return A consumer to handle the specific phase's behavior
     */
    public static Consumer<VillageEvent> createDisasterBehavior(String eventName, String phaseName) {
        return event -> {
            LOGGER.debug("Disaster phase starting: {} - {}", eventName, phaseName);
            
            BlockPos eventCenter = event.getEventCenter();
            ServerWorld world = (ServerWorld) event.getParticipatingPlayers().iterator().next().getWorld();
            
            // Find villagers in the event area
            List<VillagerEntity> nearbyVillagers = getNearbyVillagers(world, eventCenter, EVENT_RADIUS);
            
            // Different behavior based on the phase
            switch (phaseName.toLowerCase()) {
                case "early signs":
                case "warning":
                case "outbreak":
                    // Villagers look concerned, gather in groups
                    makeVillagersLookConcerned(nearbyVillagers);
                    gatherVillagersInGroups(nearbyVillagers, eventCenter);
                    break;
                
                case "crisis":
                case "growing blaze":
                case "spreading":
                case "attack":
                    // Villagers panic and flee from danger
                    makeVillagersFlee(nearbyVillagers, eventCenter);
                    spawnDangerParticles(world, eventCenter);
                    break;
                    
                case "last stand":
                case "critical point":
                    // Villagers seek shelter or help
                    makeVillagersSeekShelter(nearbyVillagers);
                    break;
                    
                case "recovery":
                case "treatment":
                    // Villagers work together to recover
                    makeVillagersWork(nearbyVillagers, eventCenter);
                    break;
                    
                default:
                    // Default disaster behavior
                    makeVillagersFlee(nearbyVillagers, eventCenter);
                    break;
            }
        };
    }
    
    /**
     * Factory method to create behavior handlers for political events
     * @param eventName The name of the event
     * @param phaseName The name of the phase
     * @return A consumer to handle the specific phase's behavior
     */
    public static Consumer<VillageEvent> createPoliticalBehavior(String eventName, String phaseName) {
        return event -> {
            LOGGER.debug("Political phase starting: {} - {}", eventName, phaseName);
            
            BlockPos eventCenter = event.getEventCenter();
            ServerWorld world = (ServerWorld) event.getParticipatingPlayers().iterator().next().getWorld();
            
            // Find villagers in the event area
            List<VillagerEntity> nearbyVillagers = getNearbyVillagers(world, eventCenter, EVENT_RADIUS);
            
            // Different behavior based on the phase
            switch (phaseName.toLowerCase()) {
                case "nominations":
                case "preparations":
                case "arrival":
                    // Villagers gather at the event center for the beginning
                    gatherVillagersAtLocation(nearbyVillagers, eventCenter);
                    announceEventStart(world, eventCenter, event.getName(), phaseName);
                    break;
                
                case "campaigning":
                case "negotiations":
                case "procession":
                    // Villagers move around in organized fashion
                    organizeProcession(nearbyVillagers, eventCenter);
                    break;
                    
                case "voting":
                case "signing":
                    // Villagers line up and take turns at focal point
                    organizeQueue(nearbyVillagers, eventCenter);
                    break;
                    
                case "results":
                case "ceremony":
                    // Formal gathering
                    gatherVillagersAtLocation(nearbyVillagers, eventCenter);
                    makeVillagersLookAtCenter(nearbyVillagers, eventCenter);
                    break;
                    
                default:
                    // Default political behavior
                    makeVillagersSocialize(nearbyVillagers, eventCenter);
                    break;
            }
        };
    }
    
    /**
     * Villager phase tick handler - updates ongoing behaviors
     * @param event The event being processed
     */
    public static Consumer<VillageEvent> createPhaseTicker(String eventType) {
        return event -> {
            BlockPos eventCenter = event.getEventCenter();
            
            // Only run ticker occasionally to prevent performance issues
            if (RANDOM.nextInt(20) != 0) return;
            
            try {
                ServerWorld world = (ServerWorld) event.getParticipatingPlayers().iterator().next().getWorld();
                List<VillagerEntity> nearbyVillagers = getNearbyVillagers(world, eventCenter, EVENT_RADIUS);
                
                // Keep villagers engaged in their assigned activities
                VillagerEventBehavior.maintainEventActivity(nearbyVillagers, eventCenter, eventType, 
                    event.getCurrentPhase() != null ? event.getCurrentPhase().getName() : "");
                
                // Spawn appropriate particles based on event type
                if (eventType.contains("FESTIVAL") || eventType.contains("CELEBRATION")) {
                    spawnFestiveParticles(world, eventCenter);
                } else if (eventType.contains("DISASTER")) {
                    spawnDangerParticles(world, eventCenter);
                } else if (eventType.contains("POLITICAL")) {
                    spawnFormalParticles(world, eventCenter);
                }
            } catch (Exception e) {
                LOGGER.error("Error in event phase ticker", e);
            }
        };
    }
    
    /**
     * Villager phase completion handler - transitions between phases
     * @param event The event being processed
     */
    public static Consumer<VillageEvent> createPhaseCompleter(String nextPhaseName) {
        return event -> {
            LOGGER.debug("Event phase completed, next phase: {}", nextPhaseName);
            BlockPos eventCenter = event.getEventCenter();
            
            try {
                ServerWorld world = (ServerWorld) event.getParticipatingPlayers().iterator().next().getWorld();
                List<VillagerEntity> nearbyVillagers = getNearbyVillagers(world, eventCenter, EVENT_RADIUS);
                
                // Announce phase completion to nearby players
                for (PlayerEntity player : world.getPlayers()) {
                    if (player.squaredDistanceTo(eventCenter.getX(), eventCenter.getY(), eventCenter.getZ()) < 50*50) {
                        player.sendMessage(Text.of("§6The village event moves to the next phase: §e" + 
                            (nextPhaseName != null ? nextPhaseName : "Final Phase")), false);
                    }
                }
                
                // Reset villager activities to prepare for next phase
                for (VillagerEntity villager : nearbyVillagers) {
                    VillagerAI ai = VillagerManager.getInstance().getVillagerAI(villager.getUuid());
                    if (ai != null) {
                        ai.setBusy(false);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error in event phase completion handler", e);
            }
        };
    }
    
    // ===== Helper Methods =====
    
    private static List<VillagerEntity> getNearbyVillagers(ServerWorld world, BlockPos center, int radius) {
        Box searchBox = new Box(
            center.getX() - radius, center.getY() - 5, center.getZ() - radius,
            center.getX() + radius, center.getY() + 15, center.getZ() + radius
        );
        
        return world.getEntitiesByClass(VillagerEntity.class, searchBox, villager -> true);
    }
    
    private static void announceEventStart(ServerWorld world, BlockPos center, String eventName, String phaseName) {
        for (PlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(center.getX(), center.getY(), center.getZ()) < 50*50) {
                player.sendMessage(Text.of("§6Village event starting: §e" + eventName + 
                    " §6(Phase: §e" + phaseName + "§6)"), false);
            }
        }
    }
    
    private static void gatherVillagersAtLocation(List<VillagerEntity> villagers, BlockPos location) {
        VillagerManager vm = VillagerManager.getInstance();
        
        for (VillagerEntity villager : villagers) {
            VillagerAI ai = vm.getVillagerAI(villager.getUuid());
            if (ai != null && !ai.isBusy()) {
                // Mark villager as busy with event
                ai.setBusy(true);
                
                // Update activity and move to location
                ai.updateActivity("attending_event");
                
                // Calculate a slightly randomized position around the event center
                BlockPos targetPos = getRandomPositionAround(location, 5);
                moveVillagerToPosition(ai, targetPos);
            }
        }
    }
    
    private static void makeVillagersCelebrate(List<VillagerEntity> villagers, BlockPos center) {
        VillagerManager vm = VillagerManager.getInstance();
        
        for (VillagerEntity villager : villagers) {
            VillagerAI ai = vm.getVillagerAI(villager.getUuid());
            if (ai != null) {
                ai.setBusy(true);
                ai.updateActivity("celebrating");
                
                // Have villagers wander around the celebration area
                BlockPos targetPos = getRandomPositionAround(center, 8);
                moveVillagerToPosition(ai, targetPos);
                
                // Show celebration effects occasionally
                if (RANDOM.nextInt(10) == 0) {
                    VillagerFeedbackHelper.showSpeakingEffect(villager);
                }
            }
        }
    }
    
    private static void makeVillagersSocialize(List<VillagerEntity> villagers, BlockPos center) {
        VillagerManager vm = VillagerManager.getInstance();
        
        // Form small groups of villagers
        for (int i = 0; i < villagers.size(); i++) {
            VillagerEntity villager = villagers.get(i);
            VillagerAI ai = vm.getVillagerAI(villager.getUuid());
            
            if (ai != null) {
                ai.setBusy(true);
                ai.updateActivity("socializing");
                
                // Calculate different gathering points around the event center
                int groupId = i % 5; // Create up to 5 different groups
                BlockPos groupPos = center.add(
                    3 + groupId * 2 - 5, 
                    0, 
                    3 + ((groupId * 7) % 10) - 5
                );
                
                moveVillagerToPosition(ai, groupPos);
            }
        }
    }
    
    private static void makeVillagersLookConcerned(List<VillagerEntity> villagers) {
        VillagerManager vm = VillagerManager.getInstance();
        
        for (VillagerEntity villager : villagers) {
            VillagerAI ai = vm.getVillagerAI(villager.getUuid());
            if (ai != null) {
                ai.setBusy(true);
                ai.updateActivity("concerned");
                
                // Make the villager look around nervously
                villager.getNavigation().stop();
                villager.getLookControl().lookAt(
                    villager.getX() + RANDOM.nextDouble() * 10 - 5,
                    villager.getY() + RANDOM.nextDouble() * 2,
                    villager.getZ() + RANDOM.nextDouble() * 10 - 5,
                    30F, 30F
                );
            }
        }
    }
    
    private static void makeVillagersFlee(List<VillagerEntity> villagers, BlockPos danger) {
        VillagerManager vm = VillagerManager.getInstance();
        
        for (VillagerEntity villager : villagers) {
            VillagerAI ai = vm.getVillagerAI(villager.getUuid());
            if (ai != null) {
                ai.setBusy(true);
                ai.updateActivity("fleeing_danger");
                
                // Calculate a position away from the danger
                double dx = villager.getX() - danger.getX();
                double dz = villager.getZ() - danger.getZ();
                
                // Normalize and scale the vector
                double length = Math.sqrt(dx * dx + dz * dz);
                if (length < 0.1) {
                    // If too close to danger, pick a random direction
                    dx = RANDOM.nextDouble() * 2 - 1;
                    dz = RANDOM.nextDouble() * 2 - 1;
                    length = Math.sqrt(dx * dx + dz * dz);
                }
                
                dx = dx / length * 15; // Scale to 15 blocks away
                dz = dz / length * 15;
                
                BlockPos fleePos = new BlockPos(
                    danger.getX() + (int)dx,
                    danger.getY(),
                    danger.getZ() + (int)dz
                );
                
                moveVillagerToPosition(ai, fleePos);
            }
        }
    }
    
    private static void makeVillagersSeekShelter(List<VillagerEntity> villagers) {
        VillagerManager vm = VillagerManager.getInstance();
        
        for (VillagerEntity villager : villagers) {
            VillagerAI ai = vm.getVillagerAI(villager.getUuid());
            if (ai != null) {
                ai.setBusy(true);
                ai.updateActivity("seeking_shelter");
                
                // Seek shelter at home locations if possible
                villager.getBrain().getOptionalMemory(net.minecraft.entity.ai.brain.MemoryModuleType.HOME)
                    .ifPresent(pos -> moveVillagerToPosition(ai, pos));
            }
        }
    }
    
    private static void makeVillagersWork(List<VillagerEntity> villagers, BlockPos workArea) {
        VillagerManager vm = VillagerManager.getInstance();
        
        for (VillagerEntity villager : villagers) {
            VillagerAI ai = vm.getVillagerAI(villager.getUuid());
            if (ai != null) {
                ai.setBusy(true);
                ai.updateActivity("working");
                
                // Assign different work positions around the area
                BlockPos workPos = getRandomPositionAround(workArea, 8);
                moveVillagerToPosition(ai, workPos);
            }
        }
    }
    
    private static void organizeCompetition(List<VillagerEntity> villagers, BlockPos center) {
        VillagerManager vm = VillagerManager.getInstance();
        
        // Create two teams/groups of villagers
        for (int i = 0; i < villagers.size(); i++) {
            VillagerEntity villager = villagers.get(i);
            VillagerAI ai = vm.getVillagerAI(villager.getUuid());
            
            if (ai != null) {
                ai.setBusy(true);
                ai.updateActivity("competing");
                
                // Determine team (0 or 1)
                int team = i % 2;
                
                // Position depends on team
                int offsetX = team == 0 ? -10 : 10;
                int positionInTeam = i / 2;
                int offsetZ = (positionInTeam % 5) * 3 - 6;
                
                BlockPos teamPos = center.add(offsetX, 0, offsetZ);
                moveVillagerToPosition(ai, teamPos);
                
                // For "competitors", make them look at the other team
                if (positionInTeam < 3) {
                    villager.getLookControl().lookAt(
                        center.getX() - offsetX, 
                        center.getY(), 
                        center.getZ() + offsetZ, 
                        30F, 30F
                    );
                }
            }
        }
    }
    
    private static void organizeProcession(List<VillagerEntity> villagers, BlockPos center) {
        VillagerManager vm = VillagerManager.getInstance();
        
        // Create a procession line
        BlockPos startPos = center.add(-15, 0, 0);
        BlockPos endPos = center.add(15, 0, 0);
        
        for (int i = 0; i < villagers.size(); i++) {
            VillagerEntity villager = villagers.get(i);
            VillagerAI ai = vm.getVillagerAI(villager.getUuid());
            
            if (ai != null) {
                ai.setBusy(true);
                ai.updateActivity("procession");
                
                // Calculate position along the line
                double progress = (double)i / Math.max(1, villagers.size() - 1);
                BlockPos processPos = new BlockPos(
                    startPos.getX() + (int)((endPos.getX() - startPos.getX()) * progress),
                    center.getY(),
                    startPos.getZ() + (int)((endPos.getZ() - startPos.getZ()) * progress)
                );
                
                moveVillagerToPosition(ai, processPos);
                
                // Make them look in the direction of the procession
                villager.getLookControl().lookAt(
                    endPos.getX(), 
                    center.getY(), 
                    endPos.getZ(), 
                    30F, 30F
                );
            }
        }
    }
    
    private static void organizeQueue(List<VillagerEntity> villagers, BlockPos center) {
        VillagerManager vm = VillagerManager.getInstance();
        
        // Create a queue line to the center
        for (int i = 0; i < villagers.size(); i++) {
            VillagerEntity villager = villagers.get(i);
            VillagerAI ai = vm.getVillagerAI(villager.getUuid());
            
            if (ai != null) {
                ai.setBusy(true);
                ai.updateActivity("queuing");
                
                // Position in line, starting from center
                BlockPos queuePos = center.add(0, 0, i * 1);
                moveVillagerToPosition(ai, queuePos);
                
                // Make them look ahead in the queue
                villager.getLookControl().lookAt(
                    center.getX(), 
                    center.getY(), 
                    center.getZ(), 
                    30F, 30F
                );
            }
        }
    }
    
    private static void gatherVillagersInGroups(List<VillagerEntity> villagers, BlockPos center) {
        VillagerManager vm = VillagerManager.getInstance();
        
        // Divide villagers into small discussion groups
        for (int i = 0; i < villagers.size(); i++) {
            VillagerEntity villager = villagers.get(i);
            VillagerAI ai = vm.getVillagerAI(villager.getUuid());
            
            if (ai != null) {
                ai.setBusy(true);
                ai.updateActivity("gathering");
                
                // Calculate group position (up to 3 groups)
                int groupId = i % 3;
                int offsetX = groupId * 8 - 8;
                int offsetZ = (i / 3) % 3 * 3 - 3;
                
                BlockPos groupPos = center.add(offsetX, 0, offsetZ);
                moveVillagerToPosition(ai, groupPos);
            }
        }
    }
    
    private static void makeVillagersLookAtCenter(List<VillagerEntity> villagers, BlockPos center) {
        for (VillagerEntity villager : villagers) {
            villager.getLookControl().lookAt(
                center.getX(), 
                center.getY(), 
                center.getZ(), 
                30F, 30F
            );
        }
    }
    
    private static void spawnFestiveParticles(ServerWorld world, BlockPos center) {
        // Spawn festive particles around the event center
        for (int i = 0; i < 5; i++) {
            double x = center.getX() + RANDOM.nextDouble() * 10 - 5;
            double y = center.getY() + 1 + RANDOM.nextDouble() * 2;
            double z = center.getZ() + RANDOM.nextDouble() * 10 - 5;
            
            world.spawnParticles(
                ParticleTypes.NOTE, 
                x, y, z, 
                1, 0.5, 0.5, 0.5, 0
            );
            
            if (RANDOM.nextInt(3) == 0) {
                world.spawnParticles(
                    ParticleTypes.HAPPY_VILLAGER, 
                    x, y, z, 
                    1, 0.5, 0.5, 0.5, 0
                );
            }
        }
    }
    
    private static void spawnAwardParticles(ServerWorld world, BlockPos center) {
        // Spawn award ceremony particles
        for (int i = 0; i < 10; i++) {
            double x = center.getX() + RANDOM.nextDouble() * 6 - 3;
            double y = center.getY() + 2 + RANDOM.nextDouble() * 1;
            double z = center.getZ() + RANDOM.nextDouble() * 6 - 3;
            
            world.spawnParticles(
                ParticleTypes.FIREWORK, 
                x, y, z, 
                1, 0.2, 0.2, 0.2, 0.05
            );
        }
    }
    
    private static void spawnDangerParticles(ServerWorld world, BlockPos center) {
        // Spawn danger particles around the event center
        for (int i = 0; i < 5; i++) {
            double x = center.getX() + RANDOM.nextDouble() * 10 - 5;
            double y = center.getY() + 1 + RANDOM.nextDouble() * 2;
            double z = center.getZ() + RANDOM.nextDouble() * 10 - 5;
            
            world.spawnParticles(
                ParticleTypes.SMOKE, 
                x, y, z, 
                2, 0.5, 0.5, 0.5, 0.02
            );
            
            if (RANDOM.nextInt(3) == 0) {
                world.spawnParticles(
                    ParticleTypes.ANGRY_VILLAGER, 
                    x, y, z, 
                    1, 0.5, 0.5, 0.5, 0
                );
            }
        }
    }
    
    private static void spawnFormalParticles(ServerWorld world, BlockPos center) {
        // Spawn formal/political event particles
        for (int i = 0; i < 3; i++) {
            double x = center.getX() + RANDOM.nextDouble() * 6 - 3;
            double y = center.getY() + 2 + RANDOM.nextDouble() * 1;
            double z = center.getZ() + RANDOM.nextDouble() * 6 - 3;
            
            world.spawnParticles(
                ParticleTypes.END_ROD, 
                x, y, z, 
                1, 0.2, 0.2, 0.2, 0.01
            );
        }
    }
    
    private static BlockPos getRandomPositionAround(BlockPos center, int radius) {
        return center.add(
            RANDOM.nextInt(radius * 2) - radius,
            0,
            RANDOM.nextInt(radius * 2) - radius
        );
    }
    
    private static void moveVillagerToPosition(VillagerAI ai, BlockPos position) {
        try {
            // Use the VillagerAI's existing moveToPosition method
            // This is done via reflection since the method is private
            java.lang.reflect.Method moveMethod = VillagerAI.class.getDeclaredMethod(
                "moveToPosition", BlockPos.class, double.class, int.class);
            moveMethod.setAccessible(true);
            moveMethod.invoke(ai, position, 0.6D, 1);
        } catch (Exception e) {
            LOGGER.error("Failed to move villager to position", e);
            
            // Fallback: direct navigation
            VillagerEntity villager = ai.getVillager();
            villager.getNavigation().startMovingTo(
                position.getX() + 0.5, 
                position.getY(), 
                position.getZ() + 0.5, 
                0.6D
            );
        }
    }
    
    private static void maintainEventActivity(List<VillagerEntity> villagers, BlockPos center, 
                                             String eventType, String phaseName) {
        VillagerManager vm = VillagerManager.getInstance();
        
        for (VillagerEntity villager : villagers) {
            VillagerAI ai = vm.getVillagerAI(villager.getUuid());
            if (ai == null) continue;
            
            // If the villager has wandered too far from the event, bring them back
            if (villager.squaredDistanceTo(center.getX(), center.getY(), center.getZ()) > 25*25) {
                BlockPos returnPos = getRandomPositionAround(center, 8);
                moveVillagerToPosition(ai, returnPos);
            }
            
            // Occasionally make villagers interact with each other
            if (RANDOM.nextInt(20) == 0) {
                // Find another nearby villager
                List<VillagerEntity> veryCloseVillagers = villagers.stream()
                    .filter(v -> v != villager && v.squaredDistanceTo(villager) < 5*5)
                    .collect(Collectors.toList());
                
                if (!veryCloseVillagers.isEmpty()) {
                    VillagerEntity other = veryCloseVillagers.get(RANDOM.nextInt(veryCloseVillagers.size()));
                    
                    // Make them look at each other
                    villager.getLookControl().lookAt(other, 30F, 30F);
                    other.getLookControl().lookAt(villager, 30F, 30F);
                    
                    // Show interaction effects occasionally
                    if (RANDOM.nextInt(3) == 0) {
                        if (eventType.contains("FESTIVAL") || eventType.contains("CELEBRATION")) {
                            VillagerFeedbackHelper.showSpeakingEffect(villager);
                        } else if (eventType.contains("DISASTER")) {
                            // Worried animation for disasters
                            ServerWorld world = (ServerWorld) villager.getWorld();
                            world.spawnParticles(
                                ParticleTypes.SMOKE, 
                                villager.getX(), villager.getY() + 2, villager.getZ(), 
                                1, 0.1, 0.1, 0.1, 0.01
                            );
                        }
                    }
                }
            }
        }
    }
}