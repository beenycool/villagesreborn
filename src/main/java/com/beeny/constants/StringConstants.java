package com.beeny.constants;

/**
 * Centralized shared string literals (keys, config names, event names, provider IDs, messages).
 * Use these constants to avoid magic strings scattered across the codebase.
 */
public final class StringConstants {

    // Mod
    public static final String MOD_ID = "villagersreborn";

    // Provider IDs / names
    public static final String PROVIDER_GEMINI_ID = "gemini";
    public static final String PROVIDER_OPENROUTER_ID = "openrouter";
    public static final String PROVIDER_LOCAL_ID = "local";
    public static final String PROVIDER_NAME_GEMINI = "Gemini";
    public static final String PROVIDER_NAME_OPENROUTER = "OpenRouter";
    public static final String PROVIDER_NAME_LOCAL = "local";

    // Environment variables
    public static final String ENV_API_KEY = "VILLAGERS_REBORN_API_KEY";

    // Default endpoints/models
    public static final String DEFAULT_GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models";
    public static final String DEFAULT_GEMINI_MODEL = "gemini-1.5-flash";
    public static final String DEFAULT_OPENROUTER_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";
    public static final String DEFAULT_OPENROUTER_MODEL = "openai/gpt-3.5-turbo";

    // Network channel / packet names
    public static final String CH_OPEN_FAMILY_TREE = "open_family_tree";
    public static final String CH_UPDATE_NOTES = "update_notes";
    public static final String CH_MARRIAGE = "marriage";
    public static final String CH_REQUEST_VILLAGER_LIST = "request_villager_list";
    public static final String CH_VILLAGER_LIST_RESPONSE = "villager_list_response";
    public static final String CH_TEST_LLM_CONNECTION = "test_llm_connection";
    public static final String CH_TEST_LLM_CONNECTION_RESULT = "test_llm_connection_result";

    // UI/Chat messages
    public static final String MSG_LLM_NOT_CONFIGURED = "LLM provider not configured";
    public static final String MSG_LLM_TROUBLE = "Sorry, I'm having trouble speaking right now...";
    public static final String MSG_INVALID_VILLAGER_ID = "§cInvalid villager ID!";
    public static final String MSG_NOT_A_VILLAGER = "§cEntity is not a villager!";
    public static final String MSG_TOO_FAR = "§cYou are too far from the villager!";
    public static final String MSG_VILLAGER_DATA_NOT_FOUND = "§cVillager data not found!";
    public static final String MSG_NOTES_UPDATED_PREFIX = "Notes updated for ";
    public static final String MSG_MARRIAGE_RATE_LIMIT_PREFIX = "Marriage request failed - please wait ";
    public static final String MSG_MARRIAGE_RATE_LIMIT_SUFFIX = " seconds before trying again";
    public static final String MSG_MARRIAGE_SAME_VILLAGER = "Marriage failed - cannot marry the same villager to themselves";
    public static final String MSG_MARRIAGE_NOT_FOUND = "Marriage failed - one or both villagers not found";
    public static final String MSG_MARRIAGE_NOT_VILLAGERS = "Marriage failed - selected entities are not villagers";
    public static final String MSG_MARRIAGE_TOO_FAR = "Marriage failed - you are too far from one or both villagers";
    public static final String MSG_MARRIAGE_SUCCESS = "Marriage successful!";
    public static final String MSG_MARRIAGE_FAILED_CONDITIONS = "Marriage failed - conditions not met";
    public static final String MSG_CONNECTION_SUCCESS = "Connection successful!";
    public static final String MSG_CONNECTION_FAILED = "Connection failed. Check server configuration.";

    // Prompt building
    public static final String PROMPT_RESPONSE_GUIDELINES_HEADER = "\nResponse guidelines:\n";
    public static final String PROMPT_GUIDE_IN_CHARACTER = "- Stay completely in character as this villager\n";
    public static final String PROMPT_GUIDE_UNDER_50 = "- Keep responses under 50 words\n";
    public static final String PROMPT_GUIDE_CONVERSATIONAL = "- Be conversational and natural\n";
    public static final String PROMPT_GUIDE_NO_ASTERISKS = "- Don't use asterisks or action descriptions\n";
    public static final String PROMPT_GUIDE_NO_AI = "- Don't break the fourth wall or mention you're an AI\n";
    public static final String PROMPT_GUIDE_MATCH_PERSONALITY = "- Match the personality traits in your speech patterns\n";
    public static final String PROMPT_GUIDE_SIMPLE_LANGUAGE = "- Use simple, villager-appropriate language\n";
    public static final String PROMPT_GUIDE_TEXT_ONLY = "- Only respond with dialogue text, nothing else\n";

