package com.beeny.data;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import com.beeny.constants.VillagerConstants;
import com.beeny.constants.VillagerConstants.PersonalityType;
import com.beeny.constants.VillagerConstants.HobbyType;
import java.util.concurrent.ThreadLocalRandom;

public class VillagerData {
    // Split into multiple codecs to avoid method parameter limit
    private static final Codec<VillagerData> BASIC_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            EmotionalState.CODEC.fieldOf("emotionalState").orElse(new EmotionalState()).forGetter(VillagerData::getEmotionalState),
            AIState.CODEC.fieldOf("aiState").orElse(new AIState()).forGetter(VillagerData::getAiState),
            LearningProfile.CODEC.fieldOf("learningProfile").orElse(new LearningProfile()).forGetter(VillagerData::getLearningProfile),
            ProfessionData.CODEC.fieldOf("professionData").orElse(new ProfessionData()).forGetter(VillagerData::getProfessionData),
            Codec.STRING.fieldOf("name").orElse(VillagerConstants.Defaults.NAME).forGetter(VillagerData::getName),
            Codec.INT.fieldOf("age").orElse(VillagerConstants.Defaults.AGE).forGetter(VillagerData::getAge),
            Codec.STRING.fieldOf("gender").orElse(VillagerConstants.Defaults.GENDER).forGetter(VillagerData::getGender),
            Codec.STRING.fieldOf("personality")
                .orElse(PersonalityType.FRIENDLY.name())
                .xmap(PersonalityType::fromString, PersonalityType::name)
                .forGetter(VillagerData::getPersonality)
        ).apply(instance, (emotionalState, aiState, learningProfile, professionData, name, age, gender, personality) -> {
            VillagerData data = new VillagerData();
            data.emotionalState = emotionalState;
            data.aiState = aiState;
            data.learningProfile = learningProfile;
            data.professionData = professionData;
            data.name = name;
            data.age = age;
            data.gender = gender;
            data.personality = personality;
            return data;
        })
    );
    
    public static final Codec<VillagerData> CODEC = BASIC_CODEC;

    private EmotionalState emotionalState;
    private AIState aiState;
    private LearningProfile learningProfile;
    private ProfessionData professionData;
    private String name;
    private int age;
    private String gender;
    private PersonalityType personality;
    private int happiness;
    private int totalTrades;
    private String favoritePlayerId;
    private List<String> professionHistory;
    private Map<String, Integer> playerRelations;
    private List<String> familyMembers;
    private String spouseName;
    private String spouseId;
    private List<String> childrenIds;
    private List<String> childrenNames;
    private String favoriteFood;
    private HobbyType hobby;
    private long birthTime;
    private String birthPlace;
    private String notes;
    private long deathTime;
    private boolean isAlive;
    private Map<String, String> playerMemories;
    private Map<String, Integer> topicFrequency;
    // Use a thread-safe, non-blocking deque for concurrent access
    private ConcurrentLinkedDeque<String> recentEvents;
    private long lastConversationTime;

    // Default constructor for codec
    public VillagerData() {
        this.emotionalState = new EmotionalState();
        this.aiState = new AIState();
        this.learningProfile = new LearningProfile();
        this.professionData = new ProfessionData();
        this.name = VillagerConstants.Defaults.NAME;
        this.age = VillagerConstants.Defaults.AGE;
        this.gender = VillagerConstants.Defaults.GENDER;
        this.personality = PersonalityType.FRIENDLY;
        this.happiness = VillagerConstants.Defaults.HAPPINESS;
        this.totalTrades = VillagerConstants.Defaults.TOTAL_TRADES;
        this.favoritePlayerId = VillagerConstants.Defaults.FAVORITE_PLAYER_ID;
        this.professionHistory = new ArrayList<>();
        this.playerRelations = new HashMap<>();
        this.familyMembers = new ArrayList<>();
        this.spouseName = VillagerConstants.Defaults.SPOUSE_NAME;
        this.spouseId = VillagerConstants.Defaults.SPOUSE_ID;
        this.childrenIds = new ArrayList<>();
        this.childrenNames = new ArrayList<>();
        this.favoriteFood = VillagerConstants.Defaults.FAVORITE_FOOD;
        this.hobby = HobbyType.GARDENING;
        this.birthTime = System.currentTimeMillis();
        this.birthPlace = VillagerConstants.Defaults.BIRTH_PLACE;
        this.notes = VillagerConstants.Defaults.NOTES;
        this.deathTime = VillagerConstants.Defaults.DEATH_TIME;
        this.isAlive = true;
        this.playerMemories = new HashMap<>();
        this.topicFrequency = new HashMap<>();
        // Initialize as thread-safe deque
        this.recentEvents = new ConcurrentLinkedDeque<>();
        this.lastConversationTime = 0L;
    }

    public VillagerData(EmotionalState emotionalState, AIState aiState, LearningProfile learningProfile, ProfessionData professionData,
                       String name, int age, String gender, PersonalityType personality, int happiness, int totalTrades,
                       String favoritePlayerId, List<String> professionHistory, Map<String, Integer> playerRelations,
                       List<String> familyMembers, String spouseName, String spouseId, List<String> childrenIds,
                       List<String> childrenNames, String favoriteFood, HobbyType hobby, long birthTime, String birthPlace,
                       String notes, long deathTime, boolean isAlive, Map<String, String> playerMemories,
                       Map<String, Integer> topicFrequency, List<String> recentEvents, long lastConversationTime) {
        this.emotionalState = emotionalState != null ? emotionalState : new EmotionalState();
        this.aiState = aiState != null ? aiState : new AIState();
        this.learningProfile = learningProfile != null ? learningProfile : new LearningProfile();
        this.professionData = professionData != null ? professionData : new ProfessionData();
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.personality = personality;
        this.happiness = happiness;
        this.totalTrades = totalTrades;
        this.favoritePlayerId = favoritePlayerId;
        this.professionHistory = new ArrayList<>(professionHistory);
        this.playerRelations = new HashMap<>(playerRelations);
        this.familyMembers = new ArrayList<>(familyMembers);
        this.spouseName = spouseName;
        this.spouseId = spouseId;
        this.childrenIds = new ArrayList<>(childrenIds);
        this.childrenNames = new ArrayList<>(childrenNames);
        this.favoriteFood = favoriteFood;
        this.hobby = hobby;
        this.birthTime = birthTime;
        this.birthPlace = birthPlace;
        this.notes = notes;
        this.deathTime = deathTime;
        this.isAlive = isAlive;
        this.playerMemories = new HashMap<>(playerMemories);
        this.topicFrequency = new HashMap<>(topicFrequency);
        this.recentEvents = new ConcurrentLinkedDeque<>(recentEvents);
        this.lastConversationTime = lastConversationTime;
    }

    // Getters
    public EmotionalState getEmotionalState() { return emotionalState; }
    public AIState getAiState() { return aiState; }
    public LearningProfile getLearningProfile() { return learningProfile; }
    public ProfessionData getProfessionData() { return professionData; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public PersonalityType getPersonality() { return personality; }
    public int getHappiness() { return happiness; }
    public int getTotalTrades() { return totalTrades; }
    public String getFavoritePlayerId() { return favoritePlayerId; }
    public List<String> getProfessionHistory() { return Collections.unmodifiableList(professionHistory); }
    public Map<String, Integer> getPlayerRelations() { return Collections.unmodifiableMap(playerRelations); }
    public List<String> getFamilyMembers() { return Collections.unmodifiableList(familyMembers); }
    public String getSpouseName() { return spouseName; }
    public String getSpouseId() { return spouseId; }
    public List<String> getChildrenIds() { return Collections.unmodifiableList(childrenIds); }
    public List<String> getChildrenNames() { return Collections.unmodifiableList(childrenNames); }
    public String getFavoriteFood() { return favoriteFood; }
    public HobbyType getHobby() { return hobby; }
    public long getBirthTime() { return birthTime; }
    public String getBirthPlace() { return birthPlace; }
    public String getNotes() { return notes; }
    public long getDeathTime() { return deathTime; }
    public boolean isAlive() { return isAlive; }
    public Map<String, String> getPlayerMemories() { return Collections.unmodifiableMap(playerMemories); }
    public Map<String, Integer> getTopicFrequency() { return Collections.unmodifiableMap(topicFrequency); }
    public List<String> getRecentEvents() { return Collections.unmodifiableList(new ArrayList<>(recentEvents)); }
    public long getLastConversationTime() { return lastConversationTime; }

    // Setters
    public void setEmotionalState(EmotionalState emotionalState) { this.emotionalState = emotionalState; }
    public void setAiState(AIState aiState) { this.aiState = aiState; }
    public void setLearningProfile(LearningProfile learningProfile) { this.learningProfile = learningProfile; }
    public void setProfessionData(ProfessionData professionData) { this.professionData = professionData; }
    public void setName(String name) { this.name = name; }
    public void setAge(int age) { this.age = age; }
    public void setGender(String gender) { this.gender = gender; }
    public void setPersonality(PersonalityType personality) { this.personality = personality; }
    public void setHappiness(int happiness) { this.happiness = Math.max(0, Math.min(100, happiness)); }
    public void setTotalTrades(int totalTrades) { this.totalTrades = Math.max(0, totalTrades); }
    public void setFavoritePlayerId(String favoritePlayerId) { this.favoritePlayerId = favoritePlayerId; }
    public void setProfessionHistory(List<String> professionHistory) { this.professionHistory = new ArrayList<>(professionHistory); }
    public void setPlayerRelations(Map<String, Integer> playerRelations) { this.playerRelations = new HashMap<>(playerRelations); }
    public void setFamilyMembers(List<String> familyMembers) { this.familyMembers = new ArrayList<>(familyMembers); }
    public void setSpouseName(String spouseName) { this.spouseName = spouseName; }
    public void setSpouseId(String spouseId) { this.spouseId = spouseId; }
    public void setChildrenIds(List<String> childrenIds) { this.childrenIds = new ArrayList<>(childrenIds); }
    public void setChildrenNames(List<String> childrenNames) { this.childrenNames = new ArrayList<>(childrenNames); }
    public void setFavoriteFood(String favoriteFood) { this.favoriteFood = favoriteFood; }
    public void setHobby(HobbyType hobby) { this.hobby = hobby; }
    public void setBirthTime(long birthTime) { this.birthTime = birthTime; }
    public void setBirthPlace(String birthPlace) { this.birthPlace = birthPlace; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setDeathTime(long deathTime) { this.deathTime = deathTime; }
    public void setAlive(boolean alive) { isAlive = alive; }
    public void setPlayerMemories(Map<String, String> playerMemories) { this.playerMemories = new HashMap<>(playerMemories); }
    public void setTopicFrequency(Map<String, Integer> topicFrequency) { this.topicFrequency = new HashMap<>(topicFrequency); }
    public void setRecentEvents(List<String> recentEvents) {
        this.recentEvents = new ConcurrentLinkedDeque<>(recentEvents);
        // Trim to the latest 10 if needed, keeping the most recent at the end
        while (this.recentEvents.size() > 10) {
            this.recentEvents.pollFirst();
        }
    }
    public void setLastConversationTime(long lastConversationTime) { this.lastConversationTime = lastConversationTime; }

    // Utility methods
    public void addProfession(String profession) {
        professionHistory.add(profession);
    }

    public void addFamilyMember(String member) {
        familyMembers.add(member);
    }

    public void addChild(String childId, String childName) {
        childrenIds.add(childId);
        childrenNames.add(childName);
    }

    public void addRecentEvent(String event) {
        // Append at the end and trim from the front to keep size ≤ 10
        recentEvents.addLast(event);
        while (recentEvents.size() > 10) {
            recentEvents.pollFirst();
        }
    }

    public void incrementPlayerRelation(String playerId) {
        playerRelations.put(playerId, playerRelations.getOrDefault(playerId, 0) + 1);
    }

    public void addPlayerMemory(String playerId, String memory) {
        playerMemories.put(playerId, memory);
    }
    
    public String getPlayerMemory(String playerId) {
        return playerMemories.get(playerId);
    }
    
    public void setPlayerMemory(String playerId, String memory) {
        playerMemories.put(playerId, memory);
    }
    
    public int getPlayerReputation(String playerId) {
        return playerRelations.getOrDefault(playerId, 0);
    }
    
    public void updateLastConversationTime() {
        this.lastConversationTime = System.currentTimeMillis();
    }
    
    public String getHappinessDescription() {
        if (happiness >= VillagerConstants.Happiness.VERY_HAPPY_THRESHOLD) {
            return VillagerConstants.Happiness.VERY_HAPPY;
        } else if (happiness >= VillagerConstants.Happiness.HAPPY_THRESHOLD) {
            return VillagerConstants.Happiness.HAPPY;
        } else if (happiness >= VillagerConstants.Happiness.CONTENT_THRESHOLD) {
            return VillagerConstants.Happiness.CONTENT;
        } else if (happiness >= VillagerConstants.Happiness.UNHAPPY_THRESHOLD) {
            return VillagerConstants.Happiness.UNHAPPY;
        } else {
            return VillagerConstants.Happiness.MISERABLE;
        }
    }
    
    public void incrementTrades() {
        this.totalTrades++;
    }
    
    public void adjustHappiness(int delta) {
        setHappiness(this.happiness + delta);
    }
    
    public void updatePlayerRelation(String playerId, int amount) {
        playerRelations.put(playerId, playerRelations.getOrDefault(playerId, 0) + amount);
    }

    public void incrementTopicFrequency(String topic) {
        topicFrequency.put(topic, topicFrequency.getOrDefault(topic, 0) + 1);
    }
    
    public void updateTopicFrequency(String topic, int count) {
        topicFrequency.put(topic, Math.max(0, count));
    }

    public VillagerData copy() {
        return new VillagerData(
            emotionalState.copy(), aiState.copy(), learningProfile.copy(), professionData.copy(),
            name, age, gender, personality, happiness, totalTrades, favoritePlayerId,
            new ArrayList<>(professionHistory), new HashMap<>(playerRelations),
            new ArrayList<>(familyMembers), spouseName, spouseId,
            new ArrayList<>(childrenIds), new ArrayList<>(childrenNames),
            favoriteFood, hobby, birthTime, birthPlace, notes, deathTime, isAlive,
            new HashMap<>(playerMemories), new HashMap<>(topicFrequency),
            new ArrayList<>(recentEvents), lastConversationTime
        );
    }
}