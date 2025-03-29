package com.beeny.gui;

import com.beeny.village.VillageCraftingManager;
import com.beeny.network.VillageCraftingClientNetwork;
import com.beeny.network.VillageCraftingClientHandler;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.client.render.RenderLayer;

public class VillageCraftingScreen extends Screen {
    // Update to use regular constructor rather than Identifier.of
    private static final Identifier BACKGROUND_TEXTURE = new Identifier("villagesreborn", "textures/gui/crafting_background.png");
    private static final Identifier ICONS_TEXTURE = new Identifier("minecraft", "textures/gui/widgets.png");
    private static final int BACKGROUND_WIDTH = 256;
    private static final int BACKGROUND_HEIGHT = 196;
    private static final int RECIPE_BUTTON_HEIGHT = 24;
    private static final int MAX_VISIBLE_RECIPES = 7;
    
    private final VillagerEntity villager;
    private final String culture;
    private final List<VillageCraftingManager.CraftingRecipe> recipes;
    private int selectedRecipe = -1;
    private int scrollOffset = 0;
    private float animationTime = 0;
    private boolean isScrolling = false;
    private ButtonWidget craftButton;

    // Tracking crafting state
    private String craftingStatus = "none";
    private String craftingMessage = "";
    private int craftingProgress = 0;
    private int craftingMaxProgress = 100;
    private String activeRecipeId = null;

    public VillageCraftingScreen(VillagerEntity villager, String culture) {
        super(Text.literal("Village Crafting"));
        this.villager = villager;
        this.culture = culture;
        this.recipes = VillageCraftingManager.getInstance().getRecipesForCulture(culture);
    }

    @Override
    protected void init() {
        super.init();
        
        int guiLeft = (width - BACKGROUND_WIDTH) / 2;
        int guiTop = (height - BACKGROUND_HEIGHT) / 2;
        
        // Add scrolling buttons if needed
        if (recipes.size() > MAX_VISIBLE_RECIPES) {
            // Up scroll button - fix TexturedButtonWidget usage for 1.21.4
            ButtonWidget scrollUpButton = ButtonWidget.builder(
                Text.literal("↑"), 
                button -> scrollRecipes(-1))
                .dimensions(guiLeft + 154, guiTop + 16, 12, 12)
                .tooltip(Tooltip.of(Text.literal("Scroll Up")))
                .build();
            addDrawableChild(scrollUpButton);
            
            // Down scroll button
            ButtonWidget scrollDownButton = ButtonWidget.builder(
                Text.literal("↓"), 
                button -> scrollRecipes(1))
                .dimensions(guiLeft + 154, guiTop + 134, 12, 12)
                .tooltip(Tooltip.of(Text.literal("Scroll Down")))
                .build();
            addDrawableChild(scrollDownButton);
        }
        
        // Add recipe buttons (will be positioned in render method)
        updateRecipeButtons();
        
        // Add craft button
        craftButton = ButtonWidget.builder(
            Text.literal("Craft Selected Item"), 
            this::onCraftButtonClick
        )
        .dimensions(guiLeft + BACKGROUND_WIDTH/2 - 60, guiTop + BACKGROUND_HEIGHT - 30, 120, 20)
        .build();
        
        addDrawableChild(craftButton);
        updateCraftButton();
        
        // Add cancel button if crafting is in progress
        if ("crafting".equals(craftingStatus)) {
            addCancelButton();
        }
        
        // Add close button
        ButtonWidget closeButton = ButtonWidget.builder(
            Text.literal("Close"), 
            button -> close()
        )
        .dimensions(guiLeft + BACKGROUND_WIDTH - 60, guiTop + BACKGROUND_HEIGHT - 30, 50, 20)
        .build();
        
        addDrawableChild(closeButton);
        
        // Request available recipes from server
        if (villager != null) {
            VillageCraftingClientHandler.requestRecipes(villager.getUuid());
        }
    }
    
