package com.beeny.village;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.util.math.Direction;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.world.Heightmap;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.Identifier;
import com.beeny.ai.LLMService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SpawnRegion {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");

    private final BlockPos center;
    private final int radius;
    private final String culture;
    private final Set<BlockPos> pointsOfInterest = new HashSet<>();
    private final Map<String, BlockPos> culturalStructures = new HashMap<>();
    private final List<String> pendingConstructions = new ArrayList<>();
    public SpawnRegion(BlockPos center, int radius, String culture) {
        this.center = center;
        this.radius = radius;
        this.culture = culture.toLowerCase();
        initializeCulturalStructures();
        LOGGER.info("Creating new {} village region at {} with radius {}", culture, center, radius);
    }

    private void initializeCulturalStructures() {
        generateCulturalStructuresList().thenAccept(structures -> {
            pendingConstructions.addAll(structures);
            LOGGER.info("Generated {} cultural structures for {} village", structures.size(), culture);
        });
    }

    private CompletableFuture<List<String>> generateCulturalStructuresList() {
        String prompt = String.format(
            "You are designing a Minecraft village based on %s culture.\n" +
            "What are 4-6 key architectural structures that would define this village?\n" +
            "Respond with building names only, one per line, no numbers or punctuation.",
            culture
        );

        return LLMService.getInstance()
            .generateResponse(prompt)
            .thenApply(response -> Arrays.asList(response.trim().split("\n")));
    }

    private CompletableFuture<BuildingTemplate> generateBuildingTemplate(String buildingName) {
        String prompt = String.format(
            "Design a %s building for a %s-style Minecraft village.\n" +
            "Specify dimensions (width, length, height) and key architectural features.\n" +
            "Respond in this format:\n" +
            "WIDTH: (number)\n" +
            "LENGTH: (number)\n" +
            "HEIGHT: (number)\n" +
            "MATERIALS: (list main Minecraft blocks to use)\n" +
            "FEATURES: (list key architectural elements)",
            buildingName, culture
        );

        return LLMService.getInstance()
            .generateResponse(prompt)
            .thenApply(this::parseBuildingTemplate);
    }

    private static record BuildingTemplate(int width, int length, int height, List<Block> materials, List<String> features) {
        // Records automatically provide getters and constructor
    }

    private BuildingTemplate parseBuildingTemplate(String response) {
        Map<String, String> params = Arrays.stream(response.split("\n"))
            .map(line -> line.split(": "))
            .filter(parts -> parts.length == 2)
            .collect(Collectors.toMap(
                parts -> parts[0],
                parts -> parts[1]
            ));

        return new BuildingTemplate(
            Integer.parseInt(params.getOrDefault("WIDTH", "5")),
            Integer.parseInt(params.getOrDefault("LENGTH", "7")),
            Integer.parseInt(params.getOrDefault("HEIGHT", "4")),
            parseMaterials(params.getOrDefault("MATERIALS", "STONE")),
            Arrays.asList(params.getOrDefault("FEATURES", "").split(", "))
        );
    }

    private List<Block> parseMaterials(String materials) {
        return Arrays.stream(materials.split(", "))
            .map(this::getBlockFromName)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private Block getBlockFromName(String name) {
        // Convert common material names to Minecraft blocks
        return switch(name.toLowerCase()) {
            case "stone" -> Blocks.STONE;
            case "wood" -> Blocks.OAK_PLANKS;
            case "brick" -> Blocks.BRICKS;
            case "sandstone" -> Blocks.SANDSTONE;
            default -> Blocks.STONE;
        };
    }

    public void tickConstruction(World world) {
        if (!pendingConstructions.isEmpty() && world.getRandom().nextFloat() < 0.05f) {
            String structure = pendingConstructions.remove(0);
            BlockPos pos = findBuildingLocation(world, structure);
            if (pos != null) {
                generateCulturalStructure(world, pos, structure);
                culturalStructures.put(structure, pos);
                LOGGER.info("Generated {} at {}", structure, pos);
            }
        }
    }

    private BlockPos findBuildingLocation(World world, String structure) {
        int maxAttempts = 50;
        int attempt = 0;
        
        while (attempt < maxAttempts) {
            // Get random position within radius
            double angle = world.getRandom().nextDouble() * Math.PI * 2;
            double distance = world.getRandom().nextDouble() * radius;
            int x = center.getX() + (int)(Math.cos(angle) * distance);
            int z = center.getZ() + (int)(Math.sin(angle) * distance);
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);

            // Skip if too close to existing structures
            if (isTooCloseToOtherStructures(pos)) {
                attempt++;
                continue;
            }

            // Validate terrain
            if (isTerrainSuitable(world, pos, structure)) {
                return pos;
            }
            attempt++;
        }
        
        LOGGER.warn("Failed to find suitable location for {}", structure);
        return null;
    }

    private boolean isTooCloseToOtherStructures(BlockPos pos) {
        int minDistance = 10;
        for (BlockPos structurePos : culturalStructures.values()) {
            if (pos.getSquaredDistance(structurePos) < minDistance * minDistance) {
                return true;
            }
        }
        return false;
    }

    private boolean isTerrainSuitable(World world, BlockPos pos, String structure) {
        final int maxHeightDiff = 3;
        final int baseY = pos.getY();
        Box bounds = getStructureBounds(structure);
        int width = (int)bounds.maxX;
        int length = (int)bounds.maxZ;
        int height = (int)bounds.maxY;

        // Check area is relatively flat and has proper support
        int validBlocks = 0;
        int totalBlocks = 0;
        for (int x = -width/2; x <= width/2; x++) {
            for (int z = -length/2; z <= length/2; z++) {
                BlockPos checkPos = pos.add(x, 0, z);
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, checkPos.getX(), checkPos.getZ());
                
                // Check height difference
                if (Math.abs(y - baseY) > maxHeightDiff) {
                    return false;
                }
                
                // Check for water or lava
                if (world.getBlockState(checkPos).getBlock() == Blocks.WATER || 
                    world.getBlockState(checkPos).getBlock() == Blocks.LAVA) {
                    return false;
                }
                
                // Check ground stability
                BlockPos groundPos = checkPos.down();
                if (world.getBlockState(groundPos).isSideSolidFullSquare(world, groundPos, Direction.UP)) {
                    validBlocks++;
                }
                totalBlocks++;
            }
        }

        // Require at least 85% solid ground support
        float solidGroundRatio = (float)validBlocks / totalBlocks;
        if (solidGroundRatio < 0.85f) {
            return false;
        }

        // Culture-specific checks
        return switch(culture.toLowerCase()) {
            case "egyptian" -> {
                BlockPos sandCheck = pos.down();
                yield world.getBlockState(sandCheck).getBlock() == Blocks.SAND;
            }
            case "roman" -> {
                BlockPos stoneCheck = pos.down();
                yield world.getBlockState(stoneCheck).getBlock().getBlastResistance() >= 6.0f;
            }
            case "victorian" -> pos.getY() > world.getSeaLevel() + 2;
            case "nyc" -> isUrbanTerrain(world, pos) && hasAdequateSpace(world, pos, structure);
            default -> true;
        };
    }

    private boolean isUrbanTerrain(World world, BlockPos pos) {
        // Check for artificial/urban type blocks in the area
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos checkPos = pos.add(x, -1, z);
                Block block = world.getBlockState(checkPos).getBlock();
                if (block == Blocks.STONE_BRICKS || block == Blocks.SMOOTH_STONE || 
                    block == Blocks.STONE) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasAdequateSpace(World world, BlockPos pos, String structure) {
        Box bounds = getStructureBounds(structure);
        int width = (int)(bounds.maxX - bounds.minX);
        int length = (int)(bounds.maxZ - bounds.minZ);
        int height = (int)(bounds.maxY - bounds.minY);

        // For skyscrapers, check vertical clearance
        if (structure.toLowerCase().contains("skyscraper")) {
            for (int y = 0; y < height; y++) {
                BlockPos checkPos = pos.up(y);
                if (!world.getBlockState(checkPos).isAir()) {
                    return false;
                }
            }
        }

        // Check for adequate spacing between buildings
        int spacing = structure.toLowerCase().contains("skyscraper") ? 10 : 5;
        for (BlockPos existingPos : culturalStructures.values()) {
            double distance = Math.sqrt(pos.getSquaredDistance(existingPos));
            if (distance < width/2 + spacing) {
                return false;
            }
        }

        return true;
    }

    private Box getStructureBounds(String structure) {
        // Default sizes for different structure types
        Vec3d size = switch(structure.toLowerCase()) {
            case "temple", "forum", "palace" -> new Vec3d(15, 12, 15);
            case "house", "shop" -> new Vec3d(7, 5, 9);
            case "monument", "statue" -> new Vec3d(5, 8, 5);
            default -> new Vec3d(9, 6, 9);
        };
        return new Box(0, 0, 0, size.x, size.y, size.z);
    }

    private void generateCulturalStructure(World world, BlockPos pos, String structureName) {
        if (!(world instanceof ServerWorld serverWorld)) {
            LOGGER.error("Cannot generate structure in non-server world");
            return;
        }

        String path = String.format("structures/%s/%s", culture.toLowerCase(), 
            structureName.toLowerCase().replace(" ", "_"));
        Identifier templateId = Identifier.of("villagesreborn", path);
        
        try {
            Optional<StructureTemplate> template = serverWorld.getStructureTemplateManager()
                .getTemplate(templateId);
            
            if (template.isEmpty()) {
                LOGGER.warn("Template not found: {}, falling back to procedural generation", templateId);
                generateProceduralStructure(world, pos, structureName);
                return;
            }

            BlockPos placementPos = findGroundPosition(world, pos, template.get().getSize());
            StructurePlacementData placementData = new StructurePlacementData()
                .setIgnoreEntities(false)
                .setRotation(BlockRotation.random(serverWorld.getRandom()))
                .setMirror(BlockMirror.NONE);

            template.get().place(serverWorld, placementPos, placementPos, 
                placementData, serverWorld.getRandom(), Block.NOTIFY_ALL);
            
            Box boundingBox = new Box(
                placementPos.getX(), placementPos.getY(), placementPos.getZ(),
                placementPos.getX() + template.get().getSize().getX(),
                placementPos.getY() + template.get().getSize().getY(),
                placementPos.getZ() + template.get().getSize().getZ()
            );
            addStructurePointsOfInterest(world, placementPos, boundingBox);
            culturalStructures.put(structureName, placementPos);
        } catch (Exception e) {
            LOGGER.error("Error generating structure {}: {}", templateId, e.getMessage());
        }
    }

    private void generateProceduralStructure(World world, BlockPos pos, String structureName) {
        switch(culture.toLowerCase()) {
            case "roman" -> generateRomanStructure(world, pos, structureName);
            case "egyptian" -> generateEgyptianStructure(world, pos, structureName);
            case "victorian" -> generateVictorianStructure(world, pos, structureName);
            case "nyc" -> generateNYCStructure(world, pos, structureName);
            default -> LOGGER.error("Unknown culture for procedural generation: {}", culture);
        }
    }

    private BlockPos findGroundPosition(World world, BlockPos pos, Vec3i size) {
        int highestY = Integer.MIN_VALUE;
        int lowestY = Integer.MAX_VALUE;
        
        // Find the highest and lowest points in the building footprint
        for (int x = 0; x < size.getX(); x++) {
            for (int z = 0; z < size.getZ(); z++) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, 
                    pos.getX() + x, pos.getZ() + z);
                highestY = Math.max(highestY, y);
                lowestY = Math.min(lowestY, y);
            }
        }

        // If the terrain is too uneven, level it
        if (highestY - lowestY > 3) {
            int averageY = (highestY + lowestY) / 2;
            levelTerrain(world, pos, size, averageY);
            return new BlockPos(pos.getX(), averageY, pos.getZ());
        }

        return new BlockPos(pos.getX(), lowestY, pos.getZ());
    }

    private void levelTerrain(World world, BlockPos pos, Vec3i size, int targetY) {
        for (int x = 0; x < size.getX(); x++) {
            for (int z = 0; z < size.getZ(); z++) {
                BlockPos levelPos = new BlockPos(pos.getX() + x, targetY, pos.getZ() + z);
                
                // Fill below target height
                for (int y = targetY - 1; y >= targetY - 3; y--) {
                    world.setBlockState(new BlockPos(levelPos.getX(), y, levelPos.getZ()),
                        Blocks.DIRT.getDefaultState());
                }
                
                // Clear above target height
                for (int y = targetY + 1; y <= targetY + size.getY(); y++) {
                    world.setBlockState(new BlockPos(levelPos.getX(), y, levelPos.getZ()),
                        Blocks.AIR.getDefaultState());
                }
                
                // Set surface block
                world.setBlockState(levelPos, switch(culture.toLowerCase()) {
                    case "egyptian" -> Blocks.SAND.getDefaultState();
                    case "roman" -> Blocks.STONE.getDefaultState();
                    default -> Blocks.GRASS_BLOCK.getDefaultState();
                });
            }
        }
    }

    private void addStructurePointsOfInterest(World world, BlockPos pos, Box boundingBox) {
        addPointOfInterest(pos);
        addPointOfInterest(pos.add((int)boundingBox.maxX, 0, 0));
        addPointOfInterest(pos.add(0, 0, (int)boundingBox.maxZ));
        addPointOfInterest(pos.add((int)boundingBox.maxX, 0, (int)boundingBox.maxZ));
        addPointOfInterest(pos.add((int)(boundingBox.maxX / 2), 0, -1)); // Entrance POI
    }

    public void addPointOfInterest(BlockPos pos) {
        pointsOfInterest.add(pos);
        LOGGER.debug("Added point of interest at {}", pos);
    }

    public BlockPos getRandomSpawnLocation(World world) {
        int attempts = 0;
        BlockPos spawnPos;

        do {
            // Get random position within radius
            net.minecraft.util.math.random.Random worldRandom = world.getRandom();
            double angle = worldRandom.nextDouble() * Math.PI * 2;
            double distance = worldRandom.nextDouble() * radius;
            int x = center.getX() + (int)(Math.cos(angle) * distance);
            int z = center.getZ() + (int)(Math.sin(angle) * distance);
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);

            spawnPos = new BlockPos(x, y, z);
            attempts++;
        } while (!isValidSpawnLocation(world, spawnPos) && attempts < 10);

        if (attempts >= 10) {
            LOGGER.warn("Failed to find valid spawn location");
            return center; // Fallback to center if no valid location found
        }

        return spawnPos;
    }

    private boolean isValidSpawnLocation(World world, BlockPos pos) {
        // Check if the position is valid for spawning (solid ground, enough space)
        return world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos.down(), Direction.UP) &&
                world.isAir(pos) &&
                world.isAir(pos.up());
    }

    public BlockPos getCenter() {
        return center;
    }

    public int getRadius() {
        return radius;
    }

    public String getCulture() {
        return culture;
    }

    public Set<BlockPos> getPointsOfInterest() {
        return Collections.unmodifiableSet(pointsOfInterest);
    }

    public Map<String, BlockPos> getCulturalStructures() {
        return Collections.unmodifiableMap(culturalStructures);
    }

    public BlockPos getNearestPointOfInterest(BlockPos pos) {
        if (pointsOfInterest.isEmpty()) {
            return center;
        }

        BlockPos nearest = pointsOfInterest.iterator().next();
        double minDistance = pos.getSquaredDistance(nearest);

        for (BlockPos poi : pointsOfInterest) {
            double distance = pos.getSquaredDistance(poi);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = poi;
            }
        }

        return nearest;
    }

    public boolean isWithinRegion(BlockPos pos) {
        return pos.getSquaredDistance(center) <= radius * radius;
    }

    public String getDistrictAtPosition(BlockPos pos) {
        if (!isWithinRegion(pos)) {
            return null;
        }

        // Calculate relative position to center
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        double distance = Math.sqrt(dx * dx + dz * dz);

        // Determine district based on angle and distance
        if (distance < radius / 3) {
            return "central";
        }

        // Convert angle to positive degrees (0-360)
        angle = (angle + 360) % 360;

        if (angle < 90) {
            return "residential";
        } else if (angle < 180) {
            return "commercial";
        } else if (angle < 270) {
            return "industrial";
        } else {
            return "cultural";
        }
    }

    public void generateVillage(World world) {
        LOGGER.info("Starting generation of {} village at {}", culture, center);

        // First get the cultural layout plan
        generateLayoutPlan()
            .thenCompose(layout -> {
                // Generate the village center first
                String centerBuilding = (String) layout.get("CENTER");
                BlockPos centerPos = findBuildingLocation(world, centerBuilding);
                if (centerPos != null) {
                    generateCulturalStructure(world, centerPos, centerBuilding);
                    culturalStructures.put(centerBuilding, centerPos);
                    addPointOfInterest(centerPos);

                    // Then generate districts in a spiral pattern around center
                    @SuppressWarnings("unchecked")
                    List<String> districts = (List<String>) layout.get("DISTRICTS");
                    List<CompletableFuture<Void>> districtFutures = new ArrayList<>();
                    
                    int spiralRadius = Math.max(5, radius / districts.size());
                    for (int i = 0; i < districts.size(); i++) {
                        double angle = (2 * Math.PI * i) / districts.size();
                        int x = center.getX() + (int)(Math.cos(angle) * spiralRadius);
                        int z = center.getZ() + (int)(Math.sin(angle) * spiralRadius);
                        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
                        BlockPos districtPos = new BlockPos(x, y, z);
                        
                        String district = districts.get(i);
                        districtFutures.add(generateDistrict(world, district, layout));
                        
                        // Connect district to center with paths
                        generateCulturalPathways(world, centerPos, districtPos);
                    }

                    return CompletableFuture.allOf(districtFutures.toArray(new CompletableFuture[0]));
                }
                return CompletableFuture.completedFuture(null);
            })
            .thenRun(() -> {
                // Add decorative elements and final touches
                addCulturalDecorations(world);
                validateVillageIntegrity(world);
                LOGGER.info("Completed generation of {} village at {}", culture, center);
            })
            .exceptionally(e -> {
                LOGGER.error("Error generating village: {}", e.getMessage());
                return null;
            });
    }

    private void validateVillageIntegrity(World world) {
        // Verify all structures are properly connected
        Set<BlockPos> connected = new HashSet<>();
        connected.add(center);
        
        // Use breadth-first search to check connectivity
        Queue<BlockPos> toCheck = new LinkedList<>(connected);
        while (!toCheck.isEmpty()) {
            BlockPos current = toCheck.poll();
            for (BlockPos target : culturalStructures.values()) {
                if (!connected.contains(target) && isPathConnected(world, current, target)) {
                    connected.add(target);
                    toCheck.add(target);
                }
            }
        }

        // Fix any disconnected structures
        for (Map.Entry<String, BlockPos> structure : culturalStructures.entrySet()) {
            if (!connected.contains(structure.getValue())) {
                BlockPos nearest = findNearestConnected(structure.getValue(), connected);
                if (nearest != null) {
                    generateCulturalPathways(world, nearest, structure.getValue());
                    LOGGER.info("Fixed connectivity for {} at {}", structure.getKey(), structure.getValue());
                }
            }
        }
    }

    private boolean isPathConnected(World world, BlockPos start, BlockPos end) {
        PathStyle style = getCulturalPathStyle();
        int minDistance = (int) Math.sqrt(start.getSquaredDistance(end));
        
        // Check if there's a continuous path of path blocks
        Set<BlockPos> checked = new HashSet<>();
        Queue<BlockPos> toCheck = new LinkedList<>();
        toCheck.add(start);
        
        while (!toCheck.isEmpty() && checked.size() < minDistance * 3) {
            BlockPos current = toCheck.poll();
            if (current.isWithinDistance(end, 2)) {
                return true;
            }
            
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos next = current.offset(dir);
                if (!checked.contains(next)) {
                    int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, next.getX(), next.getZ());
                    BlockPos pathPos = new BlockPos(next.getX(), y - 1, next.getZ());
                    Block block = world.getBlockState(pathPos).getBlock();
                    
                    if (Arrays.asList(style.blocks).contains(block)) {
                        checked.add(next);
                        toCheck.add(next);
                    }
                }
            }
        }
        
        return false;
    }

    private BlockPos findNearestConnected(BlockPos pos, Set<BlockPos> connected) {
        return connected.stream()
            .min(Comparator.comparingDouble(p -> p.getSquaredDistance(pos)))
            .orElse(null);
    }

    private CompletableFuture<Void> generateDistrict(World world, String district, Map<String, Object> layout) {
        String prompt = String.format(
            "Design a %s district for a %s village in Minecraft.\n" +
            "The village has a %s layout style.\n" +
            "Describe:\n" +
            "1. What buildings should be in this district\n" +
            "2. How they should be arranged\n" +
            "3. Special features or decorations\n" +
            "Keep descriptions Minecraft-appropriate and buildable.",
            district, culture, layout.get("LAYOUT")
        );

        return LLMService.getInstance()
            .generateResponse(prompt)
            .thenAccept(description -> {
                BlockPos districtPos = findDistrictLocation(world, district);
                generateDistrictFromDescription(world, districtPos, description);
            });
    }

    private void generateCulturalPathways(World world) {
        String prompt = String.format(
            "Design pathways for a %s village in Minecraft.\n" +
            "What blocks and patterns would best represent %s pathways?\n" +
            "Format response as:\n" +
            "BLOCKS: (comma-separated list)\n" +
            "PATTERN: (linear/winding/grid)\n" +
            "FEATURES: (special features along paths)",
            culture, culture
        );

        LLMService.getInstance()
            .generateResponse(prompt)
            .thenAccept(response -> {
                Map<String, String> pathDetails = parseResponse(response);
                generatePathsFromDescription(world, pathDetails);
            });
    }

    private void generatePathsFromDescription(World world, Map<String, String> pathDetails) {
        List<Block> pathBlocks = Arrays.stream(pathDetails.get("BLOCKS").split(","))
            .map(String::trim)
            .map(this::getBlockFromName)
            .collect(Collectors.toList());

        String pattern = pathDetails.getOrDefault("PATTERN", "linear");
        List<BlockPos> pathPoints = new ArrayList<>();

        switch (pattern.toLowerCase()) {
            case "grid" -> generateGridPaths(pathPoints);
            case "winding" -> generateWindingPaths(pathPoints, world.getRandom());
            default -> generateLinearPaths(pathPoints);
        }

        for (BlockPos pathPos : pathPoints) {
            Block block = pathBlocks.get(world.getRandom().nextInt(pathBlocks.size()));
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pathPos.getX(), pathPos.getZ());
            world.setBlockState(new BlockPos(pathPos.getX(), y - 1, pathPos.getZ()), block.getDefaultState());
        }
    }

    private List<BlockPos> generatePathsForPattern(String pattern, World world) {
        List<BlockPos> points = new ArrayList<>();
        
        switch (pattern.toLowerCase()) {
            case "grid" -> generateGridPaths(points);
            case "winding" -> generateWindingPaths(points, world.getRandom());
            default -> generateLinearPaths(points);
        }

        return points;
    }

    private void generateGridPaths(List<BlockPos> points) {
        int spacing = 10;
        for (int x = -radius; x <= radius; x += spacing) {
            for (int z = -radius; z <= radius; z++) {
                points.add(center.add(x, 0, z));
            }
        }
        for (int z = -radius; z <= radius; z += spacing) {
            for (int x = -radius; x <= radius; x++) {
                points.add(center.add(x, 0, z));
            }
        }
    }

    private void generateWindingPaths(List<BlockPos> points, net.minecraft.util.math.random.Random random) {
        double angle = 0;
        double x = center.getX();
        double z = center.getZ();

        for (int i = 0; i < radius * 4; i++) {
            points.add(new BlockPos((int)x, 0, (int)z));
            angle += (random.nextDouble() - 0.5) * 0.5;
            x += Math.cos(angle) * 2;
            z += Math.sin(angle) * 2;
        }
    }

    private void generateLinearPaths(List<BlockPos> points) {
        for (int x = -radius; x <= radius; x++) {
            points.add(center.add(x, 0, 0));
        }
        for (int z = -radius; z <= radius; z++) {
            points.add(center.add(0, 0, z));
        }
    }

    private Map<String, String> parseResponse(String response) {
        return Arrays.stream(response.split("\n"))
            .map(line -> line.split(": "))
            .filter(parts -> parts.length == 2)
            .collect(Collectors.toMap(
                parts -> parts[0],
                parts -> parts[1]
            ));
    }

    private BlockPos findDistrictLocation(World world, String district) {
        // Placeholder for finding district location logic
        net.minecraft.util.math.random.Random worldRandom = world.getRandom();
        return center.add(worldRandom.nextInt(radius), 0, worldRandom.nextInt(radius));
    }

    private void generateDistrictFromDescription(World world, BlockPos pos, String description) {
        // Placeholder for generating district from description
        LOGGER.info("Generating {} district at {}", description, pos);
    }

    private void generateBuildingFromDescription(World world, BlockPos pos, List<String> description) {
        // Placeholder for generating building from description
        LOGGER.info("Generating building at {} with description: {}", pos, description);
    }

    private CompletableFuture<List<String>> generateCulturalBuildingDescription(String buildingName) {
        String prompt = String.format(
            "You are designing a %s building in a %s village in Minecraft.\n" +
            "Consider local cultural adaptations and unique features.\n" +
            "Describe:\n" +
            "1. How this building would be uniquely %s in style\n" +
            "2. What blocks and decorations would best represent this culture\n" +
            "3. Special architectural features unique to this culture\n" +
            "Keep each point brief and minecraft-realistic.",
            buildingName, culture, culture
        );

        return LLMService.getInstance()
            .generateResponse(prompt)
            .thenApply(response -> Arrays.asList(response.split("\n")));
    }

    private CompletableFuture<Map<String, Object>> generateLayoutPlan() {
        String prompt = String.format(
            "Design a %s village layout in Minecraft considering:\n" +
            "1. Historical accuracy and cultural authenticity\n" +
            "2. Social hierarchy and community spaces\n" +
            "3. Local architectural patterns\n" +
            "4. Religious or ceremonial requirements\n" +
            "5. Climate adaptations\n" +
            "Format response as:\n" +
            "LAYOUT: (grid/organic/radial/hierarchical)\n" +
            "CENTER: (main cultural building)\n" +
            "DISTRICTS: (comma-separated list)\n" +
            "ZONING: (district arrangement rules)\n" +
            "PATHWAYS: (road style and patterns)\n" +
            "INFRASTRUCTURE: (water/drainage/defenses)",
            culture
        );

        return LLMService.getInstance()
            .generateResponse(prompt)
            .thenApply(response -> {
                Map<String, String> params = Arrays.stream(response.split("\n"))
                    .map(line -> line.split(": "))
                    .filter(parts -> parts.length == 2)
                    .collect(Collectors.toMap(
                        parts -> parts[0],
                        parts -> parts[1]
                    ));

                Map<String, Object> layout = new HashMap<>();
                layout.put("LAYOUT", params.get("LAYOUT"));
                layout.put("CENTER", params.get("CENTER"));
                layout.put("DISTRICTS", Arrays.asList(params.get("DISTRICTS").split(", ")));
                layout.put("ZONING", params.get("ZONING"));
                layout.put("PATHWAYS", params.get("PATHWAYS"));
                layout.put("INFRASTRUCTURE", params.get("INFRASTRUCTURE"));

                // Apply cultural constraints to layout
                applyCulturalLayoutRules(layout);
                
                return layout;
            });
    }

    private void applyCulturalLayoutRules(Map<String, Object> layout) {
        switch(culture.toLowerCase()) {
            case "roman" -> {
                if (!layout.get("LAYOUT").equals("grid")) {
                    layout.put("LAYOUT", "grid");  // Romans preferred grid layouts
                }
                if (!((List<String>)layout.get("DISTRICTS")).contains("forum")) {
                    ((List<String>)layout.get("DISTRICTS")).add(0, "forum");
                }
            }
            case "egyptian" -> {
                if (!((String)layout.get("INFRASTRUCTURE")).contains("irrigation")) {
                    layout.put("INFRASTRUCTURE", 
                        ((String)layout.get("INFRASTRUCTURE")) + ", irrigation systems");
                }
            }
            case "victorian" -> {
                if (!((List<String>)layout.get("DISTRICTS")).contains("industrial")) {
                    ((List<String>)layout.get("DISTRICTS")).add("industrial");
                }
                layout.put("ZONING", "residential separate from industrial");
            }
            case "nyc" -> {
                layout.put("LAYOUT", "grid");  // NYC's iconic grid system
                if (!((String)layout.get("INFRASTRUCTURE")).contains("subway")) {
                    layout.put("INFRASTRUCTURE", 
                        ((String)layout.get("INFRASTRUCTURE")) + ", subway system");
                }
            }
        }
    }

    private void addCulturalDecorations(World world) {
        // Placeholder for adding cultural decorations
        LOGGER.info("Adding cultural decorations for {} village", culture);
    }

    private void generateRomanVillage(World world) {
        LOGGER.info("Generating Roman village at {}", center);

        // Generate Forum (central plaza)
        generateForum(world);

        // Generate main roads (cardo and decumanus)
        generateRomanRoads(world);

        // Generate buildings around the forum
        generateRomanBuildings(world);

        // Add decorative elements
        addRomanDecorations(world);
    }

    private void generateForum(World world) {
        int forumSize = Math.max(5, radius / 3);

        // Create forum platform
        for (int x = center.getX() - forumSize; x <= center.getX() + forumSize; x++) {
            for (int z = center.getZ() - forumSize; z <= center.getZ() + forumSize; z++) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos pos = new BlockPos(x, y - 1, z);

                // Forum floor using polished stone and regular stone bricks
                if ((x + z) % 2 == 0) {
                    world.setBlockState(pos, Blocks.POLISHED_ANDESITE.getDefaultState());
                } else {
                    world.setBlockState(pos, Blocks.STONE_BRICKS.getDefaultState());
                }

                // Add columns around the forum
                if ((x == center.getX() - forumSize || x == center.getX() + forumSize ||
                        z == center.getZ() - forumSize || z == center.getZ() + forumSize) &&
                        (x + z) % 3 == 0) {
                    generateRomanColumn(world, new BlockPos(x, y, z));
                }
            }
        }
    }

    private void generateRomanRoads(World world) {
        int roadLength = radius;
        int roadWidth = 3;

        // Generate cardo (north-south road)
        for (int z = center.getZ() - roadLength; z <= center.getZ() + roadLength; z++) {
            for (int x = center.getX() - roadWidth; x <= center.getX() + roadWidth; x++) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos pos = new BlockPos(x, y - 1, z);
                world.setBlockState(pos, Blocks.STONE_BRICKS.getDefaultState());

                // Add road patterns
                if (x == center.getX() && z % 4 == 0) {
                    world.setBlockState(pos, Blocks.POLISHED_GRANITE.getDefaultState());
                }
            }
        }

        // Generate decumanus (east-west road)
        for (int x = center.getX() - roadLength; x <= center.getX() + roadLength; x++) {
            for (int z = center.getZ() - roadWidth; z <= center.getZ() + roadWidth; z++) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos pos = new BlockPos(x, y - 1, z);
                world.setBlockState(pos, Blocks.STONE_BRICKS.getDefaultState());

                // Add road patterns
                if (z == center.getZ() && x % 4 == 0) {
                    world.setBlockState(pos, Blocks.POLISHED_GRANITE.getDefaultState());
                }
            }
        }
    }

    private void generateRomanBuildings(World world) {
        int buildingRadius = radius - 5;
        net.minecraft.util.math.random.Random random = world.getRandom();

        // Generate buildings in each quadrant
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            int quadX = (quadrant % 2 == 0) ? -1 : 1;
            int quadZ = (quadrant < 2) ? -1 : 1;

            // Generate 2-3 buildings per quadrant
            int buildings = random.nextInt(2) + 2;
            for (int i = 0; i < buildings; i++) {
                int offsetX = random.nextInt(buildingRadius / 2) + buildingRadius / 4;
                int offsetZ = random.nextInt(buildingRadius / 2) + buildingRadius / 4;

                BlockPos buildingPos = center.add(
                        offsetX * quadX,
                        0,
                        offsetZ * quadZ
                );

                generateRomanHouse(world, buildingPos, random);
            }
        }
    }


    private void generateRomanHouse(World world, BlockPos pos, net.minecraft.util.math.random.Random random) {
        int houseWidth = random.nextInt(3) + 5;
        int houseLength = random.nextInt(3) + 6;
        int houseHeight = random.nextInt(2) + 3;

        // Generate main structure
        for (int x = -houseWidth; x <= houseWidth; x++) {
            for (int z = -houseLength; z <= houseLength; z++) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + x, pos.getZ() + z);

                // Create walls
                for (int dy = 0; dy < houseHeight; dy++) {
                    BlockPos buildPos = new BlockPos(pos.getX() + x, y + dy, pos.getZ() + z);

                    if (x == -houseWidth || x == houseWidth || z == -houseLength || z == houseLength) {
                        world.setBlockState(buildPos, Blocks.STONE_BRICKS.getDefaultState());
                    } else if (dy == 0) {
                        // Floor
                        world.setBlockState(buildPos, Blocks.SMOOTH_STONE.getDefaultState());
                    } else if (dy == houseHeight - 1) {
                        // Roof
                        world.setBlockState(buildPos, Blocks.STONE_BRICK_STAIRS.getDefaultState());
                    } else {
                        // Interior
                        world.setBlockState(buildPos, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
    }

    private void generateRomanColumn(World world, BlockPos base) {
        int columnHeight = 4;

        // Generate column base
        world.setBlockState(base, Blocks.STONE_BRICK_STAIRS.getDefaultState());

        // Generate column shaft
        for (int y = 1; y < columnHeight; y++) {
            world.setBlockState(base.up(y), Blocks.STONE_BRICKS.getDefaultState());
        }

        // Generate column capital
        world.setBlockState(base.up(columnHeight), Blocks.CHISELED_STONE_BRICKS.getDefaultState());
    }

    private void addRomanDecorations(World world) {
        net.minecraft.util.math.random.Random random = world.getRandom();

        // Add decorative elements around the forum
        for (int i = 0; i < radius / 2; i++) {
            int x = center.getX() + random.nextInt(radius) - radius / 2;
            int z = center.getZ() + random.nextInt(radius) - radius / 2;
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);

            // Add flower pots, lanterns, or other decorations
            if (random.nextBoolean()) {
                world.setBlockState(new BlockPos(x, y, z), Blocks.FLOWER_POT.getDefaultState());
            } else {
                world.setBlockState(new BlockPos(x, y, z), Blocks.LANTERN.getDefaultState());
            }
        }
    }

    private void generateRomanStructure(World world, BlockPos pos, String structureName) {
        switch(structureName.toLowerCase()) {
            case "temple" -> generateRomanTemple(world, pos);
            case "villa" -> generateRomanVilla(world, pos);
            case "bathhouse" -> generateRomanBathhouse(world, pos);
            case "market" -> generateRomanMarket(world, pos);
            default -> generateRomanHouse(world, pos, world.getRandom());
        }
    }

    private void generateRomanTemple(World world, BlockPos pos) {
        int width = 11;
        int length = 15;
        int height = 8;
        
        // Generate podium
        for (int x = -width/2; x <= width/2; x++) {
            for (int z = -length/2; z <= length/2; z++) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + x, pos.getZ() + z);
                for (int dy = -1; dy < 1; dy++) {
                    BlockPos podiumPos = new BlockPos(pos.getX() + x, y + dy, pos.getZ() + z);
                    world.setBlockState(podiumPos, Blocks.STONE_BRICKS.getDefaultState());
                }
            }
        }

        // Generate columns
        for (int x = -width/2; x <= width/2; x += 2) {
            for (int z : new int[]{-length/2, length/2}) {
                generateRomanColumn(world, new BlockPos(pos.getX() + x, 
                    world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + x, pos.getZ() + z), 
                    pos.getZ() + z));
            }
        }

        // Generate roof
        for (int x = -width/2; x <= width/2; x++) {
            for (int z = -length/2; z <= length/2; z++) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + x, pos.getZ() + z);
                BlockPos roofPos = new BlockPos(pos.getX() + x, y + height - 1, pos.getZ() + z);
                world.setBlockState(roofPos, Blocks.STONE_BRICK_SLAB.getDefaultState());
            }
        }
    }

    private void generateRomanVilla(World world, BlockPos pos) {
        int width = 13;
        int length = 17;
        int height = 6;

        // Generate main structure
        for (int x = -width/2; x <= width/2; x++) {
            for (int z = -length/2; z <= length/2; z++) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + x, pos.getZ() + z);
                
                // Foundation
                world.setBlockState(new BlockPos(pos.getX() + x, y - 1, pos.getZ() + z), 
                    Blocks.STONE_BRICKS.getDefaultState());

                // Walls
                if (x == -width/2 || x == width/2 || z == -length/2 || z == length/2) {
                    for (int dy = 0; dy < height; dy++) {
                        world.setBlockState(new BlockPos(pos.getX() + x, y + dy, pos.getZ() + z),
                            Blocks.STONE_BRICKS.getDefaultState());
                    }
                }
            }
        }

        // Generate interior courtyard
        int courtyardWidth = 5;
        int courtyardLength = 7;
        for (int x = -courtyardWidth/2; x <= courtyardWidth/2; x++) {
            for (int z = -courtyardLength/2; z <= courtyardLength/2; z++) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, 
                    pos.getX() + x, pos.getZ() + z);
                // Place water feature in center
                if (x == 0 && z == 0) {
                    world.setBlockState(new BlockPos(pos.getX() + x, y, pos.getZ() + z),
                        Blocks.WATER.getDefaultState());
                } else {
                    world.setBlockState(new BlockPos(pos.getX() + x, y - 1, pos.getZ() + z),
                        Blocks.STONE_BRICKS.getDefaultState());
                }
            }
        }
    }

    private void generateRomanBathhouse(World world, BlockPos pos) {
        int width = 9;
        int length = 13;
        int height = 5;

        // Generate main structure
        for (int x = -width/2; x <= width/2; x++) {
            for (int z = -length/2; z <= length/2; z++) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + x, pos.getZ() + z);
                
                // Foundation and floor
                world.setBlockState(new BlockPos(pos.getX() + x, y - 1, pos.getZ() + z),
                    Blocks.STONE_BRICKS.getDefaultState());

                // Walls
                if (x == -width/2 || x == width/2 || z == -length/2 || z == length/2) {
                    for (int dy = 0; dy < height; dy++) {
                        world.setBlockState(new BlockPos(pos.getX() + x, y + dy, pos.getZ() + z),
                            dy == height - 1 ? Blocks.STONE_BRICK_STAIRS.getDefaultState() :
                            Blocks.STONE_BRICKS.getDefaultState());
                    }
                }

                // Water pools
                if (Math.abs(x) < width/3 && Math.abs(z) < length/3) {
                    world.setBlockState(new BlockPos(pos.getX() + x, y, pos.getZ() + z),
                        Blocks.WATER.getDefaultState());
                }
            }
        }
    }

    private void generateRomanMarket(World world, BlockPos pos) {
        int width = 15;
        int length = 15;

        // Generate market square
        for (int x = -width/2; x <= width/2; x++) {
            for (int z = -length/2; z <= length/2; z++) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + x, pos.getZ() + z);
                
                // Market floor
                world.setBlockState(new BlockPos(pos.getX() + x, y - 1, pos.getZ() + z),
                    (x + z) % 2 == 0 ? Blocks.STONE_BRICKS.getDefaultState() : 
                    Blocks.SMOOTH_STONE.getDefaultState());

                // Market stalls on the edges
                if ((Math.abs(x) == width/2 || Math.abs(z) == length/2) && 
                    (x + z) % 3 == 0) {
                    generateMarketStall(world, new BlockPos(pos.getX() + x, y, pos.getZ() + z));
                }
            }
        }
    }

    private void generateMarketStall(World world, BlockPos pos) {
        // Generate small market stall structure
        for (int y = 0; y < 3; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (y == 2) {
                        world.setBlockState(new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z),
                            Blocks.OAK_SLAB.getDefaultState());
                    } else if (y == 1) {
                        if (x == 0 && z == 0) continue; // Empty space
                        world.setBlockState(new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z),
                            Blocks.OAK_FENCE.getDefaultState());
                    } else {
                        if (x == 0 && z == 0) {
                            world.setBlockState(new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z),
                                Blocks.CRAFTING_TABLE.getDefaultState());
                        } else {
                            world.setBlockState(new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z),
                                Blocks.OAK_PLANKS.getDefaultState());
                        }
                    }
                }
            }
        }
    }

    private void generateEgyptianVillage(World world) {
        LOGGER.info("Generating Egyptian village at {}", center);

        // Generate Great Temple complex
        generateGreatTemple(world);

        // Generate Pyramid complex
        generatePyramidComplex(world);

        // Generate residential district
        generateEgyptianResidential(world);

        // Generate bazaar
        generateBazaar(world);

        // Generate oasis
        generateOasis(world);

        // Add hieroglyph decorations
        addEgyptianDecorations(world);
    }

    private void generateGreatTemple(World world) {
        int templeSize = Math.max(7, radius / 4);
        BlockPos templePos = center.add(radius / 3, 0, radius / 3);

        // Generate temple base and pylons
        for (int x = -templeSize; x <= templeSize; x++) {
            for (int z = -templeSize; z <= templeSize; z++) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, templePos.getX() + x, templePos.getZ() + z);
                BlockPos pos = new BlockPos(templePos.getX() + x, y - 1, templePos.getZ() + z);

                // Temple floor
                world.setBlockState(pos, Blocks.SANDSTONE.getDefaultState());

                // Pylons at the entrance
                if (z == -templeSize && (x == -templeSize/2 || x == templeSize/2)) {
                    generatePylon(world, pos);
                }
            }
        }
    }

    private void generatePylon(World world, BlockPos base) {
        int height = 8;
        for (int y = 0; y < height; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    world.setBlockState(base.add(x, y, z), Blocks.SMOOTH_SANDSTONE.getDefaultState());
                }
            }
        }
    }

    private void generatePyramidComplex(World world) {
        BlockPos pyramidPos = center.add(-radius / 2, 0, -radius / 2);
        generatePyramid(world, pyramidPos, 15);

        // Generate satellite pyramids
        generatePyramid(world, pyramidPos.add(10, 0, 10), 7);
        generatePyramid(world, pyramidPos.add(-10, 0, 10), 7);
    }

    private void generatePyramid(World world, BlockPos base, int size) {
        for (int y = 0; y < size; y++) {
            for (int x = -size + y; x <= size - y; x++) {
                for (int z = -size + y; z <= size - y; z++) {
                    world.setBlockState(base.add(x, y, z), Blocks.SANDSTONE.getDefaultState());
                }
            }
        }
    }

    private void generateEgyptianResidential(World world) {
        net.minecraft.util.math.random.Random random = net.minecraft.util.math.random.Random.create();
        int houseCount = radius / 4;

        for (int i = 0; i < houseCount; i++) {
            int x = center.getX() + random.nextInt(radius) - radius / 2;
            int z = center.getZ() + random.nextInt(radius) - radius / 2;
            generateMudBrickHouse(world, new BlockPos(x, 0, z));
        }
    }

    private void generateMudBrickHouse(World world, BlockPos pos) {
        int size = 5;
        int height = 3;
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());

        for (int x = -size/2; x <= size/2; x++) {
            for (int z = -size/2; z <= size/2; z++) {
                for (int dy = 0; dy < height; dy++) {
                    BlockPos buildPos = new BlockPos(pos.getX() + x, y + dy, pos.getZ() + z);
                    world.setBlockState(buildPos, Blocks.TERRACOTTA.getDefaultState());
                }
            }
        }
    }

    private void generateBazaar(World world) {
        int bazaarSize = radius / 4;
        BlockPos bazaarPos = center.add(0, 0, radius / 3);

        for (int x = -bazaarSize; x <= bazaarSize; x++) {
            for (int z = -bazaarSize; z <= bazaarSize; z++) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, bazaarPos.getX() + x, bazaarPos.getZ() + z);
                BlockPos pos = new BlockPos(bazaarPos.getX() + x, y - 1, bazaarPos.getZ() + z);
                world.setBlockState(pos, Blocks.SMOOTH_SANDSTONE.getDefaultState());
            }
        }
    }

    private void generateOasis(World world) {
        net.minecraft.util.math.random.Random random = world.getRandom();
        BlockPos oasisPos = center.add(-radius / 3, 0, 0);
        int oasisSize = 8;

        // Generate water pool
        for (int x = -oasisSize/2; x <= oasisSize/2; x++) {
            for (int z = -oasisSize/2; z <= oasisSize/2; z++) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, oasisPos.getX() + x, oasisPos.getZ() + z);
                BlockPos pos = new BlockPos(oasisPos.getX() + x, y - 1, oasisPos.getZ() + z);
                world.setBlockState(pos, Blocks.WATER.getDefaultState());
            }
        }

        // Add palm trees
        for (int i = 0; i < 5; i++) {
            int x = oasisPos.getX() + random.nextInt(oasisSize) - oasisSize/2;
            int z = oasisPos.getZ() + random.nextInt(oasisSize) - oasisSize/2;
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            generatePalmTree(world, new BlockPos(x, y, z));
        }
    }

    private void generatePalmTree(World world, BlockPos base) {
        int height = 5;
        // Generate trunk
        for (int y = 0; y < height; y++) {
            world.setBlockState(base.up(y), Blocks.JUNGLE_LOG.getDefaultState());
        }
        // Generate leaves
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) + Math.abs(z) <= 3) {
                    world.setBlockState(base.add(x, height, z), Blocks.JUNGLE_LEAVES.getDefaultState());
                }
            }
        }
    }
