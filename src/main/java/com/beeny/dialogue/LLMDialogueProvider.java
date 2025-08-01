package com.beeny.dialogue;

import com.beeny.system.VillagerDialogueSystem;
import net.minecraft.text.Text;
import java.util.concurrent.CompletableFuture;

public interface LLMDialogueProvider {
    
    CompletableFuture<String> generateDialogue(DialogueRequest request);
    
    boolean isConfigured();
    
    String getProviderName();
    
    void shutdown();
    
    static class DialogueRequest {
        public final VillagerDialogueSystem.DialogueContext context;
        public final VillagerDialogueSystem.DialogueCategory category;
        public final String conversationHistory;
        public final String prompt;
        
        public DialogueRequest(VillagerDialogueSystem.DialogueContext context, 
                             VillagerDialogueSystem.DialogueCategory category, 
                             String conversationHistory, 
                             String prompt) {
            this.context = context;
            this.category = category;
            this.conversationHistory = conversationHistory;
            this.prompt = prompt;
        }
    }
    
    static class DialogueResponse {
        public final String text;
        public final boolean success;
        public final String error;
        public final long responseTime;
        
        public DialogueResponse(String text, boolean success, String error, long responseTime) {
            this.text = text;
            this.success = success;
            this.error = error;
            this.responseTime = responseTime;
        }
        
        public static DialogueResponse success(String text, long responseTime) {
            return new DialogueResponse(text, true, null, responseTime);
        }
        
        public static DialogueResponse error(String error, long responseTime) {
            return new DialogueResponse(null, false, error, responseTime);
        }
    }
}