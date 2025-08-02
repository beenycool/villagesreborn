package com.beeny.ai.quests;

import com.beeny.data.VillagerData;
import com.beeny.ai.core.VillagerEmotionSystem;
import com.beeny.ai.social.VillagerGossipNetwork;
import com.beeny.system.ServerVillagerManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dynamic quest generation system that creates contextual quests based on villager needs,
 * village state, relationships, and ongoing storylines
 */
public class VillagerQuestSystem {
    
    public enum QuestType {
        FETCH_ITEM("Fetch Quest", "Bring me a specific item"),
        DELIVERY("Delivery Quest", "Deliver something to another villager"),
        PROTECTION("Protection Quest", "Keep villagers safe from threats"),
        SOCIAL("Social Quest", "Help with relationships or social issues"),
        EXPLORATION("Exploration Quest", "Discover or explore something"),
        BUILDING("Building Quest", "Help construct or improve something"),
        MYSTERY("Mystery Quest", "Solve a puzzle or uncover secrets"),
        TRADE("Trade Quest", "Facilitate trading between parties"),
        ROMANCE("Romance Quest", "Help with romantic relationships"),
        FAMILY("Family Quest", "Help with family matters"),
        ECONOMIC("Economic Quest", "Help with village economy"),
        CULTURAL("Cultural Quest", "Participate in village traditions or events");
        
        public final String displayName;
        public final String description;
        
        QuestType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
    }
    
    public enum QuestPriority {
        LOW(1, "Optional"),
        MEDIUM(2, "Helpful"),
        HIGH(3, "Important"),
        URGENT(4, "Critical");
        
        public final int level;
        public final String description;
        
        QuestPriority(int level, String description) {
            this.level = level;
            this.description = description;
        }
    }
    
    public static class Quest {
        public final String id;
        public final String title;
        public final String description;
        public final QuestType type;
        public final QuestPriority priority;
        public final VillagerEntity questGiver;
        public final PlayerEntity questReceiver;
        public final Map<String, Object> objectives;
        public final Map<String, Object> rewards;
        public final long timeLimit; // 0 for no time limit
        public final long creationTime;
        
        private QuestStatus status;
        private Map<String, Object> progress;
        private String failureReason;
        
        public Quest(String title, String description, QuestType type, QuestPriority priority, 
                    VillagerEntity questGiver, PlayerEntity questReceiver) {
            this.id = UUID.randomUUID().toString();
            this.title = title;
            this.description = description;
            this.type = type;
            this.priority = priority;
            this.questGiver = questGiver;
            this.questReceiver = questReceiver;
            this.objectives = new ConcurrentHashMap<>();
            this.rewards = new ConcurrentHashMap<>();
            this.timeLimit = 0;
            this.creationTime = System.currentTimeMillis();
            this.status = QuestStatus.AVAILABLE;
            this.progress = new ConcurrentHashMap<>();
        }
        
        public enum QuestStatus {
            AVAILABLE, ACCEPTED, IN_PROGRESS, COMPLETED, FAILED, EXPIRED
        }
        
        public QuestStatus getStatus() { return status; }
        public void setStatus(QuestStatus status) { this.status = status; }
        
        public Map<String, Object> getProgress() { return new HashMap<>(progress); }
        public void updateProgress(String key, Object value) { progress.put(key, value); }
        
        public boolean isExpired() {
            return timeLimit > 0 && (System.currentTimeMillis() - creationTime) > timeLimit;
        }
        
        public float getCompletionPercentage() {
            if (objectives.isEmpty()) return 0.0f;
            
            int completedObjectives = 0;
            for (String objective : objectives.keySet()) {
                if (progress.containsKey(objective + "_completed") && 
                    (Boolean) progress.get(objective + "_completed")) {
                    completedObjectives++;
                }
            }
            
            return (float) completedObjectives / objectives.size();
        }
        
