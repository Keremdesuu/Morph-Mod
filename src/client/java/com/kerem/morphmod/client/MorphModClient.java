package com.kerem.morphmod.client;

import com.kerem.morphmod.network.ActiveMorphUpdatePayload;
import com.kerem.morphmod.network.MorphSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.UUID;

/**
 * Client-side mod initializer.
 * Registers key bindings, network packet receivers, and cleanup handlers.
 */
public class MorphModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register key bindings
        MorphKeyBindings.register();

        // ─── Network Packet Receivers ────────────────────────────

        // Receive full morph collection sync (our own data)
        ClientPlayNetworking.registerGlobalReceiver(
                MorphSyncPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        ClientMorphData.setCollectedMorphs(payload.morphIds());
                        ClientMorphData.setActiveMorph(payload.activeMorphId());
                    });
                }
        );

        // Receive morph updates for any player (including ourselves)
        ClientPlayNetworking.registerGlobalReceiver(
                ActiveMorphUpdatePayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        UUID playerId = UUID.fromString(payload.playerUuid());
                        ClientMorphData.setOtherPlayerMorph(playerId, payload.morphId());

                        // Also update our own active morph if this is about us
                        if (context.client().player != null
                                && context.client().player.getUUID().equals(playerId)) {
                            ClientMorphData.setActiveMorph(payload.morphId());
                        }
                    });
                }
        );

        // ─── Cleanup on Disconnect ───────────────────────────────

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            client.execute(ClientMorphData::reset);
        });
    }
}
