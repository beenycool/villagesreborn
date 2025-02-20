package com.beeny.village;

import com.beeny.ai.LLMService;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class VillagerAI {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    
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
                return behavior;
            })
            .exceptionally(e -> {
                LOGGER.error("Error generating behavior", e);
                return "Continue current activity";
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
}
