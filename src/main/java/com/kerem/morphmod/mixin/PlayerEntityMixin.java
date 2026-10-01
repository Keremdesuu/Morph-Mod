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
 * Mixin into Player to modify the player's bounding box dimensions
 * when morphed into another entity type.
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
                    // Use the morph entity's default dimensions
                    EntityDimensions morphDimensions = entityType.getDimensions();
                    cir.setReturnValue(morphDimensions);
                }
            }
        }
    }
}
