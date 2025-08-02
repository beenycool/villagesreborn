package com.beeny.ai.decision;

import com.beeny.data.VillagerData;
import com.beeny.ai.core.VillagerEmotionSystem;
import com.beeny.ai.social.VillagerGossipNetwork;
import com.beeny.system.VillagerRelationshipManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.InputStream;

/**
 * Utility AI system for complex decision making
 * Evaluates multiple factors to make intelligent choices about relationships, career, and life decisions
 */
public class VillagerUtilityAI {
    // Personality compatibility config
    private static Map<String, Set<String>> highCompatibility = new HashMap<>();
    private static Set<List<String>> lowCompatibility = new HashSet<>();

    static {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = VillagerUtilityAI.class.getClassLoader()
                .getResourceAsStream("personality_compatibility.json");
            if (is != null) {
                Map<String, Object> config = mapper.readValue(is, new TypeReference<Map<String, Object>>() {});
                // Parse high compatibility
                Map<String, List<String>> high = (Map<String, List<String>>) config.get("high");
                for (Map.Entry<String, List<String>> entry : high.entrySet()) {
                    highCompatibility.put(entry.getKey(), new HashSet<>(entry.getValue()));
                }
                // Parse low compatibility
                List<List<String>> low = (List<List<String>>) config.get("low");
                for (List<String> pair : low) {
                    // Store both orders for symmetry
                    if (pair.size() == 2) {
                        lowCompatibility.add(pair);
                        lowCompatibility.add(Arrays.asList(pair.get(1), pair.get(0)));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load personality compatibility config: " + e.getMessage());
        }
    }
    
    // Utility curve types for different considerations
    public enum CurveType {
        LINEAR,
        QUADRATIC,
        INVERSE_QUADRATIC,
        SIGMOID,
        BOOLEAN_THRESHOLD
    }
    
    // Base consideration class
    public abstract static class Consideration {
        public final String name;
        public final float weight;
        public final CurveType curveType;
        
        public Consideration(String name, float weight, CurveType curveType) {
            this.name = name;
            this.weight = weight;
            this.curveType = curveType;
        }
        
        public abstract float getValue(VillagerEntity villager, Object context);
        
        public float getUtility(VillagerEntity villager, Object context) {
            float value = Math.max(0.0f, Math.min(1.0f, getValue(villager, context)));
            return applyCurve(value) * weight;
        }
        
        private float applyCurve(float value) {
            return switch (curveType) {
                case LINEAR -> value;
                case QUADRATIC -> value * value;
                case INVERSE_QUADRATIC -> 1.0f - ((1.0f - value) * (1.0f - value));
                case SIGMOID -> 1.0f / (1.0f + (float) Math.exp(-10.0f * (value - 0.5f)));
                case BOOLEAN_THRESHOLD -> value > 0.5f ? 1.0f : 0.0f;
            };
        }
    }
    
    // Decision class that combines multiple considerations
    public static class Decision {
        public final String name;
        public final List<Consideration> considerations;
        private float cachedUtility = -1.0f;
        private long lastCalculation = 0;
        
        public Decision(String name) {
            this.name = name;
            this.considerations = new ArrayList<>();
        }
        
        public Decision addConsideration(Consideration consideration) {
            considerations.add(consideration);
            return this;
        }
        
        public float calculateUtility(VillagerEntity villager, Object context) {
            long currentTime = System.currentTimeMillis();
            synchronized (this) {
                if (currentTime - lastCalculation > 1000) { // Cache for 1 second
                    float sum = 0.0f;
                    int count = considerations.size();
                    for (Consideration consideration : considerations) {
                        sum += consideration.getUtility(villager, context);
                    }
                    float utility = count > 0 ? sum / count : 0.0f;
                    cachedUtility = utility;
                    lastCalculation = currentTime;
                }
                return cachedUtility;
            }
        }
    }
    
    // Specific considerations for marriage decisions
    public static class PersonalityCompatibilityConsideration extends Consideration {
        public PersonalityCompatibilityConsideration() {
            super("personality_compatibility", 1.2f, CurveType.SIGMOID);
        }
        
        @Override
        public float getValue(VillagerEntity villager, Object context) {
            if (!(context instanceof VillagerEntity partner)) return 0.0f;
            
            VillagerData villagerData = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            VillagerData partnerData = partner.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            
            if (villagerData == null || partnerData == null) return 0.0f;
            
            String personality1 = villagerData.getPersonality();
            String personality2 = partnerData.getPersonality();
            
            // High compatibility
            if (highCompatibility.getOrDefault(personality1, Collections.emptySet()).contains(personality2)) {
                return 0.9f;
            }

            // Medium compatibility - same personality
            if (personality1.equals(personality2)) {
                return 0.6f;
            }

            // Low compatibility
            List<String> pair = Arrays.asList(personality1, personality2);
            if (lowCompatibility.contains(pair)) {
                return 0.2f;
            }

            return 0.4f; // Neutral compatibility
        }
    }
    
    public static class EmotionalStateConsideration extends Consideration {
        public EmotionalStateConsideration() {
            super("emotional_state", 1.0f, CurveType.LINEAR);
        }
        
        @Override
        public float getValue(VillagerEntity villager, Object context) {
            VillagerEmotionSystem.EmotionalState emotions = VillagerEmotionSystem.getEmotionalState(villager);
            
            float love = emotions.getEmotion(VillagerEmotionSystem.EmotionType.LOVE) / 100.0f;
            float happiness = emotions.getEmotion(VillagerEmotionSystem.EmotionType.HAPPINESS) / 100.0f;
            float loneliness = emotions.getEmotion(VillagerEmotionSystem.EmotionType.LONELINESS) / 50.0f; // Normalize to 0-2 range
            
            return Math.max(0.0f, Math.min(1.0f, (love + happiness + loneliness) / 3.0f));
        }
    }
    
    public static class AgeCompatibilityConsideration extends Consideration {
        public AgeCompatibilityConsideration() {
            super("age_compatibility", 0.8f, CurveType.INVERSE_QUADRATIC);
        }
        
        @Override
        public float getValue(VillagerEntity villager, Object context) {
            if (!(context instanceof VillagerEntity partner)) return 0.0f;
            
            VillagerData villagerData = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            VillagerData partnerData = partner.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            
            if (villagerData == null || partnerData == null) return 0.0f;
            
            int ageDiff = Math.abs(villagerData.getAge() - partnerData.getAge());
            
            if (ageDiff <= 20) return 1.0f;
            if (ageDiff <= 50) return 0.8f;
            if (ageDiff <= 100) return 0.5f;
            if (ageDiff <= 150) return 0.2f;
            return 0.0f;
        }
    }
    
    public static class ReputationConsideration extends Consideration {
        public ReputationConsideration() {
            super("reputation", 0.6f, CurveType.SIGMOID);
        }
        
        @Override
        public float getValue(VillagerEntity villager, Object context) {
            if (!(context instanceof VillagerEntity partner)) return 0.0f;
            
            VillagerData villagerData = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            
            if (villagerData == null) return 0.0f;
            
            int partnerReputation = villagerData.getPlayerReputation(partner.getUuidAsString());
            
            // Convert reputation (-100 to 100) to utility (0 to 1)
            return Math.max(0.0f, Math.min(1.0f, (partnerReputation + 100) / 200.0f));
        }
    }
    
    // Career change considerations
    public static class CareerSatisfactionConsideration extends Consideration {
        public CareerSatisfactionConsideration() {
            super("career_satisfaction", 1.5f, CurveType.LINEAR);
        }
        
        @Override
        public float getValue(VillagerEntity villager, Object context) {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            VillagerEmotionSystem.EmotionalState emotions = VillagerEmotionSystem.getEmotionalState(villager);
            
            if (data == null) return 0.5f;
            
            float boredom = emotions.getEmotion(VillagerEmotionSystem.EmotionType.BOREDOM);
            float contentment = emotions.getEmotion(VillagerEmotionSystem.EmotionType.CONTENTMENT);
            int professionCount = data.getProfessionHistory().size();
            
            // More professions = more experience but also more boredom potential
            float experienceFactor = Math.min(1.0f, professionCount / 3.0f);
            float satisfactionScore = (contentment - boredom + 50.0f) / 150.0f; // Normalize
            
            return Math.max(0.0f, Math.min(1.0f, satisfactionScore * (1.0f + experienceFactor * 0.2f)));
        }
    }
    
    public static class VillageNeedsConsideration extends Consideration {
        public VillageNeedsConsideration() {
            super("village_needs", 1.0f, CurveType.QUADRATIC);
        }
        
        @Override
        public float getValue(VillagerEntity villager, Object context) {
            if (!(context instanceof String profession)) return 0.0f;
            
            // Count villagers with this profession in the area
            List<VillagerEntity> nearbyVillagers = villager.getWorld().getEntitiesByClass(
                VillagerEntity.class,
                villager.getBoundingBox().expand(50.0),
                v -> v != villager
            );
            
            long professionCount = nearbyVillagers.stream()
                .filter(v -> v.getVillagerData().profession().toString().contains(profession.toLowerCase()))
                .count();
            
            // Utility decreases as more villagers have the same profession
            if (professionCount == 0) return 1.0f;
            if (professionCount == 1) return 0.8f;
            if (professionCount == 2) return 0.5f;
            if (professionCount == 3) return 0.2f;
            return 0.1f;
        }
    }
    
    // Decision makers
    public static class MarriageDecisionMaker {
        private final Decision marriageDecision;
        
        public MarriageDecisionMaker() {
            marriageDecision = new Decision("marriage")
                .addConsideration(new PersonalityCompatibilityConsideration())
                .addConsideration(new EmotionalStateConsideration())
                .addConsideration(new AgeCompatibilityConsideration())
                .addConsideration(new ReputationConsideration());
        }
        
        public boolean shouldPropose(VillagerEntity villager, VillagerEntity potential) {
            if (!VillagerRelationshipManager.canMarry(villager, potential)) {
                return false;
            }
            
            float utility = marriageDecision.calculateUtility(villager, potential);
            float threshold = getMarriageThreshold(villager);
            
            return utility > threshold && ThreadLocalRandom.current().nextFloat() < (utility * 0.1f);
        }
        
        private float getMarriageThreshold(VillagerEntity villager) {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            if (data == null) return 0.7f;
            
            // Threshold varies by personality
            return switch (data.getPersonality()) {
                case "Shy", "Nervous" -> 0.8f; // Higher threshold - more cautious
                case "Confident", "Energetic" -> 0.5f; // Lower threshold - more willing
                case "Serious" -> 0.9f; // Very high threshold - very selective
                default -> 0.7f;
            };
        }
        
        public List<VillagerEntity> rankPotentialPartners(VillagerEntity villager, List<VillagerEntity> candidates) {
            return candidates.stream()
                .filter(candidate -> VillagerRelationshipManager.canMarry(villager, candidate))
                .sorted((a, b) -> Float.compare(
                    marriageDecision.calculateUtility(villager, b),
                    marriageDecision.calculateUtility(villager, a)
                ))
                .toList();
        }
    }
    
    public static class CareerDecisionMaker {
        private final Decision careerChangeDecision;
        
        public CareerDecisionMaker() {
            careerChangeDecision = new Decision("career_change")
                .addConsideration(new CareerSatisfactionConsideration())
                .addConsideration(new VillageNeedsConsideration());
        }
        
        public boolean shouldChangeCareer(VillagerEntity villager) {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            if (data == null) return false;
            
            // Don't change career too frequently
            if (data.getProfessionHistory().size() > 3) return false;
            
            VillagerEmotionSystem.EmotionalState emotions = VillagerEmotionSystem.getEmotionalState(villager);
            float boredom = emotions.getEmotion(VillagerEmotionSystem.EmotionType.BOREDOM);
            float contentment = emotions.getEmotion(VillagerEmotionSystem.EmotionType.CONTENTMENT);
            
            // High boredom and low contentment suggest career change
            float dissatisfaction = (boredom - contentment + 100.0f) / 200.0f;
            
            return dissatisfaction > 0.7f && ThreadLocalRandom.current().nextFloat() < 0.05f; // 5% chance when dissatisfied
        }
        
        public String chooseBestCareer(VillagerEntity villager, List<String> availableCareers) {
            return availableCareers.stream()
                .max(Comparator.comparing(career -> careerChangeDecision.calculateUtility(villager, career)))
                .orElse(availableCareers.get(0));
        }
    }
    
    public static class MigrationDecisionMaker {
        public boolean shouldMigrate(VillagerEntity villager) {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            VillagerEmotionSystem.EmotionalState emotions = VillagerEmotionSystem.getEmotionalState(villager);
            
            if (data == null) return false;
            
            // Consider multiple factors
            float happiness = data.getHappiness();
            float stress = emotions.getEmotion(VillagerEmotionSystem.EmotionType.STRESS);
            float loneliness = emotions.getEmotion(VillagerEmotionSystem.EmotionType.LONELINESS);
            int globalReputation = VillagerGossipNetwork.getGlobalReputation(villager.getUuidAsString());
            
            // Migration more likely if:
            // - Very unhappy
            // - High stress
            // - Very lonely
            // - Poor reputation
            
            float migrationScore = 0.0f;
            
            if (happiness < 20) migrationScore += 0.3f;
            if (stress > 60) migrationScore += 0.2f;
            if (loneliness > 70) migrationScore += 0.2f;
            if (globalReputation < -50) migrationScore += 0.3f;
            
            // Family ties reduce migration tendency
            if (!data.getSpouseId().isEmpty()) migrationScore *= 0.3f;
            if (!data.getChildrenIds().isEmpty()) migrationScore *= 0.2f;
            if (!data.getFamilyMembers().isEmpty()) migrationScore *= 0.7f;
            
            // Personality affects migration tendency
            migrationScore *= switch (data.getPersonality()) {
                case "Energetic", "Curious" -> 1.5f; // More likely to seek change
                case "Shy", "Nervous" -> 0.5f; // Less likely to leave comfort zone
                case "Grumpy" -> 1.2f; // Might leave if dissatisfied
                default -> 1.0f;
            };
            
            return migrationScore > 0.8f && ThreadLocalRandom.current().nextFloat() < 0.02f; // 2% chance when conditions are met
        }
    }
    
    // Player interaction decision making
    public static class PlayerInteractionDecisionMaker {
        public float calculateTradeFairness(VillagerEntity villager, PlayerEntity player,
                                          int playerOfferedValue, int villagerOfferedValue) {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            if (data == null) return 0.5f;
            if (villagerOfferedValue == 0) return 0.5f;
            
            // Base fairness calculation
            float fairnessRatio = (float) playerOfferedValue / villagerOfferedValue;
            
            // Adjust based on relationship
            int reputation = data.getPlayerReputation(player.getUuidAsString());
            float reputationModifier = 1.0f + (reputation / 200.0f); // -0.5 to +0.5
            
            // Adjust based on personality
            float personalityModifier = switch (data.getPersonality()) {
                case "Friendly", "Cheerful" -> 0.9f; // More generous
                case "Grumpy" -> 1.2f; // Wants better deals
                case "Shy" -> 1.1f; // Slightly more cautious
                default -> 1.0f;
            };
            
            float adjustedFairness = fairnessRatio * reputationModifier * personalityModifier;
            
            // Clamp between 0 and 2 (0 = very unfair to villager, 1 = fair, 2 = very unfair to player)
            return Math.max(0.0f, Math.min(2.0f, adjustedFairness));
        }
        
        public boolean shouldAcceptTrade(VillagerEntity villager, PlayerEntity player, 
                                       int playerOfferedValue, int villagerOfferedValue) {
            float fairness = calculateTradeFairness(villager, player, playerOfferedValue, villagerOfferedValue);
            
            // Generate gossip about trade fairness
            if (fairness < 0.7f) {
                VillagerGossipNetwork.CommonGossipEvents.playerTraded(villager, player, false);
            } else if (fairness > 1.0f && fairness < 1.3f) {
                VillagerGossipNetwork.CommonGossipEvents.playerTraded(villager, player, true);
            }
            
            // Accept if fairness is reasonable (0.6 to 1.8 range)
            return fairness >= 0.6f && fairness <= 1.8f;
        }
        
        public float calculateTrustLevel(VillagerEntity villager, PlayerEntity player) {
            VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
            if (data == null) return 0.5f;
            
            int reputation = data.getPlayerReputation(player.getUuidAsString());
            int globalRep = VillagerGossipNetwork.getGlobalReputation(player.getUuidAsString());
            
            // Base trust from personal reputation (normalized to 0-1)
            float personalTrust = Math.max(0.0f, Math.min(1.0f, (reputation + 100) / 200.0f));
            
            // Global reputation influence (normalized to 0-1)
            float globalTrust = Math.max(0.0f, Math.min(1.0f, (globalRep + 200) / 400.0f));
            
            // Weighted combination (personal reputation weighs more)
            float trust = (personalTrust * 0.7f) + (globalTrust * 0.3f);
            
            // Personality adjustments
            trust *= switch (data.getPersonality()) {
                case "Friendly", "Cheerful" -> 1.2f; // More trusting
                case "Nervous", "Shy" -> 0.8f; // Less trusting
                case "Grumpy" -> 0.9f; // Somewhat skeptical
                case "Confident" -> 1.1f; // Moderately trusting
                default -> 1.0f;
            };
            
            return Math.max(0.0f, Math.min(1.0f, trust));
        }
    }
    
    // Global manager for utility AI decisions
    public static class UtilityAIManager {
        private static final MarriageDecisionMaker marriageDecisionMaker = new MarriageDecisionMaker();
        private static final CareerDecisionMaker careerDecisionMaker = new CareerDecisionMaker();
        private static final MigrationDecisionMaker migrationDecisionMaker = new MigrationDecisionMaker();
        private static final PlayerInteractionDecisionMaker playerInteractionDecisionMaker = new PlayerInteractionDecisionMaker();
        
        public static MarriageDecisionMaker getMarriageDecisionMaker() {
            return marriageDecisionMaker;
        }
        
        public static CareerDecisionMaker getCareerDecisionMaker() {
            return careerDecisionMaker;
        }
        
        public static MigrationDecisionMaker getMigrationDecisionMaker() {
            return migrationDecisionMaker;
        }
        
        public static PlayerInteractionDecisionMaker getPlayerInteractionDecisionMaker() {
            return playerInteractionDecisionMaker;
        }
        
        // Periodic decision updates for all villagers
        public static void updateVillagerDecisions(VillagerEntity villager) {
            // Marriage decisions
            List<VillagerEntity> potentialPartners = VillagerRelationshipManager.findPotentialPartners(villager);
            if (!potentialPartners.isEmpty()) {
                List<VillagerEntity> rankedPartners = marriageDecisionMaker.rankPotentialPartners(villager, potentialPartners);
                VillagerEntity bestPartner = rankedPartners.get(0);
                
                if (marriageDecisionMaker.shouldPropose(villager, bestPartner)) {
                    VillagerRelationshipManager.attemptMarriage(villager, bestPartner);
                }
            }
            
            // Career change decisions
            if (careerDecisionMaker.shouldChangeCareer(villager)) {
                // This would integrate with profession changing systems
                // For now, just add to profession history
                VillagerData data = villager.getAttached(com.beeny.Villagersreborn.VILLAGER_DATA);
                if (data != null) {
                    data.addProfession("career_seeker");
                    
                    // Process emotional impact of career change
                    VillagerEmotionSystem.processEmotionalEvent(villager,
                        new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.EXCITEMENT, 20.0f, "career_change", false));
                    VillagerEmotionSystem.processEmotionalEvent(villager,
                        new VillagerEmotionSystem.EmotionalEvent(VillagerEmotionSystem.EmotionType.BOREDOM, -30.0f, "career_change", false));
                }
            }
            
            // Migration decisions
            if (migrationDecisionMaker.shouldMigrate(villager)) {
                // This would integrate with village management systems
                // For now, just create gossip about the migration
                VillagerGossipNetwork.CommonGossipEvents.mysteriousActivity(
                    villager, villager.getUuidAsString(), "is considering leaving the village"
                );
            }
        }
    }
}