package com.beeny.gui;

import com.beeny.village.VillageCraftingManager;
import com.beeny.network.VillageCraftingClientNetwork;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import java.util.List;
import java.util.Arrays;

public class VillageCraftingScreen extends Screen {
    private final VillagerEntity villager;
    private final String culture;
    private final List<VillageCraftingManager.CraftingRecipe> recipes;
    private int selectedRecipe = -1;

    public VillageCraftingScreen(VillagerEntity villager, String culture) {
        super(Text.literal("Village Crafting"));
        this.villager = villager;
        this.culture = culture;
        this.recipes = VillageCraftingManager.getInstance().getRecipesForCulture(culture);
    }

    @Override
    protected void init() {
        super.init();
        
        // Add recipe buttons
        for (int i = 0; i < recipes.size(); i++) {
            final int index = i;
            VillageCraftingManager.CraftingRecipe recipe = recipes.get(i);
            
            ButtonWidget button = ButtonWidget.builder(
                Text.literal(formatRecipeName(recipe.getId())), 
                (btn) -> selectRecipe(index)
            )
            .dimensions(width / 4, 40 + (i * 25), 150, 20)
            .build();
            
            addDrawableChild(button);
        }

        // Add craft button
        ButtonWidget craftButton = ButtonWidget.builder(
            Text.literal("Craft Selected Item"), 
            (btn) -> craftSelectedRecipe()
        )
        .dimensions(width / 2 - 60, height - 40, 120, 20)
        .build();
        
        addDrawableChild(craftButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        
        // Draw title
        context.drawTextWithShadow(
            textRenderer,
            culture + " Crafting",
            width / 2 - textRenderer.getWidth(culture + " Crafting") / 2,
            10,
            0xFFFFFF
        );

        // Draw recipe details if one is selected
        if (selectedRecipe >= 0 && selectedRecipe < recipes.size()) {
            VillageCraftingManager.CraftingRecipe recipe = recipes.get(selectedRecipe);
            
            // Draw recipe name
            String recipeName = formatRecipeName(recipe.getId());
            context.drawTextWithShadow(
                textRenderer,
                "Selected: " + recipeName,
                width / 2 + 80,
                60,
                0xFFFFFF
            );

            // Draw required materials
            int y = 80;
            context.drawTextWithShadow(textRenderer, "Required Materials:", width / 2 + 80, y, 0xAAAAAA);
            y += 20;
            
            for (var entry : recipe.getInputs().entrySet()) {
                String materialText = entry.getValue() + "x " + entry.getKey().getName().getString();
                context.drawTextWithShadow(textRenderer, materialText, width / 2 + 80, y, 0xFFFFFF);
                y += 15;
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void selectRecipe(int index) {
        this.selectedRecipe = index;
    }

    private void craftSelectedRecipe() {
        if (selectedRecipe >= 0 && selectedRecipe < recipes.size() && client.player != null) {
            VillageCraftingManager.CraftingRecipe recipe = recipes.get(selectedRecipe);
            
            // Send crafting request to server via the network handler
            VillageCraftingClientNetwork.sendCraftingRequest(villager, recipe.getId());
            
            // Close the screen after sending the request
            close();
        }
    }

    private String formatRecipeName(String id) {
        return Arrays.asList(id.replace('_', ' ').split(" "))
                .stream()
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .reduce("", (a, b) -> a + " " + b)
                .trim();
    }
}