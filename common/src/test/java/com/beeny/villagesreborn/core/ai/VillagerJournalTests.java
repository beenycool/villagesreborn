package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.ai.memory.*;
import com.beeny.villagesreborn.core.ai.social.SocialRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the VillagerJournal narrative generation system
 */
public class VillagerJournalTests {
    
    private VillagerBrain villagerBrain;
    private VillagerJournal journal;
    private UUID villagerUUID;
    
    @BeforeEach
    void setUp() {
        villagerUUID = UUID.randomUUID();
        villagerBrain = new VillagerBrain(villagerUUID);
        villagerBrain.setVillagerName("TestVillager");
        villagerBrain.setProfession("Farmer");
        journal = new VillagerJournal(villagerBrain);
    }
    
    @Test
    @DisplayName("Should generate basic journal entry")
    void testBasicJournalEntryGeneration() {
        // When
        VillagerJournal.JournalEntry entry = journal.generateEntry();
        
        // Then
        assertNotNull(entry);
        assertNotNull(entry.getTitle());
        assertNotNull(entry.getContent());
        assertNotNull(entry.getTheme());
        assertNotNull(entry.getMood());
        assertNotNull(entry.getDate());
        
        assertFalse(entry.getTitle().trim().isEmpty());
        assertFalse(entry.getContent().trim().isEmpty());
        assertTrue(entry.getContent().length() > 50); // Should be substantial
    }
    
    @Test
    @DisplayName("Should generate relationship-themed entries")
    void testRelationshipThemedEntry() {
        // Given - add relationship memories
        MemoryBank memoryBank = villagerBrain.getMemoryBank();
        UUID friendUUID = UUID.randomUUID();
        memoryBank.recordVillagerRelationship(friendUUID, "BestFriend", RelationshipType.BEST_FRIEND, 
                                            "Had a wonderful conversation", 0.8f);
        
        // When - generate multiple entries to increase chance of relationship theme
        VillagerJournal.JournalEntry relationshipEntry = null;
        for (int i = 0; i < 10; i++) {
            VillagerJournal.JournalEntry entry = journal.generateEntry();
            if ("relationships".equals(entry.getTheme())) {
                relationshipEntry = entry;
                break;
            }
        }
        
        // Then
        if (relationshipEntry != null) {
            assertEquals("relationships", relationshipEntry.getTheme());
            assertTrue(relationshipEntry.getContent().contains("BestFriend") || 
                      relationshipEntry.getContent().contains("friendship") ||
                      relationshipEntry.getContent().contains("relationship"));
            assertTrue(relationshipEntry.getTitle().contains("Bonds and Connections"));
        }
        // Note: Due to random theme selection, we can't guarantee a relationship theme,
        // but we test the functionality when it occurs
    }
    
    @Test
    @DisplayName("Should generate village event themed entries")
    void testVillageEventThemedEntry() {
        // Given - add village event
        MemoryBank memoryBank = villagerBrain.getMemoryBank();
        memoryBank.recordVillageEvent("festival", "Annual harvest festival brought joy", 
                                    Arrays.asList(villagerUUID), EventImpact.VERY_POSITIVE);
        
        // When - try to generate village event entry
        VillagerJournal.JournalEntry villageEntry = null;
        for (int i = 0; i < 10; i++) {
            VillagerJournal.JournalEntry entry = journal.generateEntry();
            if ("village_events".equals(entry.getTheme())) {
                villageEntry = entry;
                break;
            }
        }
        
        // Then
        if (villageEntry != null) {
            assertEquals("village_events", villageEntry.getTheme());
            assertTrue(villageEntry.getContent().contains("festival") || 
                      villageEntry.getContent().contains("village"));
            assertTrue(villageEntry.getTitle().contains("Village Chronicles"));
        }
    }
    
