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

    private static class BuildingTemplate {
        final int width;
        final int length;
        final int height;
        final List<Block> materials;
        final List<String> features;

        BuildingTemplate(int width, int length, int height, List<Block> materials, List<String> features) {
            this.width = width;
            this.length = length;
            this.height = height;
            this.materials = materials;
            this.features = features;
        }
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
        // Use default dimensions for terrain check
        final int width = 7;  // Default width for most structures
        final int length = 9; // Default length for most structures
        final int maxHeightDiff = 2;
        final int baseY = pos.getY();

        // Check area is relatively flat and has solid ground
        for (int x = -width/2; x <= width/2; x++) {
            for (int z = -length/2; z <= length/2; z++) {
                BlockPos checkPos = pos.add(x, 0, z);
                int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, checkPos.getX(), checkPos.getZ());
                
                // Check height difference
                if (Math.abs(y - baseY) > maxHeightDiff) {
                    return false;
                }
                
                // Check ground is solid
                if (!world.getBlockState(checkPos.down()).isSideSolidFullSquare(world, checkPos.down(), Direction.UP)) {
                    return false;
                }
                
                // Check for water
                if (world.getBlockState(checkPos).getBlock() == Blocks.WATER) {
                    return false;
                }
            }
        }
        
        return true;
    }

    private void generateCulturalStructure(World world, BlockPos pos, String structureName) {
        if (!(world instanceof ServerWorld)) {
            LOGGER.error("Cannot generate structure in non-server world");
            return;
        }

        ServerWorld serverWorld = (ServerWorld) world;
        String namespace = "villagesreborn";
        String path = String.format("structures/%s/%s",
            culture.toLowerCase(), structureName.toLowerCase().replace(" ", "_"));
            
        try {
            // Get structure template from server
            Identifier templateId = Identifier.of(namespace, path);
            Optional<StructureTemplate> template = serverWorld.getStructureTemplateManager().getTemplate(templateId);
            
            if (template.isEmpty()) {
                LOGGER.error("Structure template not found: {}", templateId);
                return;
            }

            // Create placement data
            StructurePlacementData placementData = new StructurePlacementData()
                .setIgnoreEntities(false)
                .setRotation(BlockRotation.random(serverWorld.getRandom()))
                .setMirror(BlockMirror.NONE);

            // Find suitable ground position
            BlockPos placementPos = findGroundPosition(world, pos, template.get().getSize());
            
            // Place the structure
            template.get().place(
                serverWorld,
                placementPos,
                placementPos,
                placementData,
                serverWorld.getRandom(),
                Block.NOTIFY_ALL | Block.FORCE_STATE);

            LOGGER.info("Successfully generated {} at {}", templateId, placementPos);
            Box boundingBox = new Box(
                placementPos.getX(), placementPos.getY(), placementPos.getZ(),
                placementPos.getX() + template.get().getSize().getX(),
                placementPos.getY() + template.get().getSize().getY(),
                placementPos.getZ() + template.get().getSize().getZ()
            );
            addStructurePointsOfInterest(world, placementPos, boundingBox);
        } catch (Exception e) {
            LOGGER.error("Error generating structure {}/{}: {}", namespace, path, e.getMessage());
        }
    }

    private BlockPos findGroundPosition(World world, BlockPos pos, Vec3i size) {
        int x = pos.getX();
        int z = pos.getZ();
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        
        // Check if area is flat enough
        int maxHeightDiff = 3;
        int baseHeight = y;
        
        for (int dx = 0; dx < size.getX(); dx++) {
            for (int dz = 0; dz < size.getZ(); dz++) {
                int height = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                if (Math.abs(height - baseHeight) > maxHeightDiff) {
                    y = Math.max(y, height); // Use highest point to ensure structure isn't floating
                }
            }
        }
        
        return new BlockPos(x, y, z);
    }

    private void addStructurePointsOfInterest(World world, BlockPos pos, Box boundingBox) {
        Vec3d size = boundingBox.getCenter();
        int sizeX = (int)(boundingBox.maxX - boundingBox.minX);
        int sizeZ = (int)(boundingBox.maxZ - boundingBox.minZ);
        
        // Add POIs at structure corners
        addPointOfInterest(pos);
        addPointOfInterest(pos.add(sizeX, 0, 0));
        addPointOfInterest(pos.add(0, 0, sizeZ));
        addPointOfInterest(pos.add(sizeX, 0, sizeZ));
        
        // Add POI at the entrance (assuming front is -Z)
        addPointOfInterest(pos.add(sizeX / 2, 0, -1));
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

    public void generateVillage(World world) {
        LOGGER.info("Starting generation of {} village at {}", culture, center);

        // First get the cultural layout plan
        generateLayoutPlan()
            .thenCompose(layout -> {
                // Generate the village center first
                String centerBuilding = (String) layout.get("CENTER");
                return generateCulturalBuildingDescription(centerBuilding)
                    .thenCompose(centerDesc -> {
                        BlockPos centerPos = findBuildingLocation(world, centerBuilding);
                        generateBuildingFromDescription(world, centerPos, centerDesc);

                        // Then generate districts
                        @SuppressWarnings("unchecked")
                        List<String> districts = (List<String>) layout.get("DISTRICTS");
                        List<CompletableFuture<Void>> districtFutures = new ArrayList<>();

                        for (String district : districts) {
                            districtFutures.add(generateDistrict(world, district, layout));
                        }

                        return CompletableFuture.allOf(districtFutures.toArray(new CompletableFuture[0]));
                    });
            })
            .thenRun(() -> {
                // Finally generate paths and decorations
                generateCulturalPathways(world);
                addCulturalDecorations(world);
                LOGGER.info("Completed generation of {} village at {}", culture, center);
            })
            .exceptionally(e -> {
                LOGGER.error("Error generating village: {}", e.getMessage());
                return null;
            });
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
            "Design a village layout for a %s culture in Minecraft.\n" +
            "Consider:\n" +
            "1. How buildings should be arranged (grid/organic/hierarchical)\n" +
            "2. Important cultural spaces and their placement\n" +
            "3. How villagers would naturally move through this space\n" +
            "Format response as:\n" +
            "LAYOUT: (grid/organic/radial)\n" +
            "CENTER: (main cultural building)\n" +
            "DISTRICTS: (comma-separated list)\n" +
            "PATHWAYS: (road style description)",
            culture
        );

        return LLMService.getInstance()
            .generateResponse(prompt)
            .thenApply(this::parseLayoutResponse);
    }

    private Map<String, Object> parseLayoutResponse(String response) {
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
        layout.put("PATHWAYS", params.get("PATHWAYS"));

        return layout;
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
                    }
                }
            }
        }

        // Add entrance
        int doorZ = -houseLength;
        for (int dy = 0; dy < 2; dy++) {
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ() + doorZ);
            BlockPos doorPos = new BlockPos(pos.getX(), y + dy, pos.getZ() + doorZ);
            world.setBlockState(doorPos, Blocks.AIR.getDefaultState());
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

