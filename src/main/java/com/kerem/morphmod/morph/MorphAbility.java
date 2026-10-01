package com.kerem.morphmod.morph;

/**
 * Passive abilities that are applied automatically every tick
 * or triggered on specific events (like melee attacks).
 */
public enum MorphAbility {
    // ═══ Movement Passives ═══
    FLIGHT("flight"),
    SLOW_FALLING("slow_falling"),
    SPEED_BOOST("speed_boost"),
    SWIM_SPEED("swim_speed"),
    JUMP_BOOST("jump_boost"),
    NO_FALL_DAMAGE("no_fall_damage"),
    WALL_CLIMBING("wall_climbing"),

    // ═══ Survival Passives ═══
    WATER_BREATHING("water_breathing"),
    FIRE_RESISTANCE("fire_resistance"),
    NIGHT_VISION("night_vision"),
    KNOCKBACK_RESIST("knockback_resist"),

    // ═══ Size ═══
    SMALL_SIZE("small_size"),

    // ═══ Debuffs ═══
    SUN_DAMAGE("sun_damage"),

    // ═══ Melee Strike Effects (applied when hitting entities) ═══
    WITHER_STRIKE("wither_strike"),
    HUNGER_STRIKE("hunger_strike"),
    POISON_STRIKE("poison_strike"),
    SLOW_STRIKE("slow_strike");

    private final String id;

    MorphAbility(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
