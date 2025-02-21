package com.beeny.ai;

import java.util.HashMap;
import java.util.Map;

public class CulturalPromptTemplates {
    private static final Map<String, String> TEMPLATES = new HashMap<>();

    static {
        TEMPLATES.put("village_layout", 
            """
            Design a culturally authentic %s village layout in Minecraft.
            Consider:
            - Historical accuracy and cultural significance
            - Social hierarchy and community organization
            - Resource distribution and daily life patterns
            - Climate and geographical adaptations
            - Religious or ceremonial spaces
            
            Format your response as:
            LAYOUT_STYLE: (organic/grid/radial/hierarchical)
            CENTRAL_FEATURE: (main cultural building or space)
            DISTRICTS: (comma-separated list of distinct areas)
            PATHWAYS: (path style and materials)
            GATHERING_SPACES: (list of community areas)
            SPECIAL_FEATURES: (unique cultural elements)
            """);

        TEMPLATES.put("building_style",
            """
            Design a %s-style %s for a Minecraft village.
            Consider authentic:
            - Architectural elements
            - Building materials
            - Cultural symbolism
            - Practical functions
            - Community integration
            
            Format your response as:
            DIMENSIONS: width,length,height
            MATERIALS: (comma-separated list of Minecraft blocks)
            FEATURES: (key architectural elements)
            DECORATIONS: (cultural details)
            SURROUNDINGS: (landscaping elements)
            PURPOSE: (social/cultural function)
            """);

        TEMPLATES.put("social_interaction",
            """
            Create a social interaction between two villagers in a %s culture.
            Context:
            - First villager: %s (%s)
            - Second villager: %s (%s)
            - Time of day: %s
            - Current activity: %s
            - Recent events: %s
            
            Format your response as:
            RELATIONSHIP: (type of social bond formed)
            DIALOGUE: (brief exchange, culturally appropriate)
            ACTIONS: (physical interactions or gestures)
            OUTCOME: (how this affects their relationship)
            """);

        TEMPLATES.put("cultural_event",
            """
            Design a cultural event for a %s village in Minecraft.
            Consider:
            - Cultural significance and traditions
            - Community participation
            - Required structures or spaces
            - Time and duration
            - Impact on village life
            
            Format your response as:
            NAME: (event name)
            DURATION: (in Minecraft days)
            LOCATION: (where in village)
            ACTIVITIES: (list of villager actions)
            DECORATIONS: (temporary additions)
            EFFECTS: (impact on village mood/behavior)
            """);

        TEMPLATES.put("daily_schedule",
            """
            Create a daily schedule for a %s %s in a %s village.
            Consider:
            - Professional duties
            - Cultural customs
            - Social obligations
            - Personal characteristics
            - Village dynamics
            
            Format response as time blocks (0-24000 ticks):
            TIME: activity, location, duration
            Include 4-6 distinct activities
            """);
    }

    public static String getTemplate(String templateKey, Object... args) {
        String template = TEMPLATES.getOrDefault(templateKey, "");
        return args.length > 0 ? String.format(template, args) : template;
    }

    public static Map<String, String> getAllTemplates() {
        return new HashMap<>(TEMPLATES);
    }

    public static void addTemplate(String key, String template) {
        TEMPLATES.put(key, template);
    }
}