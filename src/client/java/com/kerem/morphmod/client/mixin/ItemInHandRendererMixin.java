package com.kerem.morphmod.client.mixin;

import com.kerem.morphmod.client.ClientMorphData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to customize first-person arm rendering when morphed.
 * Renders the mob's own arm if humanoid, or hides the human arm for non-humanoid mobs.
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Inject(method = "renderPlayerArm", at = @At("HEAD"), cancellable = true)
    private void morphmod$renderMobArm(
            PoseStack poseStack, MultiBufferSource bufferSource, int light,
            float equippedProgress, float swingProgress, HumanoidArm humanoidArm,
            CallbackInfo ci
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        ResourceLocation morphId = ClientMorphData.getActiveMorph(client.player.getUUID());
        if (morphId == null) return;

        Entity fakeEntity = ClientMorphData.getOrCreateFakeEntity(morphId);
        if (fakeEntity == null) {
            ci.cancel();
            return;
        }

        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        EntityRenderer<?> rawRenderer = dispatcher.getRenderer(fakeEntity);

        if (rawRenderer instanceof LivingEntityRenderer<?, ?> livingRenderer
                && livingRenderer.getModel() instanceof HumanoidModel<?> humanoidModel) {
            @SuppressWarnings("unchecked")
            EntityRenderer<Entity> castRenderer = (EntityRenderer<Entity>) rawRenderer;
            ResourceLocation texture = castRenderer.getTextureLocation(fakeEntity);

            poseStack.pushPose();

            boolean isRight = humanoidArm != HumanoidArm.LEFT;
            float f = isRight ? 1.0F : -1.0F;
            float g = Mth.sqrt(swingProgress);
            float h = -0.3F * Mth.sin(g * (float) Math.PI);
            float i = 0.4F * Mth.sin(g * (float) (Math.PI * 2));
            float j = -0.4F * Mth.sin(swingProgress * (float) Math.PI);
            poseStack.translate(f * (h + 0.64000005F), i + -0.6F + equippedProgress * -0.6F, j + -0.71999997F);
            poseStack.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
            float k = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
            float l = Mth.sin(g * (float) Math.PI);
            poseStack.mulPose(Axis.YP.rotationDegrees(f * l * 70.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(f * k * -20.0F));

            poseStack.translate(f * -1.0F, 3.6F, 3.5F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
            poseStack.translate(f * 5.6F, 0.0F, 0.0F);

            humanoidModel.attackTime = 0.0F;
            humanoidModel.riding = false;
            humanoidModel.young = false;

            ModelPart armPart = isRight ? humanoidModel.rightArm : humanoidModel.leftArm;
            armPart.resetPose();
            armPart.xRot = 0.0F;

            VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
            armPart.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY);

            poseStack.popPose();
            ci.cancel();
            return;
        }

        // For non-humanoid mobs (creeper, spider, wolf, dragon, chicken etc.):
        // Suppress Steve's human arm so naked human skin doesn't appear
        ci.cancel();
    }
}
