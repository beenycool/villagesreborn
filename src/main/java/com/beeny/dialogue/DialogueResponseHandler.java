package com.beeny.dialogue;

import com.beeny.config.VillagersRebornConfig;
import com.beeny.constants.StringConstants;
import com.beeny.system.VillagerDialogueSystem;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class DialogueResponseHandler {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DialogueResponseHandler.class);

    private DialogueResponseHandler() {}

    static @Nullable Text onSuccess(@NotNull String cacheKey,
                          @Nullable String dialogue,
                          @NotNull VillagerDialogueSystem.DialogueContext context,
                          @NotNull VillagerDialogueSystem.DialogueCategory category) {
        if (dialogue == null || dialogue.trim().isEmpty()) {
            return null; // triggers static fallback in caller
        }

        long ttl = DialogueCache.getCategorySpecificTTL(category);
        DialogueCache.put(cacheKey, dialogue, ttl);

        VillagerMemoryManager.addVillagerResponse(context.villager, context.player, dialogue, category.name());
        context.villagerData.incrementTopicFrequency(category.name());
        context.villagerData.updateLastConversationTime();

        return Text.literal(dialogue).formatted(determineFormatting(context));
    }

    static @NotNull Text onFailure(@Nullable Throwable throwable) {
        logger.warn("[WARN] [DialogueResponseHandler] [onFailure] - LLM dialogue generation failed: {}", throwable != null ? throwable.getMessage() : "unknown");
        if (VillagersRebornConfig.FALLBACK_TO_STATIC) {
            return null;
        }
        return Text.literal(StringConstants.MSG_LLM_TROUBLE).formatted(Formatting.GRAY);
    }

    static @NotNull Text formatCached(@NotNull String cachedDialogue, @NotNull VillagerDialogueSystem.DialogueContext context) {
        return Text.literal(cachedDialogue).formatted(determineFormatting(context));
    }

    private static @NotNull Formatting determineFormatting(@NotNull VillagerDialogueSystem.DialogueContext context) {
        if (context.playerReputation > 50) {
            return Formatting.GREEN;
        } else if (context.playerReputation < -20) {
            return Formatting.RED;
        } else if (context.villagerData.getHappiness() > 70) {
            return Formatting.AQUA;
        } else if (context.villagerData.getHappiness() < 30) {
            return Formatting.GRAY;
        }
        return Formatting.WHITE;
    }
}