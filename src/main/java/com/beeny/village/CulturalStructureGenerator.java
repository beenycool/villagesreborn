package com.beeny.village;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.BlockMirror;
import com.beeny.ai.LLMService;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtCompound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;

public class CulturalStructureGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private final String culture;
    private final Random random;

    public CulturalStructureGenerator(String culture) {
        this.culture = culture.toLowerCase();
        this.random = new Random();
    }

    public void generateStructure(ServerWorld world, BlockPos pos, String structureType, double prosperityFactor) {
        String basePath = String.format("structures/%s/%s", culture, structureType.toLowerCase().replace(" ", "_"));
        Identifier templateId = Identifier.of("villagesreborn", basePath);

        // Try to load base template
        Optional<StructureTemplate> baseTemplate = world.getStructureTemplateManager().getTemplate(templateId);

        if (baseTemplate.isPresent()) {
            // Modify template based on prosperity and culture
            modifyAndPlaceTemplate(world, pos, baseTemplate.get(), prosperityFactor);
        } else {
            // Generate procedural structure if template not found
            generateProceduralStructure(world, pos, structureType, prosperityFactor);
        }
    }

    private void modifyAndPlaceTemplate(ServerWorld world, BlockPos pos, StructureTemplate template, double prosperityFactor) {
        StructurePlacementData placement = createPlacementData(prosperityFactor);
        Vec3i size = template.getSize();
        BlockPos templateSize = new BlockPos(size.getX(), size.getY(), size.getZ());
        BlockPos finalPos = adjustForTerrain(world, pos, templateSize);

        // Apply cultural variations before placing
        Map<Block, Block> blockReplacements = getCulturalBlockReplacements();
        StructureTemplate modifiedTemplate = applyBlockReplacements(template, blockReplacements);

        // Add procedural details based on prosperity
        addProceduralDetails(world, finalPos, templateSize, prosperityFactor);

        // Place the modified structure
        modifiedTemplate.place(world, finalPos, finalPos, placement, world.getRandom(), Block.NOTIFY_ALL);
    }

    private StructurePlacementData createPlacementData(double prosperityFactor) {
        return new StructurePlacementData()
            .setIgnoreEntities(false)
            .setRotation(getRandomRotation())
            .setMirror(random.nextBoolean() ? BlockMirror.NONE : BlockMirror.FRONT_BACK);
    }

    private BlockRotation getRandomRotation() {
        return BlockRotation.values()[random.nextInt(BlockRotation.values().length)];
    }

    private BlockPos adjustForTerrain(ServerWorld world, BlockPos pos, BlockPos size) {
        int highestY = Integer.MIN_VALUE;
        
        // Find highest point in footprint
        for (int x = 0; x < size.getX(); x++) {
            for (int z = 0; z < size.getZ(); z++) {
                int y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, 
                    pos.getX() + x, pos.getZ() + z);
                highestY = Math.max(highestY, y);
            }
        }

        return new BlockPos(pos.getX(), highestY, pos.getZ());
    }

    private Map<Block, Block> getCulturalBlockReplacements() {
        Map<Block, Block> replacements = new HashMap<>();
        
        switch (culture) {
            case "egyptian" -> {
                replacements.put(Blocks.STONE_BRICKS, Blocks.SANDSTONE);
                replacements.put(Blocks.STONE, Blocks.SMOOTH_SANDSTONE);
                replacements.put(Blocks.COBBLESTONE, Blocks.CUT_SANDSTONE);
            }
            case "roman" -> {
                replacements.put(Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE);
                replacements.put(Blocks.STONE, Blocks.STONE_BRICKS);
                replacements.put(Blocks.OAK_PLANKS, Blocks.QUARTZ_BLOCK);
            }
            case "victorian" -> {
                replacements.put(Blocks.STONE_BRICKS, Blocks.BRICKS);
                replacements.put(Blocks.STONE, Blocks.STONE_BRICKS);
                replacements.put(Blocks.OAK_PLANKS, Blocks.DARK_OAK_PLANKS);
            }
            case "nyc" -> {
                replacements.put(Blocks.STONE_BRICKS, Blocks.SMOOTH_STONE);
                replacements.put(Blocks.STONE, Blocks.POLISHED_ANDESITE);
                replacements.put(Blocks.OAK_PLANKS, Blocks.SMOOTH_STONE);
            }
        }
        
        return replacements;
    }

    private StructureTemplate applyBlockReplacements(StructureTemplate template, Map<Block, Block> replacements) {
        // Create a new template based on the original one
        StructureTemplate modified = new StructureTemplate();
        
        // Get the palette and blocks from the original template
        List<StructureTemplate.PalettedBlockInfoList> palettedBlockInfoLists = template.getBlockInfoLists();
        List<StructureTemplate.StructureEntityInfo> entityInfos = template.getEntityInfos();

        // Create a list to hold the modified palette
        List<StructureTemplate.PalettedBlockInfoList> modifiedPalettedBlockInfoLists = new ArrayList<>();

        // For each palette section in the original template
        for (StructureTemplate.PalettedBlockInfoList blockInfoList : palettedBlockInfoLists) {
            List<StructureTemplate.StructureBlockInfo> modifiedInfos = new ArrayList<>();

            // Replace blocks in each section based on replacement map
            for (StructureTemplate.StructureBlockInfo blockInfo : blockInfoList.getAll()) {
                BlockState currentState = blockInfo.state();
                Block currentBlock = currentState.getBlock();

                // Check if this block has a replacement
                if (replacements.containsKey(currentBlock)) {
                    Block replacementBlock = replacements.get(currentBlock);
                    // Create a new state based on the replacement block, preserving properties when possible
                    BlockState newState = replacementBlock.getDefaultState();
                    
                    // Create new StructureBlockInfo with replacement
                    StructureTemplate.StructureBlockInfo newBlockInfo = new StructureTemplate.StructureBlockInfo(
                        blockInfo.pos(),
                        newState,
                        blockInfo.nbt() != null ? blockInfo.nbt().copy() : null
                    );
                    modifiedInfos.add(newBlockInfo);
                } else {
                    // Keep original if no replacement defined
                    modifiedInfos.add(blockInfo);
                }
            }

            // Create new palette section with modified blocks
            StructureTemplate.PalettedBlockInfoList modifiedBlockInfoList = 
                new StructureTemplate.PalettedBlockInfoList(modifiedInfos);
            modifiedPalettedBlockInfoLists.add(modifiedBlockInfoList);
        }

        // Set the palette on the new template
        NbtCompound templateData = new NbtCompound();
        template.writeNbt(templateData);
        
        // Update size to match the original
        Vec3i size = template.getSize();
        modified.setAuthor(culture + " generator");
        
        // Manually set size and block lists
        try {
            modified.setBlockInfoLists(modifiedPalettedBlockInfoLists);
            modified.setSize(size.getX(), size.getY(), size.getZ());
            modified.setEntityInfos(entityInfos);
        } catch (Exception e) {
            LOGGER.error("Error applying block replacements to template", e);
            return template; // Return original if failure
        }

        return modified;
    }

    private void addProceduralDetails(ServerWorld world, BlockPos pos, BlockPos size, double prosperityFactor) {
        // Add culture-specific decorative elements based on prosperity
        int decorationCount = (int)(prosperityFactor * 10);
        
        switch (culture) {
            case "egyptian" -> addEgyptianDecorations(world, pos, size, decorationCount);
            case "roman" -> addRomanDecorations(world, pos, size, decorationCount);
            case "victorian" -> addVictorianDecorations(world, pos, size, decorationCount);
            case "nyc" -> addNYCDecorations(world, pos, size, decorationCount);
        }
    }

    private void addEgyptianDecorations(ServerWorld world, BlockPos pos, BlockPos size, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos decorPos = getRandomPositionInStructure(pos, size);
            if (random.nextBoolean()) {
                world.setBlockState(decorPos, Blocks.CHISELED_SANDSTONE.getDefaultState());
            } else {
                world.setBlockState(decorPos, Blocks.CUT_SANDSTONE.getDefaultState());
            }
        }
    }

    private void addRomanDecorations(ServerWorld world, BlockPos pos, BlockPos size, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos decorPos = getRandomPositionInStructure(pos, size);
            if (random.nextBoolean()) {
                world.setBlockState(decorPos, Blocks.CHISELED_STONE_BRICKS.getDefaultState());
            } else {
                world.setBlockState(decorPos, Blocks.STONE_BRICK_WALL.getDefaultState());
            }
        }
    }

    private void addVictorianDecorations(ServerWorld world, BlockPos pos, BlockPos size, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos decorPos = getRandomPositionInStructure(pos, size);
            if (random.nextBoolean()) {
                world.setBlockState(decorPos, Blocks.IRON_BARS.getDefaultState());
            } else {
                world.setBlockState(decorPos, Blocks.LANTERN.getDefaultState());
            }
        }
    }

    private void addNYCDecorations(ServerWorld world, BlockPos pos, BlockPos size, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos decorPos = getRandomPositionInStructure(pos, size);
            if (random.nextBoolean()) {
                world.setBlockState(decorPos, Blocks.SEA_LANTERN.getDefaultState());
            } else {
                world.setBlockState(decorPos, Blocks.GLASS_PANE.getDefaultState());
            }
        }
    }

    private BlockPos getRandomPositionInStructure(BlockPos pos, BlockPos size) {
        return pos.add(
            random.nextInt(size.getX()),
            random.nextInt(size.getY()),
            random.nextInt(size.getZ())
        );
    }

    private void generateProceduralStructure(ServerWorld world, BlockPos pos, String structureType, double prosperityFactor) {
        String prompt = String.format(
            "Design a %s structure for a %s-style Minecraft village.\n" +
            "Current prosperity level: %.2f\n" +
            "Specify:\n" +
            "1. Base dimensions (width, length, height)\n" +
            "2. Primary building materials\n" +
            "3. Architectural features\n" +
            "4. Cultural elements\n" +
            "Format response as:\n" +
            "WIDTH: (number)\n" +
            "LENGTH: (number)\n" +
            "HEIGHT: (number)\n" +
            "MATERIALS: (comma-separated list)\n" +
            "FEATURES: (comma-separated list)",
            structureType, culture, prosperityFactor
        );

        LLMService.getInstance()
            .generateResponse(prompt)
            .thenAccept(response -> {
                StructureTemplate template = createProceduralTemplate(parseStructureDescription(response));
                modifyAndPlaceTemplate(world, pos, template, prosperityFactor);
            })
            .exceptionally(e -> {
                LOGGER.error("Error generating procedural structure", e);
                return null;
            });
    }

    private Map<String, String> parseStructureDescription(String response) {
        Map<String, String> params = new HashMap<>();
        for (String line : response.split("\n")) {
            String[] parts = line.split(": ", 2);
            if (parts.length == 2) {
                params.put(parts[0].trim(), parts[1].trim());
            }
        }
        return params;
    }

    private StructureTemplate createProceduralTemplate(Map<String, String> params) {
        StructureTemplate template = new StructureTemplate();
        
        try {
            // Parse dimensions from parameters
            int width = Integer.parseInt(params.getOrDefault("WIDTH", "5"));
            int length = Integer.parseInt(params.getOrDefault("LENGTH", "5"));
            int height = Integer.parseInt(params.getOrDefault("HEIGHT", "3"));
            
            // Set size
            template.setSize(width, height, length);
            
            // Determine primary blocks based on culture and parameters
            Block primaryBlock = switch(culture) {
                case "egyptian" -> Blocks.SANDSTONE;
                case "roman" -> Blocks.STONE_BRICKS;
                case "victorian" -> Blocks.BRICKS;
                case "nyc" -> Blocks.SMOOTH_STONE;
                default -> Blocks.COBBLESTONE;
            };
            
            Block floorBlock = switch(culture) {
                case "egyptian" -> Blocks.SMOOTH_SANDSTONE;
                case "roman" -> Blocks.POLISHED_ANDESITE;
                case "victorian" -> Blocks.DARK_OAK_PLANKS;
                case "nyc" -> Blocks.POLISHED_ANDESITE;
                default -> Blocks.OAK_PLANKS;
            };
            
            Block roofBlock = switch(culture) {
                case "egyptian" -> Blocks.CUT_SANDSTONE;
                case "roman" -> Blocks.STONE;
                case "victorian" -> Blocks.DARK_OAK_STAIRS;
                case "nyc" -> Blocks.CHISELED_STONE_BRICKS;
                default -> Blocks.OAK_STAIRS;
            };
            
            // Parse materials from parameters if available
            String materials = params.getOrDefault("MATERIALS", "");
            if (materials.toLowerCase().contains("sandstone")) {
                primaryBlock = Blocks.SANDSTONE;
            } else if (materials.toLowerCase().contains("brick")) {
                primaryBlock = Blocks.BRICKS;
            } else if (materials.toLowerCase().contains("quartz")) {
                primaryBlock = Blocks.QUARTZ_BLOCK;
            } else if (materials.toLowerCase().contains("stone")) {
                primaryBlock = Blocks.STONE_BRICKS;
            }
            
            // Create list for structure blocks
            List<StructureTemplate.StructureBlockInfo> blocks = new ArrayList<>();
            
            // Generate floor
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < length; z++) {
                    blocks.add(new StructureTemplate.StructureBlockInfo(
                        new BlockPos(x, 0, z), 
                        floorBlock.getDefaultState(),
                        null
                    ));
                }
            }
            
            // Generate walls
            for (int y = 1; y < height - 1; y++) {
                for (int x = 0; x < width; x++) {
                    for (int z = 0; z < length; z++) {
                        // Only place walls on the perimeter
                        if (x == 0 || x == width - 1 || z == 0 || z == length - 1) {
                            // Add doorway in the middle of one wall
                            if (y <= 2 && ((x == width / 2 && z == 0) || (x == 0 && z == length / 2))) {
                                continue; // Skip block for doorway
                            }
                            
                            // Add windows on walls
                            if (y == 2 && (x % 3 == 0 || z % 3 == 0) && !(x == 0 && z == 0) && 
                                !(x == width - 1 && z == length - 1)) {
                                blocks.add(new StructureTemplate.StructureBlockInfo(
                                    new BlockPos(x, y, z),
                                    Blocks.GLASS_PANE.getDefaultState(),
                                    null
                                ));
                            } else {
                                blocks.add(new StructureTemplate.StructureBlockInfo(
                                    new BlockPos(x, y, z),
                                    primaryBlock.getDefaultState(),
                                    null
                                ));
                            }
                        }
                    }
                }
            }
            
            // Generate roof
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < length; z++) {
                    blocks.add(new StructureTemplate.StructureBlockInfo(
                        new BlockPos(x, height - 1, z),
                        roofBlock.getDefaultState(),
                        null
                    ));
                }
            }
            
            // Add additional features based on culture
            switch (culture) {
                case "egyptian" -> {
                    // Add pillars in corners
                    blocks.add(new StructureTemplate.StructureBlockInfo(
                        new BlockPos(1, 1, 1),
                        Blocks.CHISELED_SANDSTONE.getDefaultState(),
                        null
                    ));
                    blocks.add(new StructureTemplate.StructureBlockInfo(
                        new BlockPos(width - 2, 1, 1),
                        Blocks.CHISELED_SANDSTONE.getDefaultState(),
                        null
                    ));
                    blocks.add(new StructureTemplate.StructureBlockInfo(
                        new BlockPos(1, 1, length - 2),
                        Blocks.CHISELED_SANDSTONE.getDefaultState(),
                        null
                    ));
                    blocks.add(new StructureTemplate.StructureBlockInfo(
                        new BlockPos(width - 2, 1, length - 2),
                        Blocks.CHISELED_SANDSTONE.getDefaultState(),
                        null
                    ));
                }
                case "roman" -> {
                    // Add columns at entrance
                    int entranceX = width / 2;
                    for (int y = 1; y < height - 1; y++) {
                        blocks.add(new StructureTemplate.StructureBlockInfo(
                            new BlockPos(entranceX - 1, y, 0),
                            Blocks.QUARTZ_PILLAR.getDefaultState(),
                            null
                        ));
                        blocks.add(new StructureTemplate.StructureBlockInfo(
                            new BlockPos(entranceX + 1, y, 0),
                            Blocks.QUARTZ_PILLAR.getDefaultState(),
                            null
                        ));
                    }
                }
                case "victorian" -> {
                    // Add fancy roof details
                    for (int x = 0; x < width; x += 2) {
                        blocks.add(new StructureTemplate.StructureBlockInfo(
                            new BlockPos(x, height, 0),
                            Blocks.DARK_OAK_FENCE.getDefaultState(),
                            null
                        ));
                        blocks.add(new StructureTemplate.StructureBlockInfo(
                            new BlockPos(x, height, length - 1),
                            Blocks.DARK_OAK_FENCE.getDefaultState(),
                            null
                        ));
                    }
                }
                case "nyc" -> {
                    // Add modern details
                    for (int y = 1; y < height - 1; y += 2) {
                        for (int x = 2; x < width - 2; x += 3) {
                            blocks.add(new StructureTemplate.StructureBlockInfo(
                                new BlockPos(x, y, 0),
                                Blocks.IRON_BARS.getDefaultState(),
                                null
                            ));
                            blocks.add(new StructureTemplate.StructureBlockInfo(
                                new BlockPos(x, y, length - 1),
                                Blocks.IRON_BARS.getDefaultState(),
                                null
                            ));
                        }
                    }
                }
            }
            
            // Create PalettedBlockInfoList from blocks
            List<StructureTemplate.PalettedBlockInfoList> blockInfoLists = new ArrayList<>();
            blockInfoLists.add(new StructureTemplate.PalettedBlockInfoList(blocks));
            
            // Set blocks to the template
            template.setBlockInfoLists(blockInfoLists);
            
        } catch (Exception e) {
            LOGGER.error("Error creating procedural template", e);
            // If something goes wrong, create a simple fallback structure
            return createFallbackTemplate();
        }
        
        return template;
    }
    
    private StructureTemplate createFallbackTemplate() {
        // Create a simple fallback template
        StructureTemplate template = new StructureTemplate();
        template.setSize(5, 3, 5);
        
        List<StructureTemplate.StructureBlockInfo> blocks = new ArrayList<>();
        
        // Simple 5x3x5 structure with cobblestone
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                // Floor
                blocks.add(new StructureTemplate.StructureBlockInfo(
                    new BlockPos(x, 0, z),
                    Blocks.COBBLESTONE.getDefaultState(),
                    null
                ));
                
                // Ceiling
                blocks.add(new StructureTemplate.StructureBlockInfo(
                    new BlockPos(x, 2, z),
                    Blocks.COBBLESTONE.getDefaultState(),
                    null
                ));
                
                // Walls
                if (x == 0 || x == 4 || z == 0 || z == 4) {
                    // Skip the center of one wall for a door
                    if (!(x == 2 && z == 0)) {
                        blocks.add(new StructureTemplate.StructureBlockInfo(
                            new BlockPos(x, 1, z),
                            Blocks.COBBLESTONE.getDefaultState(),
                            null
                        ));
                    }
                }
            }
        }
        
        // Create PalettedBlockInfoList from blocks
        List<StructureTemplate.PalettedBlockInfoList> blockInfoLists = new ArrayList<>();
        blockInfoLists.add(new StructureTemplate.PalettedBlockInfoList(blocks));
        
        try {
            template.setBlockInfoLists(blockInfoLists);
        } catch (Exception e) {
            LOGGER.error("Error creating fallback template", e);
        }
        
        return template;
    }
}