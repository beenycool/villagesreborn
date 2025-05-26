# PHASE 5: Combat, Expansion & Governance AI Specification

## Overview
Phase 5 integrates AI-driven decision making into three critical village systems: combat tactics, expansion planning, and governance processes. This phase enhances existing rule-based systems with LLM-powered intelligence while maintaining robust fallback mechanisms.

## Architecture Goals
- **Modular AI Integration**: Hook into existing systems without breaking current functionality
- **Performance Constraints**: Maintain <200ms decision latency under load
- **Graceful Degradation**: Seamless fallback to rule-based logic when AI unavailable
- **Data Persistence**: Extend world data for AI memory and learning
- **Test Coverage**: Comprehensive TDD approach with integration and performance tests

## Component Specifications

### 1. Combat AI Integration (`CombatAIEngine`)

#### Core Interface
```java
public interface CombatAIEngine {
    CompletableFuture<CombatDecision> analyzeAndDecide(
        CombatSituation situation, 
        CombatPersonalityTraits traits,
        ThreatAssessment threatData,
        RelationshipData relationships,
        long timeoutMs
    );
    
    boolean shouldUseAI(VillagerEntity villager, CombatSituation situation);
    CombatDecision fallbackDecision(CombatSituation situation, CombatPersonalityTraits traits);
}
```

#### Integration with Existing [`CombatDecisionEngine`](common/src/main/java/com/beeny/villagesreborn/core/combat/CombatDecisionEngine.java:13)

**Pseudocode:**
```
FUNCTION enhancedCombatTick(villager, situation):
    // Gather contextual data
    threatAssessment = assessThreats(villager, situation.enemies)
    relationshipData = getRelationships(villager)
    personalityTraits = villager.getCombatTraits()
    
    // Determine AI usage
    IF shouldUseAI(villager, situation):
        // Async AI analysis with timeout
        aiDecisionFuture = combatAI.analyzeAndDecide(
            situation, traits, threatAssessment, relationshipData, 150ms
        )
        
        TRY:
            aiDecision = aiDecisionFuture.get(150ms, MILLISECONDS)
            IF validateAIDecision(aiDecision, situation):
                RETURN executeDecision(aiDecision)
        CATCH TimeoutException, ValidationException:
            // Fallback to rule-based
            
    // Rule-based fallback (existing logic)
    ruleBasedDecision = combatDecisionEngine.makeDecision(situation, traits)
    RETURN executeDecision(ruleBasedDecision)
```

#### AI Prompt Structure for Combat
```
COMBAT_PROMPT_TEMPLATE = """
You are a villager facing a combat situation. Analyze and decide:

Personality: {traits}
Current Threat: {threatLevel} 
Enemies: {enemyTypes}
Available Weapons: {weaponOptions}
Allies Present: {nearbyAllies}
Relationship Context: {relationshipSummary}

Decide: ATTACK/DEFEND/FLEE/NEGOTIATE
Target Priority: [enemy IDs in order]
Weapon Choice: {selectedWeapon}
Tactical Notes: [brief reasoning]

Keep response under 100 tokens.
"""
```

#### Data Structures
```java
public class CombatDecision {
    private CombatAction action; // ATTACK, DEFEND, FLEE, NEGOTIATE
    private List<UUID> targetPriority;
    private ItemStack preferredWeapon;
    private String tacticalReasoning;
    private float confidenceScore;
}

public class CombatMemoryData {
    private Map<UUID, Integer> enemyEncounterHistory;
    private Map<CombatSituation.Type, CombatDecision> learnedStrategies;
    private long lastAIDecisionTimestamp;
}
```

### 2. Village Expansion Manager AI Enhancement

#### Extended [`VillageExpansionManager`](common/src/main/java/com/beeny/villagesreborn/core/expansion/VillageExpansionManager.java:3)

**AI Integration Points:**
```java
public class VillageExpansionManagerAI extends VillageExpansionManager {
    private final ExpansionAIEngine expansionAI;
    private final TerrainAnalyzer terrainAnalyzer;
    private final BiomeExpansionStrategist biomeStrategist;
    
    public ExpansionProposal generateAIExpansionPlan(
        VillageResources resources,
        TerrainData terrainData,
        BiomeData biomeData,
        PopulationGrowthTrends trends
    ) {
        // AI-driven expansion strategy
    }
}
```

