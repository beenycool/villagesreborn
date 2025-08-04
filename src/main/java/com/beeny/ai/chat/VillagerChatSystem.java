package com.beeny.ai.chat;

import com.beeny.data.VillagerData;
import com.beeny.ai.core.VillagerEmotionSystem;
import com.beeny.ai.social.VillagerGossipNetwork;
import com.beeny.ai.learning.VillagerLearningSystem;
import com.beeny.dialogue.LLMDialogueManager;
import com.beeny.dialogue.VillagerMemoryManager;
import com.beeny.system.VillagerDialogueSystem;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Natural language chat system for player-villager interactions
 * Processes natural language input and generates contextual responses
 */
public class VillagerChatSystem {
    
    // Intent classification for natural language processing
    public enum ChatIntent {
        GREETING("hello", "hi", "hey", "good morning", "good afternoon", "good evening"),
        QUESTION_ABOUT_VILLAGER("how are you", "what's your name", "tell me about yourself", "what do you do"),
        QUESTION_ABOUT_VILLAGE("what's new", "any news", "what's happening", "village", "around here"),
        REQUEST_HELP("help me", "can you help", "i need", "where can i find", "how do i"),
        TRADE_INQUIRY("trade", "buy", "sell", "do you have", "price", "cost"),
        RELATIONSHIP_QUESTION("do you like me", "what do you think", "are we friends", "reputation"),
        GOSSIP_REQUEST("gossip", "rumors", "heard anything", "tell me secrets", "what have you heard"),
        COMPLIMENT("you're nice", "i like you", "you're great", "wonderful", "amazing"),
        INSULT("stupid", "dumb", "hate you", "annoying", "worthless"),
        GOODBYE("bye", "goodbye", "farewell", "see you later", "gotta go"),
        SMALL_TALK("weather", "nice day", "beautiful", "how's life", "what's up"),
        TEACHING("teach me", "show me", "explain", "how does", "what is"),
        QUEST_INQUIRY("quest", "task", "mission", "job", "work for you"),
        UNKNOWN();
        
        private final Set<String> keywords;
        
        ChatIntent(String... keywords) {
            this.keywords = new HashSet<>(Arrays.asList(keywords));
        }
        
        public static ChatIntent classifyIntent(String message) {
            String lowerMessage = message.toLowerCase();
            // Tokenize message into words using regex, preserving multi-word keywords
            Set<String> messageWords = new HashSet<>(Arrays.asList(lowerMessage.split("\\W+")));

            ChatIntent bestIntent = UNKNOWN;
            int bestScore = 0;

            for (ChatIntent intent : values()) {
                if (intent == UNKNOWN) continue;

                int score = 0;
                for (String keyword : intent.keywords) {
                    // For multi-word keywords, match as a phrase; for single words, match as a word
                    if (keyword.contains(" ")) {
                        // Match whole phrase (case-insensitive)
                        String pattern = "\\b" + Pattern.quote(keyword.toLowerCase()) + "\\b";
                        if (Pattern.compile(pattern).matcher(lowerMessage).find()) {
                            score++;
                        }
                    } else {
                        // Match single word
                        if (messageWords.contains(keyword.toLowerCase())) {
                            score++;
                        }
                    }
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestIntent = intent;
                }
            }

            return bestIntent;
        }
    }
    
    // Chat context for maintaining conversation state
    public static class ChatContext {
        public final VillagerEntity villager;
        public final PlayerEntity player;
        public final VillagerData villagerData;
        public final VillagerEmotionSystem.EmotionalState emotionalState;
        public final String lastPlayerMessage;
        public final ChatIntent detectedIntent;
        public final Map<String, Object> extractedEntities;
        
        public ChatContext(VillagerEntity villager, PlayerEntity player, String playerMessage) {
            this.villager = villager;
            this.player = player;
            this.villagerData = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            this.emotionalState = ServerVillagerManager.getInstance().getAIWorldManager().getEmotionManager().getEmotionalState(villager);
            this.lastPlayerMessage = playerMessage;
            this.detectedIntent = ChatIntent.classifyIntent(playerMessage);
            this.extractedEntities = extractEntities(playerMessage);
        }
        
