package com.kerem.morphmod.morph;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Stores a single player's morph collection and active morph state.
 * Serializable to NBT for world save persistence.
 */
public class MorphData {
    private final Set<ResourceLocation> collectedMorphs = new LinkedHashSet<>();
    private ResourceLocation activeMorph = null;

    /**
     * Adds a new morph to the collection.
     * @return true if the morph was new (not already collected)
     */
    public boolean addMorph(ResourceLocation morphId) {
        return collectedMorphs.add(morphId);
    }

    public boolean hasMorph(ResourceLocation morphId) {
        return collectedMorphs.contains(morphId);
    }

    public Set<ResourceLocation> getCollectedMorphs() {
        return Collections.unmodifiableSet(collectedMorphs);
    }

    public ResourceLocation getActiveMorph() {
        return activeMorph;
    }

    public void setActiveMorph(ResourceLocation morphId) {
        this.activeMorph = morphId;
    }

    public void clearActiveMorph() {
        this.activeMorph = null;
    }

    public boolean isMorphActive() {
        return activeMorph != null;
    }

    /**
     * Serializes this morph data to an NBT CompoundTag.
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        ListTag morphList = new ListTag();
        for (ResourceLocation id : collectedMorphs) {
            morphList.add(StringTag.valueOf(id.toString()));
        }
        tag.put("CollectedMorphs", morphList);

        if (activeMorph != null) {
            tag.putString("ActiveMorph", activeMorph.toString());
        }

        return tag;
    }

    /**
     * Deserializes morph data from an NBT CompoundTag.
     */
    public static MorphData load(CompoundTag tag) {
        MorphData data = new MorphData();

        ListTag morphList = tag.getList("CollectedMorphs", Tag.TAG_STRING);
        for (int i = 0; i < morphList.size(); i++) {
            data.collectedMorphs.add(ResourceLocation.parse(morphList.getString(i)));
        }

        if (tag.contains("ActiveMorph")) {
            data.activeMorph = ResourceLocation.parse(tag.getString("ActiveMorph"));
        }

        return data;
    }
}
