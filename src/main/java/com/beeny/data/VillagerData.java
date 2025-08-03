package com.beeny.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class VillagerData {
    // Use ThreadLocalRandom for thread-safe random generation
    public static final Codec<VillagerData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("name").forGetter(VillagerData::getName),
            Codec.INT.fieldOf("age").forGetter(VillagerData::getAge),
            Codec.STRING.fieldOf("gender").forGetter(VillagerData::getGender),
            Codec.STRING.fieldOf("personality").forGetter(VillagerData::getPersonality),
            Codec.INT.fieldOf("happiness").forGetter(VillagerData::getHappiness),
            Codec.INT.fieldOf("totalTrades").forGetter(VillagerData::getTotalTrades),
            Codec.STRING.optionalFieldOf("favoritePlayerId", "").forGetter(VillagerData::getFavoritePlayerId),
            Codec.list(Codec.STRING).fieldOf("professionHistory").forGetter(VillagerData::getProfessionHistory),
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("playerRelations").forGetter(VillagerData::getPlayerRelations),
            Codec.list(Codec.STRING).fieldOf("familyMembers").forGetter(VillagerData::getFamilyMembers),
            Codec.STRING.optionalFieldOf("spouseName", "").forGetter(VillagerData::getSpouseName),
            Codec.STRING.optionalFieldOf("spouseId", "").forGetter(VillagerData::getSpouseId),
            Codec.list(Codec.STRING).fieldOf("childrenIds").forGetter(VillagerData::getChildrenIds),
            Codec.list(Codec.STRING).fieldOf("childrenNames").forGetter(VillagerData::getChildrenNames),
            Codec.STRING.optionalFieldOf("favoriteFood", "").forGetter(VillagerData::getFavoriteFood),
            Codec.BOOL.fieldOf("isAlive").forGetter(VillagerData::isAlive)
        ).apply(instance, (name, age, gender, personality, happiness, totalTrades, favoritePlayerId, professionHistory,
                          playerRelations, familyMembers, spouseName, spouseId, childrenIds, childrenNames,
                          favoriteFood, isAlive) -> {
            VillagerData data = new VillagerData();
            data.name = name;
            data.age = age;
            data.gender = gender;
            data.personality = personality;
            data.happiness = happiness;
            data.totalTrades = totalTrades;
            data.favoritePlayerId = favoritePlayerId;
            data.professionHistory = new ArrayList<>(professionHistory);
            data.playerRelations = new HashMap<>(playerRelations);
            data.familyMembers = new ArrayList<>(familyMembers);
            data.spouseName = spouseName;
            data.spouseId = spouseId;
            data.childrenIds = new ArrayList<>(childrenIds);
            data.childrenNames = new ArrayList<>(childrenNames);
            data.favoriteFood = favoriteFood;
            data.isAlive = isAlive;
            data.playerMemories = new java.util.concurrent.ConcurrentHashMap<>();
            data.topicFrequency = new java.util.concurrent.ConcurrentHashMap<>();
            data.recentEvents = java.util.Collections.synchronizedList(new ArrayList<>());
            data.hobby = "";
            data.birthTime = 0L;
            data.birthPlace = "";
            data.notes = "";
            data.deathTime = 0L;
            data.lastConversationTime = 0;
            return data;
        })
    );
    
    
    private String name;
    private int age; 
    private String gender;
    private String personality;
    
    
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
    private String hobby;
    private long birthTime;
    private String birthPlace;
    private String notes;
    private long deathTime;
    private boolean isAlive;
    
    private Map<String, String> playerMemories;
    private Map<String, Integer> topicFrequency;
    private List<String> recentEvents;
    private long lastConversationTime;
    
    
    public static final String[] PERSONALITIES = {
        "Friendly", "Grumpy", "Shy", "Energetic", "Lazy", 
        "Curious", "Serious", "Cheerful", "Nervous", "Confident"
    };
    
    
    public static final String[] HOBBIES = {
        "Gardening", "Reading", "Fishing", "Cooking", "Singing",
        "Dancing", "Crafting", "Exploring", "Collecting", "Gossiping"
    };
    
    
    public VillagerData() {
        this.name = "";
        this.age = 0;
        this.gender = "Unknown";
        this.personality = PERSONALITIES[ThreadLocalRandom.current().nextInt(PERSONALITIES.length)];
        this.happiness = 50;
        this.totalTrades = 0;
        this.favoritePlayerId = "";
        this.professionHistory = new ArrayList<>();
        this.playerRelations = new HashMap<>();
        this.familyMembers = new ArrayList<>();
        this.spouseName = "";
        this.spouseId = "";
        this.childrenIds = new ArrayList<>();
        this.childrenNames = new ArrayList<>();
        this.favoriteFood = "";
        this.hobby = HOBBIES[ThreadLocalRandom.current().nextInt(HOBBIES.length)];
        this.birthTime = System.currentTimeMillis();
        this.birthPlace = "";
        this.notes = "";
        this.deathTime = 0;
        this.isAlive = true;
        this.playerMemories = new java.util.concurrent.ConcurrentHashMap<>();
        this.topicFrequency = new java.util.concurrent.ConcurrentHashMap<>();
        this.recentEvents = java.util.Collections.synchronizedList(new ArrayList<>());
        this.lastConversationTime = 0;
    }
    
    
    public VillagerData(String name, int age, String gender, String personality, int happiness,
                       int totalTrades, String favoritePlayerId, List<String> professionHistory,
                       Map<String, Integer> playerRelations, List<String> familyMembers,
                       String spouseName, String spouseId, List<String> childrenIds, List<String> childrenNames,
                       String favoriteFood, String hobby, long birthTime, String birthPlace,
                       String notes, long deathTime, boolean isAlive) {
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
        this.playerMemories = new java.util.concurrent.ConcurrentHashMap<>();
        this.topicFrequency = new java.util.concurrent.ConcurrentHashMap<>();
        this.recentEvents = java.util.Collections.synchronizedList(new ArrayList<>());
        this.lastConversationTime = 0;
    }
    
    
    public void incrementAge() {
        this.age++;
    }
    
    public void adjustHappiness(int amount) {
        this.happiness = Math.max(0, Math.min(100, this.happiness + amount));
    }
    
    public void addProfession(String profession) {
        if (!professionHistory.contains(profession)) {
            professionHistory.add(profession);
        }
    }
    
    public void updatePlayerRelation(String playerUuid, int change) {
        playerRelations.put(playerUuid, playerRelations.getOrDefault(playerUuid, 0) + change);
    }
    
    public int getPlayerReputation(String playerUuid) {
        return playerRelations.getOrDefault(playerUuid, 0);
    }
    
    public void adjustPlayerReputation(String playerUuid, int change) {
        updatePlayerRelation(playerUuid, change);
    }
    
    public void marry(String spouseId) {
        this.spouseId = spouseId;
        this.happiness = Math.min(100, this.happiness + 20);
    }
    
    public void marry(String spouseName, String spouseId) {
        this.spouseName = spouseName;
        this.spouseId = spouseId;
        this.happiness = Math.min(100, this.happiness + 20);
    }
    
    public void setWidowed() {
        this.spouseName = "";
        this.spouseId = "";
        this.notes = "Widowed";
    }
    
    public void addChild(String childId) {
        if (!childrenIds.contains(childId)) {
            childrenIds.add(childId);
            this.happiness = Math.min(100, this.happiness + 10);
        }
    }
    
    public void addChild(String childName, String childId) {
        if (!childrenIds.contains(childId)) {
            childrenIds.add(childId);
            childrenNames.add(childName);
            this.happiness = Math.min(100, this.happiness + 10);
        }
    }
    
    public void addFamilyMember(String memberId) {
        if (!familyMembers.contains(memberId)) {
            familyMembers.add(memberId);
        }
    }
    
    public void addFamilyMember(String memberName, String memberId) {
        if (!familyMembers.contains(memberId)) {
            familyMembers.add(memberId);
        }
    }
    
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public String getPersonality() { return personality; }
    public void setPersonality(String personality) { this.personality = personality; }
    
    public int getHappiness() { return happiness; }
    public void setHappiness(int happiness) { this.happiness = happiness; }
    
    public int getTotalTrades() { return totalTrades; }
    public void incrementTrades() { this.totalTrades++; }
    
    public String getSpouseId() { return spouseId; }
    public String getSpouseName() { return spouseName; }
    
    public List<String> getChildrenIds() { return new ArrayList<>(childrenIds); }
    public List<String> getChildrenNames() { return new ArrayList<>(childrenNames); }
    public List<String> getFamilyMembers() { return new ArrayList<>(familyMembers); }
    public List<String> getProfessionHistory() { return new ArrayList<>(professionHistory); }
    
    public Map<String, Integer> getPlayerRelations() { return new HashMap<>(playerRelations); }
    
    public String getFavoriteFood() { return favoriteFood; }
    public void setFavoriteFood(String favoriteFood) { this.favoriteFood = favoriteFood; }
    
    public String getHobby() { return hobby; }
    public void setHobby(String hobby) { this.hobby = hobby; }
    
    public long getBirthTime() { return birthTime; }
    public void setBirthTime(long birthTime) { this.birthTime = birthTime; }
    
    public String getBirthPlace() { return birthPlace; }
    public void setBirthPlace(String birthPlace) { this.birthPlace = birthPlace; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public long getDeathTime() { return deathTime; }
    public void setDeathTime(long deathTime) { this.deathTime = deathTime; this.isAlive = false; }
    
    public boolean isAlive() { return isAlive; }
    public void setAlive(boolean alive) { this.isAlive = alive; }
    
    public String getFavoritePlayerId() { return favoritePlayerId; }
    public void setFavoritePlayerId(String favoritePlayerId) { this.favoritePlayerId = favoritePlayerId; }
    
    
    public String getAgeInDays() {
        if (age < 20) {
            return age + " days (Baby)";
        } else if (age < 100) {
            return age + " days (Young Adult)";
        } else if (age < 300) {
            return age + " days (Adult)";
        } else {
            return age + " days (Elder)";
        }
    }
    
    
    public String getHappinessDescription() {
        if (happiness >= 80) return "Very Happy";
        if (happiness >= 60) return "Happy";
        if (happiness >= 40) return "Content";
        if (happiness >= 20) return "Unhappy";
        return "Miserable";
    }
    
    public Map<String, String> getPlayerMemories() { return new HashMap<>(playerMemories); }
    public void setPlayerMemory(String playerUuid, String memory) { 
        this.playerMemories.put(playerUuid, memory);
    }
    public String getPlayerMemory(String playerUuid) {
        return playerMemories.getOrDefault(playerUuid, "");
    }
    
    public Map<String, Integer> getTopicFrequency() { return new HashMap<>(topicFrequency); }
    public void incrementTopicFrequency(String topic) {
        topicFrequency.put(topic, topicFrequency.getOrDefault(topic, 0) + 1);
    }
    public int getTopicFrequency(String topic) {
        return topicFrequency.getOrDefault(topic, 0);
    }
    
    public List<String> getRecentEvents() { return new ArrayList<>(recentEvents); }
    public void addRecentEvent(String event) {
        recentEvents.add(0, event);
        // Keep only the last 5 events
        if (recentEvents.size() > 5) {
            recentEvents.remove(5);
        }
    }
    
    public long getLastConversationTime() { return lastConversationTime; }
    public void setLastConversationTime(long time) { this.lastConversationTime = time; }
    public void updateLastConversationTime() { this.lastConversationTime = System.currentTimeMillis(); }
    
    public boolean hasRecentConversation(long withinMillis) {
        return (System.currentTimeMillis() - lastConversationTime) < withinMillis;
    }
}