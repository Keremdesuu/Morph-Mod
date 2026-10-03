package com.kerem.morphmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * Client-side storage for morph data, smooth morph transitions,
 * dynamic third-person camera zoom, and mob-specific client states.
 */
public class ClientMorphData {
    private static final Set<ResourceLocation> collectedMorphs = new LinkedHashSet<>();
    private static ResourceLocation activeMorph = null;
    private static final Map<UUID, ResourceLocation> otherPlayerMorphs = new HashMap<>();
    private static final Map<ResourceLocation, Entity> fakeEntityCache = new HashMap<>();

    // ─── Morph Transition Animation (1-1.5s) ──────────────────────
    public static final int MAX_MORPH_TRANSITION_TICKS = 25;
    private static int morphTransitionTicks = 0;
    private static ResourceLocation previousMorph = null;

    // ─── Dynamic Camera TPS Zoom ─────────────────────────────────
    private static float currentCameraDistance = 4.0F;
    private static float previousCameraDistance = 4.0F;
    private static float targetCameraDistance = 4.0F;

    // ─── Fox Pounce State ─────────────────────────────────────────
    private static int foxPounceTicks = 0;

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
        ResourceLocation newMorph = (morphId == null || morphId.isEmpty()) ? null : ResourceLocation.parse(morphId);
        if (!Objects.equals(activeMorph, newMorph)) {
            previousMorph = activeMorph;
            activeMorph = newMorph;
            morphTransitionTicks = MAX_MORPH_TRANSITION_TICKS;

            // Play transformation chime / magic sound
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.playSound(SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 0.7F, 1.25F);
            }
        }
    }

    public static ResourceLocation getActiveMorph() {
        return activeMorph;
    }

    public static int getMorphTransitionTicks() {
        return morphTransitionTicks;
    }

    public static float getMorphTransitionProgress(float partialTick) {
        if (morphTransitionTicks <= 0) return 1.0F;
        float remaining = Math.max(0.0F, morphTransitionTicks - partialTick);
        return Math.min(1.0F, Math.max(0.0F, 1.0F - (remaining / (float) MAX_MORPH_TRANSITION_TICKS)));
    }

    public static ResourceLocation getPreviousMorph() {
        return previousMorph;
    }

    // ─── Silverfish Hidden State ─────────────────────────────────

    private static boolean silverfishHidden = false;

    public static void setSilverfishHidden(boolean hidden) {
        silverfishHidden = hidden;
    }

    public static boolean isSilverfishHidden() {
        return silverfishHidden;
    }

    // ─── Fox Pounce Handling ─────────────────────────────────────

    public static void triggerFoxPounce() {
        foxPounceTicks = 35;
    }

    public static boolean isFoxPouncing() {
        return foxPounceTicks > 0;
    }

    public static void tickFoxPounce(Player player) {
        if (foxPounceTicks > 0) {
            foxPounceTicks--;
            if (player != null && player.onGround() && foxPounceTicks < 28) {
                foxPounceTicks = 0;
            }
        }
    }

    // ─── Evoker Spell Casting ────────────────────────────────────

    private static int evokerSpellTicks = 0;

    public static void triggerEvokerSpell() {
        evokerSpellTicks = 40;
    }

    public static boolean isEvokerCasting() {
        return evokerSpellTicks > 0;
    }

    // ─── Pufferfish Puff State ───────────────────────────────────

    private static int pufferPuffTicks = 0;

    public static void triggerPufferPuff() {
        pufferPuffTicks = 100;
    }

    public static boolean isPufferPuffed() {
        return pufferPuffTicks > 0;
    }

    // ─── Camel Dash State ────────────────────────────────────────
    private static int camelDashTicks = 0;

    public static void triggerCamelDash() {
        camelDashTicks = 28;
    }

    public static boolean isCamelDashing() {
        return camelDashTicks > 0;
    }

    // ─── Armadillo Roll State ────────────────────────────────────
    private static int armadilloRollTicks = 0;

    public static void triggerArmadilloRoll() {
        armadilloRollTicks = 80;
    }

    public static boolean isArmadilloRolling() {
        return armadilloRollTicks > 0;
    }

    // ─── Axolotl Play Dead State ─────────────────────────────────
    private static int axolotlPlayDeadTicks = 0;

    public static void triggerAxolotlPlayDead() {
        axolotlPlayDeadTicks = 100;
    }

    public static boolean isAxolotlPlayingDead() {
        return axolotlPlayDeadTicks > 0;
    }

    // ─── Dynamic Camera Distance Calculation ─────────────────────

    public static float getIdealCameraDistance(ResourceLocation morphId) {
        if (morphId == null) {
            return 4.0F; // default player
        }
        String path = morphId.getPath();
        switch (path) {
            case "ender_dragon": return 15.0F;
            case "giant": return 12.0F;
            case "ghast": return 9.0F;
            case "wither": return 6.5F;
            case "warden": return 6.0F;
            case "iron_golem", "ravager", "elder_guardian": return 5.5F;
            case "horse", "donkey", "mule", "camel", "sniffer", "skeleton_horse", "zombie_horse", "trader_llama", "llama": return 5.0F;
            case "silverfish", "endermite", "bee", "bat", "tadpole", "frog", "allay": return 2.4F;
            case "cod", "salmon", "pufferfish", "tropical_fish": return 2.8F;
            case "chicken", "rabbit", "cat", "ocelot", "parrot", "vex": return 3.0F;
            case "wandering_trader": return 4.0F;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(morphId);
        if (type != null) {
            float width = type.getDimensions().width();
            float height = type.getDimensions().height();
            float maxDim = Math.max(width, height);
            float dist = 4.0F + (maxDim - 1.8F) * 0.85F;
            return Math.clamp(dist, 2.5F, 16.0F);
        }
        return 4.0F;
    }

    public static void tickCameraAndTransition() {
        // Update camera distances
        previousCameraDistance = currentCameraDistance;
        targetCameraDistance = getIdealCameraDistance(activeMorph);
        currentCameraDistance = Mth.lerp(0.12F, currentCameraDistance, targetCameraDistance);

        // Update morph transition particles
        if (morphTransitionTicks > 0) {
            morphTransitionTicks--;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                double px = mc.player.getX();
                double py = mc.player.getY();
                double pz = mc.player.getZ();
                float height = mc.player.getBbHeight();
                for (int i = 0; i < 5; i++) {
                    double ox = (mc.level.random.nextDouble() - 0.5) * 1.2;
                    double oy = mc.level.random.nextDouble() * height;
                    double oz = (mc.level.random.nextDouble() - 0.5) * 1.2;
                    mc.level.addParticle(
                            ParticleTypes.PORTAL,
                            px + ox, py + oy, pz + oz,
                            (mc.level.random.nextDouble() - 0.5) * 0.5,
                            (mc.level.random.nextDouble() - 0.5) * 0.5,
                            (mc.level.random.nextDouble() - 0.5) * 0.5
                    );
                    mc.level.addParticle(
                            ParticleTypes.REVERSE_PORTAL,
                            px + ox * 0.5, py + oy, pz + oz * 0.5,
                            0, 0.05, 0
                    );
                }
            }
        }

        if (evokerSpellTicks > 0) {
            evokerSpellTicks--;
        }

        if (pufferPuffTicks > 0) {
            pufferPuffTicks--;
        }

        if (camelDashTicks > 0) {
            camelDashTicks--;
        }

        if (armadilloRollTicks > 0) {
            armadilloRollTicks--;
        }

        if (axolotlPlayDeadTicks > 0) {
            axolotlPlayDeadTicks--;
        }
    }

    public static float getSmoothCameraDistance(float partialTick) {
        return Mth.lerp(partialTick, previousCameraDistance, currentCameraDistance);
    }

    // ─── Other Players' Active Morphs ────────────────────────────

    public static void setOtherPlayerMorph(UUID playerId, String morphId) {
        if (morphId == null || morphId.isEmpty()) {
            otherPlayerMorphs.remove(playerId);
        } else {
            otherPlayerMorphs.put(playerId, ResourceLocation.parse(morphId));
        }
    }

    public static ResourceLocation getActiveMorph(UUID playerId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(playerId)) {
            return activeMorph;
        }
        return otherPlayerMorphs.get(playerId);
    }

    // ─── Fake Entity Cache (for rendering) ───────────────────────

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

    public static void clearCache() {
        fakeEntityCache.clear();
    }

    public static void reset() {
        collectedMorphs.clear();
        activeMorph = null;
        previousMorph = null;
        morphTransitionTicks = 0;
        currentCameraDistance = 4.0F;
        previousCameraDistance = 4.0F;
        targetCameraDistance = 4.0F;
        foxPounceTicks = 0;
        otherPlayerMorphs.clear();
        fakeEntityCache.clear();
        silverfishHidden = false;
    }
}
