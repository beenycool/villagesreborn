package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.ai.memory.*;
import com.beeny.villagesreborn.core.ai.social.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Personal journal system for villagers that creates rich narratives from their memories
 * Generates readable stories about villager lives, relationships, and experiences
 */
public class VillagerJournal {
    private final VillagerBrain villagerBrain;
    private final List<JournalEntry> entries = new ArrayList<>();
    private final Map<String, Integer> narrativeThemes = new HashMap<>();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");
    
    // Journal configuration
    private boolean autoGenerateEntries = true;
    private int maxEntries = 100;
    private long lastEntryTime = 0;
    private final long entryInterval = 86400000; // 24 hours in milliseconds
    
    public VillagerJournal(VillagerBrain villagerBrain) {
        this.villagerBrain = villagerBrain;
        initializeNarrativeThemes();
    }
    
    private void initializeNarrativeThemes() {
        narrativeThemes.put("daily_life", 0);
        narrativeThemes.put("relationships", 0);
        narrativeThemes.put("personal_growth", 0);
        narrativeThemes.put("village_events", 0);
        narrativeThemes.put("conflicts", 0);
        narrativeThemes.put("achievements", 0);
        narrativeThemes.put("fears_hopes", 0);
        narrativeThemes.put("seasonal_changes", 0);
    }
    
    /**
     * Generates a journal entry based on recent experiences and memories
     */
    public JournalEntry generateEntry() {
        return generateEntry(LocalDateTime.now());
    }
    
    /**
     * Generates a journal entry for a specific date
     */
    public JournalEntry generateEntry(LocalDateTime entryDate) {
        MemoryBank memoryBank = villagerBrain.getMemoryBank();
        
        // Determine the theme for this entry
        String theme = selectEntryTheme(memoryBank, entryDate);
        
        // Generate content based on theme
        String content = generateContentForTheme(theme, memoryBank, entryDate);
        
        // Create entry
        JournalEntry entry = new JournalEntry(
            entryDate,
            theme,
            content,
            getSafelyFormattedMood(),
            generateEntryTitle(theme, entryDate)
        );
        
        // Add to journal
        addEntry(entry);
        
        // Update narrative theme tracking
        narrativeThemes.put(theme, narrativeThemes.get(theme) + 1);
        
        return entry;
    }
    
    /**
     * Manually adds a journal entry (for special events)
     */
    public void addEntry(String title, String content, String theme) {
        JournalEntry entry = new JournalEntry(
            LocalDateTime.now(),
            theme,
            content,
            getSafelyFormattedMood(),
            title
        );
        addEntry(entry);
        narrativeThemes.put(theme, narrativeThemes.get(theme) + 1);
    }
    
    private void addEntry(JournalEntry entry) {
        entries.add(entry);
        lastEntryTime = System.currentTimeMillis();
        
        // Trim old entries if needed
        if (entries.size() > maxEntries) {
            entries.remove(0);
        }
    }
    
    /**
     * Selects the most appropriate theme for a journal entry
     */
    private String selectEntryTheme(MemoryBank memoryBank, LocalDateTime entryDate) {
        Map<String, Float> themeWeights = new HashMap<>();
        
        // Check for recent significant events
        LocalDateTime yesterday = entryDate.minusDays(1);
        List<Memory> recentMemories = memoryBank.getMemoriesFromPeriod(yesterday, entryDate);
        
        if (!recentMemories.isEmpty()) {
            // Weight themes based on recent memory types
            for (Memory memory : recentMemories) {
                String memoryType = memory.getType();
                if (memoryType.contains("interaction") || memoryType.contains("relationship")) {
                    themeWeights.put("relationships", themeWeights.getOrDefault("relationships", 0.0f) + 2.0f);
                } else if (memoryType.contains("village") || memoryType.contains("event")) {
                    themeWeights.put("village_events", themeWeights.getOrDefault("village_events", 0.0f) + 2.0f);
                } else if (memoryType.contains("conflict") || memoryType.contains("trauma")) {
                    themeWeights.put("conflicts", themeWeights.getOrDefault("conflicts", 0.0f) + 1.5f);
                }
            }
        }
        
        // Check personal stories for ongoing narratives
        List<PersonalStory> stories = memoryBank.getPersonalStories();
        for (PersonalStory story : stories) {
            if (story.isOngoing()) {
                themeWeights.put("personal_growth", themeWeights.getOrDefault("personal_growth", 0.0f) + 1.0f);
            }
        }
        
        // Seasonal and mood influences
        String season = getCurrentSeason(entryDate);
        if (season.equals("spring") || season.equals("autumn")) {
            themeWeights.put("seasonal_changes", themeWeights.getOrDefault("seasonal_changes", 0.0f) + 0.5f);
        }
        
        // Mood influence
        String currentMoodStr = getSafelyFormattedMood();
        if (currentMoodStr.contains("SAD") || currentMoodStr.contains("ANXIOUS")) {
            themeWeights.put("fears_hopes", themeWeights.getOrDefault("fears_hopes", 0.0f) + 1.0f);
        }
        
        // Default to daily_life if no strong themes
        themeWeights.put("daily_life", themeWeights.getOrDefault("daily_life", 1.0f));
        
        // Select theme with highest weight
        return themeWeights.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("daily_life");
    }
    
