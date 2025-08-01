package com.beeny.client.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

import com.beeny.Villagersreborn;
import com.beeny.data.VillagerData;
import com.beeny.network.FamilyTreeDataPacket;
import com.beeny.system.VillagerAncestryManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

public class VillagerFamilyTreeScreen extends Screen {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(VillagerFamilyTreeScreen.class);
    private static final int FAMILY_BOX_WIDTH = 140;
    private static final int FAMILY_BOX_HEIGHT = 60;
    private static final int GENERATION_GAP = 80;
    private static final int SIBLING_GAP = 140;
    private static final int SPOUSE_OFFSET = 160;
    
    // Colors for different family relationships
    private static final int CURRENT_VILLAGER_COLOR = 0xFF4A90E2;  // Blue
    private static final int SPOUSE_COLOR = 0xFFE74C3C;            // Red
    private static final int PARENT_COLOR = 0xFF27AE60;            // Green
    private static final int CHILD_COLOR = 0xFFF39C12;             // Orange
    private static final int SIBLING_COLOR = 0xFF9B59B6;          // Purple
    private static final int GRANDPARENT_COLOR = 0xFF2ECC71;      // Light Green
    private static final int GRANDCHILD_COLOR = 0xFFE67E22;       // Dark Orange
    
    private final VillagerEntity currentVillager;
    private final long villagerId;
    private final Map<String, FamilyMember> familyTree;
    private final Map<String, VillagerData> ancestors;
    private final List<FamilyMember> displayedMembers;
    private final List<FamilyTreeDataPacket.FamilyMemberData> serverFamilyMembers;
    private ButtonWidget closeButton;
    private ButtonWidget backButton;
    
    private int treeOffsetX = 0;
    private int treeOffsetY = 0;
    private float animationTick = 0;
    
    public VillagerFamilyTreeScreen(VillagerEntity villager) {
        super(Text.literal("Family Tree"));
        LOGGER.info("[VillagerFamilyTreeScreen] Constructor called for villager: " + villager.getId());
        this.currentVillager = villager;
        this.villagerId = villager.getId();
        this.familyTree = new HashMap<>();
        this.ancestors = VillagerAncestryManager.generateAncestors(villager, 4); // 4 generations
        this.displayedMembers = new ArrayList<>();
        this.serverFamilyMembers = new ArrayList<>();
        LOGGER.info("[VillagerFamilyTreeScreen] Ancestors generated: " + ancestors.size());
        buildFamilyTree();
        LOGGER.info("[VillagerFamilyTreeScreen] Family tree built with " + displayedMembers.size() + " members");
    }
    
    public VillagerFamilyTreeScreen(long villagerId, List<FamilyTreeDataPacket.FamilyMemberData> familyMembers) {
        super(Text.literal("Family Tree"));
        this.currentVillager = null;
        this.villagerId = villagerId;
        this.familyTree = new HashMap<>();
        this.ancestors = new HashMap<>();
        this.displayedMembers = new ArrayList<>();
        this.serverFamilyMembers = familyMembers;
        buildFamilyTreeFromServerData();
    }
    
    @Override
    protected void init() {
        super.init();
        
        closeButton = ButtonWidget.builder(
            Text.literal("Close"),
            btn -> close()
        ).dimensions(width - 80, height - 30, 70, 20).build();
        
        backButton = ButtonWidget.builder(
            Text.literal("← Back"),
            btn -> {
                if (currentVillager != null) {
                    focusOnVillager(currentVillager);
                } else {
                    close();
                }
            }
        ).dimensions(10, height - 30, 70, 20).build();
        
        addDrawableChild(closeButton);
        addDrawableChild(backButton);
        
        calculateTreeLayout();
    }
    