private void addEgyptianDecorations(World world) {
    net.minecraft.util.math.random.Random random = net.minecraft.util.math.random.Random.create();
    for (int i = 0; i < radius / 3; i++) {
        int x = center.getX() + random.nextInt(radius) - radius / 2;
        int z = center.getZ() + random.nextInt(radius) - radius / 2;
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        
        if (random.nextBoolean()) {
            world.setBlockState(new BlockPos(x, y, z), Blocks.CHISELED_SANDSTONE.getDefaultState());
        }
    }
}

private void generateEgyptianStructure(World world, BlockPos pos, String structureName) {
    switch(structureName.toLowerCase()) {
        case "temple" -> generateEgyptianTemple(world, pos);
        case "pyramid" -> generatePyramid(world, pos, 15);
        case "obelisk" -> generateObelisk(world, pos);
        case "palace" -> generateEgyptianPalace(world, pos);
        default -> generateMudBrickHouse(world, pos);
    }
}

private void generateEgyptianTemple(World world, BlockPos pos) {
    int width = 13;
    int length = 17;
    int height = 10;

    // Generate main temple structure
    for (int x = -width/2; x <= width/2; x++) {
        for (int z = -length/2; z <= length/2; z++) {
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + x, pos.getZ() + z);
            
            // Foundation
            for (int dy = -1; dy < 1; dy++) {
                world.setBlockState(new BlockPos(pos.getX() + x, y + dy, pos.getZ() + z),
                    Blocks.SANDSTONE.getDefaultState());
            }

            // Walls
            if (x == -width/2 || x == width/2 || z == -length/2 || z == length/2) {
                for (int dy = 1; dy < height; dy++) {
                    world.setBlockState(new BlockPos(pos.getX() + x, y + dy, pos.getZ() + z),
                        Blocks.SMOOTH_SANDSTONE.getDefaultState());
                    
                    // Add hieroglyph decorations
                    if (dy > height/2 && (x + z) % 2 == 0) {
                        world.setBlockState(new BlockPos(pos.getX() + x, y + dy, pos.getZ() + z),
                            Blocks.CHISELED_SANDSTONE.getDefaultState());
                    }
                }
            }
        }
    }

    // Generate entrance pylons
    generatePylon(world, new BlockPos(pos.getX() - width/2, pos.getY(), pos.getZ() - length/2));
    generatePylon(world, new BlockPos(pos.getX() + width/2, pos.getY(), pos.getZ() - length/2));
}

