package com.kerem.morphmod.client.mixin;

import com.kerem.morphmod.client.ClientMorphData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Disables the in-wall suffocation screen overlay when the player is hiding
 * inside a block as a Silverfish, so they can see outside clearly.
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @Inject(method = "renderScreenEffect", at = @At("HEAD"), cancellable = true)
    private static void morphmod$cancelWallSuffocationOverlay(Minecraft minecraft, PoseStack poseStack, CallbackInfo ci) {
        if (minecraft.player != null) {
            net.minecraft.resources.ResourceLocation morphId = ClientMorphData.getActiveMorph(minecraft.player.getUUID());
            if (morphId != null && morphId.getPath().equals("vex")) {
                ci.cancel();
            }
        }
    }
}
