package com.beeny.village.event;

import com.beeny.ai.LLMService;
import com.beeny.village.VillagerAI;
import com.beeny.village.VillagerManager;
import com.beeny.village.SpawnRegion;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class VillageEventManager {
    private static final Logger LOGGER = Logger.getLogger(VillageEventManager.class.getName());
    private final Map<String, List<VillageEvent>> activeEvents = new HashMap<>();
    private final Map<String, List<VillageEvent>> historicalEvents = new HashMap<>();
    private final Map<String, Set<String>> successfulEventTypes = new HashMap<>();
    private final Random random = new Random();

    public CompletableFuture<VillageEvent> generateRandomEvent(String culture, ServerWorld world, List<VillagerAI> participants) {
        String prompt;
        Set<String> successful = successfulEventTypes.computeIfAbsent(culture, k -> new HashSet<>());
        
        // Get the current district if available
        SpawnRegion region = VillagerManager.getInstance()
            .getSpawnRegionForVillager(participants.get(0).getVillager());
        String district = region != null ? 
            region.getDistrictAtPosition(participants.get(0).getVillager().getBlockPos()) : null;

        if (district != null) {
            prompt = String.format(
                "Generate a cultural event appropriate for the %s district in a %s village:\n" +
                "Participants: %d\n" +
                "Recent events: %s\n" +
                "Consider the district's purpose and characteristics.",
                district, culture, participants.size(), getRecentEvents(culture)
            );
        } else if (!successful.isEmpty() && random.nextFloat() < 0.3f) {
            // Use previously successful event as inspiration if no specific district
            String inspiredBy = successful.stream()
                .skip(random.nextInt(successful.size()))
                .findFirst()
                .orElse("");
                
            prompt = String.format(
                "Generate a cultural event similar to %s for the following village:\n" +
                "Culture: %s\n" +
                "Participants: %d\n" +
                "Recent events: %s",
                inspiredBy, culture, participants.size(), getRecentEvents(culture)
            );
        } else {
            prompt = String.format(
                "Generate a random cultural event for the following village:\n" +
                "Culture: %s\n" +
                "Participants: %d\n" +
                "Recent events: %s",
                culture, participants.size(), getRecentEvents(culture)
            );
        }

        return LLMService.getInstance()
            .generateResponse(prompt)
            .thenApply(response -> {
                Map<String, String> eventDetails = parseEventResponse(response);
                String eventType = eventDetails.get("TYPE");
                
                if (eventType != null && !eventType.isEmpty()) {
                    successful.add(eventType);
                }
                
                return new VillageEvent.Builder()
                    .type(eventType)
                    .description(eventDetails.get("DESCRIPTION"))
                    .duration(Long.parseLong(eventDetails.getOrDefault("DURATION", "12000")))
                    .startTime(world.getTime())
                    .participants(participants.stream()
                        .map(ai -> ai.getVillager().getUuid())
                        .collect(Collectors.toList()))
                    .culture(culture)
                    .district(district)
                    .outcome(eventDetails.get("OUTCOME"))
                    .location(findEventLocation(world, participants))
                    .build();
            });
    }

    public void startEvent(VillageEvent event) {
        activeEvents.computeIfAbsent(event.getCulture(), k -> new ArrayList<>()).add(event);
        notifyParticipants(event);
    }

    public void updateEvents(ServerWorld world) {
        long currentTime = world.getTime();
        
        // Check for completed events
        activeEvents.forEach((culture, events) -> {
            Iterator<VillageEvent> iterator = events.iterator();
            while (iterator.hasNext()) {
                VillageEvent event = iterator.next();
                if (!event.isActive(currentTime)) {
                    completeEvent(event, world);
                    iterator.remove();
                    historicalEvents.computeIfAbsent(culture, k -> new ArrayList<>()).add(event);
                }
            }
        });

        // Generate new events if needed
        if (random.nextFloat() < 0.05f) { // 5% chance per tick
            VillagerManager.getInstance().getSpawnRegions().forEach(region -> {
                List<VillagerAI> villagers = getVillagersInRegion(region);
                if (!villagers.isEmpty() && getActiveEvents(region.getCultureAsString()).size() < 3) {
                    generateRandomEvent(region.getCultureAsString(), world, villagers)
                        .thenAccept(this::startEvent);
                }
            });
        }
    }

    private void completeEvent(VillageEvent event, ServerWorld world) {
        LOGGER.info(String.format("Completing event: %s in %s culture", event.getType(), event.getCulture()));
        
        // Generate completion effects
        String prompt = String.format(
            "Describe the lasting effects of this cultural event:\n" +
            "Event: %s\n" +
            "Description: %s\n" +
            "Culture: %s\n" +
            "Participants: %d\n\n" +
            "Consider effects on:\n" +
            "1. Village mood and atmosphere\n" +
            "2. Social bonds\n" +
            "3. Cultural evolution\n" +
            "4. Future activities",
            event.getType(),
            event.getDescription(),
            event.getCulture(),
            event.getParticipants().size()
        );

        LLMService.getInstance()
            .generateResponse(prompt)
            .thenAccept(effects -> applyEventEffects(event, effects, world));
    }

    private void applyEventEffects(VillageEvent event, String effects, ServerWorld world) {
        VillagerManager vm = VillagerManager.getInstance();
        
        // Apply effects to participants
        event.getParticipants().forEach(uuid -> {
            VillagerAI ai = vm.getVillagerAI(uuid);
            if (ai != null) {
                ai.generateBehavior("Reflecting on " + event.getType())
                    .thenAccept(reflection -> {
                        LOGGER.fine(String.format("Villager %s reflects: %s", 
                            ai.getVillager().getName().getString(), reflection));
                        
                        // Update villager's cultural knowledge
                        ai.learnFromEvent(event, reflection);
                    });
            }
        });
    }

    private void notifyParticipants(VillageEvent event) {
        VillagerManager vm = VillagerManager.getInstance();
        
        event.getParticipants().forEach(uuid -> {
            VillagerAI ai = vm.getVillagerAI(uuid);
            if (ai != null) {
                String situation = String.format("Participating in %s: %s", 
                    event.getType(), event.getDescription());
                    
                ai.generateBehavior(situation)
                    .thenAccept(behavior -> {
                        LOGGER.fine(String.format("Villager %s behavior during event: %s", 
                            ai.getVillager().getName().getString(), behavior));
                    });
            }
        });
    }

    private BlockPos findEventLocation(ServerWorld world, List<VillagerAI> participants) {
        SpawnRegion region = VillagerManager.getInstance()
            .getSpawnRegionForVillager(participants.get(0).getVillager());
            
        if (region == null) {
            return defaultEventLocation(world, participants);
        }

        // Try to find culturally appropriate location based on event type
        Map<String, BlockPos> structures = region.getCulturalStructuresByType();
        String eventType = "gathering"; // Default type
        
        switch(region.getCultureAsString().toLowerCase()) {
            case "roman" -> {
                // For bathing or social events, prefer bathhouse
                if (eventType.toLowerCase().contains("bath") ||
                    eventType.toLowerCase().contains("social")) {
                    if (structures.containsKey("bathhouse")) {
                        return structures.get("bathhouse");
                    }
                }
                // For official events, use forum
                if (structures.containsKey("forum")) {
                    return structures.get("forum");
                }
                // Fallback to any Roman structure
                if (structures.containsKey("bathhouse")) {
                    return structures.get("bathhouse");
                }
            }
            case "egyptian" -> {
                if (structures.containsKey("temple")) {
                    return structures.get("temple");
                }
            }
            case "victorian" -> {
                if (structures.containsKey("town_square")) {
                    return structures.get("town_square");
                }
            }
            case "nyc" -> {
                if (structures.containsKey("plaza")) {
                    return structures.get("plaza");
                }
            }
        }

        // Fallback to default location finding
        return defaultEventLocation(world, participants);
    }

    private BlockPos defaultEventLocation(ServerWorld world, List<VillagerAI> participants) {
        // Find central location among participants
        double avgX = participants.stream()
            .mapToDouble(ai -> ai.getVillager().getX())
            .average()
            .orElse(0);
            
        double avgZ = participants.stream()
            .mapToDouble(ai -> ai.getVillager().getZ())
            .average()
            .orElse(0);
            
        int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, (int)avgX, (int)avgZ);
        return new BlockPos((int)avgX, y, (int)avgZ);
    }

    private List<VillagerAI> getVillagersInRegion(SpawnRegion region) {
        return VillagerManager.getInstance().getActiveVillagers().stream()
            .filter(ai -> {
                BlockPos pos = ai.getVillager().getBlockPos();
                return region.isWithinRegion(pos) &&
                       isAppropriateEventParticipant(ai, region.getCultureAsString());
            })
            .collect(Collectors.toList());
    }

    private boolean isAppropriateEventParticipant(VillagerAI ai, String culture) {
        // Check if villager is already participating in an event
        if (activeEvents.values().stream()
            .flatMap(List::stream)
            .anyMatch(event -> event.getParticipants().contains(ai.getVillager().getUuid()))) {
            return false;
        }

        // Check if villager has participated in too many recent events
        long recentEventCount = historicalEvents.getOrDefault(culture, Collections.emptyList())
            .stream()
            .filter(event -> event.getParticipants().contains(ai.getVillager().getUuid()))
            .count();

        return recentEventCount < 3; // Limit participation to prevent event fatigue
    }

    private String getRecentEvents(String culture) {
        return historicalEvents.getOrDefault(culture, Collections.emptyList()).stream()
            .skip(Math.max(0, historicalEvents.size() - 3))
            .map(VillageEvent::getType)
            .collect(Collectors.joining(", "));
    }

    private Map<String, String> parseEventResponse(String response) {
        return Arrays.stream(response.split("\n"))
            .map(line -> line.split(":", 2))
            .filter(parts -> parts.length == 2)
            .collect(Collectors.toMap(
                parts -> parts[0].trim(),
                parts -> parts[1].trim()
            ));
    }

    public List<VillageEvent> getActiveEvents(String culture) {
        return activeEvents.getOrDefault(culture, Collections.emptyList());
    }

    public List<VillageEvent> getHistoricalEvents(String culture) {
        return historicalEvents.getOrDefault(culture, Collections.emptyList());
    }

    public void clear() {
        activeEvents.clear();
        historicalEvents.clear();
    }
}
