package com.gtnewhorizons.dashboard.client;

/** Bar theme sheet. */
public enum BarSlot {

    // empty / full / width, height / fills from the right like the vanilla right side rows
    EXPERIENCE(0, 0, 0, 5, 182, 5, false),
    JUMP_BAR(0, 10, 0, 15, 182, 5, false),
    BOSS_HEALTH(0, 83, 0, 88, 182, 5, false),
    HEALTH(0, 20, 81, 20, 81, 9, false),
    HEALTH_POISONED(0, 20, 0, 65, 81, 9, false),
    HEALTH_WITHERED(0, 20, 81, 65, 81, 9, false),
    HEALTH_ABSORPTION(0, 20, 0, 74, 81, 9, false),
    ARMOR(0, 29, 81, 29, 81, 9, false),
    FOOD(0, 38, 81, 38, 81, 9, true),
    FOOD_HUNGER(0, 38, 81, 74, 81, 9, true),
    AIR(0, 47, 81, 47, 81, 9, true),
    MOUNT_HEALTH(0, 56, 81, 56, 81, 9, true);

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
}
