package com.kerem.morphmod.client.mixin;

import net.minecraft.world.entity.monster.Guardian;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Guardian.class)
public interface GuardianAccessor {
    @Accessor("clientSideTailAnimation")
    float getClientSideTailAnimation();

    @Accessor("clientSideTailAnimation")
    void setClientSideTailAnimation(float value);

    @Accessor("clientSideTailAnimationO")
    void setClientSideTailAnimationO(float value);

    @Accessor("clientSideTailAnimationSpeed")
    void setClientSideTailAnimationSpeed(float value);

    @Accessor("clientSideSpikesAnimation")
    float getClientSideSpikesAnimation();

    @Accessor("clientSideSpikesAnimation")
    void setClientSideSpikesAnimation(float value);

    @Accessor("clientSideSpikesAnimationO")
    void setClientSideSpikesAnimationO(float value);
}
