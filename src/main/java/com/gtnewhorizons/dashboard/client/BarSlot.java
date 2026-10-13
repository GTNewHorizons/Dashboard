package com.gtnewhorizons.dashboard.client;

import java.util.ArrayList;
import java.util.List;

import com.gtnewhorizons.dashboard.compat.AppleCoreCompat;

/**
 * Where each bar sits on a theme sheet (512 x 512). Slots have 8 px of space around the bar for flair, like outlines or
 * glow.
 */
public enum BarSlot {

    // empty u, v / full u, v / bar width, height / fills from the right, like vanilla's right side rows
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
    FOOD_SATURATION(0, 238, 0, 238, 81, 9, true),
    FOOD_EXHAUSTION(97, 238, 97, 238, 81, 9, true),

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
            case FOOD -> getFoodStates();
            case AIR -> new BarSlot[] { AIR_POP };
            default -> new BarSlot[0];
        };
    }

    private static BarSlot[] getFoodStates() {
        List<BarSlot> states = new ArrayList<>();
        states.add(FOOD_HUNGER);
        if (AppleCoreCompat.showsSaturation()) {
            states.add(FOOD_SATURATION);
        }
        if (AppleCoreCompat.showsExhaustion()) {
            states.add(FOOD_EXHAUSTION);
        }
        return states.toArray(new BarSlot[0]);
    }
}
