package com.kerem.morphmod.client.mixin;

import com.kerem.morphmod.client.ClientMorphData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side mixin to adapt the player's bounding box dimensions to match their active morph.
 * Prevents client/server collision desync and ensures smooth movement without getting stuck.
 */
@Mixin(Player.class)
public abstract class ClientPlayerEntityMixin {

    @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
    private void morphmod$modifyClientDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide) return;

        ResourceLocation morphId = ClientMorphData.getActiveMorph(self.getUUID());
        if (morphId != null) {
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(morphId);
            if (entityType != null) {
                EntityDimensions defaultDims = entityType.getDimensions();
                // Clamp gigantic hitboxes to playable bounds so player doesn't get stuck in walls
                float width = Math.min(defaultDims.width(), 1.2F);
                float height = Math.min(defaultDims.height(), 2.4F);
                width = Math.max(width, 0.4F);
                height = Math.max(height, 0.5F);

                float eyeHeight = Math.min(defaultDims.eyeHeight(), height * 0.85F);
                cir.setReturnValue(EntityDimensions.scalable(width, height).withEyeHeight(eyeHeight));
            }
        }
    }

    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void morphmod$clientPhantomStartFallFlying(CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide) return;

        ResourceLocation morphId = ClientMorphData.getActiveMorph(self.getUUID());
        if (morphId != null && morphId.getPath().equals("phantom")) {
            if (!self.onGround() && !self.isFallFlying() && !self.isInWater()
                    && !self.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)) {
                self.startFallFlying();
                cir.setReturnValue(true);
            }
        }
    }
}
