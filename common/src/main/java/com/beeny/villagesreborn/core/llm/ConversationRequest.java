package com.beeny.villagesreborn.core.llm;

import java.time.Duration;

/**
 * Request object for LLM conversation generation
 */
public class ConversationRequest {
    private final String prompt;
    private final int maxTokens;
    private final float temperature;
    private final Duration timeout;
    private final LLMProvider provider;
    private final String apiKey;

    private ConversationRequest(Builder builder) {
        this.prompt = builder.prompt;
        this.maxTokens = builder.maxTokens;
        this.temperature = builder.temperature;
        this.timeout = builder.timeout;
        this.provider = builder.provider;
        this.apiKey = builder.apiKey;
    }

    public String getPrompt() { return prompt; }
    public int getMaxTokens() { return maxTokens; }
    public float getTemperature() { return temperature; }
    public Duration getTimeout() { return timeout; }
    public LLMProvider getProvider() { return provider; }
    public String getApiKey() { return apiKey; }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
            .prompt(prompt)
            .maxTokens(maxTokens)
            .temperature(temperature)
            .timeout(timeout)
            .provider(provider)
            .apiKey(apiKey);
    }

    public static class Builder {
        private String prompt;
        private int maxTokens = 100;
        private float temperature = 0.7f;
        private Duration timeout = Duration.ofSeconds(30);
        private LLMProvider provider;
        private String apiKey;

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder provider(LLMProvider provider) {
            this.provider = provider;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public ConversationRequest build() {
            return new ConversationRequest(this);
        }
    }
}