    private void buildFamilyTree() {
        if (client == null || client.world == null) {
            LOGGER.warn("[FamilyTree] Client or world is null!");
            return;
        }
        
        if (currentVillager == null) {
            // This is server data, handled by buildFamilyTreeFromServerData
            return;
        }
        
        VillagerData currentData = currentVillager.getAttached(Villagersreborn.VILLAGER_DATA);
        if (currentData == null) {
            LOGGER.warn("[FamilyTree] No VillagerData attached to current villager!");
            // Try to create a basic VillagerData if none exists
            currentData = new VillagerData();
            currentData.setName("Villager_" + currentVillager.getId());
            currentVillager.setAttached(Villagersreborn.VILLAGER_DATA, currentData);
        }
        
        LOGGER.info("[FamilyTree] Building tree for: " + currentData.getName());
        
        // Deprecated: client-side scans removed. Family trees are now server-computed.
        // Start with current villager box and rely on serverFamilyMembers provided via packet.
        FamilyMember current = new FamilyMember(currentVillager, RelationshipType.CURRENT);
        familyTree.put(currentVillager.getUuidAsString(), current);
        displayedMembers.add(current);

        // Build from server-provided data if available
        if (!serverFamilyMembers.isEmpty()) {
            buildFamilyTreeFromServerData();
        }

        // Sort members by generation for display
        displayedMembers.sort((a, b) -> Integer.compare(a.generation, b.generation));
    }
    
    private void buildFamilyTreeFromServerData() {
        // Build family tree from server-provided data
        for (FamilyTreeDataPacket.FamilyMemberData memberData : serverFamilyMembers) {
            FamilyMember member = new FamilyMember(memberData);
            familyTree.put(memberData.getUuid(), member);
            displayedMembers.add(member);
        }
        
        // Sort members by generation for display
        displayedMembers.sort((a, b) -> Integer.compare(a.generation, b.generation));
    }
    
    private void buildRelationshipsFromVillagers(List<VillagerEntity> villagers, VillagerData currentData) {
        if (currentVillager == null) return;
        
        String currentUuid = currentVillager.getUuidAsString();
        
        for (VillagerEntity villager : villagers) {
            if (villager.equals(currentVillager)) continue;
            
            VillagerData data = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            if (data == null) continue;
            
            String villagerUuid = villager.getUuidAsString();
            RelationshipType relationship = determineRelationship(currentData, data, currentUuid, villagerUuid);
            
            if (relationship != RelationshipType.NONE) {
                FamilyMember member = new FamilyMember(villager, relationship);
                familyTree.put(villagerUuid, member);
                displayedMembers.add(member);
            }
        }
        
        // Set generation levels
        setGenerationLevels();
    }
    
    private void addAncestorsToTree() {
        for (Map.Entry<String, VillagerData> entry : ancestors.entrySet()) {
            VillagerData ancestorData = entry.getValue();
            
            // Create a fictional FamilyMember for ancestors
            FamilyMember ancestorMember = new FamilyMember(null, RelationshipType.ANCESTOR);
            ancestorMember.ancestorData = ancestorData;
            
            familyTree.put(entry.getKey(), ancestorMember);
            displayedMembers.add(ancestorMember);
        }
    }
    
    private RelationshipType determineRelationship(VillagerData currentData, VillagerData otherData, 
                                                   String currentUuid, String otherUuid) {
        
        // Spouse check
        if (currentData.getSpouseId().equals(otherUuid)) {
            return RelationshipType.SPOUSE;
        }
        
        // Child check
        if (currentData.getChildrenIds().contains(otherUuid)) {
            return RelationshipType.CHILD;
        }
        
        // Parent check
        if (otherData.getChildrenIds().contains(currentUuid)) {
            return RelationshipType.PARENT;
        }
        
        // Sibling check (share same parents)
        for (VillagerEntity possibleParent : getAllVillagers()) {
            VillagerData parentData = possibleParent.getAttached(Villagersreborn.VILLAGER_DATA);
            if (parentData != null && 
                parentData.getChildrenIds().contains(currentUuid) &&
                parentData.getChildrenIds().contains(otherUuid)) {
                return RelationshipType.SIBLING;
            }
        }
        
        // Grandparent check (parent's parent)
        for (String parentId : getParentIds(currentData)) {
            VillagerEntity parent = findVillagerById(parentId);
            if (parent != null) {
                VillagerData parentData = parent.getAttached(Villagersreborn.VILLAGER_DATA);
                if (parentData != null) {
                    for (String grandparentId : getParentIds(parentData)) {
                        if (grandparentId.equals(otherUuid)) {
                            return RelationshipType.GRANDPARENT;
                        }
                    }
                }
            }
        }
        
        // Grandchild check (child's child)
        for (String childId : currentData.getChildrenIds()) {
            VillagerEntity child = findVillagerById(childId);
            if (child != null) {
                VillagerData childData = child.getAttached(Villagersreborn.VILLAGER_DATA);
                if (childData != null && childData.getChildrenIds().contains(otherUuid)) {
                    return RelationshipType.GRANDCHILD;
                }
            }
        }
        
        return RelationshipType.NONE;
    }
    
