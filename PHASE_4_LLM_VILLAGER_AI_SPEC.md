# Phase 4: LLM-Powered Villager AI - Technical Specification

## Overview
This specification outlines the implementation of intelligent villager AI powered by Large Language Models (LLMs). The system intercepts player chat, routes messages to appropriate villagers, generates contextual responses using LLMs, and maintains persistent villager memory and personality.

## Architecture Components

### 1. Chat Interception & Routing

#### 1.1 ChatEventHandler
**Purpose**: Primary entry point for all chat events, filtering and routing AI-triggering messages.

**Pseudocode**:
```java
class ChatEventHandler {
    private ConversationRouter conversationRouter;
    private List<Pattern> aiTriggerPatterns;
    
    void onServerChatEvent(ServerChatEvent event) {
        String message = event.getMessage();
        Player sender = event.getSender();
        
        if (shouldTriggerAI(message)) {
            ConversationContext context = new ConversationContext(
                sender, message, event.getTimestamp(), event.getWorld()
            );
            conversationRouter.routeConversation(context);
        }
    }
    
    boolean shouldTriggerAI(String message) {
        return aiTriggerPatterns.stream()
            .anyMatch(pattern -> pattern.matcher(message).find());
    }
    
    void updateTriggerPatterns(List<String> patterns) {
        this.aiTriggerPatterns = patterns.stream()
            .map(Pattern::compile)
            .collect(toList());
    }
}
```

#### 1.2 ConversationRouter
**Purpose**: Routes validated conversations to appropriate villagers and manages response flow.

**Pseudocode**:
```java
class ConversationRouter {
    private VillagerProximityDetector proximityDetector;
    private VillagerBrainManager brainManager;
    private ResponseDeliveryManager responseManager;
    private ExecutorService conversationExecutor;
    
    void routeConversation(ConversationContext context) {
        List<VillagerEntity> nearbyVillagers = proximityDetector
            .findNearbyVillagers(context.getSender(), CONVERSATION_RANGE);
        
        if (nearbyVillagers.isEmpty()) {
            return;
        }
        
        // Process each villager asynchronously to prevent blocking
        for (VillagerEntity villager : nearbyVillagers) {
            conversationExecutor.submit(() -> {
                processVillagerResponse(villager, context);
            });
        }
    }
    
    private void processVillagerResponse(VillagerEntity villager, ConversationContext context) {
        try {
            VillagerBrain brain = brainManager.getBrain(villager);
            String response = generateResponse(brain, context);
            
            if (response != null && !response.trim().isEmpty()) {
                responseManager.deliverResponse(villager, context.getSender(), response);
                brain.addInteraction(context.getSender(), context.getMessage(), response);
                brainManager.saveBrain(villager, brain);
            }
        } catch (Exception e) {
            logError("Failed to process villager response", e);
        }
    }
}
```

### 2. Prompt Construction & LLM Invocation

#### 2.1 Enhanced PromptBuilder
**Purpose**: Constructs contextual prompts incorporating villager personality, mood, and history.

