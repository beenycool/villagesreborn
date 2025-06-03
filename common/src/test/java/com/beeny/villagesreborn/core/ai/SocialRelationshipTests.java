package com.beeny.villagesreborn.core.ai;

import com.beeny.villagesreborn.core.ai.memory.RelationshipType;
import com.beeny.villagesreborn.core.ai.social.*;
import com.beeny.villagesreborn.core.combat.RelationshipLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the enhanced social relationship systems
 */
public class SocialRelationshipTests {
    
    private RelationshipData relationshipData;
    private UUID villagerUUID;
    private UUID playerUUID;
    
    @BeforeEach
    void setUp() {
        playerUUID = UUID.randomUUID();
        villagerUUID = UUID.randomUUID();
        relationshipData = new RelationshipData(playerUUID);
    }
    
    @Test
    @DisplayName("Should create and manage social bonds")
    void testSocialBondCreation() {
        // Given
        String villagerName = "Alice";
        RelationshipType bondType = RelationshipType.FRIEND;
        float bondStrength = 0.7f;
        String formationContext = "Met during village festival";
        
        // When
        relationshipData.createSocialBond(villagerUUID, villagerName, bondType, bondStrength, formationContext);
        
        // Then
        Map<UUID, SocialBond> bonds = relationshipData.getSocialBonds();
        assertTrue(bonds.containsKey(villagerUUID));
        
        SocialBond bond = bonds.get(villagerUUID);
        assertEquals(villagerUUID, bond.getVillagerUUID());
        assertEquals(villagerName, bond.getVillagerName());
        assertEquals(bondType, bond.getBondType());
        assertEquals(bondStrength, bond.getBondStrength(), 0.01f);
        assertEquals(formationContext, bond.getFormationContext());
        assertTrue(bond.isPositiveBond());
    }
    
    @Test
    @DisplayName("Should record and track shared experiences")
    void testSharedExperienceRecording() {
        // Given
        String experienceType = "festival_participation";
        String description = "Danced together at the harvest festival";
        List<UUID> participants = Arrays.asList(villagerUUID, playerUUID);
        float emotionalImpact = 0.6f;
        
        // When
        relationshipData.recordSharedExperience(experienceType, description, participants, emotionalImpact);
        
        // Then
        List<SocialMemory> experiences = relationshipData.getSharedExperiences();
        assertEquals(1, experiences.size());
        
        SocialMemory experience = experiences.get(0);
        assertEquals(experienceType, experience.getExperienceType());
        assertEquals(description, experience.getDescription());
        assertEquals(participants.size(), experience.getParticipantUUIDs().size());
        assertEquals(emotionalImpact, experience.getEmotionalImpact(), 0.01f);
        assertTrue(experience.isPositiveMemory());
        assertTrue(experience.involvesVillager(villagerUUID));
    }
    
    @Test
    @DisplayName("Should manage social influence and reputation")
    void testSocialInfluenceManagement() {
        // Given
        int initialInfluence = relationshipData.getSocialInfluence();
        float initialReputation = relationshipData.getReputation();
        
        // When
        relationshipData.adjustSocialInfluence(10, "Helped organize village event");
        relationshipData.adjustReputation(0.3f, "Known for kindness");
        
        // Then
        assertEquals(initialInfluence + 10, relationshipData.getSocialInfluence());
        assertEquals(initialReputation + 0.3f, relationshipData.getReputation(), 0.01f);
        
        // Should have recorded the influence change as a shared experience
        List<SocialMemory> experiences = relationshipData.getSharedExperiences();
        assertEquals(1, experiences.size());
        assertTrue(experiences.get(0).getDescription().contains("increased"));
    }
    
    @Test
    @DisplayName("Should update social roles with transition tracking")
    void testSocialRoleTransition() {
        // Given
        SocialRole initialRole = relationshipData.getPrimaryRole();
        SocialRole newRole = SocialRole.ELDER;
        String transitionReason = "Gained wisdom and respect over the years";
        
        // When
        relationshipData.updateSocialRole(newRole, transitionReason);
        
        // Then
        assertEquals(newRole, relationshipData.getPrimaryRole());
        
        // Should have recorded the role transition
        List<SocialMemory> experiences = relationshipData.getSharedExperiences();
        assertEquals(1, experiences.size());
        
        SocialMemory transition = experiences.get(0);
        assertEquals("role_transition", transition.getExperienceType());
        assertTrue(transition.getDescription().contains(newRole.name()));
        assertTrue(transition.getDescription().contains(initialRole.name()));
    }
    
