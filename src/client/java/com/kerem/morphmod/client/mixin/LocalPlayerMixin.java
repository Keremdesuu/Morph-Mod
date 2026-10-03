package com.kerem.morphmod.client.mixin;

import com.kerem.morphmod.client.ClientMorphData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into LocalPlayer to ensure Vex players pass through solid blocks
 * unobstructed by setting noPhysics = true right before movement and aiStep.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void morphmod$vexNoPhysicsAiStep(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (ClientMorphData.isSilverfishHidden()) {
            self.noPhysics = true;
            return;
        }
        ResourceLocation morph = ClientMorphData.getActiveMorph(self.getUUID());
        if (morph != null && morph.getPath().equals("vex")) {
            self.noPhysics = true;
            self.resetFallDistance();
        }
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void morphmod$vexNoPhysicsMove(MoverType type, Vec3 movement, CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (ClientMorphData.isSilverfishHidden()) {
            self.noPhysics = true;
            return;
        }
        ResourceLocation morph = ClientMorphData.getActiveMorph(self.getUUID());
        if (morph != null && morph.getPath().equals("vex")) {
            self.noPhysics = true;
            self.resetFallDistance();
        }
    }
}
