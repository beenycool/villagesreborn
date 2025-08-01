package com.beeny.dialogue;

import com.beeny.system.VillagerDialogueSystem;
import com.beeny.data.VillagerData;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.world.World;
import java.util.Set;

public class DialoguePromptBuilder {
    
    public static String buildContextualPrompt(VillagerDialogueSystem.DialogueContext context, 
                                             VillagerDialogueSystem.DialogueCategory category,
                                             String conversationHistory) {
        StringBuilder prompt = new StringBuilder();
        
        // Basic character setup
        prompt.append(buildCharacterIntroduction(context));
        prompt.append(buildEnvironmentalContext(context));
        prompt.append(buildRelationshipContext(context));
        prompt.append(buildMemoryContext(context, conversationHistory));
        prompt.append(buildCategorySpecificInstructions(category, context));
        prompt.append(buildResponseGuidelines());
        
        return prompt.toString();
    }
    
    private static String buildCharacterIntroduction(VillagerDialogueSystem.DialogueContext context) {
        VillagerData data = context.villagerData;
        StringBuilder intro = new StringBuilder();
        
        intro.append("You are ").append(data.getName()).append(", ");
        
        // Age and life stage
        if (data.getAge() < 20) {
            intro.append("a young villager child");
        } else if (data.getAge() < 100) {
            intro.append("a young adult villager");
        } else if (data.getAge() < 300) {
            intro.append("an adult villager");
        } else {
            intro.append("an elderly villager");
        }
        
        intro.append(" living in a Minecraft village. ");
        
        // Personality
        intro.append("Your personality is ").append(data.getPersonality().toLowerCase()).append(". ");
        
        // Profession
        String profession = context.villager.getVillagerData().profession().toString().replace("minecraft:", "");
        intro.append("You work as a ").append(profession).append(". ");
        
        // Mood based on happiness
        intro.append("Currently, you are feeling ").append(data.getHappinessDescription().toLowerCase()).append(". ");
        
        return intro.toString();
    }
    
    private static String buildEnvironmentalContext(VillagerDialogueSystem.DialogueContext context) {
        StringBuilder env = new StringBuilder();
        
        // Time of day
        env.append("It is ").append(context.timeOfDay.name.toLowerCase()).append(" ");
        
        // Weather
        env.append("and the weather is ").append(context.weather).append(". ");
        
        // Biome
        String biome = context.biome.replace("_", " ");
        env.append("You live in a ").append(biome).append(" biome. ");
        
        return env.toString();
    }
    
    private static String buildRelationshipContext(VillagerDialogueSystem.DialogueContext context) {
        StringBuilder rel = new StringBuilder();
        VillagerData data = context.villagerData;
        String playerName = context.player.getName().getString();
        
        // Player relationship
        if (context.playerReputation > 50) {
            rel.append("You consider ").append(playerName).append(" a close friend. ");
        } else if (context.playerReputation > 20) {
            rel.append("You know ").append(playerName).append(" well and like them. ");
        } else if (context.playerReputation > 0) {
            rel.append("You have a neutral opinion of ").append(playerName).append(". ");
        } else if (context.playerReputation > -20) {
            rel.append("You are somewhat wary of ").append(playerName).append(". ");
        } else {
            rel.append("You don't trust ").append(playerName).append(" much. ");
        }
        
        // Family context
        if (!data.getSpouseName().isEmpty()) {
            rel.append("You are married to ").append(data.getSpouseName()).append(". ");
        }
        
        if (!data.getChildrenNames().isEmpty()) {
            if (data.getChildrenNames().size() == 1) {
                rel.append("You have a child named ").append(data.getChildrenNames().get(0)).append(". ");
            } else {
                rel.append("You have children: ").append(String.join(", ", data.getChildrenNames())).append(". ");
            }
        }
        
        return rel.toString();
    }
    
    private static String buildMemoryContext(VillagerDialogueSystem.DialogueContext context, String conversationHistory) {
        StringBuilder memory = new StringBuilder();
        VillagerData data = context.villagerData;
        String playerUuid = context.player.getUuidAsString();
        
        // Player-specific memories
        String playerMemory = data.getPlayerMemory(playerUuid);
        if (!playerMemory.isEmpty()) {
            memory.append("You remember: ").append(playerMemory).append(" ");
        }
        
        // Recent conversation history
        if (conversationHistory != null && !conversationHistory.trim().isEmpty()) {
            memory.append("Recent conversation: ").append(conversationHistory).append(" ");
        }
        
        // Recent events
        if (!data.getRecentEvents().isEmpty()) {
            memory.append("Recent events in your life: ");
            memory.append(String.join(", ", data.getRecentEvents().subList(0, Math.min(3, data.getRecentEvents().size()))));
            memory.append(". ");
        }
        
        // Hobby
        memory.append("Your hobby is ").append(data.getHobby().toLowerCase()).append(". ");
        
        return memory.toString();
    }
    
