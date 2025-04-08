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

@Mixin(VillagerEntityRenderer.class)
public abstract class VillagerRendererMixin extends net.minecraft.client.render.entity.LivingEntityRenderer<VillagerEntity, net.minecraft.client.render.entity.model.VillagerResemblingModel<VillagerEntity>> {
    // Extend LivingEntityRenderer to access protected methods like hasLabel

    // Dummy constructor required by Mixin when extending
    protected VillagerRendererMixin(net.minecraft.client.render.entity.EntityRendererFactory.Context ctx, net.minecraft.client.render.entity.model.VillagerResemblingModel<VillagerEntity> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
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

        // Get the configured color
        int configuredColor = VillagesConfig.getInstance().getUISettings().getNameColor();

        // Modify the color argument (index 4)
        args.set(4, configuredColor);
    }
    
    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true)
    private void getCustomTexture(VillagerEntity villager, CallbackInfoReturnable<Identifier> cir) {
        VillagerManager manager = VillagerManager.getInstance();
        SpawnRegion region = manager.getNearestSpawnRegion(villager.getBlockPos());
        
        if (region != null) {
            String culture = region.getCultureAsString();
            Identifier customTexture = CulturalTextureManager.getTextureForCulture(culture);
            cir.setReturnValue(customTexture);
        }
    }
    }

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
}