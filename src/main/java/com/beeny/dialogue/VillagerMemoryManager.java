package com.beeny.dialogue;

import com.beeny.config.VillagersRebornConfig;
import com.google.gson.reflect.TypeToken;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VillagerMemoryManager {
    private static final Map<String, Map<String, ConversationHistory>> memoryCache = new ConcurrentHashMap<>();
    
    public static class ConversationEntry {
        public final String speaker;
        public final String message;
        public final long timestamp;
        public final String category;
        
        public ConversationEntry(String speaker, String message, long timestamp, String category) {
            this.speaker = speaker;
            this.message = message;
            this.timestamp = timestamp;
            this.category = category;
        }
        
        @Override
        public String toString() {
            return speaker + ": " + message;
        }
    }
    
    public static class ConversationHistory {
        private final List<ConversationEntry> entries;
        private Set<String> discussedTopics;
        private final Map<String, Integer> topicCounts;
        private long lastInteraction;
        
        public ConversationHistory() {
            this.entries = Collections.synchronizedList(new LinkedList<>());
            this.discussedTopics = Collections.synchronizedSet(new HashSet<>());
            this.topicCounts = new ConcurrentHashMap<>();
            this.lastInteraction = System.currentTimeMillis();
        }
        
        public synchronized void addEntry(ConversationEntry entry) {
            entries.addFirst(entry);
            
            // Limit conversation history size
            while (entries.size() > VillagersRebornConfig.CONVERSATION_HISTORY_LIMIT) {
                entries.removeLast();
            }
            
            // Track topics
            if (entry.category != null) {
                discussedTopics.add(entry.category);
                topicCounts.put(entry.category, topicCounts.getOrDefault(entry.category, 0) + 1);
            }
            
            lastInteraction = entry.timestamp;
        }
        
        public List<ConversationEntry> getRecentEntries(int limit) {
            synchronized (entries) {
                return new ArrayList<>(entries.subList(0, Math.min(limit, entries.size())));
            }
        }
        
        public String getRecentConversationText(int maxEntries) {
            List<ConversationEntry> recent;
            synchronized (entries) {
                if (entries.isEmpty()) return "";
                recent = new ArrayList<>(entries.subList(0, Math.min(maxEntries, entries.size())));
            }
            StringBuilder sb = new StringBuilder();
            Collections.reverse(recent);
            for (ConversationEntry entry : recent) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(entry.toString());
            }
            return sb.toString();
        }
        
        public boolean hasDiscussedTopic(String topic) {
            synchronized (discussedTopics) {
                return discussedTopics.contains(topic);
            }
        }
        
        public int getTopicCount(String topic) {
            return topicCounts.getOrDefault(topic, 0);
        }
        
        public long getLastInteraction() {
            return lastInteraction;
        }
        
        public boolean isRecentInteraction(long withinMillis) {
            return (System.currentTimeMillis() - lastInteraction) < withinMillis;
        }
        
        public Set<String> getDiscussedTopics() {
            synchronized (discussedTopics) {
                return new HashSet<>(discussedTopics);
            }
        }

        // Setter for discussedTopics
        public void setDiscussedTopics(Set<String> topics) {
            this.discussedTopics = Collections.synchronizedSet(new HashSet<>(topics));
        }

        // Setter for lastInteraction
        public void setLastInteraction(long timestamp) {
            this.lastInteraction = timestamp;
        }
    }
    
    public static ConversationHistory getConversationHistory(String villagerUuid, String playerUuid) {
        return memoryCache.computeIfAbsent(villagerUuid, k -> new ConcurrentHashMap<>())
                         .computeIfAbsent(playerUuid, k -> new ConversationHistory());
    }
    
    public static void addConversationEntry(String villagerUuid, String playerUuid, 
                                          String speaker, String message, String category) {
        ConversationHistory history = getConversationHistory(villagerUuid, playerUuid);
        ConversationEntry entry = new ConversationEntry(speaker, message, System.currentTimeMillis(), category);
        history.addEntry(entry);
    }
    
    public static void addVillagerResponse(VillagerEntity villager, PlayerEntity player, 
                                         String response, String category) {
        addConversationEntry(
            villager.getUuidAsString(), 
            player.getUuidAsString(),
            villager.getName().getString(),
            response,
            category
        );
    }
    
    public static void addPlayerMessage(VillagerEntity villager, PlayerEntity player, String message) {
        addConversationEntry(
            villager.getUuidAsString(),
            player.getUuidAsString(), 
            player.getName().getString(),
            message,
            "PLAYER_INPUT"
        );
    }
    
    public static String getRecentConversationContext(VillagerEntity villager, PlayerEntity player, int maxEntries) {
        ConversationHistory history = getConversationHistory(villager.getUuidAsString(), player.getUuidAsString());
        return history.getRecentConversationText(maxEntries);
    }
    
    public static boolean hasRecentInteraction(VillagerEntity villager, PlayerEntity player, long withinMillis) {
        ConversationHistory history = getConversationHistory(villager.getUuidAsString(), player.getUuidAsString());
        return history.isRecentInteraction(withinMillis);
    }
    
    public static Set<String> getDiscussedTopics(VillagerEntity villager, PlayerEntity player) {
        ConversationHistory history = getConversationHistory(villager.getUuidAsString(), player.getUuidAsString());
        return history.getDiscussedTopics();
    }
    
    public static void saveMemoryToNbt(VillagerEntity villager, NbtCompound nbt) {
        String villagerUuid = villager.getUuidAsString();
        Map<String, ConversationHistory> villagerMemory = memoryCache.get(villagerUuid);
        
        if (villagerMemory != null && !villagerMemory.isEmpty()) {
            NbtCompound memoryNbt = new NbtCompound();
            
            for (Map.Entry<String, ConversationHistory> entry : villagerMemory.entrySet()) {
                String playerUuid = entry.getKey();
                ConversationHistory history = entry.getValue();
                
                NbtList conversationList = new NbtList();
                for (ConversationEntry convEntry : history.getRecentEntries(VillagersRebornConfig.CONVERSATION_HISTORY_LIMIT)) {
                    NbtCompound entryNbt = new NbtCompound();
                    entryNbt.putString("speaker", convEntry.speaker);
                    entryNbt.putString("message", convEntry.message);
                    entryNbt.putLong("timestamp", convEntry.timestamp);
                    entryNbt.putString("category", convEntry.category != null ? convEntry.category : "");
                    conversationList.add(entryNbt);
                }
                
                NbtCompound playerMemory = new NbtCompound();
                playerMemory.put("conversations", conversationList);
                playerMemory.putLong("lastInteraction", history.getLastInteraction());
                
                // Save discussed topics
                NbtList topicsList = new NbtList();
                for (String topic : history.getDiscussedTopics()) {
                    topicsList.add(NbtString.of(topic));
                }
                playerMemory.put("discussedTopics", topicsList);
                
                memoryNbt.put(playerUuid, playerMemory);
            }
            
            nbt.put("conversationMemory", memoryNbt);
        }
    }
    
    public static void loadMemoryFromNbt(VillagerEntity villager, NbtCompound nbt) {
        if (!nbt.contains("conversationMemory")) return;
        
        String villagerUuid = villager.getUuidAsString();
        NbtCompound memoryNbt = nbt.getCompound("conversationMemory");
        if (memoryNbt == null) return;
        
        Map<String, ConversationHistory> villagerMemory = new ConcurrentHashMap<>();
        
        for (String playerUuid : memoryNbt.getKeys()) {
            NbtCompound playerMemory = memoryNbt.getCompound(playerUuid);
            if (playerMemory == null) continue;
            ConversationHistory history = new ConversationHistory();
            
            // Restore discussedTopics
            if (playerMemory.contains("discussedTopics")) {
                NbtList topicsList = playerMemory.getList("discussedTopics");
                Set<String> topics = new HashSet<>();
                if (topicsList != null) {
                    for (int i = 0; i < topicsList.size(); i++) {
                        String topic = topicsList.getString(i);
                        if (topic != null && !topic.isEmpty()) {
                            topics.add(topic);
                        }
                    }
                }
                history.setDiscussedTopics(topics);
            }

            // Restore lastInteraction
            if (playerMemory.contains("lastInteraction")) {
                long lastInteraction = playerMemory.getLong("lastInteraction");
                history.setLastInteraction(lastInteraction);
            }
            
            if (playerMemory.contains("conversations")) {
                NbtList conversationList = playerMemory.getList("conversations");
                if (conversationList == null) continue;
                
                List<ConversationEntry> entries = new ArrayList<>();
                for (int i = 0; i < conversationList.size(); i++) {
                    NbtCompound entryNbt = conversationList.getCompound(i);
                    if (entryNbt == null) continue;
                    String speaker = entryNbt.getString("speaker");
                    String message = entryNbt.getString("message");
                    long timestamp = entryNbt.getLong("timestamp");
                    String category = entryNbt.getString("category");
                    ConversationEntry entry = new ConversationEntry(
                        speaker != null ? speaker : "",
                        message != null ? message : "",
                        timestamp,
                        (category != null && !category.isEmpty()) ? category : null
                    );
                    entries.add(entry);
                }
                
                // Add entries in reverse order to maintain chronological order
                Collections.reverse(entries);
                for (ConversationEntry entry : entries) {
                    history.addEntry(entry);
                }
            }
            
            villagerMemory.put(playerUuid, history);
        }
        
        memoryCache.put(villagerUuid, villagerMemory);
    }
    
    public static void clearMemory(String villagerUuid) {
        memoryCache.remove(villagerUuid);
    }
    
    public static void clearMemory(String villagerUuid, String playerUuid) {
        Map<String, ConversationHistory> villagerMemory = memoryCache.get(villagerUuid);
        if (villagerMemory != null) {
            villagerMemory.remove(playerUuid);
        }
    }
    
    public static void cleanupOldMemories(long olderThanMillis) {
        long cutoffTime = System.currentTimeMillis() - olderThanMillis;
        
        memoryCache.entrySet().removeIf(villagerEntry -> {
            villagerEntry.getValue().entrySet().removeIf(playerEntry -> 
                playerEntry.getValue().getLastInteraction() < cutoffTime
            );
            return villagerEntry.getValue().isEmpty();
        });
    }
    
    public static int getMemorySize() {
        return memoryCache.values().stream()
                .mapToInt(Map::size)
                .sum();
    }
}