private void generateObelisk(World world, BlockPos pos) {
    int height = 12;
    int baseWidth = 3;

    // Generate base
    for (int x = -baseWidth/2; x <= baseWidth/2; x++) {
        for (int z = -baseWidth/2; z <= baseWidth/2; z++) {
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + x, pos.getZ() + z);
            world.setBlockState(new BlockPos(pos.getX() + x, y - 1, pos.getZ() + z),
                Blocks.SMOOTH_SANDSTONE.getDefaultState());
        }
    }

    // Generate obelisk shaft
    for (int y = 0; y < height; y++) {
        int width = Math.max(1, baseWidth - (y / 4));
        for (int x = -width/2; x <= width/2; x++) {
            for (int z = -width/2; z <= width/2; z++) {
                world.setBlockState(new BlockPos(pos.getX() + x, 
                    world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + x, pos.getZ() + z) + y,
                    pos.getZ() + z),
                    y == height - 1 ? Blocks.GOLD_BLOCK.getDefaultState() : 
                    Blocks.SMOOTH_SANDSTONE.getDefaultState());
            }
        }
    }
}

private void generateEgyptianPalace(World world, BlockPos pos) {
    int width = 15;
    int length = 19;
    int height = 7;

    // Generate main palace structure
    for (int x = -width/2; x <= width/2; x++) {
        for (int z = -length/2; z <= length/2; z++) {
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + x, pos.getZ() + z);
            
            // Foundation and floor
            for (int dy = -1; dy < 1; dy++) {
                world.setBlockState(new BlockPos(pos.getX() + x, y + dy, pos.getZ() + z),
                    Blocks.SMOOTH_SANDSTONE.getDefaultState());
            }

            // Walls
            if (x == -width/2 || x == width/2 || z == -length/2 || z == length/2) {
                for (int dy = 1; dy < height; dy++) {
                    world.setBlockState(new BlockPos(pos.getX() + x, y + dy, pos.getZ() + z),
                        Blocks.SANDSTONE.getDefaultState());
                }
            }
        }
    }

    // Generate courtyards
    generateEgyptianCourtyard(world, new BlockPos(pos.getX() - width/4, pos.getY(), pos.getZ()));
    generateEgyptianCourtyard(world, new BlockPos(pos.getX() + width/4, pos.getY(), pos.getZ()));
}