**Pseudocode for AI Expansion:**
```
FUNCTION generateExpansionStrategy(village, worldState):
    // Gather expansion context
    terrainData = analyzeNearbyTerrain(village.location, 64blocks)
    biomeData = getBiomeCharacteristics(village.biome)
    resourceState = assessResourceAvailability(village)
    populationTrends = analyzeGrowthPatterns(village.history)
    
    // Build AI prompt
    expansionPrompt = buildExpansionPrompt(
        terrainData, biomeData, resourceState, populationTrends
    )
    
    // Get AI recommendation
    TRY:
        aiResponse = llmClient.generateExpansionStrategy(expansionPrompt, 200ms)
        expansionPlan = parseExpansionResponse(aiResponse)
        
        // Validate against game constraints
        IF validateExpansionPlan(expansionPlan, worldState):
            RETURN mergeWithProceduralPlacement(expansionPlan)
    CATCH Exception:
        // Fallback to procedural generation
        
    RETURN generateProceduralExpansion(village, worldState)
```

#### AI Prompt for Expansion Planning
```
EXPANSION_PROMPT_TEMPLATE = """
Village Expansion Analysis:

Current State:
- Population: {population}
- Resources: Wood={wood}, Stone={stone}, Food={food}
- Biome: {biome} (characteristics: {biomeTraits})
- Terrain: {terrainAnalysis}

Expansion Goals:
- Housing Demand: {housingNeeded}
- Economic Growth: {economicPriorities}
- Defense Considerations: {defensiveNeeds}

Recommend:
1. Building Types: [house/workshop/farm/defense] with quantities
2. Placement Strategy: [compact/distributed/linear/defensive]
3. Resource Priority: [which resources to focus on]
4. Timeline: [expansion phases]

Response format: JSON with buildingPlan, placementStrategy, resourceFocus
"""
```

#### Data Structures
```java
public class ExpansionProposal {
    private List<BuildingRequest> buildingQueue;
    private PlacementStrategy strategy;
    private ResourceAllocation resourcePlan;
    private List<ExpansionPhase> phases;
    private String aiReasoning;
}

public class BuildingRequest {
    private String buildingType;
    private int quantity;
    private Priority priority;
    private BlockPos preferredLocation;
    private List<String> requirements;
}

public class ExpansionMemoryData {
    private Map<BiomeType, ExpansionStrategy> learnedStrategies;
    private List<ExpansionOutcome> historicalResults;
    private Map<String, Float> buildingSuccessRates;
}
```

### 3. Governance Election AI (`ElectionAIManager`)

#### Enhanced [`ElectionManager`](common/src/main/java/com/beeny/villagesreborn/core/governance/ElectionManager.java:8) with AI

**AI-Generated Candidate System:**
```java
public class ElectionAIManager extends ElectionManager {
    private final CandidatePersonaGenerator candidateAI;
    private final DebateSimulator debateAI;
    private final PolicyGenerator policyAI;
    
    public List<AICandidate> generateCandidates(
        VillageContext context, 
        int candidateCount
    );
    
    public DebateResult simulateDebate(
        List<Candidate> candidates, 
        List<PolicyIssue> issues
    );
    
    public List<PolicyProposal> generateCampaignPolicies(
        Candidate candidate, 
        VillageNeeds needs
    );
}
```

**Pseudocode for AI Elections:**
```
FUNCTION runAIEnhancedElection(village, electionId):
    // Generate AI candidates
    villageNeeds = analyzeVillageNeeds(village)
    candidates = generateAICandidates(villageNeeds, 3-5)
    
    // Add existing resident candidates
    residentCandidates = getResidentCandidates(village)
    allCandidates = merge(candidates, residentCandidates)
    
    // Generate campaign policies
    FOR candidate IN allCandidates:
        policies = generatePolicies(candidate.persona, villageNeeds)
        candidate.campaignPlatform = policies
    
    // Simulate debates and town halls
    IF electionConfig.enableDebates:
        debateResults = simulateDebates(allCandidates, villageNeeds)
        updateCandidateSupport(allCandidates, debateResults)
    
    // Use ConversationRouter for Q&A sessions
    FOR townHallSession IN scheduledSessions:
        qAndA = conversationRouter.simulateTownHall(
            allCandidates, villageNeeds, residentQuestions
        )
        updateCandidateApproval(allCandidates, qAndA)
    
    // Store AI-generated content in ElectionData
    electionData.setCandidates(allCandidates)
    electionData.setDebateTranscripts(debateResults)
    electionData.setPolicyProposals(extractPolicies(allCandidates))
    
    RETURN electionData
```