**Pseudocode**:
```java
class PromptBuilder {
    private static final int MAX_HISTORY_TOKENS = 1000;
    private static final String SYSTEM_PROMPT_TEMPLATE = """
        You are {villagerName}, a {profession} villager in a Minecraft village.
        Personality: {personalityTraits}
        Current mood: {moodState} ({moodReason})
        Relationship with {playerName}: {relationshipLevel}
        
        Guidelines:
        - Respond as this specific villager would
        - Keep responses brief (1-2 sentences)
        - Reference past interactions when relevant
        - Express personality through speech patterns
        """;
    
    ConversationRequest buildPrompt(VillagerBrain brain, ConversationContext context) {
        String systemPrompt = buildSystemPrompt(brain, context);
        String userPrompt = buildUserPrompt(context);
        List<String> conversationHistory = buildConversationHistory(brain, context.getSender());
        
        return ConversationRequest.builder()
            .systemPrompt(systemPrompt)
            .userPrompt(userPrompt)
            .conversationHistory(conversationHistory)
            .maxTokens(150)
            .temperature(0.8f)
            .build();
    }
    
    private String buildSystemPrompt(VillagerBrain brain, ConversationContext context) {
        PersonalityProfile personality = brain.getPersonalityProfile();
        MoodState mood = brain.getCurrentMood();
        RelationshipData relationship = brain.getRelationshipWith(context.getSender());
        
        return SYSTEM_PROMPT_TEMPLATE
            .replace("{villagerName}", brain.getVillagerName())
            .replace("{profession}", brain.getProfession())
            .replace("{personalityTraits}", formatPersonalityTraits(personality))
            .replace("{moodState}", mood.getCategory().toString())
            .replace("{moodReason}", mood.getReason())
            .replace("{playerName}", context.getSender().getName())
            .replace("{relationshipLevel}", relationship.getLevel().toString());
    }
    
    private List<String> buildConversationHistory(VillagerBrain brain, Player player) {
        ConversationHistory history = brain.getConversationHistory();
        List<ConversationInteraction> recentInteractions = history
            .getInteractionsWith(player)
            .stream()
            .sorted(comparing(ConversationInteraction::getTimestamp).reversed())
            .limit(10)
            .collect(toList());
        
        return trimHistoryToTokenLimit(recentInteractions, MAX_HISTORY_TOKENS);
    }
}
```

#### 2.2 Enhanced LLMApiClient
**Purpose**: Handles LLM API communication with retry logic, streaming, and error handling.

**Pseudocode**:
```java
class LLMApiClientImpl implements LLMApiClient {
    private HttpClient httpClient;
    private LLMProviderManager providerManager;
    private RetryPolicy retryPolicy;
    
    CompletableFuture<ConversationResponse> sendRequest(ConversationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            LLMProvider provider = providerManager.getActiveProvider();
            
            return retryPolicy.execute(() -> {
                HttpRequest httpRequest = buildHttpRequest(provider, request);
                HttpResponse<String> response = httpClient.send(httpRequest, 
                    HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() != 200) {
                    throw new LLMApiException("API call failed: " + response.statusCode());
                }
                
                return parseResponse(response.body());
            });
        });
    }
    
    private HttpRequest buildHttpRequest(LLMProvider provider, ConversationRequest request) {
        String requestBody = buildRequestBody(provider, request);
        
        return HttpRequest.newBuilder()
            .uri(URI.create(provider.getApiEndpoint()))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + provider.getApiKey())
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .timeout(Duration.ofSeconds(30))
            .build();
    }
    
    private String buildRequestBody(LLMProvider provider, ConversationRequest request) {
        // Provider-specific request formatting
        switch (provider.getType()) {
            case OPENAI:
                return buildOpenAIRequest(request);
            case ANTHROPIC:
                return buildAnthropicRequest(request);
            case LOCAL:
                return buildLocalRequest(request);
            default:
                throw new UnsupportedOperationException("Provider not supported: " + provider.getType());
        }
    }
}
```

### 3. VillagerBrain & Memory Management

#### 3.1 Enhanced VillagerBrain
**Purpose**: Core AI state management with personality, mood, and memory persistence.

