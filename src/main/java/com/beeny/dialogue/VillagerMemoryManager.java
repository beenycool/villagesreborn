package com.beeny.dialogue;

import com.beeny.config.VillagersRebornConfig;
import com.google.gson.reflect.TypeToken;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        
        public ConversationEntry(@NotNull String speaker, @NotNull String message, long timestamp, @Nullable String category) {
            this.speaker = speaker;
            this.message = message;
            this.timestamp = timestamp;
            this.category = category;
        }
        
        @Override
        public @NotNull String toString() {
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
        
        public synchronized void addEntry(@NotNull ConversationEntry entry) {
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
        
        public @NotNull List<ConversationEntry> getRecentEntries(int limit) {
            synchronized (entries) {
                return new ArrayList<>(entries.subList(0, Math.min(limit, entries.size())));
            }
        }
        
        public @NotNull String getRecentConversationText(int maxEntries) {
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
        
        public boolean hasDiscussedTopic(@NotNull String topic) {
            synchronized (discussedTopics) {
                return discussedTopics.contains(topic);
            }
        }
        
        public int getTopicCount(@NotNull String topic) {
            return topicCounts.getOrDefault(topic, 0);
        }
        
        public long getLastInteraction() {
            return lastInteraction;
        }
        
        public boolean isRecentInteraction(long withinMillis) {
            return (System.currentTimeMillis() - lastInteraction) < withinMillis;
        }
        
        public @NotNull Set<String> getDiscussedTopics() {
            synchronized (discussedTopics) {
                return new HashSet<>(discussedTopics);
            }
        }

        // Setter for discussedTopics
        public void setDiscussedTopics(@NotNull Set<String> topics) {
            this.discussedTopics = Collections.synchronizedSet(new HashSet<>(topics));
        }

        // Setter for lastInteraction
        public void setLastInteraction(long timestamp) {
            this.lastInteraction = timestamp;
        }
    }
    
    public static @NotNull ConversationHistory getConversationHistory(@NotNull String villagerUuid, @NotNull String playerUuid) {
        return memoryCache.computeIfAbsent(villagerUuid, k -> new ConcurrentHashMap<>())
                         .computeIfAbsent(playerUuid, k -> new ConversationHistory());
    }
    
    public static void addConversationEntry(@NotNull String villagerUuid, @NotNull String playerUuid,
                                          @NotNull String speaker, @NotNull String message, @Nullable String category) {
        ConversationHistory history = getConversationHistory(villagerUuid, playerUuid);
        ConversationEntry entry = new ConversationEntry(speaker, message, System.currentTimeMillis(), category);
        history.addEntry(entry);
    }
    
    public static void addVillagerResponse(@NotNull VillagerEntity villager, @NotNull PlayerEntity player,
                                         @NotNull String response, @NotNull String category) {
        addConversationEntry(
            villager.getUuidAsString(), 
            player.getUuidAsString(),
            villager.getName().getString(),
            response,
            category
        );
    }
    
    public static void addPlayerMessage(@NotNull VillagerEntity villager, @NotNull PlayerEntity player, @NotNull String message) {
        addConversationEntry(
            villager.getUuidAsString(),
            player.getUuidAsString(), 
            player.getName().getString(),
            message,
            "PLAYER_INPUT"
        );
    }
    
    public static @NotNull String getRecentConversationContext(@NotNull VillagerEntity villager, @NotNull PlayerEntity player, int maxEntries) {
        ConversationHistory history = getConversationHistory(villager.getUuidAsString(), player.getUuidAsString());
        return history.getRecentConversationText(maxEntries);
    }
    
    public static boolean hasRecentInteraction(@NotNull VillagerEntity villager, @NotNull PlayerEntity player, long withinMillis) {
        ConversationHistory history = getConversationHistory(villager.getUuidAsString(), player.getUuidAsString());
        return history.isRecentInteraction(withinMillis);
    }
    
    public static @NotNull Set<String> getDiscussedTopics(@NotNull VillagerEntity villager, @NotNull PlayerEntity player) {
        ConversationHistory history = getConversationHistory(villager.getUuidAsString(), player.getUuidAsString());
        return history.getDiscussedTopics();
    }
    
    public static void saveMemoryToNbt(@NotNull VillagerEntity villager, @NotNull NbtCompound nbt) {
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
    
    public static void loadMemoryFromNbt(@NotNull VillagerEntity villager, @NotNull NbtCompound nbt) {
        if (!nbt.contains("conversationMemory")) return;
        
        String villagerUuid = villager.getUuidAsString();
        NbtCompound memoryNbt = nbt.getCompound("conversationMemory").orElse(null);
        if (memoryNbt == null) return;
        
        Map<String, ConversationHistory> villagerMemory = new ConcurrentHashMap<>();
        
        for (String playerUuid : memoryNbt.getKeys()) {
            NbtCompound playerMemory = memoryNbt.getCompound(playerUuid).orElse(null);
            if (playerMemory == null) continue;
            ConversationHistory history = new ConversationHistory();
            
            // Restore discussedTopics
            if (playerMemory.contains("discussedTopics")) {
                NbtList topicsList = playerMemory.getList("discussedTopics").orElse(new NbtList());
                Set<String> topics = new HashSet<>();
                for (int i = 0; i < topicsList.size(); i++) {
                    String topic = topicsList.getString(i).orElse("");
                    if (!topic.isEmpty()) topics.add(topic);
                }
                history.setDiscussedTopics(topics);
            }

            // Restore lastInteraction
            if (playerMemory.contains("lastInteraction")) {
                long lastInteraction = playerMemory.getLong("lastInteraction").orElse(0L);
                history.setLastInteraction(lastInteraction);
            }
            
            if (playerMemory.contains("conversations")) {
                NbtList conversationList = playerMemory.getList("conversations").orElse(new NbtList());
                List<ConversationEntry> entries = new ArrayList<>();
                for (int i = 0; i < conversationList.size(); i++) {
                    NbtCompound entryNbt = conversationList.getCompound(i).orElse(new NbtCompound());
                    String speaker = entryNbt.getString("speaker").orElse("");
                    String message = entryNbt.getString("message").orElse("");
                    long timestamp = entryNbt.getLong("timestamp").orElse(0L);
                    String category = entryNbt.getString("category").orElse("");
                    ConversationEntry entry = new ConversationEntry(
                        speaker,
                        message,
                        timestamp,
                        !category.isEmpty() ? category : null
                    );
                    entries.add(entry);
                }
                Collections.reverse(entries);
                for (ConversationEntry entry : entries) history.addEntry(entry);
            }
            
            villagerMemory.put(playerUuid, history);
        }
        
        memoryCache.put(villagerUuid, villagerMemory);
    }
    
    public static void clearMemory(@NotNull String villagerUuid) {
        memoryCache.remove(villagerUuid);
    }
    
    public static void clearMemory(@NotNull String villagerUuid, @NotNull String playerUuid) {
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