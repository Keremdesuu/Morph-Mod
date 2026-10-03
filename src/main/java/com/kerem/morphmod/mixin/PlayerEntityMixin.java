package com.kerem.morphmod.mixin;

import com.kerem.morphmod.morph.MorphData;
import com.kerem.morphmod.morph.MorphManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Server-side mixin to adapt the player's bounding box dimensions to match their active morph.
 * Clamps dimensions to playable bounds so the player never gets stuck inside blocks.
 */
@Mixin(Player.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
    private void morphmod$modifyDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Player self = (Player) (Object) this;

        if (self instanceof ServerPlayer serverPlayer) {
            MorphManager manager = MorphManager.get(serverPlayer.server);
            MorphData data = manager.getMorphData(serverPlayer.getUUID());

            if (data != null && data.isMorphActive()) {
                ResourceLocation morphId = data.getActiveMorph();
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
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void morphmod$onServerPlayerTick(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self instanceof ServerPlayer serverPlayer) {
            if (com.kerem.morphmod.event.SilverfishHideHandler.isHidden(serverPlayer.getUUID())) {
                serverPlayer.noPhysics = true;
                return;
            }
            MorphManager manager = MorphManager.get(serverPlayer.server);
            MorphData data = manager.getMorphData(serverPlayer.getUUID());
            if (data != null && data.isMorphActive()) {
                ResourceLocation morphId = data.getActiveMorph();
                if (morphId != null && morphId.getPath().equals("vex")) {
                    serverPlayer.noPhysics = true;
                    serverPlayer.resetFallDistance();
                }
            }
        }
    }

    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void morphmod$phantomStartFallFlying(CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (self instanceof ServerPlayer serverPlayer) {
            MorphManager manager = MorphManager.get(serverPlayer.server);
            MorphData data = manager.getMorphData(serverPlayer.getUUID());
            if (data != null && data.isMorphActive()) {
                ResourceLocation morphId = data.getActiveMorph();
                if (morphId != null && morphId.getPath().equals("phantom")) {
                    if (!self.onGround() && !self.isFallFlying() && !self.isInWater()
                            && !self.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)) {
                        self.startFallFlying();
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }
}
