package com.beeny.village;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.BlockMirror;
import net.minecraft.server.MinecraftServer;
import com.beeny.ai.LLMService;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.util.math.random.Random;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

public class CulturalStructureGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger("villagesreborn");
    private final String culture;
    private final Random random;

    public CulturalStructureGenerator(String culture) {
        this.culture = culture.toLowerCase();
        this.random = Random.create();
    }

    public void generateStructure(ServerWorld world, BlockPos pos, String structureType, double prosperityFactor) {
        String basePath = String.format("structures/%s/%s", culture, structureType.toLowerCase().replace(" ", "_"));
        // Create identifier correctly in 1.21.4
        Identifier templateId = Identifier.of("villagesreborn", basePath);

        // Try to load base template
        StructureTemplateManager manager = world.getStructureTemplateManager();
        Optional<StructureTemplate> baseTemplate = manager.getTemplate(templateId);

        if (baseTemplate.isPresent()) {
            // Modify template based on prosperity and culture
            modifyAndPlaceTemplate(world, pos, baseTemplate.get(), prosperityFactor);
        } else {
            // Generate procedural structure if template not found
            generateBasicStructure(world, pos, structureType, prosperityFactor);
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
        modifiedTemplate.place((ServerWorldAccess)world, finalPos, finalPos, placement, random, Block.NOTIFY_ALL);
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
            // Nether cultures
            case "infernal_roman" -> {
                replacements.put(Blocks.STONE_BRICKS, Blocks.NETHER_BRICKS);
                replacements.put(Blocks.STONE, Blocks.BLACKSTONE);
                replacements.put(Blocks.OAK_PLANKS, Blocks.CRIMSON_PLANKS);
            }
            case "obsidian_egyptian" -> {
                replacements.put(Blocks.STONE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS);
                replacements.put(Blocks.STONE, Blocks.OBSIDIAN);
                replacements.put(Blocks.COBBLESTONE, Blocks.GILDED_BLACKSTONE);
            }
            case "crimson_victorian" -> {
                replacements.put(Blocks.STONE_BRICKS, Blocks.CRIMSON_PLANKS);
                replacements.put(Blocks.STONE, Blocks.NETHER_BRICKS);
                replacements.put(Blocks.OAK_PLANKS, Blocks.CRIMSON_STEM);
            }
            case "warped_nyc" -> {
                replacements.put(Blocks.STONE_BRICKS, Blocks.WARPED_PLANKS);
                replacements.put(Blocks.STONE, Blocks.WARPED_STEM);
                replacements.put(Blocks.OAK_PLANKS, Blocks.WARPED_WART_BLOCK);
            }
            // End cultures
            case "ethereal_victorian" -> {
                replacements.put(Blocks.STONE_BRICKS, Blocks.END_STONE_BRICKS);
                replacements.put(Blocks.STONE, Blocks.PURPUR_BLOCK);
                replacements.put(Blocks.OAK_PLANKS, Blocks.PURPUR_PILLAR);
            }
            case "ender_nyc" -> {
                replacements.put(Blocks.STONE_BRICKS, Blocks.PURPUR_BLOCK);
                replacements.put(Blocks.STONE, Blocks.END_STONE_BRICKS);
                replacements.put(Blocks.OAK_PLANKS, Blocks.END_STONE);
            }
            case "purpur_roman" -> {
                replacements.put(Blocks.STONE_BRICKS, Blocks.PURPUR_PILLAR);
                replacements.put(Blocks.STONE, Blocks.PURPUR_BLOCK);
                replacements.put(Blocks.OAK_PLANKS, Blocks.END_STONE_BRICKS);
            }
            case "chorus_egyptian" -> {
                replacements.put(Blocks.STONE_BRICKS, Blocks.END_STONE);
                replacements.put(Blocks.STONE, Blocks.END_STONE_BRICKS);
                replacements.put(Blocks.COBBLESTONE, Blocks.PURPUR_BLOCK);
            }
            // Original cultures
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

    private StructureTemplate applyBlockReplacements(StructureTemplate original, Map<Block, Block> replacements) {
        // In Minecraft 1.21.4, the template processing needs to be done differently
        // as the PalettedBlockInfoList constructor is no longer accessible and 
        // setPalettes() method no longer exists
        StructureTemplate modified = new StructureTemplate();
        
        // Get all blocks from the structure
        List<Structure.StructureBlockInfo> processedBlocks = new ArrayList<>();
        
        // Process each block individually
        List<Structure.StructureBlockInfo> blocks = original.getInfosForBlock();
        for (Structure.StructureBlockInfo info : blocks) {
            BlockState currentState = info.state();
            Block currentBlock = currentState.getBlock();

            // Check if this block should be replaced
            if (replacements.containsKey(currentBlock)) {
                Block replacementBlock = replacements.get(currentBlock);
                processedBlocks.add(new Structure.StructureBlockInfo(
                    info.pos(),
                    replacementBlock.getDefaultState(),
                    info.nbt() != null ? info.nbt().copy() : null
                ));
            } else {
                processedBlocks.add(info);
            }
        }
        
        // Create a new template with the modifications
        // In 1.21.4, we need to manually copy all the relevant data
        modified.setAuthor("VillagesReborn");
        modified.addBlocksFromStructure(processedBlocks, original.getSize());

        return modified;
    }

    private void addProceduralDetails(ServerWorld world, BlockPos pos, BlockPos size, double prosperityFactor) {
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
            world.setBlockState(decorPos, random.nextBoolean() ? 
                Blocks.CHISELED_SANDSTONE.getDefaultState() : 
                Blocks.CUT_SANDSTONE.getDefaultState());
        }
    }

    private void addRomanDecorations(ServerWorld world, BlockPos pos, BlockPos size, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos decorPos = getRandomPositionInStructure(pos, size);
            world.setBlockState(decorPos, random.nextBoolean() ? 
                Blocks.CHISELED_STONE_BRICKS.getDefaultState() : 
                Blocks.STONE_BRICK_WALL.getDefaultState());
        }
    }

    private void addVictorianDecorations(ServerWorld world, BlockPos pos, BlockPos size, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos decorPos = getRandomPositionInStructure(pos, size);
            world.setBlockState(decorPos, random.nextBoolean() ? 
                Blocks.IRON_BARS.getDefaultState() : 
                Blocks.LANTERN.getDefaultState());
        }
    }

    private void addNYCDecorations(ServerWorld world, BlockPos pos, BlockPos size, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos decorPos = getRandomPositionInStructure(pos, size);
            world.setBlockState(decorPos, random.nextBoolean() ? 
                Blocks.SEA_LANTERN.getDefaultState() : 
                Blocks.GLASS_PANE.getDefaultState());
        }
    }

    private BlockPos getRandomPositionInStructure(BlockPos pos, BlockPos size) {
        return pos.add(
            random.nextInt(size.getX()),
            random.nextInt(size.getY()),
            random.nextInt(size.getZ())
        );
    }

    private void generateBasicStructure(ServerWorld world, BlockPos pos, String structureType, double prosperityFactor) {
        int width = 5;
        int height = 3;
        int length = 5;
        
        StructureTemplate template = new StructureTemplate();
        List<Structure.StructureBlockInfo> blocks = new ArrayList<>();
        
        // Generate a simple building
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    // Skip interior blocks
                    if (y > 0 && y < height - 1 && x > 0 && x < width - 1 && z > 0 && z < length - 1) {
                        continue;
                    }
                    
                    // Create doorway
                    if (y == 1 && x == width / 2 && z == 0) {
                        continue;
                    }
                    
                    Block block = getBlockForStructure(y == 0, y == height - 1);
                    blocks.add(new Structure.StructureBlockInfo(
                        new BlockPos(x, y, z),
                        block.getDefaultState(),
                        null
                    ));
                }
            }
        }
        
        // Add blocks to the template
        template.setAuthor("VillagesReborn");
        template.addBlocksFromStructure(blocks, new Vec3i(width, height, length));
        
        modifyAndPlaceTemplate(world, pos, template, prosperityFactor);
    }
    
    private Block getBlockForStructure(boolean isFloor, boolean isRoof) {
        return switch(culture) {
            case "egyptian" -> isFloor ? Blocks.SMOOTH_SANDSTONE : (isRoof ? Blocks.CUT_SANDSTONE : Blocks.SANDSTONE);
            case "roman" -> isFloor ? Blocks.POLISHED_ANDESITE : (isRoof ? Blocks.STONE : Blocks.STONE_BRICKS);
            case "victorian" -> isFloor ? Blocks.STONE_BRICKS : (isRoof ? Blocks.DARK_OAK_PLANKS : Blocks.BRICKS);
            case "nyc" -> isFloor ? Blocks.POLISHED_ANDESITE : (isRoof ? Blocks.SMOOTH_STONE : Blocks.STONE);
            default -> isFloor ? Blocks.COBBLESTONE : (isRoof ? Blocks.OAK_PLANKS : Blocks.STONE_BRICKS);
        };
    }
}