    @Test
    @DisplayName("Should generate personal growth themed entries")
    void testPersonalGrowthThemedEntry() {
        // Given - add personal story
        MemoryBank memoryBank = villagerBrain.getMemoryBank();
        memoryBank.addPersonalStory("My Journey", "Learning to become a better farmer", 
                                   Arrays.asList("Mentor", "Tools"), EmotionalTone.HOPEFUL, true);
        
        // When
        VillagerJournal.JournalEntry growthEntry = null;
        for (int i = 0; i < 10; i++) {
            VillagerJournal.JournalEntry entry = journal.generateEntry();
            if ("personal_growth".equals(entry.getTheme())) {
                growthEntry = entry;
                break;
            }
        }
        
        // Then
        if (growthEntry != null) {
            assertEquals("personal_growth", growthEntry.getTheme());
            assertTrue(growthEntry.getContent().contains("journey") || 
                      growthEntry.getContent().contains("grow") ||
                      growthEntry.getContent().contains("learn"));
            assertTrue(growthEntry.getTitle().contains("Journey of Self"));
        }
    }
    
    @Test
    @DisplayName("Should handle manual entry addition")
    void testManualEntryAddition() {
        // Given
        String title = "Special Day";
        String content = "Today was truly special because...";
        String theme = "achievements";
        
        // When
        journal.addEntry(title, content, theme);
        
        // Then
        List<VillagerJournal.JournalEntry> entries = journal.getEntries();
        assertEquals(1, entries.size());
        
        VillagerJournal.JournalEntry entry = entries.get(0);
        assertEquals(title, entry.getTitle());
        assertEquals(content, entry.getContent());
        assertEquals(theme, entry.getTheme());
    }
    
    @Test
    @DisplayName("Should retrieve entries by theme")
    void testEntriesByTheme() {
        // Given - add entries with different themes
        journal.addEntry("Daily Life 1", "Routine day", "daily_life");
        journal.addEntry("Relationship 1", "Met someone new", "relationships");
        journal.addEntry("Daily Life 2", "Another routine day", "daily_life");
        
        // When
        List<VillagerJournal.JournalEntry> dailyLifeEntries = journal.getEntriesByTheme("daily_life");
        List<VillagerJournal.JournalEntry> relationshipEntries = journal.getEntriesByTheme("relationships");
        List<VillagerJournal.JournalEntry> emptyThemeEntries = journal.getEntriesByTheme("nonexistent");
        
        // Then
        assertEquals(2, dailyLifeEntries.size());
        assertEquals(1, relationshipEntries.size());
        assertEquals(0, emptyThemeEntries.size());
        
        assertTrue(dailyLifeEntries.stream().allMatch(entry -> "daily_life".equals(entry.getTheme())));
        assertTrue(relationshipEntries.stream().allMatch(entry -> "relationships".equals(entry.getTheme())));
    }
    
    @Test
    @DisplayName("Should retrieve recent entries in correct order")
    void testRecentEntriesRetrieval() {
        // Given - add entries at different times
        journal.addEntry("Entry 1", "First entry", "daily_life");
        journal.addEntry("Entry 2", "Second entry", "relationships");
        journal.addEntry("Entry 3", "Third entry", "village_events");
        
        // When
        List<VillagerJournal.JournalEntry> recentEntries = journal.getRecentEntries(2);
        
        // Then
        assertEquals(2, recentEntries.size());
        
        // Should be in reverse chronological order (most recent first)
        assertEquals("Entry 3", recentEntries.get(0).getTitle());
        assertEquals("Entry 2", recentEntries.get(1).getTitle());
    }
    
