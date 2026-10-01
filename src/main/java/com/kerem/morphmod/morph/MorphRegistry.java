package com.kerem.morphmod.morph;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Static registry that maps EntityTypes to their morph properties:
 * health, passive abilities, and active ability.
 * Initialized once at mod startup.
 */
public class MorphRegistry {
    private static final Map<EntityType<?>, MorphEntry> REGISTRY = new HashMap<>();

    public static void init() {
        // ═══════════════════════════════════════════════════════════
        //  PASSIVE MOBS
        // ═══════════════════════════════════════════════════════════
        register(EntityType.CHICKEN, 4, ActiveAbility.NONE,
                MorphAbility.SLOW_FALLING, MorphAbility.NO_FALL_DAMAGE);
        register(EntityType.BAT, 6, ActiveAbility.ECHOLOCATION,
                MorphAbility.FLIGHT, MorphAbility.SMALL_SIZE);
        register(EntityType.CAT, 10, ActiveAbility.DASH,
                MorphAbility.SPEED_BOOST, MorphAbility.NO_FALL_DAMAGE);
        register(EntityType.WOLF, 8, ActiveAbility.DASH,
                MorphAbility.SPEED_BOOST);
        register(EntityType.HORSE, 30, ActiveAbility.DASH);
        register(EntityType.DONKEY, 15, ActiveAbility.DASH);
        register(EntityType.MULE, 15, ActiveAbility.DASH);
        register(EntityType.PIG, 10, ActiveAbility.NONE);
        register(EntityType.COW, 10, ActiveAbility.CLEAR_EFFECTS);
        register(EntityType.SHEEP, 8, ActiveAbility.NONE);
        register(EntityType.RABBIT, 3, ActiveAbility.NONE,
                MorphAbility.JUMP_BOOST);
        register(EntityType.FOX, 10, ActiveAbility.POUNCE,
                MorphAbility.SPEED_BOOST);
        register(EntityType.OCELOT, 10, ActiveAbility.DASH,
                MorphAbility.SPEED_BOOST);
        register(EntityType.PARROT, 6, ActiveAbility.NONE,
                MorphAbility.FLIGHT);
        register(EntityType.GOAT, 10, ActiveAbility.RAM,
                MorphAbility.JUMP_BOOST, MorphAbility.NO_FALL_DAMAGE);
        register(EntityType.FROG, 10, ActiveAbility.TONGUE_GRAB,
                MorphAbility.JUMP_BOOST);
        register(EntityType.CAMEL, 32, ActiveAbility.DASH);
        register(EntityType.SNIFFER, 14, ActiveAbility.NONE);
        register(EntityType.ARMADILLO, 12, ActiveAbility.CURL_UP);
        register(EntityType.LLAMA, 22, ActiveAbility.SPIT);
        register(EntityType.MOOSHROOM, 10, ActiveAbility.SELF_HEAL);
        register(EntityType.PANDA, 20, ActiveAbility.SNEEZE);
        register(EntityType.POLAR_BEAR, 30, ActiveAbility.GROUND_SLAM);
        register(EntityType.BEE, 10, ActiveAbility.NONE,
                MorphAbility.FLIGHT, MorphAbility.SMALL_SIZE, MorphAbility.POISON_STRIKE);

        // ═══════════════════════════════════════════════════════════
        //  AQUATIC MOBS
        // ═══════════════════════════════════════════════════════════
        register(EntityType.DOLPHIN, 10, ActiveAbility.DOLPHIN_LEAP,
                MorphAbility.WATER_BREATHING, MorphAbility.SWIM_SPEED);
        register(EntityType.SQUID, 10, ActiveAbility.INK_DASH,
                MorphAbility.WATER_BREATHING);
        register(EntityType.GLOW_SQUID, 10, ActiveAbility.INK_DASH,
                MorphAbility.WATER_BREATHING);
        register(EntityType.COD, 3, ActiveAbility.NONE,
                MorphAbility.WATER_BREATHING, MorphAbility.SMALL_SIZE);
        register(EntityType.SALMON, 3, ActiveAbility.NONE,
                MorphAbility.WATER_BREATHING, MorphAbility.SMALL_SIZE);
        register(EntityType.PUFFERFISH, 3, ActiveAbility.PUFF_UP,
                MorphAbility.WATER_BREATHING);
        register(EntityType.TROPICAL_FISH, 3, ActiveAbility.NONE,
                MorphAbility.WATER_BREATHING, MorphAbility.SMALL_SIZE);
        register(EntityType.TURTLE, 30, ActiveAbility.SHELL_DEFENSE,
                MorphAbility.WATER_BREATHING);
        register(EntityType.AXOLOTL, 14, ActiveAbility.PLAY_DEAD,
                MorphAbility.WATER_BREATHING);
        register(EntityType.GUARDIAN, 30, ActiveAbility.LASER,
                MorphAbility.WATER_BREATHING);
        register(EntityType.ELDER_GUARDIAN, 80, ActiveAbility.LASER,
                MorphAbility.WATER_BREATHING);
        register(EntityType.TADPOLE, 6, ActiveAbility.NONE,
                MorphAbility.WATER_BREATHING, MorphAbility.SMALL_SIZE);

        // ═══════════════════════════════════════════════════════════
        //  HOSTILE MOBS
        // ═══════════════════════════════════════════════════════════
        register(EntityType.ZOMBIE, 20, ActiveAbility.NONE,
                MorphAbility.SUN_DAMAGE);
        register(EntityType.SKELETON, 20, ActiveAbility.SHOOT_ARROW,
                MorphAbility.SUN_DAMAGE);
        register(EntityType.CREEPER, 20, ActiveAbility.EXPLODE);
        register(EntityType.SPIDER, 16, ActiveAbility.SHOOT_WEB,
                MorphAbility.WALL_CLIMBING);
        register(EntityType.CAVE_SPIDER, 12, ActiveAbility.SHOOT_WEB,
                MorphAbility.WALL_CLIMBING, MorphAbility.POISON_STRIKE);
        register(EntityType.ENDERMAN, 40, ActiveAbility.TELEPORT_LOOK);
        register(EntityType.BLAZE, 20, ActiveAbility.SHOOT_FIRE_CHARGE,
                MorphAbility.FIRE_RESISTANCE, MorphAbility.FLIGHT);
        register(EntityType.GHAST, 10, ActiveAbility.SHOOT_LARGE_FIREBALL,
                MorphAbility.FLIGHT);
        register(EntityType.PHANTOM, 20, ActiveAbility.DASH,
                MorphAbility.FLIGHT);
        register(EntityType.WITHER_SKELETON, 20, ActiveAbility.NONE,
                MorphAbility.FIRE_RESISTANCE, MorphAbility.WITHER_STRIKE);
        register(EntityType.PIGLIN, 16, ActiveAbility.SHOOT_ARROW);
        register(EntityType.PIGLIN_BRUTE, 50, ActiveAbility.GROUND_SLAM,
                MorphAbility.KNOCKBACK_RESIST);
        register(EntityType.HOGLIN, 40, ActiveAbility.CHARGE);
        register(EntityType.ZOGLIN, 40, ActiveAbility.CHARGE);
        register(EntityType.ZOMBIFIED_PIGLIN, 20, ActiveAbility.NONE,
                MorphAbility.FIRE_RESISTANCE);
        register(EntityType.MAGMA_CUBE, 16, ActiveAbility.NONE,
                MorphAbility.FIRE_RESISTANCE);
        register(EntityType.SLIME, 16, ActiveAbility.NONE,
                MorphAbility.JUMP_BOOST);
        register(EntityType.WITCH, 26, ActiveAbility.THROW_POTION);
        register(EntityType.PILLAGER, 24, ActiveAbility.SHOOT_ARROW);
        register(EntityType.VINDICATOR, 24, ActiveAbility.CHARGE);
        register(EntityType.RAVAGER, 100, ActiveAbility.ROAR,
                MorphAbility.KNOCKBACK_RESIST);
        register(EntityType.VEX, 14, ActiveAbility.DASH,
                MorphAbility.FLIGHT, MorphAbility.SMALL_SIZE);
        register(EntityType.EVOKER, 24, ActiveAbility.SUMMON_FANGS);
        register(EntityType.DROWNED, 20, ActiveAbility.THROW_TRIDENT,
                MorphAbility.WATER_BREATHING, MorphAbility.SUN_DAMAGE);
        register(EntityType.HUSK, 20, ActiveAbility.NONE,
                MorphAbility.HUNGER_STRIKE);
        register(EntityType.STRAY, 20, ActiveAbility.SHOOT_ARROW,
                MorphAbility.SUN_DAMAGE, MorphAbility.SLOW_STRIKE);
        register(EntityType.SHULKER, 30, ActiveAbility.SHULKER_BULLET);
        register(EntityType.SILVERFISH, 8, ActiveAbility.DASH,
                MorphAbility.SMALL_SIZE);
        register(EntityType.ENDERMITE, 8, ActiveAbility.NONE,
                MorphAbility.SMALL_SIZE);
        register(EntityType.WARDEN, 500, ActiveAbility.SONIC_BOOM,
                MorphAbility.KNOCKBACK_RESIST);
        register(EntityType.BREEZE, 30, ActiveAbility.WIND_CHARGE,
                MorphAbility.JUMP_BOOST);
        register(EntityType.BOGGED, 16, ActiveAbility.SHOOT_ARROW,
                MorphAbility.SUN_DAMAGE, MorphAbility.POISON_STRIKE);
        register(EntityType.ZOMBIE_VILLAGER, 20, ActiveAbility.NONE,
                MorphAbility.SUN_DAMAGE);

        // ═══════════════════════════════════════════════════════════
        //  NETHER MOBS
        // ═══════════════════════════════════════════════════════════
        register(EntityType.STRIDER, 20, ActiveAbility.NONE,
                MorphAbility.FIRE_RESISTANCE);

        // ═══════════════════════════════════════════════════════════
        //  GOLEMS / UTILITY
        // ═══════════════════════════════════════════════════════════
        register(EntityType.IRON_GOLEM, 100, ActiveAbility.GROUND_SLAM,
                MorphAbility.KNOCKBACK_RESIST);
        register(EntityType.SNOW_GOLEM, 4, ActiveAbility.THROW_SNOWBALL);

        // ═══════════════════════════════════════════════════════════
        //  BOSSES
        // ═══════════════════════════════════════════════════════════
        register(EntityType.ENDER_DRAGON, 200, ActiveAbility.DRAGON_FIREBALL,
                MorphAbility.FLIGHT);
        register(EntityType.WITHER, 300, ActiveAbility.WITHER_SKULL,
                MorphAbility.FLIGHT, MorphAbility.FIRE_RESISTANCE);
    }