    /**
     * Generates journal content based on the selected theme
     */
    private String generateContentForTheme(String theme, MemoryBank memoryBank, LocalDateTime entryDate) {
        StringBuilder content = new StringBuilder();
        
        switch (theme) {
            case "relationships":
                content.append(generateRelationshipReflection(memoryBank));
                break;
            case "village_events":
                content.append(generateVillageEventReflection(memoryBank));
                break;
            case "personal_growth":
                content.append(generatePersonalGrowthReflection(memoryBank));
                break;
            case "conflicts":
                content.append(generateConflictReflection(memoryBank));
                break;
            case "achievements":
                content.append(generateAchievementReflection(memoryBank));
                break;
            case "fears_hopes":
                content.append(generateFearsHopesReflection(memoryBank));
                break;
            case "seasonal_changes":
                content.append(generateSeasonalReflection(entryDate));
                break;
            default: // daily_life
                content.append(generateDailyLifeReflection(memoryBank));
                break;
        }
        
        // Add mood context
        content.append("\n\n").append(generateMoodReflection());
        
        return content.toString();
    }
    
    private String generateRelationshipReflection(MemoryBank memoryBank) {
        StringBuilder reflection = new StringBuilder();
        
        Map<UUID, VillagerRelationshipMemory> relationships = memoryBank.getVillagerRelationships();
        if (!relationships.isEmpty()) {
            VillagerRelationshipMemory strongestRelationship = relationships.values().stream()
                .max((r1, r2) -> Float.compare(Math.abs(r1.getRelationshipStrength()), Math.abs(r2.getRelationshipStrength())))
                .orElse(null);
                
            if (strongestRelationship != null) {
                if (strongestRelationship.getRelationshipStrength() > 0) {
                    reflection.append("I've been thinking about my friendship with ").append(strongestRelationship.getVillagerName())
                        .append(". ").append(strongestRelationship.getLastInteraction())
                        .append(" It's wonderful to have someone I can count on in this village.");
                } else {
                    reflection.append("Things have been tense with ").append(strongestRelationship.getVillagerName())
                        .append(". ").append(strongestRelationship.getLastInteraction())
                        .append(" I hope we can work things out eventually.");
                }
            }
        } else {
            reflection.append("I've been feeling a bit lonely lately. Perhaps I should make more of an effort to connect with my fellow villagers.");
        }
        
        return reflection.toString();
    }
    
    private String generateVillageEventReflection(MemoryBank memoryBank) {
        StringBuilder reflection = new StringBuilder();
        
        List<VillageEvent> events = memoryBank.getVillageEvents();
        if (!events.isEmpty()) {
            VillageEvent recentEvent = events.get(events.size() - 1);
            reflection.append("The recent ").append(recentEvent.getEventType().toLowerCase())
                .append(" has been on my mind. ").append(recentEvent.getDescription())
                .append(" It's events like these that remind me how connected we all are in this village.");
        } else {
            reflection.append("Life in the village has been quiet lately. Sometimes I wonder what adventures await beyond our borders.");
        }
        
        return reflection.toString();
    }
    
    private String generatePersonalGrowthReflection(MemoryBank memoryBank) {
        StringBuilder reflection = new StringBuilder();
        
        List<PersonalStory> stories = memoryBank.getPersonalStories();
        PersonalStory recentStory = stories.stream()
            .filter(PersonalStory::isOngoing)
            .findFirst()
            .orElse(stories.isEmpty() ? null : stories.get(stories.size() - 1));
            
        if (recentStory != null) {
            reflection.append("I've been reflecting on my journey. ").append(recentStory.getNarrative())
                .append(" Each day brings new challenges and opportunities to grow.");
        } else {
            reflection.append("I feel like I'm at a crossroads in my life. There's so much I want to learn and experience.");
        }
        
        return reflection.toString();
    }
    
