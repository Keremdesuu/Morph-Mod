package com.kerem.morphmod.event;

import com.kerem.morphmod.network.SilverfishHiddenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles Silverfish block infestation / hiding ability.
 * Allows the player to enter a solid block, becoming invisible and untargetable,
 * locked inside without falling through or jumping, and emerge when pressing G again.
 */
public class SilverfishHideHandler {
    private static final Set<UUID> hiddenPlayers = new HashSet<>();
    private static final Map<UUID, Vec3> hiddenPositions = new HashMap<>();

    public static boolean isHidden(UUID playerUuid) {
        return hiddenPlayers.contains(playerUuid);
    }

    public static Vec3 getHiddenPos(UUID playerUuid) {
        return hiddenPositions.get(playerUuid);
    }

    public static boolean toggleHide(ServerPlayer player, ServerLevel level) {
        UUID uuid = player.getUUID();

        if (hiddenPlayers.contains(uuid)) {
            emerge(player, level);
            return true;
        } else {
            return enterBlock(player, level);
        }
    }

    private static boolean enterBlock(ServerPlayer player, ServerLevel level) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 reach = eyePos.add(lookVec.scale(4.0));

        BlockHitResult hit = level.clip(new ClipContext(
                eyePos, reach,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.literal("§cGirecek bir blok bulamadın! Bir bloğa bakmalısın."), true);
            return false;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);

        if (state.isAir() || !state.isSolidRender(level, pos)) {
            player.displayClientMessage(Component.literal("§cBu bloğun içine giremezsin! Katı bir blok seçmelisin."), true);
            return false;
        }

        // Enter block
        Vec3 lockPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5);
        hiddenPlayers.add(player.getUUID());
        hiddenPositions.put(player.getUUID(), lockPos);

        // Center player in the block and lock physics
        player.teleportTo(lockPos.x, lockPos.y, lockPos.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.setNoGravity(true);
        player.noPhysics = true;
        player.resetFallDistance();

        // Apply invisibility and invulnerability
        player.setInvisible(true);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 255, false, false, false));

        // Clear mob aggro around player
        level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16), m -> m.getTarget() == player)
                .forEach(m -> m.setTarget(null));

        // Sync with client
        ServerPlayNetworking.send(player, new SilverfishHiddenPayload(true));

        // Sounds and particles
        level.playSound(null, pos, SoundEvents.SILVERFISH_STEP, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, pos, SoundEvents.STONE_HIT, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                20, 0.3, 0.3, 0.3, 0.1);

        player.displayClientMessage(Component.literal("§a✦ Bloğun içine saklandın! Çıkmak için tekrar [G] tuşuna bas. ✦"), true);
        return true;
    }

    public static void emerge(ServerPlayer player, ServerLevel level) {
        if (!hiddenPlayers.remove(player.getUUID())) {
            return;
        }
        hiddenPositions.remove(player.getUUID());

        // Restore physics and visibility
        player.setNoGravity(false);
        player.noPhysics = false;
        player.setInvisible(false);
        player.removeEffect(MobEffects.INVISIBILITY);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

        // Pop up slightly
        player.setDeltaMovement(0, 0.35, 0);
        player.hurtMarked = true;

        // Sync with client
        ServerPlayNetworking.send(player, new SilverfishHiddenPayload(false));

        BlockPos pos = player.blockPosition();
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            state = level.getBlockState(pos.below());
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SILVERFISH_HURT, SoundSource.PLAYERS, 1.0F, 1.4F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);

        if (!state.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    player.getX(), player.getY() + 0.2, player.getZ(),
                    25, 0.3, 0.3, 0.3, 0.15);
        }

        player.displayClientMessage(Component.literal("§e✦ Bloktan çıktın! ✦"), true);
    }

    /**
     * Called every tick on server to keep the player locked inside the block.
     */
    public static void keepLocked(ServerPlayer player) {
        if (hiddenPlayers.contains(player.getUUID())) {
            Vec3 pos = hiddenPositions.get(player.getUUID());
            player.setDeltaMovement(Vec3.ZERO);
            player.setNoGravity(true);
            player.noPhysics = true;
            player.resetFallDistance();
            player.hurtTime = 0;
            player.hurtMarked = false;
            if (pos != null && player.position().distanceToSqr(pos) > 0.08) {
                player.teleportTo(pos.x, pos.y, pos.z);
            }
        }
    }

    public static void reset(UUID uuid) {
        hiddenPlayers.remove(uuid);
        hiddenPositions.remove(uuid);
    }
}
