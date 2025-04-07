package com.beeny.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List; // Added import
import java.util.ArrayList; // Added import

public enum ModelType {
    // Default cheapest options first for each provider
    // Model Definitions with Provider Association
    GPT35("gpt-3.5-turbo", "OPENAI"),
    CLAUDE_3_HAIKU("claude-3-haiku", "ANTHROPIC"),
    GEMINI_2_FLASH_LITE("gemini-2.0-flash-lite", "GEMINI"),
    DEEPSEEK_CODER("deepseek-coder", "DEEPSEEK"),
    MISTRAL_SMALL("mistral-small", "MISTRAL"),
    QWEN2_0_5B("qwen2-0.5b", "LOCAL"), // Added Qwen2 0.5B as a local model option
    
    // Other models
    GPT4("gpt-4", "OPENAI"),
    GPT4_TURBO("gpt-4-turbo", "OPENAI"),
    CLAUDE_3_OPUS("claude-3-opus", "ANTHROPIC"),
    CLAUDE_3_SONNET("claude-3-sonnet", "ANTHROPIC"),
    GEMINI_2_FLASH("gemini-2.0-flash", "GEMINI"),
    GEMINI_2_PRO("gemini-2.0-pro", "GEMINI"),
    // LLAMA2 might be considered local or potentially via specific providers if supported
    // For now, let's keep it separate or assign to LOCAL if it's only meant for local use.
    LLAMA2("llama2", "LOCAL"), // Uncommented and assigned to LOCAL provider
    LOCAL("local", "LOCAL"), // Explicitly local

    // OpenRouter models - Assign to OpenRouter provider
    OPENROUTER_COMMAND_R("openrouter/command-r", "OPENROUTER"),
    OPENROUTER_SOLAR("openrouter/solar", "OPENROUTER"),
    OPENROUTER_NEURAL("openrouter/neural-chat", "OPENROUTER");
    // Note: AZURE and COHERE providers from VillagesConfig don't have specific models listed here yet.
    // They might use OpenAI models (Azure) or have their own model names not in this enum.

    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private final String id;
    private final String provider; // Added provider field

    ModelType(String id, String provider) {
        this.id = id;
        this.provider = provider;
    }

    public String getId() {
        return id;
    }
    public String getProvider() {
        return provider;
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
        if (normalizedModel.startsWith("gemini-2.0-flash-lite")) return GEMINI_2_FLASH_LITE;
        if (normalizedModel.startsWith("gemini-2.0-flash")) return GEMINI_2_FLASH;
        if (normalizedModel.startsWith("gemini-2.0")) return GEMINI_2_PRO;
        if (normalizedModel.startsWith("deepseek")) return DEEPSEEK_CODER;
        if (normalizedModel.startsWith("mistral-small")) return MISTRAL_SMALL;
        if (normalizedModel.startsWith("llama")) return LLAMA2;
        if (normalizedModel.startsWith("openrouter/command-r")) return OPENROUTER_COMMAND_R;
        if (normalizedModel.startsWith("openrouter/solar")) return OPENROUTER_SOLAR;
        if (normalizedModel.startsWith("openrouter/neural")) return OPENROUTER_NEURAL;

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
/**
 * Gets a list of model IDs suitable for the given provider name.
 * @param providerName The name of the provider (e.g., "GEMINI", "OPENAI"). Case-insensitive.
 * @return An array of model ID strings.
 */
public static String[] getModelIdsForProvider(String providerName) {
    if (providerName == null || providerName.trim().isEmpty()) {
        return new String[0]; // Return empty if no provider specified
    }
    String normalizedProvider = providerName.trim().toUpperCase();
    List<String> modelIds = new ArrayList<>();
    for (ModelType type : values()) {
        // Match provider name, handling potential nulls just in case
        if (type.provider != null && type.provider.equalsIgnoreCase(normalizedProvider)) {
             // Special case: Don't include the generic "local" model unless the provider IS "LOCAL"
             if (!type.getId().equals("local") || normalizedProvider.equals("LOCAL")) {
                modelIds.add(type.getId());
             }
        }
    }
    // Add "local" option specifically if the provider allows it (or handle separately in UI)
    // For now, only add if provider IS "LOCAL"
    // if (normalizedProvider.equals("LOCAL")) {
    //     modelIds.add(LOCAL.getId());
    // }

    return modelIds.toArray(new String[0]);
}


public boolean isLocalModel() {
    return "LOCAL".equalsIgnoreCase(this.provider);
}

public boolean requiresApiKey() {
    return !"LOCAL".equalsIgnoreCase(this.provider);
}

    public int getDefaultContextLength() {
        return switch (this) {
            case GPT35 -> 4096;
            case GPT4 -> 8192;
            case GPT4_TURBO -> 128000;
            case CLAUDE_3_OPUS -> 200000;
            case CLAUDE_3_SONNET -> 150000;
            case CLAUDE_3_HAIKU -> 100000;
            case GEMINI_2_FLASH_LITE -> 32000;
            case GEMINI_2_FLASH -> 128000;
            case GEMINI_2_PRO -> 128000;
            case DEEPSEEK_CODER -> 8192;
            case MISTRAL_SMALL -> 4096; // Check actual context
            case QWEN2_0_5B -> 32768; // Qwen2 0.5B has 32k context
            case LLAMA2 -> 4096;
            case OPENROUTER_COMMAND_R -> 4096;
            case OPENROUTER_SOLAR -> 8192;
            case OPENROUTER_NEURAL -> 4096;
            case LOCAL -> 2048;
        };
    }

    public double getDefaultTemperature() {
        return switch (this) {
            case GPT35, GPT4, GPT4_TURBO -> 0.7;
            case CLAUDE_3_OPUS, CLAUDE_3_SONNET, CLAUDE_3_HAIKU -> 0.7;
            case GEMINI_2_FLASH_LITE, GEMINI_2_FLASH, GEMINI_2_PRO -> 0.7;
            case DEEPSEEK_CODER -> 0.6;
            case MISTRAL_SMALL -> 0.7;
            case QWEN2_0_5B -> 0.7; // Default temperature
            case LLAMA2 -> 0.8;
            case OPENROUTER_COMMAND_R, OPENROUTER_SOLAR, OPENROUTER_NEURAL -> 0.7;
            case LOCAL -> 0.5;
        };
    }
} // Moved closing brace here
