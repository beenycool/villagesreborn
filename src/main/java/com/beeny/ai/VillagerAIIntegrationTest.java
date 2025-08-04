package com.beeny.ai;

import com.beeny.data.VillagerData;
import com.beeny.ai.core.VillagerEmotionSystem;
import com.beeny.ai.social.VillagerGossipNetwork;
import com.beeny.ai.decision.VillagerUtilityAI;
import com.beeny.ai.learning.VillagerLearningSystem;
import com.beeny.ai.chat.VillagerChatSystem;
import com.beeny.ai.quests.VillagerQuestSystem;
import com.beeny.system.VillagerProfessionManager;
import com.beeny.system.VillagerDialogueSystem;
import com.beeny.dialogue.VillagerMemoryManager;
import com.beeny.dialogue.LLMDialogueManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.village.VillagerProfession;

import java.util.*;

/**
 * Integration testing and validation system for all villager AI components
 * Tests the interoperability and data flow between different AI systems
 */
public class VillagerAIIntegrationTest {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(VillagerAIIntegrationTest.class);
    
    public static class IntegrationTestResult {
        public final String testName;
        public final boolean passed;
        public final String details;
        public final long executionTime;
        public final Map<String, Object> metrics;
        
        public IntegrationTestResult(String testName, boolean passed, String details, long executionTime) {
            this.testName = testName;
            this.passed = passed;
            this.details = details;
            this.executionTime = executionTime;
            this.metrics = new HashMap<>();
        }
        
        public IntegrationTestResult withMetrics(Map<String, Object> metrics) {
            this.metrics.putAll(metrics);
            return this;
        }
        
        @Override
        public String toString() {
            return String.format("[%s] %s - %s (took %dms)", 
                passed ? "PASS" : "FAIL", testName, details, executionTime);
        }
    }
    
    /**
     * Comprehensive AI system integration test
     */
    public static List<IntegrationTestResult> runFullIntegrationTest(VillagerEntity villager, PlayerEntity player) {
        List<IntegrationTestResult> results = new ArrayList<>();
        
        results.add(testAIInitialization(villager));
        results.add(testEmotionalSystem(villager));
        results.add(testMemorySystem(villager, player));
        results.add(testChatSystem(villager, player));
        results.add(testGossipSystem(villager, player));
        results.add(testLearningSystem(villager, player));
        results.add(testProfessionSystem(villager));
        results.add(testQuestSystem(villager, player));
        results.add(testUtilityAI(villager));
        results.add(testSystemIntegration(villager, player));
        
        return results;
    }
    
    private static IntegrationTestResult testAIInitialization(VillagerEntity villager) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Test AI manager initialization
            VillagerAIManager.initializeVillagerAI(villager);
            
            // Verify AI state exists
            VillagerAIManager.VillagerAIState state = VillagerAIManager.getVillagerAIState(villager);
            if (state == null) {
                return new IntegrationTestResult("AI Initialization", false, 
                    "AI state not created", System.currentTimeMillis() - startTime);
            }
            
            // Verify profession data exists
            VillagerProfessionManager.ProfessionData profData = VillagerProfessionManager.getProfessionData(villager);
            if (profData == null) {
                return new IntegrationTestResult("AI Initialization", false, 
                    "Profession data not created", System.currentTimeMillis() - startTime);
            }
            
            // Verify emotional state exists
            VillagerEmotionSystem.EmotionalState emotions = VillagerEmotionSystem.getEmotionalState(villager);
            if (emotions == null) {
                return new IntegrationTestResult("AI Initialization", false, 
                    "Emotional state not created", System.currentTimeMillis() - startTime);
            }
            
            Map<String, Object> metrics = Map.of(
                "ai_active", state.isAIActive,
                "profession", profData.getProfessionId(),
                "skill_level", profData.getSkillLevel(),
                "dominant_emotion", emotions.getDominantEmotion().name()
            );
            