        public List<Text> getFormattedDescription() {
            List<Text> lines = new ArrayList<>();
            
            // Title with priority color
            Formatting titleColor = switch (priority) {
                case LOW -> Formatting.GRAY;
                case MEDIUM -> Formatting.YELLOW;
                case HIGH -> Formatting.GOLD;
                case URGENT -> Formatting.RED;
            };
            
            lines.add(Text.literal("Quest: " + title).formatted(titleColor, Formatting.BOLD));
            lines.add(Text.literal("Type: " + type.displayName).formatted(Formatting.AQUA));
            lines.add(Text.literal("Priority: " + priority.description).formatted(titleColor));
            lines.add(Text.empty());
            lines.add(Text.literal(description).formatted(Formatting.WHITE));
            
            // Objectives
            if (!objectives.isEmpty()) {
                lines.add(Text.empty());
                lines.add(Text.literal("Objectives:").formatted(Formatting.GREEN, Formatting.UNDERLINE));
                
                for (Map.Entry<String, Object> objective : objectives.entrySet()) {
                    boolean completed = progress.containsKey(objective.getKey() + "_completed") &&
                                      (Boolean) progress.get(objective.getKey() + "_completed");
                    
                    Formatting objColor = completed ? Formatting.GREEN : Formatting.WHITE;
                    String prefix = completed ? "✓ " : "○ ";
                    
                    lines.add(Text.literal(prefix + objective.getValue().toString()).formatted(objColor));
                }
            }
            
            // Rewards
            if (!rewards.isEmpty()) {
                lines.add(Text.empty());
                lines.add(Text.literal("Rewards:").formatted(Formatting.GOLD, Formatting.UNDERLINE));
                
                for (Map.Entry<String, Object> reward : rewards.entrySet()) {
                    lines.add(Text.literal("• " + reward.getValue().toString()).formatted(Formatting.YELLOW));
                }
            }
            
            // Time limit
            if (timeLimit > 0) {
                long remaining = timeLimit - (System.currentTimeMillis() - creationTime);
                if (remaining > 0) {
                    long hours = remaining / 3600000;
                    long minutes = (remaining % 3600000) / 60000;
                    lines.add(Text.empty());
                    lines.add(Text.literal("Time Remaining: " + hours + "h " + minutes + "m")
                        .formatted(Formatting.LIGHT_PURPLE));
                }
            }
            
            return lines;
        }
    }
    
    // Quest generators for different types
    public abstract static class QuestGenerator {
        public abstract Quest generateQuest(VillagerEntity villager, PlayerEntity player);
        public abstract boolean canGenerateQuest(VillagerEntity villager, PlayerEntity player);
        public abstract QuestPriority calculatePriority(VillagerEntity villager);
    }
    
    public static class FetchQuestGenerator extends QuestGenerator {
        @Override
        public Quest generateQuest(VillagerEntity villager, PlayerEntity player) {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            if (data == null) return null;
            
            // Determine what item to fetch based on villager's profession and needs
            String professionId = net.minecraft.registry.Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().profession()).getPath();
            Map<String, String> itemMap = switch (professionId) {
                case "farmer" -> Map.of(
                    "Seeds", "I need some seeds to expand my farm",
                    "Bone Meal", "Bone meal would help my crops grow faster",
                    "Water Bucket", "I need water for my irrigation system"
                );
                case "librarian" -> Map.of(
                    "Paper", "I need paper to write more books",
                    "Ink Sac", "I'm running low on ink for my writing",
                    "Feather", "A feather would help me craft better quills"
                );
                case "toolsmith" -> Map.of(
                    "Iron Ingot", "I need iron for my smithing work",
                    "Coal", "My forge is running low on fuel",
                    "Diamond", "A diamond would let me craft superior tools"
                );
                case "armorer" -> Map.of(
                    "Iron Ingot", "I need iron for my armor work",
                    "Leather", "Leather helps me craft flexible armor",
                    "Diamond", "A diamond would let me craft superior armor"
                );
                case "weaponsmith" -> Map.of(
                    "Iron Ingot", "I need iron for my tool work",
                    "Stick", "Sticks are needed for tool handles",
                    "Diamond", "A diamond would let me craft superior tools"
                );
                case "fletcher" -> Map.of(
                    "Iron Ingot", "I need iron for my weapon work",
                    "Stick", "Sticks are needed for weapon handles",
                    "Diamond", "A diamond would let me craft superior weapons"
                );
                default -> Map.of(
                    "Food", "I'm getting quite hungry",
                    "Wood", "I need some wood for repairs around here",
                    "Stone", "Some stone would be useful for construction"
                );
            };
            
