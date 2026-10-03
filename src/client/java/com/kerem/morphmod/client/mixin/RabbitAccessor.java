package com.kerem.morphmod.client.mixin;

import net.minecraft.world.entity.animal.Rabbit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Rabbit.class)
public interface RabbitAccessor {
    @Accessor("jumpTicks")
    void setJumpTicks(int ticks);

    @Accessor("jumpDuration")
    void setJumpDuration(int duration);
}
