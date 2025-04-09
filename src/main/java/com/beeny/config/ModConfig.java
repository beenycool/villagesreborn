package com.beeny.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "villagesreborn")
@Config.Gui.Background("minecraft:textures/block/stone.png") // Example background
public class ModConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip
    @Comment("General settings for village behavior.")
    public General general = new General();

    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip
    @Comment("Settings related to gameplay mechanics.")
    public Gameplay gameplay = new Gameplay();

    // Add other categories (AI, UI) here later

    public static class General {
        @ConfigEntry.Gui.Tooltip
        @Comment("Adjusts the relative frequency of village generation. Higher values mean more frequent spawns. Default: 1.0")
        @ConfigEntry.BoundedDouble(min = 0.1, max = 5.0)
        public double spawnRateModifier = 1.0; // Renamed for clarity

        @ConfigEntry.Gui.CollapsibleObject // Group culture toggles
        @ConfigEntry.Gui.Tooltip
        @Comment("Enable or disable specific village cultures.")
        public CultureToggles cultureToggles = new CultureToggles();

    }

    public static class CultureToggles {
        @Comment("Enable Roman villages")
        public boolean enableRoman = true;
        @Comment("Enable Egyptian villages")
        public boolean enableEgyptian = true;
        @Comment("Enable Victorian villages")
        public boolean enableVictorian = true;
        @Comment("Enable NYC villages")
        public boolean enableNyc = true;
        // Add more cultures here if they exist
    }

    public static class Gameplay {
        @ConfigEntry.Gui.Tooltip
        @Comment("Frequency multiplier for village events (e.g., festivals, raids). Default: 1.0")
        @ConfigEntry.BoundedDouble(min = 0.1, max = 5.0)
        public double eventFrequency = 1.0;

        @ConfigEntry.Gui.Tooltip
        @Comment("Frequency multiplier for villager activities (e.g., farming, crafting). Default: 1.0")
        @ConfigEntry.BoundedDouble(min = 0.1, max = 5.0)
        public double activityFrequency = 1.0;

        @ConfigEntry.Gui.Tooltip
        @Comment("Frequency multiplier for trading interactions. Default: 1.0")
        @ConfigEntry.BoundedDouble(min = 0.1, max = 5.0)
        public double tradingFrequency = 1.0;

        @ConfigEntry.Gui.Tooltip
        @Comment("Intensity of relationship changes between villagers. Default: 1.0")
        @ConfigEntry.BoundedDouble(min = 0.1, max = 3.0)
        public double relationshipIntensity = 1.0;

        @ConfigEntry.Gui.Tooltip
        @Comment("Duration (in game ticks) villager memories last. Default: 24000 (1 day)")
        @ConfigEntry.BoundedLong(min = 1200, max = 120000) // 1 min to 5 days
        public long memoryDurationTicks = 24000L;

        @ConfigEntry.Gui.Tooltip
        @Comment("Speed multiplier for village development (structure building, upgrades). Default: 1.0")
        @ConfigEntry.BoundedDouble(min = 0.1, max = 5.0)
        public double villageDevelopmentSpeed = 1.0;

        @ConfigEntry.Gui.Tooltip
        @Comment("Multiplier for village economic factors (production, wealth). Default: 1.0")
        @ConfigEntry.BoundedDouble(min = 0.1, max = 3.0)
        public double economyMultiplier = 1.0;

        @ConfigEntry.Gui.Tooltip
        @Comment("Multiplier for village defense capabilities (guard effectiveness, structure health). Default: 1.0")
        @ConfigEntry.BoundedDouble(min = 0.5, max = 3.0)
        public double defenseMultiplier = 1.0;

        @ConfigEntry.Gui.Tooltip
        @Comment("Multiplier for villager resource gathering rates. Default: 1.0")
        @ConfigEntry.BoundedDouble(min = 0.1, max = 5.0)
        public double resourceGatheringRate = 1.0;

        @ConfigEntry.Gui.Tooltip
        @Comment("Multiplier for village population growth rate. Default: 1.0")
        @ConfigEntry.BoundedDouble(min = 0.1, max = 3.0)
        public double populationGrowthRate = 1.0;

        @ConfigEntry.Gui.Tooltip
        @Comment("Multiplier for cultural bias effects (e.g., inter-culture relations). Default: 1.0")
        @ConfigEntry.BoundedDouble(min = 0.0, max = 2.0)
        public double culturalBiasMultiplier = 1.0;

        @ConfigEntry.Gui.Tooltip
        @Comment("Minimum time (in ticks) between major events. Default: 48000 (2 days)")
        @ConfigEntry.BoundedLong(min = 12000, max = 120000)
        public long minEventIntervalTicks = 48000L;

        @ConfigEntry.Gui.Tooltip
        @Comment("Maximum time (in ticks) between major events. Default: 96000 (4 days)")
        @ConfigEntry.BoundedLong(min = 24000, max = 240000)
        public long maxEventIntervalTicks = 96000L;

        @ConfigEntry.Gui.Tooltip
        @Comment("Enable notifications for village events. Default: true")
        public boolean enableEventNotifications = true;

        @ConfigEntry.Gui.Tooltip
        @Comment("Multiplier for reputation effects on interactions. Default: 1.0")
        @ConfigEntry.BoundedDouble(min = 0.1, max = 3.0)
        public double reputationEffectMultiplier = 1.0;

        @ConfigEntry.Gui.Tooltip
        @Comment("Trading bonus multiplier based on reputation/relations. Default: 1.0")
        @ConfigEntry.BoundedDouble(min = 0.5, max = 2.0)
        public double tradingBonusMultiplier = 1.0;

        @ConfigEntry.Gui.Tooltip
        @Comment("Enable villager crafting system. Default: true")
        public boolean enableVillagerCrafting = true;
    }
}

