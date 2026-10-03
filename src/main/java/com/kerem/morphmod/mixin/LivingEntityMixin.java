package com.kerem.morphmod.mixin;

import com.kerem.morphmod.morph.MorphAbility;
import com.kerem.morphmod.morph.MorphData;
import com.kerem.morphmod.morph.MorphManager;
import com.kerem.morphmod.morph.MorphRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.kerem.morphmod.event.SilverfishHideHandler;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into LivingEntity to support wall climbing ability and restrict jumping for specific morphs.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void morphmod$enableWallClimbing(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self instanceof ServerPlayer serverPlayer) {
            MorphManager manager = MorphManager.get(serverPlayer.server);
            MorphData data = manager.getMorphData(serverPlayer.getUUID());

            if (data != null && data.isMorphActive()) {
                ResourceLocation morphId = data.getActiveMorph();
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(morphId);

                if (entityType != null
                        && MorphRegistry.getAbilities(entityType).contains(MorphAbility.WALL_CLIMBING)) {
                    // Allow climbing when touching a wall (horizontal collision)
                    if (serverPlayer.horizontalCollision) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void morphmod$cancelServerJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self instanceof ServerPlayer serverPlayer) {
            if (SilverfishHideHandler.isHidden(serverPlayer.getUUID())) {
                ci.cancel();
                return;
            }

            MorphManager manager = MorphManager.get(serverPlayer.server);
            MorphData data = manager.getMorphData(serverPlayer.getUUID());

            if (data != null && data.isMorphActive()) {
                ResourceLocation morphId = data.getActiveMorph();
                if (morphId != null && morphId.getPath().equals("shulker")) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void morphmod$preventSilverfishHurt(net.minecraft.world.damagesource.DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer player) {
            if (SilverfishHideHandler.isHidden(player.getUUID())) {
                cir.setReturnValue(false);
            }
        }
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float morphmod$boostSnowballDamageForBlaze(float amount, net.minecraft.world.damagesource.DamageSource source) {
        if (amount <= 0.0F && source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Snowball) {
            LivingEntity self = (LivingEntity) (Object) this;
            if (self instanceof ServerPlayer player) {
                MorphManager manager = MorphManager.get(player.server);
                MorphData data = manager.getMorphData(player.getUUID());
                if (data != null && data.isMorphActive()) {
                    if (EntityType.BLAZE.equals(BuiltInRegistries.ENTITY_TYPE.get(data.getActiveMorph()))) {
                        return 3.0F;
                    }
                }
            }
        }
        return amount;
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void morphmod$handleHorseJumpFallDamage(float fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer player) {
            Double startY = com.kerem.morphmod.event.ActiveAbilityHandler.getHorseJumpStartY(player.getUUID());
            if (startY != null) {
                double fellBelow = startY - player.getY();
                if (fellBelow <= 1.5) {
                    cir.setReturnValue(false);
                }
            }
        }
    }

    @Inject(method = "increaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void morphmod$preventLandAirIncrease(int currentAir, CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer serverPlayer) {
            MorphManager manager = MorphManager.get(serverPlayer.server);
            MorphData data = manager.getMorphData(serverPlayer.getUUID());
            if (data != null && data.isMorphActive()) {
                ResourceLocation morphId = data.getActiveMorph();
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(morphId);
                if (MorphRegistry.isSuffocatingOnLand(type) && !serverPlayer.isInWaterRainOrBubble()) {
                    cir.setReturnValue(currentAir);
                }
            }
        }
    }

    @Inject(method = "canBreatheUnderwater", at = @At("HEAD"), cancellable = true)
    private void morphmod$canBreatheUnderwater(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer serverPlayer) {
            MorphManager manager = MorphManager.get(serverPlayer.server);
            MorphData data = manager.getMorphData(serverPlayer.getUUID());
            if (data != null && data.isMorphActive()) {
                ResourceLocation morphId = data.getActiveMorph();
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(morphId);
                if (type != null && (MorphRegistry.isSuffocatingOnLand(type)
                        || MorphRegistry.getAbilities(type).contains(MorphAbility.WATER_BREATHING))) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
    private void morphmod$preventVexWallSuffocation(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer serverPlayer) {
            if (SilverfishHideHandler.isHidden(serverPlayer.getUUID())) {
                cir.setReturnValue(false);
                return;
            }
            MorphManager manager = MorphManager.get(serverPlayer.server);
            MorphData data = manager.getMorphData(serverPlayer.getUUID());
            if (data != null && data.isMorphActive()) {
                ResourceLocation morphId = data.getActiveMorph();
                if (morphId != null && morphId.getPath().equals("vex")) {
                    cir.setReturnValue(false);
                }
            }
        }
    }

    @Inject(method = "updateFallFlying", at = @At("HEAD"), cancellable = true)
    private void morphmod$phantomKeepFallFlying(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer player) {
            MorphManager manager = MorphManager.get(player.server);
            MorphData data = manager.getMorphData(player.getUUID());
            if (data != null && data.isMorphActive()) {
                ResourceLocation morphId = data.getActiveMorph();
                if (morphId != null && morphId.getPath().equals("phantom")) {
                    if (player.isFallFlying()) {
                        if (player.onGround() || player.isPassenger() || player.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)) {
                            player.stopFallFlying();
                        }
                    }
                    ci.cancel();
                }
            }
        }
    }
}
