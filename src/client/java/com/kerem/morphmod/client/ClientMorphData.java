package com.kerem.morphmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.*;

/**
 * Client-side storage for morph data.
 * Tracks the local player's collected morphs, their active morph,
 * and all other players' active morphs for rendering.
 */
public class ClientMorphData {
    private static final Set<ResourceLocation> collectedMorphs = new LinkedHashSet<>();
    private static ResourceLocation activeMorph = null;
    private static final Map<UUID, ResourceLocation> otherPlayerMorphs = new HashMap<>();
    private static final Map<ResourceLocation, Entity> fakeEntityCache = new HashMap<>();

    // ─── Collection Management ───────────────────────────────────

    public static void setCollectedMorphs(List<String> morphIds) {
        collectedMorphs.clear();
        for (String id : morphIds) {
            collectedMorphs.add(ResourceLocation.parse(id));
        }
    }

    public static Set<ResourceLocation> getCollectedMorphs() {
        return Collections.unmodifiableSet(collectedMorphs);
    }

    // ─── Active Morph (Local Player) ─────────────────────────────

    public static void setActiveMorph(String morphId) {
        activeMorph = (morphId == null || morphId.isEmpty()) ? null : ResourceLocation.parse(morphId);
    }

    public static ResourceLocation getActiveMorph() {
        return activeMorph;
    }

    // ─── Other Players' Active Morphs ────────────────────────────

    public static void setOtherPlayerMorph(UUID playerId, String morphId) {
        if (morphId == null || morphId.isEmpty()) {
            otherPlayerMorphs.remove(playerId);
        } else {
            otherPlayerMorphs.put(playerId, ResourceLocation.parse(morphId));
        }
    }

    /**
     * Gets the active morph for any player (local or remote).
     * Returns null if the player is not morphed.
     */
    public static ResourceLocation getActiveMorph(UUID playerId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(playerId)) {
            return activeMorph;
        }
        return otherPlayerMorphs.get(playerId);
    }

    // ─── Fake Entity Cache (for rendering) ───────────────────────

    /**
     * Gets or creates a fake entity for rendering purposes.
     * Entities are cached to avoid creating new instances every frame.
     */
    public static Entity getOrCreateFakeEntity(ResourceLocation morphId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        return fakeEntityCache.computeIfAbsent(morphId, id -> {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
            if (type != null) {
                return type.create(mc.level);
            }
            return null;
        });
    }

    /**
     * Clears the fake entity cache (e.g., when changing dimensions).
     */
    public static void clearCache() {
        fakeEntityCache.clear();
    }

    /**
     * Full reset of all client-side morph data (e.g., on disconnect).
     */
    public static void reset() {
        collectedMorphs.clear();
        activeMorph = null;
        otherPlayerMorphs.clear();
        fakeEntityCache.clear();
    }
}
