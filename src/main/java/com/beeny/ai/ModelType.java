package com.beeny.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum ModelType {
    GPT35("gpt-3.5-turbo"),
    GPT4("gpt-4"),
    LLAMA2("llama2"),
    CLAUDE("claude-v2"),
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
        if (normalizedModel.startsWith("gpt-4")) return GPT4;
        if (normalizedModel.startsWith("llama")) return LLAMA2;
        if (normalizedModel.startsWith("claude")) return CLAUDE;

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
            case LLAMA2 -> 4096;
            case CLAUDE -> 8192;
            case LOCAL -> 2048;
        };
    }

    public double getDefaultTemperature() {
        return switch (this) {
            case GPT35, GPT4 -> 0.7;
            case LLAMA2 -> 0.8;
            case CLAUDE -> 0.7;
            case LOCAL -> 0.5;
        };
    }
}