#### AI Prompt Templates for Governance

**Candidate Generation:**
```
CANDIDATE_GENERATION_PROMPT = """
Generate a mayoral candidate for a Minecraft village:

Village Context:
- Population: {population}
- Primary Issues: {topIssues}
- Economic Status: {economicHealth}
- Recent Events: {recentHistory}

Create a candidate with:
1. Name and Background
2. Key Personality Traits (3-4 traits)
3. Campaign Platform (3 main policy points)
4. Speaking Style (formal/casual/passionate/practical)
5. Stance on Major Issues: {issueList}

Format as JSON with name, background, traits, platform, speakingStyle, stances
"""
```

**Debate Simulation:**
```
DEBATE_PROMPT_TEMPLATE = """
Moderate a mayoral debate between candidates:

Candidates:
{candidateProfiles}

Debate Topic: {currentTopic}
Previous Exchanges: {debateHistory}

Generate next exchange:
- Current Speaker: {currentSpeaker}
- Response to: {previousStatement}
- Time Limit: 30 seconds

Requirements:
- Stay in character for {currentSpeaker}
- Address the topic directly
- Reference previous statements if relevant
- Show personality through speaking style

Response: [candidate statement in character]
"""
```

#### Data Structures
```java
public class AICandidate extends Candidate {
    private PersonaProfile aiPersona;
    private SpeakingStyle speakingStyle;
    private Map<PolicyIssue, PolicyStance> platformStances;
    private List<String> campaignPromises;
    private String generationSeed; // For consistency
}

public class DebateResult {
    private Map<UUID, List<String>> candidateStatements;
    private Map<UUID, Float> performanceScores;
    private List<PolicyPoint> emergentIssues;
    private String debateTranscript;
}

public class ElectionMemoryData {
    private List<AICandidate> historicalCandidates;
    private Map<PolicyType, Float> successfulPolicyPatterns;
    private List<DebateResult> pastDebates;
    private Map<String, String> personalityTemplates;
}
```

### 4. Data Persistence & Migration

#### Extended [`VillagesRebornWorldDataPersistent`](fabric/src/main/java/com/beeny/villagesreborn/core/world/VillagesRebornWorldDataPersistent.java:16)

**New Data Fields:**
```java
public class VillagesRebornWorldDataPersistent extends PersistentState {
    // Existing fields...
    
    // Phase 5 AI Memory Fields
    private CombatMemoryData combatAIMemory;
    private ExpansionMemoryData expansionAIMemory;
    private ElectionMemoryData governanceAIMemory;
    private AIConfigurationData aiConfig;
}
```

**Migration Logic:**
```
FUNCTION migrateToPhase5(existingWorldData):
    // Preserve existing data
    preservedData = existingWorldData.getAllData()
    
    // Initialize AI memory structures
    combatMemory = new CombatMemoryData()
    expansionMemory = new ExpansionMemoryData()
    governanceMemory = new ElectionMemoryData()
    
    // Migrate rule-based data to AI context
    IF existingWorldData.hasCombatHistory():
        combatMemory.importFromRuleBasedHistory(
            existingWorldData.getCombatHistory()
        )
    
    IF existingWorldData.hasExpansionHistory():
        expansionMemory.importFromExpansionLogs(
            existingWorldData.getExpansionHistory()
        )
    
    IF existingWorldData.hasElectionHistory():
        governanceMemory.importFromElectionRecords(
            existingWorldData.getElectionHistory()
        )
    
    // Create upgraded data structure
    upgradedData = new VillagesRebornWorldDataPersistent(preservedData)
    upgradedData.setCombatAIMemory(combatMemory)
    upgradedData.setExpansionAIMemory(expansionMemory)
    upgradedData.setGovernanceAIMemory(governanceMemory)
    upgradedData.setDataVersion("phase5_ai_integration")
    
    RETURN upgradedData
```