    private List<String> getParentIds(VillagerData data) {
        List<String> parentIds = new ArrayList<>();
        for (VillagerEntity villager : getAllVillagers()) {
            VillagerData villagerData = villager.getAttached(Villagersreborn.VILLAGER_DATA);
            if (villagerData != null && villagerData.getChildrenIds().contains(data.getName())) {
                parentIds.add(villager.getUuidAsString());
            }
        }
        return parentIds;
    }
    
    private VillagerEntity findVillagerById(String uuid) {
        for (VillagerEntity villager : getAllVillagers()) {
            if (villager.getUuidAsString().equals(uuid)) {
                return villager;
            }
        }
        return null;
    }
    
    private List<VillagerEntity> getAllVillagers() {
        // Deprecated: client should not scan world for family tree. Server is source of truth.
        return Collections.emptyList();
    }
    
    private void setGenerationLevels() {
        // Set current villager as generation 0
        if (currentVillager != null) {
            FamilyMember current = familyTree.get(currentVillager.getUuidAsString());
            if (current != null) current.generation = 0;
        }
        
        // Set other generations relative to current
        for (FamilyMember member : displayedMembers) {
            switch (member.relationship) {
                case GRANDPARENT -> member.generation = -2;
                case PARENT -> member.generation = -1;
                case CURRENT -> member.generation = 0;
                case SPOUSE -> member.generation = 0;
                case SIBLING -> member.generation = 0;
                case CHILD -> member.generation = 1;
                case GRANDCHILD -> member.generation = 2;
                case ANCESTOR -> {
                    // Simple generation assignment for ancestors based on their position in the tree
                    // We'll determine this based on the ancestor's name key in the ancestors map
                    // For now, just assign them to generations -1 through -4
                    String ancestorKey = "";
                    for (Map.Entry<String, VillagerData> entry : ancestors.entrySet()) {
                        if (entry.getValue() == member.ancestorData) {
                            ancestorKey = entry.getKey();
                            break;
                        }
                    }
                    
                    // Simple generation assignment - spread ancestors across generations -1 to -4
                    int ancestorIndex = new ArrayList<>(ancestors.keySet()).indexOf(ancestorKey);
                    member.generation = -1 - (ancestorIndex % 4); // Distribute across -1, -2, -3, -4
                }
            }
        }
    }
    
    private void calculateTreeLayout() {
        Map<Integer, List<FamilyMember>> generationGroups = new HashMap<>();
        
        // Group by generation
        for (FamilyMember member : displayedMembers) {
            generationGroups.computeIfAbsent(member.generation, k -> new ArrayList<>()).add(member);
        }
        
        // Find the range of generations
        int minGeneration = generationGroups.keySet().stream().mapToInt(Integer::intValue).min().orElse(0);
        int maxGeneration = generationGroups.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        
        // Calculate layout to fit all generations on screen
        int totalGenerations = maxGeneration - minGeneration + 1;
        int totalHeight = totalGenerations * GENERATION_GAP;
        
        // Start from top of screen with some margin, positioning oldest generation first
        int startY = Math.max(100, (height - totalHeight) / 2);
        int centerX = width / 2;
        
        // Position each generation
        for (Map.Entry<Integer, List<FamilyMember>> entry : generationGroups.entrySet()) {
            int generation = entry.getKey();
            List<FamilyMember> members = entry.getValue();
            
            // Map generation to Y position (oldest at top, youngest at bottom)
            int generationIndex = maxGeneration - generation; // Invert so ancestors are at top
            int generationY = startY + (generationIndex * GENERATION_GAP);
            int startX = centerX - ((members.size() - 1) * SIBLING_GAP) / 2;
            
            for (int i = 0; i < members.size(); i++) {
                FamilyMember member = members.get(i);
                member.displayX = startX + (i * SIBLING_GAP);
                member.displayY = generationY;
                
                // Special positioning for spouse (next to current villager)
                if (member.relationship == RelationshipType.SPOUSE && currentVillager != null) {
                    FamilyMember current = familyTree.get(currentVillager.getUuidAsString());
                    if (current != null) {
                        member.displayX = current.displayX + SPOUSE_OFFSET;
                        member.displayY = current.displayY;
                    }
                }
            }
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        animationTick += delta;
        
        // Background
        context.fill(0, 0, width, height, 0xFF1A1A1A);
        
        // Title
        Text title = Text.literal("📋 Family Tree: " + getCurrentVillagerName()).formatted(Formatting.GOLD);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFFFF);
        
        // Debug info - show what we have
        context.drawTextWithShadow(textRenderer, Text.literal("Displayed Members: " + displayedMembers.size()), 10, 30, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Ancestors: " + ancestors.size()), 10, 45, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Family Tree: " + familyTree.size()), 10, 60, 0xFFFFFFFF);
        
