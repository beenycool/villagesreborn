package com.beeny.dialogue;

import com.beeny.system.VillagerDialogueSystem;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * LLMDialogueManager now delegates orchestration, provider lifecycle, and response
 * handling to specialized classes for improved modularity and maintainability.
 * External behavior remains unchanged.
 */
public class LLMDialogueManager {

    // =========================
    // Initialization / Provider
    // =========================
    public static synchronized void initialize() {
        DialogueProviderFactory.initialize();
    }

    // =========================
    // Public Orchestration API
    // =========================
    public static @NotNull CompletableFuture<Text> generateDialogueAsync(@NotNull VillagerDialogueSystem.DialogueContext context,
                                                                 @NotNull VillagerDialogueSystem.DialogueCategory category) {
        return DialogueOrchestrator.generateDialogueAsync(context, category);
    }

    // =========================
    // Introspection / Cache ops
    // =========================
    public static boolean isConfigured() {
        return DialogueProviderFactory.isConfigured();
    }

    public static @NotNull String getProviderName() {
        return DialogueProviderFactory.getProviderName();
    }

    public static void clearCache() {
        DialogueCache.invalidateAll();
    }

    public static void clearVillagerCache(@NotNull String villagerName) {
        DialogueCache.invalidateVillager(villagerName);
    }

    public static int getCacheSize() {
        return DialogueCache.size();
    }

    // =========================
    // Lifecycle
    // =========================
    public static synchronized void shutdown() {
        DialogueProviderFactory.shutdown();
        DialogueCache.shutdown();
    }

    /**
     * Server-side connection test using the server's stored configuration.
     * This is the secure way to test connections - no sensitive data from client.
     */
    public static @NotNull CompletableFuture<Boolean> testConnectionSecure() {
        try {
            LLMDialogueProvider temp = DialogueProviderFactory.createProvider(
                com.beeny.config.VillagersRebornConfig.LLM_PROVIDER,
                null, null, null
            );
            return java.util.concurrent.CompletableFuture.completedFuture(temp != null && temp.isConfigured());
        } catch (Throwable e) {
            org.slf4j.LoggerFactory.getLogger(LLMDialogueManager.class).error("[ERROR] [LLMDialogueManager] [testConnectionSecure] - LLM connection test failed", e);
            return java.util.concurrent.CompletableFuture.completedFuture(false);
        }
    }
}