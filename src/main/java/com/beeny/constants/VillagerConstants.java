package com.beeny.constants;

/**
 * Constants to replace magic strings throughout the codebase
 */
public final class VillagerConstants {
    
    // NBT Keys
    public static final class NBT {
        public static final String NAME = "name";
        public static final String AGE = "age";
        public static final String GENDER = "gender";
        public static final String PERSONALITY = "personality";
        public static final String HAPPINESS = "happiness";
        public static final String TOTAL_TRADES = "totalTrades";
        public static final String FAVORITE_PLAYER_ID = "favoritePlayerId";
        public static final String PROFESSION_HISTORY = "professionHistory";
        public static final String PLAYER_RELATIONS = "playerRelations";
        public static final String FAMILY_MEMBERS = "familyMembers";
        public static final String SPOUSE_NAME = "spouseName";
        public static final String SPOUSE_ID = "spouseId";
        public static final String CHILDREN_IDS = "childrenIds";
        public static final String CHILDREN_NAMES = "childrenNames";
        public static final String FAVORITE_FOOD = "favoriteFood";
        public static final String PLAYER_MEMORIES = "playerMemories";
        public static final String TOPIC_FREQUENCY = "topicFrequency";
        public static final String RECENT_EVENTS = "recentEvents";
        public static final String LAST_CONVERSATION_TIME = "lastConversationTime";
        public static final String IS_ALIVE = "isAlive";
        public static final String HOBBY = "hobby";
        public static final String BIRTH_TIME = "birthTime";
        public static final String BIRTH_PLACE = "birthPlace";
        public static final String NOTES = "notes";
        public static final String DEATH_TIME = "deathTime";
        public static final String AI_STATE = "ai_state";
        public static final String CURRENT_GOAL = "current_goal";
        public static final String CURRENT_ACTION = "current_action";
        public static final String IS_AI_ACTIVE = "is_ai_active";
        }
    
        public static final class Relationship {
            public static final double MARRIAGE_RANGE = 10.0;
            public static final int MIN_MARRIAGE_AGE = 100;
            public static final int MARRIAGE_COOLDOWN = 1000;
            public static final int PROPOSAL_TIME_THRESHOLD = 12000;
        }
    
    // Personalities
    public enum PersonalityType {
        FRIENDLY,
        GRUMPY,
        SHY,
        ENERGETIC,
        LAZY,
        CURIOUS,
        SERIOUS,
        CHEERFUL,
        NERVOUS,
        CONFIDENT;
    }

    // Relationship constants
    public static final class Relationship {
        public static final double MARRIAGE_RANGE = 10.0;
        public static final int MIN_MARRIAGE_AGE = 100;
        public static final int MARRIAGE_COOLDOWN = 1000;
        public static final int PROPOSAL_TIME_THRESHOLD = 12000;
    }

