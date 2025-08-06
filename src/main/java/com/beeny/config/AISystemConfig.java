package com.beeny.config;

/**
 * Configuration constants for the AI system.
 * Centralizes hardcoded values that were previously scattered throughout the codebase.
 */
public class AISystemConfig {
    
    // Update intervals (in milliseconds)
    public static final long EMOTION_SYSTEM_UPDATE_INTERVAL = 60000; // 1 minute
    public static final long HOBBY_SYSTEM_UPDATE_INTERVAL = 20000; // 20 seconds
    public static final long SCHEDULE_MANAGER_UPDATE_INTERVAL = 10000; // 10 seconds
    public static final long PERSONALITY_BEHAVIOR_UPDATE_INTERVAL = 15000; // 15 seconds
    public static final long AI_MANAGER_UPDATE_INTERVAL = 5000; // 5 seconds
    public static final long LEARNING_SYSTEM_UPDATE_INTERVAL = 30000; // 30 seconds
    public static final long PLANNING_SYSTEM_UPDATE_INTERVAL = 3000; // 3 seconds
    
    // Maintenance intervals
    public static final long MAINTENANCE_INTERVAL = 300000; // 5 minutes
    public static final long CLEANUP_THRESHOLD = 600000; // 10 minutes
    
    // Villager interaction timing (in ticks)
    public static final int GREETING_COOLDOWN = 60; // 3 seconds
    public static final int AI_UPDATE_FREQUENCY = 60; // Every 3 seconds
    public static final int HOBBY_UPDATE_FREQUENCY = 300; // 15 seconds
    public static final int EMOTIONAL_UPDATE_FREQUENCY = 200; // 10 seconds
    public static final int PERSONALITY_UPDATE_FREQUENCY = 150; // 7.5 seconds
    public static final int MEMORY_UPDATE_FREQUENCY = 600; // 30 seconds
    public static final int LIFECYCLE_UPDATE_FREQUENCY = 20; // 1 second
    public static final int HAPPINESS_UPDATE_FREQUENCY = 100; // 5 seconds
    public static final int SPOUSE_CHECK_FREQUENCY = 400; // 20 seconds
    
    // Personality behavior probabilities
    public static final float FRIENDLY_SOCIAL_CHANCE = 0.05f;
    public static final float CHEERFUL_HAPPINESS_CHANCE = 0.08f;
    public static final float CHEERFUL_RECOVERY_CHANCE = 0.05f;
    public static final float GRUMPY_SOUND_CHANCE = 0.02f;
    public static final float GRUMPY_DECAY_CHANCE = 0.1f;
    public static final float ENERGETIC_PARTICLE_CHANCE = 0.1f;
    public static final float CALM_SOOTHING_CHANCE = 0.04f;
    public static final float CONFIDENT_BOOST_CHANCE = 0.03f;
    public static final float CONFIDENT_SHY_HELP_CHANCE = 0.05f;
    public static final float CURIOUS_OBSERVATION_CHANCE = 0.06f;
    public static final float LAZY_DECAY_CHANCE = 0.08f;
    public static final float SERIOUS_CHEERFUL_ANNOYANCE_CHANCE = 0.02f;
    public static final float PERSONALITY_HAPPINESS_CHANCE = 0.1f;
    public static final float SPOUSE_MISSING_CHANCE = 0.1f;
    public static final float SPOUSE_TIME_CHANCE = 0.3f;
    
    // Search and interaction distances
    public static final int VILLAGER_SEARCH_RADIUS_SMALL = 5;
    public static final int VILLAGER_SEARCH_RADIUS_MEDIUM = 8;
    public static final int VILLAGER_SEARCH_RADIUS_LARGE = 12;
    public static final int VILLAGER_SEARCH_RADIUS_EXTRA_LARGE = 15;
    public static final int VILLAGER_SEARCH_RADIUS_MAX = 50;
    public static final int PLAYER_SEARCH_RADIUS = 8;
    public static final int PLAYER_SEARCH_RADIUS_LARGE = 10;
    public static final double TRADE_INTERACTION_DISTANCE = 5.0;
    
    // Particle effects
    public static final int HEART_PARTICLE_COUNT = 3;
    public static final int HAPPY_PARTICLE_COUNT = 8;
    public static final int CRIT_PARTICLE_COUNT = 3;
    public static final int END_ROD_PARTICLE_COUNT = 3;
    public static final double PARTICLE_SPREAD = 0.5;
    public static final double PARTICLE_SPEED = 0.1;
    
