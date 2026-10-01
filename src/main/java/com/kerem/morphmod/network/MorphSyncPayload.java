package com.kerem.morphmod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-to-Client payload: sends the full morph collection and active morph
 * to the owning player (used on join and when a new morph is collected).
 */
public record MorphSyncPayload(List<String> morphIds, String activeMorphId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MorphSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("morphmod", "sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MorphSyncPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeVarInt(payload.morphIds.size());
                        for (String id : payload.morphIds) {
                            buf.writeUtf(id);
                        }
                        buf.writeUtf(payload.activeMorphId);
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        List<String> ids = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            ids.add(buf.readUtf());
                        }
                        return new MorphSyncPayload(ids, buf.readUtf());
                    }
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