        public static PersonalityType fromString(String value) {
            for (PersonalityType type : values()) {
                if (type.name().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            // Default fallback
            return FRIENDLY;
        }

        public static String toString(PersonalityType type) {
            return type.name().substring(0, 1).toUpperCase() + type.name().substring(1).toLowerCase();
        }
    }
    
    // Hobbies
    public enum HobbyType {
        GARDENING,
        READING,
        FISHING,
        COOKING,
        SINGING,
        DANCING,
        CRAFTING,
        EXPLORING,
        COLLECTING,
        GOSSIPING;

        public static HobbyType fromString(String value) {
            for (HobbyType type : values()) {
                if (type.name().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            // Default fallback
            return GARDENING;
        }

        public static String toString(HobbyType type) {
            return type.name().substring(0, 1).toUpperCase() + type.name().substring(1).toLowerCase();
        }
    }
    
    // Gender
    public static final class Gender {
        public static final String MALE = "Male";
        public static final String FEMALE = "Female";
        public static final String UNKNOWN = "Unknown";
        
        public static final String[] ALL = { MALE, FEMALE };
    }
    
    // Age Categories
    public static final class AgeCategory {
        public static final int BABY_MAX = 20;
        public static final int YOUNG_ADULT_MAX = 100;
        public static final int ADULT_MAX = 300;
        
        public static final String BABY = "Baby";
        public static final String YOUNG_ADULT = "Young Adult";
        public static final String ADULT = "Adult";
        public static final String ELDER = "Elder";
    }
    
    // Happiness Levels
    public static final class Happiness {
        public static final int MIN = 0;
        public static final int MAX = 100;
        public static final int DEFAULT = 50;
        
        public static final int VERY_HAPPY_THRESHOLD = 80;
        public static final int HAPPY_THRESHOLD = 60;
        public static final int CONTENT_THRESHOLD = 40;
        public static final int UNHAPPY_THRESHOLD = 20;
        
        public static final String VERY_HAPPY = "Very Happy";
        public static final String HAPPY = "Happy";
        public static final String CONTENT = "Content";
        public static final String UNHAPPY = "Unhappy";
        public static final String MISERABLE = "Miserable";
    }
    
    // AI Context Keys
    public static final class AIContext {
        public static final String LAST_INITIALIZATION = "last_initialization";
        public static final String DOMINANT_EMOTION = "dominant_emotion";
        public static final String EMOTIONAL_DESCRIPTION = "emotional_description";
        public static final String LAST_PLAYER_INTERACTION = "last_player_interaction";
        public static final String LAST_INTERACTING_PLAYER = "last_interacting_player";
        public static final String MARRIAGE_CANDIDATES = "marriage_candidates";
        public static final String LEARNING_ANALYTICS = "learning_analytics";
        public static final String INITIALIZED_AT = "initialized_at";
    }
    
    // Event Types
    public static final class EventType {
        public static final String MARRIAGE = "marriage";
        public static final String BIRTH = "birth";
        public static final String DEATH = "death";
        public static final String TRADE = "trade";
        public static final String CONVERSATION = "conversation";
        public static final String PROFESSION_CHANGE = "profession_change";
        public static final String RELATIONSHIP_CHANGE = "relationship_change";
    }
    
    // Emotional Reasons
    public static final class EmotionalReason {
        public static final String PERSONALITY_INITIALIZATION = "personality_initialization";
        public static final String DAWN_PEACE = "dawn_peace";
        public static final String NIGHT_ANXIETY = "night_anxiety";
        public static final String RAINY_WEATHER = "rainy_weather";
        public static final String ISOLATION = "isolation";
        public static final String SOCIAL_ENVIRONMENT = "social_environment";
        public static final String MARRIED_LIFE = "married_life";
        public static final String FAMILY_JOY = "family_joy";
        public static final String WORK_EXPERIENCE = "work_experience";
        public static final String UNFAIR_TRADE = "unfair_trade";
    }
    
    // Time Constants
    public static final class Time {
        public static final long MINUTE_MS = 60_000L;
        public static final long HOUR_MS = 3_600_000L;
        public static final long DAY_MS = 86_400_000L;
        public static final long INTERACTION_RATE_LIMIT_MS = 1000L;
    }
    
    // Default Values
    public static final class Defaults {
        public static final String NAME = "";
        public static final String GENDER = Gender.UNKNOWN;
        public static final String PERSONALITY = "FRIENDLY";
        public static final String FAVORITE_FOOD = "";
        public static final String SPOUSE_NAME = "";
        public static final String SPOUSE_ID = "";
        public static final String FAVORITE_PLAYER_ID = "";
        public static final String BIRTH_PLACE = "";
        public static final String NOTES = "";
        public static final int AGE = 0;
        public static final int HAPPINESS = Happiness.DEFAULT;
        public static final int TOTAL_TRADES = 0;
        public static final long BIRTH_TIME = 0L;
        public static final long DEATH_TIME = 0L;
        public static final long LAST_CONVERSATION_TIME = 0L;
        public static final boolean IS_ALIVE = true;
    }
    
    // Search radii for villager commands
    public static final class SearchRadius {
        public static final double NEAREST = 10.0;
        public static final double LIST = 50.0;
        public static final double FIND = 100.0;
        public static final double RANDOMIZE = 50.0;
    }
    
    // Dialogue categories for suggestions and validation
    public static final class DialogueCategory {
        public static final String GREETING = "greeting";
        public static final String WEATHER = "weather";
        public static final String WORK = "work";
        public static final String FAMILY = "family";
        public static final String GOSSIP = "gossip";
        public static final String TRADE = "trade";
        public static final String HOBBY = "hobby";
        public static final String MOOD = "mood";
        public static final String ADVICE = "advice";
        public static final String STORY = "story";
        public static final String FAREWELL = "farewell";
        public static final String[] ALL = {
            GREETING, WEATHER, WORK, FAMILY, GOSSIP, TRADE, HOBBY, MOOD, ADVICE, STORY, FAREWELL
        };
    }
    
    // Limits
    public static final class Limits {
        public static final int MAX_RECENT_EVENTS = 5;
        public static final int MAX_STRING_LENGTH = 32767;
        public static final int MARRIAGE_BONUS_HAPPINESS = 20;
        public static final int CHILD_BONUS_HAPPINESS = 10;
        public static final int WIDOWED_HAPPINESS_PENALTY = -15;
    }
    
    // Private constructor to prevent instantiation
    private VillagerConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}