private void generateEgyptianCourtyard(World world, BlockPos pos) {
    int size = 5;
    
    for (int x = -size/2; x <= size/2; x++) {
        for (int z = -size/2; z <= size/2; z++) {
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + x, pos.getZ() + z);
            
            // Create water feature in center
            if (x == 0 && z == 0) {
                world.setBlockState(new BlockPos(pos.getX() + x, y - 1, pos.getZ() + z),
                    Blocks.WATER.getDefaultState());
            } else {
                world.setBlockState(new BlockPos(pos.getX() + x, y - 1, pos.getZ() + z),
                    Blocks.SMOOTH_SANDSTONE.getDefaultState());
            }
            
            // Add pillars at corners
            if ((Math.abs(x) == size/2 && Math.abs(z) == size/2)) {
                for (int dy = 0; dy < 3; dy++) {
                    world.setBlockState(new BlockPos(pos.getX() + x, y + dy, pos.getZ() + z),
                        Blocks.SANDSTONE_WALL.getDefaultState());
                }
            }
        }
    }
}

private void generateVictorianVillage(World world) {
    LOGGER.info("Generating Victorian village at {}", center);

    // Generate town square with clock tower
    generateTownSquare(world);

    // Generate terraced houses
    generateTerracedHouses(world);

    // Generate factory district
    generateFactoryDistrict(world);

    // Generate Victorian church
    BlockPos churchPos = center.add(-radius/2, 0, -radius/3);
    generateVictorianChurch(world, churchPos);

    // Generate traditional pub
    generateTraditionalPub(world);

    // Generate Victorian park
    generateVictorianPark(world);

    // Generate streets
    generateCobblestoneStreets(world);

    // Add decorations
    addVictorianDecorations(world);
}

