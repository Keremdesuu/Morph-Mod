package com.kerem.morphmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-Server payload: client requests to morph into a specific entity type.
 * An empty targetMorphId means the player wants to return to normal form.
 */
public record MorphRequestPayload(String targetMorphId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MorphRequestPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("morphmod", "morph_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MorphRequestPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeUtf(payload.targetMorphId),
                    buf -> new MorphRequestPayload(buf.readUtf())
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
