package com.kerem.morphmod;

import com.kerem.morphmod.event.EntityKillHandler;
import com.kerem.morphmod.event.MorphAbilityHandler;
import com.kerem.morphmod.morph.MorphData;
import com.kerem.morphmod.morph.MorphManager;
import com.kerem.morphmod.morph.MorphRegistry;
import com.kerem.morphmod.network.MorphPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MorphMod implements ModInitializer {
    public static final String MOD_ID = "morphmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Morph Mod initializing...");

        // Initialize systems
        MorphRegistry.init();
        MorphPackets.registerPackets();
        EntityKillHandler.register();
        MorphAbilityHandler.register();
        com.kerem.morphmod.event.ActiveAbilityHandler.register();

        // Register commands
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> com.kerem.morphmod.command.MorphCommand.register(dispatcher)
        );

        // Sync morph data when player joins
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            server.execute(() -> {
                ServerPlayer player = handler.player;
                MorphPackets.syncAllMorphsOnJoin(player);

                // Reapply morph abilities if the player was morphed before disconnecting
                MorphManager manager = MorphManager.get(server);
                MorphData data = manager.getMorphData(player.getUUID());
                if (data != null && data.isMorphActive()) {
                    ResourceLocation morphId = data.getActiveMorph();
                    EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(morphId);
                    if (entityType != null) {
                        MorphAbilityHandler.applyMorphHealth(player, entityType);
                        MorphAbilityHandler.applyMorphSpeed(player, entityType);
                    }
                }
            });
        });

        // Reset to normal form on death and respawn
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            newPlayer.server.execute(() -> {
                MorphManager manager = MorphManager.get(newPlayer.server);
                manager.clearActiveMorph(newPlayer.getUUID());
                MorphAbilityHandler.resetAbilities(newPlayer);
                newPlayer.refreshDimensions();
                MorphPackets.broadcastMorphUpdate(newPlayer, "");
                MorphPackets.syncMorphData(newPlayer);
            });
        });

        LOGGER.info("Morph Mod initialized successfully!");
    }
}
