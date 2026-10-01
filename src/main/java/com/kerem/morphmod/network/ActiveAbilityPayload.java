package com.kerem.morphmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-to-Server payload: signals that the player wants to use their
 * morph's active ability. No data is needed — the server determines
 * the ability from the player's current morph.
 */
public record ActiveAbilityPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ActiveAbilityPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("morphmod", "active_ability"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActiveAbilityPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> { /* no data to write */ },
                    buf -> new ActiveAbilityPayload()
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