**Pseudocode**:
```java
class VillagerBrain {
    private UUID villagerId;
    private PersonalityProfile personalityProfile;
    private MoodState currentMood;
    private ConversationHistory conversationHistory;
    private MemoryBank memoryBank;
    private Map<UUID, RelationshipData> relationships;
    
    void updateMood(MoodCategory category, String reason, int intensity) {
        MoodState previousMood = this.currentMood;
        this.currentMood = new MoodState(category, reason, intensity, System.currentTimeMillis());
        
        // Mood affects personality expression
        personalityProfile.adjustForMood(currentMood);
        
        // Log mood change for context
        memoryBank.addMemory(MemoryType.MOOD_CHANGE, 
            "Mood changed from " + previousMood + " to " + currentMood + " because: " + reason);
    }
    
    void addInteraction(Player player, String playerMessage, String villagerResponse) {
        ConversationInteraction interaction = new ConversationInteraction(
            player.getUUID(), playerMessage, villagerResponse, System.currentTimeMillis()
        );
        
        conversationHistory.addInteraction(interaction);
        updateRelationship(player, interaction);
        
        // Trim old history to prevent memory bloat
        if (conversationHistory.size() > MAX_HISTORY_SIZE) {
            conversationHistory.trimOldest(MAX_HISTORY_SIZE / 2);
        }
    }
    
    private void updateRelationship(Player player, ConversationInteraction interaction) {
        RelationshipData relationship = relationships.computeIfAbsent(
            player.getUUID(), 
            id -> new RelationshipData(id, RelationshipLevel.NEUTRAL)
        );
        
        // Analyze interaction sentiment and adjust relationship
        int sentimentScore = analyzeSentiment(interaction.getPlayerMessage());
        relationship.adjustLevel(sentimentScore);
        
        relationship.incrementInteractionCount();
        relationship.setLastInteraction(interaction.getTimestamp());
    }
    
    NBTCompound serializeToNBT() {
        NBTCompound nbt = new NBTCompound();
        nbt.putUUID("villagerId", villagerId);
        nbt.put("personality", personalityProfile.serializeToNBT());
        nbt.put("mood", currentMood.serializeToNBT());
        nbt.put("history", conversationHistory.serializeToNBT());
        nbt.put("memory", memoryBank.serializeToNBT());
        nbt.put("relationships", serializeRelationships());
        return nbt;
    }
}
```

#### 3.2 VillagerBrainManager
**Purpose**: Manages brain lifecycle, persistence, and retrieval across world sessions.

**Pseudocode**:
```java
class VillagerBrainManager {
    private Map<UUID, VillagerBrain> activeBrains;
    private VillagesRebornWorldDataPersistent worldData;
    private ScheduledExecutorService persistenceExecutor;
    
    VillagerBrain getBrain(VillagerEntity villager) {
        UUID villagerId = villager.getUUID();
        
        return activeBrains.computeIfAbsent(villagerId, id -> {
            VillagerBrain brain = loadBrainFromNBT(id);
            if (brain == null) {
                brain = createNewBrain(villager);
            }
            return brain;
        });
    }
    
    void saveBrain(VillagerEntity villager, VillagerBrain brain) {
        UUID villagerId = villager.getUUID();
        activeBrains.put(villagerId, brain);
        
        // Async persistence to prevent blocking
        persistenceExecutor.schedule(() -> {
            try {
                NBTCompound brainNBT = brain.serializeToNBT();
                worldData.setVillagerBrain(villagerId, brainNBT);
                worldData.markDirty();
            } catch (Exception e) {
                logError("Failed to persist villager brain: " + villagerId, e);
            }
        }, 1, TimeUnit.SECONDS);
    }
    
    private VillagerBrain createNewBrain(VillagerEntity villager) {
        PersonalityProfile personality = PersonalityGenerator.generateRandomPersonality();
        MoodState initialMood = new MoodState(MoodCategory.NEUTRAL, "Just created", 5, System.currentTimeMillis());
        
        return new VillagerBrain(
            villager.getUUID(),
            personality,
            initialMood,
            new ConversationHistory(),
            new MemoryBank(),
            new HashMap<>()
        );
    }
    
    void unloadInactiveB brains() {
        long cutoffTime = System.currentTimeMillis() - BRAIN_UNLOAD_TIMEOUT;
        
        activeBrains.entrySet().removeIf(entry -> {
            VillagerBrain brain = entry.getValue();
            return brain.getLastActivity() < cutoffTime;
        });
    }
}
```

### 4. Proximity Detection & Triggering

#### 4.1 VillagerProximityDetector
**Purpose**: Efficiently detects villagers within conversation range of players.