private void generateTownSquare(World world) {
    int squareSize = Math.max(8, radius / 4);
    
    // Generate square pavement
    for (int x = -squareSize; x <= squareSize; x++) {
        for (int z = -squareSize; z <= squareSize; z++) {
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, center.getX() + x, center.getZ() + z);
            BlockPos pos = new BlockPos(center.getX() + x, y - 1, center.getZ() + z);
            world.setBlockState(pos, Blocks.STONE_BRICKS.getDefaultState());
        }
    }

    // Generate clock tower
    generateClockTower(world, center);
}

private void generateClockTower(World world, BlockPos base) {
    int height = 12;
    int size = 3;

    for (int y = 0; y < height; y++) {
        for (int x = -size/2; x <= size/2; x++) {
            for (int z = -size/2; z <= size/2; z++) {
                    BlockPos buildPos = base.add(x, y, z);
                    if (y < height - 1) {
                        world.setBlockState(buildPos, Blocks.STONE_BRICKS.getDefaultState());
                    } else {
                        world.setBlockState(buildPos, Blocks.GOLD_BLOCK.getDefaultState());
                }
            }
        }
    }
}

private void generateTerracedHouses(World world) {
    int rowLength = radius / 2;
    int houseWidth = 5;
    int houseDepth = 8;
    
    // Generate rows of houses
    for (int x = -rowLength; x < rowLength; x += houseWidth) {
        generateTerracedHouse(world, center.add(x, 0, radius/3), houseWidth, houseDepth);
        generateTerracedHouse(world, center.add(x, 0, -radius/3), houseWidth, houseDepth);
    }
}

