package com.beeny.data;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;
import com.beeny.constants.VillagerConstants;
import com.beeny.constants.VillagerConstants.PersonalityType;
import com.beeny.constants.VillagerConstants.HobbyType;
import java.util.concurrent.ThreadLocalRandom;

public class VillagerData {
    public static final Codec<VillagerData> CODEC = RecordCodecBuilder.create(instance ->
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
                .forGetter(VillagerData::getPersonality),
            Codec.INT.fieldOf("happiness").orElse(VillagerConstants.Defaults.HAPPINESS).forGetter(VillagerData::getHappiness),
            Codec.INT.fieldOf("totalTrades").orElse(VillagerConstants.Defaults.TOTAL_TRADES).forGetter(VillagerData::getTotalTrades),
            Codec.STRING.fieldOf("favoritePlayerId").orElse(VillagerConstants.Defaults.FAVORITE_PLAYER_ID).forGetter(VillagerData::getFavoritePlayerId),
            Codec.list(Codec.STRING).fieldOf("professionHistory").orElse(Collections.emptyList()).forGetter(VillagerData::getProfessionHistory),
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("playerRelations").orElse(Collections.emptyMap()).forGetter(VillagerData::getPlayerRelations),
            Codec.list(Codec.STRING).fieldOf("familyMembers").orElse(Collections.emptyList()).forGetter(VillagerData::getFamilyMembers),
            Codec.STRING.fieldOf("spouseName").orElse(VillagerConstants.Defaults.SPOUSE_NAME).forGetter(VillagerData::getSpouseName),
            Codec.STRING.fieldOf("spouseId").orElse(VillagerConstants.Defaults.SPOUSE_ID).forGetter(VillagerData::getSpouseId),
            Codec.list(Codec.STRING).fieldOf("childrenIds").orElse(Collections.emptyList()).forGetter(VillagerData::getChildrenIds),
            Codec.list(Codec.STRING).fieldOf("childrenNames").orElse(Collections.emptyList()).forGetter(VillagerData::getChildrenNames),
            Codec.STRING.fieldOf("favoriteFood").orElse(VillagerConstants.Defaults.FAVORITE_FOOD).forGetter(VillagerData::getFavoriteFood),
            Codec.STRING.fieldOf("hobby")
                .orElse(HobbyType.GARDENING.name())
                .xmap(HobbyType::fromString, HobbyType::name)
                .forGetter(VillagerData::getHobby),
            Codec.LONG.fieldOf("birthTime").orElse(System.currentTimeMillis()).forGetter(VillagerData::getBirthTime),
            Codec.STRING.fieldOf("birthPlace").orElse(VillagerConstants.Defaults.BIRTH_PLACE).forGetter(VillagerData::getBirthPlace),
            Codec.STRING.fieldOf("notes").orElse(VillagerConstants.Defaults.NOTES).forGetter(VillagerData::getNotes),
            Codec.LONG.fieldOf("deathTime").orElse(VillagerConstants.Defaults.DEATH_TIME).forGetter(VillagerData::getDeathTime),
            Codec.BOOL.fieldOf("isAlive").orElse(true).forGetter(VillagerData::isAlive),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("playerMemories").orElse(Collections.emptyMap()).forGetter(VillagerData::getPlayerMemories),
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("topicFrequency").orElse(Collections.emptyMap()).forGetter(VillagerData::getTopicFrequency),
            Codec.list(Codec.STRING).fieldOf("recentEvents").orElse(Collections.emptyList()).forGetter(VillagerData::getRecentEvents),
            Codec.LONG.fieldOf("lastConversationTime").orElse(0L).forGetter(VillagerData::getLastConversationTime)
        ).apply(instance, VillagerData::new)
    );

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
    private List<String> recentEvents;
    private long lastConversationTime;

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
        this.recentEvents = new ArrayList<>(recentEvents);
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
    public List<String> getRecentEvents() { return Collections.unmodifiableList(recentEvents); }
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
    public void setRecentEvents(List<String> recentEvents) { this.recentEvents = new ArrayList<>(recentEvents); }
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
        recentEvents.add(event);
        if (recentEvents.size() > 10) {
            recentEvents.remove(0);
        }
    }

    public void incrementPlayerRelation(String playerId) {
        playerRelations.put(playerId, playerRelations.getOrDefault(playerId, 0) + 1);
    }

    public void addPlayerMemory(String playerId, String memory) {
        playerMemories.put(playerId, memory);
    }

    public void incrementTopicFrequency(String topic) {
        topicFrequency.put(topic, topicFrequency.getOrDefault(topic, 0) + 1);
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