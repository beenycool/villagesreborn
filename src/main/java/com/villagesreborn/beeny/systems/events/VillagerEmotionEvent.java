package com.villagesreborn.beeny.systems.events;

import com.villagesreborn.beeny.entities.Villager;
import com.villagesreborn.beeny.systems.EmotionManager;

public class VillagerEmotionEvent {
    private final Villager villager;
    private final EmotionManager.EmotionalState emotionalState;
    private final String eventType;
    private final String description;

    public VillagerEmotionEvent(Villager villager, EmotionManager.EmotionalState emotionalState, String eventType, String description) {
        this.villager = villager;
        this.emotionalState = emotionalState;
        this.eventType = eventType;
        this.description = description;
    }

    public Villager getVillager() {
        return villager;
    }

    public EmotionManager.EmotionalState getEmotionalState() {
        return emotionalState;
    }

    public String getEventType() {
        return eventType;
    }

    public String getDescription() {
        return description;
    }
}