        // If no family members, show a message
        if (displayedMembers.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, 
                Text.literal("No family data found - this might be a bug!"), 
                width / 2, height / 2, 0xFFFF0000);
            
            // Show details about the current villager
            VillagerData data = currentVillager != null ? currentVillager.getAttached(Villagersreborn.VILLAGER_DATA) : null;
            if (data == null) {
                context.drawCenteredTextWithShadow(textRenderer, 
                    Text.literal("Villager has no VillagerData attached!"), 
                    width / 2, height / 2 + 20, 0xFFFF0000);
            } else {
                context.drawCenteredTextWithShadow(textRenderer, 
                    Text.literal("Villager name: " + data.getName()), 
                    width / 2, height / 2 + 20, 0xFFFFFF00);
            }
        } else {
            // Draw family connections
            drawFamilyConnections(context);
            
            // Draw family members
            drawFamilyMembers(context, mouseX, mouseY);
        }
        
        super.render(context, mouseX, mouseY, delta);
        
        // Draw tooltips
        if (!displayedMembers.isEmpty()) {
            drawTooltips(context, mouseX, mouseY);
        }
    }
    
    private void drawFamilyConnections(DrawContext context) {
        if (currentVillager == null) return;
        
        FamilyMember current = familyTree.get(currentVillager.getUuidAsString());
        if (current == null) return;
        
        for (FamilyMember member : displayedMembers) {
            if (member.relationship == RelationshipType.CURRENT) continue;
            
            int color = 0x55FFFFFF;
            int lineWidth = 1;
            
            switch (member.relationship) {
                case PARENT -> {
                    // Line from parent to current
                    drawLine(context, member.displayX + FAMILY_BOX_WIDTH/2, member.displayY + FAMILY_BOX_HEIGHT,
                            current.displayX + FAMILY_BOX_WIDTH/2, current.displayY, color, lineWidth);
                }
                case CHILD -> {
                    // Line from current to child
                    drawLine(context, current.displayX + FAMILY_BOX_WIDTH/2, current.displayY + FAMILY_BOX_HEIGHT,
                            member.displayX + FAMILY_BOX_WIDTH/2, member.displayY, color, lineWidth);
                }
                case SPOUSE -> {
                    // Line connecting spouses
                    drawLine(context, current.displayX + FAMILY_BOX_WIDTH, current.displayY + FAMILY_BOX_HEIGHT/2,
                            member.displayX, member.displayY + FAMILY_BOX_HEIGHT/2, 0x55FF0000, 2);
                }
            }
        }
    }
    
    private void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color, int width) {
        // Simple line drawing - could be enhanced with proper line rendering
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            context.fill(x - width/2, y - width/2, x + width/2, y + width/2, color);
        }
    }
    
    private void drawFamilyMembers(DrawContext context, int mouseX, int mouseY) {
        for (FamilyMember member : displayedMembers) {
            boolean isHovered = isMouseOverMember(mouseX, mouseY, member);
            
            // Box background
            int backgroundColor = getRelationshipColor(member.relationship);
            if (isHovered) {
                backgroundColor = brightenColor(backgroundColor);
            }
            
            // Pulsing effect for current villager
            if (member.relationship == RelationshipType.CURRENT) {
                float pulse = (MathHelper.sin(animationTick * 0.2f) + 1) * 0.1f + 0.9f;
                backgroundColor = (int)(255 * pulse) << 24 | (backgroundColor & 0x00FFFFFF);
            }
            
            context.fill(member.displayX, member.displayY, 
                        member.displayX + FAMILY_BOX_WIDTH, member.displayY + FAMILY_BOX_HEIGHT, 
                        backgroundColor);
            
            // Border
            context.drawBorder(member.displayX, member.displayY, FAMILY_BOX_WIDTH, FAMILY_BOX_HEIGHT, 0xFFFFFFFF);
            
            // Get data (either from living villager, ancestor, or server data)
            String name;
            boolean isAlive;
            long birthTime;
            long deathTime;
            VillagerData data = null;
            
            if (member.serverData != null) {
                name = member.serverData.getName();
                isAlive = member.serverData.isAlive();
                birthTime = member.serverData.getBirthTime();
                deathTime = member.serverData.getDeathTime();
            } else {
                data = member.villager != null ?
                    member.villager.getAttached(Villagersreborn.VILLAGER_DATA) :
                    member.ancestorData;
                name = data != null ? data.getName() : "Unknown";
                isAlive = data != null ? data.isAlive() : true;
                birthTime = data != null ? data.getBirthTime() : 0;
                deathTime = data != null ? data.getDeathTime() : 0;
            }
            
            if (name.length() > 15) {
                name = name.substring(0, 15) + "...";
            }
            
            // Name
            Formatting nameColor = !isAlive ? Formatting.GRAY : Formatting.WHITE;
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(name).formatted(nameColor),
                member.displayX + FAMILY_BOX_WIDTH/2, member.displayY + 8, 0xFFFFFFFF);
            
            // Relationship label
            String relationship = getRelationshipLabel(member.relationship);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(relationship).formatted(Formatting.GRAY),
                member.displayX + FAMILY_BOX_WIDTH/2, member.displayY + 24, 0xFFAAAAAA);
            
            // Birth/Death dates
            if (member.serverData != null) {
                String dateInfo;
                if (isAlive) {
                    dateInfo = VillagerAncestryManager.formatHistoricalDate(birthTime);
                } else {
                    String birthDate = VillagerAncestryManager.formatHistoricalDate(birthTime);
                    String deathDate = VillagerAncestryManager.formatHistoricalDate(deathTime);
                    dateInfo = "†" + birthDate.split(" ")[0]; // Show death symbol and years ago
                }
                
                if (dateInfo.length() > 20) {
                    dateInfo = dateInfo.substring(0, 20) + "...";
                }
                
                context.drawCenteredTextWithShadow(textRenderer, Text.literal(dateInfo).formatted(Formatting.DARK_GRAY),
                    member.displayX + FAMILY_BOX_WIDTH/2, member.displayY + 40, 0xFF888888);
            } else if (data != null) {
                String dateInfo;
                if (isAlive) {
                    dateInfo = VillagerAncestryManager.formatHistoricalDate(birthTime);
                } else {
                    String birthDate = VillagerAncestryManager.formatHistoricalDate(birthTime);
                    String deathDate = VillagerAncestryManager.formatHistoricalDate(deathTime);
                    dateInfo = "†" + birthDate.split(" ")[0]; // Show death symbol and years ago
                }
                
                if (dateInfo.length() > 20) {
                    dateInfo = dateInfo.substring(0, 20) + "...";
                }
                
                context.drawCenteredTextWithShadow(textRenderer, Text.literal(dateInfo).formatted(Formatting.DARK_GRAY),
                    member.displayX + FAMILY_BOX_WIDTH/2, member.displayY + 40, 0xFF888888);
            }
        }
    }
    
    private void drawTooltips(DrawContext context, int mouseX, int mouseY) {
        for (FamilyMember member : displayedMembers) {
            if (isMouseOverMember(mouseX, mouseY, member)) {
                List<Text> tooltip = buildMemberTooltip(member);
                context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
                break;
            }
        }
    }
    
    private List<Text> buildMemberTooltip(FamilyMember member) {
        List<Text> tooltip = new ArrayList<>();
        
        // Get data from appropriate source
        String name;
        String relationship;
        long birthTime;
        long deathTime;
        boolean isAlive;
        String personality;
        int happiness;
        String profession;
        String spouseName;
        int childrenCount;
        String birthPlace;
        String notes;
        
        if (member.serverData != null) {
            // Use server data
            name = member.serverData.getName();
            relationship = member.serverData.getRelationship();
            birthTime = member.serverData.getBirthTime();
            deathTime = member.serverData.getDeathTime();
            isAlive = member.serverData.isAlive();
            personality = member.serverData.getPersonality();
            happiness = member.serverData.getHappiness();
            profession = member.serverData.getProfession();
            spouseName = member.serverData.getSpouseName();
            childrenCount = member.serverData.getChildrenCount();
            birthPlace = member.serverData.getBirthPlace();
            notes = member.serverData.getNotes();
        } else {
            // Use client data
            VillagerData data = member.villager != null ?
                member.villager.getAttached(Villagersreborn.VILLAGER_DATA) :
                member.ancestorData;
            
            if (data == null) {
                return tooltip;
            }
            
            name = data.getName();
            relationship = getRelationshipLabel(member.relationship);
            birthTime = data.getBirthTime();
            deathTime = data.getDeathTime();
            isAlive = data.isAlive();
            personality = data.getPersonality();
            happiness = data.getHappiness();
            profession = !data.getProfessionHistory().isEmpty() ? data.getProfessionHistory().get(0) : "";
            spouseName = data.getSpouseName();
            childrenCount = data.getChildrenNames().size();
            birthPlace = data.getBirthPlace();
            notes = data.getNotes();
        }
        
        tooltip.add(Text.literal(name).formatted(Formatting.AQUA));
        tooltip.add(Text.literal("Relationship: " + relationship).formatted(Formatting.YELLOW));
        
        // Age and life status
        String ageInfo = VillagerAncestryManager.getAgeDescription(birthTime, deathTime, isAlive);
        Formatting ageColor = isAlive ? Formatting.GREEN : Formatting.GRAY;
        tooltip.add(Text.literal(ageInfo).formatted(ageColor));
        
        // Birth date
        String birthInfo = "Born: " + VillagerAncestryManager.formatHistoricalDate(birthTime);
        tooltip.add(Text.literal(birthInfo).formatted(Formatting.DARK_GREEN));
        
        // Death date for ancestors
        if (!isAlive && deathTime > 0) {
            String deathInfo = "Died: " + VillagerAncestryManager.formatHistoricalDate(deathTime);
            tooltip.add(Text.literal(deathInfo).formatted(Formatting.DARK_RED));
        }
        
        tooltip.add(Text.literal("Personality: " + personality).formatted(Formatting.LIGHT_PURPLE));
        
        if (isAlive) {
            tooltip.add(Text.literal("Happiness: " + happiness + "%").formatted(Formatting.GOLD));
        }
        
        // Profession/Historical role
        if (!profession.isEmpty()) {
            tooltip.add(Text.literal("Role: " + profession).formatted(Formatting.BLUE));
        }
        
        // Family info
        if (!spouseName.isEmpty()) {
            tooltip.add(Text.literal("Married to: " + spouseName).formatted(Formatting.RED));
        }
        
        if (childrenCount > 0) {
            tooltip.add(Text.literal("Children: " + childrenCount).formatted(Formatting.GREEN));
        }
        
        // Birth place
        if (!birthPlace.isEmpty()) {
            tooltip.add(Text.literal("From: " + birthPlace).formatted(Formatting.YELLOW));
        }
        
        // Historical notes for ancestors
        if (!isAlive && !notes.isEmpty()) {
            tooltip.add(Text.literal(notes).formatted(Formatting.DARK_AQUA));
        }
        
        if (member.villager != null && member.serverData == null) {
            tooltip.add(Text.literal("Click to focus on this villager").formatted(Formatting.GRAY));
        } else if (member.serverData != null) {
            tooltip.add(Text.literal("Family member data from server").formatted(Formatting.DARK_GRAY));
        } else {
            tooltip.add(Text.literal("Ancient ancestor - no longer living").formatted(Formatting.DARK_GRAY));
        }
        
        return tooltip;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            for (FamilyMember member : displayedMembers) {
                if (isMouseOverMember((int)mouseX, (int)mouseY, member)) {
                    // Only allow clicking on living villagers when using client data
                    if (member.villager != null && member.serverData == null) {
                        focusOnVillager(member.villager);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private void focusOnVillager(VillagerEntity newFocus) {
        if (client != null) {
            client.setScreen(new VillagerFamilyTreeScreen(newFocus));
        }
    }
    
    private boolean isMouseOverMember(int mouseX, int mouseY, FamilyMember member) {
        return mouseX >= member.displayX && mouseX <= member.displayX + FAMILY_BOX_WIDTH &&
               mouseY >= member.displayY && mouseY <= member.displayY + FAMILY_BOX_HEIGHT;
    }
    
    private int getRelationshipColor(RelationshipType relationship) {
        return switch (relationship) {
            case CURRENT -> CURRENT_VILLAGER_COLOR;
            case SPOUSE -> SPOUSE_COLOR;
            case PARENT -> PARENT_COLOR;
            case CHILD -> CHILD_COLOR;
            case SIBLING -> SIBLING_COLOR;
            case GRANDPARENT -> GRANDPARENT_COLOR;
            case GRANDCHILD -> GRANDCHILD_COLOR;
            case ANCESTOR -> 0xFF8B4513; // Brown for ancestors
            default -> 0xFF666666;
        };
    }
    
    private String getRelationshipLabel(RelationshipType relationship) {
        return switch (relationship) {
            case CURRENT -> "You";
            case SPOUSE -> "Spouse";
            case PARENT -> "Parent";
            case CHILD -> "Child";
            case SIBLING -> "Sibling";
            case GRANDPARENT -> "Grandparent";
            case GRANDCHILD -> "Grandchild";
            case ANCESTOR -> "Ancestor";
            default -> "Unknown";
        };
    }
    
    private int brightenColor(int color) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + 30);
        int g = Math.min(255, ((color >> 8) & 0xFF) + 30);
        int b = Math.min(255, (color & 0xFF) + 30);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
    
    private String getCurrentVillagerName() {
        if (currentVillager != null) {
            VillagerData data = currentVillager.getAttached(Villagersreborn.VILLAGER_DATA);
            return data != null ? data.getName() : "Unknown Villager";
        } else if (!serverFamilyMembers.isEmpty()) {
            // Find the current villager in the server data
            for (FamilyTreeDataPacket.FamilyMemberData member : serverFamilyMembers) {
                if (member.getRelationship().equals("CURRENT")) {
                    return member.getName();
                }
            }
            // Fallback to first member
            return serverFamilyMembers.get(0).getName();
        }
        return "Unknown Villager";
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    // Inner classes
    private static class FamilyMember {
        final VillagerEntity villager;
        final RelationshipType relationship;
        final FamilyTreeDataPacket.FamilyMemberData serverData;
        VillagerData ancestorData; // For generated ancestors
        int displayX, displayY;
        int generation;
        
        FamilyMember(VillagerEntity villager, RelationshipType relationship) {
            this.villager = villager;
            this.relationship = relationship;
            this.serverData = null;
        }
        
        FamilyMember(FamilyTreeDataPacket.FamilyMemberData serverData) {
            this.villager = null;
            this.relationship = RelationshipType.valueOf(serverData.getRelationship().toUpperCase());
            this.serverData = serverData;
            // Set generation based on relationship type
            this.generation = switch (this.relationship) {
                case GRANDPARENT -> -2;
                case PARENT -> -1;
                case CURRENT -> 0;
                case SPOUSE -> 0;
                case SIBLING -> 0;
                case CHILD -> 1;
                case GRANDCHILD -> 2;
                case ANCESTOR -> -1; // Default for ancestors
                default -> 0;
            };
        }
    }
    
    private enum RelationshipType {
        CURRENT, SPOUSE, PARENT, CHILD, SIBLING, GRANDPARENT, GRANDCHILD, ANCESTOR, NONE
    }
    
    private static RelationshipType valueOf(String relationship) {
        try {
            return RelationshipType.valueOf(relationship.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RelationshipType.NONE;
        }
    }
}