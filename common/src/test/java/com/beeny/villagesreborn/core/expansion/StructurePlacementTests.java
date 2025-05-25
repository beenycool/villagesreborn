package com.beeny.villagesreborn.core.expansion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.beeny.villagesreborn.core.common.BlockPos;
import com.beeny.villagesreborn.core.common.NBTCompound;

class StructurePlacementTests {
    
    @Mock
    private FabricStructurePlacementAPI mockPlacementAPI;
    
    @Mock
    private StructureTemplate mockTemplate;
    
    @Mock
    private NBTCompound mockNBTData;
    
    private VillageExpansionManager expansionManager;
    private StructurePlacer structurePlacer;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        structurePlacer = new StructurePlacer(mockPlacementAPI);
        expansionManager = new VillageExpansionManager(structurePlacer);
    }
    
    @Test
    @DisplayName("Should invoke Fabric placement API with correct coordinates and rotation")
    void testStructurePlacementWithCorrectParameters() {
        // Given: structure template and placement parameters
        BlockPos targetPosition = new BlockPos(100, 64, 200);
        StructureRotation rotation = StructureRotation.CLOCKWISE_90;
        PlacementSettings settings = PlacementSettings.builder()
                .rotation(rotation)
                .mirror(StructureMirror.NONE)
                .ignoreEntities(false)
                .build();
        
        when(mockTemplate.getSize()).thenReturn(new BlockPos(16, 8, 16));
        when(mockPlacementAPI.place(any(), any(), any(), any())).thenReturn(true);
        
        // When: placing structure through expansion manager
        boolean placed = expansionManager.placeStructure(mockTemplate, targetPosition, settings);
        
        // Then: Fabric API should be called with correct parameters
        verify(mockPlacementAPI).place(
                eq(mockTemplate),
                eq(targetPosition),
                eq(settings),
                any(World.class)
        );
        assertTrue(placed, "Structure placement should succeed");
    }
    
    @Test
    @DisplayName("Should handle different rotation values correctly")
    void testMultipleRotationValues() {
        // Given: different rotation scenarios
        BlockPos position = new BlockPos(0, 64, 0);
        StructureRotation[] rotations = {
                StructureRotation.NONE,
                StructureRotation.CLOCKWISE_90,
                StructureRotation.CLOCKWISE_180,
                StructureRotation.COUNTERCLOCKWISE_90
        };
        
        when(mockTemplate.getSize()).thenReturn(new BlockPos(8, 4, 8));
        when(mockPlacementAPI.place(any(), any(), any(), any())).thenReturn(true);
        
        // When: placing structure with each rotation
        for (StructureRotation rotation : rotations) {
            PlacementSettings settings = PlacementSettings.builder()
                    .rotation(rotation)
                    .build();
            
            boolean placed = expansionManager.placeStructure(mockTemplate, position, settings);
            
            // Then: each placement should succeed with correct rotation
            assertTrue(placed, "Structure should place with rotation: " + rotation);
        }
        
        // Verify all rotations were used
        verify(mockPlacementAPI, times(4)).place(
                eq(mockTemplate),
                eq(position),
                any(PlacementSettings.class),
                any(World.class)
        );
    }
    
    @Test
    @DisplayName("Should deserialize NBT template data into correct block structure")
    void testNBTTemplateDeserialization() {
        // Given: NBT data representing a structure template
        when(mockNBTData.contains("blocks")).thenReturn(true);
        when(mockNBTData.contains("entities")).thenReturn(true);
        when(mockNBTData.contains("size")).thenReturn(true);
        
        // Mock block data within NBT
        NBTCompound mockBlockData = mock(NBTCompound.class);
        when(mockBlockData.getString("name")).thenReturn("minecraft:oak_planks");
        when(mockBlockData.getCompound("pos")).thenReturn(mockNBTData);
        when(mockNBTData.getInt("x")).thenReturn(2);
        when(mockNBTData.getInt("y")).thenReturn(1);
        when(mockNBTData.getInt("z")).thenReturn(3);
        
        NBTCompound[] blockArray = {mockBlockData};
        when(mockNBTData.getCompoundArray("blocks")).thenReturn(blockArray);
        
        // When: deserializing NBT to structure template
        StructureTemplate template = StructureTemplate.fromNBT(mockNBTData);
        
        // Then: template should contain correct block data
        assertNotNull(template, "Template should be created from NBT");
        assertEquals(1, template.getBlockCount(), "Template should contain one block");
        
        StructureBlock block = template.getBlockAt(0);
        assertEquals("minecraft:oak_planks", block.getBlockType());
        assertEquals(new BlockPos(2, 1, 3), block.getPosition());
    }
    
    @Test
    @DisplayName("Should validate structure bounds before placement")
    void testStructureBoundsValidation() {
        // Given: structure template with specific dimensions
        BlockPos templateSize = new BlockPos(20, 10, 20);
        when(mockTemplate.getSize()).thenReturn(templateSize);
        
        // Test positions: some valid, some invalid
        BlockPos validPosition = new BlockPos(100, 64, 100);
        BlockPos edgePosition = new BlockPos(-10, 0, -10); // Near world edge
        
        // When: validating placement bounds
        boolean validPlacement = structurePlacer.validatePlacementBounds(mockTemplate, validPosition);
        boolean edgePlacement = structurePlacer.validatePlacementBounds(mockTemplate, edgePosition);
        
        // Then: validation should work correctly
        assertTrue(validPlacement, "Valid position should pass bounds check");
        assertFalse(edgePlacement, "Edge position should fail bounds check");
    }
    
    @Test
    @DisplayName("Should handle placement failures gracefully")
    void testPlacementFailureHandling() {
        // Given: placement API that fails
        when(mockPlacementAPI.place(any(), any(), any(), any())).thenReturn(false);
        when(mockTemplate.getSize()).thenReturn(new BlockPos(8, 4, 8));
        
        BlockPos position = new BlockPos(50, 64, 50);
        PlacementSettings settings = PlacementSettings.builder().build();
        
        // When: attempting to place structure
        boolean placed = expansionManager.placeStructure(mockTemplate, position, settings);
        
        // Then: failure should be handled gracefully
        assertFalse(placed, "Placement should report failure");
        verify(mockPlacementAPI).place(any(), any(), any(), any());
    }
    
    @Test
    @DisplayName("Should process complex NBT structure with multiple blocks and entities")
    void testComplexNBTStructureProcessing() {
        // Given: complex NBT structure with multiple elements
        when(mockNBTData.contains("blocks")).thenReturn(true);
        when(mockNBTData.contains("entities")).thenReturn(true);
        
        // Multiple blocks
        NBTCompound block1 = mock(NBTCompound.class);
        NBTCompound block2 = mock(NBTCompound.class);
        NBTCompound pos1 = mock(NBTCompound.class);
        NBTCompound pos2 = mock(NBTCompound.class);
        
        when(block1.getString("name")).thenReturn("minecraft:cobblestone");
        when(block1.getCompound("pos")).thenReturn(pos1);
        when(pos1.getInt("x")).thenReturn(0);
        when(pos1.getInt("y")).thenReturn(0);
        when(pos1.getInt("z")).thenReturn(0);
        
        when(block2.getString("name")).thenReturn("minecraft:oak_door");
        when(block2.getCompound("pos")).thenReturn(pos2);
        when(pos2.getInt("x")).thenReturn(1);
        when(pos2.getInt("y")).thenReturn(0);
        when(pos2.getInt("z")).thenReturn(0);
        
        NBTCompound[] blocks = {block1, block2};
        when(mockNBTData.getCompoundArray("blocks")).thenReturn(blocks);
        
        // When: processing complex structure
        StructureTemplate template = StructureTemplate.fromNBT(mockNBTData);
        
        // Then: all blocks should be processed correctly
        assertNotNull(template);
        assertEquals(2, template.getBlockCount());
        
        StructureBlock firstBlock = template.getBlockAt(0);
        assertEquals("minecraft:cobblestone", firstBlock.getBlockType());
        
        StructureBlock secondBlock = template.getBlockAt(1);
        assertEquals("minecraft:oak_door", secondBlock.getBlockType());
    }
}