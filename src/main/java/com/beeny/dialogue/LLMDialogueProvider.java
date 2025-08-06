package com.beeny.dialogue;

import com.beeny.system.VillagerDialogueSystem;
import net.minecraft.text.Text;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LLMDialogueProvider {
    
    @NotNull CompletableFuture<String> generateDialogue(@NotNull DialogueRequest request);
    
    boolean isConfigured();
    
    @NotNull String getProviderName();
    
    void shutdown();
    
    static class DialogueRequest {
        public final @NotNull VillagerDialogueSystem.DialogueContext context;
        public final @NotNull VillagerDialogueSystem.DialogueCategory category;
        public final @Nullable String conversationHistory;
        public final @NotNull String prompt;
        
        public DialogueRequest(@NotNull VillagerDialogueSystem.DialogueContext context,
                             @NotNull VillagerDialogueSystem.DialogueCategory category,
                             @Nullable String conversationHistory,
                             @NotNull String prompt) {
            this.context = context;
            this.category = category;
            this.conversationHistory = conversationHistory;
            this.prompt = prompt;
        }
    }
    
    static class DialogueResponse {
        public final @Nullable String text;
        public final boolean success;
        public final @Nullable String error;
        public final long responseTime;
        
        public DialogueResponse(@Nullable String text, boolean success, @Nullable String error, long responseTime) {
            this.text = text;
            this.success = success;
            this.error = error;
            this.responseTime = responseTime;
        }
        
        public static @NotNull DialogueResponse success(@NotNull String text, long responseTime) {
            return new DialogueResponse(text, true, null, responseTime);
        }
        
        public static @NotNull DialogueResponse error(@NotNull String error, long responseTime) {
            return new DialogueResponse(null, false, error, responseTime);
        }
    }
}