            // Select random item and reason
            List<String> items = new ArrayList<>(itemMap.keySet());
            String selectedItem = items.get(ThreadLocalRandom.current().nextInt(items.size()));
            String reason = itemMap.get(selectedItem);
            
            Quest quest = new Quest(
                "Fetch " + selectedItem,
                "Help " + data.getName() + " by bringing them " + selectedItem.toLowerCase() + ". " + reason,
                QuestType.FETCH_ITEM,
                calculatePriority(villager),
                villager,
                player
            );
            
            quest.objectives.put("fetch_item", "Bring " + selectedItem.toLowerCase() + " to " + data.getName());
            quest.objectives.put("item_type", selectedItem.toLowerCase());
            quest.objectives.put("quantity", 1);
            
            // Rewards based on item difficulty
            int emeraldReward = switch (selectedItem.toLowerCase()) {
                case "diamond" -> 10;
                case "iron ingot", "coal" -> 3;
                default -> 1;
            };
            
            quest.rewards.put("emeralds", emeraldReward + " Emerald" + (emeraldReward > 1 ? "s" : ""));
            quest.rewards.put("reputation", "Improved relationship with " + data.getName());
            
            return quest;
        }
        
        @Override
        public boolean canGenerateQuest(VillagerEntity villager, PlayerEntity player) {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            return data != null && data.getPlayerReputation(player.getUuidAsString()) > -20;
        }
        