    private String generateConflictReflection(MemoryBank memoryBank) {
        StringBuilder reflection = new StringBuilder();
        
        List<TraumaticEvent> traumas = memoryBank.getTraumaticEvents();
        if (!traumas.isEmpty()) {
            TraumaticEvent recentTrauma = traumas.get(traumas.size() - 1);
            reflection.append("I'm still processing what happened. ").append(recentTrauma.getDescription())
                .append(" Conflict is never easy, but I hope we can learn from these experiences.");
        } else {
            reflection.append("Thankfully, things have been peaceful lately. I cherish these moments of harmony.");
        }
        
        return reflection.toString();
    }
    
    private String generateAchievementReflection(MemoryBank memoryBank) {
        StringBuilder reflection = new StringBuilder();
        
        List<JoyfulMemory> joyfulMemories = memoryBank.getJoyfulMemories();
        if (!joyfulMemories.isEmpty()) {
            JoyfulMemory recentJoy = joyfulMemories.get(joyfulMemories.size() - 1);
            reflection.append("I'm feeling proud today. ").append(recentJoy.getDescription())
                .append(" It's moments like these that make all the hard work worthwhile.");
        } else {
            reflection.append("I've been working hard lately. Perhaps it's time to celebrate the small victories in life.");
        }
        
        return reflection.toString();
    }
    
    private String generateFearsHopesReflection(MemoryBank memoryBank) {
        StringBuilder reflection = new StringBuilder();
        
        MoodState mood = villagerBrain.getCurrentMood();
        if (mood != null && mood.getOverallMood() != null) {
            String overallMoodStr = mood.getOverallMood().toString();
            if (overallMoodStr.contains("SAD")) {
                reflection.append("I've been feeling melancholy lately. Sometimes I worry about what the future holds for our village and for me.");
            } else if (overallMoodStr.contains("ANXIOUS")) {
                reflection.append("There's been a lot on my mind recently. Change can be frightening, but I try to remind myself that it often brings opportunities.");
            } else {
                reflection.append("I've been thinking about my dreams and aspirations. There's so much I hope to accomplish in this life.");
            }
        }
        
        return reflection.toString();
    }
    
    private String generateSeasonalReflection(LocalDateTime date) {
        String season = getCurrentSeason(date);
        StringBuilder reflection = new StringBuilder();
        
        switch (season) {
            case "spring":
                reflection.append("Spring has arrived, and with it comes new hope. The world is awakening, and I feel renewed energy in my bones.");
                break;
            case "summer":
                reflection.append("The warmth of summer fills me with contentment. Long days and short nights give us more time to work and play.");
                break;
            case "autumn":
                reflection.append("Autumn's arrival brings a sense of reflection. As the leaves change, I think about how much I've changed too.");
                break;
            case "winter":
                reflection.append("Winter's chill reminds me to appreciate the warmth of home and hearth. It's a time for contemplation and planning.");
                break;
        }
        
        return reflection.toString();
    }
    
    private String generateDailyLifeReflection(MemoryBank memoryBank) {
        StringBuilder reflection = new StringBuilder();
        
        String profession = villagerBrain.getProfession();
        reflection.append("Another day in the life of a ").append(profession.toLowerCase()).append(". ");
        
        if ("Farmer".equalsIgnoreCase(profession)) {
            reflection.append("The crops are growing well, and I find peace in tending to them.");
        } else if ("Blacksmith".equalsIgnoreCase(profession)) {
            reflection.append("The forge was hot today, but there's satisfaction in creating something with your hands.");
        } else if ("Librarian".equalsIgnoreCase(profession)) {
            reflection.append("Books continue to fascinate me. Each one holds a world of knowledge and adventure.");
        } else {
            reflection.append("My work keeps me busy, but I find purpose in serving my community.");
        }
        
        reflection.append(" The simple rhythms of village life bring me comfort.");
        
        return reflection.toString();
    }
    
    private String generateMoodReflection() {
        String moodString = getSafelyFormattedMood();
        
        if (moodString.contains("HAPPY")) {
            return "Overall, I'm feeling quite content with life right now.";
        } else if (moodString.contains("SAD")) {
            return "My spirits have been low lately, but I know this too shall pass.";
        } else if (moodString.contains("ANXIOUS")) {
            return "I've been feeling a bit on edge, but I'm trying to stay positive.";
        } else if (moodString.contains("ANGRY")) {
            return "I'm working through some frustrations, but I don't want them to define me.";
        } else {
            return "I'm feeling balanced today, taking life as it comes.";
        }
    }
    