**Pseudocode**:
```java
class VillagerProximityDetector {
    private static final double CONVERSATION_RANGE = 8.0;
    private static final double CONVERSATION_RANGE_SQUARED = CONVERSATION_RANGE * CONVERSATION_RANGE;
    
    List<VillagerEntity> findNearbyVillagers(Player player, double range) {
        BlockPos playerPos = player.getBlockPos();
        double rangeSquared = range * range;
        
        return player.getWorld()
            .getEntitiesOfType(VillagerEntity.class, 
                entity -> isWithinRange(playerPos, entity.getBlockPos(), rangeSquared))
            .stream()
            .filter(this::isValidConversationTarget)
            .sorted(comparing(villager -> distanceSquared(playerPos, villager.getBlockPos())))
            .collect(toList());
    }
    
    private boolean isWithinRange(BlockPos pos1, BlockPos pos2, double rangeSquared) {
        return distanceSquared(pos1, pos2) <= rangeSquared;
    }
    
    private double distanceSquared(BlockPos pos1, BlockPos pos2) {
        double dx = pos1.getX() - pos2.getX();
        double dy = pos1.getY() - pos2.getY();
        double dz = pos1.getZ() - pos2.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
    
    private boolean isValidConversationTarget(VillagerEntity villager) {
        return villager.isAlive() 
            && !villager.isSleeping() 
            && !villager.isInCombat()
            && villager.canSee(player);
    }
    
    // Optimized version using spatial indexing for large villages
    List<VillagerEntity> findNearbyVillagersOptimized(Player player, double range) {
        BlockPos playerPos = player.getBlockPos();
        
        // Use chunk-based lookup for better performance
        Set<ChunkPos> nearbyChunks = getNearbyChunks(playerPos, range);
        
        return nearbyChunks.stream()
            .flatMap(chunkPos -> getVillagersInChunk(chunkPos).stream())
            .filter(villager -> isWithinRange(playerPos, villager.getBlockPos(), range * range))
            .filter(this::isValidConversationTarget)
            .sorted(comparing(villager -> distanceSquared(playerPos, villager.getBlockPos())))
            .collect(toList());
    }
}
```

### 5. Response Delivery

#### 5.1 ResponseDeliveryManager
**Purpose**: Handles delivery of AI responses with rate limiting and formatting.

**Pseudocode**:
```java
class ResponseDeliveryManager {
    private RateLimiter globalRateLimiter;
    private Map<UUID, RateLimiter> playerRateLimiters;
    private ChatFormatter chatFormatter;
    
    void deliverResponse(VillagerEntity villager, Player targetPlayer, String response) {
        UUID playerId = targetPlayer.getUUID();
        
        // Check global and per-player rate limits
        if (!globalRateLimiter.tryAcquire() || 
            !getPlayerRateLimiter(playerId).tryAcquire()) {
            logDebug("Rate limit exceeded for player: " + playerId);
            return;
        }
        
        String formattedResponse = chatFormatter.formatVillagerMessage(villager, response);
        
        // Send as villager speech bubble or chat message
        if (shouldUseSpeechBubble(villager, targetPlayer)) {
            displaySpeechBubble(villager, formattedResponse);
        } else {
            sendChatMessage(targetPlayer, formattedResponse);
        }
        
        // Log interaction for debugging
        logInteraction(villager, targetPlayer, response);
    }
    
    private RateLimiter getPlayerRateLimiter(UUID playerId) {
        return playerRateLimiters.computeIfAbsent(playerId, 
            id -> RateLimiter.create(RESPONSES_PER_MINUTE_PER_PLAYER / 60.0));
    }
    
    private boolean shouldUseSpeechBubble(VillagerEntity villager, Player player) {
        double distance = villager.getBlockPos().distanceTo(player.getBlockPos());
        return distance <= SPEECH_BUBBLE_RANGE && 
               player.canSee(villager) &&
               !player.isSneaking(); // Sneaking players get chat messages for privacy
    }
    
    private void displaySpeechBubble(VillagerEntity villager, String message) {
        // Platform-specific implementation
        Platform.getInstance().displaySpeechBubble(villager, message, SPEECH_BUBBLE_DURATION);
    }
}
```

