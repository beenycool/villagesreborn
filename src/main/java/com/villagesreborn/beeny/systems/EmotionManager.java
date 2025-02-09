package com.villagesreborn.beeny.systems;

import com.villagesreborn.beeny.entities.Villager;
import com.villagesreborn.beeny.systems.events.EventDispatcher;
import com.villagesreborn.beeny.systems.events.VillagerEmotionEvent;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class EmotionManager {
    private final Villager villager;

    public EmotionManager(Villager villager) {
        this.villager = villager;
    }

    public enum EmotionalState { HAPPY, SAD, ANGRY, NEUTRAL }

    private static final int MAX_MEMORY_ENTRIES = 50;
    private EmotionalState currentState = EmotionalState.NEUTRAL;
    private int moodScore = 0;
    private final List<MemoryLogEntry> memoryLog = new ArrayList<>();
    private final ScheduledExecutorService asyncExecutor =
            Executors.newSingleThreadScheduledExecutor();
    private transient MemorySystem memorySystem;

    public record MemoryLogEntry(
            long timestamp,
            String eventType,
            String description,
            int moodImpact
    ) {
        public String getFormattedTimestamp() {
            return Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    public void adjustMood(int delta, String eventType, String description, Runnable callback) {
        asyncExecutor.execute(() -> {
            try {
                moodScore = Math.max(-100, Math.min(100, moodScore + delta));
                MemoryLogEntry entry = new MemoryLogEntry(
                        System.currentTimeMillis(),
                        eventType,
                        description,
                        delta
                );

                memoryLog.add(entry);

                // Memory log maintenance (lines 86-88)
                if (memoryLog.size() > MAX_MEMORY_ENTRIES) {
                    int overflow = memoryLog.size() - MAX_MEMORY_ENTRIES;
                    memoryLog.subList(0, overflow).clear();
                }

                // Sync with MemorySystem
                if (memorySystem != null) {
                    memorySystem.recordEvent(entry);
                }

                updateEmotionalState();
                // Pass event details through callback
                VillagerEmotionEvent event = new VillagerEmotionEvent(
                        this.villager,
                        currentState,
                        eventType,
                        description
                );
                EventDispatcher.publish(event);
                callback.run();
            } catch (Exception e) {
                e.printStackTrace(); // Log exceptions properly later
            }
        });
    }

    private void updateEmotionalState() {
        EmotionalState newState = currentState;
        if (moodScore >= 60) newState = EmotionalState.HAPPY;
        else if (moodScore <= -40) newState = EmotionalState.ANGRY;
        else if (moodScore <= -10) newState = EmotionalState.SAD;
        else newState = EmotionalState.NEUTRAL;

        if (newState != currentState) {
            currentState = newState;
            // Dispatch event
            VillagerEmotionEvent event = new VillagerEmotionEvent(
                    this.villager,
                    currentState,
                    "STATE_CHANGE",
                    "Emotional state changed to " + currentState
            );
            EventDispatcher.publish(event);
        }
    }

    public void registerEmotionEventHandler(Consumer<VillagerEmotionEvent> handler) {
        EventDispatcher.registerHandler(VillagerEmotionEvent.class, handler);
    }

    public EmotionalState getCurrentState() {
        return currentState;
    }

    public int getMoodScore() {
        return moodScore;
    }

    public void setMemorySystem(MemorySystem memorySystem) {
        this.memorySystem = memorySystem;
    }
}