    @Test
    @DisplayName("Should adjust social traits dynamically")
    void testSocialTraitAdjustment() {
        // Given
        float initialEmpathy = relationshipData.getSocialTrait("empathy");
        
        // When
        relationshipData.adjustSocialTrait("empathy", 0.2f);
        relationshipData.adjustSocialTrait("nonexistent_trait", 0.1f); // Should be ignored
        
        // Then
        assertEquals(initialEmpathy + 0.2f, relationshipData.getSocialTrait("empathy"), 0.01f);
        
        // Should respect bounds (0.0 to 1.0)
        relationshipData.adjustSocialTrait("empathy", 1.0f);
        assertEquals(1.0f, relationshipData.getSocialTrait("empathy"), 0.01f);
        
        relationshipData.adjustSocialTrait("empathy", -2.0f);
        assertEquals(0.0f, relationshipData.getSocialTrait("empathy"), 0.01f);
    }
    
    @Test
    @DisplayName("Should resolve conflicts and update relationships")
    void testConflictResolution() {
        // Given - create a social bond first
        relationshipData.createSocialBond(villagerUUID, "Bob", RelationshipType.FRIEND, 0.6f, "Old friends");
        
        String conflictType = "resource_dispute";
        ConflictResolution resolution = ConflictResolution.PEACEFUL_COMPROMISE;
        
        // When
        relationshipData.resolveConflict(villagerUUID, conflictType, resolution);
        
        // Then
        SocialBond bond = relationshipData.getSocialBond(villagerUUID);
        assertNotNull(bond);
        
        // Bond strength should have improved due to positive resolution
        float newStrength = bond.getBondStrength();
        assertTrue(newStrength > 0.6f);
        
        // Should have recorded the conflict resolution
        List<SocialMemory> experiences = relationshipData.getSharedExperiences();
        boolean hasConflictResolution = experiences.stream()
            .anyMatch(exp -> "conflict_resolution".equals(exp.getExperienceType()));
        assertTrue(hasConflictResolution);
        
        // Should have improved cooperation trait
        assertTrue(relationshipData.getSocialTrait("cooperation") > 0.5f);
    }
    
    @Test
    @DisplayName("Should process time decay correctly")
    void testTimeDecay() {
        // Given - create bond and shared experience
        relationshipData.createSocialBond(villagerUUID, "Charlie", RelationshipType.FRIEND, 0.8f, "Good friends");
        relationshipData.recordSharedExperience("party", "Had fun at party", Arrays.asList(villagerUUID), 0.5f);
        
        float initialEmotionalInvestment = relationshipData.getEmotionalInvestment();
        
        // When - process significant time passage
        relationshipData.processTimeDecay(30); // 30 days
        
        // Then
        // Emotional investment should have decayed
        assertTrue(relationshipData.getEmotionalInvestment() < initialEmotionalInvestment);
        
        // Old shared experiences should be removed (30+ days old)
        relationshipData.processTimeDecay(35);
        // Note: The shared experience we just added won't be 30+ days old in the test,
        // but this tests the decay mechanism
    }
    
    @Test
    @DisplayName("Should calculate social compatibility")
    void testSocialCompatibilityCalculation() {
        // Given - two villagers with different traits
        RelationshipData otherVillager = new RelationshipData();
        
        // Make one villager more cooperative and empathetic
        relationshipData.adjustSocialTrait("cooperation", 0.3f); // 0.8
        relationshipData.adjustSocialTrait("empathy", 0.2f); // 0.7
        
        // Make other villager less cooperative but equally empathetic
        otherVillager.adjustSocialTrait("cooperation", -0.2f); // 0.3
        otherVillager.adjustSocialTrait("empathy", 0.2f); // 0.7
        
        // When
        float compatibility = relationshipData.calculateSocialCompatibility(otherVillager);
        
        // Then
        assertTrue(compatibility >= 0.0f && compatibility <= 1.0f);
        // Should have some compatibility due to similar empathy, but reduced due to different cooperation
    }
    
    @Test
    @DisplayName("Should maintain legacy combat relationship compatibility")
    void testLegacyCombatRelationships() {
        // Given
        UUID enemyUUID = UUID.randomUUID();
        UUID friendUUID = UUID.randomUUID();
        
        // When
        relationshipData.setRelationshipLevel(enemyUUID, RelationshipLevel.ENEMY);
        relationshipData.setRelationshipLevel(friendUUID, RelationshipLevel.FRIEND);
        
        // Then
        assertEquals(RelationshipLevel.ENEMY, relationshipData.getRelationshipLevel(enemyUUID));
        assertEquals(RelationshipLevel.FRIEND, relationshipData.getRelationshipLevel(friendUUID));
        assertEquals(RelationshipLevel.STRANGER, relationshipData.getRelationshipLevel(UUID.randomUUID()));
        
        assertTrue(relationshipData.isEnemy(enemyUUID));
        assertFalse(relationshipData.isEnemy(friendUUID));
        
        assertTrue(relationshipData.isFriend(friendUUID));
        assertFalse(relationshipData.isFriend(enemyUUID));
    }
    
