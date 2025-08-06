package com.beeny.dialogue;

import com.beeny.config.VillagersRebornConfig;
import com.beeny.constants.StringConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DialogueProviderFactory {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DialogueProviderFactory.class);

    private static volatile LLMDialogueProvider currentProvider;
    private static volatile boolean initialized = false;

    private DialogueProviderFactory() {}

    // Lifecycle
    public static synchronized void initialize() {
        initializeProvider();
    }

    public static synchronized void shutdown() {
        if (currentProvider != null) {
            currentProvider.shutdown();
            currentProvider = null;
        }
        initialized = false;
    }

    public static synchronized boolean isConfigured() {
        return initialized && currentProvider != null && currentProvider.isConfigured();
    }

    public static synchronized @NotNull String getProviderName() {
        return (currentProvider != null) ? currentProvider.getProviderName() : StringConstants.NONE;
    }

    public static synchronized LLMDialogueProvider getProvider() {
        ensureInitialized();
        return currentProvider;
    }

    // Initialization
    private static void ensureInitialized() {
        initializeProvider();
    }

    private static synchronized void initializeProvider() {
        if (initialized) {
            shutdown();
        }
        try {
            currentProvider = selectProviderFromConfig();
            initialized = true;
        } catch (Exception e) {
            logger.error("[ERROR] [DialogueProviderFactory] [initializeProvider] - Failed to initialize LLM dialogue provider", e);
            currentProvider = null;
            initialized = false;
        }
    }

    private static @NotNull LLMDialogueProvider selectProviderFromConfig() {
        String provider = VillagersRebornConfig.LLM_PROVIDER.toLowerCase();
        return switch (provider) {
            case StringConstants.PROVIDER_GEMINI_ID -> new GeminiDialogueProvider();
            case StringConstants.PROVIDER_OPENROUTER_ID -> new OpenRouterDialogueProvider();
            case StringConstants.PROVIDER_LOCAL_ID -> new LocalLLMProvider();
            default -> throw new IllegalArgumentException("Unknown LLM provider: " + provider);
        };
    }

    // Public factory used by tests/commands
    public static @Nullable LLMDialogueProvider createProvider(@Nullable String provider, @Nullable String apiKey, @Nullable String endpoint, @Nullable String model) {
        if (provider == null) return null;
        return switch (provider.toLowerCase()) {
            case StringConstants.PROVIDER_GEMINI_ID -> new GeminiDialogueProvider(apiKey, endpoint, model);
            case StringConstants.PROVIDER_OPENROUTER_ID -> new OpenRouterDialogueProvider();
            case StringConstants.PROVIDER_LOCAL_ID -> createLocalProvider(endpoint, model);
            default -> null;
        };
    }

    // Local provider helper (moved intact from manager)
    private static @NotNull LLMDialogueProvider createLocalProvider(@Nullable String endpoint, @Nullable String model) {
        final boolean hasOverrides = (endpoint != null && !endpoint.isEmpty()) || (model != null && !model.isEmpty());
        if (!hasOverrides) {
            return new LocalLLMProvider();
        }
        final String ep = (endpoint != null && !endpoint.isEmpty())
            ? endpoint
            : VillagersRebornConfig.LLM_LOCAL_ENDPOINT;
        final String mdl = (model != null && !model.isEmpty())
            ? model
            : VillagersRebornConfig.LLM_MODEL;

        return new BaseLLMProvider("", ep, mdl) {
            private final okhttp3.MediaType JSON = okhttp3.MediaType.get("application/json; charset=utf-8");

            @Override public String getProviderName() { return StringConstants.PROVIDER_LOCAL_ID; }

            @Override protected okhttp3.Request buildRequest(DialogueRequest request) {
                String prompt = DialoguePromptBuilder.buildContextualPrompt(
                    request.context, request.category, request.conversationHistory
                );
                com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                payload.addProperty("prompt", prompt);
                payload.addProperty("temperature", VillagersRebornConfig.LLM_TEMPERATURE);
                payload.addProperty("max_tokens", VillagersRebornConfig.LLM_MAX_TOKENS);
                payload.addProperty("repeat_penalty", 1.1);
                payload.addProperty("stream", false);
                com.google.gson.JsonArray stop = new com.google.gson.JsonArray();
                stop.add("\n\n"); stop.add("Player:"); stop.add("Villager:");
                payload.add("stop", stop);
                return new okhttp3.Request.Builder()
                    .url(this.endpoint)
                    .post(okhttp3.RequestBody.create(new com.google.gson.Gson().toJson(payload), JSON))
                    .addHeader("Content-Type", "application/json")
                    .build();
            }

            @Override protected String parseResponse(String responseBody) {
                com.google.gson.JsonObject responseJson = new com.google.gson.Gson().fromJson(responseBody, com.google.gson.JsonObject.class);
                if (responseJson.has("choices") && responseJson.getAsJsonArray("choices").size() > 0) {
                    com.google.gson.JsonObject choice = responseJson.getAsJsonArray("choices").get(0).getAsJsonObject();
                    if (choice.has("text")) return clean(choice.get("text").getAsString());
                }
                if (responseJson.has("content")) return clean(responseJson.get("content").getAsString());
                if (responseJson.has("data") && responseJson.getAsJsonArray("data").size() > 0) {
                    com.google.gson.JsonObject data = responseJson.getAsJsonArray("data").get(0).getAsJsonObject();
                    if (data.has("text")) return clean(data.get("text").getAsString());
                }
                throw new IllegalStateException("Unexpected response format from local LLM");
            }

            private @NotNull String clean(@Nullable String s) {
                if (s == null) return "";
                s = s.trim();
                if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) s = s.substring(1, s.length() - 1);
                s = s.replaceFirst("(?i)^(villager says:|villager:|response:|answer:)", "").trim();
                if (!s.isEmpty() && !s.matches(".*[.!?]$")) s += ".";
                return s;
            }

            @Override public boolean isConfigured() { return this.endpoint != null && !this.endpoint.isEmpty(); }

            @Override public void shutdown() { /* OkHttp shutdown handled by BaseLLMProvider */ }
        };
    }
}