## Test-Driven Development (TDD) Plan

### 6.1 Unit Tests

#### ChatEventHandlerTest
```java
class ChatEventHandlerTest {
    @Test
    void shouldTriggerAIForMatchingPatterns() {
        // Given: Handler with configured trigger patterns
        ChatEventHandler handler = new ChatEventHandler(mockRouter, 
            Arrays.asList("hello", "@villager", "\\?$"));
        
        // When: Chat event with matching message
        ServerChatEvent event = createChatEvent("Hello villagers!");
        handler.onServerChatEvent(event);
        
        // Then: Router should be called
        verify(mockRouter).routeConversation(any(ConversationContext.class));
    }
    
    @Test
    void shouldIgnoreNonMatchingMessages() {
        // Test that regular chat doesn't trigger AI
    }
    
    @Test
    void shouldUpdateTriggersPatternsDynamically() {
        // Test pattern reconfiguration without restart
    }
    
    @Test
    void shouldHandleInvalidRegexPatterns() {
        // Test graceful handling of malformed regex
    }
}
```

#### PromptBuilderTest
```java
class PromptBuilderTest {
    @Test
    void shouldBuildCompletePromptWithAllContext() {
        // Test full prompt construction with personality, mood, history
    }
    
    @Test
    void shouldTrimHistoryToTokenLimit() {
        // Test history truncation when exceeding token limits
    }
    
    @Test
    void shouldHandleSpecialCharactersInMessages() {
        // Test escaping of quotes, newlines, unicode
    }
    
    @Test
    void shouldAdjustPromptForMoodState() {
        // Test mood-based prompt modifications
    }
    
    @Test
    void shouldIncludeRelevantRelationshipContext() {
        // Test relationship data integration
    }
}
```

#### LLMApiClientMockTest
```java
class LLMApiClientMockTest {
    @Test
    void shouldRetryOnTemporaryFailures() {
        // Mock HTTP client to return 500, then 200
        // Verify retry logic works correctly
    }
    
    @Test
    void shouldFailAfterMaxRetries() {
        // Test failure handling after exhausting retries
    }
    
    @Test
    void shouldHandleTimeouts() {
        // Mock slow response, verify timeout handling
    }
    
    @Test
    void shouldParseSuccessfulResponse() {
        // Test response parsing for different providers
    }
    
    @Test
    void shouldHandleRateLimitingHeaders() {
        // Test 429 responses and backoff
    }
}
```

#### VillagerBrainTest
```java
class VillagerBrainTest {
    @Test
    void shouldUpdateMoodBasedOnInteractions() {
        // Test mood transitions based on interaction sentiment
    }
    
    @Test
    void shouldTrimOldMemoriesWhenFull() {
        // Test memory management and trimming logic
    }
    
    @Test
    void shouldSerializeAndDeserializeCorrectly() {
        // Test NBT persistence round-trip
    }
    
    @Test
    void shouldAdjustRelationshipsOverTime() {
        // Test relationship level changes
    }
    
    @Test
    void shouldHandleCorruptedNBTData() {
        // Test graceful handling of invalid saved data
    }
}
```

#### VillagerBrainManagerTest
```java
class VillagerBrainManagerTest {
    @Test
    void shouldLoadExistingBrainFromNBT() {
        // Test brain loading from persistent storage
    }
    
    @Test
    void shouldCreateNewBrainForNewVillager() {
        // Test brain initialization for fresh villagers
    }
    
    @Test
    void shouldUnloadInactiveBrains() {
        // Test memory management for inactive brains
    }
    
    @Test
    void shouldHandleConcurrentAccess() {
        // Test thread safety with multiple simultaneous requests
    }
}
```

#### VillagerProximityDetectorTest
```java
class VillagerProximityDetectorTest {
    @Test
    void shouldDetectVillagersInRange() {
        // Mock world with villagers at various distances
        // Verify only those in range are returned
    }
    
    @Test
    void shouldSortByDistance() {
        // Test distance-based sorting
    }
    
    @Test
    void shouldFilterInvalidTargets() {
        // Test filtering of sleeping/dead/combat villagers
    }
    
    @Test
    void shouldHandleEmptyWorld() {
        // Test behavior with no villagers present
    }
    
    @Test
    void shouldUseOptimizedLookupForLargeVillages() {
        // Performance test with many villagers
    }
}
```