        private Map<String, Object> extractEntities(String message) {
            Map<String, Object> entities = new HashMap<>();
            
            // Extract item names
            Pattern itemPattern = Pattern.compile("\\b(diamond|emerald|iron|gold|bread|carrot|potato|book|enchant)s?\\b", Pattern.CASE_INSENSITIVE);
            Matcher itemMatcher = itemPattern.matcher(message);
            if (itemMatcher.find()) {
                entities.put("item", itemMatcher.group().toLowerCase());
            }
            
            // Extract numbers (for quantities, prices, etc.)
            Pattern numberPattern = Pattern.compile("\\b(\\d+)\\b");
            Matcher numberMatcher = numberPattern.matcher(message);
            if (numberMatcher.find()) {
                entities.put("quantity", Integer.parseInt(numberMatcher.group()));
            }
            
            // Extract time references
            Pattern timePattern = Pattern.compile("\\b(today|yesterday|tomorrow|morning|afternoon|evening|night)\\b", Pattern.CASE_INSENSITIVE);
            Matcher timeMatcher = timePattern.matcher(message);
            if (timeMatcher.find()) {
                entities.put("time", timeMatcher.group().toLowerCase());
            }
            
            return entities;
        }
    }
    
    // Response generator for different intents
    public static class ResponseGenerator {
        
        public static Text generateResponse(ChatContext context) {
            // Record the player message in memory
            VillagerMemoryManager.addPlayerMessage(context.villager, context.player, context.lastPlayerMessage);
            
            // Process learning from the interaction
            float interactionOutcome = calculateInteractionOutcome(context);
            VillagerLearningSystem.processPlayerInteraction(
                context.villager, context.player, "chat_" + context.detectedIntent.name(), interactionOutcome
            );
            
            // Generate response based on intent
            String response = generateResponseForIntent(context);
            
            // Add memory and emotional updates
            VillagerMemoryManager.addVillagerResponse(context.villager, context.player, response, "CHAT");
            updateEmotionalState(context, interactionOutcome);
            
            // Format response based on relationship and emotional state
            Formatting formatting = getResponseFormatting(context);
            
            return Text.literal(response).formatted(formatting);
        }
        
        private static String generateResponseForIntent(ChatContext context) {
            return switch (context.detectedIntent) {
                case GREETING -> generateGreetingResponse(context);
                case QUESTION_ABOUT_VILLAGER -> generateSelfInfoResponse(context);
                case QUESTION_ABOUT_VILLAGE -> generateVillageInfoResponse(context);
                case REQUEST_HELP -> generateHelpResponse(context);
                case TRADE_INQUIRY -> generateTradeResponse(context);
                case RELATIONSHIP_QUESTION -> generateRelationshipResponse(context);
                case GOSSIP_REQUEST -> generateGossipResponse(context);
                case COMPLIMENT -> generateComplimentResponse(context);
                case INSULT -> generateInsultResponse(context);
                case GOODBYE -> generateGoodbyeResponse(context);
                case SMALL_TALK -> generateSmallTalkResponse(context);
                case TEACHING -> generateTeachingResponse(context);
                case QUEST_INQUIRY -> generateQuestResponse(context);
                case UNKNOWN -> generateUnknownResponse(context);
            };
        }
        
