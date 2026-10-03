package com.kerem.morphmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server-to-Client payload to notify the client when the local player
 * enters or leaves a block as a Silverfish.
 */
public record SilverfishHiddenPayload(boolean hidden) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SilverfishHiddenPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("morphmod", "silverfish_hidden"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SilverfishHiddenPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeBoolean(payload.hidden),
                    buf -> new SilverfishHiddenPayload(buf.readBoolean())
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