    private void updateRecipeButtons() {
        clearRecipeButtons();
        
        int guiLeft = (width - BACKGROUND_WIDTH) / 2;
        int guiTop = (height - BACKGROUND_HEIGHT) / 2;
        
        int visibleRecipes = Math.min(recipes.size(), MAX_VISIBLE_RECIPES);
        int maxScroll = Math.max(0, recipes.size() - MAX_VISIBLE_RECIPES);
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
        
        for (int i = 0; i < visibleRecipes; i++) {
            int recipeIndex = i + scrollOffset;
            if (recipeIndex >= recipes.size()) break;
            
            final int index = recipeIndex;
            VillageCraftingManager.CraftingRecipe recipe = recipes.get(recipeIndex);
            String recipeName = formatRecipeName(recipe.getId());
            
            ButtonWidget button = ButtonWidget.builder(
                Text.literal(recipeName), 
                btn -> selectRecipe(index)
            )
            .dimensions(guiLeft + 20, guiTop + 20 + (i * RECIPE_BUTTON_HEIGHT), 140, 20)
            .tooltip(Tooltip.of(Text.literal("Click to select this recipe")))
            .build();
            
            if (recipeIndex == selectedRecipe) {
                button.setMessage(Text.literal("➤ " + recipeName).setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
            }
            
            addDrawableChild(button);
        }
    }
    
    private void clearRecipeButtons() {
        // Remove existing recipe buttons
        this.children().removeIf(child -> {
            if (child instanceof ButtonWidget button) {
                int guiLeft = (width - BACKGROUND_WIDTH) / 2;
                int guiTop = (height - BACKGROUND_HEIGHT) / 2;
                
                int x = button.getX();
                int y = button.getY();
                
                // Check if button is in the recipe list area
                return x >= guiLeft + 20 && x <= guiLeft + 160 &&
                       y >= guiTop + 20 && y <= guiTop + 140;
            }
            return false;
        });
    }
    
    private void scrollRecipes(int direction) {
        int maxScroll = Math.max(0, recipes.size() - MAX_VISIBLE_RECIPES);
        scrollOffset = MathHelper.clamp(scrollOffset + direction, 0, maxScroll);
        updateRecipeButtons();
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (recipes.size() > MAX_VISIBLE_RECIPES) {
            scrollRecipes(verticalAmount > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        animationTime += delta;
        
        int guiLeft = (width - BACKGROUND_WIDTH) / 2;
        int guiTop = (height - BACKGROUND_HEIGHT) / 2;
        
        // Draw the background texture with updated API for 1.21.4
        context.drawGuiTexture(RenderLayer.getGuiTexture(), BACKGROUND_TEXTURE, guiLeft, guiTop, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
        
        // Draw culture-specific decorative elements
        drawCulturalTheme(context, guiLeft, guiTop);
        
        // Draw title with cultural styling
        String title = culture + " Cultural Crafting";
        int titleColor = getTitleColor();
        context.drawText(
            textRenderer,
            title,
            guiLeft + BACKGROUND_WIDTH/2 - textRenderer.getWidth(title)/2,
            guiTop + 6,
            titleColor,
            true
        );
        
        // Draw villager name
        if (villager != null) {
            String villagerName = "Artisan: " + villager.getName().getString();
            context.drawText(
                textRenderer,
                villagerName,
                guiLeft + BACKGROUND_WIDTH/2 - textRenderer.getWidth(villagerName)/2,
                guiTop + BACKGROUND_HEIGHT - 45,
                0xDDDDDD,
                false
            );
        }
        
        // Draw recipe list title
        context.drawText(
            textRenderer,
            "Available Recipes",
            guiLeft + 20,
            guiTop + 8,
            0xFFD700,
            false
        );
        
        // Draw recipe details if one is selected
        if (selectedRecipe >= 0 && selectedRecipe < recipes.size()) {
            VillageCraftingManager.CraftingRecipe recipe = recipes.get(selectedRecipe);
            
            // Draw recipe name
            String recipeName = formatRecipeName(recipe.getId());
            context.drawText(
                textRenderer,
                recipeName,
                guiLeft + 190 - textRenderer.getWidth(recipeName)/2,
                guiTop + 24,
                0xFFFFFF,
                false
            );
            
            // Draw result item
            if (recipe.getOutput() != null) {
                ItemStack output = new ItemStack(recipe.getOutput());
                drawItem(context, output, guiLeft + 190, guiTop + 45);
                
                // Draw item count if more than 1
                if (output.getCount() > 1) {
                    context.drawText(
                        textRenderer,
                        String.valueOf(output.getCount()),
                        guiLeft + 197,
                        guiTop + 51,
                        0xFFFFFF,
                        true
                    );
                }
            }
            
            // Draw divider
            context.fill(guiLeft + 175, guiTop + 65, guiLeft + 235, guiTop + 66, 0x66FFFFFF);
            
            // Draw required materials
            context.drawText(
                textRenderer,
                "Required Materials:",
                guiLeft + 175,
                guiTop + 70,
                0xAAAAAA,
                false
            );
            
            int y = guiTop + 85;
            int materialsDrawn = 0;
            
            for (var entry : recipe.getInputs().entrySet()) {
                if (materialsDrawn >= 5) break; // Prevent overcrowding
                
                ItemStack itemStack = new ItemStack(entry.getKey(), entry.getValue());
                drawItem(context, itemStack, guiLeft + 175, y);
                
                String materialText = entry.getValue() + "x " + entry.getKey().getName().getString();
                context.drawText(textRenderer, materialText, guiLeft + 195, y + 4, 0xFFFFFF, false);
                
                y += 20;
                materialsDrawn++;
            }
            
            // Draw crafting progress if applicable
            if ("crafting".equals(craftingStatus) && recipe.getId().equals(activeRecipeId)) {
                drawCraftingProgress(context, guiLeft, guiTop);
            }
        } else {
            // No recipe selected, show instruction
            String noSelectionText = "← Select a recipe";
            context.drawText(
                textRenderer,
                noSelectionText,
                guiLeft + 190 - textRenderer.getWidth(noSelectionText)/2,
                guiTop + 80,
                0xAAAAAA,
                false
            );
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void drawItem(DrawContext context, ItemStack stack, int x, int y) {
        context.drawItem(stack, x, y);
        // Fixed tooltip rendering
        context.drawItemTooltip(textRenderer, stack, x, y);
    }
    
    private void drawCulturalTheme(DrawContext context, int guiLeft, int guiTop) {
        // Draw cultural decorations based on the culture type
        // This is a placeholder - you might want to add specific textures for each culture
        int borderColor = getCultureBorderColor();
        
        // Draw a decorative border
        context.fill(
            guiLeft + 2, 
            guiTop + 2, 
            guiLeft + BACKGROUND_WIDTH - 2, 
            guiTop + 4, 
            borderColor
        );
        
        context.fill(
            guiLeft + 2, 
            guiTop + BACKGROUND_HEIGHT - 4, 
            guiLeft + BACKGROUND_WIDTH - 2, 
            guiTop + BACKGROUND_HEIGHT - 2, 
            borderColor
        );
        
        context.fill(
            guiLeft + 2, 
            guiTop + 2, 
            guiLeft + 4, 
            guiTop + BACKGROUND_HEIGHT - 2, 
            borderColor
        );
        
        context.fill(
            guiLeft + BACKGROUND_WIDTH - 4, 
            guiTop + 2, 
            guiLeft + BACKGROUND_WIDTH - 2, 
            guiTop + BACKGROUND_HEIGHT - 2, 
            borderColor
        );
        
        // Draw a separator between recipe list and details
        context.fill(
            guiLeft + 170, 
            guiTop + 20, 
            guiLeft + 172, 
            guiTop + BACKGROUND_HEIGHT - 50, 
            borderColor
        );
    }
    
    private int getCultureBorderColor() {
        // Return different colors based on culture
        switch (culture.toLowerCase()) {
            case "nordic":
                return 0xFF3366CC; // Blue
            case "desert":
                return 0xFFCC9933; // Sand/Gold
            case "tropical":
                return 0xFF33CC66; // Green
            case "eastern":
                return 0xFFCC3366; // Red
            default:
                return 0xFFAA8866; // Default brown
        }
    }
    
    private int getTitleColor() {
        // Return different colors based on culture
        switch (culture.toLowerCase()) {
            case "nordic":
                return 0xFF99CCFF;
            case "desert":
                return 0xFFFFDD99;
            case "tropical":
                return 0xFF99FFAA;
            case "eastern":
                return 0xFFFF99AA;
            default:
                return 0xFFDDCCAA;
        }
    }
    
    private void drawCraftingProgress(DrawContext context, int guiLeft, int guiTop) {
        // Draw progress background
        int progressBarX = guiLeft + 175;
        int progressBarY = guiTop + 140;
        int progressBarWidth = 60;
        int progressBarHeight = 6;
        
        // Background
        context.fill(progressBarX, progressBarY, progressBarX + progressBarWidth, progressBarY + progressBarHeight, 0xFF555555);
        
        // Progress
        int progressWidth = (int)((craftingProgress / (float)craftingMaxProgress) * progressBarWidth);
        context.fill(progressBarX, progressBarY, progressBarX + progressWidth, progressBarY + progressBarHeight, 0xFF66CC66);
        
        // Progress text
        String progressText = "Crafting: " + craftingProgress + "%";
        context.drawText(
            textRenderer,
            progressText,
            progressBarX + progressBarWidth/2 - textRenderer.getWidth(progressText)/2,
            progressBarY - 12,
            0xFFFFFF,
            false
        );
    }

    private void selectRecipe(int index) {
        if (this.selectedRecipe != index) {
            this.selectedRecipe = index;
            updateRecipeButtons();
            updateCraftButton();
        }
    }
    
    private void onCraftButtonClick(ButtonWidget button) {
        if (selectedRecipe >= 0 && selectedRecipe < recipes.size() && client.player != null) {
            VillageCraftingManager.CraftingRecipe recipe = recipes.get(selectedRecipe);
            
            if ("crafting".equals(craftingStatus)) {
                // Already crafting, do nothing
                return;
            }
            
            // Send crafting request to server via the network handler
            VillageCraftingClientHandler.requestCrafting(villager.getUuid(), recipe.getId());
            
            // Update status
            craftingStatus = "crafting";
            craftingMessage = "Starting crafting process...";
            craftingProgress = 0;
            activeRecipeId = recipe.getId();
            
            // Update button state
            updateCraftButton();
            
            // Add cancel button
            addCancelButton();
        }
    }
    
    private void addCancelButton() {
        int guiLeft = (width - BACKGROUND_WIDTH) / 2;
        int guiTop = (height - BACKGROUND_HEIGHT) / 2;
        
        ButtonWidget cancelButton = ButtonWidget.builder(
            Text.literal("Cancel"), 
            button -> {
                if (activeRecipeId != null) {
                    VillageCraftingClientHandler.cancelCrafting(villager.getUuid(), activeRecipeId);
                    craftingStatus = "none";
                    updateCraftButton();
                    button.visible = false;
                }
            }
        )
        .dimensions(guiLeft + 10, guiTop + BACKGROUND_HEIGHT - 30, 50, 20)
        .build();
        
        addDrawableChild(cancelButton);
    }
    
    public void updateRecipeList(List<String> recipeIds) {
        // Convert string recipe IDs to CraftingRecipe objects
        List<VillageCraftingManager.CraftingRecipe> newRecipes = new ArrayList<>();
        for (String id : recipeIds) {
            VillageCraftingManager.CraftingRecipe recipe = VillageCraftingManager.getInstance().getRecipeById(id);
            if (recipe != null) {
                newRecipes.add(recipe);
            }
        }
        
        // Update the recipes list (this assumes recipes is mutable)
        recipes.clear();
        recipes.addAll(newRecipes);
        
        // Reset selection and update UI
        selectedRecipe = -1;
        updateRecipeButtons();
        updateCraftButton();
    }
    
    public void updateCraftingStatus(String recipeId, String status, String message) {
        if (recipeId.equals(activeRecipeId)) {
            this.craftingStatus = status;
            this.craftingMessage = message;
            
            updateCraftButton();
        }
    }
    
    public void updateCraftingProgress(String recipeId, int progress, int maxProgress) {
        if (recipeId.equals(activeRecipeId)) {
            this.craftingProgress = progress;
            this.craftingMaxProgress = maxProgress;
        }
    }
    
    public void handleCraftingComplete(String recipeId, boolean success) {
        if (recipeId.equals(activeRecipeId)) {
            // Reset crafting state
            craftingStatus = "none";
            activeRecipeId = null;
            updateCraftButton();
            
            // Show completion message
            if (success) {
                displayFeedback("Crafting completed successfully!");
            } else {
                displayFeedback("Crafting failed. Please try again.");
            }
        }
    }
    
    private void displayFeedback(String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), false);
        }
    }
    
    private void updateCraftButton() {
        if (craftButton == null) return;
        
        if ("crafting".equals(craftingStatus)) {
            craftButton.setMessage(Text.literal("Crafting..."));
            craftButton.active = false;
        } else if (selectedRecipe >= 0 && selectedRecipe < recipes.size()) {
            craftButton.setMessage(Text.literal("Craft Selected Item"));
            craftButton.active = true;
        } else {
            craftButton.setMessage(Text.literal("Select a Recipe First"));
            craftButton.active = false;
        }
    }

    private String formatRecipeName(String id) {
        return Arrays.asList(id.replace('_', ' ').split(" "))
                .stream()
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .reduce("", (a, b) -> a + " " + b)
                .trim();
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
