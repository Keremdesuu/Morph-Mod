package com.kerem.morphmod.mixin;

import com.kerem.morphmod.morph.MorphData;
import com.kerem.morphmod.morph.MorphManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes Creepers naturally flee from players who are morphed into a Cat or an Ocelot.
 */
@Mixin(Creeper.class)
public abstract class CreeperMixin {

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void morphmod$fleeFromCatPlayer(CallbackInfo ci) {
        Creeper self = (Creeper) (Object) this;
        ((MobAccessor) self).getGoalSelector().addGoal(3, new AvoidEntityGoal<>(self, Player.class, 8.0F, 1.0, 1.3, livingEntity -> {
            if (livingEntity instanceof ServerPlayer serverPlayer) {
                MorphManager manager = MorphManager.get(serverPlayer.server);
                MorphData data = manager.getMorphData(serverPlayer.getUUID());
                if (data != null && data.isMorphActive()) {
                    ResourceLocation morph = data.getActiveMorph();
                    if (morph != null) {
                        String path = morph.getPath();
                        return path.equals("cat") || path.equals("ocelot");
                    }
                }
            }
            return false;
        }));
    }
}
