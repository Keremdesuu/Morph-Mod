package com.kerem.morphmod.mixin;

import com.kerem.morphmod.morph.MorphData;
import com.kerem.morphmod.morph.MorphManager;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into ServerGamePacketListenerImpl to allow players morphed into a Vex
 * to pass through solid blocks without the server detecting "moved wrongly" and teleporting them back.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void morphmod$allowVexPassThroughBlocks(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        ServerGamePacketListenerImpl self = (ServerGamePacketListenerImpl) (Object) this;
        if (self.player != null) {
            MorphManager manager = MorphManager.get(self.player.server);
            MorphData data = manager.getMorphData(self.player.getUUID());
            if (data != null && data.isMorphActive()) {
                ResourceLocation morphId = data.getActiveMorph();
                if (morphId != null && morphId.getPath().equals("vex")) {
                    self.player.noPhysics = true;
                }
            }
        }
    }
}
