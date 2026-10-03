package com.kerem.morphmod.client.mixin;

import net.minecraft.client.renderer.entity.ShulkerRenderer;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into ShulkerRenderer to prevent jittering and sliding when moving.
 * Vanilla ShulkerRenderer interpolates positions toward block grid coordinates,
 * which causes severe shaking when moving as a morphed player.
 */
@Mixin(ShulkerRenderer.class)
public abstract class ShulkerRendererMixin {

    @Inject(method = "getRenderOffset", at = @At("HEAD"), cancellable = true)
    private void morphmod$fixShulkerJitter(Shulker shulker, float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        cir.setReturnValue(Vec3.ZERO);
    }
}
