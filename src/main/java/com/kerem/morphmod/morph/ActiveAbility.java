package com.kerem.morphmod.morph;

/**
 * Active abilities that are triggered by pressing a key (default: G).
 * Each ability has a unique cooldown in ticks (20 ticks = 1 second).
 */
public enum ActiveAbility {
    NONE("none", 0),

    // ═══ Projectile Attacks ═══
    SHOOT_ARROW("shoot_arrow", 15),
    SHOOT_FIRE_CHARGE("shoot_fire_charge", 30),
    SHOOT_LARGE_FIREBALL("shoot_large_fireball", 60),
    DRAGON_FIREBALL("dragon_fireball", 80),
    WITHER_SKULL("wither_skull", 40),
    SHULKER_BULLET("shulker_bullet", 40),
    THROW_SNOWBALL("throw_snowball", 8),
    THROW_POTION("throw_potion", 40),
    THROW_TRIDENT("throw_trident", 20),
    SPIT("spit", 15),
    WIND_CHARGE("wind_charge", 20),
    SHOOT_WEB("shoot_web", 20),

    // ═══ Area Attacks ═══
    EXPLODE("explode", 200),
    SONIC_BOOM("sonic_boom", 100),
    ROAR("roar", 80),
    GROUND_SLAM("ground_slam", 60),
    SUMMON_FANGS("summon_fangs", 60),
    PUFF_UP("puff_up", 60),
    SNEEZE("sneeze", 40),
    LASER("laser", 40),

    // ═══ Movement Abilities ═══
    TELEPORT_LOOK("teleport_look", 15),
    INK_DASH("ink_dash", 25),
    RAM("ram", 60),
    DOLPHIN_LEAP("dolphin_leap", 15),
    TONGUE_GRAB("tongue_grab", 30),
    DASH("dash", 25),
    CHARGE("charge", 40),
    POUNCE("pounce", 20),

    // ═══ Defensive Abilities ═══
    SHELL_DEFENSE("shell_defense", 100),
    PLAY_DEAD("play_dead", 200),
    CURL_UP("curl_up", 80),

    // ═══ Utility Abilities ═══
    SELF_HEAL("self_heal", 200),
    CLEAR_EFFECTS("clear_effects", 200),
    ECHOLOCATION("echolocation", 40);

    private final String id;
    private final int cooldownTicks;

    ActiveAbility(String id, int cooldownTicks) {
        this.id = id;
        this.cooldownTicks = cooldownTicks;
    }

    public String getId() {
        return id;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public float getCooldownSeconds() {
        return cooldownTicks / 20.0F;
    }
}
