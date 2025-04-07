package com.beeny.village;

import com.beeny.ai.CulturalPromptTemplates;
import com.beeny.ai.LLMService;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class VillagerDialogue {
    private final VillagerEntity villager;
    private final VillagerMemory memory;
    private final LLMService llmService;
    private String cultureType;
    private String profession;
    private Map<String, String> dialogueCache;

    public VillagerDialogue(VillagerEntity villager, VillagerMemory memory, String cultureType, String profession) {
        this.villager = villager;
        this.memory = memory;
        this.llmService = LLMService.getInstance();
        this.cultureType = cultureType;
        this.profession = profession;
        this.dialogueCache = new HashMap<>();
    }

    public CompletableFuture<String> generateBackstory() {
        var context = new HashMap<String, String>();
        context.put("culture", cultureType);
        context.put("profession", profession);
        context.put("personality", String.join(", ", 
            memory.getDominantTraits().stream()
                .map(trait -> trait.name().toLowerCase())
                .toList()
        ));
        context.put("skills", memory.getKnownRecipes().toString());
        context.put("emotional_state", memory.getEmotionalState());

        String promptKey = "backstory_" + villager.getUuid();
        if (dialogueCache.containsKey(promptKey)) {
            return CompletableFuture.completedFuture(dialogueCache.get(promptKey));
        }

        String prompt = String.format(
            "Generate a culturally authentic backstory for a %s %s villager.\n" +
            "Personality traits: %s\n" +
            "Skills: %s\n" +
            "Current emotional state: %s\n\n" +
            "Format as a brief narrative that explains their origins, skills development, " +
            "and what brought them to their current role. Keep it concise but meaningful.",
            cultureType, profession, context.get("personality"),
            context.get("skills"), context.get("emotional_state")
        );

        return llmService.generateResponse(prompt, context)
            .thenApply(response -> {
                dialogueCache.put(promptKey, response);
                return response;
            });
    }

    public CompletableFuture<String> generatePlayerInteraction(PlayerEntity player, String interactionType) {
        var relevantMemories = memory.getMemoriesInvolvingPlayer(player.getUuid());
        float relationship = memory.getPlayerRelationship(player.getUuid());

        var context = new HashMap<String, String>();
        context.put("player_name", player.getName().getString());
        context.put("relationship_level", String.format("%.2f", relationship));
        context.put("emotional_state", memory.getEmotionalState());
        context.put("recent_memories", relevantMemories.stream()
            .limit(3)
            .map(VillagerMemory.Memory::getDescription)
            .reduce((a, b) -> a + "; " + b)
            .orElse("None"));

        String prompt = CulturalPromptTemplates.getTemplate("villager_interaction",
            villager.getName().getString(),
            profession,
            memory.getEmotionalState(),
            player.getName().getString(),
            "player",
            "unknown",
            interactionType,
            "current",
            context.get("recent_memories"),
            cultureType,
            "clear",
            "trading"
        );

        return llmService.generateResponse(prompt, context);
    }

    public CompletableFuture<String> generateQuestDialog(String questType, Map<String, String> questContext) {
        var context = new HashMap<String, String>();
        context.put("quest_type", questType);
        context.put("culture", cultureType);
        context.put("profession", profession);
        context.put("emotional_state", memory.getEmotionalState());
        context.putAll(questContext);

        String prompt = String.format(
            "Create a culturally authentic quest dialogue for a %s %s villager.\n" +
            "Quest type: %s\n" +
            "Current emotional state: %s\n" +
            "Cultural context: %s\n\n" +
            "Format response as:\n" +
            "INTRODUCTION: (quest setup)\n" +
            "OBJECTIVES: (what needs to be done)\n" +
            "REWARDS: (what player will receive)\n" +
            "DIALOGUE: (the actual conversation)\n" +
            "FOLLOWUP: (what to say when player returns)",
            cultureType, profession, questType,
            memory.getEmotionalState(), questContext.getOrDefault("context", "standard")
        );

        return llmService.generateResponse(prompt, context);
    }

    public CompletableFuture<String> generateEventNarration(String eventType, Map<String, String> eventContext) {
        var context = new HashMap<String, String>();
        context.put("event_type", eventType);
        context.put("culture", cultureType);
        context.putAll(eventContext);

        String prompt = CulturalPromptTemplates.getTemplate("dynamic_event",
            eventType,
            cultureType,
            context.getOrDefault("recent_events", "none"),
            context.getOrDefault("participants", "various villagers"),
            context.getOrDefault("weather", "clear"),
            context.getOrDefault("resources", "standard")
        );

        return llmService.generateResponse(prompt, context);
    }

    public CompletableFuture<String> generateVillagerInteraction(VillagerEntity otherVillager, String location) {
        VillagerDialogue otherDialogue = VillagerManager.getInstance().getVillagerDialogue(otherVillager);
        if (otherDialogue == null) return CompletableFuture.completedFuture("...");

        String prompt = CulturalPromptTemplates.getTemplate("villager_interaction",
            villager.getName().getString(),
            this.profession,
            memory.getEmotionalState(),
            otherVillager.getName().getString(),
            otherDialogue.getProfession(),
            otherDialogue.getEmotionalState(),
            location,
            "current",
            "regular village activities",
            cultureType,
            "clear",
            "daily routines"
        );

        return llmService.generateResponse(prompt);
    }

    public void updateCulture(String newCulture) {
        this.cultureType = newCulture;
        dialogueCache.clear();
    }

    public void updateProfession(String newProfession) {
        this.profession = newProfession;
        dialogueCache.clear();
    }

    public String getProfession() {
        return profession;
    }

    public String getEmotionalState() {
        return memory.getEmotionalState();
    }

    public void clearCache() {
        dialogueCache.clear();
    }
}