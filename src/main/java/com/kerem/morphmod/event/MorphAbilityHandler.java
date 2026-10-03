package com.kerem.morphmod.event;

import com.kerem.morphmod.morph.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Applies passive morph abilities to players every server tick.
 * Handles: flight, water breathing, fire resistance, effects, knockback resist, etc.
 */
public class MorphAbilityHandler {

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                MorphManager manager = MorphManager.get(server);
                MorphData data = manager.getMorphData(player.getUUID());

                if (data != null && data.isMorphActive()) {
                    ResourceLocation morphId = data.getActiveMorph();
                    EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(morphId);

                    if (entityType != null) {
                        applyAbilities(player, entityType);
                        SilverfishHideHandler.keepLocked(player);
                    }
                }
            }
        });
    }

    /**
     * Applies all passive abilities associated with the morphed entity type.
     * Called every tick for morphed players.
     */
    public static void applyAbilities(ServerPlayer player, EntityType<?> entityType) {
        Set<MorphAbility> abilities = MorphRegistry.getAbilities(entityType);

        // Flight: strictly manage flight so non-flying morphs never retain flight
        if (!player.isCreative() && !player.isSpectator()) {
            if (abilities.contains(MorphAbility.FLIGHT)) {
                if (!player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                }
            } else {
                if (player.getAbilities().mayfly || player.getAbilities().flying) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
            }
        }

        // Synchronize morph-specific movement speed
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        float targetSpeed = MorphRegistry.getSpeed(entityType);
        if (speedAttr != null && Math.abs(speedAttr.getBaseValue() - targetSpeed) > 0.001) {
            speedAttr.setBaseValue(targetSpeed);
        }

        // ─── Special Mob-Specific Mechanics ───

        // Vex noclip / pass through blocks
        if (entityType == EntityType.VEX) {
            player.noPhysics = true;
            player.resetFallDistance();
        }

        // Snow Golem trail and melting conditions
        if (entityType == EntityType.SNOW_GOLEM) {
            BlockPos pos = player.blockPosition();
            BlockState state = player.level().getBlockState(pos);
            if (state.isAir() && Blocks.SNOW.defaultBlockState().canSurvive(player.level(), pos)) {
                player.level().setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState());
            }

            boolean inWater = player.isInWaterRainOrBubble();
            boolean isMeltingBiome = player.level().getBiome(pos).is(net.minecraft.tags.BiomeTags.SNOW_GOLEM_MELTS);
            if (inWater || isMeltingBiome) {
                if (player.tickCount % 20 == 0) {
                    player.hurt(player.damageSources().onFire(), 2.0F);
                }
            }
        }

        // Blaze water & rain damage (takes drown damage every second in water or rain)
        if (entityType == EntityType.BLAZE) {
            if (player.isInWaterRainOrBubble()) {
                if (player.tickCount % 20 == 0) {
                    player.hurt(player.damageSources().drown(), 1.0F);
                }
            }
        }

        // Aquatic & Fish mechanics: Guardian, Elder Guardian, Cod, Tropical Fish, Salmon, Pufferfish, Tadpole, Squid, etc.
        boolean isSuffocatingAquatic = MorphRegistry.isSuffocatingOnLand(entityType);

        if (isSuffocatingAquatic) {
            if (player.isInWaterRainOrBubble()) {
                // Unlimited air supply under water without potion effect icon
                player.setAirSupply(player.getMaxAirSupply());
            } else {
                // Suffocate on land like real fish/squids
                int air = player.getAirSupply() - 3;
                if (air <= 0) {
                    player.setAirSupply(0);
                    if (player.tickCount % 20 == 0) {
                        player.hurt(player.damageSources().dryOut(), 2.0F);
                        if (entityType == EntityType.COD || entityType == EntityType.TROPICAL_FISH || entityType == EntityType.SALMON) {
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.FISH_SWIM, SoundSource.PLAYERS, 1.0F, 1.4F);
                        }
                    }
                } else {
                    player.setAirSupply(air);
                }
            }
        }

        for (MorphAbility ability : abilities) {
            switch (ability) {
                case FLIGHT -> {}
                case WATER_BREATHING -> {
                    // Handled natively via canBreatheUnderwater in LivingEntityMixin and ClientLivingEntityMixin
                    // No potion effect icon or particles cluttering the screen!
                }
                case FIRE_RESISTANCE -> {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.FIRE_RESISTANCE, 60, 0, false, false, true));
                }
                case SLOW_FALLING -> {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.SLOW_FALLING, 60, 0, false, false, true));
                }
                case SPEED_BOOST -> {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.MOVEMENT_SPEED, 60, 0, false, false, true));
                }
                case SWIM_SPEED -> {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.DOLPHINS_GRACE, 60, 0, false, false, true));
                }
                case JUMP_BOOST -> {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.JUMP, 60, 1, false, false, true));
                }
                case NIGHT_VISION -> {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.NIGHT_VISION, 300, 0, false, false, true));
                }
                case RESISTANCE -> {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.DAMAGE_RESISTANCE, 60, 0, false, false, false));
                }
                case STRENGTH -> {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.DAMAGE_BOOST, 60, 0, false, false, false));
                }
                case KNOCKBACK_RESIST -> {
                    AttributeInstance knockbackAttr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                    if (knockbackAttr != null && knockbackAttr.getBaseValue() < 0.8) {
                        knockbackAttr.setBaseValue(0.8);
                    }
                }
                case SUN_DAMAGE -> {
                    // Undead behavior: burn in direct sunlight without a helmet
                    if (player.level().isDay()
                            && !player.level().isRaining()
                            && player.level().canSeeSky(player.blockPosition())
                            && player.getInventory().armor.get(3).isEmpty()) {
                        player.igniteForSeconds(2);
                    }
                }
                case NO_FALL_DAMAGE -> {
                    player.resetFallDistance();
                }
                // WALL_CLIMBING → handled via LivingEntityMixin
                // WITHER_STRIKE, HUNGER_STRIKE, POISON_STRIKE, SLOW_STRIKE → handled via ActiveAbilityHandler melee events
                default -> {}
            }
        }
    }

    /**
     * Sets the player's max health to match the morphed mob's health,
     * maintaining the exact health percentage (e.g. 100% full stays 100% full, 65% stays 65%).
     */
    public static void applyMorphHealth(ServerPlayer player, EntityType<?> entityType) {
        int morphHealth = MorphRegistry.getHealth(entityType);
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            double oldMax = healthAttr.getValue();
            float currentHealth = player.getHealth();
            float ratio = oldMax > 0.0 ? (float) (currentHealth / oldMax) : 1.0F;
            ratio = Math.clamp(ratio, 0.0F, 1.0F);

            healthAttr.setBaseValue(morphHealth);
            player.setHealth(Math.max(1.0F, morphHealth * ratio));
        }
    }

    /**
     * Sets the player's movement speed to match the morphed mob's speed.
     */
    public static void applyMorphSpeed(ServerPlayer player, EntityType<?> entityType) {
        float speed = MorphRegistry.getSpeed(entityType);
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(speed);
        }
    }

    /**
     * Resets all morph-related abilities and effects when un-morphing.
     */
    public static void resetAbilities(ServerPlayer player) {
        // Reset flight (unless in creative/spectator)
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }

        // Reset max health to default (20 HP = 10 hearts) with percentage preserved
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            double oldMax = healthAttr.getValue();
            float currentHealth = player.getHealth();
            float ratio = oldMax > 0.0 ? (float) (currentHealth / oldMax) : 1.0F;
            ratio = Math.clamp(ratio, 0.0F, 1.0F);

            healthAttr.setBaseValue(20.0);
            player.setHealth(Math.max(1.0F, 20.0F * ratio));
        }

        // Reset movement speed to default (0.10)
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(0.10);
        }

        // Reset knockback resistance
        AttributeInstance knockbackAttr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackAttr != null) {
            knockbackAttr.setBaseValue(0.0);
        }

        // Silverfish emerge if hidden in a block
        SilverfishHideHandler.emerge(player, player.serverLevel());

        // Reset physics / noclip
        player.noPhysics = false;

        // Remove all morph-related potion effects
        player.removeEffect(MobEffects.WATER_BREATHING);
        player.removeEffect(MobEffects.FIRE_RESISTANCE);
        player.removeEffect(MobEffects.SLOW_FALLING);
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        player.removeEffect(MobEffects.DOLPHINS_GRACE);
        player.removeEffect(MobEffects.JUMP);
        player.removeEffect(MobEffects.NIGHT_VISION);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(MobEffects.DAMAGE_BOOST);
    }
}
