package com.kerem.morphmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server-to-Client broadcast payload: notifies all clients that a player
 * has changed their active morph. Used for rendering other players' morphs.
 */
public record ActiveMorphUpdatePayload(String playerUuid, String morphId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ActiveMorphUpdatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("morphmod", "active_morph_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActiveMorphUpdatePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUtf(payload.playerUuid);
                        buf.writeUtf(payload.morphId);
                    },
                    buf -> new ActiveMorphUpdatePayload(buf.readUtf(), buf.readUtf())
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
