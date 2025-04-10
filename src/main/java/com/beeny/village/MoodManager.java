package com.beeny.village;

import com.beeny.ai.LLMService;
import net.minecraft.util.math.BlockPos;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MoodManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final MoodManager INSTANCE = new MoodManager();
    private final Map<UUID, Integer> moodModifiers = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDangerTick = new ConcurrentHashMap<>();
    private final Map<UUID, MoodStats> moodStats = new ConcurrentHashMap<>();
    private static final long DANGER_EFFECT_DURATION = 24000; // One Minecraft day

    public static class MoodStats {
        public int baseHappiness = 50;
        public int socialBonus = 0;
        public int prosperityBonus = 0;
        public int safetyBonus = 0;
        public int eventBonus = 0;
    }

    private MoodManager() {}

    public static MoodManager getInstance() {
        return INSTANCE;
    }

    public void updateVillagerMood(VillagerAI villagerAI, ServerWorld world) {
        var villagerUUID = villagerAI.getVillager().getUuid();
        var stats = moodStats.computeIfAbsent(villagerUUID, k -> new MoodStats());
        var vm = VillagerManager.getInstance();

        var prompt = String.format(
            "Analyze this villager's current state and determine their mood:\n" +
            "- Personality: %s\n" +
            "- Current activity: %s\n" +
            "- Social bonds: %s\n" +
            "- Recent dangers: %s\n" +
            "- Cultural events: %s\n\n" +
            "Rate each aspect (0-100):\n" +
            "BASE_HAPPINESS:\nSOCIAL_BONUS:\nPROSPERITY_BONUS:\nSAFETY_BONUS:\nEVENT_BONUS:",
            villagerAI.getPersonality(),
            villagerAI.getCurrentActivity(),
            villagerAI.getSocialSummary(),
            getLastDangerTick(villagerUUID, world.getTime()).map(time -> String.format("Danger experienced %d ticks ago", world.getTime() - time)).orElse("No recent dangers"),
            vm.getCurrentEvents(villagerAI.getVillager().getBlockPos(), world).stream().map(e -> e.name).collect(Collectors.joining(", "))
        );

        LLMService.getInstance()
            .generateResponse(prompt)
            .thenAccept(response -> {
                var values = parseMoodResponse(response);
                
                stats.baseHappiness = values.getOrDefault("BASE_HAPPINESS", 50);
                stats.socialBonus = values.getOrDefault("SOCIAL_BONUS", 0);
                stats.prosperityBonus = values.getOrDefault("PROSPERITY_BONUS", 0);
                stats.safetyBonus = values.getOrDefault("SAFETY_BONUS", 0);
                stats.eventBonus = values.getOrDefault("EVENT_BONUS", 0);
                
                int totalMood = stats.baseHappiness + stats.socialBonus + stats.prosperityBonus + stats.safetyBonus + stats.eventBonus;
                               
                villagerAI.setHappiness(Math.max(0, Math.min(100, totalMood)));
                moodModifiers.put(villagerUUID, totalMood - stats.baseHappiness);

                // Generate mood-specific behavior if mood changed significantly
                int previousMood = villagerAI.getHappiness();
                if (Math.abs(totalMood - previousMood) > 20) {
                    var situation = String.format("Feeling %s after mood change from %d to %d", getMoodDescription(totalMood), previousMood, totalMood);
                    villagerAI.generateBehavior(situation);
                }
            })
            .exceptionally(e -> {
                LOGGER.error("Error updating villager mood", e);
                return null;
            });
    }

    private Optional<Long> getLastDangerTick(UUID villagerUUID, long currentTick) {
        return Optional.ofNullable(lastDangerTick.get(villagerUUID))
            .filter(tick -> currentTick - tick <= DANGER_EFFECT_DURATION);
    }

    private Map<String, Integer> parseMoodResponse(String response) {
        return Arrays.stream(response.split("\n"))
            .map(line -> line.split(":", 2))
            .filter(parts -> parts.length == 2)
            .collect(Collectors.toMap(
                parts -> parts[0].trim(),
                parts -> {
                    try {
                        return Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
            ));
    }

    private String getMoodDescription(int mood) {
        String prompt = String.format(
            "Describe a villager's emotional state when their happiness is %d/100 in one or two words.",
            mood
        );

        try {
            return LLMService.getInstance()
                .generateResponse(prompt)
                .get(5, TimeUnit.SECONDS)
                .trim();
        } catch (Exception e) {
            return mood > 50 ? "happy" : "unhappy";
        }
    }

    public void reportDanger(UUID villagerUUID, ServerWorld world) {
        lastDangerTick.put(villagerUUID, world.getTime());
        var vm = VillagerManager.getInstance();
        var endangeredVillager = vm.getVillagerAI(villagerUUID);
        
        if (endangeredVillager != null) {
            var situation = "Reacting to immediate danger";
            endangeredVillager.generateBehavior(situation)
                .thenAccept(behavior -> {
                    LOGGER.info("Villager {} reacts to danger: {}", endangeredVillager.getVillager().getName().getString(), behavior);
                });

            // Propagate danger awareness to nearby villagers
            vm.getActiveVillagers().stream()
                .filter(ai -> ai.getVillager().squaredDistanceTo(endangeredVillager.getVillager()) < 100)
                .forEach(ai -> {
                    lastDangerTick.put(ai.getVillager().getUuid(), world.getTime());
                    
                    var nearbySituation = String.format("Reacting to danger near %s", endangeredVillager.getVillager().getName().getString());
                    ai.generateBehavior(nearbySituation);
                });
        }
    }

    public int getMoodModifier(UUID villagerUUID) {
        return moodModifiers.getOrDefault(villagerUUID, 0);
    }

    public void clearMoodModifiers() {
        moodModifiers.clear();
        lastDangerTick.clear();
        moodStats.clear();
    }
}