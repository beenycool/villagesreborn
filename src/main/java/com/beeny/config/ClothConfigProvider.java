package com.beeny.config;

import com.beeny.Villagesreborn;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.List;

/**
 * Provides Cloth Config integration for Villages Reborn config
 */
public class ClothConfigProvider {
    private final VillagesConfig config;
    
    public ClothConfigProvider() {
        this.config = VillagesConfig.getInstance();
    }
    
    /**
     * Creates a Cloth Config screen for Villages Reborn settings
     * 
     * @param parent The parent screen
     * @return The created config screen
     */
    public Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Villages Reborn Settings"))
                .setSavingRunnable(config::save);
        
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        
        // Create categories
        ConfigCategory generalCategory = builder.getOrCreateCategory(Text.literal("General Settings"));
        ConfigCategory gameplayCategory = builder.getOrCreateCategory(Text.literal("Gameplay Settings"));
        ConfigCategory aiCategory = builder.getOrCreateCategory(Text.literal("AI Settings"));
        ConfigCategory uiCategory = builder.getOrCreateCategory(Text.literal("UI Settings"));
        
        // Add general settings
        addGeneralSettings(generalCategory, entryBuilder);
        
        // Add gameplay settings
        addGameplaySettings(gameplayCategory, entryBuilder);
        
        // Add AI settings
        addAISettings(aiCategory, entryBuilder);
        
        // Add UI settings
        addUISettings(uiCategory, entryBuilder);
        
