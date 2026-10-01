package com.kerem.morphmod.client.mixin;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for WalkAnimationState private fields.
 * Allows copying walk/run limb swing animations from player to the rendered fake entity.
 */
@Mixin(WalkAnimationState.class)
public interface WalkAnimationStateAccessor {

    @Accessor("speedOld")
    void setSpeedOld(float speedOld);

    @Accessor("speed")
    void setSpeed(float speed);

    @Accessor("position")
    void setPosition(float position);

    @Accessor("speedOld")
    float getSpeedOld();

    @Accessor("speed")
    float getSpeed();

    @Accessor("position")
    float getPosition();
}