#### NBT Serialization Extensions
```java
// Add to updateSetting method in VillagesRebornWorldDataPersistent
case "ai_combat_memory_enabled":
    if (value instanceof Boolean) {
        settings.setAICombatMemoryEnabled((Boolean) value);
    }
    break;
case "ai_expansion_planning_enabled":
    if (value instanceof Boolean) {
        settings.setAIExpansionPlanningEnabled((Boolean) value);
    }
    break;
case "ai_governance_candidates_enabled":
    if (value instanceof Boolean) {
        settings.setAIGovernanceCandidatesEnabled((Boolean) value);
    }
    break;
case "ai_decision_timeout_ms":
    if (value instanceof Number) {
        settings.setAIDecisionTimeoutMs(((Number) value).intValue());
    }
    break;
```

### 5. Integration Interfaces

#### AI Decision Coordinator
```java
public class AIDecisionCoordinator {
    private final CombatAIEngine combatAI;
    private final ExpansionAIEngine expansionAI;
    private final ElectionAIManager governanceAI;
    private final PromptBuilder promptBuilder;
    private final LLMApiClient llmClient;
    
    public <T> CompletableFuture<T> coordinateAIDecision(
        AIDecisionRequest<T> request,
        long timeoutMs,
        Supplier<T> fallbackSupplier
    ) {
        // Centralized AI decision coordination with fallback
    }
}
```

#### Enhanced [`PromptBuilder`](common/src/main/java/com/beeny/villagesreborn/core/ai/PromptBuilder.java:11) Extensions
```java
public class PromptBuilder {
    // Existing conversation prompt methods...
    
    // New AI decision prompt methods
    public static String buildCombatPrompt(
        CombatSituation situation,
        CombatPersonalityTraits traits,
        ThreatAssessment threat,
        RelationshipData relationships
    );
    
    public static String buildExpansionPrompt(
        VillageResources resources,
        TerrainData terrain,
        BiomeData biome,
        PopulationGrowthTrends trends
    );
    
    public static String buildGovernancePrompt(
        ElectionContext context,
        VillageNeeds needs,
        CandidateRequirements requirements
    );
}
```

## Test-Driven Development Plan

### Unit Tests

#### [`CombatDecisionEngineTest`](common/src/test/java/com/beeny/villagesreborn/core/combat/CombatDecisionLogicTests.java:7) Extensions
```java
@Test
public void testAICombatDecisionWithTimeout() {
    // Given: combat situation requiring AI analysis
    CombatSituation complexSituation = createComplexCombatScenario();
    CombatPersonalityTraits traits = createIndecisiveTraits();
    
    // When: AI decision times out
    CompletableFuture<CombatDecision> slowAI = createTimeoutAI(500);
    CombatDecision result = combatEngine.decideCombatAction(
        complexSituation, traits, 150
    );
    
    // Then: fallback to rule-based decision
    assertNotNull(result);
    assertEquals(CombatAction.DEFEND, result.getAction()); // Expected fallback
    assertTrue(result.isFallbackDecision());
}

@Test
public void testAICombatDecisionValidation() {
    // Given: AI returns invalid decision
    CombatDecision invalidDecision = createInvalidDecision();
    
    // When: validating AI decision
    boolean isValid = combatEngine.validateAIDecision(
        invalidDecision, createValidSituation()
    );
    
    // Then: validation fails and triggers fallback
    assertFalse(isValid);
}

@Test
public void testCombatMemoryPersistence() {
    // Given: villager with combat history
    VillagerEntity villager = createVillagerWithCombatHistory();
    
    // When: saving and loading combat memory
    CombatMemoryData memory = villager.getCombatMemory();
    NBTCompound nbt = memory.serializeToNBT();
    CombatMemoryData loaded = CombatMemoryData.fromNBT(nbt);
    
    // Then: memory preserved correctly
    assertEquals(memory.getEnemyEncounterHistory(), loaded.getEnemyEncounterHistory());
    assertEquals(memory.getLearnedStrategies(), loaded.getLearnedStrategies());
}
```

