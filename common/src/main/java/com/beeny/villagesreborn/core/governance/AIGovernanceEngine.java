package com.beeny.villagesreborn.core.governance;

import com.beeny.villagesreborn.core.ai.PersonalityProfile;
import com.beeny.villagesreborn.core.ai.RelationshipData;
import com.beeny.villagesreborn.core.ai.VillagerBrain;
import com.beeny.villagesreborn.core.common.VillagerEntity;
import com.beeny.villagesreborn.core.llm.ConversationRequest;
import com.beeny.villagesreborn.core.llm.ConversationResponse;
import com.beeny.villagesreborn.core.llm.LLMApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI-powered governance engine that manages village elections, policy decisions,
 * and political dynamics using LLM for realistic political behavior
 */
public class AIGovernanceEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(AIGovernanceEngine.class);
    
    private final LLMApiClient llmClient;
    
    // Patterns for parsing AI responses
    private static final Pattern VOTE_PATTERN = Pattern.compile("(?i)vote:\\s*(\\w+)");
    private static final Pattern SUPPORT_PATTERN = Pattern.compile("(?i)support:\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern CANDIDATE_PATTERN = Pattern.compile("(?i)candidate:\\s*(\\w+)");
    private static final Pattern POLICY_PATTERN = Pattern.compile("(?i)policy preference:\\s*(.+)");
    
    public AIGovernanceEngine(LLMApiClient llmClient) {
        this.llmClient = llmClient;
    }
    
    /**
     * Conducts AI-powered village elections with personality-driven voting behavior
     */
    public CompletableFuture<ElectionResult> conductElection(List<VillagerEntity> candidates,
                                                           List<VillagerEntity> voters,
                                                           ElectionType electionType,
                                                           Map<UUID, VillagerBrain> villagerBrains,
                                                           Map<UUID, Map<UUID, RelationshipData>> relationships) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<UUID, List<Vote>> candidateVotes = new HashMap<>();
                List<VotingDecision> votingDecisions = new ArrayList<>();
                
                // Initialize vote tracking
                for (VillagerEntity candidate : candidates) {
                    candidateVotes.put(candidate.getUUID(), new ArrayList<>());
                }
                
                // Process each voter's decision
                for (VillagerEntity voter : voters) {
                    VillagerBrain voterBrain = villagerBrains.get(voter.getUUID());
                    if (voterBrain == null) continue;
                    
                    VotingDecision decision = generateVotingDecision(voter, candidates, electionType,
                                                                   voterBrain, relationships.get(voter.getUUID()));
                    
                    votingDecisions.add(decision);
                    
                    if (decision.getChosenCandidate() != null) {
                        Vote vote = new Vote(voter.getUUID(), decision.getChosenCandidate(),
                                           decision.getSupportLevel(), decision.getReasoning());
                        candidateVotes.get(decision.getChosenCandidate()).add(vote);
                    }
                }
                
                // Calculate results
                return calculateElectionResults(candidates, candidateVotes, votingDecisions, electionType);
                
            } catch (Exception e) {
                LOGGER.error("Error conducting AI election", e);
                return generateFallbackElectionResult(candidates, voters);
            }
        });
    }
    
    /**
     * AI-powered policy decision making for village governance
     */
    public CompletableFuture<PolicyDecision> makePolicyDecision(PolicyProposal proposal,
                                                              VillagerEntity leader,
                                                              List<VillagerEntity> councilMembers,
                                                              VillagerBrain leaderBrain,
                                                              Map<UUID, VillagerBrain> councilBrains,
                                                              VillageData villageData) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get leader's initial assessment
                PolicyAssessment leaderAssessment = generatePolicyAssessment(
                    proposal, leader, leaderBrain, villageData, true);
                
                // Get council opinions
                List<PolicyAssessment> councilAssessments = new ArrayList<>();
                for (VillagerEntity councilMember : councilMembers) {
                    VillagerBrain brain = councilBrains.get(councilMember.getUUID());
                    if (brain != null) {
                        PolicyAssessment assessment = generatePolicyAssessment(
                            proposal, councilMember, brain, villageData, false);
                        councilAssessments.add(assessment);
                    }
                }
                
                // Calculate final decision
                return calculatePolicyDecision(proposal, leaderAssessment, councilAssessments);
                
            } catch (Exception e) {
                LOGGER.error("Error in AI policy decision making", e);
                return generateFallbackPolicyDecision(proposal, leaderBrain);
            }
        });
    }
    
    /**
     * Generates AI-powered voting decisions based on personality and relationships
     */
    private VotingDecision generateVotingDecision(VillagerEntity voter,
                                                List<VillagerEntity> candidates,
                                                ElectionType electionType,
                                                VillagerBrain voterBrain,
                                                Map<UUID, RelationshipData> voterRelationships) {
        
        if (!shouldUseAI(voterBrain, candidates.size())) {
            return generateFallbackVotingDecision(voter, candidates, voterBrain, voterRelationships);
        }
        
        try {
            String prompt = buildVotingPrompt(voter, candidates, electionType, voterBrain, voterRelationships);
            
            ConversationRequest request = ConversationRequest.builder()
                .prompt(prompt)
                .maxTokens(150)
                .temperature(0.8f)
                .timeout(Duration.ofSeconds(5))
                .build();
            
            CompletableFuture<ConversationResponse> responseFuture = llmClient.generateConversationResponse(request);
            ConversationResponse response = responseFuture.get(5, TimeUnit.SECONDS);
            
            if (response != null && response.isSuccess()) {
                return parseVotingDecision(response.getResponse(), voter, candidates);
            }
            
            LOGGER.warn("AI voting decision failed for voter {}, using fallback", voter.getUUID());
            return generateFallbackVotingDecision(voter, candidates, voterBrain, voterRelationships);
            
        } catch (Exception e) {
            LOGGER.error("Error generating AI voting decision", e);
            return generateFallbackVotingDecision(voter, candidates, voterBrain, voterRelationships);
        }
    }
    
    /**
     * Generates policy assessment using AI analysis
     */
    private PolicyAssessment generatePolicyAssessment(PolicyProposal proposal,
                                                    VillagerEntity villager,
                                                    VillagerBrain brain,
                                                    VillageData villageData,
                                                    boolean isLeader) {
        
        try {
            String prompt = buildPolicyPrompt(proposal, villager, brain, villageData, isLeader);
            
            ConversationRequest request = ConversationRequest.builder()
                .prompt(prompt)
                .maxTokens(120)
                .temperature(0.7f)
                .timeout(Duration.ofSeconds(5))
                .build();
            
            CompletableFuture<ConversationResponse> responseFuture = llmClient.generateConversationResponse(request);
            ConversationResponse response = responseFuture.get(5, TimeUnit.SECONDS);
            
            if (response != null && response.isSuccess()) {
                return parsePolicyAssessment(response.getResponse(), villager, proposal);
            }
            
            return generateFallbackPolicyAssessment(proposal, brain, isLeader);
            
        } catch (Exception e) {
            LOGGER.error("Error generating AI policy assessment", e);
            return generateFallbackPolicyAssessment(proposal, brain, isLeader);
        }
    }
    
    /**
     * Builds AI prompt for voting decisions
     */
    private String buildVotingPrompt(VillagerEntity voter,
                                   List<VillagerEntity> candidates,
                                   ElectionType electionType,
                                   VillagerBrain voterBrain,
                                   Map<UUID, RelationshipData> relationships) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a villager voting in a ").append(electionType.name().toLowerCase()).append(" election.\n\n");
        
        // Voter personality
        PersonalityProfile personality = voterBrain.getPersonalityTraits();
        prompt.append("Your personality: ").append(personality.generateDescription()).append("\n");
        prompt.append("Your mood: ").append(voterBrain.getCurrentMood().getOverallMood()).append("\n\n");
        
        // Candidates
        prompt.append("Candidates:\n");
        for (int i = 0; i < candidates.size(); i++) {
            VillagerEntity candidate = candidates.get(i);
            prompt.append((i + 1)).append(". ").append(candidate.getName())
                  .append(" (").append(candidate.getProfession()).append(")");
            
            // Add relationship context if available
            if (relationships != null && relationships.containsKey(candidate.getUUID())) {
                RelationshipData rel = relationships.get(candidate.getUUID());
                prompt.append(" - Trust: ").append(String.format("%.1f", rel.getTrustLevel()))
                      .append(", Friendship: ").append(String.format("%.1f", rel.getFriendshipLevel()));
            }
            prompt.append("\n");
        }
        
        prompt.append("\nConsider your personality, relationships, and what's best for the village.\n");
        prompt.append("Vote: [candidate name]\n");
        prompt.append("Support: [0.0 to 1.0 - how enthusiastic you are]\n");
        prompt.append("Reasoning: [brief explanation]\n\n");
        prompt.append("Keep response under 120 tokens.");
        
        return prompt.toString();
    }
    
    /**
     * Builds AI prompt for policy decisions
     */
    private String buildPolicyPrompt(PolicyProposal proposal,
                                   VillagerEntity villager,
                                   VillagerBrain brain,
                                   VillageData villageData,
                                   boolean isLeader) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a ").append(isLeader ? "village leader" : "council member")
              .append(" reviewing a policy proposal.\n\n");
        
        prompt.append("Proposal: ").append(proposal.getTitle()).append("\n");
        prompt.append("Description: ").append(proposal.getDescription()).append("\n");
        prompt.append("Impact: ").append(proposal.getExpectedImpact()).append("\n\n");
        
        // Village context
        prompt.append("Village Status:\n");
        prompt.append("- Population: ").append(villageData.getPopulation()).append("\n");
        prompt.append("- Resources: ").append(villageData.getResourceSummary()).append("\n");
        prompt.append("- Current issues: ").append(villageData.getCurrentIssues()).append("\n\n");
        
        // Decision maker's perspective
        PersonalityProfile personality = brain.getPersonalityTraits();
        prompt.append("Your personality: ").append(personality.generateDescription()).append("\n\n");
        
        prompt.append("Decision: APPROVE/REJECT/MODIFY\n");
        prompt.append("Support: [0.0 to 1.0 - how strongly you feel]\n");
        prompt.append("Reasoning: [your justification]\n\n");
        prompt.append("Keep response under 100 tokens.");
        
        return prompt.toString();
    }
    
    /**
     * Parses AI voting decision response
     */
    private VotingDecision parseVotingDecision(String response, VillagerEntity voter, List<VillagerEntity> candidates) {
        Matcher voteMatcher = VOTE_PATTERN.matcher(response);
        Matcher supportMatcher = SUPPORT_PATTERN.matcher(response);
        
        UUID chosenCandidate = null;
        float supportLevel = 0.5f;
        String reasoning = "AI-generated decision";
        
        // Parse vote
        if (voteMatcher.find()) {
            String votedName = voteMatcher.group(1);
            for (VillagerEntity candidate : candidates) {
                if (candidate.getName().equalsIgnoreCase(votedName)) {
                    chosenCandidate = candidate.getUUID();
                    break;
                }
            }
        }
        
        // Parse support level
        if (supportMatcher.find()) {
            try {
                supportLevel = Float.parseFloat(supportMatcher.group(1));
                supportLevel = Math.max(0.0f, Math.min(1.0f, supportLevel));
            } catch (NumberFormatException e) {
                // Use default
            }
        }
        
        // Extract reasoning (simple approach - take everything after "reasoning:")
        String[] parts = response.toLowerCase().split("reasoning:");
        if (parts.length > 1) {
            reasoning = parts[1].trim().substring(0, Math.min(parts[1].trim().length(), 100));
        }
        
        return new VotingDecision(voter.getUUID(), chosenCandidate, supportLevel, reasoning);
    }
    
    /**
     * Determines if AI should be used for this governance decision
     */
    private boolean shouldUseAI(VillagerBrain brain, int complexityFactor) {
        // Use AI for complex scenarios or when personality suggests nuanced thinking
        return complexityFactor > 2 || brain.getPersonalityTraits().getTraitInfluence(
            com.beeny.villagesreborn.core.ai.TraitType.CURIOSITY) > 0.6f;
    }
    
    // Additional methods for fallback logic and result calculation...
    
    private VotingDecision generateFallbackVotingDecision(VillagerEntity voter,
                                                        List<VillagerEntity> candidates,
                                                        VillagerBrain voterBrain,
                                                        Map<UUID, RelationshipData> relationships) {
        // Simple fallback: vote for candidate with best relationship or random
        UUID bestCandidate = null;
        float bestRelationship = -1.0f;
        
        if (relationships != null) {
            for (VillagerEntity candidate : candidates) {
                RelationshipData rel = relationships.get(candidate.getUUID());
                if (rel != null) {
                    float totalRel = rel.getTrustLevel() + rel.getFriendshipLevel();
                    if (totalRel > bestRelationship) {
                        bestRelationship = totalRel;
                        bestCandidate = candidate.getUUID();
                    }
                }
            }
        }
        
        if (bestCandidate == null && !candidates.isEmpty()) {
            bestCandidate = candidates.get(new Random().nextInt(candidates.size())).getUUID();
        }
        
        return new VotingDecision(voter.getUUID(), bestCandidate, 0.6f, "Fallback decision");
    }
    
    private PolicyAssessment generateFallbackPolicyAssessment(PolicyProposal proposal, VillagerBrain brain, boolean isLeader) {
        // Simple rule-based assessment
        float support = 0.5f; // Neutral by default
        PolicyDecision.Decision decision = PolicyDecision.Decision.APPROVE;
        
        return new PolicyAssessment(brain.getVillagerUUID(), decision, support, "Rule-based assessment");
    }
    
    private ElectionResult calculateElectionResults(List<VillagerEntity> candidates,
                                                  Map<UUID, List<Vote>> candidateVotes,
                                                  List<VotingDecision> votingDecisions,
                                                  ElectionType electionType) {
        // Calculate winner based on vote counts and support levels
        UUID winner = null;
        int maxVotes = 0;
        float maxSupport = 0.0f;
        
        for (VillagerEntity candidate : candidates) {
            List<Vote> votes = candidateVotes.get(candidate.getUUID());
            if (votes.size() > maxVotes || 
                (votes.size() == maxVotes && calculateAverageSupport(votes) > maxSupport)) {
                maxVotes = votes.size();
                maxSupport = calculateAverageSupport(votes);
                winner = candidate.getUUID();
            }
        }
        
        return new ElectionResult(winner, candidateVotes, votingDecisions, electionType);
    }
    
    private float calculateAverageSupport(List<Vote> votes) {
        return (float) votes.stream().mapToDouble(Vote::getSupportLevel).average().orElse(0.0);
    }
    
    private PolicyDecision calculatePolicyDecision(PolicyProposal proposal,
                                                 PolicyAssessment leaderAssessment,
                                                 List<PolicyAssessment> councilAssessments) {
        // Weighted decision: leader has more influence
        float leaderWeight = 0.6f;
        float councilWeight = 0.4f;
        
        float totalSupport = leaderAssessment.getSupportLevel() * leaderWeight;
        if (!councilAssessments.isEmpty()) {
            float avgCouncilSupport = (float) councilAssessments.stream()
                .mapToDouble(PolicyAssessment::getSupportLevel).average().orElse(0.5);
            totalSupport += avgCouncilSupport * councilWeight;
        }
        
        PolicyDecision.Decision finalDecision = totalSupport > 0.6f ? 
            PolicyDecision.Decision.APPROVE : PolicyDecision.Decision.REJECT;
        
        return new PolicyDecision(proposal, finalDecision, totalSupport, 
                                leaderAssessment, councilAssessments);
    }
    
    private ElectionResult generateFallbackElectionResult(List<VillagerEntity> candidates, List<VillagerEntity> voters) {
        // Random fallback
        UUID winner = candidates.isEmpty() ? null : candidates.get(0).getUUID();
        return new ElectionResult(winner, new HashMap<>(), new ArrayList<>(), ElectionType.MAYOR);
    }
    
    private PolicyDecision generateFallbackPolicyDecision(PolicyProposal proposal, VillagerBrain leaderBrain) {
        PolicyAssessment fallbackAssessment = new PolicyAssessment(
            leaderBrain.getVillagerUUID(), PolicyDecision.Decision.APPROVE, 0.5f, "Fallback decision");
        return new PolicyDecision(proposal, PolicyDecision.Decision.APPROVE, 0.5f, 
                                fallbackAssessment, new ArrayList<>());
    }
    
    private PolicyAssessment parsePolicyAssessment(String response, VillagerEntity villager, PolicyProposal proposal) {
        // Parse AI policy assessment (simplified)
        PolicyDecision.Decision decision = PolicyDecision.Decision.APPROVE;
        float support = 0.5f;
        
        if (response.toLowerCase().contains("reject")) {
            decision = PolicyDecision.Decision.REJECT;
        } else if (response.toLowerCase().contains("modify")) {
            decision = PolicyDecision.Decision.MODIFY;
        }
        
        Matcher supportMatcher = SUPPORT_PATTERN.matcher(response);
        if (supportMatcher.find()) {
            try {
                support = Float.parseFloat(supportMatcher.group(1));
            } catch (NumberFormatException e) {
                // Use default
            }
        }
        
        return new PolicyAssessment(villager.getUUID(), decision, support, "AI assessment");
    }
}