package com.beeny.ai;

public enum ModelType {
    GPT35("gpt-3.5-turbo"),
    GPT4("gpt-4"),
    LLAMA2("llama-2"),
    CLAUDE("claude-2"),
    LOCAL("local");

    private final String id;

    ModelType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static ModelType fromString(String model) {
        for (ModelType type : values()) {
            if (type.getId().equalsIgnoreCase(model)) {
                return type;
            }
        }
        return GPT35; // Default model
    }
}