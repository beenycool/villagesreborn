package com.villagesreborn.beeny.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import com.villagesreborn.beeny.VillagesRebornPlayerData;
import com.villagesreborn.beeny.systems.PlayerRole;
import net.minecraft.client.gui.widget.DropdownWidget;
import java.util.Arrays;
import com.villagesreborn.beeny.Villagesreborn;
import com.villagesreborn.beeny.systems.Quest;

public class PlayerIntroScreen extends Screen {
    private TextFieldWidget nameField;
    private String selectedGender = "neutral";
    private String selectedBiome = "plains";
    private PlayerRole selectedRole = PlayerRole.MAYOR;
    private final VillagesRebornPlayerData playerData;

    public PlayerIntroScreen(VillagesRebornPlayerData playerData) {
        super(Text.translatable("villagesreborn.gui.character_creation_title"));
        this.playerData = playerData;
    }

    @Override
    protected void init() {
        super.init();

        // Name input field
        nameField = new TextFieldWidget(textRenderer, width / 2 - 100, 50, 200, 20, Text.translatable("villagesreborn.gui.name_field_label"));
        addDrawableChild(nameField);

        // Gender selection buttons
        addDrawableChild(new ButtonWidget(width / 2 - 102, 100, 68, 20, Text.translatable("villagesreborn.gui.gender_male"),
                button -> selectedGender = "male"));

        addDrawableChild(new ButtonWidget(width / 2 - 34, 100, 68, 20, Text.translatable("villagesreborn.gui.gender_neutral"),
                button -> selectedGender = "neutral"));

        addDrawableChild(new ButtonWidget(width / 2 + 34, 100, 68, 20, Text.translatable("villagesreborn.gui.gender_female"),
                button -> selectedGender = "female"));

        // Biome selection dropdown (existing)
        addDrawableChild(new ButtonWidget(width / 2 - 100, 150, 200, 20,
                Text.translatable("villagesreborn.gui.biome_dropdown_label", selectedBiome), button -> {
                    // Biome selection logic
                }));

        // Role selection dropdown
        DropdownWidget<PlayerRole> roleDropdown = DropdownWidget.builder(PlayerRole.class, textRenderer)
                .position(width / 2 - 100, 180)
                .width(200)
                .values(Arrays.asList(PlayerRole.values()))
                .initialValue(selectedRole)
                .label(Text.translatable("villagesreborn.gui.role_dropdown_label", "Role: %s"))
                .onValueChange(role -> selectedRole = role)
                .build();
        addDrawableChild(roleDropdown);

        // Quest buttons, positioned below role selection dropdown
        int questButtonYOffset = 180 + 30; // Start below the role dropdown
        for (Quest quest : Villagesreborn.questManager.getAvailableQuests()) {
            addDrawableChild(new ButtonWidget(width / 2 - 100, questButtonYOffset, 200, 20,
                    Text.translatable(quest.getTitle()), button -> {
                        Villagesreborn.questManager.startQuest(quest.getId());
                        close();
                    }));
            questButtonYOffset += 25;
        }


        // Confirm button
        addDrawableChild(new ButtonWidget(width / 2 - 100, height - 40, 200, 20,
                Text.translatable("villagesreborn.gui.create_character_button"), button -> {
                    playerData.setPlayerName(nameField.getText());
                playerData.setGender(selectedGender);
                playerData.setOriginBiome(selectedBiome);
                playerData.setPlayerRole(selectedRole.toString());
                playerData.setBackstory(generateBackstory(selectedBiome));
                close();
            }));
    }

    private String generateBackstory(String biome) {
        if (!LocalLLMManager.areModelsInstalled()) {
            client.setScreen(new ModelDownloadScreen(this));
            return "";
        }
        return LocalLLMManager.generateBackstory(
                biome,
                selectedGender,
                nameField.getText()
        );
    }
}
