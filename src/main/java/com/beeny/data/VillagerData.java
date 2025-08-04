package com.beeny.data;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;

import java.util.concurrent.ThreadLocalRandom;

public class VillagerData {
    // Use ThreadLocalRandom for thread-safe random generation
    // Rework codec to avoid exceeding arity by mapping to/from NbtCompound directly.
    public static final Codec<VillagerData> CODEC = Codec.PASSTHROUGH.comapFlatMap(
        dyn -> {
            try {
                net.minecraft.nbt.NbtElement elt = (net.minecraft.nbt.NbtElement) dyn.getValue();
                if (!(elt instanceof net.minecraft.nbt.NbtCompound nbt)) {
                    return com.mojang.serialization.DataResult.error(() -> "Expected NbtCompound");
                }
                VillagerData v = new VillagerData();
                v.name = nbt.contains("name") ? nbt.getString("name").orElse("") : "";
                v.age = nbt.contains("age") ? nbt.getInt("age").orElse(0) : 0;
                v.gender = nbt.contains("gender") ? nbt.getString("gender").orElse("Unknown") : "Unknown";
                v.personality = nbt.contains("personality") ? nbt.getString("personality").orElse("Friendly") : "Friendly";
                v.happiness = nbt.contains("happiness") ? nbt.getInt("happiness").orElse(50) : 50;
                v.totalTrades = nbt.contains("totalTrades") ? nbt.getInt("totalTrades").orElse(0) : 0;
                v.favoritePlayerId = nbt.contains("favoritePlayerId") ? nbt.getString("favoritePlayerId").orElse("") : "";
                v.professionHistory = new java.util.ArrayList<>();
                if (nbt.contains("professionHistory")) {
                    net.minecraft.nbt.NbtList lst = nbt.getList("professionHistory").orElse(new net.minecraft.nbt.NbtList());
                    for (net.minecraft.nbt.NbtElement e : lst) v.professionHistory.add(e.asString().orElse(""));
                }
                v.playerRelations = new java.util.HashMap<>();
                if (nbt.contains("playerRelations")) {
                    net.minecraft.nbt.NbtCompound rel = nbt.getCompound("playerRelations").orElse(new net.minecraft.nbt.NbtCompound());
                    for (String k : rel.getKeys()) v.playerRelations.put(k, rel.getInt(k).orElse(0));
                }
                v.familyMembers = new java.util.ArrayList<>();
                if (nbt.contains("familyMembers")) {
                    net.minecraft.nbt.NbtList lst = nbt.getList("familyMembers").orElse(new net.minecraft.nbt.NbtList());
                    for (net.minecraft.nbt.NbtElement e : lst) v.familyMembers.add(e.asString().orElse(""));
                }
                v.spouseName = nbt.contains("spouseName") ? nbt.getString("spouseName").orElse("") : "";
                v.spouseId = nbt.contains("spouseId") ? nbt.getString("spouseId").orElse("") : "";
                v.childrenIds = new java.util.ArrayList<>();
                if (nbt.contains("childrenIds")) {
                    net.minecraft.nbt.NbtList lst = nbt.getList("childrenIds").orElse(new net.minecraft.nbt.NbtList());
                    for (net.minecraft.nbt.NbtElement e : lst) v.childrenIds.add(e.asString().orElse(""));
                }
                v.childrenNames = new java.util.ArrayList<>();
                if (nbt.contains("childrenNames")) {
                    net.minecraft.nbt.NbtList lst = nbt.getList("childrenNames").orElse(new net.minecraft.nbt.NbtList());
                    for (net.minecraft.nbt.NbtElement e : lst) v.childrenNames.add(e.asString().orElse(""));
                }
                v.favoriteFood = nbt.contains("favoriteFood") ? nbt.getString("favoriteFood").orElse("") : "";
                v.playerMemories = new java.util.concurrent.ConcurrentHashMap<>();
                if (nbt.contains("playerMemories")) {
                    net.minecraft.nbt.NbtCompound rel = nbt.getCompound("playerMemories").orElse(new net.minecraft.nbt.NbtCompound());
                    for (String k : rel.getKeys()) v.playerMemories.put(k, rel.getString(k).orElse(""));
                }
                v.topicFrequency = new java.util.concurrent.ConcurrentHashMap<>();
                if (nbt.contains("topicFrequency")) {
                    net.minecraft.nbt.NbtCompound rel = nbt.getCompound("topicFrequency").orElse(new net.minecraft.nbt.NbtCompound());
                    for (String k : rel.getKeys()) v.topicFrequency.put(k, rel.getInt(k).orElse(0));
                }
                v.recentEvents = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
                if (nbt.contains("recentEvents")) {
                    net.minecraft.nbt.NbtList lst = nbt.getList("recentEvents").orElse(new net.minecraft.nbt.NbtList());
                    for (net.minecraft.nbt.NbtElement e : lst) v.recentEvents.add(e.asString().orElse(""));
                }
                v.lastConversationTime = nbt.contains("lastConversationTime") ? nbt.getLong("lastConversationTime").orElse(0L) : 0L;
                v.isAlive = nbt.contains("isAlive") ? nbt.getBoolean("isAlive").orElse(true) : true;
                return com.mojang.serialization.DataResult.success(v);
            } catch (Exception ex) {
                return com.mojang.serialization.DataResult.error(() -> "Failed to decode VillagerData: " + ex.getMessage());
            }
        },
        v -> {
            net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();
            nbt.putString("name", v.getName());
            nbt.putInt("age", v.getAge());
            nbt.putString("gender", v.getGender());
            nbt.putString("personality", v.getPersonality());
            nbt.putInt("happiness", v.getHappiness());
            nbt.putInt("totalTrades", v.getTotalTrades());
            if (!v.getFavoritePlayerId().isEmpty()) nbt.putString("favoritePlayerId", v.getFavoritePlayerId());
            net.minecraft.nbt.NbtList prof = new net.minecraft.nbt.NbtList();
            for (String s : v.getProfessionHistory()) prof.add(net.minecraft.nbt.NbtString.of(s));
            nbt.put("professionHistory", prof);
            net.minecraft.nbt.NbtCompound rel = new net.minecraft.nbt.NbtCompound();
            for (java.util.Map.Entry<String, Integer> e : v.getPlayerRelations().entrySet()) rel.putInt(e.getKey(), e.getValue());
            nbt.put("playerRelations", rel);
            net.minecraft.nbt.NbtList fam = new net.minecraft.nbt.NbtList();
            for (String s : v.getFamilyMembers()) fam.add(net.minecraft.nbt.NbtString.of(s));
            nbt.put("familyMembers", fam);
            if (!v.getSpouseName().isEmpty()) nbt.putString("spouseName", v.getSpouseName());
            if (!v.getSpouseId().isEmpty()) nbt.putString("spouseId", v.getSpouseId());
            net.minecraft.nbt.NbtList cids = new net.minecraft.nbt.NbtList();
            for (String s : v.getChildrenIds()) cids.add(net.minecraft.nbt.NbtString.of(s));
            nbt.put("childrenIds", cids);
            net.minecraft.nbt.NbtList cnames = new net.minecraft.nbt.NbtList();
            for (String s : v.getChildrenNames()) cnames.add(net.minecraft.nbt.NbtString.of(s));
            nbt.put("childrenNames", cnames);
            if (!v.getFavoriteFood().isEmpty()) nbt.putString("favoriteFood", v.getFavoriteFood());
            net.minecraft.nbt.NbtCompound mem = new net.minecraft.nbt.NbtCompound();
            for (java.util.Map.Entry<String, String> e : v.playerMemories.entrySet()) mem.putString(e.getKey(), e.getValue());
            nbt.put("playerMemories", mem);
            net.minecraft.nbt.NbtCompound topics = new net.minecraft.nbt.NbtCompound();
            for (java.util.Map.Entry<String, Integer> e : v.topicFrequency.entrySet()) topics.putInt(e.getKey(), e.getValue());
            nbt.put("topicFrequency", topics);
            net.minecraft.nbt.NbtList events = new net.minecraft.nbt.NbtList();
            for (String s : v.recentEvents) events.add(net.minecraft.nbt.NbtString.of(s));
            nbt.put("recentEvents", events);
            nbt.putLong("lastConversationTime", v.lastConversationTime);
            nbt.putBoolean("isAlive", v.isAlive);
            return new com.mojang.serialization.Dynamic<>(net.minecraft.nbt.NbtOps.INSTANCE, nbt);
        }
    ).xmap(
        obj -> (VillagerData) obj,
        v -> v
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