package com.beeny.ai.quests;

import com.beeny.data.VillagerData;
import com.beeny.ai.core.VillagerEmotionSystem;
import com.beeny.system.ServerVillagerManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;

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
        public final String giverUuid;
        public final PlayerEntity questReceiver;
        public final Map<String, Object> objectives;
        public final Map<String, Object> rewards;
        public final long timeLimit; // 0 for no time limit
        public final long creationTime;
        
        private volatile QuestStatus status;
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
            this.giverUuid = questGiver != null ? questGiver.getUuidAsString() : "";
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
            String professionId = net.minecraft.registry.Registries.VILLAGER_PROFESSION.getId(villager.getVillagerData().profession().value()).getPath();
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
            VillagerEmotionSystem.EmotionalState emotions = ServerVillagerManager.getInstance().getAIWorldManager().getEmotionSystem().getEmotionalState(villager);
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
            VillagerEmotionSystem.EmotionalState emotions = ServerVillagerManager.getInstance().getAIWorldManager().getEmotionSystem().getEmotionalState(villager);
            
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
            VillagerEmotionSystem.EmotionalState emotions = ServerVillagerManager.getInstance().getAIWorldManager().getEmotionSystem().getEmotionalState(villager);
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
    
    // Instance-based Quest management system (replaces static QuestManager)
    private final Map<String, Quest> activeQuests = new ConcurrentHashMap<>();
    private final Map<String, List<Quest>> playerQuests = new ConcurrentHashMap<>();
    private final List<QuestGenerator> questGenerators = Arrays.asList(
        new FetchQuestGenerator(),
        new DeliveryQuestGenerator(),
        new SocialQuestGenerator(),
        new MysteryQuestGenerator()
    );
    
    // Instance-based lifecycle methods
    public void initializeVillagerQuests(@NotNull VillagerEntity villager, @NotNull VillagerData data) {
        // Initialize quest state for villager if needed
    }

    public void updateVillagerQuests(@NotNull VillagerEntity villager) {
        // Update quest progress, check for completions, etc.
        cleanupExpiredQuests();
    }

    public void cleanupVillager(@NotNull String villagerUuid) {
        // Remove quests associated with this villager
        activeQuests.values().removeIf(quest -> quest.giverUuid.equals(villagerUuid));
    }

    public void performMaintenance() {
        cleanupExpiredQuests();
    }

    public void shutdown() {
        activeQuests.clear();
        playerQuests.clear();
    }

    @NotNull
    public Map<String, Object> getAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("total_active_quests", activeQuests.size());
        analytics.put("total_players_with_quests", playerQuests.size());
        return analytics;
    }
    
    public Quest generateQuestForVillager(VillagerEntity villager, PlayerEntity player) {
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
            
            // Create gossip about the quest through AIWorldManager
            try {
                com.beeny.ai.AIWorldManagerRefactored aiManager = com.beeny.system.ServerVillagerManager.getInstance().getAIWorldManager();
                aiManager.getGossipManager().createGossip(villager, player.getUuidAsString(),
                    com.beeny.ai.social.VillagerGossipManager.GossipType.ACHIEVEMENT,
                    "Gave " + player.getName().getString() + " a task to help the village");
            } catch (Exception e) {
                // Silently handle if AI manager not available
            }
        }
        
        return quest;
    }
    
    private boolean hasRecentQuest(VillagerEntity villager, PlayerEntity player) {
        List<Quest> playerQuestList = playerQuests.get(player.getUuidAsString());
        if (playerQuestList == null) return false;
        
        long cooldownTime = 600000; // 10 minutes
        long currentTime = System.currentTimeMillis();
        
        return playerQuestList.stream()
                .anyMatch(quest -> quest.questGiver.equals(villager) && 
                         (currentTime - quest.creationTime) < cooldownTime);
        }
        
        private QuestGenerator selectWeightedGenerator(List<QuestGenerator> generators, VillagerEntity villager) {
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
        
        public List<Quest> getPlayerQuests(PlayerEntity player) {
            return new ArrayList<>(playerQuests.getOrDefault(player.getUuidAsString(), new ArrayList<>()));
        }
        
        public List<Quest> getActiveQuests(PlayerEntity player) {
            return getPlayerQuests(player).stream()
                .filter(quest -> quest.getStatus() == Quest.QuestStatus.ACCEPTED || 
                              quest.getStatus() == Quest.QuestStatus.IN_PROGRESS)
                .toList();
        }
        
        public boolean acceptQuest(String questId, PlayerEntity player) {
            Quest quest = activeQuests.get(questId);
            if (quest != null && quest.questReceiver.equals(player) && 
                quest.getStatus() == Quest.QuestStatus.AVAILABLE) {
                quest.setStatus(Quest.QuestStatus.ACCEPTED);
                return true;
            }
            return false;
        }
        
        public boolean completeQuest(String questId, PlayerEntity player) {
            Quest quest = activeQuests.get(questId);
            if (quest != null && quest.questReceiver.equals(player) && 
                quest.getCompletionPercentage() >= 1.0f) {
                
                quest.setStatus(Quest.QuestStatus.COMPLETED);
                giveQuestRewards(quest, player);
                
                // Create positive gossip about quest completion through AIWorldManager
                VillagerData questGiverData = quest.questGiver.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
                if (questGiverData != null) {
                    try {
                        com.beeny.ai.AIWorldManagerRefactored aiManager = com.beeny.system.ServerVillagerManager.getInstance().getAIWorldManager();
                        aiManager.getGossipManager().createGossip(quest.questGiver, player.getUuidAsString(),
                            com.beeny.ai.social.VillagerGossipManager.GossipType.ACHIEVEMENT,
                            "Successfully completed a quest for " + questGiverData.getName());
                    } catch (Exception e) {
                        // Silently handle if AI manager not available
                    }
                }
                
                return true;
            }
            return false;
        }
        
        private void giveQuestRewards(Quest quest, PlayerEntity player) {
            VillagerData questGiverData = quest.questGiver.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            
            for (Map.Entry<String, Object> reward : quest.rewards.entrySet()) {
                switch (reward.getKey()) {
                    case "emeralds" -> {
                        // Extract emerald count from reward string and actually give emeralds
                        String rewardValue = reward.getValue().toString();
                        int emeraldCount = parseEmeraldAmount(rewardValue);
                        if (emeraldCount > 0) {
                            ItemStack emeralds = new ItemStack(Items.EMERALD, emeraldCount);
                            if (!player.giveItemStack(emeralds)) {
                                // If inventory is full, drop at player's location
                                player.dropItem(emeralds, false);
                            }
                            player.sendMessage(Text.literal("Received " + emeraldCount + " emerald" + 
                                (emeraldCount > 1 ? "s" : "") + "!").formatted(Formatting.GREEN), false);
                        }
                        // Also improve reputation
                        if (questGiverData != null) {
                            questGiverData.updatePlayerRelation(player.getUuidAsString(), 20);
                        }
                    }
                    case "reputation" -> {
                        if (questGiverData != null) {
                            questGiverData.updatePlayerRelation(player.getUuidAsString(), 30);
                            player.sendMessage(Text.literal("Your reputation with " + questGiverData.getName() + 
                                " has improved!").formatted(Formatting.AQUA), false);
                        }
                    }
                    case "special" -> {
                        // Give special rewards like rare items
                        giveSpecialReward(player, reward.getValue().toString());
                    }
                }
            }
            
            // Emotional impact on quest giver
            ServerVillagerManager.getInstance().getAIWorldManager().getEmotionSystem().processEmotionalEvent(quest.questGiver,
                new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.HAPPINESS, 25.0f, "quest_completed", false));
        }
        
        /**
         * Parse emerald amount from reward string like "5 Emeralds" or "1 Emerald"
         */
        private int parseEmeraldAmount(String rewardString) {
            try {
                String[] parts = rewardString.split(" ");
                if (parts.length > 0) {
                    return Integer.parseInt(parts[0]);
                }
            } catch (NumberFormatException e) {
                // Default to 1 if parsing fails
                return 1;
            }
            return 0;
        }
        
        /**
         * Give special rewards to player
         */
        private void giveSpecialReward(PlayerEntity player, String rewardDescription) {
            // Give some special items based on the reward description
            ItemStack specialItem = null;
            
            if (rewardDescription.toLowerCase().contains("rare item") || 
                rewardDescription.toLowerCase().contains("discovery")) {
                // Give random rare item
                ItemStack[] rareItems = {
                    new ItemStack(Items.DIAMOND, 1),
                    new ItemStack(Items.GOLD_INGOT, 3),
                    new ItemStack(Items.ENDER_PEARL, 2),
                    new ItemStack(Items.ENCHANTED_BOOK, 1),
                    new ItemStack(Items.NAME_TAG, 1)
                };
                specialItem = rareItems[ThreadLocalRandom.current().nextInt(rareItems.length)];
            } else if (rewardDescription.toLowerCase().contains("invitation")) {
                // Give written book as invitation
                specialItem = new ItemStack(Items.WRITTEN_BOOK, 1);
            }
            
            if (specialItem != null) {
                if (!player.giveItemStack(specialItem)) {
                    player.dropItem(specialItem, false);
                }
                player.sendMessage(Text.literal("Received special reward: " + 
                    specialItem.getItem().getName().getString() + "!").formatted(Formatting.LIGHT_PURPLE), false);
            } else {
                player.sendMessage(Text.literal("Received special reward: " + rewardDescription)
                    .formatted(Formatting.LIGHT_PURPLE), false);
            }
        }
        
        public void updateQuestProgress(String questId, String progressKey, Object value) {
            Quest quest = activeQuests.get(questId);
            if (quest != null) {
                quest.updateProgress(progressKey, value);
                
                if (quest.getStatus() == Quest.QuestStatus.ACCEPTED) {
                    quest.setStatus(Quest.QuestStatus.IN_PROGRESS);
                }
            }
        }
        
        public void cleanupExpiredQuests() {
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
        
        public List<Text> getQuestListForPlayer(PlayerEntity player) {
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
    
    // Integration helpers (now use AIWorldManager instance)
    public static void onPlayerInteractWithVillager(VillagerEntity villager, PlayerEntity player) {
        // Chance to offer a quest
        if (ThreadLocalRandom.current().nextFloat() < 0.1f) { // 10% chance
            try {
                com.beeny.ai.AIWorldManagerRefactored aiManager = com.beeny.system.ServerVillagerManager.getInstance().getAIWorldManager();
                Quest quest = aiManager.getQuestSystem().generateQuestForVillager(villager, player);
                if (quest != null) {
                    player.sendMessage(Text.literal("" + villager.getName().getString() + " has a quest for you!")
                        .formatted(Formatting.GREEN), false);
                }
            } catch (Exception e) {
                // Silently handle if AI manager not available
            }
        }
    }
    
    public static void onItemDelivered(PlayerEntity player, VillagerEntity recipient, String itemType) {
        try {
            com.beeny.ai.AIWorldManagerRefactored aiManager = com.beeny.system.ServerVillagerManager.getInstance().getAIWorldManager();
            List<Quest> activeQuests = aiManager.getQuestSystem().getActiveQuests(player);
            
            for (Quest quest : activeQuests) {
                if (quest.type == QuestType.DELIVERY && 
                    quest.objectives.get("recipient_uuid").equals(recipient.getUuidAsString()) &&
                    quest.objectives.get("item_type").equals(itemType)) {
                    
                    aiManager.getQuestSystem().updateQuestProgress(quest.id, "delivery_completed", true);
                    player.sendMessage(Text.literal("Quest objective completed!").formatted(Formatting.GREEN), false);
                    
                    if (quest.getCompletionPercentage() >= 1.0f) {
                        aiManager.getQuestSystem().completeQuest(quest.id, player);
                        player.sendMessage(Text.literal("Quest completed: " + quest.title).formatted(Formatting.GOLD), false);
                    }
                }
            }
        } catch (Exception e) {
            // Silently handle if AI manager not available
        }
    }
    
    public static void onItemFetchedToVillager(PlayerEntity player, VillagerEntity villager, String itemType, int quantity) {
        try {
            com.beeny.ai.AIWorldManagerRefactored aiManager = com.beeny.system.ServerVillagerManager.getInstance().getAIWorldManager();
            List<Quest> activeQuests = aiManager.getQuestSystem().getActiveQuests(player);
            
            for (Quest quest : activeQuests) {
                if (quest.type == QuestType.FETCH_ITEM && 
                    quest.questGiver.equals(villager) &&
                    quest.objectives.get("item_type").equals(itemType)) {
                    
                    int requiredQuantity = (Integer) quest.objectives.get("quantity");
                    if (quantity >= requiredQuantity) {
                        aiManager.getQuestSystem().updateQuestProgress(quest.id, "fetch_item_completed", true);
                        aiManager.getQuestSystem().completeQuest(quest.id, player);
                        player.sendMessage(Text.literal("Quest completed: " + quest.title).formatted(Formatting.GOLD), false);
                    }
                }
            }
        } catch (Exception e) {
            // Silently handle if AI manager not available
        }
    }
}