package com.kerem.morphmod.client.mixin;

import com.kerem.morphmod.client.ClientMorphData;
import com.kerem.morphmod.morph.MorphAbility;
import com.kerem.morphmod.morph.MorphRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side mixin into LivingEntity to enable wall climbing and restrict jumping.
 * Matches the server-side LivingEntityMixin so movement prediction doesn't desync or stutter.
 */
@Mixin(LivingEntity.class)
public abstract class ClientLivingEntityMixin {

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void morphmod$enableClientWallClimbing(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self instanceof Player player && player.level().isClientSide) {
            ResourceLocation morphId = ClientMorphData.getActiveMorph(player.getUUID());
            if (morphId != null) {
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(morphId);
                if (entityType != null
                        && MorphRegistry.getAbilities(entityType).contains(MorphAbility.WALL_CLIMBING)) {
                    if (player.horizontalCollision) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void morphmod$cancelClientJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self instanceof Player player && player.level().isClientSide) {
            if (ClientMorphData.isSilverfishHidden()) {
                ci.cancel();
                return;
            }
            ResourceLocation morphId = ClientMorphData.getActiveMorph(player.getUUID());
            if (morphId != null && morphId.getPath().equals("shulker")) {
                ci.cancel();
            }
        }
    }
}
