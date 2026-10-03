package com.kerem.morphmod.event;

import com.kerem.morphmod.morph.*;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

import java.util.*;

/**
 * Handles all active (key-triggered) morph abilities and melee attack effects.
 * Each mob type can have one active ability triggered by pressing the ability key.
 * Also handles passive melee effects (wither strike, poison strike, etc.)
 */
public class ActiveAbilityHandler {

    // Cooldown tracking: player UUID → (ability → last use game tick)
    private static final Map<UUID, Map<ActiveAbility, Long>> cooldowns = new HashMap<>();

    /**
     * Registers the melee attack event handler for passive strike effects.
     */
    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide && player instanceof ServerPlayer serverPlayer
                    && entity instanceof LivingEntity target) {
                handleMeleeEffects(serverPlayer, target);
            }
            return InteractionResult.PASS;
        });
    }

    // ═══════════════════════════════════════════════════════════════
    //  ACTIVE ABILITY ACTIVATION
    // ═══════════════════════════════════════════════════════════════

    private static final Map<UUID, Double> horseJumpStartPositions = new HashMap<>();

    public static Double getHorseJumpStartY(UUID uuid) {
        return horseJumpStartPositions.remove(uuid);
    }

    public static void handleAbilityUse(ServerPlayer player) {
        handleAbilityUse(player, false);
    }

    /**
     * Called when a player presses the primary (G) or secondary (H) ability key.
     * Validates the morph, checks cooldown, and executes the ability.
     */
    public static void handleAbilityUse(ServerPlayer player, boolean isSecondary) {
        MorphManager manager = MorphManager.get(player.server);
        MorphData data = manager.getMorphData(player.getUUID());
        if (data == null || !data.isMorphActive()) return;

        ResourceLocation morphId = data.getActiveMorph();
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(morphId);
        if (entityType == null) return;

        MorphRegistry.MorphEntry entry = MorphRegistry.getEntry(entityType);
        if (entry == null) return;

        ActiveAbility ability = isSecondary ? entry.secondaryAbility() : entry.primaryAbility();
        if (ability == null || ability == ActiveAbility.NONE) {
            player.displayClientMessage(
                    Component.translatable(isSecondary ? "morphmod.ability.no_secondary" : "morphmod.ability.none"), true);
            return;
        }

        // Check cooldown
        if (isOnCooldown(player, ability)) {
            long remaining = getRemainingCooldown(player, ability);
            float seconds = remaining / 20.0F;
            player.displayClientMessage(
                    Component.translatable("morphmod.ability.cooldown",
                            String.format("%.1f", seconds)),
                    true);
            return;
        }

        // Execute the ability
        boolean success = executeAbility(player, ability);
        if (success) {
            setCooldown(player, ability);
        }
    }

    /**
     * Routes to the correct ability implementation.
     */
    private static boolean executeAbility(ServerPlayer player, ActiveAbility ability) {
        ServerLevel level = player.serverLevel();

        return switch (ability) {
            // Projectiles
            case SHOOT_ARROW -> shootArrow(player, level);
            case SHOOT_FIRE_CHARGE -> shootFireCharge(player, level);
            case SHOOT_LARGE_FIREBALL -> shootLargeFireball(player, level);
            case DRAGON_FIREBALL -> shootDragonFireball(player, level);
            case WITHER_SKULL -> shootWitherSkull(player, level);
            case SHULKER_BULLET -> shootShulkerBullet(player, level);
            case THROW_SNOWBALL -> throwSnowball(player, level);
            case THROW_POTION -> throwPoisonPotion(player, level);
            case THROW_HEALING_POTION -> throwHealingPotion(player, level);
            case THROW_TRIDENT -> throwTrident(player, level);
            case SPIT -> spit(player, level);
            case WIND_CHARGE -> windCharge(player, level);
            case SHOOT_WEB -> shootWeb(player, level);

            // Area attacks
            case EXPLODE -> explode(player, level);
            case SONIC_BOOM -> sonicBoom(player, level);
            case ROAR -> roar(player, level);
            case GROUND_SLAM -> groundSlam(player, level);
            case SUMMON_FANGS -> summonFangs(player, level);
            case PUFF_UP -> puffUp(player, level);
            case SNEEZE -> sneeze(player, level);
            case LASER -> laser(player, level);

            // Movement
            case TELEPORT_LOOK -> teleportLook(player, level);
            case SHULKER_TELEPORT -> shulkerTeleport(player, level);
            case INK_DASH -> inkDash(player, level);
            case RAM -> ram(player, level);
            case DOLPHIN_LEAP -> dolphinLeap(player, level);
            case TONGUE_GRAB -> tongueGrab(player, level);
            case DASH -> dash(player, level);
            case CHARGE -> charge(player, level);
            case POUNCE -> pounce(player, level);
            case HORSE_JUMP -> horseJump(player, level);
            case ROCKET_BOOST -> rocketBoost(player);
            case CAMEL_DASH -> camelDash(player, level);

            // Defensive
            case SHELL_DEFENSE -> shellDefense(player, level);
            case PLAY_DEAD -> playDead(player, level);
            case CURL_UP -> curlUp(player, level);
            case INFEST_BLOCK -> SilverfishHideHandler.toggleHide(player, level);

            // Utility
            case SELF_HEAL -> selfHeal(player, level);
            case CLEAR_EFFECTS -> clearEffects(player, level);
            case ECHOLOCATION -> echolocation(player, level);
            case INVISIBILITY -> drinkInvisibility(player, level);

            default -> false;
        };
    }

    // ═══════════════════════════════════════════════════════════════
    //  PROJECTILE ABILITIES
    // ═══════════════════════════════════════════════════════════════

    /** Skeleton, Stray, Bogged, Pillager — shoots an arrow */
    private static boolean shootArrow(ServerPlayer player, ServerLevel level) {
        Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), null);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 1.0F);
        level.addFreshEntity(arrow);
        playSound(level, player, SoundEvents.ARROW_SHOOT);
        return true;
    }

    /** Blaze — shoots a burst of 3 fire charges */
    private static boolean shootFireCharge(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();

        double[][] offsets = {
                {0.0, 0.0},
                {-0.15, 0.03},
                {0.15, 0.03}
        };

        for (double[] offset : offsets) {
            Vec3 dir = look.add(right.scale(offset[0])).add(0, offset[1], 0).normalize();
            SmallFireball fireball = new SmallFireball(level, player, dir);
            fireball.setPos(player.getX() + dir.x * 1.2, player.getEyeY() - 0.1, player.getZ() + dir.z * 1.2);
            level.addFreshEntity(fireball);
        }

        playSound(level, player, SoundEvents.BLAZE_SHOOT);
        return true;
    }

    /** Ghast — shoots a large exploding fireball */
    private static boolean shootLargeFireball(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        LargeFireball fireball = new LargeFireball(level, player, look, 1);
        fireball.setPos(
                player.getX() + look.x * 2,
                player.getEyeY(),
                player.getZ() + look.z * 2);
        level.addFreshEntity(fireball);
        playSound(level, player, SoundEvents.GHAST_SHOOT);
        return true;
    }

    /** Ender Dragon — shoots an acid dragon fireball */
    private static boolean shootDragonFireball(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        DragonFireball fireball = new DragonFireball(level, player, look);
        fireball.setPos(
                player.getX() + look.x * 3,
                player.getEyeY(),
                player.getZ() + look.z * 3);
        level.addFreshEntity(fireball);
        playSound(level, player, SoundEvents.ENDER_DRAGON_SHOOT);
        return true;
    }

    /** Wither — shoots a wither skull */
    private static boolean shootWitherSkull(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        WitherSkull skull = new WitherSkull(level, player, look);
        skull.setPos(
                player.getX() + look.x,
                player.getEyeY(),
                player.getZ() + look.z);
        level.addFreshEntity(skull);
        playSound(level, player, SoundEvents.WITHER_SHOOT);
        return true;
    }

    /** Shulker — shoots a homing shulker bullet at the target you're looking at */
    private static boolean shootShulkerBullet(ServerPlayer player, ServerLevel level) {
        LivingEntity target = findLookTarget(player, 30.0);
        if (target == null) {
            player.displayClientMessage(Component.translatable("morphmod.ability.no_target"), true);
            return false;
        }
        ShulkerBullet bullet = new ShulkerBullet(level, player, target, Direction.Axis.Y);
        level.addFreshEntity(bullet);
        playSound(level, player, SoundEvents.SHULKER_SHOOT);
        return true;
    }

    /** Snow Golem — throws a snowball */
    private static boolean throwSnowball(ServerPlayer player, ServerLevel level) {
        Snowball snowball = new Snowball(level, player);
        snowball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
        level.addFreshEntity(snowball);
        playSound(level, player, SoundEvents.SNOWBALL_THROW);
        return true;
    }

    /** Witch — throws a splash potion of harming */
    /** Witch — throws a splash potion of poison */
    private static boolean throwPoisonPotion(ServerPlayer player, ServerLevel level) {
        ThrownPotion potion = new ThrownPotion(level, player);
        ItemStack potionStack = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.POISON);
        potion.setItem(potionStack);
        potion.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.65F, 1.0F);
        level.addFreshEntity(potion);
        playSound(level, player, SoundEvents.WITCH_THROW);
        return true;
    }

    /** Witch — throws a splash potion of instant healing */
    private static boolean throwHealingPotion(ServerPlayer player, ServerLevel level) {
        ThrownPotion potion = new ThrownPotion(level, player);
        ItemStack potionStack = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HEALING);
        potion.setItem(potionStack);
        potion.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.65F, 1.0F);
        level.addFreshEntity(potion);
        playSound(level, player, SoundEvents.WITCH_THROW);
        return true;
    }

    /** Drowned — throws a trident */
    private static boolean throwTrident(ServerPlayer player, ServerLevel level) {
        ThrownTrident trident = new ThrownTrident(level, player, new ItemStack(Items.TRIDENT));
        trident.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 1.0F);
        level.addFreshEntity(trident);
        playSound(level, player, SoundEvents.TRIDENT_THROW);
        return true;
    }

    /** Llama — spits */
    private static boolean spit(ServerPlayer player, ServerLevel level) {
        LlamaSpit llamaSpit = new LlamaSpit(EntityType.LLAMA_SPIT, level);
        llamaSpit.setOwner(player);
        llamaSpit.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        llamaSpit.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 10.0F);
        level.addFreshEntity(llamaSpit);
        playSound(level, player, SoundEvents.LLAMA_SPIT);
        return true;
    }

    /** Breeze — shoots a wind charge that knocks back entities */
    private static boolean windCharge(ServerPlayer player, ServerLevel level) {
        WindCharge charge = new WindCharge(player, level, player.getX(), player.getEyeY(), player.getZ());
        charge.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 0.0F);
        level.addFreshEntity(charge);
        playSound(level, player, SoundEvents.BREEZE_SHOOT);
        return true;
    }

    /** Spider, Cave Spider — shoots a web (slows target entity) */
    private static boolean shootWeb(ServerPlayer player, ServerLevel level) {
        LivingEntity target = findLookTarget(player, 15.0);
        if (target == null) {
            player.displayClientMessage(Component.translatable("morphmod.ability.no_target"), true);
            return false;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
        level.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                15, 0.5, 0.5, 0.5, 0.0);
        playSound(level, player, SoundEvents.SPIDER_AMBIENT);
        return true;
    }

    // ═══════════════════════════════════════════════════════════════
    //  AREA ATTACK ABILITIES
    // ═══════════════════════════════════════════════════════════════

    /** Creeper — explodes! Player is immune to their own blast. */
    private static boolean explode(ServerPlayer player, ServerLevel level) {
        float healthBefore = player.getHealth();

        level.explode(player,
                player.getX(), player.getY(), player.getZ(),
                3.0F, Level.ExplosionInteraction.MOB);

        // Creeper is immune to its own explosion
        player.setHealth(healthBefore);

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(),
                3, 0.5, 0.5, 0.5, 0.0);
        return true;
    }

    /** Warden — sonic boom: long-range beam that bypasses armor */
    private static boolean sonicBoom(ServerPlayer player, ServerLevel level) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB searchBox = AABB.ofSize(start, 30, 30, 30);

        boolean hitSomething = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e != player && e.isAlive())) {
            Vec3 toTarget = target.position().add(0, target.getBbHeight() / 2, 0).subtract(start);
            double dist = toTarget.length();
            if (dist > 15.0) continue;

            // Check if target is in the look direction (within ~45° cone)
            if (toTarget.normalize().dot(look) > 0.7) {
                target.hurt(player.damageSources().sonicBoom(player), 10.0F);
                Vec3 knockback = look.scale(2.5);
                target.push(knockback.x, 0.5, knockback.z);
                target.hurtMarked = true;
                hitSomething = true;
            }
        }

        // Sonic boom particle trail
        for (int i = 0; i < 15; i++) {
            Vec3 pos = start.add(look.scale(i));
            level.sendParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
        playSound(level, player, SoundEvents.WARDEN_SONIC_BOOM);
        return true;
    }

    /** Ravager — massive knockback roar */
    private static boolean roar(ServerPlayer player, ServerLevel level) {
        AABB area = player.getBoundingBox().inflate(8.0);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player)) {
            Vec3 knockback = entity.position().subtract(player.position()).normalize().scale(3.0);
            entity.push(knockback.x, 0.8, knockback.z);
            entity.hurtMarked = true;
            entity.hurt(player.damageSources().mobAttack(player), 4.0F);
        }
        level.sendParticles(ParticleTypes.EXPLOSION,
                player.getX(), player.getY() + 1, player.getZ(),
                15, 3.0, 1.0, 3.0, 0.0);
        playSound(level, player, SoundEvents.RAVAGER_ROAR);
        return true;
    }

    /** Iron Golem, Piglin Brute — powerful ground slam with AoE knockback */
    private static boolean groundSlam(ServerPlayer player, ServerLevel level) {
        AABB area = player.getBoundingBox().inflate(5.0, 2.0, 5.0);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player)) {
            Vec3 knockback = entity.position().subtract(player.position()).normalize().scale(2.0);
            entity.push(knockback.x, 1.2, knockback.z);
            entity.hurtMarked = true;
            entity.hurt(player.damageSources().mobAttack(player), 8.0F);
        }
        level.sendParticles(ParticleTypes.CRIT,
                player.getX(), player.getY(), player.getZ(),
                40, 3.0, 0.5, 3.0, 0.1);
        playSound(level, player, SoundEvents.IRON_GOLEM_ATTACK);
        return true;
    }

    /** Evoker — summons a line of evoker fangs in front of the player */
    private static boolean summonFangs(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        float yRot = player.getYRot();

        for (int i = 0; i < 8; i++) {
            double distance = 1.5 + i * 1.2;
            double fx = player.getX() + look.x * distance;
            double fz = player.getZ() + look.z * distance;

            // Find ground level
            BlockPos groundPos = BlockPos.containing(fx, player.getY(), fz);
            for (int j = 0; j < 5; j++) {
                if (!level.getBlockState(groundPos).isAir()) break;
                groundPos = groundPos.below();
            }

            EvokerFangs fangs = new EvokerFangs(level,
                    fx, groundPos.getY() + 1.0, fz,
                    (float) Math.toRadians(yRot), i * 2, player);
            level.addFreshEntity(fangs);
        }
        playSound(level, player, SoundEvents.EVOKER_CAST_SPELL);
        return true;
    }

    /** Pufferfish — inflates and poisons all nearby entities */
    private static boolean puffUp(ServerPlayer player, ServerLevel level) {
        AABB area = player.getBoundingBox().inflate(3.0);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player)) {
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            entity.hurt(player.damageSources().mobAttack(player), 2.0F);
        }
        level.sendParticles(ParticleTypes.BUBBLE_POP,
                player.getX(), player.getY() + 0.5, player.getZ(),
                25, 2.0, 0.5, 2.0, 0.1);
        playSound(level, player, SoundEvents.PUFFER_FISH_BLOW_UP);
        return true;
    }

    /** Panda — sneezes, knocking back all nearby entities */
    private static boolean sneeze(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        AABB area = player.getBoundingBox().inflate(4.0);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player)) {
            Vec3 knockback = entity.position().subtract(player.position()).normalize().scale(1.5);
            entity.push(knockback.x, 0.5, knockback.z);
            entity.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.SNEEZE,
                player.getX() + look.x, player.getEyeY(), player.getZ() + look.z,
                20, 0.5, 0.3, 0.5, 0.1);
        playSound(level, player, SoundEvents.PANDA_SNEEZE);
        return true;
    }

    /** Guardian, Elder Guardian — fires a laser beam at the target */
    private static boolean laser(ServerPlayer player, ServerLevel level) {
        LivingEntity target = findLookTarget(player, 15.0);
        if (target == null) {
            player.displayClientMessage(Component.translatable("morphmod.ability.no_target"), true);
            return false;
        }

        target.hurt(player.damageSources().magic(), 8.0F);

        // Beam particles from player to target
        Vec3 start = player.getEyePosition();
        Vec3 end = target.position().add(0, target.getBbHeight() / 2, 0);
        Vec3 diff = end.subtract(start);
        int steps = (int) (diff.length() / 0.5);
        for (int i = 0; i <= steps; i++) {
            Vec3 pos = start.add(diff.normalize().scale(i * 0.5));
            level.sendParticles(ParticleTypes.BUBBLE, pos.x, pos.y, pos.z, 2, 0.1, 0.1, 0.1, 0);
        }
        playSound(level, player, SoundEvents.GUARDIAN_ATTACK);
        return true;
    }

    // ═══════════════════════════════════════════════════════════════
    //  MOVEMENT ABILITIES
    // ═══════════════════════════════════════════════════════════════

    /** Enderman — teleports to where the player is looking (50 block range) */
    private static boolean teleportLook(ServerPlayer player, ServerLevel level) {
        HitResult hitResult = player.pick(50.0, 0.0F, false);
        if (hitResult.getType() != HitResult.Type.BLOCK) return false;

        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos targetPos = blockHit.getBlockPos().relative(blockHit.getDirection());

        // Safety check: target must not be inside solid blocks
        if (!level.getBlockState(targetPos).isAir()
                || !level.getBlockState(targetPos.above()).isAir()) {
            return false;
        }

        double oldX = player.getX(), oldY = player.getY(), oldZ = player.getZ();

        player.teleportTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);

        // Particles and sound at both locations
        level.sendParticles(ParticleTypes.PORTAL, oldX, oldY + 1, oldZ, 50, 0.5, 1.0, 0.5, 0.5);
        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1, player.getZ(),
                50, 0.5, 1.0, 0.5, 0.5);
        playSound(level, player, SoundEvents.ENDERMAN_TELEPORT);
        return true;
    }

    /** Shulker — teleports to targeted block within 30 blocks */
    private static boolean shulkerTeleport(ServerPlayer player, ServerLevel level) {
        HitResult hit = player.pick(30.0, 0.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.literal("§cIşınlanmak için bir bloğa bakmalısın!"), true);
            return false;
        }

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos targetPos = blockHit.getBlockPos().relative(blockHit.getDirection());

        // Check if there is space for the player
        if (!level.getBlockState(targetPos).canBeReplaced() && !level.getBlockState(targetPos).isAir()) {
            return false;
        }

        double oldX = player.getX(), oldY = player.getY(), oldZ = player.getZ();

        player.teleportTo(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);

        playSound(level, player, SoundEvents.SHULKER_TELEPORT);
        level.playSound(null, oldX, oldY, oldZ, SoundEvents.SHULKER_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

        level.sendParticles(ParticleTypes.PORTAL, oldX, oldY + 0.5, oldZ, 20, 0.3, 0.3, 0.3, 0.1);
        level.sendParticles(ParticleTypes.PORTAL, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.1);

        return true;
    }

    /** Squid, Glow Squid — squirts ink (blinds nearby enemies) and dashes forward */
    private static boolean inkDash(ServerPlayer player, ServerLevel level) {
        MorphManager manager = MorphManager.get(player.server);
        MorphData data = manager.getMorphData(player.getUUID());
        boolean isGlow = data != null && data.getActiveMorph() != null && data.getActiveMorph().getPath().equals("glow_squid");

        // Blind nearby entities
        AABB area = player.getBoundingBox().inflate(5.0);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player)) {
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            if (isGlow) {
                entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            }
        }

        // Propel player forward
        Vec3 look = player.getLookAngle();
        player.push(look.x * 2.5, 0.3, look.z * 2.5);
        player.hurtMarked = true;

        if (isGlow) {
            level.sendParticles(ParticleTypes.GLOW_SQUID_INK,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    40, 1.5, 0.5, 1.5, 0.1);
            playSound(level, player, SoundEvents.GLOW_SQUID_SQUIRT);
        } else {
            level.sendParticles(ParticleTypes.SQUID_INK,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    40, 1.5, 0.5, 1.5, 0.1);
            playSound(level, player, SoundEvents.SQUID_SQUIRT);
        }
        return true;
    }

    /** Goat — rams forward, dealing massive knockback to entities in path */
    private static boolean ram(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        player.push(look.x * 3.0, 0.5, look.z * 3.0);
        player.hurtMarked = true;

        AABB ahead = player.getBoundingBox().move(look.scale(2.0)).inflate(1.5);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, ahead, e -> e != player)) {
            entity.hurt(player.damageSources().mobAttack(player), 6.0F);
            entity.push(look.x * 2.5, 0.8, look.z * 2.5);
            entity.hurtMarked = true;
        }
        playSound(level, player, SoundEvents.GOAT_RAM_IMPACT);
        return true;
    }

    /** Dolphin — leaps out of water with a powerful jump */
    private static boolean dolphinLeap(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        player.push(look.x * 2.0, 1.5, look.z * 2.0);
        player.hurtMarked = true;

        level.sendParticles(ParticleTypes.SPLASH,
                player.getX(), player.getY(), player.getZ(),
                25, 0.5, 0.0, 0.5, 0.5);
        playSound(level, player, SoundEvents.DOLPHIN_JUMP);
        return true;
    }

    /** Frog — grabs a target with its tongue and pulls it toward the player */
    private static boolean tongueGrab(ServerPlayer player, ServerLevel level) {
        LivingEntity target = findLookTarget(player, 10.0);
        if (target == null) {
            player.displayClientMessage(Component.translatable("morphmod.ability.no_target"), true);
            return false;
        }

        Vec3 pull = player.position().subtract(target.position()).normalize().scale(2.0);
        target.push(pull.x, 0.5, pull.z);
        target.hurtMarked = true;

        // Tongue trail particles
        Vec3 start = player.getEyePosition();
        Vec3 end = target.position().add(0, target.getBbHeight() / 2, 0);
        Vec3 diff = end.subtract(start);
        int steps = (int) (diff.length() / 0.5);
        for (int i = 0; i <= steps; i++) {
            Vec3 pos = start.add(diff.normalize().scale(i * 0.5));
            level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
        playSound(level, player, SoundEvents.FROG_TONGUE);
        return true;
    }

    /** Wolf, Fox (variant), Cat, Ocelot, Horse, Camel, Donkey, Mule, Silverfish — quick speed dash */
    private static boolean dash(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        player.push(look.x * 2.5, 0.2, look.z * 2.5);
        player.hurtMarked = true;

        level.sendParticles(ParticleTypes.CLOUD,
                player.getX(), player.getY(), player.getZ(),
                10, 0.3, 0.1, 0.3, 0.0);
        playSound(level, player, SoundEvents.WOLF_GROWL);
        return true;
    }

    /** Horse — powerful jump leap */
    private static boolean horseJump(ServerPlayer player, ServerLevel level) {
        horseJumpStartPositions.put(player.getUUID(), player.getY());
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.x * 0.8, 0.85, look.z * 0.8);
        player.hasImpulse = true;
        player.hurtMarked = true;
        playSound(level, player, SoundEvents.HORSE_JUMP);
        level.sendParticles(ParticleTypes.CLOUD,
                player.getX(), player.getY(), player.getZ(),
                15, 0.4, 0.1, 0.4, 0.05);
        return true;
    }

    /** Hoglin, Zoglin, Vindicator — charges forward dealing heavy damage */
    private static boolean charge(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        player.push(look.x * 4.0, 0.5, look.z * 4.0);
        player.hurtMarked = true;

        AABB ahead = player.getBoundingBox().move(look.scale(3.0)).inflate(2.0);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, ahead, e -> e != player)) {
            entity.hurt(player.damageSources().mobAttack(player), 8.0F);
            entity.push(look.x * 2.0, 1.0, look.z * 2.0);
            entity.hurtMarked = true;
        }
        playSound(level, player, SoundEvents.HOGLIN_ATTACK);
        return true;
    }

    /** Fox — leaps high into the air and strikes target mob */
    private static boolean pounce(ServerPlayer player, ServerLevel level) {
        LivingEntity target = findLookTarget(player, 12.0);
        Vec3 look = player.getLookAngle();

        if (target != null) {
            Vec3 diff = target.position().subtract(player.position());
            Vec3 horiz = new Vec3(diff.x, 0, diff.z).normalize();
            double dist = Math.min(diff.horizontalDistance(), 10.0);
            double speed = dist * 0.16 + 0.35;
            player.setDeltaMovement(horiz.x * speed, 0.72, horiz.z * speed);
            player.hasImpulse = true;
            player.hurtMarked = true;

            // Damage and strike target
            target.hurt(player.damageSources().mobAttack(player), 7.0F);
            target.push(horiz.x * 0.4, 0.2, horiz.z * 0.4);
            target.hurtMarked = true;

            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getEyeY(), target.getZ(),
                    15, 0.3, 0.3, 0.3, 0.15);
            playSound(level, player, SoundEvents.FOX_BITE);
            playSound(level, player, SoundEvents.FOX_SCREECH);
        } else {
            player.setDeltaMovement(look.x * 1.3, 0.72, look.z * 1.3);
            player.hasImpulse = true;
            player.hurtMarked = true;
            playSound(level, player, SoundEvents.FOX_SCREECH);
        }
        return true;
    }

    /** Phantom — silent rocket boost acceleration for Elytra flight (no rocket item, no firework sounds/particles) */
    private static boolean rocketBoost(ServerPlayer player) {
        if (!player.isFallFlying()) {
            player.startFallFlying();
        }
        Vec3 look = player.getLookAngle();
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.add(look.x * 1.5, look.y * 1.5, look.z * 1.5));
        player.hasImpulse = true;
        player.hurtMarked = true;
        return true;
    }

    // ═══════════════════════════════════════════════════════════════
    //  DEFENSIVE ABILITIES
    // ═══════════════════════════════════════════════════════════════

    /** Turtle — retreats into shell: high damage resistance but very slow */
    private static boolean shellDefense(ServerPlayer player, ServerLevel level) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 3, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3, false, true, true));
        playSound(level, player, SoundEvents.TURTLE_EGG_BREAK);
        return true;
    }

    /** Axolotl — plays dead: becomes invisible and regenerates */
    private static boolean playDead(ServerPlayer player, ServerLevel level) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 4, false, false, true));
        playSound(level, player, SoundEvents.AXOLOTL_HURT);
        return true;
    }

    /** Armadillo — curls up: extreme damage resistance */
    private static boolean curlUp(ServerPlayer player, ServerLevel level) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 4, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 4, false, true, true));
        playSound(level, player, SoundEvents.ARMADILLO_PEEK);
        return true;
    }

    // ═══════════════════════════════════════════════════════════════
    //  UTILITY ABILITIES
    // ═══════════════════════════════════════════════════════════════

    /** Mooshroom — heals self with mushroom stew */
    private static boolean selfHeal(ServerPlayer player, ServerLevel level) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0, false, true, true));
        level.sendParticles(ParticleTypes.HEART,
                player.getX(), player.getY() + 1, player.getZ(),
                8, 0.5, 0.5, 0.5, 0.0);
        playSound(level, player, SoundEvents.MOOSHROOM_MILK);
        return true;
    }

    /** Cow — clears all potion effects (like drinking milk) */
    private static boolean clearEffects(ServerPlayer player, ServerLevel level) {
        player.removeAllEffects();
        level.sendParticles(ParticleTypes.SPLASH,
                player.getX(), player.getY() + 1, player.getZ(),
                20, 0.5, 0.5, 0.5, 0.1);
        playSound(level, player, SoundEvents.GENERIC_DRINK);
        return true;
    }

    /** Bat — toggles night vision (echolocation) */
    private static boolean echolocation(ServerPlayer player, ServerLevel level) {
        if (player.hasEffect(MobEffects.NIGHT_VISION)) {
            player.removeEffect(MobEffects.NIGHT_VISION);
            player.displayClientMessage(Component.translatable("morphmod.ability.echolocation.off"), true);
        } else {
            player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, true));
            player.displayClientMessage(Component.translatable("morphmod.ability.echolocation.on"), true);
        }
        playSound(level, player, SoundEvents.BAT_TAKEOFF);
        return true;
    }

    /** Camel — dashes forward with legs kicking rapidly */
    private static boolean camelDash(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        Vec3 dashVec = new Vec3(look.x, 0.0, look.z).normalize().scale(2.2).add(0.0, 0.35, 0.0);
        player.setDeltaMovement(dashVec);
        player.hasImpulse = true;
        player.hurtMarked = true;
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                player.getX(), player.getY() + 0.2, player.getZ(),
                12, 0.4, 0.1, 0.4, 0.02);
        playSound(level, player, SoundEvents.CAMEL_DASH);
        return true;
    }

    /** Wandering Trader — drinks invisibility potion to hide */
    private static boolean drinkInvisibility(ServerPlayer player, ServerLevel level) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 200, 0, false, true, true));
        playSound(level, player, SoundEvents.WANDERING_TRADER_DRINK_POTION);
        return true;
    }

    // ═══════════════════════════════════════════════════════════════
    //  MELEE ATTACK EFFECTS (passive, applied on hit)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Applies passive melee effects when a morphed player attacks an entity.
     */
    private static void handleMeleeEffects(ServerPlayer player, LivingEntity target) {
        MorphManager manager = MorphManager.get(player.server);
        MorphData data = manager.getMorphData(player.getUUID());
        if (data == null || !data.isMorphActive()) return;

        ResourceLocation morphId = data.getActiveMorph();
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(morphId);
        if (entityType == null) return;

        Set<MorphAbility> abilities = MorphRegistry.getAbilities(entityType);

        if (abilities.contains(MorphAbility.WITHER_STRIKE)) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0));
        }
        if (abilities.contains(MorphAbility.HUNGER_STRIKE)) {
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 0));
        }
        if (abilities.contains(MorphAbility.POISON_STRIKE)) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 1));
        }
        if (abilities.contains(MorphAbility.SLOW_STRIKE)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Finds the living entity the player is looking at within the given range.
     */
    private static LivingEntity findLookTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        AABB searchBox = player.getBoundingBox()
                .expandTowards(player.getLookAngle().scale(range))
                .inflate(1.0);

        LivingEntity closest = null;
        double closestDist = range * range;

        for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(
                LivingEntity.class, searchBox, e -> e != player && e.isAlive())) {
            AABB entityBox = entity.getBoundingBox().inflate(0.3);
            Optional<Vec3> hit = entityBox.clip(start, end);
            if (hit.isPresent()) {
                double dist = start.distanceToSqr(hit.get());
                if (dist < closestDist) {
                    closest = entity;
                    closestDist = dist;
                }
            }
        }
        return closest;
    }

    private static void playSound(ServerLevel level, ServerPlayer player, Holder<SoundEvent> sound) {
        level.playSound(null, player.blockPosition(), sound.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void playSound(ServerLevel level, ServerPlayer player, SoundEvent sound) {
        level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // ─── Cooldown Management ─────────────────────────────────────

    private static boolean isOnCooldown(ServerPlayer player, ActiveAbility ability) {
        Map<ActiveAbility, Long> playerCooldowns = cooldowns.get(player.getUUID());
        if (playerCooldowns == null) return false;

        Long lastUse = playerCooldowns.get(ability);
        if (lastUse == null) return false;

        long currentTick = player.server.getTickCount();
        return (currentTick - lastUse) < ability.getCooldownTicks();
    }

    private static long getRemainingCooldown(ServerPlayer player, ActiveAbility ability) {
        Map<ActiveAbility, Long> playerCooldowns = cooldowns.get(player.getUUID());
        if (playerCooldowns == null) return 0;

        Long lastUse = playerCooldowns.get(ability);
        if (lastUse == null) return 0;

        long currentTick = player.server.getTickCount();
        long elapsed = currentTick - lastUse;
        return Math.max(0, ability.getCooldownTicks() - elapsed);
    }

    private static void setCooldown(ServerPlayer player, ActiveAbility ability) {
        cooldowns.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(ability, (long) player.server.getTickCount());
    }
}
