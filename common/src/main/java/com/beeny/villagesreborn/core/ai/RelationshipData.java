package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.ai.memory.RelationshipType;
import com.beeny.villagesreborn.core.ai.social.*;
import com.beeny.villagesreborn.core.combat.RelationshipLevel;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import com.beeny.villagesreborn.core.common.NBTCompound;

/**
 * Enhanced RelationshipData implementation supporting dynamic social structures and complex relationships
 */
public class RelationshipData {
    private final UUID playerUUID;
    private float trustLevel = 0.0f;
    private float friendshipLevel = 0.0f;
    private int interactionCount = 0;
    
    // Enhanced relationship tracking
    private final Map<UUID, RelationshipLevel> relationships = new HashMap<>();
    private final Map<UUID, SocialBond> socialBonds = new HashMap<>();
    private final List<SocialMemory> sharedExperiences = new ArrayList<>();
    private final Map<String, Float> socialTraits = new HashMap<>();
    
    // Social network data
    private SocialRole primaryRole = SocialRole.COMMUNITY_MEMBER;
    private int socialInfluence = 0;
    private float reputation = 0.0f;
    private LocalDateTime lastSocialUpdate = LocalDateTime.now();
    
    // Relationship dynamics
    private float conflictTolerance = 0.5f;
    private float loyaltyStrength = 0.5f;
    private float emotionalInvestment = 0.0f;
    private boolean isSociallyActive = true;

    public RelationshipData(UUID playerUUID) {
        this.playerUUID = playerUUID;
        initializeSocialTraits();
    }
    
    // Constructor for tests that need multiple relationship tracking
    public RelationshipData() {
        this.playerUUID = UUID.randomUUID(); // Default UUID for tests
        initializeSocialTraits();
    }
    
    private void initializeSocialTraits() {
        socialTraits.put("charisma", 0.5f);
        socialTraits.put("empathy", 0.5f);
        socialTraits.put("assertiveness", 0.5f);
        socialTraits.put("cooperation", 0.5f);
        socialTraits.put("social_anxiety", 0.3f);
    }

    // Basic relationship methods (legacy compatibility)
    public UUID getPlayerUUID() { return playerUUID; }
    public float getTrustLevel() { return trustLevel; }
    public float getFriendshipLevel() { return friendshipLevel; }
    public int getInteractionCount() { return interactionCount; }

    public void adjustTrust(float amount) {
        this.trustLevel = Math.max(-1.0f, Math.min(1.0f, trustLevel + amount));
        updateLastSocialUpdate();
    }

    public void adjustFriendship(float amount) {
        this.friendshipLevel = Math.max(-1.0f, Math.min(1.0f, friendshipLevel + amount));
        updateLastSocialUpdate();
    }

    public void incrementInteractionCount() {
        this.interactionCount++;
        updateLastSocialUpdate();
    }
    
    // Enhanced social relationship methods
    
    /**
     * Creates or updates a social bond with another villager
     */
    public void createSocialBond(UUID villagerUUID, String villagerName, RelationshipType bondType, 
                                float bondStrength, String formationContext) {
        SocialBond bond = new SocialBond(
            villagerUUID,
            villagerName,
            bondType,
            bondStrength,
            LocalDateTime.now(),
            formationContext
        );
        socialBonds.put(villagerUUID, bond);
        updateLastSocialUpdate();
    }
    
    /**
     * Records a shared experience that can strengthen or weaken bonds
     */
    public void recordSharedExperience(String experienceType, String description, 
                                     List<UUID> participantUUIDs, float emotionalImpact) {
        SocialMemory memory = new SocialMemory(
            experienceType,
            description,
            LocalDateTime.now(),
            new ArrayList<>(participantUUIDs),
            emotionalImpact
        );
        sharedExperiences.add(memory);
        
        // Update emotional investment based on shared experiences
        adjustEmotionalInvestment(emotionalImpact * 0.1f);
        updateLastSocialUpdate();
    }
    
    /**
     * Updates social influence based on community interactions
     */
    public void adjustSocialInfluence(int change, String reason) {
        this.socialInfluence = Math.max(0, Math.min(100, socialInfluence + change));
        
        // Record the change for potential narrative use
        recordSharedExperience("influence_change", 
            String.format("Social influence %s by %d: %s", 
                change > 0 ? "increased" : "decreased", Math.abs(change), reason),
            new ArrayList<>(), change * 0.01f);
    }
    