private void generateTerracedHouse(World world, BlockPos pos, int width, int depth) {
    int height = 6;
    
    for (int x = 0; x < width; x++) {
        for (int z = 0; z < depth; z++) {
            for (int y = 0; y < height; y++) {
                int baseY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + x, pos.getZ() + z);
                BlockPos buildPos = new BlockPos(pos.getX() + x, baseY + y - 1, pos.getZ() + z);
                
                if (y == 0) {
                    world.setBlockState(buildPos, Blocks.STONE_BRICKS.getDefaultState());
                } else if (y == height - 1) {
                    world.setBlockState(buildPos, Blocks.BRICK_STAIRS.getDefaultState());
                } else {
                    world.setBlockState(buildPos, Blocks.BRICKS.getDefaultState());
                }
            }
        }
    }
}

private void generateFactoryDistrict(World world) {
    BlockPos factoryPos = center.add(radius/2, 0, radius/2);
    int factorySize = 12;
    int height = 10;

    // Main factory building
    for (int x = -factorySize/2; x <= factorySize/2; x++) {
        for (int z = -factorySize/2; z <= factorySize/2; z++) {
            for (int y = 0; y < height; y++) {
                int baseY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, factoryPos.getX() + x, factoryPos.getZ() + z);
                BlockPos pos = new BlockPos(factoryPos.getX() + x, baseY + y - 1, factoryPos.getZ() + z);
                world.setBlockState(pos, Blocks.STONE_BRICKS.getDefaultState());
            }
        }
    }

    // Chimney
    generateFactoryChimney(world, factoryPos.add(factorySize/2, height, factorySize/2));
}

private void generateFactoryChimney(World world, BlockPos base) {
    int chimneyHeight = 6;
    for (int y = 0; y < chimneyHeight; y++) {
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                world.setBlockState(base.add(x, y, z), Blocks.BRICKS.getDefaultState());
            }
        }
    }
}

private void generateVictorianChurch(World world, BlockPos pos) {
    int width = 8;
    int length = 12;
    int height = 8;
    BlockPos churchPos = pos;

    // Main building
    for (int x = -width/2; x <= width/2; x++) {
        for (int z = -length/2; z <= length/2; z++) {
            for (int y = 0; y < height; y++) {
                int baseY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, churchPos.getX() + x, churchPos.getZ() + z);
                BlockPos buildPos = new BlockPos(churchPos.getX() + x, baseY + y - 1, churchPos.getZ() + z);
                world.setBlockState(buildPos, Blocks.STONE_BRICKS.getDefaultState());
            }
        }
    }

    // Spire
    generateChurchSpire(world, churchPos.add(0, height, 0));
}

private void generateChurchSpire(World world, BlockPos base) {
    int spireHeight = 8;
    for (int y = 0; y < spireHeight; y++) {
        int size = spireHeight - y;
        for (int x = -size/2; x <= size/2; x++) {
            for (int z = -size/2; z <= size/2; z++) {
                if (Math.abs(x) + Math.abs(z) <= size) {
                    world.setBlockState(base.add(x, y, z), Blocks.STONE_BRICKS.getDefaultState());
                }
            }
        }
    }
}

private void generateTraditionalPub(World world) {
    BlockPos pubPos = center.add(radius/3, 0, -radius/3);
    int size = 6;
    int height = 4;

    for (int x = -size/2; x <= size/2; x++) {
        for (int z = -size/2; z <= size/2; z++) {
            int baseY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pubPos.getX() + x, pubPos.getZ() + z);
            for (int y = 0; y < height; y++) {
                BlockPos pos = new BlockPos(pubPos.getX() + x, baseY + y - 1, pubPos.getZ() + z);
                if (y == 0) {
                    world.setBlockState(pos, Blocks.STONE_BRICKS.getDefaultState());
                } else {
                    world.setBlockState(pos, Blocks.BRICKS.getDefaultState());
                }
            }
        }
    }
}

private void generateVictorianPark(World world) {
    BlockPos parkPos = center.add(0, 0, -radius/2);
    int parkSize = radius/4;

    // Generate park area
    for (int x = -parkSize; x <= parkSize; x++) {
        for (int z = -parkSize; z <= parkSize; z++) {
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, parkPos.getX() + x, parkPos.getZ() + z);
            BlockPos pos = new BlockPos(parkPos.getX() + x, y - 1, parkPos.getZ() + z);
            world.setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState());

            // Add iron fence border
            if (Math.abs(x) == parkSize || Math.abs(z) == parkSize) {
                world.setBlockState(pos.up(), Blocks.IRON_BARS.getDefaultState());
            }
        }
    }

    // Add trees and decorations
    net.minecraft.util.math.random.Random random = world.getRandom();
    for (int i = 0; i < parkSize * 2; i++) {
        int x = parkPos.getX() + random.nextInt(parkSize * 2) - parkSize;
        int z = parkPos.getZ() + random.nextInt(parkSize * 2) - parkSize;
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        world.setBlockState(new BlockPos(x, y, z), Blocks.OAK_SAPLING.getDefaultState());
    }
}

private void generateCobblestoneStreets(World world) {
    int streetWidth = 3;
    
    // Generate grid of streets
    for (int x = -radius; x <= radius; x++) {
        for (int z = -radius + streetWidth; z <= radius - streetWidth; z++) {
            if (x % (radius/2) == 0) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, center.getX() + x, center.getZ() + z);
                BlockPos pos = new BlockPos(center.getX() + x, y - 1, center.getZ() + z);
                world.setBlockState(pos, Blocks.COBBLESTONE.getDefaultState());
            }
        }
    }

    for (int z = -radius; z <= radius; z++) {
        for (int x = -radius + streetWidth; x <= radius - streetWidth; x++) {
            if (z % (radius/2) == 0) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, center.getX() + x, center.getZ() + z);
                BlockPos pos = new BlockPos(center.getX() + x, y - 1, center.getZ() + z);
                world.setBlockState(pos, Blocks.COBBLESTONE.getDefaultState());
            }
        }
    }
}

private void addVictorianDecorations(World world) {
    net.minecraft.util.math.random.Random random = net.minecraft.util.math.random.Random.create();
    
    // Add gas lamps along streets
    for (int i = 0; i < radius; i++) {
        int x = center.getX() + random.nextInt(radius * 2) - radius;
        int z = center.getZ() + random.nextInt(radius * 2) - radius;
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        
        if (isNearStreet(pos)) {
            world.setBlockState(pos, Blocks.TORCH.getDefaultState());
        }
    }
}

private boolean isNearStreet(BlockPos pos) {
    return (pos.getX() - center.getX()) % (radius/2) <= 2 ||
           (pos.getZ() - center.getZ()) % (radius/2) <= 2;
}

private void generateNYCVillage(World world) {
    LOGGER.info("Generating NYC village at {}", center);

    // Generate grid street system
    generateNYCStreets(world);

    // Generate skyscrapers
    generateSkyscrapers(world);

    // Generate Central Park area
    generateCentralPark(world);

    // Generate subway entrances
    generateSubwayEntrances(world);

    // Generate brownstone district
    generateBrownstoneDistrict(world);

    // Generate commercial district
    generateCommercialDistrict(world);

    // Add urban decorations
    addNYCDecorations(world);
}

private void generateNYCStreets(World world) {
    int streetWidth = 5;
    int blockSize = 20;

    // Generate grid pattern
    for (int x = -radius; x <= radius; x++) {
        for (int z = -radius; z <= radius; z++) {
            if (x % blockSize < streetWidth || z % blockSize < streetWidth) {
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, center.getX() + x, center.getZ() + z);
                BlockPos pos = new BlockPos(center.getX() + x, y - 1, center.getZ() + z);
                world.setBlockState(pos, Blocks.STONE_BRICKS.getDefaultState());
            }
        }
    }
}

private void generateSkyscrapers(World world) {
    net.minecraft.util.math.random.Random random = world.getRandom();
    int blockSize = 20;

    // Generate multiple skyscrapers
    for (int i = 0; i < 5; i++) {
        int x = center.getX() + (random.nextInt(5) - 2) * blockSize;
        int z = center.getZ() + (random.nextInt(5) - 2) * blockSize;
        int height = 30 + random.nextInt(20); // Varying heights
        generateSkyscraper(world, new BlockPos(x, 0, z), height);
    }
}

private void generateSkyscraper(World world, BlockPos base, int height) {
    int size = 8;
    
    for (int y = 0; y < height; y++) {
        for (int x = -size/2; x <= size/2; x++) {
            for (int z = -size/2; z <= size/2; z++) {
                int baseY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, base.getX() + x, base.getZ() + z);
                BlockPos pos = new BlockPos(base.getX() + x, baseY + y, base.getZ() + z);
                
                if (x == -size/2 || x == size/2 || z == -size/2 || z == size/2) {
                    world.setBlockState(pos, y % 4 == 0 ? Blocks.SMOOTH_STONE.getDefaultState() : Blocks.GLASS.getDefaultState());
                } else if (y % 4 == 0) {
                    // Floor levels
                    world.setBlockState(pos, Blocks.SMOOTH_STONE.getDefaultState());
                } else {
                    // Interior space
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                }
            }
        }
    }
}

private void generateCentralPark(World world) {
    BlockPos parkPos = center.add(radius/3, 0, 0);
    int parkSize = radius/3;

    // Generate park area
    for (int x = -parkSize; x <= parkSize; x++) {
        for (int z = -parkSize; z <= parkSize; z++) {
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, parkPos.getX() + x, parkPos.getZ() + z);
            BlockPos pos = new BlockPos(parkPos.getX() + x, y - 1, parkPos.getZ() + z);
            world.setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState());
        }
    }

    // Add trees and paths
    net.minecraft.util.math.random.Random random = net.minecraft.util.math.random.Random.create();
    for (int i = 0; i < parkSize * 3; i++) {
        int x = parkPos.getX() + random.nextInt(parkSize * 2) - parkSize;
        int z = parkPos.getZ() + random.nextInt(parkSize * 2) - parkSize;
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        world.setBlockState(new BlockPos(x, y, z), Blocks.OAK_SAPLING.getDefaultState());
    }
}

