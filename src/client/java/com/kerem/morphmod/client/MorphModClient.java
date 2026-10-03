package com.kerem.morphmod.client;

import com.kerem.morphmod.network.ActiveMorphUpdatePayload;
import com.kerem.morphmod.network.MorphSyncPayload;
import com.kerem.morphmod.network.SilverfishHiddenPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

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
                        if (context.client().player != null) {
                            context.client().player.refreshDimensions();
                        }
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
                            context.client().player.refreshDimensions();
                        }
                    });
                }
        );

        // Receive silverfish hidden status
        ClientPlayNetworking.registerGlobalReceiver(
                SilverfishHiddenPayload.TYPE,
                (payload, context) -> {
                    context.client().execute(() -> {
                        ClientMorphData.setSilverfishHidden(payload.hidden());
                        if (context.client().player != null) {
                            context.client().player.setNoGravity(payload.hidden());
                            context.client().player.noPhysics = payload.hidden();
                            if (payload.hidden()) {
                                context.client().player.setDeltaMovement(Vec3.ZERO);
                            }
                        }
                    });
                }
        );

        // ─── Cleanup on Disconnect ───────────────────────────────

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            client.execute(ClientMorphData::reset);
        });

        // ─── Movement / Input Locks & Gliding Mechanics ──────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.isPaused()) return;

            ClientMorphData.tickCameraAndTransition();
            ClientMorphData.tickFoxPounce(client.player);

            ResourceLocation morphId = ClientMorphData.getActiveMorph(client.player.getUUID());
            boolean isShulker = morphId != null && morphId.getPath().equals("shulker");
            boolean isHiddenSilverfish = ClientMorphData.isSilverfishHidden();

            // Lock out walking and jumping for Shulker and hidden Silverfish
            if (isShulker || isHiddenSilverfish) {
                if (client.player.input != null) {
                    client.player.input.jumping = false;
                    client.player.input.forwardImpulse = 0.0F;
                    client.player.input.leftImpulse = 0.0F;
                    client.player.input.shiftKeyDown = false;
                }
                if (isHiddenSilverfish) {
                    client.player.setDeltaMovement(Vec3.ZERO);
                    client.player.setNoGravity(true);
                    client.player.noPhysics = true;
                    client.player.hurtTime = 0;
                    client.player.hurtDuration = 0;
                }
            }

            // Vex Noclip / Pass through blocks
            if (morphId != null && morphId.getPath().equals("vex")) {
                client.player.noPhysics = true;
                client.player.resetFallDistance();
            }

            // Slime Bouncing Mechanic (behaves like a slime block)
            if (morphId != null && morphId.getPath().equals("slime")) {
                client.player.resetFallDistance();
                if (client.player.onGround() && !client.player.isShiftKeyDown()) {
                    if (morphmod$lastFallY < -0.15) {
                        double bounce = Math.min(1.2, Math.max(0.4, Math.abs(morphmod$lastFallY) * 0.8));
                        Vec3 v = client.player.getDeltaMovement();
                        client.player.setDeltaMovement(v.x, bounce, v.z);
                        client.player.hasImpulse = true;
                        client.player.playSound(net.minecraft.sounds.SoundEvents.SLIME_SQUISH, 1.0F, 1.0F);
                    }
                }
                morphmod$lastFallY = client.player.getDeltaMovement().y;
            } else {
                morphmod$lastFallY = 0.0;
            }

            // Chicken Glide Mechanic
            if (morphId != null && morphId.getPath().equals("chicken")) {
                if (!client.player.onGround() && client.player.getDeltaMovement().y < 0.0) {
                    if (client.options.keyJump.isDown()) {
                        Vec3 v = client.player.getDeltaMovement();
                        // Glide downwards gently at -0.06 m/tick (real chicken glide)
                        if (v.y < -0.06) {
                            client.player.setDeltaMovement(v.x, Math.max(v.y * 0.6, -0.06), v.z);
                        }
                        client.player.resetFallDistance();
                    }
                }
            }
        });
    }

    private static double morphmod$lastFallY = 0.0;
}
