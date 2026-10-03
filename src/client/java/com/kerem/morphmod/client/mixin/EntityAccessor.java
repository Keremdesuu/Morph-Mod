package com.kerem.morphmod.client.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("wasTouchingWater")
    void setWasTouchingWater(boolean wasTouchingWater);

    @Accessor("wasTouchingWater")
    boolean getWasTouchingWater();
}