#### [`VillageExpansionManagerTest`](common/src/test/java/com/beeny/villagesreborn/core/expansion/VillageExpansionManagerTests.java:7) Extensions
```java
@Test
public void testAIExpansionPlanGeneration() {
    // Given: village ready for expansion
    VillageResources resources = createResourceRichVillage();
    TerrainData terrain = createVariedTerrain();
    BiomeData biome = BiomeData.PLAINS;
    
    // When: generating AI expansion plan
    ExpansionProposal aiPlan = expansionManager.generateAIExpansionPlan(
        resources, terrain, biome, createGrowthTrends()
    );
    
    // Then: plan is valid and optimized
    assertNotNull(aiPlan);
    assertTrue(aiPlan.getBuildingQueue().size() > 0);
    assertTrue(aiPlan.getResourcePlan().isAffordable(resources));
    assertEquals(PlacementStrategy.DEFENSIVE, aiPlan.getStrategy()); // Expected for plains
}

@Test
public void testBiomeSpecificExpansionStrategies() {
    // Test AI generates different strategies for different biomes
    List<BiomeData> biomes = Arrays.asList(DESERT, FOREST, MOUNTAIN, SWAMP);
    
    for (BiomeData biome : biomes) {
        ExpansionProposal plan = generateExpansionForBiome(biome);
        assertBiomeAppropriate(plan, biome);
    }
}

@Test
public void testExpansionFallbackToProcedural() {
    // Given: AI unavailable
    when(llmClient.isAvailable()).thenReturn(false);
    
    // When: requesting expansion plan
    ExpansionProposal plan = expansionManager.generateExpansionPlan(village);
    
    // Then: procedural generation used
    assertTrue(plan.isProceduralGenerated());
    assertValidPlan(plan);
}
```

#### [`ElectionManagerTest`](common/src/test/java/com/beeny/villagesreborn/core/governance/ElectionManagerTests.java:7) Extensions
```java
@Test
public void testAICandidateGeneration() {
    // Given: village with specific needs
    VillageContext context = createVillageWithEconomicIssues();
    
    // When: generating AI candidates
    List<AICandidate> candidates = electionManager.generateCandidates(context, 3);
    
    // Then: candidates address village needs
    assertEquals(3, candidates.size());
    assertTrue(candidates.stream().anyMatch(c -> 
        c.getPlatform().addresses(PolicyIssue.ECONOMIC_GROWTH)
    ));
    assertTrue(allCandidatesHaveUniquePersonalities(candidates));
}

@Test
public void testDebateSimulation() {
    // Given: candidates with different platforms
    List<Candidate> candidates = createDiverseCandidates();
    List<PolicyIssue> issues = Arrays.asList(DEFENSE, EXPANSION, TRADE);
    
    // When: simulating debate
    DebateResult result = electionManager.simulateDebate(candidates, issues);
    
    // Then: coherent debate generated
    assertNotNull(result.getDebateTranscript());
    assertTrue(result.getPerformanceScores().size() == candidates.size());
    assertTrue(allIssuesAddressed(result, issues));
}

@Test
public void testConversationRouterIntegration() {
    // Given: town hall Q&A session
    List<Candidate> candidates = createCandidates();
    List<String> residentQuestions = createResidentQuestions();
    
    // When: routing Q&A through ConversationRouter
    TownHallResult result = conversationRouter.simulateTownHall(
        candidates, residentQuestions
    );
    
    // Then: candidates respond in character
    for (Candidate candidate : candidates) {
        List<String> responses = result.getResponses(candidate.getId());
        assertTrue(responses.size() > 0);
        assertTrue(responsesMatchPersonality(responses, candidate.getPersona()));
    }
}
```

### Integration Tests

#### End-to-End AI Decision Flow
```java
@Test
public void testCompleteAICombatCycle() {
    // Given: villager in combat situation
    VillagerEntity villager = createVillagerInCombat();
    CombatSituation situation = createMultiEnemySituation();
    
    // When: processing complete combat tick with AI
    when(llmClient.isAvailable()).thenReturn(true);
    when(llmClient.generateConversationResponse(any())).thenReturn(
        createValidCombatResponse()
    );
    
    CombatTickResult result = combatEngine.processCombatTick(villager, situation);
    
    // Then: AI decision executed and memory updated
    assertTrue(result.usedAI());
    assertEquals(CombatAction.ATTACK, result.getAction());
    assertNotNull(villager.getCombatMemory().getLastAIDecision());
}

@Test
public void testCrossSystemIntegration() {
    // Test AI decisions affecting multiple systems
    // e.g., combat outcome affecting expansion plans affecting governance
}
```

