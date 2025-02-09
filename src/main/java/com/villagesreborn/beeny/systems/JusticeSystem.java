package com.villagesreborn.beeny.systems;

import java.util.concurrent.*;
import java.util.*;
import com.villagesreborn.beeny.entities.Villager;
import com.villagesreborn.beeny.systems.emotion.EmotionManager;

public class JusticeSystem {
    private final Queue<CrimeEvent> crimeLog = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final EmotionManager emotionManager;
    private final TaskSystem taskSystem;

    public JusticeSystem(EmotionManager emotionManager, TaskSystem taskSystem) {
        this.emotionManager = emotionManager;
        this.taskSystem = taskSystem;
        executor.scheduleAtFixedRate(this::processCrimes, 5, 5, TimeUnit.SECONDS);
    }

    public void logCrime(CrimeEvent crime) {
        crimeLog.add(crime);
        emotionManager.recordCommunityImpact(crime.getSeverity());
        EventBus.post(new CrimeCommittedEvent(crime, System.currentTimeMillis()));
    }

    private void processCrimes() {
        CrimeEvent crime;
        while ((crime = crimeLog.poll()) != null) {
            handleCrime(crime);
            EventBus.post(new JusticeEnactedEvent(crime, System.currentTimeMillis()));
        }
    }

    private void handleCrime(CrimeEvent crime) {
        switch (crime.getType()) {
            case THEFT:
                taskSystem.assignTask(new BountyTask(crime.getPerpetrator(), 300));
                break;
            case MURDER:
                taskSystem.assignTask(new BountyTask(crime.getPerpetrator(), 1000));
                emotionManager.applyCommunityTrauma(0.2f);
                break;
            case VANDALISM:
                taskSystem.assignTask(new FineTask(crime.getPerpetrator(), 50));
                break;
        }
    }

    public void clearCrimeLog() {
        crimeLog.clear();
    }