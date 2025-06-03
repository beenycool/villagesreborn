package com.beeny.villagesreborn.core.expansion;

import com.beeny.villagesreborn.core.llm.ConversationRequest;
import com.beeny.villagesreborn.core.llm.ConversationResponse;
import com.beeny.villagesreborn.core.llm.LLMApiClient;
import com.beeny.villagesreborn.core.common.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of ExpansionAIEngine using LLM for intelligent expansion planning
 */
public class ExpansionAIEngineImpl implements ExpansionAIEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpansionAIEngineImpl.class);
    
    private final LLMApiClient llmClient;
    
    // Patterns for parsing AI responses
    private static final Pattern BUILDING_PATTERN = Pattern.compile("(?i)building types:\\s*(.+)");
    private static final Pattern STRATEGY_PATTERN = Pattern.compile("(?i)placement strategy:\\s*(\\w+)");
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("(?i)resource priority:\\s*(.+)");
    private static final Pattern TIMELINE_PATTERN = Pattern.compile("(?i)timeline:\\s*(.+)");
    
    public ExpansionAIEngineImpl(LLMApiClient llmClient) {
        this.llmClient = llmClient;
    }
    
    @Override
    public CompletableFuture<ExpansionProposal> generateExpansionPlan(VillageResources resources,
                                                                    TerrainData terrainData,
                                                                    BiomeData biomeData,
                                                                    GrowthTrends growthTrends,
                                                                    long timeoutMs) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build expansion prompt
                String prompt = buildExpansionPrompt(resources, terrainData, biomeData, growthTrends);
                
                // Create conversation request
                ConversationRequest request = ConversationRequest.builder()
                    .prompt(prompt)
                    .maxTokens(200) // More tokens for detailed expansion plans
                    .timeout(Duration.ofMillis(timeoutMs))
                    .build();
                
                // Get AI response
                CompletableFuture<ConversationResponse> responseFuture = llmClient.generateConversationResponse(request);
                ConversationResponse response = responseFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
                
                if (response != null && response.isSuccess() && response.getResponse() != null) {
                    return parseExpansionResponse(response.getResponse(), resources, biomeData);
                } else {
                    LOGGER.warn("Empty or failed AI expansion response, using procedural fallback");
                    return proceduralFallback(resources, biomeData);
                }
                
            } catch (Exception e) {
                LOGGER.error("AI expansion planning failed", e);
                return proceduralFallback(resources, biomeData);
            }
        });
    }
    
    @Override
    public boolean shouldUseAI(VillageResources resources, ExpansionComplexity complexity) {
        // Use AI for complex expansion scenarios
        if (complexity == ExpansionComplexity.HIGH) return true;
        if (resources.getPopulation() > 50) return true; // Large villages benefit from AI planning
        if (resources.getTotalResources() > 1000) return true; // Resource-rich villages
        
        // Use AI for moderate complexity
        return complexity == ExpansionComplexity.MEDIUM && resources.getPopulation() > 20;
    }
    
    @Override
    public ExpansionProposal proceduralFallback(VillageResources resources, BiomeData biomeData) {
        // Simple procedural expansion logic
        List<BuildingRequest> buildingRequests = new ArrayList<>();
        
        // Basic needs first
        if (resources.getPopulation() > resources.getHousing() * 2) {
            buildingRequests.add(new BuildingRequest("house", 3, BuildingPriority.HIGH));
        }
        
        // Economic buildings
        if (resources.getFood() < resources.getPopulation() * 10) {
            buildingRequests.add(new BuildingRequest("farm", 2, BuildingPriority.MEDIUM));
        }
        
        // Defensive structures for certain biomes
        if (biomeData != null && (biomeData.getBiomeType() == BiomeData.BiomeType.DESERT ||
                                  biomeData.getBiomeType() == BiomeData.BiomeType.MOUNTAIN)) {
            buildingRequests.add(new BuildingRequest("watchtower", 1, BuildingPriority.LOW));
        }
        
        // Convert to the format expected by ExpansionProposal
        List<BuildingPlan> buildingPlans = convertToBuildingPlans(buildingRequests);
        ExpansionProposal.PlacementStrategy strategy = getDefaultStrategy(biomeData);
        ResourcePlan resourcePlan = convertToResourcePlan(calculateResourceNeeds(buildingRequests));
        
        return new ExpansionProposal(buildingPlans, resourcePlan, strategy);
    }
    
    @Override
    public boolean validateExpansionPlan(ExpansionProposal proposal, VillageResources resources) {
        if (proposal == null || proposal.getBuildingQueue() == null) {
            return false;
        }
        
        // Check if village has enough resources
        ResourcePlan resourcePlan = proposal.getResourcePlan();
        if (resourcePlan != null) {
            ResourcePlan.ResourceAllocation woodAlloc = resourcePlan.getAllocation(ResourceCost.ResourceType.WOOD);
            ResourcePlan.ResourceAllocation stoneAlloc = resourcePlan.getAllocation(ResourceCost.ResourceType.STONE);
            ResourcePlan.ResourceAllocation foodAlloc = resourcePlan.getAllocation(ResourceCost.ResourceType.FOOD);
            
            if (woodAlloc != null && woodAlloc.getAmount() > resources.getWood()) return false;
            if (stoneAlloc != null && stoneAlloc.getAmount() > resources.getStone()) return false;
            if (foodAlloc != null && foodAlloc.getAmount() > resources.getFood()) return false;
        }
        
        // Validate building types
        for (BuildingPlan building : proposal.getBuildingQueue()) {
            if (!isValidBuildingType(building.getType().toString())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Builds AI prompt for expansion planning
     */
    private String buildExpansionPrompt(VillageResources resources,
                                      TerrainData terrainData,
                                      BiomeData biomeData,
                                      GrowthTrends growthTrends) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Village Expansion Analysis:\n\n");
        
        // Current State
        prompt.append("Current State:\n");
        prompt.append("- Population: ").append(resources.getPopulation()).append("\n");
        prompt.append("- Resources: Wood=").append(resources.getWood())
              .append(", Stone=").append(resources.getStone())
              .append(", Food=").append(resources.getFood()).append("\n");
        prompt.append("- Housing: ").append(resources.getHousing()).append(" units\n");
        prompt.append("- Biome: ").append(biomeData != null ? biomeData.getBiomeType() : "Unknown").append("\n");
        
        // Biome characteristics
        prompt.append("- Terrain: ").append(getTerrainDescription(terrainData)).append("\n\n");
        
        // Expansion Goals
        prompt.append("Expansion Goals:\n");
        prompt.append("- Housing Demand: ").append(calculateHousingNeeded(resources)).append(" new units\n");
        prompt.append("- Economic Growth: ").append(getEconomicPriorities(resources)).append("\n");
        prompt.append("- Defense Considerations: ").append(getDefensiveNeeds(biomeData)).append("\n\n");
        
        // Growth trends
        if (growthTrends != null) {
            prompt.append("Growth Trends:\n");
            prompt.append("- Population Growth: ").append(growthTrends.getPopulationGrowthRate()).append("%\n");
            prompt.append("- Economic Trend: ").append(growthTrends.getEconomicTrend()).append("\n\n");
        }
        
        // Request format
        prompt.append("Recommend:\n");
        prompt.append("1. Building Types: [house/farm/workshop/watchtower/market] with quantities\n");
        prompt.append("2. Placement Strategy: [compact/distributed/linear/defensive]\n");
        prompt.append("3. Resource Priority: [which resources to focus on]\n");
        prompt.append("4. Timeline: [expansion phases]\n\n");
        prompt.append("Keep response under 150 tokens.");
        
        return prompt.toString();
    }
    
    /**
     * Parses AI response into ExpansionProposal
     */
    private ExpansionProposal parseExpansionResponse(String response, VillageResources resources, BiomeData biomeData) {
        try {
            // Parse building types
            List<BuildingRequest> buildingRequests = parseBuildingTypes(response);
            
            // Parse placement strategy
            ExpansionProposal.PlacementStrategy strategy = parseStrategy(response);
            
            // Convert to the format expected by ExpansionProposal
            List<BuildingPlan> buildingPlans = convertToBuildingPlans(buildingRequests);
            ResourcePlan resourcePlan = convertToResourcePlan(calculateResourceNeeds(buildingRequests));
            
            return new ExpansionProposal(buildingPlans, resourcePlan, strategy);
            
        } catch (Exception e) {
            LOGGER.error("Failed to parse AI expansion response: {}", response, e);
            return proceduralFallback(resources, biomeData);
        }
    }
    
    /**
     * Converts BuildingRequest list to BuildingPlan list
     */
    private List<BuildingPlan> convertToBuildingPlans(List<BuildingRequest> buildingRequests) {
        List<BuildingPlan> buildingPlans = new ArrayList<>();
        
        for (BuildingRequest request : buildingRequests) {
            BuildingPlan.BuildingType type = convertBuildingType(request.getBuildingType());
            ResourceCost cost = calculateBuildingCost(request.getBuildingType(), request.getQuantity());
            
            for (int i = 0; i < request.getQuantity(); i++) {
                // Calculate AI-powered position based on building type and strategy
                BlockPos position = calculateOptimalPosition(request, i, buildingPlans.size());
                
                BuildingPlan plan = new BuildingPlan(
                    type,
                    position,
                    request.getPriority().ordinal(),
                    cost
                );
                buildingPlans.add(plan);
            }
        }
        
        return buildingPlans;
    }
    
    /**
     * Converts ResourceAllocation to ResourcePlan
     */
    private ResourcePlan convertToResourcePlan(ResourceAllocation allocation) {
        ResourcePlan plan = new ResourcePlan(allocation.getTotalCost(), 3);
        
        if (allocation.getWoodCost() > 0) {
            plan.addAllocation(ResourceCost.ResourceType.WOOD, allocation.getWoodCost(), 1);
        }
        if (allocation.getStoneCost() > 0) {
            plan.addAllocation(ResourceCost.ResourceType.STONE, allocation.getStoneCost(), 1);
        }
        if (allocation.getFoodCost() > 0) {
            plan.addAllocation(ResourceCost.ResourceType.FOOD, allocation.getFoodCost(), 1);
        }
        if (allocation.getIronCost() > 0) {
            plan.addAllocation(ResourceCost.ResourceType.IRON, allocation.getIronCost(), 2);
        }
        
        return plan;
    }
    
    /**
     * Converts string building type to BuildingPlan.BuildingType
     */
    private BuildingPlan.BuildingType convertBuildingType(String buildingType) {
        return switch (buildingType.toLowerCase()) {
            case "house" -> BuildingPlan.BuildingType.HOUSE;
            case "farm" -> BuildingPlan.BuildingType.FARM;
            case "workshop" -> BuildingPlan.BuildingType.WORKSHOP;
            case "watchtower" -> BuildingPlan.BuildingType.DEFENSE_TOWER;
            case "market" -> BuildingPlan.BuildingType.MARKET;
            default -> BuildingPlan.BuildingType.HOUSE;
        };
    }
    
    /**
     * Calculates the resource cost for a specific building type
     */
    private ResourceCost calculateBuildingCost(String buildingType, int quantity) {
        ResourceCost cost = new ResourceCost();
        
        switch (buildingType.toLowerCase()) {
            case "house" -> {
                cost.addCost(ResourceCost.ResourceType.WOOD, 20 * quantity);
                cost.addCost(ResourceCost.ResourceType.STONE, 10 * quantity);
            }
            case "farm" -> {
                cost.addCost(ResourceCost.ResourceType.WOOD, 15 * quantity);
                cost.addCost(ResourceCost.ResourceType.FOOD, 5 * quantity);
            }
            case "workshop" -> {
                cost.addCost(ResourceCost.ResourceType.WOOD, 25 * quantity);
                cost.addCost(ResourceCost.ResourceType.STONE, 15 * quantity);
            }
            case "watchtower" -> {
                cost.addCost(ResourceCost.ResourceType.STONE, 30 * quantity);
                cost.addCost(ResourceCost.ResourceType.WOOD, 10 * quantity);
            }
            case "market" -> {
                cost.addCost(ResourceCost.ResourceType.WOOD, 40 * quantity);
                cost.addCost(ResourceCost.ResourceType.STONE, 20 * quantity);
            }
        }
        
        return cost;
    }
    
    private List<BuildingRequest> parseBuildingTypes(String response) {
        List<BuildingRequest> buildings = new ArrayList<>();
        Matcher matcher = BUILDING_PATTERN.matcher(response);
        
        if (matcher.find()) {
            String buildingStr = matcher.group(1);
            String[] buildingParts = buildingStr.split(",");
            
            for (String part : buildingParts) {
                String trimmed = part.trim().toLowerCase();
                
                // Extract quantity and type
                if (trimmed.contains("house")) {
                    int quantity = extractQuantity(trimmed, 2);
                    buildings.add(new BuildingRequest("house", quantity, BuildingPriority.HIGH));
                } else if (trimmed.contains("farm")) {
                    int quantity = extractQuantity(trimmed, 1);
                    buildings.add(new BuildingRequest("farm", quantity, BuildingPriority.MEDIUM));
                } else if (trimmed.contains("workshop")) {
                    int quantity = extractQuantity(trimmed, 1);
                    buildings.add(new BuildingRequest("workshop", quantity, BuildingPriority.MEDIUM));
                } else if (trimmed.contains("watchtower") || trimmed.contains("tower")) {
                    int quantity = extractQuantity(trimmed, 1);
                    buildings.add(new BuildingRequest("watchtower", quantity, BuildingPriority.LOW));
                } else if (trimmed.contains("market")) {
                    int quantity = extractQuantity(trimmed, 1);
                    buildings.add(new BuildingRequest("market", quantity, BuildingPriority.LOW));
                }
            }
        }
        
        // Default if nothing parsed
        if (buildings.isEmpty()) {
            buildings.add(new BuildingRequest("house", 2, BuildingPriority.HIGH));
        }
        
        return buildings;
    }
    
    private int extractQuantity(String text, int defaultQuantity) {
        Pattern numberPattern = Pattern.compile("\\b(\\d+)\\b");
        Matcher matcher = numberPattern.matcher(text);
        if (matcher.find()) {
            try {
                int quantity = Integer.parseInt(matcher.group(1));
                return Math.max(1, Math.min(quantity, 5)); // Limit to reasonable range
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return defaultQuantity;
    }
    
    private ExpansionProposal.PlacementStrategy parseStrategy(String response) {
        Matcher matcher = STRATEGY_PATTERN.matcher(response);
        if (matcher.find()) {
            String strategyStr = matcher.group(1).toLowerCase();
            return switch (strategyStr) {
                case "compact" -> ExpansionProposal.PlacementStrategy.COMPACT;
                case "distributed" -> ExpansionProposal.PlacementStrategy.DISTRIBUTED;
                case "linear" -> ExpansionProposal.PlacementStrategy.LINEAR;
                case "defensive" -> ExpansionProposal.PlacementStrategy.DEFENSIVE;
                default -> ExpansionProposal.PlacementStrategy.COMPACT;
            };
        }
        return ExpansionProposal.PlacementStrategy.COMPACT; // Default
    }
    
    private String parseResourcePriority(String response) {
        Matcher matcher = RESOURCE_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Balanced resource allocation";
    }
    
    private String parseTimeline(String response) {
        Matcher matcher = TIMELINE_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "Standard 3-phase expansion";
    }
    
    // Helper methods
    private String getTerrainDescription(TerrainData terrainData) {
        if (terrainData == null) return "Standard terrain";
        return terrainData.toString();
    }
    
    private int calculateHousingNeeded(VillageResources resources) {
        return Math.max(0, resources.getPopulation() - resources.getHousing());
    }
    
    private String getEconomicPriorities(VillageResources resources) {
        if (resources.getFood() < resources.getPopulation() * 5) {
            return "Food production critical";
        } else if (resources.getWood() < 100) {
            return "Wood gathering needed";
        } else {
            return "Balanced economic growth";
        }
    }
    
    private String getDefensiveNeeds(BiomeData biomeData) {
        if (biomeData == null) return "Medium - standard defenses";
        
        return switch (biomeData.getBiomeType()) {
            case DESERT -> "High - hostile environment";
            case MOUNTAIN -> "Medium - difficult terrain";
            case FOREST -> "Low - natural cover";
            default -> "Medium - standard defenses";
        };
    }
    
    private ExpansionProposal.PlacementStrategy getDefaultStrategy(BiomeData biomeData) {
        if (biomeData == null) return ExpansionProposal.PlacementStrategy.COMPACT;
        
        return switch (biomeData.getBiomeType()) {
            case MOUNTAIN -> ExpansionProposal.PlacementStrategy.DEFENSIVE;
            case DESERT -> ExpansionProposal.PlacementStrategy.COMPACT;
            case FOREST -> ExpansionProposal.PlacementStrategy.DISTRIBUTED;
            default -> ExpansionProposal.PlacementStrategy.COMPACT;
        };
    }
    
    private ResourceAllocation calculateResourceNeeds(List<BuildingRequest> buildings) {
        int woodCost = 0;
        int stoneCost = 0;
        int foodCost = 0;
        
        for (BuildingRequest building : buildings) {
            int quantity = building.getQuantity();
            switch (building.getBuildingType()) {
                case "house" -> {
                    woodCost += quantity * 20;
                    stoneCost += quantity * 10;
                }
                case "farm" -> {
                    woodCost += quantity * 15;
                    foodCost += quantity * 5;
                }
                case "workshop" -> {
                    woodCost += quantity * 25;
                    stoneCost += quantity * 15;
                }
                case "watchtower" -> {
                    stoneCost += quantity * 30;
                    woodCost += quantity * 10;
                }
                case "market" -> {
                    woodCost += quantity * 40;
                    stoneCost += quantity * 20;
                }
            }
        }
        
        return new ResourceAllocation(woodCost, stoneCost, foodCost);
    }
    
    private boolean isValidBuildingType(String buildingType) {
        List<String> validTypes = Arrays.asList("house", "farm", "workshop", "watchtower", "market", "library", "tavern");
        return validTypes.contains(buildingType.toLowerCase());
    }
    
    /**
     * Calculates optimal AI-powered position for building placement
     */
    private BlockPos calculateOptimalPosition(BuildingRequest request, int buildingIndex, int totalBuildings) {
        // Base village center coordinates
        int centerX = 0;
        int centerZ = 0;
        int baseY = 64; // Standard ground level
        
        String buildingType = request.getBuildingType().toLowerCase();
        
        // AI-powered placement logic based on building type and function
        switch (buildingType) {
            case "house" -> {
                // Houses form residential clusters around the village center
                double angle = (buildingIndex * 2.0 * Math.PI) / Math.max(1, request.getQuantity());
                int radius = 20 + (buildingIndex / 4) * 10; // Expanding rings
                int x = centerX + (int)(Math.cos(angle) * radius);
                int z = centerZ + (int)(Math.sin(angle) * radius);
                return new BlockPos(x, baseY, z);
            }
            case "farm" -> {
                // Farms placed in fertile areas, slightly outside residential zones
                int farmRadius = 35 + buildingIndex * 15;
                // Prefer cardinal directions for easier access
                int direction = buildingIndex % 4;
                return switch (direction) {
                    case 0 -> new BlockPos(centerX + farmRadius, baseY, centerZ);
                    case 1 -> new BlockPos(centerX, baseY, centerZ + farmRadius);
                    case 2 -> new BlockPos(centerX - farmRadius, baseY, centerZ);
                    default -> new BlockPos(centerX, baseY, centerZ - farmRadius);
                };
            }
            case "workshop" -> {
                // Workshops clustered in an industrial district
                int workshopX = centerX + 25 + (buildingIndex % 3) * 12;
                int workshopZ = centerZ - 15 + (buildingIndex / 3) * 12;
                return new BlockPos(workshopX, baseY, workshopZ);
            }
            case "watchtower" -> {
                // Watchtowers positioned for optimal surveillance coverage
                int towerRadius = 50 + buildingIndex * 20;
                double angle = buildingIndex * Math.PI / 2; // 90-degree intervals
                int x = centerX + (int)(Math.cos(angle) * towerRadius);
                int z = centerZ + (int)(Math.sin(angle) * towerRadius);
                int y = baseY + 10; // Elevated for better vision
                return new BlockPos(x, y, z);
            }
            case "market" -> {
                // Markets positioned centrally for easy access
                int marketX = centerX + (buildingIndex % 2) * 15 - 7;
                int marketZ = centerZ + (buildingIndex / 2) * 15 - 7;
                return new BlockPos(marketX, baseY, marketZ);
            }
            default -> {
                // Default placement in a grid pattern
                int gridSize = (int)Math.ceil(Math.sqrt(totalBuildings));
                int row = totalBuildings / gridSize;
                int col = totalBuildings % gridSize;
                int x = centerX + (col - gridSize/2) * 15;
                int z = centerZ + (row - gridSize/2) * 15;
                return new BlockPos(x, baseY, z);
            }
        }
    }
}