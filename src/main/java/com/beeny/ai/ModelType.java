package com.beeny.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum ModelType {
    GPT35("gpt-3.5-turbo"),
    GPT4("gpt-4"),
    GPT4_TURBO("gpt-4-turbo"),
    CLAUDE_3_OPUS("claude-3-opus"),
    CLAUDE_3_SONNET("claude-3-sonnet"),
    CLAUDE_3_HAIKU("claude-3-haiku"),
    LLAMA2("llama2"),
    LOCAL("local");

    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private final String id;

    ModelType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static ModelType fromString(String model) {
        if (model == null || model.trim().isEmpty()) {
            LOGGER.warn("Empty or null model type provided, defaulting to GPT35");
            return GPT35;
        }

        String normalizedModel = model.trim().toLowerCase();
        
        for (ModelType type : values()) {
            if (type.getId().equalsIgnoreCase(normalizedModel)) {
                return type;
            }
        }

        // Check for alternative names or versions
        if (normalizedModel.startsWith("gpt-3.5")) return GPT35;
        if (normalizedModel.startsWith("gpt-4-turbo")) return GPT4_TURBO;
        if (normalizedModel.startsWith("gpt-4")) return GPT4;
        if (normalizedModel.startsWith("claude-3-opus")) return CLAUDE_3_OPUS;
        if (normalizedModel.startsWith("claude-3-sonnet")) return CLAUDE_3_SONNET;
        if (normalizedModel.startsWith("claude-3-haiku")) return CLAUDE_3_HAIKU;
        if (normalizedModel.startsWith("llama")) return LLAMA2;

        LOGGER.warn("Unsupported model type: '{}', defaulting to GPT35. Supported models: {}",
            model, String.join(", ", getAllModelIds()));
        return GPT35;
    }

    public static String[] getAllModelIds() {
        ModelType[] types = values();
        String[] ids = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            ids[i] = types[i].getId();
        }
        return ids;
    }

    public boolean isLocalModel() {
        return this == LOCAL;
    }

    public boolean requiresApiKey() {
        return this != LOCAL;
    }

    public int getDefaultContextLength() {
        return switch (this) {
            case GPT35 -> 4096;
            case GPT4 -> 8192;
            case GPT4_TURBO -> 128000;
            case CLAUDE_3_OPUS -> 200000;
            case CLAUDE_3_SONNET -> 150000;
            case CLAUDE_3_HAIKU -> 100000;
            case LLAMA2 -> 4096;
            case LOCAL -> 2048;
        };
    }

    public double getDefaultTemperature() {
        return switch (this) {
            case GPT35, GPT4, GPT4_TURBO -> 0.7;
            case CLAUDE_3_OPUS, CLAUDE_3_SONNET, CLAUDE_3_HAIKU -> 0.7;
            case LLAMA2 -> 0.8;
            case LOCAL -> 0.5;
        };
    }
}
