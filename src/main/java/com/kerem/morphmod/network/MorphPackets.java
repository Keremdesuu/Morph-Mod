package com.kerem.morphmod.network;

import com.kerem.morphmod.event.MorphAbilityHandler;
import com.kerem.morphmod.morph.MorphData;
import com.kerem.morphmod.morph.MorphManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers all network packets and handles server-side packet reception.
 */
public class MorphPackets {

    /**
     * Registers all payload types and server-side packet handlers.
     */
    public static void registerPackets() {
        // Register payload types for both directions
        PayloadTypeRegistry.playC2S().register(MorphRequestPayload.TYPE, MorphRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ActiveAbilityPayload.TYPE, ActiveAbilityPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MorphSyncPayload.TYPE, MorphSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ActiveMorphUpdatePayload.TYPE, ActiveMorphUpdatePayload.STREAM_CODEC);

        // Handle morph requests from clients
        ServerPlayNetworking.registerGlobalReceiver(
                MorphRequestPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();
                    context.server().execute(() -> handleMorphRequest(player, payload.targetMorphId()));
                }
        );

        // Handle active ability requests from clients
        ServerPlayNetworking.registerGlobalReceiver(
                ActiveAbilityPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();
                    context.server().execute(() -> com.kerem.morphmod.event.ActiveAbilityHandler.handleAbilityUse(player));
                }
        );
    }

    /**
     * Processes a morph request from a client.
     * Validates that the player owns the morph and applies the transformation.
     */
    private static void handleMorphRequest(ServerPlayer player, String targetMorphId) {
        MorphManager manager = MorphManager.get(player.server);
        MorphData data = manager.getOrCreateMorphData(player.getUUID());

        if (targetMorphId.isEmpty()) {
            // Player wants to return to normal form
            manager.clearActiveMorph(player.getUUID());
            MorphAbilityHandler.resetAbilities(player);

            // Refresh player dimensions
            player.refreshDimensions();

            // Broadcast un-morph to all players
            broadcastMorphUpdate(player, "");
        } else {
            ResourceLocation morphId = ResourceLocation.parse(targetMorphId);

            // Security check: verify the player actually has this morph
            if (data.hasMorph(morphId)) {
                // Reset previous morph abilities
                MorphAbilityHandler.resetAbilities(player);

                manager.setActiveMorph(player.getUUID(), morphId);

                // Apply morph health and abilities
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(morphId);
                if (entityType != null) {
                    MorphAbilityHandler.applyMorphHealth(player, entityType);
                    MorphAbilityHandler.applyAbilities(player, entityType);
                }

                // Refresh player dimensions to match morph
                player.refreshDimensions();

                // Broadcast morph change to all players
                broadcastMorphUpdate(player, targetMorphId);
            }
        }
    }

    /**
     * Sends the full morph collection to a specific player (their own data).
     */
    public static void syncMorphData(ServerPlayer player) {
        MorphManager manager = MorphManager.get(player.server);
        MorphData data = manager.getOrCreateMorphData(player.getUUID());

        List<String> morphIds = new ArrayList<>();
        for (ResourceLocation id : data.getCollectedMorphs()) {
            morphIds.add(id.toString());
        }

        String activeMorphId = data.getActiveMorph() != null
                ? data.getActiveMorph().toString() : "";

        ServerPlayNetworking.send(player, new MorphSyncPayload(morphIds, activeMorphId));
    }

    /**
     * Broadcasts a morph change to ALL connected players.
     * This allows other players to see the morphed appearance.
     */
    public static void broadcastMorphUpdate(ServerPlayer morphedPlayer, String morphId) {
        ActiveMorphUpdatePayload payload = new ActiveMorphUpdatePayload(
                morphedPlayer.getUUID().toString(), morphId
        );

        for (ServerPlayer player : morphedPlayer.server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    /**
     * Called when a player joins the server.
     * Syncs their own morph data and all other players' active morphs.
     */
    public static void syncAllMorphsOnJoin(ServerPlayer joiningPlayer) {
        // Send this player's own morph collection
        syncMorphData(joiningPlayer);

        // Send all other players' active morphs to the joining player
        MorphManager manager = MorphManager.get(joiningPlayer.server);
        for (ServerPlayer otherPlayer : joiningPlayer.server.getPlayerList().getPlayers()) {
            if (otherPlayer != joiningPlayer) {
                MorphData otherData = manager.getMorphData(otherPlayer.getUUID());
                if (otherData != null && otherData.isMorphActive()) {
                    ServerPlayNetworking.send(joiningPlayer, new ActiveMorphUpdatePayload(
                            otherPlayer.getUUID().toString(),
                            otherData.getActiveMorph().toString()
                    ));
                }
            }
        }

        // Also notify others that this player might be morphed
        MorphData joiningData = manager.getMorphData(joiningPlayer.getUUID());
        if (joiningData != null && joiningData.isMorphActive()) {
            broadcastMorphUpdate(joiningPlayer, joiningData.getActiveMorph().toString());
        }
    }
}