        private static String generateGreetingResponse(ChatContext context) {
            String playerName = context.player.getName().getString();
            
            if (context.villagerData != null) {
                int reputation = context.villagerData.getPlayerReputation(context.player.getUuidAsString());
                String personality = context.villagerData.getPersonality();

                if (reputation > 50) {
                    if (personality == null) {
                        // Fallback to default greeting if personality is null
                        return "Hello " + playerName + "! Good to see you.";
                    }
                    return switch (personality) {
                        case "Friendly" -> "Hello " + playerName + "! It's wonderful to see you again!";
                        case "Energetic" -> "HEY " + playerName + "! Great to see you! What's up?!";
                        case "Cheerful" -> "Hi there " + playerName + "! You always brighten my day!";
                        default -> "Hello " + playerName + "! Good to see you.";
                    };
                } else if (reputation < -20) {
                    if (personality == null) {
                        // Fallback to default for negative reputation if personality is null
                        return "Hello " + playerName + ".";
                    }
                    return switch (personality) {
                        case "Grumpy" -> "Oh. It's you, " + playerName + ". What do you want?";
                        case "Shy" -> "*nervously* H-hello " + playerName + "...";
                        case "Serious" -> "Good day, " + playerName + ". I hope you're here for honest business.";
                        default -> "Hello " + playerName + ".";
                    };
                }
            }
            
            return "Hello there, " + playerName + "! How can I help you today?";
        }
        
        private static String generateSelfInfoResponse(ChatContext context) {
            if (context.villagerData == null) {
                return "I'm just a simple villager, living my life day by day.";
            }
            
            StringBuilder response = new StringBuilder();
            response.append("I'm ").append(context.villagerData.getName()).append(". ");
            
            String profession = context.villager.getVillagerData().profession().toString();
            if (!profession.equals("minecraft:none")) {
                profession = profession.replace("minecraft:", "");
                response.append("I work as a ").append(profession).append(". ");
            }
            
            // Add personality-based details
            String personality = context.villagerData != null ? context.villagerData.getPersonality() : null;
            if (personality == null) {
                response.append("I enjoy the simple pleasures of village life.");
            } else {
                switch (personality) {
                    case "Friendly" -> response.append("I love meeting new people and helping out!");
                    case "Curious" -> response.append("I'm always eager to learn new things and explore!");
                    case "Grumpy" -> response.append("I prefer to keep to myself, mostly.");
                    case "Energetic" -> response.append("I have so much energy, I can barely sit still!");
                    case "Shy" -> response.append("I'm... I'm a bit quiet, but I'm friendly once you get to know me.");
                    default -> response.append("I enjoy the simple pleasures of village life.");
                }
            }
            
            // Add emotional context
            String emotionalState = context.emotionalState.getEmotionalDescription();
            if (!emotionalState.equals("calm")) {
                response.append(" I'm feeling ").append(emotionalState).append(" today.");
            }
            
            return response.toString();
        }
        
        private static String generateVillageInfoResponse(ChatContext context) {
            List<VillagerGossipNetwork.GossipPiece> recentGossip = VillagerGossipNetwork.getGossipAbout(context.villager, "village_news");
            
            if (!recentGossip.isEmpty()) {
                VillagerGossipNetwork.GossipPiece gossip = recentGossip.get(0);
                return "Well, " + gossip.details + " Have you heard about that?";
            }
            
            // Check for recent village events
            List<VillagerEntity> nearbyVillagers = context.villager.getWorld().getEntitiesByClass(
                VillagerEntity.class, context.villager.getBoundingBox().expand(30), v -> v != context.villager);
            
            if (nearbyVillagers.size() > 5) {
                return "The village has been quite busy lately! Lots of activity around here.";
            } else if (nearbyVillagers.size() < 2) {
                return "It's been pretty quiet around here. Sometimes I feel a bit lonely.";
            }
            
            return "Life in the village goes on as usual. Nothing too exciting, but that's nice sometimes.";
        }
        
        private static String generateHelpResponse(ChatContext context) {
            String item = (String) context.extractedEntities.get("item");
            
            if (item != null) {
                return switch (item) {
                    case "diamond" -> "Diamonds are rare! You'll need to mine deep underground, below level 16. Be careful of lava!";
                    case "emerald" -> "Emeralds can be found in mountains, or you can trade with us villagers for them!";
                    case "food", "bread" -> "You can grow wheat and bake bread, or trade with our farmer. Food is essential!";
                    case "book" -> "Our librarian creates wonderful books! You can also craft them with paper and leather.";
                    default -> "I'm not sure about " + item + ", but maybe ask around the village?";
                };
            }
            
            return "I'd be happy to help! What specifically do you need assistance with?";
        }
        
