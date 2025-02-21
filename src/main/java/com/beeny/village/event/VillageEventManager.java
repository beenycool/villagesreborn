package com.beeny.village.event;

import com.beeny.ai.LLMService;
import com.beeny.village.VillagerAI;
import com.beeny.village.VillagerManager;
import com.beeny.village.SpawnRegion;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelStorage.Session;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess.LevelStorageAccessException.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType.LevelStorageAccessExceptionType;
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
