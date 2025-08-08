package com.beeny.config;

public final class AIConfig {
    private AIConfig() {}

    // Tool actions
    public static final int HOE_RADIUS = 5;
    public static final float GROWTH_BOOST = 0.1f;
    public static final int FISHING_RADIUS = 8;
    public static final double FISHING_SUCCESS_CHANCE = 0.7;
    
    // AI timing
    public static final int AI_MANAGER_UPDATE_INTERVAL = 5000;
    public static final int AI_GOAP_UPDATE_INTERVAL = 3000;
    public static final int AI_EMOTION_UPDATE_INTERVAL = 60000;
    
    // Gossip system
    public static final int GOSSIP_POSITIVE_INTERACTION = 5;
    public static final int GOSSIP_NEGATIVE_INTERACTION = -8;
    public static final int GOSSIP_TRADE_SUCCESS = 3;
    public static final int GOSSIP_TRADE_EXPLOIT = -12;
    public static final int GOSSIP_KINDNESS = 8;
    public static final int GOSSIP_AGGRESSION = -15;
    public static final int GOSSIP_HELPFULNESS = 6;
    public static final int GOSSIP_ROMANCE = 2;
    public static final int GOSSIP_SCANDAL = -5;
    public static final int GOSSIP_ACHIEVEMENT = 10;
    
    // Emotional thresholds
    public static final float EMOTION_DECAY_RATE = 0.01f;
    public static final float STRESS_HIGH_THRESHOLD = 70.0f;
    public static final float STRESS_MEDIUM_THRESHOLD = 40.0f;
    public static final float LONELINESS_HIGH_THRESHOLD = 70.0f;
    public static final float LONELINESS_MEDIUM_THRESHOLD = 50.0f;
    
    // Quest system
    public static final int QUEST_COOLDOWN_MS = 600000;
    public static final int QUEST_CLEANUP_AGE_MS = 86400000;
    public static final float QUEST_OFFER_CHANCE = 0.1f;
    
    // Learning system
    public static final long RECENT_EXPERIENCE_THRESHOLD_MS = 3600000;
    public static final float DAILY_DECAY_MS = 86400000f;
    public static final int MIN_EXPERIENCES_FOR_ADAPTATION = 3;
    public static final double NEGATIVE_OUTCOME_THRESHOLD = -0.3;
    
    // Social interaction
    public static final float SOCIAL_RADIUS = 15.0f;
    public static final float FAMILY_RELATION_BONUS = 1.5f;
    public static final float SPOUSE_RELATION_BONUS = 1.8f;
    
    // Personality modifiers
    public static final float CURIOUS_PERSONALITY_MOD = 1.6f;
    public static final float FRIENDLY_PERSONALITY_MOD = 1.4f;
    public static final float GRUMPY_PERSONALITY_MOD = 1.2f;
    public static final float SHY_PERSONALITY_MOD = 0.6f;
    public static final float SERIOUS_PERSONALITY_MOD = 0.8f;
    
    // Chat system
    public static final double CHAT_DETECTION_RADIUS = 10.0;
    public static final double BROADCAST_RADIUS = 20.0;
    public static final double TOOL_ACTION_RADIUS = 15.0;
    
    // AI priorities
    public static final int MAX_PROMPT_LENGTH = 200;
    public static final int AI_MANAGER_PRIORITY = 50;
}