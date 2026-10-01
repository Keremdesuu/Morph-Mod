package com.kerem.morphmod.client;

import com.kerem.morphmod.network.ActiveAbilityPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Registers and handles key bindings for the Morph mod.
 * - Morph menu key: Minus (-) key.
 * - Active ability key: G key.
 */
public class MorphKeyBindings {
    private static KeyMapping morphMenuKey;
    private static KeyMapping activeAbilityKey;

    public static void register() {
        // Register Morph menu key binding
        morphMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.morphmod.open_menu",           // Translation key
                InputConstants.Type.KEYSYM,          // Input type
                GLFW.GLFW_KEY_MINUS,                 // Default key: -
                "category.morphmod.general"          // Category translation key
        ));

        // Register Active ability key binding
        activeAbilityKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.morphmod.active_ability",       // Translation key
                InputConstants.Type.KEYSYM,          // Input type
                GLFW.GLFW_KEY_G,                     // Default key: G
                "category.morphmod.general"          // Category translation key
        ));

        // Check for key presses each client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (morphMenuKey.consumeClick()) {
                // Only open menu if player exists and no other screen is open
                if (client.player != null && client.screen == null) {
                    Minecraft.getInstance().setScreen(new MorphScreen());
                }
            }

            while (activeAbilityKey.consumeClick()) {
                // Trigger active ability
                if (client.player != null && client.screen == null) {
                    ClientPlayNetworking.send(new ActiveAbilityPayload());
                }
            }
        });
    }
}
