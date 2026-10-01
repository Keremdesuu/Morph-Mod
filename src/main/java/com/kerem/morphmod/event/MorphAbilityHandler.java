package com.kerem.morphmod.event;

import com.kerem.morphmod.morph.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

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

        for (MorphAbility ability : abilities) {
            switch (ability) {
                case FLIGHT -> {}
                case WATER_BREATHING -> {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.WATER_BREATHING, 60, 0, false, false, true));
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
     * Sets the player's max health to match the morphed mob's health.
     */
    public static void applyMorphHealth(ServerPlayer player, EntityType<?> entityType) {
        int morphHealth = MorphRegistry.getHealth(entityType);
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(morphHealth);
            if (player.getHealth() > morphHealth) {
                player.setHealth(morphHealth);
            }
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

        // Reset max health to default (20 HP = 10 hearts)
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(20.0);
            if (player.getHealth() > 20.0f) {
                player.setHealth(20.0f);
            }
        }

        // Reset knockback resistance
        AttributeInstance knockbackAttr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackAttr != null) {
            knockbackAttr.setBaseValue(0.0);
        }

        // Remove all morph-related potion effects
        player.removeEffect(MobEffects.WATER_BREATHING);
        player.removeEffect(MobEffects.FIRE_RESISTANCE);
        player.removeEffect(MobEffects.SLOW_FALLING);
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        player.removeEffect(MobEffects.DOLPHINS_GRACE);
        player.removeEffect(MobEffects.JUMP);
        player.removeEffect(MobEffects.NIGHT_VISION);
    }
}
