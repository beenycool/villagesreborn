package com.beeny.village;

import com.beeny.ai.LLMService;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class VillagerAI {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private static final int DEFAULT_TIMEOUT_SECONDS = 5;
    private static final Map<String, String> behaviorCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;
    
    private final VillagerEntity villager;
    private final String personality;
    private BlockPos homePos;
    private String currentActivity;

    public VillagerAI(VillagerEntity villager, String personality) {
        this.villager = villager;
        this.personality = personality;
        this.homePos = villager.getBlockPos();
        this.currentActivity = "idle";
    }

    public CompletableFuture<String> generateBehavior(String situation) {
        return generateBehavior(situation, DEFAULT_TIMEOUT_SECONDS);
    }

    public CompletableFuture<String> generateBehavior(String situation, int timeoutSeconds) {
        String cacheKey = personality + "|" + situation;
        String cachedBehavior = behaviorCache.get(cacheKey);
        if (cachedBehavior != null) {
            LOGGER.debug("Using cached behavior for {}: {}", villager.getName().getString(), cachedBehavior);
            return CompletableFuture.completedFuture(cachedBehavior);
        }

        String prompt = String.format(
            "As a Minecraft villager with a %s personality, what would you do in this situation: %s? " +
            "Respond with a brief action description in 10 words or less.",
            personality, situation
        );

        return LLMService.getInstance()
            .generateResponse(prompt)
            .thenApply(response -> {
                String behavior = response.trim();
                LOGGER.debug("Generated behavior for {}: {}", villager.getName().getString(), behavior);
                
                // Cache the response if there's room
                if (behaviorCache.size() < MAX_CACHE_SIZE) {
                    behaviorCache.put(cacheKey, behavior);
                }
                
                return behavior;
            })
            .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .exceptionally(e -> {
                if (e.getCause() instanceof TimeoutException) {
                    LOGGER.warn("Behavior generation timed out after {} seconds for {}", 
                        timeoutSeconds, villager.getName().getString());
                    return "Continue previous activity";
                } else {
                    LOGGER.error("Error generating behavior for {}: {}", 
                        villager.getName().getString(), e.getMessage());
                    return "Continue current activity";
                }
            });
    }

    public void updateActivity(String newActivity) {
        this.currentActivity = newActivity;
        // Show activity as villager name suffix
        villager.setCustomName(Text.of(villager.getName().getString() + " (" + newActivity + ")"));
    }

    public String getCurrentActivity() {
        return currentActivity;
    }

    public BlockPos getHomePos() {
        return homePos;
    }

    public void setHomePos(BlockPos pos) {
        this.homePos = pos;
    }

    public String getPersonality() {
        return personality;
    }

    public VillagerEntity getVillager() {
        return villager;
    }

    // Clear the behavior cache if it gets too large
    public static void clearBehaviorCache() {
        behaviorCache.clear();
        LOGGER.debug("Cleared villager behavior cache");
    }

    // Get the current size of the behavior cache
    public static int getBehaviorCacheSize() {
        return behaviorCache.size();
    }
}