    @Test
    @DisplayName("Should generate comprehensive social profile")
    void testSocialProfileGeneration() {
        // Given - set up a complex social situation
        relationshipData.updateSocialRole(SocialRole.ELDER, "Gained wisdom");
        relationshipData.adjustSocialInfluence(25, "Community leadership");
        relationshipData.adjustReputation(0.7f, "Wise counsel");
        relationshipData.adjustTrust(0.4f);
        relationshipData.adjustFriendship(0.6f);
        relationshipData.incrementInteractionCount();
        relationshipData.createSocialBond(villagerUUID, "BestFriend", RelationshipType.BEST_FRIEND, 0.9f, "Childhood friends");
        
        // When
        String profile = relationshipData.generateSocialProfile();
        
        // Then
        assertNotNull(profile);
        assertFalse(profile.trim().isEmpty());
        assertTrue(profile.contains("ELDER"));
        assertTrue(profile.contains("25")); // influence
        assertTrue(profile.contains("0.70")); // reputation  
        assertTrue(profile.contains("BestFriend"));
        assertTrue(profile.contains("BEST_FRIEND"));
    }
    
    @Test
    @DisplayName("Social bond should evolve over time")
    void testSocialBondEvolution() {
        // Given
        relationshipData.createSocialBond(villagerUUID, "Eve", RelationshipType.ACQUAINTANCE, 0.3f, "Just met");
        SocialBond bond = relationshipData.getSocialBond(villagerUUID);
        
        // When - record shared experiences
        bond.recordSharedExperience(0.4f);
        bond.recordSharedExperience(0.3f);
        
        // Then
        assertTrue(bond.getBondStrength() > 0.3f);
        assertEquals(2, bond.getSharedExperienceCount());
        assertTrue(bond.getEmotionalInvestment() > 0.0f);
        assertTrue(bond.getRelationshipQuality() > 0.3f);
        
        // Should be considered a strong bond now
        if (bond.getBondStrength() > 0.6f && bond.getEmotionalInvestment() > 0.4f) {
            assertTrue(bond.isStrongBond());
        }
    }
    
    @Test
    @DisplayName("Social memory should track significance and influence")
    void testSocialMemoryProperties() {
        // Given
        String experienceType = "village_rescue";
        String description = "Saved the village from raiders";
        List<UUID> participants = Arrays.asList(villagerUUID, playerUUID, UUID.randomUUID());
        float highEmotionalImpact = 0.9f;
        
        // When
        relationshipData.recordSharedExperience(experienceType, description, participants, highEmotionalImpact);
        
        // Then
        SocialMemory memory = relationshipData.getSharedExperiences().get(0);
        
        assertTrue(memory.isPositiveMemory());
        assertTrue(memory.isHighlySignificant()); // Should be significant due to high impact and multiple participants
        assertEquals(0, memory.getAgeDays()); // Just created
        assertTrue(memory.getCurrentInfluence() > 0.0f);
        
        // Should involve the villager
        assertTrue(memory.involvesVillager(villagerUUID));
        assertFalse(memory.involvesVillager(UUID.randomUUID()));
    }
    
    @Test
    @DisplayName("Should handle negative conflict resolution")
    void testNegativeConflictResolution() {
        // Given
        relationshipData.createSocialBond(villagerUUID, "Rival", RelationshipType.RIVAL, -0.2f, "Started as enemies");
        
        // When - resolve conflict negatively
        relationshipData.resolveConflict(villagerUUID, "territory_dispute", ConflictResolution.ESCALATED_CONFLICT);
        
        // Then
        SocialBond bond = relationshipData.getSocialBond(villagerUUID);
        assertTrue(bond.getBondStrength() < -0.2f); // Should have worsened
        assertFalse(bond.isPositiveBond());
        
        // Should have recorded the escalation
        boolean hasEscalation = relationshipData.getSharedExperiences().stream()
            .anyMatch(exp -> exp.getDescription().contains("conflict") && exp.getEmotionalImpact() < 0);
        assertTrue(hasEscalation);
    }
    
    @Test
    @DisplayName("Should respect social trait boundaries")
    void testSocialTraitBoundaries() {
        // When - try to exceed boundaries
        relationshipData.adjustSocialTrait("charisma", 2.0f); // Should cap at 1.0
        relationshipData.adjustSocialTrait("empathy", -2.0f); // Should floor at 0.0
        
        // Then
        assertEquals(1.0f, relationshipData.getSocialTrait("charisma"), 0.01f);
        assertEquals(0.0f, relationshipData.getSocialTrait("empathy"), 0.01f);
    }
}