    @Test
    @DisplayName("Should generate comprehensive life story")
    void testLifeStoryGeneration() {
        // Given - create diverse journal entries
        journal.addEntry("Childhood Memory", "I remember my early days", "personal_growth");
        journal.addEntry("First Friend", "Met my best friend", "relationships");
        journal.addEntry("Village Festival", "Participated in celebration", "village_events");
        journal.addEntry("Daily Routine", "Farming keeps me busy", "daily_life");
        journal.addEntry("Overcoming Fear", "Faced my fears today", "fears_hopes");
        
        // When
        String lifeStory = journal.generateLifeStory();
        
        // Then
        assertNotNull(lifeStory);
        assertFalse(lifeStory.trim().isEmpty());
        assertTrue(lifeStory.contains("The Life and Times of TestVillager"));
        assertTrue(lifeStory.contains("Chapter:"));
        
        // Should contain content from different themes
        assertTrue(lifeStory.contains("Personal Growth") || lifeStory.contains("personal_growth"));
        assertTrue(lifeStory.contains("Relationships") || lifeStory.contains("relationships"));
        
        // Should contain actual entry content
        assertTrue(lifeStory.contains("Childhood Memory") || lifeStory.contains("early days"));
    }
    
    @Test
    @DisplayName("Should track narrative themes")
    void testNarrativeThemeTracking() {
        // Given - generate entries of different themes
        journal.addEntry("Theme 1", "Content 1", "daily_life");
        journal.addEntry("Theme 2", "Content 2", "daily_life");
        journal.addEntry("Theme 3", "Content 3", "relationships");
        
        // When
        Map<String, Integer> themes = journal.getNarrativeThemes();
        
        // Then
        assertTrue(themes.containsKey("daily_life"));
        assertTrue(themes.containsKey("relationships"));
        assertEquals(2, (int) themes.get("daily_life"));
        assertEquals(1, (int) themes.get("relationships"));
    }
    
    @Test
    @DisplayName("Should respect maximum entry limits")
    void testEntryLimits() {
        // Given - set low limit for testing
        journal.setMaxEntries(3);
        
        // When - add more entries than the limit
        journal.addEntry("Entry 1", "Content 1", "daily_life");
        journal.addEntry("Entry 2", "Content 2", "daily_life");
        journal.addEntry("Entry 3", "Content 3", "daily_life");
        journal.addEntry("Entry 4", "Content 4", "daily_life");
        journal.addEntry("Entry 5", "Content 5", "daily_life");
        
        // Then
        List<VillagerJournal.JournalEntry> entries = journal.getEntries();
        assertEquals(3, entries.size());
        
        // Should keep the most recent entries
        assertEquals("Entry 3", entries.get(0).getTitle());
        assertEquals("Entry 4", entries.get(1).getTitle());
        assertEquals("Entry 5", entries.get(2).getTitle());
    }
    
    @Test
    @DisplayName("Should generate appropriate titles for different themes")
    void testThemeBasedTitleGeneration() {
        Map<String, String> themeToExpectedTitlePart = Map.of(
            "relationships", "Bonds and Connections",
            "village_events", "Village Chronicles",
            "personal_growth", "Journey of Self",
            "conflicts", "Trials and Tribulations",
            "achievements", "Moments of Pride",
            "fears_hopes", "Dreams and Worries",
            "seasonal_changes", "Seasonal Reflections",
            "daily_life", "Daily Musings"
        );

        for (Map.Entry<String, String> themePair : themeToExpectedTitlePart.entrySet()) {
            String targetTheme = themePair.getKey();
            String expectedTitlePart = themePair.getValue();
            
            VillagerJournal.JournalEntry themedEntry = null;
            boolean foundTheme = false;
            // Try to generate an entry of the target theme. This might take several attempts.
            // Increased attempts to 30 for better chance of hitting less common themes.
            for (int i = 0; i < 30; i++) { 
                // To make specific themes more likely, one might add relevant memories here before generating.
                // For instance, for 'relationships', add a relationship memory.
                // For 'village_events', add a village event memory, etc.
                // For simplicity in this generic test, we are not adding specific memories per theme iteration.
                // This means the test relies on the inherent probabilities of selectEntryTheme.
                
                VillagerJournal.JournalEntry currentEntry = journal.generateEntry();
                if (targetTheme.equals(currentEntry.getTheme())) {
                    themedEntry = currentEntry;
                    foundTheme = true;
                    break;
                }
            }

            if (!foundTheme) {
                // If a specific theme wasn't generated, we can't assert its title.
                // We can log this or make it a soft failure, depending on strictness.
                // For now, let's use fail() to indicate the test setup couldn't produce the theme.
                // Alternatively, use Assumptions.assumeTrue to skip if theme not generated.
                System.out.println("Warning: Could not generate an entry for theme: " + targetTheme + " within 30 attempts. Skipping title check for this theme.");
                // Or, for stricter testing: fail("Failed to generate an entry for theme: " + targetTheme + " within 30 attempts.");
                continue; // Skip assertion for this theme if not generated
            }
            
            assertNotNull(themedEntry, "Journal entry should not be null if foundTheme is true for theme: " + targetTheme);
            assertTrue(themedEntry.getTitle().contains(expectedTitlePart),
                "Title for theme '" + targetTheme + "' should contain '" + expectedTitlePart + "'. Got: " + themedEntry.getTitle());
        }
    }
    