private void generateSubwayEntrances(World world) {
    net.minecraft.util.math.random.Random random = world.getRandom();
    int entrances = 4;

    for (int i = 0; i < entrances; i++) {
        int x = center.getX() + random.nextInt(radius * 2) - radius;
        int z = center.getZ() + random.nextInt(radius * 2) - radius;
        generateSubwayEntrance(world, new BlockPos(x, 0, z));
    }
}

private void generateSubwayEntrance(World world, BlockPos pos) {
    int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
    
    // Generate entrance structure
    for (int dx = -1; dx <= 1; dx++) {
        for (int dz = -1; dz <= 1; dz++) {
            BlockPos entrancePos = new BlockPos(pos.getX() + dx, y - 1, pos.getZ() + dz);
            world.setBlockState(entrancePos, Blocks.STONE_BRICKS.getDefaultState());
            
            // Add stairs going down
            for (int dy = 1; dy <= 3; dy++) {
                world.setBlockState(entrancePos.down(dy), Blocks.STONE_BRICK_STAIRS.getDefaultState());
            }
        }
    }
}

private void generateBrownstoneDistrict(World world) {
    int districtSize = radius/4;
    BlockPos districtPos = center.add(-radius/2, 0, -radius/2);

    for (int x = 0; x < districtSize; x += 6) {
        for (int z = 0; z < districtSize; z += 8) {
            generateBrownstone(world, districtPos.add(x, 0, z));
        }
    }
}

private void generateBrownstone(World world, BlockPos pos) {
    int width = 5;
    int depth = 7;
    int height = 4;

    for (int x = 0; x < width; x++) {
        for (int z = 0; z < depth; z++) {
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + x, pos.getZ() + z);
            for (int dy = 0; dy < height; dy++) {
                BlockPos buildPos = new BlockPos(pos.getX() + x, y + dy - 1, pos.getZ() + z);
                world.setBlockState(buildPos, Blocks.GRANITE.getDefaultState());
            }
        }
    }

    // Add stairs to entrance
    for (int i = 0; i < 3; i++) {
        BlockPos stairPos = new BlockPos(pos.getX() + width/2,
            world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + width/2, pos.getZ() - i - 1),
            pos.getZ() - i - 1);
        world.setBlockState(stairPos, Blocks.STONE_STAIRS.getDefaultState());
    }
}

private void generateCommercialDistrict(World world) {
    BlockPos districtPos = center.add(radius/3, 0, -radius/3);
    int districtSize = radius/4;

    // Generate commercial buildings
    for (int x = 0; x < districtSize; x += 10) {
        for (int z = 0; z < districtSize; z += 10) {
            generateCommercialBuilding(world, districtPos.add(x, 0, z));
        }
    }
}

private void generateCommercialBuilding(World world, BlockPos pos) {
    int width = 8;
    int depth = 8;
    int height = 12;

    for (int x = 0; x < width; x++) {
        for (int z = 0; z < depth; z++) {
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + x, pos.getZ() + z);
            for (int dy = 0; dy < height; dy++) {
                BlockPos buildPos = new BlockPos(pos.getX() + x, y + dy - 1, pos.getZ() + z);
                if (x == 0 || x == width-1 || z == 0 || z == depth-1) {
                    world.setBlockState(buildPos, Blocks.GLASS.getDefaultState());
                } else {
                    world.setBlockState(buildPos, Blocks.SMOOTH_STONE.getDefaultState());
                }
            }
        }
    }
}

private void addNYCDecorations(World world) {
    net.minecraft.util.math.random.Random random = world.getRandom();
    
    // Add modern urban decorations
    for (int i = 0; i < radius * 2; i++) {
        int x = center.getX() + random.nextInt(radius * 2) - radius;
        int z = center.getZ() + random.nextInt(radius * 2) - radius;
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);

        if (isNearNYCStreet(pos)) {
            // Add street lamps and decorations
            if (random.nextBoolean()) {
                world.setBlockState(pos, Blocks.SEA_LANTERN.getDefaultState());
            } else {
                world.setBlockState(pos, Blocks.IRON_BARS.getDefaultState());
            }
        }
    }
}

private boolean isNearNYCStreet(BlockPos pos) {
    int blockSize = 20;
    int x = pos.getX() - center.getX();
    int z = pos.getZ() - center.getZ();
    return x % blockSize < 5 || z % blockSize < 5;
}

private void generateVictorianStructure(World world, BlockPos pos, String structureName) {
    switch(structureName.toLowerCase()) {
        case "mansion" -> generateVictorianMansion(world, pos);
        case "factory" -> generateVictorianFactory(world, pos);
        case "church" -> generateVictorianChurch(world, pos);
        case "shop" -> generateVictorianShop(world, pos);
        default -> generateTerracedHouse(world, pos, 5, 8);
    }
}

private void generateVictorianMansion(World world, BlockPos pos) {
    int width = 15;
    int length = 15;
    int height = 12;

    // Generate main structure
    for (int x = -width/2; x <= width/2; x++) {
        for (int z = -length/2; z <= length/2; z++) {
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + x, pos.getZ() + z);

            // Foundation
            world.setBlockState(new BlockPos(pos.getX() + x, y - 1, pos.getZ() + z),
                Blocks.STONE_BRICKS.getDefaultState());

            // Walls
            if (x == -width/2 || x == width/2 || z == -length/2 || z == length/2) {
                for (int dy = 0; dy < height; dy++) {
                    Block block = switch(dy % 4) {
                        case 0 -> Blocks.STONE_BRICKS;
                        case 1 -> Blocks.BRICKS;
                        case 2 -> Blocks.STONE_BRICKS;
                        default -> Blocks.BRICKS;
                    };
                    world.setBlockState(new BlockPos(pos.getX() + x, y + dy, pos.getZ() + z),
                        block.getDefaultState());
                }
            }
        }
    }

    // Generate tower
    generateVictorianTower(world, new BlockPos(pos.getX() + width/2, pos.getY(), pos.getZ() + length/2));
}

private void generateVictorianTower(World world, BlockPos pos) {
    int height = 16;
    int width = 3;

    for (int y = 0; y < height; y++) {
        for (int x = -width/2; x <= width/2; x++) {
            for (int z = -width/2; z <= width/2; z++) {
                if (y == height - 1) {
                    // Spire top
                    if (x == 0 && z == 0) {
                        world.setBlockState(new BlockPos(pos.getX() + x, 
                            pos.getY() + y, pos.getZ() + z),
                            Blocks.IRON_BARS.getDefaultState());
                    }
                } else {
                    world.setBlockState(new BlockPos(pos.getX() + x, 
                        pos.getY() + y, pos.getZ() + z),
                        Blocks.STONE_BRICKS.getDefaultState());
                }
            }
        }
    }
}

private void generateVictorianFactory(World world, BlockPos pos) {
    int width = 13;
    int length = 17;
    int height = 10;

    // Main building
    for (int x = -width/2; x <= width/2; x++) {
        for (int z = -length/2; z <= length/2; z++) {
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + x, pos.getZ() + z);

            // Foundation
            world.setBlockState(new BlockPos(pos.getX() + x, y - 1, pos.getZ() + z),
                Blocks.STONE_BRICKS.getDefaultState());

            // Walls
            if (x == -width/2 || x == width/2 || z == -length/2 || z == length/2) {
                for (int dy = 0; dy < height; dy++) {
                    Block block = (dy % 3 == 0) ? Blocks.STONE_BRICKS : Blocks.BRICKS;
                    world.setBlockState(new BlockPos(pos.getX() + x, y + dy, pos.getZ() + z),
                        block.getDefaultState());

                    // Windows
                    if (dy > 1 && dy < height-1 && (x + z) % 3 == 0) {
                        world.setBlockState(new BlockPos(pos.getX() + x, y + dy, pos.getZ() + z),
                            Blocks.GLASS_PANE.getDefaultState());
                    }
                }
            }
        }
    }

    // Generate chimneys
    generateFactoryChimney(world, new BlockPos(pos.getX() - width/4, pos.getY() + height, pos.getZ() - length/4));
    generateFactoryChimney(world, new BlockPos(pos.getX() + width/4, pos.getY() + height, pos.getZ() + length/4));
}

private void generateVictorianShop(World world, BlockPos pos) {
    int width = 7;
    int length = 9;
    int height = 6;

    // Main building
    for (int x = -width/2; x <= width/2; x++) {
        for (int z = -length/2; z <= length/2; z++) {
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX() + x, pos.getZ() + z);

            // Foundation
            world.setBlockState(new BlockPos(pos.getX() + x, y - 1, pos.getZ() + z),
                Blocks.STONE_BRICKS.getDefaultState());

            // Walls and shop front
            if (x == -width/2 || x == width/2 || z == -length/2 || z == length/2) {
                for (int dy = 0; dy < height; dy++) {
                    BlockPos buildPos = new BlockPos(pos.getX() + x, y + dy, pos.getZ() + z);
                    
                    // Shop front with large windows
                    if (z == -length/2 && dy < 3 && x != -width/2 && x != width/2) {
                        world.setBlockState(buildPos, Blocks.GLASS_PANE.getDefaultState());
                    } else {
                        world.setBlockState(buildPos, Blocks.BRICKS.getDefaultState());
                    }
                }
            }
        }
    }

    // Generate awning
    for (int x = -width/2; x <= width/2; x++) {
        world.setBlockState(new BlockPos(pos.getX() + x, 
            pos.getY() + 3, pos.getZ() - length/2 - 1),
            Blocks.OAK_STAIRS.getDefaultState());
    }
}

private void generateNYCStructure(World world, BlockPos pos, String structureName) {
    switch(structureName.toLowerCase()) {
        case "skyscraper" -> generateModernSkyscraperBuilding(world, pos);
        case "apartment" -> generateBrownstone(world, pos);
        case "store" -> generateRetailStore(world, pos);
        case "subway" -> generateSubwayStationBuilding(world, pos);
        default -> generateCommercialBuilding(world, pos);
    }
}

private void generateModernSkyscraperBuilding(World world, BlockPos pos) {
    // Renamed to avoid conflict
    int baseWidth = 11;
    int height = 40 + world.getRandom().nextInt(20);

    // Core structure
    for (int y = 0; y < height; y++) {
        int width = Math.max(7, baseWidth - (y / 10));
        for (int x = -width/2; x <= width/2; x++) {
            for (int z = -width/2; z <= width/2; z++) {
                int baseY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, 
                    pos.getX() + x, pos.getZ() + z);
                    
                BlockPos buildPos = new BlockPos(pos.getX() + x, baseY + y, pos.getZ() + z);
                
                if (x == -width/2 || x == width/2 || z == -width/2 || z == width/2) {
                    world.setBlockState(buildPos, (y % 4 < 2) ? 
                        Blocks.GLASS.getDefaultState() : 
                        Blocks.SMOOTH_STONE.getDefaultState());
                }
                else if (y % 4 == 0) {
                    world.setBlockState(buildPos, Blocks.SMOOTH_STONE.getDefaultState());
                }
            }
        }
    }

    // Antenna/spire
    for (int y = 0; y < 5; y++) {
        world.setBlockState(new BlockPos(pos.getX(), pos.getY() + height + y, pos.getZ()),
            Blocks.IRON_BARS.getDefaultState());
    }
}

