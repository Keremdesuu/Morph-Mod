package com.kerem.morphmod.client.mixin;

import net.minecraft.world.entity.animal.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for IronGolem private attackAnimationTick field.
 * Allows triggering Iron Golem's arm raising and slam attack animation when attacking.
 */
@Mixin(IronGolem.class)
public interface IronGolemAccessor {

    @Accessor("attackAnimationTick")
    void setAttackAnimationTick(int tick);

    @Accessor("attackAnimationTick")
    int getAttackAnimationTick();
}