    private static String buildCategorySpecificInstructions(VillagerDialogueSystem.DialogueCategory category, 
                                                          VillagerDialogueSystem.DialogueContext context) {
        StringBuilder instructions = new StringBuilder();
        
        switch (category) {
            case GREETING -> {
                instructions.append("Give a greeting that matches your personality and relationship with this player. ");
                if (context.timeOfDay.name.equals("MORNING")) {
                    instructions.append("Consider mentioning it's morning. ");
                } else if (context.timeOfDay.name.equals("NIGHT")) {
                    instructions.append("Consider it's late - perhaps suggest it's getting late. ");
                }
            }
            
            case WEATHER -> {
                instructions.append("Comment on the current ").append(context.weather).append(" weather. ");
                instructions.append("Relate it to your work, mood, or recent activities. ");
            }
            
            case WORK -> {
                String profession = context.villager.getVillagerData().profession().toString().replace("minecraft:", "");
                instructions.append("Talk about your work as a ").append(profession).append(". ");
                instructions.append("Share something specific about your profession or recent work activities. ");
            }
            
            case FAMILY -> {
                if (!context.villagerData.getSpouseName().isEmpty() || !context.villagerData.getChildrenNames().isEmpty()) {
                    instructions.append("Talk about your family with warmth and care. ");
                } else {
                    instructions.append("Mention something about family life in the village or your thoughts on family. ");
                }
            }
            
            case GOSSIP -> {
                instructions.append("Share some light village gossip or news. ");
                instructions.append("Keep it friendly and not harmful. ");
                instructions.append("Maybe mention other villagers or village happenings. ");
            }
            
            case HOBBY -> {
                instructions.append("Talk enthusiastically about your hobby: ").append(context.villagerData.getHobby().toLowerCase()).append(". ");
                instructions.append("Share what you enjoy about it or something you've done recently. ");
            }
            
            case ADVICE -> {
                instructions.append("Give helpful advice based on your life experience and personality. ");
                instructions.append("Draw from your profession or personal experiences. ");
            }
            
            case STORY -> {
                instructions.append("Tell a brief, interesting story from your past or something you've witnessed in the village. ");
                instructions.append("Make it engaging and fitting to your personality. ");
            }
            
            case MOOD -> {
                instructions.append("Express how you're feeling today and why. ");
                instructions.append("Relate it to recent events, the weather, or your general life situation. ");
            }
            
            case TRADE -> {
                instructions.append("Mention something about trading or commerce. ");
                instructions.append("Perhaps talk about items you need or have been working with. ");
            }
            
            case FAREWELL -> {
                instructions.append("Say goodbye in a way that fits your personality. ");
                instructions.append("Maybe reference when you might see them again or something to remember. ");
            }
        }
        
        return instructions.toString();
    }
    
    private static String buildResponseGuidelines() {
        StringBuilder guidelines = new StringBuilder();
        
        guidelines.append("\nResponse guidelines:\n");
        guidelines.append("- Stay completely in character as this villager\n");
        guidelines.append("- Keep responses under 50 words\n");
        guidelines.append("- Be conversational and natural\n");
        guidelines.append("- Don't use asterisks or action descriptions\n");
        guidelines.append("- Don't break the fourth wall or mention you're an AI\n");
        guidelines.append("- Match the personality traits in your speech patterns\n");
        guidelines.append("- Use simple, villager-appropriate language\n");
        guidelines.append("- Only respond with dialogue text, nothing else\n");
        
        return guidelines.toString();
    }
    
    public static String buildPersonalityPrompt(String personality) {
        return switch (personality.toLowerCase()) {
            case "friendly" -> "Speak warmly and openly, use welcoming language, show genuine interest in others. ";
            case "grumpy" -> "Be somewhat irritable, use short responses, occasionally complain about things. ";
            case "shy" -> "Speak softly and hesitantly, use fewer words, seem a bit nervous or uncertain. ";
            case "energetic" -> "Use exclamation points, speak enthusiastically, show excitement about topics. ";
            case "lazy" -> "Speak slowly and casually, mention being tired or wanting to rest. ";
            case "curious" -> "Ask questions, show interest in learning new things, mention discoveries. ";
            case "serious" -> "Use formal language, focus on important matters, avoid jokes or casual topics. ";
            case "cheerful" -> "Be optimistic and upbeat, find positive aspects in situations. ";
            case "nervous" -> "Speak uncertainly, worry about things, use cautious language. ";
            case "confident" -> "Speak assertively, show self-assurance, give advice readily. ";
            default -> "Speak naturally according to your character. ";
        };
    }
    
    public static String getAvoidOverusedTopics(VillagerData data, VillagerDialogueSystem.DialogueCategory category) {
        int topicCount = data.getTopicFrequency(category.name());
        if (topicCount > 3) {
            return "You've talked about " + category.name().toLowerCase() + " quite a bit recently, so try to approach it from a fresh angle or mention something new. ";
        }
        return "";
    }
}