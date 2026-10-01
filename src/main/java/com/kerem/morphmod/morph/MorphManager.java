package com.kerem.morphmod.morph;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent world data manager for all players' morph collections.
 * Extends SavedData to automatically save/load with the world.
 */
public class MorphManager extends SavedData {
    private static final String DATA_NAME = "morphmod_morph_data";
    private final Map<UUID, MorphData> playerMorphs = new HashMap<>();

    public MorphManager() {
    }

    /**
     * Gets the MorphManager instance for the given server.
     * Uses the overworld's data storage for persistence.
     */
    public static MorphManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }

    public static SavedData.Factory<MorphManager> factory() {
        return new SavedData.Factory<>(
                MorphManager::new,
                MorphManager::load,
                null
        );
    }

    /**
     * Gets or creates morph data for a player.
     */
    public MorphData getOrCreateMorphData(UUID playerId) {
        return playerMorphs.computeIfAbsent(playerId, id -> new MorphData());
    }

    /**
     * Gets morph data for a player, or null if they have none.
     */
    public MorphData getMorphData(UUID playerId) {
        return playerMorphs.get(playerId);
    }

    /**
     * Adds a new morph to a player's collection.
     * @return true if the morph was newly collected
     */
    public boolean addMorph(UUID playerId, ResourceLocation morphId) {
        MorphData data = getOrCreateMorphData(playerId);
        boolean added = data.addMorph(morphId);
        if (added) {
            setDirty();
        }
        return added;
    }

    /**
     * Sets the active morph for a player.
     */
    public void setActiveMorph(UUID playerId, ResourceLocation morphId) {
        MorphData data = getOrCreateMorphData(playerId);
        data.setActiveMorph(morphId);
        setDirty();
    }

    /**
     * Clears the active morph (returns player to normal form).
     */
    public void clearActiveMorph(UUID playerId) {
        MorphData data = getOrCreateMorphData(playerId);
        data.clearActiveMorph();
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag playersTag = new CompoundTag();
        for (Map.Entry<UUID, MorphData> entry : playerMorphs.entrySet()) {
            playersTag.put(entry.getKey().toString(), entry.getValue().save());
        }
        tag.put("Players", playersTag);
        return tag;
    }

    public static MorphManager load(CompoundTag tag, HolderLookup.Provider registries) {
        MorphManager manager = new MorphManager();
        CompoundTag playersTag = tag.getCompound("Players");
        for (String key : playersTag.getAllKeys()) {
            UUID playerId = UUID.fromString(key);
            MorphData data = MorphData.load(playersTag.getCompound(key));
            manager.playerMorphs.put(playerId, data);
        }
        return manager;
    }
}
