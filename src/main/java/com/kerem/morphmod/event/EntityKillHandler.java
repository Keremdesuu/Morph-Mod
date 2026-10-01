package com.kerem.morphmod.event;

import com.kerem.morphmod.MorphMod;
import com.kerem.morphmod.morph.MorphManager;
import com.kerem.morphmod.network.MorphPackets;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

/**
 * Handles mob death events to collect morph souls.
 * When a player kills a mob for the first time, the mob's soul
 * is collected and the player gains the ability to morph into it.
 */
public class EntityKillHandler {

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            // Only trigger when a player kills the entity
            if (!(damageSource.getEntity() instanceof ServerPlayer player)) return;

            EntityType<?> entityType = entity.getType();

            // Don't allow morphing into other players
            if (entityType == EntityType.PLAYER) return;

            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            MorphManager manager = MorphManager.get(player.server);

            // Only add if this is a new morph
            if (manager.addMorph(player.getUUID(), entityId)) {
                MorphMod.LOGGER.info("Player {} collected morph soul: {}",
                        player.getName().getString(), entityId);

                // Spawn soul particle trail from dead entity to player
                spawnSoulParticles(player, entity.position().add(0, entity.getBbHeight() / 2, 0));

                // Notify the player with an action bar message
                String entityName = entityType.getDescription().getString();
                player.displayClientMessage(
                        Component.translatable("morphmod.soul_collected", entityName),
                        true
                );

                // Sync updated morph collection to the client
                MorphPackets.syncMorphData(player);
            }
        });
    }

    /**
     * Spawns a trail of soul particles from the dead entity to the player,
     * with a burst effect at the player's position.
     */
    private static void spawnSoulParticles(ServerPlayer player, Vec3 startPos) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Vec3 end = player.position().add(0, player.getBbHeight() / 2, 0);
        Vec3 direction = end.subtract(startPos);
        double distance = direction.length();
        Vec3 step = direction.normalize().scale(0.5);

        // Trail of soul particles from mob to player
        int steps = (int) (distance / 0.5);
        for (int i = 0; i <= steps; i++) {
            Vec3 pos = startPos.add(step.scale(i));
            serverLevel.sendParticles(
                    ParticleTypes.SOUL,
                    pos.x, pos.y, pos.z,
                    2,          // particle count
                    0.1, 0.1, 0.1,  // spread
                    0.02        // speed
            );
        }

        // Burst of soul fire flame particles around the player
        serverLevel.sendParticles(
                ParticleTypes.SOUL_FIRE_FLAME,
                player.getX(), player.getY() + 1, player.getZ(),
                20,             // particle count
                0.3, 0.5, 0.3,  // spread
                0.05            // speed
        );
    }
}