    /**
     * Updates reputation based on community perception
     */
    public void adjustReputation(float change, String reason) {
        this.reputation = Math.max(-1.0f, Math.min(1.0f, reputation + change));
        updateLastSocialUpdate();
    }
    
    /**
     * Adjusts emotional investment in relationships
     */
    public void adjustEmotionalInvestment(float change) {
        this.emotionalInvestment = Math.max(0.0f, Math.min(1.0f, emotionalInvestment + change));
    }
    
    /**
     * Changes the villager's primary social role
     */
    public void updateSocialRole(SocialRole newRole, String transitionReason) {
        SocialRole oldRole = this.primaryRole;
        this.primaryRole = newRole;
        
        // Record this significant social change
        recordSharedExperience("role_transition", 
            String.format("Became %s (was %s): %s", newRole.name(), oldRole.name(), transitionReason),
            new ArrayList<>(), 0.3f);
            
        updateLastSocialUpdate();
    }
    
    /**
     * Updates a specific social trait
     */
    public void adjustSocialTrait(String traitName, float adjustment) {
        if (socialTraits.containsKey(traitName)) {
            float currentValue = socialTraits.get(traitName);
            float newValue = Math.max(0.0f, Math.min(1.0f, currentValue + adjustment));
            socialTraits.put(traitName, newValue);
            updateLastSocialUpdate();
        }
    }
    
    /**
     * Resolves a conflict with another villager, affecting the relationship
     */
    public void resolveConflict(UUID villagerUUID, String conflictType, ConflictResolution resolution) {
        SocialBond bond = socialBonds.get(villagerUUID);
        if (bond != null) {
            float impactStrength = resolution.getImpactStrength();
            
            // Adjust bond strength based on resolution
            bond.adjustStrength(impactStrength);
            
            // Record the conflict resolution
            recordSharedExperience("conflict_resolution", 
                String.format("Resolved %s conflict with %s: %s", 
                    conflictType, bond.getVillagerName(), resolution.getDescription()),
                List.of(villagerUUID), impactStrength);
                
            // Affect social traits
            if (resolution == ConflictResolution.PEACEFUL_COMPROMISE) {
                adjustSocialTrait("cooperation", 0.1f);
                adjustSocialTrait("empathy", 0.05f);
            } else if (resolution == ConflictResolution.DOMINANCE_ASSERTED) {
                adjustSocialTrait("assertiveness", 0.1f);
                adjustSocialTrait("cooperation", -0.05f);
            }
        }
    }
    
    /**
     * Processes social dynamics decay over time
     */
    public void processTimeDecay(long daysPassed) {
        // Reduce intensity of old shared experiences
        sharedExperiences.removeIf(memory -> 
            memory.getTimestamp().plusDays(30).isBefore(LocalDateTime.now()));
            
        // Gradual decay of emotional investment if no recent interactions
        if (daysPassed > 7) {
            adjustEmotionalInvestment(-0.01f * daysPassed);
        }
        
        // Social bonds can strengthen or weaken over time
        for (SocialBond bond : socialBonds.values()) {
            bond.processTimeDecay(daysPassed);
        }
    }
    
    /**
     * Calculates overall social compatibility with another villager
     */
    public float calculateSocialCompatibility(RelationshipData otherVillager) {
        float compatibility = 0.0f;
        
        // Compare social traits
        for (Map.Entry<String, Float> trait : socialTraits.entrySet()) {
            float otherValue = otherVillager.getSocialTrait(trait.getKey());
            float difference = Math.abs(trait.getValue() - otherValue);
            
            // Some traits are better when similar, others when complementary
            if (trait.getKey().equals("cooperation") || trait.getKey().equals("empathy")) {
                compatibility += (1.0f - difference) * 0.2f; // Better when similar
            } else if (trait.getKey().equals("assertiveness")) {
                compatibility += difference * 0.1f; // Can be complementary
            }
        }
        
        return Math.max(0.0f, Math.min(1.0f, compatibility));
    }
    
    // Combat relationship methods (legacy compatibility)
    public void setRelationshipLevel(UUID entityUUID, RelationshipLevel level) {
        relationships.put(entityUUID, level);
    }
    
    public RelationshipLevel getRelationshipLevel(UUID entityUUID) {
        return relationships.getOrDefault(entityUUID, RelationshipLevel.STRANGER);
    }
    
