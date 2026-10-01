package com.kerem.morphmod.client.mixin;

import com.kerem.morphmod.client.ClientMorphData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

        // Get or create the fake entity for this morph type
        Entity fakeEntity = ClientMorphData.getOrCreateFakeEntity(morphId);
        if (fakeEntity == null) return;

        // Copy all transform and animation data from the player to the fake entity
        copyTransforms(player, fakeEntity, partialTick);

        // Render the fake entity instead, with recursion guard
        morphmod$isRenderingMorph = true;
        try {
            EntityRenderDispatcher self = (EntityRenderDispatcher) (Object) this;
            self.render(fakeEntity, x, y, z, rotationYaw, partialTick, poseStack, bufferSource, light);
        } finally {
            morphmod$isRenderingMorph = false;
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
                targetWalk.setSpeedOld(sourceWalk.getSpeedOld());
                targetWalk.setSpeed(sourceWalk.getSpeed());
                targetWalk.setPosition(sourceWalk.getPosition());
            } catch (Exception ignored) {
            }

            // Sync held items
            fakeLiving.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem());
            fakeLiving.setItemInHand(InteractionHand.OFF_HAND, player.getOffhandItem());
        }
    }
}
