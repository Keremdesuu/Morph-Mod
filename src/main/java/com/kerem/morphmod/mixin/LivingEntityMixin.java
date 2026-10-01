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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into LivingEntity to support wall climbing ability.
 * When a player is morphed into a spider/cave spider, they can climb walls.
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
}
