package com.beeny.system.personality;

import com.beeny.constants.VillagerConstants.PersonalityType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory for creating personality strategy instances.
 * Manages the mapping between personality types and their strategy implementations.
 */
public class PersonalityStrategyFactory {
    
    private static final Map<PersonalityType, PersonalityStrategy> STRATEGIES = new EnumMap<>(PersonalityType.class);
    
    static {
        // Register all personality strategies
        STRATEGIES.put(PersonalityType.FRIENDLY, new FriendlyPersonality());
        STRATEGIES.put(PersonalityType.GRUMPY, new GrumpyPersonality());
        STRATEGIES.put(PersonalityType.SHY, new ShyPersonality());
        STRATEGIES.put(PersonalityType.CHEERFUL, new CheerfulPersonality());
        STRATEGIES.put(PersonalityType.MELANCHOLY, new MelancholyPersonality());
        STRATEGIES.put(PersonalityType.ENERGETIC, new EnergeticPersonality());
        STRATEGIES.put(PersonalityType.CALM, new CalmPersonality());
        STRATEGIES.put(PersonalityType.CONFIDENT, new ConfidentPersonality());
        STRATEGIES.put(PersonalityType.ANXIOUS, new AnxiousPersonality());
        STRATEGIES.put(PersonalityType.NERVOUS, new AnxiousPersonality()); // Same as anxious
        STRATEGIES.put(PersonalityType.CURIOUS, new CuriousPersonality());
        STRATEGIES.put(PersonalityType.LAZY, new LazyPersonality());
        STRATEGIES.put(PersonalityType.SERIOUS, new SeriousPersonality());
    }
    
    /**
     * Get the personality strategy for a given personality type
     * @param personalityType The personality type
     * @return The corresponding strategy, or null if not found
     */
    public static PersonalityStrategy getStrategy(PersonalityType personalityType) {
        return STRATEGIES.get(personalityType);
    }
    
    /**
     * Check if a strategy exists for the given personality type
     * @param personalityType The personality type to check
     * @return true if a strategy exists
     */
    public static boolean hasStrategy(PersonalityType personalityType) {
        return STRATEGIES.containsKey(personalityType);
    }
    
    /**
     * Get all registered personality types
     * @return Set of all personality types with strategies
     */
    public static java.util.Set<PersonalityType> getRegisteredPersonalities() {
        return STRATEGIES.keySet();
    }
}