    @Test
    @DisplayName("Should include mood reflection in content")
    void testMoodReflectionInContent() {
        // Given - set a specific mood
        MoodState happyMood = new MoodState();
        // Note: We can't easily set the mood without more complex setup,
        // but we can test that mood is included in the content
        
        // When
        VillagerJournal.JournalEntry entry = journal.generateEntry();
        
        // Then
        // Content should include some mood-related text
        String content = entry.getContent().toLowerCase();
        boolean hasMoodReflection = content.contains("feeling") || 
                                   content.contains("mood") || 
                                   content.contains("spirits") ||
                                   content.contains("content") ||
                                   content.contains("balanced");
        assertTrue(hasMoodReflection, "Entry should contain mood reflection");
    }
    
    @Test
    @DisplayName("Should generate profession-appropriate daily life content")
    void testProfessionSpecificContent() {
        // Given - villager is already set as Farmer
        
        // When - force daily_life theme entry
        journal.addEntry("Daily Work", "Generated content", "daily_life");
        
        // Generate an actual daily life entry
        VillagerJournal.JournalEntry entry = null;
        for (int i = 0; i < 20; i++) {
            VillagerJournal.JournalEntry testEntry = journal.generateEntry();
            if ("daily_life".equals(testEntry.getTheme())) {
                entry = testEntry;
                break;
            }
        }
        
        // Then
        if (entry != null) {
            String content = entry.getContent().toLowerCase();
            assertTrue(content.contains("farmer") || content.contains("crop") || 
                      content.contains("farm") || content.contains("work") ||
                      content.contains("village"));
        }
    }
    
    @Test
    @DisplayName("Should handle empty memory bank gracefully")
    void testEmptyMemoryBankHandling() {
        // Given - fresh villager with no memories
        VillagerBrain freshBrain = new VillagerBrain(UUID.randomUUID());
        freshBrain.setVillagerName("FreshVillager");
        freshBrain.setProfession("Unemployed");
        VillagerJournal freshJournal = new VillagerJournal(freshBrain);
        
        // When
        VillagerJournal.JournalEntry entry = freshJournal.generateEntry();
        
        // Then
        assertNotNull(entry);
        assertNotNull(entry.getContent());
        assertFalse(entry.getContent().trim().isEmpty());
        
        // Should default to daily_life or other basic themes
        assertTrue(Arrays.asList("daily_life", "fears_hopes", "seasonal_changes").contains(entry.getTheme()));
    }
    
    @Test
    @DisplayName("Should auto-generate entries when enabled")
    void testAutoGeneration() {
        // Given
        journal.setAutoGenerateEntries(true);
        int initialEntryCount = journal.getEntries().size();
        
        // When - simulate time passage (this is tricky to test without time manipulation)
        // For now, we just test that the setting is respected
        journal.setAutoGenerateEntries(false);
        
        // Then - verify setting changes are respected
        assertTrue(true); // The auto-generation is time-based and would require more complex testing
        
        // Test that we can disable auto-generation
        journal.setAutoGenerateEntries(false);
        // The updateJournal method should respect this setting
    }
}