    private static void register(EntityType<?> type, int health, ActiveAbility activeAbility,
                                 MorphAbility... passiveAbilities) {
        REGISTRY.put(type, new MorphEntry(health, Set.of(passiveAbilities), activeAbility));
    }

    /**
     * Gets the morph entry for a given entity type.
     */
    public static MorphEntry getEntry(EntityType<?> type) {
        return REGISTRY.get(type);
    }

    /**
     * Gets the morph entry by ResourceLocation entity ID.
     */
    public static MorphEntry getEntryByLocation(ResourceLocation id) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);
        return type != null ? REGISTRY.get(type) : null;
    }

    public static boolean isRegistered(EntityType<?> type) {
        return REGISTRY.containsKey(type);
    }

    public static int getHealth(EntityType<?> type) {
        MorphEntry entry = REGISTRY.get(type);
        return entry != null ? entry.health() : 20;
    }

    public static Set<MorphAbility> getAbilities(EntityType<?> type) {
        MorphEntry entry = REGISTRY.get(type);
        return entry != null ? entry.passiveAbilities() : Set.of();
    }

    public static ActiveAbility getActiveAbility(EntityType<?> type) {
        MorphEntry entry = REGISTRY.get(type);
        return entry != null ? entry.activeAbility() : ActiveAbility.NONE;
    }

    /**
     * A morph registration entry containing health, passive abilities, and active ability.
     */
    public record MorphEntry(int health, Set<MorphAbility> passiveAbilities, ActiveAbility activeAbility) {}
}
