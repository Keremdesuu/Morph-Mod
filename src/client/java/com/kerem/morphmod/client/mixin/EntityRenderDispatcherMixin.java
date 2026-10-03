package com.kerem.morphmod.client.mixin;

import com.kerem.morphmod.client.ClientMorphData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side mixin that intercepts entity rendering to replace the player's
 * appearance with the morphed entity's model when a morph is active.
 *
 * Fully syncs walking, running, swinging, crouching, and idle animations.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Unique
    private static boolean morphmod$isRenderingMorph = false;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void morphmod$redirectPlayerRender(
            E entity, double x, double y, double z,
            float rotationYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int light,
            CallbackInfo ci
    ) {
        // Don't intercept if we're already rendering a morph (prevents recursion)
        if (morphmod$isRenderingMorph) return;

        // Only intercept player rendering
        if (!(entity instanceof Player player)) return;

        // Check if this player has an active morph
        ResourceLocation morphId = ClientMorphData.getActiveMorph(player.getUUID());
        if (morphId == null) return;

        // Hide model if player is a silverfish hidden inside a block
        if (player.isInvisible() && morphId.getPath().equals("silverfish")) {
            ci.cancel();
            return;
        }

        // Get or create the fake entity for this morph type
        Entity fakeEntity = ClientMorphData.getOrCreateFakeEntity(morphId);
        if (fakeEntity == null) return;

        // Copy all transform and animation data from the player to the fake entity
        copyTransforms(player, fakeEntity, partialTick);

        // Morph transition animation (smooth growth and subtle shimmer)
        boolean isLocalPlayer = Minecraft.getInstance().player != null && Minecraft.getInstance().player.getUUID().equals(player.getUUID());
        float transitionProgress = isLocalPlayer ? ClientMorphData.getMorphTransitionProgress(partialTick) : 1.0F;

        if (transitionProgress < 1.0F) {
            float ease = (float) Math.sin(transitionProgress * Math.PI / 2.0);
            float scale = 0.15F + 0.85F * ease;
            poseStack.pushPose();
            poseStack.scale(scale, scale, scale);
            float wobble = (float) Math.sin((1.0F - ease) * Math.PI * 3.0) * (1.0F - ease) * 8.0F;
            poseStack.mulPose(Axis.YP.rotationDegrees(wobble));
        }

        // Render the fake entity instead, with recursion guard
        morphmod$isRenderingMorph = true;
        try {
            EntityRenderDispatcher self = (EntityRenderDispatcher) (Object) this;
            self.render(fakeEntity, x, y, z, rotationYaw, partialTick, poseStack, bufferSource, light);
        } finally {
            morphmod$isRenderingMorph = false;
            if (transitionProgress < 1.0F) {
                poseStack.popPose();
            }
        }

        // Cancel the original player render
        ci.cancel();
    }

    /**
     * Copies position, rotation, physics, and animation data from the player to
     * the fake entity so it renders alive with moving limbs, swinging arms, and natural poses.
     */
    @Unique
    private static void copyTransforms(Player player, Entity fakeEntity, float partialTick) {
        // Position and interpolation
        fakeEntity.setPos(player.getX(), player.getY(), player.getZ());
        fakeEntity.xo = player.xo;
        fakeEntity.yo = player.yo;
        fakeEntity.zo = player.zo;

        // Rotation
        fakeEntity.setYRot(player.getYRot());
        fakeEntity.setXRot(player.getXRot());
        fakeEntity.yRotO = player.yRotO;
        fakeEntity.xRotO = player.xRotO;

        // Movement & physics states
        fakeEntity.tickCount = player.tickCount;
        fakeEntity.setOnGround(player.onGround());
        fakeEntity.setDeltaMovement(player.getDeltaMovement());
        fakeEntity.hasImpulse = player.hasImpulse;
        fakeEntity.horizontalCollision = player.horizontalCollision;
        fakeEntity.verticalCollision = player.verticalCollision;
        fakeEntity.fallDistance = player.fallDistance;

        // Sync water status so aquatic mobs (Cod, Tropical Fish, Salmon, Guardian) render swimming
        try {
            ((EntityAccessor) fakeEntity).setWasTouchingWater(player.isInWaterRainOrBubble());
        } catch (Exception ignored) {
        }

        // Copy LivingEntity-specific animation and limb data
        if (fakeEntity instanceof LivingEntity fakeLiving) {
            fakeLiving.yBodyRot = player.yBodyRot;
            fakeLiving.yBodyRotO = player.yBodyRotO;
            fakeLiving.yHeadRot = player.yHeadRot;
            fakeLiving.yHeadRotO = player.yHeadRotO;

            // Attack & swinging animation (when punching / breaking blocks)
            fakeLiving.swinging = player.swinging;
            fakeLiving.swingTime = player.swingTime;
            fakeLiving.attackAnim = player.attackAnim;
            fakeLiving.oAttackAnim = player.oAttackAnim;
            fakeLiving.swingingArm = player.swingingArm;

            // Poses & sneaking / sprinting / swimming
            fakeLiving.setPose(player.getPose());
            fakeLiving.setShiftKeyDown(player.isShiftKeyDown());
            fakeLiving.setSprinting(player.isSprinting());
            fakeLiving.setSwimming(player.isSwimming());

            // Hurt and death animations
            fakeLiving.hurtTime = player.hurtTime;
            fakeLiving.deathTime = player.deathTime;

            // Synchronize walking / running limb animations via WalkAnimationState
            try {
                WalkAnimationStateAccessor targetWalk = (WalkAnimationStateAccessor) fakeLiving.walkAnimation;
                WalkAnimationStateAccessor sourceWalk = (WalkAnimationStateAccessor) player.walkAnimation;
                if (fakeEntity instanceof Turtle) {
                    targetWalk.setSpeedOld(sourceWalk.getSpeedOld() * 0.25F);
                    targetWalk.setSpeed(sourceWalk.getSpeed() * 0.25F);
                    targetWalk.setPosition(sourceWalk.getPosition() * 0.25F);
                } else {
                    targetWalk.setSpeedOld(sourceWalk.getSpeedOld());
                    targetWalk.setSpeed(sourceWalk.getSpeed());
                    targetWalk.setPosition(sourceWalk.getPosition());
                }
            } catch (Exception ignored) {
            }

            // Sync held items
            fakeLiving.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem());
            fakeLiving.setItemInHand(InteractionHand.OFF_HAND, player.getOffhandItem());
        }

        // Special handling for Ender Dragon animation & latency buffer
        if (fakeEntity instanceof EnderDragon dragon) {
            float dragonYaw = player.getYRot() + 180.0F; // Fix dragon model facing backwards
            if (dragon.posPointer < 0) {
                dragon.posPointer = 0;
                for (int i = 0; i < dragon.positions.length; i++) {
                    dragon.positions[i][0] = dragonYaw;
                    dragon.positions[i][1] = player.getY();
                    dragon.positions[i][2] = 0.0;
                }
            }

            double horizDist = player.getDeltaMovement().horizontalDistance();
            float flapSpeed = (player.onGround() && horizDist < 0.01) ? 0.015F : 0.035F;
            float currentFlap = (player.tickCount + partialTick) * flapSpeed;
            dragon.oFlapTime = currentFlap - flapSpeed;
            dragon.flapTime = currentFlap;

            dragon.posPointer = (dragon.posPointer + 1) & 63;
            dragon.positions[dragon.posPointer][0] = dragonYaw;
            dragon.positions[dragon.posPointer][1] = player.getY();
            dragon.positions[dragon.posPointer][2] = 0.0;
        }

        // Special handling for Squid / Glow Squid tentacle movement animation
        if (fakeEntity instanceof Squid squid) {
            float moveSpeed = player.isInWaterRainOrBubble() ? 0.12F : 0.05F;
            float moveTime = (player.tickCount + partialTick) * moveSpeed;
            squid.oldTentacleAngle = squid.tentacleAngle;
            squid.tentacleAngle = Math.abs(Mth.sin(moveTime)) * (float) Math.PI * 0.25F;
            squid.oldTentacleMovement = squid.tentacleMovement;
            squid.tentacleMovement = moveTime;
        }

        // Special handling for Guardian tail and spikes animation (smooth natural speed)
        if (fakeEntity instanceof Guardian guardian) {
            try {
                GuardianAccessor gAccessor = (GuardianAccessor) guardian;
                gAccessor.setClientSideTailAnimationO(gAccessor.getClientSideTailAnimation());
                float tailInc = player.isInWaterRainOrBubble() ? 0.04F : 0.08F;
                gAccessor.setClientSideTailAnimation(gAccessor.getClientSideTailAnimation() + tailInc);
                gAccessor.setClientSideSpikesAnimationO(gAccessor.getClientSideSpikesAnimation());
                gAccessor.setClientSideSpikesAnimation(gAccessor.getClientSideSpikesAnimation() + 0.015F);
            } catch (Exception ignored) {
            }
        }

        // Special handling for Parrot flying and walking animation
        if (fakeEntity instanceof Parrot parrot) {
            boolean isAirborne = !player.onGround();
            if (isAirborne) {
                parrot.oFlap = parrot.flap;
                parrot.flap += 0.45F;
                parrot.flapSpeed = 1.0F;
                parrot.oFlapSpeed = 1.0F;
            } else {
                parrot.oFlap = parrot.flap;
                parrot.flapSpeed = 0.0F;
                parrot.oFlapSpeed = 0.0F;
            }
        }

        // Special handling for Rabbit hopping animation
        if (fakeEntity instanceof Rabbit rabbit) {
            try {
                RabbitAccessor rAccessor = (RabbitAccessor) rabbit;
                boolean isMoving = player.getDeltaMovement().horizontalDistance() > 0.01;
                if (!player.onGround()) {
                    rAccessor.setJumpDuration(16);
                    rAccessor.setJumpTicks(Math.min(16, player.tickCount % 16));
                } else if (isMoving) {
                    rAccessor.setJumpDuration(8);
                    rAccessor.setJumpTicks(player.tickCount % 8);
                } else {
                    rAccessor.setJumpDuration(0);
                    rAccessor.setJumpTicks(0);
                }
            } catch (Exception ignored) {
            }
        }

        // Special handling for Fox pounce pose (ONLY when active ability was used)
        if (fakeEntity instanceof Fox fox) {
            fox.setIsPouncing(ClientMorphData.isFoxPouncing() && !player.onGround());
        }

        // Special handling for Chicken wing flapping during glide
        if (fakeEntity instanceof Chicken chicken) {
            boolean isFalling = !player.onGround() && player.getDeltaMovement().y < -0.01;
            boolean isLocalPlayer = Minecraft.getInstance().player != null
                    && Minecraft.getInstance().player.getUUID().equals(player.getUUID());
            boolean jumpDown = isLocalPlayer && Minecraft.getInstance().options.keyJump.isDown();
            boolean isGliding = isFalling && (jumpDown || player.getDeltaMovement().y > -0.15);

            if (isGliding) {
                chicken.oFlap = chicken.flap;
                chicken.flap += 0.3F;
                chicken.flapSpeed = 1.0F;
                chicken.oFlapSpeed = 1.0F;
                chicken.flapping = 1.0F;
            } else {
                chicken.flapSpeed = 0.0F;
                chicken.oFlapSpeed = 0.0F;
                chicken.flapping = 0.0F;
            }
        }

        // Special handling for Iron Golem attack slam animation
        if (fakeEntity instanceof IronGolem ironGolem) {
            try {
                IronGolemAccessor accessor = (IronGolemAccessor) ironGolem;
                if (player.attackAnim > 0.0F) {
                    int attackTick = Math.max(1, (int) ((1.0F - player.attackAnim) * 10.0F));
                    accessor.setAttackAnimationTick(attackTick);
                } else {
                    accessor.setAttackAnimationTick(0);
                }
            } catch (Exception ignored) {
            }
        }

        // Special handling for Polar Bear attack standing animation
        if (fakeEntity instanceof PolarBear polarBear) {
            polarBear.setStanding(player.attackAnim > 0.0F);
        }

        // Special handling for Evoker spell casting arm pose
        if (fakeEntity instanceof SpellcasterIllager illager) {
            illager.getEntityData().set(SpellcasterIllagerAccessor.getSpellCastingId(), (byte) (ClientMorphData.isEvokerCasting() ? 1 : 0));
        }

        // Special handling for Pufferfish inflation state
        if (fakeEntity instanceof Pufferfish pufferfish) {
            pufferfish.setPuffState(ClientMorphData.isPufferPuffed() ? Pufferfish.STATE_FULL : Pufferfish.STATE_SMALL);
        }

        // Special handling for Warden attack arm raising & smash
        if (fakeEntity instanceof Warden warden) {
            if (player.attackAnim > 0.0F) {
                warden.attackAnimationState.startIfStopped(player.tickCount);
            }
            if (warden.attackAnimationState.getAccumulatedTime() > 800L) {
                warden.attackAnimationState.stop();
            }
        }

        // Special handling for Camel dash animation
        if (fakeEntity instanceof Camel camel) {
            if (ClientMorphData.isCamelDashing()) {
                camel.dashAnimationState.startIfStopped(player.tickCount);
                camel.setDashing(true);
            } else {
                camel.dashAnimationState.stop();
                camel.setDashing(false);
            }
        }

        // Special handling for Armadillo rolling up into a ball shell
        if (fakeEntity instanceof Armadillo armadillo) {
            if (ClientMorphData.isArmadilloRolling()) {
                armadillo.switchToState(Armadillo.ArmadilloState.SCARED);
                armadillo.rollUpAnimationState.startIfStopped(player.tickCount);
            } else {
                armadillo.switchToState(Armadillo.ArmadilloState.IDLE);
                armadillo.rollUpAnimationState.stop();
            }
        }

        // Special handling for Axolotl playing dead animation
        if (fakeEntity instanceof Axolotl axolotl) {
            axolotl.setPlayingDead(ClientMorphData.isAxolotlPlayingDead());
        }

        // Special handling for Bat flying animation
        if (fakeEntity instanceof Bat bat) {
            bat.restAnimationState.stop();
            bat.flyAnimationState.startIfStopped(player.tickCount);
        }
    }
}