private void generateVictorianVillage(World world) {
    LOGGER.info("Generating Victorian village at {}", center);

    // Generate town square with clock tower
    generateTownSquare(world);

    // Generate terraced houses
    generateTerracedHouses(world);

    // Generate factory district
    generateFactoryDistrict(world);

    // Generate Victorian church
    generateVictorianChurch(world);

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
                BlockPos pos = base.add(x, y, z);
                if (y < height - 1) {
                    world.setBlockState(pos, Blocks.STONE_BRICKS.getDefaultState());
                } else {
                    world.setBlockState(pos, Blocks.GOLD_BLOCK.getDefaultState());
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

private void generateVictorianChurch(World world) {
    BlockPos churchPos = center.add(-radius/2, 0, -radius/3);
    int width = 8;
    int length = 12;
    int height = 8;

    // Main building
    for (int x = -width/2; x <= width/2; x++) {
        for (int z = -length/2; z <= length/2; z++) {
            for (int y = 0; y < height; y++) {
                int baseY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, churchPos.getX() + x, churchPos.getZ() + z);
                BlockPos pos = new BlockPos(churchPos.getX() + x, baseY + y - 1, churchPos.getZ() + z);
                world.setBlockState(pos, Blocks.STONE_BRICKS.getDefaultState());
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
                BlockPos pos = new BlockPos(base.getX() + x, baseY + y - 1, base.getZ() + z);
                
                if (x == -size/2 || x == size/2 || z == -size/2 || z == size/2) {
                    world.setBlockState(pos, Blocks.GLASS.getDefaultState());
                } else {
                    world.setBlockState(pos, Blocks.SMOOTH_STONE.getDefaultState());
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
}
