package com.beeny.villagesreborn.core.combat;

import com.beeny.villagesreborn.core.ai.PersonalityProfile;
import com.beeny.villagesreborn.core.ai.RelationshipData;
import com.beeny.villagesreborn.core.common.VillagerEntity;
import com.beeny.villagesreborn.core.common.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test-Driven Development tests for Combat Decision Logic
 * Tests villager combat decision-making based on personality traits,
 * relationship data, threat assessment, and equipment selection
 */
@DisplayName("Combat Decision Logic Tests")
class CombatDecisionLogicTests {

    @Mock
    private VillagerEntity mockVillager;
    
    @Mock
    private LivingEntity mockEnemy1;
    
    @Mock
    private LivingEntity mockEnemy2;
    
    @Mock
    private LivingEntity mockFriend;
    
    @Mock
    private ItemStack mockSword;
    
    @Mock
    private ItemStack mockBow;
    
    @Mock
    private ItemStack mockIronArmor;
    
    @Mock
    private ItemStack mockLeatherArmor;
    
    private CombatDecisionEngine combatEngine;
    private CombatPersonalityTraits aggressiveTraits;
    private CombatPersonalityTraits timidTraits;
    private CombatPersonalityTraits loyalTraits;
    private UUID villagerUUID;
    private UUID enemyUUID;
    private UUID friendUUID;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        villagerUUID = UUID.randomUUID();
        enemyUUID = UUID.randomUUID();
        friendUUID = UUID.randomUUID();
        
        // Mock villager setup
        when(mockVillager.getUUID()).thenReturn(villagerUUID);
        when(mockVillager.getBlockPos()).thenReturn(new BlockPos(0, 64, 0));
        when(mockVillager.getName()).thenReturn("TestVillager");
        
        // Mock enemy setup
        when(mockEnemy1.getUUID()).thenReturn(enemyUUID);
        when(mockEnemy1.isAlive()).thenReturn(true);
        when(mockEnemy1.isHostile()).thenReturn(true);
        when(mockEnemy1.getHealth()).thenReturn(20.0f);
        when(mockEnemy1.getAttackDamage()).thenReturn(6.0f);
        when(mockEnemy1.getDistanceTo(mockVillager)).thenReturn(5.0f);
        
        when(mockEnemy2.getUUID()).thenReturn(UUID.randomUUID());
        when(mockEnemy2.isAlive()).thenReturn(true);
        when(mockEnemy2.isHostile()).thenReturn(true);
        when(mockEnemy2.getHealth()).thenReturn(10.0f);
        when(mockEnemy2.getAttackDamage()).thenReturn(4.0f);
        when(mockEnemy2.getDistanceTo(mockVillager)).thenReturn(8.0f);
        
        // Mock friend setup
        when(mockFriend.getUUID()).thenReturn(friendUUID);
        when(mockFriend.isAlive()).thenReturn(true);
        when(mockFriend.isHostile()).thenReturn(false);
        
        // Mock equipment setup
        when(mockSword.getAttackDamage()).thenReturn(7.0f);
        when(mockSword.getItemType()).thenReturn(ItemType.SWORD);
        
        when(mockBow.getAttackDamage()).thenReturn(5.0f);
        when(mockBow.getItemType()).thenReturn(ItemType.BOW);
        when(mockBow.getRange()).thenReturn(15.0f);
        
        when(mockIronArmor.getDefenseValue()).thenReturn(5);
        when(mockIronArmor.getEquipmentSlot()).thenReturn(EquipmentSlot.CHEST);
        
        when(mockLeatherArmor.getDefenseValue()).thenReturn(2);
        when(mockLeatherArmor.getEquipmentSlot()).thenReturn(EquipmentSlot.CHEST);
        
        // Setup personality traits
        aggressiveTraits = new CombatPersonalityTraits();
        aggressiveTraits.setCourage(0.9f);
        aggressiveTraits.setAggression(0.8f);
        aggressiveTraits.setLoyalty(0.6f);
        aggressiveTraits.setSelfPreservation(0.3f);
        aggressiveTraits.setVengefulness(0.7f);
        
        timidTraits = new CombatPersonalityTraits();
        timidTraits.setCourage(0.2f);
        timidTraits.setAggression(0.1f);
        timidTraits.setLoyalty(0.7f);
        timidTraits.setSelfPreservation(0.9f);
        timidTraits.setVengefulness(0.2f);
        
        loyalTraits = new CombatPersonalityTraits();
        loyalTraits.setCourage(0.6f);
        loyalTraits.setAggression(0.4f);
        loyalTraits.setLoyalty(0.9f);
        loyalTraits.setSelfPreservation(0.5f);
        loyalTraits.setVengefulness(0.6f);
        