    public boolean isEnemy(UUID entityUUID) {
        return getRelationshipLevel(entityUUID) == RelationshipLevel.ENEMY;
    }
    
    public boolean isFriend(UUID entityUUID) {
        RelationshipLevel level = getRelationshipLevel(entityUUID);
        return level == RelationshipLevel.FRIEND || level == RelationshipLevel.CLOSE_FRIEND || level == RelationshipLevel.FAMILY;
    }
    
    // Getters for enhanced social data
    public Map<UUID, SocialBond> getSocialBonds() { return new HashMap<>(socialBonds); }
    public List<SocialMemory> getSharedExperiences() { return new ArrayList<>(sharedExperiences); }
    public Map<String, Float> getSocialTraits() { return new HashMap<>(socialTraits); }
    public SocialRole getPrimaryRole() { return primaryRole; }
    public int getSocialInfluence() { return socialInfluence; }
    public float getReputation() { return reputation; }
    public float getConflictTolerance() { return conflictTolerance; }
    public float getLoyaltyStrength() { return loyaltyStrength; }
    public float getEmotionalInvestment() { return emotionalInvestment; }
    public boolean isSociallyActive() { return isSociallyActive; }
    public LocalDateTime getLastSocialUpdate() { return lastSocialUpdate; }
    
    public float getSocialTrait(String traitName) {
        return socialTraits.getOrDefault(traitName, 0.5f);
    }
    
    public SocialBond getSocialBond(UUID villagerUUID) {
        return socialBonds.get(villagerUUID);
    }
    
    // Setters
    public void setConflictTolerance(float conflictTolerance) {
        this.conflictTolerance = Math.max(0.0f, Math.min(1.0f, conflictTolerance));
    }
    
    public void setLoyaltyStrength(float loyaltyStrength) {
        this.loyaltyStrength = Math.max(0.0f, Math.min(1.0f, loyaltyStrength));
    }
    
    public void setSociallyActive(boolean sociallyActive) {
        this.isSociallyActive = sociallyActive;
        updateLastSocialUpdate();
    }
    
    private void updateLastSocialUpdate() {
        this.lastSocialUpdate = LocalDateTime.now();
    }
    
    /**
     * Generates a social profile summary for this villager
     */
    public String generateSocialProfile() {
        StringBuilder profile = new StringBuilder();
        profile.append(String.format("Social Role: %s (Influence: %d)\n", primaryRole.name(), socialInfluence));
        profile.append(String.format("Reputation: %.2f | Emotional Investment: %.2f\n", reputation, emotionalInvestment));
        profile.append(String.format("Trust: %.2f | Friendship: %.2f\n", trustLevel, friendshipLevel));
        profile.append(String.format("Interactions: %d | Active: %s\n", interactionCount, isSociallyActive));
        
        if (!socialBonds.isEmpty()) {
            profile.append("\nKey Relationships:\n");
            socialBonds.values().stream()
                .sorted((b1, b2) -> Float.compare(Math.abs(b2.getBondStrength()), Math.abs(b1.getBondStrength())))
                .limit(3)
                .forEach(bond -> profile.append(String.format("- %s (%s): %.2f\n", 
                    bond.getVillagerName(), bond.getBondType().name(), bond.getBondStrength())));
        }
        
        return profile.toString();
    }

    public NBTCompound toNBT() {
        NBTCompound nbt = new NBTCompound();
        nbt.putUUID("playerUUID", playerUUID);
        nbt.putFloat("trustLevel", trustLevel);
        nbt.putFloat("friendshipLevel", friendshipLevel);
        nbt.putInt("interactionCount", interactionCount);
        return nbt;
    }

    public static RelationshipData fromNBT(NBTCompound nbt) {
        // Validate essential data - playerUUID is required
        UUID playerUUID = nbt.getUUID("playerUUID");
        if (playerUUID == null) {
            throw new IllegalArgumentException("Cannot deserialize RelationshipData: playerUUID is missing from NBT data");
        }
        
        RelationshipData data = new RelationshipData(playerUUID);
        
        // Safely read optional data with default values if missing
        if (nbt.contains("trustLevel")) {
            data.trustLevel = nbt.getFloat("trustLevel");
        }
        
        if (nbt.contains("friendshipLevel")) {
            data.friendshipLevel = nbt.getFloat("friendshipLevel");
        }
        
        if (nbt.contains("interactionCount")) {
            data.interactionCount = nbt.getInt("interactionCount");
        }
        
        return data;
    }
}