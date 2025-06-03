package com.beeny.villagesreborn.core.ai.memory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a personal story or significant narrative in a villager's life
 */
public class PersonalStory {
    private final String title;
    private final String narrative;
    private final LocalDateTime createdAt;
    private final List<String> keyCharacters;
    private final EmotionalTone tone;
    private final boolean isOngoing;
    
    public PersonalStory(String title, String narrative, LocalDateTime createdAt,
                        List<String> keyCharacters, EmotionalTone tone, boolean isOngoing) {
        this.title = title;
        this.narrative = narrative;
        this.createdAt = createdAt;
        this.keyCharacters = keyCharacters;
        this.tone = tone;
        this.isOngoing = isOngoing;
    }
    
    public String getTitle() { return title; }
    public String getNarrative() { return narrative; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<String> getKeyCharacters() { return keyCharacters; }
    public EmotionalTone getTone() { return tone; }
    public boolean isOngoing() { return isOngoing; }
}