        combatEngine = new CombatDecisionEngine();
    }

    @Test
    @DisplayName("Should equip sword when personality favors melee combat")
    void shouldEquipSwordWhenPersonalityFavorsMelee() {
        // Given: Aggressive villager with melee preference
        List<ItemStack> availableWeapons = Arrays.asList(mockSword, mockBow);
        
        // When: Selecting best weapon based on aggressive personality
        ItemStack selectedWeapon = combatEngine.selectBestWeapon(
            availableWeapons, 
            aggressiveTraits, 
            mockVillager
        );
        
        // Then: Should prefer sword for close combat
        assertEquals(mockSword, selectedWeapon);
        assertEquals(ItemType.SWORD, selectedWeapon.getItemType());
    }

    @Test
    @DisplayName("Should equip bow when personality favors ranged combat")
    void shouldEquipBowWhenPersonalityFavorsRanged() {
        // Given: Timid villager with self-preservation focus
        List<ItemStack> availableWeapons = Arrays.asList(mockSword, mockBow);
        
        // When: Selecting best weapon based on timid personality
        ItemStack selectedWeapon = combatEngine.selectBestWeapon(
            availableWeapons, 
            timidTraits, 
            mockVillager
        );
        
        // Then: Should prefer bow for safe distance combat
        assertEquals(mockBow, selectedWeapon);
        assertEquals(ItemType.BOW, selectedWeapon.getItemType());
    }

    @Test
    @DisplayName("Should equip best armor based on defense value")
    void shouldEquipBestArmorBasedOnDefenseValue() {
        // Given: Available armor pieces
        List<ItemStack> availableArmor = Arrays.asList(mockLeatherArmor, mockIronArmor);
        
        // When: Selecting best armor
        ItemStack selectedArmor = combatEngine.selectBestArmor(
            availableArmor, 
            EquipmentSlot.CHEST, 
            aggressiveTraits
        );
        
        // Then: Should select iron armor (higher defense)
        assertEquals(mockIronArmor, selectedArmor);
        assertEquals(5, selectedArmor.getDefenseValue());
    }

    @Test
    @DisplayName("Should select enemy with highest threat score as target")
    void shouldSelectEnemyWithHighestThreatScore() {
        // Given: Multiple enemies with different threat levels
        List<LivingEntity> enemies = Arrays.asList(mockEnemy1, mockEnemy2);
        // Enemy1: 20 health, 6 damage, 5 distance = higher threat
        // Enemy2: 10 health, 4 damage, 8 distance = lower threat
        
        // When: Selecting target
        LivingEntity selectedTarget = combatEngine.selectTarget(
            enemies, 
            mockVillager, 
            aggressiveTraits
        );
        
        // Then: Should select enemy with highest threat (enemy1)
        assertEquals(mockEnemy1, selectedTarget);
    }

    @Test
    @DisplayName("Should consider relationship data when selecting targets")
    void shouldConsiderRelationshipDataWhenSelectingTargets() {
        // Given: Enemy and friend in potential target list
        List<LivingEntity> potentialTargets = Arrays.asList(mockEnemy1, mockFriend);
        
        RelationshipData relationships = new RelationshipData();
        relationships.setRelationshipLevel(friendUUID, RelationshipLevel.FRIEND);
        relationships.setRelationshipLevel(enemyUUID, RelationshipLevel.ENEMY);
        
        // When: Selecting target with relationship considerations
        LivingEntity selectedTarget = combatEngine.selectTargetWithRelationships(
            potentialTargets, 
            mockVillager, 
            aggressiveTraits,
            relationships
        );
        
        // Then: Should target enemy, not friend
        assertEquals(mockEnemy1, selectedTarget);
        assertNotEquals(mockFriend, selectedTarget);
    }

    @Test
    @DisplayName("Should respect aggression threshold when deciding to attack")
    void shouldRespectAggressionThresholdWhenDecidingToAttack() {
        // Given: Different personality traits
        ThreatAssessment moderateThreat = new ThreatAssessment(
            ThreatLevel.MODERATE, 
            mockEnemy1, 
            0.6f
        );
        
        // When: Checking if should attack with aggressive personality
        boolean aggressiveShouldAttack = combatEngine.shouldAttack(
            moderateThreat, 
            aggressiveTraits, 
            mockVillager
        );
        
        // When: Checking if should attack with timid personality  
        boolean timidShouldAttack = combatEngine.shouldAttack(
            moderateThreat, 
            timidTraits, 
            mockVillager
        );
        
        // Then: Aggressive should attack, timid should not
        assertTrue(aggressiveShouldAttack);
        assertFalse(timidShouldAttack);
    }

    @Test
    @DisplayName("Should defend friends based on loyalty trait")
    void shouldDefendFriendsBasedOnLoyaltyTrait() {
        // Given: Friend under attack
        when(mockFriend.isBeingAttacked()).thenReturn(true);
        when(mockFriend.getAttacker()).thenReturn(mockEnemy1);
        
        RelationshipData relationships = new RelationshipData();
        relationships.setRelationshipLevel(friendUUID, RelationshipLevel.CLOSE_FRIEND);
        
        // When: Checking if should defend with loyal personality
        boolean loyalShouldDefend = combatEngine.shouldDefend(
            mockFriend, 
            loyalTraits, 
            relationships
        );
        
        // When: Checking if should defend with low-loyalty personality
        boolean aggressiveShouldDefend = combatEngine.shouldDefend(
            mockFriend, 
            aggressiveTraits, 
            relationships
        );
        
        // Then: Loyal villager should defend, aggressive may not
        assertTrue(loyalShouldDefend);
        // Aggressive has 0.6 loyalty, should still defend close friends
        assertTrue(aggressiveShouldDefend);
    }

    @Test
    @DisplayName("Should avoid friendly fire when selecting targets")
    void shouldAvoidFriendlyFireWhenSelectingTargets() {
        // Given: Mixed group including friends and enemies
        List<LivingEntity> mixedTargets = Arrays.asList(mockEnemy1, mockFriend, mockEnemy2);
        
        RelationshipData relationships = new RelationshipData();
        relationships.setRelationshipLevel(friendUUID, RelationshipLevel.FRIEND);
        relationships.setRelationshipLevel(enemyUUID, RelationshipLevel.ENEMY);
        
        // When: Getting valid targets (excluding friends)
        List<LivingEntity> validTargets = combatEngine.getValidCombatTargets(
            mixedTargets, 
            relationships, 
            mockVillager
        );
        
        // Then: Should only contain enemies, not friends
        assertEquals(2, validTargets.size());
        assertTrue(validTargets.contains(mockEnemy1));
        assertTrue(validTargets.contains(mockEnemy2));
        assertFalse(validTargets.contains(mockFriend));
    }

    @Test
    @DisplayName("Should handle edge case of no enemies present")
    void shouldHandleEdgeCaseOfNoEnemiesPresent() {
        // Given: Empty enemy list
        List<LivingEntity> noEnemies = Collections.emptyList();
        
        // When: Trying to select target
        LivingEntity selectedTarget = combatEngine.selectTarget(
            noEnemies, 
            mockVillager, 
            aggressiveTraits
        );
        
        // Then: Should return null (no target)
        assertNull(selectedTarget);
    }

    @Test
    @DisplayName("Should calculate correct threat score based on enemy stats")
    void shouldCalculateCorrectThreatScoreBasedOnEnemyStats() {
        // Given: Enemy with known stats
        when(mockEnemy1.getHealth()).thenReturn(20.0f);
        when(mockEnemy1.getAttackDamage()).thenReturn(8.0f);
        when(mockEnemy1.getDistanceTo(mockVillager)).thenReturn(5.0f);
        
        // When: Calculating threat score
        float threatScore = combatEngine.calculateThreatScore(
            mockEnemy1, 
            mockVillager, 
            aggressiveTraits
        );
        
        // Then: Should return reasonable threat score (0.0 to 1.0)
        assertTrue(threatScore >= 0.0f && threatScore <= 1.0f);
        assertTrue(threatScore > 0.5f); // High health + damage should be significant threat
    }

    @Test
    @DisplayName("Should flee when self-preservation is high and threat is overwhelming")
    void shouldFleeWhenSelfPreservationIsHighAndThreatIsOverwhelming() {
        // Given: Overwhelming threat
        ThreatAssessment overwhelmingThreat = new ThreatAssessment(
            ThreatLevel.EXTREME, 
            mockEnemy1, 
            0.95f
        );
        
        // When: Checking if should flee with timid personality
        boolean timidShouldFlee = combatEngine.shouldFlee(
            overwhelmingThreat, 
            timidTraits, 
            mockVillager
        );
        
        // When: Checking if should flee with aggressive personality
        boolean aggressiveShouldFlee = combatEngine.shouldFlee(
            overwhelmingThreat, 
            aggressiveTraits, 
            mockVillager
        );
        
        // Then: Timid should flee, aggressive might stay and fight
        assertTrue(timidShouldFlee);
        assertFalse(aggressiveShouldFlee); // Aggressive + high courage should stand ground
    }
}