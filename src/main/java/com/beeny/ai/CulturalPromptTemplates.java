package com.beeny.ai;

import java.util.HashMap;
import java.util.Map;

public class CulturalPromptTemplates {
    private static final Map<String, String> TEMPLATES = new HashMap<>();

    static {
        TEMPLATES.put("dynamic_event", """
            Create a culturally authentic %s event in Minecraft.
            Current village context:
            - Time period: %s
            - Recent events: %s
            - Participating villagers: %s
            - Weather conditions: %s
            - Local resources: %s
            Consider:
            - Cultural authenticity and historical accuracy
            - Environmental and seasonal influences
            - Social dynamics and relationships
            - Available resources and infrastructure
            - Impact on village development
            Format your response as:
            TYPE: (specific type of event)
            THEME: (cultural significance)
            LOCATION: (specific area in village)
            DURATION: (in Minecraft ticks)
            PARTICIPANTS: (roles and numbers)
            ACTIVITIES: (sequence of events)
            REQUIREMENTS: (needed resources/preparations)
            OUTCOMES: (social/cultural/economic impacts)
            """);

        TEMPLATES.put("village_layout", """
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

        TEMPLATES.put("building_style", """
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

        TEMPLATES.put("social_interaction", """
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

        TEMPLATES.put("cultural_event", """
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

        TEMPLATES.put("daily_schedule", """
            Create a dynamic daily schedule for a %s %s in a %s village.
            Current context:
            - Time of year: %s
            - Weather: %s
            - Active village events: %s
            - Personal relationships: %s
            - Recent activities: %s
            - Mood: %s
            Consider:
            - Professional duties and skill level
            - Cultural customs and traditions
            - Current village events and celebrations
            - Personal relationships and social networks
            - Environmental conditions and adaptations
            - Individual preferences and characteristics
            - Recent experiences and learned behaviors
            Format response as time blocks (0-24000 ticks):
            TIME: activity, location, duration, priority_level (1-5), social_context
            Include 6-8 distinct activities that reflect current conditions
            """);

        TEMPLATES.put("villager_interaction", """
            Generate an organic interaction between villagers in current context:
            - Villager 1: %s (%s, mood: %s)
            - Villager 2: %s (%s, mood: %s)
            - Location: %s
            - Time: %s
            - Recent shared experiences: %s
            - Cultural background: %s
            - Weather: %s
            - Nearby activities: %s
            Consider:
            - Individual personalities and current moods
            - Existing relationship dynamics
            - Cultural and social context
            - Environmental influences
            - Recent village events
            - Personal histories and shared experiences
            Format response as:
            GREETING: (culturally appropriate opening)
            TOPIC: (main discussion point)
            ACTIONS: (physical gestures/movements)
            DIALOGUE: (natural conversation)
            OUTCOME: (relationship impact)
            MOOD_CHANGE: (emotional effects)
            """);

        TEMPLATES.put("cultural_adaptation", """
            Design a cultural adaptation for the village based on:
            - Base culture: %s
            - Environmental changes: %s
            - Recent challenges: %s
            - Available resources: %s
            - Population needs: %s
            - External influences: %s
            Consider:
            - Practical necessity and survival needs
            - Cultural preservation vs adaptation
            - Social cohesion and community impact
            - Resource sustainability
            - Long-term implications
            - Implementation feasibility
            Format response as:
            TYPE: (type of adaptation)
            MOTIVATION: (driving factors)
            CHANGES: (specific modifications)
            IMPLEMENTATION: (step-by-step process)
            RESOURCES: (required materials)
            TIMEFRAME: (adaptation period)
            IMPACT: (expected effects)
            """);
    }

    public static String getTemplate(String key, Object... args) {
        String template = TEMPLATES.getOrDefault(key, "");
        return args.length > 0 ? String.format(template, args) : template;
    }

    public static Map<String, String> getAllTemplates() {
        return new HashMap<>(TEMPLATES);
    }

    public static void addTemplate(String key, String template) {
        TEMPLATES.put(key, template);
    }
}
