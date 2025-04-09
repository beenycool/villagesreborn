package com.beeny.mixin.client;

import com.beeny.config.VillagesConfig; // Added import
import com.beeny.village.CulturalTextureManager;
import com.beeny.village.VillagerManager;
import com.beeny.village.SpawnRegion;
import net.minecraft.client.render.entity.VillagerEntityRenderer;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.ModifyArgs; // Added import
import org.spongepowered.asm.mixin.injection.invoke.arg.Args; // Added import
import net.minecraft.client.util.math.MatrixStack; // Added import
import net.minecraft.client.render.VertexConsumerProvider; // Added import
import net.minecraft.text.Text; // Added import
import net.minecraft.village.VillagerProfession; // Added for profession checks/markers
import net.minecraft.text.MutableText; // Added for compact info text building
import net.minecraft.util.Formatting; // Added for text formatting
import net.minecraft.text.Style; // Added for text styling
import net.minecraft.client.render.RenderLayer; // Added for rendering quads
import org.joml.Matrix4f; // Added for matrix transformations

@Mixin(VillagerEntityRenderer.class)
public abstract class VillagerRendererMixin extends net.minecraft.client.render.entity.LivingEntityRenderer<VillagerEntity, net.minecraft.client.render.entity.model.VillagerResemblingModel<VillagerEntity>> {
    // Extend LivingEntityRenderer to access protected methods like hasLabel

    // Define placeholder texture IDs for markers
    private static final Identifier DEFAULT_MARKER_TEXTURE = new Identifier("villagesreborn", "textures/gui/markers/default_marker.png");
    private static final Identifier FARMER_MARKER_TEXTURE = new Identifier("villagesreborn", "textures/gui/markers/farmer_marker.png"); // Example
    private static final Identifier LIBRARIAN_MARKER_TEXTURE = new Identifier("villagesreborn", "textures/gui/markers/librarian_marker.png"); // Example
    // Add more profession-specific markers as needed...
    // Dummy constructor required by Mixin when extending
    protected VillagerRendererMixin(net.minecraft.client.render.entity.EntityRendererFactory.Context ctx, net.minecraft.client.render.entity.model.VillagerResemblingModel<VillagerEntity> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @ModifyVariable(
        method = "renderLabelIfPresent(Lnet/minecraft/entity/passive/VillagerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At(value = "HEAD"), // Modify the argument right at the start
        argsOnly = true // Only target method arguments
    )
    private Text villagesreborn_modifyNameTagTextForCompactInfo(Text originalText, VillagerEntity entity) { // Parameters: original value, then method args
        VillagesConfig.UISettings uiSettings = VillagesConfig.getInstance().getUISettings();
        if (uiSettings.isCompactVillagerInfo()) {
            VillagesConfig.UISettings.ColorScheme scheme = uiSettings.getActiveColorScheme();
            MutableText compactPrefix = Text.literal("");
            boolean prefixAdded = false;

            // Add Profession Indicator (if enabled in config)
            if (uiSettings.isShowProfession()) {
                 net.minecraft.village.VillagerProfession profession = entity.getVillagerData().getProfession();
                 // Check for valid, non-default professions
                 if (profession != null && profession != net.minecraft.village.VillagerProfession.NONE && profession != net.minecraft.village.VillagerProfession.NITWIT) {
                     // Use first letter as indicator
                     String professionKey = profession.toString(); // Use toString() which is simpler
                     // Basic parsing, might need adjustment if registry names change format
                     String professionName = professionKey.contains(":") ? professionKey.substring(professionKey.indexOf(':') + 1) : professionKey;
                     if (!professionName.isEmpty()) {
                         String professionInitial = professionName.substring(0, 1).toUpperCase();
                         compactPrefix.append(net.minecraft.text.Text.literal("[" + professionInitial + "]").fillStyle(net.minecraft.text.Style.EMPTY.withColor(scheme.compactProfession))); // Use color from scheme
                         prefixAdded = true;
                     }
                 }
             }

            // Add Culture Indicator (if enabled in config)
            // Reusing logic similar to getCustomTexture
            if (uiSettings.isShowCulture()) {
                 com.beeny.village.VillagerManager manager = com.beeny.village.VillagerManager.getInstance();
                 com.beeny.village.SpawnRegion region = manager.getNearestSpawnRegion(entity.getBlockPos());
                 if (region != null) {
                     String culture = region.getCultureAsString();
                     if (culture != null && !culture.isEmpty() && !culture.equalsIgnoreCase("UNKNOWN")) { // Check for valid culture
                         // Use first letter as indicator
                         String cultureInitial = culture.substring(0, 1).toUpperCase();
                         if (prefixAdded) compactPrefix.append(" "); // Add space if profession was already added
                         compactPrefix.append(net.minecraft.text.Text.literal("[" + cultureInitial + "]").fillStyle(net.minecraft.text.Style.EMPTY.withColor(scheme.compactCulture))); // Use color from scheme
                         prefixAdded = true;
                     }
                 }
             }

            if (prefixAdded) {
                compactPrefix.append(" "); // Space between prefix and original name
                return compactPrefix.append(originalText); // Prepend the compact info
            }
        }
        // If compact info is off or no info was added, return the original text
        return originalText;
    }


    /**
     * Modifies the arguments passed to the text renderer within renderLabelIfPresent
     * to change the text color based on the config setting.
     */
    @ModifyArgs(
        method = "renderLabelIfPresent(Lnet/minecraft/entity/passive/VillagerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At(
            value = "INVOKE",
            // Target the call to drawWithShadow or draw methods within TextRenderer
            target = "Lnet/minecraft/client/font/TextRenderer;drawWithShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/text/Text;FFI)I",
            remap = false // Might be needed if targeting intermediary method name
        )
    )
    private void villagesreborn_modifyNameTagColor(Args args, VillagerEntity entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // Argument indices for drawWithShadow(MatrixStack, Text, float, float, int):
        // 0: MatrixStack matrices
        // 1: Text text
        // 2: float x
        // 3: float y
        // 4: int color

        // Get the active color scheme
        VillagesConfig.UISettings.ColorScheme scheme = VillagesConfig.getInstance().getUISettings().getActiveColorScheme();

        // Modify the color argument (index 4) with the name tag color from the scheme
        args.set(4, scheme.nameTag);
    }
    