        return builder.build();
    }
    
    private void addGeneralSettings(ConfigCategory category, ConfigEntryBuilder entryBuilder) {
        // Village Spawn Rate
        List<String> spawnRates = Arrays.asList("LOW", "MEDIUM", "HIGH");
        category.addEntry(entryBuilder.startSelector(
                Text.literal("Village Spawn Rate"),
                spawnRates.toArray(),
                config.getVillageSpawnRate())
                .setDefaultValue("MEDIUM")
                .setTooltip(Text.literal("Controls how frequently Villages Reborn villages appear in the world"))
                .setSaveConsumer(value -> {
                    for (int i = 0; i < spawnRates.size(); i++) {
                        if (spawnRates.get(i).equals(config.getVillageSpawnRate())) {
                            config.cycleVillageSpawnRate();
                            if (spawnRates.get(i).equals(value)) break;
                        }
                    }
                })
                .build());
        
        // Cultures
        List<String> availableCultures = Arrays.asList("ROMAN", "EGYPTIAN", "VICTORIAN", "NYC", "NETHER", "END");
        for (String culture : availableCultures) {
            category.addEntry(entryBuilder.startBooleanToggle(
                    Text.literal("Enable " + culture + " Culture"),
                    config.getEnabledCultures().contains(culture))
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable " + culture + " villages in world generation"))
                    .setSaveConsumer(value -> {
                        List<String> cultures = config.getEnabledCultures();
                        if (value && !cultures.contains(culture)) {
                            cultures.add(culture);
                        } else if (!value && cultures.contains(culture)) {
                            cultures.remove(culture);
                        }
                    })
                    .build());
        }
    }
    
    private void addGameplaySettings(ConfigCategory category, ConfigEntryBuilder entryBuilder) {
        VillagesConfig.GameplaySettings gameplay = config.getGameplaySettings();
        
        // Event Frequency Multiplier
        category.addEntry(entryBuilder.startFloatField(
                Text.literal("Event Frequency Multiplier"),
                gameplay.getEventFrequencyMultiplier())
                .setDefaultValue(1.0f)
                .setMin(0.25f)
                .setMax(5.0f)
                .setTooltip(Text.literal("Multiplies the frequency of village events"))
                .setSaveConsumer(gameplay::setEventFrequencyMultiplier)
                .build());
        
        // Villager Activity Frequency
        category.addEntry(entryBuilder.startFloatField(
                Text.literal("Villager Activity Frequency"),
                gameplay.getVillagerActivityFrequencyMultiplier())
                .setDefaultValue(1.0f)
                .setMin(0.25f)
                .setMax(5.0f)
                .setTooltip(Text.literal("Multiplies the frequency of villager activities"))
                .setSaveConsumer(gameplay::setVillagerActivityFrequencyMultiplier)
                .build());
        
        // Trading Frequency
        category.addEntry(entryBuilder.startFloatField(
                Text.literal("Trading Frequency Multiplier"),
                gameplay.getTradingFrequencyMultiplier())
                .setDefaultValue(1.0f)
                .setMin(0.25f)
                .setMax(5.0f)
                .setTooltip(Text.literal("Multiplies the frequency of trading-related events"))
                .setSaveConsumer(gameplay::setTradingFrequencyMultiplier)
                .build());
        
        // Relationship Intensity
        category.addEntry(entryBuilder.startFloatField(
                Text.literal("Relationship Intensity Multiplier"),
                gameplay.getRelationshipIntensityMultiplier())
                .setDefaultValue(1.0f)
                .setMin(0.25f)
                .setMax(5.0f)
                .setTooltip(Text.literal("Multiplies the intensity of relationship changes"))
                .setSaveConsumer(gameplay::setRelationshipIntensityMultiplier)
                .build());
        
        // Cultural Event Multiplier
        category.addEntry(entryBuilder.startFloatField(
                Text.literal("Cultural Event Multiplier"),
                gameplay.getCulturalEventMultiplier())
                .setDefaultValue(1.0f)
                .setMin(0.25f)
                .setMax(5.0f)
                .setTooltip(Text.literal("Multiplies the frequency of culture-specific events"))
                .setSaveConsumer(gameplay::setCulturalEventMultiplier)
                .build());
        
        // Villager Memory Duration
        category.addEntry(entryBuilder.startIntSlider(
                Text.literal("Villager Memory Duration (days)"),
                config.getVillagerMemoryDuration(),
                1, 7)
                .setDefaultValue(3)
                .setTooltip(Text.literal("How long villagers remember player actions"))
                .setSaveConsumer(config::setVillagerMemoryDuration)
                .build());
        
        // Cultural Gift Modifier
        category.addEntry(entryBuilder.startIntSlider(
                Text.literal("Cultural Gift Value Modifier (%)"),
                config.getCulturalGiftModifier(),
                50, 200)
                .setDefaultValue(150)
                .setTooltip(Text.literal("How much cultural preferences affect gift values"))
                .setSaveConsumer(config::setCulturalGiftModifier)
                .build());
        
        // Progressive Village Development
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Progressive Village Development"),
                gameplay.isProgressiveVillageDevelopment())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Villages evolve and develop over time"))
                .setSaveConsumer(gameplay::setProgressiveVillageDevelopment)
                .build());
        
        // Dynamic Economy
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Dynamic Economy"),
                gameplay.isDynamicEconomyEnabled())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Village economies adjust based on available resources"))
                .setSaveConsumer(gameplay::setDynamicEconomyEnabled)
                .build());
        
        // Weather Affects Events
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Weather Affects Events"),
                gameplay.isWeatherAffectsEvents())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Weather conditions influence village events"))
                .setSaveConsumer(gameplay::setWeatherAffectsEvents)
                .build());
        
        // Village Defense
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Village Defense System"),
                gameplay.isVillageDefenseEnabled())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Villages defend themselves against threats"))
                .setSaveConsumer(gameplay::setVillageDefenseEnabled)
                .build());
        
        // Resource Gathering
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Resource Gathering"),
                gameplay.isResourceGatheringEnabled())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Villagers collect resources from the environment"))
                .setSaveConsumer(gameplay::setResourceGatheringEnabled)
                .build());
        
        // Dynamic Population Growth
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Dynamic Population Growth"),
                gameplay.isDynamicPopulationGrowth())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Village populations change over time"))
                .setSaveConsumer(gameplay::setDynamicPopulationGrowth)
                .build());
        
        // Cultural Bias
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Cultural Bias"),
                gameplay.isCulturalBiasEnabled())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Villagers prefer their own culture"))
                .setSaveConsumer(gameplay::setCulturalBiasEnabled)
                .build());
        
        // Min Days Between Events
        category.addEntry(entryBuilder.startIntSlider(
                Text.literal("Minimum Days Between Events"),
                gameplay.getMinDaysBetweenEvents(),
                0, 14)
                .setDefaultValue(2)
                .setTooltip(Text.literal("Minimum days between events in the same village"))
                .setSaveConsumer(gameplay::setMinDaysBetweenEvents)
                .build());
        
        // Max Concurrent Events
        category.addEntry(entryBuilder.startIntSlider(
                Text.literal("Maximum Concurrent Events"),
                gameplay.getMaxConcurrentEvents(),
                1, 10)
                .setDefaultValue(3)
                .setTooltip(Text.literal("Maximum number of events happening at the same time"))
                .setSaveConsumer(gameplay::setMaxConcurrentEvents)
                .build());
        
        // Event Notifications
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Show Event Notifications"),
                gameplay.isShowEventNotifications())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Display notifications for village events"))
                .setSaveConsumer(gameplay::setShowEventNotifications)
                .build());
        
        // Player Reputation Affects Events
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Player Reputation Affects Events"),
                gameplay.isPlayerReputationAffectsEvents())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Your reputation influences event invitations"))
                .setSaveConsumer(gameplay::setPlayerReputationAffectsEvents)
                .build());
        
        // Villager PvP
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Villager PvP"),
                config.isVillagerPvPEnabled())
                .setDefaultValue(false)
                .setTooltip(Text.literal("Villagers can fight each other and the player"))
                .setSaveConsumer(value -> {
                    if (value != config.isVillagerPvPEnabled()) {
                        config.toggleVillagerPvP();
                    }
                })
                .build());
        
        // Theft Detection
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Theft Detection"),
                config.isTheftDetectionEnabled())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Villagers detect when you steal items"))
                .setSaveConsumer(value -> {
                    if (value != config.isTheftDetectionEnabled()) {
                        config.toggleTheftDetection();
                    }
                })
                .build());
        
        // Villager Trading Boost
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Cultural Trading Bonus"),
                config.isVillagerTradingBoostEnabled())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Better trades with villagers of cultures you have good relations with"))
                .setSaveConsumer(value -> {
                    if (value != config.isVillagerTradingBoostEnabled()) {
                        config.toggleVillagerTradingBoost();
                    }
                })
                .build());
        
        // Unique Crafting Recipes
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Cultural Crafting"),
                config.isUniqueCraftingRecipesEnabled())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Enable culture-specific crafting recipes"))
                .setSaveConsumer(value -> {
                    if (value != config.isUniqueCraftingRecipesEnabled()) {
                        config.toggleUniqueCraftingRecipes();
                    }
                })
                .build());
    }
    
    private void addAISettings(ConfigCategory category, ConfigEntryBuilder entryBuilder) {
        VillagesConfig.LLMSettings llm = config.getLLMSettings();
        
        // AI Provider
        List<String> providers = Arrays.asList("OPENAI", "ANTHROPIC", "DEEPSEEK", "GEMINI", "MISTRAL", 
                "AZURE", "COHERE", "OPENROUTER");
        category.addEntry(entryBuilder.startSelector(
                Text.literal("AI Provider"),
                providers.toArray(),
                config.getAIProvider())
                .setDefaultValue("OPENAI")
                .setTooltip(Text.literal("Select the AI service provider"))
                .setSaveConsumer(value -> {
                    while (!config.getAIProvider().equals(value)) {
                        config.cycleAIProvider();
                    }
                })
                .build());
        
        // API Key
        category.addEntry(entryBuilder.startStrField(
                Text.literal("API Key"),
                llm.getApiKey())
                .setDefaultValue("")
                .setTooltip(Text.literal("API key for the selected AI provider"))
                .setSaveConsumer(llm::setApiKey)
                .build());
        
        // Endpoint
        category.addEntry(entryBuilder.startStrField(
                Text.literal("API Endpoint"),
                llm.getEndpoint())
                .setDefaultValue("https://api.openai.com/v1/chat/completions")
                .setTooltip(Text.literal("Custom endpoint URL for the AI API"))
                .setSaveConsumer(llm::setEndpoint)
                .build());
        
        // Model
        category.addEntry(entryBuilder.startStrField(
                Text.literal("Model"),
                llm.getModel())
                .setDefaultValue("gpt-3.5-turbo")
                .setTooltip(Text.literal("AI model to use"))
                .setSaveConsumer(llm::setModel)
                .build());
        
        // AI Detail Level
        List<String> detailLevels = Arrays.asList("MINIMAL", "BALANCED", "DETAILED", "CUSTOM");
        category.addEntry(entryBuilder.startSelector(
                Text.literal("AI Detail Level"),
                detailLevels.toArray(),
                llm.getAiDetailLevel())
                .setDefaultValue("BALANCED")
                .setTooltip(Text.literal("Controls how detailed AI responses are"))
                .setSaveConsumer(value -> {
                    while (!llm.getAiDetailLevel().equals(value)) {
                        llm.cycleAiDetailLevel();
                    }
                })
                .build());
        
        // Temperature
        category.addEntry(entryBuilder.startFloatField(
                Text.literal("Temperature"),
                llm.getTemperature())
                .setDefaultValue(0.7f)
                .setMin(0.0f)
                .setMax(1.0f)
                .setTooltip(Text.literal("Controls randomness in AI responses (0.0-1.0)"))
                .setSaveConsumer(llm::setTemperature)
                .build());
        
        // Max Tokens
        category.addEntry(entryBuilder.startIntField(
                Text.literal("Max Tokens"),
                llm.getMaxTokens())
                .setDefaultValue(1000)
                .setMin(100)
                .setMax(4096)
                .setTooltip(Text.literal("Maximum tokens in AI responses"))
                .setSaveConsumer(llm::setMaxTokens)
                .build());
        
        // Context Length
        category.addEntry(entryBuilder.startIntField(
                Text.literal("Context Length"),
                llm.getContextLength())
                .setDefaultValue(4)
                .setMin(1)
                .setMax(10)
                .setTooltip(Text.literal("Number of previous interactions to remember"))
                .setSaveConsumer(llm::setContextLength)
                .build());
        
        // Cache Size
        category.addEntry(entryBuilder.startIntField(
                Text.literal("Cache Size"),
                llm.getMaxCacheSize())
                .setDefaultValue(100)
                .setMin(0)
                .setMax(1000)
                .setTooltip(Text.literal("Maximum number of cached responses"))
                .setSaveConsumer(llm::setMaxCacheSize)
                .build());
        
        // Cache TTL
        category.addEntry(entryBuilder.startIntField(
                Text.literal("Cache TTL (seconds)"),
                llm.getCacheTTLSeconds())
                .setDefaultValue(300)
                .setMin(0)
                .setMax(3600)
                .setTooltip(Text.literal("How long to keep cached responses"))
                .setSaveConsumer(llm::setCacheTTLSeconds)
                .build());
        
        // Response Delay
        category.addEntry(entryBuilder.startIntSlider(
                Text.literal("AI Response Delay (ms)"),
                llm.getAiResponseDelay(),
                0, 5000)
                .setDefaultValue(0)
                .setTooltip(Text.literal("Add delay to AI responses for realism"))
                .setSaveConsumer(llm::setAiResponseDelay)
                .build());
        
        // Use Memory
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Use Memory System"),
                llm.isUseMemory())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Villagers remember past interactions"))
                .setSaveConsumer(llm::setUseMemory)
                .build());
        
        // Local Model
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Use Local Model"),
                llm.isLocalModel())
                .setDefaultValue(false)
                .setTooltip(Text.literal("Use locally downloaded model instead of API"))
                .setSaveConsumer(llm::setLocalModel)
                .build());
        
        // Local Model Path
        category.addEntry(entryBuilder.startStrField(
                Text.literal("Local Model Path"),
                llm.getLocalModelPath())
                .setDefaultValue("")
                .setTooltip(Text.literal("Path to local model files"))
                .setSaveConsumer(llm::setLocalModelPath)
                .build());
        
        // Advanced Conversations
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Advanced Conversations"),
                llm.isAdvancedConversationsEnabled())
                .setDefaultValue(false)
                .setTooltip(Text.literal("Enable more complex dialogue with villagers"))
                .setSaveConsumer(llm::setAdvancedConversationsEnabled)
                .build());
    }
    
    private void addUISettings(ConfigCategory category, ConfigEntryBuilder entryBuilder) {
        VillagesConfig.UISettings ui = config.getUISettings();
        
        // Show Villager Name Tags
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Show Villager Name Tags"),
                ui.isShowVillagerNameTags())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Display name tags above villagers"))
                .setSaveConsumer(ui::setShowVillagerNameTags)
                .build());
        
        // Show Villager Health Bars
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Show Villager Health Bars"),
                ui.isShowVillagerHealthBars())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Display health bars above villagers"))
                .setSaveConsumer(ui::setShowVillagerHealthBars)
                .build());
        
        // Show Village Markers
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Show Village Markers"),
                ui.isShowVillageMarkers())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Display markers for villages on the HUD"))
                .setSaveConsumer(ui::setShowVillageMarkers)
                .build());
        
        // Village Marker Range
        category.addEntry(entryBuilder.startIntSlider(
                Text.literal("Village Marker Range"),
                ui.getVillageMarkerRange(),
                16, 256)
                .setDefaultValue(64)
                .setTooltip(Text.literal("Distance at which village markers are visible"))
                .setSaveConsumer(ui::setVillageMarkerRange)
                .build());
        
        // Compact Villager Info
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Compact Villager Info"),
                ui.isCompactVillagerInfo())
                .setDefaultValue(false)
                .setTooltip(Text.literal("Use compact display for villager information"))
                .setSaveConsumer(ui::setCompactVillagerInfo)
                .build());
        
        // Color Scheme
        List<String> colorSchemes = Arrays.asList("DEFAULT", "DARK", "LIGHT", "CULTURAL");
        category.addEntry(entryBuilder.startSelector(
                Text.literal("Color Scheme"),
                colorSchemes.toArray(),
                ui.getColorScheme())
                .setDefaultValue("DEFAULT")
                .setTooltip(Text.literal("UI color theme"))
                .setSaveConsumer(value -> ui.setColorScheme((String)value))
                .build());
        
        // Conversation Label Format
        category.addEntry(entryBuilder.startStrField(
                Text.literal("Conversation Label Format"),
                ui.getConversationLabelFormat())
                .setDefaultValue("Speaking to: {name}")
                .setTooltip(Text.literal("Format for conversation labels. Use {name} for villager name."))
                .setSaveConsumer(ui::setConversationLabelFormat)
                .build());
        
        // Conversation HUD Position
        List<String> hudPositions = Arrays.asList("TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT");
        category.addEntry(entryBuilder.startSelector(
                Text.literal("Conversation HUD Position"),
                hudPositions.toArray(),
                ui.getConversationHudPosition())
                .setDefaultValue("BOTTOM_RIGHT")
                .setTooltip(Text.literal("Position of conversation HUD on screen"))
                .setSaveConsumer(value -> ui.setConversationHudPosition((String)value))
                .build());
        
        // Show Culture
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Show Culture"),
                ui.isShowCulture())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Show villager's culture in UI"))
                .setSaveConsumer(ui::setShowCulture)
                .build());
        
        // Show Profession
        category.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Show Profession"),
                ui.isShowProfession())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Show villager's profession in UI"))
                .setSaveConsumer(ui::setShowProfession)
                .build());
        
        // Background Color
        category.addEntry(entryBuilder.startAlphaColorField(
                Text.literal("Background Color"),
                ui.getBackgroundColor())
                .setDefaultValue(0x80000000)
                .setTooltip(Text.literal("Background color for UI elements"))
                .setSaveConsumer(ui::setBackgroundColor)
                .build());
        
        // Border Color
        category.addEntry(entryBuilder.startAlphaColorField(
                Text.literal("Border Color"),
                ui.getBorderColor())
                .setDefaultValue(0x80FFFFFF)
                .setTooltip(Text.literal("Border color for UI elements"))
                .setSaveConsumer(ui::setBorderColor)
                .build());
        
        // Label Color
        category.addEntry(entryBuilder.startColorField(
                Text.literal("Label Color"),
                ui.getLabelColor())
                .setDefaultValue(0xFFFFFFFF)
                .setTooltip(Text.literal("Text color for labels"))
                .setSaveConsumer(ui::setLabelColor)
                .build());
        
        // Name Color
        category.addEntry(entryBuilder.startColorField(
                Text.literal("Name Color"),
                ui.getNameColor())
                .setDefaultValue(0xFFFFFF00)
                .setTooltip(Text.literal("Text color for villager names"))
                .setSaveConsumer(ui::setNameColor)
                .build());
    }
}