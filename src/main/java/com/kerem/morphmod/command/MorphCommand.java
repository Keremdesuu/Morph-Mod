package com.kerem.morphmod.command;

import com.kerem.morphmod.morph.MorphData;
import com.kerem.morphmod.morph.MorphManager;
import com.kerem.morphmod.morph.MorphRegistry;
import com.kerem.morphmod.network.MorphPackets;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Admin commands for Morph Mod.
 * Supports:
 *   /morph <targets> all - Grants all morphs to specified player(s) (e.g. /morph @a all or /morph Steve all)
 *   /morph all - Grants all morphs to the executing player
 */
public class MorphCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("morph")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("all")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        return giveAllMorphs(context.getSource(), Collections.singleton(player));
                    })
                )
                .then(Commands.argument("targets", EntityArgument.players())
                    .then(Commands.literal("all")
                        .executes(context -> {
                            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
                            return giveAllMorphs(context.getSource(), players);
                        })
                    )
                )
        );
    }

    private static int giveAllMorphs(CommandSourceStack source, Collection<ServerPlayer> targets) {
        Set<ResourceLocation> allMorphs = MorphRegistry.getAllMorphIds();
        if (allMorphs.isEmpty()) {
            source.sendFailure(Component.literal("§cKayıtlı dönüşüm mobu bulunamadı!"));
            return 0;
        }

        MorphManager manager = MorphManager.get(source.getServer());
        int totalPlayers = 0;

        for (ServerPlayer player : targets) {
            MorphData data = manager.getOrCreateMorphData(player.getUUID());
            for (ResourceLocation id : allMorphs) {
                data.addMorph(id);
            }
            manager.setDirty();
            MorphPackets.syncMorphData(player);
            player.sendSystemMessage(Component.translatable(
                    "morphmod.command.all_success",
                    player.getName().getString(),
                    allMorphs.size()
            ));
            totalPlayers++;
        }

        if (targets.size() == 1) {
            ServerPlayer singlePlayer = targets.iterator().next();
            source.sendSuccess(() -> Component.translatable(
                    "morphmod.command.all_success",
                    singlePlayer.getName().getString(),
                    allMorphs.size()
            ), true);
        } else {
            source.sendSuccess(() -> Component.translatable(
                    "morphmod.command.all_multiple_success",
                    targets.size(),
                    allMorphs.size()
            ), true);
        }

        return totalPlayers;
    }
}
