package com.beeny.villagesreborn.core.llm;

/**
 * Enumeration of supported LLM providers
 */
public enum LLMProvider {
    OPENAI("OpenAI", "https://api.openai.com/v1", "sk-"),
    ANTHROPIC("Anthropic", "https://api.anthropic.com/v1", "sk-ant-"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1", "sk-or-"),
    GROQ("Groq", "https://api.groq.com/openai/v1", "gsk_"),
    LOCAL("Local", "http://localhost:11434", "");

    private final String displayName;
    private final String baseUrl;
    private final String keyPrefix;

    LLMProvider(String displayName, String baseUrl, String keyPrefix) {
        this.displayName = displayName;
        this.baseUrl = baseUrl;
        this.keyPrefix = keyPrefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    /**
     * Check if this provider requires an API key
     */
    public boolean requiresApiKey() {
        return this != LOCAL;
    }

    /**
     * Check if this provider supports dynamic model fetching
     */
    public boolean supportsDynamicModels() {
        return this == OPENROUTER;
    }
}