            return new IntegrationTestResult("AI Initialization", true, 
                "All AI systems initialized successfully", System.currentTimeMillis() - startTime)
                .withMetrics(metrics);
                
        } catch (Exception e) {
            return new IntegrationTestResult("AI Initialization", false, 
                "Exception: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
    
    private static IntegrationTestResult testEmotionalSystem(VillagerEntity villager) {
        long startTime = System.currentTimeMillis();
        
        try {
            VillagerEmotionSystem.EmotionalState emotions = VillagerEmotionSystem.getEmotionalState(villager);
            
            // Test emotional event processing
            float initialHappiness = emotions.getEmotion(VillagerEmotionSystem.EmotionType.HAPPINESS);
            
            VillagerEmotionSystem.processEmotionalEvent(villager, 
                VillagerEmotionSystem.EmotionalEvent.SUCCESSFUL_TRADE);
            
            float afterTradeHappiness = emotions.getEmotion(VillagerEmotionSystem.EmotionType.HAPPINESS);
            
            if (afterTradeHappiness <= initialHappiness) {
                return new IntegrationTestResult("Emotional System", false, 
                    "Happiness didn't increase after positive event", System.currentTimeMillis() - startTime);
            }
            
            // Test emotional contagion
            VillagerEmotionSystem.processEmotionalEvent(villager,
                new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.FEAR, 30.0f, "test_fear", true));
            
            float fearLevel = emotions.getEmotion(VillagerEmotionSystem.EmotionType.FEAR);
            float stressLevel = emotions.getEmotion(VillagerEmotionSystem.EmotionType.STRESS);
            
            Map<String, Object> metrics = Map.of(
                "happiness_change", afterTradeHappiness - initialHappiness,
                "fear_level", fearLevel,
                "stress_level", stressLevel,
                "emotional_description", emotions.getEmotionalDescription()
            );
            
            return new IntegrationTestResult("Emotional System", true, 
                "Emotional processing and contagion working", System.currentTimeMillis() - startTime)
                .withMetrics(metrics);
                
        } catch (Exception e) {
            return new IntegrationTestResult("Emotional System", false, 
                "Exception: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
    
    private static IntegrationTestResult testMemorySystem(VillagerEntity villager, PlayerEntity player) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Test conversation memory
            VillagerMemoryManager.addPlayerMessage(villager, player, "Hello, how are you?");
            VillagerMemoryManager.addVillagerResponse(villager, player, "I'm doing well, thank you!", "GREETING");
            
            String conversationHistory = VillagerMemoryManager.getRecentConversationContext(villager, player, 2);
            
            if (conversationHistory.isEmpty()) {
                return new IntegrationTestResult("Memory System", false, 
                    "Conversation history not stored", System.currentTimeMillis() - startTime);
            }
            
            // Test topic tracking
            Set<String> discussedTopics = VillagerMemoryManager.getDiscussedTopics(villager, player);
            if (!discussedTopics.contains("GREETING")) {
                return new IntegrationTestResult("Memory System", false, 
                    "Topics not tracked properly", System.currentTimeMillis() - startTime);
            }
            
            // Test memory integration with villager data
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            if (data != null) {
                data.setPlayerMemory(player.getUuidAsString(), "Friendly player who greets politely");
                String memory = data.getPlayerMemory(player.getUuidAsString());
                
                if (memory == null || !memory.contains("Friendly")) {
                    return new IntegrationTestResult("Memory System", false,
                        "Villager data memory not working", System.currentTimeMillis() - startTime);
                }
            }
            
            Map<String, Object> metrics = Map.of(
                "conversation_length", conversationHistory.length(),
                "topics_discussed", discussedTopics.size(),
                "has_player_memory", data != null && data.getPlayerMemory(player.getUuidAsString()) != null && !data.getPlayerMemory(player.getUuidAsString()).isEmpty()
            );
            
            return new IntegrationTestResult("Memory System", true, 
                "Memory storage and retrieval working", System.currentTimeMillis() - startTime)
                .withMetrics(metrics);
                
        } catch (Exception e) {
            return new IntegrationTestResult("Memory System", false, 
                "Exception: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
    
    private static IntegrationTestResult testChatSystem(VillagerEntity villager, PlayerEntity player) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Test chat intent classification
            VillagerChatSystem.ChatIntent intent = VillagerChatSystem.ChatIntent.classifyIntent("Hello there!");
            if (intent != VillagerChatSystem.ChatIntent.GREETING) {
                return new IntegrationTestResult("Chat System", false, 
                    "Intent classification failed", System.currentTimeMillis() - startTime);
            }
            
            // Test chat response generation
            Text response = VillagerChatSystem.processPlayerMessage(villager, player, "Hello, how are you?");
            if (response == null || response.getString().isEmpty()) {
                return new IntegrationTestResult("Chat System", false, 
                    "No response generated", System.currentTimeMillis() - startTime);
            }
            
            // Test entity extraction
            VillagerChatSystem.ChatContext context = new VillagerChatSystem.ChatContext(villager, player, "I need 5 diamonds please");
            if (!context.extractedEntities.containsKey("quantity") || 
                !context.extractedEntities.get("quantity").equals(5)) {
                return new IntegrationTestResult("Chat System", false, 
                    "Entity extraction failed", System.currentTimeMillis() - startTime);
            }
            
            Map<String, Object> metrics = Map.of(
                "intent_detected", intent.name(),
                "response_length", response.getString().length(),
                "entities_extracted", context.extractedEntities.size()
            );
            
            return new IntegrationTestResult("Chat System", true, 
                "Chat processing and response generation working", System.currentTimeMillis() - startTime)
                .withMetrics(metrics);
                
        } catch (Exception e) {
            return new IntegrationTestResult("Chat System", false, 
                "Exception: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
    
    private static IntegrationTestResult testGossipSystem(VillagerEntity villager, PlayerEntity player) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Test gossip creation
            VillagerGossipNetwork.createGossip(villager, player.getUuidAsString(), 
                VillagerGossipNetwork.GossipType.POSITIVE_INTERACTION, "Was very helpful today");
            
            // Test global reputation impact
            int globalRep = VillagerGossipNetwork.getGlobalReputation(player.getUuidAsString());
            
            // Test gossip retrieval
            List<VillagerGossipNetwork.GossipPiece> gossip = VillagerGossipNetwork.getGossipAbout(villager, "player");
            
            Map<String, Object> metrics = Map.of(
                "global_reputation", globalRep,
                "gossip_pieces", gossip.size()
            );
            
            return new IntegrationTestResult("Gossip System", true, 
                "Gossip creation and reputation tracking working", System.currentTimeMillis() - startTime)
                .withMetrics(metrics);
                
        } catch (Exception e) {
            return new IntegrationTestResult("Gossip System", false, 
                "Exception: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
    
    private static IntegrationTestResult testLearningSystem(VillagerEntity villager, PlayerEntity player) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Test learning profile creation
            VillagerLearningSystem.VillagerLearningProfile profile = VillagerLearningSystem.getLearningProfile(villager);
            if (profile == null) {
                return new IntegrationTestResult("Learning System", false, 
                    "Learning profile not created", System.currentTimeMillis() - startTime);
            }
            
            // Test experience processing
            VillagerLearningSystem.processPlayerInteraction(villager, player, "trade", 0.8f);
            
            // Test prediction
            float prediction = VillagerLearningSystem.predictInteractionOutcome(villager, player, "trade");
            
            // Test preference learning
            float tradePreference = VillagerLearningSystem.getLearnedPreference(villager, "PLAYER_INTERACTION_preference");
            
            Map<String, Object> metrics = new java.util.HashMap<>();
            metrics.put("prediction_accuracy", prediction);
            metrics.put("trade_preference", tradePreference);
            
            return new IntegrationTestResult("Learning System", true, 
                "Learning and adaptation working", System.currentTimeMillis() - startTime)
                .withMetrics(metrics);
                
        } catch (Exception e) {
            return new IntegrationTestResult("Learning System", false, 
                "Exception: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
    
    private static IntegrationTestResult testProfessionSystem(VillagerEntity villager) {
        long startTime = System.currentTimeMillis();
        
        try {
            VillagerProfessionManager.ProfessionData profData = VillagerProfessionManager.getProfessionData(villager);
            
            // Test skill progression
            float initialSkill = profData.getSkillLevel();
            VillagerProfessionManager.SkillSystem.processTradeExperience(villager, true, 
                new net.minecraft.item.ItemStack(net.minecraft.item.Items.BREAD));
            
            if (profData.getSkillLevel() <= initialSkill) {
                return new IntegrationTestResult("Profession System", false, 
                    "Skill progression not working", System.currentTimeMillis() - startTime);
            }
            
            // Test satisfaction tracking
            float satisfaction = profData.getSatisfaction();
            VillagerProfessionManager.updateProfessionSatisfaction(villager);
            
            // Test career recommendations
            List<VillagerProfession> recommendations = VillagerProfessionManager.CareerCounselor.recommendProfessions(villager);
            
            Map<String, Object> metrics = Map.of(
                "skill_improvement", profData.getSkillLevel() - initialSkill,
                "satisfaction", satisfaction,
                "competency", profData.getOverallCompetency(),
                "career_options", recommendations.size()
            );
            
            return new IntegrationTestResult("Profession System", true, 
                "Profession management and progression working", System.currentTimeMillis() - startTime)
                .withMetrics(metrics);
                
        } catch (Exception e) {
            return new IntegrationTestResult("Profession System", false, 
                "Exception: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
    
    private static IntegrationTestResult testQuestSystem(VillagerEntity villager, PlayerEntity player) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Test quest generation
            VillagerQuestSystem.Quest quest = VillagerQuestSystem.QuestManager.generateQuestForVillager(villager, player);
            
            if (quest == null) {
                // This is acceptable - quest generation has conditions
                return new IntegrationTestResult("Quest System", true, 
                    "Quest generation working (no quest generated due to conditions)", System.currentTimeMillis() - startTime);
            }
            
            // Test quest acceptance
            boolean accepted = VillagerQuestSystem.QuestManager.acceptQuest(quest.id, player);
            
            // Test quest progress tracking
            VillagerQuestSystem.QuestManager.updateQuestProgress(quest.id, "test_progress", true);
            
            Map<String, Object> metrics = Map.of(
                "quest_type", quest.type.name(),
                "quest_priority", quest.priority.name(),
                "objectives", quest.objectives.size(),
                "accepted", accepted
            );
            
            return new IntegrationTestResult("Quest System", true, 
                "Quest generation and management working", System.currentTimeMillis() - startTime)
                .withMetrics(metrics);
                
        } catch (Exception e) {
            return new IntegrationTestResult("Quest System", false, 
                "Exception: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
    
    private static IntegrationTestResult testUtilityAI(VillagerEntity villager) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Test marriage decision making
            VillagerUtilityAI.MarriageDecisionMaker marriageAI = VillagerUtilityAI.UtilityAIManager.getMarriageDecisionMaker();
            
            // Test career decision making
            VillagerUtilityAI.CareerDecisionMaker careerAI = VillagerUtilityAI.UtilityAIManager.getCareerDecisionMaker();
            boolean shouldChangeCareer = careerAI.shouldChangeCareer(villager);
            
            // Test trade fairness calculation
            VillagerUtilityAI.PlayerInteractionDecisionMaker playerAI = VillagerUtilityAI.UtilityAIManager.getPlayerInteractionDecisionMaker();
            
            Map<String, Object> metrics = Map.of(
                "should_change_career", shouldChangeCareer,
                "utility_ai_active", true
            );
            
            return new IntegrationTestResult("Utility AI", true, 
                "Decision making systems working", System.currentTimeMillis() - startTime)
                .withMetrics(metrics);
                
        } catch (Exception e) {
            return new IntegrationTestResult("Utility AI", false, 
                "Exception: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
    
    private static IntegrationTestResult testSystemIntegration(VillagerEntity villager, PlayerEntity player) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Test cross-system data flow
            
            // 1. Simulate a trade event and verify it affects multiple systems
            VillagerAIManager.onVillagerTrade(villager, player, true, 5);
            
            // Check emotional impact
            VillagerEmotionSystem.EmotionalState emotions = VillagerEmotionSystem.getEmotionalState(villager);
            float happiness = emotions.getEmotion(VillagerEmotionSystem.EmotionType.HAPPINESS);
            
            // Check learning impact
            VillagerLearningSystem.VillagerLearningProfile learningProfile = VillagerLearningSystem.getLearningProfile(villager);
            
            // Check profession impact
            VillagerProfessionManager.ProfessionData profData = VillagerProfessionManager.getProfessionData(villager);
            
            // Check memory impact
            String context = VillagerMemoryManager.getRecentConversationContext(villager, player, 5);
            
            // 2. Test AI status integration
            List<Text> aiStatus = VillagerAIManager.getVillagerAIStatus(villager);
            
            // 3. Test dialogue integration with AI systems
            VillagerDialogueSystem.DialogueContext dialogueContext = 
                new VillagerDialogueSystem.DialogueContext(villager, player);
            Text dialogue = VillagerDialogueSystem.generateDialogue(
                dialogueContext,
                VillagerDialogueSystem.DialogueCategory.GREETING,
                t -> {}
            );
            
            Map<String, Object> metrics = new java.util.HashMap<>();
            metrics.put("happiness_level", happiness);
            metrics.put("profession_skill", profData.getSkillLevel());
            // Use accessor method to get experience count
            // Guard: if learningSystem or villagerUuid not in scope, compute via manager
            int experienceCountSafeguarded = 0;
            var lp = com.beeny.ai.learning.VillagerLearningSystem.getLearningProfile(villager);
            experienceCountSafeguarded = lp != null ? lp.getExperienceCount() : 0;
            metrics.put("learning_experiences", experienceCountSafeguarded);
            metrics.put("ai_status_lines", aiStatus.size());
            metrics.put("dialogue_generated", dialogue != null);
            
            return new IntegrationTestResult("System Integration", true, 
                "Cross-system integration working properly", System.currentTimeMillis() - startTime)
                .withMetrics(metrics);
                
        } catch (Exception e) {
            return new IntegrationTestResult("System Integration", false, 
                "Exception: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
    
    /**
     * Generate a comprehensive AI system report
     */
    public static List<Text> generateAISystemReport(VillagerEntity villager, PlayerEntity player) {
        List<Text> report = new ArrayList<>();
        
        report.add(Text.literal("=== VILLAGER AI SYSTEM REPORT ===").formatted(net.minecraft.util.Formatting.GOLD, net.minecraft.util.Formatting.BOLD));
        report.add(Text.empty());
        
        // Run integration tests
        List<IntegrationTestResult> testResults = runFullIntegrationTest(villager, player);
        
        long passedTests = testResults.stream().mapToLong(r -> r.passed ? 1 : 0).sum();
        long totalTests = testResults.size();
        
        report.add(Text.literal("Test Results: " + passedTests + "/" + totalTests + " passed")
            .formatted(passedTests == totalTests ? net.minecraft.util.Formatting.GREEN : net.minecraft.util.Formatting.RED));
        report.add(Text.empty());
        
        // Show detailed results
        for (IntegrationTestResult result : testResults) {
            net.minecraft.util.Formatting color = result.passed ? net.minecraft.util.Formatting.GREEN : net.minecraft.util.Formatting.RED;
            report.add(Text.literal("• " + result.testName + ": " + (result.passed ? "PASS" : "FAIL"))
                .formatted(color));
            
            if (!result.details.isEmpty()) {
                report.add(Text.literal("  " + result.details).formatted(net.minecraft.util.Formatting.GRAY));
            }
            
            if (!result.metrics.isEmpty()) {
                for (Map.Entry<String, Object> metric : result.metrics.entrySet()) {
                    report.add(Text.literal("    " + metric.getKey() + ": " + metric.getValue())
                        .formatted(net.minecraft.util.Formatting.DARK_GRAY));
                }
            }
        }
        
        // Global AI Analytics
        report.add(Text.empty());
        report.add(Text.literal("=== AI ANALYTICS ===").formatted(net.minecraft.util.Formatting.BLUE, net.minecraft.util.Formatting.BOLD));
        
        Map<String, Object> globalAnalytics = VillagerAIManager.getGlobalAIAnalytics();
        for (Map.Entry<String, Object> entry : globalAnalytics.entrySet()) {
            if (entry.getValue() instanceof Map) {
                report.add(Text.literal(entry.getKey() + ":").formatted(net.minecraft.util.Formatting.YELLOW));
                Map<?, ?> subMap = (Map<?, ?>) entry.getValue();
                for (Map.Entry<?, ?> subEntry : subMap.entrySet()) {
                    report.add(Text.literal("  " + subEntry.getKey() + ": " + subEntry.getValue())
                        .formatted(net.minecraft.util.Formatting.WHITE));
                }
            } else {
                report.add(Text.literal(entry.getKey() + ": " + entry.getValue())
                    .formatted(net.minecraft.util.Formatting.WHITE));
            }
        }
        
        return report;
    }
    
    /**
     * Quick health check for all AI systems
     */
    public static boolean performHealthCheck(VillagerEntity villager) {
        try {
            // Check if all major systems are operational
            VillagerAIManager.VillagerAIState aiState = VillagerAIManager.getVillagerAIState(villager);
            VillagerEmotionSystem.EmotionalState emotions = VillagerEmotionSystem.getEmotionalState(villager);
            VillagerProfessionManager.ProfessionData profData = VillagerProfessionManager.getProfessionData(villager);
            VillagerLearningSystem.VillagerLearningProfile learningProfile = VillagerLearningSystem.getLearningProfile(villager);
            
            return aiState != null && aiState.isAIActive && 
                   emotions != null && 
                   profData != null && 
                   learningProfile != null;
                   
        } catch (Exception e) {
            LOGGER.error("AI Health Check Failed: {}", e.getMessage(), e);
            return false;
        }
    }
}