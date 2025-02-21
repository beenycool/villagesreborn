package com.beeny.village.event;

import com.beeny.ai.LLMService;
import com.beeny.village.VillagerAI;
import com.beeny.village.VillagerManager;
import com.beeny.village.SpawnRegion;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class VillageEventManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final VillageEventManager INSTANCE = new VillageEventManager();
    
    private final Map<String, List<VillageEvent>> activeEvents = new HashMap<>();
    private final Map<String, List<VillageEvent>> historicalEvents = new HashMap<>();
    private final Random random = new Random();

    private VillageEventManager() {}

    public static VillageEventManager getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<VillageEvent> generateRandomEvent(String culture, ServerWorld world, List<VillagerAI> participants) {
        Map<String, String> context = new HashMap<>();
        context.put("culture", culture);
        context.put("time_of_day", String.valueOf(world.getTimeOfDay()));
        context.put("participant_count", String.valueOf(participants.size()));
        context.put("recent_events", getRecentEvents(culture));
        
        String prompt = String.format(
            "Generate a spontaneous cultural event for a %s village in Minecraft.\n" +
            "Current context:\n" +
            "- Time of day: %d\n" +
            "- Participants: %d villagers\n" +
            "- Recent events: %s\n\n" +
            "Create an event that:\n" +
            "1. Fits the culture and time of day\n" +
            "2. Involves available villagers naturally\n" +
            "3. Has meaningful impact on village life\n" +
            "4. Creates memorable moments\n\n" +
            "Format response as:\n" +
            "TYPE: (celebration/gathering/ritual/market/etc)\n" +
            "DESCRIPTION: (brief event description)\n" +
            "DURATION: (time in ticks)\n" +
            "OUTCOME: (effect on village)",
            culture,
            world.getTimeOfDay(),
            participants.size(),
            getRecentEvents(culture)
        );

        return LLMService.getInstance()
            .generateResponse(prompt, context)
            .thenApply(response -> {
                Map<String, String> eventDetails = parseEventResponse(response);
                return new VillageEvent.Builder()
                    .type(eventDetails.get("TYPE"))
                    .description(eventDetails.get("DESCRIPTION"))
                    .duration(Long.parseLong(eventDetails.getOrDefault("DURATION", "12000")))
                    .startTime(world.getTime())
                    .participants(participants.stream()
                        .map(ai -> ai.getVillager().getUuid())
                        .collect(Collectors.toList()))
                    .culture(culture)
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
                if (!villagers.isEmpty()) {
                    generateRandomEvent(region.getCulture(), world, villagers)
                        .thenAccept(this::startEvent);
                }
            });
        }
    }

    private void completeEvent(VillageEvent event, ServerWorld world) {
        LOGGER.info("Completing event: {} in {} culture", event.getType(), event.getCulture());
        
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
                        LOGGER.debug("Villager {} reflects: {}", 
                            ai.getVillager().getName().getString(), reflection);
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
                        LOGGER.debug("Villager {} behavior during event: {}", 
                            ai.getVillager().getName().getString(), behavior);
                    });
            }
        });
    }

    private BlockPos findEventLocation(ServerWorld world, List<VillagerAI> participants) {
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
            .filter(ai -> region.isWithinRegion(ai.getVillager().getBlockPos()))
            .collect(Collectors.toList());
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