    // Happiness and reputation thresholds
    public static final int HAPPINESS_HIGH_THRESHOLD = 70;
    public static final int HAPPINESS_MEDIUM_THRESHOLD = 40;
    public static final int HAPPINESS_GRUMPY_THRESHOLD = 60;
    public static final int REPUTATION_HIGH_THRESHOLD = 20;
    public static final int REPUTATION_NEUTRAL_THRESHOLD = -10;
    public static final int REPUTATION_FRIENDLY_HIGH = 30;
    public static final int REPUTATION_GRUMPY_HIGH = 50;
    public static final int REPUTATION_SHY_HIGH = 40;
    public static final int REPUTATION_ANXIOUS_HIGH = 60;
    public static final int REPUTATION_SERIOUS_HIGH = 35;
    
    // Happiness adjustments
    public static final int HAPPINESS_SOCIAL_BOOST = 2;
    public static final int HAPPINESS_MINOR_BOOST = 1;
    public static final int HAPPINESS_MINOR_PENALTY = -1;
    public static final int HAPPINESS_MAJOR_PENALTY = -3;
    public static final int HAPPINESS_SEVERE_PENALTY = -15;
    public static final int HAPPINESS_FAMILY_DEATH_PENALTY = -20;
    public static final int HAPPINESS_ANXIOUS_THUNDER_PENALTY = -10;
    public static final int HAPPINESS_TRADE_BOOST = 2;
    
    // Crowd thresholds
    public static final int CROWD_SIZE_SMALL = 2;
    public static final int CROWD_SIZE_MEDIUM = 3;
    public static final int CROWD_SIZE_LARGE = 4;
    
    // Time periods (in world ticks, 24000 = 1 day)
    public static final long TIME_DAWN_START = 0;
    public static final long TIME_DAWN_END = 1000;
    public static final long TIME_MORNING_START = 1000;
    public static final long TIME_MORNING_END = 6000;
    public static final long TIME_NOON_START = 6000;
    public static final long TIME_NOON_END = 7000;
    public static final long TIME_AFTERNOON_START = 7000;
    public static final long TIME_AFTERNOON_END = 11000;
    public static final long TIME_DUSK_START = 11000;
    public static final long TIME_DUSK_END = 13000;
    public static final long TIME_NIGHT_START = 13000;
    public static final long TIME_NIGHT_END = 23000;
    public static final long TIME_MIDNIGHT_START = 23000;
    public static final long TIME_MIDNIGHT_END = 24000;
    
    // Working hours for serious personality
    public static final long WORK_TIME_START = 6000;
    public static final long WORK_TIME_END = 12000;
    
    // Lazy personality time preferences  
    public static final long LAZY_DISLIKE_START = 12000; // Noon
    public static final long LAZY_DISLIKE_END = 14000;
    public static final long LAZY_LIKE_START = 2000; // Late morning
    public static final long LAZY_LIKE_END = 4000;
    
    // Energetic personality time preferences
    public static final long ENERGETIC_ACTIVE_START = 1000; // Morning
    public static final long ENERGETIC_ACTIVE_END = 11000;
    
    // Trade calculation
    public static final int TRADE_VALUE_DIVISOR = 10;
    public static final int MIN_TRADE_REPUTATION_GAIN = 1;
    
    // Age thresholds
    public static final int YOUNG_VILLAGER_AGE = 20;
    public static final int ELDER_VILLAGER_AGE = 300;
    
    // Sound settings
    public static final float VILLAGER_SOUND_VOLUME = 0.8f;
    public static final float VILLAGER_SOUND_PITCH = 0.8f;
    
    // Async processing
    public static final int ASYNC_EXECUTOR_CORE_THREADS = 2;
    public static final int ASYNC_EXECUTOR_MAX_THREADS = 8;
    public static final long ASYNC_EXECUTOR_KEEP_ALIVE = 60L; // seconds
    public static final int ASYNC_EXECUTOR_QUEUE_SIZE = 100;
    
    // Performance limits
    public static final int MAX_VILLAGERS_PER_UPDATE = 50;
    public static final long MAX_UPDATE_TIME_MS = 50; // 2.5 ticks max
    
    // Prevent instantiation
    private AISystemConfig() {}
}