### 6.2 Integration Tests

#### ConversationRouterIntegrationTest
```java
class ConversationRouterIntegrationTest {
    @Test
    void shouldCompleteFullConversationFlow() {
        // End-to-end test: chat event → LLM call → response delivery
        // Given: Real components wired together with mock LLM
        // When: Chat event occurs
        // Then: Verify complete flow execution
    }
    
    @Test
    void shouldHandleMultipleSimultaneousConversations() {
        // Test concurrent conversation processing
    }
    
    @Test
    void shouldRecoverFromLLMFailures() {
        // Test graceful degradation when LLM is unavailable
    }
}
```

#### VillagerMemoryPersistenceTest
```java
class VillagerMemoryPersistenceTest {
    @Test
    void shouldPersistBrainAcrossWorldReloads() {
        // Test brain persistence through world save/load cycles
    }
    
    @Test
    void shouldMigrateOldBrainData() {
        // Test backward compatibility with older save formats
    }
    
    @Test
    void shouldHandleWorldCorruption() {
        // Test recovery from corrupted world data
    }
}
```

### 6.3 Performance Tests

#### ConversationPerformanceTest
```java
class ConversationPerformanceTest {
    @Test
    void shouldHandleBurstOf100ConcurrentChats() {
        // Simulate 100 players chatting simultaneously
        // Measure response times and resource usage
        // Assert: 95th percentile response time < 2 seconds
        // Assert: No memory leaks or resource exhaustion
    }
    
    @Test
    void shouldMaintainPerformanceWith1000Villagers() {
        // Test proximity detection and brain management at scale
    }
    
    @Test
    void shouldLimitMemoryUsageOverTime() {
        // Long-running test to verify memory management
    }
}
```

### 6.4 Test Configuration

#### Test Data Management
```java
class TestDataBuilder {
    static VillagerBrain createTestBrain(PersonalityProfile personality) {
        // Factory methods for creating test data
    }
    
    static ConversationContext createTestContext(String message, Player player) {
        // Helper for creating test conversation contexts
    }
    
    static ServerChatEvent createChatEvent(String message) {
        // Mock chat event creation
    }
}
```

## Configuration & Tuning

### AI Trigger Patterns
```yaml
ai_triggers:
  patterns:
    - "hello|hi|hey"
    - "@villager"
    - "\\?$"  # Questions ending with ?
    - "help"
  case_sensitive: false
  timeout_ms: 30000
```

### Performance Tuning
```yaml
performance:
  conversation_range: 8.0
  max_concurrent_conversations: 50
  brain_unload_timeout_minutes: 30
  rate_limit_responses_per_minute: 10
  max_history_interactions: 100
```

### LLM Provider Configuration
```yaml
llm_providers:
  openai:
    model: "gpt-3.5-turbo"
    max_tokens: 150
    temperature: 0.8
  anthropic:
    model: "claude-3-haiku"
    max_tokens: 150
    temperature: 0.8
```

## Security Considerations

1. **Input Sanitization**: All chat messages must be sanitized before sending to LLM
2. **Rate Limiting**: Prevent abuse through aggressive rate limiting
3. **API Key Protection**: Secure storage of LLM provider API keys
4. **Content Filtering**: Filter inappropriate responses from LLM
5. **Data Privacy**: Ensure conversation data follows privacy guidelines

## Performance Targets

- **Response Time**: 95th percentile < 2 seconds
- **Throughput**: Handle 100 concurrent conversations
- **Memory Usage**: < 50MB for 1000 active villager brains
- **Reliability**: 99.5% success rate for LLM calls (excluding external failures)

This specification provides a comprehensive foundation for implementing Phase 4 with robust testing coverage and clear performance expectations.