### Performance Tests

#### Latency Requirements
```java
@Test
public void testAIDecisionLatencyUnderLoad() {
    // Given: multiple concurrent AI decision requests
    int concurrentRequests = 10;
    List<CompletableFuture<Long>> latencyTests = new ArrayList<>();
    
    for (int i = 0; i < concurrentRequests; i++) {
        latencyTests.add(measureAIDecisionLatency());
    }
    
    // When: all requests complete
    List<Long> latencies = latencyTests.stream()
        .map(CompletableFuture::join)
        .collect(Collectors.toList());
    
    // Then: all decisions under 200ms
    long maxLatency = latencies.stream().mapToLong(Long::longValue).max().orElse(0);
    assertTrue("Max latency " + maxLatency + "ms exceeds 200ms", maxLatency < 200);
    
    long avgLatency = (long) latencies.stream().mapToLong(Long::longValue).average().orElse(0);
    assertTrue("Average latency " + avgLatency + "ms exceeds 150ms", avgLatency < 150);
}

@Test
public void testMemoryUsageUnderContinuousAI() {
    // Test AI memory doesn't grow unbounded
    long initialMemory = measureMemoryUsage();
    
    // Run 1000 AI decisions
    for (int i = 0; i < 1000; i++) {
        processAIDecision();
    }
    
    long finalMemory = measureMemoryUsage();
    long memoryGrowth = finalMemory - initialMemory;
    
    assertTrue("Memory growth " + memoryGrowth + " bytes exceeds limit", 
               memoryGrowth < 50_000_000); // 50MB limit
}
```

### Headless Simulation Tests

#### Village Lifecycle with AI
```java
@Test
public void testCompleteVillageSimulationWithAI() {
    // Given: new village with AI enabled
    Village testVillage = createTestVillage();
    testVillage.enableAI(true);
    
    // When: simulating 100 game days
    VillageSimulator simulator = new VillageSimulator(testVillage);
    SimulationResult result = simulator.simulate(100 * 24000); // 100 days in ticks
    
    // Then: village shows AI-driven growth patterns
    assertTrue("Village population should grow", result.getFinalPopulation() > result.getInitialPopulation());
    assertTrue("Combat encounters should show learning", result.getCombatEfficiency() > 0.7f);
    assertTrue("Expansion should be strategic", result.getExpansionEfficiency() > 0.8f);
    assertTrue("Elections should generate diverse candidates", result.getElectionDiversity() > 0.6f);
}
```

## Error Handling & Fallback Strategies

### AI Unavailability Handling
```
GLOBAL_AI_FALLBACK_STRATEGY:
    1. Network/API Failure -> Rule-based immediate fallback
    2. Timeout -> Cached decision or rule-based
    3. Invalid Response -> Validation failure triggers rule-based
    4. Rate Limiting -> Queue decision or rule-based with exponential backoff
    5. API Key Issues -> Disable AI temporarily, log warning
```

### Data Corruption Recovery
```
AI_MEMORY_RECOVERY_PROTOCOL:
    1. Detect corrupted AI memory during load
    2. Attempt partial recovery of valid data segments
    3. Initialize fresh AI memory if corruption severe
    4. Log corruption details for debugging
    5. Continue with rule-based systems until memory rebuilds
```

## Performance Optimization

### Caching Strategy
```java
public class AIDecisionCache {
    private final LoadingCache<CombatSituationKey, CombatDecision> combatCache;
    private final LoadingCache<ExpansionContextKey, ExpansionProposal> expansionCache;
    private final LoadingCache<ElectionContextKey, List<AICandidate>> candidateCache;
    
    // Cache similar situations to reduce AI calls
    // TTL: 5 minutes for combat, 30 minutes for expansion, 24 hours for candidates
}
```