        private static String generateTradeResponse(ChatContext context) {
            String item = (String) context.extractedEntities.get("item");
            String profession = context.villager.getVillagerData().profession().toString();
            
            if (profession.contains("farmer") && (item == null || item.contains("food") || item.contains("bread"))) {
                return "I have fresh crops and bread! I'd be happy to trade some emeralds for my goods.";
            } else if (profession.contains("librarian") && (item == null || item.contains("book"))) {
                return "I have many books and enchantments available! Bring emeralds and we can make a deal.";
            } else if (profession.contains("armorer") && (item == null || item.contains("armor"))) {
                return "I craft the finest armor in the village! Show me your emeralds and we'll talk.";
            }
            
            if (context.villagerData != null) {
                int reputation = context.villagerData.getPlayerReputation(context.player.getUuidAsString());
                if (reputation < -20) {
                    return "I'm... not sure I want to trade with you right now. Maybe we should work on our relationship first.";
                }
            }
            
            return "I'm open to trading! Right-click on me to see what I have available.";
        }
        
        private static String generateRelationshipResponse(ChatContext context) {
            if (context.villagerData == null) {
                return "I think we get along just fine!";
            }
            
            int reputation = context.villagerData.getPlayerReputation(context.player.getUuidAsString());
            String playerName = context.player.getName().getString();
            
            if (reputation > 80) {
                return "Oh " + playerName + ", you're one of my dearest friends! I trust you completely.";
            } else if (reputation > 40) {
                return "I really like you, " + playerName + "! You've been very kind to me.";
            } else if (reputation > 0) {
                return "I think we get along well, " + playerName + ". You seem like a good person.";
            } else if (reputation > -40) {
                return "Well... we've had our differences, " + playerName + ". But maybe we can work things out?";
            } else {
                return "To be honest, " + playerName + ", I don't trust you very much. You've not been kind to me.";
            }
        }
        
        private static String generateGossipResponse(ChatContext context) {
            List<Text> gossipDialogue = VillagerGossipNetwork.generateGossipDialogue(context.villager, context.player);
            
            if (gossipDialogue.isEmpty() || gossipDialogue.get(0).getString().contains("don't have much news")) {
                return switch (context.villagerData.getPersonality()) {
                    case "Curious" -> "I wish I had some interesting news to share, but it's been quiet lately!";
                    case "Grumpy" -> "I don't waste time with idle gossip.";
                    case "Shy" -> "I... I don't really listen to gossip much...";
                    default -> "I haven't heard anything particularly interesting lately.";
                };
            }
            
            return gossipDialogue.get(0).getString();
        }
        
        private static String generateComplimentResponse(ChatContext context) {
            ServerVillagerManager.getInstance().getAIWorldManager().getEmotionManager().processEmotionalEvent(context.villager,
                new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.HAPPINESS, 20.0f, "compliment", false));
            
            return switch (context.villagerData.getPersonality()) {
                case "Shy" -> "*blushes* Oh... th-thank you! That's very kind of you to say!";
                case "Friendly" -> "Aww, thank you so much! You're very sweet!";
                case "Energetic" -> "WOW! Thank you! You just made my whole day amazing!";
                case "Cheerful" -> "That makes me so happy to hear! Thank you!";
                case "Grumpy" -> "*grumbles* Well... I suppose that's... nice of you to say.";
                default -> "Thank you! That really means a lot to me.";
            };
        }
        
        private static String generateInsultResponse(ChatContext context) {
            ServerVillagerManager.getInstance().getAIWorldManager().getEmotionManager().processEmotionalEvent(context.villager,
                new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.ANGER, 25.0f, "insult", false));
            
            if (context.villagerData != null) {
                context.villagerData.updatePlayerRelation(context.player.getUuidAsString(), -20);
            }
            
