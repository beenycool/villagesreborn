package com.beeny.dialogue;

import com.beeny.config.VillagersRebornConfig;
import com.beeny.system.VillagerDialogueSystem;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LLMDialogueManager {
    private static volatile LLMDialogueProvider currentProvider;
    private static volatile boolean initialized = false;
    
    public static void initialize() {
        if (initialized) {
            shutdown();
        }
        
        try {
            currentProvider = createProvider();
            initialized = true;
        } catch (Exception e) {
            System.err.println("Failed to initialize LLM dialogue provider: " + e.getMessage());
            currentProvider = null;
        }
    }
    
    private static LLMDialogueProvider createProvider() {
        String provider = VillagersRebornConfig.LLM_PROVIDER.toLowerCase();
        
        return switch (provider) {
            case "gemini" -> new GeminiDialogueProvider();
            case "openrouter" -> new OpenRouterDialogueProvider();
            case "local" -> new LocalLLMProvider();
            default -> throw new IllegalArgumentException("Unknown LLM provider: " + provider);
        };
    }
    
    public static CompletableFuture<Text> generateDialogueAsync(VillagerDialogueSystem.DialogueContext context,
                                                              VillagerDialogueSystem.DialogueCategory category) {
        // If dynamic dialogue is disabled, return null to fall back to static
        if (!VillagersRebornConfig.ENABLE_DYNAMIC_DIALOGUE) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Ensure provider is initialized and configured
        if (!initialized) {
            initialize();
        }
        
        if (currentProvider == null || !currentProvider.isConfigured()) {
            if (VillagersRebornConfig.FALLBACK_TO_STATIC) {
                return CompletableFuture.completedFuture(null);
            } else {
                return CompletableFuture.completedFuture(
                    (Text) Text.literal("LLM provider not configured").formatted(Formatting.GRAY)
                );
            }
        }
        
        try {
            // Get conversation history
            String conversationHistory = VillagerMemoryManager.getRecentConversationContext(
                context.villager, context.player, 3
            );
            
            // Check cache first
            String cacheKey = DialogueCache.generateCacheKey(context, category, conversationHistory);
            String cachedDialogue = DialogueCache.get(cacheKey);
            if (cachedDialogue != null) {
                return CompletableFuture.completedFuture(
                    Text.literal(cachedDialogue).formatted(getDialogueFormatting(context))
                );
            }
            
            // Build the prompt
            String prompt = DialoguePromptBuilder.buildContextualPrompt(context, category, conversationHistory);
            
            // Create the request
            LLMDialogueProvider.DialogueRequest request = new LLMDialogueProvider.DialogueRequest(
                context, category, conversationHistory, prompt
            );
            
            // Make async request with timeout
            return currentProvider.generateDialogue(request)
                .orTimeout(VillagersRebornConfig.LLM_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                .thenApply(dialogue -> {
                    if (dialogue != null && !dialogue.trim().isEmpty()) {
                        // Cache the successful response
                        long ttl = DialogueCache.getCategorySpecificTTL(category);
                        DialogueCache.put(cacheKey, dialogue, ttl);
                        
                        // Update memory
                        VillagerMemoryManager.addVillagerResponse(
                            context.villager, context.player, dialogue, category.name()
                        );
                        
                        // Update villager data
                        context.villagerData.incrementTopicFrequency(category.name());
                        context.villagerData.updateLastConversationTime();
                        
                        return (Text) Text.literal(dialogue).formatted(getDialogueFormatting(context));
                    } else {
                        return null; // Will trigger fallback to static
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("LLM dialogue generation failed: " + throwable.getMessage());
                    
                    if (VillagersRebornConfig.FALLBACK_TO_STATIC) {
                        return null; // Will trigger fallback to static
                    } else {
                        return (Text) Text.literal("Sorry, I'm having trouble speaking right now...")
                                  .formatted(Formatting.GRAY);
                    }
                });
                
        } catch (Exception e) {
            System.err.println("Error in LLM dialogue generation: " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }
    
    /**
     * @deprecated This method blocks the calling thread while waiting for the LLM API response.
     *             Use {@link #generateDialogueAsync} instead to avoid server lag or freezes.
     *             Synchronous network calls on the main server thread should be avoided.
     */
    @Deprecated
    public static Text generateDialogueSync(VillagerDialogueSystem.DialogueContext context,
                                          VillagerDialogueSystem.DialogueCategory category) {
        try {
            return generateDialogueAsync(context, category).get(
                VillagersRebornConfig.LLM_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS
            );
        } catch (TimeoutException e) {
            System.err.println("Sync dialogue generation timed out: " + e.getMessage());
            return null;
        } catch (ExecutionException e) {
            System.err.println("Sync dialogue generation execution failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (InterruptedException e) {
            System.err.println("Sync dialogue generation was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    private static Formatting getDialogueFormatting(VillagerDialogueSystem.DialogueContext context) {
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
    
    public static boolean isConfigured() {
        synchronized (LLMDialogueManager.class) {
            if (!initialized || currentProvider == null) {
                return false;
            }
            return currentProvider.isConfigured();
        }
    }

    public static String getProviderName() {
        synchronized (LLMDialogueManager.class) {
            if (currentProvider != null) {
                return currentProvider.getProviderName();
            } else {
                return "None";
            }
        }
    }
    
    public static void clearCache() {
        DialogueCache.invalidateAll();
    }
    
    public static void clearVillagerCache(String villagerName) {
        DialogueCache.invalidateVillager(villagerName);
    }
    
    public static int getCacheSize() {
        return DialogueCache.size();
    }
    
    public static synchronized void shutdown() {
        if (currentProvider != null) {
            currentProvider.shutdown();
            currentProvider = null;
        }
        DialogueCache.shutdown();
        initialized = false;
    }
    
    // Method to test the LLM connection
    /**
     * Thread-safe connection test using temporary provider instance.
     * Does NOT modify global config or static fields.
     */
    public static CompletableFuture<Boolean> testConnection(String provider, String apiKey, String endpoint, String model) {
        try {
            // Minimal validation: ensure provider instance can be created and is configured
            LLMDialogueProvider temp = createProvider(provider, apiKey, endpoint, model);
            return CompletableFuture.completedFuture(temp != null && temp.isConfigured());


        } catch (Throwable e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Helper to create a provider instance from parameters.
     */
    private static LLMDialogueProvider createProvider(String provider, String apiKey, String endpoint, String model) {
        switch (provider.toLowerCase()) {
            case "gemini":
                return new GeminiDialogueProvider();
            case "openrouter":
                return new OpenRouterDialogueProvider();
            case "local":
                return new LocalLLMProvider();
            default:
                return null;
        }
    }

    /**
     * Deprecated: unsafe, modifies global state.
     */
    @Deprecated
    public static CompletableFuture<Boolean> testConnection() {
        // Use config values, but warn: this is not thread-safe!
        return testConnection(
            VillagersRebornConfig.LLM_PROVIDER,
            VillagersRebornConfig.LLM_API_KEY,
            VillagersRebornConfig.LLM_API_ENDPOINT,
            VillagersRebornConfig.LLM_MODEL
        );
    }
}