@ConfigEntry.Gui.CollapsibleObject
@ConfigEntry.Gui.Tooltip
@Comment("Settings related to Villager AI behavior and LLM integration.")
public AI ai = new AI();

@ConfigEntry.Gui.CollapsibleObject
@ConfigEntry.Gui.Tooltip
@Comment("Settings related to the mod's user interface elements.")
public UI ui = new UI();


public static class AI {
    @ConfigEntry.Gui.Tooltip
    @Comment("Level of detail for AI processing (Higher uses more resources). Default: MEDIUM")
    public AIDetailLevel aiDetailLevel = AIDetailLevel.MEDIUM;

    @ConfigEntry.Gui.Tooltip
    @Comment("Maximum tokens allowed for LLM requests. Default: 150")
    @ConfigEntry.BoundedInt(min = 50, max = 500)
    public int maxTokens = 150;

    @ConfigEntry.Gui.Tooltip
    @Comment("Delay (in milliseconds) before sending LLM requests. Default: 500")
    @ConfigEntry.BoundedLong(min = 100, max = 5000)
    public long responseDelayMs = 500L;

    @ConfigEntry.Gui.Tooltip
    @Comment("Enable the advanced memory system for villagers. Default: true")
    public boolean enableMemorySystem = true;

    @ConfigEntry.Gui.Tooltip
    @Comment("Local path for storing AI-related data (optional). Leave blank to use default.")
    public String localDataPath = "";

    @ConfigEntry.Gui.Tooltip
    @Comment("Enable more advanced and context-aware conversations. Default: true")
    public boolean enableAdvancedConversations = true;
}

public static class UI {
    @ConfigEntry.Gui.Tooltip
    @Comment("Show villager name tags. Default: true")
    public boolean showNameTags = true;

    @ConfigEntry.Gui.Tooltip
    @Comment("Show villager health bars. Default: false")
    public boolean showHealthBars = false;

    @ConfigEntry.Gui.Tooltip
    @Comment("Show markers for village locations. Default: true")
    public boolean showVillageMarkers = true;

    @ConfigEntry.Gui.Tooltip
    @Comment("Use a more compact display for villager info. Default: false")
    public boolean useCompactInfo = false;

    @ConfigEntry.Gui.Tooltip
    @Comment("Select the UI color scheme. Default: DEFAULT")
    public UIColorScheme colorScheme = UIColorScheme.DEFAULT;

    @ConfigEntry.Gui.Tooltip
    @Comment("Color for villager names based on reputation. Default: true")
    public boolean colorNameByReputation = true;
}

// --- Enums for Settings ---

public enum AIDetailLevel {
    LOW, MEDIUM, HIGH
}

public enum UIColorScheme {
    DEFAULT, HIGH_CONTRAST, CULTURE_BASED
}

    @Override
    public void validatePostLoad() throws ValidationException {
        // Optional: Add validation logic here if needed after config loads
        ConfigData.super.validatePostLoad();
    }
}