    private String getCurrentSeason(LocalDateTime date) {
        int month = date.getMonthValue();
        if (month >= 3 && month <= 5) return "spring";
        if (month >= 6 && month <= 8) return "summer";
        if (month >= 9 && month <= 11) return "autumn";
        return "winter";
    }
    
    private String generateEntryTitle(String theme, LocalDateTime date) {
        String formattedDate = date.format(dateFormatter);
        
        switch (theme) {
            case "relationships":
                return "Bonds and Connections - " + formattedDate;
            case "village_events":
                return "Village Chronicles - " + formattedDate;
            case "personal_growth":
                return "Journey of Self - " + formattedDate;
            case "conflicts":
                return "Trials and Tribulations - " + formattedDate;
            case "achievements":
                return "Moments of Pride - " + formattedDate;
            case "fears_hopes":
                return "Dreams and Worries - " + formattedDate;
            case "seasonal_changes":
                return "Seasonal Reflections - " + formattedDate;
            default:
                return "Daily Musings - " + formattedDate;
        }
    }
    
    /**
     * Automatically generates journal entries based on time and significant events
     */
    public void updateJournal() {
        if (!autoGenerateEntries) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEntryTime >= entryInterval) {
            generateEntry();
        }
    }
    
    /**
     * Generates a complete life story from all journal entries
     */
    public String generateLifeStory() {
        StringBuilder lifeStory = new StringBuilder();
        lifeStory.append("The Life and Times of ").append(villagerBrain.getVillagerName()).append("\n");
        lifeStory.append("=".repeat(50)).append("\n\n");
        
        // Group entries by theme for narrative structure
        Map<String, List<JournalEntry>> entriesByTheme = entries.stream()
            .collect(Collectors.groupingBy(JournalEntry::getTheme));
            
        // Write thematic chapters
        for (Map.Entry<String, List<JournalEntry>> themeGroup : entriesByTheme.entrySet()) {
            String theme = themeGroup.getKey();
            List<JournalEntry> themeEntries = themeGroup.getValue();
            
            lifeStory.append("Chapter: ").append(formatThemeName(theme)).append("\n");
            lifeStory.append("-".repeat(30)).append("\n");
            
            for (JournalEntry entry : themeEntries.stream()
                    .sorted((e1, e2) -> e1.getDate().compareTo(e2.getDate()))
                    .limit(3) // Limit entries per theme for readability
                    .collect(Collectors.toList())) {
                lifeStory.append(entry.getDate().format(dateFormatter)).append("\n");
                lifeStory.append(entry.getContent()).append("\n\n");
            }
        }
        
        return lifeStory.toString();
    }
    
    private String formatThemeName(String theme) {
        return Arrays.stream(theme.split("_"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
            .collect(Collectors.joining(" "));
    }
    
    // Getters and utility methods
    public List<JournalEntry> getEntries() { return new ArrayList<>(entries); }
    public List<JournalEntry> getEntriesByTheme(String theme) {
        return entries.stream()
            .filter(entry -> entry.getTheme().equals(theme))
            .collect(Collectors.toList());
    }
    
    public List<JournalEntry> getRecentEntries(int count) {
        return entries.stream()
            .sorted((e1, e2) -> e2.getDate().compareTo(e1.getDate()))
            .limit(count)
            .collect(Collectors.toList());
    }
    
    public Map<String, Integer> getNarrativeThemes() { return new HashMap<>(narrativeThemes); }
    
    public void setAutoGenerateEntries(boolean autoGenerate) { this.autoGenerateEntries = autoGenerate; }
    public void setMaxEntries(int maxEntries) { this.maxEntries = maxEntries; }
    
    private String getSafelyFormattedMood() {
        MoodState mood = villagerBrain.getCurrentMood();
        if (mood != null && mood.getOverallMood() != null) {
            return mood.getOverallMood().toString();
        }
        return "NEUTRAL"; // Default mood if not available
    }
    
    /**
     * Journal entry class
     */
    public static class JournalEntry {
        private final LocalDateTime date;
        private final String theme;
        private final String content;
        private final String mood;
        private final String title;
        
        public JournalEntry(LocalDateTime date, String theme, String content, String mood, String title) {
            this.date = date;
            this.theme = theme;
            this.content = content;
            this.mood = mood;
            this.title = title;
        }
        
        public LocalDateTime getDate() { return date; }
        public String getTheme() { return theme; }
        public String getContent() { return content; }
        public String getMood() { return mood; }
        public String getTitle() { return title; }
        
        @Override
        public String toString() {
            return String.format("%s\n%s\n\nMood: %s\n%s", 
                title, "-".repeat(title.length()), mood, content);
        }
    }
}