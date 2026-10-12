package com.gtnewhorizons.dashboard.client;

/**
 * Bar theme sheet (512 x 512). Each slot is the bar with 8px of space on every side.
 */
public enum BarSlot {

    // empty slot / full slot / bar width, height / fills from the right like the vanilla right side rows
    EXPERIENCE(0, 0, 198, 0, 182, 5, false),
    JUMP_BAR(0, 21, 198, 21, 182, 5, false),
    BOSS_HEALTH(0, 42, 198, 42, 182, 5, false),
    HEALTH(0, 63, 97, 63, 81, 9, false),
    HEALTH_POISONED(0, 63, 0, 138, 81, 9, false),
    HEALTH_WITHERED(0, 63, 97, 138, 81, 9, false),
    HEALTH_ABSORPTION(0, 63, 194, 138, 81, 9, false),
    ARMOR(194, 63, 291, 63, 81, 9, false),
    FOOD(0, 88, 97, 88, 81, 9, true),
    FOOD_HUNGER(0, 88, 291, 138, 81, 9, true),
    AIR(194, 88, 291, 88, 81, 9, true),
    MOUNT_HEALTH(0, 113, 97, 113, 81, 9, true),
    HEALTH_FLASH_EMPTY(0, 163, 97, 63, 81, 9, false),
    HEALTH_FLASH(0, 163, 97, 163, 81, 9, false),
    HEALTH_FLASH_POISONED(0, 163, 194, 163, 81, 9, false),
    HEALTH_FLASH_WITHERED(0, 163, 291, 163, 81, 9, false),
    HEALTH_HARDCORE(97, 188, 97, 188, 81, 9, false),

    HEALTH_REGEN(0, 188, 0, 188, 9, 9, false),
    AIR_POP(25, 188, 25, 188, 9, 9, true);

    public final int emptyU;
    public final int emptyV;
    public final int fullU;
    public final int fullV;
    public final int width;
    public final int height;
    public final boolean fromRight;

    BarSlot(int emptyU, int emptyV, int fullU, int fullV, int width, int height, boolean fromRight) {
        this.emptyU = emptyU;
        this.emptyV = emptyV;
        this.fullU = fullU;
        this.fullV = fullV;
        this.width = width;
        this.height = height;
        this.fromRight = fromRight;
    }

    public BarSlot[] getStates() {
        return switch (this) {
            case HEALTH -> new BarSlot[] { HEALTH_POISONED, HEALTH_WITHERED, HEALTH_ABSORPTION, HEALTH_FLASH_EMPTY,
                HEALTH_FLASH, HEALTH_FLASH_POISONED, HEALTH_FLASH_WITHERED, HEALTH_REGEN, HEALTH_HARDCORE };
            case FOOD -> new BarSlot[] { FOOD_HUNGER };
            case AIR -> new BarSlot[] { AIR_POP };
            default -> new BarSlot[0];
        };
    }
}