            return switch (context.villagerData.getPersonality()) {
                case "Grumpy" -> "Well, I never! How dare you speak to me like that!";
                case "Shy" -> "*starts crying* Why would you say something so mean?";
                case "Confident" -> "Excuse me? I don't have to tolerate that kind of behavior!";
                case "Serious" -> "That was completely uncalled for. Please leave me alone.";
                default -> "That was very hurtful. I don't appreciate being spoken to that way.";
            };
        }
        
        private static String generateGoodbyeResponse(ChatContext context) {
            return switch (context.villagerData.getPersonality()) {
                case "Friendly" -> "Goodbye! It was lovely talking with you! Come back anytime!";
                case "Energetic" -> "See you later! Take care and have an AMAZING day!";
                case "Shy" -> "B-bye... it was nice talking with you...";
                case "Grumpy" -> "Hmph. Goodbye then.";
                default -> "Farewell! Safe travels!";
            };
        }
        
        private static String generateSmallTalkResponse(ChatContext context) {
            String time = (String) context.extractedEntities.get("time");
            
            if (time != null && time.contains("weather")) {
                return "The weather has been quite nice lately! Perfect for working in the fields.";
            }
            
            return "Life has been good! Just taking things one day at a time. How about you?";
        }
        
        private static String generateTeachingResponse(ChatContext context) {
            String item = (String) context.extractedEntities.get("item");
            String profession = context.villager.getVillagerData().profession().toString();
            
            if (profession.contains("farmer")) {
                return "I can teach you about farming! Plant seeds, water them, and harvest when they're ready. The key is patience!";
            } else if (profession.contains("librarian")) {
                return "Books contain so much knowledge! Reading helps you learn about the world. And enchantments can make your tools magical!";
            }
            
            return "I'm not sure I'm the best teacher for that, but I'd suggest asking around the village. Someone here might know!";
        }
        
        private static String generateQuestResponse(ChatContext context) {
            // This would integrate with a quest system
            return "I don't have any specific tasks right now, but if you're looking to help the village, maybe talk to others and see what they need!";
        }
        
        private static String generateUnknownResponse(ChatContext context) {
            // Try to use LLM for unknown intents if available
            if (LLMDialogueManager.isConfigured()) {
                VillagerDialogueSystem.DialogueContext dialogueContext =
                    new VillagerDialogueSystem.DialogueContext(context.villager, context.player);

                // Asynchronously request an LLM response and deliver it to the player when ready
                VillagerDialogueSystem.generateDialogue(dialogueContext, VillagerDialogueSystem.DialogueCategory.GREETING, llmResponse -> {
                    if (llmResponse != null && context.player != null) {
                        context.player.sendMessage(Text.of(llmResponse), false);
                    }
                });
            }

             // Return personality-based fallback immediately
             String personality = context.villagerData != null ? context.villagerData.getPersonality() : null;
             if (personality == null) {
                 return "I'm not quite sure what you mean, but I'm listening!";
             }
             return switch (personality) {
                 case "Curious" -> "That's interesting! I'm not sure I understand completely, but tell me more!";
                 case "Shy" -> "Um... I'm not sure what you mean...";
                 case "Grumpy" -> "I don't understand what you're getting at.";
                 default -> "I'm not quite sure what you mean, but I'm listening!";
             };
        }
        
        private static float calculateInteractionOutcome(ChatContext context) {
            return switch (context.detectedIntent) {
                case COMPLIMENT -> 0.8f;
                case GREETING, GOODBYE, SMALL_TALK -> 0.3f;
                case QUESTION_ABOUT_VILLAGER, QUESTION_ABOUT_VILLAGE -> 0.2f;
                case REQUEST_HELP, TEACHING -> 0.4f;
                case TRADE_INQUIRY -> 0.1f;
                case INSULT -> -0.9f;
                case GOSSIP_REQUEST -> 0.1f; // Depends on personality
                default -> 0.0f;
            };
        }
        
        private static void updateEmotionalState(ChatContext context, float outcome) {
            if (outcome > 0.5f) {
                VillagerEmotionSystem.processEmotionalEvent(context.villager,
                    new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.HAPPINESS, outcome * 10.0f, "positive_chat", false));
            } else if (outcome < -0.5f) {
                VillagerEmotionSystem.processEmotionalEvent(context.villager,
                    new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.ANGER, Math.abs(outcome) * 15.0f, "negative_chat", false));
            }
            
            // Always get a small social interaction boost
            VillagerEmotionSystem.processEmotionalEvent(context.villager,
                new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.LONELINESS, -5.0f, "social_interaction", false));
        }
        
        private static Formatting getResponseFormatting(ChatContext context) {
            if (context.villagerData == null) return Formatting.WHITE;
            
            int reputation = context.villagerData.getPlayerReputation(context.player.getUuidAsString());
            
            if (reputation > 50) return Formatting.GREEN;
            if (reputation < -20) return Formatting.RED;
            if (context.detectedIntent == ChatIntent.COMPLIMENT) return Formatting.AQUA;
            if (context.detectedIntent == ChatIntent.INSULT) return Formatting.DARK_RED;
            
            return Formatting.WHITE;
        }
    }
    
    // Combines trimming, null/empty, and length validation in one step
    private static String validateMessage(String message) {
        if (message == null) return null;
        String trimmed = message.trim();
        if (trimmed.isEmpty() || trimmed.length() > 500) return null;
        return trimmed;
    }

    // Main chat processing method
    public static Text processPlayerMessage(VillagerEntity villager, PlayerEntity player, String message) {
        String validMessage = validateMessage(message);
        if (validMessage == null) {
            return Text.literal("...").formatted(Formatting.GRAY);
        }

        String sanitized = sanitizeMessage(validMessage);
        ChatContext context = new ChatContext(villager, player, sanitized);
        return ResponseGenerator.generateResponse(context);
    }
    
    // Enhanced dialogue integration
    public static Text generateEnhancedDialogue(VillagerEntity villager, PlayerEntity player, VillagerDialogueSystem.DialogueCategory category) {
        // Check if player has recent chat history
        String recentContext = VillagerMemoryManager.getRecentConversationContext(villager, player, 3);
        
        if (!recentContext.isEmpty()) {
            // Use chat context to inform dialogue generation
            ChatContext chatContext = new ChatContext(villager, player, "continue conversation");
            
            // Generate contextual response that references recent chat
            String contextualResponse = generateContextualResponse(chatContext, category, recentContext);
            
            if (contextualResponse != null) {
                return Text.literal(contextualResponse).formatted(ResponseGenerator.getResponseFormatting(chatContext));
            }
        }
        
        // Fall back to standard dialogue system
        return VillagerDialogueSystem.generateDialogue(
            new VillagerDialogueSystem.DialogueContext(villager, player), category, t -> {});
    }
    
    private static String generateContextualResponse(ChatContext context, VillagerDialogueSystem.DialogueCategory category, String recentContext) {
        // Analyze recent conversation for continuity
        if (recentContext.toLowerCase().contains("trade")) {
            return "Speaking of trading, " + generateTradeFollowUp(context);
        } else if (recentContext.toLowerCase().contains("gossip") || recentContext.toLowerCase().contains("news")) {
            return "About that news I mentioned, " + generateGossipFollowUp(context);
        } else if (recentContext.toLowerCase().contains("help")) {
            return "Since you were asking for help, " + generateHelpFollowUp(context);
        }
        
        return null; // No contextual response available
    }
    
    private static String generateTradeFollowUp(ChatContext context) {
        return "I always try to offer fair prices. Good business relationships are important!";
    }
    
    private static String generateGossipFollowUp(ChatContext context) {
        return "village life is full of interesting stories if you know where to listen!";
    }
    
    private static String generateHelpFollowUp(ChatContext context) {
        return "I hope I was able to point you in the right direction!";
    }
    
    // Utility methods for command integration
    public static boolean isValidChatCommand(String message) {
        return validateMessage(message) != null;
    }
    
    public static String sanitizeMessage(String message) {
        if (message == null) return "";
        
        // Remove potentially problematic characters
        return message.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                     .replaceAll("\\s+", " ")
                     .trim();
    }
}