        @Override
        public QuestPriority calculatePriority(VillagerEntity villager) {
            VillagerEmotionSystem.EmotionalState emotions = VillagerEmotionSystem.getEmotionalState(villager);
            float stress = emotions.getEmotion(VillagerEmotionSystem.EmotionType.STRESS);
            
            if (stress > 70) return QuestPriority.HIGH;
            if (stress > 40) return QuestPriority.MEDIUM;
            return QuestPriority.LOW;
        }
    }
    
    public static class DeliveryQuestGenerator extends QuestGenerator {
        @Override
        public Quest generateQuest(VillagerEntity villager, PlayerEntity player) {
            VillagerData senderData = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            if (senderData == null) return null;
            
            // Find potential delivery recipients
            List<VillagerEntity> nearbyVillagers = villager.getWorld().getEntitiesByClass(
                VillagerEntity.class,
                villager.getBoundingBox().expand(50),
                v -> v != villager && v.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA) != null
            );
            
            if (nearbyVillagers.isEmpty()) return null;
            
            VillagerEntity recipient = nearbyVillagers.get(ThreadLocalRandom.current().nextInt(nearbyVillagers.size()));
            VillagerData recipientData = recipient.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            
            // Generate delivery item and message
            String[] items = {"Letter", "Package", "Gift", "Important Document", "Family Heirloom"};
            String item = items[ThreadLocalRandom.current().nextInt(items.length)];
            
            String relationship = senderData.getFamilyMembers().contains(recipient.getUuidAsString()) ? "family member" :
                                senderData.getSpouseId().equals(recipient.getUuidAsString()) ? "spouse" : "friend";
            
            Quest quest = new Quest(
                "Deliver " + item,
                senderData.getName() + " wants you to deliver a " + item.toLowerCase() +
                    " to their " + relationship + " " + recipientData.getName() + ".",
                QuestType.DELIVERY,
                calculatePriority(villager),
                villager,
                player
            );
            
            quest.objectives.put("delivery", "Take the " + item.toLowerCase() + " to " + recipientData.getName());
            quest.objectives.put("recipient_uuid", recipient.getUuidAsString());
            quest.objectives.put("item_type", item.toLowerCase());
            
            quest.rewards.put("emeralds", "2 Emeralds");
            quest.rewards.put("reputation", "Improved relationship with both " + senderData.getName() + " and " + recipientData.getName());
            
            return quest;
        }
        
        @Override
        public boolean canGenerateQuest(VillagerEntity villager, PlayerEntity player) {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            return data != null && 
                   data.getPlayerReputation(player.getUuidAsString()) > 0 &&
                   (!data.getFamilyMembers().isEmpty() || !data.getSpouseId().isEmpty());
        }
        
        @Override
        public QuestPriority calculatePriority(VillagerEntity villager) {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            if (data == null) return QuestPriority.LOW;
            
            // Higher priority if villager has strong family connections
            if (!data.getSpouseId().isEmpty() || data.getFamilyMembers().size() > 2) {
                return QuestPriority.MEDIUM;
            }
            
            return QuestPriority.LOW;
        }
    }
    
    public static class SocialQuestGenerator extends QuestGenerator {
        @Override
        public Quest generateQuest(VillagerEntity villager, PlayerEntity player) {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            VillagerEmotionSystem.EmotionalState emotions = VillagerEmotionSystem.getEmotionalState(villager);
            
            if (data == null) return null;
            
            float loneliness = emotions.getEmotion(VillagerEmotionSystem.EmotionType.LONELINESS);
            
            if (loneliness > 60 && data.getSpouseId().isEmpty()) {
                // Lonely villager wants help finding love
                return generateRomanceQuest(villager, player, data);
            } else if (data.getPlayerReputation("general") < -10) {
                // Villager with bad reputation wants help
                return generateReputationQuest(villager, player, data);
            } else {
                // General social quest
                return generateFriendshipQuest(villager, player, data);
            }
        }
        
        private Quest generateRomanceQuest(VillagerEntity villager, PlayerEntity player, VillagerData data) {
            Quest quest = new Quest(
                "Matters of the Heart",
                data.getName() + " is feeling lonely and would like help finding someone special. " +
                "Perhaps you could introduce them to someone compatible?",
                QuestType.ROMANCE,
                QuestPriority.MEDIUM,
                villager,
                player
            );
            
            quest.objectives.put("introduction", "Introduce " + data.getName() + " to a compatible villager");
            quest.objectives.put("compatibility_check", "Ensure they have compatible personalities");
            
            quest.rewards.put("emeralds", "5 Emeralds");
            quest.rewards.put("reputation", "Greatly improved relationship");
            quest.rewards.put("special", "Invitation to the wedding if successful");
            
            return quest;
        }
        
        private Quest generateReputationQuest(VillagerEntity villager, PlayerEntity player, VillagerData data) {
            Quest quest = new Quest(
                "Redemption",
                data.getName() + " has been having trouble with their reputation in the village. " +
                "Help them make amends and rebuild their relationships.",
                QuestType.SOCIAL,
                QuestPriority.HIGH,
                villager,
                player
            );
            
            quest.objectives.put("good_deeds", "Help " + data.getName() + " do 3 good deeds for other villagers");
            quest.objectives.put("apologies", "Facilitate apologies to those they've wronged");
            
            quest.rewards.put("emeralds", "8 Emeralds");
            quest.rewards.put("reputation", "Village-wide reputation improvement");
            
            return quest;
        }
        
        private Quest generateFriendshipQuest(VillagerEntity villager, PlayerEntity player, VillagerData data) {
            Quest quest = new Quest(
                "Making Friends",
                data.getName() + " would like to expand their social circle. " +
                "Help them connect with other villagers who share their interests.",
                QuestType.SOCIAL,
                QuestPriority.LOW,
                villager,
                player
            );
            
            quest.objectives.put("introductions", "Introduce " + data.getName() + " to 2 new villagers");
            quest.objectives.put("shared_activity", "Organize a group activity they can enjoy together");
            
            quest.rewards.put("emeralds", "3 Emeralds");
            quest.rewards.put("reputation", "Improved relationship with multiple villagers");
            
            return quest;
        }
        
        @Override
        public boolean canGenerateQuest(VillagerEntity villager, PlayerEntity player) {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            return data != null && data.getPlayerReputation(player.getUuidAsString()) > 20;
        }
        
        @Override
        public QuestPriority calculatePriority(VillagerEntity villager) {
            VillagerEmotionSystem.EmotionalState emotions = VillagerEmotionSystem.getEmotionalState(villager);
            float loneliness = emotions.getEmotion(VillagerEmotionSystem.EmotionType.LONELINESS);
            
            if (loneliness > 80) return QuestPriority.HIGH;
            if (loneliness > 50) return QuestPriority.MEDIUM;
            return QuestPriority.LOW;
        }
    }
    
    public static class MysteryQuestGenerator extends QuestGenerator {
        @Override
        public Quest generateQuest(VillagerEntity villager, PlayerEntity player) {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            if (data == null) return null;
            
            String[] mysteries = {
                "Strange sounds coming from the old mine",
                "Missing items from around the village",
                "Mysterious lights seen at night",
                "Ancient ruins discovered nearby",
                "Unusual animal behavior",
                "Old treasure map found"
            };
            
            String mystery = mysteries[ThreadLocalRandom.current().nextInt(mysteries.length)];
            
            Quest quest = new Quest(
                "Village Mystery",
                data.getName() + " has noticed something strange: " + mystery.toLowerCase() + 
                ". They think you might be able to investigate and solve this mystery.",
                QuestType.MYSTERY,
                calculatePriority(villager),
                villager,
                player
            );
            
            quest.objectives.put("investigation", "Investigate the " + mystery.toLowerCase());
            quest.objectives.put("clues", "Find and collect clues about what's happening");
            quest.objectives.put("solution", "Solve the mystery and report back");
            
            quest.rewards.put("emeralds", "10 Emeralds");
            quest.rewards.put("reputation", "Hero status in the village");
            quest.rewards.put("special", "Possible rare item discovery");
            
            return quest;
        }
        
        @Override
        public boolean canGenerateQuest(VillagerEntity villager, PlayerEntity player) {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            return data != null && 
                   data.getPlayerReputation(player.getUuidAsString()) > 40 &&
                   data.getPersonality().equals("Curious");
        }
        
        @Override
        public QuestPriority calculatePriority(VillagerEntity villager) {
            return QuestPriority.MEDIUM; // Mysteries are always moderately important
        }
    }
    
    // Quest management system
    public static class QuestManager {
        private static final Map<String, Quest> activeQuests = new ConcurrentHashMap<>();
        private static final Map<String, List<Quest>> playerQuests = new ConcurrentHashMap<>();
        private static final List<QuestGenerator> questGenerators = Arrays.asList(
            new FetchQuestGenerator(),
            new DeliveryQuestGenerator(),
            new SocialQuestGenerator(),
            new MysteryQuestGenerator()
        );
        
        public static Quest generateQuestForVillager(VillagerEntity villager, PlayerEntity player) {
            // Check cooldown - don't generate quests too frequently
            if (hasRecentQuest(villager, player)) return null;
            
            // Find available quest generators
            List<QuestGenerator> availableGenerators = questGenerators.stream()
                .filter(gen -> gen.canGenerateQuest(villager, player))
                .toList();
            
            if (availableGenerators.isEmpty()) return null;
            
            // Select generator based on priority and randomness
            QuestGenerator selectedGenerator = selectWeightedGenerator(availableGenerators, villager);
            Quest quest = selectedGenerator.generateQuest(villager, player);
            
            if (quest != null) {
                activeQuests.put(quest.id, quest);
                playerQuests.computeIfAbsent(player.getUuidAsString(), k -> new ArrayList<>()).add(quest);
                
                // Create gossip about the quest
                // Gossip creation temporarily disabled to match current API and compile
                // VillagerGossipNetwork.createGossip(villager, player.getUuidAsString(), VillagerGossipNetwork.GossipType.SOME_TYPE,
                //     "Gave " + player.getName().getString() + " a task to help the village");
            }
            
            return quest;
        }
        
        private static boolean hasRecentQuest(VillagerEntity villager, PlayerEntity player) {
            List<Quest> playerQuestList = playerQuests.get(player.getUuidAsString());
            if (playerQuestList == null) return false;
            
            long cooldownTime = 600000; // 10 minutes
            long currentTime = System.currentTimeMillis();
            
            return playerQuestList.stream()
                .anyMatch(quest -> quest.questGiver.equals(villager) && 
                         (currentTime - quest.creationTime) < cooldownTime);
        }
        
        private static QuestGenerator selectWeightedGenerator(List<QuestGenerator> generators, VillagerEntity villager) {
            Map<QuestGenerator, Float> weights = new HashMap<>();
            
            for (QuestGenerator generator : generators) {
                QuestPriority priority = generator.calculatePriority(villager);
                weights.put(generator, (float) priority.level);
            }
            
            float totalWeight = weights.values().stream().reduce(0.0f, Float::sum);
            float random = ThreadLocalRandom.current().nextFloat() * totalWeight;
            
            float currentWeight = 0;
            for (Map.Entry<QuestGenerator, Float> entry : weights.entrySet()) {
                currentWeight += entry.getValue();
                if (random <= currentWeight) {
                    return entry.getKey();
                }
            }
            
            return generators.get(0); // Fallback
        }
        
        public static List<Quest> getPlayerQuests(PlayerEntity player) {
            return new ArrayList<>(playerQuests.getOrDefault(player.getUuidAsString(), new ArrayList<>()));
        }
        
        public static List<Quest> getActiveQuests(PlayerEntity player) {
            return getPlayerQuests(player).stream()
                .filter(quest -> quest.getStatus() == Quest.QuestStatus.ACCEPTED || 
                              quest.getStatus() == Quest.QuestStatus.IN_PROGRESS)
                .toList();
        }
        
        public static boolean acceptQuest(String questId, PlayerEntity player) {
            Quest quest = activeQuests.get(questId);
            if (quest != null && quest.questReceiver.equals(player) && 
                quest.getStatus() == Quest.QuestStatus.AVAILABLE) {
                quest.setStatus(Quest.QuestStatus.ACCEPTED);
                return true;
            }
            return false;
        }
        
        public static boolean completeQuest(String questId, PlayerEntity player) {
            Quest quest = activeQuests.get(questId);
            if (quest != null && quest.questReceiver.equals(player) && 
                quest.getCompletionPercentage() >= 1.0f) {
                
                quest.setStatus(Quest.QuestStatus.COMPLETED);
                giveQuestRewards(quest, player);
                
                // Create positive gossip about quest completion
                VillagerData questGiverData = quest.questGiver.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
                if (questGiverData != null) {
                    VillagerGossipNetwork.createGossip(quest.questGiver, player.getUuidAsString(),
                        VillagerGossipNetwork.GossipType.ACHIEVEMENT, 
                        "Successfully completed a quest for " + questGiverData.getName());
                }
                
                return true;
            }
            return false;
        }
        
        private static void giveQuestRewards(Quest quest, PlayerEntity player) {
            VillagerData questGiverData = quest.questGiver.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            
            for (Map.Entry<String, Object> reward : quest.rewards.entrySet()) {
                switch (reward.getKey()) {
                    case "emeralds" -> {
                        // Give actual emeralds to the player
                        try {
                            String rewardText = reward.getValue().toString();
                            int emeraldCount = 1; // default
                            
                            // Parse emerald count from reward text like "3 Emeralds" or "10 Emerald"
                            String[] parts = rewardText.split(" ");
                            if (parts.length > 0) {
                                try {
                                    emeraldCount = Integer.parseInt(parts[0]);
                                } catch (NumberFormatException e) {
                                    // Use default of 1 if parsing fails
                                }
                            }
                            
                            // Give emeralds to player
                            ItemStack emeralds = new ItemStack(net.minecraft.item.Items.EMERALD, emeraldCount);
                            if (!player.getInventory().insertStack(emeralds)) {
                                // If inventory is full, drop at player's location
                                player.dropStack((net.minecraft.server.world.ServerWorld) player.getWorld(), emeralds);
                            }
                            
                            player.sendMessage(Text.literal("Received " + emeraldCount + " emerald" + (emeraldCount > 1 ? "s" : "") + "!")
                                .formatted(Formatting.GREEN), false);
                            
                            // Also improve reputation
                            if (questGiverData != null) {
                                questGiverData.updatePlayerRelation(player.getUuidAsString(), 15);
                            }
                        } catch (Exception e) {
                            // Fallback to just reputation if emerald giving fails
                            if (questGiverData != null) {
                                questGiverData.updatePlayerRelation(player.getUuidAsString(), 20);
                            }
                        }
                    }
                    case "reputation" -> {
                        if (questGiverData != null) {
                            questGiverData.updatePlayerRelation(player.getUuidAsString(), 30);
                        }
                    }
                    case "special" -> {
                        // Special rewards implementation
                        String specialReward = reward.getValue().toString().toLowerCase();
                        
                        switch (specialReward) {
                            case "rare_book" -> {
                                ItemStack book = new ItemStack(net.minecraft.item.Items.BOOK);
                                book.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal("Village Chronicle").formatted(Formatting.GOLD));
                                if (!player.getInventory().insertStack(book)) {
                                    player.dropStack((net.minecraft.server.world.ServerWorld) player.getWorld(), book);
                                }
                                player.sendMessage(Text.literal("Received a rare book!").formatted(Formatting.GOLD), false);
                            }
                            case "friendship_token" -> {
                                ItemStack token = new ItemStack(net.minecraft.item.Items.GOLD_NUGGET);
                                token.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal("Friendship Token").formatted(Formatting.YELLOW));
                                if (!player.getInventory().insertStack(token)) {
                                    player.dropStack((net.minecraft.server.world.ServerWorld) player.getWorld(), token);
                                }
                                player.sendMessage(Text.literal("Received a friendship token!").formatted(Formatting.YELLOW), false);
                                
                                // Boost reputation with all villagers
                                if (questGiverData != null) {
                                    questGiverData.updatePlayerRelation(player.getUuidAsString(), 50);
                                }
                            }
                            case "village_key" -> {
                                ItemStack key = new ItemStack(net.minecraft.item.Items.TRIPWIRE_HOOK);
                                key.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal("Village Key").formatted(Formatting.AQUA));
                                if (!player.getInventory().insertStack(key)) {
                                    player.dropStack((net.minecraft.server.world.ServerWorld) player.getWorld(), key);
                                }
                                player.sendMessage(Text.literal("Received a village key! This grants you special access.").formatted(Formatting.AQUA), false);
                            }
                            default -> {
                                // Default special reward: extra reputation and a small gift
                                if (questGiverData != null) {
                                    questGiverData.updatePlayerRelation(player.getUuidAsString(), 25);
                                }
                                ItemStack gift = new ItemStack(net.minecraft.item.Items.COOKIE, 3);
                                if (!player.getInventory().insertStack(gift)) {
                                    player.dropStack((net.minecraft.server.world.ServerWorld) player.getWorld(), gift);
                                }
                                player.sendMessage(Text.literal("Received a special thank you gift!").formatted(Formatting.LIGHT_PURPLE), false);
                            }
                        }
                    }
                }
            }
            
            // Emotional impact on quest giver
            VillagerEmotionSystem.processEmotionalEvent(quest.questGiver,
                new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.HAPPINESS, 25.0f, "quest_completed", false));
        }
        
        public static void updateQuestProgress(String questId, String progressKey, Object value) {
            Quest quest = activeQuests.get(questId);
            if (quest != null) {
                quest.updateProgress(progressKey, value);
                
                if (quest.getStatus() == Quest.QuestStatus.ACCEPTED) {
                    quest.setStatus(Quest.QuestStatus.IN_PROGRESS);
                }
            }
        }
        
        public static void cleanupExpiredQuests() {
            activeQuests.entrySet().removeIf(entry -> {
                Quest quest = entry.getValue();
                if (quest.isExpired()) {
                    quest.setStatus(Quest.QuestStatus.EXPIRED);
                    return true;
                }
                return false;
            });
            
            // Clean up completed and failed quests older than 24 hours
            long cutoffTime = System.currentTimeMillis() - 86400000;
            playerQuests.values().forEach(questList -> 
                questList.removeIf(quest -> 
                    (quest.getStatus() == Quest.QuestStatus.COMPLETED || 
                     quest.getStatus() == Quest.QuestStatus.FAILED) &&
                    quest.creationTime < cutoffTime
                )
            );
        }
        
        public static List<Text> getQuestListForPlayer(PlayerEntity player) {
            List<Quest> quests = getActiveQuests(player);
            List<Text> questList = new ArrayList<>();
            
            if (quests.isEmpty()) {
                questList.add(Text.literal("No active quests").formatted(Formatting.GRAY));
                return questList;
            }
            
            questList.add(Text.literal("=== Active Quests ===").formatted(Formatting.GOLD, Formatting.BOLD));
            questList.add(Text.empty());
            
            for (Quest quest : quests) {
                Formatting statusColor = switch (quest.priority) {
                    case LOW -> Formatting.GRAY;
                    case MEDIUM -> Formatting.YELLOW;
                    case HIGH -> Formatting.GOLD;
                    case URGENT -> Formatting.RED;
                };
                
                questList.add(Text.literal("• " + quest.title).formatted(statusColor));
                questList.add(Text.literal("  Progress: " + (int)(quest.getCompletionPercentage() * 100) + "%")
                    .formatted(Formatting.AQUA));
                questList.add(Text.empty());
            }
            
            return questList;
        }
    }
    
    // Integration helpers
    public static void onPlayerInteractWithVillager(VillagerEntity villager, PlayerEntity player) {
        // Chance to offer a quest
        if (ThreadLocalRandom.current().nextFloat() < 0.1f) { // 10% chance
            Quest quest = QuestManager.generateQuestForVillager(villager, player);
            if (quest != null) {
                player.sendMessage(Text.literal("" + villager.getName().getString() + " has a quest for you!")
                    .formatted(Formatting.GREEN), false);
            }
        }
    }
    
    public static void onItemDelivered(PlayerEntity player, VillagerEntity recipient, String itemType) {
        List<Quest> activeQuests = QuestManager.getActiveQuests(player);
        
        for (Quest quest : activeQuests) {
            if (quest.type == QuestType.DELIVERY && 
                quest.objectives.get("recipient_uuid").equals(recipient.getUuidAsString()) &&
                quest.objectives.get("item_type").equals(itemType)) {
                
                QuestManager.updateQuestProgress(quest.id, "delivery_completed", true);
                player.sendMessage(Text.literal("Quest objective completed!").formatted(Formatting.GREEN), false);
                
                if (quest.getCompletionPercentage() >= 1.0f) {
                    QuestManager.completeQuest(quest.id, player);
                    player.sendMessage(Text.literal("Quest completed: " + quest.title).formatted(Formatting.GOLD), false);
                }
            }
        }
    }
    
    public static void onItemFetchedToVillager(PlayerEntity player, VillagerEntity villager, String itemType, int quantity) {
        List<Quest> activeQuests = QuestManager.getActiveQuests(player);
        
        for (Quest quest : activeQuests) {
            if (quest.type == QuestType.FETCH_ITEM && 
                quest.questGiver.equals(villager) &&
                quest.objectives.get("item_type").equals(itemType)) {
                
                int requiredQuantity = (Integer) quest.objectives.get("quantity");
                if (quantity >= requiredQuantity) {
                    QuestManager.updateQuestProgress(quest.id, "fetch_item_completed", true);
                    QuestManager.completeQuest(quest.id, player);
                    player.sendMessage(Text.literal("Quest completed: " + quest.title).formatted(Formatting.GOLD), false);
                }
            }
        }
    }
}