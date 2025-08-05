package com.beeny.dialogue;

import com.beeny.config.VillagersRebornConfig;
import com.beeny.constants.StringConstants;
import com.beeny.system.VillagerDialogueSystem;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class DialogueOrchestrator {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DialogueOrchestrator.class);

    private DialogueOrchestrator() {}

    static @NotNull CompletableFuture<Text> generateDialogueAsync(@NotNull VillagerDialogueSystem.DialogueContext context,
                                                           @NotNull VillagerDialogueSystem.DialogueCategory category) {
        if (!VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE) {
            return CompletableFuture.completedFuture(null);
        }

        // Ensure provider ready
        DialogueProviderFactory.initialize();
        LLMDialogueProvider provider = DialogueProviderFactory.getProvider();

        if (provider == null || !provider.isConfigured()) {
            if (VillagersRebornConfig.FALLBACK_TO_STATIC) {
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.completedFuture(
                net.minecraft.text.Text.literal(StringConstants.MSG_LLM_NOT_CONFIGURED)
                    .formatted(net.minecraft.util.Formatting.GRAY)
            );
        }

        try {
            String conversationHistory = VillagerMemoryManager.getRecentConversationContext(context.villager, context.player, 3);
            String cacheKey = DialogueCache.generateCacheKey(context, category, conversationHistory);

            String cachedDialogue = DialogueCache.get(cacheKey);
            if (cachedDialogue != null) {
                return CompletableFuture.completedFuture(DialogueResponseHandler.formatCached(cachedDialogue, context));
            }

            String prompt = DialoguePromptBuilder.buildContextualPrompt(context, category, conversationHistory);
            LLMDialogueProvider.DialogueRequest request =
                new LLMDialogueProvider.DialogueRequest(context, category, conversationHistory, prompt);

            return provider.generateDialogue(request)
                .orTimeout(VillagersRebornConfig.LLM_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                .thenApply(dialogue -> DialogueResponseHandler.onSuccess(cacheKey, dialogue, context, category))
                .exceptionally(DialogueResponseHandler::onFailure);
        } catch (Exception e) {
            logger.error("[ERROR] [DialogueOrchestrator] [generateDialogueAsync] - Error in LLM dialogue generation", e);
            return CompletableFuture.completedFuture(null);
        }
    }
}