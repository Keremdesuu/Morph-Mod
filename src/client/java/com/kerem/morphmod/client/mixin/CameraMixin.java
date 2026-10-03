package com.kerem.morphmod.client.mixin;

import com.kerem.morphmod.client.ClientMorphData;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into Camera to dynamically adjust third-person camera distance
 * according to the active morph (e.g. Ender Dragon pulls back to 15 blocks, Silverfish zooms in to 2.6 blocks).
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Unique
    private float morphmod$lastPartialTick = 1.0F;

    @Inject(method = "setup", at = @At("HEAD"))
    private void morphmod$capturePartialTick(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        this.morphmod$lastPartialTick = partialTick;
    }

    @ModifyArg(
            method = "setup",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"),
            index = 0
    )
    private float morphmod$applyDynamicMorphZoom(float defaultZoom) {
        if (ClientMorphData.getActiveMorph() != null || ClientMorphData.getMorphTransitionTicks() > 0) {
            return ClientMorphData.getSmoothCameraDistance(this.morphmod$lastPartialTick);
        }
        return defaultZoom;
    }
}
