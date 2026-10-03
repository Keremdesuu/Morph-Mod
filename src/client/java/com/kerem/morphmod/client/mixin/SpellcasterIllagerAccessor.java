package com.kerem.morphmod.client.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpellcasterIllager.class)
public interface SpellcasterIllagerAccessor {
    @Accessor("DATA_SPELL_CASTING_ID")
    static EntityDataAccessor<Byte> getSpellCastingId() {
        throw new AssertionError();
    }
}