### Batch Processing
```
BATCH_AI_PROCESSING:
    // Group similar requests to reduce API calls
    IF multiple_villagers_in_combat():
        batch_combat_situations = group_by_similarity(all_combat_situations)
        FOR batch IN batch_combat_situations:
            batch_decision = process_batch_ai_decision(batch)
            distribute_decisions(batch_decision, batch.villagers)
```

## Configuration & Settings

### AI Feature Toggles
```java
public class AIConfiguration {
    @ConfigField("ai.combat.enabled")
    private boolean combatAIEnabled = true;
    
    @ConfigField("ai.expansion.enabled") 
    private boolean expansionAIEnabled = true;
    
    @ConfigField("ai.governance.enabled")
    private boolean governanceAIEnabled = true;
    
    @ConfigField("ai.decision.timeout_ms")
    private int decisionTimeoutMs = 150;
    
    @ConfigField("ai.fallback.always_available")
    private boolean fallbackAlwaysAvailable = true;
    
    @ConfigField("ai.cache.enabled")
    private boolean cacheEnabled = true;
    
    @ConfigField("ai.batch_processing.enabled")
    private boolean batchProcessingEnabled = false;
}
```

## Security Considerations

### AI Input Validation
```java
public class AIInputValidator {
    public boolean validateCombatSituation(CombatSituation situation) {
        // Ensure situation data is within expected bounds
        // Prevent injection attacks through malformed data
    }
    
    public boolean validateAIResponse(String response, AIDecisionType type) {
        // Validate AI response format and content
        // Ensure response doesn't contain harmful instructions
    }
}
```

### Rate Limiting
```java
public class AIRateLimiter {
    private final Map<VillagerEntity, TokenBucket> villagerLimits;
    private final TokenBucket globalLimit;
    
    public boolean canMakeAIRequest(VillagerEntity villager) {
        return villagerLimits.get(villager).tryConsume(1) && 
               globalLimit.tryConsume(1);
    }
}
```

## Dependencies

### Required Libraries
- **Existing**: [`LLMApiClient`](common/src/main/java/com/beeny/villagesreborn/core/llm/LLMApiClient.java:8), [`PromptBuilder`](common/src/main/java/com/beeny/villagesreborn/core/ai/PromptBuilder.java:11), [`ConversationRouter`](common/src/main/java/com/beeny/villagesreborn/core/conversation/ConversationRouter.java:9)
- **Extended**: [`CombatDecisionEngine`](common/src/main/java/com/beeny/villagesreborn/core/combat/CombatDecisionEngine.java:13), [`VillageExpansionManager`](common/src/main/java/com/beeny/villagesreborn/core/expansion/VillageExpansionManager.java:3), [`ElectionManager`](common/src/main/java/com/beeny/villagesreborn/core/governance/ElectionManager.java:8)
- **New**: AI decision engines, memory persistence, prompt templates

### Module Size Constraints
- Each AI engine implementation: <500 lines
- Prompt template files: <200 lines each  
- Memory data structures: <300 lines each
- Test suites: <400 lines per component

## Deployment & Migration

### Phase 5 Rollout Strategy
1. **Week 1**: Implement AI decision engines with fallbacks
2. **Week 2**: Add expansion and governance AI components  
3. **Week 3**: Integrate data persistence and migration
4. **Week 4**: Performance testing and optimization
5. **Week 5**: Integration testing and bug fixes

### Backward Compatibility
- Existing worlds continue working with rule-based systems
- AI features opt-in through configuration
- Migration preserves all existing data
- Graceful degradation if AI components disabled

## Success Metrics

### Performance KPIs
- AI decision latency: <200ms P95, <150ms P50
- Fallback frequency: <5% under normal conditions
- Memory usage growth: <50MB per 24-hour session
- Test coverage: >90% for all new AI components

### Functionality KPIs  
- Combat effectiveness: 15% improvement with AI vs rule-based
- Expansion efficiency: 20% better resource utilization
- Election diversity: 3+ unique candidate archetypes per election
- Player satisfaction: Measured through village growth and engagement

This specification provides the foundation for implementing AI-enhanced combat, expansion, and governance systems while maintaining robust fallback mechanisms and comprehensive test coverage.