    // Config keys and default/display strings (centralized UI strings)
    public static final String UI_TITLE_SETTINGS = "Villages Reborn Settings";
    public static final String UI_SIMPLE_TEST_SCREEN = "This is a test screen";
    public static final String UI_OPEN_FULL_SETTINGS = "Open Full Settings";
    public static final String UI_ERROR_PREFIX = "Error: ";
    public static final String UI_OPENING_SETTINGS = "Opening Villages Reborn settings...";
    public static final String UI_CLIENT_HELP_HEADER = "=== Villages Reborn Commands ===";
    public static final String UI_CLIENT_HELP_CLIENT = "\nClient Commands:";
    public static final String UI_CLIENT_HELP_SERVER = "\nServer Commands (when OP):";
    public static final String UI_CLIENT_HELP_MORE = "\nFor more commands, use /villager help on the server";
    public static final String UI_AI_SETTINGS = "🤖 AI Settings";
    public static final String UI_VILLAGER_SETTINGS = "👥 Villager Settings";
    public static final String UI_DYNAMIC_DIALOGUE = "Dynamic Dialogue";
    public static final String UI_PROVIDER_LABEL_FN = "Provider: ";
    public static final String UI_MODEL = "Model";
    public static final String UI_TEMPERATURE_LABEL_FN = "Temperature: ";
    public static final String UI_MAX_TOKENS_LABEL_FN = "Max Tokens: ";
    public static final String UI_FALLBACK_TO_STATIC = "Fallback to Static";
    public static final String UI_ENABLE_DIALOGUE_CACHE = "Enable Dialogue Cache";
    public static final String UI_AI_DIALOGUE_SETUP = "AI Dialogue Setup";
    public static final String UI_RESET_DEFAULTS = "Reset to Defaults";
    public static final String UI_SAVE_APPLY = "Save & Apply";
    public static final String UI_DIALOGUE_SETUP_TITLE = "Dynamic Dialogue Setup";
    public static final String UI_TEST_CONNECTION = "Test Connection";
    public static final String UI_SAVE = "Save";
    public static final String UI_CANCEL = "Cancel";
    public static final String UI_API_KEY = "API Key:";
    public static final String UI_ENDPOINT_OPTIONAL = "Custom Endpoint (Optional):";
    public static final String UI_TESTING_CONNECTION = "Testing connection...";
    public static final String UI_SETTINGS_SAVED = "✓ Settings saved!";
    public static final String UI_SETTINGS_FAILED_PREFIX = "✗ Failed to save settings: ";
    public static final String UI_CONFIGURE_AI_DIALOGUE = "Configure your AI dialogue settings below";
    public static final String UI_DYNAMIC_ENABLED = "Dynamic Dialogue: Enabled";
    public static final String UI_DYNAMIC_DISABLED = "Dynamic Dialogue: Disabled";
    public static final String UI_LLM_PROVIDER = "LLM Provider";
    public static final String UI_QUICK_SETUP = "Quick Setup";
    public static final String UI_APPLIED_PRESET_PREFIX = "Applied ";
    public static final String UI_APPLIED_PRESET_SUFFIX = " preset. Don't forget to add your API key!";
    public static final String UI_REQUIRE_API_KEY = "API key is required for this provider when dynamic dialogue is enabled";

    // Keybindings categories and keys
    public static final String KB_OPEN_JOURNAL = "key.villagersreborn.open_journal";
    public static final String KB_OPEN_SETTINGS = "key.villagersreborn.open_settings";
    public static final String KB_CATEGORY_GENERAL = "category.villagersreborn.general";

    // Generic labels
    public static final String UI_SEARCH = "Search";
    public static final String UI_VILLAGER_JOURNAL = "Villager Journal";
    public static final String UI_FAMILY_TREE = "Family Tree";
    public static final String UI_CLOSE = "Close";
    public static final String UI_BACK = "← Back";

    // Misc literals
    public static final String NONE = "None";
    public static final String UNKNOWN = "unknown";
    public static final String UNNAMED_VILLAGER = "Unnamed Villager";

    private StringConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}