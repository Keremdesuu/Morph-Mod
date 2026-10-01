package com.kerem.morphmod.client.mixin;

import com.kerem.morphmod.client.MorphScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "processBlurEffect", at = @At("HEAD"), cancellable = true)
    private void morphmod$cancelBlurOnMorphScreen(float partialTick, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof MorphScreen) {
            ci.cancel();
        }
    }
}