private void generateRetailStore(World world, BlockPos pos) {
    int width = 9;
    int length = 11;
    int height = 4;

    // Main structure
    for (int x = -width/2; x <= width/2; x++) {
        for (int z = -length/2; z <= length/2; z++) {
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, 
                pos.getX() + x, pos.getZ() + z);

            // Foundation
            world.setBlockState(new BlockPos(pos.getX() + x, y - 1, pos.getZ() + z),
                Blocks.SMOOTH_STONE.getDefaultState());

            // Walls
            if (x == -width/2 || x == width/2 || z == -length/2 || z == length/2) {
                for (int dy = 0; dy < height; dy++) {
                    BlockPos buildPos = new BlockPos(pos.getX() + x, y + dy, pos.getZ() + z);
                    
                    // Storefront windows
                    if (z == -length/2 && dy < 3 && x > -width/2 && x < width/2) {
                        world.setBlockState(buildPos, Blocks.GLASS_PANE.getDefaultState());
                    } else {
                        world.setBlockState(buildPos, Blocks.SMOOTH_STONE.getDefaultState());
                    }
                }
            }
        }
    }

    // Modern signage
    for (int x = -width/2 + 1; x <= width/2 - 1; x++) {
        world.setBlockState(new BlockPos(pos.getX() + x, 
            pos.getY() + height, pos.getZ() - length/2),
            Blocks.GLOWSTONE.getDefaultState());
    }
}

private void generateSubwayStationBuilding(World world, BlockPos pos) {
    // Renamed to avoid conflict
    int width = 7;
    int length = 11;
    int depth = 5;

    // Underground platform
    for (int x = -width/2; x <= width/2; x++) {
        for (int z = -length/2; z <= length/2; z++) {
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, 
                pos.getX() + x, pos.getZ() + z);
                
            for (int dy = -depth; dy < 0; dy++) {
                BlockPos buildPos = new BlockPos(pos.getX() + x, y + dy, pos.getZ() + z);
                
                if (dy == -depth) {
                    world.setBlockState(buildPos, Blocks.SMOOTH_STONE.getDefaultState());
                }
                else if (x == -width/2 || x == width/2 || z == -length/2 || z == length/2) {
                    world.setBlockState(buildPos, Blocks.STONE_BRICKS.getDefaultState());
                }
                else {
                    world.setBlockState(buildPos, Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    // Entrance structures at both ends
    generateSubwayEntranceStructure(world, pos);
    generateSubwayEntranceStructure(world, new BlockPos(pos.getX(), pos.getY(), pos.getZ() + length/2));
}

private void generateSubwayEntranceStructure(World world, BlockPos pos) {
    // Renamed to avoid conflict
    int entranceHeight = 4;
    
    for (int y = 0; y < entranceHeight; y++) {
        for (int x = -1; x <= 1; x++) {
            BlockPos stairPos = new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ());
            
            if (x == -1 || x == 1) {
                world.setBlockState(stairPos, Blocks.SMOOTH_STONE.getDefaultState());
            }
            else {
                world.setBlockState(stairPos, Blocks.STONE_STAIRS.getDefaultState());
            }
        }
    }

    // Entrance roof
    for (int x = -1; x <= 1; x++) {
        world.setBlockState(new BlockPos(pos.getX() + x, 
            pos.getY() + entranceHeight, pos.getZ() ),
            Blocks.SMOOTH_STONE_SLAB.getDefaultState());
    }
}

private void generateCulturalPathways(World world, BlockPos from, BlockPos to) {
    // Get path style based on culture
    PathStyle style = getCulturalPathStyle();
    Queue<PathNode> pathQueue = new PriorityQueue<>(Comparator.comparingDouble(n -> n.cost));
    Set<BlockPos> visited = new HashSet<>();
    Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
    Map<BlockPos, Double> costSoFar = new HashMap<>();
    
    pathQueue.add(new PathNode(from, 0));
    costSoFar.put(from, 0.0);
    
    while (!pathQueue.isEmpty()) {
        PathNode current = pathQueue.poll();
        if (current.pos.equals(to)) {
            buildPath(world, reconstructPath(current.pos, cameFrom), style);
            return;
        }
        
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos next = current.pos.offset(dir);
            double newCost = costSoFar.get(current.pos) + getPathCost(world, next, style);
            
            if (!visited.contains(next) && isValidPathLocation(world, next)) {
                visited.add(next);
                cameFrom.put(next, current.pos);
                costSoFar.put(next, newCost);
                pathQueue.add(new PathNode(next, newCost + estimateDistance(next, to)));
            }
        }
    }
}

private record PathNode(BlockPos pos, double cost) implements Comparable<PathNode> {
    @Override
    public int compareTo(PathNode other) {
        return Double.compare(this.cost, other.cost);
    }
}

private static final class PathStyle {
    final Block[] blocks;
    final boolean elevated;
    final boolean addLights;
    final int lightSpacing;

    PathStyle(Block[] blocks, boolean elevated, boolean addLights, int lightSpacing) {
        this.blocks = blocks;
        this.elevated = elevated;
        this.addLights = addLights;
        this.lightSpacing = lightSpacing;
    }
}

private PathStyle getCulturalPathStyle() {
    return switch(culture.toLowerCase()) {
        case "roman" -> new PathStyle(
            new Block[]{Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE}, 
            true, true, 6);
        case "egyptian" -> new PathStyle(
            new Block[]{Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE},
            false, true, 8);
        case "victorian" -> new PathStyle(
            new Block[]{Blocks.COBBLESTONE, Blocks.STONE_BRICKS},
            true, true, 5);
        case "nyc" -> new PathStyle(
            new Block[]{Blocks.STONE_BRICKS, Blocks.SMOOTH_STONE},
            true, true, 4);
        default -> new PathStyle(
            new Block[]{Blocks.DIRT_PATH},
            false, false, 0);
    };
}

private double getPathCost(World world, BlockPos pos, PathStyle style) {
    int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
    
    // Higher cost for elevation changes
    double heightCost = Math.abs(y - pos.getY()) * 2.0;
    
    // Higher cost for water crossing
    if (world.getBlockState(pos).getBlock() == Blocks.WATER) {
        return 10.0 + heightCost;
    }
    
    // Lower cost for existing paths
    Block block = world.getBlockState(pos.down()).getBlock();
    if (Arrays.asList(style.blocks).contains(block)) {
        return 0.5 + heightCost;
    }
    
    return 1.0 + heightCost;
}

private boolean isValidPathLocation(World world, BlockPos pos) {
    // Check if we can build a path here
    int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
    BlockPos checkPos = new BlockPos(pos.getX(), y - 1, pos.getZ());
    
    // Don't build through buildings
    if (culturalStructures.values().stream()
        .anyMatch(structurePos -> structurePos.isWithinDistance(checkPos, 3))) {
        return false;
    }
    
    return world.getBlockState(checkPos).isSolidBlock(world, checkPos);
}

private List<BlockPos> reconstructPath(BlockPos current, Map<BlockPos, BlockPos> cameFrom) {
    List<BlockPos> path = new ArrayList<>();
    BlockPos pos = current;
    
    while (cameFrom.containsKey(pos)) {
        path.add(pos);
        pos = cameFrom.get(pos);
    }
    path.add(pos);
    Collections.reverse(path);
    
    return path;
}

private void buildPath(World world, List<BlockPos> path, PathStyle style) {
    int lightCounter = 0;
    
    // Pre-calculate heights to ensure smooth transitions
    Map<BlockPos, Integer> pathHeights = new HashMap<>();
    for (int i = 0; i < path.size(); i++) {
        BlockPos pos = path.get(i);
        int baseY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        
        // Smooth height transitions with neighboring blocks
        if (i > 0 && i < path.size() - 1) {
            int prevY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, 
                path.get(i-1).getX(), path.get(i-1).getZ());
            int nextY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, 
                path.get(i+1).getX(), path.get(i+1).getZ());
            baseY = Math.round((prevY + baseY + nextY) / 3.0f);
        }
        pathHeights.put(pos, baseY);
    }
    
    // Build the path with calculated heights
    for (BlockPos pos : path) {
        int y = pathHeights.get(pos);
        BlockPos pathPos = new BlockPos(pos.getX(), y - 1, pos.getZ());
        
        // Place path block
        Block pathBlock = style.blocks[world.getRandom().nextInt(style.blocks.length)];
        world.setBlockState(pathPos, pathBlock.getDefaultState());
        
        // Handle elevation changes with stairs or slabs
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos neighbor = pos.offset(dir);
            if (path.contains(neighbor)) {
                int heightDiff = pathHeights.get(neighbor) - y;
                if (Math.abs(heightDiff) == 1) {
                    world.setBlockState(
                        heightDiff > 0 ? pathPos.up() : pathPos,
                        Blocks.STONE_BRICK_STAIRS.getDefaultState()
                    );
                }
            }
        }
        
        // Add curbs for elevated paths
        if (style.elevated) {
            for (Direction dir : Direction.Type.HORIZONTAL) {
                BlockPos edgePos = pathPos.offset(dir);
                if (!path.contains(edgePos)) {
                    world.setBlockState(edgePos, Blocks.STONE_BRICK_WALL.getDefaultState());
                    
                    // Fill underneath curbs for stability
                    for (int dy = 1; dy < 3; dy++) {
                        BlockPos support = edgePos.down(dy);
                        if (world.getBlockState(support).isAir()) {
                            world.setBlockState(support, pathBlock.getDefaultState());
                        }
                    }
                }
            }
        }
        
        // Add lights
        if (style.addLights && ++lightCounter >= style.lightSpacing) {
            lightCounter = 0;
            BlockPos lightPos = pathPos.up();
            switch(culture.toLowerCase()) {
                case "roman" -> world.setBlockState(lightPos, Blocks.TORCH.getDefaultState());
                case "egyptian" -> world.setBlockState(lightPos, Blocks.LANTERN.getDefaultState());
                case "victorian" -> world.setBlockState(lightPos, Blocks.SOUL_LANTERN.getDefaultState());
                case "nyc" -> world.setBlockState(lightPos, Blocks.SEA_LANTERN.getDefaultState());
                default -> world.setBlockState(lightPos, Blocks.TORCH.getDefaultState());
            }
        }
    }
}

private double estimateDistance(BlockPos start, BlockPos end) {
    return Math.sqrt(start.getSquaredDistance(end));
}
}