    // Corrected placement of getCustomTexture - should be outside the ModifyArgs method
    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true)
    private void villagesreborn_getCustomTexture(VillagerEntity villager, CallbackInfoReturnable<Identifier> cir) {
        VillagerManager manager = VillagerManager.getInstance();
        SpawnRegion region = manager.getNearestSpawnRegion(villager.getBlockPos());

        if (region != null) {
            String culture = region.getCultureAsString();
            // Ensure CulturalTextureManager is imported or use full path
            Identifier customTexture = com.beeny.village.CulturalTextureManager.getTextureForCulture(culture);
            if (customTexture != null) { // Check if a texture was found
                 cir.setReturnValue(customTexture);
            }
            // If no custom texture, let the original method run
        }
    }
    // Removed extra closing brace here

    /**
     * Injects into hasLabel to prevent rendering if the config setting is false.
     */
    @Inject(method = "hasLabel(Lnet/minecraft/entity/passive/VillagerEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void villagesreborn_controlNameTagVisibility(VillagerEntity villagerEntity, CallbackInfoReturnable<Boolean> cir) {
        // Check the config setting
        if (!VillagesConfig.getInstance().getUISettings().isShowVillagerNameTags()) {
            // If the setting is false, cancel the original method and return false
            cir.setReturnValue(false);
        }
        // If the setting is true, do nothing and let the original hasLabel logic continue
    }

    @Inject(method = "render(Lnet/minecraft/entity/passive/VillagerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
    private void villagesreborn_renderCustomUI(VillagerEntity entity, float entityYaw, float partialTicks, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        // Basic checks
        if (!entity.isAlive() || entity.isInvisible() || this.dispatcher.getSquaredDistanceToCamera(entity) > 64 * 64) {
             return;
        }

        // --- Health Bar Rendering ---
        VillagesConfig.UISettings uiSettings = VillagesConfig.getInstance().getUISettings();
        VillagesConfig.UISettings.ColorScheme scheme = uiSettings.getActiveColorScheme();

        // --- Health Bar Rendering ---
        if (uiSettings.isShowVillagerHealthBars()) {
            matrices.push(); // Isolate transformations for health bar

            // Configuration
            float barHeight = 0.1f;
            float barWidth = 1.0f; // Total width of the bar
            float yOffset = 0.5f; // Offset above the entity's height

            // Health calculation
            float health = entity.getHealth();
            float maxHealth = entity.getMaxHealth();
            float healthPercent = health / maxHealth;

            // Position calculation
            float renderY = entity.getHeight() + yOffset;

            // Matrix setup for billboarding and positioning
            matrices.translate(0.0, renderY, 0.0);
            matrices.multiply(this.dispatcher.camera.getRotation()); // Billboard
            matrices.scale(-0.025f, -0.025f, 0.025f); // Scale the bar down

            Matrix4f matrix = matrices.peek().getPositionMatrix();

            // Get vertex consumer for drawing colored quads
            VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getGuiOverlay()); // Simple overlay layer

            // --- Draw Background Bar ---
            // Use color from scheme for background
            int bgColor = scheme.healthBarBackground;
            float bgRed = ((bgColor >> 16) & 0xFF) / 255.0f;
            float bgGreen = ((bgColor >> 8) & 0xFF) / 255.0f;
            float bgBlue = (bgColor & 0xFF) / 255.0f;
            float bgAlpha = ((bgColor >> 24) & 0xFF) / 255.0f;
            float bgMinX = -barWidth / 2;
            float bgMaxX = barWidth / 2;
            float bgMinY = 0;
            float bgMaxY = barHeight;

            buffer.vertex(matrix, bgMinX, bgMinY, 0).color(bgRed, bgGreen, bgBlue, bgAlpha).next();
            buffer.vertex(matrix, bgMinX, bgMaxY, 0).color(bgRed, bgGreen, bgBlue, bgAlpha).next();
            buffer.vertex(matrix, bgMaxX, bgMaxY, 0).color(bgRed, bgGreen, bgBlue, bgAlpha).next();
            buffer.vertex(matrix, bgMaxX, bgMinY, 0).color(bgRed, bgGreen, bgBlue, bgAlpha).next();

            // --- Draw Health Fill Bar ---
            float fillWidth = barWidth * healthPercent;
            float fillMinX = -barWidth / 2;
            float fillMaxX = fillMinX + fillWidth; // Adjust max X based on health
            float fillMinY = 0;
            float fillMaxY = barHeight;

            // Determine color based on health using scheme colors
            int fillColor;
            if (healthPercent > 0.6f) {
                fillColor = scheme.healthBarHigh;
            } else if (healthPercent > 0.3f) {
                fillColor = scheme.healthBarMedium;
            } else {
                fillColor = scheme.healthBarLow;
            }
            float fillRed = ((fillColor >> 16) & 0xFF) / 255.0f;
            float fillGreen = ((fillColor >> 8) & 0xFF) / 255.0f;
            float fillBlue = (fillColor & 0xFF) / 255.0f;
            float fillAlpha = ((fillColor >> 24) & 0xFF) / 255.0f; // Use alpha from scheme color
            }

            buffer.vertex(matrix, fillMinX, fillMinY, 0).color(fillRed, fillGreen, fillBlue, fillAlpha).next();
            buffer.vertex(matrix, fillMinX, fillMaxY, 0).color(fillRed, fillGreen, fillBlue, fillAlpha).next();
            buffer.vertex(matrix, fillMaxX, fillMaxY, 0).color(fillRed, fillGreen, fillBlue, fillAlpha).next();
            buffer.vertex(matrix, fillMaxX, fillMinY, 0).color(fillRed, fillGreen, fillBlue, fillAlpha).next();


            matrices.pop(); // Restore matrix stack for health bar
        }


        // --- Marker Rendering ---
        // Marker rendering logic based on profession
        // --- Marker Rendering --- (uiSettings and scheme already fetched above)
        if (uiSettings.isShowVillageMarkers()) {
            // Check distance against the configured marker range
            double squaredDistance = this.dispatcher.getSquaredDistanceToCamera(entity);
            double maxSquaredDistance = uiSettings.getVillageMarkerRange() * uiSettings.getVillageMarkerRange();
            if (squaredDistance > maxSquaredDistance) {
                 // Too far away, don't render marker
            } else {
                 matrices.push();

            // Configuration
            float markerSize = 0.5f;
            float markerYOffset = 0.7f; // Base offset

            // Adjust Y offset if health bars are also shown (use cached uiSettings)
            if (uiSettings.isShowVillagerHealthBars()) {
                 markerYOffset += 0.15f; // Add space above the health bar (adjust value as needed)
            }

            // Position calculation
            float renderY = entity.getHeight() + markerYOffset;

            // Matrix setup
            matrices.translate(0.0, renderY, 0.0);
            matrices.multiply(this.dispatcher.camera.getRotation()); // Billboard
            matrices.scale(-0.025f, -0.025f, 0.025f);

            Matrix4f matrix = matrices.peek().getPositionMatrix();

            // --- Draw Textured Marker ---
            // Determine marker texture based on villager state (e.g., profession)
            Identifier currentMarkerTexture = getMarkerTextureForVillager(entity);

            // Get vertex consumer for textured quad
            VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(currentMarkerTexture)); // Use appropriate RenderLayer

            float markerMinX = -markerSize / 2;
            float markerMaxX = markerSize / 2;
            float markerMinY = 0;
            float markerMaxY = markerSize;
            float minU = 0f, maxU = 1f; // Texture coordinates (U)
            float minV = 0f, maxV = 1f; // Texture coordinates (V)

            // Draw the textured quad (vertices with color, texture coords, light)
            // Use markerTint from scheme
            int markerColor = scheme.markerTint;
            float r = ((markerColor >> 16) & 0xFF) / 255.0f;
            float g = ((markerColor >> 8) & 0xFF) / 255.0f;
            float b = (markerColor & 0xFF) / 255.0f;
            float a = ((markerColor >> 24) & 0xFF) / 255.0f;

            buffer.vertex(matrix, markerMinX, markerMinY, 0).color(r, g, b, a).texture(minU, maxV).light(light).next();
            buffer.vertex(matrix, markerMinX, markerMaxY, 0).color(r, g, b, a).texture(minU, minV).light(light).next();
            buffer.vertex(matrix, markerMaxX, markerMaxY, 0).color(r, g, b, a).texture(maxU, minV).light(light).next();
            buffer.vertex(matrix, markerMaxX, markerMinY, 0).color(r, g, b, a).texture(maxU, maxV).light(light).next();


                 matrices.pop(); // Restore matrix stack for marker
            }
        }

        // Compact info is handled by modifying the name tag text (villagesreborn_modifyNameTagTextForCompactInfo)
    }
    // Helper method to select marker texture
    private Identifier getMarkerTextureForVillager(VillagerEntity entity) {
        net.minecraft.village.VillagerProfession profession = entity.getVillagerData().getProfession();

        // Simple example: Map profession to texture
        // Simple example: Map profession to texture
        if (profession == net.minecraft.village.VillagerProfession.FARMER) {
            return FARMER_MARKER_TEXTURE;
        } else if (profession == net.minecraft.village.VillagerProfession.LIBRARIAN) {
            return LIBRARIAN_MARKER_TEXTURE;
        }
        // Add more mappings here...

        // Default marker if no specific one matches or profession is NONE/NITWIT
        return DEFAULT_MARKER_TEXTURE;
    }
}